package dev.overcraft.sigma;

import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerPlayer;

import static dev.overcraft.sigma.SigmaStats.*;

/**
 * Sigma's kit (tools/reference/sigma-plan.md "Order"), built step by step, each played before the next. Now: the shell
 * (steps 1-3): the Tank role and Stalwart (dev.overcraft.hero.Tank), his model, his HUD, the stock mover (his float is his
 * animation). Hyperspheres, the Experimental Barrier, Kinetic Grasp, Accretion and Gravitic Flux come next. His buttons by
 * his data's ids (sigma-map.md "Buttons"): the left button Hyperspheres, the right the barrier, Shift Kinetic Grasp,
 * E Accretion, Q Gravitic Flux, jump Levitation (a perk).
 */
public final class Sigma implements HeroKit {
	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.weaponRecovery = 0;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		// no ammo (Hyperspheres have none: the HUD shows no count)
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			-1, -1, -1, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, 0, 0, 0);
	}
}
