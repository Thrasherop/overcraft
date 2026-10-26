package dev.overcraft.entity;

import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroSounds;
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
		return super.canHitEntity(entity) && getOwner() instanceof LivingEntity owner && Allies.canHurt(owner, entity);
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
		Effects.play(level, "kunai_hit_entity", at, getOwner());
		level.playSound(null, at.x, at.y, at.z, OcSounds.KIRIKO_KUNAI_HIT, SoundSource.PLAYERS, 1, 1);
		if (getOwner() instanceof Player owner) {
			Combat.addUltCharge(owner, dealt);
			if (owner instanceof net.minecraft.server.level.ServerPlayer thrower && dealt > 0) {
				Effects.hitMarker(thrower, target, crit);
			}
			if (crit) {
				// the critical hit's confirmation is the thrower's (it plays where they are)
				if (owner instanceof net.minecraft.server.level.ServerPlayer thrower) {
					HeroSounds.toOwner(thrower, OcSounds.KIRIKO_KUNAI_HEADSHOT, thrower.getEyePosition());
				}
			}
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			Vec3 at = hit.getLocation();
			Effects.play(level, "kunai_hit_block", at, Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()), null, 0, getOwner());
			level.playSound(null, at.x, at.y, at.z, OcSounds.KIRIKO_KUNAI_HIT_BLOCK, SoundSource.PLAYERS, 1, 1);
			discard();
		}
	}
}
