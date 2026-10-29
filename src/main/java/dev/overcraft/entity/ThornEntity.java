package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.lifeweaver.Lifeweaver;
import dev.overcraft.lifeweaver.LifeweaverStats;
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
 * One thorn of Lifeweaver's Thorn Volley (his graph 21C7, entity 3642): 6 damage, double on the head, 100 m/s, straight,
 * gone after 5 s. Enemies only: allies don't stop it. No falloff.
 */
public class ThornEntity extends OwProjectile {
	/** How far below the eyes a hit still counts as the head (blocks): the kunai's. */
	private static final double HEAD_DEPTH = 0.25;

	public ThornEntity(EntityType<? extends ThornEntity> type, Level level) {
		super(type, level);
	}

	public ThornEntity(LivingEntity owner, Level level) {
		super(OcEntities.THORN, owner, level, new ItemStack(Items.STICK));
	}

	@Override
	public float hitRadius() {
		return (float) LifeweaverStats.THORN_RADIUS;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.STICK;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(LifeweaverStats.THORN_LIFETIME);
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
			double damage = LifeweaverStats.THORN_DAMAGE * (crit ? LifeweaverStats.THORN_CRIT : 1);
			float dealt = part != null ? Combat.damagePart(level, part, damageSources().thrown(this, owner), damage)
				: Combat.damage(target, damageSources().thrown(this, owner), damage);
			if (owner instanceof Player lifeweaver) {
				Combat.credit(lifeweaver, dealt);
				Combat.addFireForDamage(lifeweaver, dealt, Ow.toOw(target.getMaxHealth()));
				if (lifeweaver instanceof ServerPlayer shooter) {
					if (dealt > 0) {
						Effects.hitMarker(shooter, target, crit);
					}
					// Superbloom: enough thorns in one enemy burst
					Lifeweaver.thornHit(shooter, target);
				}
			}
			Effects.play(level, "lifeweaver_thorn_hit", at, getDeltaMovement(), null, 0, owner);
		} else if (target == null) {
			// not a creature (an end crystal, a boat): hit as an arrow does
			hit.getEntity().hurtServer(level, damageSources().thrown(this, owner), (float) Ow.hp(LifeweaverStats.THORN_DAMAGE));
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
			Effects.play(level, "lifeweaver_thorn_hit_block", hit.getLocation(), normal, null, 0, getOwner());
			discard();
		}
	}
}
