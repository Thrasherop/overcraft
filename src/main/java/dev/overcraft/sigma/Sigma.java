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
 * Hyperspheres (step 4), the Experimental Barrier (step 5), Kinetic Grasp (step 6). Accretion and Gravitic Flux come next.
 * His buttons by his data's ids (sigma-map.md "Buttons"): the left button Hyperspheres, the right the barrier, Shift
 * Kinetic Grasp, E Accretion, Q Gravitic Flux, jump Levitation (a perk).
 */
public final class Sigma implements HeroKit {
	/** The HUD's state bits: his barrier is out, Kinetic Grasp is on. */
	public static final int MODE_BARRIER_OUT = 1, MODE_GRASP = 2;

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
	}

	/** Leaving him (another hero, leaving the world): his barrier goes, Grasp ends with nothing. */
	public static void clear(ServerPlayer player, HeroState s) {
		SigmaBarrier.clear(player, s);
		KineticGrasp.end(player, s, false);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		s.sigmaNow = player.level().getGameTime();
		double rate = Combat.cooldownRate(player);
		if (!KineticGrasp.active(s)) {
			s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		}
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		boolean grasping = KineticGrasp.active(s);
		Hyperspheres.tick(player, s, controls, grasping);
		SigmaBarrier.tick(player, s, controls, grasping);
		KineticGrasp.tick(player, s, controls);
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			KineticGrasp.press(player, s);
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		// no ammo (Hyperspheres have none); the barrier's health share as the meter, its cooldown as the second reload
		long now = s.sigmaNow;
		int mode = (s.sigmaBarrierId != null ? MODE_BARRIER_OUT : 0) | (KineticGrasp.active(s) ? MODE_GRASP : 0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			-1, -1, (int) Math.ceil(s.barrierHealth), SigmaBarrier.cooldown(now, s), (float) (s.ability1Cooldown / 20.0),
			(float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s),
			Perk.index(s.minorPerk), Perk.index(s.majorPerk), 0, mode, KineticGrasp.active(s) ? s.sigmaGrasp / 20f : 0,
			(float) (s.barrierHealth / BARRIER_HEALTH));
	}
}
