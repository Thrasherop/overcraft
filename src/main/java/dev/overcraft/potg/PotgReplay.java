package dev.overcraft.potg;

import dev.overcraft.Overcraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Play of the Game, step 2 (techniques/potg/design.md): the server's half of a replay. {@code /overcraft potg replay}
 * (dev only) hands the hero's ring to their client (single player: one JVM), and holds the world still while it plays
 * (owner, 2026-11-11: the world pauses): Minecraft's tick freeze for everything but players, the viewer's own player
 * not ticked either, the mod's server ticks skipped, and the freeze's two packets kept from the viewer (PotgTap.onSend).
 * The client says when it's done; a watchdog thaws regardless.
 */
public final class PotgReplay {
	private PotgReplay() {
	}

	/** The replay's window: 12 s from the hero's point of view (W: the POTG's replay part). */
	public static final int WINDOW_TICKS = 240;
	/** Thaws after this many server ticks whatever the client does (ours: the window and 3 s). */
	private static final int WATCHDOG_TICKS = WINDOW_TICKS + 60;

	private static final Set<UUID> VIEWERS = new HashSet<>();
	private static boolean active;
	private static boolean wasFrozen;
	private static int ticks;
	private static volatile boolean endAsked;
	/** Set by the client in single player: plays a ring. */
	public static volatile @Nullable Consumer<PotgTap.Dump> clientPlayer;

	public static boolean active() {
		return active;
	}

	public static boolean viewing(ServerPlayer player) {
		return active && VIEWERS.contains(player.getUUID());
	}

	public static String start(ServerPlayer player) {
		if (active) {
			return "A replay is already playing";
		}
		PotgTap.Dump dump = PotgTap.snapshot(player);
		Consumer<PotgTap.Dump> play = clientPlayer;
		if (dump == null || play == null) {
			return "Nothing recorded for " + player.getName().getString() + " (arm with /overcraft potg start, and fight)";
		}
		MinecraftServer server = player.level().getServer();
		wasFrozen = server.tickRateManager().isFrozen();
		VIEWERS.clear();
		VIEWERS.add(player.getUUID());
		active = true;
		ticks = 0;
		endAsked = false;
		// broadcast to everyone but the viewer, whose copy PotgTap.onSend holds back
		server.tickRateManager().setFrozen(true);
		Overcraft.LOG.info("[POTG] replay for {} from tick {}, world frozen (was {})", player.getName().getString(), dump.nowTick(), wasFrozen);
		play.accept(dump);
		return "Replaying the last " + WINDOW_TICKS / 20 + " s";
	}

	/** The client finished (any thread). */
	public static void clientDone() {
		endAsked = true;
	}

	/**
	 * END_SERVER_TICK, first: true while a replay holds the world, and then the mod's own ticks are skipped too (cooldowns,
	 * ult charge, kits), so the hero comes back exactly as they were.
	 */
	public static boolean tick(MinecraftServer server) {
		if (!active) {
			return false;
		}
		ticks++;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (VIEWERS.contains(player.getUUID())) {
				// the viewer's client sends no moves while it watches; don't let the floating check count that
				((dev.overcraft.mixin.ServerGamePacketListenerAccessor) player.connection).overcraft$setAboveGroundTickCount(0);
			}
		}
		if (endAsked || ticks > WATCHDOG_TICKS) {
			end(server, endAsked ? "client done" : "watchdog");
		}
		return true;
	}

	private static void end(MinecraftServer server, String why) {
		active = false;
		server.tickRateManager().setFrozen(wasFrozen);
		// the viewer heard nothing of the freeze; the frozen packet's thaw went to all, and the time is put right
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (VIEWERS.contains(player.getUUID())) {
				// held looks come again, as for someone who has just come to see them (design: "Held effects are sent
				// again with HeroManager.showState at the end")
				var map = ((dev.overcraft.mixin.ChunkMapAccessor) player.level().getChunkSource().chunkMap).overcraft$entityMap();
				for (Object o : map.values()) {
					dev.overcraft.mixin.TrackedEntityAccessor tracked = (dev.overcraft.mixin.TrackedEntityAccessor) o;
					if (tracked.overcraft$entity() == player || tracked.overcraft$seenBy().contains(player.connection)) {
						dev.overcraft.hero.HeroManager.showState(tracked.overcraft$entity(), player);
					}
				}
			}
		}
		VIEWERS.clear();
		Overcraft.LOG.info("[POTG] replay ended ({}) after {} server ticks, world {}", why, ticks, wasFrozen ? "left frozen" : "thawed");
	}

	public static void clear() {
		active = false;
		VIEWERS.clear();
	}
}
