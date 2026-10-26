package dev.overcraft.hero;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
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

	/** {@code own} to the hero's player only, {@code others} to everyone else around, both at {@code at}. */
	public static void split(ServerPlayer hero, SoundEvent own, SoundEvent others, Vec3 at) {
		toOwner(hero, own, at);
		hero.level().playSound(hero, at.x, at.y, at.z, others, SoundSource.PLAYERS, 1, 1);
	}

	/** Only the hero's own player hears it (a hit confirmation, "ability not ready"). */
	public static void toOwner(ServerPlayer hero, SoundEvent sound, Vec3 at) {
		hero.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.PLAYERS,
			at.x, at.y, at.z, 1, 1, hero.getRandom().nextLong()));
	}
}
