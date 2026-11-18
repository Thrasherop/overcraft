package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.cassidy.Cassidy;
import dev.overcraft.cassidy.CassidySounds;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Headshot;
import dev.overcraft.hero.Hero;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.cassidy.CassidyStats.AMMO;
import static dev.overcraft.cassidy.CassidyStats.HEALTH;
import static dev.overcraft.cassidy.CassidyStats.RADIUS;
import static dev.overcraft.cassidy.CassidyStats.RELOAD;
import static dev.overcraft.cassidy.CassidyStats.ROLL_COOLDOWN;
import static dev.overcraft.cassidy.CassidyStats.ROLL_SPEED;
import static dev.overcraft.cassidy.CassidyStats.ROLL_TIME;

/**
 * Enemy Cassidy (#46): Peacekeeper, one shot a second at range (70, his falloff from 25 m, x2 on the head: Cassidy.peacekeeperDamage),
 * six to a cylinder and a 1.5 s reload (CassidyStats). Combat Roll (15 m/s for 0.4 s: 6 m, 5 s cooldown: CassidyStats) away to
 * one side when hit from within 6 m (ours); it reloads him, as his own does. No Flashbang, no Deadeye. Drops iron nuggets (ours).
 */
public class EnemyCassidy extends EnemyHeroEntity {
	/** Hitscan out to 60 m (ours: his is 400, CassidyStats.RANGE; a mob's follow range is far less). */
	private static final double REACH = 60;
	/** His look-ahead muzzle (ours, as a player's view draws it near enough). */
	private static final double MUZZLE_RIGHT = 0.2, MUZZLE_UP = -0.15, MUZZLE_AHEAD = 0.6;

	private int ammo = AMMO;
	private int shotWait = Ow.ticks(EnemyHeroStats.CASSIDY_SHOT_INTERVAL);
	private int reload = -1;
	private int rollCooldown;
	private int rolling = -1;
	private Vec3 rollWay = Vec3.ZERO;

	public EnemyCassidy(EntityType<? extends EnemyCassidy> type, Level level) {
		super(type, level);
	}

	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return attributes(HEALTH, dev.overcraft.hero.OwMovement.RUN_SPEED);
	}

	@Override
	public Hero hero() {
		return Hero.CASSIDY;
	}

	@Override
	protected double near() {
		return EnemyHeroStats.CASSIDY_NEAR;
	}

	@Override
	protected double far() {
		return EnemyHeroStats.CASSIDY_FAR;
	}

	@Override
	protected Item dropItem() {
		return Items.IRON_NUGGET;
	}

	@Override
	protected @Nullable SoundEvent sound(String name) {
		return thirdPerson(CassidySounds::of, name);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			return;
		}
		if (rollCooldown > 0) {
			rollCooldown--;
		}
		if (rolling >= 0) {
			// the roll carries him at its 15 m/s for its 0.4 s (CassidyStats), whatever his feet were doing
			Vec3 v = rollWay.scale(Ow.speed(ROLL_SPEED));
			setDeltaMovement(v.x, getDeltaMovement().y, v.z);
			needsSync = true;
			if (++rolling >= Ow.ticks(ROLL_TIME)) {
				rolling = -1;
			}
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
		if (reload >= 0 || rolling >= 0 || shotWait > 0 || !sees || distance > REACH) {
			return;
		}
		shot(level, target);
		ammo--;
		shotWait = Ow.ticks(EnemyHeroStats.CASSIDY_SHOT_INTERVAL);
		if (ammo <= 0) {
			startReload(level);
		}
	}

	private void startReload(ServerLevel level) {
		reload = 0;
		Combat.animate(this, "reload");
		play(level, "reload", getEyePosition(), 45);
		Effects.play(level, "cassidy_reload", getEyePosition(), Vec3.ZERO, this, Ow.ticks(RELOAD), this);
	}

	/** One Peacekeeper shot (his Cassidy.shot, cut down: no pierce, no Sharpshooter). */
	private void shot(ServerLevel level, LivingEntity target) {
		Vec3 eye = getEyePosition();
		Vec3 look = bodyOf(target).subtract(eye).normalize();
		// his primary has no spread (CassidyStats): the difficulty's alone
		Vec3 dir = dev.overcraft.sombra.Sombra.cone(look, spread(0), random);
		Traced t = trace(level, eye, dir, REACH, RADIUS, m -> Cassidy.peacekeeperDamage(m, false));
		if (t.target() != null) {
			boolean crit = Headshot.crossed(t.target(), t.at(), dir, RADIUS);
			deal(t.target(), shotSource(null), Cassidy.peacekeeperDamage(t.metres(), crit), Blow.weapon(crit));
			play(level, "peacekeeper.hit_enemy", t.at(), 32);
			Effects.play(level, "cassidy_hit", t.at(), dir.scale(-1), t.target(), 0, this);
		} else if (t.block().getType() == HitResult.Type.BLOCK) {
			play(level, "peacekeeper.hit_block", t.at(), 32);
			Vec3 n = Vec3.atLowerCornerOf(t.block().getDirection().getUnitVec3i());
			Effects.play(level, "cassidy_hit", t.at(), n, null, 1, this);
		}
		Vec3 from = fromEyes(look, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Effects.play(level, "cassidy_shot", from, t.at().subtract(from), this, 0, this);
		play(level, "peacekeeper.fire", eye, 200);
		Combat.animate(this, "fire");
	}

	/** Hit from close: Combat Roll to one side (ours), reloading him as his own does (CassidyStats.ROLL_RELOAD_AT). */
	@Override
	protected void hurtBy(ServerLevel level, LivingEntity attacker) {
		if (rollCooldown > 0 || rolling >= 0 || distanceTo(attacker) > EnemyHeroStats.CASSIDY_ROLL_WITHIN || !onGround()) {
			return;
		}
		Vec3 toward = attacker.position().subtract(position());
		Vec3 side = new Vec3(-toward.z, 0, toward.x);
		if (side.lengthSqr() < 1.0E-6) {
			return;
		}
		int direction = random.nextBoolean() ? 1 : 3;
		rollWay = side.normalize().scale(direction == 1 ? 1 : -1);
		rolling = 0;
		rollCooldown = Ow.ticks(ROLL_COOLDOWN);
		reload = -1;
		ammo = AMMO;
		getNavigation().stop();
		resetFallDistance();
		Combat.animate(this, "roll_s" + direction);
		play(level, "roll", position(), 40);
		Effects.play(level, "cassidy_roll", position(), rollWay, this, Ow.ticks(ROLL_TIME), this);
	}
}
