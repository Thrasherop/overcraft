package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.junkrat.Junkrat;
import dev.overcraft.junkrat.JunkratStats;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Junkrat's RIP-Tire (pet entity 039C, graphs 0234 / 0233; junkrat-map.md): 0.9 m across, 0.8 m tall, 100 health. The server drives
 * it from his keys and his facing ({@link Junkrat#driveTire}: the one authority; his own client only follows it and holds him still),
 * for 10 s at most; he sets it off with either fire button or Q from 1.6 s on ({@link Junkrat#tireBlast}). Broken by enemies: no blast
 * (D 0233 #518). Never saved.
 */
public class RipTireEntity extends Entity {
	/** Whose tire (an entity id, -1 none), and Nitro Boost on (its look and sound). */
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(RipTireEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> BOOST = SynchedEntityData.defineId(RipTireEntity.class, EntityDataSerializers.BOOLEAN);
	private double health = JunkratStats.TIRE_HEALTH;
	/** Ticks since it came. */
	public int age;
	/** Whether it stood on the ground last tick (its landing sound), and the jump key last tick. */
	public boolean wasDown, jumpWas;

	public RipTireEntity(EntityType<? extends RipTireEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(OWNER, -1);
		builder.define(BOOST, false);
	}

	public void setOwner(Entity owner) {
		entityData.set(OWNER, owner.getId());
	}

	public int ownerId() {
		return entityData.get(OWNER);
	}

	public @Nullable Entity owner() {
		return level().getEntity(entityData.get(OWNER));
	}

	public boolean boosted() {
		return entityData.get(BOOST);
	}

	public void setBoosted(boolean on) {
		entityData.set(BOOST, on);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			return;
		}
		age++;
		if (!(owner() instanceof ServerPlayer owner) || owner.isRemoved()) {
			// its owner is gone from this world (left, another dimension): the ult is over, no blast (ours)
			discard();
			return;
		}
		Junkrat.driveTire(owner, this);
	}

	/** It rolls up a knee-high step as a hero walks one (ours: the data's step field 35 isn't read as metres, L). */
	@Override
	public float maxUpStep() {
		return 0.6f;
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
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return false;
	}

	/** An enemy's damage: at 100 it breaks, no blast (D 03D9, 0233 #518). His own and his allies' do nothing. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved()) {
			return false;
		}
		Entity owner = owner();
		if (source.getEntity() instanceof LivingEntity attacker && owner instanceof LivingEntity o
			&& (attacker == o || !Allies.canHurt(o, attacker))) {
			return false;
		}
		health -= Ow.toOw(amount);
		if (health <= 0 && owner instanceof ServerPlayer o) {
			Junkrat.tireBroken(o, this);
		}
		return true;
	}

	public double health() {
		return health;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
