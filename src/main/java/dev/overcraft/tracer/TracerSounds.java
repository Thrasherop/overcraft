package dev.overcraft.tracer;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Tracer's sounds the server plays (tools/soundmaps/tracer_sfx.txt; registered by OcSounds.TRACER_SOUNDS), as D.Va's: "own" in
 * the head of the player named, "_3p" at the source for everyone else, the rest once for everyone. A name the pack lacks is
 * silent, so the kit plays as it is while her sound map is built.
 */
public final class TracerSounds {
	private TracerSounds() {
	}

	/** Her sound "tracer." + name, or null when it isn't registered. */
	public static @Nullable SoundEvent of(String name) {
		return BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("tracer." + name));
	}

	/** {@code name} in her head, {@code name}_3p at {@code at} for everyone else out to {@code metres}. */
	public static void split(ServerPlayer tracer, String name, Vec3 at, double metres) {
		SoundEvent own = of(name), others = of(name + "_3p");
		if (own != null) {
			HeroSounds.toOwner(tracer, own);
		}
		if (others != null) {
			HeroSounds.at(tracer.level(), others, at, metres, tracer);
		}
	}

	/** One copy for everyone at {@code at} (but {@code except}). */
	public static void at(ServerLevel level, String name, Vec3 at, double metres, Player... except) {
		SoundEvent sound = of(name);
		if (sound != null) {
			HeroSounds.at(level, sound, at, metres, except);
		}
	}

	/** One copy for everyone at {@code at} ('both' in the map). */
	public static void all(ServerLevel level, String name, Vec3 at, double metres) {
		at(level, name, at, metres);
	}

	/** In this player's head only. */
	public static void own(ServerPlayer player, String name) {
		SoundEvent sound = of(name);
		if (sound != null) {
			HeroSounds.toOwner(player, sound);
		}
	}
}
