package dev.overcraft;

import dev.overcraft.widowmaker.Widowmaker;
import dev.overcraft.widowmaker.WidowmakerStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Widowmaker's numbers (WidowmakerStats) through her kit's math. */
class WidowmakerMathTest {
	@Test
	void hipDamageAndFalloff() {
		// 14 to 20 m, 4.2 from 40 m, x2 on the head (D 1AAE / 1AC7 / 1AC6; W)
		assertEquals(14, Widowmaker.hipDamage(10, false), 1e-9);
		assertEquals(28, Widowmaker.hipDamage(10, true), 1e-9);
		assertEquals(4.2, Widowmaker.hipDamage(60, false), 1e-9);
		assertEquals(9.1, Widowmaker.hipDamage(30, false), 1e-9);
	}

	@Test
	void scopedChargeAndDamage() {
		// 12 at no charge, 120 full, x2.5 on the head: 300; 60 at 70 m and beyond (D 1ABA, 1AC4 / 1AC3 / 1AD5; W)
		assertEquals(12, Widowmaker.scopedDamage(WidowmakerStats.CHARGE_MIN, 10, false), 1e-9);
		assertEquals(120, Widowmaker.scopedDamage(WidowmakerStats.CHARGE_MAX, 10, false), 1e-9);
		assertEquals(300, Widowmaker.scopedDamage(WidowmakerStats.CHARGE_MAX, 10, true), 1e-9);
		assertEquals(60, Widowmaker.scopedDamage(WidowmakerStats.CHARGE_MAX, 90, false), 1e-9);
		assertEquals(150, Widowmaker.scopedDamage(WidowmakerStats.BITE_CHARGE_MAX, 10, false), 1e-9);
		// 0.9 s (18 ticks) to full; 0.45 s with Sniper's Instinct; Widow's Bite 1.15 s (23 ticks) to 125 %
		assertEquals(18, ticksToFull(WidowmakerStats.CHARGE_MAX, false));
		assertEquals(9, ticksToFull(WidowmakerStats.CHARGE_MAX, true));
		assertEquals(23, ticksToFull(WidowmakerStats.BITE_CHARGE_MAX, false));
		assertEquals(100, Widowmaker.chargePercent(WidowmakerStats.CHARGE_MAX), 1e-9);
		assertEquals(125, Widowmaker.chargePercent(WidowmakerStats.BITE_CHARGE_MAX), 1e-9);
	}

	private static int ticksToFull(double max, boolean instinct) {
		double c = WidowmakerStats.CHARGE_MIN;
		int t = 0;
		while (c < max - 1e-9) {
			c = Widowmaker.chargeStep(c, max, instinct);
			t++;
		}
		return t;
	}

	@Test
	void scopeTakesAThirdOfASecond() {
		double s = 0;
		int ticks = 0;
		while (s < 1) {
			s = Widowmaker.scopeStep(s, true);
			ticks++;
		}
		// 0.333 s: 7 ticks of 1/6.67 (D v169 rate 3)
		assertEquals(7, ticks);
	}

	@Test
	void hipSpreadGrowsOverElevenShots() {
		assertEquals(0, Widowmaker.hipSpread(0), 1e-9);
		assertEquals(3, Widowmaker.hipSpread(11), 1e-9);
		assertEquals(3, Widowmaker.hipSpread(20), 1e-9);
		assertTrue(Widowmaker.hipSpread(5) > 1 && Widowmaker.hipSpread(5) < 2);
	}

	@Test
	void sharpshooterAndPoison() {
		// a full scoped headshot (300) takes 2.25 s (45 ticks) off the hook; the poison is 25 steps of 5 (125 over 5 s)
		assertEquals(45, Widowmaker.sharpshooterCut(300), 1e-9);
		assertEquals(25, Widowmaker.poisonSteps());
		assertEquals(5, WidowmakerStats.POISON_TOTAL / Widowmaker.poisonSteps(), 1e-9);
	}
}
