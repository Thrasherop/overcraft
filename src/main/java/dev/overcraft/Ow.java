package dev.overcraft;

/**
 * Overwatch units to Minecraft units. 1 m = 1 block, 1 s = 20 ticks, and 250 Overwatch health = 20 Minecraft health
 * (10 hearts), so every hero's health, damage and healing scale by the same factor.
 */
public final class Ow {
	public static final float HP = 20f / 250f;

	private Ow() {
	}

	/** Overwatch health/damage/healing to Minecraft health points. */
	public static float hp(double owHealth) {
		return (float) (owHealth * HP);
	}

	/** Minecraft health points back to Overwatch health. */
	public static float toOw(double mcHealth) {
		return (float) (mcHealth / HP);
	}

	/** Seconds to ticks. */
	public static int ticks(double seconds) {
		return (int) Math.round(seconds * 20.0);
	}

	/** Metres per second to blocks per tick. */
	public static double speed(double metresPerSecond) {
		return metresPerSecond / 20.0;
	}

	/** Metres per second squared to blocks per tick squared. */
	public static double accel(double metresPerSecondSquared) {
		return metresPerSecondSquared / 400.0;
	}
}
