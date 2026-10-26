package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No block-selection outline in hero mode: Overwatch has none, and it framed the crosshair at every wall climbed. */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
	@Inject(method = "extractBlockOutline", at = @At("TAIL"))
	private void overcraft$noOutlineInHeroMode(Camera camera, LevelRenderState levelRenderState, CallbackInfo ci) {
		if (ClientHero.controls()) {
			levelRenderState.blockOutlineRenderState = null;
		}
	}
}
