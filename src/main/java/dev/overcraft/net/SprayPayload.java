package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The client asks to put down a spray (#41): its name for the player's current hero (the server checks the hero). */
public record SprayPayload(String name) implements CustomPacketPayload {
	public static final Type<SprayPayload> TYPE = new Type<>(Overcraft.id("spray"));
	public static final StreamCodec<FriendlyByteBuf, SprayPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeUtf(p.name, 64), buf -> new SprayPayload(buf.readUtf(64)));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
