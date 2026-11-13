package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.EffectPayload;
import dev.overcraft.sojourn.Disruptor;
import dev.overcraft.sojourn.Sojourn;
import dev.overcraft.sojourn.SojournStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Sojourn's effects, from her data (tools/reference/sojourn-effects.md; research\sojourn\fx): cyan, teal and cream-white to her
 * team and red to her enemies (each renderer has a twin on the same LUT's other row). The server sends her moments
 * (EffectPayload "sojourn_*": a round's or the Charged Shot's hit, the shot's tracer, the Disruptor's field); her rounds' and
 * the Disruptor's flights are fed each frame by their renderer (ProjectileModelRenderer); the glow on her gun by her energy,
 * Overclock, Power Slide's thrusters and dust, its jump and the reload are drawn each client tick from her posed points and
 * her synced state (HeroManager.SOJOURN_GUN / SOJOURN_SLIDE).
 */
public final class SojournFx {
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/**
	 * Her points the renderer hands over each frame: her Railgun's muzzle (hp 0006) and energy glow point (hp 007C, where 31E6,
	 * 2880 and the reload's 30A6 sit), then her body's: the slide's thruster points (hp 002C / 002E / 0030 / 0031: 2905 C23),
	 * her pelvis (hp 01DA: its dust), her feet (the sparks' 001B / 001C: the foot bones, ours) and the slide jump's points (hp
	 * 002D / 002F: 330E).
	 */
	public static final String[] POINTS = {"hardpoint_0006", "hardpoint_007C", "hardpoint_002C", "hardpoint_002E", "hardpoint_0030",
		"hardpoint_0031", "hardpoint_01DA", "Foot_L", "Foot_R", "hardpoint_002D", "hardpoint_002F"};
	/** The first of POINTS that are on her gun (the rest are her own bones). */
	public static final int GUN_POINTS = 2;
	private static final int MUZZLE = 0, GUN = 1, JET = 2, PELVIS = 6, FOOT = 7, BOOT = 9;

	private static final class Posed {
		Vec3[] points = new Vec3[0];
		long tick;
	}

	/** A Disruptor field: its centre, the ground under it, whose (entity id), when (Fx.now()), for how long, its look. */
	private record Field(int key, Vec3 centre, @Nullable Vec3 ground, int owner, float born, float life, boolean slows,
						 boolean enemy) {
	}

	private static final Map<Integer, Posed> POSED = new HashMap<>();
	private static final List<Field> FIELDS = new ArrayList<>();
	/** Rounds and Disruptor shots already seen leaving (their muzzle flash, the launch), by entity id: when last seen. */
	private static final Map<Integer, Float> SEEN = new HashMap<>();
	/** Each Sojourn's last slide jump and reload whose effects were drawn (their start, game seconds): once each. */
	private static final Map<Integer, Float> JUMP_DONE = new HashMap<>();
	private static final Map<Integer, Float> RELOAD_DONE = new HashMap<>();
	/** Her own view's gun glow point, kept from its last frame (a following particle needs one every frame). */
	private static final Vector3f VIEW_GUN = new Vector3f(0.25f, -0.2f, 0.6f);
	private static int keys;

	/** How far from her own eyes her rounds start to show in her first-person view (m, ours: played in step 3). */
	private static final double OWN_CLEAR = 3;
	/** A round's hit flare against the data's 0.57-2.3 m: 16 rounds a second at one spot stack (ours, as Juno's HIT_SCALE). */
	private static final float HIT_SCALE = 0.35f;
	/** The Charged Shot's tracer 3216 (250 m/s, 2BF0's glows 256-500 a second: one each 0.5-1 m along it). */
	private static final float TRACER_SPEED = 250, TRACER_STEP = 0.35f;
	/** Its look against the data's at full energy (ours: the data's beam 2835 doesn't scale with it that we read). */
	private static final float CHARGED_MIN = 0.45f;

	private SojournFx() {
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}

	private static Vec3 randomUnit() {
		Vec3 v = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return v.lengthSqr() < 1.0E-6 ? UP : v.normalize();
	}

	/** A direction within {@code angle} radians of {@code axis}. */
	private static Vec3 cone(Vec3 axis, double angle) {
		Vec3 r = randomUnit();
		return axis.normalize().add(r.scale(Math.sin(angle) * Fx.RANDOM.nextFloat())).normalize();
	}

	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/** A particle on a mask, blended for an enemy's warm red (additive red over a bright sky turned pink-white). */
	/** Her own art, made ahead as she's picked (EffectArt.prepare, #126): each mask and palette her effects below ask for. */
	public static final List<EffectArt.Art> ART = List.of(
		new EffectArt.Art("brigitte_smoke#32", Palette.SOJ_CHARGED_SMOKE, true),
		new EffectArt.Art("brigitte_smoke#32", Palette.SOJ_JUMP_SMOKE, true),
		new EffectArt.Art("brigitte_smoke#32", Palette.SOJ_SLIDE_SMOKE, true),
		new EffectArt.Art("cat_band", Palette.SOJ_FIELD_DOME, false),
		new EffectArt.Art("cat_glow_dot", Palette.SOJ_DISRUPTOR_GLOW, false),
		new EffectArt.Art("cat_glow_dot", Palette.SOJ_DISRUPTOR_LAUNCH, false),
		new EffectArt.Art("cat_puff", Palette.SOJ_FIELD_MOTE, false),
		new EffectArt.Art("cat_streak", Palette.SOJ_CHARGED_HIT_SPARK, false),
		new EffectArt.Art("cat_streak", Palette.SOJ_HIT_SPARK, false),
		new EffectArt.Art("cat_streak", Palette.SOJ_MUZZLE_SPARK, false),
		new EffectArt.Art("cat_streak", Palette.SOJ_RELOAD, false),
		new EffectArt.Art("cat_streak", Palette.SOJ_SLIDE_SPARK, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_CHARGED_MUZZLE, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_CHARGED_TRACER, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_FIELD_CORE, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_GUN_CORE, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_GUN_GLOW, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_HIT, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_JUMP_GLOW, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_MUZZLE, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_OC_CORE, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_OC_GLOW, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_RELOAD, false),
		new EffectArt.Art("lucio_glow", Palette.SOJ_ROUND, false),
		new EffectArt.Art("lw_thorn_flame#2", Palette.SOJ_SLIDE_DUST, true),
		new EffectArt.Art("mercy_glow", Palette.SOJ_FIELD_DOME, false),
		new EffectArt.Art("mercy_glow", Palette.SOJ_OC_BODY, false),
		new EffectArt.Art("mercy_glow", Palette.SOJ_SLIDE_JET, false),
		new EffectArt.Art("mercy_noise_b", Palette.SOJ_DECEL, false),
		new EffectArt.Art("sojourn_beam", Palette.SOJ_CHARGED_BEAM, false),
		new EffectArt.Art("sojourn_burst#3", Palette.SOJ_HIT, false),
		new EffectArt.Art("sojourn_burst#3", Palette.SOJ_SLIDE_STREAK, false),
		new EffectArt.Art("sojourn_dot", Palette.SOJ_DISRUPTOR_MOTE, false),
		new EffectArt.Art("sojourn_dot", Palette.SOJ_FIELD_FLASH, false),
		new EffectArt.Art("sojourn_dot", Palette.SOJ_FIELD_MOTE, false),
		new EffectArt.Art("sojourn_field_decal", Palette.SOJ_FIELD_DECAL, false),
		new EffectArt.Art("sojourn_flash#14", Palette.SOJ_CHARGED_MUZZLE, false),
		new EffectArt.Art("sojourn_line", Palette.SOJ_DISRUPTOR_CORE, false),
		new EffectArt.Art("sojourn_line", Palette.SOJ_FIELD_SWIRL, false),
		new EffectArt.Art("sojourn_round", Palette.SOJ_ROUND, false),
		new EffectArt.Art("sojourn_spark_tail", Palette.SOJ_JUMP_SPARK, false),
		new EffectArt.Art("spark#4", Palette.SOJ_CHARGED_SPARK_N, false)
	);

	/** And an enemy Sojourn's, for a client seeing one (the same, blended, in the enemy's colours). */
	public static final List<EffectArt.Art> ART_ENEMY = List.of(
		new EffectArt.Art("brigitte_smoke#32", Palette.SOJ_CHARGED_SMOKE_ENEMY, true),
		new EffectArt.Art("cat_band", Palette.SOJ_FIELD_DOME_ENEMY, true),
		new EffectArt.Art("cat_glow_dot", Palette.SOJ_DISRUPTOR_GLOW_ENEMY, true),
		new EffectArt.Art("cat_glow_dot", Palette.SOJ_DISRUPTOR_LAUNCH_ENEMY, true),
		new EffectArt.Art("cat_puff", Palette.SOJ_FIELD_MOTE_ENEMY, true),
		new EffectArt.Art("cat_streak", Palette.SOJ_CHARGED_HIT_SPARK_ENEMY, true),
		new EffectArt.Art("cat_streak", Palette.SOJ_GUN_GLOW_ENEMY, true),
		new EffectArt.Art("cat_streak", Palette.SOJ_HIT_SPARK_ENEMY, true),
		new EffectArt.Art("cat_streak", Palette.SOJ_MUZZLE_SPARK_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_CHARGED_MUZZLE_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_CHARGED_TRACER_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_FIELD_CORE_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_GUN_CORE_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_GUN_GLOW_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_HIT_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_MUZZLE_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_OC_CORE_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_OC_GLOW_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_ROUND_ENEMY, true),
		new EffectArt.Art("lucio_glow", Palette.SOJ_SLIDE_JET_ENEMY, true),
		new EffectArt.Art("mercy_glow", Palette.SOJ_FIELD_DOME_ENEMY, true),
		new EffectArt.Art("mercy_glow", Palette.SOJ_OC_GLOW_ENEMY, true),
		new EffectArt.Art("mercy_glow", Palette.SOJ_SLIDE_JET_ENEMY, true),
		new EffectArt.Art("mercy_noise_b", Palette.SOJ_DECEL_ENEMY, true),
		new EffectArt.Art("sojourn_beam", Palette.SOJ_CHARGED_BEAM_ENEMY, true),
		new EffectArt.Art("sojourn_burst#3", Palette.SOJ_HIT_ENEMY, true),
		new EffectArt.Art("sojourn_burst#3", Palette.SOJ_SLIDE_JET_ENEMY, true),
		new EffectArt.Art("sojourn_dot", Palette.SOJ_DISRUPTOR_MOTE_ENEMY, true),
		new EffectArt.Art("sojourn_dot", Palette.SOJ_FIELD_FLASH_ENEMY, true),
		new EffectArt.Art("sojourn_dot", Palette.SOJ_FIELD_MOTE_ENEMY, true),
		new EffectArt.Art("sojourn_field_decal", Palette.SOJ_FIELD_DOME_ENEMY, true),
		new EffectArt.Art("sojourn_flash#14", Palette.SOJ_CHARGED_MUZZLE_ENEMY, true),
		new EffectArt.Art("sojourn_line", Palette.SOJ_DISRUPTOR_CORE_ENEMY, true),
		new EffectArt.Art("sojourn_line", Palette.SOJ_FIELD_SWIRL_ENEMY, true),
		new EffectArt.Art("sojourn_round", Palette.SOJ_ROUND_ENEMY, true),
		new EffectArt.Art("sojourn_spark_tail", Palette.SOJ_SLIDE_JET_ENEMY, true),
		new EffectArt.Art("spark#4", Palette.SOJ_CHARGED_TRACER_ENEMY, true)
	);

	private static @Nullable Particle make(String mask, boolean enemy, Palette ours, Palette theirs) {
		return enemy ? Fx.soft(mask, theirs) : Fx.particle(mask, ours);
	}

	// ---- her posed points (HeroModelRenderer) ----

	public static void posed(Entity sojourn, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(sojourn.getId(), id -> new Posed());
		p.points = points;
		p.tick = sojourn.level().getGameTime();
	}

	/** Her posed point this tick or the last (null in her own first person, unposed, or without that point). */
	private static @Nullable Vec3 point(@Nullable Entity sojourn, int which) {
		if (sojourn == null || ownFirstPerson(sojourn)) {
			return null;
		}
		Posed p = POSED.get(sojourn.getId());
		return p == null || sojourn.level().getGameTime() - p.tick > 1 || which >= p.points.length ? null : p.points[which];
	}

	/** Her look's frame: ahead, right, up. */
	private static Vec3[] frame(Entity e) {
		Vec3 look = e.getViewVector(1);
		Vec3 right = look.cross(UP);
		right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
		return new Vec3[] {look, right, right.cross(look).normalize()};
	}

	/**
	 * Her muzzle: in her own first person the view model's as drawn (HeroModelRenderer.viewMuzzle), else posed (hp 0006), else
	 * ours, right of and below her eyes.
	 */
	private static Vec3 muzzle(Entity sojourn) {
		Minecraft mc = Minecraft.getInstance();
		if (ownFirstPerson(sojourn)) {
			Vector3f view = dev.overcraft.client.model.HeroModelRenderer.viewMuzzle();
			if (view != null) {
				return mc.gameRenderer.mainCamera().position().add(view.x, view.y, view.z);
			}
		}
		Vec3 posed = point(sojourn, MUZZLE);
		if (posed != null) {
			return posed;
		}
		Vec3[] f = frame(sojourn);
		return sojourn.getEyePosition().add(f[0].scale(0.7)).add(f[1].scale(0.2)).add(f[2].scale(-0.15));
	}

	/** Her gun's glow point (hp 007C): posed, or null (her own first person draws it on the view model: viewGun). */
	private static @Nullable Vec3 gun(Entity sojourn) {
		return point(sojourn, GUN);
	}

	/** Her own view's gun point from the camera, as last drawn. */
	private static Vector3f viewGun() {
		Vector3f v = dev.overcraft.client.model.HeroModelRenderer.viewGun();
		if (v != null) {
			VIEW_GUN.set(v);
		}
		return new Vector3f(VIEW_GUN);
	}

	// ---- each client tick ----

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			return;
		}
		float now = Fx.now();
		for (Player p : mc.level.players()) {
			if (!"sojourn".equals(p.getAttached(HeroManager.HERO)) || !p.isAlive() || p.isSpectator()) {
				continue;
			}
			Integer sync = p.getAttached(HeroManager.SOJOURN_GUN);
			int gun = sync == null ? 0 : sync;
			boolean overclock = (gun & Sojourn.SYNC_OVERCLOCK) != 0;
			float energy = gun & (Sojourn.SYNC_OVERCLOCK - 1);
			boolean enemy = KirikoFx.enemyOf(p);
			energyGlow(p, energy, enemy);
			if (overclock) {
				overclock(p, enemy);
			}
			if (Boolean.TRUE.equals(p.getAttached(HeroManager.SOJOURN_SLIDE))) {
				slide(p, enemy);
			}
			slideJump(p, enemy);
			reload(p, enemy);
		}
		fields(mc, now);
		POSED.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		SEEN.values().removeIf(t -> now - t > 2);
	}

	// ---- the Railgun ----

	/**
	 * A Railgun round in flight (ProjectileModelRenderer, each frame): false without the local pack (its item stands in). The
	 * first time it's seen, the muzzle flash at her gun (2837). The round (280D: projectile 2F98's child 312B): a ribbon 0.096 m
	 * wide on the comet strip 1FDF6, its points living 0.015-0.07 s (at 150 m/s a streak of a few metres), navy, cyan and cream
	 * to her team (red to orange to enemies). In her own first person it shows from 3 m on (found playing in step 3: its
	 * start at her eyes sat on the crosshair).
	 */
	public static boolean roundFlight(int id, Vec3 at, @Nullable Entity owner) {
		boolean enemy = KirikoFx.enemyOf(owner);
		if (SEEN.put(id, Fx.now()) == null && owner != null) {
			muzzleFlash(owner, enemy);
		}
		Minecraft mc = Minecraft.getInstance();
		if (owner == mc.player && mc.options.getCameraType().isFirstPerson() && at.distanceTo(mc.player.getEyePosition()) < OWN_CLEAR) {
			return EffectArt.get("sojourn_round", Palette.SOJ_ROUND, false) != null;
		}
		Particle head = Fx.attach("railgun_shot/" + id, at, () -> {
			Particle g = make("lucio_glow", enemy, Palette.SOJ_ROUND, Palette.SOJ_ROUND_ENEMY);
			return g == null ? null : g.life(0.1f).size(0.14f, 0.14f).alpha(0.8f).nearFade(1.5f).fade(0.02f, 0.5f);
		});
		if (head == null) {
			return false;
		}
		Fx.trail(id, at, "sojourn_round", enemy ? Palette.SOJ_ROUND_ENEMY : Palette.SOJ_ROUND, 0.096f, 0.05f, 1, enemy);
		return true;
	}

	/**
	 * Each round's muzzle (2837 on hp 0006, 0.17 s): a flare 210FD of 0.38-0.51 m and a faint one of 0.55-2.5 m (ours at 0.6 of
	 * it), 0.13 s, navy to cyan to cream; 10-15 sparks 183A7 at 2-7.6 m/s along the aim (ours: 6). In her own view a third
	 * the size and fainter (ours, as Juno's and Ana's).
	 */
	private static void muzzleFlash(Entity owner, boolean enemy) {
		boolean own = ownFirstPerson(owner);
		Vec3 at = muzzle(owner);
		Vec3 dir = owner.getViewVector(1);
		float k = own ? 0.35f : 1;
		Particle flare = make("lucio_glow", enemy, Palette.SOJ_MUZZLE, Palette.SOJ_MUZZLE_ENEMY);
		if (flare != null) {
			Fx.add(flare.at(at).life(0.127f).size(range(0.38f, 0.51f) * k, 0.3f * k).alpha(own ? 0.5f : 0.9f).fade(0.01f, 0.6f)
				.nearFade(own ? 0 : 0.3f));
		}
		Particle wide = make("lucio_glow", enemy, Palette.SOJ_MUZZLE, Palette.SOJ_MUZZLE_ENEMY);
		if (wide != null) {
			Fx.add(wide.at(at).life(0.127f).size(range(0.55f, 2.5f) * 0.6f * k, 0.4f * k).alpha(own ? 0.12f : 0.3f).fade(0.01f, 0.6f)
				.nearFade(own ? 0 : 0.5f));
		}
		for (int i = 0; i < (own ? 3 : 6); i++) {
			Particle spark = make("cat_streak", enemy, Palette.SOJ_MUZZLE_SPARK, Palette.SOJ_MUZZLE_SPARK_ENEMY);
			if (spark != null) {
				Fx.add(spark.at(at).shape(Shape.STREAK).velocity(cone(dir, 0.5).scale(range(2, 7.6f))).life(range(0.05f, 0.15f))
					.size(range(0.09f, 0.14f) * k, 0.05f * k).aspect(0.25f).fade(0.01f, 0.5f).nearFade(own ? 0 : 0.3f));
			}
		}
	}

	/**
	 * A round's hit (31E5 on a creature or the world, 0.67 s): a flare 210FD of 0.57-2.3 m (at HIT_SCALE), 0.07-0.16 s, teal to
	 * pale yellow; two star bursts 1F79A of 0.17-0.31 m off the surface at 0.37-1.38 m/s; four sparks 183A7 at 2.4-2.7 m/s in a
	 * 35-degree cone (a little bigger than the data's 4-7 cm: ours, so they read).
	 */
	private static void roundHit(Vec3 at, Vec3 normal, boolean enemy) {
		boolean surface = normal.lengthSqr() > 1.0E-6;
		Vec3 out = surface ? normal.normalize() : randomUnit();
		Particle flare = make("lucio_glow", enemy, Palette.SOJ_HIT, Palette.SOJ_HIT_ENEMY);
		if (flare != null) {
			flare.at(at).life(range(0.07f, 0.16f)).size(range(0.57f, 2.3f) * HIT_SCALE, 0.3f * HIT_SCALE).alpha(0.8f).fade(0.01f, 0.6f)
				.nearFade(0.5f);
			Fx.add(surface ? flare.lift(out) : flare);
		}
		for (int i = 0; i < 2; i++) {
			Particle star = make("sojourn_burst#3", enemy, Palette.SOJ_HIT, Palette.SOJ_HIT_ENEMY);
			if (star != null) {
				Fx.add(star.at(at.add(out.scale(0.03))).velocity(cone(out, 0.35).scale(range(0.37f, 1.38f))).life(range(0.05f, 0.1f))
					.size(range(0.17f, 0.31f), 0.2f).spin(range(0, 6.28f), 0).fade(0.01f, 0.5f).nearFade(0.3f));
			}
		}
		for (int i = 0; i < 4; i++) {
			Particle spark = make("cat_streak", enemy, Palette.SOJ_HIT_SPARK, Palette.SOJ_HIT_SPARK_ENEMY);
			if (spark != null) {
				Fx.add(spark.at(at.add(out.scale(0.03))).shape(Shape.STREAK).velocity(cone(out, 0.62).scale(range(2.4f, 2.7f)))
					.life(range(0.14f, 0.17f)).size(range(0.07f, 0.11f), 0.04f).aspect(0.25f).fade(0.01f, 0.5f).nearFade(0.3f));
			}
		}
	}

	/**
	 * The energy glow on her gun (its child 3763's 31E6 on hp 007C, by her energy, df C9 read as energy / 100: M): a cyan glow
	 * 210FD of 0.15 m, 0.33 m from half energy, 0.49 m at full (C46's curve; ours, the curve's steps, one glow held rather than
	 * its 4.75 a second); from half on a wider faint one (C61 0.55-0.97 m, ours at half its alpha). None at 0 (ours: its
	 * idle look is the gun's own material). In her own view on the view model's gun (viewGun).
	 */
	private static void energyGlow(Player p, float energy, boolean enemy) {
		boolean own = ownFirstPerson(p);
		Vec3 at = own ? null : gun(p);
		String key = "sojourn_gun/" + p.getId();
		if (energy <= 0 || !own && at == null) {
			return;
		}
		float share = energy / (float) SojournStats.ENERGY_MAX;
		float size = share < 0.503f ? 0.15f : share < 0.975f ? 0.332f : 0.486f;
		float k = own ? 0.5f : 1;
		Particle glow = Fx.attach(key + (own ? "/1p" : ""), own ? Vec3.ZERO : at, () -> {
			Particle g = make("lucio_glow", enemy, Palette.SOJ_GUN_GLOW, Palette.SOJ_GUN_GLOW_ENEMY);
			if (g == null) {
				return null;
			}
			g.life(0.3f).fade(0.1f, 0.4f).nearFade(own ? 0 : 0.3f);
			return own ? g.follow(SojournFx::viewGun).near() : g;
		});
		if (glow != null) {
			glow.size(size * k, size * k).alpha(own ? 0.45f : 0.7f);
		}
		if (share >= 0.503f) {
			Particle wide = Fx.attach(key + "/wide" + (own ? "/1p" : ""), own ? Vec3.ZERO : at, () -> {
				Particle g = make("lucio_glow", enemy, Palette.SOJ_GUN_CORE, Palette.SOJ_GUN_CORE_ENEMY);
				if (g == null) {
					return null;
				}
				g.life(0.3f).fade(0.1f, 0.4f).nearFade(own ? 0 : 0.5f).size(0.76f * k, 0.76f * k);
				return own ? g.follow(SojournFx::viewGun).near() : g;
			});
			if (wide != null) {
				wide.alpha(own ? 0.08f : 0.15f);
			}
		}
	}

	/**
	 * The reload (the gun's 1P clip 6FCB's 30A6 on hp 007C; its 3P clip 70D5's 30C4 only spawns its magazine entity, so others
	 * see the 1P's look: ours): at 0.08-0.1 s a 0.05-0.27 m flash, teal to mint, and 15 sparks at 1-1.4 m/s (ours: 6), once
	 * a reload.
	 */
	private static void reload(Player p, boolean enemy) {
		HeroAnimator a = HeroAnimator.of(p);
		float t = p.level().getGameTime() / 20f;
		float since = a.since("reload", t);
		if (since < 0.08f || since > 0.5f) {
			return;
		}
		Float done = RELOAD_DONE.get(p.getId());
		float start = t - since;
		if (done != null && Math.abs(done - start) < 0.01f) {
			return;
		}
		boolean own = ownFirstPerson(p);
		Vec3 at = own ? null : gun(p);
		if (!own && at == null) {
			return;
		}
		RELOAD_DONE.put(p.getId(), start);
		Particle flash = make("lucio_glow", enemy, Palette.SOJ_RELOAD, Palette.SOJ_GUN_GLOW_ENEMY);
		if (flash != null) {
			flash.life(0.17f).size(range(0.05f, 0.27f) + 0.1f, 0.1f).fade(0.01f, 0.5f);
			Fx.add(own ? flash.follow(SojournFx::viewGun).near() : flash.at(at).nearFade(0.3f));
		}
		for (int i = 0; i < 6; i++) {
			Particle spark = make("cat_streak", enemy, Palette.SOJ_RELOAD, Palette.SOJ_GUN_GLOW_ENEMY);
			if (spark != null) {
				spark.shape(Shape.STREAK).velocity(randomUnit().scale(range(0.97f, 1.44f))).life(range(0.2f, 0.45f))
					.size(range(0.04f, 0.07f), 0.02f).aspect(0.3f).fade(0.05f, 0.5f);
				Fx.add(own ? spark.follow(SojournFx::viewGun).near() : spark.at(at).nearFade(0.3f));
			}
		}
	}

	// ---- the Charged Shot ----

	/**
	 * The Charged Shot (the server's sojourn_tracer: from where it left to where it stopped, by her energy): the muzzle (2834
	 * on hp 0006: a 1-2.5 m flare, the 14-frame flash 1F8BE of 0.4-0.68 m for 0.29 s, sparks 183A7 at 2.9-5.2 m/s); the beam
	 * (2484's 2835: the line 2A5D0, 1.3 s, navy, teal, green to white; ours 0.3 m wide); the tracer 3216 flying it at 250 m/s
	 * (2BF0: glows 0.3-0.4 m every 0.35 m of it, cream, ice blue and blue, drifting 0.65-0.74 m/s for 0.28-0.75 s; sparks 1F35
	 * at 4.4-9 m/s, white to steel blue, one a metre where the data's are 1669 a second, at 0.6 of its size: rings, played). Fainter at low energy (ours).
	 */
	private static void tracer(@Nullable Entity shooter, Vec3 sentFrom, Vec3 to, float energy, boolean enemy) {
		dev.overcraft.hero.PressTrace.seen("fx");
		Vec3 from = shooter != null ? muzzle(shooter) : sentFrom;
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len < 0.5f) {
			return;
		}
		Vec3 dir = piece.scale(1 / len);
		boolean own = ownFirstPerson(shooter);
		float k = own ? 0.4f : 1;
		float charge = CHARGED_MIN + (1 - CHARGED_MIN) * Math.min(1, energy / (float) SojournStats.ENERGY_MAX);
		Particle flare = make("lucio_glow", enemy, Palette.SOJ_CHARGED_MUZZLE, Palette.SOJ_CHARGED_MUZZLE_ENEMY);
		if (flare != null) {
			Fx.add(flare.at(from).life(0.17f).size(range(1, 2.5f) * k, 0.8f * k).alpha(0.8f * charge).fade(0.01f, 0.6f)
				.nearFade(own ? 0 : 0.4f));
		}
		Particle flash = make("sojourn_flash#14", enemy, Palette.SOJ_CHARGED_MUZZLE, Palette.SOJ_CHARGED_MUZZLE_ENEMY);
		if (flash != null) {
			Fx.add(flash.at(from).life(0.29f).size(range(0.4f, 0.68f) * k, 0.68f * k).alpha(charge).fade(0.01f, 0.5f)
				.nearFade(own ? 0 : 0.3f));
		}
		for (int i = 0; i < 8; i++) {
			Particle spark = make("cat_streak", enemy, Palette.SOJ_CHARGED_HIT_SPARK, Palette.SOJ_CHARGED_HIT_SPARK_ENEMY);
			if (spark != null) {
				Fx.add(spark.at(from).shape(Shape.STREAK).velocity(cone(dir, 0.6).scale(range(2.9f, 5.2f))).life(range(0.08f, 0.17f))
					.size(range(0.19f, 0.31f) * k, 0.1f * k).aspect(0.15f).fade(0.01f, 0.5f).nearFade(own ? 0 : 0.3f));
			}
		}
		// the beam: a band along the whole shot, fading (2835 C26 / C34 1.3 s)
		Particle beam = make("sojourn_beam", enemy, Palette.SOJ_CHARGED_BEAM, Palette.SOJ_CHARGED_BEAM_ENEMY);
		if (beam != null) {
			Fx.add(beam.at(from.add(piece.scale(0.5))).shape(Shape.STREAK).axis(piece).size(len, len).aspect(0.3f / len).life(1.3f)
				.alpha(charge).fade(0.01f, 0.85f).nearFade(0.6f));
		}
		// the tracer's glows and sparks along it, each as the 250 m/s tracer passes (its age held back by the distance)
		int steps = Math.min(300, (int) (len / TRACER_STEP));
		for (int i = 1; i <= steps; i++) {
			float d = i * TRACER_STEP;
			Vec3 at = from.add(dir.scale(d));
			Particle glow = make("lucio_glow", enemy, Palette.SOJ_CHARGED_TRACER, Palette.SOJ_CHARGED_TRACER_ENEMY);
			if (glow != null) {
				glow.age = -d / TRACER_SPEED;
				Fx.add(glow.at(at).velocity(randomUnit().scale(range(0.65f, 0.74f))).life(range(0.28f, 0.75f))
					.size(range(0.3f, 0.4f), 0.15f).alpha(0.4f * charge).fade(0.02f, 0.7f).nearFade(0.8f));
			}
			if (i % 3 == 0) {
				Particle spark = enemy ? Fx.soft("spark#4", Palette.SOJ_CHARGED_TRACER_ENEMY) : Fx.particle("spark#4", Palette.SOJ_CHARGED_SPARK_N);
				if (spark != null) {
					spark.age = -d / TRACER_SPEED;
					Fx.add(spark.at(at).velocity(randomUnit().scale(range(4.4f, 9))).drag(0.1f).life(range(0.44f, 0.59f))
						.size(range(0.17f, 0.24f) * 0.6f, 0.03f).alpha(charge).spin(range(0, 6.28f), 0).fade(0.02f, 0.6f).nearFade(0.5f));
				}
			}
		}
	}

	/**
	 * Where the Charged Shot hits (22BF on each one it goes through, and on the block it stops at): a flare 210FD of 3.15 m
	 * (ours at 0.6) for 0.28 s; the 14-frame flash 1F8BE of 2.63 m (ours at 0.6) for 0.29 s; 27-29 sparks 183A7 at 2.6-6 m/s
	 * (ours: 14), blue to white; four puffs of smoke 1DFCA/B of 0.9-2.6 m at 0.95-1.65 m/s for 0.6-1 s, grey to ice blue.
	 */
	private static void chargedHit(Vec3 at, Vec3 normal, boolean enemy) {
		boolean surface = normal.lengthSqr() > 1.0E-6;
		Vec3 out = surface ? normal.normalize() : randomUnit();
		Particle flare = make("lucio_glow", enemy, Palette.SOJ_CHARGED_MUZZLE, Palette.SOJ_CHARGED_MUZZLE_ENEMY);
		if (flare != null) {
			flare.at(at).life(0.28f).size(3.15f * 0.6f, 1).alpha(0.8f).fade(0.01f, 0.6f).nearFade(1);
			Fx.add(surface ? flare.lift(out) : flare);
		}
		Particle flash = make("sojourn_flash#14", enemy, Palette.SOJ_CHARGED_MUZZLE, Palette.SOJ_CHARGED_MUZZLE_ENEMY);
		if (flash != null) {
			flash.at(at).life(0.29f).size(2.63f * 0.6f, 2.63f * 0.6f).fade(0.01f, 0.5f).nearFade(1);
			Fx.add(surface ? flash.lift(out) : flash);
		}
		for (int i = 0; i < 14; i++) {
			Particle spark = make("cat_streak", enemy, Palette.SOJ_CHARGED_HIT_SPARK, Palette.SOJ_CHARGED_HIT_SPARK_ENEMY);
			if (spark != null) {
				Fx.add(spark.at(at.add(out.scale(0.05))).shape(Shape.STREAK).velocity(cone(out, 0.44 * 2).scale(range(2.6f, 6)))
					.gravity(4).life(range(0.31f, 0.5f)).size(range(0.3f, 0.55f), 0.1f).aspect(0.15f).fade(0.01f, 0.5f).nearFade(0.4f));
			}
		}
		for (int i = 0; i < 4; i++) {
			Particle smoke = Fx.soft("brigitte_smoke#32", enemy ? Palette.SOJ_CHARGED_SMOKE_ENEMY : Palette.SOJ_CHARGED_SMOKE);
			if (smoke != null) {
				Fx.add(smoke.at(at.add(out.scale(0.2))).velocity(cone(out, 1.2).scale(range(0.95f, 1.65f))).drag(0.3f)
					.life(range(0.61f, 0.99f)).size(range(0.88f, 1.4f), 2.6f).alpha(0.35f).spin(range(0, 6.28f), 0.5f).easeOut()
					.fade(0.05f, 0.7f).nearFade(1.5f));
			}
		}
	}

	// ---- Overclock ----

	/**
	 * Overclock on her gun (2159 #539 2880 on hp 0006 / 0007, held through it: glows 210FD of 0.5-1.4 m 20 a second, blue to
	 * teal to green; a core sphere DBF of 0.59 m, black to aqua; its vents' streaks on mesh E613 aren't drawn, no mesh) and on
	 * her (#533 333B, a material effect on her body, cyan falling to black: ours, a faint glow round her, not in her own view).
	 */
	private static void overclock(Player p, boolean enemy) {
		boolean own = ownFirstPerson(p);
		Vec3 at = own ? null : gun(p);
		if (own || at != null) {
			Particle glow = make("lucio_glow", enemy, Palette.SOJ_OC_GLOW, Palette.SOJ_OC_GLOW_ENEMY);
			if (glow != null) {
				glow.life(0.26f).size(range(0.5f, 1.0f) * (own ? 0.5f : 1), 0.3f).alpha(own ? 0.25f : 0.5f).fade(0.05f, 0.6f);
				Fx.add(own ? glow.follow(SojournFx::viewGun).near() : glow.at(at).nearFade(0.5f));
			}
			if (p.tickCount % 2 == 0) {
				Particle core = make("lucio_glow", enemy, Palette.SOJ_OC_CORE, Palette.SOJ_OC_CORE_ENEMY);
				if (core != null) {
					core.life(range(0.36f, 0.55f)).size(0.59f * (own ? 0.5f : 1), 0.4f).alpha(own ? 0.4f : 0.8f).fade(0.05f, 0.6f);
					Fx.add(own ? core.follow(SojournFx::viewGun).near() : core.at(at).nearFade(0.3f));
				}
			}
		}
		if (!own) {
			Particle body = make("mercy_glow", enemy, Palette.SOJ_OC_BODY, Palette.SOJ_OC_GLOW_ENEMY);
			if (body != null) {
				Fx.add(body.at(p.position().add(0, p.getBbHeight() * 0.55, 0)).life(0.3f).size(1.9f, 1.9f).aspect(0.6f).alpha(0.12f)
					.fade(0.2f, 0.5f).nearFade(1.5f));
			}
		}
	}

	// ---- Power Slide ----

	/**
	 * Her slide (2905 on hp 01DA, held for it), each tick: the thruster glows 242 of 0.69 m on hp 002C / 002E / 0030 / 0031 (13
	 * a second each, 0.18 s), blue to cyan to mint; their streaks 1F79A of 0.19-0.3 m (24 a second, violet to blue); on the
	 * ground the dust 1DFCA/B of 1.06-1.2 m at her pelvis (20 a second, 0.17-0.66 s, grey) and 14F9 of 1-1.45 m, and sparks
	 * 183A7 off her feet at 2.4-4.7 m/s (12.6 a second each, gold). Not in her own view (her legs aren't in it: ours).
	 */
	private static void slide(Player p, boolean enemy) {
		if (ownFirstPerson(p)) {
			return;
		}
		Vec3 back = p.getDeltaMovement().multiply(1, 0, 1);
		back = back.lengthSqr() < 1.0E-4 ? Vec3.ZERO : back.normalize().scale(-1);
		for (int i = 0; i < 4; i++) {
			Vec3 jet = point(p, JET + i);
			if (jet == null || Fx.RANDOM.nextFloat() > 0.66f) {
				continue;
			}
			Particle glow = make("mercy_glow", enemy, Palette.SOJ_SLIDE_JET, Palette.SOJ_SLIDE_JET_ENEMY);
			if (glow != null) {
				Fx.add(glow.at(jet).life(0.18f).size(0.69f, 0.5f).alpha(0.7f).fade(0.05f, 0.6f).nearFade(0.5f));
			}
			Particle streak = make("sojourn_burst#3", enemy, Palette.SOJ_SLIDE_STREAK, Palette.SOJ_SLIDE_JET_ENEMY);
			if (streak != null) {
				Fx.add(streak.at(jet).velocity(back.scale(0.5)).life(0.2f).size(range(0.19f, 0.3f), 0.15f).spin(range(0, 6.28f), 2)
					.fade(0.05f, 0.5f).nearFade(0.4f));
			}
		}
		if (!p.onGround()) {
			return;
		}
		Vec3 pelvis = point(p, PELVIS);
		Vec3 ground = p.position().add(0, 0.2, 0);
		Particle smoke = Fx.soft("brigitte_smoke#32", Palette.SOJ_SLIDE_SMOKE);
		if (smoke != null) {
			Vec3 at = pelvis == null ? ground : new Vec3(pelvis.x, ground.y + 0.1, pelvis.z);
			Fx.add(smoke.at(at).velocity(back.scale(0.3).add(0, 0.25, 0)).life(range(0.17f, 0.66f)).size(range(1.06f, 1.2f) * 0.8f, 1.2f)
				.alpha(0.35f).spin(range(0, 6.28f), 1).easeOut().fade(0.05f, 0.7f).nearFade(1.5f));
		}
		Particle dust = Fx.soft("lw_thorn_flame#2", Palette.SOJ_SLIDE_DUST);
		if (dust != null) {
			Fx.add(dust.at(ground).velocity(back.scale(range(0.49f, 1.07f))).life(range(0.15f, 0.3f)).size(range(1.02f, 1.45f) * 0.7f, 1)
				.alpha(0.3f).spin(range(0, 6.28f), 0).fade(0.05f, 0.6f).nearFade(1.5f));
		}
		for (int side = 0; side < 2; side++) {
			Vec3 foot = point(p, FOOT + side);
			if (foot == null) {
				continue;
			}
			for (int i = 0; i < 2; i++) {
				Particle spark = Fx.particle("cat_streak", Palette.SOJ_SLIDE_SPARK);
				if (spark != null) {
					Vec3 way = cone(back.add(0, 0.6, 0), 0.6).scale(range(2.36f, 4.67f));
					Fx.add(spark.at(foot).shape(Shape.STREAK).velocity(way).gravity(9).life(range(0.29f, 0.42f))
						.size(range(0.06f, 0.1f), 0.03f).aspect(0.25f).fade(0.02f, 0.5f).nearFade(0.3f));
				}
			}
		}
	}

	/**
	 * Her slide jump (1E6F #443 330E on hp 002D / 002F, the take-off): a plume of 1DFCA/B smoke (103 a second at 10.6-14.4 m/s
	 * for its first instants: ours, 10 puffs down and back at 3-6 m/s, grey-teal), a 1.36-1.45 m glow 210FD at each point (blue
	 * to mint) and sparks 23F at 1.1-1.74 m/s (ours: 8 a side, a little bigger), once a jump.
	 */
	private static void slideJump(Player p, boolean enemy) {
		HeroAnimator a = HeroAnimator.of(p);
		float t = p.level().getGameTime() / 20f;
		float since = a.since("slide_jump", t);
		if (since < 0 || since > 0.15f) {
			return;
		}
		Float done = JUMP_DONE.get(p.getId());
		float start = t - since;
		if (done != null && Math.abs(done - start) < 0.01f) {
			return;
		}
		JUMP_DONE.put(p.getId(), start);
		boolean own = ownFirstPerson(p);
		for (int side = 0; side < 2; side++) {
			Vec3 at = point(p, BOOT + side);
			if (at == null) {
				at = own ? p.position().add(0, 0.2, 0) : null;
			}
			if (at == null) {
				continue;
			}
			if (!own) {
				Particle glow = make("lucio_glow", enemy, Palette.SOJ_JUMP_GLOW, Palette.SOJ_SLIDE_JET_ENEMY);
				if (glow != null) {
					Fx.add(glow.at(at).life(0.26f).size(range(1.36f, 1.45f), 0.6f).alpha(0.6f).fade(0.02f, 0.6f).nearFade(0.8f));
				}
			}
			for (int i = 0; i < 5; i++) {
				Particle smoke = Fx.soft("brigitte_smoke#32", Palette.SOJ_JUMP_SMOKE);
				if (smoke != null) {
					Fx.add(smoke.at(at).velocity(cone(new Vec3(0, -1, 0), 0.8).scale(range(3, 6))).drag(0.05f).life(range(0.25f, 0.58f))
						.size(range(0.63f, 0.85f), 1.4f).alpha(0.4f).spin(range(0, 6.28f), 1).easeOut().fade(0.03f, 0.7f).nearFade(1.5f));
				}
			}
			for (int i = 0; i < 8; i++) {
				Particle spark = make("sojourn_spark_tail", enemy, Palette.SOJ_JUMP_SPARK, Palette.SOJ_SLIDE_JET_ENEMY);
				if (spark != null) {
					Fx.add(spark.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(1.1f, 1.74f))).gravity(3)
						.life(range(0.2f, 0.4f)).size(range(0.08f, 0.12f), 0.04f).aspect(0.4f).fade(0.02f, 0.5f).nearFade(0.3f));
				}
			}
		}
	}

	// ---- Disruptor Shot ----

	/**
	 * Her Disruptor Shot in flight (ProjectileModelRenderer, each frame; 31EF's child 31F0, 2C8D): a soft glow A17F of 0.7 m
	 * (dark teal to white), line glows C061 of 0.6-1 m (green to ice, 16.6 a second, any turn), motes 4A58 of 0.13-0.17 m drawn
	 * in at 0.27-0.57 m/s (50 a second). The first time it's seen, the launch flash (2C92: A17F 0.4 m and a faint 6 m, 0.15 s).
	 * False without the local pack.
	 */
	public static boolean disruptorFlight(int id, Vec3 at, @Nullable Entity owner) {
		boolean enemy = KirikoFx.enemyOf(owner);
		if (SEEN.put(id, Fx.now()) == null) {
			Particle flash = make("cat_glow_dot", enemy, Palette.SOJ_DISRUPTOR_LAUNCH, Palette.SOJ_DISRUPTOR_LAUNCH_ENEMY);
			if (flash != null) {
				Fx.add(flash.at(at).life(0.15f).size(range(0.39f, 0.43f), 0.3f).fade(0.01f, 0.6f).nearFade(0.5f));
			}
			Particle wide = make("cat_glow_dot", enemy, Palette.SOJ_DISRUPTOR_LAUNCH, Palette.SOJ_DISRUPTOR_LAUNCH_ENEMY);
			if (wide != null) {
				Fx.add(wide.at(at).life(0.15f).size(6, 5).alpha(0.12f).fade(0.01f, 0.6f).nearFade(3));
			}
		}
		Particle core = Fx.attach("disruptor_shot/" + id, at, () -> {
			Particle g = make("cat_glow_dot", enemy, Palette.SOJ_DISRUPTOR_GLOW, Palette.SOJ_DISRUPTOR_GLOW_ENEMY);
			return g == null ? null : g.life(0.15f).size(0.7f, 0.7f).alpha(0.8f).nearFade(1).fade(0.02f, 0.5f);
		});
		if (core == null) {
			return false;
		}
		float dt = Fx.since(-(800_000 + id));
		if (Fx.RANDOM.nextFloat() < 16.6f * dt) {
			Particle line = make("sojourn_line", enemy, Palette.SOJ_DISRUPTOR_CORE, Palette.SOJ_DISRUPTOR_CORE_ENEMY);
			if (line != null) {
				Fx.add(line.at(at).life(range(0.25f, 0.49f)).size(range(0.6f, 1.04f), 0.4f).spin(range(0, 6.28f), range(-3, 3))
					.alpha(0.7f).fade(0.05f, 0.6f).nearFade(0.8f));
			}
		}
		int motes = (int) (50 * dt + Fx.RANDOM.nextFloat());
		for (int i = 0; i < motes; i++) {
			Particle mote = make("sojourn_dot", enemy, Palette.SOJ_DISRUPTOR_MOTE, Palette.SOJ_DISRUPTOR_MOTE_ENEMY);
			if (mote != null) {
				Vec3 off = randomUnit().scale(0.45);
				Fx.add(mote.at(at.add(off)).velocity(off.normalize().scale(-range(0.27f, 0.57f))).life(range(0.28f, 0.76f))
					.size(range(0.13f, 0.17f), 0.05f).fade(0.1f, 0.5f).nearFade(0.5f));
			}
		}
		return true;
	}

	/**
	 * Its field opened (the server's sojourn_disruptor_field: its ticks, Deceleration Field, whose): drawn for its life by
	 * fields(). At once: the flash 4A58 of 4.33 m (0.35 s, green to ice) and five puffs 18B18 of 0.58 m.
	 */
	private static void field(Vec3 at, int owner, int ticks, boolean slows, boolean enemy, float ran, boolean arriving) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 ground = null;
		if (mc.level != null) {
			var hit = mc.level.clip(new ClipContext(at.add(0, 0.2, 0), at.add(0, -SojournStats.FIELD_RADIUS, 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.ANY, net.minecraft.world.phys.shapes.CollisionContext.empty()));
			ground = hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
		}
		Field f = new Field(++keys, at, ground, owner, Fx.now() - ran, ran + Math.max(0.1f, ticks / 20f), slows, enemy);
		FIELDS.add(f);
		Particle flash = arriving ? make("sojourn_dot", enemy, Palette.SOJ_FIELD_FLASH, Palette.SOJ_FIELD_FLASH_ENEMY) : null;
		if (flash != null) {
			Fx.add(flash.at(at).life(0.35f).size(range(4.33f, 4.37f), 3).alpha(0.6f).fade(0.01f, 0.6f).nearFade(3));
		}
		for (int i = 0; arriving && i < 5; i++) {
			Particle puff = make("cat_puff", enemy, Palette.SOJ_FIELD_MOTE, Palette.SOJ_FIELD_MOTE_ENEMY);
			if (puff != null) {
				Fx.add(puff.at(at.add(randomUnit().scale(0.3))).life(range(0.24f, 0.29f)).size(0.58f, 0.3f).fade(0.01f, 0.6f).nearFade(0.5f));
			}
		}
		if (ground != null) {
			// the decal (C21: 299E5 x 3DB8, 6 m, its 3 s), on the ground under it while it lasts
			Particle decal = make("sojourn_field_decal", enemy, Palette.SOJ_FIELD_DECAL, Palette.SOJ_FIELD_DOME_ENEMY);
			if (decal != null) {
				Fx.add(decal.at(ground.add(0, 0.03, 0)).normal(UP).life(f.life).size(6, 6).spin(range(0, 6.28f), 0.3f).alpha(0.8f)
					.fade(0.03f, 0.1f).nearFade(0));
			}
		}
	}

	/**
	 * Each field each tick (2228, held for its 3.5 s): its core DBF of 0.5-0.6 m (teal to pale yellow, 10 a second); line
	 * glows C061 of 0.96-1.4 m inside it (16.6 a second, green to ice); motes 4A58 drawn in from its edge (75 a second, ours
	 * a third); its sphere 1B761 at its 4 m (dark teal to mint: the mesh cat_sphere with the band 1C7A; at 0.18 alpha a milky solid dome hid the field, played: 0.06); with
	 * Deceleration Field 6C02's 8 m sphere 1385 every 1.25 s (navy to ice). Its flame ring E22E (11.8 m, 29132 x 29149) isn't
	 * drawn: no mesh.
	 */
	private static void fields(Minecraft mc, float now) {
		float[] sphere = Fx.mesh("cat_sphere");
		for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) {
			Field f = it.next();
			float age = now - f.born;
			if (age >= f.life) {
				it.remove();
				continue;
			}
			boolean enemy = f.enemy;
			Particle core = make("lucio_glow", enemy, Palette.SOJ_FIELD_CORE, Palette.SOJ_FIELD_CORE_ENEMY);
			if (core != null && Fx.RANDOM.nextFloat() < 0.5f) {
				Fx.add(core.at(f.centre).life(range(0.5f, 0.7f)).size(range(0.52f, 0.59f), 0.3f).alpha(0.8f).fade(0.1f, 0.6f).nearFade(0.5f));
			}
			if (Fx.RANDOM.nextFloat() < 0.83f) {
				Particle line = make("sojourn_line", enemy, Palette.SOJ_FIELD_SWIRL, Palette.SOJ_FIELD_SWIRL_ENEMY);
				if (line != null) {
					Fx.add(line.at(f.centre.add(randomUnit().scale(range(0.3f, 2.5f)))).life(range(0.25f, 0.49f))
						.size(range(0.96f, 1.39f), 0.5f).spin(range(0, 6.28f), range(-2, 2)).alpha(0.6f).fade(0.05f, 0.6f).nearFade(1));
				}
			}
			for (int i = 0; i < 1 + (Fx.RANDOM.nextFloat() < 0.25f ? 1 : 0); i++) {
				Particle mote = make("sojourn_dot", enemy, Palette.SOJ_FIELD_MOTE, Palette.SOJ_FIELD_MOTE_ENEMY);
				if (mote != null) {
					Vec3 off = randomUnit().scale(range(1.5f, (float) SojournStats.FIELD_RADIUS));
					Fx.add(mote.at(f.centre.add(off)).velocity(off.normalize().scale(-range(0.8f, 1.6f))).life(range(0.65f, 1.14f))
						.size(range(0.13f, 0.17f) * 1.5f, 0.1f).fade(0.1f, 0.5f).nearFade(0.5f));
				}
			}
			float d = (float) SojournStats.FIELD_RADIUS * 2;
			if (mc.level.getGameTime() % 10 == 0) {
				Particle dome = sphere == null ? make("mercy_glow", enemy, Palette.SOJ_FIELD_DOME, Palette.SOJ_FIELD_DOME_ENEMY)
					: make("cat_band", enemy, Palette.SOJ_FIELD_DOME, Palette.SOJ_FIELD_DOME_ENEMY);
				if (dome != null) {
					dome.life(Math.min(0.6f, f.life - age)).alpha(0.06f).fade(0.3f, 0.4f).nearFade(0);
					Fx.add(sphere != null ? dome.at(f.centre.subtract(0, d * 0.51, 0)).mesh(sphere, new Vec3(1, 0, 0)).size(d, d)
						: dome.at(f.centre).shape(Shape.DOME).size(d, d));
				}
			}
			if (f.slows && sphere != null && (int) (age / 1.25f) != (int) ((age - 0.05f) / 1.25f) || f.slows && age < 0.05f) {
				Particle decel = make("mercy_noise_b", enemy, Palette.SOJ_DECEL, Palette.SOJ_DECEL_ENEMY);
				if (decel != null && sphere != null) {
					Fx.add(decel.at(f.centre.subtract(0, d * 0.51, 0)).mesh(sphere, new Vec3(1, 0, 0)).life(1.25f).size(d, d).alpha(0.15f)
						.fade(0.2f, 0.4f).nearFade(0));
				}
			}
		}
	}

	// ---- what her sounds follow (dev.overcraft.client.SojournSounds) ----

	/** Her live fields, "field/<key>/<her entity id>" -> centre: their loops and end (SojournSounds). */
	public static Map<String, Vec3> fieldsSounding() {
		Map<String, Vec3> out = new HashMap<>();
		for (Field f : FIELDS) {
			out.put("field/" + f.key + "/" + f.owner, f.centre);
		}
		return out;
	}

	/** This entity is inside an enemy Sojourn's field (its 4 m, the nearest point of its box: Disruptor.inside's rule). */
	public static boolean inEnemyField(LivingEntity e) {
		AABB box = e.getBoundingBox();
		for (Field f : FIELDS) {
			if (!f.enemy || Fx.now() - f.born < (float) SojournStats.FIELD_DELAY) {
				continue;
			}
			double x = Math.clamp(f.centre.x, box.minX, box.maxX), y = Math.clamp(f.centre.y, box.minY, box.maxY),
				z = Math.clamp(f.centre.z, box.minZ, box.maxZ);
			if (f.centre.distanceToSqr(x, y, z) <= SojournStats.FIELD_RADIUS * SojournStats.FIELD_RADIUS) {
				return true;
			}
		}
		return false;
	}

	/** Her moments from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Minecraft mc = Minecraft.getInstance();
		switch (e.effect()) {
			case "sojourn_tracer" -> tracer(mc.level == null ? null : mc.level.getEntity(e.entity()), at, at.add(dir), e.variant(), enemy);
			case "sojourn_round_hit" -> roundHit(at, dir, enemy);
			case "sojourn_charged_hit" -> chargedHit(at, dir, enemy);
			case "sojourn_disruptor_field" -> field(at, e.entity(), e.variant() & (Disruptor.FX_SLOWS - 1),
				(e.variant() & Disruptor.FX_SLOWS) != 0, enemy, 0, true);
			// for one who has just come to see it (Effects.showHeld, #96): the field, its time run in dx, no flash, one a place
			case "sojourn_disruptor_field_state" -> {
				FIELDS.removeIf(f -> f.centre.distanceToSqr(at) < 0.01);
				field(at, e.entity(), e.variant() & (Disruptor.FX_SLOWS - 1), (e.variant() & Disruptor.FX_SLOWS) != 0, enemy, e.dx(), false);
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	public static void clear() {
		POSED.clear();
		FIELDS.clear();
		SEEN.clear();
		JUMP_DONE.clear();
		RELOAD_DONE.clear();
	}
}
