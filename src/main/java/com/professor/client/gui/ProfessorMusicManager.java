package com.professor.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public class ProfessorMusicManager {

    private static SoundInstance current = null;
    private static long startMs  = -1;
    private static long pausedMs = 0;

    public static void onOpen(MinecraftClient client) {
        if (current != null && client.getSoundManager().isPlaying(current)) return;
        current = new ThemeSound();
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

    public static float getProgress() {
        if (startMs < 0) return 0f;
        long elapsed = System.currentTimeMillis() - startMs;
        return Math.min(1f, (float)(elapsed % (5L * 60 * 1000)) / (5L * 60 * 1000));
    }

    public static boolean isPlaying(MinecraftClient client) {
        return current != null && client.getSoundManager().isPlaying(current);
    }

    private static class ThemeSound extends AbstractSoundInstance {
        ThemeSound() {
            super(Identifier.of("professorclient", "music.theme"), SoundCategory.RECORDS, Random.create());
            this.repeat          = true;
            this.repeatDelay     = 0;
            this.volume          = 0.55f;
            this.pitch           = 1.0f;
            this.x = 0; this.y = 0; this.z = 0;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
        }
    }
}
