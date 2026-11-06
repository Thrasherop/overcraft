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

	/** The pair, D 1259 #638 / W H:25, V:3: blocks a tick. */
	@org.junit.jupiter.api.Test
	void theWhipPushesAlongHerLevelAimAt25And3Up() {
		net.minecraft.world.phys.Vec3 v = Brigitte.whipPush(new net.minecraft.world.phys.Vec3(0, 0.6, 2));
		org.junit.jupiter.api.Assertions.assertEquals(0, v.x, 1.0E-9);
		org.junit.jupiter.api.Assertions.assertEquals(dev.overcraft.Ow.speed(25), v.z, 1.0E-9);
		org.junit.jupiter.api.Assertions.assertEquals(dev.overcraft.Ow.speed(3), v.y, 1.0E-9);
	}
}
