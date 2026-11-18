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
 * Minecraft spawns naturally (EntitySpawnReason.NATURAL) is, 1 time in 12, replaced by one of the eight enemy heroes, equally
 * likely, never in Peaceful, and never when a player within 64 m of it already has 2 within 64 m.
 * <p>
 * Minecraft has no hook for this, so MobMixin marks the mob from Mob.finalizeSpawn (before the spawner adds it to the world) and
 * the swap happens on the server's next tick: the hero is made at the mob's place and facing, finalized as a natural spawn, and
 * the mob is discarded unseen. Marked, it's inert until then (no AI, no pickup, silent: the critique's finding 5 of batch 1, a
 * zombie that lived a tick could take a swing or pick up an item); a swap that doesn't happen gives it all back.
 * Mobs made any other way (spawners, eggs, /summon, chunk generation, reinforcements) never change.
 */
public final class EnemyHeroSpawns {
	private EnemyHeroSpawns() {
	}

	/** Marked this tick, swapped at its end (server thread only). */
	private static final List<Pending> PENDING = new ArrayList<>();

	/** A marked mob and what it had before it was made inert. */
	private record Pending(Mob mob, boolean noAi, boolean pickUp, boolean silent) {
		void restore() {
			mob.setNoAi(noAi);
			mob.setCanPickUpLoot(pickUp);
			mob.setSilent(silent);
		}
	}

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
			PENDING.add(new Pending(mob, mob.isNoAi(), mob.canPickUpLoot(), mob.isSilent()));
			// inert until the swap at the tick's end: it ticks once in the world before then
			mob.setNoAi(true);
			mob.setCanPickUpLoot(false);
			mob.setSilent(true);
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
		List<Pending> marked = new ArrayList<>(PENDING);
		PENDING.clear();
		for (Pending p : marked) {
			Mob mob = p.mob();
			if (!swap(mob)) {
				p.restore();
			}
		}
	}

	/** Swaps {@code mob} for an enemy hero; false when it stays. */
	private static boolean swap(Mob mob) {
		if (mob.isRemoved() || !mob.isAlive() || !(mob.level() instanceof ServerLevel level) || mob.isPassenger() || mob.isVehicle()
			|| !night(level) || crowded(level, mob)) {
			// (a jockey keeps its rider: the swap would leave one of them alone)
			return false;
		}
		EntityType<? extends EnemyHeroEntity> type = switch (level.getRandom().nextInt(8)) {
			case 0 -> OcEntities.ENEMY_TRACER;
			case 1 -> OcEntities.ENEMY_CASSIDY;
			case 2 -> OcEntities.ENEMY_JUNKRAT;
			case 3 -> OcEntities.ENEMY_PHARAH;
			case 4 -> OcEntities.ENEMY_ASHE;
			case 5 -> OcEntities.ENEMY_SOJOURN;
			case 6 -> OcEntities.ENEMY_TORBJORN;
			default -> OcEntities.ENEMY_WIDOWMAKER;
		};
		EnemyHeroEntity hero = type.create(level, EntitySpawnReason.NATURAL);
		if (hero == null) {
			return false;
		}
		hero.snapTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0);
		if (!level.noCollision(hero)) {
			return false;
		}
		hero.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.NATURAL, null);
		level.addFreshEntity(hero);
		mob.discard();
		return true;
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
