package dev.overcraft.ashe;

/**
 * Ashe's numbers. Sources: the game data (D: build 154088 of 2026-10-06, tools/reference/ashe-map.md and its node-by-node readings
 * E:\Overwatch Archive\research\ashe\work_kit\kit_ashe_hero.md / kit_ashe_bob_dynamite.md: the value id or graph node), Blizzard's
 * patch notes (BZ) and the Overwatch wiki (W rev 408621): tools/reference/ashe.md has every date. Her last live change is BZ
 * 2026-10-06 (hit volumes, the global hitscan size); her last number change BZ 2026-06-16 (perks), both before the data build: the
 * data's numbers are the live ones (BZ 2026-10-20 / 11-03 don't touch her). No Workshop recording of her: her movement is the
 * default mover's (D). Confidence: H read in the data and the sources agree, M one link inferred, L a guess; ours where no source
 * gives one.
 */
public final class AsheStats {
	private AsheStats() {
	}

	/** Damage, Sharpshooter: 250 health, no armor or shields (D 0538 250; W 250) H. */
	public static final int HEALTH = 250;

	/** B.O.B.'s cost (D 0ADE 2450, M: no node reads it, the only unshared Ultimate Charge Max; W 2450) H/M. */
	public static final double ULT_COST = 2450;

	/** Perk XP: 1750 to the minor, 2630 more to the major (D 1296 / 1297; W 1750 / 4380) H. */
	public static final double PERK_MINOR_XP = 1750, PERK_MAJOR_XP = 1750 + 2630;

	// ---- The Viper (14FC, the left button; aimed down the sights with the right held) ----
	/** From the hip: 35 a shot (D 053A, 14FC #770; W) H. */
	public static final double HIP_DAMAGE = 35;
	/** 0.25 s from a hip shot to the next (D 14FC #672; W 0.256 s recovery) H. */
	public static final double HIP_INTERVAL = 0.25;
	/** Hip falloff 20 -> 40 m, down to x0.3 (D 1151 / 1150, floor 0.3; W 35 - 10.5 over 20 - 40 m) H; linear between (M). */
	public static final double HIP_FALLOFF_START = 20, HIP_FALLOFF_END = 40;
	/** Hip spread 0 -> 2 degrees (D 1C9A / 1C99; W) H; +1/6 of it a shot (D m_AC20B1C1 6, L), back at 2 a second (D #677) M. */
	public static final double HIP_SPREAD = 2, HIP_SPREAD_PER_SHOT = 1 / 6.0, HIP_SPREAD_RECOVERY = 2;
	/** Scoped: 75 a shot (D 0541, 14FC #555; W 75 - 22.5) H; no spread (D #668) H. */
	public static final double ADS_DAMAGE = 75;
	/** 0.65 s from a scoped shot to the next, paused while scoping in or out (D 14FC #773 v30009, #729; W 0.656 s) H/M. */
	public static final double ADS_INTERVAL = 0.65;
	/** Scoped falloff 35 -> 55 m, down to x0.3 (D 1152 / 1153; BZ 2026-01-08 40-60 -> 35-55; W) H. */
	public static final double ADS_FALLOFF_START = 35, ADS_FALLOFF_END = 55;
	/** Into the scope in 0.2 s, out in 0.1666 s, no fire during either (D 14FC #568 / #569, #575) H/M. */
	public static final double ADS_IN = 0.2, ADS_OUT = 0.1666;
	/** The scope's view: 40 degrees across (D #552 v172 40, M as a field of view), full at half the scope-in (D clamp(v169 x 2)) M. */
	public static final double ADS_FOV = 40;
	/**
	 * Scoped, she moves at x0.75 (W "-25 %, 4.125 m/s") M. The data's #565 has 0.6 in two fields of a movement modifier while fully
	 * scoped (m_69A20070 / m_10705336, meaning unread: L): the wiki's kept, the data's open.
	 */
	public static final double ADS_MOVE = 0.75;
	/** Both: hitscan 0.04 m across (D 28A2, the global large size after BZ 2026-10-06; W) H; x2 on the head (D default, M; W ✓) M. */
	public static final double RADIUS = 0.04, CRIT = 2, FALLOFF_MIN = 0.3;
	/** The longest shot (ours: 200 m, as far as Minecraft draws). */
	public static final double RANGE = 200;
	/** Twelve rounds (D 053B; W) H. */
	public static final int AMMO = 12;
	/**
	 * The reload, a shell at a time: 0.5 s to start with the first shell at its end, then one each 0.25 s; a shot (either button) with
	 * a round in cuts it into its 0.3 s end; 3.25 s from empty to full (D 113B / 113C, 14FC #681 / #688 / #693 / #695 / #704; W 0.5 +
	 * 0.25 a round) H. It starts once the hip shot's 0.25 s is within 0.05 s of its end (D #544, v30010 - v30011) H.
	 */
	public static final double RELOAD_START = 0.5, RELOAD_SHELL = 0.25, RELOAD_END = 0.3;

	// ---- Coach Gun (1513, Shift) ----
	/** 0.55 s cast, the blast 0.165 s in (D 1513 #107 v670, #109 x0.3; W) H. */
	public static final double COACH_CAST = 0.55, COACH_FIRE_AT = 0.165;
	/** 15 pellets of 6 (90), no crit, no falloff field (D 0545 / 1A44, m_6256F1D7 1; W 6 per shot, 90 a volley) H. */
	public static final double COACH_PELLET = 6;
	public static final int COACH_PELLETS = 15;
	/**
	 * Spread 20 degrees (D 1307), the pellets in rings: 1 in the middle, 3 at 0.3, 4 at 0.6, 7 at 1.0 of it (D volley #169) H; read as
	 * the cone's whole width, 10 degrees off the aim at most (M; found playing, A2a: as the half-width a husk 4 m off took 4 of 15).
	 */
	public static final double COACH_SPREAD = 20;
	/** Each pellet 0.03 m (W; the data's 28DE 0.4 + 0.04 reads as the volley's whole, W "0.45 total") M; 15 m reach (W) M. */
	public static final double COACH_PELLET_RADIUS = 0.03, COACH_RANGE = 15;
	/**
	 * The push on an enemy a pellet hits: 20 m/s along her aim (+0.5 up) and 5 up, the whole of it to 8 m, none from 15 m (D 1513
	 * #169: m_E33EFEE1 20 x1, m_583C208B 5, rings 8 x1 / 15 x0; W 20 m/s, 15 m) H; linear between (M).
	 */
	public static final double COACH_PUSH = 20, COACH_PUSH_UP = 5, COACH_PUSH_FULL = 8, COACH_PUSH_NONE = 15;
	/** Her own: 18 m/s back along her aim with 0.1 up (D 0EAF, #141; W 18 m/s, 9.4 m) H/M. */
	public static final double COACH_SELF = 18, COACH_SELF_UP = 0.1;
	/** 10 s from the cast (D 131F, 1513 #124; W) H. */
	public static final double COACH_COOLDOWN = 10;

	// ---- Dynamite (14FD, E; the stick 2533 / 1502, the burn 150F) ----
	/** Thrown 0.15 s in (anim 0.66 s), cooldown from 0.55 s in (D 14FD #41 / #100 / #42 0.4) H. */
	public static final double DYNAMITE_THROW_AT = 0.15, DYNAMITE_CAST = 0.66, DYNAMITE_COOLDOWN_AT = 0.55;
	/** 25 m/s, falling at 8 m/s² (M), 0.1 m (D 28C9 / v7389 / 28C8; W 25 m/s, 0.1 m) H/M. */
	public static final double DYNAMITE_SPEED = 25, DYNAMITE_GRAVITY = 8, DYNAMITE_RADIUS = 0.1;
	/** 12 s (D 1320; W) H. */
	public static final double DYNAMITE_COOLDOWN = 12;
	/** It goes off 2 s after it lands (D 1D41, 1502 #180, M "from landing"; W "2 seconds explosion delay on ground") H/M; 30 s at most (D). */
	public static final double DYNAMITE_FUSE = 2, DYNAMITE_LIFETIME = 30;
	/** Off a wall or a ceiling it bounces at this share (ours: the data's bounce block unread, L). */
	public static final double DYNAMITE_BOUNCE = 0.3;
	/** Its box to her shots: a 0.3 m cube (ours: the 0.1 m size reads as its collision; Minecraft's aim needs more). */
	public static final double DYNAMITE_SHOOT_BOX = 0.3;
	/** 50 to 1.5 m, down to 20 at 5 m (D 0546 / 0547 / 1D40 / 131E, 1502 #133; W 50 - 20 over 1.5 - 5 m) H; linear (M). */
	public static final double BLAST_DAMAGE = 50, BLAST_MIN = 20, BLAST_FULL = 1.5, BLAST_RADIUS = 5;
	/** On herself half (D 1502 #178; W 25 - 10) H, no push (D) H. */
	public static final double BLAST_SELF = 0.5;
	/** The burn: 100 over 5 s on each hit (her own half) (D 0548 / 1A43, 150F #143 / #239; W 3.84 every 0.192 s) H; ours in 0.2 s steps. */
	public static final double BURN_TOTAL = 100, BURN_TIME = 5, BURN_STEP = 0.2;

	// ---- B.O.B. (1536 / his 1537, 153B, 35B3; Q) ----
	/** 0.5 s cast (D 1536 #107 / #41; W 0.496 s recovery) H. */
	public static final double BOB_CAST = 0.5;
	/**
	 * His health: 1000 (D 054C; W) and the Tank role's 150 (W: "1150 with the Tank role passive", 5v5 role queue, the mode the mod
	 * plays) H/M. No armor (D: none in any graph; W none) H.
	 */
	public static final double BOB_HEALTH = 1000 + 150;
	/** Down 0.5 s after the spawn (D 1537 #351) H; then 4 s of charge at 15 m/s (D 1322 / #363 H, mover #409 m_031FE7EE 15 M; W) H/M. */
	public static final double BOB_DROP = 0.5, BOB_CHARGE_TIME = 4, BOB_CHARGE_SPEED = 15;
	/** A wall stops it: rays 2 m ahead at 0 and 0.75 m up (D #640 / #641) H. */
	public static final double BOB_WALL_RAY = 2, BOB_WALL_RAY_UP = 0.75;
	/** The charge ends 0.8 s after his uppercut lands (D #362 0.2 + #392 0.6) H. */
	public static final double BOB_AFTER_UPPERCUT = 0.8;
	/** The uppercut: an enemy within 2 m and 90 degrees ahead starts it; it takes all within 4 m and 90 degrees (D #252 / #404) H. */
	public static final double BOB_UPPERCUT_TRIGGER = 2, BOB_UPPERCUT_REACH = 4, BOB_UPPERCUT_CONE = 90;
	/** 120 (D 054E, #276; W) H. */
	public static final double BOB_UPPERCUT_DAMAGE = 120;
	/**
	 * Knocked up 15 m/s from the ground, 12 in the air, 0.5 along his way (D #276: m_583C208B expr(flying, 12, 15), m_E33EFEE1 0.5;
	 * L which is which) H/L.
	 */
	public static final double BOB_KNOCK_UP = 15, BOB_KNOCK_UP_AIR = 12, BOB_KNOCK_ALONG = 0.5;
	/** Held 1.5 s, 0.5 s if it was flying (D 2405 / 2406, 17F6 #348; W) H. */
	public static final double BOB_IMMOBILIZE = 1.5, BOB_IMMOBILIZE_AIR = 0.5;
	/** Then 10 s of fire (D 1321; W) H, then 0.5 s to go (D clip 0578, #375) H. */
	public static final double BOB_FIRE_TIME = 10, BOB_DESPAWN = 0.5;
	/** His cannons: 17 a shot, 8 a second, 1.45 degrees of spread, 40 m (D 054D, 153B; W 17, 8/s, 1.45 deg, 40 m) H. */
	public static final double BOB_SHOT = 17, BOB_RATE = 8, BOB_SPREAD = 1.45, BOB_RANGE = 40;
	/** He prefers an enemy she hit in the last few seconds (W "prefers to target enemies that Ashe damages"; ours 3 s) M. */
	public static final double BOB_PREFER_TIME = 3;
	/** His cannons' height (ours: 1.6 m, his arms on his 2.3 m body). */
	public static final double BOB_GUN_UP = 1.6;
	/** His body: 2.3 m tall, 0.7 m round (D mover) H; drawn as Minecraft's box 1.4 x 2.3. */
	public static final double BOB_HEIGHT = 2.3, BOB_RADIUS = 0.7;

	// ---- Sharpshooter (01C7 #2832, tag 1188) ----
	/** Crit damage dealt takes 0.0075 s a point off Coach Gun's cooldown (D 23D7 0.0075, 01C7 #2832 -> 1513 #451; BZ 2026-02-10) H. */
	public static final double SHARPSHOOTER = 0.0075;

	// ---- Perks (51E3; her live list: 0CCF Remote Detonator, 0E4F Double-Barreled; 0B3E Viper's Sting, 0B40 Airburst) ----
	/** Remote Detonator (minor): E again with it out sets it off 0.5 s later (D 51E3 #368, 1502 0.5; W) H. */
	public static final double REMOTE_DELAY = 0.5;
	/** Double-Barreled (minor): a Coach Gun that knocks an enemy back may fire once more within 2 s (D 28B4 v65709 2; BZ 2026-04-14) H. */
	public static final double DOUBLE_WINDOW = 2;
	/** Viper's Sting (major): two scoped hits in a row on one target within 3 s: 25 more over 1 s (D 51E3 #344 / #330; W) H. */
	public static final double STING_DAMAGE = 25, STING_TIME = 1, STING_WINDOW = 3;
	/** Airburst (major): the blast x1.4 while it hasn't landed, and 6 rounds back on the throw (D 51E3 #329 / #359; BZ 2026-06-16 3 -> 6) H. */
	public static final double AIRBURST_RADIUS = 1.4;
	public static final int AIRBURST_AMMO = 6;

	/** Where her shots' look leaves from (right, up, ahead from her eyes, m): the rifle at the hip; ours. */
	public static final double MUZZLE_RIGHT = 0.2, MUZZLE_UP = -0.15, MUZZLE_AHEAD = 0.7;
	/** Where the stick leaves her hand (right, up, ahead): her left hand; ours. */
	public static final double THROW_RIGHT = -0.25, THROW_UP = -0.1, THROW_AHEAD = 0.5;
}
