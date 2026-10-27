package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: join this scoreboard team ("" for none), from the Overwatch menu's team picker (Teams). */
public record TeamPayload(String team) implements CustomPacketPayload {
	public static final Type<TeamPayload> TYPE = new Type<>(Overcraft.id("team"));
	public static final StreamCodec<FriendlyByteBuf, TeamPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeUtf(p.team, 64), buf -> new TeamPayload(buf.readUtf(64)));

	@Override
	public Type<TeamPayload> type() {
		return TYPE;
	}
}
