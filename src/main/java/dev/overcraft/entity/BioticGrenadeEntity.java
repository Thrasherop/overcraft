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
	@Override
	public double barrierDamage() {
		// what an enemy barrier or Kinetic Grasp takes when it stops it (the critique of 2026-11-02: 0 before)
		return dev.overcraft.ana.AnaStats.GRENADE_DAMAGE;
	}

	// (a burst on an enemy barrier: the barrier takes it and it's gone, no splash on either side: ours, the wiki's
	// "barriers block the explosion")

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
	private boolean canHit(Entity entity) {
		if (!super.canHitEntity(entity) || entity == getOwner()) {
			return false;
		}
		return !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && Allies.isAlly(owner, living)
			&& living.getHealth() >= living.getMaxHealth());
	}

	/** Biotic Bounce: it has burst once and bounced (its second burst is the perk's 60). */
	private boolean bounced;
	/** Ticks left in which it can't hit a creature again: a bounce off one would land on it at once. */
	private int noHit;

	@Override
	public void tick() {
		if (noHit > 0) {
			noHit--;
		}
		super.tick();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return noHit <= 0 && canHit(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		burst(null);
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			// its contact set 0751 on stone (892E + 8930: the other materials' pairs aren't extracted)
			dev.overcraft.hero.HeroSounds.at(level, dev.overcraft.registry.OcSounds.ANA_GRENADE_IMPACT, hit.getLocation(), 90);
		}
		burst(hit);
	}

	/**
	 * Bursts where it is. With Biotic Bounce (major) the first burst sends it on (D 0B97 #176 turns the bounce on; how far
	 * isn't in any source, ours: AnaStats.BOUNCE_KEEP / BOUNCE_UP) and the next contact bursts again for 60.
	 */
	private void burst(@org.jspecify.annotations.Nullable BlockHitResult block) {
		if (!(level() instanceof ServerLevel level) || !isAlive()) {
			return;
		}
		ServerPlayer owner = getOwner() instanceof ServerPlayer p ? p : null;
		if (owner != null) {
			if (bounced) {
				Ana.grenadeBurst(level, owner, position(), this, AnaStats.BOUNCE_BURST, AnaStats.BOUNCE_BURST,
					dev.overcraft.hero.Blow.perk("biotic_bounce"));
			} else {
				Ana.grenadeBurst(level, owner, position(), this);
			}
		}
		if (bounced || owner == null || !dev.overcraft.hero.HeroManager.hasPerk(owner, dev.overcraft.hero.Perk.ANA_BIOTIC_BOUNCE)) {
			discard();
			return;
		}
		bounced = true;
		noHit = 4;
		net.minecraft.world.phys.Vec3 v = getDeltaMovement();
		if (block != null) {
			// off a block: mirrored across its face, slowed
			net.minecraft.core.Direction face = block.getDirection();
			net.minecraft.world.phys.Vec3 n = new net.minecraft.world.phys.Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
			v = v.subtract(n.scale(2 * v.dot(n))).scale(AnaStats.BOUNCE_KEEP);
			// out of the block it hit, so it doesn't hit it again at once
			setPos(block.getLocation().add(n.scale(hitRadius() + 0.01)));
		} else {
			// off a creature: up
			v = new net.minecraft.world.phys.Vec3(v.x * AnaStats.BOUNCE_KEEP, Ow.speed(AnaStats.BOUNCE_UP), v.z * AnaStats.BOUNCE_KEEP);
		}
		setDeltaMovement(v);
	}
}
