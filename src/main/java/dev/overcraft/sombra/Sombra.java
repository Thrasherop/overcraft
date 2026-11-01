package dev.overcraft.sombra;

import dev.overcraft.Ow;
import dev.overcraft.entity.TranslocatorEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.sombra.SombraStats.*;

/**
 * Sombra's kit (tools/reference/sombra-plan.md "Order"), built step by step, each played before the next. Now: the
 * Machine Pistol (her weapon graph 0B1B): hitscan, 20 shots a second while the left button is held, 60 to a clip, a 1.2 s
 * reload (the clip full at 0.48 s), automatic at 0 and on R; damage falling off from 20 m to 35 m; the spread growing from 0
 * to 2 degrees over 10 shots and back to 0 after 0.5 s without firing; head hits x2. Her abilities come next.
 */
public final class Sombra implements HeroKit {
	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = PISTOL_AMMO;
		s.primaryReload = -1;
		s.weaponRecovery = 0;
		s.burstShots = 0;
		s.hotfixCharges = HOTFIX_CHARGES;
		s.hotfixRecharge = -1;
		s.hotfixChannel = -1;
		s.hotfixTarget = null;
		s.beaconId = null;
		s.teleportTicks = -1;
		s.stealthFrom = s.stealthUntil = s.revealedUntil = -1;
		s.castTicks = -1;
	}

	/** Leaving Sombra (another hero, leaving the world): her beacon goes, stealth and its speed end. */
	public static void clear(ServerPlayer player, HeroState s) {
		if (s.beaconId != null && player.level().getEntity(s.beaconId) instanceof TranslocatorEntity beacon) {
			beacon.discard();
		}
		s.beaconId = null;
		s.teleportTicks = -1;
		Stealth.clear(player, s);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double attack = Combat.attackRate(player);
		reload(s, attack);
		s.weaponRecovery = Math.max(0, s.weaponRecovery - attack);
		recharge(player, s);
		double cooldown = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - cooldown);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - cooldown);
		if (!player.isAlive()) {
			Stealth.clear(player, s);
			s.teleportTicks = -1;
		}
		Stealth.tick(player, s);
		if (s.teleportTicks >= 0) {
			teleporting(player, s);
			return;
		}
		if (!controls || !player.isAlive()) {
			s.hotfixChannel = -1;
			s.hotfixTarget = null;
			s.hotfixHeld = false;
			return;
		}
		cyberspaceCast(player, s);
		hotfix(player, s);
		// the clip empty: its reload starts by itself
		if (s.primaryAmmo == 0 && s.primaryReload < 0) {
			startReload(player, s);
		}
		if (s.primaryHeld && s.primaryReload < 0 && s.primaryAmmo > 0 && s.hotfixChannel < 0 && s.castTicks < 0) {
			// as many shots as the rate allows this tick (20 a second at 20 ticks: one each tick)
			while (s.weaponRecovery <= 0 && s.primaryAmmo > 0) {
				fire(player, s);
			}
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if (s.teleportTicks >= 0) {
			return;
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0) {
			startReload(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0) {
			translocator(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			startCyberspace(player, s);
		}
	}

	// ---- Cyberspace (Shift: 610E, the zone 6102) ----

	/** Shift: a 0.45 s cast, the projectile leaving at 0.1 s from her left wrist; the cooldown from the cast's end. */
	private static void startCyberspace(ServerPlayer player, HeroState s) {
		if (s.ability1Cooldown > 0 || s.castTicks >= 0) {
			return;
		}
		s.castTicks = 0;
		s.ability1Cooldown = Ow.ticks(CYBER_COOLDOWN + CYBER_CAST);
		Combat.animate(player, "throw_cyberspace");
		Stealth.end(player, s);
	}

	private static void cyberspaceCast(ServerPlayer player, HeroState s) {
		if (s.castTicks < 0) {
			return;
		}
		if (s.castTicks == Ow.ticks(CYBER_FIRE_AT)) {
			ServerLevel level = player.level();
			Vec3 from = fromEyes(player, WRIST_RIGHT, WRIST_UP, WRIST_AHEAD);
			dev.overcraft.entity.CyberspaceEntity shot = new dev.overcraft.entity.CyberspaceEntity(player, level);
			shot.launch(from, aimPoint(player).subtract(from), Ow.speed(CYBER_SPEED));
			level.addFreshEntity(shot);
		}
		if (++s.castTicks >= Ow.ticks(CYBER_CAST)) {
			s.castTicks = -1;
		}
	}

	/** What her crosshair is on (a block, else 100 m out): the projectile flies there from her wrist. */
	static Vec3 aimPoint(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(player.getViewVector(1.0f).scale(100));
		Vec3 hit = player.level().clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		return hit.distanceTo(eye) < 1 ? far : hit;
	}

	// ---- Translocator (0C19, the beacon 0C1A) and stealth (420C) ----

	/**
	 * E: throws the beacon from her left wrist (hp 0002) with the cooldown ready; E again, once it's 0.17 s out, teleports
	 * her to it (its 7.5 s cooldown 1 s after landing). A beacon gone unused (its 15 s, shot down) starts the cooldown.
	 */
	private static void translocator(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		TranslocatorEntity beacon = s.beaconId != null && level.getEntity(s.beaconId) instanceof TranslocatorEntity b && b.isAlive() ? b : null;
		if (beacon != null) {
			if (beacon.tickCount >= Ow.ticks(BEACON_READY)) {
				s.teleportTicks = 0;
				Combat.animate(player, "translocate");
			}
			return;
		}
		if (s.ability2Cooldown > 0) {
			return;
		}
		Vec3 from = fromEyes(player, WRIST_RIGHT, WRIST_UP, WRIST_AHEAD);
		TranslocatorEntity thrown = new TranslocatorEntity(player, level);
		thrown.launch(from, player.getViewVector(1.0f), Ow.speed(BEACON_SPEED));
		level.addFreshEntity(thrown);
		s.beaconId = thrown.getId();
		Combat.animate(player, "throw");
	}

	/** Her left wrist (hp 0002), from her eyes in her look's frame: ours until measured on her model (Ana's numbers). */
	private static final double WRIST_RIGHT = -0.25, WRIST_UP = -0.2, WRIST_AHEAD = 0.45;

	/** A beacon gone without a teleport (its 15 s, or shot down): the cooldown from now (ours: when it starts then isn't read). */
	public static void beaconGone(ServerPlayer player, TranslocatorEntity beacon) {
		HeroState s = HeroManager.state(player);
		if (s != null && s.beaconId != null && s.beaconId == beacon.getId()) {
			s.beaconId = null;
			s.teleportTicks = -1;
			s.ability2Cooldown = Ow.ticks(TRANSLOCATOR_COOLDOWN);
		}
	}

	/** The 0.4 s move: protected and without her keys, then she's at the beacon, 0.5 m above it, and stealth starts. */
	private static void teleporting(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		Combat.phase(player, 0.1);
		TranslocatorEntity beacon = s.beaconId != null && level.getEntity(s.beaconId) instanceof TranslocatorEntity b && b.isAlive() ? b : null;
		if (beacon == null) {
			s.teleportTicks = -1;
			return;
		}
		if (++s.teleportTicks < Ow.ticks(TELEPORT_TIME)) {
			return;
		}
		s.teleportTicks = -1;
		// on a wall she stands in front of it, on a ceiling under it (ours: Minecraft's box is wider than her capsule)
		Vec3 face = beacon.face();
		double half = player.getBbWidth() / 2 + 0.05;
		Vec3 to = beacon.position().add(face.x * half, face.y < 0 ? -player.getBbHeight() : TELEPORT_UP, face.z * half);
		// still in something solid: up a little at a time (ours)
		for (int i = 0; i < 8 && !level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(to)); i++) {
			to = to.add(0, 0.25, 0);
		}
		beacon.discard();
		s.beaconId = null;
		player.teleportTo(level, to.x, to.y, to.z, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
		s.ability2Cooldown = Ow.ticks(TRANSLOCATOR_COOLDOWN + TRANSLOCATOR_COOLDOWN_DELAY);
		Stealth.start(player, s);
	}

	/** From her eyes in her look's frame (right, up, ahead, m), clipped to her eyes if a block is in the way. */
	static Vec3 fromEyes(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 upv = side.cross(look).normalize();
		Vec3 at = eye.add(side.scale(right)).add(upv.scale(up)).add(look.scale(ahead));
		return player.level().clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType()
			== HitResult.Type.MISS ? at : eye;
	}

	// ---- Hotfix (614C, its heal 60FB) ----

	/** One charge back every 8 s while she has fewer than two (2A57 / 2ADA), at the cooldown rate. */
	private static void recharge(ServerPlayer player, HeroState s) {
		if (s.hotfixCharges >= HOTFIX_CHARGES) {
			s.hotfixRecharge = -1;
			return;
		}
		if (s.hotfixRecharge < 0) {
			s.hotfixRecharge = Ow.ticks(HOTFIX_RECHARGE);
		}
		s.hotfixRecharge -= Combat.cooldownRate(player);
		if (s.hotfixRecharge <= 0) {
			s.hotfixCharges++;
			s.hotfixRecharge = s.hotfixCharges < HOTFIX_CHARGES ? Ow.ticks(HOTFIX_RECHARGE) : -1;
		}
	}

	/**
	 * The right button, held (the data's default; letting go cancels): the ally nearest her crosshair within 17 m and 10
	 * degrees, in sight, is locked; after a 0.5 s channel still held, a charge is spent and they're healed (HotfixHeal).
	 * Then 0.3 s of recovery before the next. No ally in view: nothing happens, nothing spent.
	 */
	private static void hotfix(ServerPlayer player, HeroState s) {
		boolean pressed = s.secondaryHeld && !s.hotfixHeld;
		s.hotfixHeld = s.secondaryHeld;
		if (s.hotfixChannel >= 0) {
			LivingEntity target = s.hotfixTarget == null ? null : player.level().getEntity(s.hotfixTarget) instanceof LivingEntity l ? l : null;
			if (!s.secondaryHeld || target == null || !target.isAlive() || target.distanceTo(player) > HOTFIX_RANGE + 1) {
				s.hotfixChannel = -1;
				s.hotfixTarget = null;
				return;
			}
			if (++s.hotfixChannel >= Ow.ticks(HOTFIX_CAST)) {
				s.hotfixChannel = -1;
				s.hotfixTarget = null;
				s.hotfixCharges--;
				s.weaponRecovery = Math.max(s.weaponRecovery, Ow.ticks(HOTFIX_RECOVERY));
				HotfixHeal.start(player, target);
			}
			return;
		}
		if (!pressed || s.hotfixCharges <= 0 || s.weaponRecovery > 0 || s.primaryReload >= 0) {
			return;
		}
		LivingEntity target = dev.overcraft.hero.Targeting.allyInView(player, HOTFIX_RANGE, HOTFIX_CONE, true);
		if (target == null) {
			return;
		}
		s.hotfixTarget = target.getId();
		s.hotfixChannel = 0;
		// Hotfix in stealth reveals her without ending it (BZ 2026-10-06; ours: for its channel and recovery)
		Stealth.reveal(player, s, HOTFIX_CAST + HOTFIX_RECOVERY);
		Combat.animate(player, "hotfix");
	}

	// ---- Machine Pistol (0B1B) ----

	private static void fire(ServerPlayer player, HeroState s) {
		long now = player.level().getGameTime();
		if (now - s.lastShotAt > Ow.ticks(PISTOL_SPREAD_RESET)) {
			s.burstShots = 0;
		}
		s.primaryAmmo--;
		s.weaponRecovery += 20 / PISTOL_RATE;
		// a shot ends stealth (420C)
		Stealth.end(player, s);
		s.lastShotAt = now;
		double spread = PISTOL_SPREAD_MAX * Math.min(1, s.burstShots / PISTOL_SPREAD_SHOTS);
		s.burstShots++;
		Combat.animate(player, "fire");
		hitscan(player, s, spread);
	}

	/**
	 * One shot: from her eyes along her aim, turned by up to {@code spread} degrees at random (a cone), to the first enemy
	 * within the shot's size before a block. Allies are passed through (it doesn't heal).
	 */
	private static void hitscan(ServerPlayer player, HeroState s, double spread) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = cone(player.getViewVector(1.0f), spread, player.getRandom());
		Vec3 end = eye.add(dir.scale(PISTOL_RANGE));
		Vec3 block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		LivingEntity best = null;
		Vec3 bestAt = null;
		double bestDist = eye.distanceToSqr(block);
		for (net.minecraft.world.entity.Entity e : level.getEntities(player, new AABB(eye, block).inflate(1))) {
			LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
			if (target == null || !target.isAlive() || !Allies.canHurt(player, target)) {
				continue;
			}
			java.util.Optional<Vec3> hit = e.getBoundingBox().inflate(PISTOL_RADIUS).clip(eye, block);
			if (hit.isPresent() && hit.get().distanceToSqr(eye) < bestDist) {
				best = target;
				bestAt = hit.get();
				bestDist = hit.get().distanceToSqr(eye);
			}
		}
		if (best == null) {
			return;
		}
		double metres = Math.sqrt(bestDist);
		double falloff = metres <= PISTOL_FALLOFF_START ? 1 : metres >= PISTOL_FALLOFF_END ? PISTOL_FALLOFF_MIN
			: 1 - (1 - PISTOL_FALLOFF_MIN) * (metres - PISTOL_FALLOFF_START) / (PISTOL_FALLOFF_END - PISTOL_FALLOFF_START);
		boolean crit = bestAt.y >= best.getEyeY() - HEAD_DEPTH;
		float dealt = Combat.damage(best, shotSource(player), PISTOL_DAMAGE * falloff * (crit ? PISTOL_CRIT : 1));
		Combat.credit(player, dealt);
		Combat.addFireForDamage(player, dealt, Ow.toOw(best.getMaxHealth()));
		if (dealt > 0) {
			if (crit) {
				s.critAt = level.getGameTime();
			}
			Effects.hitMarker(player, best, crit);
		}
	}

	/** The head is the top of the box, as for Kiriko's kunai and Ana's darts (ours: 0.25 m under the eyes and up). */
	private static final double HEAD_DEPTH = 0.25;

	/** {@code dir} turned by an angle up to {@code degrees}, evenly over the cone's disc. */
	static Vec3 cone(Vec3 dir, double degrees, net.minecraft.util.RandomSource random) {
		if (degrees <= 0) {
			return dir;
		}
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(dir).normalize();
		double r = Math.tan(Math.toRadians(degrees)) * Math.sqrt(random.nextDouble());
		double a = random.nextDouble() * Math.PI * 2;
		return dir.add(side.scale(r * Math.cos(a))).add(up.scale(r * Math.sin(a))).normalize();
	}

	/** Damage from her with no projectile of its own (the pistol's shots: hitscan), so a kill isn't a melee kill. */
	static DamageSource shotSource(ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(DamageTypes.ARROW), null, owner);
	}

	/** R (not with a full clip or mid-reload), or the clip empty: 1.2 s, the clip full 0.48 s in. */
	private static void startReload(ServerPlayer player, HeroState s) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= PISTOL_AMMO) {
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
		if (s.primaryReload >= Ow.ticks(PISTOL_RELOAD * PISTOL_RELOAD_AMMO)) {
			s.primaryAmmo = PISTOL_AMMO;
		}
		if (s.primaryReload >= Ow.ticks(PISTOL_RELOAD)) {
			s.primaryReload = -1;
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float reload = s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(PISTOL_RELOAD)) : -1;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			s.primaryAmmo, reload, s.hotfixCharges, s.hotfixRecharge < 0 ? -1 : (float) (1 - s.hotfixRecharge / Ow.ticks(HOTFIX_RECHARGE)),
			(float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, 0, 0, 0);
	}
}
