package dev.overcraft.hero;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mobs at the origin, facing +z (yaw 0), with vanilla sizes (#110, the critique's 11). */
class HeadshotTest {
	private record Mob(Headshot.Plan plan, double width, double height, double eye) {
		AABB box(double r) {
			return new AABB(-width / 2 - r, -r, -width / 2 - r, width / 2 + r, height + r, width / 2 + r);
		}

		/** A shot from {@code from} aimed at {@code aim}; it must touch the box inflated by r, as a weapon's hit point does. */
		boolean head(Vec3 from, Vec3 aim, double r) {
			Vec3 dir = aim.subtract(from);
			Vec3 entry = box(r).clip(from, from.add(dir.scale(2))).orElseThrow();
			return Headshot.crossed(Headshot.head(plan, 0, 0, 0, width, height, eye, 0), width, entry, dir, r);
		}
	}

	private static final Mob ZOMBIE = new Mob(Headshot.Plan.UPRIGHT, 0.6, 1.95, 1.74);
	private static final Mob SPIDER = new Mob(Headshot.Plan.FRONT, 1.4, 0.9, 0.65);
	private static final Mob COW = new Mob(Headshot.Plan.FRONT, 0.9, 1.4, 1.3);
	private static final Mob GOLEM = new Mob(Headshot.Plan.BROAD, 1.4, 2.7, 2.29);
	private static final Mob GHAST = new Mob(Headshot.Plan.FRONT, 4, 4, 2.6);

	@Test
	void zombieLevelShots() {
		Vec3 eye = new Vec3(0, 1.62, -10);
		assertFalse(ZOMBIE.head(eye, new Vec3(0, 0.3, 0), 0.02), "legs");
		assertFalse(ZOMBIE.head(eye, new Vec3(0, 1.4, 0), 0.02), "just under the head");
		assertTrue(ZOMBIE.head(eye, new Vec3(0, 1.55, 0), 0.02), "just inside the head");
		assertTrue(ZOMBIE.head(eye, new Vec3(0, 1.9, 0), 0.02), "the crown");
	}

	@Test
	void theTwoZombieLineAtALowPitch() {
		// the issue's steps: the ray goes on from a first zombie's chest to a second's legs 12.5 m behind it
		assertFalse(ZOMBIE.head(new Vec3(0, 1.62, -25), new Vec3(0, 0.58, 0), 0.11));
	}

	@Test
	void steeplyDownFromAPillar() {
		// 10 m up, 2.5 m out: the shot played in game. A thin shot aimed at the legs misses the head; the waist's line goes through it
		Vec3 eye = new Vec3(0, 11.62, -2.5);
		assertFalse(ZOMBIE.head(eye, new Vec3(0, 0.3, 0), 0.02));
		assertFalse(ZOMBIE.head(eye, new Vec3(0, 0.3, 0), 0.11), "thick too: its thickness adds nothing along its way");
		assertTrue(ZOMBIE.head(eye, new Vec3(0, 1.0, 0), 0.02));
		assertTrue(ZOMBIE.head(eye, new Vec3(0, 1.6, 0), 0.02));
		// 45 degrees: legs and waist are the body, thick or thin
		Vec3 eye45 = new Vec3(0, 6.3, -6);
		assertFalse(ZOMBIE.head(eye45, new Vec3(0, 0.3, 0), 0.11));
		assertFalse(ZOMBIE.head(eye45, new Vec3(0, 0.9, 0), 0.11));
	}

	@Test
	void straightDownIsTheHead() {
		assertTrue(ZOMBIE.head(new Vec3(0, 20, 0), new Vec3(0, 0, 0), 0.02));
	}

	@Test
	void theShotsThicknessCountsOnTheSides() {
		Vec3 from = new Vec3(0.3, 1.7, -10), aim = new Vec3(0.3, 1.7, 0);
		assertFalse(ZOMBIE.head(from, aim, 0.02), "a thin shot just past the side of its head");
		assertTrue(ZOMBIE.head(from, aim, 0.175), "a Railgun round's edge meets it");
		assertFalse(COW.head(new Vec3(6, 0.9, 0), new Vec3(0, 0.9, 0), 0.195), "a kunai at a cow's flank");
	}

	@Test
	void spiderHeadIsItsHeadFromAnySide() {
		assertTrue(SPIDER.head(new Vec3(0, 0.6, 6), new Vec3(0, 0.5, 0.6), 0.1), "from the front");
		assertTrue(SPIDER.head(new Vec3(6, 0.6, 0.55), new Vec3(0, 0.6, 0.55), 0.1), "from its side, at the front");
		assertFalse(SPIDER.head(new Vec3(6, 0.6, -0.4), new Vec3(0, 0.6, -0.4), 0.1), "abdomen from its side");
		assertFalse(SPIDER.head(new Vec3(0, 3, -4), new Vec3(0, 0.85, -0.3), 0.1), "abdomen's top from above and behind");
	}

	@Test
	void cowHeadAtTheFront() {
		assertTrue(COW.head(new Vec3(0, 1.3, 6), new Vec3(0, 1.25, 0.45), 0.1), "from the front");
		assertFalse(COW.head(new Vec3(6, 0.9, -0.2), new Vec3(0, 0.9, -0.2), 0.1), "flank");
	}

	@Test
	void ironGolemShouldersAreNotItsHead() {
		assertFalse(GOLEM.head(new Vec3(0.6, 2.2, -6), new Vec3(0.6, 2.2, 0), 0.1), "shoulder");
		assertTrue(GOLEM.head(new Vec3(0, 2.4, -6), new Vec3(0, 2.4, 0), 0.1), "head");
		assertFalse(GOLEM.head(new Vec3(0, 1.62, -6), new Vec3(0, 1.3, 0), 0.1), "chest");
	}

	@Test
	void ghastHasAFaceNotHalfItsBody() {
		assertTrue(GHAST.head(new Vec3(0, 2.6, 12), new Vec3(0, 2.6, 2), 0.1), "its face");
		assertFalse(GHAST.head(new Vec3(12, 3, 0), new Vec3(0, 3, 0), 0.1), "its side");
		assertFalse(GHAST.head(new Vec3(0, 0.8, 12), new Vec3(0, 0.8, 2), 0.1), "low on its front");
	}
}
