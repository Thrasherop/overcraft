package dev.overcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.overcraft.potg.PlayScore;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game's scoring: a hero picking up an item, worth what PotgValues.PICKUP says. ItemEntity.playerTouch
 * calls Player.take after the inventory took the stack, so by then the entity's stack is what's left (empty, an air
 * stack, when all was taken): the item and the count before come from playerTouch's own locals, and the count taken is
 * that less what's left.
 */
@Mixin(ItemEntity.class)
public abstract class PotgPickupMixin {
	@Inject(method = "playerTouch", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V"))
	private void overcraft$potgPickup(Player player, CallbackInfo ci, @Local Item item, @Local int orgCount) {
		PlayScore.pickup(player, item, orgCount - ((ItemEntity) (Object) this).getItem().getCount());
	}
}
