package dev.overcraft.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overcraft.client.ClientHero;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * In hero mode the first-person arms don't take Minecraft's walking bob: it swung them up and down like Steve's hand
 * (issue #14). Overwatch's arms only drift a little with the movement (HeroModelRenderer). The camera's own bob is
 * Minecraft's View Bobbing setting, left as it is.
 */
@Mixin(GameRenderer.class)
public abstract class HandBobMixin {
	@Shadow
	protected abstract void bobView(CameraRenderState cameraState, PoseStack poseStack);

	@Redirect(method = "renderItemInHand", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void overcraft$heroArmsNoBob(GameRenderer renderer, CameraRenderState cameraState, PoseStack poseStack) {
		if (!ClientHero.heroView()) {
			bobView(cameraState, poseStack);
		}
	}
}
