package dev.overcraft.sojourn;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sojourn's Railgun and Charged Shot numbers (SojournStats; tools/reference/sojourn.md). */
class RailgunMathTest {
	@Test
	void chargedShotOutranksAllButItsRecoveryAndOverclocksCast() {
		// #126, D 1A23: SECONDARY #421 hangs off #615 (v2447 = 0) alone, beside the reload and the rounds
		dev.overcraft.hero.HeroState s = new dev.overcraft.hero.HeroState();
		s.sojournUlt = -1;
		assertTrue(Railgun.chargedReady(s), "idle");
		s.primaryReload = 5;
		assertTrue(Railgun.chargedReady(s), "mid-reload");
		s.primaryReload = -1;
		s.weaponRecovery = 8;
		assertTrue(Railgun.chargedReady(s), "a melee's recovery");
		s.weaponRecovery = 0;
		s.sojournSlide = 5;
		s.sojournDisruptor = 1;
		assertTrue(Railgun.chargedReady(s), "sliding, in Disruptor's cast");
		s.sojournChargedLock = 1;
		assertFalse(Railgun.chargedReady(s), "its own recovery");
		s.sojournChargedLock = 0;
		s.sojournUlt = 5;
		assertFalse(Railgun.chargedReady(s), "Overclock's 0.75 s cast");
		s.sojournUlt = 15;
		assertTrue(Railgun.chargedReady(s), "in Overclock, past its cast");
	}

	@Test
	void rightButtonPressRules() {
		assertTrue(Railgun.rightPressed(true, false, false), "goes down");
		assertFalse(Railgun.rightPressed(true, true, false), "stays down");
		assertTrue(Railgun.rightPressed(true, true, true), "still down as the recovery ends: #421 begins again");
		assertFalse(Railgun.rightPressed(false, true, true), "let go before it ended");
		assertFalse(Railgun.rightPressed(false, false, false), "up");
	}

	@Test
	void chargedSoundByEnergy() {
		assertEquals("charged.fire_low", Railgun.chargedFireSound(3));
		assertEquals("charged.fire_mid", Railgun.chargedFireSound(50));
		assertEquals("charged.fire", Railgun.chargedFireSound(100));
		assertEquals("charged.fire", Railgun.chargedFireSound(150));
	}

	@Test
	void chargedShotFalloff() {
		// D 1CE6 40 m / 1CE5 60 m / 1CE7 0.5: full to 40, half from 60, linear between
		assertEquals(1, Railgun.falloff(10), 1e-9);
		assertEquals(1, Railgun.falloff(40), 1e-9);
		assertEquals(0.75, Railgun.falloff(50), 1e-9);
		assertEquals(0.5, Railgun.falloff(60), 1e-9);
		assertEquals(0.5, Railgun.falloff(90), 1e-9);
	}

	@Test
	void spreadStaysInsideItsEllipse() {
		// 1.6 degrees up and down, 0.64 sideways (W's split on D's 1.6)
		RandomSource random = RandomSource.create(7);
		Vec3 ahead = new Vec3(0, 0, 1);
		double up = 0, side = 0;
		for (int i = 0; i < 20000; i++) {
			Vec3 d = Railgun.spread(ahead, 1.6, 0.64, random);
			double a = Math.toDegrees(Math.atan2(d.y, d.z)), b = Math.toDegrees(Math.atan2(Math.abs(d.x), d.z));
			up = Math.max(up, Math.abs(a));
			side = Math.max(side, b);
		}
		assertTrue(up <= 1.6 + 1e-6 && up > 1.55, "vertical " + up);
		assertTrue(side <= 0.64 + 1e-6 && side > 0.6, "sideways " + side);
		assertEquals(ahead, Railgun.spread(ahead, 0, 0, random));
	}
}
