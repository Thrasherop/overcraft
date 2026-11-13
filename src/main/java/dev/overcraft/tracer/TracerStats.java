package dev.overcraft.tracer;

/**
 * Tracer's numbers. Sources: the game data (D: build 154088 of 2026-10-06, tools/reference/tracer-map.md, its value id or graph
 * node), the owner's Workshop recording (WS 2026-11-11: Log-2026-11-11-23-49-15, tools/workshop/analysis/tracer_ws.py, its output
 * in techniques/testing/runs/tracer/ws_2026-11-11.txt), Blizzard's patch notes (BZ) and the Overwatch wiki (W rev 410824):
 * tools/reference/tracer.md has every date. Her last live number change is BZ 2026-08-11 (Recall 13 -> 12 s, Temporal Regen in),
 * before the data build, which holds 12 s (02E1): the data's numbers are the live ones (BZ 2026-11-03 / 11-05 don't touch her).
 * Confidence: H read in the data or the sources agree, M one link inferred, L a guess; where no source gives a value it's ours.
 */
public final class TracerStats {
	private TracerStats() {
	}

	/** Damage, Flanker: 175 health, no armor or shields (D 32FC #125: 0001 175 / 02F1 0 / 02F2 0; BZ 2024-02-13 150 -> 175; W) H. */
	public static final int HEALTH = 175;

	/** Pulse Bomb's cost (D 0B00 1375 in its loadout, M that it's the cost; W 1375) H/M. */
	public static final double ULT_COST = 1375;

	/** Perk XP: 1575 to the minor, 2420 more to the major (D 12E0 / 12E2, 32FC #145; W 1575 / 3995) H. */
	public static final double PERK_MINOR_XP = 1575, PERK_MAJOR_XP = 1575 + 2420;

	// ---- Movement (her mover, 0003.075) ----
	/**
	 * Forward and sideways 6.0 m/s, back 5.4 (D mover m_CFD3E635 / m_19452F15 6.0, m_61D6B4E9 5.4; W Movement speed) H; WS
	 * 2026-11-11 measured 5.999 forward and sideways, 5.400 back, 5.600 diagonally back (the shared 14/15), from rest 3.36 / 5.28
	 * / 6.00 m/s over her first three 0.016 s ticks. Everything else in her mover is the default (D: crouch 3.0, jump 5.72 m/s
	 * (WS 5.72), gravity 17.5 (WS 0.28 m/s a 0.016 s tick), the air cap 5.5: WS shows a 6.0 jump slowing to 5.92 in the air).
	 */
	public static final double RUN_SPEED = 6.0;

	// ---- Pulse Pistols (0016) ----
	/** 5.75 a bullet (D 02D6 5.75, 32FC #111; BZ 2026-02-10 6 -> 5.75; W) H. */
	public static final double BULLET_DAMAGE = 5.75;
	/** Two bullets a shot, one from each pistol at once (D 1B2F 2, m_muzzles 1B9B / 1B9C, 0016 #384) H. */
	public static final int BULLETS = 2;
	/** 20 shots a second while the left button is held (D 032D 20, 32FC #123; W 40 bullets / s) H. */
	public static final double RATE = 20;
	/** The magazine: 40 (D 02E0 40, 32FC #117; W) H; 2 a shot (W "2 ammo per burst": 20 shots, 1 s; D m_79CE614D 1, L meaning) M. */
	public static final int AMMO = 40, AMMO_PER_SHOT = 2;
	/** Falloff from 10 m to 20 m, down to x0.3 (D 02DB 10 / 02DA 20, 3361 #132 0303 0.3; W 1.725 at 20 m) H; linear between (M). */
	public static final double FALLOFF_START = 10, FALLOFF_END = 20, FALLOFF_MIN = 0.3;
	/** x2 on the head (D m_66D29548 2; W) H. */
	public static final double CRIT = 2;
	/** Hitscan, 0.02 m across (D 28A3 0.02 = the global small size, 01C7 #1198; BZ 2026-10-06 0.04 -> 0.02) H; 100 m (D m_FEC435C6 100, L). */
	public static final double RADIUS = 0.02, RANGE = 100;
	/** The spread: 0 to 3.5 degrees over the first 3 shots (D 0 / 02D8 3.5 / 1E41 3; BZ 2025-07-22 3.0 -> 3.5; W) H. */
	public static final double SPREAD_MAX = 3.5, SPREAD_SHOTS = 3;
	/** Back to none: held 0.2 s after the last shot, then down at 4 a second (0.25 s from the most) (D 0016 #82 / #83 / #45) M. */
	public static final double SPREAD_HOLD = 0.2, SPREAD_RECOVER = 4;
	/** After she lets go, 0.2 s before the next burst (D 0016 #2 v52 -> #12 StateWait 0.2) H node, M meaning. */
	public static final double REFIRE_LOCK = 0.2;
	/** The reload: 1 s, the ammo back at 0.7 s, no fire until it's over (D 02D7 1.0, v6884 0.7, #1 / #91; W 1 s / 0.686) H/M. */
	public static final double RELOAD = 1.0, RELOAD_AMMO_AT = 0.7;
	/**
	 * Where her shots' look leaves from (right, up, ahead from her eyes, m): her two pistols' hp 0006 (D m_muzzles) as the
	 * first-person view draws them at rest: ours until measured on her drawn model (Pharah's lesson: read it where it's drawn).
	 */
	public static final double MUZZLE_SIDE = 0.2, MUZZLE_UP = -0.2, MUZZLE_AHEAD = 0.6;

	// ---- Blink (0024, Shift or the right button) ----
	/** 3 charges (D 2195 3, #59 / #60; W) H. */
	public static final int BLINK_CHARGES = 3;
	/**
	 * One charge back every 3 s, one at a time, the timer running while any is missing and not restarted by another Blink (D
	 * 02E3 3, #54 / #143 / #56) H; WS 2026-11-11: charges back at 10.10, 13.10, 16.11, 19.12, 22.13 after a first Blink at 7.10.
	 */
	public static final double BLINK_RECHARGE = 3;
	/**
	 * 7.5 m (D 1AED 7.5 on the blink node #34, M field; W) H; WS: 7.49-7.51 m, all of it within one of Overwatch's 0.016 s
	 * ticks (the Workshop's "in use" lasts 0.10 s after).
	 */
	public static final double BLINK_DISTANCE = 7.5;
	/** 0.1 s before the next press does anything (D #153 StateWait 0.1; W 0.096 s; WS "lasted 0.10") H. */
	public static final double BLINK_LOCKOUT = 0.1;
	/**
	 * How far up she may step over what's in her way, from where she is at that step of the Blink, not from where she started
	 * (critic d0c7e1f #3: a cap from the start stopped her 1.5 m up any stair). Data: the value 1.5 on the blink node
	 * (m_497165F8, kit_tracer_move.md: H value, L meaning; no graph reads it, the path is engine-side). Ours: reading it as a
	 * per-step step-up, so she follows stairs and ramps up as she follows them down (W "cannot blink over an obstacle she could
	 * not jump over"; WS: 1.00 m up a step and 1.39 m up a ledge seen) L/M. Down: she follows the ground down as far a step while
	 * she was on it (WS 1.40 m down a slope, landing on it), else stays level.
	 */
	public static final double BLINK_STEP = 1.5;

	// ---- Recall (001A, E) ----
	/** She goes back 3 s (D #590 v7125 = 3, the rewind node's m_6B5DA460; W) H; WS: to within 0.42 m of where she was 3.0 s before. */
	public static final double RECALL_HISTORY = 3;
	/** 0.9 s, phased throughout (D 1B7C 0.9; W; WS: 0.896 s, phased in all 56 ticks) H. */
	public static final double RECALL_TIME = 0.9;
	/**
	 * Where she reappears: 0.44 s in she is back where she was, with the health she had at the best of those 3 s and the speed she
	 * had there; until 0.9 s she holds still there while gravity keeps adding to that speed (WS 2026-11-11: still at the start
	 * for 0.43 s, then at once the old place, 165 -> 175 health, (4.25, -1.96, -4.24) m/s growing to -9.48 down at the end) M:
	 * one Recall measured. The data's opening (STU_54FEB21C) has no length of its own (D, H by search).
	 */
	public static final double RECALL_ARRIVE = 0.44;
	/** Her magazine full 0.72 s in (D #95 StateWait 0.9 x 0.8 -> message 007D -> 0016 #19) H nodes, M meaning. */
	public static final double RECALL_AMMO_AT = 0.72;
	/** 12 s, from its end (D 02E1 12, #1 onFinished -> #92; BZ 2026-08-11 13 -> 12; W) H. */
	public static final double RECALL_COOLDOWN = 12;
	/** Ult charge: a tenth of the health it gave back (D 001A #147 STU_008D30B1 x 0.1, M as D.Va's / Brigitte's reading). */
	public static final double RECALL_ULT_SHARE = 0.1;
	/** Not in her first 3 s after spawning (D #255 -> #254 3 s -> #253 the button enabled, L meaning) L. */
	public static final double RECALL_SPAWN_LOCK = 3;

	// ---- Pulse Bomb (0034 / its entity 0045 graph 0C1B, Q) ----
	/** 0.15 s from the press to the throw, then 0.2 s before she can fire (D #41 / #53; W 0.144 / 0.192; WS 0.15 / 0.19) H. */
	public static final double BOMB_CAST = 0.15, BOMB_RECOVERY = 0.2;
	/** 15 m/s (D 02D5 15; W) H, falling at 30 m/s² (D m_599AB8D7 v7389 30, M field) M; 0.22 m (D 2914 0.1 + 28A4 0.12) H. */
	public static final double BOMB_SPEED = 15, BOMB_GRAVITY = 30, BOMB_RADIUS = 0.22;
	/** Gone after 99 s unstuck (D lifetime 99) H. */
	public static final double BOMB_LIFETIME = 99;
	/** 5 to what it sticks to (D 1B7A 5, 32FC #150) H. */
	public static final double BOMB_STICK = 5;
	/** Its fuse: 1 s from sticking or landing (D 1B7B 1.0, 0C1B #617 / #136, on message 05D4 or 059D) H. */
	public static final double BOMB_FUSE = 1.0;
	/** 350 to 2 m, down to 70 at 4 m, linear (D 02CF 350, x0.2, 02D3 2 / 02D4 4, 0C1B #61) H; none past 4 m. */
	public static final double BOMB_DAMAGE = 350, BOMB_FULL = 2, BOMB_RADIUS_OUT = 4, BOMB_EDGE = 0.2;
	/** Half on herself (D 0C1B #155 x0.5; W) H. No knockback (D: none on the blast, H by absence). */
	public static final double BOMB_SELF = 0.5;
	/** Where it leaves her: her hp 0003 (D volley #21) as seen from her eyes (right, up, ahead): ours. */
	public static final double BOMB_RIGHT = 0.15, BOMB_UP = -0.15, BOMB_AHEAD = 0.5;

	// ---- Flanker (sub-role, 01C7 #2579) ----
	/**
	 * +50 from every health pack (D 23D3 50, #2563; BZ 2026-08-11 75 -> 50; W) H. Minecraft has none: ours, a potion of Healing on
	 * her (drunk, splashed, an arrow) is her health pack, for this and Blink Packs.
	 */
	public static final double FLANKER_PACK = 50;
	/**
	 * Ours: a lingering cloud of Healing re-applies itself every second she stands in it; it is one pack (counted once a cloud), and
	 * a Healing effect with no thrown source (a command's) one at most this often (s). Counted every time it was an endless pack
	 * (critic d0c7e1f).
	 */
	public static final double PACK_TICK_GAP = 1;

	// ---- Perks (51EE; her live list D 005E.177) ----
	/** Temporal Regen (minor): the regeneration starts after 3 s, not 6 (D #462 v42465 0.5 x 0B0F 6; W "100 % faster") H; BZ says 50 %. */
	public static final double TEMPORAL_REGEN_DELAY = 3;
	/** Kinetic Reload (minor): 12 rounds for each enemy her quick melee hits (D #393 message 278A 12; BZ 2026-06-18 20 -> 12) H. */
	public static final int KINETIC_AMMO = 12;
	/** Blink Packs (major): a health pack gives back a Blink charge (D #404 message 007F 1) H. */
	public static final int PACK_BLINKS = 1;
	/**
	 * Quantum Entanglement (major): Recall gives 50 overhealth, held 6 s then going at 25 a second (D #416 50, #415 6 s, #417 25:
	 * M on the 25 a second), and her magazine to 60 (D #410 27E7 20: v53 = 40 + 20) H, the extra kept 6 s (D 0016 #186) then
	 * going at 20 a second (ours: the chase rate's unit isn't read) L.
	 */
	public static final double QUANTUM_OVERHEALTH = 50, QUANTUM_HOLD = 6, QUANTUM_DECAY = 25, QUANTUM_AMMO_DECAY = 20;
	public static final int QUANTUM_AMMO = 20;
}
