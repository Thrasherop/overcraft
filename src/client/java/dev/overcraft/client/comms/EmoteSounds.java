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
 * 2.53 s in, its whoosh at 4.33 s), as its Wwise events have it: a held sound (a dance's music, the length of its
 * clip) restarts every loop while its window lasts and stops (fading) when the window ends, its clip plays again or
 * the emote ends; a one-shot plays out unless its event cuts it; some sound only some plays; some only the emoting
 * player hears (their own 2D copy), some only everyone else.
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
			held.forEach(Held::end);
			held.clear();
		}
	}

	/** A sound held for its window: until this time into the clip, on this play of it; restarted from next on a loop. */
	private static final class Held {
		/** Its passes still playing (a loop's new pass starts over the old one's tail). */
		final List<Sound> sounds = new ArrayList<>();
		final EmoteData.Event event;
		final int turn;
		final float until;
		float next;

		Held(Sound sound, EmoteData.Event event, int turn) {
			this.sounds.add(sound);
			this.event = event;
			this.turn = turn;
			this.until = event.at() + event.duration();
			this.next = event.loop() > 0 ? event.at() + event.loop() : Float.MAX_VALUE;
		}

		void end() {
			sounds.forEach(Sound::end);
		}
	}

	private static final Map<Integer, Track> TRACKS = new HashMap<>();
	/** How many times each "every nth play" sound has come up (Lúcio's 4A77: every 4th). */
	private static final Map<String, Integer> PLAYS = new HashMap<>();

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
			boolean own = player == mc.player;
			EmotePlan.At at = plan.at(animator.emoteTime(now));
			if (at.turn() != track.turn) {
				// the rest of the clip before (a tick can pass its end), then the new one from its start
				if (track.clip != null && at.turn() > track.turn) {
					play(mc, player, own, hero, data, track, track.clip, track.turn, track.at, track.clip.duration());
				}
				play(mc, player, own, hero, data, track, at.clip(), at.turn(), -1, at.at());
			} else {
				play(mc, player, own, hero, data, track, at.clip(), at.turn(), track.at, at.at());
			}
			for (Held h : List.copyOf(track.held)) {
				if (h.turn != at.turn() || at.at() >= h.until) {
					h.end();
					track.held.remove(h);
				} else if (at.at() >= h.next) {
					// a loop goes round again: a new pass over the old one's tail, as Wwise's do
					Sound again = start(mc, player, hero, h.event);
					if (again != null) {
						h.sounds.removeIf(Sound::isStopped);
						h.sounds.add(again);
					}
					h.next += h.event.loop();
				}
			}
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
	private static void play(Minecraft mc, Player player, boolean own, Hero hero, EmoteData.Emotes data, Track track,
		SkinnedModel.Clip clip, int turn, float from, float to) {
		for (EmoteData.Event e : data.events(track.emote, clip.name())) {
			if (e.at() <= from || e.at() > to || !heardHere(e, own) || !thisPlay(hero, e)) {
				continue;
			}
			Sound sound = start(mc, player, hero, e);
			if (sound != null && e.duration() > 0) {
				track.held.add(new Held(sound, e, turn));
			}
		}
	}

	/** Whether this client hears it: an "own" sound only the emoting player, an "others" one everyone else. */
	private static boolean heardHere(EmoteData.Event e, boolean own) {
		return e.audience() == null || e.audience().equals("own") == own;
	}

	/** Whether this play of it sounds (some sound only every nth play, or by chance). */
	private static boolean thisPlay(Hero hero, EmoteData.Event e) {
		if (e.nth() > 1) {
			return PLAYS.merge(e.event(hero.id), 1, Integer::sum) % e.nth() == 1;
		}
		return e.chance() >= 1 || java.util.concurrent.ThreadLocalRandom.current().nextFloat() < e.chance();
	}

	private static @Nullable Sound start(Minecraft mc, Player player, Hero hero, EmoteData.Event e) {
		Identifier id = Overcraft.id(e.event(hero.id));
		if (mc.getSoundManager().getSoundEvent(id) == null) {
			// not in the player's pack (built before #48, or a sound it couldn't make)
			return null;
		}
		Sound sound = new Sound(SoundEvent.createVariableRangeEvent(id), player, Math.round(e.cut() * 20), Math.round(e.fade() * 20));
		mc.getSoundManager().play(sound);
		return sound;
	}

	public static void clear() {
		TRACKS.values().forEach(Track::stop);
		TRACKS.clear();
	}

	/** An emote's sound at the hero, following them; cut after cutTicks (0 never), fading over fadeTicks when ended. */
	private static final class Sound extends AbstractTickableSoundInstance {
		private final Entity hero;
		private final int cutTicks;
		private final int fadeTicks;
		private int age;
		private int fading = -1;

		Sound(SoundEvent event, Entity hero, int cutTicks, int fadeTicks) {
			super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			this.hero = hero;
			this.cutTicks = cutTicks;
			this.fadeTicks = fadeTicks;
			x = hero.getX();
			y = hero.getY();
			z = hero.getZ();
		}

		@Override
		public void tick() {
			age++;
			if (hero.isRemoved() || cutTicks > 0 && age >= cutTicks) {
				stop();
				return;
			}
			if (fading >= 0) {
				if (++fading >= fadeTicks) {
					stop();
					return;
				}
				volume = 1 - (float) fading / fadeTicks;
			}
			x = hero.getX();
			y = hero.getY();
			z = hero.getZ();
		}

		/** Its window is over: it fades out (its event's stop fade), or stops now. */
		void end() {
			if (fadeTicks > 0) {
				fading = Math.max(fading, 0);
			} else {
				stop();
			}
		}
	}
}
