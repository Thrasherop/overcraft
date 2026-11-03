package dev.overcraft.client.hud;

import dev.overcraft.client.HeroArt;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.MobFaces.MobFace;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.hero.Hero;
import dev.overcraft.net.KillFeedPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
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
 * portrait beside a hero's name (else a mob's face or a player's skin face, from Minecraft's textures), the line framed
 * white when it's this player's kill. Each line lasts LIFE seconds.
 */
public final class KillFeedHud {
	private static final long LIFE = 6000;
	private static final long FADE = 500;
	private static final int MAX = 6;
	private static final float ROW_H = 34;
	private static final float ROW_GAP = 6;
	private static final float CAP = 13;
	/** A portrait's width: cropped hero art about 1.5 times as wide as the row is high (the fifth critique: about 52 px). */
	private static final float PORTRAIT_W = 52;
	/** The bright edge on a portrait's inner end, next to the middle (Overwatch's: #30B1DD ally, #AA3F66 enemy). */
	private static final float EDGE = 4;
	private static final int ALLY_EDGE = 0x30B1DD;
	/** Where the face band starts in a hero's 256 px portrait art (ours, read off Kiriko's). */
	private static final int PORTRAIT_TOP = 30;
	private static final int ENEMY_EDGE = 0xAA3F66;
	/** Overwatch's team colours in the feed (the third critique, sampled): teal-blue and crimson. */
	private static final int ALLY = 0x277CA6;
	private static final int ENEMY = 0x8B293C;

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
		x = side(g, kill.victim(), kill.victimHero(), kill.victimType(), kill.victimAlly(), x, y, alpha, true);
		// what it was done with (icons()), then a bold white ">" (about 9 x 14 px, a 3 px stroke)
		java.util.List<Icon> icons = icons(kill);
		float iconsW = 0;
		for (Icon icon : icons) {
			iconsW += icon.width() + 2;
		}
		float iconW = 24 + iconsW;
		x -= iconW;
		fill(g, x, y, iconW, ROW_H, argb(0x0B1520, 0.7f * alpha));
		float at = x + 3;
		for (Icon icon : icons) {
			// an ult's icon pulses, as the ready ult does (the wiki: "animated similar to the one shown when it is fully
			// charged"; the pulse's speed and depth are ours)
			float glow = icon.pulse() ? 0.7f + 0.3f * (float) Math.sin(Util.getMillis() / 1000f * Math.PI * 2 * 1.5f) : 1;
			int rgb = icon.pulse() ? lerp(0xFFC24A, icon.rgb(), glow) : icon.rgb();
			if (icon.text() != null) {
				// no art in the pack: the ability's name, small, in its place
				HudText.plain(g, Face.BODY, icon.text(), at + icon.width() / 2, y + ROW_H / 2 + 4, 9, argb(rgb, alpha), Align.CENTER);
			} else if (icon.glyph()) {
				image(g, icon.id(), at + 2, y + 6, ROW_H - 12, ROW_H - 12, argb(rgb, alpha));
			} else {
				image(g, icon.id(), at, y + 5, ICON_SLOT, ICON_SLOT, argb(rgb, alpha));
			}
			at += icon.width() + 2;
		}
		g.pose().pushMatrix();
		g.pose().translate(x + iconW - 11, y + ROW_H / 2);
		g.pose().rotate((float) Math.toRadians(-90));
		image(g, HudShapes.CHEVRON_BOLD, -9.3f, -6.5f, 18.7f, 15.6f, argb(0xFFFFFF, alpha));
		g.pose().popMatrix();
		if (!kill.killer().isEmpty()) {
			x = side(g, kill.killer(), kill.killerHero(), kill.killerType(), kill.killerAlly(), x, y, alpha, false);
		}
		if (kill.byYou()) {
			outline(g, x - 2, y - 2, left - x + 4, ROW_H + 4, 2, argb(0xFFFFFF, 0.9f * alpha));
		}
	}

	/** One side's name plate (and portrait), ending at x; returns its left edge. */
	private static float side(GuiGraphicsExtractor g, String name, String heroId, String type, boolean ally, float x, float y,
							  float alpha, boolean victim) {
		String text = name.toUpperCase(Locale.ROOT);
		float plateW = HudText.width(Face.BODY, text, CAP) + 20;
		Hero hero = heroId.isEmpty() ? null : Hero.byId(heroId);
		Identifier portrait = hero == null ? null : HeroArt.portrait(hero);
		int colour = ally ? ALLY : ENEMY;
		// a face where there's no hero: a mob's, or a player's skin
		MobFace mob = portrait == null ? MobFaces.get(type) : null;
		PlayerInfo player = portrait == null && mob == null && type.equals("minecraft:player") ? playerInfo(name) : null;
		boolean hasFace = portrait != null || mob != null || player != null;
		// names on the outside, portraits on the inside next to the weapon (Overwatch's: NAME [face] > [face] NAME), each
		// portrait with a bright edge on its inner end, next to the middle
		int edge = ally ? ALLY_EDGE : ENEMY_EDGE;
		if (!victim && hasFace) {
			x -= PORTRAIT_W;
			portrait(g, portrait, mob, player, x, y, colour, alpha);
			fill(g, x + PORTRAIT_W - EDGE, y, EDGE, ROW_H, argb(edge, alpha));
		}
		x -= plateW;
		fill(g, x, y, plateW, ROW_H, argb(colour, 0.9f * alpha));
		// no shadow on the names (Overwatch's have none)
		HudText.plain(g, Face.BODY, text, x + plateW / 2, y + ROW_H / 2 + CAP / 2, CAP, argb(0xFFFFFF, alpha), Align.CENTER);
		if (victim && hasFace) {
			x -= PORTRAIT_W;
			portrait(g, portrait, mob, player, x, y, colour, alpha);
			fill(g, x, y, EDGE, ROW_H, argb(edge, alpha));
		}
		return x;
	}

	/** A hero's portrait, else a mob's face, else a player's skin face, in a box of their team's colour. */
	private static void portrait(GuiGraphicsExtractor g, @Nullable Identifier portrait, @Nullable MobFace mob,
								 @Nullable PlayerInfo player, float x, float y, int colour, float alpha) {
		fill(g, x, y, PORTRAIT_W, ROW_H, argb(colour, 0.85f * alpha));
		int tint = argb(0xFFFFFF, alpha);
		if (portrait != null) {
			// the hero's art cropped to the box's shape: the face band of the square portrait
			float bandH = 256 * ROW_H / PORTRAIT_W;
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(PORTRAIT_W / 256, ROW_H / bandH);
			g.blit(RenderPipelines.GUI_TEXTURED, portrait, 0, 0, 0, PORTRAIT_TOP, 256, Math.round(bandH), 256, Math.round(bandH),
				256, 256, tint);
			g.pose().popMatrix();
		} else if (mob != null) {
			// fitted to the row's height with a margin
			MobFaces.draw(g, mob, x, y + 3, PORTRAIT_W, ROW_H - 6, tint);
		} else if (player != null) {
			g.pose().pushMatrix();
			g.pose().translate(x + (PORTRAIT_W - (ROW_H - 6)) / 2, y + 3);
			g.pose().scale((ROW_H - 6) / 8f, (ROW_H - 6) / 8f);
			PlayerFaceExtractor.extractRenderState(g, player.getSkin(), 0, 0, 8, tint);
			g.pose().popMatrix();
		}
	}

	private static @Nullable PlayerInfo playerInfo(String name) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection == null ? null : connection.getPlayerInfo(name);
	}

	/**
	 * One icon in a row's middle: a hero's ability art, or a square glyph (Overwatch's or ours), its colour, whether it
	 * pulses (an ult's), or a name drawn in its place when the pack has no art.
	 */
	private record Icon(@Nullable Identifier id, boolean glyph, int rgb, boolean pulse, @Nullable String text) {
		Icon(Identifier id, boolean glyph) {
			this(id, glyph, 0xFFFFFF, false, null);
		}

		float width() {
			return text != null ? HudText.width(Face.BODY, text, 9) + 6 : ICON_SLOT;
		}
	}

	/** The headshot mark's red: the crit hit marker's. */
	private static final int HEADSHOT_RED = 0xFF3A3A;

	/** The width (and height) each icon takes in a row's middle: the row less a 5 px margin above and below. */
	private static final float ICON_SLOT = ROW_H - 10;

	/**
	 * The icons between the two sides, left to right (the wiki's "Elimination Feed", 2026-06-19): the weapon's fire none,
	 * only the arrow (the owner's #33 too: Ana's rifle silhouette in her GUI data is her HUD weapon panel's art, not a
	 * feed icon); Quick Melee the fist; an ability, a perk's own damage or the ult its icon (an ult's pulsing); a death with
	 * no killer the skull (#33); a mob's ranged kill only the arrow, its bite the fist; a critical hit the red target after any of them.
	 */
	static java.util.List<Icon> icons(KillFeedPayload kill) {
		java.util.List<Icon> icons = new java.util.ArrayList<>(2);
		for (String token : dev.overcraft.hero.KillFeed.iconTokens(kill.weapon(), kill.killer().isEmpty(), kill.headshot())) {
			if (token.equals("skull")) {
				icons.add(new Icon(HudShapes.SKULL, true));
			} else if (token.equals("fist")) {
				// Overwatch's Quick Melee fist from the local pack, else ours
				Identifier fist = HeroArt.killFeed("melee");
				icons.add(new Icon(fist != null ? fist : HudShapes.FIST, true));
			} else if (token.equals("headshot")) {
				icons.add(new Icon(HudShapes.HEADSHOT, true, HEADSHOT_RED, false, null));
			} else {
				// "icon:<kind>:<slug>": the hero's art, abilities first, then perks; with no art in the pack, its name
				String[] parts = token.split(":", 3);
				Hero hero = kill.killerHero().isEmpty() ? null : Hero.byId(kill.killerHero());
				Identifier art = hero == null ? null : HeroArt.ability(hero, parts[2]);
				if (art == null && hero != null) {
					art = HeroArt.perk(hero, parts[2]);
				}
				String text = art != null ? null : parts[2].replace('_', ' ').toUpperCase(Locale.ROOT);
				icons.add(new Icon(art, false, 0xFFFFFF, parts[1].equals("ult"), text));
			}
		}
		return icons;
	}

	/**
	 * The victim of this player's newest kill line, if it came in the last second: ELIMINATED's name when the victim is
	 * past tracking range and the client can't look it up.
	 */
	public static @Nullable String ownVictim() {
		for (Line line : LINES) {
			if (line.kill().byYou() && Util.getMillis() - line.at() < 1000) {
				return line.kill().victim();
			}
		}
		return null;
	}

	private static int lerp(int from, int to, float t) {
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int gr = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return r << 16 | gr << 8 | b;
	}

	private static int argb(int rgb, float a) {
		return Math.clamp(Math.round(a * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}
}
