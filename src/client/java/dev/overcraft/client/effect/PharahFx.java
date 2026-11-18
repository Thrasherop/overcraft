package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.entity.PharahRocketEntity;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.EffectPayload;
import dev.overcraft.pharah.Pharah;
import dev.overcraft.pharah.PharahStats;
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
 * Pharah's effects (tools/reference/pharah-effects.md, read from her effects' data 2026-11-17; #150: her rockets were Minecraft's
 * explosions and her jets drew nothing). Her rockets are gold to her side, a redder twin to her enemies; Concussive Blast blue to her
 * side, red to her enemies (H rows, M roles, as Tracer's). Her Hover Jets burn blue (H; the orange-red second rows on them may be an
 * enemy twin, L, so everyone sees the blue), Jump Jet and Jet Dash orange (H). From the server: the launches (0091 / 012F), the bursts
 * (0115 / 0081 / 0116), the jumps (00F2 / 5910), Barrage's pod flashes (008A) and the fuel running out (0185, L). Each frame: the hover
 * (00F3) and Barrage's pods from her synced look (HeroManager.PHARAH_STATE: Pharah.LOOK_*) at her posed hardpoints, the jumps' flames and
 * smoke for their 0.33 / 0.67 s, the blast's debris; the rockets in flight (0078 / 0080 / 0132) from their renderer. Sizes in metres, times
 * in seconds; H read in the data, M inferred, ours where the data gives nothing or we drew it smaller.
 */
public final class PharahFx {
	/** Its art, made ahead as the hero is picked (HeroFxArt). */
	public static final List<EffectArt.Art> ART = List.of(
		new EffectArt.Art("sombra_impact_burst", Palette.PH_MUZZLE, false),
		new EffectArt.Art("pharah_flame_noise", Palette.PH_MUZZLE_FLAME, false),
		new EffectArt.Art("spark#4", Palette.PH_MUZZLE_SPARK, false),
		new EffectArt.Art("cat_soft", Palette.PH_ROCKET_GLOW, false),
		new EffectArt.Art("pharah_flare", Palette.PH_ROCKET_FLARE, false),
		new EffectArt.Art("pharah_flame_noise", Palette.PH_ROCKET_FLAME, false),
		new EffectArt.Art("torb_trail", Palette.PH_ROCKET_TRAIL, true),
		new EffectArt.Art("cat_noise", Palette.PH_ROCKET_STREAK, false),
		new EffectArt.Art("dva_missile_fire#12", Palette.PH_BLAST_FIRE, false),
		new EffectArt.Art("pharah_streak", Palette.PH_BLAST_FLASH, false),
		new EffectArt.Art("pharah_flare_line", Palette.PH_BLAST_FLARE, false),
		new EffectArt.Art("spark#4", Palette.PH_BLAST_SPARK, false),
		new EffectArt.Art("cat_soft", Palette.PH_BLAST_DEBRIS, false),
		new EffectArt.Art("pharah_smoke_ribbon", Palette.PH_DEBRIS_TRAIL, true),
		new EffectArt.Art("smoke#48", Palette.PH_BLAST_SMOKE, true),
		new EffectArt.Art("glow", Palette.PH_CONC_LAUNCH, false),
		new EffectArt.Art("juno_hit_ring#4", Palette.PH_CONC_LAUNCH_B, false),
		new EffectArt.Art("cat_soft", Palette.PH_CONC_GLOW, false),
		new EffectArt.Art("spark#4", Palette.PH_CONC_GLOW, false),
		new EffectArt.Art("pharah_flare", Palette.PH_CONC_HALO, false),
		new EffectArt.Art("dva_solid", Palette.PH_CONC_TRAIL, false),
		new EffectArt.Art("torb_trail", Palette.PH_CONC_SMOKE_TRAIL, true),
		new EffectArt.Art("pharah_wave_band", Palette.PH_CONC_WAVE, false),
		new EffectArt.Art("noise", Palette.PH_CONC_SHELL, false),
		new EffectArt.Art("pharah_swirl_noise", Palette.PH_CONC_SWIRL, false),
		new EffectArt.Art("cat_cone_line", Palette.PH_CONC_DOME, false),
		new EffectArt.Art("juno_cloud_noise", Palette.PH_CONC_CORE, false),
		new EffectArt.Art("glow", Palette.PH_CONC_CORE, false),
		new EffectArt.Art("spark#4", Palette.PH_CONC_CORE, false),
		new EffectArt.Art("pharah_jet", Palette.PH_HOVER_JET, false),
		new EffectArt.Art("mercy_soft", Palette.PH_HOVER_FLAME, false),
		new EffectArt.Art("mercy_soft", Palette.PH_HOVER_SPARK, false),
		new EffectArt.Art("pharah_flare", Palette.PH_HOVER, false),
		new EffectArt.Art("spark#4", Palette.PH_HOVER_FLAME_B, false),
		new EffectArt.Art("pharah_flare", Palette.PH_JUMP_FLASH, false),
		new EffectArt.Art("pharah_jet_ring#14", Palette.PH_JUMP_FLASH, false),
		new EffectArt.Art("mercy_soft", Palette.PH_JUMP_FLAME, false),
		new EffectArt.Art("brigitte_smoke#32", Palette.PH_JUMP_SMOKE, true),
		new EffectArt.Art("lucio_spark", Palette.PH_MINI_FLAME, false),
		new EffectArt.Art("cat_slash", Palette.PH_MINI_TRAIL, false),
		new EffectArt.Art("dva_missile_fire#12", Palette.PH_MINI_HIT, false),
		new EffectArt.Art("lucio_soft", Palette.PH_MINI_HIT_SPARK, false),
		new EffectArt.Art("sombra_smoke", Palette.PH_MINI_HIT_SMOKE, true),
		new EffectArt.Art("mercy_soft", Palette.PH_POD, false),
		new EffectArt.Art("pharah_pod_flash#3", Palette.PH_POD_FLASH, false),
		new EffectArt.Art("mercy_glow", Palette.PH_POD_FLASH, false),
		new EffectArt.Art("pharah_pod_glow", Palette.PH_POD_GLOW, false));
	public static final List<EffectArt.Art> ART_ENEMY = List.of(
		new EffectArt.Art("sombra_impact_burst", Palette.PH_MUZZLE_ENEMY, false),
		new EffectArt.Art("pharah_flame_noise", Palette.PH_MUZZLE_FLAME_ENEMY, false),
		new EffectArt.Art("spark#4", Palette.PH_MUZZLE_SPARK_ENEMY, false),
		new EffectArt.Art("torb_trail", Palette.PH_ROCKET_TRAIL_ENEMY, true),
		new EffectArt.Art("dva_missile_fire#12", Palette.PH_BLAST_FIRE_ENEMY, false),
		new EffectArt.Art("pharah_streak", Palette.PH_BLAST_FLASH_ENEMY, false),
		new EffectArt.Art("pharah_flare_line", Palette.PH_BLAST_FLARE_ENEMY, false),
		new EffectArt.Art("glow", Palette.PH_CONC_LAUNCH_ENEMY, false),
		new EffectArt.Art("juno_hit_ring#4", Palette.PH_CONC_LAUNCH_B_ENEMY, false),
		new EffectArt.Art("cat_soft", Palette.PH_CONC_GLOW_ENEMY, false),
		new EffectArt.Art("spark#4", Palette.PH_CONC_GLOW_ENEMY, false),
		new EffectArt.Art("pharah_flare", Palette.PH_CONC_HALO_ENEMY, false),
		new EffectArt.Art("dva_solid", Palette.PH_CONC_TRAIL_ENEMY, false),
		new EffectArt.Art("torb_trail", Palette.PH_CONC_SMOKE_TRAIL_ENEMY, true),
		new EffectArt.Art("pharah_wave_band", Palette.PH_CONC_WAVE_ENEMY, false),
		new EffectArt.Art("noise", Palette.PH_CONC_SHELL_ENEMY, false),
		new EffectArt.Art("pharah_swirl_noise", Palette.PH_CONC_SWIRL_ENEMY, false),
		new EffectArt.Art("cat_cone_line", Palette.PH_CONC_DOME_ENEMY, false),
		new EffectArt.Art("juno_cloud_noise", Palette.PH_CONC_CORE_ENEMY, false),
		new EffectArt.Art("glow", Palette.PH_CONC_CORE_ENEMY, false),
		new EffectArt.Art("spark#4", Palette.PH_CONC_CORE_ENEMY, false),
		new EffectArt.Art("dva_missile_fire#12", Palette.PH_MINI_HIT_ENEMY, false),
		new EffectArt.Art("pharah_pod_flash#3", Palette.PH_POD_FLASH_ENEMY, false),
		new EffectArt.Art("mercy_glow", Palette.PH_POD_FLASH_ENEMY, false));

	/**
	 * Her posed points (HeroModelRenderer): the launcher's muzzle hp 0006 (pharah_weapon.glb), then her own: the six jets 002C-0031
	 * (the hover; the jumps use 002C-002F), Barrage's pods 0034 / 0035 (shoulders) and 0036 / 0037 (hips), her left wrist 0038
	 * (Concussive's muzzle), her body 0004 (the jumps' flash) (H ids from her map and the cards, M roles; all in pharah.glb).
	 */
	public static final String[] POINTS = {"hardpoint_0006", "hardpoint_002C", "hardpoint_002D", "hardpoint_002E", "hardpoint_002F",
		"hardpoint_0030", "hardpoint_0031", "hardpoint_0034", "hardpoint_0035", "hardpoint_0036", "hardpoint_0037", "hardpoint_0038",
		"hardpoint_0004"};
	/** The first of POINTS that are on her body (the rest is on her launcher). */
	public static final int GUN_POINTS = 1;
	private static final int MUZZLE = 0, JET = 1, JETS = 6, JUMP_JETS = 4, POD = 7, WRIST = 11, BODY = 12;
	/** Barrage's pods in turn (PharahStats.BARRAGE_PODS: hp 0034 0035 0037 0036 0037 0035 0034 0036, D 002B #21) as POINTS' indices. */
	private static final int[] POD_ORDER = {POD, POD + 1, POD + 3, POD + 2, POD + 3, POD + 1, POD, POD + 2};
	/** BARRAGE_PODS' entry for each of POINTS' pods 0034-0037 (their rest, the fallback when she isn't posed). */
	private static final int[] POD_REST = {0, 1, 3, 2};
	/** Her jets' rest when she isn't posed (ours: from her feet in her facing's frame, right, up, ahead: her back's pack). */
	private static final double[][] JET_REST = {{-0.2, 1.25, -0.3}, {0.2, 1.25, -0.3}, {-0.13, 1.0, -0.28}, {0.13, 1.0, -0.28},
		{-0.27, 1.05, -0.22}, {0.27, 1.05, -0.22}};
	private static final Vec3 UP = new Vec3(0, 1, 0), DOWN = new Vec3(0, -1, 0);

	private static final class Posed {
		Vec3[] points = new Vec3[0];
		long tick;
	}

	private static final Map<Integer, Posed> POSED = new HashMap<>();

	/** An emitter's rate kept between frames: how many to make this frame for so many a second (TorbjornFx's). */
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

	/** Each Pharah's held look: her emitters (the hover's per jet, Barrage's), whether she hovered last frame (the start's flash). */
	private static final class Look {
		final Rate[] rates = new Rate[JETS * 3 + 2];
		boolean hovering;

		Look() {
			for (int i = 0; i < rates.length; i++) {
				rates[i] = new Rate();
			}
		}
	}

	private static final Map<Integer, Look> LOOKS = new HashMap<>();

	/** A Jump Jet or Jet Dash under way (00F2 / 5910): whose, from when (Fx.now()), its emitters. */
	private static final class Jet {
		final int entity;
		final float start;
		final Rate flames = new Rate(), smoke = new Rate();

		Jet(int entity, float start) {
			this.entity = entity;
			this.start = start;
		}
	}

	private static final List<Jet> JETS_GOING = new ArrayList<>();

	/** A piece of the rocket blast's debris (0115's 1F35 x 22A8, each with 0554's smoke ribbon), flown here so its ribbon is fed. */
	private static final class Debris {
		final int id;
		final Vec3 from, velocity;
		final float born, life;

		Debris(int id, Vec3 from, Vec3 velocity, float born, float life) {
			this.id = id;
			this.from = from;
			this.velocity = velocity;
			this.born = born;
			this.life = life;
		}
	}

	private static final List<Debris> DEBRIS = new ArrayList<>();
	/** The debris ribbons' ids (Fx.trail's), below every entity's and the rockets' second ribbons (ours). */
	private static int debrisSerial;

	private PharahFx() {
	}

	// ---- her posed points (HeroModelRenderer) ----

	public static void posed(Entity pharah, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(pharah.getId(), id -> new Posed());
		p.points = points;
		p.tick = pharah.level().getGameTime();
	}

	/** Her posed point this tick or the last (null in her own first person, unposed, or without that point). */
	private static @Nullable Vec3 point(@Nullable Entity pharah, int which) {
		if (pharah == null || ownFirstPerson(pharah)) {
			return null;
		}
		Posed p = POSED.get(pharah.getId());
		return p == null || pharah.level().getGameTime() - p.tick > 1 || which >= p.points.length ? null : p.points[which];
	}

	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/** A point from her feet in her body's facing (right, up, ahead), for when she isn't posed. */
	private static Vec3 rest(Entity p, double right, double up, double ahead) {
		Vec3 fwd = Vec3.directionFromRotation(0, p.getVisualRotationYInDegrees());
		Vec3 side = fwd.cross(UP).normalize();
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		return p.getPosition(partial).add(side.scale(right)).add(0, up, 0).add(fwd.scale(ahead));
	}

	/** Jet j (0-5) as posed, else its rest. */
	private static Vec3 jet(Entity p, int j) {
		Vec3 posed = point(p, JET + j);
		if (posed != null) {
			return posed;
		}
		double[] r = JET_REST[j];
		return rest(p, r[0], r[1], r[2]);
	}

	/** Pod k (POINTS index POD + k) as posed, else its rest (BARRAGE_PODS: x her left, so right is -x). */
	private static Vec3 pod(Entity p, int k) {
		Vec3 posed = point(p, POD + k);
		if (posed != null) {
			return posed;
		}
		double[] r = PharahStats.BARRAGE_PODS[POD_REST[k]];
		return rest(p, -r[0], r[1], r[2]);
	}

	/** Her muzzle: in her own first person the view model's as drawn, else posed (hp 0006), else where the server shot from. */
	private static Vec3 muzzle(@Nullable Entity pharah, Vec3 fallback) {
		Minecraft mc = Minecraft.getInstance();
		if (ownFirstPerson(pharah)) {
			Vector3f view = dev.overcraft.client.model.HeroModelRenderer.viewMuzzle();
			return view == null ? fallback : mc.gameRenderer.mainCamera().position().add(view.x, view.y, view.z);
		}
		Vec3 posed = point(pharah, MUZZLE);
		return posed != null ? posed : fallback;
	}

	// ---- each frame ----

	/** Each frame: every Pharah's hover and Barrage pods, the jumps under way, the blast's debris. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			JETS_GOING.clear();
			DEBRIS.clear();
			LOOKS.clear();
			return;
		}
		float now = Fx.now();
		DEBRIS.removeIf(d -> {
			float t = now - d.born;
			if (t > d.life || t < -1) {
				return true;
			}
			debris(d, t);
			return false;
		});
		JETS_GOING.removeIf(j -> {
			Entity e = mc.level.getEntity(j.entity);
			float t = now - j.start;
			if (e == null || !e.isAlive() || t > 0.67f || t < -1) {
				return true;
			}
			jetGoing(e, j, t);
			return false;
		});
		LOOKS.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		POSED.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		for (Player p : mc.level.players()) {
			Integer look = p.getAttached(HeroManager.PHARAH_STATE);
			if (look == null || look == 0 || !p.isAlive()) {
				Look l = LOOKS.get(p.getId());
				if (l != null) {
					l.hovering = false;
				}
				continue;
			}
			Look l = LOOKS.computeIfAbsent(p.getId(), id -> new Look());
			boolean hover = (look & Pharah.LOOK_HOVER) != 0;
			// her own first person: her jets sit under the camera, so nothing of them here (ours, as Torbjorn's vents)
			if (!ownFirstPerson(p)) {
				if (hover) {
					hover(p, l, !l.hovering);
				}
				if ((look & Pharah.LOOK_BARRAGE) != 0) {
					barrageHeld(p, l);
				}
			}
			l.hovering = hover;
		}
	}

	// ---- Hover Jets (00F3) ----

	/**
	 * Hovering (00F3, H; at each of the six jets 002C-0031, 1 s looping): the jet cone 5233 60 a second + 8 (ours 20), 0.21 +- 0.13 s,
	 * 0.62 > 0.19 m, in a 40 degree cone (PH_HOVER_JET, 1281 r2); the flame 139C 15 a second (ours 10), 0.2-0.24 s, 1.5 m/s, 0.58 m
	 * (ours 0.5; 4DC7 r2); sparks 139C 60 a second (ours 10), 0.1-0.14 s, 2-2.9 m/s, 0.19 m (ramp 3200 x 00A0 r2); at the start a flash
	 * 9EE 1.5 m, 0.29 s (ours: once at the jets' middle at 0.35 alpha, not six stacked). Down from her (ours: the data's along each jet).
	 */
	private static void hover(Player p, Look l, boolean start) {
		Vec3 middle = Vec3.ZERO;
		for (int j = 0; j < JETS; j++) {
			Vec3 at = jet(p, j).add(0, -0.05, 0);
			middle = middle.add(at.scale(1.0 / JETS));
			int cones = l.rates[j].take(20);
			for (int i = 0; i < cones; i++) {
				Particle c = Fx.particle("pharah_jet", Palette.PH_HOVER_JET);
				if (c != null) {
					Vec3 dir = cone(DOWN, 20);
					Fx.add(c.at(at.add(dir.scale(0.15))).shape(Shape.STREAK).velocity(dir.scale(range(1.5f, 2.5f))).life(range(0.08f, 0.34f))
						.size(0.62f * 0.7f, 0.19f * 0.7f).aspect(0.45f).alpha(0.8f).fade(0.05f, 0.6f).nearFade(0.8f));
				}
			}
			int flames = l.rates[JETS + j].take(10);
			for (int i = 0; i < flames; i++) {
				Particle f = Fx.particle("mercy_soft", Palette.PH_HOVER_FLAME);
				if (f != null) {
					Fx.add(f.at(at).velocity(cone(DOWN, 10).scale(1.5f)).life(range(0.2f, 0.24f)).size(0.5f, 0.2f).spin(range(0, 6.28f), 0)
						.alpha(0.8f).fade(0.05f, 0.6f).nearFade(0.8f));
				}
			}
			int sparks = l.rates[JETS * 2 + j].take(10);
			for (int i = 0; i < sparks; i++) {
				Particle s = Fx.particle("mercy_soft", Palette.PH_HOVER_SPARK);
				if (s != null) {
					Fx.add(s.at(at).shape(Shape.STREAK).velocity(cone(DOWN, 30).scale(range(2, 2.9f))).life(range(0.1f, 0.14f))
						.size(0.19f, 0.05f).aspect(0.3f).fade(0.05f, 0.5f).nearFade(0.5f));
				}
			}
		}
		if (start) {
			Particle flash = Fx.particle("pharah_flare", Palette.PH_HOVER);
			if (flash != null) {
				Fx.add(flash.at(middle).life(0.29f).size(1.5f, 1.2f).alpha(0.35f).fade(0.02f, 0.6f).nearFade(1.5f));
			}
		}
	}

	/**
	 * The fuel running out (0185, L which event plays it): 4-6 drips 1F5B at 2-3.3 m/s (ours 2, as spark streaks), 90 sparks EA42 at
	 * 2.5-3 m/s (ours 10) at her jets, red-orange (PH_HOVER_FLAME_B: 5311 r0; the data's 3DEB r0 red).
	 */
	private static void hoverEmpty(@Nullable Entity p, Vec3 at) {
		if (p == null || ownFirstPerson(p)) {
			return;
		}
		for (int i = 0; i < 10; i++) {
			spark(jet(p, Fx.RANDOM.nextInt(JETS)), cone(DOWN, 50).scale(range(2.5f, 3)), Palette.PH_HOVER_FLAME_B, range(0.2f, 0.35f),
				0.04f, 2);
		}
		for (int i = 0; i < 2; i++) {
			spark(jet(p, Fx.RANDOM.nextInt(JETS)), cone(DOWN, 20).scale(range(2, 3.3f)), Palette.PH_HOVER_FLAME_B, 0.5f, 0.06f, 9.8f);
		}
	}

	// ---- Jump Jet (00F2) and Jet Dash (5910: the same renderers, sizes and times, H) ----

	/**
	 * Its start (00F2 / 5910, H): the flash 9EE x 31C0 1.82 m, 0.3 s at her body 0004 (PH_JUMP_FLASH; at 0.5 alpha, ours); the ring
	 * 3AE1 (14 frames) on the disc 1E8E at size 1.0 (1.28 m), 0.34 s: flat under her for Jump Jet, upright behind her for Jet Dash
	 * (ours: the data's ring is on her jets' hardpoint). Then 0.33 s of flames and 0.67 s of smoke (jetGoing).
	 */
	private static void jumpStart(@Nullable Entity p, Vec3 feet, Vec3 way, boolean dash) {
		if (p == null) {
			return;
		}
		boolean own = ownFirstPerson(p);
		Vec3 body = point(p, BODY);
		if (body == null) {
			body = feet.add(0, 0.9, 0);
		}
		if (!own) {
			Particle flash = Fx.particle("pharah_flare", Palette.PH_JUMP_FLASH);
			if (flash != null) {
				Fx.add(flash.at(body.add(0, -0.3, 0)).life(0.3f).size(1.82f, 1.4f).alpha(0.5f).fade(0.02f, 0.6f).nearFade(1.5f));
			}
		}
		float[] disc = Fx.mesh("pharah_jet_disc");
		Particle ring = disc == null ? null : Fx.particle("pharah_jet_ring#14", Palette.PH_JUMP_FLASH);
		if (ring != null) {
			Vec3 w = new Vec3(way.x, 0, way.z);
			if (dash && w.lengthSqr() > 1.0E-6 && !own) {
				// upright, facing the dash, half a metre behind her middle; the disc's normal (its Y) along the dash
				w = w.normalize();
				ring.mesh(disc, w.cross(UP).normalize(), w).at(body.subtract(w.scale(0.5)));
			} else {
				ring.mesh(disc, new Vec3(1, 0, 0)).at(feet.add(0, 0.05, 0));
			}
			Fx.add(ring.life(0.34f).size(1.28f, 1.28f).loop(14 / 0.34f).alpha(0.8f).fade(0.02f, 0.5f));
		}
		JETS_GOING.removeIf(j -> j.entity == p.getId());
		JETS_GOING.add(new Jet(p.getId(), Fx.now()));
	}

	/**
	 * A jump under way, t s in (00F2 / 5910, H): the flames 139C x 31C0 70 a second for 0.33 s (ours 40), 4 m/s, 0.55 m, 0.2-0.24 s
	 * from the jets 002C-002F (PH_JUMP_FLAME); the smoke 1DFCB 180 a second for 0.67 s (ours 30), 0.7 m out along the jet, 0.86-1.36 s,
	 * 2-3 m/s, 0.6 +- 0.58 m (PH_JUMP_SMOKE, B2B1 r1). Away from her motion (ours: the data's along each jet). In her own first person,
	 * the smoke only (ours).
	 */
	private static void jetGoing(Entity p, Jet j, float t) {
		Vec3 moved = p.position().subtract(p.xo, p.yo, p.zo);
		Vec3 back = moved.lengthSqr() < 1.0E-4 ? DOWN : moved.normalize().scale(-1);
		boolean own = ownFirstPerson(p);
		if (t < 0.33f && !own) {
			int flames = j.flames.take(40);
			for (int i = 0; i < flames; i++) {
				Particle f = Fx.particle("mercy_soft", Palette.PH_JUMP_FLAME);
				if (f != null) {
					Fx.add(f.at(jet(p, Fx.RANDOM.nextInt(JUMP_JETS))).velocity(cone(back, 15).scale(4)).life(range(0.2f, 0.24f))
						.size(0.55f, 0.2f).spin(range(0, 6.28f), 0).alpha(0.9f).fade(0.05f, 0.6f).nearFade(0.8f));
				}
			}
		}
		int smoke = j.smoke.take(30);
		for (int i = 0; i < smoke; i++) {
			Particle s = Fx.soft("brigitte_smoke#32", Palette.PH_JUMP_SMOKE);
			if (s != null) {
				Vec3 from = own ? p.position() : jet(p, 0).add(back.scale(0.7));
				float size = Math.max(0.2f, 0.6f + (Fx.RANDOM.nextFloat() - 0.5f) * 1.16f);
				Fx.add(s.at(from).velocity(cone(back, 30).scale(range(2, 3))).drag(0.2f).life(range(0.86f, 1.36f)).size(size * 0.6f, size)
					.spin(range(0, 6.28f), range(-1, 1)).alpha(0.4f).fade(0.1f, 0.6f).nearFade(1.5f));
			}
		}
	}

	// ---- Barrage (002B; the pods: her 3P clip 008A) ----

	/**
	 * Barrage held (008A, H): flames 139C 70 a second from the jets 002C-0031 (ours 20), 2 m/s, 0.55 m (PH_POD: ramp 31C0 x 8); at each
	 * pod the blue glow 27BFD 0.2 m (20 a second in the data; ours one held glow at 0.5 alpha, PH_POD_GLOW 809D r6).
	 */
	private static void barrageHeld(Player p, Look l) {
		int flames = l.rates[JETS * 3].take(20);
		for (int i = 0; i < flames; i++) {
			Particle f = Fx.particle("mercy_soft", Palette.PH_POD);
			if (f != null) {
				Fx.add(f.at(jet(p, Fx.RANDOM.nextInt(JETS))).velocity(cone(DOWN, 15).scale(2)).life(range(0.15f, 0.25f)).size(0.55f, 0.2f)
					.spin(range(0, 6.28f), 0).alpha(0.8f).fade(0.05f, 0.6f).nearFade(0.8f));
			}
		}
		for (int k = 0; k < 4; k++) {
			Vec3 at = pod(p, k);
			Particle g = Fx.attach("pharah_pod/" + p.getId() + "/" + k, at, () -> {
				Particle s = Fx.particle("pharah_pod_glow", Palette.PH_POD_GLOW);
				return s == null ? null : s.life(0.3f).fade(0.1f, 0.5f).nearFade(0.5f);
			});
			if (g != null) {
				g.size(0.2f, 0.2f).alpha(0.5f);
			}
		}
	}

	/**
	 * A Barrage rocket leaving its pod (variant: the pod in turn, BARRAGE_PODS): the flash 5E9C (3 frames) 0.25-0.36 m, 0.15 s and the
	 * flash 242 0.3 m (H sizes; PH_POD_FLASH 7CE2 r2 / FAB3 r2). The data's 5 a second per pod for the clip; ours one per rocket.
	 */
	private static void podFlash(@Nullable Entity p, Vec3 at, int variant, boolean enemy) {
		Vec3 from = p == null ? at : point(p, POD_ORDER[Math.floorMod(variant, POD_ORDER.length)]);
		if (from == null) {
			if (ownFirstPerson(p)) {
				return;
			}
			from = at;
		}
		Palette colour = enemy ? Palette.PH_POD_FLASH_ENEMY : Palette.PH_POD_FLASH;
		Particle f = Fx.particle("pharah_pod_flash#3", colour);
		if (f != null) {
			Fx.add(f.at(from).life(0.15f).size(range(0.25f, 0.36f), 0.2f).spin(range(0, 6.28f), 0).fade(0.01f, 0.5f).nearFade(0.5f));
		}
		Particle g = Fx.particle("mercy_glow", colour);
		if (g != null) {
			Fx.add(g.at(from).life(0.12f).size(0.3f, 0.2f).alpha(0.7f).fade(0.01f, 0.5f).nearFade(0.5f));
		}
	}

	// ---- the Rocket Launcher (0023) and Rocket Salvo's launch ----

	/**
	 * The launch 0091 at the launcher's 0006 (H): flashes 220A 0.5 m in her view / 0.96 m for others, 0.2-0.29 s, stretched along the
	 * shot (x7.5 in the data; ours x1.5 on a quarter-wide streak: the data's read as a 7 m line); the flame 299E6 0.49 / 1.12 m, 0.21-0.29
	 * s; sparks 1F35 16-20 at 4-7.4 m/s in a 75 degree cone (ours 6). In her own first person a quarter the size again (ours, after
	 * Tracer's: the data's 1P size still covered the launcher). {@code half}: Rocket Salvo's (ours: 7769 has no launch effect, H).
	 */
	private static void launch(@Nullable Entity p, Vec3 at, Vec3 dir, boolean half, boolean enemy) {
		boolean own = ownFirstPerson(p);
		Vec3 from = muzzle(p, at);
		Vec3 fwd = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
		float k = (own ? 0.25f : 1) * (half ? 0.5f : 1);
		float near = own ? 0.2f : 0.6f;
		Particle flash = Fx.particle("sombra_impact_burst", enemy ? Palette.PH_MUZZLE_ENEMY : Palette.PH_MUZZLE);
		if (flash != null) {
			float len = (own ? 0.5f : 0.96f) * 1.5f * k;
			Fx.add(flash.at(from.add(fwd.scale(len * 0.4f))).shape(Shape.STREAK).velocity(fwd).life(range(0.2f, 0.29f)).size(len, len * 0.8f)
				.aspect(0.25f).fade(0.01f, 0.6f).nearFade(near));
		}
		Particle flame = Fx.particle("pharah_flame_noise", enemy ? Palette.PH_MUZZLE_FLAME_ENEMY : Palette.PH_MUZZLE_FLAME);
		if (flame != null) {
			float len = (own ? 0.49f : 1.12f) * k;
			Fx.add(flame.at(from.add(fwd.scale(len * 0.3f))).shape(Shape.STREAK).velocity(fwd.scale(1.5f)).life(range(0.21f, 0.29f))
				.size(len, len * 0.7f).aspect(0.45f).fade(0.02f, 0.6f).nearFade(near));
		}
		for (int i = 0; i < (half ? 3 : 6); i++) {
			spark(from, cone(fwd, 37).scale(range(4, 7.4f)), enemy ? Palette.PH_MUZZLE_SPARK_ENEMY : Palette.PH_MUZZLE_SPARK,
				range(0.12f, 0.25f), 0.03f * (own ? 0.5f : 1), 0);
		}
	}

	// ---- her projectiles in flight (from ProjectileModelRenderer) ----

	/** Each frame for each of her projectiles: 0078 for the launcher's rocket, 0132 for Concussive Blast, 0080 for Barrage's and Salvo's. */
	public static void rocketFlight(int id, Vec3 at, Vec3 velocity, float dt, PharahRocketEntity.Kind kind, boolean enemy) {
		Vec3 fwd = velocity.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : velocity.normalize();
		switch (kind) {
			case ROCKET -> rocket(id, at, fwd, dt, enemy);
			case CONCUSSIVE -> concussive(id, at, fwd, dt, enemy);
			default -> mini(id, at, fwd);
		}
	}

	/**
	 * The rocket (0078, H): the glow 4C09 0.4 m on its tail (33D6 r1); the flare 9EE 1 m (ours at 0.4 alpha: it's 7 a second in the
	 * data); exhaust flames 299E6 16 + 16 a second, 0.95-0.99 m, 0.47-0.73 s (ours 10, 0.5 m, 0.3 s); its smoke ribbons 0.7 m wide,
	 * living 2 s (ours 0.35 m and 1 s: a stream of rockets walled the view) and the thin hot ribbon 0.25 m, 2 s (ours 0.1 m, 0.5 s).
	 */
	private static void rocket(int id, Vec3 at, Vec3 fwd, float dt, boolean enemy) {
		Vec3 tail = at.subtract(fwd.scale(0.3));
		Particle glow = Fx.attach("pharah_rocket/" + id + "/glow", tail, () -> {
			Particle s = Fx.particle("cat_soft", Palette.PH_ROCKET_GLOW);
			return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f).nearFade(0.5f);
		});
		if (glow != null) {
			glow.size(0.4f, 0.4f).alpha(0.9f);
		}
		Particle flare = Fx.attach("pharah_rocket/" + id + "/flare", tail, () -> {
			Particle s = Fx.particle("pharah_flare", Palette.PH_ROCKET_FLARE);
			return s == null ? null : s.life(0.3f).fade(0.05f, 0.5f).nearFade(1);
		});
		if (flare != null) {
			flare.size(1, 1).alpha(0.4f);
		}
		int flames = Math.round(10 * dt + Fx.RANDOM.nextFloat() * 0.5f);
		for (int i = 0; i < flames; i++) {
			Particle f = Fx.particle("pharah_flame_noise", Palette.PH_ROCKET_FLAME);
			if (f != null) {
				Fx.add(f.at(tail).velocity(cone(fwd.scale(-1), 20).scale(range(3, 5))).drag(0.1f).life(0.3f).size(0.5f, 0.2f)
					.spin(range(0, 6.28f), 0).alpha(0.8f).fade(0.05f, 0.6f).nearFade(1));
			}
		}
		Fx.trail(id, tail, "torb_trail", enemy ? Palette.PH_ROCKET_TRAIL_ENEMY : Palette.PH_ROCKET_TRAIL, 0.35f, 1f, 0.6f, true);
		Fx.trailNearFade(id, 2);
		int streak = second(id, 1);
		Fx.trail(streak, tail, "cat_noise", Palette.PH_ROCKET_STREAK, 0.1f, 0.5f, 0.9f);
		Fx.trailNearFade(streak, 2);
	}

	/**
	 * Concussive Blast in flight (0132, H): the glow 4C09 0.6 m at -0.22 m (2C95 r1 / r3); the halo 9EE 1.9 m (ours 1.2 m at 0.3 alpha;
	 * 762A); its ribbon (1298, width L: ours 0.3 m) points living 0.3 s (40B0); the smoke ribbon 0.55 m, 1 s (ours 0.3 m; 4018 r8);
	 * sparks 1F35 135 a second (ours 30) at 2.9-7 m/s in a 15 degree cone, 0.22-0.42 s.
	 */
	private static void concussive(int id, Vec3 at, Vec3 fwd, float dt, boolean enemy) {
		Vec3 back = at.subtract(fwd.scale(0.22));
		Palette glowColour = enemy ? Palette.PH_CONC_GLOW_ENEMY : Palette.PH_CONC_GLOW;
		Particle glow = Fx.attach("pharah_conc/" + id + "/glow", back, () -> {
			Particle s = Fx.particle("cat_soft", glowColour);
			return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f).nearFade(0.5f);
		});
		if (glow != null) {
			glow.size(0.6f, 0.6f).alpha(0.9f);
		}
		Palette haloColour = enemy ? Palette.PH_CONC_HALO_ENEMY : Palette.PH_CONC_HALO;
		Particle halo = Fx.attach("pharah_conc/" + id + "/halo", back, () -> {
			Particle s = Fx.particle("pharah_flare", haloColour);
			return s == null ? null : s.life(0.3f).fade(0.05f, 0.5f).nearFade(1.5f);
		});
		if (halo != null) {
			halo.size(1.2f, 1.2f).alpha(0.3f);
		}
		Fx.trail(id, back, "dva_solid", enemy ? Palette.PH_CONC_TRAIL_ENEMY : Palette.PH_CONC_TRAIL, 0.3f, 0.3f, 0.7f);
		Fx.trailNearFade(id, 2);
		int smoke = second(id, 2);
		Fx.trail(smoke, back, "torb_trail", enemy ? Palette.PH_CONC_SMOKE_TRAIL_ENEMY : Palette.PH_CONC_SMOKE_TRAIL, 0.3f, 1f, 0.5f, true);
		Fx.trailNearFade(smoke, 2);
		int sparks = Math.round(30 * dt + Fx.RANDOM.nextFloat() * 0.5f);
		for (int i = 0; i < sparks; i++) {
			spark(back, cone(fwd.scale(-1), 15).scale(range(2.9f, 7)), glowColour, range(0.22f, 0.42f), 0.05f, 0);
		}
	}

	/**
	 * A Barrage or Rocket Salvo mini-rocket (0080; Salvo's 7769 has the same child, H): a glow 29F7 (its size is data flow: ours 0.25 m;
	 * FE81 r0) and its ribbons 37021 0.3 m, 0.3 s (ours one, 0.1 m: 75 of them in 2.5 s; F758 r3).
	 */
	private static void mini(int id, Vec3 at, Vec3 fwd) {
		Vec3 tail = at.subtract(fwd.scale(0.15));
		Particle glow = Fx.attach("pharah_mini/" + id, tail, () -> {
			Particle s = Fx.particle("lucio_spark", Palette.PH_MINI_FLAME);
			return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f).nearFade(0.5f);
		});
		if (glow != null) {
			glow.size(0.25f, 0.25f).alpha(0.9f);
		}
		Fx.trail(id, tail, "cat_slash", Palette.PH_MINI_TRAIL, 0.1f, 0.3f, 0.8f);
		Fx.trailNearFade(id, 2);
	}

	/** A projectile's second ribbon's id (Fx.trail): negative, apart from the entity ids and the debris' (ours). */
	private static int second(int id, int which) {
		return -(id * 4 + which) - 1;
	}

	// ---- the bursts ----

	/**
	 * A rocket's blast (0115, H; sizes are data flow, L: ours from its 2.5 m radius): the fireball 2291 (12 frames) four copies turned
	 * 90 degrees, 0.37 +- 0.14 s (ours 2 crossed at 2.5 m: four read as one blob); the streak flash 98C 0.25 s (ours 2 m); the flare
	 * 22D0 0.25 s; 50 sparks 1F35 at 9.2-16.2 m/s, 0.2-0.5 s (ours 20); debris 1F35 3-8 at 6.8-11 m/s, 1 +- 0.55 s, each with its smoke
	 * ribbon 0554 (ours 3, the ribbon 0.3 m, 1 s); smoke 377D5 0.86 +- 0.46 s. The big parts fade near the camera and stay under full
	 * alpha (ours: a blast at her feet otherwise whitened the screen).
	 */
	private static void rocketBurst(Vec3 at, boolean enemy) {
		Palette fire = enemy ? Palette.PH_BLAST_FIRE_ENEMY : Palette.PH_BLAST_FIRE;
		for (int i = 0; i < 2; i++) {
			Particle b = Fx.particle("dva_missile_fire#12", fire);
			if (b != null) {
				float life = range(0.3f, 0.5f);
				Fx.add(b.at(at).velocity(randomDir().scale(range(1, 1.5f))).life(life).size(2.2f, 2.8f).spin(range(0, 6.28f) + i * 1.57f, 0)
					.loop(12 / life).alpha(0.85f).fade(0.02f, 0.4f).nearFade(2));
			}
		}
		Particle streak = Fx.particle("pharah_streak", enemy ? Palette.PH_BLAST_FLASH_ENEMY : Palette.PH_BLAST_FLASH);
		if (streak != null) {
			Fx.add(streak.at(at).life(0.25f).size(2, 2.4f).aspect(0.3f).spin(range(0, 6.28f), 0).alpha(0.8f).fade(0.01f, 0.6f).nearFade(2));
		}
		Particle flare = Fx.particle("pharah_flare_line", enemy ? Palette.PH_BLAST_FLARE_ENEMY : Palette.PH_BLAST_FLARE);
		if (flare != null) {
			Fx.add(flare.at(at).life(0.25f).size(2.5f, 3f).aspect(0.25f).alpha(0.7f).fade(0.01f, 0.6f).nearFade(2));
		}
		for (int i = 0; i < 20; i++) {
			spark(at, randomDir().scale(range(9.2f, 16.2f)), Palette.PH_BLAST_SPARK, range(0.2f, 0.5f), 0.04f, 6);
		}
		for (int i = 0; i < 3; i++) {
			Vec3 v = cone(UP, 60).scale(range(6.8f, 11));
			DEBRIS.add(new Debris(Integer.MIN_VALUE / 2 - (debrisSerial++ & 0xFFFFF), at, v, Fx.now(), range(0.45f, 1.55f)));
		}
		Particle smoke = Fx.soft("smoke#48", Palette.PH_BLAST_SMOKE);
		if (smoke != null) {
			Fx.add(smoke.at(at).velocity(UP.scale(0.5)).life(range(0.6f, 1.3f)).size(1.5f, 2.6f).spin(range(0, 6.28f), range(-1, 1))
				.alpha(0.5f).loop(48).fade(0.05f, 0.6f).nearFade(2));
		}
	}

	/** A piece of debris t s in: falling (ours 9.8 m/s^2), its hot head 0.16-0.28 m (22A8) and its smoke ribbon (0554, E8FD r4). */
	private static void debris(Debris d, float t) {
		Vec3 at = d.from.add(d.velocity.scale(t)).add(0, -0.5 * 9.8 * t * t, 0);
		Particle head = Fx.attach("pharah_debris/" + d.id, at, () -> {
			Particle s = Fx.particle("cat_soft", Palette.PH_BLAST_DEBRIS);
			return s == null ? null : s.life(0.3f).fade(0.05f, 0.5f).nearFade(0.5f);
		});
		if (head != null) {
			float k = 1 - t / d.life;
			head.size(0.22f * (0.4f + 0.6f * k), 0.22f).alpha(0.9f);
		}
		Fx.trail(d.id, at, "pharah_smoke_ribbon", Palette.PH_DEBRIS_TRAIL, 0.3f, 1f, 0.6f, true);
		Fx.trailNearFade(d.id, 2);
	}

	/**
	 * A mini-rocket's hit (0081, H; creature and world the same; sizes data flow, L: ours from its 1.5 m radius): the fireball 2291 at
	 * 0.37 +- 0.14 s (ours one at 1 m); 5-15 sparks 4F3 at 5-10.7 m/s (ours 4); the smoke 3E28, 0.8 s. 75 of them in 2.5 s: no light,
	 * half the sparks (ours).
	 */
	private static void miniBurst(Vec3 at, boolean enemy) {
		Particle b = Fx.particle("dva_missile_fire#12", enemy ? Palette.PH_MINI_HIT_ENEMY : Palette.PH_MINI_HIT);
		if (b != null) {
			float life = range(0.25f, 0.45f);
			Fx.add(b.at(at).life(life).size(0.9f, 1.1f).spin(range(0, 6.28f), 0).loop(12 / life).alpha(0.85f).fade(0.02f, 0.4f).nearFade(1));
		}
		for (int i = 0; i < 4; i++) {
			Particle s = Fx.particle("lucio_soft", Palette.PH_MINI_HIT_SPARK);
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(randomDir().scale(range(5, 10.7f))).gravity(4).life(range(0.15f, 0.35f))
					.size(0.12f, 0.04f).aspect(0.3f).fade(0.02f, 0.5f).nearFade(0.5f));
			}
		}
		Particle smoke = Fx.soft("sombra_smoke", Palette.PH_MINI_HIT_SMOKE);
		if (smoke != null) {
			Fx.add(smoke.at(at).velocity(UP.scale(0.5)).life(0.8f).size(0.6f, 1.1f).spin(range(0, 6.28f), 0).alpha(0.4f).fade(0.05f, 0.6f)
				.nearFade(1));
		}
	}

	/**
	 * Concussive Blast's burst (0116, H): the ground wave 96C1 (a 1.28 m disc) at size 10 = 12.8 m, 0.22 s from 0.067 s, flat (373E);
	 * (ours: swelling to it from 4 m); the shell 085B at size 12, 0.28 s (ours at 0.15 alpha: from inside it tinted the view; F87C); the swirl ED1C at size 27 (~16 m
	 * wide), 0.33 s from 0.033 s (ours at 0.35 alpha; D704); the dome 963D 3 x 1.5 m, 0.44 s (25AF); the core 299E9 on 1385, 3.5 m,
	 * 0.08 s (D8A1); 32 sparks 1F35 at 10-21 m/s, 0.12 +- 0.21 s (ours 16). Blue to her side, red to her enemies.
	 */
	private static void concBurst(Vec3 at, boolean enemy) {
		float[] disc = Fx.mesh("pharah_disc");
		Particle wave = disc == null ? null : Fx.particle("pharah_wave_band", enemy ? Palette.PH_CONC_WAVE_ENEMY : Palette.PH_CONC_WAVE);
		if (wave != null) {
			wave.mesh(disc, new Vec3(1, 0, 0)).at(at).life(0.22f).size(4f, 12.8f).alpha(0.7f).fade(0.05f, 0.6f);
			wave.age = -0.067f;
			Fx.add(wave);
		}
		float[] sphere = Fx.mesh("pharah_sphere");
		if (sphere == null) {
			sphere = Fx.mesh("cat_sphere");
		}
		Particle shell = sphere == null ? null : Fx.particle("noise", enemy ? Palette.PH_CONC_SHELL_ENEMY : Palette.PH_CONC_SHELL);
		if (shell != null) {
			// the sphere stands on its foot: set down by half its size
			Fx.add(shell.mesh(sphere, new Vec3(1, 0, 0)).at(at.subtract(0, 6, 0)).life(0.28f).size(12, 12).alpha(0.15f).fade(0.05f, 0.6f));
		}
		float[] swirl = Fx.mesh("pharah_conc_swirl");
		Particle sw = swirl == null ? null : Fx.particle("pharah_swirl_noise", enemy ? Palette.PH_CONC_SWIRL_ENEMY : Palette.PH_CONC_SWIRL);
		if (sw != null) {
			// 27 x its 0.58 m widest side, 0.44 / 0.58 of that tall, set down by half its height; turned at random (ours)
			float size = 27 * 0.58f;
			double turn = Fx.RANDOM.nextDouble() * Math.PI * 2;
			sw.mesh(swirl, new Vec3(Math.cos(turn), 0, Math.sin(turn))).at(at.subtract(0, size * 0.44f / 0.58f / 2, 0)).life(0.33f)
				.size(size * 0.7f, size).alpha(0.35f).fade(0.05f, 0.6f);
			sw.age = -0.033f;
			Fx.add(sw);
		}
		float[] dome = Fx.mesh("torb_hemisphere");
		Particle d = dome == null ? null : Fx.particle("cat_cone_line", enemy ? Palette.PH_CONC_DOME_ENEMY : Palette.PH_CONC_DOME);
		if (d != null) {
			Fx.add(d.mesh(dome, new Vec3(1, 0, 0)).at(at.subtract(0, 0.2, 0)).life(0.44f).size(2.4f, 3f).alpha(0.6f).fade(0.03f, 0.6f));
		}
		Palette core = enemy ? Palette.PH_CONC_CORE_ENEMY : Palette.PH_CONC_CORE;
		float[] ball = Fx.mesh("cat_sphere");
		Particle c = ball == null ? null : Fx.particle("juno_cloud_noise", core);
		if (c != null) {
			Fx.add(c.mesh(ball, new Vec3(1, 0, 0)).at(at.subtract(0, 1.75, 0)).life(0.08f).size(3.5f, 3.5f).alpha(0.6f).fade(0.01f, 0.5f));
		}
		Particle glow = Fx.particle("glow", core);
		if (glow != null) {
			Fx.add(glow.at(at).life(0.15f).size(3.5f, 2.5f).alpha(0.5f).fade(0.01f, 0.6f).nearFade(2));
		}
		for (int i = 0; i < 16; i++) {
			Particle s = Fx.particle("spark#4", core);
			if (s == null) {
				break;
			}
			Fx.add(s.at(at).shape(Shape.STREAK).velocity(randomDir().scale(range(10, 21))).drag(0.05f).life(range(0.1f, 0.33f))
				.size(range(0.5f, 1), 0.3f).aspect(0.15f).fade(0.02f, 0.5f).nearFade(1));
		}
	}

	/**
	 * Concussive's launch at her wrist 0038 (012F, H): the glows A7B6 0.6 and 0.3 m, 0.17 s (114A r1 / r2), the flash 299E9 x 1CB5E
	 * (4 frames) 0.66 m (6C6B r3 / r4). In her own first person at the server's point, a third the size (ours).
	 */
	private static void concLaunch(@Nullable Entity p, Vec3 at, boolean enemy) {
		boolean own = ownFirstPerson(p);
		Vec3 wrist = point(p, WRIST);
		Vec3 from = wrist != null ? wrist : at;
		float k = own ? 0.3f : 1;
		Particle glow = Fx.particle("glow", enemy ? Palette.PH_CONC_LAUNCH_ENEMY : Palette.PH_CONC_LAUNCH);
		if (glow != null) {
			Fx.add(glow.at(from).life(0.17f).size(0.6f * k, 0.3f * k).alpha(0.8f).fade(0.02f, 0.6f).nearFade(own ? 0.2f : 0.5f));
		}
		Particle flash = Fx.particle("juno_hit_ring#4", enemy ? Palette.PH_CONC_LAUNCH_B_ENEMY : Palette.PH_CONC_LAUNCH_B);
		if (flash != null) {
			Fx.add(flash.at(from).life(0.17f).size(0.66f * k, 0.5f * k).spin(range(0, 6.28f), 0).fade(0.02f, 0.6f)
				.nearFade(own ? 0.2f : 0.5f));
		}
	}

	// ---- from the server ----

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		if (!e.effect().startsWith("pharah_")) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Entity entity = mc.level == null || e.entity() < 0 ? null : mc.level.getEntity(e.entity());
		switch (e.effect()) {
			case "pharah_launch" -> launch(entity, at, dir, e.variant() == 1, enemy);
			case "pharah_conc_launch" -> concLaunch(entity, at, enemy);
			case "pharah_rocket_burst" -> rocketBurst(at, enemy);
			case "pharah_mini_burst" -> miniBurst(at, enemy);
			case "pharah_conc_burst" -> concBurst(at, enemy);
			case "pharah_jump_jet" -> jumpStart(entity, at, dir, false);
			case "pharah_jet_dash" -> jumpStart(entity, at, dir, true);
			case "pharah_pod" -> podFlash(entity, at, e.variant(), enemy);
			case "pharah_hover_empty" -> hoverEmpty(entity, at);
			default -> {
				return false;
			}
		}
		return true;
	}

	// ---- shared ----

	/** One spark streak: its mask 1F35 (4 frames), stretched along its flight (TorbjornFx's). */
	private static void spark(Vec3 at, Vec3 velocity, Palette palette, float life, float size, float gravity) {
		Particle s = Fx.particle("spark#4", palette);
		if (s != null) {
			Fx.add(s.at(at).shape(Shape.STREAK).velocity(velocity).gravity(gravity).life(life).size(size * 3, size * 2).aspect(0.35f)
				.fade(0.05f, 0.5f).nearFade(0.5f));
		}
	}

	private static Vec3 randomDir() {
		Vec3 v = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return v.lengthSqr() < 1.0E-6 ? UP : v.normalize();
	}

	/** A direction within this many degrees of axis (TorbjornFx's). */
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
