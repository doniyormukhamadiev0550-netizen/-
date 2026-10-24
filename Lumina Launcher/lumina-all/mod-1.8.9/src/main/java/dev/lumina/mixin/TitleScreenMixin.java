package dev.lumina.mixin;

import dev.lumina.gui.LuminaTitle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the Lumina main menu instead of the vanilla one (vanilla buttons stay as invisible hit areas). */
@Mixin(targets = "net.minecraft.client.gui.screen.TitleScreen")
public class TitleScreenMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void lumina$render(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        LuminaTitle.render(mouseX, mouseY);
        ci.cancel();
    }
}
