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
	 * Before the play (techniques/potg/potg_reference.md, the owner's clip): the team lineup 2.95 s (8.00-10.95), a dip to
	 * black 0.18 s (12.85-13.03), the hero's highlight intro 4.93 s (13.03 to the cut to white at 17.967). The play starts
	 * under the white. A solo highlight has no lineup: it opens on the dip.
	 */
	public static final int LINEUP_MS = 2950;
	public static final int DIP_MS = 180;
	public static final int INTRO_MS = 4930;
	/** After the play (the clip): black 0.6 s, a 0.2 s fade into the end screen; the end screen 6 s and a 0.3 s fade out (ours). */
	public static final int OUTRO_MS = 600 + 200 + 6000 + 300;
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
	/** The world stays held (wall clock) through the POTG's end screen, after the viewers are back. */
	private static long holdUntil;
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
		MobStats.decided(server, dragon);
		if (!killerAllows || choice == null) {
			return;
		}
		startPotg(server, show(choice.highlight(), choice.owner()));
	}

	/**
	 * What the viewers' screens show besides the play (techniques/potg/potg_look.md), as JSON: the POTG's player and hero,
	 * their card (most-played heroes in this world, PlayerStats), and for the POTG the lineup (every viewer and their hero)
	 * and the end screen's cards (players, then mobs with what they did: MobStats; the dragon always).
	 */
	static com.google.gson.JsonObject showData(MinecraftServer server, Show show, List<ServerPlayer> viewers, boolean potg) {
		com.google.gson.JsonObject o = new com.google.gson.JsonObject();
		o.addProperty("player", show.player());
		o.addProperty("hero", show.highlight().hero);
		o.addProperty("heroName", show.hero());
		com.google.gson.JsonArray most = new com.google.gson.JsonArray();
		for (var e : PlayerStats.mostPlayed(server, show.owner(), 3)) {
			com.google.gson.JsonObject m = new com.google.gson.JsonObject();
			m.addProperty("hero", e.getKey());
			m.addProperty("seconds", e.getValue());
			most.add(m);
		}
		o.add("most", most);
		if (!potg) {
			return o;
		}
		com.google.gson.JsonArray lineup = new com.google.gson.JsonArray();
		com.google.gson.JsonArray cards = new com.google.gson.JsonArray();
		for (ServerPlayer p : viewers) {
			com.google.gson.JsonObject l = new com.google.gson.JsonObject();
			l.addProperty("name", p.getName().getString());
			l.addProperty("hero", heroOf(server, p));
			lineup.add(l);
		}
		// the clip's end screen has two cards a side; ours: up to three players, then two mobs (ours)
		List<ServerPlayer> byStat = new ArrayList<>(viewers);
		byStat.sort(java.util.Comparator.comparingDouble((ServerPlayer p) -> statOf(server, p)[0]).reversed());
		for (ServerPlayer p : byStat.subList(0, Math.min(3, byStat.size()))) {
			double[] stat = statOf(server, p);
			com.google.gson.JsonObject c = new com.google.gson.JsonObject();
			c.addProperty("side", "ally");
			c.addProperty("name", p.getName().getString());
			c.addProperty("hero", heroOf(server, p));
			c.addProperty("stat", Math.round(stat[0]));
			// the clip's labels (HEALING DONE, HERO DAMAGE DONE); "DAMAGE DONE" since most of ours is on mobs
			c.addProperty("label", stat[1] == 1 ? "HEALING DONE" : stat[1] == 2 ? "ELIMINATIONS" : "DAMAGE DONE");
			cards.add(c);
		}
		MobStats.Mob dragon = null;
		MobStats.Mob other = null;
		for (MobStats.Mob m : MobStats.last(server)) {
			if (m.type.equals("minecraft:ender_dragon")) {
				dragon = dragon == null || better(m, dragon) ? m : dragon;
			} else if (other == null || better(m, other)) {
				other = m;
			}
		}
		if (dragon == null) {
			dragon = new MobStats.Mob();
			dragon.type = "minecraft:ender_dragon";
		}
		List<MobStats.Mob> enemies = other == null || other.playersKilled == 0 && other.damageToPlayers < 1
			? List.of(dragon) : List.of(dragon, other);
		for (MobStats.Mob m : enemies) {
			com.google.gson.JsonObject c = new com.google.gson.JsonObject();
			c.addProperty("side", "enemy");
			c.addProperty("mob", m.type);
			c.addProperty("name", m.name != null ? m.name : "");
			boolean kills = m.playersKilled > 0;
			c.addProperty("stat", kills ? m.playersKilled : Math.round(m.damageToPlayers));
			c.addProperty("label", kills ? (m.playersKilled == 1 ? "PLAYER KILLED" : "PLAYERS KILLED") : "DAMAGE TO PLAYERS");
			cards.add(c);
		}
		o.add("lineup", lineup);
		o.add("cards", cards);
		return o;
	}

	private static String heroOf(MinecraftServer server, ServerPlayer p) {
		dev.overcraft.hero.Hero hero = HeroManager.hero(p);
		return hero != null ? hero.id : PlayerStats.of(server, p.getUUID()).lastHero;
	}

	private static boolean better(MobStats.Mob a, MobStats.Mob b) {
		return a.playersKilled != b.playersKilled ? a.playersKilled > b.playersKilled : a.damageToPlayers > b.damageToPlayers;
	}

	/** A player's one stat for their card, a notable one as Overwatch shows: healing if it beats damage, else damage, else eliminations (ours). */
	private static double[] statOf(MinecraftServer server, ServerPlayer p) {
		PlayerStats.Player s = PlayerStats.of(server, p.getUUID());
		if (s.healing > s.damage) {
			return new double[] {s.healing, 1};
		}
		if (s.damage > 0 || s.kills == 0) {
			return new double[] {s.damage, 0};
		}
		return new double[] {s.kills, 2};
	}

	/** Whether this world's dragon has been killed (the Highlights menu appears only after: owner, 2026-11-11). */
	public static boolean dragonKilled(MinecraftServer server) {
		ServerLevel end = server.getLevel(net.minecraft.world.level.Level.END);
		var fight = end == null ? null : end.getDragonFight();
		return fight != null && fight.hasPreviouslyKilledDragon();
	}

	/**
	 * The Highlights menu (HighlightsPayload): watch -1 sends the player their own top 5, watch n plays their n-th to them
	 * alone (nobody else is pulled in). Only after the world's dragon has died; not during a showing.
	 */
	public static void highlightsAsked(ServerPlayer player, int watch) {
		MinecraftServer server = player.level().getServer();
		com.google.gson.JsonObject o = new com.google.gson.JsonObject();
		if (!dragonKilled(server)) {
			o.addProperty("locked", true);
			ServerPlayNetworking.send(player, new dev.overcraft.net.HighlightsPayload(-1, o.toString()));
			return;
		}
		List<Highlights.Highlight> list = Highlights.of(server, player.getUUID());
		if (watch >= 0) {
			if (watch < list.size() && !active && player.isAlive()) {
				Overcraft.LOG.info("[POTG] {} watches highlight {}: {}", player.getName().getString(), watch,
					watchHighlight(player, player.getUUID(), list.get(watch)));
			}
			return;
		}
		com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
		for (Highlights.Highlight h : list) {
			com.google.gson.JsonObject j = new com.google.gson.JsonObject();
			j.addProperty("hero", h.hero);
			j.addProperty("category", h.category);
			j.addProperty("score", Math.round(h.score));
			j.addProperty("epochMs", h.epochMs);
			j.addProperty("dimension", h.dimension);
			arr.add(j);
		}
		o.add("highlights", arr);
		ServerPlayNetworking.send(player, new dev.overcraft.net.HighlightsPayload(-1, o.toString()));
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
		int lead = LINEUP_MS + DIP_MS + INTRO_MS;
		begin(server, viewers, true, lead);
		PotgState.setLastPotg(server, System.currentTimeMillis());
		String data = showData(server, show, viewers, true).toString();
		PlayerStats.runOver(server);
		for (ServerPlayer p : viewers) {
			ServerPlayNetworking.send(p, new PotgPayload(PotgPayload.CARD, show.player(), show.hero(), show.category(), lead / 1000f, data));
		}
		Overcraft.LOG.info("[POTG] showing {} to {} viewer(s), world frozen (was {})", show.highlight(), viewers.size(), wasFrozen);
		var zipped = PotgRecorder.hold(server);
		later(server, lead, () -> zipped.whenComplete((v, err) -> server.execute(() -> replay(server, show, viewers))));
		return "Showing " + show.highlight();
	}

	/** One player watches one of their saved highlights alone (owner, 2026-11-11): nobody else moves, the world runs on. */
	public static String watchHighlight(ServerPlayer player, UUID owner, Highlights.Highlight h) {
		MinecraftServer server = player.level().getServer();
		if (active) {
			return "A showing is already playing";
		}
		int lead = DIP_MS + INTRO_MS;
		begin(server, List.of(player), false, lead);
		Show show = show(h, owner);
		// every highlight opens with the hero's intro, as the POTG does (owner, 2026-11-11)
		ServerPlayNetworking.send(player, new PotgPayload(PotgPayload.SOLO, h.player, show.hero(), h.category, lead / 1000f,
			showData(server, show, List.of(player), false).toString()));
		var zipped = PotgRecorder.holdOne(server, owner).thenCompose(v -> PotgRecorder.holdOne(server, player.getUUID()));
		later(server, lead, () -> zipped.whenComplete((v, err) -> server.execute(() -> {
			if (active) {
				replay(server, show, List.of(player));
			}
		})));
		return "Watching " + h;
	}

	private static void begin(MinecraftServer server, List<ServerPlayer> viewers, boolean holdWorld, int leadMs) {
		generation++;
		active = true;
		holdsWorld = holdWorld;
		VIEWERS.clear();
		WATCHING.clear();
		for (ServerPlayer viewer : viewers) {
			VIEWERS.add(viewer.getUUID());
		}
		startedAt = System.currentTimeMillis();
		watchdogAt = startedAt + leadMs + WINDOW_TICKS * 50L + WATCHDOG_EXTRA_MS;
		// a new showing during the last one's end screen: the world is already held, by us
		wasFrozen = holdUntil > 0 ? wasFrozen : server.tickRateManager().isFrozen();
		holdUntil = 0;
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
		cut(server, id);
		ReplayViewer v = viewerOf(id);
		if (v != null) {
			v.stop();
		}
		if (WATCHING.isEmpty()) {
			end(server, "replay done");
		}
	}

	/** The play is over for this viewer: their screen cuts to black now, before they're put back (the clip's hard cut). */
	private static void cut(MinecraftServer server, UUID id) {
		PotgPayload cut = new PotgPayload(PotgPayload.CUT, "", "", "", 0);
		ReplayViewer v = viewerOf(id);
		if (v != null) {
			// a viewer is out of the player list while it watches: through the viewer's own channel (found in play: the CUT
			// sent to the player list never came, and the return's loading screen showed under the banner)
			((dev.overcraft.vendor.arcade.replay.ducks.ReplayViewable) v.getConnection())
				.arcade_sendReplayViewerPacket(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(cut));
			return;
		}
		ServerPlayer p = server.getPlayerList().getPlayer(id);
		if (p != null) {
			ServerPlayNetworking.send(p, cut);
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
		for (UUID id : new ArrayList<>(WATCHING)) {
			cut(server, id);
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
			// held on through the end screen (thawed in tick)
			holdUntil = System.currentTimeMillis() + OUTRO_MS;
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
			!holdsWorld ? "not held" : "held for the end screen");
		VIEWERS.clear();
	}

	/** END_SERVER_TICK, first: true while a showing holds the world, and then the mod's own ticks are skipped too. */
	public static boolean tick(MinecraftServer server) {
		if (!active) {
			if (holdUntil > 0 && System.currentTimeMillis() >= holdUntil) {
				holdUntil = 0;
				server.tickRateManager().setFrozen(wasFrozen);
				Overcraft.LOG.info("[POTG] end screen over: world {}", wasFrozen ? "left frozen" : "thawed");
			}
			return holdUntil > 0;
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
		holdUntil = 0;
		dragonPending = false;
		generation++;
		VIEWERS.clear();
		WATCHING.clear();
	}
}
