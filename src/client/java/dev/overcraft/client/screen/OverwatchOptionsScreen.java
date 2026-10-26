package dev.overcraft.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.client.ClientHero;
import dev.overcraft.client.HeroHud;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.controls.HeroControls.Section;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.hero.Hero;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * Hero mode's options, Overwatch style: tabs across the top (CONTROLS here; video, sound and mouse open Minecraft's own
 * screens, which hero mode shares), and the hero's key set by section, each action with the hero's own name for it
 * (PRIMARY FIRE: HEALING OFUDA). Click a key box, then press a key or mouse button; Esc cancels, Delete clears.
 */
public class OverwatchOptionsScreen extends OwScreen {
	private static final float LEFT = 130;
	private static final float TOP = 228;
	private static final float ROW_H = 35;
	private static final float WIDTH = 900;

	private final Screen parent;
	private @Nullable Action listening;

	private record Tab(String key, @Nullable Supplier<Screen> open) {
	}

	private record Row(@Nullable Section header, @Nullable Action action, float y) {
	}

	private final List<Tab> tabs;
	private final List<Row> rows = new ArrayList<>();

	public OverwatchOptionsScreen(Screen parent) {
		super(Component.translatable("overcraft.options.title"));
		this.parent = parent;
		this.tabs = List.of(
			new Tab("controls", null),
			new Tab("video", () -> new VideoSettingsScreen(this, minecraft, minecraft.options)),
			new Tab("sound", () -> new SoundOptionsScreen(this, minecraft.options)),
			new Tab("mouse", () -> new MouseSettingsScreen(this, minecraft.options)));
	}

	@Override
	protected void init() {
		rows.clear();
		float y = TOP;
		Section section = null;
		for (Action action : Action.values()) {
			if (action.section != section) {
				section = action.section;
				y += section == Action.values()[0].section ? 0 : 14;
				rows.add(new Row(section, null, y));
				y += ROW_H;
			}
			rows.add(new Row(null, action, y));
			y += ROW_H;
		}
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	// ---- input ----

	private @Nullable Action keyBoxAt(double mouseX, double mouseY) {
		float x = refX(mouseX);
		float y = refY(mouseY);
		for (Row row : rows) {
			if (row.action() != null && y >= row.y() && y < row.y() + ROW_H - 4 && x >= LEFT && x <= LEFT + WIDTH) {
				return row.action();
			}
		}
		return null;
	}

	private int tabAt(double mouseX, double mouseY) {
		float x = refX(mouseX);
		float y = refY(mouseY);
		if (y < 140 || y > 190) {
			return -1;
		}
		float tx = LEFT;
		for (int i = 0; i < tabs.size(); i++) {
			float w = tabWidth(i);
			if (x >= tx - 8 && x < tx + w + 8) {
				return i;
			}
			tx += w + 48;
		}
		return -1;
	}

	private float tabWidth(int i) {
		return HudText.width(Face.BODY, tabLabel(i), 20);
	}

	private String tabLabel(int i) {
		return Component.translatable("overcraft.options.tab." + tabs.get(i).key()).getString().toUpperCase(Locale.ROOT);
	}

	private boolean defaultsAt(double mouseX, double mouseY) {
		float x = refX(mouseX);
		float y = refY(mouseY);
		return x >= LEFT + WIDTH - 260 && x <= LEFT + WIDTH && y >= 992 && y <= 1028;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (listening != null) {
			// the click after the one on the box: that button is the new key
			bind(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			return true;
		}
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(event, doubleClick);
		}
		Action action = keyBoxAt(event.x(), event.y());
		if (action != null) {
			listening = action;
			return true;
		}
		int tab = tabAt(event.x(), event.y());
		if (tab >= 0 && tabs.get(tab).open() != null) {
			minecraft.gui.setScreen(tabs.get(tab).open().get());
			return true;
		}
		if (defaultsAt(event.x(), event.y())) {
			HeroControls.resetAll();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listening != null) {
			if (event.key() == InputConstants.KEY_ESCAPE) {
				listening = null;
			} else if (event.key() == InputConstants.KEY_DELETE || event.key() == InputConstants.KEY_BACKSPACE) {
				bind(InputConstants.UNKNOWN);
			} else {
				bind(InputConstants.getKey(event));
			}
			return true;
		}
		return super.keyPressed(event);
	}

	private void bind(InputConstants.Key key) {
		if (listening != null) {
			HeroControls.set(listening, key);
			listening = null;
		}
	}

	// ---- drawing ----

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		begin(g);
		HudText.draw(g, Face.NUMBERS, title.getString().toUpperCase(Locale.ROOT), LEFT, 110, 46, WHITE, Align.LEFT);

		// tabs: this one white and underlined in orange, the others dim
		int hoverTab = tabAt(mouseX, mouseY);
		float tx = LEFT;
		for (int i = 0; i < tabs.size(); i++) {
			float w = tabWidth(i);
			boolean current = tabs.get(i).open() == null;
			int color = current ? WHITE : i == hoverTab ? 0xE6FFFFFF : TEXT_DIM;
			HudText.draw(g, Face.BODY, tabLabel(i), tx, 176, 20, color, Align.LEFT);
			if (current) {
				fill(g, tx, 186, w, 3, ORANGE);
			}
			tx += w + 48;
		}
		fill(g, LEFT, 196, WIDTH, 1.5f, 0x40FFFFFF);

		Action hover = listening == null ? keyBoxAt(mouseX, mouseY) : null;
		Hero hero = ClientHero.hero();
		for (Row row : rows) {
			if (row.header() != null) {
				String name = Component.translatable("overcraft.options.section." + row.header().name().toLowerCase(Locale.ROOT))
					.getString().toUpperCase(Locale.ROOT);
				HudText.draw(g, Face.BODY, name, LEFT, row.y() + 25, 15, ORANGE, Align.LEFT);
				continue;
			}
			Action action = row.action();
			boolean live = action == listening;
			fill(g, LEFT, row.y(), WIDTH, ROW_H - 4, live || action == hover ? ROW_HOVER : ROW);
			String name = Component.translatable("overcraft.controls." + action.id()).getString().toUpperCase(Locale.ROOT);
			HudText.draw(g, Face.BODY, name, LEFT + 18, row.y() + 21, 15, TEXT, Align.LEFT);
			String what = heroAbility(hero, action);
			if (what != null) {
				float after = LEFT + 18 + HudText.width(Face.BODY, name, 15) + 16;
				HudText.draw(g, Face.BODY, what, after, row.y() + 21, 13, TEXT_DIM, Align.LEFT);
			}
			keyCell(g, action, row.y(), live);
		}

		// restore defaults, bottom right; Esc back, bottom left
		boolean overDefaults = listening == null && defaultsAt(mouseX, mouseY);
		fill(g, LEFT + WIDTH - 260, 992, 260, 36, overDefaults ? 0x40FFFFFF : 0x26FFFFFF);
		outline(g, LEFT + WIDTH - 260, 992, 260, 36, 1.5f, 0x66FFFFFF);
		HudText.draw(g, Face.BODY, Component.translatable("overcraft.options.defaults").getString().toUpperCase(Locale.ROOT),
			LEFT + WIDTH - 130, 1016, 14, WHITE, Align.CENTER);
		keyBox(g, "ESC", LEFT + 8, 1010, WHITE);
		HudText.draw(g, Face.BODY, Component.translatable(listening != null ? "overcraft.options.cancel" : "overcraft.options.back")
			.getString().toUpperCase(Locale.ROOT), LEFT + 40, 1017, 14, TEXT, Align.LEFT);
		if (listening != null) {
			HudText.draw(g, Face.BODY, Component.translatable("overcraft.options.press_key_hint").getString().toUpperCase(Locale.ROOT),
				LEFT + 150, 1017, 14, TEXT_DIM, Align.LEFT);
		}
		g.pose().popMatrix();
	}

	/** The action's key, in a box on the right: orange while waiting for a key, red-tinted when shared with another. */
	private void keyCell(GuiGraphicsExtractor g, Action action, float y, boolean live) {
		float w = 220;
		float x = LEFT + WIDTH - w - 4;
		float h = ROW_H - 10;
		fill(g, x, y + 2, w, h, KEY_BOX);
		String label;
		int color;
		if (live) {
			outline(g, x, y + 2, w, h, 2, ORANGE);
			label = Component.translatable("overcraft.options.press_key").getString().toUpperCase(Locale.ROOT);
			color = ORANGE;
		} else {
			boolean unbound = action.key().equals(InputConstants.UNKNOWN);
			label = unbound ? "-" : HeroHud.shortKey(action.mapping);
			color = HeroControls.conflicts(action) ? 0xFFFF6B6B : WHITE;
		}
		HudText.draw(g, Face.BODY, label, x + w / 2, y + 2 + h / 2 + 7, 14, color, Align.CENTER);
	}

	/** What the action does for this hero ("HEALING OFUDA" for Kiriko's primary fire), or null. */
	private static @Nullable String heroAbility(@Nullable Hero hero, Action action) {
		if (hero != Hero.KIRIKO) {
			return null;
		}
		String ability = switch (action) {
			case PRIMARY_FIRE -> "healing_ofuda";
			case SECONDARY_FIRE -> "kunai";
			case ABILITY_1 -> "swift_step";
			case ABILITY_2 -> "protection_suzu";
			case ULTIMATE -> "kitsune_rush";
			case JUMP -> "wall_climb";
			default -> null;
		};
		return ability == null ? null
			: Component.translatable("overcraft.ability." + ability).getString().toUpperCase(Locale.ROOT);
	}
}
