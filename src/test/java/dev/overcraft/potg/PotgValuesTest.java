package dev.overcraft.potg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The owner's progression values (owner, 2026-11-11) as PlayScore scores them. Health in Overwatch HP (250 = 20 MC). */
class PotgValuesTest {
	@Test
	void killsOfTheRunsMobs() {
		assertEquals(250, PlayScore.killWorth("minecraft:zombie", 250), 1e-9); // no bonus
		assertEquals(1000, PlayScore.killWorth("minecraft:blaze", 250), 1e-9); // 250 + 750
		assertEquals(1000, PlayScore.killWorth("minecraft:enderman", 500), 1e-9); // 500 + 500
		assertEquals(450, PlayScore.killWorth("minecraft:wither_skeleton", 250), 1e-9); // 250 + 200
		assertEquals(2000, PlayScore.killWorth("minecraft:elder_guardian", 1000), 1e-9); // 1000 + 1000
		assertEquals(3000, PlayScore.killWorth("minecraft:wither", 3750), 1e-9); // capped 1000 + 2000
		assertEquals(2500, PlayScore.killWorth("minecraft:ender_dragon", 2500), 1e-9); // capped 1000 + 1500
	}

	@Test
	void pickupsAndEvents() {
		assertEquals(250, PotgValues.PICKUP.get("minecraft:blaze_rod"), 1e-9);
		assertEquals(150, PotgValues.PICKUP.get("minecraft:ender_pearl"), 1e-9);
		assertNull(PotgValues.PICKUP.get("minecraft:ender_eye"));
		assertEquals(400, PotgValues.END_CRYSTAL, 1e-9);
		assertEquals(100, PotgValues.EYE_PLACED, 1e-9);
		assertEquals(200, PotgValues.PORTAL_OPENED, 1e-9);
		assertEquals(500, PotgValues.FIRST_NETHER, 1e-9);
		assertEquals(1000, PotgValues.FIRST_END, 1e-9);
	}
}
