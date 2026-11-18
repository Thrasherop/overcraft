package dev.overcraft.entity.enemy;

import dev.overcraft.registry.OcEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

/**
 * Where enemy heroes come from (#46, the plan's "How often", ours): at night in the Overworld, a zombie, skeleton or husk that
 * Minecraft spawns naturally (EntitySpawnReason.NATURAL) is, 1 time in 12, replaced by one of the eight enemy heroes, equally
 * likely, never in Peaceful, and never when a player within 64 m of it already has 2 within 64 m.
 * <p>
 * Minecraft has no hook for this, so MobMixin marks the mob from Mob.finalizeSpawn's end, and EnemySpawnSwapMixin swaps it as the
 * spawner adds it (ServerLevel.addFreshEntity): the hero is made at the mob's place and facing, finalized as a natural spawn and added
 * instead, and the mob never enters the world. Nothing on the mob is changed, so a swap that doesn't happen (crowded, a jockey, no
 * room) leaves it exactly as Minecraft made it (the critique of 29c1797: the old next-tick swap's inert flags were overwritten by the
 * zombie's own pickup roll and could be saved). Marks not added by the tick's end are dropped. Mobs made any other way (spawners,
 * eggs, /summon, chunk generation, reinforcements) never change.
 */
public final class EnemyHeroSpawns {
	private EnemyHeroSpawns() {
	}

	/** Marked, waiting for the spawner to add them (server thread only; by identity). */
	private static final java.util.Set<Mob> PENDING = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

	/** Dev only: off, to see a night without them (and the rule's effect). */
	public static boolean devOff;

	/**
	 * {@code /gamerule overcraft:enemy_hero_chance <0-100>}: the percent of natural night zombies, skeletons and husks that come as
	 * an enemy hero instead (owner, 2026-11-18: "a gamerule for the chances of enemy heros, including a way to disable spawning mob
	 * heros"). 0 turns them off; the default 8 is the old fixed 1 in 12 (EnemyHeroStats.SPAWN_ONE_IN). The cap (2 within 64 m of a
	 * player) still holds at any chance.
	 */
	public static final net.minecraft.world.level.gamerules.GameRule<Integer> ENEMY_HERO_CHANCE =
		net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder.forInteger(8).range(0, 100)
			.category(net.minecraft.world.level.gamerules.GameRuleCategory.MOBS)
			.buildAndRegister(dev.overcraft.Overcraft.id("enemy_hero_chance"));

	public static void init() {
		// a mark the spawner didn't add this tick (never expected) doesn't linger
		ServerTickEvents.END_SERVER_TICK.register(server -> PENDING.clear());
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
		int chance = level.getGameRules().get(ENEMY_HERO_CHANCE);
		if (chance <= 0) {
			return;
		}
		// the rule's percent: a roll of 0 replaces (mayReplace)
		if (EnemyHeroStats.mayReplace(level.getRandom().nextInt(100) < chance ? 0 : 1, 0)) {
			PENDING.add(mob);
		}
	}

	/**
	 * From ServerLevel.addFreshEntity's start (EnemySpawnSwapMixin): a marked mob being added is swapped for its hero, which is added
	 * in its place. True when it was (the mob is then not added); false lets the add go on as Minecraft's.
	 */
	public static boolean adding(ServerLevel level, net.minecraft.world.entity.Entity entity) {
		if (PENDING.isEmpty() || !(entity instanceof Mob mob) || !PENDING.remove(mob)) {
			return false;
		}
		return mob.level() == level && swap(mob);
	}

	/** The kinds a hero may stand in for: the plan's zombie, skeleton and husk (exactly those, not their variants). */
	static boolean replaceable(EntityType<?> type) {
		return type == net.minecraft.world.entity.EntityTypes.ZOMBIE || type == net.minecraft.world.entity.EntityTypes.SKELETON
			|| type == net.minecraft.world.entity.EntityTypes.HUSK;
	}

	private static boolean night(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && level.isDarkOutside() && level.getDifficulty() != Difficulty.PEACEFUL;
	}

	/** Swaps {@code mob}, not yet in the world, for an enemy hero added in its place; false when it stays. */
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
