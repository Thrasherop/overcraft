package dev.overcraft.brigitte;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

import static dev.overcraft.brigitte.BrigitteStats.*;

/**
 * Inspire, her passive (12D8 -> 11B9 on each ally): her flail, Whip Shot and Shield Bash hitting an enemy heal her and her
 * allies within 20 m (no sight test, M) 12 at once and 45 over 4 s. The flail can start it at most every 1.25 s; Whip Shot
 * and Shield Bash always do (12D8 #428 -> #443). A new one on an ally replaces what's left of the last (ours: its refresh or
 * stack policy isn't decoded, L; W: it doesn't stack).
 */
public final class Inspire {
	private record Heal(ServerPlayer brigitte, LivingEntity ally, long until, double perTick) {
	}

	private static final List<Heal> HEALS = new ArrayList<>();

	private Inspire() {
	}

	/** An enemy hit by her flail (gated by the 1.25 s), or by Whip Shot or Shield Bash ({@code always}). */
	public static void trigger(ServerPlayer brigitte, HeroState s, boolean always, double seconds) {
		long now = brigitte.level().getGameTime();
		if (!always && now < s.inspireReadyAt) {
			return;
		}
		s.inspireReadyAt = now + Ow.ticks(INSPIRE_COOLDOWN);
		for (LivingEntity e : brigitte.level().getEntitiesOfClass(LivingEntity.class, brigitte.getBoundingBox().inflate(INSPIRE_RADIUS),
			e -> e.isAlive() && Allies.isSelfOrAlly(brigitte, e) && e.distanceTo(brigitte) <= INSPIRE_RADIUS)) {
			HEALS.removeIf(h -> h.ally == e);
			give(brigitte, e, INSPIRE_HEAL_NOW);
			HEALS.add(new Heal(brigitte, e, now + Ow.ticks(seconds), INSPIRE_HEAL_OVER_TIME / Ow.ticks(INSPIRE_TIME)));
		}
	}

	private static void give(ServerPlayer brigitte, LivingEntity ally, double amount) {
		float done = Combat.heal(ally, amount);
		if (done > 0) {
			Combat.credit(brigitte, done);
			Combat.addFireForHealing(brigitte, done);
		}
	}

	public static void tick(MinecraftServer server) {
		HEALS.removeIf(h -> {
			if (!h.ally.isAlive() || h.ally.isRemoved() || h.ally.level().getGameTime() >= h.until) {
				return true;
			}
			give(h.brigitte, h.ally, h.perTick);
			return false;
		});
	}

	public static void clear() {
		HEALS.clear();
	}
}
