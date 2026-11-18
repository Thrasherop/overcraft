package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overcraft.entity.enemy.EnemyHeroEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/**
 * An enemy hero (#46): drawn as its hero with the player's asset pack, exactly as a player hero is (HeroModelRenderer.extract reads
 * the synced HeroManager.HERO the mob carries; LivingEntityRendererMixin then draws the hero model in place of this renderer's).
 * Without the pack (or before its model loads), a plain humanoid stand-in in Minecraft's default skin (ours, as every hero model
 * falls back). Its name, "TRACER" in red, shows over it within 24 m (ours).
 */
public class EnemyHeroRenderer extends MobRenderer<EnemyHeroEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	/** Minecraft's own default skin, for the stand-in (nothing of Overwatch's). */
	private static final Identifier STAND_IN = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");
	/** How near its name shows (ours). */
	private static final double NAME_RANGE = 24;

	public EnemyHeroRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return STAND_IN;
	}

	@Override
	public void extractRenderState(EnemyHeroEntity entity, HumanoidRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, itemModelResolver);
		HeroModelRenderer.extract(entity, state, partialTicks);
	}

	@Override
	protected boolean shouldShowName(EnemyHeroEntity entity, double distanceToCameraSq) {
		return distanceToCameraSq < NAME_RANGE * NAME_RANGE && entity.isAlive() && super.shouldShowName(entity, distanceToCameraSq);
	}

	@Override
	public void submit(HumanoidRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.getData(HeroModelRenderer.DRAW) != null) {
			// the hero model was drawn instead (LivingEntityRendererMixin cancels the rest of submit, the name with it)
			submitNameDisplay(state, poseStack, collector, camera);
		}
	}
}
