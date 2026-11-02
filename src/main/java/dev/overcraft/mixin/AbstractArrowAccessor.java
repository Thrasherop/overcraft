package dev.overcraft.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An arrow's base damage (no getter in 26.3): what a barrier or Kinetic Grasp takes from it (dev.overcraft.hero.Barriers). */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
	@Accessor("baseDamage")
	double overcraft$baseDamage();
}
