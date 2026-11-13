package dev.overcraft.junkrat;

/**
 * Junkrat's numbers. Sources: the game data (D: build 154088 of 2026-10-06, tools/reference/junkrat-map.md and its node-by-node
 * reading E:\Overwatch Archive\research\junkrat\work_kit\kit_junkrat.md: the value id or graph node), Blizzard's patch notes (BZ)
 * and the Overwatch wiki (W rev 410886): tools/reference/junkrat.md has every date. His last live number change is BZ 2026-09-08
 * (the mine's 7 s, Nitro Boost's 100 %), before the data build and in it: the data's numbers are the live ones. No Workshop
 * recording of him exists yet: his movement is the default mover's (D), not measured. Confidence: H read in the data or the
 * sources agree, M one link inferred, L a guess; where no source gives a value it's ours.
 */
public final class JunkratStats {
	private JunkratStats() {
	}

	/** Damage, Specialist: 250 health, no armor or shields (D 0178 250; BZ 2024-02-13; W) H. */
	public static final int HEALTH = 250;

	/** RIP-Tire's cost (D 0AEA 2100, M that it's the cost; W 2100) H/M. */
	public static final double ULT_COST = 2100;

	/** Perk XP: 1675 to the minor, 2510 more to the major (D 336B #142: 129E / 129F) H. */
	public static final double PERK_MINOR_XP = 1675, PERK_MAJOR_XP = 1675 + 2510;

	// ---- Frag Launcher (022B, the left button) ----
	/** 45 on a direct hit (D 0343) plus the splash, no crit (D m_A872C70E 1; W) H/M. */
	public static final double FRAG_DIRECT = 45;
	/** The splash: 80 inside 0.5 m (2 m x 1D3F 0.25), down to 10 (x 1D2F 0.125) at 2 m (D 0344 / 0347; W) H; linear between (M). */
	public static final double FRAG_SPLASH = 80, FRAG_SPLASH_FULL = 0.5, FRAG_SPLASH_RADIUS = 2, FRAG_SPLASH_EDGE = 0.125;
	/** 30 m/s (D 0345; BZ 2025-12-09 25 -> 30) H, falling at 17 m/s² (D 022B #162 v7389, M that it's gravity). */
	public static final double FRAG_SPEED = 30, FRAG_GRAVITY = 17;
	/** Frag Cannon: x1.25 its speed, 37.5 m/s (D 022B #446; BZ 2025-12-09; W) H. */
	public static final double FRAG_CANNON = 1.25;
	/** Its hit size on a creature: 0.1 m (D 28EE), halved after its second bounce (D v59304 [1, 0.5] M; W 0.32 -> 0.16 m) H/M. */
	public static final double FRAG_HIT_RADIUS = 0.1;
	/** Off a block it keeps half its speed into the face (D m_0D20DAFD 0.5, M) and 0.75 along it (D m_C29D35E8 0.75, L). */
	public static final double FRAG_RESTITUTION = 0.5, FRAG_FRICTION = 0.75;
	/**
	 * When it goes off by itself (W, M; the data's bounce block isn't named: junkrat-map.md Open): on a ground bounce after an
	 * earlier bounce (two ground bounces, or a wall then the ground), or 4.5 s after the shot (W; the data's lifetime is 7 s, L that
	 * the fuse is shorter). Walls alone don't set it off.
	 */
	public static final double FRAG_FUSE = 4.5;
	/** A face counts as ground when it points this far up (ours: Minecraft's faces are only up, down or sideways). */
	public static final double GROUND_NORMAL = 0.7;
	/** Its push: 5 away and 8 up (D 1D2C / 1D2D) H, x1 inside 0.5 m, x0.75 at 2 m (D rings) H, half on him (W 2.5 / 4: L). */
	public static final double FRAG_KNOCK_AWAY = 5, FRAG_KNOCK_UP = 8, FRAG_KNOCK_EDGE = 0.75, FRAG_KNOCK_SELF = 0.5;
	/** Five frags (D 0346; W) H. */
	public static final int AMMO = 5;
	/** 0.667 s from a shot to the next (D 022B #102 v173), shortened by attack speed (Bomb Voyage) H. */
	public static final double FRAG_INTERVAL = 0.667;
	/** The reload: 1.5 s (D 0348), the frags back at 1.5 x 0.578 = 0.867 s (D #87; W 0.848) H. */
	public static final double RELOAD = 1.5, RELOAD_AMMO_AT = 1.5 * 0.578;
	/** Where the frag leaves from, in his look's frame (right, up, ahead; ours: the data's muzzle is hp 0006 on the launcher). */
	public static final double MUZZLE_RIGHT = 0.25, MUZZLE_UP = -0.15, MUZZLE_AHEAD = 0.5;

	// ---- Concussion Mine (096A / 096B, Shift; the right button sets it off) ----
	/** Two charges, 7 s each (D 034C, v181 2; BZ 2026-09-08 8 -> 7) H. */
	public static final int MINE_CHARGES = 2;
	public static final double MINE_RECHARGE = 7;
	/** The throw leaves 0.144 s into its 0.5 s animation (D #234 / 0392; W 0.128) H. */
	public static final double MINE_CAST = 0.144, MINE_ANIM = 0.5;
	/** 25 m/s, falling at 20 m/s², 0.2 m (D #227: 034D, v7389, 28EF) H; it sticks where it lands (D restitution 0.05, M). */
	public static final double MINE_SPEED = 25, MINE_GRAVITY = 20, MINE_RADIUS = 0.2;
	/** The right button sets it off from 0.1 s after the throw (D #230) H. */
	public static final double MINE_ARM = 0.1;
	/** Its blast: 120 inside 1 m, down to 55 at 3 m, enemies only (D 034F / 034B / 034A / 0350) H; linear between (M). */
	public static final double MINE_DAMAGE = 120, MINE_FULL = 1, MINE_EDGE_DAMAGE = 55, MINE_RADIUS_BLAST = 3;
	/** It goes off 0.1 s after the press (D 096B #400) H. */
	public static final double MINE_DELAY = 0.1;
	/** Its push on enemies: 15 away and 15 up (D 1D51 / 1D50; W 21.2 m/s) H. */
	public static final double MINE_KNOCK_AWAY = 15, MINE_KNOCK_UP = 15;
	/** His own launch, within 3.5 m of it: 15 away and 15 up, no damage (D #264 / #262: 187A, 1D47) H. */
	public static final double MINE_SELF_RANGE = 3.5, MINE_SELF_AWAY = 15, MINE_SELF_UP = 15;
	/** 25 health: enemies can break it, and then it doesn't go off (D 19A8; W) H/M. */
	public static final double MINE_HEALTH = 25;
	/** Bomb Voyage: +0.35 attack speed for 3 s after the mine's launch (D 51E8 #485; BZ 2026-06-16) H. */
	public static final double VOYAGE_SPEED = 0.35, VOYAGE_TIME = 3;

	// ---- Steel Trap (09BA / 09BB, E) ----
	/** 10 s, from 0.5 s after the throw (D 0354, #446 / #448) H. */
	public static final double TRAP_COOLDOWN = 10, TRAP_COOLDOWN_FROM = 0.5;
	/** The throw: 0.35 s animation (D 0432), 15 m/s (D 0356; W 17) falling at 20 m/s², 0.3 m (D 28F0), 5 s in flight at most (D) H. */
	public static final double TRAP_ANIM = 0.35, TRAP_SPEED = 15, TRAP_GRAVITY = 20, TRAP_RADIUS = 0.3, TRAP_FLIGHT = 5;
	/** Armed 0.75 s after it lands, sprung by an enemy within 1 m (D 09BB #448, 1D61; W) H. */
	public static final double TRAP_ARM = 0.75, TRAP_TRIGGER = 1;
	/** 100 to what it catches and a 2.5 s root (D 0352 / 0353; W) H. */
	public static final double TRAP_DAMAGE = 100, TRAP_ROOT = 2.5;
	/** 100 health (D 0355; BZ 2026-06-30's Community Crafted "from 100") H. */
	public static final double TRAP_HEALTH = 100;
	/** A sprung trap stays this long after it lets go (D #573 / #576, M), then goes. */
	public static final double TRAP_LINGER = 8;

	// ---- RIP-Tire (0232 / 0233 / 0234, Q) ----
	/**
	 * The tire comes at the cast, 0.05 m ahead of him (D 0232 #317 under the cast's StateAbility, M that it's at once), and is his to
	 * drive from then; the cast's clip runs 2.0 s (D 0131, v670 1.5 + 0.5) H. W's "1.58 s startup" is the arm below (M).
	 */
	public static final double TIRE_AHEAD = 0.05, TIRE_CAST_ANIM = 2.0;
	/** It can be set off 1.4 + 0.2 s after it comes (D 0233 #467 / #387) H. */
	public static final double TIRE_ARM = 1.6;
	/** 10 s at most (D 035B), 100 health (D 03D9) H. */
	public static final double TIRE_TIME = 10, TIRE_HEALTH = 100;
	/** 12 m/s forward, 6 back, 3 to the side (D 039C's mover, M which field is which; W 12 m/s) H/M. */
	public static final double TIRE_FORWARD = 12, TIRE_BACK = 6, TIRE_STRAFE = 3;
	/** Its jump, 9 m/s up (D 039C m_EC289987 9 against the hero's 6: L); its climb up a wall it's driven into with Jump held (ours). */
	public static final double TIRE_JUMP = 9, TIRE_CLIMB = 6;
	/** 0.9 m across, 0.8 m tall (D 039C radius 0.45, height 0.8, M) H/M. */
	public static final double TIRE_WIDTH = 0.9, TIRE_HEIGHT = 0.8;
	/** Its blast: 600 inside 2 m, down to 50 at 10 m (D 0349 / 035A / 0357 / 0358 / 0359) H; linear between (M). */
	public static final double TIRE_DAMAGE = 600, TIRE_FULL = 2, TIRE_EDGE_DAMAGE = 50, TIRE_RADIUS = 10;
	/** Its push: 6 away and 6 up, x1 inside 2 m, x0.75 at 10 m (D 1D6E / 1D6F, rings) H. */
	public static final double TIRE_KNOCK_AWAY = 6, TIRE_KNOCK_UP = 6, TIRE_KNOCK_EDGE = 0.75;
	/** Nitro Boost: once, from 1.6 s after the tire comes; +100 % falling to +25 % over 2 s, 2.5 s, its blast x0.5 (D 51E8 #425-#438, 0233 #682) H/M. */
	public static final double NITRO_FROM = 1.6, NITRO_START = 1.0, NITRO_END = 0.25, NITRO_FADE = 2, NITRO_TIME = 2.5, NITRO_DAMAGE = 0.5;

	// ---- Total Mayhem (022A) ----
	/** Six bombs of 50 within 2 m, no falloff, dropped at 1.5 m/s within 90 degrees of straight down, gravity 9.8 (D 187C / 1996 / 28F2) H. */
	public static final int MAYHEM_BOMBS = 6;
	public static final double MAYHEM_DAMAGE = 50, MAYHEM_RADIUS = 2, MAYHEM_SPEED = 1.5, MAYHEM_SPREAD = 90, MAYHEM_GRAVITY = 9.8;
	/** Each goes off 0.8, 0.9 ... 1.3 s after his death (D v561 0.7 + 0.1 ... 0.6) H. */
	public static final double MAYHEM_FIRST = 0.8, MAYHEM_STEP = 0.1;

	// ---- Specialist (01C7, his sub-role) ----
	/** An elimination: +0.5 reload speed for 3 s, used up by one reload (D 23D5 / 23D6 H; M that one reload uses it). */
	public static final double SPECIALIST_SPEED = 0.5, SPECIALIST_TIME = 3;
}
