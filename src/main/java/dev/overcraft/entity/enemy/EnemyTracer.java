package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.Headshot;
import dev.overcraft.tracer.Tracer;
import dev.overcraft.tracer.TracerSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static dev.overcraft.tracer.TracerStats.AMMO;
import static dev.overcraft.tracer.TracerStats.AMMO_PER_SHOT;
import static dev.overcraft.tracer.TracerStats.BLINK_CHARGES;
import static dev.overcraft.tracer.TracerStats.BLINK_DISTANCE;
import static dev.overcraft.tracer.TracerStats.BLINK_RECHARGE;
import static dev.overcraft.tracer.TracerStats.BLINK_STEP;
import static dev.overcraft.tracer.TracerStats.BULLETS;
import static dev.overcraft.tracer.TracerStats.FALLOFF_END;
import static dev.overcraft.tracer.TracerStats.HEALTH;
import static dev.overcraft.tracer.TracerStats.MUZZLE_AHEAD;
import static dev.overcraft.tracer.TracerStats.MUZZLE_SIDE;
import static dev.overcraft.tracer.TracerStats.MUZZLE_UP;
import static dev.overcraft.tracer.TracerStats.RADIUS;
import static dev.overcraft.tracer.TracerStats.RANGE;
import static dev.overcraft.tracer.TracerStats.RELOAD;
import static dev.overcraft.tracer.TracerStats.RUN_SPEED;
import static dev.overcraft.tracer.TracerStats.SPREAD_MAX;

/**
 * Enemy Tracer (#46): Pulse Pistols in bursts of 5 shots (10 bullets, 5.75 each with her falloff and x2 on the head: Tracer.bulletDamage)
 * over 0.5 s, then a pause; her 40-round magazine and 1 s reload (TracerStats). Blink (7.5 m, 3 charges, 3 s each: TracerStats) to
 * close in from past 12 m, or sideways when she's hit (ours: half the time). No Recall, no Pulse Bomb. Drops redstone (ours).
 */
public class EnemyTracer extends EnemyHeroEntity {
	/** Ours: at least 1 s between her Blinks, so she doesn't spend all three at once. */
	private static final int BLINK_GAP = 20;

	private int shots = AMMO / AMMO_PER_SHOT;
	private int burstLeft;
	private int shotWait;
	private int pauseLeft;
	private int reload = -1;
	private int blinks = BLINK_CHARGES;
	private int blinkRecharge;
	private int blinkWait;

	public EnemyTracer(EntityType<? extends EnemyTracer> type, Level level) {
		super(type, level);
	}

	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return attributes(HEALTH, RUN_SPEED);
	}

	@Override
	public Hero hero() {
		return Hero.TRACER;
	}

	@Override
	protected double near() {
		return EnemyHeroStats.TRACER_NEAR;
	}

	@Override
	protected double far() {
		return EnemyHeroStats.TRACER_FAR;
	}

	@Override
	protected Item dropItem() {
		return Items.REDSTONE;
	}

	@Override
	protected @Nullable SoundEvent sound(String name) {
		return thirdPerson(TracerSounds::of, name);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			return;
		}
		// her charges come back one at a time, 3 s each (TracerStats.BLINK_RECHARGE), in a fight or out
		if (blinks < BLINK_CHARGES && ++blinkRecharge >= Ow.ticks(BLINK_RECHARGE)) {
			blinks++;
			blinkRecharge = 0;
		}
		if (blinkWait > 0) {
			blinkWait--;
		}
		if (reload >= 0 && ++reload >= Ow.ticks(RELOAD)) {
			reload = -1;
			shots = AMMO / AMMO_PER_SHOT;
		}
	}

	@Override
	protected void fight(ServerLevel level, LivingEntity target, double distance, boolean sees) {
		if (sees && distance > EnemyHeroStats.TRACER_BLINK_IN && random.nextInt(10) == 0) {
			blink(level, bodyOf(target).subtract(position()));
		}
		if (reload >= 0) {
			return;
		}
		if (pauseLeft > 0) {
			pauseLeft--;
			return;
		}
		if (shotWait > 0) {
			shotWait--;
			return;
		}
		if (!sees || distance > FALLOFF_END) {
			burstLeft = 0;
			return;
		}
		if (burstLeft <= 0) {
			burstLeft = EnemyHeroStats.TRACER_BURST_SHOTS;
		}
		volley(level, target);
		shots--;
		burstLeft--;
		shotWait = EnemyHeroStats.TRACER_SHOT_TICKS - 1;
		if (shots <= 0) {
			reload = 0;
			burstLeft = 0;
			Combat.animate(this, "reload");
			play(level, "reload", getEyePosition(), 45);
		} else if (burstLeft <= 0) {
			pauseLeft = pause();
			play(level, "pistol.fire_end", getEyePosition(), 100);
		}
	}

	/** One shot: a bullet from each pistol (her Tracer.volley, cut down: no bloom, the spread the plan's). */
	private void volley(ServerLevel level, LivingEntity target) {
		Vec3 eye = getEyePosition();
		Vec3 look = bodyOf(target).subtract(eye).normalize();
		Map<LivingEntity, double[]> hits = new LinkedHashMap<>();
		Map<LivingEntity, Boolean> crits = new LinkedHashMap<>();
		for (int i = 0; i < BULLETS; i++) {
			Vec3 dir = dev.overcraft.sombra.Sombra.cone(look, spread(SPREAD_MAX), random);
			Traced t = trace(level, eye, dir, RANGE, RADIUS, m -> Tracer.bulletDamage(m, false));
			Vec3 muzzle = fromEyes(look, i == 0 ? -MUZZLE_SIDE : MUZZLE_SIDE, MUZZLE_UP, MUZZLE_AHEAD);
			Effects.play(level, "tracer_shot", muzzle, t.at().subtract(muzzle), this, 0, this);
			if (t.target() != null) {
				Effects.play(level, "tracer_hit", t.at(), Vec3.ZERO, this, 0, this);
				boolean crit = Headshot.crossed(t.target(), t.at(), dir, RADIUS);
				double[] h = hits.computeIfAbsent(t.target(), k -> new double[2]);
				h[0] += Tracer.bulletDamage(t.metres(), crit);
				h[1]++;
				crits.merge(t.target(), crit, Boolean::logicalOr);
			} else if (t.block().getType() == HitResult.Type.BLOCK) {
				Effects.play(level, "tracer_hit_block", t.at(), Vec3.atLowerCornerOf(t.block().getDirection().getUnitVec3i()), this, 0, this);
			}
		}
		play(level, "pistol.fire", eye, 200);
		Combat.animate(this, "fire");
		for (Map.Entry<LivingEntity, double[]> e : hits.entrySet()) {
			// a shot's bullets on one creature summed into one hit, armor taking it as its bullets (as her own volley)
			dev.overcraft.hero.Shields.pellets = (int) e.getValue()[1];
			try {
				deal(e.getKey(), shotSource(null), e.getValue()[0], Blow.weapon(crits.get(e.getKey())));
			} finally {
				dev.overcraft.hero.Shields.pellets = 1;
			}
			play(level, "pistol.hit_enemy", e.getKey().getEyePosition(), 32);
		}
	}

	/** Hit: half the time a Blink to one side (ours), to dodge as a Tracer does. */
	@Override
	protected void hurtBy(ServerLevel level, LivingEntity attacker) {
		if (random.nextBoolean()) {
			Vec3 toward = attacker.position().subtract(position());
			Vec3 side = new Vec3(-toward.z, 0, toward.x).scale(random.nextBoolean() ? 1 : -1);
			blink(level, side);
		}
	}

	/**
	 * Blink: up to 7.5 m along {@code way} (flat), in 1.5 m steps while each is clear of blocks (her Blink's own steps:
	 * TracerStats.BLINK_STEP), a step up or down a block allowed. Takes a charge.
	 */
	/**
	 * Her box clear every 0.3 m from {@code a} to {@code b}: a 1.5 m step checked only at its end jumped a shut door, a pane or a fence
	 * (the critique of c728bea).
	 */
	private boolean clearBetween(ServerLevel level, Vec3 a, Vec3 b) {
		double length = b.distanceTo(a);
		for (double d = 0.3; d < length; d += 0.3) {
			Vec3 at = a.add(b.subtract(a).scale(d / length));
			if (!level.noCollision(this, getBoundingBox().move(at.subtract(position())))) {
				return false;
			}
		}
		return true;
	}

		private void blink(ServerLevel level, Vec3 way) {
		Vec3 flat = new Vec3(way.x, 0, way.z);
		// not while hacked: no abilities (Sombra's Hack; the critique of #46: a hacked Tracer still Blinked in her fight)
		if (blinks <= 0 || blinkWait > 0 || flat.lengthSqr() < 1.0E-6 || isPassenger() || dev.overcraft.sombra.Hack.silenced(this)) {
			return;
		}
		flat = flat.normalize();
		Vec3 from = position();
		Vec3 to = from;
		for (double d = BLINK_STEP; d <= BLINK_DISTANCE + 1.0E-6; d += BLINK_STEP) {
			Vec3 next = null;
			for (double dy : new double[] {0, 1, -1}) {
				Vec3 at = from.add(flat.scale(d)).add(0, to.y - from.y + dy, 0);
				AABB box = getBoundingBox().move(at.subtract(position()));
				if (level.noCollision(this, box)) {
					next = at;
					break;
				}
			}
			if (next == null || !clearBetween(level, to, next)) {
				break;
			}
			to = next;
		}
		if (to.distanceToSqr(from) < 1) {
			return;
		}
		blinks--;
		blinkWait = BLINK_GAP;
		Effects.play(level, "tracer_blink", from, Vec3.ZERO, this, 0, this);
		play(level, "blink_1", from, 70);
		Combat.animate(this, "blink");
		teleportTo(to.x, to.y, to.z);
		resetFallDistance();
		getNavigation().stop();
	}
}
