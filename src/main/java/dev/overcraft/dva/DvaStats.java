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

	// ---- the kit's cooldowns and ammo the HUD shows now (their abilities: steps 4-9) ----
	/** Boosters' cooldown (D 33D9 03A9 4, 047E #221; BZ 2026-03-10 3.5 -> 4; W 4) H. */
	public static final double BOOSTERS_COOLDOWN = 4;
	/** Micro Missiles' cooldown (D 33D9 03B3 7, 1128 #372; W 7) H. */
	public static final double MISSILES_COOLDOWN = 7;
	/** The Light Gun's magazine (D 348C 03A8 20, 02B8 #325; W 20) H. */
	public static final int LIGHT_GUN_AMMO = 20;

	// ---- the Initiator sub-role (dva-map.md "Role: Tank and Initiator", 01C7) ----
	/** Boosters' start heals 50 over 1 s, not at full health (D 01C7 #2539 / #2857: 23D0 50, 23D1 1; BZ 2026-05-12 60 -> 50) H. */
	public static final double INITIATOR_HEAL = 50, INITIATOR_HEAL_TIME = 1;
	/** Then 5 s before it can heal again (D 01C7 #2889: v74592 chased from 257B 5) H. */
	public static final double INITIATOR_LOCKOUT = 5;
}
