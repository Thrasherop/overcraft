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
	 * The effects that are a state, told once as it begins (#96). Each lasts its variant on its entity (or, with no entity, in the world,
	 * while its maker is tracked). A player who starts seeing the entity or its maker later hears of it through {@link #showHeld}: never
	 * the arrival again. The effect is sent with what's left as its variant and how long it has run (seconds) in dx, so a client's fade-in
	 * doesn't start over, and {@code replay} says which name: the same one where the client only puts it in a map by entity (a repeat
	 * replaces), its "_state" form where the effect also bursts or lays something in the world (those are keyed on the client too: a
	 * second arrival replaces or skips). Add a new effect here when its client holds it by its variant.
	 * Juno's ray isn't here: it walks, so its place is the server's (OrbitalRay.showTo). Bursts aren't here: they're over before anyone arrives.
	 *
	 * @param unit game ticks in one unit of the variant
	 * @param flags the variant's bits that aren't its time, kept as they are
	 */
	private record Spec(int unit, String replay, int flags) {
		static Spec plain(String effect) {
			return new Spec(1, effect, 0);
		}
	}

	private static final java.util.Map<String, Spec> HELD = held();

	private static java.util.Map<String, Spec> held() {
		java.util.Map<String, Spec> m = new java.util.HashMap<>();
		for (String e : new String[] {"ana_asleep", "ana_boosted", "ana_anti_healed", "ana_nano", "ana_numbed", "brigitte_stun", "juno_ring_boost",
			"juno_ray_ally", "sombra_hotfixed", "sombra_hacked", "sombra_revealed", "sombra_hacked_thing", "sombra_weakened", "sound_barrier_ally"}) {
			m.put(e, Spec.plain(e));
		}
		m.put("sombra_cyberspace", new Spec(1, "sombra_cyberspace_state", 0));
		m.put("sojourn_disruptor_field", new Spec(1, "sojourn_disruptor_field_state", dev.overcraft.sojourn.Disruptor.FX_SLOWS));
		// (Kitsune Rush's corridor: its variant is tenths of a second)
		m.put("rush_gate", new Spec(2, "rush_gate_state", 0));
		m.put("rush_tiles", new Spec(2, "rush_tiles_state", 0));
		return m;
	}
	/** The effects that end a held one early. */
	private static final java.util.Map<String, String> ENDS = java.util.Map.of("ana_wake", "ana_asleep", "ana_nano_end", "ana_nano",
		"sombra_hotfixed_end", "sombra_hotfixed");

	private record Held(String effect, Vec3 at, Vec3 direction, @Nullable Entity entity, @Nullable Entity owner, long since, long until, Spec spec, int flagBits) {
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
		Spec spec = HELD.get(effect);
		if (spec != null && (entity != null || owner != null)) {
			long now = level.getGameTime();
			Held held = new Held(effect, at, direction, entity, owner, now, now + (long) (variant & ~spec.flags) * spec.unit, spec, variant & spec.flags);
			HELD_NOW.removeIf(h -> h.sameAs(held));
			HELD_NOW.add(held);
		}
	}

	/** A player has just started to see {@code tracked}: what's still running on it, or that it made in the world, with the time left. */
	public static void showHeld(Entity tracked, ServerPlayer viewer) {
		HELD_NOW.removeIf(Held::over);
		for (Held h : HELD_NOW) {
			if (h.entity == tracked || h.entity == null && h.owner == tracked) {
				long now = tracked.level().getGameTime();
				boolean enemy = h.owner != null && h.owner != viewer && !Allies.wasAlly(viewer, h.owner);
				// what lies along a direction keeps it (a gate's width, the tiles' way); everything else has none, so its time run goes in dx
				boolean world = h.direction.lengthSqr() > 0;
				float ran = (now - h.since) / 20f;
				int entity = h.entity != null ? h.entity.getId() : h.spec.replay.equals("sojourn_disruptor_field_state") && h.owner != null ? h.owner.getId() : -1;
				ServerPlayNetworking.send(viewer, new EffectPayload(h.spec.replay, h.at.x, h.at.y, h.at.z, world ? (float) h.direction.x : ran,
					world ? (float) h.direction.y : 0, world ? (float) h.direction.z : 0, entity, (int) Math.max(1, (h.until - now) / h.spec.unit) | h.flagBits, enemy));
			}
		}
	}

	/** A state for one player alone, with nothing else to say than this (a walking ray's place: OrbitalRay.showTo; Purr's wall). */
	public static void sendState(ServerPlayer viewer, String effect, Vec3 at, Vec3 direction, @Nullable Entity entity, int variant, @Nullable Entity owner) {
		boolean enemy = owner != null && owner != viewer && !Allies.wasAlly(viewer, owner);
		ServerPlayNetworking.send(viewer, new EffectPayload(effect, at.x, at.y, at.z, (float) direction.x, (float) direction.y, (float) direction.z,
			entity == null ? -1 : entity.getId(), variant, enemy));
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
