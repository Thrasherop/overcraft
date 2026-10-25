package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to clients near a hero (and the hero): they did something with an animation (a throw, a cast). */
public record HeroActionPayload(int entityId, String clip) implements CustomPacketPayload {
	public static final Type<HeroActionPayload> TYPE = new Type<>(Overcraft.id("hero_action"));
	public static final StreamCodec<FriendlyByteBuf, HeroActionPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeVarInt(p.entityId);
			buf.writeUtf(p.clip, 64);
		}, buf -> new HeroActionPayload(buf.readVarInt(), buf.readUtf(64)));

	@Override
	public Type<HeroActionPayload> type() {
		return TYPE;
	}
}
