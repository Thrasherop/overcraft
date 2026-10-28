package dev.overcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.hud.HudDraw;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.client.model.HeroModelRenderer;
import dev.overcraft.client.model.HeroModels;
import dev.overcraft.client.screen.OwScreen;
import dev.overcraft.hero.Hero;
import dev.overcraft.net.SelectHeroPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.gradient;
import static dev.overcraft.client.hud.HudDraw.image;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * "Select your hero", on joining a world and with H near your bed, laid out like Overwatch's 2026 select screen
 * (tools/reference/kiriko-reference.md, section 6): the hero itself large in the middle (the real model, when the
 * local pack has it), the name huge at the top right with role and sub-role, the roster in role groups along the bottom
 * with the pick framed in orange, an orange READY button, key hints at the bottom. Overwatch keeps the abilities behind
 * F1 (hero details); here a row of their icons and keys shows, and F1 adds their descriptions.
 *
 * <p>Click a portrait to preview, READY (or Enter, or the portrait again) to play it; Esc keeps the current hero (or
 * plain Minecraft).
 */
public class HeroSelectScreen extends OwScreen {
	/** How far (blocks) the previewed model sits below the middle of its frame: puts her head near the top. */
	private static final float MODEL_DROP = 0.18f;
	private static final float PORTRAIT = 72;
	private static final float PORTRAIT_GAP = 8;
	private static final float GROUP_GAP = 48;
	private static final float ROSTER_TOP = 830;
	private static final float READY_W = 300;
	private static final float READY_H = 58;
	private static final float READY_Y = 978;
	private static final float ICON = 56;

	private final boolean onJoin;
	private Hero preview;
	private boolean details;

	private record Card(Hero hero, float x, float y) {
	}

	public HeroSelectScreen(boolean onJoin) {
		super(Component.translatable("overcraft.select.title"));
		this.onJoin = onJoin;
		Hero current = ClientHero.hero();
		this.preview = current != null ? current : Hero.KIRIKO;
		HeroModelRenderer.restartShowcase();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void confirmPick() {
		if (!preview.playable) {
			return;
		}
		ClientPlayNetworking.send(new SelectHeroPayload(preview.id));
		ClientHero.pickedHero();
		onClose();
	}

	// ---- layout ----

	/** The roster's portraits, grouped by role (only roles with heroes), centred along the bottom. */
	private List<Card> cards() {
		List<List<Hero>> groups = new ArrayList<>();
		for (Hero.Role role : Hero.Role.values()) {
			List<Hero> group = new ArrayList<>();
			for (Hero hero : Hero.values()) {
				if (hero.role == role) {
					group.add(hero);
				}
			}
			if (!group.isEmpty()) {
				groups.add(group);
			}
		}
		float total = -GROUP_GAP;
		for (List<Hero> group : groups) {
			total += GROUP_GAP + group.size() * (PORTRAIT + PORTRAIT_GAP) - PORTRAIT_GAP;
		}
		float x = (refWidth() - total) / 2;
		List<Card> cards = new ArrayList<>();
		for (List<Hero> group : groups) {
			for (Hero hero : group) {
				cards.add(new Card(hero, x, ROSTER_TOP));
				x += PORTRAIT + PORTRAIT_GAP;
			}
			x += GROUP_GAP - PORTRAIT_GAP;
		}
		return cards;
	}

	private @Nullable Hero cardAt(double mouseX, double mouseY) {
		float x = refX(mouseX);
		float y = refY(mouseY);
		for (Card card : cards()) {
			if (x >= card.x() && x < card.x() + PORTRAIT && y >= card.y() && y < card.y() + PORTRAIT) {
				return card.hero();
			}
		}
		return null;
	}

	private boolean overReady(double mouseX, double mouseY) {
		float x = refX(mouseX);
		float y = refY(mouseY);
		float left = refWidth() / 2 - READY_W / 2;
		return x >= left && x <= left + READY_W && y >= READY_Y - READY_H / 2 && y <= READY_Y + READY_H / 2;
	}

	// ---- input ----

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			Hero clicked = cardAt(event.x(), event.y());
			if (clicked != null) {
				boolean again = clicked == preview;
				if (preview != clicked) {
					HeroModelRenderer.restartShowcase();
				}
				preview = clicked;
				if (again || doubleClick) {
					confirmPick();
				}
				return true;
			}
			if (overReady(event.x(), event.y())) {
				confirmPick();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_SPACE) {
			confirmPick();
			return true;
		}
		if (key == InputConstants.KEY_F1) {
			details = !details;
			return true;
		}
		if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT) {
			List<Card> cards = cards();
			int at = 0;
			for (int i = 0; i < cards.size(); i++) {
				if (cards.get(i).hero() == preview) {
					at = i;
				}
			}
			preview = cards.get(Math.floorMod(at + (key == InputConstants.KEY_LEFT ? -1 : 1), cards.size())).hero();
			HeroModelRenderer.restartShowcase();
			return true;
		}
		return super.keyPressed(event);
	}

	// ---- drawing ----

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		if (minecraft.level != null) {
			extractBlurredBackground(g);
		}
		g.fill(0, 0, width, height, 0x73060A10);
		begin(g);
		// darker along the bottom, under the roster
		gradient(g, 0, 640, refWidth(), HudDraw.REF_H - 640, 0x00000000, 0xD9000000);
		g.pose().popMatrix();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractRenderState(g, mouseX, mouseY, a);
		hero(g, mouseX, mouseY);
		begin(g);
		// her legs run on behind the roster, fading into its dark band (no hard edge across the coat)
		gradient(g, 0, 690, refWidth(), 190, 0x00060A10, 0xE6060A10);
		fill(g, 0, 880, refWidth(), HudDraw.REF_H - 880, 0xE6060A10);
		float w = refWidth();
		header(g);
		nameplate(g, w);
		kit(g, w);
		roster(g, mouseX, mouseY);
		ready(g, w, mouseX, mouseY);
		hints(g, w);
		g.pose().popMatrix();
	}

	/**
	 * The previewed hero, large in the middle: the player drawn as that hero (their real model), or the hero's card
	 * art when there's no model for it.
	 */
	private void hero(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		float s = scale();
		if (HeroModels.get(preview) != null && minecraft.player != null) {
			// framed as Overwatch frames its heroes: large, from mid-thigh up, the head near the top and the legs cut
			// off above the roster (the critique: a full body at 55% of the height looked small)
			int top = (int) (40 * s);
			int bottom = height;
			int size = (int) (860 * s);
			int cx = width / 2;
			HeroModelRenderer.preview(preview);
			try {
				// looking ahead into the distance, as Overwatch's hero select (not following the mouse: #29). Minecraft's
				// preview turns toward a point: the box's own centre is straight ahead
				InventoryScreen.extractEntityInInventoryFollowsMouse(g, cx - size, top, cx + size, bottom, size, MODEL_DROP,
					cx, (top + bottom) / 2f, minecraft.player);
			} finally {
				HeroModelRenderer.preview(null);
			}
			return;
		}
		Identifier art = HeroArt.card(preview);
		Identifier bust = art == null ? HeroArt.bust(preview) : null;
		if (bust != null) {
			// no card art extracted for this hero (Sombra, Illari): the head-and-shoulders bust instead
			begin(g);
			float h = 560;
			float w = h * 200 / 280;
			image(g, bust, refWidth() / 2 - w / 2, 180, w, h, preview.playable ? 0xFFFFFFFF : 0xFFA4A9B3);
			g.pose().popMatrix();
		} else if (art != null) {
			begin(g);
			float h = 740;
			float w = h * 368 / 500;
			float left = refWidth() / 2 - w / 2;
			// the card art stops at the knees: let its lower part melt away rather than end in a line
			HudDraw.imageFadingDown(g, art, left, 90, w, h, 368, 500, 0.3f, preview.playable ? 0xFFFFFF : 0x9A9FA9);
			g.pose().popMatrix();
		}
	}

	/** Top left: the screen's title, as Overwatch shows the mode, and why it's showing. */
	private void header(GuiGraphicsExtractor g) {
		HudText.draw(g, Face.NUMBERS, Component.translatable("overcraft.select.mode").getString().toUpperCase(Locale.ROOT),
			90, 120, 54, 0xB3FFFFFF, Align.LEFT);
		HudText.draw(g, Face.BODY, Component.translatable(onJoin ? "overcraft.select.subtitle_join" : "overcraft.select.subtitle_bed")
			.getString().toUpperCase(Locale.ROOT), 92, 152, 15, TEXT, Align.LEFT);
	}

	/** Top right: the name huge, then role and sub-role, then health. */
	private void nameplate(GuiGraphicsExtractor g, float w) {
		float right = w - 90;
		HudText.draw(g, Face.NUMBERS, preview.displayName.toUpperCase(Locale.ROOT), right, 175, 100, WHITE, Align.RIGHT);
		String sub = Component.translatable("overcraft.subrole." + preview.subRole).getString().toUpperCase(Locale.ROOT);
		String role = Component.translatable("overcraft.role." + preview.role.name().toLowerCase(Locale.ROOT)).getString()
			.toUpperCase(Locale.ROOT);
		float subW = HudText.width(Face.BODY, sub, 20);
		HudText.draw(g, Face.BODY, sub, right, 215, 20, ORANGE, Align.RIGHT);
		Identifier badge = HeroArt.role(preview.role);
		float roleRight = right - subW - 18;
		HudText.draw(g, Face.BODY, role, roleRight, 215, 20, TEXT, Align.RIGHT);
		if (badge != null) {
			float roleW = HudText.width(Face.BODY, role, 20);
			image(g, badge, roleRight - roleW - 34, 191, 26, 26, WHITE);
		}
		HudText.draw(g, Face.BODY, Component.translatable("overcraft.select.health", preview.owHealth).getString().toUpperCase(Locale.ROOT),
			right, 245, 14, TEXT_DIM, Align.RIGHT);
	}

	/**
	 * Right, under the name: the kit's icons with their keys; with F1, each one's name and description too (Overwatch's
	 * hero details).
	 */
	private void kit(GuiGraphicsExtractor g, float w) {
		if (!preview.playable) {
			HudText.draw(g, Face.BODY, Component.translatable("overcraft.select.coming_soon_long").getString().toUpperCase(Locale.ROOT),
				w - 90, 290, 15, TEXT_DIM, Align.RIGHT);
			return;
		}
		List<Line> lines = kit(preview);
		if (details) {
			float panelW = 560;
			float x = w - 90 - panelW;
			float y = 280;
			fill(g, x - 20, y - 16, panelW + 40, lines.size() * 74 + 24, 0xB30B1118);
			for (Line line : lines) {
				Identifier icon = HeroArt.ability(preview, line.ability());
				if (icon != null) {
					fill(g, x, y, ICON, ICON, 0xE6F2F2F2);
					image(g, icon, x + 6, y + 6, ICON - 12, ICON - 12, 0xFF111111);
				}
				String name = Component.translatable("overcraft.ability." + line.ability()).getString().toUpperCase(Locale.ROOT);
				HudText.draw(g, Face.BODY, name, x + ICON + 16, y + 20, 17, WHITE, Align.LEFT);
				float nameW = HudText.width(Face.BODY, name, 17);
				keyBox(g, line.key(), x + ICON + 16 + nameW + 14 + Math.max(30, HudText.width(Face.SMALL, line.key(), 13) + 16) / 2, y + 14, WHITE);
				drawWrapped(g, Component.translatable("overcraft.ability." + line.ability() + ".desc").getString(), x + ICON + 16,
					y + 44, panelW - ICON - 16, 13);
				y += 74;
			}
			return;
		}
		float x = w - 90 - lines.size() * (ICON + 12) + 12;
		for (Line line : lines) {
			Identifier icon = HeroArt.ability(preview, line.ability());
			fill(g, x, 280, ICON, ICON, 0xD9F2F2F2);
			if (icon != null) {
				image(g, icon, x + 6, 286, ICON - 12, ICON - 12, 0xFF111111);
			}
			keyBox(g, line.key(), x + ICON / 2, 280 + ICON + 20, WHITE);
			x += ICON + 12;
		}
	}

	/** Text wrapped to a width, line by line (Overwatch's details are short paragraphs). */
	private static void drawWrapped(GuiGraphicsExtractor g, String text, float x, float y, float width, float cap) {
		StringBuilder line = new StringBuilder();
		float lineY = y;
		for (String word : text.split(" ")) {
			String tryLine = line.isEmpty() ? word : line + " " + word;
			if (HudText.width(Face.BODY, tryLine, cap) > width && !line.isEmpty()) {
				HudText.draw(g, Face.BODY, line.toString(), x, lineY, cap, TEXT_DIM, Align.LEFT);
				lineY += cap * 1.6f;
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(tryLine);
			}
		}
		if (!line.isEmpty()) {
			HudText.draw(g, Face.BODY, line.toString(), x, lineY, cap, TEXT_DIM, Align.LEFT);
		}
	}

	/** The roster: role headers with a rule, portraits, the pick framed in orange with its name under it. */
	private void roster(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		List<Card> cards = cards();
		Hero hovered = cardAt(mouseX, mouseY);
		Hero.Role lastRole = null;
		for (int i = 0; i < cards.size(); i++) {
			Card card = cards.get(i);
			Hero hero = card.hero();
			if (hero.role != lastRole) {
				lastRole = hero.role;
				float groupEnd = card.x();
				for (int j = i; j < cards.size() && cards.get(j).hero().role == lastRole; j++) {
					groupEnd = cards.get(j).x() + PORTRAIT;
				}
				String role = Component.translatable("overcraft.role." + lastRole.name().toLowerCase(Locale.ROOT)).getString()
					.toUpperCase(Locale.ROOT);
				Identifier badge = HeroArt.role(lastRole);
				float textX = card.x();
				if (badge != null) {
					image(g, badge, card.x(), ROSTER_TOP - 42, 24, 24, WHITE);
					textX += 32;
				}
				HudText.draw(g, Face.BODY, role, textX, ROSTER_TOP - 23, 16, TEXT, Align.LEFT);
				fill(g, card.x(), ROSTER_TOP - 12, groupEnd - card.x(), 2, 0x66FFFFFF);
			}
			boolean picked = hero == preview;
			float x = card.x();
			float y = card.y() - (picked ? 6 : hero == hovered ? 2 : 0);
			if (picked) {
				// the orange glow round the pick
				outline(g, x - 6, y - 6, PORTRAIT + 12, PORTRAIT + 12, 3, 0x66F99E1A);
				outline(g, x - 3, y - 3, PORTRAIT + 6, PORTRAIT + 6, 3, ORANGE);
			}
			fill(g, x, y, PORTRAIT, PORTRAIT, 0xCC1B2333);
			Identifier portrait = HeroArt.portrait(hero);
			if (portrait != null) {
				image(g, portrait, x, y, PORTRAIT, PORTRAIT, hero.playable ? WHITE : 0xFF6A6F79);
			} else {
				HudText.draw(g, Face.NUMBERS, hero.displayName.substring(0, 1), x + PORTRAIT / 2, y + PORTRAIT / 2 + 18, 36,
					hero.playable ? WHITE : 0xFF7A8294, Align.CENTER);
			}
			if (!hero.playable) {
				fill(g, x, y + PORTRAIT - 20, PORTRAIT, 20, 0xCC0B1118);
				HudText.draw(g, Face.SMALL, Component.translatable("overcraft.select.soon").getString().toUpperCase(Locale.ROOT),
					x + PORTRAIT / 2, y + PORTRAIT - 5, 10, TEXT_DIM, Align.CENTER);
			}
			if (!picked) {
				outline(g, x, y, PORTRAIT, PORTRAIT, 1.5f, hero == hovered ? 0xE6FFFFFF : 0x40FFFFFF);
			}
			if (hero == ClientHero.hero()) {
				// the hero being played now
				fill(g, x + 8, y + PORTRAIT + 4, PORTRAIT - 16, 3, WHITE);
			}
			if (picked) {
				String name = hero.displayName.toUpperCase(Locale.ROOT);
				float nameW = HudText.width(Face.BODY, name, 12) + 16;
				fill(g, x + PORTRAIT / 2 - nameW / 2, y + PORTRAIT + 10, nameW, 22, ORANGE);
				HudText.draw(g, Face.BODY, name, x + PORTRAIT / 2, y + PORTRAIT + 27, 12, 0xFF1A1206, Align.CENTER);
			}
		}
	}

	/** The orange READY button (grey "coming soon" for heroes not playable yet). */
	private void ready(GuiGraphicsExtractor g, float w, int mouseX, int mouseY) {
		float left = w / 2 - READY_W / 2;
		float top = READY_Y - READY_H / 2;
		boolean hover = overReady(mouseX, mouseY);
		if (preview.playable) {
			fill(g, left, top, READY_W, READY_H, hover ? 0xFFFFB13B : ORANGE);
			HudText.draw(g, Face.NUMBERS, Component.translatable("overcraft.select.ready").getString().toUpperCase(Locale.ROOT),
				w / 2, READY_Y + 16, 32, WHITE, Align.CENTER);
		} else {
			fill(g, left, top, READY_W, READY_H, 0xCC3A4152);
			HudText.draw(g, Face.BODY, Component.translatable("overcraft.select.coming_soon").getString().toUpperCase(Locale.ROOT),
				w / 2, READY_Y + 8, 16, TEXT_DIM, Align.CENTER);
		}
	}

	/** The key hints along the bottom, as Overwatch lists them. */
	private void hints(GuiGraphicsExtractor g, float w) {
		float y = 1046;
		keyBox(g, "F1", 106, y - 5, WHITE);
		HudText.draw(g, Face.BODY, Component.translatable("overcraft.select.details").getString().toUpperCase(Locale.ROOT),
			130, y + 2, 13, TEXT, Align.LEFT);
		String back = Component.translatable(ClientHero.hero() == null ? "overcraft.select.back_none" : "overcraft.select.back")
			.getString().toUpperCase(Locale.ROOT);
		HudText.draw(g, Face.BODY, back, w - 90, y + 2, 13, TEXT, Align.RIGHT);
		keyBox(g, "ESC", w - 90 - HudText.width(Face.BODY, back, 13) - 30, y - 5, WHITE);
	}

	/** One ability of the kit: the key and the ability (its name, description and icon go by this id). */
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
			// Overwatch's order: the Sonic Amplifier (its primary fire, on the left button as Kiriko's kunai), Soundwave
			// (the right), the abilities, the ultimate, the passive
			case LUCIO -> List.of(
				new Line(HeroHud.shortKey(HeroControls.get(Action.SECONDARY_FIRE)), "sonic_amplifier"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.PRIMARY_FIRE)), "soundwave"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.ABILITY_1)), "crossfade"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.ABILITY_2)), "amp_it_up"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.ULTIMATE)), "sound_barrier"),
				new Line(Component.translatable("overcraft.key.passive").getString(), "wall_ride"));
			// Biotic Pawjectiles (her primary fire, on the left button), Frenetic Flight (the right), Lifeline, Purr,
			// Catnapper, the Jetpack
			case JETPACK_CAT -> List.of(
				new Line(HeroHud.shortKey(HeroControls.get(Action.SECONDARY_FIRE)), "biotic_pawjectiles"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.PRIMARY_FIRE)), "frenetic_flight"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.ABILITY_1)), "lifeline"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.ABILITY_2)), "purr"),
				new Line(HeroHud.shortKey(HeroControls.get(Action.ULTIMATE)), "catnapper"),
				new Line(Component.translatable("overcraft.key.passive").getString(), "jetpack"));
			default -> List.of();
		};
	}
}
