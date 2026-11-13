package dev.overcraft;

import dev.overcraft.hero.OwMovement;
import dev.overcraft.pharah.Pharah;
import dev.overcraft.pharah.PharahStats;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pharah's movement against the owner's Workshop recording (WS 2026-11-11, techniques/heroes/pharah_workshop_2026-11-11.md). */
class PharahMathTest {
	/** One travel tick's vertical speed with the hover (blocks a tick), as OwMovement.travel works it out. */
	private static double hoverTick(int hover, double vy) {
		double fallen = OwMovement.verticalStep(vy, false)[1];
		return Pharah.hoverStep(hover, vy, fallen);
	}

	@Test
	void hoverSettlesAt466() {
		// from a -6.4 m/s fall (the 9.85 air press) or standing still, it settles on 4.66 within 1 s
		for (double from : new double[] {-6.4, 0, -19.6}) {
			double vy = Ow.speed(from);
			for (int t = 0; t < 20; t++) {
				vy = hoverTick(Pharah.HOVER_FUEL, vy);
			}
			assertEquals(4.66, vy * 20, from < -10 ? 0.1 : 0.03);
		}
		// the ease's rate: from -6.4 a tick moves 6 x (4.66 + 6.4) x ... = the exact 1 - e^-0.3 share (WS k 5.9-6.0)
		double after = hoverTick(Pharah.HOVER_FUEL, Ow.speed(-6.4)) * 20;
		assertEquals(4.66 - (4.66 + 6.4) * Math.exp(-0.3), after, 1e-9);
	}

	@Test
	void hoverNeverPullsDownHarderThanGravity() {
		// Jump held in a jet at 17 m/s: -17.5 m/s² exactly (WS), the gravity step's
		double vy = Ow.speed(17);
		double next = hoverTick(Pharah.HOVER_JET, vy);
		assertEquals(OwMovement.verticalStep(vy, false)[1], next, 1e-12);
		assertEquals(17 - 17.5 / 20, next * 20, 1e-9);
	}

	@Test
	void emptyTankFallsNoFasterThan8() {
		double vy = 0;
		for (int t = 0; t < 60; t++) {
			vy = hoverTick(Pharah.HOVER_EMPTY, vy);
		}
		assertEquals(-PharahStats.EMPTY_FALL, vy * 20, 1e-9);
	}

	@Test
	void barrageFlightAngles() {
		double speed = 5.5 * (1 - PharahStats.BARRAGE_SLOW);
		assertEquals(4.4, speed, 1e-9);
		Vec3 keys = new Vec3(0, 0, 1);
		// Crouch with a move key: 45 deg down, -3.11 at 4.40 (WS)
		Vec3 down = OwMovement.barrageWish(keys, false, true, speed);
		assertEquals(4.4, down.length(), 1e-9);
		assertEquals(-3.11, down.y, 0.01);
		// Jump and Crouch: level
		assertEquals(0, OwMovement.barrageWish(keys, true, true, speed).y, 1e-9);
		// Jump with a move key: about 20 deg up, +1.5 (WS 1.34-1.81)
		Vec3 up = OwMovement.barrageWish(keys, true, false, speed);
		assertEquals(4.4, up.length(), 1e-9);
		assertEquals(1.55, up.y, 0.1);
		// from a standstill (her speed zeroed at the cast) to 4.40 in about 0.29 s, then held there
		Vec3 v = Vec3.ZERO;
		int steps = 0;
		while (v.length() < 4.4 - 1e-6) {
			v = OwMovement.barrageStep(v, up, 1.0 / 60);
			steps++;
		}
		assertEquals(0.29, steps / 60.0, 0.02);
		// a turn to the crouch keeps her at 4.40 (WS |v| 4.40 through every turn)
		for (int i = 0; i < 30; i++) {
			v = OwMovement.barrageStep(v, down, 1.0 / 60);
			assertEquals(4.4, v.length(), 1e-6);
		}
		assertTrue(v.y < -3);
	}
}
