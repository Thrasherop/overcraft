package dev.overcraft.potg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What each player did, saved with the world (overcraft/potg/players.json), for the Play of the Game's screens
 * (techniques/potg/potg_look.md):
 * <ul>
 * <li>seconds in hero mode per hero, over the world's whole life: the player card's most-played heroes (owner,
 * 2026-11-11: "track hero-mode time per hero per player, saved with the world");</li>
 * <li>this run's totals since the last POTG (damage dealt, healing done, eliminations, in Overwatch HP), counted where
 * PlayScore counts them (heroes in hero mode, not on bystanders): the end screen's one stat per player card. Cleared
 * when a POTG is shown.</li>
 * </ul>
 */
public final class PlayerStats {
	private PlayerStats() {
	}

	public static final class Player {
		public String name = "";
		public Map<String, Long> heroSeconds = new LinkedHashMap<>();
		public double damage;
		public double healing;
		public int kills;
		/** The hero they last played, for the end screen's figure. */
		public String lastHero = "";
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<UUID, Player> PLAYERS = new HashMap<>();
	private static boolean loaded;
	private static boolean dirty;

	private static Path file(MinecraftServer server) {
		return PotgRecorder.root(server).resolve("players.json");
	}

	private static void load(MinecraftServer server) {
		if (loaded) {
			return;
		}
		loaded = true;
		Path f = file(server);
		if (Files.exists(f)) {
			try (Reader r = Files.newBufferedReader(f)) {
				Map<String, Player> map = GSON.fromJson(r, new TypeToken<Map<String, Player>>() { }.getType());
				if (map != null) {
					map.forEach((k, v) -> PLAYERS.put(UUID.fromString(k), v));
				}
			} catch (Exception e) {
				Overcraft.LOG.warn("[POTG] couldn't read {}: {}", f, e.toString());
			}
		}
	}

	public static Player of(MinecraftServer server, UUID id) {
		load(server);
		return PLAYERS.computeIfAbsent(id, k -> new Player());
	}

	private static @Nullable Player of(ServerPlayer player) {
		Player p = of(player.level().getServer(), player.getUUID());
		p.name = player.getName().getString();
		dirty = true;
		return p;
	}

	/** PlayScore.damage, under its conditions (a hero in hero mode, not a bystander): Overwatch HP. */
	static void damage(ServerPlayer player, double dealt) {
		Player p = of(player);
		if (p != null) {
			p.damage += dealt;
		}
	}

	static void healing(ServerPlayer player, double healed) {
		Player p = of(player);
		if (p != null) {
			p.healing += healed;
		}
	}

	static void kill(ServerPlayer player) {
		Player p = of(player);
		if (p != null) {
			p.kills++;
		}
	}

	/** END_SERVER_TICK (not while a showing holds the world): a second of hero mode per hero; saved each minute. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Hero hero = HeroManager.hero(player);
			if (hero != null && HeroManager.heroMode(player) && player.isAlive()) {
				Player p = of(player);
				p.heroSeconds.merge(hero.id, 1L, Long::sum);
				p.lastHero = hero.id;
			}
		}
		if (server.getTickCount() % 1200 == 0) {
			flush(server);
		}
	}

	/** The player's most-played heroes, most first: [hero id, seconds]. */
	public static List<Map.Entry<String, Long>> mostPlayed(MinecraftServer server, UUID id, int n) {
		List<Map.Entry<String, Long>> list = new ArrayList<>(of(server, id).heroSeconds.entrySet());
		list.sort(Map.Entry.<String, Long>comparingByValue().reversed());
		return list.subList(0, Math.min(n, list.size()));
	}

	/** A dragon's decision (POTG or not; not the dev command's): the run's totals start again (hero time stays). */
	public static void runOver(MinecraftServer server) {
		load(server);
		for (Player p : PLAYERS.values()) {
			p.damage = 0;
			p.healing = 0;
			p.kills = 0;
		}
		dirty = true;
		flush(server);
	}

	public static void flush(MinecraftServer server) {
		if (!dirty || !loaded) {
			return;
		}
		dirty = false;
		Map<String, Player> out = new LinkedHashMap<>();
		PLAYERS.forEach((k, v) -> out.put(k.toString(), v));
		try {
			Files.createDirectories(PotgRecorder.root(server));
			// written whole, then moved over the old one (critique 9: a crash mid-write lost the file)
			Path tmp = file(server).resolveSibling("players.json.tmp");
			try (Writer w = Files.newBufferedWriter(tmp)) {
				GSON.toJson(out, w);
			}
			try {
				Files.move(tmp, file(server), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(tmp, file(server), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] couldn't save players.json: {}", e.toString());
		}
	}

	public static void clear() {
		PLAYERS.clear();
		loaded = false;
		dirty = false;
	}
}
