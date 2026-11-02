package dev.overcraft.sojourn;

import dev.overcraft.Ow;
import dev.overcraft.entity.RailgunShotEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static dev.overcraft.sojourn.SojournStats.*;

/**
 * Sojourn's Railgun (her weapon graph 1A23): the left button fires rounds at 16 a second (projectiles: RailgunShotEntity), their
 * hits build her energy; the right button's Charged Shot spends it all on a piercing hitscan through every enemy on its line up
 * to a block. 45 rounds, 1.2 s reload (full at 0.96 s), automatic on empty.
 */
public final class Railgun {
	private Railgun() {
	}

	/** Each tick of her kit (Sojourn.tick): reload, energy decay, the two fires. */
	static void tick(ServerPlayer player, HeroState s, boolean controls) {
		double attack = Combat.attackRate(player);
		long now = player.level().getGameTime();
		reload(s, attack);
		if (s.reloadHeld > 0) {
			s.reloadHeld--;
		}
		s.weaponRecovery = Math.max(0, s.weaponRecovery - attack);
		decay(s, now);
		boolean rightPressed = s.secondaryHeld && !s.sojournRightWas;
		s.sojournRightWas = s.secondaryHeld;
		// the spread comes back 0.3 s after the last shot, over 0.25 s (D #629 / #628)
		if (now - s.sojournShotAt > Ow.ticks(SPREAD_HOLD)) {
			s.sojournSpread = Math.max(0, s.sojournSpread - SPREAD_SHOTS / (SPREAD_RECOVER * 20));
		}
		double rate = RAILGUN_RATE / 20 * attack;
		if (!controls || !player.isAlive()) {
			s.sojournShotClock = 1;
			return;
		}
		if (s.primaryAmmo == 0 && s.primaryReload < 0 && s.reloadHeld <= 0) {
			startReload(player, s);
		}
		// her fire is locked through the whole reload (D #283) and the Charged Shot's 0.65 s
		boolean ready = s.primaryReload < 0 && s.weaponRecovery <= 0;
		// the right button, buffered 0.15 s while her gun is busy (D: SECONDARY's buffer); a press while the Railgun fires
		// stops its rounds, and the shot goes 0.16 s after the last of them (D #513 / #230; ours, that the press waits for it
		// rather than being lost while the left button stays held: found playing)
		if (rightPressed && s.sojournEnergy > 0) {
			s.sojournCharged = Ow.ticks(CHARGED_BUFFER);
		} else if (s.sojournCharged > 0 && (!ready || s.sojournEnergy <= 0)) {
			s.sojournCharged--;
		}
		if (s.sojournCharged > 0 && s.sojournEnergy > 0) {
			if (ready && now - s.sojournShotAt >= Ow.ticks(RAILGUN_TO_CHARGED)) {
				s.sojournCharged = 0;
				chargedShot(player, s);
				s.sojournShotClock = 1;
			}
			return;
		}
		if (s.primaryHeld && ready && s.primaryAmmo > 0) {
			// an accumulator on the exact schedule: the first round at once, then 0.8 a tick (four rounds every five ticks)
			while (s.sojournShotClock >= 1 && s.primaryAmmo > 0) {
				s.sojournShotClock -= 1;
				s.primaryAmmo--;
				shoot(player, s, now);
			}
			s.sojournShotClock += rate;
			if (s.sojournShotAt == now) {
				// the fire layer's clip (the first of a burst 6F09, then 6F08: HeroAnimator)
				Combat.animate(player, "fire");
			}
		} else {
			s.sojournShotClock = Math.min(1, s.sojournShotClock + rate);
		}
	}

	/** R (not full, not reloading, not locked out), or the magazine empty. */
	static void startReload(ServerPlayer player, HeroState s) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= RAILGUN_AMMO || s.weaponRecovery > 0) {
			return;
		}
		s.primaryReload = 0;
		Combat.animate(player, "reload");
	}

	private static void reload(HeroState s, double attack) {
		if (s.primaryReload < 0) {
			return;
		}
		s.primaryReload += attack;
		if (s.primaryReload >= Ow.ticks(RELOAD_FULL_AT)) {
			s.primaryAmmo = RAILGUN_AMMO;
		}
		if (s.primaryReload >= Ow.ticks(RELOAD)) {
			s.primaryReload = -1;
		}
	}

	/** 9 s after her last gain her energy runs down at 15 a second (D #596 / #590). */
	private static void decay(HeroState s, long now) {
		if (s.sojournEnergy > 0 && now - s.sojournGainAt >= Ow.ticks(ENERGY_DECAY_DELAY)) {
			s.sojournEnergy = Math.max(0, s.sojournEnergy - ENERGY_DECAY / 20);
		}
	}

	private static void gain(ServerPlayer player, double energy) {
		HeroState s = HeroManager.state(player);
		if (s == null || s.hero != dev.overcraft.hero.Hero.SOJOURN || energy <= 0) {
			return;
		}
		s.sojournEnergy = Math.min(ENERGY_MAX, s.sojournEnergy + energy);
		s.sojournGainAt = player.level().getGameTime();
	}

	// ---- the rounds ----

	/** One round from her eyes along her aim, spread inside the ellipse its ramp has reached. */
	private static void shoot(ServerPlayer player, HeroState s, long now) {
		double share = Math.min(s.sojournSpread, SPREAD_SHOTS) / SPREAD_SHOTS;
		Vec3 dir = spread(player.getViewVector(1.0f), SPREAD_MAX * share, SPREAD_SIDE * share, player.getRandom());
		s.sojournSpread = Math.min(SPREAD_SHOTS, s.sojournSpread + 1);
		s.sojournShotAt = now;
		RailgunShotEntity shot = new RailgunShotEntity(player, player.level());
		// from her eyes, as Kiriko's kunai (the data's leave the gun's muzzle hp 0006: ours, so the crosshair is where they go)
		shot.launch(player.getEyePosition(), dir, Ow.speed(RAILGUN_SPEED));
		player.level().addFreshEntity(shot);
	}

	/** {@code dir} turned to a random point inside an ellipse {@code up} degrees tall and {@code side} wide (half-angles). */
	static Vec3 spread(Vec3 dir, double up, double side, net.minecraft.util.RandomSource random) {
		if (up <= 0 && side <= 0) {
			return dir;
		}
		Vec3 right = dir.cross(new Vec3(0, 1, 0));
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
		Vec3 upv = right.cross(dir).normalize();
		double r = Math.sqrt(random.nextDouble());
		double a = random.nextDouble() * Math.PI * 2;
		return dir.add(right.scale(Math.tan(Math.toRadians(side)) * r * Math.cos(a)))
			.add(upv.scale(Math.tan(Math.toRadians(up)) * r * Math.sin(a))).normalize();
	}

	/** A round hit {@code entity} at {@code at} (RailgunShotEntity): its damage and her energy. */
	public static void hit(ServerPlayer owner, RailgunShotEntity shot, Entity entity, Vec3 at) {
		ServerLevel level = owner.level();
		EnderDragonPart part = entity instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : entity instanceof LivingEntity living ? living : null;
		DamageSource source = level.damageSources().thrown(shot, owner);
		if (target == null || target instanceof ArmorStand) {
			// an object (an end crystal, a boat, a minecart, an armor stand): hurt as an arrow does, +1 energy (+2 on a crit)
			boolean crit = target != null && at.y >= target.getEyeY() - HEAD_DEPTH;
			entity.hurtServer(level, source, Ow.hp(RAILGUN_DAMAGE * (crit ? RAILGUN_CRIT : 1)));
			gain(owner, crit ? ENERGY_OBJECT_CRIT : ENERGY_OBJECT);
			return;
		}
		if (!Allies.canHurt(owner, target)) {
			return;
		}
		boolean crit = part != null ? part == part.parentMob.head : at.y >= target.getEyeY() - HEAD_DEPTH;
		HeroState s = HeroManager.state(owner);
		if (crit && s != null) {
			s.critAt = level.getGameTime();
		}
		double damage = RAILGUN_DAMAGE * (crit ? RAILGUN_CRIT : 1);
		dev.overcraft.brigitte.Barrier.took = false;
		float dealt = part != null ? Combat.damagePart(level, part, source, damage) : Combat.damage(target, source, damage);
		boolean barrier = dev.overcraft.brigitte.Barrier.took;
		dev.overcraft.brigitte.Barrier.took = false;
		Combat.credit(owner, dealt);
		Combat.addFireForDamage(owner, dealt, Ow.toOw(target.getMaxHealth()));
		if (dealt > 0) {
			Effects.hitMarker(owner, target, crit);
		}
		// energy from the hit, not the damage: none on a hit that did nothing (invulnerable), +1 on a barrier
		gain(owner, barrier ? ENERGY_BARRIER : dealt > 0 ? crit ? ENERGY_CRIT : ENERGY_HIT : 0);
	}

	// ---- Charged Shot ----

	/** One shot along her aim, through every enemy on its line up to a block: 20 + energy each, spending it all. */
	private static void chargedShot(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		double energy = s.sojournEnergy;
		s.sojournEnergy = 0;
		s.weaponRecovery = Ow.ticks(CHARGED_LOCKOUT);
		Combat.animate(player, "charged_shot");
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(CHARGED_RANGE));
		var blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
		record Hit(Entity entity, Vec3 at, double dist) {
		}
		List<Hit> hits = new ArrayList<>();
		java.util.Set<LivingEntity> seen = new java.util.HashSet<>();
		for (Entity e : level.getEntities(player, new AABB(eye, stop).inflate(CHARGED_RADIUS + 2))) {
			LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
			if (target != null ? !target.isAlive() || !Allies.canHurt(player, target) || target == player
				: !e.isPickable() || e.isRemoved()) {
				continue;
			}
			java.util.Optional<Vec3> at = e.getBoundingBox().inflate(CHARGED_RADIUS).clip(eye, stop);
			if (at.isEmpty() && e.getBoundingBox().inflate(CHARGED_RADIUS).contains(eye)) {
				at = java.util.Optional.of(eye);
			}
			at.ifPresent(v -> hits.add(new Hit(e, v, v.distanceTo(eye))));
		}
		hits.sort(java.util.Comparator.comparingDouble(Hit::dist));
		double base = CHARGED_BASE + (CHARGED_FULL - CHARGED_BASE) * energy / ENERGY_MAX;
		for (Hit hit : hits) {
			EnderDragonPart part = hit.entity() instanceof EnderDragonPart p ? p : null;
			LivingEntity target = part != null ? part.parentMob : hit.entity() instanceof LivingEntity living ? living : null;
			if (target != null && !seen.add(target)) {
				// the dragon once, by the first of its parts on the line
				continue;
			}
			double falloff = falloff(hit.dist());
			DamageSource source = shotSource(player);
			if (target == null) {
				hit.entity().hurtServer(level, source, Ow.hp(base * falloff));
				continue;
			}
			boolean crit = part != null ? part == part.parentMob.head : hit.at().y >= target.getEyeY() - HEAD_DEPTH;
			if (crit) {
				s.critAt = level.getGameTime();
			}
			double damage = base * falloff * (crit ? CHARGED_CRIT : 1);
			float dealt = part != null ? Combat.damagePart(level, part, source, damage) : Combat.damage(target, source, damage);
			Combat.credit(player, dealt);
			Combat.addFireForDamage(player, dealt, Ow.toOw(target.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(player, target, crit);
			}
		}
		Vec3 from = muzzle(player);
		Effects.play(level, "sojourn_tracer", from, stop.subtract(from), null, 0, player);
	}

	/** Full to 40 m, then down to 50 % at 60 m and past it. */
	static double falloff(double metres) {
		return metres <= CHARGED_FALLOFF_START ? 1 : metres >= CHARGED_FALLOFF_END ? CHARGED_FALLOFF_MIN
			: 1 - (1 - CHARGED_FALLOFF_MIN) * (metres - CHARGED_FALLOFF_START) / (CHARGED_FALLOFF_END - CHARGED_FALLOFF_START);
	}

	/** Where her gun's muzzle is, from her eyes in her look's frame (ours, by eye: right, down, ahead; the tracer only). */
	private static Vec3 muzzle(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(look).normalize();
		Vec3 at = eye.add(side.scale(0.2)).add(up.scale(-0.15)).add(look.scale(0.7));
		return player.level().clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType()
			== HitResult.Type.MISS ? at : eye;
	}

	/** The head: the top of the box, 0.25 m under the eyes and up (Kiriko's kunai's band). */
	private static final double HEAD_DEPTH = 0.25;

	/** Damage from her with no projectile of its own (the Charged Shot: hitscan), so a kill isn't a melee kill. */
	private static DamageSource shotSource(ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(DamageTypes.ARROW), null, owner);
	}
}
