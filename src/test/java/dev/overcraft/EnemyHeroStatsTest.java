package dev.overcraft;

import dev.overcraft.entity.enemy.EnemyHeroStats;
import dev.overcraft.junkrat.JunkratStats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The enemy heroes' own numbers (#46, tools/reference/enemy-heroes-plan.md). */
class EnemyHeroStatsTest {
	@Test
	void damageScalesByDifficulty() {
		assertEquals(0, EnemyHeroStats.playerDamageFactor(Difficulty.PEACEFUL));
		assertEquals(0.2, EnemyHeroStats.playerDamageFactor(Difficulty.EASY));
		assertEquals(0.3, EnemyHeroStats.playerDamageFactor(Difficulty.NORMAL));
		assertEquals(0.4, EnemyHeroStats.playerDamageFactor(Difficulty.HARD));
		// worse aim on easier difficulties
		assertTrue(EnemyHeroStats.extraSpread(Difficulty.EASY) > EnemyHeroStats.extraSpread(Difficulty.HARD));
	}

	@Test
	void pausesAreHalfASecondToASecond() {
		assertEquals(10, EnemyHeroStats.pauseTicks(0));
		assertEquals(20, EnemyHeroStats.pauseTicks(0.9999));
		for (double r = 0; r < 1; r += 0.01) {
			int t = EnemyHeroStats.pauseTicks(r);
			assertTrue(t >= 10 && t <= 20, "pause " + t);
		}
	}

	@Test
	void capAndRoll() {
		assertTrue(EnemyHeroStats.mayReplace(0, 0));
		assertTrue(EnemyHeroStats.mayReplace(0, 1));
		assertFalse(EnemyHeroStats.mayReplace(0, 2));
		assertFalse(EnemyHeroStats.mayReplace(1, 0));
		// the gamerule's cap
		assertFalse(EnemyHeroStats.mayReplace(0, 0, 0));
		assertTrue(EnemyHeroStats.mayReplace(0, 4, 5));
		assertFalse(EnemyHeroStats.mayReplace(0, 5, 5));
	}

	@Test
	void speedAttributeWalksAZombie() {
		// Minecraft's zombie, 0.23, walks about 2.33 m/s (44.05 x 0.23²)
		assertEquals(0.23, EnemyHeroStats.speedAttribute(2.33), 0.001);
	}

	@Test
	void lobLandsOnTheTarget() {
		double v = Ow.speed(JunkratStats.FRAG_SPEED), g = Ow.accel(JunkratStats.FRAG_GRAVITY);
		for (Vec3 to : new Vec3[] {new Vec3(10, -1.5, 0), new Vec3(4, 0, 7), new Vec3(15, 3, -2)}) {
			Vec3 dir = EnemyHeroStats.lob(to, v, g);
			// fly it as OwProjectile does (no drag, gravity a tick), and find its height where it reaches the target's distance
			Vec3 p = Vec3.ZERO, vel = dir.scale(v);
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			for (int t = 0; t < 200 && Math.sqrt(p.x * p.x + p.z * p.z) < flat; t++) {
				vel = vel.add(0, -g, 0);
				p = p.add(vel);
			}
			assertEquals(to.y, p.y, 0.6, "lands near " + to);
		}
	}

	// ---- batch 2 ----

	@Test
	void asheCoachesOnlyUpClose() {
		assertTrue(EnemyHeroStats.asheCoach(4, true, 0));
		assertTrue(EnemyHeroStats.asheCoach(5, true, 0));
		assertFalse(EnemyHeroStats.asheCoach(6, true, 0));
		assertFalse(EnemyHeroStats.asheCoach(3, false, 0));
		assertFalse(EnemyHeroStats.asheCoach(3, true, 1));
	}

	@Test
	void sojournChargedShotIsTwentyPlusEnergy() {
		assertEquals(20, EnemyHeroStats.sojournCharged(0), 1e-9);
		assertEquals(80, EnemyHeroStats.sojournCharged(60), 1e-9);
		assertEquals(120, EnemyHeroStats.sojournCharged(100), 1e-9);
		// clamped to her 100
		assertEquals(120, EnemyHeroStats.sojournCharged(140), 1e-9);
	}

	@Test
	void sojournEnergyHoldsNineSecondsThenDrains() {
		assertEquals(50, EnemyHeroStats.sojournEnergyTick(50, 0), 1e-9);
		assertEquals(50, EnemyHeroStats.sojournEnergyTick(50, 179), 1e-9);
		assertEquals(50 - 0.75, EnemyHeroStats.sojournEnergyTick(50, 180), 1e-9);
		assertEquals(0, EnemyHeroStats.sojournEnergyTick(0.5, 400), 1e-9);
	}

	@Test
	void widowmakerShotIsHalfAFullCharge() {
		// a full charge's 120, x2.5 on the head (WidowmakerStats), halved (ours)
		assertEquals(60, EnemyHeroStats.widowShot(30, false), 1e-9);
		assertEquals(150, EnemyHeroStats.widowShot(30, true), 1e-9);
		// her 50-70 m falloff to half
		assertEquals(30, EnemyHeroStats.widowShot(80, false), 1e-9);
		// her aim follows, then holds still its last 0.25 s (5 ticks of 30)
		assertTrue(EnemyHeroStats.widowTracks(0));
		assertTrue(EnemyHeroStats.widowTracks(24));
		assertFalse(EnemyHeroStats.widowTracks(25));
	}

	@Test
	void rivetAndTurretLobsLand() {
		lands(Ow.speed(dev.overcraft.torbjorn.TorbjornStats.RIVET_SPEED), Ow.accel(dev.overcraft.torbjorn.TorbjornStats.RIVET_GRAVITY),
			new Vec3[] {new Vec3(18, 0, 0), new Vec3(6, -2, 5), new Vec3(10, 4, -3)});
		lands(Ow.speed(dev.overcraft.torbjorn.TorbjornStats.TURRET_THROW_SPEED), Ow.accel(dev.overcraft.torbjorn.TorbjornStats.TURRET_GRAVITY),
			new Vec3[] {new Vec3(3, -1.1, 0), new Vec3(-2, -1.1, 2)});
	}

	@Test
	void asheLaunchSparesHerUntilShesLandedOrAGraceOnTheGround() {
		// in the air: held
		assertTrue(EnemyHeroStats.asheLaunchHeld(true, false, 3));
		// up and back down: done
		assertFalse(EnemyHeroStats.asheLaunchHeld(true, true, 8));
		// a flat launch that never left the ground: held through the grace, not the first grounded tick
		assertTrue(EnemyHeroStats.asheLaunchHeld(false, true, 1));
		assertTrue(EnemyHeroStats.asheLaunchHeld(false, true, EnemyHeroStats.ASHE_LAUNCH_GRACE - 1));
		assertFalse(EnemyHeroStats.asheLaunchHeld(false, true, EnemyHeroStats.ASHE_LAUNCH_GRACE));
		// slid off a ledge after the grace began: in the air again, held
		assertTrue(EnemyHeroStats.asheLaunchHeld(false, false, 10));
	}

	@Test
	void scanSkipsCreativeAndSpectators() {
		assertTrue(EnemyHeroStats.scanMayPick(true, false, false));
		assertFalse(EnemyHeroStats.scanMayPick(true, true, false));
		assertFalse(EnemyHeroStats.scanMayPick(true, false, true));
		assertTrue(EnemyHeroStats.scanMayPick(false, false, false));
	}

	@Test
	void turretGoesWhenItsTorbjornHasAnother() {
		assertTrue(EnemyHeroStats.turretStays(true, true, 0));
		assertFalse(EnemyHeroStats.turretStays(true, false, 0));
		// he isn't loaded yet: it waits 2 s
		assertTrue(EnemyHeroStats.turretStays(false, false, 39));
		assertFalse(EnemyHeroStats.turretStays(false, false, 40));
	}

	private static void lands(double v, double g, Vec3[] targets) {
		for (Vec3 to : targets) {
			Vec3 dir = EnemyHeroStats.lob(to, v, g);
			Vec3 p = Vec3.ZERO, vel = dir.scale(v);
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			Vec3 before = p;
			for (int t = 0; t < 400 && Math.sqrt(p.x * p.x + p.z * p.z) < flat; t++) {
				before = p;
				vel = vel.add(0, -g, 0);
				p = p.add(vel);
			}
			// its height where its path crosses the target's distance, between the ticks either side (a fast rivet steps 3.5 m a tick)
			double a = Math.sqrt(before.x * before.x + before.z * before.z), b = Math.sqrt(p.x * p.x + p.z * p.z);
			double y = b - a < 1.0E-9 ? p.y : before.y + (p.y - before.y) * (flat - a) / (b - a);
			assertEquals(to.y, y, 0.6, "lands near " + to);
		}
	}
}
