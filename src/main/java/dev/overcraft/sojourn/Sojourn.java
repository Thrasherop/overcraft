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
 * Sojourn's kit (tools/reference/sojourn-plan.md "Order"), built step by step, each played before the next: her model and HUD
 * (step 1), Power Slide with its jump (step 2), the Railgun and its energy (step 3, {@link Railgun}), the Charged Shot (step 4),
 * Disruptor Shot (step 5, {@link Disruptor}), Overclock and Sharpshooter (step 6) and her four perks (step 7). Her buttons by her
 * data's ids (sojourn-map.md, "Abilities"): the left button the Railgun, the right Charged Shot, Shift Power Slide (jump during
 * it: the slide jump), E Disruptor Shot, Q Overclock.
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
		s.sojournDisruptor = -1;
		s.sojournDisruptorWrite = -1;
		s.sojournUlt = -1;
		s.sojournSlideCharges = DUAL_CHARGES;
	}

	/** Leaving her (another hero, leaving the game): the synced slide off. */
	public static void clear(ServerPlayer player, HeroState s) {
		s.sojournSlide = -1;
		s.sojournUlt = -1;
		player.setAttached(HeroManager.SOJOURN_SLIDE, false);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double rate = Combat.cooldownRate(player);
		slideRecharge(player, s, rate);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		// (before the slide's tick, so each of its 20 ticks gains: 19 of 20 did, 71.25 a full slide: played)
		friction(player, s);
		if (s.sojournSlide >= 0 && (++s.sojournSlide >= Ow.ticks(slideTime(player)) || !controls || !player.isAlive())) {
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
		disruptor(player, s, controls);
		overclock(player, s);
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
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && s.ability2Cooldown <= 0 && s.sojournDisruptor < 0
			&& s.sojournDisruptorWrite < 0) {
			// the cast (D #114 / #361, 0.17 s; anim 0x479): the shot leaves at its end
			s.sojournDisruptor = 0;
			Combat.animate(player, "disruptor");
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && s.sojournUlt < 0 && s.ultCharge >= ULT_COST) {
			s.ultCharge = Combat.afterUlt(dev.overcraft.hero.Hero.SOJOURN, s.ultCharge);
			s.sojournUlt = 0;
			// no ult charge through all three phases (W Ultimate ability: none while it's active)
			s.ultActive = ultTicks();
			// the cast: no fire for 0.75 s (D #364; W), a press of the right button waits in its buffer
			s.weaponRecovery = Math.max(s.weaponRecovery, Ow.ticks(OVERCLOCK_CAST));
			Combat.animate(player, "ult");
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
		boolean dual = dual(player);
		if (s.sojournSlide >= 0 || (dual ? s.sojournSlideCharges <= 0 : s.ability1Cooldown > 0) || player.isInWater()) {
			return;
		}
		if (dual) {
			s.sojournSlideCharges--;
		}
		s.sojournSlide = 0;
		s.softLanding = true;
		s.interruptReload(slideTime(player));
		Combat.animate(player, "slide");
	}

	private static void endSlide(ServerPlayer player, HeroState s, boolean jumped) {
		s.sojournSlide = -1;
		s.sojournSlideEndAt = player.level().getGameTime();
		if (!dual(player)) {
			// from its end (D #368 -> #1035); with Dual Thrusters the recharge runs on its own (slideRecharge)
			s.ability1Cooldown = Ow.ticks(SLIDE_COOLDOWN);
		}
		player.setAttached(HeroManager.SOJOURN_SLIDE, false);
		if (jumped) {
			Combat.animate(player, "slide_jump");
		}
	}

	/**
	 * The slide's cooldown. With Dual Thrusters (D 1E6F #1033 / #822): two charges, one recharging at a time over 7 s, starting
	 * once a slide has ended and a charge is missing (ability1Cooldown is that recharge's time left).
	 */
	private static void slideRecharge(ServerPlayer player, HeroState s, double rate) {
		if (!dual(player)) {
			s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
			return;
		}
		if (s.sojournSlideCharges >= DUAL_CHARGES) {
			s.sojournSlideCharges = DUAL_CHARGES;
			s.ability1Cooldown = 0;
			return;
		}
		if (s.ability1Cooldown > 0) {
			s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
			if (s.ability1Cooldown <= 0) {
				s.sojournSlideCharges++;
			}
		}
		if (s.sojournSlideCharges < DUAL_CHARGES && s.ability1Cooldown <= 0 && s.sojournSlide < 0) {
			s.ability1Cooldown = Ow.ticks(SLIDE_COOLDOWN);
		}
	}

	/** Sharpshooter (01C7 #2832): crit damage dealt to a living target takes 0.0075 s a point off Power Slide's cooldown. */
	static void sharpshooter(ServerPlayer player, HeroState s, double critDamage) {
		if (critDamage <= 0 || s.ability1Cooldown <= 0) {
			return;
		}
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - critDamage * SHARPSHOOTER * 20);
		if (s.ability1Cooldown <= 0 && dual(player) && s.sojournSlideCharges < DUAL_CHARGES) {
			s.sojournSlideCharges++;
		}
	}

	/** Friction Generators (51EB #468 / #470): 75 energy over a full slide, its decay paused meanwhile. */
	private static void friction(ServerPlayer player, HeroState s) {
		if (s.sojournSlide < 0 || !HeroManager.hasPerk(player, Perk.SOJOURN_FRICTION_GENERATORS)) {
			return;
		}
		s.sojournEnergy = Math.min(Railgun.cap(player, s), s.sojournEnergy + FRICTION_ENERGY / Ow.ticks(slideTime(player)));
		s.sojournGainAt = player.level().getGameTime();
	}

	/** She's sliding, for her low pose (PlayerMixin): her client's own slide, or her kit's on the server. */
	public static boolean slidingPose(net.minecraft.world.entity.player.Player player) {
		if (player instanceof ServerPlayer server) {
			HeroState s = HeroManager.state(server);
			return s != null && s.hero == dev.overcraft.hero.Hero.SOJOURN && s.sojournSlide >= 0 && HeroManager.controls(server);
		}
		return dev.overcraft.hero.OwMovement.sliding(player);
	}

	private static boolean dual(ServerPlayer player) {
		return HeroManager.hasPerk(player, Perk.SOJOURN_DUAL_THRUSTERS);
	}

	/** The slide's length: 1 s, 0.75 s with Dual Thrusters (D 51EB #422). */
	public static double slideTime(ServerPlayer player) {
		return dual(player) ? SLIDE_TIME_DUAL : SLIDE_TIME;
	}

	// ---- Disruptor Shot (1441) ----

	/** Its cast (0.17 s) then the launch; its cooldown written 0.5 s after that (D #360 -> #355). */
	private static void disruptor(ServerPlayer player, HeroState s, boolean controls) {
		if (s.sojournDisruptorWrite >= 0 && --s.sojournDisruptorWrite < 0) {
			s.ability2Cooldown = Ow.ticks(DISRUPTOR_COOLDOWN);
		}
		if (s.sojournDisruptor < 0) {
			return;
		}
		if (!controls || !player.isAlive()) {
			// G or death in the cast: no shot, nothing spent (ours)
			s.sojournDisruptor = -1;
			return;
		}
		if (++s.sojournDisruptor >= Ow.ticks(DISRUPTOR_CAST)) {
			s.sojournDisruptor = -1;
			Disruptor.launch(player);
			s.sojournDisruptorWrite = Ow.ticks(DISRUPTOR_COOLDOWN_AFTER);
		}
	}

	// ---- Overclock (2159) ----

	/** Cast, active, tail in ticks. */
	private static int ultTicks() {
		return Ow.ticks(OVERCLOCK_CAST) + Ow.ticks(OVERCLOCK_TIME) + Ow.ticks(OVERCLOCK_TAIL);
	}

	/** In Overclock now (any of its three phases: her energy fills, no gain from hits, no decay). */
	public static boolean overclocked(HeroState s) {
		return s.hero == dev.overcraft.hero.Hero.SOJOURN && s.sojournUlt >= 0;
	}

	/** Through its phases: +5 energy a tick to the cap (100/s, D 04DF from the cast); death ends it. */
	private static void overclock(ServerPlayer player, HeroState s) {
		if (s.sojournUlt < 0) {
			return;
		}
		if (!player.isAlive() || ++s.sojournUlt > ultTicks()) {
			s.sojournUlt = -1;
			s.ultActive = 0;
			return;
		}
		s.sojournEnergy = Math.min(Railgun.cap(player, s), s.sojournEnergy + OVERCLOCK_FILL / 20);
		// (the 9 s before the decay count from its end: ours, D pauses the decay through it)
		s.sojournGainAt = player.level().getGameTime();
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		boolean dualSync = s.has(Perk.SOJOURN_DUAL_THRUSTERS);
		// Disruptor's cooldown shows full from the press until it's written (ours: the box marked "just used")
		float disruptor = s.sojournDisruptor >= 0 || s.sojournDisruptorWrite >= 0 ? (float) DISRUPTOR_COOLDOWN
			: (float) (s.ability2Cooldown / 20.0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, s.sojournUlt >= 0,
			s.primaryAmmo, s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(RELOAD)) : -1,
			dualSync ? s.sojournSlideCharges : -1, -1,
			(float) (s.ability1Cooldown / 20.0), disruptor, s.rushed, s.canChangeHero, s.onFire,
			Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk), 0,
			s.sojournSlide >= 0 ? MODE_SLIDE : 0,
			s.sojournUlt >= 0 ? (float) ((ultTicks() - s.sojournUlt) / 20.0)
				: s.sojournSlide >= 0 ? (float) ((Ow.ticks(dualSync ? SLIDE_TIME_DUAL : SLIDE_TIME) - s.sojournSlide) / 20.0) : 0,
			(float) (s.sojournEnergy / ENERGY_MAX));
	}
}
