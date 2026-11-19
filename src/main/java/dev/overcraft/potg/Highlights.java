package dev.overcraft.potg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.overcraft.Overcraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Each player's highlights (owner, 2026-11-11: "the top 5 highlight scores from each player ... everyone [has] their own
 * highlights, vs just one person getting POTG"): the 5 best 12 s windows by score, any category, a new one replacing the
 * lowest, two windows never overlapping (the higher keeps the place). Saved with the world (owner: "Saved with the
 * world"; "rewatchable even if a player leaves"), in overcraft/potg/&lt;uuid&gt;/highlights.json beside the segments they're
 * in. The POTG at the dragon's death is the best of everyone's highlights made since the last POTG. Watched solo by their
 * owner (PotgReplay.watchHighlight); the menu that opens them (after a dragon kill) and the hero's intro before each are the
 * next step, not built.
 */
public final class Highlights {
	private Highlights() {
	}

	/** Kept per player (owner, 2026-11-11). */
	public static final int KEPT = 5;

	/** One highlight: its score, category, hero, when (epoch ms) and where, and its place in a segment. */
	public static final class Highlight {
		public String player = "";
		public double score;
		public String category = "";
		public String hero = "";
		public long epochMs;
		public String dimension = "";
		public String segment = "";
		/** The window's last recorded tick (the play's end) in its segment. */
		public int endTick;
		/** The scorer's entity id in the recording: the camera follows it. */
		public int entityId;

		public int startTick() {
			return Math.max(0, endTick - PotgReplay.WINDOW_TICKS);
		}

		@Override
		public String toString() {
			return String.format(Locale.ROOT, "%s %s %.0f as %s in %s (%s t%d)", player, category, score, hero, dimension, segment, endTick);
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<UUID, List<Highlight>> KEPT_BY = new HashMap<>();
	private static final Set<UUID> LOADED = new HashSet<>();

	public static void clear() {
		KEPT_BY.clear();
		LOADED.clear();
		UNREADABLE.clear();
	}

	/** A player's highlights, best first (loaded from the world on first use). */
	public static List<Highlight> of(MinecraftServer server, UUID id) {
		List<Highlight> list = KEPT_BY.computeIfAbsent(id, k -> new ArrayList<>());
		if (LOADED.add(id)) {
			Path file = PotgRecorder.dir(server, id).resolve("highlights.json");
			if (Files.exists(file)) {
				try (Reader r = Files.newBufferedReader(file)) {
					List<Highlight> read = GSON.fromJson(r, new TypeToken<List<Highlight>>() { }.getType());
					if (read != null) {
						for (Highlight h : read) {
							if (Files.exists(PotgRecorder.dir(server, id).resolve(h.segment))) {
								list.add(h);
							}
						}
					}
				} catch (Exception e) {
					// kept aside, and nothing of this player's is tidied away this session (#172: the empty list made the next
					// tidy delete every kept zip); a new highlight still saves a fresh list
					UNREADABLE.add(id);
					try {
						Files.copy(file, file.resolveSibling("highlights.json.bad"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException ignored) {
						// the warning below says it all
					}
					Overcraft.LOG.warn("[POTG] couldn't read {} (kept as highlights.json.bad; no segment deleted): {}", file, e.toString());
				}
			}
			list.sort(Comparator.comparingDouble((Highlight h) -> h.score).reversed());
		}
		return list;
	}

	/** Every player with highlights in this world (the folder names). */
	public static List<UUID> owners(MinecraftServer server) {
		List<UUID> ids = new ArrayList<>();
		Path root = PotgRecorder.root(server);
		if (Files.isDirectory(root)) {
			try (Stream<Path> s = Files.list(root)) {
				for (Path p : s.toList()) {
					try {
						ids.add(UUID.fromString(p.getFileName().toString()));
					} catch (IllegalArgumentException ignored) {
						// not a player's
					}
				}
			} catch (IOException ignored) {
				// none
			}
		}
		return ids;
	}

	static boolean uses(MinecraftServer server, UUID id, String segment) {
		for (Highlight h : of(server, id)) {
			if (h.segment.equals(segment)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A window just closed with this score (PlayScore, each second): kept if it's among the player's 5 best. A window that
	 * overlaps a kept one replaces it only if it scores higher. Returns the highlight kept, or null.
	 */
	public static @Nullable Highlight offer(ServerPlayer player, PlayScore.Category category, double score, String hero) {
		if (score <= 0) {
			return null;
		}
		MinecraftServer server = player.level().getServer();
		PotgRecorder.Clip clip = PotgRecorder.clip(player);
		if (clip == null) {
			return null;
		}
		List<Highlight> list = of(server, player.getUUID());
		Highlight overlap = null;
		for (Highlight h : list) {
			if (h.segment.equals(clip.segment()) && Math.abs(h.endTick - clip.tick()) < PotgReplay.WINDOW_TICKS) {
				overlap = h;
				break;
			}
		}
		if (overlap != null) {
			if (score <= overlap.score) {
				return null;
			}
			list.remove(overlap);
		} else if (!makesTheCut(server, list, score)) {
			return null;
		}
		Highlight h = new Highlight();
		h.player = player.getName().getString();
		h.score = score;
		h.category = category.title;
		h.hero = hero;
		h.epochMs = System.currentTimeMillis();
		h.dimension = player.level().dimension().identifier().toString();
		h.segment = clip.segment();
		h.endTick = clip.tick();
		h.entityId = clip.entityId();
		list.add(h);
		trim(server, list);
		save(server, player.getUUID());
		tidy(server, player.getUUID());
		return h;
	}

	/** A death just after a kept window: it's taken again to end now, so the death is in it (PlayScore). */
	static void extend(ServerPlayer player, int reachTicks) {
		PotgRecorder.Clip clip = PotgRecorder.clip(player);
		if (clip == null) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		// only the latest window in reach is taken again (#172: each one in reach was, and two of one play ended up the same)
		Highlight latest = null;
		for (Highlight h : of(server, player.getUUID())) {
			if (h.segment.equals(clip.segment()) && clip.tick() - h.endTick <= reachTicks && clip.tick() > h.endTick
				&& (latest == null || h.endTick > latest.endTick)) {
				latest = h;
			}
		}
		if (latest != null) {
			Overcraft.LOG.info("[POTG] {}: {} taken again to show the death ({} ticks after it)", latest.player, latest.category,
				clip.tick() - latest.endTick);
			latest.endTick = clip.tick();
			save(server, player.getUUID());
		}
	}

	private static void save(MinecraftServer server, UUID id) {
		Path dir = PotgRecorder.dir(server, id);
		try {
			Files.createDirectories(dir);
			Path tmp = dir.resolve("highlights.json.tmp");
			try (Writer w = Files.newBufferedWriter(tmp)) {
				GSON.toJson(of(server, id), w);
			}
			Files.move(tmp, dir.resolve("highlights.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] couldn't save highlights: {}", e.toString());
		}
	}

	/** A segment that didn't save (PotgRecorder.finish): its highlights are removed and the list saved. */
	static void dropSegment(MinecraftServer server, UUID id, String segment) {
		if (of(server, id).removeIf(h -> h.segment.equals(segment))) {
			save(server, id);
		}
	}

	/** Players whose highlights.json couldn't be read this session: their segments are never deleted (the list can't say which are used). */
	private static final Set<UUID> UNREADABLE = new HashSet<>();

	/** Deletes every saved segment of this player's no highlight uses (never the one being recorded). */
	static void tidy(MinecraftServer server, UUID id) {
		Path dir = PotgRecorder.dir(server, id);
		of(server, id);
		if (!Files.isDirectory(dir) || UNREADABLE.contains(id)) {
			return;
		}
		Set<String> used = new HashSet<>();
		String current = PotgRecorder.current(id);
		Set<String> dead = new HashSet<>();
		for (Highlight h : of(server, id)) {
			if (!Files.exists(dir.resolve(h.segment))) {
				if (h.segment.equals(current) || PotgRecorder.zipping(id, h.segment)) {
					// a highlight whose segment isn't zipped yet (still recording, or zipping): the zips it replaced stay until
					// it is (#172: deleted first, a crash in between lost both); the finished zip's tidy deletes them
					return;
				}
				// neither recording nor zipping, and no zip: it will never come (a recorder that died some other way); the
				// highlight goes, rather than holding the tidy back all session (the critique of 0.3.120)
				dead.add(h.segment);
				continue;
			}
			used.add(h.segment);
		}
		for (String segment : dead) {
			Overcraft.LOG.warn("[POTG] {}'s segment {} has no zip and isn't coming: its highlights are dropped", id, segment);
			dropSegment(server, id, segment);
		}
		try (Stream<Path> files = Files.list(dir)) {
			for (Path f : files.toList()) {
				String n = f.getFileName().toString();
				if (n.startsWith("seg-") && n.endsWith(".zip") && !used.contains(n) && !n.equals(current)) {
					Files.deleteIfExists(f);
				}
			}
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] couldn't tidy {}: {}", dir, e.toString());
		}
	}

	/**
	 * Whether a new window of this score is kept: in the player's top KEPT, or their best since the last POTG. The owner on
	 * 0.3.66 (#161, the critique's 2): with only the top KEPT ever, a first dragon fight's big windows filled every list, and a
	 * later fight's never got in, so its dragon found "no play since the last POTG" and showed nothing.
	 */
	private static boolean makesTheCut(MinecraftServer server, List<Highlight> list, double score) {
		List<Highlight> sorted = new ArrayList<>(list);
		sorted.sort(Comparator.comparingDouble((Highlight x) -> x.score).reversed());
		if (sorted.size() < KEPT || score > sorted.get(KEPT - 1).score) {
			return true;
		}
		Highlight run = runBest(sorted, PotgState.lastPotg(server));
		return run == null || score > run.score;
	}

	/** The highest-scoring of these made after {@code sinceMs}, or null. */
	private static @Nullable Highlight runBest(List<Highlight> list, long sinceMs) {
		Highlight best = null;
		for (Highlight h : list) {
			if (h.epochMs > sinceMs && (best == null || h.score > best.score)) {
				best = h;
			}
		}
		return best;
	}

	/** Keeps the top KEPT and the best since the last POTG (if not among them), best first; the rest go. */
	private static void trim(MinecraftServer server, List<Highlight> list) {
		list.sort(Comparator.comparingDouble((Highlight x) -> x.score).reversed());
		Highlight run = runBest(list, PotgState.lastPotg(server));
		for (int i = list.size() - 1; i >= KEPT; i--) {
			if (list.get(i) != run) {
				list.remove(i);
			}
		}
	}

	/** The best highlight of anyone's made after {@code sinceMs}: the POTG's choice. */
	public static @Nullable Highlight best(MinecraftServer server, long sinceMs) {
		Highlight best = null;
		UUID whose = null;
		for (UUID id : owners(server)) {
			for (Highlight h : of(server, id)) {
				// a tie goes to the earlier play (#172: it went by the folders' order)
				if (h.epochMs > sinceMs && (best == null || h.score > best.score || h.score == best.score && h.epochMs < best.epochMs)) {
					best = h;
					whose = id;
				}
			}
		}
		if (best != null) {
			bestOwner = whose;
		}
		return best;
	}

	/** Whose the last {@link #best} was. */
	static @Nullable UUID bestOwner;

	public static String table(MinecraftServer server) {
		StringBuilder s = new StringBuilder();
		for (UUID id : owners(server)) {
			s.append(' ').append(of(server, id));
		}
		return s.toString();
	}
}
