package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Effects;
import dev.overcraft.lifeweaver.Lifeweaver;
import dev.overcraft.lifeweaver.LifeweaverStats;
import dev.overcraft.registry.OcEntities;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What Lifeweaver throws from his left wrist (his model 126CC): Petal Platform's seed (entity 450F, 50 m/s) or Sow the
 * Seed's (6F4C, 25 m/s), both with gravity 9.8. It deploys only on the ground: a wall or a ceiling turns it back (W "it
 * bounces"; how much is ours: half its speed), creatures don't stop it.
 */
public class PetalThrowEntity extends OwProjectile {
	private static final EntityDataAccessor<Boolean> SEED = SynchedEntityData.defineId(PetalThrowEntity.class, EntityDataSerializers.BOOLEAN);
	/** A bounce keeps this much of the speed (ours: the data's bounce block isn't read, L). */
	private static final double BOUNCE = 0.5;

	public PetalThrowEntity(EntityType<? extends PetalThrowEntity> type, Level level) {
		super(type, level);
	}

	public PetalThrowEntity(LivingEntity owner, Level level, boolean seed) {
		super(OcEntities.PETAL_THROW, owner, level, new ItemStack(Items.PINK_PETALS));
		entityData.set(SEED, seed);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SEED, false);
	}

	/** Sow the Seed's, not Petal Platform's. */
	public boolean seed() {
		return entityData.get(SEED);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.PINK_PETALS;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(LifeweaverStats.PLATFORM_THROW_LIFETIME);
	}

	@Override
	public float hitRadius() {
		return (float) LifeweaverStats.PLATFORM_THROW_RADIUS;
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(LifeweaverStats.THROW_GRAVITY);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return false;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 v = getDeltaMovement();
		if (hit.getDirection() != Direction.UP) {
			// a wall or a ceiling: back off it, slower
			Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
			Vec3 bounced = v.subtract(n.scale(2 * v.dot(n))).scale(BOUNCE);
			setPos(hit.getLocation().add(n.scale(hitRadius() + 0.01)));
			setDeltaMovement(bounced);
			return;
		}
		Vec3 at = hit.getLocation();
		ServerPlayer owner = getOwner() instanceof ServerPlayer p ? p : null;
		if (seed()) {
			SeedEntity seed = new SeedEntity(level, owner, at);
			level.addFreshEntity(seed);
		} else if (owner != null) {
			deploy(level, owner, at);
		}
		discard();
	}

	/** The platform opens where it landed, if there's room 2 m above (D 2219); else it's gone, the cooldown almost over. */
	private void deploy(ServerLevel level, ServerPlayer owner, Vec3 at) {
		double r = LifeweaverStats.PLATFORM_RADIUS;
		AABB room = new AABB(at.x - r * 0.5, at.y + 0.05, at.z - r * 0.5, at.x + r * 0.5, at.y + LifeweaverStats.PLATFORM_CLEARANCE,
			at.z + r * 0.5);
		if (!level.noCollision(room)) {
			Effects.play(level, "lifeweaver_platform_blocked", at, owner);
			Lifeweaver.blocked(owner);
			return;
		}
		PetalPlatformEntity platform = new PetalPlatformEntity(level, owner, at);
		level.addFreshEntity(platform);
		Lifeweaver.deployed(owner, platform);
	}
}
