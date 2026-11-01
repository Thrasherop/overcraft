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
	KIRIKO_FOXTROT(Hero.KIRIKO, Tier.MAJOR, "foxtrot"),
	LUCIO_SOUNDWAVE_RIDER(Hero.LUCIO, Tier.MINOR, "soundwave_rider"),
	LUCIO_BEAT_DROP(Hero.LUCIO, Tier.MINOR, "beat_drop"),
	LUCIO_NOISE_VIOLATION(Hero.LUCIO, Tier.MAJOR, "noise_violation"),
	LUCIO_ACCELERANDO(Hero.LUCIO, Tier.MAJOR, "accelerando"),
	JETPACK_CAT_ULTERIOR_MOTIVE(Hero.JETPACK_CAT, Tier.MINOR, "ulterior_motive"),
	JETPACK_CAT_TRANSPORT_SHIELDING(Hero.JETPACK_CAT, Tier.MINOR, "transport_shielding"),
	JETPACK_CAT_PURRFECT_FORM(Hero.JETPACK_CAT, Tier.MAJOR, "purrfect_form"),
	JETPACK_CAT_CLAWS_OUT(Hero.JETPACK_CAT, Tier.MAJOR, "claws_out"),
	MERCY_ANGELIC_RESURRECTION(Hero.MERCY, Tier.MINOR, "angelic_resurrection"),
	MERCY_WINGED_REACH(Hero.MERCY, Tier.MINOR, "winged_reach"),
	MERCY_CHAIN_BOOST(Hero.MERCY, Tier.MAJOR, "chain_boost"),
	MERCY_DOUBLE_DOSE(Hero.MERCY, Tier.MAJOR, "double_dose"),
	LIFEWEAVER_PETAL_PROTECTION(Hero.LIFEWEAVER, Tier.MINOR, "petal_protection"),
	LIFEWEAVER_DASHING_ESCAPE(Hero.LIFEWEAVER, Tier.MINOR, "dashing_escape"),
	LIFEWEAVER_SUPERBLOOM(Hero.LIFEWEAVER, Tier.MAJOR, "superbloom"),
	LIFEWEAVER_SOW_THE_SEED(Hero.LIFEWEAVER, Tier.MAJOR, "sow_the_seed"),
	ANA_LOCAL_ANESTHETIC(Hero.ANA, Tier.MINOR, "local_anesthetic"),
	ANA_SPEED_SERUM(Hero.ANA, Tier.MINOR, "speed_serum"),
	ANA_BIOTIC_BOUNCE(Hero.ANA, Tier.MAJOR, "biotic_bounce"),
	ANA_HEADHUNTER(Hero.ANA, Tier.MAJOR, "headhunter"),
	// Sombra's, the live list's order (D 005B.177)
	SOMBRA_LIFE_HACK(Hero.SOMBRA, Tier.MINOR, "life_hack"),
	SOMBRA_CTRL_ALT_ESC(Hero.SOMBRA, Tier.MINOR, "ctrl_alt_esc"),
	SOMBRA_DATA_PACKET(Hero.SOMBRA, Tier.MAJOR, "data_packet"),
	SOMBRA_CYBERSECURITY(Hero.SOMBRA, Tier.MAJOR, "cybersecurity"),
	// Brigitte's, the live list's order (D 0063.177)
	BRIGITTE_COMBAT_MEDIC(Hero.BRIGITTE, Tier.MINOR, "combat_medic"),
	BRIGITTE_MORALE_BOOST(Hero.BRIGITTE, Tier.MINOR, "morale_boost"),
	BRIGITTE_INSPIRING_STRIKE(Hero.BRIGITTE, Tier.MAJOR, "inspiring_strike"),
	BRIGITTE_WHIPLASH(Hero.BRIGITTE, Tier.MAJOR, "whiplash");

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
