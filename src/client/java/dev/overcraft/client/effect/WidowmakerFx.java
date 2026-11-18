package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.net.EffectPayload;
import dev.overcraft.widowmaker.Widowmaker;
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
 * Widowmaker's effects (#138; tools/reference/widowmaker-effects.md, read from her effects' data, 2026-11-17). The server's messages:
 * widow_shot (her muzzle to where the shot stopped; the variant hip / scoped under 50 % / to 99 % / full: Widowmaker.SHOT_*), widow_hit
 * (a creature, a block, a scoped hit), widow_mine_burst, widow_poison (on the target, 5 s), widow_dart (Seeker Mine: from the mine to
 * the target). The mine's look and the hook's rope come from their renderers (ProjectileModelRenderer: mine / hook); her outlines through
 * walls are WidowReveal's. Her renderers are twins as Ashe's: her side white / purple / blue, her enemies red (H rows, M roles). Her
 * hip fire is a thin orange line (0066) with a gold muzzle (18F7) and Cassidy's Deadeye hit (010F, the same effect); a scoped shot is
 * an orange core and a white smoke band living 3 s that widens with the charge (0118 / 0067 / 0117), a bigger muzzle (18FB / 1FD6 /
 * 18F9). Sizes in metres, times in seconds; each says where it came from: H read in the data, M inferred, ours where the data gives
 * nothing or we drew it smaller.
 */
public final class WidowmakerFx {
	/** Its art, made ahead as the hero is picked (HeroFxArt). */
	public static final List<EffectArt.Art> ART = List.of(
		new EffectArt.Art("sombra_tracer", Palette.WIDOW_LINE, false),
		new EffectArt.Art("sombra_tracer", Palette.WIDOW_CORE, false),
		new EffectArt.Art("ana_nano_line", Palette.WIDOW_GLOW, false),
		new EffectArt.Art("widow_smoke_band", Palette.WIDOW_SMOKE, true),
		new EffectArt.Art("widow_flash#4", Palette.WIDOW_MUZZLE, false),
		new EffectArt.Art("glow", Palette.WIDOW_MUZZLE, false),
		new EffectArt.Art("widow_muzzle_glow", Palette.WIDOW_MUZZLE_GLOW, false),
		new EffectArt.Art("widow_scoped_flash#4", Palette.NATURAL, false),
		new EffectArt.Art("cat_flare", Palette.WIDOW_FLARE, false),
		new EffectArt.Art("sombra_impact_spark#4", Palette.WIDOW_HIT_SPARK, false),
		new EffectArt.Art("sombra_impact_star#2", Palette.WIDOW_HIT_STAR, false),
		new EffectArt.Art("sombra_impact_burst", Palette.WIDOW_HIT_DECAL, false),
		new EffectArt.Art("widow_rope", Palette.WIDOW_ROPE, true),
		new EffectArt.Art("widow_cast_puff", Palette.WIDOW_CAST, false),
		new EffectArt.Art("spark#4", Palette.WIDOW_CAST_SPARK, false),
		new EffectArt.Art("mercy_glow", Palette.WIDOW_MINE_FLASH_BLUE, false),
		new EffectArt.Art("cassidy_ribbon", Palette.WIDOW_MINE_TRAIL, false),
		new EffectArt.Art("pharah_flame_noise", Palette.WIDOW_BURST_SHELL, false),
		new EffectArt.Art("cat_noise", Palette.WIDOW_BURST_HEMI, false),
		new EffectArt.Art("cat_ring_line", Palette.WIDOW_BURST_SPHERE, false),
		new EffectArt.Art("torb_flame", Palette.WIDOW_BURST_FLAME, false),
		new EffectArt.Art("dva_sd_smoke#4", Palette.WIDOW_POISON_CLOUD, true),
		new EffectArt.Art("cat_halo_ring", Palette.WIDOW_POISON_RING, false),
		new EffectArt.Art("cat_noise", Palette.WIDOW_POISON_DOME, false),
		new EffectArt.Art("widow_poison_bits#4", Palette.WIDOW_POISON_BITS, false),
		new EffectArt.Art("lw_bokeh", Palette.WIDOW_DART_TRAIL, false));
	public static final List<EffectArt.Art> ART_ENEMY = List.of(
		new EffectArt.Art("sombra_tracer", Palette.WIDOW_LINE_ENEMY, false),
		new EffectArt.Art("widow_smoke_band", Palette.WIDOW_SMOKE_ENEMY, true),
		new EffectArt.Art("widow_flash#4", Palette.WIDOW_MUZZLE_ENEMY, false),
		new EffectArt.Art("sombra_impact_spark#4", Palette.WIDOW_HIT_SPARK_ENEMY, false),
		new EffectArt.Art("sombra_impact_star#2", Palette.WIDOW_HIT_STAR_ENEMY, false),
		new EffectArt.Art("mercy_glow", Palette.WIDOW_MINE_FLASH_RED, false),
		new EffectArt.Art("pharah_flame_noise", Palette.WIDOW_BURST_SHELL_ENEMY, false),
		new EffectArt.Art("cat_noise", Palette.WIDOW_BURST_HEMI_ENEMY, false),
		new EffectArt.Art("cat_ring_line", Palette.WIDOW_BURST_SPHERE_ENEMY, false),
		new EffectArt.Art("torb_flame", Palette.WIDOW_BURST_FLAME_ENEMY, false),
		new EffectArt.Art("cat_noise", Palette.WIDOW_POISON_DOME_ENEMY, false),
		new EffectArt.Art("lw_bokeh", Palette.WIDOW_DART_TRAIL_ENEMY, false));

	/** Her posed points (HeroModelRenderer): the Kiss's muzzle (hp 0006), her hook launcher (hp 0055) (widowmaker-map.md). */
	public static final String[] POINTS = {"hardpoint_0006", "hardpoint_0055"};
	private static final int MUZZLE = 0, LAUNCHER = 1;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** The hip line (0066 C7 / C9): masks 510 x 50C, width field 59DC556C 0.035, life 0.15 (H; width M, as Ashe's / Cassidy's). */
	private static final float HIP_WIDTH = 0.035f, HIP_LIFE = 0.15f;
	/** The scoped core (0118 C14, 0067 C21, 0117 C27): 510 x 50C, 0.158 m, 1 s (H; drawn at half width, ours: it reads as a bar). */
	private static final float CORE_WIDTH = 0.158f, CORE_LIFE = 1;
	/** The scoped smoke bands 4D5 x B35 x 965, 3 s (H): 0.733 m under 50 % (0118 C16 / C18), 0.933 to 99 % (0067), 1.375 / 2.13 full (0117). */
	private static final float SMOKE_LIFE = 3;
	/** Ours: the bands drawn at half the data's width (B35's cross-section fills about half its quad, L) and capped at 80 m along. */
	private static final float SMOKE_SCALE = 0.5f, SMOKE_REACH = 80;
	/** The rope (0277 C3: ribbon 0.025 m on her own texture 1DD6, H). */
	private static final float ROPE_WIDTH = 0.025f;
	/** Seeker Mine's dart, 15 m/s (59BF #175, widowmaker-map.md H). */
	private static final float DART_SPEED = 15;
	/** The poison's look on its target (3FCA, 5 s, H: the poison's length). */
	private static final float POISON_TIME = 5;

	private static final class Posed {
		Vec3[] points = new Vec3[0];
		long tick;
	}

	private static final Map<Integer, Posed> POSED = new HashMap<>();

	/** An emitter's rate kept between frames (AsheFx's). */
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

	/** A poison on a creature (3FCA): on whom, since when, an enemy's look, its emitters. */
	private static final class Poison {
		final int entity;
		final float start;
		final boolean enemy;
		final Rate clouds = new Rate(), domes = new Rate(), bits = new Rate();

		Poison(int entity, float start, boolean enemy) {
			this.entity = entity;
			this.start = start;
			this.enemy = enemy;
		}
	}

	/** A Seeker Mine dart (6B75) on its way: from, to whom (and where, should it go), since when, its ribbons' ids, an enemy's look. */
	private record Dart(Vec3 from, int target, Vec3 aim, float start, int trail, boolean enemy) {
	}

	private static final List<Poison> POISONS = new ArrayList<>();
	private static final List<Dart> DARTS = new ArrayList<>();
	/** Each mine's moments drawn (bits: 1 thrown, 2 stuck, 4 armed), by entity id. */
	private static final Map<Integer, Integer> MINES = new HashMap<>();
	/** The hooks whose launch has been drawn (their ids). */
	private static final java.util.Set<Integer> HOOKS = new java.util.HashSet<>();
	/** The darts' ribbons' ids: negative, apart from the entities' (Fx.trail keys by id). */
	private static int nextTrail = -1_000_000;

	private WidowmakerFx() {
	}

	// ---- her posed points (HeroModelRenderer) ----

	public static void posed(Entity widow, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(widow.getId(), id -> new Posed());
		p.points = points;
		p.tick = widow.level().getGameTime();
	}

	private static @Nullable Vec3 point(@Nullable Entity widow, int which) {
		if (widow == null || ownFirstPerson(widow)) {
			return null;
		}
		Posed p = POSED.get(widow.getId());
		return p == null || widow.level().getGameTime() - p.tick > 1 || which >= p.points.length ? null : p.points[which];
	}

	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/** Her muzzle: in her own first person the view model's as drawn, else posed (hp 0006), else where the server shot from. */
	private static Vec3 muzzle(@Nullable Entity widow, Vec3 fallback) {
		Minecraft mc = Minecraft.getInstance();
		if (ownFirstPerson(widow)) {
			Vector3f view = dev.overcraft.client.model.HeroModelRenderer.viewMuzzle();
			return view == null ? fallback : mc.gameRenderer.mainCamera().position().add(view.x, view.y, view.z);
		}
		Vec3 posed = point(widow, MUZZLE);
		return posed != null ? posed : fallback;
	}

	/** Her launcher (hp 0055) as posed; in her own first person or unposed, ahead of and below her eyes (ours). */
	private static Vec3 launcher(Entity owner) {
		Vec3 from = point(owner, LAUNCHER);
		if (from != null) {
			return from;
		}
		Minecraft mc = Minecraft.getInstance();
		return ownFirstPerson(owner) ? mc.gameRenderer.mainCamera().position().add(owner.getViewVector(1).scale(0.4)).add(0, -0.3, 0)
			: owner.position().add(0, owner.getBbHeight() * 0.7, 0);
	}

	// ---- from the server ----

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		if (!e.effect().startsWith("widow_")) {
			return false;
		}
		if (WidowReveal.dispatch(e)) {
			return true;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Entity entity = mc.level == null || e.entity() < 0 ? null : mc.level.getEntity(e.entity());
		switch (e.effect()) {
			case "widow_shot" -> shot(entity, at, dir, e.variant(), enemy);
			case "widow_hit" -> hit(at, dir, e.variant(), enemy);
			// an enemy Widowmaker's aim (#46, ours: Overwatch draws no line): a thin red line, sent every 0.1 s while she aims, wider
			// once it holds still (variant 1)
			case "widow_enemy_aim" -> line("sombra_tracer", muzzle(entity, at), at.add(dir), Palette.WIDOW_LINE_ENEMY,
				e.variant() == 1 ? 0.08f : 0.04f, 0.12f, 0.3f);
			case "widow_mine_burst" -> burst(at, false, enemy);
			case "widow_poison" -> {
				POISONS.removeIf(p -> p.entity == e.entity());
				POISONS.add(new Poison(e.entity(), Fx.now(), enemy));
			}
			case "widow_dart" -> dart(at, dir, e.entity(), enemy);
			default -> {
				return false;
			}
		}
		return true;
	}

	// ---- Widow's Kiss (0066 + 18F7 hip; 0118 + 18FB, 0067 + 1FD6, 0117 + 18F9 scoped by charge) ----

	/** One shot: its muzzle, its line from there to where it stopped ({@code toStop} from the server's muzzle point), a scoped one's smoke. */
	private static void shot(@Nullable Entity shooter, Vec3 from, Vec3 toStop, int variant, boolean enemy) {
		Vec3 end = from.add(toStop);
		Vec3 start = muzzle(shooter, from);
		Vec3 fwd = toStop.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : toStop.normalize();
		if (variant == Widowmaker.SHOT_HIP) {
			hipMuzzle(shooter, start, fwd, enemy);
			line("sombra_tracer", start, end, enemy ? Palette.WIDOW_LINE_ENEMY : Palette.WIDOW_LINE, HIP_WIDTH, HIP_LIFE, 0.6f);
			return;
		}
		scopedMuzzle(shooter, start, fwd, variant);
		// the core 510 x 50C (35AC r4 / 818E r4, the same orange; one renderer, no twin: H)
		line("sombra_tracer", start, end, Palette.WIDOW_CORE, CORE_WIDTH * 0.5f, CORE_LIFE, 0.25f);
		if (variant != Widowmaker.SHOT_SOFT) {
			// the glow band 965 (7C8A r2): 0.1 m to 99 %, 0.5 m full, 0.3 s (0067 C23, 0117 C25; H)
			line("ana_nano_line", start, end, Palette.WIDOW_GLOW, variant == Widowmaker.SHOT_FULL ? 0.5f : 0.1f, 0.3f, 0.6f);
		}
		Palette smoke = enemy ? Palette.WIDOW_SMOKE_ENEMY : Palette.WIDOW_SMOKE;
		if (variant == Widowmaker.SHOT_FULL) {
			smoke(start, end, smoke, 1.375f * SMOKE_SCALE, 0.3f);
			smoke(start, end, smoke, 2.13f * SMOKE_SCALE, 0.15f);
		} else {
			smoke(start, end, smoke, (variant == Widowmaker.SHOT_MID ? 0.933f : 0.733f) * SMOKE_SCALE, 0.3f);
		}
	}

	/** A band along the shot (Sombra's tracer 510 x 50C, or the glow mask 965) from the muzzle to where it stopped, added. */
	private static void line(String mask, Vec3 from, Vec3 to, Palette colour, float width, float life, float fadeOut) {
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len < 0.5f) {
			return;
		}
		Particle band = Fx.particle(mask, colour);
		if (band != null) {
			Fx.add(band.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(width / len).life(life).fade(0.01f, fadeOut)
				.nearFade(0.6f));
		}
	}

	/** A scoped shot's smoke (4D5 x B35 x 965, 3 s; its alpha falls over its life, the LUTs' r0 alpha 0.27 > 0, H): blended, widening. */
	private static void smoke(Vec3 from, Vec3 to, Palette colour, float width, float alpha) {
		Vec3 piece = to.subtract(from);
		float len = (float) Math.min(SMOKE_REACH, piece.length());
		if (len < 0.5f) {
			return;
		}
		Vec3 along = piece.normalize().scale(len);
		Particle band = Fx.soft("widow_smoke_band", colour);
		if (band != null) {
			Fx.add(band.at(from.add(along.scale(0.5))).axis(along).size(len, len * 1.04f).aspect(width / len).life(SMOKE_LIFE).alpha(alpha)
				.fade(0.02f, 0.9f).nearFade(1));
		}
	}

	/**
	 * The hip muzzle 18F7 at the Kiss's hp 0006 (H): flashes 299E1 x 3269F (4) 1.3 +- 0.75 m, 0.05-0.07 s (9BF5 r5 gold; the red row 1E64
	 * r6 for enemies, M); a flash 3269F 0.4 +- 0.15 m, 0.1 s; the glow A7B6 0.3 m, 0.05 s; the glow 2007A 0.2 m, 0.03 s (DFEE r0); lights
	 * 2 m (not drawn). Ours, as Ashe's: the long flash at half its size, a third of all of it in her own first person.
	 */
	private static void hipMuzzle(@Nullable Entity shooter, Vec3 at, Vec3 fwd, boolean enemy) {
		boolean own = ownFirstPerson(shooter);
		float k = own ? 1 / 3f : 1;
		float near = own ? 0.15f : 0.4f;
		Palette flash = enemy ? Palette.WIDOW_MUZZLE_ENEMY : Palette.WIDOW_MUZZLE;
		Particle f = Fx.particle("widow_flash#4", flash);
		if (f != null) {
			float size = range(0.55f, 2.05f) * 0.5f * k;
			Fx.add(f.at(at.add(fwd.scale(size * 0.3))).axis(fwd).shape(Shape.STREAK).life(range(0.05f, 0.07f)).size(size, size).aspect(0.5f)
				.fade(0.01f, 0.5f).nearFade(near));
		}
		Particle b = Fx.particle("widow_flash#4", flash);
		if (b != null) {
			float size = range(0.25f, 0.55f) * k;
			Fx.add(b.at(at).life(0.1f).size(size, size).spin(range(0, 6.28f), 0).fade(0.01f, 0.5f).nearFade(near));
		}
		Particle g = Fx.particle("glow", Palette.WIDOW_MUZZLE);
		if (g != null) {
			Fx.add(g.at(at).life(0.05f).size(0.3f * k, 0.3f * k).fade(0.01f, 0.5f).nearFade(near));
		}
		Particle h = Fx.particle("widow_muzzle_glow", Palette.WIDOW_MUZZLE_GLOW);
		if (h != null) {
			Fx.add(h.at(at).life(0.03f).size(0.2f * k, 0.2f * k).fade(0.01f, 0.5f).nearFade(near));
		}
	}

	/**
	 * The scoped muzzle at hp 0006 (H; 18FB under 50 %, 1FD6 to 99 %, 18F9 full): the colour flash 2A02D (4) 1.0 / 1.5 / 2.0 m, 0.053-0.06 s
	 * (5FCF r1 x10: its own colours, drawn as they are); two side flashes 2A02D 0.61-0.75 / 1-1.14 / 1.25-1.39 m, 0.04-0.06 s, 0.13 m to
	 * each side; the flare DA5B 1-1.5 / 1.25-1.75 / 1.5-2 m, 0.03 s (86EF r0); lights 2 / 2.5 / 3 m (not drawn). Ours: at half size, a third
	 * of that in her own first person; one look for both sides (the data has no twin).
	 */
	private static void scopedMuzzle(@Nullable Entity shooter, Vec3 at, Vec3 fwd, int variant) {
		boolean own = ownFirstPerson(shooter);
		float k = (own ? 1 / 3f : 1) * 0.5f;
		float near = own ? 0.15f : 0.4f;
		int charge = variant == Widowmaker.SHOT_FULL ? 2 : variant == Widowmaker.SHOT_MID ? 1 : 0;
		float[] main = {1, 1.5f, 2};
		float[][] sides = {{0.61f, 0.75f}, {1, 1.14f}, {1.25f, 1.39f}};
		float[][] flares = {{1, 1.5f}, {1.25f, 1.75f}, {1.5f, 2}};
		Vec3 side = fwd.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Particle f = Fx.particle("widow_scoped_flash#4", Palette.NATURAL);
		if (f != null) {
			float size = main[charge] * k;
			Fx.add(f.at(at.add(fwd.scale(size * 0.3))).axis(fwd).shape(Shape.STREAK).life(range(0.053f, 0.06f)).size(size, size).aspect(0.5f)
				.fade(0.01f, 0.5f).nearFade(near));
		}
		for (int s = -1; s <= 1; s += 2) {
			Particle p = Fx.particle("widow_scoped_flash#4", Palette.NATURAL);
			if (p != null) {
				float size = range(sides[charge][0], sides[charge][1]) * k;
				Fx.add(p.at(at.add(side.scale(0.128 * s * k * 2))).axis(side.scale(s)).shape(Shape.STREAK).life(range(0.04f, 0.06f))
					.size(size, size).aspect(0.5f).fade(0.01f, 0.5f).nearFade(near));
			}
		}
		Particle fl = Fx.particle("cat_flare", Palette.WIDOW_FLARE);
		if (fl != null) {
			float size = range(flares[charge][0], flares[charge][1]) * k;
			// 0.03 s is under two frames at 60 Hz: ours 0.05
			Fx.add(fl.at(at).life(0.05f).size(size, size).fade(0.01f, 0.5f).nearFade(near));
		}
	}

	/**
	 * Where a shot stops (H): the hip shot's 010F (the same effect for a creature and the world, and Cassidy's Deadeye hit) and the scoped
	 * shots' 0110 / 0111 / 0112 (the same numbers whatever the charge, no decal): sparks 1BE6 (4) 2 at 2 m/s, 0.225-0.325 m, 0.05 s, 0.05 m
	 * (hip) / 0.1 m (scoped) off the surface (6AA6 r2 white / 76F6 r2 red); a star 1BEC (2) on mesh EF7 0.19-0.29 m, 0.05 s (0304 r2 / 2B70
	 * r2); ours both 0.1 s, as Cassidy's. The hip hit's decal 220A 0.1 m (f0; 0.035 m its sides), 0.667 s, turned at random (35DA r2): ours
	 * on a block face only (a block hit doesn't say hip or scoped: drawn for both, L).
	 */
	private static void hit(Vec3 at, Vec3 normal, int variant, boolean enemy) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal.normalize();
		Vec3 c = at.add(n.scale(variant == Widowmaker.HIT_SCOPED ? 0.1 : 0.05));
		for (int i = 0; i < 2; i++) {
			Particle p = Fx.particle("sombra_impact_spark#4", enemy ? Palette.WIDOW_HIT_SPARK_ENEMY : Palette.WIDOW_HIT_SPARK);
			if (p != null) {
				Fx.add(p.at(c).velocity(cone(n, 40).scale(2)).life(0.1f).size(range(0.225f, 0.325f), 0.3f).spin(range(0, 6.28f), 0)
					.loop(40).fade(0.02f, 0.5f).nearFade(0.3f));
			}
		}
		float[] shard = Fx.mesh("torb_hit_shard_mesh");
		Particle s = shard == null ? null : Fx.particle("sombra_impact_star#2", enemy ? Palette.WIDOW_HIT_STAR_ENEMY : Palette.WIDOW_HIT_STAR);
		if (s != null) {
			Fx.add(s.mesh(shard, cone(n, 80).cross(n), n).at(c).life(0.1f).size(range(0.19f, 0.29f), 0.2f).fade(0.02f, 0.5f));
		}
		if (variant == Widowmaker.HIT_BLOCK) {
			Particle decal = Fx.particle("sombra_impact_burst", Palette.WIDOW_HIT_DECAL);
			if (decal != null) {
				Fx.add(decal.at(at).normal(n).lift(n).life(0.667f).size(0.1f, 0.1f).spin(range(0, 6.28f), 0).fade(0.01f, 0.6f));
			}
		}
	}

	// ---- Grappling Hook (005A: the rope 0277 / 33C5, the launch 0291) ----

	/**
	 * Each frame of her hook's renderer: the rope from her launcher (hp 0055) to the claw, while it's out (005A #38 3P / #260 1P: beams 0277
	 * / 33C5, H). 0277 is a 0.025 m ribbon on her own grey texture 1DD6 (H), drawn blended (ours; 33C5, her own view's, is on the energy
	 * masks 19167 / 29D41 with blue / orange rows: ours the same rope in both views). On its first frame the launch 0291 at the launcher (H:
	 * hp 0029, M her launching hand). The claw has no trail and no hit effect in the data (0154: sounds; 0425 / 7AD1: empty, H).
	 */
	public static void hook(dev.overcraft.entity.GrappleHookEntity hook, Vec3 at, boolean enemy) {
		Entity owner = hook.getOwner();
		if (owner == null) {
			return;
		}
		Vec3 from = launcher(owner);
		if (HOOKS.add(hook.getId())) {
			cast(from, at.subtract(from));
		}
		// one frame's band (a life of one frame: it's drawn again each frame while the claw is out)
		Vec3 piece = at.subtract(from);
		float len = (float) piece.length();
		if (len < 0.2f) {
			return;
		}
		Particle band = Fx.soft("widow_rope", Palette.WIDOW_ROPE);
		if (band != null) {
			Fx.add(band.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(ROPE_WIDTH / len).life(0.05f).alpha(0.9f).fade(0, 0));
		}
	}

	/**
	 * The cast 0291 (the hook) / 0285 (the mine) at her hand (H, hp 0029): a puff 1DC68 x 1DC1F 0.15-0.2 m, 0.35 s at 0.25-0.3 m/s (C636 r4
	 * purple) and 6 sparks 1F35 at 5-8 m/s, 0.065 m, 0.09 s (A089 r0 gold).
	 */
	private static void cast(Vec3 at, Vec3 dir) {
		Vec3 fwd = dir.lengthSqr() < 1.0E-6 ? UP : dir.normalize();
		Particle p = Fx.particle("widow_cast_puff", Palette.WIDOW_CAST);
		if (p != null) {
			Fx.add(p.at(at.add(fwd.scale(0.1))).velocity(fwd.scale(range(0.25f, 0.3f))).life(0.35f).size(range(0.15f, 0.2f), 0.3f)
				.spin(range(0, 6.28f), 0).fade(0.02f, 0.6f).nearFade(0.3f));
		}
		for (int i = 0; i < 6; i++) {
			Particle s = Fx.particle("spark#4", Palette.WIDOW_CAST_SPARK);
			if (s != null) {
				Fx.add(s.at(at.add(fwd.scale(0.1))).shape(Shape.STREAK).velocity(cone(fwd, 40).scale(range(5, 8))).life(0.0909f)
					.size(0.13f, 0.065f).aspect(0.3f).fade(0.01f, 0.5f).nearFade(0.3f));
			}
		}
	}

	// ---- Venom Mine (0056 throw: 0285; the mine 0057: 00FE + 18F8, 00EC the burst; 0073 the poison: 3FCA) ----

	/**
	 * Each frame of her mine's renderer. Thrown: the cast 0285 where it starts (H: at her hp 0029; ours its first position) and the flash
	 * 00FE (H: the thrown entity's child 0161, and again as it arms, 0057 #118); in flight its ribbon 18F8 on AC4, 0.5 m, 0.5 s (FD44 r1
	 * violet; ours 0.3 m, one of its two). Armed: ours a small pulsing light of the flash's colour (L: the model's own light isn't in the
	 * effect data).
	 */
	public static void mine(dev.overcraft.entity.VenomMineEntity mine, Vec3 at, boolean enemy) {
		int seen = MINES.getOrDefault(mine.getId(), 0);
		int now = seen;
		if ((seen & 1) == 0) {
			now |= 1;
			cast(at, mine.getDeltaMovement());
			mineFlash(at, enemy, 1);
		}
		if (!mine.stuck()) {
			Fx.trail(mine.getId(), at, "cassidy_ribbon", Palette.WIDOW_MINE_TRAIL, 0.3f, 0.5f, 0.7f);
		} else {
			now |= 2;
		}
		if (mine.armed()) {
			if ((seen & 4) == 0) {
				now |= 4;
				mineFlash(at, enemy, 1);
			}
			Particle light = Fx.attach("widow_mine/" + mine.getId(), at.add(0, 0.05, 0), () -> {
				Particle g = Fx.particle("mercy_glow", enemy ? Palette.WIDOW_MINE_FLASH_RED : Palette.WIDOW_MINE_FLASH_BLUE);
				return g == null ? null : g.life(0.15f).fade(0.05f, 0.6f).nearFade(0.3f);
			});
			if (light != null) {
				float pulse = 0.5f + 0.5f * (float) Math.sin(Fx.now() * Math.PI * 2);
				light.size(0.35f, 0.35f).alpha(0.25f + 0.5f * pulse);
			}
		}
		if (now != seen) {
			MINES.put(mine.getId(), now);
		}
	}

	/**
	 * 00FE / 6DBF's flash (H): glows 242 3.7 m (8EE5 r2 red) and 2.07 m (B4DA r2 blue), 0.25 s; ours: the blue for her side, the red for
	 * her enemies (M, as a mine's light in Overwatch), at 0.6 alpha, scaled.
	 */
	private static void mineFlash(Vec3 at, boolean enemy, float scale) {
		Particle g = Fx.particle("mercy_glow", enemy ? Palette.WIDOW_MINE_FLASH_RED : Palette.WIDOW_MINE_FLASH_BLUE);
		if (g != null) {
			float size = (enemy ? 3.7f : 2.07f) * scale;
			Fx.add(g.at(at.add(0, 0.2, 0)).life(0.25f).size(size, size).alpha(0.6f).fade(0.02f, 0.6f).nearFade(1));
		}
	}

	/**
	 * The burst 00EC (H; purple for her side, red for her enemies): a half dome E531 5.4-5.9 m, 0.67 s (0665 r7 / A504 r7; ours on B.O.B.'s
	 * half dome 85D0, the same 0.99 x 0.5 x 0.99 shape: E531 isn't exported); a hemisphere 963D 2.4 +- 0.3, stretch 3, 0.92 +- 0.12 s (5F6E
	 * r7 / B62E r7); a sphere 85B 3 m, 0.17 s (B7E9 r4 / CD26 r4); 3 flames 1CAFA on the flame mesh E58C 1.17 +- 0.5, 0.65 s (429E r5 /
	 * 017E r5; ours as sprites: E58C isn't exported); two lights 7 m (not drawn). Seeker Mine's dart hit 6CDD is the same LUTs at 1.1-1.6
	 * m, 0.45 +- 0.3, 0.6 m and 0.22-0.5 (H): {@code dart}.
	 */
	private static void burst(Vec3 at, boolean dart, boolean enemy) {
		float[] half = Fx.mesh("ashe_charge_dome");
		Particle shell = half == null ? null : Fx.particle("pharah_flame_noise", enemy ? Palette.WIDOW_BURST_SHELL_ENEMY : Palette.WIDOW_BURST_SHELL);
		if (shell != null) {
			float size = dart ? range(1.1f, 1.6f) : range(5.4f, 5.9f);
			Fx.add(shell.mesh(half, new Vec3(1, 0, 0), UP).at(at).life(0.6682f).size(size * 0.5f, size).easeOut().alpha(0.5f)
				.fade(0.02f, 0.6f).nearFade(2));
		}
		float[] hemi = Fx.mesh("torb_hemisphere");
		Particle h = hemi == null ? null : Fx.particle("cat_noise", enemy ? Palette.WIDOW_BURST_HEMI_ENEMY : Palette.WIDOW_BURST_HEMI);
		if (h != null) {
			float size = dart ? range(0.15f, 0.75f) : range(2.1f, 2.7f);
			Fx.add(h.mesh(hemi, new Vec3(1, 0, 0), UP).at(at).life(range(0.81f, 1.04f)).size(size * 0.5f, size).easeOut().alpha(0.7f)
				.fade(0.02f, 0.6f).nearFade(2));
		}
		float[] sphere = Fx.mesh("pharah_sphere");
		Particle s = sphere == null ? null : Fx.particle("cat_ring_line", enemy ? Palette.WIDOW_BURST_SPHERE_ENEMY : Palette.WIDOW_BURST_SPHERE);
		if (s != null) {
			float size = dart ? 0.6f : 3;
			// the sphere stands on its foot: set down by half its size
			Fx.add(s.mesh(sphere, new Vec3(1, 0, 0)).at(at.subtract(0, size / 2, 0)).life(0.1727f).size(size * 0.6f, size).easeOut().alpha(0.8f)
				.fade(0.02f, 0.6f).nearFade(2));
		}
		for (int i = 0; i < 3; i++) {
			Particle f = Fx.particle("torb_flame", enemy ? Palette.WIDOW_BURST_FLAME_ENEMY : Palette.WIDOW_BURST_FLAME);
			if (f != null) {
				float size = dart ? range(0.22f, 0.5f) : range(0.67f, 1.67f);
				Fx.add(f.at(at.add(randomUnit().scale(size * 0.3)).add(0, size * 0.3, 0)).velocity(UP.scale(range(0.3f, 0.8f))).life(0.65f)
					.size(size, size * 1.2f).spin(range(0, 6.28f), 0).fade(0.02f, 0.6f).nearFade(1));
			}
		}
	}

	/**
	 * The poison 3FCA on its target for its 5 s (H; 0073 #53 / 59C0 #53, the mine's and the dart's the same): clouds 4F0 (4) 8 a second, 2
	 * s, 0.56-0.78 m/s, 0.8 m (A7B8 r3 purple; blended, ours); a ring 52E8 on the flat 310C 2 m living 3 s (6B93 r2 blue-violet; ours held
	 * the 5 s at its feet); domes DAF 10 a second, 0.76 +- 0.5 s, 0.65 +- 0.35 m, stretch 3, spinning (5185 r10 dim purple / 30D7 r9 red:
	 * ours 4 a second as round noise sprites); bits 9487 (4) 8-14 a second, 1-2 s, 0.05-0.5 m (F0AC r1 violet; ours 6). Its fading over the
	 * last half second is ours.
	 */
	private static void poison(Entity e, Poison p, float t) {
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Vec3 feet = e.getPosition(partial);
		Vec3 chest = feet.add(0, e.getBbHeight() * 0.6, 0);
		float w = Math.max(0.4f, e.getBbWidth());
		float fade = Math.clamp((POISON_TIME - t) / 0.5f, 0, 1);
		for (int i = p.clouds.take(8); i > 0; i--) {
			Particle c = Fx.soft("dva_sd_smoke#4", Palette.WIDOW_POISON_CLOUD);
			if (c == null) {
				break;
			}
			Vec3 off = new Vec3(Fx.RANDOM.nextGaussian() * w * 0.3, (Fx.RANDOM.nextFloat() - 0.5) * e.getBbHeight() * 0.7,
				Fx.RANDOM.nextGaussian() * w * 0.3);
			Fx.add(c.at(chest.add(off)).velocity(randomUnit().scale(range(0.56f, 0.78f)).multiply(1, 0.5, 1)).drag(0.5f).life(2)
				.size(0.8f * 0.6f, 0.8f).alpha(0.35f * fade).spin(range(0, 6.28f), range(-1, 1)).loop(4 / 2f).fade(0.1f, 0.6f).nearFade(0.8f));
		}
		for (int i = p.domes.take(4); i > 0; i--) {
			Particle d = Fx.particle("cat_noise", p.enemy ? Palette.WIDOW_POISON_DOME_ENEMY : Palette.WIDOW_POISON_DOME);
			if (d == null) {
				break;
			}
			float size = range(0.3f, 1);
			Fx.add(d.at(chest.add(randomUnit().scale(w * 0.4))).life(range(0.26f, 1.26f)).size(size * 0.5f, size).alpha(fade)
				.spin(range(0, 6.28f), range(1, 2)).fade(0.1f, 0.6f).nearFade(0.6f));
		}
		for (int i = p.bits.take(6); i > 0; i--) {
			Particle b = Fx.particle("widow_poison_bits#4", Palette.WIDOW_POISON_BITS);
			if (b == null) {
				break;
			}
			Fx.add(b.at(chest.add(randomUnit().scale(w * 0.5))).velocity(UP.scale(range(0.1f, 0.4f))).life(range(1, 2))
				.size(range(0.05f, 0.5f) * 0.5f, 0.1f).alpha(fade).spin(range(0, 6.28f), range(0.23f, 0.52f)).fade(0.1f, 0.6f).nearFade(0.4f));
		}
		Particle ring = Fx.attach("widow_poison_ring/" + e.getId(), feet.add(0, 0.05, 0), () -> {
			Particle g = Fx.particle("cat_halo_ring", Palette.WIDOW_POISON_RING);
			return g == null ? null : g.normal(UP).life(0.2f).fade(0.05f, 0.6f).nearFade(1);
		});
		if (ring != null) {
			ring.size(2, 2).alpha(0.5f * fade);
		}
	}

	// ---- Seeker Mine (59BF: the dart 6B75 with 6DBF / 6DC0, its hit 6CDD) ----

	/**
	 * A dart from the mine at 15 m/s, following its target (59BF #169 / #175, H): the flash 6DBF at the mine (00FE's numbers, H; ours at
	 * half size: a dart, not the mine); its ribbons 6DC0 on 187EB, 0.155 m / 0.2 s and 0.53 m / 0.59 s (F081 r2 purple to white, r3 red for
	 * enemies; ours the wider at 0.3 alpha, and not the third, 0.71 m / 0.73 s); its head a 0.25 m glow (ours: the dart's model 61CA isn't
	 * exported); 6CDD where it lands (M: 59BF #186 / #189).
	 */
	private static void dart(Vec3 from, Vec3 toTarget, int target, boolean enemy) {
		mineFlash(from, enemy, 0.5f);
		int id = nextTrail;
		nextTrail -= 2;
		DARTS.add(new Dart(from, target, from.add(toTarget), Fx.now(), id, enemy));
	}

	/** One dart this frame: where it is, its ribbons and head; true once it's arrived (its hit drawn). */
	private static boolean fly(Dart d, float now) {
		Minecraft mc = Minecraft.getInstance();
		Entity e = mc.level == null || d.target() < 0 ? null : mc.level.getEntity(d.target());
		Vec3 to = e == null ? d.aim() : e.getBoundingBox().getCenter();
		double dist = to.distanceTo(d.from());
		float t = now - d.start();
		double done = dist < 0.1 ? 1 : Math.min(1, t * DART_SPEED / dist);
		Vec3 at = d.from().add(to.subtract(d.from()).scale(done));
		Palette trail = d.enemy() ? Palette.WIDOW_DART_TRAIL_ENEMY : Palette.WIDOW_DART_TRAIL;
		Fx.trail(d.trail(), at, "lw_bokeh", trail, 0.155f, 0.2f, 0.9f);
		Fx.trail(d.trail() - 1, at, "lw_bokeh", trail, 0.53f, 0.59f, 0.3f);
		Particle head = Fx.attach("widow_dart/" + d.trail(), at, () -> {
			Particle g = Fx.particle("mercy_glow", d.enemy() ? Palette.WIDOW_MINE_FLASH_RED : Palette.WIDOW_MINE_FLASH_BLUE);
			return g == null ? null : g.life(0.1f).fade(0.02f, 0.6f).nearFade(0.3f);
		});
		if (head != null) {
			head.size(0.25f, 0.25f).alpha(0.9f);
		}
		if (done >= 1 || t > 5) {
			burst(at, true, d.enemy());
			return true;
		}
		return false;
	}

	/** Each frame (OvercraftClient): the poisons, the darts, and forgetting what's gone. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			POSED.clear();
			POISONS.clear();
			DARTS.clear();
			MINES.clear();
			HOOKS.clear();
			WidowReveal.clear();
			return;
		}
		float now = Fx.now();
		POISONS.removeIf(p -> {
			Entity e = mc.level.getEntity(p.entity);
			float t = now - p.start;
			if (e == null || !e.isAlive() || t > POISON_TIME || t < -1) {
				return true;
			}
			poison(e, p, t);
			return false;
		});
		DARTS.removeIf(d -> fly(d, now));
		if (mc.level.getGameTime() % 40 == 0) {
			POSED.keySet().removeIf(id -> mc.level.getEntity(id) == null);
			MINES.keySet().removeIf(id -> mc.level.getEntity(id) == null);
			HOOKS.removeIf(id -> mc.level.getEntity(id) == null);
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
