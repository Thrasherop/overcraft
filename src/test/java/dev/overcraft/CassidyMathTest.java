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
		// full cut for 2 s, then 10 steps of 0.15 s down to none, stepped as Giddy Up is: the first 0.15 s on, none from 3.5 s
		assertEquals(1, Cassidy.deadeyeFade(0), 1e-9);
		assertEquals(1, Cassidy.deadeyeFade(40), 1e-9);
		assertEquals(1, Cassidy.deadeyeFade(42), 1e-9);
		assertEquals(0.9, Cassidy.deadeyeFade(43), 1e-9);
		assertTrue(Cassidy.deadeyeFade(67) > 0);
		assertEquals(0, Cassidy.deadeyeFade(40 + 30), 1e-9);
		assertTrue(Cassidy.deadeyeFade(50) < 0.9 && Cassidy.deadeyeFade(50) > 0);
		// the same steps as Giddy Up's from its start (critic b79a4d1 #6)
		for (int t = 0; t <= 40; t++) {
			assertEquals(Cassidy.giddyBonus(t) / CassidyStats.GIDDY_SPEED, Cassidy.deadeyeFade(40 + t), 1e-9);
		}
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

	@Test
	void damageTaken() {
		// rolling: half (critic b79a4d1's mutation took it to 1 with every test green); Deadeye: 40 % less through the cast and its 2 s
		assertEquals(0.5, Cassidy.takenFactorRule(true, -1), 1e-9);
		assertEquals(0.5, Cassidy.takenFactorRule(true, 30), 1e-9);
		assertEquals(1, Cassidy.takenFactorRule(false, -1), 1e-9);
		assertEquals(0.6, Cassidy.takenFactorRule(false, 0), 1e-9);
		int cast = Ow.ticks(CassidyStats.DEADEYE_CAST);
		assertEquals(0.6, Cassidy.takenFactorRule(false, cast + 40), 1e-9);
		assertEquals(1, Cassidy.takenFactorRule(false, cast + 70), 1e-9);
	}

	@Test
	void deadeyeTargets() {
		// in the 105 degree cone within 200 m, and in sight: out of sight it doesn't gain (the critic's mutation locked through walls)
		assertTrue(Cassidy.lockable(10, 1, true));
		assertTrue(!Cassidy.lockable(10, 1, false));
		assertTrue(!Cassidy.lockable(201, 1, true));
		assertTrue(Cassidy.lockable(10, Math.cos(Math.toRadians(52)), true));
		assertTrue(!Cassidy.lockable(10, Math.cos(Math.toRadians(53)), true));
		// no one in sight: the press does nothing (W: can't fire without a target in sight; critic b79a4d1 #2)
		assertTrue(!Cassidy.canFire(0));
		assertTrue(Cassidy.canFire(1));
		// the skull: what lands over the health (armor's cut counted in what lands: Combat.projected)
		assertTrue(Cassidy.skullShare(250, 250) >= 1);
		assertTrue(Cassidy.skullShare(240, 250) < 1);
	}

	@Test
	void flashbang() {
		// within 3 m, in sight, no barrier between (the critic's mutation let it through walls)
		assertTrue(Cassidy.flashTakes(2.9, true, false));
		assertTrue(!Cassidy.flashTakes(2.9, false, false));
		assertTrue(!Cassidy.flashTakes(2.9, true, true));
		assertTrue(!Cassidy.flashTakes(3.1, true, false));
		// its range met along the move, not after it: 1.5 m legs from 6.0 m cross 7 m two thirds in (it went off at ~7.5 m)
		net.minecraft.world.phys.Vec3 o = net.minecraft.world.phys.Vec3.ZERO;
		double t = Cassidy.rangeCrossing(o, new net.minecraft.world.phys.Vec3(6, 0, 0),
			new net.minecraft.world.phys.Vec3(7.5, 0, 0), CassidyStats.FLASH_RANGE);
		assertEquals(2 / 3.0, t, 1e-9);
		assertEquals(-1, Cassidy.rangeCrossing(o, new net.minecraft.world.phys.Vec3(4.5, 0, 0),
			new net.minecraft.world.phys.Vec3(6, 0, 0), CassidyStats.FLASH_RANGE), 1e-9);
		assertEquals(0, Cassidy.rangeCrossing(o, new net.minecraft.world.phys.Vec3(7, 0, 0),
			new net.minecraft.world.phys.Vec3(8, 0, 0), CassidyStats.FLASH_RANGE), 1e-9);
	}

	@Test
	void rollAuthority() {
		int mode = 0;
		// ready, nothing pending: his client rolls (and so does the server: mayRoll is the same rule)
		assertTrue(Cassidy.clientMay(0, 0, mode, false, false, false));
		// an E (or Q, or his last roll) the server hasn't seen yet: no roll here, so none there (critic b79a4d1 #1, E then Shift)
		assertTrue(!Cassidy.clientMay(1, 0, mode, false, false, false));
		// a cooldown the last sync still shows (0.15 s left): no roll here, and none sent (the tap just before it ends)
		assertTrue(!Cassidy.clientMay(0, 3, mode, false, false, false));
		assertTrue(!Cassidy.clientMay(0, 0, Cassidy.MODE_CASTING, false, false, false));
		assertTrue(!Cassidy.clientMay(0, 0, Cassidy.MODE_HINDERED, false, false, false));
		assertTrue(!Cassidy.clientMay(0, 0, Cassidy.MODE_DEADEYE, false, false, false));
		assertTrue(!Cassidy.clientMay(0, 0, 0, false, false, true));
		// the counts ride above the flags without touching them
		int synced = 5 << Cassidy.SEEN_SHIFT | 7 << Cassidy.CUT_SHIFT;
		assertTrue(Cassidy.clientMay(0, 0, synced, false, false, false));
		assertEquals(0, Cassidy.pending(5, (synced >>> Cassidy.SEEN_SHIFT) & 0xFF));
		assertEquals(1, Cassidy.pending(0, 255));
		// what's counted: E, Q, a roll; not a bare Shift, a reload or a melee
		assertTrue(Cassidy.counts(dev.overcraft.net.HeroInputPayload.PRESS_ABILITY_2, 0));
		assertTrue(Cassidy.counts(dev.overcraft.net.HeroInputPayload.PRESS_ULTIMATE, 0));
		assertTrue(Cassidy.counts(dev.overcraft.net.HeroInputPayload.PRESS_ABILITY_1, 1));
		assertTrue(!Cassidy.counts(dev.overcraft.net.HeroInputPayload.PRESS_ABILITY_1, 0));
		assertTrue(!Cassidy.counts(dev.overcraft.net.HeroInputPayload.PRESS_RELOAD | dev.overcraft.net.HeroInputPayload.PRESS_MELEE, 0));
	}
}