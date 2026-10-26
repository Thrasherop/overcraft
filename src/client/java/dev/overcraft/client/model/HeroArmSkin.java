package dev.overcraft.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Random;

/**
 * A Minecraft arm dressed as the hero, for Minecraft mode's first person (the owner, 2026-10-26: "Let me see what first
 * person kiri in minecraft mode would look like"): her third-person body is Kiriko there, and her own skin's arm in
 * first person didn't match. A player skin texture with only the arms painted, at four times Minecraft's resolution
 * (the arm model's UVs are fractions of the texture, so finer texels just add detail), row by row from shoulder to
 * fingers so it fits both the wide and the slim arm. Colours read off her model (ours, after her red jacket, black
 * wristband and fingerless glove).
 */
public final class HeroArmSkin {
	private static final int SCALE = 4;
	private static final int SLEEVE = 0x8F1F2C;
	private static final int SLEEVE_DARK = 0x6A1520;
	private static final int BAND = 0x232327;
	private static final int GLOVE = 0x38383D;
	private static final int GLOVE_RED = 0x8F1F2C;
	private static final int SKIN = 0xE2A886;
	private static final int SKIN_DARK = 0xB97B5E;

	private static @Nullable Identifier kiriko;

	private HeroArmSkin() {
	}

	/** The hero's arm skin, or null for a hero without one (Minecraft's own arm, the player's skin). */
	public static @Nullable Identifier get(Hero hero) {
		if (hero != Hero.KIRIKO) {
			return null;
		}
		if (kiriko == null) {
			Identifier id = Overcraft.id("hero_arm/kiriko");
			// sharp texels like a skin's (HeroTexture's smooth filtering blurred it)
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, paint()));
			kiriko = id;
		}
		return kiriko;
	}

	private static NativeImage paint() {
		NativeImage image = new NativeImage(64 * SCALE, 64 * SCALE, true);
		Random noise = new Random(7);
		// the arms (right at 40, 16; left at 32, 48) and their sleeve layers (40, 32; 48, 48): only the cuff on those,
		// a little proud of the arm, like her rolled sleeve end
		arm(image, 40, 16, noise);
		arm(image, 32, 48, noise);
		cuff(image, 40, 32);
		cuff(image, 48, 48);
		return image;
	}

	/**
	 * One arm's 16 x 16 texel area at (u, v): the top four rows hold the shoulder end (left part, sleeve) and the hand
	 * end (right part, fingers); the twelve below run round the arm from shoulder (row 0) to fingertips (row 12).
	 */
	private static void arm(NativeImage image, int u, int v, Random noise) {
		for (int y = v * SCALE; y < (v + 16) * SCALE; y++) {
			for (int x = u * SCALE; x < (u + 16) * SCALE; x++) {
				int rgb;
				if (y < (v + 4) * SCALE) {
					// the end faces: the shoulder (top face, sleeve) left of x u+7, the hand's end (fingertips) right of it
					rgb = x < (u + 7) * SCALE ? SLEEVE : SKIN;
				} else {
					float row = (y - (v + 4) * SCALE + 0.5f) / SCALE;
					rgb = row(row, (x - u * SCALE) % (4 * SCALE));
				}
				// a little texture, as cloth and skin have
				image.setPixel(x, y, 0xFF000000 | shade(rgb, 1 + (noise.nextFloat() - 0.5f) * 0.08f));
			}
		}
	}

	/**
	 * The colour at this many texels down the arm (0 the shoulder, 12 the fingertips), xIn texels along its face: her
	 * red sleeve to the cuff, the black wristband, the dark fingerless glove, the fingers.
	 */
	private static int row(float row, int xIn) {
		if (row < 6.4f) {
			// the sleeve, a fold line now and then
			return Math.abs(row - 2.2f) < 0.12f || Math.abs(row - 4.5f) < 0.12f ? SLEEVE_DARK : SLEEVE;
		}
		if (row < 7.4f) {
			// the cuff (its sleeve layer stands a little proud), darker at its lower edge
			return row > 7.1f ? SLEEVE_DARK : SLEEVE;
		}
		if (row < 8.4f) {
			return BAND;
		}
		if (row < 10.9f) {
			// the glove, its red trim along the knuckles
			return row > 10.5f ? GLOVE_RED : GLOVE;
		}
		// the fingers: skin, a dark line between each (one texel apart, a quarter of a Minecraft pixel wide)
		return xIn % SCALE == 0 ? SKIN_DARK : SKIN;
	}

	/** The sleeve layer: the cuff only (rows 6.4 to 7.4 of the arm), the rest clear. */
	private static void cuff(NativeImage image, int u, int v) {
		for (int y = (v + 4) * SCALE; y < (v + 16) * SCALE; y++) {
			float row = (y - (v + 4) * SCALE + 0.5f) / SCALE;
			if (row < 6.4f || row >= 7.4f) {
				continue;
			}
			for (int x = u * SCALE; x < (u + 16) * SCALE; x++) {
				image.setPixel(x, y, 0xFF000000 | (row > 7.1f ? SLEEVE_DARK : SLEEVE));
			}
		}
	}

	private static int shade(int rgb, float k) {
		int r = Math.clamp(Math.round(((rgb >> 16) & 0xFF) * k), 0, 255);
		int g = Math.clamp(Math.round(((rgb >> 8) & 0xFF) * k), 0, 255);
		int b = Math.clamp(Math.round((rgb & 0xFF) * k), 0, 255);
		return r << 16 | g << 8 | b;
	}
}
