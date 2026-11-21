package dev.overcraft.potg;

import dev.overcraft.Overcraft;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads the replay engine's classes in the background as the server starts (#178). The first recording after a server
 * starts (a player's first hero pick) loaded them on the server thread, one class file at a time through Fabric's class
 * loader: thread dumps on a dedicated server showed RecorderSettings, SimpleRecorderSettings.CODEC, FlashbackMeta.CODEC and
 * RejoinedReplayPlayer loading while the server stood still 2.3 s (one run 7.4 s). Loaded here first, the server thread
 * finds them ready. Classes are only loaded, not initialized (no static code runs off the server thread); mixin classes are
 * never touched (loading one directly is an error).
 */
public final class ReplayWarmup {
	private static final String[] PACKAGES = {"dev/overcraft/vendor/arcade", "dev/overcraft/potg"};

	private ReplayWarmup() {
	}

	/** SERVER_STARTED: starts the background load (a daemon thread; nothing waits for it). */
	public static void start() {
		Thread thread = new Thread(ReplayWarmup::run, "Overcraft replay warm-up");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	private static void run() {
		long began = System.nanoTime();
		List<String> names = new ArrayList<>();
		FabricLoader.getInstance().getModContainer(Overcraft.MOD_ID).ifPresent(mod -> {
			for (Path root : mod.getRootPaths()) {
				for (String pkg : PACKAGES) {
					Path dir = root.resolve(pkg);
					if (!Files.isDirectory(dir)) {
						continue;
					}
					try (Stream<Path> files = Files.walk(dir)) {
						files.map(p -> root.relativize(p).toString().replace('\\', '/'))
							.filter(n -> n.endsWith(".class") && !n.contains("/mixins/") && !n.contains("/mixin/"))
							.forEach(n -> names.add(n.substring(0, n.length() - ".class".length()).replace('/', '.')));
					} catch (IOException | RuntimeException e) {
						Overcraft.LOG.debug("Replay warm-up couldn't list {}: {}", dir, e.toString());
					}
				}
			}
		});
		ClassLoader loader = ReplayWarmup.class.getClassLoader();
		int loaded = 0;
		for (String name : names) {
			try {
				Class.forName(name, false, loader);
				loaded++;
			} catch (Throwable e) {
				// a class that can't load here (one that needs the client) is simply left to load as it would have
			}
		}
		Overcraft.LOG.info("Replay engine warmed up: {} of {} classes in {} ms", loaded, names.size(),
			(System.nanoTime() - began) / 1_000_000);
	}
}
