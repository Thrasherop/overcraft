package dev.overcraft.net;

import dev.overcraft.Overcraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to players near a kill: a line for the kill feed. Names as shown; heroes by id ("" for none, a mob); weapon
 * what did it ("kunai", "melee", or "" for anything else); the two sides' teams as this player sees them (true =
 * their own side); killer whether it was this player.
 */
public record KillFeedPayload(String killer, String killerHero, String weapon, String victim, String victimHero,
							  boolean killerAlly, boolean victimAlly, boolean byYou) implements CustomPacketPayload {
	public static final Type<KillFeedPayload> TYPE = new Type<>(Overcraft.id("kill_feed"));
	public static final StreamCodec<FriendlyByteBuf, KillFeedPayload> CODEC = CustomPacketPayload.codec(
		(p, buf) -> {
			buf.writeUtf(p.killer, 64);
			buf.writeUtf(p.killerHero, 32);
			buf.writeUtf(p.weapon, 32);
			buf.writeUtf(p.victim, 64);
			buf.writeUtf(p.victimHero, 32);
			buf.writeBoolean(p.killerAlly);
			buf.writeBoolean(p.victimAlly);
			buf.writeBoolean(p.byYou);
		}, buf -> new KillFeedPayload(buf.readUtf(64), buf.readUtf(32), buf.readUtf(32), buf.readUtf(64), buf.readUtf(32),
			buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

	@Override
	public Type<KillFeedPayload> type() {
		return TYPE;
	}
}
