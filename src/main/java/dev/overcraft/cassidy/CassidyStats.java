package dev.overcraft.cassidy;

/**
 * Cassidy's numbers. Sources: the game data (D: build 154088 of 2026-10-06, tools/reference/cassidy-map.md and its node-by-node
 * reading E:\Overwatch Archive\research\cassidy\work_kit\kit_cassidy.md: the value id or graph node), Blizzard's patch notes (BZ)
 * and the Overwatch wiki (W rev 409375): tools/reference/cassidy.md has every date. His last live number change is BZ 2026-08-11
 * (Giddy Up), before the data build: the data's numbers are the live ones (BZ 2026-11-03 / 11-05 don't touch him). No Workshop
 * recording of him exists yet: his movement is the default mover's (D), not measured. Confidence: H read in the data or the
 * sources agree, M one link inferred, L a guess; where no source gives a value it's ours.
 */
public final class CassidyStats {
	private CassidyStats() {
	}

	/** Damage, Sharpshooter: 250 health, no armor or shields (D 02B7 250; BZ 2025-08-26 275 -> 250; W) H. */
	public static final int HEALTH = 250;

	/** Deadeye's cost (D 0AEF 1800, M that it's the cost; W 1800) H/M. */
	public static final double ULT_COST = 1800;

	/** Perk XP: 1560 to the minor, 2340 more to the major (D, M; W 1560 / 3900) H/M. */
	public static final double PERK_MINOR_XP = 1560, PERK_MAJOR_XP = 1560 + 2340;

	// ---- Peacekeeper (01D2, the left button) ----
	/** 70 a shot (D 02BF 70; W; 65 is Stadium's) H. */
	public static final double PEACEKEEPER_DAMAGE = 70;
	/** 0.5 s from a shot to the next (D 01D2 v173 chase 0.5; W) H. */
	public static final double PEACEKEEPER_INTERVAL = 0.5;
	/** Falloff from 25 m to 35 m, down to x0.3 (D 01D2; BZ moved its start 20 -> 25 m; W 21 at 35 m) H; linear between (M). */
	public static final double FALLOFF_START = 25, FALLOFF_END = 35, FALLOFF_MIN = 0.3;
	/** x2 on the head (D m_66D29548 2; W) H. */
	public static final double CRIT = 2;
	/** Hitscan 0.04 m across (D, the global large size after BZ 2026-10-06; W) H; no spread (D) H. */
	public static final double RADIUS = 0.04;
	/** The longest shot (D m_FEC435C6 400 on Peacekeeper, 100 on Fan the Hammer: L as ranges). */
	public static final double RANGE = 400, FAN_RANGE = 100;
	/** Six rounds (D 01D2; W) H. */
	public static final int AMMO = 6;
	/** The reload: 1.5 s, the rounds back at 0.8 s (1.5 x 0.5333), no fire until it's over (D 01D2 H; W 1.5 / 0.8) H/M. */
	public static final double RELOAD = 1.5, RELOAD_AMMO_AT = 0.8;

	// ---- Fan the Hammer (01D2 loadout 005E, the right button) ----
	/** 50 a shot (D 01D2; W) H. */
	public static final double FAN_DAMAGE = 50;
	/** Falloff from 20 m to 30 m, down to x0.3 (D; W 15 at 30 m) H. */
	public static final double FAN_FALLOFF_START = 20, FAN_FALLOFF_END = 30;
	/** 0.12 s between its shots, the cylinder run out from the press (D 01D2, M that it runs out whatever the button; W) H/M. */
	public static final double FAN_INTERVAL = 0.12;
	/** The first shot exact, every later one up to 6 degrees off (D: 0 then 6, one shot to the most; W) H/M. */
	public static final double FAN_SPREAD = 6;
	/** After a fan, 0.3 s before Peacekeeper fires (D 01D2 #1310 -> #347 v173 = 0.3) M. */
	public static final double FAN_PRIMARY_LOCK = 0.3;

	// ---- Combat Roll (01F0, Shift) ----
	/** 15 m/s for 0.4 s (6.0 m), x1.3 in the air (19.5 m/s, 7.8 m) (D 01F0 v1648 157D 15, 2299 1.3 when airborne #221 / #178) H/M. */
	public static final double ROLL_SPEED = 15, ROLL_TIME = 0.4, ROLL_AIR = 1.3;
	/** Out of it at 5.5 m/s at most (D 01F0: exits at the run speed) H/M. */
	public static final double ROLL_EXIT_SPEED = 5.5;
	/** 50 % less damage the whole roll (D 01F0; BZ; W) H. */
	public static final double ROLL_DAMAGE_TAKEN = 0.5;
	/** The cylinder full 0.2 s in (D 01F0 message 007D) H/M. */
	public static final double ROLL_RELOAD_AT = 0.2;
	/** 5 s, from its end (D 01F0; BZ 2026-02-10 6 -> 5; W) H. */
	public static final double ROLL_COOLDOWN = 5;

	// ---- Flashbang (01E9, E) ----
	/** 0.1 s from the press to the throw, then 0.35 s before he fires (D 01E9; W 0.128 + 0.608) H. */
	public static final double FLASH_CAST = 0.1, FLASH_RECOVERY = 0.35;
	/** 30 m/s, falling at 20 m/s² (M), 0.2 m (D 01E9; W 30 m/s, 0.2 m) H/M. */
	public static final double FLASH_SPEED = 30, FLASH_GRAVITY = 20, FLASH_RADIUS = 0.2;
	/**
	 * It goes off at the first enemy or block, or after this far (D v7056 = (23F6 7 - 0.5) + 0.5 x STU_FDBB0B67, the last term
	 * unread: L; W 7 m) M: 7 m.
	 */
	public static final double FLASH_RANGE = 7;
	/** 75 to everything within 3 m, no falloff, no knockback (D 01E9: one ring at 1.0, M; W) H/M. */
	public static final double FLASH_DAMAGE = 75, FLASH_BLAST = 3;
	/** 12 s from the cast's end (D 01E9; W) H. */
	public static final double FLASH_COOLDOWN = 12;
	/** Gone after 5 s in flight (D life 5) H. */
	public static final double FLASH_LIFETIME = 5;
	/**
	 * Hinder (status 0079.053, graph 0228): 1.2 s, half speed, no movement abilities, no crouch (D 1.2 x (1 - CC resist) H, the
	 * rest M; BZ 2024-06-20 0.9 -> 1.2 s; W) H/M. Ours: Minecraft creatures take the slow only.
	 */
	public static final double HINDER_TIME = 1.2, HINDER_SLOW = 0.5;
	/** Where it leaves his hand (right, up, ahead from his eyes, m): ours. */
	public static final double FLASH_RIGHT = -0.2, FLASH_UP = -0.15, FLASH_AHEAD = 0.5;

	// ---- Deadeye (022F / per target 01EE, Q) ----
	/** 0.2 s cast, then up to 7 s (D 022F) H. */
	public static final double DEADEYE_CAST = 0.2, DEADEYE_TIME = 7;
	/** Each target's lock: 150 a second for its own first 2 s, then 300 (D 01EE, paused while out of sight) H; W the same. */
	public static final double LOCK_SLOW = 150, LOCK_FAST = 300, LOCK_RAMP = 2;
	/** His targets: within a 105 degree cone (M: read as its full width), 200 m, in sight (D 022F #486 StateTrackTargets) H. */
	public static final double DEADEYE_CONE = 105, DEADEYE_RANGE = 200;
	/**
	 * His own speed and damage taken: 70 % slower and 40 % less damage for 2 s, then down to 35 % slower and none less in 10 steps
	 * of 0.15 s (D 022F #605 / #611 H/M; W 1.4 s fade) H/M.
	 */
	public static final double DEADEYE_SLOW = 0.7, DEADEYE_SLOW_AFTER = 0.35, DEADEYE_DAMAGE_TAKEN = 0.4;
	public static final double DEADEYE_FADE_AFTER = 2, DEADEYE_FADE_STEP = 0.15;
	public static final int DEADEYE_FADE_STEPS = 10;
	/**
	 * Fire: 0.1 s, then one target every 0.1 s, each shot 0.066 s after its turn, the charge as its damage, 1 round each (never below
	 * 0), then 0.3 s (D 022F #445-#452, 01EE #60 / #61) H; W 0.112 s apart.
	 */
	public static final double DEADEYE_FIRE_START = 0.1, DEADEYE_SHOT_GAP = 0.1, DEADEYE_SHOT_DELAY = 0.066, DEADEYE_END = 0.3;
	/** The right button cancels it in 0.1 s (D 022F; W) H. */
	public static final double DEADEYE_CANCEL = 0.1;
	/** "It's high noon" 0.5 s after the cast (D 022F #621, M moment). */
	public static final double DEADEYE_LINE_AT = 0.5;

	// ---- Sharpshooter (01C7, his sub-role) ----
	/** Crit damage dealt takes 0.0075 s a point off Combat Roll's cooldown (D 01C7, as Sojourn's; BZ 2026-02-10 0.75 %) H. */
	public static final double SHARPSHOOTER = 0.0075;

	// ---- Perks (51E5; his live list D 006C.177) ----
	/**
	 * Bang Bang (minor): a second Flashbang thrown with the first at 25 m/s that goes 3.25 m farther, both x0.6 (45) (D 01E9 #203 /
	 * #206 H; the x0.6 M; BZ 2025-08-26 -40 %; W 10 m) H/M.
	 */
	public static final double BANG_SPEED = 25, BANG_RANGE = FLASH_RANGE + 3.25, BANG_DAMAGE = 0.6;
	/** Giddy Up (minor): +60 % speed from the roll's end, down to none over 1.5 s in 10 steps (D 51E5 H/M; BZ 2026-08-11) H. */
	public static final double GIDDY_SPEED = 0.6, GIDDY_TIME = 1.5;
	public static final int GIDDY_STEPS = 10;
	/** Rollin' Round-Up (major): 15 health for each round the roll reloads (D 51E5; BZ 2026-02-10) H. */
	public static final double ROUND_UP_HEAL = 15;
	/**
	 * Silver Bullet (major, in Fan the Hammer's place: hold the right button, the left fires it): 70, x2 on the head, piercing
	 * enemies, then 50 bleed over 1.5 s; 8 s; Combat Roll and Deadeye reset it (D 01D2 / 5FDA H; BZ 2026-06-16; W) H. The bleed in
	 * steps of 0.2 s (W 6.4 per 0.192 s; ours the tick-rounded step).
	 */
	public static final double SILVER_DAMAGE = 70, SILVER_BLEED = 50, SILVER_BLEED_TIME = 1.5, SILVER_BLEED_STEP = 0.2,
		SILVER_COOLDOWN = 8;

	/** Where his shots' look leaves from (right, up, ahead from his eyes, m): his revolver held at rest; ours. */
	public static final double MUZZLE_RIGHT = 0.22, MUZZLE_UP = -0.18, MUZZLE_AHEAD = 0.65;
}
