package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The scroll wheel doesn't change hotbar slots in hero mode: a stray scroll would drop out of hero mode mid-fight
 * (Overwatch's wheel does nothing for most heroes). Screens still scroll.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void overcraft$noSlotScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		if (ClientHero.controls() && Minecraft.getInstance().gui.screen() == null) {
			ci.cancel();
		}
	}
}
