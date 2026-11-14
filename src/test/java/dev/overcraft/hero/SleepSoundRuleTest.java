package dev.overcraft.hero;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Who gets the stop for the others' sleep bed when one sleeper wakes (#123): a stop names a sound, not one playing. */
class SleepSoundRuleTest {
	@Test
	void aListenerWithNoOtherSleeperNearGetsIt() {
		assertTrue(Sleep.stopsBed(false, 10 * 10, false));
	}

	@Test
	void notWithAnotherSleepersBedReachingThem() {
		assertFalse(Sleep.stopsBed(false, 10 * 10, true));
	}

	@Test
	void notTheSleeper() {
		assertFalse(Sleep.stopsBed(true, 0, false));
	}

	@Test
	void notPastWhereTheBedWasSent() {
		assertTrue(Sleep.stopsBed(false, 53 * 53, false));
		assertFalse(Sleep.stopsBed(false, 54 * 54, false));
	}
}
