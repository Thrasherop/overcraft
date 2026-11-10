package dev.overcraft.client;

import dev.overcraft.hero.HeroManager;
import dev.overcraft.sombra.SombraStats;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Sombra on the client: how far phased out her stealth has her (#87), for every client's look of her
 * (HeroModelRenderer.submitStealth) and her own view's arms. Her stealth fades in over 0.75 s from its start (graph 420C,
 * the synced HeroManager.SOMBRA_STEALTH), holds, and fades back over 0.3 s when it ends (1AD8); revealed by damage she
 * shows solid (Minecraft's invisibility is off then: Stealth.tick).
 */
public final class ClientSombra {
	/** Stealth's fade back when it ends (D 420C #260: 0.3 s). */
	private static final float FADE_OUT = 0.3f;
	/** Per entity: the game time (s) her stealth last ended, for its fade back. */
	private static final Map<Integer, Float> ENDED = new HashMap<>();
	/** Per entity: the last phase seen, to notice the end. */
	private static final Map<Integer, Float> LAST = new HashMap<>();

	private ClientSombra() {
	}

	/** Leaving a world: no phantom fade on a reused entity id. */
	public static void clear() {
		ENDED.clear();
		LAST.clear();
	}

	/**
	 * 0 (solid) to 1 (fully phased out) at this time ((game time + partial) / 20 s). Fading in from the start, fully out while
	 * Minecraft's invisibility hides her, solid while revealed, fading back after.
	 */
	public static float phase(Entity entity, float now) {
		Long from = entity.getAttached(HeroManager.SOMBRA_STEALTH);
		int id = entity.getId();
		float phase = 0;
		if (from != null && from >= 0) {
			float age = now - from / 20f;
			float fadeIn = (float) SombraStats.STEALTH_FADE_IN;
			if (age < fadeIn) {
				phase = Math.clamp(age / fadeIn, 0, 1);
			} else if (entity.isInvisible()) {
				phase = 1;
			}
		}
		// a reveal's end comes back with a short fade, not a snap (10 calls is a few frames: ours)
		Float seen = LAST.get(id);
		if (seen != null && phase >= 1 && seen < 1 && from != null && now - from / 20f > (float) SombraStats.STEALTH_FADE_IN) {
			phase = Math.min(1, seen + 0.02f);
		}
		Float last = LAST.put(id, phase);
		if (last != null && last > 0 && phase <= 0) {
			ENDED.put(id, now);
		}
		if (phase <= 0) {
			Float ended = ENDED.get(id);
			if (ended != null && now - ended < FADE_OUT) {
				return 1 - (now - ended) / FADE_OUT;
			}
		}
		return phase;
	}
}
