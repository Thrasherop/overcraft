package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overcraft.hero.Hero;
import dev.overcraft.client.ClientHero;
import dev.overcraft.client.hud.HudShapes;
import dev.overcraft.client.effect.FxRender;
import dev.overcraft.client.effect.Cleansed;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Draws a hero's own model in place of the Minecraft player model, for a player in hero mode (holding the hero's
 * weapon: other clients see that too) when the local asset pack has the model. Skinned on the CPU each frame.
 */
public final class HeroModelRenderer {
	/** What to draw this frame, worked out while extracting the render state. */
	public record Draw(HeroModels.Loaded loaded, Matrix4f[] skin, float climbShift, Set<Integer> hidden) {
	}

	public static final RenderStateDataKey<Draw> DRAW = RenderStateDataKey.create(() -> "overcraft:hero_draw");
	/** How white an entity flashes (Protection Suzu's cleanse: client/effect/Cleansed). */
	/** Packed light for full brightness (block and sky light 15). */
	private static final int FULL_BRIGHT = 0xF000F0;
	public static final RenderStateDataKey<Float> WHITE = RenderStateDataKey.create(() -> "overcraft:white");
	/** The hero each entity was last drawn as (by entity id), while it's in hero mode or dying. */
	private static final Map<Integer, Hero> LAST_HERO = new HashMap<>();

	private HeroModelRenderer() {
	}

	/** The hero the select screen shows the local player as, while it draws them; null otherwise. */
	private static @Nullable Hero preview;

	/** The select screen draws the player as this hero (null: as they are). */
	public static void preview(@Nullable Hero hero) {
		preview = hero;
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
		if (preview != null && entity == Minecraft.getInstance().player) {
			hero = preview;
		} else if (entity.getMainHandItem().getItem() instanceof HeroWeaponItem weapon) {
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
		// the body faces her aim (HeroAnimator.facing); the head's turn is then measured from there
		float facing = animator.facing(entity, partialTicks);
		state.yRot = Mth.wrapDegrees(state.yRot + state.bodyRot - facing);
		state.bodyRot = facing;
		Matrix4f[] skin = animator.pose(loaded.model(), entity, now, state.yRot, state.xRot);
		state.setData(DRAW, new Draw(loaded, skin, animator.climbShift(), hiddenProps(loaded.model(), animator.action())));
	}

	private static float white(LivingEntityRenderState state) {
		Float white = state.getData(WHITE);
		return white == null ? 0 : white;
	}

	/** In place of LivingEntityRenderer.submit: false when there's nothing of ours to draw. */
	public static boolean submit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		Draw draw = state.getData(DRAW);
		if (draw == null || state.isInvisible) {
			return false;
		}
		int light = state.lightCoords;
		int overlay = LivingEntityRenderer.getOverlayCoords(state, white(state));
		poseStack.pushPose();
		// the model faces +Z; Minecraft's yaw 0 faces +Z (south) and turns the other way
		poseStack.rotateDegrees(Axis.YP, -state.bodyRot);
		poseStack.translate(0, 0, -draw.climbShift());
		float scale = state.scale;
		poseStack.scale(scale, scale, scale);
		submitModel(draw.loaded(), draw.skin(), poseStack, collector, light, overlay, draw.hidden());
		submitGlow(draw.loaded(), draw.skin(), poseStack, collector, draw.hidden(), white(state));
		poseStack.popPose();
		return true;
	}

	/**
	 * First person in hero mode: the hero's view model (hero_models/<hero>_1p.glb), its Camera bone put at the eye. The
	 * pose stack is in view space (looking down -Z). False (Minecraft draws its hand) without the model or out of hero
	 * mode.
	 */
	public static boolean submitFirstPerson(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, int light,
											float swayPitch, float swayYaw) {
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
		Matrix4f view = new Matrix4f();
		Matrix4f[] skin = HeroAnimator.of(player).poseFirstPerson(loaded.model(), player, now, view);
		poseStack.pushPose();
		// the arms lag a little behind the view when turning (Minecraft's hand sway, a bit stronger: Overwatch's arms
		// swing visibly)
		poseStack.rotateDegrees(Axis.XP, swayPitch * ARM_SWAY);
		poseStack.rotateDegrees(Axis.YP, swayYaw * ARM_SWAY);
		// the camera looks down +Z in the model's space and down -Z in the view's: turn it round, then the model as
		// its Camera bone sees it
		poseStack.rotateDegrees(Axis.YP, 180);
		poseStack.mulPose(view);
		// her own arms flash white when her Suzu cleanses her
		float white = Cleansed.white(player.getId());
		int overlay = white > 0 ? OverlayTexture.pack(white, false) : OverlayTexture.NO_OVERLAY;
		submitModel(loaded, skin, poseStack, collector, light, overlay);
		submitGlow(loaded, skin, poseStack, collector, Set.of(), white);
		poseStack.popPose();
		return true;
	}

	/**
	 * Props shown only during one action: Kiriko's locomotion clips (run, strafe, jump, fall) carry the Suzu in her hand,
	 * which the game hides until she throws it (the props survey: those clips move its bone, bone_006B, to the hand).
	 * Bone name -> the action (prefix) that shows it.
	 */
	private static final Map<String, String> PROP_ACTIONS = Map.of("bone_006B", "throw_suzu");

	/** The joints of props to hide this frame (their action isn't playing). */
	private static Set<Integer> hiddenProps(SkinnedModel model, @Nullable String action) {
		Set<Integer> hidden = new HashSet<>();
		for (int j = 0; j < model.joints.length; j++) {
			String shownBy = PROP_ACTIONS.get(model.nodeNames[model.joints[j]]);
			if (shownBy != null && (action == null || !action.startsWith(shownBy))) {
				hidden.add(j);
			}
		}
		return hidden;
	}

	/** Submits a skinned model's pieces (hero models, Kitsune Rush's fox), held props not in use left out. */
	public static void submitModel(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		int light, int overlay) {
		submitModel(loaded, skin, poseStack, collector, light, overlay, Set.of());
	}

	/** ... with the props on these joints hidden. */
	public static void submitModel(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		int light, int overlay, Set<Integer> hidden) {
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			Identifier texture = piece.image() >= 0 ? loaded.textures()[piece.image()] : null;
			if (texture == null) {
				continue;
			}
			boolean prop = PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (prop && !hidden.isEmpty() && piece.vertexCount() > 0 && hidden.contains(piece.joint4()[0])) {
				continue;
			}
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
				(pose, buffer) -> emit(piece, skin, pose, buffer, light, overlay, prop));
		}
	}

	/**
	 * The model again as pure added light, white-cyan at this strength (0 to 1): Protection Suzu's cleanse, which in
	 * Overwatch turns them a glowing white (the second critique: the overlay alone only paled her, a ghost).
	 */
	public static void submitGlow(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		Set<Integer> hidden, float strength) {
		if (strength <= 0) {
			return;
		}
		HudShapes.ensure();
		int color = Math.clamp(Math.round(strength * 0.8f * 255), 0, 255) << 24 | 0xE6FFFF;
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			if (piece.image() < 0 || loaded.textures()[piece.image()] == null) {
				continue;
			}
			boolean prop = PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (prop && !hidden.isEmpty() && piece.vertexCount() > 0 && hidden.contains(piece.joint4()[0])) {
				continue;
			}
			collector.submitCustomGeometry(poseStack, FxRender.additive(HudShapes.WHITE),
				(pose, buffer) -> emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, prop, color));
		}
	}

	/**
	 * Held props: Overwatch models carry their weapons (Kiriko's three kunai, ofuda, Suzu) as small pieces on their own
	 * bones, parked at the model's origin (between the feet) unless an animation puts them in a hand. One piece can hold
	 * several (the three kunai), so a prop piece's triangles still at the origin after skinning aren't drawn.
	 */
	private static boolean isProp(SkinnedModel.Piece piece) {
		float[] pos = piece.pos();
		Vector3f lo = new Vector3f(Float.MAX_VALUE), hi = new Vector3f(-Float.MAX_VALUE);
		for (int v = 0; v < piece.vertexCount(); v++) {
			lo.min(new Vector3f(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2]));
			hi.max(new Vector3f(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2]));
		}
		Vector3f center = new Vector3f(lo).add(hi).mul(0.5f);
		return hi.distance(lo) <= PROP_SIZE && center.length() <= STOWED_RADIUS;
	}

	/** How much of the view's lag the arms follow (Minecraft's hand uses 0.1: ours, by eye against gameplay footage). */
	private static final float ARM_SWAY = 0.15f;
	/** Pieces at most this big (metres, corner to corner) can be held props. */
	private static final float PROP_SIZE = 0.6f;
	/** A prop triangle within this of the model's origin (metres) is parked, not held. */
	private static final float STOWED_RADIUS = 0.3f;
	private static final Map<SkinnedModel.Piece, Boolean> PROPS = new IdentityHashMap<>();

	/** Skins the piece's vertices and writes its triangles (as quads with the last corner doubled). */
	private static void emit(SkinnedModel.Piece piece, Matrix4f[] skin, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
							 boolean prop) {
		emit(piece, skin, pose, buffer, light, overlay, prop, -1);
	}

	private static void emit(SkinnedModel.Piece piece, Matrix4f[] skin, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
							 boolean prop, int color) {
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
		float parked = STOWED_RADIUS * STOWED_RADIUS;
		for (int t = 0; t + 2 < idx.length; t += 3) {
			if (prop && near(p, idx[t], parked) && near(p, idx[t + 1], parked) && near(p, idx[t + 2], parked)) {
				continue;
			}
			vertex(buffer, pose, p, nm, uv, idx[t], light, overlay, color);
			vertex(buffer, pose, p, nm, uv, idx[t + 1], light, overlay, color);
			vertex(buffer, pose, p, nm, uv, idx[t + 2], light, overlay, color);
			vertex(buffer, pose, p, nm, uv, idx[t + 2], light, overlay, color);
		}
	}

	private static boolean near(float[] p, int i, float radiusSq) {
		return p[i * 3] * p[i * 3] + p[i * 3 + 1] * p[i * 3 + 1] + p[i * 3 + 2] * p[i * 3 + 2] < radiusSq;
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float[] p, float[] n, float[] uv, int i, int light, int overlay,
							   int color) {
		buffer.addVertex(pose, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
			.setColor(color)
			.setUv(uv[i * 2], uv[i * 2 + 1])
			.setOverlay(overlay)
			.setLight(light)
			.setNormal(pose, n[i * 3], n[i * 3 + 1], n[i * 3 + 2]);
	}
}
