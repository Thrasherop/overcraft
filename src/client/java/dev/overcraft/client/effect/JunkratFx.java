package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.entity.ConcussionMineEntity;
import dev.overcraft.entity.FragEntity;
import dev.overcraft.entity.RipTireEntity;
import dev.overcraft.entity.SteelTrapEntity;
import dev.overcraft.junkrat.JunkratStats;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Junkrat's effects (tools/reference/junkrat-effects.md, read from his effects' data, 2026-11-17; #150: his blasts were Minecraft's
 * EXPLOSION / FLAME / CLOUD). His fire is orange-gold to his side, a redder twin to his enemies on the renderers that carry one (the
 * frag's glow, the mine's flames and ribbons, the thrown trap's ribbon, the tire's fuse and ribbons: H rows, M roles); his blasts are
 * the same for both (the data's creature and world share one, H). The frag, mine, trap and tire are fed each frame from their
 * renderers; the detonator, the reload, the tire's cast, Nitro Boost's view and the tire blast's debris are kept here and drawn each
 * frame (frame()); the rest from the server's messages. Sizes in metres, times in seconds; each says where it came from: H read in the
 * data, M inferred, ours where the data gives nothing or we drew it smaller.
 */
public final class JunkratFx {
	/** Its art, made ahead as the hero is picked (HeroFxArt). */
	public static final List<EffectArt.Art> ART = List.of(
		new EffectArt.Art("cat_soft", Palette.JR_FRAG_MUZZLE, false),
		new EffectArt.Art("jr_muzzle_flash#2", Palette.JR_FRAG_MUZZLE, false),
		new EffectArt.Art("ana_puff#4", Palette.JR_FRAG_MUZZLE_SMOKE, true),
		new EffectArt.Art("spark#4", Palette.JR_FRAG_MUZZLE_SPARK, false),
		new EffectArt.Art("jr_frag_glow#4", Palette.JR_FRAG_GLOW, false),
		new EffectArt.Art("torb_trail", Palette.JR_FRAG_TRAIL, false),
		new EffectArt.Art("spark#4", Palette.JR_FRAG_SPARK, false),
		new EffectArt.Art("jr_flash", Palette.JR_FRAG_FLASH, false),
		new EffectArt.Art("jr_fireball#11", Palette.JR_FRAG_FIRE, false),
		new EffectArt.Art("jr_spark_streak", Palette.JR_SPARK, false),
		new EffectArt.Art("brigitte_smoke#32", Palette.JR_FRAG_SMOKE, true),
		new EffectArt.Art("jr_scorch", Palette.JR_SCORCH, true),
		new EffectArt.Art("spark#4", Palette.JR_RELOAD_SPARK, false),
		new EffectArt.Art("spark#4", Palette.JR_RELOAD_SPARK_B, false),
		new EffectArt.Art("glow", Palette.JR_DETONATOR, false),
		new EffectArt.Art("glow", Palette.JR_MINE_LIGHT, false),
		new EffectArt.Art("lw_burst", Palette.JR_MINE_BLINK, false),
		new EffectArt.Art("sombra_trail_band", Palette.JR_MINE_TRAIL, false),
		new EffectArt.Art("jr_flash", Palette.JR_MINE_GLOW, false),
		new EffectArt.Art("jr_fireball#11", Palette.JR_MINE_FIRE, false),
		new EffectArt.Art("lw_thorn_flame#2", Palette.JR_MINE_FLAME, false),
		new EffectArt.Art("brigitte_smoke#32", Palette.JR_MINE_SMOKE, false),
		new EffectArt.Art("cat_cloud", Palette.JR_TRAP_THROWN, false),
		new EffectArt.Art("jr_chain", Palette.JR_TRAP_CHAIN, false),
		new EffectArt.Art("brigitte_smoke#32", Palette.JR_TRAP_SMOKE, true),
		new EffectArt.Art("ana_ring", Palette.JR_TRAPPED_RING, false),
		new EffectArt.Art("mercy_plus", Palette.JR_TRAPPED, false),
		new EffectArt.Art("band", Palette.JR_TRAPPED_B, false),
		new EffectArt.Art("torb_trail", Palette.JR_TIRE_TRAIL, false),
		new EffectArt.Art("cat_soft", Palette.JR_TIRE_FUSE, false),
		new EffectArt.Art("jr_glow_ball", Palette.JR_TIRE_GLOW, false),
		new EffectArt.Art("cat_streak", Palette.JR_TIRE_SPARK, false),
		new EffectArt.Art("spark#4", Palette.JR_TIRE_SPARK, false),
		new EffectArt.Art("jr_flash", Palette.JR_TIRE_FLASH_BIG, false),
		new EffectArt.Art("dva_solid", Palette.JR_TIRE_SHELL, false),
		new EffectArt.Art("jr_ring_noise", Palette.JR_TIRE_RING, false),
		new EffectArt.Art("jr_fireball#11", Palette.JR_TIRE_FIRE, false),
		new EffectArt.Art("lw_thorn_flame#2", Palette.JR_TIRE_FLAMES, false),
		new EffectArt.Art("torb_trail", Palette.JR_BOMB_TRAIL, false),
		new EffectArt.Art("lucio_spark", Palette.JR_BOMB_GLOW, false),
		new EffectArt.Art("spark#4", Palette.JR_BOMB_SPARK, false),
		new EffectArt.Art("jr_flash", Palette.JR_BOMB_FLASH, false));
	public static final List<EffectArt.Art> ART_ENEMY = List.of(
		new EffectArt.Art("jr_frag_glow#4", Palette.JR_FRAG_GLOW_ENEMY, false),
		new EffectArt.Art("sombra_trail_band", Palette.JR_MINE_TRAIL_ENEMY, false),
		new EffectArt.Art("lw_thorn_flame#2", Palette.JR_MINE_FLAME_ENEMY, false),
		new EffectArt.Art("cat_cloud", Palette.JR_TRAP_THROWN_ENEMY, false),
		new EffectArt.Art("torb_trail", Palette.JR_TIRE_TRAIL_ENEMY, false),
		new EffectArt.Art("cat_soft", Palette.JR_TIRE_FUSE_ENEMY, false));

	/**
	 * His posed points (HeroModelRenderer): the Frag Launcher's muzzle 0006 and drum 006F (junkrat_weapon.glb's: the launch, the reload's
	 * sparks), then his own left hand 000B (the detonator, the tire's cast) (H ids, M roles: junkrat-effects.md "Hardpoints").
	 */
	public static final String[] POINTS = {"hardpoint_0006", "hardpoint_006F", "hardpoint_000B"};
	/** The first of POINTS that are on his body (the rest is on his gun). */
	public static final int GUN_POINTS = 2;
	private static final int MUZZLE = 0, DRUM = 1, HAND = 2;
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

	/** Each frag's, mine's, trap's and tire's emitters, by entity id (dropped when the entity is gone). */
	private static final Map<Integer, Rate[]> RATES = new HashMap<>();

	/** A reload: whose, when it started, its length (s), what it's drawn yet (bits). */
	private static final class Reload {
		final int entity;
		final float start, length;
		int done;

		Reload(int entity, float start, float length) {
			this.entity = entity;
			this.start = start;
			this.length = length;
		}
	}

	private static final List<Reload> RELOADS = new ArrayList<>();

	/** Something due later: the tire cast's sparks 1.3 s in (whose, when). */
	private record Due(int entity, float at) {
	}

	private static final List<Due> CASTS = new ArrayList<>();
	/** Nitro Boost on a tire: its id, until when (its flare and the driver's speed lines). */
	private static final Map<Integer, Float> NITRO = new HashMap<>();

	/** One of the tire blast's flying bits (15D2's 7 debris with 35DC behind each): simulated here for its trail. */
	private static final class Debris {
		final int id;
		Vec3 at, velocity;
		final float born, life;
		final boolean enemy;

		Debris(int id, Vec3 at, Vec3 velocity, float born, float life, boolean enemy) {
			this.id = id;
			this.at = at;
			this.velocity = velocity;
			this.born = born;
			this.life = life;
			this.enemy = enemy;
		}
	}

	private static final List<Debris> DEBRIS = new ArrayList<>();
	/** The debris' trail ids: below every entity's (they're keyed like entities' in Fx.trail). */
	private static int debrisSerial = -2_000_000;
	private static float debrisLast = -1;

	private JunkratFx() {
	}

	// ---- his posed points (HeroModelRenderer) ----

	public static void posed(Entity junkrat, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(junkrat.getId(), id -> new Posed());
		p.points = points;
		p.tick = junkrat.level().getGameTime();
	}

	/** His posed point this tick or the last (null in his own first person, unposed, or without that point). */
	private static @Nullable Vec3 point(@Nullable Entity junkrat, int which) {
		if (junkrat == null || ownFirstPerson(junkrat)) {
			return null;
		}
		Posed p = POSED.get(junkrat.getId());
		return p == null || junkrat.level().getGameTime() - p.tick > 1 || which >= p.points.length ? null : p.points[which];
	}

	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/** His muzzle: in his own first person the view model's as drawn, else posed (hp 0006), else where the server shot from. */
	private static Vec3 muzzle(@Nullable Entity junkrat, Vec3 fallback) {
		Minecraft mc = Minecraft.getInstance();
		if (ownFirstPerson(junkrat)) {
			Vector3f view = dev.overcraft.client.model.HeroModelRenderer.viewMuzzle();
			return view == null ? fallback : mc.gameRenderer.mainCamera().position().add(view.x, view.y, view.z);
		}
		Vec3 posed = point(junkrat, MUZZLE);
		return posed != null ? posed : fallback;
	}

	/** His left hand (hp 000B posed), else ours: 0.3 m to his left, 0.45 m under his eyes, 0.3 m ahead (the server's leftHand, about). */
	private static Vec3 hand(Entity junkrat) {
		Vec3 posed = point(junkrat, HAND);
		if (posed != null) {
			return posed;
		}
		Vec3 look = junkrat.getViewVector(1);
		Vec3 side = look.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		return junkrat.getEyePosition().subtract(side.scale(0.3)).add(0, -0.45, 0).add(look.scale(0.3));
	}

	// ---- each frame ----

	/** Each frame: the detonators lit, the reloads, the tire casts, Nitro's speed lines, the tire blasts' debris. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			RELOADS.clear();
			CASTS.clear();
			NITRO.clear();
			DEBRIS.clear();
			RATES.clear();
			POSED.clear();
			return;
		}
		float now = Fx.now();
		RATES.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		POSED.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		RELOADS.removeIf(r -> {
			Entity e = mc.level.getEntity(r.entity);
			float t = now - r.start;
			if (e == null || !e.isAlive() || t > r.length || t < -1) {
				return true;
			}
			reload(e, r, t);
			return false;
		});
		CASTS.removeIf(c -> {
			if (now < c.at() - 5) {
				return true;
			}
			if (now < c.at()) {
				return false;
			}
			Entity e = mc.level.getEntity(c.entity());
			if (e != null && e.isAlive()) {
				castSparks(e);
			}
			return true;
		});
		NITRO.entrySet().removeIf(n -> now > n.getValue() || now < n.getValue() - 5);
		for (Map.Entry<Integer, Float> n : NITRO.entrySet()) {
			if (mc.level.getEntity(n.getKey()) instanceof RipTireEntity tire && tire.owner() == mc.player) {
				speedLines(mc);
			}
		}
		debris(now);
		// the detonator lit (625C) on every Junkrat with a mine out
		Set<Integer> mines = new HashSet<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof ConcussionMineEntity mine && mine.isAlive() && mine.getOwner() != null) {
				mines.add(mine.getOwner().getId());
			}
		}
		for (Player p : mc.level.players()) {
			if (mines.contains(p.getId()) && p.isAlive() && !ownFirstPerson(p)) {
				detonator(p);
			}
		}
	}

	/**
	 * The detonator lit (625C, H; held while a mine is out, M from its gate) at his left hand 000B: glows A7B6 3 a second, 0.68-1.23 s,
	 * 0.08-0.23 m, red to white (5688 / FEA8 / DCDF shifted). Ours: one 0.15 m glow kept on his hand, pulsing 3 a second.
	 */
	private static void detonator(Player p) {
		Vec3 at = hand(p);
		Particle g = Fx.attach("jr_detonator/" + p.getId(), at, () -> {
			Particle s = Fx.particle("glow", Palette.JR_DETONATOR);
			return s == null ? null : s.life(0.4f).size(0.15f, 0.15f).alpha(0.8f).twinkle(3).fade(0.1f, 0.5f).nearFade(0.3f);
		});
		if (g != null) {
			g.at(at);
		}
	}

	// ---- the Frag Launcher ----

	/**
	 * The launch (1468, H; at the muzzle 0006): two streak flashes 4C09 0.25 m and 0.18 m stretched x11-13 along the shot, 0.09-0.13 s
	 * (ours x3: at the data's x12 they read as a beam); the burst mesh 27B3 x 11F7F, 3 at size 1-1.35 in a 10 degree cone, 0.07-0.11 s;
	 * a smoke puff 5429 0.65 m, 0.34-0.52 s, 1 m/s; sparks 1F35 0.1-0.17 m at 1.9-3.8 m/s, 0.15-0.38 s (4 here: ours). In his own first
	 * person a third the size (ours, as Tracer's).
	 */
	private static void launch(@Nullable Entity junkrat, Vec3 at, Vec3 dir) {
		boolean own = ownFirstPerson(junkrat);
		float k = own ? 1 / 3f : 1;
		float near = own ? 0.2f : 0.5f;
		Vec3 from = muzzle(junkrat, at);
		Vec3 fwd = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
		for (float w : new float[] {0.25f, 0.18f}) {
			Particle s = Fx.particle("cat_soft", Palette.JR_FRAG_MUZZLE);
			if (s != null) {
				float len = w * 3 * k;
				Fx.add(s.at(from.add(fwd.scale(len / 2))).axis(fwd).life(range(0.09f, 0.13f)).size(len, len).aspect(w * k / len)
					.fade(0.01f, 0.5f).nearFade(near));
			}
		}
		float[] burst = Fx.mesh("jr_muzzle_burst");
		for (int i = 0; i < 3; i++) {
			Particle b = Fx.particle("jr_muzzle_flash#2", Palette.JR_FRAG_MUZZLE);
			if (b == null) {
				break;
			}
			// the mesh is 0.2 m across at size 1 (H): its size x0.2 in metres; it stands on its base, along the shot
			float size = range(1, 1.35f) * 0.2f * k;
			Vec3 d = cone(fwd, 10);
			if (burst != null) {
				Fx.add(b.mesh(burst, d.cross(UP).lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : d.cross(UP), d).at(from)
					.life(range(0.07f, 0.11f)).size(size, size * 1.2f).fade(0.01f, 0.5f));
			} else {
				Fx.add(b.at(from.add(d.scale(size / 2))).life(range(0.07f, 0.11f)).size(size, size * 1.2f).spin(range(0, 6.28f), 0)
					.fade(0.01f, 0.5f).nearFade(near));
			}
		}
		Particle smoke = Fx.soft("ana_puff#4", Palette.JR_FRAG_MUZZLE_SMOKE);
		if (smoke != null) {
			Fx.add(smoke.at(from.add(fwd.scale(0.1 * k))).velocity(cone(fwd, 20)).life(range(0.34f, 0.52f)).size(0.3f * k, 0.65f * k)
				.alpha(0.5f).spin(range(0, 6.28f), range(-1, 1)).fade(0.05f, 0.6f).nearFade(near));
		}
		for (int i = 0; i < 4; i++) {
			spark("spark#4", from, cone(fwd, 30).scale(range(1.9f, 3.8f)), Palette.JR_FRAG_MUZZLE_SPARK, range(0.15f, 0.38f),
				range(0.1f, 0.17f) * k, 0, near);
		}
	}

	/**
	 * A frag (051B, H) or a Total Mayhem bomb (1792, H) in flight, fed each frame from its renderer. A frag: its glow 18BAD (4 frames)
	 * 10 a second, 0.2 s (its size data flow: 0.3 m, ours), red to orange (1FA3 r1 / the enemy's r2); its ribbon 0.55 m wide in the data,
	 * 0.3 m here (ours: the frag is 0.2 m), 0.77 s; sparks 1F35 39-71 a second (ours 18), 0.2-2 m/s, 0.08-0.13 m. A bomb: its ribbon 0.38
	 * m, 1.5 s in the data (ours 0.15 m, 0.5 s: three at once), the glow 29F7 1.5 m (ours 0.5), sparks 39-71 a second (ours 15).
	 */
	public static void fragFlight(FragEntity frag, Vec3 at, float dt, boolean enemy) {
		int id = frag.getId();
		Rate[] rates = RATES.computeIfAbsent(id, k -> new Rate[] {new Rate()});
		if (frag.bomb()) {
			Particle g = Fx.attach("jr_bomb/" + id, at, () -> {
				Particle s = Fx.particle("lucio_spark", Palette.JR_BOMB_GLOW);
				return s == null ? null : s.life(0.3f).size(0.5f, 0.5f).alpha(0.8f).fade(0.05f, 0.5f).nearFade(1);
			});
			if (g != null) {
				g.at(at);
			}
			Fx.trail(id, at, "torb_trail", Palette.JR_BOMB_TRAIL, 0.15f, 0.5f, 0.9f);
			int sparks = rates[0].take(15);
			for (int i = 0; i < sparks; i++) {
				spark("spark#4", at, randomDir().scale(range(0.75f, 2)), Palette.JR_BOMB_SPARK, range(0.2f, 0.4f), range(0.04f, 0.08f), 2,
					0.5f);
			}
			return;
		}
		Palette glowColour = enemy ? Palette.JR_FRAG_GLOW_ENEMY : Palette.JR_FRAG_GLOW;
		Particle g = Fx.attach("jr_frag/" + id, at, () -> {
			Particle s = Fx.particle("jr_frag_glow#4", glowColour);
			return s == null ? null : s.life(0.2f).size(0.3f, 0.3f).loop(10).fade(0.05f, 0.5f).nearFade(0.8f);
		});
		if (g != null) {
			g.at(at);
		}
		Fx.trail(id, at, "torb_trail", Palette.JR_FRAG_TRAIL, 0.3f, 0.77f, 0.9f);
		Fx.trailNearFade(id, 2);
		int sparks = rates[0].take(18);
		Vec3 back = frag.getDeltaMovement().lengthSqr() < 1.0E-6 ? UP : frag.getDeltaMovement().normalize().scale(-1);
		for (int i = 0; i < sparks; i++) {
			spark("spark#4", at, cone(back, 15).scale(range(0.2f, 2)), Palette.JR_FRAG_SPARK, range(0.15f, 0.3f), range(0.08f, 0.13f) * 0.5f,
				0, 0.5f);
		}
	}

	/**
	 * A frag's burst (13C6, H; creature and world share it): the flash 2613A 4-5 m, 0.15-0.17 s (at 0.3 alpha and faded near the camera:
	 * ours, a 4.5 m flash at full washed the view); the fireball 47C4 (11 frames) 1.2 +- 0.25 m, 0.35 +- 0.15 s, 0.8-1.5 m/s in a 32
	 * degree cone; sparks 2C009 20 at 1.4-8.9 m/s (ours 12), 0.25-0.45 s, in a 95 degree cone; smoke 1DFCB 1.25-1.53 m, 0.8-1.1 s,
	 * 2.7-6.3 m/s (2 here, slowed: ours); the scorch decal 22BE / 31E8 1.5 x 2 m (M) for 4 s, on what it hit.
	 */
	private static void fragBurst(Vec3 at, Vec3 normal) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal.normalize();
		flash(at, Palette.JR_FRAG_FLASH, range(4, 5), range(0.15f, 0.17f), 0.3f);
		for (int i = 0; i < 3; i++) {
			fireball(at, cone(n, 32).scale(range(0.8f, 1.5f)), Palette.JR_FRAG_FIRE, 1.2f + (Fx.RANDOM.nextFloat() - 0.5f) * 0.5f,
				0.35f + (Fx.RANDOM.nextFloat() - 0.5f) * 0.3f);
		}
		for (int i = 0; i < 12; i++) {
			streak(at, cone(n, 95).scale(range(1.4f, 8.9f)), Palette.JR_SPARK, range(0.25f, 0.45f));
		}
		for (int i = 0; i < 2; i++) {
			smoke(at, cone(n, 90).scale(range(1.2f, 2.5f)), Palette.JR_FRAG_SMOKE, range(1.25f, 1.53f), range(0.8f, 1.1f), 0.45f);
		}
		if (normal.lengthSqr() > 1.0E-6) {
			scorch(at, n, 1.75f, 4);
		}
	}

	/**
	 * A Total Mayhem bomb's burst (14CC, H, its sizes data flow: ours from the 2 m radius): the flash 29F7 0.33 s (0B6C r0) 2 m; a
	 * fireball 47C4 1 m, 0.33-0.48 s; sparks 2C009 20 at 5.4-8.7 m/s all ways (ours 8); one smoke.
	 */
	private static void bombBurst(Vec3 at) {
		flash(at, Palette.JR_BOMB_FLASH, 2, 0.33f, 0.4f);
		fireball(at, randomDir().scale(0.5f), Palette.JR_FRAG_FIRE, 1, range(0.33f, 0.48f));
		for (int i = 0; i < 8; i++) {
			streak(at, randomDir().scale(range(5.4f, 8.7f)), Palette.JR_BOMB_SPARK, range(0.2f, 0.4f));
		}
		smoke(at, UP.scale(0.8f), Palette.JR_FRAG_SMOKE, 1, 0.8f, 0.4f);
	}

	/**
	 * The reload (H, scaled to its length: the clips are 1.5 s in his view (08AD), 1.43 s as others see him (16AA)), at the launcher's
	 * drum 006F and muzzle 0006: a steam puff 4DB x 218F from 0.23 s for 0.1 s at the muzzle; 12 sparks 1F35 at 0.83 s (3P 0.87),
	 * 0.9-1.3 m/s, spinning (33D8 r0); 15 at 1.2 s, 0.5-1.2 m/s (3P 1-2.2), 0.04-0.07 m, 0.23-0.49 s (195E r0). In his own view a third
	 * the size, at his view model's muzzle (ours).
	 */
	private static void reload(Entity e, Reload r, float t) {
		boolean own = ownFirstPerson(e);
		float clip = own ? 1.5f : 1.43f;
		float k = r.length / clip;
		Vec3 muzzle = muzzle(e, e.getEyePosition().add(e.getViewVector(1).scale(0.5)).add(0, -0.3, 0));
		Vec3 drum = own ? muzzle.subtract(e.getViewVector(1).scale(0.12)) : point(e, DRUM);
		if (drum == null) {
			drum = muzzle.subtract(e.getViewVector(1).scale(0.3));
		}
		float scale = own ? 1 / 3f : 1;
		float near = own ? 0.15f : 0.5f;
		if ((r.done & 1) == 0 && t >= 0.23f * k) {
			r.done |= 1;
			for (int i = 0; i < 3; i++) {
				Particle puff = Fx.soft("ana_puff#4", Palette.JR_FRAG_MUZZLE_SMOKE);
				if (puff != null) {
					Fx.add(puff.at(muzzle).velocity(cone(UP, 30).scale(0.4f)).life(range(0.4f, 0.6f)).size(range(0.17f, 0.22f) * scale,
						0.35f * scale).alpha(0.5f).spin(range(0, 6.28f), range(-1, 1)).fade(0.05f, 0.6f).nearFade(near));
				}
			}
		}
		if ((r.done & 2) == 0 && t >= (own ? 0.83f : 0.87f) * k) {
			r.done |= 2;
			for (int i = 0; i < 12; i++) {
				spark("spark#4", drum, randomUp().scale(range(0.9f, 1.3f) * (own ? 1 : 1.5f)), Palette.JR_RELOAD_SPARK_B,
					range(0.23f, 0.49f), range(0.04f, 0.07f) * scale, 4, near);
			}
		}
		if ((r.done & 4) == 0 && t >= 1.2f * k) {
			r.done |= 4;
			for (int i = 0; i < 15; i++) {
				spark("spark#4", drum, randomUp().scale(own ? range(0.5f, 1.2f) : range(1, 2.2f)), Palette.JR_RELOAD_SPARK,
					range(0.23f, 0.49f), range(0.04f, 0.07f) * scale, 4, near);
			}
		}
	}

	// ---- Concussion Mine ----

	/**
	 * The mine (156A, H; on its 0011, looping), fed each frame from its renderer: blinks 2D6CC 4 a second living 0.26 s (red, CC40 r1;
	 * ours 0.25 m) and 2 a second, 0.52 s, 0.5 m (gold, 1976 r0; ours 0.35 m); while it flies its ribbons 0.38 s (1.91 m wide in the
	 * data, scaled by the mine: M; ours 0.15 m), olive-cream to his side, dark red to his enemies (AFB3 r2 / 63C1 r2, M).
	 */
	public static void mineFlight(ConcussionMineEntity mine, Vec3 at, boolean enemy) {
		int id = mine.getId();
		Rate[] rates = RATES.computeIfAbsent(id, k -> new Rate[] {new Rate(), new Rate()});
		net.minecraft.core.Direction face = mine.stuckFace();
		Vec3 n = face == null ? UP : Vec3.atLowerCornerOf(face.getUnitVec3i());
		// its light 0011 sits on its top (0.2 m up the model, H); the entity's box is its middle (ours: 0.12 m out of the middle)
		Vec3 light = at.add(0, mine.getBbHeight() / 2, 0).add(n.scale(0.12));
		int red = rates[0].take(4);
		for (int i = 0; i < red; i++) {
			Particle s = Fx.particle("glow", Palette.JR_MINE_LIGHT);
			if (s != null) {
				Fx.add(s.at(light).life(0.26f).size(0.25f, 0.2f).fade(0.05f, 0.6f).nearFade(0.5f));
			}
		}
		int gold = rates[1].take(2);
		for (int i = 0; i < gold; i++) {
			Particle s = Fx.particle("lw_burst", Palette.JR_MINE_BLINK);
			if (s != null) {
				Fx.add(s.at(light).life(0.52f).size(0.35f, 0.3f).alpha(0.8f).spin(range(0, 6.28f), 0).fade(0.05f, 0.6f).nearFade(0.5f));
			}
		}
		if (!mine.stuck()) {
			Fx.trail(id, light, "sombra_trail_band", enemy ? Palette.JR_MINE_TRAIL_ENEMY : Palette.JR_MINE_TRAIL, 0.15f, 0.38f, 0.8f);
			Fx.trailNearFade(id, 2);
		}
	}

	/**
	 * The mine's blast (151A, H; its sizes data flow: ours from the 3 m radius): the flash 2613A 0.18-0.2 s, 3 m (ours); the fireball
	 * 47C4 1 burst 0.4-0.55 s (1.5 m: ours); flame tongues 14F9 11 at 3.6-5.9 m/s all ways + 8 at 3-4 m/s, 0.35 +- 0.15 s (ours 10),
	 * orange to his side, redder to his enemies (803B r1 / r2); sparks 2C009 25 (ours 12), 0.2-0.5 s; one lit smoke 0.4-0.65 s at
	 * 4.4-5.8 m/s (slowed: ours); the scorch decal 3 s on the face it was stuck to.
	 */
	private static void mineBlast(Vec3 at, Vec3 normal, boolean enemy) {
		flash(at, Palette.JR_MINE_GLOW, 3, range(0.18f, 0.2f), 0.35f);
		fireball(at, Vec3.ZERO, Palette.JR_MINE_FIRE, 1.5f, range(0.4f, 0.55f));
		Palette flame = enemy ? Palette.JR_MINE_FLAME_ENEMY : Palette.JR_MINE_FLAME;
		for (int i = 0; i < 10; i++) {
			Particle f = Fx.particle("lw_thorn_flame#2", flame);
			if (f == null) {
				break;
			}
			Vec3 d = randomDir();
			float len = range(0.5f, 0.8f);
			Fx.add(f.at(at.add(d.scale(0.2))).shape(Shape.STREAK).velocity(d.scale(range(3.6f, 5.9f))).drag(0.05f)
				.life(0.35f + (Fx.RANDOM.nextFloat() - 0.5f) * 0.3f).size(len, len * 1.3f).aspect(0.5f).fade(0.05f, 0.6f).nearFade(1));
		}
		for (int i = 0; i < 12; i++) {
			streak(at, randomDir().scale(range(3, 8)), Palette.JR_SPARK, range(0.2f, 0.5f));
		}
		Particle lit = Fx.particle("brigitte_smoke#32", Palette.JR_MINE_SMOKE);
		if (lit != null) {
			Fx.add(lit.at(at).velocity(UP.scale(1.5f)).drag(0.1f).life(range(0.4f, 0.65f)).size(1, 1.6f).alpha(0.6f).spin(range(0, 6.28f), 0)
				.fade(0.05f, 0.6f).nearFade(1.5f));
		}
		if (normal.lengthSqr() > 1.0E-6) {
			scorch(at, normal.normalize(), 1.75f, 3);
		}
	}

	// ---- Steel Trap ----

	/**
	 * The trap, fed each frame from its renderer. Thrown: its ribbon 31AF x 18E48 (6705, H) 1.2 m wide, 0.5 s (ours 0.3 m: the trap is
	 * 0.6 m), white-gold to his side, pink-red to his enemies (D3BB r3 / r4, M). Holding one: the chain 16D5 (H: ribbon E71 0.117 m
	 * wide on the chain masks 2A8F-2A92, F309 r1) from its 0011 to the caught's 0001 (ours: their ankles), and the root's look on them
	 * (19F4, H): a 2 m ring at their feet (violet-blue, 8FF5), plus marks E0AA 3 -> 6 a second (ours 4.5), 0.82 +- 0.2 s, rising
	 * 0.57 +- 0.15 m/s (33DC r0; ours 0.25 m sprites, mercy_plus), wisps 2FF91 10 a second (ours 5), 0.5 +- 0.4 s (5443 r3).
	 */
	public static void trapFlight(SteelTrapEntity trap, Vec3 at, boolean enemy) {
		int id = trap.getId();
		if (trap.stage() == 0) {
			Fx.trail(id, at.add(0, 0.1, 0), "cat_cloud", enemy ? Palette.JR_TRAP_THROWN_ENEMY : Palette.JR_TRAP_THROWN, 0.3f, 0.5f, 0.8f);
			Fx.trailNearFade(id, 2);
			return;
		}
		LivingEntity caught = trap.caught();
		if (trap.stage() != 2 || caught == null || !caught.isAlive()) {
			return;
		}
		Rate[] rates = RATES.computeIfAbsent(id, k -> new Rate[] {new Rate(), new Rate()});
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Vec3 feet = caught.getPosition(partial);
		Vec3 from = at.add(0, 0.1, 0);
		Vec3 to = feet.add(0, 0.2, 0);
		Vec3 piece = to.subtract(from);
		float l = (float) piece.length();
		if (l > 0.05f) {
			Particle chain = Fx.attach("jr_chain/" + id, from.add(piece.scale(0.5)), () -> {
				Particle s = Fx.particle("jr_chain", Palette.JR_TRAP_CHAIN);
				return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f).nearFade(0.5f);
			});
			if (chain != null) {
				chain.axis(piece).size(l, l).aspect(0.1f / l);
			}
		}
		Particle ring = Fx.attach("jr_root/" + id + "/ring", feet, () -> {
			Particle s = Fx.particle("ana_ring", Palette.JR_TRAPPED_RING);
			return s == null ? null : s.normal(UP).lift(UP).life(0.4f).size(2, 2).alpha(0.6f).fade(0.1f, 0.5f);
		});
		if (ring != null) {
			ring.at(feet.add(0, 0.03, 0));
		}
		float w = caught.getBbWidth() / 2;
		float h = caught.getBbHeight();
		int plus = rates[0].take(4.5f);
		for (int i = 0; i < plus; i++) {
			Particle s = Fx.particle("mercy_plus", Palette.JR_TRAPPED);
			if (s != null) {
				Vec3 p = feet.add((Fx.RANDOM.nextFloat() - 0.5) * w * 2.4, Fx.RANDOM.nextFloat() * h * 0.7, (Fx.RANDOM.nextFloat() - 0.5) * w * 2.4);
				Fx.add(s.at(p).velocity(UP.scale(0.57f + (Fx.RANDOM.nextFloat() - 0.5f) * 0.3f)).life(0.82f + (Fx.RANDOM.nextFloat() - 0.5f) * 0.4f)
					.size(0.2f, 0.28f).fade(0.15f, 0.5f).nearFade(0.5f));
			}
		}
		int wisps = rates[1].take(5);
		for (int i = 0; i < wisps; i++) {
			Particle s = Fx.particle("band", Palette.JR_TRAPPED_B);
			if (s != null) {
				Vec3 p = feet.add((Fx.RANDOM.nextFloat() - 0.5) * w * 2, 0.1 + Fx.RANDOM.nextFloat() * 0.3, (Fx.RANDOM.nextFloat() - 0.5) * w * 2);
				Fx.add(s.at(p).velocity(UP.scale(0.4f)).life(0.5f + (Fx.RANDOM.nextFloat() - 0.5f) * 0.6f).size(0.5f, 0.8f).alpha(0.4f)
					.spin(range(0, 6.28f), 0).fade(0.1f, 0.6f).nearFade(0.8f));
			}
		}
	}

	/**
	 * The trap snapping shut (16E0, H; at the end of its armed state, M the snap): dust 1DFCB 5 at 2.3-3.2 m/s, 1.8-2.2 m, 0.5-1.4 s
	 * (ours 2, slowed); sparks 2C009 18 at 2-5 m/s (ours 10), 0.5-1.4 s (ours 0.3-0.6: at 2-5 m/s they flew too far); metal bits 22FD
	 * (8 frames, a colour sprite) 15 at 3.1-5 m/s (ours 8), 0.2-0.28 m, 0.7-0.94 s, spinning, in a 40 degree cone. Broken: a third.
	 */
	private static void trapSnap(Vec3 at, boolean broken) {
		float k = broken ? 1 / 3f : 1;
		Vec3 c = at.add(0, 0.15, 0);
		for (int i = 0; i < Math.max(1, Math.round(2 * k)); i++) {
			smoke(c, cone(UP, 45).scale(range(0.8f, 1.2f)), Palette.JR_TRAP_SMOKE, range(1.8f, 2.2f) * (broken ? 0.6f : 1),
				range(0.5f, 1.4f), 0.4f);
		}
		for (int i = 0; i < Math.round(10 * k); i++) {
			streak(c, cone(UP, 60).scale(range(2, 5)), Palette.JR_SPARK, range(0.3f, 0.6f));
		}
		for (int i = 0; i < Math.round(8 * k); i++) {
			bit(c, cone(UP, 40).scale(range(3.1f, 5)), range(0.2f, 0.28f), range(0.7f, 0.94f));
		}
	}

	// ---- RIP-Tire ----

	/** The tire's fuse 0079 (in the data on its hub's side; ours from its middle: half its height up, 0.25 m to its left). */
	private static Vec3 fuse(RipTireEntity tire, Vec3 at, float partial) {
		double yaw = Math.toRadians(tire.getYRot(partial));
		Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		return at.add(0, JunkratStats.TIRE_HEIGHT * 0.55, 0).add(left.scale(0.25));
	}

	/**
	 * The tire (156F, H; at its fuse 0079), each frame from its renderer: ribbons 7F5 x 2A480 0.99 m and 0.35 m wide, 0.77 s (ours one,
	 * 0.3 m), orange to black (9804 r6; his enemies' 3422 r8 redder); fuse glows 4C09 0.2 m 5 a second, 0.3 s (DA5F r0 / r1); the glow
	 * ball 3699E on 1385 1.65-1.8 m (ours a 1 m sprite at 0.3 alpha); sparks 183A7 25-35 a second at 5-15 m/s backward and up (ours 9 a
	 * second at 3-6 m/s: at the data's speed and 0.25-0.75 s they flew 10 m). Under Nitro Boost its flare (6740: below).
	 */
	public static void tire(RipTireEntity tire, Vec3 at, float partial) {
		int id = tire.getId();
		boolean enemy = KirikoFx.enemyOf(tire.owner());
		Rate[] rates = RATES.computeIfAbsent(id, k -> new Rate[] {new Rate(), new Rate()});
		Vec3 fuse = fuse(tire, at, partial);
		Fx.trail(id, fuse, "torb_trail", enemy ? Palette.JR_TIRE_TRAIL_ENEMY : Palette.JR_TIRE_TRAIL, 0.3f, 0.77f, 0.9f);
		Fx.trailNearFade(id, 2);
		Palette fuseColour = enemy ? Palette.JR_TIRE_FUSE_ENEMY : Palette.JR_TIRE_FUSE;
		int glows = rates[0].take(5);
		for (int i = 0; i < glows; i++) {
			Particle s = Fx.particle("cat_soft", fuseColour);
			if (s != null) {
				Fx.add(s.at(fuse).life(0.3f).size(0.2f, 0.15f).alpha(0.9f).fade(0.05f, 0.6f).nearFade(0.5f));
			}
		}
		Particle ball = Fx.attach("jr_tire/" + id + "/ball", fuse, () -> {
			Particle s = Fx.particle("jr_glow_ball", Palette.JR_TIRE_GLOW);
			return s == null ? null : s.life(0.3f).size(1, 1).alpha(0.3f).fade(0.1f, 0.5f).nearFade(1.5f);
		});
		if (ball != null) {
			ball.at(fuse);
		}
		double yaw = Math.toRadians(tire.getYRot(partial));
		Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
		int sparks = rates[1].take(9);
		for (int i = 0; i < sparks; i++) {
			Particle s = Fx.particle("cat_streak", Palette.JR_TIRE_SPARK);
			if (s != null) {
				Vec3 d = cone(back.add(0, 0.6, 0), 25);
				Fx.add(s.at(fuse).shape(Shape.STREAK).velocity(d.scale(range(3, 6))).gravity(6).drag(0.2f).life(range(0.3f, 0.5f))
					.size(0.25f, 0.15f).aspect(0.2f).fade(0.02f, 0.5f).nearFade(0.5f));
			}
		}
		Float nitro = NITRO.get(id);
		if (nitro != null) {
			nitroFlare(tire, at, partial, nitro);
		}
	}

	/**
	 * Nitro Boost's flare (6740, H): the mesh 14346 (0.95 x 0.39 x 1.65 m) at size 3, once, 1.2 s, behind the tire (ours at size 1.5, a
	 * 2.5 m flare: at 3 it was 5 m long), its flame colour (397A r3 / r4: JR_TIRE_FLAMES, ours).
	 */
	private static void nitroFlare(RipTireEntity tire, Vec3 at, float partial, float until) {
		float[] flare = Fx.mesh("jr_nitro_flare");
		if (flare == null) {
			return;
		}
		double yaw = Math.toRadians(tire.getYRot(partial));
		Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
		float size = 1.65f * 1.5f;
		Particle f = Fx.attach("jr_nitro/" + tire.getId(), at, () -> {
			Particle s = Fx.particle("torb_flame", Palette.JR_TIRE_FLAMES);
			return s == null ? null : s.life(0.3f).fade(0.05f, 0.6f);
		});
		if (f != null) {
			// its long side (X once Fx.mesh turns it) back from the tire's middle, standing on its base
			f.mesh(flare, back).at(at.add(0, 0.1, 0).add(back.scale(size / 2))).size(size, size).alpha(Math.min(1, (until - Fx.now()) / 0.3f));
		}
	}

	/** Nitro Boost's speed lines E99F (3 frames) at size 10, 50 a second, for 1.17 s, in the driver's view (6740, H; ours 30 a second). */
	private static void speedLines(Minecraft mc) {
		Vec3 eye = mc.gameRenderer.mainCamera().position();
		Vec3 look = mc.player == null ? new Vec3(0, 0, 1) : mc.player.getViewVector(1);
		Rate[] rates = RATES.computeIfAbsent(mc.player == null ? 0 : mc.player.getId(), k -> new Rate[] {new Rate()});
		int n = rates[0].take(30);
		for (int i = 0; i < n; i++) {
			Particle s = Fx.particle("cat_speed_line#3", Palette.JR_TIRE_FUSE);
			if (s == null) {
				return;
			}
			Vec3 side = randomDir().subtract(look.scale(randomDir().dot(look)));
			side = side.lengthSqr() < 1.0E-6 ? UP : side.normalize();
			Vec3 p = eye.add(look.scale(range(2, 5))).add(side.scale(range(1.2f, 2.5f)));
			Fx.add(s.at(p).shape(Shape.STREAK).velocity(look.scale(-15)).life(0.2f).size(0.8f, 0.8f).aspect(0.05f).alpha(0.5f)
				.fade(0.1f, 0.5f).near());
		}
	}

	/**
	 * The tire's cast (3P clip 1749, H): at 1.3 s (the fuse lit, M) sparks 1F35 31-42 at 2.6-8.1 m/s (ours 15 at a third of that),
	 * 0.34-1.03 s, at his left hand; the tire set down (4557): 3 dust puffs BE5 1.1-1.2 m, 0.33-0.4 s, 0.9-1.25 m/s. No decal (L: under
	 * him).
	 */
	private static void tireCast(@Nullable Entity junkrat, Vec3 at) {
		for (int i = 0; i < 3; i++) {
			Particle d = Fx.soft("dust", Palette.JR_TRAP_SMOKE);
			if (d != null) {
				Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), 0.3, Fx.RANDOM.nextGaussian()).normalize();
				Fx.add(d.at(at.add(0, 0.2, 0)).velocity(dir.scale(range(0.9f, 1.25f))).life(range(0.33f, 0.4f)).size(0.8f, range(1.1f, 1.2f))
					.alpha(0.6f).spin(range(0, 6.28f), 0).fade(0.05f, 0.6f).nearFade(1));
			}
		}
		if (junkrat != null) {
			CASTS.add(new Due(junkrat.getId(), Fx.now() + 1.3f));
		}
	}

	private static void castSparks(Entity junkrat) {
		boolean own = ownFirstPerson(junkrat);
		Vec3 at = own ? junkrat.getEyePosition().add(junkrat.getViewVector(1).scale(0.6)).add(0, -0.4, 0) : hand(junkrat);
		for (int i = 0; i < 15; i++) {
			spark("spark#4", at, randomUp().scale(range(2.6f, 8.1f) / 3), Palette.JR_TIRE_SPARK, range(0.34f, 1.03f),
				range(0.04f, 0.06f), 6, own ? 0.2f : 0.5f);
		}
	}

	/**
	 * The tire shot down (1994, H; after the game's message 003E and a 0.12 s wait, M): debris 22FD 20 at 3-7.7 m/s, 0.24-0.46 m (ours
	 * 8); smoke 9EF 10 at 3.4-7.5 m/s, 1.6-2.1 m (ours 3, slowed); 40 sparks 1F35 at 4.2-5.3 m/s, 0.33-0.65 s, 0.056 m (ours 15).
	 */
	private static void tireBroken(Vec3 at) {
		Vec3 c = at.add(0, JunkratStats.TIRE_HEIGHT / 2, 0);
		for (int i = 0; i < 8; i++) {
			bit(c, randomUp().scale(range(3, 7.7f)), range(0.24f, 0.46f), range(0.6f, 1));
		}
		for (int i = 0; i < 3; i++) {
			smoke(c, randomUp().scale(range(1, 2)), Palette.JR_FRAG_SMOKE, range(1.6f, 2.1f) * 0.7f, range(0.8f, 1.2f), 0.45f);
		}
		for (int i = 0; i < 15; i++) {
			spark("spark#4", c, randomDir().scale(range(4.2f, 5.3f)), Palette.JR_TIRE_SPARK, range(0.33f, 0.65f), 0.056f, 6, 0.5f);
		}
	}

	/**
	 * The tire's blast (15D2, H): the flash 2613A 6-7.5 m, 0.3-0.32 s (ours 7 m at 0.25 alpha, faded near the camera: a 7 m flash at
	 * full washed the view white); the sphere 1298 on mesh 085B 4 m, 0.2 s (ours on cat_sphere, at 0.35); the ground ring 4CB7 on 296B
	 * at size 2 (2.6 m), 0.3-0.35 s; fireballs 47C4 1.3-2.7 m, 0.4-0.53 s; the glow A7B6 1 m, 0.18 s; flame tongues 14F9 5 at 8.9-11.2
	 * m/s all ways, 0.3 +- 0.1 s, 1.0 +- 0.7 m; smoke 1DFCB 3 from 0.23 s, 2 m, 1-1.7 s, 4.9-9 m/s (ours slowed); sparks 2C009 10 at
	 * 3-18 m/s; 7 debris at 6-16 m/s living 2.5-3.5 s, each with 35DC behind it (a glow and a 0.5 m smoke ribbon, 0.6 s; ours 0.15 m,
	 * kept here: debris()); the scorch decal on the ground under it.
	 */
	private static void tireBlast(Vec3 at, boolean grounded, boolean enemy) {
		flash(at, Palette.JR_TIRE_FLASH_BIG, 7, range(0.3f, 0.32f), 0.25f);
		float[] sphere = Fx.mesh("cat_sphere");
		Particle shell = sphere == null ? null : Fx.particle("dva_solid", Palette.JR_TIRE_SHELL);
		if (shell != null) {
			// the sphere stands on its base: down by half its height to centre it
			Fx.add(shell.mesh(sphere, new Vec3(1, 0, 0), UP).at(at.subtract(0, 2, 0)).life(0.2f).size(3, 4).easeOut().alpha(0.35f)
				.fade(0.02f, 0.7f).nearFade(2));
		}
		Vec3 ground = at.subtract(0, JunkratStats.TIRE_HEIGHT / 2, 0);
		float[] ring = Fx.mesh("brigitte_ring");
		Particle r = ring == null ? null : Fx.particle("jr_ring_noise", Palette.JR_TIRE_RING);
		if (r != null) {
			// the ring stands in its X-Y plane: laid flat with its Y along +Z, from its far edge (half its 2.6 m back)
			Fx.add(r.mesh(ring, new Vec3(1, 0, 0), new Vec3(0, 0, 1)).at(ground.add(0, 0.1, -1.3)).life(range(0.3f, 0.35f)).size(2, 2.6f)
				.alpha(0.8f).fade(0.02f, 0.6f).nearFade(1));
		}
		for (int i = 0; i < 3; i++) {
			fireball(at, cone(UP, 20).scale(range(1, 2)), Palette.JR_TIRE_FIRE, range(1.3f, 2.7f), range(0.4f, 0.53f));
		}
		Particle glow = Fx.particle("glow", Palette.JR_TIRE_FIRE);
		if (glow != null) {
			Fx.add(glow.at(at).life(0.18f).size(1, 1.2f).fade(0.02f, 0.6f).nearFade(1));
		}
		for (int i = 0; i < 5; i++) {
			Particle f = Fx.particle("lw_thorn_flame#2", Palette.JR_TIRE_FLAMES);
			if (f == null) {
				break;
			}
			Vec3 d = randomDir();
			float len = 1.0f + (Fx.RANDOM.nextFloat() - 0.5f) * 1.4f;
			Fx.add(f.at(at.add(d.scale(0.3))).shape(Shape.STREAK).velocity(d.scale(range(8.9f, 11.2f))).drag(0.02f)
				.life(0.3f + (Fx.RANDOM.nextFloat() - 0.5f) * 0.2f).size(len, len * 1.4f).aspect(0.5f).fade(0.05f, 0.6f).nearFade(1.5f));
		}
		for (int i = 0; i < 3; i++) {
			smoke(at, randomUp().scale(range(1.5f, 3)), Palette.JR_FRAG_SMOKE, 2, range(1, 1.7f), 0.45f);
		}
		for (int i = 0; i < 10; i++) {
			streak(at, randomDir().scale(range(3, 18)), Palette.JR_SPARK, range(0.3f, 0.5f));
		}
		float now = Fx.now();
		for (int i = 0; i < 7; i++) {
			DEBRIS.add(new Debris(debrisSerial--, at, randomUp().scale(range(6, 16)), now, range(2.5f, 3.5f), enemy));
		}
		if (grounded) {
			scorch(at, UP, 3, 4);
		}
	}

	/** Each frame: the tire blasts' debris fly and fall (9.8 m/s², ours), each with its glow and ribbon (35DC); a block stops one. */
	private static void debris(float now) {
		float dt = debrisLast < 0 ? 0 : Math.clamp(now - debrisLast, 0, 0.1f);
		debrisLast = now;
		var level = Minecraft.getInstance().level;
		DEBRIS.removeIf(d -> {
			if (now - d.born > d.life || now < d.born - 1 || level == null) {
				return true;
			}
			d.velocity = d.velocity.add(0, -9.8 * dt, 0).scale(Math.pow(0.6, dt));
			Vec3 next = d.at.add(d.velocity.scale(dt));
			BlockPos block = BlockPos.containing(next);
			if (!level.getBlockState(block).getCollisionShape(level, block).isEmpty()) {
				return true;
			}
			d.at = next;
			Fx.trail(d.id, d.at, "torb_trail", d.enemy ? Palette.JR_TIRE_TRAIL_ENEMY : Palette.JR_TIRE_TRAIL, 0.15f, 0.6f, 0.8f);
			Particle g = Fx.attach("jr_debris/" + d.id, d.at, () -> {
				Particle s = Fx.particle("cat_soft", Palette.JR_TIRE_SPARK);
				return s == null ? null : s.life(0.3f).size(0.25f, 0.25f).alpha(0.8f).fade(0.05f, 0.5f).nearFade(0.8f);
			});
			if (g != null) {
				g.at(d.at);
			}
			return false;
		});
	}

	// ---- the pieces ----

	/** A blast's flash (2613A): one sprite, its alpha kept moderate and faded over its own size near the camera (ours, as IllariFx.burst). */
	private static void flash(Vec3 at, Palette palette, float size, float life, float alpha) {
		Particle f = Fx.particle("jr_flash", palette);
		if (f != null) {
			Fx.add(f.at(at).life(life).size(size * 0.8f, size).alpha(alpha).fade(0.02f, 0.7f).nearFade(Math.max(1.5f, size * 0.6f)));
		}
	}

	/** One fireball 47C4: its 11 frames over its life, growing a third, turned at random. */
	private static void fireball(Vec3 at, Vec3 velocity, Palette palette, float size, float life) {
		Particle f = Fx.particle("jr_fireball#11", palette);
		if (f != null) {
			Fx.add(f.at(at).velocity(velocity).drag(0.1f).life(life).size(size * 0.8f, size * 1.1f).spin(range(0, 6.28f), range(-1, 1))
				.fade(0.02f, 0.4f).nearFade(Math.max(1, size)));
		}
	}

	/** One blast spark 2C009 (a 64 x 128 streak), stretched along its flight, falling (ours: 0.3 m long, 0.05 m wide). */
	private static void streak(Vec3 at, Vec3 velocity, Palette palette, float life) {
		Particle s = Fx.particle("jr_spark_streak", palette);
		if (s != null) {
			float len = range(0.25f, 0.4f);
			Fx.add(s.at(at).shape(Shape.STREAK).velocity(velocity).gravity(9.8f).drag(0.1f).life(life).size(len, len * 0.6f)
				.aspect(0.15f).fade(0.02f, 0.5f).nearFade(0.5f));
		}
	}

	/** One spark streak from a 4-frame spark mask (1F35), stretched along its flight. */
	private static void spark(String mask, Vec3 at, Vec3 velocity, Palette palette, float life, float size, float gravity, float near) {
		Particle s = Fx.particle(mask, palette);
		if (s != null) {
			Fx.add(s.at(at).shape(Shape.STREAK).velocity(velocity).gravity(gravity).life(life).size(size * 3, size * 2).aspect(0.35f)
				.fade(0.05f, 0.5f).nearFade(near));
		}
	}

	/** One smoke 1DFCB (32 frames), blended, slowing as it rises. */
	private static void smoke(Vec3 at, Vec3 velocity, Palette palette, float size, float life, float alpha) {
		Particle s = Fx.soft("brigitte_smoke#32", palette);
		if (s != null) {
			Fx.add(s.at(at).velocity(velocity).drag(0.15f).life(life).size(size * 0.6f, size).alpha(alpha).spin(range(0, 6.28f), range(-0.5f, 0.5f))
				.fade(0.1f, 0.6f).nearFade(Math.max(1, size)));
		}
	}

	/** One metal bit 22FD (8 frames, a colour sprite: drawn as it is), spinning, falling. */
	private static void bit(Vec3 at, Vec3 velocity, float size, float life) {
		Particle b = Fx.particle("jr_debris#8", Palette.NATURAL);
		if (b != null) {
			Fx.add(b.at(at).velocity(velocity).gravity(9.8f).life(life).size(size, size).spin(range(0, 6.28f), range(-8, 8))
				.loop(range(8, 16)).fade(0.02f, 0.3f).nearFade(0.5f));
		}
	}

	/**
	 * The scorch decal 31E8 (13C6 / 151A / 15D2, H; dark, C7EA): on the surface within a metre behind the blast along -normal (ours: it
	 * finds that face), else nowhere; drawn blended, fading over its last half.
	 */
	private static void scorch(Vec3 at, Vec3 normal, float size, float life) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		BlockHitResult hit = mc.level.clip(new ClipContext(at.add(normal.scale(0.2)), at.subtract(normal.scale(1.2)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
		if (hit.getType() == HitResult.Type.MISS) {
			return;
		}
		Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
		Particle s = Fx.soft("jr_scorch", Palette.JR_SCORCH);
		if (s != null) {
			Fx.add(s.at(hit.getLocation().add(n.scale(0.02))).normal(n).lift(n).life(life).size(size, size).alpha(0.85f)
				.spin(range(0, 6.28f), 0).fade(0.02f, 0.5f));
		}
	}

	// ---- from the server ----

	/** His effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		if (!e.effect().startsWith("jr_")) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Entity entity = mc.level == null || e.entity() < 0 ? null : mc.level.getEntity(e.entity());
		switch (e.effect()) {
			case "jr_frag_launch" -> launch(entity, at, dir);
			case "jr_frag_burst" -> fragBurst(at, dir);
			case "jr_bomb_burst" -> bombBurst(at);
			case "jr_reload" -> {
				if (e.entity() >= 0) {
					RELOADS.removeIf(r -> r.entity == e.entity());
					RELOADS.add(new Reload(e.entity(), Fx.now(), Math.max(5, e.variant()) / 20f));
				}
			}
			case "jr_reload_cut" -> RELOADS.removeIf(r -> r.entity == e.entity());
			case "jr_mine_blast" -> mineBlast(at, dir, enemy);
			case "jr_trap_snap" -> trapSnap(at, false);
			case "jr_trap_broken" -> trapSnap(at, true);
			case "jr_tire_cast" -> tireCast(entity, at);
			case "jr_tire_broken" -> tireBroken(at);
			case "jr_tire_blast" -> tireBlast(at, e.variant() == 1, enemy);
			case "jr_nitro" -> {
				if (e.entity() >= 0) {
					// its flare and the driver's lines for 1.2 s (6740's 1.17-1.2 s, H)
					NITRO.put(e.entity(), Fx.now() + 1.2f);
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	private static Vec3 randomDir() {
		Vec3 d = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return d.lengthSqr() < 1.0E-6 ? UP : d.normalize();
	}

	private static Vec3 randomUp() {
		Vec3 d = new Vec3(Fx.RANDOM.nextGaussian(), Math.abs(Fx.RANDOM.nextGaussian()), Fx.RANDOM.nextGaussian());
		return d.lengthSqr() < 1.0E-6 ? UP : d.normalize();
	}

	/** A direction within this many degrees of axis. */
	private static Vec3 cone(Vec3 axis, float degrees) {
		Vec3 a = axis.lengthSqr() < 1.0E-6 ? UP : axis.normalize();
		Vec3 side = a.cross(Math.abs(a.y) < 0.9 ? UP : new Vec3(1, 0, 0)).normalize();
		Vec3 up = side.cross(a);
		double turn = Fx.RANDOM.nextDouble() * Math.PI * 2;
		double off = Math.tan(Math.toRadians(Math.min(degrees, 89))) * Math.sqrt(Fx.RANDOM.nextDouble());
		return a.add(side.scale(Math.cos(turn) * off)).add(up.scale(Math.sin(turn) * off)).normalize();
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}
}
