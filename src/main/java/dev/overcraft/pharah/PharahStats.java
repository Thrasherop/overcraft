package dev.overcraft.pharah;

/**
 * Pharah's numbers. Sources: the game data (D: build 154088 of 2026-10-06, tools/reference/pharah-map.md, its value id or graph
 * node), Blizzard's patch notes (BZ) and the Overwatch wiki (W rev 408633): tools/reference/pharah.md has every date and
 * revision. Her last live change is BZ 2026-08-11 (Concussive Blast 7 -> 8 s), before the data build, which holds 8 s (092B):
 * the data's numbers are the live ones (BZ 2026-11-03 / 11-05 don't touch her). Confidence: H read in the data or the sources
 * agree, M one link inferred, L a guess; where no source gives a value it's ours and says so.
 */
public final class PharahStats {
	private PharahStats() {
	}

	/** Damage, Recon: 225 health, no armor or shields (D 3B6E #7: 0925 225 / 0B94 0; BZ 2024-07-12 250 -> 225; W) H. */
	public static final int HEALTH = 225;

	/** Barrage's cost (D 0AF5 2200, M that it's hers; W 2200) H/M. */
	public static final double ULT_COST = 2200;

	/** Perk XP: 1730 to the minor, 2600 more to the major (D 12D1 / 12D2, 3B6E #64; W) M. */
	public static final double PERK_MINOR_XP = 1730, PERK_MAJOR_XP = 1730 + 2600;

	// ---- Rocket Launcher (0023) ----
	/** The magazine (D 0928 6; W) H. */
	public static final int AMMO = 6;
	/** 0.8 s between rockets (D 0023 #771 v173; BZ 2024-02-13 0.85 -> 0.8; W) H; 0.55 after the one that empties it (D v71594 0.25) H. */
	public static final double RECOVERY = 0.8, RECOVERY_LAST = 0.55;
	/** The reload: 1.5 s, the rockets back at 0.6 s, no fire until it's over (D 1D94 1.5, #72 x0.4, #33 onFinished: M; W 1.5) M. */
	public static final double RELOAD = 1.5, RELOAD_AMMO_AT = 0.6;
	/** A direct hit's 40 (D 0927; W) H, then the splash. */
	public static final double ROCKET_DIRECT = 40;
	/**
	 * The splash: 80 to 0.5 m, down to x0.308 (24.6) at 2.5 m, linear between (D 0926 80, 2390 0.5, 0B66 2.5, 0B1D 0.308; the
	 * tooltip "0.5 - 2.5 m" H; linear M). W has 0.8 m for the full ring: the data's 0.5 is used.
	 */
	public static final double ROCKET_SPLASH = 80, ROCKET_SPLASH_FULL = 0.5, ROCKET_SPLASH_RADIUS = 2.5, ROCKET_SPLASH_EDGE = 0.308;
	/** Her own rockets hurt her a quarter (D 0B68 0.25, M field; BZ 2024-02-13 50 -> 25 %; W) H. */
	public static final double ROCKET_SELF = 0.25;
	/**
	 * Knockback: 5 m/s away and 8 m/s up, x0.8 inside 0.5 m, to 0 at 2.5 m (D 1D9B / 1D9C / 1D9D / 1D9E: max 7.55 m/s; W 7.5) H/M;
	 * on herself x1.875 (D 0B69, M field: 14.15 m/s; W 14.2) H.
	 */
	public static final double ROCKET_KNOCK_AWAY = 5, ROCKET_KNOCK_UP = 8, ROCKET_KNOCK_RING = 0.8, ROCKET_KNOCK_SELF = 1.875;
	/** 40 m/s, no drop, 5 s (D 28F9 40; BZ 2024-02-13 35 -> 40; W) H; 0.32 m (D 28F8 0.2 + the global 0.12: BZ 2024-12-10) H/M. */
	public static final double ROCKET_SPEED = 40, ROCKET_RADIUS = 0.32, ROCKET_LIFETIME = 5;
	/**
	 * Where a rocket leaves from, from her eyes in her look's frame (right, up, ahead, m): the launcher's hp 0006 as her 1P idle
	 * holds it (D 0023 #2 m_muzzles; measured on the posed 1P model: research\pharah\scripts\muzzle_1p_pharah.py) M.
	 */
	public static final double MUZZLE_RIGHT = 0.075, MUZZLE_UP = 0.13,MUZZLE_AHEAD = 0.945;

	// ---- Jump Jet (0027, Shift) ----
	/** A press, not a held charge (D 0027 #82: no charge state; W) H: 17 m/s up and 7 along the move input (D #36 1DA0 / 1D9F / 1DA1) H. */
	public static final double JUMP_JET_UP = 17, JUMP_JET_ALONG = 7;
	/** Its state lasts 0.85 s (D 1E1E, #109) H; 14 s cooldown from its end (D 092C, #379; BZ 2024-02-13 10 -> 14; W) H. */
	public static final double JET_TIME = 0.85, JUMP_JET_COOLDOWN = 14;
	/** +50 fuel over 0.85 s, into overfuel (D 0025 #764 / #762) H; W: "50 % over 0.9 s". */
	public static final double JET_FUEL = 50;

	// ---- Jet Dash (4B92, the right button) ----
	/** 19 m/s along the move input (forward without) and 8 up (D #36 1E29 / 1E2A / 1E2B, #545) H; 0.85 s (D v9110, M). */
	public static final double JET_DASH_ALONG = 19, JET_DASH_UP = 8;
	/** 9 s cooldown from its end (D 1A89, #523; BZ 2024-12-10 8 -> 9; W) H. No fuel of its own (D) H. */
	public static final double JET_DASH_COOLDOWN = 9;

	// ---- Hover Jets (0025, hold Jump) ----
	/** Fuel: 100 (D 0B18) H; overfuel to 150 (D 0BCB 0.5), 200 with Fuel Stores (D #730) H. */
	public static final double FUEL_MAX = 100, OVERFUEL_MAX = 150, OVERFUEL_MAX_STORES = 200;
	/** 50 a second while hovering: 2 s from full (D 1A6A, #30; W) H. */
	public static final double FUEL_DRAIN = 50;
	/** 45 a second back, on the ground only, 0.3 s after landing or letting go (D 1E4F / 1E52, #28 / #33; W) H/M. */
	public static final double FUEL_REGEN = 45, FUEL_REGEN_DELAY = 0.3;
	/** Overfuel is held 5 s, then drains to 100 at 30 a second, 80 while hovering (D #682 / #681 / #688) M. */
	public static final double OVERFUEL_HOLD = 5, OVERFUEL_DECAY = 30, OVERFUEL_DECAY_HOVER = 80;
	/**
	 * The push: 50 m/s² up (55 while falling) until she rises at 2.9 m/s (D 0B9A / 0B99, #650: M meaning). W says she rises at
	 * 4.66 m/s; the data's 2.9 is used (to measure against a clip: pharah-map.md "Open").
	 */
	public static final double HOVER_PUSH = 50, HOVER_PUSH_FALLING = 55, HOVER_RISE = 2.9;
	/** During a jet: the push 80, 30 with no fuel, the hover on without the key and no fuel spent (D 0025 #698 / #694 / #29) H/M. */
	public static final double JET_PUSH = 80, JET_PUSH_EMPTY = 30;
	/** With no fuel the key held still slows her fall: a 7.5 m/s² push and a fall of 8 m/s at most (D #655, 1E54; BZ 2024-02-13; W) H. */
	public static final double EMPTY_PUSH = 7.5, EMPTY_FALL = 8;
	/** Horizontal speed +30 % while she hovers with fuel (D 1E36 on x786680F6, M field; BZ 2026-04-14 40 -> 30 %; W) H. */
	public static final double HOVER_SPEED = 0.3;
	/** From the ground it takes 0.2 s of holding (D #22, M); in the air it's at once. */
	public static final double HOVER_FROM_GROUND = 0.2;

	// ---- Concussive Blast (007B, E) ----
	/** 8 s cooldown (D 092B; BZ 2026-08-11 7 -> 8; W) H. */
	public static final double CONCUSSIVE_COOLDOWN = 8;
	/** It leaves 0.167 s into the cast; 0.5 s before she can fire again (D #85 / #257 / #61) H. */
	public static final double CONCUSSIVE_CAST = 0.1667, CONCUSSIVE_RECOVERY = 0.5;
	/** 60 m/s, 0.225 m (D 28FB / 28FC 0.15 + the global 0.075, M) H. */
	public static final double CONCUSSIVE_SPEED = 60, CONCUSSIVE_RADIUS = 0.225, CONCUSSIVE_LIFETIME = 5;
	/** An 8 m blast (D 0B7B; BZ 2024-07-09 6 -> 8; W) H; 30 to what it hits directly, none in the blast (D 092E / 0B17; W) H. */
	public static final double CONCUSSIVE_BLAST = 8, CONCUSSIVE_DIRECT = 30;
	/**
	 * Its push: 20 m/s away and 5 up anywhere in the blast, no falloff (D 1E2D / 1E2E, one ring: M) H; a direct hit 10 / 3 more
	 * (D 1E32 / 1E33: 31.1 m/s; W) H. She takes it too, all of it (D m_2FE97560 1, M), and none of its damage (D 0) H.
	 */
	public static final double CONCUSSIVE_AWAY = 20, CONCUSSIVE_UP = 5, CONCUSSIVE_DIRECT_AWAY = 10, CONCUSSIVE_DIRECT_UP = 3;
	/** Where it leaves from: her left wrist (D hp 0038), from her eyes in her look's frame (left, down, ahead: ours from 3P 0.53 / 1.18) M. */
	public static final double CONCUSSIVE_LEFT = 0.35, CONCUSSIVE_UP_OFF = -0.3, CONCUSSIVE_AHEAD = 0.5;

	// ---- Barrage (002B, Q) ----
	/** 2.5 s (D 1A5D) H, no cast time (D #54, M), 30 rockets a second (D 1E46: 75; W) H. */
	public static final double BARRAGE_TIME = 2.5, BARRAGE_RATE = 30;
	/** Each: 10 direct, 25 splash to 0.5 m, x0.3 (7.5) at 1.5 m (D 092A / 0929 / 1E37 / 1E38 / 1E47; BZ 2026-04-14 30 -> 25; W) H. */
	public static final double BARRAGE_DIRECT = 10, BARRAGE_SPLASH = 25, BARRAGE_FULL = 0.5, BARRAGE_RADIUS = 1.5, BARRAGE_EDGE = 0.3;
	/** 45 m/s, 0.075 m, an 8 degree spread (D 28FA / v54937 / 0B7D; W) H/M; no knockback (D) H; half on herself (D 0074 #331) M. */
	public static final double BARRAGE_SPEED = 45, BARRAGE_SIZE = 0.075, BARRAGE_SPREAD = 8, BARRAGE_SELF = 0.5;
	/** She moves at -20 % (D 24A8 0.8 on x786680F6, #377: M field; BZ 2026-04-14 "can move"; W 4.4 m/s) M. */
	public static final double BARRAGE_SLOW = 0.2;
	/**
	 * The pods it fires from, in turn (D #21: hp 0034 / 0035 / 0037 / 0036 / 0037 / 0035 / 0034 / 0036), from her feet in her
	 * facing's frame (right, up, ahead, m): shoulders (+-0.273, 1.564, -0.052), hips (-+0.159, 0.886, 0.051) H (her rest pose).
	 */
	public static final double[][] BARRAGE_PODS = {
		{-0.273, 1.564, -0.052}, {0.273, 1.564, -0.052}, {-0.159, 0.886, 0.051}, {0.159, 0.886, 0.051},
		{-0.159, 0.886, 0.051}, {0.273, 1.564, -0.052}, {-0.273, 1.564, -0.052}, {0.159, 0.886, 0.051}};

	// ---- Recon (the sub-role, 0DAD) ----
	/** Her damage on an enemy under half its health shows it through walls 3.5 s (D 23D8; BZ 2026-04-14 5 -> 3.5; W Sub-Roles) H. */
	public static final double RECON_TIME = 3.5, RECON_BELOW = 0.5;

	// ---- Perks (51E9) ----
	/** Helix Shields: 125 of her health are shields (D #331 / #332; BZ 2025-03-25 75 -> 125; W) H; a direct hit starts their regeneration (D #361, M; BZ 2025-06-24) H. */
	public static final int HELIX_SHIELDS = 125;
	/** Concussive Force: the blast does 30 to 4 m, 15 at 8 m (D #707 / #356) H; W "up to 30" (BZ 2026-05-12 50 -> 30). */
	public static final double FORCE_SPLASH = 30, FORCE_FULL = 4, FORCE_EDGE = 0.5;
	/** Fuel Stores: Jet Dash gives 50 fuel over 0.85 s, overfuel to 200 (D 0025 #758 / #730; W) H. */
	public static final double STORES_FUEL = 50;
	/**
	 * Rocket Salvo: a jet arms it for 5 s (D 0027 #367 / 4B92 #108; the window M); the next rocket brings 2 mini-rockets 0.1 s
	 * later (D #77), each 10 direct + 20 splash to 0.5 m, x0.3 at 1.5 m, 40 m/s (D 7769) H. Their spread 0.1 (D, unit L): ours 0.2 m apart.
	 */
	public static final double SALVO_WINDOW = 5, SALVO_DELAY = 0.1, SALVO_DIRECT = 10, SALVO_SPLASH = 20, SALVO_SPEED = 40, SALVO_APART = 0.2;
	public static final int SALVO_COUNT = 2;
}
