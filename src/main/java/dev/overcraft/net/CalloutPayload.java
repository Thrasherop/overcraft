package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a callout from the communication wheel (its Callout ordinal). */
public record CalloutPayload(int callout) implements CustomPacketPayload {
	public static final Type<CalloutPayload> TYPE = new Type<>(Overcraft.id("callout"));
	public static final StreamCodec<FriendlyByteBuf, CalloutPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeVarInt(p.callout), buf -> new CalloutPayload(buf.readVarInt()));

	@Override
	public Type<CalloutPayload> type() {
		return TYPE;
	}
}
