package dev.overcraft.comms;

import dev.overcraft.hero.Allies;
import dev.overcraft.net.PingPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pings (#40, tools/reference/comms-reference.md: the owner's Overwatch 2 screenshots): what a player pings goes to
 * them and their teammates (Allies: the scoreboard team, or every player with no teams), as Overwatch's do. The client
 * picks the target and draws them (client.comms.Pings); the server only checks and passes them on.
 */
public final class Pings {
	/** How far a ping reaches (m, ours: Overwatch gives no number; it pings across maps). */
	public static final double REACH = 120;

	private Pings() {
	}

	public static void ping(ServerPlayer player, PingPayload ping) {
		if (ping.kind() < PingPayload.CANCEL || ping.kind() > PingPayload.ENEMY || !player.isAlive()) {
			return;
		}
		if (ping.kind() != PingPayload.CANCEL && player.getEyePosition().distanceToSqr(ping.x(), ping.y(), ping.z()) > REACH * REACH) {
			return;
		}
		PingPayload out = new PingPayload(player.getId(), ping.kind(), ping.x(), ping.y(), ping.z(), ping.entity());
		for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
			if (other.level() == player.level() && (other == player || Allies.isAlly(player, other))) {
				ServerPlayNetworking.send(other, out);
			}
		}
	}
}
