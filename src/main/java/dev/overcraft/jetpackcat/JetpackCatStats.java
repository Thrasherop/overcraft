package dev.overcraft.jetpackcat;

/**
 * Jetpack Cat's numbers in Overwatch units (metres, seconds, Overwatch health), live game as of the Nov 3 2026 patch
 * (tools/reference/jetpack_cat.md has every quote). Sources: Blizzard patch notes (BZ) first, the Overwatch wiki (W)
 * where the notes give no number, the game data where it holds one. Where none gave a value, it's ours and says so.
 */
public final class JetpackCatStats {
	private JetpackCatStats() {
	}

	// Base
	public static final int HEALTH = 200;                      // BZ Nov 3 2026 (225 -> 200); W rev 408976
	/** Knockback taken while flying: -40% ("flight resistance", W). */
	public static final double KNOCKBACK_TAKEN = 0.6;

	// Jetpack (passive): permanent flight (game data: her flight mover, graph 59DC; tools/reference/movement.md)
	/** Every way, backing off too (R: the owner's Workshop recording 2026-11-01, 5.50 forward, back, strafe, up, down). */
	public static final double FLY_SPEED = 5.5;                // BZ Aug 14 2026 (back from 6)
	/**
	 * Speeding up toward the wish, times the wish speed, a second: 0 to 5.5 m/s in 0.13 s, steps of 1.37 every 0.032 s (R:
	 * 42.7 m/s/s = 7.75 x 5.5; BZ Aug 11 2026 reads "7.7 -> 9.5" and the data holds 9.5, but the live game measures 7.75).
	 */
	public static final double FLY_ACCELERATE = 7.75;
	/**
	 * Slowing, times the wish speed, a second, in a straight line: with no input to a stop (5.5 to 0 in 0.56 s: 9.6 m/s/s,
	 * R, three stops), and with input down to the wish speed (12 to 5.5 in 0.67 s); under Lifeline's +30% both 30% more
	 * (R: 12.3 and 12.9 m/s/s). The data's "drag 8" read as an exponential stopped her in one tick (found against the
	 * recording).
	 */
	public static final double FLY_SLOW = 1.745;
	/** Above this, with input, her speed is cut to it at once, once a boost has been over 0.3 s (R: 14.59 -> 12.01). */
	public static final double FLY_MAX = 12;
	public static final double FLY_MAX_AFTER_BOOST = 0.3;

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
	/** Top speed (W: 17.8 m/s, "+230.1%"; R: 17.80 held in three boosts). */
	public static final double BOOST_SPEED = 17.8;
	/**
	 * Its speed-up (R, three boosts from 5.5-7 m/s, 2026-11-01): about 20 m/s/s for its first 0.2 s, then 10.4 m/s/s, easing into
	 * the top speed (the gap shrinking about 7 a second: 16.85, 17.36, 17.60, 17.71 every 0.1 s): 17.8 by about 1.4 s. The
	 * recording is of the old push: BZ Nov 3 2026 "Acceleration increased from 25 to 28 meters per second" (D 59E0 #63's 25 is the
	 * old value). Ours (M, inferred, no new recording): both rates x 28/25, the top speed and its easing left alone (the notes
	 * touch neither). A new recording should replace these.
	 */
	public static final double BOOST_KICK = 20 * 28 / 25.0, BOOST_KICK_TIME = 0.2, BOOST_RAMP = 10.4 * 28 / 25.0, BOOST_EASE = 7;
	/** How fast a boost's direction follows the input, a second (ours: the recording has no turns). */
	public static final double BOOST_TURN = 6;
	/**
	 * Fuel: 40% a second burnt, 20% a second back after 0.75 s (BZ Nov 3 2026: 25 -> 20; W rev 408976), a third of that carrying
	 * (BZ Jun 16 2026: 3x slower; W's infobox still says 8%, a third of the old 25: not updated).
	 */
	public static final double FUEL_BURN = 0.40;
	public static final double FUEL_REFILL = 0.20;
	public static final double FUEL_DELAY = 0.75;
	public static final double FUEL_CARRY_FACTOR = 1 / 3.0;    // BZ Jun 16 2026: 3x slower

	// Lifeline (ability 1): a toggle; an ally in reach is towed on a rope
	public static final double LIFELINE_REACH = 8.5;           // W: a cylinder 8.5 m in radius; D 5A8F #1215 (v11861)
	/**
	 * The cylinder's span on her: from this far below her feet to LIFELINE_HEIGHT above that (1.25 m above her), sight
	 * checked from this high on her (D 5A8F #1215: v1165 15 m, offset (0, -13.75, 0), sight from her root + 0.75; read
	 * as the cylinder's base, M). research\jetpack_cat\assets2.md 1.
	 */
	public static final double LIFELINE_BELOW = 13.75;
	public static final double LIFELINE_SIGHT = 0.75;
	/** An ally let go isn't taken again for this long (s): D v74583 -> v52054. */
	public static final double LIFELINE_LOCKOUT = 2;
	/** She can switch it off from this long after switching it on, and not within LIFELINE_HOLD of a catch (D #1057). */
	public static final double LIFELINE_OFF_AFTER = 0.6;
	public static final double LIFELINE_HOLD = 0.5;
	/** The rope's distance break waits this long after a catch (D). */
	public static final double LIFELINE_BREAK_GRACE = 0.2;
	public static final double LIFELINE_HEIGHT = 15;           // W: and 15 m tall
	public static final double LIFELINE_ROPE = 4;              // W; her data's rope length, max(4, 0.2) (graph #1137)
	/**
	 * The rope's force is ours, shaped to look like a rope: D 5A8F #1137 (and 5B7B #391) holds numbers, none decoded (gaps.md:
	 * "stiffness: L", the 12 under "Still unknown"). 30, 2, 20, 10 are its only STUConfigVarInt values, so counts (segments,
	 * iterations) are as live a reading as force ones. No recording has an ally on the rope.
	 * The pull, m/s² per metre past its length: 30 is m_81D6C3A4's value, its meaning L. An ally hangs about 0.56 m past the 4 m.
	 */
	public static final double ROPE_STIFFNESS = 30;
	/**
	 * Its damping along itself, a second. Ours: m_725EB364's 2, read as this (L), bounced 4.2-5.1 m at every change of height and
	 * threw a catch about; 5 (a damping ratio of about 0.46) keeps a little stretch and no bungee. A design choice until a recording.
	 */
	public static final double ROPE_DAMPING = 5;
	/** The most it reels an ally in at, m/s, closing on her: m_6211A3C1's value, its meaning L (ours). */
	public static final double ROPE_REEL_SPEED = 10;
	/** And it closes at no more than this many m/s per metre still to go, a second: ours, so a catch arrives and doesn't throw them. */
	public static final double ROPE_ARRIVE = 3;
	/** Her speed, as far as the rope takes it (m/s): 17.8 x 1.3, her measured top speed under Lifeline's bonus. Past it she has blinked. */
	public static final double ROPE_HER_TOP = 23.2;
	/**
	 * How fast a swing dies, a second, on what's across the rope. Ours: nothing in the data reads as this, and with no damping
	 * at all a mover keeps a swing under 5.5 m/s going for ever. 0.5 halves it in about 3 s, a swing or so.
	 */
	public static final double ROPE_SWING_DAMPING = 0.5;
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
	/** A towed player can let go from this far into the tow (5A8F; was inline in JetpackCat.lifeline). */
	public static final double LIFELINE_DETACH_AFTER = 0.6;
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
