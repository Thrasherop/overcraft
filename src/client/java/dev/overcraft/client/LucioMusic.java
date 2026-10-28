package dev.overcraft.client;

import dev.overcraft.hero.HeroManager;
import dev.overcraft.registry.OcSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Lúcio's songs, as Overwatch plays them (research/lucio/voice_sounds_effects.md, section 2): bars played one after
 * another, picked at random, never one long track. His own in his head (09DC / 09DD); every other Lúcio's at him, heard
 * 50 m away (2447 / 2446). Only one song at a time: a swap is a hard cut for himself (the sting plays from the server),
 * and others hear the new song come in 468 ms later, behind the scratch the server plays them.
 */
public final class LucioMusic {
	/** Others hear his song this far (the data's 50 m: 16 blocks a unit of volume). */
	private static final float VOLUME_3P = 50 / 16f;
	/** Others hear a new song this long after the swap (the data's 468 ms), behind the scratch. */
	private static final long SWAP_DELAY_MS = 468;

	private record Playing(int song, SoundInstance bar) {
	}

	private static final Map<Integer, Playing> PLAYING = new HashMap<>();
	/** When each Lúcio's new song may start (a swap heard by someone else), by entity id. */
	private static final Map<Integer, Long> WAIT_UNTIL = new HashMap<>();

	private LucioMusic() {
	}

	/** Each client tick: keeps a bar of each Lúcio's song going, his song as the server last said. */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			clear();
			return;
		}
		long nowMs = net.minecraft.util.Util.getMillis();
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (Player player : mc.level.players()) {
			int id = player.getId();
			seen.add(id);
			Integer attached = player.getAttached(HeroManager.SONG);
			int song = "lucio".equals(player.getAttached(HeroManager.HERO)) && attached != null ? attached & 3 : 0;
			boolean own = player == mc.player;
			Playing playing = PLAYING.get(id);
			if (playing != null && playing.song() != song) {
				mc.getSoundManager().stop(playing.bar());
				PLAYING.remove(id);
				playing = null;
				if (song != 0 && !own) {
					WAIT_UNTIL.put(id, nowMs + SWAP_DELAY_MS);
				}
			}
			if (song == 0 || nowMs < WAIT_UNTIL.getOrDefault(id, 0L)) {
				continue;
			}
			if (playing == null || !mc.getSoundManager().isActive(playing.bar())) {
				SoundEvent event = own ? (song == 2 ? OcSounds.LUCIO_SONG_SPEED : OcSounds.LUCIO_SONG_HEAL)
					: (song == 2 ? OcSounds.LUCIO_SONG_SPEED_3P : OcSounds.LUCIO_SONG_HEAL_3P);
				if (mc.getSoundManager().getSoundEvent(event.location()) == null) {
					continue;
				}
				SoundInstance bar = own
					? new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, 1, 1, SoundInstance.createUnseededRandom(), false, 0,
						SoundInstance.Attenuation.NONE, 0, 0, 0, true)
					: new EntityBoundSoundInstance(event, SoundSource.PLAYERS, VOLUME_3P, 1, player, player.getRandom().nextLong());
				mc.getSoundManager().play(bar);
				PLAYING.put(id, new Playing(song, bar));
			}
		}
		PLAYING.keySet().removeIf(id -> {
			if (!seen.contains(id)) {
				mc.getSoundManager().stop(PLAYING.get(id).bar());
				return true;
			}
			return false;
		});
		WAIT_UNTIL.keySet().retainAll(seen);
	}

	public static void clear() {
		Minecraft mc = Minecraft.getInstance();
		PLAYING.values().forEach(p -> mc.getSoundManager().stop(p.bar()));
		PLAYING.clear();
		WAIT_UNTIL.clear();
	}
}
