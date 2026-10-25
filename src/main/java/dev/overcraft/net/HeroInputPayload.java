package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, hero controls (only while hotbar slot 1 is selected): which fire buttons are held, and the
 * ability keys pressed since the last one.
 */
public record HeroInputPayload(int held, int pressed) implements CustomPacketPayload {
	public static final int HELD_PRIMARY = 1;
	public static final int HELD_SECONDARY = 2;

	public static final int PRESS_ABILITY_1 = 1;
	public static final int PRESS_ABILITY_2 = 2;
	public static final int PRESS_ULTIMATE = 4;
	public static final int PRESS_RELOAD = 8;

	public static final Type<HeroInputPayload> TYPE = new Type<>(Overcraft.id("hero_input"));
	public static final StreamCodec<FriendlyByteBuf, HeroInputPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeByte(p.held);
			buf.writeByte(p.pressed);
		}, buf -> new HeroInputPayload(buf.readByte(), buf.readByte()));

	@Override
	public Type<HeroInputPayload> type() {
		return TYPE;
	}
}
