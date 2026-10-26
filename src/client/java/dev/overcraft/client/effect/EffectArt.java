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
		/**
		 * Kiriko's teal: most of her effects, as she and her allies see them. The game's ramp from 0.82 up; its faint end
		 * (0.25, 0.5: (0, .25, .22), (0, .5, .44)) moved toward blue: added over Minecraft's green grass it summed to
		 * mint (the fifth critique measured hues of 135-162 degrees against Kiriko's 176). Ours.
		 */
		TEAL(0, 0, 0, 0, .25f, 0, .15f, .28f, .5f, 0, .38f, .58f, .82f, 0, 1, .87f, 1, .55f, 1, .97f),
		/** The homing ofuda's gold. */
		GOLD(0, 0, 0, 0, .25f, .36f, .24f, 0, .5f, .71f, .49f, 0, .82f, 1, .82f, .3f, 1, 1, .97f, .75f),
		/**
		 * The kunai's and the Kitsune's cyan, white-hot at the top. Its faint end moved toward blue, as TEAL's (game:
		 * (0, .51, .45) at 0.3, (0, 1, .87) at 0.65).
		 */
		CYAN(0, 0, 0, 0, .3f, 0, .42f, .55f, .65f, 0, .95f, 1, 1, 1, 1, 1),
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
	/** The talisman mask cut from the ofuda model's texture (see keepTalisman), or null without the pack. */
	private static @Nullable NativeImage talisman;
	/** Where the talisman's face sits in the ofuda model's texture atlas (fractions of its size; read off the atlas). */
	private static final float TALISMAN_X0 = 0.737f, TALISMAN_X1 = 0.862f, TALISMAN_Y0 = 0.05f, TALISMAN_Y1 = 0.452f;
	/**
	 * Masks whose dim parts the game cuts away (its ring meshes use only the bright edge; the soft interior is for
	 * other uses): brightness below this is black. Without it Suzu's 8 m ground ring was a lit disc.
	 */
	private static final Map<String, Float> BLACK_POINT = Map.of("ring_line", 0.45f,
		// the kunai's lens glint: its faint halo, gamma-encoded and added, read as a teal sphere round each hit (the
		// fourth critique); the streak itself is brighter than this
		"glint", 0.12f);
	/**
	 * Blended ("soft") colours given directly in sRGB, brightness stop then red, green, blue (0-1), for palettes whose
	 * game ramp runs too pale to blend: soft gold took the ramp's top, near-white cream, and read as cream-khaki comets
	 * over grass (the fourth critique: Overwatch's on-screen gold is about #DCB900 to #A18700). Ours, from those.
	 */
	private static final Map<Palette, float[]> SOFT_RAMPS = Map.of(
		// more orange than the on-screen gold itself: blended at part alpha over green grass it shifts toward olive (the
		// sixth critique measured hues of 54-62 degrees against Overwatch's 45-50), and bright at the faint end too (a
		// dark one darkened leaves to khaki, the seventh)
		Palette.GOLD, new float[] {0, .9f, .5f, 0, .5f, .93f, .6f, 0, .9f, .95f, .73f, .08f, 1, .98f, .82f, .3f},
		// Kiriko's teal and the kunai's cyan, blended (the faint parts of her effects: added over green grass they
		// summed to mint, the fifth and sixth critiques), leaning blue so that over grass they land near 176 degrees
		// (its faint end bluer and brighter again: at part alpha the path tiles still read 158-164, the seventh)
		Palette.TEAL, new float[] {0, 0, .4f, .72f, .5f, 0, .62f, .86f, .9f, .12f, .86f, .92f, 1, .62f, .98f, .98f},
		Palette.CYAN, new float[] {0, 0, .5f, .75f, .5f, 0, .7f, .9f, .9f, .3f, .92f, 1, 1, .8f, 1, 1},
		// an enemy's effects, always blended (Fx): added, red over sky or grass washed out to pink-white. Ours
		// its dark end read brick brown over grass (the sixth critique): brighter throughout
		Palette.ENEMY, new float[] {0, .7f, .1f, .12f, .5f, .86f, .18f, .17f, .9f, .95f, .32f, .28f, 1, 1, .6f, .55f},
		// Suzu's 5 s ground ring: the game's (3.2, 2.17, 0.1) is a saturated gold, about #FFD732 on screen; from the
		// ramp's pale top it read khaki over grass (the fifth critique)
		// (its faint end bright orange: dark, it read olive over grass, the seventh)
		Palette.SUZU_GOLD, new float[] {0, .92f, .6f, 0, .5f, .96f, .72f, .04f, .9f, 1, .84f, .2f, 1, 1, .9f, .45f});

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
		if (name.equals("talisman")) {
			if (talisman == null) {
				return null;
			}
			NativeImage copy = new NativeImage(talisman.getWidth(), talisman.getHeight(), false);
			copy.copyFrom(talisman);
			return copy;
		}
		if (name.equals("sparkle_soft")) {
			return softCross(128);
		}
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

	/**
	 * A glowing "+" (the rush's sparkles, 3DC7): two arms with a gaussian falloff across and along, and a bright core. The
	 * pack's own sparkle mask is a flat cross with an outline (the game blooms it in HDR); drawn as it is, it read as a
	 * Minecraft health icon (the fourth critique). Ours.
	 */
	private static NativeImage softCross(int size) {
		NativeImage image = new NativeImage(size, size, false);
		float c = size / 2f;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				float dx = (x + 0.5f - c) / c;
				float dy = (y + 0.5f - c) / c;
				// each arm: thin across (sigma 0.06), long along (sigma 0.45)
				float arms = (float) Math.max(Math.exp(-dx * dx / 0.0072f - dy * dy / 0.405f), Math.exp(-dy * dy / 0.0072f - dx * dx / 0.405f));
				float core = (float) Math.exp(-(dx * dx + dy * dy) / 0.02f);
				int v = Math.round(255 * Math.clamp(Math.max(arms, core), 0, 1));
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
					float[] ramp = soft ? SOFT_RAMPS.get(palette) : null;
					if (ramp != null) {
						ramp(ramp, v, c);
						out.setPixel(x, y, Math.round(255 * v) << 24 | byte255(c[0]) << 16 | byte255(c[1]) << 8 | byte255(c[2]));
					} else if (soft) {
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

	/** A colour from stops of (brightness, r, g, b), as they are. */
	private static void ramp(float[] stops, float v, float[] out) {
		int last = stops.length / 4 - 1;
		for (int i = 0; i < last; i++) {
			float t0 = stops[i * 4];
			float t1 = stops[(i + 1) * 4];
			if (v <= t1 || i == last - 1) {
				float k = Math.clamp((v - t0) / (t1 - t0), 0, 1);
				for (int c = 0; c < 3; c++) {
					out[c] = stops[i * 4 + 1 + c] + (stops[(i + 1) * 4 + 1 + c] - stops[i * 4 + 1 + c]) * k;
				}
				return;
			}
		}
	}

	private static int byte255(float v) {
		return Math.round(255 * Math.clamp(v, 0, 1));
	}

	private static int srgb(float linear) {
		return Math.round(255 * (float) Math.pow(Math.clamp(linear, 0, 1), 1 / 2.2));
	}

	/**
	 * Keeps a mask of the ofuda's paper talisman (from its model's texture, as it loads: the local pack's) for effects:
	 * its ink and seal bright, the paper faint, so coloured and added it glows as glyphs on a card (the rush path's
	 * talisman tiles).
	 */
	public static void keepTalisman(NativeImage atlas) {
		int x0 = Math.round(atlas.getWidth() * TALISMAN_X0);
		int x1 = Math.round(atlas.getWidth() * TALISMAN_X1);
		int y0 = Math.round(atlas.getHeight() * TALISMAN_Y0);
		int y1 = Math.round(atlas.getHeight() * TALISMAN_Y1);
		int w = 96;
		int h = 288;
		NativeImage mask = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int argb = atlas.getPixel(x0 + (x1 - x0) * x / w, y0 + (y1 - y0) * y / h);
				float r = ((argb >> 16) & 0xFF) / 255f;
				float g = ((argb >> 8) & 0xFF) / 255f;
				float b = (argb & 0xFF) / 255f;
				float lum = 0.3f * r + 0.59f * g + 0.11f * b;
				float ink = Math.clamp((0.85f - lum) / 0.65f, 0, 1);
				// a soft rim so the card's edge shows, fading to nothing at the very border
				float edge = Math.min(Math.min(x, w - 1 - x) / 6f, Math.min(y, h - 1 - y) / 6f);
				float rim = edge < 1 ? edge : Math.max(0, 1.6f - edge * 0.6f);
				float v = Math.clamp(Math.max(0.2f + 0.8f * ink, rim * 0.7f), 0, 1) * Math.clamp(edge, 0, 1);
				int c = Math.round(255 * v);
				mask.setPixel(x, y, 0xFF000000 | c << 16 | c << 8 | c);
			}
		}
		if (talisman != null) {
			talisman.close();
		}
		talisman = mask;
	}

	/** Resource packs changed: colour them again when next asked. */
	public static void clear() {
		CACHE.clear();
	}
}
