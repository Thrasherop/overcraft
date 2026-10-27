package dev.overcraft.registry;

import dev.overcraft.Overcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * The heroes' sounds, one event per moment in the game. The jar's sounds.json points each at a vanilla sound so the mod
 * works on its own (voice lines at nothing); the player's local asset pack (tools/build_asset_pack.py sounds, mapped in
 * tools/soundmaps/) replaces them with Overwatch's own recordings.
 */
public final class OcSounds {
	/** Kitsune Rush's cast line as allies (and Kiriko) hear it: "Let the Kitsune guide you!" */
	public static final SoundEvent KIRIKO_ULT_FRIENDLY = register("kiriko.ult.friendly");
	/** ...and as enemies hear it: "(Japanese) O fox spirit, unleash your claws!" */
	public static final SoundEvent KIRIKO_ULT_ENEMY = register("kiriko.ult.enemy");

	// "_3P": what everyone but Kiriko's own player hears (HeroSounds.split)
	public static final SoundEvent KIRIKO_KUNAI_THROW = register("kiriko.kunai.throw");
	public static final SoundEvent KIRIKO_KUNAI_THROW_3P = register("kiriko.kunai.throw_3p");
	public static final SoundEvent KIRIKO_KUNAI_HIT = register("kiriko.kunai.hit");
	/** The thrower's confirmation of a critical hit. */
	public static final SoundEvent KIRIKO_KUNAI_HEADSHOT = register("kiriko.kunai.headshot");
	public static final SoundEvent KIRIKO_KUNAI_HIT_BLOCK = register("kiriko.kunai.hit_block");
	public static final SoundEvent KIRIKO_KUNAI_RELOAD = register("kiriko.kunai.reload");
	public static final SoundEvent KIRIKO_KUNAI_RELOAD_3P = register("kiriko.kunai.reload_3p");
	public static final SoundEvent KIRIKO_OFUDA_THROW = register("kiriko.ofuda.throw");
	public static final SoundEvent KIRIKO_OFUDA_THROW_3P = register("kiriko.ofuda.throw_3p");
	public static final SoundEvent KIRIKO_OFUDA_HEAL = register("kiriko.ofuda.heal");
	public static final SoundEvent KIRIKO_OFUDA_HIT_BLOCK = register("kiriko.ofuda.hit_block");
	public static final SoundEvent KIRIKO_SUZU_THROW = register("kiriko.suzu.throw");
	public static final SoundEvent KIRIKO_SUZU_THROW_3P = register("kiriko.suzu.throw_3p");
	public static final SoundEvent KIRIKO_SUZU_BURST = register("kiriko.suzu.burst");
	public static final SoundEvent KIRIKO_SWIFT_STEP_CAST = register("kiriko.swift_step.cast");
	public static final SoundEvent KIRIKO_SWIFT_STEP_DEPART = register("kiriko.swift_step.depart");
	public static final SoundEvent KIRIKO_SWIFT_STEP_ARRIVE = register("kiriko.swift_step.arrive");
	public static final SoundEvent KIRIKO_CLIMB_START = register("kiriko.climb.start");
	public static final SoundEvent KIRIKO_CLIMB_STEP = register("kiriko.climb.step");
	public static final SoundEvent KIRIKO_CLIMB_TOP = register("kiriko.climb.top");
	public static final SoundEvent KIRIKO_RUSH_SUMMON = register("kiriko.rush.summon");
	public static final SoundEvent KIRIKO_RUSH_CAST = register("kiriko.rush.cast");
	public static final SoundEvent KIRIKO_RUSH_CAST_3P = register("kiriko.rush.cast_3p");
	/** Each torii gate's chime, rising in pitch from the first to the sixth. */
	public static final SoundEvent[] KIRIKO_RUSH_GATES = {register("kiriko.rush.gate_1"), register("kiriko.rush.gate_2"),
		register("kiriko.rush.gate_3"), register("kiriko.rush.gate_4"), register("kiriko.rush.gate_5"), register("kiriko.rush.gate_6")};
	/** The click when an ability can't be used (Swift Step with no ally in view). */
	public static final SoundEvent KIRIKO_ABILITY_DENIED = register("kiriko.ability.denied");
	public static final SoundEvent MELEE_SWING = register("melee.swing");
	public static final SoundEvent MELEE_HIT = register("melee.hit");
	public static final SoundEvent KIRIKO_VOICE_SWIFT_STEP = register("kiriko.voice.swift_step");
	public static final SoundEvent KIRIKO_VOICE_SUZU = register("kiriko.voice.suzu");
	public static final SoundEvent KIRIKO_VOICE_HEAL = register("kiriko.voice.heal");
	/** Lines for the game's moments (Voice.say looks them up by hero: "<hero>.voice.<moment>"). */
	public static final SoundEvent KIRIKO_VOICE_ULT_READY = register("kiriko.voice.ult_ready");
	public static final SoundEvent KIRIKO_VOICE_ELIMINATION = register("kiriko.voice.elimination");
	public static final SoundEvent KIRIKO_VOICE_MELEE_KILL = register("kiriko.voice.melee_kill");
	public static final SoundEvent KIRIKO_VOICE_MULTIKILL = register("kiriko.voice.multikill");
	public static final SoundEvent KIRIKO_VOICE_RESPAWN = register("kiriko.voice.respawn");
	public static final SoundEvent KIRIKO_VOICE_SELECT = register("kiriko.voice.select");
	public static final SoundEvent KIRIKO_VOICE_DEATH = register("kiriko.voice.death");
	public static final SoundEvent KIRIKO_VOICE_PAIN = register("kiriko.voice.pain");
	public static final SoundEvent KIRIKO_VOICE_MELEE = register("kiriko.voice.melee");
	public static final SoundEvent KIRIKO_VOICE_ON_FIRE = register("kiriko.voice.on_fire");
	/** Kitsune Rush's corridor, heard by each player in it (RushSound, #16): the opening hits, and the three looping beds. */
	public static final SoundEvent KIRIKO_RUSH_PATH_START = register("kiriko.rush.path_start");
	public static final SoundEvent KIRIKO_RUSH_PATH_1 = register("kiriko.rush.path_1");
	public static final SoundEvent KIRIKO_RUSH_PATH_2 = register("kiriko.rush.path_2");
	public static final SoundEvent KIRIKO_RUSH_PATH_3 = register("kiriko.rush.path_3");
	/**
	 * Kiriko's lines for one ally hero or one kind of kill, "kiriko.voice.<moment>.<condition>" (Voice picks them when
	 * the condition holds: tools/reference/kiriko-voice.md, #13).
	 */
	public static final java.util.List<String> KIRIKO_VOICE_CONDITIONS = java.util.List.of("swift_step.wrecking_ball", "swift_step.widowmaker", "swift_step.sigma", "swift_step.lucio", "swift_step.roadhog", "swift_step.reinhardt", "swift_step.junker_queen", "swift_step.ashe", "swift_step.cassidy", "swift_step.mercy", "swift_step.illari", "swift_step.genji", "swift_step.hanzo", "heal.reaper", "heal.genji", "heal.hanzo", "elimination.headshot", "elimination.in_air");

	static {
		KIRIKO_VOICE_CONDITIONS.forEach(condition -> register("kiriko.voice." + condition));
	}

	private OcSounds() {
	}

	/** Overwatch's end of a won match: the announcer's "Victory!" and its music (#18). From the local pack only. */
	public static final SoundEvent ANNOUNCER_VICTORY = register("announcer.victory");
	public static final SoundEvent MUSIC_VICTORY = register("music.victory");

	private static SoundEvent register(String name) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, Overcraft.id(name), SoundEvent.createVariableRangeEvent(Overcraft.id(name)));
	}

	/**
	 * Every hero's communication-wheel lines (dev.overcraft.comms): "<hero>.voice.call_<callout>", and the ult status's
	 * charging and almost-ready pools (its ready one is the hero's ult_ready). Silent until the hero's sounds are in the
	 * local pack.
	 */
	public static void init() {
		for (dev.overcraft.hero.Hero hero : dev.overcraft.hero.Hero.values()) {
			for (dev.overcraft.comms.Callout callout : dev.overcraft.comms.Callout.values()) {
				if (callout != dev.overcraft.comms.Callout.ULTIMATE_STATUS) {
					register(hero.id + ".voice.call_" + callout.id);
				}
			}
			register(hero.id + ".voice.call_ult_charging");
			register(hero.id + ".voice.call_ult_almost");
		}
	}
}
