package dev.overcraft.client.model;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
