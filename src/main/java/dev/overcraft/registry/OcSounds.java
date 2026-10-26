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

	public static final SoundEvent KIRIKO_KUNAI_THROW = register("kiriko.kunai.throw");
	public static final SoundEvent KIRIKO_KUNAI_HIT = register("kiriko.kunai.hit");
	/** The thrower's confirmation of a critical hit. */
	public static final SoundEvent KIRIKO_KUNAI_HEADSHOT = register("kiriko.kunai.headshot");
	public static final SoundEvent KIRIKO_KUNAI_HIT_BLOCK = register("kiriko.kunai.hit_block");
	public static final SoundEvent KIRIKO_KUNAI_RELOAD = register("kiriko.kunai.reload");
	public static final SoundEvent KIRIKO_OFUDA_THROW = register("kiriko.ofuda.throw");
	public static final SoundEvent KIRIKO_OFUDA_HEAL = register("kiriko.ofuda.heal");
	public static final SoundEvent KIRIKO_OFUDA_HIT_BLOCK = register("kiriko.ofuda.hit_block");
	public static final SoundEvent KIRIKO_OFUDA_RELOAD = register("kiriko.ofuda.reload");
	public static final SoundEvent KIRIKO_SUZU_THROW = register("kiriko.suzu.throw");
	public static final SoundEvent KIRIKO_SUZU_BURST = register("kiriko.suzu.burst");
	public static final SoundEvent KIRIKO_SWIFT_STEP_CAST = register("kiriko.swift_step.cast");
	public static final SoundEvent KIRIKO_SWIFT_STEP_DEPART = register("kiriko.swift_step.depart");
	public static final SoundEvent KIRIKO_SWIFT_STEP_ARRIVE = register("kiriko.swift_step.arrive");
	public static final SoundEvent KIRIKO_CLIMB_STEP = register("kiriko.climb.step");
	public static final SoundEvent KIRIKO_RUSH_SUMMON = register("kiriko.rush.summon");
	public static final SoundEvent KIRIKO_RUSH_GATE = register("kiriko.rush.gate");
	public static final SoundEvent MELEE_SWING = register("melee.swing");
	public static final SoundEvent MELEE_HIT = register("melee.hit");
	public static final SoundEvent KIRIKO_VOICE_SWIFT_STEP = register("kiriko.voice.swift_step");
	public static final SoundEvent KIRIKO_VOICE_SUZU = register("kiriko.voice.suzu");
	public static final SoundEvent KIRIKO_VOICE_HEAL = register("kiriko.voice.heal");

	private OcSounds() {
	}

	private static SoundEvent register(String name) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, Overcraft.id(name), SoundEvent.createVariableRangeEvent(Overcraft.id(name)));
	}

	public static void init() {
	}
}
