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
		new EffectArt.Art("dva_absorb_flash#8", Palette.DVA_ABSORB, false));
	public static final java.util.List<EffectArt.Art> ART_ENEMY = java.util.List.of(
		new EffectArt.Art("dva_matrix_field", Palette.DVA_MATRIX_TUBE, false),
		new EffectArt.Art("dva_solid", Palette.DVA_MATRIX_ENEMY, false),
		new EffectArt.Art("dva_absorb_beam", Palette.DVA_ABSORB_ENEMY, false),
		new EffectArt.Art("dva_absorb_flash#8", Palette.DVA_ABSORB_ENEMY, false));

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

	private DvaFx() {
	}

	/** Each frame: every D.Va whose matrix is up. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
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

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
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
