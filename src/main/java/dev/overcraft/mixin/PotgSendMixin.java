package dev.overcraft.mixin;

import dev.overcraft.potg.PotgTap;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game's tap (PotgTap, dev only): every packet sent to a player in play passes here; when armed it's recorded
 * and goes on unchanged. Only packets made for a keyframe are held back, and only on the thread making it.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class PotgSendMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
	private void overcraft$tap(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
		if ((Object) this instanceof ServerGamePacketListenerImpl game && PotgTap.onSend(game.player, packet)) {
			ci.cancel();
		}
	}
}