package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A ping (#40, dev.overcraft.comms.Pings). Client to server: the player pings (or cancels theirs: kind CANCEL). Server
 * to client: a teammate's ping to show (owner is their entity id). Kind: SPOT, WARNING (a double press), ENEMY (on an
 * enemy: entity is theirs, followed while it lasts). Position in the world; entity -1 for none.
 */
public record PingPayload(int owner, int kind, double x, double y, double z, int entity) implements CustomPacketPayload {
	public static final int CANCEL = -1;
	public static final int SPOT = 0;
	public static final int WARNING = 1;
	public static final int ENEMY = 2;

	public static final Type<PingPayload> TYPE = new Type<>(Overcraft.id("ping"));
	public static final StreamCodec<FriendlyByteBuf, PingPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeVarInt(p.owner);
			buf.writeByte(p.kind);
			buf.writeDouble(p.x);
			buf.writeDouble(p.y);
			buf.writeDouble(p.z);
			buf.writeVarInt(p.entity);
		}, buf -> new PingPayload(buf.readVarInt(), buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
			buf.readVarInt()));

	@Override
	public Type<PingPayload> type() {
		return TYPE;
	}
}
