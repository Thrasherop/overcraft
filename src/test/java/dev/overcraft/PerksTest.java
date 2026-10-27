package dev.overcraft;

import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The perk progression's numbers (#36): the ramp, the levels, the progress shown and the tier waiting. */
class PerksTest {
	@Test
	void rateRampsFromThreeToSevenMinutes() {
		assertEquals(1, Perks.rate(0));
		assertEquals(1, Perks.rate(180));
		assertEquals(1.5, Perks.rate(300), 1e-9);
		assertEquals(2, Perks.rate(420));
		assertEquals(2, Perks.rate(3600));
	}

	@Test
	void levelsAndProgressForKiriko() {
		HeroState s = new HeroState();
		s.reset(Hero.KIRIKO);
		assertEquals(1, Perks.level(s));
		assertEquals(0, Perks.ready(s));
		s.perkXp = 950;
		assertEquals(0.5f, Perks.progress(s), 1e-6);
		s.perkXp = 1900;
		assertEquals(2, Perks.level(s));
		assertEquals(1, Perks.ready(s));
		assertEquals(0, Perks.progress(s), 1e-6);
		s.perkXp = 1900 + 1450;
		assertEquals(0.5f, Perks.progress(s), 1e-6);
		s.perkXp = 4800;
		assertEquals(3, Perks.level(s));
		// both waiting: the minor comes first
		assertEquals(1, Perks.ready(s));
		s.minorPerk = Perk.KIRIKO_URGENT_CARE;
		assertEquals(2, Perks.ready(s));
		s.majorPerk = Perk.KIRIKO_FOXTROT;
		assertEquals(0, Perks.ready(s));
		assertEquals(1, Perks.progress(s), 1e-6);
	}

	@Test
	void passiveGainIsAPointASecond() {
		HeroState s = new HeroState();
		s.reset(Hero.KIRIKO);
		for (int i = 0; i < 20 * 60; i++) {
			Perks.tick(s);
		}
		assertEquals(60, s.perkXp, 1e-6);
		// it stops at the major perk's level
		s.perkXp = 4799.99;
		Perks.tick(s);
		assertEquals(4800, s.perkXp, 1e-9);
	}

	@Test
	void savedProgressComesBack() {
		HeroState s = new HeroState();
		s.reset(Hero.KIRIKO);
		s.perkXp = 2500;
		s.perkTicks = 1234;
		s.minorPerk = Perk.KIRIKO_FORTUNE_TELLER;
		Perks.Progress saved = Perks.save(s);
		s.reset(Hero.KIRIKO);
		assertNull(s.minorPerk);
		Perks.load(s, saved);
		assertEquals(2500, s.perkXp);
		assertEquals(1234, s.perkTicks);
		assertEquals(Perk.KIRIKO_FORTUNE_TELLER, s.minorPerk);
		assertNull(s.majorPerk);
	}

	@Test
	void kirikoHasTwoPerksPerTier() {
		assertEquals(java.util.List.of(Perk.KIRIKO_URGENT_CARE, Perk.KIRIKO_FORTUNE_TELLER), Perk.of(Hero.KIRIKO, Perk.Tier.MINOR));
		assertEquals(java.util.List.of(Perk.KIRIKO_READY_STEP, Perk.KIRIKO_FOXTROT), Perk.of(Hero.KIRIKO, Perk.Tier.MAJOR));
	}
}
