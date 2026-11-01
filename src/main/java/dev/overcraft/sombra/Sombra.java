package dev.overcraft.sombra;

import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerPlayer;

import static dev.overcraft.sombra.SombraStats.*;

/**
 * Sombra's kit (tools/reference/sombra-plan.md "Order"). Its first step: her models and clips on the select screen and in
 * play, the pistol full in the HUD. Her weapons and abilities come next, each played before the one after.
 */
public final class Sombra implements HeroKit {
	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = PISTOL_AMMO;
		s.primaryReload = -1;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float reload = s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / dev.overcraft.Ow.ticks(PISTOL_RELOAD)) : -1;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			s.primaryAmmo, reload, 0, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, 0, 0, 0);
	}
}
