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
	/**
	 * A loaded model and its textures (by image index; null where the image didn't load), and which images are one flat
	 * colour (Overwatch's shader-driven overlays use them: HeroModelRenderer.isEyeShadow).
	 */
	public record Loaded(SkinnedModel model, @Nullable Identifier[] textures, boolean[] flat) {
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

	/** The white of the eyes, under their iris images (ours: a warm off-white, darkened by the lighting). */
	private static final int EYE_WHITE = 0xE6DED6;

	/** Lays the image over this colour: opaque throughout, its own pixels kept by their alpha. */
	private static void underlay(NativeImage image, int rgb) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getPixel(x, y);
				float a = (argb >>> 24) / 255f;
				int r = Math.round(((argb >> 16) & 0xFF) * a + ((rgb >> 16) & 0xFF) * (1 - a));
				int g = Math.round(((argb >> 8) & 0xFF) * a + ((rgb >> 8) & 0xFF) * (1 - a));
				int b = Math.round((argb & 0xFF) * a + (rgb & 0xFF) * (1 - a));
				image.setPixel(x, y, 0xFF000000 | r << 16 | g << 8 | b);
			}
		}
	}

	/**
	 * Where the held talismans' paper is in Kiriko's weapon atlas (fractions of its size; read off the atlas, and nothing
	 * else of hers samples it): the two fronts (seal and kanji) above, their blank backs below; and a patch of the
	 * fronts' plain white above the seal.
	 */
	private static final float PAPER_U0 = 0.745f, PAPER_U1 = 0.997f;
	private static final float FRONT_V0 = 0.03f, FRONT_V1 = 0.425f, BACK_V0 = 0.47f, BACK_V1 = 0.875f;
	private static final float WHITE_U0 = 0.76f, WHITE_U1 = 0.84f, WHITE_V0 = 0.045f, WHITE_V1 = 0.085f;

	/** The talisman paper: a piece nearly all of whose triangles sample the paper's part of the atlas. */
	private static boolean isPaper(SkinnedModel.Piece piece) {
		int in = 0;
		int tris = piece.indices().length / 3;
		for (int t = 0; t < tris; t++) {
			float u = 0;
			float v = 0;
			for (int k = 0; k < 3; k++) {
				int i = piece.indices()[t * 3 + k];
				u += piece.uv()[i * 2] / 3;
				v += piece.uv()[i * 2 + 1] / 3;
			}
			if (u >= 0.74f && u <= 1 && v >= FRONT_V0 && v <= BACK_V1) {
				in++;
			}
		}
		return tris > 0 && in > tris * 0.9f;
	}

	/**
	 * The talismans' paper white all over (the owner, 2026-10-27: "the back should be the same color as the white base
	 * of the ofuda texture"). Blizzard baked a shade into both sides, the white fading to #756664 toward where the
	 * fingers hold them, and under Minecraft's light the paper read as a grey slab (the seventh critique, #6). Each row's
	 * paper colour (its brightest share, the ink and the seal being the rest) is brought up to the plain white above the
	 * seal, channel by channel: the ink, the seal and the paper's grain stay, the shade goes.
	 */
	private static void whitenPaper(NativeImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int[] white = paperColour(image, Math.round(w * WHITE_U0), Math.round(w * WHITE_U1), Math.round(h * WHITE_V0),
			Math.round(h * WHITE_V1), 0.5f);
		int x0 = Math.round(w * PAPER_U0);
		int x1 = Math.round(w * PAPER_U1);
		for (float[] band : new float[][] {{FRONT_V0, FRONT_V1}, {BACK_V0, BACK_V1}}) {
			for (int y = Math.round(h * band[0]); y < Math.round(h * band[1]); y++) {
				int[] paper = paperColour(image, x0, x1, y, y + 1, 0.85f);
				for (int x = x0; x < x1; x++) {
					int argb = image.getPixel(x, y);
					int out = argb & 0xFF000000;
					for (int c = 0; c < 3; c++) {
						int shift = 16 - c * 8;
						int v = (argb >> shift) & 0xFF;
						out |= Math.min(255, Math.round(v * (float) white[c] / Math.max(1, paper[c]))) << shift;
					}
					image.setPixel(x, y, out);
				}
			}
		}
	}

	/** Each channel's value at this share of the way up its sorted values, over a rectangle of the image. */
	private static int[] paperColour(NativeImage image, int x0, int x1, int y0, int y1, float share) {
		int n = Math.max(1, (x1 - x0) * (y1 - y0));
		int[][] values = new int[3][n];
		int i = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				int argb = image.getPixel(x, y);
				values[0][i] = (argb >> 16) & 0xFF;
				values[1][i] = (argb >> 8) & 0xFF;
				values[2][i] = argb & 0xFF;
				i++;
			}
		}
		int[] out = new int[3];
		for (int c = 0; c < 3; c++) {
			java.util.Arrays.sort(values[c], 0, i);
			out[c] = values[c][Math.min(i - 1, Math.max(0, Math.round((i - 1) * share)))];
		}
		return out;
	}

	/** A small image of one colour throughout. */
	private static boolean isFlat(NativeImage image) {
		if (image.getWidth() * image.getHeight() > 64 * 64) {
			return false;
		}
		int first = image.getPixel(0, 0);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (image.getPixel(x, y) != first) {
					return false;
				}
			}
		}
		return true;
	}

	private static void register(Map<String, Prepared> prepared) {
		HeroArt.clear();
		dev.overcraft.client.assets.AssetPackVersion.clear();
		dev.overcraft.client.hud.HudShapes.clear();
		dev.overcraft.client.effect.EffectArt.clear();
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		REGISTERED.forEach(textures::release);
		REGISTERED.clear();
		MODELS.clear();
		EXTRA_MODELS.clear();
		prepared.forEach((name, p) -> {
			Identifier[] ids = new Identifier[p.images().size()];
			boolean[] flat = new boolean[ids.length];
			// the eyeballs' images: only the iris is opaque in them, Overwatch's eye shader draws the white around it.
			// Drawn as they are, the eyes were see-through round the iris (a pale ring of sky, #19): the white goes in
			// under the iris here
			java.util.Set<Integer> paperDone = new java.util.HashSet<>();
			for (SkinnedModel.Piece piece : p.model().pieces) {
				if (piece.image() >= 0 && piece.image() < ids.length && p.images().get(piece.image()) != null
					&& p.model().mostlyOn(piece, "Eyeball")) {
					underlay(p.images().get(piece.image()), EYE_WHITE);
				}
				if (piece.image() >= 0 && piece.image() < ids.length && p.images().get(piece.image()) != null
					&& isPaper(piece) && paperDone.add(piece.image())) {
					whitenPaper(p.images().get(piece.image()));
				}
			}
			for (int i = 0; i < ids.length; i++) {
				NativeImage image = p.images().get(i);
				if (image == null) {
					continue;
				}
				flat[i] = isFlat(image);
				if (name.equals("projectile_ofuda") && i == 0) {
					dev.overcraft.client.effect.EffectArt.keepTalisman(image);
				}
				Identifier id = Overcraft.id("hero_models/" + name + "/" + i);
				textures.register(id, new HeroTexture(id, image));
				REGISTERED.add(id);
				ids[i] = id;
			}
			Loaded loaded = new Loaded(p.model(), ids, flat);
			Hero hero = Hero.byId(name);
			if (hero != null) {
				MODELS.put(hero, loaded);
			} else {
				EXTRA_MODELS.put(name, loaded);
			}
		});
	}
}
