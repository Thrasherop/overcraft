package dev.overcraft.hero;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Overwatch plays many of a hero's sounds twice over: one version for the hero's own player (close, bright: "1P") and
 * another for everyone else (further, darker: "3P"), as the sound research found in the game's banks. Same here.
 */
public final class HeroSounds {
	private HeroSounds() {
	}

	/** {@code own} to the hero's player only (following them), {@code others} to everyone else around, at {@code at}. */
	public static void split(ServerPlayer hero, SoundEvent own, SoundEvent others, Vec3 at) {
		toOwner(hero, own, at);
		hero.level().playSound(hero, at.x, at.y, at.z, others, SoundSource.PLAYERS, 1, 1);
	}

	/**
	 * Only the hero's own player hears it (a hit confirmation, "ability not ready"), and it moves with them: Overwatch's
	 * own-player sounds sit on the hero. Placed at a point instead, Swift Step's hand seal stayed where she cast it and
	 * faded as she teleported up to 15 m away: she heard only part of it (#17). ({@code at} is unused: the hero is the
	 * place.)
	 */
	public static void toOwner(ServerPlayer hero, SoundEvent sound, Vec3 at) {
		hero.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS,
			hero, 1, 1, hero.getRandom().nextLong()));
	}
}
