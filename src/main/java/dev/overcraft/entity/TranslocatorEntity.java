package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.sombra.SombraStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Sombra's Translocator beacon (entity 1575, graph 0C1A): thrown at 72 m/s under a gravity of 20 (M), it takes her (Sombra.arrive)
 * on its first touch of terrain or 12.25 m along its path, wherever it is then (BZ 2026-10-06; W). It passes through creatures
 * and can't be damaged (W).
 */
public class TranslocatorEntity extends OwProjectile {
	/** The face it touched, outward (none when it stopped in the air). */
	private net.minecraft.world.phys.Vec3 face = net.minecraft.world.phys.Vec3.ZERO;
	/** How far it has come along its path, and where this tick's move started. */
	private double travelled;
	private net.minecraft.world.phys.Vec3 tickStart = net.minecraft.world.phys.Vec3.ZERO;

	public TranslocatorEntity(EntityType<? extends TranslocatorEntity> type, Level level) {
		super(type, level);
	}

	public TranslocatorEntity(LivingEntity owner, Level level) {
		super(OcEntities.TRANSLOCATOR, owner, level, new ItemStack(Items.ENDER_EYE));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.ENDER_EYE;
	}

	/** It takes her within 0.17 s: a second at most is only a safety. */
	@Override
	protected int lifetime() {
		return 20;
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(SombraStats.BEACON_GRAVITY);
	}

	@Override
	public void tick() {
		tickStart = position();
		super.tick();
		if (level().isClientSide() || !isAlive()) {
			return;
		}
		net.minecraft.world.phys.Vec3 moved = position().subtract(tickStart);
		travelled += moved.length();
		if (travelled >= SombraStats.BEACON_RANGE && moved.lengthSqr() > 0) {
			// its range reached inside this tick's move: back to the exact point on the path
			setPos(position().subtract(moved.normalize().scale(travelled - SombraStats.BEACON_RANGE)));
			arrive();
		}
	}

	/** It flies through creatures (W: no collision with heroes or deployables). */
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
		if (level().isClientSide() || !isAlive()) {
			return;
		}
		net.minecraft.world.phys.Vec3 path = hit.getLocation().subtract(tickStart);
		if (travelled + path.length() > SombraStats.BEACON_RANGE && path.lengthSqr() > 0) {
			// the wall lies past its range: it stops in the air before it
			setPos(tickStart.add(path.normalize().scale(SombraStats.BEACON_RANGE - travelled)));
		} else {
			// a hair off the face it touched
			face = net.minecraft.world.phys.Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
			setPos(hit.getLocation().add(face.scale(0.08)));
		}
		arrive();
	}

	public net.minecraft.world.phys.Vec3 face() {
		return face;
	}

	private void arrive() {
		if (getOwner() instanceof ServerPlayer owner) {
			dev.overcraft.sombra.Sombra.arrive(owner, this);
		}
		discard();
	}

	/** It can't be damaged (W). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void expire() {
		if (getOwner() instanceof ServerPlayer owner) {
			dev.overcraft.sombra.Sombra.beaconGone(owner, this);
		}
		discard();
	}

	@Override
	public void remove(RemovalReason reason) {
		// out of the world or unloaded: she gets her cooldown back (W: a death plane refunds it)
		if (reason != RemovalReason.DISCARDED && getOwner() instanceof ServerPlayer owner) {
			dev.overcraft.sombra.Sombra.beaconGone(owner, this);
		}
		super.remove(reason);
	}
}
