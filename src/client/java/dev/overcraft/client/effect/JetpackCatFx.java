package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.net.EffectPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Jetpack Cat's effects, rebuilt from Overwatch's own (tools/reference/jetpack_cat-effects.md: the emitters, sizes,
 * lifetimes, speeds and colour strips read from her effect files). Sizes in metres and times in seconds are the
 * reference's unless marked "ours". Played by EffectPayload from the server (KirikoFx hands hers on), by her shots'
 * renderer, and each client tick (the rope, the boost trails, Catnapper's target).
 */
public final class JetpackCatFx {
	/** Whether the effect being made is an enemy hero's (drawn in the enemy red and orange). */
	private static boolean enemy;
	/** Each Jetpack Cat towing or carrying someone: her entity id -> {theirs, 1 if she's an enemy's}. */
	private static final Map<Integer, int[]> TETHERS = new HashMap<>();
	/** Each Jetpack Cat's position last tick, for her speed (the boost trails). */
	private static final Map<Integer, Vec3> LAST_AT = new HashMap<>();
	/** Above this speed (m/s) she's boosting: her flight tops out at 12, Frenetic Flight at 17.8 (ours). */
	private static final double BOOST_SEEN = 12.5;
	private static final Vec3 UP = new Vec3(0, 1, 0);
	/**
	 * Each Jetpack Cat's points as last drawn (HeroModelRenderer, from her look's hardpoint table): nozzles 0075, 0077,
	 * 0078, 007A, paws 000A, 000B, each with its down axis, and the game time they were posed at.
	 */
	private record Posed(Vec3[] at, Vec3[] down, long tick) {
	}

	private static final Map<Integer, Posed> POSED = new HashMap<>();
	private static final int HIGH_LEFT = 0, HIGH_RIGHT = 1, LOW_LEFT = 2, LOW_RIGHT = 3;

	/** Her points posed this frame or the last tick, or null (in her own first person: the 1P look has no nozzles). */
	private static @Nullable Posed posed(net.minecraft.world.entity.Entity cat) {
		Posed p = POSED.get(cat.getId());
		return p == null || cat.level().getGameTime() - p.tick > 1 ? null : p;
	}

	private JetpackCatFx() {
	}

	/** Her gold parts' palette, or the enemy view's red-to-white. */
	private static Palette gold(Palette normal) {
		return enemy ? Palette.CAT_ENEMY_HOT : normal;
	}

	/** Her blue parts' palette, or the enemy view's red-to-orange. */
	private static Palette blue(Palette normal) {
		return enemy ? Palette.CAT_ENEMY_ORANGE : normal;
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
		Vec3 a = axis.lengthSqr() < 1.0E-6 ? UP : axis.normalize();
		for (int i = 0; i < 20; i++) {
			Vec3 d = randomUnit();
			if (d.dot(a) >= Math.cos(halfAngle)) {
				return d;
			}
		}
		return a;
	}

	/** Her effects by name (from KirikoFx's dispatch); false for one that isn't hers. */
	static boolean dispatch(EffectPayload e, boolean enemyOwner) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		enemy = enemyOwner;
		try {
			switch (e.effect()) {
				// a paw shot on a creature has no effect of its own (its target slot is empty); the centre shot prints
				// the paw (709F) and flashes for the enemy (7077) or the ally (7075)
				case "paw_heal", "paw_hit" -> {
				}
				case "paw_hit_centre" -> {
					pawPrint(at, dir);
					enemyHit(at, 1);
				}
				case "paw_heal_centre" -> {
					pawPrint(at, dir);
					allyHit(at);
				}
				case "paw_hit_block" -> wallHit(at, dir);
				case "purr" -> purrCast(at);
				case "purr_wave" -> purrPulse(at);
				case "purr_heal" -> allyHit(at);
				case "purr_hit" -> enemyHit(at, 1);
				case "lifeline_attach" -> {
					TETHERS.put(e.variant(), new int[] {e.entity(), enemy ? 1 : 0});
					attach(at);
				}
				case "catnapper_tether" -> {
					TETHERS.put(e.variant(), new int[] {e.entity(), enemy ? 1 : 0});
					tetherStart(at);
				}
				case "tether_end" -> TETHERS.remove(e.variant());
				case "catnapper_cast" -> {
				}
				case "catnapper_land" -> landing(at);
				default -> {
					return false;
				}
			}
			return true;
		} finally {
			enemy = false;
		}
	}

	// ---- Biotic Pawjectiles ----

	/**
	 * A shot in flight (707A paw / 7079 centre), each frame: a gold core (0.157 / 0.34 m) in a blue halo (0.233 / 0.49
	 * m), a gold tube tracer (0.084 / 0.2 m wide, 0.07 / 0.1 s of travel), a fainter blue flat trail (0.15 / 0.35 m,
	 * 0.12 s) and sparks left behind (20 a second, 0.055-0.118 s). Its first frame flares at the paw (728B / 728C). False
	 * without the pack.
	 */
	public static boolean pawFlight(int id, Vec3 at, Vec3 velocity, boolean centre,
		net.minecraft.world.entity.@Nullable Entity owner, boolean enemyOwner, boolean ownView) {
		enemy = enemyOwner;
		try {
			boolean first = Fx.trailHead(id) == null;
			// her own shots in first person fade in over their first 4 m (60 m/s: 0.07 s): at 1 m a centre shot's halo
			// was a bright ball beside her paw (found playing)
			float near = ownView ? 4 : 1.5f;
			float core = centre ? 0.34f : 0.157f;
			float halo = centre ? 0.49f : 0.233f;
			Particle glow = Fx.attach("paw_core/" + id, at, () -> {
				Particle g = Fx.particle("cat_soft", gold(Palette.CAT_CORE));
				return g == null ? null : g.life(0.2f).size(core, core).nearFade(near).fade(0.02f, 0.5f);
			});
			if (glow == null) {
				return false;
			}
			Fx.attach("paw_halo/" + id, at, () -> {
				Particle h = Fx.particle("cat_soft", blue(Palette.CAT_HALO));
				return h == null ? null : h.life(0.2f).size(halo, halo).alpha(0.8f).nearFade(near).fade(0.02f, 0.5f);
			});
			Fx.trail(id, at, "cat_fade", gold(Palette.CAT_TRACER), centre ? 0.2f : 0.084f, centre ? 0.1f : 0.07f, 0.9f);
			Fx.trail(-id - 1, at, "cat_fade", blue(Palette.CAT_RIBBON), centre ? 0.35f : 0.15f, 0.12f, 0.5f);
			Fx.trailNearFade(id, near + 0.5f);
			Fx.trailNearFade(-id - 1, near + 0.5f);
			int sparks = (int) (20 * Fx.since(id) + Fx.RANDOM.nextFloat());
			for (int i = 0; i < sparks; i++) {
				Particle s = Fx.particle("spark#4", blue(Palette.CAT_SPARK));
				if (s != null) {
					// ours: 0.05-0.15 m (the data's spawn size of its blob is 0.15-0.5, a 4-frame flipbook mostly empty)
					Fx.add(s.at(at.add(randomUnit().scale(0.05))).life(range(0.055f, centre ? 0.18f : 0.118f))
						.size(range(0.05f, 0.15f), 0.02f).nearFade(1.5f).fade(0.02f, 0.6f));
				}
			}
			// her own shots start at her eyes in first person, not at the view model's paw: a flare there covered the
			// whole view (found playing)
			if (first && !ownView) {
				muzzle(paw(owner, at), velocity, centre);
			}
			return true;
		} finally {
			enemy = false;
		}
	}

	/**
	 * The paw a shot left from: of her posed paws (hardpoints 000A / 000B, as drawn), the one nearer the shot (the server
	 * swaps paws each volley and doesn't say which); where she isn't drawn, the shot's own first point.
	 */
	private static Vec3 paw(net.minecraft.world.entity.@Nullable Entity owner, Vec3 shot) {
		Posed posed = owner == null ? null : posed(owner);
		if (posed == null) {
			return shot;
		}
		Vec3 a = posed.at[4], b = posed.at[5];
		return a.distanceToSqr(shot) <= b.distanceToSqr(shot) ? a : b;
	}

	/** At the paw (728B / 728C): a 0.8 m star flare for 0.25 s; the centre shot adds a gold and a blue ring along it. */
	private static void muzzle(Vec3 at, Vec3 velocity, boolean centre) {
		Particle flare = Fx.particle("cat_flare", blue(Palette.CAT_HALO));
		if (flare != null) {
			Fx.add(flare.at(at).life(0.25f).size(0.8f, 0.5f).spin(range(0, Mth.TWO_PI), 0).nearFade(1).fade(0.02f, 0.6f));
		}
		if (!centre || velocity.lengthSqr() < 1.0E-6) {
			return;
		}
		Vec3 axis = velocity.normalize();
		float[][] rings = {{1.0f, 0}, {1.3f, 1}};
		for (float[] ring : rings) {
			Particle r = Fx.particle("cat_ring", ring[1] == 0 ? gold(Palette.CAT_CORE) : blue(Palette.CAT_HALO));
			if (r != null) {
				Fx.add(r.at(at.add(axis.scale(0.25))).shape(Shape.FLAT).normal(axis).life(0.3f).size(ring[0] * 0.6f, ring[0])
					.easeOut().nearFade(1).fade(0.02f, 0.6f));
			}
		}
	}

	/**
	 * The centre shot on a creature (709F): a paw print of five quads, a pad and four toes, standing across the shot
	 * (the data's layout scaled up five times, ours, so it reads at Minecraft's distances), a gold core and a blue rim
	 * shrinking out over 0.2 s; flares alternating gold and blue; 3 gold streaks.
	 */
	private static void pawPrint(Vec3 at, Vec3 shot) {
		Vec3 axis = shot.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : shot.normalize();
		Vec3 side = axis.cross(UP).lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : axis.cross(UP).normalize();
		Vec3 up = side.cross(axis).normalize();
		// pad at (0, -0.02), toes at (+-0.02, 0.03) and (+-0.03, 0.01), toes 0.4545 of the pad
		double[][] parts = {{0, -0.02, 1}, {0.02, 0.03, 0.4545}, {-0.02, 0.03, 0.4545}, {0.03, 0.01, 0.4545}, {-0.03, 0.01, 0.4545}};
		for (double[] part : parts) {
			Vec3 where = at.add(side.scale(part[0] * 5)).add(up.scale(part[1] * 5)).subtract(axis.scale(0.05));
			float size = (float) (0.27 * part[2]);
			Particle glow = Fx.particle("cat_soft", gold(Palette.CAT_PAW_PRINT));
			if (glow != null) {
				Fx.add(glow.at(where).shape(Shape.FLAT).normal(axis).life(0.22f).size(size, size * 0.4f).fade(0.01f, 0.7f));
			}
			Particle rim = Fx.particle("cat_soft", blue(Palette.CAT_STAMP_BLUE));
			if (rim != null) {
				Fx.add(rim.at(where).shape(Shape.FLAT).normal(axis).life(0.18f).size(size * 1.7f, size * 0.5f).alpha(0.7f)
					.fade(0.01f, 0.6f));
			}
		}
		for (int i = 0; i < 4; i++) {
			Particle flash = Fx.particle(i % 2 == 0 ? "cat_flare" : "cat_soft",
				i % 2 == 0 ? gold(Palette.CAT_STAMP_GOLD) : blue(Palette.CAT_STAMP_BLUE));
			if (flash != null) {
				Fx.add(flash.at(at).life(0.08f).size(range(0.19f, 0.85f), 0.3f).spin(range(0, Mth.TWO_PI), 0).fade(0.01f, 0.5f));
			}
		}
		for (int i = 0; i < 3; i++) {
			Particle s = Fx.particle("cat_streak", gold(Palette.CAT_PAW_PRINT));
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(randomUnit()).life(0.12f).size(range(0.57f, 0.67f), 0.3f)
					.aspect(0.15f).fade(0.01f, 0.6f));
			}
		}
	}

	/** On an enemy (7077 / Purr's 741C): 5-10 violet streaks flung 10-12 m/s, a 2.7 m blue star flare, soft glows. */
	private static void enemyHit(Vec3 at, float scale) {
		int n = 5 + Fx.RANDOM.nextInt(6);
		for (int i = 0; i < n; i++) {
			Particle s = Fx.particle("cat_streak", blue(Palette.CAT_FLASH_BLUE));
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(9.9f, 11.9f))).drag(0.02f)
					.life(range(0.25f, 0.37f)).size(range(0.12f, 0.18f) * 2, 0.05f).aspect(0.25f).fade(0.01f, 0.6f));
			}
		}
		Particle flare = Fx.particle("cat_flare", blue(Palette.CAT_FLASH_BLUE));
		if (flare != null) {
			Fx.add(flare.at(at).life(range(0.05f, 0.16f)).size(2.7f * scale, 1.5f * scale).spin(range(0, Mth.TWO_PI), 0)
				.nearFade(1.5f).fade(0.01f, 0.6f));
		}
		Particle glow = Fx.particle("cat_soft", blue(Palette.CAT_HALO));
		if (glow != null) {
			Fx.add(glow.at(at).life(range(0.05f, 0.2f)).size(1.5f * scale, 2.4f * scale).alpha(0.5f).nearFade(1.5f).fade(0.01f, 0.7f));
		}
	}

	/** On an ally (7075 / Purr's 741D): gold sparks, a ring opening to 2.4 m, a soft yellow glow. */
	private static void allyHit(Vec3 at) {
		int n = 5 + Fx.RANDOM.nextInt(6);
		for (int i = 0; i < n; i++) {
			Particle s = Fx.particle("cat_streak", gold(Palette.CAT_HEAL_GOLD));
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(1.6f, 4))).drag(0.1f)
					.life(range(0.16f, 0.28f)).size(range(0.12f, 0.18f) * 2, 0.05f).aspect(0.25f).fade(0.01f, 0.6f));
			}
		}
		Particle ring = Fx.particle("cat_ring", blue(Palette.CAT_HALO));
		if (ring != null) {
			Fx.add(ring.at(at).shape(Shape.FLAT).normal(UP).life(range(0.21f, 0.23f)).size(1.5f, 2.4f).easeOut().nearFade(1.5f)
				.fade(0.01f, 0.6f));
		}
		Particle glow = Fx.particle("cat_soft", gold(Palette.CAT_HEAL_GOLD));
		if (glow != null) {
			Fx.add(glow.at(at).life(0.3f).size(0.9f, 1.5f).alpha(0.6f).nearFade(1.5f).fade(0.02f, 0.7f));
		}
	}

	/** On a block (7084): two glows, a 0.25 m flat ring on the face, 4 sparks in a 35-degree cone at 2.5 m/s, a mark. */
	private static void wallHit(Vec3 at, Vec3 normal) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal;
		for (int i = 0; i < 2; i++) {
			Particle glow = Fx.particle("lucio_glow", blue(Palette.CAT_HALO));
			if (glow != null) {
				Fx.add(glow.at(at.add(n.scale(0.05))).life(range(0.07f, 0.16f)).size(range(0.14f, 0.53f), 0.1f).fade(0.01f, 0.6f));
			}
		}
		Particle ring = Fx.particle("cat_ring", blue(Palette.CAT_RIBBON));
		if (ring != null) {
			Fx.add(ring.at(at.add(n.scale(0.02))).shape(Shape.FLAT).normal(n).life(range(0.07f, 0.16f)).size(0.2f, 0.5f).easeOut()
				.fade(0.01f, 0.6f));
		}
		for (int i = 0; i < 4; i++) {
			Particle s = Fx.particle("cat_streak", blue(Palette.CAT_SPARK));
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(cone(n, Math.toRadians(35)).scale(range(2.4f, 2.7f)))
					.life(range(0.14f, 0.17f)).size(range(0.12f, 0.2f), 0.04f).aspect(0.3f).fade(0.01f, 0.6f));
			}
		}
		Particle decal = Fx.particle("cat_soft", gold(Palette.CAT_CORE));
		if (decal != null) {
			// the data's 0.05 m decal read as half-extents: a 0.1 m mark (M)
			Fx.add(decal.at(at.add(n.scale(0.01))).shape(Shape.FLAT).normal(n).life(0.67f).size(0.1f, 0.1f).fade(0.01f, 0.8f));
		}
	}

	// ---- Purr ----

	/**
	 * The cast (70ED): a blue sphere flashing out to the 10 m range and a violet one to 8 m (both centred 1.45 m below
	 * her, 0.45 s), a 2.7 m blue-white sphere on her body for 0.6 s, and 7 short gold streaks round her.
	 */
	private static void purrCast(Vec3 feet) {
		Vec3 centre = feet.subtract(0, 1.45, 0);
		Particle range = Fx.particle("cat_noise", blue(Palette.CAT_PURR_BLUE));
		if (range != null) {
			Fx.add(range.at(centre).shape(Shape.DOME).life(0.45f).size(4, 19.8f).alpha(0.35f).easeOut().fade(0.02f, 0.7f));
		}
		Particle shock = Fx.particle("cat_noise", blue(Palette.CAT_FLASH_BLUE));
		if (shock != null) {
			Fx.add(shock.at(centre).shape(Shape.DOME).life(0.45f).size(3, 16.6f).alpha(0.25f).easeOut().fade(0.02f, 0.7f));
		}
		Particle body = Fx.particle("cat_soft", blue(Palette.CAT_PURR_BLUE));
		if (body != null) {
			Fx.add(body.at(feet.add(0, 0.84, 0)).life(0.6f).size(1.5f, 2.7f).alpha(0.7f).easeOut().nearFade(1.5f).fade(0.02f, 0.7f));
		}
		for (int i = 0; i < 7; i++) {
			Particle line = Fx.particle("cat_purr_streak", gold(Palette.CAT_PURR_GOLD));
			if (line != null) {
				Vec3 out = cone(UP, Math.toRadians(120));
				Fx.add(line.at(feet.add(0, 0.84, 0).add(out.scale(0.6))).shape(Shape.STREAK).velocity(out.scale(0.33))
					.life(range(0.14f, 0.2f)).size(1, 0.8f).aspect(0.2f).fade(0.01f, 0.6f));
			}
		}
	}

	/**
	 * Each wave (70F3): a 1 m gold flash on her body, a gold ring 1.1 m across her middle (0.3 s) and a flat blue ring
	 * sweeping out to 6.4 m (0.33 s; ours: grown from 1 m).
	 */
	private static void purrPulse(Vec3 feet) {
		Vec3 middle = feet.add(0, 1.13, 0);
		Particle flash = Fx.particle("cat_soft", gold(Palette.CAT_PURR_GOLD));
		if (flash != null) {
			Fx.add(flash.at(feet.add(0, 0.6, 0)).life(0.6f).size(1, 0.6f).alpha(0.7f).nearFade(1.5f).fade(0.02f, 0.7f));
		}
		Particle small = Fx.particle("cat_ring", gold(Palette.CAT_PURR_GOLD));
		if (small != null) {
			Fx.add(small.at(middle).shape(Shape.FLAT).normal(UP).life(0.3f).size(1.6f, 2.2f).easeOut().fade(0.01f, 0.6f));
		}
		Particle big = Fx.particle("cat_ring", blue(Palette.CAT_PURR_RING));
		if (big != null) {
			Fx.add(big.at(middle).shape(Shape.FLAT).normal(UP).life(0.33f).size(2, 12.8f).easeOut().fade(0.01f, 0.6f));
		}
	}

	// ---- Lifeline ----

	/** On the ally as the rope latches (7255): 13 blue star sparkles on a circle and a 0.57 m glow. */
	private static void attach(Vec3 feet) {
		Vec3 chest = feet.add(0, 1.1, 0);
		for (int i = 0; i < 13; i++) {
			double a = Mth.TWO_PI * i / 13;
			Particle star = Fx.particle("cat_star", blue(Palette.CAT_TETHER));
			if (star != null) {
				Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
				Fx.add(star.at(chest.add(out.scale(0.4))).velocity(out).life(range(0.17f, 0.27f))
					.size(range(0.12f, 0.24f), 0.05f).fade(0.01f, 0.6f));
			}
		}
		Particle glow = Fx.particle("cat_soft", blue(Palette.CAT_HALO));
		if (glow != null) {
			Fx.add(glow.at(chest).life(0.3f).size(0.57f, 0.8f).nearFade(1.5f).fade(0.01f, 0.7f));
		}
	}

	/**
	 * Each client tick: every rope (Lifeline's 72FA, and Catnapper's, which has no look of its own: drawn the same), and
	 * every Jetpack Cat's boost trails. The rope is straight, as the data's two-point ribbon: a 0.178 m bright cyan core
	 * and a 0.324 m soft blue glow, from her jetpack (ours: 0.3 m behind and up from her centre) to their feet.
	 */
	public static void tethers(net.minecraft.client.multiplayer.ClientLevel level) {
		java.util.Iterator<Map.Entry<Integer, int[]>> it = TETHERS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, int[]> t = it.next();
			net.minecraft.world.entity.Entity cat = level.getEntity(t.getKey());
			net.minecraft.world.entity.Entity towed = level.getEntity(t.getValue()[0]);
			if (cat == null || towed == null || !cat.isAlive() || !towed.isAlive()) {
				it.remove();
				continue;
			}
			enemy = t.getValue()[1] == 1;
			try {
				double yaw = Math.toRadians(cat.getYRot());
				Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(0.3);
				band(t.getKey(), cat.position().add(0, 0.8, 0).add(back), towed.position());
			} finally {
				enemy = false;
			}
		}
		boostTrails(level);
	}

	private static void band(int id, Vec3 from, Vec3 to) {
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len < 1.0E-3f) {
			return;
		}
		Vec3 middle = from.add(piece.scale(0.5));
		Palette palette = enemy ? Palette.CAT_TETHER_ENEMY : Palette.CAT_TETHER;
		String[] masks = {"cat_rope", "cat_soft"};
		float[] widths = {0.178f, 0.324f};
		float[] alphas = {1, 0.5f};
		for (int i = 0; i < masks.length; i++) {
			String mask = masks[i];
			float alpha = alphas[i];
			Particle band = Fx.attach("rope/" + mask + "/" + id, middle, () -> {
				Particle r = Fx.particle(mask, palette);
				return r == null ? null : r.shape(Shape.STREAK).life(0.15f).alpha(alpha).fade(0.02f, 0.5f);
			});
			if (band != null) {
				band.axis(piece).size(len, len).aspect(widths[i] / len);
			}
		}
	}

	/** The rope latching onto the mob she carries (72BE): blue spheres flashing to 4.4 and 6.3 m and a 10 m disc. */
	private static void tetherStart(Vec3 feet) {
		Vec3 chest = feet.add(0, 1, 0);
		for (float size : new float[] {4.4f, 6.3f}) {
			Particle p = Fx.particle("cat_soft", blue(Palette.CAT_PURR_BLUE));
			if (p != null) {
				Fx.add(p.at(chest).life(0.3f).size(size * 0.3f, size).alpha(0.5f).easeOut().nearFade(2).fade(0.01f, 0.7f));
			}
		}
		Particle disc = Fx.particle("cat_noise", blue(Palette.CAT_HALO));
		if (disc != null) {
			Fx.add(disc.at(feet.add(0, 0.05, 0)).shape(Shape.FLAT).normal(UP).life(0.25f).size(4, 10.3f).alpha(0.4f).easeOut()
				.fade(0.01f, 0.7f));
		}
	}

	// ---- Frenetic Flight ----

	/**
	 * Frenetic Flight (7067), for every Jetpack Cat moving faster than her flight can: two cyan ribbon trails from each
	 * side of her jetpack, 0.073 and 0.173 m wide, white at the head, 0.2-0.32 s long, from her low nozzles 0078 / 007A
	 * as posed (where she isn't drawn, as in her own first person: ours, 0.5 m to each side and 0.3 m behind).
	 */
	private static void boostTrails(net.minecraft.client.multiplayer.ClientLevel level) {
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (net.minecraft.world.entity.player.Player player : level.players()) {
			if (!"jetpack_cat".equals(player.getAttached(dev.overcraft.hero.HeroManager.HERO)) || !player.isAlive()) {
				continue;
			}
			int id = player.getId();
			seen.add(id);
			Vec3 now = player.position();
			Vec3 before = LAST_AT.put(id, now);
			if (before == null || now.distanceTo(before) * 20 < BOOST_SEEN) {
				continue;
			}
			enemy = KirikoFx.enemyOf(player);
			try {
				double yaw = Math.toRadians(player.getYRot());
				Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
				Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
				Palette palette = enemy ? Palette.CAT_BOOST_ENEMY : Palette.CAT_BOOST;
				Posed posed = posed(player);
				for (int s = -1; s <= 1; s += 2) {
					Vec3 nozzle = posed != null ? posed.at[s < 0 ? LOW_LEFT : LOW_RIGHT]
						: now.add(0, 0.7, 0).add(side.scale(0.5 * s)).add(back.scale(0.3));
					int trail = 1_000_000 + id * 4 + (s < 0 ? 0 : 2);
					Fx.trail(trail, nozzle, "cat_trail", palette, 0.073f, 0.2f, 0.9f);
					Fx.trail(trail + 1, nozzle, "cat_trail", palette, 0.173f, 0.32f, 0.6f);
				}
			} finally {
				enemy = false;
			}
		}
		LAST_AT.keySet().retainAll(seen);
	}

	/**
	 * Her jetpack, every frame she's drawn (HeroModelRenderer hands in her points as posed): the idle flames (7064, "always
	 * while flying") on the high nozzles 0075 / 0077. Each is the data's open tube (0.13 m wide, 0.17 m long) in three
	 * layers: a blue one and a gold one at 1.1 times, a white-hot core at 0.55, flickering; a 0.08 m glow on each low nozzle
	 * (0078 / 007A); gold sparks falling from 0.13 m under the nozzle (the data's 50 a second of 0.011 m dots drawn as 6 a
	 * second of 0.03 m: ours, so they show at Minecraft's distances); an exhaust ring (0.2 m) 4 times a second drifting
	 * down. Her heat haze isn't drawn. Out of hero mode she walks: no flames.
	 */
	public static void jetpack(net.minecraft.world.entity.LivingEntity cat, Vec3[] at, Vec3[] down) {
		POSED.put(cat.getId(), new Posed(at, down, cat.level().getGameTime()));
		if (Boolean.FALSE.equals(cat.getAttached(dev.overcraft.hero.HeroManager.HERO_MODE))) {
			return;
		}
		enemy = KirikoFx.enemyOf(cat);
		try {
			int id = cat.getId();
			float dt = Fx.since(-(id * 8 + 7) - 1);
			float t = Fx.now();
			for (int n = HIGH_LEFT; n <= HIGH_RIGHT; n++) {
				Vec3 nozzle = at[n];
				Vec3 axis = down[n];
				float flicker = 0.85f + 0.15f * Mth.sin(t * 37 + n * 2.1f);
				flame(id, n, 0, nozzle, axis, 0.143f, 0.187f * flicker, "cat_fade", blue(Palette.CAT_FLAME), 0.55f);
				flame(id, n, 1, nozzle, axis, 0.143f, 0.187f * (2 - flicker), "cat_fade", gold(Palette.CAT_FLAME_GOLD), 0.7f);
				flame(id, n, 2, nozzle, axis, 0.0715f, 0.094f, "cat_fade", gold(Palette.CAT_FLAME), flicker);
				Vec3 under = nozzle.add(axis.scale(0.13));
				int sparks = (int) (6 * dt + Fx.RANDOM.nextFloat());
				for (int i = 0; i < sparks; i++) {
					Particle spark = Fx.particle("cat_soft", gold(Palette.CAT_FLAME_GOLD));
					if (spark != null) {
						Fx.add(spark.at(under).velocity(axis.scale(1).add(randomUnit().scale(0.3))).life(range(0.07f, 0.25f))
							.size(0.03f, 0.01f).nearFade(0.5f).fade(0.02f, 0.6f));
					}
				}
				if (Fx.RANDOM.nextFloat() < 4 * dt) {
					Particle ring = Fx.particle("cat_ring", blue(Palette.CAT_FLAME));
					if (ring != null) {
						Fx.add(ring.at(under).shape(Shape.FLAT).normal(axis).velocity(axis.scale(0.12)).life(0.35f)
							.size(0.2f, 0.4f).alpha(0.5f).easeOut().nearFade(0.5f).fade(0.05f, 0.6f));
					}
				}
			}
			for (int n = LOW_LEFT; n <= LOW_RIGHT; n++) {
				Vec3 glowAt = at[n];
				Fx.attach("jetpack_glow/" + id + "/" + n, glowAt, () -> {
					Particle g = Fx.particle("cat_soft", blue(Palette.CAT_FLAME));
					return g == null ? null : g.life(0.1f).size(0.08f, 0.08f).alpha(0.8f).nearFade(0.5f).fade(0.02f, 0.5f);
				});
			}
		} finally {
			enemy = false;
		}
	}

	/** One layer of a nozzle's flame: a streak from the nozzle along its down axis, kept on it frame by frame. */
	private static void flame(int id, int nozzle, int layer, Vec3 from, Vec3 axis, float width, float length, String mask,
		Palette palette, float alpha) {
		Particle p = Fx.attach("jetpack_flame/" + id + "/" + nozzle + "/" + layer, from.add(axis.scale(length / 2)), () -> {
			Particle f = Fx.particle(mask, palette);
			return f == null ? null : f.shape(Shape.STREAK).life(0.1f).fade(0.02f, 0.5f).nearFade(0.5f);
		});
		if (p != null) {
			p.axis(axis).size(length, length).aspect(width / length).alpha(alpha);
		}
	}

	// ---- Catnapper ----

	/**
	 * Catnapper's aim, each tick while she aims (71B3 at the target point, shown to her): cyan rings about 3-4 m across
	 * pulsing out 3 times a second, a blue glow disc under them, and two thin cyan lines from her paws to it (71AF).
	 */
	public static void aim(net.minecraft.client.Minecraft mc) {
		if (mc.player == null || dev.overcraft.client.ClientHero.hero() != dev.overcraft.hero.Hero.JETPACK_CAT
			|| (dev.overcraft.client.ClientHero.sync().mode() & 7) != dev.overcraft.jetpackcat.JetpackCat.ULT_AIM) {
			return;
		}
		Vec3 at = dev.overcraft.jetpackcat.JetpackCat.diveTarget(mc.player);
		if (at == null) {
			return;
		}
		Vec3 ground = at.add(0, 0.06, 0);
		if (Fx.RANDOM.nextFloat() < 3 / 20f) {
			Particle ring = Fx.particle("cat_ring", Palette.CAT_NAP_MARKER);
			if (ring != null) {
				Fx.add(ring.at(ground).shape(Shape.FLAT).normal(UP).life(1.5f).size(2.4f, 4.1f).easeOut().fade(0.1f, 0.6f));
			}
		}
		Fx.attach("catnapper_aim_disc", ground, () -> {
			Particle d = Fx.particle("cat_soft", Palette.CAT_HALO);
			return d == null ? null : d.shape(Shape.FLAT).normal(UP).life(0.3f).size(2, 2).alpha(0.6f).fade(0.05f, 0.5f);
		});
		// the paws (hardpoints 000A / 000B): a little to each side of her below the eyes (ours, as her volley's)
		net.minecraft.world.entity.player.Player cat = mc.player;
		double yaw = Math.toRadians(cat.getYRot());
		Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		for (int s = -1; s <= 1; s += 2) {
			Vec3 paw = cat.getEyePosition().add(side.scale(0.25 * s)).add(0, -0.35, 0);
			Vec3 piece = ground.subtract(paw);
			float len = (float) piece.length();
			if (len < 0.1f) {
				continue;
			}
			Particle line = Fx.attach("catnapper_line/" + s, paw.add(piece.scale(0.5)), () -> {
				Particle l = Fx.particle("cat_rope", Palette.CAT_TETHER);
				return l == null ? null : l.shape(Shape.STREAK).life(0.15f).alpha(0.8f).fade(0.02f, 0.5f);
			});
			if (line != null) {
				line.axis(piece).size(len, len).aspect(0.045f / len);
			}
		}
	}

	/**
	 * The landing (71C2): a 5.9 m violet-cyan flash, a ground flash, a 1.9 m sphere, 15 smoke puffs thrown up 12-19 m/s,
	 * and the crater's glowing cracks, 10 m across, fading over 2.5 s.
	 */
	private static void landing(Vec3 feet) {
		Particle flash = Fx.particle("cat_soft", gold(Palette.CAT_NAP_IMPACT));
		if (flash != null) {
			Fx.add(flash.at(feet.add(0, 1, 0)).life(0.25f).size(3, 5.9f).easeOut().nearFade(2).fade(0.01f, 0.7f));
		}
		Particle ground = Fx.particle("cat_soft", blue(Palette.CAT_HALO));
		if (ground != null) {
			// the data's 21.7 m ground flash, kept to 12 m (ours: at full size it would light the whole view in first person)
			Fx.add(ground.at(feet.add(0, 0.05, 0)).shape(Shape.FLAT).normal(UP).life(0.25f).size(6, 12).alpha(0.6f).easeOut()
				.fade(0.01f, 0.7f));
		}
		Particle sphere = Fx.particle("cat_soft", blue(Palette.CAT_PURR_BLUE));
		if (sphere != null) {
			Fx.add(sphere.at(feet.add(0, 0.6, 0)).life(0.3f).size(1, 1.9f).nearFade(2).fade(0.01f, 0.7f));
		}
		for (int i = 0; i < 15; i++) {
			Particle puff = Fx.soft("smoke#48", Palette.NATURAL);
			if (puff != null) {
				Fx.add(puff.at(feet.add(0, 0.3, 0)).velocity(cone(UP, Math.toRadians(45)).scale(range(11.6f, 18.9f))).drag(0.02f)
					.gravity(9.8f).life(range(0.62f, 1.3f)).size(range(0.61f, 1.12f), range(1.2f, 2)).alpha(0.7f).fade(0.05f, 0.7f));
			}
		}
		// the crater's dark cracks (CBB9) darken the ground (a multiplied decal: white where there's no crack), which these
		// additive and blended particles can't do: drawn as a 10 m square of white (found playing), so left out; its
		// glowing edges (CA10) stay
		Particle glowCrack = Fx.particle("cat_crack_glow", blue(Palette.CAT_HALO));
		if (glowCrack != null) {
			Fx.add(glowCrack.at(feet.add(0, 0.04, 0)).shape(Shape.FLAT).normal(UP).life(2.47f).size(10, 10).fade(0.01f, 0.5f));
		}
	}
}
