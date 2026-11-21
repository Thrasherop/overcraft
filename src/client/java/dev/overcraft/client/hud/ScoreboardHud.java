package dev.overcraft.client.hud;

import dev.overcraft.client.HeroArt;
import dev.overcraft.client.HeroHud;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.hero.Hero;
import dev.overcraft.net.StatsPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * The Tab scoreboard in hero mode (owner, 2026-11-21: "is it possible to add a tab UI for stats?"), in place of Minecraft's
 * player list while Tab is held: Overwatch's scoreboard rows, your team on top and any other team below, each player's
 * hero portrait, name and this run's numbers (PlayerStats, since the last dragon: StatsPayload once a second). Overwatch's
 * columns are E A D DMG H MIT; ours are the ones counted: E (eliminations), D (deaths), DMG (damage done), H (healing done),
 * in Overwatch HP. No assists or mitigation are counted yet. Layout and sizes ours, in the HUD's 1920x1080 reference
 * pixels; the colours are the kill feed's (Overwatch's team blue and red).
 */
public final class ScoreboardHud {
	private ScoreboardHud() {
	}

	private static final float WIDTH = 1040;
	private static final float ROW_H = 46;
	private static final float ROW_GAP = 4;
	private static final float PORTRAIT_W = 68;
	private static final float HEADER_H = 30;
	private static final int ALLY = 0x277CA6;
	private static final int ENEMY = 0x8B293C;
	private static final int ALLY_EDGE = 0x30B1DD;
	private static final int ENEMY_EDGE = 0xAA3F66;
	private static final int PANEL = 0x0B1520;
	/** The columns' right edges from the panel's right, and their headings. */
	private static final String[] HEADINGS = {"E", "D", "DMG", "H"};
	private static final float[] COLUMNS = {470, 380, 220, 60};
	/** Where the face band starts in a hero's 256 px portrait art (KillFeedHud's). */
	private static final int PORTRAIT_TOP = 30;

	private static List<StatsPayload.Row> rows = List.of();

	public static void set(StatsPayload payload) {
		rows = payload.rows();
	}

	public static void clear() {
		rows = List.of();
	}

	/** Shown instead of Minecraft's list: Tab held, in hero mode. */
	public static boolean showing() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.options.keyPlayerList.isDown() && dev.overcraft.client.ClientHero.heroModeOn();
	}

	/** Minecraft's player list's slot: ours in hero mode with Tab held, else theirs. */
	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		float scale = HeroHud.scaleOf(g);
		float w = g.guiWidth() / scale;
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		String me = mc.player.getName().getString();
		Scoreboard board = mc.level == null ? null : mc.level.getScoreboard();
		PlayerTeam myTeam = board == null ? null : board.getPlayersTeam(me);
		List<StatsPayload.Row> mine = new ArrayList<>();
		List<StatsPayload.Row> theirs = new ArrayList<>();
		for (StatsPayload.Row r : rows) {
			PlayerTeam team = board == null ? null : board.getPlayersTeam(r.name());
			(Objects.equals(team, myTeam) ? mine : theirs).add(r);
		}
		Comparator<StatsPayload.Row> order = Comparator.comparingDouble((StatsPayload.Row r) -> r.damage() + r.healing()).reversed();
		mine.sort(order);
		theirs.sort(order);
		float x = (w - WIDTH) / 2;
		float y = 120;
		// the title band
		fill(g, x, y, WIDTH, 44, argb(PANEL, 0.82f));
		HudText.draw(g, Face.BODY, "SCOREBOARD", x + 20, y + 30, 20, 0xFFFFFFFF, Align.LEFT);
		HudText.draw(g, Face.SMALL, "THIS RUN, SINCE THE LAST DRAGON", x + WIDTH - 20, y + 28, 12, 0xFFB7C3D0, Align.RIGHT);
		y += 44 + 10;
		y = team(g, myTeam == null ? "YOUR TEAM" : team(myTeam) + " (YOUR TEAM)", mine, me, true, x, y);
		if (!theirs.isEmpty()) {
			y += 16;
			team(g, "ENEMY TEAM", theirs, me, false, x, y);
		}
		g.pose().popMatrix();
	}

	private static String team(PlayerTeam team) {
		return team.getDisplayName().getString().toUpperCase(Locale.ROOT);
	}

	/** One team's heading and rows from y; returns the y below them. */
	private static float team(GuiGraphicsExtractor g, String title, List<StatsPayload.Row> list, String me, boolean ally, float x,
							  float y) {
		int colour = ally ? ALLY : ENEMY;
		fill(g, x, y, WIDTH, HEADER_H, argb(PANEL, 0.75f));
		fill(g, x, y + HEADER_H - 3, WIDTH, 3, argb(ally ? ALLY_EDGE : ENEMY_EDGE, 1));
		HudText.draw(g, Face.SMALL, title, x + 16, y + 21, 13, 0xFFFFFFFF, Align.LEFT);
		for (int i = 0; i < HEADINGS.length; i++) {
			HudText.draw(g, Face.SMALL, HEADINGS[i], x + WIDTH - COLUMNS[i], y + 21, 13, 0xFFB7C3D0, Align.RIGHT);
		}
		y += HEADER_H + ROW_GAP;
		if (list.isEmpty()) {
			fill(g, x, y, WIDTH, ROW_H, argb(PANEL, 0.6f));
			HudText.draw(g, Face.SMALL, "NO PLAYERS", x + WIDTH / 2, y + ROW_H / 2 + 6, 13, 0xFFB7C3D0, Align.CENTER);
			return y + ROW_H + ROW_GAP;
		}
		for (StatsPayload.Row r : list) {
			row(g, r, r.name().equals(me), colour, ally ? ALLY_EDGE : ENEMY_EDGE, x, y);
			y += ROW_H + ROW_GAP;
		}
		return y;
	}

	private static void row(GuiGraphicsExtractor g, StatsPayload.Row r, boolean you, int colour, int edge, float x, float y) {
		fill(g, x, y, WIDTH, ROW_H, argb(colour, you ? 0.92f : 0.7f));
		// the portrait, its bright edge next to the name
		fill(g, x, y, PORTRAIT_W, ROW_H, argb(PANEL, 0.6f));
		Hero hero = r.hero().isEmpty() ? null : Hero.byId(r.hero());
		Identifier portrait = hero == null ? null : HeroArt.portrait(hero);
		if (portrait != null) {
			float bandH = 256 * ROW_H / PORTRAIT_W;
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(PORTRAIT_W / 256, ROW_H / bandH);
			g.blit(RenderPipelines.GUI_TEXTURED, portrait, 0, 0, 0, PORTRAIT_TOP, 256, Math.round(bandH), 256, Math.round(bandH), 256,
				256, r.heroMode() ? 0xFFFFFFFF : 0xFF808080);
			g.pose().popMatrix();
		}
		fill(g, x + PORTRAIT_W, y, 4, ROW_H, argb(edge, 1));
		HudText.draw(g, Face.BODY, r.name().toUpperCase(Locale.ROOT), x + PORTRAIT_W + 18, y + ROW_H / 2 + 7, 16, 0xFFFFFFFF, Align.LEFT);
		String sub = hero == null ? "NO HERO" : !r.heroMode() ? hero.displayName.toUpperCase(Locale.ROOT) + "  (MINECRAFT MODE)"
			: hero.displayName.toUpperCase(Locale.ROOT);
		HudText.draw(g, Face.SMALL, sub, x + PORTRAIT_W + 18 + HudText.width(Face.BODY, r.name().toUpperCase(Locale.ROOT), 16) + 14,
			y + ROW_H / 2 + 6, 11, 0xFFD6E2EE, Align.LEFT);
		String[] values = {Integer.toString(r.kills()), Integer.toString(r.deaths()), number(r.damage()), number(r.healing())};
		for (int i = 0; i < values.length; i++) {
			HudText.draw(g, Face.NUMBERS, values[i], x + WIDTH - COLUMNS[i], y + ROW_H / 2 + 10, 24, 0xFFFFFFFF, Align.RIGHT);
		}
		if (you) {
			outline(g, x - 2, y - 2, WIDTH + 4, ROW_H + 4, 2, 0xE6FFFFFF);
		}
	}

	/** Overwatch's scoreboard writes 12,345 in full. */
	private static String number(double value) {
		return String.format(Locale.ROOT, "%,d", Math.round(value));
	}

	private static int argb(int rgb, float a) {
		return Math.clamp(Math.round(a * 255), 0, 255) << 24 | rgb & 0xFFFFFF;
	}

	/** For a dev check: the rows held now. */
	public static @Nullable List<StatsPayload.Row> rows() {
		return rows;
	}
}
