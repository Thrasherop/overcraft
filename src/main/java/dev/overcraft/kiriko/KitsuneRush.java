package dev.overcraft.kiriko;

import dev.overcraft.Ow;
import dev.overcraft.entity.KitsuneEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
	private static final float ULT_LINE_VOLUME = 4.0f;
	/** Ticks the fox stays at the end of its run before vanishing in a puff (ours). */
	private static final int FOX_FADE_TICKS = 6;

	/** Torii gates along the path: six over its 32.2 m (the effects survey: one gate effect per gate sound, six). */
	private static final int GATES = 6;
	/** Talisman tiles laid on the path every this many steps (half-metres: ours). */
	private static final int TILE_STEPS = 3;
	/** Ground ripples sent along the path, one every this many ticks (the game: 3 + 2 a second). */
	private static final int RIPPLE_TICKS = 4;
	/** Speed lines on allies in the corridor, every this many ticks (ours: the game streams 40 a second). */
	private static final int ALLY_LINES_TICKS = 3;

	private final ServerPlayer owner;
	private final ServerLevel level;
	private final Vec3 direction;
	private final List<Vec3> path = new ArrayList<>();
	private Vec3 fox;
	private double laid;
	private boolean laying = true;
	private int age;
	/** The fox clients draw (when they have its model): follows {@link #fox}, gone a little after the run ends. */
	private final KitsuneEntity foxEntity;
	private int doneAt = -1;
	private int gatesPlaced;

	private KitsuneRush(ServerPlayer owner) {
		this.owner = owner;
		this.level = owner.level();
		Vec3 look = owner.getViewVector(1.0f);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		this.direction = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
		this.fox = owner.position();
		path.add(fox);
		foxEntity = new KitsuneEntity(OcEntities.KITSUNE, level);
		foxEntity.snapTo(fox.x, fox.y, fox.z, (float) Math.toDegrees(Math.atan2(-direction.x, direction.z)), 0);
		level.addFreshEntity(foxEntity);
	}

	public static void start(ServerPlayer owner) {
		ACTIVE.add(new KitsuneRush(owner));
		// the cast line, as Overwatch plays it: Kiriko and her allies hear "Let the Kitsune guide you!", enemies the
		// Japanese line. Loud enough to carry across a fight (volume 4 = 64 blocks: ours)
		for (ServerPlayer listener : owner.level().players()) {
			SoundEvent line = Allies.isSelfOrAlly(owner, listener) ? OcSounds.KIRIKO_ULT_FRIENDLY : OcSounds.KIRIKO_ULT_ENEMY;
			listener.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(line), SoundSource.PLAYERS,
				owner.getX(), owner.getEyeY(), owner.getZ(), ULT_LINE_VOLUME, 1.0f, owner.getRandom().nextLong()));
		}
		owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), OcSounds.KIRIKO_RUSH_SUMMON, SoundSource.PLAYERS, 1, 1);
		HeroSounds.split(owner, OcSounds.KIRIKO_RUSH_CAST, OcSounds.KIRIKO_RUSH_CAST_3P, owner.getEyePosition());
		Effects.play(owner.level(), "rush_cast", owner.getEyePosition().add(owner.getViewVector(1.0f).scale(0.6)).add(0, -0.4, 0));
	}

	public static void tickAll(MinecraftServer server) {
		ACTIVE.removeIf(rush -> !rush.tick());
	}

	public static void clear() {
		ACTIVE.forEach(rush -> rush.foxEntity.discard());
		ACTIVE.clear();
	}

	/** One tick; false when the corridor is gone. */
	private boolean tick() {
		if (age++ >= Ow.ticks(RUSH_DURATION)) {
			foxEntity.discard();
			return false;
		}
		if (laying) {
			double perTick = Ow.speed(RUSH_FOX_SPEED);
			for (double moved = 0; moved < perTick && laying; moved += STEP) {
				advanceFox();
			}
			foxEntity.setPos(fox);
			placeGates();
		} else if (doneAt < 0) {
			doneAt = age;
		} else if (age - doneAt > FOX_FADE_TICKS && !foxEntity.isRemoved()) {
			Effects.play(level, "rush_end", fox);
			foxEntity.discard();
		}
		buffAllies();
		if (age % RIPPLE_TICKS == 0 && path.size() > 1) {
			Effects.play(level, "rush_ripple", path.get(owner.getRandom().nextInt(path.size())));
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
		if (path.size() % TILE_STEPS == 0) {
			double seconds = (Ow.ticks(RUSH_DURATION) - age) / 20.0;
			Effects.play(level, "rush_tiles", fox, direction, null, (int) Math.round(seconds * 10));
		}
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
				if (age % ALLY_LINES_TICKS == 0) {
					Effects.play(level, "rush_ally", entity.position(), direction, entity, 0);
				}
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

	/** Each gate rises as the fox passes its place, and stands until the corridor ends (its effect lasts as long). */
	private void placeGates() {
		double spacing = RUSH_LENGTH / GATES;
		while (gatesPlaced < GATES && laid >= spacing * (gatesPlaced + 0.5)) {
			int index = Math.min(path.size() - 1, (int) Math.round(spacing * (gatesPlaced + 0.5) / STEP));
			Vec3 across = new Vec3(-direction.z, 0, direction.x).scale(RUSH_WIDTH - 2);
			double seconds = (Ow.ticks(RUSH_DURATION) - age) / 20.0;
			Vec3 gate = path.get(index);
			Effects.play(level, "rush_gate", gate, across, null, (int) Math.round(seconds * 10));
			// each gate its own chime, rising from the first to the sixth
			SoundEvent chime = OcSounds.KIRIKO_RUSH_GATES[Math.min(gatesPlaced, OcSounds.KIRIKO_RUSH_GATES.length - 1)];
			level.playSound(null, gate.x, gate.y + 2, gate.z, chime, SoundSource.PLAYERS, 1, 1);
			gatesPlaced++;
		}
	}
}
