package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: a Mercy has resurrected you. The client respawns as its death screen's button would (the only
 * respawn Minecraft has), and the server puts the player back where they fell (dev.overcraft.mercy.Souls).
 */
public record ResurrectPayload() implements CustomPacketPayload {
	public static final Type<ResurrectPayload> TYPE = new Type<>(Overcraft.id("resurrect"));
	public static final StreamCodec<FriendlyByteBuf, ResurrectPayload> CODEC = CustomPacketPayload.codec((p, buf) -> {
	}, buf -> new ResurrectPayload());

	@Override
	public Type<ResurrectPayload> type() {
		return TYPE;
	}
}
