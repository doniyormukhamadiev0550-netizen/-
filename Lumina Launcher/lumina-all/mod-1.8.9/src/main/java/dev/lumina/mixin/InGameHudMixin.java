package dev.lumina.mixin;

import dev.lumina.modules.ModuleManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs once per rendered frame after the vanilla HUD: ticks modules and draws the Lumina HUD. */
@Mixin(targets = "net.minecraft.client.gui.hud.InGameHud")
public class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void lumina$render(float tickDelta, CallbackInfo ci) {
        ModuleManager.frame(MinecraftClient.getInstance());
    }
}
