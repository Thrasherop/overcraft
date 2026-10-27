package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A ping (#40, dev.overcraft.comms.Pings). Client to server: the player pings (or cancels theirs: kind CANCEL). Server
 * to client: a teammate's ping to show (owner is their entity id). Kind: SPOT, WARNING (a double press), ENEMY (on an
 * enemy: entity is theirs, followed while it lasts), and the notable things (owner, 2026-10-27): ITEM (a dropped item,
 * followed), THING (any other creature or vehicle that isn't an enemy: villagers, pets, animals, boats; followed),
 * BLOCK (a notable block: chests, ores, spawners, portals...: the overcraft:notable_pings tag), STRUCTURE (a generated
 * structure seen from outside it; only the server makes these). Position in the world; entity -1 for none. What: the
 * thing's registry id (an entity type, an item, a block or a structure), so a teammate too far away to have the thing
 * loaded can still name it; empty for spots.
 */
public record PingPayload(int owner, int kind, double x, double y, double z, int entity, String what)
	implements CustomPacketPayload {
	public static final int CANCEL = -1;
	public static final int SPOT = 0;
	public static final int WARNING = 1;
	public static final int ENEMY = 2;
	public static final int ITEM = 3;
	public static final int THING = 4;
	public static final int BLOCK = 5;
	public static final int STRUCTURE = 6;

	private static final int WHAT_MAX = 256;

	public static final Type<PingPayload> TYPE = new Type<>(Overcraft.id("ping"));
	public static final StreamCodec<FriendlyByteBuf, PingPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeVarInt(p.owner);
			buf.writeByte(p.kind);
			buf.writeDouble(p.x);
			buf.writeDouble(p.y);
			buf.writeDouble(p.z);
			buf.writeVarInt(p.entity);
			buf.writeUtf(p.what, WHAT_MAX);
		}, buf -> new PingPayload(buf.readVarInt(), buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
			buf.readVarInt(), buf.readUtf(WHAT_MAX)));

	/** Whether a ping of this kind follows its entity. */
	public static boolean follows(int kind) {
		return kind == ENEMY || kind == ITEM || kind == THING;
	}

	@Override
	public Type<PingPayload> type() {
		return TYPE;
	}
}
