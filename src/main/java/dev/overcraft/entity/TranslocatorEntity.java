package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
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
 * Sombra's Translocator beacon (entity 1575, graph 0C1A): thrown at 72 m/s under a gravity of 20 (M), it sticks where it
 * lands (M) and lasts 15 s; enemies can shoot it, 5 health (D). She teleports to it with E again (Sombra.teleport).
 */
public class TranslocatorEntity extends OwProjectile {
	private boolean stuck;
	/** The face it stuck to, outward (none in flight). */
	private net.minecraft.world.phys.Vec3 face = net.minecraft.world.phys.Vec3.ZERO;
	private double health = SombraStats.BEACON_HEALTH;

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

	@Override
	protected int lifetime() {
		return Ow.ticks(SombraStats.BEACON_LIFETIME);
	}

	@Override
	protected double getDefaultGravity() {
		return stuck ? 0 : Ow.accel(SombraStats.BEACON_GRAVITY);
	}

	@Override
	public void tick() {
		if (stuck) {
			setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		}
		super.tick();
	}

	/** It flies through creatures (its throw only stops on the world, M). */
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
		// sticks where it lands, a hair off the face (M: 0C1A's stick fields aren't read)
		stuck = true;
		face = net.minecraft.world.phys.Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
		setPos(hit.getLocation().add(face.scale(0.08)));
		setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		setNoGravity(true);
	}

	public net.minecraft.world.phys.Vec3 face() {
		return face;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	/** An enemy's hit takes its 5 health (in Overwatch units); her own side's doesn't. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (getOwner() instanceof ServerPlayer owner && attacker instanceof LivingEntity living && !Allies.canHurt(living, owner)) {
			return false;
		}
		health -= Ow.toOw(amount);
		if (health <= 0) {
			expire();
		}
		return true;
	}

	@Override
	protected void expire() {
		if (getOwner() instanceof ServerPlayer owner) {
			dev.overcraft.sombra.Sombra.beaconGone(owner, this);
		}
		discard();
	}
}
