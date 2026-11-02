package dev.overcraft.sigma;

import dev.overcraft.Ow;
import dev.overcraft.entity.HypersphereEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.sigma.SigmaStats.*;

/**
 * Hyperspheres (his weapon graph 187F, sigma-map.md): the left button sends a volley of two spheres (HypersphereEntity),
 * the second 0.2 s after the first, then 1.3 s at his attack speed before the next (1.5 s a volley). No ammo, no reload,
 * no crits, no self-damage, no block damage. Each sphere: 20 to the creature it touches, then its burst: 40 within 1 m,
 * down to 20 at its 2.85 m edge, and a small inward pull. Stand-in look and sound (his own come in step 10).
 */
public final class Hyperspheres {
	private Hyperspheres() {
	}

	/** Each tick of his kit: the volley's second sphere, the 1.3 s, the left button (buffered 0.15 s: D PRIMARY's buffer). */
	static void tick(ServerPlayer player, HeroState s, boolean controls, boolean locked) {
		double attack = Combat.attackRate(player);
		if (s.sigmaSecondIn > 0 && --s.sigmaSecondIn == 0) {
			s.sigmaSecondIn = -1;
			if (controls && player.isAlive()) {
				shoot(player);
			}
			s.sigmaReload = Ow.ticks(VOLLEY_RELOAD);
		} else if (s.sigmaSecondIn < 0 && s.sigmaReload > 0) {
			s.sigmaReload = Math.max(0, s.sigmaReload - attack);
		}
		if (s.primaryHeld && controls) {
			s.sigmaBuffer = Ow.ticks(0.15);
		} else if (s.sigmaBuffer > 0) {
			s.sigmaBuffer--;
		}
		if (!controls || !player.isAlive() || locked) {
			return;
		}
		if (s.sigmaBuffer > 0 && s.sigmaSecondIn < 0 && s.sigmaReload <= 0) {
			s.sigmaBuffer = 0;
			shoot(player);
			s.sigmaSecondIn = Ow.ticks(VOLLEY_GAP);
			// the three fire clips cycle a volley each (187F v59 0 -> 1 -> 2: HeroAnimator)
			Combat.animate(player, "spheres_" + s.sigmaVolleys % 3);
			s.sigmaVolleys++;
		}
	}

	/** One sphere from his eyes along his aim (the data's leave the spheres' muzzle hp 007C: ours, so the crosshair is where they go). */
	private static void shoot(ServerPlayer player) {
		HypersphereEntity sphere = new HypersphereEntity(player, player.level());
		sphere.launch(player.getEyePosition(), player.getViewVector(1.0f), Ow.speed(SPHERE_SPEED));
		player.level().addFreshEntity(sphere);
		// stand-in sound (his volley's own 2CE0 / its sounds: step 10)
		player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.6f, 0.7f);
	}

	/** The burst's damage at {@code d} m: 40 to 1 m, then down to x0.5 at 2.85 m, none past it (sigma.md; linear: ours). */
	public static double burstDamage(double d) {
		if (d > SPHERE_BURST_RADIUS) {
			return 0;
		}
		if (d <= SPHERE_BURST_FULL) {
			return SPHERE_BURST;
		}
		double t = (d - SPHERE_BURST_FULL) / (SPHERE_BURST_RADIUS - SPHERE_BURST_FULL);
		return SPHERE_BURST * (1 - t * (1 - SPHERE_BURST_EDGE));
	}

	/** The pull's share at {@code d} m: x0.8 to 0.5 m, down to x0.2 at 2.8 m (its knockback rings, M), linear between (ours). */
	static double pullFactor(double d) {
		if (d <= SPHERE_PULL_NEAR) {
			return SPHERE_PULL_NEAR_FACTOR;
		}
		double t = Math.min(1, (d - SPHERE_PULL_NEAR) / (SPHERE_PULL_FAR - SPHERE_PULL_NEAR));
		return SPHERE_PULL_NEAR_FACTOR + t * (SPHERE_PULL_FAR_FACTOR - SPHERE_PULL_NEAR_FACTOR);
	}

	/**
	 * A sphere bursts at {@code at} ({@code direct}: the creature it touched, which takes the direct 20 first). Every enemy
	 * in its 2.85 m (to the nearest point of its box) in sight of it and not behind an enemy barrier takes the burst, and an
	 * enemy barrier in front of it takes it too.
	 */
	public static void burst(ServerPlayer owner, HypersphereEntity sphere, Vec3 at, @Nullable Entity direct) {
		ServerLevel level = owner.level();
		EnderDragonPart part = direct instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : direct instanceof LivingEntity living ? living : null;
		if (target != null && Allies.canHurt(owner, target)) {
			DamageSource hit = level.damageSources().thrown(sphere, owner);
			float dealt = part != null ? Combat.damagePart(level, part, hit, SPHERE_DIRECT) : Combat.damage(target, hit, SPHERE_DIRECT);
			Combat.credit(owner, dealt, target);
			Combat.addFireForDamage(owner, dealt, Ow.toOw(target.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(owner, target, false);
			}
		} else if (direct != null && !(direct instanceof LivingEntity) && part == null) {
			// an object (a boat, an end crystal): hurt as an arrow would
			direct.hurtServer(level, level.damageSources().thrown(sphere, owner), Ow.hp(SPHERE_DIRECT));
		}
		for (Barriers.Shape b : Barriers.enemyBarriers(level, owner)) {
			Vec3 near = Barriers.nearestFront(b, at);
			if (near != null && near.distanceTo(at) <= SPHERE_BURST_RADIUS) {
				b.barrierTake(burstDamage(near.distanceTo(at)), near);
			}
		}
		DamageSource source = level.damageSources().explosion(sphere, owner);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(SPHERE_BURST_RADIUS + 2))) {
			if (!e.isAlive() || e == owner || !Allies.canHurt(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d > SPHERE_BURST_RADIUS || !inSight(level, at, e) || Barriers.between(level, at, e.getBoundingBox().getCenter(), owner) != null) {
				continue;
			}
			float dealt = Combat.damage(e, source, burstDamage(d));
			Combat.credit(owner, dealt, e);
			Combat.addFireForDamage(owner, dealt, Ow.toOw(e.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(owner, e, false);
			}
			if (e.isAlive()) {
				// the pull: in toward the burst and up (D 1B64 / 1B65), a Stalwart's 40% less
				Vec3 in = new Vec3(at.x - e.getX(), 0, at.z - e.getZ());
				in = in.lengthSqr() < 1.0E-6 ? Vec3.ZERO : in.normalize();
				double f = pullFactor(d) * dev.overcraft.hero.Tank.knockbackFactor(e);
				e.push(in.x * Ow.speed(SPHERE_PULL) * f, Ow.speed(SPHERE_PULL_UP) * f, in.z * Ow.speed(SPHERE_PULL) * f);
				e.needsSync = true;
			}
		}
		// stand-in look and sound (his burst's own: step 10)
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 30, 0.6, 0.6, 0.6, 0.15);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.6f);
	}

	private static boolean inSight(ServerLevel level, Vec3 at, LivingEntity e) {
		for (Vec3 to : new Vec3[] {e.getBoundingBox().getCenter(), e.getEyePosition()}) {
			if (level.clip(new ClipContext(at, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}
}
