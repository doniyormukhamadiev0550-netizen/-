package dev.lumina.mixin;

import dev.lumina.modules.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Low Shield: pushes the first-person item down a bit while a shield is held. */
@Mixin(targets = "net.minecraft.client.render.item.HeldItemRenderer")
public class HeldItemRendererMixin {
    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float lumina$lowShield(float equipProgress) {
        var p = MinecraftClient.getInstance().player;
        if (p != null && ModuleManager.on("Low Shield")
                && (p.getMainHandStack().isOf(Items.SHIELD) || p.getOffHandStack().isOf(Items.SHIELD))) {
            return equipProgress + 0.3f;
        }
        return equipProgress;
    }
}
