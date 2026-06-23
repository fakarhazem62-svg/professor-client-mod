package com.professor.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

/**
 * Manages background rap music for Professor Client.
 * Music plays when the main screen opens and stops when closed.
 *
 * To add custom rap music:
 *   1. Place your OGG file at:
 *      .minecraft/resourcepacks/ProfessorClient/assets/professorclient/sounds/music/rap.ogg
 *   2. The sounds.json already registers it as "professorclient:music.rap"
 */
public class ProfessorMusicManager {

    private static SoundInstance currentMusic = null;
    private static boolean       playing      = false;

    private static final Identifier MUSIC_RAP =
            Identifier.of("professorclient", "music.rap");

    private static final Identifier MUSIC_THEME =
            Identifier.of("professorclient", "music.theme");

    /** Call when ProfessorScreen opens. */
    public static void onOpen(MinecraftClient client) {
        if (playing || client == null) return;

        // Try custom rap music first, fall back to theme
        try {
            currentMusic = PositionedSoundInstance.master(
                    net.minecraft.registry.Registries.SOUND_EVENT
                            .get(MUSIC_RAP) != null
                            ? net.minecraft.registry.Registries.SOUND_EVENT.get(MUSIC_RAP)
                            : net.minecraft.registry.Registries.SOUND_EVENT.get(MUSIC_THEME),
                    0.85f, 1.0f
            );
            if (currentMusic != null) {
                client.getSoundManager().play(currentMusic);
                playing = true;
            }
        } catch (Exception e) {
            // Silently ignore if sound isn't available
        }
    }

    /** Call when ProfessorScreen closes. */
    public static void onClose(MinecraftClient client) {
        if (!playing || client == null || currentMusic == null) return;
        try {
            client.getSoundManager().stop(currentMusic);
        } catch (Exception ignored) {}
        currentMusic = null;
        playing      = false;
    }

    public static boolean isPlaying() { return playing; }
}
