package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Illari's effects (tools/reference/illari-effects.md, read from her effects' data 2026-11-17; #150, the owner: "everything in
 * illari ult" and "illari reload sun ball"). One warm family, gold to white-hot (D1CA / 74C6 rows); the data has no team-coded
 * pair as Tracer's blue / red, so everyone sees her gold (M). Captive Sun: the cast at her hand (4EF3), the sun held at her rifle
 * once armed (4EF2), the throw (4F06), the ball in flight (4983), its burst (4E27), Sunstruck's mark (4B50) and detonation (4E17);
 * the reload's orb (4F07); the rifle's shot line (48BE), muzzle star (4BA3) and hit (4BA1). Sizes and timings from the data (H)
 * unless marked ours; the 10-30 m glows and lens flares are drawn faint or smaller (ours: they're lights in Overwatch).
 */
public final class IllariFx {
	public static final java.util.List<EffectArt.Art> ART = java.util.List.of(
		new EffectArt.Art("sojourn_dot", Palette.IL_SUN, true),
		new EffectArt.Art("glow", Palette.IL_SUN, true),
		new EffectArt.Art("illari_star", Palette.IL_SUN, false),
		new EffectArt.Art("illari_halo", Palette.IL_SUN, true),
		new EffectArt.Art("illari_burst_star", Palette.IL_SUN, false),
		new EffectArt.Art("illari_sun_mark", Palette.IL_MARK, false),
		new EffectArt.Art("illari_ember#4", Palette.IL_FIRE, false),
		new EffectArt.Art("illari_flame", Palette.IL_FIRE, false),
		new EffectArt.Art("illari_shot_line", Palette.IL_SHOT, false),
		new EffectArt.Art("illari_hit_glow", Palette.IL_SHOT, false),
		new EffectArt.Art("illari_scorch", Palette.IL_SCORCH, false),
		new EffectArt.Art("spark#4", Palette.IL_FIRE, false),
		new EffectArt.Art("sojourn_dot", Palette.IL_SUN, false),
		new EffectArt.Art("sojourn_dot", Palette.IL_MARK, false));
	public static final java.util.List<EffectArt.Art> ART_ENEMY = ART;

	private IllariFx() {
	}

	private static final Palette SUN = Palette.IL_SUN, FIRE = Palette.IL_FIRE;

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		switch (e.effect()) {
			case "illari_shot" -> shot(at, dir, e.entity());
			case "illari_hit" -> hit(at);
			case "illari_reload" -> reload(e.entity());
			case "illari_cast" -> cast(at);
			case "illari_armed" -> ARMED.put(e.entity(), Fx.now() + 4f);
			case "illari_throw" -> {
				ARMED.remove(e.entity());
				fireBall(at, dir);
			}
			case "illari_burst" -> burst(at);
			case "illari_mark" -> mark(at, e.variant() / 100f);
			case "illari_sunstruck" -> sunstruck(at);
			default -> {
				return false;
			}
		}
		return true;
	}

	// ---- placement ----

	/** Where her rifle's muzzle (hp 0006) is drawn for this player: in her own first person low right of the view (ours). */
	private static Vec3 muzzle(Entity p, float partial) {
		Vec3 eye = p.getEyePosition(partial);
		Vec3 look = p.getViewVector(partial);
		Vec3 right = look.cross(new Vec3(0, 1, 0));
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
		return eye.add(look.scale(0.8)).add(right.scale(0.25)).add(0, -0.22, 0);
	}

	/**
	 * Her hand (hp 000B, the orb at reload and the cast): in her own first person beside the rifle, where her left hand works it
	 * (played 2026-11-17: low left was off the hands), else left low in front of her (ours).
	 */
	private static Vec3 hand(Entity p, float partial) {
		Vec3 eye = p.getEyePosition(partial);
		Vec3 look = p.getViewVector(partial);
		Vec3 right = look.cross(new Vec3(0, 1, 0));
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
		if (ownFirstPerson(p)) {
			// the first-person hands and rifle draw over the world, so beside them, low centre (ours; played 2026-11-17: at the hand it hid)
			return eye.add(look.scale(0.5)).add(right.scale(-0.04)).add(0, -0.17, 0);
		}
		return eye.add(look.scale(0.55)).add(right.scale(-0.18)).add(0, -0.3, 0);
	}

	private static boolean ownFirstPerson(Entity p) {
		Minecraft mc = Minecraft.getInstance();
		return p == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	private static @Nullable Entity entity(int id) {
		var level = Minecraft.getInstance().level;
		return level == null ? null : level.getEntity(id);
	}

	// ---- Captive Sun ----

	/** Players whose sun is armed at the rifle (4EF2), until when (Fx.now() s). */
	private static final java.util.Map<Integer, Float> ARMED = new java.util.HashMap<>();
	/** Reload orbs under way (4F07): whose, from when. */
	private static final java.util.Map<Integer, Float> RELOADS = new java.util.HashMap<>();

	/** Each frame: the held effects (the armed sun at her rifle, the reload's orb in her hand). */
	public static void frame() {
		float now = Fx.now();
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		ARMED.entrySet().removeIf(en -> en.getValue() < now || entity(en.getKey()) == null);
		for (var en : ARMED.entrySet()) {
			Entity p = entity(en.getKey());
			armed(p, muzzle(p, partial), ownFirstPerson(p) ? 0.35f : 1);
		}
		RELOADS.entrySet().removeIf(en -> now - en.getValue() > 1f || entity(en.getKey()) == null);
		for (var en : RELOADS.entrySet()) {
			Entity p = entity(en.getKey());
			reloadOrb(p, hand(p, partial), now - en.getValue(), ownFirstPerson(p) ? 0.5f : 1);
		}
	}

	/**
	 * 4EF2, the sun held at the muzzle (+0.2 m): the 0.18 m orb, the 1.0 m flickering halo, the 0.85 m glow, the 10 m flare
	 * (drawn 2.5 m, faint: ours), embers 64 /s (H). {@code k}: smaller in her own view (ours).
	 */
	private static void armed(Entity p, Vec3 at, float k) {
		String key = "illari_armed/" + p.getId();
		Fx.attach(key + "/orb", at, () -> {
			Particle o = Fx.particle("sojourn_dot", SUN);
			return o == null ? null : o.life(4).size(0.18f * k, 0.18f * k).nearFade(0.1f);
		});
		Fx.attach(key + "/halo", at, () -> {
			Particle h = Fx.soft("illari_halo", SUN);
			return h == null ? null : h.life(4).size(1.0f * k, 1.0f * k).alpha(0.7f).nearFade(0.2f);
		});
		Fx.attach(key + "/glow", at, () -> {
			Particle g = Fx.soft("sojourn_dot", SUN);
			return g == null ? null : g.life(4).size(0.85f * k, 0.85f * k).alpha(0.6f).nearFade(0.2f);
		});
		Fx.attach(key + "/flare", at, () -> {
			Particle f = Fx.soft("glow", SUN);
			return f == null ? null : f.life(4).size(2.5f * k, 2.5f * k).alpha(0.18f).nearFade(0.5f);
		});
		int embers = Math.max(0, Math.round(64 * Fx.since(p.getId() * 31 + 7)));
		for (int i = 0; i < Math.min(embers, 6); i++) {
			Particle e = Fx.particle("illari_ember#4", FIRE);
			if (e != null) {
				Vec3 v = randomDir().scale(0.3 + 0.4 * Fx.RANDOM.nextFloat());
				Fx.add(e.at(at).velocity(v).life(0.4f + 0.3f * Fx.RANDOM.nextFloat()).size(0.02f * k * 2, 0.01f).loop(20)
					.fade(0.05f, 0.5f).nearFade(0.1f));
			}
		}
	}

	/**
	 * 4EF3, the sun forming in her hand as she casts: the 0.08 / 0.15 m orb, the 3.5 m glow at 0.47 s (drawn 2 m: ours), the 1.3-1.5 m
	 * flashes, sparks, then the two sun-ray fans 13561 3.5 m and 5 m (H; drawn as the eight-point sun, ours).
	 */
	private static void cast(Vec3 eye) {
		Vec3 at = eye.add(0, -0.4, 0);
		Particle o = Fx.particle("sojourn_dot", SUN);
		if (o != null) {
			Fx.add(o.at(at).life(1.2f).size(0.08f, 0.15f).fade(0.05f, 0.4f).nearFade(0.3f));
		}
		Particle g = Fx.soft("sojourn_dot", SUN);
		if (g != null) {
			Fx.add(g.at(at).life(1.1f).size(0.6f, 2f).alpha(0.7f).fade(0.3f, 0.5f).nearFade(0.5f));
		}
		Particle rays = Fx.particle("illari_sun_mark", Palette.IL_MARK);
		if (rays != null) {
			Fx.add(rays.at(at).life(0.75f).size(3.5f, 5f).spin(Fx.RANDOM.nextFloat() * 6.28f, 0.6f).alpha(0.8f).fade(0.1f, 0.6f).nearFade(1));
		}
		for (int i = 0; i < 2; i++) {
			Particle s = Fx.particle("illari_star", SUN);
			if (s != null) {
				Fx.add(s.at(at).life(0.3f).size(1.3f + 0.2f * i, 0.5f).spin(Fx.RANDOM.nextFloat() * 6.28f, 0).fade(0.02f, 0.6f).nearFade(0.5f));
			}
		}
		sparks(at, 30, 2, 5, 0.04f, 0.3f);
	}

	/**
	 * 4F06, the throw at the muzzle: the 1 m and 3 m flashes (0.05-0.16 s), rings 0.2-0.28 m along the aim at 0.55 / 0.75 / 1.0 /
	 * 1.5 m, 20 streaks at 4-8 m/s in a 15 degree cone, the flame cone (H).
	 */
	private static void fireBall(Vec3 at, Vec3 aim) {
		Vec3 fwd = aim.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : aim.normalize();
		for (float size : new float[] {1f, 3f}) {
			Particle f = Fx.particle("illari_star", SUN);
			if (f != null) {
				Fx.add(f.at(at).life(0.12f).size(size, size * 0.6f).spin(Fx.RANDOM.nextFloat() * 6.28f, 0).fade(0.01f, 0.6f).nearFade(0.6f));
			}
		}
		for (float d : new float[] {0.55f, 0.75f, 1f, 1.5f}) {
			Particle r = Fx.particle("illari_halo", SUN);
			if (r != null) {
				Fx.add(r.at(at.add(fwd.scale(d))).normal(fwd).life(0.45f).size(0.2f, 0.28f).fade(0.02f, 0.6f).nearFade(0.3f));
			}
		}
		for (int i = 0; i < 20; i++) {
			Particle s = Fx.particle("spark#4", FIRE);
			if (s == null) {
				break;
			}
			Vec3 v = cone(fwd, 0.27).scale(4 + 4 * Fx.RANDOM.nextFloat());
			Fx.add(s.at(at).shape(Fx.Shape.STREAK).velocity(v).life(0.2f + 0.2f * Fx.RANDOM.nextFloat()).size(0.08f, 0.04f).fade(0.02f, 0.5f));
		}
		Particle flame = Fx.particle("illari_flame", FIRE);
		if (flame != null) {
			Fx.add(flame.at(at.add(fwd.scale(0.4))).axis(fwd).life(0.25f).size(0.75f, 1.2f).velocity(fwd.scale(1.5)).fade(0.02f, 0.6f).nearFade(0.5f));
		}
	}

	/**
	 * 4983, the ball in flight (from its renderer each frame): its 8.5 m glow (drawn at 0.25 alpha: ours), the 4.2 m halo, a 0.5 m
	 * hot core, embers 32 /s at 1.5-2.5 m/s (H).
	 */
	public static void ballFlight(int id, Vec3 at) {
		String key = "illari_ball/" + id;
		Fx.attach(key + "/glow", at, () -> {
			Particle g = Fx.soft("glow", SUN);
			return g == null ? null : g.life(12).size(8.5f, 8.5f).alpha(0.25f).nearFade(2);
		});
		Fx.attach(key + "/halo", at, () -> {
			Particle h = Fx.soft("illari_halo", SUN);
			return h == null ? null : h.life(12).size(4.2f, 4.2f).alpha(0.6f).nearFade(1);
		});
		Fx.attach(key + "/core", at, () -> {
			Particle c = Fx.particle("sojourn_dot", SUN);
			return c == null ? null : c.life(12).size(0.5f, 0.5f).nearFade(0.5f);
		});
		int embers = Math.round(32 * Fx.since(id));
		for (int i = 0; i < Math.min(embers, 4); i++) {
			Particle e = Fx.particle("illari_ember#4", FIRE);
			if (e != null) {
				Fx.add(e.at(at).velocity(randomDir().scale(1.5 + Fx.RANDOM.nextFloat())).life(0.5f + 0.3f * Fx.RANDOM.nextFloat())
					.size(0.1f, 0.04f).loop(20).fade(0.05f, 0.5f).nearFade(0.5f));
			}
		}
	}

	/**
	 * 4E27 at scale 1.25 (the 10 m splash): the 16 m shock disc (drawn as the 2.5 m star swelling to 12 m, ours), the 16-21 m flash
	 * (a 10 m glow, 0.6 s: ours), sparks 75-175 at 45-65 m/s, 60 embers rising 2-4 s, the 6 m scorch for 3.9 s (H).
	 */
	private static void burst(Vec3 at) {
		Particle flash = Fx.soft("glow", SUN);
		if (flash != null) {
			Fx.add(flash.at(at).life(0.6f).size(6f, 12f).alpha(0.8f).fade(0.02f, 0.7f).nearFade(2));
		}
		Particle star = Fx.particle("illari_burst_star", SUN);
		if (star != null) {
			Fx.add(star.at(at).life(0.5f).size(2.5f * 1.25f, 12f).spin(Fx.RANDOM.nextFloat() * 6.28f, 0.4f).fade(0.02f, 0.6f).nearFade(2));
		}
		Particle rays = Fx.particle("illari_sun_mark", Palette.IL_MARK);
		if (rays != null) {
			Fx.add(rays.at(at).life(0.9f).size(5f, 16f).spin(Fx.RANDOM.nextFloat() * 6.28f, 0.3f).alpha(0.7f).fade(0.05f, 0.7f).nearFade(2));
		}
		sparks(at, 125, 25, 40, 0.05f, 0.4f);
		for (int i = 0; i < 60; i++) {
			Particle e = Fx.particle("illari_ember#4", FIRE);
			if (e == null) {
				break;
			}
			Vec3 off = randomDir().scale(2.5 * Fx.RANDOM.nextFloat());
			Fx.add(e.at(at.add(off)).velocity(new Vec3(0, 0.6 + 0.6 * Fx.RANDOM.nextFloat(), 0)).life(2 + 2 * Fx.RANDOM.nextFloat())
				.size(0.4f, 0.1f).loop(15).fade(0.1f, 0.6f).nearFade(1));
		}
		scorch(at, 6, 3.9f);
	}

	/**
	 * 4B50, Sunstruck's mark on a creature (each second from the server while it lasts): the eight-point sun 1.5 > 4 m stamped once
	 * (variant 1000), else the 0.2-0.3 m orb and its 1.5-2 m glow at its centre growing with the stored damage (0-1, M), embers (H).
	 */
	private static void mark(Vec3 at, float stored) {
		boolean stamp = stored >= 10;
		float s = Math.clamp(stamp ? 0 : stored, 0, 1);
		if (stamp) {
			Particle sun = Fx.particle("illari_sun_mark", Palette.IL_MARK);
			if (sun != null) {
				Fx.add(sun.at(at).life(1.25f).size(1.5f, 4f).spin(Fx.RANDOM.nextFloat() * 6.28f, 0.5f).alpha(0.8f).fade(0.03f, 0.6f).nearFade(1));
			}
		}
		Particle orb = Fx.particle("sojourn_dot", Palette.IL_MARK);
		if (orb != null) {
			Fx.add(orb.at(at).life(1.05f).size(0.2f + 0.1f * s, 0.2f + 0.1f * s).fade(0.05f, 0.1f).nearFade(0.3f));
		}
		Particle glow = Fx.soft("sojourn_dot", SUN);
		if (glow != null) {
			Fx.add(glow.at(at).life(1.05f).size(1.5f + 0.5f * s, 1.5f + 0.5f * s).alpha(0.6f).fade(0.05f, 0.1f).nearFade(0.5f));
		}
		for (int i = 0; i < 6; i++) {
			Particle e = Fx.particle("illari_ember#4", FIRE);
			if (e != null) {
				Fx.add(e.at(at.add(randomDir().scale(0.3))).velocity(randomDir().scale(0.45)).life(0.8f).size(0.04f, 0.02f).loop(20)
					.fade(0.05f, 0.5f).nearFade(0.3f));
			}
		}
	}

	/**
	 * 4E17, Sunstruck going off: the 0.7-0.85 m hot sphere, the 2.35 m flare and 2.25 m star (0.15-0.35 s), sparks 175-275 at 35-50 m/s,
	 * the rays at 4.75 m, the 12 m shock (drawn as a glow, ours), embers rising (H).
	 */
	private static void sunstruck(Vec3 at) {
		Particle core = Fx.particle("sojourn_dot", SUN);
		if (core != null) {
			Fx.add(core.at(at).life(0.3f).size(0.7f, 0.85f).fade(0.02f, 0.6f).nearFade(0.5f));
		}
		Particle star = Fx.particle("illari_star", SUN);
		if (star != null) {
			Fx.add(star.at(at).life(0.3f).size(2.35f, 2.25f).spin(Fx.RANDOM.nextFloat() * 6.28f, 0).fade(0.02f, 0.6f).nearFade(1));
		}
		Particle rays = Fx.particle("illari_sun_mark", Palette.IL_MARK);
		if (rays != null) {
			Fx.add(rays.at(at).life(0.5f).size(2.5f, 4.75f).spin(Fx.RANDOM.nextFloat() * 6.28f, 0.6f).fade(0.02f, 0.6f).nearFade(1));
		}
		Particle shock = Fx.soft("glow", SUN);
		if (shock != null) {
			Fx.add(shock.at(at).life(0.6f).size(3f, 12f).alpha(0.5f).fade(0.02f, 0.7f).nearFade(1.5f));
		}
		sparks(at, 200, 20, 30, 0.03f, 0.35f);
		for (int i = 0; i < 50; i++) {
			Particle e = Fx.particle("illari_ember#4", FIRE);
			if (e == null) {
				break;
			}
			Fx.add(e.at(at.add(randomDir().scale(1.2 * Fx.RANDOM.nextFloat()))).velocity(new Vec3(0, 0.8 + 0.5 * Fx.RANDOM.nextFloat(), 0))
				.life(2 + Fx.RANDOM.nextFloat()).size(0.25f, 0.06f).loop(15).fade(0.1f, 0.6f).nearFade(0.5f));
		}
		scorch(at, 2, 3f);
	}

	// ---- the reload's sun (4F07) ----

	private static void reload(int id) {
		RELOADS.put(id, Fx.now());
	}

	/**
	 * 4F07 in her hand, {@code t} s in: from 0.4 s the 0.087 m orb (and its 0.023 m core), its swirl 0.15 m, the 0.21 m glow to 0.6 s,
	 * the 0.1-0.2 m ring at 0.13 s, the 0.18-0.2 m star at 0.7 s with the flash of it going into the rifle (H).
	 */
	private static void reloadOrb(Entity p, Vec3 at, float t, float k) {
		String key = "illari_reload/" + p.getId();
		if (t >= 0.4f) {
			Fx.attach(key + "/orb", at, () -> {
				Particle o = Fx.particle("sojourn_dot", SUN);
				return o == null ? null : o.life(0.65f).size(0.087f * k, 0.087f * k).nearFade(0.05f);
			});
			Fx.attach(key + "/swirl", at, () -> {
				Particle s = Fx.soft("illari_halo", SUN);
				return s == null ? null : s.life(0.65f).size(0.17f * k, 0.15f * k).alpha(0.8f).spin(0, 4).nearFade(0.05f);
			});
		}
		if (t >= 0.37f && t < 0.6f) {
			Fx.attach(key + "/glow", at, () -> {
				Particle g = Fx.soft("sojourn_dot", SUN);
				return g == null ? null : g.life(0.3f).size(0.21f * k, 0.21f * k).alpha(0.8f).nearFade(0.05f);
			});
		}
		if (t >= 0.7f && !FLASHED.contains(key)) {
			FLASHED.add(key);
			Particle s = Fx.particle("illari_star", SUN);
			if (s != null) {
				Fx.add(s.at(at).life(0.35f).size(0.25f * k, 0.18f * k).spin(Fx.RANDOM.nextFloat() * 6.28f, 0).fade(0.02f, 0.6f).nearFade(0.05f));
			}
		}
		if (t < 0.1f) {
			FLASHED.remove(key);
		}
	}

	private static final java.util.Set<String> FLASHED = new java.util.HashSet<>();

	// ---- the Solar Rifle ----

	/** 48BE, the shot's band muzzle to hit, 0.25 s, gold, ~0.05 m (width ours, L in the data); 4BA3's star at the muzzle (H). */
	private static void shot(Vec3 from, Vec3 piece, int shooter) {
		float len = (float) piece.length();
		if (len >= 0.5f) {
			Particle band = Fx.particle("illari_shot_line", Palette.IL_SHOT);
			if (band != null) {
				Fx.add(band.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(0.05f / len).life(0.25f).fade(0.01f, 0.7f)
					.nearFade(0.6f));
			}
		}
		Entity p = entity(shooter);
		float k = p != null && ownFirstPerson(p) ? 0.25f : 1;
		Particle star = Fx.particle("illari_star", SUN);
		if (star != null) {
			Vec3 fwd = len < 1.0E-3 ? Vec3.ZERO : piece.scale(1 / len);
			Fx.add(star.at(from.add(fwd.scale(0.2))).life(0.14f).size(0.5f * k, 0.2f * k).spin(Fx.RANDOM.nextFloat() * 6.28f, 0)
				.fade(0.01f, 0.6f).nearFade(0.2f));
		}
	}

	/** 4BA1, the hit (creature and world alike): the 1 m flash 0.2-0.35 s, a 0.45 m glow, 12-18 sparks (H). */
	private static void hit(Vec3 at) {
		Particle f = Fx.particle("illari_hit_glow", Palette.IL_SHOT);
		if (f != null) {
			Fx.add(f.at(at).life(0.25f).size(1f, 0.6f).spin(Fx.RANDOM.nextFloat() * 6.28f, 0).fade(0.01f, 0.6f).nearFade(0.3f));
		}
		Particle g = Fx.soft("glow", SUN);
		if (g != null) {
			Fx.add(g.at(at).life(0.35f).size(0.45f, 0.3f).alpha(0.7f).fade(0.02f, 0.6f).nearFade(0.3f));
		}
		sparks(at, 15, 0.75f, 1f, 0.03f, 0.3f);
	}

	// ---- shared ----

	private static void sparks(Vec3 at, int n, float speedMin, float speedMax, float size, float life) {
		for (int i = 0; i < n; i++) {
			Particle s = Fx.particle("spark#4", FIRE);
			if (s == null) {
				break;
			}
			Vec3 v = randomDir().scale(speedMin + (speedMax - speedMin) * Fx.RANDOM.nextFloat());
			Fx.add(s.at(at).shape(Fx.Shape.STREAK).velocity(v).gravity(4).life(life * (0.6f + 0.8f * Fx.RANDOM.nextFloat()))
				.size(size, size * 0.5f).fade(0.02f, 0.5f));
		}
	}

	/** The scorch 19008 on the ground under {@code at} (when there's ground within 3 m), {@code size} m, for {@code life} s. */
	private static void scorch(Vec3 at, float size, float life) {
		var level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		var hit = level.clip(new net.minecraft.world.level.ClipContext(at, at.add(0, -3, 0), net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
		if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
			return;
		}
		Particle s = Fx.particle("illari_scorch", Palette.IL_SCORCH);
		if (s != null) {
			Vec3 up = new Vec3(0, 1, 0);
			Fx.add(s.at(hit.getLocation()).normal(up).lift(up).life(life).size(size, size).spin(Fx.RANDOM.nextFloat() * 6.28f, 0)
				.alpha(0.8f).fade(0.02f, 0.4f));
		}
	}

	private static Vec3 randomDir() {
		Vec3 v = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return v.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : v.normalize();
	}

	/** A direction within {@code spread} radians of {@code fwd}. */
	private static Vec3 cone(Vec3 fwd, double spread) {
		return fwd.add(randomDir().scale(Math.tan(spread) * Fx.RANDOM.nextFloat())).normalize();
	}
}
