package dev.overcraft.mixin;

import dev.overcraft.entity.OwProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overwatch projectiles hit with their own radius from the moment they're thrown. Minecraft widens every projectile's
 * hits from nothing to 0.3 m over its first 8 ticks (computeMargin), whatever its size: a kunai had no width for its
 * first 9 m and 0.3 m after (issue #15).
 */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {
	@Inject(method = "computeMargin", at = @At("HEAD"), cancellable = true)
	private static void overcraft$ownRadius(Entity source, CallbackInfoReturnable<Float> cir) {
		if (source instanceof OwProjectile projectile) {
			cir.setReturnValue(projectile.hitRadius());
		}
	}
}
