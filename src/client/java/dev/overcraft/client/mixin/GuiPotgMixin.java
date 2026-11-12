package dev.overcraft.client.mixin;

import dev.overcraft.client.hud.PotgCard;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Play of the Game's look (PotgCard) is drawn last in the GUI, after the screen and before the toasts, so it covers any
 * screen: the replay's loading screens hide under the white and the black, as the clip's cuts hide everything.
 */
@Mixin(Gui.class)
public abstract class GuiPotgMixin {
	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private GuiGraphicsExtractor overcraft$potgOverEverything(GuiGraphicsExtractor graphics) {
		PotgCard.render(graphics);
		return graphics;
	}
}
