package dev.overcraft.mixin;

import dev.overcraft.entity.OwProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * A hero's projectile hits a mob's drawn head too (#186: HeadBoxes), as the hitscan weapons do (Combat.clipEntity): Minecraft's
 * ProjectileUtil.getEntityHitResult tries only each mob's box, and an animal's head is drawn mostly outside it. Only the heroes'
 * projectiles (OwProjectile): Minecraft's own arrows keep Minecraft's rule.
 */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilHeadsMixin {
	@Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
		at = @At("RETURN"), cancellable = true)
	private static void overcraft$heads(Level level, Entity source, Vec3 from, Vec3 to, AABB area, Predicate<Entity> matching, float margin,
										CallbackInfoReturnable<@Nullable EntityHitResult> cir) {
		if (!(source instanceof OwProjectile)) {
			return;
		}
		EntityHitResult best = cir.getReturnValue();
		double nearest = best == null ? Double.MAX_VALUE : from.distanceToSqr(best.getLocation());
		// a head reaches up to about a metre past its mob's box: look that much further round the projectile's way
		for (Entity e : level.getEntities(source, area.inflate(1.0), matching)) {
			var at = dev.overcraft.hero.HeadBoxes.clip(e, margin, from, to);
			if (at.isPresent() && from.distanceToSqr(at.get()) < nearest) {
				nearest = from.distanceToSqr(at.get());
				best = new EntityHitResult(e, at.get());
			}
		}
		if (best != cir.getReturnValue()) {
			cir.setReturnValue(best);
		}
	}
}
