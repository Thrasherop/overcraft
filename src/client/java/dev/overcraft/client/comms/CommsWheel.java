package dev.overcraft.client.comms;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.hud.HudShapes;
import dev.overcraft.client.hud.HudText;
import dev.overcraft.client.hud.HudText.Align;
import dev.overcraft.client.hud.HudText.Face;
import dev.overcraft.comms.Callout;
import dev.overcraft.net.CalloutPayload;
import dev.overcraft.net.VoiceLinePayload;
import dev.overcraft.client.ClientHero;
import dev.overcraft.hero.Hero;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import static dev.overcraft.client.hud.HudDraw.fill;
import static dev.overcraft.client.hud.HudDraw.image;
import static dev.overcraft.client.hud.HudDraw.outline;

/**
 * Overwatch 2's communication wheel, for every hero (tools/reference/comms-reference.md, the owner's spec and
 * screenshot): hold the wheel's key (T) and it opens; the mouse moves a pointer from the centre instead of the view;
 * releasing the key plays the callout under the pointer (none in the centre). A quick tap plays "Good job". While it's
 * held, the left button turns to the voice lines page and the right to the emotes page (and back). The fire buttons
 * fire nothing meanwhile.
 */
public final class CommsWheel {
	/** Held longer than this (ms), the key opens the wheel; let go sooner, it's a tap (ours). */
	private static final long TAP_MS = 250;
	/** The pointer's dead centre and its reach, in reference pixels (ours, sized to the reference's ring). */
	private static final float DEAD = 70;
	private static final float REACH = 260;
	/** Mouse units to reference pixels (ours: about a third of a mouse sweep picks a slice). */
	private static final float POINTER_SPEED = 0.9f;

	public enum Page { COMMUNICATIONS, VOICE_LINES, EMOTES }

	private static boolean held;
	private static long pressedAt;
	private static boolean open;
	private static Page page = Page.COMMUNICATIONS;
	private static float pointerX;
	private static float pointerY;
	private static boolean leftWas;
	private static boolean rightWas;

	private CommsWheel() {
	}

	public static boolean open() {
		return open;
	}

	/**
	 * Each client tick in hero mode. True while the wheel has the key (held or open): the fire buttons are the wheel's
	 * then.
	 */
	public static boolean tick(Minecraft mc) {
		boolean down = HeroControls.get(Action.COMMS).isDown();
		long now = Util.getMillis();
		if (down && !held) {
			held = true;
			pressedAt = now;
			pointerX = pointerY = 0;
			page = Page.COMMUNICATIONS;
			leftWas = mc.mouseHandler.isLeftPressed();
			rightWas = mc.mouseHandler.isRightPressed();
		}
		if (held && down && !open && now - pressedAt > TAP_MS) {
			open = true;
		}
		if (held && open) {
			boolean left = mc.mouseHandler.isLeftPressed() || HeroControls.mouseButtonHeld(InputConstants.MOUSE_BUTTON_LEFT);
			boolean right = mc.mouseHandler.isRightPressed() || HeroControls.mouseButtonHeld(InputConstants.MOUSE_BUTTON_RIGHT);
			if (left && !leftWas) {
				page = page == Page.EMOTES ? Page.COMMUNICATIONS : Page.VOICE_LINES;
				pointerX = pointerY = 0;
			}
			if (right && !rightWas) {
				page = page == Page.VOICE_LINES ? Page.COMMUNICATIONS : Page.EMOTES;
				pointerX = pointerY = 0;
			}
			leftWas = left;
			rightWas = right;
		}
		if (held && !down) {
			held = false;
			if (!open) {
				send(Callout.GOOD_JOB);
			} else if (page == Page.COMMUNICATIONS) {
				Callout callout = hovered();
				if (callout != null) {
					send(callout);
				}
			} else if (page == Page.VOICE_LINES) {
				String line = hoveredLine();
				if (line != null && ClientPlayNetworking.canSend(VoiceLinePayload.TYPE)) {
					ClientPlayNetworking.send(new VoiceLinePayload(line));
				}
			}
			open = false;
		}
		return held;
	}

	/** The mouse while the wheel is open: it moves the pointer, not the view (MouseHandlerMixin). */
	public static void move(double dx, double dy) {
		pointerX += (float) dx * POINTER_SPEED;
		pointerY += (float) dy * POINTER_SPEED;
		float length = (float) Math.hypot(pointerX, pointerY);
		if (length > REACH) {
			pointerX *= REACH / length;
			pointerY *= REACH / length;
		}
	}

	/** The wheel closes with no callout (a screen opened, hero mode ended). */
	public static void cancel() {
		held = open = false;
	}

	/** The callout under the pointer: eight slices of 45°, clockwise from the top; none in the dead centre. */
	private static @Nullable Callout hovered() {
		if (Math.hypot(pointerX, pointerY) < DEAD) {
			return null;
		}
		double angle = Math.toDegrees(Math.atan2(pointerX, -pointerY));
		int slice = (int) Math.floorMod(Math.round(angle / 45), 8);
		return Callout.WHEEL[slice];
	}

	/** The voice page's line under the pointer: four quarters, up, right, down, left; none in the centre or empty. */
	private static @Nullable String hoveredLine() {
		Hero hero = ClientHero.hero();
		if (hero == null || Math.hypot(pointerX, pointerY) < DEAD) {
			return null;
		}
		double angle = Math.toDegrees(Math.atan2(pointerX, -pointerY));
		String id = Loadout.voiceLines(hero).get((int) Math.floorMod(Math.round(angle / 90), 4));
		return id.isEmpty() ? null : id;
	}

	private static void send(Callout callout) {
		if (ClientPlayNetworking.canSend(CalloutPayload.TYPE)) {
			ClientPlayNetworking.send(new CalloutPayload(callout.ordinal()));
		}
	}

	/**
	 * Drawn over the view in reference pixels (HeroHud's scale), centred at (cx, cy): the view dimmed, a white ring at
	 * the centre (about 200 px across), thin dividers between the slices, each callout's label in white caps round it
	 * (the hovered one brighter and larger), and the page tabs on the screen's edges.
	 */
	public static void render(GuiGraphicsExtractor g, float w, float h) {
		if (!open) {
			return;
		}
		float cx = w / 2;
		float cy = h / 2;
		fill(g, 0, 0, w, h, 0x59000000);
		image(g, HudShapes.RING_THIN, cx - 100, cy - 100, 200, 200, 0xF2FFFFFF);
		if (page == Page.COMMUNICATIONS) {
			Callout over = hovered();
			for (int i = 0; i < 8; i++) {
				// the dividers sit between the slices, from the ring outward
				double between = Math.toRadians(i * 45 + 22.5);
				for (float r = 110; r < 330; r += 4) {
					fill(g, cx + (float) Math.sin(between) * r, cy - (float) Math.cos(between) * r, 1.5f, 1.5f, 0x66FFFFFF);
				}
				Callout callout = Callout.WHEEL[i];
				double at = Math.toRadians(i * 45);
				float lx = cx + (float) Math.sin(at) * 255;
				float ly = cy - (float) Math.cos(at) * 235;
				boolean hot = callout == over;
				String label = Component.translatable("overcraft.wheel." + callout.id).getString().toUpperCase(Locale.ROOT);
				HudText.draw(g, Face.BODY, label, lx, ly + 7, hot ? 19 : 16, hot ? 0xFFFFFFFF : 0xE6E6EAEE, Align.CENTER);
				if (hot) {
					fill(g, lx - HudText.width(Face.BODY, label, 19) / 2, ly + 14, HudText.width(Face.BODY, label, 19), 3, 0xFFF99E1A);
				}
			}
		} else if (page == Page.VOICE_LINES) {
			voicePage(g, cx, cy);
		} else {
			String note = Component.translatable("overcraft.wheel.emotes").getString().toUpperCase(Locale.ROOT);
			HudText.draw(g, Face.BODY, note, cx, cy - 130, 18, 0xFFFFFFFF, Align.CENTER);
		}
		// the page tabs: a white bar with the mouse button that goes there
		if (page != Page.VOICE_LINES) {
			tab(g, 0, cy, page == Page.EMOTES ? "overcraft.wheel.communications" : "overcraft.wheel.voice_lines", true);
		}
		if (page != Page.EMOTES) {
			tab(g, w, cy, page == Page.VOICE_LINES ? "overcraft.wheel.communications" : "overcraft.wheel.emotes", false);
		}
		// where the pointer is (ours: Overwatch shows only the highlight)
		image(g, HudShapes.DISC, cx + pointerX - 5, cy + pointerY - 5, 10, 10, 0xCCFFFFFF);
	}

	/**
	 * The voice page (the owner's screenshot): the four equipped lines at the compass points, each name in white caps in
	 * a dark box with a light outline, diagonal dividers between them; the one under the pointer brighter.
	 */
	private static void voicePage(GuiGraphicsExtractor g, float cx, float cy) {
		Hero hero = ClientHero.hero();
		if (hero == null) {
			return;
		}
		for (int i = 0; i < 4; i++) {
			double between = Math.toRadians(i * 90 + 45);
			for (float r = 110; r < 330; r += 4) {
				fill(g, cx + (float) Math.sin(between) * r, cy - (float) Math.cos(between) * r, 1.5f, 1.5f, 0x66FFFFFF);
			}
		}
		List<String> lines = Loadout.voiceLines(hero);
		String over = hoveredLine();
		float[][] at = {{0, -182}, {255, 0}, {0, 182}, {-255, 0}};
		for (int i = 0; i < 4; i++) {
			String id = lines.get(i);
			String name = id.isEmpty() ? "-" : Component.translatable("overcraft.voiceline." + hero.id + "." + id).getString();
			name = name.toUpperCase(Locale.ROOT);
			boolean hot = !id.isEmpty() && id.equals(over);
			float cap = 15;
			float width = HudText.width(Face.BODY, name, cap) + 28;
			float x = cx + at[i][0] - width / 2;
			float y = cy + at[i][1] - 21;
			fill(g, x, y, width, 42, hot ? 0xE6283846 : 0xCC10161C);
			outline(g, x, y, width, 42, 1.5f, hot ? 0xFFFFFFFF : 0x99C8D0D8);
			HudText.draw(g, Face.BODY, name, x + width / 2, y + 21 + cap / 2, cap, 0xFFFFFFFF, Align.CENTER);
		}
	}

	private static void tab(GuiGraphicsExtractor g, float edge, float cy, String key, boolean left) {
		String label = Component.translatable(key).getString().toUpperCase(Locale.ROOT);
		float width = 280;
		float x = left ? edge : edge - width;
		fill(g, x, cy - 30, width, 60, 0xF2F4F4F4);
		HudText.draw(g, Face.BODY, label, left ? x + 16 : x + width - 16, cy + 7, 17, 0xFF15181C, left ? Align.LEFT : Align.RIGHT);
		float mx = left ? x + width + 30 : x - 30;
		float mw = 26;
		float mh = mw * HudShapes.MOUSE_H / HudShapes.MOUSE_W;
		image(g, HudShapes.MOUSE, mx - mw / 2, cy - mh / 2, mw, mh, 0xFFF4F4F4);
		image(g, left ? HudShapes.MOUSE_LEFT : HudShapes.MOUSE_RIGHT, mx - mw / 2, cy - mh / 2, mw, mh, 0xFFE8632A);
	}
}
