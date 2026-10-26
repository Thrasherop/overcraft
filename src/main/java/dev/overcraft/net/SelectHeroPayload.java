package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the hero picked on the select screen ("" for none). */
public record SelectHeroPayload(String heroId) implements CustomPacketPayload {
	public static final Type<SelectHeroPayload> TYPE = new Type<>(Overcraft.id("select_hero"));
	public static final StreamCodec<FriendlyByteBuf, SelectHeroPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeUtf(p.heroId, 32), buf -> new SelectHeroPayload(buf.readUtf(32)));

	@Override
	public Type<SelectHeroPayload> type() {
		return TYPE;
	}
}
