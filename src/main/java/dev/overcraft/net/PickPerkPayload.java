package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the perk picked (its Perk ordinal) once one is ready (#36). */
public record PickPerkPayload(int perk) implements CustomPacketPayload {
	public static final Type<PickPerkPayload> TYPE = new Type<>(Overcraft.id("pick_perk"));
	public static final StreamCodec<FriendlyByteBuf, PickPerkPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeVarInt(p.perk), buf -> new PickPerkPayload(buf.readVarInt()));

	@Override
	public Type<PickPerkPayload> type() {
		return TYPE;
	}
}
