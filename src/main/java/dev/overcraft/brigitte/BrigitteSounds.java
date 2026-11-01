package dev.overcraft.brigitte;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Brigitte's sounds (tools/soundmaps/brigitte.txt; registered by OcSounds.BRIGITTE_SOUNDS). "Own" is heard in the head of
 * the player named (her, or the healed ally / the stunned target where the map says), "_3p" at the source by everyone
 * else, the rest once for everyone; the ranges are the map's.
 */
public final class BrigitteSounds {
	private BrigitteSounds() {
	}

	/** Her sound "brigitte." + name. */
	public static SoundEvent of(String name) {
		return BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("brigitte." + name));
	}

	/** {@code name} in her head, {@code name}_3p at {@code at} for everyone else out to {@code metres}. */
	static void split(ServerPlayer brigitte, String name, Vec3 at, double metres) {
		HeroSounds.split(brigitte, of(name), of(name + "_3p"), at, metres);
	}

	/**
	 * The same for a sound whose own copy is the entity's it plays on (the healed ally, the stunned target): a player hears
	 * {@code name} in their head and the rest {@code name}_3p; anything else only the _3p copy, at it, for everyone.
	 */
	static void on(LivingEntity target, String name, double metres) {
		if (target instanceof ServerPlayer player) {
			split(player, name, player.getEyePosition(), metres);
		} else if (target.level() instanceof ServerLevel level) {
			HeroSounds.at(level, of(name + "_3p"), target.getEyePosition(), metres);
		}
	}

	/** One copy for everyone at {@code at}. */
	static void at(ServerLevel level, String name, Vec3 at, double metres) {
		HeroSounds.at(level, of(name), at, metres);
	}

	/** Everyone but her, at {@code at}: a sound whose own copy has none (Rally's end). */
	static void others(ServerPlayer brigitte, String name, Vec3 at, double metres) {
		HeroSounds.at(brigitte.level(), of(name), at, metres, brigitte);
	}

	/** In this player's head only. */
	static void own(ServerPlayer player, String name) {
		HeroSounds.toOwner(player, of(name));
	}

	/** A loop stopped for everyone in her level (its state ended). */
	static void stop(ServerLevel level, String... names) {
		for (String name : names) {
			ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(Overcraft.id("brigitte." + name), SoundSource.PLAYERS);
			for (ServerPlayer listener : level.players()) {
				listener.connection.send(packet);
			}
		}
	}
}
