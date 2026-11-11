package dev.overcraft.client.mixin;

import dev.overcraft.client.dev.PotgReplayClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game (dev only): a frame extracted and drawn while a replay plays sees the ghosts (PotgReplayClient.ghostView).
 * Extraction (the camera, the entities' render states) is its own call before render(), from Minecraft.runTick.
 */
@Mixin(GameRenderer.class)
public abstract class PotgReplayRenderMixin {
	@Inject(method = {"render()V", "extract"}, at = @At("HEAD"))
	private void overcraft$ghostFrame(CallbackInfo ci) {
		PotgReplayClient.rendering(true);
	}

	@Inject(method = "extract", at = @At("HEAD"))
	private void overcraft$frameStarts(CallbackInfo ci) {
		PotgReplayClient.frame();
	}

	@Inject(method = {"render()V", "extract"}, at = @At("RETURN"))
	private void overcraft$liveAgain(CallbackInfo ci) {
		PotgReplayClient.rendering(false);
	}
}