package dev.overcraft.client;

import dev.overcraft.dva.Dva;
import dev.overcraft.hero.HeroManager;
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
 * D.Va's sounds that follow a state (tools/soundmaps/dva_sfx.txt), for every D.Va this client sees, as Sigma's
 * (client.SigmaSounds): Defense Matrix's hum (5F57: 2AA8 hers, 2AA9 others' 70 m), Boosters' roar (1492: 5DC3 hers, 2AEF
 * others' 60 m) and Precision Fusion's hum (6E7C: DC07, hers only) from the synced state (HeroManager.DVA_STATE). Their starts
 * and stops are one-shots from the server (dev.overcraft.dva.DvaSounds). Each loop's file is a loop length long and starts
 * again as it ends; everything stops with what it follows.
 */
public final class DvaSounds {
	private static final Map<Integer, Map<String, SoundInstance>> HERS = new HashMap<>();

	private DvaSounds() {
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			HERS.values().forEach(m -> stopAll(mc, m));
			HERS.clear();
			return;
		}
		Set<Integer> seen = new HashSet<>();
		for (Player p : mc.level.players()) {
			if (!"dva".equals(p.getAttached(HeroManager.HERO)) || !p.isAlive() || p.isSpectator()) {
				continue;
			}
			seen.add(p.getId());
			Map<String, SoundInstance> hers = HERS.computeIfAbsent(p.getId(), id -> new HashMap<>());
			boolean own = p == mc.player;
			Integer synced = p.getAttached(HeroManager.DVA_STATE);
			int state = synced == null ? 0 : synced;
			hold(mc, hers, "matrix", p, own, (state & Dva.MODE_MATRIX) != 0, "matrix.loop", 70);
			hold(mc, hers, "boosters", p, own, (state & Dva.MODE_BOOSTING) != 0, "boosters.loop", 60);
			hold(mc, hers, "precision", p, true, own && (state & Dva.MODE_PRECISION) != 0, "perk.precision.loop", 0);
		}
		HERS.entrySet().removeIf(e -> {
			if (!seen.contains(e.getKey())) {
				stopAll(mc, e.getValue());
				return true;
			}
			return false;
		});
	}

	private static void hold(Minecraft mc, Map<String, SoundInstance> playing, String key, Player p, boolean own, boolean on,
							 String name, double metres) {
		SoundInstance sound = playing.get(key);
		if (!on) {
			if (sound != null) {
				mc.getSoundManager().stop(sound);
				playing.remove(key);
			}
			return;
		}
		if (sound != null && mc.getSoundManager().isActive(sound)) {
			return;
		}
		SoundEvent event = event(mc, own ? name : name + "_3p");
		if (event == null) {
			return;
		}
		SoundInstance next = own
			? new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, 1, 1, SoundInstance.createUnseededRandom(), false, 0,
				SoundInstance.Attenuation.NONE, 0, 0, 0, true)
			: new EntityBoundSoundInstance(event, SoundSource.PLAYERS, (float) Math.max(1, metres / 16), 1, p, p.getRandom().nextLong());
		mc.getSoundManager().play(next);
		playing.put(key, next);
	}

	private static void stopAll(Minecraft mc, Map<String, SoundInstance> playing) {
		playing.values().forEach(s -> mc.getSoundManager().stop(s));
		playing.clear();
	}

	/** "dva." + name, null without the local pack (the event names no file). */
	private static @Nullable SoundEvent event(Minecraft mc, String name) {
		SoundEvent event = dev.overcraft.dva.DvaSounds.of(name);
		if (event == null) {
			return null;
		}
		var registered = mc.getSoundManager().getSoundEvent(event.location());
		return registered != null && registered.getSound(SoundInstance.createUnseededRandom()) != null ? event : null;
	}
}
