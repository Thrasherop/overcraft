package dev.overcraft.mixin;

import dev.overcraft.potg.PotgTap;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game's tap (PotgTap, dev only): every packet sent to a player in play passes here; when armed it's recorded
 * and goes on unchanged. Only packets made for a keyframe are held back, and only on the thread making it.
 * It sits on Connection, not ServerCommonPacketListenerImpl.send (the design's first guess): Fabric's PacketSender sends
 * straight to the Connection (AbstractChanneledNetworkAddon.sendPacket), so its attachment bundles went round the
 * listener's send (found in the first run, 2026-11-11).
 */
@Mixin(Connection.class)
public abstract class PotgSendMixin {
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
	private void overcraft$tap(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
		if (((Connection) (Object) this).getPacketListener() instanceof ServerGamePacketListenerImpl game && PotgTap.onSend(game.player, packet)) {
			ci.cancel();
		}
	}

	/** The leak check's other end: what a client's connection received (PotgTap.onReceived). */
	@Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
	private void overcraft$received(io.netty.channel.ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
		if (packet.type().flow() == net.minecraft.network.protocol.PacketFlow.CLIENTBOUND) {
			PotgTap.onReceived(packet);
		}
	}
}