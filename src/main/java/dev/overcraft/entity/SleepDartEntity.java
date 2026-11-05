package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.ana.AnaStats;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Sleep;
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
import net.minecraft.world.phys.EntityHitResult;

/**
 * Ana's Sleep Dart (her graph 0BDD, projectile 15E8): 60 m/s, straight, 0.2 m, gone after 20 s. Its projectile has no
 * target filter of its own (H), so the engine's team rule stands: it flies through allies and stops on enemies (M).
 * The one it hits takes 5 and sleeps 5 s (Sleep).
 */
public class SleepDartEntity extends OwProjectile {
	@Override
	public double barrierDamage() {
		// what an enemy barrier or Kinetic Grasp takes when it stops it (the critique of 2026-11-02: 0 before)
		return dev.overcraft.ana.AnaStats.SLEEP_DAMAGE;
	}

	public SleepDartEntity(EntityType<? extends SleepDartEntity> type, Level level) {
		super(type, level);
	}

	public SleepDartEntity(LivingEntity owner, Level level) {
		super(OcEntities.SLEEP_DART, owner, level, new ItemStack(Items.TIPPED_ARROW));
	}

	@Override
	public float hitRadius() {
		return (float) AnaStats.SLEEP_DART_RADIUS;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.TIPPED_ARROW;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(AnaStats.SLEEP_DART_LIFETIME);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && Allies.isAlly(owner, living))
			// through the phased (W Kiriko rev 408632 "pass through them"), so neither it nor Local Anesthetic's burst is spent there
			&& !(entity instanceof LivingEntity phased && Combat.phaseStops(phased));
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
			// its 5 first, then the sleep: the dart's own hit doesn't wake it
			float dealt = Combat.damage(target, damageSources().thrown(this, owner), AnaStats.SLEEP_DAMAGE, dev.overcraft.hero.Blow.ability("sleep_dart").by(dev.overcraft.hero.Hero.ANA));
			if (isRemoved()) {
				// a barrier between it and the target took it, there and then (LivingEntityMixin: a shot it stops is gone), and the
				// sleep stops with it (#104). Hers is met in flight (Barrier.tickProjectiles), a point-blank one here
				return;
			}
			Combat.credit(owner, dealt, target);
			if (dealt > 0) {
				Effects.hitMarker(owner, target, false);
			}
			if (target.isAlive()) {
				Sleep.put(target, AnaStats.SLEEP_TIME);
				// her line about the sleeper, 0.5 s into the sleep (224E #424)
				dev.overcraft.ana.Ana.later(level, AnaStats.SLEEP_LINE_AT, () -> {
					if (Sleep.asleep(target)) {
						dev.overcraft.hero.Voice.say(owner, "sleep", 1);
					}
				});
			}
			dev.overcraft.hero.HeroSounds.toOwner(owner, dev.overcraft.registry.OcSounds.ANA_SLEEP_DART_HIT);
			dev.overcraft.hero.Effects.play(level, "ana_sleep_hit", hit.getLocation(), net.minecraft.world.phys.Vec3.ZERO, null, 0,
				owner);
		}
		anesthetic(level, owner, hit.getLocation());
		discard();
	}

	@Override
	protected void onHitBlock(net.minecraft.world.phys.BlockHitResult hit) {
		if (level() instanceof ServerLevel level && getOwner() instanceof ServerPlayer owner) {
			anesthetic(level, owner, hit.getLocation());
		}
		if (level() instanceof ServerLevel level) {
			dev.overcraft.hero.HeroSounds.at(level, dev.overcraft.registry.OcSounds.ANA_HIT_BLOCK, hit.getLocation(), 32);
			dev.overcraft.hero.Effects.play(level, "ana_sleep_hit_block", hit.getLocation(),
				net.minecraft.world.phys.Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()), null, 0, getOwner());
		}
		super.onHitBlock(hit);
		// it ends on the world: left alive it hit the ground again every tick (and Local Anesthetic burst each time)
		if (!level().isClientSide()) {
			discard();
		}
	}

	/** Local Anesthetic (minor): the dart bursts where it hits, a creature or a block (D 0BDD #297). */
	private static void anesthetic(ServerLevel level, ServerPlayer owner, net.minecraft.world.phys.Vec3 at) {
		if (dev.overcraft.hero.HeroManager.hasPerk(owner, dev.overcraft.hero.Perk.ANA_LOCAL_ANESTHETIC)) {
			dev.overcraft.ana.Anesthetic.burst(level, owner, at);
		}
	}
}
