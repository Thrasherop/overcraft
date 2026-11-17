package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: open the hero select screen (an operator's /changehero with no hero, #165). */
public record OpenSelectPayload() implements CustomPacketPayload {
	public static final Type<OpenSelectPayload> TYPE = new Type<>(Overcraft.id("open_select"));
	public static final StreamCodec<FriendlyByteBuf, OpenSelectPayload> CODEC = CustomPacketPayload.codec((p, buf) -> {
	}, buf -> new OpenSelectPayload());

	@Override
	public Type<OpenSelectPayload> type() {
		return TYPE;
	}
}
