package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.tracer.Tracer;
import dev.overcraft.tracer.TracerStats;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Tracer's Pulse Bomb (entity 0045, graph 0C1B; tracer-map.md): thrown at 15 m/s, falling at 30 m/s², 0.22 m. It sticks to the
 * first enemy it touches (5 to it, and it rides them) or the first block, and goes off 1 s later ({@link Tracer#explode}), she
 * alive or not (D 0C1B: nothing tests the owner, M). Allies and she don't stop it. Unstuck it lives 99 s (D).
 */
public class PulseBombEntity extends OwProjectile {
	/** Ticks since it stuck (-1 in flight). */
	private int fuse = -1;
	private @Nullable Entity stuckTo;
	/** Its place against what it's stuck to (its feet), or its own place on a block. */
	private Vec3 offset = Vec3.ZERO;
	/** Synced: stuck, so its client copy stops flying and only follows the server's place. */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> STUCK = net.minecraft.network.syncher.SynchedEntityData
		.defineId(PulseBombEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

	public PulseBombEntity(EntityType<? extends PulseBombEntity> type, Level level) {
		super(type, level);
	}

	public PulseBombEntity(LivingEntity owner, Level level) {
		super(OcEntities.PULSE_BOMB, owner, level, new ItemStack(Items.FIRE_CHARGE));
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STUCK, false);
	}

	public boolean stuck() {
		return entityData.get(STUCK);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIRE_CHARGE;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(TracerStats.BOMB_LIFETIME);
	}

	@Override
	protected double getDefaultGravity() {
		// 30 m/s² (D m_599AB8D7 v7389 30, M field)
		return Ow.accel(TracerStats.BOMB_GRAVITY);
	}

	@Override
	public float hitRadius() {
		return (float) TracerStats.BOMB_RADIUS;
	}

	@Override
	public double barrierDamage() {
		return TracerStats.BOMB_STICK;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof PulseBombEntity)
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			if (stuck()) {
				// (the server's place comes with each update: a projectile has no interpolation)
				commonTick();
			} else {
				super.tick();
			}
			return;
		}
		if (fuse < 0) {
			super.tick();
			return;
		}
		// stuck: it goes where what it's on goes, and counts down
		setDeltaMovement(Vec3.ZERO);
		if (stuckTo != null) {
			if (stuckTo.isAlive() && stuckTo.level() == level()) {
				setPos(stuckTo.position().add(offset));
			} else {
				// what it rode is gone: it stays where it was (ours)
				stuckTo = null;
			}
		}
		if (++fuse >= Ow.ticks(TracerStats.BOMB_FUSE)) {
			if (getOwner() instanceof ServerPlayer owner) {
				Tracer.explode(owner, this, position().add(0, getBbHeight() / 2, 0), stuckTo);
			}
			discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (fuse >= 0 || !(level() instanceof ServerLevel)) {
			return;
		}
		Entity e = hit.getEntity();
		LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
		Entity ride = target != null ? target : e;
		// on the box's face where it touched, kept against the creature's feet
		net.minecraft.world.phys.AABB box = e.getBoundingBox();
		Vec3 at = hit.getLocation();
		Vec3 on = new Vec3(Math.clamp(at.x, box.minX, box.maxX), Math.clamp(at.y, box.minY, box.maxY), Math.clamp(at.z, box.minZ, box.maxZ));
		stick(ride, on.subtract(ride.position()), on, target);
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		if (fuse >= 0) {
			return;
		}
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel)) {
			return;
		}
		Direction face = hit.getDirection();
		Vec3 at = hit.getLocation().add(face.getStepX() * 0.11, face.getStepY() * 0.11 - (face == Direction.UP ? 0 : getBbHeight() / 2),
			face.getStepZ() * 0.11);
		stick(null, Vec3.ZERO, at, null);
	}

	@Override
	public void hitBarrier(Vec3 at) {
		// an enemy barrier: it sticks there (ours: the wiki says it sticks to barriers; it stays where it met it)
		if (fuse < 0) {
			stick(null, Vec3.ZERO, at, null);
		}
	}

	private void stick(@Nullable Entity ride, Vec3 offset, Vec3 at, @Nullable LivingEntity target) {
		fuse = 0;
		entityData.set(STUCK, true);
		stuckTo = ride;
		this.offset = offset;
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		setPos(ride != null ? ride.position().add(offset) : at);
		if (getOwner() instanceof ServerPlayer owner) {
			Tracer.stuck(owner, this, target, at);
		}
	}

	@Override
	protected void expire() {
		if (fuse < 0) {
			discard();
		}
	}
}
