package dev.overcraft.mixin;

import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A hero's own body box and eye height in hero mode (dev.overcraft.hero.HeroBody: Sigma's 1.4 x 2.5 m). */
@Mixin(Avatar.class)
public abstract class AvatarMixin {
	@Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
	private void overcraft$heroBody(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if ((Object) this instanceof Player player) {
			EntityDimensions body = dev.overcraft.hero.HeroBody.current(player, pose);
			if (body != null) {
				cir.setReturnValue(body);
			}
		}
	}
}
