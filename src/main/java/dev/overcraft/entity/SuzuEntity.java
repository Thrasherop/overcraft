package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.kiriko.KirikoStats;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcItems;
import dev.overcraft.registry.OcSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Protection Suzu: arcs, passes through everyone, bursts on terrain. Kiriko and allies in the burst (unless fully
 * behind a wall) are healed, cleansed and phased out (immune to damage) for a moment.
 */
public class SuzuEntity extends OwProjectile {

	public SuzuEntity(EntityType<? extends SuzuEntity> type, Level level) {
		super(type, level);
	}

	public SuzuEntity(LivingEntity owner, Level level) {
		super(OcEntities.SUZU, owner, level, new ItemStack(OcItems.SUZU));
	}

	@Override
	protected Item getDefaultItem() {
		return OcItems.SUZU;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(10);
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(KirikoStats.SUZU_GRAVITY);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return false;
	}

	@Override
	protected void expire() {
		burst(position());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!level().isClientSide()) {
			burst(hit.getLocation());
		}
	}

	private void burst(Vec3 at) {
		if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof LivingEntity owner)) {
			discard();
			return;
		}
		double r = KirikoStats.SUZU_RADIUS;
		float healedTotal = 0;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r))) {
			if (!Allies.isSelfOrAlly(owner, entity) || !touches(entity, at, r) || !inSight(level, at, entity)) {
				continue;
			}
			healedTotal += Combat.heal(entity, KirikoStats.SUZU_HEAL);
			Combat.cleanse(entity);
			Combat.phase(entity, KirikoStats.SUZU_PHASED);
			// they glow white for a moment, seen by everyone near
			Effects.play(level, "suzu_cleansed", entity.position(), Vec3.ZERO, entity, 0, getOwner());
		}
		if (owner instanceof Player player) {
			Combat.addUltCharge(player, healedTotal);
		}
		Effects.play(level, "suzu_burst", at, Vec3.ZERO, null, (int) Math.round(r * 10), getOwner());
		level.playSound(null, at.x, at.y, at.z, OcSounds.KIRIKO_SUZU_BURST, SoundSource.PLAYERS, 1, 1);
		discard();
	}

	/** The burst sphere reaches the entity's box. */
	private static boolean touches(LivingEntity entity, Vec3 at, double r) {
		AABB box = entity.getBoundingBox();
		double dx = Math.max(box.minX - at.x, Math.max(0, at.x - box.maxX));
		double dy = Math.max(box.minY - at.y, Math.max(0, at.y - box.maxY));
		double dz = Math.max(box.minZ - at.z, Math.max(0, at.z - box.maxZ));
		return dx * dx + dy * dy + dz * dz <= r * r;
	}

	/** Some part of the entity (feet, middle or head) can be seen from the burst. */
	private boolean inSight(ServerLevel level, Vec3 at, LivingEntity entity) {
		Vec3 from = at.add(0, 0.1, 0);
		for (double y : new double[] {entity.getY() + 0.1, entity.getY() + entity.getBbHeight() / 2, entity.getEyeY()}) {
			Vec3 to = new Vec3(entity.getX(), y, entity.getZ());
			if (level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}
}
