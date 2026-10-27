package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: play one of the hero's voice lines (its id in the local pack's voicelines/<hero>.json). */
public record VoiceLinePayload(String id) implements CustomPacketPayload {
	public static final Type<VoiceLinePayload> TYPE = new Type<>(Overcraft.id("voice_line"));
	public static final StreamCodec<FriendlyByteBuf, VoiceLinePayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeUtf(p.id, 64), buf -> new VoiceLinePayload(buf.readUtf(64)));

	@Override
	public Type<VoiceLinePayload> type() {
		return TYPE;
	}
}
