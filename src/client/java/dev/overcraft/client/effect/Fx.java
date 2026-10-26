package dev.overcraft.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.overcraft.client.effect.EffectArt.Palette;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The client's live effects: particles (camera-facing sprites, flat decals and rings, streaks along their motion,
 * domes) and ribbon trails behind projectiles, simulated per frame on the game's clock (they pause with it) and drawn
 * after the world's solid parts (LevelRenderEvents.COLLECT_SUBMITS). Recipes for each ability are in KirikoFx.
 */
public final class Fx {
	public enum Shape {
		/** Faces the camera. */
		SPRITE,
		/** Lies in the plane of its normal (decals, ground rings). */
		FLAT,
		/** Faces the camera but stretched along its velocity (sparks, speed lines). */
		STREAK,
		/** An upper hemisphere (Suzu's burst). */
		DOME,
		/** A mesh (the torii gate), standing on its position, its width along {@link Particle#side}. */
		MESH
	}

	/** One particle. Sizes are metres (a sprite's height; width = size x aspect); times seconds. */
	public static final class Particle {
		Identifier[] frames;
		boolean additive = true;
		Shape shape = Shape.SPRITE;
		double x, y, z;
		double vx, vy, vz;
		float gravity;
		/** Fraction of velocity kept per second. */
		float drag = 1;
		float age;
		float life = 1;
		float size0 = 1, size1 = 1;
		/** Size grows along an ease-out curve instead of linearly. */
		boolean easeOut;
		float aspect = 1;
		float rot, spin;
		float alpha = 1;
		/** Share of the life spent fading in, and fading out. */
		float fadeIn = 0.05f, fadeOut = 0.5f;
		float nx, ny = 1, nz;
		/** A streak's direction when it isn't moving (gate posts and beams). */
		float axisX, axisY = 1, axisZ;
		/** A mesh's triangles: x, y, z, u, v per corner, in metres at size 1 (see Fx.Mesh). */
		float @Nullable [] mesh;
		float sideX = 1, sideZ;
		int rgb = 0xFFFFFF;
		/** Flipbook frames per second (0: frames spread over the life). */
		float fps;
		boolean removed;
		/** Meant to be seen up close (a first-person hand's puff): not faded near the camera. */
		boolean near;

		public Particle at(Vec3 p) {
			x = p.x;
			y = p.y;
			z = p.z;
			return this;
		}

		public Particle velocity(Vec3 v) {
			vx = v.x;
			vy = v.y;
			vz = v.z;
			return this;
		}

		public Particle life(float seconds) {
			life = seconds;
			return this;
		}

		public Particle size(float from, float to) {
			size0 = from;
			size1 = to;
			return this;
		}

		public Particle fade(float in, float out) {
			fadeIn = in;
			fadeOut = out;
			return this;
		}

		public Particle normal(Vec3 n) {
			shape = Shape.FLAT;
			nx = (float) n.x;
			ny = (float) n.y;
			nz = (float) n.z;
			return this;
		}

		/** A still streak lying along this direction. */
		public Particle axis(Vec3 a) {
			shape = Shape.STREAK;
			axisX = (float) a.x;
			axisY = (float) a.y;
			axisZ = (float) a.z;
			return this;
		}

		/** Draws this mesh, scaled by size, its X axis along side (horizontal). */
		public Particle mesh(float[] triangles, Vec3 side) {
			shape = Shape.MESH;
			mesh = triangles;
			Vec3 s = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : new Vec3(side.x, 0, side.z).normalize();
			sideX = (float) s.x;
			sideZ = (float) s.z;
			return this;
		}

		public Particle shape(Shape s) {
			shape = s;
			return this;
		}

		public Particle spin(float rotation, float perSecond) {
			rot = rotation;
			spin = perSecond;
			return this;
		}

		public Particle gravity(float g) {
			gravity = g;
			return this;
		}

		public Particle drag(float keptPerSecond) {
			drag = keptPerSecond;
			return this;
		}

		public Particle alpha(float a) {
			alpha = a;
			return this;
		}

		public Particle aspect(float a) {
			aspect = a;
			return this;
		}

		public Particle easeOut() {
			easeOut = true;
			return this;
		}

		public Particle tint(int rgb) {
			this.rgb = rgb;
			return this;
		}

		public Particle near() {
			near = true;
			return this;
		}

		public Particle blended() {
			additive = false;
			return this;
		}

		float size() {
			float t = Math.clamp(age / life, 0, 1);
			if (easeOut) {
				t = 1 - (1 - t) * (1 - t);
			}
			return size0 + (size1 - size0) * t;
		}

		float opacity() {
			float t = Math.clamp(age / life, 0, 1);
			float in = fadeIn <= 0 ? 1 : Math.min(1, t / fadeIn);
			float out = fadeOut <= 0 ? 1 : Math.min(1, (1 - t) / fadeOut);
			return alpha * in * out;
		}

		Identifier frame() {
			if (frames.length == 1) {
				return frames[0];
			}
			int i = fps > 0 ? (int) (age * fps) : (int) (age / life * frames.length);
			return frames[Math.clamp(i, 0, frames.length - 1)];
		}
	}

	/** A ribbon behind a moving thing: its recent positions, each fading over the trail's duration. */
	public static final class Trail {
		final Identifier texture;
		final float width;
		final float duration;
		final float alpha;
		final ArrayDeque<double[]> points = new ArrayDeque<>();
		float lastFed;

		Trail(Identifier texture, float width, float duration, float alpha) {
			this.texture = texture;
			this.width = width;
			this.duration = duration;
			this.alpha = alpha;
		}
	}

	public static final RandomSource RANDOM = RandomSource.create();
	private static final List<Particle> PARTICLES = new ArrayList<>();
	private static final Map<Integer, Trail> TRAILS = new LinkedHashMap<>();
	private static final Map<Integer, Float> LAST_SEEN = new HashMap<>();
	private static final Map<String, Particle> ATTACHED = new HashMap<>();
	private static final int MAX_PARTICLES = 4000;
	/** Sprites closer to the camera than this (metres) aren't drawn; they fade in over the next NEAR_FADE. */
	private static final float NEAR_FADE_START = 0.3f;
	private static final float NEAR_FADE = 1.2f;
	private static float lastTime = -1;

	private Fx() {
	}

	/** The effects' clock: game time in seconds, stopping when the game pauses. */
	public static float now() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return 0;
		}
		return (mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)) / 20f;
	}

	/**
	 * A particle using the effect mask name in the palette; null (and nothing drawn) without the local pack. Several
	 * frames: "smoke#48" loads smoke_0 to smoke_47.
	 */
	public static @Nullable Particle particle(String mask, Palette palette) {
		Identifier[] frames;
		int hash = mask.indexOf('#');
		if (hash >= 0) {
			String base = mask.substring(0, hash);
			int count = Integer.parseInt(mask.substring(hash + 1));
			frames = new Identifier[count];
			for (int i = 0; i < count; i++) {
				frames[i] = EffectArt.get(base + "_" + i, palette);
				if (frames[i] == null) {
					return null;
				}
			}
		} else {
			Identifier one = EffectArt.get(mask, palette);
			if (one == null) {
				return null;
			}
			frames = new Identifier[] {one};
		}
		Particle p = new Particle();
		p.frames = frames;
		p.additive = palette != Palette.NATURAL;
		return p;
	}

	public static void add(@Nullable Particle p) {
		if (p != null && PARTICLES.size() < MAX_PARTICLES) {
			PARTICLES.add(p);
		}
	}

	/** Feeds a ribbon behind entity id with its position this frame (from its renderer). */
	public static void trail(int id, Vec3 at, String mask, Palette palette, float width, float duration, float alpha) {
		Trail trail = TRAILS.get(id);
		if (trail == null) {
			Identifier texture = EffectArt.get(mask, palette);
			if (texture == null) {
				return;
			}
			trail = new Trail(texture, width, duration, alpha);
			TRAILS.put(id, trail);
		}
		float now = now();
		double[] last = trail.points.peekLast();
		if (last == null || Math.abs(last[0] - at.x) + Math.abs(last[1] - at.y) + Math.abs(last[2] - at.z) > 0.01) {
			trail.points.addLast(new double[] {at.x, at.y, at.z, now});
		}
		trail.lastFed = now;
	}

	/**
	 * A particle that follows something (a glow on a projectile): made once, then moved here each frame and kept
	 * lit; once no longer fed it fades out over the rest of its life.
	 */
	public static void attach(String key, Vec3 at, java.util.function.Supplier<@Nullable Particle> make) {
		Particle p = ATTACHED.get(key);
		if (p == null || p.removed) {
			p = make.get();
			if (p == null) {
				return;
			}
			ATTACHED.put(key, p);
			add(p);
		}
		p.at(at);
		p.age = Math.min(p.age, p.life * (1 - p.fadeOut));
	}

	/** Seconds since this entity's effects last asked (0 the first time): for emitters with a rate per second. */
	public static float since(int id) {
		float now = now();
		Float last = LAST_SEEN.put(id, now);
		return last == null ? 0 : Math.clamp(now - last, 0, 0.25f);
	}

	/** World left: forget everything. */
	public static void clear() {
		PARTICLES.clear();
		TRAILS.clear();
		LAST_SEEN.clear();
		ATTACHED.clear();
		lastTime = -1;
	}

	// ---- per frame ----

	public static void render(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || (PARTICLES.isEmpty() && TRAILS.isEmpty())) {
			lastTime = now();
			return;
		}
		float now = now();
		float dt = lastTime < 0 ? 0 : Math.clamp(now - lastTime, 0, 0.25f);
		lastTime = now;
		simulate(dt);

		Camera camera = mc.gameRenderer.mainCamera();
		Vec3 cam = camera.position();
		Vector3f right = new Vector3f(camera.leftVector()).negate();
		Vector3f up = new Vector3f(camera.upVector());
		PoseStack poseStack = context.poseStack();

		// one draw per texture and blend
		Map<RenderType, List<Particle>> byType = new HashMap<>();
		for (Particle p : PARTICLES) {
			if (p.age < 0) {
				continue;
			}
			Identifier tex = p.frame();
			RenderType type = p.additive ? FxRender.additive(tex) : FxRender.blended(tex);
			byType.computeIfAbsent(type, t -> new ArrayList<>()).add(p);
		}
		for (Map.Entry<RenderType, List<Particle>> e : byType.entrySet()) {
			List<Particle> list = e.getValue();
			context.submitNodeCollector().submitCustomGeometry(poseStack, e.getKey(), (pose, buffer) -> {
				for (Particle p : list) {
					emit(p, pose, buffer, cam, right, up);
				}
			});
		}
		for (Trail trail : TRAILS.values()) {
			if (trail.points.size() >= 2) {
				context.submitNodeCollector().submitCustomGeometry(poseStack, FxRender.additive(trail.texture),
					(pose, buffer) -> ribbon(trail, pose, buffer, cam, now));
			}
		}
	}

	private static void simulate(float dt) {
		Iterator<Particle> it = PARTICLES.iterator();
		while (it.hasNext()) {
			Particle p = it.next();
			p.age += dt;
			if (p.age >= p.life) {
				p.removed = true;
				it.remove();
				continue;
			}
			if (p.age < 0) {
				continue;
			}
			if (p.drag != 1) {
				double keep = Math.pow(p.drag, dt);
				p.vx *= keep;
				p.vy *= keep;
				p.vz *= keep;
			}
			p.vy -= p.gravity * dt;
			p.x += p.vx * dt;
			p.y += p.vy * dt;
			p.z += p.vz * dt;
			p.rot += p.spin * dt;
		}
		float now = now();
		LAST_SEEN.values().removeIf(t -> now - t > 2);
		ATTACHED.values().removeIf(p -> p.removed);
		Iterator<Trail> trails = TRAILS.values().iterator();
		while (trails.hasNext()) {
			Trail t = trails.next();
			while (!t.points.isEmpty() && now - t.points.peekFirst()[3] > t.duration) {
				t.points.removeFirst();
			}
			if (t.points.isEmpty() && now - t.lastFed > t.duration) {
				trails.remove();
			}
		}
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int argb) {
		buffer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
			.setNormal(pose, 0, 1, 0);
	}

	/** 0 at the camera, 1 from NEAR_FADE_START + NEAR_FADE on. */
	private static float nearFade(float distance) {
		return Math.clamp((distance - NEAR_FADE_START) / NEAR_FADE, 0, 1);
	}

	private static int argb(int rgb, float alpha) {
		return Math.clamp(Math.round(alpha * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}

	private static void emit(Particle p, PoseStack.Pose pose, VertexConsumer buffer, Vec3 cam, Vector3f right, Vector3f up) {
		float cx = (float) (p.x - cam.x);
		float cy = (float) (p.y - cam.y);
		float cz = (float) (p.z - cam.z);
		float a = p.opacity();
		if ((p.shape == Shape.SPRITE || p.shape == Shape.STREAK) && !p.near) {
			// sprites fade out as they reach the camera: one at her own position would otherwise fill the screen
			float distance = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
			a *= nearFade(distance);
		}
		if (a <= 0.003f) {
			return;
		}
		int color = argb(p.rgb, a);
		float h = p.size() / 2;
		float w = h * p.aspect;
		Vector3f ax;
		Vector3f ay;
		switch (p.shape) {
			case FLAT -> {
				Vector3f n = new Vector3f(p.nx, p.ny, p.nz).normalize();
				Vector3f t = Math.abs(n.y) < 0.95f ? new Vector3f(0, 1, 0).cross(n).normalize() : new Vector3f(1, 0, 0).cross(n).normalize();
				Vector3f b = new Vector3f(n).cross(t);
				ax = t;
				ay = b;
			}
			case STREAK -> {
				Vector3f v = new Vector3f((float) p.vx, (float) p.vy, (float) p.vz);
				if (v.lengthSquared() < 1.0E-6f) {
					v.set(p.axisX, p.axisY, p.axisZ);
				}
				if (v.lengthSquared() < 1.0E-6f) {
					ax = new Vector3f(right);
					ay = new Vector3f(up);
				} else {
					v.normalize();
					Vector3f toCam = new Vector3f(-cx, -cy, -cz).normalize();
					ax = new Vector3f(v).cross(toCam).normalize();
					ay = v;
				}
			}
			case DOME -> {
				dome(p, pose, buffer, cx, cy, cz, color);
				return;
			}
			case MESH -> {
				mesh(p, pose, buffer, cx, cy, cz, color);
				return;
			}
			default -> {
				ax = new Vector3f(right);
				ay = new Vector3f(up);
			}
		}
		if (p.rot != 0 && p.shape != Shape.STREAK) {
			float cos = (float) Math.cos(p.rot);
			float sin = (float) Math.sin(p.rot);
			Vector3f rx = new Vector3f(ax).mul(cos).add(new Vector3f(ay).mul(sin));
			Vector3f ry = new Vector3f(ay).mul(cos).sub(new Vector3f(ax).mul(sin));
			ax = rx;
			ay = ry;
		}
		ax.mul(w);
		ay.mul(h);
		vertex(pose, buffer, cx - ax.x - ay.x, cy - ax.y - ay.y, cz - ax.z - ay.z, 0, 1, color);
		vertex(pose, buffer, cx + ax.x - ay.x, cy + ax.y - ay.y, cz + ax.z - ay.z, 1, 1, color);
		vertex(pose, buffer, cx + ax.x + ay.x, cy + ax.y + ay.y, cz + ax.z + ay.z, 1, 0, color);
		vertex(pose, buffer, cx - ax.x + ay.x, cy - ax.y + ay.y, cz - ax.z + ay.z, 0, 0, color);
	}

	/** A mesh's triangles (each sent as a quad with its last corner twice), scaled and turned to its side. */
	private static void mesh(Particle p, PoseStack.Pose pose, VertexConsumer buffer, float cx, float cy, float cz, int color) {
		float[] m = p.mesh;
		if (m == null) {
			return;
		}
		float s = p.size();
		// X along side, Y up, Z along side x up
		float fx = -p.sideZ;
		float fz = p.sideX;
		for (int i = 0; i + 14 < m.length; i += 15) {
			for (int k = 0; k < 4; k++) {
				int c = i + Math.min(k, 2) * 5;
				float x = m[c] * s;
				float y = m[c + 1] * s;
				float z = m[c + 2] * s;
				vertex(pose, buffer, cx + x * p.sideX + z * fx, cy + y, cz + x * p.sideZ + z * fz, m[c + 3], m[c + 4], color);
			}
		}
	}

	/** An upper hemisphere of radius size/2, its texture wrapped twice round and once up. */
	private static void dome(Particle p, PoseStack.Pose pose, VertexConsumer buffer, float cx, float cy, float cz, int color) {
		float r = p.size() / 2;
		int around = 24;
		int rings = 8;
		for (int j = 0; j < rings; j++) {
			double e0 = Math.PI / 2 * j / rings;
			double e1 = Math.PI / 2 * (j + 1) / rings;
			for (int i = 0; i < around; i++) {
				double a0 = 2 * Math.PI * i / around + p.rot;
				double a1 = 2 * Math.PI * (i + 1) / around + p.rot;
				float u0 = 2f * i / around;
				float u1 = 2f * (i + 1) / around;
				float v0 = 1 - (float) j / rings;
				float v1 = 1 - (float) (j + 1) / rings;
				vertex(pose, buffer, cx + (float) (r * Math.cos(e0) * Math.cos(a0)), cy + (float) (r * Math.sin(e0)),
					cz + (float) (r * Math.cos(e0) * Math.sin(a0)), u0, v0, color);
				vertex(pose, buffer, cx + (float) (r * Math.cos(e0) * Math.cos(a1)), cy + (float) (r * Math.sin(e0)),
					cz + (float) (r * Math.cos(e0) * Math.sin(a1)), u1, v0, color);
				vertex(pose, buffer, cx + (float) (r * Math.cos(e1) * Math.cos(a1)), cy + (float) (r * Math.sin(e1)),
					cz + (float) (r * Math.cos(e1) * Math.sin(a1)), u1, v1, color);
				vertex(pose, buffer, cx + (float) (r * Math.cos(e1) * Math.cos(a0)), cy + (float) (r * Math.sin(e1)),
					cz + (float) (r * Math.cos(e1) * Math.sin(a0)), u0, v1, color);
			}
		}
	}

	/**
	 * The ribbon: a strip facing the camera through the trail's points, newest first, narrowing and fading toward
	 * the tail. The texture's U runs across it, V along it.
	 */
	private static void ribbon(Trail trail, PoseStack.Pose pose, VertexConsumer buffer, Vec3 cam, float now) {
		List<double[]> pts = new ArrayList<>(trail.points);
		int n = pts.size();
		float[][] left = new float[n][];
		float[][] rightSide = new float[n][];
		float[] alpha = new float[n];
		for (int i = 0; i < n; i++) {
			double[] p = pts.get(i);
			double[] prev = pts.get(Math.max(0, i - 1));
			double[] next = pts.get(Math.min(n - 1, i + 1));
			Vector3f tangent = new Vector3f((float) (next[0] - prev[0]), (float) (next[1] - prev[1]), (float) (next[2] - prev[2]));
			Vector3f rel = new Vector3f((float) (p[0] - cam.x), (float) (p[1] - cam.y), (float) (p[2] - cam.z));
			Vector3f side = tangent.cross(new Vector3f(rel).negate(), new Vector3f());
			if (side.lengthSquared() < 1.0E-8f) {
				side.set(0, 1, 0);
			}
			float life = Math.clamp(1 - (now - (float) p[3]) / trail.duration, 0, 1);
			side.normalize().mul(trail.width / 2 * (0.25f + 0.75f * life));
			left[i] = new float[] {rel.x - side.x, rel.y - side.y, rel.z - side.z};
			rightSide[i] = new float[] {rel.x + side.x, rel.y + side.y, rel.z + side.z};
			alpha[i] = trail.alpha * life * life * nearFade(rel.length());
		}
		for (int i = 0; i < n - 1; i++) {
			// oldest point first in the deque: V 1 at the tail, 0 at the head
			float v0 = 1 - (float) i / (n - 1);
			float v1 = 1 - (float) (i + 1) / (n - 1);
			int c0 = argb(0xFFFFFF, alpha[i]);
			int c1 = argb(0xFFFFFF, alpha[i + 1]);
			vertex(pose, buffer, left[i][0], left[i][1], left[i][2], 0, v0, c0);
			vertex(pose, buffer, rightSide[i][0], rightSide[i][1], rightSide[i][2], 1, v0, c0);
			vertex(pose, buffer, rightSide[i + 1][0], rightSide[i + 1][1], rightSide[i + 1][2], 1, v1, c1);
			vertex(pose, buffer, left[i + 1][0], left[i + 1][1], left[i + 1][2], 0, v1, c1);
		}
	}
}
