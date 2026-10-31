package dev.overcraft.client;

import dev.overcraft.ana.Ana;
import dev.overcraft.entity.AnaDartEntity;
import dev.overcraft.registry.OcSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ana's sounds that follow a state (tools/soundmaps/ana.txt): her scope's bed while she's scoped (1A87, hers, in her
 * head) and each unscoped dart's whine in flight (19F7's 3847, 12 m, on the dart). The one-shots come from the server
 * (HeroSounds); the Nano Boost's bed is one there too, its file as long as the boost.
 */
public final class AnaSounds {
	/** What each loop is playing, by what it follows ("scope", "dart/<id>"). */
	private static final Map<String, SoundInstance> PLAYING = new HashMap<>();

	private AnaSounds() {
	}

	/** Each client tick. */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			PLAYING.values().forEach(s -> mc.getSoundManager().stop(s));
			PLAYING.clear();
			return;
		}
		Map<String, Supplier<@Nullable SoundInstance>> wanted = new HashMap<>();
		ClientAna.State mine = ClientAna.state(mc.player);
		if (mine != null && mine.has(Ana.FLAG_SCOPED) && mc.player.isAlive()) {
			wanted.put("scope", () -> inHead(mc, OcSounds.ANA_SCOPE_LOOP));
		}
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof AnaDartEntity) {
				wanted.put("dart/" + e.getId(), () -> on(mc, OcSounds.ANA_DART_FLIGHT, e, 12));
			}
		}
		for (Iterator<Map.Entry<String, SoundInstance>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, SoundInstance> entry = it.next();
			if (!wanted.containsKey(entry.getKey())) {
				mc.getSoundManager().stop(entry.getValue());
				it.remove();
			}
		}
		wanted.forEach((key, make) -> {
			SoundInstance playing = PLAYING.get(key);
			if (playing != null && mc.getSoundManager().isActive(playing)) {
				return;
			}
			// (again as its file ends: the map builds each loop one loop length long)
			SoundInstance sound = make.get();
			if (sound != null) {
				mc.getSoundManager().play(sound);
				PLAYING.put(key, sound);
			}
		});
	}

	/** Null without the local pack (the event names no file). */
	private static boolean available(Minecraft mc, SoundEvent event) {
		var registered = mc.getSoundManager().getSoundEvent(event.location());
		return registered != null && registered.getSound(SoundInstance.createUnseededRandom()) != null;
	}

	private static @Nullable SoundInstance inHead(Minecraft mc, SoundEvent event) {
		return available(mc, event) ? new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, 1, 1,
			SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true) : null;
	}

	/** On an entity, heard out to {@code metres} (a volume over 1 only widens Minecraft's 16 blocks). */
	private static @Nullable SoundInstance on(Minecraft mc, SoundEvent event, Entity entity, double metres) {
		return available(mc, event) ? new EntityBoundSoundInstance(event, SoundSource.PLAYERS, (float) Math.max(1, metres / 16), 1,
			entity, entity.getRandom().nextLong()) : null;
	}
}
