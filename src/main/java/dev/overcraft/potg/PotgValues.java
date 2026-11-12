package dev.overcraft.potg;

import java.util.Map;

/**
 * Play of the Game: every scoring value in one place, for the owner to tune (owner, 2026-11-11: "I will want to adjust
 * some stuff with it ... Don't make those changes without my feedback"). Marks: D the pulled data (build 154088), BZ
 * Blizzard (patent US 10,456,680 B2, patch notes), W the wiki, ours. What the data search found and didn't is in
 * techniques/potg/design.md, "What the game data says about scoring": the categories' names are there (D), the events,
 * values and the selection aren't (the selection runs on Blizzard's servers, BZ), so every number below is ours.
 *
 * To change the scoring, change this table. The progression values at the end (kills of the run's mobs, rods and
 * pearls, crystals, eyes, the portal, first entries) are the owner's (2026-11-11), each ours (no source); the note's
 * "Progression events" has the decision.
 */
public final class PotgValues {
	private PotgValues() {
	}

	// ---- the window and the choice ----
	/** Seconds in a window. W H: the replay is 12 s. */
	public static final int WINDOW_SECONDS = 12;
	/** The window slides in steps of this many seconds. BZ H (the patent's "increments"); 1 s ours. */
	public static final int STEP_SECONDS = 1;
	/** High Score under this gives way to the best of the others, if any scored. BZ M (the patent's option); the number ours. */
	public static final double HIGH_SCORE_FLOOR = 300;

	// ---- events ----
	/** High Score: each Overwatch HP of damage to a non-bystander. Ours (BZ: "total damage" counts; no weight given). */
	public static final double DAMAGE_PER_HP = 1;
	/** High Score: a kill's base worth is the victim's health in Overwatch HP, capped here. Ours. */
	public static final double KILL_CAP = 1000;
	/** High Score: an ult's start. Ours. */
	public static final double ULT_START = 200;
	/** Ult events scaled by this. BZ M: "may be reduced by a set amount or share"; the share ours. */
	public static final double ULT_SHARE = 0.75;
	/** Lifesaver: each Overwatch HP healed. Ours (BZ: healing counts toward Life Saver). */
	public static final double HEAL_PER_HP = 1;
	/** Sharpshooter: a critical hit's damage again, times 1 + distance / this many metres. Ours (BZ: distance counts). */
	public static final double SHARP_METRES = 15;
	/** Shutdown: killing a lit creeper, or a hero in their ult. Ours (BZ: "stopping a powerful ability, mainly an ult"). */
	public static final double SHUTDOWN_KILL = 500;
	/** A death after a best window still belongs to it within this many ticks; the play is taken again this long after. Ours. */
	public static final int DEATH_REACH_TICKS = 60;
	public static final int DEATH_TAIL_TICKS = 20;

	// ---- progression: the owner's decisions (owner, 2026-11-11), each ours (no source); no anti-farm cap (owner: "there
	// won't be farms. This is a one-off speedrun") ----
	/**
	 * A kill's worth by entity type id ("minecraft:blaze"), on top of the victim's health (capped to KILL_CAP); to High
	 * Score. The dragon's is the owner's #44 ("the last hits count"): 2500 health capped to 1000, and this on top. The rest
	 * owner, 2026-11-11: a wither skeleton ~700 in all, "about 1.4 of a zombie" (a zombie is ~500 with its damage).
	 */
	public static final Map<String, Double> KILL_BONUS = Map.of(
		"minecraft:ender_dragon", 1500.0, // ours (#44)
		"minecraft:blaze", 750.0, // owner, 2026-11-11
		"minecraft:enderman", 500.0, // owner, 2026-11-11
		"minecraft:wither_skeleton", 200.0, // owner, 2026-11-11
		"minecraft:elder_guardian", 1000.0, // owner, 2026-11-11
		"minecraft:wither", 2000.0); // owner, 2026-11-11
	/** Picking up an item, by item id, to High Score: per item, no cap (owner, 2026-11-11). */
	public static final Map<String, Double> PICKUP = Map.of(
		"minecraft:blaze_rod", 250.0, // owner, 2026-11-11
		"minecraft:ender_pearl", 150.0); // owner, 2026-11-11
	/** An end crystal destroyed by a hero, directly or by their projectile or explosion. Owner, 2026-11-11 ("there is like
	 *  9 of them ... not the most exciting POTG"). Not Shutdown (undecided). */
	public static final double END_CRYSTAL = 400;
	/** An eye of ender placed in an End portal frame. Owner, 2026-11-11. */
	public static final double EYE_PLACED = 100;
	/** The eye that completes and opens the portal: this instead of EYE_PLACED, not on top. Owner, 2026-11-11. */
	public static final double PORTAL_OPENED = 200;
	/** A player's first entry into the Nether, and into the End: once per player per world, saved with it. Owner, 2026-11-11. */
	public static final double FIRST_NETHER = 500;
	public static final double FIRST_END = 1000;
}