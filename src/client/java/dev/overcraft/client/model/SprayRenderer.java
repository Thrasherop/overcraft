package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overcraft.client.HeroArt;
import dev.overcraft.client.effect.FxRender;
import dev.overcraft.entity.SprayEntity;
import dev.overcraft.hero.Hero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A spray on its face (#41): the image from the player's own install in the local asset pack, textures/sprays/<hero>/<name>.png,
 * a square on the face. On a wall its top is up; on a floor or a ceiling, toward where the sprayer faced. Without the pack, the
 * hero's portrait stands in. Lit by the world, not full bright (ours).
 */
public class SprayRenderer extends EntityRenderer<SprayEntity, SprayRenderer.State> {
	public static class State extends EntityRenderState {
		@Nullable Identifier texture;
		@Nullable Direction face;
		float yRot;
		/** Width over height of its image (12 of the sprays aren't square: 512 x 256 or 256 x 512, tools/reference/sprays.md). */
		float ratio = 1;
	}

	/** Each image's width over height, read once. */
	private static final java.util.Map<Identifier, Float> RATIOS = new java.util.HashMap<>();

	public static float ratio(Identifier id) {
		return RATIOS.computeIfAbsent(id, k -> {
			var resource = Minecraft.getInstance().getResourceManager().getResource(k);
			if (resource.isEmpty()) {
				return 1f;
			}
			try (var in = resource.get().open(); var image = com.mojang.blaze3d.platform.NativeImage.read(in)) {
				return image.getHeight() == 0 ? 1f : (float) image.getWidth() / image.getHeight();
			} catch (java.io.IOException e) {
				return 1f;
			}
		});
	}

	public SprayRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(SprayEntity entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.face = entity.stuckFace();
		state.yRot = entity.getYRot();
		state.texture = texture(entity.hero(), entity.sprayName());
		state.ratio = state.texture == null ? 1 : ratio(state.texture);
	}

	/** Its image in the pack, else the hero's portrait, else none (nothing drawn). */
	private static @Nullable Identifier texture(String hero, String name) {
		Identifier id = Identifier.fromNamespaceAndPath("overcraft", "textures/sprays/" + hero + "/" + name + ".png");
		if (Minecraft.getInstance().getResourceManager().getResource(id).isPresent()) {
			return id;
		}
		Hero h = Hero.byId(hero);
		return h == null ? null : HeroArt.portrait(h);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.texture == null || state.face == null) {
			return;
		}
		Direction face = state.face;
		Vector3f n = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
		Vector3f up;
		if (face.getAxis().isHorizontal()) {
			up = new Vector3f(0, 1, 0);
		} else {
			// on a floor or a ceiling: its top toward where they faced
			float yaw = (float) Math.toRadians(state.yRot);
			up = new Vector3f(-(float) Math.sin(yaw), 0, (float) Math.cos(yaw));
		}
		Vector3f right = new Vector3f(up).cross(n).normalize();
		// its larger side SIZE, the other kept to the image's ratio
		float hw = SprayEntity.SIZE / 2 * Math.min(1, state.ratio), hh = SprayEntity.SIZE / 2 * Math.min(1, 1 / state.ratio);
		int light = state.lightCoords;
		collector.submitCustomGeometry(poseStack, FxRender.blended(state.texture), (pose, buffer) -> {
			float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
			for (float[] c : corners) {
				float x = right.x * c[0] * hw + up.x * c[1] * hh, y = right.y * c[0] * hw + up.y * c[1] * hh, z = right.z * c[0] * hw + up.z * c[1] * hh;
				buffer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(c[2], c[3]).setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(light).setNormal(pose, n.x, n.y, n.z);
			}
		});
	}
}
