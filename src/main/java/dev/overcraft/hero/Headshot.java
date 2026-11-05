package dev.overcraft.hero;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Whether a shot crossed a creature's head (#110): every hero's weapon asks here. The head is the top of its box from 0.25 m
 * under its eyes (Kiriko's kunai's band, ours), and narrower than the box: 5/12 of its width each side of its middle (a
 * zombie's or a player's head is 0.5 m wide in a 0.6 m box; ours for every mob). What counts is the shot's line through that
 * head, not where it first touched the box: fired steeply down, a shot aimed at a zombie's legs enters its box through the top
 * face and was called a headshot (x1.5 and Sharpshooter's refund on a Charged Shot, measured: every aim from a 10 m pillar).
 * Dragon parts have their own rule (the head part), at each weapon.
 */
public final class Headshot {
	private Headshot() {
	}

	private static final double DEPTH = 0.25;
	private static final double HALF_WIDTH_SHARE = 5.0 / 12;
	/** How far above the box a shot still grazes it: the fattest shot's own radius (Charged Shot's 0.11 m, ours for all). */
	private static final double OVER = 0.11;

	/** {@code at}: where it struck; {@code direction}: which way it was going. */
	public static boolean crossed(LivingEntity target, Vec3 at, Vec3 direction) {
		return crossed(target.getX(), target.getY(), target.getZ(), target.getBbWidth(), target.getBbHeight(), target.getEyeHeight(), at, direction);
	}

	/** The same from the creature's feet, width, height and eye height (for HeadshotTest). */
	static boolean crossed(double x, double y, double z, double width, double height, double eye, Vec3 at, Vec3 direction) {
		if (direction.lengthSqr() < 1.0E-8) {
			return at.y >= y + eye - DEPTH;
		}
		double half = width * HALF_WIDTH_SHARE;
		AABB head = new AABB(x - half, y + eye - DEPTH, z - half, x + half, y + height + OVER, z + half);
		// the whole line through the point it struck: well before it, well after
		Vec3 way = direction.normalize().scale(width + height + 1);
		return head.contains(at) || head.clip(at.subtract(way), at.add(way)).isPresent();
	}
}
