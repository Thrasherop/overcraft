package dev.overcraft.hero;

import org.jspecify.annotations.Nullable;

/** The heroes on the select screen. Health is Overwatch health (scaled by {@link dev.overcraft.Ow#HP}). */
public enum Hero {
	KIRIKO("kiriko", "Kiriko", Role.SUPPORT, 250, true, 0xFFE9A23B),
	LUCIO("lucio", "Lúcio", Role.SUPPORT, 225, false, 0xFF7CC242),
	SOMBRA("sombra", "Sombra", Role.SUPPORT, 225, false, 0xFF8A4FD8),
	ILLARI("illari", "Illari", Role.SUPPORT, 250, false, 0xFFF2C14E);

	public enum Role { TANK, DAMAGE, SUPPORT }

	public final String id;
	public final String displayName;
	public final Role role;
	public final int owHealth;
	/** False: on the select screen but not playable yet. */
	public final boolean playable;
	public final int color;

	Hero(String id, String displayName, Role role, int owHealth, boolean playable, int color) {
		this.id = id;
		this.displayName = displayName;
		this.role = role;
		this.owHealth = owHealth;
		this.playable = playable;
		this.color = color;
	}

	public static @Nullable Hero byId(String id) {
		for (Hero hero : values()) {
			if (hero.id.equals(id)) {
				return hero;
			}
		}
		return null;
	}
}
