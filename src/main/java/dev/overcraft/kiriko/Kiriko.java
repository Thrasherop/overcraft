package dev.overcraft.kiriko;

import dev.overcraft.Ow;
import dev.overcraft.entity.KunaiEntity;
import dev.overcraft.entity.OfudaEntity;
import dev.overcraft.entity.SuzuEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.HeroState;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
			} else if (s.secondaryHeld && s.secondaryAmmo >= OFUDA_PER_THROW && s.secondaryReload < 0) {
				if (s.primaryReload >= 0 && !s.primaryAmmoRestored) {
					s.primaryReload = -1;
				}
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
			Voice.maybe(player, s, OcSounds.KIRIKO_VOICE_SUZU, 0.5f);
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
	 * The ofuda refill is silent: Overwatch has no sound of its own for it. One reload animation serves both weapons:
	 * played here only when the kunai aren't reloading already.
	 */
	private static void startSecondaryReload(ServerPlayer player, HeroState s) {
		s.secondaryReload = 0;
		if (s.primaryReload < 0) {
			Combat.animate(player, "reload");
		}
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
		ServerLevel level = player.level();
		LivingEntity target = Targeting.allyInView(player, OFUDA_SEEK_RANGE, OFUDA_LOCK_HALF_ANGLE, true);
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
		Combat.animate(player, "throw_ofuda");
		HeroSounds.split(player, OcSounds.KIRIKO_OFUDA_THROW, OcSounds.KIRIKO_OFUDA_THROW_3P, player.getEyePosition());
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
		LivingEntity target = Targeting.allyInView(player, SWIFT_STEP_RANGE, SWIFT_STEP_HALF_ANGLE, false);
		if (target == null) {
			// no ally in view: nothing happens, no cooldown, just the "not now" click (as in Overwatch)
			HeroSounds.toOwner(player, OcSounds.KIRIKO_ABILITY_DENIED, player.getEyePosition());
			return;
		}
		s.castTicks = Ow.ticks(SWIFT_STEP_CAST);
		s.castTarget = target.getUUID();
		Combat.animate(player, "swift_step");
		// her player hears the hand sign, everyone else her departure (the sound research: audiences 1 and 2)
		HeroSounds.split(player, OcSounds.KIRIKO_SWIFT_STEP_CAST, OcSounds.KIRIKO_SWIFT_STEP_DEPART, player.position());
		Voice.maybe(player, s, OcSounds.KIRIKO_VOICE_SWIFT_STEP, 0.6f);
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
		Effects.play(level, "swift_step_depart", from);
		Vec3 arrive = beside(player, target, from);
		player.teleportTo(arrive.x, arrive.y, arrive.z);
		player.resetFallDistance();
		Combat.phase(player, SWIFT_STEP_PHASED);
		s.ability1Cooldown = Ow.ticks(SWIFT_STEP_COOLDOWN);
		Effects.play(level, "swift_step_arrive", arrive);
		level.playSound(null, arrive.x, arrive.y, arrive.z, OcSounds.KIRIKO_SWIFT_STEP_ARRIVE, SoundSource.PLAYERS, 1, 1);
	}

	/**
	 * Where Swift Step lands: Overwatch puts her on the ally (its heroes overlap softly); in Minecraft that put the
	 * camera inside their head, so just beside them on the side she came from, when there's room (ours).
	 */
	private static Vec3 beside(ServerPlayer player, LivingEntity target, Vec3 from) {
		Vec3 at = target.position();
		Vec3 back = new Vec3(from.x - at.x, 0, from.z - at.z);
		if (back.lengthSqr() > 1.0E-4) {
			// half a metre clear of the ally (ours): a hand's width away put her camera in their face (played with a
			// second player); closer only when there's no room
			for (double clear : new double[] {0.5, 0.1}) {
				double gap = (target.getBbWidth() + player.getBbWidth()) / 2 + clear;
				Vec3 spot = at.add(back.normalize().scale(gap));
				AABB box = player.getDimensions(player.getPose()).makeBoundingBox(spot);
				if (player.level().noCollision(player, box)) {
					return spot;
				}
			}
		}
		return at;
	}

	private static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float primaryReload = s.primaryReload >= 0 && !s.primaryAmmoRestored
			? (float) Math.min(1, s.primaryReload / Ow.ticks(KUNAI_RELOAD_AMMO_AT)) : -1;
		float secondaryReload = s.secondaryReload >= 0 ? (float) Math.min(1, s.secondaryReload / Ow.ticks(OFUDA_RELOAD)) : -1;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, ULT_COST, s.ultActive > 0,
			s.primaryAmmo, primaryReload, s.secondaryAmmo, secondaryReload,
			(float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero);
	}
}
