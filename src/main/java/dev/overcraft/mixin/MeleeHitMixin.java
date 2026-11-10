package dev.overcraft.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A mob's melee blow (dev.overcraft.brigitte.Barrier.isMelee): Minecraft's doHurtTarget, in Mob and in the mobs that
 * strike without calling it (bees, iron golems, hoglins, zoglins). Melee goes through Brigitte's barrier (the owner,
 * 2026-11-02); a guardian's beam ends in doHurtTarget too and stays a ranged hit.
 */
@Mixin({Mob.class, Bee.class, IronGolem.class, Hoglin.class, Zoglin.class})
public abstract class MeleeHitMixin {
	@Inject(method = "doHurtTarget", at = @At("HEAD"))
	private void overcraft$meleeStart(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof Guardian)) {
			dev.overcraft.brigitte.Barrier.meleeDepth++;
		}
	}

	@Inject(method = "doHurtTarget", at = @At("RETURN"))
	private void overcraft$meleeEnd(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof Guardian)) {
			dev.overcraft.brigitte.Barrier.meleeDepth--;
		}
	}
}
