package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.entity.RivetEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Headshot;
import dev.overcraft.hero.Hero;
import dev.overcraft.torbjorn.Torbjorn;
import dev.overcraft.torbjorn.TorbjornSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static dev.overcraft.torbjorn.TorbjornStats.AMMO;
import static dev.overcraft.torbjorn.TorbjornStats.ARMOR;
import static dev.overcraft.torbjorn.TorbjornStats.CRIT;
import static dev.overcraft.torbjorn.TorbjornStats.HEALTH;
import static dev.overcraft.torbjorn.TorbjornStats.MUZZLE_AHEAD;
import static dev.overcraft.torbjorn.TorbjornStats.MUZZLE_RIGHT;
import static dev.overcraft.torbjorn.TorbjornStats.MUZZLE_UP;
import static dev.overcraft.torbjorn.TorbjornStats.PELLET_DAMAGE;
import static dev.overcraft.torbjorn.TorbjornStats.RELOAD;
import static dev.overcraft.torbjorn.TorbjornStats.RIVET_DAMAGE;
import static dev.overcraft.torbjorn.TorbjornStats.RIVET_GRAVITY;
import static dev.overcraft.torbjorn.TorbjornStats.RIVET_SPEED;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_CAST;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_COOLDOWN;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_GRAVITY;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_HEIGHT;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_THROW_AT;
import static dev.overcraft.torbjorn.TorbjornStats.TURRET_THROW_SPEED;

/**
 * Enemy Torbjörn (#46, batch 2): his Rivet Gun's rivets (his own RivetEntity: 70, x2 on the head, 70 m/s falling at 9.8 m/s²), lobbed
 * on the arc that reaches his target's middle, one each 1 s (ours; his 0.51 s), 18 then his 2 s reload (TorbjornStats). His turret
 * (EnemyTurretEntity: his own TurretEntity, built in 2.5 s, 225 health, 12 a shot) thrown once his target is within 20 m and in sight
 * (ours), 3 m toward it on the arc of its 17 m/s throw and 20 m/s² fall (TorbjornStats), one out at a time; when it's broken, his
 * 12 s cooldown before the next (TorbjornStats.TURRET_COOLDOWN). His health 225 + his armor's 75 as health (ours: Minecraft's
 * creatures have no Overwatch armor layer). No alt fire, Forge Hammer, Overload or Molten Core. Drops copper ingots (ours).
 */
public class EnemyTorbjorn extends EnemyHeroEntity {
	private int ammo = AMMO;
	private int shotWait = Ow.ticks(EnemyHeroStats.TORBJORN_SHOT_INTERVAL);
	private int reload = -1;
	/** Ticks into his turret's throw (-1: none): it leaves 0.05 s in, his weapons held for 0.5 s (TorbjornStats). */
	private int turretCast = -1;
	private int turretCooldown;
	private @Nullable UUID turret;
	private int turretMissing;

	public EnemyTorbjorn(EntityType<? extends EnemyTorbjorn> type, Level level) {
		super(type, level);
	}

	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return attributes(HEALTH + ARMOR, dev.overcraft.hero.OwMovement.RUN_SPEED);
	}

	@Override
	public Hero hero() {
		return Hero.TORBJORN;
	}

	@Override
	protected double near() {
		return EnemyHeroStats.TORBJORN_NEAR;
	}

	@Override
	protected double far() {
		return EnemyHeroStats.TORBJORN_FAR;
	}

	@Override
	protected Item dropItem() {
		return Items.COPPER_INGOT;
	}

	@Override
	protected @Nullable SoundEvent sound(String name) {
		return thirdPerson(TorbjornSounds::of, name);
	}

	/** Whether {@code t} is the turret he has out (EnemyTurretEntity: one that isn't goes). */
	public boolean holds(EnemyTurretEntity t) {
		return t.getUUID().equals(turret);
	}

	/** A new target or none: a throw not yet made is dropped (the critique of 29c1797: it went later without its checks). */
	@Override
	protected void targetChanged() {
		turretCast = -1;
	}

	/** His turret, out and alive, or null. */
	private @Nullable EnemyTurretEntity turret(ServerLevel level) {
		return turret != null && level.getEntity(turret) instanceof EnemyTurretEntity t && t.isAlive() ? t : null;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (turretCooldown > 0) {
			turretCooldown--;
		}
		if (turret != null && turret(level) == null) {
			// broken or gone for 2 s (not a chunk's load order): the data's in-combat cooldown before the next
			// (TorbjornStats.TURRET_COOLDOWN). One he forgets goes itself on meeting him again (EnemyTurretEntity: not his)
			if (++turretMissing >= Ow.ticks(EnemyHeroStats.TURRET_ORPHAN)) {
				turret = null;
				turretMissing = 0;
				turretCooldown = Ow.ticks(TURRET_COOLDOWN);
			}
		} else {
			turretMissing = 0;
		}
		if (reload >= 0 && ++reload >= Ow.ticks(RELOAD)) {
			reload = -1;
			ammo = AMMO;
		}
	}

	@Override
	protected void fight(ServerLevel level, LivingEntity target, double distance, boolean sees) {
		if (shotWait > 0) {
			shotWait--;
		}
		if (turretCast >= 0) {
			if (++turretCast == Math.max(1, Ow.ticks(TURRET_THROW_AT))) {
				throwTurret(level, target);
			}
			if (turretCast >= Ow.ticks(TURRET_CAST)) {
				turretCast = -1;
			}
			return;
		}
		// (not while hacked: no abilities, the critique of #46)
		if (sees && turret == null && turretCooldown <= 0 && distance <= EnemyHeroStats.TORBJORN_TURRET_WITHIN && onGround()
			&& !dev.overcraft.sombra.Hack.silenced(this)) {
			turretCast = 0;
			Combat.animate(this, "turret_throw");
			play(level, "turret.throw", getEyePosition(), 40);
			return;
		}
		if (reload >= 0 || shotWait > 0 || !sees) {
			return;
		}
		rivet(level, target);
		ammo--;
		shotWait = Ow.ticks(EnemyHeroStats.TORBJORN_SHOT_INTERVAL);
		if (ammo <= 0) {
			reload = 0;
			Combat.animate(this, "reload");
			play(level, "reload", getEyePosition(), 30);
			Effects.play(level, "torb_reload", getEyePosition(), Vec3.ZERO, this, Ow.ticks(RELOAD), this);
		}
	}

	/** One rivet at {@code target}'s middle, on the low arc of its 70 m/s and 9.8 m/s² fall (EnemyHeroStats.lob). */
	private void rivet(ServerLevel level, LivingEntity target) {
		Vec3 aimAt = bodyOf(target);
		Vec3 look = aimAt.subtract(getEyePosition()).normalize();
		Vec3 from = fromEyes(look, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Vec3 dir = EnemyHeroStats.lob(aimAt.subtract(from), Ow.speed(RIVET_SPEED), Ow.accel(RIVET_GRAVITY));
		dir = dev.overcraft.sombra.Sombra.cone(dir, spread(0), random);
		RivetEntity rivet = new RivetEntity(this, level, false);
		rivet.launch(from, dir, Ow.speed(RIVET_SPEED));
		level.addFreshEntity(rivet);
		Combat.animate(this, "fire");
		play(level, "rivet.fire", from, 80);
		Effects.play(level, "torb_muzzle", from, rivet.getDeltaMovement(), this, 0, this);
	}

	/** A rivet of his hit {@code hit} (RivetEntity): 70, x2 on the head (his Torbjorn.rivetHit, without a player's credit). */
	public void rivetHit(RivetEntity shot, Entity hit, Vec3 at) {
		if (!(level() instanceof ServerLevel level) || !(hit instanceof LivingEntity target) || !target.isAlive() || !Allies.canHurt(this, target)) {
			return;
		}
		boolean crit = Headshot.crossed(target, at, shot.getDeltaMovement(), shot.hitRadius());
		double base = shot.pellet() ? PELLET_DAMAGE * Torbjorn.pelletFalloff(shot.travelled()) : RIVET_DAMAGE;
		if (deal(target, shotSource(shot), base * (crit ? CRIT : 1), Blow.weapon(crit)) > 0) {
			play(level, shot.pellet() ? "rivet.alt_hit_enemy" : "rivet.hit_enemy", at, 80);
		}
	}

	/** His turret leaves his hand: 3 m toward his target (ours) on the arc of its throw (TorbjornStats). */
	private void throwTurret(ServerLevel level, LivingEntity target) {
		Vec3 eye = getEyePosition();
		Vec3 toward = target.position().subtract(position());
		Vec3 flat = new Vec3(toward.x, 0, toward.z);
		flat = flat.lengthSqr() < 1.0E-6 ? getLookAngle() : flat.normalize();
		Vec3 land = position().add(flat.scale(EnemyHeroStats.TORBJORN_TURRET_AHEAD));
		// it flies with its box's foot TURRET_HEIGHT / 2 under the point it's launched from (TurretEntity.launch)
		Vec3 from = eye.add(flat.scale(0.5));
		Vec3 foot = from.add(0, -TURRET_HEIGHT / 2, 0);
		Vec3 dir = EnemyHeroStats.lob(land.subtract(foot), Ow.speed(TURRET_THROW_SPEED), Ow.accel(TURRET_GRAVITY));
		EnemyTurretEntity t = new EnemyTurretEntity(this, level);
		t.launch(from, dir.scale(Ow.speed(TURRET_THROW_SPEED)));
		level.addFreshEntity(t);
		turret = t.getUUID();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (turret != null) {
			EntityReference.store(EntityReference.of(turret), output, "EnemyTurret");
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		EntityReference<Entity> t = EntityReference.read(input, "EnemyTurret");
		turret = t == null ? null : t.getUUID();
	}
}
