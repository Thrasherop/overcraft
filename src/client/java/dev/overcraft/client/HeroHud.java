package dev.overcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.Ow;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.effect.Fx;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.controls.HeroOptions;
import dev.overcraft.client.hud.HudShapes;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.client.hud.KillFeedHud;
import dev.overcraft.client.mixin.ToastManagerAccessor;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Targeting;
import dev.overcraft.kiriko.KirikoStats;
import dev.overcraft.jetpackcat.JetpackCatStats;
import dev.overcraft.lucio.LucioStats;
import dev.overcraft.net.HeroSyncPayload;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.gradient;
import static dev.overcraft.client.hud.HudDraw.image;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * The hero HUD, drawn like Overwatch's (tools/reference/kiriko-reference.md, section 1, measured from gameplay at
 * 1920x1080): portrait, health and pips bottom-left; the ult meter bottom-centre; ability boxes and the two weapon
 * panels bottom-right; the crosshair, the ofuda counter under it and the Swift Step target in the middle. Both side
 * groups curve up toward the centre, as Overwatch's do.
 *
 * <p>Everything is laid out in pixels of a 1080-high screen (REF_H) and scaled to the window, so it looks the same at
 * any GUI scale. Art comes from the player's local asset pack (HeroArt, HudText's fonts); without it, plain shapes
 * and Minecraft's font stand in. In Minecraft mode only a small ult meter beside the hotbar is left.
 */
public final class HeroHud {
	private static final float REF_H = 1080;
	/** The HUD's curve: y offset = CURVE * (distance from the centre)^2, so slope 2 * CURVE * distance (2.6° at the
	 * ofuda panel, 4.4° at the kunai's, 3.5° across the pips: OWN measurements in the reference). */
	private static final float CURVE = 4.7e-5f;
	/** The reference layout's width: narrower screens (4:3, 5:4) scale the HUD down to fit it across. */
	private static final float REF_W = 1920;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int TEXT_DIM = 0xFFC8CDD3;
	private static final int SHADE = 0x73000000;
	private static final int BOX_READY_TOP = 0xF2F4F4F4;
	private static final int BOX_READY_BOTTOM = 0xF2D4D4D6;
	private static final int BOX_CAST_TOP = 0xF2E6876C;
	private static final int BOX_CAST_BOTTOM = 0xF2D9AFA3;
	private static final int BOX_COOLDOWN = 0xB3575C59;
	private static final int COOLDOWN_FILL = 0xE6FF7F2D;
	private static final int COOLDOWN_EDGE = 0xFFEAD8D7;
	private static final int FLASH = 0xFFB4FEFF;
	private static final int FLASH_BORDER = 0xFF2B58DD;
	private static final int ICON = 0xFF111111;
	private static final int BLUE = 0xFF2D96DA;
	private static final int BLUE_GLOW = 0xB30B386A;
	/** The weapon panels' line (the eighth critique: #49A5EC-#52ADF8 over a dark scene). */
	private static final int PANEL_LINE = 0xFF4DA8F0;
	private static final int ULT_TICK = 0xFFF18449;
	/** The ticks' inner ends, shading to ULT_TICK outward. */
	private static final int ULT_TICK_DARK = 0xFFC65B1E;
	/** The unfilled part of the ring: nearly nothing (the critique: "just a dark band"). */
	private static final int ULT_TRACK = 0x14FFFFFF;
	private static final int ULT_READY_DISC = 0xFFEAEAE9;
	private static final int ULT_RING_INNER = 0xFF3FCEFD;
	private static final int ULT_RING_OUTER = 0xFF2C40C5;
	private static final int AMBER = 0xFFE6AA27;
	private static final int SPENT_EDGE = 0x99C8C8C8;
	private static final int SPENT_FILL = 0x59000000;
	private static final int MOUSE_BODY = 0xFFF4F4F4;
	private static final int MOUSE_BUTTON = 0xFFE8632A;
	private static final int CYAN = 0xFF29A8E0;
	/** The portrait's border: a saturated cyan core (the owner's frames: #15D2FF-#2CC5FF; the eighth critique). */
	private static final int PORTRAIT_BORDER = 0xFF22D0FE;
	private static final int PORTRAIT_BACK = 0xFF1B2A4A;
	private static final int DIVIDER = 0xB34FA3A5;
	/** A perk waiting to be picked: Overwatch's bar orange (the owner's screenshot, #FC802D undimmed). */
	private static final int PERK_ORANGE = 0xFFFC802D;

	/** How long an ability box shows coral after it's used, and cyan once it's back (s). */
	private static final float CAST_TIME = 0.35f;
	private static final float FLASH_TIME = 0.25f;
	/** Ticks round the ult meter: a 3.3 px pitch at 42 px of radius (the critique's measurement of Overwatch's). */
	private static final int ULT_TICKS = 80;

	// state for the flashes and the ult's countdown, kept between frames
	private static float ability1Seen;
	private static float ability2Seen;
	private static long ability1ReadyAt = -1;
	private static long ability2ReadyAt = -1;
	private static long ability3ReadyAt = -1;
	private static float ability3Seen;
	private static int ofudaSeen = -1;
	private static long ofudaRefilledAt = -1;
	private static long ultStartedAt = -1;
	private static long stepCastAt = -1;
	/** The perk level last seen, for the level-up chime (0 before the first sync with a hero). */
	private static int perkLevelSeen;
	/** Swift Step's screen flash: teal at the edges and streaks rushing in, as she steps (the reference, §2). */
	private static final float STEP_FLASH = 0.45f;
	/** Below this share of her health the screen's edges go red (the wiki: "~25-30% or lower ... blood appears"). */
	private static final float LOW_HEALTH = 0.3f;
	/** Allies' markers show this far (metres: ours, about Overwatch's view of its teammates in a fight). */
	private static final double ALLY_RANGE = 40;
	/** Villagers, golems and allays at full health show only this close. */
	private static final double ALLY_NEAR = 8;
	/** Where hero-mode chat starts from the left edge, in reference pixels (Minecraft then indents 4 units). */
	private static final float CHAT_LEFT = 52;
	/** How far the ally marker's bottom sits above the top of their head, in reference pixels. Ours. */
	private static final float ALLY_LIFT = 10;
	private static final int STREAKS = 14;
	/** Swift Step's streaks rushing in from the edges (the critique: 20-30). */
	private static final int STEP_STREAKS = 26;

	private HeroHud() {
	}

	/** GUI units per reference pixel: the 1920x1080 layout fitted to the screen's height, or its width when narrower. */
	private static float scale(int guiWidth, int guiHeight) {
		return Math.min(guiHeight / REF_H, guiWidth / REF_W);
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		Hero hero = ClientHero.hero();
		if (player == null || hero == null || player.isSpectator()) {
			return;
		}
		HudShapes.ensure();
		HeroSyncPayload s = ClientHero.sync();
		track(s);
		if (!ClientHero.heroView()) {
			minecraftMode(g, s, hero);
			return;
		}
		screenEffects(g, s, player);
		float scale = scale(g.guiWidth(), g.guiHeight());
		float w = g.guiWidth() / scale;
		float h = g.guiHeight() / scale;
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		allyMarkers(g, player, w, h, delta.getGameTimeDeltaPartialTick(true));
		// Overwatch's kill feed sits where Minecraft's toasts and status-effect icons do: below any showing (toasts 32 GUI
		// units a slot; the icons' rows end 25 or 50 units down). The weakness icon hid a line's victim (found playing)
		int toastSlots = ((ToastManagerAccessor) mc.gui.toastManager()).overcraft$occupiedSlots().length();
		float below = Math.max(toastSlots * 32, effectRowsBottom(player));
		KillFeedHud.render(g, w - 36, Math.max(36, below / scale + 12));
		// the crosshair and the counter under it only in first person, as Minecraft's (none on her while she emotes)
		boolean firstPerson = mc.options.getCameraType().isFirstPerson();
		if (firstPerson) {
			crosshair(g, w / 2, h / 2);
		}
		hitMarker(g, w / 2, h / 2);
		eliminatedCaption(g, w / 2, h / 2 + 205);
		if (hero == Hero.KIRIKO) {
			if (firstPerson && HeroOptions.get().ofudaUi) {
				ofudaCounter(g, s, w / 2, h / 2 + 136);
			}
			swiftStepTarget(g, player, s, w, h, delta.getGameTimeDeltaPartialTick(true));
		}
		if (ClientHero.perkMenuOpen()) {
			perkMenu(g, hero, s, w, h);
		}
		dev.overcraft.client.comms.Pings.render(g, w, h);
		dev.overcraft.client.comms.CommsWheel.render(g, w, h);
		// the rest is laid out on a 1080-high strip along the bottom (taller screens than 16:9 have room above it)
		g.pose().translate(0, h - REF_H);
		health(g, player, hero);
		ult(g, hero, s, w / 2, 930);
		abilities(g, hero, s, w);
		weapons(g, hero, s, w);
		pickedPerks(g, hero, s, w);
		g.pose().popMatrix();
	}

	/** Notices cooldowns ending, the ofuda clip refilling and the ult starting, for the flashes and the countdown. */
	private static void track(HeroSyncPayload s) {
		long now = Util.getMillis();
		// Swift Step's hand seal is sent on top of its cooldown (Kiriko.sync): past it, she has stepped. Ready again only
		// after a cooldown, not after a seal that found no room by the ally (nothing was spent)
		float stepTotal = (float) KirikoStats.SWIFT_STEP_COOLDOWN;
		if (ability1Seen > 0 && ability1Seen <= stepTotal && s.ability1Cooldown() <= 0) {
			ability1ReadyAt = now;
		}
		if (ability1Seen > stepTotal && s.ability1Cooldown() > 0 && s.ability1Cooldown() <= stepTotal) {
			stepCastAt = now;
		}
		if (ability2Seen > 0 && s.ability2Cooldown() <= 0) {
			ability2ReadyAt = now;
		}
		if (ability3Seen > 0 && s.ability3Cooldown() <= 0) {
			ability3ReadyAt = now;
		}
		ability1Seen = s.ability1Cooldown();
		ability2Seen = s.ability2Cooldown();
		ability3Seen = s.ability3Cooldown();
		if (ofudaSeen >= 0 && s.secondaryAmmo() > ofudaSeen && s.secondaryAmmo() == KirikoStats.OFUDA_AMMO) {
			ofudaRefilledAt = now;
		}
		ofudaSeen = s.secondaryAmmo();
		if (!s.ultActive()) {
			ultStartedAt = -1;
		} else if (ultStartedAt < 0) {
			ultStartedAt = now;
		}
		// a perk level reached: a chime (ours: Aftermath describes "a subtle chime"; Overwatch's isn't extracted)
		if (!s.heroId().isEmpty()) {
			if (perkLevelSeen > 0 && s.perkLevel() > perkLevelSeen) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1));
			}
			perkLevelSeen = s.perkLevel();
		}
	}

	private static float since(long at) {
		return at < 0 ? Float.MAX_VALUE : (Util.getMillis() - at) / 1000f;
	}

	// ---- the screen itself ----

	/**
	 * Tints over the whole view (the reference, §2): stepping, the edges flash teal and streaks rush in; in Kitsune
	 * Rush's path, a white-cyan mist at the edges and light streaks flying past. Low on health, the edges glow red.
	 */
	private static void screenEffects(GuiGraphicsExtractor g, HeroSyncPayload s, LocalPlayer player) {
		int w = g.guiWidth();
		int h = g.guiHeight();
		float health = player.getHealth() / Math.max(1, player.getMaxHealth());
		if (health > 0 && health < LOW_HEALTH) {
			// stronger the lower it gets, pulsing slowly (the critique, from Overwatch at 27 of 225: "the screen edges
			// glow strong red")
			float low = 1 - health / LOW_HEALTH;
			float pulse = 0.06f * (float) Math.sin(Util.getMillis() / 1000f * Mth.TWO_PI * 0.8f);
			// darkened first, then crimson smoke over it, so it stays red over a bright sky (the third critique)
			float strength = 0.6f + 0.35f * low + pulse;
			image(g, HudShapes.LOW_HEALTH, 0, 0, w, h, alpha(0x000000, 0.55f * strength));
			image(g, HudShapes.LOW_HEALTH, 0, 0, w, h, alpha(0x8C1420, 0.85f * strength));
		}
		// the view's own effects belong to the first-person view (in third person they tinted the whole frame)
		boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();
		float step = since(stepCastAt);
		if (firstPerson && step < STEP_FLASH) {
			// teal at the edges only and streaks rushing in (the second critique: the centre stays readable)
			float k = 1 - step / STEP_FLASH;
			// dark mottled teal smoke in the corners (Overwatch's mid-step frame, the third critique), then the streaks
			image(g, HudShapes.LOW_HEALTH, 0, 0, w, h, alpha(0x000000, 0.35f * k));
			image(g, HudShapes.LOW_HEALTH, 0, 0, w, h, alpha(0x0A8A84, 0.8f * k));
			java.util.Random random = new java.util.Random(stepCastAt);
			for (int i = 0; i < STEP_STREAKS; i++) {
				float angle = random.nextFloat() * Mth.TWO_PI;
				float travel = Math.clamp(step / STEP_FLASH + random.nextFloat() * 0.2f, 0, 1);
				streak(g, w, h, angle, 1.05f - 0.45f * travel, false, alpha(0x9AFFF4, 0.75f * k));
			}
		}
	}

	/** The last hit marker: when (the effects' game clock, Fx.now), and what (0 hit, 1 critical, 2 kill). */
	private static float hitAt = -100;
	private static int hitKind;

	/** The last final blow's caption: whom, and when (the effects' clock). */
	private static String eliminated = "";
	private static float eliminatedAt = -100;
	/** How long "ELIMINATED <name>" shows, and its fade (seconds: ours). */
	private static final float ELIMINATED_LIFE = 1.6f;
	private static final float ELIMINATED_FADE = 0.4f;

	/** The server says one of our attacks landed (Effects.hitMarker); on a kill, target is who fell. */
	public static void hitMarker(int kind, int target) {
		if (kind == 2) {
			net.minecraft.world.entity.Entity victim = Minecraft.getInstance().level == null ? null
				: Minecraft.getInstance().level.getEntity(target);
			if (victim != null) {
				eliminated = victim.getName().getString().toUpperCase(Locale.ROOT);
				eliminatedAt = Fx.now();
			}
		}
		hitMarker(kind);
	}

	private static void hitMarker(int kind) {
		// a kill outranks the hit that made it, if both arrive together
		if (Fx.now() - hitAt > 0.05f || kind >= hitKind) {
			hitKind = kind;
		}
		hitAt = Fx.now();
	}

	/**
	 * Overwatch's hit marker: four short strokes in an X round the crosshair, flashing on each hit and fading over a
	 * fifth of a second; red and a little bigger on a critical hit, bigger again on a kill (the shape, sizes and colours
	 * are ours, after the game's: no reference measures them).
	 */
	/**
	 * Overwatch's final-blow notice under the crosshair, below the ofuda counter: "ELIMINATED" and who, for a moment
	 * (the fifth critique; its size and place are ours, no reference frame shows one).
	 */
	private static void eliminatedCaption(GuiGraphicsExtractor g, float cx, float y) {
		float t = age(eliminatedAt);
		if (t >= ELIMINATED_LIFE || eliminated.isEmpty()) {
			return;
		}
		float a = Math.min(1, (ELIMINATED_LIFE - t) / ELIMINATED_FADE);
		String label = Component.translatable("overcraft.hud.eliminated").getString().toUpperCase(Locale.ROOT) + " ";
		float cap = 13;
		float labelW = HudText.width(Face.BODY, label, cap);
		float total = labelW + HudText.width(Face.BODY, eliminated, cap);
		float left = cx - total / 2;
		HudText.draw(g, Face.BODY, label, left, y, cap, alpha(0xFFFFFF, a), Align.LEFT);
		HudText.draw(g, Face.BODY, eliminated, left + labelW, y, cap, alpha(0xF0505A, a), Align.LEFT);
	}

	/**
	 * Seconds since a moment on the effects' clock: a hair below zero reads as zero (the frame's partial tick can be
	 * behind the one it arrived on); well below zero it's from another world's clock, and long past. Clamped to zero,
	 * a kill in one world kept "ELIMINATED PIG" on screen for good in a younger one (found playing).
	 */
	private static float age(float at) {
		float t = Fx.now() - at;
		return t < -0.5f ? Float.MAX_VALUE : Math.max(0, t);
	}

	/** World left: no hit marker or final-blow caption carries into the next. */
	public static void clearMoments() {
		hitAt = -100;
		eliminatedAt = -100;
		eliminated = "";
	}

	private static void hitMarker(GuiGraphicsExtractor g, float cx, float cy) {
		float t = age(hitAt);
		float life = hitKind == 2 ? 0.45f : 0.2f;
		if (t >= life) {
			return;
		}
		float k = 1 - t / life;
		int color = hitKind == 0 ? 0xFFFFFF : 0xFF3A3A;
		float inner = hitKind == 2 ? 11 : 8;
		float len = hitKind == 0 ? 8 : hitKind == 1 ? 10 : 14;
		float thick = hitKind == 2 ? 3 : 2.2f;
		for (int i = 0; i < 4; i++) {
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			g.pose().rotate((float) Math.toRadians(45 + 90 * i));
			fill(g, inner, -thick / 2 - 1, len + 2, thick + 2, alpha(0x000000, 0.45f * k));
			fill(g, inner + 1, -thick / 2, len, thick, alpha(color, k));
			g.pose().popMatrix();
		}
	}

	/** The local player stood in an ally Kiriko's Kitsune Rush path at this time (any player, hero or not). */
	private static long inRushAt = -1;

	/** The server's speed lines on an ally in the rush path named the local player. */
	public static void markInRush() {
		inRushAt = Util.getMillis();
	}

	/**
	 * In a Kitsune Rush corridor now: the server marks the players in it every 0.15 s (rush_ally), so this follows the
	 * corridor itself, about 0.2 s behind as in Overwatch (not the speed buff, which lingers 1 s after leaving).
	 */
	public static boolean inRushPath() {
		return since(inRushAt) < 0.3f;
	}

	/**
	 * In Kitsune Rush's path, a white-cyan mist at the screen's edges and light streaks flying past (the reference, §2):
	 * for every player in it, Kiriko's allies too, not only heroes (the second critique: the ally saw nothing). Its own
	 * HUD element, drawn whatever HUD the player has.
	 */
	public static void rushView(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
			return;
		}
		boolean hero = ClientHero.hero() != null && ClientHero.sync().rushed();
		if (!hero && since(inRushAt) > 0.4f) {
			return;
		}
		// a player with no hero has no hero HUD to have made the masks
		HudShapes.ensure();
		int w = g.guiWidth();
		int h = g.guiHeight();
		{
			float time = Util.getMillis() / 1000f;
			float pulse = 0.5f + 0.08f * (float) Math.sin(time * Mth.TWO_PI * 0.7f);
			// lighter: at half strength over grass it tinted the whole view mint (the sixth critique). In the dark, cyan:
			// the pale one read as grey fog at night (the seventh, #7FE6F5 at the edge)
			float light = Math.clamp((mc.level.getMaxLocalRawBrightness(mc.player.blockPosition()) / 15f - 0.2f) / 0.6f, 0, 1);
			int mist = ARGB.srgbLerp(light, 0x7FE6F5, 0xD2F4FF) & 0xFFFFFF;
			image(g, HudShapes.VIGNETTE, 0, 0, w, h, alpha(mist, pulse * 0.6f));
			for (int i = 0; i < STREAKS / 2; i++) {
				float phase = (time * 1.6f + i * 0.37f) % 1;
				float angle = (i * 2.399963f + (int) (time * 1.6f + i * 0.37f) * 1.7f) % Mth.TWO_PI;
				float fade = (float) Math.sin(phase * Math.PI);
				streak(g, w, h, angle, 0.45f + 0.65f * phase, true, alpha(0xBDF6FF, 0.45f * fade));
			}
		}
	}

	/** A streak at {@code radius} (of half the screen's diagonal) out along {@code angle}, head outward or inward. */
	private static void streak(GuiGraphicsExtractor g, int w, int h, float angle, float radius, boolean outward, int color) {
		float half = (float) Math.hypot(w, h) / 2;
		float length = 0.28f * half;
		float thickness = length * HudShapes.STREAK_W / HudShapes.STREAK_H * 0.6f;
		float x = w / 2f + (float) Math.cos(angle) * radius * half;
		float y = h / 2f + (float) Math.sin(angle) * radius * half;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		// the texture's head is toward +y: turn +y onto the direction of travel
		g.pose().rotate(angle + (outward ? -Mth.HALF_PI : Mth.HALF_PI));
		image(g, HudShapes.STREAK, -thickness / 2, -length * 0.8f, thickness, length, color);
		g.pose().popMatrix();
	}

	private static int alpha(int rgb, float a) {
		return Math.clamp(Math.round(a * 255), 0, 255) << 24 | (rgb & 0xFFFFFF);
	}

	// ---- bottom left: portrait, health, pips ----

	private static void health(GuiGraphicsExtractor g, LocalPlayer player, Hero hero) {
		// the portrait, cut to a shield with a light-blue border
		Identifier portrait = HudShapes.portrait(hero);
		// the gable's peak under the head near y 902, the point at 1015 (the owner's frames, the eighth critique: 6% larger
		// than before, so only a few pixels of gable show beside the mask's ears)
		float pw = 110;
		float px = 49;
		float py = 902;
		float ph = pw * HudShapes.SHIELD_H / HudShapes.SHIELD_W;
		boolean onFire = ClientHero.sync().onFire();
		if (onFire) {
			image(g, HudShapes.GLOW, px - pw * 0.6f, py - ph * 0.5f, pw * 2.2f, ph * 2, alpha(0xFF6A1A, 0.7f));
		}
		image(g, HudShapes.SHIELD, px, py, pw, ph, PORTRAIT_BACK);
		if (portrait != null) {
			image(g, portrait, px, py, pw, ph, WHITE);
		}
		// the border glows (the eighth critique): a saturated cyan core, a soft blue fading out over about 5 px, a deep
		// blue fading in over 3; no dark edge
		for (int i = 5; i >= 1; i--) {
			image(g, HudShapes.SHIELD_BORDER, px - i, py - i, pw + 2 * i, ph + 2 * i,
				alpha(onFire ? 0xFF9A3C : 0x1B82CA, 0.32f * (1 - i / 6f)));
		}
		for (int i = 1; i <= 3; i++) {
			image(g, HudShapes.SHIELD_BORDER, px + i, py + i, pw - 2 * i, ph - 2 * i, alpha(0x0270BB, 0.45f * (1 - i / 4f)));
		}
		image(g, HudShapes.SHIELD_BORDER, px, py, pw, ph, onFire ? 0xFFFF9A3C : PORTRAIT_BORDER);
		if (onFire) {
			flames(g, px, py, pw, ph);
		}
		// the hair breaking out over the frame's top (Overwatch's)
		Identifier pop = HudShapes.portraitPop(hero);
		if (pop != null) {
			float texel = pw / (HudShapes.SHIELD_W * 2f);
			image(g, pop, px, py - HudShapes.POP_H * texel, pw, (HudShapes.POP_H + HudShapes.POP_BELOW) * texel, WHITE);
		}

		// the rest tilts with the HUD's curve: inner (right) end higher. Its left edge at x 168 (the owner's frames, the
		// eighth critique: 26 px further left than before)
		float left = 168;
		g.pose().pushMatrix();
		curve(g, left, 933, slopeAt(left + 120 - 960));
		int max = Math.round(Ow.toOw(player.getMaxHealth()));
		int now = (int) Math.ceil(Ow.toOw(player.getHealth()));
		// "+" box: at full health a white rounded square with a dark "+" and a dark tail; hurt, all dark with a white "+"
		// (every full-health frame of the owner's is white, every hurt one dark)
		if (now >= max) {
			box(g, left - 2, 899, 26, 26, 0xFFF4F4F4);
			image(g, HudShapes.PLUS, left + 2, 903, 18, 18, 0xFF222222);
			box(g, left + 24, 899, 18, 26, SHADE);
		} else {
			box(g, left - 2, 899, 44, 26, SHADE);
			image(g, HudShapes.PLUS, left + 2, 903, 18, 18, WHITE);
		}
		float x = left + 54;
		String current = String.valueOf(now);
		HudText.draw(g, Face.BODY, current, x, 918, 18.8f, WHITE, Align.LEFT);
		x += HudText.width(Face.BODY, current, 18.8f) + 7;
		fill(g, x, 896, 2, 22, DIVIDER);
		HudText.draw(g, Face.BODY, String.valueOf(max), x + 8, 918, 15, TEXT_DIM, Align.LEFT);

		// one pip per 25 health, white while it holds health, a faint dark box when empty. The row is a fixed length: the
		// pips narrow as health grows (Overwatch's 10 at a 24.8 pitch, 9 at 27.3: the eighth critique)
		int pips = Math.max(1, (int) Math.ceil(max / 25.0));
		float pitch = PIP_ROW / pips;
		float pipW = pitch - 4;
		float rowEnd = left + 3 + PIP_ROW - 4;
		for (int i = 0; i < pips; i++) {
			float pipX = left + 3 + i * pitch;
			float held = Math.clamp((now - i * 25) / 25f, 0, 1);
			box(g, pipX, 933, pipW, 30, 0x40000000);
			if (held > 0) {
				// a nearly full pip reads as full, like Overwatch's (199 shows 8 whole pips); a partial one fills from
				// the left (27 health shows one pip and a sliver)
				float shown = held > 0.9f ? 1 : held;
				box(g, pipX, 933, pipW * shown, 30, WHITE);
			}
		}

		// the player's slot in a tall light-blue box (Overwatch's party number, round 1 #28: #41CCFD; 30 x 44 with a thin
		// pale digit, the seventh critique), then the name plate: dark, with a glowing blue underline from the box,
		// ending in a dot
		// The box holds the perk level (#36: Blizzard calls the line under the name a "level bar"; the number at its start
		// being the level is our reading of it)
		HeroSyncPayload s = ClientHero.sync();
		if (s.perkReady() > 0) {
			// a perk waiting: the whole row becomes Overwatch's orange "[LALT] SELECT PERK" bar, from the level box's left
			// edge to the pips' right end (the owner's screenshot, 2026-10-27)
			selectPerkBar(g, left - 2, 968, rowEnd - (left - 2));
			g.pose().popMatrix();
			return;
		}
		// the level box, 31 x 30 in Overwatch's #3AD5FE with a clear white digit, then the plate: a uniform translucent
		// slab (it darkens what's behind by about a quarter) to the pips' end, the name in light grey (the eighth critique)
		fill(g, left - 2, 970, 31, 30, 0xFF3AD5FE);
		HudText.draw(g, Face.BODY, String.valueOf(s.perkLevel()), left + 13.5f, 991.5f, 13, 0xE6FFFFFF, Align.CENTER);
		float plate = left + 29;
		fill(g, plate, 968, rowEnd - plate, 32, 0x47000000);
		String name = player.getName().getString().toUpperCase(Locale.ROOT);
		HudText.draw(g, Face.BODY, name, plate + 14, 991, 14, 0xFFC9CFD6, Align.LEFT);
		levelBar(g, s, left - 2, 1000, rowEnd - (left - 2));
		g.pose().popMatrix();
	}

	/** The pip row's length, whatever the health (the eighth critique: 10 pips at a 24.8 pitch, 9 at 27.3). */
	private static final float PIP_ROW = 248;

	/**
	 * Overwatch's perk-waiting bar (the owner's screenshot, 2026-10-27, measured: 32 px tall, #FC802D undimmed), the
	 * perk key in a near-black box with orange letters, then "SELECT PERK" in near-black caps, centred.
	 */
	private static void selectPerkBar(GuiGraphicsExtractor g, float x, float y, float width) {
		fill(g, x, y, width, 32, PERK_ORANGE);
		String key = shortKey(HeroControls.get(Action.PERK_MENU));
		String label = Component.translatable("overcraft.hud.select_perk").getString().toUpperCase(Locale.ROOT);
		float cap = 11.5f;
		float keyW = HudText.width(Face.SMALL, key, 10.5f) + 12;
		float labelW = HudText.width(Face.BODY, label, cap);
		float start = x + (width - keyW - 8 - labelW) / 2;
		fill(g, start, y + 6, keyW, 20, 0xF01C0000);
		HudText.draw(g, Face.SMALL, key, start + keyW / 2, y + 16 + 5.25f, 10.5f, PERK_ORANGE, Align.CENTER);
		HudText.draw(g, Face.BODY, label, start + keyW + 8, y + 16 + cap / 2, cap, 0xFF1C0000, Align.LEFT);
	}

	/**
	 * The perk level bar along the name plate's foot (#36): blue filling toward the next level with a bright dot at its
	 * end (third-party descriptions: "a blue bar slowly filling"; the owner's screenshots show it); full once both
	 * perks are picked. A perk waiting replaces the whole row (selectPerkBar).
	 */
	private static void levelBar(GuiGraphicsExtractor g, HeroSyncPayload s, float x, float y, float length) {
		// no track: only the filled part and its glowing end; at no progress just the dot at the box's corner, with a
		// short bright edge along the box's foot (the owner's frames, the eighth critique)
		float filled = length * Math.clamp(s.perkProgress(), 0, 1);
		fill(g, x, y - 1, Math.max(filled, 18), 2, alpha(0x6FD3FF, 0.85f));
		if (filled > 0) {
			fill(g, x, y - 1, filled, 3, BLUE);
		}
		image(g, HudShapes.GLOW, x + filled - 9, y - 9, 18, 18, 0xFF6FD3FF);
	}

	/**
	 * The perk menu, up while the perk key is held and a perk waits (#36), picked with the left or right mouse button
	 * (Overwatch's: "Hold down Left Alt ..., then use Left/Right click"). Overwatch's layout (the owner's screenshot,
	 * 2026-10-27, 2000 x 1125, measured and scaled to 1080): the two perks as black discs under the crosshair, about
	 * 141 px apart on a dark band, an orange bracket arc round each one's outer side and its mouse button under it; each
	 * perk's name (orange caps) and text (white) in a navy box beside its disc.
	 */
	private static void perkMenu(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float w, float h) {
		Perk.Tier tier = s.perkReady() == 1 ? Perk.Tier.MINOR : Perk.Tier.MAJOR;
		java.util.List<Perk> choices = Perk.of(hero, tier);
		float cy = 754;
		float r = 50;
		float apart = 141;
		// the band the discs sit on
		fill(g, w / 2 - apart / 2, cy - 28, apart, 56, 0xD9121519);
		for (int i = 0; i < choices.size() && i < 2; i++) {
			Perk perk = choices.get(i);
			boolean left = i == 0;
			float cx = w / 2 + (left ? -apart / 2 : apart / 2);
			disc(g, cx, cy, r, 0xF20D0F12);
			image(g, HudShapes.RING_THIN, cx - r, cy - r, 2 * r, 2 * r, 0x59FFFFFF);
			Identifier icon = HeroArt.perk(hero, perk.id);
			if (icon != null) {
				image(g, icon, cx - 29, cy - 29, 58, 58, WHITE);
			}
			// the orange bracket round the outer side, about 100 degrees of it
			arc(g, cx, cy, r + 9, 7, left ? 220 : 40, left ? 320 : 140, PERK_ARC);
			// its mouse button under it, that button orange
			float mw = 19;
			float mh = mw * HudShapes.MOUSE_H / HudShapes.MOUSE_W;
			float my = 823;
			image(g, HudShapes.MOUSE_OUTLINE, cx - mw / 2, my - mh / 2, mw, mh, 0xFF111111);
			image(g, HudShapes.MOUSE, cx - mw / 2, my - mh / 2, mw, mh, MOUSE_BODY);
			image(g, left ? HudShapes.MOUSE_LEFT : HudShapes.MOUSE_RIGHT, cx - mw / 2, my - mh / 2, mw, mh, MOUSE_BUTTON);
			// the name and text in a navy box beside it, 154 px out from the middle
			float boxW = 369;
			float boxX = left ? w / 2 - 154 - boxW : w / 2 + 154;
			float boxY = 696;
			java.util.List<String> lines = wrap(Component.translatable("overcraft.perk." + hero.id + "." + perk.id + ".desc")
				.getString(), boxW - 40, 13);
			float boxH = Math.max(118, 52 + lines.size() * 24 + 8);
			fill(g, boxX, boxY, boxW, boxH, 0xE0172036);
			String name = Component.translatable("overcraft.perk." + hero.id + "." + perk.id).getString().toUpperCase(Locale.ROOT);
			HudText.draw(g, Face.BODY, name, boxX + 20, boxY + 28, 12.5f, PERK_TITLE, Align.LEFT);
			float lineY = boxY + 55;
			for (String line : lines) {
				HudText.draw(g, Face.SMALL, line, boxX + 20, lineY, 13, WHITE, Align.LEFT);
				lineY += 24;
			}
		}
	}

	/** Overwatch's perk menu orange: the brackets round the discs, and the perks' names (the owner's screenshot). */
	private static final int PERK_ARC = 0xFFF0781E;
	private static final int PERK_TITLE = 0xFFF5962E;

	/**
	 * An arc of the circle round (cx, cy), radius r to its middle, thick across, from one angle to another (degrees,
	 * clockwise from 12 o'clock), as short overlapping soft-edged ticks.
	 */
	private static void arc(GuiGraphicsExtractor g, float cx, float cy, float r, float thick, float from, float to, int color) {
		int segments = Math.max(8, Math.round((to - from) / 2));
		float step = (to - from) / segments;
		float len = (float) (2 * Math.PI * r * step / 360) * 1.6f;
		float quadW = len / HudShapes.TICK_FILL_W;
		float quadH = thick / HudShapes.TICK_FILL_H;
		for (int i = 0; i <= segments; i++) {
			// each tick points along the radius, as thick as the arc and a little longer along it than its step
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			g.pose().rotate((float) Math.toRadians(from + i * step));
			image(g, HudShapes.TICK, -quadW / 2, -r - quadH / 2, quadW, quadH, color);
			g.pose().popMatrix();
		}
	}

	/** Words into lines no wider than {@code width} in the small face. */
	private static java.util.List<String> wrap(String text, float width, float cap) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String next = line.isEmpty() ? word : line + " " + word;
			if (!line.isEmpty() && HudText.width(Face.SMALL, next, cap) > width) {
				lines.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(next);
			}
		}
		if (!line.isEmpty()) {
			lines.add(line.toString());
		}
		return lines;
	}

	/**
	 * The picked perks: white discs with the perk's glyph in black, above the weapon panels' right end (the reference
	 * video: "white discs about 0.025 W with dark glyphs ... y 0.80-0.84"), minor then major.
	 */
	private static void pickedPerks(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float w) {
		Perk[] picked = {Perk.byIndex(s.minorPerk()), Perk.byIndex(s.majorPerk())};
		float size = 44;
		float cx = w - 140;
		float cy = 884;
		for (Perk perk : picked) {
			if (perk == null) {
				continue;
			}
			g.pose().pushMatrix();
			curve(g, cx, cy, slopeAt(960 - (w - cx)));
			image(g, HudShapes.DISC, cx - size / 2, cy - size / 2, size, size, 0xF2F4F4F4);
			Identifier icon = HeroArt.perk(hero, perk.id);
			if (icon != null) {
				image(g, icon, cx - size * 0.36f, cy - size * 0.36f, size * 0.72f, size * 0.72f, ICON);
			} else {
				HudText.draw(g, Face.SMALL, perk.tier == Perk.Tier.MINOR ? "2" : "3", cx, cy + 6, 12, ICON, Align.CENTER);
			}
			g.pose().popMatrix();
			cx += size + 10;
		}
	}

	/**
	 * On Fire: the portrait burns, as Overwatch's does (#13): an orange glow behind it, its border orange, and flames
	 * licking up its sides and over its top, flickering (ours: Overwatch's is an animated fire texture, not in the pack;
	 * the first try's flames drifted off above it, too faint to see).
	 */
	private static void flames(GuiGraphicsExtractor g, float px, float py, float pw, float ph) {
		float t = Util.getMillis() / 1000f;
		for (int i = 0; i < 12; i++) {
			float phase = (t * (1.6f + (i % 4) * 0.23f) + i * 0.37f) % 1;
			// along the frame's edge, from low on its sides to its top
			float along = (i * 0.618f) % 1;
			float side = along < 0.5f ? -1 : 1;
			float k = along < 0.5f ? along * 2 : (along - 0.5f) * 2;
			float x = px + pw / 2 + side * pw * 0.5f * (1 - 0.6f * k) + Mth.sin(t * 5 + i) * 2;
			float y = py + ph * (0.55f - 0.55f * k) - phase * ph * 0.3f;
			float size = pw * (0.32f - 0.18f * phase);
			float fade = Mth.sin(phase * Mth.PI);
			image(g, HudShapes.GLOW, x - size / 2, y - size * 0.75f, size, size * 1.5f, alpha(phase < 0.4f ? 0xFFD060 : 0xFF6A1A, 0.8f * fade));
		}
	}

	// ---- bottom centre: the ult meter ----

	private static void ult(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float cx, float cy) {
		// casting empties the meter at once (the reference: just used, it shows 0 with no ticks)
		float charge = s.ultActive() ? 0 : Math.min(1, s.ultCharge() / Math.max(1, s.ultCost()));
		boolean ready = charge >= 1;
		if (ready) {
			// a white disc with the fox in a thick electric-blue ring (darker outside), a soft blue halo, and small
			// white triangles at 3 and 9 o'clock pointing in
			image(g, HudShapes.SOFT_DISC, cx - 80, cy - 80, 160, 160, 0xE62C40C5);
			disc(g, cx, cy, 57, ULT_RING_OUTER);
			disc(g, cx, cy, 55, 0xFF3355E7);
			disc(g, cx, cy, 53, 0xFF36ACFF);
			disc(g, cx, cy, 51, ULT_RING_INNER);
			disc(g, cx, cy, 47, ULT_READY_DISC);
			Identifier fox = HeroArt.ability(hero, switch (hero) {
				case LUCIO -> "sound_barrier";
				case JETPACK_CAT -> "catnapper";
				case MERCY -> "valkyrie";
				default -> "kitsune_rush";
			});
			if (fox != null) {
				image(g, fox, cx - 30, cy - 30, 60, 60, ICON);
			}
			for (int side = -1; side <= 1; side += 2) {
				g.pose().pushMatrix();
				g.pose().translate(cx + side * 62, cy);
				g.pose().rotate(side < 0 ? 0 : Mth.PI);
				image(g, HudShapes.TRIANGLE, -6, -6, 12, 12, WHITE);
				g.pose().popMatrix();
			}
		} else {
			// see-through, as Overwatch's (the second critique, measured: the middle barely darker than what's behind,
			// the tick band about 40% darker): a fine dark outer circle, a brownish inner one, the orange ticks
			disc(g, cx, cy, 37, 0x1F000000);
			image(g, HudShapes.BAND_RING, cx - 47, cy - 47, 94, 94, 0x66000000);
			image(g, HudShapes.RING_THIN, cx - 56, cy - 56, 112, 112, 0xFF4A4F4F);
			innerRing(g, cx, cy);
			// lit ticks a little see-through, as Overwatch's (about 70%: the eighth critique)
			ticks(g, cx, cy, 37, 47, charge, alpha(ULT_TICK_DARK, 0.72f), alpha(ULT_TICK, 0.72f), 0);
			int percent = (int) Math.floor(100 * charge);
			HudText.outlined(g, Face.NUMBERS, String.valueOf(percent), cx, cy + 18, 36, 0xFFE6E8EE, 0xB31C2233, 1.5f,
				Align.CENTER);
		}
		keyLabel(g, HeroControls.get(Action.ULTIMATE), cx, 1008, 0, ready);
	}

	/**
	 * The ult meter's inner ring (the eighth critique, the owner's frames): an orange line about 3 px wide at r 33, half
	 * see-through, with two short gaps (8°) opposite each other that turn slowly (one turn in 10 s: ours; each frame has
	 * them somewhere else).
	 */
	private static void innerRing(GuiGraphicsExtractor g, float cx, float cy) {
		int segments = 180;
		float r = 33;
		float gap = (Util.getMillis() % 10000) / 10000f * 360;
		float arc = (float) (2 * Math.PI * r / segments) * 2.2f;
		float quadW = arc / HudShapes.TICK_FILL_W;
		float quadH = 3 / HudShapes.TICK_FILL_H;
		for (int i = 0; i < segments; i++) {
			float angle = i * 360f / segments;
			float off = Math.abs(((angle - gap) % 180 + 180) % 180);
			if (off < 4 || off > 176) {
				continue;
			}
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			g.pose().rotate((float) Math.toRadians(angle));
			image(g, HudShapes.TICK, -quadW / 2, -r - quadH / 2, quadW, quadH, 0x80E0703F);
			g.pose().popMatrix();
		}
	}

	/**
	 * A ring of radial ticks, clockwise from 12 o'clock: the first fraction lit (shading from inner to outer), the rest
	 * in the track colour. Each tick is a soft-edged texture, so it stays smooth at every angle.
	 */
	private static void ticks(GuiGraphicsExtractor g, float cx, float cy, float r0, float r1, float fraction, int inner,
							  int outer, int track) {
		int litTicks = Math.round(ULT_TICKS * fraction);
		float mid = (r0 + r1) / 2;
		// 70% of the pitch: 2.3 px wide at Overwatch's size
		float width = (float) (2 * Math.PI * mid / ULT_TICKS) * 0.7f;
		float quadW = width / HudShapes.TICK_FILL_W;
		float quadH = (r1 - r0) / HudShapes.TICK_FILL_H;
		for (int i = 0; i < ULT_TICKS; i++) {
			boolean lit = i < litTicks;
			if (!lit && track == 0) {
				continue;
			}
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			g.pose().rotate((float) (2 * Math.PI * (i + 0.5) / ULT_TICKS));
			image(g, HudShapes.TICK, -quadW / 2, -mid - quadH / 2, quadW, quadH, lit ? inner : track);
			if (lit && outer != inner) {
				image(g, HudShapes.TICK_OUTER, -quadW / 2, -mid - quadH / 2, quadW, quadH, outer);
			}
			g.pose().popMatrix();
		}
	}

	// ---- bottom right: abilities and weapons ----

	private static void abilities(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float w) {
		if (hero == Hero.MERCY) {
			// Guardian Angel (Shift), Resurrect (E), Flash Heal (R: its charges in the corner with Double Dose), in Lúcio's
			// three places (ours)
			ability(g, hero, w, "guardian_angel", w - 719, 900, HeroControls.get(Action.ABILITY_1), s.ability1Cooldown(),
				(float) dev.overcraft.mercy.MercyStats.GA_COOLDOWN, ability1ReadyAt);
			ability(g, hero, w, "resurrect", w - 641, 903, HeroControls.get(Action.ABILITY_2), s.ability2Cooldown(),
				(float) dev.overcraft.mercy.MercyStats.RES_COOLDOWN, ability2ReadyAt);
			float flash = s.secondaryAmmo() > 0 ? 0 : s.ability3Cooldown();
			ability(g, hero, w, "flash_heal", w - 563, 906, HeroControls.get(Action.RELOAD), flash,
				(float) dev.overcraft.mercy.MercyStats.FLASH_COOLDOWN, ability3ReadyAt);
			if (s.secondaryAmmo() > 1 || s.secondaryAmmo() == 1 && s.ability3Cooldown() > 0) {
				charges(g, s.secondaryAmmo(), w - 563, 906, w);
			}
			return;
		}
		if (hero == Hero.JETPACK_CAT) {
			// Frenetic Flight (the right button) with its energy along the foot of its box, Lifeline (Shift: marked while
			// she tows), Purr (E); in Lúcio's three places (ours)
			ability(g, hero, w, "frenetic_flight", w - 719, 900, HeroControls.get(Action.PRIMARY_FIRE), 0, 1, -1);
			energy(g, s.meter(), w - 719, 900, w);
			ability(g, hero, w, "lifeline", w - 641, 903, HeroControls.get(Action.ABILITY_1), s.ability1Cooldown(),
				(float) JetpackCatStats.LIFELINE_COOLDOWN, ability1ReadyAt);
			if ((s.mode() & dev.overcraft.jetpackcat.JetpackCat.MODE_TOWING) != 0) {
				song(g, false, false, w - 641, 903, w);
			}
			ability(g, hero, w, "purr", w - 563, 906, HeroControls.get(Action.ABILITY_2), s.ability2Cooldown(),
				(float) JetpackCatStats.PURR_COOLDOWN, ability2ReadyAt);
			return;
		}
		if (hero == Hero.LUCIO) {
			// Soundwave (the right button), Crossfade (Shift: the song playing marked on it), Amp It Up (E: coral while it
			// runs, then its cooldown), the boxes where Kiriko's two are and one more to their left (ours)
			ability(g, hero, w, "soundwave", w - 719, 900, HeroControls.get(Action.PRIMARY_FIRE), s.ability3Cooldown(),
				(float) LucioStats.WAVE_COOLDOWN, ability3ReadyAt);
			ability(g, hero, w, "crossfade", w - 641, 903, HeroControls.get(Action.ABILITY_1), 0, 1, -1);
			song(g, s.mode() == 1, s.ability1Cooldown() > 0, w - 641, 903, w);
			float amp = s.activeLeft() > 0 ? (float) LucioStats.AMP_UP_COOLDOWN : s.ability2Cooldown();
			ability(g, hero, w, "amp_it_up", w - 563, 906, HeroControls.get(Action.ABILITY_2), amp,
				(float) LucioStats.AMP_UP_COOLDOWN, ability2ReadyAt);
			return;
		}
		ability(g, hero, w, "swift_step", w - 641, 903, HeroControls.get(Action.ABILITY_1), s.ability1Cooldown(),
			(float) KirikoStats.SWIFT_STEP_COOLDOWN, ability1ReadyAt);
		ability(g, hero, w, "protection_suzu", w - 563, 906, HeroControls.get(Action.ABILITY_2), s.ability2Cooldown(),
			(float) KirikoStats.SUZU_COOLDOWN, ability2ReadyAt);
	}

	/**
	 * Frenetic Flight's energy (0-1) on its box: a band along the foot as long as what's left, and the percentage; red
	 * under 20 (her data's "low energy"), the colours ours.
	 */
	private static void energy(GuiGraphicsExtractor g, float energy, float x, float base, float w) {
		float cx = x + 32;
		g.pose().pushMatrix();
		curve(g, cx, base + 36, slopeAt(960 - (w - cx)));
		int colour = energy < 0.2f ? 0xFFFF4A4A : 0xFFF0A23C;
		fill(g, x + 4, base + 60, 56, 6, 0x66000000);
		fill(g, x + 4, base + 60, 56 * Math.clamp(energy, 0, 1), 6, colour);
		HudText.draw(g, Face.BODY, Math.round(energy * 100) + "%", x + 50, base + 22, 13, colour, Align.CENTER);
		g.pose().popMatrix();
	}

	/** Charges left on an ability's box (Flash Heal with Double Dose): the count in its corner (ours). */
	private static void charges(GuiGraphicsExtractor g, int count, float x, float base, float w) {
		float cx = x + 32;
		g.pose().pushMatrix();
		curve(g, cx, base + 36, slopeAt(960 - (w - cx)));
		HudText.draw(g, Face.BODY, String.valueOf(count), x + 54, base + 22, 13, 0xFFF2D43A, Align.CENTER);
		g.pose().popMatrix();
	}

	/**
	 * Lúcio's song on the Crossfade box: a band along its foot, Overwatch's colours for the two (healing yellow, speed
	 * green: ours from the songs' auras), and "+" or "»" in its corner; it blinks during the 0.4 s swap.
	 */
	private static void song(GuiGraphicsExtractor g, boolean speed, boolean swapping, float x, float base, float w) {
		if (swapping && (Util.getMillis() / 100) % 2 == 0) {
			return;
		}
		float cx = x + 32;
		g.pose().pushMatrix();
		curve(g, cx, base + 36, slopeAt(960 - (w - cx)));
		int colour = speed ? 0xFF6FD23C : 0xFFF2D43A;
		fill(g, x + 4, base + 60, 56, 6, colour);
		HudText.draw(g, Face.BODY, speed ? "»" : "+", x + 54, base + 22, 13, colour, Align.CENTER);
		g.pose().popMatrix();
	}

	/**
	 * An ability's box (the critique's measurements of Overwatch's). Ready: tall (64 x 66), off-white with the black
	 * icon, a white underline, the key label filled. Just used: the same, coral. Cooling down: shorter and lower (64 x
	 * 60), dark, an orange fill rising as it recharges, the icon darkened behind an ink splash with the whole seconds
	 * left, the key label outlined. Back: a short cyan flash.
	 */
	private static void ability(GuiGraphicsExtractor g, Hero hero, float w, String ability, float x, float base, KeyMapping key,
								float cooldown, float total, long readyAt) {
		float size = 64;
		float cx = x + size / 2;
		g.pose().pushMatrix();
		curve(g, cx, base + 36, slopeAt(960 - (w - cx)));
		Identifier icon = HeroArt.ability(hero, ability);
		boolean ready = cooldown <= 0;
		if (ready || cooldown > total - CAST_TIME) {
			float y = base + 3;
			float h = 66;
			if (!ready) {
				box(g, x, y, size, h, BOX_CAST_BOTTOM);
				boxFade(g, x, y, size, h, BOX_CAST_TOP);
			} else if (since(readyAt) < FLASH_TIME) {
				box(g, x - 2, y - 2, size + 4, h + 4, FLASH_BORDER);
				box(g, x, y, size, h, FLASH);
			} else {
				box(g, x, y, size, h, BOX_READY_BOTTOM);
				boxFade(g, x, y, size, h, BOX_READY_TOP);
			}
			image(g, HudShapes.LINE, x, y + h + 7 + 1 - 3, size, 6, 0xE6FFFFFF);
			iconOrName(g, icon, ability, x, y + 1, size, 0xE62A2A2C);
		} else {
			float y = base + 11;
			float h = 60;
			box(g, x, y, size, h, BOX_COOLDOWN);
			float done = Math.clamp(1 - cooldown / total, 0, 1);
			float fillH = h * done;
			box(g, x, y + h - fillH, size, fillH, COOLDOWN_FILL);
			fill(g, x, y + h - fillH - 1.5f, size, 1.5f, COOLDOWN_EDGE);
			// the icon as a faint ghost, a little darker than the box (the third critique: black merged with the splash)
			iconOrName(g, icon, ability, x, y - 2, size, 0x4D000000);
			String seconds = String.valueOf((int) Math.ceil(cooldown));
			brush(g, seconds, cx, y + h / 2 + 15, 31);
			HudText.plain(g, Face.NUMBERS, seconds, cx, y + h / 2 + 15, 31, WHITE, Align.CENTER);
		}
		if (KeyMappingHelper.getBoundKeyOf(key).getType() == InputConstants.Type.MOUSE) {
			mouse(g, key, cx, base + 98);
		} else {
			keyLabel(g, key, cx, base + 98, 0, ready);
		}
		g.pose().popMatrix();
	}

	private static void iconOrName(GuiGraphicsExtractor g, @Nullable Identifier icon, String ability, float x, float y,
								   float size, int color) {
		if (icon != null) {
			image(g, icon, x + 7, y + 7, size - 14, size - 14, color);
		} else {
			String name = Component.translatable("overcraft.ability." + ability + ".short").getString();
			HudText.draw(g, Face.BODY, name, x + size / 2, y + size / 2 + 6, 12, color, Align.CENTER);
		}
	}

	private static void weapons(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float w) {
		if (hero == Hero.MERCY) {
			// the weapon she holds, where Kiriko's kunai panel is: the staff (no ammo) or the blaster's rounds
			boolean blaster = (s.mode() & dev.overcraft.mercy.Mercy.FLAG_BLASTER) != 0;
			weapon(g, hero, w, blaster ? "caduceus_blaster" : "caduceus_staff", w - 307, 938.5f, 251, 65, 109, 36, w - 81,
				blaster ? s.primaryAmmo() : -1, blaster ? dev.overcraft.mercy.MercyStats.BLASTER_AMMO : -1,
				HeroControls.get(Action.SECONDARY_FIRE), w - 96, 1026);
			return;
		}
		if (hero == Hero.JETPACK_CAT) {
			// Biotic Pawjectiles on the left button, where Kiriko's kunai panel is
			weapon(g, hero, w, "biotic_pawjectiles", w - 307, 938.5f, 251, 65, 109, 36, w - 81, s.primaryAmmo(),
				JetpackCatStats.AMMO, HeroControls.get(Action.SECONDARY_FIRE), w - 96, 1026);
			return;
		}
		if (hero == Hero.LUCIO) {
			// the Sonic Amplifier on the left button, where Kiriko's kunai panel is
			weapon(g, hero, w, "sonic_amplifier", w - 307, 938.5f, 251, 65, 109, 36, w - 81, s.primaryAmmo(), LucioStats.AMP_AMMO,
				HeroControls.get(Action.SECONDARY_FIRE), w - 96, 1026);
			return;
		}
		// ofuda (primary fire), then kunai (secondary): Overwatch's order. The server's weapon slots are the other way
		// round (its primary is the kunai), hence the crossed ammo fields.
		// line centres 61-62 px apart, 16 px between the panels, the mice under them (the second critique, measured)
		weapon(g, hero, w, "healing_ofuda", w - 484, 923.5f, 169, 64, 40, 40, w - 342, s.secondaryAmmo(),
			KirikoStats.OFUDA_AMMO, HeroControls.get(Action.PRIMARY_FIRE), w - 355, 1009);
		weapon(g, hero, w, "kunai", w - 307, 938.5f, 251, 65, 114, 36, w - 81, s.primaryAmmo(), KirikoStats.KUNAI_AMMO,
			HeroControls.get(Action.SECONDARY_FIRE), w - 96, 1026);
	}

	/**
	 * A weapon panel: blue lines along its top and bottom, the icon, "ammo | clip", and under it the mouse with its fire
	 * button in orange; the ammo ends at textRight (Overwatch's: digits about 19 px tall, 22 px clear of the ofuda's
	 * icon). A reload shows nothing here: the count stays as it was until the ammo is back, the animation being
	 * Overwatch's only sign of it (the owner, 2026-10-27).
	 */
	private static void weapon(GuiGraphicsExtractor g, Hero hero, float w, String weapon, float x, float y, float width, float height,
							   float iconW, float iconH, float textRight, int ammo, int clip, KeyMapping fire,
							   float mouseX, float mouseY) {
		g.pose().pushMatrix();
		float cx = x + width / 2;
		curve(g, cx, y + height / 2, slopeAt(960 - (w - cx)));
		// no fill: inside a panel the scene shows unchanged (the eighth critique)
		line(g, x, y, width, true);
		line(g, x, y + height - 3, width, false);
		float mid = y + height / 2;
		Identifier icon = HeroArt.ability(hero, weapon);
		if (icon != null) {
			// the kunai's icon a little further in than the ofuda's card (the eighth critique, the owner's frames)
			image(g, icon, x + (iconW > 100 ? 21 : 16), mid - iconH / 2, iconW, iconH, WHITE);
		}
		if (clip > 0) {
			// a weapon with no ammo (Mercy's staff) shows none
			String current = String.valueOf(ammo);
			String max = String.valueOf(clip);
			float cap = 16;
			float maxW = HudText.width(Face.BODY, max, cap);
			HudText.draw(g, Face.BODY, max, textRight, mid + cap / 2, cap, WHITE, Align.RIGHT);
			fill(g, textRight - maxW - 6, mid - 10, 1.5f, 20, DIVIDER);
			HudText.draw(g, Face.BODY, current, textRight - maxW - 11, mid + cap / 2, cap, WHITE, Align.RIGHT);
		}
		g.pose().popMatrix();
		mouse(g, fire, mouseX, mouseY);
	}

	/**
	 * The panels' bright blue line (3 px), with a soft glow: soft-edged textures rather than fills, which would step
	 * where the HUD's curve slopes them.
	 */
	private static void line(GuiGraphicsExtractor g, float x, float y, float width, boolean top) {
		// a 2.5 px core in Overwatch's lighter blue with a soft glow of about 3 px, each end turning inward in a small
		// rounded hook (the eighth critique, the owner's frames: the top lines bend down, the bottom ones up)
		image(g, HudShapes.LINE_GLOW, x, y + 1.5f - 8, width, 16, BLUE_GLOW);
		image(g, HudShapes.LINE, x, y + 1.5f - 3.75f, width, 7.5f, PANEL_LINE);
		float hookY = top ? y + 1.5f : y + 1.5f - 4;
		for (float end : new float[] {x, x + width - 2.5f}) {
			image(g, HudShapes.LINE, end - 0.6f, hookY, 3.7f, 4, PANEL_LINE);
		}
	}

	/** Under a weapon panel: the mouse with the fire button in orange, or the key it's bound to. */
	private static void mouse(GuiGraphicsExtractor g, KeyMapping fire, float cx, float cy) {
		String key = KeyMappingHelper.getBoundKeyOf(fire).getName();
		if (key.equals("key.mouse.left") || key.equals("key.mouse.right")) {
			// 19 x 27 with its outline (Overwatch's: white, a crisp dark outline and button lines, the bound button
			// orange-red: the eighth critique)
			float mw = 19;
			float mh = mw * HudShapes.MOUSE_H / HudShapes.MOUSE_W;
			image(g, HudShapes.MOUSE_OUTLINE, cx - mw / 2, cy - mh / 2, mw, mh, 0xFF111111);
			image(g, HudShapes.MOUSE, cx - mw / 2, cy - mh / 2, mw, mh, MOUSE_BODY);
			image(g, key.equals("key.mouse.left") ? HudShapes.MOUSE_LEFT : HudShapes.MOUSE_RIGHT, cx - mw / 2, cy - mh / 2, mw, mh,
				MOUSE_BUTTON);
		} else {
			keyLabel(g, fire, cx, cy, 0, false);
		}
	}

	// ---- the middle: crosshair, ofuda counter, Swift Step target ----

	/** Overwatch's default: a small white "+" of four ticks around a gap, outlined dark. */
	private static void crosshair(GuiGraphicsExtractor g, float cx, float cy) {
		float gap = 4;
		float len = 7;
		float t = 2;
		for (int pass = 0; pass < 2; pass++) {
			float o = pass == 0 ? 1 : 0;
			int color = pass == 0 ? 0x99000000 : WHITE;
			fill(g, cx - t / 2 - o, cy - gap - len - o, t + 2 * o, len + 2 * o, color);
			fill(g, cx - t / 2 - o, cy + gap - o, t + 2 * o, len + 2 * o, color);
			fill(g, cx - gap - len - o, cy - t / 2 - o, len + 2 * o, t + 2 * o, color);
			fill(g, cx + gap - o, cy - t / 2 - o, len + 2 * o, t + 2 * o, color);
		}
	}

	/**
	 * The ofuda counter under the crosshair: five amber "/" bars, one per volley (two ofuda), spent ones from the right
	 * as thin light outlines over a dark fill, a white flash when the clip comes back; a thin bracket under it. The
	 * refill started by letting go keeps the ofuda left showing (all dim, they read as all thrown: #12).
	 */
	private static void ofudaCounter(GuiGraphicsExtractor g, HeroSyncPayload s, float cx, float cy) {
		int segments = KirikoStats.OFUDA_AMMO / KirikoStats.OFUDA_PER_THROW;
		int loaded = s.secondaryAmmo() / KirikoStats.OFUDA_PER_THROW;
		boolean flash = since(ofudaRefilledAt) < FLASH_TIME;
		// the masks are drawn at two texels a pixel
		float w = HudShapes.SEGMENT_W / 2f;
		float h = HudShapes.SEGMENT_H / 2f;
		float pitch = 29;
		for (int i = 0; i < segments; i++) {
			float x = cx + (i - (segments - 1) / 2f) * pitch - w / 2;
			float y = cy - h / 2;
			// about 10% smaller than the mask and see-through (the eighth critique: 25 x 20 at the middle row, about 80%)
			float sw = w * 0.9f;
			float sh = h * 0.9f;
			float sx = x + (w - sw) / 2;
			float sy = y + (h - sh) / 2;
			if (i < loaded) {
				image(g, HudShapes.OFUDA_SEGMENT, sx, sy, sw, sh, flash ? WHITE : alpha(AMBER, 0.82f));
			} else {
				image(g, HudShapes.OFUDA_SEGMENT, sx, sy, sw, sh, SPENT_FILL);
				image(g, HudShapes.OFUDA_SEGMENT_OUTLINE, sx, sy, sw, sh, SPENT_EDGE);
			}
		}
		// the bracket: a 6 px dot at each end, then 5 px out a 1.5 px line slanting 45° down and in to dotY + 19, then
		// 11 px flat (the eighth critique)
		int bracket = 0x8CC8D0E0;
		float span = 92;
		for (int side = -1; side <= 1; side += 2) {
			float dotX = cx + side * span;
			float dotY = cy + 6;
			image(g, HudShapes.DISC, dotX - 3, dotY - 3, 6, 6, 0xD9C8D0E0);
			float startX = dotX - side * 3.5f;
			float startY = dotY + 3.5f;
			g.pose().pushMatrix();
			g.pose().translate(startX, startY);
			g.pose().rotate((float) Math.toRadians(side < 0 ? 45 : 135));
			fill(g, 0, -0.75f, 21.2f, 1.5f, bracket);
			g.pose().popMatrix();
			float flatX = startX - side * 15;
			fill(g, Math.min(flatX, flatX - side * 11), dotY + 19 - 0.75f, 11, 1.5f, bracket);
		}
	}

	/**
	 * Swift Step's target: four cyan corner brackets in a diamond around the ally it would go to, with its key under
	 * them, on the ally (not the crosshair).
	 */
	private static void swiftStepTarget(GuiGraphicsExtractor g, LocalPlayer player, HeroSyncPayload s, float w, float h,
										float partial) {
		if (s.ability1Cooldown() > 0) {
			return;
		}
		LivingEntity target = Targeting.allyInView(player, KirikoStats.SWIFT_STEP_RANGE,
			KirikoStats.SWIFT_STEP_HALF_ANGLE * HeroOptions.get().swiftStepSensitivity / 100.0, false);
		if (target == null) {
			return;
		}
		Vec3 at = target.getPosition(partial).add(0, target.getBbHeight() * 0.55, 0);
		float[] screen = project(at, w, h);
		if (screen == null) {
			return;
		}
		float x = screen[0];
		float y = screen[1];
		float half = 48;
		float arm = 18;
		float t = 3;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().rotate((float) Math.toRadians(45));
		for (int corner = 0; corner < 4; corner++) {
			float sx = (corner & 1) == 0 ? -1 : 1;
			float sy = (corner & 2) == 0 ? -1 : 1;
			float ox = sx * half;
			float oy = sy * half;
			// the corner's two arms, pointing back along the square's sides
			fill(g, sx < 0 ? ox : ox - arm, sy < 0 ? oy : oy - t, arm, t, CYAN);
			fill(g, sx < 0 ? ox : ox - t, sy < 0 ? oy : oy - arm, t, arm, CYAN);
		}
		g.pose().popMatrix();
		keyLabel(g, HeroControls.get(Action.ABILITY_1), x, y + half * 1.42f + 18, CYAN, false);
	}

	/**
	 * Allies' health over their heads, as Overwatch shows its teammates' (the critique, from the owner's frames: a cyan
	 * "v" under a row of white pips, one per 25 health): players and pets always, within ALLY_RANGE; villagers, golems
	 * and allays only when hurt or close, or a village would be a wall of bars. Smaller further away.
	 */
	private static void allyMarkers(GuiGraphicsExtractor g, LocalPlayer player, float w, float h, float partial) {
		Vec3 eye = player.getEyePosition(partial);
		for (LivingEntity ally : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(ALLY_RANGE),
			e -> Allies.isAlly(player, e) && !e.isInvisible())) {
			Vec3 at = ally.getPosition(partial);
			double distance = at.distanceTo(eye);
			boolean always = ally instanceof Player || ally instanceof OwnableEntity;
			if (distance > ALLY_RANGE || !always && ally.getHealth() >= ally.getMaxHealth() && distance > ALLY_NEAR) {
				continue;
			}
			// just above the head, a few pixels clear of it on screen (hero mode hides allies' Minecraft name tags: the
			// name is in the marker). It was 0.35 m above in the world, which an ally close by put near the top of the
			// screen, far above their head (the fourth critique); off the top, it's off the screen
			Vec3 head = at.add(0, ally.getBbHeight() + 0.05, 0);
			float[] screen = project(head, w, h);
			if (screen == null) {
				continue;
			}
			// the head's top as seen: the highest of its box's top corners on screen (the box's top centre lies behind
			// the face, and an ally close by got the bar on their forehead, the fifth critique)
			double half = ally.getBbWidth() / 2;
			for (int corner = 0; corner < 4; corner++) {
				float[] c = project(head.add(corner < 2 ? -half : half, 0, corner % 2 == 0 ? -half : half), w, h);
				if (c != null) {
					screen[1] = Math.min(screen[1], c[1]);
				}
			}
			if (screen[1] < 0) {
				continue;
			}
			screen[1] -= ALLY_LIFT;
			float size = (float) Math.clamp(1.15 - distance / ALLY_RANGE * 0.5, 0.65, 1);
			// one that would cross a side of the screen isn't drawn, as one off the top isn't (a long bar ran off the
			// right edge cut mid-pip, the seventh critique)
			float barHalf = barWidth(Ow.toOw(ally.getMaxHealth())) / 2 * size;
			if (screen[0] - barHalf < 0 || screen[0] + barHalf > w) {
				continue;
			}
			float alpha = (float) Math.clamp((ALLY_RANGE - distance) / 6, 0, 1);
			String name = ally instanceof Player || ally.hasCustomName() ? ally.getName().getString().toUpperCase(Locale.ROOT) : null;
			allyMarker(g, screen[0], screen[1], size, alpha, Ow.toOw(ally.getHealth()), Ow.toOw(ally.getMaxHealth()), name);
		}
	}

	/**
	 * One ally's marker, its bottom at (x, y): from the top, the pips, the name in blue caps (players and named mobs), a
	 * small open "v" (Overwatch's, the second critique).
	 */
	private static void allyMarker(GuiGraphicsExtractor g, float x, float y, float size, float alpha, float health, float max,
								   @Nullable String name) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(size, size);
		// everything above the bottom: chevron (10), name (14) if any, then the pips
		g.pose().translate(0, name == null ? -14 : -30);
		int pips = allyPips(max);
		float per = max / pips;
		float left = -barWidth(max) / 2;
		for (int i = 0; i < pips; i++) {
			float held = Math.clamp((health - i * per) / per, 0, 1);
			float px = left + i * PIP_PITCH;
			if (held > 0.9f) {
				// a thin dark edge, half a pixel each side, so neighbours keep a gap
				fill(g, px - 0.5f, -PIP_H - 0.5f, PIP_W + 1, PIP_H + 1, alpha(0x000000, 0.45f * alpha));
				fill(g, px, -PIP_H, PIP_W, PIP_H, alpha(0xFFFFFF, alpha));
			} else {
				// an empty slot of its own, dark with a lighter rim (the edges touched: lost health read as one dark slab,
				// the seventh critique), and what's left of it in white
				fill(g, px, -PIP_H, PIP_W, PIP_H, alpha(0x000000, 0.45f * alpha));
				outline(g, px, -PIP_H, PIP_W, PIP_H, 1, alpha(0xFFFFFF, 0.3f * alpha));
				if (held > 0) {
					fill(g, px, -PIP_H, PIP_W * held, PIP_H, alpha(0xFFFFFF, alpha));
				}
			}
		}
		float below = 4;
		if (name != null) {
			HudText.draw(g, Face.SMALL, name, 0, below + 12, 11, alpha(0x5CC6DC, alpha), Align.CENTER);
			below += 16;
		}
		image(g, HudShapes.CHEVRON, -6, below, 12, 10, alpha(0x29A8E0, alpha));
		g.pose().popMatrix();
	}

	/**
	 * Ally pips: squarer, about Overwatch's 10 x 9 at an 11-12 px pitch (the fourth critique: ours were 6 x 10, taller than
	 * wide, 80 px for ten).
	 */
	private static final float PIP_W = 9;
	private static final float PIP_H = 8;
	private static final float PIP_PITCH = 11;

	/** One pip per 25 health, at most 20 (a golem's 1250 would run off the screen: then each pip holds more). */
	private static int allyPips(float max) {
		return Math.clamp((int) Math.ceil(max / 25), 1, 20);
	}

	/** An ally bar's width at full size. */
	private static float barWidth(float max) {
		return allyPips(max) * PIP_PITCH - (PIP_PITCH - PIP_W);
	}

	/** World position to HUD position (reference pixels), or null behind the camera. */
	public static float @Nullable [] project(Vec3 at, float w, float h) {
		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
		Vec3 rel = at.subtract(camera.position());
		Vector3f v = new Vector3f((float) rel.x, (float) rel.y, (float) rel.z);
		float depth = v.dot(camera.forwardVector());
		if (depth < 0.1f) {
			return null;
		}
		float tan = (float) Math.tan(Math.toRadians(camera.getFov()) / 2);
		float aspect = w / h;
		float nx = -v.dot(camera.leftVector()) / (depth * tan * aspect);
		float ny = v.dot(camera.upVector()) / (depth * tan);
		return new float[] {w / 2 * (1 + nx), h / 2 * (1 - ny)};
	}

	// ---- Minecraft mode ----

	/** Out of hero mode: just a small ult meter right of the hotbar, and how to get back. */
	private static void minecraftMode(GuiGraphicsExtractor g, HeroSyncPayload s, Hero hero) {
		int w = g.guiWidth();
		int h = g.guiHeight();
		float cx = w / 2f + 91 + 34;
		float cy = h - 12;
		float charge = Math.min(1, s.ultCharge() / Math.max(1, s.ultCost()));
		disc(g, cx, cy, 10.5f, 0x8C000000);
		ticks(g, cx, cy, 8, 10.5f, charge, ULT_TICK_DARK, ULT_TICK, ULT_TRACK);
		HudText.draw(g, Face.NUMBERS, String.valueOf((int) Math.floor(100 * charge)), cx, cy + 3.5f,
			7, WHITE, Align.CENTER);
		Component hint = Component.translatable("overcraft.hud.hero_mode", ClientHero.HERO_MODE.getTranslatedKeyMessage());
		HudText.draw(g, Face.SMALL, hint.getString().toUpperCase(Locale.ROOT), cx + 15, cy + 3, 6, TEXT_DIM, Align.LEFT);
	}

	/**
	 * How far up Minecraft's chat moves in hero mode, in GUI units: from just above the hotbar (which hero mode hides)
	 * to above the portrait, so it never covers the health.
	 */
	public static float chatLift(int guiWidth, int guiHeight) {
		float scale = scale(guiWidth, guiHeight);
		return Math.max(0, (REF_H - 870) * scale - 40);
	}

	/** Where Minecraft's status-effect icons end, in GUI units from the top (0 with none): as Hud.extractEffects lays them. */
	private static float effectRowsBottom(LocalPlayer player) {
		boolean beneficial = false;
		boolean harmful = false;
		for (MobEffectInstance effect : player.getActiveEffects()) {
			if (effect.showIcon()) {
				if (effect.getEffect().value().isBeneficial()) {
					beneficial = true;
				} else {
					harmful = true;
				}
			}
		}
		return harmful ? 51 : beneficial ? 25 : 0;
	}

	/**
	 * Minecraft's air bubbles in hero mode (Overwatch has no breath): just above the health readout, as wide as its
	 * pips and tilted with them. In their own place they sat against the ult meter and the ability boxes (found playing
	 * underwater). Sets the pose so vanilla's bubbles, drawn where vanilla puts them (right end at the hotbar's right
	 * edge, top 59 GUI units up from the bottom), land there.
	 */
	public static void airPlace(GuiGraphicsExtractor g) {
		barPlace(g, 49);
	}

	/**
	 * A ridden animal's hearts in hero mode, in the same place as the air bubbles (the hearts sat on the ability boxes,
	 * found playing on a horse). Vanilla draws them from 39 GUI units up from the bottom, rows going up.
	 */
	public static void mountPlace(GuiGraphicsExtractor g) {
		barPlace(g, 39);
	}

	/** Moves a vanilla bar drawn right-aligned to the hotbar's right edge, its top fromBottom GUI units up, above the health. */
	private static void barPlace(GuiGraphicsExtractor g, float fromBottom) {
		float scale = scale(g.guiWidth(), g.guiHeight());
		float left = 194;
		float pipsWidth = 10 * 26.6f - 2.6f;
		g.pose().scale(scale, scale);
		curve(g, left, 933, slopeAt(left + 120 - 960));
		// the bar's right end over the last pip, its top just above the "+" box (vanilla's air line, with the food bar
		// hidden, is 49 units up: measured on screen)
		g.pose().translate(left + 2 + pipsWidth, 864);
		// vanilla's ten bubbles span 81 units: as wide as the pips
		float k = pipsWidth / 81;
		g.pose().scale(k, k);
		g.pose().translate(-(g.guiWidth() / 2f + 91), -(g.guiHeight() - fromBottom));
	}

	/**
	 * Hero mode's chat, as Overwatch's (the second critique: text about 18 px tall, at the left, its bottom near y
	 * 700): Minecraft's chat scaled about its bottom-left corner (40 GUI units up from the screen's bottom).
	 */
	/** Hero chat's wrap width in chat units: about 360 reference pixels of text, to x 425 (the eighth critique). */
	public static final int CHAT_WRAP = 147;

	public static void chatPlace(GuiGraphicsExtractor g) {
		float scale = scale(g.guiWidth(), g.guiHeight());
		// a line is 9 GUI units: make it 22 reference pixels, Overwatch's line pitch (the eighth critique), never larger
		// than Minecraft's own
		float k = Math.min(1, 22 * scale / 9);
		// in from the edge as Overwatch's (its lines start about 60-90 px in; ours were 8 px in, the fourth critique), the
		// newest line's baseline at about y 657 (the eighth critique: it sat about 40 px low)
		g.pose().translate(CHAT_LEFT * scale, (REF_H - 423) * scale + (g.guiHeight() - REF_H * scale));
		g.pose().scale(k, k);
		g.pose().translate(0, -(g.guiHeight() - 40));
	}

	// ---- pieces ----

	/**
	 * A key's label in a small box: outlined, white on dark; or inverted, filled off-white with a dark letter (a ready
	 * ability's, the ready ult's).
	 */
	private static void keyLabel(GuiGraphicsExtractor g, KeyMapping key, float cx, float cy, int tint, boolean inverted) {
		String label = shortKey(key);
		float cap = 10.5f;
		float textW = HudText.width(Face.SMALL, label, cap);
		float bw = Math.max(22, textW + 8);
		float bh = 21;
		float x = cx - bw / 2;
		float y = cy - bh / 2;
		int edge = tint != 0 ? tint : 0xCCE6EEF0;
		if (inverted) {
			// Overwatch's: #D6D9DD slightly see-through, rounded about 3 px, the letters #343639 (the eighth critique)
			box(g, x, y, bw, bh, 0xEBD6D9DD);
		} else {
			fill(g, x, y, bw, bh, SHADE);
			outline(g, x, y, bw, bh, 1.5f, edge);
		}
		HudText.draw(g, Face.SMALL, label, cx, cy + cap / 2, cap, inverted ? 0xFF343639 : edge, Align.CENTER);
	}

	/** A key's name as Overwatch writes it: LSHIFT, E, Q, SPACE, MOUSE4. */
	public static String shortKey(KeyMapping key) {
		InputConstants.Key bound = KeyMappingHelper.getBoundKeyOf(key);
		String name = bound.getName();
		if (name.startsWith("key.keyboard.")) {
			String rest = name.substring("key.keyboard.".length());
			if (rest.startsWith("left.") || rest.startsWith("right.")) {
				String side = rest.startsWith("left.") ? "L" : "R";
				String what = rest.substring(rest.indexOf('.') + 1);
				what = what.equals("control") ? "ctrl" : what;
				return (side + what).toUpperCase(Locale.ROOT);
			}
			if (rest.length() <= 6 && !rest.contains(".")) {
				return rest.toUpperCase(Locale.ROOT);
			}
		} else if (name.startsWith("key.mouse.")) {
			String button = name.substring("key.mouse.".length());
			return switch (button) {
				case "left" -> "LMB";
				case "right" -> "RMB";
				case "middle" -> "MMB";
				default -> "MOUSE" + button;
			};
		}
		String text = key.getTranslatedKeyMessage().getString().trim();
		return text.toUpperCase(Locale.ROOT);
	}

	/** Draws text with a thin dark outline all round (a cooldown's seconds, over their ink splash). */
	/**
	 * Overwatch's splash behind a cooldown's seconds: a ragged black brush stroke hugging the digits 3-7 px out, with a
	 * couple of drips (ref1080's "12"; the fourth critique: ours was a round blot behind any number). The digits drawn
	 * in black round themselves at jittered offsets, the same for the same number (it doesn't flicker).
	 */
	private static void brush(GuiGraphicsExtractor g, String text, float x, float baseline, float cap) {
		java.util.Random random = new java.util.Random(text.hashCode() * 31L + 7);
		int black = 0xE6000000;
		for (int ring = 0; ring < 2; ring++) {
			for (int i = 0; i < 12; i++) {
				double a = Math.PI * 2 * (i + random.nextFloat() * 0.6f) / 12;
				float r = (ring == 0 ? 3 : 5) + random.nextFloat() * 2.5f;
				HudText.plain(g, Face.NUMBERS, text, x + (float) Math.cos(a) * r, baseline + (float) Math.sin(a) * r, cap, black,
					Align.CENTER);
			}
		}
		// drips running down from under the digits
		float width = HudText.width(Face.NUMBERS, text, cap);
		for (int i = 0; i < 2; i++) {
			float dx = x + (i == 0 ? -0.25f : 0.2f) * width + (random.nextFloat() - 0.5f) * 6;
			float length = 3 + random.nextFloat() * 5;
			float top = baseline + 2;
			fill(g, dx - 1.25f, top, 2.5f, length, black);
			image(g, HudShapes.DISC, dx - 1.9f, top + length - 1.9f, 3.8f, 3.8f, black);
		}
	}

	/** The local slope of the HUD's curve at dx from the centre (16:9 layout), applied around (x, y). */
	private static float slopeAt(float dx) {
		return 2 * CURVE * dx;
	}

	private static void curve(GuiGraphicsExtractor g, float x, float y, float slope) {
		g.pose().translate(x, y);
		g.pose().mul(new Matrix3x2f(1, slope, 0, 1, 0, 0));
		g.pose().translate(-x, -y);
	}

	/**
	 * A box with smooth edges where the HUD's curve slopes it: a plain fill steps a pixel at a time along a sloped
	 * edge (the second critique saw the underline in three pieces). The square mask's clear margin sits just outside.
	 */
	private static void box(GuiGraphicsExtractor g, float x, float y, float w, float h, int color) {
		float k = (float) HudShapes.SQUARE_SIZE / (HudShapes.SQUARE_SIZE - 2 * HudShapes.SQUARE_MARGIN);
		float mx = w * (k - 1) / 2;
		float my = h * (k - 1) / 2;
		image(g, HudShapes.SQUARE, x - mx, y - my, w * k, h * k, color);
	}

	/** box() in a colour fading out downward: over a box, a vertical gradient. */
	private static void boxFade(GuiGraphicsExtractor g, float x, float y, float w, float h, int color) {
		float k = (float) HudShapes.SQUARE_SIZE / (HudShapes.SQUARE_SIZE - 2 * HudShapes.SQUARE_MARGIN);
		float mx = w * (k - 1) / 2;
		float my = h * (k - 1) / 2;
		image(g, HudShapes.SQUARE_FADE, x - mx, y - my, w * k, h * k, color);
	}

	private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
		image(g, HudShapes.DISC, cx - r, cy - r, 2 * r, 2 * r, color);
	}

	private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
		image(g, HudShapes.RING, cx - r, cy - r, 2 * r, 2 * r, color);
	}
}
