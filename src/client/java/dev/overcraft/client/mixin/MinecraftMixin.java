package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In hero mode the mouse buttons are the hero's weapons: no attacking, mining or using items. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void overcraft$noAttack(CallbackInfoReturnable<Boolean> cir) {
		if (ClientHero.controls()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void overcraft$noMining(boolean down, CallbackInfo ci) {
		if (ClientHero.controls()) {
			ci.cancel();
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void overcraft$noUse(CallbackInfo ci) {
		if (ClientHero.controls()) {
			ci.cancel();
		}
	}
}
