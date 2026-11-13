package dev.overcraft;

import dev.overcraft.hero.Hero;
import dev.overcraft.hero.Tank;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Tank role's and Stalwart's math (dev.overcraft.hero.Tank; tools/reference/sigma.md "The Tank role"). */
class TankMathTest {
	private static final Identifier SLOWNESS = Identifier.withDefaultNamespace("effect.slowness");
	private static final Identifier ANESTHETIC = Identifier.fromNamespaceAndPath("overcraft", "ana_local_anesthetic");
	private static final Identifier OWN = Identifier.fromNamespaceAndPath("overcraft", "sigma_accretion_cast");
	private static final Identifier RUSH = Identifier.fromNamespaceAndPath("overcraft", "kitsune_rush_speed");

	@Test
	void sigmasHealth() {
		// 200 + the role's 150 + 250 shields = 600 Overwatch = 48 Minecraft health (24 hearts)
		assertEquals(Hero.Role.TANK, Hero.SIGMA.role);
		assertEquals(600, Hero.SIGMA.owHealth);
		assertEquals(250, Hero.SIGMA.shields());
		assertEquals(48f, Ow.hp(Hero.SIGMA.owHealth), 1e-5);
		assertTrue(Tank.tank(Hero.SIGMA) && Tank.stalwart(Hero.SIGMA));
		// no one else is a Stalwart; D.Va is the only other tank (an Initiator)
		for (Hero hero : Hero.values()) {
			if (hero != Hero.SIGMA) {
				assertTrue(!Tank.stalwart(hero), hero.id);
				assertEquals(hero == Hero.DVA, Tank.tank(hero), hero.id);
			}
		}
		assertEquals(250, Hero.KIRIKO.owHealth);
	}

	@Test
	void sigmasBody() {
		// his mover (D 2AB1): 1.4 x 2.5 m, crouched 2.05 m; the eye at his stand's head hardpoint (2.06 m)
		var standing = dev.overcraft.hero.HeroBody.of(Hero.SIGMA, net.minecraft.world.entity.Pose.STANDING);
		var crouching = dev.overcraft.hero.HeroBody.of(Hero.SIGMA, net.minecraft.world.entity.Pose.CROUCHING);
		assertEquals(1.4f, standing.width(), 1e-6);
		assertEquals(2.5f, standing.height(), 1e-6);
		assertEquals(2.06f, standing.eyeHeight(), 1e-6);
		assertEquals(2.05f, crouching.height(), 1e-6);
		// the eye as far under the top crouched as standing
		assertEquals(standing.height() - standing.eyeHeight(), crouching.height() - crouching.eyeHeight(), 1e-6);
		// a 1-wide door and a 2-high tunnel don't take him; anyone else keeps Minecraft's box
		assertTrue(standing.width() > 1 && crouching.height() > 2);
		assertEquals(null, dev.overcraft.hero.HeroBody.of(Hero.KIRIKO, net.minecraft.world.entity.Pose.STANDING));
		assertEquals(null, dev.overcraft.hero.HeroBody.of(Hero.SIGMA, net.minecraft.world.entity.Pose.SWIMMING));
	}

	@Test
	void enemiesChargeLessOnATank() {
		assertEquals(0.6, Tank.ultFactor(true, false), 1e-12);
		// healing a tank ally, or damage to anyone else: in full
		assertEquals(1, Tank.ultFactor(true, true), 1e-12);
		assertEquals(1, Tank.ultFactor(false, false), 1e-12);
		// 100 damage on him: 60 points of a 2300 ult
		assertEquals(60, 100 * Tank.ultFactor(true, false), 1e-9);
	}

	@Test
	void stalwartKnockback() {
		assertEquals(0.6, Tank.knockbackFactor(true), 1e-12);
		assertEquals(1, Tank.knockbackFactor(false), 1e-12);
		// a zombie's 0.4 knockback: 0.24
		assertEquals(0.24, 0.4 * Tank.knockbackFactor(true), 1e-12);
	}

	@Test
	void stalwartSlows() {
		// a 50% slow is 30%; Local Anesthetic's 40% is 24%
		assertEquals(0.3, Tank.resistedSlow(0.5), 1e-12);
		assertEquals(0.24, Tank.resistedSlow(0.4), 1e-12);
		double base = 0.1;
		// Slowness II: -0.3 of the total, resisted -0.18
		List<AttributeModifier> slowness = List.of(new AttributeModifier(SLOWNESS, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		assertEquals(base * 0.82, Tank.resistedValue(base, slowness, Set.of()), 1e-12);
		// Local Anesthetic's -0.4 of the base, resisted -0.24
		List<AttributeModifier> dart = List.of(new AttributeModifier(ANESTHETIC, -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		assertEquals(base * 0.76, Tank.resistedValue(base, dart, Set.of()), 1e-12);
		// his own slow (Accretion's cast, -75%) is not resisted; a buff is untouched
		List<AttributeModifier> own = List.of(new AttributeModifier(OWN, -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
			new AttributeModifier(RUSH, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		assertEquals(base * 1.3 * 0.25, Tank.resistedValue(base, own, Set.of(OWN)), 1e-12);
		// nothing on him: the base
		assertEquals(base, Tank.resistedValue(base, List.of(), Set.of()), 1e-12);
	}
}
