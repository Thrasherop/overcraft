package dev.overcraft.client.effect;

import dev.overcraft.ashe.Ashe;
import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ashe's effects (tools/reference/ashe-effects.md, read from her effects' data, 2026-11-17; #150: they were Minecraft's smoke, crit,
 * cloud, flame and explosion particles). Nearly every renderer of hers is a twin on one mask, gold for her side and redder for her
 * enemies (H rows, M roles). Her hitscan kit is Cassidy's layout in her colours: a line on Sombra's tracer masks (510), a smoke band,
 * a muzzle at the Viper's hp 0006 (Cassidy's cone mesh 80B8, Torbjorn's dome DAF), one hit 25CF for every shot of hers and B.O.B.'s.
 * The shots, hits, Coach Gun, blast, burn, knock-up and sting come from the server's messages; the stick's fuse from its renderer
 * (ProjectileModelRenderer); B.O.B.'s charge glow and his going from his (LifeweaverPropRenderer.bob). Sizes in metres, times in
 * seconds; each says where it came from: H read in the data, M inferred, ours where the data gives nothing or we drew it smaller.
 */
public final class AsheFx {
	/** Its art, made ahead as the hero is picked (HeroFxArt). */
	public static final List<EffectArt.Art> ART = List.of(
		new EffectArt.Art("sombra_tracer", Palette.ASHE_LINE, false),
		new EffectArt.Art("sombra_tracer", Palette.ASHE_LINE_SCOPED, false),
		new EffectArt.Art("sombra_tracer", Palette.ASHE_BOB_LINE, false),
		new EffectArt.Art("cassidy_smoke_noise", Palette.ASHE_SMOKE_LINE, true),
		new EffectArt.Art("cassidy_smoke_noise", Palette.ASHE_SMOKE_LINE_SCOPED, true),
		new EffectArt.Art("dva_solid", Palette.ASHE_HIT, false),
		new EffectArt.Art("cassidy_cone", Palette.ASHE_MUZZLE_CONE, false),
		new EffectArt.Art("ashe_flash#4", Palette.ASHE_MUZZLE_FLASH, false),
		new EffectArt.Art("ashe_flash#4", Palette.ASHE_MUZZLE_FLASH_B, false),
		new EffectArt.Art("spark#4", Palette.ASHE_MUZZLE_SPARK, false),
		new EffectArt.Art("brigitte_smoke#32", Palette.ASHE_MUZZLE_SMOKE, true),
		new EffectArt.Art("cat_blob#4", Palette.ASHE_HIT_DECAL, false),
		new EffectArt.Art("ashe_coach_glow", Palette.ASHE_COACH_FLASH, false),
		new EffectArt.Art("ashe_flash#4", Palette.ASHE_COACH_FLASH_BIG, false),
		new EffectArt.Art("ashe_coach_spike#4", Palette.ASHE_COACH_SPARK, false),
		new EffectArt.Art("spark#4", Palette.ASHE_COACH_PELLET, false),
		new EffectArt.Art("torb_flame", Palette.ASHE_COACH_FLAME, false),
		new EffectArt.Art("brigitte_smoke#32", Palette.ASHE_COACH_SMOKE, true),
		new EffectArt.Art("mercy_glow", Palette.ASHE_DYN_FUSE, false),
		new EffectArt.Art("spark#4", Palette.ASHE_DYN_FUSE, false),
		new EffectArt.Art("ana_beam_line", Palette.ASHE_DYN_TRAIL, false),
		new EffectArt.Art("brigitte_smoke#32", Palette.ASHE_DYN_SMOKE, true),
		new EffectArt.Art("cat_soft", Palette.ASHE_DYN_LIT_SWIRL, true),
		new EffectArt.Art("cat_cloud", Palette.ASHE_BLAST_SHELL, false),
		new EffectArt.Art("dva_solid", Palette.ASHE_BLAST_SPHERE, false),
		new EffectArt.Art("mercy_noise", Palette.ASHE_BLAST_BALL, false),
		new EffectArt.Art("mercy_noise", Palette.ASHE_BLAST_FIRE, false),
		new EffectArt.Art("spark#4", Palette.ASHE_BLAST_SPARK, false),
		new EffectArt.Art("torb_flame", Palette.ASHE_BURN_FLAME, false),
		new EffectArt.Art("torb_burn_ring", Palette.ASHE_BURN_RING, false),
		new EffectArt.Art("spark#4", Palette.ASHE_BURN_EMBER, false),
		new EffectArt.Art("spark#4", Palette.ASHE_BOB_MUZZLE_SPARK, false),
		new EffectArt.Art("ashe_charge_strip", Palette.ASHE_BOB_CHARGE, false),
		new EffectArt.Art("cat_strip", Palette.ASHE_BOB_CHARGE_TRAIL, false),
		new EffectArt.Art("jr_frag_glow#4", Palette.ASHE_MUZZLE_CONE, false),
		new EffectArt.Art("cat_streak", Palette.ASHE_BOB_KNOCK_STREAK, false),
		new EffectArt.Art("cat_cloud", Palette.ASHE_BOB_KNOCK_DOME, false),
		new EffectArt.Art("spark#4", Palette.ASHE_STING_SPARK, false),
		new EffectArt.Art("ashe_smoke_puff#2", Palette.NATURAL, true));
	public static final List<EffectArt.Art> ART_ENEMY = List.of(
		new EffectArt.Art("sombra_tracer", Palette.ASHE_LINE_ENEMY, false),
		new EffectArt.Art("sombra_tracer", Palette.ASHE_BOB_LINE_ENEMY, false),
		new EffectArt.Art("cassidy_smoke_noise", Palette.ASHE_SMOKE_LINE_ENEMY, true),
		new EffectArt.Art("dva_solid", Palette.ASHE_HIT_ENEMY, false),
		new EffectArt.Art("cassidy_cone", Palette.ASHE_MUZZLE_CONE_ENEMY, false),
		new EffectArt.Art("cassidy_cone", Palette.ASHE_SCOPED_CONE_ENEMY, false),
		new EffectArt.Art("cat_blob#4", Palette.ASHE_HIT_DECAL_ENEMY, false),
		new EffectArt.Art("ashe_coach_glow", Palette.ASHE_COACH_FLASH_ENEMY, false),
		new EffectArt.Art("ashe_flash#4", Palette.ASHE_COACH_FLASH_BIG_ENEMY, false),
		new EffectArt.Art("ashe_coach_spike#4", Palette.ASHE_COACH_SPARK_ENEMY, false),
		new EffectArt.Art("mercy_glow", Palette.ASHE_DYN_FUSE_ENEMY, false),
		new EffectArt.Art("ana_beam_line", Palette.ASHE_DYN_TRAIL_ENEMY, false),
		new EffectArt.Art("cat_cloud", Palette.ASHE_BLAST_SHELL_ENEMY, false),
		new EffectArt.Art("mercy_noise", Palette.ASHE_BLAST_BALL_ENEMY, false),
		new EffectArt.Art("torb_flame", Palette.ASHE_BURN_FLAME_ENEMY, false),
		new EffectArt.Art("ashe_charge_strip", Palette.ASHE_BOB_CHARGE_ENEMY, false),
		new EffectArt.Art("cat_strip", Palette.ASHE_BOB_CHARGE_TRAIL_ENEMY, false),
		new EffectArt.Art("jr_frag_glow#4", Palette.ASHE_BOB_KNOCK_ENEMY, false),
		new EffectArt.Art("cat_streak", Palette.ASHE_BOB_KNOCK_STREAK_ENEMY, false),
		new EffectArt.Art("cat_cloud", Palette.ASHE_BOB_KNOCK_DOME_ENEMY, false));

	/** Her posed points (HeroModelRenderer): the Viper's muzzle hp 0006 (ashe_viper.glb; H id, M role). */
	public static final String[] POINTS = {"hardpoint_0006"};
	private static final int MUZZLE = 0;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Her lines (2456 / 25E4): mask 510, width field 59DC556C 0.055, life 0.15 (H; width M, as Cassidy's). */
	private static final float LINE_WIDTH = 0.055f, LINE_LIFE = 0.15f;
	/** B.O.B.'s (25CD): 0.075 +- 0.05 wide, 0.15 (H). */
	private static final float BOB_LINE_WIDTH = 0.075f;

	private static final class Posed {
		Vec3[] points = new Vec3[0];
		long tick;
	}

	private static final Map<Integer, Posed> POSED = new HashMap<>();

	/** An emitter's rate kept between frames (TorbjornFx's). */
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

	/** A burn on a creature: whose, since when, how long (s), an enemy's, its emitters. */
	private static final class Burn {
		final int entity;
		final float start, length;
		final boolean enemy;
		final Rate flames = new Rate(), sparks = new Rate(), embers = new Rate();

		Burn(int entity, float start, float length, boolean enemy) {
			this.entity = entity;
			this.start = start;
			this.length = length;
			this.enemy = enemy;
		}
	}

	private static final List<Burn> BURNS = new ArrayList<>();
	/** Each stick's and each B.O.B.'s emitters, by entity id. */
	private static final Map<Integer, Rate[]> RATES = new HashMap<>();
	/** The B.O.B.s whose going has been drawn (their ids). */
	private static final java.util.Set<Integer> GONE = new java.util.HashSet<>();

	private AsheFx() {
	}

	// ---- her posed points (HeroModelRenderer) ----

	public static void posed(Entity ashe, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(ashe.getId(), id -> new Posed());
		p.points = points;
		p.tick = ashe.level().getGameTime();
	}

	private static @Nullable Vec3 point(@Nullable Entity ashe, int which) {
		if (ashe == null || ownFirstPerson(ashe)) {
			return null;
		}
		Posed p = POSED.get(ashe.getId());
		return p == null || ashe.level().getGameTime() - p.tick > 1 || which >= p.points.length ? null : p.points[which];
	}

	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/** Her muzzle: in her own first person the view model's as drawn, else posed (hp 0006), else where the server shot from. */
	private static Vec3 muzzle(@Nullable Entity ashe, Vec3 fallback) {
		Minecraft mc = Minecraft.getInstance();
		if (ownFirstPerson(ashe)) {
			Vector3f view = dev.overcraft.client.model.HeroModelRenderer.viewMuzzle();
			return view == null ? fallback : mc.gameRenderer.mainCamera().position().add(view.x, view.y, view.z);
		}
		Vec3 posed = point(ashe, MUZZLE);
		return posed != null ? posed : fallback;
	}

	// ---- from the server ----

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		if (!e.effect().startsWith("ashe_")) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Entity entity = mc.level == null || e.entity() < 0 ? null : mc.level.getEntity(e.entity());
		switch (e.effect()) {
			case "ashe_shot" -> shot(entity, at, dir, e.variant(), enemy);
			case "ashe_hit" -> hit(at, dir, e.variant(), enemy);
			case "ashe_coach" -> coach(entity, at, dir, enemy);
			case "ashe_dynamite" -> blast(at, e.variant() == 1 ? (float) dev.overcraft.ashe.AsheStats.AIRBURST_RADIUS : 1, enemy);
			case "ashe_burn" -> {
				BURNS.removeIf(b -> b.entity == e.entity());
				BURNS.add(new Burn(e.entity(), Fx.now(), Math.max(1, e.variant()) / 20f, enemy));
			}
			case "ashe_bob_knock" -> knock(at, enemy);
			case "ashe_sting" -> sting(at);
			default -> {
				return false;
			}
		}
		return true;
	}

	// ---- the shots (2456 + 2458 hip, 25E4 + 25E5 scoped, 25CD + 25CE B.O.B.'s) ----

	/** One shot: its muzzle and its line from there to where it stopped ({@code toStop} from the server's muzzle point), its smoke. */
	private static void shot(@Nullable Entity shooter, Vec3 from, Vec3 toStop, int variant, boolean enemy) {
		Vec3 end = from.add(toStop);
		boolean bob = variant == Ashe.SHOT_BOB;
		Vec3 start = bob ? from : muzzle(shooter, from);
		Vec3 fwd = toStop.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : toStop.normalize();
		if (bob) {
			bobMuzzle(start, fwd);
			line(start, end, enemy ? Palette.ASHE_BOB_LINE_ENEMY : Palette.ASHE_BOB_LINE, BOB_LINE_WIDTH, LINE_LIFE);
			// 25CD's smoke bands 0.4 +- 0.2 m, 0.5 +- 0.25 s (81EC r0; H): one 0.25 m band (ours: his eight a second would be a wall)
			if (Fx.RANDOM.nextInt(3) == 0) {
				smoke(start, end, Palette.ASHE_SMOKE_LINE_SCOPED, 0.25f, 0.5f, 0.3f);
			}
			return;
		}
		boolean scoped = variant == Ashe.SHOT_SCOPED;
		muzzleFlash(shooter, start, fwd, scoped, enemy);
		line(start, end, enemy ? Palette.ASHE_LINE_ENEMY : scoped ? Palette.ASHE_LINE_SCOPED : Palette.ASHE_LINE, LINE_WIDTH, LINE_LIFE);
		// the smoke bands 16E9D x B35 x 18BB5, 0.4 +- 0.2 m, 0.25 +- 0.25 s hip / 0.75 +- 0.25 s scoped (H): one band 0.3 m (ours)
		smoke(start, end, enemy ? Palette.ASHE_SMOKE_LINE_ENEMY : scoped ? Palette.ASHE_SMOKE_LINE_SCOPED : Palette.ASHE_SMOKE_LINE, 0.3f,
			scoped ? 0.75f : 0.3f, 0.35f);
	}

	/** The line: a thin band (Sombra's tracer, mask 510) from the muzzle to where the shot stopped. */
	private static void line(Vec3 from, Vec3 to, Palette colour, float width, float life) {
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len < 0.5f) {
			return;
		}
		Particle band = Fx.particle("sombra_tracer", colour);
		if (band != null) {
			Fx.add(band.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(width / len).life(life).fade(0.01f, 0.6f)
				.nearFade(0.6f));
		}
	}

	/** The smoke along a shot: one soft band, widening a little as it fades (CassidyFx's, ours past 40 m). */
	private static void smoke(Vec3 from, Vec3 to, Palette colour, float width, float life, float alpha) {
		Vec3 piece = to.subtract(from);
		float len = (float) Math.min(40, piece.length());
		if (len < 0.5f) {
			return;
		}
		Vec3 along = piece.normalize().scale(len);
		Particle band = Fx.soft("cassidy_smoke_noise", colour);
		if (band != null) {
			Fx.add(band.at(from.add(along.scale(0.5))).axis(along).size(len, len * 1.02f).aspect(width / len).life(life).alpha(alpha)
				.fade(0.05f, 0.7f).nearFade(1));
		}
	}

	/**
	 * The muzzle 2458 (hip) / 25E5 (scoped) at the Viper's hp 0006 (H): the core dome DAF 0.35 +- 0.2 m, 0.075 s at +0.2 m (D0D5 r4); the
	 * cones 80B8 1.0 +- 0.25 m (scoped 1.5), 0.05-0.07 s; flashes 18BAC 1-1.25 m (scoped 1.5), 0.06-0.09 s (57C5 r0, 420C r0); sparks 1F35
	 * at 2-10 m/s, 0.01-0.03 m, 0.1-0.3 s in a 35-40 degree cone (178E r0: 6 of 30, ours); smoke 1DFCB 0.4-0.6 m (scoped 0.5-0.8), 0.3-0.6
	 * s (scoped 1-1.5) at 1-3 m/s, stretch 4.6 (8954 r1: 2 of 7, ours). In her own first person a third the size and no dome (ours, as
	 * Tracer's / Cassidy's: the data has one effect for both views, the Viper 1.05-1.18 bigger in 1P, 14FC #715).
	 */
	private static void muzzleFlash(@Nullable Entity shooter, Vec3 at, Vec3 fwd, boolean scoped, boolean enemy) {
		boolean own = ownFirstPerson(shooter);
		float k = own ? 1 / 3f : 1;
		float near = own ? 0.15f : 0.4f;
		Vec3 side = fwd.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(fwd).normalize();
		float[] dome = own ? null : Fx.mesh("torb_dome");
		Particle d = dome == null ? null : Fx.particle("dva_solid", enemy ? Palette.ASHE_MUZZLE_DOME_ENEMY : Palette.ASHE_HIT);
		if (d != null) {
			// ASHE_HIT is D0D5 r4's numbers too (the note: the same row under three roles)
			float size = range(0.15f, 0.55f) * (scoped ? 1.15f : 1);
			Fx.add(d.mesh(dome, side, up).at(at.add(fwd.scale(0.2)).subtract(up.scale(size / 2))).life(scoped ? 0.1f : 0.075f).size(size, size)
				.alpha(0.6f).fade(0.01f, 0.5f));
		}
		float[] cones = Fx.mesh("cassidy_muzzle_cone");
		Particle cone = cones == null ? null : Fx.particle("cassidy_cone", enemy
			? scoped ? Palette.ASHE_SCOPED_CONE_ENEMY : Palette.ASHE_MUZZLE_CONE_ENEMY : Palette.ASHE_MUZZLE_CONE);
		if (cone != null) {
			// (2400 r1, the scoped cone, is 3773 r1's numbers: one palette) opening along the shot, as CassidyFx's
			float size = (scoped ? range(1.25f, 1.75f) : range(0.75f, 1.25f)) * 0.5f * k;
			Fx.add(cone.mesh(cones, side.scale(-1), up).at(at.add(fwd.scale(0.04 + 0.177 * size)).subtract(up.scale(size / 2)))
				.life(range(0.05f, scoped ? 0.07f : 0.06f)).size(size, size).alpha(0.8f).fade(0.01f, 0.5f).nearFade(near));
		}
		for (int i = 0; i < 2; i++) {
			Particle f = Fx.particle("ashe_flash#4", i == 0 ? Palette.ASHE_MUZZLE_FLASH : Palette.ASHE_MUZZLE_FLASH_B);
			if (f != null) {
				float size = (scoped ? 1.5f : range(1, 1.25f)) * 0.5f * k;
				Fx.add(f.at(at.add(fwd.scale(size * 0.3))).axis(fwd).shape(Shape.STREAK).life(range(0.06f, 0.09f)).size(size, size)
					.aspect(0.5f).fade(0.01f, 0.5f).nearFade(near));
			}
		}
		for (int i = 0; i < 6; i++) {
			Particle s = Fx.particle("spark#4", Palette.ASHE_MUZZLE_SPARK);
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(cone(fwd, 38).scale(range(2, 10) * k)).life(range(0.1f, 0.3f))
					.size(range(0.03f, 0.08f) * k, 0.02f * k).aspect(0.3f).fade(0.01f, 0.5f).nearFade(near));
			}
		}
		for (int i = 0; i < 2; i++) {
			Particle s = Fx.soft("brigitte_smoke#32", Palette.ASHE_MUZZLE_SMOKE);
			if (s != null) {
				float len = (scoped ? range(0.5f, 0.8f) : range(0.4f, 0.6f)) * k;
				Vec3 dir = cone(fwd, 30);
				Fx.add(s.at(at.add(dir.scale(len / 2))).velocity(dir.scale(range(1, 3) * k)).drag(0.2f)
					.life(scoped ? range(1, 1.5f) : range(0.3f, 0.6f)).size(len, len * 1.4f).alpha(0.35f).spin(range(0, 6.28f), 0)
					.loop(32 / 0.6f).fade(0.05f, 0.6f).nearFade(near));
			}
		}
	}

	/**
	 * B.O.B.'s muzzle 25CE (H; at the shot's start: his cannons 0297 / 0298 / 0007 in turn, M; ours the server's point): cones 80B8 1.5
	 * +- 0.25 m, 0.06-0.07 s (204F r1, 3773 r1's numbers); flashes 18BAC 1.5 m (420C r0); sparks 1F35 at 2-10 m/s, 0.2-0.35 s (FC0C r0: 4
	 * of 30, ours); smoke 13F75 (colour) 0.6-0.8 m, 0.25-0.75 s (one of 6, ours).
	 */
	private static void bobMuzzle(Vec3 at, Vec3 fwd) {
		Vec3 side = fwd.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(fwd).normalize();
		float[] cones = Fx.mesh("cassidy_muzzle_cone");
		Particle cone = cones == null ? null : Fx.particle("cassidy_cone", Palette.ASHE_MUZZLE_CONE);
		if (cone != null) {
			float size = range(1.25f, 1.75f) * 0.5f;
			Fx.add(cone.mesh(cones, side.scale(-1), up).at(at.add(fwd.scale(0.04 + 0.177 * size)).subtract(up.scale(size / 2)))
				.life(range(0.06f, 0.07f)).size(size, size).alpha(0.8f).fade(0.01f, 0.5f).nearFade(0.4f));
		}
		Particle f = Fx.particle("ashe_flash#4", Palette.ASHE_MUZZLE_FLASH_B);
		if (f != null) {
			Fx.add(f.at(at.add(fwd.scale(0.2))).axis(fwd).shape(Shape.STREAK).life(range(0.06f, 0.08f)).size(0.75f, 0.75f).aspect(0.5f)
				.fade(0.01f, 0.5f).nearFade(0.4f));
		}
		for (int i = 0; i < 4; i++) {
			Particle s = Fx.particle("spark#4", Palette.ASHE_BOB_MUZZLE_SPARK);
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(cone(fwd, 35).scale(range(2, 10))).life(range(0.2f, 0.35f))
					.size(range(0.06f, 0.12f), 0.045f).aspect(0.3f).fade(0.01f, 0.5f).nearFade(0.4f));
			}
		}
		if (Fx.RANDOM.nextInt(2) == 0) {
			Particle s = Fx.soft("ashe_smoke_puff#2", Palette.NATURAL);
			if (s != null) {
				Fx.add(s.at(at.add(fwd.scale(0.3))).velocity(cone(fwd, 30).scale(range(2.9f, 3.1f))).drag(0.1f).life(range(0.25f, 0.75f))
					.size(0.6f, 0.8f).alpha(0.4f).spin(range(0, 6.28f), 0).fade(0.05f, 0.6f).nearFade(0.8f));
			}
		}
	}

	/**
	 * Where a shot stops (25CF, H: the same effect for a creature and the world, for all her shots, B.O.B.'s and the pellets): the dome
	 * DAF 0.25 +- 0.25 m, 0.1-0.15 s (F573 r4 / 1BD2 r4); the cone 80B8 0.75 +- 0.25 m, 0.075-0.125 s along the normal (colour ours: D6C3
	 * r0 is a channel-0 ramp, L); on a block the decal 18BC2 0.1 m, 0.5 s, turned at random (D025 r2 / 1A50 r2; ours: none on a creature).
	 * A pellet's at half the size (ours).
	 */
	private static void hit(Vec3 at, Vec3 normal, int variant, boolean enemy) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal.normalize();
		float k = variant == Ashe.HIT_PELLET ? 0.5f : 1;
		Vec3 side = n.cross(Math.abs(n.y) < 0.9 ? UP : new Vec3(1, 0, 0)).normalize();
		Vec3 up = side.cross(n).normalize();
		float[] dome = Fx.mesh("torb_dome");
		Particle d = dome == null ? null : Fx.particle("dva_solid", enemy ? Palette.ASHE_HIT_ENEMY : Palette.ASHE_HIT);
		if (d != null) {
			float size = range(0.1f, 0.5f) * k;
			Fx.add(d.mesh(dome, side, up).at(at.add(n.scale(0.03)).subtract(up.scale(size / 2))).life(range(0.1f, 0.15f)).size(size, size)
				.alpha(0.7f).fade(0.01f, 0.5f));
		}
		float[] cones = Fx.mesh("cassidy_muzzle_cone");
		Particle cone = cones == null ? null : Fx.particle("cassidy_cone", enemy ? Palette.ASHE_MUZZLE_CONE_ENEMY : Palette.ASHE_MUZZLE_CONE);
		if (cone != null) {
			float size = range(0.5f, 1) * 0.5f * k;
			Fx.add(cone.mesh(cones, side.scale(-1), up).at(at.add(n.scale(0.025 + 0.177 * size)).subtract(up.scale(size / 2)))
				.life(range(0.075f, 0.125f)).size(size, size).alpha(0.7f).fade(0.01f, 0.5f).nearFade(0.3f));
		}
		if (variant == Ashe.HIT_BLOCK || variant == Ashe.HIT_PELLET) {
			Particle decal = Fx.particle("cat_blob#4", enemy ? Palette.ASHE_HIT_DECAL_ENEMY : Palette.ASHE_HIT_DECAL);
			if (decal != null) {
				Fx.add(decal.at(at).normal(n).lift(n).life(0.5f).size(0.1f * k, 0.1f * k).spin(range(0, 6.28f), 0).fade(0.01f, 0.6f));
			}
		}
	}

	// ---- Coach Gun (2455) ----

	/**
	 * The blast 2455 at the Coach Gun's 0006 (H; at 0.165 s): flashes 8CF 1.7-2.2 m, 0.12-0.15 s (D574 r1 / 291E r1); the 3 m flash 18BAC,
	 * 0.13-0.145 s (A893 r1, ours at 0.3); spikes 18D9B 0.1-0.25 m at 5-10 m/s, stretch 7.5 (8335 r2: 8 of 25, ours); pellet streaks 1F35 at
	 * 2-17 m/s, 0.07-0.4 s within 25-45 degrees (99A0 r0: 15, one per pellet, ours of 150); flame tongues 1CAFA 0.1-0.2 m at 1-20 m/s,
	 * 0.075-0.24 s (7C18 r0: 8 of 39, ours); smoke 1DFCB 1.25-2.5 m, 0.5-1 s at 1.1-2.4 m/s (EAD9 r1: 3 of 34, ours). In her own first
	 * person a third the size (ours, as her muzzle). Where: the server's point a metre ahead of her eyes (M: the gun's 0006 not posed).
	 */
	private static void coach(@Nullable Entity ashe, Vec3 at, Vec3 dir, boolean enemy) {
		boolean own = ownFirstPerson(ashe);
		float k = own ? 1 / 3f : 1;
		float near = own ? 0.2f : 0.5f;
		Vec3 fwd = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
		Particle flash = Fx.particle("ashe_coach_glow", enemy ? Palette.ASHE_COACH_FLASH_ENEMY : Palette.ASHE_COACH_FLASH);
		if (flash != null) {
			float size = range(1.7f, 2.2f) * k;
			Fx.add(flash.at(at.add(fwd.scale(0.3 * k))).life(range(0.12f, 0.15f)).size(size, size * 1.1f).fade(0.01f, 0.5f).nearFade(near));
		}
		Particle big = Fx.particle("ashe_flash#4", enemy ? Palette.ASHE_COACH_FLASH_BIG_ENEMY : Palette.ASHE_COACH_FLASH_BIG);
		if (big != null) {
			Fx.add(big.at(at.add(fwd.scale(0.6 * k))).axis(fwd).shape(Shape.STREAK).life(range(0.13f, 0.145f)).size(3 * k, 3 * k).aspect(0.5f)
				.alpha(0.3f).fade(0.01f, 0.5f).nearFade(near));
		}
		for (int i = 0; i < 8; i++) {
			Particle s = Fx.particle("ashe_coach_spike#4", enemy ? Palette.ASHE_COACH_SPARK_ENEMY : Palette.ASHE_COACH_SPARK);
			if (s != null) {
				Vec3 d = cone(fwd, 45);
				Fx.add(s.at(at.add(d.scale(0.2 * k))).shape(Shape.STREAK).velocity(d.scale(range(5, 10) * k)).life(range(0.1f, 0.2f))
					.size(range(0.3f, 0.75f) * k, 0.2f * k).aspect(0.3f).fade(0.01f, 0.5f).nearFade(near));
			}
		}
		for (int i = 0; i < 15; i++) {
			Particle s = Fx.particle("spark#4", Palette.ASHE_COACH_PELLET);
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(cone(fwd, 10).scale(range(8, 17) * k)).life(range(0.07f, 0.3f))
					.size(range(0.15f, 0.4f) * k, 0.04f * k).aspect(0.25f).fade(0.01f, 0.5f).nearFade(near));
			}
		}
		for (int i = 0; i < 8; i++) {
			Particle f = Fx.particle("torb_flame", Palette.ASHE_COACH_FLAME);
			if (f != null) {
				Vec3 d = cone(fwd, 30);
				Fx.add(f.at(at.add(d.scale(0.15))).velocity(d.scale(range(1, 10) * k)).life(range(0.075f, 0.24f))
					.size(range(0.2f, 0.4f) * k, 0.1f * k).spin(range(0, 6.28f), 0).fade(0.01f, 0.5f).nearFade(near));
			}
		}
		for (int i = 0; i < 3; i++) {
			Particle s = Fx.soft("brigitte_smoke#32", Palette.ASHE_COACH_SMOKE);
			if (s != null) {
				float size = range(1.25f, 2.5f) * k * 0.7f;
				Vec3 d = cone(fwd, 25);
				Fx.add(s.at(at.add(d.scale(size * 0.4))).velocity(d.scale(range(1.1f, 2.4f) * k)).drag(0.3f).life(range(0.5f, 1))
					.size(size * 0.6f, size).alpha(0.3f).spin(range(0, 6.28f), range(-1, 1)).loop(32 / 0.8f).fade(0.05f, 0.6f).nearFade(near * 2));
			}
		}
	}

	// ---- Dynamite (25C7 in flight, 25C8 landed and lit, 25C9 the blast, 254F the burn) ----

	/**
	 * Her stick each frame (ProjectileModelRenderer), at its fuse (hp 0076, ours: its middle 0.1 m up). In flight 25C7 (H): fuse glows 242
	 * 0.15-0.85 m 50 a second, 0.05 s (59D7 r1 / 6FF7 r1: ours one 0.4 m held), two ribbons 0.75 s (7D63 r0 by age: ours one 0.1 m for
	 * 0.4 s), sparks 1F35 100 a second at 2.9-7 m/s, 0.025 m, 0.2-0.4 s, gravity 9.8 (ours 40), smoke 1DFCB 50 a second 0.5 m, 0.5-0.75 s
	 * (6A26 r3: ours 10). Landed 25C8 (H): the glow 242 0.5 m held, the swirl 19167 x 4C09 25 a second, 0.25 +- 0.25 m, 0.75-1 s, spin 3
	 * (77EC r8: ours on the round 4C09 alone, 10 a second: 19167 fills its square), sparks 100 a second (ours 40).
	 */
	public static void dynamite(dev.overcraft.entity.DynamiteEntity stick, Vec3 at, boolean enemy) {
		Rate[] r = RATES.computeIfAbsent(stick.getId(), id -> new Rate[] {new Rate(), new Rate(), new Rate()});
		Vec3 fuse = at.add(0, 0.1, 0);
		boolean landed = stick.landed();
		Particle glow = Fx.attach("ashe_fuse/" + stick.getId(), fuse, () -> {
			Particle g = Fx.particle("mercy_glow", enemy ? Palette.ASHE_DYN_FUSE_ENEMY : Palette.ASHE_DYN_FUSE);
			return g == null ? null : g.life(0.1f).fade(0.02f, 0.6f).nearFade(0.5f);
		});
		if (glow != null) {
			float size = landed ? 0.5f : 0.4f;
			glow.size(size * range(0.85f, 1.15f), size).alpha(0.9f);
		}
		for (int i = r[0].take(40); i > 0; i--) {
			Particle s = Fx.particle("spark#4", enemy ? Palette.ASHE_DYN_FUSE_ENEMY : Palette.ASHE_DYN_FUSE);
			if (s == null) {
				break;
			}
			Fx.add(s.at(fuse).shape(Shape.STREAK).velocity(cone(UP, 15).scale(range(2.9f, 7))).gravity(9.8f).life(range(0.2f, 0.4f))
				.size(0.06f, 0.025f).aspect(0.3f).fade(0.01f, 0.5f).nearFade(0.3f));
		}
		if (!landed) {
			Fx.trail(stick.getId(), fuse, "ana_beam_line", enemy ? Palette.ASHE_DYN_TRAIL_ENEMY : Palette.ASHE_DYN_TRAIL, 0.1f, 0.4f, 0.8f);
			for (int i = r[1].take(10); i > 0; i--) {
				Particle s = Fx.soft("brigitte_smoke#32", Palette.ASHE_DYN_SMOKE);
				if (s == null) {
					break;
				}
				Fx.add(s.at(fuse).velocity(randomUnit().scale(range(0.1f, 0.5f))).life(range(0.5f, 0.75f)).size(0.25f, 0.6f).alpha(0.35f)
					.spin(range(0, 6.28f), 0).loop(32 / 0.7f).fade(0.05f, 0.6f).nearFade(0.8f));
			}
		} else {
			for (int i = r[2].take(10); i > 0; i--) {
				Particle s = Fx.soft("cat_soft", Palette.ASHE_DYN_LIT_SWIRL);
				if (s == null) {
					break;
				}
				Fx.add(s.at(fuse.add(randomUnit().scale(0.08))).velocity(UP.scale(range(0.2f, 0.5f))).life(range(0.75f, 1))
					.size(range(0.1f, 0.5f), 0.5f).alpha(0.5f).spin(range(0, 6.28f), 3).fade(0.05f, 0.6f).nearFade(0.5f));
			}
		}
	}

	/**
	 * The blast 25C9 (H; meshes only: no smoke, no flipbook, no decal in the live effect): the hemisphere 963D x 18E48 13 m, 0.3 s (BC76
	 * r2: ours 0.8 of it at 0.3, inside her 5 m radius); the sphere 1385 11 m, 0.3 s (C390 r3: ours 5 m at 0.35); the fire balls 7CDA 10.5
	 * m 0.5 s (3CBA r4) and 10 / 6 m (72C0 r4) 1 / 0.75 s (ours 5 / 3.5 m growing: the full sizes filled the screen at any range); the
	 * dome DAF 5 m, 0.5 s (F21D r4, the balls' numbers); sparks 1F35 25 at 7-11 m/s, 1 s, 0.05-0.1 m, all ways (FC12 r1: ours 15).
	 * Airburst's 1.4 scales it all (35A6 #93).
	 */
	private static void blast(Vec3 at, float scale, boolean enemy) {
		float[] hemi = Fx.mesh("torb_hemisphere");
		Particle shell = hemi == null ? null : Fx.particle("cat_cloud", enemy ? Palette.ASHE_BLAST_SHELL_ENEMY : Palette.ASHE_BLAST_SHELL);
		if (shell != null) {
			float size = 10 * scale;
			Fx.add(shell.mesh(hemi, new Vec3(1, 0, 0), UP).at(at.subtract(0, 0.5, 0)).life(0.3f).size(size * 0.6f, size).easeOut().alpha(0.3f)
				.fade(0.02f, 0.6f).nearFade(2));
		}
		float[] sphere = Fx.mesh("cat_sphere");
		Particle s = sphere == null ? null : Fx.particle("dva_solid", Palette.ASHE_BLAST_SPHERE);
		if (s != null) {
			float size = 5 * scale;
			Fx.add(s.mesh(sphere, new Vec3(1, 0, 0), UP).at(at.subtract(0, size / 2, 0)).life(0.3f).size(size * 0.5f, size).easeOut()
				.alpha(0.35f).fade(0.02f, 0.7f).nearFade(2));
		}
		float[] ball = Fx.mesh("dva_missile_ball");
		float[][] balls = {{5, 0.5f}, {3.5f, 0.75f}};
		for (int i = 0; i < balls.length && ball != null; i++) {
			Particle b = Fx.particle("mercy_noise", i == 0 ? enemy ? Palette.ASHE_BLAST_BALL_ENEMY : Palette.ASHE_BLAST_BALL : Palette.ASHE_BLAST_FIRE);
			if (b != null) {
				float size = balls[i][0] * scale;
				Fx.add(b.mesh(ball, new Vec3(1, 0, 0), UP).at(at.subtract(0, size / 2, 0)).life(balls[i][1]).size(size * 0.4f, size).easeOut()
					.alpha(0.7f).fade(0.02f, 0.6f).nearFade(2));
			}
		}
		float[] dome = Fx.mesh("torb_dome");
		Particle d = dome == null ? null : Fx.particle("dva_solid", enemy ? Palette.ASHE_BLAST_BALL_ENEMY : Palette.ASHE_BLAST_BALL);
		if (d != null) {
			float size = 5 * scale;
			Fx.add(d.mesh(dome, new Vec3(1, 0, 0), UP).at(at.subtract(0, size / 2, 0)).life(0.5f).size(size * 0.5f, size).easeOut().alpha(0.4f)
				.fade(0.02f, 0.6f).nearFade(2));
		}
		for (int i = 0; i < 15; i++) {
			Particle p = Fx.particle("spark#4", Palette.ASHE_BLAST_SPARK);
			if (p == null) {
				break;
			}
			Fx.add(p.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(7, 11) * scale)).drag(0.2f).gravity(4).life(range(0.6f, 1))
				.size(range(0.15f, 0.3f), 0.08f).aspect(0.35f).fade(0.02f, 0.5f).nearFade(0.8f));
		}
	}

	/**
	 * The burn 254F on each one hit, at its chest (hp 0001; H, held over the 5 s burn, M): flames 4DB x 4A7E x 4C09 7-10 a second, 0.5-0.75
	 * m, 0.5-1.5 s (0454 r12 / D2A3 r12: ours on the round torb_flame, the noise masks fill their square); sparks 20 a second, 0.5-1 m/s,
	 * 0.5 s (FE83 r1, FC12 r1's numbers); Torbjorn's ground ring 86CF x D9B size 2 at its feet (B8BD r6); embers 1F35 20 a second, 0.5-1.5
	 * s (A8A6 r0). The material glow 27EF isn't drawable.
	 */
	private static void burn(Entity e, Burn b, float t) {
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Vec3 feet = e.getPosition(partial);
		Vec3 chest = feet.add(0, e.getBbHeight() * 0.6, 0);
		float w = e.getBbWidth();
		for (int i = b.flames.take(9); i > 0; i--) {
			Particle f = Fx.particle("torb_flame", b.enemy ? Palette.ASHE_BURN_FLAME_ENEMY : Palette.ASHE_BURN_FLAME);
			if (f == null) {
				break;
			}
			Vec3 off = new Vec3(Fx.RANDOM.nextGaussian() * w * 0.3, (Fx.RANDOM.nextFloat() - 0.5) * e.getBbHeight() * 0.6,
				Fx.RANDOM.nextGaussian() * w * 0.3);
			Fx.add(f.at(chest.add(off)).velocity(UP.scale(range(0.4f, 0.9f))).life(range(0.5f, 1)).size(range(0.5f, 0.75f) * 0.6f, 0.2f)
				.spin(range(0, 6.28f), 0).fade(0.1f, 0.6f).nearFade(0.6f));
		}
		for (int i = b.sparks.take(20); i > 0; i--) {
			Particle s = Fx.particle("spark#4", Palette.ASHE_BLAST_SPARK);
			if (s == null) {
				break;
			}
			Fx.add(s.at(chest.add(randomUnit().scale(w * 0.4))).shape(Shape.STREAK).velocity(randomUnit().scale(range(0.5f, 1))).life(0.5f)
				.size(range(0.06f, 0.12f), 0.04f).aspect(0.3f).fade(0.02f, 0.5f).nearFade(0.4f));
		}
		for (int i = b.embers.take(20); i > 0; i--) {
			Particle s = Fx.particle("spark#4", Palette.ASHE_BURN_EMBER);
			if (s == null) {
				break;
			}
			Fx.add(s.at(chest.add(randomUnit().scale(w * 0.5))).velocity(UP.scale(range(0.3f, 0.8f))).life(range(0.5f, 1.5f))
				.size(range(0.017f, 0.032f) * 2, 0.03f).fade(0.05f, 0.5f).nearFade(0.3f));
		}
		float[] ring = Fx.mesh("torb_burn_ring_mesh");
		Particle r = ring == null ? null : Fx.attach("ashe_burn_ring/" + e.getId(), feet.add(0, 0.05, 0), () -> {
			Particle g = Fx.particle("torb_burn_ring", Palette.ASHE_BURN_RING);
			return g == null ? null : g.mesh(ring, new Vec3(1, 0, 0), new Vec3(0, 0, 1)).life(0.2f).fade(0.05f, 0.6f).nearFade(1);
		});
		if (r != null) {
			float fade = Math.clamp((b.length - t) / 0.5f, 0, 1);
			r.size(2, 2).alpha(0.6f * fade);
		}
	}

	// ---- B.O.B. (25CC charge, 270F knock-up, 2B06 going) ----

	/**
	 * B.O.B. each frame (LifeweaverPropRenderer.bob). Charging, 25CC (H, at his root 0004): the half dome 85D0 at (0, 1.2, 1.75) size 3, 2
	 * a second, living 1 s (47CC r5 / 7B6E r5: ours one held, pulsing 2 a second, at 0.5) and the ribbon 7F5 3 m wide at 1.2 m up, 0.25 s
	 * (9C50 r6: ours 1.5 m). Going (his DESPAWN phase), 2B06 once: smoke 13F75 (colour) and a glow 19167 x 16E9D over his body (ours: 3
	 * puffs and a round 1.5 m glow; the dissolve's material isn't drawable).
	 */
	public static void bob(dev.overcraft.entity.BobEntity bob, Vec3 at, float yaw, boolean enemy) {
		int phase = bob.phase();
		if (phase == dev.overcraft.entity.BobEntity.CHARGE) {
			Vec3 ahead = Vec3.directionFromRotation(0, yaw);
			Vec3 side = ahead.cross(UP).normalize();
			float pulse = 0.85f + 0.15f * (float) Math.sin(Fx.now() * Math.PI * 4);
			float[] dome = Fx.mesh("ashe_charge_dome");
			Particle d = dome == null ? null : Fx.attach("ashe_charge/" + bob.getId(), at, () -> {
				Particle g = Fx.particle("ashe_charge_strip", enemy ? Palette.ASHE_BOB_CHARGE_ENEMY : Palette.ASHE_BOB_CHARGE);
				return g == null ? null : g.life(0.15f).fade(0.05f, 0.6f).nearFade(1);
			});
			if (d != null) {
				// the dome stands on its foot (y 0): turned so it bulges ahead of him, its foot 1.75 m out at 1.2 m up
				d.mesh(dome, side, ahead.scale(-1)).at(at.add(ahead.scale(1.75)).add(0, 1.2, 0)).size(3 * pulse, 3 * pulse).alpha(0.5f);
			}
			Fx.trail(bob.getId(), at.add(0, 1.2, 0), "cat_strip", enemy ? Palette.ASHE_BOB_CHARGE_TRAIL_ENEMY : Palette.ASHE_BOB_CHARGE_TRAIL,
				1.5f, 0.25f, 0.6f);
		} else if (phase == dev.overcraft.entity.BobEntity.DESPAWN && GONE.add(bob.getId())) {
			for (int i = 0; i < 3; i++) {
				Particle s = Fx.soft("ashe_smoke_puff#2", Palette.NATURAL);
				if (s != null) {
					Fx.add(s.at(at.add(0, 0.5 + i * 0.6, 0).add(randomUnit().scale(0.3))).velocity(randomUnit().scale(0.5f)).life(range(0.6f, 1))
						.size(1, 1.6f).alpha(0.5f).spin(range(0, 6.28f), 0).fade(0.05f, 0.6f).nearFade(1));
				}
			}
			Particle g = Fx.particle("mercy_glow", Palette.ASHE_BOB_CHARGE);
			if (g != null) {
				Fx.add(g.at(at.add(0, 1.2, 0)).life(0.4f).size(1.5f, 1.8f).alpha(0.6f).fade(0.02f, 0.6f).nearFade(1));
			}
		}
	}

	/**
	 * The knock-up 270F on each one B.O.B. lifts, at its chest (hp 0014; H): flashes 18BAD 2 m, 0.2 s (4C69 r1, 3773 r1's numbers / 43EC
	 * r1); spikes 18D9B 0.15 m at 1 m/s, 0.2 s (10 of them: ours 6); streaks 183A7 / 1BCFB at 5-10 m/s, 0.2-0.5 m, 0.1-0.3 s, all ways up
	 * (A321 r1: 10 of 32, ours); the dome DAF x 18E48 4 m, 0.2 s (2073 r3: ours at 0.3). The held look 3BF7 isn't drawn (no sync of the hold).
	 */
	private static void knock(Vec3 at, boolean enemy) {
		Particle f = Fx.particle("jr_frag_glow#4", enemy ? Palette.ASHE_BOB_KNOCK_ENEMY : Palette.ASHE_MUZZLE_CONE);
		if (f != null) {
			Fx.add(f.at(at).life(0.2f).size(2, 2.2f).spin(range(0, 6.28f), 0).fade(0.02f, 0.5f).nearFade(1));
		}
		for (int i = 0; i < 6; i++) {
			Particle s = Fx.particle("ashe_coach_spike#4", Palette.ASHE_COACH_SPARK);
			if (s != null) {
				Vec3 d = cone(UP, 60);
				Fx.add(s.at(at.add(d.scale(0.2))).shape(Shape.STREAK).velocity(d).life(0.2f).size(0.3f, 0.15f).aspect(0.3f).fade(0.02f, 0.5f)
					.nearFade(0.5f));
			}
		}
		for (int i = 0; i < 10; i++) {
			Particle s = Fx.particle("cat_streak", enemy ? Palette.ASHE_BOB_KNOCK_STREAK_ENEMY : Palette.ASHE_BOB_KNOCK_STREAK);
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(cone(UP, 70).scale(range(5, 10))).life(range(0.1f, 0.3f)).size(range(0.2f, 0.5f), 0.06f)
					.aspect(0.2f).fade(0.02f, 0.5f).nearFade(0.5f));
			}
		}
		float[] dome = Fx.mesh("torb_dome");
		Particle d = dome == null ? null : Fx.particle("cat_cloud", enemy ? Palette.ASHE_BOB_KNOCK_DOME_ENEMY : Palette.ASHE_BOB_KNOCK_DOME);
		if (d != null) {
			Fx.add(d.mesh(dome, new Vec3(1, 0, 0), UP).at(at.subtract(0, 2, 0)).life(0.2f).size(3, 4).easeOut().alpha(0.3f).fade(0.02f, 0.6f)
				.nearFade(2));
		}
	}

	/**
	 * Viper's Sting's burst 65A8 on the target (H): the dome DAF 2.74 m, 0.3 s (B0CB r4, the blast balls' numbers); sparks 1F35 20 at
	 * 6.2-10.2 m/s, 0.24-0.6 s, all ways (55E1 r1: ours 10); the ball 7CDA 2.73 m, 0.7 s (7506 r4, 72C0 r4's numbers).
	 */
	private static void sting(Vec3 at) {
		float[] dome = Fx.mesh("torb_dome");
		Particle d = dome == null ? null : Fx.particle("dva_solid", Palette.ASHE_BLAST_BALL);
		if (d != null) {
			Fx.add(d.mesh(dome, new Vec3(1, 0, 0), UP).at(at.subtract(0, 1.37, 0)).life(0.3f).size(1.5f, 2.74f).easeOut().alpha(0.4f)
				.fade(0.02f, 0.6f).nearFade(1));
		}
		float[] ball = Fx.mesh("dva_missile_ball");
		Particle b = ball == null ? null : Fx.particle("mercy_noise", Palette.ASHE_BLAST_FIRE);
		if (b != null) {
			Fx.add(b.mesh(ball, new Vec3(1, 0, 0), UP).at(at.subtract(0, 1.37, 0)).life(0.7f).size(1, 2.73f).easeOut().alpha(0.6f)
				.fade(0.02f, 0.6f).nearFade(1));
		}
		for (int i = 0; i < 10; i++) {
			Particle s = Fx.particle("spark#4", Palette.ASHE_STING_SPARK);
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(6.2f, 10.2f))).drag(0.1f).life(range(0.24f, 0.6f))
					.size(range(0.15f, 0.4f), 0.05f).aspect(0.25f).fade(0.02f, 0.5f).nearFade(0.5f));
			}
		}
	}

	/** Each frame (OvercraftClient): the burns, and forgetting what's gone. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			BURNS.clear();
			RATES.clear();
			GONE.clear();
			POSED.clear();
			return;
		}
		float now = Fx.now();
		BURNS.removeIf(b -> {
			Entity e = mc.level.getEntity(b.entity);
			float t = now - b.start;
			if (e == null || !e.isAlive() || t > b.length || t < -1) {
				return true;
			}
			burn(e, b, t);
			return false;
		});
		if (mc.level.getGameTime() % 40 == 0) {
			RATES.keySet().removeIf(id -> mc.level.getEntity(id) == null);
			GONE.removeIf(id -> mc.level.getEntity(id) == null);
			POSED.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		}
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

	private static Vec3 randomUnit() {
		Vec3 v = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return v.lengthSqr() < 1.0E-6 ? UP : v.normalize();
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}
}
