package dev.overcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * A blast a Brigitte's barrier stands in front of pushes no one behind it (dev.overcraft.brigitte.Barrier.blocksBlast):
 * Minecraft pushes from an explosion apart from its damage, so a creeper's blast the barrier took whole still threw her
 * 4 m back (found playing). Others' push (pushFromExplosion), and a player's, which their own client applies (hitPlayers).
 * A Stalwart tank's push is 40% smaller in both (dev.overcraft.hero.Tank).
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private Vec3 center;

	@WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pushFromExplosion(Lnet/minecraft/world/phys/Vec3;)V"))
	private void overcraft$barrierPush(Entity entity, Vec3 impulse, Operation<Void> original) {
		original.call(entity, dev.overcraft.brigitte.Barrier.blocksBlast(entity, center) ? Vec3.ZERO
			: impulse.scale(dev.overcraft.hero.Tank.knockbackFactor(entity)));
	}

	@WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object overcraft$barrierPlayerPush(Map<Object, Object> map, Object player, Object impulse, Operation<Object> original) {
		boolean blocked = player instanceof Entity entity && dev.overcraft.brigitte.Barrier.blocksBlast(entity, center);
		// a Stalwart tank (Sigma) is pushed 40% less (dev.overcraft.hero.Tank)
		return original.call(map, player, blocked ? Vec3.ZERO
			: impulse instanceof Vec3 v && player instanceof Entity entity ? v.scale(dev.overcraft.hero.Tank.knockbackFactor(entity)) : impulse);
	}
}
