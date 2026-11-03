package dev.overcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overcraft.brigitte.Barrier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Explosions and Brigitte's barrier (dev.overcraft.brigitte.Barrier). Each entity's hit is dealt with the blast's centre
 * and the explosion known to the barrier: a creeper's blast was judged from the creeper's eyes, and from its centre a
 * blast is charged to the barrier once however many stand behind it (#88). One the barrier stopped pushes no one either:
 * Minecraft pushes from an explosion apart from its damage, so a creeper's blast the barrier took whole still threw her
 * 4 m back (found playing). Others' push (pushFromExplosion), and a player's, which their own client applies (hitPlayers).
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private Vec3 center;

	/** Who this blast's hit was stopped for (by a barrier): their push is stopped too. */
	@Unique
	private final Set<Entity> overcraft$shielded = Collections.newSetFromMap(new IdentityHashMap<>());

	@WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean overcraft$barrierHit(Entity entity, ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		Barrier.blast = center;
		Barrier.blastKey = this;
		Barrier.blocked = false;
		try {
			return original.call(entity, level, source, damage);
		} finally {
			if (Barrier.blocked) {
				overcraft$shielded.add(entity);
			}
			Barrier.blast = null;
			Barrier.blastKey = null;
			Barrier.blocked = false;
		}
	}

	@WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pushFromExplosion(Lnet/minecraft/world/phys/Vec3;)V"))
	private void overcraft$barrierPush(Entity entity, Vec3 impulse, Operation<Void> original) {
		original.call(entity, overcraft$shielded.contains(entity) ? Vec3.ZERO : impulse);
	}

	@WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object overcraft$barrierPlayerPush(Map<Object, Object> map, Object player, Object impulse, Operation<Object> original) {
		return original.call(map, player, player instanceof Entity entity && overcraft$shielded.contains(entity) ? Vec3.ZERO : impulse);
	}
}
