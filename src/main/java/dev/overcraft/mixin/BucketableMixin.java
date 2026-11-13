package dev.overcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.overcraft.hero.Sleep;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** A held mob caught in a bucket (a slept axolotl) keeps its own NoAI flag in the bucket, not the hold (#97). */
@Mixin(Bucketable.class)
public interface BucketableMixin {
	@WrapMethod(method = "saveDefaultDataToBucketTag")
	private static void overcraft$ownAi(Mob entity, ItemStack bucket, Operation<Void> original) {
		Sleep.ownAi(() -> original.call(entity, bucket));
	}
}
