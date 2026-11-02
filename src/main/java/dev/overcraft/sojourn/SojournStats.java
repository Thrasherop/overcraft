package dev.overcraft.sojourn;

/**
 * Sojourn's numbers (tools/reference/sojourn.md has every quote, revision and date; the game data map is sojourn-map.md, build
 * 154088 of 2026-10-06, its detail in E:\Overwatch Archive\research\sojourn\work_kit\kit_sojourn.md; the plan sojourn-plan.md).
 * Sources: the game data (D, with its value id or graph node), Blizzard's patch notes (BZ) and the Overwatch wiki (W). No patch
 * has changed her kit since the data build (BZ 2026-10-06 was hit volumes and global hitscan sizes; the last kit change BZ
 * 2026-06-16), so the data's numbers are the live ones. Confidence: H read in the data or the sources agree, M one link
 * inferred, L a guess; where no source gives a value it's ours and says so. Filled in step by step (sojourn-plan.md, "Order").
 */
public final class SojournStats {
	private SojournStats() {
	}

	/** Damage, Sharpshooter: 225 health, no armor or shields (D 3505 #106: 04D8 225 / 04D7 0 / 04D9 0; BZ 2025-08-26; W) H. */
	public static final int HEALTH = 225;

	/** Overclock's cost (D 0AFB 2300; W 2300; BZ 2024-02-13 +10% on 2100 = 2310) H/M. */
	public static final double ULT_COST = 2300;

	/** Perk XP: 1695 to the minor, 2540 more to the major (D 12D5 / 12D6; W rev 265678) M. */
	public static final double PERK_MINOR_XP = 1695, PERK_MAJOR_XP = 1695 + 2540;

	// ---- Railgun (1A23; step 3) and its energy ----
	/** The magazine (D 04D4 45; W) M; its reload 1.2 s, full at 0.96 s (D 04C8, #287 / #283) M. */
	public static final int RAILGUN_AMMO = 45;
	public static final double RELOAD = 1.2, RELOAD_FULL_AT = 0.96;
	/** Energy: 0-100 (D v971 100; BZ 2025-02-18; W) H; 150 in Overclock with Overcharged (D #1485) H. */
	public static final double ENERGY_MAX = 100, ENERGY_MAX_OVERCHARGED = 150;

	// ---- Power Slide (1E6F, ability 1) ----
	/** At most 1 s (D 04E2 = v9110; BZ 2025-03-25; W) H; 0.75 s with Dual Thrusters (D 51EB #422 x0.75) H. */
	public static final double SLIDE_TIME = 1, SLIDE_TIME_DUAL = 0.75;
	/**
	 * Its speed: the mover's speed-bonus field (x786680F6, H by 01C7 #2011) set to 1CFC 1.6 x the ramp v7779 for the whole slide
	 * (#311): +160 %, 5.5 x 2.6 = 14.3 m/s at the full ramp (M; W +160 %). In the air the fly node #387 puts 1CFD 1.2 on the same
	 * field, read as added (+280 %, 20.9 m/s: W's number) M.
	 */
	public static final double SLIDE_BONUS = 1.6, SLIDE_BONUS_AIR = 1.2;
	/**
	 * The ramp v7779: 0.4 to 1 over the first 0.2 s (#354 / #334), then from 0.3 s before the end down to 0.3 (#305 / #306):
	 * 9.0 m/s at the start, 14.3 from 0.2 s, 8.1 at the end (M: m_5BEFF040 read as the chase time).
	 */
	public static final double SLIDE_RAMP_FROM = 0.4, SLIDE_RAMP_UP = 0.2, SLIDE_RAMP_TO = 0.3, SLIDE_RAMP_DOWN = 0.3;
	/** Its fall capped at 2 m/s (D override x978C5453 = 2.0, the field of Mercy's descent and Juno's hover) M. */
	public static final double SLIDE_FALL = 2;
	/**
	 * Against a wall it ends: rays #684 / #685 set v2823 / v2824, and 0.3 s on #961 -> #963 end it (D nodes H, purpose M). Ours: 0.3
	 * s with her move blocked.
	 */
	public static final double SLIDE_WALL_END = 0.3;
	/** The cooldown 7 s (D 1CE9 7; BZ 2025-08-26; W) H, from the slide's end (D #368 onEnd -> #1035 writes it) M. */
	public static final double SLIDE_COOLDOWN = 7;
	/**
	 * The slide jump: the jump key while sliding (D #192), its vertical speed set to 1CFA 17 m/s and 6.5 m/s added along the
	 * slide (#411: (dir x 6.5, 17, dir x 6.5) read as added, M; W 17 m/s up and +273 % = 20.5 m/s forward, that sum at full
	 * slide speed); with Dual Thrusters 13.75 up and 11 along (#1031) H values.
	 */
	public static final double SLIDE_JUMP_UP = 17, SLIDE_JUMP_ALONG = 6.5, SLIDE_JUMP_UP_DUAL = 13.75, SLIDE_JUMP_ALONG_DUAL = 11;
	/**
	 * Allowed with ground within 2 m below (D #337: a 2 m ray from 0.5 m above her) or within 0.15 s of losing it (v568 counting
	 * 100 a second, #409) M.
	 */
	public static final double SLIDE_JUMP_GROUND = 2, SLIDE_JUMP_COYOTE = 0.15;
	/** For 0.25 s after it her gravity is x1.4 (D #190 / #200 x5B83ECB5 = 1.4, the meaning Juno's) H/M; then the slide ends. */
	public static final double SLIDE_JUMP_GRAVITY = 1.4, SLIDE_JUMP_GRAVITY_TIME = 0.25;

	// ---- Disruptor Shot (1441, ability 2; step 5) ----
	/** Its cooldown 15 s (D 1CE4 15; W) M. */
	public static final double DISRUPTOR_COOLDOWN = 15;
}
