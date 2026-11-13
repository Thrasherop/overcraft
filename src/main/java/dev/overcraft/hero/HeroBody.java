package dev.overcraft.hero;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A hero's own body box, where it isn't Minecraft's 0.6 x 1.8 (the owner, 2026-11-02: "Sigma should be unable to fit
 * through a door; that's funny. And the camera height should match"). Sigma's is his mover's (D 2AB1: 2.5 m tall, 0.7 m
 * radius = 1.4 m wide, crouched 2.05 m), with his camera at his model's eye (the head hardpoint 0076 in his stand: 2.06 m).
 * Only in hero mode (G off: Minecraft's box), on every side (the hero and the mode are synced attachments).
 * <p>
 * The box grows only where it fits (ours): picking him or pressing G in a 2-high tunnel keeps Minecraft's box until he
 * steps out to where at least his crouched box fits (Minecraft then crouches him if only that fits), so a switch never
 * puts him inside blocks (suffocating). Put where even his crouched box doesn't fit (a respawn beside a bed, a /tp), he has
 * Minecraft's box until he steps out, as after G in a tunnel.
 */
public final class HeroBody {
	private HeroBody() {
	}

	/**
	 * Sigma's box: 1.4 x 2.5 m, eye 2.06 m (D 2AB1 mover height 2.5 / radius 0.7, H; the eye: hardpoint 0076 on bone_0011 in
	 * his stand clip 6E65's first frame, 2.062 m over his feet, M: Overwatch's camera height isn't a published number).
	 */
	public static final EntityDimensions SIGMA_STANDING = EntityDimensions.scalable(1.4f, 2.5f).withEyeHeight(2.06f);
	/**
	 * Crouched: 2.05 m tall (D 2AB1 crouch height, H), the eye as far under the top as standing (1.61 m: ours, M; he has no
	 * crouch clips, his model stands).
	 */
	public static final EntityDimensions SIGMA_CROUCHING = EntityDimensions.scalable(1.4f, 2.05f).withEyeHeight(1.61f);

	/**
	 * D.Va's MEKA: its mover's 1.4 x 2.3 m, the camera at its first-person eye 1.74 m (D 0407 mover height 2.3 / radius 0.7, H;
	 * 041F's eye field 1.74 standing and crouched, H field, M meaning). It can't crouch (no crouch in any mech graph, D 0276
	 * #246): its crouched box is the same. Only in the mech's body (dev.overcraft.dva.Dva.mechBody): the pilot is Minecraft's.
	 */
	public static final EntityDimensions DVA_MECH = EntityDimensions.scalable(dev.overcraft.dva.DvaStats.MECH_WIDTH,
		dev.overcraft.dva.DvaStats.MECH_HEIGHT).withEyeHeight(dev.overcraft.dva.DvaStats.MECH_EYE);

	/** Who has their hero's box now (the client's and the server's players apart; weak: players leave). */
	private static final Map<Player, Boolean> BIG = Collections.synchronizedMap(new WeakHashMap<>());

	/** The box this hero wants in this pose, or null for Minecraft's (other heroes, other poses). */
	public static @Nullable EntityDimensions of(@Nullable Hero hero, Pose pose) {
		if (hero == Hero.DVA) {
			return pose == Pose.STANDING || pose == Pose.CROUCHING ? DVA_MECH : null;
		}
		if (hero != Hero.SIGMA) {
			return null;
		}
		return pose == Pose.STANDING ? SIGMA_STANDING : pose == Pose.CROUCHING ? SIGMA_CROUCHING : null;
	}

	/**
	 * The client's own player: its hero in hero mode as the client knows it (ClientHero, as the client's movement reads it),
	 * or null for any other player. Set by the client; its own player switched boxes late from the attachments alone (G
	 * pressed: the server's box changed, the client's stayed: found playing).
	 */
	public static java.util.function.Function<Player, java.util.Optional<Hero>> local = player -> null;

	/** The hero this player shows in hero mode, from the synced attachments (null: none, or Minecraft mode). */
	public static @Nullable Hero wanted(Player player) {
		java.util.Optional<Hero> mine = local.apply(player);
		if (mine != null) {
			Hero hero = mine.orElse(null);
			return hero == Hero.DVA && !dev.overcraft.dva.Dva.mechBody(player) ? null : hero;
		}
		String id = player.getAttached(HeroManager.HERO);
		Hero hero = id == null || !HeroManager.heroMode(player) || player.isSpectator() ? null : Hero.byId(id);
		// D.Va's box is the mech's only in its body (the pilot, a call: Minecraft's)
		return hero == Hero.DVA && !dev.overcraft.dva.Dva.mechBody(player) ? null : hero;
	}

	/** The box the player has now in this pose, or null for Minecraft's. */
	public static @Nullable EntityDimensions current(Player player, Pose pose) {
		return Boolean.TRUE.equals(BIG.get(player)) ? of(wanted(player), pose) : null;
	}

	/**
	 * Each tick (PlayerMixin): on while wanted and his crouched box fits where he is, off when not wanted or when he's been
	 * put where it doesn't fit (a respawn beside the bed, a /tp: Minecraft places a player by its own 0.6 x 1.8 box; with his
	 * kept, Minecraft laid him down in its swimming pose with a 0.4 m camera until he walked out: found in the release play,
	 * 2026-11-02). Walking never gets him there, since his box collides first. Minecraft's dimensions refreshed when it
	 * changes. True when it changed.
	 */
	public static boolean update(Player player) {
		Hero hero = wanted(player);
		boolean was = Boolean.TRUE.equals(BIG.get(player));
		EntityDimensions crouched = of(hero, Pose.CROUCHING);
		boolean now = crouched != null && player.level().noCollision(player,
			crouched.makeBoundingBox(player.position()).deflate(1.0E-7));
		if (now == was) {
			return false;
		}
		BIG.put(player, now);
		return true;
	}

	/**
	 * On the server, each tick: a hero in hero mode whose box doesn't fit where he is (G pressed in a doorway, a respawn
	 * beside the bed, a /tp) is moved to the nearest spot within 2 m where his crouched box fits, or, with none, put back
	 * in Minecraft mode with a word why. Otherwise G in a 1-wide doorway gave Sigma a 0.6 m box that walked him through
	 * it and along any 1-wide corridor (the critique's #15), undoing the owner's "unable to fit through a door".
	 */
	public static void makeRoom(net.minecraft.server.level.ServerPlayer player) {
		Hero hero = wanted(player);
		EntityDimensions crouched = of(hero, Pose.CROUCHING);
		if (crouched == null || big(player) || player.isPassenger() || player.isSpectator() || !player.isAlive()
			|| fits(player, crouched, player.position())) {
			return;
		}
		net.minecraft.world.phys.Vec3 at = player.position();
		net.minecraft.world.phys.Vec3 best = null;
		double bestD = Double.MAX_VALUE;
		for (double dy : new double[] {0, 0.5, 1.0}) {
			for (double dx = -2; dx <= 2.001; dx += 0.25) {
				for (double dz = -2; dz <= 2.001; dz += 0.25) {
					double d = dx * dx + dz * dz + dy * dy;
					if (d >= bestD || d > 4.0) {
						continue;
					}
					net.minecraft.world.phys.Vec3 to = at.add(dx, dy, dz);
					if (fits(player, crouched, to) && !player.level().noCollision(player,
						crouched.makeBoundingBox(to).move(0, -1.0, 0).deflate(0.05))) {
						// room, and ground within a metre under it
						best = to;
						bestD = d;
					}
				}
			}
		}
		if (best != null) {
			player.connection.teleport(best.x, best.y, best.z, player.getYRot(), player.getXRot());
		} else {
			HeroManager.setHeroMode(player, false);
			player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("overcraft.hero.no_room",
				hero.displayName), true);
		}
	}

	private static boolean fits(Player player, EntityDimensions box, net.minecraft.world.phys.Vec3 at) {
		return player.level().noCollision(player, box.makeBoundingBox(at).deflate(1.0E-7));
	}

	public static boolean big(Player player) {
		return Boolean.TRUE.equals(BIG.get(player));
	}
}
