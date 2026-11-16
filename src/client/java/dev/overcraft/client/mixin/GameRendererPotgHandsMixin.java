package dev.overcraft.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Play of the Game's replay (#155): the viewer is a spectator whose camera follows the scorer, and Minecraft draws no
 * first-person hands for a spectator. Following a scorer, the hands are drawn (FirstPersonHandsMixin then draws the
 * scorer's hero's arms: HeroModelRenderer.viewed).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererPotgHandsMixin {
	@ModifyExpressionValue(method = "renderItemInHand", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;"))
	private GameType overcraft$scorerHands(GameType mode) {
		return mode == GameType.SPECTATOR && dev.overcraft.client.ClientPotg.followed() != null ? GameType.SURVIVAL : mode;
	}
}
