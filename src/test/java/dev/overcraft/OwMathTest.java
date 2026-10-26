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
	void fallSpeedFollowsTheWikiCurve() {
		// the wiki's measured graph: about 15.8 m/s after 1 s and 24.7 after 2 s (v = 30 tanh(17.5 t / 30))
		double vy = 0;
		for (int t = 0; t < 40; t++) {
			vy = OwMovement.verticalStep(vy, false)[1];
			if (t == 19) {
				assertEquals(-15.8, vy * 20, 0.4);
			}
		}
		assertEquals(-24.7, vy * 20, 0.5);
	}

	/** Steps of the mover at 60 Hz until {@code done}: the time it took in seconds. */
	private static double groundTime(Vec3 from, Vec3 wish, java.util.function.Predicate<Vec3> done) {
		Vec3 v = from;
		int steps = 0;
		while (!done.test(v)) {
			v = OwMovement.groundStep(v, wish, 1.0 / 60);
			steps++;
		}
		return steps / 60.0;
	}

	@Test
	void groundResponseIsOverwatchs() {
		Vec3 run = new Vec3(5.5, 0, 0);
		// full speed in about 0.05 s, a stop in about 0.08 s, a strafe reversed in about 0.06 s (tools/reference/movement.md)
		assertEquals(0.05, groundTime(Vec3.ZERO, run, v -> v.x >= 5.5 - 1e-9), 0.02);
		assertEquals(0.08, groundTime(run, Vec3.ZERO, v -> v.length() == 0), 0.02);
		assertEquals(0.06, groundTime(run, run.reverse(), v -> v.x <= -5.5 + 1e-9), 0.02);
		// held at full speed, it stays there: friction and acceleration balance
		assertEquals(5.5, OwMovement.groundStep(run, run, 1.0 / 60).x, 1e-9);
	}

	@Test
	void airControlTurnsAJumpAround() {
		// holding back right after take-off at a run: back is 4.95 m/s, so 5.5 / (3.5 x 4.95) = 0.32 s, inside one jump (0.67 s)
		Vec3 v = new Vec3(5.5, 0, 0);
		Vec3 back = new Vec3(-4.95, 0, 0);
		int steps = 0;
		while (v.x > 0) {
			v = OwMovement.airStep(v, back, 5.5, 1.0 / 60);
			steps++;
		}
		assertEquals(0.32, steps / 60.0, 0.02);
		// pressing sideways instead of forward turns the jump without slowing it: 90° in about 0.45 s at 5.5 m/s
		Vec3 turning = new Vec3(5.5, 0, 0);
		Vec3 side = new Vec3(0, 0, 5.5);
		steps = 0;
		while (turning.z < 5.5 - 1e-6) {
			turning = OwMovement.airStep(turning, side, 5.5, 1.0 / 60);
			assertEquals(5.5, turning.length(), 1e-6);
			steps++;
		}
		assertEquals(0.45, steps / 60.0, 0.03);
		// speed above the run speed decays in the air with no input: 3 m/s over goes under 1 in a second
		Vec3 fast = new Vec3(8.5, 0, 0);
		for (int i = 0; i < 60; i++) {
			fast = OwMovement.airStep(fast, Vec3.ZERO, 5.5, 1.0 / 60);
		}
		assertEquals(5.5 + 3 * Math.exp(-2), fast.x, 0.1);
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
