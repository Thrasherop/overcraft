package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.EffectPayload;
import dev.overcraft.torbjorn.Torbjorn;
import dev.overcraft.torbjorn.TorbjornStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Torbjorn's effects (tools/reference/torbjorn-effects.md, read from his effects' data, 2026-11-17; #150, the owner: "torb refuel
 * lava", "torb lava floor", his shots still Minecraft's). His fire is orange-gold to his side, a redder twin to his enemies (H rows,
 * M roles). Overload's vent flames (26F1) and Molten Core's held forge glow (26E5) are drawn each frame from his synced look
 * (Torbjorn.LOOK_*) at his posed hardpoints; the slag (2509) and the rivets (2653 / 1865) from their renderers; the pools (2754)
 * kept here from one message for their life; the rest from the server's messages. Sizes in metres, times in seconds; each says
 * where it came from: H read in the data, M inferred, ours where the data gives nothing or we drew it smaller.
 */
public final class TorbjornFx {
	/** Its art, made ahead as the hero is picked (HeroFxArt). */
	public static final List<EffectArt.Art> ART = List.of(
		new EffectArt.Art("torb_flame", Palette.TB_OVERLOAD, false),
		new EffectArt.Art("torb_flame", Palette.TB_OVERLOAD_B, false),
		new EffectArt.Art("smoke#48", Palette.TB_OVERLOAD_SMOKE, true),
		new EffectArt.Art("spark#4", Palette.TB_OVERLOAD_SPARK, false),
		new EffectArt.Art("brigitte_soft_blob_b", Palette.TB_MOLTEN_GLOW, false),
		new EffectArt.Art("torb_flame", Palette.TB_MOLTEN_FLAME, false),
		new EffectArt.Art("spark#4", Palette.TB_MOLTEN_SPARK, false),
		new EffectArt.Art("torb_flame", Palette.TB_SLAG_LAUNCH, false),
		new EffectArt.Art("cat_soft", Palette.TB_SLAG_LAUNCH, false),
		new EffectArt.Art("brigitte_soft_blob_b", Palette.TB_SLAG_GLOW, false),
		new EffectArt.Art("torb_ribbon_noise", Palette.TB_SLAG_GLOW, false),
		new EffectArt.Art("torb_ribbon_noise", Palette.TB_SLAG_TRAIL, false),
		new EffectArt.Art("glow", Palette.TB_SLAG_GLOW, false),
		new EffectArt.Art("spark#4", Palette.TB_SLAG_DRIP, false),
		new EffectArt.Art("torb_slag_hit", Palette.TB_RIVET_SPARK, false),
		new EffectArt.Art("torb_lava_crust", Palette.TB_POOL_LAVA, false),
		new EffectArt.Art("cat_soft", Palette.TB_POOL_EMBER, true),
		new EffectArt.Art("sigma_disc", Palette.TB_POOL_EMBER, false),
		new EffectArt.Art("torb_pool_fire", Palette.TB_POOL_FIRE, false),
		new EffectArt.Art("torb_pool_flash", Palette.TB_POOL_FLASH, false),
		new EffectArt.Art("torb_dome_noise", Palette.TB_POOL_FLASH, false),
		new EffectArt.Art("torb_flame", Palette.TB_BURN, false),
		new EffectArt.Art("torb_flame", Palette.TB_BURN_B, false),
		new EffectArt.Art("torb_burn_ring", Palette.TB_BURN_RING, false),
		new EffectArt.Art("spark#4", Palette.TB_BURN, false),
		new EffectArt.Art("cat_noise", Palette.TB_RIVET, false),
		new EffectArt.Art("torb_trail", Palette.TB_RIVET_TRAIL, false),
		new EffectArt.Art("spark#4", Palette.TB_RIVET_SPARK, false),
		new EffectArt.Art("cat_noise", Palette.TB_RIVET, false),
		new EffectArt.Art("torb_ribbon", Palette.TB_PELLET_TRAIL, false),
		new EffectArt.Art("torb_star#4", Palette.TB_MUZZLE_B, false),
		new EffectArt.Art("torb_flash#4", Palette.TB_MUZZLE, false),
		new EffectArt.Art("cat_burst#4", Palette.TB_MUZZLE, false),
		new EffectArt.Art("torb_smoke_puff", Palette.TB_MUZZLE_SMOKE, true),
		new EffectArt.Art("torb_flash#4", Palette.TB_MUZZLE, false),
		new EffectArt.Art("cat_burst#4", Palette.TB_MUZZLE, false),
		new EffectArt.Art("cat_soft", Palette.TB_RIVET_HIT, false),
		new EffectArt.Art("torb_hit_puff#4", Palette.TB_PELLET_HIT, false),
		new EffectArt.Art("torb_hit_smoke", Palette.TB_MUZZLE_SMOKE, true),
		new EffectArt.Art("torb_reload_glow", Palette.TB_RELOAD_GLOW, false),
		new EffectArt.Art("glow", Palette.TB_RELOAD_GLOW_B, false),
		new EffectArt.Art("cat_puff", Palette.TB_RELOAD_3P, false),
		new EffectArt.Art("spark#4", Palette.TB_RELOAD_GLOW, false),
		new EffectArt.Art("sombra_tracer", Palette.TB_TURRET_LINE, false),
		new EffectArt.Art("cat_soft", Palette.TB_TURRET_FLASH, false),
		new EffectArt.Art("torb_flash#4", Palette.TB_TURRET_FLASH, false),
		new EffectArt.Art("smoke#48", Palette.TB_TURRET_SMOKE, true),
		new EffectArt.Art("smoke#48", Palette.TB_OVERLOAD_SMOKE, true),
		new EffectArt.Art("dva_solid", Palette.TB_THROWN, false));
	public static final List<EffectArt.Art> ART_ENEMY = List.of(
		new EffectArt.Art("torb_flame", Palette.TB_OVERLOAD_ENEMY, false),
		new EffectArt.Art("torb_flame", Palette.TB_OVERLOAD_B_ENEMY, false),
		new EffectArt.Art("brigitte_soft_blob_b", Palette.TB_MOLTEN_GLOW_ENEMY, false),
		new EffectArt.Art("torb_flame", Palette.TB_MOLTEN_FLAME_ENEMY, false),
		new EffectArt.Art("spark#4", Palette.TB_MOLTEN_SPARK_ENEMY, false),
		new EffectArt.Art("torb_flame", Palette.TB_SLAG_LAUNCH_ENEMY, false),
		new EffectArt.Art("cat_soft", Palette.TB_SLAG_LAUNCH_ENEMY, false),
		new EffectArt.Art("brigitte_soft_blob_b", Palette.TB_SLAG_GLOW_ENEMY, false),
		new EffectArt.Art("torb_ribbon_noise", Palette.TB_SLAG_GLOW_ENEMY, false),
		new EffectArt.Art("torb_ribbon_noise", Palette.TB_SLAG_TRAIL_ENEMY, false),
		new EffectArt.Art("glow", Palette.TB_SLAG_GLOW_ENEMY, false),
		new EffectArt.Art("torb_lava_crust", Palette.TB_POOL_LAVA_ENEMY, false),
		new EffectArt.Art("torb_pool_fire", Palette.TB_POOL_FIRE_ENEMY, false),
		new EffectArt.Art("torb_pool_flash", Palette.TB_POOL_FLASH_ENEMY, false),
		new EffectArt.Art("torb_dome_noise", Palette.TB_POOL_FLASH_ENEMY, false),
		new EffectArt.Art("torb_ribbon", Palette.TB_PELLET_TRAIL_ENEMY, false),
		new EffectArt.Art("torb_star#4", Palette.TB_MUZZLE_B_ENEMY, false),
		new EffectArt.Art("torb_flash#4", Palette.TB_MUZZLE_ENEMY, false),
		new EffectArt.Art("cat_burst#4", Palette.TB_MUZZLE_ENEMY, false),
		new EffectArt.Art("torb_flash#4", Palette.TB_PELLET_MUZZLE_ENEMY, false),
		new EffectArt.Art("cat_burst#4", Palette.TB_PELLET_MUZZLE_ENEMY, false));

	/**
	 * His posed points (HeroModelRenderer): the Rivet Gun's muzzle hp 0006 (its weapon model's), then his own: the forge's two vents
	 * 0029 / 0032 (on his back: Overload's and Molten Core's flames), his forge arm 0075 (the slag's glow), his hands 000A / 000B
	 * (the reload's glow: 000A in his first person, 000B as others see him) (H positions from his model's socket table, M roles).
	 */
	public static final String[] POINTS = {"hardpoint_0006", "hardpoint_0029", "hardpoint_0032", "hardpoint_0075", "hardpoint_000A",
		"hardpoint_000B"};
	/** The first of POINTS that are on his body (the rest is on his gun). */
	public static final int GUN_POINTS = 1;
	private static final int MUZZLE = 0, VENT_L = 1, VENT_R = 2, FORGE = 3, HAND = 5;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	private static final class Posed {
		Vec3[] points = new Vec3[0];
		long tick;
	}

	private static final Map<Integer, Posed> POSED = new HashMap<>();

	/** An emitter's rate kept between frames: how many to make this frame for so many a second. */
	private static final class Rate {
		float last = -1;
		float carry = Fx.RANDOM.nextFloat();

		int take(float perSecond) {
			float now = Fx.now();
			float dt = last < 0 ? 0 : Math.clamp(now - last, 0, 0.25f);
			last = now;
			carry += perSecond * dt;
			int n = (int) carry;
			carry -= n;
			return n;
		}
	}

	/** Each Torbjorn's emitters (Overload's and Molten Core's), by entity id. */
	private static final Map<Integer, Rate[]> RATES = new HashMap<>();

	/** A pool of slag: its middle on the ground, when it came (Fx.now()), its life (s), an enemy's, its blobs' layout and emitters. */
	private static final class Pool {
		final int id;
		final Vec3 at;
		final float born, life;
		final boolean enemy;
		final Vec3[] blobs;
		final float[] sizes;
		final Vec3[] sides;
		final String[] meshes;
		final Rate[] embers, sparks;

		Pool(int id, Vec3 at, float born, float life, boolean enemy) {
			this.id = id;
			this.at = at;
			this.born = born;
			this.life = life;
			this.enemy = enemy;
			// the blobs over the 2.4 m disc: one in the middle, 5 at 1 m, 8 at 1.85 m (ours: the data's layout is a data-flow shape
			// not decoded; its seven read sparse over the disc), each a little off its place, turned at random
			int n = 1 + 5 + 8;
			blobs = new Vec3[n];
			sizes = new float[n];
			sides = new Vec3[n];
			meshes = new String[n];
			for (int i = 0; i < n; i++) {
				double r = i == 0 ? 0 : i < 6 ? 1.0 : 1.85;
				double a = i == 0 ? 0 : i < 6 ? (i - 1) * Math.PI * 2 / 5 : (i - 6) * Math.PI * 2 / 8 + 0.4;
				a += (Fx.RANDOM.nextFloat() - 0.5f) * 0.4f;
				r += (Fx.RANDOM.nextFloat() - 0.5f) * 0.3f;
				blobs[i] = at.add(Math.cos(a) * r, 0, Math.sin(a) * r);
				// the blob meshes are 1.1-1.5 m across; the data's size 0.65 of them (H) read as gaps on the floor: ours 1.0-1.3 m wide
				sizes[i] = range(1.0f, 1.3f);
				double turn = Fx.RANDOM.nextFloat() * Math.PI * 2;
				sides[i] = new Vec3(Math.cos(turn), 0, Math.sin(turn));
				meshes[i] = BLOBS[i % BLOBS.length];
			}
			embers = new Rate[] {new Rate()};
			sparks = new Rate[] {new Rate()};
		}
	}

	/** The pool's blob meshes 887F / 83F7 / 885C (2754, H). */
	private static final String[] BLOBS = {"torb_lava_a", "torb_lava_b_mesh", "torb_lava_c"};
	/** How far each blob sits into the ground, of its width (ours: their meshes' origins are 0.23-0.34 of the width up; set lower, flatter). */
	private static final float BLOB_SINK = 0.42f;
	private static final List<Pool> POOLS = new ArrayList<>();
	private static int poolSerial;

	/** A burning enemy (254F): until when (Fx.now()), whose pool (an enemy's), its emitters. */
	private static final class Burn {
		float until;
		boolean enemy;
		final Rate flames = new Rate(), flamesB = new Rate(), sparks = new Rate();
	}

	private static final Map<Integer, Burn> BURNING = new HashMap<>();
	/** A burn is held this long past its last step (ours: the server's step is 0.2 s). */
	private static final float BURN_HOLD = 0.3f;

	/** A reload: whose, when it started, its length (s), an enemy's, what it's drawn yet (bits). */
	private static final class Reload {
		final int entity;
		final float start, length;
		final boolean enemy;
		int done;

		Reload(int entity, float start, float length, boolean enemy) {
			this.entity = entity;
			this.start = start;
			this.length = length;
			this.enemy = enemy;
		}
	}

	private static final List<Reload> RELOADS = new ArrayList<>();

	/** A turret's emitters (its damaged smoke), by entity id. */
	private static final Map<Integer, Rate> TURRET_SMOKE = new HashMap<>();

	private TorbjornFx() {
	}

	// ---- his posed points (HeroModelRenderer) ----

	public static void posed(Entity torbjorn, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(torbjorn.getId(), id -> new Posed());
		p.points = points;
		p.tick = torbjorn.level().getGameTime();
	}

	/** His posed point this tick or the last (null in his own first person, unposed, or without that point). */
	private static @Nullable Vec3 point(@Nullable Entity torbjorn, int which) {
		if (torbjorn == null || ownFirstPerson(torbjorn)) {
			return null;
		}
		Posed p = POSED.get(torbjorn.getId());
		return p == null || torbjorn.level().getGameTime() - p.tick > 1 || which >= p.points.length ? null : p.points[which];
	}

	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/** His muzzle: in his own first person the view model's as drawn, else posed (hp 0006), else where the server shot from. */
	private static Vec3 muzzle(@Nullable Entity torbjorn, Vec3 fallback) {
		Minecraft mc = Minecraft.getInstance();
		if (ownFirstPerson(torbjorn)) {
			Vector3f view = dev.overcraft.client.model.HeroModelRenderer.viewMuzzle();
			return view == null ? fallback : mc.gameRenderer.mainCamera().position().add(view.x, view.y, view.z);
		}
		Vec3 posed = point(torbjorn, MUZZLE);
		return posed != null ? posed : fallback;
	}

	// ---- each frame ----

	/** Each frame: every Torbjorn's Overload and Molten Core, the pools, the burning, the reloads. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			POOLS.clear();
			BURNING.clear();
			RELOADS.clear();
			return;
		}
		float now = Fx.now();
		POOLS.removeIf(p -> now - p.born > p.life || now < p.born - 1);
		for (Pool p : POOLS) {
			pool(p, now - p.born);
		}
		BURNING.entrySet().removeIf(b -> {
			Entity e = mc.level.getEntity(b.getKey());
			if (e == null || !e.isAlive() || now > b.getValue().until || now < b.getValue().until - 5) {
				return true;
			}
			burning(e, b.getValue());
			return false;
		});
		RELOADS.removeIf(r -> {
			Entity e = mc.level.getEntity(r.entity);
			float t = now - r.start;
			if (e == null || t > r.length || t < -1) {
				return true;
			}
			reload(e, r, t);
			return false;
		});
		RATES.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		TURRET_SMOKE.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		for (Player p : mc.level.players()) {
			Integer look = p.getAttached(HeroManager.TORBJORN_LOOK);
			if (look == null || look == 0 || !p.isAlive() || ownFirstPerson(p)) {
				continue;
			}
			boolean enemy = KirikoFx.enemyOf(p);
			Rate[] rates = RATES.computeIfAbsent(p.getId(), id -> new Rate[] {new Rate(), new Rate(), new Rate(), new Rate(), new Rate(),
				new Rate(), new Rate(), new Rate(), new Rate(), new Rate(), new Rate(), new Rate()});
			if ((look & Torbjorn.LOOK_OVERLOAD) != 0) {
				overload(p, rates, enemy);
			}
			if ((look & Torbjorn.LOOK_MOLTEN) != 0) {
				molten(p, rates, enemy);
			}
		}
	}

	/** His vents (posed), else ours: 1.48 m up, 0.38 m back, 0.28 m either side (his socket table's rest, H). */
	private static Vec3 vent(Player p, int which) {
		Vec3 posed = point(p, which);
		if (posed != null) {
			return posed;
		}
		Vec3 fwd = Vec3.directionFromRotation(0, p.getVisualRotationYInDegrees());
		Vec3 side = fwd.cross(UP).normalize();
		return p.position().add(0, 1.48, 0).subtract(fwd.scale(0.38)).add(side.scale(which == VENT_L ? -0.28 : 0.28));
	}

	/**
	 * Overload's forge roaring (26F1, H) at both vents for its 6 s: flame tongues 1CAFA, 15 -> 10 a second each (ours 14), 0.28 +-
	 * 0.06 s, 0.42-0.66 m, stretched x2 -> x4 along their path, rising 1.5 -> 0.5 m/s; the second layer at 12 -> 6 a second (ours 7);
	 * heat smoke 377D5 8 -> 3 a second (ours 5), 0.36-1.67 s, rising 3.6-5 m/s (ours 2: at the data's speed it streaked off his back
	 * as a column), 0.19-0.91 m; sparks 1F35 about 6 a second, 0.51-0.76 s, 0.03-0.08 m, about 1.4 m/s in a 25 degree cone.
	 */
	private static void overload(Player p, Rate[] rates, boolean enemy) {
		for (int k = 0; k < 2; k++) {
			Vec3 at = vent(p, k == 0 ? VENT_L : VENT_R);
			int flames = rates[k].take(14);
			for (int i = 0; i < flames; i++) {
				flame(at, enemy ? Palette.TB_OVERLOAD_ENEMY : Palette.TB_OVERLOAD, range(0.42f, 0.66f), range(1.0f, 1.5f), 0.28f, 1);
			}
			int second = rates[2 + k].take(7);
			for (int i = 0; i < second; i++) {
				flame(at, enemy ? Palette.TB_OVERLOAD_B_ENEMY : Palette.TB_OVERLOAD_B, range(0.35f, 0.55f), range(0.8f, 1.2f), 0.28f, 0.8f);
			}
			int smoke = rates[4 + k].take(5);
			for (int i = 0; i < smoke; i++) {
				Particle s = Fx.soft("smoke#48", Palette.TB_OVERLOAD_SMOKE);
				if (s != null) {
					Fx.add(s.at(at.add(0, 0.2, 0)).velocity(cone(UP, 15).scale(2)).life(range(0.6f, 1.4f)).size(range(0.19f, 0.4f),
						range(0.6f, 0.91f)).alpha(0.4f).spin(range(0, 6.28f), range(-1, 1)).loop(48).fade(0.1f, 0.6f).nearFade(1));
				}
			}
			int sparks = rates[6 + k].take(6);
			for (int i = 0; i < sparks; i++) {
				spark(at, cone(UP, 25).scale(range(1.2f, 1.6f)), Palette.TB_OVERLOAD_SPARK, range(0.51f, 0.76f), range(0.03f, 0.08f), 0);
			}
		}
	}

	/** One flame tongue rising from a point: width w, stretched x(stretch) along its rise, living life +- a fifth. */
	private static void flame(Vec3 at, Palette palette, float w, float stretch, float life, float alpha) {
		Particle f = Fx.particle("torb_flame", palette);
		if (f == null) {
			return;
		}
		float len = w * stretch * 2;
		float rise = range(0.5f, 1.5f);
		Vec3 dir = cone(UP, 10);
		// its bright foot at the vent: the quad's middle a third of its length up
		Fx.add(f.at(at.add(dir.scale(len * 0.3f))).shape(Shape.STREAK).velocity(dir.scale(rise)).life(life * range(0.8f, 1.2f))
			.size(len * 0.7f, len).aspect(w / len).alpha(alpha).fade(0.1f, 0.5f).nearFade(0.8f));
	}

	/** One spark streak: its mask 1F35 (4 frames), stretched along its flight. */
	private static void spark(Vec3 at, Vec3 velocity, Palette palette, float life, float size, float gravity) {
		Particle s = Fx.particle("spark#4", palette);
		if (s != null) {
			Fx.add(s.at(at).shape(Shape.STREAK).velocity(velocity).gravity(gravity).life(life).size(size * 3, size * 2).aspect(0.35f)
				.fade(0.05f, 0.5f).nearFade(0.5f));
		}
	}

	/**
	 * Molten Core held (26E5, H; authored 3 s, looped for the ult, M) at his forge arm 0075 and the vents: the glow 174D8 0.42-0.5 m,
	 * 9 a second + 3, 0.1-0.25 s; flames 1CAFA 22 a second (a third here: ours), 0.41-0.59 m, 0.22-0.34 s; sparks 1F35 8-18 a second,
	 * 3-4 m/s, stretched x5.5, 0.29-0.74 s; at each vent the Overload flame at half its rate (ours).
	 */
	private static void molten(Player p, Rate[] rates, boolean enemy) {
		Vec3 forge = point(p, FORGE);
		if (forge == null) {
			Vec3 fwd = Vec3.directionFromRotation(0, p.getVisualRotationYInDegrees());
			// ours, from his socket table's rest: 0.75 m to his left, 0.58 m up his body's middle
			forge = p.position().add(0, 0.9, 0).add(fwd.cross(UP).normalize().scale(-0.75 * 0.6));
		}
		Palette glowColour = enemy ? Palette.TB_MOLTEN_GLOW_ENEMY : Palette.TB_MOLTEN_GLOW;
		Palette flameColour = enemy ? Palette.TB_MOLTEN_FLAME_ENEMY : Palette.TB_MOLTEN_FLAME;
		Palette sparkColour = enemy ? Palette.TB_MOLTEN_SPARK_ENEMY : Palette.TB_MOLTEN_SPARK;
		int glows = rates[8].take(9);
		for (int i = 0; i < glows; i++) {
			Particle g = Fx.particle("brigitte_soft_blob_b", glowColour);
			if (g != null) {
				Fx.add(g.at(forge).life(range(0.1f, 0.25f)).size(range(0.42f, 0.5f), 0.45f).alpha(0.7f).spin(range(0, 6.28f), 0)
					.fade(0.1f, 0.5f).nearFade(0.8f));
			}
		}
		int flames = rates[9].take(7);
		for (int i = 0; i < flames; i++) {
			flame(forge, flameColour, range(0.2f, 0.3f), range(0.6f, 1), range(0.22f, 0.34f), 1);
		}
		int sparks = rates[10].take(12);
		for (int i = 0; i < sparks; i++) {
			spark(forge, new Vec3(Fx.RANDOM.nextGaussian(), Math.abs(Fx.RANDOM.nextGaussian()), Fx.RANDOM.nextGaussian()).normalize()
				.scale(range(3, 4)), sparkColour, range(0.29f, 0.74f), range(0.03f, 0.05f), 4);
		}
		int vents = rates[11].take(7);
		for (int i = 0; i < vents; i++) {
			flame(vent(p, Fx.RANDOM.nextBoolean() ? VENT_L : VENT_R), flameColour, range(0.3f, 0.45f), range(0.8f, 1.2f), 0.28f, 0.9f);
		}
	}

	// ---- the slag ----

	/**
	 * The slag in flight (2509, H), fed each frame from its renderer: its glow 174D8 (1.0 m in the data, 0.6 here: ours, the slag is
	 * 0.2 m), its blob mesh E20D (0.38 m; ours 0.3), its drips 1F35 120-160 a second (ours 24), 0.1-0.18 m, 0.35-0.64 s, falling; glows
	 * A7B6 left behind 30 a second (ours 10), 0.26-0.99 m, 0.19 s; its ribbon E71 (about 1 m wide in the data, 0.5 here: ours), 0.26 s.
	 * Whether it drew itself (else the item stands in).
	 */
	public static boolean slagFlight(int id, Vec3 at, Vec3 velocity, float dt, boolean enemy) {
		Palette glowColour = enemy ? Palette.TB_SLAG_GLOW_ENEMY : Palette.TB_SLAG_GLOW;
		Particle glow = Fx.attach("torb_slag/" + id + "/glow", at, () -> {
			Particle s = Fx.particle("brigitte_soft_blob_b", glowColour);
			return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f).nearFade(1);
		});
		if (glow != null) {
			glow.at(at).size(0.6f, 0.6f).alpha(0.8f);
		}
		float[] blob = Fx.mesh("torb_slag_blob");
		boolean drawn = false;
		if (blob != null) {
			final float size = 0.3f;
			Particle b = Fx.attach("torb_slag/" + id + "/blob", at, () -> {
				Particle s = Fx.particle("torb_ribbon_noise", glowColour);
				return s == null ? null : s.life(0.1f).fade(0.01f, 0.5f);
			});
			if (b != null) {
				// the mesh stands on its foot: set down by half its height (it's about as tall as wide), tumbling as it flies (ours)
				float turn = Fx.now() * 6 + id;
				b.mesh(blob, new Vec3(Math.cos(turn), 0, Math.sin(turn))).at(at.subtract(0, size / 2, 0)).size(size, size);
				drawn = true;
			}
		}
		Fx.trail(id, at, "torb_ribbon_noise", enemy ? Palette.TB_SLAG_TRAIL_ENEMY : Palette.TB_SLAG_TRAIL, 0.5f, 0.26f, 0.8f);
		Fx.trailNearFade(id, 2);
		int drips = Math.round(24 * dt + Fx.RANDOM.nextFloat() * 0.5f);
		for (int i = 0; i < drips; i++) {
			Vec3 off = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian()).scale(0.06);
			spark(at.add(off), new Vec3(0, -1, 0).add(off.scale(4)), Palette.TB_SLAG_DRIP, range(0.35f, 0.64f), range(0.05f, 0.09f), 6);
		}
		int glows = Math.round(10 * dt + Fx.RANDOM.nextFloat() * 0.5f);
		for (int i = 0; i < glows; i++) {
			Particle g = Fx.particle("glow", glowColour);
			if (g != null) {
				Fx.add(g.at(at).life(0.19f).size(range(0.26f, 0.6f), 0.2f).alpha(0.6f).fade(0.05f, 0.6f).nearFade(1.5f));
			}
		}
		return drawn;
	}

	/** The slag's launch (26CC, H): its flare 1CB5D x 1CAFA 0.7-1.0 m stretched x4 along the shot, 0.18-0.21 s; a glow 0.26-0.78 m. */
	private static void slagLaunch(@Nullable Entity torbjorn, Vec3 at, Vec3 dir, boolean enemy) {
		boolean own = ownFirstPerson(torbjorn);
		float k = own ? 0.3f : 1;
		Vec3 from = own ? muzzle(torbjorn, at) : at;
		Vec3 fwd = dir.lengthSqr() < 1.0E-6 ? UP : dir.normalize();
		Palette colour = enemy ? Palette.TB_SLAG_LAUNCH_ENEMY : Palette.TB_SLAG_LAUNCH;
		Particle flare = Fx.particle("torb_flame", colour);
		if (flare != null) {
			float len = range(0.7f, 1.0f) * 2 * k;
			Fx.add(flare.at(from.add(fwd.scale(len * 0.4f))).shape(Shape.STREAK).velocity(fwd.scale(2)).life(range(0.18f, 0.21f))
				.size(len, len).aspect(0.25f).fade(0.02f, 0.6f).nearFade(own ? 0.2f : 0.8f));
		}
		Particle glow = Fx.particle("cat_soft", colour);
		if (glow != null) {
			Fx.add(glow.at(from).life(range(0.15f, 0.27f)).size(range(0.26f, 0.78f) * k, 0.3f * k).alpha(0.8f).fade(0.02f, 0.6f)
				.nearFade(own ? 0.2f : 0.8f));
		}
	}

	/** Where a slag lands (26B7, H): the flash 2A81E 0.3-0.8 m, 0.06-0.17 s, twice; sparks 1F35 (15-17, ours 8) up to 4 m/s, 1-1.5 s. */
	private static void slagHit(Vec3 at) {
		for (int i = 0; i < 2; i++) {
			Particle flash = Fx.particle("torb_slag_hit", Palette.TB_RIVET_SPARK);
			if (flash != null) {
				Fx.add(flash.at(at.add(0, 0.1, 0)).life(range(0.06f, 0.17f)).size(range(0.3f, 0.8f), 0.4f).spin(range(0, 6.28f), 0)
					.fade(0.02f, 0.5f).nearFade(0.5f));
			}
		}
		for (int i = 0; i < 8; i++) {
			spark(at.add(0, 0.1, 0), cone(UP, 70).scale(range(2, 4)), Palette.TB_RIVET_SPARK, range(1, 1.5f), range(0.05f, 0.08f), 9.8f);
		}
	}

	/**
	 * Molten Core's cast burst 34B0 at his forge arm 0.27 s in (H): 64 sparks 1F35 at 4-7 m/s (ours half), 0.29-0.74 s; the line flare
	 * 2006C 2 m, 0.28 s; the hemisphere 963D (2 x 1 m) at 0.9, 0.19 s, along his aim; the flare 210FD 2 m, 0.15 s.
	 */
	private static void ultCast(@Nullable Entity torbjorn, Vec3 at, Vec3 aim, boolean enemy) {
		Vec3 forge = point(torbjorn, FORGE);
		Vec3 c = forge != null ? forge : at;
		boolean own = ownFirstPerson(torbjorn);
		float k = own ? 0.4f : 1;
		Palette spark = enemy ? Palette.TB_MOLTEN_SPARK_ENEMY : Palette.TB_MOLTEN_SPARK;
		Palette colour = enemy ? Palette.TB_SLAG_LAUNCH_ENEMY : Palette.TB_SLAG_LAUNCH;
		for (int i = 0; i < 32; i++) {
			Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian()).normalize();
			spark(c, dir.scale(range(4, 7)), spark, range(0.29f, 0.74f), range(0.047f, 0.067f), 4);
		}
		Particle line = Fx.particle("sigma_lens_line", colour);
		if (line != null) {
			// a horizontal flare: its height 0.3 m, its width 2 m
			Fx.add(line.at(c).life(0.28f).size(0.3f * k, 0.3f * k).aspect(2 / 0.3f).alpha(0.8f).fade(0.02f, 0.6f).nearFade(own ? 0.3f : 1));
		}
		Particle flare = Fx.particle("lucio_glow", colour);
		if (flare != null) {
			Fx.add(flare.at(c).life(0.15f).size(2 * k, 1.5f * k).alpha(0.7f).fade(0.02f, 0.6f).nearFade(own ? 0.3f : 1));
		}
		float[] dome = Fx.mesh("torb_hemisphere");
		Particle h = dome == null || own ? null : Fx.particle("cat_soft", colour);
		if (h != null) {
			// its dome along his aim (the mesh's Z: side x up), its foot set back by half its height
			Vec3 fwd = aim.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : aim.normalize();
			Vec3 side = fwd.cross(UP);
			side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
			Vec3 up = fwd.cross(side);
			float size = 0.9f * 2;
			Fx.add(h.mesh(dome, side, up).at(c.subtract(up.scale(size / 2))).life(0.19f).size(size * 0.7f, size).alpha(0.5f)
				.fade(0.02f, 0.6f));
		}
	}

	// ---- the pools ----

	/**
	 * A pool forming (26FB, H) where the slag landed: the dome DAF 0.57 m growing x2 over 0.58 s; the fire 1B709 1.5-1.73 m, 0.63-0.66
	 * s, rising 1 m/s; the flash 1B97A 1.66-1.96 m; 15-17 sparks (ours 10), 0.78-1.15 s, in a 56-90 degree cone.
	 */
	private static void poolForm(Vec3 at, boolean enemy) {
		float[] dome = Fx.mesh("torb_dome");
		Particle d = dome == null ? null : Fx.particle("torb_dome_noise", enemy ? Palette.TB_POOL_FLASH_ENEMY : Palette.TB_POOL_FLASH);
		if (d != null) {
			// the dome stands on its foot: half of it in the ground
			Fx.add(d.mesh(dome, new Vec3(1, 0, 0)).at(at.subtract(0, 0.3, 0)).life(0.58f).size(0.57f, 1.14f).alpha(0.8f).fade(0.05f, 0.6f));
		}
		Particle fire = Fx.particle("torb_pool_fire", enemy ? Palette.TB_POOL_FIRE_ENEMY : Palette.TB_POOL_FIRE);
		if (fire != null) {
			Fx.add(fire.at(at.add(0, 0.6, 0)).velocity(UP).life(range(0.63f, 0.66f)).size(1.5f, 1.73f).spin(range(0, 6.28f), 0)
				.fade(0.05f, 0.6f).nearFade(1));
		}
		Particle flash = Fx.particle("torb_pool_flash", enemy ? Palette.TB_POOL_FLASH_ENEMY : Palette.TB_POOL_FLASH);
		if (flash != null) {
			Fx.add(flash.at(at.add(0, 0.4, 0)).life(0.2f).size(range(1.66f, 1.96f), 1.8f).alpha(0.8f).fade(0.02f, 0.6f).nearFade(1));
		}
		for (int i = 0; i < 10; i++) {
			spark(at.add(0, 0.1, 0), cone(UP, 70).scale(range(2, 4)), Palette.TB_RIVET_SPARK, range(0.78f, 1.15f), range(0.05f, 0.08f),
				9.8f);
		}
	}

	/**
	 * The lava floor (2754 / 274D, H), each frame of its 10.5 s: lava blob meshes 83F7 / 885C / 887F on the cracked crust 561FC, the
	 * ramp FE77 r3 falling (pale yellow cracks, a dark crust: the crust is the mask's bright part); embers 9489 2-3 a second, 0.044 m
	 * (ours 0.06), 0.54-1.41 s, rising 0.4-0.8 m/s; sparks 0.5-1 a second. Under them a soft molten glow over the disc (ours: the data
	 * has no decal, the blobs are the floor; between them the grass showed). It fades in over 0.3 s and out over its last 0.5 (ours).
	 */
	private static void pool(Pool p, float t) {
		boolean ending = t > p.life - 0.5f;
		Palette lava = p.enemy ? Palette.TB_POOL_LAVA_ENEMY : Palette.TB_POOL_LAVA;
		if (!ending) {
			Particle floor = Fx.attach("torb_pool/" + p.id + "/floor", p.at, () -> {
				Particle s = Fx.soft("cat_soft", Palette.TB_POOL_EMBER);
				return s == null ? null : s.normal(UP).lift(UP).life(1).fade(0.3f, 0.5f);
			});
			if (floor != null) {
				float d = (float) TorbjornStats.POOL_RADIUS * 2.3f;
				floor.size(d, d).alpha(0.75f);
			}
			float[][] meshes = new float[BLOBS.length][];
			for (int i = 0; i < p.blobs.length; i++) {
				String name = p.meshes[i];
				int m = name.equals(BLOBS[0]) ? 0 : name.equals(BLOBS[1]) ? 1 : 2;
				if (meshes[m] == null) {
					meshes[m] = Fx.mesh(name);
				}
				float[] mesh = meshes[m];
				if (mesh == null) {
					continue;
				}
				final int k = i;
				Particle b = Fx.attach("torb_pool/" + p.id + "/" + i, p.blobs[i], () -> {
					Particle s = Fx.particle("torb_lava_crust", lava);
					return s == null ? null : s.life(1).fade(0.3f, 0.5f);
				});
				if (b != null) {
					b.mesh(mesh, p.sides[k]).at(p.blobs[k].subtract(0, p.sizes[k] * BLOB_SINK, 0)).size(p.sizes[k], p.sizes[k]);
				}
			}
		}
		if (ending) {
			return;
		}
		int embers = p.embers[0].take(2.5f * p.blobs.length / 2);
		for (int i = 0; i < embers; i++) {
			Particle e = Fx.particle("sigma_disc", Palette.TB_POOL_EMBER);
			if (e == null) {
				break;
			}
			Vec3 at = p.blobs[Fx.RANDOM.nextInt(p.blobs.length)].add((Fx.RANDOM.nextFloat() - 0.5) * 0.8, 0.15,
				(Fx.RANDOM.nextFloat() - 0.5) * 0.8);
			Fx.add(e.at(at).velocity(cone(UP, 20).scale(range(0.4f, 0.8f))).life(range(0.54f, 1.41f)).size(0.06f, 0.03f)
				.spin(range(0, 6.28f), range(1, 2)).fade(0.1f, 0.5f).nearFade(0.5f));
		}
		int sparks = p.sparks[0].take(0.75f * p.blobs.length / 2);
		for (int i = 0; i < sparks; i++) {
			Vec3 at = p.blobs[Fx.RANDOM.nextInt(p.blobs.length)].add(0, 0.15, 0);
			spark(at, cone(UP, 30).scale(range(1.5f, 3)), Palette.TB_MOLTEN_SPARK, range(0.4f, 0.8f), range(0.03f, 0.05f), 4);
		}
	}

	/**
	 * Burning (254F, H, while the pool's burn 030C is on them; at their feet, M): flames 4A7E 7-10 a second, 0.5-0.75 m, 0.75-1 s (ours
	 * 0.5 s), two layers (D2A3 r12 / 0454 r12); sparks 1F35 20 a second (ours 10), 0.5 s, 0.025-0.05 m, all ways; the ring 86CF 2 m at
	 * their feet held (B8BD r6).
	 */
	private static void burning(Entity e, Burn b) {
		Vec3 feet = e.getPosition(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
		float w = e.getBbWidth() / 2;
		float h = e.getBbHeight();
		int flames = b.flames.take(8);
		for (int i = 0; i < flames; i++) {
			Vec3 at = feet.add((Fx.RANDOM.nextFloat() - 0.5) * w * 2, Fx.RANDOM.nextFloat() * h * 0.6, (Fx.RANDOM.nextFloat() - 0.5) * w * 2);
			flame(at, Palette.TB_BURN, range(0.25f, 0.37f), range(0.8f, 1.1f), 0.5f, 0.8f);
		}
		int second = b.flamesB.take(6);
		for (int i = 0; i < second; i++) {
			Vec3 at = feet.add((Fx.RANDOM.nextFloat() - 0.5) * w * 2, Fx.RANDOM.nextFloat() * h * 0.5, (Fx.RANDOM.nextFloat() - 0.5) * w * 2);
			flame(at, Palette.TB_BURN_B, range(0.2f, 0.3f), range(0.8f, 1.1f), 0.45f, 0.7f);
		}
		int sparks = b.sparks.take(10);
		for (int i = 0; i < sparks; i++) {
			Vec3 at = feet.add(0, Fx.RANDOM.nextFloat() * h, 0);
			Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Math.abs(Fx.RANDOM.nextGaussian()), Fx.RANDOM.nextGaussian()).normalize();
			spark(at, dir.scale(range(1, 2.5f)), Palette.TB_BURN, 0.5f, range(0.025f, 0.05f), 0);
		}
		float[] ring = Fx.mesh("torb_burn_ring_mesh");
		if (ring != null) {
			Particle r = Fx.attach("torb_burn/" + e.getId() + "/ring", feet, () -> {
				Particle s = Fx.particle("torb_burn_ring", Palette.TB_BURN_RING);
				return s == null ? null : s.life(0.4f).fade(0.1f, 0.5f);
			});
			if (r != null) {
				// the ring stands in its X-Y plane in its file: laid flat (its Y along +Z), set back half its size
				float size = Math.max(1.2f, Math.min(2, w * 4));
				r.mesh(ring, new Vec3(1, 0, 0), new Vec3(0, 0, 1)).at(feet.add(0, 0.05, -size / 2)).size(size, size).alpha(0.6f);
			}
		}
	}

	// ---- the Rivet Gun ----

	/**
	 * A rivet (2653) or pellet (1865) in flight, fed each frame from its renderer (H): the slug mesh 83D6 (0.45 m) / 80BE (1.04 m; at
	 * 0.5 here, ours: ten at the data's 0.72 read as a wall) along its flight, glowing; its ribbon 2A480 0.2 m wide, 0.25 s (a pellet's
	 * 192B0 0.24 m, 0.17 s; ours 0.12); the rivet's sparks shed its first 0.25 s, 11 -> 83 -> 40 a second (ours 20), 2.2 m/s,
	 * 0.21-0.39 s. Whether it drew itself (else the item stands in).
	 */
	public static boolean rivetFlight(int id, Vec3 at, Vec3 velocity, float age, float dt, boolean pellet, boolean enemy) {
		Vec3 fwd = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : velocity.normalize();
		Vec3 side = fwd.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(fwd).normalize();
		boolean drawn = false;
		float[] slug = Fx.mesh(pellet ? "torb_pellet" : "torb_rivet");
		if (slug != null) {
			Palette colour = pellet ? Palette.TB_RIVET : Palette.TB_RIVET;
			Particle m = Fx.attach("torb_rivet/" + id, at, () -> {
				Particle s = Fx.particle("cat_noise", colour);
				return s == null ? null : s.life(0.1f).fade(0.01f, 0.5f);
			});
			if (m != null) {
				// the mesh's length is its X (Fx.mesh turns its long side there), its head (the dense end) at -X: laid back along the
				// flight, its head at the projectile; it stands on its foot, so set down by half its height (0.2 of its length, from its
				// bounds)
				float size = pellet ? 0.52f : 0.45f;
				m.mesh(slug, fwd.scale(-1), up).at(at.subtract(up.scale(size * 0.1f)).subtract(fwd.scale(size * 0.45f))).size(size, size);
				drawn = true;
			}
		}
		if (pellet) {
			Fx.trail(id, at, "torb_ribbon", enemy ? Palette.TB_PELLET_TRAIL_ENEMY : Palette.TB_PELLET_TRAIL, 0.12f, 0.17f, 0.8f);
		} else {
			Fx.trail(id, at, "torb_trail", Palette.TB_RIVET_TRAIL, 0.2f, 0.25f, 0.9f);
			if (age < 0.25f) {
				int sparks = Math.round(20 * dt + Fx.RANDOM.nextFloat() * 0.5f);
				for (int i = 0; i < sparks; i++) {
					Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian()).normalize();
					spark(at, dir.scale(2.2f), Palette.TB_RIVET_SPARK, range(0.21f, 0.39f), range(0.03f, 0.05f), 0);
				}
			}
		}
		Fx.trailNearFade(id, 2);
		return drawn;
	}

	/**
	 * The rivet's muzzle 2651 at the gun's hp 0006 (H): the star 191DD (4 frames) 0.23-0.27 m, 0.18 s; the flash 1924B 0.24-0.28 m,
	 * 0.135-0.165 s; the streak 1917E 0.96 m stretched x4, 0.15 s, 1 m/s; a smoke wisp 19271 0.17-0.22 m stretched x4.6, 0.27 s, 1.3
	 * m/s. In his own first person a fifth the size (ours, after Tracer's: a third covered the gun).
	 */
	private static void muzzleFlash(@Nullable Entity torbjorn, Vec3 at, Vec3 dir, boolean alt, boolean enemy) {
		boolean own = ownFirstPerson(torbjorn);
		float k = own ? 0.2f : 1;
		Vec3 from = muzzle(torbjorn, at);
		Vec3 fwd = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
		Palette flashColour = alt ? enemy ? Palette.TB_PELLET_MUZZLE_ENEMY : Palette.TB_MUZZLE
			: enemy ? Palette.TB_MUZZLE_ENEMY : Palette.TB_MUZZLE;
		Palette starColour = enemy ? Palette.TB_MUZZLE_B_ENEMY : Palette.TB_MUZZLE_B;
		float near = own ? 0.2f : 0.5f;
		Particle star = Fx.particle("torb_star#4", starColour);
		if (star != null) {
			// the alt's 2662: 0.65-0.99 m, 0.14 s
			float size = alt ? range(0.65f, 0.99f) : range(0.23f, 0.27f);
			Fx.add(star.at(from).life(alt ? 0.14f : 0.18f).size(size * k, size * 0.8f * k).spin(range(0, 6.28f), 0).fade(0.01f, 0.5f)
				.nearFade(near));
		}
		Particle flash = Fx.particle("torb_flash#4", flashColour);
		if (flash != null) {
			// the alt's 0.38-0.42 m, 0.106 s
			float size = alt ? range(0.38f, 0.42f) : range(0.24f, 0.28f);
			Fx.add(flash.at(from.add(fwd.scale(0.05))).life(alt ? 0.106f : range(0.135f, 0.165f)).size(size * k, size * 0.8f * k)
				.spin(range(0, 6.28f), 0).fade(0.01f, 0.5f).nearFade(near));
		}
		for (int i = 0; i < (alt ? 3 : 1); i++) {
			Particle streak = Fx.particle("cat_burst#4", flashColour);
			if (streak != null) {
				// the alt's three 0.57-0.77 m, 0.07-0.1 s
				float len = (alt ? range(0.57f, 0.77f) : 0.96f) * k;
				Vec3 d = alt ? cone(fwd, 8) : fwd;
				Fx.add(streak.at(from.add(d.scale(len / 2))).shape(Shape.STREAK).velocity(d).life(alt ? range(0.07f, 0.1f) : 0.15f)
					.size(len, len).aspect(0.25f).fade(0.01f, 0.5f).nearFade(near));
			}
		}
		if (!alt) {
			Particle smoke = Fx.soft("torb_smoke_puff", Palette.TB_MUZZLE_SMOKE);
			if (smoke != null) {
				float len = range(0.17f, 0.22f) * 4.6f * k;
				Vec3 d = cone(fwd, 20);
				Fx.add(smoke.at(from.add(d.scale(len / 2))).shape(Shape.STREAK).velocity(d.scale(1.3f)).life(0.27f).size(len, len)
					.aspect(0.22f).alpha(0.5f).fade(0.05f, 0.6f).nearFade(near));
			}
		}
	}

	/**
	 * A rivet's hit (2650, H; creature and world the same): sparks 1F35 5-8 at about 6 m/s, 0.29-0.74 s, stretched x5; the flash 4C09
	 * 0.27-0.33 m, 0.08 s; 10 fast sparks 122C at 15-17 m/s off the surface in a 9 degree cone (ours 5, slowed hard: at the data's
	 * 0.54-0.94 s they flew 10 m).
	 */
	private static void rivetHit(Vec3 at, Vec3 normal) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal.normalize();
		Vec3 c = at.add(n.scale(0.05));
		Particle flash = Fx.particle("cat_soft", Palette.TB_RIVET_HIT);
		if (flash != null) {
			Fx.add(flash.at(c).life(0.08f).size(range(0.27f, 0.33f), 0.3f).fade(0.01f, 0.5f).nearFade(0.3f));
		}
		int sparks = 5 + Fx.RANDOM.nextInt(4);
		for (int i = 0; i < sparks; i++) {
			spark(c, cone(n, 60).scale(range(5, 7)), Palette.TB_RIVET_SPARK, range(0.29f, 0.74f), range(0.047f, 0.067f), 9.8f);
		}
		for (int i = 0; i < 5; i++) {
			Particle s = Fx.particle("spark#4", Palette.TB_RIVET_SPARK);
			if (s != null) {
				Fx.add(s.at(c).shape(Shape.STREAK).velocity(cone(n, 9).scale(range(15, 17))).drag(0.01f).life(range(0.2f, 0.35f))
					.size(0.4f, 0.1f).aspect(0.1f).fade(0.02f, 0.5f).nearFade(0.3f));
			}
		}
	}

	/**
	 * A pellet on a creature (05AF, H): the puff 44A8F (4 frames) about 0.2 m (ours 0.3), 0.33 s; smoke 3F25F 0.25-0.29 m, 0.1-0.9 s.
	 * On the world (265B): 2-3 sparks 1F35, 0.29-0.74 s.
	 */
	private static void pelletHit(Vec3 at, Vec3 normal, boolean creature) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal.normalize();
		Vec3 c = at.add(n.scale(0.05));
		if (creature) {
			Particle puff = Fx.particle("torb_hit_puff#4", Palette.TB_PELLET_HIT);
			if (puff != null) {
				Fx.add(puff.at(c).life(0.33f).size(0.3f, 0.35f).spin(range(0, 6.28f), 0).fade(0.02f, 0.5f).nearFade(0.3f));
			}
			Particle smoke = Fx.soft("torb_hit_smoke", Palette.TB_MUZZLE_SMOKE);
			if (smoke != null) {
				Fx.add(smoke.at(c).velocity(cone(n, 30).scale(0.4f)).life(range(0.3f, 0.9f)).size(range(0.25f, 0.29f), 0.4f).alpha(0.5f)
					.fade(0.05f, 0.6f).nearFade(0.3f));
			}
			return;
		}
		int sparks = 2 + Fx.RANDOM.nextInt(2);
		for (int i = 0; i < sparks; i++) {
			spark(c, cone(n, 50).scale(range(3, 5)), Palette.TB_RIVET_SPARK, range(0.29f, 0.74f), range(0.04f, 0.06f), 9.8f);
		}
	}

	// ---- the reload ("refuel") ----

	/**
	 * The reload (H, scaled to its length: the clips are 2.2 s in his view, 2.0 s as others see him): in his own view 00E2 at the gun
	 * hand: the molten glow 1B6D6 0.175 m and the soft glow A7B6 0.51 m from 0.1 s for 1.07 s, drips 1F5B at 0.66 s for 0.4 s (2.2-2.9
	 * m/s, 0.32 s), sparks 25 a second + 5 at 0.8 s for 0.5 s (2.1-4.5 m/s); as others see him 029D at his hand 000B: a small glow
	 * 0.23 m for 1.67 s from 0.13 s, the burst 5DF x 18B18 0.69 m at 0.8-0.9 s, 0.45 s, about 1 m/s.
	 */
	private static void reload(Entity e, Reload r, float t) {
		boolean own = ownFirstPerson(e);
		float clip = own ? 2.2f : 2.0f;
		float k = r.length / clip;
		Vec3 hand;
		if (own) {
			hand = muzzle(e, e.getEyePosition());
		} else {
			Vec3 posed = point(e, HAND);
			if (posed == null) {
				Vec3 look = e.getViewVector(1);
				Vec3 side = look.cross(UP);
				side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
				posed = e.getEyePosition().add(side.scale(0.3)).add(0, -0.5, 0).add(look.scale(0.3));
			}
			hand = posed;
		}
		float near = own ? 0.15f : 0.5f;
		float scale = own ? 0.35f : 1;
		if (t >= (own ? 0.1f : 0.13f) * k && t <= (own ? 1.17f : 1.8f) * k) {
			Particle glow = Fx.attach("torb_reload/" + r.entity + "/glow", hand, () -> {
				Particle s = Fx.particle("glow", Palette.TB_RELOAD_GLOW_B);
				return s == null ? null : s.life(0.3f).fade(0.2f, 0.5f).nearFade(near);
			});
			if (glow != null) {
				float size = (own ? 0.51f : 0.23f) * scale;
				glow.size(size, size).alpha(0.8f);
			}
			if (own) {
				Particle core = Fx.attach("torb_reload/" + r.entity + "/core", hand, () -> {
					Particle s = Fx.particle("torb_reload_glow", Palette.TB_RELOAD_GLOW);
					return s == null ? null : s.life(0.3f).fade(0.2f, 0.5f).nearFade(near);
				});
				if (core != null) {
					core.size(0.175f * scale, 0.175f * scale);
				}
			}
		}
		if (own && (r.done & 1) == 0 && t >= 0.66f * k) {
			r.done |= 1;
			for (int i = 0; i < 6; i++) {
				spark(hand, new Vec3(0, -range(2.2f, 2.9f), 0).add(cone(UP, 30).scale(0.5)), Palette.TB_SLAG_DRIP, 0.32f,
					range(0.045f, 0.09f) * scale, 4);
			}
		}
		if ((r.done & 2) == 0 && t >= (own ? 0.8f : 0.85f) * k) {
			r.done |= 2;
			if (own) {
				for (int i = 0; i < 12; i++) {
					Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Math.abs(Fx.RANDOM.nextGaussian()), Fx.RANDOM.nextGaussian()).normalize();
					spark(hand, dir.scale(range(2.1f, 4.5f) * scale), Palette.TB_RELOAD_GLOW, range(0.2f, 0.4f), range(0.04f, 0.06f) * scale,
						4);
				}
			} else {
				for (int i = 0; i < 5; i++) {
					Particle burst = Fx.particle("cat_puff", Palette.TB_RELOAD_3P);
					if (burst != null) {
						Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian()).normalize();
						Fx.add(burst.at(hand).velocity(dir).life(0.45f).size(range(0.45f, 0.69f), 0.3f).spin(range(0, 6.28f), 0).alpha(0.8f)
							.fade(0.05f, 0.6f).nearFade(0.8f));
					}
				}
				for (int i = 0; i < 8; i++) {
					Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Math.abs(Fx.RANDOM.nextGaussian()), Fx.RANDOM.nextGaussian()).normalize();
					spark(hand, dir.scale(range(2.1f, 4.5f)), Palette.TB_RELOAD_GLOW, range(0.3f, 0.5f), range(0.04f, 0.06f), 6);
				}
			}
		}
	}

	// ---- the turret ----

	/**
	 * A turret shot (2654, H; at its gun): the line 801 x 510 x 50C (Sombra's tracer masks), 0.15 s, gun to hit, orange HDR (its width
	 * isn't in the data: 0.04 m, ours); the flash 4C09 0.58-0.93 m, 0.1 s; the flash 1924B 0.77-1.08 m at +0.12 m (ours 0.5: it hid the
	 * turret's head); smoke 377D5 0.59-0.79 m, 0.32-0.48 s, about 6.8 m/s along the shot (ours 3).
	 */
	private static void turretShot(Vec3 gun, Vec3 toHit) {
		float len = (float) toHit.length();
		Vec3 fwd = len < 1.0E-3 ? new Vec3(0, 0, 1) : toHit.scale(1 / len);
		Vec3 from = gun.add(fwd.scale(0.35));
		Particle line = Fx.particle("sombra_tracer", Palette.TB_TURRET_LINE);
		if (line != null && len > 1) {
			Vec3 piece = gun.add(toHit).subtract(from);
			float l = (float) piece.length();
			Fx.add(line.at(from.add(piece.scale(0.5))).axis(piece).size(l, l).aspect(0.04f / l).life(0.15f).fade(0.01f, 0.6f).nearFade(0.6f));
		}
		Particle flash = Fx.particle("cat_soft", Palette.TB_TURRET_FLASH);
		if (flash != null) {
			Fx.add(flash.at(from).life(0.1f).size(range(0.58f, 0.93f), 0.5f).alpha(0.7f).fade(0.01f, 0.5f).nearFade(0.5f));
		}
		Particle star = Fx.particle("torb_flash#4", Palette.TB_TURRET_FLASH);
		if (star != null) {
			Fx.add(star.at(from.add(fwd.scale(0.12))).life(range(0.1f, 0.12f)).size(range(0.4f, 0.55f), 0.4f).spin(range(0, 6.28f), 0)
				.fade(0.01f, 0.5f).nearFade(0.5f));
		}
		Particle smoke = Fx.soft("smoke#48", Palette.TB_TURRET_SMOKE);
		if (smoke != null) {
			float l = range(0.59f, 0.79f);
			Fx.add(smoke.at(from.add(fwd.scale(l / 2))).shape(Shape.STREAK).velocity(fwd.scale(3)).drag(0.1f).life(range(0.32f, 0.48f))
				.size(l, l * 1.5f).aspect(0.4f).alpha(0.5f).loop(48).fade(0.05f, 0.6f).nearFade(0.8f));
		}
	}

	/**
	 * Each frame from the turret's renderer: thrown, its ribbon 272D (99F x 1298, 0.2 m wide, 0.2 s; H); under half its health, the
	 * damaged smoke 03A3 from its head (15 a second + 5 in the data, ours 8; 1.0-1.2 s, rising 1.5-1.7 m/s, 0.41-0.75 m; H).
	 */
	public static void turret(dev.overcraft.entity.TurretEntity turret, Vec3 at, boolean flying) {
		if (flying) {
			Fx.trail(turret.getId(), at.add(0, 0.3, 0), "dva_solid", Palette.TB_THROWN, 0.2f, 0.2f, 0.8f);
			return;
		}
		if (turret.getHealth() >= turret.getMaxHealth() * 0.5f || !turret.isAlive()) {
			return;
		}
		int puffs = TURRET_SMOKE.computeIfAbsent(turret.getId(), id -> new Rate()).take(8);
		for (int i = 0; i < puffs; i++) {
			Particle s = Fx.soft("smoke#48", Palette.TB_OVERLOAD_SMOKE);
			if (s != null) {
				Fx.add(s.at(at.add(0, turret.getBbHeight() * 0.85, 0)).velocity(cone(UP, 15).scale(range(1.5f, 1.7f)))
					.life(range(1.0f, 1.2f)).size(range(0.41f, 0.55f), 0.75f).alpha(0.5f).spin(range(0, 6.28f), range(-1, 1)).loop(48)
					.fade(0.1f, 0.6f).nearFade(1));
			}
		}
	}

	// ---- from the server ----

	/** His effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		if (!e.effect().startsWith("torb_")) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Entity entity = mc.level == null || e.entity() < 0 ? null : mc.level.getEntity(e.entity());
		switch (e.effect()) {
			case "torb_muzzle" -> muzzleFlash(entity, at, dir, false, enemy);
			case "torb_alt_muzzle" -> muzzleFlash(entity, at, dir, true, enemy);
			case "torb_rivet_hit" -> rivetHit(at, dir);
			// his turret broken or gone (#150: EXPLOSION stood in). The data has no look for it (0228 / 0229 / 0230 sounds only, 0519 a
			// material: torbjorn-effects.md), so ours: his slag's and his rivet's hit bursts together
			case "torb_turret_gone" -> {
				slagHit(at);
				rivetHit(at, UP);
			}
			case "torb_pellet_hit" -> pelletHit(at, dir, true);
			case "torb_pellet_world" -> pelletHit(at, dir, false);
			case "torb_slag_launch" -> slagLaunch(entity, at, dir, enemy);
			case "torb_slag_hit" -> slagHit(at);
			case "torb_ult_cast" -> ultCast(entity, at, dir, enemy);
			case "torb_pool" -> {
				poolForm(at, enemy);
				POOLS.add(new Pool(poolSerial++, at, Fx.now(), Math.max(1, e.variant()) / 20f, enemy));
			}
			case "torb_burn" -> {
				Burn b = BURNING.computeIfAbsent(e.entity(), id -> new Burn());
				b.until = Fx.now() + e.variant() / 20f + BURN_HOLD;
				b.enemy = enemy;
			}
			case "torb_reload" -> {
				if (e.entity() >= 0) {
					RELOADS.removeIf(r -> r.entity == e.entity());
					RELOADS.add(new Reload(e.entity(), Fx.now(), Math.max(5, e.variant()) / 20f, enemy));
				}
			}
			case "torb_turret_shot" -> turretShot(at, dir);
			default -> {
				return false;
			}
		}
		return true;
	}

	/** A direction within this many degrees of axis. */
	private static Vec3 cone(Vec3 axis, float degrees) {
		Vec3 a = axis.lengthSqr() < 1.0E-6 ? UP : axis.normalize();
		Vec3 side = a.cross(Math.abs(a.y) < 0.9 ? UP : new Vec3(1, 0, 0)).normalize();
		Vec3 up = side.cross(a);
		double turn = Fx.RANDOM.nextDouble() * Math.PI * 2;
		double off = Math.tan(Math.toRadians(degrees)) * Math.sqrt(Fx.RANDOM.nextDouble());
		return a.add(side.scale(Math.cos(turn) * off)).add(up.scale(Math.sin(turn) * off)).normalize();
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}
}
