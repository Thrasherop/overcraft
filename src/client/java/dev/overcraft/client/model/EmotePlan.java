package dev.overcraft.client.model;

import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * How an emote's clips play (#4). They come out of Overwatch's data in id order with the emote's layers and its prop's
 * clips among them (tools/owm_survey.py --emotes: additive layers, a bike's or umbrella's own clips), and that order
 * isn't the order they play in (Orion Dance's way in is its second clip). So the body's chain is found from the poses
 * themselves, as they join up in the data: it starts with the clip that starts in her stance (idle's first frame).
 * One that ends there too is a one-shot (Cheer, Laugh: played once, then she's back, as in Overwatch). Otherwise the
 * clip that carries on from where it stops and comes back to that pose is the loop (a dance, a kneel), played until
 * she moves; with none, the clips that carry on from it in turn. Clips that never join on (layers, props) are left
 * out: played alone they put her in a rest pose, sunk to the waist with her arms out (found playing on LAN).
 */
public record EmotePlan(List<SkinnedModel.Clip> steps, SkinnedModel.@Nullable Clip loop, boolean ends) {
	/** Body bones compared (Overwatch's rigs share these names); a model without them can't be planned, see of(). */
	private static final String[] CORE = {"Pelvis", "Hips", "Spine_1", "Spine_2", "Spine_3", "Neck", "Head", "Clavicle_L",
		"Clavicle_R", "Arm_L", "Arm_R", "Elbow_L", "Elbow_R", "Wrist_L", "Wrist_R", "Thigh_L", "Thigh_R", "Knee_L", "Knee_R",
		"Foot_L", "Foot_R"};
	/**
	 * Two poses are the same one under this (mean 1 - |q.q'| over CORE). Measured on Kiriko's 82 emote clips: where
	 * clips join, 0.000 to 0.007; where they don't, 0.013 and up.
	 */
	private static final float SAME = 0.01f;
	/** Shorter clips (some are a single frame) are poses, not motion: never a step or a loop. */
	private static final float MIN_LENGTH = 0.05f;
	private static final int MAX_STEPS = 6;
	private static final Map<SkinnedModel, Map<String, @Nullable EmotePlan>> PLANS = new WeakHashMap<>();

	/** The emote's clips past its steps, in seconds. */
	public float total() {
		float total = 0;
		for (SkinnedModel.Clip step : steps) {
			total += step.duration();
		}
		return total;
	}

	/**
	 * Where an emote is: the clip playing, the time into it, which play of a clip this is (0 for the first step, counting
	 * on through the steps and then each turn of the loop) and, once a plan with no loop is over, how long ago it ended.
	 */
	public record At(SkinnedModel.Clip clip, float at, int turn, float past) {
	}

	/** Where the emote is t seconds in. */
	public At at(float t) {
		float into = t;
		int turn = 0;
		for (SkinnedModel.Clip step : steps) {
			if (into < step.duration()) {
				return new At(step, into, turn, 0);
			}
			into -= step.duration();
			turn++;
		}
		float past = Math.max(0, t - total());
		if (loop != null) {
			float length = Math.max(0.01f, loop.duration());
			return new At(loop, past % length, steps.size() + (int) (past / length), 0);
		}
		// it holds its last pose (or eases out, when it ends in her stance)
		return new At(steps.getLast(), steps.getLast().duration(), steps.size() - 1, past);
	}

	/** The plan for model's emote_<id>_<n> clips, or null when it has none. */
	public static @Nullable EmotePlan of(SkinnedModel model, String id) {
		return PLANS.computeIfAbsent(model, m -> new HashMap<>()).computeIfAbsent(id, e -> plan(model, e));
	}

	private record Ends(SkinnedModel.Clip clip, Skeleton.Pose first, Skeleton.Pose last) {
	}

	private static @Nullable EmotePlan plan(SkinnedModel model, String id) {
		List<SkinnedModel.Clip> clips = new ArrayList<>();
		// numbered in id order, with gaps where a clip didn't import
		for (int n = 1; n <= 32; n++) {
			SkinnedModel.Clip clip = model.clips.get("emote_" + id + "_" + n);
			if (clip != null) {
				clips.add(clip);
			}
		}
		if (clips.isEmpty()) {
			return null;
		}
		int[] core = new int[CORE.length];
		int found = 0;
		for (String name : CORE) {
			int node = model.node(name);
			if (node >= 0) {
				core[found++] = node;
			}
		}
		int[] bones = java.util.Arrays.copyOf(core, found);
		Skeleton.Pose stance = new Skeleton.Pose(model.nodeNames.length).rest(model);
		SkinnedModel.Clip idle = model.clips.get("idle");
		if (idle != null) {
			Skeleton.sample(idle, 0, stance);
		}
		List<Ends> ends = new ArrayList<>();
		for (SkinnedModel.Clip clip : clips) {
			Skeleton.Pose first = new Skeleton.Pose(model.nodeNames.length).rest(model);
			Skeleton.Pose last = new Skeleton.Pose(model.nodeNames.length).rest(model);
			Skeleton.sample(clip, 0, first);
			Skeleton.sample(clip, clip.duration(), last);
			ends.add(new Ends(clip, first, last));
		}
		Ends start = null;
		for (Ends e : ends) {
			if (bones.length > 0 && e.clip.duration() > MIN_LENGTH && distance(e.first, stance, bones) < SAME) {
				start = e;
				break;
			}
		}
		if (start == null) {
			// none starts in her stance (Listen Up, Mic Drop): the first clip, once
			return new EmotePlan(List.of(clips.getFirst()), null, true);
		}
		List<Ends> steps = new ArrayList<>(List.of(start));
		SkinnedModel.Clip loop = null;
		while (distance(steps.getLast().last, stance, bones) >= SAME && steps.size() < MAX_STEPS) {
			Skeleton.Pose at = steps.getLast().last;
			Ends next = null;
			for (Ends e : ends) {
				if (steps.contains(e) || e.clip.duration() <= MIN_LENGTH || distance(e.first, at, bones) >= SAME) {
					continue;
				}
				if (distance(e.first, e.last, bones) < SAME) {
					loop = e.clip;
					break;
				}
				if (next == null) {
					next = e;
				}
			}
			if (loop != null || next == null) {
				break;
			}
			steps.add(next);
		}
		boolean back = distance(steps.getLast().last, stance, bones) < SAME;
		return new EmotePlan(steps.stream().map(Ends::clip).toList(), loop, back);
	}

	private static float distance(Skeleton.Pose a, Skeleton.Pose b, int[] bones) {
		float sum = 0;
		for (int bone : bones) {
			Quaternionf p = a.r[bone];
			Quaternionf q = b.r[bone];
			sum += 1 - Math.min(1, Math.abs(p.x * q.x + p.y * q.y + p.z * q.z + p.w * q.w));
		}
		return bones.length == 0 ? 1 : sum / bones.length;
	}
}
