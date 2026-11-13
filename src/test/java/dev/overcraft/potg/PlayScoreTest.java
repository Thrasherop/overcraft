package dev.overcraft.potg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Play of the Game's choice of category (the patent's rule, with our floor). */
class PlayScoreTest {
	@Test
	void theHighestBestWins() {
		assertEquals(PlayScore.Category.LIFE_SAVER.ordinal(), PlayScore.pick(new double[]{900, 1200, 0, 0}));
		assertEquals(PlayScore.Category.HIGH_SCORE.ordinal(), PlayScore.pick(new double[]{2500, 1200, 0, 500}));
	}

	@Test
	void aSmallHighScoreGivesWayToAnotherThatScored() {
		// High Score under the floor falls to the best of the others, even a smaller one
		assertEquals(PlayScore.Category.SHUTDOWN.ordinal(), PlayScore.pick(new double[]{PlayScore.HIGH_SCORE_FLOOR - 1, 0, 0, 50}));
		// but stays when nothing else scored
		assertEquals(PlayScore.Category.HIGH_SCORE.ordinal(), PlayScore.pick(new double[]{100, 0, 0, 0}));
	}

	@Test
	void nothingScoredIsNoPlay() {
		assertEquals(-1, PlayScore.pick(new double[]{0, 0, 0, 0}));
	}

	@Test
	void theWindowAddsItsSeconds() {
		double[] seconds = new double[PlayScore.WINDOW];
		seconds[0] = 100;
		seconds[PlayScore.WINDOW - 1] = 50;
		assertEquals(150, PlayScore.windowSum(seconds), 1e-9);
	}
}
