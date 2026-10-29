package dev.overcraft.hero;

import dev.overcraft.net.OwnSoundPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Overwatch plays many of a hero's sounds twice over: one version for the hero's own player (close, bright: "1P") and
 * another for everyone else (further, darker: "3P"), as the sound research found in the game's banks. Same here. The
 * hero's own player hears their sounds in their head (Overwatch's own sounds are 2D): placed in the world, at her feet
 * or where she cast, they came from a direction and stayed behind when she moved (#17).
 */
public final class HeroSounds {
	private HeroSounds() {
	}

	/** {@code own} to the hero's player (in their head), {@code others} to everyone else around, at {@code at}. */
	public static void split(ServerPlayer hero, SoundEvent own, SoundEvent others, Vec3 at) {
		toOwner(hero, own);
		hero.level().playSound(hero, at.x, at.y, at.z, others, SoundSource.PLAYERS, 1, 1);
	}

	/**
	 * The same, others hearing it out to {@code metres} (the game's range for that copy: a volume over 1 only widens
	 * Minecraft's 16 blocks, it doesn't make it louder).
	 */
	public static void split(ServerPlayer hero, SoundEvent own, SoundEvent others, Vec3 at, double metres) {
		toOwner(hero, own);
		at(hero.level(), others, others, null, at, metres, hero);
	}

	/** A sound at a point for everyone in reach but {@code except}, heard out to {@code metres}. */
	public static void at(net.minecraft.server.level.ServerLevel level, SoundEvent sound, Vec3 at, double metres,
						  net.minecraft.world.entity.player.Player... except) {
		at(level, sound, sound, null, at, metres, except);
	}

	/**
	 * The same with one copy for {@code hero}'s side (the hero too) and another for their enemies, as Overwatch's team
	 * views do (Lifeweaver's platform and tree); {@code hero} null: everyone hears {@code friendly}.
	 */
	public static void at(net.minecraft.server.level.ServerLevel level, SoundEvent friendly, SoundEvent enemy,
						  @org.jspecify.annotations.Nullable ServerPlayer hero, Vec3 at, double metres,
						  net.minecraft.world.entity.player.Player... except) {
		float volume = (float) Math.max(1, metres / 16);
		long seed = level.getRandom().nextLong();
		listeners:
		for (ServerPlayer listener : level.players()) {
			for (net.minecraft.world.entity.player.Player skip : except) {
				if (listener == skip) {
					continue listeners;
				}
			}
			if (listener.distanceToSqr(at) > (metres + 8) * (metres + 8)) {
				continue;
			}
			SoundEvent sound = hero == null || Allies.isSelfOrAlly(hero, listener) ? friendly : enemy;
			listener.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
				BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS, at.x, at.y, at.z, volume, 1.0f, seed));
		}
	}

	/**
	 * An ultimate's cast line, as Overwatch plays it: the hero and their allies hear {@code friendly}, enemies
	 * {@code enemy} (Kiriko's Japanese line, Lúcio's Portuguese one), from where the hero is, {@code volume} 16 blocks a unit.
	 */
	public static void ultLine(ServerPlayer hero, SoundEvent friendly, SoundEvent enemy, float volume) {
		toOwner(hero, friendly);
		for (ServerPlayer listener : hero.level().players()) {
			if (listener != hero) {
				SoundEvent line = Allies.isSelfOrAlly(hero, listener) ? friendly : enemy;
				listener.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
					BuiltInRegistries.SOUND_EVENT.wrapAsHolder(line), SoundSource.PLAYERS, hero.getX(), hero.getEyeY(), hero.getZ(),
					volume, 1.0f, hero.getRandom().nextLong()));
			}
		}
	}

	/** Only the hero's own player hears it, in their head (a hit confirmation, "ability not ready"). */
	public static void toOwner(ServerPlayer hero, SoundEvent sound) {
		toOwner(hero, sound, 1);
	}

	public static void toOwner(ServerPlayer hero, SoundEvent sound, float volume) {
		ServerPlayNetworking.send(hero, new OwnSoundPayload(sound.location().toString(), volume, 1, hero.getRandom().nextLong()));
	}

	/**
	 * A sound the hero makes (a voice line, a swing): in their own head, and to everyone else from the hero, following
	 * them as they move. {@code volume} sets how far others hear it (16 blocks a unit).
	 */
	public static void fromHero(ServerPlayer hero, SoundEvent sound, float volume) {
		toOwner(hero, sound);
		ClientboundSoundEntityPacket packet = new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
			SoundSource.PLAYERS, hero, volume, 1, hero.getRandom().nextLong());
		for (ServerPlayer watcher : PlayerLookup.tracking(hero)) {
			if (watcher != hero) {
				watcher.connection.send(packet);
			}
		}
	}
}
