package dev.overcraft.mixin;

import dev.overcraft.hero.Combat;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Completing an advancement charges the hero's ultimate (owner's rule: achievements earn ult charge). */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	@Inject(method = "award", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
	private void overcraft$ultChargeOnAdvancement(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		holder.value().display().ifPresent(display -> Combat.onAdvancement(player, display.type()));
	}
}
