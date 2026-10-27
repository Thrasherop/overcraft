package dev.overcraft.hero;

import org.jspecify.annotations.Nullable;

/** The heroes on the select screen. Health is Overwatch health (scaled by {@link dev.overcraft.Ow#HP}). */
public enum Hero {
	// roles and sub-roles: the wiki's infoboxes, 2026-10-26 (Sombra is a support since 2026)
	KIRIKO("kiriko", "Kiriko", Role.SUPPORT, "medic", 250, true, 0xFFE9A23B),
	LUCIO("lucio", "Lúcio", Role.SUPPORT, "tactician", 225, false, 0xFF7CC242),
	SOMBRA("sombra", "Sombra", Role.SUPPORT, "tactician", 225, false, 0xFF8A4FD8),
	ILLARI("illari", "Illari", Role.SUPPORT, "survivor", 250, false, 0xFFF2C14E);

	public enum Role { TANK, DAMAGE, SUPPORT }

	public final String id;
	public final String displayName;
	public final Role role;
	/** Overwatch's sub-role ("medic"): a translation key's last part, overcraft.subrole.<id>. */
	public final String subRole;
	public final int owHealth;
	/** False: on the select screen but not playable yet. */
	public final boolean playable;
	public final int color;

	/**
	 * The fall this hero lands from unhurt in hero mode, in metres (#35): Overwatch has no fall damage, but Minecraft's
	 * cliffs are taller, so falls hurt from here, as Minecraft's from its 3 blocks. Kiriko: her climb's height.
	 */
	public double safeFall() {
		return this == KIRIKO ? dev.overcraft.kiriko.KirikoStats.SAFE_FALL : 3;
	}

	Hero(String id, String displayName, Role role, String subRole, int owHealth, boolean playable, int color) {
		this.id = id;
		this.displayName = displayName;
		this.role = role;
		this.subRole = subRole;
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
