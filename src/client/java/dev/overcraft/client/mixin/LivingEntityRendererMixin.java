package dev.overcraft.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overcraft.client.effect.Cleansed;
import dev.overcraft.client.model.HeroModelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	/** Protection Suzu's white flash on whoever it cleansed (client/effect/Cleansed), carried in the render state. */
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
		at = @At("TAIL"))
	private void overcraft$cleansed(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
		state.setData(HeroModelRenderer.WHITE, Cleansed.white(entity.getId()));
	}

	/** ...drawn as Minecraft's white overlay (a creeper's flash). */
	@Inject(method = "getWhiteOverlayProgress", at = @At("RETURN"), cancellable = true)
	private void overcraft$white(LivingEntityRenderState state, CallbackInfoReturnable<Float> cir) {
		Float white = state.getData(HeroModelRenderer.WHITE);
		if (white != null && white > cir.getReturnValueF()) {
			cir.setReturnValue(white);
		}
	}

	/** Draws the hero model instead of the Minecraft one when the render state carries one (players in hero mode). */
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At("HEAD"), cancellable = true)
	private void overcraft$heroModel(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
									 CameraRenderState camera, CallbackInfo ci) {
		if (HeroModelRenderer.submit(state, poseStack, collector)) {
			ci.cancel();
		}
	}
}
