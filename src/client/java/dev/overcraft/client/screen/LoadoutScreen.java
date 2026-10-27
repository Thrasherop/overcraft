package dev.overcraft.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.Overcraft;
import dev.overcraft.client.ClientHero;
import dev.overcraft.client.comms.Loadout;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.hero.Hero;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

import java.util.List;
import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * The Overwatch menu's VOICE LINES: which four of the hero's voice lines and which four emotes are on the communication
 * wheel's pages (the owner: "a settings menu to choose the ones to equip"), on two tabs. The four slots across the top
 * (up, right, down, left, as the wheel lays them out); every line or emote below, in columns. Click a slot, then an
 * entry to put it there; right-click a voice line to hear it. Saved per hero (Loadout).
 */
public class LoadoutScreen extends OwScreen {
	private static final float LEFT = 130;
	private static final float SLOTS_TOP = 280;
	private static final float SLOT_W = 380;
	private static final float SLOT_H = 54;
	private static final float LIST_TOP = 420;
	private static final float ROW_H = 40;
	private static final float COLUMN_W = 420;
	private static final int ROWS = 13;
	private static final String[] SLOT_NAMES = {"overcraft.loadout.up", "overcraft.loadout.right", "overcraft.loadout.down",
		"overcraft.loadout.left"};

	private static final float TABS_TOP = 250;

	private final Screen parent;
	private int slot;
	private boolean emotes;

	public LoadoutScreen(Screen parent) {
		super(Component.translatable("overcraft.loadout.title"));
		this.parent = parent;
	}

	private float slotX(int i) {
		return LEFT + i * (SLOT_W + 20);
	}

	private float lineX(int i) {
		return LEFT + (i / ROWS) * COLUMN_W;
	}

	private float lineY(int i) {
		return LIST_TOP + (i % ROWS) * ROW_H;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		Hero hero = ClientHero.hero();
		if (hero == null) {
			return super.mouseClicked(event, doubleClick);
		}
		float x = refX(event.x());
		float y = refY(event.y());
		for (int t = 0; t < 2; t++) {
			if (x >= tabX(t) && x < tabX(t) + 200 && y >= TABS_TOP - 30 && y < TABS_TOP + 6) {
				emotes = t == 1;
				return true;
			}
		}
		for (int i = 0; i < Loadout.SLOTS; i++) {
			if (x >= slotX(i) && x < slotX(i) + SLOT_W && y >= SLOTS_TOP && y < SLOTS_TOP + SLOT_H) {
				slot = i;
				return true;
			}
		}
		List<String> lines = entries(hero);
		for (int i = 0; i < lines.size(); i++) {
			if (x >= lineX(i) && x < lineX(i) + COLUMN_W - 20 && y >= lineY(i) && y < lineY(i) + ROW_H - 4) {
				if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
					if (!emotes) {
						minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
							SoundEvent.createVariableRangeEvent(Overcraft.id(hero.id + ".voiceline." + lines.get(i))), 1));
					}
				} else {
					if (emotes) {
						Loadout.setEmote(hero, slot, lines.get(i));
					} else {
						Loadout.setVoiceLine(hero, slot, lines.get(i));
					}
					slot = (slot + 1) % Loadout.SLOTS;
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private float tabX(int tab) {
		return LEFT + 420 + tab * 220;
	}

	private List<String> entries(Hero hero) {
		return emotes ? Loadout.availableEmotes(hero) : Loadout.available(hero);
	}

	private String name(Hero hero, String id) {
		return emotes ? Loadout.emoteName(id) : name(hero, id, true);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		begin(g);
		HudText.draw(g, Face.BODY, Component.translatable("overcraft.menu.title").getString().toUpperCase(Locale.ROOT),
			LEFT, 150, 15, ORANGE, Align.LEFT);
		HudText.draw(g, Face.NUMBERS, getTitle().getString().toUpperCase(Locale.ROOT), LEFT, 230, 64, WHITE, Align.LEFT);
		for (int t = 0; t < 2; t++) {
			boolean on = (t == 1) == emotes;
			String tab = Component.translatable(t == 0 ? "overcraft.wheel.voice_lines" : "overcraft.wheel.emotes").getString()
				.toUpperCase(Locale.ROOT);
			HudText.draw(g, Face.BODY, tab, tabX(t), TABS_TOP, 18, on ? WHITE : TEXT_DIM, Align.LEFT);
			if (on) {
				fill(g, tabX(t), TABS_TOP + 6, HudText.width(Face.BODY, tab, 18), 3, ORANGE);
			}
		}
		Hero hero = ClientHero.hero();
		if (hero != null) {
			List<String> equipped = emotes ? Loadout.emotes(hero) : Loadout.voiceLines(hero);
			for (int i = 0; i < Loadout.SLOTS; i++) {
				float x = slotX(i);
				boolean chosen = i == slot;
				fill(g, x, SLOTS_TOP, SLOT_W, SLOT_H, chosen ? 0xE6283846 : 0xCC10161C);
				outline(g, x, SLOTS_TOP, SLOT_W, SLOT_H, 1.5f, chosen ? ORANGE : 0x99C8D0D8);
				HudText.draw(g, Face.SMALL, Component.translatable(SLOT_NAMES[i]).getString().toUpperCase(Locale.ROOT),
					x + 12, SLOTS_TOP + 17, 10, chosen ? ORANGE : TEXT_DIM, Align.LEFT);
				HudText.draw(g, Face.BODY, name(hero, equipped.get(i)), x + 12, SLOTS_TOP + 42, 15, WHITE, Align.LEFT);
			}
			List<String> lines = entries(hero);
			float rx = refX(mouseX);
			float ry = refY(mouseY);
			for (int i = 0; i < lines.size(); i++) {
				float x = lineX(i);
				float y = lineY(i);
				boolean over = rx >= x && rx < x + COLUMN_W - 20 && ry >= y && ry < y + ROW_H - 4;
				boolean on = equipped.contains(lines.get(i));
				fill(g, x, y, COLUMN_W - 20, ROW_H - 4, over ? ROW_HOVER : ROW);
				HudText.draw(g, Face.BODY, name(hero, lines.get(i)), x + 12, y + 25, 13, on ? ORANGE : WHITE, Align.LEFT);
			}
			if (lines.isEmpty()) {
				HudText.draw(g, Face.BODY, Component.translatable(emotes ? "overcraft.loadout.none_emotes" : "overcraft.loadout.none")
					.getString(), LEFT, LIST_TOP + 20, 14, TEXT_DIM, Align.LEFT);
			}
		}
		keyBox(g, "ESC", LEFT + 8, 1010, WHITE);
		// emotes can't be heard: no right click on their tab
		HudText.draw(g, Face.BODY, Component.translatable(emotes ? "overcraft.loadout.hint_emotes" : "overcraft.loadout.hint").getString()
			.toUpperCase(Locale.ROOT), LEFT + 40, 1017, 14, TEXT, Align.LEFT);
		g.pose().popMatrix();
	}

	private static String name(Hero hero, String id, boolean voiceLine) {
		return id.isEmpty() ? "-" : Component.translatable("overcraft.voiceline." + hero.id + "." + id).getString().toUpperCase(Locale.ROOT);
	}
}
