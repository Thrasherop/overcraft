package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The Highlights menu (techniques/potg/potg_look.md), both ways. Client to server: LIST asks for the player's own
 * highlights, WATCH plays the one whose epochMs is in data alone (PotgReplay.watchHighlight), DONE says this client's
 * showing is over (its end screen played out, its input back: the server's hold waits for it). Server to client: the list
 * as JSON in data ("locked" while the world's dragon hasn't been killed).
 */
public record HighlightsPayload(int watch, String data) implements CustomPacketPayload {
	public static final int LIST = -1;
	public static final int WATCH = 0;
	public static final int DONE = -2;

	public static final Type<HighlightsPayload> TYPE = new Type<>(Overcraft.id("highlights"));
	public static final StreamCodec<FriendlyByteBuf, HighlightsPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeVarInt(p.watch);
			buf.writeUtf(p.data, 32767);
		}, buf -> new HighlightsPayload(buf.readVarInt(), buf.readUtf(32767)));

	@Override
	public Type<HighlightsPayload> type() {
		return TYPE;
	}
}
