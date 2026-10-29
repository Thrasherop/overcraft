package dev.overcraft.hero;

import dev.overcraft.Ow;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shields (Lifeweaver's 50: the first hero with them). In Overwatch they're the top of the health bar: damage takes
 * overhealth, then shields, then health, and healing fills health only; shields come back by themselves 3 s after the
 * last damage, at 30 a second (W Hit points). Here they're the top {@link Hero#shields()} of the hero's Minecraft health
 * (Lifeweaver's 250 = 20: the top 4), with what's left of them kept in {@link HeroState#shield}: every change to the
 * health (LivingEntityMixin, setHealth) takes a loss off them first and stops a gain at the health under them.
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
		// a gain heals the health under the shields, never the shields themselves
		float cap = player.getMaxHealth() - Ow.hp(s.hero.shields() - s.shield);
		return health > old ? Math.max(old, Math.min(health, cap)) : health;
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
