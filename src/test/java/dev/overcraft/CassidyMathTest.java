package dev.overcraft;

import dev.overcraft.cassidy.Cassidy;
import dev.overcraft.cassidy.CassidyStats;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.Perk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cassidy's math (dev.overcraft.cassidy; tools/reference/cassidy-map.md). */
class CassidyMathTest {
	@Test
	void health() {
		// 250 = 20 Minecraft health (10 hearts), Damage, Sharpshooter, Deadeye 1800
		assertEquals(250, Hero.CASSIDY.owHealth);
		assertEquals(20f, Ow.hp(Hero.CASSIDY.owHealth), 1e-5);
		assertEquals(Hero.Role.DAMAGE, Hero.CASSIDY.role);
		assertEquals(1800, Combat.ultCost(Hero.CASSIDY));
	}

	@Test
	void peacekeeperFalloff() {
		// 70 to 25 m, 21 from 35 m, 45.5 at 30 m; x2 on the head (140 close)
		assertEquals(70, Cassidy.peacekeeperDamage(10, false), 1e-9);
		assertEquals(70, Cassidy.peacekeeperDamage(25, false), 1e-9);
		assertEquals(45.5, Cassidy.peacekeeperDamage(30, false), 1e-9);
		assertEquals(21, Cassidy.peacekeeperDamage(35, false), 1e-9);
		assertEquals(21, Cassidy.peacekeeperDamage(80, false), 1e-9);
		assertEquals(140, Cassidy.peacekeeperDamage(5, true), 1e-9);
	}

	@Test
	void fanFalloff() {
		// 50 to 20 m, 15 from 30 m (W "15 at 30 m"), 32.5 at 25 m
		assertEquals(50, Cassidy.fanDamage(20), 1e-9);
		assertEquals(32.5, Cassidy.fanDamage(25), 1e-9);
		assertEquals(15, Cassidy.fanDamage(30), 1e-9);
		// six shots 0.12 s apart: 0.6 s from the first to the last
		assertEquals(0.6, 5 * CassidyStats.FAN_INTERVAL, 1e-9);
	}

	@Test
	void sharpshooter() {
		// a 140 headshot takes 1.05 s (21 ticks) off Combat Roll (BZ / W example)
		assertEquals(21, Cassidy.sharpshooterCut(140), 1e-9);
		assertEquals(0, Cassidy.sharpshooterCut(-5), 1e-9);
	}

	@Test
	void silverBleed() {
		// 50 in all over its steps (found playing: 8 steps at the 0.2 s rate gave 53.3)
		int steps = Cassidy.bleedSteps();
		assertEquals(8, steps);
		assertEquals(50, CassidyStats.SILVER_BLEED / steps * steps, 1e-9);
	}

	@Test
	void roll() {
		// 15 m/s for 0.4 s = 6 m on the ground, 7.8 m in the air (x1.3)
		assertEquals(6.0, CassidyStats.ROLL_SPEED * CassidyStats.ROLL_TIME, 1e-9);
		assertEquals(7.8, CassidyStats.ROLL_SPEED * CassidyStats.ROLL_AIR * CassidyStats.ROLL_TIME, 1e-9);
		assertEquals(4, Ow.ticks(CassidyStats.ROLL_RELOAD_AT));
		assertEquals(8, Ow.ticks(CassidyStats.ROLL_TIME));
	}

	@Test
	void deadeyeLock() {
		// 150 a second for a target's first 2 s (300 in 2 s), then 300: 2 s + 1 s = 600
		double charge = 0;
		for (int t = 0; t < 60; t++) {
			charge += Cassidy.lockStep(t);
		}
		assertEquals(600, charge, 1e-9);
		// a 250 target is a kill after 1.67 s of lock (34 ticks: 250 / 7.5)
		charge = 0;
		int ticks = 0;
		while (charge < 250) {
			charge += Cassidy.lockStep(ticks++);
		}
		assertEquals(34, ticks);
	}

	@Test
	void deadeyeFade() {
		// full cut for 2 s, then 10 steps of 0.15 s down to none
		assertEquals(1, Cassidy.deadeyeFade(0), 1e-9);
		assertEquals(1, Cassidy.deadeyeFade(39), 1e-9);
		assertEquals(0.9, Cassidy.deadeyeFade(40), 1e-9);
		assertEquals(0, Cassidy.deadeyeFade(40 + 30), 1e-9);
		assertTrue(Cassidy.deadeyeFade(50) < 0.9 && Cassidy.deadeyeFade(50) > 0);
		// shots: 0.166 s, 0.266, 0.366 from the press
		assertEquals(3, Cassidy.shotTick(0));
		assertEquals(5, Cassidy.shotTick(1));
		assertEquals(7, Cassidy.shotTick(2));
	}

	@Test
	void giddyUp() {
		// +60 % at the roll's end, 10 steps over 1.5 s
		assertEquals(0.6, Cassidy.giddyBonus(0), 1e-9);
		assertEquals(0.6, Cassidy.giddyBonus(2), 1e-9);
		assertEquals(0.54, Cassidy.giddyBonus(3), 1e-9);
		assertEquals(0, Cassidy.giddyBonus(30), 1e-9);
	}

	@Test
	void perks() {
		assertEquals(Hero.CASSIDY, Perk.CASSIDY_BANG_BANG.hero);
		assertEquals(Perk.Tier.MINOR, Perk.CASSIDY_GIDDY_UP.tier);
		assertEquals(Perk.Tier.MAJOR, Perk.CASSIDY_SILVER_BULLET.tier);
		// Bang Bang: 45 each
		assertEquals(45, CassidyStats.FLASH_DAMAGE * CassidyStats.BANG_DAMAGE, 1e-9);
	}
}
