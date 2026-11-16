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
		new EffectArt.Art("sombra_impact_burst", Palette.TR_SHOT, false));
	public static final java.util.List<EffectArt.Art> ART_ENEMY = java.util.List.of(
		new EffectArt.Art("sombra_tracer", Palette.TR_SHOT_ENEMY, false),
		new EffectArt.Art("sombra_impact_spark#4", Palette.TR_SHOT_ENEMY, false),
		new EffectArt.Art("sombra_impact_burst", Palette.TR_SHOT_ENEMY, false));

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

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}
}
