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
 * puts him inside blocks (suffocating). It never shrinks to fit a gap: crawling under a ledge is Minecraft's own swimming
 * pose, as for anyone already stuck.
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

	/** Who has their hero's box now (the client's and the server's players apart; weak: players leave). */
	private static final Map<Player, Boolean> BIG = Collections.synchronizedMap(new WeakHashMap<>());

	/** The box this hero wants in this pose, or null for Minecraft's (other heroes, other poses). */
	public static @Nullable EntityDimensions of(@Nullable Hero hero, Pose pose) {
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
			return mine.orElse(null);
		}
		String id = player.getAttached(HeroManager.HERO);
		return id == null || !HeroManager.heroMode(player) || player.isSpectator() ? null : Hero.byId(id);
	}

	/** The box the player has now in this pose, or null for Minecraft's. */
	public static @Nullable EntityDimensions current(Player player, Pose pose) {
		return Boolean.TRUE.equals(BIG.get(player)) ? of(wanted(player), pose) : null;
	}

	/**
	 * Each tick (PlayerMixin): on when wanted and his crouched box fits where he is, off when not wanted; Minecraft's
	 * dimensions refreshed when it changes. True when it changed.
	 */
	public static boolean update(Player player) {
		Hero hero = wanted(player);
		boolean was = Boolean.TRUE.equals(BIG.get(player));
		EntityDimensions crouched = of(hero, Pose.CROUCHING);
		boolean now = crouched != null && (was || player.level().noCollision(player,
			crouched.makeBoundingBox(player.position()).deflate(1.0E-7)));
		if (now == was) {
			return false;
		}
		BIG.put(player, now);
		return true;
	}

	public static boolean big(Player player) {
		return Boolean.TRUE.equals(BIG.get(player));
	}
}
