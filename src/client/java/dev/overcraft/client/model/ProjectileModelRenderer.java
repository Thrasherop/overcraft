package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.overcraft.client.effect.Fx;
import dev.overcraft.client.effect.KirikoFx;
import dev.overcraft.entity.KunaiEntity;
import dev.overcraft.entity.OfudaEntity;
import dev.overcraft.entity.OwProjectile;
import dev.overcraft.entity.SuzuEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A hero's projectile as its Overwatch model (hero_models/projectile_<name>.glb in the local asset pack), its long
 * axis pointed along its flight. Without the model: the item sprite, as Minecraft draws thrown items.
 */
public class ProjectileModelRenderer<T extends OwProjectile> extends ThrownItemRenderer<T> {
	public static class State extends ThrownItemRenderState {
		float yRot;
		float xRot;
		HeroModels.@Nullable Loaded loaded;
		/** Its flight effect is its look (Lúcio's round: no model, all glow and rings). */
		boolean drawnByEffect;
	}

	/** Per model: its rest skin and the turn that lays its longest side along +Z. */
	private record Prepared(Matrix4f[] skin, Quaternionf align) {
	}

	private static final Map<SkinnedModel, Prepared> PREPARED = new IdentityHashMap<>();

	private final String model;
	private final boolean flip;
	private final float modelScale;
	private final boolean effectOnly;

	/** {@code flip}: the model's tip points the other way along its long side. */
	public ProjectileModelRenderer(EntityRendererProvider.Context context, String model, float spriteScale, boolean flip) {
		this(context, model, spriteScale, flip, 1, false);
	}

	/**
	 * {@code modelScale}: drawn this much larger than the model. {@code effectOnly}: with the local pack, no model at all,
	 * its flight effect is its look (the ofuda: Overwatch draws a thrown one as a glowing talisman, not the paper model
	 * in her hand).
	 */
	public ProjectileModelRenderer(EntityRendererProvider.Context context, String model, float spriteScale, boolean flip,
								   float modelScale, boolean effectOnly) {
		super(context, spriteScale, true);
		this.model = model;
		this.flip = flip;
		this.modelScale = modelScale;
		this.effectOnly = effectOnly;
	}

	@Override
	public ThrownItemRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T entity, ThrownItemRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		State s = (State) state;
		s.yRot = entity.getYRot(partialTicks);
		s.xRot = entity.getXRot(partialTicks);
		s.loaded = HeroModels.get("projectile_" + model);
		// the flight effects (trail, glow, sparkles), fed with where it is this frame
		Vec3 at = entity.getPosition(partialTicks);
		float dt = Fx.since(entity.getId());
		switch (entity) {
			case OfudaEntity ofuda -> KirikoFx.ofudaFlight(ofuda.getId(), at, ofuda.homing(), KirikoFx.enemyOf(ofuda.getOwner()));
			case KunaiEntity kunai -> KirikoFx.kunaiFlight(kunai.getId(), at, dt, KirikoFx.enemyOf(kunai.getOwner()));
			case SuzuEntity suzu -> KirikoFx.suzuFlight(suzu.getId(), at, dt, KirikoFx.enemyOf(suzu.getOwner()));
			case dev.overcraft.entity.SonicEntity sonic -> s.drawnByEffect = dev.overcraft.client.effect.LucioFx.sonicFlight(
				sonic.getId(), at, sonic.getDeltaMovement(), dt, KirikoFx.enemyOf(sonic.getOwner()));
			case dev.overcraft.entity.PawEntity paw -> s.drawnByEffect = dev.overcraft.client.effect.JetpackCatFx.pawFlight(
				paw.getId(), at, paw.getDeltaMovement(), paw.centre(), paw.getOwner(), KirikoFx.enemyOf(paw.getOwner()),
				paw.getOwner() == net.minecraft.client.Minecraft.getInstance().player
					&& net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson());
			default -> {
			}
		}
	}

	@Override
	public void submit(ThrownItemRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		State s = (State) state;
		HeroModels.Loaded loaded = s.loaded;
		if (loaded == null) {
			if (!s.drawnByEffect) {
				super.submit(state, poseStack, collector, camera);
			}
			return;
		}
		if (effectOnly) {
			// drawn by its flight effect alone (KirikoFx)
			return;
		}
		Prepared p = PREPARED.computeIfAbsent(loaded.model(), ProjectileModelRenderer::prepare);
		poseStack.pushPose();
		// yaw then pitch take +Z onto the flight direction (yRot = atan2(vx, vz), xRot = atan2(vy, horizontal))
		poseStack.rotateDegrees(Axis.YP, s.yRot);
		poseStack.rotateDegrees(Axis.XP, -s.xRot);
		if (modelScale != 1) {
			poseStack.scale(modelScale, modelScale, modelScale);
		}
		if (flip) {
			poseStack.rotateDegrees(Axis.YP, 180);
		}
		poseStack.mulPose(new Matrix4f().rotation(p.align()));
		// no back-face culling (a talisman is a thin card seen from either side), and not a hero's stowed prop
		HeroModelRenderer.submitModel(loaded, p.skin(), poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY,
			java.util.Set.of(), false, false);
		poseStack.popPose();
	}

	private static Prepared prepare(SkinnedModel model) {
		Skeleton.Pose rest = new Skeleton.Pose(model.nodeNames.length).rest(model);
		// props sit scaled to nothing in a model's rest pose (the clips that hold them scale them up): a thrown one is
		// shown at full size. Drawn at rest, every projectile model collapsed to a point and only its glow showed (the
		// fifth critique: no paper on the ofuda)
		for (Vector3f scale : rest.s) {
			if (Math.abs(scale.x) < 1.0E-3f && Math.abs(scale.y) < 1.0E-3f && Math.abs(scale.z) < 1.0E-3f) {
				scale.set(1);
			}
		}
		Matrix4f[] skin = Skeleton.skin(model, rest, -1, null);
		Vector3f lo = new Vector3f(Float.MAX_VALUE), hi = new Vector3f(-Float.MAX_VALUE);
		for (SkinnedModel.Piece piece : model.pieces) {
			float[] pos = piece.pos();
			for (int v = 0; v < piece.vertexCount(); v++) {
				Vector3f at = skin[piece.joint4()[v * 4]].transformPosition(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2], new Vector3f());
				lo.min(at);
				hi.max(at);
			}
		}
		Vector3f size = new Vector3f(hi).sub(lo);
		Vector3f center = new Vector3f(lo).add(hi).mul(0.5f);
		Quaternionf align = new Quaternionf();
		if (size.x >= size.y && size.x >= size.z) {
			align.rotateY((float) Math.toRadians(-90));
		} else if (size.y >= size.z) {
			align.rotateX((float) Math.toRadians(90));
		}
		// centre it on the entity: the model's own origin is where the hand holds it
		Matrix4f[] centred = new Matrix4f[skin.length];
		for (int i = 0; i < skin.length; i++) {
			centred[i] = new Matrix4f().translation(-center.x, -center.y, -center.z).mul(skin[i]);
		}
		return new Prepared(centred, align);
	}
}
