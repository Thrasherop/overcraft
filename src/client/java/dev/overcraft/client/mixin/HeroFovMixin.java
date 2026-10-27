package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In hero mode the field of view stays put: Minecraft widens it with the speed attribute, so Kitsune Rush's +30%
 * zoomed the view out by 15% (#31). Overwatch's FOV is the player's setting, whatever the hero's speed.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class HeroFovMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
	private void overcraft$fixedFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
		if ((Object) this == Minecraft.getInstance().player && ClientHero.heroView()) {
			cir.setReturnValue(1.0F);
		}
	}
}
