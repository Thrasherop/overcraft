package dev.overcraft.client.screen;

import dev.overcraft.client.hud.HudDraw;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.gradientRight;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * Base of the Overwatch-style screens (hero mode's menu and options): laid out in pixels of a 1080-high screen like the
 * HUD, Overwatch's fonts, the world blurred and darkened behind. Subclasses draw in reference pixels; mouse
 * positions come converted (refX, refY).
 */
public abstract class OwScreen extends Screen {
	protected static final int WHITE = 0xFFFFFFFF;
	protected static final int TEXT = 0xE6DDE3EA;
	protected static final int TEXT_DIM = 0x99C3CBD4;
	protected static final int ORANGE = 0xFFF99E1A;
	protected static final int ROW = 0x1FFFFFFF;
	protected static final int ROW_HOVER = 0x38FFFFFF;
	protected static final int KEY_BOX = 0x8C0B1118;

	protected OwScreen(Component title) {
		super(title);
	}

	protected float scale() {
		return height / HudDraw.REF_H;
	}

	/** The reference screen's width (wider screens are wider here, not taller). */
	protected float refWidth() {
		return width / scale();
	}

	protected float refX(double mouseX) {
		return (float) (mouseX / scale());
	}

	protected float refY(double mouseY) {
		return (float) (mouseY / scale());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		if (minecraft.level != null) {
			extractBlurredBackground(g);
		}
		g.fill(0, 0, width, height, 0x99060A10);
		// darker to the left, behind the menu column, as Overwatch's menus are
		g.pose().pushMatrix();
		g.pose().scale(scale(), scale());
		gradientRight(g, 0, 0, refWidth() * 0.55f, HudDraw.REF_H, 0xB3000000, 0x00000000);
		g.pose().popMatrix();
	}

	/** Starts drawing in reference pixels; end with g.pose().popMatrix(). */
	protected void begin(GuiGraphicsExtractor g) {
		g.pose().pushMatrix();
		g.pose().scale(scale(), scale());
	}

	/** A key's label in a box, as on the HUD: dark with a thin light edge. */
	protected static void keyBox(GuiGraphicsExtractor g, String label, float cx, float cy, int color) {
		float cap = 13;
		float w = Math.max(30, HudText.width(Face.SMALL, label, cap) + 16);
		float h = 26;
		fill(g, cx - w / 2, cy - h / 2, w, h, KEY_BOX);
		outline(g, cx - w / 2, cy - h / 2, w, h, 1.5f, 0xB3E6EEF0);
		HudText.draw(g, Face.SMALL, label, cx, cy + cap / 2, cap, color, Align.CENTER);
	}
}
