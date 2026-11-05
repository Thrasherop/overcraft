package dev.overcraft.hero;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Whether a shot crossed a creature's head (#110): every hero's weapon asks here. All of it is ours: Overwatch has no mobs. What
 * counts is the shot, as thick as it is, crossing the head, not where it first touched the box: fired steeply down, a shot aimed at a
 * zombie's legs enters its box through the top face and was called a headshot. The head comes from the mob's own numbers: its eye height
 * and the way it faces (Minecraft has no head position on the server) and its body plan:
 * <ul>
 * <li><b>Upright</b> (the default: a zombie, a player, a skeleton, a creeper, an enderman, a slime): from 0.25 m under its eyes to its top,
 * 5/12 of its width each side of its middle (a zombie's head is 0.5 m wide in a 0.6 m box).</li>
 * <li><b>Broad</b> (an iron golem): the same, a quarter of its width: its shoulders aren't its head. <b>A Wither</b>: all three heads, the whole row.</li>
 * <li><b>Front-headed</b> (animals, a spider, a ravager, a zoglin, a silverfish, a phantom, a ghast's face): at the front of the box the way its head
 * faces, around its eye height, 3/10 of its width each side, so an abdomen or a flank isn't a head and the head seen from the side still is.</li>
 * </ul>
 * Dragon parts have their own rule (the head part), at each weapon.
 */
public final class Headshot {
	private Headshot() {
	}

	private static final double DEPTH = 0.25;
	private static final double THICKEST = 0.11;

	enum Plan {
		UPRIGHT, BROAD, ROW, FRONT
	}

	static Plan plan(LivingEntity target) {
		if (target instanceof Animal || target instanceof Spider || target instanceof Ravager || target instanceof Zoglin
			|| target instanceof Silverfish || target instanceof Phantom || target instanceof Ghast) {
			return Plan.FRONT;
		}
		return target instanceof IronGolem ? Plan.BROAD : target instanceof WitherBoss ? Plan.ROW : Plan.UPRIGHT;
	}

	/** {@code at}: where it struck; {@code direction}: which way it was going; {@code radius}: how thick the shot is. */
	public static boolean crossed(LivingEntity target, Vec3 at, Vec3 direction, double radius) {
		return crossed(head(plan(target), target.getX(), target.getY(), target.getZ(), target.getBbWidth(), target.getBbHeight(), target.getEyeHeight(),
			target.getYHeadRot()), target.getBbWidth(), at, direction, radius);
	}

	/** The head of a creature standing at x y z (feet). Package-private for HeadshotTest. */
	static AABB head(Plan plan, double x, double y, double z, double width, double height, double eye, float yaw) {
		double half = switch (plan) {
			case UPRIGHT -> width * 5 / 12;
			case BROAD -> width / 4;
			case ROW -> width;
			case FRONT -> width * 0.3;
		};
		if (plan != Plan.FRONT) {
			return new AABB(x - half, y + eye - DEPTH, z - half, x + half, y + height, z + half);
		}
		double yawRad = Math.toRadians(yaw);
		double cx = x - Math.sin(yawRad) * width / 2, cz = z + Math.cos(yawRad) * width / 2;
		double down = Math.max(DEPTH, half * 0.7);
		return new AABB(cx - half, y + eye - down, cz - half, cx + half, Math.min(y + height, y + eye + half), cz + half);
	}

	/**
	 * Whether the shot's way through {@code at} meets {@code head}, about where it met the body: past that, it's a body hit whose line
	 * would have gone on into the head (from above and behind, a spider's abdomen). A shot's thickness counts to its sides and over the
	 * top, not along its way, so a steep shot doesn't reach further forward for being thick. Package-private for HeadshotTest.
	 */
	static boolean crossed(AABB head, double width, Vec3 at, Vec3 direction, double shotRadius) {
		// (at most the thickest hitscan's 0.11 m: a kunai's 0.195 m contact is a body's, and it had a cow's flank a crit, found playing)
		double radius = Math.min(shotRadius, THICKEST);
		double length = direction.length();
		if (length < 1.0E-4) {
			return head.inflate(radius).contains(at);
		}
		Vec3 d = direction.scale(1 / length);
		double level = Math.sqrt(d.x * d.x + d.z * d.z);
		double sideX = level < 1.0E-4 ? radius : radius * Math.abs(d.z) / level, sideZ = level < 1.0E-4 ? radius : radius * Math.abs(d.x) / level;
		AABB fat = new AABB(head.minX - sideX, head.minY - radius * level, head.minZ - sideZ, head.maxX + sideX, head.maxY + radius, head.maxZ + sideZ);
		if (fat.contains(at)) {
			return true;
		}
		Vec3 way = d.scale(width + head.getYsize() + 4);
		var met = fat.clip(at.subtract(way), at.add(way));
		if (met.isEmpty()) {
			return false;
		}
		double margin = Math.max(0.3, width / 2 - head.getXsize() / 2 + 0.05) + shotRadius;
		return met.get().subtract(at).dot(d) <= margin;
	}
}
