package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.overcraft.client.comms.CommsWheel;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The scroll wheel doesn't change hotbar slots in hero mode: there's no hotbar there (Overwatch's wheel does nothing
 * for most heroes). Screens still scroll.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	/** While the communication wheel is open, the mouse moves its pointer, not the view (CommsWheel). */
	@Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void overcraft$wheelPointer(LocalPlayer player, double dx, double dy) {
		if (CommsWheel.open()) {
			CommsWheel.move(dx, dy);
		} else {
			player.turn(dx, dy);
		}
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void overcraft$noSlotScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		if (ClientHero.controls() && Minecraft.getInstance().gui.screen() == null) {
			ci.cancel();
		}
	}
}
