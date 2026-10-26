package dev.overcraft.client;

import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.hero.Hero;
import dev.overcraft.net.SelectHeroPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;
import java.util.Locale;

/**
 * "Select your hero", shown on joining a world and with H near your bed, laid out like Overwatch's: the hero's
 * full-body art large on the right, name, role and kit on the left, the roster of portraits along the bottom. Click a
 * portrait to see the kit, click it again (or Confirm / Enter) to play it; Esc keeps the current hero (or plain
 * Minecraft). The art comes from the player's own Overwatch install (HeroArt); without it, placeholder letter cards.
 */
public class HeroSelectScreen extends Screen {
	private static final int CARD_W = 32;
	private static final int CARD_H = 32;
	private static final int GAP = 4;
	/** The kit's rows: an icon, the key, the name (and the description when the screen is tall enough). */
	private static final int ICON = 14;
	private static final int BG_TOP = 0xF0101A2E;
	private static final int BG_BOTTOM = 0xF0050912;
	private static final int ACCENT = 0xFFF0A030;
	private static final int DIM = 0xFF8A93A6;

	private final boolean onJoin;
	private Hero preview;
	private @Nullable Button confirm;

	public HeroSelectScreen(boolean onJoin) {
		super(Component.translatable("overcraft.select.title"));
		this.onJoin = onJoin;
		Hero current = ClientHero.hero();
		this.preview = current != null ? current : Hero.KIRIKO;
	}

	@Override
	protected void init() {
		confirm = addRenderableWidget(Button.builder(Component.translatable("overcraft.select.confirm"), b -> confirmPick())
			.bounds(width - 92, height - 28, 80, 20).build());
		updateConfirm();
	}

	private void updateConfirm() {
		if (confirm != null) {
			confirm.active = preview.playable;
			confirm.setMessage(Component.translatable(preview.playable ? "overcraft.select.confirm" : "overcraft.select.coming_soon"));
		}
	}

	private void confirmPick() {
		if (!preview.playable) {
			return;
		}
		ClientPlayNetworking.send(new SelectHeroPayload(preview.id));
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int cardsLeft() {
		int n = Hero.values().length;
		return (width - (n * CARD_W + (n - 1) * GAP)) / 2;
	}

	private int cardsTop() {
		return height - CARD_H - 10;
	}

	private @Nullable Hero cardAt(double mx, double my) {
		int left = cardsLeft();
		int top = cardsTop();
		Hero[] heroes = Hero.values();
		for (int i = 0; i < heroes.length; i++) {
			int x = left + i * (CARD_W + GAP);
			if (mx >= x && mx < x + CARD_W && my >= top && my < top + CARD_H) {
				return heroes[i];
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		Hero clicked = cardAt(event.x(), event.y());
		if (clicked != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			boolean again = clicked == preview;
			preview = clicked;
			updateConfirm();
			if (again || doubleClick) {
				confirmPick();
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_SPACE) {
			confirmPick();
			return true;
		}
		if (event.key() == InputConstants.KEY_LEFT || event.key() == InputConstants.KEY_RIGHT) {
			Hero[] heroes = Hero.values();
			int step = event.key() == InputConstants.KEY_LEFT ? -1 : 1;
			preview = heroes[Math.floorMod(preview.ordinal() + step, heroes.length)];
			updateConfirm();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		g.fillGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		int rosterTop = cardsTop() - 12;

		// the previewed hero's art, large, right of centre (behind everything else): the lit card art (the full-body
		// render is a dark, tinted one)
		Identifier art = HeroArt.card(preview);
		if (art != null) {
			int artH = rosterTop + 4;
			int artW = artH * 368 / 500;
			int artX = Math.max(width / 2, width - artW - 96);
			g.blit(art, artX, 0, artX + artW, artH, 0, 1, 0, 1);
			// fade the art's cut-off bottom into the background
			g.fillGradient(artX, artH - 40, artX + artW, artH, 0x00050912, 0xFF070C18);
		}

		// title, top-left
		scaled(g, title.getString().toUpperCase(Locale.ROOT), 12, 8, 1.5f, 0xFFFFFFFF);
		g.text(font, Component.translatable(onJoin ? "overcraft.select.subtitle_join" : "overcraft.select.subtitle_bed"), 12, 22, DIM, true);

		// the previewed hero: name, role, health, kit
		int infoX = 16;
		int infoY = 38;
		scaled(g, preview.displayName.toUpperCase(Locale.ROOT), infoX, infoY, 3.0f, preview.color);
		int roleX = infoX;
		Identifier badge = HeroArt.role(preview.role);
		if (badge != null) {
			g.blit(badge, infoX, infoY + 25, infoX + 11, infoY + 36, 0, 1, 0, 1);
			roleX += 14;
		}
		g.text(font, Component.translatable("overcraft.role." + preview.role.name().toLowerCase(Locale.ROOT))
			.append("  ·  ").append(Component.translatable("overcraft.select.health", preview.owHealth)), roleX, infoY + 27, DIM, true);
		int y = infoY + 42;
		if (preview.playable) {
			List<Line> lines = kit(preview);
			// descriptions too when there's room above the roster
			boolean descriptions = y + lines.size() * 26 <= rosterTop;
			int row = descriptions ? 26 : 16;
			int keyWidth = 0;
			for (Line line : lines) {
				keyWidth = Math.max(keyWidth, font.width(line.key));
			}
			for (Line line : lines) {
				Identifier icon = HeroArt.ability(preview, line.ability);
				int textX = infoX;
				if (icon != null) {
					g.blit(icon, infoX, y - 3, infoX + ICON, y - 3 + ICON, 0, 1, 0, 1);
					textX += ICON + 4;
				}
				g.text(font, line.key, textX, y, ACCENT, true);
				Component name = Component.translatable("overcraft.ability." + line.ability);
				g.text(font, name, textX + keyWidth + 6, y, 0xFFFFFFFF, true);
				if (descriptions) {
					g.text(font, Component.translatable("overcraft.ability." + line.ability + ".desc"), textX + keyWidth + 6, y + 10, DIM, false);
				}
				y += row;
			}
		} else {
			g.text(font, Component.translatable("overcraft.select.coming_soon_long"), infoX, y, DIM, true);
		}

		// the roster along the bottom
		int left = cardsLeft();
		int top = cardsTop();
		Hero hovered = cardAt(mouseX, mouseY);
		Hero[] heroes = Hero.values();
		Component roleName = Component.translatable("overcraft.role.support");
		g.centeredText(font, roleName, width / 2, rosterTop, DIM);
		Identifier supportBadge = HeroArt.role(Hero.Role.SUPPORT);
		if (supportBadge != null) {
			int bx = width / 2 - font.width(roleName) / 2 - 13;
			g.blit(supportBadge, bx, rosterTop - 2, bx + 11, rosterTop + 9, 0, 1, 0, 1);
		}
		for (int i = 0; i < heroes.length; i++) {
			Hero hero = heroes[i];
			int x = left + i * (CARD_W + GAP);
			int lift = hero == preview ? 3 : hero == hovered ? 1 : 0;
			int cy = top - lift;
			Identifier portrait = HeroArt.portrait(hero);
			if (portrait != null) {
				g.fill(x, cy, x + CARD_W, cy + CARD_H, darken(hero.color));
				g.blit(portrait, x, cy, x + CARD_W, cy + CARD_H, 0, 1, 0, 1);
				if (!hero.playable) {
					g.fill(x, cy, x + CARD_W, cy + CARD_H, 0xA0101420);
					g.centeredText(font, Component.translatable("overcraft.select.soon"), x + CARD_W / 2, cy + CARD_H / 2 - 4, DIM);
				}
			} else {
				int base = hero.playable ? hero.color : 0xFF3A4152;
				g.fillGradient(x, cy, x + CARD_W, cy + CARD_H, base, darken(base));
				scaledCentered(g, hero.displayName.substring(0, 1), x + CARD_W / 2, cy + 4, 2.0f, hero.playable ? 0xFFFFFFFF : 0xFF7A8294);
				if (!hero.playable) {
					g.centeredText(font, Component.translatable("overcraft.select.soon"), x + CARD_W / 2, cy + CARD_H - 10, DIM);
				}
			}
			if (hero == ClientHero.hero()) {
				g.fill(x, cy + CARD_H + 1, x + CARD_W, cy + CARD_H + 3, 0xFFFFFFFF);
			}
			g.outline(x - 1, cy - 1, CARD_W + 2, CARD_H + 2, hero == preview ? ACCENT : hero == hovered ? 0xFFFFFFFF : 0x60FFFFFF);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	/** One row of the kit: the key and the ability (its name, description and icon go by this id). */
	private record Line(String key, String ability) {
	}

	private static List<Line> kit(Hero hero) {
		return switch (hero) {
			case KIRIKO -> List.of(
				new Line(HeroHud.shortKey(HeroControls.get(Action.PRIMARY_FIRE)), "healing_ofuda"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.SECONDARY_FIRE)), "kunai"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.ABILITY_1)), "swift_step"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.ABILITY_2)), "protection_suzu"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.ULTIMATE)), "kitsune_rush"),
				new Line(Component.translatable("overcraft.key.passive").getString(), "wall_climb"));
			default -> List.of();
		};
	}

	private static int darken(int color) {
		int r = (color >> 16 & 0xFF) / 3;
		int gr = (color >> 8 & 0xFF) / 3;
		int b = (color & 0xFF) / 3;
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}

	private void scaled(GuiGraphicsExtractor g, String text, int x, int y, float scale, int color) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(font, text, 0, 0, color, true);
		g.pose().popMatrix();
	}

	private void scaledCentered(GuiGraphicsExtractor g, String text, int cx, int y, float scale, int color) {
		scaled(g, text, (int) (cx - font.width(text) * scale / 2), y, scale, color);
	}
}
