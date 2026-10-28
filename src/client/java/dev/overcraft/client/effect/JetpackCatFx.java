package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.net.EffectPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Jetpack Cat's effects. A first look, ours: her effect files (6D1E -> 707A on a paw shot, 6D1D -> 7079 on the centre
 * shot, hits 73A4 / 709F; research\jetpack_cat\jetpack_cat-map.md) aren't read yet, so these are a warm glow in our own
 * colours until her effects pass reads their emitters and colour strips as LucioFx's did.
 */
public final class JetpackCatFx {
	/** Whether the effect being made is an enemy hero's (drawn in the enemy red). */
	private static boolean enemy;

	private JetpackCatFx() {
	}

	private static Palette p(Palette normal) {
		return enemy ? Palette.ENEMY : normal;
	}

	/** Her effects by name (from KirikoFx's dispatch); false for one that isn't hers. */
	static boolean dispatch(EffectPayload e, boolean enemyOwner) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		enemy = enemyOwner;
		try {
			switch (e.effect()) {
				case "paw_heal", "paw_hit" -> flash(at, 0.35f);
				case "paw_heal_centre", "paw_hit_centre" -> flash(at, 0.6f);
				case "paw_hit_block" -> flash(at, e.variant() == 1 ? 0.4f : 0.25f);
				default -> {
					return false;
				}
			}
			return true;
		} finally {
			enemy = false;
		}
	}

	/**
	 * A shot in flight, each frame: a small held glow (paw 0.3 m, centre 0.5 m: ours) and a short trail. False without
	 * the pack (nothing to draw it with).
	 */
	public static boolean pawFlight(int id, Vec3 at, boolean centre, boolean enemyOwner) {
		enemy = enemyOwner;
		try {
			float size = centre ? 0.5f : 0.3f;
			Particle glow = Fx.attach("paw_glow/" + id, at, () -> {
				Particle g = Fx.particle("glow", p(Palette.CAT_WARM));
				return g == null ? null : g.life(0.2f).size(size, size).alpha(0.9f).nearFade(2.5f).fade(0.05f, 0.5f);
			});
			if (glow == null) {
				return false;
			}
			Fx.trail(id, at, "lucio_trail", p(Palette.CAT_WARM), centre ? 0.1f : 0.06f, 0.04f, 0.7f);
			return true;
		} finally {
			enemy = false;
		}
	}

	/**
	 * Catnapper's aim: a ring the size of the landing's reach on the spot she'd land, each tick while she aims (the
	 * game's aim loop 739E isn't read yet: ours, so the player sees where the dive goes).
	 */
	public static void aim(net.minecraft.client.Minecraft mc) {
		if (mc.player == null || dev.overcraft.client.ClientHero.hero() != dev.overcraft.hero.Hero.JETPACK_CAT
			|| (dev.overcraft.client.ClientHero.sync().mode() & 7) != dev.overcraft.jetpackcat.JetpackCat.ULT_AIM) {
			return;
		}
		Vec3 at = dev.overcraft.jetpackcat.JetpackCat.diveTarget(mc.player);
		if (at == null) {
			return;
		}
		float size = (float) (dev.overcraft.jetpackcat.JetpackCatStats.ULT_RADIUS * 2);
		Fx.attach("catnapper_aim", at.add(0, 0.05, 0), () -> {
			Particle r = Fx.particle("ring_line", Palette.CAT_WARM);
			return r == null ? null : r.shape(Fx.Shape.FLAT).normal(new Vec3(0, 1, 0)).life(0.3f).size(size, size).alpha(0.9f)
				.fade(0.05f, 0.5f);
		});
	}

	private static void flash(Vec3 at, float size) {
		Particle f = Fx.particle("glow", p(Palette.CAT_WARM));
		if (f != null) {
			Fx.add(f.at(at).life(0.18f).size(size * 0.6f, size).fade(0.02f, 0.7f));
		}
	}
}
