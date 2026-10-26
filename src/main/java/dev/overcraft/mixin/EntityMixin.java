package dev.overcraft.mixin;

import dev.overcraft.hero.Combat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phased (Protection Suzu) heroes and allies are out of explosions altogether: Minecraft pushes everything in a blast
 * whether it takes damage or not, so a creeper still knocked them back (#27).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
	private void overcraft$phasedOutOfBlasts(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof LivingEntity living && Combat.isPhased(living)) {
			cir.setReturnValue(true);
		}
	}
}
