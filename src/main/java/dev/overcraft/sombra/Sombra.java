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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.sombra.SombraStats.*;

/**
 * Sombra's kit (tools/reference/sombra-plan.md "Order"), built step by step, each played before the next: the Machine
 * Pistol (her weapon graph 0B1B): hitscan, 20 shots a second while the left button is held, 60 to a clip, a 1.2 s reload
 * (the clip full at 0.48 s), automatic at 0 and on R; damage falling off from 20 m to 35 m; the spread growing from 0 to 2
 * degrees over 10 shots and back to 0 after 0.5 s without firing; head hits x2. Hotfix (right button), Cyberspace (Shift),
 * the Translocator (E), EMP (Q) and her four perks below.
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
		s.stealthFrom = s.stealthUntil = s.revealedUntil = -1;
		s.castTicks = -1;
	}

	/** Leaving Sombra (another hero, leaving the world): her beacon goes, stealth and its speed end. */
	public static void clear(ServerPlayer player, HeroState s) {
		if (s.beaconId != null && player.level().getEntity(s.beaconId) instanceof TranslocatorEntity beacon) {
			beacon.discard();
		}
		s.beaconId = null;
		Stealth.clear(player, s);
		Conspirator.clear(player, s);
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
		}
		Stealth.tick(player, s);
		Conspirator.tick(player, s);
		if (s.sombraFiring && player.level().getGameTime() - s.lastShotAt > 2) {
			// the burst over (released, empty, an ability): the stream's tail once
			s.sombraFiring = false;
			SombraSounds.split(player, "pistol.fire_end", player.getEyePosition(), 200);
		}
		// an ability's landing never hurts (the owner's rule, #35): the teleport's fall doesn't count until she's down
		if (s.softLanding) {
			player.resetFallDistance();
			// (from a few ticks after the arrival, stealthFrom: the ground she left still counts just after the teleport)
			if (player.onGround() && player.level().getGameTime() > s.stealthFrom + 4) {
				s.softLanding = false;
			}
		}
		if (!controls || !player.isAlive()) {
			s.hotfixChannel = -1;
			s.hotfixTarget = null;
			s.hotfixHeld = false;
			return;
		}
		cyberspaceCast(player, s);
		empCast(player, s);
		hotfix(player, s);
		// the clip empty: its reload starts by itself
		if (s.primaryAmmo == 0 && s.primaryReload < 0) {
			startReload(player, s);
		}
		if (s.primaryHeld && s.primaryReload < 0 && s.primaryAmmo > 0 && s.hotfixChannel < 0 && s.castTicks < 0 && s.empCast < 0) {
			// as many shots as the rate allows this tick (20 a second at 20 ticks: one each tick)
			while (s.weaponRecovery <= 0 && s.primaryAmmo > 0) {
				fire(player, s);
			}
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0) {
			startReload(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0) {
			translocator(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			startCyberspace(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0) {
			startEmp(player, s);
		}
	}

	// ---- EMP (Q: 0B31, each target 0B32, the status 0B1F) ----

	/**
	 * Q with the charge full: spent at the press (a Tactician's excess stays: Combat.afterUlt); a 0.65 s cast, the pulse
	 * 0.35 s in; no teleport during it (press returns while casting). Her charge stays still until 5 s after the cast (v842).
	 */
	private static void startEmp(ServerPlayer player, HeroState s) {
		if (s.ultCharge < ULT_COST || s.empCast >= 0 || s.castTicks >= 0 || s.hotfixChannel >= 0) {
			return;
		}
		s.ultCharge = Combat.afterUlt(dev.overcraft.hero.Hero.SOMBRA, s.ultCharge);
		s.ultActive = Ow.ticks(EMP_CAST + EMP_NO_CHARGE);
		s.empCast = 0;
		Combat.animate(player, "ult_cast");
		Effects.play(player.level(), "sombra_emp_cast", player.position(), Vec3.ZERO, player, 0, player);
		// her cast, and her lines: "EMP activated!" to her side, the Spanish one to her enemies (069A, 0699)
		SombraSounds.own(player, "emp.cast");
		dev.overcraft.hero.HeroSounds.ultLine(player, SombraSounds.of("ult.friendly"), SombraSounds.of("ult.enemy"), 5);
		Stealth.end(player, s);
	}

	private static void empCast(ServerPlayer player, HeroState s) {
		if (s.empCast < 0) {
			return;
		}
		if (s.empCast == Ow.ticks(EMP_PULSE_AT)) {
			pulse(player);
		}
		if (++s.empCast >= Ow.ticks(EMP_CAST)) {
			s.empCast = -1;
		}
	}

	/**
	 * The pulse: every enemy within 15 m of her, in sight (terrain blocks it), takes 20% of its current health (no crit, through
	 * armour: magic) and is hacked (Hack); enemy deployables in reach are hacked 10 s. Life Hack: Hotfix on every ally within
	 * 15 m, her too (0B31 #294 / #372).
	 */
	private static void pulse(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		AABB reach = player.getBoundingBox().inflate(EMP_RADIUS);
		DamageSource source = Cyberspace.zoneSource(player);
		boolean lifeHack = HeroManager.hasPerk(player, Perk.SOMBRA_LIFE_HACK);
		Effects.play(level, "sombra_emp", player.position(), Vec3.ZERO, player, 0, player);
		SombraSounds.split(player, "emp.pulse", player.getEyePosition(), 75);
		if (lifeHack) {
			SombraSounds.split(player, "perk.life_hack", player.getEyePosition(), 45);
		}
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, reach, e -> e.isAlive() && e != player)) {
			if (nearest(e, eye).distanceTo(eye) > EMP_RADIUS || !inSight(player, e)) {
				continue;
			}
			if (Allies.canHurt(player, e)) {
				float dealt = Combat.damage(e, source, EMP_SHARE * Ow.toOw(e.getHealth()));
				Combat.credit(player, dealt);
				Combat.addFireForDamage(player, dealt, Ow.toOw(e.getMaxHealth()));
				Effects.hitMarker(player, e, false);
				Hack.creature(e, HACK_SILENCE, player);
				SombraSounds.at(level, "emp.hit", e.getEyePosition(), 45);
				if (e instanceof ServerPlayer enemy && HeroManager.state(enemy) instanceof HeroState es && es.hero == dev.overcraft.hero.Hero.SOMBRA) {
					// a hacked Sombra's stealth ends too (0B1F's interrupt, M)
					Stealth.end(enemy, es);
				}
			} else if (lifeHack && Allies.isAlly(player, e) && e.distanceTo(player) <= LIFE_HACK_RADIUS) {
				HotfixHeal.start(player, e);
			}
		}
		if (lifeHack) {
			HotfixHeal.start(player, player);
		}
		for (net.minecraft.world.entity.Entity d : level.getEntities(player, reach, Sombra::deployable)) {
			if (enemyDeployable(player, d) && nearest(d, eye).distanceTo(eye) <= EMP_RADIUS) {
				Hack.deployable(d, player);
			}
		}
	}

	/** Hotfix's other target: the enemy deployable nearest her crosshair within 17 m and 10 degrees, in sight, not hacked. */
	private static net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity deployableInView(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		net.minecraft.world.entity.Entity best = null;
		double bestAngle = HOTFIX_CONE;
		for (net.minecraft.world.entity.Entity d : player.level().getEntities(player, player.getBoundingBox().inflate(HOTFIX_RANGE), Sombra::deployable)) {
			Vec3 to = d.getBoundingBox().getCenter().subtract(eye);
			double angle = Math.toDegrees(Math.acos(Math.clamp(to.normalize().dot(look), -1, 1)));
			if (to.length() > HOTFIX_RANGE || angle > bestAngle || !enemyDeployable(player, d) || Hack.hacked(d)
				|| player.level().clip(new ClipContext(eye, eye.add(to), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
				continue;
			}
			best = d;
			bestAngle = angle;
		}
		return best;
	}

	/** The point of its box nearest her eyes (a big mob, the dragon, is in reach by its edge). */
	private static Vec3 nearest(net.minecraft.world.entity.Entity e, Vec3 eye) {
		AABB box = e.getBoundingBox();
		return new Vec3(Math.clamp(eye.x, box.minX, box.maxX), Math.clamp(eye.y, box.minY, box.maxY), Math.clamp(eye.z, box.minZ, box.maxZ));
	}

	/** Sight to any of its hit points (0B31: several; ours its eyes, middle and feet), blocks in the way. */
	private static boolean inSight(ServerPlayer player, LivingEntity e) {
		Vec3 eye = player.getEyePosition();
		for (Vec3 to : new Vec3[] {e.getEyePosition(), e.getBoundingBox().getCenter(), e.position().add(0, 0.1, 0)}) {
			if (player.level().clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}

	/** What EMP and Hotfix hack: Lifeweaver's platform and tree (a Sombra's beacon flies 0.17 s and can't be touched: W). */
	private static boolean deployable(net.minecraft.world.entity.Entity e) {
		return e instanceof dev.overcraft.entity.PetalPlatformEntity
			|| e instanceof dev.overcraft.entity.TreeOfLifeEntity;
	}

	private static boolean enemyDeployable(ServerPlayer player, net.minecraft.world.entity.Entity d) {
		net.minecraft.world.entity.Entity owner = d instanceof dev.overcraft.entity.PetalPlatformEntity p ? player.level().getEntity(p.ownerId())
			: d instanceof dev.overcraft.entity.TreeOfLifeEntity t ? player.level().getEntity(t.ownerId()) : null;
		return owner instanceof LivingEntity living && living != player && Allies.canHurt(player, living);
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
		SombraSounds.split(player, "cyberspace.cast", player.getEyePosition(), 70);
		Stealth.end(player, s);
	}

	private static void cyberspaceCast(ServerPlayer player, HeroState s) {
		if (s.castTicks < 0) {
			return;
		}
		if (s.castTicks == Ow.ticks(CYBER_FIRE_AT)) {
			ServerLevel level = player.level();
			Vec3 from = fromEyes(player, WRIST_RIGHT, WRIST_UP, WRIST_AHEAD);
			Effects.play(level, "sombra_cyber_launch", from, Vec3.ZERO, player, 0, player);
			SombraSounds.at(level, "cyberspace.shot", from, 45);
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
	 * E: throws the beacon from her left wrist (hp 0002) with the cooldown ready. It takes her by itself (no second press):
	 * on its first touch of terrain, or 12.25 m along its path (0.17 s at 72 m/s) wherever it is (BZ 2026-10-06; W). While
	 * it flies, E does nothing. (The E-again beacon with 15 s and 5 health read from the data matches the Community
	 * Crafted mode's, not the live game.)
	 */
	private static void translocator(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		if (s.beaconId != null && level.getEntity(s.beaconId) instanceof TranslocatorEntity b && b.isAlive()) {
			return;
		}
		s.beaconId = null;
		if (s.ability2Cooldown > 0) {
			return;
		}
		Vec3 from = fromEyes(player, WRIST_RIGHT, WRIST_UP, WRIST_AHEAD);
		TranslocatorEntity thrown = new TranslocatorEntity(player, level);
		thrown.launch(from, player.getViewVector(1.0f), Ow.speed(BEACON_SPEED));
		level.addFreshEntity(thrown);
		s.beaconId = thrown.getId();
		Combat.animate(player, "throw");
		SombraSounds.split(player, "translocator.throw", player.getEyePosition(), 35);
	}

	/** Her left wrist (hp 0002), from her eyes in her look's frame: ours until measured on her model (Ana's numbers). */
	private static final double WRIST_RIGHT = -0.25, WRIST_UP = -0.2, WRIST_AHEAD = 0.45;

	/** A beacon gone without taking her (out of the world, she left Sombra): nothing spent (W: a death plane refunds it). */
	public static void beaconGone(ServerPlayer player, TranslocatorEntity beacon) {
		HeroState s = HeroManager.state(player);
		if (s != null && s.beaconId != null && s.beaconId == beacon.getId()) {
			s.beaconId = null;
		}
	}

	/**
	 * The beacon takes her: she's there at once, 0.5 m above it (D 0C19), phased out 0.4 s (W), the 7.5 s cooldown from now
	 * (BZ; W), stealth starting (420C).
	 */
	public static void arrive(ServerPlayer player, TranslocatorEntity beacon) {
		HeroState s = HeroManager.state(player);
		if (s == null || s.hero != dev.overcraft.hero.Hero.SOMBRA || s.beaconId == null || s.beaconId != beacon.getId()
			|| !player.isAlive() || player.level() != beacon.level()) {
			return;
		}
		ServerLevel level = player.level();
		s.beaconId = null;
		// on a wall she stands in front of it, on a ceiling under it (ours: Minecraft's box is wider than her capsule)
		Vec3 face = beacon.face();
		double half = player.getBbWidth() / 2 + 0.05;
		Vec3 to = beacon.position().add(face.x * half, face.y < 0 ? -player.getBbHeight() : TELEPORT_UP, face.z * half);
		// still in something solid: up a little at a time (ours)
		for (int i = 0; i < 8 && !level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(to)); i++) {
			to = to.add(0, 0.25, 0);
		}
		// where she left and where she lands (1A76, 5FE0), then stealth's fade starting on her (1AD9)
		Effects.play(level, "sombra_teleport", player.position(), Vec3.ZERO, player, 0, player);
		SombraSounds.own(player, "translocator.depart");
		dev.overcraft.hero.HeroSounds.at(level, SombraSounds.of("translocator.depart_3p"), player.position(), 45, player);
		dev.overcraft.hero.HeroSounds.at(level, SombraSounds.of("translocator.dest_3p"), to, 45, player);
		player.teleportTo(level, to.x, to.y, to.z, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
		Effects.play(level, "sombra_teleport", to, Vec3.ZERO, player, 0, player);
		Effects.play(level, "sombra_stealth", to, Vec3.ZERO, player, 0, player);
		SombraSounds.at(level, "translocator.arrive", to, 45);
		SombraSounds.split(player, "stealth.start", to, 30);
		SombraSounds.own(player, "stealth.loop");
		dev.overcraft.hero.Voice.say(player, "translocate", 1);
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
		s.softLanding = true;
		Combat.phase(player, TELEPORT_PHASED);
		s.ability2Cooldown = Ow.ticks(TRANSLOCATOR_COOLDOWN);
		Combat.animate(player, "translocate");
		Stealth.start(player, s);
		// CTRL ALT ESC: a teleport below half health starts the regeneration at once (BA5; BZ; W)
		if (HeroManager.hasPerk(player, Perk.SOMBRA_CTRL_ALT_ESC) && player.getHealth() < player.getMaxHealth() * CTRL_ALT_ESC_BELOW) {
			s.lastDamagedTick = Math.min(s.lastDamagedTick, Combat.now(player) - Combat.REGEN_DELAY);
		}
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
			SombraSounds.own(player, "hotfix.recharge");
			s.hotfixRecharge = s.hotfixCharges < HOTFIX_CHARGES ? Ow.ticks(HOTFIX_RECHARGE) : -1;
		}
	}

	/**
	 * Data Packet: a hack of a non-ally takes 4 s off the recharge, the rest carried into the next charge (FBC). Ours: her
	 * hacks here are enemy deployables only (Minecraft has no health packs: sombra-plan.md).
	 */
	static void dataPacket(HeroState s) {
		double cut = Ow.ticks(DATA_PACKET);
		while (cut > 0 && s.hotfixCharges < HOTFIX_CHARGES) {
			if (s.hotfixRecharge < 0) {
				s.hotfixRecharge = Ow.ticks(HOTFIX_RECHARGE);
			}
			double take = Math.min(cut, s.hotfixRecharge);
			s.hotfixRecharge -= take;
			cut -= take;
			if (s.hotfixRecharge <= 0) {
				s.hotfixCharges++;
				s.hotfixRecharge = s.hotfixCharges < HOTFIX_CHARGES ? Ow.ticks(HOTFIX_RECHARGE) : -1;
			}
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
			net.minecraft.world.entity.Entity target = s.hotfixTarget == null ? null : player.level().getEntity(s.hotfixTarget);
			if (!s.secondaryHeld || target == null || !target.isAlive() || target.distanceTo(player) > HOTFIX_RANGE + 1) {
				s.hotfixChannel = -1;
				s.hotfixTarget = null;
				SombraSounds.stop(player.level(), "hotfix.channel", "hotfix.channel_3p", "hack.channel", "hack.channel_3p");
				return;
			}
			if (++s.hotfixChannel >= Ow.ticks(HOTFIX_CAST)) {
				s.hotfixChannel = -1;
				s.hotfixTarget = null;
				s.weaponRecovery = Math.max(s.weaponRecovery, Ow.ticks(HOTFIX_RECOVERY));
				SombraSounds.split(player, "hotfix.end", player.getEyePosition(), 70);
				if (target instanceof LivingEntity ally) {
					s.hotfixCharges--;
					HotfixHeal.start(player, ally);
					dev.overcraft.hero.Voice.say(player, "hotfix", 1);
				} else {
					dev.overcraft.hero.Voice.say(player, "hack", 1);
					// hacking an enemy deployable: no charge spent, then 6 s before she can hack again (614C)
					Hack.deployable(target, player);
					s.hackLockoutUntil = player.level().getGameTime() + Ow.ticks(HOTFIX_HACK_LOCKOUT);
					if (HeroManager.hasPerk(player, Perk.SOMBRA_DATA_PACKET)) {
						int had = s.hotfixCharges;
						dataPacket(s);
						if (s.hotfixCharges > had) {
							SombraSounds.own(player, "hotfix.data_packet");
						}
					}
				}
			}
			return;
		}
		if (!pressed || s.weaponRecovery > 0 || s.primaryReload >= 0) {
			return;
		}
		net.minecraft.world.entity.Entity target = s.hotfixCharges > 0
			? dev.overcraft.hero.Targeting.allyInView(player, HOTFIX_RANGE, HOTFIX_CONE, true) : null;
		if (target == null && player.level().getGameTime() >= s.hackLockoutUntil) {
			target = deployableInView(player);
		}
		if (target == null) {
			return;
		}
		s.hotfixTarget = target.getId();
		s.hotfixChannel = 0;
		boolean hack = !(target instanceof LivingEntity);
		SombraSounds.split(player, "hotfix.cast", player.getEyePosition(), 70);
		SombraSounds.own(player, hack ? "hack.channel" : "hotfix.channel");
		SombraSounds.at(player.level(), hack ? "hack.channel_3p" : "hotfix.channel_3p", player.getEyePosition(), 50);
		// its strands from her left forearm to them over the channel (7C6E.. to an ally, 1AC7.. to a hacked object)
		Effects.play(player.level(), "sombra_strands", player.position(), Vec3.ZERO, target,
			player.getId() * 2 + (target instanceof LivingEntity ? 0 : 1), player);
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
		// each shot one shot's worth of the stream (5DCC); its tail once the burst ends (tick: fire_end)
		SombraSounds.split(player, "pistol.fire", player.getEyePosition(), 200);
		s.sombraFiring = true;
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
		net.minecraft.world.phys.BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 block = blockHit.getLocation();
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
		// the muzzle (hp 0006), the tracer to where the shot ended, the hit (1999, 1998, 010F)
		Vec3 muzzle = fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Vec3 stop = best != null ? bestAt : block;
		Effects.play(level, "sombra_fire", muzzle, player.getViewVector(1.0f), player, 0, player);
		SombraSounds.at(level, "pistol.shot", muzzle, 60);
		Effects.play(level, "sombra_tracer", muzzle, stop.subtract(muzzle), player, 0, player);
		if (best != null) {
			Effects.play(level, "sombra_hit", bestAt, Vec3.ZERO, best, 0, player);
			SombraSounds.own(player, "pistol.hit");
		} else if (blockHit.getType() == HitResult.Type.BLOCK) {
			Effects.play(level, "sombra_hit_block", block, Vec3.atLowerCornerOf(blockHit.getDirection().getUnitVec3i()), null, 0, player);
			SombraSounds.at(level, "pistol.hit_block", block, 32);
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

	/** Her pistol's muzzle (hp 0006) from her eyes in her look's frame: ours until measured on her model (Ana's way). */
	private static final double MUZZLE_RIGHT = 0.22, MUZZLE_UP = -0.18, MUZZLE_AHEAD = 0.55;

	/** The head is the top of the box, as for Kiriko's kunai and Ana's darts (ours: 0.25 m under the eyes and up). */
	private static final double HEAD_DEPTH = 0.25;

	/** {@code dir} turned by an angle up to {@code degrees}, evenly over the cone's disc. */
	public static Vec3 cone(Vec3 dir, double degrees, net.minecraft.util.RandomSource random) {
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
		return dev.overcraft.hero.Combat.hitscan(owner);
	}

	/** R (not with a full clip or mid-reload), or the clip empty: 1.2 s, the clip full 0.48 s in. */
	private static void startReload(ServerPlayer player, HeroState s) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= PISTOL_AMMO) {
			return;
		}
		s.primaryReload = 0;
		Combat.animate(player, "reload");
		// the reload's sounds are spread over its 1.2 s in the file (sombra.txt)
		SombraSounds.split(player, "reload", player.position(), 45);
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
