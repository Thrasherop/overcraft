package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.ana.Ana;
import dev.overcraft.ana.AnaStats;
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

/**
 * Ana's Biotic Grenade (her graph 0B97, the burst graphs 0B99 / 0C72): 30 m/s, falling at 9.8 m/s², a 0.275 m contact
 * radius, gone after 60 s. It bursts on its first block or creature; allies at full health don't stop it, as her darts
 * (M: the hit filter). The burst is Ana.grenadeBurst.
 */
public class BioticGrenadeEntity extends OwProjectile {
	public BioticGrenadeEntity(EntityType<? extends BioticGrenadeEntity> type, Level level) {
		super(type, level);
	}

	public BioticGrenadeEntity(LivingEntity owner, Level level) {
		super(OcEntities.BIOTIC_GRENADE, owner, level, new ItemStack(Items.SPLASH_POTION));
	}

	@Override
	public float hitRadius() {
		return (float) AnaStats.GRENADE_RADIUS_CONTACT;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SPLASH_POTION;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(AnaStats.GRENADE_LIFETIME);
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(AnaStats.GRENADE_GRAVITY);
	}

	/** Not her, and not an ally at full health (it passes them). */
	@Override
	protected boolean canHitEntity(Entity entity) {
		if (!super.canHitEntity(entity) || entity == getOwner()) {
			return false;
		}
		return !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && Allies.isAlly(owner, living)
			&& living.getHealth() >= living.getMaxHealth());
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		burst();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		burst();
	}

	private void burst() {
		if (level() instanceof ServerLevel level && isAlive()) {
			if (getOwner() instanceof ServerPlayer owner) {
				Ana.grenadeBurst(level, owner, position(), this);
			}
			discard();
		}
	}
}
