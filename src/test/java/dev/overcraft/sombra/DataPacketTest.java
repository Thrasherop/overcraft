package dev.overcraft.sombra;

import dev.overcraft.Ow;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Data Packet (FBC): a hack takes 4 s off Hotfix's recharge, the rest carried into the next charge. */
class DataPacketTest {
	@Test
	void cutsTheRunningRecharge() {
		HeroState s = new HeroState();
		s.reset(Hero.SOMBRA);
		s.hotfixCharges = 1;
		s.hotfixRecharge = Ow.ticks(6.5);
		Sombra.dataPacket(s);
		assertEquals(1, s.hotfixCharges);
		assertEquals(Ow.ticks(2.5), s.hotfixRecharge, 1e-9);
	}

	@Test
	void carriesTheRestIntoTheNextCharge() {
		HeroState s = new HeroState();
		s.reset(Hero.SOMBRA);
		s.hotfixCharges = 0;
		s.hotfixRecharge = Ow.ticks(1);
		Sombra.dataPacket(s);
		assertEquals(1, s.hotfixCharges);
		assertEquals(Ow.ticks(8 - 3), s.hotfixRecharge, 1e-9);
	}

	@Test
	void nothingToCutWhenFull() {
		HeroState s = new HeroState();
		s.reset(Hero.SOMBRA);
		s.hotfixCharges = 2;
		s.hotfixRecharge = -1;
		Sombra.dataPacket(s);
		assertEquals(2, s.hotfixCharges);
		assertEquals(-1, s.hotfixRecharge, 1e-9);
	}
}
