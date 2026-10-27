package dev.overcraft.client.mixin;

import dev.overcraft.client.comms.Pings;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A pinged enemy's outline is the ping's red (PingOutlineMixin; the outline takes the entity's team colour). */
@Mixin(Entity.class)
public abstract class PingOutlineColourMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void overcraft$pingRed(CallbackInfoReturnable<Integer> cir) {
		if (Pings.outlined((Entity) (Object) this)) {
			cir.setReturnValue(Pings.OUTLINE_RED);
		}
	}
}
