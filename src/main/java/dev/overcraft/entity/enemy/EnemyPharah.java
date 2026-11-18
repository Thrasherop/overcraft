package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.entity.PharahRocketEntity;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.pharah.Pharah;
import dev.overcraft.pharah.PharahSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.pharah.PharahStats.AMMO;
import static dev.overcraft.pharah.PharahStats.FUEL_MAX;
import static dev.overcraft.pharah.PharahStats.FUEL_REGEN;
import static dev.overcraft.pharah.PharahStats.FUEL_REGEN_DELAY;
import static dev.overcraft.pharah.PharahStats.HEALTH;
import static dev.overcraft.pharah.PharahStats.HOVER_RISE;
import static dev.overcraft.pharah.PharahStats.MUZZLE_AHEAD;
import static dev.overcraft.pharah.PharahStats.MUZZLE_RIGHT;
import static dev.overcraft.pharah.PharahStats.MUZZLE_UP;
import static dev.overcraft.pharah.PharahStats.RECOVERY;
import static dev.overcraft.pharah.PharahStats.RECOVERY_LAST;
import static dev.overcraft.pharah.PharahStats.RELOAD;
import static dev.overcraft.pharah.PharahStats.ROCKET_SPEED;

/**
 * Enemy Pharah (#46): her Rocket Launcher's rockets (her own PharahRocketEntity: 40 m/s; 40 direct, 80 splash to 2.5 m and its push:
 * EnemyBlasts.rocket), six then her 1.5 s reload, 0.8 s apart (0.55 after the last) (PharahStats). In a fight she takes to the air
 * on her Hover Jets: no gravity, rising at most her 4.66 m/s (PharahStats.HOVER_RISE) to 3-6 m over the ground, drifting to keep her
 * range; after about 5 s she comes down and "refuels" on the ground for her fuel's time (100 at 45 a second after 0.3 s:
 * PharahStats) (the times ours, the plan). No Concussive Blast, no Barrage. Drops firework rockets (ours).
 */
public class EnemyPharah extends EnemyHeroEntity {
	private static final int GROUND = 0, HOVER = 1, DOWN = 2;
	/** Her way down when her jets are spent (ours: slow enough to land without a fall). */
	private static final double DESCENT = 2.5;
	/** Ours: how hard she eases back to her height (a share of the gap a tick). */
	private static final double HEIGHT_EASE = 0.15;

	private int ammo = AMMO;
	private int recovery;
	private int reload = -1;
	private int flight = GROUND;
	private int flightTicks;
	private int groundTicks = Ow.ticks(FUEL_REGEN_DELAY + FUEL_MAX / FUEL_REGEN);

	public EnemyPharah(EntityType<? extends EnemyPharah> type, Level level) {
		super(type, level);
	}

	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return attributes(HEALTH, dev.overcraft.hero.OwMovement.RUN_SPEED);
	}

	@Override
	public Hero hero() {
		return Hero.PHARAH;
	}

	@Override
	protected double near() {
		return EnemyHeroStats.PHARAH_NEAR;
	}

	@Override
	protected double far() {
		return EnemyHeroStats.PHARAH_FAR;
	}

	@Override
	protected Item dropItem() {
		return Items.FIREWORK_ROCKET;
	}

	@Override
	protected @Nullable SoundEvent sound(String name) {
		return thirdPerson(PharahSounds::of, name);
	}

	@Override
	protected boolean steersItself() {
		return flight != GROUND;
	}

	/** Her jets set her down softly (ours): no fall damage from a landing she flew. */
	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return flight == GROUND && super.causeFallDamage(fallDistance, damageModifier, damageSource);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			return;
		}
		if (recovery > 0) {
			recovery--;
		}
		if (reload >= 0 && ++reload >= Ow.ticks(RELOAD)) {
			reload = -1;
			ammo = AMMO;
		}
		LivingEntity target = getTarget();
		boolean fighting = target != null && target.isAlive();
		switch (flight) {
			case GROUND -> {
				groundTicks++;
				if (fighting && onGround() && groundTicks >= Ow.ticks(FUEL_REGEN_DELAY + FUEL_MAX / FUEL_REGEN) && !isInWater()) {
					flight = HOVER;
					flightTicks = 0;
					setNoGravity(true);
				}
			}
			case HOVER -> {
				if (++flightTicks >= Ow.ticks(EnemyHeroStats.PHARAH_HOVER_TIME) || !fighting || isInWater()) {
					flight = DOWN;
				}
				steer(target, EnemyHeroStats.PHARAH_HOVER_AIM - heightAboveGround());
			}
			case DOWN -> {
				if (onGround() || isInWater()) {
					flight = GROUND;
					groundTicks = 0;
					setNoGravity(false);
				} else {
					steer(target, Double.NEGATIVE_INFINITY);
				}
			}
			default -> {
			}
		}
		int look = flight != GROUND ? Pharah.LOOK_HOVER : 0;
		if (!java.util.Objects.equals(getAttached(HeroManager.PHARAH_STATE), look)) {
			// her pose in the air (HeroAnimator reads it as it reads a player Pharah's)
			setAttached(HeroManager.PHARAH_STATE, look);
		}
	}

	/** Her speed for the next tick in the air: toward her height ({@code gap} m off it; -inf: down), drifting to her range. */
	private void steer(@Nullable LivingEntity target, double gap) {
		double up = gap == Double.NEGATIVE_INFINITY ? -Ow.speed(DESCENT)
			: Math.clamp(gap * HEIGHT_EASE, -Ow.speed(DESCENT), Ow.speed(HOVER_RISE));
		Vec3 flat = Vec3.ZERO;
		if (target != null) {
			Vec3 to = target.position().subtract(position());
			to = new Vec3(to.x, 0, to.z);
			double d = to.length();
			if (d > 1.0E-3) {
				double way = d > far() ? 1 : d < near() ? -1 : 0;
				flat = to.scale(way / d * Ow.speed(EnemyHeroStats.PHARAH_DRIFT));
			}
		}
		setDeltaMovement(flat.x, up, flat.z);
		needsSync = true;
		resetFallDistance();
	}

	/** How far the ground is below her (m), up to 10 (ours: past that, she's high enough to come down). */
	private double heightAboveGround() {
		Vec3 from = position();
		HitResult hit = level().clip(new ClipContext(from, from.add(0, -10, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
		return hit.getType() == HitResult.Type.MISS ? 10 : from.y - hit.getLocation().y;
	}

	@Override
	protected void fight(ServerLevel level, LivingEntity target, double distance, boolean sees) {
		if (reload >= 0 || recovery > 0 || !sees) {
			return;
		}
		// at the feet, for the splash (ours, as a Pharah player aims)
		Vec3 aim = target.position().add(0, 0.3, 0);
		Vec3 look = aim.subtract(getEyePosition()).normalize();
		Vec3 from = fromEyes(look, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Vec3 dir = dev.overcraft.sombra.Sombra.cone(aim.subtract(from).normalize(), spread(0), random);
		PharahRocketEntity rocket = new PharahRocketEntity(this, level, PharahRocketEntity.Kind.ROCKET);
		rocket.launch(from, dir, Ow.speed(ROCKET_SPEED));
		level.addFreshEntity(rocket);
		ammo--;
		recovery = Ow.ticks(ammo <= 0 ? RECOVERY_LAST : RECOVERY);
		Effects.play(level, "pharah_launch", from, dir, this, 0, this);
		Combat.animate(this, "fire");
		play(level, "rocket.fire", from, 80);
		if (ammo <= 0) {
			reload = 0;
			Combat.animate(this, "reload");
			play(level, "reload", getEyePosition(), 30);
		}
	}
}
