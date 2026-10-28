package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.mercy.MercyStats;
import dev.overcraft.registry.OcEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One shot of Mercy's Caduceus Blaster (her graph 006D: projectile entity 0158): 20 damage, double on the head, 50 m/s
 * (100 in Valkyrie), gone after 5 s. Enemies only: allies don't stop it. No falloff, no spread.
 */
public class BlasterShotEntity extends OwProjectile {
	/** How far below the eyes a hit still counts as the head (blocks): the kunai's. */
	private static final double HEAD_DEPTH = 0.25;
	/** The shot's life (s): her data's 5 s lifetime. */
	private static final double LIFETIME = 5;

	public BlasterShotEntity(EntityType<? extends BlasterShotEntity> type, Level level) {
		super(type, level);
	}

	public BlasterShotEntity(LivingEntity owner, Level level) {
		super(OcEntities.BLASTER_SHOT, owner, level, new ItemStack(Items.SNOWBALL));
	}

	@Override
	public float hitRadius() {
		return (float) MercyStats.BLASTER_RADIUS;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SNOWBALL;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(LIFETIME);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && Allies.isAlly(owner, living)
			&& !Allies.canHurt(owner, living));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof LivingEntity owner)) {
			discard();
			return;
		}
		EnderDragonPart part = hit.getEntity() instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : hit.getEntity() instanceof LivingEntity living ? living : null;
		Vec3 at = hit.getLocation();
		if (target != null && Allies.canHurt(owner, target)) {
			boolean crit = part != null ? part == part.parentMob.head : at.y >= target.getEyeY() - HEAD_DEPTH;
			double damage = MercyStats.BLASTER_DAMAGE * (crit ? MercyStats.BLASTER_CRIT : 1);
			float dealt = part != null ? Combat.damagePart(level, part, damageSources().thrown(this, owner), damage)
				: Combat.damage(target, damageSources().thrown(this, owner), damage);
			if (owner instanceof Player mercy) {
				Combat.credit(mercy, dealt);
				Combat.addFireForDamage(mercy, dealt, Ow.toOw(target.getMaxHealth()));
				if (mercy instanceof ServerPlayer shooter && dealt > 0) {
					Effects.hitMarker(shooter, target, crit);
				}
			}
			Effects.play(level, "blaster_hit", at, getDeltaMovement(), null, 0, owner);
		} else if (target == null) {
			// not a creature (an end crystal, a boat): hit as an arrow does
			hit.getEntity().hurtServer(level, damageSources().thrown(this, owner), (float) Ow.hp(MercyStats.BLASTER_DAMAGE));
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
			Effects.play(level, "blaster_hit_block", hit.getLocation(), normal, null, 0, getOwner());
			discard();
		}
	}
}
