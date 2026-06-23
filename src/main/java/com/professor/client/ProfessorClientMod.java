package com.professor.client;

import com.professor.client.gui.ProfessorMusicManager;
import com.professor.client.gui.ProfessorScreen;
import com.professor.client.gui.ProfessorSplashScreen;
import com.professor.client.proxy.ProxyManager;
import com.professor.client.task.BackgroundTaskManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ProfessorClientMod implements ClientModInitializer {

    public static KeyBinding openGuiKey;
    public static final String MOD_ID      = "professorclient";
    public static final String CLIENT_NAME = "Xerion Client";
    public static final String VERSION     = "v4.0";

    // ── Timed packet queue (ExploitFixer bypass) ──────────────────────────
    public static final ConcurrentLinkedQueue<Packet<?>> PACKET_QUEUE = new ConcurrentLinkedQueue<>();
    public static volatile int     PACKETS_PER_TICK   = 100;
    public static volatile boolean BG_MODE_ACTIVE      = false;

    // Swing rate limiter (ExploitFixer: 100ms cooldown)
    private static long    lastSwingMs     = 0;
    public  static volatile int SWING_COOLDOWN_MS = 105;

    @Override
    public void onInitializeClient() {

        // Splash screen on startup
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
            client.execute(() -> client.setScreen(new ProfessorSplashScreen()))
        );

        // Keybinding: M → open GUI
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.professorclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.professorclient.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Open GUI
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    playSound(client, "hello_friend");
                    client.setScreen(new ProfessorScreen());
                }
            }

            // Drain packet queue (rate-limited)
            if (!PACKET_QUEUE.isEmpty() && client.getNetworkHandler() != null) {
                int sent = 0;
                while (!PACKET_QUEUE.isEmpty() && sent < PACKETS_PER_TICK) {
                    Packet<?> pkt = PACKET_QUEUE.poll();
                    if (pkt != null) {
                        try { client.getNetworkHandler().sendPacket(pkt); } catch (Exception ignored) {}
                        sent++;
                    }
                }
                // Report progress to background task manager
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
