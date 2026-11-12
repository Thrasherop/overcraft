package dev.overcraft.client;

import dev.overcraft.hero.Hero;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.tracer.Blink;
import dev.overcraft.tracer.Tracer;
import dev.overcraft.tracer.TracerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Tracer on her own client: a Blink moves her here, the tick she presses (WS 2026-11-11: she's 7.5 m on within the press's
 * tick), as her movement is her client's; the press goes to the server, which takes the charge (Tracer.blink). Shift and the
 * right button both blink (her data's ABILITY 1 and SECONDARY): both go as ABILITY 1. Recall holds her still here while the
 * server moves her (OwMovement.held).
 */
public final class ClientTracer {
	/** Blinks this client made that the server's count doesn't show yet, and that count when it last changed. */
	private static int spent;
	private static int syncedCharges = -1;
	/** The game time of her last Blink here (the 0.1 s lockout). */
	private static long lastBlink = Long.MIN_VALUE / 2;

	private ClientTracer() {
	}

	public static void reset() {
		spent = 0;
		syncedCharges = -1;
		lastBlink = Long.MIN_VALUE / 2;
	}

	/** ClientHero's presses this tick: a Blink press (Shift, or the right button's new press) blinks her now. */
	public static int input(LocalPlayer player, int pressed, boolean rightPressed) {
		if (ClientHero.hero() != Hero.TRACER) {
			return pressed;
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) == 0 && !rightPressed) {
			return pressed;
		}
		blink(player);
		return pressed | HeroInputPayload.PRESS_ABILITY_1;
	}

	private static void blink(LocalPlayer player) {
		dev.overcraft.net.HeroSyncPayload s = ClientHero.sync();
		int charges = s.secondaryAmmo();
		if (charges != syncedCharges) {
			syncedCharges = charges;
			spent = 0;
		}
		long now = player.level().getGameTime();
		boolean asleep = Boolean.TRUE.equals(player.getAttached(dev.overcraft.hero.HeroManager.ASLEEP));
		if (charges - spent <= 0 || now - lastBlink < dev.overcraft.Ow.ticks(TracerStats.BLINK_LOCKOUT)
			|| (s.mode() & (Tracer.MODE_RECALL | Tracer.MODE_SILENCED)) != 0 || asleep || player.isPassenger()) {
			return;
		}
		spent++;
		lastBlink = now;
		Vec3 dir = inputDirection(player);
		Vec3 to = Blink.destination(player.level(), player, dir != null ? dir : forward(player), TracerStats.BLINK_DISTANCE);
		Vec3 v = player.getDeltaMovement();
		player.setPos(to);
		// her level speed kept, her vertical speed gone (WS: vy 0 the tick she lands, then gravity again)
		player.setDeltaMovement(v.x, 0, v.z);
		player.resetFallDistance();
	}

	private static Vec3 forward(LocalPlayer player) {
		double yaw = Math.toRadians(player.getYRot());
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	/** Her move keys as a level direction in the world, or null with none held (forward then: D 0024 #165). */
	private static Vec3 inputDirection(LocalPlayer player) {
		Vec2 move = player.input.getMoveVector();
		if (move.lengthSquared() < 1.0E-6f) {
			return null;
		}
		double yaw = Math.toRadians(player.getYRot());
		Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		return fwd.scale(move.y).add(left.scale(move.x)).normalize();
	}

	/** OwMovement.held: Recall holds her own player still. */
	public static boolean held(LivingEntity entity) {
		return entity == Minecraft.getInstance().player && ClientHero.hero() == Hero.TRACER
			&& (ClientHero.sync().mode() & Tracer.MODE_RECALL) != 0;
	}
}
