package com.professor.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

/**
 * Manages background music for Professor Client screens.
 * Remembers the elapsed position so reopening the screen "resumes" from where it left off.
 */
public class ProfessorMusicManager {

    private static SoundInstance current   = null;
    private static long          startMs   = -1;  // System.currentTimeMillis() when music began
    private static long          pausedMs  = 0;   // How many ms had elapsed when paused

    /** Call when the Professor screen is opened. */
    public static void onOpen(MinecraftClient client) {
        if (current != null && client.getSoundManager().isPlaying(current)) return;
        // Create a looping instance of the menu music
        current = new LoopingMusic();
        client.getSoundManager().play(current);
        startMs = System.currentTimeMillis() - pausedMs;
    }

    /** Call when the Professor screen is closed (removed). */
    public static void onClose(MinecraftClient client) {
        if (current != null) {
            pausedMs = System.currentTimeMillis() - startMs;
            client.getSoundManager().stop(current);
            current = null;
        }
    }

    /** Returns 0-1 progress through a virtual 4-minute track, for UI display. */
    public static float getVisualProgress() {
        if (startMs < 0) return 0f;
        long elapsed = System.currentTimeMillis() - startMs;
        long totalMs = 4L * 60 * 1000; // 4-minute loop
        return (float)(elapsed % totalMs) / totalMs;
    }

    public static boolean isPlaying(MinecraftClient client) {
        return current != null && client.getSoundManager().isPlaying(current);
    }

    // ── Looping music sound instance ───────────────────────────────────────

    private static class LoopingMusic extends AbstractSoundInstance {
        LoopingMusic() {
            super(SoundEvents.MUSIC_MENU.getId(), SoundCategory.MUSIC, Random.create());
            this.repeat      = true;
            this.repeatDelay = 0;
            this.volume      = 0.35f;
            this.pitch       = 1.0f;
            this.x = 0; this.y = 0; this.z = 0;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
        }
    }
}
