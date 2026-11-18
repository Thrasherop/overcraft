package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.ashe.Ashe;
import dev.overcraft.ashe.AsheSounds;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Headshot;
import dev.overcraft.hero.Hero;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static dev.overcraft.ashe.AsheStats.AMMO;
import static dev.overcraft.ashe.AsheStats.COACH_COOLDOWN;
import static dev.overcraft.ashe.AsheStats.COACH_FIRE_AT;
import static dev.overcraft.ashe.AsheStats.COACH_PELLET;
import static dev.overcraft.ashe.AsheStats.COACH_PELLET_RADIUS;
import static dev.overcraft.ashe.AsheStats.COACH_PUSH;
import static dev.overcraft.ashe.AsheStats.COACH_PUSH_UP;
import static dev.overcraft.ashe.AsheStats.COACH_RANGE;
import static dev.overcraft.ashe.AsheStats.COACH_SPREAD;
import static dev.overcraft.ashe.AsheStats.HEALTH;
import static dev.overcraft.ashe.AsheStats.MUZZLE_AHEAD;
import static dev.overcraft.ashe.AsheStats.MUZZLE_RIGHT;
import static dev.overcraft.ashe.AsheStats.MUZZLE_UP;
import static dev.overcraft.ashe.AsheStats.RADIUS;

/**
 * Enemy Ashe (#46, batch 2): The Viper at range, scoped-style: she aims down the sights 0.5 s (ours: her 0.2 s scope-in and a settle),
 * then one hitscan shot of the scoped Viper's (75, its 35-55 m falloff to x0.3, x2 on the head: Ashe.viperDamage), one each 1.2 s
 * (ours); twelve rounds, then her reload from empty, 3.25 s (AsheStats, Ashe.reloadSeconds). Coach Gun when her target is within
 * 5 m (ours): its 15 pellets of 6 in its rings (Ashe.pellets), its push on whoever they hit (20 m/s along her aim and 5 up, full
 * to 8 m: Ashe.coachPushShare) and her own 18 m/s back (Ashe.coachSelf), every 10 s (AsheStats). No Dynamite, no B.O.B. Drops
 * gold nuggets (ours).
 */
public class EnemyAshe extends EnemyHeroEntity {
	/** Hitscan out to 60 m (ours, as enemy Cassidy's; hers is 200 m: AsheStats.RANGE). */
	private static final double REACH = 60;
	private static final Blow COACH_BLOW = Blow.ability("coach_gun");

	private int ammo = AMMO;
	private int shotWait = Ow.ticks(EnemyHeroStats.ASHE_SHOT_INTERVAL);
	/** Ticks into her aim down the sights (-1: not aiming). */
	private int aiming = -1;
	private int reload = -1;
	private int coachCooldown;
	/** Ticks into Coach Gun's cast (-1: none): the blast comes 0.165 s in (AsheStats.COACH_FIRE_AT). */
	private int coachCast = -1;
	/** Launched by her own Coach Gun: no fall damage until she lands (ours, as a player hero's softLanding). */
	private boolean launched;
	/** Ticks since that launch, and whether she has left the ground since (EnemyHeroStats.asheLaunchHeld). */
	private int launchedFor;
	private boolean launchAirborne;

	public EnemyAshe(EntityType<? extends EnemyAshe> type, Level level) {
		super(type, level);
	}

	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return attributes(HEALTH, dev.overcraft.hero.OwMovement.RUN_SPEED);
	}

	@Override
	public Hero hero() {
		return Hero.ASHE;
	}

	@Override
	protected double near() {
		return EnemyHeroStats.ASHE_NEAR;
	}

	@Override
	protected double far() {
		return EnemyHeroStats.ASHE_FAR;
	}

	@Override
	protected Item dropItem() {
		return Items.GOLD_NUGGET;
	}

	@Override
	protected @Nullable SoundEvent sound(String name) {
		return thirdPerson(AsheSounds::of, name);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return !launched && super.causeFallDamage(fallDistance, damageModifier, damageSource);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			return;
		}
		if (coachCooldown > 0) {
			coachCooldown--;
		}
		if (launched) {
			launchedFor++;
			launchAirborne |= !onGround();
			launched = EnemyHeroStats.asheLaunchHeld(launchAirborne, onGround(), launchedFor);
		}
		if (reload >= 0 && ++reload >= Ow.ticks(Ashe.reloadSeconds(0))) {
			reload = -1;
			ammo = AMMO;
		}
	}

	/** A new target or none: a Coach Gun not yet fired is dropped and its cooldown given back (ours), and so is her aim. */
	@Override
	protected void targetChanged() {
		if (coachCast >= 0) {
			coachCast = -1;
			coachCooldown = 0;
		}
		aiming = -1;
	}

	@Override
	protected void fight(ServerLevel level, LivingEntity target, double distance, boolean sees) {
		if (shotWait > 0) {
			shotWait--;
		}
		if (coachCast >= 0) {
			if (++coachCast == Ow.ticks(COACH_FIRE_AT)) {
				coachBlast(level, target);
				coachCast = -1;
			}
			return;
		}
		if (EnemyHeroStats.asheCoach(distance, sees, coachCooldown) && onGround()) {
			// the Coach Gun's cast (0.55 s, the blast 0.165 s in: AsheStats); it ends an aim, not the reload's shells (ours: she reloads
			// all at once)
			coachCast = 0;
			aiming = -1;
			coachCooldown = Ow.ticks(COACH_COOLDOWN);
			Combat.animate(this, "coach_gun");
			return;
		}
		if (reload >= 0 || !sees || distance > REACH) {
			aiming = -1;
			return;
		}
		if (aiming < 0) {
			if (shotWait > 0) {
				return;
			}
			aiming = 0;
			Combat.animate(this, "ads_in");
			play(level, "ads.in", getEyePosition(), 20);
			return;
		}
		if (++aiming < Ow.ticks(EnemyHeroStats.ASHE_AIM)) {
			return;
		}
		aiming = -1;
		shot(level, target);
		ammo--;
		shotWait = Ow.ticks(EnemyHeroStats.ASHE_SHOT_INTERVAL);
		if (ammo <= 0) {
			reload = 0;
			Combat.animate(this, "reload_start");
			play(level, "reload.start", getEyePosition(), 20);
		}
	}

	/** One scoped Viper shot (her Ashe.shot, cut down: no Dynamite to shoot, no Viper's Sting). */
	private void shot(ServerLevel level, LivingEntity target) {
		Vec3 eye = getEyePosition();
		Vec3 look = bodyOf(target).subtract(eye).normalize();
		// scoped, her shot has no spread (AsheStats, D #668): the difficulty's alone
		Vec3 dir = dev.overcraft.sombra.Sombra.cone(look, spread(0), random);
		Traced t = trace(level, eye, dir, REACH, RADIUS, m -> Ashe.viperDamage(m, true, false));
		Vec3 from = fromEyes(look, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Effects.play(level, "ashe_shot", from, t.at().subtract(from), this, Ashe.SHOT_SCOPED, this);
		if (t.target() != null) {
			boolean crit = Headshot.crossed(t.target(), t.at(), dir, RADIUS);
			deal(t.target(), shotSource(null), Ashe.viperDamage(t.metres(), true, crit), Blow.weapon("the_viper", crit));
			Effects.play(level, "ashe_hit", t.at(), dir.scale(-1), null, Ashe.HIT_CREATURE, this);
		} else if (t.block().getType() == HitResult.Type.BLOCK) {
			play(level, "viper.hit_world_ads", t.at(), 32);
			Effects.play(level, "ashe_hit", t.at(), Vec3.atLowerCornerOf(t.block().getDirection().getUnitVec3i()), null, Ashe.HIT_BLOCK, this);
		}
		play(level, "viper.fire_ads", eye, 200);
		Combat.animate(this, "ads_fire");
	}

	/**
	 * Coach Gun's blast at {@code target} (her Ashe.coachBlast, cut down: no Double-Barreled): the pellets summed on each creature into
	 * one hit (no crit), the push on each, and her own launch back.
	 */
	private void coachBlast(ServerLevel level, LivingEntity target) {
		Vec3 eye = getEyePosition();
		Vec3 look = bodyOf(target).subtract(eye).normalize();
		Map<LivingEntity, Double> hitWith = new LinkedHashMap<>();
		Map<LivingEntity, Double> metres = new LinkedHashMap<>();
		for (Vec3 dir : Ashe.pellets(look, COACH_SPREAD / 2)) {
			Traced t = trace(level, eye, dir, COACH_RANGE, COACH_PELLET_RADIUS, m -> COACH_PELLET);
			if (t.target() == null) {
				if (t.block().getType() == HitResult.Type.BLOCK && random.nextInt(3) == 0) {
					Effects.play(level, "ashe_hit", t.at(), Vec3.atLowerCornerOf(t.block().getDirection().getUnitVec3i()), null,
						Ashe.HIT_PELLET, this);
				}
				continue;
			}
			hitWith.merge(t.target(), COACH_PELLET, Double::sum);
			metres.merge(t.target(), t.metres(), Math::min);
		}
		for (Map.Entry<LivingEntity, Double> e : hitWith.entrySet()) {
			LivingEntity hit = e.getKey();
			// armor takes its 7 off each pellet (Shields.pellets, as the player's Coach Gun)
			dev.overcraft.hero.Shields.pellets = (int) Math.max(1, Math.round(e.getValue() / COACH_PELLET));
			try {
				deal(hit, shotSource(null), e.getValue(), COACH_BLOW);
			} finally {
				dev.overcraft.hero.Shields.pellets = 1;
			}
			play(level, "coach_gun.hit", hit.position(), 40);
			double share = Ashe.coachPushShare(metres.get(hit));
			if (hit.isAlive() && share > 0) {
				Vec3 along = look.add(0, 0.5, 0).normalize();
				Vec3 flat = new Vec3(along.x, 0, along.z);
				flat = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
				Combat.push(hit, flat.scale(Ow.speed(COACH_PUSH * share)).add(0, Ow.speed(COACH_PUSH_UP * share), 0), this,
					COACH_BLOW.by(hero()));
			}
		}
		// her own push back, hit or not (AsheStats: 18 m/s, 0.1 up)
		getNavigation().stop();
		Combat.launch(this, Ashe.coachSelf(look).scale(1 / 20.0), null, COACH_BLOW.by(hero()));
		launched = true;
		launchedFor = 0;
		launchAirborne = false;
		resetFallDistance();
		play(level, "coach_gun.fire", eye, 120);
		Effects.play(level, "ashe_coach", eye.add(look).add(0, -0.2, 0), look, this, 0, this);
	}
}
