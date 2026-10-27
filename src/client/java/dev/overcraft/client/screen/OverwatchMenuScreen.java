package dev.overcraft.client.screen;

import dev.overcraft.client.assets.AssetPackVersion;
import dev.overcraft.Overcraft;
import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.client.ClientHero;
import dev.overcraft.client.HeroArt;
import dev.overcraft.client.HeroSelectScreen;
import dev.overcraft.client.hud.HudShapes;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.hero.Hero;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.gradientRight;
import static dev.overcraft.client.hud.HudDraw.image;

/**
 * Esc in hero mode: Overwatch's menu, not Minecraft's (owner, 2026-10-26: "esc in minecraft mode opens minecraft
 * settings. Esc in hero mode opens overwatch settings"). Resume, the hero's options (controls), change hero, change
 * team (TeamScreen), Minecraft's own menu (for everything else it has), and leaving. Big italic items down the left, as in Overwatch's menus.
 */
public class OverwatchMenuScreen extends OwScreen {
	private static final float ITEM_X = 130;
	private static final float ITEM_TOP = 430;
	private static final float ITEM_STEP = 74;
	private static final float ITEM_CAP = 42;

	private record Item(String key, Runnable action, boolean enabled, @Nullable Component note) {
	}

	private List<Item> items = List.of();
	private int hovered = -1;
	private int selected = 0;

	public OverwatchMenuScreen() {
		super(Component.translatable("overcraft.menu.title"));
	}

	@Override
	protected void init() {
		boolean canChange = ClientHero.hero() == null || ClientHero.sync().canChangeHero();
		items = List.of(
			new Item("resume", this::onClose, true, null),
			new Item("options", () -> minecraft.gui.setScreen(new OverwatchOptionsScreen(this)), true, null),
			new Item("change_hero", () -> minecraft.gui.setScreen(new HeroSelectScreen(false)), canChange,
				canChange ? null : Component.translatable("overcraft.change_hero.too_far")),
			new Item("change_team", () -> minecraft.gui.setScreen(new TeamScreen(this)), true, null),
			new Item("minecraft_menu", () -> minecraft.gui.setScreen(new PauseScreen(true)), true, null),
			new Item(minecraft.isLocalServer() ? "save_and_leave" : "leave", this::leave, true, null));
	}

	private void leave() {
		minecraft.getReportingContext().draftReportHandled(minecraft, this,
			() -> minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
	}

	private int itemAt(double mouseX, double mouseY) {
		float x = refX(mouseX);
		float y = refY(mouseY);
		for (int i = 0; i < items.size(); i++) {
			float top = ITEM_TOP + i * ITEM_STEP - ITEM_CAP - 12;
			if (x >= ITEM_X - 30 && x <= ITEM_X + 620 && y >= top && y < top + ITEM_STEP) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int i = itemAt(event.x(), event.y());
		if (i >= 0 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			choose(i);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN) {
			selected = Math.floorMod(selected + (key == InputConstants.KEY_UP ? -1 : 1), items.size());
			hovered = -1;
			return true;
		}
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_SPACE) {
			choose(selected);
			return true;
		}
		return super.keyPressed(event);
	}

	private void choose(int i) {
		Item item = items.get(i);
		if (item.enabled()) {
			item.action().run();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		int under = itemAt(mouseX, mouseY);
		if (under != hovered) {
			hovered = under;
			if (under >= 0) {
				selected = under;
			}
		}
		begin(g);
		header(g);
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			float baseline = ITEM_TOP + i * ITEM_STEP;
			boolean active = i == selected;
			if (active) {
				// a light band fading right, and Overwatch's orange tab at its left end
				gradientRight(g, ITEM_X - 30, baseline - ITEM_CAP - 12, 560, ITEM_CAP + 24, 0x40FFFFFF, 0x00FFFFFF);
				fill(g, ITEM_X - 30, baseline - ITEM_CAP - 12, 6, ITEM_CAP + 24, ORANGE);
			}
			String label = Component.translatable("overcraft.menu." + item.key()).getString().toUpperCase(Locale.ROOT);
			int color = !item.enabled() ? 0x66FFFFFF : active ? WHITE : TEXT;
			HudText.draw(g, Face.NUMBERS, label, ITEM_X, baseline, ITEM_CAP, color, Align.LEFT);
			if (item.note() != null && active) {
				float after = ITEM_X + HudText.width(Face.NUMBERS, label, ITEM_CAP) + 24;
				HudText.draw(g, Face.BODY, item.note().getString().toUpperCase(Locale.ROOT), after, baseline - 4, 14,
					TEXT_DIM, Align.LEFT);
			}
		}
		// how to get back, bottom left
		keyBox(g, "ESC", ITEM_X + 8, 1010, WHITE);
		HudText.draw(g, Face.BODY, Component.translatable("overcraft.menu.resume").getString().toUpperCase(Locale.ROOT),
			ITEM_X + 40, 1017, 14, TEXT, Align.LEFT);
		g.pose().popMatrix();
	}

	/** The hero at the top: portrait, name in Overwatch's big italic type, role under it. */
	private void header(GuiGraphicsExtractor g) {
		Hero hero = ClientHero.hero();
		float x = ITEM_X;
		float y = 150;
		String title = Component.translatable("overcraft.menu.title").getString().toUpperCase(Locale.ROOT);
		HudText.draw(g, Face.BODY, title, x, y, 15, ORANGE, Align.LEFT);
		// which build this is, and the asset pack's (#21): red when the pack is older than the build needs
		String pack = AssetPackVersion.get();
		String build = Overcraft.version() + (AssetPackVersion.installed() ? "  ·  ASSETS " + (pack == null ? "OLD" : pack) : "");
		HudText.draw(g, Face.BODY, build, x + HudText.width(Face.BODY, title, 15) + 12, y, 12,
			AssetPackVersion.outdated() ? 0xFFFF6B6B : 0xFFB8C2CC, Align.LEFT);
		if (hero == null) {
			return;
		}
		Identifier portrait = HudShapes.portrait(hero);
		float pw = 110;
		float ph = pw * HudShapes.SHIELD_H / HudShapes.SHIELD_W;
		float textX = x;
		if (portrait != null) {
			image(g, HudShapes.SHIELD, x, y + 32, pw, ph, 0xFF1B2A4A);
			image(g, portrait, x, y + 32, pw, ph, WHITE);
			image(g, HudShapes.SHIELD_BORDER, x, y + 32, pw, ph, 0xFF68CCFF);
			// the hair breaking out over the frame's top, as on the HUD
			Identifier pop = HudShapes.portraitPop(hero);
			if (pop != null) {
				float texel = pw / (HudShapes.SHIELD_W * 2f);
				image(g, pop, x, y + 32 - HudShapes.POP_H * texel, pw, (HudShapes.POP_H + HudShapes.POP_OVERLAP) * texel, WHITE);
			}
			textX = x + pw + 28;
		}
		HudText.draw(g, Face.NUMBERS, hero.displayName.toUpperCase(Locale.ROOT), textX, y + 100, 64, WHITE, Align.LEFT);
		Identifier role = HeroArt.role(hero.role);
		float roleX = textX;
		if (role != null) {
			image(g, role, textX, y + 116, 26, 26, WHITE);
			roleX += 34;
		}
		String roleName = Component.translatable("overcraft.role." + hero.role.name().toLowerCase(Locale.ROOT)).getString()
			.toUpperCase(Locale.ROOT);
		HudText.draw(g, Face.BODY, roleName, roleX, y + 137, 16, TEXT, Align.LEFT);
		// the sub-role after it in orange, as on the hero select screen
		String sub = Component.translatable("overcraft.subrole." + hero.subRole).getString().toUpperCase(Locale.ROOT);
		HudText.draw(g, Face.BODY, sub, roleX + HudText.width(Face.BODY, roleName, 16) + 12, y + 137, 16, ORANGE, Align.LEFT);
	}
}
