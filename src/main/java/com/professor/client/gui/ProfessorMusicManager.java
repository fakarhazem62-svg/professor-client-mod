package com.professor.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Professor Client — Music Manager
 * ═══════════════════════════════════════════════════════════════════
 *
 *  HOW TO ADD CUSTOM MUSIC (the YouTube song):
 *  ──────────────────────────────────────────
 *  1. Download the audio using: https://yt1s.com  or  https://y2mate.com
 *     Paste: https://youtu.be/5qm8PH4xAss
 *
 *  2. Convert to OGG format (required by Minecraft):
 *     Use: https://convertio.co/mp3-ogg/
 *     OR with ffmpeg: ffmpeg -i input.mp3 -c:a libvorbis output.ogg
 *
 *  3. Place the OGG file at:
 *     src/main/resources/assets/professorclient/sounds/music/theme.ogg
 *
 *  4. Create the file sounds.json at:
 *     src/main/resources/assets/professorclient/sounds.json
 *     With this content:
 *     {
 *       "music.theme": {
 *         "sounds": [{ "name": "professorclient:music/theme", "stream": true }]
 *       }
 *     }
 *
 *  5. In the LoopingMusic class below, UNCOMMENT the custom line
 *     and COMMENT OUT the Pigstep line.
 *
 * ═══════════════════════════════════════════════════════════════════
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

    /** Returns 0.0–1.0 for the visual music bar (4-minute virtual loop). */
    public static float getVisualProgress() {
        if (startMs < 0) return 0f;
        long elapsed = System.currentTimeMillis() - startMs;
        long totalMs = 4L * 60 * 1000;
        return (float)(elapsed % totalMs) / totalMs;
    }

    public static boolean isPlaying(MinecraftClient client) {
        return current != null && client.getSoundManager().isPlaying(current);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Sound instance — swap between Pigstep and custom OGG here
    // ─────────────────────────────────────────────────────────────────────
    private static class LoopingMusic extends AbstractSoundInstance {
        LoopingMusic() {
            // ── Option A: Pigstep (default, no extra files needed) ──────────
            super(SoundEvents.MUSIC_DISC_PIGSTEP.value().getId(), SoundCategory.RECORDS, Random.create());

            // ── Option B: Custom OGG (uncomment after adding theme.ogg) ────
            // super(Identifier.of("professorclient", "music.theme"), SoundCategory.RECORDS, Random.create());

            this.repeat          = true;
            this.repeatDelay     = 0;
            this.volume          = 0.45f;
            this.pitch           = 1.0f;
            this.x = 0; this.y = 0; this.z = 0;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
        }
    }
}
