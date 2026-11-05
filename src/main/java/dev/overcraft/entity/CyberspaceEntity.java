package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.sombra.Cyberspace;
import dev.overcraft.sombra.SombraStats;
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
 * Sombra's Cyberspace (projectile 7763, graph 6102): straight at 50 m/s, no gravity (M), 0.3 m; on the first enemy or block
 * it opens its zone there (Cyberspace). No model of its own: the data draws it as an effect (step 8).
 */
public class CyberspaceEntity extends OwProjectile {
	public CyberspaceEntity(EntityType<? extends CyberspaceEntity> type, Level level) {
		super(type, level);
	}

	public CyberspaceEntity(LivingEntity owner, Level level) {
		super(OcEntities.CYBERSPACE, owner, level, new ItemStack(Items.AMETHYST_SHARD));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.AMETHYST_SHARD;
	}

	/** Ours: far enough for its 50 m/s to cross any fight (the data's lifetime isn't read). */
	@Override
	protected int lifetime() {
		return Ow.ticks(2);
	}

	@Override
	public float hitRadius() {
		return (float) SombraStats.CYBER_RADIUS;
	}

	/** It flies through her side and stops on an enemy. */
	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && Allies.isSelfOrAlly(owner, entity));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		open();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		// a hair off the face it hit, so the zone's centre isn't inside the block
		setPos(hit.getLocation().add(net.minecraft.world.phys.Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(0.1)));
		open();
	}

	/** At its life's end in the air it opens where it is (M). */
	@Override
	protected void expire() {
		open();
	}

	private void open() {
		if (level() instanceof ServerLevel level && getOwner() instanceof ServerPlayer owner && isAlive()) {
			Cyberspace.open(level, owner, position());
		}
		discard();
	}
}
