package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.entity.TurretEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.torbjorn.TorbjornSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static dev.overcraft.torbjorn.TorbjornStats.TURRET_DAMAGE;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_FIRE_CONE;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_INTERVAL;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_RANGE;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_TURN;

/**
 * An enemy Torbjörn's turret (#46, batch 2): his TurretEntity (its flight, its 2.5 s build, its 225 health, its model and look: the
 * same renderer), with no player owner, so none of a player's turret code runs for it (TurretEntity.tick fights only for a player
 * owner). It's the monsters' side as he is ({@link Enemy}: a player's heroes take it as an enemy, Allies; the monsters leave it be;
 * iron golems fight it). Here it fights for him: the closest creature in sight within 40 m he may lock on to (Allies.lockOn with him
 * as the hero: players, their pets, golems after him; never a monster or a bystander), his own target first; 12 a shot every 0.256 s,
 * turning 30 degrees a tick and firing within 10 (TorbjornStats). Its damage is his (EnemyHeroEntity.deal: the difficulty's factor
 * on a player). It goes when he's been gone 2 s (ours: dead, despawned or unloaded). No level 3 (no Overload).
 */
public class EnemyTurretEntity extends TurretEntity implements Enemy {
	private static final Blow TURRET_BLOW = Blow.ability("deploy_turret");

	private @Nullable UUID builder;
	private @Nullable UUID targetId;
	private double fireIn;
	private int orphaned;

	public EnemyTurretEntity(EntityType<? extends EnemyTurretEntity> type, Level level) {
		super(type, level);
	}

	public EnemyTurretEntity(EnemyTorbjorn torbjorn, Level level) {
		this(dev.overcraft.registry.OcEntities.ENEMY_TURRET, level);
		builder = torbjorn.getUUID();
		setHealth(getMaxHealth());
	}

	/** The enemy Torbjörn it fights for, here and alive, or null. */
	public @Nullable EnemyTorbjorn torbjorn() {
		return builder != null && level() instanceof ServerLevel level && level.getEntity(builder) instanceof EnemyTorbjorn t && t.isAlive()
			? t : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level) || isRemoved()) {
			return;
		}
		EnemyTorbjorn torbjorn = torbjorn();
		orphaned = torbjorn == null ? orphaned + 1 : 0;
		if (!EnemyHeroStats.turretStays(torbjorn != null, torbjorn != null && torbjorn.holds(this), orphaned)) {
			// he's gone, or he has another out (he forgot this one while its chunk was unloaded): it goes
			discard();
			return;
		}
		if (torbjorn == null) {
			return;
		}
		if (built() && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL) {
			fight(level, torbjorn);
		}
	}

	private boolean canShoot(ServerLevel level, EnemyTorbjorn owner, LivingEntity e) {
		Vec3 gun = gun();
		// (not a creative or spectating player, as Minecraft's own targeting: the critique of 29c1797)
		return e.isAlive() && e != this && Allies.lockOn(owner, e) && EnemyHeroStats.scanMayPick(e instanceof net.minecraft.world.entity.player.Player,
			e instanceof net.minecraft.world.entity.player.Player p && p.isCreative(), e.isSpectator()) && e.getBoundingBox().getCenter().distanceTo(gun) <= TURRET_RANGE
			&& Combat.inSight(level, gun, e, this);
	}

	/** Its target: the one it has while it can still shoot it, else his, else the closest it may shoot in sight within 40 m. */
	private @Nullable LivingEntity target(ServerLevel level, EnemyTorbjorn owner) {
		if (targetId != null && level.getEntity(targetId) instanceof LivingEntity e && canShoot(level, owner, e)) {
			return e;
		}
		targetId = null;
		LivingEntity his = owner.getTarget();
		LivingEntity best = his != null && canShoot(level, owner, his) ? his : null;
		if (best == null) {
			double bestD = Double.MAX_VALUE;
			Vec3 gun = gun();
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(gun, gun).inflate(TURRET_RANGE))) {
				double d = e.getBoundingBox().getCenter().distanceToSqr(gun);
				if (d < bestD && canShoot(level, owner, e)) {
					best = e;
					bestD = d;
				}
			}
		}
		if (best != null) {
			targetId = best.getUUID();
		}
		return best;
	}

	/** As TurretEntity's own fight (its turn, its cone, its rate), for him. */
	private void fight(ServerLevel level, EnemyTorbjorn owner) {
		fireIn = Math.max(0, fireIn - 1);
		// hacked by a player's Sombra: shut down for the hack, as a player's turret is (TurretEntity.fight)
		if (dev.overcraft.sombra.Hack.hacked(this)) {
			return;
		}
		LivingEntity target = tickCount % 2 == 0 || targetId != null ? target(level, owner) : null;
		if (target == null) {
			return;
		}
		Vec3 gun = gun();
		Vec3 to = target.getBoundingBox().getCenter().subtract(gun);
		float want = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
		float diff = net.minecraft.util.Mth.wrapDegrees(want - getYRot());
		float turn = (float) Math.max(-TURRET_TURN, Math.min(TURRET_TURN, diff));
		setYRot(getYRot() + turn);
		setXRot((float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))));
		if (Math.abs(diff - turn) > TURRET_FIRE_CONE || fireIn > 0) {
			return;
		}
		fireIn += TURRET_INTERVAL * 20;
		Vec3 at = target.getBoundingBox().getCenter();
		fired();
		Barriers.Shape barrier = Barriers.between(level, gun, at, owner);
		if (barrier != null) {
			barrier.barrierTake(TURRET_DAMAGE, at);
		} else {
			owner.deal(target, owner.shotSource(this), TURRET_DAMAGE, TURRET_BLOW);
		}
		dev.overcraft.hero.Effects.play(level, "torb_turret_shot", gun, at.subtract(gun), this, 0, owner);
		TorbjornSounds.all(level, "turret.fire", gun, 50);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (builder != null) {
			EntityReference.store(EntityReference.of(builder), output, "EnemyTorbjorn");
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		EntityReference<LivingEntity> t = EntityReference.read(input, "EnemyTorbjorn");
		builder = t == null ? null : t.getUUID();
	}
}
