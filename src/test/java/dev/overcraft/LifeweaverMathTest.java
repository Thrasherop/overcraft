package dev.overcraft;

import dev.overcraft.lifeweaver.LifeGrip;
import dev.overcraft.lifeweaver.LifeweaverStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Lifeweaver's numbers as his data and the patch notes give them (lifeweaver-map.md, tools/reference/lifeweaver.md). */
class LifeweaverMathTest {
	@Test
	void healingFillsHealthThenShields() {
		// W Hit points: health, armor and shields are recoverable, health replenished first
		// a hit on his shields alone (250 -> 200), then the dash's 55: all 50 back to the shields
		assertArrayEquals(new double[] {0, 50}, dev.overcraft.hero.Shields.heal(200, 0, 200, 50, 55), 1e-9);
		// 100 damage (150 health, no shields), then 55: 50 to health, 5 to the shields
		assertArrayEquals(new double[] {50, 5}, dev.overcraft.hero.Shields.heal(150, 0, 200, 50, 55), 1e-9);
		// shields back by themselves first (150 + 50), then 75: health only
		assertArrayEquals(new double[] {50, 0}, dev.overcraft.hero.Shields.heal(150, 50, 200, 50, 75), 1e-9);
	}

	@Test
	void healthAndShields() {
		// 200 + 50 shields = 250 Overwatch health = 10 hearts; the shields are the top 4
		assertEquals(20f, Ow.hp(LifeweaverStats.HEALTH + LifeweaverStats.SHIELDS), 1e-5);
		assertEquals(4f, Ow.hp(LifeweaverStats.SHIELDS), 1e-5);
	}

	@Test
	void blossomHealsTenToNinety() {
		// 10 below 10% charge, 90 at full (BZ 2026-04-14), the curve between (D 21C6)
		assertEquals(10, LifeweaverStats.heal(0), 1e-9);
		assertEquals(10, LifeweaverStats.heal(0.1), 1e-9);
		assertEquals(90, LifeweaverStats.heal(1), 1e-9);
		assertEquals(50, LifeweaverStats.heal((0.1 + 0.9985) / 2), 1e-6);
	}

	@Test
	void blossomSpeedJumpsToSixtyAtNinetyPercent() {
		assertEquals(30, LifeweaverStats.blossomSpeed(0), 1e-9);
		assertEquals(45, LifeweaverStats.blossomSpeed(0.5), 1e-9);
		assertEquals(60, LifeweaverStats.blossomSpeed(0.9), 1e-9);
		assertEquals(60, LifeweaverStats.blossomSpeed(1), 1e-9);
	}

	@Test
	void passiveChargeTakesFiveSeconds() {
		// 1.15 s held, 4.5 times slower when not: full in 5.175 s (D 1C5A)
		assertEquals(5.175, LifeweaverStats.CHARGE_TIME * LifeweaverStats.CHARGE_PASSIVE, 1e-9);
	}

	@Test
	void dashCoversSevenMetres() {
		// 0.25 s at 28.875 m/s: 7.2 m before the speed after it (D 36A2); Dashing Escape x1.3
		assertEquals(28.875, LifeweaverStats.DASH_SPEED, 1e-9);
		assertEquals(7.21875, LifeweaverStats.DASH_SPEED * LifeweaverStats.DASH_TIME, 1e-9);
		assertEquals(9.384375, LifeweaverStats.DASH_SPEED * LifeweaverStats.DASHING_ESCAPE * LifeweaverStats.DASH_TIME, 1e-9);
	}

	@Test
	void gripPullSpeeds() {
		// within 15 m: 25 m/s throughout; past it: 12 for 0.4 s, 12 -> 35 over 0.6 s, then 35 (D 21EE)
		assertEquals(25, LifeGrip.speed(false, 0), 1e-9);
		assertEquals(25, LifeGrip.speed(false, 40), 1e-9);
		assertEquals(12, LifeGrip.speed(true, 0), 1e-9);
		assertEquals(12, LifeGrip.speed(true, 7), 1e-9);
		assertEquals(12 + 23 * 0.5, LifeGrip.speed(true, 14), 1e-9);
		assertEquals(35, LifeGrip.speed(true, 20), 1e-9);
		assertEquals(35, LifeGrip.speed(true, 60), 1e-9);
	}

	@Test
	void treeHealsUpTo870() {
		// 150 as it sprouts, then 8 pulses of 90: 870 at most (W)
		assertEquals(870, LifeweaverStats.TREE_SPROUT + LifeweaverStats.TREE_PULSES * LifeweaverStats.TREE_PULSE, 1e-9);
		assertEquals(14.0, LifeweaverStats.TREE_PULSES * LifeweaverStats.TREE_PULSE_EVERY, 1e-9);
	}
}
