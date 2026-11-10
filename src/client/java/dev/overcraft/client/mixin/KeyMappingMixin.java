package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.controls.HeroControls;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hero mode switches key sets: Minecraft's gameplay keys read as up and unclicked (HeroControls says which), so the
 * hero's keys on the same buttons are the only ones that act. Their clicks are dropped, not saved for Minecraft mode.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
	@Shadow
	private int clickCount;

	@Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
	private void overcraft$heroModeUp(CallbackInfoReturnable<Boolean> cir) {
		if (overcraft$suppressed()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
	private void overcraft$heroModeNoClick(CallbackInfoReturnable<Boolean> cir) {
		if (overcraft$suppressed()) {
			clickCount = 0;
			cir.setReturnValue(false);
		}
	}

	/** A key going down, at the moment it does, not at the next tick (MouseHandler.onButton, KeyboardHandler.keyPress). */
	@Inject(method = "click", at = @At("TAIL"))
	private static void overcraft$clicked(com.mojang.blaze3d.platform.InputConstants.Key key, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		dev.overcraft.client.ClientSojourn.clicked(key);
	}

	@Inject(method = "set", at = @At("HEAD"))
	private static void overcraft$released(com.mojang.blaze3d.platform.InputConstants.Key key, boolean down, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		if (!down) {
			dev.overcraft.client.ClientSojourn.released(key);
		}
	}

	private boolean overcraft$suppressed() {
		return HeroControls.suppressedInHeroMode((KeyMapping) (Object) this) && ClientHero.controls();
	}
}
