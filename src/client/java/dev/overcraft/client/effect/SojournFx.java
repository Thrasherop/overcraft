package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.net.EffectPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sojourn's effects, for now stand-ins (her real ones, read from her data, are step 8 of sojourn-plan.md): her Railgun rounds a
 * small cyan glow with a short trail, her Charged Shot a cyan band from the muzzle to where it stopped (its tracer 3216 is a
 * visual only, 250 m/s: drawn at once here). Her side sees cyan, her enemies the red.
 */
public final class SojournFx {
	private SojournFx() {
	}

	/** How far from her own eyes her rounds start to show in her first-person view (m, ours). */
	private static final double OWN_CLEAR = 3;

	private static Palette or(boolean enemy, Palette ours) {
		return enemy ? Palette.ENEMY : ours;
	}

	/** A Railgun round in flight (ProjectileModelRenderer): false without the local pack (its item stands in). */
	public static boolean roundFlight(int id, Vec3 at, @Nullable Entity owner) {
		Palette p = or(KirikoFx.enemyOf(owner), Palette.CYAN);
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (owner == mc.player && mc.options.getCameraType().isFirstPerson() && at.distanceTo(mc.player.getEyePosition()) < OWN_CLEAR) {
			// in her own view a round leaves from her eyes: its glow there sat on the crosshair, a blob over the target at 16 a
			// second (found playing). Drawn from 3 m on
			return true;
		}
		Particle core = Fx.attach("railgun_shot/" + id, at, () -> {
			Particle g = Fx.particle("lucio_spark", p);
			return g == null ? null : g.life(0.1f).size(0.18f, 0.18f).nearFade(1.5f).fade(0.02f, 0.5f);
		});
		if (core == null) {
			return false;
		}
		Fx.trail(id, at, "mercy_band", p, 0.06f, 0.05f, 0.8f);
		return true;
	}

	/** Her moments from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		switch (e.effect()) {
			case "sojourn_tracer" -> tracer(at, at.add(dir), enemy);
			default -> {
				return false;
			}
		}
		return true;
	}

	/** The Charged Shot's tracer: a cyan band 0.2 m wide from the muzzle to where it stopped, fading over 0.5 s (ours). */
	private static void tracer(Vec3 from, Vec3 to, boolean enemy) {
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len < 0.5f) {
			return;
		}
		Particle band = Fx.particle("ana_tracer", or(enemy, Palette.CYAN));
		if (band != null) {
			Fx.add(band.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(0.2f / len).life(0.5f).fade(0.01f, 0.8f)
				.nearFade(0.6f));
		}
	}
}
