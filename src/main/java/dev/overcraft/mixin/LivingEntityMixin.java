package dev.overcraft.mixin;

import dev.overcraft.Ow;
import dev.overcraft.hero.OwMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Overwatch movement and jump for a hero in slot 1 (see {@link OwMovement}). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "travelInAir", at = @At("HEAD"), cancellable = true)
	private void overcraft$owTravel(Vec3 input, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (OwMovement.handles(self)) {
			OwMovement.travel(self, input);
			ci.cancel();
		}
	}

	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void overcraft$owJump(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (OwMovement.handles(self)) {
			Vec3 v = self.getDeltaMovement();
			self.setDeltaMovement(v.x, Math.max(Ow.speed(OwMovement.JUMP_SPEED), v.y), v.z);
			ci.cancel();
		}
	}
}
