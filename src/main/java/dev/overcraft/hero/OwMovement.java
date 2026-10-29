package dev.overcraft.hero;

import dev.overcraft.Ow;
import dev.overcraft.kiriko.KirikoStats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

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
	/**
	 * How far below a ledge's top the feet may be for the ledge assist to lift the hero onto it (m, ours). 0.6 (it was
	 * 0.3) so a running jump onto a 1-block step needn't be timed: the owner, #56, "fluid with minecraft's 1 meter
	 * blocks". Measured on a 1-block step at 5.5 m/s: at 0.3 any jump from more than 1.6 m out left the hero at its foot.
	 */
	public static final double LEDGE_REACH = 0.6;

	/**
	 * A hero's mover (each hero's CharacterMover in the game data: tools/reference/movement.md): ground acceleration
	 * (times the wish speed), ground friction and stop speed, air acceleration, the decay of air speed above
	 * {@code maxAir} (m/s at 5.5 m/s run speed, scaled with speed buffs).
	 */
	public record Mover(double groundAccelerate, double friction, double stopSpeed, double airAccelerate, double airDecay,
		double maxAir) {
		/** Every hero's but the skaters' (game data: m_DAA0618B 35, m_E0A0FE25 15, m_E91D1664 4.5, m_E631BA84 3.5, m_4E03E892 2, m_031FE7EE 5.5). */
		public static final Mover DEFAULT = new Mover(GROUND_ACCELERATE, FRICTION, STOP_SPEED, AIR_ACCELERATE, AIR_DECAY, RUN_SPEED);
		/**
		 * Lúcio's, on skates (the same fields: acceleration 15, friction 2, air decay 0.5, max air 11): full speed in about
		 * 0.08 s, a 0.6 s glide to a stop, and in the air speed above twice his run speed decays slowly while speed under it
		 * is kept (the wiki: "air speed doesn't decay below double his ground speed": 11, 13.76 with Speed Boost, 17.6
		 * amped). Before 2026-10-27 he moved on the default mover: stopping dead and losing a wall jump's speed at once.
		 */
		public static final Mover LUCIO = new Mover(15, 2, STOP_SPEED, AIR_ACCELERATE, 0.5, 2 * RUN_SPEED);
	}

	/** Set by the client: the mover an entity uses (its hero's). */
	public static java.util.function.Function<LivingEntity, Mover> mover = entity -> Mover.DEFAULT;

	/** Set by the client: this entity moves the Overwatch way (the local player, a hero, slot 1). */
	public static Predicate<LivingEntity> active = entity -> false;
	/** Set by the client: this entity has Kiriko's wall climb. */
	public static Predicate<LivingEntity> wallClimb = entity -> false;

	private static final Map<LivingEntity, Climb> CLIMBS = new WeakHashMap<>();

	/** Set by the client: this entity flies (Jetpack Cat's jetpack). */
	public static Predicate<LivingEntity> flight = entity -> false;
	/** Set by the client: ticks into Frenetic Flight (-1 not boosting). */
	public static java.util.function.ToIntFunction<LivingEntity> boosting = entity -> -1;
	/** Set by the client: the server moves her (Catnapper's dive and windup): her own flight stands aside. */
	public static Predicate<LivingEntity> driven = entity -> false;
	/** Mercy in Valkyrie: the flight with her mover's numbers (else Jetpack Cat's). */
	public static Predicate<LivingEntity> valkyrie = entity -> false;
	/** Mercy's Guardian Angel: where it takes her and how fast (null when she isn't flying to someone). */
	public static java.util.function.Function<LivingEntity, @org.jspecify.annotations.Nullable Glide> glide = entity -> null;
	/** Mercy just after a Guardian Angel launch: her air damping is off (1 s; 0.5 s in Valkyrie, with her speed raised). */
	public static Predicate<LivingEntity> undamped = entity -> false;
	/** Mercy's Angelic Descent: held, her fall is capped. */
	public static Predicate<LivingEntity> descent = entity -> false;

	/** Guardian Angel's move: to this point at this speed (m/s), arriving within this distance. */
	public record Glide(Vec3 to, double speed, double arrive) {
	}
	/** Set by the client: the crouch key is down (flying, it sinks her). */
	public static Predicate<LivingEntity> crouchHeld = entity -> false;

	/** Set by the client: this entity has Lúcio's Wall Ride. */
	public static Predicate<LivingEntity> wallRide = entity -> false;
	/** Set by the client: the jump key itself is down (Wall Ride holds on it, whatever the auto-climb option says). */
	public static Predicate<LivingEntity> jumpKeyHeld = entity -> false;

	private static final Map<LivingEntity, Ride> RIDES = new WeakHashMap<>();

	private static final class Ride {
		boolean riding;
		/** The wall's outward normal (axis-aligned: Minecraft's walls are). */
		Vec3 normal = Vec3.ZERO;
		/** One way along the wall, unit length; the ride's speed {@link #speed} is signed along it. */
		Vec3 axis = Vec3.ZERO;
		/** m/s along {@link #axis} (negative: the other way). */
		double speed;
		int cooldown;
		/** Ticks on this wall (the wall jump needs a moment on it first). */
		int ticks;
		/** Ticks left in which letting go of jump still wall-jumps after a drop-off (0.2 s: Blizzard 2020-11-17). */
		int jumpWindow;
		/** The wall he last left (its normal) and where: the same wall needs 2 m before he rides it again. */
		@Nullable Vec3 lastNormal;
		@Nullable Vec3 leftAt;
		/** His height when he got on (the rise eases off above it). */
		double startY;

		/** The way he's riding, unit length. */
		Vec3 heading() {
			return speed < 0 ? axis.scale(-1) : axis;
		}
	}

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
		return groundStep(v, wish, Mover.DEFAULT, dt);
	}

	public static Vec3 groundStep(Vec3 v, Vec3 wish, Mover mover, double dt) {
		double speed = v.length();
		if (speed > 0) {
			double drop = Math.max(speed, mover.stopSpeed()) * mover.friction() * dt;
			v = v.scale(Math.max(0, speed - drop) / speed);
		}
		return accelerate(v, wish, mover.groundAccelerate(), dt);
	}

	/**
	 * One step in the air, m/s. Input within 90° of the velocity turns it toward the input at the air acceleration,
	 * keeping its speed ("the direction is changing faster but your overall speed isn't being slowed down": Geoff Goodman,
	 * 2018-01-17), and speeds it up to the input speed; input further round brakes along it. Then speed above
	 * {@code maxSpeed} decays.
	 */
	public static Vec3 airStep(Vec3 v, Vec3 wish, double maxSpeed, double dt) {
		return airStep(v, wish, maxSpeed, Mover.DEFAULT, dt);
	}

	public static Vec3 airStep(Vec3 v, Vec3 wish, double maxSpeed, Mover mover, double dt) {
		double wishSpeed = wish.length();
		if (wishSpeed > 0) {
			double step = mover.airAccelerate() * wishSpeed * dt;
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
			v = v.scale((maxSpeed + (speed - maxSpeed) * Math.max(0, 1 - mover.airDecay() * dt)) / speed);
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
		Mover mover = OwMovement.mover.apply(entity);
		// just after Mercy's launch nothing damps the air speed (her data's F808ACE8 0): no decay above the max
		double maxAirSpeed = undamped.test(entity) ? Double.MAX_VALUE : mover.maxAir() * speedFactor(entity);
		for (int i = 0; i < SUBSTEPS; i++) {
			stepped = grounded ? groundStep(stepped, wishMs, mover, 0.05 / SUBSTEPS)
				: airStep(stepped, wishMs, maxAirSpeed, mover, 0.05 / SUBSTEPS);
			travelled = travelled.add(stepped);
		}
		Vec3 horizontal = travelled.scale(1.0 / SUBSTEPS / 20);
		Vec3 end = stepped.scale(1.0 / 20);

		if (guardianAngel(entity)) {
			return;
		}
		if (fly(entity, input)) {
			return;
		}
		if (ride(entity, v, input)) {
			return;
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
			if (descent.test(entity) && vy < -Ow.speed(dev.overcraft.mercy.MercyStats.DESCENT_SPEED)) {
				// Angelic Descent: her fall held at 2 m/s (W; her data's property 978C5453 = 2.0)
				vy = -Ow.speed(dev.overcraft.mercy.MercyStats.DESCENT_SPEED);
				dy = Math.max(dy, vy);
			}
		}

		entity.setDeltaMovement(horizontal.x, dy, horizontal.z);
		entity.move(MoverType.SELF, entity.getDeltaMovement());
		if (!entity.onGround() && entity.horizontalCollision && wish != Vec3.ZERO && ledge(entity, wish)) {
			// up onto it: the speed the ledge's face took away comes back, and she stops falling (#56: lifted but stopped,
			// she fell back past the edge and was lifted again every tick, hanging there until the key was let go)
			entity.setDeltaMovement(end.x, Math.max(0, dy), end.z);
			return;
		}
		Vec3 after = entity.getDeltaMovement();
		// move() zeroes what a wall or the floor stopped (keep that); the speeds carried on are the last step's
		double carried = after.y == 0 || (entity.onGround() && vy < 0) ? 0 : vy;
		entity.setDeltaMovement(after.x == 0 ? 0 : end.x, carried, after.z == 0 ? 0 : end.z);
	}

	/**
	 * Ledge assist (ours): Overwatch's jump peaks at 0.98 m, just short of a Minecraft block, and Minecraft only steps up
	 * 0.6 m on landing. In the air, pushing into a ledge whose top is at most {@link #LEDGE_REACH} above the feet lifts
	 * the hero onto it, from any direction (running, strafing, backing up), as a 1-block step should allow, and over its
	 * edge. True when it did.
	 */
	private static boolean ledge(LivingEntity entity, Vec3 wish) {
		Vec3 push = wish.normalize().scale(0.1);
		AABB box = entity.getBoundingBox();
		if (entity.level().noCollision(entity, box.move(push.x, 0, push.z))) {
			return false; // brushing along a wall, not pushing into a ledge
		}
		for (double lift = 0.05; lift <= LEDGE_REACH + 1.0E-6; lift += 0.05) {
			AABB raised = box.move(0, lift, 0);
			if (entity.level().noCollision(entity, raised) && entity.level().noCollision(entity, raised.move(push.x, 0, push.z))) {
				entity.setPos(entity.getX() + push.x, entity.getY() + lift, entity.getZ() + push.z);
				return true;
			}
		}
		return false;
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

	/**
	 * Lúcio's Wall Ride (tools/reference/lucio.md, tools/reference/lucio-movement-research.md; the owner's playtest,
	 * 2026-10-27).
	 * <ul>
	 * <li>Getting on: in the air, holding jump, beside a wall that covers his body up to his head. A knee-high step
	 * doesn't count, so jumping up a block with jump held lands on it (the old rule took any block within reach and he
	 * grabbed every step he jumped up).</li>
	 * <li>On it: no gravity, rising slowly ({@link dev.overcraft.lucio.LucioStats#WALL_RISE}), at +30% (7.15 m/s, past the
	 * buff cap). W rides the way the camera faces along the wall, S the other way, neither keeps going: so he can ride
	 * backwards, and turn round mid-ride by turning the camera (the owner). Faster than that (a chain of wall jumps) the
	 * speed is kept, easing toward the ride speed as his air speed does.</li>
	 * <li>Corners: a wall ahead (an inside corner) turns him onto it; the wall's end with a face round it (an outside
	 * corner, a pillar) wraps him round onto that face. A wall that just ends lets him fly on with his speed.</li>
	 * <li>Off: letting go of jump leaps off (+2.5 m/s along the ride, 6.54 m/s up looking ahead, 6.9 holding forward,
	 * 9.12 looking up, 4.12 down, and a push off the wall: ours, 2 m/s); crouch drops off. 0.5 s before the next ride.
	 * In the air his own mover keeps that speed (Mover.LUCIO).</li>
	 * </ul>
	 * True while it moves him this tick.
	 */
	private static boolean ride(LivingEntity entity, Vec3 v, Vec3 input) {
		if (!wallRide.test(entity)) {
			return false;
		}
		Ride ride = RIDES.computeIfAbsent(entity, e -> new Ride());
		if (ride.cooldown > 0) {
			ride.cooldown--;
		}
		boolean jump = jumpKeyHeld.test(entity);
		if (entity.onGround() || entity.isInWater()) {
			ride.riding = false;
			ride.jumpWindow = 0;
			return false;
		}
		// the wall bonus adds to his song's: 5.5 x (1 + 0.25 + 0.30) = 8.53 m/s with Speed Boost (the wiki; Lucio Surf's
		// workshop code computes 8.53), past the +75% cap
		double rideSpeed = RUN_SPEED * (Math.max(1, speedFactor(entity)) + dev.overcraft.lucio.LucioStats.WALL_SPEED);
		if (!ride.riding) {
			if (ride.jumpWindow > 0) {
				// off the wall's end with jump still held: letting go within 0.2 s still wall-jumps
				ride.jumpWindow--;
				if (!jump) {
					ride.jumpWindow = 0;
					wallJump(entity, ride, input);
					return leave(entity);
				}
			}
			if (!jump || ride.cooldown > 0) {
				return false;
			}
			Vec3 flat = new Vec3(v.x, 0, v.z).scale(20);
			Vec3 normal = wallToRide(entity, flat);
			if (normal == null) {
				return false;
			}
			if (ride.lastNormal != null && ride.leftAt != null && normal.equals(ride.lastNormal)
				&& entity.position().distanceTo(ride.leftAt) < SAME_WALL_GAP) {
				// the same wall needs about 2 m from where he left it (the Wallriding Academy; Blizzard 2018-05-03)
				return false;
			}
			ride.riding = true;
			ride.ticks = 0;
			ride.jumpWindow = 0;
			ride.startY = entity.getY();
			ride.normal = normal;
			ride.axis = new Vec3(-normal.z, 0, normal.x);
			// his speed along the wall carries on (Blizzard 2018: no loss landing on a wall at an angle), eased toward the
			// wall speed below; the way comes from W / S and the camera, else from his motion
			double along = flat.dot(ride.axis);
			int way = steer(entity, ride, input);
			if (way == 0) {
				way = Math.abs(along) > 0.5 ? (int) Math.signum(along) : cameraWay(entity, ride);
			}
			if (way == 0) {
				way = 1;
			}
			ride.speed = way * Math.abs(along);
		}
		ride.ticks++;
		if (entity.isCrouching()) {
			// drop off: no jump, his speed along the wall carries on
			Vec3 off = ride.heading().scale(Math.abs(ride.speed) / 20);
			entity.setDeltaMovement(off.x, 0, off.z);
			endRide(ride, entity);
			return leave(entity);
		}
		if (!jump) {
			wallJump(entity, ride, input);
			// moved by the leap this tick: the air step after would start from the speed he had on the wall (until
			// 2026-10-27 it did, and his wall jumps fell straight down off the wall)
			return leave(entity);
		}
		// speed along the wall: toward the wall speed both ways, not at once (the Wallriding Academy: a one-tick touch keeps
		// nearly all of a chain's speed, and doesn't bring a walk up to wall speed), never above max(entry, wall speed) on a
		// straight wall (Lucio Surf's code). The rates aren't published: ours, his mover's air acceleration up (about
		// 25 m/s²) and its friction down (2/s, a 0.35 s half-life). A reversal (W / S, the camera) brakes and comes back
		// at the same rate up.
		int way = steer(entity, ride, input);
		int going = ride.speed == 0 ? (way != 0 ? way : 1) : (int) Math.signum(ride.speed);
		int wanted = way != 0 ? way : going;
		double dt = 0.05;
		double up = WALL_EASE_UP * rideSpeed * dt;
		if (wanted != going) {
			// turning round: braking, then the other way
			ride.speed -= going * Math.min(Math.abs(ride.speed) + up, 2 * up);
		} else if (Math.abs(ride.speed) < rideSpeed) {
			ride.speed = going * Math.min(rideSpeed, Math.abs(ride.speed) + up);
		} else {
			double over = Math.abs(ride.speed) - rideSpeed;
			ride.speed = going * (rideSpeed + over * Math.max(0, 1 - Mover.LUCIO.friction() * dt));
		}

		// he rises gradually, more looking up, less (or sinking) looking down (players; Blizzard 2018 stopped him riding
		// straight up): no source measures it, so the rates are ours, for the owner to correct
		double pitch = Math.clamp(-entity.getXRot() / 90.0, -1, 1);
		double rise = Ow.speed(dev.overcraft.lucio.LucioStats.WALL_RISE + dev.overcraft.lucio.LucioStats.WALL_RISE_AIM * pitch);
		Vec3 move = ride.heading().scale(Math.abs(ride.speed) / 20).subtract(ride.normal.scale(0.01));
		entity.setDeltaMovement(move.x, rise, move.z);
		entity.move(MoverType.SELF, entity.getDeltaMovement());
		entity.resetFallDistance();

		if (blockedAhead(entity, ride.heading())) {
			// an inside corner: the wall ahead becomes the wall, and he rides away from the old one
			Vec3 old = ride.normal;
			ride.normal = ride.heading().scale(-1);
			ride.axis = old;
			ride.speed = Math.abs(ride.speed);
		} else if (!touches(entity, ride.normal, 0.5, 1.0)) {
			// past the wall's end: round the corner if there's a face there, else fly on
			Vec3 round = ride.heading();
			Vec3 newHeading = ride.normal.scale(-1);
			if (cornerFace(entity, round, newHeading)) {
				ride.normal = round;
				ride.axis = newHeading;
				ride.speed = Math.abs(ride.speed);
			} else {
				Vec3 off = ride.heading().scale(Math.abs(ride.speed) / 20);
				entity.setDeltaMovement(off.x, rise, off.z);
				endRide(ride, entity);
				// off the end with jump held: letting go within 0.2 s still jumps (Blizzard 2020-11-17)
				ride.jumpWindow = Ow.ticks(WALL_JUMP_WINDOW);
				return true;
			}
		}
		Vec3 keep = ride.heading().scale(Math.abs(ride.speed) / 20);
		entity.setDeltaMovement(keep.x, entity.verticalCollision ? 0 : rise, keep.z);
		return true;
	}

	/**
	 * Jetpack Cat's flight (her jetpack, a held mover override in her data's graph 59DC: air acceleration 9.5, speed
	 * above 12 m/s decays, drag 8 a second with no input; the "fly" flag: no gravity). W follows her aim (look up to
	 * climb), S backs off along it (4.95 m/s), A / D strafe, Space rises and Crouch sinks (her data: -1 / 0 / +1; the
	 * vertical speed isn't a named value: ours, her run speed). Frenetic Flight (graph 59E0): its speed bonus ramps from
	 * +25% to +65% (+5% every 0.04 s), its first 0.3 s on her burst mover (air acceleration 2, drag 2), then the sustain
	 * (6 m/s, air acceleration 6, drag 1.25), pushed along the input at 25 m/s² (her data) up to 17.8 m/s (the wiki's
	 * measured top speed: ours to combine them). True when it moved her.
	 */
	private static boolean fly(LivingEntity entity, Vec3 input) {
		if (!flight.test(entity)) {
			return false;
		}
		entity.resetFallDistance();
		if (driven.test(entity)) {
			// Catnapper: the server sets her velocity each tick
			entity.move(MoverType.SELF, entity.getDeltaMovement());
			return true;
		}
		double factor = speedFactor(entity);
		Vec3 look = entity.getLookAngle();
		double yaw = Math.toRadians(entity.getYRot());
		double fwd = Math.signum(input.z);
		double strafe = Math.signum(input.x);
		double vert = (jumpKeyHeld.test(entity) ? 1 : 0) - (crouchHeld.test(entity) ? 1 : 0);
		Vec3 dir = look.scale(fwd).add(new Vec3(Math.cos(yaw), 0, Math.sin(yaw)).scale(strafe)).add(0, vert, 0);
		boolean wants = dir.lengthSqr() > 1.0E-6;
		Vec3 wishDir = wants ? dir.normalize() : Vec3.ZERO;
		boolean valk = valkyrie.test(entity);
		int boost = valk ? -1 : boosting.applyAsInt(entity);
		double t = Math.max(0, boost) * 0.05;
		double wishSpeed;
		double accel;
		double drag;
		double max;
		boolean free = valk && undamped.test(entity);
		if (valk) {
			// Valkyrie (her graph 10DA's mover: forward, back and strafe 5.5, x1.636 = 9 m/s every way, air acceleration
			// 8, air drag 0.35 with no input, above 9 m/s a 2/s decay); in the 0.5 s after a launch her speed is
			// x2.4444 (22 m/s) and nothing damps it (006E #477)
			wishSpeed = dev.overcraft.mercy.MercyStats.VALKYRIE_SPEED * factor
				* (free ? 1 + dev.overcraft.mercy.MercyStats.GA_LAUNCH_SPEED_VALKYRIE : 1);
			accel = dev.overcraft.mercy.MercyStats.VALKYRIE_AIR_ACCEL;
			drag = free ? 0 : dev.overcraft.mercy.MercyStats.VALKYRIE_DRAG;
			max = free ? Double.MAX_VALUE : wishSpeed;
		} else if (boost >= 0) {
			double bonus = Math.min(0.65, 0.25 + 0.05 * Math.floor(t / 0.04));
			wishSpeed = (fwd < 0 ? 5.5 : 6.0) * factor * (1 + bonus);
			boolean burst = t < 0.3;
			accel = burst ? 2 : 6;
			drag = burst ? 2 : 1.25;
			max = dev.overcraft.jetpackcat.JetpackCatStats.BOOST_SPEED * factor;
		} else {
			wishSpeed = (fwd < 0 ? 4.95 : 5.5) * factor;
			accel = dev.overcraft.jetpackcat.JetpackCatStats.FLY_ACCELERATE;
			drag = dev.overcraft.jetpackcat.JetpackCatStats.FLY_DRAG;
			max = dev.overcraft.jetpackcat.JetpackCatStats.FLY_MAX * factor;
		}
		Vec3 v = entity.getDeltaMovement().scale(20);
		Vec3 travelled = Vec3.ZERO;
		double dt = 0.05 / SUBSTEPS;
		for (int i = 0; i < SUBSTEPS; i++) {
			if (valk && wants) {
				// toward the wish at her acceleration times her speed, a second (Overwatch's rate); with none, the drag below
				v = approach(v, wishDir.scale(wishSpeed), accel * wishSpeed * dt);
			} else if (wants) {
				v = accelerate(v, wishDir.scale(wishSpeed), accel, dt);
			} else {
				v = v.scale(Math.max(0, 1 - drag * dt));
			}
			if (boost >= 0) {
				// the boost's own push, along the input (else her motion, else her aim)
				Vec3 push = wants ? wishDir : v.lengthSqr() > 1.0E-4 ? v.normalize() : look;
				v = v.add(push.scale(BOOST_PUSH * dt));
			}
			double speed = v.length();
			if (speed > max && boost >= 0) {
				// boosting, its push stops at the top speed (a soft cap let it run on to 29.5 m/s: measured playing)
				v = v.scale(max / speed);
			} else if (speed > max) {
				v = v.scale((max + (speed - max) * Math.max(0, 1 - AIR_DECAY * dt)) / speed);
			}
			travelled = travelled.add(v);
		}
		Vec3 step = travelled.scale(1.0 / SUBSTEPS / 20);
		entity.setDeltaMovement(step);
		entity.move(MoverType.SELF, step);
		Vec3 after = entity.getDeltaMovement();
		Vec3 end = v.scale(1.0 / 20);
		entity.setDeltaMovement(after.x == 0 ? 0 : end.x, after.y == 0 ? 0 : end.y, after.z == 0 ? 0 : end.z);
		return true;
	}

	/**
	 * Mercy's Guardian Angel (her graph 006E: a StateMoveTo): her position (feet) straight to the point beside the ally at
	 * 17 m/s (25 in Valkyrie), no gravity, the walls stopping her as any move. Within the arrival distance she holds there
	 * until the server ends it, her speed spent (ours: the graph sets no velocity at the end, and what the engine's move
	 * leaves isn't in the data; stopping keeps the last tick's part-step from flinging her on). True when it moved her.
	 */
	private static boolean guardianAngel(LivingEntity entity) {
		Glide g = glide.apply(entity);
		if (g == null) {
			return false;
		}
		entity.resetFallDistance();
		Vec3 to = g.to().subtract(entity.position());
		double dist = to.length();
		Vec3 step = dist <= g.arrive() ? Vec3.ZERO : to.scale(Math.min(Ow.speed(g.speed()), dist - g.arrive()) / dist);
		entity.setDeltaMovement(step);
		entity.move(MoverType.SELF, step);
		if (dist - g.arrive() <= Ow.speed(g.speed())) {
			entity.setDeltaMovement(Vec3.ZERO);
		}
		return true;
	}

	/** Frenetic Flight's push along the input, m/s² (her data's graph 59E0: 25). */
	private static final double BOOST_PUSH = 25;

	/** Leaving the wall: this tick's move at the velocity just set, which the next ticks' air steps carry on. */
	private static boolean leave(LivingEntity entity) {
		Vec3 v = entity.getDeltaMovement();
		entity.move(MoverType.SELF, v);
		Vec3 after = entity.getDeltaMovement();
		entity.setDeltaMovement(after.x == 0 ? 0 : v.x, after.y == 0 ? 0 : v.y, after.z == 0 ? 0 : v.z);
		return true;
	}

	/** Which way W / S ride him: +1 along the axis, -1 against it, 0 with neither or the camera square on the wall. */
	private static int steer(LivingEntity entity, Ride ride, Vec3 input) {
		if (Math.abs(input.z) < 1.0E-3) {
			return 0;
		}
		int camera = cameraWay(entity, ride);
		return camera == 0 ? 0 : input.z > 0 ? camera : -camera;
	}

	/** Which way along the wall the camera faces: +1, -1, or 0 looking square at it (or square away from it). */
	private static int cameraWay(LivingEntity entity, Ride ride) {
		Vec3 look = entity.getLookAngle();
		double along = look.x * ride.axis.x + look.z * ride.axis.z;
		return Math.abs(along) < 0.1 ? 0 : (int) Math.signum(along);
	}

	/**
	 * The leap off, as his wall-ride graph (0263, the velocity actions #280 / #285 / #290: research\lucio\
	 * wallride-data.md) does it: a jump 1.15 times his own (5.72 -> 6.58 m/s up) plus 2.5 m/s (game value 212A) along
	 * where he looks with forward held (in 3D: looking up it's 9.08 up, looking down 4.08: the wiki measured 9.12 and
	 * 4.12), along the input's direction with another key, along his motion with none (the graph's third vector,
	 * unidentified: ours). His speed along the wall carries on; nothing pushes him off it.
	 */
	private static void wallJump(LivingEntity entity, Ride ride, Vec3 input) {
		Vec3 dir;
		Vec3 flat = new Vec3(input.x, 0, input.z);
		if (input.z > 0) {
			dir = entity.getLookAngle().normalize();
		} else if (flat.lengthSqr() > 1.0E-7) {
			// the input in the world, as travel() turns it
			Vec3 in = flat.normalize();
			double sin = Math.sin(Math.toRadians(entity.getYRot()));
			double cos = Math.cos(Math.toRadians(entity.getYRot()));
			dir = new Vec3(in.x * cos - in.z * sin, 0, in.z * cos + in.x * sin);
		} else {
			dir = ride.heading();
		}
		Vec3 ridden = ride.heading().scale(Math.abs(ride.speed));
		Vec3 out = ridden.add(dir.scale(dev.overcraft.lucio.LucioStats.WALL_LEAP_FORWARD));
		double up = JUMP_SPEED * dev.overcraft.lucio.LucioStats.WALL_JUMP_SCALE + dir.y * dev.overcraft.lucio.LucioStats.WALL_LEAP_FORWARD;
		entity.setDeltaMovement(Ow.speed(out.x), Ow.speed(up), Ow.speed(out.z));
		endRide(ride, entity);
	}

	private static void endRide(Ride ride, LivingEntity entity) {
		ride.riding = false;
		ride.cooldown = Ow.ticks(dev.overcraft.lucio.LucioStats.WALL_COOLDOWN);
		ride.lastNormal = ride.normal;
		ride.leftAt = entity.position();
	}

	/** About 2 m between leaving a wall and riding the same one again (the Wallriding Academy). */
	private static final double SAME_WALL_GAP = 2;
	/** Letting go of jump this long after leaving a wall still wall-jumps (Blizzard 2020-11-17: 0.2 s). */
	private static final double WALL_JUMP_WINDOW = 0.2;
	/** How fast his speed rises to wall speed on a wall, times the wall speed per second (ours: his air acceleration). */
	private static final double WALL_EASE_UP = 3.5;

	/**
	 * The outward normal of a wall he can get on: within reach beside him and covering his body from the waist to his
	 * head (a step at his knees isn't one), the one most along his run (a wall he skims, rather than one he jumps
	 * square into, wins). Null when there's none.
	 */
	private static @Nullable Vec3 wallToRide(LivingEntity entity, Vec3 run) {
		Vec3 best = null;
		double bestAcross = Double.MAX_VALUE;
		for (Vec3 normal : new Vec3[] {new Vec3(1, 0, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 0, -1)}) {
			if (!touches(entity, normal, 0.5, 1.0) || !touches(entity, normal, 1.2, 1.7)) {
				continue;
			}
			double across = run.lengthSqr() < 1.0E-6 ? 0 : Math.abs(run.normalize().dot(normal));
			if (across < bestAcross) {
				best = normal;
				bestAcross = across;
			}
		}
		return best;
	}

	/** A block within reach on the side opposite {@code normal}, between these heights above his feet (m). */
	private static boolean touches(LivingEntity entity, Vec3 normal, double from, double to) {
		double reach = dev.overcraft.lucio.LucioStats.WALL_REACH;
		AABB box = entity.getBoundingBox();
		AABB band = new AABB(box.minX, box.minY + from, box.minZ, box.maxX, Math.min(box.maxY, box.minY + to), box.maxZ);
		return !entity.level().noCollision(entity, band.move(-normal.x * reach, 0, -normal.z * reach));
	}

	/** Any part of his body: kept for the climb and the old callers. */
	private static boolean touches(LivingEntity entity, Vec3 normal) {
		return touches(entity, normal, 0, entity.getBbHeight());
	}

	/** A wall straight ahead of him at his waist (an inside corner). */
	private static boolean blockedAhead(LivingEntity entity, Vec3 heading) {
		AABB box = entity.getBoundingBox();
		AABB band = new AABB(box.minX, box.minY + 0.5, box.minZ, box.maxX, box.minY + 1.0, box.maxZ);
		return !entity.level().noCollision(entity, band.move(heading.x * 0.1, 0, heading.z * 0.1));
	}

	/**
	 * Past a wall's end: whether its last block has a face round the corner to ride on next (heading the new way, that
	 * face behind him on the side the ride came from). Probed a step on along the new heading, so he wraps round.
	 */
	private static boolean cornerFace(LivingEntity entity, Vec3 faceNormal, Vec3 newHeading) {
		double reach = dev.overcraft.lucio.LucioStats.WALL_REACH;
		AABB box = entity.getBoundingBox();
		AABB band = new AABB(box.minX, box.minY + 0.5, box.minZ, box.maxX, box.minY + 1.0, box.maxZ)
			.move(newHeading.x * box.getXsize(), 0, newHeading.z * box.getXsize());
		return entity.level().noCollision(entity, band)
			&& !entity.level().noCollision(entity, band.move(-faceNormal.x * reach, 0, -faceNormal.z * reach));
	}

	/** Riding a wall right now (the local player only), and on which side: 1 the wall on his right, -1 his left. */
	public static int wallRiding(LivingEntity entity) {
		Ride ride = RIDES.get(entity);
		if (ride == null || !ride.riding) {
			return 0;
		}
		// the wall is on the side the normal points away from: right if (heading x up) points into it
		Vec3 heading = ride.heading();
		double cross = heading.z * ride.normal.x - heading.x * ride.normal.z;
		return cross > 0 ? 1 : -1;
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
