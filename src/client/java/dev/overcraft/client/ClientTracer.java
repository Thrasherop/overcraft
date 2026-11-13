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
 * tick), as her movement is her client's; the press goes to the server, which takes the charge (Tracer.blink). Only a press that
 * moved her is sent: one that can't go plays its sound here and goes nowhere. Whether it can: the server's count less the
 * presses sent that it hasn't seen yet (synced back in the mode's high bits, Tracer.SEEN_SHIFT), so both sides keep one count
 * (critic d0c7e1f #1). Shift and the right button both blink (her data's ABILITY 1 and SECONDARY): both go as ABILITY 1. Recall
 * holds her still here while the server moves her (OwMovement.held).
 */
public final class ClientTracer {
	/** The Blink presses this client has sent (mod 256; -1: take the server's seen count first). */
	private static int sent = -1;
	/** The game time of her last Blink here (the 0.1 s lockout). */
	private static long lastBlink = Long.MIN_VALUE / 2;

	private ClientTracer() {
	}

	public static void reset() {
		sent = -1;
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
		// sent only when she went (the server takes a charge for each press it sees)
		return blink(player) ? pressed | HeroInputPayload.PRESS_ABILITY_1 : pressed & ~HeroInputPayload.PRESS_ABILITY_1;
	}

	private static boolean blink(LocalPlayer player) {
		dev.overcraft.net.HeroSyncPayload s = ClientHero.sync();
		int charges = s.secondaryAmmo();
		int seen = (s.mode() >>> Tracer.SEEN_SHIFT) & 0xFF;
		if (sent < 0 || Tracer.pending(sent, seen) > TracerStats.BLINK_CHARGES + 2) {
			// a fresh start, or the server's count restarted (a new hero state): take its count
			sent = seen;
		}
		long now = player.level().getGameTime();
		if (now - lastBlink < dev.overcraft.Ow.ticks(TracerStats.BLINK_LOCKOUT)) {
			// the 0.1 s after a Blink takes no press (D #153): no sound either
			return false;
		}
		boolean asleep = Boolean.TRUE.equals(player.getAttached(dev.overcraft.hero.HeroManager.ASLEEP));
		if (!Tracer.clientMay(charges, sent, seen) || (s.mode() & (Tracer.MODE_RECALL | Tracer.MODE_SILENCED)) != 0 || asleep
			|| player.isPassenger()) {
			// a press that can't go: its own sound (0024 #57 false -> #200 -> #77 effect 007F), here, as nothing goes to the server
			net.minecraft.sounds.SoundEvent empty = dev.overcraft.tracer.TracerSounds.of("blink_empty");
			if (empty != null) {
				Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(empty, 1));
			}
			return false;
		}
		sent = (sent + 1) & 0xFF;
		lastBlink = now;
		Vec3 dir = inputDirection(player);
		Vec3 to = Blink.destination(player.level(), player, dir != null ? dir : forward(player), TracerStats.BLINK_DISTANCE);
		Vec3 v = player.getDeltaMovement();
		player.setPos(to);
		// her level speed kept, her vertical speed gone (WS: vy 0 the tick she lands, then gravity again)
		player.setDeltaMovement(v.x, 0, v.z);
		player.resetFallDistance();
		return true;
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
