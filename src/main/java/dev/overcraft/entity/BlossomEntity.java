package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.lifeweaver.Lifeweaver;
import dev.overcraft.lifeweaver.LifeweaverStats;
import dev.overcraft.registry.OcEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One Healing Blossom (his graph 21C6, the projectile 35FD run by script 369A): thrown from his right hand at an ally
 * already picked, toward his crosshair, at 30 + 30 x charge m/s (60 from 90%). It flies straight for half its straight
 * flight time, then turns onto the ally's chest over a quarter more (369A's timing fields: M), and from there homes. It
 * passes through blocks (369A only ray-casts for its enter and leave effects: M) and through anyone else: only its ally
 * takes it. It's gone if they die, or after 8 s. Moved on both sides (no Minecraft collision), the same way.
 */
public class BlossomEntity extends OwProjectile {
	private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(BlossomEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> CHARGE = SynchedEntityData.defineId(BlossomEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(BlossomEntity.class, EntityDataSerializers.FLOAT);

	/** The heal it brings (Overwatch points), and over how long it lands (s): set by the thrower, server side. */
	private double heal;
	private double healOver;
	/** Ticks it flies straight before turning, and the turn's length. */
	private int straight = 1;
	private int turn = 1;

	public BlossomEntity(EntityType<? extends BlossomEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public BlossomEntity(LivingEntity owner, Level level, LivingEntity target, double charge, double heal) {
		super(OcEntities.BLOSSOM, owner, level, new ItemStack(Items.PINK_PETALS));
		noPhysics = true;
		entityData.set(TARGET, target.getId());
		entityData.set(CHARGE, (float) charge);
		entityData.set(SPEED, (float) LifeweaverStats.blossomSpeed(charge));
		this.heal = heal;
		this.healOver = LifeweaverStats.HEAL_OVER * charge;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TARGET, -1);
		builder.define(CHARGE, 0f);
		builder.define(SPEED, (float) LifeweaverStats.BLOSSOM_SPEED);
	}

	/** Its charge (0-1): its look grows with it (three sizes of release in his data). */
	public float charge() {
		return entityData.get(CHARGE);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.PINK_PETALS;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(LifeweaverStats.BLOSSOM_LIFETIME);
	}

	@Override
	public float hitRadius() {
		return (float) LifeweaverStats.BLOSSOM_RADIUS;
	}

	public @Nullable LivingEntity target() {
		int id = entityData.get(TARGET);
		return id >= 0 && level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
	}

	/** Where it heads on its ally: the chest (his hardpoint 0001, 1.47 m of 2.17: two thirds of the height). */
	public static Vec3 chest(LivingEntity target) {
		return target.position().add(0, target.getBbHeight() * 0.68, 0);
	}

	@Override
	public void launch(Vec3 from, Vec3 direction, double speed) {
		super.launch(from, direction, speed);
		LivingEntity target = target();
		if (target != null) {
			// the straight flight's time: the distance to the ally at its speed
			int flight = Math.max(1, (int) Math.ceil(chest(target).distanceTo(from) / speed));
			straight = Math.max(1, (int) Math.round(flight * LifeweaverStats.HOMING_FROM));
			turn = Math.max(1, (int) Math.round(flight * LifeweaverStats.HOMING_FROM / 2));
		}
	}

	@Override
	public void tick() {
		LivingEntity target = target();
		if (!level().isClientSide() && (target == null || tickCount >= lifetime())) {
			discard();
			return;
		}
		double speed = Ow.speed(entityData.get(SPEED));
		Vec3 v = getDeltaMovement();
		if (target != null) {
			Vec3 to = chest(target).subtract(position());
			double dist = to.length();
			if (!level().isClientSide() && dist <= speed + hitRadius() + target.getBbWidth() / 2) {
				arrive(target);
				return;
			}
			// straight, then turning onto the ally, then homing
			double w = Math.clamp((tickCount - straight) / (double) turn, 0, 1);
			Vec3 dir = v.lengthSqr() < 1.0E-8 ? to.normalize() : v.normalize().scale(1 - w).add(to.normalize().scale(w));
			v = (dir.lengthSqr() < 1.0E-8 ? to : dir).normalize().scale(speed);
			setDeltaMovement(v);
		}
		setPos(position().add(v));
		double h = v.horizontalDistance();
		setYRot((float) (Math.atan2(v.x, v.z) * 180 / Math.PI));
		setXRot((float) (Math.atan2(v.y, h) * 180 / Math.PI));
	}

	/** It reaches its ally: the heal (over 0.3 x charge s), the Medic's share to him. */
	private void arrive(LivingEntity target) {
		if (level() instanceof ServerLevel level && getOwner() instanceof ServerPlayer owner) {
			Lifeweaver.blossomHeals(owner, target, heal, healOver, charge());
			dev.overcraft.hero.Effects.play(level, "lifeweaver_blossom_heal", chest(target), Vec3.ZERO, target, 0, owner);
		}
		discard();
	}
}
