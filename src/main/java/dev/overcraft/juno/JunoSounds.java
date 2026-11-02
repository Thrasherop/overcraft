package dev.overcraft.juno;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Juno's sounds the server plays (tools/soundmaps/juno.txt, read for wiring in tools/reference/juno-sounds.md; registered by
 * OcSounds.JUNO_SOUNDS). "Own" is heard in the head of the player named (her, or the boosted / healed ally where the map
 * says), "_3p" at the source by everyone else, the rest once for everyone; the ranges are the map's. The loops that follow a
 * state (her boots, the glide, the lock, the torpedoes' and the ring's flights, the ray's hum and the ally's amp) are her
 * clients' (dev.overcraft.client.JunoSounds), so they end with what they follow (a death, a hero switch, leaving).
 */
public final class JunoSounds {
	private JunoSounds() {
	}

	/** Her sound "juno." + name. */
	public static SoundEvent of(String name) {
		return BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("juno." + name));
	}

	/** {@code name} in her head, {@code name}_3p at {@code at} for everyone else out to {@code metres}. */
	static void split(ServerPlayer juno, String name, Vec3 at, double metres) {
		HeroSounds.split(juno, of(name), of(name + "_3p"), at, metres);
	}

	/**
	 * The same for a sound whose own copy is the entity's it plays on (the boosted ally): a player hears {@code name} in
	 * their head and the rest {@code name}_3p; anything else only the _3p copy, at it, for everyone.
	 */
	static void on(LivingEntity target, String name, double metres) {
		if (target instanceof ServerPlayer player) {
			split(player, name, player.getEyePosition(), metres);
		} else if (target.level() instanceof ServerLevel level) {
			HeroSounds.at(level, of(name + "_3p"), target.getEyePosition(), metres);
		}
	}

	/** One copy for everyone at {@code at} (but {@code except}). */
	static void at(ServerLevel level, String name, Vec3 at, double metres, net.minecraft.world.entity.player.Player... except) {
		HeroSounds.at(level, of(name), at, metres, except);
	}

	/** In this player's head only. */
	static void own(ServerPlayer player, String name) {
		HeroSounds.toOwner(player, of(name));
	}

	/** At a point for this one player only (the torpedo lock's tick at its target, which only she hears: 4B99 #220, v40). */
	static void forOne(ServerPlayer listener, String name, Vec3 at, double metres) {
		listener.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(of(name)), SoundSource.PLAYERS,
			at.x, at.y, at.z, (float) Math.max(1, metres / 16), 1, listener.getRandom().nextLong()));
	}

	/** A sound stopped for this one player (every copy of it they hear). */
	static void stopFor(ServerPlayer listener, String name) {
		listener.connection.send(new ClientboundStopSoundPacket(Overcraft.id("juno." + name), SoundSource.PLAYERS));
	}
}
