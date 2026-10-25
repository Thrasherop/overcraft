package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.kiriko.KirikoStats;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Healing Ofuda. Locked on an ally (yellow) it homes on them; unlocked (blue) it flies straight and heals any ally it
 * touches. Passes through enemies, breaks on terrain.
 */
public class OfudaEntity extends OwProjectile {
	private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(OfudaEntity.class, EntityDataSerializers.INT);
	private static final DustParticleOptions LOCKED = new DustParticleOptions(0xFFE8C547, 0.6f);
	private static final DustParticleOptions UNLOCKED = new DustParticleOptions(0xFF6FC3FF, 0.6f);

	public OfudaEntity(EntityType<? extends OfudaEntity> type, Level level) {
		super(type, level);
	}

	public OfudaEntity(LivingEntity owner, Level level, @Nullable LivingEntity target) {
		super(OcEntities.OFUDA, owner, level, new ItemStack(OcItems.OFUDA));
		entityData.set(TARGET, target == null ? -1 : target.getId());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TARGET, -1);
	}

	@Override
	protected Item getDefaultItem() {
		return OcItems.OFUDA;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(KirikoStats.OFUDA_LIFETIME);
	}

	private @Nullable LivingEntity target() {
		int id = entityData.get(TARGET);
		return id >= 0 && level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
	}

	@Override
	public void tick() {
		LivingEntity target = target();
		if (target != null) {
			Vec3 to = target.getBoundingBox().getCenter().subtract(position());
			if (to.lengthSqr() > 1.0E-4) {
				setDeltaMovement(to.normalize().scale(Ow.speed(KirikoStats.OFUDA_SPEED)));
			}
		}
		super.tick();
		if (level().isClientSide()) {
			level().addParticle(target != null ? LOCKED : UNLOCKED, getX(), getY(), getZ(), 0, 0, 0);
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && getOwner() instanceof LivingEntity owner && Allies.isAlly(owner, entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (!(level() instanceof ServerLevel) || !(hit.getEntity() instanceof LivingEntity ally)) {
			return;
		}
		float healed = Combat.heal(ally, KirikoStats.OFUDA_HEAL);
		if (getOwner() instanceof Player owner) {
			Combat.addUltCharge(owner, healed);
			// Medic: weapon healing also heals Kiriko (no ult charge for that part)
			if (healed > 0) {
				Combat.heal(owner, healed * KirikoStats.MEDIC_SELF_HEAL);
			}
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!level().isClientSide()) {
			discard();
		}
	}
}
