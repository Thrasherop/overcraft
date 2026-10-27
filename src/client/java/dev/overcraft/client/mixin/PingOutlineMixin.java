package dev.overcraft.client.mixin;

import dev.overcraft.client.comms.Pings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A pinged enemy is outlined for the pinger's team, as in Overwatch (the owner's screenshot, #40): this client only. */
@Mixin(Minecraft.class)
public abstract class PingOutlineMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void overcraft$pingedGlows(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (Pings.outlined(entity)) {
			cir.setReturnValue(true);
		}
	}
}
