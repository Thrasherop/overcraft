package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.sojourn.Railgun;
import dev.overcraft.sojourn.SojournStats;
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
 * One round of Sojourn's Railgun (her weapon graph 1A23, volley #502: projectile entity 2F98): 9 damage, x2 on the head, 150
 * m/s, 0.175 m, no drop, no falloff, gone after 10 s. Swept along its step as Kiriko's kunai (Minecraft's projectile hit test,
 * ProjectileUtilMixin's own radius). Allies don't stop it. What it does on a hit is the Railgun's (energy): {@link Railgun#hit}.
 */
public class RailgunShotEntity extends OwProjectile {
	public RailgunShotEntity(EntityType<? extends RailgunShotEntity> type, Level level) {
		super(type, level);
	}

	public RailgunShotEntity(LivingEntity owner, Level level) {
		super(OcEntities.RAILGUN_SHOT, owner, level, new ItemStack(Items.SNOWBALL));
	}

	@Override
	public float hitRadius() {
		return (float) SojournStats.RAILGUN_RADIUS;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SNOWBALL;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(SojournStats.RAILGUN_LIFETIME);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && Allies.isAlly(owner, living)
			&& !Allies.canHurt(owner, living));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (level() instanceof ServerLevel && getOwner() instanceof ServerPlayer owner) {
			Railgun.hit(owner, this, hit.getEntity(), hit.getLocation());
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel) {
			if (getOwner() instanceof ServerPlayer owner) {
				net.minecraft.core.Direction face = hit.getDirection();
				Railgun.hitBlock(owner, hit.getLocation(),
					new net.minecraft.world.phys.Vec3(face.getStepX(), face.getStepY(), face.getStepZ()));
			}
			discard();
		}
	}
}
