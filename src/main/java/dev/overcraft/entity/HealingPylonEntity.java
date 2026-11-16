package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.illari.Illari;
import dev.overcraft.illari.IllariStats;
import dev.overcraft.registry.OcEntities;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Illari's Healing Pylon (thrown 4B4A, set 4BCA / 10FA, graphs 59EA / 3DF5 / 3DF6; illari-map.md): thrown at 25 m/s falling at 15,
 * it sets on the first floor or wall it meets (a ceiling: off it); built 1.25 s later (x0.25 the time with Rapid Construction), it
 * fires a heal shot every 0.8 s at the ally in 15 m and in its sight with the lowest health share, her included (50; 25 on her),
 * the shot flying at 55 m/s ({@link Illari#pylonHeal}). 75 health under 50 shields; her enemies break it, a monster beside it
 * clawing at it (ours). Not saved: it goes with its chunk.
 */
public class HealingPylonEntity extends OwProjectile implements SurfaceStuck {
	/** 0 in flight, 1 building, 2 built (synced: its clip). */
	private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(HealingPylonEntity.class, EntityDataSerializers.INT);
	/** Ticks since it set down (synced: the build clip's time). */
	private static final EntityDataAccessor<Integer> DOWN = SynchedEntityData.defineId(HealingPylonEntity.class, EntityDataSerializers.INT);
	/** The face it's set on, for its clients' look (SurfaceStuck: -1 flying). */
	private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(HealingPylonEntity.class, EntityDataSerializers.INT);
	private double health = IllariStats.PYLON_HEALTH, shields = IllariStats.PYLON_SHIELDS;
	private double buildTicks = Ow.ticks(IllariStats.PYLON_BUILD);
	private int nextShot;
	private long hitAt = -1000;
	private int mobClock;
	/** Its time down when it was built (its hum's clock). */
	private int builtAt;
	/** Heal shots in flight: who, ticks to arrival. */
	private final List<int[]> shots = new ArrayList<>();

	public HealingPylonEntity(EntityType<? extends HealingPylonEntity> type, Level level) {
		super(type, level);
	}

	public HealingPylonEntity(LivingEntity owner, Level level, double buildShare) {
		super(OcEntities.HEALING_PYLON, owner, level, new ItemStack(Items.END_ROD));
		buildTicks = Ow.ticks(IllariStats.PYLON_BUILD) * buildShare;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STAGE, 0);
		builder.define(DOWN, 0);
		builder.define(FACE, -1);
	}

	public int stage() {
		return entityData.get(STAGE);
	}

	/** Ticks since it set down. */
	@Override
	public @org.jspecify.annotations.Nullable Direction stuckFace() {
		return stage() >= 1 ? SurfaceStuck.decode(entityData.get(FACE)) : null;
	}

	public int down() {
		return entityData.get(DOWN);
	}

	public boolean built() {
		return stage() == 2;
	}

	/** Whether it was hit in the last 3 s (its longer cooldown: D 1D36). */
	public boolean recentlyHit() {
		return level().getGameTime() - hitAt < Ow.ticks(IllariStats.PYLON_HIT_WINDOW);
	}

	public double healthLeft() {
		return health + shields;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.END_ROD;
	}

	@Override
	protected int lifetime() {
		// 5 s in flight (ours); set down, until it's broken or taken back
		return stage() == 0 ? Ow.ticks(5) : Integer.MAX_VALUE;
	}

	@Override
	protected double getDefaultGravity() {
		return stage() == 0 ? Ow.accel(IllariStats.PYLON_GRAVITY) : 0;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return false;
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			if (stage() != 0) {
				commonTick();
			} else {
				super.tick();
			}
			return;
		}
		if (tickCount > 1 && !Illari.stillHis(this, getOwner())) {
			discard();
			return;
		}
		if (stage() == 0) {
			super.tick();
			return;
		}
		setDeltaMovement(Vec3.ZERO);
		if (!(getOwner() instanceof ServerPlayer owner) || owner.level() != level()) {
			return;
		}
		ServerLevel level = (ServerLevel) level();
		int down = down() + 1;
		entityData.set(DOWN, down);
		if (stage() == 1 && down >= buildTicks) {
			entityData.set(STAGE, 2);
			nextShot = 0;
			builtAt = down;
			Illari.pylonBuilt(owner, this);
		} else if (built() && (down - builtAt) % Illari.HUM_EVERY == 0) {
			Illari.pylonHum(this);
		}
		if (!recentlyHit() && shields < IllariStats.PYLON_SHIELDS) {
			shields = Math.min(IllariStats.PYLON_SHIELDS, shields + IllariStats.PYLON_SHIELD_REGEN / 20.0);
		}
		// the shots on their way land
		for (int i = shots.size() - 1; i >= 0; i--) {
			int[] shot = shots.get(i);
			if (--shot[1] <= 0) {
				shots.remove(i);
				if (level.getEntity(shot[0]) instanceof LivingEntity target && target.isAlive()) {
					Illari.pylonHealLands(owner, this, target);
				}
			}
		}
		if (built() && !dev.overcraft.sombra.Hack.hacked(this) && --nextShot <= 0) {
			LivingEntity target = Illari.pylonTarget(owner, this);
			if (target != null) {
				nextShot = Ow.ticks(IllariStats.PYLON_EVERY);
				Vec3 from = eye();
				double metres = from.distanceTo(target.getBoundingBox().getCenter());
				shots.add(new int[] {target.getId(), Math.max(1, (int) Math.round(metres / IllariStats.PYLON_SHOT_SPEED * 20))});
				Illari.pylonFired(owner, this, target);
			} else {
				nextShot = 2;
			}
		}
		clawedAt(level, owner);
	}

	/** Where its shots leave from: its top (ours: 0.6 m out of its face). */
	public Vec3 eye() {
		return position().add(0, getBbHeight() * 0.8, 0);
	}

	/**
	 * Her enemies among Minecraft's monsters beside it hit it, once a second for their attack (ours: a mob can only target a creature,
	 * so one that walks up to it where she stands claws at what's in its way).
	 */
	private void clawedAt(ServerLevel level, ServerPlayer owner) {
		if (++mobClock < Ow.ticks(IllariStats.PYLON_MOB_EVERY)) {
			return;
		}
		mobClock = 0;
		AABB reach = getBoundingBox().inflate(IllariStats.PYLON_MOB_REACH);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, reach)) {
			if (!mob.isAlive() || !Allies.isEnemy(owner, mob) || mob.getAttribute(Attributes.ATTACK_DAMAGE) == null
				|| mob.getAttributeValue(Attributes.ATTACK_DAMAGE) <= 0) {
				continue;
			}
			mob.getLookControl().setLookAt(this);
			hurtServer(level, level.damageSources().mobAttack(mob), (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
			if (isRemoved()) {
				return;
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel) || stage() != 0) {
			return;
		}
		Direction face = hit.getDirection();
		if (face == Direction.DOWN) {
			// a ceiling: it drops off (ours: W "can be placed on walls and floors")
			setDeltaMovement(Vec3.ZERO);
			setPos(hit.getLocation().add(0, -0.3, 0));
			needsSync = true;
			return;
		}
		entityData.set(STAGE, 1);
		entityData.set(DOWN, 0);
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		// on a wall it stands out of the face its own half width (ours)
		Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
		Vec3 at = face == Direction.UP ? hit.getLocation().add(0, 0.01, 0)
			: hit.getLocation().add(n.scale(getBbWidth() / 2 + 0.02)).subtract(0, getBbHeight() / 2, 0);
		setPos(at);
		setYRot((float) (face == Direction.UP ? getYRot() : Math.toDegrees(Math.atan2(-n.x, n.z))));
		entityData.set(FACE, SurfaceStuck.encode(face));
		if (getOwner() instanceof ServerPlayer owner) {
			Illari.pylonLanded(owner, this);
		}
	}

	@Override
	public void hitBarrier(Vec3 at) {
		setPos(at);
		setDeltaMovement(Vec3.ZERO);
		needsSync = true;
	}

	@Override
	public boolean isPickable() {
		return stage() != 0;
	}

	/** Her enemies' damage: the shields first, then its health (D 15A3 / 15A4); at none it's destroyed. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || stage() == 0) {
			return false;
		}
		Entity by = source.getEntity();
		if (getOwner() instanceof LivingEntity owner && (by == null || by == owner || by instanceof LivingEntity attacker && !Allies.canHurt(owner, attacker))) {
			// hers, her side's, or nobody's (a fall, fire): nothing
			return false;
		}
		double ow = Ow.toOw(amount);
		hitAt = level.getGameTime();
		double onShields = Math.min(shields, ow);
		shields -= onShields;
		health -= ow - onShields;
		if (getOwner() instanceof ServerPlayer owner) {
			Illari.pylonHit(owner, this, by);
			if (health <= 0) {
				Illari.pylonDestroyed(owner, this);
			}
		}
		if (health <= 0) {
			discard();
		}
		return true;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!level().isClientSide()) {
			Illari.pylonGone(this);
		}
		super.remove(reason);
	}

	@Override
	public boolean canBeHitByProjectile() {
		return stage() != 0;
	}
}
