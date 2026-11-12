package dev.overcraft.mixin;

import dev.overcraft.potg.PlayScore;
import dev.overcraft.potg.PotgValues;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Play of the Game's scoring: an eye of ender placed in an End portal frame (PotgValues.EYE_PLACED), or the eye that
 * completes the frame and opens the portal (PotgValues.PORTAL_OPENED instead, not on top; owner, 2026-11-11).
 * EnderEyeItem.useOn, server side: the eye is in once it plays level event 1503, and the portal opens where it plays the
 * global event 1038; the placing eye is scored on the way out, as one or the other.
 */
@Mixin(EnderEyeItem.class)
public abstract class PotgEyeMixin {
	@Unique
	private static @Nullable UseOnContext overcraft$placed;
	@Unique
	private static boolean overcraft$opened;

	@Inject(method = "useOn", at = @At(value = "INVOKE", ordinal = 0,
		target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
	private void overcraft$eyeIn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		overcraft$placed = context;
		overcraft$opened = false;
	}

	@Inject(method = "useOn", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;globalLevelEvent(ILnet/minecraft/core/BlockPos;I)V"))
	private void overcraft$portalOpen(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		overcraft$opened = true;
	}

	@Inject(method = "useOn", at = @At("RETURN"))
	private void overcraft$score(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (overcraft$placed == context) {
			PlayScore.progress(context.getPlayer(), overcraft$opened ? PotgValues.PORTAL_OPENED : PotgValues.EYE_PLACED,
				overcraft$opened ? "eye of ender, portal opened" : "eye of ender placed");
		}
		overcraft$placed = null;
		overcraft$opened = false;
	}
}
