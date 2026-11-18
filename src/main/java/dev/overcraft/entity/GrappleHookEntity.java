package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.widowmaker.Widowmaker;
import dev.overcraft.widowmaker.WidowmakerStats;
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
 * Widowmaker's grappling claw (entity 00D3, model 0711; 005A #37): flies at 40 m/s with no fall, up to 20 m from where it left. A block
 * holds it (her kit pulls her to it: {@link Widowmaker#hookHeld}); a creature or the end of its reach is a miss, and it reels back to
 * her at 120 m/s (005A #288 / #22), then goes. Her clients draw the rope from her to it (WidowmakerFx). Not saved.
 */
public class GrappleHookEntity extends OwProjectile {
	/** 0 flying, 1 held by a block, 2 reeling back. */
	private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.INT);
	public static final int FLYING = 0, HELD = 1, REELING = 2;
	private Vec3 start = Vec3.ZERO;
	/** From the point it holds into the block it holds (a little behind the face it hit). */
	private Vec3 into = new Vec3(0, -0.05, 0);

	public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, Level level) {
		super(type, level);
	}

	public GrappleHookEntity(LivingEntity owner, Level level) {
		super(OcEntities.GRAPPLE_HOOK, owner, level, new ItemStack(Items.TRIPWIRE_HOOK));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STATE, FLYING);
	}

	public int state() {
		return entityData.get(STATE);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.TRIPWIRE_HOOK;
	}

	@Override
	public void launch(Vec3 from, Vec3 direction, double speed) {
		super.launch(from, direction, speed);
		start = from;
	}

	/** Its flight is its reach's (20 m at 40 m/s: 0.5 s) and some; held, its owner's kit ends it. */
	@Override
	protected int lifetime() {
		return Ow.ticks(WidowmakerStats.HOOK_RANGE / WidowmakerStats.HOOK_SPEED) + 2;
	}

	@Override
	protected void expire() {
		reel();
	}

	@Override
	protected double getDefaultGravity() {
		return 0;
	}

	@Override
	public float hitRadius() {
		return (float) WidowmakerStats.HOOK_RADIUS;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return state() == FLYING && super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof OwProjectile);
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			if (state() == FLYING) {
				super.tick();
			} else {
				commonTick();
				if (state() == REELING && getOwner() != null) {
					reelStep();
				}
			}
			return;
		}
		Entity owner = getOwner();
		if (!(owner instanceof ServerPlayer player) || owner.level() != level() || !Widowmaker.hookIsHers(player, this)) {
			discard();
			return;
		}
		switch (state()) {
			case HELD -> {
				setDeltaMovement(Vec3.ZERO);
				net.minecraft.core.BlockPos on = net.minecraft.core.BlockPos.containing(position().add(into));
				if (level().getBlockState(on).getCollisionShape(level(), on).isEmpty()) {
					// the block it holds went (mined, burnt): it lets go and the pull ends there, with its cooldown (ours; she was
					// pulled on to the empty air: critique 2026-11-17)
					discard();
				}
			}
			case REELING -> {
				commonTick();
				if (reelStep()) {
					discard();
				}
			}
			default -> {
				if (start != Vec3.ZERO && position().distanceTo(start) > WidowmakerStats.HOOK_RANGE) {
					reel();
					return;
				}
				super.tick();
			}
		}
	}

	/** One tick of reeling back to its owner's hand at 120 m/s; whether it's back. */
	private boolean reelStep() {
		Entity owner = getOwner();
		if (owner == null) {
			return true;
		}
		Vec3 hand = owner.getEyePosition().add(0, -0.4, 0);
		Vec3 to = hand.subtract(position());
		double step = Ow.speed(WidowmakerStats.HOOK_REEL_SPEED);
		if (to.length() <= step) {
			setPos(hand);
			return true;
		}
		setPos(position().add(to.normalize().scale(step)));
		return false;
	}

	/** A miss (a creature, its reach, an abort): it reels back, no cooldown (D 005A #288 / #162). */
	public void reel() {
		if (state() == REELING) {
			return;
		}
		entityData.set(STATE, REELING);
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		if (getOwner() instanceof ServerPlayer owner) {
			Widowmaker.hookMissed(owner, this);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		reel();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		if (!(level() instanceof ServerLevel) || state() != FLYING) {
			return;
		}
		entityData.set(STATE, HELD);
		into = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ()).scale(-0.05);
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		setPos(hit.getLocation());
		needsSync = true;
		if (getOwner() instanceof ServerPlayer owner) {
			Widowmaker.hookHeld(owner, this, hit);
		}
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
