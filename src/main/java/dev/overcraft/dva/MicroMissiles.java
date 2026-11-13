package dev.overcraft.dva;

import dev.overcraft.Ow;
import dev.overcraft.entity.DvaMissileEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Sleep;
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

import static dev.overcraft.dva.DvaStats.*;

/**
 * Micro Missiles (1128; dva-plan.md "Micro Missiles"): E in the mech. After 0.25 s, 18 missiles at 14 a second (an
 * accumulator: 0.7 a tick), from her two shoulder muzzles in turn (hp 0075 / 0077) toward her crosshair, each 50 m/s. A
 * missile does 2 to what it hits, then bursts: 5.5 to every enemy within 0.1 m, down to x0.25 at 1.5 m, <b>her too</b> (W;
 * the plan's Open 5: on), no knockback (D: its knockback fields 0), no block damage. The 7 s cooldown runs from the volley's
 * end. Usable with the Fusion Cannons, Defense Matrix and Boosters (W; D: its own graph). Stand-in look and sound.
 */
public final class MicroMissiles {
	/** What its hits are, for the kill feed (Combat.damage, #84). */
	static final dev.overcraft.hero.Blow BLOW = dev.overcraft.hero.Blow.ability("micro_missiles").by(dev.overcraft.hero.Hero.DVA);

	private MicroMissiles() {
	}

	public static boolean active(HeroState s) {
		return s.dvaMissileTicks >= 0;
	}

	/** E in the mech: a volley, if it's ready (one charge: D #722). */
	static void press(ServerPlayer player, HeroState s) {
		if (active(s) || s.ability2Cooldown > 0 || Sleep.asleep(player)) {
			return;
		}
		s.dvaMissileTicks = 0;
		s.dvaMissilesLeft = MISSILES_COUNT;
		// the first leaves the moment the 0.25 s are over
		s.dvaMissileClock = 1;
		// her missile clips (alias 0x517: 33B6 -> 33B8, additive)
		Combat.animate(player, "add_missiles_se0");
		// the cast (#364 effect 1F99: 4373 hers, 4375 others' 60 m)
		DvaSounds.split(player, "missiles.cast", player.getEyePosition(), 60);
	}

	/** Each tick in hero mode (Dva.tick); {@code can}: she's in her mech with her controls. */
	static void tick(ServerPlayer player, HeroState s, boolean can) {
		if (!active(s)) {
			return;
		}
		if (!can) {
			stop(player, s);
			return;
		}
		s.dvaMissileTicks++;
		if (s.dvaMissileTicks == Ow.ticks(MISSILES_CAST)) {
			// the volley (#357 holds effect 4562: 5DC4 hers, 4376 others' 70 m)
			DvaSounds.split(player, "missiles.volley", player.getEyePosition(), 70);
		}
		if (s.dvaMissileTicks < Ow.ticks(MISSILES_CAST)) {
			return;
		}
		s.dvaMissileClock += MISSILES_RATE / 20.0;
		while (s.dvaMissileClock >= 1 && s.dvaMissilesLeft > 0) {
			s.dvaMissileClock -= 1;
			s.dvaMissilesLeft--;
			shoot(player, s);
		}
		if (s.dvaMissilesLeft <= 0) {
			stop(player, s);
		}
	}

	/** The volley's over (all fired, or cut short): its 7 s from now (#412 -> #407). */
	static void stop(ServerPlayer player, HeroState s) {
		if (!active(s)) {
			return;
		}
		if (s.dvaMissileTicks >= Ow.ticks(MISSILES_CAST)) {
			// 4562's Stop event
			DvaSounds.split(player, "missiles.volley_end", player.getEyePosition(), 70);
		}
		s.dvaMissileTicks = -1;
		s.dvaMissilesLeft = 0;
		s.ability2Cooldown = Ow.ticks(MISSILES_COOLDOWN);
	}

	private static void shoot(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(look).normalize();
		double sign = (s.dvaMissileSide++ & 1) == 0 ? 1 : -1;
		Vec3 from = eye.add(side.scale(sign * MISSILES_MUZZLE_SIDE)).add(up.scale(MISSILES_MUZZLE_UP)).add(look.scale(MISSILES_MUZZLE_AHEAD));
		if (level.clip(new ClipContext(eye, from, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			// a wall beside her: from her eyes
			from = eye;
		}
		// toward what the crosshair is on (else the missile's 100 m out), so both muzzles meet it
		Vec3 far = eye.add(look.scale(MISSILES_RANGE));
		Vec3 target = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		// a creature the crosshair is on comes first: the muzzles sit 0.88 m to either side, so aimed at the block behind it
		// both passed it by (1-2 of 18 on a zombie at 12 m, none on a husk at 5, 12 or 20 m: the real-input pass, 2026-11-11).
		// The volley has no spread or homing (D 1128 #357: no spread field, a straight 50 m/s projectile 1E8A; W none)
		net.minecraft.world.phys.EntityHitResult onIt = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,
			player, eye, target, new AABB(eye, target).inflate(1.0), e -> e != player && e.isPickable() && !e.isSpectator()
				&& !(e instanceof DvaMissileEntity), 0.0f);
		if (onIt != null) {
			target = onIt.getLocation();
		}
		if (target.distanceTo(eye) < 1.5) {
			target = far;
		}
		DvaMissileEntity missile = new DvaMissileEntity(player, level);
		missile.launch(from, target.subtract(from), Ow.speed(MISSILES_SPEED));
		level.addFreshEntity(missile);
	}

	/** The burst's share at {@code d} m: full to 0.1 m, then down to x0.25 at 1.5 m, nothing past it (linear between: M). */
	public static double splashFactor(double d) {
		if (d > MISSILES_SPLASH_RADIUS) {
			return 0;
		}
		if (d <= MISSILES_SPLASH_FULL) {
			return 1;
		}
		double t = (d - MISSILES_SPLASH_FULL) / (MISSILES_SPLASH_RADIUS - MISSILES_SPLASH_FULL);
		return 1 - t * (1 - MISSILES_SPLASH_EDGE);
	}

	/**
	 * A missile bursts at {@code at} ({@code direct}: the creature it hit, which takes the 2 first). Every enemy and she
	 * herself within 1.5 m (to the nearest point of the box), in sight of it and not behind an enemy barrier, take the burst.
	 */
	public static void burst(ServerPlayer owner, DvaMissileEntity missile, Vec3 at, @Nullable Entity direct) {
		ServerLevel level = owner.level();
		EnderDragonPart part = direct instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : direct instanceof LivingEntity living ? living : null;
		if (target != null && Allies.canHurt(owner, target)) {
			DamageSource hit = level.damageSources().thrown(missile, owner);
			// no knockback (D: its fields 0; a hero hit takes none of Minecraft's, #93)
			float dealt = part != null ? Combat.damagePart(level, part, hit, MISSILES_IMPACT, BLOW) : Combat.damage(target, hit, MISSILES_IMPACT, BLOW);
			Combat.credit(owner, dealt, target);
			Combat.addFireForDamage(owner, dealt, target);
			if (dealt > 0) {
				Effects.hitMarker(owner, target, false);
			}
		} else if (direct != null && !(direct instanceof LivingEntity) && part == null) {
			direct.hurtServer(level, level.damageSources().thrown(missile, owner), Ow.hp(MISSILES_IMPACT));
		}
		for (Barriers.Shape b : Barriers.enemyBarriers(level, owner)) {
			Vec3 near = Barriers.nearest(b, at);
			if (near.distanceTo(at) <= MISSILES_SPLASH_RADIUS) {
				b.barrierTake(MISSILES_SPLASH * splashFactor(near.distanceTo(at)), near);
			}
		}
		// a Brigitte's up barrier the same, once (Accretion's way)
		dev.overcraft.brigitte.Barrier.burst(level, owner, at, MISSILES_SPLASH_RADIUS, d -> MISSILES_SPLASH * splashFactor(d));
		DamageSource source = dev.overcraft.hero.Combat.blast(level, missile, owner);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(MISSILES_SPLASH_RADIUS + 2))) {
			boolean self = e == owner;
			if (!e.isAlive() || !self && !Allies.areaHits(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d > MISSILES_SPLASH_RADIUS || !inSight(level, at, e) || !self && (Barriers.between(level, at, e.getBoundingBox().getCenter(), owner) != null
				|| dev.overcraft.brigitte.Barrier.shields(e, owner, at))) {
				continue;
			}
			float dealt = Combat.damage(e, source, MISSILES_SPLASH * splashFactor(d), BLOW);
			if (self) {
				// her own burst hurts her (W), and gives her nothing
				continue;
			}
			Combat.credit(owner, dealt, e);
			Combat.addFireForDamage(owner, dealt, e);
			if (dealt > 0) {
				Effects.hitMarker(owner, e, false);
			}
		}
		// stand-in look and sound (hers: 1E8A's effects, step 11)
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.02);
		// its burst (1FA5: 4372, 200 m), and what it hit (contact sets 783 / 772)
		DvaSounds.all(level, "missiles.burst", at, 200);
		if (target != null) {
			DvaSounds.split(owner, "missiles.hit_enemy", at, 500);
		} else if (direct == null) {
			DvaSounds.all(level, "missiles.hit_block", at, 90);
		}
	}

	static boolean inSight(ServerLevel level, Vec3 at, LivingEntity e) {
		for (Vec3 to : new Vec3[] {e.getBoundingBox().getCenter(), e.getEyePosition()}) {
			if (level.clip(new ClipContext(at, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}
}
