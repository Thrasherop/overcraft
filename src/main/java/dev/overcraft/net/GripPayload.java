package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: a Lifeweaver's Life Grip has you (on), pulling you to (x, y, z), or let you go (off). far: you were
 * more than 15 m from there (the slow start and the ramp). Your client moves you (dev.overcraft.lifeweaver.LifeGrip):
 * held still until the pull starts, then along the speed curve to the point.
 */
public record GripPayload(boolean on, double x, double y, double z, boolean far) implements CustomPacketPayload {
	public static final GripPayload OFF = new GripPayload(false, 0, 0, 0, false);

	public static final Type<GripPayload> TYPE = new Type<>(Overcraft.id("life_grip"));
	public static final StreamCodec<FriendlyByteBuf, GripPayload> CODEC = CustomPacketPayload.codec((p, buf) -> {
		buf.writeBoolean(p.on);
		buf.writeDouble(p.x);
		buf.writeDouble(p.y);
		buf.writeDouble(p.z);
		buf.writeBoolean(p.far);
	}, buf -> new GripPayload(buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean()));

	@Override
	public Type<GripPayload> type() {
		return TYPE;
	}
}
