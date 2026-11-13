package dev.overcraft.hero;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The rule every hit asks, the damage the phase refuses, and whose map it reads. */
class PhaseRulesTest {
	@Test
	void anEnemyIsHit() {
		assertTrue(Allies.hurtRule(true, false, false, false));
	}

	@Test
	void aPhasedEnemyIsNot() {
		assertFalse(Allies.hurtRule(true, false, false, true));
	}

	@Test
	void anAllyOnlyWithFriendlyFireAndNotPhased() {
		assertFalse(Allies.hurtRule(true, true, false, false));
		assertTrue(Allies.hurtRule(true, true, true, false));
		assertFalse(Allies.hurtRule(true, true, true, true));
	}

	@Test
	void theServerReadsThePhase() {
		assertTrue(Combat.phaseStopsRule(false, false, true));
		assertFalse(Combat.phaseStopsRule(false, false, false));
	}

	@Test
	void theClientNeverDoes() {
		assertFalse(Combat.phaseStopsRule(true, false, true));
	}

	@Test
	void theDevSwitchTurnsItOff() {
		assertFalse(Combat.phaseStopsRule(false, true, true));
	}

	@Test
	void thePhaseRefusesOrdinaryDamage() {
		assertTrue(Combat.refusesRule(true, false));
	}

	@Test
	void notWhatBeatsInvulnerability() {
		assertFalse(Combat.refusesRule(true, true));
	}

	@Test
	void unphasedRefusesNothing() {
		assertFalse(Combat.refusesRule(false, false));
	}
}
