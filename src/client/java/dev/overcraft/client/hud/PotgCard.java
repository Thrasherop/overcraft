package dev.overcraft.client.hud;

import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.registry.OcSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Play of the Game's title card (techniques/potg/design.md, step 3): "PLAY OF THE GAME" (string 0DC5, D) over the player's
 * name and hero joined by " AS " (string 0DC6, D: probably that line), and the category (BZ M: the patent's option), on a
 * dark band like VICTORY's, with the announcer's line and cue 5B1F from the local pack. Layout, timings and colours are
 * ours: no card art was found in the data (the note's "What wasn't found"). It's drawn by PotgCard.Screen, the screen held
 * open for the whole showing: it takes every key and click, and no key closes it (owner, 2026-11-11: nobody skips).
 */
public final class PotgCard {
	private PotgCard() {
	}

	private static final int WHITE = 0xFFFFFFFF;
	private static final int ORANGE = 0xFFF7A54A;
	/** Seconds: in, held, out (ours). */
	private static final float IN = 0.3f;
	private static final float OUT = 0.5f;

	private static @Nullable String player;
	private static String hero = "";
	private static String category = "";
	private static float length;
	private static long shownAt = -1;

	/** The card shows for {@code seconds}, with the line and the music. */
	public static void show(String who, String asHero, String what, float seconds) {
		player = who;
		hero = asHero;
		category = what;
		length = seconds;
		shownAt = net.minecraft.util.Util.getMillis();
		play(OcSounds.MUSIC_POTG);
		play(OcSounds.ANNOUNCER_POTG);
	}

	public static void hide() {
		shownAt = -1;
	}

	/** As VictoryHud: the interface's sound, if the pack has it. */
	private static void play(SoundEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSoundManager().getSoundEvent(event.location()) != null) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(event, 1));
		}
	}

	static void render(GuiGraphicsExtractor g) {
		if (shownAt < 0 || player == null) {
			return;
		}
		float t = (net.minecraft.util.Util.getMillis() - shownAt) / 1000f;
		if (t > length) {
			return;
		}
		float alpha = t < IN ? t / IN : t > length - OUT ? Math.max(0, (length - t) / OUT) : 1;
		float slide = t < IN ? (1 - Mth.sin(t / IN * Mth.HALF_PI)) * 40 : 0;
		float scale = HudDraw.scale(g);
		float w = g.guiWidth() / scale;
		float h = g.guiHeight() / scale;
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		float cy = h * 0.42f;
		float band = 360;
		HudDraw.gradient(g, 0, cy - band / 2, w, band / 2, 0, alpha(0x000000, 0.6f * alpha));
		HudDraw.gradient(g, 0, cy, w, band / 2, alpha(0x000000, 0.6f * alpha), 0);
		String title = Component.translatable("overcraft.hud.potg").getString().toUpperCase(Locale.ROOT);
		HudText.draw(g, Face.NUMBERS, title, w / 2 - slide, cy, 120, alpha(WHITE, alpha), Align.CENTER);
		HudDraw.gradientRight(g, 0, cy + 22, w / 2, 3, 0, alpha(0xF7A54A, 0.8f * alpha));
		HudDraw.gradientRight(g, w / 2, cy + 22, w / 2, 3, alpha(0xF7A54A, 0.8f * alpha), 0);
		String line = player.toUpperCase(Locale.ROOT) + (hero.isEmpty() ? "" : " AS " + hero.toUpperCase(Locale.ROOT));
		HudText.draw(g, Face.NUMBERS, line, w / 2 + slide, cy + 80, 52, alpha(ORANGE, alpha), Align.CENTER);
		HudText.draw(g, Face.BODY, category.toUpperCase(Locale.ROOT), w / 2, cy + 120, 22, alpha(WHITE, 0.8f * alpha), Align.CENTER);
		g.pose().popMatrix();
	}

	private static int alpha(int rgb, float a) {
		return Math.clamp(Math.round(a * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}

	/** Held open for the whole showing (card and replay): no world input, nothing closes it but the end. */
	public static final class Holder extends Screen {
		public Holder() {
			super(Component.translatable("overcraft.hud.potg"));
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		public boolean shouldCloseOnEsc() {
			return false;
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			return true;
		}

		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			// nothing: the world (or the replay) shows through
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			render(graphics);
		}
	}
}
