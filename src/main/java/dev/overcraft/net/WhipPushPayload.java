package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: Whip Shot's push has you: hold this level speed (blocks a tick) for the push's time (dev.overcraft.brigitte.BrigitteStats), your
 * own vertical speed kept; or let go (off: a wall slam, a phase). Your client moves you: the server's copy of a player's vertical speed is stale, so it
 * can't send the hold itself each tick.
 */
public record WhipPushPayload(boolean on, double x, double z) implements CustomPacketPayload {
	public static final WhipPushPayload OFF = new WhipPushPayload(false, 0, 0);

	public static final Type<WhipPushPayload> TYPE = new Type<>(Overcraft.id("whip_push"));
	public static final StreamCodec<FriendlyByteBuf, WhipPushPayload> CODEC = CustomPacketPayload.codec((p, buf) -> {
		buf.writeBoolean(p.on);
		buf.writeDouble(p.x);
		buf.writeDouble(p.z);
	}, buf -> new WhipPushPayload(buf.readBoolean(), buf.readDouble(), buf.readDouble()));

	@Override
	public Type<WhipPushPayload> type() {
		return TYPE;
	}
}
