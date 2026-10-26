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
	}

	/** Per model: its rest skin and the turn that lays its longest side along +Z. */
	private record Prepared(Matrix4f[] skin, Quaternionf align) {
	}

	private static final Map<SkinnedModel, Prepared> PREPARED = new IdentityHashMap<>();

	private final String model;
	private final boolean flip;

	/** {@code flip}: the model's tip points the other way along its long side. */
	public ProjectileModelRenderer(EntityRendererProvider.Context context, String model, float spriteScale, boolean flip) {
		super(context, spriteScale, true);
		this.model = model;
		this.flip = flip;
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
			case OfudaEntity ofuda -> KirikoFx.ofudaFlight(ofuda.getId(), at, ofuda.homing());
			case KunaiEntity kunai -> KirikoFx.kunaiFlight(kunai.getId(), at, dt);
			case SuzuEntity suzu -> KirikoFx.suzuFlight(suzu.getId(), at, dt);
			default -> {
			}
		}
	}

	@Override
	public void submit(ThrownItemRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		State s = (State) state;
		HeroModels.Loaded loaded = s.loaded;
		if (loaded == null) {
			super.submit(state, poseStack, collector, camera);
			return;
		}
		Prepared p = PREPARED.computeIfAbsent(loaded.model(), ProjectileModelRenderer::prepare);
		poseStack.pushPose();
		// yaw then pitch take +Z onto the flight direction (yRot = atan2(vx, vz), xRot = atan2(vy, horizontal))
		poseStack.rotateDegrees(Axis.YP, s.yRot);
		poseStack.rotateDegrees(Axis.XP, -s.xRot);
		if (flip) {
			poseStack.rotateDegrees(Axis.YP, 180);
		}
		poseStack.mulPose(new Matrix4f().rotation(p.align()));
		HeroModelRenderer.submitModel(loaded, p.skin(), poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}

	private static Prepared prepare(SkinnedModel model) {
		Matrix4f[] skin = Skeleton.skin(model, new Skeleton.Pose(model.nodeNames.length).rest(model), -1, null);
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
