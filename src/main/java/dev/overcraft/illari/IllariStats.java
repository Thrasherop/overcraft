package dev.overcraft.illari;

/**
 * Illari's numbers (#135), each with its source: D = her game data, build 154088 (tools/reference/illari-map.md: graph and node), BZ =
 * Blizzard's live patch notes by date, W = the wiki (Weird Gloop's API, Illari rev 408912) (tools/reference/illari.md), ours = no source
 * gives it. Confidence after the source: H read / agreed, M one link inferred, L a guess. The build predates BZ 2026-11-03, so its charge
 * time and pylon heal are the patch's.
 */
public final class IllariStats {
	private IllariStats() {
	}

	/** 250 health (D 0B30; BZ 2024-02-13 200 -> 250) H. */
	public static final int HEALTH = 250;

	// ---- Solar Rifle (3A65) ----
	/** Full charge in 0.575 s (BZ 2026-11-03 0.65 -> 0.575; the data's 3A65 #1751 still 0.65) H. */
	public static final double CHARGE_TIME = 0.575;
	/** The charge starts 0.25 s after a shot (D #2922 v30010; BZ 2024-04-16 0.2 -> 0.25) H. */
	public static final double RECOVERY = 0.25;
	/** No shot below 40 % charge (D #1827: v50965 = 0.4 x the charge) H. */
	public static final double MIN_CHARGE = 0.4;
	/** 25 at the least charge to 75 at full (D 11C5 / 0B31; BZ 2024-12-19 / 2026-01-08 70/20 -> 75/25) H. */
	public static final double DAMAGE_MIN = 25, DAMAGE_MAX = 75;
	/** Its damage curve: charge 0.4 -> 0, 0.7 -> 0.5, 1 -> 1 of the 50 on top (D #3042) H/M. */
	public static final double CURVE_MID_CHARGE = 0.7, CURVE_MID = 0.5;
	/** Falloff from 30 m to 50 m, down to x0.3 (D 11C9 / 11CA, volley #555; W) H. */
	public static final double FALLOFF_START = 30, FALLOFF_END = 50, FALLOFF_MIN = 0.3;
	/** x1.5 on the head (D 1CEB; W) H. */
	public static final double CRIT = 1.5;
	/** The shot's radius: 0.04 m (D 28A2; BZ 2026-10-06 global hitscan 0.07 -> 0.04, BZ 2026-05-12 her own 0) H. */
	public static final double SHOT_RADIUS = 0.04;
	/** Hitscan range (ours: Overwatch's hitscan has none to speak of; Cassidy's 200). */
	public static final double SHOT_RANGE = 200;
	/** 14 shots, 1.5 s reload, the ammo back 1.08 s in (D 11C8 / 11C7, #2862: 0.72 x 1.5; BZ 2024-08-20 16 -> 14) H. */
	public static final int AMMO = 14;
	public static final double RELOAD = 1.5, RELOAD_AMMO_AT = 1.08;
	/** A shot needs 0.15 s since the last one and 0.2 s since the beam (D #544) H. */
	public static final double SHOT_GAP = 0.15, AFTER_BEAM = 0.2;

	// ---- the healing beam (3A65) ----
	/** 115 a second (D 0B32; BZ 2024-04-16 105 -> 115) H. */
	public static final double BEAM_HEAL = 115;
	/** 20 m (D 11F5; BZ 2025-05-20 15 -> 20), 0.25 m round (D 293A) H. */
	public static final double BEAM_RANGE = 20, BEAM_RADIUS = 0.25;
	/** Solar energy: 100, 3 s of beam (D 11F6; BZ 2026-03-10 3.5 -> 3 s) H. */
	public static final double ENERGY = 100, ENERGY_DRAIN = 100 / 3.0;
	/** Back at 35 a second (D 1D21; BZ 2026-02-10 43 -> 35 %), 0.6 s after the beam (D 1DCD) H. */
	public static final double ENERGY_REGEN = 35, ENERGY_REGEN_DELAY = 0.6;
	/** Emptied: the regeneration from ~1.0 s later and the beam locked until full (D #2565 / #2573) H/M. */
	public static final double ENERGY_EMPTY_DELAY = 1.0;

	// ---- Healing Pylon (3DF4; the pylon's 59EA / 3DF5 / 3DF6) ----
	/** Thrown 0.1 s into a 0.6 s cast at 25 m/s, falling at 15 m/s², 0.15 m (D 3DF4 #89 / #115, 293C / 293B; gravity M) H. */
	public static final double PYLON_THROW_AT = 0.1, PYLON_CAST = 0.6, PYLON_SPEED = 25, PYLON_GRAVITY = 15, PYLON_SIZE = 0.15;
	/** 75 health + 50 shields (D 15A3 / 15A4; BZ 2024-06-20) H. */
	public static final double PYLON_HEALTH = 75, PYLON_SHIELDS = 50;
	/** Built 1.25 s after it sets down (D 1387; W 1.3) H. */
	public static final double PYLON_BUILD = 1.25;
	/** 50 a shot, 25 on her (BZ 2026-11-03 40 -> 50; D 1204 / 1206 still 40 / 20; half on herself BZ 2024-06-20) H. */
	public static final double PYLON_HEAL = 50, PYLON_SELF_HEAL = 25;
	/** One shot every 0.8 s, to the ally in 15 m with the lowest health share (D 1D37, 3DF5; W) H/M. */
	public static final double PYLON_EVERY = 0.8, PYLON_RANGE = 15;
	/** Its heal shot flies at 55 m/s, homing (D 3DF6 #216; W's 25 m/s is the throw) H. */
	public static final double PYLON_SHOT_SPEED = 55;
	/** Cooldown 7 s from the throw; 14 s from its destruction; no new one within 3 s of a hit (D 11FB / 11FC / 1D36; W 14 / 7 / 3) H/M. */
	public static final double PYLON_COOLDOWN = 7, PYLON_COOLDOWN_HIT = 14, PYLON_HIT_WINDOW = 3;
	/** Its shields come back 3 s after a hit (W Hit points: Shields; ours that the pylon's do as a hero's) M. */
	public static final double PYLON_SHIELD_DELAY = 3, PYLON_SHIELD_REGEN = 30;
	/** Rapid Construction: built x0.25 as fast (D 51F3), cooldowns 2 s shorter: 12 / 5 s (BZ 2026-06-16 1.5 -> 2 s; W; the data's 6 L) H. */
	public static final double RAPID_BUILD = 0.25, RAPID_COOLDOWN_HIT = 12, RAPID_COOLDOWN = 5;
	/** A monster within 1 m of the pylon hits it every 1 s for its attack damage (ours: Minecraft's mobs can't target a non-creature). */
	public static final double PYLON_MOB_REACH = 1.0, PYLON_MOB_EVERY = 1.0;

	// ---- Outburst (3B38) ----
	/** 25 to enemies within 8 m, 8 s cooldown (D 11F8 / 11FA / 11F9; BZ 2024-06-20 10 -> 25, BZ 2026-02-10 7 -> 8 s) H. */
	public static final double OUTBURST_DAMAGE = 25, OUTBURST_RADIUS = 8, OUTBURST_COOLDOWN = 8;
	/** Their push: 18 away, 5 up, x1 to 5.6 m easing to x0.3 at 8 m (D 1D35 / 1D34 m_E33EFEE1 / m_583C208B, 3B38) H. */
	public static final double OUTBURST_AWAY = 18, OUTBURST_UP = 5, OUTBURST_FULL = 5.6, OUTBURST_EDGE = 0.3;
	/** Her launch: 6 m/s up on a standing tap, 13.5 moving or held, along (x, 0.6, z) tapped / (x, 2, z) held (D 0EC2 / 0EC3) H. */
	public static final double OUTBURST_TAP = 6, OUTBURST_HELD = 13.5, OUTBURST_TAP_RISE = 0.6, OUTBURST_HELD_RISE = 2;
	/** The burst 0.2 s into a 0.8 s cast (D anim 0677, #635) H. */
	public static final double OUTBURST_AT = 0.2, OUTBURST_CAST = 0.8;
	/** Sunburn: 50 over 3 s on each enemy hit (D 51F3 2.984 s; BZ 2026-03-10 70 -> 50) H. */
	public static final double SUNBURN_DAMAGE = 50, SUNBURN_TIME = 3;

	// ---- Captive Sun (3CAB; the ball 3CB1, the mark 3DFF) ----
	/** 2975 points (D 0AE6; W) H/M. */
	public static final double ULT_COST = 2975;
	/** The launch: 1.2 s, 12 m/s up, the beam's energy and the ammo full 0.6 s in (D anim 01B8 #822, #1076) H. */
	public static final double SUN_LAUNCH = 1.2, SUN_LAUNCH_UP = 12, SUN_REFILL_AT = 0.6;
	/** Flight: 5.2 s from the cast (W), x1.45 her speed (D 1D3D), knockback taken halved (D 1DF9) H/M. */
	public static final double SUN_FLIGHT = 5.2, SUN_FLIGHT_SPEED = 1.45, SUN_KNOCK_TAKEN = 0.5;
	/** The ball fires on the left button, or by itself 4 s after the cast (D 1388) H/M. */
	public static final double SUN_AUTO_FIRE = 4;
	/** The ball: 30 m/s, radius 0.45 m, 10 s (D 293E / 293D; BZ 2025-12-09 0.75 -> 0.45; W rev 408912 0.45 radius) H. */
	public static final double BALL_SPEED = 30, BALL_SIZE = 0.45, BALL_LIFE = 10;
	/** 5 direct, 50 splash with no falloff (D 1D38 / 1D39) H; 10 m (BZ 2024-12-10 8 -> 10; the data's 1216 still 8) L. */
	public static final double BALL_DIRECT = 5, BALL_SPLASH = 50, BALL_RADIUS = 10;
	/** The mark: 6 s (BZ 2023-08-24 7 -> 6; D 1218 7) L; slowed 40 % for 1 s, back to none over the rest (D 3DFF) M. */
	public static final double MARK_TIME = 6, MARK_SLOW = 0.4, MARK_SLOW_HOLD = 1;
	/** Sunstruck: 90 damage on the marked sets it off 0.8 s later, 160 inside 1 m down to 120 at 5 m (D 1A34, 1212 / 1213, 1D3A / 1D3B) H. */
	public static final double SUNSTRUCK_AT = 90, SUNSTRUCK_DELAY = 0.8, SUNSTRUCK_DAMAGE = 160, SUNSTRUCK_EDGE_DAMAGE = 120,
		SUNSTRUCK_FULL = 1, SUNSTRUCK_RADIUS = 5;
	/** Summer Solstice: +3 s of flight, +20 % flight and attack speed in it (D 51F3; BZ 2026-03-10 30 -> 20 %) H. */
	public static final double SOLSTICE_TIME = 3, SOLSTICE_SPEED = 0.2;

	// ---- Solar Flare (51F3 #453 / #482) ----
	/** The left button while beaming: 100 to every ally in a 40 degree cone and 20 m, 10 s cooldown, 0.4 s cast, no energy H/M. */
	public static final double FLARE_HEAL = 100, FLARE_RANGE = 20, FLARE_CONE = 40, FLARE_COOLDOWN = 10, FLARE_CAST = 0.4;

	// ---- Survivor (01C7 #2564, tag 1173) ----
	/** Outburst alone starts the regeneration at once, for 0.25 s at least (D 23DA) H/M. */
	public static final double SURVIVOR_MIN = 0.25;

	/** Perk XP: 2355 minor, 5890 major (D 12D7 / 12D8; W) H. */
	public static final double PERK_MINOR_XP = 2355, PERK_MAJOR_XP = 5890;

	/** The rifle's mouth in her look's frame (ours, by eye on her 1P model). */
	public static final double MUZZLE_RIGHT = 0.2, MUZZLE_UP = -0.15, MUZZLE_AHEAD = 0.6;
}
