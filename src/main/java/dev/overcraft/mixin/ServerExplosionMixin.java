package dev.overcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.overcraft.brigitte.Barrier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Explosions and Brigitte's barrier (dev.overcraft.brigitte.Barrier). Before the blast hurts anyone, each barrier in
 * front of it takes it once, by its own distance (Barrier.blast); while it hurts them, its centre, the explosion and who
 * set it off are known to Barrier, so a hit behind a barrier is 0 (a creeper's blast had been judged from the creeper's
 * eyes, #88). Nobody behind a barrier is pushed either, judged by where they are, not by whether they were hurt (a
 * breeze's wind charge hurts no one): Minecraft pushes from an explosion apart from its damage, so a creeper's blast the
 * barrier took whole still threw her 4 m back (found playing). Others' push (pushFromExplosion), and a player's, which
 * their own client applies (hitPlayers).
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private Vec3 center;
	@Shadow
	@Final
	private float radius;
	@Shadow
	@Final
	private ServerLevel level;
	@Shadow
	@Final
	private @Nullable Entity source;
	@Shadow
	@Final
	private ExplosionDamageCalculator damageCalculator;

	/** The blast state of an explosion set off while another was being dealt (an end crystal's chain), put back after. */
	@Unique
	private static final Deque<Object[]> overcraft$outer = new ArrayDeque<>();

	@Inject(method = "hurtEntities", at = @At("HEAD"))
	private void overcraft$barrierBlast(CallbackInfo ci) {
		overcraft$outer.push(new Object[] {Barrier.blast, Barrier.blastKey, Barrier.blastCause});
		Entity cause = overcraft$cause();
		Barrier.blast = center;
		Barrier.blastKey = this;
		Barrier.blastCause = cause;
		// a wind charge's calculator hurts no one: its barrier takes nothing either
		boolean hurts = !level.players().isEmpty()
			&& damageCalculator.shouldDamageEntity((ServerExplosion) (Object) this, level.players().getFirst());
		Barrier.blast(level, center, radius, cause, hurts);
		// the barriers that are entities (Sigma's) take it the same way, by where they stand
		dev.overcraft.hero.Barriers.blast(level, center, radius, cause, hurts);
	}

	@Inject(method = "hurtEntities", at = @At("RETURN"))
	private void overcraft$barrierBlastEnd(CallbackInfo ci) {
		Object[] outer = overcraft$outer.isEmpty() ? new Object[3] : overcraft$outer.pop();
		Barrier.blast = (Vec3) outer[0];
		Barrier.blastKey = outer[1];
		Barrier.blastCause = (Entity) outer[2];
	}

	/** Who set it off: a creeper, a ghast's fireball's ghast, TNT's lighter. */
	@Unique
	private @Nullable Entity overcraft$cause() {
		if (source instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() != null) {
			return p.getOwner();
		}
		if (source instanceof net.minecraft.world.entity.item.PrimedTnt tnt) {
			return tnt.getOwner();
		}
		return source;
	}

	@WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pushFromExplosion(Lnet/minecraft/world/phys/Vec3;)V"))
	private void overcraft$barrierPush(Entity entity, Vec3 impulse, Operation<Void> original) {
		// a Stalwart tank (Sigma) is pushed 40% less (dev.overcraft.hero.Tank), half that again flying in his Gravitic Flux
		original.call(entity, Barrier.blocksBlast(entity, center, overcraft$cause()) ? Vec3.ZERO
			: impulse.scale(dev.overcraft.hero.Tank.knockbackFactor(entity) * dev.overcraft.sigma.GraviticFlux.knockbackFactor(entity)));
	}

	@WrapOperation(method = "hurtEntities", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object overcraft$barrierPlayerPush(Map<Object, Object> map, Object player, Object impulse, Operation<Object> original) {
		boolean blocked = player instanceof Entity entity && Barrier.blocksBlast(entity, center, overcraft$cause());
		return original.call(map, player, blocked ? Vec3.ZERO
			: impulse instanceof Vec3 v && player instanceof Entity entity ? v.scale(dev.overcraft.hero.Tank.knockbackFactor(entity)
				* dev.overcraft.sigma.GraviticFlux.knockbackFactor(entity)) : impulse);
	}
}
