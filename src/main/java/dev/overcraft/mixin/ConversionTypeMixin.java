package dev.overcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overcraft.hero.Sleep;
import net.minecraft.world.entity.ConversionType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A mob converting while Overcraft holds its AI (a slept villager struck by lightning) passes on its own NoAI flag (#97). */
@Mixin(ConversionType.class)
public abstract class ConversionTypeMixin {
	@WrapOperation(method = "convertCommon", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;isNoAi()Z"))
	private static boolean overcraft$ownAi(Mob from, Operation<Boolean> original) {
		return Sleep.ownAi(() -> original.call(from));
	}
}
