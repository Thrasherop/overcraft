package dev.overcraft.client.mixin;

import dev.overcraft.client.PotgMusic;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The end of the match's music runs on through a level change (PotgMusic, #161): Minecraft stops every sound when its level
 * changes (the replay's dimension, the way back), which cut the music the moment a POTG took a player elsewhere. While the
 * track plays, the world's sounds stop as before and the interface's (the track, the announcer) go on.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftSoundKeepMixin {
	@ModifyVariable(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"), argsOnly = true)
	private boolean overcraft$keepPotgMusic(boolean stopSound, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.client.multiplayer.ClientLevel level) {
		// a new level (the replay's, the way back), not leaving the world (disconnect passes none: the critique's 4)
		// the dragon fight's end-phase music too (#45: everyone hears it, wherever they go), only while it should play
		if (stopSound && level != null && (PotgMusic.playing() || dev.overcraft.client.EndMusic.playing())) {
			PotgMusic.stopAllButInterface();
			return false;
		}
		return stopSound;
	}
}
