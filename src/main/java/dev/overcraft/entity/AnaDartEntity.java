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
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * One unscoped Biotic Rifle dart (her rifle graph 0B94, projectile 15B7): 125 m/s, straight, gone after 5 s. It hits an
 * enemy within 0.15 m (Minecraft's own hit test, with that margin) and a hurt ally within 0.3 m (its own sweep, below);
 * allies at full health don't stop it, and it never heals Ana herself (tools/reference/ana.md "Biotic Rifle"). What it
 * hits takes 75, heal or damage, over 0.5 s (Ana.dose).
 */
public class AnaDartEntity extends OwProjectile {
	public AnaDartEntity(EntityType<? extends AnaDartEntity> type, Level level) {
		super(type, level);
	}

	public AnaDartEntity(LivingEntity owner, Level level) {
		super(OcEntities.ANA_DART, owner, level, new ItemStack(Items.ARROW));
	}

	@Override
	public float hitRadius() {
		return (float) AnaStats.DART_RADIUS_ENEMY;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.ARROW;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(AnaStats.DART_LIFETIME);
	}

	/** Minecraft's hit test takes enemies only; allies are this dart's own sweep's. */
	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && Allies.isAlly(owner, living));
	}

	@Override
	public void tick() {
		if (level() instanceof ServerLevel level && getOwner() instanceof ServerPlayer owner && isAlive()) {
			LivingEntity ally = hurtAllyAhead(owner);
			if (ally != null) {
				Ana.dose(owner, ally, true, this);
				discard();
				return;
			}
		}
		super.tick();
	}

	/** The nearest hurt ally (not Ana) whose box, widened by the ally radius, lies on this tick's flight. */
	private LivingEntity hurtAllyAhead(ServerPlayer owner) {
		Vec3 from = position();
		Vec3 to = from.add(getDeltaMovement());
		double r = AnaStats.DART_RADIUS_ALLY;
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(r + 1))) {
			if (e == owner || !e.isAlive() || e.getHealth() >= e.getMaxHealth() || !Allies.isAlly(owner, e)) {
				continue;
			}
			Optional<Vec3> hit = e.getBoundingBox().inflate(r).clip(from, to);
			if (hit.isPresent() && hit.get().distanceToSqr(from) < bestDist) {
				best = e;
				bestDist = hit.get().distanceToSqr(from);
			}
		}
		return best;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof ServerPlayer owner)) {
			discard();
			return;
		}
		EnderDragonPart part = hit.getEntity() instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : hit.getEntity() instanceof LivingEntity living ? living : null;
		if (target != null && Allies.canHurt(owner, target)) {
			Ana.dose(owner, target, false, this);
		} else if (target == null) {
			// not a creature (an end crystal, a boat): hit as an arrow does
			hit.getEntity().hurtServer(level, damageSources().thrown(this, owner), (float) Ow.hp(AnaStats.RIFLE_DAMAGE));
		}
		discard();
	}
}
