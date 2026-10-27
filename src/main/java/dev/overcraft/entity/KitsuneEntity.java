package dev.overcraft.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Kitsune Rush's spirit fox, for clients to draw: KitsuneRush moves it along the corridor it lays and removes it when
 * the run ends. Nothing else: it can't be hit, has no physics and is never saved.
 */
public class KitsuneEntity extends Entity {
	/** Whose fox it is (an entity id, -1 none): its enemies see it in red. */
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(KitsuneEntity.class,
		EntityDataSerializers.INT);

	public KitsuneEntity(EntityType<? extends KitsuneEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(OWNER, -1);
	}

	public void setOwner(Entity owner) {
		entityData.set(OWNER, owner.getId());
	}

	public @Nullable Entity owner() {
		return level().getEntity(entityData.get(OWNER));
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
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
