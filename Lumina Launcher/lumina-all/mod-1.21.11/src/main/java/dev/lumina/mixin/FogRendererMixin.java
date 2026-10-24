package dev.lumina.mixin;

import dev.lumina.modules.ModuleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** No Fog: pretends the view distance is huge so terrain fog starts far away. */
@Mixin(targets = "net.minecraft.client.render.fog.FogRenderer")
public class FogRendererMixin {
    @ModifyVariable(method = "applyFog", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int lumina$noFog(int viewDistance) {
        return ModuleManager.on("No Fog") ? viewDistance * 16 : viewDistance;
    }
}
