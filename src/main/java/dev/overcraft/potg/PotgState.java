package dev.overcraft.potg;

import dev.overcraft.Overcraft;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The world's POTG state, saved with it (overcraft/potg/world.txt): when the last POTG was shown, so the next dragon's
 * POTG chooses among highlights made since (each player's highlights stay: Highlights).
 */
final class PotgState {
	private PotgState() {
	}

	private static Path file(MinecraftServer server) {
		return PotgRecorder.root(server).resolve("world.txt");
	}

	static long lastPotg(MinecraftServer server) {
		try {
			Path f = file(server);
			return Files.exists(f) ? Long.parseLong(Files.readString(f).trim()) : 0;
		} catch (IOException | NumberFormatException e) {
			return 0;
		}
	}

	static void setLastPotg(MinecraftServer server, long ms) {
		try {
			Files.createDirectories(PotgRecorder.root(server));
			Files.writeString(file(server), Long.toString(ms));
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] couldn't save the POTG state: {}", e.toString());
		}
	}
}
