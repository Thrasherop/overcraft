package dev.overcraft.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Faces for the mobs a hero meets most, cut from Minecraft's own entity textures (the front of each model's head): the
 * kill feed's portraits where there's no hero (the fourth critique: a mob had none, where Overwatch always shows one)
 * and the pings' markers (#40). Others get none.
 */
public final class MobFaces {
	/** Where a mob's face is in its texture. */
	public record MobFace(Identifier texture, int u, int v, int w, int h, int texW, int texH) {
	}

	private static MobFace face(String path, int u, int v, int w, int h, int texW, int texH) {
		return new MobFace(Identifier.withDefaultNamespace("textures/entity/" + path + ".png"), u, v, w, h, texW, texH);
	}

	private static final Map<String, MobFace> FACES = Map.ofEntries(
		Map.entry("minecraft:zombie", face("zombie/zombie", 8, 8, 8, 8, 64, 64)),
		Map.entry("minecraft:husk", face("zombie/husk", 8, 8, 8, 8, 64, 64)),
		Map.entry("minecraft:drowned", face("zombie/drowned", 8, 8, 8, 8, 64, 64)),
		Map.entry("minecraft:zombie_villager", face("zombie_villager/zombie_villager", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:skeleton", face("skeleton/skeleton", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:stray", face("skeleton/stray", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:wither_skeleton", face("skeleton/wither_skeleton", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:bogged", face("skeleton/bogged", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:creeper", face("creeper/creeper", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:spider", face("spider/spider", 40, 12, 8, 8, 64, 32)),
		Map.entry("minecraft:cave_spider", face("spider/cave_spider", 40, 12, 8, 8, 64, 32)),
		Map.entry("minecraft:enderman", face("enderman/enderman", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:villager", face("villager/villager", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:wandering_trader", face("wandering_trader/wandering_trader", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:witch", face("witch/witch", 8, 8, 8, 10, 64, 128)),
		Map.entry("minecraft:pillager", face("illager/pillager", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:vindicator", face("illager/vindicator", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:evoker", face("illager/evoker", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:piglin", face("piglin/piglin", 8, 8, 10, 8, 64, 64)),
		Map.entry("minecraft:piglin_brute", face("piglin/piglin_brute", 8, 8, 10, 8, 64, 64)),
		Map.entry("minecraft:zombified_piglin", face("piglin/zombified_piglin", 8, 8, 10, 8, 64, 64)),
		Map.entry("minecraft:blaze", face("blaze/blaze", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:iron_golem", face("iron_golem/iron_golem", 8, 8, 8, 10, 128, 128)),
		Map.entry("minecraft:pig", face("pig/pig_temperate", 8, 8, 8, 8, 64, 64)),
		Map.entry("minecraft:cow", face("cow/cow_temperate", 6, 6, 8, 8, 64, 64)),
		Map.entry("minecraft:sheep", face("sheep/sheep", 8, 8, 6, 6, 64, 32)),
		Map.entry("minecraft:wolf", face("wolf/wolf", 4, 4, 6, 6, 64, 32)));

	private MobFaces() {
	}

	/** The face for this entity type id ("minecraft:zombie"), if there is one. */
	public static @Nullable MobFace get(String type) {
		return FACES.get(type);
	}

	/** The face drawn as large as fits in the box (pixel art kept sharp), centred: some faces are taller than wide. */
	public static void draw(GuiGraphicsExtractor g, MobFace face, float x, float y, float w, float h, int tint) {
		float scale = Math.min(w, h) / Math.max(face.w(), face.h());
		float fw = face.w() * scale;
		float fh = face.h() * scale;
		g.pose().pushMatrix();
		g.pose().translate(x + (w - fw) / 2, y + (h - fh) / 2);
		g.pose().scale(scale, scale);
		g.blit(RenderPipelines.GUI_TEXTURED, face.texture(), 0, 0, face.u(), face.v(), face.w(), face.h(), face.w(), face.h(),
			face.texW(), face.texH(), tint);
		g.pose().popMatrix();
	}
}
