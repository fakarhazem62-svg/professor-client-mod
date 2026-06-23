package com.professor.client;

import com.professor.client.gui.ProfessorScreen;
import com.professor.client.gui.ProfessorSplashScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class ProfessorClientMod implements ClientModInitializer {

    public static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        // Show splash screen on game start
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
                    // Play hype music (Pigstep disc)
                    client.getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.MUSIC_DISC_PIGSTEP, 1.0f)
                    );
                    client.setScreen(new ProfessorScreen());
                }
            }
        });
    }
}
