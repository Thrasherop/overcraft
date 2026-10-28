package dev.overcraft.client;

import dev.overcraft.mercy.Mercy;
import dev.overcraft.registry.OcSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mercy's sounds that follow a state (tools/soundmaps/mercy.txt), for every Mercy this client sees: the beam's loop in
 * either mode, Angelic Descent's start, loop and end, Valkyrie's loop, its 3 s warning (12 s in: the data's delay) and
 * its end. The local Mercy hears her own copies in her head; the others' follow them, positioned ("_3p").
 */
public final class MercySounds {
	private enum Loop {
		HEAL, BOOST, DESCENT, VALKYRIE
	}

	/** Per Mercy (entity id): what each loop is playing, and her last states. */
	private static final class Her {
		final Map<Loop, SoundInstance> playing = new java.util.EnumMap<>(Loop.class);
		boolean descending;
		boolean valkyrie;
		int valkyrieTicks;
	}

	private static final Map<Integer, Her> HERS = new HashMap<>();

	private MercySounds() {
	}

	/** Each client tick. */
	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			HERS.values().forEach(h -> stopAll(mc, h));
			HERS.clear();
			return;
		}
		Set<Integer> seen = new HashSet<>();
		for (Player player : mc.level.players()) {
			ClientMercy.State s = ClientMercy.state(player);
			if (s == null || !player.isAlive()) {
				continue;
			}
			seen.add(player.getId());
			Her her = HERS.computeIfAbsent(player.getId(), id -> new Her());
			boolean own = player == mc.player;
			hold(mc, her, Loop.HEAL, player, own, s.beamMode() == Mercy.BEAM_HEAL, OcSounds.MERCY_HEAL_LOOP, OcSounds.MERCY_HEAL_LOOP_3P);
			hold(mc, her, Loop.BOOST, player, own, s.beamMode() == Mercy.BEAM_BOOST, OcSounds.MERCY_BOOST_LOOP,
				OcSounds.MERCY_BOOST_LOOP_3P);

			// Angelic Descent: hers as her keys hold it, others' from their fall at its 2 m/s
			double vy = player.getY() - player.yo;
			boolean descending = own ? ClientMercy.descent(player) && vy < -0.05
				: !player.onGround() && vy < -0.07 && vy > -0.125 && !s.has(Mercy.FLAG_VALKYRIE);
			if (descending && !her.descending) {
				once(mc, player, own, OcSounds.MERCY_DESCENT_START, OcSounds.MERCY_DESCENT_START_3P);
			} else if (!descending && her.descending) {
				once(mc, player, own, OcSounds.MERCY_DESCENT_END, OcSounds.MERCY_DESCENT_END_3P);
			}
			her.descending = descending;
			hold(mc, her, Loop.DESCENT, player, own, descending, OcSounds.MERCY_DESCENT_LOOP, OcSounds.MERCY_DESCENT_LOOP_3P);

			boolean valkyrie = s.has(Mercy.FLAG_VALKYRIE);
			her.valkyrieTicks = valkyrie ? her.valkyrieTicks + 1 : 0;
			if (valkyrie && her.valkyrieTicks == 12 * 20) {
				once(mc, player, own, OcSounds.MERCY_VALKYRIE_ENDING, OcSounds.MERCY_VALKYRIE_ENDING_3P);
			}
			if (!valkyrie && her.valkyrie) {
				once(mc, player, own, OcSounds.MERCY_VALKYRIE_END, OcSounds.MERCY_VALKYRIE_END_3P);
			}
			her.valkyrie = valkyrie;
			hold(mc, her, Loop.VALKYRIE, player, own, valkyrie, OcSounds.MERCY_VALKYRIE_LOOP, OcSounds.MERCY_VALKYRIE_LOOP_3P);
		}
		HERS.entrySet().removeIf(e -> {
			if (!seen.contains(e.getKey())) {
				stopAll(mc, e.getValue());
				return true;
			}
			return false;
		});
	}

	/** Keeps a loop going while {@code on}, started again each time its file ends; stops it once off. */
	private static void hold(Minecraft mc, Her her, Loop loop, Player mercy, boolean own, boolean on, SoundEvent ownEvent,
		SoundEvent othersEvent) {
		SoundInstance playing = her.playing.get(loop);
		if (!on) {
			if (playing != null) {
				mc.getSoundManager().stop(playing);
				her.playing.remove(loop);
			}
			return;
		}
		if (playing != null && mc.getSoundManager().isActive(playing)) {
			return;
		}
		SoundInstance sound = sound(mc, mercy, own ? ownEvent : othersEvent, own);
		if (sound != null) {
			mc.getSoundManager().play(sound);
			her.playing.put(loop, sound);
		}
	}

	private static void once(Minecraft mc, Player mercy, boolean own, SoundEvent ownEvent, SoundEvent othersEvent) {
		SoundInstance sound = sound(mc, mercy, own ? ownEvent : othersEvent, own);
		if (sound != null) {
			mc.getSoundManager().play(sound);
		}
	}

	/** Hers in her head (no position, no falloff); another Mercy's on her. Null without the local pack. */
	private static @Nullable SoundInstance sound(Minecraft mc, Player mercy, SoundEvent event, boolean own) {
		var registered = mc.getSoundManager().getSoundEvent(event.location());
		if (registered == null || registered.getSound(SoundInstance.createUnseededRandom()) == null) {
			return null;
		}
		if (own) {
			return new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, 1, 1, SoundInstance.createUnseededRandom(), false,
				0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
		}
		return new EntityBoundSoundInstance(event, SoundSource.PLAYERS, 1, 1, mercy, mercy.getRandom().nextLong());
	}

	private static void stopAll(Minecraft mc, Her her) {
		her.playing.values().forEach(s -> mc.getSoundManager().stop(s));
		her.playing.clear();
	}
}
