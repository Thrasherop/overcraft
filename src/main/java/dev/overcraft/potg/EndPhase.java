package dev.overcraft.potg;

import dev.overcraft.net.EndPhasePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The dragon fight's end phase (#45). The owner (2026-10-27): "in overwatch, when a team is about to win, a song plays to
 * hint at it. This should occur with the dragon. Starting quiet at 1/3 health. Then, the victory/potg music." Overwatch's
 * own is its overtime cue (tools/reference/music-round9.md: effect 30C9, STUSound 69B7, H), which plays for both teams.
 * When a dragon's health first falls to a third, everyone online hears it (as VICTORY goes to everyone: Overcraft's
 * AFTER_DEATH handler, #161), its volume rising as the dragon's health falls (the client: EndMusic); the killing blow's
 * VICTORY fades it out. This side only watches the dragon and sends its health share (EndPhasePayload).
 *
 * <p>Ours, as no source says: the music doesn't stop if the dragon heals back above a third (its crystals): it stays at its
 * quietest. A dragon that goes away without dying (everyone left the End, its chunk unloaded) stops it; when it is back
 * and the fight goes on, the music comes back on its loop. A dragon's music never starts twice from its intro; the next
 * dragon (a respawn) starts afresh.
 */
public final class EndPhase {
	private EndPhase() {
	}

	/** The owner: "Starting ... at 1/3 health". */
	public static final float START_SHARE = 1f / 3;
	/** A new share goes out when it has moved more than this (ours, the brief's 2%); the client eases between them. */
	private static final float STEP = 0.02f;
	/** How often the levels are searched for a dragon at a third (ours: 0.5 s, the start can't be later than that). */
	private static final int SCAN_TICKS = 10;

	/** The dragon whose music is on, or null. */
	private static @Nullable UUID current;
	/** The share last sent for it; NaN while it isn't loaded (stopped). */
	private static float sent = Float.NaN;
	/** Dragons that died: their music never comes back. */
	private static final Set<UUID> done = new HashSet<>();
	private static int ticks;

	/** Each server tick (not while a POTG holds the world: PotgReplay.tick). */
	public static void tick(MinecraftServer server) {
		if (current != null) {
			EnderDragon dragon = find(server, current);
			if (dragon == null || !dragon.isAlive()) {
				if (!Float.isNaN(sent)) {
					sent = Float.NaN;
					sendAll(server, new EndPhasePayload(-1, current.hashCode()));
				}
				return;
			}
			float share = share(dragon);
			if (Float.isNaN(sent) || Math.abs(share - sent) > STEP) {
				sent = share;
				sendAll(server, new EndPhasePayload(share, current.hashCode()));
			}
			return;
		}
		if (++ticks % SCAN_TICKS != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (EnderDragon dragon : level.getDragons()) {
				if (!done.contains(dragon.getUUID()) && share(dragon) <= START_SHARE) {
					current = dragon.getUUID();
					sent = share(dragon);
					sendAll(server, new EndPhasePayload(sent, current.hashCode()));
					return;
				}
			}
		}
	}

	/** The dragon died (Overcraft's AFTER_DEATH, as VICTORY is sent): its music ends with VICTORY's fade, never to come back. */
	public static void dragonDied(EnderDragon dragon) {
		done.add(dragon.getUUID());
		if (dragon.getUUID().equals(current)) {
			current = null;
			sent = Float.NaN;
		}
	}

	/** A player who joins mid-phase hears it too. */
	public static void joined(ServerPlayer player) {
		if (current != null && !Float.isNaN(sent)) {
			ServerPlayNetworking.send(player, new EndPhasePayload(sent, current.hashCode()));
		}
	}

	/** The server stopped (single player's world closed): nothing carries into the next. */
	public static void clear() {
		current = null;
		sent = Float.NaN;
		done.clear();
		ticks = 0;
	}

	private static float share(EnderDragon dragon) {
		return dragon.getHealth() / Math.max(1, dragon.getMaxHealth());
	}

	private static @Nullable EnderDragon find(MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity e = level.getEntity(id);
			if (e instanceof EnderDragon dragon) {
				return dragon;
			}
		}
		return null;
	}

	private static void sendAll(MinecraftServer server, EndPhasePayload payload) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
