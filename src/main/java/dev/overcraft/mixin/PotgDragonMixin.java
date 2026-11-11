package dev.overcraft.mixin;

import dev.overcraft.potg.PotgReplay;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game (PotgReplay): at the dragon's death tick 199, one before it's removed and the exit portal placed, the
 * POTG starts and the world freezes, so the portal opens after it (techniques/potg/design.md, "Why the hold at tick 199").
 */
@Mixin(EnderDragon.class)
public abstract class PotgDragonMixin {
	@Inject(method = "tickDeath", at = @At("TAIL"))
	private void overcraft$potgAt199(CallbackInfo ci) {
		EnderDragon dragon = (EnderDragon) (Object) this;
		if (dragon.dragonDeathTime == 199 && !dragon.level().isClientSide()) {
			PotgReplay.dragonAt199(dragon);
		}
	}
}