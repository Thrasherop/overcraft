package dev.overcraft.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A stealthed Sombra is seen only within 4 m and in sight (her graph 420C's detection): every mob's targeting goes through
 * here, so none picks her up from further (Minecraft's own invisibility would let a zombie see her from about 2.5 m).
 */
@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {
	@Inject(method = "test", at = @At("HEAD"), cancellable = true)
	private void overcraft$stealthedSombra(ServerLevel level, LivingEntity targeter, LivingEntity target,
		CallbackInfoReturnable<Boolean> cir) {
		if (targeter != null && dev.overcraft.sombra.Stealth.hiddenFrom(target, targeter)) {
			cir.setReturnValue(false);
		}
	}
}
