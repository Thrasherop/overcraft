package dev.overcraft.mixin;

import dev.overcraft.potg.PlayScore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Play of the Game's scoring: the item a dying mob drops (every spawnAtLocation overload ends in this one, as the loot
 * table's drops do) is tagged when it's a scoring drop, so only that first pickup scores (owner, 2026-11-12).
 */
@Mixin(Entity.class)
public abstract class PotgDropMixin {
	@Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",
		at = @At("RETURN"))
	private void overcraft$potgDrop(ServerLevel level, ItemStack stack, Vec3 offset, CallbackInfoReturnable<ItemEntity> cir) {
		ItemEntity drop = cir.getReturnValue();
		if (drop != null) {
			PlayScore.dropped((Entity) (Object) this, drop);
		}
	}
}
