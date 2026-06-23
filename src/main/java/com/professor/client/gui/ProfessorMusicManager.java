package com.professor.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Identifier;

public class ProfessorMusicManager {

    private static SoundInstance currentMusic = null;
    private static long   musicStartMs = 0;
    private static boolean playing     = false;

    // Silhouette drop zones in ticks (1 tick = 50ms)
    // Intro 0-280, Verse 280-1000, Chorus 1000-1640, Bridge 1640-2200, Chorus2 2200-2800
    private static final int[][] HYPE_ZONES = {{1000,1640},{2200,2800}};

    public static void onOpen(MinecraftClient client) {
        if (client == null || playing) return;
        try {
            var id  = Identifier.of("professorclient", "music.silhouette");
            var reg = net.minecraft.registry.Registries.SOUND_EVENT;
            if (!reg.containsId(id)) return;
            currentMusic = PositionedSoundInstance.master(reg.get(id), 1f, 0.85f);
            client.getSoundManager().play(currentMusic);
            musicStartMs = System.currentTimeMillis();
            playing = true;
        } catch (Exception ignored) {}
    }

    public static void onClose(MinecraftClient client) {
        if (client == null || currentMusic == null) return;
        try { client.getSoundManager().stop(currentMusic); } catch (Exception ignored) {}
        currentMusic = null;
        playing = false;
    }

    public static boolean isPlaying(MinecraftClient client) { return playing; }

    public static int getMusicTicks() {
        return playing ? (int)((System.currentTimeMillis() - musicStartMs) / 50L) : 0;
    }

    /** 0 = calm, 1 = full drop/hype (chorus) */
    public static float getHypeLevel() {
        if (!playing) return 0f;
        int t = getMusicTicks();
        for (int[] z : HYPE_ZONES) {
            if (t >= z[0] && t < z[1]) {
                float rel = (float)(t - z[0]) / (z[1] - z[0]);
                if      (rel < 0.2f) return rel * 5f;
                else if (rel > 0.8f) return (1f - rel) * 5f;
                else                 return 1f;
            }
        }
        if (t >= 280 && t < 1000) return 0.3f;
        return 0f;
    }

    public static float getVisualProgress() {
        return playing ? Math.min(1f, getMusicTicks() / 3200f) : 0f;
    }
}
