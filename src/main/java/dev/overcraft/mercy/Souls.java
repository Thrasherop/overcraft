package dev.overcraft.mercy;

import com.mojang.logging.LogUtils;
import dev.overcraft.hero.Allies;
import dev.overcraft.net.ResurrectPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The allies who just died, for Mercy's Resurrect. Each death of someone who is a player's ally (another player, a
 * pet, a villager, a golem, an allay: Allies) leaves a soul where they fell. A player's soul lasts while they wait on
 * the death screen (respawning, leaving or the soul's time running out takes it); a mob's soul is its saved data, made
 * again in the world when it's resurrected (its body is gone by then: Minecraft removes the dead after 1 s).
 */
public final class Souls {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final class Soul {
		public final UUID id;
		/** The dead entity (a removed mob, or a player on the death screen): who it was, for the ally check. */
		public final LivingEntity body;
		public final ResourceKey<Level> level;
		public final Vec3 at;
		/** Game time of the death. */
		public final long diedAt;
		/** A mob's saved data (null for a player, who comes back through their respawn). */
		final @Nullable CompoundTag mob;

		Soul(LivingEntity body, @Nullable CompoundTag mob) {
			this.id = body.getUUID();
			this.body = body;
			this.level = body.level().dimension();
			this.at = body.position();
			this.diedAt = body.level().getGameTime();
			this.mob = mob;
		}
	}

	private static final Map<UUID, Soul> SOULS = new LinkedHashMap<>();
	/** Players being brought back, until their respawn puts them where they fell, with the health they come back with. */
	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private record Pending(Soul soul, float healthFraction) {
	}

	private Souls() {
	}

	/** AFTER_DEATH: a death leaves a soul if any player in the world had them as an ally. */
	public static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level) || !anyAlly(level, entity)) {
			return;
		}
		CompoundTag mob = null;
		if (!(entity instanceof Player)) {
			// saved now, before Minecraft removes it (its drops have already fallen: none come back with it)
			try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
				TagValueOutput out = TagValueOutput.createWithContext(reporter, entity.registryAccess());
				if (!entity.saveAsPassenger(out)) {
					return;
				}
				mob = out.buildResult();
			}
		}
		SOULS.put(entity.getUUID(), new Soul(entity, mob));
	}

	private static boolean anyAlly(ServerLevel level, LivingEntity dead) {
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (Allies.wasAlly(player, dead)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The soul nearest the hero (within range of her, and dead less than lifetime seconds) that is her ally's, or null.
	 * A player's soul counts only while they're still dead and on the server.
	 */
	public static @Nullable Soul nearest(ServerPlayer hero, double range, double lifetime) {
		Soul best = null;
		double bestDistance = range * range;
		long now = hero.level().getGameTime();
		for (Soul soul : SOULS.values()) {
			if (soul.level != hero.level().dimension() || now - soul.diedAt > lifetime * 20 || PENDING.containsKey(soul.id)
				|| !Allies.wasAlly(hero, soul.body) || !waiting(hero.level().getServer(), soul)) {
				continue;
			}
			double distance = soul.at.distanceToSqr(hero.position());
			if (distance <= bestDistance) {
				best = soul;
				bestDistance = distance;
			}
		}
		return best;
	}

	/** Every soul of the hero's allies within range of her and dead less than lifetime seconds, nearest first. */
	public static java.util.List<Soul> near(ServerPlayer hero, double range, double lifetime) {
		java.util.List<Soul> found = new java.util.ArrayList<>();
		long now = hero.level().getGameTime();
		for (Soul soul : SOULS.values()) {
			if (soul.level == hero.level().dimension() && now - soul.diedAt <= lifetime * 20 && !PENDING.containsKey(soul.id)
				&& soul.at.distanceTo(hero.position()) <= range && Allies.wasAlly(hero, soul.body)
				&& waiting(hero.level().getServer(), soul)) {
				found.add(soul);
			}
		}
		found.sort(java.util.Comparator.comparingDouble(soul -> soul.at.distanceToSqr(hero.position())));
		return found;
	}

	public static @Nullable Soul get(UUID id) {
		return SOULS.get(id);
	}

	/** Whether a player's soul is still on the death screen (the same dead player, still connected). */
	private static boolean waiting(MinecraftServer server, Soul soul) {
		if (soul.mob != null) {
			return true;
		}
		ServerPlayer player = server.getPlayerList().getPlayer(soul.id);
		return player == soul.body && player.isDeadOrDying() && !player.hasDisconnected() && !server.isHardcore();
	}

	/**
	 * Brings the soul back where it fell with that share of its health. A mob is made again at once; a player is told
	 * to respawn, and onRespawn puts them there. True when it worked.
	 */
	public static boolean resurrect(ServerLevel level, Soul soul, float healthFraction) {
		SOULS.remove(soul.id);
		if (soul.mob == null) {
			if (!(soul.body instanceof ServerPlayer player) || !waiting(level.getServer(), soul)) {
				return false;
			}
			PENDING.put(soul.id, new Pending(soul, healthFraction));
			ServerPlayNetworking.send(player, new ResurrectPayload());
			return true;
		}
		Entity entity = EntityType.loadEntityRecursive(soul.mob, level, new EntitySpawnRequest(EntitySpawnReason.LOAD, false),
			EntityProcessor.NOP);
		if (!(entity instanceof LivingEntity living)) {
			return false;
		}
		if (level.getEntity(soul.id) != null) {
			living.setUUID(UUID.randomUUID());
		}
		living.snapTo(soul.at.x, soul.at.y, soul.at.z, living.getYRot(), living.getXRot());
		living.setDeltaMovement(Vec3.ZERO);
		living.deathTime = 0;
		living.hurtTime = 0;
		living.clearFire();
		living.setHealth(living.getMaxHealth() * healthFraction);
		if (!level.addFreshEntity(living)) {
			return false;
		}
		comeBack(living);
		return true;
	}

	/** A resurrected ally can't be hurt for 2.25 s and can't move for the first 1.5 s of it (W; her data's 1BE7). */
	private static void comeBack(LivingEntity living) {
		dev.overcraft.hero.Combat.phase(living, MercyStats.RES_INVULNERABLE);
		dev.overcraft.hero.Combat.buff(living, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, ROOTED, -1,
			MercyStats.RES_ROOTED);
	}

	private static final net.minecraft.resources.Identifier ROOTED = dev.overcraft.Overcraft.id("resurrected");

	/** AFTER_RESPAWN: a resurrected player lands where they fell; anyone else's respawn takes their soul. */
	public static void onRespawn(ServerPlayer old, ServerPlayer fresh) {
		SOULS.remove(fresh.getUUID());
		Pending pending = PENDING.remove(fresh.getUUID());
		if (pending == null) {
			return;
		}
		ServerLevel level = fresh.level().getServer().getLevel(pending.soul.level);
		if (level == null) {
			return;
		}
		Vec3 at = pending.soul.at;
		fresh.teleportTo(level, at.x, at.y, at.z, Set.of(), old.getYRot(), old.getXRot(), true);
		fresh.setHealth(fresh.getMaxHealth() * pending.healthFraction);
		comeBack(fresh);
		// 483F's 95DF, played on the revived ally (M: their own copy)
		dev.overcraft.hero.HeroSounds.toOwner(fresh, dev.overcraft.registry.OcSounds.MERCY_RES_REVIVED);
	}

	/** Whether this player's respawn is a resurrection (checked before onRespawn takes it). */
	public static boolean resurrecting(ServerPlayer player) {
		return PENDING.containsKey(player.getUUID());
	}

	/** Every tick: souls older than lifetime seconds, and players who left, are gone. */
	public static void tick(MinecraftServer server, double lifetime) {
		long now = server.overworld().getGameTime();
		Iterator<Soul> it = SOULS.values().iterator();
		while (it.hasNext()) {
			Soul soul = it.next();
			if (now - soul.diedAt > lifetime * 20 || !waiting(server, soul)) {
				it.remove();
			}
		}
		PENDING.values().removeIf(p -> server.getPlayerList().getPlayer(p.soul.id) == null);
	}

	public static void clear() {
		SOULS.clear();
		PENDING.clear();
	}
}
