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
	/**
	 * What their Overwatch damage is multiplied by on a player: Easy 0.2, Normal 0.3, Hard 0.4 (ours; play 2026-11-17 at 0.5 on Normal: Junkrat
	 * ~7.5 health a second, Tracer up close ~5.3, Cassidy ~2.2, against a skeleton's 2-3: a plain player died in 3 s). Peaceful none.
	 */
	public static double playerDamageFactor(Difficulty difficulty) {
		return switch (difficulty) {
			case PEACEFUL -> 0;
			case EASY -> 0.2;
			case NORMAL -> 0.3;
			case HARD -> 0.4;
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

	// ---- batch 2 (#46): Ashe, Sojourn, Torbjörn, Widowmaker ----

	/** Ashe keeps 10 to 30 m (ours: inside her scoped falloff start, 35 m: AsheStats). */
	public static final double ASHE_NEAR = 10, ASHE_FAR = 30;
	/** A scoped-style shot every 1.2 s (ours, the plan's cut-down kit; her scoped Viper fires every 0.65 s: AsheStats.ADS_INTERVAL). */
	public static final double ASHE_SHOT_INTERVAL = 1.2;
	/** She aims down the sights for 0.5 s before each shot (ours: her 0.2 s scope-in, AsheStats.ADS_IN, and a 0.3 s settle). */
	public static final double ASHE_AIM = 0.5;
	/** Coach Gun when her target is within 5 m (ours, the plan's). */
	public static final double ASHE_COACH_WITHIN = 5;

	/** Whether enemy Ashe fires Coach Gun at a target {@code metres} off: close, seen and off cooldown (ours). */
	public static boolean asheCoach(double metres, boolean sees, int cooldown) {
		return sees && cooldown <= 0 && metres <= ASHE_COACH_WITHIN;
	}

	/** Sojourn keeps 8 to 20 m (ours: mid range, the plan's). */
	public static final double SOJOURN_NEAR = 8, SOJOURN_FAR = 20;
	/** Railgun bursts of 8 rounds, 0.5 s at her 16 a second (ours: the length; the rate is SojournStats.RAILGUN_RATE). */
	public static final int SOJOURN_BURST = 8;
	/** She fires her Charged Shot once she has 60 energy (ours: of her 100, SojournStats.ENERGY_MAX). */
	public static final double SOJOURN_CHARGE_AT = 60;

	/** Her Charged Shot's damage before falloff and crit: 20 + energy, 120 at 100 (SojournStats: CHARGED_BASE, CHARGED_FULL). */
	public static double sojournCharged(double energy) {
		double e = Math.max(0, Math.min(dev.overcraft.sojourn.SojournStats.ENERGY_MAX, energy));
		return dev.overcraft.sojourn.SojournStats.CHARGED_BASE
			+ (dev.overcraft.sojourn.SojournStats.CHARGED_FULL - dev.overcraft.sojourn.SojournStats.CHARGED_BASE) * e
			/ dev.overcraft.sojourn.SojournStats.ENERGY_MAX;
	}

	/**
	 * Her energy one tick later, {@code sinceGain} ticks after its last gain: held for 9 s, then 15 a second down to 0
	 * (SojournStats.ENERGY_DECAY_DELAY / ENERGY_DECAY).
	 */
	public static double sojournEnergyTick(double energy, int sinceGain) {
		if (sinceGain < dev.overcraft.Ow.ticks(dev.overcraft.sojourn.SojournStats.ENERGY_DECAY_DELAY)) {
			return energy;
		}
		return Math.max(0, energy - dev.overcraft.sojourn.SojournStats.ENERGY_DECAY / 20.0);
	}

	/** Torbjörn keeps 6 to 18 m (ours: his rivets' 70 m/s arc barely drops that far). */
	public static final double TORBJORN_NEAR = 6, TORBJORN_FAR = 18;
	/** A rivet every 1 s (ours, the plan's cut-down kit; his 0.51 s: TorbjornStats.RIVET_INTERVAL). */
	public static final double TORBJORN_SHOT_INTERVAL = 1.0;
	/** He throws his turret when his target is within 20 m and in sight (ours), 3 m toward it (ours: "near himself"). */
	public static final double TORBJORN_TURRET_WITHIN = 20, TORBJORN_TURRET_AHEAD = 3;
	/** His turret goes once he's been gone this long (ours: dead, or his chunk unloaded; 2 s covers a save's reload order). */
	public static final double TURRET_ORPHAN = 2;

	/** Widowmaker keeps 20 to 40 m (ours, the plan's). */
	public static final double WIDOW_NEAR = 20, WIDOW_FAR = 40;
	/** Her sight: 48 m (ours: past her 40 m, so she finds a target at her own range; the others' is FOLLOW_RANGE). */
	public static final double WIDOW_FOLLOW_RANGE = 48;
	/** Each scoped shot takes 1.5 s of aim, her red sight line showing it (ours: time to take cover; her own full charge is 0.9 s). */
	public static final double WIDOW_AIM = 1.5;
	/** Her aim holds still the last 0.25 s (ours: a player who sidesteps then is missed). */
	public static final double WIDOW_AIM_LOCK = 0.25;
	/** 1 s more between one shot and the next aim (ours; her own 0.5 s recovery is WidowmakerStats.SCOPED_RECOVERY). */
	public static final double WIDOW_BETWEEN = 1.0;
	/** Her shot at half its Overwatch damage, before the difficulty's factor (ours: "scaled hard down", she one-shots in Overwatch). */
	public static final double WIDOW_DAMAGE_SHARE = 0.5;

	/** Her shot: a full charge's (WidowmakerStats: 120, its 50-70 m falloff, x2.5 on the head), x WIDOW_DAMAGE_SHARE. */
	public static double widowShot(double metres, boolean crit) {
		return dev.overcraft.widowmaker.Widowmaker.scopedDamage(dev.overcraft.widowmaker.WidowmakerStats.CHARGE_MAX, metres, crit)
			* WIDOW_DAMAGE_SHARE;
	}

	/** Whether her aim, {@code ticksIn} into its WIDOW_AIM, still follows the target (else held still). */
	public static boolean widowTracks(int ticksIn) {
		return ticksIn < dev.overcraft.Ow.ticks(WIDOW_AIM) - dev.overcraft.Ow.ticks(WIDOW_AIM_LOCK);
	}
}
