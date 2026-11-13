package dev.overcraft.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game (ClientPotg): the real body holds still under the card, mid-air or not, the world being frozen; it
 * sends no moves, and the server doesn't tick it either (PotgFreezeMixin). Then the replay engine takes the player out.
 */
@Mixin(LocalPlayer.class)
public abstract class PotgReplayHoldMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void overcraft$holdForReplay(CallbackInfo ci) {
		if (dev.overcraft.client.ClientPotg.holdBody()) {
			ci.cancel();
		}
	}
}