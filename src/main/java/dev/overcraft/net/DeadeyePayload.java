package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client, to a Cassidy in Deadeye (dev.overcraft.cassidy.Cassidy): his targets by entity id and each one's charge as a
 * share of its health (1 or more: the shot kills); empty when Deadeye is over. Every tick of the channel.
 */
public record DeadeyePayload(int[] targets, float[] share) implements CustomPacketPayload {
	private static final int MAX = 64;

	public static final Type<DeadeyePayload> TYPE = new Type<>(Overcraft.id("deadeye"));
	public static final StreamCodec<FriendlyByteBuf, DeadeyePayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			int n = Math.min(MAX, Math.min(p.targets.length, p.share.length));
			buf.writeVarInt(n);
			for (int i = 0; i < n; i++) {
				buf.writeVarInt(p.targets[i]);
				buf.writeFloat(p.share[i]);
			}
		}, buf -> {
			int n = Math.min(MAX, buf.readVarInt());
			int[] targets = new int[n];
			float[] share = new float[n];
			for (int i = 0; i < n; i++) {
				targets[i] = buf.readVarInt();
				share[i] = buf.readFloat();
			}
			return new DeadeyePayload(targets, share);
		});

	@Override
	public Type<DeadeyePayload> type() {
		return TYPE;
	}
}
