package dev.overcraft.client;

import dev.overcraft.Ow;
import dev.overcraft.brigitte.BrigitteStats;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.hero.Hero;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Brigitte on her own client: Shield Bash moves her at once (as Lifeweaver's dash: OwMovement), 0.3 s straight ahead at 4x
 * her speed, level; the server's hit or a wall ends it early ("brigitte_bash_end"). The server times the bash from the
 * same press (dev.overcraft.brigitte.Brigitte).
 */
public final class ClientBrigitte {
	private static boolean leftWas;
	private static Vec3 bashVelocity = Vec3.ZERO;
	private static int bashLeft = -1;

	private ClientBrigitte() {
	}

	/** Each tick in hero mode: the left button pressed with the shield up (the right held) and the bash ready. */
	public static int input(LocalPlayer player) {
		boolean left = HeroControls.get(Action.SECONDARY_FIRE).isDown();
		boolean pressed = left && !leftWas;
		leftWas = left;
		// the barrier's health is synced in the primary ammo's place (Brigitte.sync)
		if (ClientHero.hero() != Hero.BRIGITTE || !pressed || !HeroControls.get(Action.PRIMARY_FIRE).isDown()
			|| ClientHero.sync().ability2Cooldown() > 0 || ClientHero.sync().primaryAmmo() <= 0 || bashLeft >= 0) {
			return 0;
		}
		double yaw = Math.toRadians(player.getYRot());
		bashVelocity = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)).scale(Ow.speed(BrigitteStats.BASH_SPEED));
		bashLeft = Ow.ticks(BrigitteStats.BASH_TIME);
		return 0;
	}

	/** The bash's move this tick (OwMovement), or null; at its end she keeps her walk's speed. */
	public static @Nullable Vec3 bash(LivingEntity entity) {
		if (entity != Minecraft.getInstance().player || bashLeft < 0) {
			return null;
		}
		if (bashLeft-- == 0 || entity.horizontalCollision && bashLeft < Ow.ticks(BrigitteStats.BASH_TIME) - 1) {
			bashLeft = -1;
			return null;
		}
		return bashVelocity;
	}

	/** The server's word that the bash ended (a hit, a wall). */
	public static void ended(int entity) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getId() == entity) {
			bashLeft = -1;
		}
	}

	public static void clear() {
		bashLeft = -1;
	}
}
