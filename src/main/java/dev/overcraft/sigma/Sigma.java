package dev.overcraft.sigma;

import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerPlayer;

import static dev.overcraft.sigma.SigmaStats.*;

/**
 * Sigma's kit (tools/reference/sigma-plan.md "Order"), built step by step, each played before the next. Now: the Tank role
 * and Stalwart (dev.overcraft.hero.Tank), his model, his HUD, the stock mover (his float is his animation) (steps 1-3);
 * Hyperspheres (step 4), the Experimental Barrier (step 5), Kinetic Grasp (step 6), Accretion (step 7), Gravitic Flux (step 8).
 * His buttons by his data's ids (sigma-map.md "Buttons"): the left button Hyperspheres, the right the barrier, Shift
 * Kinetic Grasp, E Accretion, Q Gravitic Flux, jump Levitation (a perk).
 */
public final class Sigma implements HeroKit {
	/**
	 * The HUD's and his client's state bits: his barrier is out, Kinetic Grasp is on, Accretion's gather (no jump), Gravitic
	 * Flux's flight, its 0.6 s rise.
	 */
	public static final int MODE_BARRIER_OUT = 1, MODE_GRASP = 2, MODE_GATHER = 4, MODE_FLUX_FLIGHT = 8, MODE_FLUX_RISE = 16;
	/**
	 * HeroManager.SIGMA_STATE's bits, for every client's look and sound (SigmaFx, client.SigmaSounds): the MODE_* above but the
	 * barrier's, plus Flux's aim (the reticle), Levitation and Hyper Strike charged.
	 */
	public static final int SYNC_FLUX_AIM = 32, SYNC_LEVITATE = 64, SYNC_STRIKE = 128;

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		clear(player, s);
		s.weaponRecovery = 0;
		s.sigmaSecondIn = -1;
		s.sigmaReload = 0;
		s.sigmaBuffer = 0;
		s.sigmaVolleys = 0;
		s.sigmaRightWas = false;
		s.sigmaBarrierReadyAt = -1;
		s.barrierHealth = BARRIER_HEALTH;
		s.barrierBrokenUntil = -1;
		s.barrierDownSince = -10000;
		s.sigmaGrasp = -1;
		s.sigmaGraspAbsorbed = 0;
		s.sigmaCast = -1;
		s.sigmaFluxPhase = GraviticFlux.NONE;
		s.sigmaFluxTicks = 0;
		s.sigmaFluxPoint = null;
		s.sigmaDirectHits = 0;
		s.sigmaStrikeReady = false;
		s.sigmaBarrierSample = -1;
		s.sigmaBarrierUnder = false;
		s.sigmaLevitateUntil = -1;
	}

	/** Leaving him (another hero, leaving the world): his barrier goes, Grasp ends with nothing. */
	public static void clear(ServerPlayer player, HeroState s) {
		SigmaBarrier.clear(player, s);
		KineticGrasp.end(player, s, false);
		Accretion.cancel(player, s);
		GraviticFlux.end(player, s);
		player.setAttached(dev.overcraft.hero.HeroManager.SIGMA_STATE, 0);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		s.sigmaNow = player.level().getGameTime();
		double rate = Combat.cooldownRate(player);
		if (!KineticGrasp.active(s)) {
			s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		}
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		// his hands are busy in Grasp, Accretion's gather and Gravitic Flux: no spheres, no barrier sent meanwhile (ours, M)
		boolean busy = KineticGrasp.active(s) || Accretion.casting(s) || GraviticFlux.active(s);
		Hyperspheres.tick(player, s, controls, busy);
		SigmaBarrier.tick(player, s, controls, busy);
		KineticGrasp.tick(player, s, controls);
		Accretion.tick(player, s, controls);
		GraviticFlux.tick(player, s, controls);
		SigmaPerks.tick(player, s);
		int state = (KineticGrasp.active(s) ? MODE_GRASP : 0) | (Accretion.casting(s) ? MODE_GATHER : 0)
			| (GraviticFlux.flying(s) ? MODE_FLUX_FLIGHT : 0) | (s.sigmaFluxPhase == GraviticFlux.START ? MODE_FLUX_RISE : 0)
			| (s.sigmaFluxPhase == GraviticFlux.AIM ? SYNC_FLUX_AIM : 0) | (s.sigmaNow < s.sigmaLevitateUntil ? SYNC_LEVITATE : 0)
			| (s.sigmaStrikeReady ? SYNC_STRIKE : 0);
		if (!java.util.Objects.equals(player.getAttached(dev.overcraft.hero.HeroManager.SIGMA_STATE), state)) {
			player.setAttached(dev.overcraft.hero.HeroManager.SIGMA_STATE, state);
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			KineticGrasp.press(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0) {
			Accretion.press(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0) {
			GraviticFlux.press(player, s);
		}
	}

	/**
	 * He killed an enemy (Voice.onDeath): his rock's kill line "Collision!" (0B9D: a kill by damage id 53A7, 1BBB #722 -> #723's
	 * message) or the slam's "To the void!" (0BA5: damage id 53A8, 1DBC #577 -> 1E41 #1134), before the shared ones.
	 */
	public static void killed(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) {
		if (source.getDirectEntity() instanceof dev.overcraft.entity.AccretionEntity) {
			dev.overcraft.hero.Voice.say(player, "accretion_kill", 1);
		} else if (source.is(GraviticFlux.SLAM_TYPE)) {
			dev.overcraft.hero.Voice.say(player, "flux_kill", 1);
		}
	}

	/** His client's extra input (Levitation began: SigmaPerks). */
	public static void extra(ServerPlayer player, HeroState s, int extra) {
		SigmaPerks.extra(player, s, extra);
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		// no ammo (Hyperspheres have none); the barrier's health share as the meter, its cooldown as the second reload
		long now = s.sigmaNow;
		int mode = (s.sigmaBarrierId != null ? MODE_BARRIER_OUT : 0) | (KineticGrasp.active(s) ? MODE_GRASP : 0)
			| (Accretion.casting(s) ? MODE_GATHER : 0) | (GraviticFlux.flying(s) ? MODE_FLUX_FLIGHT : 0)
			| (s.sigmaFluxPhase == GraviticFlux.START ? MODE_FLUX_RISE : 0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, GraviticFlux.active(s),
			-1, -1, (int) Math.ceil(s.barrierHealth), SigmaBarrier.cooldown(now, s), (float) (s.ability1Cooldown / 20.0),
			(float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s),
			Perk.index(s.minorPerk), Perk.index(s.majorPerk), 0, mode, KineticGrasp.active(s) ? s.sigmaGrasp / 20f : 0,
			(float) (s.barrierHealth / BARRIER_HEALTH));
	}
}
