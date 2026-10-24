package dev.lumina.mixin;

import dev.lumina.modules.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Fire (variant for a static target). Exactly one of the two fire mixins matches; the other is skipped. */
@Mixin(targets = "net.minecraft.client.gui.hud.InGameOverlayRenderer")
public class FireOverlayStaticMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void lumina$noFire(CallbackInfo ci) {
        if (ModuleManager.on("No Fire")) ci.cancel();
    }
}
