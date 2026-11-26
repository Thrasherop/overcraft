package dev.overcraft.hero;

import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** #186: a drawn head turned with its mob's body. The sheep's head from head_boxes.json (Minecraft 26.3's SheepModel). */
class HeadBoxesTest {
	private static final float[] SHEEP = {-0.187f, 1.001f, 0.375f, 0.188f, 1.376f, 0.875f};

	private static void near(double want, double got) {
		assertEquals(want, got, 1.0E-3);
	}

	@Test
	void facingSouthTheHeadIsAheadOnPlusZ() {
		AABB h = HeadBoxes.place(SHEEP, 10, 64, 20, 0, 1);
		near(20.375, h.minZ);
		near(20.875, h.maxZ);
		near(65.001, h.minY);
		near(65.376, h.maxY);
	}

	@Test
	void facingWestTheHeadIsOnMinusX() {
		// yaw 90 faces -x (Minecraft: forward is (-sin yaw, cos yaw))
		AABB h = HeadBoxes.place(SHEEP, 10, 64, 20, 90, 1);
		near(10 - 0.875, h.minX);
		near(10 - 0.375, h.maxX);
		near(20 - 0.188, h.minZ);
		near(20 + 0.187, h.maxZ);
	}

	@Test
	void scaleGrowsItFromTheFeet() {
		AABB h = HeadBoxes.place(SHEEP, 0, 0, 0, 0, 2);
		near(0.75, h.minZ);
		near(1.75, h.maxZ);
		near(2.752, h.maxY);
	}
}
