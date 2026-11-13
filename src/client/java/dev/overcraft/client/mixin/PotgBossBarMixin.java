package dev.overcraft.client.mixin;

import dev.overcraft.client.hud.PotgCard;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No boss bar under the Play of the Game's top band (the coordinator, 2026-11-11: the dragon's bar sat in the band). */
@Mixin(BossHealthOverlay.class)
public abstract class PotgBossBarMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void overcraft$noBarUnderPotg(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (PotgCard.showing()) {
			ci.cancel();
		}
	}
}
