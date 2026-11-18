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

	/**
	 * The factor on the damage this attacker deals now (1 with no boost): a Mercy's and an Ana's Nano Boost add into one
	 * sum (+30% and +50% make +80%: the data adds damage modifiers into a sum, ana-plan.md, M).
	 */
	public static double damageFactor(@Nullable Entity attacker) {
		return mercyFactor(attacker) + dev.overcraft.ana.Nano.dealtBonus(attacker);
	}

	private static double mercyFactor(@Nullable Entity attacker) {
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
	public static float amplify(@Nullable Entity attacker, float damage, @Nullable Entity victim) {
		double factor = damageFactor(attacker);
		if (factor == 1 || damage <= 0 || attacker == null || attacker.level().getServer() == null) {
			return damage;
		}
		float raised = (float) (damage * factor);
		UUID by = boostedBy(attacker);
		net.minecraft.server.level.ServerPlayer mercy = by == null ? null : attacker.level().getServer().getPlayerList().getPlayer(by);
		if (mercy != null && victim instanceof net.minecraft.world.entity.LivingEntity hurt) {
			// her share of what was added, not Nano's: credited once the hit lands (landed), at most what the victim had left. Credited
			// here, every hit Minecraft then refused (its hit cooldown, a phased or dying target) counted in full (a critique)
			PENDING.put(hurt, new Pending(mercy.getUUID(), dev.overcraft.Ow.toOw((float) (damage * (mercyFactor(attacker) - 1))),
				dev.overcraft.Ow.toOw(hurt.getHealth() + hurt.getAbsorptionAmount())));
		}
		return raised;
	}

	private record Pending(UUID mercy, double points, double cap) {
	}

	/**
	 * Shares waiting on their hit (by victim: a hit inside a hit, thorns, is another victim's). Server thread only. Weak: a hit
	 * cancelled at hurtServer's head (a barrier, D.Va untargetable) never reaches landed, and its entry mustn't keep the victim.
	 */
	private static final java.util.Map<net.minecraft.world.entity.LivingEntity, Pending> PENDING = new java.util.WeakHashMap<>();

	/** {@code victim}'s hit is over: its Mercy's share credited if it landed (LivingEntityMixin, hurtServer's return). */
	public static void landed(net.minecraft.world.entity.LivingEntity victim, boolean hurt) {
		Pending p = PENDING.remove(victim);
		if (p == null || !hurt || victim.level().getServer() == null) {
			return;
		}
		net.minecraft.server.level.ServerPlayer mercy = victim.level().getServer().getPlayerList().getPlayer(p.mercy());
		if (mercy != null) {
			dev.overcraft.hero.Combat.credit(mercy, Math.min(p.points(), p.cap()), victim);
		}
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
