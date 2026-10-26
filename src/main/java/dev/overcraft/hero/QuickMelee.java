package dev.overcraft.hero;

import dev.overcraft.Ow;
import dev.overcraft.registry.OcSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Quick melee, every hero's (key V): 40 damage to every enemy within 2.5 m in front, a very small knockback, 1 s
 * cooldown from the press, and 0.528 s recovery during which the weapons can't fire (Overwatch wiki, "Melee").
 */
public final class QuickMelee {
	public static final double DAMAGE = 40;
	public static final double COOLDOWN = 1.0;
	public static final double RECOVERY = 0.528;
	public static final double RANGE = 2.5;
	/** Ours: how wide in front it reaches (the wiki gives only the range). */
	public static final double HALF_ANGLE = 45;
	/** Ours: "a very small knockback" (Minecraft knockback strength; a sword's is 0.4). */
	public static final double KNOCKBACK = 0.15;
	private static final String[] CLIPS = {"melee", "melee_2", "melee_3"};

	private QuickMelee() {
	}

	public static void tick(HeroState s) {
		s.meleeCooldown = Math.max(0, s.meleeCooldown - 1);
	}

	public static void use(ServerPlayer hero, HeroState s) {
		if (s.meleeCooldown > 0) {
			return;
		}
		s.meleeCooldown = Ow.ticks(COOLDOWN);
		s.weaponRecovery = Math.max(s.weaponRecovery, Ow.ticks(RECOVERY));
		ServerLevel level = hero.level();
		Vec3 eye = hero.getEyePosition();
		Vec3 look = hero.getViewVector(1.0f);
		double cosLimit = Math.cos(Math.toRadians(HALF_ANGLE));
		boolean hit = false;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(RANGE + 1))) {
			if (!Allies.canHurt(hero, target)) {
				continue;
			}
			// the nearest point of the body counts, so a melee at the feet or the head reaches
			AABB box = target.getBoundingBox();
			Vec3 point = new Vec3(Math.clamp(eye.x, box.minX, box.maxX), Math.clamp(eye.y, box.minY, box.maxY),
				Math.clamp(eye.z, box.minZ, box.maxZ));
			Vec3 to = point.subtract(eye);
			double dist = to.length();
			Vec3 toCenter = box.getCenter().subtract(eye).normalize();
			if (dist > RANGE || (dist > 0.3 && toCenter.dot(look) < cosLimit)) {
				continue;
			}
			if (level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, hero)).getType() != HitResult.Type.MISS) {
				continue;
			}
			DamageSource source = hero.damageSources().playerAttack(hero);
			float dealt = Combat.damage(target, source, DAMAGE);
			Combat.addUltCharge(hero, dealt);
			Combat.addFireForDamage(hero, dealt, Ow.toOw(target.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(hero, target, false);
			}
			// (minus the look: Minecraft's knockback takes the direction towards the attacker)
			target.knockback(KNOCKBACK, -look.x, -look.z, source, Ow.hp(dealt));
			hit = true;
		}
		Combat.animate(hero, CLIPS[hero.getRandom().nextInt(CLIPS.length)]);
		Voice.say(hero, "melee", 0.8f, false);
		level.playSound(null, hero.getX(), hero.getY(), hero.getZ(),
			hit ? OcSounds.MELEE_HIT : OcSounds.MELEE_SWING, SoundSource.PLAYERS, 1, 1);
	}
}
