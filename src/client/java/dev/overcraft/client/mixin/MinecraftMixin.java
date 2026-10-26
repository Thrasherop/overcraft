package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.screen.OverwatchMenuScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In hero mode the mouse buttons are the hero's weapons: no attacking, mining or using items (the interact key does
 * doors and chests). Esc opens Overwatch's menu instead of Minecraft's.
 */
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
		if (ClientHero.controls() && !ClientHero.interacting()) {
			ci.cancel();
		}
	}

	@Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
	private void overcraft$overwatchMenu(boolean suppressPauseMenu, CallbackInfo ci) {
		Minecraft mc = (Minecraft) (Object) this;
		if (ClientHero.controls() && mc.gui.screen() == null && !suppressPauseMenu) {
			mc.gui.setScreen(new OverwatchMenuScreen());
			ci.cancel();
		}
	}
}
