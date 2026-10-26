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

	public static class State extends EntityRenderState {
		float yRot;
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
		KirikoFx.foxPetals(at, Fx.since(entity.getId()), 33);
		KirikoFx.foxWake(entity.getId(), at.add(0, 0.6 * SCALE, 0));
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
			HeroModelRenderer.submitSpirit(loaded, skin, poseStack, collector, 0.6f, 0.5f);
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}
}
