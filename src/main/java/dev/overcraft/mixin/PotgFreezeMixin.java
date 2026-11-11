package dev.overcraft.mixin;

import dev.overcraft.potg.PotgReplay;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game (PotgReplay, dev only): a player watching a replay isn't ticked on the server either (Minecraft's tick
 * freeze leaves players running), so their effects, food, air and fire wait with the world and nothing can hurt them.
 */
@Mixin(ServerPlayer.class)
public abstract class PotgFreezeMixin {
	@Inject(method = "doTick", at = @At("HEAD"), cancellable = true)
	private void overcraft$heldForReplay(CallbackInfo ci) {
		if (PotgReplay.viewing((ServerPlayer) (Object) this)) {
			ci.cancel();
		}
	}
}