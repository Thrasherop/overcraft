package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.torbjorn.Torbjorn;
import dev.overcraft.torbjorn.TorbjornStats;
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
import net.minecraft.world.phys.Vec3;

/**
 * One of Torbjörn's Rivet Gun shots: a rivet (1683 #171: projectile 2A66; 70, 70 m/s falling at 9.8, x2 on the head, no falloff) or
 * one of the alt's ten pellets (#178: 13ED; 12.5 at 120 m/s, no gravity, falling off from 10 to 20 m to x0.3). Allies don't stop it.
 * No model in his data (effects only): the item stands in.
 */
public class RivetEntity extends OwProjectile {
	private Vec3 from = Vec3.ZERO;

	public RivetEntity(EntityType<? extends RivetEntity> type, Level level) {
		super(type, level);
	}

	public RivetEntity(LivingEntity owner, Level level, boolean pellet) {
		super(pellet ? OcEntities.TORBJORN_PELLET : OcEntities.RIVET, owner, level, new ItemStack(Items.IRON_NUGGET));
	}

	public boolean pellet() {
		return getType() == OcEntities.TORBJORN_PELLET;
	}

	@Override
	public void launch(Vec3 at, Vec3 direction, double speed) {
		super.launch(at, direction, speed);
		from = at;
	}

	@Override
	public float hitRadius() {
		return (float) (pellet() ? TorbjornStats.PELLET_RADIUS : TorbjornStats.RIVET_RADIUS);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.IRON_NUGGET;
	}

	@Override
	protected int lifetime() {
		// 20 s a rivet, 10 a pellet (D #171 / #178)
		return Ow.ticks(pellet() ? 10 : 20);
	}

	@Override
	protected double getDefaultGravity() {
		return pellet() ? 0 : Ow.accel(TorbjornStats.RIVET_GRAVITY);
	}

	/** Travelled so far (m): the pellets' falloff. */
	public double travelled() {
		return position().distanceTo(from);
	}

	@Override
	public double barrierDamage() {
		return pellet() ? TorbjornStats.PELLET_DAMAGE : TorbjornStats.RIVET_DAMAGE;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof TurretEntity t && t.ownerUuid() != null
			&& getOwner() != null && t.ownerUuid().equals(getOwner().getUUID()))
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (level() instanceof ServerLevel level && getOwner() instanceof ServerPlayer owner) {
			Torbjorn.rivetHit(owner, this, hit.getEntity(), hit.getLocation());
			// its look on a creature: the rivet's 2650, a pellet's 05AF (TorbjornFx), facing back along the shot
			Vec3 back = getDeltaMovement().lengthSqr() < 1.0E-6 ? Vec3.ZERO : getDeltaMovement().normalize().scale(-1);
			dev.overcraft.hero.Effects.play(level, pellet() ? "torb_pellet_hit" : "torb_rivet_hit", hit.getLocation(), back, null, 0, owner);
		} else if (level() instanceof ServerLevel level && getOwner() instanceof dev.overcraft.entity.enemy.EnemyTorbjorn mob) {
			// an enemy Torbjörn's (#46): the same 70, the mob's own rules (Torbjorn.rivetHit takes a player)
			mob.rivetHit(this, hit.getEntity(), hit.getLocation());
			Vec3 back = getDeltaMovement().lengthSqr() < 1.0E-6 ? Vec3.ZERO : getDeltaMovement().normalize().scale(-1);
			dev.overcraft.hero.Effects.play(level, pellet() ? "torb_pellet_hit" : "torb_rivet_hit", hit.getLocation(), back, null, 0, mob);
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel level) {
			Vec3 at = hit.getLocation();
			// its look on the world: the rivet's 2650, a pellet's 265B (TorbjornFx), off the face it hit
			Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
			dev.overcraft.hero.Effects.play(level, pellet() ? "torb_pellet_world" : "torb_rivet_hit", at, normal, null, 0, getOwner());
			dev.overcraft.torbjorn.TorbjornSounds.all(level, pellet() ? "rivet.alt_hit_world" : "rivet.hit_world", at, 40);
			discard();
		}
	}
}
