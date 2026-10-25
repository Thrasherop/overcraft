package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hero mode: Overwatch's keys. The sneak key (Shift) is an ability, so it doesn't sneak; the sprint key (Ctrl) crouches,
 * as in Overwatch, and nothing sprints.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void overcraft$heroKeys(CallbackInfo ci) {
		if (!ClientHero.controls()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		boolean shiftIsAbility = ClientHero.sharesKey(mc.options.keyShift);
		boolean crouch = mc.options.keySprint.isDown() || (!shiftIsAbility && mc.options.keyShift.isDown());
		Input k = this.keyPresses;
		this.keyPresses = new Input(k.forward(), k.backward(), k.left(), k.right(), k.jump(), crouch, false);
	}
}
