package dev.overcraft.client.comms;

import dev.overcraft.Overcraft;
import dev.overcraft.client.model.EmoteData;
import dev.overcraft.client.model.EmotePlan;
import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.client.model.HeroModels;
import dev.overcraft.client.model.SkinnedModel;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The sounds and lines of the emotes, as Overwatch plays them (#48): each emote clip's animation effect names its
 * sounds and voice lines and when in the clip they start (EmoteData, from tools/emote_data.py). Each one plays at its
 * time as the clip reaches it, at the emoting hero and heard around them (Killer Beam's "Killer Beam! ...or whatever"
 * 2.53 s in, its whoosh at 4.33 s). A sound held for a window (a dance's music bed, the length of its clip) stops when
 * the window ends, its clip plays again or the emote ends; a one-shot plays out.
 */
public final class EmoteSounds {
	/** One player's emote, as last heard. */
	private static final class Track {
		final String emote;
		final float start;
		int turn = -1;
		SkinnedModel.@Nullable Clip clip;
		float at;
		final List<Held> held = new ArrayList<>();

		Track(String emote, float start) {
			this.emote = emote;
			this.start = start;
		}

		void stop() {
			held.forEach(h -> h.sound().end());
			held.clear();
		}
	}

	/** A sound held for its window: until this time into the clip, on this play of it. */
	private record Held(Sound sound, int turn, float until) {
	}

	private static final Map<Integer, Track> TRACKS = new HashMap<>();

	private EmoteSounds() {
	}

	/** Each client tick: starts each emoting hero's sounds as their clips reach them, ends the held ones. */
	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			clear();
			return;
		}
		float now = mc.level.getGameTime() / 20f;
		Set<Integer> seen = new HashSet<>();
		for (Player player : mc.level.players()) {
			HeroAnimator animator = HeroAnimator.existing(player.getId());
			String emote = animator == null ? null : animator.emote();
			String chosen = player.getAttached(HeroManager.HERO);
			Hero hero = chosen == null ? null : Hero.byId(chosen);
			// an emote held at a time for a screenshot (DevServer "emoteat") makes no sound
			if (emote == null || hero == null || HeroAnimator.devEmoteAt >= 0) {
				continue;
			}
			HeroModels.Loaded loaded = HeroModels.get(hero);
			EmoteData.Emotes data = EmoteData.of(hero.id);
			EmotePlan plan = loaded == null ? null : EmotePlan.of(loaded.model(), emote);
			if (data == null || plan == null) {
				continue;
			}
			seen.add(player.getId());
			Track track = TRACKS.get(player.getId());
			if (track == null || track.start != animator.emoteStart() || !track.emote.equals(emote)) {
				if (track != null) {
					track.stop();
				}
				track = new Track(emote, animator.emoteStart());
				TRACKS.put(player.getId(), track);
			}
			EmotePlan.At at = plan.at(animator.emoteTime(now));
			if (at.turn() != track.turn) {
				// the rest of the clip before (a tick can pass its end), then the new one from its start
				if (track.clip != null && at.turn() > track.turn) {
					play(mc, player, hero, data, track, track.clip, track.turn, track.at, track.clip.duration());
				}
				play(mc, player, hero, data, track, at.clip(), at.turn(), -1, at.at());
			} else {
				play(mc, player, hero, data, track, at.clip(), at.turn(), track.at, at.at());
			}
			track.held.removeIf(h -> {
				if (h.turn() != at.turn() || at.at() >= h.until() || h.sound().isStopped()) {
					h.sound().end();
					return true;
				}
				return false;
			});
			track.turn = at.turn();
			track.clip = at.clip();
			track.at = at.at();
		}
		TRACKS.entrySet().removeIf(e -> {
			if (!seen.contains(e.getKey())) {
				e.getValue().stop();
				return true;
			}
			return false;
		});
	}

	/** Plays the clip's sounds that start after from and by to (seconds into it). */
	private static void play(Minecraft mc, Player player, Hero hero, EmoteData.Emotes data, Track track,
		SkinnedModel.Clip clip, int turn, float from, float to) {
		for (EmoteData.Event e : data.events(track.emote, clip.name())) {
			if (e.at() <= from || e.at() > to) {
				continue;
			}
			Identifier id = Overcraft.id(e.event(hero.id));
			if (mc.getSoundManager().getSoundEvent(id) == null) {
				// not in the player's pack (built before #48, or a sound it couldn't make)
				continue;
			}
			Sound sound = new Sound(SoundEvent.createVariableRangeEvent(id), player, e.loops());
			mc.getSoundManager().play(sound);
			if (e.duration() > 0) {
				track.held.add(new Held(sound, turn, e.at() + e.duration()));
			}
		}
	}

	public static void clear() {
		TRACKS.values().forEach(Track::stop);
		TRACKS.clear();
	}

	/** An emote's sound at the hero, following them; looping when its Wwise container does, until ended. */
	private static final class Sound extends AbstractTickableSoundInstance {
		private final Entity hero;

		Sound(SoundEvent event, Entity hero, boolean loops) {
			super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			this.hero = hero;
			looping = loops;
			x = hero.getX();
			y = hero.getY();
			z = hero.getZ();
		}

		@Override
		public void tick() {
			if (hero.isRemoved()) {
				stop();
				return;
			}
			x = hero.getX();
			y = hero.getY();
			z = hero.getZ();
		}

		void end() {
			stop();
		}
	}
}
