package dev.overcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/**
 * The end of the match's music as one track (the owner on 0.3.66, #161: "it should instead work alongside the music"):
 * VICTORY starts it at the dragon's killing blow (the end track from its 4.93 s), and the POTG's card starts on its 15.28 s cue
 * (PotgReplay.CARD_AFTER_KILL_MS), so the track just runs on into the intro; only a player who missed VICTORY gets the POTG's
 * own cut (from 15.28 s) at the intro. It is kept through the level changes of the replay (Minecraft stops every sound on
 * one: MinecraftSoundKeepMixin), where it had gone silent (the owner: "if it occurs in the nether, no music can be heard
 * once it starts").
 */
public final class PotgMusic {
	private static @Nullable SoundInstance music;

	private PotgMusic() {
	}

	/** Starts the track (a UI sound, as the announcer's); the one already playing stops first. */
	public static void start(SoundEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSoundManager().getSoundEvent(event.location()) == null) {
			return;
		}
		stop();
		music = SimpleSoundInstance.forUI(event, 1);
		mc.getSoundManager().play(music);
	}

	/** The track is still playing. */
	public static boolean playing() {
		return music != null && Minecraft.getInstance().getSoundManager().isActive(music);
	}

	public static void stop() {
		if (music != null) {
			Minecraft.getInstance().getSoundManager().stop(music);
			music = null;
		}
	}

	/**
	 * Every sound but the interface's (the track and the announcer) stops: what Minecraft's own stop-everything does on a level
	 * change, while the track plays.
	 */
	public static void stopAllButInterface() {
		var sounds = Minecraft.getInstance().getSoundManager();
		for (SoundSource source : SoundSource.values()) {
			if (source != SoundSource.UI && source != SoundSource.MASTER) {
				sounds.stop(null, source);
			}
		}
	}
}
