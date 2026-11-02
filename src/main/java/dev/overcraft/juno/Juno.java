package dev.overcraft.juno;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerPlayer;

import static dev.overcraft.juno.JunoStats.*;

/**
 * Juno's kit (tools/reference/juno-plan.md "Order"), built step by step, each played before the next. Now: the shell (step 1):
 * her model and her HUD; her Martian Overboots, Glide Boost, the Mediblaster, Pulsar Torpedoes, Hyper Ring and Orbital Ray come
 * next. Her buttons by her data's ids: the left button the Mediblaster (and fires locked torpedoes), the right Pulsar
 * Torpedoes, Shift Glide Boost, E Hyper Ring, Q Orbital Ray, jump her double jump and hover (juno-map.md, "Abilities").
 */
public final class Juno implements HeroKit {
	/** The synced mode's bit: she's gliding (her client moves her: ClientJuno). */
	public static final int MODE_GLIDE = 1;

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.weaponRecovery = 0;
		s.primaryAmmo = MEDIBLASTER_AMMO;
		s.junoGlide = -1;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double rate = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		if (s.junoGlide >= 0 && (++s.junoGlide >= Ow.ticks(GLIDE_TIME) || !controls || !player.isAlive())) {
			endGlide(player, s);
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			glide(player, s);
		}
	}

	// ---- Glide Boost (47B7) ----

	/**
	 * Shift: the glide (her client gives the burst and moves her, ClientJuno), or, 0.4 s into one, its end (the toggle, her
	 * data's default). Survivor's regeneration starts with it (message 26FD: her only one). The cooldown runs from its end.
	 */
	private static void glide(ServerPlayer player, HeroState s) {
		if (s.junoGlide >= 0) {
			if (s.junoGlide >= Ow.ticks(GLIDE_CANCEL_FROM)) {
				endGlide(player, s);
			}
			return;
		}
		if (s.ability1Cooldown > 0) {
			return;
		}
		s.junoGlide = 0;
		s.lastDamagedTick = Math.min(s.lastDamagedTick, Combat.now(player) - Combat.REGEN_DELAY);
		Combat.animate(player, "glide");
	}

	private static void endGlide(ServerPlayer player, HeroState s) {
		s.junoGlide = -1;
		s.ability1Cooldown = Ow.ticks(GLIDE_COOLDOWN);
		Combat.animate(player, "glide_end");
	}

	/** Gliding (knockback taken halved: LivingEntityMixin). */
	public static boolean gliding(HeroState s) {
		return s.hero == dev.overcraft.hero.Hero.JUNO && s.junoGlide >= 0;
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			s.primaryAmmo, -1, -1, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, s.junoGlide >= 0 ? MODE_GLIDE : 0,
			s.junoGlide >= 0 ? (float) ((Ow.ticks(GLIDE_TIME) - s.junoGlide) / 20.0) : 0, 0);
	}
}
