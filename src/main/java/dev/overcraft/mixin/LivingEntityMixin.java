package dev.overcraft.mixin;

import dev.overcraft.Ow;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Overwatch movement and jump for a hero in slot 1 (see {@link OwMovement}). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "travelInAir", at = @At("HEAD"), cancellable = true)
	private void overcraft$owTravel(Vec3 input, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (OwMovement.handles(self)) {
			OwMovement.travel(self, input);
			ci.cancel();
		}
	}

	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void overcraft$owJump(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (OwMovement.handles(self)) {
			Vec3 v = self.getDeltaMovement();
			self.setDeltaMovement(v.x, Math.max(Ow.speed(OwMovement.JUMP_SPEED), v.y), v.z);
			ci.cancel();
		}
	}

	/**
	 * A hero's fall hurts from the hero's own safe height (#35), Minecraft's way: what's beyond it, times the block's
	 * and the attribute's multipliers. The higher of the hero's and Minecraft's own safe distance counts.
	 */
	@Inject(method = "calculateFallDamage", at = @At("HEAD"), cancellable = true)
	private void overcraft$heroSafeFall(double fallDistance, float damageModifier, CallbackInfoReturnable<Integer> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		Hero hero = self instanceof Player player && HeroManager.controls(player) ? HeroManager.hero(player) : null;
		if (hero != null) {
			double safe = Math.max(hero.safeFall(), self.getAttributeValue(Attributes.SAFE_FALL_DISTANCE));
			cir.setReturnValue(Mth.floor((fallDistance + 1.0E-6 - safe) * damageModifier
				* self.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER)));
		}
	}

	/** Phased (Protection Suzu): no knockback either, as no damage (a creeper's blast pushed her: #27). */
	@Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"), cancellable = true)
	private void overcraft$phasedKnockback(double power, double xd, double zd, net.minecraft.world.damagesource.DamageSource source,
										   float damage, boolean comesFromEffect, CallbackInfo ci) {
		if (dev.overcraft.hero.Combat.isPhased((LivingEntity) (Object) this)) {
			ci.cancel();
			return;
		}
		// a hero's knockback starts from the motion their client reports: Minecraft keeps an airborne target's stored
		// vertical speed, which for a player moved by their client is stale (a zombie's hit threw a flying Jetpack Cat
		// down at 22 m/s: found playing)
		if ((Object) this instanceof net.minecraft.server.level.ServerPlayer player && HeroManager.controls(player)) {
			player.setDeltaMovement(player.getKnownMovement());
		}
	}

	/** Jetpack Cat flying takes 40% less knockback (W: her "flight resistance"). */
	@org.spongepowered.asm.mixin.injection.ModifyVariable(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double overcraft$flightResistance(double power) {
		return (Object) this instanceof Player player && HeroManager.controls(player) && HeroManager.hero(player) == Hero.JETPACK_CAT
			? power * dev.overcraft.jetpackcat.JetpackCatStats.KNOCKBACK_TAKEN : power;
	}
}
