package dev.overcraft.client.mixin;

import dev.overcraft.client.AbilityCamera;
import dev.overcraft.client.comms.EmoteCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the hero emotes, the view orbits her where the mouse turns it (EmoteCamera), placed as Minecraft places its
 * third person view: back from the point it looks at, pulled in where a block's in the way. While an ability holds the
 * view from behind (AbilityCamera), the view rises off her line of sight and turns to the point that line meets, so
 * that point (Catnapper's marker, where her shots go) shows above her head under the crosshair.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow private @Nullable Entity entity;
	@Shadow private float eyeHeight;
	@Shadow private float eyeHeightOld;

	@Shadow protected abstract void setRotation(float yRot, float xRot);

	@Shadow protected abstract void setPosition(double x, double y, double z);

	@Shadow protected abstract void move(float forwards, float up, float right);

	@Shadow protected abstract float getMaxZoom(float cameraDist);

	@Shadow @org.spongepowered.asm.mixin.Final private org.joml.Quaternionf rotation;
	@Shadow @org.spongepowered.asm.mixin.Final private org.joml.Vector3f forwards;
	@Shadow @org.spongepowered.asm.mixin.Final private org.joml.Vector3f up;
	@Shadow @org.spongepowered.asm.mixin.Final private org.joml.Vector3f left;

	/** Lúcio's roll on the wall, first person (#105): setRotation has no roll, so the camera's turn takes it after, and its axes with it. */
	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void overcraft$wallRoll(float partialTicks, CallbackInfo ci) {
		float roll = dev.overcraft.client.ClientHero.wallRoll(entity, partialTicks);
		if (roll != 0) {
			rotation.rotateZ(-roll * Mth.DEG_TO_RAD);
			new org.joml.Vector3f(0, 0, -1).rotate(rotation, forwards);
			new org.joml.Vector3f(0, 1, 0).rotate(rotation, up);
			new org.joml.Vector3f(-1, 0, 0).rotate(rotation, left);
			matrixPropertiesDirty |= 3;
		}
	}

	@Shadow private int matrixPropertiesDirty;

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void overcraft$emoteOrbit(float partialTicks, CallbackInfo ci) {
		if (!EmoteCamera.active() || entity == null || entity != Minecraft.getInstance().player) {
			return;
		}
		if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
			EmoteCamera.firstPersonFrames++;
		}
		float eye = Mth.lerp(partialTicks, eyeHeightOld, eyeHeight);
		setRotation(EmoteCamera.viewYaw(), EmoteCamera.viewPitch());
		setPosition(Mth.lerp(partialTicks, entity.xo, entity.getX()),
			Mth.lerp(partialTicks, entity.yo, entity.getY()) + EmoteCamera.pivot(eye),
			Mth.lerp(partialTicks, entity.zo, entity.getZ()));
		move(-getMaxZoom(EmoteCamera.distance()), 0, 0);
	}

	/**
	 * Play of the Game's chase view (PotgReplayClient, dev only): behind the hero's ghost, 4 m back and a little above,
	 * pulled in where a block's in the way. The camera entity stays the real player (design: "Camera").
	 */
	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void overcraft$replayChase(float partialTicks, CallbackInfo ci) {
		// only once the replay plays: under the card the view is the viewer's own, of the still world (found in play: a
		// card showing the last replay's stale place)
		if (!dev.overcraft.client.dev.PotgReplayClient.playing()) {
			return;
		}
		Entity ghost = dev.overcraft.client.dev.PotgReplayClient.heroGhost();
		if (ghost != null) {
			dev.overcraft.client.dev.PotgReplayClient.camX = Mth.lerp(partialTicks, ghost.xo, ghost.getX());
			dev.overcraft.client.dev.PotgReplayClient.camY = Mth.lerp(partialTicks, ghost.yo, ghost.getY()) + ghost.getEyeHeight();
			dev.overcraft.client.dev.PotgReplayClient.camZ = Mth.lerp(partialTicks, ghost.zo, ghost.getZ());
			dev.overcraft.client.dev.PotgReplayClient.camYaw = ghost.getViewYRot(partialTicks);
			dev.overcraft.client.dev.PotgReplayClient.camPitch = Mth.clamp(ghost.getViewXRot(partialTicks) * 0.5F + 12, -30, 45);
		}
		setRotation(dev.overcraft.client.dev.PotgReplayClient.camYaw, dev.overcraft.client.dev.PotgReplayClient.camPitch);
		setPosition(dev.overcraft.client.dev.PotgReplayClient.camX, dev.overcraft.client.dev.PotgReplayClient.camY + 0.4,
			dev.overcraft.client.dev.PotgReplayClient.camZ);
		move(-getMaxZoom(4.0F), 0, 0);
	}

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void overcraft$abilityView(float partialTicks, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (!AbilityCamera.active() || EmoteCamera.active() || entity == null || entity != mc.player
			|| mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK || entity.isPassenger()) {
			return;
		}
		Camera camera = (Camera) (Object) this;
		Vec3 eye = entity.getEyePosition(partialTicks);
		Vec3 look = entity.getViewVector(partialTicks);
		Vec3 up = new Vec3(camera.upVector());
		// how far back: the ability's own (Brigitte's barrier: 2 m) or Minecraft's third person, blended in from her eyes
		float own = entity instanceof LivingEntity living
			? (float) living.getAttributeValue(Attributes.CAMERA_DISTANCE) * living.getScale() : 4;
		float distance = AbilityCamera.frameDistance(own);
		// the lift, cut short under a ceiling
		double lift = AbilityCamera.lift();
		HitResult roof = entity.level().clip(new ClipContext(eye, eye.add(up.scale(lift + 0.2)), ClipContext.Block.VISUAL,
			ClipContext.Fluid.NONE, entity));
		if (roof.getType() != HitResult.Type.MISS) {
			lift = Math.max(0, roof.getLocation().distanceTo(eye) - 0.2);
		}
		Vec3 from = eye.add(up.scale(lift));
		float right = AbilityCamera.side();
		if (right != 0) {
			// to her side, cut short by a wall
			Vec3 toSide = new Vec3(camera.leftVector()).scale(-right);
			HitResult wall = entity.level().clip(new ClipContext(from, from.add(toSide), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
			from = wall.getType() == HitResult.Type.MISS ? from.add(toSide) : from.add(wall.getLocation().subtract(from).scale(0.8));
		}
		setPosition(from.x, from.y, from.z);
		move(-getMaxZoom(distance), 0, 0);
		HitResult seen = entity.level().clip(new ClipContext(eye, eye.add(look.scale(AbilityCamera.AIM_REACH)),
			ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		Vec3 to = seen.getLocation().subtract(camera.position());
		if (to.lengthSqr() > 1.0E-4) {
			setRotation((float) Math.toDegrees(Math.atan2(-to.x, to.z)), (float) Math.toDegrees(Math.atan2(-to.y, to.horizontalDistance())));
		}
	}
}
