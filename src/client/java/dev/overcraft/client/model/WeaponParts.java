package dev.overcraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.overcraft.Overcraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The models the data attaches to a weapon or a hand as equipment (assets/overcraft/weapon_parts.json, one row per child entity or
 * clip effect's model: techniques/animation/weapon_parts.md), so a new hero's magazine is a row, not code.
 */
public final class WeaponParts {
	private static final Map<String, List<Part>> PARTS = new HashMap<>();

	private WeaponParts() {
	}

	/** Not-then row: an action's window, for some views. */
	private record Hide(String action, float from, float until, String views) {
	}

	public record Part(String model, boolean onWeapon, String bone, Vector3f t, Quaternionf q, String views, String action, float from, float until,
		List<Hide> hidden, @org.jspecify.annotations.Nullable String unless) {
		/** Whether it shows in this view now (unless: a state that puts it away, "mine_out" Junkrat's mine in his hand). */
		boolean shown(HeroAnimator a, boolean firstPerson, float now) {
			if (!in(views, firstPerson) || "mine_out".equals(unless) && a.mineOut()) {
				return false;
			}
			if (action != null) {
				float s = a.since(action, now);
				if (s < from || s >= until) {
					return false;
				}
			}
			for (Hide h : hidden) {
				float s = a.since(h.action(), now);
				if (in(h.views(), firstPerson) && s >= h.from() && s < h.until()) {
					return false;
				}
			}
			return true;
		}

		private static boolean in(String views, boolean firstPerson) {
			return views.equals("both") || views.equals(firstPerson ? "first" : "third");
		}
	}

	/** Every model the table names (HeroModels warns about those the pack lacks). */
	static java.util.Set<String> names() {
		java.util.Set<String> out = new java.util.HashSet<>();
		PARTS.values().forEach(parts -> parts.forEach(part -> out.add(part.model())));
		return out;
	}

	public static List<Part> of(String hero) {
		return PARTS.getOrDefault(hero, List.of());
	}

	/** Reads the table and returns the models it names, for HeroModels to load. */
	static List<String> load(ResourceManager resources) {
		PARTS.clear();
		List<String> names = new ArrayList<>();
		var res = resources.getResource(Overcraft.id("weapon_parts.json"));
		if (res.isEmpty()) {
			return names;
		}
		try (java.io.Reader reader = res.get().openAsReader()) {
			JsonObject table = JsonParser.parseReader(reader).getAsJsonObject();
			for (String hero : table.keySet()) {
				if (hero.equals("_")) {
					continue;
				}
				List<Part> parts = new ArrayList<>();
				for (JsonElement e : table.getAsJsonArray(hero)) {
					JsonObject o = e.getAsJsonObject();
					JsonArray window = o.getAsJsonArray("window");
					List<Hide> hidden = new ArrayList<>();
					if (o.has("hidden")) {
						for (JsonElement h : o.getAsJsonArray("hidden")) {
							JsonArray r = h.getAsJsonArray();
							hidden.add(new Hide(r.get(0).getAsString(), r.get(1).getAsFloat(), r.get(2).getAsFloat(), r.get(3).getAsString()));
						}
					}
					String model = o.get("model").getAsString();
					parts.add(new Part(model, o.get("on").getAsString().equals("weapon"), o.get("bone").getAsString(), vector(o), turn(o),
						o.get("views").getAsString(), window == null ? null : window.get(0).getAsString(),
						window == null ? 0 : window.get(1).getAsFloat(), window == null ? 0 : window.get(2).getAsFloat(), hidden,
						o.has("unless") ? o.get("unless").getAsString() : null));
					if (!names.contains(model)) {
						names.add(model);
					}
				}
				PARTS.put(hero, parts);
			}
		} catch (Exception e) {
			Overcraft.LOG.error("Couldn't read the weapon parts", e);
		}
		return names;
	}

	private static Vector3f vector(JsonObject o) {
		JsonArray t = o.getAsJsonArray("t");
		return t == null ? new Vector3f() : new Vector3f(t.get(0).getAsFloat(), t.get(1).getAsFloat(), t.get(2).getAsFloat());
	}

	private static Quaternionf turn(JsonObject o) {
		JsonArray q = o.getAsJsonArray("q");
		return q == null ? new Quaternionf() : new Quaternionf(q.get(0).getAsFloat(), q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat());
	}
}
