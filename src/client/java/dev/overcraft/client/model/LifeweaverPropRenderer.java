package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.overcraft.entity.PetalPlatformEntity;
import dev.overcraft.entity.SeedEntity;
import dev.overcraft.entity.TreeOfLifeEntity;
import dev.overcraft.lifeweaver.LifeweaverStats;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Lifeweaver's placed things as their Overwatch models (hero_models/lifeweaver_<name>.glb in the local pack), each
 * playing the clips its own graph plays in its state (the alias tables, tools/clipmaps/lifeweaver_platform.txt and
 * lifeweaver_tree.txt). Without the pack nothing is drawn (their effects still show them).
 */
public class LifeweaverPropRenderer<T extends Entity> extends EntityRenderer<T, LifeweaverPropRenderer.State> {
	public static class State extends EntityRenderState {
		float yRot;
		HeroModels.@Nullable Loaded loaded;
		/** The clip and its time, and an additive one on top (or null). */
		@Nullable String clip;
		float time;
		@Nullable String add;
		float addTime;
		/** Drawn this far up from the entity's position (the tree's box starts under its root). */
		float lift;
		float partialTick;
	}

	private final String model;
	private final BiConsumer<T, State> poser;

	private LifeweaverPropRenderer(EntityRendererProvider.Context context, String model, BiConsumer<T, State> poser) {
		super(context);
		this.model = model;
		this.poser = poser;
	}

	/**
	 * Petal Platform (368D): unfolding as it lands, then its idle loop; the rise over the rise; the raised loop at the
	 * top; the descent over the descent, then its landing (7/30 s).
	 */
	public static LifeweaverPropRenderer<PetalPlatformEntity> platform(EntityRendererProvider.Context context) {
		return new LifeweaverPropRenderer<>(context, "lifeweaver_platform", (e, s) -> {
			float t = (e.phaseTicks() + s.partialTick) / 20f;
			switch (e.phase()) {
				case PetalPlatformEntity.RISING -> clip(s, "rise", t, false);
				case PetalPlatformEntity.RAISED -> clip(s, "raised", t, true);
				case PetalPlatformEntity.DESCENDING -> clip(s, "descend", t, false);
				default -> {
					boolean landed = e.tickCount > e.phaseTicks() + 1;
					float first = landed ? 7 / 30f : 0.8f;
					if (t < first) {
						clip(s, landed ? "land" : "unfold", t, false);
					} else {
						clip(s, "idle", t - first, true);
					}
				}
			}
		});
	}

	/** Sow the Seed's seed on the ground: his model 126CC, still. */
	public static LifeweaverPropRenderer<SeedEntity> seed(EntityRendererProvider.Context context) {
		return new LifeweaverPropRenderer<>(context, "lifeweaver_seed", (e, s) -> s.clip = null);
	}

	/**
	 * Tree of Life (36AA): its growth (in the open, by a wall, or under a low ceiling: two clips, the second cut by the
	 * room), held at its end; the pulse layer on each pulse; its end 1.67 s before it goes.
	 */
	public static LifeweaverPropRenderer<TreeOfLifeEntity> tree(EntityRendererProvider.Context context) {
		return new LifeweaverPropRenderer<>(context, "lifeweaver_tree", (e, s) -> {
			s.lift = (float) LifeweaverStats.TRUNK_BELOW;
			float t = (e.tickCount + s.partialTick) / 20f;
			float left = (e.left() - s.partialTick) / 20f;
			if (left < 5 / 3f) {
				clip(s, e.growth() == TreeOfLifeEntity.GROW_LEANING ? "end_leaning" : "end", 5 / 3f - left, false);
				return;
			}
			switch (e.growth()) {
				case TreeOfLifeEntity.GROW_LEANING -> clip(s, "grow_leaning", t, false);
				case TreeOfLifeEntity.GROW_ENCLOSED -> {
					// 7/30 s, then the second as far as the room lets it (11/30 x (h / 5 + 0.06) s: D 36AA #297)
					float second = Math.min(11 / 30f * (e.room() / 5f + 0.06f), 11 / 30f);
					if (t < 7 / 30f) {
						clip(s, "grow_enclosed", t, false);
					} else {
						clip(s, "grow_enclosed_2", Math.min(t - 7 / 30f, second), false);
					}
				}
				default -> clip(s, "grow", t, false);
			}
			// each pulse (1.75 s apart) plays its layer
			float every = (float) LifeweaverStats.TREE_PULSE_EVERY;
			if (t >= every && e.growth() != TreeOfLifeEntity.GROW_ENCLOSED) {
				s.add = "add_pulse";
				s.addTime = t % every;
			}
		});
	}

	private static void clip(State s, String name, float time, boolean loop) {
		s.clip = name;
		s.time = time;
		s.add = null;
		if (loop) {
			s.time = -time - 1; // negative: wrap it (below)
		}
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.loaded = HeroModels.get(model);
		state.lift = 0;
		state.partialTick = partialTicks;
		poser.accept(entity, state);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		HeroModels.Loaded loaded = state.loaded;
		if (loaded != null) {
			SkinnedModel model = loaded.model();
			Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
			SkinnedModel.Clip clip = state.clip == null ? null : model.clips.get(state.clip);
			if (clip != null && clip.duration() > 0) {
				float t = state.time < 0 ? (-state.time - 1) % clip.duration() : Math.min(state.time, clip.duration());
				Skeleton.sample(clip, t, pose);
			}
			SkinnedModel.Clip add = state.add == null ? null : model.clips.get(state.add);
			if (add != null && add.duration() > 0 && state.addTime < add.duration()) {
				Skeleton.addAdditive(add, state.addTime, pose, 1);
			}
			Matrix4f[] skin = Skeleton.skin(model, pose, -1, null);
			poseStack.pushPose();
			poseStack.translate(0, state.lift, 0);
			// the models face +Z, like the heroes'
			poseStack.rotateDegrees(Axis.YP, -state.yRot);
			HeroModelRenderer.submitModel(loaded, skin, poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, java.util.Set.of(),
				true, false);
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}
}
