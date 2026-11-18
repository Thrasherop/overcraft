package dev.overcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overcraft.hero.Sleep;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A mob asleep, stunned, knocked down or lifted (Sleep.aiHeld) answers isNoAi with true, so it does nothing, as a NoAI mob
 * does (its goals, its travel, the dragon's flight), while its own NoAI flag, the one saved, is never touched (#97). Its
 * save reads its own flag.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "isNoAi", at = @At("HEAD"), cancellable = true)
	private void overcraft$heldAi(CallbackInfoReturnable<Boolean> cir) {
		// (server side only: in single player the client's thread shares Sleep's maps, and a client mob is never held)
		Mob self = (Mob) (Object) this;
		if (!self.level().isClientSide() && Sleep.holdApplies(self)) {
			cir.setReturnValue(true);
		}
	}

	@WrapOperation(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;isNoAi()Z"))
	private boolean overcraft$savedAi(Mob self, Operation<Boolean> original) {
		return Sleep.ownAi(() -> original.call(self));
	}

	/**
	 * Enemy heroes at night (#46): a natural spawn is marked here, at the end of Mob.finalizeSpawn (a zombie's and a skeleton's own
	 * finalizeSpawn call it first), and swapped for a hero on the next tick (EnemyHeroSpawns).
	 */
	@Inject(method = "finalizeSpawn", at = @At("TAIL"))
	private void overcraft$enemyHero(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
									 net.minecraft.world.entity.EntitySpawnReason reason, net.minecraft.world.entity.SpawnGroupData groupData,
									 CallbackInfoReturnable<net.minecraft.world.entity.SpawnGroupData> cir) {
		dev.overcraft.entity.enemy.EnemyHeroSpawns.finalized((Mob) (Object) this, level, reason);
	}
}
