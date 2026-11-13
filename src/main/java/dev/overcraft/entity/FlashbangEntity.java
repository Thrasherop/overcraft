package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.cassidy.Cassidy;
import dev.overcraft.cassidy.CassidyStats;
import dev.overcraft.hero.Allies;
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
 * Cassidy's Flashbang (entity 037D, graph 01E9; cassidy-map.md): thrown at 30 m/s (Bang Bang's second at 25), falling at 20 m/s²,
 * 0.2 m. It goes off at once on the first enemy or block it meets, or once it has gone its range (7 m; Bang Bang's 10.25)
 * ({@link Cassidy#flashBang}). Allies and he don't stop it. 5 s at most (D).
 */
public class FlashbangEntity extends OwProjectile {
	private double range = CassidyStats.FLASH_RANGE;
	/** Its damage's share (Bang Bang's x0.6). */
	private double share = 1;
	private Vec3 start = Vec3.ZERO;
	private boolean done;

	public FlashbangEntity(EntityType<? extends FlashbangEntity> type, Level level) {
		super(type, level);
	}

	public FlashbangEntity(LivingEntity owner, Level level, double range, double share) {
		super(OcEntities.FLASHBANG, owner, level, new ItemStack(Items.FIREWORK_STAR));
		this.range = range;
		this.share = share;
	}

	@Override
	public void launch(Vec3 from, Vec3 direction, double speed) {
		super.launch(from, direction, speed);
		start = from;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIREWORK_STAR;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(CassidyStats.FLASH_LIFETIME);
	}

	@Override
	protected double getDefaultGravity() {
		// 20 m/s² (D 01E9, M)
		return Ow.accel(CassidyStats.FLASH_GRAVITY);
	}

	@Override
	public float hitRadius() {
		return (float) CassidyStats.FLASH_RADIUS;
	}

	@Override
	public double barrierDamage() {
		return 0;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof FlashbangEntity)
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	@Override
	public void tick() {
		boolean last = false;
		if (!level().isClientSide() && !done && isAlive() && !isInWater()) {
			// its range checked along the move, not after it (critic b79a4d1 #6: at 1.5 m a tick it went off at ~7.5 m): the leg that
			// would cross it is cut to end there, and what it meets on the way still stops it first
			Vec3 now = position();
			Vec3 next = now.add(getDeltaMovement()).subtract(0, getGravity(), 0);
			double t = Cassidy.rangeCrossing(start, now, next, range);
			if (t >= 0 && t < 1) {
				Vec3 to = now.add(next.subtract(now).scale(t));
				setDeltaMovement(to.subtract(now).add(0, getGravity(), 0));
				last = true;
			}
		}
		super.tick();
		if (!level().isClientSide() && !done && isAlive() && (last || position().distanceTo(start) >= range - 1.0E-4)) {
			// its range gone: it goes off where it is (D 01E9 m_FEC435C6)
			bang(position());
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (level() instanceof ServerLevel) {
			bang(hit.getLocation());
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel) {
			Vec3 n = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
			bang(hit.getLocation().add(n.scale(0.1)));
		}
	}

	@Override
	public void hitBarrier(Vec3 at) {
		bang(at);
	}

	private void bang(Vec3 at) {
		if (done) {
			return;
		}
		done = true;
		if (getOwner() instanceof ServerPlayer owner) {
			Cassidy.flashBang(owner, this, at, share);
		}
		discard();
	}

	@Override
	protected void expire() {
		bang(position());
	}
}
