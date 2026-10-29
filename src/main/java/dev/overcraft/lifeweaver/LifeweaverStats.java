package dev.overcraft.lifeweaver;

/**
 * Lifeweaver's numbers (tools/reference/lifeweaver.md has every quote and its date; the game data map is
 * E:\Overwatch Archive\research\lifeweaver\lifeweaver-map.md, build 154088, with its detail in work_kit\). Sources:
 * Blizzard patch notes (BZ) first, the Overwatch wiki (W) where the notes give no number, the game data (D, with its
 * value id or graph node) where it holds one; the data wins where it's more exact (lifeweaver-plan.md, "Where the data
 * differs"). Where none gave a value, it's ours and says so.
 */
public final class LifeweaverStats {
	private LifeweaverStats() {
	}

	/** Health, then shields on top: 250 in all. */
	public static final int HEALTH = 200;                      // BZ 2025-08-26 (225 -> 200); D 0B2A
	public static final int SHIELDS = 50;                      // BZ 2023-08-10; D 0B3C
	/** Medic sub-role: his weapon's healing heals him too (no ult charge for that part). */
	public static final double MEDIC_SELF_HEAL = 0.40;         // BZ 2026-03-10 (25% -> 40%)

	// ---- the weapons: Healing Blossom (left button) and Thorn Volley (right), swapped ----
	public static final double SWAP = 0.125;                   // BZ 2025-11-11 (0.25 -> 0.125); D 0015 (0.5 / (1 + 3))
	/** After a swap, the weapon fires no sooner than this (D 21C6: the draw delay). */
	public static final double DRAW_DELAY = 0.175;
	/** The weapon not in hand refills itself after this long. */
	public static final double HOLSTER_RELOAD = 2;             // BZ 2025-11-11 (2.5 -> 2); D 25DF

	// ---- Healing Blossom ----
	public static final int BLOSSOM_AMMO = 16;                 // BZ 2023-10-31 (20 -> 16); D 0B2D
	public static final double BLOSSOM_RELOAD = 1.75;          // W; D 1C55
	/** The ammo is back this far into the reload (D: 0.75 x 1.75 = 1.3125 s), the reload running on to 1.75 s. */
	public static final double BLOSSOM_RELOAD_AMMO = 0.75;
	/** Held, the charge fills 0 -> 1 in this long, linearly (D 1C54: M linear). */
	public static final double CHARGE_TIME = 1.15;             // BZ 2024-03-12 (1 -> 1.15)
	/** Not held (in hand or holstered), it fills 4.5 times slower: full in 5.175 s (W's 5.4 is a measurement). */
	public static final double CHARGE_PASSIVE = 4.5;           // D 1C5A
	/** The heal: this at no charge (below 10%), rising to HEAL_MAX at full. */
	public static final double HEAL_MIN = 10;                  // W; D 21C6
	public static final double HEAL_MAX = 90;                  // BZ 2026-04-14 (80 -> 90); D
	/** The charge's curve: 0 until 0.1, 1 at 0.9985 (D 21C6; its interpolation L: linear between). */
	public static final double CURVE_FROM = 0.1, CURVE_TO = 0.9985;
	/** The heal lands over this times the charge (instant at none). */
	public static final double HEAL_OVER = 0.3;                // D 1C58 (W "over 0.3 seconds")
	/** The blossom's speed: 30 + 30 x charge (m/s), 60 from 90% (D 2A3F / 2A3E; W 30 to 60). */
	public static final double BLOSSOM_SPEED = 30, BLOSSOM_SPEED_CHARGE = 30, BLOSSOM_SPEED_FULL_AT = 0.9;
	public static final double BLOSSOM_RADIUS = 0.25;          // W; D 2A32
	public static final double BLOSSOM_LIFETIME = 8;           // D 35FD
	/** After a throw, the next charge waits this long. */
	public static final double BLOSSOM_RECOVERY = 0.3;         // W; D 21C6
	/** The target: the ally nearest the crosshair within this angle (degrees), this range, held this long out of the cone. */
	public static final double TARGET_CONE = 20;               // D 21C6 (20 x sensitivity 1.0; half / full angle L: read as the angle from the aim, as Mercy's)
	public static final double TARGET_RANGE = 30;              // W; D 1C56
	public static final double TARGET_LINGER = 0.15;           // D 21C6
	/** The blossom flies straight, then homes on its target from this share of its straight flight time (D 369A: M). */
	public static final double HOMING_FROM = 0.5;

	// ---- Thorn Volley ----
	public static final double THORN_DAMAGE = 6;               // BZ 2023-06-13 (5 -> 6); D 0B2B
	public static final double VOLLEYS_PER_SECOND = 11;        // W; D 21C7 (a literal)
	public static final int THORN_AMMO = 100;                  // BZ 2024-09-17 (80 -> 100); D 0B2C
	/** Each volley (two thorns) takes this much ammo. */
	public static final int THORN_AMMO_PER_VOLLEY = 2;         // D 21C7 #94
	public static final double THORN_RELOAD = 1.5;             // W; D 1C71
	public static final double THORN_RELOAD_AMMO = 0.75;       // D (1.125 s of 1.5)
	public static final double THORN_SPEED = 100;              // BZ 2025-04-22 (80 -> 100); D 2A33
	/** Hit radius: 0.125 (BZ 2023-06-13; D 2A37) plus the 0.075 every fast projectile gets (BZ 2024-03-12; D 28A6). */
	public static final double THORN_RADIUS = 0.2;
	public static final double THORN_LIFETIME = 5;             // D 3642
	public static final double THORN_CRIT = 2;                 // W; D m_66D29548 = 2 (M)
	/** Spread (degrees from the aim): the first thorn 0 -> 0.9, the second 0.2 -> 1.2, over 10 volleys. */
	public static final double SPREAD_A_MAX = 0.9, SPREAD_B_MIN = 0.2, SPREAD_B_MAX = 1.2;   // D 1CF9 (1.2 x 0.75); W 1.2
	public static final double SPREAD_VOLLEYS = 10;            // D 1E53; BZ 2023-06-28 (20 thorns)
	/** The spread's volleys count back down this fast (a second) while not firing (D 21C7: M). */
	public static final double SPREAD_DECAY = 2;

	// ---- Petal Platform (Shift) ----
	public static final double PLATFORM_COOLDOWN = 12;         // W; D 1C9D
	/** The cooldown starts this long after the press (the cast state's end). */
	public static final double PLATFORM_COOLDOWN_FROM = 0.25;  // D 2219
	/** The throw leaves the left wrist this long after the press. */
	public static final double PLATFORM_WINDUP = 0.1;          // D 2219
	/** Thrown at 50 m/s (W says 40), with gravity 9.8. */
	public static final double PLATFORM_THROW_SPEED = 50;      // D 2219 (a literal; panel 2A3B)
	public static final double THROW_GRAVITY = 9.8;            // D 2219 (M)
	public static final double PLATFORM_THROW_RADIUS = 0.15;   // W; D 2A3D
	public static final double PLATFORM_THROW_LIFETIME = 20;   // D 450F
	/** Blocked where it lands (no room 2 m up), it's gone and the cooldown has this left (W: an 11 s refund). */
	public static final double PLATFORM_BLOCKED_COOLDOWN = 1;  // D 2219
	public static final double PLATFORM_CLEARANCE = 2;         // D 2219 (the cast 2 m up)
	/** Pressing again destroys it, from this long after the throw. */
	public static final double PLATFORM_DESTROY_FROM = 0.4;    // D 1F1A
	public static final double PLATFORM_HEALTH = 300;          // BZ 2024-08-20 (400 -> 300); D 1C79
	public static final double PLATFORM_LIFT_SPEED = 10;       // W; D 1C84 (m/s)
	public static final double PLATFORM_HEIGHT = 9;            // W; D 1C86
	/** Its time raised in all (counted only while raised, never refilled). */
	public static final double PLATFORM_RAISED_TIME = 12;      // BZ 2024-08-20 (10 -> 12); D 1C87
	/** No one on it this long: it comes back down, its time left raised to at least this. */
	/** The raised platform's warning ticks over its last this many seconds (368D #660: v215 <= 5). */
	public static final double PLATFORM_WARNING_FROM = 5;      // D 368D
	public static final double PLATFORM_EMPTY = 2;             // BZ 2024-08-20; D 368D
	/** Standing on it this long sets it off (0.05 s from 0.475 s after landing). */
	public static final double PLATFORM_TRIGGER = 0.15;        // D 368D #135
	public static final double PLATFORM_TRIGGER_LATE = 0.05, PLATFORM_TRIGGER_LATE_FROM = 0.475;
	/** The trigger's reach as it lands, shrinking to the late one between 0.175 and 0.475 s (D 368D). */
	public static final double PLATFORM_TRIGGER_RADIUS = 2, PLATFORM_TRIGGER_RADIUS_LATE = 0.75;   // BZ 2023-06-13
	public static final double PLATFORM_SHRINK_FROM = 0.175;
	/** Under a ceiling it stops this far below it, and doesn't rise at all with less room than LOW_CEILING. */
	public static final double PLATFORM_HEADROOM = 2.75, PLATFORM_LOW_CEILING = 3.35;   // D 368D
	/** Riders' jump is faster by this during the rise and just after (D 267A: L unit, read as m/s). */
	public static final double PLATFORM_JUMP_BONUS = 6;
	public static final double PLATFORM_JUMP_AFTER = 0.2;      // D 368D
	/**
	 * The platform's top: a disc 1.9 m round (its stacking check's cylinder, D 368D; its petals' hardpoints at 1.79 m:
	 * M-L), 0.32 m thick (its rest mesh). The collision shape itself isn't in the data: ours, from those.
	 */
	public static final double PLATFORM_RADIUS = 1.9, PLATFORM_THICKNESS = 0.32;
	/** Petal Protection (minor perk): allies within 3 m of the platform heal 20 a second (BZ "standing on"; the data's 3 m). */
	public static final double PETAL_PROTECTION = 20;          // BZ 2026-02-10; D 28B6
	public static final double PETAL_PROTECTION_RANGE = 3;     // D 28B6
	/** Life Grip and Petal Platform share a lock: Grip sets it this long, the platform this (D: M). */
	public static final double GRIP_LOCK = 1.05, PLATFORM_LOCK = 0.75;

	// ---- Rejuvenating Dash (Jump in the air) ----
	public static final double DASH_COOLDOWN = 5;              // W; D 1CA4 (from its end)
	public static final double DASH_TIME = 0.25;               // D 1CA3 (W's 0.3 s doesn't match its own distances)
	/** The walk's 5.5 m/s times 5.25: 28.875 m/s along the keys (forward with none), level, gravity off. */
	public static final double DASH_SPEED = 5.5 * 5.25;        // D 1CA5; W's +425%
	public static final double DASH_HEAL = 55;                 // BZ 2026-04-14 (45 -> 55); D 1CA2 (over the dash)
	/** Usable from this long after leaving the ground. */
	public static final double DASH_AIR_DELAY = 0.1;           // D 36A2
	/** At its end the speed is halved (D 36A2: an x0.5 op, L). */
	public static final double DASH_END = 0.5;
	/** Dashing Escape (minor perk): the dash's speed x1.3 (distance +30%). */
	public static final double DASHING_ESCAPE = 1.3;           // BZ 2026-02-10; D 2874

	// ---- Life Grip (E) ----
	public static final double GRIP_COOLDOWN = 18;             // BZ 2024-11-12 (19 -> 18); D 1CA8 (from the cast)
	public static final double GRIP_RANGE = 30;                // W; D 1C56
	public static final double GRIP_CONE = 20;                 // D 21EF (20 x sensitivity 1.0)
	public static final double GRIP_CAST = 0.3;                // D 21EF
	public static final double GRIP_HEAL = 75;                 // BZ 2024-11-12 (50 -> 75); D 1CA7 (at once)
	/** The pull starts this long after the cast (the vine travelling to the ally). */
	public static final double GRIP_PULL_FROM = 0.35;          // D 21EF
	/** The pull: 25 m/s from within 15 m, else 12 m/s for 0.4 s, then 12 -> 35 over 0.6 s, then 35. */
	public static final double GRIP_SPEED = 25;                // BZ 2026-08-11 (30 -> 25); D 1CBB
	public static final double GRIP_NEAR = 15;                 // BZ 2026-08-11; D 21EE
	public static final double GRIP_FAR_SPEED = 12, GRIP_FAR_SLOW = 0.4, GRIP_FAR_RAMP = 0.6, GRIP_FAR_TOP = 35;   // D 21EE
	/** The pulled ally's Jump ends it from this long after the cast. */
	public static final double GRIP_JUMP_CANCEL = 0.65;        // D 21EE (BZ 2026-08-11: 0.6 s)
	public static final double GRIP_MAX = 2.5;                 // D 21EE
	/** Within this of him this long, it ends. */
	public static final double GRIP_CLOSE = 3, GRIP_CLOSE_TIME = 0.4;   // D 21EE
	/** The ally's own abilities are locked this long. */
	public static final double GRIP_ABILITY_LOCK = 0.75;       // BZ 2023-08-10; D 21EE
	/** The destination: from his feet 0.95 m up, then ahead min(distance, 1.1) m, 0.15 m right, 0.225 m and 0.25 m up. */
	public static final double GRIP_DEST_UP = 0.95 + 0.225 + 0.25, GRIP_DEST_AHEAD = 1.1, GRIP_DEST_RIGHT = 0.15;   // D 21EF
	/** Arriving within this of the destination (ours: the data's MoveTo arrival distance isn't read). */
	public static final double GRIP_ARRIVE = 0.3;

	// ---- Tree of Life (Q) ----
	public static final int ULT_COST = 2400;                   // W (2200 + BZ 2026-06-16's 9%); D 0AEC
	public static final double TREE_RANGE = 30;                // W; D 36A4
	public static final double TREE_CAST = 0.99;               // D 36A4 (anim 0x8EE)
	/** The tree spawns (and the charge goes) this far into the cast. */
	public static final double TREE_SPAWN_AT = 0.15;           // D 36A4
	/** Life Grip's vine grows at this (m/s): its sound of landing on the ally then (3110 on 21EE#316). */
	public static final double GRIP_VINE_SPEED = 88;            // D 3110 (tools/reference/lifeweaver-effects.md)
	/** His quick melee's foley, this far into the swing (s): the clips' sound keys. */
	public static final double MELEE_SOUND_AT = 0.6;            // D 1P C775 / C5B7, 3P D5A5 / D5A3 / D5A4
	public static final double TREE_TIME = 15;                 // W (BZ 2025-03-18 via W); D 1CC1
	/** Q destroys it from this long after it spawns. */
	public static final double TREE_DESTROY_FROM = 0.5;        // D 36A4
	public static final double TREE_HEALTH = 1200;             // BZ 2023-04-25 (1000 -> 1200); D 1CBC
	public static final double TREE_SPROUT = 150;              // W; D 1CBD
	public static final double TREE_PULSE = 90;                // BZ 2024-04-16 (75 -> 90); D 1CBE
	public static final double TREE_PULSE_EVERY = 1.75;        // W; D 1CF4
	public static final int TREE_PULSES = 8;                   // W; D 1CF4 (1.75 ... 14.0 s)
	/** Each pulse's heal lands over this long. */
	public static final double TREE_PULSE_OVER = 0.15;         // D 5A52
	public static final double TREE_RADIUS = 12;               // W; D 1CBF
	/** Its sight test runs from the trunk this high. */
	public static final double TREE_EYE = 1.8;                 // D 5A52
	/** Half of what it overheals becomes overhealth, up to 100, kept 6 s after its last pulse, then 20 a second away. */
	public static final double TREE_OVERHEAL = 0.5;            // BZ 2026-06-16 (100% -> 50%); D 1CC0
	public static final double TREE_OVERHEALTH_MAX = 100;      // BZ 2023-08-10; D 1CEE
	public static final double TREE_OVERHEALTH_HOLD = 6;       // BZ 2024-11-12 (2.5 -> 6); D 1D4F
	public static final double TREE_OVERHEALTH_DECAY = 20;     // D 5A52 (M)
	/** Its collision: a trunk 1 m round, 6.5 m tall from 0.5 m under its root; a canopy 3 m round, 3 m tall at +4.5 m. */
	public static final double TRUNK_RADIUS = 1, TRUNK_HEIGHT = 6.5, TRUNK_BELOW = 0.5;   // D 5A52 (H values, M shapes)
	public static final double CANOPY_RADIUS = 3, CANOPY_HEIGHT = 3, CANOPY_AT = 4.5;

	// ---- Perks ----
	public static final double PERK_MINOR_XP = 1975;           // W; D 12BD
	public static final double PERK_MAJOR_XP = 4940;           // W; D 12BE (1975 + 2965)
	/** Superbloom: 16 thorns in one enemy within 2.5 s burst for 40. */
	public static final int SUPERBLOOM_THORNS = 16;            // BZ 2025-05-20 (20 -> 16); D 25A5
	public static final double SUPERBLOOM_TIME = 2.5;          // BZ 2025-03-18 (1.5 -> 2.5); D 25A5
	public static final double SUPERBLOOM_DAMAGE = 40;         // BZ 2025-03-18 (30 -> 40); D 25A5
	/** Sow the Seed: quick melee with the Blossom out throws a seed (11 s); an ally within 3 m of it takes it. */
	public static final double SEED_COOLDOWN = 11;             // BZ 2026-04-14 (14 -> 11); D 287A
	public static final double SEED_SPEED = 25;                // D 287A
	public static final double SEED_THROW_AT = 0.1;            // D 287A (of a 0.5 s state)
	public static final double SEED_TIME = 14;                 // D 5C5A (on the ground)
	public static final double SEED_PICKUP = 3;                // D 5C5A (W 5 m)
	/** Picked up no sooner than this after it lands. */
	public static final double SEED_PICKUP_FROM = 0.3;         // D 5C5A
	public static final double SEED_OVERHEALTH = 75;           // BZ 2026-02-10; D 5C5C
	public static final double SEED_SPEED_BONUS = 0.25;        // BZ 2026-02-10; D 5C5C
	public static final double SEED_BUFF_TIME = 5;             // BZ 2026-02-10; D 5C5C
	/** After the 5 s, the seed's overhealth goes 25 a second. */
	public static final double SEED_OVERHEALTH_DECAY = 25;     // D 5C5C (M)

	/** Healing Blossom's heal at this charge (0-1): 10 below 10%, rising to 90 at full (linear between: L). */
	public static double heal(double charge) {
		double c = Math.clamp((charge - CURVE_FROM) / (CURVE_TO - CURVE_FROM), 0, 1);
		return HEAL_MIN + (HEAL_MAX - HEAL_MIN) * c;
	}

	/** Healing Blossom's speed at this charge (m/s). */
	public static double blossomSpeed(double charge) {
		return charge >= BLOSSOM_SPEED_FULL_AT ? BLOSSOM_SPEED + BLOSSOM_SPEED_CHARGE : BLOSSOM_SPEED + BLOSSOM_SPEED_CHARGE * charge;
	}
}
