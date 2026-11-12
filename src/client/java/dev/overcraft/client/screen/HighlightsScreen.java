package dev.overcraft.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.client.HeroArt;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.hero.Hero;
import dev.overcraft.net.HighlightsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.gradientRight;
import static dev.overcraft.client.hud.HudDraw.image;

/**
 * The Highlights menu (owner, 2026-11-11): the player's own top 5 plays in this world (Highlights, saved with it), each
 * watched alone on a click, opening with the hero's intro as the POTG does (PotgReplay.watchHighlight). Only after the
 * world's dragon has been killed: before that the server answers "locked" and this says so. Opened by the Highlights key
 * (K) or from hero mode's Esc menu. Laid out like the Esc menu (ours).
 */
public class HighlightsScreen extends OwScreen {
	private static final float ITEM_X = 130;
	private static final float ITEM_TOP = 300;
	private static final float ITEM_STEP = 110;

	private record Row(Hero hero, String when, String where) {
	}

	private final boolean locked;
	private final List<Row> rows;
	private int selected;

	private HighlightsScreen(boolean locked, List<Row> rows) {
		super(Component.translatable("overcraft.highlights.title"));
		this.locked = locked;
		this.rows = rows;
	}

	/** The server's answer to the key: the menu opens with it. */
	public static void received(String json) {
		boolean locked = false;
		List<Row> rows = new ArrayList<>();
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			locked = o.has("locked") && o.get("locked").getAsBoolean();
			JsonArray list = o.has("highlights") ? o.getAsJsonArray("highlights") : new JsonArray();
			SimpleDateFormat format = new SimpleDateFormat("d MMM, HH:mm", Locale.ROOT);
			for (JsonElement e : list) {
				JsonObject h = e.getAsJsonObject();
				Hero hero = Hero.byId(h.get("hero").getAsString());
				if (hero == null) {
					hero = Hero.KIRIKO;
				}
				String dim = h.has("dimension") ? h.get("dimension").getAsString() : "";
				String where = dim.endsWith("the_end") ? "THE END" : dim.endsWith("the_nether") ? "THE NETHER" : "OVERWORLD";
				rows.add(new Row(hero, format.format(new Date(h.get("epochMs").getAsLong())).toUpperCase(Locale.ROOT), where));
			}
		} catch (Exception e) {
			locked = true;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.screen() == null || mc.gui.screen() instanceof OverwatchMenuScreen) {
			mc.gui.setScreen(new HighlightsScreen(locked, rows));
		}
	}

	private int rowAt(double mouseX, double mouseY) {
		float x = refX(mouseX);
		float y = refY(mouseY);
		for (int i = 0; i < rows.size(); i++) {
			float top = ITEM_TOP + i * ITEM_STEP;
			if (x >= ITEM_X - 30 && x <= ITEM_X + 760 && y >= top && y < top + ITEM_STEP - 14) {
				return i;
			}
		}
		return -1;
	}

	private void watch(int i) {
		if (i >= 0 && i < rows.size()) {
			onClose();
			ClientPlayNetworking.send(new HighlightsPayload(i, ""));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int i = rowAt(event.x(), event.y());
		if (i >= 0 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			watch(i);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (!rows.isEmpty() && (key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN)) {
			selected = Math.floorMod(selected + (key == InputConstants.KEY_UP ? -1 : 1), rows.size());
			return true;
		}
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_SPACE) {
			watch(selected);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		int under = rowAt(mouseX, mouseY);
		if (under >= 0) {
			selected = under;
		}
		begin(g);
		HudText.draw(g, Face.NUMBERS, Component.translatable("overcraft.highlights.title").getString().toUpperCase(Locale.ROOT),
			ITEM_X, 200, 64, WHITE, Align.LEFT);
		if (locked || rows.isEmpty()) {
			String why = Component.translatable(locked ? "overcraft.highlights.locked" : "overcraft.highlights.none").getString()
				.toUpperCase(Locale.ROOT);
			HudText.draw(g, Face.BODY, why, ITEM_X, ITEM_TOP + 20, 18, TEXT, Align.LEFT);
		}
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			float top = ITEM_TOP + i * ITEM_STEP;
			float rowH = ITEM_STEP - 14;
			boolean active = i == selected;
			fill(g, ITEM_X - 30, top, 760, rowH, active ? ROW_HOVER : ROW);
			if (active) {
				gradientRight(g, ITEM_X - 30, top, 560, rowH, 0x30FFFFFF, 0x00FFFFFF);
				fill(g, ITEM_X - 30, top, 6, rowH, ORANGE);
			}
			Identifier portrait = HeroArt.portrait(row.hero());
			if (portrait != null) {
				image(g, portrait, ITEM_X - 10, top + 8, rowH - 16, rowH - 16, WHITE);
			}
			float tx = ITEM_X + rowH + 6;
			HudText.draw(g, Face.NUMBERS, (i + 1) + ".  " + row.hero().displayName.toUpperCase(Locale.ROOT), tx, top + 46, 36,
				active ? WHITE : TEXT, Align.LEFT);
			HudText.draw(g, Face.BODY, row.when() + "   " + row.where(), tx, top + 78, 15, TEXT_DIM, Align.LEFT);
		}
		keyBox(g, "ESC", ITEM_X + 8, 1010, WHITE);
		HudText.draw(g, Face.BODY, Component.translatable("overcraft.highlights.back").getString().toUpperCase(Locale.ROOT),
			ITEM_X + 40, 1017, 14, TEXT, Align.LEFT);
		g.pose().popMatrix();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
