package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Headshot;
import dev.overcraft.hero.Hero;
import dev.overcraft.widowmaker.Widowmaker;
import dev.overcraft.widowmaker.WidowmakerSounds;
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

import static dev.overcraft.widowmaker.WidowmakerStats.AMMO;
import static dev.overcraft.widowmaker.WidowmakerStats.HEALTH;
import static dev.overcraft.widowmaker.WidowmakerStats.MUZZLE_AHEAD;
import static dev.overcraft.widowmaker.WidowmakerStats.MUZZLE_RIGHT;
import static dev.overcraft.widowmaker.WidowmakerStats.MUZZLE_UP;
import static dev.overcraft.widowmaker.WidowmakerStats.RELOAD;
import static dev.overcraft.widowmaker.WidowmakerStats.SCOPED_COST;
import static dev.overcraft.widowmaker.WidowmakerStats.SCOPED_RADIUS;
import static dev.overcraft.widowmaker.WidowmakerStats.SCOPED_RECOVERY;

/**
 * Enemy Widowmaker (#46, batch 2): she keeps 20 to 40 m (ours) and takes scoped shots. Each is aimed for 1.5 s first (ours: time
 * for a player to take cover), shown to everyone near by her red sight line from her rifle to where she aims ("widow_enemy_aim",
 * ours: Overwatch shows no line, only her scope's glint); the aim follows the target, then holds still its last 0.25 s (ours), so a
 * sidestep then is missed. The shot is a full charge's hitscan (120, its 50-70 m falloff to half, x2.5 on the head: Widowmaker
 * .scopedDamage) at half (ours: EnemyHeroStats.WIDOW_DAMAGE_SHARE), then the difficulty's factor on a player; 5 rounds a shot of
 * her 35, her 1.5 s reload, her 0.5 s recovery and 1 s more (ours) before the next aim (WidowmakerStats). Her aim's spread is half
 * the difficulty's (ours: a sniper). No hip fire, Grappling Hook, Venom Mine or Infra-Sight. Drops string (ours).
 */
public class EnemyWidowmaker extends EnemyHeroEntity {
	/** Her shot reaches 80 m (ours; hers 200 m, WidowmakerStats.RANGE; her sight 48 m). */
	private static final double REACH = 80;

	private int ammo = AMMO;
	private int shotWait = Ow.ticks(EnemyHeroStats.WIDOW_BETWEEN);
	private int reload = -1;
	/** Ticks into her aim (-1: not aiming), and where it points. */
	private int aiming = -1;
	private Vec3 aimAt = Vec3.ZERO;

	public EnemyWidowmaker(EntityType<? extends EnemyWidowmaker> type, Level level) {
		super(type, level);
	}

	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return attributes(HEALTH, dev.overcraft.hero.OwMovement.RUN_SPEED, EnemyHeroStats.WIDOW_FOLLOW_RANGE);
	}

	@Override
	public Hero hero() {
		return Hero.WIDOWMAKER;
	}

	@Override
	protected double near() {
		return EnemyHeroStats.WIDOW_NEAR;
	}

	@Override
	protected double far() {
		return EnemyHeroStats.WIDOW_FAR;
	}

	@Override
	protected Item dropItem() {
		return Items.STRING;
	}

	@Override
	protected @Nullable SoundEvent sound(String name) {
		return thirdPerson(WidowmakerSounds::of, name);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			return;
		}
		if (reload >= 0 && ++reload >= Ow.ticks(RELOAD)) {
			reload = -1;
			ammo = AMMO;
		}
	}

	/** A new target or none: her aim starts over, so every target gets the whole 1.5 s warning (the critique of 29c1797). */
	@Override
	protected void targetChanged() {
		aiming = -1;
	}

	@Override
	protected void fight(ServerLevel level, LivingEntity target, double distance, boolean sees) {
		if (shotWait > 0) {
			shotWait--;
		}
		if (reload >= 0 || !sees || distance > REACH) {
			// out of sight the aim is lost: the next starts over (a player behind cover is safe)
			aiming = -1;
			return;
		}
		if (aiming < 0) {
			if (shotWait > 0) {
				return;
			}
			aiming = 0;
			aimAt = bodyOf(target);
			Combat.animate(this, "ads_in");
			play(level, "ads.in", getEyePosition(), 30);
		}
		if (EnemyHeroStats.widowTracks(aiming)) {
			aimAt = bodyOf(target);
		}
		Vec3 eye = getEyePosition();
		Vec3 look = aimAt.subtract(eye).normalize();
		if (aiming % 2 == 0) {
			// her sight line to where she aims, brighter once it holds still (variant 1): WidowmakerFx, ours
			Vec3 from = fromEyes(look, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
			Traced t = trace(level, eye, look, REACH, 0);
			Effects.play(level, "widow_enemy_aim", from, t.at().subtract(from), this, EnemyHeroStats.widowTracks(aiming) ? 0 : 1, this);
		}
		if (++aiming < Ow.ticks(EnemyHeroStats.WIDOW_AIM)) {
			return;
		}
		aiming = -1;
		shot(level, look);
		ammo -= SCOPED_COST;
		shotWait = Ow.ticks(SCOPED_RECOVERY + EnemyHeroStats.WIDOW_BETWEEN);
		if (ammo < SCOPED_COST) {
			reload = 0;
			Combat.animate(this, "reload");
			play(level, "reload", getEyePosition(), 30);
		}
	}

	/** Her scoped shot along {@code look} (her Widowmaker.shot, cut down: no pierce, no Sharpshooter). */
	private void shot(ServerLevel level, Vec3 look) {
		Vec3 eye = getEyePosition();
		Vec3 dir = dev.overcraft.sombra.Sombra.cone(look, spread(0) / 2, random);
		Traced t = trace(level, eye, dir, REACH, SCOPED_RADIUS);
		Vec3 from = fromEyes(look, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Effects.play(level, "widow_shot", from, t.at().subtract(from), this, Widowmaker.SHOT_FULL, this);
		if (t.target() != null) {
			boolean crit = Headshot.crossed(t.target(), t.at(), dir, SCOPED_RADIUS);
			deal(t.target(), shotSource(null), EnemyHeroStats.widowShot(t.metres(), crit), Blow.weapon("widow_s_kiss", crit));
			Effects.play(level, "widow_hit", t.at(), dir.scale(-1), t.target(), Widowmaker.HIT_SCOPED, this);
		} else if (t.block().getType() == HitResult.Type.BLOCK) {
			play(level, "kiss.hit_world", t.at(), 32);
			Effects.play(level, "widow_hit", t.at(), Vec3.atLowerCornerOf(t.block().getDirection().getUnitVec3i()), null, Widowmaker.HIT_BLOCK, this);
		}
		play(level, "kiss.fire_ads", eye, 200);
		Combat.animate(this, "ads_fire");
	}
}
