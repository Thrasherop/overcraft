package dev.overcraft.sigma;

/**
 * Sigma's numbers (tools/reference/sigma.md, each with its source: D = his game data, build 154088; BZ = Blizzard's patch
 * notes; W = the Overwatch wiki; H / M / L = confidence), built step by step (sigma-plan.md "Order"). Nothing has changed
 * his kit since the data build (the last change BZ 2026-07-14 is in it), so D's numbers are the live ones.
 */
public final class SigmaStats {
	private SigmaStats() {
	}

	// ---- base (sigma.md "Base") ----
	/** Health without the role's bonus (D Hero Stats 0DA0 047C 200, alt 300; W 200) H. The Tank role adds 150 (Tank). */
	public static final int HEALTH = 200;
	/** Shields (D 047D 250; BZ 2026-07-14 275 -> 250; W 250) H. */
	public static final int SHIELDS = 250;
	/** Ult cost, Gravitic Flux (D 0AFA 2300, alt 3800; W 2300) H. */
	public static final double ULT_COST = 2300;
	/** Perk XP: the minor at 1955, the major 2935 more (4890) (D 12AA / 12AB on 34C3 #99; W 1955 / 4890) H. */
	public static final double PERK_MINOR_XP = 1955, PERK_MAJOR_XP = 4890;

	// ---- Hyperspheres (sigma.md "Hyperspheres", D 187F) ----
	/** Two spheres a volley, the second 0.2 s after the first (D 187F #175 -> #199 wait 0.2 -> #204; W 0.192) H / M. */
	public static final double VOLLEY_GAP = 0.2;
	/** Then 1.3 s before the next volley ("Reload Time" 0479, v173 chased to 0 at attack speed: D #99 / #102): 1.5 s a volley H. */
	public static final double VOLLEY_RELOAD = 1.3;
	/** 50 m/s (D 0478, v6368; W 48) H. */
	public static final double SPHERE_SPEED = 50;
	/** 0.32 m across (D 28DA 0.2 + the global 28A4 0.12; W 0.32): its radius vs creatures 0.16 m H / M. */
	public static final double SPHERE_SIZE = 0.32;
	/** Its radius against blocks (D: environment 0.075) H. */
	public static final double SPHERE_BLOCK_RADIUS = 0.075;
	/** Bursts after 21 m + 0.7 m x his scale, at 50 m/s: 0.434 s (D 1AAB 21, #240 / #241) H. */
	public static final double SPHERE_LIFETIME = (21 + 0.7) / SPHERE_SPEED;
	/** A slight drop (D the volley's motion field m_599AB8D7 = 5.5, read as gravity as Accretion's 18 is) M. */
	public static final double SPHERE_GRAVITY = 5.5;
	/** On a direct hit (D 0474, v2577; BZ 2025-12-12 25 -> 20; W 20) H. */
	public static final double SPHERE_DIRECT = 20;
	/** Its burst: 40 (D 0477, v11863; W 40) H, full within 1 m (D 1B63 ring v31805) H, x0.5 at its 2.85 m edge (D 047A ring v31806, factor 1AF5 0.5; W 12 at 2.9 m) M, linear between (ours, M). */
	public static final double SPHERE_BURST = 40, SPHERE_BURST_FULL = 1.0, SPHERE_BURST_RADIUS = 2.85, SPHERE_BURST_EDGE = 0.5;
	/**
	 * The pull of its burst: horizontal -0.75 (inward), vertical +1.0 m/s (D 1B64 / 1B65; W 1.3 m/s inward), scaled by its
	 * rings, x0.8 at 0.5 m and x0.2 at 2.8 m (D the knockback rings, M reading), linear between (ours).
	 */
	public static final double SPHERE_PULL = 0.75, SPHERE_PULL_UP = 1.0, SPHERE_PULL_NEAR = 0.5, SPHERE_PULL_NEAR_FACTOR = 0.8,
		SPHERE_PULL_FAR = 2.8, SPHERE_PULL_FAR_FACTOR = 0.2;

	// ---- Experimental Barrier (sigma.md "Experimental Barrier", D 1B55 on him, 1A2A on the barrier) ----
	/** 650 health (D 048F; BZ 2026-02-10 700 -> 650; W 650) H. */
	public static final double BARRIER_HEALTH = 650;
	/** 0.122 of its max a second = 79.3/s (D 0490 in percent mode, 1A2A #664; W 79.3; BZ's 85 was 700's) H. */
	public static final double BARRIER_REGEN = 0.122 * BARRIER_HEALTH;
	/** From 2 s after it is back with him (D 1A2A #658, state 1 only) H. */
	public static final double BARRIER_REGEN_DELAY = 2;
	/** Sent at 20 m/s along his aim (D 0F04, v1002, #636; BZ 2024-04-16 16.5 -> 20; W 20) H, no range limit (D 9999) H. */
	public static final double BARRIER_SPEED = 20;
	/** It stops when a block is 0.9 m ahead of it (D #638's 0.9 m ray) H. */
	public static final double BARRIER_STOP_RAY = 0.9;
	/**
	 * Where it appears: along a 3.2 m ray from his eyes, 0.9 m short of what the ray hits (D 1A2A #740 / #738) H nodes, M that
	 * the ray starts at his eyes; its centre there (ours).
	 */
	public static final double BARRIER_DEPLOY_RAY = 3.2, BARRIER_DEPLOY_BACK = 0.9;
	/** 1 s before it can go out again after a recall (D 0493, 1B55 #1041; BZ 2025-07-22 2 -> 1; W 1) H. */
	public static final double BARRIER_RECALL_COOLDOWN = 1;
	/** 5 s after it is destroyed (D 1BC1, #1099 / #1100; W 5) H. */
	public static final double BARRIER_BROKEN_COOLDOWN = 5;
	/** Its mesh at rest: 6.70 m wide, 4.18 m tall, 0.73 m deep, curved toward the enemy (D C015's bounds) M (its clips may shape it). */
	public static final double BARRIER_WIDTH = 6.70, BARRIER_HEIGHT = 4.18;
	/** 2% of what it absorbs goes to his ult (D 1A2A #683 v337 x 0.02, as Brigitte's) M. */
	public static final double BARRIER_ULT_SHARE = 0.02;

	// ---- Kinetic Grasp (sigma.md "Kinetic Grasp", D 1D02 / 1D03) ----
	/** 2 s (D 04AD, #489) H. */
	public static final double GRASP_TIME = 2;
	/**
	 * Its box about his eyes, along his aim: 1 m either side, 1.6 m down to 1.1 m up, 0.4 m to 2.6 m ahead (D #274
	 * StateDeflectProjectiles (-1, -1.6, 0.4) -> (1, 1.1, 2.6) x his scale) H values, M the origin.
	 */
	public static final double GRASP_SIDE = 1, GRASP_DOWN = 1.6, GRASP_UP = 1.1, GRASP_NEAR = 0.4, GRASP_FAR = 2.6;
	/** 60% of the damage absorbed becomes overhealth at a normal end (D 07A1 0.6, #593; W 60%) H. */
	public static final double GRASP_CONVERSION = 0.6;
	/** Up to 400 (D 07A0, 1D03 #201-#207; W 400) H. */
	public static final double GRASP_MAX = 400;
	/** Held 3 s (D 1D53), then 5 a second (D #545, absolute mode, M rate) H / M. */
	public static final double GRASP_HOLD = 3, GRASP_DECAY = 5;
	/** 5% of what it absorbs to his ult (D #432) M. */
	public static final double GRASP_ULT_SHARE = 0.05;
	/** Kinetic Grasp: 12 s from its end (D 0494; W 12 s) H. */
	public static final double GRASP_COOLDOWN = 12;
	/** Accretion: 10 s from 0.45 s after the throw (D 0498, #669; W 10 s) H. */
	public static final double ACCRETION_COOLDOWN = 10;
	/** Its cooldown starts this long after the throw (D 1BBB #669: a chase from 0.45 to 0, then #570 v61 = 10) H nodes, M timing. */
	public static final double ACCRETION_COOLDOWN_DELAY = 0.45;

	// ---- Accretion (sigma.md "Accretion", D 1BBB on him, 1EB9 on the rock) ----
	/** The gather: 0.65 s (D #869 v670; anim 0x608 at 0.65 s; W 0.64) H. */
	public static final double ACCRETION_CAST = 0.65;
	/** -75% move speed while gathering (D 1B69 0.75 into v23184, #651 / #681; W -75%) H, and no jump (#651 x5C571F43 / x292F571E = 0) H nodes, M meaning. */
	public static final double ACCRETION_CAST_SLOW = 0.75;
	/** The rock: 37 m/s (D 0499, v6368; W 37) H, 18 m/s² of gravity (D #869 v7389 = 18) H, 30 s at most (D) H. */
	public static final double ROCK_SPEED = 37, ROCK_GRAVITY = 18, ROCK_LIFETIME = 30;
	/**
	 * Its size: 0.9 m against creatures (D 265F, v76077; W 0.9) read as its radius (its model C5CA is 2 m across, drawn at its
	 * data size: M), 0.3 m against blocks (D 28C0) H.
	 */
	public static final double ROCK_SIZE = 0.9, ROCK_BLOCK_SIZE = 0.3;
	/** On a hit: 80 (D 049F, 1BBB v476; BZ 2024-07-09 40 -> 80; W 80) H. */
	public static final double ROCK_IMPACT = 80;
	/**
	 * The burst where it lands: 40 (D 0495, v11863; W 40) H, full to 1.5 m (D 0496) H, its edge at 2.5 m (D 0497) H. D's
	 * edge ring has factor 1 (no falloff read: L); ours W's 12 at the edge (x0.3), linear from 1.5 m (sigma-plan.md, M).
	 */
	public static final double ROCK_BURST = 40, ROCK_BURST_FULL = 1.5, ROCK_BURST_RADIUS = 2.5, ROCK_BURST_EDGE = 12.0 / 40;
	/** The one it hits is knocked down 1.1 s (D 04A0; #590 max(1.1, ...); BZ 2023-04-11 0.8 -> 1.1; W 1.1) H. */
	public static final double ROCK_KNOCKDOWN = 1.1;
	/** Knockback on a hit: 6 m/s along its flight, 3 up (D 1B67 / 1B68; W 6.7 m/s) H. */
	public static final double ROCK_KNOCKBACK = 6, ROCK_KNOCKBACK_UP = 3;
	/** The burst's: 5 m/s away, 3 up (D 1B6A / 1B6B, M which pair; W 5.8 m/s) M. */
	public static final double ROCK_BURST_KNOCKBACK = 5, ROCK_BURST_KNOCKBACK_UP = 3;
	/** +25 ult charge on a hit (D 1BBB #683 STU_008D30B1 25) M. */
	public static final double ROCK_ULT_ON_HIT = 25;

	// ---- Gravitic Flux (sigma.md "Gravitic Flux", D 1E41 on him, 1DBC on each target) ----
	/** The start: 0.6 s rising at 10 m/s² before the aim (D #954 xA634D7B9 = up x 10, #967 wait 0.6; anim 0x1B8 0.4 s) H nodes, M field. */
	public static final double FLUX_START = 0.6, FLUX_START_RISE = 10;
	/** The aim: up to 5 s (D 04AE, v567, chase #1271, then it fires itself #1294; W 5 s) H; out to 35 m (D #1268 m_6386E7B4; W 35) H. */
	public static final double FLUX_AIM = 5, FLUX_RANGE = 35;
	/** The cast: 1 s (D #961 v670; W 1 s) H. */
	public static final double FLUX_CAST = 1;
	/** The cylinder: 7.5 m radius (D 1867, alt 5.0; W 8: D's) H, 15 m tall (D 1DFB) H, its base 5 m under the aimed point (D #977 / #1292) H. */
	public static final double FLUX_RADIUS = 7.5, FLUX_HEIGHT = 15, FLUX_BELOW = 5;
	/** His lift phase: 2.5 s (D 1868, v68035; W 2.5) H; then his slam phase 0.5 s (D #1039) H. */
	public static final double FLUX_LIFT_PHASE = 2.5, FLUX_SLAM_PHASE = 0.5;
	/** Each target: 50 at the lift (D 04AB, v476; W 50) H, pulled 1 m/s in (D 1DBC #575 m_E33EFEE1 -1) M. */
	public static final double FLUX_LIFT_DAMAGE = 50, FLUX_LIFT_PULL = 1;
	/** Held still (no move, no jump, no abilities) for 0.4 s (D 23F2, v74536; 1DBC #586 / #552) H. */
	public static final double FLUX_HOLD = 0.4;
	/** Lifted 2 s (D 1DBC #597 v21244 = 2.0) H. */
	public static final double FLUX_LIFT_TIME = 2.0;
	/** Lifted: gravity x0.1, move speed x0.55 (D 1DBC #574 / #571: 1 - (1 - 0.1) x 0.5) H values, M meanings. */
	public static final double FLUX_LIFT_GRAVITY = 0.1, FLUX_LIFT_SPEED = 0.55;
	/**
	 * The rise: 18 m/s² up (D #615 vec(0, 18, 0)), 5 in its last 0.2 s (#604 / #606), held to 3 m/s up from the end of the
	 * hold (#608 x57643451 = 3) H values, M meanings (read as Levitation's accel and cap): about 6 m in all.
	 */
	public static final double FLUX_RISE = 18, FLUX_RISE_END = 5, FLUX_RISE_END_TIME = 0.2, FLUX_RISE_CAP = 3;
	/** The slam: 50% of the target's max health (D 1AAD, v71023; 1DBC #577, m_FB3F6CA9 = 1: no modifiers; W 50%) H, once ground is within 5 m below (#644) H. */
	public static final double FLUX_SLAM_SHARE = 0.5, FLUX_SLAM_GROUND = 5;
	/** ...at most 200 on the Ender Dragon, the Wither and the Warden (ours, sigma-plan.md "Minecraft decisions": Open). */
	public static final double FLUX_BOSS_CAP = 200;
	/** After the lift: gravity x22 (D #582) and move speed -50% (D 1DFA -0.5 / #579 0.5) for 0.6 s after the slam (D 1DFC, #585) H values, M meanings. */
	public static final double FLUX_HEAVY_GRAVITY = 22, FLUX_HEAVY_SLOW = 0.5, FLUX_HEAVY_TIME = 0.6;
	/** His flight: gravity off, 5.5 m/s +30% = 7.15 (D #976 / 1B66 0.3; W 7.15) H, half the knockback (D #1071 x0A04B51B -0.5; W) H. */
	public static final double FLUX_FLIGHT_SPEED = 5.5 * 1.3, FLUX_FLIGHT_KNOCKBACK = 0.5;
	/** His flying mover's acceleration, x the speed a second (D #976 m_5C6B4602 = 8) L reading. */
	public static final double FLUX_FLIGHT_ACCEL = 8;

	// ---- perks (sigma.md "Perks", D 51E0; the target filters' "enemy heroes" read as every creature: sigma-plan.md) ----
	/** Kinetic Cycle: at Grasp's normal end, Accretion's cooldown -0.02 s a point absorbed (D 1D02 #735-#738 v21312 x 0.02; W -2 s per 100) H. */
	public static final double KINETIC_CYCLE_PER_POINT = 0.02;
	/** Hyper Regeneration: 30% of his Hyperspheres' damage heals his barrier (D 51E0 #431-#443 -> 1A2A #1039 x0.3; BZ 2026-07-14 40 -> 30%) H. */
	public static final double HYPER_REGENERATION = 0.3;
	/** Hyper Strike: every 5 direct sphere hits (D 51E0 v181 = 5, counted on id 9C8B: #398-#402) H. */
	public static final int HYPER_STRIKE_HITS = 5;
	/**
	 * ...the next quick melee that lands knocks its target 14 m/s along his aim lifted 0.3 (D #391 v11272 = 14, #392
	 * norm(aim + (0, 0.3, 0)) x 14; W 15) H, hurts 50 more (D 54A5 #513 v476 = 50, over 0.2 s: ours at once; W +50) H, and
	 * floats it 0.8 s with gravity x0.01 and its input off (D 54A5 #454 v955 = 0.8, #404; W 1 s) H values, M meanings.
	 */
	public static final double HYPER_STRIKE_SPEED = 14, HYPER_STRIKE_LIFT = 0.3, HYPER_STRIKE_DAMAGE = 50, HYPER_STRIKE_FLOAT = 0.8,
		HYPER_STRIKE_GRAVITY = 0.01;
	/**
	 * Levitation: jump held in the air from 0.2 s after leaving the ground (D 51E0 #379 / #375 0.2) H, once a landing (v1843)
	 * H, up to 1.75 s (#383) H, accelerating at 25 m/s² (#382 STU_A9C4FF83 (0, 25, 0)) up to W's 2.3 m/s (Sigma rev 408635:
	 * "mspeed = 2.3 m/s", "up to a 5 meter height"). The data's "max 3" read as the cap (M) rose 6.38 m from a standing
	 * jump, 28% over W's 5 m (the critique's #16); 2.3 is W's measured speed. Ending when let go (H nodes).
	 */
	public static final double LEVITATION_FROM = 0.2, LEVITATION_TIME = 1.75, LEVITATION_ACCEL = 25, LEVITATION_SPEED = 2.3;

	// ---- his size (sigma.md "Size") ----
	/**
	 * His mover: 2.5 m tall, 0.7 m radius, crouched 2.05 m (D 2AB1, the default hero's 2.0 / 0.5 / 1.25) H. His body box in
	 * hero mode (dev.overcraft.hero.HeroBody: the owner, 2026-11-02, "Sigma should be unable to fit through a door"); his
	 * model is drawn at its data size (2.14 m: D 106F7 bounds), floating 0.3 m up in his stand (his clips).
	 */
	public static final double MOVER_HEIGHT = 2.5, MOVER_RADIUS = 0.7, MOVER_CROUCH_HEIGHT = 2.05;
	/** Falls hurt him from Minecraft's 3 m (Hero.safeFall's default): his data has the default falling component (D) L. */
	public static final double SAFE_FALL = 3;
}
