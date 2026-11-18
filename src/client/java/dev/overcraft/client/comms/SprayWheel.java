package dev.overcraft.client.comms;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.hud.HudDraw;
import dev.overcraft.hero.Hero;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * The spray wheel (#41; Overwatch's SPRAY UP / RIGHT / DOWN / LEFT and SPRAY MENU, its data's strings: tools/reference/sprays.md):
 * a tap of the spray key sprays the first; held past a quarter second, up to four of the hero's sprays round the crosshair, the
 * mouse points at one, letting go sprays it. The sprays are the hero's defaults from the player's own install, in the local pack
 * (textures/sprays/<hero>/); without them the portrait stands in.
 */
public final class SprayWheel {
	private static final long TAP_MS = 250;
	private static final float REACH = 200, POINTER_SPEED = 0.9f, SLOT = 150, THUMB = 112;
	/** Up, right, down, left (the data's order). */
	private static final float[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

	private static boolean held, open;
	private static long pressedAt;
	private static float pointerX, pointerY;
	private static List<String> sprays = List.of();

	private SprayWheel() {
	}

	public static boolean open() {
		return open;
	}

	/** Each client tick in hero mode. True while the spray key is held (the fire buttons stay the hero's). */
	public static boolean tick(Minecraft mc) {
		var key = HeroControls.get(Action.SPRAY);
		boolean clicked = false;
		while (key.consumeClick()) {
			clicked = true;
		}
		boolean down = key.isDown();
		long now = Util.getMillis();
		if (clicked && !down && !held) {
			// a tap pressed and let go between two ticks, which isDown never saw (found playing: a quick Y put nothing down)
			List<String> list = sprays();
			if (!list.isEmpty() && ClientPlayNetworking.canSend(dev.overcraft.net.SprayPayload.TYPE)) {
				ClientPlayNetworking.send(new dev.overcraft.net.SprayPayload(list.get(0)));
			}
			return false;
		}
		if (down && !held) {
			held = true;
			pressedAt = now;
			pointerX = pointerY = 0;
			sprays = sprays();
		}
		if (held && down && !open && now - pressedAt > TAP_MS && sprays.size() > 1) {
			open = true;
		}
		if (held && !down) {
			held = false;
			String name = open ? hovered() : sprays.isEmpty() ? null : sprays.get(0);
			open = false;
			if (name != null && ClientPlayNetworking.canSend(dev.overcraft.net.SprayPayload.TYPE)) {
				ClientPlayNetworking.send(new dev.overcraft.net.SprayPayload(name));
			}
		}
		return held;
	}

	public static void cancel() {
		held = open = false;
	}

	/** The mouse while it's open: the pointer, not the view (MouseHandlerMixin). */
	public static void move(double dx, double dy) {
		pointerX += (float) dx * POINTER_SPEED;
		pointerY += (float) dy * POINTER_SPEED;
		float length = (float) Math.hypot(pointerX, pointerY);
		if (length > REACH) {
			pointerX *= REACH / length;
			pointerY *= REACH / length;
		}
	}

	/** The current hero's four equipped sprays (the loadout's SPRAYS tab: Loadout.sprays), empty slots left out; else the portrait. */
	private static List<String> sprays() {
		Hero hero = ClientHero.hero();
		if (hero == null) {
			return List.of();
		}
		List<String> names = new ArrayList<>(Loadout.sprays(hero));
		names.removeIf(String::isEmpty);
		return names.isEmpty() ? List.of("portrait") : List.copyOf(names);
	}

	private static @Nullable String hovered() {
		if (Math.hypot(pointerX, pointerY) < 40) {
			return null;
		}
		int best = -1;
		float bestDot = -2;
		for (int i = 0; i < sprays.size(); i++) {
			float dot = (DIRS[i][0] * pointerX + DIRS[i][1] * pointerY) / (float) Math.hypot(pointerX, pointerY);
			if (dot > bestDot) {
				bestDot = dot;
				best = i;
			}
		}
		return best < 0 ? null : sprays.get(best);
	}

	/** Drawn over the HUD (reference pixels, 1920 x 1080). */
	public static void render(GuiGraphicsExtractor g, float w, float h) {
		if (!open) {
			return;
		}
		Hero hero = ClientHero.hero();
		String hot = hovered();
		float cx = w / 2, cy = h / 2;
		for (int i = 0; i < sprays.size(); i++) {
			String name = sprays.get(i);
			float x = cx + DIRS[i][0] * SLOT, y = cy + DIRS[i][1] * SLOT;
			boolean on = name.equals(hot);
			HudDraw.fill(g, x - THUMB / 2 - 6, y - THUMB / 2 - 6, THUMB + 12, THUMB + 12, on ? 0xC0FFFFFF : 0x80101418);
			Identifier tex = hero == null ? null : Identifier.fromNamespaceAndPath("overcraft", "textures/sprays/" + hero.id + "/" + name + ".png");
			if (tex != null && Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()) {
				// fitted in its slot at the image's own ratio (a 512 x 256 spray was squashed square)
				float r = dev.overcraft.client.model.SprayRenderer.ratio(tex);
				float tw = THUMB * Math.min(1, r), th = THUMB * Math.min(1, 1 / r);
				HudDraw.image(g, tex, x - tw / 2, y - th / 2, tw, th, 0xFFFFFFFF);
			}
		}
	}
}
