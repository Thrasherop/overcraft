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
		/** An additive layer over the clip (Self-Destruct's), and its time from the cast. */
		@Nullable String layer;
		float layerTime;
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
		state.layer = null;
		float t = (entity.tickCount + partialTicks) / 20f;
		if (entity.kind() == DvaMechEntity.WRECK) {
			state.clip = "destroyed_sm0";
			state.time = (float) dev.overcraft.dva.DvaStats.WRECK_TIME + t;
		} else if (entity.kind() == DvaMechEntity.SELF_DESTRUCT) {
			// flying on to its blast: her stand, and over it the Self-Destruct layer (alias 0x175: 3644, additive, 5.03 s played
			// over 3.5 s from the cast, D 0B2F #300; the mech's panels and arms open by stages, #132)
			state.clip = "idle";
			state.time = t;
			state.layer = "add_self_destruct_sm0";
			state.layerTime = t;
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
			SkinnedModel.Clip layer = state.layer == null ? null : model.clips.get(state.layer);
			if (layer != null && layer.duration() > 0) {
				float speed = layer.duration() / (float) dev.overcraft.dva.DvaStats.SELF_DESTRUCT_CLIP_TIME;
				Skeleton.addAdditive(layer, Math.min(state.layerTime * speed, layer.duration()), pose, 1);
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
