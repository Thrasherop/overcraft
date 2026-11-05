package dev.overcraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.overcraft.jetpackcat.LifelineRope;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Lifeline's rope on a keyless ally, stepped as the game steps it: these say the math does what the notes say, no more. */
class LifelineRopeTest {
	private static final double DT = 1 / 60.0;
	private static final double LENGTH = 4;

	/** One flight: she moves by {@code her} (a function of time), the ally by the rope and gravity. Returns ally pos, vel. */
	private interface Flight {
		Vec3 her(double t);
	}

	private static Vec3[] fly(Flight flight, Vec3 herAt, Vec3 allyAt, Vec3 v, double from, double secs) {
		double t = from;
		for (int i = 0; i < secs * 60; i++, t += DT) {
			Vec3 her = flight.her(t);
			v = LifelineRope.free(v, DT);
			v = LifelineRope.pull(herAt.subtract(allyAt), her, v, LENGTH, DT);
			allyAt = allyAt.add(v.scale(DT));
			herAt = herAt.add(her.scale(DT));
		}
		return new Vec3[] {herAt, allyAt, v};
	}

	private static final Vec3 HANG = new Vec3(0, -4.58, 0);

	@Test
	void hangsPastItsLengthByGravityOverStiffness() {
		Vec3[] end = fly(t -> Vec3.ZERO, Vec3.ZERO, HANG, Vec3.ZERO, 0, 12);
		assertEquals(4 + 17.1 / 30, end[0].distanceTo(end[1]), 0.1); // gravity is 16.7 rising, 17.5 falling
		assertEquals(0, end[2].length(), 0.3); // a bob of about 0.04 m: the two gravities
	}

	@Test
	void aSlackRopeDoesNothing() {
		Vec3 v = new Vec3(1, -3, 2);
		assertEquals(v, LifelineRope.pull(new Vec3(0, 3.9, 0), new Vec3(7, 0, 0), v, LENGTH, DT));
	}

	@Test
	void trailsBehindHerAtCruiseByTheAirDecay() {
		Vec3 her = new Vec3(0, 0, 7.15);
		Vec3[] end = fly(t -> her, Vec3.ZERO, HANG, her, 0, 25);
		double back = end[0].z - end[1].z;
		double below = end[0].y - end[1].y;
		double angle = Math.toDegrees(Math.atan2(back, below));
		// tan = 2.0 x (7.15 - 5.5) / gravity
		assertEquals(Math.toDegrees(Math.atan2(2 * 1.65, 17.0)), angle, 1.5);
		assertTrue(back > 0, "behind her, not ahead");
	}

	@Test
	void trailsFarBehindAtTopSpeedWithoutBreaking() {
		Vec3 her = new Vec3(0, 0, 17.8);
		Vec3[] end = fly(t -> her, Vec3.ZERO, HANG, Vec3.ZERO, 0, 2.5);
		double across = Math.hypot(end[0].x - end[1].x, end[0].z - end[1].z);
		assertTrue(across > 2 && across < 12, "across " + across);
	}

	@Test
	void swingsForwardWhenSheStops() {
		Vec3 cruise = new Vec3(0, 0, 7.15);
		Vec3[] flying = fly(t -> cruise, Vec3.ZERO, HANG, cruise, 0, 10);
		Vec3 at = flying[0];
		double most = -100;
		Vec3 ally = flying[1];
		Vec3 v = flying[2];
		for (int i = 0; i < 180; i++) {
			v = LifelineRope.free(v, DT);
			v = LifelineRope.pull(at.subtract(ally), Vec3.ZERO, v, LENGTH, DT);
			ally = ally.add(v.scale(DT));
			most = Math.max(most, ally.z - at.z);
		}
		assertTrue(most > 1.5, "swung past her by " + most);
		// and it dies away: hanging again, near enough, after 25 s
		Vec3[] rest = fly(t -> Vec3.ZERO, at, ally, v, 0, 25);
		assertEquals(0, rest[1].z - at.z, 0.2);
	}

	@Test
	void reelsInNoFasterThanTheCap() {
		Vec3 v = LifelineRope.pull(new Vec3(0, 13, 0), Vec3.ZERO, Vec3.ZERO, LENGTH, 0.05);
		assertTrue(v.y <= 10.001, "v " + v.y);
		// a rope never pushes, however fast it's closing
		Vec3 closing = new Vec3(0, 20, 0);
		assertEquals(20, LifelineRope.pull(new Vec3(0, 5, 0), Vec3.ZERO, closing, LENGTH, DT).y, 1.0E-6);
	}

	@Test
	void aCatchArrivesWithoutThrowingThemPastHer() {
		// 8 m under her and 3 m aside, as a villager's caught: the rope never goes far slack, and the swing it leaves is small
		Vec3 ally = new Vec3(0, -8, 3);
		Vec3 v = Vec3.ZERO;
		double shortest = 99;
		for (int i = 0; i < 360; i++) {
			v = LifelineRope.free(v, DT);
			v = LifelineRope.pull(ally.scale(-1), Vec3.ZERO, v, LENGTH, DT);
			ally = ally.add(v.scale(DT));
			shortest = Math.min(shortest, ally.length());
		}
		assertTrue(shortest > 4.0, "came to " + shortest);
	}

	@Test
	void herBlinkingDoesntThrowThem() {
		// a bunch of position updates reads as 3000 m/s for a tick: what they get is bounded by her real top speed and the reel
		Vec3 v = LifelineRope.pull(new Vec3(0, 0, 150), new Vec3(0, 0, 3000), Vec3.ZERO, LENGTH, DT);
		assertTrue(v.length() <= 23.2 + 10 + 1.0E-6, "v " + v.length());
	}

	@Test
	void liftsAnAllyOffTheGroundAsSheRises() {
		Vec3 up = new Vec3(0, 7.15, 0);
		Vec3[] end = fly(t -> up, new Vec3(0, 3.9, 0), Vec3.ZERO, Vec3.ZERO, 0, 3);
		assertTrue(end[2].y > 5, "rising at " + end[2].y);
		assertTrue(end[0].y - end[1].y < 6, "still on the rope");
	}
}
