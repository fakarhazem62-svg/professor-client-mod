package com.professor.client;

import com.professor.client.gui.ProfessorMusicManager;
import com.professor.client.gui.ProfessorScreen;
import com.professor.client.proxy.ProxyManager;
import com.professor.client.task.BackgroundTaskManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ProfessorClientMod implements ClientModInitializer {

    public static KeyBinding openGuiKey;
    public static KeyBinding proxyKey;

    public static final String MOD_ID      = "professorclient";
    public static final String CLIENT_NAME = "Xerion Client";
    public static final String VERSION     = "v1";

    // ── Timed packet queue ─────────────────────────────────────────────────
    public static final ConcurrentLinkedQueue<Packet<?>> PACKET_QUEUE = new ConcurrentLinkedQueue<>();
    public static volatile int     PACKETS_PER_TICK   = 100;

    // Swing rate limiter
    private static long lastSwingMs = 0;
    public static volatile int SWING_COOLDOWN_MS = 105;

    @Override
    public void onInitializeClient() {

        // ── Key: M → open Client GUI ───────────────────────────────────────
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.professorclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.professorclient.general"
        ));

        // ── Key: V → toggle proxy (background, NO screen change) ──────────
        proxyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.professorclient.proxy_toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.professorclient.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // M key → open ProfessorScreen directly, no loading screen
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    playClickSound(client);
                    client.setScreen(new ProfessorScreen());
                }
            }

            // V key → toggle proxy in background (NO screen opened)
            while (proxyKey.wasPressed()) {
                if (client.player != null) {
                    boolean nowEnabled = !ProxyManager.isEnabled();
                    ProxyManager.setEnabled(nowEnabled);
                    String msg;
                    if (nowEnabled && ProxyManager.count() > 0) {
                        msg = "§b❄ §fProxy §aON §7— §b" + ProxyManager.aliveCount()
                            + "§7/§b" + ProxyManager.count() + " §7proxies active";
                    } else if (nowEnabled) {
                        msg = "§b❄ §fProxy §aON §7— §cadd proxies via the §bM §cmenu";
                    } else {
                        msg = "§b❄ §fProxy §cOFF";
                    }
                    // Show as action bar (above hotbar) — no screen change
                    client.player.sendMessage(Text.literal(msg), true);
                }
            }

            // Drain packet queue (rate-limited, on main thread)
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
                BackgroundTaskManager.setPacketProgress(
                    BackgroundTaskManager.getPacketsSent() + sent,
                    BackgroundTaskManager.getPacketsTotal()
                );
            }
        });

        // Title screen music
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) {
                ProfessorMusicManager.playOnTitleScreen(client);
            }
        });
    }

    // ── Sound helpers ──────────────────────────────────────────────────────
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

    // ── Packet queue helpers ───────────────────────────────────────────────
    public static void queuePacket(Packet<?> pkt) { PACKET_QUEUE.offer(pkt); }
    public static void clearQueue()               { PACKET_QUEUE.clear(); }

    // ── Swing gate ────────────────────────────────────────────────────────
    public static boolean canSwing() {
        long now = System.currentTimeMillis();
        if (now - lastSwingMs >= SWING_COOLDOWN_MS) { lastSwingMs = now; return true; }
        return false;
    }

    // ── Proxy helper ──────────────────────────────────────────────────────
    public static boolean proxyEnabled() { return ProxyManager.isEnabled() && ProxyManager.count() > 0; }
}
