package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.entity.RailgunShotEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Headshot;
import dev.overcraft.hero.Hero;
import dev.overcraft.sojourn.Railgun;
import dev.overcraft.sojourn.SojournSounds;
import dev.overcraft.widowmaker.Widowmaker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.sojourn.SojournStats.CHARGED_CRIT;
import static dev.overcraft.sojourn.SojournStats.CHARGED_FALLOFF_END;
import static dev.overcraft.sojourn.SojournStats.CHARGED_FALLOFF_MIN;
import static dev.overcraft.sojourn.SojournStats.CHARGED_FALLOFF_START;
import static dev.overcraft.sojourn.SojournStats.CHARGED_LOCKOUT;
import static dev.overcraft.sojourn.SojournStats.CHARGED_RADIUS;
import static dev.overcraft.sojourn.SojournStats.CHARGED_RANGE;
import static dev.overcraft.sojourn.SojournStats.ENERGY_CRIT;
import static dev.overcraft.sojourn.SojournStats.ENERGY_HIT;
import static dev.overcraft.sojourn.SojournStats.ENERGY_MAX;
import static dev.overcraft.sojourn.SojournStats.HEALTH;
import static dev.overcraft.sojourn.SojournStats.RAILGUN_AMMO;
import static dev.overcraft.sojourn.SojournStats.RAILGUN_CRIT;
import static dev.overcraft.sojourn.SojournStats.RAILGUN_DAMAGE;
import static dev.overcraft.sojourn.SojournStats.RAILGUN_RATE;
import static dev.overcraft.sojourn.SojournStats.RAILGUN_SPEED;
import static dev.overcraft.sojourn.SojournStats.RELOAD;
import static dev.overcraft.sojourn.SojournStats.SPREAD_MAX;

/**
 * Enemy Sojourn (#46, batch 2): her Railgun's rounds (her own RailgunShotEntity: 9 a hit, x2 on the head, 150 m/s, no falloff) in
 * bursts of 8 at her 16 a second (the burst ours), 45 to the magazine and her 1.2 s reload (SojournStats). Each round that hits a
 * creature it may hurt gives her energy, +5 or +10 on a crit, held 9 s and then lost at 15 a second (SojournStats); at 60 (ours) she
 * fires her Charged Shot, hitscan, 20 + energy (Railgun's rule: EnemyHeroStats.sojournCharged), its 40-60 m falloff to half, x1.5 on
 * the head, then her 0.65 s lockout (SojournStats). It takes the first creature on its line (hers goes through all: ours, cut down).
 * No Power Slide, Disruptor Shot or Overclock. Drops glowstone dust (ours).
 */
public class EnemySojourn extends EnemyHeroEntity {
	/** Her rounds leave from ahead of her eyes (ours; a player's from her eyes, Railgun.shoot). */
	private static final double MUZZLE_RIGHT = 0.2, MUZZLE_UP = -0.15, MUZZLE_AHEAD = 0.6;

	private int ammo = RAILGUN_AMMO;
	private double recovery;
	private int burstLeft;
	private int pauseLeft;
	private int reload = -1;
	private double energy;
	private int sinceGain;
	private int lockout;

	public EnemySojourn(EntityType<? extends EnemySojourn> type, Level level) {
		super(type, level);
	}

	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return attributes(HEALTH, dev.overcraft.hero.OwMovement.RUN_SPEED);
	}

	@Override
	public Hero hero() {
		return Hero.SOJOURN;
	}

	@Override
	protected double near() {
		return EnemyHeroStats.SOJOURN_NEAR;
	}

	@Override
	protected double far() {
		return EnemyHeroStats.SOJOURN_FAR;
	}

	@Override
	protected Item dropItem() {
		return Items.GLOWSTONE_DUST;
	}

	@Override
	protected @Nullable SoundEvent sound(String name) {
		return thirdPerson(SojournSounds::of, name);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			return;
		}
		recovery = Math.max(0, recovery - 1);
		if (lockout > 0) {
			lockout--;
		}
		energy = EnemyHeroStats.sojournEnergyTick(energy, sinceGain);
		if (sinceGain < Integer.MAX_VALUE) {
			sinceGain++;
		}
		if (reload >= 0 && ++reload >= Ow.ticks(RELOAD)) {
			reload = -1;
			ammo = RAILGUN_AMMO;
		}
	}

	@Override
	protected void fight(ServerLevel level, LivingEntity target, double distance, boolean sees) {
		if (reload >= 0 || lockout > 0) {
			return;
		}
		if (pauseLeft > 0) {
			pauseLeft--;
			return;
		}
		if (!sees) {
			burstLeft = 0;
			return;
		}
		if (burstLeft <= 0 && energy >= EnemyHeroStats.SOJOURN_CHARGE_AT) {
			chargedShot(level, target);
			return;
		}
		if (recovery > 0) {
			return;
		}
		if (burstLeft <= 0) {
			burstLeft = EnemyHeroStats.SOJOURN_BURST;
		}
		round(level, target, burstLeft == EnemyHeroStats.SOJOURN_BURST);
		ammo--;
		burstLeft--;
		// 16 a second (SojournStats.RAILGUN_RATE), the fraction kept
		recovery += 20 / RAILGUN_RATE;
		if (ammo <= 0) {
			reload = 0;
			burstLeft = 0;
			Combat.animate(this, "reload");
			play(level, "railgun.reload", getEyePosition(), 30);
		} else if (burstLeft <= 0) {
			pauseLeft = pause();
		}
	}

	/** One round at {@code target}, its spread the Railgun's widest (SojournStats.SPREAD_MAX) and the difficulty's. */
	private void round(ServerLevel level, LivingEntity target, boolean first) {
		Vec3 eye = getEyePosition();
		Vec3 look = bodyOf(target).subtract(eye).normalize();
		Vec3 from = fromEyes(look, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Vec3 dir = dev.overcraft.sombra.Sombra.cone(bodyOf(target).subtract(from).normalize(), spread(SPREAD_MAX), random);
		RailgunShotEntity shot = new RailgunShotEntity(this, level);
		shot.launch(from, dir, Ow.speed(RAILGUN_SPEED));
		level.addFreshEntity(shot);
		play(level, first ? "railgun.fire_first" : "railgun.fire", eye, 100);
		play(level, "railgun.projectile", eye, 60);
		Combat.animate(this, "fire");
	}

	/** One of her rounds hit {@code entity} at {@code at} (RailgunShotEntity): 9, x2 on the head, and her energy for it. */
	public void roundHit(RailgunShotEntity shot, Entity entity, Vec3 at) {
		if (!(level() instanceof ServerLevel level) || !(entity instanceof LivingEntity target) || !target.isAlive()
			|| !Allies.canHurt(this, target)) {
			return;
		}
		boolean crit = Headshot.crossed(target, at, shot.getDeltaMovement(), shot.hitRadius());
		float dealt = deal(target, shotSource(shot), RAILGUN_DAMAGE * (crit ? RAILGUN_CRIT : 1), Blow.weapon(crit));
		play(level, "railgun.hit_enemy", at, 60);
		Effects.play(level, "sojourn_round_hit", at, Vec3.ZERO, null, 1, this);
		if (dealt > 0) {
			// energy from the hit, as hers (Railgun.hit): +5, +10 a crit, up to 100
			energy = Math.min(ENERGY_MAX, energy + (crit ? ENERGY_CRIT : ENERGY_HIT));
			sinceGain = 0;
		}
	}

	/** One of her rounds stopped on a block (RailgunShotEntity): its look on the face (Railgun.hitBlock's, without a player). */
	public void roundHitBlock(Vec3 at, Vec3 normal) {
		if (level() instanceof ServerLevel level) {
			play(level, "railgun.hit_block", at, 90);
			Effects.play(level, "sojourn_round_hit", at, normal, null, 0, this);
		}
	}

	/** Her Charged Shot (Railgun.chargedShot, cut down: the first creature on its line only), spending all her energy. */
	private void chargedShot(ServerLevel level, LivingEntity target) {
		double spent = energy;
		energy = 0;
		lockout = Ow.ticks(CHARGED_LOCKOUT);
		Vec3 eye = getEyePosition();
		Vec3 look = bodyOf(target).subtract(eye).normalize();
		// one aimed shot: no spread but the difficulty's (hers has none, SojournStats)
		Vec3 dir = dev.overcraft.sombra.Sombra.cone(look, spread(0), random);
		Traced t = trace(level, eye, dir, CHARGED_RANGE, CHARGED_RADIUS,
			m -> EnemyHeroStats.sojournCharged(spent) * Widowmaker.falloff(m, CHARGED_FALLOFF_START, CHARGED_FALLOFF_END, CHARGED_FALLOFF_MIN));
		Vec3 from = fromEyes(look, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Effects.play(level, "sojourn_tracer", from, t.at().subtract(from), this, (int) Math.round(spent), this);
		play(level, Railgun.chargedFireSound(spent), eye, 100);
		play(level, "charged.shot", eye, 60);
		Combat.animate(this, "charged_shot");
		if (t.target() != null) {
			boolean crit = Headshot.crossed(t.target(), t.at(), dir, CHARGED_RADIUS);
			double damage = EnemyHeroStats.sojournCharged(spent)
				* Widowmaker.falloff(t.metres(), CHARGED_FALLOFF_START, CHARGED_FALLOFF_END, CHARGED_FALLOFF_MIN) * (crit ? CHARGED_CRIT : 1);
			deal(t.target(), shotSource(null), damage, Blow.weapon("charged_shot", crit));
			play(level, "charged.hit_enemy", t.at(), 60);
			Effects.play(level, "sojourn_charged_hit", t.at(), Vec3.ZERO, null, 1, this);
		} else if (t.block().getType() == HitResult.Type.BLOCK) {
			play(level, "charged.hit_block", t.at(), 90);
			Effects.play(level, "sojourn_charged_hit", t.at(), Vec3.atLowerCornerOf(t.block().getDirection().getUnitVec3i()), null, 0, this);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putDouble("SojournEnergy", energy);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		energy = input.getDoubleOr("SojournEnergy", 0);
	}
}
