package dev.overcraft.hero;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Who the server's copy of a shot takes: through the phased, unless they're the owner's side. (The client's returns first in canHitEntity: not offline-testable.) */
class ShotRuleTest {
	@Test
	void theServerHitsAnEnemy() {
		assertTrue(Allies.shotRule(true, false, false));
	}

	@Test
	void aPhasedEnemyIsPassedThrough() {
		assertFalse(Allies.shotRule(true, true, false));
	}

	@Test
	void aPhasedAllyStillTakesAHeal() {
		assertTrue(Allies.shotRule(true, true, true));
	}

	@Test
	void minecraftsNoStandsWhateverElse() {
		assertFalse(Allies.shotRule(false, false, true));
	}
}
