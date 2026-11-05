package dev.overcraft.sojourn;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.hero.Voice;
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
	/** HeroManager.SOJOURN_GUN's flag over her energy points: she's in Overclock (any of its three phases). */
	public static final int SYNC_OVERCLOCK = 1 << 10;

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
		player.setAttached(HeroManager.SOJOURN_GUN, 0);
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
		// on, so the ground she jumped from doesn't count as the landing) and has stayed down a moment: the jump's speed carried
		// her off a ledge she had just touched (its edge, or a corner grazed on the way down) and the drop below hurt her (found
		// in the release play: a 9 m drop, 18 -> 12)
		if (s.softLanding) {
			player.resetFallDistance();
			boolean down = s.sojournSlide < 0 && player.onGround()
				&& player.level().getGameTime() > s.sojournSlideEndAt + SOFT_LANDING_GRACE;
			s.sojournSettled = down ? s.sojournSettled + 1 : 0;
			if (s.sojournSettled >= SOFT_LANDING_SETTLE) {
				s.softLanding = false;
				s.sojournSettled = 0;
			}
		}
		disruptor(player, s, controls);
		overclock(player, s);
		Railgun.tick(player, s, controls);
		boolean sliding = s.sojournSlide >= 0;
		if (!java.util.Objects.equals(player.getAttached(HeroManager.SOJOURN_SLIDE), sliding)) {
			player.setAttached(HeroManager.SOJOURN_SLIDE, sliding);
		}
		int gun = (int) Math.round(s.sojournEnergy) | (s.sojournUlt >= 0 ? SYNC_OVERCLOCK : 0);
		if (!java.util.Objects.equals(player.getAttached(HeroManager.SOJOURN_GUN), gun)) {
			player.setAttached(HeroManager.SOJOURN_GUN, gun);
		}
		lines(player, s);
	}

	/**
	 * Her lines on her Railgun and Overclock: energy at 100 outside Overclock, 0E7C "Railgun charged!" (1A23 #1181 -> #1182);
	 * still full 10 s on, 0E7D (#1183 / #1184); Overclock over, 0.5 s on, 0E87 "Cooling down" (2159 #363's end plug -> #709).
	 */
	private static void lines(ServerPlayer player, HeroState s) {
		long now = player.level().getGameTime();
		boolean full = s.sojournEnergy >= ENERGY_MAX;
		if (!full) {
			s.sojournFullAt = -1;
			s.sojournFullSaid = false;
			s.sojournHoldingSaid = false;
		} else if (s.sojournFullAt < 0) {
			s.sojournFullAt = now;
		}
		if (full && overclocked(s)) {
			// full in Overclock: #1181's onTrue goes through #1300's gate unsaid, and it stays full after (no new onTrue:
			// found playing, it said "Railgun charged" as Overclock ended)
			s.sojournFullSaid = true;
			s.sojournHoldingSaid = true;
		}
		if (full && !s.sojournFullSaid) {
			s.sojournFullSaid = true;
			Voice.say(player, "railgun_charged", 1);
		} else if (full && !s.sojournHoldingSaid && now - s.sojournFullAt >= Ow.ticks(HOLDING_LINE)) {
			s.sojournHoldingSaid = true;
			Voice.say(player, "railgun_holding", 1);
		}
		if (s.sojournOcEndSay >= 0 && now >= s.sojournOcEndSay) {
			s.sojournOcEndSay = -1;
			if (player.isAlive()) {
				Voice.say(player, "overclock_end", 1);
			}
		}
	}

	/** "Railgun still at full power!" after this long at full (1A23 #1183: a 10 s wait). */
	private static final double HOLDING_LINE = 10;
	/** Overclock's end line this long after it ends (2159 #709: 0.5 s). */
	private static final double OVERCLOCK_END_LINE = 0.5;
	/** Her in-air kill line holds while she's off the ground this long after her slide jump (ticks; ours: v41319's reset isn't read). */
	private static final long AIR_KILL = 60;

	/**
	 * She killed an enemy (Voice.onKill): in Overclock the kill counts and its count picks the line, 0B93 / 113C / 113D (3 and
	 * 4) / 113E (2159 #1278 -> #1283); outside it a Charged Shot kill says 0B8F "Vaporized", 0E7A in the air after her slide
	 * jump (v41319).
	 */
	public static void killed(ServerPlayer player, HeroState s) {
		if (overclocked(s)) {
			int n = ++s.sojournOcKills;
			Voice.say(player, n == 1 ? "overclock_kill" : n == 2 ? "overclock_kill_2" : n <= 4 ? "overclock_kill_3" : "overclock_kill_5", 1);
		} else if (s.sojournCharging) {
			boolean air = !player.onGround() && player.level().getGameTime() - s.sojournSlideJumpAt < AIR_KILL;
			Voice.say(player, air ? "charged_kill_air" : "charged_kill", 1);
		}
	}

	/** Ticks after the slide's end before a landing clears its soft landing: past the jump's take-off (ours). */
	private static final int SOFT_LANDING_GRACE = 6;
	/**
	 * Ticks on the ground in a row that end the soft landing: her landing speed (6-8 m/s) is gone in 3 ticks at the mover's
	 * friction (trace: 6.33, 3.56, 0.49, 0), so a landing that slides her off an edge is still the jump's (ours).
	 */
	private static final int SOFT_LANDING_SETTLE = 5;

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
			// 1441 #84's activation -> #123 322C (54E9 hers, 54FB 70 m) and #124 her line 0B92
			SojournSounds.split(player, "disruptor.cast", player.getEyePosition(), 70);
			Voice.say(player, "disruptor", 1);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && s.sojournUlt < 0 && s.ultCharge >= ULT_COST) {
			s.ultCharge = Combat.afterUlt(dev.overcraft.hero.Hero.SOJOURN, s.ultCharge);
			s.sojournUlt = 0;
			// no ult charge through all three phases (W Ultimate ability: none while it's active)
			s.ultActive = ultTicks();
			// the cast: no fire for 0.75 s (D #364; W), a press of the right button waits in its buffer
			s.weaponRecovery = Math.max(s.weaponRecovery, Ow.ticks(OVERCLOCK_CAST));
			Combat.animate(player, "ult");
			s.sojournOcKills = 0;
			s.sojournOcEndSay = -1;
			// 6FE3's cast (E539 hers, E53A 75 m; its loop is her clients': SojournSounds), the lines 0B19 / 0D2F (#771), her
			// energy filling (32DC: 5E23, M the moment)
			SojournSounds.split(player, "ult.cast", player.getEyePosition(), 75);
			dev.overcraft.hero.HeroSounds.ultLine(player, SojournSounds.of("ult.friendly"), SojournSounds.of("ult.enemy"), 5);
			SojournSounds.own(player, "energy.fill");
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
			// into a wall (1E6F #962 -> #961 -> #641: the sweep's contact set 0702)
			SojournSounds.split(player, "slide.wall", player.position(), 50);
			endSlide(player, s, false);
		}
	}

	// ---- Power Slide (1E6F) ----

	/** Shift: the slide, on the ground or in the air (D #178, M), while it's ready. Her client moves her (ClientSojourn). */
	private static void slide(ServerPlayer player, HeroState s) {
		// (not in water: Minecraft's swimming moves her there, and a slide there spent its cooldown standing still: found
		// playing; ours, Overwatch has no water. Not seated in a boat or a minecart either: there it ended the next tick,
		// its cooldown spent, its sound and line played and nothing moved: found in the release play)
		boolean dual = dual(player);
		if (s.sojournSlide >= 0 || (dual ? s.sojournSlideCharges <= 0 : s.ability1Cooldown > 0) || player.isInWater()
			|| player.isPassenger()) {
			return;
		}
		if (dual) {
			s.sojournSlideCharges--;
		}
		s.sojournSlide = 0;
		s.softLanding = true;
		s.interruptReload(slideTime(player));
		Combat.animate(player, "slide");
		// 2905 / 28E8's start (5185 hers, 5186 others'), Dual Thrusters' own on top (65B6: C3C7), her exert 0B90; the loop, the
		// scrape and the end are her clients' (SojournSounds); Friction Generators' fill (51EB #442 -> 32DC)
		SojournSounds.split(player, "slide.start", player.position(), 50);
		if (dual) {
			SojournSounds.own(player, "perk.thrusters.start");
		}
		if (HeroManager.hasPerk(player, Perk.SOJOURN_FRICTION_GENERATORS)) {
			SojournSounds.own(player, "energy.fill");
		}
		Voice.say(player, "slide", 1);
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
			// #411 -> #277 2C2E (549E hers, 54FC 70 m), with Dual Thrusters #1031 -> #1029 65B7 instead; her exert 0B91
			SojournSounds.split(player, dual(player) ? "perk.thrusters.jump" : "slide.jump", player.position(), 70);
			Voice.say(player, "slide_jump", 1);
			s.sojournSlideJumpAt = player.level().getGameTime();
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
				// #1022 -> #1024: a charge back (009B: 0FCF), both back (5F58: BB32)
				SojournSounds.own(player, s.sojournSlideCharges >= DUAL_CHARGES ? "perk.thrusters.recharge_full" : "perk.thrusters.recharge");
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
			if (player.isAlive()) {
				s.sojournOcEndSay = player.level().getGameTime() + Ow.ticks(OVERCLOCK_END_LINE);
			}
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

	/** Out of play (#100: HeroKit.leavePlay): a slide ends (its cooldown starts); for good, Overclock and her gun's look too. */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, dev.overcraft.hero.HeroKit.OutOfPlay why) {
		if (s.sojournSlide >= 0) {
			endSlide(player, s, false);
		}
		if (why.forGood()) {
			clear(player, s);
		}
	}
}
