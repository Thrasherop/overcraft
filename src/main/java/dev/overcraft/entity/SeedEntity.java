package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.registry.OcEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static dev.overcraft.lifeweaver.LifeweaverStats.*;

/**
 * Sow the Seed's seed on the ground (entity 6F11, graph 5C5A; his model 126CC): 14 s (pulsing its last 3), taken by the
 * first ally within 3 m from 0.3 s after it lands, not by him (D: M). They get 75 overhealth, held 5 s then going 25 a
 * second, and 25% more speed for 5 s (graph 5C5C).
 */
public class SeedEntity extends Entity {
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(SeedEntity.class, EntityDataSerializers.INT);
	private static final Identifier SPEED_ID = dev.overcraft.Overcraft.id("sow_the_seed");

	private @Nullable UUID ownerId;

	public SeedEntity(EntityType<? extends SeedEntity> type, Level level) {
		super(type, level);
	}

	public SeedEntity(ServerLevel level, @Nullable ServerPlayer owner, Vec3 at) {
		this(OcEntities.SEED, level);
		setPos(at);
		if (owner != null) {
			ownerId = owner.getUUID();
			entityData.set(OWNER, owner.getId());
		}
		Effects.play(level, "lifeweaver_seed_land", at, owner);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(OWNER, -1);
	}

	public int ownerId() {
		return entityData.get(OWNER);
	}

	/** Its last 3 s: it pulses (D 5C5A). */
	public boolean ending() {
		return tickCount >= Ow.ticks(SEED_TIME - 3);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null || tickCount >= Ow.ticks(SEED_TIME)) {
			discard();
			return;
		}
		if (tickCount < Ow.ticks(SEED_PICKUP_FROM)) {
			return;
		}
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SEED_PICKUP))) {
			if (e != owner && Allies.isAlly(owner, e) && e.distanceTo(this) <= SEED_PICKUP) {
				Combat.overhealthPool(e, SEED_OVERHEALTH, SEED_OVERHEALTH, SEED_BUFF_TIME, SEED_OVERHEALTH_DECAY);
				Combat.buff(e, Attributes.MOVEMENT_SPEED, SPEED_ID, SEED_SPEED_BONUS, SEED_BUFF_TIME);
				Effects.play(level, "lifeweaver_seed_taken", e.position(), Vec3.ZERO, e, 0, owner);
				discard();
				return;
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
