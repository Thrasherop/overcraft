package dev.overcraft;

import dev.overcraft.sigma.Hyperspheres;
import dev.overcraft.sigma.SigmaStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Sigma's kit math (tools/reference/sigma.md: Hyperspheres, Experimental Barrier). */
class SigmaMathTest {
	@Test
	void burstFalloff() {
		// 40 to 1 m, 20 at the 2.85 m edge, none past it
		assertEquals(40, Hyperspheres.burstDamage(0), 1e-9);
		assertEquals(40, Hyperspheres.burstDamage(1), 1e-9);
		assertEquals(30, Hyperspheres.burstDamage(1.925), 1e-9);
		assertEquals(20, Hyperspheres.burstDamage(2.85), 1e-9);
		assertEquals(0, Hyperspheres.burstDamage(2.9), 1e-9);
	}

	@Test
	void sphereTimings() {
		// 21.7 m at 50 m/s: 0.434 s, 8.68 ticks; a volley every 0.2 + 1.3 = 1.5 s
		assertEquals(0.434, SigmaStats.SPHERE_LIFETIME, 1e-9);
		assertEquals(30, Ow.ticks(SigmaStats.VOLLEY_GAP) + Ow.ticks(SigmaStats.VOLLEY_RELOAD));
	}

	@Test
	void barrierRegen() {
		// 0.122 of 650 a second
		assertEquals(79.3, SigmaStats.BARRIER_REGEN, 1e-9);
	}
}
