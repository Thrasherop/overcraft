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
 * What each hero has equipped on the communication wheel's pages: four voice lines and four emotes (up, right, down,
 * left, as Overwatch's voice page lays them out). A hero's voice lines are listed by the player's local pack
 * (assets/overcraft/voicelines/<hero>.json, from tools/build_asset_pack.py sounds), its emotes by its model; the choice
 * is saved in config/overcraft-loadout.json. Until chosen, the first four voice lines, and Heroic alone on the emote
 * page.
 */
public final class Loadout {
	public static final int SLOTS = 4;
	/** The emote equipped until the player picks: every hero's Heroic (owner, 2026-10-27). */
	private static final String DEFAULT_EMOTE = "heroic";
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("overcraft-loadout.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, List<String>> VOICE_LINES = new HashMap<>();
	private static final Map<String, List<String>> EMOTES = new HashMap<>();
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

	/** Every emote the hero's model has (its emote_<id>_1 clips: tools/owm_survey.py --emotes), by id. */
	public static List<String> availableEmotes(Hero hero) {
		dev.overcraft.client.model.HeroModels.Loaded loaded = dev.overcraft.client.model.HeroModels.get(hero);
		List<String> ids = new ArrayList<>();
		if (loaded != null) {
			for (String clip : loaded.model().clips.keySet()) {
				if (clip.startsWith("emote_") && clip.endsWith("_1")) {
					ids.add(clip.substring("emote_".length(), clip.length() - 2));
				}
			}
		}
		ids.sort(null);
		return ids;
	}

	/**
	 * Voice lines shown but not to be picked, by hero: greyed out in the list, with a note on hover (the line's
	 * "overcraft.loadout.blocked.<hero>.<id>"). Kiriko's "If this is a simulation, I guess I'm a glitch": the owner's
	 * call (2026-10-27).
	 */
	private static final Map<String, java.util.Set<String>> BLOCKED = Map.of("kiriko", java.util.Set.of("i_m_a_glitch"));

	/** Whether a voice line is shown greyed out and can't be equipped. */
	public static boolean blocked(Hero hero, String id) {
		return BLOCKED.getOrDefault(hero.id, java.util.Set.of()).contains(id);
	}

	/** The four equipped voice lines (up, right, down, left); an empty slot is "" (a blocked line equipped before is too). */
	public static List<String> voiceLines(Hero hero) {
		List<String> pickable = new ArrayList<>(available(hero));
		pickable.removeIf(id -> blocked(hero, id));
		List<String> chosen = new ArrayList<>(chosen(VOICE_LINES, hero, pickable));
		chosen.replaceAll(id -> blocked(hero, id) ? "" : id);
		return chosen;
	}

	/** The four equipped emotes (up, right, down, left); an empty slot is "". */
	public static List<String> emotes(Hero hero) {
		return chosen(EMOTES, hero, availableEmotes(hero).contains(DEFAULT_EMOTE) ? List.of(DEFAULT_EMOTE) : List.of());
	}

	private static List<String> chosen(Map<String, List<String>> saved, Hero hero, List<String> all) {
		load();
		List<String> chosen = saved.get(hero.id);
		if (chosen == null) {
			chosen = new ArrayList<>();
			for (int i = 0; i < SLOTS; i++) {
				chosen.add(i < all.size() ? all.get(i) : "");
			}
		}
		return chosen;
	}

	public static void setVoiceLine(Hero hero, int slot, String id) {
		if (blocked(hero, id)) {
			return;
		}
		List<String> chosen = new ArrayList<>(voiceLines(hero));
		chosen.set(slot, id);
		VOICE_LINES.put(hero.id, chosen);
		save();
	}

	public static void setEmote(Hero hero, int slot, String id) {
		List<String> chosen = new ArrayList<>(emotes(hero));
		chosen.set(slot, id);
		EMOTES.put(hero.id, chosen);
		save();
	}

	/**
	 * An emote's name as shown: its id in words ("fox_cuddles" -> "FOX CUDDLES"), an apostrophe back where the folder
	 * name's became an underscore ("isn_t_this_fun" -> "ISN'T THIS FUN").
	 */
	public static String emoteName(String id) {
		return id.isEmpty() ? "-" : id.replaceAll("(?<=[a-z])_([ts])(?=_|$)", "'$1").replace('_', ' ').toUpperCase(java.util.Locale.ROOT);
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
			read(saved == null ? null : saved.getAsJsonObject("voiceLines"), VOICE_LINES);
			read(saved == null ? null : saved.getAsJsonObject("emotes"), EMOTES);
		} catch (IOException | RuntimeException e) {
			Overcraft.LOG.warn("Couldn't read {}: {}", FILE, e.toString());
		}
	}

	private static void read(JsonObject from, Map<String, List<String>> into) {
		if (from == null) {
			return;
		}
		for (String hero : from.keySet()) {
			List<String> ids = new ArrayList<>();
			for (JsonElement id : from.getAsJsonArray(hero)) {
				ids.add(id.getAsString());
			}
			while (ids.size() < SLOTS) {
				ids.add("");
			}
			into.put(hero, new ArrayList<>(ids.subList(0, SLOTS)));
		}
	}

	private static JsonObject write(Map<String, List<String>> from) {
		JsonObject out = new JsonObject();
		from.forEach((hero, ids) -> {
			JsonArray array = new JsonArray();
			ids.forEach(array::add);
			out.add(hero, array);
		});
		return out;
	}

	private static void save() {
		JsonObject out = new JsonObject();
		out.add("voiceLines", write(VOICE_LINES));
		out.add("emotes", write(EMOTES));
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(out));
		} catch (IOException e) {
			Overcraft.LOG.warn("Couldn't save {}: {}", FILE, e.toString());
		}
	}
}
