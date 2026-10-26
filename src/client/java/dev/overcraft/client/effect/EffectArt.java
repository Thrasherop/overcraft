package dev.overcraft.client.effect;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overcraft.Overcraft;
import dev.overcraft.client.model.HeroTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The effects' textures: Overwatch's grey-scale masks from the local asset pack (textures/effect/, from
 * build_asset_pack.py effects), coloured here the way Overwatch colours them, through a gradient map from the mask's
 * brightness to a colour (tools/reference/kiriko-effects.md, "Colours": the ramps' linear values). Coloured for
 * additive drawing: the colour is the light added, so black adds nothing. Null without the pack: no effect is drawn.
 */
public final class EffectArt {
	/**
	 * Gradient maps: brightness stops and their linear colours, from the game's ramps. Overwatch renders in linear
	 * light; these become sRGB here. Values over 1 in the game glow (bloom); here the top of each map runs to a paler,
	 * whiter colour instead.
	 */
	public enum Palette {
		/** Kiriko's teal: most of her effects, as she and her allies see them. */
		TEAL(0, 0, 0, 0, .25f, 0, .25f, .22f, .5f, 0, .5f, .44f, .82f, 0, 1, .87f, 1, .55f, 1, .97f),
		/** The homing ofuda's gold. */
		GOLD(0, 0, 0, 0, .25f, .36f, .24f, 0, .5f, .71f, .49f, 0, .82f, 1, .82f, .3f, 1, 1, .97f, .75f),
		/** The kunai's and the Kitsune's cyan, white-hot at the top. */
		CYAN(0, 0, 0, 0, .3f, 0, .51f, .45f, .65f, 0, 1, .87f, 1, 1, 1, 1),
		/** Suzu's burst ring: (0.1, 2, 3) in the game. */
		SUZU_BLUE(0, 0, 0, 0, .5f, .017f, .33f, .5f, .85f, .033f, .67f, 1, 1, .5f, .9f, 1),
		/** Suzu's ground mark: (3.2, 2.17, 0.1). */
		SUZU_GOLD(0, 0, 0, 0, .5f, .5f, .34f, .016f, .85f, 1, .68f, .03f, 1, 1, .9f, .5f),
		/** As the enemy sees her effects. */
		ENEMY(0, 0, 0, 0, .25f, .25f, .05f, .05f, .5f, .5f, .1f, .1f, .85f, 1, .21f, .21f, 1, 1, .6f, .6f),
		/** The mask as it is. */
		WHITE(0, 0, 0, 0, 1, 1, 1, 1),
		/** A colour texture used as it is (with its alpha): drawn blended, not added. */
		NATURAL;

		private final float[] stops;

		Palette(float... stops) {
			this.stops = stops;
		}

		/** The colour for a mask brightness, linear. */
		void colour(float v, float[] out) {
			int last = stops.length / 4 - 1;
			for (int i = 0; i < last; i++) {
				float t0 = stops[i * 4];
				float t1 = stops[(i + 1) * 4];
				if (v <= t1 || i == last - 1) {
					float k = t1 > t0 ? Math.clamp((v - t0) / (t1 - t0), 0, 1) : 1;
					for (int c = 0; c < 3; c++) {
						out[c] = stops[i * 4 + 1 + c] + (stops[(i + 1) * 4 + 1 + c] - stops[i * 4 + 1 + c]) * k;
					}
					return;
				}
			}
		}
	}

	private static final Map<String, Optional<Identifier>> CACHE = new HashMap<>();
	/**
	 * Masks whose dim parts the game cuts away (its ring meshes use only the bright edge; the soft interior is for
	 * other uses): brightness below this is black. Without it Suzu's 8 m ground ring was a lit disc.
	 */
	private static final Map<String, Float> BLACK_POINT = Map.of("ring_line", 0.45f);

	private EffectArt() {
	}

	/** The mask called name (textures/effect/name.png) coloured with the palette, or null without the pack. */
	public static @Nullable Identifier get(String name, Palette palette) {
		return get(name, palette, false);
	}

	/**
	 * The same, or (soft) made for blending rather than adding: its brightness becomes alpha and its colour the
	 * palette's brighter half. Added light washes out over bright ground (gold over grass turned lime, the critique);
	 * blended, gold stays gold.
	 */
	public static @Nullable Identifier get(String name, Palette palette, boolean soft) {
		String key = name + "/" + palette.name().toLowerCase(Locale.ROOT) + (soft ? "_soft" : "");
		return CACHE.computeIfAbsent(key, k -> Optional.ofNullable(load(name, palette, soft, k))).orElse(null);
	}

	/**
	 * Masks drawn here rather than read from the pack (original shapes): "card", a rounded card with a bright rim and a
	 * faint face, standing in for the rush path's talisman slabs (the game's mesh 109BC is such a card).
	 */
	private static @Nullable NativeImage generated(String name) {
		if (!name.equals("card")) {
			return null;
		}
		int w = 128;
		int h = 64;
		NativeImage image = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				// distance inside a rounded box, in pixels
				float qx = Math.abs(x + 0.5f - w / 2f) - (w / 2f - 14);
				float qy = Math.abs(y + 0.5f - h / 2f) - (h / 2f - 14);
				float outside = (float) Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) + Math.min(Math.max(qx, qy), 0) - 10;
				float inside = -outside;
				float rim = inside < 0 ? 0 : (float) Math.exp(-Math.pow((inside - 3) / 2.5f, 2));
				float face = Math.clamp(inside / 4, 0, 1) * 0.35f;
				int v = Math.round(255 * Math.clamp(Math.max(rim, face), 0, 1));
				image.setPixel(x, y, 0xFF000000 | v << 16 | v << 8 | v);
			}
		}
		return image;
	}

	private static @Nullable Identifier load(String name, Palette palette, boolean soft, String key) {
		Identifier source = Overcraft.id("textures/effect/" + name + ".png");
		Minecraft mc = Minecraft.getInstance();
		NativeImage made = generated(name);
		if (made == null && mc.getResourceManager().getResource(source).isEmpty()) {
			return null;
		}
		try (InputStream in = made != null ? null : mc.getResourceManager().open(source);
			 NativeImage mask = made != null ? made : NativeImage.read(in)) {
			NativeImage out = new NativeImage(mask.getWidth(), mask.getHeight(), false);
			float[] c = new float[3];
			float black = BLACK_POINT.getOrDefault(name, 0f);
			for (int y = 0; y < mask.getHeight(); y++) {
				for (int x = 0; x < mask.getWidth(); x++) {
					int argb = mask.getPixel(x, y);
					if (palette == Palette.NATURAL) {
						// soft: its silhouette in white, to be tinted
						out.setPixel(x, y, soft ? (argb & 0xFF000000) | 0xFFFFFF : argb);
						continue;
					}
					// the masks are grey (one channel copied to three); their alpha is solid
					float v = Math.clamp(((((argb >> 16) & 0xFF) / 255f) - black) / (1 - black), 0, 1);
					if (soft) {
						palette.colour(0.6f + 0.4f * v, c);
						out.setPixel(x, y, Math.round(255 * v) << 24 | srgb(c[0]) << 16 | srgb(c[1]) << 8 | srgb(c[2]));
					} else {
						palette.colour(v, c);
						out.setPixel(x, y, 0xFF000000 | srgb(c[0]) << 16 | srgb(c[1]) << 8 | srgb(c[2]));
					}
				}
			}
			Identifier id = Overcraft.id("effect/" + key);
			mc.getTextureManager().register(id, new HeroTexture(id, out, MipmapStrategy.MEAN));
			return id;
		} catch (IOException e) {
			Overcraft.LOG.warn("Effect texture {} didn't load: {}", name, e.toString());
			return null;
		}
	}

	private static int srgb(float linear) {
		return Math.round(255 * (float) Math.pow(Math.clamp(linear, 0, 1), 1 / 2.2));
	}

	/** Resource packs changed: colour them again when next asked. */
	public static void clear() {
		CACHE.clear();
	}
}
