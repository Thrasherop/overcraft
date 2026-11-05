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

	/**
	 * Juno's Glide Boost is a jetpack: in water or lava it keeps its own mover rather than swimming, as Overwatch has no
	 * liquids to slow it (ours, #79: it crawled at the swim speed in her glide pose).
	 */
	@Inject(method = "travelInFluid", at = @At("HEAD"), cancellable = true)
	private void overcraft$glideInFluid(Vec3 input, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (OwMovement.handles(self) && OwMovement.junoGlide.applyAsInt(self) >= 0) {
			OwMovement.travel(self, input);
			ci.cancel();
		}
	}

	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void overcraft$owJump(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (OwMovement.handles(self) && OwMovement.noJump.test(self)) {
			// an ability holds the jump (Sigma's Accretion gather: 1BBB #651)
			ci.cancel();
			return;
		}
		if (OwMovement.handles(self) && OwMovement.sliding(self)) {
			// sliding, the jump key is Power Slide's jump (OwMovement.slideTravel), not Minecraft's
			ci.cancel();
			return;
		}
		if (OwMovement.handles(self)) {
			Vec3 v = self.getDeltaMovement();
			// off a rising Petal Platform the jump is 6 m/s stronger (dev.overcraft.entity.PetalPlatformEntity)
			double bonus = dev.overcraft.entity.PetalPlatformEntity.jumpBonus(self);
			self.setDeltaMovement(v.x, Math.max(Ow.speed(OwMovement.JUMP_SPEED + bonus), v.y), v.z);
			ci.cancel();
		}
	}

	/** Minecraft's own jump off a rising Petal Platform: 6 m/s stronger too. */
	@Inject(method = "jumpFromGround", at = @At("TAIL"))
	private void overcraft$platformJump(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		double bonus = dev.overcraft.entity.PetalPlatformEntity.jumpBonus(self);
		if (bonus > 0) {
			Vec3 v = self.getDeltaMovement();
			self.setDeltaMovement(v.x, v.y + Ow.speed(bonus), v.z);
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

	/**
	 * A Mercy's damage boost and Ana's Nano Boost (dev.overcraft.mercy.Boosts): what a boosted attacker deals arrives
	 * raised, from any weapon, a pet's bite or an arrow they shot (the source's entity is the shooter), before armour and
	 * the rest; a Nano-boosted victim takes half (dev.overcraft.ana.Nano), before shields take it.
	 */
	@org.spongepowered.asm.mixin.injection.ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float overcraft$damageBoost(float damage,
										@com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.damagesource.DamageSource source) {
		// a weakened attacker (Sombra's Cyberspace) deals half
		float boosted = dev.overcraft.mercy.Boosts.amplify(source.getEntity(), damage) * dev.overcraft.ana.Nano.takenFactor((LivingEntity) (Object) this)
			* (float) dev.overcraft.sombra.Cyberspace.dealtFactor(source.getEntity());
		// a hero's armor (Brigitte's) takes each hit 7 smaller, at most halved (dev.overcraft.hero.Shields.armorReduce): not
		// one a Brigitte's barrier takes instead, which it would have shrunk on the barrier (#88)
		return (Object) this instanceof net.minecraft.server.level.ServerPlayer player
			&& dev.overcraft.brigitte.Barrier.covering(player, source) == null
			? dev.overcraft.hero.Shields.armorReduce(player, boosted) : boosted;
	}

	/**
	 * A Brigitte's barrier between the hit and them (dev.overcraft.brigitte.Barrier) takes it before anything else, all of
	 * it: no hit at all (absorbed with 0 damage left, Minecraft still knocked her 3 m back and let the next arrow past her:
	 * found playing), and a projectile stops at it.
	 */
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void overcraft$barrier(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
								   float damage, CallbackInfoReturnable<Boolean> cir) {
		if (dev.overcraft.brigitte.Barrier.absorb((LivingEntity) (Object) this, source, damage) < damage) {
			if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
				projectile.discard();
			}
			cir.setReturnValue(false);
		}
	}

	/**
	 * A hero's hit takes no Minecraft knockback (#93, Combat.heroHit): Overwatch's weapons and most abilities don't push,
	 * and those that do push for themselves after the hit. The hurt still tilts a hit player's view toward whoever hit
	 * them (indicateDamage, from the shooter for a hitscan shot, which has no position of its own).
	 */
	@com.llamalad7.mixinextras.injector.v2.WrapWithCondition(method = "hurtServer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;dealDefaultKnockback(Lnet/minecraft/world/damagesource/DamageSource;FZ)V"))
	private boolean overcraft$heroHitKnockback(LivingEntity self, net.minecraft.world.damagesource.DamageSource source, float damage,
											   boolean blocked) {
		if (!dev.overcraft.hero.Combat.heroHit(self, source)) {
			return true;
		}
		net.minecraft.world.phys.Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition()
			: source.getEntity() != null ? source.getEntity().position() : null;
		if (from != null && !blocked) {
			self.indicateDamage(from.x - self.getX(), from.z - self.getZ());
		}
		return false;
	}

	/** Nor Minecraft's impact (#93): a hit player would be sent their stale motion, a jolt (minecraft_26.md, #82). */
	@com.llamalad7.mixinextras.injector.v2.WrapWithCondition(method = "hurtServer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;markHurt()V"))
	private boolean overcraft$heroHitImpact(LivingEntity self,
											@com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.damagesource.DamageSource source) {
		return !dev.overcraft.hero.Combat.heroHit(self, source);
	}

	/**
	 * Shields (dev.overcraft.hero.Shields): every change to a shielded hero's health passes here. A loss comes off the
	 * shields first; a gain heals the health under them, then them.
	 */
	@org.spongepowered.asm.mixin.injection.ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float overcraft$shields(float health) {
		return (Object) this instanceof net.minecraft.server.level.ServerPlayer player
			? dev.overcraft.hero.Shields.onSetHealth(player, health) : health;
	}

	/**
	 * Jetpack Cat flying takes 40% less knockback (W: her "flight resistance"); a Stalwart tank (Sigma) 40% less of every
	 * knockback, times any state's own (dev.overcraft.hero.Tank: D 01C7 #969).
	 */
	@org.spongepowered.asm.mixin.injection.ModifyVariable(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double overcraft$flightResistance(double power) {
		return power * dev.overcraft.hero.Combat.knockbackTaken((LivingEntity) (Object) this);
	}
}
