package dev.overcraft.client.dev;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dev only ("headboxes <file>"): every living mob's head as Minecraft draws it, for the server's hit tests (#186: a sheep's, a
 * cow's, a chicken's head is drawn outside its bounding box, so a shot at it missed). For each type, adult and baby: the mob made
 * on this client, its renderer's model posed from a standing render state, and every cube under a part whose name has "head" in it
 * (head, head_parts: a horse's neck and head, a beak, ears, horns), placed as LivingEntityRenderer.submit places the model
 * (26.3: turned to face +z, the entity's scale, flipped, the renderer's own scale, 1.501 down). Written as one box per type in
 * metres, the mob's feet at 0 and facing +z: src/main/resources/overcraft/head_boxes.json.
 */
public final class HeadBoxDump {
	private HeadBoxDump() {
	}

	/** Why types gave no head (the first few of each reason), for the dev command's reply. */
	public static final Map<String, String> WHY = new TreeMap<>();

	private static void why(String reason, EntityType<?> type) {
		WHY.merge(reason, BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath(), (a, b) -> a.length() > 120 ? a : a + " " + b);
	}

	public static int dump(Minecraft mc, Path file) throws Exception {
		WHY.clear();
		Map<String, JsonObject> out = new TreeMap<>();
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			JsonObject entry = new JsonObject();
			for (boolean baby : new boolean[] {false, true}) {
				float[] box = head(mc, type, baby);
				if (box != null) {
					JsonArray a = new JsonArray();
					for (float f : box) {
						a.add(Math.round(f * 1000) / 1000.0);
					}
					entry.add(baby ? "baby" : "adult", a);
				}
			}
			if (entry.size() > 0) {
				out.put(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), entry);
			}
		}
		JsonObject root = new JsonObject();
		root.addProperty("_about", "Each mob's drawn head (model parts named *head*) in metres: feet at 0, facing +z, scale 1. "
			+ "Made by the dev command \"headboxes\" from Minecraft 26.3's own models (HeadBoxDump).");
		out.forEach(root::add);
		Files.createDirectories(file.getParent());
		Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));
		return out.size();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static float @org.jspecify.annotations.Nullable [] head(Minecraft mc, EntityType<?> type, boolean baby) {
		Entity e;
		try {
			e = type.create(mc.level, EntitySpawnReason.LOAD);
		} catch (Exception ex) {
			why("create: " + ex, type);
			return null;
		}
		if (!(e instanceof LivingEntity living)) {
			return null;
		}
		// never added to the level: an id of its own all the same (a render state reads it)
		e.setId(-1_000_000 - BuiltInRegistries.ENTITY_TYPE.getId(type) * 2 - (baby ? 1 : 0));
		if (baby) {
			if (!(e instanceof Mob mob)) {
				return null;
			}
			mob.setBaby(true);
			if (!mob.isBaby()) {
				return null;
			}
		}
		living.setYRot(0);
		living.setYBodyRot(0);
		living.setYHeadRot(0);
		living.setXRot(0);
		EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(e);
		if (!(renderer instanceof LivingEntityRenderer lr)) {
			why("renderer " + (renderer == null ? "none" : renderer.getClass().getSimpleName()), type);
			return null;
		}
		LivingEntityRenderState state;
		try {
			state = (LivingEntityRenderState) lr.createRenderState(living, 1.0F);
		} catch (Exception ex) {
			why("state: " + ex, type);
			return null;
		}
		// an ageable mob's renderer swaps adult and baby models only as it draws (AgeableMobRenderer.submit): take the right one here
		net.minecraft.client.model.EntityModel model = lr instanceof net.minecraft.client.renderer.entity.AgeableMobRenderer
			? (baby ? ((dev.overcraft.client.mixin.AgeableMobRendererAccessor) lr).overcraft$babyModel()
				: ((dev.overcraft.client.mixin.AgeableMobRendererAccessor) lr).overcraft$adultModel())
			: lr.getModel();
		try {
			model.setupAnim(state);
		} catch (Exception ex) {
			why("anim: " + ex, type);
			return null;
		}
		PoseStack pose = new PoseStack();
		pose.rotate(Axis.YP.rotationDegrees(180.0F));
		pose.scale(state.scale, state.scale, state.scale);
		pose.scale(-1.0F, -1.0F, 1.0F);
		((dev.overcraft.client.mixin.LivingEntityRendererInvoker) lr).overcraft$scale(state, pose);
		pose.translate(0.0F, -1.501F, 0.0F);
		float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		boolean[] any = {false};
		model.root().visit(pose, (p, path, index, cube) -> {
			boolean head = false;
			for (String part : path.split("/")) {
				head |= part.contains("head");
			}
			if (!head) {
				return;
			}
			for (int c = 0; c < 8; c++) {
				Vector3f v = new Vector3f((c & 1) == 0 ? cube.minX : cube.maxX, (c & 2) == 0 ? cube.minY : cube.maxY,
					(c & 4) == 0 ? cube.minZ : cube.maxZ).div(16.0F);
				p.pose().transformPosition(v);
				box[0] = Math.min(box[0], v.x);
				box[1] = Math.min(box[1], v.y);
				box[2] = Math.min(box[2], v.z);
				box[3] = Math.max(box[3], v.x);
				box[4] = Math.max(box[4], v.y);
				box[5] = Math.max(box[5], v.z);
				any[0] = true;
			}
		});
		if (!any[0]) {
			why("no head part", type);
		}
		return any[0] ? box : null;
	}
}
