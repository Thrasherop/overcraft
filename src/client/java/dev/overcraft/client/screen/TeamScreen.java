package dev.overcraft.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.hero.Teams;
import dev.overcraft.net.TeamPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.gradientRight;

/**
 * The Overwatch menu's CHANGE TEAM (Teams): no team (everyone together), Blue, Red and any other team on the
 * scoreboard, as a list like the menu's; the current one is marked. The server checks the allow_team_change rule and
 * answers in chat.
 */
public class TeamScreen extends OwScreen {
	private static final float ITEM_X = 130;
	private static final float ITEM_TOP = 330;
	private static final float ITEM_STEP = 64;
	private static final float ITEM_CAP = 36;

	/** A choice: the team's name ("" none), what to show, its colour. */
	private record Choice(String team, Component label, int colour) {
	}

	private final Screen parent;
	private List<Choice> choices = List.of();
	private String current = "";
	private int hovered = -1;
	private int selected;

	public TeamScreen(Screen parent) {
		super(Component.translatable("overcraft.team.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		List<Choice> list = new ArrayList<>();
		list.add(new Choice("", Component.translatable("overcraft.team.none"), WHITE));
		list.add(new Choice(Teams.BLUE, Component.translatable("overcraft.team.blue"), 0xFF3FA9F5));
		list.add(new Choice(Teams.RED, Component.translatable("overcraft.team.red"), 0xFFF2484B));
		Scoreboard scoreboard = minecraft.level == null ? null : minecraft.level.getScoreboard();
		if (scoreboard != null) {
			for (PlayerTeam team : scoreboard.getPlayerTeams()) {
				if (!team.getName().equals(Teams.BLUE) && !team.getName().equals(Teams.RED)) {
					list.add(new Choice(team.getName(), team.getDisplayName(), colour(team)));
				}
			}
			PlayerTeam mine = minecraft.player == null ? null : scoreboard.getPlayersTeam(minecraft.player.getScoreboardName());
			current = mine == null ? "" : mine.getName();
		}
		choices = list;
		for (int i = 0; i < choices.size(); i++) {
			if (choices.get(i).team().equals(current)) {
				selected = i;
			}
		}
	}

	private static int colour(PlayerTeam team) {
		return team.getColor().map(TeamColor::rgb).map(rgb -> 0xFF000000 | rgb).orElse(WHITE);
	}

	private int itemAt(double mouseX, double mouseY) {
		float x = refX(mouseX);
		float y = refY(mouseY);
		for (int i = 0; i < choices.size(); i++) {
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
			selected = Math.floorMod(selected + (key == InputConstants.KEY_UP ? -1 : 1), choices.size());
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
		if (ClientPlayNetworking.canSend(TeamPayload.TYPE)) {
			ClientPlayNetworking.send(new TeamPayload(choices.get(i).team()));
		}
		minecraft.gui.setScreen(null);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
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
		HudText.draw(g, Face.BODY, Component.translatable("overcraft.menu.title").getString().toUpperCase(Locale.ROOT),
			ITEM_X, 150, 15, ORANGE, Align.LEFT);
		HudText.draw(g, Face.NUMBERS, getTitle().getString().toUpperCase(Locale.ROOT), ITEM_X, 230, 64, WHITE, Align.LEFT);
		for (int i = 0; i < choices.size(); i++) {
			Choice choice = choices.get(i);
			float baseline = ITEM_TOP + i * ITEM_STEP;
			boolean active = i == selected;
			if (active) {
				gradientRight(g, ITEM_X - 30, baseline - ITEM_CAP - 12, 560, ITEM_CAP + 24, 0x40FFFFFF, 0x00FFFFFF);
				fill(g, ITEM_X - 30, baseline - ITEM_CAP - 12, 6, ITEM_CAP + 24, ORANGE);
			}
			// the team's colour as a bar before its name
			fill(g, ITEM_X, baseline - ITEM_CAP + 2, 8, ITEM_CAP - 4, choice.colour());
			String label = choice.label().getString().toUpperCase(Locale.ROOT);
			HudText.draw(g, Face.NUMBERS, label, ITEM_X + 22, baseline, ITEM_CAP, active ? WHITE : TEXT, Align.LEFT);
			if (choice.team().equals(current)) {
				float after = ITEM_X + 22 + HudText.width(Face.NUMBERS, label, ITEM_CAP) + 24;
				HudText.draw(g, Face.BODY, Component.translatable("overcraft.team.current").getString().toUpperCase(Locale.ROOT),
					after, baseline - 4, 14, ORANGE, Align.LEFT);
			}
		}
		keyBox(g, "ESC", ITEM_X + 8, 1010, WHITE);
		HudText.draw(g, Face.BODY, Component.translatable("overcraft.team.back").getString().toUpperCase(Locale.ROOT),
			ITEM_X + 40, 1017, 14, TEXT, Align.LEFT);
		g.pose().popMatrix();
	}
}
