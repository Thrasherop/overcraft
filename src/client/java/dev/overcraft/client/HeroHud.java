package dev.overcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.Ow;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.hud.HudShapes;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.client.hud.KillFeedHud;
import dev.overcraft.client.mixin.ToastManagerAccessor;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.Targeting;
import dev.overcraft.kiriko.KirikoStats;
import dev.overcraft.net.HeroSyncPayload;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
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
	private static final int BLUE_GLOW = 0x99153858;
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
	private static final int PORTRAIT_BORDER = 0xFF68CCFF;
	private static final int PORTRAIT_BACK = 0xFF1B2A4A;
	private static final int DIVIDER = 0xB34FA3A5;

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
	private static int ofudaSeen = -1;
	private static long ofudaRefilledAt = -1;
	private static long ultStartedAt = -1;
	private static long stepCastAt = -1;
	/** Swift Step's screen flash: teal at the edges and streaks rushing in, as she steps (the reference, §2). */
	private static final float STEP_FLASH = 0.45f;
	/** Below this share of her health the screen's edges glow red (ours: the critique's "about 35%"). */
	private static final float LOW_HEALTH = 0.35f;
	/** Allies' markers show this far (metres: ours, about Overwatch's view of its teammates in a fight). */
	private static final double ALLY_RANGE = 40;
	/** Villagers, golems and allays at full health show only this close. */
	private static final double ALLY_NEAR = 8;
	private static final int STREAKS = 14;

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
		if (!ClientHero.controls()) {
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
		// Overwatch's kill feed sits where Minecraft's toasts do: below any showing (32 GUI units a slot)
		int toastSlots = ((ToastManagerAccessor) mc.gui.toastManager()).overcraft$occupiedSlots().length();
		KillFeedHud.render(g, w - 36, Math.max(36, toastSlots * 32 / scale + 12));
		crosshair(g, w / 2, h / 2);
		ofudaCounter(g, s, w / 2, h / 2 + 136);
		swiftStepTarget(g, player, s, w, h, delta.getGameTimeDeltaPartialTick(true));
		// the rest is laid out on a 1080-high strip along the bottom (taller screens than 16:9 have room above it)
		g.pose().translate(0, h - REF_H);
		health(g, player, hero);
		ult(g, hero, s, w / 2, 930);
		abilities(g, hero, s, w);
		weapons(g, hero, s, w);
		g.pose().popMatrix();
	}

	/** Notices cooldowns ending, the ofuda clip refilling and the ult starting, for the flashes and the countdown. */
	private static void track(HeroSyncPayload s) {
		long now = Util.getMillis();
		if (ability1Seen > 0 && s.ability1Cooldown() <= 0) {
			ability1ReadyAt = now;
		}
		if (ability1Seen <= 0 && s.ability1Cooldown() > 0) {
			stepCastAt = now;
		}
		if (ability2Seen > 0 && s.ability2Cooldown() <= 0) {
			ability2ReadyAt = now;
		}
		ability1Seen = s.ability1Cooldown();
		ability2Seen = s.ability2Cooldown();
		if (ofudaSeen >= 0 && s.secondaryAmmo() > ofudaSeen && s.secondaryAmmo() == KirikoStats.OFUDA_AMMO) {
			ofudaRefilledAt = now;
		}
		ofudaSeen = s.secondaryAmmo();
		if (!s.ultActive()) {
			ultStartedAt = -1;
		} else if (ultStartedAt < 0) {
			ultStartedAt = now;
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
			image(g, HudShapes.VIGNETTE, 0, 0, w, h, alpha(0xB0102A, 0.45f + 0.4f * low + pulse));
		}
		float step = since(stepCastAt);
		if (step < STEP_FLASH) {
			float k = 1 - step / STEP_FLASH;
			image(g, HudShapes.VIGNETTE, 0, 0, w, h, alpha(0x29E0CF, 0.9f * k));
			java.util.Random random = new java.util.Random(stepCastAt);
			for (int i = 0; i < STREAKS; i++) {
				float angle = random.nextFloat() * Mth.TWO_PI;
				float travel = Math.clamp(step / STEP_FLASH + random.nextFloat() * 0.2f, 0, 1);
				streak(g, w, h, angle, 1.05f - 0.5f * travel, false, alpha(0x9AFFF4, 0.8f * k));
			}
		}
		if (s.rushed()) {
			float time = Util.getMillis() / 1000f;
			float pulse = 0.5f + 0.08f * (float) Math.sin(time * Mth.TWO_PI * 0.7f);
			image(g, HudShapes.VIGNETTE, 0, 0, w, h, alpha(0xA8F2FF, pulse));
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
		float px = 52;
		float py = 890;
		float pw = 104;
		float ph = pw * HudShapes.SHIELD_H / HudShapes.SHIELD_W;
		image(g, HudShapes.SHIELD, px - 3, py - 3, pw + 6, ph + 6, 0x80000000);
		image(g, HudShapes.SHIELD, px, py, pw, ph, PORTRAIT_BACK);
		if (portrait != null) {
			image(g, portrait, px, py, pw, ph, WHITE);
		}
		image(g, HudShapes.SHIELD_BORDER, px, py, pw, ph, PORTRAIT_BORDER);

		// the rest tilts with the HUD's curve: inner (right) end higher
		float left = 194;
		g.pose().pushMatrix();
		curve(g, left, 933, slopeAt(left + 120 - 960));
		int max = Math.round(Ow.toOw(player.getMaxHealth()));
		int now = (int) Math.ceil(Ow.toOw(player.getHealth()));
		// "+" box, then current | max
		fill(g, left, 899, 44, 24, SHADE);
		image(g, HudShapes.PLUS, left + 5, 902, 18, 18, WHITE);
		float x = left + 54;
		String current = String.valueOf(now);
		HudText.draw(g, Face.BODY, current, x, 921, 20.5f, WHITE, Align.LEFT);
		x += HudText.width(Face.BODY, current, 20.5f) + 7;
		fill(g, x, 899, 2, 22, DIVIDER);
		HudText.draw(g, Face.BODY, String.valueOf(max), x + 8, 921, 15, TEXT_DIM, Align.LEFT);

		// one pip per 25 health: white while it holds health, a faint dark box when empty
		int pips = Math.max(1, (int) Math.ceil(max / 25.0));
		for (int i = 0; i < pips; i++) {
			float pipX = left + 2 + i * 26.6f;
			float held = Math.clamp((now - i * 25) / 25f, 0, 1);
			fill(g, pipX, 933, 24, 30, 0x40000000);
			if (held > 0) {
				// a nearly full pip reads as full, like Overwatch's (199 shows 8 whole pips); a partial one fills from
				// the left (27 health shows one pip and a sliver)
				float shown = held > 0.9f ? 1 : held;
				fill(g, pipX, 933, 24 * shown, 30, WHITE);
			}
		}

		// the player's name plate under the pips: dark, with a glowing blue underline ending in a dot
		String name = player.getName().getString().toUpperCase(Locale.ROOT);
		float nameW = Math.max(120, HudText.width(Face.BODY, name, 14) + 40);
		for (int i = 0; i < 12; i++) {
			// fades out to the right
			int alpha = (int) (0x90 * (1 - i / 12f));
			fill(g, left + 2 + nameW * i / 12, 972, nameW / 12 + 0.5f, 30, alpha << 24 | 0x0B1520);
		}
		HudText.draw(g, Face.BODY, name, left + 14, 993, 14, WHITE, Align.LEFT);
		fill(g, left + 2, 1004, nameW * 0.7f, 3, BLUE);
		image(g, HudShapes.GLOW, left + 2 + nameW * 0.7f - 9, 996, 18, 18, 0xFF6FD3FF);
		g.pose().popMatrix();
	}

	// ---- bottom centre: the ult meter ----

	private static void ult(GuiGraphicsExtractor g, Hero hero, HeroSyncPayload s, float cx, float cy) {
		// casting empties the meter at once (the reference: just used, it shows 0 with no ticks)
		float charge = s.ultActive() ? 0 : Math.min(1, s.ultCharge() / Math.max(1, s.ultCost()));
		boolean ready = charge >= 1;
		if (ready) {
			// a white disc with the fox in a thick electric-blue ring (darker outside), a soft blue halo, and small
			// white triangles at 3 and 9 o'clock pointing in
			image(g, HudShapes.SOFT_DISC, cx - 70, cy - 70, 140, 140, 0xB32C40C5);
			disc(g, cx, cy, 57, ULT_RING_OUTER);
			disc(g, cx, cy, 55, 0xFF3355E7);
			disc(g, cx, cy, 53, 0xFF36ACFF);
			disc(g, cx, cy, 51, ULT_RING_INNER);
			disc(g, cx, cy, 47, ULT_READY_DISC);
			Identifier fox = HeroArt.ability(hero, "kitsune_rush");
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
			// a dark disc, a fine dark outer circle, a brownish inner one, the orange ticks and the charge
			disc(g, cx, cy, 52, 0xBF000000);
			image(g, HudShapes.RING_THIN, cx - 56, cy - 56, 112, 112, 0xFF4A4F4F);
			image(g, HudShapes.RING_THIN, cx - 35, cy - 35, 70, 70, 0xB37A3A2A);
			ticks(g, cx, cy, 37, 47, charge, ULT_TICK_DARK, ULT_TICK, ULT_TRACK);
			int percent = (int) Math.floor(100 * charge);
			HudText.draw(g, Face.NUMBERS, String.valueOf(percent), cx, cy + 18, 36, WHITE, Align.CENTER);
		}
		keyLabel(g, HeroControls.get(Action.ULTIMATE), cx, 1008, 0, ready);
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
		ability(g, hero, w, "swift_step", w - 641, 903, HeroControls.get(Action.ABILITY_1), s.ability1Cooldown(),
			(float) KirikoStats.SWIFT_STEP_COOLDOWN, ability1ReadyAt);
		ability(g, hero, w, "protection_suzu", w - 563, 906, HeroControls.get(Action.ABILITY_2), s.ability2Cooldown(),
			(float) KirikoStats.SUZU_COOLDOWN, ability2ReadyAt);
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
				gradient(g, x, y, size, h, BOX_CAST_TOP, BOX_CAST_BOTTOM);
			} else if (since(readyAt) < FLASH_TIME) {
				fill(g, x - 2, y - 2, size + 4, h + 4, FLASH_BORDER);
				fill(g, x, y, size, h, FLASH);
			} else {
				gradient(g, x, y, size, h, BOX_READY_TOP, BOX_READY_BOTTOM);
			}
			fill(g, x, y + h + 3, size, 3, 0xE6FFFFFF);
			iconOrName(g, icon, ability, x, y + 1, size, ICON);
		} else {
			float y = base + 11;
			float h = 60;
			fill(g, x, y, size, h, BOX_COOLDOWN);
			float done = Math.clamp(1 - cooldown / total, 0, 1);
			float fillH = h * done;
			fill(g, x, y + h - fillH, size, fillH, COOLDOWN_FILL);
			fill(g, x, y + h - fillH - 1.5f, size, 1.5f, COOLDOWN_EDGE);
			iconOrName(g, icon, ability, x, y - 2, size, 0xB3101010);
			float splashW = 46;
			float splashH = splashW * HudShapes.SPLASH_H / HudShapes.SPLASH_W;
			image(g, HudShapes.SPLASH, cx - splashW / 2, y + h / 2 - splashH * 0.43f, splashW, splashH, 0xE6000000);
			outlined(g, Face.NUMBERS, String.valueOf((int) Math.ceil(cooldown)), cx, y + h / 2 + 15, 31, WHITE);
		}
		keyLabel(g, key, cx, base + 98, 0, ready);
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
		// ofuda (primary fire), then kunai (secondary): Overwatch's order. The server's weapon slots are the other way
		// round (its primary is the kunai), hence the crossed ammo fields.
		weapon(g, hero, w, "healing_ofuda", w - 486, 919, 171, 74, 42, 42, w - 342, s.secondaryAmmo(),
			KirikoStats.OFUDA_AMMO, s.secondaryReload(), HeroControls.get(Action.PRIMARY_FIRE), w - 355, 1018);
		weapon(g, hero, w, "kunai", w - 307, 932, 251, 78, 109, 36, w - 84, s.primaryAmmo(), KirikoStats.KUNAI_AMMO,
			s.primaryReload(), HeroControls.get(Action.SECONDARY_FIRE), w - 96, 1035);
	}

	/**
	 * A weapon panel: blue lines along its top and bottom, the icon, "ammo | clip" (dimmed while reloading, the
	 * bottom line filling white with the reload), and under it the mouse with its fire button in orange. The ammo
	 * ends at textRight (Overwatch's: digits about 19 px tall, 22 px clear of the ofuda's icon).
	 */
	private static void weapon(GuiGraphicsExtractor g, Hero hero, float w, String weapon, float x, float y, float width, float height,
							   float iconW, float iconH, float textRight, int ammo, int clip, float reload, KeyMapping fire,
							   float mouseX, float mouseY) {
		g.pose().pushMatrix();
		float cx = x + width / 2;
		curve(g, cx, y + height / 2, slopeAt(960 - (w - cx)));
		fill(g, x, y, width, height, 0x26000000);
		line(g, x, y, width);
		line(g, x, y + height - 3, width);
		if (reload >= 0) {
			image(g, HudShapes.LINE, x, y + height - 3 + 1.5f - 4.5f, width * reload, 9, 0xF2FFFFFF);
		}
		float mid = y + height / 2;
		Identifier icon = HeroArt.ability(hero, weapon);
		if (icon != null) {
			image(g, icon, x + 16, mid - iconH / 2, iconW, iconH, WHITE);
		}
		int color = reload >= 0 ? TEXT_DIM : WHITE;
		String current = String.valueOf(ammo);
		String max = String.valueOf(clip);
		float cap = 18;
		float maxW = HudText.width(Face.BODY, max, cap);
		HudText.draw(g, Face.BODY, max, textRight, mid + cap / 2, cap, color, Align.RIGHT);
		fill(g, textRight - maxW - 7, mid - 12, 2, 24, DIVIDER);
		HudText.draw(g, Face.BODY, current, textRight - maxW - 13, mid + cap / 2, cap, color, Align.RIGHT);
		g.pose().popMatrix();
		mouse(g, fire, mouseX, mouseY);
	}

	/**
	 * The panels' bright blue line (3 px), with a soft glow: soft-edged textures rather than fills, which would step
	 * where the HUD's curve slopes them.
	 */
	private static void line(GuiGraphicsExtractor g, float x, float y, float width) {
		image(g, HudShapes.LINE_GLOW, x, y + 1.5f - 8, width, 16, BLUE_GLOW);
		image(g, HudShapes.LINE, x, y + 1.5f - 4.5f, width, 9, BLUE);
	}

	/** Under a weapon panel: the mouse with the fire button in orange, or the key it's bound to. */
	private static void mouse(GuiGraphicsExtractor g, KeyMapping fire, float cx, float cy) {
		String key = KeyMappingHelper.getBoundKeyOf(fire).getName();
		if (key.equals("key.mouse.left") || key.equals("key.mouse.right")) {
			// the visible body about 20 x 29 (Overwatch's: white, outlined dark, the bound button orange-red)
			float mw = 26.7f;
			float mh = mw * HudShapes.MOUSE_H / HudShapes.MOUSE_W;
			image(g, HudShapes.MOUSE_OUTLINE, cx - mw / 2, cy - mh / 2, mw, mh, 0xB31A1A1A);
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
	 * as thin light outlines over a dark fill, all spent while reloading, a white flash when the clip comes back; a thin
	 * bracket under it.
	 */
	private static void ofudaCounter(GuiGraphicsExtractor g, HeroSyncPayload s, float cx, float cy) {
		int segments = KirikoStats.OFUDA_AMMO / KirikoStats.OFUDA_PER_THROW;
		int loaded = s.secondaryReload() >= 0 ? 0 : s.secondaryAmmo() / KirikoStats.OFUDA_PER_THROW;
		boolean flash = since(ofudaRefilledAt) < FLASH_TIME;
		// the masks are drawn at two texels a pixel
		float w = HudShapes.SEGMENT_W / 2f;
		float h = HudShapes.SEGMENT_H / 2f;
		float pitch = 29;
		for (int i = 0; i < segments; i++) {
			float x = cx + (i - (segments - 1) / 2f) * pitch - w / 2;
			float y = cy - h / 2;
			if (i < loaded) {
				image(g, HudShapes.OFUDA_SEGMENT, x, y, w, h, flash ? WHITE : AMBER);
			} else {
				image(g, HudShapes.OFUDA_SEGMENT, x, y, w, h, SPENT_FILL);
				image(g, HudShapes.OFUDA_SEGMENT_OUTLINE, x, y, w, h, SPENT_EDGE);
			}
		}
		// the bracket: a dot at each end, a short slant down and in, then flat under the bar
		int bracket = 0x8CC8C8C8;
		float span = 92;
		for (int side = -1; side <= 1; side += 2) {
			float dotX = cx + side * span;
			float dotY = cy + 6;
			image(g, HudShapes.DISC, dotX - 4, dotY - 4, 8, 8, 0xD99A9A9A);
			g.pose().pushMatrix();
			g.pose().translate(dotX, dotY);
			g.pose().rotate((float) Math.toRadians(side < 0 ? 45 : 135));
			fill(g, 0, -1, 17, 2, bracket);
			g.pose().popMatrix();
			float flatX = dotX - side * 12;
			float flatEnd = flatX - side * 24;
			fill(g, Math.min(flatX, flatEnd), dotY + 11, 24, 2, bracket);
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
		LivingEntity target = Targeting.allyInView(player, KirikoStats.SWIFT_STEP_RANGE, KirikoStats.SWIFT_STEP_HALF_ANGLE, false);
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
			// over a player's name tag, just over anyone else's head
			Vec3 head = at.add(0, ally.getBbHeight() + (ally instanceof Player ? 0.75 : 0.3), 0);
			float[] screen = project(head, w, h);
			if (screen == null) {
				continue;
			}
			float size = (float) Math.clamp(1.15 - distance / ALLY_RANGE * 0.5, 0.65, 1);
			float alpha = (float) Math.clamp((ALLY_RANGE - distance) / 6, 0, 1);
			allyMarker(g, screen[0], screen[1], size, alpha, Ow.toOw(ally.getHealth()), Ow.toOw(ally.getMaxHealth()));
		}
	}

	/** One ally's marker centred on (x, y): pips over a small chevron. */
	private static void allyMarker(GuiGraphicsExtractor g, float x, float y, float size, float alpha, float health, float max) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(size, size);
		// one pip per 25 health, at most 20 (a golem's 1250 would run off the screen: then each pip holds more)
		int pips = Math.clamp((int) Math.ceil(max / 25), 1, 20);
		float per = max / pips;
		float pipW = 6;
		float pitch = 8;
		float left = -(pips * pitch - (pitch - pipW)) / 2;
		for (int i = 0; i < pips; i++) {
			float held = Math.clamp((health - i * per) / per, 0, 1);
			float px = left + i * pitch;
			fill(g, px - 1, -11, pipW + 2, 12, alpha(0x000000, 0.45f * alpha));
			if (held > 0) {
				fill(g, px, -10, pipW * (held > 0.9f ? 1 : held), 10, alpha(0xFFFFFF, alpha));
			}
		}
		// the chevron under the pips, pointing down at the ally
		g.pose().pushMatrix();
		g.pose().translate(0, 8);
		g.pose().rotate((float) Math.toRadians(90));
		image(g, HudShapes.TRIANGLE, -5, -7, 10, 14, alpha(0x29A8E0, alpha));
		g.pose().popMatrix();
		g.pose().popMatrix();
	}

	/** World position to HUD position (reference pixels), or null behind the camera. */
	private static float @Nullable [] project(Vec3 at, float w, float h) {
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
			fill(g, x, y, bw, bh, 0xF2EAEAE9);
		} else {
			fill(g, x, y, bw, bh, SHADE);
			outline(g, x, y, bw, bh, 1.5f, edge);
		}
		HudText.draw(g, Face.SMALL, label, cx, cy + cap / 2, cap, inverted ? 0xFF111111 : edge, Align.CENTER);
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
	private static void outlined(GuiGraphicsExtractor g, Face face, String text, float x, float baseline, float cap, int color) {
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8;
			HudText.plain(g, face, text, x + (float) Math.cos(a) * 1.2f, baseline + (float) Math.sin(a) * 1.2f, cap, 0xCC000000,
				Align.CENTER);
		}
		HudText.plain(g, face, text, x, baseline, cap, color, Align.CENTER);
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

	private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
		image(g, HudShapes.DISC, cx - r, cy - r, 2 * r, 2 * r, color);
	}

	private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, int color) {
		image(g, HudShapes.RING, cx - r, cy - r, 2 * r, 2 * r, color);
	}
}
