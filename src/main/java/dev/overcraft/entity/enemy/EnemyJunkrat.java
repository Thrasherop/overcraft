package dev.overcraft.entity.enemy;

import dev.overcraft.Ow;
import dev.overcraft.entity.FragEntity;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.junkrat.JunkratSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.junkrat.JunkratStats.AMMO;
import static dev.overcraft.junkrat.JunkratStats.FRAG_FUSE;
import static dev.overcraft.junkrat.JunkratStats.FRAG_GRAVITY;
import static dev.overcraft.junkrat.JunkratStats.FRAG_INTERVAL;
import static dev.overcraft.junkrat.JunkratStats.FRAG_SPEED;
import static dev.overcraft.junkrat.JunkratStats.HEALTH;
import static dev.overcraft.junkrat.JunkratStats.RELOAD;

/**
 * Enemy Junkrat (#46): his Frag Launcher's grenades (his own FragEntity: 30 m/s, 17 m/s² fall, the bounces and fuse), lobbed at the
 * target's feet on the low arc that lands there, one each 0.667 s, five then his 1.5 s reload (JunkratStats). The burst is his
 * numbers (45 direct, 80 splash to 2 m and its push: EnemyBlasts.frag). No mines, trap or tire. Drops gunpowder (ours).
 */
public class EnemyJunkrat extends EnemyHeroEntity {
	/** Where his frags leave from, ahead and right of his eyes (ours: his launcher's muzzle, near enough). */
	private static final double MUZZLE_RIGHT = 0.25, MUZZLE_UP = -0.25, MUZZLE_AHEAD = 0.6;

	private int ammo = AMMO;
	private double recovery;
	private int reload = -1;

	public EnemyJunkrat(EntityType<? extends EnemyJunkrat> type, Level level) {
		super(type, level);
	}

	public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
		return attributes(HEALTH, dev.overcraft.hero.OwMovement.RUN_SPEED);
	}

	@Override
	public Hero hero() {
		return Hero.JUNKRAT;
	}

	@Override
	protected double near() {
		return EnemyHeroStats.JUNKRAT_NEAR;
	}

	@Override
	protected double far() {
		return EnemyHeroStats.JUNKRAT_FAR;
	}

	@Override
	protected Item dropItem() {
		return Items.GUNPOWDER;
	}

	@Override
	protected @Nullable SoundEvent sound(String name) {
		return thirdPerson(JunkratSounds::of, name);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			return;
		}
		recovery = Math.max(0, recovery - 1);
		if (reload >= 0 && ++reload >= Ow.ticks(RELOAD)) {
			reload = -1;
			ammo = AMMO;
		}
	}

	@Override
	protected void fight(ServerLevel level, LivingEntity target, double distance, boolean sees) {
		if (reload >= 0 || recovery > 0 || !sees) {
			return;
		}
		Vec3 feet = target.position().add(0, 0.1, 0);
		Vec3 look = feet.subtract(getEyePosition()).normalize();
		Vec3 from = fromEyes(look, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Vec3 dir = EnemyHeroStats.lob(feet.subtract(from), Ow.speed(FRAG_SPEED), Ow.accel(FRAG_GRAVITY));
		dir = dev.overcraft.sombra.Sombra.cone(dir, spread(0), random);
		FragEntity frag = new FragEntity(this, level, false, Ow.ticks(FRAG_FUSE));
		frag.launch(from, dir, Ow.speed(FRAG_SPEED));
		level.addFreshEntity(frag);
		ammo--;
		// 0.667 s to the next (JunkratStats.FRAG_INTERVAL), the fraction kept as his own does
		recovery += FRAG_INTERVAL * 20;
		Combat.animate(this, "fire");
		play(level, "frag.fire", from, 80);
		Effects.play(level, "jr_frag_launch", from, frag.getDeltaMovement(), this, 0, this);
		if (ammo <= 0) {
			reload = 0;
			Combat.animate(this, "reload");
			play(level, "reload", getEyePosition(), 30);
			Effects.play(level, "jr_reload", getEyePosition(), Vec3.ZERO, this, Ow.ticks(RELOAD), this);
		}
	}

}
