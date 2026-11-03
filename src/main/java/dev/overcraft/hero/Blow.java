package dev.overcraft.hero;

/**
 * What dealt a hero's damage, as Overwatch's elimination feed tells it (the wiki's "Elimination Feed", 2026-06-19): the
 * hero's weapon (any fire mode: no icon, only the arrow), Quick Melee (the fist), an ability, a perk that deals damage of
 * its own (Superbloom, Whiplash: its icon), or the ultimate (its icon); and whether the hit was critical (the feed's red
 * target). Every damage a hero deals names one (Combat.damage), so a kill can always be told: no hero needs a rule of its
 * own in the feed.
 *
 * @param icon the ability's, perk's or ult's icon slug as the HUD names it ("soundwave", "claws_out"); "" for the weapon
 *             and melee
 */
public record Blow(Kind kind, String icon, boolean crit) {
	public enum Kind {
		WEAPON, MELEE, ABILITY, PERK, ULT
	}

	public static final Blow WEAPON = new Blow(Kind.WEAPON, "", false);
	public static final Blow MELEE = new Blow(Kind.MELEE, "", false);

	/** The weapon's fire (primary or secondary: the feed doesn't tell them apart), a critical hit or not. */
	public static Blow weapon(boolean crit) {
		return crit ? new Blow(Kind.WEAPON, "", true) : WEAPON;
	}

	public static Blow ability(String icon) {
		return new Blow(Kind.ABILITY, icon, false);
	}

	public static Blow ability(String icon, boolean crit) {
		return new Blow(Kind.ABILITY, icon, crit);
	}

	public static Blow perk(String icon) {
		return new Blow(Kind.PERK, icon, false);
	}

	public static Blow ult(String icon) {
		return new Blow(Kind.ULT, icon, false);
	}

	/** The same blow, critical or not. */
	public Blow crit(boolean crit) {
		return crit == this.crit ? this : new Blow(kind, icon, crit);
	}

	/**
	 * As the kill feed's payload carries it: "weapon", "melee", "ability:soundwave", "perk:superbloom", "ult:catnapper".
	 */
	public String feed() {
		return switch (kind) {
			case WEAPON -> "weapon";
			case MELEE -> "melee";
			case ABILITY -> "ability:" + icon;
			case PERK -> "perk:" + icon;
			case ULT -> "ult:" + icon;
		};
	}
}
