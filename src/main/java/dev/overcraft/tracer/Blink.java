package dev.overcraft.tracer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Where a Blink takes her (her own client works it out and moves her: ClientTracer). The data's blink node (0024 #34) leaves the
 * path to the engine (its target is set by no graph, D); the rule here is the public one and the Workshop's (TracerStats
 * BLINK_*): 7.5 m level along {@code dir}, her box swept in 0.1 m steps so she never ends inside a block; what's in her way she
 * goes up over by as much as BLINK_STEP above where she started, else she stops at it; on the ground she follows it down as
 * far, and off a ledge deeper than that she goes on level (into the air). Runs on either side.
 */
public final class Blink {
	private Blink() {
	}

	/** The step along her way (m): finer than any block a 0.6 m box could slip through. */
	static final double STEP = 0.1;
	/** How finely the climb and the drop are searched (m). */
	static final double LIFT = 0.05;

	/** Her feet's place after a Blink from where she stands along {@code dir} (horizontal, unit) over {@code distance} m. */
	public static Vec3 destination(Level level, Entity entity, Vec3 dir, double distance) {
		Vec3 start = entity.position();
		AABB box = entity.getBoundingBox();
		Vec3 at = start;
		boolean ground = entity.onGround();
		double gone = 0;
		while (gone < distance - 1.0E-6) {
			double len = Math.min(STEP, distance - gone);
			Vec3 next = at.add(dir.scale(len));
			Vec3 placed = null;
			if (free(level, entity, box, start, next)) {
				placed = next;
			} else {
				// up over it: as little as will do, never above BLINK_STEP over her start, and only with room overhead where she is
				for (double up = LIFT; next.y + up - start.y <= TracerStats.BLINK_STEP + 1.0E-6; up += LIFT) {
					Vec3 raised = next.add(0, up, 0);
					if (free(level, entity, box, start, raised) && free(level, entity, box, start, at.add(0, up, 0))) {
						placed = raised;
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
				while (drop < TracerStats.BLINK_STEP && free(level, entity, box, start, placed.add(0, -(drop + LIFT), 0))) {
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

	/** Her box, moved from {@code start} to {@code feet}, touches no block. */
	private static boolean free(Level level, Entity entity, AABB box, Vec3 start, Vec3 feet) {
		return level.noCollision(entity, box.move(feet.subtract(start)).deflate(1.0E-4));
	}
}
