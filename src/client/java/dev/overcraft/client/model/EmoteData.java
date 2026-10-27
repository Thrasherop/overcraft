package dev.overcraft.client.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * What a hero's emotes show besides the hero, read from the game data: {@code assets/overcraft/emotes/<hero>.json},
 * written by tools/emote_data.py (EMOTES.md, rule 0). For each of the hero's emote clips (emote_<id>_<n>, as the hero's
 * model names them), the props its animation effect spawns: which model, on which hardpoint, from when and for how
 * long in the clip, the prop's own clip and where in her clip it starts, and whether it flies off as a projectile when
 * its time is up (Mic Drop's mic). Each clip's GeoSetFlags too: which of her model's geosets are hidden, sampled evenly
 * over the clip (her weapons put away: Laugh hides her kunai, else in her mouth), and which geoset each of her stowed
 * weapon pieces is. And each hardpoint: the bone it hangs on and its offset in that bone's frame (our models keep
 * Overwatch's own bone frames, so the offset applies as it is). Ids only: the models themselves come from the player's
 * pack (hero_models/prop_<model id>.glb: tools/build_asset_pack.py props).
 */
public final class EmoteData {
	/** A prop shown in one of the hero's clips (times in seconds into that clip). */
	public record Spawn(String model, @Nullable String hardpoint, float start, float duration, @Nullable String clip,
		float clipStart, boolean falls) {
		/** Its file in the pack: hero_models/prop_<model id>.glb, lower case (resource paths must be). */
		public String file() {
			return "prop_" + model.toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** Where a hardpoint is: on a bone, moved and turned in the bone's own frame. */
	public record Hardpoint(String bone, Vector3f t, Quaternionf q) {
	}

	/**
	 * One hero's emotes (emote id -> clip name -> its spawns), each clip's geoset masks (emote id -> clip name -> masks
	 * over the clip), her weapon pieces' geosets (mesh name -> bit) and hardpoints (by id).
	 */
	public record Emotes(Map<String, Map<String, List<Spawn>>> emotes, Map<String, Map<String, int[]>> geosets,
		Map<String, Integer> pieceGeosets, Map<String, Hardpoint> hardpoints) {
		/**
		 * The geosets hidden at this point of the clip (a bit per geoset), or -1 when the data has none for it (her weapons
		 * then stay as her other rules have them).
		 */
		public int hiddenGeosets(String emote, @Nullable String clip, float at, float duration) {
			Map<String, int[]> clips = geosets.get(emote);
			int[] masks = clips == null || clip == null ? null : clips.get(clip);
			if (masks == null || masks.length == 0) {
				return -1;
			}
			int i = duration <= 0 ? 0 : (int) (at / duration * masks.length);
			return masks[Math.clamp(i, 0, masks.length - 1)];
		}

		/** The spawns in this clip of this emote (none when it has none). */
		public List<Spawn> spawns(String emote, @Nullable String clip) {
			Map<String, List<Spawn>> clips = emotes.get(emote);
			List<Spawn> spawns = clips == null || clip == null ? null : clips.get(clip);
			return spawns == null ? List.of() : spawns;
		}

		/** Whether any clip of this emote shows a prop. */
		public boolean hasProps(String emote) {
			Map<String, List<Spawn>> clips = emotes.get(emote);
			return clips != null && clips.values().stream().anyMatch(s -> !s.isEmpty());
		}
	}

	private static volatile Map<String, Emotes> heroes = Map.of();

	private EmoteData() {
	}

	/** The hero's emote data, or null when the mod has none for them. */
	public static @Nullable Emotes of(String hero) {
		return heroes.get(hero);
	}

	/**
	 * Reads every hero's emote file, and returns the prop models they name (pack file names), for HeroModels to load.
	 * Run on resource reload, before the models load.
	 */
	static Set<String> load(ResourceManager resources) {
		Map<String, Emotes> out = new HashMap<>();
		Set<String> models = new TreeSet<>();
		for (Hero hero : Hero.values()) {
			Optional<Resource> res = resources.getResource(Overcraft.id("emotes/" + hero.id + ".json"));
			if (res.isEmpty()) {
				continue;
			}
			try (Reader in = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
				Emotes emotes = parse(JsonParser.parseReader(in).getAsJsonObject());
				out.put(hero.id, emotes);
				emotes.emotes().values().forEach(clips -> clips.values().forEach(s -> s.forEach(spawn -> models.add(spawn.file()))));
			} catch (Exception e) {
				Overcraft.LOG.error("Couldn't read {}'s emote data", hero.id, e);
			}
		}
		heroes = out;
		return models;
	}

	private static Emotes parse(JsonObject json) {
		Map<String, Map<String, List<Spawn>>> emotes = new HashMap<>();
		Map<String, Map<String, int[]>> geosets = new HashMap<>();
		for (Map.Entry<String, JsonElement> emote : json.getAsJsonObject("emotes").entrySet()) {
			Map<String, List<Spawn>> clips = new HashMap<>();
			Map<String, int[]> masks = new HashMap<>();
			for (Map.Entry<String, JsonElement> clip : emote.getValue().getAsJsonObject().getAsJsonObject("clips").entrySet()) {
				if (clip.getValue().getAsJsonObject().has("geosets")) {
					var list = clip.getValue().getAsJsonObject().getAsJsonArray("geosets");
					int[] m = new int[list.size()];
					for (int i = 0; i < m.length; i++) {
						m[i] = list.get(i).getAsInt();
					}
					masks.put(clip.getKey(), m);
				}
				List<Spawn> spawns = new ArrayList<>();
				for (JsonElement e : clip.getValue().getAsJsonObject().getAsJsonArray("spawns")) {
					JsonObject s = e.getAsJsonObject();
					String model = string(s, "model");
					if (model == null) {
						continue;
					}
					spawns.add(new Spawn(model, string(s, "hardpoint"), s.get("start").getAsFloat(), s.get("duration").getAsFloat(),
						string(s, "clip"), s.has("clipStart") && !s.get("clipStart").isJsonNull() ? s.get("clipStart").getAsFloat() : 0,
						s.has("falls") && !s.get("falls").isJsonNull() && s.get("falls").getAsBoolean()));
				}
				clips.put(clip.getKey(), List.copyOf(spawns));
			}
			emotes.put(emote.getKey(), clips);
			geosets.put(emote.getKey(), masks);
		}
		Map<String, Integer> pieceGeosets = new HashMap<>();
		if (json.has("pieceGeosets")) {
			for (Map.Entry<String, JsonElement> piece : json.getAsJsonObject("pieceGeosets").entrySet()) {
				pieceGeosets.put(piece.getKey(), piece.getValue().getAsInt());
			}
		}
		Map<String, Hardpoint> hardpoints = new HashMap<>();
		for (Map.Entry<String, JsonElement> hp : json.getAsJsonObject("hardpoints").entrySet()) {
			JsonObject h = hp.getValue().getAsJsonObject();
			var t = h.getAsJsonArray("t");
			var q = h.getAsJsonArray("q");
			hardpoints.put(hp.getKey(), new Hardpoint(h.get("bone").getAsString(),
				new Vector3f(t.get(0).getAsFloat(), t.get(1).getAsFloat(), t.get(2).getAsFloat()),
				new Quaternionf(q.get(0).getAsFloat(), q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat()).normalize()));
		}
		return new Emotes(emotes, geosets, pieceGeosets, hardpoints);
	}

	private static @Nullable String string(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e == null || e.isJsonNull() ? null : e.getAsString();
	}
}
