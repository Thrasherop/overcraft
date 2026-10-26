package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: play an ability effect (the client draws it from Overwatch's effect textures, client/effect).
 * effect is the effect's name ("ofuda_hit_ally"); x, y, z where; dx, dy, dz a direction (a surface's normal, a
 * throw's heading) or zero; entity the entity it belongs to (-1 for none); variant picks a colour or size
 * (0 = normal; for the ofuda 1 = homing gold); enemy whether the hero who made it is this player's enemy (Overwatch
 * shows an enemy's effects in red).
 */
public record EffectPayload(String effect, double x, double y, double z, float dx, float dy, float dz, int entity,
							int variant, boolean enemy) implements CustomPacketPayload {
	public static final Type<EffectPayload> TYPE = new Type<>(Overcraft.id("effect"));
	public static final StreamCodec<FriendlyByteBuf, EffectPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeUtf(p.effect, 64);
			buf.writeDouble(p.x);
			buf.writeDouble(p.y);
			buf.writeDouble(p.z);
			buf.writeFloat(p.dx);
			buf.writeFloat(p.dy);
			buf.writeFloat(p.dz);
			buf.writeVarInt(p.entity);
			buf.writeVarInt(p.variant);
			buf.writeBoolean(p.enemy);
		}, buf -> new EffectPayload(buf.readUtf(64), buf.readDouble(), buf.readDouble(), buf.readDouble(),
			buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

	@Override
	public Type<EffectPayload> type() {
		return TYPE;
	}
}
