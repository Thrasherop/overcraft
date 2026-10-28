package dev.overcraft.client;

import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.hero.Hero;
import dev.overcraft.jetpackcat.JetpackCat;
import dev.overcraft.registry.OcSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Jetpack Cat's own sounds that follow a state, in her head as Overwatch plays a hero's own (tools/soundmaps/
 * jetpack_cat.txt): the jetpack's hum while she flies, Frenetic Flight's start, loop and end, the energy refilling, the
 * tow, Catnapper's aim, dive and tether; and the jetpack's up and down as Space and Crouch are pressed. The data holds
 * these loops only in her own copy: the others hear her shots and reloads (the server's).
 */
public final class JetpackCatSounds {
	private enum Loop {
		HUM, BOOST, REGEN, TOW, AIM, DIVE, TETHER
	}

	private static final Map<Loop, SoundInstance> PLAYING = new EnumMap<>(Loop.class);
	private static boolean wasBoosting;
	private static boolean wasUp;
	private static boolean wasDown;
	private static int lastEnergy = -1;

	private JetpackCatSounds() {
	}

	/** Each client tick. */
	public static void tick(Minecraft mc) {
		boolean cat = mc.player != null && ClientHero.hero() == Hero.JETPACK_CAT && ClientHero.controls();
		if (!cat) {
			stopAll(mc);
			wasBoosting = false;
			lastEnergy = -1;
			return;
		}
		var sync = ClientHero.sync();
		int phase = sync.mode() & 7;
		boolean boosting = ClientHero.boostTicks() >= 0;
		int energy = sync.secondaryAmmo();
		boolean refilling = !boosting && lastEnergy >= 0 && energy > lastEnergy && energy < 100;
		lastEnergy = energy;

		hold(mc, Loop.HUM, OcSounds.JC_HUM, true);
		hold(mc, Loop.BOOST, OcSounds.JC_BOOST_LOOP, boosting);
		hold(mc, Loop.REGEN, OcSounds.JC_BOOST_REGEN, refilling || (PLAYING.containsKey(Loop.REGEN) && !boosting && energy < 100));
		hold(mc, Loop.TOW, OcSounds.JC_LIFELINE_LOOP, (sync.mode() & JetpackCat.MODE_TOWING) != 0);
		hold(mc, Loop.AIM, OcSounds.JC_ULT_AIM, phase == JetpackCat.ULT_AIM);
		hold(mc, Loop.DIVE, OcSounds.JC_ULT_DIVE_LOOP, phase == JetpackCat.ULT_WINDUP || phase == JetpackCat.ULT_DIVE);
		hold(mc, Loop.TETHER, OcSounds.JC_ULT_TETHER, phase == JetpackCat.ULT_CARRY);

		if (boosting && !wasBoosting) {
			once(mc, OcSounds.JC_BOOST_START);
		} else if (!boosting && wasBoosting) {
			once(mc, OcSounds.JC_BOOST_END);
		}
		wasBoosting = boosting;
		// 74BA / 74BB, as up or down is pressed (her graph 59DC: v10912 going to +1 or -1)
		boolean up = HeroControls.get(HeroControls.Action.JUMP).isDown();
		boolean down = HeroControls.get(HeroControls.Action.CROUCH).isDown();
		if (up && !wasUp) {
			once(mc, OcSounds.JC_UP);
		}
		if (down && !wasDown) {
			once(mc, OcSounds.JC_DOWN);
		}
		wasUp = up;
		wasDown = down;
	}

	/** Keeps a loop going while {@code on}, its file started again each time it ends; stops it once off. */
	private static void hold(Minecraft mc, Loop loop, SoundEvent event, boolean on) {
		SoundInstance playing = PLAYING.get(loop);
		if (!on) {
			if (playing != null) {
				mc.getSoundManager().stop(playing);
				PLAYING.remove(loop);
			}
			return;
		}
		if (playing != null && mc.getSoundManager().isActive(playing)) {
			return;
		}
		SoundInstance sound = own(mc, event);
		if (sound != null) {
			mc.getSoundManager().play(sound);
			PLAYING.put(loop, sound);
		}
	}

	private static void once(Minecraft mc, SoundEvent event) {
		SoundInstance sound = own(mc, event);
		if (sound != null) {
			mc.getSoundManager().play(sound);
		}
	}

	/** In her head: no position, no falloff. Null without the local pack (the jar's entries hold no sound). */
	private static @Nullable SoundInstance own(Minecraft mc, SoundEvent event) {
		var registered = mc.getSoundManager().getSoundEvent(event.location());
		if (registered == null || registered.getSound(SoundInstance.createUnseededRandom()) == null) {
			return null;
		}
		return new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, 1, 1, SoundInstance.createUnseededRandom(), false, 0,
			SoundInstance.Attenuation.NONE, 0, 0, 0, true);
	}

	private static void stopAll(Minecraft mc) {
		PLAYING.values().forEach(s -> mc.getSoundManager().stop(s));
		PLAYING.clear();
	}
}
