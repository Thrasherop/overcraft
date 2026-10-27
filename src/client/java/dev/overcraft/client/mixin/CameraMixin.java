package dev.overcraft.client.mixin;

import dev.overcraft.client.comms.EmoteCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the hero emotes, the view orbits her where the mouse turns it (EmoteCamera), placed as Minecraft places its
 * third person view: back from the point it looks at, pulled in where a block's in the way.
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
}
