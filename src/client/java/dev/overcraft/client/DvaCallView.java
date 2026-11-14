package dev.overcraft.client;

import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.dva.Dva;
import dev.overcraft.dva.DvaStats;
import dev.overcraft.entity.DvaMechEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * D.Va's own first-person Call Mech (#133), as Overwatch plays it: the view stays on the pilot and follows her first-person
 * climb into the mech (pilot 1P clip 3134, alias 0xE5: D 026A #405), whose Camera bone is laid out about the mech's spot; a
 * short fade to black as she reaches the cockpit, then the mech's own first-person spawn view (3329, alias 0x435: 0276 #176)
 * with its HUD. Her third person and everyone else's view of her are unchanged (3120 and the mech's 311F). The fade's
 * timing is ours (DvaStats.CALL_FADE_START / CALL_FADE_OUT: no fade node found in the data).
 */
public final class DvaCallView {
	private DvaCallView() {
	}

	/** Blends from her own eyes onto the clip's camera, and from the mech's spawn view back to her look (ours). */
	private static final float BLEND_IN = 0.3f, BLEND_OUT = 0.4f;

	private static @Nullable String clip;
	private static final Vector3f POS = new Vector3f(), FORWARD = new Vector3f();
	private static float posedAt = -100;

	/** The first-person pose just made (HeroAnimator.poseFirstPerson): the camera's transform in the model's space. */
	public static void posed(String viewBase, Matrix4f camera, float now) {
		boolean ours = viewBase.equals("call_mech_sm0") || viewBase.equals("mech_spawn_sm0");
		clip = ours ? viewBase : null;
		if (ours) {
			camera.getTranslation(POS);
			camera.transformDirection(FORWARD.set(0, 0, 1)).normalize();
			posedAt = now;
		}
	}

	private static boolean firstPerson(Minecraft mc) {
		return mc.options.getCameraType().isFirstPerson() && ClientHero.hero() == dev.overcraft.hero.Hero.DVA && ClientHero.heroView();
	}

	/** Where the view is and how it's turned while her clip runs: {x, y, z, yaw, pitch}, or null for Minecraft's own. */
	public static double @Nullable [] camera(LocalPlayer player, float partialTicks) {
		Minecraft mc = Minecraft.getInstance();
		float now = (player.level().getGameTime() + partialTicks) / 20f;
		// (a pose from the future is stale too: a rejoin into a world whose clock is behind the last one's)
		if (clip == null || now - posedAt > 0.25f || now < posedAt || !firstPerson(mc)) {
			return null;
		}
		HeroAnimator a = HeroAnimator.of(player);
		Vec3 feet = player.getPosition(partialTicks);
		float yaw = player.getViewYRot(partialTicks);
		float blend;
		if (clip.equals("call_mech_sm0")) {
			DvaMechEntity mech = mech(player);
			if (mech == null) {
				return null;
			}
			// the clip's camera is laid out about the mech's spot, facing its way (the call's yaw)
			// the clip's origin isn't the mech's spot: its camera starts 2.06 m behind the origin (3134's Camera at frame 0) where
			// she stands 1.27 m behind the mech (026A #434 / #766's MoveTo), so the origin is 0.79 m past the mech's spot, where
			// 311F draws the landed mech's body (its pelvis 0.8-1.1 m ahead of its root). Anchored at the spot, her hands closed
			// 0.8-0.9 m short of the mech's cockpit, on nothing (#134); there they meet it (the built-in pilot's grips at 1.6 s:
			// hers 1.26 m ahead, 1.18 up; the mech's 1.38 / 1.24)
			Vec3 spot = mech.getPosition(partialTicks).add(Vec3.directionFromRotation(0, mech.getYRot()).scale(DvaStats.CALL_VIEW_AHEAD));
			// the clip's camera is laid out on level ground; where the spot is more than a step below or above her (a ledge),
			// its height stays hers so the view doesn't sink into the block she stands on (the critic of 83b6bfa; ours)
			feet = Math.abs(spot.y - feet.y) > STEP ? new Vec3(spot.x, feet.y, spot.z) : spot;
			yaw = mech.getYRot();
			blend = Math.clamp((now - a.dvaFormSince() - (float) DvaStats.CALL_CAST) / BLEND_IN, 0, 1);
		} else {
			// the mech's spawn view about her own feet; back to her own look by its end
			float left = (float) DvaStats.MECH_SPAWN_VIEW - (now - a.dvaLinkedAt());
			blend = Math.clamp(left / BLEND_OUT, 0, 1);
		}
		Vector3f p = new Vector3f(POS).rotateY(-yaw * Mth.DEG_TO_RAD);
		Vector3f f = new Vector3f(FORWARD).rotateY(-yaw * Mth.DEG_TO_RAD);
		Vec3 eye = player.getEyePosition(partialTicks);
		Vec3 at = inOpenAir(player, eye, eye.lerp(feet.add(p.x, p.y, p.z), blend));
		float clipYaw = (float) (Mth.atan2(-f.x, f.z) * Mth.RAD_TO_DEG);
		float clipPitch = (float) (-Math.asin(Math.clamp(f.y, -1, 1)) * Mth.RAD_TO_DEG);
		float ownYaw = player.getViewYRot(partialTicks), ownPitch = player.getViewXRot(partialTicks);
		return new double[] {at.x, at.y, at.z, ownYaw + Mth.wrapDegrees(clipYaw - ownYaw) * blend, Mth.lerp(blend, ownPitch, clipPitch)};
	}

	/** A step (Minecraft's 0.6 m step height): a spot within it counts as level with her. */
	private static final double STEP = 0.6;

	/**
	 * The view pulled in along the ray from her eyes where a block is in the way, as Minecraft's third person is (Camera's
	 * getMaxZoom: 0.1 m off the face): a wall at her back, a ledge's block (the critic of 83b6bfa, #133).
	 */
	private static Vec3 inOpenAir(LocalPlayer player, Vec3 eye, Vec3 want) {
		Vec3 to = want.subtract(eye);
		double length = to.length();
		if (length < 1.0E-4) {
			return want;
		}
		Vec3 dir = to.scale(1 / length);
		double reach = length;
		// eight rays from the corners of a 0.2 m cube about her eyes, as getMaxZoom does, so the near plane doesn't touch a face either
		for (int i = 0; i < 8; i++) {
			double ox = ((i & 1) * 2 - 1) * 0.1, oy = ((i >> 1 & 1) * 2 - 1) * 0.1, oz = ((i >> 2 & 1) * 2 - 1) * 0.1;
			Vec3 from = eye.add(ox, oy, oz);
			net.minecraft.world.phys.BlockHitResult hit = player.level().clip(new net.minecraft.world.level.ClipContext(from,
				from.add(dir.scale(length + 0.1)), net.minecraft.world.level.ClipContext.Block.VISUAL,
				net.minecraft.world.level.ClipContext.Fluid.NONE, player));
			if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
				reach = Math.min(reach, hit.getLocation().distanceTo(from) - 0.1);
			}
		}
		return eye.add(dir.scale(Math.max(0, reach)));
	}

	/** How black the screen is (0-1): in as she reaches the cockpit, held to the link, out as the mech's view comes up. */
	public static float fade(LocalPlayer player, float partialTicks) {
		Minecraft mc = Minecraft.getInstance();
		if (!firstPerson(mc)) {
			return 0;
		}
		HeroAnimator a = HeroAnimator.of(player);
		float now = (player.level().getGameTime() + partialTicks) / 20f;
		if (a.dvaForm() == Dva.CALLING) {
			float t = now - a.dvaFormSince() - (float) DvaStats.CALL_CAST;
			return Math.clamp((t - (float) DvaStats.CALL_FADE_START) / (float) (DvaStats.CALL_FADE_BLACK - DvaStats.CALL_FADE_START), 0, 1);
		}
		if (a.dvaForm() == Dva.MECH) {
			float since = now - a.dvaLinkedAt();
			return since < 0 || since >= DvaStats.CALL_FADE_OUT ? 0 : 1 - since / (float) DvaStats.CALL_FADE_OUT;
		}
		return 0;
	}

	/** Her called mech, falling in (this client's copy). */
	private static @Nullable DvaMechEntity mech(LocalPlayer player) {
		for (DvaMechEntity m : player.level().getEntitiesOfClass(DvaMechEntity.class, new AABB(player.blockPosition()).inflate(8))) {
			if (m.kind() == DvaMechEntity.CALL && m.ownerId() == player.getId()) {
				return m;
			}
		}
		return null;
	}
}
