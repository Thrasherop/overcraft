package dev.overcraft.brigitte;

import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerPlayer;

import static dev.overcraft.brigitte.BrigitteStats.*;

/**
 * Brigitte's kit (tools/reference/brigitte-plan.md "Order"), built step by step, each played before the next. Now: the
 * shell (step 1): her model, her flail and folded shield, her HUD; Rocket Flail, Whip Shot, Repair Pack, Barrier Shield,
 * Shield Bash and Rally come next. Her buttons by her data's ids: the left button the flail (Shield Bash with the shield
 * up), the right the shield, Shift Whip Shot, E Repair Pack, Q Rally (brigitte-map.md, "Buttons").
 */
public final class Brigitte implements HeroKit {
	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.weaponRecovery = 0;
		s.secondaryAmmo = REPAIR_CHARGES;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			0, -1, s.secondaryAmmo, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, 0, 0, 0);
	}
}
