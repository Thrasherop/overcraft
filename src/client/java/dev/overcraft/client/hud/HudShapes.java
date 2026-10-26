package dev.overcraft.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overcraft.Overcraft;
import dev.overcraft.client.HeroArt;
import dev.overcraft.client.model.HeroTexture;
import dev.overcraft.hero.Hero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MipmapStrategy;
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
	/** A ring a third as thick as RING: the ult meter's fine outer circle. */
	public static final Identifier RING_THIN = Overcraft.id("hud/ring_thin");
	/** A disc solid to 70% of its radius, then fading out: halos. */
	public static final Identifier SOFT_DISC = Overcraft.id("hud/soft_disc");
	/** A small triangle pointing right (+x). */
	public static final Identifier TRIANGLE = Overcraft.id("hud/triangle");
	/** One ult meter tick, pointing up, with clear margins so its edges filter smoothly at any angle. */
	public static final Identifier TICK = Overcraft.id("hud/tick");
	/** The same tick fading out toward its bottom: drawn over TICK in a brighter colour, a gradient outward. */
	public static final Identifier TICK_OUTER = Overcraft.id("hud/tick_outer");
	public static final int TICK_W = 16;
	public static final int TICK_H = 64;
	/** Fraction of TICK's width the tick fills, and of its height (the rest is margin). */
	public static final float TICK_FILL_W = 0.5f;
	public static final float TICK_FILL_H = 60f / 64;
	/** A horizontal band with soft edges (the middle third of its height), for lines that slope without stair steps. */
	public static final Identifier LINE = Overcraft.id("hud/line");
	/** A soft horizontal glow, brightest along its middle. */
	public static final Identifier LINE_GLOW = Overcraft.id("hud/line_glow");
	public static final int LINE_H = 24;
	/** A square with a two-texel clear margin: boxes that stay smooth when the HUD's curve slopes them (see box()). */
	public static final Identifier SQUARE = Overcraft.id("hud/square");
	/** The same square fading from solid at the top to clear at the bottom: a vertical gradient's top colour. */
	public static final Identifier SQUARE_FADE = Overcraft.id("hud/square_fade");
	public static final int SQUARE_SIZE = 256;
	public static final int SQUARE_MARGIN = 2;
	/** A band round a disc (inner radius 37/47 of the outer): the ult meter's tick band. */
	public static final Identifier BAND_RING = Overcraft.id("hud/band_ring");
	/** Solid white: models drawn a second time as pure light (Suzu's cleanse glow). */
	public static final Identifier WHITE = Overcraft.id("hud/white");
	/** An open "v" (two strokes): the ally marker's chevron. */
	public static final Identifier CHEVRON = Overcraft.id("hud/chevron");
	/** The same, bold: the kill feed's ">" (turned). */
	public static final Identifier CHEVRON_BOLD = Overcraft.id("hud/chevron_bold");
	/** A fist (the kill feed's melee kill) and a skull (killed by the world: falls, lava). Ours, after Overwatch's. */
	public static final Identifier FIST = Overcraft.id("hud/fist");
	public static final Identifier SKULL = Overcraft.id("hud/skull");
	/** Low health's red edges: deepest in the corners, mottled (ours, after Overwatch's smoky edges). */
	public static final Identifier LOW_HEALTH = Overcraft.id("hud/low_health");
	public static final Identifier GLOW = Overcraft.id("hud/glow");
	public static final Identifier OFUDA_SEGMENT = Overcraft.id("hud/ofuda_segment");
	public static final Identifier OFUDA_SEGMENT_OUTLINE = Overcraft.id("hud/ofuda_segment_outline");
	public static final Identifier MOUSE = Overcraft.id("hud/mouse");
	public static final Identifier MOUSE_LEFT = Overcraft.id("hud/mouse_left");
	public static final Identifier MOUSE_RIGHT = Overcraft.id("hud/mouse_right");
	/** The mouse's body grown a little and without the button split or wheel: drawn dark under it, an outline. */
	public static final Identifier MOUSE_OUTLINE = Overcraft.id("hud/mouse_outline");
	public static final Identifier PLUS = Overcraft.id("hud/plus");
	public static final Identifier SHIELD = Overcraft.id("hud/shield");
	public static final Identifier SHIELD_BORDER = Overcraft.id("hud/shield_border");
	/** Clear in the middle, soft toward the edges: screen-edge tints (Kitsune Rush's mist, Swift Step's flash). */
	public static final Identifier VIGNETTE = Overcraft.id("hud/vignette");
	/** A soft streak, brightest near one end: speed lines across the screen. */
	public static final Identifier STREAK = Overcraft.id("hud/streak");
	public static final int STREAK_W = 32;
	public static final int STREAK_H = 128;
	/** Texture sizes: the masks are drawn at this many pixels and scaled (smoothly, mipmapped) to the HUD's size. */
	public static final int SIZE = 128;
	public static final int MOUSE_W = 48;
	public static final int MOUSE_H = 64;
	public static final int SEGMENT_W = 96;
	public static final int SEGMENT_H = 72;
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
		register(RING_THIN, SIZE, SIZE, (x, y) -> Math.min(circle(x, y, SIZE / 2f, SIZE / 2f, SIZE / 2f - 1),
			1 - circle(x, y, SIZE / 2f, SIZE / 2f, SIZE / 2f - 2.6f)));
		register(SOFT_DISC, SIZE, SIZE, (x, y) -> {
			float d = (float) Math.hypot(x - SIZE / 2f, y - SIZE / 2f) / (SIZE / 2f);
			float t = Math.clamp((1 - d) / 0.3f, 0, 1);
			return t * t * (3 - 2 * t);
		});
		register(TRIANGLE, 32, 32, (x, y) -> triangle(x, y, 5, 4, 28, 16, 5, 28));
		register(TICK, TICK_W, TICK_H, HudShapes::tick);
		register(TICK_OUTER, TICK_W, TICK_H, (x, y) -> tick(x, y) * Math.clamp(1.15f - y / TICK_H * 1.3f, 0, 1));
		// a 3 px core and about 2 px of falloff each side, at 9 px drawn (Overwatch's: "5-7 px, bright core, falloff")
		register(LINE, 8, LINE_H, (x, y) -> {
			float d = Math.abs(y - LINE_H / 2f);
			return d <= LINE_H / 6f ? 1 : Math.clamp((LINE_H * 0.39f - d) / (LINE_H * 0.39f - LINE_H / 6f), 0, 1) * 0.55f;
		});
		register(SQUARE, SQUARE_SIZE, SQUARE_SIZE, (x, y) -> square(x, y));
		register(SQUARE_FADE, SQUARE_SIZE, SQUARE_SIZE, (x, y) -> square(x, y) * Math.clamp(1 - y / SQUARE_SIZE, 0, 1));
		register(BAND_RING, SIZE, SIZE, (x, y) -> Math.min(circle(x, y, SIZE / 2f, SIZE / 2f, SIZE / 2f - 1),
			1 - circle(x, y, SIZE / 2f, SIZE / 2f, (SIZE / 2f - 1) * 37 / 47)));
		register(LOW_HEALTH, SIZE, SIZE, HudShapes::lowHealth);
		register(WHITE, 16, 16, (x, y) -> 1);
		register(CHEVRON, 48, 40, (x, y) -> Math.max(stroke(x, y, 6, 8, 24, 32, 3.5f), stroke(x, y, 24, 32, 42, 8, 3.5f)));
		register(CHEVRON_BOLD, 48, 40, (x, y) -> Math.max(stroke(x, y, 8, 9, 24, 31, 7.5f), stroke(x, y, 24, 31, 40, 9, 7.5f)));
		register(FIST, 64, 64, HudShapes::fist);
		register(SKULL, 64, 64, HudShapes::skull);
		register(LINE_GLOW, 8, LINE_H, (x, y) -> {
			float d = (y - LINE_H / 2f) / (LINE_H / 2f);
			return (float) Math.exp(-d * d * 4) * coverage(LINE_H / 2f - 1 - Math.abs(y - LINE_H / 2f));
		});
		register(GLOW, SIZE, SIZE, (x, y) -> {
			float d = (float) Math.hypot(x - SIZE / 2f, y - SIZE / 2f) / (SIZE / 2f);
			return d >= 1 ? 0 : (1 - d) * (1 - d);
		});
		register(OFUDA_SEGMENT, SEGMENT_W, SEGMENT_H, (x, y) -> segment(x, y, 0));
		register(OFUDA_SEGMENT_OUTLINE, SEGMENT_W, SEGMENT_H, (x, y) -> Math.min(segment(x, y, 0), 1 - segment(x, y, 6)));
		register(MOUSE, MOUSE_W, MOUSE_H, HudShapes::mouse);
		register(MOUSE_OUTLINE, MOUSE_W, MOUSE_H, (x, y) -> mouseBody(x, y, 3));
		register(MOUSE_LEFT, MOUSE_W, MOUSE_H, (x, y) -> Math.min(mouse(x, y), coverage(MOUSE_W / 2f - 1.5f - x) * coverage(25 - y)));
		register(MOUSE_RIGHT, MOUSE_W, MOUSE_H, (x, y) -> Math.min(mouse(x, y), coverage(x - MOUSE_W / 2f - 1.5f) * coverage(25 - y)));
		register(PLUS, SIZE, SIZE, (x, y) -> Math.max(roundBox(x, y, SIZE / 2f, SIZE / 2f, 14, 48, 6),
			roundBox(x, y, SIZE / 2f, SIZE / 2f, 48, 14, 6)));
		register(SHIELD, SHIELD_W, SHIELD_H, (x, y) -> shield(x, y, 0));
		register(VIGNETTE, SIZE, SIZE, (x, y) -> {
			float dx = x / SIZE * 2 - 1;
			float dy = y / SIZE * 2 - 1;
			float t = Math.clamp(((float) Math.hypot(dx, dy) - 0.55f) / 0.75f, 0, 1);
			return t * t * (3 - 2 * t);
		});
		register(STREAK, STREAK_W, STREAK_H, (x, y) -> {
			float across = (x - STREAK_W / 2f) / (STREAK_W / 2f);
			float along = y / STREAK_H;
			// thin and soft across; along, rising fast to a bright head at 80% and fading to the tail
			float body = along < 0.8f ? along / 0.8f : (1 - along) / 0.2f;
			return (float) Math.exp(-across * across * 9) * body * body;
		});
		register(SHIELD_BORDER, SHIELD_W, SHIELD_H, (x, y) -> Math.min(shield(x, y, 0), 1 - shield(x, y, 5)));
	}

	/**
	 * The hero's portrait cut to the HUD's shield shape (from the local asset pack: null without it).
	 */
	public static @Nullable Identifier portrait(Hero hero) {
		if (PORTRAITS.containsKey(hero)) {
			return PORTRAITS.get(hero);
		}
		Identifier head = HeroArt.head(hero);
		if (head != null && cutHead(hero, head)) {
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
				Minecraft.getInstance().getTextureManager().register(id, new HeroTexture(id, cut, MipmapStrategy.MEAN));
			} catch (IOException e) {
				Overcraft.LOG.warn("Couldn't cut {}'s HUD portrait: {}", hero.id, e.toString());
			}
		}
		PORTRAITS.put(hero, id);
		return id;
	}

	/** The part of the hero's HUD head above the frame's top (POP_H texels high, then the frame's top band), or null. */
	public static @Nullable Identifier portraitPop(Hero hero) {
		portrait(hero);
		return POPS.get(hero);
	}

	/** How far the HUD head breaks out above the frame, in the frame's texels (two a pixel), and the band it overlaps. */
	public static final int POP_H = 52;
	public static final int POP_OVERLAP = 10;
	private static final Map<Hero, Identifier> POPS = new HashMap<>();

	/**
	 * Cuts Overwatch's HUD head art into the frame (portrait) and the part breaking out over its top (portraitPop): the
	 * head scaled to about 86% of the frame's width, its hair top POP_H texels above the frame, the shoulders below the
	 * frame's V clipped (round 1 #27: the head breaks out over the top; ours was clipped inside). False if it can't.
	 */
	private static boolean cutHead(Hero hero, Identifier source) {
		try (InputStream in = Minecraft.getInstance().getResourceManager().open(source);
			 NativeImage art = NativeImage.read(in)) {
			// the head's opaque bounds in the art
			int x0 = art.getWidth(), y0 = art.getHeight(), x1 = -1, y1 = -1;
			for (int y = 0; y < art.getHeight(); y++) {
				for (int x = 0; x < art.getWidth(); x++) {
					if ((art.getPixel(x, y) >>> 24) > 16) {
						x0 = Math.min(x0, x);
						y0 = Math.min(y0, y);
						x1 = Math.max(x1, x);
						y1 = Math.max(y1, y);
					}
				}
			}
			if (x1 < 0) {
				return false;
			}
			int w = SHIELD_W * 2;
			int h = SHIELD_H * 2;
			float scale = 0.86f * w / (x1 - x0 + 1);
			float centreX = (x0 + x1 + 1) / 2f;
			// art pixel for frame texel (x, y): y measured from the frame's top, the hair top at -POP_H
			NativeImage inside = new NativeImage(w, h, true);
			NativeImage pop = new NativeImage(w, POP_H + POP_OVERLAP, true);
			for (int y = -POP_H; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int sx = Math.round(centreX + (x + 0.5f - w / 2f) / scale);
					int sy = Math.round(y0 + (y + POP_H + 0.5f) / scale);
					if (sx < 0 || sy < 0 || sx >= art.getWidth() || sy >= art.getHeight()) {
						continue;
					}
					int argb = art.getPixel(sx, sy);
					if (y < POP_OVERLAP) {
						// above the frame's top line, and its top band: drawn over the border
						pop.setPixel(x, y + POP_H, argb);
					}
					if (y >= 0) {
						float mask = shield(x / 2f, y / 2f, 3);
						if (mask > 0) {
							inside.setPixel(x, y, Math.round(((argb >>> 24) & 0xFF) * mask) << 24 | (argb & 0xFFFFFF));
						}
					}
				}
			}
			Identifier id = Overcraft.id("hud/portrait/" + hero.id);
			Identifier popId = Overcraft.id("hud/portrait_pop/" + hero.id);
			Minecraft.getInstance().getTextureManager().register(id, new HeroTexture(id, inside, MipmapStrategy.MEAN));
			Minecraft.getInstance().getTextureManager().register(popId, new HeroTexture(popId, pop, MipmapStrategy.MEAN));
			PORTRAITS.put(hero, id);
			POPS.put(hero, popId);
			return true;
		} catch (IOException e) {
			Overcraft.LOG.warn("Couldn't cut {}'s HUD head: {}", hero.id, e.toString());
			return false;
		}
	}

	/** Resource packs changed: cut the portraits again (the masks stay). */
	public static void clear() {
		PORTRAITS.clear();
		POPS.clear();
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
		Minecraft.getInstance().getTextureManager().register(id, new HeroTexture(id, image, MipmapStrategy.MEAN));
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
		return coverage(-roundBoxDistance(x - cx, y - cy, 2 * hx, 2 * hy, r));
	}

	/** Signed distance (positive outside) from a centred box of full size (w, h) with corners rounded by r. */
	private static float roundBoxDistance(float x, float y, float w, float h, float r) {
		float qx = Math.abs(x) - w / 2 + r;
		float qy = Math.abs(y) - h / 2 + r;
		return (float) Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) + Math.min(Math.max(qx, qy), 0) - r;
	}

	/** A triangle (corners in any order). */
	private static float triangle(float x, float y, float ax, float ay, float bx, float by, float cx, float cy) {
		float d = Math.min(edge(x, y, ax, ay, bx, by, cx, cy), Math.min(edge(x, y, bx, by, cx, cy, ax, ay),
			edge(x, y, cx, cy, ax, ay, bx, by)));
		return coverage(d);
	}

	/** Signed distance from the line through a and b, positive on the side of c. */
	private static float edge(float x, float y, float ax, float ay, float bx, float by, float cx, float cy) {
		float nx = ay - by;
		float ny = bx - ax;
		float len = (float) Math.hypot(nx, ny);
		float d = ((x - ax) * nx + (y - ay) * ny) / len;
		float side = (cx - ax) * nx + (cy - ay) * ny;
		return side < 0 ? -d : d;
	}

	/** A line from a to b, w thick, with round ends. */
	private static float stroke(float x, float y, float ax, float ay, float bx, float by, float w) {
		float dx = bx - ax;
		float dy = by - ay;
		float t = Math.clamp(((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy), 0, 1);
		return coverage(w / 2 - (float) Math.hypot(x - ax - t * dx, y - ay - t * dy));
	}

	private static float square(float x, float y) {
		float m = SQUARE_MARGIN;
		return coverage(Math.min(Math.min(x - m, SQUARE_SIZE - m - x), Math.min(y - m, SQUARE_SIZE - m - y)));
	}

	/**
	 * Red at the screen's edges: strongest at the edge, gone 14% of the way in (top and bottom; 10% at the sides),
	 * heavier in the corners, broken up by soft noise so it reads as smoke rather than a gradient.
	 */
	private static float lowHealth(float x, float y) {
		float u = x / SIZE;
		float v = y / SIZE;
		float side = Math.clamp(1 - Math.min(u, 1 - u) / 0.10f, 0, 1);
		float topBottom = Math.clamp(1 - Math.min(v, 1 - v) / 0.15f, 0, 1);
		// the corners reach much further in (Overwatch's: about 400 x 250 px at 1080p)
		float cu = Math.min(u, 1 - u) / 0.24f;
		float cv = Math.min(v, 1 - v) / 0.26f;
		float corner = Math.clamp(1 - (float) Math.hypot(cu, cv), 0, 1);
		float edge = Math.max(Math.max(side, topBottom), corner * 1.1f) + side * topBottom * 0.5f;
		float noise = 0.65f + 0.35f * (float) (Math.sin(u * 37 + Math.sin(v * 23) * 2) * Math.sin(v * 29 + Math.sin(u * 17) * 2));
		return Math.clamp(edge * edge * noise * 1.2f, 0, 1);
	}

	/** The tick: half the texture wide, the full height less a margin, its ends squared. */
	private static float tick(float x, float y) {
		float halfW = TICK_W * TICK_FILL_W / 2;
		float halfH = TICK_H * TICK_FILL_H / 2;
		return coverage(halfW - Math.abs(x - TICK_W / 2f)) * coverage(halfH - Math.abs(y - TICK_H / 2f));
	}

	/**
	 * The ofuda counter's segment: a rounded rectangle about 30 x 15 px, its long sides rising 40 degrees to the right
	 * (measured at 12x on Overwatch's frames, the third critique; an earlier flat-topped parallelogram read as hazard
	 * stripes; at 25 x 14 the row had gaps where Overwatch's tiles overlap, the fourth), here at two texels a pixel.
	 * Shrunk by inset (for its outline).
	 */
	private static float segment(float x, float y, float inset) {
		float px = x - SEGMENT_W / 2f;
		float py = y - SEGMENT_H / 2f;
		float c = (float) Math.cos(Math.toRadians(40));
		float s = (float) Math.sin(Math.toRadians(40));
		// u along the long side (up and to the right on screen: y runs down), v across it
		float u = px * c - py * s;
		float v = px * s + py * c;
		return coverage(-roundBoxDistance(u, v, 60 - 2 * inset, 30 - 2 * inset, Math.max(1, 6 - inset)));
	}

	/** A fist seen from the front: four knuckles over the back of the hand, the thumb across, a wrist below. */
	private static float fist(float x, float y) {
		float hand = roundBox(x, y, 32, 36, 18, 13, 6);
		float wrist = roundBox(x, y, 33, 54, 10, 8, 3);
		float knuckles = 0;
		for (int i = 0; i < 4; i++) {
			knuckles = Math.max(knuckles, roundBox(x, y, 18.5f + i * 9, 24, 4.5f, 8, 4));
		}
		float shape = Math.max(Math.max(hand, wrist), knuckles);
		// grooves between the fingers, and the thumb's edge across them
		float grooves = 0;
		for (int i = 1; i < 4; i++) {
			grooves = Math.max(grooves, stroke(x, y, 14 + i * 9, 18, 14 + i * 9, 30, 2.6f));
		}
		float thumb = stroke(x, y, 15, 38, 40, 33, 2.8f);
		float wristLine = stroke(x, y, 22, 49.5f, 44, 49.5f, 2.4f);
		return Math.min(shape, 1 - Math.max(Math.max(grooves, thumb), wristLine));
	}

	/** A skull from the front: the dome, the jaw, two eyes, the nose, teeth. */
	private static float skull(float x, float y) {
		float shape = Math.max(circle(x, y, 32, 27, 20), roundBox(x, y, 32, 46, 12, 8, 3));
		float eyes = Math.max(circle(x, y, 24, 29, 6), circle(x, y, 40, 29, 6));
		float nose = triangle(x, y, 32, 35, 29, 41, 35, 41);
		float teeth = 0;
		for (int i = -1; i <= 1; i++) {
			teeth = Math.max(teeth, stroke(x, y, 32 + i * 6, 47, 32 + i * 6, 54, 2.6f));
		}
		return Math.min(shape, 1 - Math.max(Math.max(eyes, nose), teeth));
	}

	/** A mouse seen from above: round at the bottom, the buttons on top split by a gap, the wheel a slot. */
	private static float mouse(float x, float y) {
		float cx = MOUSE_W / 2f;
		float split = coverage(Math.abs(x - cx) - 1.5f) + coverage(y - 25);
		float wheel = 1 - roundBox(x, y, cx, 14, 3.5f, 6.5f, 3.5f);
		return Math.min(mouseBody(x, y, 0), Math.min(Math.min(split, 1), wheel));
	}

	/** The mouse's outline shape, grown by grow texels. */
	private static float mouseBody(float x, float y, float grow) {
		float cx = MOUSE_W / 2f;
		float body = roundBox(x, y, cx, 23, 18 + grow, 18 + grow, 10 + grow);
		float bottom = circle(x, y, cx, 40, 18 + grow);
		return Math.max(body, Math.min(bottom, coverage(y - 23)));
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
		// the bottom narrows from 62% of the height to a point, just blunted (Overwatch's frame ends in a V: round 1 #27;
		// it was flat, a third as wide)
		float tip = half * 0.06f;
		float taperStart = top + (bottom - top) * 0.62f;
		float taperWidth = half - (half - tip) * Math.clamp((y - taperStart) / (bottom - taperStart), 0, 1);
		float slope = (half - tip) / (bottom - taperStart);
		d = Math.min(d, (taperWidth - dx) / (float) Math.sqrt(1 + slope * slope));
		return coverage(d);
	}
}
