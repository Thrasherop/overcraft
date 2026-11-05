package dev.overcraft.hero;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A zombie at the origin: 0.6 m wide, 1.95 m tall, eyes at 1.74 m, so its head runs from 1.49 m up (#110). */
class HeadshotTest {
	private static boolean shot(Vec3 from, Vec3 aimedAt) {
		Vec3 dir = aimedAt.subtract(from);
		// where it first touches the box inflated by the shot's 0.11 m, as a weapon's hit point is
		var entry = new AABB(-0.41, -0.11, -0.41, 0.41, 2.06, 0.41).clip(from, from.add(dir.scale(2)));
		return Headshot.crossed(0, 0, 0, 0.6, 1.95, 1.74, entry.orElseThrow(), dir);
	}

	@Test
	void levelShots() {
		Vec3 eye = new Vec3(0, 1.62, -10);
		assertFalse(shot(eye, new Vec3(0, 0.3, 0)), "legs");
		assertFalse(shot(eye, new Vec3(0, 1.4, 0)), "just under the head");
		assertTrue(shot(eye, new Vec3(0, 1.55, 0)), "just inside the head");
		assertTrue(shot(eye, new Vec3(0, 2.0, 0)), "the crown");
	}

	@Test
	void theTwoZombieLineAtALowPitch() {
		// the issue's steps: the ray goes on from a first zombie's chest to a second's legs 12.5 m behind it
		Vec3 eye = new Vec3(0, 1.62, -25);
		assertFalse(shot(eye, new Vec3(0, 0.58, 0)));
	}

	@Test
	void steeplyDownFromAPillarOnlyTheHeadCounts() {
		// 10 m up, 2.5 m out: the shot played in game
		Vec3 eye = new Vec3(0, 11.62, -2.5);
		assertFalse(shot(eye, new Vec3(0, 0.3, 0)), "aimed at the legs: enters the box through its top, misses the head");
		assertTrue(shot(eye, new Vec3(0, 1.6, 0)), "aimed at the head");
		assertTrue(shot(eye, new Vec3(0, 1.0, 0)), "aimed at the waist: higher up, the line is through the head");
	}

	@Test
	void straightDownIsTheHead() {
		assertTrue(shot(new Vec3(0, 20, 0), new Vec3(0, 0, 0)));
	}

	@Test
	void pastTheSideOfTheHeadIsNotIt() {
		assertFalse(shot(new Vec3(0.38, 1.7, -10), new Vec3(0.38, 1.7, 0)));
	}

	@Test
	void fromBelowTheLineStillHasToReachTheHead() {
		Vec3 eye = new Vec3(0, -6, -5);
		assertTrue(shot(eye, new Vec3(0, 1.7, 0)));
		assertFalse(shot(eye, new Vec3(0, 0.6, 0)));
	}
}
