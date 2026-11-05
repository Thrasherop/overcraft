package dev.overcraft.jetpackcat;

import dev.overcraft.hero.OwMovement;
import net.minecraft.world.phys.Vec3;

/**
 * Lifeline's rope, as it acts on the one it tows (#89). Her graph runs it on the ally, not on her (5A8F #1137, in the ally's
 * context through #963): she isn't slowed or pulled by what she tows. So the ally keeps their own mover, their gravity and
 * their keys, and the rope is one more force on them, and only while it's stretched past its length: that's what makes them
 * hang, trail, swing and lift. Velocities in m/s, positions in m.
 *
 * <p>Her data names the node's numbers but not what they mean (`E:\Overwatch Archive\research\jetpack_cat\gaps.md`, "What the
 * rope node's 12 numbers mean"), so the reading here is M: see {@code JetpackCatStats.ROPE_*}. No recording has an ally on it.
 */
public final class LifelineRope {
	private LifelineRope() {
	}

	/** One step of the rope on an ally {@code toHer} away from her (ally to her), moving at {@code ally}, she at {@code her}. */
	public static Vec3 pull(Vec3 toHer, Vec3 her, Vec3 ally, double length, double dt) {
		double dist = toHer.length();
		double stretch = dist - length;
		if (stretch <= 0 || dist < 1.0E-6) {
			return ally; // slack: a rope doesn't push
		}
		Vec3 along = toHer.scale(1 / dist);
		Vec3 relative = her.subtract(ally);
		double separating = relative.dot(along);
		double add = (JetpackCatStats.ROPE_STIFFNESS * stretch + JetpackCatStats.ROPE_DAMPING * separating) * dt;
		// it reels them in no faster than this: from 13 m under her the spring alone threw a villager past her at 50 m/s
		add = Math.min(add, Math.max(0, JetpackCatStats.ROPE_REEL_SPEED + separating));
		Vec3 pulled = ally.add(along.scale(Math.max(0, add)));
		// the swing dies away, slowly: only what's across the rope
		Vec3 across = relative.subtract(along.scale(separating));
		return pulled.add(across.scale(Math.min(1, JetpackCatStats.ROPE_SWING_DAMPING * dt)));
	}

	/** Whether the rope is pulling at this distance. */
	public static boolean taut(Vec3 toHer, double length) {
		return toHer.lengthSqr() > length * length;
	}

	/**
	 * One step of an ally with no keys in the air, on Overwatch's default mover, as a hero's own movement does it (OwMovement):
	 * gravity, and the decay of air speed above the run speed. That decay is what makes them trail her: at her 7.15 m/s it's
	 * 3.3 m/s², the rope leans about 11° to pay for it; at 17.8 m/s 24.6 m/s², about 55°.
	 */
	public static Vec3 free(Vec3 v, double dt) {
		Vec3 flat = OwMovement.airStep(new Vec3(v.x, 0, v.z), Vec3.ZERO, OwMovement.RUN_SPEED, dt);
		double vy = Math.max(v.y - OwMovement.gravity(v.y) * dt, -OwMovement.TERMINAL_SPEED);
		return new Vec3(flat.x, vy, flat.z);
	}
}
