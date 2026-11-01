package dev.overcraft.hero;

import dev.overcraft.Ow;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shields (Lifeweaver's 50: the first hero with them), and armor (Brigitte's 75) the same way: {@link Hero#layer()}, kept in
 * {@link HeroState#shield}; armor never comes back by itself, and takes each hit 7 smaller, at most halved (armorReduce).
 * In Overwatch they're the top of the health bar: damage takes
 * overhealth, then shields, then health; healing fills health first, then shields (health, armor and shields are all
 * recoverable); and shields come back by themselves 3 s after the last damage, at 30 a second (W Hit points). Ours had
 * healing stop under them: a dash's heal after a hit on his shields alone healed nothing (found playing). Here they're the top {@link Hero#shields()} of the hero's Minecraft health
 * (Lifeweaver's 250 = 20: the top 4), with what's left of them kept in {@link HeroState#shield}: every change to the
 * health (LivingEntityMixin, setHealth) takes a loss off them first, and a gain fills the health under them, then them.
 * Overhealth (absorption) is Minecraft's and goes before either. They hold whatever the mode (G): the hero's health
 * does.
 */
public final class Shields {
	private Shields() {
	}

	/** Every hero's shields come back this long after the last damage, at this rate (a second). */
	public static final double DELAY = 3;                      // W (Hit points)
	public static final double REGEN = 30;                     // W (Hit points)

	/** True while the shields' own regeneration sets the health (its gain isn't healing). */
	private static boolean regenerating;

	/** The health a shielded hero's setHealth may really set. */
	public static float onSetHealth(ServerPlayer player, float health) {
		HeroState s = HeroManager.state(player);
		if (s == null || s.hero == null || s.hero.layer() + s.extraArmor == 0 || regenerating) {
			return health;
		}
		double layer = s.hero.layer() + s.extraArmor;
		float old = player.getHealth();
		if (health < old) {
			// a loss comes off the top: the shields first
			s.shield = Math.max(0, s.shield - Ow.toOw(old - health));
			return health;
		}
		if (health <= old) {
			return health;
		}
		// a gain heals the health under the shields first, then the shields
		double[] split = heal(Ow.toOw(old) - s.shield, s.shield, Ow.toOw(player.getMaxHealth()) - layer, layer,
			Ow.toOw(Math.min(health, player.getMaxHealth()) - old));
		s.shield += split[1];
		return old + Ow.hp(split[0] + split[1]);
	}

	/**
	 * How a heal of {@code gain} splits (Overwatch points): {health, shields}, the health under the shields first up to
	 * {@code healthMax}, then the shields up to {@code shieldMax}.
	 */
	public static double[] heal(double health, double shield, double healthMax, double shieldMax, double gain) {
		double toHealth = Math.max(0, Math.min(gain, healthMax - health));
		double toShield = Math.max(0, Math.min(gain - toHealth, shieldMax - shield));
		return new double[] {toHealth, toShield};
	}

	/** Armor takes a hit this much smaller: 7 (Overwatch points), at most half of it (W Hit points: Armor; BZ 2024-09-17). */
	public static final double ARMOR_FLAT = 7;
	public static final double ARMOR_CAP = 0.5;

	/**
	 * A hit (Minecraft health) on a hero with armor left: 7 smaller, at most halved (ours: the whole hit, where Overwatch reduces
	 * only the part that lands on the armor, M; beams' -30% and damage over time not told apart yet).
	 */
	public static float armorReduce(ServerPlayer player, float damage) {
		HeroState s = HeroManager.state(player);
		if (s == null || s.hero == null || s.hero.armor() == 0 || s.shield <= 0 || damage <= 0) {
			return damage;
		}
		double ow = Ow.toOw(damage);
		return Ow.hp(ow - Math.min(ARMOR_FLAT, ow * ARMOR_CAP));
	}

	/** Adds this much (Overwatch points) to the top layer as armor, her health growing with it (Rally's +100). */
	public static void grant(ServerPlayer player, HeroState s, double ow) {
		s.shield += ow;
		regenerating = true;
		try {
			player.setHealth(player.getHealth() + Ow.hp(ow));
		} finally {
			regenerating = false;
		}
	}

	/** Each tick (HeroManager): the shields regenerate 3 s after the last damage. */
	public static void tick(ServerPlayer player, HeroState s) {
		int max = s.hero == null ? 0 : s.hero.shields();
		if (max == 0 || s.shield >= max || !player.isAlive()
			|| Combat.now(player) - s.lastDamagedTick < Ow.ticks(DELAY)) {
			return;
		}
		double add = Math.min(max - s.shield, REGEN / 20.0);
		s.shield += add;
		regenerating = true;
		try {
			player.setHealth(player.getHealth() + Ow.hp(add));
		} finally {
			regenerating = false;
		}
	}
}
