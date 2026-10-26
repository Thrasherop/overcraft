package dev.overcraft.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overcraft.Overcraft;
import dev.overcraft.client.HeroArt;
import dev.overcraft.client.model.HeroTexture;
import dev.overcraft.hero.Hero;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The HUD's shapes (discs, rings, the ofuda segment, the mouse icon), drawn here at load as smooth white masks and
 * tinted when drawn: Minecraft's GUI only fills rectangles, and Overwatch's HUD is round and slanted. All original
 * shapes, nothing from the game. Also the hero portrait cut to the HUD's shield shape.
 */
public final class HudShapes {
	public static final Identifier DISC = Overcraft.id("hud/disc");
	public static final Identifier RING = Overcraft.id("hud/ring");
	public static final Identifier GLOW = Overcraft.id("hud/glow");
	public static final Identifier OFUDA_SEGMENT = Overcraft.id("hud/ofuda_segment");
	public static final Identifier OFUDA_SEGMENT_OUTLINE = Overcraft.id("hud/ofuda_segment_outline");
	public static final Identifier MOUSE = Overcraft.id("hud/mouse");
	public static final Identifier MOUSE_LEFT = Overcraft.id("hud/mouse_left");
	public static final Identifier MOUSE_RIGHT = Overcraft.id("hud/mouse_right");
	public static final Identifier PLUS = Overcraft.id("hud/plus");
	public static final Identifier SHIELD = Overcraft.id("hud/shield");
	public static final Identifier SHIELD_BORDER = Overcraft.id("hud/shield_border");
	/** Texture sizes: the masks are drawn at this many pixels and scaled (smoothly, mipmapped) to the HUD's size. */
	public static final int SIZE = 128;
	public static final int MOUSE_W = 48;
	public static final int MOUSE_H = 64;
	public static final int SEGMENT_W = 96;
	public static final int SEGMENT_H = 64;
	public static final int SHIELD_W = 120;
	public static final int SHIELD_H = 132;

	private static boolean made;
	private static final Map<Hero, Identifier> PORTRAITS = new HashMap<>();

	private HudShapes() {
	}

	/** Makes the masks the first time the HUD draws (on the render thread, with the GPU ready). */
	public static void ensure() {
		if (made) {
			return;
		}
		made = true;
		register(DISC, SIZE, SIZE, (x, y) -> circle(x, y, SIZE / 2f, SIZE / 2f, SIZE / 2f - 1));
		register(RING, SIZE, SIZE, (x, y) -> Math.min(circle(x, y, SIZE / 2f, SIZE / 2f, SIZE / 2f - 1),
			1 - circle(x, y, SIZE / 2f, SIZE / 2f, SIZE / 2f - 5)));
		register(GLOW, SIZE, SIZE, (x, y) -> {
			float d = (float) Math.hypot(x - SIZE / 2f, y - SIZE / 2f) / (SIZE / 2f);
			return d >= 1 ? 0 : (1 - d) * (1 - d);
		});
		register(OFUDA_SEGMENT, SEGMENT_W, SEGMENT_H, (x, y) -> segment(x, y, 0));
		register(OFUDA_SEGMENT_OUTLINE, SEGMENT_W, SEGMENT_H, (x, y) -> Math.min(segment(x, y, 0), 1 - segment(x, y, 6)));
		register(MOUSE, MOUSE_W, MOUSE_H, HudShapes::mouse);
		register(MOUSE_LEFT, MOUSE_W, MOUSE_H, (x, y) -> Math.min(mouse(x, y), coverage(MOUSE_W / 2f - 1.5f - x) * coverage(26 - y)));
		register(MOUSE_RIGHT, MOUSE_W, MOUSE_H, (x, y) -> Math.min(mouse(x, y), coverage(x - MOUSE_W / 2f - 1.5f) * coverage(26 - y)));
		register(PLUS, SIZE, SIZE, (x, y) -> Math.max(roundBox(x, y, SIZE / 2f, SIZE / 2f, 14, 48, 6),
			roundBox(x, y, SIZE / 2f, SIZE / 2f, 48, 14, 6)));
		register(SHIELD, SHIELD_W, SHIELD_H, (x, y) -> shield(x, y, 0));
		register(SHIELD_BORDER, SHIELD_W, SHIELD_H, (x, y) -> Math.min(shield(x, y, 0), 1 - shield(x, y, 5)));
	}

	/**
	 * The hero's portrait cut to the HUD's shield shape (from the local asset pack: null without it).
	 */
	public static @Nullable Identifier portrait(Hero hero) {
		if (PORTRAITS.containsKey(hero)) {
			return PORTRAITS.get(hero);
		}
		Identifier source = HeroArt.portrait(hero);
		Identifier id = null;
		if (source != null) {
			try (InputStream in = Minecraft.getInstance().getResourceManager().open(source);
				 NativeImage art = NativeImage.read(in)) {
				// the portrait covers the shield (cropping its sides), from the top: the face sits high, like Overwatch's
				int w = SHIELD_W * 2;
				int h = SHIELD_H * 2;
				float scale = Math.max((float) w / art.getWidth(), (float) h / art.getHeight());
				float left = (art.getWidth() * scale - w) / 2;
				NativeImage cut = new NativeImage(w, h, true);
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						float mask = shield(x / 2f, y / 2f, 3);
						if (mask <= 0) {
							continue;
						}
						int sx = Math.min(art.getWidth() - 1, (int) ((x + left) / scale));
						int sy = Math.min(art.getHeight() - 1, (int) (y / scale));
						int argb = art.getPixel(sx, sy);
						int alpha = Math.round(((argb >>> 24) & 0xFF) * mask);
						cut.setPixel(x, y, (alpha << 24) | (argb & 0xFFFFFF));
					}
				}
				id = Overcraft.id("hud/portrait/" + hero.id);
				Minecraft.getInstance().getTextureManager().register(id, new HeroTexture(id, cut));
			} catch (IOException e) {
				Overcraft.LOG.warn("Couldn't cut {}'s HUD portrait: {}", hero.id, e.toString());
			}
		}
		PORTRAITS.put(hero, id);
		return id;
	}

	/** Resource packs changed: cut the portraits again (the masks stay). */
	public static void clear() {
		PORTRAITS.clear();
	}

	private interface Mask {
		/** Coverage of the pixel centred at (x, y), 0 to 1. */
		float at(float x, float y);
	}

	private static void register(Identifier id, int w, int h, Mask mask) {
		NativeImage image = new NativeImage(w, h, true);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int alpha = Math.round(255 * Math.clamp(mask.at(x + 0.5f, y + 0.5f), 0, 1));
				// white everywhere, even where clear: smooth filtering then never pulls in a dark fringe
				image.setPixel(x, y, (alpha << 24) | 0xFFFFFF);
			}
		}
		Minecraft.getInstance().getTextureManager().register(id, new HeroTexture(id, image));
	}

	/** Coverage from a signed distance (positive inside), antialiased over one pixel. */
	private static float coverage(float inside) {
		return Math.clamp(inside + 0.5f, 0, 1);
	}

	private static float circle(float x, float y, float cx, float cy, float r) {
		return coverage(r - (float) Math.hypot(x - cx, y - cy));
	}

	/** A box of half-size (hx, hy) with corners rounded by r. */
	private static float roundBox(float x, float y, float cx, float cy, float hx, float hy, float r) {
		float qx = Math.abs(x - cx) - hx + r;
		float qy = Math.abs(y - cy) - hy + r;
		float outside = (float) Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) + Math.min(Math.max(qx, qy), 0) - r;
		return coverage(-outside);
	}

	/**
	 * The ofuda counter's segment: a "/" parallelogram with rounded corners, shrunk by inset (for its outline). The
	 * box is sheared: a box of half-width 18 whose top leans 30 px right of its bottom.
	 */
	private static float segment(float x, float y, float inset) {
		float lean = 30f / (SEGMENT_H - 8);
		float ux = x - SEGMENT_W / 2f + (y - SEGMENT_H / 2f) * lean;
		return roundBox(ux, y, 0, SEGMENT_H / 2f, 20 - inset, SEGMENT_H / 2f - 4 - inset, Math.max(2, 7 - inset));
	}

	/** A mouse seen from above: round at the bottom, the buttons on top split by a gap, the wheel a slot. */
	private static float mouse(float x, float y) {
		float cx = MOUSE_W / 2f;
		float body = Math.min(roundBox(x, y, cx, 22, 20, 18, 10), 1);
		float bottom = circle(x, y, cx, 38, 20);
		float base = Math.max(body, Math.min(bottom, coverage(y - 22)));
		float split = coverage(Math.abs(x - cx) - 1.5f) + coverage(y - 26);
		float wheel = 1 - roundBox(x, y, cx, 15, 3.5f, 7, 3.5f);
		return Math.min(base, Math.min(Math.min(split, 1), wheel));
	}

	/**
	 * The portrait's shield: a flat top with cut corners, straight sides, and a bottom that narrows to a short flat
	 * point. Shrunk by inset (the border's inner edge).
	 */
	private static float shield(float x, float y, float inset) {
		float cx = SHIELD_W / 2f;
		float half = SHIELD_W / 2f - 2 - inset;
		float top = 2 + inset;
		float bottom = SHIELD_H - 2 - inset;
		float dx = Math.abs(x - cx);
		float d = Math.min(half - dx, y - top);
		d = Math.min(d, bottom - y);
		// cut top corners
		d = Math.min(d, (half + (y - top) - dx - 12) * 0.7071f);
		// the bottom narrows from 62% of the height to a flat point a third as wide
		float taperStart = top + (bottom - top) * 0.62f;
		float taperWidth = half - (half - half / 3) * Math.clamp((y - taperStart) / (bottom - taperStart), 0, 1);
		float slope = (half - half / 3) / (bottom - taperStart);
		d = Math.min(d, (taperWidth - dx) / (float) Math.sqrt(1 + slope * slope));
		return coverage(d);
	}
}
