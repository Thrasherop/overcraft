package dev.overcraft.hero;

import dev.overcraft.Ow;
import dev.overcraft.kiriko.KirikoStats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * Overwatch movement in place of Minecraft's walking physics, for a hero with hotbar slot 1 held. Players move
 * themselves (the client simulates its own player), so this runs on the client for the local player.
 *
 * Numbers (public sources: the Overwatch wiki's Movement speed page, a Workshop measurement where noted):
 * 5.5 m/s in any direction but straight back (90%) and diagonally back (93.3%), crouch 3 m/s; ground speed reached
 * and lost in about 0.1 s; jump 5.72 m/s up (0.98 m apex); gravity about 16.7 m/s² rising (fits the jump and climb
 * heights) and 17.5 m/s² falling, easing off toward a 30 m/s fall speed; no fall damage.
 */
public final class OwMovement {
	public static final double RUN_SPEED = 5.5;
	public static final double BACK_FACTOR = 0.9;
	public static final double BACK_DIAGONAL_FACTOR = 14.0 / 15.0;
	public static final double CROUCH_SPEED = 3.0;
	/** 5.5 m/s in 0.1 s (Workshop measurement). */
	public static final double GROUND_ACCEL = 55;
	/** Ours: Overwatch has air control but publishes no number. Steering, not speeding up. */
	public static final double AIR_ACCEL = 12;
	public static final double JUMP_SPEED = 5.72;
	public static final double GRAVITY_UP = 16.7;
	public static final double GRAVITY_DOWN = 17.5;
	public static final double TERMINAL_SPEED = 30;
	/** Buffs add up to at most +75% (wiki). */
	public static final double MAX_SPEED_BONUS = 0.75;

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
		// eases off linearly toward the terminal speed (fits the wiki's 30 m/s after about 6.4 s)
		return Math.max(0, GRAVITY_DOWN * (1 + verticalSpeed / TERMINAL_SPEED));
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
		Vec3 horizontal = new Vec3(v.x, 0, v.z);
		if (entity.onGround()) {
			horizontal = approach(horizontal, wish, Ow.accel(GROUND_ACCEL));
		} else if (wish != Vec3.ZERO) {
			// air control: steer toward the input without gaining speed beyond the faster of the two
			double keep = Math.max(horizontal.length(), wish.length());
			horizontal = approach(horizontal, wish, Ow.accel(AIR_ACCEL));
			if (horizontal.length() > keep) {
				horizontal = horizontal.normalize().scale(keep);
			}
		}

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
		Vec3 after = entity.getDeltaMovement();
		// move() zeroes what a wall or the floor stopped (keep that); the vertical speed carried on is the new one
		double carried = after.y == 0 || (entity.onGround() && vy < 0) ? 0 : vy;
		entity.setDeltaMovement(after.x, carried, after.z);
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
