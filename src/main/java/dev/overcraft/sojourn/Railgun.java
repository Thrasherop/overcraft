package dev.overcraft.sojourn;

import dev.overcraft.Ow;
import dev.overcraft.entity.RailgunShotEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
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
		if (s.sojournFiring && now - s.sojournShotAt >= BURST_OVER) {
			// the burst is over (PRIMARY up, empty, the Charged Shot, G): 2BF5's Stop, its ring-out (643C hers, 8AF0 100 m)
			s.sojournFiring = false;
			SojournSounds.split(player, "railgun.fire_end", player.getEyePosition(), 100);
		}
		if (rightPressed && s.sojournEnergy <= 0 && controls) {
			// no energy: #451 false -> #632 0186, the shared "not available" click
			dev.overcraft.hero.HeroSounds.toOwner(player, dev.overcraft.registry.OcSounds.KIRIKO_ABILITY_DENIED);
		}
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
		// the gun's own reload clips: 1P 6FCB's 30A6 (921C-921F) hers, 3P 70D5's 30C4 (962A-962D, 45 m) for others
		SojournSounds.split(player, "railgun.reload", player.getEyePosition(), 45);
	}

	/** Ticks with no round that end a burst (16 a second is four rounds every five ticks: a gap of 2 is still firing). */
	private static final int BURST_OVER = 3;

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

	/**
	 * 9 s after her last gain her energy runs down at 15 a second (D #596 / #590), not in Overclock (#931); above 100 after an
	 * Overcharged Overclock it drains to 100 at 20 a second first (#1487 / #1488).
	 */
	private static void decay(HeroState s, long now) {
		if (Sojourn.overclocked(s)) {
			return;
		}
		if (s.sojournEnergy > ENERGY_MAX) {
			s.sojournEnergy = Math.max(ENERGY_MAX, s.sojournEnergy - OVERCHARGED_DRAIN / 20);
		}
		if (s.sojournEnergy > 0 && now - s.sojournGainAt >= Ow.ticks(ENERGY_DECAY_DELAY)) {
			s.sojournEnergy = Math.max(0, s.sojournEnergy - ENERGY_DECAY / 20);
		}
	}

	/** Her energy's cap: 100, 150 in Overclock with Overcharged (D v971, 1A23 #1485). */
	static double cap(ServerPlayer player, HeroState s) {
		return Sojourn.overclocked(s) && HeroManager.hasPerk(player, dev.overcraft.hero.Perk.SOJOURN_OVERCHARGED)
			? ENERGY_MAX_OVERCHARGED : ENERGY_MAX;
	}

	/** Energy from a hit: none in Overclock (D #655 / #656 / #776). */
	private static void gain(ServerPlayer player, double energy) {
		HeroState s = HeroManager.state(player);
		if (s == null || s.hero != dev.overcraft.hero.Hero.SOJOURN || energy <= 0 || Sojourn.overclocked(s)) {
			return;
		}
		s.sojournEnergy = Math.max(s.sojournEnergy, Math.min(cap(player, s), s.sojournEnergy + energy));
		s.sojournGainAt = player.level().getGameTime();
		// #586 2C9A on each gain (5505)
		SojournSounds.own(player, "energy.gain");
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
		// 2BF5 held while she fires (a self-repeating stream, built per round): its once-only first-shot sets on a burst's first
		// round; 2F98's sound slot 3412 (8F0D, 60 m) as each round is made
		SojournSounds.split(player, s.sojournFiring ? "railgun.fire" : "railgun.fire_first", player.getEyePosition(), 100);
		SojournSounds.at(player.level(), "railgun.projectile", player.getEyePosition(), 60);
		s.sojournFiring = true;
	}

	/** A round stopped on a block (RailgunShotEntity): 0701 type 2's hit (5F13 90 m + 5F21 32 m) and 31E5's look, on its face. */
	public static void hitBlock(ServerPlayer owner, Vec3 at, Vec3 normal) {
		SojournSounds.at(owner.level(), "railgun.hit_block", at, 90);
		Effects.play(owner.level(), "sojourn_round_hit", at, normal, null, 0, owner);
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
			struck(owner, at);
			return;
		}
		if (!Allies.canHurt(owner, target)) {
			return;
		}
		struck(owner, at);
		boolean crit = part != null ? part == part.parentMob.head : at.y >= target.getEyeY() - HEAD_DEPTH;
		HeroState s = HeroManager.state(owner);
		if (crit && s != null) {
			s.critAt = level.getGameTime();
		}
		double damage = RAILGUN_DAMAGE * (crit ? RAILGUN_CRIT : 1);
		dev.overcraft.brigitte.Barrier.took = false;
		float dealt = part != null ? Combat.damagePart(level, part, source, damage, Blow.weapon(crit))
			: Combat.damage(target, source, damage, Blow.weapon(crit));
		boolean barrier = dev.overcraft.brigitte.Barrier.took;
		dev.overcraft.brigitte.Barrier.took = false;
		Combat.credit(owner, dealt);
		Combat.addFireForDamage(owner, dealt, Ow.toOw(target.getMaxHealth()));
		if (dealt > 0) {
			Effects.hitMarker(owner, target, crit);
		}
		if (crit && s != null) {
			Sojourn.sharpshooter(owner, s, dealt);
		}
		// energy from the hit, not the damage: none on a hit that did nothing (invulnerable), +1 on a barrier
		gain(owner, barrier ? ENERGY_BARRIER : dealt > 0 ? crit ? ENERGY_CRIT : ENERGY_HIT : 0);
	}

	/**
	 * A round on something (a creature, an object): 2F98's "on a target" slot 2837 -> 4010 -> 8DE7, the shared crack, in her
	 * head; 0701 type 0's 3E84 (8D36 hers, 9058 500 m at the hit for others); 31E5's look there.
	 */
	private static void struck(ServerPlayer owner, Vec3 at) {
		SojournSounds.own(owner, "railgun.hit");
		SojournSounds.own(owner, "railgun.hit_enemy");
		SojournSounds.at(owner.level(), "railgun.hit_enemy_3p", at, 500, owner);
		Effects.play(owner.level(), "sojourn_round_hit", at, Vec3.ZERO, null, 1, owner);
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
		// 2484's 2835 (517B hers, 517D 100 m), pitched by game parameter 2139477060 (read as energy / 100, M: three bins), and
		// its sound slot 3349 (8F11 + 551F, 60 m) for everyone
		double charge = Math.min(1, energy / ENERGY_MAX);
		String fire = charge <= 0.3 ? "charged.fire_low" : charge <= 0.9 ? "charged.fire_mid" : "charged.fire";
		SojournSounds.split(player, fire, eye, 100);
		SojournSounds.at(level, "charged.shot", eye, 60);
		Vec3 from = muzzle(player);
		Effects.play(level, "sojourn_tracer", from, stop.subtract(from), player, (int) Math.round(energy), player);
		s.sojournCharging = true;
		for (Hit hit : hits) {
			EnderDragonPart part = hit.entity() instanceof EnderDragonPart p ? p : null;
			LivingEntity target = part != null ? part.parentMob : hit.entity() instanceof LivingEntity living ? living : null;
			if (target != null && !seen.add(target)) {
				// the dragon once, by the first of its parts on the line
				continue;
			}
			double falloff = falloff(hit.dist());
			DamageSource source = shotSource(player);
			// each one it goes through: the crack in her head (4010), 06FD type 0's 3DBE (8D93 hers, 8FA4 500 m), 22BF's look
			SojournSounds.own(player, "charged.hit");
			SojournSounds.own(player, "charged.hit_enemy");
			SojournSounds.at(level, "charged.hit_enemy_3p", hit.at(), 500, player);
			Effects.play(level, "sojourn_charged_hit", hit.at(), Vec3.ZERO, null, 1, player);
			if (target == null) {
				hit.entity().hurtServer(level, source, Ow.hp(base * falloff));
				continue;
			}
			boolean crit = part != null ? part == part.parentMob.head : hit.at().y >= target.getEyeY() - HEAD_DEPTH;
			if (crit) {
				s.critAt = level.getGameTime();
			}
			double damage = base * falloff * (crit ? CHARGED_CRIT : 1);
			// the Charged Shot has its own icon in the feed; fired in Overclock, the ult's (the wiki names Overclock among
			// her elimination icons, and in it her charged shots are the ult's: which shot it shows for is L)
			Blow blow = (Sojourn.overclocked(s) ? Blow.ult("overclock") : Blow.ability("charged_shot")).crit(crit);
			float dealt = part != null ? Combat.damagePart(level, part, source, damage, blow) : Combat.damage(target, source, damage, blow);
			Combat.credit(player, dealt);
			Combat.addFireForDamage(player, dealt, Ow.toOw(target.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(player, target, crit);
			}
			if (crit) {
				Sojourn.sharpshooter(player, s, dealt);
			}
		}
		s.sojournCharging = false;
		if (blockHit.getType() != HitResult.Type.MISS) {
			// on the block it stopped at: 06FD type 2's 3DC0 (5517 90 m + 5518 32 m) and 22BF's look on its face
			SojournSounds.at(level, "charged.hit_block", stop, 90);
			net.minecraft.core.Direction face = ((net.minecraft.world.phys.BlockHitResult) blockHit).getDirection();
			Effects.play(level, "sojourn_charged_hit", stop, new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()), null, 0,
				player);
		}
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
		return dev.overcraft.hero.Combat.hitscan(owner);
	}
}
