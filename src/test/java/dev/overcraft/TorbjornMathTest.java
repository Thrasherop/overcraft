package dev.overcraft;

import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.torbjorn.Torbjorn;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.overcraft.torbjorn.TorbjornStats.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Torbjörn's math (dev.overcraft.torbjorn; tools/reference/torbjorn-map.md). */
class TorbjornMathTest {
	@Test
	void health() {
		// 225 + 75 armor = 300 = 24 Minecraft health, the armor the top layer; Damage, Specialist; Molten Core 2350
		assertEquals(300, Hero.TORBJORN.owHealth);
		assertEquals(75, Hero.TORBJORN.armor());
		assertEquals(24f, Ow.hp(Hero.TORBJORN.owHealth), 1e-5);
		assertEquals(Hero.Role.DAMAGE, Hero.TORBJORN.role);
		assertEquals("specialist", Hero.TORBJORN.subRole);
		assertEquals(2350, Combat.ultCost(Hero.TORBJORN));
	}

	@Test
	void pelletFalloff() {
		// 12.5 to 10 m, 3.75 from 20 m, 8.125 half way
		assertEquals(12.5, PELLET_DAMAGE * Torbjorn.pelletFalloff(5), 1e-9);
		assertEquals(12.5, PELLET_DAMAGE * Torbjorn.pelletFalloff(10), 1e-9);
		assertEquals(8.125, PELLET_DAMAGE * Torbjorn.pelletFalloff(15), 1e-9);
		assertEquals(3.75, PELLET_DAMAGE * Torbjorn.pelletFalloff(20), 1e-9);
		assertEquals(3.75, PELLET_DAMAGE * Torbjorn.pelletFalloff(60), 1e-9);
		// all ten up close: 125
		assertEquals(125, PELLETS * PELLET_DAMAGE, 1e-9);
	}

	@Test
	void pelletPattern() {
		// ten, one along the aim, the rest within 4 degrees: four at 2, five at 4
		Vec3 aim = new Vec3(0.3, -0.2, 1).normalize();
		List<Vec3> dirs = Torbjorn.pelletDirections(aim);
		assertEquals(PELLETS, dirs.size());
		int half = 0, edge = 0;
		for (Vec3 d : dirs) {
			double deg = Math.toDegrees(Math.acos(Math.min(1, d.dot(aim))));
			assertTrue(deg <= PELLET_SPREAD + 1e-6, "pellet at " + deg);
			if (Math.abs(deg - Math.toDegrees(Math.atan(Math.tan(Math.toRadians(PELLET_SPREAD)) / 2))) < 0.01) {
				half++;
			} else if (Math.abs(deg - PELLET_SPREAD) < 0.01) {
				edge++;
			}
		}
		assertEquals(4, half);
		assertEquals(5, edge);
	}

	@Test
	void reloads() {
		// 2.0 s, the rivets back at 1.0 s; with Specialist's +50 % 1.33 s; Overload's +43 % attack rate makes it 1.4 s
		assertEquals(40, Torbjorn.reloadTicks(false), 1e-9);
		assertEquals(40 / 1.5, Torbjorn.reloadTicks(true), 1e-9);
		assertEquals(1.4, RELOAD / (1 + OVERLOAD_ATTACK), 0.01);
		// Overload's rivet: 0.51 / 1.43 = 0.357 s (W 0.368)
		assertEquals(0.357, RIVET_INTERVAL / (1 + OVERLOAD_ATTACK), 0.001);
	}

	@Test
	void turret() {
		// 225 health = 18 Minecraft health; 12 a shot, 4 a second: 48 / s (W); 150 more at level 3
		assertEquals(18f, Ow.hp(TURRET_HEALTH), 1e-5);
		assertEquals(46.9, TURRET_DAMAGE / TURRET_INTERVAL, 0.1);
		assertEquals(30f, Ow.hp(TURRET_HEALTH + L3_HEALTH), 1e-5);
		// Molten Core's pools: 190 / s, 38 per 0.2 s step
		assertEquals(38, POOL_DPS * 0.2, 1e-9);
	}
}
