package dev.overcraft.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.overcraft.client.model.HeroModelRenderer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In hero mode, the first-person view shows the hero's own view model (Overwatch's arms) instead of Steve's hand; in
 * Minecraft mode with a hero picked, her arms holding Minecraft's items (HeroModelRenderer.submitMinecraftArms).
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void overcraft$heroArms(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector,
									PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
		int light = playerState.avatarRenderState != null ? playerState.avatarRenderState.lightCoords : 0xF000F0;
		if (HeroModelRenderer.submitFirstPerson(partialTicks, poseStack, collector, light,
			state.viewXRot - state.xBob, state.viewYRot - state.yBob)) {
			ci.cancel();
		} else if (playerState.avatarRenderState != null && state.handRenderSelection != null
			&& HeroModelRenderer.submitMinecraftArms(partialTicks, poseStack, collector, light, state.viewXRot - state.xBob,
			state.viewYRot - state.yBob, state, playerState.avatarRenderState)) {
			ci.cancel();
		}
	}
}
