package dev.overcraft.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The numbers of the global healing reduction (#103). What it does to a real target is in the game: techniques/minecraft/allies.md. */
class HealScaleTest {
	private static final double EPS = 1.0E-9;

	@Test
	void hurtCutsThirtyPercent() {
		assertEquals(1, Combat.healScale(false, false, false, false), EPS);
		assertEquals(0.7, Combat.healScale(false, false, true, false), EPS);
	}

	@Test
	void aTankTakesHalfTheCut() {
		assertEquals(0.85, Combat.healScale(false, false, true, true), EPS);
		// an unhurt tank heals in full
		assertEquals(1, Combat.healScale(false, false, false, true), EPS);
	}

	@Test
	void itMultipliesWithOtherModifiers() {
		// Biotic Grenade's +50% and the cut: 1.5 x 0.7 (W: "stacks multiplicatively")
		assertEquals(1.05, Combat.healScale(false, true, true, false), EPS);
		assertEquals(0, Combat.healScale(true, false, true, false), EPS);
	}

	@Test
	void onlyAnEnemysHitCuts() {
		assertTrue(Combat.cutsHealing(true, false, false));
		assertFalse(Combat.cutsHealing(false, false, false), "the world: a fall, lava");
		assertFalse(Combat.cutsHealing(true, true, false), "self-damage");
		assertFalse(Combat.cutsHealing(true, false, true), "an ally's stray hit");
	}
}
