package dev.overcraft.jetpackcat;

/**
 * Jetpack Cat's numbers in Overwatch units (metres, seconds, Overwatch health), live game as of the Oct 20 2026 patch
 * (tools/reference/jetpack_cat.md has every quote). Sources: Blizzard patch notes (BZ) first, the Overwatch wiki (W)
 * where the notes give no number, the game data where it holds one. Where none gave a value, it's ours and says so.
 */
public final class JetpackCatStats {
	private JetpackCatStats() {
	}

	// Base
	public static final int HEALTH = 225;                      // W
	/** Knockback taken while flying: -40% ("flight resistance", W). */
	public static final double KNOCKBACK_TAKEN = 0.6;

	// Jetpack (passive): permanent flight (game data: her flight mover, graph 59DC; tools/reference/movement.md)
	public static final double FLY_SPEED = 5.5;                // BZ Aug 14 2026 (back from 6)
	/** Air acceleration, times the wish speed (BZ Aug 11 2026: 7.7 -> 9.5; the data holds 9.5). */
	public static final double FLY_ACCELERATE = 9.5;
	/** Speed above this decays (the data's flight mover: max air speed 12). */
	public static final double FLY_MAX = 12;
	/** Drag with no input, per second (the data's flight mover: 8). */
	public static final double FLY_DRAG = 8;

	// Biotic Pawjectiles (primary fire): 1 centre pellet, 4 paws above it
	public static final double PAW_DAMAGE = 4;                 // BZ Sep 8 2026 (4.5 -> 4)
	public static final double CENTRE_DAMAGE = 8;              // W
	public static final int PAWS = 4;                          // W
	/** Headshots x2 on the paw pellets only (W). */
	public static final double PAW_CRIT = 2.0;
	/** 4.48 volleys a second (W). */
	public static final double VOLLEY_TIME = 1 / 4.48;
	public static final int AMMO = 24;                         // W: a volley each
	public static final double RELOAD = 1.25;                  // W
	public static final double PROJECTILE_SPEED = 60;          // W
	/** Gone after 40 m (W). */
	public static final double RANGE = 40;
	/** Spread: the near paws 0.3 degrees off the centre, the far ones 0.9 (W). */
	public static final double PAW_SPREAD_NEAR = 0.3;
	public static final double PAW_SPREAD_FAR = 0.9;
	/**
	 * Hit radius against enemies / allies: her volley graph's base (0.12 paw, 0.25 centre; doubled for allies) plus the
	 * bonus every hero's shared graph adds (0.075, 0.12): paws 0.195 / 0.315 m, the centre 0.37 / 0.62 m (the wiki's
	 * 0.19 / 0.31 / 0.37 / 0.62, rounded).
	 */
	public static final double PAW_RADIUS = 0.195;
	public static final double PAW_RADIUS_ALLY = 0.315;
	public static final double CENTRE_RADIUS = 0.37;
	public static final double CENTRE_RADIUS_ALLY = 0.62;

	// Frenetic Flight (secondary fire, held)
	/** Top speed (W: 17.8 m/s, "+230.1%"). */
	public static final double BOOST_SPEED = 17.8;
	/** Fuel: 40% a second burnt, 25% a second back after 0.75 s (BZ Feb 13 2026: 30 -> 25), a third of that carrying. */
	public static final double FUEL_BURN = 0.40;
	public static final double FUEL_REFILL = 0.25;
	public static final double FUEL_DELAY = 0.75;
	public static final double FUEL_CARRY_FACTOR = 1 / 3.0;    // BZ Jun 16 2026: 3x slower

	// Lifeline (ability 1): a toggle; an ally in reach is towed on a rope
	public static final double LIFELINE_REACH = 8.5;           // W: a cylinder 8.5 m in radius
	public static final double LIFELINE_HEIGHT = 15;           // W: and 15 m tall
	public static final double LIFELINE_ROPE = 4;              // W; her data's rope length, max(4, 0.2) (graph #1137)
	/** The rope snaps when she is this many rope lengths from the ally across (her data's #928: M, a horizontal offset). */
	public static final double LIFELINE_BREAK = 3;
	public static final double LIFELINE_COOLDOWN = 2;          // W
	public static final double LIFELINE_SPEED = 0.30;          // BZ Aug 11 2026: 40% -> 30%
	public static final double LIFELINE_HEAL = 25;             // W: a second
	/** Leaving with Jump launches the ally at 13.2 m/s (W): her data's 8 ahead and 10.5 up (graph 5A8F, read as M). */
	public static final double LIFELINE_LAUNCH = 13.2;
	public static final double LIFELINE_LAUNCH_FORWARD = 8;
	public static final double LIFELINE_LAUNCH_UP = 10.5;

	// Purr (ability 2)
	public static final double PURR_COOLDOWN = 12;             // W
	public static final double PURR_TIME = 4;                  // W
	public static final double PURR_RADIUS = 10;               // W
	public static final double PURR_DAMAGE = 30;               // W: on the cast
	/** The cast's knockback: 15 m/s out, 7 up (W: 16.6 m/s). */
	public static final double PURR_KNOCKBACK = 15;
	public static final double PURR_KNOCKBACK_UP = 7;
	/** Healing waves: pulses of 1, 2, 3 and 4 waves, about 0.95 s apart, 30 a wave to allies, 18 to herself (W). */
	public static final int[] PURR_PULSES = {1, 2, 3, 4};
	public static final double PURR_PULSE_GAP = 0.95;
	public static final double PURR_HEAL = 30;
	public static final double PURR_SELF_HEAL = 18;            // BZ Aug 14 2026: a 40% self-healing penalty

	// Catnapper (ultimate)
	public static final int ULT_COST = 2575;                   // W (BZ launch 2400 +7% = 2568: W used)
	public static final double ULT_RANGE = 25;                 // W
	public static final double ULT_DIVE_SPEED = 30;            // W
	/** The landing's reach: 5.5 m in her data (graph 5B7B; the wiki says 6). */
	public static final double ULT_RADIUS = 5.5;
	public static final double ULT_DAMAGE = 25;                // W
	public static final double ULT_KNOCKDOWN = 1.5;            // W
	public static final double ULT_PUSH = 2.8;                 // W
	/** She carries the nearest enemy up to 4 s, healing 30 a second, 30% slower with a tank (W). */
	public static final double ULT_CARRY = 4;
	public static final double ULT_SELF_HEAL = 30;
	public static final double ULT_TANK_SLOW = 0.30;

	// Perks: the XP her values graph holds (59DD#4: 2100, then 3100 more; the wiki 2100 / 5200)
	public static final double PERK_MINOR_XP = 2100;
	public static final double PERK_MAJOR_XP = 5200;
	public static final double ULTERIOR_MOTIVE = 0.15;         // BZ Mar 10 2026: healing's share recovered as fuel
	public static final double TRANSPORT_SHIELD = 50;          // BZ Jun 16 2026
	public static final double TRANSPORT_SHIELD_TIME = 3;      // BZ Jun 16 2026
	public static final int PURRFECT_WAVES = 3;                // BZ Aug 11 2026: each pulse 3 waves
	public static final double CLAWS_OUT_COOLDOWN = 6;         // BZ Mar 10 2026
	public static final double CLAWS_OUT_DAMAGE = 40;          // BZ: a wound over 1 s
	public static final double CLAWS_OUT_SLOW = 0.30;          // BZ: for 1 s
}
