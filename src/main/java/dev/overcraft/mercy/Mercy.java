package dev.overcraft.mercy;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.BlasterShotEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.registry.OcSounds;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.hero.Voice;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static dev.overcraft.mercy.MercyStats.*;

/**
 * Mercy: her kit on the server, from her game data (E:\Overwatch Archive\research\mercy\mercy-map.md) and the public
 * numbers (tools/reference/mercy.md; plan: tools/reference/mercy-plan.md). Her movement (Guardian Angel's flight and
 * launches, Angelic Descent, Valkyrie's flight) is the local player's (OwMovement); here are its states and her abilities.
 * Staff: left button heals, right boosts (one beam: switching keeps the target). Blaster: left button fires. 1 / 2 or
 * the scroll wheel: the staff / the blaster. Shift: Guardian Angel (Jump launches, Crouch launches up). E: Resurrect.
 * R: Flash Heal (the blaster out: reload). Q: Valkyrie. Space held in the air: Angelic Descent.
 */
public final class Mercy implements HeroKit {
	/** The synced state's flags (HeroManager.MERCY [0], and the sync's mode). */
	public static final int FLAG_BLASTER = 1, FLAG_GA = 2, FLAG_GA_LAUNCH = 4, FLAG_VALKYRIE = 8, FLAG_VALKYRIE_CAST = 16,
		FLAG_RESURRECT = 32, FLAG_FLASH = 64, FLAG_SWAP = 128;
	public static final int BEAM_NONE = 0, BEAM_HEAL = 1, BEAM_BOOST = 2;
	public static final int GA_NONE = 0, GA_FLY = 1, GA_LAUNCHED = 2;
	public static final int ULT_NONE = 0, ULT_CAST = 1, ULT_ON = 2;
	/** Guardian Angel picks the ally nearest the crosshair within this angle of it (degrees, ours: its sensitivity isn't read). */
	private static final double GA_AIM = 25;
	/** Resurrect's soul tracker: a 60° cone round the crosshair (her data's 00CA). */
	private static final double RES_AIM = 30;

	private static final Identifier RES_SLOW_ID = Overcraft.id("mercy_resurrect");

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = BLASTER_AMMO;
		s.primaryReload = -1;
		s.blasterOut = false;
		s.flashCharges = flashMax(player);
		s.ultPhase = ULT_NONE;
		endBeam(s);
		s.gaPhase = GA_NONE;
		s.gaTarget = null;
		s.resSoul = null;
		s.flashCast = -1;
		publish(player, s);
	}

	/** Valkyrie is on (after its cast): she flies. */
	public static boolean flying(HeroState s) {
		return s.ultPhase == ULT_ON;
	}

	public static boolean valkyrie(HeroState s) {
		return s.ultPhase != ULT_NONE;
	}

	private static boolean has(ServerPlayer player, Perk perk) {
		return HeroManager.hasPerk(player, perk);
	}

	private static int flashMax(ServerPlayer player) {
		return has(player, Perk.MERCY_DOUBLE_DOSE) ? 2 : 1;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double attack = Combat.attackRate(player);
		double cooldown = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - cooldown);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - cooldown);
		flashRecharge(player, s, cooldown);
		if (s.swapTicks > 0) {
			s.swapTicks--;
		}
		// put away, the blaster reloads itself (W: 1.4 s holstered)
		if (!s.blasterOut && s.primaryAmmo < BLASTER_AMMO && ++s.holsterTicks >= Ow.ticks(BLASTER_HOLSTER_RELOAD)) {
			s.primaryAmmo = BLASTER_AMMO;
			s.primaryReload = -1;
		}

		int modeBefore = s.beamTarget == null ? BEAM_NONE : s.beamMode;
		boolean lockedBefore = s.beamTarget != null;
		if (!controls || !player.isAlive()) {
			// out of play (dead, Minecraft mode): what she was doing ends; Valkyrie's time runs on
			endBeam(s);
			beamSounds(player, s, modeBefore, lockedBefore);
			if (s.gaPhase != GA_NONE) {
				endGuardianAngel(s);
			}
			if (s.resSoul != null) {
				endResurrect(player, s, false);
			}
			s.flashCast = -1;
			valkyrieTimer(player, s);
			publish(player, s);
			return;
		}

		valkyrieTimer(player, s);
		guardianAngel(player, s);
		descent(player, s);
		resurrect(player, s);
		flashHeal(player, s);
		if (s.blasterOut) {
			endBeam(s);
			blaster(player, s, attack);
		} else {
			beam(player, s);
		}
		beamSounds(player, s, modeBefore, lockedBefore);
		publish(player, s);
	}

	/**
	 * The beam's sounds as its state changes this tick: the lock and its loss (hers only: no 3p copy in the data), each
	 * mode's start and end (hers, and everyone else's at her). The loops are the clients' (MercySounds).
	 */
	private static void beamSounds(ServerPlayer player, HeroState s, int modeBefore, boolean lockedBefore) {
		int modeNow = s.beamTarget == null ? BEAM_NONE : s.beamMode;
		boolean lockedNow = s.beamTarget != null;
		if (lockedNow && !lockedBefore) {
			HeroSounds.toOwner(player, OcSounds.MERCY_BEAM_LOCK);
		} else if (!lockedNow && lockedBefore) {
			HeroSounds.toOwner(player, OcSounds.MERCY_BEAM_UNLOCK);
		}
		if (modeNow == modeBefore) {
			return;
		}
		Vec3 at = player.getEyePosition();
		if (modeBefore == BEAM_HEAL) {
			HeroSounds.split(player, OcSounds.MERCY_HEAL_END, OcSounds.MERCY_HEAL_END_3P, at);
		} else if (modeBefore == BEAM_BOOST) {
			HeroSounds.split(player, OcSounds.MERCY_BOOST_END, OcSounds.MERCY_BOOST_END_3P, at);
		}
		if (modeNow == BEAM_HEAL) {
			HeroSounds.split(player, OcSounds.MERCY_HEAL_START, OcSounds.MERCY_HEAL_START_3P, at);
		} else if (modeNow == BEAM_BOOST) {
			HeroSounds.split(player, OcSounds.MERCY_BOOST_START, OcSounds.MERCY_BOOST_START_3P, at);
		}
	}

	// ---- the weapons ----

	/** Weapon keys (Overwatch's 1 / 2, the scroll wheel) and Guardian Angel's launches (Jump, Crouch). */
	public static void extra(ServerPlayer player, HeroState s, int extra) {
		if (!HeroManager.controls(player)) {
			return;
		}
		boolean wantBlaster = s.blasterOut;
		if ((extra & HeroInputPayload.EXTRA_STAFF) != 0) {
			wantBlaster = false;
		} else if ((extra & HeroInputPayload.EXTRA_BLASTER) != 0) {
			wantBlaster = true;
		} else if ((extra & HeroInputPayload.EXTRA_SWAP) != 0) {
			wantBlaster = !s.blasterOut;
		}
		if (wantBlaster != s.blasterOut && s.resSoul == null && s.flashCast < 0 && s.ultPhase != ULT_CAST) {
			s.blasterOut = wantBlaster;
			s.swapTicks = Ow.ticks(SWAP);
			s.holsterTicks = 0;
			if (wantBlaster) {
				endBeam(s);
			} else {
				s.primaryReload = -1;
			}
			Combat.animate(player, wantBlaster ? "draw_blaster" : "draw");
			HeroSounds.split(player, wantBlaster ? OcSounds.MERCY_SWAP_BLASTER : OcSounds.MERCY_SWAP_STAFF,
				wantBlaster ? OcSounds.MERCY_SWAP_BLASTER_3P : OcSounds.MERCY_SWAP_STAFF_3P, player.getEyePosition());
		}
		if (s.gaPhase == GA_FLY && (extra & (HeroInputPayload.EXTRA_JUMP | HeroInputPayload.EXTRA_CROUCH)) != 0) {
			// a launch: her client has already flung her (OwMovement); here its state, 1 s before the cooldown
			s.gaPhase = GA_LAUNCHED;
			s.gaTicks = Ow.ticks(valkyrie(s) ? GA_LAUNCH_STATE_VALKYRIE : GA_LAUNCH_STATE);
			s.gaTarget = null;
			s.gaSoul = null;
			s.softLanding = true;
			Combat.animate(player, (extra & HeroInputPayload.EXTRA_CROUCH) != 0 ? "ga_launch_up" : "ga_launch");
			Effects.play(player.level(), "mercy_ga_launch", player.position(), player);
			HeroSounds.split(player, OcSounds.MERCY_GA_LAUNCH, OcSounds.MERCY_GA_LAUNCH_3P, player.position());
		}
	}

	private static void blaster(ServerPlayer player, HeroState s, double attack) {
		boolean unlimited = flying(s);
		if (s.reloadHeld > 0) {
			s.reloadHeld--;
		}
		if (s.primaryAmmo == 0 && s.primaryReload < 0 && !unlimited) {
			startReload(player, s);
		}
		if (s.primaryReload >= 0) {
			s.primaryReload += attack;
			if (s.primaryReload >= Ow.ticks(BLASTER_RELOAD)) {
				// her data gives the ammo back at 1.4 s times a fraction it doesn't name (v6884): at the end, ours
				s.primaryAmmo = BLASTER_AMMO;
				s.primaryReload = -1;
			}
		}
		s.weaponRecovery -= attack;
		if (s.primaryHeld && s.swapTicks == 0 && s.weaponRecovery <= 0 && (s.primaryAmmo > 0 || unlimited)
			&& s.primaryReload < 0 && s.resSoul == null && s.ultPhase != ULT_CAST) {
			if (!unlimited) {
				s.primaryAmmo--;
			}
			s.weaponRecovery += Ow.ticks(1 / BLASTER_RATE);
			shoot(player, unlimited);
			Combat.animate(player, "fire_blaster");
			// 228D, the shot's own sound slot: one copy, everyone (her too) hears it at her
			player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), OcSounds.MERCY_BLASTER_FIRE,
				net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
		}
		if (s.weaponRecovery < 0) {
			s.weaponRecovery = 0;
		}
	}

	/** One blaster shot from by her eyes along the aim (the muzzle's flare is drawn at the blaster's hp 0006: MercyFx). */
	private static void shoot(ServerPlayer player, boolean valkyrie) {
		ServerLevel level = player.level();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		// ours: a little to her right and below the eyes, where she holds it (where her graph spawns it isn't decoded)
		Vec3 from = player.getEyePosition().add(side.scale(0.2)).add(0, -0.15, 0);
		BlasterShotEntity shot = new BlasterShotEntity(player, level);
		Vec3 aim = player.getEyePosition().add(look.scale(30)).subtract(from).normalize();
		shot.launch(from, aim, Ow.speed(valkyrie ? BLASTER_SPEED_VALKYRIE : BLASTER_SPEED));
		level.addFreshEntity(shot);
	}

	private static void startReload(ServerPlayer player, HeroState s) {
		s.primaryReload = 0;
		Combat.animate(player, "reload_blaster");
		HeroSounds.split(player, OcSounds.MERCY_RELOAD, OcSounds.MERCY_RELOAD_3P, player.getEyePosition());
	}

	// ---- Caduceus Staff ----

	private static void endBeam(HeroState s) {
		s.beamMode = BEAM_NONE;
		s.beamTarget = null;
		s.beamLost = 0;
	}

	private static double beamRange(HeroState s) {
		return flying(s) ? BEAM_RANGE_VALKYRIE : BEAM_RANGE;
	}

	/**
	 * The beam, each tick the staff is out. The left button heals, the right boosts; the one pressed last wins and
	 * switching keeps the target (BZ 2016-04-05). With no target it looks for one 12 times a second: the ally nearest the
	 * crosshair in a 40° cone and 15 m, in sight. Once locked she needn't look at them; out of sight or out of range it
	 * holds 1.3 s, then breaks. Letting go of both buttons ends it (Overwatch's default, Toggle Beam Connection off).
	 */
	private static void beam(ServerPlayer player, HeroState s) {
		int was = s.beamMode;
		int held = (s.primaryHeld ? 1 : 0) | (s.secondaryHeld ? 2 : 0);
		int fresh = held & ~s.beamHeldBefore;
		s.beamHeldBefore = held;
		int mode = s.beamMode;
		if ((fresh & 2) != 0) {
			mode = BEAM_BOOST;
		} else if ((fresh & 1) != 0) {
			mode = BEAM_HEAL;
		} else if (mode == BEAM_HEAL && !s.primaryHeld) {
			mode = s.secondaryHeld ? BEAM_BOOST : BEAM_NONE;
		} else if (mode == BEAM_BOOST && !s.secondaryHeld) {
			mode = s.primaryHeld ? BEAM_HEAL : BEAM_NONE;
		} else if (mode == BEAM_NONE && held != 0) {
			mode = s.primaryHeld ? BEAM_HEAL : BEAM_BOOST;
		}
		if (s.swapTicks > 0 || s.resSoul != null || s.ultPhase == ULT_CAST) {
			mode = BEAM_NONE;
		}
		if (mode == BEAM_NONE) {
			endBeam(s);
			return;
		}
		s.beamMode = mode;
		LivingEntity target = s.beamTarget == null ? null : player.level().getEntity(s.beamTarget) instanceof LivingEntity l ? l : null;
		if (target != null) {
			boolean ok = target.isAlive() && Allies.isAlly(player, target);
			boolean seen = ok && target.distanceTo(player) <= beamRange(s) && sees(player, target);
			s.beamLost = seen ? 0 : s.beamLost + 1;
			if (!ok || s.beamLost > Ow.ticks(BEAM_LINGER)) {
				target = null;
				s.beamTarget = null;
				s.beamLost = 0;
			}
		}
		if (target == null) {
			if (--s.beamSearch > 0) {
				return;
			}
			s.beamSearch = Math.max(1, (int) Math.round(20 / BEAM_SEARCH_PER_SECOND));
			target = beamTarget(player, s);
			if (target == null) {
				return;
			}
			s.beamTarget = target.getId();
			s.beamLost = 0;
			was = BEAM_NONE;
		}
		if (was != mode) {
			// her beam lines (00B9 heal, 00BA boost): now and then as it connects
			Voice.say(player, mode == BEAM_HEAL ? "beam_heal" : "beam_boost", 1);
			if (mode == BEAM_BOOST && target instanceof ServerPlayer ally) {
				Voice.say(ally, "damage_boosted", 1);
			}
		}
		List<LivingEntity> targets = new ArrayList<>();
		targets.add(target);
		if (flying(s)) {
			// Valkyrie: the beam branches from its target to every ally within 10 m of it, in its sight (W)
			for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(CHAIN_RADIUS))) {
				if (other != target && other != player && Allies.isAlly(player, other) && other.distanceTo(target) <= CHAIN_RADIUS
					&& target.hasLineOfSight(other)) {
					targets.add(other);
				}
			}
		} else if (mode == BEAM_BOOST && has(player, Perk.MERCY_CHAIN_BOOST)) {
			// Chain Boost: the boost links to a second ally near the target (the nearest: ours, the data's link not found)
			LivingEntity second = null;
			for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class,
				target.getBoundingBox().inflate(CHAIN_BOOST_RANGE))) {
				if (other != target && other != player && Allies.isAlly(player, other) && other.distanceTo(target) <= CHAIN_BOOST_RANGE
					&& target.hasLineOfSight(other) && (second == null || other.distanceTo(target) < second.distanceTo(target))) {
					second = other;
				}
			}
			if (second != null) {
				targets.add(second);
			}
		}
		s.beamChain = targets.subList(1, targets.size()).stream().map(Entity::getId).toList();
		long now = player.level().getGameTime();
		for (LivingEntity t : targets) {
			if (mode == BEAM_HEAL) {
				float healed = Combat.heal(t, (flying(s) ? HEAL_PER_SECOND_VALKYRIE : HEAL_PER_SECOND) / 20.0);
				if (healed > 0) {
					Combat.credit(player, healed);
					Combat.addFireForHealing(player, healed);
					// Medic: her beam's healing heals her too, for each ally (no ult charge for that part)
					Combat.heal(player, healed * MEDIC_SELF_HEAL);
				}
			} else {
				double boost = BOOST + (has(player, Perk.MERCY_CHAIN_BOOST) ? CHAIN_BOOST : 0);
				Boosts.boost(t, 1 + boost, now + 2, player.getUUID());
			}
		}
	}

	/** The beam's pick: the ally nearest the crosshair within the cone and range, in sight (her data's weights: M). */
	private static @Nullable LivingEntity beamTarget(ServerPlayer player, HeroState s) {
		double range = beamRange(s);
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))) {
			if (!Allies.isAlly(player, other) || other.distanceTo(player) > range) {
				continue;
			}
			double angle = angleTo(player, other.getBoundingBox().getCenter());
			if (angle > BEAM_CONE / 2 || !sees(player, other)) {
				continue;
			}
			// distance weighted 1, the angle 15 x 0.01 a degree (her graph 006B's scoring: M)
			double score = other.distanceTo(player) + angle * 0.15;
			if (score < bestScore) {
				best = other;
				bestScore = score;
			}
		}
		return best;
	}

	/** Degrees between her aim and the point. */
	private static double angleTo(ServerPlayer player, Vec3 point) {
		Vec3 to = point.subtract(player.getEyePosition());
		if (to.lengthSqr() < 1.0E-6) {
			return 0;
		}
		double dot = player.getViewVector(1.0f).dot(to.normalize());
		return Math.toDegrees(Math.acos(Math.clamp(dot, -1, 1)));
	}

	/** In her sight: their eyes or their middle seen from hers. */
	private static boolean sees(ServerPlayer player, LivingEntity other) {
		return player.hasLineOfSight(other) || clear(player, player.getEyePosition(), other.getBoundingBox().getCenter());
	}

	private static boolean clear(Entity from, Vec3 a, Vec3 b) {
		return from.level().clip(new net.minecraft.world.level.ClipContext(a, b, net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE, from)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
	}

	// ---- Guardian Angel ----

	private static double gaRange(ServerPlayer player, HeroState s) {
		double range = flying(s) ? GA_RANGE_VALKYRIE : GA_RANGE;
		return range * (has(player, Perk.MERCY_WINGED_REACH) ? 1 + WINGED_REACH : 1);
	}

	/** Where Guardian Angel takes her: the ally's middle, or the soul where it fell; null when it's gone. */
	public static @Nullable Vec3 gaDestination(ServerPlayer player, HeroState s) {
		if (s.gaSoul != null) {
			Souls.Soul soul = Souls.get(s.gaSoul);
			return soul == null ? null : soul.at.add(0, 0.5, 0);
		}
		Entity target = s.gaTarget == null ? null : player.level().getEntity(s.gaTarget);
		return target instanceof LivingEntity living && living.isAlive() ? living.position().add(0, living.getBbHeight() * 0.5, 0) : null;
	}

	/**
	 * Shift: to an ally within 30 m (60 in Valkyrie, +30% with Winged Reach), or a dead ally's soul, flown at 17 m/s (25)
	 * by her client. The beam's target first (W: "prefer beam target", the default), else the ally or soul nearest the
	 * crosshair, in sight. Shift again cancels; the cooldown starts when it ends.
	 */
	private static void startGuardianAngel(ServerPlayer player, HeroState s) {
		double range = gaRange(player, s);
		LivingEntity beam = s.beamTarget == null ? null : player.level().getEntity(s.beamTarget) instanceof LivingEntity l ? l : null;
		if (beam != null && beam.isAlive() && beam.distanceTo(player) <= range && sees(player, beam)) {
			begin(player, s, beam.getId(), null);
			return;
		}
		LivingEntity best = null;
		double bestAngle = GA_AIM;
		for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))) {
			if (!Allies.isAlly(player, other) || other.distanceTo(player) > range || other.distanceTo(player) < GA_ARRIVE) {
				continue;
			}
			double angle = angleTo(player, other.getBoundingBox().getCenter());
			if (angle < bestAngle && sees(player, other)) {
				best = other;
				bestAngle = angle;
			}
		}
		Souls.Soul bestSoul = null;
		for (Souls.Soul soul : Souls.near(player, range, SOUL_TIME)) {
			Vec3 at = soul.at.add(0, 0.5, 0);
			double angle = angleTo(player, at);
			if (angle < bestAngle && clear(player, player.getEyePosition(), at)) {
				bestSoul = soul;
				bestAngle = angle;
			}
		}
		if (bestSoul != null) {
			begin(player, s, null, bestSoul.id);
		} else if (best != null) {
			begin(player, s, best.getId(), null);
		}
	}

	private static void begin(ServerPlayer player, HeroState s, @Nullable Integer target, java.util.@Nullable UUID soul) {
		s.gaPhase = GA_FLY;
		s.gaTarget = target;
		s.gaSoul = soul;
		s.gaTicks = 0;
		s.gaMomentum = 0;
		s.softLanding = true;
		Combat.animate(player, "ga_start");
		Effects.play(player.level(), "mercy_ga_start", player.position(), player);
		HeroSounds.split(player, OcSounds.MERCY_GA_START, OcSounds.MERCY_GA_START_3P, player.position());
		Voice.say(player, "guardian_angel", 1);
	}

	private static void endGuardianAngel(HeroState s) {
		s.gaPhase = GA_NONE;
		s.gaTarget = null;
		s.gaSoul = null;
		s.ability1Cooldown = Ow.ticks(GA_COOLDOWN);
	}

	private static void guardianAngel(ServerPlayer player, HeroState s) {
		if (s.gaPhase == GA_FLY) {
			s.gaTicks++;
			s.gaMomentum = Math.min(1, s.gaTicks / (double) Ow.ticks(GA_MOMENTUM_FILL));
			Vec3 to = gaDestination(player, s);
			if (to == null || s.gaTicks > Ow.ticks(GA_LIMIT) || player.position().add(0, 0.9, 0).distanceTo(to) <= GA_ARRIVE) {
				endGuardianAngel(s);
			}
		} else if (s.gaPhase == GA_LAUNCHED && --s.gaTicks <= 0) {
			endGuardianAngel(s);
		}
		if (s.gaPhase != GA_NONE) {
			player.resetFallDistance();
		}
		if (s.softLanding && s.gaPhase == GA_NONE && !flying(s) && player.onGround()) {
			s.softLanding = false;
		}
	}

	/**
	 * Angelic Descent: Jump (or Crouch: Overwatch's option, on by default) held in the air caps her fall at 2 m/s; her
	 * client does the capping (OwMovement). A slow fall takes no fall damage: the distance fallen is cleared while it
	 * holds, so only a drop after letting go counts. Not in Valkyrie (she flies).
	 */
	private static void descent(ServerPlayer player, HeroState s) {
		var keys = player.getLastClientInput();
		if (!player.onGround() && !flying(s) && s.gaPhase == GA_NONE && (keys.jump() || keys.shift())
			&& player.getKnownMovement().y > -DESCENT_SPEED * 1.25) {
			player.resetFallDistance();
		}
		if (flying(s)) {
			player.resetFallDistance();
		}
	}

	// ---- Resurrect ----

	private static void startResurrect(ServerPlayer player, HeroState s) {
		Souls.Soul soul = null;
		double bestAngle = RES_AIM;
		for (Souls.Soul candidate : Souls.near(player, RES_RANGE, SOUL_TIME)) {
			double angle = angleTo(player, candidate.at.add(0, 0.5, 0));
			// close enough, she's on it whatever her aim (ours: a soul at her feet sits far below the crosshair)
			if (candidate.at.distanceTo(player.position()) < 1.5) {
				angle = 0;
			}
			if (angle <= bestAngle) {
				soul = candidate;
				bestAngle = angle;
			}
		}
		if (soul == null) {
			return;
		}
		s.resSoul = soul.id;
		s.castTicks = Ow.ticks(RES_CAST);
		endBeam(s);
		Combat.buff(player, Attributes.MOVEMENT_SPEED, RES_SLOW_ID, -RES_SLOW, RES_CAST);
		Combat.animate(player, s.blasterOut ? "resurrect_blaster" : "resurrect");
		Voice.say(player, "resurrect", 1);
		Effects.play(player.level(), "mercy_resurrect", soul.at, player);
		HeroSounds.split(player, OcSounds.MERCY_RES_CAST, OcSounds.MERCY_RES_CAST_3P, player.position());
	}

	private static void resurrect(ServerPlayer player, HeroState s) {
		if (s.resSoul == null) {
			return;
		}
		Souls.Soul soul = Souls.get(s.resSoul);
		// broken past 7 m from the soul (W), or with the soul gone (they respawned, left)
		if (soul == null || soul.at.distanceTo(player.position()) > RES_BREAK) {
			endResurrect(player, s, false);
			return;
		}
		if (--s.castTicks <= 0) {
			ServerLevel level = player.level().getServer().getLevel(soul.level);
			boolean done = level != null && Souls.resurrect(level, soul, 1);
			endResurrect(player, s, done);
			if (done) {
				Effects.play(level, "mercy_resurrected", soul.at, player);
				HeroSounds.toOwner(player, OcSounds.MERCY_RES_DONE);
			}
		}
	}

	private static void endResurrect(ServerPlayer player, HeroState s, boolean done) {
		s.resSoul = null;
		s.castTicks = -1;
		Combat.buff(player, Attributes.MOVEMENT_SPEED, RES_SLOW_ID, 0, 0);
		s.ability2Cooldown = Ow.ticks(RES_COOLDOWN);
		// Angelic Resurrection: overhealth after the cast, even an interrupted one (W)
		if (has(player, Perk.MERCY_ANGELIC_RESURRECTION)) {
			Combat.overhealth(player, ANGELIC_RESURRECTION, ANGELIC_RESURRECTION_TIME);
		}
	}

	// ---- Flash Heal ----

	private static void flashRecharge(ServerPlayer player, HeroState s, double cooldown) {
		int max = flashMax(player);
		if (s.flashCharges >= max) {
			s.ability3Cooldown = 0;
			s.flashCharges = Math.min(s.flashCharges, max);
			return;
		}
		s.ability3Cooldown = Math.max(0, s.ability3Cooldown - cooldown);
		if (s.ability3Cooldown <= 0) {
			s.flashCharges++;
			if (s.flashCharges < max) {
				s.ability3Cooldown = Ow.ticks(FLASH_COOLDOWN);
			}
		}
	}

	private static @Nullable LivingEntity staffTarget(ServerPlayer player, HeroState s) {
		LivingEntity target = s.beamTarget == null ? null : player.level().getEntity(s.beamTarget) instanceof LivingEntity l ? l : null;
		return target != null && target.isAlive() && Allies.isAlly(player, target) ? target : null;
	}

	private static void startFlashHeal(ServerPlayer player, HeroState s) {
		if (s.flashCharges <= 0 || s.flashCast >= 0 || s.resSoul != null || staffTarget(player, s) == null) {
			return;
		}
		if (s.flashCharges == flashMax(player)) {
			s.ability3Cooldown = Ow.ticks(FLASH_COOLDOWN);
		}
		s.flashCharges--;
		s.flashCast = Ow.ticks(FLASH_CAST);
		Combat.animate(player, flying(s) ? "flash_heal_flying" : "flash_heal");
		Effects.play(player.level(), "mercy_flash_cast", player.position(), player);
		HeroSounds.split(player, OcSounds.MERCY_FLASH_CAST, OcSounds.MERCY_FLASH_CAST_3P, player.getEyePosition());
	}

	/** Flash Heal's cast ends: 60 to the staff's target (50 with Double Dose), doubled when it's below half health. */
	private static void flashHeal(ServerPlayer player, HeroState s) {
		if (s.flashCast < 0 || --s.flashCast > 0) {
			return;
		}
		s.flashCast = -1;
		LivingEntity target = staffTarget(player, s);
		if (target == null) {
			return;
		}
		double amount = FLASH_HEAL - (has(player, Perk.MERCY_DOUBLE_DOSE) ? DOUBLE_DOSE_LESS : 0);
		if (target.getHealth() < target.getMaxHealth() * FLASH_LOW) {
			amount *= FLASH_LOW_FACTOR;
		}
		float healed = Combat.heal(target, amount);
		if (healed > 0) {
			Combat.credit(player, healed);
			Combat.addFireForHealing(player, healed);
		}
		// on the ally's chest; the owner is her (her staff head starts its band)
		Effects.play(player.level(), "mercy_flash_heal", target.position().add(0, target.getBbHeight() * 0.6, 0), player);
		// 64E1's C2C6, played on the target: everyone hears it there
		player.level().playSound(null, target.getX(), target.getEyeY(), target.getZ(), OcSounds.MERCY_FLASH_HEAL,
			net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
	}

	// ---- Valkyrie ----

	private static void startValkyrie(ServerPlayer player, HeroState s) {
		s.ultPhase = ULT_CAST;
		s.ultTimer = Ow.ticks(VALKYRIE_CAST);
		s.ultCharge = Combat.afterUlt(Hero.MERCY, s.ultCharge);
		s.ultActive = Ow.ticks(VALKYRIE_CAST + VALKYRIE_TIME);
		// it resets Guardian Angel's cooldown (BZ 2022-12-06)
		s.ability1Cooldown = 0;
		Combat.animate(player, s.blasterOut ? "ult_cast_blaster" : "ult_cast");
		// "Heroes never die!" for allies and her, the enemies' line for them (her data's 08CB / 08CA)
		dev.overcraft.hero.HeroSounds.ultLine(player, dev.overcraft.registry.OcSounds.MERCY_ULT_FRIENDLY,
			dev.overcraft.registry.OcSounds.MERCY_ULT_ENEMY, 1);
		Effects.play(player.level(), "mercy_valkyrie_cast", player.position(), player);
		HeroSounds.split(player, OcSounds.MERCY_VALKYRIE_CAST, OcSounds.MERCY_VALKYRIE_CAST_3P, player.position());
	}

	private static void valkyrieTimer(ServerPlayer player, HeroState s) {
		if (s.ultPhase == ULT_CAST && --s.ultTimer <= 0) {
			s.ultPhase = ULT_ON;
		} else if (s.ultPhase == ULT_ON && s.ultActive <= 0) {
			s.ultPhase = ULT_NONE;
			s.softLanding = true;
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		boolean casting = s.resSoul != null || s.ultPhase == ULT_CAST;
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && !casting) {
			if (s.gaPhase == GA_FLY) {
				endGuardianAngel(s);
			} else if (s.gaPhase == GA_NONE && s.ability1Cooldown <= 0) {
				startGuardianAngel(player, s);
			}
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && !casting && s.ability2Cooldown <= 0 && s.flashCast < 0) {
			startResurrect(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && !casting) {
			if (s.blasterOut) {
				// R with the blaster out reloads it (ours: Flash Heal holds Overwatch's Reload button and needs the staff's target)
				if (s.primaryAmmo < BLASTER_AMMO && s.primaryReload < 0 && !flying(s)) {
					startReload(player, s);
				}
			} else {
				startFlashHeal(player, s);
			}
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && s.ultPhase == ULT_NONE && s.ultCharge >= ULT_COST && !casting) {
			startValkyrie(player, s);
		}
	}

	// ---- what everyone sees ----

	/** Her state for every client (HeroManager.MERCY), set when it changes. */
	private static void publish(ServerPlayer player, HeroState s) {
		List<Integer> state = new ArrayList<>();
		state.add(flags(s));
		// Guardian Angel's target: an entity, or a soul (-2) at the point that follows (cm: no body to fly to on clients)
		Vec3 dest = s.gaPhase == GA_FLY ? gaDestination(player, s) : null;
		state.add(s.gaPhase != GA_FLY ? -1 : s.gaSoul != null ? -2 : s.gaTarget != null ? s.gaTarget : -1);
		state.add(dest == null ? 0 : (int) Math.round(dest.x * 100));
		state.add(dest == null ? 0 : (int) Math.round(dest.y * 100));
		state.add(dest == null ? 0 : (int) Math.round(dest.z * 100));
		state.add(s.beamTarget == null ? BEAM_NONE : s.beamMode);
		state.add(s.beamTarget == null ? -1 : s.beamTarget);
		if (s.beamTarget != null) {
			state.addAll(s.beamChain);
		}
		if (!state.equals(player.getAttached(HeroManager.MERCY))) {
			player.setAttached(HeroManager.MERCY, state);
		}
	}

	private static int flags(HeroState s) {
		return (s.blasterOut ? FLAG_BLASTER : 0) | (s.gaPhase == GA_FLY ? FLAG_GA : 0) | (s.gaPhase == GA_LAUNCHED ? FLAG_GA_LAUNCH : 0)
			| (s.ultPhase == ULT_ON ? FLAG_VALKYRIE : 0) | (s.ultPhase == ULT_CAST ? FLAG_VALKYRIE_CAST : 0)
			| (s.resSoul != null ? FLAG_RESURRECT : 0) | (s.flashCast >= 0 ? FLAG_FLASH : 0) | (s.swapTicks > 0 ? FLAG_SWAP : 0);
	}

	/** Leaving Mercy (another hero, leaving the world): her synced state goes. */
	public static void clear(ServerPlayer player, HeroState s) {
		Combat.buff(player, Attributes.MOVEMENT_SPEED, RES_SLOW_ID, 0, 0);
		player.removeAttached(HeroManager.MERCY);
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float reload = s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(BLASTER_RELOAD)) : -1;
		float left = s.ultPhase == ULT_ON ? s.ultActive / 20f : s.resSoul != null ? s.castTicks / 20f : 0;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, ULT_COST, s.ultPhase != ULT_NONE,
			s.primaryAmmo, reload, s.flashCharges, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), (float) (s.ability3Cooldown / 20.0), flags(s), left, (float) s.gaMomentum);
	}
}
