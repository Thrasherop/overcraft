package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * D.Va's effects (tools/reference/dva-effects.md, read from her effects' data, 2026-11-16; #150, the owner: "dva matrix" still
 * Minecraft's). Her side sees teal-green, her enemies orange-red (H rows, M roles). Defense Matrix 2D61 is drawn each frame
 * while her synced state says it's up: the field's tube along her aim, its seven hexagonal ring frames and two pulse rings
 * a second flying out; each projectile it eats sends "dva_absorb" (1506's beam from her arm, 1507's flash).
 */
public final class DvaFx {
	public static final java.util.List<EffectArt.Art> ART = java.util.List.of(
		new EffectArt.Art("dva_matrix_field", Palette.DVA_MATRIX_TUBE, false),
		new EffectArt.Art("dva_solid", Palette.DVA_MATRIX, false),
		new EffectArt.Art("dva_absorb_beam", Palette.DVA_ABSORB, false),
		new EffectArt.Art("dva_absorb_flash#8", Palette.DVA_ABSORB, false),
		new EffectArt.Art("dva_sd_flare", Palette.DVA_SD, false),
		new EffectArt.Art("dva_sd_spark_in", Palette.DVA_SD, false),
		new EffectArt.Art("dva_sd_burst#20", Palette.DVA_SD, false),
		new EffectArt.Art("dva_sd_core", Palette.DVA_SD, false),
		new EffectArt.Art("dva_solid", Palette.DVA_SD, false),
		new EffectArt.Art("spark", Palette.DVA_SD, false),
		new EffectArt.Art("dva_sd_smoke#4", Palette.DVA_SD, false),
		new EffectArt.Art("dva_sd_scorch", Palette.DVA_SD, false));
	public static final java.util.List<EffectArt.Art> ART_ENEMY = java.util.List.of(
		new EffectArt.Art("dva_matrix_field", Palette.DVA_MATRIX_TUBE, false),
		new EffectArt.Art("dva_solid", Palette.DVA_MATRIX_ENEMY, false),
		new EffectArt.Art("dva_absorb_beam", Palette.DVA_ABSORB_ENEMY, false),
		new EffectArt.Art("dva_absorb_flash#8", Palette.DVA_ABSORB_ENEMY, false),
		new EffectArt.Art("dva_sd_flare", Palette.DVA_SD_ENEMY, false),
		new EffectArt.Art("dva_sd_spark_in", Palette.DVA_SD_ENEMY, false),
		new EffectArt.Art("dva_sd_burst#20", Palette.DVA_SD_ENEMY, false),
		new EffectArt.Art("dva_sd_core", Palette.DVA_SD_ENEMY, false),
		new EffectArt.Art("dva_solid", Palette.DVA_SD_ENEMY, false),
		new EffectArt.Art("spark", Palette.DVA_SD_ENEMY, false),
		new EffectArt.Art("dva_sd_smoke#4", Palette.DVA_SD_ENEMY, false),
		new EffectArt.Art("dva_sd_scorch", Palette.DVA_SD_ENEMY, false));

	/** The field's ring frames D655 (2D61, H): their distance ahead (m), size and alpha (rows BC -> 59). */
	private static final float[] RING_AT = {0.35f, 1, 2, 4, 6, 8, 9.5f};
	private static final float[] RING_SIZE = {0.75f, 1, 1.25f, 1.5f, 1.75f, 2, 0.75f};
	private static final float[] RING_ALPHA = {0.74f, 0.68f, 0.6f, 0.52f, 0.45f, 0.4f, 0.35f};
	/** The tube's width (m, its mesh 1.92 m across, length 9.77) and its alpha (the data doesn't give it: ours, low). */
	private static final float TUBE_WIDTH = 1.92f, TUBE_HEIGHT = 1.96f, TUBE_ALPHA = 0.12f;
	/** The ring frame's height against its width (its mesh 0.96 x 0.99 m). */
	private static final float RING_TALL = 0.99f / 0.96f;
	/** The pulse rings (2D61's C37 / C53): 9 m/s, two a second, 0.75 m (H). */
	private static final float PULSE_SPEED = 9, PULSE_EVERY = 0.5f, PULSE_SIZE = 0.75f, FIELD_LENGTH = 9.77f;

	/** Self-Destruct's fields now: the mech's entity id -> when it started (Fx.now), its fuse (s), whether an enemy's. */
	private record Field(float at, float fuse, boolean enemy) {
	}

	private static final java.util.Map<Integer, Field> FIELDS = new java.util.HashMap<>();

	private DvaFx() {
	}

	/** Each frame: every D.Va whose matrix is up. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		float now = Fx.now();
		FIELDS.entrySet().removeIf(f -> {
			Entity mech = mc.level.getEntity(f.getKey());
			float t = now - f.getValue().at();
			if (mech == null || t > f.getValue().fuse()) {
				return true;
			}
			field(f.getKey(), mech.getPosition(partial).add(0, mech.getBbHeight() * 0.6, 0), t, f.getValue());
			return false;
		});
		for (Player p : mc.level.players()) {
			Integer state = p.getAttached(dev.overcraft.hero.HeroManager.DVA_STATE);
			if (state != null && (state & dev.overcraft.dva.Dva.MODE_MATRIX) != 0 && p.isAlive()) {
				matrix(p, partial, KirikoFx.enemyOf(p));
			}
		}
	}

	private static void matrix(Player p, float partial, boolean enemy) {
		Vec3 eye = p.getEyePosition(partial);
		Vec3 aim = p.getViewVector(partial);
		Vec3 side = aim.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		// the meshes stand upright in their files (the glTF export's Y up), their thin side / length along their Z: laid with their up
		// square to her aim, their Z runs along it; they stand on their base, so each is set down by half its height
		Vec3 up = side.cross(aim).normalize();
		boolean own = p == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
		float in = own ? 0.6f : 1;
		float[] tube = Fx.mesh("dva_matrix_tube");
		if (tube != null) {
			Particle t = Fx.attach("dva_matrix/" + p.getId() + "/tube", eye, () -> {
				Particle s = Fx.particle("dva_matrix_field", Palette.DVA_MATRIX_TUBE);
				return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f).nearFade(1.5f);
			});
			if (t != null) {
				// Fx.mesh turns a mesh so its longer level side is its X and scales by it: the tube's 9.77 m length is X, centred
				t.at(eye.add(aim.scale(FIELD_LENGTH / 2)).subtract(up.scale(TUBE_HEIGHT / 2))).mesh(tube, aim, up)
					.size(FIELD_LENGTH, FIELD_LENGTH).alpha(TUBE_ALPHA * in);
			}
		}
		float[] frame = Fx.mesh("dva_matrix_frame");
		Palette rings = enemy ? Palette.DVA_MATRIX_ENEMY : Palette.DVA_MATRIX;
		if (frame != null) {
			for (int i = 0; i < RING_AT.length; i++) {
				final int k = i;
				Particle r = Fx.attach("dva_matrix/" + p.getId() + "/ring" + i, eye, () -> {
					Particle s = Fx.particle("dva_solid", rings);
					return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f).nearFade(own ? 1.5f : 0.5f);
				});
				if (r != null) {
					r.at(eye.add(aim.scale(RING_AT[k])).subtract(up.scale(RING_SIZE[k] * RING_TALL / 2))).mesh(frame, side, up)
						.size(RING_SIZE[k], RING_SIZE[k]).alpha(RING_ALPHA[k] * in);
				}
			}
			// two pulse rings a second, flying out from her at 9 m/s to the field's end
			float now = Fx.now();
			float run = FIELD_LENGTH / PULSE_SPEED;
			for (int k = 0; k < 2; k++) {
				float t = (now + k * PULSE_EVERY) % run;
				final float fade = 1 - t / run;
				Particle r = Fx.attach("dva_matrix/" + p.getId() + "/pulse" + k, eye, () -> {
					Particle s = Fx.particle("dva_solid", rings);
					return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f).nearFade(own ? 1.5f : 0.5f);
				});
				if (r != null) {
					r.at(eye.add(aim.scale(t * PULSE_SPEED)).subtract(up.scale(PULSE_SIZE * RING_TALL / 2))).mesh(frame, side, up)
						.size(PULSE_SIZE, PULSE_SIZE).alpha(0.6f * fade * in);
				}
			}
		}
	}

	/**
	 * Self-Destruct's warning field 1702 on the mech through its fuse (H): the big flare 34DC growing 4 -> 10 m over the first
	 * half (alpha ours, 0.35), sparks 1C00 drawn in at 20-30 m/s, their rate 50 -> 400 a second (a fifth of it here: ours),
	 * the 1.8 m core pulse five times a second, burst flares 4A39 1.5-2 m four a second.
	 */
	private static void field(int id, Vec3 c, float t, Field f) {
		Palette colour = f.enemy() ? Palette.DVA_SD_ENEMY : Palette.DVA_SD;
		float k = Math.min(1, t / f.fuse());
		float grow = Math.min(1, t / (f.fuse() / 2));
		Particle flare = Fx.attach("dva_sd/" + id + "/flare", c, () -> {
			Particle s = Fx.particle("dva_sd_flare", colour);
			return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f);
		});
		if (flare != null) {
			float size = 4 + 6 * grow;
			flare.at(c).size(size, size).alpha(0.35f);
		}
		float dt = Fx.since(-(id * 8 + 1) - 1);
		int sparks = Math.round((10 + 70 * k) * dt);
		for (int i = 0; i < sparks; i++) {
			Particle spark = Fx.particle("dva_sd_spark_in", colour);
			if (spark == null) {
				break;
			}
			Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian()).normalize();
			float speed = 20 + 10 * k;
			float r = speed * 0.2f;
			Fx.add(spark.at(c.add(dir.scale(r))).shape(Fx.Shape.STREAK).velocity(dir.scale(-speed)).life(0.2f).size(0.15f, 0.1f)
				.fade(0.1f, 0.3f));
		}
		// the core pulse: five a second
		if ((int) (t * 5) != (int) ((t - dt) * 5)) {
			Particle core = Fx.particle("dva_sd_core", colour);
			if (core != null) {
				Fx.add(core.at(c).life(0.2f).size(1.75f, 1.8f).alpha(0.6f).fade(0.05f, 0.6f).nearFade(1));
			}
		}
		if ((int) (t * 4) != (int) ((t - dt) * 4)) {
			Particle burst = Fx.particle("dva_sd_burst#20", colour);
			if (burst != null) {
				Vec3 off = new Vec3(Fx.RANDOM.nextFloat() - 0.5, Fx.RANDOM.nextFloat() - 0.5, Fx.RANDOM.nextFloat() - 0.5);
				Fx.add(burst.at(c.add(off)).life(0.5f).size(1.5f + 0.5f * Fx.RANDOM.nextFloat(), 2).loop(40).fade(0.05f, 0.5f).nearFade(1));
			}
		}
	}

	/**
	 * The blast 1703 (H): the 3 m flash sphere 0.6 s, the shock ring 2B63 at 7.9 m for 0.5 s, sparks at 40-60 m/s (40 of the
	 * data's 80: ours), three smoke puffs 3-3.5 m for 2-2.5 s, and a 2 m scorch on the ground for 2 s.
	 */
	private static void blast(Vec3 c, boolean enemy) {
		Palette colour = enemy ? Palette.DVA_SD_ENEMY : Palette.DVA_SD;
		float[] sphere = Fx.mesh("cat_sphere");
		Particle flash = Fx.particle("dva_solid", colour);
		if (flash != null) {
			if (sphere != null) {
				flash.mesh(sphere, new Vec3(1, 0, 0), new Vec3(0, 1, 0));
				Fx.add(flash.at(c.add(0, -1.5, 0)).life(0.6f).size(3, 3.4f).alpha(0.5f).fade(0.02f, 0.7f).nearFade(1));
			} else {
				Fx.add(flash.at(c).life(0.6f).size(3, 3.4f).alpha(0.5f).fade(0.02f, 0.7f).nearFade(1));
			}
		}
		float[] ring = Fx.mesh("dva_sd_ring");
		Particle shock = Fx.particle("dva_solid", colour);
		if (ring != null && shock != null) {
			// the ring stands in its X-Y plane in its file: laid flat (its Y along +Z), set back half its size
			Fx.add(shock.at(c.add(0, -0.75, -7.9 / 2)).mesh(ring, new Vec3(1, 0, 0), new Vec3(0, 0, 1)).life(0.5f).size(2, 7.9f)
				.alpha(0.6f).fade(0.02f, 0.6f).nearFade(1));
		}
		for (int i = 0; i < 40; i++) {
			Particle spark = Fx.particle("spark", colour);
			if (spark == null) {
				break;
			}
			Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Math.abs(Fx.RANDOM.nextGaussian()) * 0.7, Fx.RANDOM.nextGaussian()).normalize();
			Fx.add(spark.at(c).shape(Fx.Shape.STREAK).velocity(dir.scale(40 + 20 * Fx.RANDOM.nextFloat())).drag(0.05f)
				.life(0.25f + 0.75f * Fx.RANDOM.nextFloat()).size(0.08f, 0.05f).fade(0.05f, 0.5f));
		}
		for (int i = 0; i < 3; i++) {
			Particle smoke = Fx.soft("dva_sd_smoke#4", colour);
			if (smoke != null) {
				Vec3 off = new Vec3(Fx.RANDOM.nextFloat() - 0.5, Fx.RANDOM.nextFloat() * 0.5, Fx.RANDOM.nextFloat() - 0.5).scale(2);
				Fx.add(smoke.at(c.add(off)).velocity(new Vec3(0, 0.4, 0)).life(2 + 0.5f * Fx.RANDOM.nextFloat()).size(3, 3.5f)
					.alpha(0.4f).loop(2).fade(0.1f, 0.6f).nearFade(1.5f));
			}
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			// the scorch on the ground under it (the first solid block below, within 6 m: ours)
			net.minecraft.world.phys.BlockHitResult down = mc.level.clip(new net.minecraft.world.level.ClipContext(c, c.add(0, -6, 0),
				net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
				net.minecraft.world.phys.shapes.CollisionContext.empty()));
			if (down.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
				Particle scorch = Fx.soft("dva_sd_scorch", colour);
				if (scorch != null) {
					Fx.add(scorch.at(down.getLocation()).normal(new Vec3(0, 1, 0)).lift(new Vec3(0, 1, 0)).life(2).size(2, 2).alpha(0.7f)
						.fade(0.02f, 0.4f));
				}
			}
		}
	}

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		if (e.effect().equals("dva_sd_field")) {
			FIELDS.put(e.entity(), new Field(Fx.now(), e.variant() / 20f, enemy));
			return true;
		}
		if (e.effect().equals("dva_sd_blast")) {
			blast(new Vec3(e.x(), e.y(), e.z()), enemy);
			return true;
		}
		if (!e.effect().equals("dva_absorb")) {
			return false;
		}
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Minecraft mc = Minecraft.getInstance();
		Entity dva = mc.level == null ? null : mc.level.getEntity(e.entity());
		absorb(dva, at, e.variant(), enemy);
		return true;
	}

	/**
	 * A projectile eaten: 1506's beam from her arm (hp 003D / 003E, alternating: ours as 0.9 m to her side, 0.6 m down from her
	 * eyes) to the hit, 1.0 m wide for 0.17 s; 1507's flash flipbook there, 0.2-0.3 m, 0.2-0.35 s (H).
	 */
	private static void absorb(Entity dva, Vec3 at, int arm, boolean enemy) {
		Palette colour = enemy ? Palette.DVA_ABSORB_ENEMY : Palette.DVA_ABSORB;
		if (dva != null) {
			Vec3 aim = dva.getViewVector(1);
			Vec3 side = aim.cross(new Vec3(0, 1, 0));
			side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
			Vec3 from = dva.getEyePosition().add(side.scale((arm & 1) == 0 ? 0.9 : -0.9)).add(0, -0.6, 0);
			Vec3 piece = at.subtract(from);
			float len = (float) piece.length();
			Particle beam = Fx.particle("dva_absorb_beam", colour);
			if (beam != null && len > 0.3f) {
				Fx.add(beam.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(0.35f / len).life(0.17f).fade(0.02f, 0.6f)
					.nearFade(0.8f));
			}
		}
		Particle flash = Fx.particle("dva_absorb_flash#8", colour);
		if (flash != null) {
			Fx.add(flash.at(at).life(0.3f).size(0.3f, 0.25f).loop(8 / 0.3f).fade(0.02f, 0.5f).nearFade(0.3f));
		}
	}
}
