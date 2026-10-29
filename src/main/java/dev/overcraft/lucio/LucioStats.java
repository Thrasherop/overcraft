package dev.overcraft.lucio;

/**
 * Lúcio's numbers in Overwatch units (metres, seconds, Overwatch health), live game as of the Oct 20 2026 patch
 * (tools/reference/lucio.md has every quote). Sources: Blizzard patch notes first, the Overwatch wiki where the notes
 * give no number. Where a source gave none, the value is ours and says so.
 */
public final class LucioStats {
	private LucioStats() {
	}

	// Base
	public static final int HEALTH = 225;                     // Aug 20 2024

	// Sonic Amplifier (primary fire): a volley of 4
	public static final double AMP_DAMAGE = 22;               // Jul 14 2026
	public static final double AMP_CRIT = 2.0;                // wiki
	public static final int AMP_PER_VOLLEY = 4;               // wiki
	public static final double AMP_SHOT_GAP = 0.125;          // wiki: 0.125 s between a volley's rounds
	/** A volley per 0.935 s (wiki). */
	public static final double AMP_VOLLEY_TIME = 0.935;
	public static final int AMP_AMMO = 20;                    // wiki
	public static final double AMP_RELOAD = 1.5;              // wiki: the animation
	public static final double AMP_RELOAD_AMMO_AT = 1.088;    // wiki: ammo back
	public static final double AMP_SPEED = 50;                // wiki; Apr 11 2017
	/** 0.10 base (Feb 21 2024) + 0.12 for projectiles (Dec 10 2024); the wiki's infobox: 0.22. */
	public static final double AMP_RADIUS = 0.22;
	/** Ours: the knockback exists (Jul 12 2024: "reduced by 10%") but no source gives it. */
	public static final double AMP_KNOCKBACK = 0.6;

	// Soundwave (secondary fire)
	public static final double WAVE_DAMAGE = 45;              // Apr 16 2024
	public static final double WAVE_COOLDOWN = 5;             // Jan 8 2026
	public static final double WAVE_RANGE = 7;                // wiki
	/** Ours: "an arc in front of Lúcio" (wiki), no angle given. */
	public static final double WAVE_HALF_ANGLE = 45;
	/** 17 m/s (wiki: 13.73 horizontal, 10.15 vertical along his aim). */
	public static final double WAVE_KNOCKBACK = 17;
	public static final double WAVE_LOCKOUT = 0.45;           // Feb 13 2024: targets can't move
	public static final double WAVE_RECOVERY = 0.56;          // wiki: before primary fire again

	// Crossfade (Shift): the songs
	public static final double HEAL_SONG = 20;                // Jun 24 2025: allies, per second
	/** Himself: the wiki's 12 a second (60%; Blizzard's May 12 2026 wording would give 8, flagged in lucio.md). */
	public static final double HEAL_SONG_SELF = 12;
	public static final double SPEED_SONG = 0.25;             // Oct 15 2019: +25%
	public static final double SONG_RADIUS = 12;              // Aug 9 2018
	public static final double SONG_LINGER = 1;               // wiki: after leaving the area
	/**
	 * The game data (graph 0266, build 154088): the song changes 0.2 s after the press (an ally's speed 0.2 s after that:
	 * the wiki's 0.4 counts both), and Crossfade can't be pressed again for 0.25 s after.
	 */
	public static final double SONG_SWAP = 0.2;
	public static final double CROSSFADE_COOLDOWN = 0.25;
	/** Ours: the wiki's 0.192 s healing tick, rounded to Minecraft's ticks (4 ticks = 0.2 s). */
	public static final int SONG_TICKS = 4;

	// Amp It Up (E)
	public static final double AMP_UP_TIME = 3;               // wiki; the game data (0510)
	/** The game data (graph 0277): Amp It Up starts 0.2 s after the press, its animation from the press. */
	public static final double AMP_UP_DELAY = 0.2;
	/**
	 * The game data (graph 0277): from the press, Amp It Up holds the weapon state (var 33) for a 0.7 s wait at priority 0
	 * (024 asset 0001), over the reload's -10 (0002, graph 0776): it ends a reload in progress. Crossfade (0266) never
	 * takes that state, so a reload carries on through it.
	 */
	public static final double AMP_UP_LOCK = 0.7;
	public static final double AMP_UP_COOLDOWN = 12;          // wiki: after it ends
	public static final double AMP_UP_HEAL = 56;              // wiki (2.8x the song)
	public static final double AMP_UP_SPEED = 0.60;           // Oct 15 2019

	// Wall Ride (passive)
	/**
	 * Oct 15 2019: +30% on the wall. The game data's wall-ride graph (0263) holds a 1.15 movement modifier while riding and
	 * 1.35 after the wall jump, of a type whose meaning is hashed: kept to Blizzard's number until that's read.
	 */
	public static final double WALL_SPEED = 0.30;
	/**
	 * The leap off (his wall-ride graph 0263, research\lucio\wallride-data.md): 6.9 m/s up, the graph's 1.15 times his
	 * 6 m/s jump (the owner's Workshop recording, 2026-10-28, tools/reference/lucio-wallride-log.md: 6.90 at the launch
	 * looking level, peaking 1.41 m above it), plus 2.5 m/s (game value 212A) along where he looks with forward held,
	 * along the keys with another, along his motion with none.
	 */
	public static final double WALL_JUMP_SPEED = 6.9;
	public static final double WALL_LEAP_FORWARD = 2.5;
	/**
	 * How he rises on the wall (the recording, 151 rides): no gravity; for his first 0.3 s at least 2.72 m/s (arriving
	 * faster, gravity slows him to it), 3.0 m/s until 0.43 s, then that fading at 2 a second. About 2.7 m in all, and
	 * the same wherever he looks (until 2026-10-28 ours: 0.5 m/s, more looking up).
	 */
	public static final double WALL_LIFT = 2.72;
	public static final double WALL_LIFT_PEAK = 3.0;
	public static final double WALL_LIFT_TIME = 0.30;
	public static final double WALL_LIFT_PEAK_END = 0.43;
	public static final double WALL_LIFT_FADE = 2.0;
	/**
	 * Speed along the wall (the recording): up to the wall speed at 30 m/s² (6.87 to 8.53 in 0.06 s); from a stall (he
	 * came square on) at 80 m/s² once the ride starts properly at 0.3 s; above it (a chain of wall jumps) kept for the
	 * first 0.3 s, then braked at 20 m/s².
	 */
	public static final double WALL_ACCEL = 30;
	public static final double WALL_STALL_ACCEL = 80;
	public static final double WALL_BRAKE = 20;
	/** Slower than this along the wall when he gets on, he's square on: the way waits for the ride proper (ours). */
	public static final double WALL_STALL_SPEED = 1.0;
	public static final double WALL_COOLDOWN = 0.5;           // wiki; the data: game value 1E3F, a 0.5 s wait after any ride
	/**
	 * How close a wall must be to ride it, past Minecraft's player box: he gets on within about 1 m of his middle (the
	 * recording at chest height on the attach tick: median 0.84 m, 1.06 at the 90th percentile; until 2026-10-28 ours,
	 * 0.4: the owner, #57, "more forgiving"), then rides close (0.5-0.6 m), pulled in at {@link #WALL_PULL}.
	 */
	public static final double WALL_REACH = 0.7;
	/** How fast he's drawn in to the wall he got on (m/s; the recording: 0.84 to 0.51 m in 0.19 s). */
	public static final double WALL_PULL = 1.7;

	// Sound Barrier (Q)
	public static final int ULT_COST = 2900;                  // wiki
	public static final double BARRIER_OVERHEALTH = 625;      // May 12 2026
	public static final double BARRIER_TIME = 6;              // Oct 6 2026
	public static final double BARRIER_RADIUS = 30;           // wiki
	public static final double BARRIER_CAST = 0.656;          // wiki
	/**
	 * The leap on casting, up, and the slam: the owner's Workshop recording (2026-10-28, cast in the air after a wall
	 * jump; tools/reference/lucio-wallride-log.md) rises 1.07 m, its top 0.34 s after the cast (6.0 m/s here; until
	 * 2026-10-29 ours was 7.4, fitted to the cast clip's feet, 1.6 m), his speed along kept; from the cast's end
	 * (0.656 s) he falls at about 40 m/s² until he lands: Blizzard's 240% gravity (Dec 10 2024), which the recording
	 * shows starting with the cast's end.
	 */
	public static final double BARRIER_LEAP = 6.0;
	public static final double BARRIER_GRAVITY = 2.4;

	// Tactician (sub-role, Feb 10 2026): ult charge kept past 100%, up to 125%, gained at -25% above 100%
	public static final double TACTICIAN_CAP = 1.25;
	public static final double TACTICIAN_RATE = 0.75;

	// Perks (Blizzard's hero page, 2026-10-26)
	/**
	 * Level 2 and 3, counted from the start (the wiki's infobox; ours as for Kiriko). The game data's own costs are 2020 then
	 * 3030 (12A0 / 12A1): 5050 in all.
	 */
	public static final double PERK_MINOR_XP = 2020;
	public static final double PERK_MAJOR_XP = 5050;
	/** Soundwave Rider: riding a wall charges the next Soundwave, +25% knockback and +50% damage; ~5 s fills it (wiki). */
	public static final double RIDER_KNOCKBACK = 0.25;
	public static final double RIDER_DAMAGE = 0.50;
	public static final double RIDER_CHARGE_TIME = 5;
	/** Beat Drop: Amp It Up for 4 s after casting Sound Barrier (Oct 6 2026). */
	public static final double BEAT_DROP_TIME = 4;
	/** Noise Violation: Crossfade's range +150% while Amp It Up is on (30 m). */
	public static final double NOISE_VIOLATION_RANGE = 1.5;
	/** Accelerando: +15% attack speed with the speed song, tripled (+45%) during Amp It Up (Apr 14 2026). */
	public static final double ACCELERANDO = 0.15;
}
