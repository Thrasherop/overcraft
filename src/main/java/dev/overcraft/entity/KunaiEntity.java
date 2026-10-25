package dev.overcraft.entity;

import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.kiriko.KirikoStats;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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

/** Kiriko's kunai: fast, no drop, no falloff, 2x on a headshot. Passes through allies. */
public class KunaiEntity extends OwProjectile {
	/** How far below the eyes a hit still counts as the head (blocks). */
	private static final double HEAD_DEPTH = 0.25;

	public KunaiEntity(EntityType<? extends KunaiEntity> type, Level level) {
		super(type, level);
	}

	public KunaiEntity(LivingEntity owner, Level level) {
		super(OcEntities.KUNAI, owner, level, new ItemStack(OcItems.KUNAI));
	}

	@Override
	protected Item getDefaultItem() {
		return OcItems.KUNAI;
	}

	@Override
	protected int lifetime() {
		return 40;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && getOwner() instanceof LivingEntity owner && Allies.isEnemy(owner, entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (!(level() instanceof ServerLevel level) || !(hit.getEntity() instanceof LivingEntity target)) {
			return;
		}
		Vec3 at = hit.getLocation();
		boolean crit = at.y >= target.getEyeY() - HEAD_DEPTH;
		double damage = KirikoStats.KUNAI_DAMAGE * (crit ? KirikoStats.KUNAI_CRIT : 1);
		float dealt = Combat.damage(target, damageSources().thrown(this, getOwner()), damage);
		if (getOwner() instanceof Player owner) {
			Combat.addUltCharge(owner, dealt);
			if (crit) {
				level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.6f, 1.6f);
				level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.2);
			}
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			Vec3 at = hit.getLocation();
			level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.05);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 0.5f, 1.8f);
			discard();
		}
	}
}
