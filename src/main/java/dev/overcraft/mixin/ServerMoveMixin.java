package dev.overcraft.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Each move packet handled on the server's thread: Sojourn's Charged Shot keeps her last real step from it (Railgun.moved). Sampled once a tick, two
 * moves in one tick (below 20 fps, jitter, a hitch) made the step two long (the critic, #126).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMoveMixin {
	@Shadow
	public ServerPlayer player;

	/** Tracer's Blink: the server's straight replay of her move mustn't set her back (#152, Tracer.beforeMove). */
	@Inject(method = "handleMovePlayer", at = @At("HEAD"))
	private void overcraft$blink(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
		if (packet.hasPosition() && player.level().getServer().isSameThread()) {
			dev.overcraft.tracer.Tracer.beforeMove(player, new net.minecraft.world.phys.Vec3(packet.getX(player.getX()),
				packet.getY(player.getY()), packet.getZ(player.getZ())));
		}
	}

	@Inject(method = "handleMovePlayer", at = @At("RETURN"))
	private void overcraft$moved(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
		dev.overcraft.sojourn.Railgun.moved(player);
	}
}
