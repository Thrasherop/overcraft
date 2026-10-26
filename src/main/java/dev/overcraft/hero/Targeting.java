package dev.overcraft.hero;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Picking the ally a hero is aiming at (Swift Step, Healing Ofuda's lock). Runs on both sides. */
public final class Targeting {
	private Targeting() {
	}

	/**
	 * The ally closest to the crosshair inside a cone of {@code halfAngle} degrees around the view, within
	 * {@code range} metres. {@code needSight}: walls block it.
	 */
	public static @Nullable LivingEntity allyInView(LivingEntity hero, double range, double halfAngle, boolean needSight) {
		Vec3 eye = hero.getEyePosition();
		Vec3 look = hero.getViewVector(1.0f);
		double cosLimit = Math.cos(Math.toRadians(halfAngle));
		LivingEntity best = null;
		double bestCos = cosLimit;
		for (LivingEntity entity : hero.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(range))) {
			if (!Allies.isAlly(hero, entity)) {
				continue;
			}
			AABB box = entity.getBoundingBox();
			// aim at the nearest point of the body along the view: aiming at the feet or the head both count
			Vec3 center = box.getCenter();
			Vec3 aimed = eye.add(look.scale(Math.max(0, center.subtract(eye).dot(look))));
			Vec3 point = new Vec3(clamp(aimed.x, box.minX, box.maxX), clamp(aimed.y, box.minY, box.maxY), clamp(aimed.z, box.minZ, box.maxZ));
			Vec3 to = point.subtract(eye);
			double dist = to.length();
			if (dist > range || dist < 1.0E-3) {
				continue;
			}
			double cos = to.scale(1 / dist).dot(look);
			if (cos <= bestCos) {
				continue;
			}
			if (needSight && hero.level().clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, hero)).getType() != HitResult.Type.MISS) {
				continue;
			}
			best = entity;
			bestCos = cos;
		}
		return best;
	}

	private static double clamp(double v, double min, double max) {
		return v < min ? min : Math.min(v, max);
	}
}
