package dev.overcraft.hero;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The kill feed's rules that need no game: the blow being dealt, the credit window, the world deaths, the icons. */
class KillFeedLogicTest {
	@Test
	void theBlowBeingDealtIsRestoredAfterANestedHurt() {
		Object hero = new Object(), first = new Object(), second = new Object();
		Blow outer = Blow.ability("soundwave"), inner = Blow.perk("superbloom");
		Combat.dealingWhile(hero, first, outer, () -> {
			assertSame(outer, Combat.dealtTo(first, hero));
			Combat.dealingWhile(hero, second, inner, () -> {
				assertSame(inner, Combat.dealtTo(second, hero));
				// a death of the first target inside the second's hurt isn't told with the second's blow
				assertNull(Combat.dealtTo(first, hero));
			});
			assertSame(outer, Combat.dealtTo(first, hero));
			// another attacker's damage to the same target isn't this blow
			assertNull(Combat.dealtTo(first, new Object()));
		});
		assertNull(Combat.dealtTo(first, hero));
	}

	@Test
	void theBlowIsRestoredEvenWhenTheHurtThrows() {
		Object hero = new Object(), target = new Object();
		try {
			Combat.dealingWhile(hero, target, Blow.WEAPON, () -> {
				throw new IllegalStateException();
			});
		} catch (IllegalStateException expected) {
			// the hurt failed
		}
		assertNull(Combat.dealtTo(target, hero));
	}

	@Test
	void aPushCountsFor100Ticks() {
		assertTrue(Combat.inCreditWindow(1100, 1000));
		assertFalse(Combat.inCreditWindow(1101, 1000));
	}

	@Test
	void onlyTheWorldsPushDeathsAreCredited() {
		for (String id : List.of("minecraft:fall", "minecraft:out_of_world", "minecraft:lava", "minecraft:in_wall",
			"minecraft:fly_into_wall")) {
			assertTrue(Combat.pushDeathType(id), id);
		}
		// /kill, sunburn, drowning, starving, cramming, a generic death
		for (String id : List.of("minecraft:generic_kill", "minecraft:on_fire", "minecraft:in_fire", "minecraft:drown",
			"minecraft:starve", "minecraft:cramming", "minecraft:generic", "minecraft:magic")) {
			assertFalse(Combat.pushDeathType(id), id);
		}
	}

	@Test
	void weaponNamesTheBlowElseTheHand() {
		assertEquals("ability:purr", KillFeed.weapon(Blow.ability("purr"), true));
		assertEquals("melee", KillFeed.weapon(null, true));
		assertEquals("weapon", KillFeed.weapon(null, false));
	}

	@Test
	void theIconsFollowTheRule() {
		// weapon fire, any mode without its own icon, and a mob's ranged kill: only the arrow
		assertEquals(List.of(), KillFeed.iconTokens("weapon", false, false));
		assertEquals(List.of("headshot"), KillFeed.iconTokens("weapon", false, true));
		// Quick Melee (and a mob's bite): the fist
		assertEquals(List.of("fist"), KillFeed.iconTokens("melee", false, false));
		// a melee or weapon mode with an icon of its own, abilities, perks, the ult: the icon, the crit after it
		assertEquals(List.of("icon:melee:claws_out"), KillFeed.iconTokens("melee:claws_out", false, false));
		assertEquals(List.of("icon:weapon:charged_shot", "headshot"), KillFeed.iconTokens("weapon:charged_shot", false, true));
		assertEquals(List.of("icon:perk:superbloom"), KillFeed.iconTokens("perk:superbloom", false, false));
		assertEquals(List.of("icon:ult:catnapper"), KillFeed.iconTokens("ult:catnapper", false, false));
		// no killer: the skull (#33)
		assertEquals(List.of("skull"), KillFeed.iconTokens("weapon", true, false));
	}

	@Test
	void blowsCarryTheirKindIconCritAndHero() {
		Blow charged = Blow.weapon("charged_shot", true).by(Hero.SOJOURN);
		assertEquals(Blow.Kind.WEAPON, charged.kind());
		assertEquals("weapon:charged_shot", charged.feed());
		assertTrue(charged.crit());
		assertSame(Hero.SOJOURN, charged.hero());
		assertEquals("melee:claws_out", Blow.melee("claws_out").feed());
		assertEquals(Blow.Kind.MELEE, Blow.melee("claws_out").kind());
		assertFalse(charged.crit(false).crit());
		assertSame(Hero.SOJOURN, charged.crit(false).hero());
	}

	/**
	 * The call sites deal the kind the rule gives (kill_feed.md): read from the source, since they need the game to run.
	 * Brigitte's Quick Melee was tagged WEAPON (the critique's finding 2).
	 */
	@Test
	void callSitesDealTheRulesKinds() throws IOException {
		String brigitte = Files.readString(Path.of("src/main/java/dev/overcraft/brigitte/Brigitte.java"));
		assertTrue(brigitte.contains("s.flailMelee ? dev.overcraft.hero.Blow.MELEE : dev.overcraft.hero.Blow.WEAPON"));
		assertTrue(brigitte.contains("s.flailMelee = true;"));
		assertTrue(brigitte.contains("Blow.ult(\"rally\")"), "Rally's shockwave is the ult's");
		String melee = Files.readString(Path.of("src/main/java/dev/overcraft/hero/QuickMelee.java"));
		assertTrue(melee.contains("claws ? Blow.melee(\"claws_out\") : Blow.MELEE"));
		String railgun = Files.readString(Path.of("src/main/java/dev/overcraft/sojourn/Railgun.java"));
		assertTrue(railgun.contains("Blow.ult(\"overclock\")") && railgun.contains("Blow.weapon(\"charged_shot\", crit)"));
		String bounce = Files.readString(Path.of("src/main/java/dev/overcraft/entity/BioticGrenadeEntity.java"));
		assertTrue(bounce.contains("Blow.perk(\"biotic_bounce\")"));
	}
}
