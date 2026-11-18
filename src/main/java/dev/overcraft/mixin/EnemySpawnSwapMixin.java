package dev.overcraft.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Enemy heroes at night (#46): a natural spawn MobMixin marked (at the end of its finalizeSpawn) is swapped for its hero here, as the
 * spawner adds it (NaturalSpawner: finalizeSpawn, then addFreshEntityWithPassengers, which adds each through addFreshEntity), so the
 * zombie or skeleton never enters the world and nothing on it changes (the critique of 29c1797: the old next-tick swap's inert flags
 * were overwritten by the subclass's own pickup roll, and could be saved). EnemyHeroSpawns.adding decides.
 */
@Mixin(ServerLevel.class)
public abstract class EnemySpawnSwapMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void overcraft$enemyHeroSwap(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (dev.overcraft.entity.enemy.EnemyHeroSpawns.adding((ServerLevel) (Object) this, entity)) {
			cir.setReturnValue(true);
		}
	}
}
