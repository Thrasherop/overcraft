package dev.overcraft.client.mixin;

import dev.overcraft.client.dev.PotgReplayClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Play of the Game (dev only): no key acts while a replay plays (attack, use, hotbar, inventory); none skips it (owner). */
@Mixin(Minecraft.class)
public abstract class PotgReplayKeysMixin {
	@Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
	private void overcraft$noKeysInReplay(CallbackInfo ci) {
		if (PotgReplayClient.active()) {
			ci.cancel();
		}
	}
}