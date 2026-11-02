package dev.overcraft.hero;

import org.jspecify.annotations.Nullable;

/** The heroes on the select screen. Health is Overwatch health (scaled by {@link dev.overcraft.Ow#HP}). */
public enum Hero {
	// roles and sub-roles: the wiki's infoboxes, 2026-10-26 (Sombra is a support since 2026)
	// his kit is being built (#68), step by step on his branch: playable there, not merged until it's whole. The first Tank:
	// 200 health + 250 shields, + the role's 150 (Tank.BONUS_HEALTH, added below) = 600; colour his data's (0.486, 0.545, 0.549)
	SIGMA("sigma", "Sigma", Role.TANK, "stalwart", dev.overcraft.sigma.SigmaStats.HEALTH + dev.overcraft.sigma.SigmaStats.SHIELDS, true,
		0xFF7C8B8C),
	KIRIKO("kiriko", "Kiriko", Role.SUPPORT, "medic", 250, true, 0xFFE9A23B),
	LUCIO("lucio", "Lúcio", Role.SUPPORT, "tactician", dev.overcraft.lucio.LucioStats.HEALTH, true, 0xFF7CC242),
	JETPACK_CAT("jetpack_cat", "Jetpack Cat", Role.SUPPORT, "tactician", dev.overcraft.jetpackcat.JetpackCatStats.HEALTH, true,
		0xFFF08A3C),
	MERCY("mercy", "Mercy", Role.SUPPORT, "medic", dev.overcraft.mercy.MercyStats.HEALTH, true, 0xFFFDF9D7),
	LIFEWEAVER("lifeweaver", "Lifeweaver", Role.SUPPORT, "medic",
		dev.overcraft.lifeweaver.LifeweaverStats.HEALTH + dev.overcraft.lifeweaver.LifeweaverStats.SHIELDS, true, 0xFFF3A7C8),
	// her kit is being built (#63), step by step on her branch: playable there, not merged until it's whole
	ANA("ana", "Ana", Role.SUPPORT, "tactician", dev.overcraft.ana.AnaStats.HEALTH, true, 0xFF7E9CC2),
	SOMBRA("sombra", "Sombra", Role.SUPPORT, "tactician", dev.overcraft.sombra.SombraStats.HEALTH, true, 0xFF8A4FD8),
	// her kit is being built (#65), step by step on her branch: playable there, not merged until it's whole
	BRIGITTE("brigitte", "Brigitte", Role.SUPPORT, "survivor",
		dev.overcraft.brigitte.BrigitteStats.HEALTH + dev.overcraft.brigitte.BrigitteStats.ARMOR, true, 0xFFC7432E),
	ILLARI("illari", "Illari", Role.SUPPORT, "survivor", 250, false, 0xFFF2C14E),
	// her kit is being built (#1), step by step on her branch: playable there, not merged until it's whole
	JUNO("juno", "Juno", Role.SUPPORT, "survivor", dev.overcraft.juno.JunoStats.HEALTH + dev.overcraft.juno.JunoStats.SHIELDS, true,
		0xFFE85A9B);

	public enum Role { TANK, DAMAGE, SUPPORT }

	public final String id;
	public final String displayName;
	public final Role role;
	/** Overwatch's sub-role ("medic"): a translation key's last part, overcraft.subrole.<id>. */
	public final String subRole;
	/** Health and shields together (Lifeweaver: 200 + 50), a tank's role bonus included (Sigma: 200 + 150 + 250). */
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

	/** Shields: the top of owHealth, regenerating by themselves, healed after the health under them (Hit points: Shields). */
	public int shields() {
		return this == LIFEWEAVER ? dev.overcraft.lifeweaver.LifeweaverStats.SHIELDS : this == JUNO ? dev.overcraft.juno.JunoStats.SHIELDS
			: this == SIGMA ? dev.overcraft.sigma.SigmaStats.SHIELDS : 0;
	}

	/**
	 * Armor: the top of owHealth like shields (no hero has both), healed after the health under it, but never back by itself,
	 * and each hit on it is 7 smaller, at most halved (W Hit points: Armor; BZ 2024-09-17). Brigitte's 75.
	 */
	public int armor() {
		return this == BRIGITTE ? dev.overcraft.brigitte.BrigitteStats.ARMOR : 0;
	}

	/** The top layer of owHealth: shields or armor. */
	public int layer() {
		return shields() + armor();
	}

	Hero(String id, String displayName, Role role, String subRole, int owHealth, boolean playable, int color) {
		this.id = id;
		this.displayName = displayName;
		this.role = role;
		this.subRole = subRole;
		// every tank's +150 (Overwatch's Role Queue bonus: Overcraft has no queue, so always: sigma-plan.md)
		this.owHealth = owHealth + (role == Role.TANK ? Tank.BONUS_HEALTH : 0);
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
