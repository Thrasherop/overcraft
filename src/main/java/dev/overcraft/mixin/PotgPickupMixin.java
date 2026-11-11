package dev.overcraft.mixin;

import dev.overcraft.potg.PlayScore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game's scoring: a hero picking up an item (ItemEntity.playerTouch calls take before the item goes into the
 * inventory), worth what PotgValues.PICKUP says. The table is empty until the owner decides (owner, 2026-11-11).
 */
@Mixin(LivingEntity.class)
public abstract class PotgPickupMixin {
	@Inject(method = "take", at = @At("HEAD"))
	private void overcraft$potgPickup(Entity entity, int count, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && entity instanceof ItemEntity item) {
			PlayScore.pickup(player, item.getItem());
		}
	}
}