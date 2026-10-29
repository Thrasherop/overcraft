package dev.overcraft.client.mixin;

import dev.overcraft.client.comms.EmoteCamera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** While the hero emotes or an ability shows them from behind, no view change (F5) reaches first person. */
@Mixin(Options.class)
public abstract class OptionsCameraMixin {
	@ModifyVariable(method = "setCameraType", at = @At("HEAD"), argsOnly = true)
	private CameraType overcraft$emoteStaysOut(CameraType type) {
		return dev.overcraft.client.AbilityCamera.allowed(EmoteCamera.allowed(type));
	}
}
