package com.professor.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClickableWidget.class)
public class ButtonSoundMixin {

    @Inject(at = @At("HEAD"), method = "playDownSound", cancellable = true)
    private void xerion$btnSound(net.minecraft.client.sound.SoundManager sm, CallbackInfo ci) {
        ci.cancel();
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getSoundManager() == null) return;
            mc.getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 2.0f, 0.55f)
            );
        } catch (Exception ignored) {}
    }
}
