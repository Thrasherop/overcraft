package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hero mode moves with the hero's keys (HeroControls: Overwatch's WASD, Space, Ctrl to crouch), not Minecraft's. Nothing
 * sprints: heroes have one speed.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void overcraft$heroKeys(CallbackInfo ci) {
		var player = net.minecraft.client.Minecraft.getInstance().player;
		if (player != null && (Boolean.TRUE.equals(player.getAttached(dev.overcraft.hero.HeroManager.ASLEEP))
			|| dev.overcraft.dva.Dva.held(player))) {
			// (D.Va calling her mech, and its wreck: no moving, no jumping either: D 026A #105, 0276 #79)
			// asleep (Ana's Sleep Dart: dev.overcraft.hero.Sleep): no moving, no jumping, hero or not
			this.keyPresses = Input.EMPTY;
			this.moveVector = Vec2.ZERO;
			return;
		}
		if (!ClientHero.controls()) {
			return;
		}
		boolean forward = HeroControls.get(Action.MOVE_FORWARD).isDown();
		boolean back = HeroControls.get(Action.MOVE_BACK).isDown();
		boolean left = HeroControls.get(Action.MOVE_LEFT).isDown();
		boolean right = HeroControls.get(Action.MOVE_RIGHT).isDown();
		// D.Va's mech can't crouch (no crouch in any mech graph: D 0276 #246); her crouch key does nothing in it
		boolean crouch = HeroControls.get(Action.CROUCH).isDown() && !dev.overcraft.dva.Dva.mechBody(player);
		// Cassidy in Deadeye: no jump, no crouch (W rev 409375: "cannot jump, crouch or use other abilities while channeling")
		boolean deadeye = dev.overcraft.client.ClientCassidy.inDeadeye();
		this.keyPresses = new Input(forward, back, left, right, HeroControls.get(Action.JUMP).isDown() && !deadeye, crouch && !deadeye, false);
		this.moveVector = new Vec2(impulse(left, right), impulse(forward, back)).normalized();
	}

	private static float impulse(boolean positive, boolean negative) {
		return positive == negative ? 0 : positive ? 1 : -1;
	}
}
