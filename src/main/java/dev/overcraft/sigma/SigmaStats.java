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
