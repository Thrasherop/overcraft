package dev.overcraft;

import dev.overcraft.tracer.Blink;
import dev.overcraft.tracer.Tracer;
import dev.overcraft.tracer.TracerStats;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Blink's path on a block grid, her charges' one count, Recall's choice and the pistols' head multiplier: the critic's d0c7e1f
 * mutations (Recall to the newest sample, the climb's headroom removed, headshot x1) and findings #1, #3, #4 each fail one here.
 */
class BlinkTest {
	/** Solid unit blocks at integer corners; a floor at y = -1 under everything (|x|, |z| <= 40). */
	private static final class Grid implements Blink.Space {
		final Set<Long> solid = new HashSet<>();
		final java.util.List<AABB> shapes = new java.util.ArrayList<>();

		Grid put(int x, int y, int z) {
			solid.add(key(x, y, z));
			return this;
		}

		static long key(int x, int y, int z) {
			return ((long) (x + 512) << 40) | ((long) (y + 512) << 20) | (z + 512);
		}

		@Override
		public boolean free(AABB box) {
			for (AABB shape : shapes) {
				if (shape.intersects(box)) {
					return false;
				}
			}
			for (int x = (int) Math.floor(box.minX); x < box.maxX; x++) {
				for (int y = (int) Math.floor(box.minY); y < box.maxY; y++) {
					for (int z = (int) Math.floor(box.minZ); z < box.maxZ; z++) {
						if (y == -1 || solid.contains(key(x, y, z))) {
							return false;
						}
					}
				}
			}
			return true;
		}
	}

	private static AABB box(Vec3 feet) {
		return new AABB(feet.x - 0.3, feet.y, feet.z - 0.3, feet.x + 0.3, feet.y + 1.8, feet.z + 0.3);
	}

	private static Vec3 blink(Grid g, Vec3 from, Vec3 dir) {
		return Blink.destination(g, from, box(from), true, dir.normalize(), TracerStats.BLINK_DISTANCE);
	}

	@Test
	void openField() {
		Vec3 to = blink(new Grid(), new Vec3(0.5, 0, 0.5), new Vec3(0, 0, 1));
		assertEquals(8.0, to.z, 1e-6);
		assertEquals(0, to.y, 1e-6);
	}

	@Test
	void upAStaircase() {
		// +1 a block from z = 2 (critic C1a: she stopped at y 1.5, z 3.2): she follows it all the way up
		Grid g = new Grid();
		for (int i = 0; i < 10; i++) {
			for (int y = 0; y <= i; y++) {
				g.put(0, y, 2 + i);
			}
		}
		Vec3 to = blink(g, new Vec3(0.5, 0, 0.5), new Vec3(0, 0, 1));
		assertTrue(to.y >= 5 - 1e-6, "up the stairs: " + to);
		assertTrue(to.z > 6.5, "along them: " + to);
	}

	@Test
	void notUpAWall() {
		// a 2-high wall at z = 3: more than a step, she stops against it, not inside or over it
		Grid g = new Grid().put(0, 0, 3).put(0, 1, 3);
		for (int x = -3; x <= 3; x++) {
			g.put(x, 0, 3).put(x, 1, 3);
		}
		Vec3 to = blink(g, new Vec3(0.5, 0, 0.5), new Vec3(0, 0, 1));
		assertEquals(0, to.y, 1e-6);
		assertTrue(to.z <= 2.7 + 1e-6 && to.z > 2.5, "against it: " + to);
	}

	@Test
	void headroomBeforeAStep() {
		// a ceiling 2 m up that ends at z 1.25, a 1 m step from z 1.85 with open air over it. Where she meets the step her box is
		// still under the ceiling's edge (no room to rise there) though the raised place past it is clear: she stops rather than
		// rising through the ceiling's edge (the critic's mutation removed this check). With whole blocks her 0.6 m box never
		// meets this (the raised place overlaps the same ceiling), hence the shapes.
		Grid g = new Grid();
		g.shapes.add(new AABB(-3, 2, -3, 3, 3, 1.25));
		g.shapes.add(new AABB(-3, 0, 1.85, 3, 1, 12));
		Vec3 to = blink(g, new Vec3(0.5, 0, 0.5), new Vec3(0, 0, 1));
		assertEquals(0, to.y, 1e-6, "under the ceiling still: " + to);
		assertTrue(to.z < 1.6, "stopped at the step: " + to);
	}

	@Test
	void slidesAlongAWall() {
		// a wall along x = -1 (her left, facing +z), the Blink diagonal into it (W+A): she slides along it (critic C2d: ~0 m)
		Grid g = new Grid();
		for (int z = -2; z <= 12; z++) {
			g.put(1, 0, z).put(1, 1, z).put(1, 2, z);
		}
		Vec3 from = new Vec3(0.69, 0, 0.5);
		Vec3 to = blink(g, from, new Vec3(1, 0, 1));
		assertTrue(to.z - from.z > 4.5, "slid along: " + to);
		assertTrue(to.x <= 0.7 + 1e-6, "not into it: " + to);
	}

	@Test
	void straightIntoAWall() {
		// straight at the wall: nowhere (as Overwatch's: the charge is still spent, ClientTracer)
		Grid g = new Grid();
		for (int z = -2; z <= 12; z++) {
			g.put(1, 0, z).put(1, 1, z).put(1, 2, z);
		}
		Vec3 from = new Vec3(0.69, 0, 0.5);
		Vec3 to = blink(g, from, new Vec3(1, 0, 0));
		assertEquals(from.x, to.x, 0.02);
		assertEquals(from.z, to.z, 1e-6);
	}

	@Test
	void downASlope() {
		// a 1-block drop at z = 3: she follows it down and lands on it
		Grid g = new Grid();
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				g.put(x, 0, z);
			}
		}
		Vec3 to = Blink.destination(g, new Vec3(0.5, 1, 0.5), box(new Vec3(0.5, 1, 0.5)), true, new Vec3(0, 0, 1), TracerStats.BLINK_DISTANCE);
		assertEquals(0, to.y, 1e-6);
		assertEquals(8.0, to.z, 1e-6);
	}

	@Test
	void oneCount() {
		// critic #1: Blink, wait for the recharge, Blink, five times: each one goes (the count was 3 each press, and the old
		// client's own count ran out on the 4th)
		int sent = 0, seen = 0, charges = 3;
		for (int i = 0; i < 5; i++) {
			assertTrue(Tracer.clientMay(charges, sent, seen), "press " + (i + 1));
			sent++;
			// the server sees it, takes a charge, it comes back 3 s later
			seen++;
		}
		// three fast presses before any sync: the 4th can't go
		seen = sent;
		for (int i = 0; i < 3; i++) {
			assertTrue(Tracer.clientMay(charges, sent, seen));
			sent++;
		}
		assertFalse(Tracer.clientMay(charges, sent, seen));
		// the server's sync: 0 charges, all seen: still no
		assertFalse(Tracer.clientMay(0, sent, sent));
		// across the mod-256 wrap
		assertEquals(2, Tracer.pending(1, 255));
	}

	@Test
	void recallTakesTheOldest() {
		ArrayDeque<Tracer.Sample> h = new ArrayDeque<>();
		for (int t = 0; t < 60; t++) {
			h.addLast(new Tracer.Sample(t, new Vec3(t, 0, 0), 0, 0, Vec3.ZERO, t == 20 ? 175 : 100));
		}
		assertEquals(0, Tracer.recallTo(h).pos().x, 1e-9);
		assertEquals(175, Tracer.recallHealth(h, 90), 1e-9);
		assertEquals(150, Tracer.recallHealth(new ArrayDeque<>(), 150), 1e-9);
	}

	@Test
	void headshotDoubles() {
		assertEquals(11.5, Tracer.bulletDamage(5, true), 1e-9);
		assertEquals(5.75, Tracer.bulletDamage(5, false), 1e-9);
		assertEquals(2 * Tracer.bulletDamage(15, false), Tracer.bulletDamage(15, true), 1e-9);
	}
}
