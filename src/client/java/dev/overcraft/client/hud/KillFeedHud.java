package dev.overcraft.client.hud;

import dev.overcraft.client.HeroArt;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.hero.Hero;
import dev.overcraft.net.KillFeedPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.image;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * Overwatch's kill feed at the top right (the reference, §1.6: rows about 36 px from x 0.707 to 0.985 of the width):
 * the killer, what they killed with, the victim; names on their team's colour as this player sees it, the hero's
 * portrait beside a hero's name, the line framed white when it's this player's kill. Each line lasts LIFE seconds.
 */
public final class KillFeedHud {
	private static final long LIFE = 6000;
	private static final long FADE = 500;
	private static final int MAX = 6;
	private static final float ROW_H = 34;
	private static final float ROW_GAP = 6;
	private static final float CAP = 13;
	private static final int ALLY = 0x2D6FE0;
	private static final int ENEMY = 0xD93644;

	private record Line(KillFeedPayload kill, long at) {
	}

	private static final ArrayDeque<Line> LINES = new ArrayDeque<>();

	private KillFeedHud() {
	}

	public static void add(KillFeedPayload kill) {
		LINES.addFirst(new Line(kill, Util.getMillis()));
		while (LINES.size() > MAX) {
			LINES.removeLast();
		}
	}

	public static void clear() {
		LINES.clear();
	}

	/** Drawn in the HUD's reference pixels, right edge at right, from top down. */
	public static void render(GuiGraphicsExtractor g, float right, float top) {
		long now = Util.getMillis();
		float y = top;
		for (Iterator<Line> it = LINES.iterator(); it.hasNext(); ) {
			Line line = it.next();
			long age = now - line.at();
			if (age > LIFE) {
				it.remove();
				continue;
			}
			float alpha = Math.clamp((LIFE - age) / (float) FADE, 0, 1);
			row(g, line.kill(), right, y, alpha);
			y += ROW_H + ROW_GAP;
		}
	}

	private static void row(GuiGraphicsExtractor g, KillFeedPayload kill, float right, float y, float alpha) {
		float x = right;
		float left = x;
		// right to left: the victim, the weapon, the killer
		x = side(g, kill.victim(), kill.victimHero(), kill.victimAlly(), x, y, alpha, true);
		float iconW = 46;
		x -= iconW;
		fill(g, x, y, iconW, ROW_H, argb(0x0B1520, 0.7f * alpha));
		Identifier icon = weaponIcon(kill);
		if (icon != null) {
			image(g, icon, x + 5, y + 4, iconW - 10, ROW_H - 8, argb(0xFFFFFF, alpha));
		} else {
			// no weapon to show (a mob's hit, a fall): Overwatch's plain arrow
			image(g, HudShapes.TRIANGLE, x + iconW / 2 - 7, y + ROW_H / 2 - 7, 14, 14, argb(0xFFFFFF, alpha));
		}
		if (!kill.killer().isEmpty()) {
			x = side(g, kill.killer(), kill.killerHero(), kill.killerAlly(), x, y, alpha, false);
		}
		if (kill.byYou()) {
			outline(g, x - 2, y - 2, left - x + 4, ROW_H + 4, 2, argb(0xFFFFFF, 0.9f * alpha));
		}
	}

	/** One side's name plate (and portrait), ending at x; returns its left edge. */
	private static float side(GuiGraphicsExtractor g, String name, String heroId, boolean ally, float x, float y, float alpha,
							  boolean victim) {
		String text = name.toUpperCase(Locale.ROOT);
		float plateW = HudText.width(Face.BODY, text, CAP) + 20;
		Hero hero = heroId.isEmpty() ? null : Hero.byId(heroId);
		Identifier portrait = hero == null ? null : HeroArt.portrait(hero);
		int colour = ally ? ALLY : ENEMY;
		// the victim's portrait sits outside its name (rightmost), the killer's outside its name (leftmost)
		if (victim && portrait != null) {
			x -= ROW_H;
			portrait(g, portrait, x, y, colour, alpha);
		}
		x -= plateW;
		fill(g, x, y, plateW, ROW_H, argb(colour, 0.85f * alpha));
		HudText.draw(g, Face.BODY, text, x + plateW / 2, y + ROW_H / 2 + CAP / 2, CAP, argb(0xFFFFFF, alpha), Align.CENTER);
		if (!victim && portrait != null) {
			x -= ROW_H;
			portrait(g, portrait, x, y, colour, alpha);
		}
		return x;
	}

	private static void portrait(GuiGraphicsExtractor g, Identifier portrait, float x, float y, int colour, float alpha) {
		fill(g, x, y, ROW_H, ROW_H, argb(colour, 0.85f * alpha));
		image(g, portrait, x + 1, y + 1, ROW_H - 2, ROW_H - 2, argb(0xFFFFFF, alpha));
	}

	private static @Nullable Identifier weaponIcon(KillFeedPayload kill) {
		Hero hero = kill.killerHero().isEmpty() ? null : Hero.byId(kill.killerHero());
		if (hero == null || kill.weapon().isEmpty() || kill.weapon().equals("melee")) {
			return null;
		}
		return HeroArt.ability(hero, kill.weapon());
	}

	private static int argb(int rgb, float a) {
		return Math.clamp(Math.round(a * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}
}
