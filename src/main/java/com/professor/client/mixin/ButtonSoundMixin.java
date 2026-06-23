package com.professor.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the default button click sound with a crisp ice-click tone
 * on every button press in every screen (menus, in-game, everywhere).
 */
@Mixin(ButtonWidget.class)
public class ButtonSoundMixin {

    @Inject(at = @At("HEAD"), method = "playDownSound", cancellable = true)
    private void xerion$btnSound(net.minecraft.client.sound.SoundManager sm, CallbackInfo ci) {
        ci.cancel(); // block the vanilla "click" sound
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getSoundManager() == null) return;
            // Crystal ice-click: note_block.bell at high pitch — light, crisp
            mc.getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 2.0f, 0.55f)
            );
        } catch (Exception ignored) {}
    }
}
