package dev.overcraft.client.effect;

import dev.overcraft.cassidy.Cassidy;
import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.EffectPayload;
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
 * Cassidy's effects (tools/reference/cassidy-effects.md, read from his effects' data, 2026-11-17; #150: they were Minecraft's smoke
 * and crit particles). Every shot's line is a twin on Sombra's tracer masks (510 x 50C): cyan to his side, orange-red to his enemies
 * (0FEF / C54D r4: H rows, M roles); the muzzle, its smoke and sparks are warm gold for everyone (no twin in the data). The shots,
 * hits, reload, roll, bang, hinder and bleed come from the server's messages; the Flashbang's ribbon from its renderer; Silver
 * Bullet's armed glow and Deadeye's held look each frame from his synced look (HeroManager.CASSIDY_LOOK) at his posed hardpoints.
 * Sizes in metres, times in seconds; each says where it came from: H read in the data, M inferred, ours where the data gives
 * nothing or we drew it smaller.
 */
public final class CassidyFx {
	/** Its art, made ahead as the hero is picked (HeroFxArt). */
	public static final List<EffectArt.Art> ART = List.of(
		new EffectArt.Art("sombra_tracer", Palette.CS_LINE, false),
		new EffectArt.Art("sombra_tracer", Palette.CS_DEADEYE_LINE, false),
		new EffectArt.Art("cassidy_smoke_noise", Palette.CS_SMOKE_LINE, true),
		new EffectArt.Art("cassidy_smoke_noise", Palette.CS_SILVER_LINE, true),
		new EffectArt.Art("cassidy_smoke_noise", Palette.CS_DEADEYE_TUBE, true),
		new EffectArt.Art("cat_cone_line", Palette.CS_MUZZLE_FLASH, false),
		new EffectArt.Art("cassidy_muzzle_flame#32", Palette.CS_MUZZLE_FLASH, false),
		new EffectArt.Art("cassidy_muzzle_flame#32", Palette.CS_FAN_FLASH, false),
		new EffectArt.Art("cassidy_cone", Palette.CS_MUZZLE_FLASH, false),
		new EffectArt.Art("cassidy_flash#4", Palette.CS_MUZZLE_FLASH, false),
		new EffectArt.Art("cassidy_flash#4", Palette.CS_MUZZLE_FLASH_C, false),
		new EffectArt.Art("cassidy_flash#4", Palette.CS_FAN_FLASH, false),
		new EffectArt.Art("cassidy_spark", Palette.CS_MUZZLE_SPARK, false),
		new EffectArt.Art("cassidy_cone", Palette.CS_MUZZLE_RING, false),
		new EffectArt.Art("ana_puff#4", Palette.CS_MUZZLE_SMOKE, true),
		new EffectArt.Art("cassidy_hit_flash#4", Palette.CS_HIT, false),
		new EffectArt.Art("torb_hit_shard#4", Palette.CS_HIT_SHARD, false),
		new EffectArt.Art("lucio_spark", Palette.CS_HIT_DECAL, false),
		new EffectArt.Art("ana_smoke#4", Palette.CS_RELOAD_SMOKE, true),
		new EffectArt.Art("cassidy_dust#32", Palette.CS_ROLL_DUST, true),
		new EffectArt.Art("mercy_glow", Palette.CS_ROLL_DUST, false),
		new EffectArt.Art("cassidy_ribbon", Palette.CS_FLASH_TRAIL, false),
		new EffectArt.Art("cassidy_ribbon", Palette.CS_FLASH_TRAIL_B, false),
		new EffectArt.Art("cassidy_bang", Palette.CS_FLASH, false),
		new EffectArt.Art("cat_cone_line", Palette.CS_FLASH_RAYS, false),
		new EffectArt.Art("cat_cloud", Palette.CS_FLASH_DOME, false),
		new EffectArt.Art("sigma_streak", Palette.CS_FLASH_STREAK, false),
		new EffectArt.Art("cat_soft", Palette.CS_SILVER_ARMED, false),
		new EffectArt.Art("cat_cloud", Palette.CS_SILVER_ARMED, false),
		new EffectArt.Art("cassidy_bleed_glow", Palette.CS_BLEED, false),
		new EffectArt.Art("spark#4", Palette.CS_BLEED_SPARK, false),
		new EffectArt.Art("brigitte_flare_star", Palette.CS_DEADEYE_EYE, false),
		new EffectArt.Art("cat_sparkle", Palette.CS_DEADEYE_STAR, false),
		new EffectArt.Art("mercy_soft", Palette.CS_DEADEYE_GLOW, false),
		new EffectArt.Art("cassidy_dust_cloud", Palette.CS_ROLL_DUST, true),
		new EffectArt.Art("sombra_impact_spark#4", Palette.CS_DEADEYE_DECAL, false),
		new EffectArt.Art("sombra_impact_star#2", Palette.CS_DEADEYE_DECAL, false),
		new EffectArt.Art("brigitte_stun_halo", Palette.NATURAL, false),
		new EffectArt.Art("brigitte_stun_star", Palette.NATURAL, false));
	public static final List<EffectArt.Art> ART_ENEMY = List.of(
		new EffectArt.Art("sombra_tracer", Palette.CS_LINE_ENEMY, false),
		new EffectArt.Art("cassidy_hit_flash#4", Palette.CS_HIT_ENEMY, false),
		new EffectArt.Art("torb_hit_shard#4", Palette.CS_HIT_SHARD_ENEMY, false),
		new EffectArt.Art("lucio_spark", Palette.CS_HIT_DECAL_ENEMY, false),
		new EffectArt.Art("cassidy_bang", Palette.CS_FLASH_ENEMY, false),
		new EffectArt.Art("cat_cloud", Palette.CS_FLASH_DOME_ENEMY, false),
		new EffectArt.Art("sigma_streak", Palette.CS_FLASH_STREAK_ENEMY, false),
		new EffectArt.Art("cat_soft", Palette.CS_SILVER_ARMED_ENEMY, false),
		new EffectArt.Art("cat_cloud", Palette.CS_SILVER_ARMED_ENEMY, false),
		new EffectArt.Art("mercy_soft", Palette.CS_DEADEYE_GLOW_ENEMY, false));

	/**
	 * His posed points (HeroModelRenderer): the revolver's muzzle hp 0006 (its weapon model's), then his own: his head 0012 (Deadeye's
	 * eye glint), his chest 0001 (Deadeye's glows), his right wrist 0003 (Silver Bullet's armed glow) (H positions from his models'
	 * hardpoint tables, work\models.json; M roles).
	 */
	public static final String[] POINTS = {"hardpoint_0006", "hardpoint_0012", "hardpoint_0001", "hardpoint_0003"};
	/** The first of POINTS that are on his body (the rest is on his gun). */
	public static final int GUN_POINTS = 1;
	private static final int MUZZLE = 0, HEAD = 1, CHEST = 2, WRIST = 3;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** The line's width (m): 18D7's width field 59DC556C, 0.055 (M width). */
	private static final float LINE_WIDTH = 0.055f;
	/** The line's life (s): 18D7, 0.25 (H). */
	private static final float LINE_LIFE = 0.25f;
	/** The shot variants the server sends (Cassidy.shot): Peacekeeper, Fan the Hammer, Silver Bullet, Deadeye; and "no smoke band". */
	private static final int PEACEKEEPER = 0, FAN = 1, SILVER = 2, DEADEYE = 3, NO_SMOKE = 8;

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

	/** Something on an entity for a while: whose, since when, how long (s), an enemy's, its direction, its emitters. */
	private static final class Timed {
		final int entity;
		final float start, length;
		final boolean enemy;
		final Vec3 dir;
		final Rate rate = new Rate(), rateB = new Rate();

		Timed(int entity, float start, float length, boolean enemy, Vec3 dir) {
			this.entity = entity;
			this.start = start;
			this.length = length;
			this.enemy = enemy;
			this.dir = dir;
		}
	}

	private static final List<Timed> RELOADS = new ArrayList<>();
	private static final List<Timed> ROLLS = new ArrayList<>();
	private static final List<Timed> BLEEDS = new ArrayList<>();
	/** Each Cassidy's held emitters (Silver Bullet armed, Deadeye), by entity id. */
	private static final Map<Integer, Rate[]> RATES = new HashMap<>();

	private CassidyFx() {
	}

	// ---- his posed points (HeroModelRenderer) ----

	public static void posed(Entity cassidy, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(cassidy.getId(), id -> new Posed());
		p.points = points;
		p.tick = cassidy.level().getGameTime();
	}

	/** His posed point this tick or the last (null in his own first person, unposed, or without that point). */
	private static @Nullable Vec3 point(@Nullable Entity cassidy, int which) {
		if (cassidy == null || ownFirstPerson(cassidy)) {
			return null;
		}
		Posed p = POSED.get(cassidy.getId());
		return p == null || cassidy.level().getGameTime() - p.tick > 1 || which >= p.points.length ? null : p.points[which];
	}

	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/** His muzzle: in his own first person the view model's as drawn, else posed (hp 0006), else where the server shot from. */
	private static Vec3 muzzle(@Nullable Entity cassidy, Vec3 fallback) {
		Minecraft mc = Minecraft.getInstance();
		if (ownFirstPerson(cassidy)) {
			Vector3f view = dev.overcraft.client.model.HeroModelRenderer.viewMuzzle();
			return view == null ? fallback : mc.gameRenderer.mainCamera().position().add(view.x, view.y, view.z);
		}
		Vec3 posed = point(cassidy, MUZZLE);
		return posed != null ? posed : fallback;
	}

	// ---- from the server ----

	/** His effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		if (!e.effect().startsWith("cassidy_")) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Entity entity = mc.level == null || e.entity() < 0 ? null : mc.level.getEntity(e.entity());
		switch (e.effect()) {
			case "cassidy_shot" -> shot(entity, at, dir, e.variant(), enemy);
			case "cassidy_hit" -> hit(at, dir, e.variant() == 1, enemy);
			case "cassidy_deadeye_hit" -> deadeyeHit(at, dir);
			case "cassidy_bang" -> bang(at, e.variant() == 1, enemy);
			case "cassidy_hinder" -> BrigitteFx.stun(e.entity(), Math.max(1, e.variant()) / 20f, enemy);
			case "cassidy_bleed" -> {
				BLEEDS.removeIf(b -> b.entity == e.entity());
				BLEEDS.add(new Timed(e.entity(), Fx.now(), Math.max(1, e.variant()) / 20f, enemy, Vec3.ZERO));
			}
			case "cassidy_reload" -> {
				if (e.entity() >= 0) {
					RELOADS.removeIf(r -> r.entity == e.entity());
					RELOADS.add(new Timed(e.entity(), Fx.now(), Math.max(5, e.variant()) / 20f, enemy, Vec3.ZERO));
				}
			}
			case "cassidy_roll" -> {
				if (e.entity() >= 0) {
					ROLLS.removeIf(r -> r.entity == e.entity());
					ROLLS.add(new Timed(e.entity(), Fx.now(), Math.max(1, e.variant()) / 20f, enemy, dir));
					rollStart(entity, at);
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	// ---- the shots (18D7 / 356A / 6DB8 / 060D) ----

	/**
	 * One shot: its muzzle at the revolver's hp 0006 and its line from there to where it stopped (the server's muzzle point to the
	 * stop is {@code toStop}), with the smoke along it.
	 */
	private static void shot(@Nullable Entity shooter, Vec3 from, Vec3 toStop, int variant, boolean enemy) {
		int kind = variant & 7;
		Vec3 end = from.add(toStop);
		Vec3 start = muzzle(shooter, from);
		Vec3 fwd = toStop.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : toStop.normalize();
		muzzleFlash(shooter, start, fwd, kind == FAN);
		line(start, end, enemy ? Palette.CS_LINE_ENEMY : Palette.CS_LINE, LINE_WIDTH, LINE_LIFE);
		if (kind == DEADEYE) {
			// 060D's second line 510 x 50C, 0.1 s (59BA r4, H; its width not in the data: the line's, ours) and the tube 8C0 (width 0.69,
			// 1.0 s, H) as one soft band 0.3 m (ours: the tube as a band)
			line(start, end, Palette.CS_DEADEYE_LINE, LINE_WIDTH, 0.1f);
			smoke(start, end, Palette.CS_DEADEYE_TUBE, 0.3f, 1.0f, 0.3f);
			return;
		}
		if ((variant & NO_SMOKE) != 0) {
			// Fan the Hammer's smoke only every other shot (ours: six overlapping bands in 0.6 s read as a wall)
			return;
		}
		if (kind == SILVER) {
			// 6DB8's blue-headed smoke 0.34 m for 1.6 s (734D r7, H): one band 0.35 m (ours)
			smoke(start, end, Palette.CS_SILVER_LINE, 0.35f, 1.6f, 0.35f);
		} else {
			// 18D7's smoke 4DB x B35 x 965 0.34 m, 0.87 s and 4D5 0.5 m, 1.0 s (H; widths M): one band 0.3 m for 0.9 s (ours)
			smoke(start, end, Palette.CS_SMOKE_LINE, 0.3f, 0.9f, 0.35f);
		}
	}

	/** The line 18D7: a thin band (mask 801 x 510 x 50C, Sombra's tracer) from the muzzle to where the shot stopped. */
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

	/** The smoke along a shot: one soft band on the noise 4D5, widening a little as it fades (ours). */
	private static void smoke(Vec3 from, Vec3 to, Palette colour, float width, float life, float alpha) {
		Vec3 piece = to.subtract(from);
		// the data's line runs the whole shot; past 40 m a smoke band reads as a rope across the sky (ours)
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
	 * The muzzle 057A at hp 0006 each shot (H; under 0.45 s): the flash dome DAF x 16E9D 0.3-0.9 m, 0.075 s; a flame 1FB83 (32 frames)
	 * 0.5-0.65 m stretched x4.6, 0.125-0.45 s (one of the 4: ours); the cones 80B8 x 2A2B9 1.94-2.15 m, 0.04-0.06 s; two flashes 5C04
	 * 1.1-1.2 m, 0.08-0.09 s (3352 r1) and their back pair -0.07 m behind (B2A1 r1); sparks 1BCFB at 1.9-5.3 m/s, 0.01-0.03 m, 0.02-0.18
	 * s in a 40 degree cone (4 of 10: ours); the ring E700 0.7-0.8 m, 0.03-0.04 s; smoke B08 x 1C77 x 5429 0.28-0.4 m stretched x4.6,
	 * 0.125-0.45 s at 1.5-1.7 m/s in a 27 degree cone (2 of 10: ours). Fan the Hammer's 356B: the same, its flame and flash on BD91 r1.
	 * In his own first person a sixth the size and no dome (ours, as Tracer's: a third covered the gun).
	 */
	private static void muzzleFlash(@Nullable Entity shooter, Vec3 at, Vec3 fwd, boolean fan) {
		boolean own = ownFirstPerson(shooter);
		float k = own ? 1 / 6f : 1;
		float near = own ? 0.15f : 0.4f;
		Palette flash = fan ? Palette.CS_FAN_FLASH : Palette.CS_MUZZLE_FLASH;
		Vec3 side = fwd.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(fwd).normalize();
		float[] dome = own ? null : Fx.mesh("torb_dome");
		Particle d = dome == null ? null : Fx.particle("cat_cone_line", Palette.CS_MUZZLE_FLASH);
		if (d != null) {
			// the dome (a 1 m ball, its file's bounds) 0.25 m ahead, set down by half its size to centre it
			float size = range(0.3f, 0.9f);
			Fx.add(d.mesh(dome, side, up).at(at.add(fwd.scale(0.25)).subtract(up.scale(size / 2))).life(0.075f).size(size, size)
				.alpha(0.6f).fade(0.01f, 0.5f));
		}
		Particle flame = Fx.particle("cassidy_muzzle_flame#32", flash);
		if (flame != null) {
			float len = range(0.5f, 0.65f) * 1.5f * k;
			Fx.add(flame.at(at.add(fwd.scale(len * 0.5f))).shape(Shape.STREAK).velocity(fwd.scale(range(2.9f, 3.1f) * k))
				.life(range(0.125f, 0.2f)).size(len, len).aspect(1 / 2.3f).fade(0.01f, 0.5f).nearFade(near));
		}
		float[] cones = Fx.mesh("cassidy_muzzle_cone");
		Particle cone = cones == null ? null : Fx.particle("cassidy_cone", Palette.CS_MUZZLE_FLASH);
		if (cone != null) {
			// the cone (1 m across, 0.35 m deep along its axis: the file's Y, the glb's Z, as Tracer's ring) opening along the shot:
			// its Z (side x up) along fwd, centred across, its middle 0.04 m + half its depth ahead (M: which way it opens isn't read)
			float size = range(1.94f, 2.15f) * 0.5f * k;
			Fx.add(cone.mesh(cones, side.scale(-1), up).at(at.add(fwd.scale(0.04 + 0.177 * size)).subtract(up.scale(size / 2)))
				.life(range(0.04f, 0.06f)).size(size, size).alpha(0.8f).fade(0.01f, 0.5f).nearFade(near));
		}
		for (int i = 0; i < 2; i++) {
			Particle f = Fx.particle("cassidy_flash#4", flash);
			if (f != null) {
				float size = range(1.1f, 1.2f) * 0.5f * k;
				Fx.add(f.at(at).life(range(0.08f, 0.09f)).size(size, size * 0.8f).spin(range(0, 6.28f), 0).fade(0.01f, 0.5f).nearFade(near));
			}
			Particle b = Fx.particle("cassidy_flash#4", Palette.CS_MUZZLE_FLASH_C);
			if (b != null) {
				float size = range(1.22f, 1.32f) * 0.5f * k;
				Fx.add(b.at(at.subtract(fwd.scale(0.07 * k))).life(0.06f).size(size, size * 0.8f).alpha(0.7f).spin(range(0, 6.28f), 0)
					.fade(0.01f, 0.5f).nearFade(near));
			}
		}
		for (int i = 0; i < 4; i++) {
			Particle s = Fx.particle("cassidy_spark", Palette.CS_MUZZLE_SPARK);
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(cone(fwd, 40).scale(range(1.9f, 5.3f) * k)).life(range(0.02f, 0.18f))
					.size(range(0.03f, 0.06f) * k, 0.02f * k).aspect(0.3f).fade(0.01f, 0.5f).nearFade(near));
			}
		}
		float[] ring = own ? null : Fx.mesh("cassidy_muzzle_ring");
		Particle r = ring == null ? null : Fx.particle("cassidy_cone", Palette.CS_MUZZLE_RING);
		if (r != null) {
			// the flat ring (flat in its file's X-Z, so the glb's X-Y, as Tracer's recall ring) square to the shot, set down half its size
			float size = range(0.7f, 0.8f);
			Fx.add(r.mesh(ring, side, up).at(at.subtract(up.scale(size / 2))).life(range(0.03f, 0.04f)).size(size, size * 1.2f).alpha(0.7f)
				.fade(0.01f, 0.5f));
		}
		for (int i = 0; i < 2; i++) {
			Particle s = Fx.soft("ana_puff#4", Palette.CS_MUZZLE_SMOKE);
			if (s != null) {
				float len = range(0.28f, 0.4f) * 2 * k;
				Vec3 dir = cone(fwd, 27);
				Fx.add(s.at(at.add(dir.scale(len / 2))).shape(Shape.STREAK).velocity(dir.scale(range(1.5f, 1.7f) * k)).drag(0.2f)
					.life(range(0.25f, 0.45f)).size(len, len * 1.4f).aspect(0.45f).alpha(0.45f).spin(range(0, 6.28f), 0)
					.fade(0.05f, 0.6f).nearFade(near));
			}
		}
	}

	/**
	 * Where a shot stops, a creature or the world (05AE, H: the same effect for both): flashes 5B71 (4) 0.175-0.2 m, 0.19 s, at 0.5
	 * m/s in a 30 degree cone (4 of 8: ours); shards EF7 x 5B70 0.25 m, 0.18 s, 0.15 m off the surface; on a block the decal 29F7 0.2
	 * m, 1.87 s, turned at random (ours: none on a creature, the data puts one on both).
	 */
	private static void hit(Vec3 at, Vec3 normal, boolean block, boolean enemy) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal.normalize();
		Vec3 c = at.add(n.scale(0.03));
		for (int i = 0; i < 4; i++) {
			Particle f = Fx.particle("cassidy_hit_flash#4", enemy ? Palette.CS_HIT_ENEMY : Palette.CS_HIT);
			if (f != null) {
				Fx.add(f.at(c).velocity(cone(n, 30).scale(0.5f)).life(0.19f).size(range(0.175f, 0.2f), 0.15f).spin(range(0, 6.28f), 0)
					.loop(4 / 0.19f).fade(0.02f, 0.5f).nearFade(0.3f));
			}
		}
		float[] shard = Fx.mesh("torb_hit_shard_mesh");
		for (int i = 0; i < 2 && shard != null; i++) {
			Particle s = Fx.particle("torb_hit_shard#4", enemy ? Palette.CS_HIT_SHARD_ENEMY : Palette.CS_HIT_SHARD);
			if (s != null) {
				Vec3 side = cone(n, 80).cross(n);
				Fx.add(s.mesh(shard, side, cone(n, 30)).at(c.add(n.scale(0.15 * Fx.RANDOM.nextFloat()))).life(0.18f).size(0.25f, 0.2f)
					.fade(0.02f, 0.5f));
			}
		}
		if (block) {
			Particle decal = Fx.particle("lucio_spark", enemy ? Palette.CS_HIT_DECAL_ENEMY : Palette.CS_HIT_DECAL);
			if (decal != null) {
				Fx.add(decal.at(at).normal(n).lift(n).life(1.87f).size(0.2f, 0.2f).alpha(0.8f).spin(range(0, 6.28f), 0).fade(0.01f, 0.6f));
			}
		}
	}

	/**
	 * Deadeye's hit 010F on the target's chest (H; no world slot): puffs 1BE6 (4) 2 at 2 m/s, 0.23-0.33 m, 0.05 s; a shard EF7 x 1BEC
	 * 0.19-0.29 m, 0.05 s (one of 2: ours); no decal on a creature (ours: the data's 220A 0.035 m).
	 */
	private static void deadeyeHit(Vec3 at, Vec3 normal) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal.normalize();
		for (int i = 0; i < 2; i++) {
			Particle p = Fx.particle("sombra_impact_spark#4", Palette.CS_DEADEYE_DECAL);
			if (p != null) {
				Fx.add(p.at(at.add(n.scale(0.1))).velocity(cone(n, 40).scale(2)).life(0.05f + 0.05f).size(range(0.23f, 0.33f), 0.3f)
					.spin(range(0, 6.28f), 0).loop(40).fade(0.02f, 0.5f).nearFade(0.3f));
			}
		}
		float[] shard = Fx.mesh("torb_hit_shard_mesh");
		Particle s = shard == null ? null : Fx.particle("sombra_impact_star#2", Palette.CS_DEADEYE_DECAL);
		if (s != null) {
			Fx.add(s.mesh(shard, cone(n, 80).cross(n), n).at(at.add(n.scale(0.1))).life(0.1f).size(range(0.19f, 0.29f), 0.2f).fade(0.02f, 0.5f));
		}
	}

	// ---- Flashbang (01E9) ----

	/**
	 * A Flashbang in flight, fed each frame from its renderer (0600 at the grenade's 0076, H): two ribbons on the solid mask AC4,
	 * 0.3 s, orange (ECA2 r2) and warm yellow (DB42 r3, M both drawn); 0.5 m wide in the data, 0.15 here (ours: 0.5 m on a 25 cm
	 * grenade read as a slab), the second a little narrower (ours) so both show.
	 */
	public static void flashFlight(int id, Vec3 at) {
		Fx.trail(id, at, "cassidy_ribbon", Palette.CS_FLASH_TRAIL, 0.15f, 0.3f, 0.7f);
		Fx.trailNearFade(id, 1.5f);
		int second = id ^ 0x40000000;
		Fx.trail(second, at, "cassidy_ribbon", Palette.CS_FLASH_TRAIL_B, 0.08f, 0.3f, 0.8f);
		Fx.trailNearFade(second, 1.5f);
	}

	/**
	 * The bang (5C8C, H; its parts, the gold rows for his side M): the flash 2D6C9 1.5 m, 0.26 s; the flash 16E9D x 29887 2.0 m, 0.14 s;
	 * the hemisphere 963D (2 x 1 m) at 1.0, 0.33 s; 4 streaks 1FBCE 0.37-0.5 m, 0.06-0.12 s. Bang Bang's second (6541, H): flashes 1.7 m
	 * and 2.6 m, 8 streaks 0.35-0.75 m, the sphere DBF at 3.45 instead of the dome.
	 */
	private static void bang(Vec3 at, boolean second, boolean enemy) {
		Particle flash = Fx.particle("cassidy_bang", enemy ? Palette.CS_FLASH_ENEMY : Palette.CS_FLASH);
		if (flash != null) {
			float size = second ? 1.7f : 1.5f;
			Fx.add(flash.at(at).life(0.26f).size(size, size * 1.1f).spin(range(0, 6.28f), 0).fade(0.02f, 0.6f).nearFade(1));
		}
		Particle rays = Fx.particle("cat_cone_line", Palette.CS_FLASH_RAYS);
		if (rays != null) {
			float size = second ? 2.6f : 2.0f;
			Fx.add(rays.at(at).life(0.14f).size(size, size).alpha(0.7f).spin(range(0, 6.28f), 0).fade(0.02f, 0.6f).nearFade(1));
		}
		Palette domeColour = enemy ? Palette.CS_FLASH_DOME_ENEMY : Palette.CS_FLASH_DOME;
		float[] ball = Fx.mesh("cat_bubble");
		Particle b = ball == null ? null : Fx.particle("cat_cloud", domeColour);
		if (b != null) {
			// Bang Bang's sphere DBF at 3.45 (H); the first's two half-domes 963D (2 m across, H) drawn as the 2 m sphere DBF (ours: the
			// two halves close into that ball; their axis in the file not read). It stands on its foot: set down by half its size
			float size = second ? 3.45f : 2.0f;
			Fx.add(b.mesh(ball, new Vec3(1, 0, 0)).at(at.subtract(0, size / 2, 0)).life(0.33f).size(size, size * 1.05f).alpha(0.45f)
				.fade(0.02f, 0.6f));
		}
		int streaks = second ? 8 : 4;
		for (int i = 0; i < streaks; i++) {
			Particle s = Fx.particle("sigma_streak", enemy ? Palette.CS_FLASH_STREAK_ENEMY : Palette.CS_FLASH_STREAK);
			if (s != null) {
				Vec3 dir = randomUnit();
				float len = second ? range(0.35f, 0.75f) : range(0.37f, 0.5f);
				Fx.add(s.at(at.add(dir.scale(len * 0.6f))).axis(dir).velocity(dir.scale(0.1f)).life(range(0.06f, 0.12f)).size(len, len)
					.aspect(0.25f).fade(0.02f, 0.5f).nearFade(1));
			}
		}
	}

	// ---- each frame ----

	/** Each frame: every Cassidy's Silver Bullet armed and Deadeye held; the reloads, rolls and bleeds under way. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			RELOADS.clear();
			ROLLS.clear();
			BLEEDS.clear();
			RATES.clear();
			return;
		}
		float now = Fx.now();
		RELOADS.removeIf(r -> {
			Entity e = mc.level.getEntity(r.entity);
			float t = now - r.start;
			if (e == null || !e.isAlive() || t > r.length || t < -1) {
				return true;
			}
			reload(e, r, t);
			return false;
		});
		ROLLS.removeIf(r -> {
			Entity e = mc.level.getEntity(r.entity);
			float t = now - r.start;
			if (e == null || !e.isAlive() || t > r.length || t < -1) {
				return true;
			}
			roll(e, r);
			return false;
		});
		BLEEDS.removeIf(b -> {
			Entity e = mc.level.getEntity(b.entity);
			float t = now - b.start;
			if (e == null || !e.isAlive() || t > b.length || t < -1) {
				return true;
			}
			bleed(e, b, t);
			return false;
		});
		RATES.keySet().removeIf(id -> mc.level.getEntity(id) == null);
		for (Player p : mc.level.players()) {
			Integer look = p.getAttached(HeroManager.CASSIDY_LOOK);
			if (look == null || look == 0 || !p.isAlive() || ownFirstPerson(p)) {
				continue;
			}
			boolean enemy = KirikoFx.enemyOf(p);
			Rate[] rates = RATES.computeIfAbsent(p.getId(), id -> new Rate[] {new Rate(), new Rate(), new Rate()});
			if ((look & Cassidy.LOOK_SILVER) != 0) {
				silverArmed(p, enemy);
			}
			if ((look & Cassidy.LOOK_DEADEYE) != 0) {
				deadeye(p, rates, enemy);
			}
		}
	}

	/** A posed point, else ours from his body: so far up him, a little ahead and to his right. */
	private static Vec3 body(Player p, int which, double up, double right, double ahead) {
		Vec3 posed = point(p, which);
		if (posed != null) {
			return posed;
		}
		Vec3 fwd = Vec3.directionFromRotation(0, p.getVisualRotationYInDegrees());
		Vec3 side = fwd.cross(UP).normalize();
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		return p.getPosition(partial).add(0, up, 0).add(side.scale(right)).add(fwd.scale(ahead));
	}

	/**
	 * Silver Bullet armed (6E6B at his right wrist 0003, H; re-held while armed, M): the glow 19167 x 4C09 0.55-0.74 m (ours 0.6 at 0.4
	 * alpha) and the glow 18E48 x 4C09 0.4 m; the navy-to-cyan row to his side, the dark red one to his enemies (CB3F r4 / r5, M).
	 */
	private static void silverArmed(Player p, boolean enemy) {
		Vec3 wrist = body(p, WRIST, 1.04, 0.54, 0.2);
		Palette colour = enemy ? Palette.CS_SILVER_ARMED_ENEMY : Palette.CS_SILVER_ARMED;
		Particle glow = Fx.attach("cassidy_silver/" + p.getId() + "/glow", wrist, () -> {
			Particle s = Fx.particle("cat_soft", colour);
			return s == null ? null : s.life(0.3f).fade(0.2f, 0.5f).nearFade(0.6f);
		});
		if (glow != null) {
			glow.at(wrist.add(0, -0.05, 0)).size(0.6f, 0.6f).alpha(0.4f);
		}
		Particle core = Fx.attach("cassidy_silver/" + p.getId() + "/core", wrist, () -> {
			Particle s = Fx.particle("cat_cloud", colour);
			return s == null ? null : s.life(0.3f).fade(0.2f, 0.5f).nearFade(0.6f);
		});
		if (core != null) {
			core.size(0.4f, 0.4f).alpha(0.7f);
		}
	}

	/**
	 * Deadeye held on him (0610, H parts, M roles), skipped in his own view: the red eye glint 2F4F0 1.25 m at his head 0012 + (-0.05,
	 * -0.02, +0.10) (ours 0.6 m: at the data's size from 3 m away his face read as lit) and the star F7A7 0.8 m there (ours 0.4); the
	 * glow 139C 1.67-1.86 m at his chest 0001 + 0.1 m (ours 1.75 m at 0.15 alpha); the dust AFF 2.2-3.3 a second, 1.0-2.3 m, 1.7-2.0 s,
	 * 1.7-2.2 m/s sideways (ours 2 a second, 1.5 m) off to his side.
	 */
	private static void deadeye(Player p, Rate[] rates, boolean enemy) {
		Vec3 fwd = Vec3.directionFromRotation(0, p.getVisualRotationYInDegrees());
		Vec3 side = fwd.cross(UP).normalize();
		Vec3 head = body(p, HEAD, 1.70, 0, 0);
		Vec3 eye = head.add(fwd.scale(0.10)).add(side.scale(-0.05)).add(0, -0.02, 0);
		Particle glint = Fx.attach("cassidy_deadeye/" + p.getId() + "/eye", eye, () -> {
			Particle s = Fx.particle("brigitte_flare_star", Palette.CS_DEADEYE_EYE);
			return s == null ? null : s.life(0.4f).fade(0.2f, 0.5f).nearFade(0.8f);
		});
		if (glint != null) {
			glint.size(0.6f, 0.6f).alpha(0.9f);
		}
		Particle star = Fx.attach("cassidy_deadeye/" + p.getId() + "/star", eye, () -> {
			Particle s = Fx.particle("cat_sparkle", Palette.CS_DEADEYE_STAR);
			return s == null ? null : s.life(0.4f).fade(0.2f, 0.5f).nearFade(0.8f).twinkle(1.5f);
		});
		if (star != null) {
			star.size(0.4f, 0.4f);
		}
		Vec3 chest = body(p, CHEST, 1.44, 0, 0).add(fwd.scale(0.1));
		Palette glowColour = enemy ? Palette.CS_DEADEYE_GLOW_ENEMY : Palette.CS_DEADEYE_GLOW;
		Particle glow = Fx.attach("cassidy_deadeye/" + p.getId() + "/glow", chest, () -> {
			Particle s = Fx.particle("mercy_soft", glowColour);
			return s == null ? null : s.life(0.5f).fade(0.3f, 0.5f).nearFade(1.5f);
		});
		if (glow != null) {
			glow.size(1.75f, 1.75f).alpha(0.15f);
		}
		int dust = rates[0].take(2);
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		for (int i = 0; i < dust; i++) {
			Particle d = Fx.soft("cassidy_dust_cloud", Palette.CS_ROLL_DUST);
			if (d != null) {
				Vec3 way = Fx.RANDOM.nextBoolean() ? side : side.scale(-1);
				Vec3 at = p.getPosition(partial).add(way.scale(range(0.8f, 1.2f))).add(fwd.scale(range(1, 2.5f))).add(0, range(0.8f, 1), 0);
				Fx.add(d.at(at).velocity(way.scale(range(1.7f, 2.2f))).drag(0.5f).life(range(1.7f, 2.0f)).size(1.0f, 1.5f).alpha(0.3f)
					.spin(range(0, 6.28f), range(-0.5f, 0.5f)).fade(0.2f, 0.6f).nearFade(1.5f));
			}
		}
	}

	/**
	 * The reload's barrel smoke (revolver clips 0402 1P 1.5 s / 05C0 3P 1.63 s at the muzzle 0006, H): smoke 9EF (4) x B0A, 50 a second
	 * from 0.33 s for 0.67 s (ours: one a tick, 20), 0.67-0.87 s, 0.11-0.24 m (ours 0.18), drifting up 0.06-0.11 m/s; peach (4287 r0);
	 * at 0.5 alpha (ours), scaled to the reload's length.
	 */
	private static void reload(Entity e, Timed r, float t) {
		boolean own = ownFirstPerson(e);
		float k = r.length / (own ? 1.5f : 1.63f);
		if (t < 0.33f * k || t > 1.0f * k) {
			return;
		}
		Vec3 at;
		if (own) {
			at = muzzle(e, e.getEyePosition());
		} else {
			Vec3 posed = point(e, MUZZLE);
			if (posed == null) {
				Vec3 look = e.getViewVector(1);
				Vec3 side = look.cross(UP);
				side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
				posed = e.getEyePosition().add(side.scale(0.3)).add(0, -0.45, 0).add(look.scale(0.4));
			}
			at = posed;
		}
		int puffs = r.rate.take(20);
		float scale = own ? 0.35f : 1;
		for (int i = 0; i < puffs; i++) {
			Particle s = Fx.soft("ana_smoke#4", Palette.CS_RELOAD_SMOKE);
			if (s != null) {
				Fx.add(s.at(at).velocity(new Vec3(0, range(0.06f, 0.11f), 0)).life(range(0.67f, 0.87f)).size(0.18f * scale, 0.24f * scale)
					.alpha(0.5f).spin(range(0, 6.28f), range(-1, 1)).loop(4 / 0.5f).fade(0.1f, 0.6f).nearFade(own ? 0.15f : 0.5f));
			}
		}
	}

	/**
	 * Combat Roll's start (05E4 at his root 0004, H): 600 motes 242 at 4-4.2 m/s, 0.02-0.13 m, 0.34-0.44 s (ours 40, a fifteenth),
	 * warm (4A41 r2's tint; ours CS_ROLL_DUST's top).
	 */
	private static void rollStart(@Nullable Entity e, Vec3 feet) {
		if (ownFirstPerson(e)) {
			return;
		}
		for (int i = 0; i < 40; i++) {
			Particle m = Fx.particle("mercy_glow", Palette.CS_ROLL_DUST);
			if (m == null) {
				break;
			}
			Vec3 dir = randomUnit().multiply(1, 0.4, 1);
			Fx.add(m.at(feet.add(0, 0.2, 0)).velocity(dir.scale(range(4, 4.2f))).drag(0.05f).life(range(0.34f, 0.44f))
				.size(range(0.02f, 0.13f), 0.02f).fade(0.05f, 0.5f).nearFade(0.5f));
		}
	}

	/**
	 * Through the roll (05E4, H): dust clouds 5429 x B08 x 1FC65 (32 frames), 60 a second + 3 for 0.33 s (ours 3 a tick: 60), 1.26-1.76
	 * m (ours 1.5), 0.5-0.64 s, rising 1.24-1.55 m/s back along the roll (dir (0, 0.75 up, -0.37 back)); at 0.35 alpha (ours). His own
	 * view gets them too, behind and below him.
	 */
	private static void roll(Entity e, Timed r) {
		if (Fx.now() - r.start > 0.33f) {
			return;
		}
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Vec3 feet = e.getPosition(partial);
		Vec3 way = r.dir.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, e.getYRot()) : r.dir.normalize();
		Vec3 rise = new Vec3(0, 0.75, 0).add(way.scale(-0.37)).normalize();
		int puffs = r.rate.take(60);
		boolean own = ownFirstPerson(e);
		for (int i = 0; i < puffs; i++) {
			Particle d = Fx.soft("cassidy_dust#32", Palette.CS_ROLL_DUST);
			if (d == null) {
				break;
			}
			Vec3 off = new Vec3(Fx.RANDOM.nextGaussian(), 0, Fx.RANDOM.nextGaussian()).scale(0.2);
			float size = range(1.26f, 1.76f) * (own ? 0.6f : 0.85f);
			Fx.add(d.at(feet.add(off).add(0, 0.3, 0)).velocity(rise.scale(range(1.24f, 1.55f))).drag(0.3f).life(range(0.5f, 0.64f))
				.size(size * 0.6f, size).alpha(0.35f).spin(range(0, 6.28f), range(-1, 1)).loop(32 / 0.6f).fade(0.1f, 0.6f).nearFade(1.5f));
		}
	}

	/**
	 * Silver Bullet's bleed on the target's chest 0001 (3488, H; the bleed runs 1.5 s): glows A7B6 x 37B2E 0.11 -> 0.63 m and 0.49 ->
	 * 1.01 m (sized by its data flow, M: grows with the bleed; ours one glow 0.3 -> 0.8 m over its 1.5 s), pink to magenta (F7C6 r0);
	 * sparks 1F35 at 3.8-4.4 m/s, 0.02-0.05 m, 0.2 s, 5 at each step (ours 2 every 0.2 s: 10 a second), dark red to pink (8FF6 r1).
	 */
	private static void bleed(Entity e, Timed b, float t) {
		float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Vec3 chest = e.getPosition(partial).add(0, e.getBbHeight() * 0.6, 0);
		float share = Math.clamp(t / b.length, 0, 1);
		Particle glow = Fx.attach("cassidy_bleed/" + e.getId(), chest, () -> {
			Particle s = Fx.particle("cassidy_bleed_glow", Palette.CS_BLEED);
			return s == null ? null : s.life(0.3f).fade(0.1f, 0.6f).nearFade(0.8f);
		});
		if (glow != null) {
			float size = 0.3f + 0.5f * share;
			glow.size(size, size).alpha(0.7f);
		}
		int sparks = b.rate.take(10);
		for (int i = 0; i < sparks; i++) {
			Particle s = Fx.particle("spark#4", Palette.CS_BLEED_SPARK);
			if (s != null) {
				Fx.add(s.at(chest).shape(Shape.STREAK).velocity(randomUnit().scale(range(3.8f, 4.4f))).drag(0.05f).life(0.2f)
					.size(range(0.06f, 0.15f), 0.05f).aspect(0.3f).fade(0.02f, 0.5f).nearFade(0.5f));
			}
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
