package dev.overcraft.kiriko;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static dev.overcraft.kiriko.KirikoStats.*;

/**
 * Kitsune Rush: a spirit fox runs ahead along the ground and lays a corridor 32.2 m long and 10 m wide for 8 s.
 * Kiriko and allies in it (and for 1 s after leaving) move 30% faster, attack and reload 50% faster, and their
 * cooldowns recharge twice as fast. The fox follows the ground: it hops ledges up to about Kiriko's height, follows
 * drops, and stops at taller walls or a gap it can't find ground across.
 */
public final class KitsuneRush {
	private static final List<KitsuneRush> ACTIVE = new ArrayList<>();
	private static final double STEP = 0.5;
	private static final int MAX_HOP = 2;
	private static final int MAX_DROP = 30;
	/** How far above or below the path's floor still counts as in the corridor (ours). */
	private static final double CORRIDOR_HEIGHT = 4;

	private static final DustParticleOptions EDGE = new DustParticleOptions(0xFFE0442E, 1.3f);
	private static final DustParticleOptions FLOOR = new DustParticleOptions(0xFFF6C35B, 0.9f);
	private static final DustParticleOptions FOX = new DustParticleOptions(0xFFFFF4E0, 1.6f);

	private final ServerPlayer owner;
	private final ServerLevel level;
	private final Vec3 direction;
	private final List<Vec3> path = new ArrayList<>();
	private Vec3 fox;
	private double laid;
	private boolean laying = true;
	private int age;

	private KitsuneRush(ServerPlayer owner) {
		this.owner = owner;
		this.level = owner.level();
		Vec3 look = owner.getViewVector(1.0f);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		this.direction = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
		this.fox = owner.position();
		path.add(fox);
	}

	public static void start(ServerPlayer owner) {
		ACTIVE.add(new KitsuneRush(owner));
		owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 1.5f, 0.8f);
		owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.2f);
	}

	public static void tickAll(MinecraftServer server) {
		ACTIVE.removeIf(rush -> !rush.tick());
	}

	public static void clear() {
		ACTIVE.clear();
	}

	/** One tick; false when the corridor is gone. */
	private boolean tick() {
		if (age++ >= Ow.ticks(RUSH_DURATION)) {
			return false;
		}
		if (laying) {
			double perTick = Ow.speed(RUSH_FOX_SPEED);
			for (double moved = 0; moved < perTick && laying; moved += STEP) {
				advanceFox();
			}
			level.sendParticles(FOX, fox.x, fox.y + 0.5, fox.z, 8, 0.3, 0.3, 0.3, 0.0);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, fox.x, fox.y + 0.4, fox.z, 4, 0.2, 0.2, 0.2, 0.01);
		}
		buffAllies();
		if (age % 3 == 0) {
			drawCorridor();
		}
		return true;
	}

	private void advanceFox() {
		if (laid >= RUSH_LENGTH) {
			laying = false;
			return;
		}
		Vec3 next = fox.add(direction.scale(STEP));
		BlockPos feet = BlockPos.containing(next.x, fox.y + 0.01, next.z);
		int hop = 0;
		while (!free(feet.above(hop))) {
			if (++hop > MAX_HOP) {
				laying = false; // a wall taller than Kiriko
				return;
			}
		}
		feet = feet.above(hop);
		int drop = 0;
		while (free(feet.below())) {
			feet = feet.below();
			if (++drop > MAX_DROP) {
				laying = false; // no ground to run on
				return;
			}
		}
		fox = new Vec3(next.x, feet.getY(), next.z);
		path.add(fox);
		laid += STEP;
	}

	private boolean free(BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	private void buffAllies() {
		double half = RUSH_WIDTH / 2;
		AABB area = boundsOfPath().inflate(half, CORRIDOR_HEIGHT, half);
		long until = level.getGameTime() + Ow.ticks(RUSH_LINGER);
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (Allies.isSelfOrAlly(owner, entity) && inCorridor(entity.position(), half)) {
				Combat.rush(entity, until);
			}
		}
	}

	private AABB boundsOfPath() {
		AABB box = new AABB(path.getFirst(), path.getFirst());
		for (Vec3 p : path) {
			box = box.minmax(new AABB(p, p));
		}
		return box;
	}

	/** Within half the width (horizontally) of some point of the path, and near its floor. */
	private boolean inCorridor(Vec3 pos, double half) {
		for (int i = 0; i + 1 < path.size(); i++) {
			Vec3 a = path.get(i);
			Vec3 b = path.get(i + 1);
			double dx = b.x - a.x;
			double dz = b.z - a.z;
			double len2 = dx * dx + dz * dz;
			double t = len2 == 0 ? 0 : Math.clamp(((pos.x - a.x) * dx + (pos.z - a.z) * dz) / len2, 0, 1);
			double cx = a.x + dx * t;
			double cz = a.z + dz * t;
			double cy = a.y + (b.y - a.y) * t;
			double h2 = (pos.x - cx) * (pos.x - cx) + (pos.z - cz) * (pos.z - cz);
			if (h2 <= half * half && pos.y >= cy - 1.5 && pos.y <= cy + CORRIDOR_HEIGHT) {
				return true;
			}
		}
		return false;
	}

	private void drawCorridor() {
		double half = RUSH_WIDTH / 2;
		Vec3 side = new Vec3(-direction.z, 0, direction.x);
		for (int i = 0; i < path.size(); i += 4) {
			Vec3 p = path.get(i).add(0, 0.15, 0);
			for (int s = -1; s <= 1; s += 2) {
				Vec3 edge = p.add(side.scale(s * half));
				level.sendParticles(EDGE, edge.x, edge.y, edge.z, 1, 0.05, 0.05, 0.05, 0);
			}
			level.sendParticles(FLOOR, p.x, p.y, p.z, 2, half / 2 * Math.abs(side.x) + 0.2, 0.02, half / 2 * Math.abs(side.z) + 0.2, 0);
		}
		// torii gates along the way, every 8 m
		if (age % 6 == 0) {
			for (int i = 16; i < path.size(); i += 16) {
				Vec3 p = path.get(i);
				for (int s = -1; s <= 1; s += 2) {
					Vec3 post = p.add(side.scale(s * (half - 1)));
					for (double y = 0; y <= 4; y += 0.5) {
						level.sendParticles(EDGE, post.x, post.y + y, post.z, 1, 0.02, 0.02, 0.02, 0);
					}
				}
				for (double w = -half; w <= half; w += 0.5) {
					Vec3 beam = p.add(side.scale(w));
					level.sendParticles(EDGE, beam.x, beam.y + 4.2, beam.z, 1, 0.02, 0.02, 0.02, 0);
				}
			}
		}
	}
}
