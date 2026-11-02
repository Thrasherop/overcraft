package dev.overcraft.mixin;

import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A ghast fireball's blast power (private in 26.3): its blast at a barrier, its damage to Kinetic Grasp (dev.overcraft.hero.Barriers). */
@Mixin(LargeFireball.class)
public interface LargeFireballAccessor {
	@Accessor("explosionPower")
	int overcraft$explosionPower();
}
