package dev.overcraft.hero;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Who a shot's copy takes: never the client's (it can't know who's phased, and it would vanish there); through the phased, unless they're the owner's side. */
class ShotRuleTest {
	@Test
	void theServerHitsAnEnemy() {
		assertTrue(Allies.shotRule(false, true, false, false));
	}

	@Test
	void theClientNeverTakesACreature() {
		assertFalse(Allies.shotRule(true, true, false, false));
		assertFalse(Allies.shotRule(true, true, true, true));
	}

	@Test
	void aPhasedEnemyIsPassedThrough() {
		assertFalse(Allies.shotRule(false, true, true, false));
	}

	@Test
	void aPhasedAllyStillTakesAHeal() {
		assertTrue(Allies.shotRule(false, true, true, true));
	}

	@Test
	void minecraftsNoStandsWhateverElse() {
		assertFalse(Allies.shotRule(false, false, false, true));
	}
}
