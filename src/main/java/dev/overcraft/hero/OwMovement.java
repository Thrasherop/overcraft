package dev.overcraft.hero;

import dev.overcraft.Ow;
import dev.overcraft.kiriko.KirikoStats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * Overwatch movement in place of Minecraft's walking physics, for a hero with hotbar slot 1 held. Players move
 * themselves (the client simulates its own player), so this runs on the client for the local player.
 *
 * Numbers (public sources: the Overwatch wiki's Movement speed page, a Workshop measurement where noted):
 * 5.5 m/s in any direction but straight back (90%) and diagonally back (93.3%), crouch 3 m/s; jump 5.72 m/s up
 * (0.98 m apex); gravity about 16.7 m/s² rising (fits the jump and climb heights) and 17.5 m/s² falling, easing off
 * toward a 30 m/s fall speed. Falls hurt only from the hero's own safe height (Hero.safeFall, #35).
 *
 * How speed is gained and lost is Overwatch's own mover (#31): every hero's CharacterMover in the game data (build
 * 154088, read offline: tools/reference/movement.md) holds a ground friction and stop speed, a ground and an air
 * acceleration (times the input speed, Quake style), and a decay of air speed above the run speed. Their units are
 * inferred, and they agree with the public measurements: full speed in about 0.05 s, a stop in about 0.08 s, Lúcio's
 * 0.6 s slide. It runs three steps a tick (60 Hz), close to Overwatch's 62.5 Hz.
 */
public final class OwMovement {
	public static final double RUN_SPEED = 5.5;
	public static final double BACK_FACTOR = 0.9;
	public static final double BACK_DIAGONAL_FACTOR = 14.0 / 15.0;
	public static final double CROUCH_SPEED = 3.0;
	/** Ground acceleration per second, times the input speed: 5.5 m/s in 0.05 s (game data mover; Workshop: "almost instant"). */
	public static final double GROUND_ACCELERATE = 35;
	/** Ground friction per second, on at least {@link #STOP_SPEED}: a 5.5 m/s run stops in about 0.08 s (game data mover). */
	public static final double FRICTION = 15;
	public static final double STOP_SPEED = 4.5;
	/** Air acceleration per second, times the input speed: about 19 m/s² at a run (game data mover; units inferred). */
	public static final double AIR_ACCELERATE = 3.5;
	/** Air speed above the run speed (5.5 m/s, buffs included) decays by this much per second (game data mover). */
	public static final double AIR_DECAY = 2.0;
	/** Steps per tick: Overwatch simulates at 62.5 Hz (Derek Mulder, Blizzard); 3 a tick is 60 Hz. */
	public static final int SUBSTEPS = 3;
	public static final double JUMP_SPEED = 5.72;
	public static final double GRAVITY_UP = 16.7;
	public static final double GRAVITY_DOWN = 17.5;
	public static final double TERMINAL_SPEED = 30;
	/** Buffs add up to at most +75% (wiki). */
	public static final double MAX_SPEED_BONUS = 0.75;
	/** How far below a ledge's top the feet may be for the ledge assist to lift the hero onto it (m, ours). */
	public static final double LEDGE_REACH = 0.3;

	/** Set by the client: this entity moves the Overwatch way (the local player, a hero, slot 1). */
	public static Predicate<LivingEntity> active = entity -> false;
	/** Set by the client: this entity has Kiriko's wall climb. */
	public static Predicate<LivingEntity> wallClimb = entity -> false;

	private static final Map<LivingEntity, Climb> CLIMBS = new WeakHashMap<>();

	private static final class Climb {
		int ticks;
		boolean climbing;
		boolean used;
	}

	private OwMovement() {
	}

	/** Whether to replace Minecraft's walking/falling this tick (not swimming, flying, on ladders or levitating). */
	public static boolean handles(LivingEntity entity) {
		return active.test(entity) && !entity.onClimbable() && !entity.hasEffect(MobEffects.LEVITATION)
			&& !entity.isFallFlying();
	}

	/** Speed multiplier from attribute buffs and slows (e.g. Kitsune Rush +30%), sprinting not included. */
	public static double speedFactor(LivingEntity entity) {
		AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null || speed.getBaseValue() <= 0) {
			return 1;
		}
		double factor = speed.getValue() / speed.getBaseValue();
		if (entity.isSprinting()) {
			factor /= 1.3; // Minecraft's sprint modifier; heroes don't sprint
		}
		return Math.clamp(factor, 0, 1 + MAX_SPEED_BONUS);
	}

	/** Target speed in m/s for this input: x strafe (left +), z forward (+). */
	public static double runSpeed(double x, double z, boolean crouching) {
		if (x == 0 && z == 0) {
			return 0;
		}
		if (crouching) {
			return CROUCH_SPEED;
		}
		if (z < 0) {
			return RUN_SPEED * (x == 0 ? BACK_FACTOR : BACK_DIAGONAL_FACTOR);
		}
		return RUN_SPEED;
	}

	/** Gravity this tick in m/s², from the vertical speed in m/s. */
	public static double gravity(double verticalSpeed) {
		if (verticalSpeed > 0) {
			return GRAVITY_UP;
		}
		// eases off toward the terminal speed: v = 30 tanh(17.5 t / 30) fits the wiki's measured falling graph within 1%
		double ratio = verticalSpeed / TERMINAL_SPEED;
		return Math.max(0, GRAVITY_DOWN * (1 - ratio * ratio));
	}

	/** One step on the ground, velocities in m/s: friction, then acceleration toward {@code wish} (Overwatch's mover). */
	public static Vec3 groundStep(Vec3 v, Vec3 wish, double dt) {
		double speed = v.length();
		if (speed > 0) {
			double drop = Math.max(speed, STOP_SPEED) * FRICTION * dt;
			v = v.scale(Math.max(0, speed - drop) / speed);
		}
		return accelerate(v, wish, GROUND_ACCELERATE, dt);
	}

	/**
	 * One step in the air, m/s. Input within 90° of the velocity turns it toward the input at the air acceleration,
	 * keeping its speed ("the direction is changing faster but your overall speed isn't being slowed down": Geoff Goodman,
	 * 2018-01-17), and speeds it up to the input speed; input further round brakes along it. Then speed above
	 * {@code maxSpeed} decays.
	 */
	public static Vec3 airStep(Vec3 v, Vec3 wish, double maxSpeed, double dt) {
		double wishSpeed = wish.length();
		if (wishSpeed > 0) {
			double step = AIR_ACCELERATE * wishSpeed * dt;
			double speed = v.length();
			double dot = v.x * wish.x + v.z * wish.z;
			if (speed < 1.0E-6 || dot < 0) {
				double keep = Math.max(speed, wishSpeed);
				v = approach(v, wish, step);
				if (v.length() > keep) {
					v = v.normalize().scale(keep);
				}
			} else {
				// a turn at constant speed: the acceleration over the speed is the turn rate
				double angle = Math.atan2(v.x * wish.z - v.z * wish.x, dot);
				double turn = Math.copySign(Math.min(Math.abs(angle), step / speed), angle);
				double newSpeed = speed < wishSpeed ? Math.min(wishSpeed, speed + step) : speed;
				double cos = Math.cos(turn);
				double sin = Math.sin(turn);
				v = new Vec3(v.x * cos - v.z * sin, 0, v.x * sin + v.z * cos).scale(newSpeed / speed);
			}
		}
		double speed = v.length();
		if (speed > maxSpeed) {
			v = v.scale((maxSpeed + (speed - maxSpeed) * Math.max(0, 1 - AIR_DECAY * dt)) / speed);
		}
		return v;
	}

	/** Adds speed along {@code wish}'s direction, up to its length, at {@code rate} times its length per second. */
	private static Vec3 accelerate(Vec3 v, Vec3 wish, double rate, double dt) {
		double wishSpeed = wish.length();
		if (wishSpeed <= 0) {
			return v;
		}
		Vec3 dir = wish.scale(1 / wishSpeed);
		double add = wishSpeed - v.dot(dir);
		return add <= 0 ? v : v.add(dir.scale(Math.min(rate * wishSpeed * dt, add)));
	}

	/** Moves {@code current} toward {@code target} by at most {@code step}. */
	public static Vec3 approach(Vec3 current, Vec3 target, double step) {
		Vec3 diff = target.subtract(current);
		double len = diff.length();
		return len <= step ? target : current.add(diff.scale(step / len));
	}

	/** Replaces {@code LivingEntity.travelInAir}. {@code input}: x strafe, z forward, as Minecraft passes it. */
	public static void travel(LivingEntity entity, Vec3 input) {
		Vec3 v = entity.getDeltaMovement();
		Vec3 flat = new Vec3(input.x, 0, input.z);
		double speed = Ow.speed(runSpeed(Math.signum(input.x), Math.signum(input.z), entity.isCrouching())) * speedFactor(entity);
		Vec3 wish = Vec3.ZERO;
		if (speed > 0 && flat.lengthSqr() > 1.0E-7) {
			Vec3 dir = flat.normalize();
			double sin = Math.sin(Math.toRadians(entity.getYRot()));
			double cos = Math.cos(Math.toRadians(entity.getYRot()));
			wish = new Vec3(dir.x * cos - dir.z * sin, 0, dir.z * cos + dir.x * sin).scale(speed);
		}
		// the mover runs in m/s at 60 Hz; the tick moves by the steps' average and carries the last step's speed
		Vec3 wishMs = wish.scale(20);
		Vec3 stepped = new Vec3(v.x, 0, v.z).scale(20);
		Vec3 travelled = Vec3.ZERO;
		// the tick she jumps she's still on the ground: that tick is in the air already (no friction on take-off)
		boolean grounded = entity.onGround() && v.y <= 0;
		double maxAirSpeed = RUN_SPEED * speedFactor(entity);
		for (int i = 0; i < SUBSTEPS; i++) {
			stepped = grounded ? groundStep(stepped, wishMs, 0.05 / SUBSTEPS) : airStep(stepped, wishMs, maxAirSpeed, 0.05 / SUBSTEPS);
			travelled = travelled.add(stepped);
		}
		Vec3 horizontal = travelled.scale(1.0 / SUBSTEPS / 20);
		Vec3 end = stepped.scale(1.0 / 20);

		double vy = v.y;
		double dy = vy;
		if (climb(entity, input)) {
			vy = dy = Ow.speed(KirikoStats.CLIMB_SPEED);
		} else {
			// on the ground too: the small push down is what keeps Minecraft's onGround true
			double[] step = verticalStep(vy, entity.hasEffect(MobEffects.SLOW_FALLING));
			dy = step[0];
			vy = step[1];
		}

		entity.setDeltaMovement(horizontal.x, dy, horizontal.z);
		entity.move(MoverType.SELF, entity.getDeltaMovement());
		if (!entity.onGround() && entity.horizontalCollision && wish != Vec3.ZERO) {
			ledge(entity, wish);
		}
		Vec3 after = entity.getDeltaMovement();
		// move() zeroes what a wall or the floor stopped (keep that); the speeds carried on are the last step's
		double carried = after.y == 0 || (entity.onGround() && vy < 0) ? 0 : vy;
		entity.setDeltaMovement(after.x == 0 ? 0 : end.x, carried, after.z == 0 ? 0 : end.z);
	}

	/**
	 * Ledge assist (ours): Overwatch's jump peaks at 0.98 m, just short of a Minecraft block, and Minecraft only steps up
	 * 0.6 m on landing. In the air, pushing into a ledge whose top is at most {@link #LEDGE_REACH} above the feet lifts
	 * the hero onto it, from any direction (running, strafing, backing up), as a 1-block step should allow.
	 */
	private static void ledge(LivingEntity entity, Vec3 wish) {
		Vec3 push = wish.normalize().scale(0.1);
		AABB box = entity.getBoundingBox();
		if (entity.level().noCollision(entity, box.move(push.x, 0, push.z))) {
			return; // brushing along a wall, not pushing into a ledge
		}
		for (double lift = 0.05; lift <= LEDGE_REACH + 1.0E-6; lift += 0.05) {
			AABB raised = box.move(0, lift, 0);
			if (entity.level().noCollision(entity, raised) && entity.level().noCollision(entity, raised.move(push.x, 0, push.z))) {
				entity.setPos(entity.getX(), entity.getY() + lift, entity.getZ());
				return;
			}
		}
	}

	/**
	 * One tick of falling, in blocks per tick: {displacement, new vertical speed}. The displacement is the average of
	 * the old and new speeds, so a jump peaks at the published height (gravity-then-move, Minecraft's order, falls
	 * about 15% short).
	 */
	public static double[] verticalStep(double vy, boolean slowFalling) {
		double g = gravity(vy * 20);
		if (slowFalling && vy <= 0) {
			g = Math.min(g, 0.01 * 400);
		}
		double next = Math.max(vy - Ow.accel(g), -Ow.speed(TERMINAL_SPEED));
		return new double[] {(vy + next) / 2, next};
	}

	/**
	 * Kiriko's wall climb: hold jump (and forward) facing a wall to run up it at 8.3 m/s for up to 1 s. Once per time in
	 * the air: it comes back on landing. Momentum carries her over the top.
	 */
	private static boolean climb(LivingEntity entity, Vec3 input) {
		if (!wallClimb.test(entity)) {
			return false;
		}
		Climb climb = CLIMBS.computeIfAbsent(entity, e -> new Climb());
		if (entity.onGround() && !climb.climbing) {
			climb.used = false;
			climb.ticks = 0;
		}
		boolean wants = jumping(entity) && input.z > 0 && entity.horizontalCollision;
		if (climb.climbing) {
			if (wants && climb.ticks < Ow.ticks(KirikoStats.CLIMB_TIME)) {
				climb.ticks++;
				return true;
			}
			climb.climbing = false;
			climb.used = true;
			return false;
		}
		if (wants && !climb.used) {
			climb.climbing = true;
			climb.ticks = 1;
			return true;
		}
		return false;
	}

	/** Climbing a wall right now (the local player only: other players' movement isn't simulated here). */
	public static boolean isClimbing(LivingEntity entity) {
		Climb climb = CLIMBS.get(entity);
		return climb != null && climb.climbing;
	}

	/** Set by the client: the jump key is held. */
	public static Predicate<LivingEntity> jumpHeld = entity -> false;

	private static boolean jumping(LivingEntity entity) {
		return jumpHeld.test(entity);
	}
}
