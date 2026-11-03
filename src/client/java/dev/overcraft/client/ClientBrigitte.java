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
 * Brigitte on her own client: Shield Bash moves her at once (as Lifeweaver's dash: OwMovement), 0.256 s straight ahead at
 * 25.85 m/s with gravity on her, leaving at 15 m/s at most; the server's hit (she stops on it) or a wall ends it early
 * ("brigitte_bash_end"). The server times the bash from the same press (dev.overcraft.brigitte.Brigitte).
 */
public final class ClientBrigitte {
	private static boolean leftWas;
	private static Vec3 bashVelocity = Vec3.ZERO;
	/** Ticks of the bash left; -1 none; {@link #STOPPED} the server's hit stopped it (she stops on her target). */
	private static int bashLeft = -1;
	private static final int STOPPED = -2;
	/** Her vertical speed through the bash (blocks a tick): gravity goes on acting on her (BrigitteStats). */
	private static double bashVy;

	private ClientBrigitte() {
	}

	/** Each tick in hero mode: the left button pressed with the shield up (the right held) and the bash ready. */
	public static int input(LocalPlayer player) {
		boolean right = HeroControls.get(Action.PRIMARY_FIRE).isDown();
		if (right && !rightWas) {
			rightPressedAt = System.nanoTime();
		}
		rightWas = right;
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
		bashStartedAt = System.nanoTime();
		bashVy = player.onGround() ? 0 : player.getDeltaMovement().y;
		return 0;
	}

	/**
	 * The bash's move this tick (OwMovement), or null; at its end her speed is cut to 15 m/s (the steps that follow start
	 * from it), on a hit to nothing across.
	 */
	public static @Nullable Vec3 bash(LivingEntity entity) {
		if (entity != Minecraft.getInstance().player || bashLeft == -1) {
			return null;
		}
		if (bashLeft == STOPPED) {
			bashLeft = -1;
			entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
			return null;
		}
		if (bashLeft-- == 0 || entity.horizontalCollision && bashLeft < Ow.ticks(BrigitteStats.BASH_TIME) - 1) {
			bashLeft = -1;
			Vec3 v = entity.getDeltaMovement();
			double cap = Ow.speed(BrigitteStats.BASH_EXIT_SPEED);
			entity.setDeltaMovement(v.length() > cap ? v.normalize().scale(cap) : v);
			return null;
		}
		// her fall goes on (Overwatch's gravity, OwMovement's numbers); on the ground a touch down keeps her on it
		double g = Ow.speed(bashVy > 0 ? dev.overcraft.hero.OwMovement.GRAVITY_UP : dev.overcraft.hero.OwMovement.GRAVITY_DOWN) / 20;
		bashVy = entity.onGround() && bashVy <= 0 ? -g : bashVy - g;
		return new Vec3(bashVelocity.x, bashVy, bashVelocity.z);
	}

	/**
	 * Her barrier's camera wanted now (ClientHero, AbilityCamera): the server says it's up, or the right button went down
	 * this moment with the barrier able to go up (her own press foreseen, as the bash is: the server's word comes a tick or
	 * more later, and the view stayed in first person that long). Not for the first 0.35 s of a bash (0EE3 v68007).
	 */
	public static boolean barrierView(boolean up, boolean locked) {
		if (secondsSince(bashStartedAt) < BASH_VIEW_OFF) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		// not where the server will refuse it: emoting, asleep or stunned (Sleep's attachment), a screen open
		boolean refused = mc.player == null || mc.gui.screen() != null || mc.player.isSleeping()
			|| dev.overcraft.client.model.HeroAnimator.of(mc.player).emoting();
		return up || rightWas && !locked && !refused && ClientHero.sync().primaryAmmo() > 0 && secondsSince(rightPressedAt) < FORESEE;
	}

	private static double secondsSince(long nanos) {
		return nanos == 0 ? 1000 : (System.nanoTime() - nanos) / 1.0E9;
	}

	/** Off from a Shield Bash's start for this long (s: v68007, set 0.35 s on while v596 rises, 0EE3 #1042 -> #909, H). */
	static final double BASH_VIEW_OFF = 0.35;
	/** How long her own press stands for the server's word that the barrier went up (s, ours). */
	private static final double FORESEE = 0.3;
	private static boolean rightWas;
	private static long rightPressedAt, bashStartedAt;

	/** The server's word that the bash ended (a hit, a wall). */
	public static void ended(int entity) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getId() == entity && bashLeft >= 0) {
			bashLeft = STOPPED;
		}
	}

	public static void clear() {
		bashLeft = -1;
	}
}
