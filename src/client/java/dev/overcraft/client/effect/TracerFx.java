package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.net.EffectPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Tracer's effects (tools/reference/tracer-effects.md, read from her effects' data, 2026-11-16; #150, the owner: "tracer
 * bullets are just minecraft particles"). Her side sees her blue (LUT 0967 row 4), her enemies the red-orange twin (CDAD row
 * 4; M which team sees which). Her shot's line 002C uses the same masks as Sombra's tracer (510 x 50C, the pack's
 * sombra_tracer), so it's drawn with that art in her colours. The world hit 0114 is a flash and a small mark; her data's
 * creature hit is empty, so a creature gets the flash alone (ours).
 */
public final class TracerFx {
	/** Its art, made ahead as the hero is picked (HeroFxArt). */
	public static final java.util.List<EffectArt.Art> ART = java.util.List.of(
		new EffectArt.Art("sombra_tracer", Palette.TR_SHOT, false),
		new EffectArt.Art("sombra_impact_spark#4", Palette.TR_SHOT, false),
		new EffectArt.Art("sombra_impact_burst", Palette.TR_SHOT, false),
		new EffectArt.Art("tracer_blink", Palette.TR_BLINK, false),
		new EffectArt.Art("cat_soft", Palette.TR_BLINK, false),
		new EffectArt.Art("tracer_glow", Palette.TR_BLINK, false),
		new EffectArt.Art("dva_solid", Palette.TR_BLINK, false),
		new EffectArt.Art("lucio_glow", Palette.TR_BOMB, false),
		new EffectArt.Art("tracer_blast#8", Palette.TR_BOMB, false),
		new EffectArt.Art("tracer_blast_decal", Palette.TR_BOMB, false),
		new EffectArt.Art("cat_streak", Palette.TR_BOMB, false));
	public static final java.util.List<EffectArt.Art> ART_ENEMY = java.util.List.of(
		new EffectArt.Art("sombra_tracer", Palette.TR_SHOT_ENEMY, false),
		new EffectArt.Art("sombra_impact_spark#4", Palette.TR_SHOT_ENEMY, false),
		new EffectArt.Art("sombra_impact_burst", Palette.TR_SHOT_ENEMY, false),
		new EffectArt.Art("tracer_blink", Palette.TR_BLINK_ENEMY, false),
		new EffectArt.Art("cat_soft", Palette.TR_BLINK_ENEMY, false),
		new EffectArt.Art("tracer_glow", Palette.TR_BLINK_ENEMY, false),
		new EffectArt.Art("dva_solid", Palette.TR_BLINK_ENEMY, false),
		new EffectArt.Art("lucio_glow", Palette.TR_BOMB_ENEMY, false),
		new EffectArt.Art("tracer_blast#8", Palette.TR_BOMB_ENEMY, false),
		new EffectArt.Art("tracer_blast_decal", Palette.TR_BOMB_ENEMY, false),
		new EffectArt.Art("cat_streak", Palette.TR_BOMB_ENEMY, false));

	/** The line's width (m): 002C's width field, 0.03 (M). */
	private static final float LINE_WIDTH = 0.03f;
	/** The line's life (s): 002C, 0.1 (H). */
	private static final float LINE_LIFE = 0.1f;

	private TracerFx() {
	}

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		switch (e.effect()) {
			case "tracer_shot" -> line(at, at.add(dir), enemy);
			case "tracer_hit" -> flash(at, enemy);
			case "tracer_hit_block" -> blockHit(at, dir, enemy);
			case "tracer_blink" -> blink(at, enemy);
			case "tracer_recall_start" -> recallStart(at, enemy);
			case "tracer_recall_end" -> recallEnd(at, enemy);
			case "tracer_bomb_blast" -> bombBlast(at, enemy);
			default -> {
				return false;
			}
		}
		return true;
	}

	/** The line 002C: a thin band from the muzzle to where the bullet ended, 0.1 s. */
	private static void line(Vec3 from, Vec3 to, boolean enemy) {
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len < 0.5f) {
			return;
		}
		Particle band = Fx.particle("sombra_tracer", enemy ? Palette.TR_SHOT_ENEMY : Palette.TR_SHOT);
		if (band != null) {
			Fx.add(band.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(LINE_WIDTH / len).life(LINE_LIFE)
				.fade(0.01f, 0.6f).nearFade(0.6f));
		}
	}

	/** The world hit's flash F55C (4 frames), about 0.25 m, quick. */
	private static void flash(Vec3 at, boolean enemy) {
		Particle spark = Fx.particle("sombra_impact_spark#4", enemy ? Palette.TR_SHOT_ENEMY : Palette.TR_SHOT);
		if (spark != null) {
			Fx.add(spark.at(at).life(0.06f).size(range(0.23f, 0.29f), 0.2f).spin(range(0, 6.28f), 0).loop(70).nearFade(0.3f));
		}
	}

	/** On a block (0114): the flash and the small mark (decal 220A 0.035 m; 0.2 m 29F7 as one 0.1 m mark, ours), 0.67 s. */
	private static void blockHit(Vec3 at, Vec3 normal, boolean enemy) {
		flash(at.add(normal.scale(0.03)), enemy);
		Particle mark = Fx.particle("sombra_impact_burst", enemy ? Palette.TR_SHOT_ENEMY : Palette.TR_SHOT);
		if (mark != null && normal.lengthSqr() > 1.0E-6) {
			Fx.add(mark.at(at).normal(normal).lift(normal).life(0.67f).size(0.1f, 0.1f).spin(range(0, 6.28f), 0).fade(0.01f, 0.3f));
		}
	}

	/**
	 * Her Pulse Bomb in flight and stuck, fed each frame from its renderer: its ribbon 0171 (mask 1298, 0.53 m wide, points living
	 * 0.59 s, H), and through its fuse the glow 210FD swelling 1.38 -> 2.52 m in three pulses at 0 / 0.33 / 0.67 s of each
	 * second (016E, H; repeated each second of the fuse: ours).
	 */
	public static void bombFlight(int id, Vec3 at, float age, boolean enemy) {
		Palette colour = enemy ? Palette.TR_BOMB_ENEMY : Palette.TR_BOMB;
		Fx.trail(id, at, "dva_solid", colour, 0.53f, 0.59f, 0.8f);
		float phase = age % 0.33f;
		Particle glow = Fx.attach("tracer_bomb/" + id, at, () -> {
			Particle s = Fx.particle("lucio_glow", colour);
			return s == null ? null : s.life(0.2f).fade(0.05f, 0.5f).nearFade(0.5f);
		});
		if (glow != null) {
			float size = 0.6f + 0.9f * (1 - phase / 0.33f);
			glow.at(at).size(size, size).alpha(0.7f);
		}
	}

	/** The blast 0153 (H): its 8-frame flipbook 1D113 0.3 s, a 2.5 m scorch 20CE6 for 1.17 s, 100 sparks at 9-10 m/s. */
	private static void bombBlast(Vec3 at, boolean enemy) {
		Palette colour = enemy ? Palette.TR_BOMB_ENEMY : Palette.TR_BOMB;
		Particle burst = Fx.particle("tracer_blast#8", colour);
		if (burst != null) {
			Fx.add(burst.at(at).life(0.3f).size(3, 4).loop(8 / 0.3f).fade(0.02f, 0.4f).nearFade(1));
		}
		Particle flare = Fx.particle("lucio_glow", colour);
		if (flare != null) {
			Fx.add(flare.at(at).life(0.73f).size(4, 2).alpha(0.8f).fade(0.02f, 0.7f).nearFade(1));
		}
		for (int i = 0; i < 100; i++) {
			Particle spark = Fx.particle("cat_streak", colour);
			if (spark == null) {
				break;
			}
			Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian()).normalize();
			Fx.add(spark.at(at).shape(Fx.Shape.STREAK).velocity(dir.scale(range(9.2f, 10.3f))).life(range(0.13f, 0.52f))
				.size(0.05f, 0.03f).fade(0.05f, 0.5f));
		}
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.level != null) {
			net.minecraft.world.phys.BlockHitResult down = mc.level.clip(new net.minecraft.world.level.ClipContext(at, at.add(0, -3, 0),
				net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
				net.minecraft.world.phys.shapes.CollisionContext.empty()));
			if (down.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
				Particle decal = Fx.particle("tracer_blast_decal", colour);
				if (decal != null) {
					Fx.add(decal.at(down.getLocation()).normal(new Vec3(0, 1, 0)).lift(new Vec3(0, 1, 0)).life(1.17f).size(2.5f, 2.5f)
						.alpha(0.6f).fade(0.02f, 0.5f));
				}
			}
		}
	}

	/**
	 * A Blink (H): where she leaves, 018F's streak flash 214A5 4 m for 0.21 s; with her, 0190's flash 3 m for 0.09 s and its sparks
	 * at 4 m/s (a third of the 200: ours), at her pelvis + 1.1 m.
	 */
	private static void blink(Vec3 feet, boolean enemy) {
		Palette colour = enemy ? Palette.TR_BLINK_ENEMY : Palette.TR_BLINK;
		Vec3 at = feet.add(0, 1.1, 0);
		Particle streak = Fx.particle("tracer_blink", colour);
		if (streak != null) {
			Fx.add(streak.at(at).life(0.21f).size(4, 4).aspect(0.125f).fade(0.02f, 0.6f).nearFade(1.5f));
		}
		Particle flash = Fx.particle("tracer_glow", colour);
		if (flash != null) {
			Fx.add(flash.at(at).life(0.09f).size(3, 3).alpha(0.7f).fade(0.02f, 0.5f).nearFade(1.5f));
		}
		for (int i = 0; i < 60; i++) {
			Particle spark = Fx.particle("cat_soft", colour);
			if (spark == null) {
				break;
			}
			Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian()).normalize();
			Fx.add(spark.at(at).velocity(dir.scale(range(4, 4.2f))).life(range(0.2f, 0.3f)).size(0.05f, 0.02f).fade(0.05f, 0.5f)
				.nearFade(1));
		}
	}

	/** Where her Recall leaves (00BA, H): the 3.5 m flash and the 1.45 m glow at +1 m, 60 sparks drawn in at 5.5 m/s. */
	private static void recallStart(Vec3 feet, boolean enemy) {
		Palette colour = enemy ? Palette.TR_BLINK_ENEMY : Palette.TR_BLINK;
		Vec3 at = feet.add(0, 1, 0);
		Particle flash = Fx.particle("tracer_glow", colour);
		if (flash != null) {
			Fx.add(flash.at(at).life(0.12f).size(3.5f, 3.5f).alpha(0.6f).fade(0.02f, 0.5f).nearFade(1.5f));
		}
		Particle glow = Fx.particle("tracer_glow", colour);
		if (glow != null) {
			Fx.add(glow.at(at).life(0.35f).size(1.45f, 1.45f).alpha(0.8f).fade(0.05f, 0.6f).nearFade(1));
		}
		for (int i = 0; i < 60; i++) {
			Particle spark = Fx.particle("cat_soft", colour);
			if (spark == null) {
				break;
			}
			Vec3 dir = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian()).normalize();
			float life = range(0.22f, 0.33f);
			Fx.add(spark.at(at.add(dir.scale(5.5f * life))).velocity(dir.scale(-5.5f)).life(life).size(0.08f, 0.05f).fade(0.05f, 0.4f)
				.nearFade(1));
		}
	}

	/** Where her Recall lands (00BB, H): the 1.35 m glows and three rings 0958 at +1.1 m rising at 2.25 m/s, 0.23 s each. */
	private static void recallEnd(Vec3 feet, boolean enemy) {
		Palette colour = enemy ? Palette.TR_BLINK_ENEMY : Palette.TR_BLINK;
		Particle glow = Fx.particle("tracer_glow", colour);
		if (glow != null) {
			Fx.add(glow.at(feet.add(0, 1, 0)).life(0.35f).size(1.35f, 1.35f).alpha(0.8f).fade(0.05f, 0.6f).nearFade(1));
		}
		float[] ring = Fx.mesh("tracer_recall_ring");
		for (int i = 0; i < 3; i++) {
			Particle r = Fx.particle("dva_solid", colour);
			if (r == null) {
				break;
			}
			Vec3 at = feet.add(0, 1.1 + 0.2 * i, 0);
			if (ring != null) {
				// laid flat (its Y along +Z in the file's upright pose), set back half its 0.85 m
				r.mesh(ring, new Vec3(1, 0, 0), new Vec3(0, 0, 1));
				at = at.add(0, 0, -0.85 / 2);
			} else {
				r.normal(new Vec3(0, 1, 0));
			}
			Fx.add(r.at(at).velocity(new Vec3(0, 2.25, 0)).life(0.23f).size(0.85f, 0.85f).alpha(0.7f).fade(0.05f, 0.6f).nearFade(1));
		}
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}
}
