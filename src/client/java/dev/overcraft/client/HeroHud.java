package dev.overcraft.client;

import dev.overcraft.Ow;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.Targeting;
import dev.overcraft.kiriko.KirikoStats;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;

/**
 * The hero HUD, laid out like Overwatch's: health bottom-left, ult meter bottom-centre, abilities and ammo
 * bottom-right, the Swift Step target over the crosshair. The portrait and icons come from the player's own Overwatch
 * install (HeroArt); without it, shapes and text.
 */
public final class HeroHud {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int DIM = 0xFF8A93A6;
	private static final int PANEL = 0x90101828;
	private static final int ACCENT = 0xFFF0A030;
	private static final int HEALTH = 0xFFF2F2F2;
	private static final int HEALTH_LOST = 0x60FFFFFF;
	private static final int ULT_READY = 0xFF5BD6FF;

	private HeroHud() {
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		Hero hero = ClientHero.hero();
		if (player == null || hero == null) {
			return;
		}
		HeroSyncPayload s = ClientHero.sync();
		boolean controls = ClientHero.controls();
		Font font = mc.font;
		int w = g.guiWidth();
		int h = g.guiHeight();

		health(g, font, player, hero, 10, h - 34);
		ult(g, font, s, w / 2, h - 30);
		if (controls) {
			abilities(g, font, hero, s, w - 10, h - 34);
			swiftStepTarget(g, font, player, s, w / 2, h / 2 + 10);
		} else {
			Component hint = Component.translatable("overcraft.hud.hero_mode", ClientHero.HERO_MODE.getTranslatedKeyMessage());
			g.text(font, hint, w - 10 - font.width(hint), h - 14, DIM, true);
		}
		if (s.rushed()) {
			Component rush = Component.translatable("overcraft.hud.kitsune_rush");
			g.centeredText(font, rush, w / 2, h - 58, ACCENT);
		}
	}

	private static void health(GuiGraphicsExtractor g, Font font, LocalPlayer player, Hero hero, int x, int y) {
		int max = Math.round(Ow.toOw(player.getMaxHealth()));
		int now = Math.round(Ow.toOw(player.getHealth()));
		// the hero's portrait first, like Overwatch's (when the local asset pack has it)
		Identifier portrait = HeroArt.portrait(hero);
		if (portrait != null) {
			int size = 30;
			g.fill(x - 1, y - 13, x + size + 1, y - 13 + size + 2, PANEL);
			g.blit(portrait, x, y - 12, x + size, y - 12 + size, 0, 1, 0, 1);
			x += size + 5;
		}
		g.text(font, hero.displayName.toUpperCase(Locale.ROOT), x, y - 12, DIM, true);
		big(g, font, String.valueOf(now), x, y, WHITE);
		int numberWidth = font.width(String.valueOf(now)) * 2;
		g.text(font, "/" + max, x + numberWidth + 3, y + 7, DIM, true);
		// segmented bar: one segment per 25 health, like Overwatch's
		int segments = Math.max(1, max / 25);
		int segW = 5;
		int barY = y + 19;
		for (int i = 0; i < segments; i++) {
			int sx = x + i * (segW + 1);
			double segmentHealth = Math.clamp(now - i * 25, 0, 25) / 25.0;
			g.fill(sx, barY, sx + segW, barY + 7, HEALTH_LOST);
			if (segmentHealth > 0) {
				g.fill(sx, barY + (int) Math.round(7 * (1 - segmentHealth)), sx + segW, barY + 7, HEALTH);
			}
		}
	}

	private static void ult(GuiGraphicsExtractor g, Font font, HeroSyncPayload s, int cx, int y) {
		int percent = (int) Math.floor(100 * s.ultCharge() / Math.max(1, s.ultCost()));
		boolean ready = percent >= 100;
		int size = 22;
		g.fill(cx - size, y - size / 2, cx + size, y + size / 2 + 4, PANEL);
		int fillW = (int) Math.round(2 * size * Math.min(1, s.ultCharge() / Math.max(1, s.ultCost())));
		g.fill(cx - size, y + size / 2 + 2, cx - size + fillW, y + size / 2 + 4, ready ? ULT_READY : ACCENT);
		g.outline(cx - size, y - size / 2, 2 * size, size + 4, ready ? ULT_READY : DIM);
		if (s.ultActive()) {
			g.centeredText(font, Component.translatable("overcraft.hud.ult_active"), cx, y - 4, ACCENT);
		} else if (ready) {
			g.centeredText(font, ClientHero.ULTIMATE.getTranslatedKeyMessage(), cx, y - 4, ULT_READY);
		} else {
			g.centeredText(font, percent + "%", cx, y - 4, WHITE);
		}
	}

	private static void abilities(GuiGraphicsExtractor g, Font font, Hero hero, HeroSyncPayload s, int right, int y) {
		// ammo, rightmost: kunai big (its icon beside it), ofuda under it
		String kunai = s.primaryReload() >= 0 ? "--" : String.valueOf(s.primaryAmmo());
		int kunaiW = font.width(kunai) * 2;
		big(g, font, kunai, right - kunaiW - 20, y, s.primaryAmmo() == 0 ? ACCENT : WHITE);
		g.text(font, "/" + KirikoStats.KUNAI_AMMO, right - 18, y + 7, DIM, true);
		Identifier weapon = HeroArt.ability(hero, "kunai");
		if (weapon != null) {
			g.blit(weapon, right - kunaiW - 38, y, right - kunaiW - 24, y + 14, 0, 1, 0, 1);
		}
		reloadBar(g, right - 60, y + 18, 60, s.primaryReload());
		String ofuda = Component.translatable("overcraft.hud.ofuda").getString() + " " + s.secondaryAmmo() + "/" + KirikoStats.OFUDA_AMMO;
		g.text(font, ofuda, right - font.width(ofuda), y + 22, s.secondaryReload() >= 0 ? DIM : WHITE, true);
		reloadBar(g, right - 60, y + 31, 60, s.secondaryReload());

		// abilities, to the left of the ammo
		int box = 22;
		int x = right - 70 - box - (box + 6);
		ability(g, font, hero, "protection_suzu", x, y, box, ClientHero.ABILITY_2, s.ability2Cooldown());
		x -= box + 6;
		ability(g, font, hero, "swift_step", x, y, box, ClientHero.ABILITY_1, s.ability1Cooldown());
	}

	/** An ability's box: its icon (or short name), dimmed with the seconds left while cooling down, the key under it. */
	private static void ability(GuiGraphicsExtractor g, Font font, Hero hero, String ability, int x, int y, int box, KeyMapping key,
								float cooldown) {
		boolean ready = cooldown <= 0;
		g.fill(x, y, x + box, y + box, PANEL);
		Identifier icon = HeroArt.ability(hero, ability);
		if (icon != null) {
			g.blit(icon, x + 2, y + 2, x + box - 2, y + box - 2, 0, 1, 0, 1);
		} else if (ready) {
			g.centeredText(font, Component.translatable("overcraft.ability." + ability + ".short"), x + box / 2, y + box / 2 - 4, WHITE);
		}
		if (!ready) {
			if (icon != null) {
				g.fill(x, y, x + box, y + box, 0xB0101828);
			}
			g.centeredText(font, String.valueOf((int) Math.ceil(cooldown)), x + box / 2, y + box / 2 - 4, ACCENT);
		}
		g.outline(x, y, box, box, ready ? WHITE : DIM);
		g.centeredText(font, shortKey(key), x + box / 2, y + box + 3, DIM);
	}

	/** "Left Shift" -> "SHIFT": fits under a 22-pixel box. */
	public static String shortKey(KeyMapping key) {
		String name = key.getTranslatedKeyMessage().getString().trim();
		int space = name.lastIndexOf(' ');
		return (space >= 0 ? name.substring(space + 1) : name).toUpperCase(Locale.ROOT);
	}

	private static void reloadBar(GuiGraphicsExtractor g, int x, int y, int width, float progress) {
		if (progress < 0) {
			return;
		}
		g.fill(x, y, x + width, y + 2, HEALTH_LOST);
		g.fill(x, y, x + (int) (width * progress), y + 2, ACCENT);
	}

	private static void swiftStepTarget(GuiGraphicsExtractor g, Font font, LocalPlayer player, HeroSyncPayload s, int cx, int y) {
		if (s.ability1Cooldown() > 0) {
			return;
		}
		LivingEntity target = Targeting.allyInView(player, KirikoStats.SWIFT_STEP_RANGE, KirikoStats.SWIFT_STEP_HALF_ANGLE, false);
		if (target != null) {
			g.centeredText(font, Component.translatable("overcraft.hud.swift_step_target", target.getDisplayName()), cx, y, ACCENT);
		}
	}

	private static void big(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(2f, 2f);
		g.text(font, text, 0, 0, color, true);
		g.pose().popMatrix();
	}
}
