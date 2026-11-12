package dev.overcraft.client.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.overcraft.client.HeroArt;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.client.model.HeroModelRenderer;
import dev.overcraft.hero.Hero;
import dev.overcraft.registry.OcSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Play of the Game's look, recreated from the owner's clip (techniques/potg/potg_reference.md, measured frame by frame;
 * techniques/potg/potg_look.md). In the order a player sees it:
 * <ol>
 * <li>the team lineup (POTG only): every player's hero posed, their name over their head (clip 8.00-10.95 s);</li>
 * <li>a dip to black, then the hero's highlight intro (the clip from the data: HeroModelRenderer.previewClip) with the
 * left-stacked text: PLAY OF THE GAME, the name popping in, AS &lt;HERO&gt; word by word, the flare, the slow drift (§1);</li>
 * <li>the cut to white: held 0.3 s (longer while the replay's login is still on screen), then a 0.5 s ease-in fade (§2);</li>
 * <li>over the play: the top band and rule, PLAY OF THE GAME &lt;HERO&gt; (the hero in #FF7712), BY &lt;PLAYER&gt;, and the
 * player card on the right (§3);</li>
 * <li>the hard cut to black at the play's end (the server's CUT), 0.6 s, then 0.2 s into the end screen (POTG only): the
 * VICTORY bar and the cards, players then mobs, staggered 0.3 s with streaks, each figure posed above its card (§4).</li>
 * </ol>
 * Every position, size and colour below is the clip's (fractions of the screen) unless it says ours. It's drawn last in
 * the GUI (GuiPotgMixin), over any screen, the login's loading screens included, so the white and the black hide them. No
 * category word anywhere, no rank emblem, no player title, no capture prompt (owner, 2026-11-11).
 */
public final class PotgCard {
	private PotgCard() {
	}

	// the clip's colours (potg_reference.md)
	private static final int INTRO_TITLE = 0xCFDAE5;
	private static final int INTRO_NAME = 0xFF9721;
	private static final int INTRO_HERO = 0xCFDAE6;
	private static final int BANNER_HERO = 0xFF7712;
	private static final int CARD_FILL = 0x09101E;
	private static final int CARD_BORDER = 0x17BDEA;
	private static final int CARD_NAME = 0x06BDEE;
	private static final int END_HEADER_TOP = 0x0A1020;
	private static final int END_HEADER_BOTTOM = 0x0F1727;
	private static final int END_RULE = 0x051435;
	private static final int END_VICTORY = 0x00ADFF;
	private static final int END_BG_TOP = 0x5B76A9;
	private static final int END_BG_BOTTOM = 0x6985B6;
	private static final int ALLY = 0x037ABE;
	private static final int ENEMY = 0xA64C61;
	private static final int STAT_LABEL = 0xD87800;
	/** The lineup's names (the clip, by eye from t_8.5: a light blue). */
	private static final int LINEUP_NAME = 0x3AA8F0;

	/** Seconds (the clip): the lineup, the dip, the intro; the white's hold and fade; the black and its fade. */
	private static final float LINEUP = dev.overcraft.potg.PotgReplay.LINEUP_MS / 1000f;
	private static final float DIP = dev.overcraft.potg.PotgReplay.DIP_MS / 1000f;
	private static final float INTRO = dev.overcraft.potg.PotgReplay.INTRO_MS / 1000f;
	private static final float INTRO_FADE = 0.27f;
	private static final float WHITE_HOLD = 0.3f;
	private static final float WHITE_FADE = 0.5f;
	/** The white holds at most this long waiting for the replay's login (ours). */
	private static final float WHITE_MAX = 4f;
	private static final float BLACK = 0.6f;
	private static final float BLACK_FADE = 0.2f;
	/** The end screen: held 6 s, faded out 0.3 s (ours: the clip ends at 32.5 s, 1.4 s in). */
	private static final float END_HOLD = 6f;
	private static final float END_FADE = 0.3f;
	/** The cards come one every 0.3 s, each with a streak about 0.2 s (the clip). */
	private static final float CARD_STEP = 0.3f;

	private static boolean showing;
	private static boolean potg;
	private static long t0;
	private static JsonObject data = new JsonObject();
	private static float introAt;
	private static boolean soundsPlayed;
	/** Wall clock (ms) of the cut to black, the last loading screen and the last change of level. */
	private static long cutAt = -1;
	private static long lastLoading = -1;
	private static long levelChanged = -1;
	private static @Nullable Object lastLevel;
	private static long fadeFromWhiteAt = -1;
	private static long endScreenAt = -1;
	private static boolean done;
	private static final Map<String, LivingEntity> MOBS = new HashMap<>();
	private static @Nullable Boolean hideGuiBefore;

	/** A POTG (the lineup and the end screen) or a solo highlight: the server's CARD or SOLO. */
	public static void show(boolean isPotg, String json) {
		showing = true;
		potg = isPotg;
		t0 = Util.getMillis();
		JsonObject parsed;
		try {
			JsonElement e = JsonParser.parseString(json == null || json.isEmpty() ? "{}" : json);
			parsed = e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
		} catch (Exception e) {
			parsed = new JsonObject();
		}
		data = parsed;
		introAt = potg ? LINEUP + DIP : DIP;
		soundsPlayed = false;
		cutAt = -1;
		fadeFromWhiteAt = -1;
		endScreenAt = -1;
		lastLoading = -1;
		levelChanged = -1;
		lastLevel = Minecraft.getInstance().level;
		done = false;
		MOBS.clear();
		Minecraft mc = Minecraft.getInstance();
		if (hideGuiBefore == null) {
			hideGuiBefore = mc.gui.hud.isHidden();
		}
		hud(true);
	}

	/** The play has ended (the server's CUT): the hard cut to black. */
	public static void cut() {
		if (showing && cutAt < 0) {
			cutAt = Util.getMillis();
			hud(true);
		}
	}

	public static boolean showing() {
		return showing && !done;
	}

	/** Whether the whole sequence (end screen included) has played out: the showing can close. */
	public static boolean finished() {
		return done;
	}

	public static void hide() {
		// the replay's kill feed doesn't carry over into the world (found in play: a replayed kill showed after the return)
		KillFeedHud.clear();
		showing = false;
		done = false;
		MOBS.clear();
		if (hideGuiBefore != null) {
			hud(hideGuiBefore);
			hideGuiBefore = null;
		}
	}

	/** The HUD (and the hand) hidden as F1 hides them: none in the lineup, the intro or the end screen. */
	private static void hud(boolean hidden) {
		var hud = Minecraft.getInstance().gui.hud;
		if (hud.isHidden() != hidden) {
			hud.toggle();
		}
	}

	private static void play(SoundEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSoundManager().getSoundEvent(event.location()) != null) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(event, 1));
		}
	}

	/** Each client tick: what's on screen (loading screens, a new level) moves the white's and the black's ends. */
	public static void tick(Minecraft mc) {
		if (!showing) {
			return;
		}
		long now = Util.getMillis();
		var screen = mc.gui.screen();
		if (screen != null && (screen.getClass().getSimpleName().contains("Loading") || screen.getClass().getSimpleName().contains("Receiving"))
			|| mc.level == null) {
			lastLoading = now;
		}
		if (mc.level != lastLevel) {
			lastLevel = mc.level;
			levelChanged = now;
		}
		float t = (now - t0) / 1000f;
		if (!soundsPlayed && t >= introAt) {
			// the announcer's line and the music start with the intro's first frames (the clip: 13.05 s, §5)
			soundsPlayed = true;
			mc.getSoundManager().stop();
			play(OcSounds.MUSIC_POTG);
			play(OcSounds.ANNOUNCER_POTG);
		}
		// the play shows the replayed view: the HUD comes back for it (and goes at the cut)
		boolean inPlay = fadeFromWhiteAt > 0 && cutAt < 0;
		hud(!inPlay);
	}

	// ---------------------------------------------------------------- drawing

	/** Drawn last in the GUI, over screens (GuiPotgMixin). */
	public static void render(GuiGraphicsExtractor g) {
		if (!showing || done) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		g.nextStratum();
		long now = Util.getMillis();
		float t = (now - t0) / 1000f;
		float scale = HudDraw.scale(g);
		float w = g.guiWidth() / scale;
		float h = g.guiHeight() / scale;
		if (cutAt >= 0) {
			outro(g, mc, now, scale, w, h);
			return;
		}
		if (potg && t < LINEUP) {
			lineup(g, mc, t, scale, w, h);
			return;
		}
		if (t < introAt) {
			fill(g, scale, w, h, 0x000000, 1);
			return;
		}
		float it = t - introAt;
		if (it < INTRO) {
			intro(g, mc, it, scale, w, h);
			return;
		}
		// the cut to white, held while the replay's login is still on screen
		long whiteAt = t0 + Math.round((introAt + INTRO) * 1000);
		if (fadeFromWhiteAt < 0) {
			long ready = Math.max(whiteAt, Math.max(lastLoading, levelChanged)) + Math.round(WHITE_HOLD * 1000);
			boolean started = levelChanged > whiteAt - 1000;
			if (now >= ready && (started || now > whiteAt + WHITE_MAX * 1000)) {
				fadeFromWhiteAt = now;
			}
		}
		float white = 1;
		if (fadeFromWhiteAt > 0) {
			float u = Math.min(1, (now - fadeFromWhiteAt) / 1000f / WHITE_FADE);
			// ease-in: the white leaves slowly, then fast (per-frame luma drop 6 at the start, 20 at the end: §2)
			white = 1 - u * u;
			banner(g, (now - fadeFromWhiteAt) / 1000f, scale, w, h);
		}
		if (white > 0) {
			fill(g, scale, w, h, 0xFFFFFF, white);
		}
	}

	private static void fill(GuiGraphicsExtractor g, float scale, float w, float h, int rgb, float a) {
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		HudDraw.fill(g, 0, 0, w, h, alpha(rgb, a));
		g.pose().popMatrix();
	}

	// ---- 1. the lineup
	private static void lineup(GuiGraphicsExtractor g, Minecraft mc, float t, float scale, float w, float h) {
		JsonArray list = data.has("lineup") ? data.getAsJsonArray("lineup") : new JsonArray();
		int n = list.size();
		// the clip: five heroes across 20-85% W, feet about 88% H, about 55% H tall; ours spaced 15% W about the middle
		float spacing = 0.15f * w;
		float figure = 0.55f * h;
		for (int i = 0; i < n; i++) {
			JsonObject p = list.get(i).getAsJsonObject();
			float cx = w / 2 + (i - (n - 1) / 2f) * spacing;
			float feet = 0.88f * h;
			Hero hero = Hero.byId(str(p, "hero"));
			if (hero != null) {
				// each at its own point of the hold (ours), turned a little toward the middle (ours)
				drawHero(mc, g, hero, null, 0, 1.5f + i * 0.7f, cx * scale, feet * scale, figure * scale, (cx - w / 2) * 0.15f * scale);
			}
		}
		// the names over every figure, after them all (found in play: a wing drawn later covered a name)
		g.nextStratum();
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		for (int i = 0; i < n; i++) {
			JsonObject p = list.get(i).getAsJsonObject();
			float cx = w / 2 + (i - (n - 1) / 2f) * spacing;
			HudText.draw(g, Face.BODY, str(p, "name").toUpperCase(Locale.ROOT), cx, 0.88f * h - figure - 0.02f * h, 0.014f * h,
				alpha(LINEUP_NAME, 1), Align.CENTER);
		}
		g.pose().popMatrix();
		// the clip's dip: dark at 7.6 s, back up by 8.0 (ours: from 60% black over 0.25 s); then a hard cut to black
		float dim = t < 0.25f ? 0.6f * (1 - t / 0.25f) : 0;
		if (dim > 0) {
			fill(g, scale, w, h, 0x000000, dim);
		}
	}

	// ---- 2. the hero's intro and its text (§1)
	private static void intro(GuiGraphicsExtractor g, Minecraft mc, float it, float scale, float w, float h) {
		Hero hero = Hero.byId(str(data, "hero"));
		if (hero != null) {
			// a still camera on the hero, the hero at the middle-right, feet below the frame (the clip at 16.0 s); ours: the
			// frozen world behind instead of Overwatch's set
			drawHero(mc, g, hero, "potg_intro", it, it, 0.55f * w * scale, 1.10f * h * scale, 1.02f * h * scale, -0.04f * w * scale);
		}
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		// the block drifts right 0.43% W/s all through (§1)
		float drift = 0.0043f * w * it;
		// PLAY OF THE GAME: from 0.12 s (frame 789), readable 0.05 s later; grows 2%/s
		float titleA = clamp01((it - 0.12f) / 0.08f);
		float titleCap = 0.039f * h * (1 + 0.02f * it);
		String title = title();
		shadowed(g, Face.NUMBERS, title, 0.077f * w + drift, 0.224f * h, titleCap, INTRO_TITLE, titleA);
		// the flare: a blue-white streak sweeps left to right across the title line, 0.17-0.32 s (frames 792-801)
		float fu = (it - 0.17f) / 0.15f;
		if (fu >= 0 && fu <= 1) {
			float y = 0.224f * h - titleCap / 2;
			float a = Mth.sin(fu * Mth.PI);
			HudDraw.fill(g, 0, y - 1, w, 2, alpha(0xCFE8FF, 0.45f * a));
			float gx = -0.1f * w + fu * 1.2f * w;
			HudDraw.gradientRight(g, gx - 0.12f * w, y - 3, 0.12f * w, 6, 0, alpha(0xE6F4FF, 0.9f * a));
			HudDraw.gradientRight(g, gx, y - 3, 0.12f * w, 6, alpha(0xE6F4FF, 0.9f * a), 0);
			HudDraw.gradientRight(g, gx - 0.05f * w, y - 14, 0.05f * w, 28, 0, alpha(0xB8DCFF, 0.35f * a));
			HudDraw.gradientRight(g, gx, y - 14, 0.05f * w, 28, alpha(0xB8DCFF, 0.35f * a), 0);
		}
		// the name pops in at 0.52 s (frame 813) at 1.5x, motion-blurred right, and settles by 0.75 s, ease-out; its left
		// edge moves right 42 px (1.6% W) as it shrinks; then it grows 5%/s
		float pop = (it - 0.52f) / 0.23f;
		if (pop >= 0) {
			float e = 1 - (float) Math.pow(1 - clamp01(pop), 3);
			float size = pop < 1 ? 1.5f - 0.5f * e : 1 + 0.05f * (it - 0.75f);
			float x = 0.105f * w + drift - 0.0164f * w * (1 - e);
			float a = clamp01(pop / 0.15f);
			String name = str(data, "player").toUpperCase(Locale.ROOT);
			float cap = 0.069f * h * size;
			if (pop < 1) {
				for (int i = 1; i <= 3; i++) {
					plainAt(g, Face.NUMBERS, name, x + i * 0.012f * w * (1 - e), 0.31f * h, cap, INTRO_NAME, 0.18f * a * (1 - e));
				}
			}
			shadowed(g, Face.NUMBERS, name, x, 0.31f * h, cap, INTRO_NAME, a);
		}
		// AS at 1.37 s, the hero's name at 1.52 s, each a quick fade (14.4 and 14.5-14.6 s in the clip)
		float heroCap = 0.030f * h * (1 + 0.02f * it);
		float asA = clamp01((it - 1.37f) / 0.1f);
		float heroA = clamp01((it - 1.52f) / 0.1f);
		if (asA > 0) {
			// " AS " (string 0DC6, D) on its own line
			float x = 0.127f * w + drift;
			shadowed(g, Face.NUMBERS, "AS", x, 0.382f * h, heroCap, INTRO_HERO, asA);
			if (heroA > 0) {
				float asW = HudText.width(Face.NUMBERS, "AS ", heroCap);
				shadowed(g, Face.NUMBERS, str(data, "heroName").toUpperCase(Locale.ROOT), x + asW, 0.382f * h, heroCap, INTRO_HERO, heroA);
			}
		}
		g.pose().popMatrix();
		// the scene fades up from black (frames 782-798)
		if (it < INTRO_FADE) {
			fill(g, scale, w, h, 0x000000, 1 - it / INTRO_FADE);
		}
	}

	// ---- 4 and 5. over the play (§3)
	private static void banner(GuiGraphicsExtractor g, float pt, float scale, float w, float h) {
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		// the top band: the scene darkened to about 30% (black at 70%), 11.6% H; the light rule under it, 0.35% H, white at
		// about 27%. Flat, there from the play's first frame (under the white)
		HudDraw.fill(g, 0, 0, w, 0.116f * h, alpha(0x000000, 0.70f));
		HudDraw.fill(g, 0, 0.116f * h, w, 0.0035f * h, alpha(0xFFFFFF, 0.27f));
		// the text fades in 0.33 s after the white has cleared (19.1 s), over about 0.2 s, drifting a little right
		float ta = clamp01((pt - WHITE_FADE - 0.33f) / 0.2f);
		if (ta > 0) {
			float dx = 0.002f * w * Math.min(1, pt - WHITE_FADE - 0.33f);
			float cap = 0.032f * h;
			String title = title() + " ";
			float x = 0.022f * w + dx;
			shadowed(g, Face.NUMBERS, title, x, 0.062f * h, cap, 0xFFFFFF, ta);
			shadowed(g, Face.NUMBERS, str(data, "heroName").toUpperCase(Locale.ROOT), x + HudText.width(Face.NUMBERS, title, cap),
				0.062f * h, cap, BANNER_HERO, ta);
			shadowed(g, Face.BODY, "BY " + str(data, "player").toUpperCase(Locale.ROOT), 0.0215f * w + dx, 0.089f * h, 0.011f * h,
				0xFFFFFF, ta);
		}
		playerCard(g, w, h);
		g.pose().popMatrix();
	}

	/**
	 * The player card on the right (§3: x 83.2-97.4% W, y 29.7-68.8% H, navy fill, a cyan border with a glow). The owner's
	 * stand-ins (2026-11-11): the hero's portrait for the player's icon, their name, and their most-played heroes in this
	 * world (hero-mode time, PlayerStats) under them, with the hours. No level, title, endorsement or add-friend.
	 */
	private static void playerCard(GuiGraphicsExtractor g, float w, float h) {
		float x0 = 0.832f * w;
		float x1 = 0.974f * w;
		float y0 = 0.297f * h;
		float y1 = 0.688f * h;
		float cw = x1 - x0;
		float ch = y1 - y0;
		// the glow, the border, the fill
		for (int i = 3; i >= 1; i--) {
			HudDraw.fill(g, x0 - i * 2, y0 - i * 2, cw + i * 4, ch + i * 4, alpha(CARD_BORDER, 0.08f));
		}
		HudDraw.fill(g, x0, y0, cw, ch, alpha(CARD_BORDER, 1));
		HudDraw.fill(g, x0 + 3, y0 + 3, cw - 6, ch - 6, alpha(CARD_FILL, 0.96f));
		// the portrait: centred at 28% of the card's height, 39% of its width (crop_card_full), in an orange frame (ours)
		Hero hero = Hero.byId(str(data, "hero"));
		Identifier portrait = hero == null ? null : HeroArt.portrait(hero);
		float ps = 0.39f * cw;
		float px = x0 + cw / 2 - ps / 2;
		float py = y0 + 0.28f * ch - ps / 2;
		HudDraw.fill(g, px - 4, py - 4, ps + 8, ps + 8, alpha(0xFF9721, 0.9f));
		if (portrait != null) {
			HudDraw.image(g, portrait, px, py, ps, ps, 0xFFFFFFFF);
		} else {
			HudDraw.fill(g, px, py, ps, ps, alpha(0x1A2440, 1));
		}
		// the name, Big Noodle in cyan, cap 1.8% H, at 56% of the card
		HudText.draw(g, Face.NUMBERS, str(data, "player").toUpperCase(Locale.ROOT), x0 + cw / 2, y0 + 0.575f * ch, 0.018f * h,
			alpha(CARD_NAME, 1), Align.CENTER);
		// most played: three circles at 21, 47.5 and 74% across, 79% down, the number under each at 90% (crop_card_full);
		// ours: the label, and the time in hero mode with that hero in this world
		JsonArray most = data.has("most") ? data.getAsJsonArray("most") : new JsonArray();
		HudText.draw(g, Face.SMALL, "MOST PLAYED", x0 + cw / 2, y0 + 0.67f * ch, 0.0085f * h, alpha(0x8FA3C4, 1), Align.CENTER);
		float[] at = {0.21f, 0.475f, 0.74f};
		float d = 0.2f * cw;
		for (int i = 0; i < Math.min(3, most.size()); i++) {
			JsonObject m = most.get(i).getAsJsonObject();
			Hero mh = Hero.byId(str(m, "hero"));
			float cx = x0 + at[i] * cw;
			float cy = y0 + 0.79f * ch;
			HudDraw.image(g, HudShapes.DISC, cx - d / 2 - 3, cy - d / 2 - 3, d + 6, d + 6, alpha(0x000000, 0.9f));
			Identifier icon = mh == null ? null : HeroArt.portrait(mh);
			if (icon != null) {
				HudDraw.image(g, icon, cx - d / 2, cy - d / 2, d, d, 0xFFFFFFFF);
			}
			long seconds = m.has("seconds") ? m.get("seconds").getAsLong() : 0;
			String time = seconds >= 3600 ? String.format(Locale.ROOT, "%.1fH", seconds / 3600f) : Math.max(1, seconds / 60) + "M";
			HudText.draw(g, Face.BODY, time, cx, y0 + 0.92f * ch, 0.0095f * h, alpha(0xA9B4C8, 1), Align.CENTER);
		}
	}

	// ---- 6 and 7. the cut to black and the end screen (§4)
	private static void outro(GuiGraphicsExtractor g, Minecraft mc, long now, float scale, float w, float h) {
		if (endScreenAt < 0) {
			long ready = Math.max(cutAt + Math.round(BLACK * 1000), lastLoading + 100);
			if (now >= ready && mc.level != null) {
				endScreenAt = now;
			}
		}
		if (endScreenAt < 0) {
			fill(g, scale, w, h, 0x000000, 1);
			return;
		}
		float et = (now - endScreenAt) / 1000f;
		if (!potg) {
			// a solo highlight: back to the world, faded up from black (the clip's 0.2 s)
			if (et >= BLACK_FADE) {
				done = true;
				return;
			}
			fill(g, scale, w, h, 0x000000, 1 - et / BLACK_FADE);
			return;
		}
		if (et > BLACK_FADE + END_HOLD + END_FADE) {
			done = true;
			return;
		}
		float a = et > BLACK_FADE + END_HOLD ? 1 - (et - BLACK_FADE - END_HOLD) / END_FADE : 1;
		endScreen(g, mc, et - BLACK_FADE, scale, w, h, a);
		if (et < BLACK_FADE) {
			fill(g, scale, w, h, 0x000000, 1 - et / BLACK_FADE);
		}
	}

	private static void endScreen(GuiGraphicsExtractor g, Minecraft mc, float et, float scale, float w, float h, float a) {
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		// a flat void, blue-grey; ours: a lighter floor from 72% H
		HudDraw.gradient(g, 0, 0, w, h, alpha(END_BG_TOP, a), alpha(END_BG_BOTTOM, a));
		HudDraw.gradient(g, 0, 0.72f * h, w, 0.28f * h, alpha(0xFFFFFF, 0), alpha(0xFFFFFF, 0.08f * a));
		g.pose().popMatrix();
		JsonArray cards = data.has("cards") ? data.getAsJsonArray("cards") : new JsonArray();
		List<JsonObject> allies = new ArrayList<>();
		List<JsonObject> enemies = new ArrayList<>();
		for (JsonElement e : cards) {
			JsonObject c = e.getAsJsonObject();
			("enemy".equals(str(c, "side")) ? enemies : allies).add(c);
		}
		// the clip's centres, two a side: 16.4, 36.4 | 64.5, 83.6% W; ours for one or three
		float[][] allyAt = {{}, {0.264f}, {0.164f, 0.364f}, {0.11f, 0.265f, 0.42f}};
		float[][] enemyAt = {{}, {0.74f}, {0.645f, 0.836f}};
		int i = 0;
		int na = Math.min(3, allies.size());
		int ne = Math.min(2, enemies.size());
		for (int k = 0; k < na; k++, i++) {
			card(g, mc, allies.get(k), allyAt[na][k], true, et - i * CARD_STEP, scale, w, h, a);
		}
		for (int k = 0; k < ne; k++, i++) {
			card(g, mc, enemies.get(k), enemyAt[ne][k], false, et - i * CARD_STEP, scale, w, h, a);
		}
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		// the header bar: 12.5% H, dark navy, a dark rule under it; VICTORY in #00ADFF, cap 3.7% H, at 2.6% W
		HudDraw.gradient(g, 0, 0, w, 0.125f * h, alpha(END_HEADER_TOP, a), alpha(END_HEADER_BOTTOM, a));
		HudDraw.fill(g, 0, 0.125f * h, w, 0.003f * h, alpha(END_RULE, a));
		String victory = Component.translatable("overcraft.hud.victory").getString().toUpperCase(Locale.ROOT);
		HudText.plain(g, Face.BODY, victory, 0.026f * w, 0.080f * h, 0.037f * h, alpha(END_VICTORY, a), Align.LEFT);
		// to its right the map's name (ours: the dragon's dimension)
		float vw = HudText.width(Face.BODY, victory, 0.037f * h);
		HudText.plain(g, Face.BODY, "THE END", 0.026f * w + vw + 0.015f * w, 0.058f * h, 0.012f * h, alpha(0xFFFFFF, a), Align.LEFT);
		g.pose().popMatrix();
	}

	/** One card: the figure posed above it, the name, the stat, its label, the team's rule; it comes in with a streak. */
	private static void card(GuiGraphicsExtractor g, Minecraft mc, JsonObject c, float centre, boolean ally, float ct, float scale,
							 float w, float h, float a) {
		if (ct < 0) {
			return;
		}
		float in = clamp01(ct / 0.15f) * a;
		float cx = centre * w;
		float cw = 0.148f * w;
		float top = 0.69f * h;
		float bottom = 0.87f * h;
		int team = ally ? ALLY : ENEMY;
		// the figure, standing on the card (the clip: feet about 85% H, about 50% H tall)
		if (in > 0.3f) {
			Hero hero = Hero.byId(str(c, "hero"));
			try {
				if (hero != null) {
					drawHero(mc, g, hero, null, 0, 2 + ct, cx * scale, 0.85f * h * scale, 0.5f * h * scale, 0);
				} else if (c.has("mob")) {
					drawMob(mc, g, str(c, "mob"), cx * scale, 0.85f * h * scale, 0.5f * h * scale, cw * scale);
				}
			} catch (RuntimeException e) {
				// a figure that can't be drawn leaves its card bare, never the game down
				MOBS.remove(str(c, "mob"));
				dev.overcraft.Overcraft.LOG.warn("[POTG] couldn't draw the figure for {}: {}", c, e.toString());
			}
		}
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		HudDraw.fill(g, cx - cw / 2, top, cw, bottom - top, alpha(0x000A1A, 0.2f * in));
		HudDraw.fill(g, cx - cw / 2, bottom - 0.0035f * h, cw, 0.0035f * h, alpha(team, in));
		HudDraw.gradient(g, cx - cw / 2, bottom - 0.02f * h, cw, 0.0165f * h, 0, alpha(team, 0.25f * in));
		String name = str(c, "name");
		if (name.isEmpty() && c.has("mob")) {
			String id = str(c, "mob");
			name = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id)).map(t -> t.getDescription().getString()).orElse(id);
		}
		HudText.draw(g, Face.NUMBERS, name.toUpperCase(Locale.ROOT), cx, 0.74f * h, 0.026f * h, alpha(0xFFFFFF, in), Align.CENTER);
		long stat = c.has("stat") ? c.get("stat").getAsLong() : 0;
		HudText.draw(g, Face.NUMBERS, String.format(Locale.ROOT, "%,d", stat), cx, 0.804f * h, 0.050f * h, alpha(0xFFFFFF, in), Align.CENTER);
		HudText.plain(g, Face.BODY, str(c, "label"), cx, 0.846f * h, 0.0125f * h, alpha(STAT_LABEL, in), Align.CENTER);
		// the streak: a horizontal light in the team's colour across the card, about 0.2 s
		float su = ct / 0.2f;
		if (su <= 1) {
			int light = ally ? 0x6FE4FF : 0xFF6A7E;
			float sa = Mth.sin(su * Mth.PI) * a;
			float y = 0.78f * h;
			HudDraw.gradientRight(g, cx - cw, y - 2, cw, 4, 0, alpha(light, sa));
			HudDraw.gradientRight(g, cx, y - 2, cw, 4, alpha(light, sa), 0);
			HudDraw.gradientRight(g, cx - cw * 0.6f, y - 18, cw * 0.6f, 36, 0, alpha(light, 0.25f * sa));
			HudDraw.gradientRight(g, cx, y - 18, cw * 0.6f, 36, alpha(light, 0.25f * sa), 0);
		}
		g.pose().popMatrix();
	}

	// ---- figures

	/**
	 * A hero drawn as the select screen draws its preview (the local player as that hero: HeroModelRenderer.preview), its
	 * feet at (cx, feet), about {@code tall} high (GUI units), turned by {@code turn}: the clip played at {@code clipAt}
	 * when the model has it (the highlight intro), else the select screen's showcase hold at {@code holdAt}.
	 */
	private static void drawHero(Minecraft mc, GuiGraphicsExtractor g, Hero hero, @Nullable String clip, float clipAt, float holdAt,
								 float cx, float feet, float tall, float turn) {
		if (mc.player == null || dev.overcraft.client.model.HeroModels.get(hero) == null) {
			return;
		}
		float size = tall / 1.8f;
		int x0 = Math.round(cx - size * 1.6f);
		int x1 = Math.round(cx + size * 1.6f);
		int y0 = Math.round(feet - size * 2.6f);
		int y1 = Math.round(Math.min(g.guiHeight(), feet + size * 0.3f));
		float centreY = (y0 + y1) / 2f;
		// HeroSelectScreen's rule: the model's middle (half its 1.8 m box) sits offsetY blocks below the frame's middle
		float offsetY = (feet - centreY) / size - 0.9f;
		float boxShift = (mc.player.getBbHeight() - 1.8f) / 2;
		HeroModelRenderer.preview(hero);
		HeroModelRenderer.previewClip = clip;
		HeroModelRenderer.previewClipAt = clipAt;
		HeroModelRenderer.previewShowcaseAt = holdAt + 100;
		try {
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0, y0, x1, y1, Math.round(size), offsetY - boxShift,
				cx - turn, centreY, mc.player);
		} finally {
			HeroModelRenderer.preview(null);
			HeroModelRenderer.previewClip = null;
			HeroModelRenderer.previewShowcaseAt = -1;
		}
	}

	/** A mob for the enemy side, as Minecraft draws one in a menu, fitted to the card's width and the figure's height. */
	private static void drawMob(Minecraft mc, GuiGraphicsExtractor g, String type, float cx, float feet, float tall, float wide) {
		if (mc.level == null) {
			return;
		}
		LivingEntity mob = MOBS.computeIfAbsent(type, k -> withId(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(k))
			.map(t -> t.create(mc.level, EntitySpawnReason.LOAD)).filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity) e)
			.orElse(null)));
		if (mob == null) {
			return;
		}
		float bh = mob.getBbHeight();
		float bw = mob.getBbWidth();
		// ours, found in play: a mob at the heroes' height looked a head taller (their models stand under 1.8 m), and the
		// dragon, wider than tall, sank into its card: it hovers over the card instead
		tall *= 0.85f;
		if (bw > bh) {
			feet -= tall * 0.35f;
		}
		// a wide mob (the dragon, 16 m) may spread past its card, as the clip's figures do (found in play: fitted to the card,
		// the dragon was a speck)
		float size = Math.min(tall / bh, wide * 2.4f / bw);
		int x0 = Math.round(cx - wide * 1.4f);
		int x1 = Math.round(cx + wide * 1.4f);
		int y0 = Math.round(feet - tall * 1.3f);
		int y1 = Math.round(Math.min(g.guiHeight(), feet + tall * 0.2f));
		float centreY = (y0 + y1) / 2f;
		float offsetY = (feet - centreY) / size - bh / 2;
		InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0, y0, x1, y1, Math.round(size), offsetY, cx - 60, centreY + 12, mob);
	}

	private static int nextId = -1000;

	/** A mob never added to the level has no id, and Minecraft's renderers read it (found in play: a crash on the zombie's card). */
	private static @Nullable LivingEntity withId(@Nullable LivingEntity mob) {
		if (mob != null) {
			mob.setId(nextId--);
		}
		return mob;
	}

	/** PLAY OF THE GAME (string 0DC5, D) for the POTG; for a highlight watched alone, HIGHLIGHT (ours). */
	private static String title() {
		return Component.translatable(potg ? "overcraft.hud.potg" : "overcraft.highlights.one").getString().toUpperCase(Locale.ROOT);
	}

	// ---- text helpers

	/** Text with the clip's light shadow (soft, dark, a few pixels down-right; no outline). */
	private static void shadowed(GuiGraphicsExtractor g, Face face, String text, float x, float baseline, float cap, int rgb, float a) {
		float d = Math.max(1, cap / 22);
		HudText.plain(g, face, text, x + d, baseline + d, cap, alpha(0x000000, 0.35f * a), Align.LEFT);
		HudText.plain(g, face, text, x + d * 2, baseline + d * 2, cap, alpha(0x000000, 0.15f * a), Align.LEFT);
		HudText.plain(g, face, text, x, baseline, cap, alpha(rgb, a), Align.LEFT);
	}

	private static void plainAt(GuiGraphicsExtractor g, Face face, String text, float x, float baseline, float cap, int rgb, float a) {
		HudText.plain(g, face, text, x, baseline, cap, alpha(rgb, a), Align.LEFT);
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
	}

	private static float clamp01(float v) {
		return Math.max(0, Math.min(1, v));
	}

	private static int alpha(int rgb, float a) {
		return Math.clamp(Math.round(a * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}

	/** Held open for the whole showing (lineup to end screen): no world input, nothing closes it but the end (no skipping). */
	public static final class Holder extends Screen {
		public Holder() {
			super(Component.translatable("overcraft.hud.potg"));
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		public boolean shouldCloseOnEsc() {
			return false;
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			return true;
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			return true;
		}

		@Override
		public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			// nothing: the world (or the replay) shows through; the look is drawn over everything (GuiPotgMixin)
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		}
	}
}
