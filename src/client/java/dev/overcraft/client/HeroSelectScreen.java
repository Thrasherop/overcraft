package dev.overcraft.client;

import dev.overcraft.hero.Hero;
import dev.overcraft.net.SelectHeroPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;
import java.util.Locale;

/**
 * "Select your hero", shown on joining a world and with H near your bed. Portraits along the bottom like Overwatch's
 * hero select; click one to see the kit, click it again (or Confirm / Enter) to play it. Esc keeps the current hero
 * (or plain Minecraft). Placeholder portraits until the hero art comes from the player's own Overwatch install.
 */
public class HeroSelectScreen extends Screen {
	private static final int CARD_W = 44;
	private static final int CARD_H = 52;
	private static final int GAP = 6;
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
			.bounds(width - 110, height - 34, 96, 20).build());
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
		return height - CARD_H - 44;
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
		// title, top-left
		scaled(g, title.getString().toUpperCase(Locale.ROOT), 16, 14, 2.0f, 0xFFFFFFFF);
		g.text(font, Component.translatable(onJoin ? "overcraft.select.subtitle_join" : "overcraft.select.subtitle_bed"), 16, 36, DIM, true);

		// the previewed hero: name, role, health, kit
		int infoX = 24;
		int infoY = 62;
		scaled(g, preview.displayName.toUpperCase(Locale.ROOT), infoX, infoY, 3.0f, preview.color);
		g.text(font, Component.translatable("overcraft.role." + preview.role.name().toLowerCase(Locale.ROOT))
			.append("  ·  ").append(Component.translatable("overcraft.select.health", preview.owHealth)), infoX, infoY + 30, DIM, true);
		int y = infoY + 48;
		if (preview.playable) {
			for (Line line : kit(preview)) {
				g.text(font, "[" + line.key + "]", infoX, y, ACCENT, true);
				g.text(font, Component.translatable(line.name), infoX + 54, y, 0xFFFFFFFF, true);
				g.text(font, Component.translatable(line.name + ".desc"), infoX + 54, y + 10, DIM, false);
				y += 24;
			}
		} else {
			g.text(font, Component.translatable("overcraft.select.coming_soon_long"), infoX, y, DIM, true);
		}

		// portraits along the bottom
		int left = cardsLeft();
		int top = cardsTop();
		Hero hovered = cardAt(mouseX, mouseY);
		Hero[] heroes = Hero.values();
		g.centeredText(font, Component.translatable("overcraft.role.support"), width / 2, top - 12, DIM);
		for (int i = 0; i < heroes.length; i++) {
			Hero hero = heroes[i];
			int x = left + i * (CARD_W + GAP);
			int lift = hero == preview ? 4 : hero == hovered ? 2 : 0;
			int cy = top - lift;
			int base = hero.playable ? hero.color : 0xFF3A4152;
			g.fillGradient(x, cy, x + CARD_W, cy + CARD_H, base, darken(base));
			scaledCentered(g, hero.displayName.substring(0, 1), x + CARD_W / 2, cy + 10, 3.0f, hero.playable ? 0xFFFFFFFF : 0xFF7A8294);
			g.fill(x, cy + CARD_H - 12, x + CARD_W, cy + CARD_H, 0xC0000000);
			g.centeredText(font, hero.displayName.toUpperCase(Locale.ROOT), x + CARD_W / 2, cy + CARD_H - 10, hero.playable ? 0xFFFFFFFF : DIM);
			if (!hero.playable) {
				g.centeredText(font, Component.translatable("overcraft.select.soon"), x + CARD_W / 2, cy + 2, DIM);
			}
			if (hero == ClientHero.hero()) {
				g.fill(x, cy - 3, x + CARD_W, cy - 1, 0xFFFFFFFF);
			}
			g.outline(x - 1, cy - 1, CARD_W + 2, CARD_H + 2, hero == preview ? ACCENT : hero == hovered ? 0xFFFFFFFF : 0x60FFFFFF);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	private record Line(String key, String name) {
	}

	private static List<Line> kit(Hero hero) {
		return switch (hero) {
			case KIRIKO -> List.of(
				new Line(Component.translatable("overcraft.key.lmb").getString(), "overcraft.ability.kunai"),
				new Line(Component.translatable("overcraft.key.rmb").getString(), "overcraft.ability.healing_ofuda"),
				new Line(HeroHud.shortKey(ClientHero.ABILITY_1), "overcraft.ability.swift_step"),
				new Line(HeroHud.shortKey(ClientHero.ABILITY_2), "overcraft.ability.protection_suzu"),
				new Line(HeroHud.shortKey(ClientHero.ULTIMATE), "overcraft.ability.kitsune_rush"),
				new Line(Component.translatable("overcraft.key.passive").getString(), "overcraft.ability.wall_climb"));
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
