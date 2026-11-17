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
		/** The face it's set on (SurfaceStuck: Illari's pylon), or null; its width, to draw a wall's back on the face. */
		net.minecraft.core.@Nullable Direction face;
		float width;
		float height;
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

	/**
	 * Junkrat's RIP-Tire (model 118D8, pet 039C): its roll clip looped (tools/clipmaps/junkrat_tire.txt tire_roll_ra0). Ours: the
	 * same renderer as Lifeweaver's props (a placed model and its clip); drawn turned to his facing.
	 */
	public static LifeweaverPropRenderer<dev.overcraft.entity.RipTireEntity> tire(EntityRendererProvider.Context context) {
		return new LifeweaverPropRenderer<>(context, "junkrat_tire", (e, s) -> {
			clip(s, "tire_roll_ra0", (e.tickCount + s.partialTick) / 20f, true);
			// its fuse, ribbons and sparks (156F) and Nitro Boost's flare (JunkratFx)
			dev.overcraft.client.effect.JunkratFx.tire(e, e.getPosition(s.partialTick), s.partialTick);
		});
	}

	/**
	 * Torbjörn's turret (model 1000C, set 0996: tools/clipmaps/torbjorn_turret.txt): in flight its rest pose, building its build clip
	 * (00DC, 2.5 s), built its level 2 idle with a fire layer on each shot, its upgrade (00DE) then the level 3 idle; turned to its aim.
	 */
	public static LifeweaverPropRenderer<dev.overcraft.entity.TurretEntity> turret(EntityRendererProvider.Context context) {
		return new LifeweaverPropRenderer<>(context, "torbjorn_turret", (e, s) -> {
			float t = e.stateTime(s.partialTick);
			switch (e.state()) {
				case dev.overcraft.entity.TurretEntity.FLYING -> s.clip = null;
				case dev.overcraft.entity.TurretEntity.BUILDING -> clip(s, "build", t, false);
				case dev.overcraft.entity.TurretEntity.UPGRADING -> clip(s, "upgrade", t, false);
				case dev.overcraft.entity.TurretEntity.LEVEL3 -> clip(s, "l3_idle", t, true);
				default -> clip(s, "l2_idle", t, true);
			}
			// its thrown ribbon and damaged smoke (TorbjornFx)
			dev.overcraft.client.effect.TorbjornFx.turret(e, e.getPosition(s.partialTick), e.state() == dev.overcraft.entity.TurretEntity.FLYING);
			float shot = (e.tickCount - e.shotAt() + s.partialTick) / 20f;
			if (e.built() && shot >= 0 && shot < 0.3f) {
				s.add = e.state() == dev.overcraft.entity.TurretEntity.LEVEL3 ? "add_l3_fire" : "add_l2_fire";
				s.addTime = shot;
			}
		});
	}

	/**
	 * Illari's Healing Pylon (model 134D8, set 10FA: tools/clipmaps/illari_pylon.txt): in flight still, set down its build clip,
	 * built its idle looped. Ours: the same renderer as Lifeweaver's props.
	 */
	public static LifeweaverPropRenderer<dev.overcraft.entity.HealingPylonEntity> pylon(EntityRendererProvider.Context context) {
		return new LifeweaverPropRenderer<>(context, "illari_pylon", (e, s) -> {
			if (e.stage() == 0) {
				s.clip = null;
			} else if (e.stage() == 1) {
				clip(s, "build", (e.down() + s.partialTick) / 20f, false);
			} else {
				clip(s, "idle", (e.tickCount + s.partialTick) / 20f, true);
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
		state.face = entity instanceof dev.overcraft.entity.SurfaceStuck stuck ? stuck.stuckFace() : null;
		state.width = entity.getBbWidth();
		state.height = entity.getBbHeight();
		poser.accept(entity, state);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		HeroModels.Loaded loaded = state.loaded;
		if (loaded != null) {
			SkinnedModel model = loaded.model();
			Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
			SkinnedModel.Clip clip = state.clip == null ? null : model.clips.get(state.clip);
			if (clip != null) {
				// a one-key clip is a held pose of length 0 (the turret's l2_idle / l3_idle, which scale away the other level's
				// parts): it's sampled too, at its only key (#146: skipping it drew the rest pose, level 3's pods at level 2)
				float t = clip.duration() <= 0 ? 0 : state.time < 0 ? (-state.time - 1) % clip.duration() : Math.min(state.time, clip.duration());
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
			if (state.face != null && state.face.getAxis().isHorizontal()) {
				// on a wall (#154, the owner: Illari's pylon "on a wall is straight up instead of tangent to the surface"): its base
				// on the face (the entity stands its half width out, half its height down), its up out of the wall. It already
				// faces out (HealingPylonEntity's yaw); a quarter turn about its side takes its up onto its front
				net.minecraft.world.phys.Vec3 n = net.minecraft.world.phys.Vec3.atLowerCornerOf(state.face.getUnitVec3i());
				poseStack.translate(-n.x * state.width / 2, state.height / 2, -n.z * state.width / 2);
				poseStack.rotateDegrees(Axis.YP, -state.yRot);
				poseStack.rotateDegrees(Axis.XP, 90);
			} else {
				poseStack.rotateDegrees(Axis.YP, -state.yRot);
			}
			HeroModelRenderer.submitModel(loaded, skin, poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, java.util.Set.of(),
				true, false);
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}
}
