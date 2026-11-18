package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.entity.FragEntity;
import dev.overcraft.entity.PharahRocketEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.junkrat.Junkrat;
import dev.overcraft.junkrat.JunkratSounds;
import dev.overcraft.pharah.Pharah;
import dev.overcraft.pharah.PharahSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.junkrat.JunkratStats.FRAG_DIRECT;
import static dev.overcraft.junkrat.JunkratStats.FRAG_KNOCK_AWAY;
import static dev.overcraft.junkrat.JunkratStats.FRAG_KNOCK_EDGE;
import static dev.overcraft.junkrat.JunkratStats.FRAG_KNOCK_UP;
import static dev.overcraft.junkrat.JunkratStats.FRAG_SPLASH;
import static dev.overcraft.junkrat.JunkratStats.FRAG_SPLASH_EDGE;
import static dev.overcraft.junkrat.JunkratStats.FRAG_SPLASH_FULL;
import static dev.overcraft.junkrat.JunkratStats.FRAG_SPLASH_RADIUS;
import static dev.overcraft.pharah.PharahStats.ROCKET_DIRECT;
import static dev.overcraft.pharah.PharahStats.ROCKET_KNOCK_AWAY;
import static dev.overcraft.pharah.PharahStats.ROCKET_KNOCK_UP;
import static dev.overcraft.pharah.PharahStats.ROCKET_SPLASH;
import static dev.overcraft.pharah.PharahStats.ROCKET_SPLASH_EDGE;
import static dev.overcraft.pharah.PharahStats.ROCKET_SPLASH_FULL;
import static dev.overcraft.pharah.PharahStats.ROCKET_SPLASH_RADIUS;

/**
 * An enemy hero's frag or rocket going off (#46): the mob's side of Junkrat.fragBurst and Pharah.burst, which take a player owner
 * (their perks, ult charge, hit markers, self-push). The same numbers (JunkratStats / PharahStats): the direct hit, then the
 * splash with its falloff on everyone it may hurt (Allies.areaHits with the enemy hero as the shooter: players, their pets,
 * villagers, bystanders; never a monster) in sight and not behind a barrier, and its push. No damage to itself (Junkrat's Total
 * Mayhem; Pharah's own share of her splash is left out, ours: a mob that hurt itself would retarget itself).
 */
public final class EnemyBlasts {
	private EnemyBlasts() {
	}

	/** A frag of enemy Junkrat's goes off (FragEntity.burst). */
	public static void frag(EnemyHeroEntity owner, FragEntity frag, Vec3 at, @Nullable Entity direct) {
		ServerLevel level = (ServerLevel) frag.level();
		boolean hit = blast(owner, frag, at, direct, FRAG_DIRECT, FRAG_SPLASH, FRAG_SPLASH_FULL, FRAG_SPLASH_RADIUS, FRAG_SPLASH_EDGE,
			FRAG_KNOCK_AWAY, FRAG_KNOCK_UP, FRAG_KNOCK_EDGE, false);
		Effects.play(level, "jr_frag_burst", at, frag.burstNormal(), null, 0, owner);
		JunkratSounds.all(level, "frag.explode", at, 120);
		if (!hit && direct == null) {
			JunkratSounds.all(level, "frag.explode_world", at, 60);
		}
	}

	/** A rocket of enemy Pharah's goes off (PharahRocketEntity.burst). */
	public static void rocket(EnemyHeroEntity owner, PharahRocketEntity rocket, Vec3 at, @Nullable Entity direct) {
		ServerLevel level = (ServerLevel) rocket.level();
		boolean hit = blast(owner, rocket, at, direct, ROCKET_DIRECT, ROCKET_SPLASH, ROCKET_SPLASH_FULL, ROCKET_SPLASH_RADIUS,
			ROCKET_SPLASH_EDGE, ROCKET_KNOCK_AWAY, ROCKET_KNOCK_UP, 0, true);
		Effects.play(level, "pharah_rocket_burst", at, Vec3.ZERO, null, 0, owner);
		PharahSounds.all(level, "rocket.burst", at, 150);
		if (!hit && direct == null) {
			PharahSounds.all(level, "rocket.hit_block", at, 60);
		}
	}

	/**
	 * The direct hit ({@code direct}, when the owner may hurt it), then the splash, full to {@code full} m and {@code edge} of it at
	 * {@code radius} (Junkrat.falloff, the same linear rule as Pharah.splashFactor), and its push: Junkrat.knockShare,
	 * easing to {@code knockEdge} at the radius, or Pharah's ring ({@code pharahKnock}: Pharah.rocketKnockFactor). Returns whether
	 * anyone was hurt.
	 */
	private static boolean blast(EnemyHeroEntity owner, Entity from, Vec3 at, @Nullable Entity direct, double directDamage,
								 double splash, double full, double radius, double edge, double away, double up, double knockEdge,
								 boolean pharahKnock) {
		ServerLevel level = (ServerLevel) from.level();
		boolean hit = false;
		Blow blow = Blow.weapon(false);
		if (direct instanceof LivingEntity target && target.isAlive() && Allies.canHurt(owner, target)) {
			hit |= owner.deal(target, owner.shotSource(from), directDamage, blow) > 0;
		}
		// a player's barriers in reach take the splash, as a player's blast's do
		for (Barriers.Shape b : Barriers.enemyBarriers(level, owner)) {
			Vec3 near = Barriers.nearest(b, at);
			if (near.distanceTo(at) <= radius) {
				b.barrierTake(splash * Junkrat.falloff(near.distanceTo(at), full, radius, edge), near);
			}
		}
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius + 2))) {
			if (e == owner || !e.isAlive() || !Allies.areaHits(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d > radius || !Combat.inSight(level, at, e, owner) || Barriers.between(level, at, e.getBoundingBox().getCenter(), owner) != null
				|| dev.overcraft.brigitte.Barrier.shields(e, owner, at)) {
				continue;
			}
			hit |= owner.deal(e, owner.blastSource(from), splash * Junkrat.falloff(d, full, radius, edge), blow) > 0;
			if (!e.isAlive()) {
				continue;
			}
			double k = pharahKnock ? Pharah.rocketKnockFactor(d) : Junkrat.knockShare(d, full, radius, knockEdge);
			Vec3 dir = e.getBoundingBox().getCenter().subtract(at);
			dir = new Vec3(dir.x, 0, dir.z);
			dir = dir.lengthSqr() < 1.0E-6 ? Vec3.ZERO : dir.normalize();
			Combat.push(e, dir.scale(Ow.speed(away * k)).add(0, Ow.speed(up * k), 0), owner, blow.by(owner.hero()));
		}
		return hit;
	}
}
