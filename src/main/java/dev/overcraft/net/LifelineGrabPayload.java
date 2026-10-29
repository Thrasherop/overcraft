package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: a player ally took a Jetpack Cat's Lifeline (Interact on its prompt, as Overwatch's allies opt in:
 * her graph 5A8F's prompt on graph 3A94), by her entity id. The server checks she still seeks and they're in reach.
 */
public record LifelineGrabPayload(int cat) implements CustomPacketPayload {
	public static final Type<LifelineGrabPayload> TYPE = new Type<>(Overcraft.id("lifeline_grab"));
	public static final StreamCodec<FriendlyByteBuf, LifelineGrabPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> buf.writeVarInt(p.cat), buf -> new LifelineGrabPayload(buf.readVarInt()));

	@Override
	public Type<LifelineGrabPayload> type() {
		return TYPE;
	}
}
