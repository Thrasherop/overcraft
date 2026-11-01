package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client, to a Sombra: the enemy players whose ultimate is ready (Conspirator: dev.overcraft.sombra.Conspirator),
 * by entity id; empty when there are none or she isn't Sombra any more.
 */
public record ConspiratorPayload(int[] enemies) implements CustomPacketPayload {
	private static final int MAX = 64;

	public static final Type<ConspiratorPayload> TYPE = new Type<>(Overcraft.id("conspirator"));
	public static final StreamCodec<FriendlyByteBuf, ConspiratorPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeVarInt(Math.min(MAX, p.enemies.length));
			for (int i = 0; i < Math.min(MAX, p.enemies.length); i++) {
				buf.writeVarInt(p.enemies[i]);
			}
		}, buf -> {
			int[] enemies = new int[Math.min(MAX, buf.readVarInt())];
			for (int i = 0; i < enemies.length; i++) {
				enemies[i] = buf.readVarInt();
			}
			return new ConspiratorPayload(enemies);
		});

	@Override
	public Type<ConspiratorPayload> type() {
		return TYPE;
	}
}
