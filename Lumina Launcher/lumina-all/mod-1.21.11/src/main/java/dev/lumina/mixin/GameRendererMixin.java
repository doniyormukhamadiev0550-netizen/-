package dev.lumina.mixin;

import dev.lumina.modules.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Hurt Cam. */
@Mixin(targets = "net.minecraft.client.render.GameRenderer")
public class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void lumina$noHurtCam(CallbackInfo ci) {
        if (ModuleManager.on("No Hurt Cam")) ci.cancel();
    }
}
