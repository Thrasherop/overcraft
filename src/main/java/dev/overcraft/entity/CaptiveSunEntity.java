package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.illari.Illari;
import dev.overcraft.illari.IllariStats;
import dev.overcraft.registry.OcEntities;
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
 * Captive Sun's ball (entity 4AC0, model 10BAE, graph 3CB1; illari-map.md): 30 m/s in a straight line, 0.45 m, 10 s. It bursts on
 * the first enemy or block it meets, or at an enemy barrier: 5 to an enemy it hit, then 50 to every enemy in 10 m and in sight, each
 * of them marked ({@link Illari#ballBurst}).
 */
public class CaptiveSunEntity extends OwProjectile {
	private boolean done;

	public CaptiveSunEntity(EntityType<? extends CaptiveSunEntity> type, Level level) {
		super(type, level);
	}

	public CaptiveSunEntity(LivingEntity owner, Level level) {
		super(OcEntities.CAPTIVE_SUN, owner, level, new ItemStack(Items.FIRE_CHARGE));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIRE_CHARGE;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(IllariStats.BALL_LIFE);
	}

	@Override
	public float hitRadius() {
		return (float) IllariStats.BALL_SIZE / 2;
	}

	@Override
	public double barrierDamage() {
		return IllariStats.BALL_DIRECT + IllariStats.BALL_SPLASH;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof OwProjectile)
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level && isAlive()) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.15, 0.15, 0.15, 0.01);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		burst(hit.getLocation(), hit.getEntity());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		Vec3 n = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
		burst(hit.getLocation().add(n.scale(0.2)), null);
	}

	@Override
	public void hitBarrier(Vec3 at) {
		burst(at, null);
	}

	@Override
	protected void expire() {
		burst(position(), null);
	}

	private void burst(Vec3 at, Entity direct) {
		if (done || !(level() instanceof ServerLevel)) {
			return;
		}
		done = true;
		if (getOwner() instanceof ServerPlayer owner) {
			Illari.ballBurst(owner, this, at, direct);
		}
		discard();
	}
}
