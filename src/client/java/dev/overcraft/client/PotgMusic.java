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
	/** When VICTORY started the track (ms), or -1: it gives way if no POTG card comes by its cue. */
	private static long victoryAt = -1;
	/** The card is due 10.35 s after VICTORY (PotgReplay.CARD_AFTER_KILL_MS + the card's lineup and dip); the slack is ours. */
	private static final long NO_CARD_MS = 15280 - 4930 + 1500;

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

	/** VICTORY's track, which a POTG card picks up on its cue. */
	public static void victory(SoundEvent event) {
		start(event);
		victoryAt = net.minecraft.util.Util.getMillis();
	}

	/**
	 * Each client tick: VICTORY's track stops if no POTG came by its cue (no play since the last one, a Minecraft-mode
	 * killer), rather than playing the POTG's part over nothing (the critique's 5).
	 */
	public static void tick() {
		if (victoryAt < 0) {
			return;
		}
		if (ClientPotg.active()) {
			victoryAt = -1;
		} else if (net.minecraft.util.Util.getMillis() - victoryAt > NO_CARD_MS) {
			victoryAt = -1;
			stop();
		}
	}

	/** The track is still playing. */
	public static boolean playing() {
		return music != null && Minecraft.getInstance().getSoundManager().isActive(music);
	}

	public static void stop() {
		victoryAt = -1;
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
