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
	private @Nullable String fromClip;
	private float fadingOutFrom = -1;
	private int kunaiThrows;
	private float channelUntil = -1;

	/** An action was triggered (by name, as the server sends them). */
	void trigger(String action, float now) {
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
			case "reload" -> start(now, "reload_start", "reload");
			default -> start(now, action);
		}
	}

	private void start(float now, String... names) {
		steps.clear();
		steps.addAll(List.of(names));
		step = 0;
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
				break;
			}
			if (step == steps.size() - 1) {
				break; // the last step holds its end while fading out
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
		if (step == 0 && local < 0) {
			// swinging in: ease from the movement pose to the clip's first frame
			float in = 1 + local / SWING_IN;
			weight = Math.max(0, in * in * (3 - 2 * in));
		}
		if (step == steps.size() - 1 && local > clip.duration()) {
			if (fadingOutFrom < 0) {
				fadingOutFrom = now;
			}
			weight = 1 - (now - fadingOutFrom) / FADE;
			if (weight <= 0) {
				step = -1;
				return;
			}
		}
		Skeleton.Pose act = new Skeleton.Pose(model.nodeNames.length).rest(model);
		Skeleton.sample(clip, Math.clamp(local, 0, clip.duration()), act);
		// crossfade from the previous step's last frame
		SkinnedModel.Clip from = fromClip == null ? null : model.clips.get(fromClip);
		if (from != null && local < FADE && local >= 0) {
			Skeleton.Pose before = new Skeleton.Pose(model.nodeNames.length).rest(model);
			Skeleton.sample(from, from.duration(), before);
			Skeleton.blend(before, act, local / FADE, null);
			act = before;
		}
		Skeleton.blend(pose, act, weight, mask);
	}

	/** "throw_kunai_#": the next arm's throw, alternating through the model's throw_kunai_1, _2, ... */
	private String resolve(SkinnedModel model, String name) {
		if (!name.endsWith("_#")) {
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
