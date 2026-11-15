package dev.overcraft;

import dev.overcraft.illari.Illari;
import dev.overcraft.illari.IllariStats;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Illari's numbers (IllariStats) through her kit's math. */
class IllariMathTest {
	@Test
	void rifleDamageFollowsTheDataCurve() {
		// 25 at 40 % charge, 50 at 70 %, 75 at full (D 3A65 #3042)
		assertEquals(25, Illari.chargedDamage(0.4), 1e-9);
		assertEquals(50, Illari.chargedDamage(0.7), 1e-9);
		assertEquals(75, Illari.chargedDamage(1), 1e-9);
		assertEquals(37.5, Illari.chargedDamage(0.55), 1e-9);
	}

	@Test
	void rifleFalloff() {
		assertEquals(1, Illari.falloff(30), 1e-9);
		assertEquals(0.65, Illari.falloff(40), 1e-9);
		assertEquals(0.3, Illari.falloff(60), 1e-9);
	}

	@Test
	void pressFiresFromFortyPercentHeldAtFull() {
		assertFalse(Illari.fires(true, true, 0.39));
		assertTrue(Illari.fires(true, true, 0.4));
		assertFalse(Illari.fires(false, true, 0.9));
		assertTrue(Illari.fires(false, true, 1));
	}

	@Test
	void outburstLaunch() {
		assertEquals(new Vec3(0, 6, 0), Illari.outburstVelocity(0, 0, false));
		assertEquals(new Vec3(0, 13.5, 0), Illari.outburstVelocity(0, 0, true));
		Vec3 tap = Illari.outburstVelocity(1, 0, false);
		assertEquals(13.5, tap.length(), 1e-9);
		assertEquals(0.6, tap.y / tap.x, 1e-9);
		Vec3 held = Illari.outburstVelocity(0, 1, true);
		assertEquals(2, held.y / held.z, 1e-9);
	}

	@Test
	void sunstruck() {
		assertFalse(Illari.sunstruckReady(89.9));
		assertTrue(Illari.sunstruckReady(90));
		assertEquals(160, Illari.sunstruckDamage(0.5), 1e-9);
		assertEquals(140, Illari.sunstruckDamage(3), 1e-9);
		assertEquals(120, Illari.sunstruckDamage(5), 1e-9);
		assertEquals(0, Illari.sunstruckDamage(5.1), 1e-9);
		assertEquals(0.4, Illari.markSlow(0.5), 1e-9);
		assertEquals(0.2, Illari.markSlow(3.5), 1e-9);
		assertEquals(0, Illari.markSlow(IllariStats.MARK_TIME), 1e-9);
	}

	@Test
	void outburstPushShare() {
		assertEquals(1, Illari.share(5, 5.6, 8, 0.3), 1e-9);
		assertEquals(0.3, Illari.share(8, 5.6, 8, 0.3), 1e-9);
	}

	@Test
	void flightSpeed() {
		assertEquals(5.5 * 1.45, Illari.flightSpeed(false), 1e-9);
		assertEquals(5.5 * 1.45 * 1.2, Illari.flightSpeed(true), 1e-9);
	}
}
