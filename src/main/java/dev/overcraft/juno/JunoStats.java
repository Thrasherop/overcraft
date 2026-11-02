package dev.overcraft.juno;

/**
 * Juno's numbers (tools/reference/juno.md has every quote, revision and date; the game data map is juno-map.md, build 154088
 * of 2026-10-06, with its detail in E:\Overwatch Archive\research\juno\work_kit\kit_juno.md). Sources: the game data (D, with
 * its value id or graph node), Blizzard patch notes (BZ), the Overwatch wiki (W), and the owner's Workshop recording of her
 * in the real game (R: 2026-11-01, `tools/workshop/analysis/juno.py`; 25 double jumps, 7 glides, the hovers). No patch has
 * changed her kit since the data build (BZ 2026-10-06: hit volumes only), so the data's numbers are the live ones; where none
 * gives a value, it's ours and says so. Filled in step by step (juno-plan.md, "Order").
 */
public final class JunoStats {
	private JunoStats() {
	}

	/** Support, Survivor: 75 health and 150 shields (D 4448 #119: 0BA4 / 0BA6; W; BZ). */
	public static final int HEALTH = 75;
	public static final int SHIELDS = 150;

	/** Orbital Ray's cost (D 0B1B 2900; W 2900; BZ only percentages, ~2894). */
	public static final double ULT_COST = 2900;

	// ---- Martian Overboots (4441) ----
	/**
	 * The double jump: her vertical speed set to 10.8 m/s, her horizontal speed kept (R: 10.80 every one of 13, whatever she was
	 * doing before; W 10.8; D x1.8 of the mover's 6.0 jump field, #570), from 0.1 s after leaving the ground (D #244, M; R the
	 * earliest 0.16 s), once per airtime (twice with Lift Off, D #652), back on landing. About 3.4 m up from a standstill (R).
	 */
	public static final double DOUBLE_JUMP_SPEED = 10.8, DOUBLE_JUMP_FROM = 0.1;
	public static final int AIR_JUMPS = 1, AIR_JUMPS_LIFT_OFF = 2;
	/**
	 * The hover: jump held in the air brings her fall to 3 m/s (D 1A5E, #382; R exactly -3.00), once no ground is within 2.5 m
	 * below (D #376, M; R only the tall drops caught it). Not at once: from -9.2 m/s it eased to -3.1 in 0.27 s, the gap shrinking
	 * about 16 per second (R: -6.86, -5.67, -4.95, -4.47 ... at 62.5 ticks a second).
	 */
	public static final double HOVER_FALL = 3, HOVER_CLEARANCE = 2.5, HOVER_EASE = 16;

	// ---- Glide Boost (47B7) ----
	/**
	 * A 14 m/s horizontal burst added along her move input (forward without), vertical speed cancelled (D #589; R 5.5 -> 19.13
	 * one tick in, 5.81 -> 19.44: 14 less a tick's slowing; W's 19.1 is the total from a run).
	 */
	public static final double GLIDE_BURST = 14;
	/**
	 * Its slowing back to her glide speed (R, every glide alike): about 3 % a tick (1.86 x her speed per second) down to 12.45
	 * m/s, then a steady 23 m/s² (1.86 x 12.45), to her +40 % run speed (7.7 m/s) at 0.5 s; held there. With no key held she
	 * keeps her speed (R 7.3-7.7 m/s): the glide goes on.
	 */
	public static final double GLIDE_SLOWING = 1.86, GLIDE_SLOWING_FLOOR = 12.45;
	/** Up to 4 s (D 13BB); a second press ends it from 0.4 s (D #793 / #799: toggle, the default). */
	public static final double GLIDE_TIME = 4, GLIDE_CANCEL_FROM = 0.4;
	/**
	 * Its running-out sound: on hers with 0.4 s left (47B7 #642 -> #544 op71(v215, 0.4) -> #545 5843), on the others' copy 3.0 s
	 * in (B924's running-out set, delayed 3.0 s in its Play event) (H).
	 */
	public static final double GLIDE_ENDING = 0.4, GLIDE_ENDING_3P = 3.0;
	/** Its mover: air acceleration 12, air speed max 9 (D #528); +40% move speed (D 1A38, #742); gravity off (D #780 / #761). */
	public static final double GLIDE_AIR_ACCELERATE = 12, GLIDE_MAX_AIR = 9, GLIDE_SPEED = 0.4;
	/**
	 * Over a step or up a slope it rises at up to 3 m/s (D #812-#816 x830FF3AA +3; R the glides' vertical speed tops at 3.00,
	 * 12.5 m up a stair in one glide); ours 3 m/s while something blocks her.
	 */
	public static final double GLIDE_CLIMB = 3;
	/** Knockback taken while gliding: -50% (D x0A04B51B = 1DF3 -0.5, #774). */
	public static final double GLIDE_KNOCKBACK = 0.5;

	// ---- Mediblaster (4445): hitscan volleys (W; BZ 2026-10-06's small hitscan size) ----
	/** The magazine (D 4445; W 180); its reload 1.5 s, full at 0.75 s (D; W 1.5 after an unsourced edit, was 1.8). */
	public static final int MEDIBLASTER_AMMO = 180;
	public static final double RELOAD = 1.5, RELOAD_FULL_AT = 0.75;
	/** A volley: 12 rounds at 34 a second, then 0.45 s before the next (D: one volley per 0.80 s; BZ 2024-09-03 34/s; W 15.5/s). */
	public static final int VOLLEY = 12;
	public static final double VOLLEY_RATE = 34, VOLLEY_RECOVERY = 0.45;
	/** Each round: 7.5 to an enemy, 6 healing to an ally (D 1381 / 1382; BZ 2024-09-03 / 2024-10-15; W). */
	public static final double ROUND_DAMAGE = 7.5, ROUND_HEAL = 6;
	/** Its size: 0.02 m against enemies, 0.22 m for allies (D; BZ 2026-10-06 small hitscan 0.02; W). */
	public static final double ROUND_RADIUS_ENEMY = 0.02, ROUND_RADIUS_ALLY = 0.22;
	/** Head hits x1.5, enemies only (BZ 2025-12-09; W; D x2 read as M). */
	public static final double ROUND_CRIT = 1.5;
	/**
	 * Falloff: full to 25 m, 30% from 35 m, damage and healing (BZ 2024-10-15 / 2025-07-22 change it, so it is live; W the 30%;
	 * D has the values but no node of hers reads them: the engine's, M). Out to 60 m (ours).
	 */
	public static final double FALLOFF_START = 25, FALLOFF_END = 35, FALLOFF_MIN = 0.3, RANGE = 60;
	/** Spread (D _; W 0.85 degrees, M). */
	public static final double SPREAD = 0.85;
	/** Glide Boost (ABILITY 1): its cooldown from the glide's end (D 47B7 #538, 1625 6 s; W 6). */
	public static final double GLIDE_COOLDOWN = 6;
	/** Hyper Ring (ABILITY 2): 14 s from its cast's end (D 4857; W 14; M when it starts). */
	public static final double RING_COOLDOWN = 14;
	/** Its cast 0.55 s, the ring thrown 0.25 s in (D 4857, anim 0x479). */
	public static final double RING_CAST = 0.55, RING_THROW = 0.25;
	/** It flies 35 m/s for 0.5 s (17.5 m, or to a wall), then hangs until 6 s are up; a 4.5 m ring (D 4858; W 4 m: D). */
	public static final double RING_SPEED = 35, RING_FLIGHT = 0.5, RING_LIFE = 6, RING_RADIUS = 4.5;
	/** The ring's end sound, this long before its 6 s run out (4858 #463 -> #544 op71(v215, 0.15) -> #508 53CA, H). */
	public static final double RING_END_SOUND = 0.15;
	/** Each ally through it +50% speed for 3 s, once a ring (D 4858; BZ; W; the recording: Juno at 8.12-8.25 m/s after hers). */
	public static final double RING_BOOST = 0.5, RING_BOOST_TIME = 3;

	// ---- Orbital Ray (4A44, 501A, 54A6) ----
	/** Its cast 0.8 s, the ray landing 0.5 s in, 4 m ahead (D 4A44 0x175). */
	public static final double RAY_CAST = 0.8, RAY_LAND = 0.5, RAY_AHEAD = 4;
	/** 8 s, 8 m round, moving at up to 2.25 m/s (up over 0.5 s, down over its last 1.25 s) (D 501A; BZ 2024-10-15 2.25; W). */
	public static final double RAY_TIME = 8, RAY_RADIUS = 8, RAY_SPEED = 2.25, RAY_RAMP_UP = 0.5, RAY_RAMP_DOWN = 1.25;
	/** Allies inside: 85 a second and +30% damage, lingering 1 s (D 501A / 54A6; W; the linger W). */
	public static final double RAY_HEAL = 85, RAY_DAMAGE_BOOST = 0.3, RAY_LINGER = 1;
	/** The ray's ending sound, with this long of it left (501A #22 op71(v29029, 2.7) -> #23 5AD2, H). */
	public static final double RAY_ENDING = 2.7;
	/** A target's lock tick file (5C17's 0.02 s stream, built as 1.0 s: the longest lock), started again as it ends. */
	public static final double LOCKING_LOOP = 1.0;
	/** Pulsar Torpedoes (the right button): 10 s, 2 s when nothing was locked (D 4695 #929-#932, 16FC / 2396; BZ 2024-08-20; W). */
	public static final double TORPEDO_COOLDOWN = 10, TORPEDO_COOLDOWN_NONE = 2;
	/** The lock: up to 4 s (D 1AF0 via v567); 40 m, 45 once a target's lock has begun (D 1A57 40 + 5; BZ 2024-08-20; W). */
	public static final double LOCK_WINDOW = 4, LOCK_RANGE = 40, LOCK_FORGIVE = 5;
	/** Each target's lock time: 0.35 s at 5 m or nearer to 1.0 s at 40 m (D 4B99 #193, 2BB8 / 2BB7; BZ 2026-08-11 0.35; W). */
	public static final double LOCK_MIN = 0.35, LOCK_MAX = 1.0;
	/**
	 * Near her aim: a cone this wide (degrees; D v8451 103 on StateTrackTargets, L: read as the cone, which is the game's
	 * default field of view: anyone on her screen). Ours to check by play.
	 */
	public static final double LOCK_CONE = 103;
	/** One torpedo every 0.08 s (D v9805, #772); 24 m/s (D 1550, #931), the enemy's launched at 0.3 of it (D 2960); 5 s life (D). */
	public static final double TORPEDO_GAP = 0.08, TORPEDO_SPEED = 24, ENEMY_LAUNCH = 0.3, TORPEDO_LIFE = 5;
	/** On an enemy 85 (D 1441; BZ 2024-08-20; W); on an ally 85 at once and 50 over 2.5 s (D 1442 / 1443 / 1A3F; BZ; W). */
	public static final double TORPEDO_DAMAGE = 85, TORPEDO_HEAL = 85, TORPEDO_HEAL_OVER_TIME = 50, TORPEDO_HEAL_TIME = 2.5;

	// ---- Perks (the live list D 0065.177) ----
	public static final double PERK_MINOR_XP = 2110, PERK_MAJOR_XP = 2110 + 3165;   // D 12B9 / 12BA; W 2110 / 5275
	/** Familiar Vitals (minor): locks on allies take this share of the time (D 4B99 #260 x (1 - 0.35); BZ "35% faster"). */
	public static final double FAMILIAR_VITALS = 0.65;
	/** Locked On (minor): -1 s of the torpedoes' cooldown per enemy hit (D 522E #357-#360; BZ 2025-08-26), at most 5 s (W). */
	public static final double LOCKED_ON = 1, LOCKED_ON_MAX = 5;
	/** Faster Blaster (major): while gliding, the Mediblaster fires on, 22 rounds a second (D #1308-#1316; BZ 2026-06-16 22). */
	public static final double FASTER_BLASTER_RATE = 22;
}
