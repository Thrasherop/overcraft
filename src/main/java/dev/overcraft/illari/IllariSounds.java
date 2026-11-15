package dev.overcraft.illari;

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
 * Illari's sounds the server plays (tools/soundmaps/illari_sfx.txt; registered by OcSounds.ILLARI_SOUNDS), as Cassidy's: "own" in
 * the head of the player named, "_3p" at the source for everyone else, the rest once for everyone. A name the pack lacks is silent.
 */
public final class IllariSounds {
	private IllariSounds() {
	}

	/** His sound "illari." + name, or null when it isn't registered. */
	public static @Nullable SoundEvent of(String name) {
		return BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("illari." + name));
	}

	/** {@code name} in his head, {@code name}_3p at {@code at} for everyone else out to {@code metres}. */
	public static void split(ServerPlayer illari, String name, Vec3 at, double metres) {
		SoundEvent own = of(name), others = of(name + "_3p");
		if (own != null) {
			HeroSounds.toOwner(illari, own);
		}
		if (others != null) {
			HeroSounds.at(illari.level(), others, at, metres, illari);
		}
	}

	/** One copy for everyone at {@code at} (but {@code except}). */
	public static void at(ServerLevel level, String name, Vec3 at, double metres, Player... except) {
		SoundEvent sound = of(name);
		if (sound != null) {
			HeroSounds.at(level, sound, at, metres, except);
		}
	}

	/** One copy for everyone at {@code at}. */
	public static void all(ServerLevel level, String name, Vec3 at, double metres) {
		at(level, name, at, metres);
	}

	/** {@code name} for him at {@code at} (a split's own copy, placed), {@code name}_3p for everyone else: a blast he set off. */
	public static void blast(ServerPlayer illari, ServerLevel level, String name, Vec3 at, double metres) {
		SoundEvent own = of(name), others = of(name + "_3p");
		if (own != null) {
			HeroSounds.at(level, own, at, metres, level.players().stream().filter(p -> p != illari).toArray(Player[]::new));
		}
		if (others != null) {
			HeroSounds.at(level, others, at, metres, illari);
		} else if (own != null) {
			HeroSounds.at(level, own, at, metres, illari);
		}
	}
}
