package dev.overcraft.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A projectile's own hit, for one that meets Brigitte's barrier (dev.overcraft.brigitte.Barrier.tickProjectiles). */
@Mixin(Projectile.class)
public interface ProjectileInvoker {
	@Invoker("onHit")
	void overcraft$onHit(HitResult hit);

	@Invoker("canHitEntity")
	boolean overcraft$canHitEntity(net.minecraft.world.entity.Entity entity);
}
