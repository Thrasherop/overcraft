package dev.overcraft.client;

import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.hero.Perk;
import dev.overcraft.lifeweaver.LifeGrip;
import dev.overcraft.lifeweaver.Lifeweaver;
import dev.overcraft.lifeweaver.LifeweaverStats;
import dev.overcraft.net.GripPayload;
import dev.overcraft.net.HeroInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Lifeweaver on the client: his synced state (HeroManager.LIFEWEAVER), read for every Lifeweaver (his weapon, his
 * poses), and for the local one Rejuvenating Dash (Jump in the air: his client moves him, as Mercy's launches). And for
 * anyone a Lifeweaver's Life Grip has (GripPayload), the pull: their own client moves them to its point.
 */
public final class ClientLifeweaver {
	/** A Lifeweaver's synced state, parsed. */
	public record State(int flags, float charge, int bloomTarget, int gripTarget) {
		public boolean has(int flag) {
			return (flags & flag) != 0;
		}
	}

	private static boolean jumpWas;
	private static int airTicks;
	/** The dash under way: its velocity (blocks a tick) and ticks left (-1 none). */
	private static Vec3 dashVelocity = Vec3.ZERO;
	private static int dashLeft = -1;

	/** Life Grip on the local player: where it takes them, when it started (game time), from far (the slow start). */
	private static @Nullable Vec3 gripTo;
	private static long gripAt;
	private static boolean gripFar;

	private ClientLifeweaver() {
	}

	public static @Nullable State state(@Nullable Entity entity) {
		List<Integer> s = entity == null ? null : entity.getAttached(HeroManager.LIFEWEAVER);
		if (s == null || s.size() < 4) {
			return null;
		}
		return new State(s.get(0), s.get(1) / 100f, s.get(2), s.get(3));
	}

	/** Thorn Volley in hand (else Healing Blossom). */
	public static boolean thorns(Entity entity) {
		State s = state(entity);
		return s != null && s.has(Lifeweaver.FLAG_THORNS);
	}

	/**
	 * Each tick in hero mode: Rejuvenating Dash. Jump pressed in the air, from 0.1 s after leaving the ground, with the
	 * dash ready, sets him dashing here at once (D 36A2): 0.25 s at 28.875 m/s (x1.3 with Dashing Escape) along his keys
	 * (forward with none), level, his falling speed gone.
	 */
	public static int input(LocalPlayer player) {
		boolean jump = HeroControls.get(Action.JUMP).isDown();
		boolean pressed = jump && !jumpWas;
		jumpWas = jump;
		airTicks = player.onGround() ? 0 : airTicks + 1;
		if (ClientHero.hero() != Hero.LIFEWEAVER || !pressed || player.onGround() || airTicks < dev.overcraft.Ow.ticks(LifeweaverStats.DASH_AIR_DELAY)
			|| ClientHero.sync().ability3Cooldown() > 0 || dashLeft >= 0 || gripTo != null) {
			return 0;
		}
		double forward = (HeroControls.get(Action.MOVE_FORWARD).isDown() ? 1 : 0) - (HeroControls.get(Action.MOVE_BACK).isDown() ? 1 : 0);
		double left = (HeroControls.get(Action.MOVE_LEFT).isDown() ? 1 : 0) - (HeroControls.get(Action.MOVE_RIGHT).isDown() ? 1 : 0);
		if (forward == 0 && left == 0) {
			forward = 1;
		}
		double yaw = Math.toRadians(player.getYRot());
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		// Minecraft's frame: +Z ahead at yaw 0, +X to the left
		Vec3 dir = new Vec3(left * cos - forward * sin, 0, forward * cos + left * sin).normalize();
		Perk minor = Perk.byIndex(ClientHero.sync().minorPerk());
		double speed = LifeweaverStats.DASH_SPEED * (minor == Perk.LIFEWEAVER_DASHING_ESCAPE ? LifeweaverStats.DASHING_ESCAPE : 1);
		dashVelocity = dir.scale(dev.overcraft.Ow.speed(speed));
		dashLeft = dev.overcraft.Ow.ticks(LifeweaverStats.DASH_TIME);
		return HeroInputPayload.EXTRA_JUMP;
	}

	/**
	 * The dash's move this tick (OwMovement), or null. At its end the speed is halved (D 36A2's x0.5, L) and no more
	 * than his walk's (ours: kept at 14 m/s, the dash carried him 12.3 m from a jump; W measures 6.9 m in the air, and the
	 * data's 0.25 s at 28.875 m/s is 7.2 m, so little is left after it).
	 */
	public static @Nullable Vec3 dash(LivingEntity entity) {
		if (entity != Minecraft.getInstance().player || dashLeft < 0) {
			return null;
		}
		if (dashLeft-- == 0 || entity.horizontalCollision && dashLeft < dev.overcraft.Ow.ticks(LifeweaverStats.DASH_TIME) - 1) {
			dashLeft = -1;
			Vec3 after = dashVelocity.scale(LifeweaverStats.DASH_END);
			double walk = dev.overcraft.Ow.speed(OwMovement.RUN_SPEED);
			if (after.horizontalDistance() > walk) {
				after = after.scale(walk / after.horizontalDistance());
			}
			entity.setDeltaMovement(after);
			return null;
		}
		return dashVelocity;
	}

	// ---- Life Grip, on whoever it has ----

	public static void onGrip(GripPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		if (payload.on() && mc.player != null) {
			gripTo = new Vec3(payload.x(), payload.y(), payload.z());
			gripAt = mc.player.level().getGameTime();
			gripFar = payload.far();
			dashLeft = -1;
		} else {
			gripTo = null;
		}
	}

	/** Life Grip has the local player. */
	public static boolean gripped() {
		return gripTo != null;
	}

	/**
	 * Life Grip's move for the local player: held still until the pull starts (0.35 s after the cast), then to its point
	 * along its speed curve; it ends when the server says (arrival, Jump, the cap).
	 */
	public static OwMovement.@Nullable Glide grip(LivingEntity entity) {
		Minecraft mc = Minecraft.getInstance();
		if (entity != mc.player || gripTo == null) {
			return null;
		}
		long t = entity.level().getGameTime() - gripAt;
		// their Jump lets go from 0.65 s (D 21EE), here at once: waiting for the server's word took 0.4 s more (found
		// playing with a second player); the server ends it too, seeing the same Jump
		if (t >= dev.overcraft.Ow.ticks(LifeweaverStats.GRIP_JUMP_CANCEL) && mc.player.input.keyPresses.jump()) {
			gripTo = null;
			return null;
		}
		int moving = (int) (t - dev.overcraft.Ow.ticks(LifeweaverStats.GRIP_PULL_FROM));
		if (moving <= 0) {
			return new OwMovement.Glide(entity.position(), 0, 0);
		}
		return new OwMovement.Glide(gripTo, LifeGrip.speed(gripFar, moving), LifeweaverStats.GRIP_ARRIVE * 0.5);
	}

	/** Leaving the world: nothing carries over. */
	public static void clear() {
		gripTo = null;
		dashLeft = -1;
	}
}
