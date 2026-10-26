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

	private VictoryHud() {
	}

	/** The match is won: show it, and play the announcer and the music. */
	public static void show() {
		shownAt = Util.getMillis();
		play(OcSounds.ANNOUNCER_VICTORY);
		play(OcSounds.MUSIC_VICTORY);
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
		float t = (Util.getMillis() - shownAt) / 1000f;
		if (t > IN + HOLD + OUT) {
			shownAt = -1;
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
		// the band it sits on: dark in the middle, fading out above and below
		float cy = h * 0.42f;
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
		HudText.draw(g, Face.NUMBERS, word, 0, cap / 2, cap, alpha(GOLD, alpha), Align.CENTER);
		g.pose().popMatrix();
		g.pose().popMatrix();
	}

	private static int alpha(int rgb, float a) {
		return Math.clamp(Math.round(a * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}
}
