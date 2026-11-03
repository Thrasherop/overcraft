package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.overcraft.entity.DvaMechEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * D.Va's unmanned MEKA (DvaMechEntity) as her model (hero_models/dva.glb, FBB0), playing the clip its graph plays in that
 * state (tools/clipmaps/dva.txt): falling in on Call Mech, its landing 0xE5 (311F, D 0276 #309 / #316); the wreck, the
 * destruction 0x8 (31C3, #482) from where the eject leaves it (the wreck's first 1.0 s were the player's own). Without the
 * pack nothing is drawn.
 */
public class DvaMechRenderer extends EntityRenderer<DvaMechEntity, DvaMechRenderer.State> {
	public static class State extends EntityRenderState {
		float yRot;
		HeroModels.@Nullable Loaded loaded;
		@Nullable String clip;
		float time;
	}

	public DvaMechRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DvaMechEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.loaded = HeroModels.get(dev.overcraft.hero.Hero.DVA);
		float t = (entity.tickCount + partialTicks) / 20f;
		if (entity.kind() == DvaMechEntity.WRECK) {
			state.clip = "destroyed_sm0";
			state.time = (float) dev.overcraft.dva.DvaStats.WRECK_TIME + t;
		} else if (entity.kind() == DvaMechEntity.SELF_DESTRUCT) {
			// flying on to its blast: her stand (its own 3644 is an additive layer over the base: step 11)
			state.clip = "idle";
			state.time = t;
		} else {
			state.clip = "call_mech";
			state.time = t;
		}
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		HeroModels.Loaded loaded = state.loaded;
		if (loaded != null) {
			SkinnedModel model = loaded.model();
			Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
			SkinnedModel.Clip clip = state.clip == null ? null : model.clips.get(state.clip);
			if (clip != null && clip.duration() > 0) {
				Skeleton.sample(clip, Math.min(state.time, clip.duration()), pose);
			}
			Matrix4f[] skin = Skeleton.skin(model, pose, -1, null);
			poseStack.pushPose();
			// the models face +Z, like the heroes'
			poseStack.rotateDegrees(Axis.YP, -state.yRot);
			HeroModelRenderer.submitModel(loaded, skin, poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY,
				java.util.Set.of(), true, false);
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}
}
