package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.lifeweaver.Lifeweaver;
import dev.overcraft.registry.OcEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static dev.overcraft.lifeweaver.LifeweaverStats.*;

/**
 * Tree of Life (his model 1267E, entity 4525, graphs 36AA + 5A52): placed where he aimed, it heals allies within 12 m in
 * its sight (from its trunk 1.8 m up) 150 as it sprouts, then 90 every 1.75 s, 8 times (each over 0.15 s); half of what
 * that overheals becomes overhealth, up to 100, held 6 s after their last pulse, then 20 a second away. 1200 health,
 * 15 s. Its trunk (1 m round, 6.5 m tall from 0.5 m under its root) is solid; its canopy isn't (ours: the data's canopy
 * collider, 3 m round at 4.5 m, is the owner's call, lifeweaver-plan.md). Allies inside the trunk are moved out.
 */
public class TreeOfLifeEntity extends Entity {
	/** Which growth it plays: in the open, by a wall, or under a low ceiling (D 36AA #297's survey). */
	public static final int GROW = 0, GROW_LEANING = 1, GROW_ENCLOSED = 2;

	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(TreeOfLifeEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> GROWTH = SynchedEntityData.defineId(TreeOfLifeEntity.class, EntityDataSerializers.INT);
	/** Ticks of life it was given (its end plays 1.67 s before). */
	private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(TreeOfLifeEntity.class, EntityDataSerializers.INT);
	/** The free height over it (m), for the enclosed growth's cut. */
	private static final EntityDataAccessor<Float> ROOM = SynchedEntityData.defineId(TreeOfLifeEntity.class, EntityDataSerializers.FLOAT);

	private @Nullable UUID ownerId;
	private double health = TREE_HEALTH;
	/** The heal now landing (Overwatch points a tick, ticks left) and who's in this window's reach already. */
	private final Set<UUID> reached = new HashSet<>();

	public TreeOfLifeEntity(EntityType<? extends TreeOfLifeEntity> type, Level level) {
		super(type, level);
	}

	public TreeOfLifeEntity(ServerLevel level, ServerPlayer owner, Vec3 root) {
		this(OcEntities.TREE_OF_LIFE, level);
		// its box starts 0.5 m under its root (D 5A52's trunk collider)
		setPos(root.x, root.y - TRUNK_BELOW, root.z);
		setYRot(owner.getYRot() + 180);
		ownerId = owner.getUUID();
		entityData.set(OWNER, owner.getId());
		entityData.set(LIFE, Ow.ticks(TREE_TIME));
		double room = room(level, root);
		entityData.set(ROOM, (float) room);
		entityData.set(GROWTH, room < TRUNK_HEIGHT ? GROW_ENCLOSED : nearWall(level, root) ? GROW_LEANING : GROW);
		Effects.play(level, "lifeweaver_tree_spawn", root, owner);
	}

	/** The free height over its root, up to the trunk's height and a bit (ours: one ray; the data surveys several). */
	private double room(ServerLevel level, Vec3 root) {
		Vec3 from = root.add(0, 0.1, 0);
		var hit = level.clip(new ClipContext(from, from.add(0, TRUNK_HEIGHT + 2, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().y - from.y : TRUNK_HEIGHT + 2;
	}

	/** A wall within the canopy's reach at its height (ours, for the leaning growth). */
	private boolean nearWall(ServerLevel level, Vec3 root) {
		Vec3 at = root.add(0, CANOPY_AT, 0);
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * i / 4;
			Vec3 to = at.add(Math.cos(a) * CANOPY_RADIUS, 0, Math.sin(a) * CANOPY_RADIUS);
			if (level.clip(new ClipContext(at, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.BLOCK) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(OWNER, -1);
		builder.define(GROWTH, GROW);
		builder.define(LIFE, Ow.ticks(TREE_TIME));
		builder.define(ROOM, 10f);
	}

	public int ownerId() {
		return entityData.get(OWNER);
	}

	public int growth() {
		return entityData.get(GROWTH);
	}

	public float room() {
		return entityData.get(ROOM);
	}

	/** Ticks left. */
	public int left() {
		return entityData.get(LIFE) - tickCount;
	}

	/** Its root (the box starts 0.5 m under it). */
	public Vec3 root() {
		return position().add(0, TRUNK_BELOW, 0);
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return true;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null || owner.level() != level) {
			// its owner gone: so is it (D 36AA #241)
			end();
			return;
		}
		int t = tickCount;
		if (t >= entityData.get(LIFE)) {
			end();
			return;
		}
		// the sprout at once, then a pulse every 1.75 s (1.75 ... 14.0 s): each a 0.35 s window whose reach grows 2.5 ->
		// 12 m over its first 0.25 s, everyone in it healed once (D 36AA #534 / #365 / #367)
		int every = Ow.ticks(TREE_PULSE_EVERY);
		int window = Ow.ticks(0.35);
		int pulse = t / every;
		int into = t % every;
		if (pulse <= TREE_PULSES && into < window) {
			if (into == 0) {
				reached.clear();
				if (pulse > 0) {
					Effects.play(level, "lifeweaver_tree_pulse", root(), Vec3.ZERO, this, pulse, owner);
				}
			}
			double reach = 2.5 + (TREE_RADIUS - 2.5) * Math.min(1, into / (double) Ow.ticks(0.25));
			heal(level, owner, reach, pulse == 0 ? TREE_SPROUT : TREE_PULSE, pulse > 0);
		}
		unstick(level, owner);
	}

	/** Heals every ally in reach and in sight of the trunk not yet reached this window. */
	private void heal(ServerLevel level, ServerPlayer owner, double reach, double amount, boolean pulse) {
		Vec3 eye = root().add(0, TREE_EYE, 0);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach))) {
			if (!Allies.isSelfOrAlly(owner, e) || reached.contains(e.getUUID()) || e.getBoundingBox().getCenter().distanceTo(root()) > reach) {
				continue;
			}
			Vec3 to = e.getBoundingBox().getCenter();
			if (level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS
				&& level.clip(new ClipContext(eye, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
				!= HitResult.Type.MISS) {
				continue;
			}
			reached.add(e.getUUID());
			double missing = Ow.toOw(e.getMaxHealth() - e.getHealth());
			Lifeweaver.healOver(owner, e, amount, TREE_PULSE_OVER, false);
			// half the overheal becomes overhealth (up to 100); every pulse holds it 6 s more (D 5A52 #554-#564)
			double over = Math.max(0, amount - missing) * TREE_OVERHEAL;
			if (over > 0 || pulse) {
				Combat.overhealthPool(e, over, TREE_OVERHEALTH_MAX, TREE_OVERHEALTH_HOLD, TREE_OVERHEALTH_DECAY);
			}
			Effects.play(level, "lifeweaver_tree_heal", e.getBoundingBox().getCenter(), Vec3.ZERO, e, 0, owner);
		}
	}

	/** Allies inside the trunk are moved out (D 36AA #567: 1 m out from within 0.65 m of it). */
	private void unstick(ServerLevel level, ServerPlayer owner) {
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox())) {
			if (e instanceof ServerPlayer) {
				continue; // a player's own client keeps them out of its box (Minecraft's collision)
			}
			Vec3 away = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
			away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
			e.move(MoverType.SHULKER, away.scale(0.25));
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || isInvulnerableToBase(source)) {
			return false;
		}
		health -= Ow.toOw(amount);
		Effects.play(level, "lifeweaver_tree_hit", root().add(0, 1.5, 0), null);
		if (health <= 0) {
			end();
		}
		return true;
	}

	/** It's gone (its time, its health, felled with Q, a new tree, its owner gone). */
	public void end() {
		if (isRemoved()) {
			return;
		}
		if (level() instanceof ServerLevel level) {
			Effects.play(level, "lifeweaver_tree_end", root().add(0, 3, 0), null);
			ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
			if (owner != null) {
				Lifeweaver.treeEnded(owner, this);
				dev.overcraft.hero.Voice.say(owner, "tree_end", 1);
			}
		}
		discard();
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
