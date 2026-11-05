package dev.overcraft.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Combat.cleanse's table for what Sleep holds, and a stun landing on a knock-down (#119's critique). */
class SleepRulesTest {
	@Test
	void aSleepWakes() {
		assertEquals(Sleep.Cleansing.WAKES, Sleep.cleansing(false, false, 1));
	}

	@Test
	void aStunEnds() {
		assertEquals(Sleep.Cleansing.ENDS, Sleep.cleansing(true, false, 1));
	}

	@Test
	void hyperStrikesFloatEnds() {
		assertEquals(Sleep.Cleansing.ENDS_FLOAT, Sleep.cleansing(true, true, 0.01));
	}

	@Test
	void aKnockDownStays() {
		assertEquals(Sleep.Cleansing.STAYS, Sleep.cleansing(true, true, 1));
	}

	@Test
	void aStunOnAKnockDownStaysAKnockDown() {
		assertTrue(Sleep.slidesAfter(true, false));
		assertEquals(Sleep.Cleansing.STAYS, Sleep.cleansing(true, Sleep.slidesAfter(true, false), 1));
	}
}
