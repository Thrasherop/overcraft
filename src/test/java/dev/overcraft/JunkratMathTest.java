package dev.overcraft;

import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.junkrat.Junkrat;
import dev.overcraft.junkrat.JunkratStats;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static dev.overcraft.junkrat.JunkratStats.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Junkrat's math (dev.overcraft.junkrat; tools/reference/junkrat-map.md). */
class JunkratMathTest {
	@Test
	void health() {
		// 250 = 20 Minecraft health, Damage, Specialist, RIP-Tire 2100
		assertEquals(250, Hero.JUNKRAT.owHealth);
		assertEquals(20f, Ow.hp(Hero.JUNKRAT.owHealth), 1e-5);
		assertEquals(Hero.Role.DAMAGE, Hero.JUNKRAT.role);
		assertEquals(2100, Combat.ultCost(Hero.JUNKRAT));
	}

	@Test
	void fragSplash() {
		// 80 to 0.5 m, 10 at 2 m, 45 at 1.25 m, none past 2 m; a direct hit 45 + 80 = 125
		assertEquals(80, FRAG_SPLASH * Junkrat.falloff(0.3, FRAG_SPLASH_FULL, FRAG_SPLASH_RADIUS, FRAG_SPLASH_EDGE), 1e-9);
		assertEquals(80, FRAG_SPLASH * Junkrat.falloff(0.5, FRAG_SPLASH_FULL, FRAG_SPLASH_RADIUS, FRAG_SPLASH_EDGE), 1e-9);
		assertEquals(45, FRAG_SPLASH * Junkrat.falloff(1.25, FRAG_SPLASH_FULL, FRAG_SPLASH_RADIUS, FRAG_SPLASH_EDGE), 1e-9);
		assertEquals(10, FRAG_SPLASH * Junkrat.falloff(2, FRAG_SPLASH_FULL, FRAG_SPLASH_RADIUS, FRAG_SPLASH_EDGE), 1e-9);
		assertEquals(0, Junkrat.falloff(2.01, FRAG_SPLASH_FULL, FRAG_SPLASH_RADIUS, FRAG_SPLASH_EDGE), 1e-9);
		assertEquals(125, FRAG_DIRECT + FRAG_SPLASH, 1e-9);
	}

	@Test
	void mineAndTireBlasts() {
		// the mine 120 to 1 m, 55 at 3 m; the tire 600 to 2 m, 325 at 6 m, 50 at 10 m
		assertEquals(120, MINE_DAMAGE * Junkrat.falloff(1, MINE_FULL, MINE_RADIUS_BLAST, MINE_EDGE_DAMAGE / MINE_DAMAGE), 1e-9);
		assertEquals(87.5, MINE_DAMAGE * Junkrat.falloff(2, MINE_FULL, MINE_RADIUS_BLAST, MINE_EDGE_DAMAGE / MINE_DAMAGE), 1e-9);
		assertEquals(55, MINE_DAMAGE * Junkrat.falloff(3, MINE_FULL, MINE_RADIUS_BLAST, MINE_EDGE_DAMAGE / MINE_DAMAGE), 1e-9);
		assertEquals(600, TIRE_DAMAGE * Junkrat.falloff(2, TIRE_FULL, TIRE_RADIUS, TIRE_EDGE_DAMAGE / TIRE_DAMAGE), 1e-9);
		assertEquals(325, TIRE_DAMAGE * Junkrat.falloff(6, TIRE_FULL, TIRE_RADIUS, TIRE_EDGE_DAMAGE / TIRE_DAMAGE), 1e-9);
		assertEquals(50, TIRE_DAMAGE * Junkrat.falloff(10, TIRE_FULL, TIRE_RADIUS, TIRE_EDGE_DAMAGE / TIRE_DAMAGE), 1e-9);
	}

	@Test
	void totalMayhem() {
		// his blasts damage enemies, never him; not a bystander or ally either
		assertTrue(Junkrat.blastDamages(false, true));
		assertFalse(Junkrat.blastDamages(true, false));
		assertFalse(Junkrat.blastDamages(true, true));
		assertFalse(Junkrat.blastDamages(false, false));
	}

	@Test
	void fragBounces() {
		// a first ground bounce bounces, a second goes off; a wall never; a wall then the ground goes off
		assertFalse(Junkrat.bounceBursts(0, true));
		assertTrue(Junkrat.bounceBursts(1, true));
		assertFalse(Junkrat.bounceBursts(0, false));
		assertFalse(Junkrat.bounceBursts(3, false));
		assertTrue(Junkrat.bounceBursts(1, true));
		// off the floor: half the fall back up, 0.75 of the run kept
		Vec3 off = Junkrat.bounce(new Vec3(1, -2, 0), new Vec3(0, 1, 0));
		assertEquals(0.75, off.x, 1e-9);
		assertEquals(1.0, off.y, 1e-9);
		// off a wall facing +x
		Vec3 wall = Junkrat.bounce(new Vec3(-2, 0, 1), new Vec3(1, 0, 0));
		assertEquals(1.0, wall.x, 1e-9);
		assertEquals(0.75, wall.z, 1e-9);
	}

	@Test
	void mineRules() {
		// set off from 0.1 s (2 ticks) after the throw, only with one out; launches him within 3.5 m
		assertFalse(Junkrat.canDetonate(true, 1));
		assertTrue(Junkrat.canDetonate(true, 2));
		assertFalse(Junkrat.canDetonate(false, 100));
		assertTrue(Junkrat.mineLaunches(3.4));
		assertFalse(Junkrat.mineLaunches(3.5));
		assertEquals(2, MINE_CHARGES);
	}

	@Test
	void reloadAndSpecialist() {
		// 1.5 s, 1.0 s with Specialist's +50 %; the frags back at 0.867 s either way
		assertEquals(30, Junkrat.reloadTicks(false), 1e-9);
		assertEquals(20, Junkrat.reloadTicks(true), 1e-9);
		assertTrue(RELOAD_AMMO_AT * 20 < Junkrat.reloadTicks(true));
		assertEquals(0.867, RELOAD_AMMO_AT, 1e-3);
	}

	@Test
	void nitro() {
		// +100 % at the start, +62.5 % at 1 s, +25 % from 2 s to 2.5 s, then none
		assertEquals(1.0, Junkrat.nitroShare(0), 1e-9);
		assertEquals(0.625, Junkrat.nitroShare(20), 1e-9);
		assertEquals(0.25, Junkrat.nitroShare(45), 1e-9);
		assertEquals(0, Junkrat.nitroShare(50), 1e-9);
		assertEquals(0, Junkrat.nitroShare(-1), 1e-9);
		assertEquals(0.5, JunkratStats.NITRO_DAMAGE, 1e-9);
	}
}
