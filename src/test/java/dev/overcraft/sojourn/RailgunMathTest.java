package dev.overcraft.sojourn;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sojourn's Railgun and Charged Shot numbers (SojournStats; tools/reference/sojourn.md). */
class RailgunMathTest {
	@Test
	void chargedShotFalloff() {
		// D 1CE6 40 m / 1CE5 60 m / 1CE7 0.5: full to 40, half from 60, linear between
		assertEquals(1, Railgun.falloff(10), 1e-9);
		assertEquals(1, Railgun.falloff(40), 1e-9);
		assertEquals(0.75, Railgun.falloff(50), 1e-9);
		assertEquals(0.5, Railgun.falloff(60), 1e-9);
		assertEquals(0.5, Railgun.falloff(90), 1e-9);
	}

	@Test
	void spreadStaysInsideItsEllipse() {
		// 1.6 degrees up and down, 0.64 sideways (W's split on D's 1.6)
		RandomSource random = RandomSource.create(7);
		Vec3 ahead = new Vec3(0, 0, 1);
		double up = 0, side = 0;
		for (int i = 0; i < 20000; i++) {
			Vec3 d = Railgun.spread(ahead, 1.6, 0.64, random);
			double a = Math.toDegrees(Math.atan2(d.y, d.z)), b = Math.toDegrees(Math.atan2(Math.abs(d.x), d.z));
			up = Math.max(up, Math.abs(a));
			side = Math.max(side, b);
		}
		assertTrue(up <= 1.6 + 1e-6 && up > 1.55, "vertical " + up);
		assertTrue(side <= 0.64 + 1e-6 && side > 0.6, "sideways " + side);
		assertEquals(ahead, Railgun.spread(ahead, 0, 0, random));
	}
}
