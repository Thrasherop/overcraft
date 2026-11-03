package dev.overcraft.hero;

import org.jspecify.annotations.Nullable;

/**
 * What dealt a hero's damage, as Overwatch's elimination feed tells it (the wiki's "Elimination Feed", 2026-06-19), and
 * as the rest of the mod reads it (Voice's melee line). Every damage a hero deals names one (Combat.damage), so a kill
 * can always be told; the rule that picks it is in techniques/hud/kill_feed.md:
 * <ul>
 *   <li>{@code kind} is what the hit is in the game: the weapon's fire (any mode), Quick Melee, an ability, a perk's own
 *   damage, the ultimate. Damage that only exists while the ult runs is the ult's (Overclock's charged shots, Rally's
 *   bash shockwave); damage that only exists because of a perk is the perk's (Superbloom, Whiplash, Biotic Bounce's
 *   second burst, Local Anesthetic, Claws Out's wound).</li>
 *   <li>{@code icon} is the art slug the feed shows ("" for none). Weapon fire and Quick Melee show none, except a mode
 *   the wiki gives an icon of its own (Sojourn's Charged Shot) and a melee an ability changes (Claws Out).</li>
 *   <li>{@code hero} is the hero who dealt it (the feed's portrait), fixed when it was dealt or, for damage that lands
 *   later (Ana's doses, torpedoes, a field), when it was made: a hero switch in between doesn't change it.</li>
 * </ul>
 */
public record Blow(Kind kind, String icon, boolean crit, @Nullable Hero hero) {
	public enum Kind {
		WEAPON, MELEE, ABILITY, PERK, ULT
	}

	public static final Blow WEAPON = new Blow(Kind.WEAPON, "", false, null);
	public static final Blow MELEE = new Blow(Kind.MELEE, "", false, null);

	/** The weapon's fire (primary or secondary: the feed doesn't tell them apart), a critical hit or not. */
	public static Blow weapon(boolean crit) {
		return crit ? new Blow(Kind.WEAPON, "", true, null) : WEAPON;
	}

	/** A weapon mode the feed shows an icon for (the wiki lists Sojourn's Charged Shot among her elimination icons). */
	public static Blow weapon(String icon, boolean crit) {
		return new Blow(Kind.WEAPON, icon, crit, null);
	}

	/** Quick Melee changed by an ability or perk, which the feed shows instead of the fist (Claws Out). */
	public static Blow melee(String icon) {
		return new Blow(Kind.MELEE, icon, false, null);
	}

	public static Blow ability(String icon) {
		return new Blow(Kind.ABILITY, icon, false, null);
	}

	public static Blow perk(String icon) {
		return new Blow(Kind.PERK, icon, false, null);
	}

	public static Blow ult(String icon) {
		return new Blow(Kind.ULT, icon, false, null);
	}

	/** The same blow, critical or not. */
	public Blow crit(boolean crit) {
		return crit == this.crit ? this : new Blow(kind, icon, crit, hero);
	}

	/** The same blow, dealt by {@code hero} (for damage that lands after it was made). */
	public Blow by(@Nullable Hero hero) {
		return hero == this.hero ? this : new Blow(kind, icon, crit, hero);
	}

	/**
	 * As the kill feed's payload carries it: the kind, and the icon after a colon when there is one ("weapon", "melee",
	 * "weapon:charged_shot", "melee:claws_out", "ability:soundwave", "perk:superbloom", "ult:catnapper").
	 */
	public String feed() {
		String name = kind.name().toLowerCase(java.util.Locale.ROOT);
		return icon.isEmpty() ? name : name + ":" + icon;
	}
}
