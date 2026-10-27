package dev.overcraft.client.model;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Cloth that the clips don't move: Overwatch drives its cloth bones (cloth_*) with physics, never with animation, so
 * played from the clips Kiriko's coat tails hang rigid from her hips and her legs pass straight through them (the
 * owner: "kiri's fabric things being clipped into legs"). Here each long chain of cloth bones hanging from the body is
 * simulated every frame, in model space: pinned at its top, pulled back toward its drape at rest (firmly near the
 * waist, loosely at the hem), swinging with gravity and trailing as she moves, kept to its bones' lengths, and pushed
 * out of capsules around the thighs and shins. The result is written back as the cloth bones' rotations.
 *
 * <p>The numbers (stiffness, damping, capsule radii) are ours, tuned by eye: the game's cloth settings aren't in the
 * data we read.
 */
public final class ClothSim {
	/** Chains shorter than this are left alone (the trouser cuffs at the knees: they ride the shins already). */
	private static final int MIN_CHAIN = 6;
	private static final float GRAVITY = 9.8f;
	/** Share of the velocity a joint keeps per second (air drag). */
	private static final float KEEP_PER_SECOND = 0.04f;
	/** Pull back toward the rest drape per 60th of a second: at the waist, and at the hem. */
	private static final float STIFF_TOP = 0.55f;
	private static final float STIFF_HEM = 0.12f;
	/** How much of her own movement the cloth lags behind (1 = it would hang still in the air as she runs). */
	private static final float INERTIA = 0.6f;
	/** The legs' capsules, with the baggy trousers. */
	private static final float THIGH_RADIUS = 0.15f;
	private static final float SHIN_RADIUS = 0.11f;
	/**
	 * How much a pushed-out point goes outward from the body rather than straight off the leg: straight off, the chains
	 * either side of a leg part around it and the cloth between them still crosses it; outward, they lift over it.
	 */
	private static final float OUTWARD = 0.7f;
	private static final int ITERATIONS = 3;
	/**
	 * The ground in model space (her feet's origin), and how far above it the cloth stays (metres, ours): sitting or
	 * kneeling in an emote, the coat went 0.6-0.7 m under the floor (the emote critique, 2026-10-27).
	 */
	private static final float FLOOR = 0.02f;
	/** Dev only (DevServer "clothfloor"): the floor constraint on (off shows what it fixed). */
	public static boolean devFloor = true;
	/** Dev only: the lowest the cloth went in the last step (model space, metres). */
	public static float devLowest;

	private record Chain(int[] nodes, float[] length) {
	}

	private record Capsule(int from, int to, float radius) {
	}

	private final List<Chain> chains;
	private final List<Capsule> capsules;
	private final Vector3f[][] pos;
	private final Vector3f[][] prev;
	private boolean started;
	/**
	 * When it last stepped (seconds): its own clock. Her pose can be worked out more than once a frame, and the
	 * animator's shared clock gave every call after the first no time, which restarted the cloth from its drape every
	 * frame in an emote (the coat never fell to the floor: found with DevServer's clothLowest, 2026-10-27).
	 */
	private float lastNow = -1;

	private ClothSim(List<Chain> chains, List<Capsule> capsules) {
		this.chains = chains;
		this.capsules = capsules;
		pos = new Vector3f[chains.size()][];
		prev = new Vector3f[chains.size()][];
		for (int c = 0; c < chains.size(); c++) {
			int n = chains.get(c).nodes().length;
			pos[c] = new Vector3f[n];
			prev[c] = new Vector3f[n];
			for (int j = 0; j < n; j++) {
				pos[c][j] = new Vector3f();
				prev[c][j] = new Vector3f();
			}
		}
	}

	/** The model's cloth chains and leg capsules, or null when it has no cloth bones (or no legs to keep out). */
	public static @Nullable ClothSim of(SkinnedModel model) {
		int n = model.nodeNames.length;
		List<Capsule> capsules = new ArrayList<>();
		for (String side : new String[] {"_L", "_R"}) {
			int thigh = index(model, "Thigh" + side);
			int knee = index(model, "Knee" + side);
			int foot = index(model, "Foot" + side);
			if (thigh >= 0 && knee >= 0) {
				capsules.add(new Capsule(thigh, knee, THIGH_RADIUS));
			}
			if (knee >= 0 && foot >= 0) {
				capsules.add(new Capsule(knee, foot, SHIN_RADIUS));
			}
		}
		if (capsules.isEmpty()) {
			return null;
		}
		// rest-pose positions, for the bones' lengths
		Skeleton.Pose rest = new Skeleton.Pose(n).rest(model);
		Matrix4f[] world = worlds(model, rest);
		List<Chain> chains = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			int parent = model.parent[i];
			if (!isCloth(model, i) || parent < 0 || isCloth(model, parent)) {
				continue;
			}
			List<Integer> chain = new ArrayList<>();
			for (int at = i; at >= 0; at = firstClothChild(model, at)) {
				chain.add(at);
			}
			if (chain.size() < MIN_CHAIN) {
				continue;
			}
			int[] nodes = chain.stream().mapToInt(Integer::intValue).toArray();
			float[] length = new float[nodes.length];
			for (int j = 1; j < nodes.length; j++) {
				length[j] = position(world[nodes[j]]).distance(position(world[nodes[j - 1]]));
			}
			chains.add(new Chain(nodes, length));
		}
		return chains.isEmpty() ? null : new ClothSim(chains, capsules);
	}

	private static boolean isCloth(SkinnedModel model, int node) {
		return model.nodeNames[node].startsWith("cloth_");
	}

	private static int firstClothChild(SkinnedModel model, int node) {
		for (int i = 0; i < model.nodeNames.length; i++) {
			if (model.parent[i] == node && isCloth(model, i)) {
				return i;
			}
		}
		return -1;
	}

	private static int index(SkinnedModel model, String name) {
		for (int i = 0; i < model.nodeNames.length; i++) {
			if (model.nodeNames[i].equals(name)) {
				return i;
			}
		}
		return -1;
	}

	private static Matrix4f[] worlds(SkinnedModel model, Skeleton.Pose pose) {
		Matrix4f[] world = new Matrix4f[model.nodeNames.length];
		for (int k = 0; k < model.order.length; k++) {
			int i = model.order[k];
			Matrix4f local = new Matrix4f().translationRotateScale(pose.t[i], pose.r[i], pose.s[i]);
			world[i] = model.parent[i] < 0 ? local : new Matrix4f(world[model.parent[i]]).mul(local);
		}
		return world;
	}

	private static Vector3f position(Matrix4f m) {
		return m.getTranslation(new Vector3f());
	}

	/** Forget the motion (a new hero, a teleport): the cloth starts again from its drape. */
	public void reset() {
		started = false;
	}

	/**
	 * One frame at {@code now} (seconds), the body moving at {@code bodyVelocity} (model space, m/s). Sets the cloth bones'
	 * rotations in the pose. Called again at the same time (the pose drawn twice in a frame), it puts the cloth where it
	 * already is.
	 */
	public void step(SkinnedModel model, Skeleton.Pose pose, float now, Vector3f bodyVelocity) {
		Matrix4f[] world = worlds(model, pose);
		if (started && now == lastNow) {
			for (int c = 0; c < chains.size(); c++) {
				writeBack(model, pose, world, chains.get(c), pos[c]);
			}
			return;
		}
		float dt = lastNow < 0 ? 0 : Math.clamp(now - lastNow, 0, 0.25f);
		lastNow = now;
		Vector3f moved = new Vector3f(bodyVelocity).mul(dt);
		Vector3f[] capA = new Vector3f[capsules.size()];
		Vector3f[] capB = new Vector3f[capsules.size()];
		for (int k = 0; k < capsules.size(); k++) {
			capA[k] = position(world[capsules.get(k).from()]);
			capB[k] = position(world[capsules.get(k).to()]);
		}
		boolean restart = !started || dt <= 0 || dt > 0.2f || moved.lengthSquared() > 4;
		// the body's vertical axis, through the cloth's anchor: "outward" is away from it
		Vector3f axis = position(world[model.parent[chains.getFirst().nodes()[0]]]);
		started = true;
		float keep = (float) Math.pow(KEEP_PER_SECOND, dt);
		float frames = dt * 60;
		float lowest = Float.MAX_VALUE;
		for (int c = 0; c < chains.size(); c++) {
			Chain chain = chains.get(c);
			int n = chain.nodes().length;
			Vector3f[] p = pos[c];
			Vector3f[] q = prev[c];
			for (int j = 0; j < n; j++) {
				Vector3f drape = position(world[chain.nodes()[j]]);
				if (restart || j == 0) {
					p[j].set(drape);
					q[j].set(drape);
					continue;
				}
				// inertia and gravity, then lag behind her own motion, then back toward the drape
				Vector3f velocity = new Vector3f(p[j]).sub(q[j]).mul(keep);
				q[j].set(p[j]);
				p[j].add(velocity).add(0, -GRAVITY * dt * dt, 0).sub(new Vector3f(moved).mul(INERTIA));
				float t = (float) j / (n - 1);
				float stiff = STIFF_TOP + (STIFF_HEM - STIFF_TOP) * t;
				float pull = 1 - (float) Math.pow(1 - stiff, frames);
				p[j].lerp(drape, pull);
			}
			if (restart) {
				continue;
			}
			for (int it = 0; it < ITERATIONS; it++) {
				for (int j = 1; j < n; j++) {
					Vector3f d = new Vector3f(p[j]).sub(p[j - 1]);
					float len = d.length();
					if (len > 1.0E-6f) {
						p[j].set(p[j - 1]).add(d.mul(chain.length()[j] / len));
					}
					for (int k = 0; k < capsules.size(); k++) {
						pushOut(p[j], capA[k], capB[k], capsules.get(k).radius(), axis);
					}
					if (devFloor && p[j].y < FLOOR) {
						p[j].y = FLOOR;
					}
				}
			}
			for (int j = 1; j < n; j++) {
				lowest = Math.min(lowest, p[j].y);
			}
			writeBack(model, pose, world, chain, p);
		}
		devLowest = lowest;
	}

	/**
	 * Moves the point out of the capsule (segment a-b, radius r) when it's inside: to the capsule's surface, on the side
	 * leaning outward from the body's axis.
	 */
	private static void pushOut(Vector3f point, Vector3f a, Vector3f b, float r, Vector3f axis) {
		Vector3f ab = new Vector3f(b).sub(a);
		float t = Math.clamp(new Vector3f(point).sub(a).dot(ab) / Math.max(1.0E-8f, ab.lengthSquared()), 0, 1);
		Vector3f closest = new Vector3f(a).fma(t, ab);
		Vector3f out = new Vector3f(point).sub(closest);
		float d = out.length();
		if (d >= r) {
			return;
		}
		Vector3f radial = new Vector3f(point.x - axis.x, 0, point.z - axis.z);
		Vector3f dir = d > 1.0E-6f ? out.div(d) : new Vector3f(radial);
		if (radial.lengthSquared() > 1.0E-8f) {
			dir.lerp(radial.normalize(), OUTWARD);
		}
		// keep it off the leg's own axis: the part along the leg doesn't take the point out
		dir.sub(new Vector3f(ab).mul(dir.dot(ab) / Math.max(1.0E-8f, ab.lengthSquared())));
		if (dir.lengthSquared() < 1.0E-8f) {
			return;
		}
		point.set(closest).add(dir.normalize().mul(r));
	}

	/**
	 * Turns each bone of the chain, from the top, so its child lies toward the simulated point; updates the pose's
	 * local rotations (and the world matrices as it goes, for the bones below).
	 */
	private static void writeBack(SkinnedModel model, Skeleton.Pose pose, Matrix4f[] world, Chain chain, Vector3f[] p) {
		int[] nodes = chain.nodes();
		for (int j = 0; j + 1 < nodes.length; j++) {
			int node = nodes[j];
			int parent = model.parent[node];
			Matrix4f parentWorld = parent < 0 ? new Matrix4f() : world[parent];
			Matrix4f self = new Matrix4f(parentWorld).mul(new Matrix4f().translationRotateScale(pose.t[node], pose.r[node], pose.s[node]));
			int child = nodes[j + 1];
			Vector3f at = position(self);
			Vector3f childNow = position(new Matrix4f(self).mul(new Matrix4f().translationRotateScale(pose.t[child], pose.r[child], pose.s[child])));
			Vector3f from = childNow.sub(at);
			Vector3f to = new Vector3f(p[j + 1]).sub(at);
			if (from.lengthSquared() > 1.0E-10f && to.lengthSquared() > 1.0E-10f) {
				Quaternionf turn = new Quaternionf().rotationTo(from.normalize(), to.normalize());
				Quaternionf worldRot = self.getNormalizedRotation(new Quaternionf());
				Quaternionf parentRot = parentWorld.getNormalizedRotation(new Quaternionf());
				// local = parent^-1 * (turn * world)
				pose.r[node].set(parentRot.conjugate().mul(turn.mul(worldRot))).normalize();
				self = new Matrix4f(parentWorld).mul(new Matrix4f().translationRotateScale(pose.t[node], pose.r[node], pose.s[node]));
			}
			world[node] = self;
		}
		int last = nodes[nodes.length - 1];
		world[last] = new Matrix4f(world[model.parent[last]]).mul(
			new Matrix4f().translationRotateScale(pose.t[last], pose.r[last], pose.s[last]));
	}
}
