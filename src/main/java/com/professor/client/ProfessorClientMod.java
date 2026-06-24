package com.professor.client;

  import com.professor.client.gui.ProfessorScreen;
  import net.fabricmc.api.ClientModInitializer;
  import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
  import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
  import net.minecraft.client.option.KeyBinding;
  import net.minecraft.client.sound.PositionedSoundInstance;
  import net.minecraft.client.util.InputUtil;
  import net.minecraft.sound.SoundEvents;
  import org.lwjgl.glfw.GLFW;

  public class ProfessorClientMod implements ClientModInitializer {

      public static final String VERSION = "4.0";
      public static final String CLIENT_NAME = "Professor Client";

      public static KeyBinding openGuiKey;

      @Override
      public void onInitializeClient() {
          openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                  "key.professorclient.open_gui",
                  InputUtil.Type.KEYSYM,
                  GLFW.GLFW_KEY_M,
                  "category.professorclient.general"
          ));

          ClientTickEvents.END_CLIENT_TICK.register(client -> {
              while (openGuiKey.wasPressed()) {
                  if (client.player != null) {
                      client.getSoundManager().play(
                              PositionedSoundInstance.master(SoundEvents.MUSIC_DISC_PIGSTEP, 1.0f)
                      );
                      client.setScreen(new ProfessorScreen());
                  }
              }
          });
      }
  }
  