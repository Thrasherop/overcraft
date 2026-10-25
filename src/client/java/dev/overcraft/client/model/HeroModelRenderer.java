package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroWeaponItem;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

/**
 * Draws a hero's own model in place of the Minecraft player model, for a player in hero mode (holding the hero's
 * weapon: other clients see that too) when the local asset pack has the model. Skinned on the CPU each frame.
 */
public final class HeroModelRenderer {
	/** What to draw this frame, worked out while extracting the render state. */
	public record Draw(HeroModels.Loaded loaded, Matrix4f[] skin) {
	}

	public static final RenderStateDataKey<Draw> DRAW = RenderStateDataKey.create(() -> "overcraft:hero_draw");

	private HeroModelRenderer() {
	}

	/** While extracting a player's render state: a hero in hero mode with a model gets a {@link Draw}. */
	public static void extract(LivingEntity entity, LivingEntityRenderState state, float partialTicks) {
		state.setData(DRAW, null);
		if (!(entity.getMainHandItem().getItem() instanceof HeroWeaponItem weapon)) {
			return;
		}
		Hero hero = weapon.hero;
		HeroModels.Loaded loaded = HeroModels.get(hero);
		if (loaded == null) {
			return;
		}
		float now = (entity.level().getGameTime() + partialTicks) / 20f;
		Matrix4f[] skin = HeroAnimator.of(entity).pose(loaded.model(), entity, now, state.yRot, state.xRot);
		state.setData(DRAW, new Draw(loaded, skin));
	}

	/** In place of LivingEntityRenderer.submit: false when there's nothing of ours to draw. */
	public static boolean submit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		Draw draw = state.getData(DRAW);
		if (draw == null || state.isInvisible) {
			return false;
		}
		int light = state.lightCoords;
		int overlay = LivingEntityRenderer.getOverlayCoords(state, 0);
		poseStack.pushPose();
		// the model faces +Z; Minecraft's yaw 0 faces +Z (south) and turns the other way
		poseStack.rotateDegrees(Axis.YP, -state.bodyRot);
		float scale = state.scale;
		poseStack.scale(scale, scale, scale);
		SkinnedModel model = draw.loaded().model();
		for (SkinnedModel.Piece piece : model.pieces) {
			Identifier texture = piece.image() >= 0 ? draw.loaded().textures()[piece.image()] : null;
			if (texture == null) {
				continue;
			}
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
				(pose, buffer) -> emit(piece, draw.skin(), pose, buffer, light, overlay));
		}
		poseStack.popPose();
		return true;
	}

	/** Skins the piece's vertices and writes its triangles (as quads with the last corner doubled). */
	private static void emit(SkinnedModel.Piece piece, Matrix4f[] skin, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay) {
		int n = piece.vertexCount();
		float[] p = new float[n * 3];
		float[] nm = new float[n * 3];
		float[] pos = piece.pos();
		float[] normal = piece.normal();
		for (int v = 0; v < n; v++) {
			float px = pos[v * 3], py = pos[v * 3 + 1], pz = pos[v * 3 + 2];
			float nx = normal[v * 3], ny = normal[v * 3 + 1], nz = normal[v * 3 + 2];
			float ox = 0, oy = 0, oz = 0, qx = 0, qy = 0, qz = 0;
			for (int k = 0; k < 4; k++) {
				float w = piece.weight4()[v * 4 + k];
				if (w <= 0) {
					continue;
				}
				Matrix4f m = skin[piece.joint4()[v * 4 + k]];
				ox += w * (m.m00() * px + m.m10() * py + m.m20() * pz + m.m30());
				oy += w * (m.m01() * px + m.m11() * py + m.m21() * pz + m.m31());
				oz += w * (m.m02() * px + m.m12() * py + m.m22() * pz + m.m32());
				qx += w * (m.m00() * nx + m.m10() * ny + m.m20() * nz);
				qy += w * (m.m01() * nx + m.m11() * ny + m.m21() * nz);
				qz += w * (m.m02() * nx + m.m12() * ny + m.m22() * nz);
			}
			float len = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
			if (len > 1.0E-6f) {
				qx /= len;
				qy /= len;
				qz /= len;
			}
			p[v * 3] = ox;
			p[v * 3 + 1] = oy;
			p[v * 3 + 2] = oz;
			nm[v * 3] = qx;
			nm[v * 3 + 1] = qy;
			nm[v * 3 + 2] = qz;
		}
		int[] idx = piece.indices();
		float[] uv = piece.uv();
		for (int t = 0; t + 2 < idx.length; t += 3) {
			vertex(buffer, pose, p, nm, uv, idx[t], light, overlay);
			vertex(buffer, pose, p, nm, uv, idx[t + 1], light, overlay);
			vertex(buffer, pose, p, nm, uv, idx[t + 2], light, overlay);
			vertex(buffer, pose, p, nm, uv, idx[t + 2], light, overlay);
		}
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float[] p, float[] n, float[] uv, int i, int light, int overlay) {
		buffer.addVertex(pose, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
			.setColor(-1)
			.setUv(uv[i * 2], uv[i * 2 + 1])
			.setOverlay(overlay)
			.setLight(light)
			.setNormal(pose, n[i * 3], n[i * 3 + 1], n[i * 3 + 2]);
	}
}
