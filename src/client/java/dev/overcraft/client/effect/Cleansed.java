package dev.overcraft.client.effect;

import java.util.HashMap;
import java.util.Map;

/**
 * Who Protection Suzu just touched: they glow white for a moment (the effects survey: material overlay 03C3 on
 * everyone it cleanses, Kiriko included, about 0.65 s). Drawn through Minecraft's white overlay (the creeper's
 * flash), so it works on any mob, and on the hero models and Kiriko's own arms.
 */
public final class Cleansed {
	/** How long the white lasts, fading out (the survey's overlay: about 0.65 s). */
	private static final float SECONDS = 0.65f;
	private static final Map<Integer, Float> SINCE = new HashMap<>();

	private Cleansed() {
	}

	public static void mark(int entityId) {
		SINCE.put(entityId, Fx.now());
	}

	/** How white the entity is now, 0 to 1. */
	public static float white(int entityId) {
		Float at = SINCE.get(entityId);
		if (at == null) {
			return 0;
		}
		float t = (Fx.now() - at) / SECONDS;
		if (t >= 1 || t < 0) {
			SINCE.remove(entityId);
			return 0;
		}
		return 1 - t * t;
	}

	public static void clear() {
		SINCE.clear();
	}
}
