package dev.overcraft.client.hud;

import dev.overcraft.Overcraft;
import dev.overcraft.client.HeroArt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;

/**
 * Text in Overwatch's HUD faces: a bold geometric sans (Futura) for health, ammo and key labels, and the tall italic
 * Big Noodle for the ult charge and cooldowns. The fonts come from the player's own install through the local asset
 * pack (tools/build_asset_pack.py fonts); without it, Minecraft's font stands in (the jar's font/*.json only refer
 * to it). Text is placed by its baseline and sized by its capital height, in the caller's units.
 */
public final class HudText {
	public enum Face {
		BODY("hud", "futura_demibold.ttf", 32 * 0.667f),
		SMALL("hud_small", "futura_demibold.ttf", 16 * 0.667f),
		NUMBERS("hud_numbers", "big_noodle.ttf", 32 * 0.718f);

		final FontDescription font;
		final String file;
		/** Capital height in font units: the font's size (font/*.json) times its letters' share of the em. */
		final float capHeight;

		Face(String font, String file, float capHeight) {
			this.font = new FontDescription.Resource(Overcraft.id(font));
			this.file = file;
			this.capHeight = capHeight;
		}

		/** The real font is in the local pack (otherwise Minecraft's, capitals 7 high). */
		boolean real() {
			return HeroArt.has(Overcraft.id("font/" + file));
		}
	}

	public enum Align { LEFT, CENTER, RIGHT }

	/** Minecraft's own font: capitals 7 units high, the baseline 7 below the top. */
	private static final float DEFAULT_CAP = 7;
	/** Every font's baseline sits this far below where its text is drawn (GlyphBitmap.getTop). */
	private static final float BASELINE = 7;

	private HudText() {
	}

	private static MutableComponent styled(Face face, String text) {
		boolean real = face.real();
		return Component.literal(text).withStyle(style -> real
			? style.withFont(face.font)
			: style.withItalic(face == Face.NUMBERS));
	}

	private static float scale(Face face, float cap) {
		return cap / (face.real() ? face.capHeight : DEFAULT_CAP);
	}

	/** Width of the text drawn with capitals cap high. */
	public static float width(Face face, String text, float cap) {
		return Minecraft.getInstance().font.width(styled(face, text)) * scale(face, cap);
	}

	/** Draws text with its baseline at y and capitals cap high, aligned on x. */
	public static void draw(GuiGraphicsExtractor g, Face face, String text, float x, float baseline, float cap, int color,
							Align align) {
		Font font = Minecraft.getInstance().font;
		MutableComponent line = styled(face, text);
		float k = scale(face, cap);
		float w = font.width(line);
		float left = switch (align) {
			case LEFT -> 0;
			case CENTER -> -w / 2;
			case RIGHT -> -w;
		};
		g.pose().pushMatrix();
		g.pose().translate(x, baseline);
		g.pose().scale(k, k);
		g.pose().translate(left, -BASELINE);
		g.text(font, line, 0, 0, color, true);
		g.pose().popMatrix();
	}
}
