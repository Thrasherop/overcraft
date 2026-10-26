package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.HeroSelectScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
	/**
	 * Behind the hero select screen, no hotbar, HUD or held hand (Overwatch shows a clean blurred map there): the screen
	 * hides the HUD the way F1 does while it's open.
	 */
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void overcraft$hideBehindHeroSelect(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.screen() instanceof HeroSelectScreen) {
			mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden = true;
			ci.cancel();
		}
	}

	/** In hero mode the hero HUD draws Overwatch's crosshair, so Minecraft's (and its attack indicator) steps aside. */
	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void overcraft$heroCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (ClientHero.controls()) {
			ci.cancel();
		}
	}
}
