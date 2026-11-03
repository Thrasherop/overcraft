package dev.overcraft;

import dev.overcraft.dva.Dva;
import dev.overcraft.dva.DvaStats;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroBody;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Tank;
import net.minecraft.world.entity.Pose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** D.Va's two bodies' math (dev.overcraft.dva; tools/reference/dva-plan.md steps 1-3). */
class DvaMathTest {
	@Test
	void mechAndPilotHealth() {
		// the MEKA: 200 + the role's 150 + 325 armor = 675 = 54 Minecraft health (27 hearts); 27 pips: 14 health, 13 armor
		assertEquals(Hero.Role.TANK, Hero.DVA.role);
		assertEquals(675, Hero.DVA.owHealth);
		assertEquals(325, Hero.DVA.armor());
		assertEquals(54f, Ow.hp(Hero.DVA.owHealth), 1e-5);
		assertEquals(27, (int) Math.ceil(675 / 25.0));
		assertEquals(14, (int) Math.ceil((675 - 325) / 25.0));
		// the pilot: 175, no bonus, no armor = 14 Minecraft health (7 hearts), 7 pips
		assertEquals(175, DvaStats.PILOT_HEALTH);
		assertEquals(14f, Ow.hp(DvaStats.PILOT_HEALTH), 1e-5);
		assertEquals(7, (int) Math.ceil(175 / 25.0));
		// no player: the mech's (the select screen, the HUD before the form arrives)
		assertEquals(675, Dva.owHealth(null));
		assertEquals(325, Dva.armor(null));
		assertEquals(675, Hero.DVA.owHealth(null));
		assertEquals(250, Hero.KIRIKO.owHealth(null));
	}

	@Test
	void initiator() {
		assertTrue(Tank.initiator(Hero.DVA) && Tank.tank(Hero.DVA) && !Tank.stalwart(Hero.DVA));
		assertFalse(Tank.initiator(Hero.SIGMA));
		// heals when hurt and its 5 s are over; not at full, not before the 5 s
		assertTrue(Tank.initiatorReady(100, -1, 40, 54));
		assertFalse(Tank.initiatorReady(100, -1, 54, 54));
		assertFalse(Tank.initiatorReady(100, 101, 40, 54));
		assertTrue(Tank.initiatorReady(101, 101, 40, 54));
		assertEquals(50, DvaStats.INITIATOR_HEAL, 0);
		assertEquals(100, Ow.ticks(DvaStats.INITIATOR_LOCKOUT));
	}

	@Test
	void twoUltMeters() {
		HeroState s = new HeroState();
		s.reset(Hero.DVA);
		s.dvaForm = Dva.MECH;
		assertEquals(1700, Combat.ultCost(s));
		assertEquals(1700, Combat.ultCost(Hero.DVA));
		s.dvaForm = Dva.PILOT;
		assertEquals(280, Combat.ultCost(s));
		s.dvaForm = Dva.CALLING;
		assertEquals(280, Combat.ultCost(s));
		s.dvaForm = Dva.WRECK;
		assertEquals(1700, Combat.ultCost(s));
	}

	@Test
	void mechBody() {
		// the mover (D 0407): 1.4 x 2.3 m, the eye 1.74 m (041F); no crouch: crouched is the same box
		var standing = HeroBody.of(Hero.DVA, Pose.STANDING);
		var crouching = HeroBody.of(Hero.DVA, Pose.CROUCHING);
		assertEquals(1.4f, standing.width(), 1e-6);
		assertEquals(2.3f, standing.height(), 1e-6);
		assertEquals(1.74f, standing.eyeHeight(), 1e-6);
		assertEquals(standing, crouching);
		// no 1-wide door, no 2-high tunnel; a 2-wide, 3-high opening takes it
		assertTrue(standing.width() > 1 && standing.height() > 2 && standing.width() < 2 && standing.height() < 3);
		assertEquals(null, HeroBody.of(Hero.DVA, Pose.SWIMMING));
	}

	@Test
	void callMechTimings() {
		// the cast 0.2 s, the landing 0.9 s after the press (W 0.896), untargetable from 1.0 s, driving at 1.95 s (D 026A)
		assertEquals(4, Ow.ticks(DvaStats.CALL_CAST));
		assertEquals(18, Ow.ticks(DvaStats.CALL_CAST + DvaStats.CALL_LAND));
		assertEquals(20, Ow.ticks(DvaStats.CALL_CAST + DvaStats.CALL_UNTARGETABLE));
		assertEquals(39, Ow.ticks(DvaStats.CALL_CAST + DvaStats.CALL_LINK));
		// the wreck 1.0 s, the eject's 0.4 s immunity and 1 s Call Mech lock
		assertEquals(20, Ow.ticks(DvaStats.WRECK_TIME));
		assertEquals(8, Ow.ticks(DvaStats.EJECT_IMMUNE));
		assertEquals(20, Ow.ticks(DvaStats.EJECT_CALL_LOCK));
		// the eject's launch: 6 back, 7 up = 9.2 m/s
		assertEquals(9.22, Math.hypot(DvaStats.EJECT_BACK, DvaStats.EJECT_UP), 0.01);
	}
}
