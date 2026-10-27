package dev.overcraft.kiriko;

import dev.overcraft.Ow;
import dev.overcraft.entity.KunaiEntity;
import dev.overcraft.entity.OfudaEntity;
import dev.overcraft.entity.SuzuEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.hero.Targeting;
import dev.overcraft.hero.Voice;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import dev.overcraft.registry.OcSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.kiriko.KirikoStats.*;

/**
 * Kiriko, server side. LMB kunai, RMB (hold) Healing Ofuda, Shift Swift Step, E Protection Suzu, Q Kitsune Rush,
 * R reload. The two weapons share one recovery (one hand throws at a time); the Ofuda reloads by itself whenever
 * it isn't being thrown, and throwing it cancels a kunai reload that hasn't given the ammo back yet.
 */
public final class Kiriko implements HeroKit {
	private static final double OFUDA_INTERVAL = 20.0 / OFUDA_THROWS_PER_SECOND;
	/** Sideways offset of each hand's talisman from the eyes (blocks). */
	private static final double OFUDA_HAND_OFFSET = 0.25;

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = KUNAI_AMMO;
		s.secondaryAmmo = OFUDA_AMMO;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double attack = Combat.attackRate(player);
		double cooldown = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - cooldown);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - cooldown);

		// climbing a wall interrupts the kunai reload (the owner, #42); an empty clip starts again once she's off the wall
		if (s.wallClimbing && s.primaryReload >= 0) {
			s.primaryReload = -1;
			Combat.animate(player, "reload_cancel");
		} else if (!s.wallClimbing && s.primaryAmmo == 0 && s.primaryReload < 0) {
			startPrimaryReload(player, s);
		}
		if (s.primaryReload >= 0) {
			s.primaryReload += attack;
			if (!s.primaryAmmoRestored && s.primaryReload >= Ow.ticks(KUNAI_RELOAD_AMMO_AT)) {
				s.primaryAmmo = KUNAI_AMMO;
				s.primaryAmmoRestored = true;
			}
			if (s.primaryReload >= Ow.ticks(KUNAI_RELOAD)) {
				s.primaryReload = -1;
			}
		}
		if (s.secondaryReload >= 0) {
			s.secondaryReload += attack;
			if (s.secondaryReload >= Ow.ticks(OFUDA_RELOAD)) {
				s.secondaryAmmo = OFUDA_AMMO;
				s.secondaryReload = -1;
			}
		}

		if (s.castTicks >= 0 && --s.castTicks < 0) {
			finishSwiftStep(player, s);
		}

		s.weaponRecovery -= attack;
		boolean fired = false;
		if (controls && s.weaponRecovery <= 0) {
			if (s.primaryHeld && s.primaryAmmo > 0 && (s.primaryReload < 0 || s.primaryAmmoRestored)) {
				s.primaryReload = -1; // throwing again ends the rest of the reload animation
				throwKunai(player);
				s.primaryAmmo--;
				s.weaponRecovery += Ow.ticks(KUNAI_RECOVERY);
				fired = true;
				if (s.primaryAmmo == 0) {
					startPrimaryReload(player, s);
				}
			} else if (s.secondaryHeld && s.secondaryAmmo >= OFUDA_PER_THROW
				&& (s.primaryReload < 0 || s.primaryAmmoRestored || HeroManager.options(player).ofudaCancelsReload())) {
				// throwing ofuda interrupts a kunai reload (Overwatch's "Healing Ofuda cancels reload", on by default;
				// off, the ofuda wait for it)
				if (s.primaryReload >= 0 && !s.primaryAmmoRestored) {
					s.primaryReload = -1;
				}
				// and the refill they start when let go: the ofuda still in hand are thrown at once (ours: the wiki only
				// says they reload when not in use; waiting out the refill read as all of them gone, #12)
				s.secondaryReload = -1;
				throwOfuda(player);
				s.secondaryAmmo -= OFUDA_PER_THROW;
				s.weaponRecovery += OFUDA_INTERVAL;
				fired = true;
				if (s.secondaryAmmo < OFUDA_PER_THROW) {
					startSecondaryReload(player, s);
				}
			}
		}
		if (!fired && s.weaponRecovery < 0) {
			s.weaponRecovery = 0;
		}
		if (!s.secondaryHeld && s.secondaryAmmo < OFUDA_AMMO && s.secondaryReload < 0) {
			startSecondaryReload(player, s);
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			startSwiftStep(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && s.ability2Cooldown <= 0) {
			throwSuzu(player);
			Voice.say(player, "suzu", 1);
			s.ability2Cooldown = Ow.ticks(SUZU_COOLDOWN);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && s.ultActive == 0 && s.ultCharge >= ULT_COST) {
			KitsuneRush.start(player);
			Combat.animate(player, "ult_cast");
			s.ultCharge = 0;
			s.ultActive = Ow.ticks(RUSH_DURATION);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0) {
			if (s.primaryAmmo < KUNAI_AMMO && s.primaryReload < 0) {
				startPrimaryReload(player, s);
			}
			if (s.secondaryAmmo < OFUDA_AMMO && s.secondaryReload < 0) {
				startSecondaryReload(player, s);
			}
		}
	}

	private static void startPrimaryReload(ServerPlayer player, HeroState s) {
		s.primaryReload = 0;
		s.primaryAmmoRestored = false;
		Combat.animate(player, "reload");
		HeroSounds.split(player, OcSounds.KIRIKO_KUNAI_RELOAD, OcSounds.KIRIKO_KUNAI_RELOAD_3P, player.position());
	}

	/**
	 * The ofuda refill is silent, with no animation of its own: Overwatch has neither (its only reload clip draws
	 * kunai; the volley's end, throw_ofuda_end, shows the new ofuda appear). Playing the kunai reload here looked like
	 * the ofuda all thrown at once (#12).
	 */
	private static void startSecondaryReload(ServerPlayer player, HeroState s) {
		s.secondaryReload = 0;
	}

	private static void throwKunai(ServerPlayer player) {
		ServerLevel level = player.level();
		KunaiEntity kunai = new KunaiEntity(player, level);
		kunai.launch(player.getEyePosition(), player.getViewVector(1.0f), Ow.speed(KUNAI_SPEED));
		level.addFreshEntity(kunai);
		Combat.animate(player, "throw_kunai");
		HeroSounds.split(player, OcSounds.KIRIKO_KUNAI_THROW, OcSounds.KIRIKO_KUNAI_THROW_3P, player.getEyePosition());
	}

	private static void throwOfuda(ServerPlayer player) {
		// the cone narrows with the player's Healing Ofuda sensitivity (an Overwatch option)
		LivingEntity target = Targeting.allyInView(player, OFUDA_SEEK_RANGE,
			OFUDA_LOCK_HALF_ANGLE * HeroManager.options(player).ofudaSensitivity(), true);
		launchOfuda(player, target);
		Combat.animate(player, "throw_ofuda");
		HeroSounds.split(player, OcSounds.KIRIKO_OFUDA_THROW, OcSounds.KIRIKO_OFUDA_THROW_3P, player.getEyePosition());
	}

	/**
	 * Fortune Teller (perk): a kunai hit sends two Healing Ofuda to an ally in front of her, on top of her own volleys
	 * (no ammo spent, no throw of her own).
	 */
	public static void fortuneTeller(ServerPlayer player) {
		if (!HeroManager.hasPerk(player, Perk.KIRIKO_FORTUNE_TELLER)) {
			return;
		}
		LivingEntity target = Targeting.allyInView(player, OFUDA_SEEK_RANGE, FORTUNE_TELLER_HALF_ANGLE, true);
		if (target != null) {
			launchOfuda(player, target);
		}
	}

	/** One pair of talismans, one from each hand, homing on the target when there is one. */
	private static void launchOfuda(ServerPlayer player, @Nullable LivingEntity target) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		for (int i = 0; i < OFUDA_PER_THROW; i++) {
			double offset = (i == 0 ? -1 : 1) * OFUDA_HAND_OFFSET;
			OfudaEntity ofuda = new OfudaEntity(player, level, target);
			ofuda.launch(eye.add(side.scale(offset)).add(0, -0.1, 0), look, Ow.speed(OFUDA_SPEED));
			level.addFreshEntity(ofuda);
		}
	}

	private static void throwSuzu(ServerPlayer player) {
		ServerLevel level = player.level();
		SuzuEntity suzu = new SuzuEntity(player, level);
		suzu.launch(player.getEyePosition(), player.getViewVector(1.0f), Ow.speed(SUZU_SPEED));
		level.addFreshEntity(suzu);
		Combat.animate(player, "throw_suzu");
		HeroSounds.split(player, OcSounds.KIRIKO_SUZU_THROW, OcSounds.KIRIKO_SUZU_THROW_3P, player.getEyePosition());
	}

	private static void startSwiftStep(ServerPlayer player, HeroState s) {
		if (s.ability1Cooldown > 0 || s.castTicks >= 0) {
			return;
		}
		LivingEntity target = Targeting.allyInView(player, SWIFT_STEP_RANGE,
			SWIFT_STEP_HALF_ANGLE * HeroManager.options(player).swiftStepSensitivity(), false);
		if (target == null) {
			// no ally in view: nothing happens, no cooldown, just the "not now" click (as in Overwatch)
			HeroSounds.toOwner(player, OcSounds.KIRIKO_ABILITY_DENIED);
			return;
		}
		s.castTicks = Ow.ticks(SWIFT_STEP_CAST);
		s.castTarget = target.getUUID();
		Combat.animate(player, "swift_step");
		// her player hears the hand sign, everyone else her departure (the sound research: audiences 1 and 2)
		HeroSounds.split(player, OcSounds.KIRIKO_SWIFT_STEP_CAST, OcSounds.KIRIKO_SWIFT_STEP_DEPART, player.position());
	}

	private static void finishSwiftStep(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		Entity entity = s.castTarget == null ? null : level.getEntity(s.castTarget);
		s.castTarget = null;
		if (!(entity instanceof LivingEntity target) || !Allies.isAlly(player, target)
			|| target.distanceTo(player) > SWIFT_STEP_RANGE + 5) {
			return; // the target died, left or got out of range during the cast
		}
		Vec3 from = player.position();
		Vec3 arrive = beside(player, target, from);
		if (arrive == null) {
			// nowhere she fits by the ally (in a tunnel she can't stand in, half in a wall): she stays, nothing spent
			// (landing on them anyway put her inside the blocks, suffocating: found playing)
			HeroSounds.toOwner(player, OcSounds.KIRIKO_ABILITY_DENIED);
			return;
		}
		Effects.play(level, "swift_step_depart", from, player);
		player.teleportTo(arrive.x, arrive.y, arrive.z);
		player.resetFallDistance();
		Combat.phase(player, SWIFT_STEP_PHASED);
		s.ability1Cooldown = Ow.ticks(SWIFT_STEP_COOLDOWN);
		if (s.has(Perk.KIRIKO_READY_STEP)) {
			// Ready Step (perk): faster throws and reloads for a moment after arriving
			s.attackBuff = READY_STEP_ATTACK;
			s.attackBuffUntil = Combat.now(player) + Ow.ticks(READY_STEP_TIME);
		}
		Effects.play(level, "swift_step_arrive", arrive, player);
		// to everyone else, out to Overwatch's 70 m (volume 4.4 = 70 / 16 blocks); not to her: her own cast whoosh, which
		// follows her, covers the whole step (the game plays her nothing else: #17, sounds-round8.md)
		level.playSound(player, arrive.x, arrive.y, arrive.z, OcSounds.KIRIKO_SWIFT_STEP_ARRIVE, SoundSource.PLAYERS, 4.4f, 1);
		// her line once she's beside them ("I'm with you" before she got there made no sense: #17); the ally's hero may
		// have a line of their own ("Rolling with you" for Wrecking Ball: #13)
		Voice.say(player, "swift_step", 1, Voice.heroOf(target));
	}

	/**
	 * Where Swift Step lands: at the ally's side, shoulder to shoulder across the line she came along (the wiki: "at
	 * their side"; the owner, 2026-10-26: "Swift step should put you beside"), her right first, then her left. Overwatch
	 * puts her on the ally (its heroes overlap softly), which in Minecraft put the camera inside their head; landing
	 * between them and where she came from left the ally filling her screen. Failing room at either side, on the side
	 * she came from, then on them, then on top of them (ours); null when she fits nowhere there.
	 */
	private static @Nullable Vec3 beside(ServerPlayer player, LivingEntity target, Vec3 from) {
		Vec3 at = target.position();
		Vec3 back = new Vec3(from.x - at.x, 0, from.z - at.z);
		if (back.lengthSqr() > 1.0E-4) {
			Vec3 toward = back.normalize().scale(-1);
			// her right, facing the way she came toward them (y up: right = forward x up)
			Vec3 right = new Vec3(-toward.z, 0, toward.x);
			for (Vec3 side : new Vec3[] {right, right.scale(-1), back.normalize()}) {
				// 0.5 m clear of the ally (ours); closer only when there's no room
				for (double clear : new double[] {0.5, 0.2}) {
					double gap = (target.getBbWidth() + player.getBbWidth()) / 2 + clear;
					Vec3 spot = at.add(side.scale(gap));
					if (fits(player, spot)) {
						return spot;
					}
				}
			}
		}
		for (Vec3 spot : new Vec3[] {at, at.add(0, target.getBbHeight(), 0)}) {
			if (fits(player, spot)) {
				return spot;
			}
		}
		return null;
	}

	/** She can stand at spot: no blocks or solid entities in her box. */
	private static boolean fits(ServerPlayer player, Vec3 spot) {
		return player.level().noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(spot));
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float primaryReload = s.primaryReload >= 0 && !s.primaryAmmoRestored
			? (float) Math.min(1, s.primaryReload / Ow.ticks(KUNAI_RELOAD_AMMO_AT)) : -1;
		float secondaryReload = s.secondaryReload >= 0 ? (float) Math.min(1, s.secondaryReload / Ow.ticks(OFUDA_RELOAD)) : -1;
		// during Swift Step's hand seal, the time until it's ready again: the rest of the seal and the whole cooldown, so
		// the HUD shows the box as just used (coral) from the key press, as Overwatch does (the seventh critique)
		float step = s.castTicks >= 0 ? (float) (SWIFT_STEP_COOLDOWN + (s.castTicks + 1) / 20.0) : (float) (s.ability1Cooldown / 20.0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, ULT_COST, s.ultActive > 0,
			s.primaryAmmo, primaryReload, s.secondaryAmmo, secondaryReload,
			step, (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero, s.onFire,
			Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk), 0, 0, 0, 0);
	}
}
