package dev.overcraft.mixin;

import dev.overcraft.mercy.Souls;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mob Mercy resurrected (Souls.RESURRECTED) gave its loot table and experience at its first death: its later deaths drop
 * neither (#98). What it carries (equipment, a chest) still drops: that is real, and it doesn't come back on it.
 */
@Mixin(LivingEntity.class)
public abstract class ResurrectedLootMixin {
	@Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
		at = @At("HEAD"), cancellable = true)
	private void overcraft$lootOnce(ServerLevel level, DamageSource source, boolean playerKilled, CallbackInfo ci) {
		if (Souls.lootGiven((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
	private void overcraft$experienceOnce(ServerLevel level, Entity killer, CallbackInfo ci) {
		if (Souls.lootGiven((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}
}
