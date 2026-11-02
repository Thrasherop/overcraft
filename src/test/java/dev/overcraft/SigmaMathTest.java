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

	@Test
	void accretionBurst() {
		// 40 to 1.5 m, 12 at the 2.5 m edge (ours: W's minimum), none past it; 0.65 s gather = 13 ticks
		assertEquals(40, dev.overcraft.sigma.Accretion.burstDamage(1.5), 1e-9);
		assertEquals(26, dev.overcraft.sigma.Accretion.burstDamage(2.0), 1e-9);
		assertEquals(12, dev.overcraft.sigma.Accretion.burstDamage(2.5), 1e-9);
		assertEquals(0, dev.overcraft.sigma.Accretion.burstDamage(2.51), 1e-9);
		assertEquals(13, Ow.ticks(SigmaStats.ACCRETION_CAST));
	}

	@Test
	void fluxSlam() {
		// half of max health, at most 200 on a boss: a zombie (250) 125, an iron golem (1250) 625, the Wither (3750) 200
		assertEquals(125, dev.overcraft.sigma.GraviticFlux.slamDamage(250, false), 1e-9);
		assertEquals(625, dev.overcraft.sigma.GraviticFlux.slamDamage(1250, false), 1e-9);
		assertEquals(200, dev.overcraft.sigma.GraviticFlux.slamDamage(3750, true), 1e-9);
		assertEquals(100, dev.overcraft.sigma.GraviticFlux.slamDamage(200, true), 1e-9);
	}

	@Test
	void fluxRise() {
		// held 0.4 s rising at 18 - 1.75 m/s² (6.5 m/s), then held to 3 m/s: about 6 m in its 2 s
		double vy = 0, y = 0;
		for (int i = 0; i < 40; i++) {
			vy = dev.overcraft.sigma.GraviticFlux.liftSpeed(vy, i * 0.05);
			y += vy * 0.05;
		}
		assertEquals(3, vy, 1e-9);
		assertEquals(6.0, y, 0.4);
	}
}
