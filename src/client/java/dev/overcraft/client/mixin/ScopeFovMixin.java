package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientAna;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ana's zoom follows her scope as it moves (#90). Minecraft eases the view's FOV halfway to its target each tick (Camera.tickFov,
 * meant for the speed attribute's widening): over her 0.25 s scope-in it trailed by two or three ticks, so the zoom lagged
 * the overlay and the scoped shot. Overwatch's camera takes the scope's FOV straight from v172 by its progress (0B94 #84).
 * While she scopes, the eased value is set to the target.
 */
@Mixin(Camera.class)
public abstract class ScopeFovMixin {
	@Shadow
	private float fovModifier;

	@Inject(method = "tickFov", at = @At("TAIL"))
	private void overcraft$scopeFov(CallbackInfo ci) {
		if (ClientAna.scopeMoving()) {
			fovModifier = ClientAna.fovModifier();
		}
	}
}
