package dev.overcraft.client.hud;

import dev.overcraft.client.HeroArt;
import dev.overcraft.client.HeroHud;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.ConspiratorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.client.hud.HudDraw.image;

/**
 * Sombra's Conspirator (dev.overcraft.sombra.Conspirator): the ult icon of each enemy whose ultimate is ready, over their
 * head, through walls (ours: Overwatch shows "an ult icon above those enemies", W). The enemy's red ring around their
 * ult's icon on a dark disc, the size of a small ping.
 */
public final class ConspiratorHud {
	private static final int RED = 0xFFE8384F;
	private static int[] enemies = new int[0];

	private ConspiratorHud() {
	}

	public static void receive(ConspiratorPayload payload) {
		enemies = payload.enemies();
	}

	public static void render(GuiGraphicsExtractor g, LocalPlayer player, Hero hero, float w, float h) {
		Minecraft mc = Minecraft.getInstance();
		if (hero != Hero.SOMBRA || mc.level == null || enemies.length == 0) {
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		for (int id : enemies) {
			Entity e = mc.level.getEntity(id);
			if (!(e instanceof Player enemy) || !enemy.isAlive() || enemy == player) {
				continue;
			}
			Hero theirs = HeroManager.hero(enemy);
			Vec3 at = enemy.getPosition(partial).add(0, enemy.getBbHeight() + 0.75, 0);
			float[] screen = HeroHud.project(at, w, h);
			if (screen == null) {
				continue;
			}
			float x = screen[0];
			float y = screen[1];
			image(g, HudShapes.DISC, x - 18, y - 18, 36, 36, 0xE61A1015);
			Identifier icon = theirs == null ? null : HeroArt.ultimate(theirs);
			if (icon != null) {
				image(g, icon, x - 14, y - 14, 28, 28, 0xFFFFFFFF);
			}
			image(g, HudShapes.RING, x - 19, y - 19, 38, 38, RED);
		}
	}

	public static void clear() {
		enemies = new int[0];
	}
}
