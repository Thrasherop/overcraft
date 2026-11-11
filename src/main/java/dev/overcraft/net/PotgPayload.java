package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to a viewer of a Play of the Game or a highlight (PotgReplay): phase CARD starts the showing (the card, the
 * announcer's line and the music for the POTG; for a solo highlight no card: the hero's intro goes here, the next step),
 * phase END says the viewer is back in their own world. Between them the client is in a showing (ClientPotg): no Hero
 * Select, no join toasts, no world input.
 */
public record PotgPayload(int phase, String player, String hero, String category, float cardSeconds) implements CustomPacketPayload {
	public static final int CARD = 0;
	public static final int SOLO = 1;
	public static final int END = 2;

	public static final Type<PotgPayload> TYPE = new Type<>(Overcraft.id("potg"));
	public static final StreamCodec<FriendlyByteBuf, PotgPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeVarInt(p.phase);
			buf.writeUtf(p.player, 64);
			buf.writeUtf(p.hero, 64);
			buf.writeUtf(p.category, 64);
			buf.writeFloat(p.cardSeconds);
		}, buf -> new PotgPayload(buf.readVarInt(), buf.readUtf(64), buf.readUtf(64), buf.readUtf(64), buf.readFloat()));

	@Override
	public Type<PotgPayload> type() {
		return TYPE;
	}
}
