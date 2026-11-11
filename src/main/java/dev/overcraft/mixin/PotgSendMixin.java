package dev.overcraft.mixin;

import dev.overcraft.potg.ReplayTap;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game's tap (ReplayTap): every packet sent to a player in play passes here, and is recorded for that
 * player's replay recorder. It sits on Connection, not ServerCommonPacketListenerImpl.send (where Arcade's recorder
 * listens): Fabric's PacketSender sends straight to the Connection (AbstractChanneledNetworkAddon.sendPacket), so its
 * attachment bundles went round the listener's send (step 1, 2026-11-11; the spike again). Its other end drops what a
 * replay's viewer sends (input), as Arcade's serverbound event did.
 */
@Mixin(value = Connection.class, priority = 5000)
public abstract class PotgSendMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
	private void overcraft$tap(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
		if (ReplayTap.onSend((Connection) (Object) this, packet)) {
			ci.cancel();
		}
	}

	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void overcraft$received(io.netty.channel.ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
		if (ReplayTap.dropReceived((Connection) (Object) this, packet)) {
			ci.cancel();
		}
	}
}
