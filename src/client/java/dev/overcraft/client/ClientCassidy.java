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
 * server's clip. The server times the damage cut and the reload from the same press (dev.overcraft.cassidy.Cassidy). One authority
 * (Tracer's Blink's way, critic b79a4d1 #1): he rolls here only when the last sync says the server will take it and the server has
 * seen every E, Q and roll sent (its count rides the mode's high bits); the server rolls only for a roll sent; one it can't take
 * (a hinder or a hack landing in the round trip) it counts as cut, and the roll here stops.
 */
public final class ClientCassidy {
	/** The roll: its level direction (unit), ticks left (-1 none), its vertical speed (blocks a tick). */
	private static Vec3 rollDir = Vec3.ZERO;
	private static int rollLeft = -1;
	private static double rollVy;

	private ClientCassidy() {
	}

	/** Packets sent that count (Cassidy.counts; mod 256, -1: take the server's count first), the server's roll cuts last seen. */
	private static int sent = -1, cutSeen = -1;
	/** Game time pending first looked stuck (the server's count restarted), or MIN. */
	private static long stuckSince = Long.MIN_VALUE;

	public static void reset() {
		rollLeft = -1;
		sent = -1;
		cutSeen = -1;
		stuckSince = Long.MIN_VALUE;
	}

	/** The server's counts from the last sync: a roll it refused or cut stops his here (critic b79a4d1 #1). */
	private static void follow(LocalPlayer player) {
		int mode = ClientHero.sync().mode();
		int seen = (mode >>> Cassidy.SEEN_SHIFT) & 0xFF, cut = (mode >>> Cassidy.CUT_SHIFT) & 0xFF;
		long now = player.level().getGameTime();
		if (sent < 0) {
			sent = seen;
		} else if (Cassidy.pending(sent, seen) != 0) {
			// a count that never comes (the server's restarted: a new hero state) is taken after 2 s
			if (stuckSince == Long.MIN_VALUE) {
				stuckSince = now;
			} else if (now - stuckSince > 40) {
				sent = seen;
				stuckSince = Long.MIN_VALUE;
			}
		} else {
			stuckSince = Long.MIN_VALUE;
		}
		if (cutSeen < 0) {
			cutSeen = cut;
		} else if (cut != cutSeen) {
			cutSeen = cut;
			if (rollLeft >= 0) {
				stop(player);
			}
		}
	}

	/** His roll stops here, his level speed cut to 5.5 m/s as at its end. */
	private static void stop(LivingEntity entity) {
		rollLeft = -1;
		Vec3 v = entity.getDeltaMovement();
		Vec3 flat = new Vec3(v.x, 0, v.z);
		double cap = Ow.speed(CassidyStats.ROLL_EXIT_SPEED);
		if (flat.length() > cap) {
			flat = flat.normalize().scale(cap);
		}
		entity.setDeltaMovement(flat.x, v.y, flat.z);
	}

	/** Deadeye on his own client (the last sync): no jump, no crouch (KeyboardInputMixin; W rev 409375). */
	public static boolean inDeadeye() {
		return ClientHero.hero() == Hero.CASSIDY && ClientHero.controls() && (ClientHero.sync().mode() & Cassidy.MODE_DEADEYE) != 0;
	}

	/**
	 * ClientHero's presses this tick, just before they're sent: Shift with the roll ready starts it here and the extra byte carries its
	 * direction (the server rolls only for that); the packet is counted as the server counts it (Cassidy.input).
	 */
	public static int extra(LocalPlayer player, int pressed) {
		if (ClientHero.hero() != Hero.CASSIDY) {
			sent = -1;
			cutSeen = -1;
			return 0;
		}
		follow(player);
		int out = roll(player, pressed);
		if (Cassidy.counts(pressed, out)) {
			sent = (sent + 1) & 0xFF;
		}
		return out;
	}

	private static int roll(LocalPlayer player, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) == 0) {
			return 0;
		}
		dev.overcraft.net.HeroSyncPayload s = ClientHero.sync();
		int seen = (s.mode() >>> Cassidy.SEEN_SHIFT) & 0xFF;
		boolean asleep = Boolean.TRUE.equals(player.getAttached(dev.overcraft.hero.HeroManager.ASLEEP));
		if (!Cassidy.clientMay(Cassidy.pending(sent, seen), s.ability1Cooldown(), s.mode(), asleep, player.isPassenger(), rollLeft >= 0)) {
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
		if (entity instanceof LocalPlayer player) {
			// a cut the server sent since: stop now, not at the next press
			follow(player);
			if (rollLeft < 0) {
				return null;
			}
		}
		if (rollLeft-- == 0) {
			stop(entity);
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
