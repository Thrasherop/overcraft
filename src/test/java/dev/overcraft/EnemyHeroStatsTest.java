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
}
