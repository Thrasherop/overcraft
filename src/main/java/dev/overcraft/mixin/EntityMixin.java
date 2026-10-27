package dev.overcraft.mixin;

import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phased (Protection Suzu) heroes and allies are out of explosions altogether: Minecraft pushes everything in a blast
 * whether it takes damage or not, so a creeper still knocked them back (#27).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	/**
	 * In hero mode a fall counts from the highest point reached: going up (her wall climb, a jump's rise) starts it
	 * again, as a ladder does in Minecraft. Without this, a fall caught by climbing a wall added to the next one (#35).
	 */
	@Inject(method = "checkFallDamage", at = @At("HEAD"))
	private void overcraft$heroFallFromTop(double ya, boolean onGround, BlockState onState, BlockPos pos, CallbackInfo ci) {
		if (ya > 0 && (Object) this instanceof Player player && HeroManager.controls(player)) {
			player.resetFallDistance();
		}
	}

	@Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
	private void overcraft$phasedOutOfBlasts(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof LivingEntity living && Combat.isPhased(living)) {
			cir.setReturnValue(true);
		}
	}
}
