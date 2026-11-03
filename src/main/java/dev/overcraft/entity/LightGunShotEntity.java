package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.dva.DvaStats;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.registry.OcEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One round of the pilot's Light Gun (02B8 #94: projectile 1000): 14, double on the head, 50 m/s, a 0.275 m hit, gone after
 * 5 s. No falloff, no spread. Allies don't stop it. Stand-in look (her effects 1000 / FEC: step 11).
 */
public class LightGunShotEntity extends OwProjectile {
	/** How far below the eyes a hit still counts as the head (the kunai's, Mercy's blaster's). */
	private static final double HEAD_DEPTH = 0.25;

	public LightGunShotEntity(EntityType<? extends LightGunShotEntity> type, Level level) {
		super(type, level);
	}

	public LightGunShotEntity(LivingEntity owner, Level level) {
		super(OcEntities.LIGHT_GUN_SHOT, owner, level, new ItemStack(Items.SNOWBALL));
	}

	@Override
	public float hitRadius() {
		return (float) DvaStats.LIGHT_GUN_RADIUS;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SNOWBALL;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(DvaStats.LIGHT_GUN_LIFE);
	}

	@Override
	public double barrierDamage() {
		return DvaStats.LIGHT_GUN_DAMAGE;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
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
			double damage = DvaStats.LIGHT_GUN_DAMAGE * (crit ? DvaStats.LIGHT_GUN_CRIT : 1);
			float dealt = part != null ? Combat.damagePart(level, part, damageSources().thrown(this, owner), damage)
				: Combat.damage(target, damageSources().thrown(this, owner), damage);
			if (owner instanceof ServerPlayer dva) {
				// her Call Mech charges from it (W: damage as the pilot)
				Combat.credit(dva, dealt, target);
				Combat.addFireForDamage(dva, dealt, Ow.toOw(target.getMaxHealth()));
				if (dealt > 0) {
					if (crit) {
						var s = dev.overcraft.hero.HeroManager.state(dva);
						if (s != null) {
							s.critAt = level.getGameTime();
						}
					}
					Effects.hitMarker(dva, target, crit);
				}
			}
		} else if (target == null) {
			// not a creature (an end crystal, a boat): hit as an arrow does
			hit.getEntity().hurtServer(level, damageSources().thrown(this, owner), Ow.hp(DvaStats.LIGHT_GUN_DAMAGE));
		}
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.02);
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			Vec3 at = hit.getLocation();
			level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 2, 0.03, 0.03, 0.03, 0.01);
			discard();
		}
	}
}
