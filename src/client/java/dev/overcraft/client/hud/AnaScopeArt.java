package dev.overcraft.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overcraft.Overcraft;
import dev.overcraft.client.model.HeroTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.resources.Identifier;

/**
 * Ana's scope line art (#90): OURS, redrawn from the owner's frame 1 (2000 x 1121, crosshair (1001, 562)), because the art isn't found in the data
 * (techniques/testing/side_by_side_2026-11-05.md). All of it adds light, which the frame shows: the inner arc reads (255, 255, 255) over the bright wall and (188, 187, 149) over
 * dark stairs at (59, 63, 96); a strut (62, 80, 140) over the same stairs. So one texture, added to the scene, each stroke's colour the difference read off the frame.
 *
 * <p>Places are the frame's pixels, left side, mirrored about the crosshair; widths half-widths. Baked once into a 1920 x 1080 texture, drawn over the reference grid.
 */
public final class AnaScopeArt {
	public static final Identifier TEXTURE = Overcraft.id("hud/ana_scope_art");
	public static final int W = 1920, H = 1080;
	private static final float CX = 1001, CY = 562, K = 1080f / 1121;

	/** Differences against the backdrop (frame 1, measured): the arcs', corners', thin yellow's, blue lines', the small circle's inner ring's, the band's. */
	private static final float[] ARC = {135, 130, 55}, CORNER = {120, 115, 30}, THIN = {95, 95, 40}, BLUE = {0, 26, 62}, RING = {0, 38, 77}, BAND = {0, 22, 50};

	private static boolean made;
	private static float[] rgb;

	private AnaScopeArt() {
	}

	/** Makes the texture the first time it's wanted. */
	public static void ensure() {
		if (made) {
			return;
		}
		made = true;
		rgb = new float[W * H * 3];
		// the inner arcs, tapered (x 735, y 505-618)
		stroke(ARC, 737, 507, 1.5f, 735, 530, 3, 734, 560, 4, 735, 590, 4, 738, 617, 3.5f);
		// the side arcs on the circle r 332 round the crosshair: thin above, thick to (685, 667), a thin tail down to (740, 774); never the whole circle
		circleStroke(BLUE, CX, CY, 332, new float[] {745, 355, 0.6f, 690, 450, 0.7f, 668, 565, 2.5f, 675, 615, 4, 685, 665, 4, 687, 669, 0.7f, 740, 774, 0.6f});
		// the lens arc: centre (1000, 453), r 378, down to its elbow, hooked in to (680, 356)
		stroke(THIN, 680, 356, 1, 654, 325, 1, 650, 314, 1);
		circleStroke(THIN, 1000, 453, 378, new float[] {650, 314, 1, 1000, 75, 1});
		stroke(THIN, 620, 381, 0.8f, 660, 391, 0.8f);
		// the struts: closed capsules, (883, 562) to (964, 680), radius 9
		capsule(BLUE, 883, 562, 964, 680, 9, 1.5f);
		// the thin V under them, down to the crosshair's line
		stroke(BLUE, 927, 565, 0.7f, 1001, 670, 0.7f);
		// the stepped contour from under the struts to the small circle
		stroke(BLUE, 880, 627, 0.7f, 840, 655, 0.7f, 837, 680, 0.7f, 825, 695, 0.7f, 825, 707, 0.7f, 740, 774, 0.7f, 699, 808, 0.7f);
		stroke(BLUE, 787, 580, 0.7f, 850, 580, 0.7f, 905, 662, 0.7f);
		stroke(BLUE, 560, 562, 1, 585, 589, 1, 607, 565, 1);
		// the small circles: outer thin, inner ring visible at 3-4 px (critic's (51, 101, 173))
		circle(BLUE, 687, 819, 31, 0.7f);
		circle(RING, 687, 819, 11, 1.8f);
		// the chevrons: elongated hexagons, narrowing, ending in a small solid wedge
		float[][] rows = {{735, 61}, {758, 56}, {781, 49}, {803, 43}};
		for (float[] row : rows) {
			float y = row[0];
			float x = CX - row[1];
			stroke(BLUE, CX, y - 7, 0.8f, x + 7, y - 7, 0.8f, x, y, 0.8f, x + 7, y + 7, 0.8f, CX, y + 7, 0.8f);
		}
		stroke(BLUE, 977, 818, 2, 985, 822, 3, CX, 822, 3);
		// thin yellow: the curve (596, 635) to (505, 787); the foot (652, 997) to (810, 956); the tick (910, 857) to (875, 915)
		stroke(THIN, 596, 635, 1, 570, 635, 1, 557, 640, 1, 547, 655, 1, 505, 787, 1);
		stroke(BLUE, 560, 662, 0.7f, 535, 735, 0.7f);
		stroke(THIN, 652, 997, 1, 767, 997, 1, 780, 992, 1, 810, 956, 1);
		stroke(THIN, 910, 857, 0.8f, 875, 915, 0.8f);
		// the thick corners, about 3 to 19 wide, round elbows
		stroke(CORNER, 501, 807, 1.5f, 496, 835, 5, 498, 855, 8, 540, 905, 9.5f, 590, 970, 9, 610, 988, 6, 636, 997, 1.2f);
		// the blue band: broad, translucent, following the corner, and its outer stroke
		stroke(BAND, 485, 760, 2, 462, 850, 8, 465, 877, 9, 582, 1012, 9, 640, 1020, 7, 690, 1021, 1.5f);
		stroke(BLUE, 448, 870, 0.8f, 570, 1025, 0.8f, 640, 1032, 0.8f);
		// the contours inside the corner
		stroke(BLUE, 520, 830, 0.7f, 530, 855, 0.7f, 632, 972, 0.7f, 710, 972, 0.7f, 732, 982, 0.7f, 770, 975, 0.7f, 840, 888, 0.7f, 895, 885, 0.7f);
		stroke(BLUE, 532, 858, 0.7f, 627, 960, 0.7f, 702, 960, 0.7f, 715, 972, 0.7f);

		NativeImage image = new NativeImage(W, H, true);
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int i = (y * W + x) * 3;
				float r = Math.min(255, rgb[i]);
				float g = Math.min(255, rgb[i + 1]);
				float b = Math.min(255, rgb[i + 2]);
				float a = Math.max(r, Math.max(g, b));
				// its colour at full strength, how much of it in alpha, for an add by source alpha
				image.setPixel(x, y, a < 1 ? 0x00FFFFFF : Math.round(a) << 24 | Math.round(r * 255 / a) << 16 | Math.round(g * 255 / a) << 8 | Math.round(b * 255 / a));
			}
		}
		rgb = null;
		Minecraft.getInstance().getTextureManager().register(TEXTURE, new HeroTexture(TEXTURE, image, MipmapStrategy.MEAN));
	}

	/** x, y, half-width triples: a polyline, widths running between its points, ends and elbows round; both sides. */
	private static void stroke(float[] colour, float... p) {
		paint(colour, p, false);
		paint(colour, p, true);
	}

	private static void paint(float[] colour, float[] p, boolean mirror) {
		float lox = Float.MAX_VALUE, loy = Float.MAX_VALUE, hix = -Float.MAX_VALUE, hiy = -Float.MAX_VALUE;
		float[] t = new float[p.length];
		for (int i = 0; i < p.length; i += 3) {
			t[i] = W / 2f + (mirror ? -1 : 1) * (p[i] - CX) * K;
			t[i + 1] = H / 2f + (p[i + 1] - CY) * K;
			t[i + 2] = p[i + 2] * K;
			lox = Math.min(lox, t[i] - t[i + 2]);
			hix = Math.max(hix, t[i] + t[i + 2]);
			loy = Math.min(loy, t[i + 1] - t[i + 2]);
			hiy = Math.max(hiy, t[i + 1] + t[i + 2]);
		}
		int x0 = Math.max(0, (int) lox - 2), x1 = Math.min(W - 1, (int) hix + 2), y0 = Math.max(0, (int) loy - 2), y1 = Math.min(H - 1, (int) hiy + 2);
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				float best = 0;
				for (int i = 0; i + 5 < t.length; i += 3) {
					best = Math.max(best, cover(x + 0.5f, y + 0.5f, t[i], t[i + 1], t[i + 2], t[i + 3], t[i + 4], t[i + 5]));
				}
				add(x, y, colour, best);
			}
		}
	}

	private static float cover(float x, float y, float ax, float ay, float aw, float bx, float by, float bw) {
		float dx = bx - ax, dy = by - ay;
		float s = Math.clamp(((x - ax) * dx + (y - ay) * dy) / Math.max(1.0E-4f, dx * dx + dy * dy), 0, 1);
		float d = (float) Math.hypot(x - ax - s * dx, y - ay - s * dy);
		return Math.clamp(aw + (bw - aw) * s - d + 0.5f, 0, 1);
	}

	private static void add(int x, int y, float[] colour, float amount) {
		if (amount > 0) {
			int i = (y * W + x) * 3;
			rgb[i] += colour[0] * amount;
			rgb[i + 1] += colour[1] * amount;
			rgb[i + 2] += colour[2] * amount;
		}
	}

	/** Along a circle between the given places (x, y, half-width triples, taken as angles round its centre), both sides. */
	private static void circleStroke(float[] colour, float cx, float cy, float r, float[] places) {
		java.util.ArrayList<Float> out = new java.util.ArrayList<>();
		for (int i = 0; i + 5 < places.length; i += 3) {
			double a = Math.atan2(places[i + 1] - cy, places[i] - cx);
			double b = Math.atan2(places[i + 4] - cy, places[i + 3] - cx);
			if (a < 0) {
				a += 2 * Math.PI;
			}
			if (b < 0) {
				b += 2 * Math.PI;
			}
			int steps = Math.max(2, (int) (Math.abs(b - a) * r / 6));
			for (int s = 0; s <= steps; s++) {
				double angle = a + (b - a) * s / steps;
				out.add(cx + (float) Math.cos(angle) * r);
				out.add(cy + (float) Math.sin(angle) * r);
				out.add(places[i + 2] + (places[i + 5] - places[i + 2]) * s / steps);
			}
		}
		float[] p = new float[out.size()];
		for (int i = 0; i < p.length; i++) {
			p[i] = out.get(i);
		}
		stroke(colour, p);
	}

	private static void circle(float[] colour, float x, float y, float r, float half) {
		float[] p = new float[3 * 49];
		for (int i = 0; i <= 48; i++) {
			double a = i * 2 * Math.PI / 48;
			p[i * 3] = x + (float) Math.cos(a) * r;
			p[i * 3 + 1] = y + (float) Math.sin(a) * r;
			p[i * 3 + 2] = half;
		}
		stroke(colour, p);
	}

	/** A closed round-ended outline round the segment, both sides. */
	private static void capsule(float[] colour, float ax, float ay, float bx, float by, float r, float half) {
		float len = (float) Math.hypot(bx - ax, by - ay);
		float nx = (by - ay) / len * r, ny = -(bx - ax) / len * r;
		double from = Math.atan2(ny, nx);
		java.util.ArrayList<Float> out = new java.util.ArrayList<>();
		for (int i = 0; i <= 12; i++) {
			double a = from + Math.PI * i / 12;
			out.add(bx + (float) Math.cos(a) * r);
			out.add(by + (float) Math.sin(a) * r);
			out.add(half);
		}
		for (int i = 0; i <= 12; i++) {
			double a = from + Math.PI + Math.PI * i / 12;
			out.add(ax + (float) Math.cos(a) * r);
			out.add(ay + (float) Math.sin(a) * r);
			out.add(half);
		}
		out.add(out.get(0));
		out.add(out.get(1));
		out.add(half);
		float[] p = new float[out.size()];
		for (int i = 0; i < p.length; i++) {
			p[i] = out.get(i);
		}
		stroke(colour, p);
	}
}
