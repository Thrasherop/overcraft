package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.registry.OcEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * D.Va's MEKA while nobody drives it (the data's pet entity 0407, model FBB0): falling in on Call Mech until she's linked
 * (dev.overcraft.dva.Dva removes it then), and the wreck she leaves behind on the eject (0.75 s, D 0276 #60). While she's
 * in it, the mech is the player herself (dva-plan.md "What's new": two bodies as one player). Not a creature: mobs ignore
 * it, nothing hurts it (ours, M). Its clips by kind (DvaMechRenderer): the landing 0xE5, the destruction 0x8.
 */
public class DvaMechEntity extends Entity {
	public static final int CALL = 0, WRECK = 1;
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(DvaMechEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(DvaMechEntity.class, EntityDataSerializers.INT);

	private @Nullable UUID ownerId;

	public DvaMechEntity(EntityType<? extends DvaMechEntity> type, Level level) {
		super(type, level);
	}

	public DvaMechEntity(ServerLevel level, ServerPlayer owner, int kind, Vec3 at, float yaw) {
		this(OcEntities.DVA_MECH, level);
		setPos(at);
		setYRot(yaw);
		yRotO = yaw;
		ownerId = owner.getUUID();
		entityData.set(OWNER, owner.getId());
		entityData.set(KIND, kind);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(OWNER, -1);
		builder.define(KIND, CALL);
	}

	public int ownerId() {
		return entityData.get(OWNER);
	}

	public int kind() {
		return entityData.get(KIND);
	}

	@Override
	public void tick() {
		super.tick();
		// it keeps whatever motion it has and falls (no mover of its own: D 0B2F has no mover node, M)
		if (!onGround()) {
			setDeltaMovement(getDeltaMovement().add(0, -Ow.speed(17.5) / 20, 0));
		}
		move(MoverType.SELF, getDeltaMovement());
		if (onGround()) {
			setDeltaMovement(getDeltaMovement().multiply(0.5, 0, 0.5));
		}
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
		// the wreck lingers 0.75 s (D 0276 #60 v69943 = 1B58); a called mech goes when she drives it (or 3 s at most: ours)
		double life = kind() == WRECK ? dev.overcraft.dva.DvaStats.WRECK_LINGER : 3;
		if (owner == null || tickCount >= Ow.ticks(life)) {
			discard();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
