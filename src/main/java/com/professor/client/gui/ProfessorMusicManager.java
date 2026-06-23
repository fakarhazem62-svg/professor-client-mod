package com.professor.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

/**
 * Manages background music for Professor Client screens.
 *
 * Default: plays Pigstep (the most hype disc in Minecraft) on loop.
 *
 * To add CUSTOM music:
 *   1. Convert your audio to OGG format (use convertio.co or ffmpeg)
 *   2. Place the file at:
 *      src/main/resources/assets/professorclient/sounds/music/theme.ogg
 *   3. Create src/main/resources/assets/professorclient/sounds.json:
 *      {
 *        "music.theme": {
 *          "sounds": [{ "name": "professorclient:music/theme", "stream": true }]
 *        }
 *      }
 *   4. Replace the sound in LoopingMusic() with:
 *      super(net.minecraft.util.Identifier.of("professorclient","music.theme"), ...)
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

    /** Returns 0.0-1.0 progress for the visual music bar (4-minute loop). */
    public static float getVisualProgress() {
        if (startMs < 0) return 0f;
        long elapsed = System.currentTimeMillis() - startMs;
        long totalMs = 4L * 60 * 1000;
        return (float)(elapsed % totalMs) / totalMs;
    }

    public static boolean isPlaying(MinecraftClient client) {
        return current != null && client.getSoundManager().isPlaying(current);
    }

    private static class LoopingMusic extends AbstractSoundInstance {
        LoopingMusic() {
            // Using PIGSTEP — most hype/epic Minecraft music disc
            super(SoundEvents.MUSIC_DISC_PIGSTEP.value().getId(), SoundCategory.RECORDS, Random.create());
            this.repeat          = true;
            this.repeatDelay     = 0;
            this.volume          = 0.40f;
            this.pitch           = 1.0f;
            this.x = 0; this.y = 0; this.z = 0;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
        }
    }
}
