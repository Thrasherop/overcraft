package dev.overcraft;

import dev.overcraft.ashe.Ashe;
import dev.overcraft.ashe.AsheStats;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ashe's numbers (AsheStats) through her kit's math. */
class AsheMathTest {
	@Test
	void viperDamageAndFalloff() {
		// hip 35 to 20 m, 10.5 from 40 m; scoped 75 to 35 m, 22.5 from 55 m; x2 on the head (W)
		assertEquals(35, Ashe.viperDamage(10, false, false), 1e-9);
		assertEquals(10.5, Ashe.viperDamage(60, false, false), 1e-9);
		assertEquals(22.75, Ashe.viperDamage(30, false, false), 1e-9);
		assertEquals(75, Ashe.viperDamage(35, true, false), 1e-9);
		assertEquals(150, Ashe.viperDamage(20, true, true), 1e-9);
		assertEquals(22.5, Ashe.viperDamage(80, true, false), 1e-9);
	}

	@Test
	void reloadShellByShell() {
		// the first shell at 0.5 s, then one each 0.25 s: 12 from empty at 3.25 s, then the 0.3 s end (D 14FC; W)
		assertEquals(10, Ashe.shellTick(1));
		assertEquals(15, Ashe.shellTick(2));
		assertEquals(65, Ashe.shellTick(12));
		assertEquals(3.55, Ashe.reloadSeconds(0), 1e-9);
		assertEquals(0.8, Ashe.reloadSeconds(11), 1e-9);
		assertEquals(0, Ashe.reloadSeconds(12), 1e-9);
	}

	@Test
	void scopeTakesItsTime() {
		double s = 0;
		int ticks = 0;
		while (s < 1) {
			s = Ashe.scopeStep(s, true);
			ticks++;
		}
		assertEquals(4, ticks); // 0.2 s
		ticks = 0;
		while (s > 0) {
			s = Ashe.scopeStep(s, false);
			ticks++;
		}
		assertEquals(4, ticks); // 0.1666 s, rounded up to the tick
	}

	@Test
	void coachGunPelletsAndPush() {
		List<Vec3> p = Ashe.pellets(new Vec3(0, 0, 1), AsheStats.COACH_SPREAD / 2);
		assertEquals(AsheStats.COACH_PELLETS, p.size());
		assertEquals(1, p.get(0).z, 1e-9);
		for (Vec3 d : p) {
			// all inside the 20 degree cone
			assertTrue(d.z >= Math.cos(Math.toRadians(AsheStats.COACH_SPREAD)) - 1e-9);
		}
		assertEquals(90, AsheStats.COACH_PELLET * AsheStats.COACH_PELLETS, 1e-9);
		assertEquals(1, Ashe.coachPushShare(8), 1e-9);
		assertEquals(0.5, Ashe.coachPushShare(11.5), 1e-9);
		assertEquals(0, Ashe.coachPushShare(15), 1e-9);
		// her own: 18 m/s back and a little up, along the aim's reverse
		Vec3 self = Ashe.coachSelf(new Vec3(0, -1, 0));
		assertEquals(18, self.length(), 1e-9);
		assertTrue(self.y > 17.9);
	}

	@Test
	void dynamiteBlastAndBurn() {
		// 50 to 1.5 m, 20 at 5 m, none beyond; Airburst x1.4 reach (W 5 -> 7 m)
		assertEquals(50, Ashe.blastDamage(1, 1), 1e-9);
		assertEquals(35, Ashe.blastDamage(3.25, 1), 1e-9);
		assertEquals(20, Ashe.blastDamage(5, 1), 1e-9);
		assertEquals(0, Ashe.blastDamage(5.1, 1), 1e-9);
		assertEquals(20, Ashe.blastDamage(7, AsheStats.AIRBURST_RADIUS), 1e-9);
		// the burn in 25 steps of 0.2 s: 4 each, 100 in all
		assertEquals(25, Ashe.burnSteps(AsheStats.BURN_TIME));
		assertEquals(100, AsheStats.BURN_TOTAL / Ashe.burnSteps(AsheStats.BURN_TIME) * 25, 1e-9);
	}

	@Test
	void sharpshooterCutsCoachGun() {
		// a 150 scoped headshot takes 1.125 s off (D 0.0075 a point): 22.5 ticks
		assertEquals(22.5, Ashe.sharpshooterCut(150), 1e-9);
		assertEquals(0, Ashe.sharpshooterCut(-5), 1e-9);
	}

	@Test
	void bobNumbers() {
		// 1150 health = 92 Minecraft, 136 a second from his cannons (W), 15 s out in all
		assertEquals(92, Ow.hp(AsheStats.BOB_HEALTH), 1e-4);
		assertEquals(136, AsheStats.BOB_SHOT * AsheStats.BOB_RATE, 1e-9);
		assertEquals(15, AsheStats.BOB_DROP + AsheStats.BOB_CHARGE_TIME + AsheStats.BOB_FIRE_TIME + AsheStats.BOB_DESPAWN, 1e-9);
	}
}
