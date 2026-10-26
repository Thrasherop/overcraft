package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.HeroHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In hero mode the subtitles move up out of the hero HUD's way, as the chat does (they sat on the ammo panels). */
@Mixin(SubtitleOverlay.class)
public abstract class SubtitleOverlayMixin {
	@Unique
	private boolean overcraft$lifted;

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void overcraft$lift(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		overcraft$lifted = ClientHero.controls();
		if (overcraft$lifted) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(0, -HeroHud.chatLift(graphics.guiWidth(), graphics.guiHeight()));
		}
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void overcraft$drop(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (overcraft$lifted) {
			graphics.pose().popMatrix();
			overcraft$lifted = false;
		}
	}
}
