package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.entity.SigmaBarrierEntity;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.EffectPayload;
import dev.overcraft.sigma.Sigma;
import dev.overcraft.sigma.SigmaStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Sigma's effects, from his data (tools/reference/sigma-effects.md; research\sigma\fx): dark navy, indigo and lavender to
 * white for his team, dark red to white for his enemies (each renderer has a twin row on its LUT), sky blue on his barrier.
 * The server sends his moments (EffectPayload "sigma_*": a volley, a burst, an absorbed shot, the overhealth, the rock's
 * impact, the barrier's break, recall and Hyper Regeneration heal, the reticle (to him), Flux's cast, its targets taken, each
 * lifted target, each slam, Hyper Strike's target); the spheres' and the rock's flights are fed each frame by their renderer
 * (ProjectileModelRenderer); his hand spheres' cores, Kinetic Grasp's field, the gather, Flux's glow round him, Levitation and
 * Hyper Strike's charge are drawn each client tick from his posed points and his synced state (HeroManager.SIGMA_STATE),
 * the barrier's corner motes and the send streaks from each barrier entity.
 */
public final class SigmaFx {
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/**
	 * His points the renderer hands over each frame: on his hand spheres (106B4) the two cores' bones (bone_0442 / bone_0444:
	 * the cores' submeshes 2 / 3 are skinned to them alone, H) and the spheres' muzzle hp 007C (2D5B, each shot); then his
	 * body's: hp 000B Weapon_L (the barrier's hand effects 2DD2 / 2DF5 / 2DD7), hp 0002 Wrist_L (Hyper Strike's 65C7), hp 01B6
	 * (Grasp's dish, the gather 2C64), hp 0076 Head (Flux's 2C68), hp 0004 (Grasp's 2BE4).
	 */
	public static final String[] POINTS = {"bone_0442", "bone_0444", "hardpoint_007C", "hardpoint_000B", "hardpoint_0002",
		"hardpoint_01B6", "hardpoint_0076", "hardpoint_0004"};
	/** The first of POINTS that are on his body (the rest are on his hand spheres). */
	public static final int GUN_POINTS = 3;
	private static final int CORE_A = 0, CORE_B = 1, MUZZLE = 2, LEFT_HAND = 3, WRIST = 4, CHEST = 5, HEAD = 6, BODY = 7;

	private static final class Posed {
		Vec3[] points = new Vec3[0];
		long tick;
	}

	/** Each Sigma's state as last seen: when his gather began (game seconds, -1: none), his synced bits. */
	private static final class His {
		float gatherAt = -1;
		int state;
	}

	/** Something to draw later (a part of an effect at its data time). */
	private record Pending(float at, Runnable run) {
	}

	/** A creature lifted by Flux (1DBC): when, whether enemy-coloured, how long its look lasts. */
	private record Lift(float born, boolean enemy, float life) {
	}

	private static final Map<Integer, Posed> POSED = new HashMap<>();
	private static final Map<Integer, His> HIS = new HashMap<>();
	private static final Map<Integer, Lift> LIFTED = new HashMap<>();

	/** Dev only: what it draws on this entity now. */
	public static String devHeld(int id) {
		return LIFTED.containsKey(id) ? "lifted " : "";
	}
	private static final List<Pending> PENDING = new ArrayList<>();
	/** Spheres and rocks already seen leaving (their launch), by entity id: when last seen. */
	private static final Map<Integer, Float> SEEN = new HashMap<>();
	/** Each barrier's last time its four corners' motes were fed (2C8E: 2 a second each). */
	private static final Map<Integer, Float> MOTES = new HashMap<>();
	private static final Map<String, float[]> SCALED = new HashMap<>();
	/** His reticle (sigma_reticle, his alone): where, and when last sent (its effect ends when the server stops sending). */
	private static @Nullable Vec3 reticle;
	private static float reticleAt = -10;

	/** How far from his own eyes his spheres start to show in his own view (ours, as Sojourn's rounds: 2 m). */
	private static final double OWN_CLEAR = 2;
	/** The burst's sphere, flash and disc against the data's 4.5 / 8-9 / 5 m: shown at a share (ours: a volley is four of them). */
	private static final float BURST_ALPHA = 0.12f;

	private SigmaFx() {
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}

	private static Vec3 randomUnit() {
		Vec3 v = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return v.lengthSqr() < 1.0E-6 ? UP : v.normalize();
	}

	private static Vec3 cone(Vec3 axis, double angle) {
		Vec3 r = randomUnit();
		return axis.normalize().add(r.scale(Math.sin(angle) * Fx.RANDOM.nextFloat())).normalize();
	}

	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/** A particle on a mask, his team's row or the enemy's (blended: added red over a bright sky turned pink-white). */
	private static @Nullable Particle make(String mask, boolean enemy, Palette ours, @Nullable Palette theirs) {
		return enemy ? Fx.soft(mask, theirs == null ? ours : theirs) : Fx.particle(mask, ours);
	}

	/** A dark "anti-glow" part (the burst's streaks, the spheres' motes: rows peaking at 0.1-0.25): blended. */
	private static @Nullable Particle dark(String mask, Palette palette) {
		return Fx.soft(mask, palette);
	}

	/** Each per-second rate as a whole count this tick (the rest by chance). */
	private static int per(float perSecond, float seconds) {
		float n = perSecond * seconds;
		int whole = (int) n;
		return whole + (Fx.RANDOM.nextFloat() < n - whole ? 1 : 0);
	}

	/** An effect mesh with its height scaled (a cylinder band a set height: the data sizes it by script inputs). */
	private static float @Nullable [] scaled(String name, float yScale) {
		float[] m = Fx.mesh(name);
		if (m == null) {
			return null;
		}
		String key = name + "/" + yScale;
		float[] done = SCALED.get(key);
		if (done == null || done.length != m.length) {
			done = m.clone();
			for (int i = 1; i < done.length; i += 5) {
				done[i] *= yScale;
			}
			SCALED.put(key, done);
		}
		return done;
	}

	private static void later(float seconds, Runnable run) {
		PENDING.add(new Pending(Fx.now() + seconds, run));
	}

	// ---- his posed points (HeroModelRenderer) ----

	public static void posed(Entity sigma, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(sigma.getId(), id -> new Posed());
		p.points = points;
		p.tick = sigma.level().getGameTime();
	}

	/** His posed point this tick or the last (null in his own first person, unposed, or without that point). */
	private static @Nullable Vec3 point(@Nullable Entity sigma, int which) {
		if (sigma == null || ownFirstPerson(sigma)) {
			return null;
		}
		Posed p = POSED.get(sigma.getId());
		return p == null || sigma.level().getGameTime() - p.tick > 1 || which >= p.points.length ? null : p.points[which];
	}

	/** His look's frame: ahead, right, up. */
	private static Vec3[] frame(Entity e) {
		Vec3 look = e.getViewVector(1);
		Vec3 right = look.cross(UP);
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
		return new Vec3[] {look, right, right.cross(look).normalize()};
	}

	/** A point of his, else ours from his eyes (ahead, right, up in metres): the first person and an unposed model. */
	private static Vec3 pointOr(Entity sigma, int which, double ahead, double right, double up) {
		Vec3 posed = point(sigma, which);
		if (posed != null) {
			return posed;
		}
		Vec3[] f = frame(sigma);
		return sigma.getEyePosition().add(f[0].scale(ahead)).add(f[1].scale(right)).add(f[2].scale(up));
	}

	// ---- each client tick ----

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			return;
		}
		float now = Fx.now();
		for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
			Pending p = it.next();
			if (now >= p.at) {
				it.remove();
				p.run.run();
			}
		}
		for (Player p : mc.level.players()) {
			if (!"sigma".equals(p.getAttached(HeroManager.HERO)) || !p.isAlive() || p.isSpectator()) {
				HIS.remove(p.getId());
				continue;
			}
			His his = HIS.computeIfAbsent(p.getId(), id -> new His());
			Integer synced = p.getAttached(HeroManager.SIGMA_STATE);
			int state = synced == null ? 0 : synced;
			boolean enemy = KirikoFx.enemyOf(p);
			handSpheres(p, enemy);
			if ((state & Sigma.MODE_GRASP) != 0) {
				grasp(p, enemy);
			}
			if ((state & Sigma.MODE_GATHER) != 0 && (his.state & Sigma.MODE_GATHER) == 0) {
				his.gatherAt = now;
				gather(p, enemy);
			}
			if ((state & Sigma.MODE_FLUX_FLIGHT) != 0) {
				fluxBody(p, enemy);
			}
			if ((state & Sigma.SYNC_LEVITATE) != 0 || p == mc.player && dev.overcraft.client.ClientSigma.levitating(p)) {
				levitate(p, enemy);
			}
			if ((state & Sigma.SYNC_STRIKE) != 0) {
				strikeCharge(p, enemy);
			}
			his.state = state;
		}
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof SigmaBarrierEntity b && b.isAlive()) {
				barrier(mc, b, now);
			}
		}
		lifted(mc, now);
		if (reticle != null && now - reticleAt < 0.2f) {
			reticle(reticle);
		}
		POSED.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		SEEN.values().removeIf(t -> now - t > 2);
		MOTES.keySet().removeIf(id -> mc.level.getEntity(id) == null);
	}

	// ---- his hand spheres ----

	/**
	 * The cores inside his two hand spheres (material 7C98 on submeshes 2 / 3, 0.052 m: a shader of cloud noise 19167 / 18F00
	 * and caustics 5DF through LUT 8000DFB0EAFE row 1, black, plum, violet to blue, its emission rising to 3.4 near its top;
	 * no effect of their own, H) are drawn as their own meshes in that light (HeroModelRenderer.submitModel: both views); here
	 * only a faint 0.16 m halo on each, twinkling (ours: the emission's bloom). Not in his own view (the view model covers it).
	 */
	private static void handSpheres(Player p, boolean enemy) {
		for (int i = CORE_A; i <= CORE_B; i++) {
			Vec3 at = point(p, i);
			if (at == null) {
				continue;
			}
			Fx.attach("sigma_core_halo/" + p.getId() + "/" + i, at, () -> {
				Particle g = make("lucio_glow", enemy, Palette.SIG_HAND_GLOW, null);
				return g == null ? null : g.life(0.4f).size(0.16f, 0.16f).alpha(0.3f).fade(0.05f, 0.5f).twinkle(1.3f).nearFade(0.3f);
			});
		}
	}

	/**
	 * Each volley: his hand spheres re-form (2D5B on hp 007C, from the spheres' fire clips): at 0.6 s a 0.085 m core (SIG_HAND_CORE,
	 * HDR 5) and a 0.68-0.83 m glow 174D8 for 0.68 s (SIG_HAND_GLOW), then from 0.9 s sparkles 1FC54 of 0.24-0.37 m at 7 a
	 * second, 0.23 s (SIG_HAND_SPARK; ours: three of them).
	 */
	private static void volley(@Nullable Entity sigma, boolean enemy) {
		if (sigma == null) {
			return;
		}
		int id = sigma.getId();
		later(0.6f, () -> {
			Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(id);
			Vec3 at = point(e, MUZZLE);
			if (at == null) {
				return;
			}
			Particle core = make("lucio_glow", enemy, Palette.SIG_HAND_CORE, null);
			if (core != null) {
				Fx.add(core.at(at).life(0.35f).size(0.085f, 0.06f).alpha(0.9f).fade(0.05f, 0.6f).nearFade(0.3f));
			}
			Particle glow = make("brigitte_soft_blob_b", enemy, Palette.SIG_HAND_GLOW, null);
			if (glow != null) {
				Fx.add(glow.at(at).life(0.68f).size(range(0.68f, 0.83f) * 0.6f, 0.3f).alpha(0.35f).fade(0.1f, 0.6f).nearFade(0.5f));
			}
		});
		for (int i = 0; i < 3; i++) {
			later(0.9f + i * 0.14f, () -> {
				Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(id);
				Vec3 at = point(e, MUZZLE);
				Particle spark = at == null ? null : make("sigma_wisp_ring", enemy, Palette.SIG_HAND_SPARK, Palette.SIG_HAND_SPARK_ENEMY);
				if (spark != null) {
					Fx.add(spark.at(at.add(randomUnit().scale(0.05))).life(0.23f).size(range(0.24f, 0.37f) * 0.6f, 0.05f)
						.spin(range(0, 6.28f), 3).alpha(0.7f).fade(0.1f, 0.5f).nearFade(0.3f));
				}
			});
		}
	}

	// ---- Hyperspheres ----

	/**
	 * A sphere in flight (ProjectileModelRenderer, each frame): false without the local pack (its item stands in). The first
	 * time it's seen, its launch 2BE1 at his spheres' muzzle (hp 007C): a swirl 20126 stretched along the shot (0.18-0.49 m x
	 * 11-13, 0.18 s), a 0.6 m wisp glow for the cone CA08 (ours: no mesh). In flight 2BDE (child 311A): the sphere itself, 85B
	 * at 0.22 m (SIG_SPHERE_CORE, HDR 7: a bright core), a 0.32 m halo 4C09 (SIG_SPHERE_HALO), a wide ribbon 1 m for 0.25 s and
	 * a thin one 0.25 m for 0.32 s (SIG_SPHERE_TRAIL / _THIN on a soft band: their 1298 is solid white, the ramp is the shape)
	 * and dark motes 1F35 at 10-16 a second drifting off at 3 m/s (SIG_SPHERE_MOTE, peak 0.23: blended). In his own view from
	 * 2 m out.
	 */
	public static boolean sphereFlight(int id, Vec3 at, Vec3 velocity, float dt, @Nullable Entity owner) {
		boolean enemy = KirikoFx.enemyOf(owner);
		if (SEEN.put(id, Fx.now()) == null && owner != null) {
			launch(owner, velocity, enemy);
		}
		Minecraft mc = Minecraft.getInstance();
		if (owner == mc.player && mc.options.getCameraType().isFirstPerson() && at.distanceTo(mc.player.getEyePosition()) < OWN_CLEAR) {
			return EffectArt.get("lucio_glow", Palette.SIG_SPHERE_CORE, false) != null;
		}
		Particle core = Fx.attach("hypersphere/" + id, at, () -> {
			Particle g = make("lucio_glow", enemy, Palette.SIG_SPHERE_CORE, Palette.SIG_SPHERE_CORE_ENEMY);
			return g == null ? null : g.life(0.1f).size(0.22f, 0.22f).alpha(1).fade(0.02f, 0.5f).nearFade(1.5f);
		});
		if (core == null) {
			return false;
		}
		Fx.attach("hypersphere_halo/" + id, at, () -> {
			Particle g = make("cat_soft", enemy, Palette.SIG_SPHERE_HALO, Palette.SIG_SPHERE_HALO_ENEMY);
			return g == null ? null : g.life(0.1f).size(0.4f, 0.4f).alpha(0.5f).fade(0.02f, 0.5f).nearFade(1.5f);
		});
		Fx.trail(id, at, "cat_band", enemy ? Palette.SIG_SPHERE_TRAIL_ENEMY : Palette.SIG_SPHERE_TRAIL, 0.6f, 0.25f, 0.5f, enemy);
		Fx.trail(-id - 7_000_000, at, "cat_band", enemy ? Palette.SIG_SPHERE_TRAIL_THIN_ENEMY : Palette.SIG_SPHERE_TRAIL_THIN, 0.25f,
			0.32f, 0.9f, enemy);
		for (int i = per(13, dt); i > 0; i--) {
			Particle mote = dark("spark#4", Palette.SIG_SPHERE_MOTE);
			if (mote != null) {
				Fx.add(mote.at(at).velocity(randomUnit().scale(3)).life(0.33f).size(0.16f, 0.05f).alpha(0.8f).fade(0.05f, 0.5f)
					.nearFade(0.5f));
			}
		}
		return true;
	}

	private static void launch(Entity owner, Vec3 velocity, boolean enemy) {
		boolean own = ownFirstPerson(owner);
		Vec3 dir = velocity.lengthSqr() < 1.0E-6 ? owner.getViewVector(1) : velocity.normalize();
		Vec3 at = own ? owner.getEyePosition().add(dir.scale(OWN_CLEAR)) : pointOr(owner, MUZZLE, 0.6, 0.35, -0.3);
		float k = own ? 0.5f : 1;
		Particle swirl = make("sigma_swirl", enemy, Palette.SIG_SPHERE_LAUNCH, null);
		if (swirl != null) {
			// (stretched along the shot as the data's x11-13 it drew as a bar: a spinning swirl, ours)
			Fx.add(swirl.at(at).life(0.18f).size(range(0.18f, 0.49f) * k, 0.1f * k).spin(range(0, 6.28f), 12).alpha(0.8f).fade(0.05f, 0.6f)
				.nearFade(own ? 0 : 0.3f));
		}
		Particle ring = make("sigma_wisp_soft", enemy, Palette.SIG_SPHERE_LAUNCH_RING, Palette.SIG_SPHERE_LAUNCH_RING_ENEMY);
		if (ring != null) {
			Fx.add(ring.at(at).life(0.3f).size(range(0.33f, 0.82f) * k, 0.8f * k).alpha(0.5f).spin(range(0, 6.28f), 2).fade(0.05f, 0.6f)
				.nearFade(own ? 0 : 0.3f));
		}
	}

	/**
	 * A sphere's burst (2EAB, the same on a creature, the world or at its range's end; its hit slot is sounds only): a shell
	 * 4.5 m for 0.32-0.37 s (SIG_BURST_SHELL), a flash 8-9 m for 0.25-0.3 s (208CC x 7AB, SIG_BURST_FLASH), a disc 4.4-5.4 x
	 * 1.28 m for 0.3-0.39 s (1FDB2, SIG_BURST_RING, HDR 10) and dark streaks stretched x20 (SIG_BURST_STREAK, peak 0.12); no
	 * refraction (not drawable). A volley is two of them in 0.2 s: the big parts faint (BURST_ALPHA, ours).
	 */
	private static void burst(Vec3 at, boolean enemy) {
		float[] sphere = Fx.mesh("cat_sphere");
		Particle shell = make("cat_band", enemy, Palette.SIG_BURST_SHELL, Palette.SIG_BURST_SHELL_ENEMY);
		if (shell != null) {
			float d = 4.5f * 0.6f;
			Fx.add(sphere != null ? shell.at(at.subtract(0, d * 0.51, 0)).mesh(sphere, new Vec3(1, 0, 0)).life(0.35f).size(d * 0.4f, d)
				.easeOut().alpha(BURST_ALPHA * 0.6f).fade(0.05f, 0.7f) : shell.at(at).life(0.35f).size(1, d).alpha(BURST_ALPHA).fade(0.05f, 0.7f));
		}
		Particle flash = make("sigma_burst_flash", enemy, Palette.SIG_BURST_FLASH, Palette.SIG_BURST_FLASH_ENEMY);
		if (flash != null) {
			Fx.add(flash.at(at).life(range(0.25f, 0.3f)).size(range(8, 9) * 0.35f, 2).spin(range(0, 6.28f), 0).alpha(0.6f).fade(0.02f, 0.7f)
				.nearFade(1));
		}
		Particle disc = make("sigma_lens", enemy, Palette.SIG_BURST_RING, Palette.SIG_BURST_RING_ENEMY);
		if (disc != null) {
			Fx.add(disc.at(at).life(range(0.3f, 0.39f)).size(1.2f, range(4.4f, 5.4f) * 0.5f).easeOut().spin(range(0, 6.28f), 0).alpha(0.45f)
				.fade(0.02f, 0.7f).nearFade(1));
		}
		for (int i = 0; i < 6; i++) {
			Particle streak = dark("sigma_streak", enemy ? Palette.SIG_BURST_STREAK_ENEMY : Palette.SIG_BURST_STREAK);
			if (streak != null) {
				Fx.add(streak.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(4, 8))).drag(0.05f).life(range(0.35f, 0.4f))
					.size(range(0.27f, 0.57f), 0.2f).aspect(0.1f).alpha(0.9f).fade(0.02f, 0.6f).nearFade(0.5f));
			}
		}
	}

	// ---- Experimental Barrier ----

	/**
	 * A barrier out (each tick): its corner motes 2C8E (the pet's child 31F1, always on: 4DB x 1FC54, 0.65-0.75 m, 2 a second
	 * at each of 0029 / 0032 / 002A / 0033, 0.66-1.3 s; SIG_BARRIER_MOTE), and while sent, his hand's streaks 2DD2 (16E9D x
	 * 4C09, 0.49 m stretched x2, 6 a second at 4.9 m/s along his aim, 0.67-1.3 s; SIG_BARRIER_SEND) off his left hand.
	 */
	private static void barrier(Minecraft mc, SigmaBarrierEntity b, float now) {
		Entity owner = mc.level.getEntity(b.ownerId());
		boolean enemy = KirikoFx.enemyOf(owner);
		Float last = MOTES.put(b.getId(), now);
		float dt = last == null ? 0.05f : Math.clamp(now - last, 0, 0.25f);
		Vec3 c = b.barrierCentre();
		Vec3 face = b.barrierFacing();
		Vec3 side = face.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		double hw = SigmaStats.BARRIER_WIDTH / 2 * 0.95, hh = SigmaStats.BARRIER_HEIGHT / 2 * 0.95;
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sy = -1; sy <= 1; sy += 2) {
				for (int i = per(2, dt); i > 0; i--) {
					Particle mote = make("sigma_mote", enemy, Palette.SIG_BARRIER_MOTE, Palette.SIG_BARRIER_MOTE_ENEMY);
					if (mote != null) {
						Fx.add(mote.at(c.add(side.scale(sx * hw)).add(0, sy * hh, 0)).life(range(0.66f, 1.3f)).size(range(0.65f, 0.75f), 0.3f)
							.spin(range(0, 6.28f), range(-1, 1)).alpha(0.6f).fade(0.2f, 0.6f).nearFade(1));
					}
				}
			}
		}
		if (owner != null && b.state() == SigmaBarrierEntity.SENT) {
			Vec3 hand = pointOr(owner, LEFT_HAND, 0.5, -0.4, -0.4);
			Vec3 aim = owner.getViewVector(1);
			for (int i = per(6, dt); i > 0; i--) {
				Particle streak = make("sigma_send_streak", enemy, Palette.SIG_BARRIER_SEND, null);
				if (streak != null) {
					Fx.add(streak.at(hand).shape(Shape.STREAK).velocity(cone(aim, 0.2).scale(4.9)).life(range(0.67f, 1.3f) * 0.6f)
						.size(0.49f, 0.2f).aspect(0.5f).alpha(0.5f).fade(0.1f, 0.6f).nearFade(0.8f));
				}
			}
		}
	}

	/**
	 * The barrier recalled (1A2A #675 / #691): at it 2C90, a 2 m flash 29F7 for 0.1 s and glows 174D8 of 2 / 3 m for 0.18-0.36 s
	 * (SIG_BARRIER_RECALL_FLASH / _GLOW); then 2E68 at each corner: 2-3 streaks 1FBCE of 1 m flying to him at 20-30 m/s,
	 * 0.19-0.33 s (SIG_BARRIER_FLYBACK); at his hand 2DD7's 0.84 m glow (SIG_BARRIER_HAND).
	 */
	private static void recall(Vec3 c, Vec3 face, @Nullable Entity owner, boolean enemy) {
		Particle flash = make("lucio_spark", enemy, Palette.SIG_BARRIER_RECALL_FLASH, Palette.SIG_BARRIER_RECALL_FLASH_ENEMY);
		if (flash != null) {
			Fx.add(flash.at(c).life(0.1f).size(2, 2.4f).alpha(0.6f).fade(0.02f, 0.7f).nearFade(1));
		}
		for (float size : new float[] {2, 3}) {
			Particle glow = make("brigitte_soft_blob_b", enemy, Palette.SIG_BARRIER_RECALL_GLOW, Palette.SIG_BARRIER_RECALL_GLOW_ENEMY);
			if (glow != null) {
				Fx.add(glow.at(c).life(range(0.18f, 0.36f)).size(size, size * 0.6f).alpha(0.4f).fade(0.05f, 0.7f).nearFade(1));
			}
		}
		Vec3 side = face.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 to = owner == null ? c : pointOr(owner, LEFT_HAND, 0.5, -0.4, -0.4);
		double hw = SigmaStats.BARRIER_WIDTH / 2, hh = SigmaStats.BARRIER_HEIGHT / 2;
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sy = -1; sy <= 1; sy += 2) {
				Vec3 corner = c.add(side.scale(sx * hw)).add(0, sy * hh, 0);
				Vec3 dir = to.subtract(corner);
				double d = dir.length();
				dir = d < 1.0E-3 ? UP : dir.scale(1 / d);
				for (int i = 0; i < 3; i++) {
					float speed = range(20, 30);
					Particle streak = make("sigma_streak", enemy, Palette.SIG_BARRIER_FLYBACK, Palette.SIG_BARRIER_FLYBACK_ENEMY);
					if (streak != null) {
						Fx.add(streak.at(corner).shape(Shape.STREAK).velocity(dir.scale(speed)).life((float) Math.min(0.33, d / speed))
							.size(1, 0.6f).aspect(0.15f).alpha(0.8f).fade(0.05f, 0.4f).nearFade(0.8f));
					}
				}
			}
		}
		if (owner != null) {
			Particle hand = make("cat_soft", enemy, Palette.SIG_BARRIER_HAND, null);
			if (hand != null) {
				Fx.add(hand.at(to).life(0.35f).size(0.84f, 0.5f).alpha(0.6f).fade(0.05f, 0.6f).nearFade(0.5f));
			}
		}
	}

	/**
	 * Broken (4E26 stopped or held, 2E66 while moving: M which): 16 puffs 14F9 of 0.68-1.28 m in a 65-degree cone (SIG_BARRIER_BREAK),
	 * 50 sparks 1F35 at 4.4-9.4 m/s (SIG_BARRIER_BREAK_SPARK), 12 hex shards 2B0E of 0.75-1.3 m at 3-5 m/s (SIG_BARRIER_BREAK_STREAK);
	 * 2E66 adds a barrier-shaped flash (A3C, 6.79 x 4.25 m, 0.18 s, 17C0: SIG_BARRIER_SHATTER) drawn here as a flat flash its size
	 * with the field's rim. Spread over its 6.7 x 4.2 m face (ours: the data spawns them from a point).
	 */
	private static void breakApart(Vec3 c, Vec3 face, boolean moving, boolean enemy) {
		Vec3 side = face.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		double w = SigmaStats.BARRIER_WIDTH, h = SigmaStats.BARRIER_HEIGHT;
		for (int i = 0; i < 16; i++) {
			Particle puff = make("lw_thorn_flame#2", enemy, Palette.SIG_BARRIER_BREAK, Palette.SIG_BARRIER_BREAK_ENEMY);
			if (puff != null) {
				Vec3 at = c.add(side.scale((Fx.RANDOM.nextFloat() - 0.5) * w)).add(0, (Fx.RANDOM.nextFloat() - 0.5) * h, 0);
				Fx.add(puff.at(at).velocity(cone(face, 1.1).scale(range(1, 3))).life(range(0.2f, 0.35f)).size(range(0.68f, 1.28f), 1.6f)
					.spin(range(0, 6.28f), range(-2, 2)).alpha(0.6f).fade(0.05f, 0.6f).nearFade(1));
			}
		}
		for (int i = 0; i < 50; i++) {
			Particle spark = make("spark#4", enemy, Palette.SIG_BARRIER_BREAK_SPARK, Palette.SIG_BARRIER_BREAK_SPARK_ENEMY);
			if (spark != null) {
				Vec3 at = c.add(side.scale((Fx.RANDOM.nextFloat() - 0.5) * w)).add(0, (Fx.RANDOM.nextFloat() - 0.5) * h, 0);
				Fx.add(spark.at(at).shape(Shape.STREAK).velocity(cone(Fx.RANDOM.nextBoolean() ? face : face.reverse(), 1.2)
					.scale(range(4.4f, 9.4f))).gravity(6).drag(0.3f).life(range(0.25f, 1)).size(range(0.08f, 0.1f) * 1.5f, 0.03f)
					.aspect(0.3f).fade(0.02f, 0.5f).nearFade(0.5f));
			}
		}
		for (int i = 0; i < 12; i++) {
			Particle shard = make("brigitte_hex_outline", enemy, Palette.SIG_BARRIER_BREAK_STREAK, null);
			if (shard != null) {
				Vec3 at = c.add(side.scale((Fx.RANDOM.nextFloat() - 0.5) * w)).add(0, (Fx.RANDOM.nextFloat() - 0.5) * h, 0);
				Fx.add(shard.at(at).velocity(randomUnit().scale(range(3, 5))).life(range(0.2f, 0.27f) * 2).size(range(0.75f, 1.3f) * 0.6f, 0.2f)
					.spin(range(0, 6.28f), range(-6, 6)).alpha(0.8f).fade(0.02f, 0.5f).nearFade(0.8f));
			}
		}
		Particle flash = make("sigma_barrier_field", enemy, Palette.SIG_BARRIER_SHATTER, Palette.SIG_BARRIER_ENEMY);
		if (flash != null) {
			Fx.add(flash.at(c).shape(Shape.FLAT).normal(face).life(moving ? 0.18f : 0.25f).size((float) h, (float) h * 1.05f)
				.aspect((float) (w / h)).alpha(0.5f).fade(0.02f, 0.8f).nearFade(0.5f));
		}
	}

	/** Hyper Regeneration's heal on the barrier (6E81 at its 01DA, 16 a second for 0.5 s): teal streaks rising over its face. */
	private static void barrierHeal(Vec3 c, Vec3 face, boolean enemy) {
		Vec3 side = face.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		for (int i = 0; i < 8; i++) {
			Particle streak = make("brigitte_hex", enemy, Palette.SIG_BARRIER_HEAL, Palette.SIG_BARRIER_HEAL_ENEMY);
			if (streak != null) {
				Vec3 at = c.add(side.scale((Fx.RANDOM.nextFloat() - 0.5) * SigmaStats.BARRIER_WIDTH * 0.8))
					.add(0, (Fx.RANDOM.nextFloat() - 0.7) * SigmaStats.BARRIER_HEIGHT * 0.6, 0);
				Fx.add(streak.at(at).shape(Shape.STREAK).velocity(new Vec3(0, range(1.5f, 2.5f), 0)).life(range(0.34f, 0.66f))
					.size(0.95f * 0.5f, 0.2f).aspect(0.4f).alpha(0.7f).fade(0.1f, 0.6f).nearFade(0.8f));
			}
		}
	}

	// ---- Kinetic Grasp ----

	/**
	 * Held (2BE4, each tick): its disc C770 2.8 m ahead, facing him, 8 a second, 0.37-0.49 s (18A1 x 200E4: SIG_GRASP_FIELD) drawn
	 * flat 2.7 m tall (the box it guards: the data's size read as quads would be 8-9 m, ours until checked); its shell 2 a
	 * second at 1.5 m drifting back (SIG_GRASP_SHELL); an orb 0.3 m held before his left palm (SIG_GRASP_ORB) with glows
	 * 208CC flickering round it (SIG_GRASP_GLOW); a 3 m haze 2 a second (SIG_GRASP_HAZE).
	 */
	private static void grasp(Player p, boolean enemy) {
		boolean own = ownFirstPerson(p);
		Vec3[] f = frame(p);
		Vec3 eye = p.getEyePosition();
		Vec3 centre = eye.add(f[0].scale(SigmaStats.GRASP_FAR)).add(f[2].scale((SigmaStats.GRASP_UP - SigmaStats.GRASP_DOWN) / 2));
		float dt = 0.05f;
		for (int i = per(8, dt); i > 0; i--) {
			Particle disc = make("sigma_wisp_soft", enemy, Palette.SIG_GRASP_FIELD, Palette.SIG_GRASP_SHELL_ENEMY);
			if (disc != null) {
				Fx.add(disc.at(centre).shape(Shape.FLAT).normal(f[0].reverse()).life(range(0.37f, 0.49f)).size(2.4f, 2.9f)
					.spin(range(0, 6.28f), range(-1, 1)).alpha(own ? 0.1f : 0.18f).fade(0.2f, 0.6f).nearFade(own ? 0 : 2.5f));
			}
		}
		for (int i = per(2, dt); i > 0; i--) {
			Particle shell = make("cat_soft", enemy, Palette.SIG_GRASP_SHELL, Palette.SIG_GRASP_SHELL_ENEMY);
			if (shell != null) {
				Fx.add(shell.at(centre).velocity(f[0].scale(-0.25)).life(1).size(1.5f, 1.7f).alpha(own ? 0.12f : 0.25f).fade(0.3f, 0.6f)
					.nearFade(own ? 0 : 1));
			}
		}
		if (!own) {
			Vec3 palm = pointOr(p, BODY, 0.8, 0, -0.3);
			Fx.attach("sigma_grasp_orb/" + p.getId(), palm, () -> {
				Particle orb = make("lucio_glow", enemy, Palette.SIG_GRASP_ORB, Palette.SIG_GRASP_ORB_ENEMY);
				return orb == null ? null : orb.life(0.3f).size(0.3f, 0.3f).alpha(0.9f).fade(0.1f, 0.5f).nearFade(0.5f);
			});
			for (int i = per(14, dt); i > 0; i--) {
				Particle glow = make("sigma_star", enemy, Palette.SIG_GRASP_GLOW, Palette.SIG_GRASP_GLOW_ENEMY);
				if (glow != null) {
					Fx.add(glow.at(palm).life(range(0.1f, 0.15f)).size(range(0.6f, 0.8f), 0.4f).spin(range(0, 6.28f), 0).alpha(0.35f)
						.fade(0.05f, 0.6f).nearFade(0.8f));
				}
			}
		}
		for (int i = per(2, dt); i > 0; i--) {
			Particle haze = make("cat_soft", enemy, Palette.SIG_GRASP_HAZE, null);
			if (haze != null) {
				Fx.add(haze.at(centre).life(2).size(3, 3).alpha(own ? 0.03f : 0.05f).fade(0.3f, 0.6f).nearFade(own ? 0 : 3));
			}
		}
	}

	/**
	 * A shot absorbed (2C84 where it dies): a flare 208CC of about 1.5 m for 0.14 s (SIG_GRASP_ABSORB), the 4-frame ring 1CB5E
	 * (SIG_GRASP_ABSORB_RING), and 222B / 222F's sparks (58 at 4.4-7.6 m/s: ours 12).
	 */
	private static void absorb(Vec3 at, Vec3 dir, boolean enemy) {
		Particle flare = make("sigma_star", enemy, Palette.SIG_GRASP_ABSORB, Palette.SIG_GRASP_ABSORB_ENEMY);
		if (flare != null) {
			Fx.add(flare.at(at).life(0.14f).size(1.5f, 0.8f).spin(range(0, 6.28f), 0).alpha(0.8f).fade(0.02f, 0.6f).nearFade(0.8f));
		}
		Particle ring = make("juno_hit_ring#4", enemy, Palette.SIG_GRASP_ABSORB_RING, Palette.SIG_GRASP_ABSORB_RING_ENEMY);
		if (ring != null) {
			Fx.add(ring.at(at).life(0.25f).size(0.6f, 1.4f).alpha(0.8f).fade(0.02f, 0.5f).nearFade(0.8f));
		}
		Vec3 back = dir.lengthSqr() < 1.0E-6 ? UP : dir.normalize().reverse();
		for (int i = 0; i < 12; i++) {
			Particle spark = make("spark#4", enemy, Palette.SIG_GRASP_ABSORB, Palette.SIG_GRASP_ABSORB_ENEMY);
			if (spark != null) {
				Fx.add(spark.at(at).shape(Shape.STREAK).velocity(cone(back, 1.2).scale(range(4.4f, 7.6f))).drag(0.1f).life(range(0.15f, 0.3f))
					.size(0.1f, 0.03f).aspect(0.3f).fade(0.02f, 0.5f).nearFade(0.4f));
			}
		}
	}

	/**
	 * The overhealth given (2D3E on him, hp 0001): short streaks rising round his upper body for 0.27 s (300 a second at 0.1-0.2 m,
	 * 2 m up: SIG_OVERHEALTH_STREAK; ours 40), at 0.73 s a 2 m sky-blue sphere for 0.4-0.52 s (SIG_OVERHEALTH_SHELL), and its
	 * material effect on his body for 2.17 s as a faint violet glow round him (SIG_OVERHEALTH: ours, not a material).
	 */
	private static void overhealth(@Nullable Entity sigma, boolean enemy) {
		if (sigma == null) {
			return;
		}
		boolean own = ownFirstPerson(sigma);
		int id = sigma.getId();
		Vec3 base = sigma.position();
		for (int i = 0; i < (own ? 12 : 40); i++) {
			Particle streak = make("cat_streak", enemy, Palette.SIG_OVERHEALTH_STREAK, null);
			if (streak != null) {
				double a = Fx.RANDOM.nextFloat() * Math.PI * 2;
				Vec3 at = base.add(Math.cos(a) * 0.8, range(0.8f, 2.2f), Math.sin(a) * 0.8);
				Fx.add(streak.at(at).shape(Shape.STREAK).velocity(new Vec3(0, range(2, 4), 0)).life(0.5f).size(range(0.1f, 0.2f) * 2, 0.05f)
					.aspect(0.3f).alpha(0.8f).fade(0.05f, 0.6f).nearFade(own ? 0 : 0.5f));
			}
		}
		later(0.73f, () -> {
			Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(id);
			if (e == null) {
				return;
			}
			Particle shell = make("cat_soft", enemy, Palette.SIG_OVERHEALTH_SHELL, Palette.SIG_OVERHEALTH_SHELL_ENEMY);
			if (shell != null) {
				Fx.add(shell.at(e.position().add(0, e.getBbHeight() * 0.55, 0)).life(range(0.4f, 0.52f)).size(2, 2.6f)
					.alpha(ownFirstPerson(e) ? 0.08f : 0.3f).fade(0.1f, 0.7f).nearFade(ownFirstPerson(e) ? 0 : 1));
			}
		});
		if (!own) {
			Particle body = make("cat_soft", enemy, Palette.SIG_OVERHEALTH, Palette.SIG_OVERHEALTH_ENEMY);
			if (body != null) {
				Fx.add(body.at(base.add(0, sigma.getBbHeight() * 0.55, 0)).life(2.17f).size(2.4f, 2.4f).alpha(0.18f).fade(0.1f, 0.6f)
					.nearFade(1));
			}
		}
	}

	// ---- Accretion ----

	/**
	 * The gather (2C64 at hp 01B6 through the 0.65 s cast): at 0.37 s spheres of 1.58 and 3.16 m for 0.45 s (SIG_ROCK_GATHER),
	 * at 0.5 s dishes of 1.32 / 1.65 m (C943 drawn flat: SIG_ROCK_GATHER_DISC), at 0.83-0.9 s a 3.5 m flash and a 1.96 m star
	 * for 0.15 s (SIG_ROCK_GATHER_FLASH). Not in his own view (his arms gather there).
	 */
	private static void gather(Player p, boolean enemy) {
		int id = p.getId();
		later(0.37f, () -> gatherPart(id, enemy, 0));
		later(0.5f, () -> gatherPart(id, enemy, 1));
		later(0.85f, () -> gatherPart(id, enemy, 2));
	}

	private static void gatherPart(int id, boolean enemy, int part) {
		Minecraft mc = Minecraft.getInstance();
		Entity p = mc.level == null ? null : mc.level.getEntity(id);
		if (p == null || ownFirstPerson(p)) {
			return;
		}
		Vec3 at = pointOr(p, CHEST, 0.8, 0.2, -0.6);
		switch (part) {
			case 0 -> {
				for (float size : new float[] {1.58f, 3.16f}) {
					Particle s = make("cat_soft", enemy, Palette.SIG_ROCK_GATHER, Palette.SIG_ROCK_GATHER_ENEMY);
					if (s != null) {
						Fx.add(s.at(at).life(0.45f).size(size * 0.3f, size * 0.5f).alpha(size > 2 ? 0.25f : 0.5f).fade(0.2f, 0.6f).nearFade(0.8f));
					}
				}
			}
			case 1 -> {
				for (float size : new float[] {1.32f, 1.65f}) {
					Particle d = make("sigma_disc", enemy, Palette.SIG_ROCK_GATHER_DISC, null);
					if (d != null) {
						Fx.add(d.at(at).shape(Shape.FLAT).normal(p.getViewVector(1).reverse()).life(range(0.34f, 0.37f)).size(size * 0.5f, size)
							.alpha(0.4f).fade(0.1f, 0.6f).nearFade(0.8f));
					}
				}
			}
			default -> {
				Particle flash = make("sigma_soft_square", enemy, Palette.SIG_ROCK_GATHER_FLASH, null);
				if (flash != null) {
					Fx.add(flash.at(at).life(0.15f).size(3.5f * 0.6f, 3.5f * 0.7f).alpha(0.35f).fade(0.05f, 0.7f).nearFade(1));
				}
				Particle star = make("sigma_star", enemy, Palette.SIG_ROCK_GATHER_FLASH, null);
				if (star != null) {
					Fx.add(star.at(at).life(0.15f).size(1.96f, 1.4f).spin(range(0, 6.28f), 0).alpha(0.6f).fade(0.05f, 0.7f).nearFade(0.8f));
				}
			}
		}
	}

	/**
	 * The rock in flight (2D99, child 3330; fed each frame with its model drawn): two ribbons up to 1 m wide for 0.5 s (a black-blue
	 * bell: SIG_ROCK_TRAIL, on a soft band), and debris chunks 2C0F8 of 0.12-0.22 m, 20 a second, falling off at 4-4.9 m/s for
	 * 1.9-2.8 s, spinning (grey stone, blended, with gravity: ours, the data's colour row is a parameter).
	 */
	public static void rockFlight(int id, Vec3 at, float dt, @Nullable Entity owner) {
		boolean enemy = KirikoFx.enemyOf(owner);
		SEEN.put(id, Fx.now());
		Fx.trail(id, at, "cat_band", enemy ? Palette.SIG_ROCK_TRAIL_ENEMY : Palette.SIG_ROCK_TRAIL, 1.0f, 0.5f, 0.8f, enemy);
		for (int i = per(20, dt); i > 0; i--) {
			Particle chunk = Fx.soft("sigma_rock_chunk#4", Palette.SIG_ROCK_SMOKE);
			if (chunk != null) {
				Fx.add(chunk.at(at.add(randomUnit().scale(0.6))).velocity(randomUnit().scale(range(1, 2))).gravity(9).life(range(1.0f, 1.5f))
					.size(range(0.12f, 0.22f) * 1.3f, 0.1f).spin(range(0, 6.28f), -13).alpha(0.9f).fade(0.02f, 0.3f).nearFade(0.5f));
			}
		}
	}

	/**
	 * The rock's burst (2C07): a 2.5 m flash 4DB x 1FC54 (SIG_ROCK_IMPACT_FLASH), 3 stars 208CC (SIG_ROCK_IMPACT_GLOW), a violet
	 * core (SIG_ROCK_CORE, HDR 17), 12 puffs 14F9 at 3.3-5.5 m/s (SIG_ROCK_IMPACT_DUST), 5 brown dust clouds 5429 stretched for 2 s
	 * (SIG_ROCK_SMOKE), 15 rock chunks 2C0F8 thrown out at 6-8.4 m/s in a 64-degree cone, and its crater decal 2048A on the face it
	 * hit for 3.9 s (SIG_ROCK_SCORCH). No distortion dome (not drawable).
	 */
	private static void rockImpact(Vec3 at, Vec3 face, boolean enemy) {
		boolean surface = face.lengthSqr() > 1.0E-6;
		Vec3 out = surface ? face.normalize() : UP;
		Particle flash = make("sigma_mote", enemy, Palette.SIG_ROCK_IMPACT_FLASH, Palette.SIG_ROCK_IMPACT_FLASH_ENEMY);
		if (flash != null) {
			Fx.add(flash.at(at).life(range(0.25f, 0.35f)).size(2.5f, 3.2f).spin(range(0, 6.28f), 0).alpha(0.6f).fade(0.02f, 0.7f).nearFade(1));
		}
		for (int i = 0; i < 3; i++) {
			Particle star = make("sigma_star", enemy, Palette.SIG_ROCK_IMPACT_GLOW, Palette.SIG_ROCK_IMPACT_GLOW_ENEMY);
			if (star != null) {
				Fx.add(star.at(at.add(randomUnit().scale(0.3))).life(range(0.2f, 0.32f)).size(range(1.2f, 1.8f), 0.6f).spin(range(0, 6.28f), 0)
					.alpha(0.6f).fade(0.02f, 0.7f).nearFade(0.8f));
			}
		}
		Particle core = make("lucio_glow", enemy, Palette.SIG_ROCK_CORE, null);
		if (core != null) {
			Fx.add(core.at(at).life(0.26f).size(1.2f, 0.4f).alpha(0.9f).fade(0.02f, 0.6f).nearFade(0.8f));
		}
		for (int i = 0; i < 12; i++) {
			Particle puff = make("lw_thorn_flame#2", enemy, Palette.SIG_ROCK_IMPACT_DUST, Palette.SIG_ROCK_IMPACT_DUST_ENEMY);
			if (puff != null) {
				Fx.add(puff.at(at).velocity(cone(out, 1.1).scale(range(3.3f, 5.5f))).drag(0.05f).life(range(0.15f, 0.3f)).size(0.6f, 1.3f)
					.spin(range(0, 6.28f), 0).alpha(0.6f).fade(0.02f, 0.6f).nearFade(0.8f));
			}
		}
		for (int i = 0; i < 5; i++) {
			Particle dust = Fx.soft("ana_puff#4", Palette.SIG_ROCK_SMOKE);
			if (dust != null) {
				Fx.add(dust.at(at.add(randomUnit().scale(0.5))).velocity(cone(out, 1).scale(range(0.4f, 1))).drag(0.3f).life(2)
					.size(range(0.7f, 1.7f), 2.4f).spin(range(0, 6.28f), range(-0.3f, 0.3f)).alpha(0.5f).fade(0.1f, 0.7f).nearFade(1));
			}
		}
		for (int i = 0; i < 15; i++) {
			Particle chunk = Fx.soft("sigma_rock_chunk#4", Palette.SIG_ROCK_SMOKE);
			if (chunk != null) {
				Fx.add(chunk.at(at.add(out.scale(0.1))).velocity(cone(out, 0.56).scale(range(6, 8.4f))).gravity(12).drag(0.4f)
					.life(range(1.2f, 1.9f)).size(range(0.25f, 0.39f), 0.2f).spin(range(0, 6.28f), range(-13, 13)).alpha(0.95f)
					.fade(0.02f, 0.3f).nearFade(0.5f));
			}
		}
		Particle crater = Fx.soft("sigma_crater", enemy ? Palette.SIG_ROCK_SCORCH_ENEMY : Palette.SIG_ROCK_SCORCH);
		if (crater != null) {
			Fx.add(crater.at(at.add(out.scale(-0.13))).shape(Shape.FLAT).normal(out).life(3.9f).size(2.4f, 2.4f).spin(range(0, 6.28f), 0)
				.alpha(0.85f).fade(0.01f, 0.4f));
		}
	}

	// ---- Gravitic Flux ----

	/** His reticle's point from the server, each tick of its aim (his alone). */
	private static void reticleAt(Vec3 at) {
		reticle = at;
		reticleAt = Fx.now();
	}

	/** Where his reticle is now (client.SigmaSounds holds its loop there), or null. */
	public static @Nullable Vec3 reticle() {
		return reticle != null && Fx.now() - reticleAt < 0.2f ? reticle : null;
	}

	/**
	 * The reticle (2C9E at the aimed point, his alone): its ground decal, the rings 202A6 over the soft disc 9489 (SIG_FLUX_RETICLE_DECAL,
	 * HDR 10) the cylinder's 15 m across, the vortex ring 202B3 at its middle, and a faint band cylinder DE0 (SIG_FLUX_RETICLE: its
	 * size from the script, M; 3 m tall, ours).
	 */
	private static void reticle(Vec3 at) {
		float d = (float) SigmaStats.FLUX_RADIUS * 2;
		Vec3 ground = at.add(0, 0.05, 0);
		Fx.attach("sigma_reticle_rings", ground, () -> {
			Particle r = Fx.particle("sigma_flux_rings", Palette.SIG_FLUX_RETICLE_DECAL);
			return r == null ? null : r.shape(Shape.FLAT).normal(UP).life(0.3f).size(d, d).alpha(0.22f).fade(0.1f, 0.5f);
		});
		Fx.attach("sigma_reticle_vortex", ground.add(0, 0.01, 0), () -> {
			Particle r = Fx.particle("sigma_flux_vortex", Palette.SIG_FLUX_RETICLE_DECAL);
			return r == null ? null : r.shape(Shape.FLAT).normal(UP).life(0.3f).size(d * 0.35f, d * 0.35f).spin(0, 0.6f).alpha(0.25f)
				.fade(0.1f, 0.5f);
		});
		float[] band = scaled("brigitte_band", 0.2f);
		if (band != null) {
			Fx.attach("sigma_reticle_band", at, () -> {
				Particle r = Fx.particle("cat_band", Palette.SIG_FLUX_RETICLE);
				return r == null ? null : r.mesh(band, new Vec3(1, 0, 0)).life(0.3f).size(d, d).alpha(0.07f).fade(0.1f, 0.5f);
			});
		}
	}

	/**
	 * His glow through the ult (2C68 on hp 0076 Head, each phase): glows 4C09 x 4DB of 0.66-1.33 m 5 a second for 0.65-1 s
	 * (SIG_FLUX_BODY) and 2 m halos 2006C 5 a second for 1 s (SIG_FLUX_BODY_HALO, HDR 7). Not in his own view.
	 */
	private static void fluxBody(Player p, boolean enemy) {
		if (ownFirstPerson(p)) {
			return;
		}
		Vec3 head = pointOr(p, HEAD, 0, 0, 0);
		for (int i = per(5, 0.05f); i > 0; i--) {
			Particle glow = make("sigma_flux_glow", enemy, Palette.SIG_FLUX_BODY, Palette.SIG_FLUX_BODY_ENEMY);
			if (glow != null) {
				Fx.add(glow.at(head.add(randomUnit().scale(0.3))).velocity(randomUnit().scale(0.2)).life(range(0.65f, 1))
					.size(range(0.66f, 1.33f), 1).spin(range(0, 6.28f), range(-1, 1)).alpha(0.15f).fade(0.2f, 0.6f).nearFade(1));
			}
		}
		for (int i = per(5, 0.05f); i > 0; i--) {
			Particle halo = make("sigma_lens_line", enemy, Palette.SIG_FLUX_BODY_HALO, Palette.SIG_FLUX_BODY_HALO_ENEMY);
			if (halo != null) {
				Fx.add(halo.at(head).life(1).size(2, 2).spin(range(0, 6.28f), range(-0.5f, 0.5f)).alpha(0.06f).fade(0.2f, 0.6f).nearFade(1));
			}
		}
	}

	/**
	 * The cast (2C6B at the point, 1 s): a column cylinder DE0 (1C7A x 200E4) held 3 s (SIG_FLUX_COLUMN) and an outer one 6 s
	 * (SIG_FLUX_COLUMN_OUTER), the cylinder's 15 m across and 15 m tall from 5 m under the point (M: their size is the script's),
	 * a ground decal (SIG_FLUX_DECAL) and sparks rising at 10-12 m/s round its edge (64 a second + 32: SIG_FLUX_SPARK; ours 40).
	 */
	private static void fluxCast(Vec3 at, boolean enemy) {
		float d = (float) SigmaStats.FLUX_RADIUS * 2;
		float h = (float) SigmaStats.FLUX_HEIGHT;
		Vec3 base = at.subtract(0, SigmaStats.FLUX_BELOW, 0);
		float[] band = scaled("brigitte_band", h / d);
		if (band != null) {
			Particle column = make("cat_band", enemy, Palette.SIG_FLUX_COLUMN, Palette.SIG_FLUX_COLUMN_ENEMY);
			if (column != null) {
				Fx.add(column.at(base).mesh(band, new Vec3(1, 0, 0)).life(3).size(d, d).alpha(0.05f).fade(0.1f, 0.4f));
			}
			Particle outer = make("sigma_wisp", enemy, Palette.SIG_FLUX_COLUMN_OUTER, Palette.SIG_FLUX_COLUMN_OUTER_ENEMY);
			if (outer != null) {
				Fx.add(outer.at(base).mesh(band, new Vec3(0, 0, 1)).life(6).size(d * 1.02f, d * 1.02f).alpha(0.04f).fade(0.1f, 0.5f));
			}
		}
		Particle decal = make("sigma_flux_vortex", enemy, Palette.SIG_FLUX_DECAL, Palette.SIG_FLUX_DECAL_ENEMY);
		if (decal != null) {
			Fx.add(decal.at(at.add(0, 0.06, 0)).shape(Shape.FLAT).normal(UP).life(3).size(d, d).spin(0, 0.8f).alpha(0.25f).fade(0.1f, 0.5f));
		}
		for (int i = 0; i < 40; i++) {
			later(i / 40f, () -> {
				double a = Fx.RANDOM.nextFloat() * Math.PI * 2;
				Particle spark = make("spark#4", enemy, Palette.SIG_FLUX_SPARK, null);
				if (spark != null) {
					Fx.add(spark.at(at.add(Math.cos(a) * d / 2, 0.2, Math.sin(a) * d / 2)).shape(Shape.STREAK)
						.velocity(new Vec3(0, range(10, 12), 0)).life(range(0.17f, 0.43f)).size(0.25f, 0.08f).aspect(0.25f).fade(0.02f, 0.5f)
						.nearFade(0.5f));
				}
			});
		}
	}

	/** Targets taken (2C6A at the point): the cylinder swept upward at 20 m/s for 0.56 s (SIG_FLUX_PULL) and fast sparks (SIG_FLUX_PULL_SPARK). */
	private static void fluxTaken(Vec3 at, boolean enemy) {
		float d = (float) SigmaStats.FLUX_RADIUS * 2;
		float[] band = scaled("brigitte_band", 0.25f);
		Vec3 base = at.subtract(0, SigmaStats.FLUX_BELOW, 0);
		if (band != null) {
			Particle pull = make("cat_band", enemy, Palette.SIG_FLUX_PULL, Palette.SIG_FLUX_PULL_ENEMY);
			if (pull != null) {
				Fx.add(pull.at(base).mesh(band, new Vec3(1, 0, 0)).velocity(new Vec3(0, 20, 0)).life(0.56f).size(d, d).alpha(0.1f)
					.fade(0.05f, 0.5f));
			}
		}
		for (int i = 0; i < 40; i++) {
			double a = Fx.RANDOM.nextFloat() * Math.PI * 2, r = Math.sqrt(Fx.RANDOM.nextFloat()) * d / 2;
			Particle spark = make("spark#4", enemy, Palette.SIG_FLUX_PULL_SPARK, Palette.SIG_FLUX_PULL_SPARK_ENEMY);
			if (spark != null) {
				Fx.add(spark.at(at.add(Math.cos(a) * r, 0.2, Math.sin(a) * r)).shape(Shape.STREAK).velocity(new Vec3(0, range(23, 41), 0))
					.life(range(0.2f, 0.35f)).size(0.4f, 0.1f).aspect(0.15f).fade(0.02f, 0.5f).nearFade(0.5f));
			}
		}
	}

	/**
	 * A creature lifted (1DBC): at its start 2D88, three rings 11C1 x 3.0 for 0.22-0.27 s (SIG_LIFT_RING) and a 4 m glow; if it
	 * stood on the ground 2BEA, dust 14F9 of 2 m (SIG_LIFTOFF_DUST) and debris; then while lifted (lifted(), each tick).
	 */
	private static void lift(Entity e, Vec3 at, boolean ground, boolean enemy) {
		LIFTED.put(e.getId(), new Lift(Fx.now(), enemy, 2.2f));
		for (int i = 0; i < 3; i++) {
			Particle ring = make("juno_hit_ring#4", enemy, Palette.SIG_LIFT_RING, Palette.SIG_LIFT_RING_ENEMY);
			if (ring != null) {
				Fx.add(ring.at(at.add(0, (i - 1) * 0.5, 0)).shape(Shape.FLAT).normal(UP).life(range(0.22f, 0.27f)).size(1, 3).alpha(0.8f)
					.fade(0.02f, 0.5f).nearFade(0.5f));
			}
		}
		Particle glow = make("brigitte_soft_blob_b", enemy, Palette.SIG_LIFTED_GLOW, Palette.SIG_LIFTED_GLOW_ENEMY);
		if (glow != null) {
			Fx.add(glow.at(at).life(0.32f).size(range(4, 4.9f) * 0.5f, 1).alpha(0.4f).fade(0.05f, 0.6f).nearFade(1));
		}
		if (ground) {
			Vec3 feet = e.position().add(0, 0.1, 0);
			for (int i = 0; i < 6; i++) {
				Particle dust = Fx.soft("lw_thorn_flame#2", Palette.SIG_LIFTOFF_DUST);
				if (dust != null) {
					Fx.add(dust.at(feet).velocity(new Vec3(Fx.RANDOM.nextGaussian(), 0.3, Fx.RANDOM.nextGaussian()).normalize().scale(range(1, 2.5f)))
						.drag(0.2f).life(range(0.4f, 0.7f)).size(range(1.95f, 2.15f) * 0.5f, 1.4f).spin(range(0, 6.28f), 0).alpha(0.5f)
						.fade(0.05f, 0.6f).nearFade(1));
				}
			}
			for (int i = 0; i < 6; i++) {
				Particle bit = Fx.soft("sigma_rock_chunk#4", Palette.SIG_ROCK_SMOKE);
				if (bit != null) {
					Fx.add(bit.at(feet).velocity(new Vec3(Fx.RANDOM.nextGaussian() * 0.3, 1, Fx.RANDOM.nextGaussian() * 0.3).normalize()
						.scale(range(5, 8))).gravity(12).life(range(0.8f, 1.2f)).size(range(0.15f, 0.25f), 0.1f).spin(range(0, 6.28f), 8)
						.alpha(0.9f).fade(0.02f, 0.3f).nearFade(0.5f));
				}
			}
		}
	}

	/**
	 * Each lifted creature, each tick: 2BEB's ribbons following it (0.34 / 0.49 m wide, 0.23-0.29 s: SIG_LIFT_TRAIL and
	 * SIG_LIFT_TRAIL_DARK), and 2BE7 round it, rings 11C1 x 2-2.5 1.5-2 a second for 1-1.5 s (SIG_LIFTED_RING) and glows 174D8
	 * of 2-3.9 m 4 a second for 0.5 s (SIG_LIFTED_GLOW). Hyper Strike's target wears 2BE7 through its 0.8 s float.
	 */
	private static void lifted(Minecraft mc, float now) {
		for (Iterator<Map.Entry<Integer, Lift>> it = LIFTED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Lift> entry = it.next();
			Entity e = mc.level.getEntity(entry.getKey());
			Lift l = entry.getValue();
			if (e == null || !e.isAlive() || now - l.born > l.life) {
				it.remove();
				continue;
			}
			boolean enemy = l.enemy;
			Vec3 mid = e.position().add(0, e.getBbHeight() / 2, 0);
			boolean own = ownFirstPerson(e);
			if (!own) {
				Vec3 side = e.getViewVector(1).cross(UP);
				side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize().scale(e.getBbWidth() * 0.6);
				Fx.trail(-e.getId() * 4 - 8_000_001, mid.add(side), "cat_band", enemy ? Palette.SIG_LIFT_TRAIL_ENEMY : Palette.SIG_LIFT_TRAIL,
					0.34f, 0.26f, 0.7f, enemy);
				Fx.trail(-e.getId() * 4 - 8_000_002, mid.subtract(side), "cat_band",
					enemy ? Palette.SIG_LIFT_TRAIL_DARK_ENEMY : Palette.SIG_LIFT_TRAIL_DARK, 0.49f, 0.26f, 0.7f, true);
			}
			for (int i = per(1.75f, 0.05f); i > 0; i--) {
				Particle ring = make("juno_hit_ring#4", enemy, Palette.SIG_LIFTED_RING, Palette.SIG_LIFTED_RING_ENEMY);
				if (ring != null) {
					Fx.add(ring.at(mid).shape(Shape.FLAT).normal(UP).life(range(1, 1.5f)).size(range(2, 2.5f) * 0.5f, range(2, 2.5f))
						.alpha(own ? 0.25f : 0.6f).fade(0.1f, 0.6f).nearFade(own ? 0 : 0.5f));
				}
			}
			if (!own) {
				for (int i = per(4, 0.05f); i > 0; i--) {
					Particle glow = make("brigitte_soft_blob_b", enemy, Palette.SIG_LIFTED_GLOW, Palette.SIG_LIFTED_GLOW_ENEMY);
					if (glow != null) {
						Fx.add(glow.at(mid).life(0.5f).size(range(2, 3.9f) * 0.5f, 1.4f).alpha(0.25f).fade(0.1f, 0.6f).nearFade(1));
					}
				}
			}
		}
	}

	/** The slam lands (no look in the data: 2CA2 / 2CA3 are sounds, H): ours, a ring of dust and a small crater where it lands. */
	private static void slam(Vec3 at, boolean enemy) {
		for (int i = 0; i < 8; i++) {
			Particle dust = Fx.soft("lw_thorn_flame#2", Palette.SIG_LIFTOFF_DUST);
			if (dust != null) {
				Fx.add(dust.at(at.add(0, 0.2, 0)).velocity(new Vec3(Fx.RANDOM.nextGaussian(), 0.1, Fx.RANDOM.nextGaussian()).normalize()
					.scale(range(2, 4))).drag(0.1f).life(range(0.4f, 0.7f)).size(1, 1.8f).spin(range(0, 6.28f), 0).alpha(0.5f)
					.fade(0.05f, 0.6f).nearFade(1));
			}
		}
		Particle crater = Fx.soft("sigma_crater", enemy ? Palette.SIG_ROCK_SCORCH_ENEMY : Palette.SIG_ROCK_SCORCH);
		if (crater != null) {
			Fx.add(crater.at(at.add(0, 0.03, 0)).shape(Shape.FLAT).normal(UP).life(2).size(1.6f, 1.6f).spin(range(0, 6.28f), 0).alpha(0.7f)
				.fade(0.01f, 0.4f));
		}
	}

	// ---- perks ----

	/** Levitation (65BF on hp 0001, 3 a second for 1.1 s): violet streaks rising round him, spinning (SIG_LEVITATE; mesh 963D as quads). */
	private static void levitate(Player p, boolean enemy) {
		boolean own = ownFirstPerson(p);
		for (int i = per(own ? 1.5f : 3, 0.05f); i > 0; i--) {
			Particle streak = make("sigma_levitate", enemy, Palette.SIG_LEVITATE, Palette.SIG_LEVITATE_ENEMY);
			if (streak != null) {
				double a = Fx.RANDOM.nextFloat() * Math.PI * 2;
				Fx.add(streak.at(p.position().add(Math.cos(a) * 0.7, range(0, 1), Math.sin(a) * 0.7)).shape(Shape.STREAK)
					.velocity(new Vec3(0, range(1.5f, 2.5f), 0)).life(1.1f).size(range(0.66f, 0.78f) * 1.5f, 0.5f).aspect(0.15f)
					.alpha(own ? 0.2f : 0.45f).fade(0.2f, 0.6f).nearFade(own ? 0 : 0.8f));
			}
		}
	}

	/** Hyper Strike charged (65C7 at his left wrist, hp 0002): glows 4C09 of 0.5-0.94 m 20 a second (SIG_HYPER_STRIKE), sparks. */
	private static void strikeCharge(Player p, boolean enemy) {
		if (ownFirstPerson(p)) {
			return;
		}
		Vec3 wrist = pointOr(p, WRIST, 0.5, -0.4, -0.5);
		Particle glow = make("cat_soft", enemy, Palette.SIG_HYPER_STRIKE, Palette.SIG_HYPER_STRIKE_ENEMY);
		if (glow != null) {
			Fx.add(glow.at(wrist).life(range(0.28f, 0.41f)).size(range(0.5f, 0.94f) * 0.6f, 0.3f).alpha(0.5f).fade(0.1f, 0.6f).nearFade(0.5f));
		}
		if (Fx.RANDOM.nextFloat() < 0.6f) {
			Particle spark = make("sigma_wisp_ring", enemy, Palette.SIG_HYPER_STRIKE_SPARK, Palette.SIG_HYPER_STRIKE_SPARK_ENEMY);
			if (spark != null) {
				Fx.add(spark.at(wrist.add(randomUnit().scale(0.1))).life(0.3f).size(0.2f, 0.05f).spin(range(0, 6.28f), 4).alpha(0.7f)
					.fade(0.1f, 0.5f).nearFade(0.3f));
			}
		}
	}

	// ---- the server's moments ----

	/** His moments from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Minecraft mc = Minecraft.getInstance();
		Entity entity = mc.level == null || e.entity() < 0 ? null : mc.level.getEntity(e.entity());
		switch (e.effect()) {
			case "sigma_volley" -> volley(entity, enemy);
			case "sigma_burst" -> burst(at, enemy);
			case "sigma_absorb" -> absorb(at, dir, enemy);
			case "sigma_overhealth" -> overhealth(entity, enemy);
			case "sigma_rock_impact" -> rockImpact(at, dir, enemy);
			case "sigma_barrier_break" -> breakApart(at, dir, e.variant() == 1, enemy);
			case "sigma_barrier_recall" -> recall(at, dir, entity, enemy);
			case "sigma_barrier_heal" -> barrierHeal(at, dir, enemy);
			case "sigma_reticle" -> reticleAt(at);
			case "sigma_flux_cast" -> fluxCast(at, enemy);
			case "sigma_flux_taken" -> fluxTaken(at, enemy);
			case "sigma_lift" -> {
				if (entity != null) {
					lift(entity, at, e.variant() == 1, enemy);
				}
			}
			case "sigma_slam" -> slam(at, enemy);
			// (Hyper Strike's float cleansed: its lifted look goes with it, Sleep.cleanse)
			case "sigma_strike_end" -> LIFTED.remove(e.entity());
			case "sigma_strike" -> {
				if (entity != null) {
					LIFTED.put(entity.getId(), new Lift(Fx.now(), enemy, (float) SigmaStats.HYPER_STRIKE_FLOAT));
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	public static void clear() {
		POSED.clear();
		HIS.clear();
		LIFTED.clear();
		PENDING.clear();
		SEEN.clear();
		MOTES.clear();
		reticle = null;
	}
}
