package dev.overcraft.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Lúcio's ride as one number (#115): side and yaw there and back, 0 only off the wall. */
class WallCodeTest {
	@Test
	void sideAndYawSurvive() {
		for (int side : new int[] {-1, 1}) {
			for (float yaw : new float[] {0, 0.1f, 90, 179.9f, 180, 359.9f}) {
				int code = OwMovement.wallCode(side, yaw);
				assertEquals(side, OwMovement.wallCodeSide(code));
				assertEquals(yaw, OwMovement.wallCodeYaw(code), 0.051f);
			}
		}
	}

	@Test
	void anyYawIsOnWireAndNeverOff() {
		assertEquals(270, OwMovement.wallCodeYaw(OwMovement.wallCode(1, -90)), 0.051f);
		assertEquals(0, OwMovement.wallCodeYaw(OwMovement.wallCode(1, 360)), 0.051f);
		assertEquals(1, OwMovement.wallCode(-1, 0));
		assertEquals(0, OwMovement.wallCodeSide(0));
	}
}
