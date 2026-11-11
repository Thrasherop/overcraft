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

	@Test
	void fusionVolley() {
		// 11 pellets: the middle, 4 at half the cone, 6 at its edge (D 050A #444)
		net.minecraft.world.phys.Vec3 look = new net.minecraft.world.phys.Vec3(0, 0, 1);
		var dirs = dev.overcraft.dva.FusionCannons.pattern(look, DvaStats.FUSION_SPREAD, 0.3);
		assertEquals(11, dirs.size());
		int middle = 0, half = 0, edge = 0;
		for (var d : dirs) {
			double deg = Math.toDegrees(Math.acos(Math.min(1, d.dot(look))));
			if (deg < 0.01) {
				middle++;
			} else if (Math.abs(deg - DvaStats.FUSION_SPREAD / 2) < 0.01) {
				half++;
			} else if (Math.abs(deg - DvaStats.FUSION_SPREAD) < 0.01) {
				edge++;
			}
		}
		assertEquals(1, middle);
		assertEquals(4, half);
		assertEquals(6, edge);
		// a volley every 0.15 s (3 ticks): 6.67 a second, 146.7 a second at 2 a pellet
		assertEquals(3, Ow.ticks(DvaStats.FUSION_INTERVAL));
		assertEquals(146.7, DvaStats.FUSION_PELLETS * DvaStats.FUSION_DAMAGE / DvaStats.FUSION_INTERVAL, 0.1);
		// falloff 10 -> 20 m to x0.3 (W 0.6 a pellet)
		assertEquals(1, dev.overcraft.dva.FusionCannons.falloff(10), 1e-9);
		assertEquals(0.65, dev.overcraft.dva.FusionCannons.falloff(15), 1e-9);
		assertEquals(0.3, dev.overcraft.dva.FusionCannons.falloff(20), 1e-9);
		assertEquals(0.3, dev.overcraft.dva.FusionCannons.falloff(40), 1e-9);
		// armor takes each pellet its 7, at most halved: a 2-damage pellet does 1 (W); 11 pellets 22 -> 11
		assertEquals(1, dev.overcraft.hero.Shields.armorReduce(2, 1), 1e-9);
		assertEquals(11, dev.overcraft.hero.Shields.armorReduce(22, 11), 1e-9);
		// one big hit is still one: 100 -> 93
		assertEquals(93, dev.overcraft.hero.Shields.armorReduce(100, 1), 1e-9);
	}

	@Test
	void defenseMatrixAndBoosters() {
		// 100 at 33 a second: 3.03 s; back at 16 a second after 0.75 s: 6.25 s from empty (D 02CC)
		assertEquals(3.03, DvaStats.MATRIX_ENERGY / DvaStats.MATRIX_DRAIN, 0.01);
		assertEquals(6.25, DvaStats.MATRIX_ENERGY / DvaStats.MATRIX_REGEN, 1e-9);
		assertEquals(15, Ow.ticks(DvaStats.MATRIX_REGEN_DELAY));
		assertEquals(10, Ow.ticks(DvaStats.MATRIX_MIN));
		// its box along the aim: 2.5 m wide, 2 m tall, 0.8 to 10.25 m ahead (D #205)
		var box = new dev.overcraft.hero.Barriers.Box(DvaStats.MATRIX_SIDE, DvaStats.MATRIX_HALF_HEIGHT, DvaStats.MATRIX_HALF_HEIGHT,
			DvaStats.MATRIX_NEAR, DvaStats.MATRIX_FAR);
		var f = new net.minecraft.world.phys.Vec3(0, 0, 1);
		var r = new net.minecraft.world.phys.Vec3(-1, 0, 0);
		var u = new net.minecraft.world.phys.Vec3(0, 1, 0);
		assertTrue(box.contains(new net.minecraft.world.phys.Vec3(1.2, 0.9, 10), f, r, u));
		assertFalse(box.contains(new net.minecraft.world.phys.Vec3(0, 0, 0.5), f, r, u));
		assertFalse(box.contains(new net.minecraft.world.phys.Vec3(0, 0, 10.5), f, r, u));
		assertFalse(box.contains(new net.minecraft.world.phys.Vec3(1.3, 0, 5), f, r, u));
		// Boosters: 12 m/s (5.5 x 2.18), 0.4 to 2 s: 4.8 to 24 m; 25 + 17.5 m/s push (D 047E)
		assertEquals(12.0, DvaStats.BOOSTERS_SPEED, 0.02);
		assertEquals(23.98, DvaStats.BOOSTERS_SPEED * DvaStats.BOOSTERS_TIME, 0.01);
		assertEquals(17.46, Math.hypot(DvaStats.BOOSTERS_KNOCKBACK, DvaStats.BOOSTERS_KNOCKBACK_UP), 0.01);
		assertEquals(0.1, DvaStats.BOOSTERS_KNOCKBACK_TAKEN, 1e-9);
	}

	@Test
	void missilesSelfDestructAndLightGun() {
		// 18 at 14 a second (0.7 a tick) after 0.25 s: 135 if all land (2 + 5.5 each) (D 1128)
		assertEquals(5, Ow.ticks(DvaStats.MISSILES_CAST));
		assertEquals(0.7, DvaStats.MISSILES_RATE / 20, 1e-9);
		assertEquals(135, DvaStats.MISSILES_COUNT * (DvaStats.MISSILES_IMPACT + DvaStats.MISSILES_SPLASH), 1e-9);
		assertEquals(40, Ow.ticks(DvaStats.MISSILES_RANGE / DvaStats.MISSILES_SPEED));
		// the burst: full to 0.1 m, x0.25 at 1.5 m (1.375: W), none past
		assertEquals(1, dev.overcraft.dva.MicroMissiles.splashFactor(0.05), 1e-9);
		assertEquals(1.375, DvaStats.MISSILES_SPLASH * dev.overcraft.dva.MicroMissiles.splashFactor(1.5), 1e-9);
		assertEquals(0, dev.overcraft.dva.MicroMissiles.splashFactor(1.6), 1e-9);
		// Self-Destruct: 1000 to 6 m, then 50 + 950 x (21 - d) / 15 (D #280)
		assertEquals(1000, dev.overcraft.dva.SelfDestruct.damage(3), 1e-9);
		assertEquals(1000, dev.overcraft.dva.SelfDestruct.damage(6), 1e-9);
		assertEquals(640, dev.overcraft.dva.SelfDestruct.damage(12), 1e-9);
		assertEquals(280, dev.overcraft.dva.SelfDestruct.damage(18), 1e-9);
		assertEquals(100, dev.overcraft.dva.SelfDestruct.damage(21), 1e-9);
		assertEquals(0, dev.overcraft.dva.SelfDestruct.damage(21.5), 1e-9);
		// its knockback: 22.4 m/s at the centre, 7.8 at 21 m (ours, linear)
		double full = Math.hypot(DvaStats.SELF_DESTRUCT_KNOCKBACK, DvaStats.SELF_DESTRUCT_KNOCKBACK_UP);
		assertEquals(22.36, full, 0.01);
		assertEquals(7.8, full * dev.overcraft.dva.SelfDestruct.knockbackFactor(21), 1e-9);
		assertEquals(60, Ow.ticks(DvaStats.SELF_DESTRUCT_FUSE));
		// the Light Gun: 7 a second, 98 a second, 20 rounds in 2.86 s; the rounds back at 0.91 s of the 1.4 (D 02B8)
		assertEquals(98, DvaStats.LIGHT_GUN_DAMAGE * DvaStats.LIGHT_GUN_RATE, 1e-9);
		assertEquals(18, Ow.ticks(DvaStats.LIGHT_GUN_RELOAD * DvaStats.LIGHT_GUN_RELOAD_AMMO));
		assertEquals(28, Ow.ticks(DvaStats.LIGHT_GUN_RELOAD));
	}

	@Test
	void perks() {
		// Bunny Power's landing: 250 to 3 m, then down to 0 at 4.5 (026A #908); without it nothing past 3
		assertEquals(250, dev.overcraft.dva.DvaPerks.callDamage(3, true), 1e-9);
		assertEquals(125, dev.overcraft.dva.DvaPerks.callDamage(3.75, true), 1e-9);
		assertEquals(0, dev.overcraft.dva.DvaPerks.callDamage(4.5, true), 1e-9);
		assertEquals(0, dev.overcraft.dva.DvaPerks.callDamage(3.5, false), 1e-9);
		// Precision Fusion: 13.33 volleys a second, 1 a pellet, falloff 15-30 m
		assertEquals(13.33, 2 / DvaStats.FUSION_INTERVAL, 0.01);
		assertEquals(1, dev.overcraft.dva.FusionCannons.falloff(15, DvaStats.PRECISION_FALLOFF), 1e-9);
		assertEquals(0.3, dev.overcraft.dva.FusionCannons.falloff(30, DvaStats.PRECISION_FALLOFF), 1e-9);
		assertEquals(62, Ow.ticks(DvaStats.PRECISION_TIME));
		// Extended Boosters: 25 x 1.4
		assertEquals(35, DvaStats.BOOSTERS_DAMAGE * 1.4, 1e-9);
	}
}
