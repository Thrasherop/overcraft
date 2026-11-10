package dev.overcraft.ana;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Who gets the stop for the numbed sound's 3p when one is cleansed: a stop names a sound, not one playing. */
class AnestheticRuleTest {
	@Test
	void aListenerWithNoOtherNumbedNearGetsIt() {
		assertTrue(Anesthetic.stops3p(false, 10, false));
	}

	@Test
	void notWithAnotherNumbedInEarshot() {
		assertFalse(Anesthetic.stops3p(false, 10, true));
	}

	@Test
	void notTheOneCleansed() {
		assertFalse(Anesthetic.stops3p(true, 0, false));
	}

	@Test
	void notOutOfEarshot() {
		assertFalse(Anesthetic.stops3p(false, 46, false));
	}
}
