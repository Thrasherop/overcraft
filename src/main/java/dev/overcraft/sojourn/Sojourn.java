package dev.overcraft.sojourn;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerPlayer;

import static dev.overcraft.sojourn.SojournStats.*;

/**
 * Sojourn's kit (tools/reference/sojourn-plan.md "Order"), built step by step, each played before the next. Now: the shell
 * (step 1: her model, her HUD with the energy meter), Power Slide with its jump (step 2), the Railgun and its energy (step 3,
 * {@link Railgun}) and the Charged Shot (step 4); Disruptor Shot and Overclock come next. Her buttons by her data's ids (sojourn-map.md, "Abilities"): the left button the
 * Railgun, the right Charged Shot, Shift Power Slide (jump during it: the slide jump), E Disruptor Shot, Q Overclock.
 */
public final class Sojourn implements HeroKit {
	/** The synced mode's bit: she's sliding (her client moves her: ClientSojourn). */
	public static final int MODE_SLIDE = 1;

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.weaponRecovery = 0;
		s.primaryAmmo = RAILGUN_AMMO;
		s.sojournSlide = -1;
		s.sojournEnergy = 0;
		s.sojournShotClock = 1;
		s.sojournSpread = 0;
		s.sojournRightWas = false;
		s.sojournCharged = 0;
	}

	/** Leaving her (another hero, leaving the game): the synced slide off. */
	public static void clear(ServerPlayer player, HeroState s) {
		s.sojournSlide = -1;
		player.setAttached(HeroManager.SOJOURN_SLIDE, false);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double rate = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		if (s.sojournSlide >= 0 && (++s.sojournSlide >= Ow.ticks(slideTime(s)) || !controls || !player.isAlive())) {
			endSlide(player, s, false);
		}
		// the slide and its jump land her unhurt (CLAUDE.md, #35): her fall doesn't count until she's down after it (a few ticks
		// on, so the ground she jumped from doesn't count as the landing)
		if (s.softLanding) {
			player.resetFallDistance();
			if (s.sojournSlide < 0 && player.onGround() && player.level().getGameTime() > s.sojournSlideEndAt + SOFT_LANDING_GRACE) {
				s.softLanding = false;
			}
		}
		Railgun.tick(player, s, controls);
		boolean sliding = s.sojournSlide >= 0;
		if (!java.util.Objects.equals(player.getAttached(HeroManager.SOJOURN_SLIDE), sliding)) {
			player.setAttached(HeroManager.SOJOURN_SLIDE, sliding);
		}
	}

	/** Ticks after the slide's end before a landing clears its soft landing: past the jump's take-off (ours). */
	private static final int SOFT_LANDING_GRACE = 6;

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			slide(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0) {
			Railgun.startReload(player, s);
		}
	}

	/**
	 * Her client's extra input while sliding: EXTRA_JUMP, her slide jump went off (OwMovement moved her: the slide ends, its
	 * cooldown from here); EXTRA_SLIDE_END, a wall ended it (D #961 -> #963).
	 */
	public static void extra(ServerPlayer player, HeroState s, int extra) {
		if (s.sojournSlide < 0) {
			return;
		}
		if ((extra & HeroInputPayload.EXTRA_JUMP) != 0) {
			endSlide(player, s, true);
		} else if ((extra & HeroInputPayload.EXTRA_SLIDE_END) != 0) {
			endSlide(player, s, false);
		}
	}

	// ---- Power Slide (1E6F) ----

	/** Shift: the slide, on the ground or in the air (D #178, M), while it's ready. Her client moves her (ClientSojourn). */
	private static void slide(ServerPlayer player, HeroState s) {
		// (not in water: Minecraft's swimming moves her there, and a slide there spent its cooldown standing still: found
		// playing; ours, Overwatch has no water)
		if (s.sojournSlide >= 0 || s.ability1Cooldown > 0 || player.isInWater()) {
			return;
		}
		s.sojournSlide = 0;
		s.softLanding = true;
		s.interruptReload(slideTime(s));
		Combat.animate(player, "slide");
	}

	private static void endSlide(ServerPlayer player, HeroState s, boolean jumped) {
		s.sojournSlide = -1;
		s.sojournSlideEndAt = player.level().getGameTime();
		s.ability1Cooldown = Ow.ticks(SLIDE_COOLDOWN);
		player.setAttached(HeroManager.SOJOURN_SLIDE, false);
		if (jumped) {
			Combat.animate(player, "slide_jump");
		}
	}

	/** She's sliding, for her low pose (PlayerMixin): her client's own slide, or her kit's on the server. */
	public static boolean slidingPose(net.minecraft.world.entity.player.Player player) {
		if (player instanceof ServerPlayer server) {
			HeroState s = HeroManager.state(server);
			return s != null && s.hero == dev.overcraft.hero.Hero.SOJOURN && s.sojournSlide >= 0 && HeroManager.controls(server);
		}
		return dev.overcraft.hero.OwMovement.sliding(player);
	}

	/** The slide's length: 1 s (0.75 with Dual Thrusters, step 7). */
	public static double slideTime(HeroState s) {
		return SLIDE_TIME;
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			s.primaryAmmo, s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(RELOAD)) : -1, -1, -1,
			(float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero, s.onFire,
			Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk), 0,
			s.sojournSlide >= 0 ? MODE_SLIDE : 0,
			s.sojournSlide >= 0 ? (float) ((Ow.ticks(slideTime(s)) - s.sojournSlide) / 20.0) : 0,
			(float) (s.sojournEnergy / ENERGY_MAX));
	}
}
