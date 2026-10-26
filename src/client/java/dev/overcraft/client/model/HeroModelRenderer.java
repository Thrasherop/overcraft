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
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.core.Direction;
import net.minecraft.client.Camera;
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
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
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
	/** {@code cull}: back faces left out before upload (not for the select screen's GUI view, whose camera isn't at the origin). */
	public record Draw(HeroModels.Loaded loaded, Matrix4f[] skin, float climbShift, Set<Integer> hidden, boolean cull) {
	}

	public static final RenderStateDataKey<Draw> DRAW = RenderStateDataKey.create(() -> "overcraft:hero_draw");
	/** Prone (crawling, swimming), how far back along her body she's drawn and how far off the floor (metres: ours). */
	private static final float PRONE_BACK = 0.95f;
	private static final float PRONE_LIFT = 0.25f;
	/** Lying in bed, how far she's lifted so her back rests on the mattress rather than in it (metres: ours). */
	private static final float SLEEP_LIFT = 0.12f;
	/** Packed light for full brightness (block and sky light 15). */
	private static final int FULL_BRIGHT = 0xF000F0;
	/**
	 * The view model's least light, as block light (0-15). Overwatch's arms stay readable on night maps; at Minecraft's
	 * night light they went nearly black and purple, a quarter of the screen merging into dark grass (the fourth
	 * critique). Ours.
	 */
	private static final int VIEW_MODEL_MIN_LIGHT = 11;
	/** How white an entity flashes (Protection Suzu's cleanse: client/effect/Cleansed). */
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
		long t0 = System.nanoTime();
		Matrix4f[] skin = animator.pose(loaded.model(), entity, now, state.yRot, state.xRot);
		Timing.pose += System.nanoTime() - t0;
		state.setData(DRAW, new Draw(loaded, skin, animator.climbShift(), hiddenProps(loaded.model(), animator.action()),
			preview == null));
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
		int overlay = LivingEntityRenderer.getOverlayCoords(state, white(state) * 0.6f);
		poseStack.pushPose();
		Direction bed = state.hasPose(Pose.SLEEPING) ? state.bedOrientation : null;
		if (bed != null) {
			// asleep: on her back, head on the pillow, feet toward the bed's foot, as Minecraft lays a sleeping player
			// (LivingEntityRenderer: the origin moved eyeHeight - 0.1 toward the foot). She stood upright on the bed
			// (found playing)
			float headOffset = state.eyeHeight - 0.1f;
			poseStack.translate(-bed.getStepX() * headOffset, SLEEP_LIFT, -bed.getStepZ() * headOffset);
			// her up (+Y) toward the pillow, her front (+Z) to the sky, her side (+X = Y x Z) across the bed
			Vector3f head = new Vector3f(bed.getStepX(), 0, bed.getStepZ());
			Vector3f side = new Vector3f(head).cross(0, 1, 0);
			poseStack.mulPose(new Matrix4f().set(new Matrix3f(side, head, new Vector3f(0, 1, 0))));
		} else {
			// the model faces +Z; Minecraft's yaw 0 faces +Z (south) and turns the other way
			poseStack.rotateDegrees(Axis.YP, -state.bodyRot);
			poseStack.translate(0, 0, -draw.climbShift());
			if (state instanceof AvatarRenderState avatar) {
				// gliding, swimming, crawling: pitched forward about her feet as Minecraft pitches a player
				// (AvatarRenderer.setupRotations); upright, she crawled standing through a one-high gap, the camera in
				// her legs, and swam and flew standing (found playing)
				float pitch = 0;
				if (avatar.isFallFlying) {
					pitch = avatar.fallFlyingScale() * (90 + avatar.xRot);
				} else if (avatar.swimAmount > 0) {
					pitch = Mth.lerp(avatar.swimAmount, 0, avatar.isInWater ? 90 + avatar.xRot : 90);
				}
				if (pitch != 0) {
					poseStack.rotateDegrees(Axis.XP, pitch);
					if (avatar.isVisuallySwimming) {
						// lying over her box, not ahead of it: back along her body, lifted off the floor
						poseStack.translate(0, -PRONE_BACK, -PRONE_LIFT);
					}
				}
			}
		}
		float scale = state.scale;
		poseStack.scale(scale, scale, scale);
		submitModel(draw.loaded(), draw.skin(), poseStack, collector, light, overlay, draw.hidden(), draw.cull());
		submitGlow(draw.loaded(), draw.skin(), poseStack, collector, draw.hidden(), white(state), draw.cull());
		poseStack.popPose();
		return true;
	}

	/**
	 * The first-person ofuda hand's index fingertip from the camera, in the world's axes (the arms are drawn in the
	 * world's camera-relative space, turned with the view), from the last frame drawn, and when (Util.getMillis).
	 */
	private static final Vector3f VIEW_FINGERTIP = new Vector3f();
	private static long viewFingertipAt = -1;

	public static Vector3f viewFingertip() {
		return new Vector3f(VIEW_FINGERTIP);
	}

	/** The arms were drawn just now (not hidden: F1, another view), so the fingertip is where they are. */
	public static boolean viewFingertipFresh() {
		return viewFingertipAt >= 0 && Util.getMillis() - viewFingertipAt < 200;
	}

	/**
	 * First person in hero mode: the hero's view model (hero_models/<hero>_1p.glb), its Camera bone put at the eye. The
	 * pose stack starts as the inverse of the view's turn (GameRenderer.renderItemInHand): camera-relative, in the
	 * world's axes. False (Minecraft draws its hand) without the model or out of hero mode.
	 */
	public static boolean submitFirstPerson(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, int light,
											float swayPitch, float swayYaw) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		Hero hero = ClientHero.hero();
		// heroView, not controls: her arms stay while she dies (Minecraft's blocky arm took their place on the death screen)
		if (player == null || hero == null || !ClientHero.heroView()) {
			return false;
		}
		HeroModels.Loaded loaded = HeroModels.firstPerson(hero);
		if (loaded == null) {
			return false;
		}
		float now = (player.level().getGameTime() + partialTicks) / 20f;
		Matrix4f view = new Matrix4f();
		HeroAnimator animator = HeroAnimator.of(player);
		Matrix4f[] skin = animator.poseFirstPerson(loaded.model(), player, now, view);
		poseStack.pushPose();
		// (the stack acts in view space from here: x right, y up, -z ahead)
		// a little behind the movement, as Overwatch's arms drift: aside when strafing, up when falling (#14)
		drift(player, partialTicks);
		poseStack.translate(driftX, driftY, 0);
		// 10% bigger on screen than the model's own (the owner's playtest, #14): scaled across the view, not along it
		poseStack.scale(ARM_SCALE, ARM_SCALE, 1);
		// the arms lag a little behind the view when turning (Minecraft's hand sway, a bit stronger: Overwatch's arms
		// swing visibly)
		poseStack.rotateDegrees(Axis.XP, swayPitch * ARM_SWAY);
		poseStack.rotateDegrees(Axis.YP, swayYaw * ARM_SWAY);
		// the camera looks down +Z in the model's space and down -Z in the view's: turn it round, then the model as
		// its Camera bone sees it
		poseStack.rotateDegrees(Axis.YP, 180);
		poseStack.mulPose(view);
		// where her ofuda hand's fingertip is in the view: the throw's puff pops there
		poseStack.last().pose().transformPosition(animator.fingertip, VIEW_FINGERTIP);
		viewFingertipAt = Util.getMillis();
		// her own arms flash white when her Suzu cleanses her
		float white = Cleansed.white(player.getId());
		int overlay = white > 0 ? OverlayTexture.pack(white * 0.6f, false) : OverlayTexture.NO_OVERLAY;
		int lit = Math.max(light & 0xFFFF, VIEW_MODEL_MIN_LIGHT << 4) | light & 0xFFFF0000;
		submitModel(loaded, skin, poseStack, collector, lit, overlay);
		submitGlow(loaded, skin, poseStack, collector, Set.of(), white, true);
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

	/** Dev timing: nanoseconds spent posing and emitting hero models since the last report (DevServer "timing"). */
	public static final class Timing {
		public static long pose;
		public static long emit;
		/** Dev: draw only every this many triangles (1 = all), to see what the vertex count costs. */
		public static int stride = 1;
		/** Dev: CPU back-face culling on (compare pictures with it off). */
		public static boolean cull = true;
	}

	/** Submits a skinned model's pieces (hero models, Kitsune Rush's fox), held props not in use left out. */
	public static void submitModel(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		int light, int overlay) {
		submitModel(loaded, skin, poseStack, collector, light, overlay, Set.of(), true);
	}

	/** ... with the props on these joints hidden. */
	public static void submitModel(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		int light, int overlay, Set<Integer> hidden, boolean cull) {
		submitModel(loaded, skin, poseStack, collector, light, overlay, hidden, cull, true);
	}

	/**
	 * ... and, with {@code props} false, no hiding of stowed props: a projectile's model is small and centred on its
	 * origin, so as a hero's prop every triangle counted as parked and none was drawn (the fifth critique: no paper on
	 * the ofuda; no kunai or Suzu model either).
	 */
	public static void submitModel(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		int light, int overlay, Set<Integer> hidden, boolean cull, boolean props) {
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			Identifier texture = piece.image() >= 0 ? loaded.textures()[piece.image()] : null;
			if (texture == null || isEyeShadow(loaded, piece)) {
				continue;
			}
			boolean prop = props && PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (prop && !hidden.isEmpty() && piece.vertexCount() > 0 && hidden.contains(piece.joint4()[0])) {
				continue;
			}
			collector.submitCustomGeometry(poseStack, FxRender.heroCutout(texture), (pose, buffer) -> {
				long t0 = System.nanoTime();
				emit(piece, skin, pose, buffer, light, overlay, prop, -1, cull, true);
				Timing.emit += System.nanoTime() - t0;
			});
		}
	}

	/**
	 * A spirit: the model see-through and glowing (Kitsune Rush's fox: Overwatch's is a translucent ice-blue spirit; drawn
	 * solid it read as a plush toy, the third critique), its texture at this opacity, full bright, with a cyan glow over it.
	 */
	public static void submitSpirit(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		float opacity, float glow, int rgb, int glowRgb, boolean added) {
		int color = Math.clamp(Math.round(opacity * 255), 0, 255) << 24 | rgb;
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			Identifier texture = piece.image() >= 0 ? loaded.textures()[piece.image()] : null;
			if (texture == null) {
				continue;
			}
			// added: its light and the glow's sum whatever order they're drawn in (blended, it was drawn over its own glow
			// and hid most of it: a matte blue fox, the seventh critique)
			collector.submitCustomGeometry(poseStack, added ? FxRender.additive(texture) : RenderTypes.entityTranslucentEmissive(texture),
				(pose, buffer) -> emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false, color, true, false));
		}
		submitGlow(loaded, skin, poseStack, collector, Set.of(), glow, true, glowRgb);
	}

	/**
	 * The model again as pure added light, white-cyan at this strength (0 to 1): Protection Suzu's cleanse, which in
	 * Overwatch turns them a glowing white (the second critique: the overlay alone only paled her, a ghost).
	 */
	public static void submitGlow(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		Set<Integer> hidden, float strength, boolean cull) {
		submitGlow(loaded, skin, poseStack, collector, hidden, strength, cull, 0xCFF8FF);
	}

	/** The same in this colour. */
	public static void submitGlow(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		Set<Integer> hidden, float strength, boolean cull, int rgb) {
		if (strength <= 0) {
			return;
		}
		HudShapes.ensure();
		// below clipping, so her shape still reads (at full white she was a flat cut-out, the third critique)
		int color = Math.clamp(Math.round(strength * 0.45f * 255), 0, 255) << 24 | rgb;
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			if (piece.image() < 0 || loaded.textures()[piece.image()] == null || isEyeShadow(loaded, piece)) {
				continue;
			}
			boolean prop = PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (prop && !hidden.isEmpty() && piece.vertexCount() > 0 && hidden.contains(piece.joint4()[0])) {
				continue;
			}
			collector.submitCustomGeometry(poseStack, FxRender.additive(HudShapes.WHITE),
				(pose, buffer) -> emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, prop, color, cull, false));
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
	/** The first-person arms' size on screen, against the model's (the owner: "try making them 10% bigger", #14). */
	private static final float ARM_SCALE = 1.1f;
	/**
	 * How far the arms drift behind the movement (metres per m/s, ours), its limit, and how fast it follows (s): about
	 * 9 mm aside at a 5.5 m/s strafe (1.2 cm moved the arms 42 px: more than "slightly"), up to 2.5 cm falling;
	 * Overwatch's is subtle (#14).
	 */
	private static final float DRIFT_SIDE = 0.0016f;
	private static final float DRIFT_UP = 0.0015f;
	private static final float DRIFT_MAX = 0.025f;
	private static final float DRIFT_TIME = 0.1f;
	private static float driftX;
	private static float driftY;
	private static long driftAt = -1;

	/** Moves the arms' drift toward the player's velocity in view space, against it. */
	private static void drift(LocalPlayer player, float partialTicks) {
		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
		Vec3 v = player.getDeltaMovement().scale(20);
		Vector3fc left = camera.leftVector();
		Vector3fc up = camera.upVector();
		float right = -(float) (v.x * left.x() + v.y * left.y() + v.z * left.z());
		float rise = (float) (v.x * up.x() + v.y * up.y() + v.z * up.z());
		float targetX = Math.clamp(-right * DRIFT_SIDE, -DRIFT_MAX, DRIFT_MAX);
		float targetY = Math.clamp(-rise * DRIFT_UP, -DRIFT_MAX, DRIFT_MAX);
		long now = Util.getMillis();
		float dt = driftAt < 0 ? 1 : Math.min(1, (now - driftAt) / 1000f);
		driftAt = now;
		float k = 1 - (float) Math.exp(-dt / DRIFT_TIME);
		driftX += (targetX - driftX) * k;
		driftY += (targetY - driftY) * k;
	}
	/** Pieces at most this big (metres, corner to corner) can be held props. */
	private static final float PROP_SIZE = 0.6f;
	/** A prop triangle within this of the model's origin (metres) is parked, not held. */
	private static final float STOWED_RADIUS = 0.3f;
	private static final Map<SkinnedModel.Piece, Boolean> PROPS = new IdentityHashMap<>();

	/**
	 * Overwatch's eye shadow: a shell over the eyes, skinned to the eyelids, in one flat near-black (Kiriko's: 32 x 32 of
	 * #0E0E0E). The game's shader fades it softly; drawn as it is, it laid black slabs over her eyes ("she looks dead
	 * inside", the owner's playtest: #19), so it isn't drawn.
	 */
	private static boolean isEyeShadow(HeroModels.Loaded loaded, SkinnedModel.Piece piece) {
		return EYE_SHADOWS.computeIfAbsent(piece, p -> p.image() >= 0 && p.image() < loaded.flat().length && loaded.flat()[p.image()]
			&& loaded.model().mostlyOn(p, "Lid"));
	}

	private static final Map<SkinnedModel.Piece, Boolean> EYE_SHADOWS = new IdentityHashMap<>();

	/** Skins the piece's vertices and writes its triangles (as quads with the last corner doubled). */
	private static void emit(SkinnedModel.Piece piece, Matrix4f[] skin, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
							 boolean prop, boolean cull) {
		emit(piece, skin, pose, buffer, light, overlay, prop, -1, cull, false);
	}

	private static void emit(SkinnedModel.Piece piece, Matrix4f[] skin, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
							 boolean prop, int color, boolean cull, boolean triangles) {
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
		boolean closed = cull && Timing.cull && CLOSED.computeIfAbsent(piece, HeroModelRenderer::isClosed);
		// into the pose's space once per vertex (camera-relative: the camera at the origin), normals too
		Matrix4f m = pose.pose();
		Vector3f t = new Vector3f();
		float[] q = new float[n * 3];
		float[] qn = new float[n * 3];
		for (int v = 0; v < n; v++) {
			m.transformPosition(p[v * 3], p[v * 3 + 1], p[v * 3 + 2], t);
			q[v * 3] = t.x;
			q[v * 3 + 1] = t.y;
			q[v * 3 + 2] = t.z;
			pose.transformNormal(nm[v * 3], nm[v * 3 + 1], nm[v * 3 + 2], t);
			qn[v * 3] = t.x;
			qn[v * 3 + 1] = t.y;
			qn[v * 3 + 2] = t.z;
		}
		for (int t0 = 0; t0 + 2 < idx.length; t0 += 3 * Timing.stride) {
			int a = idx[t0], b = idx[t0 + 1], c = idx[t0 + 2];
			if (prop && near(p, a, parked) && near(p, b, parked) && near(p, c, parked)) {
				continue;
			}
			if (closed && facesAway(q, a, b, c)) {
				continue;
			}
			vertex(buffer, q, qn, uv, a, light, overlay, color);
			vertex(buffer, q, qn, uv, b, light, overlay, color);
			vertex(buffer, q, qn, uv, c, light, overlay, color);
			if (!triangles) {
				// a quad's fourth corner, the third again (for render types that draw quads)
				vertex(buffer, q, qn, uv, c, light, overlay, color);
			}
		}
	}

	/**
	 * The triangle faces away from the camera (at the origin). On a closed piece (her body) it's hidden behind the
	 * piece's near side anyway, so it isn't sent: sending her 67k triangles cost 23 ms a frame on an integrated GPU (31
	 * FPS in third person against 115 without her), almost all of it the upload, not our CPU work. Open pieces (hair
	 * and cloth cards, paper) show both sides and are sent whole: culled, her hair showed the sky through it.
	 */
	private static boolean facesAway(float[] q, int a, int b, int c) {
		float ax = q[a * 3], ay = q[a * 3 + 1], az = q[a * 3 + 2];
		float e1x = q[b * 3] - ax, e1y = q[b * 3 + 1] - ay, e1z = q[b * 3 + 2] - az;
		float e2x = q[c * 3] - ax, e2y = q[c * 3 + 1] - ay, e2z = q[c * 3 + 2] - az;
		float nx = e1y * e2z - e1z * e2y;
		float ny = e1z * e2x - e1x * e2z;
		float nz = e1x * e2y - e1y * e2x;
		return nx * ax + ny * ay + nz * az >= 0;
	}

	/**
	 * Nearly closed: under CLOSED_OPEN_EDGES of its edges border only one triangle (corners welded by position, as UV
	 * seams split them). Her body pieces: 0.8-2%; hair and prop cards: 5-50%.
	 */
	private static boolean isClosed(SkinnedModel.Piece piece) {
		float[] pos = piece.pos();
		Map<Long, Integer> weld = new HashMap<>();
		int[] id = new int[piece.vertexCount()];
		for (int v = 0; v < id.length; v++) {
			long key = Math.round(pos[v * 3] * 1e4) * 73856093L ^ Math.round(pos[v * 3 + 1] * 1e4) * 19349663L
				^ Math.round(pos[v * 3 + 2] * 1e4) * 83492791L;
			id[v] = weld.computeIfAbsent(key, k -> weld.size());
		}
		Map<Long, Integer> edges = new HashMap<>();
		int[] idx = piece.indices();
		for (int t = 0; t + 2 < idx.length; t += 3) {
			for (int e = 0; e < 3; e++) {
				int a = id[idx[t + e]];
				int b = id[idx[t + (e + 1) % 3]];
				edges.merge(Math.min(a, b) * 4294967296L + Math.max(a, b), 1, Integer::sum);
			}
		}
		long open = edges.values().stream().filter(n -> n == 1).count();
		return !edges.isEmpty() && open < edges.size() * CLOSED_OPEN_EDGES;
	}

	private static final float CLOSED_OPEN_EDGES = 0.03f;
	private static final Map<SkinnedModel.Piece, Boolean> CLOSED = new IdentityHashMap<>();

	private static boolean near(float[] p, int i, float radiusSq) {
		return p[i * 3] * p[i * 3] + p[i * 3 + 1] * p[i * 3 + 1] + p[i * 3 + 2] * p[i * 3 + 2] < radiusSq;
	}

	/** One corner, already in the pose's space. */
	private static void vertex(VertexConsumer buffer, float[] q, float[] n, float[] uv, int i, int light, int overlay, int color) {
		buffer.addVertex(q[i * 3], q[i * 3 + 1], q[i * 3 + 2])
			.setColor(color)
			.setUv(uv[i * 2], uv[i * 2 + 1])
			.setOverlay(overlay)
			.setLight(light)
			.setNormal(n[i * 3], n[i * 3 + 1], n[i * 3 + 2]);
	}
}
