package dev.overcraft.client;

import dev.overcraft.hero.HeroManager;
import dev.overcraft.pharah.Pharah;
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
 * Pharah's held sound (tools/soundmaps/pharah_sfx.txt), for every Pharah this client sees, as D.Va's (client.DvaSounds): the Hover
 * Jets' thrust while she hovers (0025 #122 holds effect 00F3: 105D's bed hers, 105C's others' 80 m), from the synced state
 * (HeroManager.PHARAH_STATE's Pharah.LOOK_HOVER). Its ignition and cut-out are one-shots from the server
 * (dev.overcraft.pharah.PharahSounds: hover.start / hover.end). The loop's file is a loop length long and starts again as it ends.
 */
public final class PharahSounds {
	private static final Map<Integer, SoundInstance> HOVER = new HashMap<>();

	private PharahSounds() {
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			HOVER.values().forEach(s -> mc.getSoundManager().stop(s));
			HOVER.clear();
			return;
		}
		Set<Integer> seen = new HashSet<>();
		for (Player p : mc.level.players()) {
			if (!"pharah".equals(p.getAttached(HeroManager.HERO)) || !p.isAlive() || p.isSpectator()) {
				continue;
			}
			seen.add(p.getId());
			Integer synced = p.getAttached(HeroManager.PHARAH_STATE);
			hold(mc, p, p == mc.player, synced != null && (synced & Pharah.LOOK_HOVER) != 0);
		}
		HOVER.entrySet().removeIf(e -> {
			if (!seen.contains(e.getKey())) {
				mc.getSoundManager().stop(e.getValue());
				return true;
			}
			return false;
		});
	}

	private static void hold(Minecraft mc, Player p, boolean own, boolean on) {
		SoundInstance sound = HOVER.get(p.getId());
		if (!on) {
			if (sound != null) {
				mc.getSoundManager().stop(sound);
				HOVER.remove(p.getId());
			}
			return;
		}
		if (sound != null && mc.getSoundManager().isActive(sound)) {
			return;
		}
		SoundEvent event = event(mc, own ? "hover.loop" : "hover.loop_3p");
		if (event == null) {
			return;
		}
		// in her head for her; at her for everyone else, out to the 80 m of 105C (metres / 16 as the volume, sounds.md)
		SoundInstance next = own
			? new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, 1, 1, SoundInstance.createUnseededRandom(), false, 0,
				SoundInstance.Attenuation.NONE, 0, 0, 0, true)
			: new EntityBoundSoundInstance(event, SoundSource.PLAYERS, 80 / 16f, 1, p, p.getRandom().nextLong());
		mc.getSoundManager().play(next);
		HOVER.put(p.getId(), next);
	}

	/** "pharah." + name, null without the local pack (the event names no file). */
	private static @Nullable SoundEvent event(Minecraft mc, String name) {
		SoundEvent event = dev.overcraft.pharah.PharahSounds.of(name);
		if (event == null) {
			return null;
		}
		var registered = mc.getSoundManager().getSoundEvent(event.location());
		return registered != null && registered.getSound(SoundInstance.createUnseededRandom()) != null ? event : null;
	}
}
