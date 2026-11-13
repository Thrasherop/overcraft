package dev.overcraft.tracer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Where a Blink takes her (her own client works it out and moves her: ClientTracer). The data's blink node (0024 #34
 * STU_A807207A) leaves the path to the engine: its target is set by no graph (D, kit_tracer_move.md). What is data: 7.5 m
 * (1AED, H) and a field of 1.5 on that node (m_497165F8, H value, L meaning: read as the height it may step up). What the
 * Workshop saw (WS, tracer-map.md): level in the air; on the ground +1.00 up a step, +1.39 up a ledge, -1.40 down a slope. What
 * the public text says: she goes "around and over obstacles" (W quoting BZ 2020-04-14). The rule here is ours, built on those:
 * her box swept 7.5 m level along {@code dir} in 0.1 m steps so she never ends inside a block; at each step, what's in her way
 * she steps up over by up to BLINK_STEP above where she is <i>at that step</i> (so she follows stairs and ramps up as she follows
 * them down), with room overhead where she is; else she slides along it (the part of the step along the wall, block axes); else
 * she stops there. On the ground she follows it down by up to BLINK_STEP a step; off a deeper ledge she goes on level (the air).
 * A Blink straight into a wall goes nowhere and still takes its charge (as Overwatch's: the press is the cost). Runs on either side.
 */
public final class Blink {
	private Blink() {
	}

	/** The step along her way (m): finer than any block a 0.6 m box could slip through. */
	static final double STEP = 0.1;
	/** How finely the climb and the drop are searched (m). */
	static final double LIFT = 0.05;
	/** A slide shorter than this a step is a stop (m): straight into a wall. */
	static final double MIN_SLIDE = 0.01;

	/** Whether a box touches no block (the level, or a test's grid). */
	public interface Space {
		boolean free(AABB box);
	}

	/** Her feet's place after a Blink from where she stands along {@code dir} (horizontal, unit) over {@code distance} m. */
	public static Vec3 destination(Level level, Entity entity, Vec3 dir, double distance) {
		return destination(box -> level.noCollision(entity, box), entity.position(), entity.getBoundingBox(), entity.onGround(), dir, distance);
	}

	/** The same, on any {@link Space}: {@code box} is her box at {@code start}. */
	public static Vec3 destination(Space space, Vec3 start, AABB box, boolean onGround, Vec3 dir, double distance) {
		Vec3 at = start;
		boolean ground = onGround;
		double gone = 0;
		while (gone < distance - 1.0E-6) {
			double len = Math.min(STEP, distance - gone);
			Vec3 move = dir.scale(len);
			Vec3 placed = step(space, box, start, at, move);
			if (placed == null) {
				// along the wall: the step's part on each block axis, the longer first (ours: Minecraft's own collision slides so)
				Vec3 a = new Vec3(move.x, 0, 0), b = new Vec3(0, 0, move.z);
				if (Math.abs(move.z) > Math.abs(move.x)) {
					Vec3 t = a;
					a = b;
					b = t;
				}
				for (Vec3 slide : new Vec3[] {a, b}) {
					if (slide.length() >= MIN_SLIDE && (placed = step(space, box, start, at, slide)) != null) {
						break;
					}
				}
			}
			if (placed == null) {
				break;
			}
			if (ground) {
				// along the ground: down to it if it's within BLINK_STEP below, else she's off a ledge and goes on level
				double drop = 0;
				while (drop < TracerStats.BLINK_STEP && free(space, box, start, placed.add(0, -(drop + LIFT), 0))) {
					drop += LIFT;
				}
				if (drop < TracerStats.BLINK_STEP) {
					placed = placed.add(0, -drop, 0);
				} else {
					ground = false;
				}
			}
			at = placed;
			gone += len;
		}
		return at;
	}

	/** One step {@code move} from {@code at}: there, or up over what's in the way by up to BLINK_STEP over {@code at}; else null. */
	private static Vec3 step(Space space, AABB box, Vec3 start, Vec3 at, Vec3 move) {
		Vec3 next = at.add(move);
		if (free(space, box, start, next)) {
			return next;
		}
		// up over it: as little as will do, never more than BLINK_STEP over where she is now, and only with room overhead here
		for (double up = LIFT; up <= TracerStats.BLINK_STEP + 1.0E-6; up += LIFT) {
			if (!free(space, box, start, at.add(0, up, 0))) {
				return null;
			}
			Vec3 raised = next.add(0, up, 0);
			if (free(space, box, start, raised)) {
				return raised;
			}
		}
		return null;
	}

	/** Her box, moved from {@code start} to {@code feet}, touches no block. */
	private static boolean free(Space space, AABB box, Vec3 start, Vec3 feet) {
		return space.free(box.move(feet.subtract(start)).deflate(1.0E-4));
	}
}
