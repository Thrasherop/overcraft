package dev.overcraft.client.mixin;

import dev.overcraft.client.dev.PotgReplayClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Play of the Game (dev only): the replay's run-in, from the keyframe to the window, is silent (design: "Run-in"). */
@Mixin(SoundManager.class)
public abstract class PotgReplaySoundMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void overcraft$quietRunIn(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		if (PotgReplayClient.quiet()) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}
}