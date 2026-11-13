package dev.overcraft.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The table read off Lúcio's wall-ride lean sheets (#105): which column for an aim, and the camera's roll from the Camera bone's. */
class WallLeanTest {
	private static final float EPS = 1.0E-3f;

	@Test
	void columnsFollowTheAim() {
		assertEquals(14, HeroAnimator.wallLeanColumn(0), EPS);
		assertEquals(7, HeroAnimator.wallLeanColumn(90), EPS);
		assertEquals(21, HeroAnimator.wallLeanColumn(-90), EPS);
		// looking back along the ride: an end of the row, and the turn doesn't jump over the wrap
		assertEquals(0, HeroAnimator.wallLeanColumn(179.999f), 0.01f);
		assertEquals(28, HeroAnimator.wallLeanColumn(-179.999f), 0.01f);
		assertEquals(7, HeroAnimator.wallLeanColumn(90 + 360), EPS);
	}

	@Test
	void rollIsTheSheetsTable() {
		// the wall on his left (side -1): along it 20, square to it none, looking back the other way
		assertEquals(20, HeroAnimator.wallRoll(-1, 0, 0), EPS);
		assertEquals(0, HeroAnimator.wallRoll(-1, 90, 0), EPS);
		assertEquals(0, HeroAnimator.wallRoll(-1, -90, 0), EPS);
		assertEquals(-20, HeroAnimator.wallRoll(-1, 180, 0), EPS);
		// the frames between are linear (2519: -17.1 at one column off the middle)
		assertEquals(20 * (1 - 12.857f / 90), HeroAnimator.wallRoll(-1, 12.857f, 0), EPS);
		// the other wall
		assertEquals(-20, HeroAnimator.wallRoll(1, 0, 0), EPS);
		// none looking straight up or down (the sheet's rows at +-89)
		assertEquals(0, HeroAnimator.wallRoll(-1, 0, 89), EPS);
		assertEquals(10, HeroAnimator.wallRoll(-1, 0, -44.5f), EPS);
	}
}
