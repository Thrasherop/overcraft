package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.junkrat.Junkrat;
import dev.overcraft.junkrat.JunkratStats;
import dev.overcraft.registry.OcEntities;
import net.minecraft.core.Direction;
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
import org.jspecify.annotations.Nullable;

/**
 * Junkrat's frag (entity 0385, his Frag Launcher's volley 022B #136; junkrat-map.md) and his Total Mayhem bombs (entity 0515, the same
 * model). A frag flies at 30 m/s (Frag Cannon's 37.5) falling at 17 m/s², bounces off blocks keeping half its speed into the face and
 * 0.75 along it, and goes off on the first enemy it touches, on a ground bounce after an earlier bounce, or 4.5 s after the shot
 * ({@link Junkrat#fragBurst}; the rule is W's: junkrat-map.md Open). A bomb falls at 9.8, bounces the same, and goes off at its own
 * time. Allies and he don't stop either.
 */
public class FragEntity extends OwProjectile {
	private boolean bomb;
	/** Its bounces so far, and its fuse (ticks from the shot). */
	private int bounces;
	private int fuse = Ow.ticks(JunkratStats.FRAG_FUSE);
	private boolean done;

	public FragEntity(EntityType<? extends FragEntity> type, Level level) {
		super(type, level);
	}

	public FragEntity(LivingEntity owner, Level level, boolean bomb, int fuseTicks) {
		super(OcEntities.FRAG, owner, level, new ItemStack(Items.FIRE_CHARGE));
		this.bomb = bomb;
		this.fuse = fuseTicks;
	}

	public boolean bomb() {
		return bomb;
	}

	public int bounces() {
		return bounces;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIRE_CHARGE;
	}

	@Override
	protected int lifetime() {
		return fuse;
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(bomb ? JunkratStats.MAYHEM_GRAVITY : JunkratStats.FRAG_GRAVITY);
	}

	/** 0.1 m on a creature, halved from its second bounce (D 28EE, v59304 [1, 0.5]: M); a bomb 0.15 m (D 28F1). */
	@Override
	public float hitRadius() {
		if (bomb) {
			return 0.15f;
		}
		return (float) (JunkratStats.FRAG_HIT_RADIUS * (bounces >= 2 ? 0.5 : 1));
	}

	@Override
	public double barrierDamage() {
		return bomb ? 0 : JunkratStats.FRAG_DIRECT;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return !bomb && super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof FragEntity)
			&& !(entity instanceof ConcussionMineEntity) && !(entity instanceof SteelTrapEntity) && !(entity instanceof RipTireEntity)
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (level() instanceof ServerLevel) {
			burst(hit.getLocation(), hit.getEntity());
		}
	}

	/** A block: it bounces (or goes off, by {@link Junkrat#bounceBursts}). */
	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel) || done) {
			return;
		}
		Direction face = hit.getDirection();
		Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
		boolean ground = n.y >= JunkratStats.GROUND_NORMAL;
		if (!bomb && Junkrat.bounceBursts(bounces, ground)) {
			burst(hit.getLocation().add(n.scale(0.1)), null);
			return;
		}
		bounces++;
		setDeltaMovement(Junkrat.bounce(getDeltaMovement(), n));
		setPos(hit.getLocation().add(n.scale(0.05)));
		needsSync = true;
		if (getOwner() instanceof ServerPlayer owner) {
			Junkrat.fragBounced(owner, this, hit.getLocation());
		}
	}

	@Override
	public void hitBarrier(Vec3 at) {
		burst(at, null);
	}

	/**
	 * Total Mayhem's bombs fly through enemy barriers ("can bounce through barriers, but barriers block their explosions": the
	 * wiki's Barrier page, rev 406358; the Junkrat page rev 410907, "The grenades can penetrate barriers, but their explosions
	 * cannot"); their burst still stops at one (Junkrat.fragBurst). A Frag Launcher grenade stops and bursts there.
	 */
	@Override
	public boolean passesBarriers() {
		return bomb;
	}

	@Override
	protected void expire() {
		burst(position(), null);
	}

	private void burst(Vec3 at, @Nullable Entity direct) {
		if (done) {
			return;
		}
		done = true;
		if (getOwner() instanceof ServerPlayer owner && owner.level() == level()) {
			Junkrat.fragBurst(owner, this, at, direct);
		}
		discard();
	}
}
