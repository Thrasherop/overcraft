package dev.overcraft.brigitte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import dev.overcraft.hero.HeroState;
import org.junit.jupiter.api.Test;

/** Rally's shockwave: the tracker's 10, then this Brigitte's bashed target out of the list (0EE3 #877 -> #890, #120), as Brigitte.shockwave calls it. */
class BrigitteRulesTest {
	private static List<UUID> ids(int n) {
		List<UUID> list = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			list.add(new UUID(0, i));
		}
		return list;
	}

	private static HeroState bashed(UUID id) {
		HeroState s = new HeroState();
		s.rallyBashed = id;
		return s;
	}

	@Test
	void theBashedOneIsPassedOver() {
		List<UUID> tracked = ids(3);
		List<UUID> hits = Brigitte.shockwaveHits(tracked, bashed(tracked.get(1)), Function.identity());
		assertEquals(2, hits.size());
		assertFalse(hits.contains(tracked.get(1)));
	}

	@Test
	void itStillFillsOneOfTheTen() {
		List<UUID> tracked = ids(12);
		assertEquals(9, Brigitte.shockwaveHits(tracked, bashed(tracked.get(0)), Function.identity()).size());
	}

	@Test
	void outsideTheTenItCostsNothing() {
		List<UUID> tracked = ids(12);
		assertEquals(10, Brigitte.shockwaveHits(tracked, bashed(tracked.get(11)), Function.identity()).size());
	}

	@Test
	void noBashNoExclusion() {
		assertEquals(3, Brigitte.shockwaveHits(ids(3), bashed(null), Function.identity()).size());
	}
}
