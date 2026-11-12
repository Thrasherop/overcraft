package dev.overcraft.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A potion of Healing on a creature (drunk, splashed, lingering, an arrow): the health pack Minecraft doesn't have, for
 * Tracer's Flanker (+50) and Blink Packs (dev.overcraft.tracer.Tracer.healthPack; ours). Only where it heals (its heal call).
 */
@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class HealOrHarmMixin {
	@Inject(method = "applyInstantaneousEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
	private void overcraft$packNow(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity source,
		net.minecraft.world.entity.Entity owner, LivingEntity mob, int amplification, double scale, CallbackInfo ci) {
		dev.overcraft.tracer.Tracer.healthPack(mob);
	}

	@Inject(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
	private void overcraft$packTick(net.minecraft.server.level.ServerLevel level, LivingEntity mob, int amplification,
		CallbackInfoReturnable<Boolean> cir) {
		dev.overcraft.tracer.Tracer.healthPack(mob);
	}
}
