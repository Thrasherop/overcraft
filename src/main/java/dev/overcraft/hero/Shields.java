package dev.overcraft.hero;

import dev.overcraft.Ow;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shields (Lifeweaver's 50: the first hero with them). In Overwatch they're the top of the health bar: damage takes
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
		if (s == null || s.hero == null || s.hero.shields() == 0 || regenerating) {
			return health;
		}
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
		double[] split = heal(Ow.toOw(old) - s.shield, s.shield, Ow.toOw(player.getMaxHealth()) - s.hero.shields(), s.hero.shields(),
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
