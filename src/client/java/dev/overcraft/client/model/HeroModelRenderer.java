package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overcraft.hero.Hero;
import dev.overcraft.client.ClientHero;
import dev.overcraft.hero.HeroManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import dev.overcraft.hero.HeroWeaponItem;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Draws a hero's own model in place of the Minecraft player model, for a player in hero mode (holding the hero's
 * weapon: other clients see that too) when the local asset pack has the model. Skinned on the CPU each frame.
 */
public final class HeroModelRenderer {
	/** What to draw this frame, worked out while extracting the render state. */
	public record Draw(HeroModels.Loaded loaded, Matrix4f[] skin, float climbShift) {
	}

	public static final RenderStateDataKey<Draw> DRAW = RenderStateDataKey.create(() -> "overcraft:hero_draw");
	/** The hero each entity was last drawn as (by entity id), while it's in hero mode or dying. */
	private static final Map<Integer, Hero> LAST_HERO = new HashMap<>();

	private HeroModelRenderer() {
	}

	/** Forgets who was which hero (leaving a world). */
	public static void clear() {
		LAST_HERO.clear();
	}

	/** While extracting a player's render state: a hero in hero mode with a model gets a {@link Draw}. */
	public static void extract(LivingEntity entity, LivingEntityRenderState state, float partialTicks) {
		state.setData(DRAW, null);
		Hero hero;
		String chosen = entity.getAttached(HeroManager.HERO);
		if (entity.getMainHandItem().getItem() instanceof HeroWeaponItem weapon) {
			hero = weapon.hero;
			LAST_HERO.put(entity.getId(), hero);
		} else if (chosen != null && Hero.byId(chosen) != null) {
			// out of hero mode (Minecraft on slots 2-9) a player still looks like their hero (owner)
			hero = Hero.byId(chosen);
			LAST_HERO.put(entity.getId(), hero);
		} else if (entity.isDeadOrDying() && LAST_HERO.containsKey(entity.getId())) {
			// the weapon is gone the moment a hero dies (Overcraft removes it): keep the hero for the death animation
			hero = LAST_HERO.get(entity.getId());
		} else {
			LAST_HERO.remove(entity.getId());
			return;
		}
		HeroModels.Loaded loaded = HeroModels.get(hero);
		if (loaded == null) {
			return;
		}
		float now = (entity.level().getGameTime() + partialTicks) / 20f;
		HeroAnimator animator = HeroAnimator.of(entity);
		Matrix4f[] skin = animator.pose(loaded.model(), entity, now, state.yRot, state.xRot);
		state.setData(DRAW, new Draw(loaded, skin, animator.climbShift()));
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
		poseStack.translate(0, 0, -draw.climbShift());
		float scale = state.scale;
		poseStack.scale(scale, scale, scale);
		submitModel(draw.loaded(), draw.skin(), poseStack, collector, light, overlay);
		poseStack.popPose();
		return true;
	}

	/**
	 * First person in hero mode: the hero's view model (hero_models/<hero>_1p.glb), its Camera bone put at the eye. The
	 * pose stack is in view space (looking down -Z). False (Minecraft draws its hand) without the model or out of hero
	 * mode.
	 */
	public static boolean submitFirstPerson(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, int light) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		Hero hero = ClientHero.hero();
		if (player == null || hero == null || !ClientHero.controls()) {
			return false;
		}
		HeroModels.Loaded loaded = HeroModels.firstPerson(hero);
		if (loaded == null) {
			return false;
		}
		float now = (player.level().getGameTime() + partialTicks) / 20f;
		Vector3f eye = new Vector3f();
		Matrix4f[] skin = HeroAnimator.of(player).poseFirstPerson(loaded.model(), player, now, eye);
		poseStack.pushPose();
		// the model faces +Z, the view looks down -Z: turn it round, then put its eye at the camera
		poseStack.rotateDegrees(Axis.YP, 180);
		poseStack.translate(-eye.x, -eye.y, -eye.z);
		submitModel(loaded, skin, poseStack, collector, light, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
		return true;
	}

	/** Submits a skinned model's pieces (hero models, Kitsune Rush's fox), held props not in use left out. */
	public static void submitModel(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		int light, int overlay) {
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			Identifier texture = piece.image() >= 0 ? loaded.textures()[piece.image()] : null;
			if (texture == null || stowed(piece, skin)) {
				continue;
			}
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
				(pose, buffer) -> emit(piece, skin, pose, buffer, light, overlay));
		}
	}

	/**
	 * A held prop the current clips don't use: Overwatch models carry their weapons (Kiriko's kunai, ofuda, Suzu) as
	 * small pieces on their own bones, parked at the model's origin (between the feet) unless an animation puts them
	 * in a hand. Small and still at the origin after skinning = not in use, so not drawn.
	 */
	private static boolean stowed(SkinnedModel.Piece piece, Matrix4f[] skin) {
		Prop prop = PROPS.get(piece);
		if (prop == null) {
			if (PROPS.containsKey(piece)) {
				return false;
			}
			prop = Prop.of(piece);
			PROPS.put(piece, prop);
			if (prop == null) {
				return false;
			}
		}
		Vector3f at = skin[prop.joint()].transformPosition(new Vector3f(prop.center()));
		return at.length() < STOWED_RADIUS;
	}

	/** A small piece's rest centre and the joint that moves it most. */
	private record Prop(Vector3f center, int joint) {
		static @Nullable Prop of(SkinnedModel.Piece piece) {
			int n = piece.vertexCount();
			float[] pos = piece.pos();
			Vector3f lo = new Vector3f(Float.MAX_VALUE), hi = new Vector3f(-Float.MAX_VALUE);
			for (int v = 0; v < n; v++) {
				lo.min(new Vector3f(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2]));
				hi.max(new Vector3f(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2]));
			}
			Vector3f center = new Vector3f(lo).add(hi).mul(0.5f);
			if (hi.distance(lo) > PROP_SIZE || center.length() > STOWED_RADIUS) {
				return null;
			}
			int joint = piece.joint4()[0];
			float best = -1;
			for (int k = 0; k < 4; k++) {
				if (piece.weight4()[k] > best) {
					best = piece.weight4()[k];
					joint = piece.joint4()[k];
				}
			}
			return new Prop(center, joint);
		}
	}

	/** Pieces at most this big (metres, corner to corner) can be held props. */
	private static final float PROP_SIZE = 0.6f;
	/** A prop whose centre is within this of the model's origin (metres) is parked, not held. */
	private static final float STOWED_RADIUS = 0.3f;
	private static final Map<SkinnedModel.Piece, @Nullable Prop> PROPS = new IdentityHashMap<>();

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
