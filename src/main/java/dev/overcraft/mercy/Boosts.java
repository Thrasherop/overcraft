package dev.overcraft.mercy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who a Mercy's damage boost is on: each of them deals more damage with anything, a hero's weapons, a sword, a wolf's
 * bite, an arrow they shot (LivingEntityMixin raises the damage as it arrives, before Minecraft's armour). Her beam
 * renews it every tick it's on them; it ends a moment after the beam leaves.
 */
public final class Boosts {
	private record Boost(double factor, long until, UUID mercy) {
	}

	private static final Map<UUID, Boost> BOOSTS = new HashMap<>();

	private Boosts() {
	}

	/** Boosts the entity by factor until game time {@code until}, from that Mercy (for her assists). */
	public static void boost(Entity entity, double factor, long until, UUID mercy) {
		BOOSTS.put(entity.getUUID(), new Boost(factor, until, mercy));
	}

	public static void end(Entity entity) {
		BOOSTS.remove(entity.getUUID());
	}

	/** The factor on the damage this attacker deals now (1 with no boost). */
	public static double damageFactor(@Nullable Entity attacker) {
		if (attacker == null) {
			return 1;
		}
		Boost boost = BOOSTS.get(attacker.getUUID());
		return boost == null || attacker.level().getGameTime() >= boost.until ? 1 : boost.factor;
	}

	/**
	 * The damage a boosted attacker deals, raised; the Mercy boosting them gets ult charge for what she added (W). The
	 * boost is applied when the damage lands (W says when a projectile is fired: ours, Minecraft's arrows don't say).
	 */
	public static float amplify(@Nullable Entity attacker, float damage) {
		double factor = damageFactor(attacker);
		if (factor == 1 || damage <= 0 || attacker == null || attacker.level().getServer() == null) {
			return damage;
		}
		float raised = (float) (damage * factor);
		UUID by = boostedBy(attacker);
		net.minecraft.server.level.ServerPlayer mercy = by == null ? null : attacker.level().getServer().getPlayerList().getPlayer(by);
		if (mercy != null) {
			dev.overcraft.hero.Combat.credit(mercy, dev.overcraft.Ow.toOw(raised - damage));
		}
		return raised;
	}

	/** The Mercy boosting this attacker now, or null. */
	public static @Nullable UUID boostedBy(@Nullable Entity attacker) {
		if (attacker == null) {
			return null;
		}
		Boost boost = BOOSTS.get(attacker.getUUID());
		return boost == null || attacker.level().getGameTime() >= boost.until ? null : boost.mercy;
	}

	public static boolean boosted(@Nullable Entity entity) {
		return damageFactor(entity) != 1;
	}

	public static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		BOOSTS.values().removeIf(b -> now >= b.until);
	}

	public static void clear() {
		BOOSTS.clear();
	}
}
