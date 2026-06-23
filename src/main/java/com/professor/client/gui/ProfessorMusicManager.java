package com.professor.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Identifier;

public class ProfessorMusicManager {

    private static SoundInstance currentMusic  = null;
    private static boolean       playingState  = false;
    private static float         visualProgress = 0f;

    private static final Identifier MUSIC_RAP =
            Identifier.of("professorclient", "music.rap");
    private static final Identifier MUSIC_THEME =
            Identifier.of("professorclient", "music.theme");

    public static void onOpen(MinecraftClient client) {
        if (playingState || client == null) return;
        try {
            var reg   = net.minecraft.registry.Registries.SOUND_EVENT;
            var event = reg.containsId(MUSIC_RAP) ? reg.get(MUSIC_RAP)
                      : reg.containsId(MUSIC_THEME) ? reg.get(MUSIC_THEME) : null;
            if (event != null) {
                currentMusic = PositionedSoundInstance.master(event, 0.85f, 1.0f);
                client.getSoundManager().play(currentMusic);
                playingState   = true;
                visualProgress = 0f;
            }
        } catch (Exception ignored) {}
    }

    public static void onClose(MinecraftClient client) {
        if (!playingState || client == null || currentMusic == null) return;
        try { client.getSoundManager().stop(currentMusic); } catch (Exception ignored) {}
        currentMusic   = null;
        playingState   = false;
        visualProgress = 0f;
    }

    /** Used by ProfessorScreen's music bar. */
    public static boolean isPlaying(MinecraftClient client) { return playingState; }

    /** Fake visual progress that loops 0→1 while music plays. */
    public static float getVisualProgress() {
        if (playingState) visualProgress = (visualProgress + 0.0008f) % 1f;
        return visualProgress;
    }
}
