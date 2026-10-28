package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.jetpackcat.JetpackCatStats;
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
 * One shot of Jetpack Cat's Biotic Pawjectiles (her data's graph 5ADB): a paw shot (4) or the centre shot (8, which
 * can't crit), 60 m/s, gone after 40 m. It heals an ally it hits and hurts an enemy by the same amount; a paw shot's
 * headshot does double. Paw shots are her entity 6D19, the centre shot 6D1B: no model, all particles.
 */
public class PawEntity extends OwProjectile {
	/** How far below the eyes a hit still counts as the head (blocks): the kunai's. */
	private static final double HEAD_DEPTH = 0.25;

	private double amount = JetpackCatStats.PAW_DAMAGE;
	private boolean centre;

	public PawEntity(EntityType<? extends PawEntity> type, Level level) {
		super(type, level);
	}

	public PawEntity(LivingEntity owner, Level level, boolean centre) {
		super(OcEntities.PAW, owner, level, new ItemStack(Items.SNOWBALL));
		this.centre = centre;
		this.amount = centre ? JetpackCatStats.CENTRE_DAMAGE : JetpackCatStats.PAW_DAMAGE;
	}

	/** Against enemies: paw 0.19 m, centre 0.37 m (W); the box is the centre's. */
	@Override
	public float hitRadius() {
		return (float) (centre ? JetpackCatStats.CENTRE_RADIUS : JetpackCatStats.PAW_RADIUS);
	}

	/**
	 * Allies are hit from further (paw 0.31 m, centre 0.62 m: W; her data doubles them): before its move, a sweep along
	 * it for an ally within that reach, short of any block in the way.
	 */
	@Override
	public void tick() {
		if (!level().isClientSide() && isAlive() && getOwner() instanceof LivingEntity owner) {
			Vec3 from = position();
			Vec3 to = from.add(getDeltaMovement());
			BlockHitResult wall = level().clip(new net.minecraft.world.level.ClipContext(from, to,
				net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
			if (wall.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
				to = wall.getLocation();
			}
			float reach = (float) (centre ? JetpackCatStats.CENTRE_RADIUS_ALLY : JetpackCatStats.PAW_RADIUS_ALLY);
			EntityHitResult ally = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level(), this, from, to,
				getBoundingBox().expandTowards(getDeltaMovement()).inflate(1.0),
				e -> canHitEntity(e) && e instanceof LivingEntity living && Allies.isAlly(owner, living), reach);
			if (ally != null) {
				onHitEntity(ally);
				return;
			}
		}
		super.tick();
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SNOWBALL;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(JetpackCatStats.RANGE / JetpackCatStats.PROJECTILE_SPEED);
	}

	public boolean centre() {
		return centre;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// allies and enemies alike (it heals one and hurts the other); not her
		return super.canHitEntity(entity) && entity != getOwner();
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
		if (target != null && Allies.isAlly(owner, target)) {
			float healed = Combat.heal(target, amount);
			if (owner instanceof Player cat && healed > 0) {
				Combat.credit(cat, healed);
				Combat.addFireForHealing(cat, healed);
				dev.overcraft.jetpackcat.JetpackCat.healedWithPaws(cat, healed);
			}
			Effects.play(level, centre ? "paw_heal_centre" : "paw_heal", at, owner);
		} else if (target != null && Allies.canHurt(owner, target)) {
			boolean crit = !centre && (part != null ? part == part.parentMob.head : at.y >= target.getEyeY() - HEAD_DEPTH);
			double damage = amount * (crit ? JetpackCatStats.PAW_CRIT : 1);
			float dealt = part != null ? Combat.damagePart(level, part, damageSources().thrown(this, owner), damage)
				: Combat.damage(target, damageSources().thrown(this, owner), damage);
			if (owner instanceof Player cat) {
				Combat.credit(cat, dealt);
				Combat.addFireForDamage(cat, dealt, Ow.toOw(target.getMaxHealth()));
				if (cat instanceof ServerPlayer shooter && dealt > 0) {
					Effects.hitMarker(shooter, target, crit);
				}
			}
			Effects.play(level, centre ? "paw_hit_centre" : "paw_hit", at, owner);
		} else if (target == null) {
			// not a creature (an end crystal, a boat): hit as an arrow does
			hit.getEntity().hurtServer(level, damageSources().thrown(this, owner), (float) Ow.hp(amount));
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
			Effects.play(level, "paw_hit_block", hit.getLocation(), normal, null, centre ? 1 : 0, getOwner());
			discard();
		}
	}
}
