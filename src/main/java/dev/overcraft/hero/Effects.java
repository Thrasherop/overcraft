package dev.overcraft.hero;

import dev.overcraft.net.EffectPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Plays an ability effect for every player near it. The effects are drawn client-side from Overwatch's own effect
 * textures (client/effect); the server only says which, where and in which direction (they replaced Minecraft's
 * particles: the owner, 2026-10-26: "Thats not how ofuda look in game").
 */
public final class Effects {
	/** How far away an effect is still sent (blocks). */
	private static final double RANGE = 96;

	/**
	 * The effects that are a state, told once as it begins (#96): how many game ticks one unit of their variant is. Each lasts that
	 * long on its entity (or, with no entity, in the world, while its maker is tracked). A player who starts seeing the entity or its
	 * maker later hears of it with what's left: {@link #showHeld}. Add a new effect here when its client holds it by its variant.
	 * Bursts aren't here: they're over before anyone arrives.
	 */
	private static final java.util.Map<String, Integer> HELD = java.util.Map.ofEntries(
		java.util.Map.entry("ana_asleep", 1), java.util.Map.entry("ana_boosted", 1), java.util.Map.entry("ana_anti_healed", 1),
		java.util.Map.entry("ana_nano", 1), java.util.Map.entry("ana_numbed", 1), java.util.Map.entry("brigitte_stun", 1),
		java.util.Map.entry("juno_ring_boost", 1), java.util.Map.entry("juno_ray", 1), java.util.Map.entry("juno_ray_ally", 1),
		java.util.Map.entry("sombra_hotfixed", 1), java.util.Map.entry("sombra_hacked", 1), java.util.Map.entry("sombra_revealed", 1),
		java.util.Map.entry("sombra_hacked_thing", 1), java.util.Map.entry("sombra_weakened", 1), java.util.Map.entry("sound_barrier_ally", 1),
		// (Kitsune Rush's corridor: its variant is tenths of a second)
		java.util.Map.entry("rush_gate", 2), java.util.Map.entry("rush_tiles", 2));
	/** The effects that end a held one early. */
	private static final java.util.Map<String, String> ENDS = java.util.Map.of("ana_wake", "ana_asleep", "ana_nano_end", "ana_nano",
		"sombra_hotfixed_end", "sombra_hotfixed");

	private record Held(String effect, Vec3 at, Vec3 direction, @Nullable Entity entity, @Nullable Entity owner, long until, int unit) {
		boolean over() {
			Entity on = entity != null ? entity : owner;
			return on == null || on.isRemoved() || on.level().getGameTime() >= until;
		}

		boolean sameAs(Held other) {
			return effect.equals(other.effect) && entity == other.entity && (entity != null || at.equals(other.at));
		}
	}

	private static final java.util.List<Held> HELD_NOW = new java.util.ArrayList<>();

	/** An effect has just been played: remember it if it's a state, forget the state it ends. */
	private static void note(ServerLevel level, String effect, Vec3 at, Vec3 direction, @Nullable Entity entity, int variant, @Nullable Entity owner) {
		String ended = ENDS.get(effect);
		if (ended != null) {
			HELD_NOW.removeIf(h -> h.effect.equals(ended) && h.entity == entity);
		}
		Integer unit = HELD.get(effect);
		if (unit != null && (entity != null || owner != null)) {
			Held held = new Held(effect, at, direction, entity, owner, level.getGameTime() + (long) variant * unit, unit);
			HELD_NOW.removeIf(h -> h.sameAs(held));
			HELD_NOW.add(held);
		}
	}

	/** A player has just started to see {@code tracked}: what's still running on it, or that it made in the world, with the time left. */
	public static void showHeld(Entity tracked, ServerPlayer viewer) {
		HELD_NOW.removeIf(Held::over);
		for (Held h : HELD_NOW) {
			if (h.entity == tracked || h.entity == null && h.owner == tracked) {
				long left = h.until - tracked.level().getGameTime();
				boolean enemy = h.owner != null && h.owner != viewer && !Allies.wasAlly(viewer, h.owner);
				ServerPlayNetworking.send(viewer, new EffectPayload(h.effect, h.at.x, h.at.y, h.at.z, (float) h.direction.x, (float) h.direction.y,
					(float) h.direction.z, h.entity == null ? -1 : h.entity.getId(), (int) Math.max(1, left / h.unit), enemy));
			}
		}
	}

	/** Every so often: what's over goes (nobody may start seeing it again). */
	public static void tick(net.minecraft.server.MinecraftServer server) {
		if (server.getTickCount() % 200 == 0) {
			HELD_NOW.removeIf(Held::over);
		}
	}

	public static void clear() {
		HELD_NOW.clear();
	}

	private Effects() {
	}

	public static void play(ServerLevel level, String effect, Vec3 at) {
		play(level, effect, at, Vec3.ZERO, null, 0, null);
	}

	/** An effect made by owner (the hero): its enemies see it in red. */
	public static void play(ServerLevel level, String effect, Vec3 at, @Nullable Entity owner) {
		play(level, effect, at, Vec3.ZERO, null, 0, owner);
	}

	/**
	 * The thrower's hit marker on their crosshair: 0 a hit, 1 a critical hit. Sent to them alone (others don't see your
	 * hit markers). A hit that killed sends nothing here: the kill marker (2, with "ELIMINATED") comes from the death
	 * itself (killMarker, from KillFeed), so every way a hero kills gets it once, whether or not the damage's own code
	 * marks its hits (#106: Soundwave, Purr, the Disruptor field and Ana's rifle never got it).
	 */
	public static void hitMarker(ServerPlayer shooter, LivingEntity target, boolean crit) {
		if (!target.isAlive() || target.getHealth() <= 0) {
			return;
		}
		// (a bystander's head earns no crit marker: #99)
		ServerPlayNetworking.send(shooter, new EffectPayload("hit_marker", target.getX(), target.getY(), target.getZ(), 0, 0, 0,
			target.getId(), crit && !Allies.isBystander(shooter, target) ? 1 : 0, false));
	}

	/** The kill marker and the final blow's "ELIMINATED" caption, for the hero who killed {@code victim}. */
	public static void killMarker(ServerPlayer killer, LivingEntity victim) {
		ServerPlayNetworking.send(killer, new EffectPayload("hit_marker", victim.getX(), victim.getY(), victim.getZ(), 0, 0, 0,
			victim.getId(), 2, false));
	}

	/**
	 * An effect for one player alone, in their team's view of owner: a state they missed because they couldn't see it
	 * as it began (they joined, respawned or came into range).
	 */
	public static void send(ServerPlayer player, String effect, Vec3 at, @Nullable Entity entity, int variant,
							@Nullable Entity owner) {
		boolean enemy = owner != null && owner != player && !Allies.wasAlly(player, owner);
		ServerPlayNetworking.send(player, new EffectPayload(effect, at.x, at.y, at.z, 0, 0, 0, entity == null ? -1 : entity.getId(),
			variant, enemy));
	}

	public static void play(ServerLevel level, String effect, Vec3 at, Vec3 direction, @Nullable Entity entity, int variant) {
		play(level, effect, at, direction, entity, variant, null);
	}

	/** The same, made by owner: each player near it gets it in their own team's view of the owner. */
	public static void play(ServerLevel level, String effect, Vec3 at, Vec3 direction, @Nullable Entity entity, int variant,
							@Nullable Entity owner) {
		note(level, effect, at, direction, entity, variant, owner);
		for (ServerPlayer player : PlayerLookup.around(level, at, RANGE)) {
			boolean enemy = owner != null && owner != player && !Allies.wasAlly(player, owner);
			ServerPlayNetworking.send(player, new EffectPayload(effect, at.x, at.y, at.z, (float) direction.x,
				(float) direction.y, (float) direction.z, entity == null ? -1 : entity.getId(), variant, enemy));
		}
	}
}
