package dev.overcraft.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A running target goal lets go of a stealthed Sombra it can't see (Stealth.hiddenFrom). Minecraft's TargetGoal.canContinueToUse
 * falls back to the creature it remembers (targetMob) when the mob's target has been cleared and sets it again, so clearing it
 * (Stealth.dropTargets) didn't hold: an enemy Ashe kept shooting and chasing a stealthed Sombra from 10 m (owner, 2026-11-22, #185).
 */
@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin {
	@Shadow
	@Final
	protected Mob mob;

	@Shadow
	protected @Nullable LivingEntity targetMob;

	@Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
	private void overcraft$stealthedSombra(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity target = mob.getTarget() != null ? mob.getTarget() : targetMob;
		if (target != null && dev.overcraft.sombra.Stealth.hiddenFrom(target, mob)) {
			cir.setReturnValue(false);
		}
	}
}
