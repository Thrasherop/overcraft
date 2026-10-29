package dev.overcraft.ana;

import dev.overcraft.Ow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who Ana's Nano Boost is on (her graph 0B9E, the buff 0F17): for 8 s they deal 50% more damage (with anything: a hero's
 * weapons, a sword, a wolf's bite, an arrow; added to a Mercy's boost, mercy.Boosts) and take 50% less (LivingEntityMixin,
 * before shields take it). It ends early when they die; Ana's death doesn't end it (tools/reference/ana.md).
 */
public final class Nano {
	private record Nanoed(LivingEntity entity, long until, UUID ana) {
	}

	private static final Map<UUID, Nanoed> NANOED = new HashMap<>();

	private Nano() {
	}

	public static void boost(LivingEntity entity, UUID ana) {
		NANOED.put(entity.getUUID(), new Nanoed(entity, entity.level().getGameTime() + Ow.ticks(AnaStats.NANO_TIME), ana));
	}

	public static boolean boosted(@Nullable Entity entity) {
		if (entity == null) {
			return false;
		}
		Nanoed n = NANOED.get(entity.getUUID());
		return n != null && entity.level().getGameTime() < n.until && n.entity.isAlive();
	}

	/** What Nano adds to the factor on the damage this attacker deals now (0 when not boosted). */
	public static double dealtBonus(@Nullable Entity attacker) {
		return boosted(attacker) ? AnaStats.NANO_DAMAGE_DEALT : 0;
	}

	/** The factor on the damage this victim takes now (1 when not boosted). */
	public static float takenFactor(Entity victim) {
		return boosted(victim) ? (float) (1 - AnaStats.NANO_DAMAGE_TAKEN) : 1;
	}

	/** Seconds of boost left (for the HUD), 0 when none. */
	public static double left(@Nullable Entity entity) {
		Nanoed n = entity == null ? null : NANOED.get(entity.getUUID());
		return n == null || !boosted(entity) ? 0 : (n.until - entity.level().getGameTime()) / 20.0;
	}

	public static void tick(MinecraftServer server) {
		NANOED.values().removeIf(n -> !n.entity.isAlive() || n.entity.isRemoved() || n.entity.level().getGameTime() >= n.until);
	}

	public static void clear() {
		NANOED.clear();
	}
}
