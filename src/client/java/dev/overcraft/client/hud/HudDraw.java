package dev.overcraft.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Drawing at fractional positions and sizes, for the Overwatch-style HUD and menus: they're laid out in pixels of a
 * 1080-high screen and scaled, so a unit there is a quarter of a GUI unit or less, finer than Minecraft's whole-unit
 * fill and blit.
 */
public final class HudDraw {
	/** The reference screen height the HUD and menus are laid out in. */
	public static final float REF_H = 1080;

	private HudDraw() {
	}

	/** Scale from GUI units to reference pixels for this screen. */
	public static float scale(GuiGraphicsExtractor g) {
		return g.guiHeight() / REF_H;
	}

	public static void fill(GuiGraphicsExtractor g, float x, float y, float w, float h, int color) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(w / 64, h / 64);
		g.fill(0, 0, 64, 64, color);
		g.pose().popMatrix();
	}

	/** A vertical gradient. */
	public static void gradient(GuiGraphicsExtractor g, float x, float y, float w, float h, int top, int bottom) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(w / 64, h / 64);
		g.fillGradient(0, 0, 64, 64, top, bottom);
		g.pose().popMatrix();
	}

	/** A horizontal gradient, left colour to right colour. */
	public static void gradientRight(GuiGraphicsExtractor g, float x, float y, float w, float h, int left, int right) {
		g.pose().pushMatrix();
		g.pose().translate(x, y + h);
		g.pose().rotate((float) (-Math.PI / 2));
		g.pose().scale(h / 64, w / 64);
		g.fillGradient(0, 0, 64, 64, left, right);
		g.pose().popMatrix();
	}

	public static void outline(GuiGraphicsExtractor g, float x, float y, float w, float h, float t, int color) {
		fill(g, x, y, w, t, color);
		fill(g, x, y + h - t, w, t, color);
		fill(g, x, y + t, t, h - 2 * t, color);
		fill(g, x + w - t, y + t, t, h - 2 * t, color);
	}

	/** A texture stretched over (x, y, w, h), tinted. */
	public static void image(GuiGraphicsExtractor g, Identifier texture, float x, float y, float w, float h, int color) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(w / 256, h / 256);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, 256, 256, 256, 256, color);
		g.pose().popMatrix();
	}
}
