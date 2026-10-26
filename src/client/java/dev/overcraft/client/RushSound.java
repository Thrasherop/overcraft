package dev.overcraft.client;

import dev.overcraft.registry.OcSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Kitsune Rush's corridor sound, as Overwatch plays it (tools/reference/sounds-round8.md, #16): to each player in the
 * corridor alone, in their head (no position, no falloff). Entering plays the opening hits and fades the beds in over
 * 0.3 s; leaving fades the beds out over 2.5 s; entering again starts it over. The beds are three loops at once (they
 * play together in the game), each its own instance so each loops cleanly.
 */
public final class RushSound {
	private static final int FADE_IN_TICKS = 6;
	private static final int FADE_OUT_TICKS = 50;
	private static final SoundEvent[] BEDS = {OcSounds.KIRIKO_RUSH_PATH_1, OcSounds.KIRIKO_RUSH_PATH_2, OcSounds.KIRIKO_RUSH_PATH_3};

	private static final List<Bed> PLAYING = new ArrayList<>();
	private static boolean wasIn;

	private RushSound() {
	}

	/** Each client tick: starts or fades the corridor sound as the local player enters or leaves it. */
	public static void tick(Minecraft mc) {
		boolean in = mc.player != null && mc.level != null && HeroHud.inRushPath();
		if (in && !wasIn) {
			PLAYING.forEach(Bed::fadeOut);
			PLAYING.clear();
			play(mc, new SimpleSoundInstance(OcSounds.KIRIKO_RUSH_PATH_START.location(), SoundSource.PLAYERS, 1, 1,
				SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
			for (SoundEvent bed : BEDS) {
				Bed instance = new Bed(bed);
				if (play(mc, instance)) {
					PLAYING.add(instance);
				}
			}
		} else if (!in && wasIn) {
			PLAYING.forEach(Bed::fadeOut);
			PLAYING.clear();
		}
		wasIn = in;
	}

	/** Plays it if the pack has the sound (the jar's stand-in events are empty). */
	private static boolean play(Minecraft mc, SoundInstance sound) {
		if (mc.getSoundManager().getSoundEvent(sound.getIdentifier()) == null) {
			return false;
		}
		mc.getSoundManager().play(sound);
		return true;
	}

	public static void clear() {
		PLAYING.forEach(Bed::fadeOut);
		PLAYING.clear();
		wasIn = false;
	}

	/** One looping bed, at the listener, fading in, then out when told. */
	private static final class Bed extends AbstractTickableSoundInstance {
		private int age;
		private int fadingSince = -1;

		Bed(SoundEvent event) {
			super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			looping = true;
			relative = true;
			attenuation = Attenuation.NONE;
			volume = 0.01f;
		}

		void fadeOut() {
			if (fadingSince < 0) {
				fadingSince = age;
			}
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			age++;
			float in = Mth.clamp(age / (float) FADE_IN_TICKS, 0, 1);
			float out = fadingSince < 0 ? 1 : 1 - (age - fadingSince) / (float) FADE_OUT_TICKS;
			if (out <= 0) {
				stop();
				return;
			}
			volume = Math.max(0.01f, in * out);
		}
	}
}
