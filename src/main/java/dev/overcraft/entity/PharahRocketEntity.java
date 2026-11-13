package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.pharah.Pharah;
import dev.overcraft.pharah.PharahStats;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One of Pharah's projectiles in flight (pharah-map.md): a Rocket Launcher rocket (0023: 40 m/s, 0.32 m), a Barrage rocket
 * (002B: 45 m/s, 0.075 m), a Rocket Salvo mini-rocket (7769: 40 m/s) or Concussive Blast (007B: 60 m/s, 0.225 m). None drops.
 * On its first enemy, block or barrier it bursts ({@link Pharah#burst}); allies and she don't stop it.
 */
public class PharahRocketEntity extends OwProjectile {
	public enum Kind { ROCKET, BARRAGE, SALVO, CONCUSSIVE }

	/** Synced for the client's look (the renderer: no model for the blast). */
	private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(PharahRocketEntity.class, EntityDataSerializers.INT);

	public PharahRocketEntity(EntityType<? extends PharahRocketEntity> type, Level level) {
		super(type, level);
	}

	public PharahRocketEntity(LivingEntity owner, Level level, Kind kind) {
		super(OcEntities.PHARAH_ROCKET, owner, level, new ItemStack(Items.FIREWORK_ROCKET));
		entityData.set(KIND, kind.ordinal());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(KIND, 0);
	}

	public Kind kind() {
		int k = entityData.get(KIND);
		return k >= 0 && k < Kind.values().length ? Kind.values()[k] : Kind.ROCKET;
	}

	@Override
	public float hitRadius() {
		return (float) switch (kind()) {
			case ROCKET, SALVO -> PharahStats.ROCKET_RADIUS;
			case BARRAGE -> PharahStats.BARRAGE_SIZE;
			case CONCUSSIVE -> PharahStats.CONCUSSIVE_RADIUS;
		};
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIREWORK_ROCKET;
	}

	@Override
	protected int lifetime() {
		// each lives 5 s (D 0023 / 002B / 007B), the mini-rockets 10 s (D 7769)
		return Ow.ticks(kind() == Kind.SALVO ? 10 : PharahStats.ROCKET_LIFETIME);
	}

	@Override
	public double barrierDamage() {
		return switch (kind()) {
			case ROCKET -> PharahStats.ROCKET_DIRECT + PharahStats.ROCKET_SPLASH;
			case BARRAGE -> PharahStats.BARRAGE_DIRECT + PharahStats.BARRAGE_SPLASH;
			case SALVO -> PharahStats.SALVO_DIRECT + PharahStats.SALVO_SPLASH;
			case CONCUSSIVE -> PharahStats.CONCUSSIVE_DIRECT;
		};
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof PharahRocketEntity)
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		// it bursts where it touches: the point of the creature's box nearest its centre (D.Va's missiles: the centre is out
		// from the box by the radius, and a burst measured from there lost splash on a direct hit)
		Vec3 at = hit.getLocation();
		net.minecraft.world.phys.AABB box = hit.getEntity().getBoundingBox();
		burst(new Vec3(Math.clamp(at.x, box.minX, box.maxX), Math.clamp(at.y, box.minY, box.maxY), Math.clamp(at.z, box.minZ, box.maxZ)),
			hit.getEntity());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		Direction face = hit.getDirection();
		burst(hit.getLocation().add(face.getStepX() * 0.05, face.getStepY() * 0.05, face.getStepZ() * 0.05), null);
	}

	@Override
	public void hitBarrier(Vec3 at) {
		burst(at, null);
	}

	private void burst(Vec3 at, Entity direct) {
		if (!(level() instanceof ServerLevel) || !isAlive()) {
			return;
		}
		if (getOwner() instanceof ServerPlayer owner) {
			Pharah.burst(owner, this, at, direct);
		}
		discard();
	}
}
