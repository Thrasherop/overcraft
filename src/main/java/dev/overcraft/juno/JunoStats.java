package dev.overcraft.juno;

/**
 * Juno's numbers (tools/reference/juno.md has every quote, revision and date; the game data map is juno-map.md, build 154088
 * of 2026-10-06, with its detail in E:\Overwatch Archive\research\juno\work_kit\kit_juno.md). Sources: the game data (D, with
 * its value id or graph node), Blizzard patch notes (BZ), the Overwatch wiki (W), and the owner's Workshop recording of her
 * in the real game (R: 2026-11-01, `tools/workshop/analysis/juno.py`; 25 double jumps, 7 glides, the hovers). No patch has
 * changed her kit since the data build (BZ 2026-10-06: hit volumes only), so the data's numbers are the live ones; where none
 * gives a value, it's ours and says so. Filled in step by step (juno-plan.md, "Order").
 */
public final class JunoStats {
	private JunoStats() {
	}

	/** Support, Survivor: 75 health and 150 shields (D 4448 #119: 0BA4 / 0BA6; W; BZ). */
	public static final int HEALTH = 75;
	public static final int SHIELDS = 150;

	/** Orbital Ray's cost (D 0B1B 2900; W 2900; BZ only percentages, ~2894). */
	public static final double ULT_COST = 2900;

	// ---- Martian Overboots (4441) ----
	/**
	 * The double jump: her vertical speed set to 10.8 m/s, her horizontal speed kept (R: 10.80 every one of 13, whatever she was
	 * doing before; W 10.8; D x1.8 of the mover's 6.0 jump field, #570), from 0.1 s after leaving the ground (D #244, M; R the
	 * earliest 0.16 s), once per airtime (twice with Lift Off, D #652), back on landing. About 3.4 m up from a standstill (R).
	 */
	public static final double DOUBLE_JUMP_SPEED = 10.8, DOUBLE_JUMP_FROM = 0.1;
	public static final int AIR_JUMPS = 1, AIR_JUMPS_LIFT_OFF = 2;
	/**
	 * The hover: jump held in the air brings her fall to 3 m/s (D 1A5E, #382; R exactly -3.00), once no ground is within 2.5 m
	 * below (D #376, M; R only the tall drops caught it). Not at once: from -9.2 m/s it eased to -3.1 in 0.27 s, the gap shrinking
	 * about 16 per second (R: -6.86, -5.67, -4.95, -4.47 ... at 62.5 ticks a second).
	 */
	public static final double HOVER_FALL = 3, HOVER_CLEARANCE = 2.5, HOVER_EASE = 16;

	// ---- Glide Boost (47B7) ----
	/**
	 * A 14 m/s horizontal burst added along her move input (forward without), vertical speed cancelled (D #589; R 5.5 -> 19.13
	 * one tick in, 5.81 -> 19.44: 14 less a tick's slowing; W's 19.1 is the total from a run).
	 */
	public static final double GLIDE_BURST = 14;
	/**
	 * Its slowing back to her glide speed (R, every glide alike): about 3 % a tick (1.86 x her speed per second) down to 12.45
	 * m/s, then a steady 23 m/s² (1.86 x 12.45), to her +40 % run speed (7.7 m/s) at 0.5 s; held there. With no key held she
	 * keeps her speed (R 7.3-7.7 m/s): the glide goes on.
	 */
	public static final double GLIDE_SLOWING = 1.86, GLIDE_SLOWING_FLOOR = 12.45;
	/** Up to 4 s (D 13BB); a second press ends it from 0.4 s (D #793 / #799: toggle, the default). */
	public static final double GLIDE_TIME = 4, GLIDE_CANCEL_FROM = 0.4;
	/** Its mover: air acceleration 12, air speed max 9 (D #528); +40% move speed (D 1A38, #742); gravity off (D #780 / #761). */
	public static final double GLIDE_AIR_ACCELERATE = 12, GLIDE_MAX_AIR = 9, GLIDE_SPEED = 0.4;
	/**
	 * Over a step or up a slope it rises at up to 3 m/s (D #812-#816 x830FF3AA +3; R the glides' vertical speed tops at 3.00,
	 * 12.5 m up a stair in one glide); ours 3 m/s while something blocks her.
	 */
	public static final double GLIDE_CLIMB = 3;
	/** Knockback taken while gliding: -50% (D x0A04B51B = 1DF3 -0.5, #774). */
	public static final double GLIDE_KNOCKBACK = 0.5;

	// ---- What the HUD shows (each ability's step fills in the rest) ----
	/** The Mediblaster's magazine (D 4445; W 180). */
	public static final int MEDIBLASTER_AMMO = 180;
	/** Glide Boost (ABILITY 1): its cooldown from the glide's end (D 47B7 #538, 1625 6 s; W 6). */
	public static final double GLIDE_COOLDOWN = 6;
	/** Hyper Ring (ABILITY 2) (D 4857, 14 s; W 14). */
	public static final double RING_COOLDOWN = 14;
	/** Pulsar Torpedoes (the right button): 10 s, 2 s when nothing was locked (D 4695; W). */
	public static final double TORPEDO_COOLDOWN = 10, TORPEDO_COOLDOWN_NONE = 2;

	// ---- Perks (the live list D 0065.177) ----
	public static final double PERK_MINOR_XP = 2110, PERK_MAJOR_XP = 2110 + 3165;   // D 12B9 / 12BA; W 2110 / 5275
}
