package dev.overcraft.hero;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Where a mob's head is drawn, for the heroes' hit tests (#186, owner, 2026-11-25: "sheep, chickens, cows, etc don't register hits
 * when you hit the actual head. instead you have to hit the front of the torso"). Minecraft's server knows one box per mob, around
 * its body; an animal's head is drawn mostly outside it (a sheep's from 0.38 m to 0.88 m ahead of its middle, its box ending at
 * 0.45 m), so a shot through the head went through nothing. The heads come from Minecraft's own models (head_boxes.json, made by
 * the dev command "headboxes": client HeadBoxDump), placed by the mob's body turn, its scale and whether it's a baby. A shot takes a
 * mob where it meets its box or its head, whichever comes first (Combat.clipEntity; projectiles: ProjectileUtilHeadsMixin).
 * The head's own turn and nod (a sheep grazing) aren't followed: the head stays where it is standing still, looking ahead.
 */
public final class HeadBoxes {
	/** Read on first use (the registries must be up: a unit test of place() never reads it). */
	private static final class Table {
		static final Map<EntityType<?>, float[][]> BOXES = load();
	}

	private HeadBoxes() {
	}

	private static Map<EntityType<?>, float[][]> load() {
		Map<EntityType<?>, float[][]> map = new HashMap<>();
		try (var in = HeadBoxes.class.getResourceAsStream("/overcraft/head_boxes.json")) {
			if (in == null) {
				return map;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (var entry : root.entrySet()) {
				if (!entry.getValue().isJsonObject()) {
					continue;
				}
				var key = net.minecraft.resources.Identifier.tryParse(entry.getKey());
				Optional<EntityType<?>> type = key == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(key);
				if (type.isEmpty()) {
					continue;
				}
				JsonObject o = entry.getValue().getAsJsonObject();
				float[] adult = read(o.get("adult")), baby = read(o.get("baby"));
				// a baby read the same as its adult is a renderer that picks its baby model some other way (a cow's, a chicken's): no
				// head of its own known, so its box alone, as before
				map.put(type.get(), new float[][] {adult, java.util.Arrays.equals(adult, baby) ? null : baby});
			}
		} catch (Exception e) {
			dev.overcraft.Overcraft.LOG.warn("Couldn't read the mobs' head boxes: {}", e.toString());
		}
		return map;
	}

	private static float @Nullable [] read(@Nullable JsonElement e) {
		if (e == null || !e.isJsonArray()) {
			return null;
		}
		JsonArray a = e.getAsJsonArray();
		float[] f = new float[6];
		for (int i = 0; i < 6; i++) {
			f[i] = a.get(i).getAsFloat();
		}
		return f;
	}

	/** {@code entity}'s head where it stands now, or null (no head drawn, or not a mob). */
	public static @Nullable AABB head(Entity entity) {
		if (!(entity instanceof LivingEntity living)) {
			return null;
		}
		float[][] both = Table.BOXES.get(entity.getType());
		if (both == null) {
			return null;
		}
		float[] b = living.isBaby() ? both[1] : both[0];
		if (b == null) {
			return null;
		}
		return place(b, living.getX(), living.getY(), living.getZ(), living.yBodyRot, living.getScale());
	}

	/** A head box (metres, feet at 0, facing +z) for a mob at x y z turned {@code yaw} (Minecraft's: 0 faces +z) at {@code scale}. */
	static AABB place(float[] b, double x, double y, double z, float yaw, float scale) {
		double r = Math.toRadians(yaw), cos = Math.cos(r), sin = Math.sin(r);
		double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
		for (int c = 0; c < 4; c++) {
			double lx = ((c & 1) == 0 ? b[0] : b[3]) * scale, lz = ((c & 2) == 0 ? b[2] : b[5]) * scale;
			// as LivingEntityRenderer turns the model (180 - body yaw, then flipped): forward (0, 1) goes to (-sin, cos)
			double wx = lx * cos - lz * sin, wz = lx * sin + lz * cos;
			minX = Math.min(minX, wx);
			maxX = Math.max(maxX, wx);
			minZ = Math.min(minZ, wz);
			maxZ = Math.max(maxZ, wz);
		}
		return new AABB(x + minX, y + b[1] * scale, z + minZ, x + maxX, y + b[4] * scale, z + maxZ);
	}

	/** Where a shot from {@code from} to {@code to}, {@code radius} thick, meets {@code entity}'s head, if it does. */
	public static Optional<Vec3> clip(Entity entity, double radius, Vec3 from, Vec3 to) {
		AABB head = head(entity);
		return head == null ? Optional.empty() : Combat.clipFrom(head.inflate(radius), from, to);
	}
}
