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

	/**
	 * Whether a dragon of this world has taken its killing blow while the mod watched (PotgReplay.dragonDied): the highlights'
	 * unlock, beside Minecraft's own flag. Minecraft's (EnderDragonFight.hasPreviouslyKilledDragon) is only set at the end of the
	 * death, and played 2026-11-19 it stayed false after a kill and its POTG until the world was reloaded (the Highlights item
	 * didn't come back), so this one is kept too (overcraft/potg/dragon_killed).
	 */
	static boolean dragonKilled(MinecraftServer server) {
		return Files.exists(PotgRecorder.root(server).resolve("dragon_killed"));
	}

	static void setDragonKilled(MinecraftServer server) {
		try {
			Files.createDirectories(PotgRecorder.root(server));
			Path f = PotgRecorder.root(server).resolve("dragon_killed");
			if (!Files.exists(f)) {
				Files.writeString(f, Long.toString(System.currentTimeMillis()));
			}
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] couldn't save the dragon's kill: {}", e.toString());
		}
	}

	static void setLastPotg(MinecraftServer server, long ms) {
		try {
			Files.createDirectories(PotgRecorder.root(server));
			// written whole, then moved over the old one (#172: written in place, a crash mid-write lost it)
			Path tmp = file(server).resolveSibling(file(server).getFileName() + ".tmp");
			Files.writeString(tmp, Long.toString(ms));
			Files.move(tmp, file(server), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] couldn't save the POTG state: {}", e.toString());
		}
	}
}
