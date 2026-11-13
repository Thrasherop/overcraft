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
	BRIGITTE_WHIPLASH(Hero.BRIGITTE, Tier.MAJOR, "whiplash"),
	// Juno's, the live list's order (D 0065.177)
	JUNO_FAMILIAR_VITALS(Hero.JUNO, Tier.MINOR, "familiar_vitals"),
	JUNO_LOCKED_ON(Hero.JUNO, Tier.MINOR, "locked_on"),
	JUNO_FASTER_BLASTER(Hero.JUNO, Tier.MAJOR, "faster_blaster"),
	JUNO_LIFT_OFF(Hero.JUNO, Tier.MAJOR, "lift_off"),
	// Sigma's, the live list's order (D 004B.177 = [2578, 26B7, 25AF, 2577].134)
	SIGMA_KINETIC_CYCLE(Hero.SIGMA, Tier.MINOR, "kinetic_cycle"),
	SIGMA_HYPER_REGENERATION(Hero.SIGMA, Tier.MINOR, "hyper_regeneration"),
	SIGMA_HYPER_STRIKE(Hero.SIGMA, Tier.MAJOR, "hyper_strike"),
	SIGMA_LEVITATION(Hero.SIGMA, Tier.MAJOR, "levitation"),
	// D.Va's, the live list's order (D 0042.177 = [253B, 25E6, 253E, 273E].134): their effects come in step 10 (dva-plan.md)
	DVA_BUNNY_POWER(Hero.DVA, Tier.MINOR, "bunny_power"),
	DVA_EXTENDED_BOOSTERS(Hero.DVA, Tier.MINOR, "extended_boosters"),
	DVA_SHIELD_SYSTEM(Hero.DVA, Tier.MAJOR, "shield_system"),
	DVA_PRECISION_FUSION(Hero.DVA, Tier.MAJOR, "precision_fusion"),
	// Sojourn's, the live list's order (BZ 2025-02-18 / 2025-08-26; W): their effects come in step 7 (sojourn-plan.md)
	SOJOURN_OVERCHARGED(Hero.SOJOURN, Tier.MINOR, "overcharged"),
	SOJOURN_DECELERATION_FIELD(Hero.SOJOURN, Tier.MINOR, "deceleration_field"),
	SOJOURN_DUAL_THRUSTERS(Hero.SOJOURN, Tier.MAJOR, "dual_thrusters"),
	SOJOURN_FRICTION_GENERATORS(Hero.SOJOURN, Tier.MAJOR, "friction_generators"),
	// Pharah's, the live list's order (D 0057.177: 28E7, 2507; 2509, 28C6; pharah-map.md)
	PHARAH_CONCUSSIVE_FORCE(Hero.PHARAH, Tier.MINOR, "concussive_force"),
	PHARAH_HELIX_SHIELDS(Hero.PHARAH, Tier.MINOR, "helix_shields"),
	PHARAH_FUEL_STORES(Hero.PHARAH, Tier.MAJOR, "fuel_stores"),
	PHARAH_ROCKET_SALVO(Hero.PHARAH, Tier.MAJOR, "rocket_salvo"),
	// Tracer's, the live list's order (D 005E.177: 2AC7, 26B6; 259B, 259D; tracer-map.md)
	TRACER_TEMPORAL_REGEN(Hero.TRACER, Tier.MINOR, "temporal_regen"),
	TRACER_KINETIC_RELOAD(Hero.TRACER, Tier.MINOR, "kinetic_reload"),
	TRACER_BLINK_PACKS(Hero.TRACER, Tier.MAJOR, "blink_packs"),
	TRACER_QUANTUM_ENTANGLEMENT(Hero.TRACER, Tier.MAJOR, "quantum_entanglement"),
	// Cassidy's, the live list's order (D 006C.177: 2527, 2ADA; 286A, 2723; cassidy-map.md)
	CASSIDY_BANG_BANG(Hero.CASSIDY, Tier.MINOR, "bang_bang"),
	CASSIDY_GIDDY_UP(Hero.CASSIDY, Tier.MINOR, "giddy_up"),
	CASSIDY_ROLLIN_ROUND_UP(Hero.CASSIDY, Tier.MAJOR, "rollin_round_up"),
	CASSIDY_SILVER_BULLET(Hero.CASSIDY, Tier.MAJOR, "silver_bullet"),
	// Junkrat's, the live list's order (D 0055.177: 252C, 26F3; 25C0, 272B; junkrat-map.md)
	JUNKRAT_NITRO_BOOST(Hero.JUNKRAT, Tier.MINOR, "nitro_boost"),
	JUNKRAT_BOMB_VOYAGE(Hero.JUNKRAT, Tier.MINOR, "bomb_voyage"),
	JUNKRAT_FRAG_CANNON(Hero.JUNKRAT, Tier.MAJOR, "frag_cannon"),
	JUNKRAT_MINE_RECYCLING(Hero.JUNKRAT, Tier.MAJOR, "mine_recycling");

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
