package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.juno.JunoStats;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Juno's effects (tools/reference/juno-effects.md, read from her effects' data; each moment's numbers there). Her look is
 * cyan, blue and yellow-white to her team and red to her enemies: nearly every LUT of hers carries both rows (the Hyper
 * Ring's two looks 1BABB / 1BC0D settle which is which). Her ability graphs hold the visuals; her clips add the reload and
 * the boots' jets. The server sends her moments (EffectPayload "juno_*"); the torpedoes' and the ring's flights, the ray's
 * walk along the ground, her pods, boots and jets are drawn each client tick from what it sent and from her posed bones.
 */
public final class JunoFx {
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/**
	 * Her model's points the renderer hands over each frame: the Mediblaster's muzzle (hp 0006), the torpedo pods (bones 0182
	 * / 0183 = hp 048D / 048E) and their flaps (3CFB / 3CFC = hp 051D / 051E), her feet (her boots' jets 002C / 002E aren't in
	 * her 3P model's table: the foot bones, ours), her right wrist (the ray's cast on her hand, hp 007B-0080: M), the gun piece
	 * swapped in the reload (hp 01B5).
	 */
	public static final String[] POINTS = {"hardpoint_0006", "bone_0182", "bone_0183", "bone_3CFB", "bone_3CFC", "Foot_L", "Foot_R",
		"Wrist_R", "hardpoint_01B5"};
	private static final int MUZZLE = 0, POD = 1, FLAP = 3, FOOT = 5, HAND = 7, GUN = 8;

	private static final class Posed {
		Vec3[] points = new Vec3[0];
		long tick;
	}

	/** A state on an entity: since when, until when (Fx.now() seconds), and whether its giver is an enemy of ours. */
	private record Held(float since, float until, boolean enemy) {
	}

	/** A torpedo in flight (the server flies it: Torpedoes.tickAll; drawn here on the same homing): at whom, healing or not. */
	private static final class Torpedo {
		final int key;
		final int target;
		final boolean heal, enemy;
		final float since;
		Vec3 at;
		@Nullable Vec3 heading;

		Torpedo(int key, int target, Vec3 at, boolean heal, boolean enemy, float since) {
			this.key = key;
			this.target = target;
			this.at = at;
			this.heal = heal;
			this.enemy = enemy;
			this.since = since;
		}
	}

	/** A Hyper Ring: thrown from, along, when; where its flight ends (its 0.5 s at 35 m/s, or a wall). */
	private record Ring(int key, Vec3 from, Vec3 dir, Vec3 stop, float since, boolean enemy) {
		Vec3 centre(float now) {
			double flown = Math.min(JunoStats.RING_SPEED * (now - since), from.distanceTo(stop));
			return from.add(dir.scale(flown));
		}
	}

	/** An Orbital Ray walking along the ground: where it is, the way it goes, its ticks so far and in all. */
	private static final class Ray {
		final int key;
		final Vec3 along;
		final int life;
		final boolean enemy;
		/** Its column's pieces still falling, with their full alpha: faded as the camera nears its axis. */
		final List<Particle> pieces = new ArrayList<>();
		final List<Float> piecesAlpha = new ArrayList<>();
		Vec3 at;
		int step;

		Ray(int key, Vec3 at, Vec3 along, int life, boolean enemy) {
			this.key = key;
			this.at = at;
			this.along = along;
			this.life = life;
			this.enemy = enemy;
		}
	}

	private static final Map<Integer, Posed> POSED = new HashMap<>();
	private static final List<Torpedo> TORPEDOES = new ArrayList<>();
	private static final List<Ring> RINGS = new ArrayList<>();
	private static final List<Ray> RAYS = new ArrayList<>();
	/** Her lock markers on the targets she has locked (her own client only), the ring's boost, the ray's allies. */
	private static final Map<Integer, Held> LOCKS = new HashMap<>();
	private static final Map<Integer, Held> BOOSTED = new HashMap<>();
	private static final Map<Integer, Held> RAYED = new HashMap<>();
	/** Each Juno's last reload whose sparks were drawn (its start, game s): once a reload. */
	private static final Map<Integer, Float> RELOAD_DONE = new HashMap<>();
	private static int keys;

	/** The Mediblaster's round in flight (53B6: 0.33 s; its speed isn't in the data: ours, fast enough to read as a tracer). */
	private static final float ROUND_SPEED = 100, ROUND_LIFE = 0.33f;
	/** The ally hit's flashes against the data's sizes (ours: see allyHit). */
	private static final float HIT_SCALE = 0.3f;
	/** The longest lock (JunoStats.LOCK_MAX, 1 s at 40 m): the pods' glow grows over it (5254 by the lock's progress, M; ours). */
	private static final float POD_FULL = (float) JunoStats.LOCK_MAX;
	/** The pods' fire trails after a volley (62B6, 1.6 s). */
	private static final float FIRE_TRAILS = 1.6f;
	/** The Hyper Ring: 9.25 m across (15AD6, 0.37 m, at scale 25), 0.28 m thick (its Y at scale 5); its pop-in (C46 / C47, M). */
	private static final float RING_SIZE = 9.25f, RING_SQUASH = 5 / 25f, RING_POP = 0.08f;
	/** The ring's end (5D44, 1.67 s from its 6 s): the inward streaks to 0.3 s, the disc flash at 0.37 s, the pop at 0.6 s. */
	private static final float END_STREAKS = 0.3f, END_DISC = 0.37f, END_POP = 0.6f, END_DONE = 0.8f;
	/** The ray's column and its falling light: drawn up to here over the ground (the data's from +200 m: ours, so it reads). */
	private static final float COLUMN_HEIGHT = 100;
	/** The column: the tube 15A21 at size 6 is 5.4 m across; its pieces' length and fall (ours). */
	private static final float COLUMN_WIDTH = 5.4f, COLUMN_PIECE = 30, COLUMN_FALL = 40;
	/** The ray's ground circle (C32: 8.5 m half-extents). */
	private static final float RAY_CIRCLE = 17;
	/** The ray's falling light (49944 at +70 m, 40-45 m/s) and rings (size 1.5, from +150 m, 28 m/s, 4 a second). */
	private static final float RAIN_FROM = 70, RAIN_SPEED = 42, RINGS_FROM = 140, RINGS_SPEED = 28;
	/** The fat torus 15A26 is 3.46 m across at size 1 (Fx.mesh hands it back one metre wide). */
	private static final float TORUS = 3.46f;

	private static float[] squashedRing;
	private static float[] squashedFrom;

	private JunoFx() {
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}

	private static Vec3 randomUnit() {
		Vec3 v = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return v.lengthSqr() < 1.0E-6 ? UP : v.normalize();
	}

	private static Vec3 facing(Entity e) {
		float yaw = e.getYRot() * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
	}

	private static Vec3 chest(Entity e) {
		return e.position().add(0, e.getBbHeight() * 0.6, 0);
	}

	/**
	 * Where a flat quad's centre goes so its texture corner (1, 1) sits on a point, turned by turn about up, at half-size h
	 * (Fx's FLAT axes for a normal straight up: X along +Z, Y along +X).
	 */
	private static Vec3 quarter(float turn, float h) {
		Vec3 ax = new Vec3(0, 0, 1), ay = new Vec3(1, 0, 0);
		float cos = Mth.cos(turn), sin = Mth.sin(turn);
		Vec3 rx = ax.scale(cos).add(ay.scale(sin));
		Vec3 ry = ay.scale(cos).subtract(ax.scale(sin));
		// corner (1, 1) is at centre + rx - ry
		return rx.subtract(ry).scale(-h);
	}

	/** Any unit vector square to d. */
	private static Vec3 across(Vec3 d) {
		Vec3 a = Math.abs(d.y) < 0.9 ? UP.cross(d) : new Vec3(1, 0, 0).cross(d);
		return a.normalize();
	}

	/** The local player in first person: their own states aren't drawn round their eyes. */
	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/** The ground under a point (from 3 m above it down to 20 m below), as the server's OrbitalRay.ground (liquids' surface: #80). */
	private static Vec3 ground(Level level, Vec3 at, @Nullable Entity by) {
		var hit = level.clip(new ClipContext(at.add(0, 3, 0), at.add(0, -20, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
			by == null ? net.minecraft.world.phys.shapes.CollisionContext.empty() : net.minecraft.world.phys.shapes.CollisionContext.of(by)));
		return hit.getType() == HitResult.Type.MISS ? at : dev.overcraft.juno.OrbitalRay.onTop(level, hit);
	}

	// ---- her posed points (HeroModelRenderer) ----

	public static void posed(Entity juno, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(juno.getId(), id -> new Posed());
		p.points = points;
		p.tick = juno.level().getGameTime();
	}

	/** Her posed point this tick or the last (null in her own first person, unposed, or without that bone). */
	private static @Nullable Vec3 point(@Nullable Entity juno, int which) {
		if (juno == null || ownFirstPerson(juno)) {
			return null;
		}
		Posed p = POSED.get(juno.getId());
		// (a slow frame or a screenshot's stall skips ticks: past 1 tick her pods' glow fell back to the rest place on her chest,
		// critique M4; a quarter second of old pose is nearer than that)
		return p == null || juno.level().getGameTime() - p.tick > 5 || which >= p.points.length ? null : p.points[which];
	}

	/**
	 * A torpedo pod (hp 048D / 048E): posed, or where the server launches from (1.35 m up, 0.19 m behind, 0.04 m to the side:
	 * the hardpoints' rest place, Torpedoes.launch).
	 */
	private static Vec3 pod(Entity juno, int side) {
		if (ownFirstPerson(juno)) {
			// her own view: the pod as her view model draws it (HeroModelRenderer.viewPod: up in a top corner while she locks)
			org.joml.Vector3f view = dev.overcraft.client.model.HeroModelRenderer.viewPod(side);
			if (view != null) {
				return Minecraft.getInstance().gameRenderer.mainCamera().position().add(view.x, view.y, view.z);
			}
		}
		Vec3 posed = point(juno, POD + side);
		if (posed != null) {
			return posed;
		}
		Vec3 ahead = facing(juno);
		Vec3 right = new Vec3(-ahead.z, 0, ahead.x);
		return juno.position().add(0, 1.35, 0).add(ahead.scale(-0.19)).add(right.scale(side == 0 ? 0.04 : -0.04));
	}

	/** Her muzzle: posed (hp 0006), or in her own view under the crosshair right of it (ours, as her 1P gun sits). */
	private static Vec3 muzzle(Entity juno) {
		Vec3 posed = point(juno, MUZZLE);
		if (posed != null) {
			return posed;
		}
		Vec3 look = juno.getViewVector(1);
		Vec3 right = across(look).scale(-1);
		if (right.dot(new Vec3(-facing(juno).z, 0, facing(juno).x)) < 0) {
			right = right.scale(-1);
		}
		Vec3 up = right.cross(look).normalize();
		return juno.getEyePosition().add(look.scale(0.55)).add(right.scale(0.18)).add(up.scale(-0.15));
	}

	// ---- each client tick ----

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			return;
		}
		float now = Fx.now();
		for (Player p : mc.level.players()) {
			if ("juno".equals(p.getAttached(dev.overcraft.hero.HeroManager.HERO)) && p.isAlive()) {
				pods(p);
				reload(p);
				boots(p);
				cast(p);
			}
		}
		torpedoes(mc, now);
		locks(mc, now);
		rings(mc, now);
		rays(mc);
		boosted(mc, now);
		rayed(mc, now);
		POSED.keySet().removeIf(id -> mc.level.getEntity(id) == null);
	}

	/** A held state's draw on its entity each tick. */
	private interface Draw {
		void draw(LivingEntity entity, Held held, float now);
	}

	private static void held(Minecraft mc, Map<Integer, Held> states, float now, Draw draw) {
		for (Iterator<Map.Entry<Integer, Held>> it = states.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Held> entry = it.next();
			Entity e = mc.level.getEntity(entry.getKey());
			if (!(e instanceof LivingEntity living) || !living.isAlive() || now >= entry.getValue().until) {
				it.remove();
				continue;
			}
			draw.draw(living, entry.getValue(), now);
		}
	}

	// ---- Mediblaster ----

	/**
	 * One round (the server's juno_round: from her eyes to where it stopped; variant 1 an ally hit, 2 an enemy). The muzzle
	 * (51DA on hp 0006): lens flares 0.2 m and 0.7 m and soft dots 5.5-12 cm, 0.05-0.1 s, blue then yellow (red then peach to
	 * her enemies). The round (53B6): a 0.25 m streak along the shot (strips 3A512 x 47DD9, juno_tracer), navy to gold, with a
	 * gold core. In her own view the muzzle is a third the size and fainter (ours, as Sombra's and Ana's).
	 */
	private static void round(@Nullable Entity juno, Vec3 eye, Vec3 shot, int hit, boolean enemy) {
		if (juno == null) {
			return;
		}
		boolean own = ownFirstPerson(juno);
		Vec3 from = muzzle(juno);
		Vec3 end = eye.add(shot);
		Vec3 way = end.subtract(from);
		double length = way.length();
		if (length < 0.3) {
			return;
		}
		Vec3 dir = way.scale(1 / length);
		float k = own ? 0.35f : 1;
		Palette flash = enemy ? Palette.JUNO_MEDI_FLASH_ENEMY_N : Palette.JUNO_MEDI_FLASH_N;
		Vec3 front = from.add(dir.scale(0.03));
		for (float s : new float[] {0.2f, 0.7f}) {
			Particle lens = Fx.particle("juno_lens_flare", flash);
			if (lens != null) {
				Fx.add(lens.at(front).life(range(0.05f, 0.1f)).size(s * k, s * k * 0.6f).alpha(own ? 0.3f : 0.6f).fade(0.01f, 0.5f)
					.nearFade(own ? 0 : 0.3f));
			}
		}
		Particle dot = Fx.particle("juno_soft_dot", enemy ? Palette.JUNO_MEDI_MUZZLE_ENEMY_N : Palette.JUNO_MEDI_MUZZLE);
		if (dot != null) {
			Fx.add(dot.at(front).life(range(0.05f, 0.1f)).size(range(0.055f, 0.12f) * k, 0.04f * k).alpha(own ? 0.4f : 0.8f).fade(0.01f, 0.5f)
				.nearFade(own ? 0 : 0.3f));
		}
		// the round: flies the shot at ROUND_SPEED, at most its 0.33 s
		float life = (float) Math.min(ROUND_LIFE, length / ROUND_SPEED);
		Vec3 v = dir.scale(Math.min(ROUND_SPEED, length / ROUND_LIFE));
		Particle round = Fx.particle("juno_tracer", enemy ? Palette.JUNO_MEDI_TRACER_ENEMY_N : Palette.JUNO_MEDI_TRACER_N);
		if (round != null) {
			Fx.add(round.at(from.add(dir.scale(0.15))).shape(Shape.STREAK).velocity(v).life(life).size(0.25f, 0.25f).aspect(0.16f)
				.fade(0.05f, 0.2f).nearFade(own ? 1.5f : 0.5f));
		}
		Particle core = Fx.particle("juno_tracer", enemy ? Palette.JUNO_MEDI_TRACER_ENEMY_N : Palette.JUNO_MEDI_TRACER_CORE_N);
		if (core != null) {
			Fx.add(core.at(from.add(dir.scale(0.15))).shape(Shape.STREAK).velocity(v).life(life).size(0.2f, 0.2f).aspect(0.06f)
				.fade(0.05f, 0.2f).nearFade(own ? 1.5f : 0.5f));
		}
		if (hit == 1) {
			allyHit(end, enemy, life);
		} else if (hit == 2) {
			// an enemy shows nothing of hers (5841 is empty): ours, a 2-tick red spark so the hit reads at all
			Particle spark = Fx.particle("spark#4", Palette.ENEMY);
			if (spark != null) {
				spark.age = -life;
				Fx.add(spark.at(end).life(0.1f).size(range(0.12f, 0.18f), 0.08f).spin(range(0, 6.28f), 0).fade(0.01f, 0.5f).nearFade(0.3f));
			}
		}
	}

	/**
	 * A round on an ally (58B4), once it gets there: a ring flash 1CB5E (4 frames) of 2.37-2.55 m for 0.19-0.24 s, black to
	 * cyan, and a 2-2.5 m star glow 266F, pale yellow-green. Both at HIT_SCALE (ours: at the data's size, and at half, the
	 * flashes of 34 rounds a second stacked to a solid ball over a villager, hiding it: found playing).
	 */
	private static void allyHit(Vec3 at, boolean enemy, float delay) {
		Particle ring = Fx.particle("juno_hit_ring#4", enemy ? Palette.JUNO_MEDI_FLASH_ENEMY_N : Palette.JUNO_MEDI_ALLY_HIT_N);
		if (ring != null) {
			ring.age = -delay;
			Fx.add(ring.at(at).life(range(0.19f, 0.24f)).size(range(2.37f, 2.55f) * HIT_SCALE, 2.5f * HIT_SCALE).spin(range(0, 6.28f), 0)
				.alpha(0.5f).fade(0.01f, 0.4f).nearFade(0.5f));
		}
		Particle glow = Fx.particle("juno_star_glow", enemy ? Palette.JUNO_MEDI_FLASH_ENEMY_N : Palette.JUNO_MEDI_ALLY_HIT_GLOW_N);
		if (glow != null) {
			glow.age = -delay;
			Fx.add(glow.at(at).life(range(0.22f, 0.27f)).size(range(2, 2.5f) * HIT_SCALE, 2 * HIT_SCALE).alpha(0.2f).fade(0.01f, 0.5f).nearFade(0.5f));
		}
	}

	/** A round on a block (53B7): a 0.5 x 0.5 m decal on the face, random spin, 0.27 s, black to pale yellow (pink to enemies). */
	private static void blockHit(Vec3 at, Vec3 normal, boolean enemy) {
		if (normal.lengthSqr() < 1.0E-6) {
			return;
		}
		Particle mark = Fx.particle("juno_hit_ring#4", enemy ? Palette.JUNO_MEDI_WORLD_ENEMY_N : Palette.JUNO_MEDI_WORLD_N);
		if (mark != null) {
			Fx.add(mark.at(at.add(normal.scale(0.02))).normal(normal).life(0.27f).size(0.5f, 0.5f).spin(range(0, 6.28f), 0)
				.fade(0.01f, 0.5f).nearFade(0.3f));
		}
	}

	/**
	 * The reload (her clip F6C3's 60BA, 1.57 s): sparks on a 2 cm torus 8 a second at 0.88 s rising 0.45 m/s and a short tube
	 * flash at 0.9 s, by the gun piece (hp 01B5); a 0.1 m ribbon from it 0.51-1.1 s (JUNO_RELOAD_RIBBON). The gun piece 5ADB
	 * swapped out 0.17-0.57 s isn't drawn (her gun model has it in place). Sparks a little bigger than the data's (ours: 2 cm
	 * sparks don't read at Minecraft's distances).
	 */
	private static void reload(Player juno) {
		HeroAnimator a = HeroAnimator.of(juno);
		float t = juno.level().getGameTime() / 20f;
		float since = a.since("reload", t);
		if (since > 1.2f) {
			return;
		}
		Vec3 gun = point(juno, GUN);
		if (gun == null) {
			gun = point(juno, MUZZLE);
		}
		if (gun == null) {
			return;
		}
		boolean enemy = KirikoFx.enemyOf(juno);
		if (since >= 0.51f && since <= 1.1f) {
			Fx.trail(-(500_000 + juno.getId()), gun, "lw_wisp", enemy ? Palette.JUNO_RELOAD_RIBBON_ENEMY_N : Palette.JUNO_RELOAD_RIBBON_N,
				0.1f, 0.22f, 0.7f);
		}
		Float done = RELOAD_DONE.get(juno.getId());
		float start = t - since;
		if (since >= 0.88f && (done == null || Math.abs(done - start) > 0.01f)) {
			RELOAD_DONE.put(juno.getId(), start);
			for (int i = 0; i < 6; i++) {
				Particle spark = Fx.particle("spark#4", enemy ? Palette.ENEMY : Palette.JUNO_RELOAD_SPARK_N);
				if (spark != null) {
					Fx.add(spark.at(gun.add(randomUnit().scale(0.02))).velocity(new Vec3(0, 0.45, 0).add(randomUnit().scale(0.3)))
						.life(range(0.2f, 0.4f)).size(range(0.05f, 0.08f), 0.02f).fade(0.05f, 0.5f).nearFade(0.3f));
				}
			}
			Particle flash = Fx.particle("juno_soft_dot", enemy ? Palette.JUNO_RELOAD_RIBBON_ENEMY_N : Palette.JUNO_RELOAD_SPARK_N);
			if (flash != null) {
				flash.age = -0.02f;
				Fx.add(flash.at(gun).life(0.12f).size(0.1f, 0.18f).alpha(0.8f).fade(0.05f, 0.5f).nearFade(0.3f));
			}
		}
	}

	// ---- Pulsar Torpedoes ----

	/**
	 * The pods while she locks (5254 on hp 048D / 048E, held: sizes by the lock's progress, M input): per pod a burst glow
	 * 37C36 growing to 1 m over the last quarter, a star flare E221 to 0.73 m, sparks drifting back, blue (red to her enemies).
	 * The progress here: her time locking over the longest lock (ours). After a volley, the pods' fire trails (62B6: 0.15 m
	 * ribbons, life 0.3 s, for 1.6 s; drawn while posed). In her own view on the view model's pods as drawn (up in the top
	 * corners while she locks: HeroModelRenderer.viewPod), a quarter the size (they're half a metre from her eyes there), no
	 * sparks or trails (#86: nothing showed on them in her view).
	 */
	private static void pods(Player juno) {
		boolean own = ownFirstPerson(juno);
		HeroAnimator a = HeroAnimator.of(juno);
		float t = juno.level().getGameTime() / 20f;
		float lockFor = a.since("torpedo_lock", t);
		boolean locking = lockFor < a.since("torpedo_fire", t) && lockFor < a.since("torpedo_cancel", t) && lockFor < a.since("ult", t)
			&& lockFor < JunoStats.LOCK_WINDOW + 0.1f;
		boolean enemy = KirikoFx.enemyOf(juno);
		if (locking) {
			float progress = Math.min(1, lockFor / POD_FULL);
			// the burst glow only over the last quarter of it
			float burst = Math.max(0, (progress - 0.75f) / 0.25f);
			for (int side = 0; side < 2; side++) {
				int s = side;
				// in her own view the particle follows the drawn pod (its position a drift from it: none)
				Vec3 at = own ? Vec3.ZERO : pod(juno, side);
				java.util.function.Supplier<org.joml.Vector3f> view = () -> {
					org.joml.Vector3f v = dev.overcraft.client.model.HeroModelRenderer.viewPod(s);
					return v != null ? v : new org.joml.Vector3f(0, 0, -100);
				};
				float k = own ? 0.25f : 1;
				Particle glow = Fx.attach("juno_pod/" + juno.getId() + "/" + side + (own ? "/view" : ""), at, () -> {
					Particle g = Fx.particle("lw_burst", enemy ? Palette.JUNO_POD_GLOW_ENEMY_N : Palette.JUNO_POD_GLOW_N);
					return g == null ? null : own ? g.life(0.2f).alpha(0.8f).fade(0.05f, 0.5f).follow(view).near()
						: g.life(0.2f).alpha(0.8f).fade(0.05f, 0.5f).nearFade(0.5f);
				});
				if (glow != null) {
					float size = (0.15f + 0.85f * burst) * k;
					glow.size(size, size);
				}
				Particle star = Fx.attach("juno_pod_star/" + juno.getId() + "/" + side + (own ? "/view" : ""), at, () -> {
					Particle g = Fx.particle("lw_star", enemy ? Palette.JUNO_POD_GLOW_ENEMY_N : Palette.JUNO_POD_GLOW_N);
					return g == null ? null : own ? g.life(0.2f).spin(s * 1.2f, 2).alpha(0.7f).fade(0.05f, 0.5f).follow(view).near()
						: g.life(0.2f).spin(s * 1.2f, 2).alpha(0.7f).fade(0.05f, 0.5f).nearFade(0.5f);
				});
				if (star != null) {
					float size = 0.73f * progress * k;
					star.size(size, size);
				}
				// sparks up to 20 a second, drifting back
				if (!own && Fx.RANDOM.nextFloat() < progress) {
					Particle spark = Fx.particle("spark#4", enemy ? Palette.JUNO_POD_GLOW_ENEMY_N : Palette.JUNO_POD_GLOW_N);
					if (spark != null) {
						Fx.add(spark.at(at).velocity(facing(juno).scale(-range(0.3f, 1)).add(randomUnit().scale(0.3))).life(range(0.2f, 0.4f))
							.size(range(0.04f, 0.08f), 0.02f).fade(0.05f, 0.5f).nearFade(0.3f));
					}
				}
			}
		}
		if (a.since("torpedo_fire", t) < FIRE_TRAILS) {
			for (int side = 0; side < 2; side++) {
				Vec3 flap = point(juno, FLAP + side);
				Vec3 at = flap != null ? flap : point(juno, POD + side);
				if (at != null) {
					Fx.trail(-(400_000 + juno.getId() * 2 + side), at, "juno_heal_trail",
						enemy ? Palette.JUNO_POD_RIBBON_ENEMY_N : Palette.JUNO_POD_RIBBON_N, 0.15f, 0.3f, 0.8f);
				}
			}
		}
	}

	/**
	 * A torpedo launched (5AF3 on its pod): soft dots 0.35 / 0.64 m and lens flares 0.65 / 1.5 m for 0.05-0.1 s, blue to
	 * yellow (red to peach to her enemies); then its flight.
	 */
	private static int podLaunches;

	/**
	 * Where a torpedo the server launched at this point leaves from as drawn: from the pods of the Juno it left (in turn), as
	 * posed (raised over her shoulders while she locks) or in her own view as her view model draws them. The server's point,
	 * her back's hardpoints at rest, is behind her eyes and under the raised pods (#86).
	 */
	private static Vec3 podLaunch(Minecraft mc, Vec3 at) {
		if (mc.level != null) {
			for (Player p : mc.level.players()) {
				if ("juno".equals(p.getAttached(dev.overcraft.hero.HeroManager.HERO)) && at.distanceToSqr(p.getEyePosition()) < 1.5 * 1.5) {
					return pod(p, podLaunches++ & 1);
				}
			}
		}
		return at;
	}

	private static void launch(Vec3 at, int target, boolean heal, boolean enemy) {
		LOCKS.remove(target);
		Palette p = enemy ? Palette.JUNO_TORP_LAUNCH_ENEMY_N : Palette.JUNO_TORP_LAUNCH_N;
		// in her own view her pods are half a metre from her eyes: a quarter the size, as their lock glow (critique M5)
		Minecraft mc = Minecraft.getInstance();
		float k = mc.player != null && ownFirstPerson(mc.player) && at.distanceToSqr(mc.player.getEyePosition()) < 2 * 2 ? 0.25f : 1;
		for (float s0 : new float[] {0.65f, 1.5f}) {
			float s = s0 * k;
			Particle lens = Fx.particle("juno_lens_flare", p);
			if (lens != null) {
				Fx.add(lens.at(at).life(range(0.05f, 0.1f)).size(s, s * 0.7f).alpha(0.35f).fade(0.01f, 0.5f).nearFade(1.5f));
			}
		}
		for (float s0 : new float[] {0.35f, 0.64f}) {
			float s = s0 * k;
			Particle dot = Fx.particle("juno_soft_dot", p);
			if (dot != null) {
				Fx.add(dot.at(at).life(range(0.05f, 0.1f)).size(s, s * 0.7f).alpha(0.35f).fade(0.01f, 0.5f).nearFade(1.5f));
			}
		}
		TORPEDOES.add(new Torpedo(-(100_000 + (keys++ & 0xFFFF)), target, at, heal, enemy, Fx.now()));
	}

	/**
	 * The torpedoes in flight, homing on their targets as the server's do (24 m/s; the enemy's from 7.2 m/s over its first 0.3
	 * s: Torpedoes.tickAll). The healing one (51E8): core spheres 0.25-0.5 m (cream), a nose half-shell 160EA 0.7 m, sparks
	 * 2-2.2 m/s back, a 0.38 m trail (lw_wisp x cat_fade, gold-white; the data's two 0.38 / 0.76 m trails as one 0.4 m). The
	 * damage one (5940): a 1 m sphere (white-blue: ours a 0.5 m core), a 1 m nose shell, a 1 m trail (299E3 x 4C003 x 7A9, navy
	 * to pale blue). Each red to her enemies.
	 */
	private static void torpedoes(Minecraft mc, float now) {
		for (Iterator<Torpedo> it = TORPEDOES.iterator(); it.hasNext(); ) {
			Torpedo f = it.next();
			Entity target = mc.level.getEntity(f.target);
			float age = now - f.since;
			if (!(target instanceof LivingEntity living) || !living.isAlive() || age > JunoStats.TORPEDO_LIFE) {
				it.remove();
				continue;
			}
			Vec3 aim = target.getBoundingBox().getCenter();
			double speed = f.heal ? JunoStats.TORPEDO_SPEED
				: JunoStats.TORPEDO_SPEED * Math.min(1, JunoStats.ENEMY_LAUNCH + (1 - JunoStats.ENEMY_LAUNCH) * age / 0.3);
			Vec3 to = aim.subtract(f.at);
			double step = speed / 20;
			if (to.length() > 1.0E-3) {
				f.heading = to.normalize();
			}
			f.at = to.length() <= step ? aim : f.at.add(to.normalize().scale(step));
			draw(f);
		}
	}

	private static void draw(Torpedo f) {
		Vec3 at = f.at;
		Palette core = f.heal ? (f.enemy ? Palette.JUNO_TORP_HEAL_SHELL_ENEMY_N : Palette.JUNO_TORP_HEAL_CORE_N)
			: f.enemy ? Palette.JUNO_TORP_DMG_SHELL_ENEMY_N : Palette.JUNO_TORP_DMG_SHELL_N;
		float coreSize = f.heal ? 0.3f : 0.5f;
		Particle c = Fx.attach("juno_torp/" + f.key, at, () -> {
			Particle g = Fx.particle("juno_soft_dot", core);
			return g == null ? null : g.life(0.15f).size(coreSize, coreSize).fade(0.02f, 0.5f).nearFade(1);
		});
		if (c != null) {
			c.at(at);
		}
		Palette burst = f.heal ? (f.enemy ? Palette.JUNO_TORP_HEAL_RIBBON_ENEMY_N : Palette.JUNO_TORP_HEAL_RIBBON_N)
			: f.enemy ? Palette.JUNO_TORP_DMG_RIBBON_ENEMY_N : Palette.JUNO_TORP_DMG_RIBBON_N;
		Particle halo = Fx.attach("juno_torp_halo/" + f.key, at, () -> {
			Particle g = Fx.particle("lw_burst", burst);
			return g == null ? null : g.life(0.15f).size(0.66f, 0.66f).alpha(0.5f).spin(range(0, 6.28f), 3).fade(0.02f, 0.5f).nearFade(1);
		});
		if (halo != null) {
			halo.at(at);
		}
		float[] shell = Fx.mesh("juno_half_shell");
		if (shell != null && f.heading != null) {
			// the half-shell bulges along its Z (side x up): up square to the heading, side = up x heading
			Vec3 up = across(f.heading);
			float size = f.heal ? 0.7f : 1;
			Particle nose = Fx.particle("juno_noise_soft", core);
			if (nose != null) {
				Fx.add(nose.at(at.subtract(up.scale(size / 2))).mesh(shell, up.cross(f.heading), up).life(0.06f).size(size, size).alpha(0.5f)
					.fade(0.01f, 0.3f).nearFade(1));
			}
		}
		Fx.trail(f.key, at, f.heal ? "juno_heal_trail" : "juno_dmg_trail", burst, f.heal ? 0.38f : 1, 0.3f, 0.85f);
		if (f.heal && f.heading != null) {
			for (int i = 0; i < 3; i++) {
				Particle spark = Fx.particle("spark#4", burst);
				if (spark != null) {
					Fx.add(spark.at(at.add(randomUnit().scale(0.1))).shape(Shape.STREAK).velocity(f.heading.scale(-range(2, 2.2f))
						.add(randomUnit().scale(0.4))).life(0.32f).size(range(0.24f, 0.41f) * 0.5f, 0.05f).aspect(0.3f).fade(0.02f, 0.5f)
						.nearFade(0.5f));
				}
			}
		}
	}

	private static @Nullable Torpedo landed(int target) {
		for (Iterator<Torpedo> it = TORPEDOES.iterator(); it.hasNext(); ) {
			Torpedo f = it.next();
			if (f.target == target) {
				it.remove();
				return f;
			}
		}
		return null;
	}

	/**
	 * A torpedo's hit. On an ally (5B0C, all at the heal's start): two flares DA5B 1.5 m for 0.4 s (white to yellow), 18-24
	 * sparks 1-1.2 m/s, a big soft flash (the data's 8 m: ours 2.5 m, at block scale 8 m covered everything), a 0.35 m
	 * half-shell; no visual for the 2.5 s heal over time (H: only a held sound). On an enemy (5B0D): two star flares 2 m, a ring
	 * 724 2.25 m for 0.2 s (white to blue; red to her enemies).
	 */
	private static void torpedoHit(Vec3 at, int target, boolean heal, boolean enemy) {
		landed(target);
		if (heal) {
			for (int i = 0; i < 2; i++) {
				Particle flare = Fx.particle("cat_flare", enemy ? Palette.JUNO_TORP_HEAL_SHELL_ENEMY_N : Palette.JUNO_TORP_HEAL_HIT);
				if (flare != null) {
					Fx.add(flare.at(at).life(0.4f).size(1.5f, 1).spin(range(0, 6.28f), 0).alpha(0.7f).fade(0.02f, 0.6f).nearFade(0.8f));
				}
			}
			Particle soft = Fx.particle("juno_soft_dot", enemy ? Palette.JUNO_TORP_HEAL_SHELL_ENEMY_N : Palette.JUNO_TORP_HEAL_HIT);
			if (soft != null) {
				Fx.add(soft.at(at).life(range(0.22f, 0.27f)).size(1.5f, 2.5f).easeOut().alpha(0.4f).fade(0.02f, 0.6f).nearFade(1));
			}
			int n = 18 + Fx.RANDOM.nextInt(7);
			for (int i = 0; i < n; i++) {
				Particle spark = Fx.particle("spark#4", enemy ? Palette.JUNO_TORP_HEAL_SHELL_ENEMY_N : Palette.JUNO_TORP_HEAL_HIT_SHELL_N);
				if (spark != null) {
					Fx.add(spark.at(at).velocity(randomUnit().scale(range(1, 1.2f))).life(range(0.3f, 0.6f)).size(range(0.35f, 0.84f) * 0.3f, 0.05f)
						.fade(0.02f, 0.5f).nearFade(0.5f));
				}
			}
			return;
		}
		Palette hit = enemy ? Palette.JUNO_TORP_DMG_HIT_ENEMY_N : Palette.JUNO_TORP_DMG_HIT_N;
		for (int i = 0; i < 2; i++) {
			Particle flare = Fx.particle("cat_flare", hit);
			if (flare != null) {
				Fx.add(flare.at(at).life(0.3f).size(2, 1.2f).spin(range(0, 6.28f), 0).alpha(0.7f).fade(0.02f, 0.6f).nearFade(0.8f));
			}
		}
		Particle ring = Fx.particle("juno_ring_thin", hit);
		if (ring != null) {
			Fx.add(ring.at(at).life(0.2f).size(0.8f, 2.25f).easeOut().fade(0.02f, 0.6f).nearFade(0.8f));
		}
	}

	/**
	 * A damage torpedo stopped by a block (the server's juno_torpedo_wall): the data's wall hit 5F45 (from her healing
	 * torpedo's graph, 4795 #459: ours for this one) along the normal: a star 29F7 0.3-0.8 m growing x1.25, 10 sparks
	 * 0.05-0.09 m, a swirl, 0.3 s, gold-white.
	 */
	private static void wall(Vec3 at, Vec3 normal, int target, boolean enemy) {
		landed(target);
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal;
		Palette p = enemy ? Palette.JUNO_TORP_DMG_HIT_ENEMY_N : Palette.JUNO_TORP_WALL_N;
		Particle star = Fx.particle("lucio_spark", p);
		if (star != null) {
			float s = range(0.3f, 0.8f);
			Fx.add(star.at(at.add(n.scale(0.05))).lift(n).life(0.3f).size(s, s * 1.25f).spin(range(0, 6.28f), 0).fade(0.02f, 0.5f).nearFade(0.5f));
		}
		Particle swirl = Fx.particle("lw_swirl", p);
		if (swirl != null) {
			Fx.add(swirl.at(at.add(n.scale(0.05))).normal(n).life(0.3f).size(0.4f, 0.8f).spin(range(0, 6.28f), 4).alpha(0.6f).fade(0.02f, 0.5f));
		}
		for (int i = 0; i < 10; i++) {
			Particle spark = Fx.particle("spark#4", p);
			if (spark != null) {
				Vec3 v = randomUnit().add(n.scale(1.2)).normalize().scale(range(2, 5));
				Fx.add(spark.at(at).shape(Shape.STREAK).velocity(v).gravity(4).life(range(0.2f, 0.4f)).size(range(0.05f, 0.09f) * 2, 0.04f)
					.aspect(0.4f).fade(0.02f, 0.5f).nearFade(0.4f));
			}
		}
	}

	/**
	 * Her lock markers (the data's is UX element 0727.116 on the target's hp 0001, a HUD widget, not decoded: ours a ring
	 * round the target's middle, cyan on an ally, red on an enemy, turning), on her own client only, from the lock until its
	 * torpedo leaves.
	 */
	private static void locks(Minecraft mc, float now) {
		held(mc, LOCKS, now, (target, h, n) -> {
			boolean ally = mc.player != null && dev.overcraft.hero.Allies.wasAlly(mc.player, target);
			Vec3 centre = target.getBoundingBox().getCenter();
			float size = Math.max(0.9f, target.getBbHeight() * 0.9f);
			Particle ring = Fx.attach("juno_lock/" + target.getId(), centre, () -> {
				Particle r = Fx.particle("juno_ring_thin", ally ? Palette.JUNO_TORP_LOCK_GLINT_N : Palette.ENEMY);
				return r == null ? null : r.life(0.2f).spin(0, 1.5f).alpha(0.9f).fade(0.05f, 0.5f).nearFade(0.5f);
			});
			if (ring != null) {
				// a snap in at the lock: from 1.6x to its size over 0.15 s
				float in = Math.min(1, (n - h.since) / 0.15f);
				ring.size(size * (1.6f - 0.6f * in), size * (1.6f - 0.6f * in));
			}
		});
	}

	// ---- Hyper Ring ----

	/**
	 * The throw (5AE7 at her hand, hp 0002: L, the server's point): soft flashes 0.2 / 1 m for 0.05-0.1 s (mint), rings 1 m
	 * (cyan) and 3 m (violet-peach) for 0.15 s; then the ring.
	 */
	private static void throwRing(@Nullable Entity juno, Vec3 at, Vec3 dir, boolean enemy) {
		Palette p = enemy ? Palette.JUNO_RING_GLOW_ENEMY_N : Palette.JUNO_RING_LAUNCH_N;
		for (float s : new float[] {0.2f, 1}) {
			Particle flash = Fx.particle("juno_lens_flare", p);
			if (flash != null) {
				Fx.add(flash.at(at).life(range(0.05f, 0.1f)).size(s, s * 0.7f).alpha(0.7f).fade(0.01f, 0.5f).nearFade(1));
			}
		}
		for (float s : new float[] {1, 3}) {
			Particle ring = Fx.particle("lw_ring", p);
			if (ring != null) {
				Fx.add(ring.at(at).normal(dir).life(0.15f).size(s * 0.5f, s).easeOut().alpha(0.6f).fade(0.01f, 0.5f).nearFade(1));
			}
		}
		Level level = Minecraft.getInstance().level;
		Vec3 d = dir.lengthSqr() < 1.0E-6 ? UP : dir.normalize();
		// its flight's end: 17.5 m along, or 0.3 m short of a wall (HyperRing.tickAll)
		Vec3 stop = at.add(d.scale(JunoStats.RING_SPEED * JunoStats.RING_FLIGHT));
		if (level != null) {
			var hit = level.clip(new ClipContext(at, stop, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
				juno == null ? net.minecraft.world.phys.shapes.CollisionContext.empty() : net.minecraft.world.phys.shapes.CollisionContext.of(juno)));
			if (hit.getType() != HitResult.Type.MISS) {
				stop = hit.getLocation().subtract(d.scale(0.3));
			}
		}
		RINGS.add(new Ring(keys++ & 0xFFFF, at, d, stop, Fx.now(), enemy));
		// at its spawn a 16 m disc flash (5E5E size 16, 190EC x 299DF, 0.3 s)
		discFlash(at.add(d.scale(1)), d, 16, enemy);
	}

	/**
	 * A disc flash (the flat disc 5E5E, its 190EC band, 0.3 s; JUNO_RING_DISC) facing along n, size metres across, growing in.
	 * The band is a stripe in the mask's V, the disc's radius (on a flat quad it drew a bar: found playing).
	 */
	private static void discFlash(Vec3 centre, Vec3 n, float size, boolean enemy) {
		float[] mesh = Fx.mesh("juno_disc");
		Particle disc = mesh == null ? null : Fx.particle("juno_disc_band", enemy ? Palette.JUNO_RING_DISC_ENEMY_N : Palette.JUNO_RING_DISC_N);
		if (disc == null) {
			return;
		}
		// the disc stands in its X-Y plane, facing its Z (side x up): up square to n, side = up x n, back half its height
		Vec3 up = across(n);
		Fx.add(disc.at(centre.subtract(up.scale(size / 2))).mesh(mesh, up.cross(n), up).life(0.3f).size(size, size).alpha(0.35f)
			.fade(0.02f, 0.7f).nearFade(4));
	}

	/** The ring's mesh with its Y (its thickness) at a fifth of its width: the data's scale (25, 5, 25). */
	private static float @Nullable [] ringMesh() {
		float[] mesh = Fx.mesh("juno_ring");
		if (mesh == null) {
			return null;
		}
		if (mesh != squashedFrom) {
			squashedFrom = mesh;
			squashedRing = mesh.clone();
			for (int i = 1; i < squashedRing.length; i += 5) {
				squashedRing[i] *= RING_SQUASH;
			}
		}
		return squashedRing;
	}

	/**
	 * The rings, in flight and hanging (541C held for the ring's 6 s): the ring 15AD6 at 9.25 m facing her aim, masked by its
	 * look's wavy band 3FD49 through LUT row 1 (JUNO_RING: navy to blue, HDR 1.78; red to her enemies), popping in over 0.08 s;
	 * a torus glow 15A26 of 8 m round it (from 0.17 s), sparkles 3C068 rising 3-5 m/s off it (20-50 a second, 0.21-0.26 m,
	 * stretched x3). In flight (its 0.5 s), the three orbs (5B22: 1 m spheres with 0.7 m trails; ours: riding its rim). Then
	 * its end (5D44).
	 */
	private static void rings(Minecraft mc, float now) {
		for (Iterator<Ring> it = RINGS.iterator(); it.hasNext(); ) {
			Ring r = it.next();
			float age = now - r.since;
			float end = (float) JunoStats.RING_LIFE;
			if (age > end + END_DONE) {
				it.remove();
				continue;
			}
			Vec3 centre = r.centre(now);
			Vec3 u = across(r.dir);
			Vec3 v = r.dir.cross(u).normalize();
			if (age >= end) {
				ringEnd(r, centre, u, v, age - end);
				continue;
			}
			float[] mesh = ringMesh();
			if (mesh != null) {
				float size = RING_SIZE * Math.min(1, age / RING_POP);
				// the mesh stands on its base along up (her aim): back half its thickness to centre it
				float thick = size * 0.153f * RING_SQUASH;
				Vec3 base = centre.subtract(r.dir.scale(thick / 2));
				Particle ring = Fx.attach("juno_ring/" + r.key, base, () -> {
					Particle p = Fx.particle("juno_ring_band", r.enemy ? Palette.JUNO_RING_ENEMY_N : Palette.JUNO_RING_N);
					return p == null ? null : p.mesh(mesh, u, r.dir).life(0.3f).alpha(0.9f).fade(0.05f, 0.5f).nearFade(1.5f);
				});
				if (ring != null) {
					ring.mesh(mesh, u, r.dir).size(size, size);
				}
			}
			float[] torus = Fx.mesh("cat_torus");
			if (torus != null && age > 0.17f) {
				Particle glow = Fx.attach("juno_ring_glow/" + r.key, centre, () -> {
					Particle p = Fx.particle("cat_band", r.enemy ? Palette.JUNO_RING_GLOW_ENEMY_N : Palette.JUNO_RING_GLOW_N);
					return p == null ? null : p.life(0.4f).alpha(0.3f).fade(0.3f, 0.5f).nearFade(1.5f);
				});
				if (glow != null) {
					// 8 m across (15A26 at 2.3); its tube is 0.245 of its width: back half that to centre it
					glow.at(centre.subtract(r.dir.scale(0.1225 * 8))).mesh(torus, u, r.dir).size(8, 8);
				}
			}
			// sparkles off the ring, 20-50 a second (ours: 2 a tick)
			for (int i = 0; i < 2; i++) {
				Particle spark = Fx.particle("juno_sparkle#4", r.enemy ? Palette.JUNO_RING_SPARK_ENEMY_N : Palette.JUNO_RING_SPARK_N);
				if (spark != null) {
					double a = range(0, Mth.TWO_PI);
					Vec3 rim = centre.add(u.scale(Math.cos(a) * RING_SIZE / 2)).add(v.scale(Math.sin(a) * RING_SIZE / 2));
					Fx.add(spark.at(rim).shape(Shape.STREAK).velocity(new Vec3(0, range(3, 5), 0)).life(range(0.3f, 0.7f))
						.size(range(0.21f, 0.26f) * 3, 0.2f).aspect(0.33f).fade(0.05f, 0.5f).nearFade(1));
				}
			}
			if (age < JunoStats.RING_FLIGHT) {
				for (int i = 0; i < 3; i++) {
					double a = i * Mth.TWO_PI / 3 + age * Mth.TWO_PI * 2;
					Vec3 at = centre.add(u.scale(Math.cos(a) * RING_SIZE / 2)).add(v.scale(Math.sin(a) * RING_SIZE / 2));
					int n = i;
					Particle orb = Fx.attach("juno_ring_orb/" + r.key + "/" + i, at, () -> {
						Particle p = Fx.particle("juno_soft_dot", r.enemy ? Palette.JUNO_RING_ORB_RIBBON_ENEMY_N : Palette.JUNO_RING_ORB_RIBBON_N);
						return p == null ? null : p.life(0.15f).size(1, 1).alpha(0.7f).fade(0.02f, 0.5f).nearFade(1.5f);
					});
					if (orb != null) {
						orb.at(at);
					}
					Fx.trail(-(200_000 + r.key * 3 + n), at, "juno_heal_trail",
						r.enemy ? Palette.JUNO_RING_ORB_RIBBON_ENEMY_N : Palette.JUNO_RING_ORB_RIBBON_N, 0.7f, 0.2f, 0.8f);
				}
			}
		}
	}

	/**
	 * The ring's end (5D44, 1.67 s): streaks 49944 pulled inward 2.4-3 m/s for 0.3 s (300 a second: ours 6 a tick, 0.26-0.58
	 * m stretched x3-5.5), at 0.37 s two crossed 18 m disc flashes (0.3 s), at 0.6-0.8 s a 3.2 m sphere flash (0.15 s) and
	 * 25 sparks 0.3-0.6 m.
	 */
	private static void ringEnd(Ring r, Vec3 centre, Vec3 u, Vec3 v, float t) {
		Palette p = r.enemy ? Palette.JUNO_RING_END_ENEMY_N : Palette.JUNO_RING_END_N;
		if (t < END_STREAKS) {
			for (int i = 0; i < 6; i++) {
				Particle streak = Fx.particle("juno_streak_v", p);
				if (streak != null) {
					double a = range(0, Mth.TWO_PI);
					Vec3 out = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
					Fx.add(streak.at(centre.add(out.scale(RING_SIZE / 2))).shape(Shape.STREAK).velocity(out.scale(-range(2.4f, 3)))
						.life(0.3f).size(range(0.26f, 0.58f) * 4, 0.3f).aspect(0.22f).fade(0.05f, 0.5f).nearFade(1));
				}
			}
		}
		float tick = 0.05f;
		if (t >= END_DISC && t < END_DISC + tick) {
			for (Vec3 n : new Vec3[] {r.dir, u}) {
				discFlash(centre, n, 18, r.enemy);
			}
		}
		if (t >= END_POP && t < END_POP + tick) {
			Particle pop = Fx.particle("juno_soft_dot", p);
			if (pop != null) {
				Fx.add(pop.at(centre).life(0.15f).size(3.2f, 2).alpha(0.8f).fade(0.01f, 0.5f).nearFade(1.5f));
			}
			for (int i = 0; i < 25; i++) {
				Particle spark = Fx.particle("spark#4", p);
				if (spark != null) {
					Fx.add(spark.at(centre).shape(Shape.STREAK).velocity(randomUnit().scale(range(3, 8))).life(range(0.2f, 0.5f))
						.size(range(0.3f, 0.6f), 0.1f).aspect(0.25f).fade(0.02f, 0.5f).nearFade(0.8f));
				}
			}
		}
	}

	/**
	 * An ally through the ring (56DE, held for the boost's 3 s): speed lines E99F (3 frames) 300 a second streaming past them,
	 * 4 m/s, life 0.27-0.37 s, yellow to green (HDR 25). The data's 2-5 cm lines are lost at Minecraft's distances (ours: 0.3-0.6
	 * m long, 4-6 a tick). In the boosted player's own view, lines past the edges of the view (the data's size 10 set, L).
	 */
	private static void boosted(Minecraft mc, float now) {
		held(mc, BOOSTED, now, (e, h, n) -> {
			Vec3 moving = e.position().subtract(e.xo, e.yo, e.zo);
			Vec3 heading = moving.lengthSqr() > 1.0E-4 ? moving.normalize() : facing(e);
			if (ownFirstPerson(e)) {
				Vec3 look = e.getViewVector(1);
				Vec3 side = across(look);
				Vec3 up = look.cross(side).normalize();
				for (int i = 0; i < 2; i++) {
					Particle line = Fx.particle("cat_speed_line#3", Palette.JUNO_RING_BOOST_N);
					if (line != null) {
						double a = range(0, Mth.TWO_PI);
						Vec3 off = side.scale(Math.cos(a) * range(1.2f, 1.8f)).add(up.scale(Math.sin(a) * range(0.8f, 1.2f)));
						Fx.add(line.at(e.getEyePosition().add(look.scale(range(2, 4))).add(off)).shape(Shape.STREAK).velocity(look.scale(-range(8, 10)))
							.life(range(0.22f, 0.32f)).loop(10).size(range(0.5f, 0.9f), 0.5f).aspect(0.05f).alpha(0.5f).fade(0.1f, 0.5f).nearFade(0.6f));
					}
				}
				return;
			}
			int count = 4 + Fx.RANDOM.nextInt(3);
			for (int i = 0; i < count; i++) {
				Particle line = Fx.particle("cat_speed_line#3", Palette.JUNO_RING_BOOST_N);
				if (line != null) {
					Vec3 round = randomUnit().multiply(1, 0, 1).normalize().scale(e.getBbWidth() * range(0.6f, 1));
					Vec3 at = e.position().add(round.x, range(0.1f, e.getBbHeight()), round.z).add(heading.scale(0.6));
					Fx.add(line.at(at).shape(Shape.STREAK).velocity(heading.scale(-4).add(moving.scale(20))).life(range(0.27f, 0.37f)).loop(10)
						.size(range(0.3f, 0.6f), 0.3f).aspect(0.08f).alpha(0.8f).fade(0.1f, 0.5f).nearFade(0.6f));
				}
			}
		});
	}

	// ---- Orbital Ray ----

	/**
	 * The ult's cast on her hand (5CCA, the 0.8 s cast; bursts 0-0.47 s): a small cone flash 0.17-0.26 m long (blue to cyan,
	 * orange to her enemies), flares 3-20 cm, 50 tiny sparks a second. At her right wrist (hp 007B-0080 aren't in her model's
	 * table: M); not in her own view.
	 */
	private static void cast(Player juno) {
		HeroAnimator a = HeroAnimator.of(juno);
		float since = a.since("ult", juno.level().getGameTime() / 20f);
		Vec3 hand = point(juno, HAND);
		if (since > (float) JunoStats.RAY_CAST || hand == null) {
			return;
		}
		boolean enemy = KirikoFx.enemyOf(juno);
		Palette p = enemy ? Palette.JUNO_RAY_CAST_ENEMY_N : Palette.JUNO_RAY_CAST_N;
		Particle glow = Fx.attach("juno_cast/" + juno.getId(), hand, () -> {
			Particle g = Fx.particle("cat_flare", p);
			return g == null ? null : g.life(0.2f).size(0.26f, 0.26f).spin(0, 4).alpha(0.9f).fade(0.05f, 0.5f).nearFade(0.3f);
		});
		if (glow != null) {
			glow.at(hand);
		}
		if (since < 0.47f) {
			Particle flare = Fx.particle("juno_lens_flare", p);
			if (flare != null) {
				Fx.add(flare.at(hand).life(0.1f).size(range(0.03f, 0.2f), 0.05f).fade(0.01f, 0.5f).nearFade(0.3f));
			}
		}
		if (since > 0.17f && since < 0.6f) {
			for (int i = 0; i < 3; i++) {
				Particle spark = Fx.particle("spark#4", p);
				if (spark != null) {
					Fx.add(spark.at(hand).velocity(randomUnit().scale(range(0.5f, 1.5f))).life(range(0.15f, 0.3f)).size(0.05f, 0.02f)
						.fade(0.02f, 0.5f).nearFade(0.3f));
				}
			}
		}
	}

	/**
	 * The ray landing (54F9's start, at its ground point hp 01DA): four fat tori 15A26 dropping in at +196, +150, +100, +50 m
	 * at 0, 0.07, 0.16, 0.26 s, life 1 s (754 x 1C7A, the cat_band strip; the data's size 8 is 27.7 m across, the visible band
	 * narrower: ours 8 m across), and the shockwave decal 139C growing over 1.2 s (ours its recipe's 0 -> 7 m).
	 */
	private static void land(Vec3 at, Vec3 along, int ticks, boolean enemy) {
		RAYS.add(new Ray(keys++ & 0xFFFF, at, along.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : along.normalize(), Math.max(ticks, 1), enemy));
		float[] torus = Fx.mesh("cat_torus");
		float[][] drops = {{196, 0}, {150, 0.07f}, {100, 0.16f}, {50, 0.26f}};
		for (float[] d : drops) {
			Particle ring = torus == null ? null : Fx.particle("cat_band", enemy ? Palette.JUNO_RAY_RINGS_ENEMY_N : Palette.JUNO_RAY_RINGS_N);
			if (ring != null) {
				ring.age = -d[1];
				Fx.add(ring.at(at.add(0, d[0] - 0.1225 * 8, 0)).mesh(torus, new Vec3(1, 0, 0)).life(1).size(8, 8).alpha(0.8f).fade(0.05f, 0.6f)
					.nearFade(2));
			}
		}
		Particle shock = Fx.particle("mercy_soft", enemy ? Palette.JUNO_RAY_SHOCK_ENEMY_N : Palette.JUNO_RAY_SHOCK_N);
		if (shock != null) {
			Fx.add(shock.at(at.add(0, 0.08, 0)).normal(UP).life(1.2f).size(0.5f, 7).easeOut().alpha(0.7f).fade(0.02f, 0.5f).nearFade(0.5f));
		}
	}

	/**
	 * The rays walking along the ground as the server's do (OrbitalRay.tickAll: 2.25 m/s, up to speed over 0.5 s, down over
	 * its last 1.25 s, kept on the ground), each held 54F9 for the 8 s: the ground circle decal C32 (17 m across, the circle
	 * edge 4DBCC: gold to pale blue-white; red-peach to her enemies) and a faint disc (16E7A); the column from the sky (tube
	 * 15A21 at size 6: 5.4 m across, 18E48 x 31951, pale blue to yellow to white; ours: the 31951 band in falling pieces, its
	 * scroll), the falling light (49944, 40-45 m/s, 4-7 m long, ours 3 a tick), rings of 5.2 m (15A26 size 1.5) falling 28 m/s
	 * 4 a second, a 3 m ground glow (801). The flat circle lies at the ray's own ground point (ours: on a slope it floats).
	 */
	private static void rays(Minecraft mc) {
		for (Iterator<Ray> it = RAYS.iterator(); it.hasNext(); ) {
			Ray r = it.next();
			double t = r.step / 20.0;
			if (r.step >= r.life) {
				it.remove();
				continue;
			}
			double speed = JunoStats.RAY_SPEED * Math.min(1, Math.min(t / JunoStats.RAY_RAMP_UP, (JunoStats.RAY_TIME - t) / JunoStats.RAY_RAMP_DOWN));
			r.at = ground(mc.level, r.at.add(r.along.scale(speed / 20)), null);
			r.step++;
			Vec3 at = r.at;
			// the circle mask holds one quarter of it, its centre in the corner at (1, 1): four quarters turned round the centre
			for (int q = 0; q < 4; q++) {
				float turn = q * Mth.HALF_PI;
				Particle circle = Fx.attach("juno_ray_circle/" + r.key + "/" + q, at, () -> {
					Particle p = Fx.particle("juno_ray_circle", r.enemy ? Palette.JUNO_RAY_DECAL_ENEMY : Palette.JUNO_RAY_DECAL_N);
					return p == null ? null : p.normal(UP).spin(turn, 0).life(0.4f).size(RAY_CIRCLE / 2, RAY_CIRCLE / 2).alpha(0.9f)
						.fade(0.2f, 0.5f).nearFade(0.3f);
				});
				if (circle != null) {
					circle.at(at.add(quarter(turn, RAY_CIRCLE / 4)).add(0, 0.06, 0));
				}
			}
			Particle disc = Fx.attach("juno_ray_disc/" + r.key, at, () -> {
				Particle p = Fx.particle("juno_ray_disc", r.enemy ? Palette.JUNO_RAY_GROUND_ENEMY_N : Palette.JUNO_RAY_GROUND_N);
				return p == null ? null : p.normal(UP).life(0.4f).size(16, 16).alpha(0.15f).fade(0.2f, 0.5f).nearFade(0.3f);
			});
			if (disc != null) {
				disc.at(at.add(0, 0.05, 0));
			}
			Particle glow = Fx.attach("juno_ray_glow/" + r.key, at, () -> {
				Particle p = Fx.particle("glow", r.enemy ? Palette.JUNO_RAY_GROUND_ENEMY_N : Palette.JUNO_RAY_GROUND_N);
				return p == null ? null : p.life(0.4f).size(3, 3).alpha(0.7f).fade(0.2f, 0.5f).nearFade(1);
			});
			if (glow != null) {
				glow.at(at.add(0, 0.5, 0));
			}
			Palette column = r.enemy ? Palette.JUNO_RAY_COLUMN_ENEMY_N : Palette.JUNO_RAY_COLUMN_N;
			if (r.step % 4 == 1) {
				// the column in 30 m pieces every 0.2 s, falling 40 m/s from the top into the ground, overlapping: its band
				// scrolling down (ours: the data scrolls 31951 on the tube 15A21; that mask wraps round a tube and drawn on the
				// tube against the sky it was a flat-ended white bar, on a quad it had hard sides: found playing). A soft line
				// mask (E99F, its lit middle 0.4 of its width), teal outside (the edge tube's row) and pale blue-white inside
				for (int layer = 0; layer < 2; layer++) {
					Particle piece = Fx.particle("cat_speed_line_1", layer == 0 ? r.enemy ? column : Palette.JUNO_RAY_COLUMN_EDGE_N : column);
					if (piece != null) {
						r.pieces.add(piece);
						r.piecesAlpha.add(layer == 0 ? 0.35f : 0.5f);
						float wide = (layer == 0 ? COLUMN_WIDTH : COLUMN_WIDTH * 0.45f) / 0.4f;
						Fx.add(piece.at(at.add(0, COLUMN_HEIGHT, 0)).shape(Shape.STREAK).velocity(new Vec3(0, -COLUMN_FALL, 0))
							.life(COLUMN_HEIGHT / COLUMN_FALL).size(COLUMN_PIECE, COLUMN_PIECE).aspect(wide / COLUMN_PIECE)
							.alpha(layer == 0 ? 0.35f : 0.5f).fade(0.05f, 0.05f).nearFade(3));
					}
				}
			}
			// a camera inside the column saw its camera-facing pieces fill the view white (found playing): fade them out from
			// 4 m off its axis to 1 m (ours)
			Vec3 cam = mc.gameRenderer.mainCamera().position();
			float fromAxis = (float) Math.hypot(cam.x - at.x, cam.z - at.z);
			float keep = Mth.clamp((fromAxis - 1) / 3, 0, 1);
			for (int i = r.pieces.size() - 1; i >= 0; i--) {
				Particle piece = r.pieces.get(i);
				if (piece.removed) {
					r.pieces.remove(i);
					r.piecesAlpha.remove(i);
				} else {
					piece.alpha(r.piecesAlpha.get(i) * keep);
				}
			}
			for (int i = 0; i < 3; i++) {
				Particle rain = Fx.particle("juno_streak_v", r.enemy ? Palette.JUNO_RAY_RAIN_ENEMY_N : Palette.JUNO_RAY_RAIN_N);
				if (rain != null) {
					Vec3 off = randomUnit().multiply(1, 0, 1).normalize().scale(range(0, 2.7f));
					float fall = range(40, 45);
					Fx.add(rain.at(at.add(off.x, RAIN_FROM, off.z)).shape(Shape.STREAK).velocity(new Vec3(0, -fall, 0)).life(RAIN_FROM / fall)
						.size(range(4, 7), 5).aspect(0.06f).fade(0.05f, 0.1f).nearFade(2));
				}
			}
			float[] torus = Fx.mesh("cat_torus");
			if (torus != null && r.step % 5 == 0) {
				Particle ring = Fx.particle("cat_band", r.enemy ? Palette.JUNO_RAY_RINGS_ENEMY_N : Palette.JUNO_RAY_RINGS_N);
				if (ring != null) {
					float size = 1.5f * TORUS;
					Fx.add(ring.at(at.add(0, RINGS_FROM - 0.1225 * size, 0)).mesh(torus, new Vec3(1, 0, 0)).velocity(new Vec3(0, -RINGS_SPEED, 0))
						.life(RINGS_FROM / RINGS_SPEED).size(size, size).alpha(0.7f).fade(0.02f, 0.05f).nearFade(2));
				}
			}
		}
	}

	/**
	 * An ally in the ray (3169, held while inside): material effects on their body (19167, blue HDR 5 and gold rows: ours a
	 * soft blue glow over them, as Brigitte's Rally glow) and rising chevrons DFDC 0.75-1 m, 8 a second, stretched x3-3.5,
	 * life 0.75-1.25 s. The chevrons are the ally's own view (ECIM(40), M): in first person a few rise before them; their
	 * screen rim (DAE1) isn't drawn (ours: none).
	 */
	private static void rayed(Minecraft mc, float now) {
		held(mc, RAYED, now, (e, h, n) -> {
			boolean own = ownFirstPerson(e);
			if (!own) {
				Particle glow = Fx.attach("juno_rayed/" + e.getId(), chest(e), () -> {
					Particle p = Fx.particle("mercy_glow", h.enemy ? Palette.JUNO_RAY_GROUND_ENEMY_N : Palette.JUNO_RAY_ALLY_N);
					return p == null ? null : p.life(0.3f).alpha(0.3f).fade(0.1f, 0.5f).nearFade(0.8f);
				});
				if (glow != null) {
					glow.size(e.getBbHeight() * 1.3f, e.getBbHeight() * 1.3f).at(chest(e));
				}
			}
			if (Fx.RANDOM.nextFloat() < 8f / 20) {
				// the chevrons' shape is their mesh DFDC's (its masks 27D0D / 27D0A are a frame and a ramp: drawn on a quad they made
				// squares, found playing): ours, soft lines rising
				Particle chevron = Fx.particle("cat_speed_line#3", h.enemy ? Palette.JUNO_RAY_GROUND_ENEMY_N : Palette.JUNO_RAY_ALLY_RISE);
				if (chevron != null) {
					Vec3 from;
					if (own) {
						Vec3 ahead = facing(e);
						Vec3 side = new Vec3(-ahead.z, 0, ahead.x);
						from = e.position().add(ahead.scale(range(1.5f, 2.5f))).add(side.scale(range(-1.2f, 1.2f))).add(0, range(0, 0.6f), 0);
					} else {
						Vec3 round = randomUnit().multiply(1, 0, 1).normalize().scale(e.getBbWidth() * range(0.3f, 0.8f));
						from = e.position().add(round.x, range(0, e.getBbHeight() * 0.5f), round.z);
					}
					float s = range(0.75f, 1);
					Fx.add(chevron.at(from).shape(Shape.STREAK).velocity(new Vec3(0, 1, 0)).life(range(0.75f, 1.25f)).loop(6).size(s * 0.5f, s * 0.5f)
						.aspect(0.1f).alpha(own ? 0.4f : 0.7f).fade(0.2f, 0.5f).nearFade(0.8f));
				}
			}
		});
	}

	// ---- Martian Overboots and Glide Boost ----

	/**
	 * Her boots (from her posed feet: the jets' hp 002C / 002E aren't in her 3P model's table). Always (5985): a 0.37 m glow
	 * 2F4F0 at each, flickering 12 a second (navy to teal to white), and a 0.15 m ribbon behind (life 0.2 s, pale blue to navy).
	 * The double jump (5F7B, 0.27 s, and the fall loop 5FA9 while she rises after it -> 5C36): two 1 m flame cones E21 x 2D278
	 * down from each foot (blue to white) and a 0.15 m glow. The hover (5D10): 0.78 m flames down (violet), sparks 1F35 20 a
	 * second 1.1-3.1 m/s, a 17 cm torus sinking 0.29 m/s under her 5 a second. The glide (5A40): flames 1.0 and 1.4 m
	 * streaming back, a 0.3 m glow, sparks 50 a second 2.7-4.7 m/s, a 17 cm torus rising 1 m/s 5 a second; no glide trail (H).
	 */
	private static void boots(Player juno) {
		Vec3 left = point(juno, FOOT), right = point(juno, FOOT + 1);
		if (left == null || right == null) {
			return;
		}
		HeroAnimator a = HeroAnimator.of(juno);
		float t = juno.level().getGameTime() / 20f;
		boolean enemy = KirikoFx.enemyOf(juno);
		Vec3 moving = juno.position().subtract(juno.xo, juno.yo, juno.zo);
		float sinceJump = t - a.junoDoubleJumpAt();
		boolean jumping = !juno.onGround() && (sinceJump < 0.27f || sinceJump < 1 && moving.y > 0);
		boolean hovering = a.junoHovering();
		boolean gliding = a.junoGliding();
		Vec3 back = moving.lengthSqr() > 1.0E-4 ? moving.normalize().scale(-1) : facing(juno).scale(-1);
		Vec3 down = new Vec3(0, -1, 0);
		Vec3[] feet = {left, right};
		for (int side = 0; side < 2; side++) {
			Vec3 foot = feet[side].add(0, -0.08, 0);
			Particle glow = Fx.attach("juno_boot/" + juno.getId() + "/" + side, foot, () -> {
				Particle g = Fx.particle("juno_jet_glow", Palette.JUNO_BOOT_JET_N);
				return g == null ? null : g.life(0.2f).twinkle(12).alpha(0.6f).fade(0.05f, 0.5f).nearFade(0.5f);
			});
			if (glow != null) {
				float size = gliding ? 0.3f + 0.37f : 0.37f;
				glow.size(size, size);
			}
			Fx.trail(-(300_000 + juno.getId() * 2 + side), foot, "lw_wisp", Palette.JUNO_BOOT_RIBBON_N, 0.15f, 0.2f, 0.6f);
			if (jumping || hovering) {
				// held on the foot as she rises (spawned and left behind, the flames trailed a metre under her: found playing)
				float length = jumping ? 1 : 0.78f;
				Palette p = jumping ? Palette.JUNO_BOOT_FLAME_N : Palette.JUNO_BOOT_FLAME_VIOLET_N;
				Vec3 mid = foot.add(down.scale(length * 0.4));
				Particle jet = Fx.attach("juno_jet/" + juno.getId() + "/" + side + (jumping ? "/jump" : "/hover"), mid, () -> {
					Particle f = Fx.particle("lw_beam_cone", p);
					return f == null ? null : f.axis(down).life(0.2f).size(length, length).aspect(0.3f).alpha(0.5f).fade(0.05f, 0.5f).nearFade(0.5f);
				});
				if (jet != null) {
					jet.at(mid);
				}
				if (hovering) {
					spark(foot, down, 1.1f, 3.1f);
				}
			}
			if (gliding) {
				flame(foot, back, side == 0 ? 1 : 1.4f, Palette.JUNO_BOOT_FLAME_N);
				flame(foot, back, side == 0 ? 1.4f : 1, Palette.JUNO_BOOT_FLAME_VIOLET_N);
				for (int i = 0; i < 2; i++) {
					spark(foot, back, 2.7f, 4.7f);
				}
			}
		}
		float[] torus = Fx.mesh("cat_torus");
		if (torus != null && (hovering || gliding) && juno.tickCount % 4 == 0) {
			Particle ring = Fx.particle("cat_band", hovering ? enemy ? Palette.JUNO_HOVER_RING_ENEMY_N : Palette.JUNO_HOVER_RING_N
				: enemy ? Palette.JUNO_GLIDE_RING_ENEMY_N : Palette.JUNO_GLIDE_RING_N);
			if (ring != null) {
				Vec3 under = left.add(right).scale(0.5).add(0, -0.1, 0);
				Fx.add(ring.at(under).mesh(torus, new Vec3(1, 0, 0)).velocity(new Vec3(0, hovering ? -0.29 : 1, 0)).life(0.56f).size(0.17f, 0.4f)
					.alpha(0.8f).fade(0.05f, 0.6f).nearFade(0.5f));
			}
		}
	}

	/** One flame cone (E21 x 2D278: lw_beam_cone, its bright base at the foot) along way, length long, 1 m/s, 0.2-0.25 s. */
	private static void flame(Vec3 foot, Vec3 way, float length, Palette palette) {
		Particle flame = Fx.particle("lw_beam_cone", palette);
		if (flame != null) {
			Fx.add(flame.at(foot.add(way.scale(length * 0.4))).axis(way).velocity(way.scale(1)).life(range(0.2f, 0.25f)).size(length, length)
				.aspect(0.3f).alpha(0.4f).fade(0.05f, 0.5f).nearFade(0.5f));
		}
	}

	/** A boot spark (1F35, JUNO_BOOT_SPARK cyan): 0.05-0.22 m, life about 0.1 s. */
	private static void spark(Vec3 foot, Vec3 way, float slow, float fast) {
		Particle spark = Fx.particle("spark#4", Palette.JUNO_BOOT_SPARK);
		if (spark != null) {
			Fx.add(spark.at(foot).shape(Shape.STREAK).velocity(way.add(randomUnit().scale(0.4)).normalize().scale(range(slow, fast)))
				.life(range(0.08f, 0.12f)).size(range(0.05f, 0.22f), 0.04f).aspect(0.35f).fade(0.02f, 0.5f).nearFade(0.4f));
		}
	}

	// ---- her moments (EffectPayload) ----

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Minecraft mc = Minecraft.getInstance();
		float now = Fx.now();
		Entity entity = mc.level == null ? null : mc.level.getEntity(e.entity());
		switch (e.effect()) {
			case "juno_round" -> round(entity, at, dir, e.variant(), enemy);
			case "juno_round_block" -> blockHit(at, dir, enemy);
			case "juno_locked" -> {
				if (mc.player != null && e.variant() == mc.player.getId()) {
					LOCKS.put(e.entity(), new Held(now, now + (float) JunoStats.LOCK_WINDOW + 1, enemy));
				}
			}
			case "juno_torpedo" -> launch(podLaunch(mc, at), e.entity(), e.variant() == 1, enemy);
			case "juno_torpedo_hit" -> torpedoHit(at, e.entity(), e.variant() == 1, enemy);
			case "juno_torpedo_wall" -> wall(at, dir, e.entity(), enemy);
			case "juno_ring" -> throwRing(entity, at, dir, enemy);
			case "juno_ring_boost" -> BOOSTED.put(e.entity(), new Held(now, now + e.variant() / 20f, enemy));
			case "juno_ray" -> land(at, dir, e.variant(), enemy);
			case "juno_ray_ally" -> RAYED.put(e.entity(), new Held(now, now + e.variant() / 20f, enemy));
			default -> {
				return false;
			}
		}
		return true;
	}

	// ---- what her sounds follow (dev.overcraft.client.JunoSounds) ----

	/**
	 * Each thing of hers in flight now, by key ("torpedo_heal/<key>" an ally torpedo, "torpedo/<key>" an enemy-bound one,
	 * "ring/<key>" a ring before its 6 s are up, "ray/<key>" a ray in its 8 s), and where it is.
	 */
	public static Map<String, Vec3> sounding() {
		Map<String, Vec3> out = new HashMap<>();
		float now = Fx.now();
		for (Torpedo f : TORPEDOES) {
			out.put((f.heal ? "torpedo_heal/" : "torpedo/") + f.key, f.at);
		}
		for (Ring r : RINGS) {
			if (now - r.since < JunoStats.RING_LIFE) {
				out.put("ring/" + r.key, r.centre(now));
			}
		}
		for (Ray r : RAYS) {
			out.put("ray/" + r.key, r.at);
		}
		return out;
	}

	/** Where one of them is now (null: gone). */
	public static @Nullable Vec3 sounding(String key) {
		return sounding().get(key);
	}

	/** An entity inside one of her rays (3169 held on it: the server's juno_ray_ally, twice a second). */
	public static boolean inRay(Entity e) {
		Held h = RAYED.get(e.getId());
		return h != null && Fx.now() < h.until;
	}

	public static void clear() {
		POSED.clear();
		TORPEDOES.clear();
		RINGS.clear();
		RAYS.clear();
		LOCKS.clear();
		BOOSTED.clear();
		RAYED.clear();
		RELOAD_DONE.clear();
	}
}
