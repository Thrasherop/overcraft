package dev.overcraft.client;

import dev.overcraft.Ow;
import dev.overcraft.cassidy.Cassidy;
import dev.overcraft.cassidy.CassidyStats;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.net.HeroInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Cassidy on his own client: Combat Roll moves him at once (as Lifeweaver's dash and Brigitte's bash: OwMovement.dash), 0.4 s along
 * his move keys (his facing with none) at 15 m/s, x1.3 off the ground, his fall going on, leaving at 5.5 m/s at most (D 01F0,
 * cassidy-map.md). Its direction goes with the press (HeroInputPayload.extra: 1 + 0 forward / 1 right / 2 back / 3 left) for the
 * server's clip. The server times the damage cut and the reload from the same press (dev.overcraft.cassidy.Cassidy).
 */
public final class ClientCassidy {
	/** The roll: its level direction (unit), ticks left (-1 none), its vertical speed (blocks a tick). */
	private static Vec3 rollDir = Vec3.ZERO;
	private static int rollLeft = -1;
	private static double rollVy;

	private ClientCassidy() {
	}

	public static void reset() {
		rollLeft = -1;
	}

	/** ClientHero's presses this tick: Shift with the roll ready starts it here; the extra byte carries its direction. */
	public static int extra(LocalPlayer player, int pressed) {
		if (ClientHero.hero() != Hero.CASSIDY || (pressed & HeroInputPayload.PRESS_ABILITY_1) == 0 || rollLeft >= 0) {
			return 0;
		}
		dev.overcraft.net.HeroSyncPayload s = ClientHero.sync();
		int blocked = Cassidy.MODE_DEADEYE | Cassidy.MODE_ROLL | Cassidy.MODE_HINDERED | Cassidy.MODE_SILENCED | Cassidy.MODE_CASTING;
		boolean asleep = Boolean.TRUE.equals(player.getAttached(dev.overcraft.hero.HeroManager.ASLEEP));
		if (s.ability1Cooldown() > 0 || (s.mode() & blocked) != 0 || asleep || player.isPassenger()) {
			return 0;
		}
		double yaw = Math.toRadians(player.getYRot());
		Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		Vec2 move = player.input.getMoveVector();
		int dir = 0;
		if (move.lengthSquared() < 1.0E-6f) {
			rollDir = fwd;
		} else {
			rollDir = fwd.scale(move.y).add(left.scale(move.x)).normalize();
			// the clip by the key that leads (ours: 0 forward, 1 right, 2 back, 3 left)
			dir = Math.abs(move.y) >= Math.abs(move.x) ? (move.y >= 0 ? 0 : 2) : (move.x > 0 ? 3 : 1);
		}
		rollLeft = Ow.ticks(CassidyStats.ROLL_TIME);
		rollVy = player.onGround() ? 0 : player.getDeltaMovement().y;
		return 1 + dir;
	}

	public static boolean rolling(LivingEntity entity) {
		return entity == Minecraft.getInstance().player && rollLeft >= 0;
	}

	/** The roll's move this tick (OwMovement.dash), or null; at its end his level speed is cut to 5.5 m/s. */
	public static @Nullable Vec3 roll(LivingEntity entity) {
		if (entity != Minecraft.getInstance().player || rollLeft < 0) {
			return null;
		}
		if (ClientHero.hero() != Hero.CASSIDY || !ClientHero.controls()) {
			rollLeft = -1;
			return null;
		}
		if (rollLeft-- == 0) {
			rollLeft = -1;
			Vec3 v = entity.getDeltaMovement();
			Vec3 flat = new Vec3(v.x, 0, v.z);
			double cap = Ow.speed(CassidyStats.ROLL_EXIT_SPEED);
			if (flat.length() > cap) {
				flat = flat.normalize().scale(cap);
			}
			entity.setDeltaMovement(flat.x, v.y, flat.z);
			return null;
		}
		boolean ground = entity.onGround();
		double speed = Ow.speed(CassidyStats.ROLL_SPEED * (ground ? 1 : CassidyStats.ROLL_AIR));
		// his fall goes on (Overwatch's gravity, OwMovement's numbers); on the ground a touch down keeps him on it
		double g = Ow.speed(rollVy > 0 ? OwMovement.GRAVITY_UP : OwMovement.GRAVITY_DOWN) / 20;
		rollVy = ground && rollVy <= 0 ? -g : rollVy - g;
		return new Vec3(rollDir.x * speed, rollVy, rollDir.z * speed);
	}
}
