package dev.overcraft;

import dev.overcraft.hero.OwMovement;
import dev.overcraft.kiriko.KirikoStats;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OwMathTest {
	@Test
	void healthScale() {
		assertEquals(20f, Ow.hp(250), 1e-5);   // 250 Overwatch health = 10 hearts
		assertEquals(18f, Ow.hp(225), 1e-5);
		assertEquals(4.8f, Ow.hp(KirikoStats.KUNAI_DAMAGE), 1e-5);
		assertEquals(250f, Ow.toOw(20), 1e-4);
	}

	@Test
	void runSpeeds() {
		assertEquals(5.5, OwMovement.runSpeed(0, 1, false));
		assertEquals(5.5, OwMovement.runSpeed(1, 0, false));
		assertEquals(4.95, OwMovement.runSpeed(0, -1, false), 1e-9);
		assertEquals(5.5 * 14 / 15, OwMovement.runSpeed(1, -1, false), 1e-9);
		assertEquals(3.0, OwMovement.runSpeed(0, -1, true));
		assertEquals(0, OwMovement.runSpeed(0, 0, false));
	}

	@Test
	void jumpApexMatchesWiki() {
		// simulate the jump tick by tick: apex should be the wiki's 0.98 m (within a tick's worth)
		double vy = Ow.speed(OwMovement.JUMP_SPEED);
		double y = 0;
		while (vy > 0) {
			double[] step = OwMovement.verticalStep(vy, false);
			y += step[0];
			vy = step[1];
		}
		assertEquals(0.98, y, 0.03);
	}

	@Test
	void fallSpeedTendsToTerminal() {
		double vy = 0;
		for (int t = 0; t < 20 * 10; t++) {
			vy -= Ow.accel(OwMovement.gravity(vy * 20));
		}
		assertEquals(-30, vy * 20, 0.5);
		assertEquals(17.5, OwMovement.gravity(0), 1e-9);
	}

	@Test
	void groundAccelerationTakesAboutATenthOfASecond() {
		Vec3 v = Vec3.ZERO;
		Vec3 target = new Vec3(Ow.speed(5.5), 0, 0);
		int ticks = 0;
		while (!v.equals(target)) {
			v = OwMovement.approach(v, target, Ow.accel(OwMovement.GROUND_ACCEL));
			ticks++;
		}
		assertEquals(2, ticks); // 0.1 s
	}

	@Test
	void suzuLandsAfterAboutSevenSecondsThrownUp() {
		double vy = Ow.speed(KirikoStats.SUZU_SPEED);
		double y = 0;
		int ticks = 0;
		do {
			vy -= Ow.accel(KirikoStats.SUZU_GRAVITY);
			y += vy;
			ticks++;
		} while (y > 0);
		assertEquals(7.0, ticks / 20.0, 0.1);
	}
}
