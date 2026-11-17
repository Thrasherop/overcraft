package dev.overcraft.client.mixin;

import dev.overcraft.client.EndMusic;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One music at a time (#45, ours): while the dragon fight's end-phase music plays (EndMusic), Minecraft's own (the End's
 * boss music) stops and waits, rather than playing over it. It picks up again by itself once ours is over.
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
	@Shadow
	private @Nullable SoundInstance currentMusic;

	@Shadow
	public abstract void stopPlaying();

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void overcraft$giveWayToEndMusic(CallbackInfo ci) {
		if (EndMusic.playing()) {
			if (currentMusic != null) {
				stopPlaying();
			}
			ci.cancel();
		}
	}
}
