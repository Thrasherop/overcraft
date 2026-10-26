package dev.overcraft.client.controls;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.overcraft.Overcraft;
import dev.overcraft.net.HeroOptionsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Kiriko's hero-specific options as Overwatch has them (the wiki's "Hero-specific options", with Overwatch's
 * defaults), set on the Overwatch options screen's HERO tab and saved in config/overcraft-hero-options.json.
 */
public final class HeroOptions {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("overcraft-hero-options.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** The saved values (field names are the JSON keys). */
	public static final class Values {
		/** Healing Ofuda sensitivity, 10-100%: scales the lock-on cone. */
		public int ofudaSensitivity = 100;
		/** Toggle Healing Ofuda: on, one press starts it and the next stops it; off, it throws while held. */
		public boolean toggleOfuda = true;
		/** Swift Step sensitivity, 10-100%: scales the targeting cone. */
		public int swiftStepSensitivity = 100;
		/** Automatically climb walls: on, forward while airborne climbs; off, jump and forward. */
		public boolean autoClimb = false;
		/** Healing Ofuda cancels reload: on, throwing ofuda interrupts a reload; off, it waits for it. */
		public boolean ofudaCancelsReload = true;
		/** Healing Ofuda UI: the ofuda counter under the crosshair. */
		public boolean ofudaUi = true;
	}

	private static Values values = new Values();

	private HeroOptions() {
	}

	public static Values get() {
		return values;
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try {
				Values read = GSON.fromJson(Files.readString(FILE), Values.class);
				if (read != null) {
					values = read;
				}
			} catch (IOException | RuntimeException e) {
				Overcraft.LOG.warn("Couldn't read {}: {}", FILE, e.toString());
			}
		}
		values.ofudaSensitivity = Math.clamp(values.ofudaSensitivity, 10, 100);
		values.swiftStepSensitivity = Math.clamp(values.swiftStepSensitivity, 10, 100);
	}

	/** Saves, and tells the server the options it acts on. */
	public static void changed() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(values));
		} catch (IOException e) {
			Overcraft.LOG.warn("Couldn't save {}: {}", FILE, e.toString());
		}
		send();
	}

	/** The server's share of the options (on joining a world, and after a change). */
	public static void send() {
		if (ClientPlayNetworking.canSend(HeroOptionsPayload.TYPE)) {
			ClientPlayNetworking.send(new HeroOptionsPayload(values.ofudaSensitivity / 100f, values.swiftStepSensitivity / 100f,
				values.ofudaCancelsReload));
		}
	}

	public static void reset() {
		values = new Values();
		changed();
	}
}
