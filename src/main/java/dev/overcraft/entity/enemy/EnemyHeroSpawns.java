package dev.overcraft.entity.enemy;

import dev.overcraft.registry.OcEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Where enemy heroes come from (#46, the plan's "How often", ours): at night in the Overworld, a zombie, skeleton or husk that
 * Minecraft spawns naturally (EntitySpawnReason.NATURAL) is, 1 time in 12, replaced by one of the four enemy heroes, equally
 * likely, never in Peaceful, and never when a player within 64 m of it already has 2 within 64 m.
 * <p>
 * Minecraft has no hook for this, so MobMixin marks the mob from Mob.finalizeSpawn (before the spawner adds it to the world) and
 * the swap happens on the server's next tick: the hero is made at the mob's place and facing, finalized as a natural spawn, and
 * the mob is discarded unseen. Mobs made any other way (spawners, eggs, /summon, chunk generation, reinforcements) never change.
 */
public final class EnemyHeroSpawns {
	private EnemyHeroSpawns() {
	}

	/** Marked this tick, swapped at its end (server thread only). */
	private static final List<Mob> PENDING = new ArrayList<>();

	/** Dev only: off, to see a night without them (and the rule's effect). */
	public static boolean devOff;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(EnemyHeroSpawns::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
	}

	/** From Mob.finalizeSpawn's end (MobMixin): mark a natural night spawn that won the roll. */
	public static void finalized(Mob mob, ServerLevelAccessor accessor, EntitySpawnReason reason) {
		if (devOff || reason != EntitySpawnReason.NATURAL || !replaceable(mob.getType())) {
			return;
		}
		ServerLevel level = accessor.getLevel();
		if (!night(level)) {
			return;
		}
		if (EnemyHeroStats.mayReplace(level.getRandom().nextInt(EnemyHeroStats.SPAWN_ONE_IN), 0)) {
			PENDING.add(mob);
		}
	}

	/** The kinds a hero may stand in for: the plan's zombie, skeleton and husk (exactly those, not their variants). */
	static boolean replaceable(EntityType<?> type) {
		return type == net.minecraft.world.entity.EntityTypes.ZOMBIE || type == net.minecraft.world.entity.EntityTypes.SKELETON
			|| type == net.minecraft.world.entity.EntityTypes.HUSK;
	}

	private static boolean night(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && level.isDarkOutside() && level.getDifficulty() != Difficulty.PEACEFUL;
	}

	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		List<Mob> marked = new ArrayList<>(PENDING);
		PENDING.clear();
		for (Mob mob : marked) {
			if (mob.isRemoved() || !mob.isAlive() || !(mob.level() instanceof ServerLevel level) || mob.isPassenger() || mob.isVehicle()
				|| !night(level) || crowded(level, mob)) {
				// (a jockey keeps its rider: the swap would leave one of them alone)
				continue;
			}
			EntityType<? extends EnemyHeroEntity> type = switch (level.getRandom().nextInt(4)) {
				case 0 -> OcEntities.ENEMY_TRACER;
				case 1 -> OcEntities.ENEMY_CASSIDY;
				case 2 -> OcEntities.ENEMY_JUNKRAT;
				default -> OcEntities.ENEMY_PHARAH;
			};
			EnemyHeroEntity hero = type.create(level, EntitySpawnReason.NATURAL);
			if (hero == null) {
				continue;
			}
			hero.snapTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0);
			if (!level.noCollision(hero)) {
				continue;
			}
			hero.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.NATURAL, null);
			level.addFreshEntity(hero);
			mob.discard();
		}
	}

	/** Whether a player within the cap's reach of {@code at} already has the cap's number of enemy heroes within it. */
	private static boolean crowded(ServerLevel level, Mob at) {
		double r = EnemyHeroStats.CAP_RADIUS;
		for (ServerPlayer player : level.players()) {
			// natural spawns land 24-128 m from a player: any player that far may be the one it walks to (critique of c728bea)
			if (player.distanceToSqr(at) > 128 * 128) {
				continue;
			}
			int near = level.getEntitiesOfClass(EnemyHeroEntity.class, new AABB(player.position(), player.position()).inflate(r),
				e -> e.isAlive() && e.distanceToSqr(player) <= r * r).size();
			if (!EnemyHeroStats.mayReplace(0, near)) {
				return true;
			}
		}
		return false;
	}
}
