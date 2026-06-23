package com.professor.client;

import com.professor.client.gui.KeyActivationScreen;
import com.professor.client.gui.ProfessorMusicManager;
import com.professor.client.gui.ProfessorSplashScreen;
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
    public static final String MOD_ID = "professorclient";
    public static final String CLIENT_NAME = "Xerion Client";

    // ── Timed packet queue (ExploitFixer bypass) ──────────────────────────
    // ExploitFixer analysis:
    //   - TeleportConfirm VL = 0.01 per packet
    //   - VL reduces at 20/sec (1 VL/tick at 20tps)
    //   - Cancel threshold = 25 VL → safe rate = 25 VL / 0.01 = 2500 pkts/tick before cancel
    //   - Sustainable rate = 20 VL/sec / 0.01 = 2000 pkts/sec = 100 pkts/tick
    //   - Kick threshold = 100 VL (tempban 1 min) → we stay under cancel, so never kick
    //   - PPS hard limit = 4096/sec → 2000/sec is safe
    //   - Swing cooldown = 100ms → max 10 swings/sec
    //   - Movement VL = 0.2 each → max 100 movement pkts/sec before cancel
    public static final ConcurrentLinkedQueue<Packet<?>> PACKET_QUEUE = new ConcurrentLinkedQueue<>();
    // Default 100 pkts/tick = 2000/sec (safe for ExploitFixer TeleportConfirm bypass)
    public static volatile int PACKETS_PER_TICK = 100;
    // Swing rate limiter (ExploitFixer: 100ms cooldown)
    private static long lastSwingMs = 0;
    public static volatile int SWING_COOLDOWN_MS = 105; // slight margin over 100ms

    @Override
    public void onInitializeClient() {

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            client.execute(() -> client.setScreen(new ProfessorSplashScreen()));
        });

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.professorclient.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.professorclient.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // ── Open GUI ──────────────────────────────────────────────────
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    playSound(client, "hello_friend");
                    client.setScreen(new KeyActivationScreen());
                }
            }

            // ── Drain packet queue (timed flood) ──────────────────────────
            if (!PACKET_QUEUE.isEmpty() && client.getNetworkHandler() != null) {
                int sent = 0;
                while (!PACKET_QUEUE.isEmpty() && sent < PACKETS_PER_TICK) {
                    Packet<?> pkt = PACKET_QUEUE.poll();
                    if (pkt != null) {
                        try { client.getNetworkHandler().sendPacket(pkt); } catch (Exception ignored) {}
                        sent++;
                    }
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) {
                ProfessorMusicManager.playOnTitleScreen(client);
            }
        });
    }

    // ── Sound helpers ─────────────────────────────────────────────────────
    public static void playSound(MinecraftClient client, String name) {
        try {
            var reg = net.minecraft.registry.Registries.SOUND_EVENT;
            var id  = Identifier.of(MOD_ID, name);
            if (reg.containsId(id)) {
                client.getSoundManager().play(
                    PositionedSoundInstance.master(reg.get(id), 1f, 1f));
            }
        } catch (Exception ignored) {}
    }

    public static void playClickSound(MinecraftClient client) {
        try {
            var reg = net.minecraft.registry.Registries.SOUND_EVENT;
            var id  = Identifier.of("minecraft", "block.note_block.pling");
            if (reg.containsId(id)) {
                client.getSoundManager().play(
                    PositionedSoundInstance.master(reg.get(id), 1.8f, 0.45f));
            }
        } catch (Exception ignored) {}
    }

    // ── Queue helpers ─────────────────────────────────────────────────────
    /** Queue a packet for rate-limited delivery (ExploitFixer bypass) */
    public static void queuePacket(Packet<?> pkt) {
        PACKET_QUEUE.offer(pkt);
    }

    /** Check if swing is allowed under ExploitFixer's 100ms cooldown */
    public static boolean canSwing() {
        long now = System.currentTimeMillis();
        if (now - lastSwingMs >= SWING_COOLDOWN_MS) {
            lastSwingMs = now;
            return true;
        }
        return false;
    }

    public static void clearQueue() { PACKET_QUEUE.clear(); }
}
