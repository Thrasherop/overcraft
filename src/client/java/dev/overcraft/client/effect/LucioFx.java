package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.net.EffectPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * Lúcio's effects, rebuilt from Overwatch's own (tools/reference/lucio-effects.md: the emitters, sizes, lifetimes,
 * speeds and colour strips read from his effect files). Sizes in metres and times in seconds are the reference's
 * unless marked "ours". Played by EffectPayload from the server (KirikoFx hands his on), by the round's renderer and by
 * his model's (the skates' song trails).
 */
public final class LucioFx {
	/** Whether the effect being made is an enemy hero's (drawn in the enemy red). */
	private static boolean enemy;
	/** When each Lúcio's song last beat (his skates' pulse, once a second), and the song it was. */
	private static final Map<Integer, Float> BEAT = new HashMap<>();
	private static final Map<Integer, Integer> SONG = new HashMap<>();
	private static final Map<Integer, Float> AMP_RINGS = new HashMap<>();

	private LucioFx() {
	}

	private static Palette p(Palette normal) {
		return enemy ? Palette.ENEMY : normal;
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}

	private static Vec3 randomUnit() {
		double z = Fx.RANDOM.nextDouble() * 2 - 1;
		double a = Fx.RANDOM.nextDouble() * Math.PI * 2;
		double s = Math.sqrt(1 - z * z);
		return new Vec3(s * Math.cos(a), z, s * Math.sin(a));
	}

	/** A random direction within halfAngle (radians) of axis. */
	private static Vec3 cone(Vec3 axis, double halfAngle) {
		Vec3 a = axis.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : axis.normalize();
		for (int i = 0; i < 20; i++) {
			Vec3 d = randomUnit();
			if (d.dot(a) >= Math.cos(halfAngle)) {
				return d;
			}
		}
		return a;
	}

	/** His effects by name (from KirikoFx's dispatch); false for one that isn't his. */
	static boolean dispatch(EffectPayload e, boolean enemyOwner) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		enemy = enemyOwner;
		try {
			switch (e.effect()) {
				case "soundwave" -> soundwave(at, dir);
				case "sonic_hit_entity" -> sonicHitEntity(at);
				case "sonic_hit_block" -> sonicHitBlock(at, dir);
				case "sound_barrier_cast" -> barrierCast(at);
				case "sound_barrier" -> barrierSlam(at);
				default -> {
					return false;
				}
			}
			return true;
		} finally {
			enemy = false;
		}
	}

	// ---- Sonic Amplifier ----

	/**
	 * A round in flight (A3F), each frame: a 1.5 m soft glow held on it, thin rings shed backwards 16 a second (r 0.15 m,
	 * 0.4 s, 7 m/s) and 8 a second bigger ones (r 0.21 m, 0.5 s, 3 m/s), and a short trail (its width and length aren't
	 * in the data: ours). False without the pack (nothing to draw it with).
	 */
	public static boolean sonicFlight(int id, Vec3 at, Vec3 velocity, float dt, boolean enemyOwner) {
		enemy = enemyOwner;
		try {
			Palette green = p(Palette.LUCIO_GREEN);
			Particle glow = Fx.attach("sonic_glow/" + id, at, () -> {
				Particle g = Fx.particle("lucio_glow", p(Palette.LUCIO_ROUND));
				// the game's 1.5 m quad is mostly faint falloff (ours: 0.5 m held; at 0.9 m a volley's four stacked to a
				// white ball, seen playing)
				return g == null ? null : g.life(0.2f).size(0.5f, 0.5f).alpha(0.8f).nearFade(2.5f).fade(0.05f, 0.5f);
			});
			if (glow == null) {
				return false;
			}
			Fx.attach("sonic_core/" + id, at, () -> {
				Particle c = Fx.particle("lucio_soft", green);
				return c == null ? null : c.life(0.2f).size(0.2f, 0.2f).nearFade(2.5f).fade(0.05f, 0.5f);
			});
			Vec3 heading = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : velocity.normalize();
			shed(at, heading, dt, 16, 0.3f, 0.4f, 7);
			shed(at, heading, dt, 8, 0.42f, 0.5f, 3);
			Fx.trail(id, at, "lucio_trail", green, 0.12f, 0.03f, 0.8f);
			return true;
		} finally {
			enemy = false;
		}
	}

	private static void shed(Vec3 at, Vec3 heading, float dt, float perSecond, float size, float life, float speed) {
		int n = (int) (perSecond * dt + Fx.RANDOM.nextFloat());
		for (int i = 0; i < n; i++) {
			Particle r = Fx.particle("ring_line", p(Palette.LUCIO_GREEN));
			if (r != null) {
				Fx.add(r.at(at).shape(Shape.FLAT).normal(heading).velocity(heading.scale(-speed)).life(life)
					.size(size, size * 1.3f).nearFade(2.5f).fade(0.05f, 0.7f));
			}
		}
	}

	/** On a target (0838): two rings growing to 0.5 m in 0.15-0.25 s and a 0.5 m flash. */
	static void sonicHitEntity(Vec3 at) {
		for (float[] ring : new float[][] {{0.46f, 0.2f}, {1.0f, 0.25f}}) {
			Particle r = Fx.particle("lucio_pulse#9", p(Palette.LUCIO_GREEN));
			if (r != null) {
				Fx.add(r.at(at).life(ring[1]).size(ring[0] * 0.3f, ring[0]).easeOut().fade(0.02f, 0.5f));
			}
		}
		Particle flash = Fx.particle("lucio_soft", p(Palette.LUCIO_GREEN));
		if (flash != null) {
			Fx.add(flash.at(at).life(0.2f).size(0.5f, 0.6f).fade(0.02f, 0.7f));
		}
	}

	/** On a block or when it runs out (0AE0): 15 sparks at 1.5 m/s and a small puff. */
	static void sonicHitBlock(Vec3 at, Vec3 normal) {
		for (int i = 0; i < 15; i++) {
			Particle s = Fx.particle("lucio_spark", p(Palette.LUCIO_GREEN));
			if (s == null) {
				return;
			}
			Fx.add(s.at(at).velocity(cone(normal, Math.toRadians(80)).scale(1.5)).life(range(0.17f, 0.44f))
				.size(0.11f, 0.05f).spin(range(0, Mth.TWO_PI), range(-6, 6)).fade(0.02f, 0.6f));
		}
		Particle puff = Fx.particle("lucio_soft", p(Palette.LUCIO_GREEN));
		if (puff != null) {
			Fx.add(puff.at(at).velocity(new Vec3(0, range(0.9f, 1.1f), 0)).life(0.175f).size(0.15f, 0.25f).fade(0.02f, 0.7f));
		}
	}

	// ---- Soundwave ----

	/**
	 * The blast (05AD, 0.5 s), from his muzzle along his aim: a 1.4 m pulse disc at 6 m/s, a faint 2.7 m one at 8 m/s,
	 * three rings (0.6-1.2 m) at 2, 4 and 6 m/s, about 30 sparks at 17 m/s (streaks) and a flash at the muzzle. Green
	 * to white, added. The muzzle: ours, ahead of the eye on his right where his gun is.
	 */
	static void soundwave(Vec3 eye, Vec3 look) {
		Vec3 aim = look.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : look.normalize();
		Vec3 right = aim.cross(new Vec3(0, 1, 0));
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
		Vec3 at = eye.add(aim.scale(0.7)).add(right.scale(0.22)).add(0, -0.2, 0);
		Palette green = p(Palette.LUCIO_GREEN);
		Particle pulse = Fx.particle("lucio_pulse#9", green);
		if (pulse == null) {
			return;
		}
		Fx.add(pulse.at(at).shape(Shape.FLAT).normal(aim).velocity(aim.scale(6)).life(0.3f).size(1.4f, 2.6f).easeOut()
			.nearFade(1.5f).fade(0.02f, 0.5f));
		Particle wide = Fx.particle("lucio_soft", green);
		if (wide != null) {
			Fx.add(wide.at(at).shape(Shape.FLAT).normal(aim).velocity(aim.scale(8)).life(0.4f).size(2.7f, 3.2f).alpha(0.35f)
				.nearFade(1.5f).fade(0.05f, 0.6f));
		}
		for (float[] ring : new float[][] {{1.2f, 0.3f, 4}, {0.8f, 0.25f, 2}, {1.6f, 0.35f, 6}}) {
			Particle r = Fx.particle("ring_line", green);
			if (r != null) {
				Fx.add(r.at(at).shape(Shape.FLAT).normal(aim).velocity(aim.scale(ring[2])).life(ring[1])
					.size(ring[0], ring[0] * 2).easeOut().nearFade(1.5f).fade(0.02f, 0.6f));
			}
		}
		for (int i = 0; i < 30; i++) {
			Particle s = Fx.particle("lucio_spark", green);
			if (s == null) {
				break;
			}
			Fx.add(s.at(at).velocity(cone(aim, Math.toRadians(25)).scale(range(17, 19))).shape(Shape.STREAK)
				.life(range(0.3f, 0.4f)).size(0.12f, 0.06f).aspect(0.2f).nearFade(1.5f).fade(0.02f, 0.6f));
		}
		Particle flash = Fx.particle("lucio_soft", green);
		if (flash != null) {
			Fx.add(flash.at(at).life(0.4f).size(0.75f, 0.9f).nearFade(1.2f).fade(0.02f, 0.6f));
		}
	}

	// ---- Crossfade: the song on his skates ----

	/**
	 * His song as it shows on him (CAB heal / CD2 speed, 1 s loops), from his model's renderer each frame: a light
	 * trail from each skate (about 0.4 s of travel, amber for Healing Boost, green for Speed Boost: width ours), a glow
	 * on each skate (0.3 m: ours) and a beat once a second (a 0.15 m spark at each foot, 0.3 s). Swapping songs: a column
	 * round him in the new song's colour (1D2C / 1D2D: a 1 m cylinder, faded over 1 s). Amped (075C, every 0.97 s): two
	 * flat rings at his feet and a burst of 12 sparkles. No ring on the ground: the data has none (H).
	 *
	 * @param song 0 none, 1 heal, 2 speed, +4 amped (HeroManager.SONG)
	 */
	public static void skates(int id, Vec3 left, Vec3 right, int song, float now, boolean enemyOwner) {
		int which = song & 3;
		Integer was = SONG.put(id, which);
		if (which == 0) {
			return;
		}
		enemy = enemyOwner;
		try {
			Palette colour = p(which == 2 ? Palette.SPEED_GREEN : Palette.HEAL_AMBER);
			Vec3[] feet = {left, right};
			for (int side = 0; side < 2; side++) {
				// a trail per skate and song (a trail keeps its colour: the other song's fades out on its own)
				Fx.trail(-(id * 8 + which * 2 + side) - 1, feet[side].add(0, 0.08, 0), "lucio_trail", colour, 0.2f, 0.4f, 0.75f);
				String key = "lucio_skate/" + id + "/" + which + "/" + side;
				Fx.attach(key, feet[side].add(0, 0.1, 0), () -> {
					Particle g = Fx.particle("lucio_soft", colour);
					return g == null ? null : g.life(0.25f).size(0.3f, 0.3f).alpha(0.8f).fade(0.1f, 0.5f);
				});
			}
			float beat = BEAT.getOrDefault(id, -10f);
			if (now - beat >= 1 || now < beat) {
				BEAT.put(id, now);
				for (Vec3 foot : feet) {
					Particle s = Fx.particle("lucio_spark", colour);
					if (s != null) {
						Fx.add(s.at(foot.add(0, 0.1, 0)).life(range(0.3f, 0.35f)).size(0.3f, 0.15f)
							.spin(range(0, Mth.TWO_PI), 0).fade(0.06f, 0.8f));
					}
				}
			}
			if (was != null && was != 0 && was != which) {
				swapColumn(left.add(right).scale(0.5), colour);
			}
			if ((song & 4) != 0) {
				amped(id, left, right, colour, now);
			} else {
				AMP_RINGS.remove(id);
			}
		} finally {
			enemy = false;
		}
	}

	/** The swap's column (1D2C / 1D2D): an open cylinder 0.94 m across and 1 m tall round him, fading over 1 s. */
	private static void swapColumn(Vec3 feet, Palette colour) {
		Particle c = Fx.particle("lucio_band", colour);
		if (c != null) {
			Fx.add(c.at(feet).shape(Shape.ARC).arc(new Quaternionf(), Mth.TWO_PI).life(1).size(0.94f, 1.1f).aspect(1.06f)
				.alpha(0.6f).fade(0.05f, 0.8f));
		}
	}

	/** Amp It Up on him (075C, a 0.97 s loop): rings at his feet (ours: three times the data's r 0.16-0.21 m, to read), sparkles. */
	private static void amped(int id, Vec3 left, Vec3 right, Palette colour, float now) {
		float last = AMP_RINGS.getOrDefault(id, -10f);
		if (now - last < 0.97f && now >= last) {
			return;
		}
		AMP_RINGS.put(id, now);
		Vec3 feet = left.add(right).scale(0.5).add(0, 0.05, 0);
		for (float size : new float[] {0.48f, 0.63f}) {
			Particle r = Fx.particle("ring_line", colour);
			if (r != null) {
				Fx.add(r.at(feet).shape(Shape.FLAT).normal(new Vec3(0, 1, 0)).life(0.45f).size(size * 0.3f, size * 3).easeOut()
					.fade(0.02f, 0.6f));
			}
		}
		for (int i = 0; i < 12; i++) {
			Particle s = Fx.particle("lucio_sparkle", Palette.NATURAL);
			if (s == null) {
				break;
			}
			Fx.add(s.at(feet.add(0, range(0.2f, 1.2f), 0)).velocity(cone(new Vec3(0, 1, 0), Math.toRadians(70)).scale(range(0.7f, 2)))
				.life(range(0.58f, 0.86f)).size(0.1f, 0.06f).spin(range(0, Mth.TWO_PI), range(-4, 4)).fade(0.05f, 0.6f));
		}
		Particle puff = Fx.particle("lucio_soft", colour);
		if (puff != null) {
			Fx.add(puff.at(feet.add(0, 1.2, 0)).life(range(0.62f, 0.83f)).size(0.38f, 0.5f).alpha(0.7f).fade(0.1f, 0.6f));
		}
	}

	// ---- Sound Barrier ----

	/** The cast (11BF): a ground ring flashing out to 8 m in 0.3 s, and bands round him rising and fading (0.7-0.9 s). */
	static void barrierCast(Vec3 feet) {
		Palette green = p(Palette.LUCIO_GREEN);
		Particle ring = Fx.particle("lucio_pulse#9", green);
		if (ring == null) {
			return;
		}
		Fx.add(ring.at(feet.add(0, 0.05, 0)).shape(Shape.FLAT).normal(new Vec3(0, 1, 0)).life(0.3f).size(1, 8).easeOut()
			.fade(0.02f, 0.5f));
		for (float life : new float[] {0.7f, 0.8f, 0.9f}) {
			Particle band = Fx.particle("lucio_band", green);
			if (band != null) {
				Fx.add(band.at(feet).shape(Shape.ARC).arc(new Quaternionf(), Mth.TWO_PI).velocity(new Vec3(0, 1.2, 0)).life(life)
					.size(0.94f, 1.2f).aspect(0.71f).alpha(0.7f).fade(0.05f, 0.7f));
			}
		}
	}

	/** The slam (67CE): a 7 m flash fading in 0.3 s, a gold-white noise burst and a ring across the ground. */
	static void barrierSlam(Vec3 feet) {
		Palette green = p(Palette.LUCIO_GREEN);
		Particle flash = Fx.particle("lucio_soft", green);
		if (flash == null) {
			return;
		}
		Fx.add(flash.at(feet.add(0, 1, 0)).life(0.3f).size(4, 7).easeOut().fade(0.02f, 0.8f));
		Particle burst = Fx.particle("lucio_glow", green);
		if (burst != null) {
			Fx.add(burst.at(feet.add(0, 1, 0)).life(0.34f).size(3, 7).alpha(0.6f).fade(0.02f, 0.7f));
		}
		Particle ring = Fx.particle("lucio_pulse#9", green);
		if (ring != null) {
			Fx.add(ring.at(feet.add(0, 0.05, 0)).shape(Shape.FLAT).normal(new Vec3(0, 1, 0)).life(0.35f).size(2, 9).easeOut()
				.fade(0.02f, 0.5f));
		}
	}

	public static void clear() {
		BEAT.clear();
		SONG.clear();
		AMP_RINGS.clear();
	}
}
