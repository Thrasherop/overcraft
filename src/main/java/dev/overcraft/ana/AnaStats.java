package dev.overcraft.ana;

/**
 * Ana's numbers (E:\Overwatch Archive\research\ana\ana.md has every quote and its date; the game data map is ana-map.md,
 * build 154088 of 2026-10-06, with its detail in work_kit\). Sources: the game data (D, with its value id or graph node),
 * Blizzard patch notes (BZ), the Overwatch wiki (W). Where they disagree the data is used, as the live build's own code
 * (ana-plan.md, "Where the data differs"), and the line says so; where none gives a value, it's ours and says so.
 */
public final class AnaStats {
	private AnaStats() {
	}

	public static final int HEALTH = 250;                      // BZ 2024-02-13 (200 -> 250); D 4BE4 #3 (0BA7)

	// ---- Biotic Rifle (primary fire) ----
	public static final double RIFLE_DAMAGE = 75;              // BZ 2024-11-12 (70 -> 75); D 1037
	public static final double RIFLE_HEAL = 75;                // BZ 2024-11-12 (70 -> 75); D 1038
	/** Both land over this long, not at once (D 0B95 #193 / #205, M: the change's duration; W 0.592 s in 5 ticks). */
	public static final double RIFLE_OVER = 0.5;
	public static final int RIFLE_AMMO = 15;                   // W; D 106E
	public static final double RIFLE_RELOAD = 1.5;             // W; D 102D
	/** The clip is full this far into the reload (D 0B94 #72: 0.4 x 1.5 = 0.6 s), firing locked to its end. */
	public static final double RIFLE_RELOAD_AMMO = 0.4;
	/** Between shots (D 0B94 #390), holding the button refires. */
	public static final double RIFLE_RECOVERY = 0.8;           // W; D 0B94
	/** Unscoped: a straight projectile (no gravity), gone after 5 s (D projectile 15B7). */
	public static final double DART_SPEED = 125;               // W; D 2934
	public static final double DART_LIFETIME = 5;              // D 15B7
	/** Its size against enemies and against allies (D 2933 + 01C7 28A5, 2935; W says 0.05 / 0.15: the data used). */
	public static final double DART_RADIUS_ENEMY = 0.15, DART_RADIUS_ALLY = 0.3;

	// ---- Take Aim (secondary fire, held) ----
	public static final double SCOPE_IN = 0.25;                // D 0B94 #80 (W 0.24)
	public static final double SCOPE_OUT = 1 / 6.0;            // D 0B94 #79 (W 0.16)
	/** Scoped: hitscan, this size against enemies and allies. */
	public static final double SCOPED_RADIUS_ENEMY = 0.04;     // BZ 2026-10-06 (0.07 -> 0.04); D 28A2
	public static final double SCOPED_RADIUS_ALLY = 0.3;       // D 2935; W
	/** Its reach (D m_FEC435C6 = 100: L, read as metres). */
	public static final double SCOPED_RANGE = 100;
	/** Fully scoped she moves 65% slower (D 1E61 into the shared slow channel: M; W 1.925 m/s). */
	public static final double SCOPED_SLOW = 0.65;
	/** Aim sensitivity while zoomed, the option's default. */
	public static final double SCOPED_SENSITIVITY = 0.3;       // D v8174; W
	/** The zoomed field of view, degrees (D v172 = 30: L, unit unread; ours to check against the game by eye). */
	public static final double SCOPED_FOV = 30;

	// ---- Sleep Dart (ability 1) ----
	public static final double SLEEP_COOLDOWN = 14;            // BZ 2022-12-06 (15 -> 14); D 0F77
	/** The cast: the pistol drawn, the dart leaving at 0.3 s, the cooldown starting at its end (1.0 s). */
	public static final double SLEEP_CAST = 1.0, SLEEP_FIRE_AT = 0.3;   // D 0BDD #107 / #109 / #87
	public static final double SLEEP_PISTOL_GONE = 0.7;        // D 0BDD #105 (out at 0) / #116 (gone)
	public static final double SLEEP_DAMAGE = 5;               // W; D 0F22
	public static final double SLEEP_DART_SPEED = 60;          // W; D 0F0E
	public static final double SLEEP_DART_RADIUS = 0.2;        // W; D 0F0D
	public static final double SLEEP_DART_LIFETIME = 20;       // D 15E8
	public static final double SLEEP_TIME = 5;                 // W; D 0F10
	/** On tanks: D 3.5 s (1245, and the in-game panel) against BZ 2024-07-09's 3 s; the data used (ana.md, Disagreements). */
	public static final double SLEEP_TIME_TANK = 3.5;
	/** Damage wakes a sleeper only after this long asleep (damage before is ignored), and waking takes this long. */
	public static final double SLEEP_WAKE_FROM = 0.5;          // W; D 1D80
	public static final double SLEEP_WAKING = 0.5;             // W; D 1D7F

	// ---- Biotic Grenade (ability 2) ----
	public static final double GRENADE_COOLDOWN = 12;          // BZ 2026-04-14 (14 -> 12); D 1246
	/** The throw: a 0.7 s state, the grenade leaving about 2 ticks in (D 0B97 #41 / #160: M), the cooldown from its end. */
	public static final double GRENADE_CAST = 0.7, GRENADE_THROW_AT = 0.1;
	public static final double GRENADE_DAMAGE = 90;            // BZ 2026-07-14 (75 -> 90); D 0F25
	public static final double GRENADE_HEAL = 90;              // BZ 2026-07-14 (75 -> 90); D 0F25
	public static final double GRENADE_RADIUS = 4;             // W; D 119B
	/** Allies: +50% healing received; enemies: none at all, both for 3 s. */
	public static final double GRENADE_BOOST = 0.5;            // W; D 1DE6
	public static final double GRENADE_EFFECT_TIME = 3;        // BZ 2024-01-09 (3.5 -> 3); D 0F12
	public static final double GRENADE_SPEED = 30;             // W; D 0F13
	/** It arcs under this (D 0B97 #96: M). */
	public static final double GRENADE_GRAVITY = 9.8;
	public static final double GRENADE_RADIUS_CONTACT = 0.275; // W; D 2936 + 28A6
	public static final double GRENADE_LIFETIME = 60;          // D

	// ---- Nano Boost (ultimate) ----
	public static final double ULT_COST = 2300;                // W; D 0ADD
	public static final double NANO_RANGE = 40;                // W; D 1B4F
	/** The pick's cone (D, x the Nano Boost Sensitivity option at its default 100%; half or full angle L: ours half). */
	public static final double NANO_CONE = 10;
	/** The cast: the boost starts at 0.13 s, the whole cast 0.75 s; the charge is spent as it starts. */
	public static final double NANO_START = 0.13, NANO_CAST = 0.75;    // D 0B9E #176 / #175; W 0.128 + 0.608
	public static final double NANO_HEAL = 250;                // W; D 125C
	public static final double NANO_DAMAGE_DEALT = 0.5;        // W; D 125B
	public static final double NANO_DAMAGE_TAKEN = 0.5;        // W; D 125A
	public static final double NANO_TIME = 8;                  // W; D 0F17

	// ---- quick melee and the passives ----
	public static final double MELEE_DAMAGE = 40;              // BZ 2024-02-13; D 0043 #352
	/** Tactician: the ult meter fills on to +25% of the cost, gains above 100% at x0.75, the excess kept after the ult. */
	public static final double TACTICIAN_BONUS = 0.25;         // BZ 2026-02-10; D 23DB
	public static final double TACTICIAN_GAIN = 0.75;          // BZ 2026-02-10; D 23DC

	// ---- perks ----
	public static final double PERK_MINOR_XP = 2025, PERK_MAJOR_XP = 5060;   // D 4BE4 #55 (12B2, + 1291)
	/** Local Anesthetic (minor): the sleep dart bursts, 45 over 3 s in 30 ticks to enemies within 4 m, slowing them. */
	public static final double ANESTHETIC_DAMAGE = 45, ANESTHETIC_TIME = 3, ANESTHETIC_RADIUS = 4;   // BZ 2026-10-06; D 6123
	/** Its slow: 40% easing to 20% over the 3 s (D 6123 #380: M; W says 50%: the data used). */
	public static final double ANESTHETIC_SLOW_FROM = 0.4, ANESTHETIC_SLOW_TO = 0.2;
	/** Speed Serum (minor): Nano Boost speeds Ana and her target. */
	public static final double SPEED_SERUM = 0.3;              // BZ 2026-06-16 (40 -> 30%); D 51F0 #401
	/** Biotic Bounce (major): the grenade bounces and bursts again for this. */
	public static final double BOUNCE_BURST = 60;              // BZ 2025-08-26 (25 -> 60); D 1549
	/** Headhunter (major): the rifle's head hits deal this much more, scoped and unscoped. */
	public static final double HEADHUNTER = 2;                 // BZ 2025-10-14 (1.5x -> 2x); D 0B95 #269
}
