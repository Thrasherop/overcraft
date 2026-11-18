package dev.overcraft.entity.enemy;

import net.minecraft.world.Difficulty;

/**
 * The enemy heroes' own numbers (#46, tools/reference/enemy-heroes-plan.md). Each hero's kit numbers come from its Stats class
 * (TracerStats, CassidyStats, JunkratStats, PharahStats), already sourced there; everything here is ours (the plan's defaults,
 * open for the owner) unless it says otherwise. Pure, so EnemyHeroStatsTest can check it offline.
 */
public final class EnemyHeroStats {
	private EnemyHeroStats() {
	}

	// ---- spawning (ours: the plan's "How often") ----
	/** 1 in 12 natural night spawns of a zombie, skeleton or husk in the Overworld becomes an enemy hero (ours). */
	public static final int SPAWN_ONE_IN = 12;
	/** Never more than 2 enemy heroes within 64 m of a player (ours). */
	public static final int CAP = 2;
	public static final double CAP_RADIUS = 64;

	// ---- damage and aim (ours: the plan's "Damage to a player" and "Aim") ----
	/** What their Overwatch damage is multiplied by on a player: Easy 0.4, Normal 0.5, Hard 0.65 (ours); Peaceful none. */
	public static double playerDamageFactor(Difficulty difficulty) {
		return switch (difficulty) {
			case PEACEFUL -> 0;
			case EASY -> 0.4;
			case NORMAL -> 0.5;
			case HARD -> 0.65;
		};
	}

	/** Degrees of spread added to the hero's own, as a skeleton's aim is worse on easier difficulties (ours). */
	public static double extraSpread(Difficulty difficulty) {
		return switch (difficulty) {
			case PEACEFUL, EASY -> 5;
			case NORMAL -> 3;
			case HARD -> 2;
		};
	}

	/** The pause between bursts, 0.5 to 1 s (ours: the plan's aim row), as ticks from a roll in [0, 1). */
	public static int pauseTicks(double roll) {
		return 10 + (int) Math.floor(Math.max(0, Math.min(0.999, roll)) * 11);
	}

	/** Whether a spawn may become one: the roll hit (1 in SPAWN_ONE_IN), and fewer than CAP are near the nearest player. */
	public static boolean mayReplace(int roll, int nearby) {
		return roll == 0 && nearby < CAP;
	}

	// ---- movement (M: Minecraft's own movement) ----
	/**
	 * The MOVEMENT_SPEED attribute that walks a mob at {@code metresPerSecond}. A player walks 4.317 m/s at 0.1 with a 0.98 input; a
	 * mob's input is its speed itself (Mob.setSpeed sets zza to it), so a mob covers about 44.05 x speed² m/s (a zombie's 0.23:
	 * 2.33 m/s). M, from Minecraft 26.3's LivingEntity.travel / MoveControl.
	 */
	public static double speedAttribute(double metresPerSecond) {
		return Math.sqrt(metresPerSecond / (4.317 / 0.098));
	}

	/**
	 * The way to throw at {@code speed} (blocks a tick) falling at {@code gravity} (blocks a tick²) to land {@code to} away (enemy
	 * Junkrat's frags at the feet): the low arc, tan a = (v² - sqrt(v⁴ - g(g x² + 2 y v²))) / (g x), the textbook drag-free arc (his
	 * frags have no drag: OwProjectile); out of reach, 45 degrees, the furthest.
	 */
	public static net.minecraft.world.phys.Vec3 lob(net.minecraft.world.phys.Vec3 to, double speed, double gravity) {
		double x = Math.sqrt(to.x * to.x + to.z * to.z);
		if (x < 1.0E-6) {
			return to.normalize();
		}
		double v2 = speed * speed;
		double root = v2 * v2 - gravity * (gravity * x * x + 2 * to.y * v2);
		double tan = root < 0 ? 1 : (v2 - Math.sqrt(root)) / (gravity * x);
		return new net.minecraft.world.phys.Vec3(to.x / x, tan, to.z / x).normalize();
	}

	// ---- the rest (ours) ----
	/** XP as a skeleton's (Minecraft's 5: AbstractSkeleton's xpReward). */
	public static final int XP = 5;
	/** 1 to 3 of the hero's themed item on death (ours). */
	public static final int DROP_MIN = 1, DROP_MAX = 3;
	/** How far they see and track a target (ours: a skeleton's 16 doubled, for a hitscan hero at range). */
	public static final double FOLLOW_RANGE = 32;
	/** Wandering, out of a fight, at this share of the run (ours). */
	public static final double WANDER_SHARE = 0.6;

	/** Tracer: bursts of 5 shots (10 bullets) a shot every 2 ticks: 0.5 s (ours, half her 20 shots/s: the plan's cut-down kit). */
	public static final int TRACER_BURST_SHOTS = 5, TRACER_SHOT_TICKS = 2;
	/** Tracer keeps 3 to 10 m from her target (ours: inside her 10 m falloff start). */
	public static final double TRACER_NEAR = 3, TRACER_FAR = 10;
	/** Tracer Blinks in when her target is further than this (ours). */
	public static final double TRACER_BLINK_IN = 12;

	/** Cassidy: one shot every second at range (ours, the plan; his Peacekeeper can fire every 0.5 s: CassidyStats). */
	public static final double CASSIDY_SHOT_INTERVAL = 1.0;
	/** Cassidy keeps 8 to 20 m (ours: inside his 25 m falloff start). */
	public static final double CASSIDY_NEAR = 8, CASSIDY_FAR = 20;
	/** Combat Roll when hit by someone this close (ours: "up close"). */
	public static final double CASSIDY_ROLL_WITHIN = 6;

	/** Junkrat keeps 6 to 15 m (ours: his frags' 30 m/s arc reaches that far at a low angle). */
	public static final double JUNKRAT_NEAR = 6, JUNKRAT_FAR = 15;

	/** Pharah keeps 8 to 20 m (ours). */
	public static final double PHARAH_NEAR = 8, PHARAH_FAR = 20;
	/** She hovers 3 to 6 m up, aiming for 4.5 (ours, the plan). */
	public static final double PHARAH_HOVER_LOW = 3, PHARAH_HOVER_HIGH = 6, PHARAH_HOVER_AIM = 4.5;
	/** About 5 s up, then down to refuel (ours, the plan; her fuel's real numbers are in PharahStats). */
	public static final double PHARAH_HOVER_TIME = 5;
	/** Her drift toward her range while hovering (ours, below her 5.5 run). */
	public static final double PHARAH_DRIFT = 3;
}
