package dev.overcraft.hero;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Overwatch's perks (#36): each hero has two minor perks, offered at perk level 2, and two major perks, offered at
 * level 3; the player picks one of each pair for the rest of that hero's life. Names and effects from the game data's
 * hero text (DataTool's heroes.txt); the numbers live with the hero's stats.
 */
public enum Perk {
	KIRIKO_URGENT_CARE(Hero.KIRIKO, Tier.MINOR, "urgent_care"),
	KIRIKO_FORTUNE_TELLER(Hero.KIRIKO, Tier.MINOR, "fortune_teller"),
	KIRIKO_READY_STEP(Hero.KIRIKO, Tier.MAJOR, "ready_step"),
	KIRIKO_FOXTROT(Hero.KIRIKO, Tier.MAJOR, "foxtrot");

	public enum Tier { MINOR, MAJOR }

	public final Hero hero;
	public final Tier tier;
	/** The icon's and the translation's name: textures/gui/heroes/<hero>/perks/<id>.png, overcraft.perk.<hero>.<id>. */
	public final String id;

	Perk(Hero hero, Tier tier, String id) {
		this.hero = hero;
		this.tier = tier;
		this.id = id;
	}

	/** The hero's two perks of a tier, in the game's order (the left choice first). */
	public static List<Perk> of(Hero hero, Tier tier) {
		List<Perk> perks = new ArrayList<>(2);
		for (Perk perk : values()) {
			if (perk.hero == hero && perk.tier == tier) {
				perks.add(perk);
			}
		}
		return perks;
	}

	/** By ordinal, for the network: -1 or out of range is none. */
	public static @Nullable Perk byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}

	public static int index(@Nullable Perk perk) {
		return perk == null ? -1 : perk.ordinal();
	}
}
