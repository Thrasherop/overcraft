package dev.overcraft.client.hud;

import dev.overcraft.client.HeroHud;
import dev.overcraft.hero.Hero;
import dev.overcraft.net.DeadeyePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.client.hud.HudDraw.image;

/**
 * Cassidy's Deadeye (dev.overcraft.cassidy.Cassidy, the server's locks: DeadeyePayload): on each target a red ring closing in as its
 * charge grows toward its health, and a skull once the shot would kill it, as Overwatch's (W: "a skull appears"). The look is
 * ours, from the HUD's own shapes (his ult's reticle art isn't read yet).
 */
public final class DeadeyeHud {
	private static final int RED = 0xFFE8384F;
	private static int[] targets = new int[0];
	private static float[] share = new float[0];

	private DeadeyeHud() {
	}

	public static void receive(DeadeyePayload payload) {
		targets = payload.targets();
		share = payload.share();
	}

	public static void render(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, float w, float h) {
		Minecraft mc = Minecraft.getInstance();
		if (hero != Hero.CASSIDY || mc.level == null || targets.length == 0) {
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		for (int i = 0; i < targets.length && i < share.length; i++) {
			Entity e = mc.level.getEntity(targets[i]);
			if (e == null || !e.isAlive() || e == player) {
				continue;
			}
			Vec3 at = e.getPosition(partial).add(0, e.getBbHeight() * 0.6, 0);
			float[] screen = HeroHud.project(at, w, h);
			if (screen == null) {
				continue;
			}
			float x = screen[0];
			float y = screen[1];
			float s = Math.min(1, share[i]);
			if (s >= 1) {
				image(g, HudShapes.DISC, x - 22, y - 22, 44, 44, 0xC01A1015);
				image(g, HudShapes.SKULL, x - 16, y - 16, 32, 32, RED);
				image(g, HudShapes.RING, x - 24, y - 24, 48, 48, RED);
			} else {
				// from 120 px across down to 48 as the charge fills
				float size = 120 - 72 * s;
				int alpha = (int) (0x60 + 0x9F * s);
				image(g, HudShapes.RING_THIN, x - size / 2, y - size / 2, size, size, alpha << 24 | (RED & 0xFFFFFF));
			}
		}
	}

	public static void clear() {
		targets = new int[0];
		share = new float[0];
	}
}
