package dev.overcraft.sigma;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Sigma's sounds the server plays (tools/soundmaps/sigma.txt, read for wiring in tools/reference/sigma-sounds.md; registered
 * by OcSounds.SIGMA_SOUNDS). "Own" is heard in the head of the player named (him, a shooter whose shot he absorbed, a
 * lifted or struck target), "_3p" at the source by everyone else, the rest once for everyone; the ranges are the map's.
 * The loops that follow a state (the spheres' and the rock's flight, the barrier's travel and hum, Grasp, Flux's flight
 * and reticle, Levitation) are his clients' (dev.overcraft.client.SigmaSounds), so they end with what they follow.
 */
public final class SigmaSounds {
	private SigmaSounds() {
	}

	/** His sound "sigma." + name. */
	public static SoundEvent of(String name) {
		return BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("sigma." + name));
	}

	/** {@code name} in his head, {@code name}_3p at {@code at} for everyone else out to {@code metres}. */
	static void split(ServerPlayer sigma, String name, Vec3 at, double metres) {
		HeroSounds.split(sigma, of(name), of(name + "_3p"), at, metres);
	}

	/**
	 * {@code name} for him at {@code at} (his own copies with a range: the bursts, the walls his spheres hit, the barrier:
	 * aud 1 with a range, played there for him), {@code name}_3p there for everyone else.
	 */
	static void splitAt(ServerPlayer sigma, String name, Vec3 at, double ownMetres, double metres) {
		forOne(sigma, name, at, ownMetres);
		at(sigma.level(), name + "_3p", at, metres, sigma);
	}

	/** One copy for everyone at {@code at} (but {@code except}). */
	static void at(ServerLevel level, String name, Vec3 at, double metres, Player... except) {
		HeroSounds.at(level, of(name), at, metres, except);
	}

	/** In this player's head only. */
	static void own(ServerPlayer player, String name) {
		HeroSounds.toOwner(player, of(name));
	}

	/** At a point for this one player only. */
	static void forOne(ServerPlayer listener, String name, Vec3 at, double metres) {
		listener.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(of(name)), SoundSource.PLAYERS,
			at.x, at.y, at.z, (float) Math.max(1, metres / 16), 1, listener.getRandom().nextLong()));
	}
}
