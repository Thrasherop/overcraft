package dev.overcraft;

import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroState;
import dev.overcraft.pharah.Pharah;
import dev.overcraft.tracer.Tracer;
import dev.overcraft.tracer.TracerStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tracer's math (dev.overcraft.tracer; tools/reference/tracer-map.md). */
class TracerMathTest {
	@Test
	void health() {
		// 175 = 14 Minecraft health (7 hearts), Damage, Flanker
		assertEquals(175, Hero.TRACER.owHealth);
		assertEquals(14f, Ow.hp(Hero.TRACER.owHealth), 1e-5);
		assertEquals(Hero.Role.DAMAGE, Hero.TRACER.role);
		assertEquals(1375, Combat.ultCost(Hero.TRACER));
	}

	@Test
	void pistolFalloff() {
		// 5.75 to 10 m, 1.725 from 20 m, linear between: 3.7375 at 15 m
		assertEquals(5.75, TracerStats.BULLET_DAMAGE * Tracer.falloff(10), 1e-9);
		assertEquals(3.7375, TracerStats.BULLET_DAMAGE * Tracer.falloff(15), 1e-9);
		assertEquals(1.725, TracerStats.BULLET_DAMAGE * Tracer.falloff(20), 1e-9);
		assertEquals(1.725, TracerStats.BULLET_DAMAGE * Tracer.falloff(60), 1e-9);
		// 20 shots of 2 bullets a second: 230 a second at full, the magazine's 40 in 1 s at 2 a shot
		assertEquals(230, TracerStats.BULLET_DAMAGE * TracerStats.BULLETS * TracerStats.RATE, 1e-9);
		assertEquals(1.0, TracerStats.AMMO / (double) TracerStats.AMMO_PER_SHOT / TracerStats.RATE, 1e-9);
	}

	@Test
	void bombFalloff() {
		// 350 to 2 m, 210 at 3 m, 70 at 4 m, none past
		double at = TracerStats.BOMB_DAMAGE;
		assertEquals(350, at * Pharah.splashFactor(1.5, TracerStats.BOMB_FULL, TracerStats.BOMB_RADIUS_OUT, TracerStats.BOMB_EDGE), 1e-9);
		assertEquals(210, at * Pharah.splashFactor(3, TracerStats.BOMB_FULL, TracerStats.BOMB_RADIUS_OUT, TracerStats.BOMB_EDGE), 1e-9);
		assertEquals(70, at * Pharah.splashFactor(4, TracerStats.BOMB_FULL, TracerStats.BOMB_RADIUS_OUT, TracerStats.BOMB_EDGE), 1e-9);
		assertEquals(0, Pharah.splashFactor(4.01, TracerStats.BOMB_FULL, TracerStats.BOMB_RADIUS_OUT, TracerStats.BOMB_EDGE), 1e-9);
		// on herself half: 175 point blank, her whole health
		assertEquals(175, at * TracerStats.BOMB_SELF, 1e-9);
	}

	@Test
	void blinkGate() {
		HeroState s = new HeroState();
		assertEquals(3, s.tracerBlinks);
		assertTrue(Tracer.canBlink(s, false, false, false, 100));
		s.tracerBlinkReady = 102;
		assertFalse(Tracer.canBlink(s, false, false, false, 101));
		assertTrue(Tracer.canBlink(s, false, false, false, 102));
		assertFalse(Tracer.canBlink(s, true, false, false, 102));
		s.tracerBlinks = 0;
		assertFalse(Tracer.canBlink(s, false, false, false, 200));
		// the run is 6.0, back 5.4 (90 %), crouching 3 like everyone
		assertEquals(5.4, TracerStats.RUN_SPEED * dev.overcraft.hero.OwMovement.BACK_FACTOR, 1e-9);
	}
}
