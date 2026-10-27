package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: play one of the hero's emotes (its id: the model's emote_<id>_<n> clips), or end it (no id). */
public record EmotePayload(String id) implements CustomPacketPayload {
	public static final Type<EmotePayload> TYPE = new Type<>(Overcraft.id("emote"));
	public static final StreamCodec<FriendlyByteBuf, EmotePayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeUtf(p.id, 48), buf -> new EmotePayload(buf.readUtf(48)));

	@Override
	public Type<EmotePayload> type() {
		return TYPE;
	}
}
