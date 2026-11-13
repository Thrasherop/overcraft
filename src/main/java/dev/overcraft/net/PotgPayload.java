package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to a viewer of a Play of the Game or a highlight (PotgReplay, techniques/potg/potg_look.md): CARD starts the
 * POTG (the lineup, the hero's intro, then the play), SOLO a highlight watched alone (the intro, then the play), CUT says
 * the play has ended (the hard cut to black, then the end screen for a POTG), END that the viewer is back in their own
 * world. Between CARD/SOLO and the end screen's close the client is in a showing (ClientPotg): no Hero Select, no join
 * toasts, no world input. data is JSON: the lineup, the player card and the end screen's cards (PotgReplay.showData).
 */
public record PotgPayload(int phase, String player, String hero, String category, float cardSeconds, String data)
	implements CustomPacketPayload {
	public static final int CARD = 0;
	public static final int SOLO = 1;
	public static final int END = 2;
	public static final int CUT = 3;

	public PotgPayload(int phase, String player, String hero, String category, float cardSeconds) {
		this(phase, player, hero, category, cardSeconds, "");
	}

	public static final Type<PotgPayload> TYPE = new Type<>(Overcraft.id("potg"));
	public static final StreamCodec<FriendlyByteBuf, PotgPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeVarInt(p.phase);
			buf.writeUtf(p.player, 64);
			buf.writeUtf(p.hero, 64);
			buf.writeUtf(p.category, 64);
			buf.writeFloat(p.cardSeconds);
			buf.writeUtf(p.data, 32767);
		}, buf -> new PotgPayload(buf.readVarInt(), buf.readUtf(64), buf.readUtf(64), buf.readUtf(64), buf.readFloat(),
			buf.readUtf(32767)));

	@Override
	public Type<PotgPayload> type() {
		return TYPE;
	}
}
