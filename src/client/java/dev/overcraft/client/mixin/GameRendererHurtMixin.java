package dev.overcraft.client.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hero mode: no camera tilt when the hero is hit (#160, the owner: "taking damage in hero mode shouldn't tilt the screen
 * like minecraft"). Overwatch's camera doesn't roll on a hit; its hit feedback is the HUD's. Minecraft's death roll stays
 * (bobHurt also rolls the dying camera): the hero is out of play then.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererHurtMixin {
	@Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
	private void overcraft$noHurtTilt(net.minecraft.client.renderer.state.level.CameraRenderState cameraState,
									  com.mojang.blaze3d.vertex.PoseStack poseStack, CallbackInfo ci) {
		if (dev.overcraft.client.ClientHero.controls() && !cameraState.entityRenderState.isDeadOrDying) {
			ci.cancel();
		}
	}
}
