package dev.overcraft.potg;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.vendor.arcade.replay.io.FlashbackIO;
import dev.overcraft.vendor.arcade.replay.io.ReplayFormat;
import dev.overcraft.vendor.arcade.replay.recorder.ReplayRecorder;
import dev.overcraft.vendor.arcade.replay.recorder.player.ReplayPlayerRecorder;
import dev.overcraft.vendor.arcade.replay.recorder.player.ReplayPlayerRecorders;
import dev.overcraft.vendor.arcade.replay.recorder.settings.SimpleRecorderSettings;
import dev.overcraft.vendor.arcade.replay.viewer.ReplayViewers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Play of the Game's recording (techniques/potg/design.md, "The replay engine"): every player who has a hero is recorded
 * by the vendored replay engine (Flashback format, chunks included, so blocks are right), in segments of 5 minutes, one
 * Flashback chunk each (FlashbackIO.CHUNK_LENGTH: one snapshot, so a seek fast-forwards at most a segment). The disk is
 * bounded (owner, 2026-11-11): per player only the segment being recorded and the segments that hold that player's kept
 * highlights (Highlights: at most 5) stay; a finished segment nothing keeps is thrown away unsaved, and a kept one is
 * deleted once its last highlight is replaced. Everything lives in the world's folder, overcraft/potg/&lt;uuid&gt;/.
 */
public final class PotgRecorder {
	private PotgRecorder() {
	}

	/** A segment's length in recorded ticks (ours: one Flashback chunk, its 5 minutes). */
	public static final int SEGMENT_TICKS = FlashbackIO.CHUNK_LENGTH;

	/** A place in a recording: which segment of whose, the recorded tick, the recorded player's entity id. */
	public record Clip(String segment, int tick, int entityId) {
	}

	private static final class Rec {
		final UUID id;
		@Nullable ReplayPlayerRecorder recorder;
		@Nullable String segment;
		int next;

		Rec(UUID id) {
			this.id = id;
		}
	}

	private static final Map<UUID, Rec> RECS = new HashMap<>();
	private static final List<CompletableFuture<Long>> CLOSING = new ArrayList<>();
	private static boolean held;

	public static Path root(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("overcraft").resolve("potg");
	}

	public static Path dir(MinecraftServer server, UUID id) {
		return root(server).resolve(id.toString());
	}

	/** SERVER_STARTED: a raw segment left by a crash is unreadable (never zipped): gone. */
	public static void serverStarted(MinecraftServer server) {
		RECS.clear();
		held = false;
		Path root = root(server);
		if (!Files.isDirectory(root)) {
			return;
		}
		try (Stream<Path> players = Files.list(root)) {
			for (Path p : players.toList()) {
				if (Files.isDirectory(p)) {
					try (Stream<Path> files = Files.list(p)) {
						for (Path f : files.toList()) {
							if (Files.isDirectory(f)) {
								deleteTree(f);
							}
						}
					}
				}
			}
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] couldn't tidy {}", root, e);
		}
	}

	/** END_SERVER_TICK: start, roll and stop each player's recorder; then the engine's own tick. */
	public static void tick(MinecraftServer server) {
		if (!held) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Rec r = RECS.computeIfAbsent(player.getUUID(), Rec::new);
				boolean want = HeroManager.hero(player) != null && !ReplayViewers.has(player.getUUID());
				if (!want) {
					if (r.recorder != null) {
						finish(server, r);
					}
				} else if (r.recorder == null || r.recorder.getStopped()) {
					start(server, r, player);
				} else if (r.recorder.recordedTicks() >= SEGMENT_TICKS) {
					finish(server, r);
					start(server, r, player);
				}
			}
		}
		ReplayPlayerRecorders.tickAll();
		CLOSING.removeIf(CompletableFuture::isDone);
	}

	private static void start(MinecraftServer server, Rec r, ServerPlayer player) {
		Path dir = dir(server, r.id);
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] can't record {}: {}", player.getName().getString(), e.toString());
			return;
		}
		if (r.next == 0) {
			r.next = nextSegment(dir);
		}
		String name = "seg-" + r.next++;
		ReplayPlayerRecorder recorder = ReplayPlayerRecorders.create(player, dir.resolve(name), ReplayFormat.Flashback,
			SimpleRecorderSettings.OVERCRAFT);
		if (!recorder.start(ReplayRecorder.StartingMode.Start)) {
			recorder.stop(false);
			return;
		}
		r.recorder = recorder;
		r.segment = name;
	}

	private static int nextSegment(Path dir) {
		int max = 0;
		try (Stream<Path> files = Files.list(dir)) {
			for (Path f : files.toList()) {
				String n = f.getFileName().toString();
				if (n.startsWith("seg-")) {
					String num = n.substring(4).replace(".zip", "");
					try {
						max = Math.max(max, Integer.parseInt(num));
					} catch (NumberFormatException ignored) {
						// not ours
					}
				}
			}
		} catch (IOException ignored) {
			// none yet
		}
		return max + 1;
	}

	/** Stops a player's recorder: saved (zipped) only if a highlight is in it, then the folder is tidied. */
	private static void finish(MinecraftServer server, Rec r) {
		ReplayPlayerRecorder recorder = r.recorder;
		String segment = r.segment;
		r.recorder = null;
		r.segment = null;
		if (recorder == null || recorder.getStopped()) {
			return;
		}
		boolean keep = segment != null && Highlights.uses(server, r.id, segment + ".zip");
		CompletableFuture<Long> future = recorder.stop(keep);
		CLOSING.add(future);
		future.whenComplete((size, err) -> server.execute(() -> Highlights.tidy(server, r.id)));
	}

	/** Where a player's recording is now: the segment, its tick, their entity id. Null if they aren't being recorded. */
	public static @Nullable Clip clip(ServerPlayer player) {
		Rec r = RECS.get(player.getUUID());
		if (r == null || r.recorder == null || r.recorder.getStopped() || r.segment == null || !r.recorder.isInitialized()) {
			return null;
		}
		return new Clip(r.segment + ".zip", r.recorder.recordedTicks(), player.getId());
	}

	/** The segment a player's recorder writes now (never deleted by a tidy), or null. */
	static @Nullable String current(UUID id) {
		Rec r = RECS.get(id);
		return r == null || r.segment == null ? null : r.segment + ".zip";
	}

	/**
	 * Before a showing: every recorder stops (each segment with a highlight is zipped, so it can be read), and none starts
	 * again until {@link #release}. The future completes when the zips are written.
	 */
	public static CompletableFuture<Void> hold(MinecraftServer server) {
		held = true;
		for (Rec r : RECS.values()) {
			if (r.recorder != null) {
				finish(server, r);
			}
		}
		return CompletableFuture.allOf(CLOSING.toArray(CompletableFuture[]::new));
	}

	/** Stops one player's recorder, for a solo highlight; the future completes when its zip is written. */
	public static CompletableFuture<Void> holdOne(MinecraftServer server, UUID id) {
		Rec r = RECS.get(id);
		if (r != null && r.recorder != null) {
			finish(server, r);
		}
		return CompletableFuture.allOf(CLOSING.toArray(CompletableFuture[]::new));
	}

	public static void release() {
		held = false;
	}

	/** SERVER_STOPPING: every segment closes (a kept one saved), so the highlights survive the world closing. */
	public static void stopAll(MinecraftServer server) {
		for (Rec r : RECS.values()) {
			if (r.recorder != null) {
				finish(server, r);
			}
		}
		for (CompletableFuture<Long> f : new ArrayList<>(CLOSING)) {
			try {
				f.get(30, java.util.concurrent.TimeUnit.SECONDS);
			} catch (Exception e) {
				Overcraft.LOG.warn("[POTG] a segment didn't close: {}", e.toString());
			}
		}
		CLOSING.clear();
		RECS.clear();
	}

	/** Bytes on disk for a player's folder (the raw segment being written and the kept zips), and the whole root. */
	public static long bytes(Path dir) {
		if (!Files.exists(dir)) {
			return 0;
		}
		try (Stream<Path> w = Files.walk(dir)) {
			return w.filter(Files::isRegularFile).mapToLong(f -> f.toFile().length()).sum();
		} catch (IOException | java.io.UncheckedIOException e) {
			return -1;
		}
	}

	/** The recorders' raw sizes, for the disk check (what's being written, uncompressed). */
	public static String report(MinecraftServer server) {
		StringBuilder s = new StringBuilder();
		for (Rec r : RECS.values()) {
			Path dir = dir(server, r.id);
			long zips = 0;
			int nZips = 0;
			try (Stream<Path> files = Files.exists(dir) ? Files.list(dir) : Stream.empty()) {
				for (Path f : files.toList()) {
					if (f.toString().endsWith(".zip")) {
						zips += f.toFile().length();
						nZips++;
					}
				}
			} catch (IOException ignored) {
				// none
			}
			s.append(String.format(java.util.Locale.ROOT, " %s[seg %s raw %d KB, %d zips %d KB, folder %d KB, highlights %d]",
				r.id.toString().substring(0, 8), r.segment, r.recorder == null ? 0 : r.recorder.getRawRecordingSize() / 1024,
				nZips, zips / 1024, bytes(dir) / 1024, Highlights.of(server, r.id).size()));
		}
		return "potg disk: root " + bytes(root(server)) / 1024 + " KB;" + s;
	}

	static void deleteTree(Path p) {
		try (Stream<Path> w = Files.walk(p)) {
			for (Path f : w.sorted(java.util.Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(f);
			}
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] couldn't delete {}: {}", p, e.toString());
		}
	}
}
