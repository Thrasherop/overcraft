package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the match is won, show Overwatch's VICTORY (sent to everyone in the End when the Ender Dragon dies,
 * issue #18).
 */
public record VictoryPayload() implements CustomPacketPayload {
	public static final Type<VictoryPayload> TYPE = new Type<>(Overcraft.id("victory"));
	public static final StreamCodec<FriendlyByteBuf, VictoryPayload> CODEC = CustomPacketPayload.codec((p, buf) -> {
	}, buf -> new VictoryPayload());

	@Override
	public Type<VictoryPayload> type() {
		return TYPE;
	}
}
