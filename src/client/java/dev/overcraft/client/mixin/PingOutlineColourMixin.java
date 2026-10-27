package dev.overcraft.client.mixin;

import dev.overcraft.client.comms.Pings;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A pinged thing's outline is its ping's colour (PingOutlineMixin; the outline takes the entity's team colour): red on
 * an enemy, blue on a notable thing.
 */
@Mixin(Entity.class)
public abstract class PingOutlineColourMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void overcraft$pingColour(CallbackInfoReturnable<Integer> cir) {
		int colour = Pings.outlineColour((Entity) (Object) this);
		if (colour >= 0) {
			cir.setReturnValue(colour);
		}
	}
}
