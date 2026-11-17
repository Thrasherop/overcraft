package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the dragon fight's end-phase music (#45, dev.overcraft.potg.EndPhase). {@code share} is the dragon's
 * health share (0-1), which the music's volume follows, or -1: the music stops (the dragon is gone without dying: it
 * comes back on the loop if the same fight goes on). {@code fight} tells one dragon from the next: a new one starts the
 * music from its intro.
 */
public record EndPhasePayload(float share, int fight) implements CustomPacketPayload {
	public static final Type<EndPhasePayload> TYPE = new Type<>(Overcraft.id("end_phase"));
	public static final StreamCodec<FriendlyByteBuf, EndPhasePayload> CODEC = CustomPacketPayload.codec((p, buf) -> {
		buf.writeFloat(p.share);
		buf.writeInt(p.fight);
	}, buf -> new EndPhasePayload(buf.readFloat(), buf.readInt()));

	@Override
	public Type<EndPhasePayload> type() {
		return TYPE;
	}
}
