package dev.overcraft.entity;

import net.minecraft.server.level.ServerPlayer;
import dev.overcraft.hero.Voice;
import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.Perk;
import dev.overcraft.kiriko.KirikoStats;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcItems;
import dev.overcraft.registry.OcSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Healing Ofuda. Locked on an ally (yellow) it homes on them; unlocked (blue) it flies straight and heals any ally it
 * touches. Passes through enemies, breaks on terrain.
 */
public class OfudaEntity extends OwProjectile {
	private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(OfudaEntity.class, EntityDataSerializers.INT);

	public OfudaEntity(EntityType<? extends OfudaEntity> type, Level level) {
		super(type, level);
	}

	public OfudaEntity(LivingEntity owner, Level level, @Nullable LivingEntity target) {
		super(OcEntities.OFUDA, owner, level, new ItemStack(OcItems.OFUDA));
		entityData.set(TARGET, target == null ? -1 : target.getId());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TARGET, -1);
	}

	@Override
	protected Item getDefaultItem() {
		return OcItems.OFUDA;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(KirikoStats.OFUDA_LIFETIME);
	}

	/** Locked on an ally and homing: drawn gold rather than teal. */
	public boolean homing() {
		return target() != null;
	}

	private @Nullable LivingEntity target() {
		int id = entityData.get(TARGET);
		return id >= 0 && level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
	}

	@Override
	public void tick() {
		LivingEntity target = target();
		if (target != null) {
			Vec3 to = target.getBoundingBox().getCenter().subtract(position());
			if (to.lengthSqr() > 1.0E-4) {
				// Urgent Care (perk): faster toward an ally below half health
				boolean urgent = getOwner() instanceof Player owner && HeroManager.hasPerk(owner, Perk.KIRIKO_URGENT_CARE)
					&& target.getHealth() < target.getMaxHealth() * KirikoStats.URGENT_CARE_BELOW;
				double speed = KirikoStats.OFUDA_SPEED * (urgent ? 1 + KirikoStats.URGENT_CARE_SPEED : 1);
				setDeltaMovement(to.normalize().scale(Ow.speed(speed)));
			}
		}
		super.tick();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && getOwner() instanceof LivingEntity owner && Allies.isAlly(owner, entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (!(level() instanceof ServerLevel level) || !(hit.getEntity() instanceof LivingEntity ally)) {
			return;
		}
		Effects.play(level, "ofuda_hit_ally", ally.getBoundingBox().getCenter(), Vec3.ZERO, ally, homing() ? 1 : 0, getOwner());
		level.playSound(null, ally.getX(), ally.getY(0.5), ally.getZ(), OcSounds.KIRIKO_OFUDA_HEAL, SoundSource.PLAYERS, 1, 1);
		float healed = Combat.heal(ally, KirikoStats.OFUDA_HEAL);
		if (getOwner() instanceof Player owner) {
			Combat.credit(owner, healed);
			Combat.addFireForHealing(owner, healed);
			// Medic: weapon healing also heals Kiriko (no ult charge for that part)
			if (healed > 0) {
				Combat.heal(owner, healed * KirikoStats.MEDIC_SELF_HEAL);
				// now and then a healing line ("Be healed", "Like a gentle rain": the sound research's 0.15 chance; the
				// 20 s between them is ours)
				if (owner instanceof ServerPlayer kiriko) {
					Voice.say(kiriko, "heal", 1, Voice.heroOf(ally));
				}
			}
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			Effects.play(level, "ofuda_hit_block", hit.getLocation(), Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()), null,
				homing() ? 1 : 0, getOwner());
			Vec3 at = hit.getLocation();
			level.playSound(null, at.x, at.y, at.z, OcSounds.KIRIKO_OFUDA_HIT_BLOCK, SoundSource.PLAYERS, 1, 1);
			discard();
		}
	}
}
