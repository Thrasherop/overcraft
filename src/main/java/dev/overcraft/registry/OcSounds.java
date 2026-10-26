package dev.overcraft.registry;

import dev.overcraft.Overcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

/**
 * The heroes' sounds. The jar's sounds.json points each at a vanilla sound so the mod works on its own; the
 * player's local asset pack (tools/build_asset_pack.py sounds) replaces them with Overwatch's own recordings.
 */
public final class OcSounds {
	/** Kitsune Rush's cast line as allies (and Kiriko) hear it: "Let the Kitsune guide you!" */
	public static final SoundEvent KIRIKO_ULT_FRIENDLY = register("kiriko.ult.friendly");
	/** ...and as enemies hear it: "(Japanese) O fox spirit, unleash your claws!" */
	public static final SoundEvent KIRIKO_ULT_ENEMY = register("kiriko.ult.enemy");

	private OcSounds() {
	}

	private static SoundEvent register(String name) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, Overcraft.id(name), SoundEvent.createVariableRangeEvent(Overcraft.id(name)));
	}

	public static void init() {
	}
}
