package dev.overcraft.client.comms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What each hero has equipped on the communication wheel's pages: four voice lines (up, right, down, left, as
 * Overwatch's voice page lays them out). A hero's voice lines are listed by the player's local pack
 * (assets/overcraft/voicelines/<hero>.json, from tools/build_asset_pack.py sounds); the choice is saved in
 * config/overcraft-loadout.json. Until chosen, the first four.
 */
public final class Loadout {
	public static final int SLOTS = 4;
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("overcraft-loadout.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, List<String>> VOICE_LINES = new HashMap<>();
	private static boolean loaded;

	private Loadout() {
	}

	/** Every voice line the pack has for the hero, in the pack's order (empty without the pack). */
	public static List<String> available(Hero hero) {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
			.getResource(Overcraft.id("voicelines/" + hero.id + ".json"));
		List<String> ids = new ArrayList<>();
		if (resource.isPresent()) {
			try (Reader reader = resource.get().openAsReader()) {
				for (JsonElement entry : GSON.fromJson(reader, JsonArray.class)) {
					ids.add(entry.getAsJsonObject().get("id").getAsString());
				}
			} catch (IOException | RuntimeException e) {
				Overcraft.LOG.warn("Couldn't read the voice lines for {}: {}", hero.id, e.toString());
			}
		}
		return ids;
	}

	/** The four equipped (up, right, down, left); an empty slot is "". */
	public static List<String> voiceLines(Hero hero) {
		load();
		List<String> chosen = VOICE_LINES.get(hero.id);
		if (chosen == null) {
			List<String> all = available(hero);
			chosen = new ArrayList<>();
			for (int i = 0; i < SLOTS; i++) {
				chosen.add(i < all.size() ? all.get(i) : "");
			}
		}
		return chosen;
	}

	public static void setVoiceLine(Hero hero, int slot, String id) {
		List<String> chosen = new ArrayList<>(voiceLines(hero));
		chosen.set(slot, id);
		VOICE_LINES.put(hero.id, chosen);
		save();
	}

	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		if (!Files.exists(FILE)) {
			return;
		}
		try {
			JsonObject saved = GSON.fromJson(Files.readString(FILE), JsonObject.class);
			JsonObject lines = saved == null ? null : saved.getAsJsonObject("voiceLines");
			if (lines != null) {
				for (String hero : lines.keySet()) {
					List<String> ids = new ArrayList<>();
					for (JsonElement id : lines.getAsJsonArray(hero)) {
						ids.add(id.getAsString());
					}
					while (ids.size() < SLOTS) {
						ids.add("");
					}
					VOICE_LINES.put(hero, ids.subList(0, SLOTS));
				}
			}
		} catch (IOException | RuntimeException e) {
			Overcraft.LOG.warn("Couldn't read {}: {}", FILE, e.toString());
		}
	}

	private static void save() {
		JsonObject lines = new JsonObject();
		VOICE_LINES.forEach((hero, ids) -> {
			JsonArray array = new JsonArray();
			ids.forEach(array::add);
			lines.add(hero, array);
		});
		JsonObject out = new JsonObject();
		out.add("voiceLines", lines);
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(out));
		} catch (IOException e) {
			Overcraft.LOG.warn("Couldn't save {}: {}", FILE, e.toString());
		}
	}
}
