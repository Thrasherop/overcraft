package dev.overcraft.potg;

import java.util.Map;

/**
 * Play of the Game: every scoring value in one place, for the owner to tune (owner, 2026-11-11: "I will want to adjust
 * some stuff with it ... Don't make those changes without my feedback"). Marks: D the pulled data (build 154088), BZ
 * Blizzard (patent US 10,456,680 B2, patch notes), W the wiki, ours. What the data search found and didn't is in
 * techniques/potg/design.md, "What the game data says about scoring": the categories' names are there (D), the events,
 * values and the selection aren't (the selection runs on Blizzard's servers, BZ), so every number below is ours.
 *
 * To change the scoring, change this table. The per-mob and per-item maps are empty on purpose: progression (blazes,
 * blaze rods, ender pearls, eyes) waits for the owner's word (the note lists the proposals).
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

	// ---- per mob, per item: empty until the owner says (owner, 2026-11-11) ----
	/**
	 * A kill's worth by entity type id ("minecraft:blaze"), replacing the victim's health; added to High Score. Only the
	 * dragon has one, the owner's #44 ("the last hits count"): its health would be 2500 capped to KILL_CAP, so its worth
	 * is the cap and this on top. Ours.
	 */
	public static final Map<String, Double> KILL_BONUS = Map.of("minecraft:ender_dragon", 1500.0);
	/** Picking up an item, by item id ("minecraft:blaze_rod"), to High Score. Empty: progression is the owner's call. */
	public static final Map<String, Double> PICKUP = Map.of();
}
