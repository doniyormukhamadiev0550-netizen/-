package dev.lumina.mixin;

import dev.lumina.Badge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Lumina Client" tag in the bottom-right corner of inventories. */
@Mixin(targets = "net.minecraft.client.gui.screen.ingame.HandledScreen")
public class HandledScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void lumina$badge(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Badge.draw();
    }
}
