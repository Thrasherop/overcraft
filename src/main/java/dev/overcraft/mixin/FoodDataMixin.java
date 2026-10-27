package dev.overcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.overcraft.hero.HeroManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hunger in hero mode (#43): it burns as in Minecraft, down to 4 of the bar's 10 and no further (owner, 2026-10-27), so
 * a hero never starves under the food bar the hero HUD hides. One already below that (hungry when they switched with G)
 * stays where they are, and doesn't starve either. Nor does a full bar heal them: Overwatch's own passive healing is the
 * hero's (Minecraft's on top healed her between a zombie's hits). Eating, and Minecraft mode's hunger, are Minecraft's.
 */
@Mixin(net.minecraft.world.food.FoodData.class)
public abstract class FoodDataMixin {
	/** Where hero mode's hunger stops: 4 bars, 8 food points (owner, 2026-10-27). */
	private static final int HERO_FOOD_FLOOR = 8;

	@Shadow private int foodLevel;

	/** The food level after exhaustion takes a point (Math.max(foodLevel - 1, 0)). */
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
	private int overcraft$heroHungerFloor(int food, ServerPlayer player) {
		return HeroManager.controls(player) ? Math.max(food, Math.min(foodLevel, HERO_FOOD_FLOOR)) : food;
	}

	/** The naturalRegeneration game rule as the tick reads it: off for a hero in hero mode. */
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/Boolean;booleanValue()Z"))
	private boolean overcraft$noHeroFoodHealing(boolean naturalRegen, ServerPlayer player) {
		return naturalRegen && !HeroManager.controls(player);
	}

	@WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerPlayer;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean overcraft$noHeroStarving(ServerPlayer player, ServerLevel level, DamageSource source, float amount) {
		return !HeroManager.controls(player);
	}
}
