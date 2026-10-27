package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player switched hero mode on or off (the G key, #26). */
public record HeroModePayload(boolean on) implements CustomPacketPayload {
	public static final Type<HeroModePayload> TYPE = new Type<>(Overcraft.id("hero_mode"));
	public static final StreamCodec<FriendlyByteBuf, HeroModePayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeBoolean(p.on), buf -> new HeroModePayload(buf.readBoolean()));

	@Override
	public Type<HeroModePayload> type() {
		return TYPE;
	}
}
