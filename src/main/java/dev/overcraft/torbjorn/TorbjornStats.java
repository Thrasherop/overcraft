package dev.overcraft.torbjorn;

/**
 * Torbjörn's numbers. Sources: the game data (D: build 154088 of 2026-10-06, tools/reference/torbjorn-map.md and its node-by-node
 * readings E:\Overwatch Archive\research\torbjorn\work_kit\kit_torbjorn.md / kit_turret.md / kit_overload_ult.md: the value id or
 * graph node), Blizzard's patch notes (BZ) and the Overwatch wiki (W rev 409440): tools/reference/torbjorn.md has every date. His last
 * live number change is BZ 2026-09-08 (Rivet Gun's alt spread 4.5 -> 4, Anchor Bolts 50 -> 25 %), before the data build and in it.
 * No Workshop recording of him: his movement is the default mover's. Confidence: H read in the data or the sources agree, M one link
 * inferred, L a guess; where no source gives a value it's ours.
 */
public final class TorbjornStats {
	private TorbjornStats() {
	}

	/** Damage, Specialist: 225 health + 75 armor (D 0DC8: 0740 / 0741; BZ 2024-08-20; W) H. */
	public static final int HEALTH = 225, ARMOR = 75;

	/** Molten Core's cost (D 0AFF 2350; W 2350) H. */
	public static final double ULT_COST = 2350;

	/** Perk XP: 1750 to the minor, 2625 more to the major (D 38D0: 12B3 / 12B4; W 1750 / 4375 in all) H. */
	public static final double PERK_MINOR_XP = 1750, PERK_MAJOR_XP = 1750 + 2625;

	// ---- Rivet Gun (1683; the left button, the right its alt) ----
	/** 18 rivets (D 0744; W) H. */
	public static final int AMMO = 18;
	/** A rivet: 70 (D 0742; W), x2 on the head (D #171 m_66D29548 2, M; W ✓), no falloff (D #171, M; W) H. */
	public static final double RIVET_DAMAGE = 70, CRIT = 2;
	/** 70 m/s (D 0745; W), falling at 9.8 m/s² (D #171 m_599AB8D7) H. */
	public static final double RIVET_SPEED = 70, RIVET_GRAVITY = 9.8;
	/** Its hit size: 0.1 m + the global 0.075 (D 290D; W 0.175) H. */
	public static final double RIVET_RADIUS = 0.175;
	/** 0.51 s from a rivet to the next shot (D #237 v173; W) H. */
	public static final double RIVET_INTERVAL = 0.51;
	/** The alt: 10 pellets of 12.5 (D 1AB1 / 0743; W) at 120 m/s, no gravity (D 0746; W) H. */
	public static final int PELLETS = 10;
	public static final double PELLET_DAMAGE = 12.5, PELLET_SPEED = 120;
	/** Over 4 degrees (D 1AB7; BZ 2026-09-08 4.5 -> 4) in a ring: 1 at the centre, 4 at half the spread, 5 at the edge (D #178) H. */
	public static final double PELLET_SPREAD = 4;
	/** Their falloff: full to 10 m, x0.3 from 20 m (D 1AB2 / 1AB3, m_72A81154; W 12.5 -> 3.75) H/M. */
	public static final double PELLET_FALL_START = 10, PELLET_FALL_END = 20, PELLET_FALL_MIN = 0.3;
	/** 0.05 m (D 28A5; W) H; 2 ammo a blast (D 131B; W) H; 0.7 s to the next shot of either fire (D #238; W) H. */
	public static final double PELLET_RADIUS = 0.05;
	public static final int ALT_AMMO = 2;
	public static final double ALT_INTERVAL = 0.7;
	/** The reload: 2.0 s, the rivets back at half of it, 1.0 s (D 0747, #204 v6884 0.5; W 2 s / 0.992) H. */
	public static final double RELOAD = 2.0, RELOAD_AMMO_SHARE = 0.5;
	/** Where the shots leave from, in his look's frame (right, up, ahead; ours: the data's muzzle is hp 0006 on the gun). */
	public static final double MUZZLE_RIGHT = 0.25, MUZZLE_UP = -0.2, MUZZLE_AHEAD = 0.5;

	// ---- Forge Hammer (168C; the 1 / 2 keys or the wheel) ----
	/** 70 a swing (D 1AB6; BZ 2024-12-10 55 -> 70; W) H, no crit (W) H. */
	public static final double HAMMER_DAMAGE = 70;
	/**
	 * A swing every 0.6 s (D 168C #374 v215 = 0.6, the StateWait #325 that holds the whole swing) H; its hit volumes from 0.32 s (D #345's
	 * delay 0.32 / attack speed, then #350's 0.2 s window) H. W says 1.43 a second (0.352 s in + ~0.1 + 0.6 s) and 0.6 s in Overload: D's
	 * 0.6 / 1.43 = 0.42 s there and its 0.32 / 1.43 = 0.224 s match W's "initial swing time 0.11 s shorter"; W's extra 0.1 s isn't in the
	 * graph (a press's own latency, L). D kept.
	 */
	public static final double HAMMER_INTERVAL = 0.6, HAMMER_HIT_AT = 0.32;
	/** The Rivet Gun reloads itself after 2 s with the hammer out (W) M. */
	public static final double HAMMER_RELOAD = 2;
	/** Its reach: a 1 m sphere 1.5 m ahead of him, 1.2 m up (D #256: 2040 x (0, 1.2, 1.5)) H/M; W 2.5 m, the quick melee's. */
	public static final double HAMMER_AHEAD = 1.5, HAMMER_UP = 1.2, HAMMER_RADIUS = 1.0;
	/** A hit on his built, hurt turret repairs 50 (D 08F8; W) H. */
	public static final double HAMMER_REPAIR = 50;
	/** Swapping weapons takes 0.4 s (W) M. */
	public static final double SWAP = 0.4;

	// ---- Deploy Turret (16C5; the turret 28B7: 1688 / 1689 / 38EA; Shift) ----
	/** The cast holds his weapons 0.5 s (D #492), the turret leaves at 0.05 s (D #463 v670) H. */
	public static final double TURRET_CAST = 0.5, TURRET_THROW_AT = 0.05;
	/** Thrown at 17 m/s (D 10FA; W), falling at 20 m/s² (D #557 v7389, M that it's gravity) H. */
	public static final double TURRET_THROW_SPEED = 17, TURRET_GRAVITY = 20;
	/** Anchor Bolts: x1.25 the throw (D 51F6 #354 0.25; BZ 2026-09-08 50 -> 25 %) H. */
	public static final double ANCHOR_THROW = 1.25;
	/** It builds itself in 2.5 s (D 091A, #103; BZ 2024-12-10 3 -> 2.5; W) H. */
	public static final double TURRET_BUILD = 2.5;
	/** 225 health (D 00C7 / 075D; BZ 2025-03-18 250 -> 225; W) H. D's second pool 078C 75 is unnamed (M): left out, as W has 225. */
	public static final double TURRET_HEALTH = 225;
	/** 12 a shot (D 0765; BZ 2025-03-18 13 -> 12; W) H, hitscan, no crit (W) H. */
	public static final double TURRET_DAMAGE = 12;
	/**
	 * A shot every 0.256 s (W, 4 / s) M. D reads 0.5 s at its level 2 (1689 #173: 1 / (0830 4 / 2)) and 0.25 s only at its level 3
	 * (#181): W's 4 / s is kept (BZ 2026-02-18 fixed "the rate of his ... Turret fire"; W's DPS numbers are the live game's), L which.
	 */
	public static final double TURRET_INTERVAL = 0.256;
	/** It aims at enemies within 40 m (D 1B0C, 1688 #792; W) H. */
	public static final double TURRET_RANGE = 40;
	/**
	 * Who it shoots (W, M: D's turret AI is a native component with no rule in the graphs): the closest enemy in sight, until it's
	 * dead or out of sight; his rivet's hit on an enemy makes that one its target. Its turn: ours, 30 degrees a tick, firing once
	 * within 10 degrees (the data has no turn rate).
	 */
	public static final double TURRET_TURN = 30, TURRET_FIRE_CONE = 10;
	/** Its gun's height above its feet (ours: the model's barrels, by eye). */
	public static final double TURRET_GUN_UP = 0.65;
	/** Its box (D 28B7's mover: radius 0.5, height 1.0) H. */
	public static final float TURRET_WIDTH = 1.0f, TURRET_HEIGHT = 1.0f;
	/** The cooldown: 6 s from the cast (D #466: 07DF 12 / 2); when the turret goes, 12 s if it was in combat, else 6 s (D #531) H. */
	public static final double TURRET_COOLDOWN = 12, TURRET_COOLDOWN_CALM = 6;
	/** In combat: it fired in the last 1 s or took damage in the last 3 s (D #509, 1AF4) H/M. */
	public static final double COMBAT_FIRED = 1, COMBAT_HIT = 3;

	// ---- Overload (1721; E) ----
	/** 6 s from the press (D 0771, #88; BZ 2025-09-16 5 -> 6; W) H; his weapons held 0.7 s (D #88 v670; W 0.686) H. */
	public static final double OVERLOAD_TIME = 6, OVERLOAD_CAST = 0.7;
	/** 10 s, from its end (D 0772, #243; W 10) H. */
	public static final double OVERLOAD_COOLDOWN = 10;
	/** 100 overhealth, held whole while it runs (D 1AB9, #79; W) H. */
	public static final double OVERLOAD_OVERHEALTH = 100;
	/** +30 % move speed (D 1AB8; W) H, +43 % attack speed (D 1ADB; W 0.51 -> 0.368 s) H, his reload 2.0 -> 1.4 s (D 1B13 0.3; W) H. */
	public static final double OVERLOAD_SPEED = 0.3, OVERLOAD_ATTACK = 0.43, OVERLOAD_RELOAD = 0.3;
	/** 6 rivets given at the press (D 1ABF; W "Restores 6 ammo") H/M. */
	public static final int OVERLOAD_AMMO = 6;

	// ---- Molten Core (16DA / 16DD / 16DE; Q) ----
	/** The cast: 0.75 s (D #370 v670; W 0.592) H, then 5.5 s to fire his 10 (D #363 chase from 6 - 0.5; W 5.5) H. */
	public static final double ULT_CAST = 0.75, ULT_TIME = 5.5;
	/** The cast's burst 34B0 comes 0.27 s into it (clip 2B61 / 2B4A's effect key: tools/reference/torbjorn-effects.md, H). */
	public static final double ULT_CAST_BURST = 0.27;
	/** 10 slag shots (D 074E; W), 6.5 a second (D #382; W 1 per 0.144 s held), 40 m/s (D 074D; W) falling at 30 (D #462, M) H. */
	public static final int SLAG_SHOTS = 10;
	public static final double SLAG_RATE = 6.5, SLAG_SPEED = 40, SLAG_GRAVITY = 30;
	/** 25 on a direct hit (D 1ADC; W) H; 0.2 m against creatures (D 290E; W) H. */
	public static final double SLAG_DAMAGE = 25, SLAG_RADIUS = 0.2;
	/** A pool: 2.4 m across its radius (D 1D4D; W), formed 0.5 s after it lands (D 16DD #44), then 10 s (D 1AE1; W) H. */
	public static final double POOL_RADIUS = 2.4, POOL_FORM = 0.5, POOL_TIME = 10;
	/**
	 * Its burn: 190 a second (BZ 2025-09-16 160 -> 190; W), +100 more on armor (W). D reads 100 to all health and 190 on armor
	 * (16DE #148 / #147, M on the filter): BZ and W kept, L which. Pools don't add up (W "do not stack") M. Ours: Minecraft's
	 * creatures have no Overwatch armor, so the +100 goes on a hero's armor layer only.
	 */
	public static final double POOL_DPS = 190, POOL_ARMOR_DPS = 100;

	// ---- Specialist (01C7, his sub-role) ----
	/** An elimination: +0.5 reload speed for 3 s, used up by one reload (D 23D5 / 23D6, as Junkrat's; BZ 2026-02-10) H/M. */
	public static final double SPECIALIST_SPEED = 0.5, SPECIALIST_TIME = 3;

	// ---- Perks (51F6) ----
	/** Hammer Time: +20 % move speed with the hammer out, not in Molten Core (D 265C, #422; W) H. */
	public static final double HAMMER_TIME_SPEED = 0.2;
	/**
	 * Overloaded Turret: while Overload runs his built turret is at level 3: +150 health (D 1688 #69; W), 1.7 s of upgrading with no
	 * fire first (D 00DE 51 / 30 s; W "2 second upgrade delay", L between), then 3 missiles of 30 every 1.5 s beside its gun (W; D's
	 * level 3 is its 4 / s, W's level 2 rate already: L). Back to level 2 when Overload ends (D #444; the perk text's 5 s is
	 * Overload's old length, M).
	 */
	public static final double L3_HEALTH = 150, L3_UPGRADE = 1.7, MISSILE_DAMAGE = 30, MISSILE_EVERY = 1.5;
	public static final int MISSILES = 3;
}
