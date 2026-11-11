package dev.overcraft.dva;

/**
 * D.Va's numbers (tools/reference/dva.md, each with its source: D = her game data, build 154088; BZ = Blizzard's patch notes;
 * W = the Overwatch wiki; H / M / L = confidence), built step by step (dva-plan.md "Order"). Her last live change (BZ
 * 2026-10-06, missiles 11 -> 14 a second) is in the data build, so D's numbers are the live ones. Two bodies: the MEKA (the
 * data's pet entity 0407, values graph 33D9) and the pilot (the hero entity 040A, values graph 348C): dva-map.md.
 */
public final class DvaStats {
	private DvaStats() {
	}

	// ---- the MEKA (dva.md "MEKA: base") ----
	/** The mech's health without the role's bonus (D 33D9 #119: 0377 200; BZ 2026-09-08 175 -> 200; W 200) H. Tank adds 150. */
	public static final int MECH_HEALTH = 200;
	/** The mech's armor (D 33D9 #119: 0376 325; BZ 2024-09-17 375 -> 325; W 325) H: 200 + 150 + 325 = 675 in all. */
	public static final int MECH_ARMOR = 325;
	/** Self-Destruct's cost, the mech's ult (D 0AE4 1700, alt 3080; W 1700) H. */
	public static final double MECH_ULT_COST = 1700;

	// ---- the pilot (dva.md "Pilot") ----
	/** The pilot's health, no role bonus (D 348C #119: 0268 175; BZ 2024-02-13 150 -> 175, "mech only" for the +150; W 175) H / M. */
	public static final int PILOT_HEALTH = 175;
	/** Call Mech's cost, the pilot's ult (D 0AE3 280, alt 420; W 280) H. */
	public static final double PILOT_ULT_COST = 280;

	/** Perk XP: the minor at 1615, the major 2325 more (3940) (D 12C3 / 12C4 on 33D9 #194 / 348C #165; W 1615 / 3940) H. */
	public static final double PERK_MINOR_XP = 1615, PERK_MAJOR_XP = 3940;

	// ---- her body boxes (dva-plan.md "Minecraft decisions": the owner's rule for tanks, 2026-11-02) ----
	/** The MEKA's mover: 2.3 m tall, radius 0.7 m = 1.4 m wide (D 0407's mover; the model 2.43 m tall, 2.62 m wide) H. */
	public static final float MECH_WIDTH = 1.4f, MECH_HEIGHT = 2.3f;
	/** Her first-person eye in the mech, 1.74 m standing and crouched (D 041F m_E29993A5 / m_816B830F) H field, M meaning. */
	public static final float MECH_EYE = 1.74f;

	// ---- the mech's death and the eject (dva-map.md "Mech base, death and eject", 0276 / 026A) ----
	/** The wreck: from the mech's 0 to the eject, 1.0 s, Self-Destruct still castable (D 0276 #84 v69941 = 1B57 1.0) H. */
	public static final double WRECK_TIME = 1.0;
	/** The wreck stays 0.75 s after the eject, then goes (D 0276 #60 v69943 = 1B58 0.75) H. */
	public static final double WRECK_LINGER = 0.75;
	/** The eject's launch: 6 m/s back, 7 m/s up, from a point 1 m ahead of the mech (D 026A #97's knockback 6 / 7) H. */
	public static final double EJECT_BACK = 6, EJECT_UP = 7;
	/** After the eject she can't be hurt for 0.4 s (D 026A #880 0.4 s of x45AB6711, M meaning; BZ 2023-08-10 0.4; W 1.8) H / M. */
	public static final double EJECT_IMMUNE = 0.4;
	/** Call Mech locked for 1 s after the eject (D 026A #640 v7145) H. */
	public static final double EJECT_CALL_LOCK = 1.0;

	// ---- Call Mech (dva-map.md "Call Mech", 026A) ----
	/** Its cast: 0.2 s, no moving, no jumping (D 026A #91 0.2 s, #105 the inputs off) H. */
	public static final double CALL_CAST = 0.2;
	/** The mech falls in and lands 0.7 s after it spawns: 0.9 s after the press (D 026A #869 / #639 v12209 0.7; W 0.896) H. */
	public static final double CALL_LAND = 0.7;
	/** From 0.8 s after the spawn she can't be hurt (D 026A #350 -> #351 x45AB6711) H node, M meaning. */
	public static final double CALL_UNTARGETABLE = 0.8;
	/** She drives the mech 1.75 s after the spawn: 1.95 s after the press (D 026A #431 -> #98) H. */
	public static final double CALL_LINK = 1.75;
	/** Placed 1.5 m ahead along her flat facing, probed from 1.5 m up, 4 m down (D 026A #256) H fields, M meanings. */
	public static final double CALL_AHEAD = 1.5, CALL_PROBE_UP = 1.5, CALL_PROBE_DOWN = 4;
	/** No ground ahead: on her own spot, if there's ground within 2 m under her (D 026A #455: a ray from 1 m up, 3 m long) H. */
	public static final double CALL_GROUND_BELOW = 2;
	/** Where the mech drops in from (ours: the data spawns it at the spot and its landing clip 0xE5 brings it down, M). */
	public static final double CALL_DROP_HEIGHT = 4;
	/** The landing: 250 to enemies within 3 m (D 348C 03BE 250 / 03BF 3, 026A #242; W 250 / 3 m) H. */
	public static final double CALL_DAMAGE = 250, CALL_RADIUS = 3;
	/** Its knockback, 15 m/s along and 3 up (D 348C 1B5D 15 / 1B5E 3, 026A #949 / #950) H. */
	public static final double CALL_KNOCKBACK = 15, CALL_KNOCKBACK_UP = 3;

	// ---- Fusion Cannons (dva.md "Fusion Cannons", 050A) ----
	/** Pellets a volley: 1 in the middle, 4 at half the cone, 6 at its edge (D 1185 11 at 050A #444, its pattern m_5EC1EB38; W 11) H. */
	public static final int FUSION_PELLETS = 11, FUSION_INNER = 4, FUSION_OUTER = 6;
	/** Damage a pellet (D 0398 2 at #421; W 2) H. */
	public static final double FUSION_DAMAGE = 2;
	/** Falloff from 10 m to 20 m (D 039C 10 / 039B 20 at #447 / #448; W) H. */
	public static final double FUSION_FALLOFF_START = 10, FUSION_FALLOFF_END = 20;
	/** The floor past 20 m: x0.3 (W 0.6 a pellet; D's floor var v37674 has no writer read, 0303 0.3 likely: L). */
	public static final double FUSION_FALLOFF_MIN = 0.3;
	/** A volley every 0.15 s: 6.67 a second, 146.7 damage a second (D 050A #411 / #269 0.15 s; W 0.15) H. */
	public static final double FUSION_INTERVAL = 0.15;
	/** The cone: 3.375 degrees (D 0399 3.375; BZ 2024-06-20 3.75 -> 3.375; W) H. */
	public static final double FUSION_SPREAD = 3.375;
	/** -30 % move speed while firing, none while boosting (D 050A #376 1DB9 0.3, gated by Boosters' v87; BZ 2023-10-31; W) H. */
	public static final double FUSION_SLOW = 0.3;
	/** A pellet's radius, the global small hitscan (D 01C7 #1198 28A3 0.02; BZ 2026-10-06 0.04 -> 0.02) H. */
	public static final double FUSION_RADIUS = 0.02;
	/** Headshots x2 (D 03A6 1.0 unread, the generic x2: M; W) M. */
	public static final double FUSION_CRIT = 2;
	/** How far a pellet reaches (ours: past 20 m it does the floor; Overwatch's hitscan has no end given, L). */
	public static final double FUSION_RANGE = 60;

	// ---- Defense Matrix (dva.md "Defense Matrix", 02CC) ----
	/** Its energy, 100, drained 33 a second while up: 3.03 s from full (D 1864 100, 1B49 33 at #395; BZ 2024-08-20 3 s; W) H. */
	public static final double MATRIX_ENERGY = 100, MATRIX_DRAIN = 33;
	/** Back at 16 a second, 0.75 s after it's lowered: 6.25 s from empty (D 03B7 16 at #389, 1B50 0.75 at #284; W 16 / 0.75) M. */
	public static final double MATRIX_REGEN = 16, MATRIX_REGEN_DELAY = 0.75;
	/** Up at least 0.5 s once raised (D 02CC #321; W 0.496) M. */
	public static final double MATRIX_MIN = 0.5;
	/** Its cooldown after each use, 1 s (D 1865 1.0 at #326 / #377; W) M. */
	public static final double MATRIX_COOLDOWN = 1;
	/** The box along her aim: 1.25 m either side, 1 m up and down, from 0.8 to 10.25 m ahead (D #205, 1B4E 10.25; W 10.3) H values, M origin. */
	public static final double MATRIX_SIDE = 1.25, MATRIX_HALF_HEIGHT = 1, MATRIX_NEAR = 0.8, MATRIX_FAR = 10.25;
	/** 4 % of what it eats to her ult (D 02CC #255 v6584 x 0.04 to STU_008D30B1, M that it's ult charge) L. */
	public static final double MATRIX_ULT_SHARE = 0.04;

	// ---- Boosters (dva.md "Boosters", 047E) ----
	/** Her flight's speed along her aim: 5.5 x (1 + 1.18) = 12 m/s (D 047E #158 flight mover, 1285 1.18; W 12) M. */
	public static final double BOOSTERS_SPEED = 5.5 * (1 + 1.18);
	/** Its length: 2 s at most, ended by a second press from 0.4 s on (D 03AF 2 at #166, #179 0.4 s; W) M. */
	public static final double BOOSTERS_TIME = 2, BOOSTERS_MIN = 0.4;
	/** Its cooldown starts 0.3 s after the flight ends (D 047E #168 -> #171) M. */
	public static final double BOOSTERS_COOLDOWN_DELAY = 0.3;
	/** Each enemy hit once a flight in a 1.5 m sphere 1 m up and 2 m ahead: 25 (D 03AB 25, #172 / #173; BZ 2024-06-20; W) H. */
	public static final double BOOSTERS_DAMAGE = 25, BOOSTERS_HIT_RADIUS = 1.5, BOOSTERS_HIT_UP = 1, BOOSTERS_HIT_AHEAD = 2;
	/** Its push: 17 m/s along her flight and 4 up (D 03AC 17 / 03AD 4 at #369 / #370; W 17.5) M. */
	public static final double BOOSTERS_KNOCKBACK = 17, BOOSTERS_KNOCKBACK_UP = 4;
	/** Knockback she takes while flying: x0.1 (D 1E09 -0.9 at #243) M. */
	public static final double BOOSTERS_KNOCKBACK_TAKEN = 1 - 0.9;

	// ---- the kit's cooldowns and ammo the HUD shows now (their abilities: steps 4-9) ----
	/** Boosters' cooldown (D 33D9 03A9 4, 047E #221; BZ 2026-03-10 3.5 -> 4; W 4) H. */
	public static final double BOOSTERS_COOLDOWN = 4;
	/** Micro Missiles' cooldown (D 33D9 03B3 7, 1128 #372; W 7) H. */
	public static final double MISSILES_COOLDOWN = 7;
	/** The Light Gun's magazine (D 348C 03A8 20, 02B8 #325; W 20) H. */
	public static final int LIGHT_GUN_AMMO = 20;

	// ---- Micro Missiles (dva.md "Micro Missiles", 1128) ----
	/** The wait before the first missile: 0.25 s (D 1128 #652 / #363 v670; W 0.16) L. */
	public static final double MISSILES_CAST = 0.25;
	/** 18 missiles at 14 a second: 1.29 s a volley (D 1B56 18, 0404 14 at #645; BZ 2026-10-06 11 -> 14; W 18) H. */
	public static final int MISSILES_COUNT = 18;
	public static final double MISSILES_RATE = 14;
	/** 50 m/s, 0.175 m (D 03B5 50 at #651; 28D6 0.1 + 28A6 0.075 at #357; BZ 2023-10-31 40 -> 50) H. */
	public static final double MISSILES_SPEED = 50, MISSILES_RADIUS = 0.1 + 0.075;
	/** How far one flies: 100 m (D #357 m_FEC435C6 100, its lifetime field 0: M that it's a range) = 2 s at 50 m/s. */
	public static final double MISSILES_RANGE = 100;
	/** 2 to the creature it hits, then 5.5 to all within 0.1 m, down to x0.25 at 1.5 m (D 03B0 2, 03B1 5.5, 03B6 0.1, 03B4 1.5, 1B51 0.25; BZ 2026-05-12 3 -> 2) H. */
	public static final double MISSILES_IMPACT = 2, MISSILES_SPLASH = 5.5, MISSILES_SPLASH_FULL = 0.1, MISSILES_SPLASH_RADIUS = 1.5,
		MISSILES_SPLASH_EDGE = 0.25;
	/**
	 * The muzzles they alternate from: hp 0075 / 0077 on the MEKA (D 1128 #357), read off her first-person model 10354 (rest
	 * pose, from her feet): 0.88 m either side, 1.976 m up, 0.32 m ahead (H positions; from her eyes in her look's frame: M).
	 */
	public static final double MISSILES_MUZZLE_SIDE = 0.88, MISSILES_MUZZLE_UP = 1.976 - MECH_EYE, MISSILES_MUZZLE_AHEAD = 0.32;

	// ---- Self-Destruct (dva.md "Self-Destruct", 0B2F) ----
	/** Its fuse: 3 s from the cast (D 0C6A 3 at #410, misnamed "Outer Radius"; W 3) H. */
	public static final double SELF_DESTRUCT_FUSE = 3;
	/**
	 * 1000 within 6 m, then down to 100 at 21 m, none past (BZ 2024-02-13: max 900 -> 1000, inner 4 -> 6 m; BZ names no minimum;
	 * W rev 408749 "1000 - 100", followed as the newest public source; D #280 reads 03B9 = 50, the disagreement noted) H / M.
	 */
	public static final double SELF_DESTRUCT_DAMAGE = 1000, SELF_DESTRUCT_INNER = 6, SELF_DESTRUCT_MIN = 100, SELF_DESTRUCT_RADIUS = 21;
	/** Its centre, 1.3 m above the mech's feet (D #259) M. */
	public static final double SELF_DESTRUCT_UP = 1.3;
	/** Its knockback at the centre: 20 m/s out, 10 up = 22.4 (D 1B59 20 / 1B5A 10 at #458 / #459) H values, M axes. */
	public static final double SELF_DESTRUCT_KNOCKBACK = 20, SELF_DESTRUCT_KNOCKBACK_UP = 10;
	/** At 21 m: 7.8 m/s of the 22.4 (W's minimum; D's ring factors 0.75 / 0.75 / 0.25 unread as to axis: ours linear, Open 4) L. */
	public static final double SELF_DESTRUCT_KNOCKBACK_EDGE = 7.8 / Math.hypot(20, 10);
	/** The mech is gone 0.3 s after the blast (D 0B2F, after #274) M. */
	public static final double SELF_DESTRUCT_LINGER = 0.3;

	// ---- the pilot's Light Gun (dva.md "Pilot", 02B8) ----
	/** 14 a shot, 7 a second, x2 on the head, no falloff, no spread (D 03A2 14 / 03A4 7 at #326 / #327, 03A7 unread: the generic x2) H / M. */
	public static final double LIGHT_GUN_DAMAGE = 14, LIGHT_GUN_RATE = 7, LIGHT_GUN_CRIT = 2;
	/** 50 m/s, a 0.275 m hit, 5 s (D 03A5 50, 28D5 0.2 + 28A6 0.075, lifetime 5 at #94; BZ 2024-03-12 size) H. */
	public static final double LIGHT_GUN_SPEED = 50, LIGHT_GUN_RADIUS = 0.2 + 0.075, LIGHT_GUN_LIFE = 5;
	/** Reload 1.4 s, the magazine full at 0.65 of it (0.91 s), firing allowed from then (D 03A3 1.4 at #323, #354 0.65, #218 v414: M) H / M. */
	public static final double LIGHT_GUN_RELOAD = 1.4, LIGHT_GUN_RELOAD_AMMO = 0.65;

	// ---- the Initiator sub-role (dva-map.md "Role: Tank and Initiator", 01C7) ----
	/** Boosters' start heals 50 over 1 s, not at full health (D 01C7 #2539 / #2857: 23D0 50, 23D1 1; BZ 2026-05-12 60 -> 50) H. */
	public static final double INITIATOR_HEAL = 50, INITIATOR_HEAL_TIME = 1;
	/** Then 5 s before it can heal again (D 01C7 #2889: v74592 chased from 257B 5) H. */
	public static final double INITIATOR_LOCKOUT = 5;

	// ---- perks (tools/reference/dva.md "Perks": live list 0042.177; BZ to 2026-11-05, W revision 408749: unchanged) ----
	/** Bunny Power: 75 overhealth when she ejects, held 10 s, then 25 a second (D 514D #424-#427; W 75 / 10 s / 25/s) H. */
	public static final double BUNNY_OVERHEALTH = 75, BUNNY_HOLD = 10, BUNNY_DECAY = 25;
	/** And Call Mech's radius x1.5 = 4.5 m, its 250 falling off from 3 m to 0 at 4.5 m (D 026A #941 v38703 0.5, #908; W 4.5 m) H. */
	public static final double BUNNY_CALL_RADIUS = 4.5;
	/** Extended Boosters: a hit +0.5 s, at most 2 s left (D 047E #458-#463; W +0.5 s; the cap: D only) H. */
	public static final double EXTENDED_BOOSTERS_TIME = 0.5, EXTENDED_BOOSTERS_CAP = 2;
	/** And Boosters' hit x1.4 = 35 (D 514D #310 v73021 += 0.4 -> 33D9 #139; BZ 2026-01-08 +40 %; W 35) H. */
	public static final double EXTENDED_BOOSTERS_DAMAGE = 35;
	/** Shield System: 100 of the mech's health becomes shields (D 514D #399 / #400 / #406 v221 100; BZ 2026-06-16; W 100) H. */
	public static final int SHIELD_SYSTEM_SHIELDS = 100;
	/** And Defense Matrix gives 25 % of what it eats back as shields (D 514D #402 v176 0.25, message 2638; W 25 %) H. */
	public static final double SHIELD_SYSTEM_SHARE = 0.25;
	/** Precision Fusion (Reload, the mech): 3.1 s (3 + its 0.1 s ramp) (D 514D #616 v567; W 3 s) H. */
	public static final double PRECISION_TIME = 3.1;
	/** Spread -75 %, ramped in over 0.1 s and out over 0.15 s (D #636 / #639 / #643 v37527) H. */
	public static final double PRECISION_SPREAD_CUT = 0.75, PRECISION_RAMP_IN = 0.1, PRECISION_RAMP_OUT = 0.15;
	/** Fire rate x2 (13.33/s), damage x0.5 (1 a pellet), falloff x1.5 (15-30 m) (D #635 / #637 / #632; W 13.33/s, 1 -> 0.3, 15-30 m) H. */
	public static final double PRECISION_RATE = 2, PRECISION_DAMAGE = 0.5, PRECISION_FALLOFF = 1.5;
	/** Reload again ends it, from 0.4 s in (D #650 / #647) H. */
	public static final double PRECISION_CANCEL_AFTER = 0.4;
	/** Its cooldown, 7.5 s from its end (D 514D #610 / #613 v472 7.5; W 8 s) H (dva.md "Where the sources disagree" 5: D). */
	public static final double PRECISION_COOLDOWN = 7.5;
}
