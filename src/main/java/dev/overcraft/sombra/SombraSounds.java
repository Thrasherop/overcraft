package dev.overcraft.sombra;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Sombra's sounds (tools/soundmaps/sombra.txt; registered by OcSounds.SOMBRA_SOUNDS). "Own" is heard in the head of the
 * player named, "_3p" at the source by everyone else, the rest once for everyone; the ranges are the map's.
 */
public final class SombraSounds {
	private SombraSounds() {
	}

	/** Her sound "sombra." + name. */
	public static SoundEvent of(String name) {
		return BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("sombra." + name));
	}

	/** {@code name} in her head, {@code name}_3p (or {@code others}) at {@code at} for everyone else out to {@code metres}. */
	static void split(ServerPlayer sombra, String name, Vec3 at, double metres) {
		HeroSounds.split(sombra, of(name), of(name + "_3p"), at, metres);
	}

	/** One copy for everyone at {@code at}. */
	static void at(ServerLevel level, String name, Vec3 at, double metres) {
		HeroSounds.at(level, of(name), at, metres);
	}

	/** In this player's head only. */
	static void own(ServerPlayer player, String name) {
		HeroSounds.toOwner(player, of(name));
	}

	/** A loop stopped for everyone in her level (its state ended). */
	static void stop(ServerLevel level, String... names) {
		for (String name : names) {
			ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(Overcraft.id("sombra." + name), SoundSource.PLAYERS);
			for (ServerPlayer listener : level.players()) {
				listener.connection.send(packet);
			}
		}
	}
}
