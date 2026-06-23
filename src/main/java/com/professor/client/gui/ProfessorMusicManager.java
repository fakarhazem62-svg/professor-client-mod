package com.professor.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

/**
 * Manages background music for Professor Client screens.
 * Uses Pigstep (the hype disc) as the epic background track.
 * To use CUSTOM music: add your OGG file to
 *   src/main/resources/assets/professorclient/sounds/music/theme.ogg
 * and update LoopingMusic() to reference it.
 */
public class ProfessorMusicManager {

    private static SoundInstance current  = null;
    private static long          startMs  = -1;
    private static long          pausedMs = 0;

    public static void onOpen(MinecraftClient client) {
        if (current != null && client.getSoundManager().isPlaying(current)) return;
        current = new LoopingMusic();
        client.getSoundManager().play(current);
        startMs = System.currentTimeMillis() - pausedMs;
    }

    public static void onClose(MinecraftClient client) {
        if (current != null) {
            pausedMs = System.currentTimeMillis() - startMs;
            client.getSoundManager().stop(current);
            current = null;
        }
    }

    public static float getVisualProgress() {
        if (startMs < 0) return 0f;
        long elapsed = System.currentTimeMillis() - startMs;
        long totalMs = 4L * 60 * 1000; // 4-minute visual loop
        return (float)(elapsed % totalMs) / totalMs;
    }

    public static boolean isPlaying(MinecraftClient client) {
        return current != null && client.getSoundManager().isPlaying(current);
    }

    // ── Looping music ─────────────────────────────────────────────────────
    // Using PIGSTEP — the most epic/hype disc in Minecraft.
    // Replace SoundEvents.MUSIC_DISC_PIGSTEP.value().getId() with your custom
    // sound identifier (e.g. Identifier.of("professorclient","music.theme"))
    // if you add a custom OGG file.
    private static class LoopingMusic extends AbstractSoundInstance {
        LoopingMusic() {
            super(SoundEvents.MUSIC_DISC_PIGSTEP.value().getId(), SoundCategory.RECORDS, Random.create());
            this.repeat           = true;
            this.repeatDelay      = 0;
            this.volume           = 0.40f;
            this.pitch            = 1.0f;
            this.x = 0; this.y = 0; this.z = 0;
            this.attenuationType  = SoundInstance.AttenuationType.NONE;
        }
    }
}
