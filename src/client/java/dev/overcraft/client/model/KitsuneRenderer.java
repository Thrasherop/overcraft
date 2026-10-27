package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overcraft.client.effect.Fx;
import dev.overcraft.client.effect.KirikoFx;
import com.mojang.math.Axis;
import dev.overcraft.entity.KitsuneEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Kitsune Rush's spirit fox: its Overwatch model galloping (from the local asset pack, hero_models/kitsune.glb).
 * Without the model nothing is drawn here: the rush's particles still show the fox's path.
 */
public class KitsuneRenderer extends EntityRenderer<KitsuneEntity, KitsuneRenderer.State> {
	/** Drawn larger than its model (2.9 m nose to tail, 0.85 m tall): ours, so it stands about her chest high. */
	private static final float SCALE = 1.4f;
	/** Within NEAR_START of the camera it isn't drawn; it fades in over the next NEAR_FADE (metres: ours). */
	private static final double NEAR_START = 1.5;
	private static final double NEAR_FADE = 3;

	/** The spirit's colour and glow: ice blue, or pale red for an enemy's fox (ours, after the enemy palette). */
	static final int SPIRIT = 0xC8F4FF;
	static final int GLOW = 0xCFF8FF;
	private static final int ENEMY_SPIRIT = 0xFFB0A8;
	private static final int ENEMY_GLOW = 0xFF3A30;

	public static class State extends EntityRenderState {
		float yRot;
		/** An enemy's fox: drawn red (Overwatch shows an enemy's effects in red). */
		boolean enemy;
		HeroModels.@Nullable Loaded loaded;
	}

	public KitsuneRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected int getBlockLightLevel(KitsuneEntity entity, BlockPos pos) {
		return 15; // a glowing spirit
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(KitsuneEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.loaded = HeroModels.get("kitsune");
		// it sheds sakura petals as it runs (33 a second, the effects survey), and leaves a glowing wake (ours: at
		// 20 m/s, low and ice-blue among the gates, the model alone read as nothing, the second critique)
		Vec3 at = entity.getPosition(partialTicks);
		state.enemy = KirikoFx.enemyOf(entity.owner());
		KirikoFx.foxPetals(at, Fx.since(entity.getId()), 33, state.enemy);
		// not while it's at the camera: it starts inside Kiriko, and its glow there washed her whole view cyan
		Vec3 eye = Minecraft.getInstance().gameRenderer.mainCamera().position();
		if (at.add(0, 0.6 * SCALE, 0).distanceTo(eye) > NEAR_START + NEAR_FADE) {
			// the wake low along the ground, the glow on its body
			KirikoFx.foxWake(entity.getId(), at.add(0, 0.15, 0), state.enemy);
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		HeroModels.Loaded loaded = state.loaded;
		if (loaded != null) {
			SkinnedModel model = loaded.model();
			Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
			SkinnedModel.Clip run = model.clips.get("run");
			if (run != null && run.duration() > 0) {
				Skeleton.sample(run, (state.ageInTicks / 20f) % run.duration(), pose);
			}
			Matrix4f[] skin = Skeleton.skin(model, pose, -1, null);
			poseStack.pushPose();
			// the model faces +Z, like the heroes'
			poseStack.rotateDegrees(Axis.YP, -state.yRot);
			poseStack.scale(SCALE, SCALE, SCALE);
			// fading out as it nears the camera: it starts inside Kiriko and runs through whoever stands in its path, and
			// up close it filled the view with white
			double distance = Math.sqrt(Math.pow(state.x - camera.pos.x, 2) + Math.pow(state.y + 0.6 * SCALE - camera.pos.y, 2)
				+ Math.pow(state.z - camera.pos.z, 2));
			float near = (float) Math.clamp((distance - NEAR_START) / NEAR_FADE, 0, 1);
			if (near > 0) {
				// its own glow strong, so the body is the bright thing (the sixth critique: no brighter than the sky)
				// an enemy's: its blue texture faint under a strong red glow (tinted red, blue went dark and the glow over
				// it lilac-grey)
				// an ally's all added light, its ice-blue texture and a white-cyan glow on it, brighter than the sky behind it
				// (the seventh critique: the fox is the bright thing, about #D8FFFF)
				HeroModelRenderer.submitSpirit(loaded, skin, poseStack, collector, (state.enemy ? 0.35f : 0.8f) * near,
					(state.enemy ? 2.1f : 0.7f) * near, state.enemy ? ENEMY_SPIRIT : SPIRIT, state.enemy ? ENEMY_GLOW : GLOW, !state.enemy);
			}
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}
}
