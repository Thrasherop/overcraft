package dev.overcraft.potg;

import dev.overcraft.vendor.arcade.replay.ducks.ReplayViewable;
import dev.overcraft.vendor.arcade.replay.recorder.player.ReplayPlayerRecorders;
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Play of the Game: where every packet to a player meets the vendored replay engine (dev.overcraft.vendor.arcade, MIT;
 * techniques/potg/server_replay.md). The recording tap is on Connection.send (PotgSendMixin), not the listener's send
 * as Arcade has it: Fabric's PacketSender writes to the Connection past the listener, so the hero attachments never
 * reached a recording and replayed heroes were plain Steves (the spike, 2026-11-11).
 */
public final class ReplayTap {
	private ReplayTap() {
	}

	/** A recorder's snapshot capturing what a newcomer would be sent: recorded, not sent (one thread at a time). */
	private static final ThreadLocal<Connection> CAPTURING = new ThreadLocal<>();

	/** Set by the client in single player: whether its game is paused (the viewer holds its stream then). */
	public static volatile BooleanSupplier clientPaused = () -> false;

	public static boolean clientPaused() {
		return clientPaused.getAsBoolean();
	}

	public static void captureOnly(Connection connection, Runnable run) {
		Connection was = CAPTURING.get();
		CAPTURING.set(connection);
		try {
			run.run();
		} finally {
			CAPTURING.set(was);
		}
	}

	/** Connection.send: true if the packet must not go out. Records it for the player's recorder otherwise. */
	public static boolean onSend(Connection connection, Packet<?> packet) {
		if (!(connection.getPacketListener() instanceof ServerGamePacketListenerImpl game)) {
			return false;
		}
		ServerPlayer player = game.player;
		boolean capture = CAPTURING.get() == connection;
		if (!capture) {
			if (PotgReplay.viewing(player) && (packet instanceof ClientboundTickingStatePacket || packet instanceof ClientboundSetTimePacket)) {
				// the world is frozen for the showing, and the viewer's client mustn't hear it (design: "Freezing, with the
				// news kept from the viewer"), or its clock and animators stop
				return true;
			}
			ReplayViewer viewer = ((ReplayViewable) game).arcade_getViewingReplay();
			if (viewer != null) {
				// what the viewer is shown is the replay, never recorded again
				return false;
			}
		}
		if (player.level().getServer().isSameThread()) {
			ReplayPlayerRecorders.record(player.getUUID(), packet);
		} else {
			ReplayPlayerRecorders.recordIfInitialized(player.getUUID(), packet);
		}
		return capture;
	}

	/** Connection.channelRead0: a viewer's input is dropped (Arcade's serverbound filter, without its event bus). */
	public static boolean dropReceived(Connection connection, Packet<?> packet) {
		return connection.getPacketListener() instanceof ServerGamePacketListenerImpl game && ReplayViewer.dropServerbound(game, packet);
	}

	/** Our payloads that are heard or seen once and mean nothing while a replay seeks to its start. */
	private static final Set<String> ONCE = Set.of("overcraft:own_sound", "overcraft:kill_feed");

	/** While a viewer fast-forwards to the play's start: sounds and particles are skipped, they'd all play at once. */
	public static boolean dropWhileSeeking(@Nullable Packet<?> packet) {
		return packet instanceof ClientboundSoundPacket || packet instanceof ClientboundSoundEntityPacket
			|| packet instanceof ClientboundLevelParticlesPacket
			|| packet instanceof ClientboundCustomPayloadPacket custom && ONCE.contains(custom.payload().type().id().toString());
	}
}
