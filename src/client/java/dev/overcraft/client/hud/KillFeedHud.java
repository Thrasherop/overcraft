package dev.overcraft.client.hud;

import dev.overcraft.client.HeroArt;
import dev.overcraft.client.hud.HudText.Align;
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
import java.util.Map;

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
	/** Overwatch's team colours in the feed (the third critique, sampled): teal-blue and crimson. */
	private static final int ALLY = 0x277CA6;
	private static final int ENEMY = 0x8B293C;

	private record Line(KillFeedPayload kill, long at) {
	}

	/** Where a mob's face is in its texture (the front of its model's head). */
	private record MobFace(Identifier texture, int u, int v, int w, int h, int texW, int texH) {
	}

	private static MobFace face(String path, int u, int v, int w, int h, int texW, int texH) {
		return new MobFace(Identifier.withDefaultNamespace("textures/entity/" + path + ".png"), u, v, w, h, texW, texH);
	}

	/**
	 * Faces for the mobs a hero meets most (the fourth critique: a mob had no portrait, where Overwatch always shows
	 * one), read off each model's head (front face of its texture box); others get none.
	 */
	private static final Map<String, MobFace> FACES = Map.ofEntries(
		Map.entry("minecraft:zombie", face("zombie/zombie", 8, 8, 8, 8, 64, 64)),
		Map.entry("minecraft:husk", face("zombie/husk", 8, 8, 8, 8, 64, 64)),
		Map.entry("minecraft:drowned", face("zombie/drowned", 8, 8, 8, 8, 64, 64)),
		Map.entry("minecraft:zombie_villager", face("zombie_villager/zombie_villager", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:skeleton", face("skeleton/skeleton", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:stray", face("skeleton/stray", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:wither_skeleton", face("skeleton/wither_skeleton", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:bogged", face("skeleton/bogged", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:creeper", face("creeper/creeper", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:spider", face("spider/spider", 40, 12, 8, 8, 64, 32)),
		Map.entry("minecraft:cave_spider", face("spider/cave_spider", 40, 12, 8, 8, 64, 32)),
		Map.entry("minecraft:enderman", face("enderman/enderman", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:villager", face("villager/villager", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:witch", face("witch/witch", 8, 8, 8, 10, 64, 128)),
		Map.entry("minecraft:pillager", face("illager/pillager", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:vindicator", face("illager/vindicator", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:evoker", face("illager/evoker", 8, 8, 8, 10, 64, 64)),
		Map.entry("minecraft:piglin", face("piglin/piglin", 8, 8, 10, 8, 64, 64)),
		Map.entry("minecraft:piglin_brute", face("piglin/piglin_brute", 8, 8, 10, 8, 64, 64)),
		Map.entry("minecraft:zombified_piglin", face("piglin/zombified_piglin", 8, 8, 10, 8, 64, 64)),
		Map.entry("minecraft:blaze", face("blaze/blaze", 8, 8, 8, 8, 64, 32)),
		Map.entry("minecraft:iron_golem", face("iron_golem/iron_golem", 8, 8, 8, 10, 128, 128)),
		Map.entry("minecraft:pig", face("pig/pig_temperate", 8, 8, 8, 8, 64, 64)),
		Map.entry("minecraft:cow", face("cow/cow_temperate", 6, 6, 8, 8, 64, 64)),
		Map.entry("minecraft:sheep", face("sheep/sheep", 8, 8, 6, 6, 64, 32)),
		Map.entry("minecraft:wolf", face("wolf/wolf", 4, 4, 6, 6, 64, 32)));

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
		float iconW = 50;
		x -= iconW;
		fill(g, x, y, iconW, ROW_H, argb(0x0B1520, 0.7f * alpha));
		// what it was done with (the hero's weapon art; else a fist for melee, a skull for the world's doing: Overwatch
		// always shows one, the fourth critique), then a bold white ">" (about 9 x 14 px, a 3 px stroke)
		Identifier icon = weaponIcon(kill);
		if (icon != null) {
			image(g, icon, x + 3, y + 5, iconW - 24, ROW_H - 10, argb(0xFFFFFF, alpha));
		} else {
			Identifier glyph = kill.killer().isEmpty() ? HudShapes.SKULL : HudShapes.FIST;
			image(g, glyph, x + 6, y + 6, ROW_H - 12, ROW_H - 12, argb(0xFFFFFF, alpha));
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
		MobFace mob = portrait == null ? FACES.get(type) : null;
		PlayerInfo player = portrait == null && mob == null && type.equals("minecraft:player") ? playerInfo(name) : null;
		boolean hasFace = portrait != null || mob != null || player != null;
		// names on the outside, portraits on the inside next to the weapon (Overwatch's: NAME [face] > [face] NAME)
		if (!victim && hasFace) {
			x -= ROW_H;
			portrait(g, portrait, mob, player, x, y, colour, alpha);
		}
		x -= plateW;
		fill(g, x, y, plateW, ROW_H, argb(colour, 0.9f * alpha));
		// a light edge on the box's inner end
		fill(g, victim ? x : x + plateW - 3, y, 3, ROW_H, argb(0x8FD6F0, 0.5f * alpha));
		HudText.draw(g, Face.BODY, text, x + plateW / 2, y + ROW_H / 2 + CAP / 2, CAP, argb(0xFFFFFF, alpha), Align.CENTER);
		if (victim && hasFace) {
			x -= ROW_H;
			portrait(g, portrait, mob, player, x, y, colour, alpha);
		}
		return x;
	}

	/** A hero's portrait, else a mob's face, else a player's skin face, in a box of their team's colour. */
	private static void portrait(GuiGraphicsExtractor g, @Nullable Identifier portrait, @Nullable MobFace mob,
								 @Nullable PlayerInfo player, float x, float y, int colour, float alpha) {
		fill(g, x, y, ROW_H, ROW_H, argb(colour, 0.85f * alpha));
		int tint = argb(0xFFFFFF, alpha);
		if (portrait != null) {
			image(g, portrait, x + 1, y + 1, ROW_H - 2, ROW_H - 2, tint);
		} else if (mob != null) {
			// pixel art kept sharp, fitted to the box with a margin, centred (some faces are taller than wide)
			float inner = ROW_H - 6;
			float scale = inner / Math.max(mob.w(), mob.h());
			float fw = mob.w() * scale;
			float fh = mob.h() * scale;
			g.pose().pushMatrix();
			g.pose().translate(x + (ROW_H - fw) / 2, y + (ROW_H - fh) / 2);
			g.pose().scale(scale, scale);
			g.blit(RenderPipelines.GUI_TEXTURED, mob.texture(), 0, 0, mob.u(), mob.v(), mob.w(), mob.h(), mob.w(), mob.h(),
				mob.texW(), mob.texH(), tint);
			g.pose().popMatrix();
		} else if (player != null) {
			g.pose().pushMatrix();
			g.pose().translate(x + 3, y + 3);
			g.pose().scale((ROW_H - 6) / 8f, (ROW_H - 6) / 8f);
			PlayerFaceExtractor.extractRenderState(g, player.getSkin(), 0, 0, 8, tint);
			g.pose().popMatrix();
		}
	}

	private static @Nullable PlayerInfo playerInfo(String name) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection == null ? null : connection.getPlayerInfo(name);
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
