package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.lucio.LucioStats;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One round of Lúcio's Sonic Amplifier: 50 m/s, no drop, no falloff, 2x on a headshot, a small push. Passes through
 * allies; hits what isn't a creature (an end crystal, a boat) as an arrow does, like the kunai (#34).
 */
public class SonicEntity extends OwProjectile {
	@Override
	public double barrierDamage() {
		// what an enemy barrier or Kinetic Grasp takes when it stops it (the critique of 2026-11-02: 0 before)
		return dev.overcraft.lucio.LucioStats.AMP_DAMAGE;
	}


	public SonicEntity(EntityType<? extends SonicEntity> type, Level level) {
		super(type, level);
	}

	public SonicEntity(LivingEntity owner, Level level) {
		super(OcEntities.SONIC, owner, level, new ItemStack(OcItems.SONIC));
	}

	@Override
	protected Item getDefaultItem() {
		return OcItems.SONIC;
	}

	@Override
	protected int lifetime() {
		// ours: 60 m of flight, past which a Minecraft fight rarely reaches
		return Ow.ticks(60 / LucioStats.AMP_SPEED);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		Entity target = entity instanceof EnderDragonPart part ? part.parentMob : entity;
		if (!super.canHitEntity(entity)) {
			return false;
		}
		return !(target instanceof LivingEntity) || getOwner() instanceof LivingEntity owner && Allies.canHurt(owner, target);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		EnderDragonPart part = hit.getEntity() instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : hit.getEntity() instanceof LivingEntity living ? living : null;
		Vec3 at = hit.getLocation();
		if (target == null) {
			hit.getEntity().hurtServer(level, damageSources().thrown(this, getOwner()), (float) Ow.hp(LucioStats.AMP_DAMAGE));
			Effects.play(level, "sonic_hit_entity", at, getOwner());
		// the round's hit (effect 0838)
		level.playSound(null, at.x, at.y, at.z, dev.overcraft.registry.OcSounds.LUCIO_AMP_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
			discard();
			return;
		}
		boolean crit = part != null ? part == part.parentMob.head : dev.overcraft.hero.Headshot.crossed(target, at, getDeltaMovement());
		HeroState state = crit && getOwner() instanceof Player shooter ? HeroManager.state(shooter) : null;
		if (state != null) {
			state.critAt = level.getGameTime();
		}
		double damage = LucioStats.AMP_DAMAGE * (crit ? LucioStats.AMP_CRIT : 1);
		float dealt = part != null ? Combat.damagePart(level, part, damageSources().thrown(this, getOwner()), damage, dev.overcraft.hero.Blow.weapon(crit).by(dev.overcraft.hero.Hero.LUCIO))
			: Combat.damage(target, damageSources().thrown(this, getOwner()), damage, dev.overcraft.hero.Blow.weapon(crit).by(dev.overcraft.hero.Hero.LUCIO));
		// a small push along the round's flight (the knockback exists: BZ 2024-07-12, tools/reference/lucio.md; its size is ours)
		Vec3 push = getDeltaMovement().normalize().scale(Ow.speed(LucioStats.AMP_KNOCKBACK));
		if (part == null && target.isAlive()) {
			Combat.push(target, new Vec3(push.x, 0, push.z), getOwner(), dev.overcraft.hero.Blow.WEAPON.by(dev.overcraft.hero.Hero.LUCIO));
		}
		Effects.play(level, "sonic_hit_entity", at, getOwner());
		if (getOwner() instanceof Player owner) {
			Combat.credit(owner, dealt, target);
			Combat.addFireForDamage(owner, dealt, Ow.toOw(target.getMaxHealth()));
			if (owner instanceof ServerPlayer shooter && dealt > 0) {
				Effects.hitMarker(shooter, target, crit);
			}
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			// sparks off the face it hit (0AE0)
			Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
			Effects.play(level, "sonic_hit_block", hit.getLocation(), normal, null, 0, getOwner());
			discard();
		}
	}
}
