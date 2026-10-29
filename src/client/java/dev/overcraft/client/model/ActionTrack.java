package dev.overcraft.client.model;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Actions (throws, casts) as Overwatch plays them: a short sequence of clips over the movement pose. A kunai throw is
 * the next arm's throw then the hands coming back; holding the ofuda is a start, throws for as long as they keep
 * coming, a hold, then the way back to the idle. One track per view (third person, first person): their clips last
 * different lengths, so each keeps its own place.
 */
final class ActionTrack {
	/** Seconds the first clip takes to swing in from the current pose to its first frame (fire clips start at the release). */
	static final float SWING_IN = 0.1f;
	/** Crossfade between steps, and the fade back to the movement pose at the end. */
	static final float FADE = 0.15f;
	/** The ofuda throw step keeps looping while throws arrive at least this often (s): Kiriko throws 7 a second. */
	private static final float CHANNEL_GAP = 0.3f;
	/**
	 * The share of the ofuda throw clip that is the flick itself: the rest draws a new fan from the belt (a fist in first
	 * person, seen in the survey renders of B0E1). Looping the whole clip at 7 throws a second showed that fist mid-volley
	 * (the second critique); about 0.16 s of its 0.63.
	 */
	private static final float FLICK_SHARE = 0.25f;

	private final List<String> steps = new ArrayList<>();
	private int step = -1;
	/** When the current step's clip started playing (s). */
	private float stepStart;
	/** Amp It Up's clip (2A81, 2.5 s), the longest one that has a priority (see priority()). */
	private static final float UNSEEN_LENGTH = 2.5f;
	/** The playing step's clip length (s), once apply() has resolved it; -1 before. */
	private float stepLength = -1;
	private @Nullable String fromClip;
	private float fadingOutFrom = -1;
	private int kunaiThrows;
	private float channelUntil = -1;
	/** When the playing step was cut short (s), -1 if it wasn't: it holds that frame while fading out. */
	private float cancelledAt = -1;
	/** The last step loops until another action (Jetpack Cat's Catnapper aim, held until she dives or lets go). */
	private boolean holdLast;

	/** An action was triggered (by name, as the server sends them). */
	void trigger(String action, float now) {
		Integer wanted = priority(action);
		Integer playing = priority(current());
		// a track not drawn since its clip started (the view model in third person, a hero off screen) doesn't know the
		// clip's length yet: the longest of these clips (Amp It Up's 2.5 s) bounds it
		float length = stepLength >= 0 ? stepLength : UNSEEN_LENGTH;
		if (wanted != null && playing != null && wanted < playing && now - stepStart <= length) {
			// a lower-priority clip doesn't cut the one playing: Crossfade during a reload leaves the reload's clip (its
			// additive layer plays over it: HeroAnimator), and a reload starting during Amp It Up leaves Amp It Up's
			return;
		}
		switch (action) {
			case "throw_kunai" -> {
				kunaiThrows++;
				start(now, "throw_kunai_#", "throw_kunai_end");
			}
			case "throw_ofuda" -> {
				channelUntil = now + CHANNEL_GAP;
				String current = current();
				if (current != null && current.startsWith("throw_ofuda") && !current.equals("throw_ofuda_end")) {
					if (current.equals("throw_ofuda_hold")) {
						jump(steps.indexOf("throw_ofuda"), now);
					}
				} else {
					start(now, "throw_ofuda_start", "throw_ofuda", "throw_ofuda_hold", "throw_ofuda_end");
				}
			}
			case "throw_suzu" -> start(now, "throw_suzu", "throw_suzu_end");
			case "swift_step" -> start(now, "swift_step", "swift_step_end");
			case "ult_cast" -> start(now, "ult_cast", "ult_cast_end");
			// one clip (alias 0x004), the kunai's fresh fan (it was an ofuda throw's variant: #11)
			case "reload" -> start(now, "reload");
			// a reload cut short (a wall climb, #42): it fades out from where it is
			case "reload_cancel" -> {
				if ("reload".equals(current()) && cancelledAt < 0) {
					cancelledAt = now;
				}
			}
			// Lúcio (lucio-map.md): his volleys and Soundwave are additive layers (HeroAnimator), not clips over the arms;
			// Amp It Up is one clip, Sound Barrier the leap then the slam
			case "fire", "soundwave" -> {
			}
			case "amp_it_up" -> start(now, "amp");
			case "sound_barrier" -> start(now, "ult_cast", "ult_end");
			// Jetpack Cat (jetpack_cat-map.md): Catnapper's aim (0x175: enter, then held), its exit, the dive (0xAFC).
			// In the third person her aim's clips are additive layers (HeroAnimator): these steps find nothing there
			case "ult_aim" -> {
				start(now, "ult_aim_start", "ult_aim");
				holdLast = true;
			}
			case "ult_dive" -> start(now, "ult_dive_start", "ult_dive");
			// Mercy: Valkyrie's cast is one clip (alias 0x1B8), as are Resurrect and Guardian Angel's launch
			case "ult_cast_blaster" -> start(now, "ult_cast_blaster");
			default -> start(now, action);
		}
	}

	/** The playing action fades out from where it is if its name starts with {@code prefix} (Hello's wave, cut by firing). */
	void cancel(String prefix, float now) {
		String current = current();
		if (current != null && current.startsWith(prefix) && cancelledAt < 0) {
			cancelledAt = now;
		}
	}

	/**
	 * An action's animation priority, from the data (each StateAnim's m_priority, a 024 asset): Lúcio's Crossfade 0090 is
	 * -1, Amp It Up's 008D 3, and his reload sets none (the default, 0). Null for the rest, whose priorities aren't read:
	 * they cut and are cut as before.
	 */
	private static @Nullable Integer priority(@Nullable String action) {
		if (action == null) {
			return null;
		}
		if (action.startsWith("crossfade")) {
			return -1;
		}
		return switch (action) {
			case "amp", "amp_it_up" -> 3;
			case "reload" -> 0;
			default -> null;
		};
	}

	private void start(float now, String... names) {
		stepLength = -1;
		holdLast = false;
		steps.clear();
		steps.addAll(List.of(names));
		step = 0;
		cancelledAt = -1;
		stepStart = now + SWING_IN;
		fromClip = null;
		fadingOutFrom = -1;
	}

	private void jump(int index, float now) {
		if (index >= 0) {
			fromClip = current();
			step = index;
			stepStart = now;
		}
	}

	private @Nullable String current() {
		return step >= 0 && step < steps.size() ? steps.get(step) : null;
	}

	/** The playing step, for the dev harness. */
	@Nullable String playing() {
		return current();
	}

	/**
	 * Blends the action over {@code pose} (only the nodes in {@code mask}, all when null). The model may lack some of a
	 * sequence's clips (stopgap models): those steps are skipped, and "throw_kunai_#" falls back to "throw_kunai".
	 */
	void apply(SkinnedModel model, Skeleton.Pose pose, float now, boolean @Nullable [] mask) {
		if (step < 0) {
			return;
		}
		SkinnedModel.Clip clip = null;
		// advance past finished (or missing) steps
		while (step < steps.size()) {
			clip = model.clips.get(resolve(model, steps.get(step)));
			if (clip == null) {
				step++;
				continue;
			}
			float local = now - stepStart;
			if (steps.get(step).equals("throw_ofuda") && now < channelUntil && local > clip.duration() * FLICK_SHARE) {
				// keep throwing: only the flick repeats while fire is held, the redraw of the fan plays when it stops
				stepStart += clip.duration() * FLICK_SHARE;
				continue;
			}
			if (local <= clip.duration()) {
				stepLength = clip.duration();
				break;
			}
			if (step == steps.size() - 1) {
				if (holdLast && clip.duration() > 1.0E-3f) {
					// loops: on to this pass of it (in one step: her first-person aim pose is a single frame, and
					// stepping by its length never caught up, freezing the game: found playing)
					stepStart += clip.duration() * (float) Math.floor(local / clip.duration());
					break;
				}
				break; // the last step holds its end while fading out (a held pose too)
			}
			fromClip = resolve(model, steps.get(step));
			stepStart += clip.duration();
			step++;
		}
		if (clip == null || step >= steps.size()) {
			step = -1;
			return;
		}
		float local = now - stepStart;
		float weight = 1;
		if (cancelledAt >= 0) {
			local = cancelledAt - stepStart;
			weight = 1 - (now - cancelledAt) / FADE;
			if (weight <= 0) {
				step = -1;
				cancelledAt = -1;
				return;
			}
		}
		if (step == 0 && local < 0) {
			// swinging in: ease from the movement pose to the clip's first frame
			float in = 1 + local / SWING_IN;
			weight = Math.max(0, in * in * (3 - 2 * in));
		}
		if (step == steps.size() - 1 && local > clip.duration() && !holdLast) {
			if (fadingOutFrom < 0) {
				fadingOutFrom = now;
			}
			weight = 1 - (now - fadingOutFrom) / FADE;
			if (weight <= 0) {
				step = -1;
				return;
			}
		}
		// over the pose under it: bones the clip doesn't drive keep that pose, as Overwatch's do (techniques: emotes). Played
		// over the rest pose, Lúcio's Crossfade (a left-hand clip, folder 0-4) dropped his right arm, and the gun on it, to
		// the bind pose, out of the first-person view
		Skeleton.Pose act = copy(pose);
		Skeleton.sample(HeroAnimator.driven(model, clip), Math.clamp(local, 0, clip.duration()), act);
		// crossfade from the previous step's last frame
		SkinnedModel.Clip from = fromClip == null ? null : model.clips.get(fromClip);
		if (from != null && local < FADE && local >= 0) {
			Skeleton.Pose before = copy(pose);
			Skeleton.sample(HeroAnimator.driven(model, from), from.duration(), before);
			Skeleton.blend(before, act, local / FADE, null);
			act = before;
		}
		Skeleton.blend(pose, act, weight, mask);
	}

	private static Skeleton.Pose copy(Skeleton.Pose pose) {
		Skeleton.Pose out = new Skeleton.Pose(pose.t.length);
		for (int i = 0; i < pose.t.length; i++) {
			out.t[i].set(pose.t[i]);
			out.r[i].set(pose.r[i]);
			out.s[i].set(pose.s[i]);
		}
		return out;
	}

	/** "throw_kunai_#": the next arm's throw, alternating through the model's throw_kunai_1, _2, ... */
	private String resolve(SkinnedModel model, String name) {
		if (!name.endsWith("_#")) {
			// a skating variant the model doesn't have (Lúcio's first-person Crossfade has one clip): the standing one
			int moving = name.indexOf("_moving");
			if (moving > 0 && !model.clips.containsKey(name)) {
				return name.substring(0, moving);
			}
			// a numbered variant the model doesn't have plays its first: the first-person model has one quick melee
			// clip, so two melees in three (melee_2, melee_3) showed nothing in first person (#24)
			int underscore = name.lastIndexOf('_');
			if (!model.clips.containsKey(name) && underscore > 0 && name.substring(underscore + 1).chars().allMatch(Character::isDigit)) {
				return name.substring(0, underscore);
			}
			return name;
		}
		String base = name.substring(0, name.length() - 2);
		int variants = 0;
		while (model.clips.containsKey(base + "_" + (variants + 1))) {
			variants++;
		}
		return variants == 0 ? base : base + "_" + (Math.floorMod(kunaiThrows - 1, variants) + 1);
	}
}
