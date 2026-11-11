package dev.overcraft.potg;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Play of the Game (techniques/potg/design.md): the server's half of a showing. The dragon's death (step 3) or, in dev
 * builds, {@code /overcraft potg replay} (step 2) hands a ring to the viewers' clients (single player: one JVM), and holds
 * the world still while it plays (owner, 2026-11-11: the world pauses; nobody can skip it, and there's no off switch):
 * Minecraft's tick freeze for everything but players, the viewers' own players not ticked either, the mod's server ticks
 * skipped, and the freeze's two packets kept from the viewers (PotgTap.onSend). The client says when it's done; a watchdog
 * thaws regardless.
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
	/** Thaws after this many server ticks past the showing's length whatever the client does (ours: 3 s). */
	private static final int WATCHDOG_EXTRA = 60;

	/** What a client is handed: the ring, and the card's words (player null for the dev replay, which has no card). */
	public record Show(PotgTap.Dump dump, @Nullable String player, String hero, String category) {
	}

	private static final Set<UUID> VIEWERS = new HashSet<>();
	private static boolean active;
	private static boolean wasFrozen;
	private static int ticks;
	private static int watchdog;
	private static volatile boolean endAsked;
	/** The dragon died and its POTG waits for tick 199; whether its killer allows one (not a Minecraft-mode player). */
	private static boolean dragonPending;
	private static boolean killerAllows;
	/** Set by the client in single player: plays a showing. */
	public static volatile @Nullable Consumer<Show> clientPlayer;

	private static boolean shown;

	/** A dragon's POTG has been shown since the tap armed (PotgTap's disarm). */
	public static boolean shownSinceArmed() {
		return shown;
	}

	public static void armed() {
		shown = false;
	}

	public static boolean active() {
		return active;
	}

	public static boolean viewing(ServerPlayer player) {
		return active && VIEWERS.contains(player.getUUID());
	}

	/** The dev replay: the issuing hero's own last 12 s, no card. */
	public static String start(ServerPlayer player) {
		PotgTap.Dump dump = PotgTap.snapshot(player);
		if (dump == null) {
			return "Nothing recorded for " + player.getName().getString() + " (arm with /overcraft potg start, and fight)";
		}
		return show(player.level().getServer(), List.of(player), new Show(dump, null, "", ""), WINDOW_TICKS);
	}

	private static String show(MinecraftServer server, List<ServerPlayer> viewers, Show show, int length) {
		Consumer<Show> play = clientPlayer;
		if (active) {
			return "A replay is already playing";
		}
		if (play == null) {
			return "No client to play it (single player only for now)";
		}
		wasFrozen = server.tickRateManager().isFrozen();
		VIEWERS.clear();
		for (ServerPlayer viewer : viewers) {
			VIEWERS.add(viewer.getUUID());
		}
		active = true;
		ticks = 0;
		watchdog = length + WATCHDOG_EXTRA;
		endAsked = false;
		// broadcast to everyone but the viewers, whose copies PotgTap.onSend holds back
		server.tickRateManager().setFrozen(true);
		Overcraft.LOG.info("[POTG] showing {} to {} viewer(s), ring to tick {}, world frozen (was {})",
			show.player() == null ? "a replay" : show.player() + " (" + show.category() + ")", viewers.size(), show.dump().nowTick(), wasFrozen);
		play.accept(show);
		return "Replaying the last " + WINDOW_TICKS / 20 + " s";
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
		shown = true; // decided, a POTG or none: the tap may disarm once the dragon's gone
		PlayScore.Choice choice = PlayScore.choose();
		Overcraft.LOG.info("[POTG] at the dragon's tick 199: {}; plays:{}; {}", choice == null ? "no play" : choice.player() + " "
			+ choice.category().title + " " + Math.round(choice.score()), choice == null ? "" : choice.table(), PlayScore.costReport());
		if (!killerAllows || choice == null) {
			return;
		}
		// everyone in the dragon's level, the dead too (they aren't in level.players(); design: "The viewer is dead when it
		// starts": it plays over the death screen)
		List<ServerPlayer> viewers = level.getServer().getPlayerList().getPlayers().stream().filter(p -> p.level() == level).toList();
		if (viewers.isEmpty()) {
			return;
		}
		dev.overcraft.hero.Hero hero = dev.overcraft.hero.Hero.byId(choice.hero());
		shown = true;
		show(level.getServer(), viewers, new Show(choice.dump(), choice.player(), hero == null ? "" : hero.displayName,
			choice.category().title), CARD_TICKS + WINDOW_TICKS);
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
		if (endAsked || ticks > watchdog) {
			end(server, endAsked ? "client done" : "watchdog");
		}
		return true;
	}

	private static void end(MinecraftServer server, String why) {
		active = false;
		server.tickRateManager().setFrozen(wasFrozen);
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
		dragonPending = false;
		VIEWERS.clear();
	}
}
