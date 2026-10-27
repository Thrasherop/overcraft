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
 * In hero mode neither the camera nor the first-person arms take Minecraft's walking bob: it swung the arms up and
 * down like Steve's hand (issue #14) and bounced and rolled the view on every step (#31). Overwatch's camera stays
 * level while running; its arms only drift a little with the movement (HeroModelRenderer).
 */
@Mixin(GameRenderer.class)
public abstract class ViewBobMixin {
	@Shadow
	protected abstract void bobView(CameraRenderState cameraState, PoseStack poseStack);

	@Redirect(method = {"renderItemInHand", "renderLevel"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void overcraft$heroNoBob(GameRenderer renderer, CameraRenderState cameraState, PoseStack poseStack) {
		if (!ClientHero.heroView()) {
			bobView(cameraState, poseStack);
		}
	}
}
