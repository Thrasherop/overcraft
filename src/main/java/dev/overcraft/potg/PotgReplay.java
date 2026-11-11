package dev.overcraft.potg;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.PotgPayload;
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewer;
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewers;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Play of the Game (techniques/potg/design.md): the server's half of a showing. At the dragon's death (tick 199) the best
 * of everyone's highlights since the last POTG is chosen; every player sees the card, the line and the music, then is
 * taken out of the world by the vendored replay engine (Arcade's server-side viewer, MIT: dev.overcraft.vendor.arcade),
 * brought to the play's dimension and shown it through the scorer's eyes, all starting in the same tick, and put back
 * exactly as they were. The world is held still meanwhile (owner, 2026-11-11: nobody skips): Minecraft's tick freeze,
 * its news kept from the viewers (ReplayTap), the viewers not ticked, the mod's own ticks skipped; in single player the
 * integrated server also pauses itself once its only player is out watching. All timing here is by wall clock (the
 * integrated server pauses, so server ticks stop). A highlight can also be watched alone by its owner
 * ({@link #watchHighlight}): only that player is taken out, and the world isn't held.
 */
public final class PotgReplay {
	private PotgReplay() {
	}

	/** The replay's window: 12 s from the hero's point of view (W: the POTG's replay part). */
	public static final int WINDOW_TICKS = 240;
	/**
	 * The card before it ("PLAY OF THE GAME", name, hero; the announcer's line and cue 5B1F begin under it): ours, 4.8 s, so
	 * card and replay together are 5B1F's 16.84 s (D: music-round9) and the wiki's 17 s (W), the intro's place taken by it.
	 */
	public static final int CARD_TICKS = 96;
	/** Ends regardless this long after the replay should have (ours: a slow seek, a lost client). */
	private static final long WATCHDOG_EXTRA_MS = 20_000;

	/** A showing: the highlight, whose it is, and the card's words. */
	public record Show(Highlights.Highlight highlight, UUID owner, String player, String hero, String category) {
	}

	/** The replay backend: plays a showing for these viewers, each reporting when its replay is over. */
	public interface Backend {
		/** Whether it can replay a play from dimension {@code where} for viewers standing in {@code here}. */
		boolean canReplay(String where, ServerLevel here);

		/** Starts the replay for every viewer in this tick; returns the viewers started. */
		List<ServerPlayer> play(MinecraftServer server, Show show, List<ServerPlayer> viewers);
	}

	/**
	 * The vendored engine (owner, 2026-11-11): each viewer gets their own server-side viewer of the play's segment, which
	 * takes them to its dimension, seeks to the window and locks the camera on the scorer. Step 2's ghost engine is gone:
	 * this plays in single player, on a LAN and on a server, from any dimension.
	 */
	public static final Backend REPLAY = new Backend() {
		@Override
		public boolean canReplay(String where, ServerLevel here) {
			return true;
		}

		@Override
		public List<ServerPlayer> play(MinecraftServer server, Show show, List<ServerPlayer> viewers) {
			Highlights.Highlight h = show.highlight();
			Path zip = PotgRecorder.dir(server, show.owner()).resolve(h.segment);
			List<ServerPlayer> started = new ArrayList<>();
			if (!Files.exists(zip)) {
				Overcraft.LOG.warn("[POTG] the play's segment {} is missing", zip);
				return started;
			}
			List<ReplayViewer> made = new ArrayList<>();
			for (ServerPlayer p : viewers) {
				if (ReplayViewers.has(p.getUUID())) {
					continue;
				}
				try {
					made.add(ReplayViewers.create(zip, p));
					started.add(p);
				} catch (Exception e) {
					Overcraft.LOG.warn("[POTG] couldn't open a viewer for {}: {}", p.getName().getString(), e.toString());
				}
			}
			// every viewer starts in this one tick (the spike: they stayed together)
			long from = h.startTick() * 50L;
			long to = h.endTick * 50L + 250;
			for (ReplayViewer v : made) {
				v.start();
				UUID id = v.getPlayer().getUUID();
				v.playWindow(from, to, h.entityId, () -> viewerDone(server, id));
			}
			return started;
		}
	};

	public static Backend backend = REPLAY;

	private static final Set<UUID> VIEWERS = new HashSet<>();
	private static final Set<UUID> WATCHING = new HashSet<>();
	private static boolean active;
	/** The world is held for this showing (the POTG), not for a solo highlight. */
	private static boolean holdsWorld;
	private static boolean wasFrozen;
	private static long startedAt;
	private static long watchdogAt;
	private static int generation;
	/** The dragon died and its POTG waits for tick 199; whether its killer allows one (not a Minecraft-mode player). */
	private static boolean dragonPending;
	private static boolean killerAllows;
	private static boolean shown;
	private static final java.util.Timer TIMER = new java.util.Timer("overcraft-potg", true);

	public static boolean shownSinceArmed() {
		return shown;
	}

	public static boolean active() {
		return active;
	}

	public static boolean viewing(ServerPlayer player) {
		return active && VIEWERS.contains(player.getUUID());
	}

	/** Runs on the server thread after {@code ms} of wall clock, if this showing is still the one. */
	private static void later(MinecraftServer server, long ms, Runnable run) {
		int gen = generation;
		TIMER.schedule(new java.util.TimerTask() {
			@Override
			public void run() {
				server.execute(() -> {
					if (gen == generation && active) {
						run.run();
					}
				});
			}
		}, ms);
	}

	/**
	 * The dragon has died (AFTER_DEATH, as VICTORY shows): scoring stops, and its killer decides whether there's a POTG at
	 * all: a player out of hero mode takes it away (owner, 2026-11-11: Minecraft-mode kills don't count). Anything else
	 * (a hero, the world, a command) leaves it to the plays.
	 */
	public static void dragonDied(EnderDragon dragon, DamageSource source) {
		Entity killer = source.getEntity();
		killerAllows = killerAllows(killer);
		dragonPending = true;
		if (dragon.level() instanceof ServerLevel level) {
			PlayScore.close(level.getServer());
		}
		Overcraft.LOG.info("[POTG] the dragon died, killer {} ({}); scoring closed", killer == null ? "(none)" : killer.getName().getString(),
			killerAllows ? "allows a POTG" : "out of hero mode: no POTG");
	}

	static boolean killerAllows(@Nullable Entity killer) {
		return !(killer instanceof Player p) || HeroManager.hero(p) != null && HeroManager.heroMode(p);
	}

	/**
	 * The dragon's death has reached tick 199 (PotgDragonMixin), one before it's removed and the exit portal placed
	 * (setDragonKilled at 200): the world freezes here, so the portal opens after the POTG (the note's timeline).
	 */
	public static void dragonAt199(EnderDragon dragon) {
		if (!dragonPending || !(dragon.level() instanceof ServerLevel level)) {
			return;
		}
		dragonPending = false;
		shown = true;
		MinecraftServer server = level.getServer();
		PlayScore.Choice choice = PlayScore.choose(server, PotgState.lastPotg(server));
		Overcraft.LOG.info("[POTG] at the dragon's tick 199: {}; highlights:{}; {}", choice == null ? "no play" : choice.player() + " "
			+ choice.category() + " " + Math.round(choice.score()), choice == null ? "" : choice.table(), PlayScore.costReport());
		// the next plays start from nothing (the run goes on after the dragon: "the whole time"); the highlights stay
		PlayScore.open();
		if (!killerAllows || choice == null) {
			return;
		}
		startPotg(server, show(choice.highlight(), choice.owner()));
	}

	/** A highlight as a showing, the hero by name for the card. */
	public static Show show(Highlights.Highlight h, UUID owner) {
		dev.overcraft.hero.Hero hero = dev.overcraft.hero.Hero.byId(h.hero);
		return new Show(h, owner, h.player, hero == null ? "" : hero.displayName, h.category);
	}

	/** The POTG for everyone online, the world held: the card, then the replay. Also the dev command's. */
	public static String startPotg(MinecraftServer server, Show show) {
		if (active) {
			return "A showing is already playing";
		}
		List<ServerPlayer> viewers = new ArrayList<>(server.getPlayerList().getPlayers());
		if (viewers.isEmpty()) {
			return "Nobody to show it to";
		}
		begin(server, viewers, true);
		PotgState.setLastPotg(server, System.currentTimeMillis());
		float card = CARD_TICKS / 20f;
		for (ServerPlayer p : viewers) {
			ServerPlayNetworking.send(p, new PotgPayload(PotgPayload.CARD, show.player(), show.hero(), show.category(), card));
		}
		Overcraft.LOG.info("[POTG] showing {} to {} viewer(s), world frozen (was {})", show.highlight(), viewers.size(), wasFrozen);
		var zipped = PotgRecorder.hold(server);
		later(server, CARD_TICKS * 50L, () -> zipped.whenComplete((v, err) -> server.execute(() -> replay(server, show, viewers))));
		return "Showing " + show.highlight();
	}

	/** One player watches one of their saved highlights alone (owner, 2026-11-11): nobody else moves, the world runs on. */
	public static String watchHighlight(ServerPlayer player, UUID owner, Highlights.Highlight h) {
		MinecraftServer server = player.level().getServer();
		if (active) {
			return "A showing is already playing";
		}
		begin(server, List.of(player), false);
		// the hero's intro goes here, before each highlight (owner: "every one should open with the hero's intro"): next step
		ServerPlayNetworking.send(player, new PotgPayload(PotgPayload.SOLO, h.player, h.hero, h.category, 0));
		Show show = show(h, owner);
		var zipped = PotgRecorder.holdOne(server, owner).thenCompose(v -> PotgRecorder.holdOne(server, player.getUUID()));
		zipped.whenComplete((v, err) -> server.execute(() -> {
			if (active) {
				replay(server, show, List.of(player));
			}
		}));
		return "Watching " + h;
	}

	private static void begin(MinecraftServer server, List<ServerPlayer> viewers, boolean holdWorld) {
		generation++;
		active = true;
		holdsWorld = holdWorld;
		VIEWERS.clear();
		WATCHING.clear();
		for (ServerPlayer viewer : viewers) {
			VIEWERS.add(viewer.getUUID());
		}
		startedAt = System.currentTimeMillis();
		watchdogAt = startedAt + CARD_TICKS * 50L + WINDOW_TICKS * 50L + WATCHDOG_EXTRA_MS;
		wasFrozen = server.tickRateManager().isFrozen();
		if (holdWorld) {
			// broadcast to everyone but the viewers, whose copies ReplayTap holds back
			server.tickRateManager().setFrozen(true);
		}
		later(server, watchdogAt - startedAt, () -> end(server, "watchdog"));
	}

	private static void replay(MinecraftServer server, Show show, List<ServerPlayer> viewers) {
		if (!active) {
			return;
		}
		List<ServerPlayer> online = new ArrayList<>();
		for (ServerPlayer p : viewers) {
			ServerPlayer now = server.getPlayerList().getPlayer(p.getUUID());
			if (now != null) {
				online.add(now);
			}
		}
		List<ServerPlayer> started = backend.play(server, show, online);
		for (ServerPlayer p : started) {
			WATCHING.add(p.getUUID());
		}
		Overcraft.LOG.info("[POTG] replay started for {} viewer(s) {} ms after the card began", started.size(),
			System.currentTimeMillis() - startedAt);
		if (started.isEmpty()) {
			end(server, "nothing to replay");
		}
	}

	/** A viewer's stream reached the window's end (server thread): they're put back; the last one ends the showing. */
	private static void viewerDone(MinecraftServer server, UUID id) {
		if (!WATCHING.remove(id)) {
			return;
		}
		ReplayViewer v = viewerOf(id);
		if (v != null) {
			v.stop();
		}
		if (WATCHING.isEmpty()) {
			end(server, "replay done");
		}
	}

	private static @Nullable ReplayViewer viewerOf(UUID id) {
		for (ReplayViewer v : ReplayViewers.viewers()) {
			if (v.getPlayer().getUUID().equals(id)) {
				return v;
			}
		}
		return null;
	}

	/** Every viewer is put back, the world thaws, the clients hear the showing is over. */
	private static void end(MinecraftServer server, String why) {
		if (!active) {
			return;
		}
		for (ReplayViewer v : new ArrayList<>(ReplayViewers.viewers())) {
			if (VIEWERS.contains(v.getPlayer().getUUID())) {
				try {
					v.stop();
				} catch (Exception e) {
					Overcraft.LOG.warn("[POTG] a viewer didn't stop cleanly", e);
				}
			}
		}
		active = false;
		generation++;
		WATCHING.clear();
		if (holdsWorld) {
			server.tickRateManager().setFrozen(wasFrozen);
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (VIEWERS.contains(player.getUUID())) {
				ServerPlayNetworking.send(player, new PotgPayload(PotgPayload.END, "", "", "", 0));
				// held looks come again, as for someone who has just come to see them (the replay's hero_sync overwrote the
				// viewer's own client state: the spike)
				var map = ((dev.overcraft.mixin.ChunkMapAccessor) player.level().getChunkSource().chunkMap).overcraft$entityMap();
				for (Object o : map.values()) {
					dev.overcraft.mixin.TrackedEntityAccessor tracked = (dev.overcraft.mixin.TrackedEntityAccessor) o;
					if (tracked.overcraft$entity() == player || tracked.overcraft$seenBy().contains(player.connection)) {
						HeroManager.showState(tracked.overcraft$entity(), player);
					}
				}
			}
		}
		PotgRecorder.release();
		Overcraft.LOG.info("[POTG] showing ended ({}) after {} ms, world {}", why, System.currentTimeMillis() - startedAt,
			!holdsWorld ? "not held" : wasFrozen ? "left frozen" : "thawed");
		VIEWERS.clear();
	}

	/** END_SERVER_TICK, first: true while a showing holds the world, and then the mod's own ticks are skipped too. */
	public static boolean tick(MinecraftServer server) {
		if (!active) {
			return false;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (VIEWERS.contains(player.getUUID())) {
				// the viewer's client sends no moves while it watches; don't let the floating check count that
				((dev.overcraft.mixin.ServerGamePacketListenerAccessor) player.connection).overcraft$setAboveGroundTickCount(0);
			}
		}
		if (System.currentTimeMillis() > watchdogAt) {
			end(server, "watchdog");
		}
		return holdsWorld;
	}

	public static void clear() {
		active = false;
		dragonPending = false;
		generation++;
		VIEWERS.clear();
		WATCHING.clear();
	}
}
