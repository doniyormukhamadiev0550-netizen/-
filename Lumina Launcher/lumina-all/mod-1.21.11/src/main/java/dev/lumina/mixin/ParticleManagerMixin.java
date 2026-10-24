package dev.lumina.mixin;

import dev.lumina.modules.ModuleManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No Smoke: drops smoke particles before they are created. */
@Mixin(targets = "net.minecraft.client.particle.ParticleManager")
public class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void lumina$noSmoke(ParticleEffect effect, double x, double y, double z, double vx, double vy, double vz,
                                CallbackInfoReturnable<Particle> cir) {
        if (!ModuleManager.on("No Smoke")) return;
        var t = effect.getType();
        if (t == ParticleTypes.SMOKE || t == ParticleTypes.LARGE_SMOKE || t == ParticleTypes.CAMPFIRE_COSY_SMOKE
                || t == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE) {
            cir.setReturnValue(null);
        }
    }
}
