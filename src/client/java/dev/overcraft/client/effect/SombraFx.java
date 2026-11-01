package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.entity.CyberspaceEntity;
import dev.overcraft.entity.TranslocatorEntity;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Sombra's effects (tools/reference/sombra-effects.md, read from her effects' data; each moment's numbers there). Her side
 * sees the purple rows and her heals' gold, her enemies the red-orange twins (M which team sees which). The server sends her
 * moments (EffectPayload "sombra_*"); her projectiles' trails and the states she puts on others (hacked, Hotfix's heal,
 * weakened, the zone) are drawn each client tick from what it sent, until their time is up. Her effect meshes (the circuit
 * sphere, the code panels) are drawn with the sprite shapes the mod has (domes, arcs, flat discs): ours.
 */
public final class SombraFx {
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** A state on an entity: since when, until when (Fx.now() seconds), and whether its giver is an enemy of ours. */
	private record Held(float since, float until, boolean enemy) {
	}

	/** Hotfix's strands: from her (entity id) to the target, since when, to a hacked object or not. */
	private record Strands(int sombra, float since, boolean hack, boolean enemy) {
	}

	/** A Cyberspace zone: where, since when, until when. */
	private record Zone(Vec3 at, float since, float until, boolean enemy) {
	}

	private static final Map<Integer, Held> HACKED = new HashMap<>();
	private static final Map<Integer, Held> REVEALED = new HashMap<>();
	private static final Map<Integer, Held> HOTFIXED = new HashMap<>();
	private static final Map<Integer, Held> WEAKENED = new HashMap<>();
	private static final Map<Integer, Held> HACKED_THINGS = new HashMap<>();
	private static final Map<Integer, Strands> STRANDS = new HashMap<>();
	private static final java.util.List<Zone> ZONES = new java.util.ArrayList<>();

	/** Hotfix's strands start at 0, 0.133, 0.267 and 0.4 s and each lands at the target (7C6E..7D41). */
	private static final float STRAND_TIME = 0.53f;

	private SombraFx() {
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}

	private static Vec3 randomUnit() {
		Vec3 v = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return v.lengthSqr() < 1.0E-6 ? UP : v.normalize();
	}

	private static Palette purple(boolean enemy) {
		return enemy ? Palette.SOMBRA_ENEMY_RED : Palette.SOMBRA_PURPLE;
	}

	private static Palette gold(boolean enemy) {
		return enemy ? Palette.SOMBRA_HOTFIX_HEAL_ENEMY_N : Palette.SOMBRA_HEAL_GOLD;
	}

	private static Vec3 chest(Entity e) {
		return e.position().add(0, e.getBbHeight() * 0.6, 0);
	}

	// ---- each client tick ----

	/** Her projectiles' trails and the states on others. */
	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			return;
		}
		float now = Fx.now();
		for (Entity e : mc.level.entitiesForRendering()) {
			boolean enemy = e instanceof net.minecraft.world.entity.projectile.Projectile p && KirikoFx.enemyOf(p.getOwner());
			if (e instanceof CyberspaceEntity) {
				cyberFlight(e, enemy);
			} else if (e instanceof TranslocatorEntity) {
				// the beacon's ribbons: 0.25 and 0.5 m wide, 0.3 s, violet (1B0F)
				Fx.trail(e.getId(), e.position(), "lucio_trail", enemy ? Palette.SOMBRA_BEACON_TRAIL_ENEMY_N : Palette.SOMBRA_BEACON_TRAIL_N,
					0.25f, 0.3f, 0.9f);
				Fx.trail(e.getId() + 1_000_000, e.position(), "lucio_trail", enemy ? Palette.SOMBRA_BEACON_TRAIL_ENEMY_N
					: Palette.SOMBRA_BEACON_TRAIL_N, 0.5f, 0.3f, 0.4f);
			}
		}
		held(mc, HACKED, now, SombraFx::hacked);
		held(mc, REVEALED, now, SombraFx::revealed);
		held(mc, HOTFIXED, now, SombraFx::hotfixed);
		held(mc, WEAKENED, now, SombraFx::weakened);
		heldThings(mc, now);
		strands(mc, now);
		zones(now);
	}

	private interface Draw {
		void draw(Entity entity, Held held, float now);
	}

	private static void held(Minecraft mc, Map<Integer, Held> states, float now, Draw draw) {
		for (Iterator<Map.Entry<Integer, Held>> it = states.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Held> entry = it.next();
			Entity e = mc.level.getEntity(entry.getKey());
			if (!(e instanceof LivingEntity living) || !living.isAlive() || now >= entry.getValue().until) {
				it.remove();
				continue;
			}
			if (!ownFirstPerson(living)) {
				draw.draw(living, entry.getValue(), now);
			}
		}
	}

	/** The local player in first person: their own states aren't drawn round their eyes. */
	private static boolean ownFirstPerson(Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/**
	 * Hacked (1704, 1B8E): a purple skull on the chest growing x0.82 to x2.86 of 0.62 m over its 0.75 s, 10 a second, and
	 * curved code panels 1.1-1.4 m wide rising at the feet, 3 a second, each 0.675 s; a skull flash over the head twice a
	 * second.
	 */
	private static void hacked(Entity e, Held h, float now) {
		if (Fx.RANDOM.nextFloat() < 10f / 20) {
			Particle skull = Fx.particle("sombra_skull", h.enemy ? Palette.SOMBRA_ENEMY_RED : Palette.SOMBRA_SKULL);
			if (skull != null) {
				Fx.add(skull.at(chest(e)).life(0.75f).size(0.62f * 0.82f, 0.62f * 2.86f).alpha(0.35f).fade(0.05f, 0.4f).nearFade(0.6f));
			}
		}
		if (Fx.RANDOM.nextFloat() < 3f / 20) {
			Particle panel = Fx.particle("sombra_code_wall", purple(h.enemy));
			if (panel != null) {
				float w = range(1.1f, 1.4f);
				Fx.add(panel.at(e.position().add(0, 0.1, 0)).arc(new Quaternionf(), 1.2f).spin(range(0, 6.28f), 0).velocity(new Vec3(0, 1.2, 0))
					.life(0.675f).size(w, w).aspect(0.35f).alpha(0.7f).fade(0.1f, 0.5f).nearFade(0.6f));
			}
		}
		flash(e, h.enemy, e.getBbHeight() * 0.6f + 0.855f);
	}

	/** Revealed (1B2F): the 0.52 m skull flash at the chest + 0.4 m, twice a second. */
	private static void revealed(Entity e, Held h, float now) {
		if (!HACKED.containsKey(e.getId())) {
			flash(e, h.enemy, e.getBbHeight() * 0.6f + 0.4f);
		}
	}

	private static void flash(Entity e, boolean enemy, float up) {
		if (Fx.RANDOM.nextFloat() < 2f / 20) {
			Particle skull = Fx.particle("sombra_skull_outline", enemy ? Palette.SOMBRA_ENEMY_RED : Palette.SOMBRA_SKULL);
			if (skull != null) {
				Fx.add(skull.at(e.position().add(0, up, 0)).life(0.4f).size(0.52f, 0.52f).fade(0.05f, 0.5f).nearFade(0.6f));
			}
		}
	}

	/**
	 * Hotfix healing them (6570): a gold skull icon over the head + 0.5 m, 10 a second, each 0.75 s growing x0.82 to x2.86 of
	 * 0.3 m; gold streaks rising 3 a second; a body glow.
	 */
	private static void hotfixed(Entity e, Held h, float now) {
		if (Fx.RANDOM.nextFloat() < 10f / 20) {
			Particle skull = Fx.particle("sombra_skull_heal", gold(h.enemy));
			if (skull != null) {
				Fx.add(skull.at(e.position().add(0, e.getBbHeight() + 0.5, 0)).life(0.75f).size(0.3f * 0.82f, 0.3f * 2.86f).alpha(0.3f)
					.fade(0.05f, 0.4f).nearFade(0.6f));
			}
		}
		if (Fx.RANDOM.nextFloat() < 3f / 20) {
			Particle streak = Fx.particle("sombra_streak_v", gold(h.enemy));
			if (streak != null) {
				Vec3 side = randomUnit().multiply(1, 0, 1).normalize().scale(e.getBbWidth() * 0.6);
				Fx.add(streak.at(e.position().add(side.x, range(0, e.getBbHeight()), side.z)).shape(Shape.STREAK)
					.velocity(new Vec3(0, range(1.2f, 2), 0)).life(0.5f).size(0.4f, 0.3f).aspect(0.1f).fade(0.05f, 0.5f).nearFade(0.6f));
			}
		}
		Particle glow = Fx.attach("sombra_hotfixed/" + e.getId(), chest(e), () -> {
			Particle p = Fx.particle("mercy_glow", gold(h.enemy));
			return p == null ? null : p.life(0.3f).alpha(0.25f).fade(0.1f, 0.5f).nearFade(0.8f);
		});
		if (glow != null) {
			glow.size(e.getBbHeight() * 1.4f, e.getBbHeight() * 1.4f).at(chest(e));
		}
	}

	/**
	 * Weakened in the zone (7AE1): arrows pointing down at the chest - 0.15 m, 2 a second, violet to pink; pink chevrons
	 * falling 8 a second; a ring rising round them about once a second.
	 */
	private static void weakened(Entity e, Held h, float now) {
		Palette arrows = h.enemy ? Palette.SOMBRA_WEAKENED_ALT_N : Palette.SOMBRA_WEAKENED_N;
		if (Fx.RANDOM.nextFloat() < 2f / 20) {
			Particle arrow = Fx.particle("sombra_arrow", arrows);
			if (arrow != null) {
				Fx.add(arrow.at(chest(e).add(0, -0.15, 0)).velocity(new Vec3(0, -0.4, 0)).life(0.5f).size(0.35f, 0.3f).fade(0.05f, 0.5f)
					.nearFade(0.6f));
			}
		}
		if (Fx.RANDOM.nextFloat() < 8f / 20) {
			Particle chevron = Fx.particle("sombra_chevron_glow", Palette.SOMBRA_WEAKENED_CHEVRON);
			if (chevron != null) {
				Vec3 side = randomUnit().multiply(1, 0, 1).normalize().scale(e.getBbWidth() * 0.6);
				Fx.add(chevron.at(e.position().add(side.x, range(0.3f, e.getBbHeight()), side.z)).shape(Shape.STREAK)
					.velocity(new Vec3(0, -1, 0)).life(0.4f).size(0.07f * 3, 0.07f).aspect(0.33f).fade(0.05f, 0.5f).nearFade(0.5f));
			}
		}
		if (Fx.RANDOM.nextFloat() < 1.25f / 20) {
			Particle ring = Fx.particle("ana_ring", arrows);
			if (ring != null) {
				Fx.add(ring.at(e.position().add(0, 0.1, 0)).normal(UP).velocity(new Vec3(0, 1.2, 0)).life(0.8f)
					.size(e.getBbWidth() * 1.6f, e.getBbWidth() * 1.8f).alpha(0.6f).fade(0.05f, 0.5f).nearFade(0.6f));
			}
		}
	}

	/** A hacked deployable (7D35): purple rings 1.28 m twice a second, code panels 3 a second. */
	private static void heldThings(Minecraft mc, float now) {
		for (Iterator<Map.Entry<Integer, Held>> it = HACKED_THINGS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Held> entry = it.next();
			Entity e = mc.level.getEntity(entry.getKey());
			if (e == null || e.isRemoved() || now >= entry.getValue().until) {
				it.remove();
				continue;
			}
			boolean enemy = entry.getValue().enemy;
			if (Fx.RANDOM.nextFloat() < 2f / 20) {
				Particle ring = Fx.particle("ana_ring", purple(enemy));
				if (ring != null) {
					Fx.add(ring.at(e.position().add(0, 0.1, 0)).normal(UP).life(0.6f).size(0.6f, 1.28f).fade(0.05f, 0.5f).nearFade(0.6f));
				}
			}
			if (Fx.RANDOM.nextFloat() < 3f / 20) {
				Particle panel = Fx.particle("sombra_code_wall", purple(enemy));
				if (panel != null) {
					float w = range(1.8f, 2.7f);
					Fx.add(panel.at(e.position()).arc(new Quaternionf(), 1.2f).spin(range(0, 6.28f), 0).velocity(new Vec3(0, 1, 0)).life(0.675f)
						.size(w, w).aspect(0.25f).alpha(0.6f).fade(0.1f, 0.5f).nearFade(0.6f));
				}
			}
		}
	}

	/**
	 * Hotfix's strands (to an ally 7C6E-7D41 in gold, to a hacked object 1AC7-1B32 in purple): four, starting 0.133 s apart,
	 * each a 0.07 m pixel ribbon over a 0.133 m band from her left forearm to their chest; where each lands, a glow, sparks
	 * and pixel bars.
	 */
	private static void strands(Minecraft mc, float now) {
		for (Iterator<Map.Entry<Integer, Strands>> it = STRANDS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Strands> entry = it.next();
			Strands s = entry.getValue();
			Entity target = mc.level.getEntity(entry.getKey());
			Entity sombra = mc.level.getEntity(s.sombra);
			float t = now - s.since;
			if (target == null || sombra == null || t > STRAND_TIME + 0.2f) {
				it.remove();
				continue;
			}
			Vec3 from = sombra.getEyePosition().add(sombra.getViewVector(1).scale(0.45)).add(0, -0.25, 0);
			Vec3 to = chest(target);
			Palette band = s.hack ? (s.enemy ? Palette.SOMBRA_HACK_STRAND_ENEMY_N : Palette.SOMBRA_HACK_STRAND_N)
				: (s.enemy ? Palette.SOMBRA_HOTFIX_STRAND_ENEMY_N : Palette.SOMBRA_HOTFIX_STRAND_N);
			for (int i = 0; i < 4; i++) {
				float start = i * 0.133f;
				float into = (t - start) / 0.13f;
				if (into < 0 || into > 1.5f) {
					continue;
				}
				// each strand reaches out over 0.13 s, bowed a little to its own side
				Vec3 tip = from.lerp(to, Math.min(1, into));
				Vec3 piece = tip.subtract(from);
				float len = (float) piece.length();
				if (len < 0.1f) {
					continue;
				}
				Vec3 bow = piece.cross(UP).normalize().scale((i - 1.5) * 0.05);
				Particle ribbon = Fx.particle(i % 2 == 0 ? "sombra_ribbon_pixels" : "mercy_band", band);
				if (ribbon != null) {
					float w = i % 2 == 0 ? 0.07f : 0.133f;
					Fx.add(ribbon.at(from.add(piece.scale(0.5)).add(bow)).axis(piece).size(len, len).aspect(w / len).life(0.06f)
						.fade(0.01f, 0.5f).nearFade(0.4f));
				}
				if (into >= 1 && into < 1.08f) {
					land(to, s.hack ? purple(s.enemy) : gold(s.enemy));
				}
			}
		}
	}

	private static void land(Vec3 at, Palette palette) {
		Particle glow = Fx.particle("mercy_glow", palette);
		if (glow != null) {
			Fx.add(glow.at(at).life(0.3f).size(0.8f, 0.6f).alpha(0.5f).fade(0.02f, 0.6f).nearFade(0.6f));
		}
		for (int i = 0; i < 4; i++) {
			Particle bar = Fx.particle("sombra_pixel_bars#13", palette);
			if (bar != null) {
				Fx.add(bar.at(at.add(randomUnit().scale(0.25))).velocity(randomUnit().scale(1.5)).life(0.35f).size(range(0.16f, 0.29f), 0.1f)
					.loop(30).fade(0.02f, 0.5f).nearFade(0.4f));
			}
		}
	}

	/** In flight (502E): three spinning violet cubes 0.34-0.43 m and a 0.75 m violet-pink ribbon living 0.5 s. */
	private static void cyberFlight(Entity e, boolean enemy) {
		Fx.trail(e.getId(), e.position(), "sombra_trail_band", enemy ? Palette.SOMBRA_CYBER_TRAIL_ENEMY_N : Palette.SOMBRA_CYBER_TRAIL_N,
			0.75f, 0.5f, 0.8f);
		for (int i = 0; i < 3; i++) {
			int n = i;
			Particle cube = Fx.attach("sombra_cube/" + e.getId() + "/" + i, e.position(), () -> {
				Particle p = Fx.particle("sombra_cube_glow", enemy ? Palette.SOMBRA_CYBER_CUBE_ENEMY : Palette.SOMBRA_CYBER_CUBE);
				return p == null ? null : p.life(0.15f).size(new float[] {0.43f, 0.35f, 0.34f}[n], new float[] {0.43f, 0.35f, 0.34f}[n])
					.spin(n * 2.1f, 6 + n).fade(0.02f, 0.5f).nearFade(0.5f);
			});
			if (cube != null) {
				cube.at(e.position());
			}
		}
		if (Fx.RANDOM.nextFloat() < 0.5f) {
			Particle spark = Fx.particle("sombra_soft_square", enemy ? Palette.SOMBRA_CYBER_BURST_ENEMY_N : Palette.SOMBRA_CYBER_BURST_N);
			if (spark != null) {
				Fx.add(spark.at(e.position().add(randomUnit().scale(0.2))).velocity(randomUnit().scale(0.5)).life(0.4f).size(0.08f, 0.04f)
					.fade(0.02f, 0.5f).nearFade(0.4f));
			}
		}
	}

	/**
	 * The zone (7C7E, 3 s): a translucent purple sphere 11.8 m across (a dome over a flat disc here), a gold circuit dome 9 m,
	 * gold code rain falling 4-8 m/s (13-18 a second), gold pluses rising (3-7 a second) and a spinning 5.75 m ground disc.
	 */
	private static void zones(float now) {
		ZONES.removeIf(z -> now >= z.until);
		for (Zone z : ZONES) {
			Palette sphere = z.enemy ? Palette.SOMBRA_CYBER_SPHERE_ENEMY : Palette.SOMBRA_CYBER_SPHERE;
			Particle dome = Fx.attach("sombra_zone/" + z.since, z.at, () -> {
				Particle p = Fx.particle("mercy_glow", sphere);
				return p == null ? null : p.shape(Shape.DOME).life(z.until - now).size(11.8f, 11.8f).alpha(0.18f).fade(0.05f, 0.9f).nearFade(2);
			});
			if (dome != null) {
				dome.at(z.at);
			}
			Particle disc = Fx.attach("sombra_zone_disc/" + z.since, z.at, () -> {
				Particle p = Fx.particle("sombra_circuit_disc", z.enemy ? Palette.SOMBRA_CYBER_CIRCUIT_ENEMY_N : Palette.SOMBRA_CYBER_DECAL_N);
				return p == null ? null : p.normal(UP).lift(UP).life(z.until - now).size(5.75f, 5.75f).spin(0, 0.6f).alpha(0.8f)
					.fade(0.05f, 0.9f);
			});
			if (disc != null) {
				disc.at(z.at.add(0, 0.05, 0));
			}
			Palette goldRow = z.enemy ? Palette.SOMBRA_CYBER_ORANGE_N : Palette.SOMBRA_CYBER_GOLD_N;
			for (int i = 0; i < (Fx.RANDOM.nextFloat() < 0.75f ? 1 : 0); i++) {
				Particle rain = Fx.particle("sombra_rain_tall", goldRow);
				if (rain != null) {
					Vec3 at = z.at.add(randomUnit().multiply(1, 0, 1).scale(range(0, 4.5f))).add(0, range(2, 4.5f), 0);
					Fx.add(rain.at(at).shape(Shape.STREAK).velocity(new Vec3(0, -range(4, 8), 0)).life(0.6f).size(0.8f, 0.6f).aspect(0.08f)
						.fade(0.05f, 0.5f).nearFade(0.8f));
				}
			}
			if (Fx.RANDOM.nextFloat() < 5f / 20) {
				Particle plus = Fx.particle("sombra_plus", z.enemy ? Palette.SOMBRA_CYBER_ORANGE_N : Palette.SOMBRA_CYBER_PLUS_N);
				if (plus != null) {
					Vec3 at = z.at.add(randomUnit().multiply(1, 0, 1).scale(range(0, 4.5f))).add(0, range(0.2f, 2), 0);
					Fx.add(plus.at(at).velocity(new Vec3(0, range(0.3f, 0.5f), 0)).life(1).size(range(0.43f, 0.61f), 0.4f).fade(0.1f, 0.5f)
						.nearFade(0.6f));
				}
			}
		}
	}

	// ---- her moments (EffectPayload) ----

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Minecraft mc = Minecraft.getInstance();
		float now = Fx.now();
		switch (e.effect()) {
			case "sombra_fire" -> muzzle(at, dir, enemy, mc.level != null && ownFirstPerson(mc.level.getEntity(e.entity())));
			case "sombra_tracer" -> tracer(at, at.add(dir), enemy);
			case "sombra_hit" -> hit(at, enemy);
			case "sombra_hit_block" -> blockHit(at, dir, enemy);
			case "sombra_strands" -> STRANDS.put(e.entity(), new Strands(e.variant() >> 1, now, (e.variant() & 1) == 1, enemy));
			case "sombra_hotfixed" -> HOTFIXED.put(e.entity(), new Held(now, now + e.variant() / 20f, enemy));
			case "sombra_hotfixed_end" -> HOTFIXED.remove(e.entity());
			case "sombra_hacked" -> HACKED.put(e.entity(), new Held(now, now + e.variant() / 20f, enemy));
			case "sombra_revealed" -> REVEALED.put(e.entity(), new Held(now, now + e.variant() / 20f, enemy));
			case "sombra_hacked_thing" -> HACKED_THINGS.put(e.entity(), new Held(now, now + e.variant() / 20f, enemy));
			case "sombra_weakened" -> WEAKENED.put(e.entity(), new Held(now, now + e.variant() / 20f, enemy));
			case "sombra_cyber_launch" -> launch(at, enemy);
			case "sombra_cyberspace" -> {
				burst(at, enemy);
				ZONES.add(new Zone(at, now, now + e.variant() / 20f, enemy));
			}
			case "sombra_teleport" -> teleport(at, enemy);
			case "sombra_stealth" -> stealth(at, enemy);
			case "sombra_emp_cast" -> empCast(at, enemy);
			case "sombra_emp" -> empPulse(at, enemy);
			default -> {
				return false;
			}
		}
		return true;
	}

	/**
	 * The pistol's muzzle (1999, hp 0006): four flame sprites 0.04-0.19 m for 0.07-0.1 s, a spike 0.2-0.43 m for 0.08 s and a
	 * 0.7-0.8 m glow for 0.1 s, magenta to gold. Her own first-person view has the muzzle half a metre from her eye: a third
	 * the size and fainter there (ours, as Ana's).
	 */
	private static void muzzle(Vec3 at, Vec3 dir, boolean enemy, boolean own) {
		float k = own ? 0.35f : 1;
		Palette palette = enemy ? Palette.SOMBRA_ENEMY_RED : Palette.SOMBRA_MUZZLE;
		Vec3 front = at.add(dir.normalize().scale(0.03));
		Particle glow = Fx.particle("lucio_spark", palette);
		if (glow != null) {
			Fx.add(glow.at(front).life(0.1f).size(range(0.7f, 0.8f) * k, 0.5f * k).alpha(own ? 0.35f : 0.7f).fade(0.01f, 0.5f)
				.nearFade(own ? 0 : 0.3f));
		}
		Particle flame = Fx.particle("sombra_muzzle_flame#6", palette);
		if (flame != null) {
			Fx.add(flame.at(front).spin(range(0, 6.28f), 0).life(range(0.07f, 0.1f)).size(range(0.12f, 0.19f) * k, 0.08f * k).loop(60)
				.fade(0.01f, 0.5f).nearFade(own ? 0 : 0.3f));
		}
		Particle spike = Fx.particle("sombra_muzzle_spike#2", palette);
		if (spike != null) {
			Fx.add(spike.at(front.add(dir.normalize().scale(0.12 * k))).axis(dir).size(range(0.2f, 0.43f) * k, 0.2f * k).aspect(0.3f)
				.life(0.08f).loop(25).fade(0.01f, 0.5f).nearFade(own ? 0 : 0.3f));
		}
	}

	/** The tracer (1998): a 0.035 m warm orange band from the muzzle to where the shot ended, 0.15 s. */
	private static void tracer(Vec3 from, Vec3 to, boolean enemy) {
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len < 0.5f) {
			return;
		}
		Particle band = Fx.particle("sombra_tracer", enemy ? Palette.SOMBRA_ENEMY_RED : Palette.SOMBRA_TRACER);
		if (band != null) {
			Fx.add(band.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(0.035f / len).life(0.15f).fade(0.01f, 0.6f)
				.nearFade(0.6f));
		}
	}

	/**
	 * A shot hits someone (010F, the shared impact): an orange spark 0.23-0.33 m for 0.05 s. A colour sprite on black: added
	 * in the tracer's orange (drawn blended as it is, its black square showed: played).
	 */
	private static void hit(Vec3 at, boolean enemy) {
		Particle spark = Fx.particle("sombra_impact_spark#4", enemy ? Palette.SOMBRA_ENEMY_RED : Palette.SOMBRA_TRACER);
		if (spark != null) {
			Fx.add(spark.at(at).life(0.05f).size(range(0.23f, 0.33f), 0.2f).spin(range(0, 6.28f), 0).loop(80).nearFade(0.3f));
		}
	}

	/** On a block: the spark and a 3.5 cm burst mark fading over 0.67 s. */
	private static void blockHit(Vec3 at, Vec3 normal, boolean enemy) {
		hit(at.add(normal.scale(0.03)), enemy);
		Particle mark = Fx.particle("sombra_impact_burst", enemy ? Palette.SOMBRA_ENEMY_RED : Palette.SOMBRA_TRACER);
		if (mark != null && normal.lengthSqr() > 1.0E-6) {
			Fx.add(mark.at(at).normal(normal).lift(normal).life(0.67f).size(0.035f * 3, 0.035f * 3).spin(range(0, 6.28f), 0)
				.fade(0.01f, 0.3f));
		}
	}

	/** Cyberspace leaves her left hand (51FD): violet frames 0.2 and 0.8 m, a 1.75 m flash, a 1 m lens glow. */
	private static void launch(Vec3 at, boolean enemy) {
		Palette p = enemy ? Palette.SOMBRA_CYBER_BURST_ENEMY_N : Palette.SOMBRA_CYBER_BURST_N;
		for (float s : new float[] {0.2f, 0.8f}) {
			Particle frame = Fx.particle("sombra_frame", p);
			if (frame != null) {
				Fx.add(frame.at(at).life(0.2f).size(s, s * 1.3f).fade(0.01f, 0.5f).nearFade(0.4f));
			}
		}
		Particle flash = Fx.particle("lucio_spark", p);
		if (flash != null) {
			Fx.add(flash.at(at).life(0.2f).size(1.75f, 1).alpha(0.5f).fade(0.01f, 0.5f).nearFade(0.5f));
		}
		Particle lens = Fx.particle("ana_lens", p);
		if (lens != null) {
			Fx.add(lens.at(at).life(0.25f).size(1, 1).alpha(0.5f).fade(0.01f, 0.5f).nearFade(0.5f));
		}
	}

	/**
	 * The burst (7CDB): an 8 m circuit sphere for 1.35 s (a dome here), violet flares 1-1.35 m and gold flares 1.5-1.85 m,
	 * 15-32 square sparks at 2-10 m/s, a 1.8 m flower and a 5.5 m ground ring for 0.5 s.
	 */
	private static void burst(Vec3 at, boolean enemy) {
		Palette violet = enemy ? Palette.SOMBRA_CYBER_BURST_ENEMY_N : Palette.SOMBRA_CYBER_BURST_N;
		Palette goldRow = enemy ? Palette.SOMBRA_CYBER_ORANGE_N : Palette.SOMBRA_CYBER_GOLD_N;
		Particle circuit = Fx.particle("sombra_circuit", enemy ? Palette.SOMBRA_CYBER_CIRCUIT_ENEMY_N : Palette.SOMBRA_CYBER_CIRCUIT_N);
		if (circuit != null) {
			Fx.add(circuit.at(at).shape(Shape.DOME).life(1.35f).size(2, 8).alpha(0.6f).fade(0.02f, 0.6f).nearFade(1.5f));
		}
		Particle flare = Fx.particle("mercy_glow", violet);
		if (flare != null) {
			Fx.add(flare.at(at).life(0.35f).size(range(1, 1.35f), 1.5f).alpha(0.8f).fade(0.01f, 0.5f).nearFade(0.8f));
		}
		Particle gflare = Fx.particle("cat_flare", goldRow);
		if (gflare != null) {
			Fx.add(gflare.at(at).life(0.3f).size(range(1.5f, 1.85f), 1.5f).alpha(0.7f).fade(0.01f, 0.5f).nearFade(0.8f));
		}
		int n = 15 + Fx.RANDOM.nextInt(18);
		for (int i = 0; i < n; i++) {
			Particle spark = Fx.particle("sombra_soft_square", i % 2 == 0 ? violet : goldRow);
			if (spark == null) {
				break;
			}
			Fx.add(spark.at(at).velocity(randomUnit().scale(range(2, 10))).drag(0.4f).life(range(0.4f, 0.8f)).size(0.12f, 0.06f)
				.fade(0.02f, 0.5f).nearFade(0.4f));
		}
		Particle ring = Fx.particle("sombra_ring_burst", violet);
		if (ring != null) {
			Fx.add(ring.at(at.add(0, 0.05, 0)).normal(UP).lift(UP).life(0.5f).size(1.8f, 5.5f).fade(0.01f, 0.5f));
		}
	}

	/**
	 * Her teleport (1A76 out / 5FE0 in, at the point + 1 m; 1B1E on her): soft violet glows, a ring of pixel bars, 40 sparks
	 * drawn in at 3 m/s, then a 0.5 m flare and bars thrown out, and a lilac flash on her.
	 */
	private static void teleport(Vec3 at, boolean enemy) {
		Palette p = enemy ? Palette.SOMBRA_TELEPORT_ENEMY : Palette.SOMBRA_TELEPORT;
		Vec3 mid = at.add(0, 1, 0);
		for (int i = 0; i < 6; i++) {
			Particle soft = Fx.particle("lucio_soft", p);
			if (soft != null) {
				Fx.add(soft.at(mid.add(randomUnit().scale(0.3))).life(0.4f).size(range(0.6f, 1), 0.5f).alpha(0.5f).fade(0.05f, 0.5f).nearFade(0.6f));
			}
		}
		for (int i = 0; i < 40; i++) {
			Particle spark = Fx.particle("lucio_spark", p);
			if (spark == null) {
				break;
			}
			Vec3 from = mid.add(randomUnit().scale(1.2));
			Fx.add(spark.at(from).velocity(mid.subtract(from).scale(3 / 1.2)).life(0.35f).size(range(0.35f, 0.5f), 0.1f).fade(0.05f, 0.5f)
				.nearFade(0.5f));
		}
		for (int i = 0; i < 7; i++) {
			Particle bar = Fx.particle("sombra_pixel_bars_b#10", p);
			if (bar != null) {
				Fx.add(bar.at(mid).velocity(randomUnit().scale(range(6, 11))).drag(0.5f).life(0.4f).size(0.3f, 0.15f).loop(30).fade(0.02f, 0.5f)
					.nearFade(0.5f));
			}
		}
		Particle flash = Fx.particle("mercy_glow", enemy ? Palette.SOMBRA_TELEPORT_BODY_ENEMY_N : Palette.SOMBRA_TELEPORT_BODY_N);
		if (flash != null) {
			Fx.add(flash.at(mid).life(0.35f).size(2, 1.4f).alpha(0.6f).fade(0.02f, 0.6f).nearFade(0.8f));
		}
	}

	/** Stealth fades in (1AD9): purple sparkles round her middle as she goes. */
	private static void stealth(Vec3 at, boolean enemy) {
		for (int i = 0; i < 60; i++) {
			Particle sparkle = Fx.particle("glow", purple(enemy));
			if (sparkle == null) {
				break;
			}
			Vec3 side = randomUnit().multiply(1, 0, 1).normalize().scale(range(0.1f, 0.4f));
			Fx.add(sparkle.at(at.add(side.x, range(0.3f, 1.6f), side.z)).velocity(randomUnit().scale(0.3)).life(range(0.3f, 0.8f))
				.size(range(0.03f, 0.05f) * 2, 0.04f).fade(0.05f, 0.5f).nearFade(0.4f));
		}
	}

	/** EMP's cast (223E): six purple rings 1.5-2.4 m for 0.2 s round her. */
	private static void empCast(Vec3 at, boolean enemy) {
		for (int i = 0; i < 6; i++) {
			Particle ring = Fx.particle("sombra_emp_band#10", purple(enemy));
			if (ring != null) {
				float s = range(1.5f, 2.4f);
				Fx.add(ring.at(at.add(0, 0.2 + i * 0.3, 0)).arc(new Quaternionf(), 6.28f).spin(range(0, 6.28f), 0).size(s, s).aspect(0.15f).life(0.2f)
					.loop(50).fade(0.02f, 0.5f).nearFade(0.6f));
			}
		}
	}

	/**
	 * The pulse (1AC8): a skyline band and a circuit dome growing to 30 m across (the 15 m reach) over 0.5 s and 1 s, a
	 * flower growing to about 25 m, 320 glow streaks at 24-29 m/s, a 1.4-2.3 m flash at her.
	 */
	private static void empPulse(Vec3 at, boolean enemy) {
		Palette p = purple(enemy);
		Particle band = Fx.particle("sombra_skyline#14", p);
		if (band != null) {
			Fx.add(band.at(at.add(0, 0.1, 0)).arc(new Quaternionf(), 6.28f).size(2.05f, 30).aspect(0.08f).life(0.5f).loop(28).alpha(0.8f)
				.fade(0.01f, 0.6f).nearFade(1));
		}
		Particle dome = Fx.particle("sombra_circuit", p);
		if (dome != null) {
			Fx.add(dome.at(at).shape(Shape.DOME).size(2, 30).life(1).alpha(0.5f).fade(0.01f, 0.6f).nearFade(2));
		}
		Particle flower = Fx.particle("sombra_ring_burst", p);
		if (flower != null) {
			Fx.add(flower.at(at.add(0, 0.05, 0)).normal(UP).lift(UP).size(8, 25).life(1.1f).alpha(0.7f).fade(0.01f, 0.6f));
		}
		for (int i = 0; i < 320; i++) {
			Particle streak = Fx.particle("cat_glow_dot", p);
			if (streak == null) {
				break;
			}
			Vec3 v = randomUnit().multiply(1, 0.35, 1).normalize().scale(range(24, 29));
			Fx.add(streak.at(at.add(0, 1, 0)).shape(Shape.STREAK).velocity(v).drag(0.2f).life(0.5f).size(0.6f, 0.08f).aspect(0.1f)
				.fade(0.02f, 0.5f).nearFade(1));
		}
		Particle flash = Fx.particle("mercy_glow", p);
		if (flash != null) {
			Fx.add(flash.at(at.add(0, 1, 0)).life(0.3f).size(range(1.4f, 2.3f), 1.5f).alpha(0.8f).fade(0.01f, 0.5f).nearFade(1));
		}
	}

	public static void clear() {
		HACKED.clear();
		REVEALED.clear();
		HOTFIXED.clear();
		WEAKENED.clear();
		HACKED_THINGS.clear();
		STRANDS.clear();
		ZONES.clear();
	}
}
