package dev.overcraft.sojourn;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Sojourn's sounds the server plays (tools/soundmaps/sojourn.txt, read for wiring in tools/reference/sojourn-sounds.md;
 * registered by OcSounds.SOJOURN_SOUNDS). "Own" is heard in the head of the player named (her, or the shooter of a hit), "_3p"
 * at the source by everyone else, the rest once for everyone; the ranges are the map's. The loops that follow a state (the
 * slide and its scrape, Overclock, the Disruptor's flight, its field and the burn inside it) are her clients'
 * (dev.overcraft.client.SojournSounds), so they end with what they follow.
 */
public final class SojournSounds {
	private SojournSounds() {
	}

	/** Her sound "sojourn." + name. */
	public static SoundEvent of(String name) {
		return BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("sojourn." + name));
	}

	/** {@code name} in her head, {@code name}_3p at {@code at} for everyone else out to {@code metres}. */
	static void split(ServerPlayer sojourn, String name, Vec3 at, double metres) {
		HeroSounds.split(sojourn, of(name), of(name + "_3p"), at, metres);
	}

	/** One copy for everyone at {@code at} (but {@code except}). */
	static void at(ServerLevel level, String name, Vec3 at, double metres, net.minecraft.world.entity.player.Player... except) {
		HeroSounds.at(level, of(name), at, metres, except);
	}

	/** In this player's head only. */
	static void own(ServerPlayer player, String name) {
		HeroSounds.toOwner(player, of(name));
	}

	/** At a point for this one player only (her own copy of a sound at her field: 9210, aud 1 with a range). */
	static void forOne(ServerPlayer listener, String name, Vec3 at, double metres) {
		listener.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(of(name)), SoundSource.PLAYERS,
			at.x, at.y, at.z, (float) Math.max(1, metres / 16), 1, listener.getRandom().nextLong()));
	}
}
