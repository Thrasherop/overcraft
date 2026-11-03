package dev.overcraft.sigma;

import dev.overcraft.Ow;
import dev.overcraft.entity.HypersphereEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * down to 20 at its 2.85 m edge, and a small inward pull. Look and sound from his data (step 10: sigma-effects.md
 * "Hyperspheres", sigma-sounds.md 1): the volley 2CE0, each sphere's flight 2BDE (SigmaFx, client.SigmaSounds), its burst 2EAB.
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
				shoot(player, true);
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
			shoot(player, false);
			s.sigmaSecondIn = Ow.ticks(VOLLEY_GAP);
			// 187F #207 2CE0 once a volley (5E06 + 64F3 his, 5461 + D8EB others' 100 m); his hand spheres re-form (2D5B,
			// SigmaFx); his line (0B99, #221: its 5 s cooldown)
			SigmaSounds.split(player, "sphere.volley", player.getEyePosition(), 100);
			Effects.play(player.level(), "sigma_volley", player.getEyePosition(), player.getViewVector(1.0f), player, 0, player);
			dev.overcraft.hero.Voice.say(player, "hyperspheres", 1);
			// the three fire clips cycle a volley each (187F v59 0 -> 1 -> 2: HeroAnimator)
			Combat.animate(player, "spheres_" + s.sigmaVolleys % 3);
			s.sigmaVolleys++;
		}
	}

	/** One sphere from his eyes along his aim (the data's leave the spheres' muzzle hp 007C: ours, so the crosshair is where they go). */
	private static void shoot(ServerPlayer player, boolean second) {
		HypersphereEntity sphere = new HypersphereEntity(player, player.level());
		sphere.second = second;
		sphere.launch(player.getEyePosition(), player.getViewVector(1.0f), Ow.speed(SPHERE_SPEED));
		player.level().addFreshEntity(sphere);
	}

	/**
	 * A sphere came off a wall (its contact set: 6CB the first sphere's, 6CC the second's): 591A / 591B his at the hit point
	 * (70 m), 591C / 591D others' (30 m).
	 */
	public static void bounced(ServerPlayer owner, boolean second, Vec3 at) {
		SigmaSounds.splitAt(owner, second ? "sphere.hit_block2" : "sphere.hit_block", at, 70, 30);
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
			float dealt = part != null ? Combat.damagePart(level, part, hit, SPHERE_DIRECT, dev.overcraft.hero.Blow.WEAPON.by(dev.overcraft.hero.Hero.SIGMA))
				: Combat.damage(target, hit, SPHERE_DIRECT, dev.overcraft.hero.Blow.WEAPON.by(dev.overcraft.hero.Hero.SIGMA));
			// Hyper Regeneration and Hyper Strike's count (perks)
			SigmaPerks.sphereDealt(owner, dealt, true);
			Combat.credit(owner, dealt, target);
			Combat.addFireForDamage(owner, dealt, Ow.toOw(target.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(owner, target, false);
			}
			// the hit (2BE1's 4010 -> 8DE7, the shared crack) and the enemy contact 6D3 (8A83 his, 90E0 others' 500 m)
			SigmaSounds.own(owner, "sphere.hit");
			SigmaSounds.own(owner, "sphere.hit_enemy");
			SigmaSounds.at(level, "sphere.hit_enemy_3p", at, 500, owner);
		} else if (direct != null && !(direct instanceof LivingEntity) && part == null) {
			// an object (a boat, an end crystal): hurt as an arrow would
			direct.hurtServer(level, level.damageSources().thrown(sphere, owner), Ow.hp(SPHERE_DIRECT));
		}
		for (Barriers.Shape b : Barriers.enemyBarriers(level, owner)) {
			Vec3 near = Barriers.nearest(b, at);
			if (near.distanceTo(at) <= SPHERE_BURST_RADIUS) {
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
			float dealt = Combat.damage(e, source, burstDamage(d), dev.overcraft.hero.Blow.WEAPON.by(dev.overcraft.hero.Hero.SIGMA));
			SigmaPerks.sphereDealt(owner, dealt, false);
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
				Accretion.sendSpeed(e);
			}
		}
		// its burst 2EAB (m_6CB564CC, the same on a creature, a wall or at its range's end: SigmaFx), and its sound: 5E0A / 5E09
		// his at the sphere (200 m), 5496 / 5582 others' (100 m); on contact others also hear 59C8 (m_AED73588's, 100 m)
		Effects.play(level, "sigma_burst", at, Vec3.ZERO, null, direct != null ? 1 : 0, owner);
		boolean second = sphere.second;
		SigmaSounds.splitAt(owner, second ? "sphere.burst2" : "sphere.burst", at, 200, 100);
		if (direct != null) {
			SigmaSounds.at(level, "sphere.impact_3p", at, 100, owner);
		}
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
