package dev.overcraft.hero;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A movement ability's landing (#35): set while moving, kept until down after it. */
class SoftLandingTest {
	@Test
	void setWhileTheAbilityMoves() {
		assertTrue(Holds.softLanding(false, true, true, false));
	}

	@Test
	void keptInTheAirAfterIt() {
		assertTrue(Holds.softLanding(true, false, false, false));
	}

	@Test
	void overOnceDown() {
		assertFalse(Holds.softLanding(true, false, true, false));
	}

	@Test
	void theGroundSheLeftDoesntCount() {
		assertTrue(Holds.softLanding(true, false, true, true));
	}

	@Test
	void aPlainFallIsntOne() {
		assertFalse(Holds.softLanding(false, false, false, false));
	}

	@Test
	void minecraftModeEndsItAndSoDoesStayingOnALadderNotATouch() {
		assertTrue(Holds.landingEnds(false, 0));
		assertTrue(Holds.landingEnds(true, Holds.LANDING_TOUCH));
		assertFalse(Holds.landingEnds(true, Holds.LANDING_TOUCH - 1));
		assertFalse(Holds.landingEnds(true, 0));
	}
}
