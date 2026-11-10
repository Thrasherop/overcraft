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
 * allies within 20 m (no sight test, M) 12 at once and 45 over 4 s. The flail can start it at most every INSPIRE_COOLDOWN (1 s, BZ 2026-11-03); Whip Shot
 * and Shield Bash always do (12D8 #428 -> #443). A new one on an ally replaces what's left of the last (ours: its refresh or
 * stack policy isn't decoded, L; W: it doesn't stack).
 */
public final class Inspire {
	private record Heal(ServerPlayer brigitte, LivingEntity ally, long until, double perTick) {
	}

	private static final List<Heal> HEALS = new ArrayList<>();

	private Inspire() {
	}

	/**
	 * Inspiring Strike's version (51F2 #459, 12D8 #469): her and her allies within 20 m healed 57 at once, no heal over time.
	 */
	public static void strike(ServerPlayer brigitte, HeroState s) {
		s.inspireReadyAt = brigitte.level().getGameTime() + Ow.ticks(INSPIRE_COOLDOWN);
		pulse(brigitte);
		for (LivingEntity e : brigitte.level().getEntitiesOfClass(LivingEntity.class, brigitte.getBoundingBox().inflate(INSPIRE_RADIUS),
			e -> e.isAlive() && Allies.isSelfOrAlly(brigitte, e) && e.distanceTo(brigitte) <= INSPIRE_RADIUS)) {
			give(brigitte, e, INSPIRING_STRIKE_HEAL);
		}
	}

	/** An enemy hit by her flail (gated by INSPIRE_COOLDOWN), or by Whip Shot or Shield Bash ({@code always}). */
	public static void trigger(ServerPlayer brigitte, HeroState s, boolean always, double seconds) {
		long now = brigitte.level().getGameTime();
		if (!always && now < s.inspireReadyAt) {
			return;
		}
		s.inspireReadyAt = now + Ow.ticks(INSPIRE_COOLDOWN);
		pulse(brigitte);
		for (LivingEntity e : brigitte.level().getEntitiesOfClass(LivingEntity.class, brigitte.getBoundingBox().inflate(INSPIRE_RADIUS),
			e -> e.isAlive() && Allies.isSelfOrAlly(brigitte, e) && e.distanceTo(brigitte) <= INSPIRE_RADIUS)) {
			HEALS.removeIf(h -> h.ally == e);
			if (e != brigitte && e.getHealth() < e.getMaxHealth()) {
				BrigitteSounds.on(e, "inspire.heal", 60);
			}
			give(brigitte, e, INSPIRE_HEAL_NOW);
			HEALS.add(new Heal(brigitte, e, now + Ow.ticks(seconds), INSPIRE_HEAL_OVER_TIME / Ow.ticks(INSPIRE_TIME)));
		}
	}

	/** Its ring round her waist (2182 at her + 1 m); nothing on the allies (their effects are sounds). */
	private static void pulse(ServerPlayer brigitte) {
		BrigitteSounds.split(brigitte, "inspire.pulse", brigitte.getEyePosition(), 70);
		dev.overcraft.hero.Effects.play(brigitte.level(), "brigitte_inspire", brigitte.position(), net.minecraft.world.phys.Vec3.ZERO,
			brigitte, 0, brigitte);
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

	/** Her Inspire is healing someone now, herself included. */
	public static boolean active(ServerPlayer brigitte) {
		for (Heal heal : HEALS) {
			if (heal.brigitte == brigitte) {
				return true;
			}
		}
		return false;
	}

	/** How many of her allies her Inspire is healing now, herself not counted (the widget under her crosshair). */
	public static int healing(ServerPlayer brigitte) {
		int count = 0;
		for (Heal heal : HEALS) {
			if (heal.brigitte == brigitte && heal.ally != brigitte) {
				count++;
			}
		}
		return count;
	}

	public static void clear() {
		HEALS.clear();
	}
}
