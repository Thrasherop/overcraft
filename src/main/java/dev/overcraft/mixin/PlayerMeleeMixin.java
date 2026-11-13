package dev.overcraft.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player's own blow (Minecraft mode's punch or sword): melee, through Brigitte's barrier (Barrier.isMelee). */
@Mixin(Player.class)
public abstract class PlayerMeleeMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void overcraft$meleeStart(Entity entity, CallbackInfo ci) {
		dev.overcraft.brigitte.Barrier.meleeDepth++;
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void overcraft$meleeEnd(Entity entity, CallbackInfo ci) {
		dev.overcraft.brigitte.Barrier.meleeDepth--;
	}
}
