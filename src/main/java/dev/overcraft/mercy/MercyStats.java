package dev.overcraft.mercy;

import dev.overcraft.Ow;

/**
 * Mercy's numbers (tools/reference/mercy.md has every quote and its date; the game data map is
 * E:\Overwatch Archive\research\mercy\mercy-map.md, build 154088). Sources: Blizzard patch notes (BZ) first, the
 * Overwatch wiki (W) where the notes give no number, the game data (D, with its value id) where it holds one. Where none
 * gave a value, it's ours and says so. Where the data disagrees with the notes, both are given.
 */
public final class MercyStats {
	private MercyStats() {
	}

	public static final int HEALTH = 225;                      // BZ 2024-08-20 (250 -> 225); W; D 0179
	public static final double MELEE = 40;                     // D 02D0 (every hero's quick melee)

	// ---- Caduceus Staff: one beam, two modes (left button heals, right boosts: Overwatch's own layout) ----
	public static final double HEAL_PER_SECOND = 55;           // BZ 2026-04-14 (60 -> 55); D 032A
	public static final double HEAL_PER_SECOND_VALKYRIE = 65;  // W; D 032C (BZ's last number, 60 in 2018, is stale)
	/** The damage boost: what the target deals is raised this much. */
	public static final double BOOST = 0.25;                   // BZ 2026-06-16 (30% -> 25%); D 036A
	public static final double BEAM_RANGE = 15;                // W; D 1034
	public static final double BEAM_RANGE_VALKYRIE = 30;       // W "+100%"; D 1036 = 1.0 (M: a +100% range)
	/**
	 * The lock picks within this angle of the crosshair (degrees): D 006B, 40 x Beam Sensitivity (default 1.0). The data
	 * doesn't say whole cone or half; the wiki calls the sensitivity "the maximum view angle for acquiring a target", the
	 * angle between the aim and the target (M: read as that, for all three of her cones).
	 */
	public static final double BEAM_CONE = 40;
	/** How often the beam looks for a target while it has none (a second: D, M meaning). */
	public static final double BEAM_SEARCH_PER_SECOND = 12;
	/**
	 * Out of sight or out of range, the beam holds on this long, then breaks; back in sight resets it. D 006B #30: its
	 * lock tracker's m_93AB8C13 = 1EA7 = 1.0 (M meaning: Lúcio's aura uses that field for the wiki's 1 s). The wiki's 1.3
	 * is not it: 1BE6 = 1.3 is Valkyrie's Guardian Angel launch factor (kit_details.md 4a).
	 */
	public static final double BEAM_LINGER = 1.0;
	/** Valkyrie: the beam branches from its target to every ally this close to it, in its sight. */
	public static final double CHAIN_RADIUS = 10;              // W; D 1035 (M)
	/** Medic sub-role: her beam's healing heals her too (no ult charge for that part). */
	public static final double MEDIC_SELF_HEAL = 0.40;         // BZ 2026-03-10 (25% -> 40%)

	// ---- Caduceus Blaster ----
	public static final double BLASTER_DAMAGE = 20;            // W; D 0338
	public static final double BLASTER_RATE = 5;               // shots a second: W; D 033E
	public static final int BLASTER_AMMO = 25;                 // BZ 2022-12-06 (20 -> 25); D 033F
	public static final double BLASTER_RELOAD = 1.4;           // W; D 033D
	/** The ammo comes back this far into the reload (0.91 s), the reload running on to 1.4 s. D 006D #186: v6884. */
	public static final double BLASTER_RELOAD_AMMO = 0.65;
	public static final double BLASTER_SPEED = 50;             // m/s: W; D 033A
	public static final double BLASTER_SPEED_VALKYRIE = 100;   // W; D 033B (+100%)
	/** Hit radius: 0.2 (BZ 2024-02-21; D 2A40) plus the 0.075 every projectile gets (BZ 2024-03-12). */
	public static final double BLASTER_RADIUS = 0.275;
	public static final double BLASTER_CRIT = 2;               // W (Critical hit); D 0340 = 1.0 is the default x2 (M)
	/** Put away, the blaster reloads itself after this long. */
	public static final double BLASTER_HOLSTER_RELOAD = 1.4;   // W; D (v67157 chases to 1.4: M)
	/** Changing weapons (s). */
	public static final double SWAP = 0.35;                    // BZ 2022-12-06 (0.5 -> 0.35; W says 0.51)

	// ---- Guardian Angel (Shift): her graph 006E, read node by node in kit_details.md (sections 1-3) ----
	public static final double GA_RANGE = 30;                  // W; D 1BE0
	public static final double GA_RANGE_VALKYRIE = 60;         // W; D 1BE1
	/**
	 * The pick: the ally (or soul) nearest the crosshair within this angle of it, in sight (D 006E #815: 30 x Guardian
	 * Angel Sensitivity, default 1.0; the angle from the aim, M: see BEAM_CONE).
	 */
	public static final double GA_CONE = 30;
	/** She won't start toward a target this close (D 006E #814: distance > 4). */
	public static final double GA_MIN = 4;
	/** Her beam's target is taken first, with no cone or sight test, out to her range plus this (D #812 / #814). */
	public static final double GA_BEAM_REACH = 10;
	public static final double GA_SPEED = 17;                  // m/s: W; D 1B0E
	public static final double GA_SPEED_VALKYRIE = 25;         // W; D 1B0F
	/**
	 * Where she flies, fixed when the flight starts (D #249 / #253 / #254; the ally isn't followed, H): the ally's
	 * position (its feet, M) 0.5 m up, pulled back toward her horizontally by 3.7 m less half the height between them, and
	 * raised by a tenth of the drop when the ally is below her.
	 */
	public static final double GA_ABOVE = 0.5;
	public static final double GA_SHORT = 3.7;
	public static final double GA_SHORT_PER_HEIGHT = 0.5;
	public static final double GA_RAISE = 0.1;
	/** Within this of that point she has arrived (D StateMoveTo #241 m_86EA271E = 1: the field's meaning M). */
	public static final double GA_ARRIVE = 1;
	/** Ours: the data has no time limit (H); in Minecraft a wall can stop her short, so a flight gives up after this. */
	public static final double GA_LIMIT = 4;
	/**
	 * After she arrives, Jump or Crouch still launches for this long. Her data's is 0.35 s (#939); ours 0.6 s, more
	 * forgiving (the owner, #57: "the timings for guardian angel ... should be more forgiving").
	 */
	public static final double GA_WINDOW = 0.6;
	/**
	 * The launches (D #1027): (11 + 6 m) m/s, m the momentum 0-1 (1BE3 11, 1BE4 6), x1.3 in Valkyrie (1BE6), x(1 + 0.2 m)
	 * for Crouch's straight up, x(1 + 0.2 m |pitch| / 89) for Jump's (1BE5 0.2). Checks: a full-meter jump 17, crouch 20.4
	 * (W), Valkyrie's crouch 26.52 (W 26.5). A further x1.15 under a condition the data doesn't name (26EF.134) is left out.
	 */
	public static final double GA_LAUNCH_MIN = 11;
	public static final double GA_LAUNCH_MOMENTUM = 6;
	public static final double GA_LAUNCH_VALKYRIE = 1.3;
	public static final double GA_LAUNCH_PITCH = 0.2;
	/** Jump's direction: her look (with its pitch) plus this much up, + 0.1 x momentum, normalized (D #976). */
	public static final double GA_LAUNCH_UP = 0.5;
	public static final double GA_LAUNCH_UP_MOMENTUM = 0.1;
	/** Back held: the look reversed (D #975: s = -1), and this much slower (D 2CE3; BZ 2023-02-07 "20% slower"). */
	public static final double GA_LAUNCH_BACK = 0.8;
	/**
	 * The meter fills over this much flight, 17/25 of it in Valkyrie. Her data's is 1 s (#912 m_5BEFF040, M a
	 * duration); ours 0.6 s, so a launch partway through a short flight is near full ("a more precise space gives more
	 * of a boost": the owner, #57).
	 */
	public static final double GA_MOMENTUM_FILL = 0.6;
	/**
	 * After a launch, this long before the cooldown starts (it can't be cut short). Through it her air damping is off
	 * (D 006E #487: property F808ACE8 set 0, read as air speed damping, M): the launch carries undiminished.
	 */
	public static final double GA_LAUNCH_STATE = 1;            // BZ 2024-02-13 (1.5 -> 1); D 1C20
	public static final double GA_LAUNCH_STATE_VALKYRIE = 0.5; // D 1C21
	/**
	 * In Valkyrie's 0.5 s after a launch her speed gets +1.4444 (D 006E #477, an add on the movement speed: 5.5 x 1.636 x
	 * 2.4444 = 22 m/s, gaps2.md 3) and her knockback x0.2 on top of Valkyrie's -50%.
	 */
	public static final double GA_LAUNCH_SPEED_VALKYRIE = 1.4444;
	public static final double GA_LAUNCH_KNOCKBACK_VALKYRIE = 0.2;
	public static final double GA_COOLDOWN = 1.5;              // BZ 2023-04-11 (2.5 -> 1.5); D 0334

	// ---- Angelic Descent (Jump held in the air) ----
	public static final double DESCENT_SPEED = 2;              // m/s: W; D 1B18
	/** After this long falling slowly she spreads her wings (the flight blend). */
	public static final double DESCENT_POSE = 0.2;             // D (0071: the flight anim after 0.2 s)

	// ---- Resurrect (E) ----
	public static final double RES_RANGE = 5;                  // BZ 2017-09-19; D 123D (W says 5.5)
	/** Her soul tracker picks within this angle of the crosshair (D 00CA #1028 / #1073 / #1153; the angle from the aim, M). */
	public static final double RES_CONE = 60;
	public static final double RES_CAST = 1.75;                // BZ 2017-11-16; W; D 0335
	/** While casting she moves this much slower. */
	public static final double RES_SLOW = 0.75;                // BZ 2017-11-16; D 1BEC (M)
	public static final double RES_COOLDOWN = 30;              // W; D 0337
	/** Further from the soul than this, the cast breaks (the cooldown still starts). */
	public static final double RES_BREAK = 7;                  // W; D 00CA #581: 5 + 2 (H; 13EF = 7 has no reader)
	/** The ally comes back unhurtable this long; they can't move for the first 1.5 s of it. */
	public static final double RES_INVULNERABLE = 2.25;        // W; D 1BE7
	public static final double RES_ROOTED = 1.5;               // W
	/**
	 * A dead ally's soul can be raised for this long. Ours: Overwatch's soul lasts until the player respawns (its
	 * respawn wait), and Minecraft's death screen waits for ever.
	 */
	public static final double SOUL_TIME = 10;

	// ---- Flash Heal (R: Overwatch's Reload button) ----
	public static final double FLASH_HEAL = 60;                // BZ 2026-04-14 (50 -> 60); D 24B7
	/** Doubled on a target below this share of its health. */
	public static final double FLASH_LOW = 0.5;                // BZ 2026-04-14 (x3 -> x2); D #570
	public static final double FLASH_LOW_FACTOR = 2;
	public static final double FLASH_COOLDOWN = 12;            // BZ 2026-04-14 (15 -> 12); D 24B6
	/** The cast (she can't fire through it). BZ 2025-03-18 0.4 (0.875 -> 0.4); the data's 0.5 (D 24B8). */
	public static final double FLASH_CAST = 0.4;

	// ---- Valkyrie (Q) ----
	public static final int ULT_COST = 2150;                   // W (2000 + BZ 2026-06-16's 7%); not in her data
	public static final double VALKYRIE_CAST = 0.5;            // D 10DA (W 0.496)
	public static final double VALKYRIE_TIME = 15;             // W; D 0341
	/** Her flight in Valkyrie, every way (her walk 5.5 x 1.636). */
	public static final double VALKYRIE_SPEED = 9;             // W; D 1B14 (x1.636)
	public static final double VALKYRIE_AIR_ACCEL = 8;         // D 10DA#309
	/**
	 * With no input she drifts, slowing this much a second (D 10DA #309's mover: air drag m_8895A9C8 0.35, every hero's
	 * default; the field Jetpack Cat's flight sets to 8). Above her speed it decays at 2/s past 5.5 x 1.636 (the same).
	 */
	public static final double VALKYRIE_DRAG = 0.35;
	/** Knockback she takes in Valkyrie. */
	public static final double VALKYRIE_KNOCKBACK = 0.5;       // W (-50%); D 0A04B51B -0.5 (M)

	// ---- Swift Recovery (passive): regeneration starts sooner ----
	public static final int REGEN_DELAY = Ow.ticks(3);         // W (6 -> 3 s); D 125A (-50%)

	// ---- Perks ----
	public static final double PERK_MINOR_XP = 1835;           // W; D 1294
	public static final double PERK_MAJOR_XP = 4590;           // W; D 1295 (1835 + 2755)
	/** Angelic Resurrection: overhealth after casting Resurrect, held 2 s, then 5 a second away (W). */
	public static final double ANGELIC_RESURRECTION = 100;     // BZ 2025-04-22; D 0C7F
	public static final double ANGELIC_RESURRECTION_TIME = 2 + 100 / 5.0;
	/** Winged Reach: Guardian Angel's range +30%. */
	public static final double WINGED_REACH = 0.3;             // BZ 2026-05-12; D 5161
	/** Chain Boost: the boost +5%, and it links to a second ally this close to the target (the range is ours). */
	public static final double CHAIN_BOOST = 0.05;             // BZ 2026-06-16; D 0B24
	public static final double CHAIN_BOOST_RANGE = 10;
	/** Double Dose: Flash Heal heals this much less, and has two charges. */
	public static final double DOUBLE_DOSE_LESS = 10;          // BZ 2026-04-14
}
