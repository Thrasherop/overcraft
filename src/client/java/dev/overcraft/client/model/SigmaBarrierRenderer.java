package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.overcraft.entity.SigmaBarrierEntity;
import dev.overcraft.sigma.SigmaStats;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Sigma's Experimental Barrier as his data's models (hero_models/sigma_barrier.glb, C015; sigma_barrier_orb.glb, FC88, in
 * the local pack): the barrier, its centre 2.09 m over the entity's foot, facing +Z the way he faced (its middle bulges
 * toward the enemy), and its four emitter orbs on its hardpoints 0029 / 002A / 0032 / 0033 at 2.5 x (1B55 #816-#819).
 * The barrier's material has no texture (a shader in Overwatch): drawn as a field of added light (HeroModelRenderer.submitField).
 * Clips: the barrier's first clip (barrier_sm0, set 0AEF; L: read as its opening) as it goes out, held at its end; the
 * orbs' own loop. Without the pack nothing is drawn (its stand-in effects still show it).
 */
public class SigmaBarrierRenderer extends EntityRenderer<SigmaBarrierEntity, SigmaBarrierRenderer.State> {
	public static class State extends EntityRenderState {
		float yRot;
		float outFor;
		float health;
		HeroModels.@Nullable Loaded barrier;
		HeroModels.@Nullable Loaded orb;
	}

	private static final String[] ORB_POINTS = {"hardpoint_0029", "hardpoint_002A", "hardpoint_0032", "hardpoint_0033"};
	private static final float ORB_SCALE = 2.5f;
	/** The field's strength: its colour scaled to a faint tint (ours, by eye). */
	private static final float FIELD = 0.22f;

	public SigmaBarrierRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	protected AABB getBoundingBoxForCulling(SigmaBarrierEntity entity, float partialTicks) {
		return super.getBoundingBoxForCulling(entity, partialTicks).inflate(SigmaStats.BARRIER_WIDTH / 2, 0.5, SigmaStats.BARRIER_WIDTH / 2);
	}

	@Override
	public void extractRenderState(SigmaBarrierEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.outFor = entity.outFor(partialTicks);
		state.health = entity.health();
		state.barrier = HeroModels.get("sigma_barrier");
		state.orb = HeroModels.get("sigma_barrier_orb");
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		HeroModels.Loaded loaded = state.barrier;
		if (loaded != null) {
			SkinnedModel model = loaded.model();
			Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
			SkinnedModel.Clip open = model.clips.get("barrier_sm0");
			if (open != null && open.duration() > 0) {
				Skeleton.sample(open, Math.min(state.outFor, open.duration()), pose);
			}
			Matrix4f[] skin = Skeleton.skin(model, pose, -1, null);
			poseStack.pushPose();
			poseStack.translate(0, SigmaStats.BARRIER_HEIGHT / 2, 0);
			poseStack.rotateDegrees(Axis.YP, -state.yRot);
			// its material (7C75) is a shader with no texture: a pale blue field of added light, warmer as it weakens (ours)
			float h = Math.clamp(state.health, 0, 1);
			// (added light ignores alpha: the colour itself is its strength; at full strength it hid the world behind it, found
			// playing)
			int r = (int) ((110 + 120 * (1 - h)) * FIELD), gr = (int) ((170 + 40 * h) * FIELD), bl = (int) ((255 - 120 * (1 - h)) * FIELD);
			HeroModelRenderer.submitField(loaded, skin, poseStack, collector, 0x5A000000 | r << 16 | gr << 8 | bl);
			HeroModels.Loaded orb = state.orb;
			if (orb != null) {
				SkinnedModel orbModel = orb.model();
				Skeleton.Pose orbPose = new Skeleton.Pose(orbModel.nodeNames.length).rest(orbModel);
				SkinnedModel.Clip spin = orbModel.clips.get("barrier_orb");
				if (spin != null && spin.duration() > 0) {
					Skeleton.sample(spin, state.outFor % spin.duration(), orbPose);
				}
				Matrix4f[] orbSkin = Skeleton.skin(orbModel, orbPose, -1, null);
				for (String point : ORB_POINTS) {
					int node = model.node(point);
					if (node < 0) {
						continue;
					}
					poseStack.pushPose();
					poseStack.mulPose(Skeleton.world(model, pose, node));
					poseStack.scale(ORB_SCALE, ORB_SCALE, ORB_SCALE);
					HeroModelRenderer.submitModel(orb, orbSkin, poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY,
						java.util.Set.of(), true, false);
					poseStack.popPose();
				}
			}
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}
}
