package dev.overcraft.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The stacking table and the cleanse: what Sleep.put, Sleep.stun, Sleep.cleanse and Sleep.release use (polish4.md says what can't be tested offline: those calls themselves). */
class HoldsTest {
	private static Holds.Hold sleep(long until) {
		return Holds.sleep(0, until);
	}

	private static Holds.Hold stun(long until) {
		return Holds.stun(0, until, false, 1);
	}

	private static Holds.Hold knock(long until) {
		return Holds.stun(0, until, true, 1);
	}

	private static Holds.Hold floatH(long until) {
		return Holds.stun(0, until, true, 0.01);
	}

	@Test
	void aFirstHoldIsItself() {
		assertEquals(stun(15), Holds.stacked(null, stun(15)));
	}

	@Test
	void aStunOnAKnockDownStaysAKnockDownAndUncleansable() {
		Holds.Hold h = Holds.stacked(knock(22), stun(15));
		assertNotNull(h);
		assertTrue(h.slides());
		assertEquals(Holds.Cleansing.STAYS, Holds.cleansing(h));
	}

	@Test
	void aShorterHoldNeverShortensIt() {
		assertEquals(22, Holds.stacked(knock(22), stun(15)).until());
		assertEquals(22, Holds.stacked(stun(22), knock(15)).until());
		assertEquals(30, Holds.stacked(stun(15), knock(30)).until());
	}

	@Test
	void aFloatOnAKnockDownDoesntFloat() {
		Holds.Hold h = Holds.stacked(knock(22), floatH(16));
		assertEquals(1, h.gravity());
		assertEquals(Holds.Cleansing.STAYS, Holds.cleansing(h));
	}

	@Test
	void aKnockDownEndsAFloat() {
		assertEquals(1, Holds.stacked(floatH(16), knock(22)).gravity());
	}

	@Test
	void aStunOnAFloatKeepsItFloating() {
		Holds.Hold h = Holds.stacked(floatH(16), stun(15));
		assertEquals(0.01, h.gravity());
		assertTrue(h.slides());
	}

	@Test
	void aDartDoesntLandOnAKnockDownOrAFloat() {
		assertNull(Holds.stacked(knock(22), sleep(100)));
		assertNull(Holds.stacked(floatH(16), sleep(100)));
	}

	@Test
	void aDartReplacesAStunAndASleep() {
		assertFalse(Holds.stacked(stun(15), sleep(100)).stun());
		assertEquals(100, Holds.stacked(sleep(20), sleep(100)).until());
	}

	@Test
	void aStunOrKnockDownDoesntLandOnASleeper() {
		assertNull(Holds.stacked(sleep(100), stun(15)));
		assertNull(Holds.stacked(sleep(100), knock(22)));
	}

	@Test
	void aCleanseWakesASleepInItsWaking() {
		Holds.Hold h = Holds.cleansed(sleep(100), 10, 10);
		assertTrue(h.waking());
		assertEquals(20, h.until());
	}

	@Test
	void aCleanseDoesntLengthenAWaking() {
		Holds.Hold nearly = sleep(15);
		assertEquals(nearly, Holds.cleansed(nearly, 10, 10));
	}

	@Test
	void aCleanseEndsAStunAtOnce() {
		assertNull(Holds.cleansed(stun(15), 5, 10));
	}

	@Test
	void aCleanseLeavesAKnockDownAndAFloat() {
		assertEquals(knock(22), Holds.cleansed(knock(22), 5, 10));
		assertEquals(floatH(16), Holds.cleansed(floatH(16), 5, 10));
	}

	@Test
	void aClientIsLetGoOnlyWhenNothingHolds() {
		assertTrue(Holds.letsGo(false, false));
		assertFalse(Holds.letsGo(true, false));
		assertFalse(Holds.letsGo(false, true));
	}
	/** #121: what answers a cleanse by saying so (1C7D), which is all Kiriko's Suzu line asks. */
	@org.junit.jupiter.api.Test
	void sleepAndStunTellKnockDownDoesNot() {
		org.junit.jupiter.api.Assertions.assertTrue(Holds.tells(Holds.sleep(0, 100)));
		org.junit.jupiter.api.Assertions.assertTrue(Holds.tells(Holds.stun(0, 100, false, 1)));
		org.junit.jupiter.api.Assertions.assertFalse(Holds.tells(Holds.stun(0, 100, true, 1)));
	}
}
