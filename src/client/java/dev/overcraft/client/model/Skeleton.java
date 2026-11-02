package dev.overcraft.client.model;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Posing a {@link SkinnedModel}: sampling clips into local transforms, blending them, and the skin matrices. */
public final class Skeleton {
	private Skeleton() {
	}

	/** Local transforms of every node. */
	public static final class Pose {
		public final Vector3f[] t;
		public final Quaternionf[] r;
		public final Vector3f[] s;

		public Pose(int n) {
			t = new Vector3f[n];
			r = new Quaternionf[n];
			s = new Vector3f[n];
			for (int i = 0; i < n; i++) {
				t[i] = new Vector3f();
				r[i] = new Quaternionf();
				s[i] = new Vector3f(1);
			}
		}

		public Pose rest(SkinnedModel model) {
			for (int i = 0; i < t.length; i++) {
				t[i].set(model.restT[i]);
				r[i].set(model.restR[i]);
				s[i].set(model.restS[i]);
			}
			return this;
		}
	}

	/** Samples {@code clip} at {@code time} seconds into {@code pose} (nodes the clip doesn't animate keep their value). */
	public static void sample(SkinnedModel.Clip clip, float time, Pose pose) {
		for (SkinnedModel.Channel ch : clip.channels()) {
			float[] times = ch.times();
			int n = times.length;
			int i = 0;
			while (i < n - 1 && times[i + 1] <= time) {
				i++;
			}
			int j = Math.min(i + 1, n - 1);
			float f = (j == i || ch.step()) ? 0 : Math.clamp((time - times[i]) / (times[j] - times[i]), 0, 1);
			float[] v = ch.values();
			switch (ch.path()) {
				case 'T' -> pose.t[ch.node()].set(lerp(v, i * 3, j * 3, f, 0), lerp(v, i * 3, j * 3, f, 1), lerp(v, i * 3, j * 3, f, 2));
				case 'S' -> pose.s[ch.node()].set(lerp(v, i * 3, j * 3, f, 0), lerp(v, i * 3, j * 3, f, 1), lerp(v, i * 3, j * 3, f, 2));
				case 'R' -> {
					Quaternionf a = new Quaternionf(v[i * 4], v[i * 4 + 1], v[i * 4 + 2], v[i * 4 + 3]);
					Quaternionf b = new Quaternionf(v[j * 4], v[j * 4 + 1], v[j * 4 + 2], v[j * 4 + 3]);
					pose.r[ch.node()].set(a.slerp(b, f).normalize());
				}
				default -> {
				}
			}
		}
	}

	/**
	 * Adds an additive clip (Overwatch's additive layers: breathing, sway, walk bob, aim) at {@code time} on top of
	 * {@code pose}, scaled by {@code weight}. Each keyed bone's delta is composed after its pose, as one transform: base x
	 * delta. Its turn turns the bone further in its own frame, and its offset is taken in the bone's own frame too
	 * (turned by the bone's pose before this clip's turn), not added in the parent's. That is how the io_scene_owm
	 * add-on imports an additive clip (BLAnimation.populateActionAdditive: basis @ delta), and what the data shows:
	 * mirrored pairs (Juno's torpedo pods 3CFB / 3CFC, the second turned 180 degrees about X; her 1P 2C1D / 2C1E; the
	 * clavicles of Sombra's 1P knockdown) carry deltas that are exact mirror images only read in each bone's frame
	 * (techniques/animation/additive_clips.md, #75).
	 */
	public static void addAdditive(SkinnedModel.Clip clip, float time, Pose pose, float weight) {
		addAdditive(clip, time, pose, weight, null);
	}

	/** The same, on the nodes in {@code mask} only (all when null). */
	public static void addAdditive(SkinnedModel.Clip clip, float time, Pose pose, float weight, boolean @Nullable [] mask) {
		if (weight <= 0) {
			return;
		}
		Pose delta = new Pose(pose.t.length);
		sample(clip, time, delta);
		Quaternionf none = new Quaternionf();
		// the base pose's turns, before this clip's own (a clip's turn channel may come before its offset channel)
		Quaternionf[] base = new Quaternionf[pose.r.length];
		for (int i = 0; i < base.length; i++) {
			base[i] = new Quaternionf(pose.r[i]);
		}
		for (SkinnedModel.Channel ch : clip.channels()) {
			int i = ch.node();
			if (mask != null && !mask[i]) {
				continue;
			}
			switch (ch.path()) {
				case 'T' -> pose.t[i].add(base[i].transform(new Vector3f(delta.t[i]).mul(weight)));
				case 'R' -> pose.r[i].mul(new Quaternionf(none).slerp(delta.r[i], weight)).normalize();
				default -> {
				}
			}
		}
	}

	private static float lerp(float[] v, int a, int b, float f, int c) {
		return v[a + c] + (v[b + c] - v[a + c]) * f;
	}

	/** {@code into = mix(into, other, w)} for the nodes in {@code mask} (all when null). */
	public static void blend(Pose into, Pose other, float w, boolean[] mask) {
		if (w <= 0) {
			return;
		}
		for (int i = 0; i < into.t.length; i++) {
			if (mask != null && !mask[i]) {
				continue;
			}
			into.t[i].lerp(other.t[i], w);
			into.s[i].lerp(other.s[i], w);
			into.r[i].slerp(other.r[i], w).normalize();
		}
	}

	/** One node's world (model-space) matrix in the pose. */
	public static Matrix4f world(SkinnedModel model, Pose pose, int node) {
		Matrix4f m = new Matrix4f().translationRotateScale(pose.t[node], pose.r[node], pose.s[node]);
		for (int p = model.parent[node]; p >= 0; p = model.parent[p]) {
			m = new Matrix4f().translationRotateScale(pose.t[p], pose.r[p], pose.s[p]).mul(m);
		}
		return m;
	}

	/**
	 * Skin matrices (joint world matrix x inverse bind) for the pose. {@code head}, when set, gets an extra turn in model
	 * space (looking around), which its children follow.
	 */
	public static Matrix4f[] skin(SkinnedModel model, Pose pose, int head, Quaternionf headTurn) {
		int n = model.nodeNames.length;
		Matrix4f[] world = new Matrix4f[n];
		for (int k = 0; k < n; k++) {
			int i = model.order[k];
			Matrix4f local = new Matrix4f().translationRotateScale(pose.t[i], pose.r[i], pose.s[i]);
			world[i] = model.parent[i] < 0 ? local : new Matrix4f(world[model.parent[i]]).mul(local);
			if (i == head && headTurn != null) {
				Vector3f at = world[i].getTranslation(new Vector3f());
				world[i] = new Matrix4f().translation(at).rotate(headTurn).translate(-at.x, -at.y, -at.z).mul(world[i]);
			}
		}
		Matrix4f[] skin = new Matrix4f[model.joints.length];
		for (int j = 0; j < skin.length; j++) {
			skin[j] = new Matrix4f(world[model.joints[j]]).mul(model.inverseBind[j]);
		}
		return skin;
	}
}
