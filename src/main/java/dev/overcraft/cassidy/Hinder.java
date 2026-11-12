package dev.overcraft.cassidy;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flashbang's hinder (status 0079.053, graph 0228; cassidy-map.md): 1.2 s at half speed, no movement abilities, no crouch (D, M on
 * the last two). The slow is an attribute buff, so a creature's and a player's speed both take it (OwMovement.speedFactor). Ours:
 * of the movement abilities only Cassidy's own Combat Roll asks {@link #hindered} so far (a hero's others don't yet), and the
 * crouch isn't blocked (Minecraft's sneak is the client's).
 */
public final class Hinder {
	private static final Identifier SLOW = Overcraft.id("hinder_slow");
	private static final Map<UUID, Long> UNTIL = new ConcurrentHashMap<>();

	private Hinder() {
	}

	/** Hinders {@code target} for 1.2 s from now (renewed, never shortened). */
	public static void apply(LivingEntity target) {
		long until = Combat.now(target) + Ow.ticks(CassidyStats.HINDER_TIME);
		UNTIL.merge(target.getUUID(), until, Math::max);
		Combat.buff(target, Attributes.MOVEMENT_SPEED, SLOW, -CassidyStats.HINDER_SLOW, CassidyStats.HINDER_TIME);
	}

	public static boolean hindered(LivingEntity entity) {
		Long until = UNTIL.get(entity.getUUID());
		if (until == null) {
			return false;
		}
		if (Combat.now(entity) >= until || !entity.isAlive()) {
			UNTIL.remove(entity.getUUID());
			return false;
		}
		return true;
	}

	public static void clear() {
		UNTIL.clear();
	}
}
