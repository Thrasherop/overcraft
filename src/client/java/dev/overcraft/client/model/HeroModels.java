package dev.overcraft.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overcraft.Overcraft;
import dev.overcraft.client.HeroArt;
import dev.overcraft.hero.Hero;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The heroes' models, from {@code assets/overcraft/hero_models/<hero>.glb}: only in the player's local asset pack
 * (never shipped, see LocalAssetPack). A hero without one keeps the Minecraft player model.
 */
public final class HeroModels {
	/** A loaded model and its textures (by image index; null where the image didn't load). */
	public record Loaded(SkinnedModel model, @Nullable Identifier[] textures) {
	}

	private static final Map<Hero, Loaded> MODELS = new EnumMap<>(Hero.class);
	/** Models that aren't heroes (Kitsune Rush's fox), by file name: {@code hero_models/<name>.glb}. */
	private static final List<String> EXTRAS = List.of("kitsune", "projectile_kunai", "projectile_ofuda", "projectile_suzu", "torii");
	private static final Map<String, Loaded> EXTRA_MODELS = new HashMap<>();
	private static final List<Identifier> REGISTERED = new ArrayList<>();
	/** Bigger images are halved until they fit: a 4096 texture per material is a lot of video memory for a player. */
	private static final int MAX_TEXTURE = 2048;

	private HeroModels() {
	}

	public static @Nullable Loaded get(Hero hero) {
		return MODELS.get(hero);
	}

	/** The hero's first-person view model (hero_models/<hero>_1p.glb: Overwatch's first-person arms). */
	public static @Nullable Loaded firstPerson(Hero hero) {
		return EXTRA_MODELS.get(hero.id + "_1p");
	}

	/** A model that isn't a hero's (see {@link #EXTRAS}). */
	public static @Nullable Loaded get(String name) {
		return EXTRA_MODELS.get(name);
	}

	public static void init() {
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Overcraft.id("hero_models"),
			new SimpleReloadListener<Map<String, Prepared>>() {
				@Override
				protected Map<String, Prepared> prepare(SharedState state) {
					return load(state.resourceManager());
				}

				@Override
				protected void apply(Map<String, Prepared> prepared, SharedState state) {
					register(prepared);
				}
			});
	}

	private record Prepared(SkinnedModel model, List<@Nullable NativeImage> images) {
	}

	/** Every model the pack has, by file name (hero ids and {@link #EXTRAS}). */
	private static Map<String, Prepared> load(ResourceManager resources) {
		Map<String, Prepared> out = new HashMap<>();
		List<String> names = new ArrayList<>(EXTRAS);
		for (Hero hero : Hero.values()) {
			names.add(hero.id);
			names.add(hero.id + "_1p");
		}
		for (String name : names) {
			Optional<Resource> res = resources.getResource(Overcraft.id("hero_models/" + name + ".glb"));
			if (res.isEmpty()) {
				continue;
			}
			try (InputStream in = res.get().open()) {
				SkinnedModel model = SkinnedModel.load(in.readAllBytes());
				List<NativeImage> images = new ArrayList<>();
				for (byte[] png : model.images) {
					images.add(readImage(png));
				}
				out.put(name, new Prepared(model, images));
				Overcraft.LOG.info("Hero model {}: {} nodes, {} joints, {} pieces, {} clips {}", name, model.nodeNames.length,
					model.joints.length, model.pieces.size(), model.clips.size(), model.clips.keySet());
			} catch (Exception e) {
				Overcraft.LOG.error("Couldn't load the {} model", name, e);
			}
		}
		return out;
	}

	private static @Nullable NativeImage readImage(byte[] png) {
		try {
			NativeImage image = NativeImage.read(png);
			while (image.getWidth() > MAX_TEXTURE || image.getHeight() > MAX_TEXTURE) {
				NativeImage half = new NativeImage(image.getWidth() / 2, image.getHeight() / 2, true);
				image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), half);
				image.close();
				image = half;
			}
			return image;
		} catch (Exception e) {
			Overcraft.LOG.warn("A hero texture didn't load: {}", e.toString());
			return null;
		}
	}

	private static void register(Map<String, Prepared> prepared) {
		HeroArt.clear();
		dev.overcraft.client.hud.HudShapes.clear();
		dev.overcraft.client.effect.EffectArt.clear();
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		REGISTERED.forEach(textures::release);
		REGISTERED.clear();
		MODELS.clear();
		EXTRA_MODELS.clear();
		prepared.forEach((name, p) -> {
			Identifier[] ids = new Identifier[p.images().size()];
			for (int i = 0; i < ids.length; i++) {
				NativeImage image = p.images().get(i);
				if (image == null) {
					continue;
				}
				if (name.equals("projectile_ofuda") && i == 0) {
					dev.overcraft.client.effect.EffectArt.keepTalisman(image);
				}
				Identifier id = Overcraft.id("hero_models/" + name + "/" + i);
				textures.register(id, new HeroTexture(id, image));
				REGISTERED.add(id);
				ids[i] = id;
			}
			Loaded loaded = new Loaded(p.model(), ids);
			Hero hero = Hero.byId(name);
			if (hero != null) {
				MODELS.put(hero, loaded);
			} else {
				EXTRA_MODELS.put(name, loaded);
			}
		});
	}
}
