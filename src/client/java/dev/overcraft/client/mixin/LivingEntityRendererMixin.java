package dev.overcraft.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overcraft.client.model.HeroModelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the hero model instead of the Minecraft one when the render state carries one (players in hero mode). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At("HEAD"), cancellable = true)
	private void overcraft$heroModel(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
									 CameraRenderState camera, CallbackInfo ci) {
		if (HeroModelRenderer.submit(state, poseStack, collector)) {
			ci.cancel();
		}
	}
}
