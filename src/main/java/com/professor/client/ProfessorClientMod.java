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
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ProfessorClientMod implements ClientModInitializer {

    public static KeyBinding openGuiKey;
    public static final String MOD_ID = "professorclient";
    public static final String CLIENT_NAME = "Xerion Client";

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
            while (openGuiKey.wasPressed()) {
                if (client.player != null) {
                    playSound(client, "hello_friend");
                    client.setScreen(new KeyActivationScreen());
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) {
                ProfessorMusicManager.playOnTitleScreen(client);
            }
        });
    }

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
}
