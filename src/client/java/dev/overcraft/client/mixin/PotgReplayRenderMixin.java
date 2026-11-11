package dev.overcraft.client.mixin;

import dev.overcraft.client.dev.PotgReplayClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Play of the Game (dev only): a frame drawn while a replay plays sees the ghosts (PotgReplayClient.ghostView). */
@Mixin(GameRenderer.class)
public abstract class PotgReplayRenderMixin {
	@Inject(method = "render()V", at = @At("HEAD"))
	private void overcraft$ghostFrame(CallbackInfo ci) {
		PotgReplayClient.rendering(true);
	}

	@Inject(method = "render()V", at = @At("RETURN"))
	private void overcraft$liveAgain(CallbackInfo ci) {
		PotgReplayClient.rendering(false);
	}
}