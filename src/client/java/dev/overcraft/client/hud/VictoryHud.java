package dev.overcraft.client.hud;

import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.registry.OcSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.Locale;

/**
 * Overwatch's end of a won match (issue #18: killing the Ender Dragon): "VICTORY" in big gold letters across the middle
 * of the screen over a darkened band, the announcer's line and the victory music. Its timings, sizes and colours are
 * ours, from how the game's screen looks; the sounds come from the local pack when it has them.
 */
public final class VictoryHud {
	/** Seconds: growing in, held, fading out. */
	private static final float IN = 0.35f;
	private static final float HOLD = 4.5f;
	private static final float OUT = 0.8f;
	private static final int GOLD = 0xFFFFCF4A;
	private static final int GLOW = 0xFFFFE6A0;

	private static long shownAt = -1;
	/** Dev only (the harness's "victory <slow>"): the banner's clock this many times slower, to look at its entrance frame by frame. */
	public static float devSlow = 1;

	private VictoryHud() {
	}

	/** The match is won: show it, and play the announcer and the music. */
	public static void show() {
		shownAt = Util.getMillis();
		play(OcSounds.ANNOUNCER_VICTORY);
		// the end track, which runs on into the POTG (PotgMusic)
		dev.overcraft.client.PotgMusic.victory(OcSounds.MUSIC_VICTORY);
	}

	/** A sound heard as the interface's (not placed in the world), if the pack has it. */
	private static void play(SoundEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSoundManager().getSoundEvent(event.location()) != null) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(event, 1));
		}
	}

	public static void clear() {
		shownAt = -1;
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (shownAt < 0) {
			return;
		}
		float t = (Util.getMillis() - shownAt) / 1000f / devSlow;
		if (t > IN + HOLD + OUT) {
			shownAt = -1;
			devSlow = 1;
			return;
		}
		float alpha = t < IN ? t / IN : t > IN + HOLD ? 1 - (t - IN - HOLD) / OUT : 1;
		// eased from 125% down to full size as it comes in
		float grow = t < IN ? 1 + 0.25f * (1 - Mth.sin(t / IN * Mth.HALF_PI)) : 1;
		float scale = HudDraw.scale(g);
		float w = g.guiWidth() / scale;
		float h = g.guiHeight() / scale;
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		float cy = h * 0.42f;
		entrance(g, t, w, h, cy);
		// the band it sits on: dark in the middle, fading out above and below
		float band = 300;
		HudDraw.gradient(g, 0, cy - band / 2, w, band / 2, 0, alpha(0x000000, 0.55f * alpha));
		HudDraw.gradient(g, 0, cy, w, band / 2, alpha(0x000000, 0.55f * alpha), 0);
		// a thin line of light under the word
		HudDraw.gradientRight(g, 0, cy + 92, w / 2, 3, 0, alpha(0xFFE6A0, 0.8f * alpha));
		HudDraw.gradientRight(g, w / 2, cy + 92, w / 2, 3, alpha(0xFFE6A0, 0.8f * alpha), 0);
		String word = net.minecraft.network.chat.Component.translatable("overcraft.hud.victory").getString().toUpperCase(Locale.ROOT);
		g.pose().pushMatrix();
		g.pose().translate(w / 2, cy);
		g.pose().scale(grow, grow);
		float cap = 150;
		// a soft glow behind the letters: the word again, larger and faint
		for (int i = 1; i <= 3; i++) {
			float s = 1 + i * 0.012f;
			g.pose().pushMatrix();
			g.pose().scale(s, s);
			HudText.plain(g, Face.NUMBERS, word, 0, cap / 2, cap, alpha(GLOW, 0.12f * alpha), Align.CENTER);
			g.pose().popMatrix();
		}
		// pale as it comes in (the flash's white still on it), gold once it settles
		float pale = t < GLINT_AT ? 1 - Mth.clamp((t - FLASH) / (GLINT_AT - FLASH), 0, 1) : 0;
		HudText.draw(g, Face.NUMBERS, word, 0, cap / 2, cap, alpha(lerpRgb(GOLD, 0xFFF8E8, pale), alpha), Align.CENTER);
		g.pose().popMatrix();
		glint(g, t, w, cy, cap * grow);
		g.pose().popMatrix();
	}

	// ---- the entrance (#155, the owner: "there is like a blue flash and animation as it comes on the screen ... it makes it
	// a lot smoother"). Measured frame by frame from the owner's Overwatch clip (Downloads/kiri potg (11).mp4, 60 fps,
	// 2026-11-16): a white-blue flash fills the screen for about 4 frames; the word shows large and pale while a white ring
	// and a gold double ring open out from the middle and a thin streak of light crosses it; the rings fade by about 0.6 s
	// and a gold glint runs along the word as it settles. The shapes are drawn (the game's own effect art isn't found:
	// music-round9); the timings are the clip's.

	/** Seconds: the flash at full, the rings' life, the glint's run. */
	private static final float FLASH = 0.07f, FLASH_OUT = 0.3f, RINGS = 0.6f, GLINT_AT = 0.42f, GLINT = 0.3f;

	private static void entrance(GuiGraphicsExtractor g, float t, float w, float h, float cy) {
		if (t > RINGS + 0.1f) {
			return;
		}
		// the flash: white-blue over everything, up in a frame, gone by 0.3 s
		float flash = t < FLASH ? t / FLASH : 1 - Mth.clamp((t - FLASH) / (FLASH_OUT - FLASH), 0, 1);
		if (flash > 0) {
			HudDraw.fill(g, 0, 0, w, h, alpha(0xE4F4FF, 0.85f * flash));
			HudDraw.gradient(g, 0, 0, w, h, alpha(0x7FC8FF, 0.35f * flash), alpha(0xB8E2FF, 0.2f * flash));
		}
		float life = Mth.clamp(t / RINGS, 0, 1);
		float fade = 1 - life * life;
		// the streak across the middle: a hot white line in a wide blue glow
		if (t > 0.03f) {
			float s = fade * Mth.clamp((t - 0.03f) / 0.05f, 0, 1);
			HudDraw.gradient(g, 0, cy - 14, w, 14, 0, alpha(0x6FB6FF, 0.65f * s));
			HudDraw.gradient(g, 0, cy, w, 14, alpha(0x6FB6FF, 0.65f * s), 0);
			HudDraw.fill(g, 0, cy - 2.5f, w, 5, alpha(0xFFFFFF, 0.95f * s));
		}
		// the rings: white first and fastest, the gold pair a little behind
		float ease = 1 - (1 - life) * (1 - life);
		ring(g, w / 2, cy, h * (0.12f + 0.55f * ease), 12, alpha(0xFFFFFF, 0.9f * fade));
		if (t > 0.08f) {
			float e2 = 1 - (float) Math.pow(1 - Mth.clamp((t - 0.08f) / (RINGS - 0.08f), 0, 1), 2);
			ring(g, w / 2, cy, h * (0.2f + 0.45f * e2), 14, alpha(0xFFD27A, 0.85f * fade));
			ring(g, w / 2, cy, h * (0.26f + 0.5f * e2), 8, alpha(0xFFE6A0, 0.6f * fade));
		}
	}

	/** A ring of {@code thick} px at radius {@code r}, as short segments round it. */
	private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, float thick, int color) {
		int n = Math.max(96, Math.round(r * 1.5f));
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			float x = cx + (float) Math.cos(a) * r, y = cy + (float) Math.sin(a) * r;
			HudDraw.fill(g, x - thick / 2, y - thick / 2, thick, thick, color);
		}
	}

	/** The gold glint along the word as it settles: a bright band running left to right over it. */
	private static void glint(GuiGraphicsExtractor g, float t, float w, float cy, float cap) {
		float p = (t - GLINT_AT) / GLINT;
		if (p < 0 || p > 1) {
			return;
		}
		float half = cap * 2.2f;
		float x = w / 2 - half + 2 * half * p;
		float band = cap * 0.5f;
		float a = Mth.sin(p * Mth.PI);
		HudDraw.gradientRight(g, x - band, cy - cap * 0.05f, band, cap * 1.1f, 0, alpha(0xFFE9A8, 0.9f * a));
		HudDraw.gradientRight(g, x, cy - cap * 0.05f, band, cap * 1.1f, alpha(0xFFE9A8, 0.9f * a), 0);
	}

	private static int lerpRgb(int from, int to, float k) {
		int r = Math.round(Mth.lerp(k, from >> 16 & 0xFF, to >> 16 & 0xFF));
		int gr = Math.round(Mth.lerp(k, from >> 8 & 0xFF, to >> 8 & 0xFF));
		int b = Math.round(Mth.lerp(k, from & 0xFF, to & 0xFF));
		return r << 16 | gr << 8 | b;
	}

	private static int alpha(int rgb, float a) {
		return Math.clamp(Math.round(a * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}
}
