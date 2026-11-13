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
	/** The swing's animation: Kiriko's melee clips (AF57 / AFB8 / AFB9) are 0.733 s, Lúcio's (2913 ...) 0.767 s. */
	public static final double ANIMATION = 0.75;
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
		// the swing cuts a reload short, as its animation replaces the reload's
		s.interruptReload(ANIMATION);
		ServerLevel level = hero.level();
		Vec3 eye = hero.getEyePosition();
		Vec3 look = hero.getViewVector(1.0f);
		double cosLimit = Math.cos(Math.toRadians(HALF_ANGLE));
		boolean hit = false;
		// Jetpack Cat's Claws Out (major perk): every 6 s the swing wounds and slows
		boolean claws = s.hero == Hero.JETPACK_CAT && dev.overcraft.jetpackcat.JetpackCat.clawsReady(hero, s);
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
			// melee: through barriers (dev.overcraft.brigitte.Barrier.isMelee)
			dev.overcraft.brigitte.Barrier.meleeDepth++;
			float dealt;
			try {
				dealt = Combat.damage(target, source, DAMAGE, claws ? Blow.melee("claws_out") : Blow.MELEE);
			} finally {
				dev.overcraft.brigitte.Barrier.meleeDepth--;
			}
			Combat.credit(hero, dealt, target);
			Combat.addFireForDamage(hero, dealt, target);
			if (dealt > 0) {
				Effects.hitMarker(hero, target, false);
				if (s.hero == Hero.TRACER) {
					// Kinetic Reload: 12 rounds an enemy hit (D 51EE #396 -> #393)
					dev.overcraft.tracer.Tracer.meleeHit(hero, s);
				}
			}
			// (minus the look: Minecraft's knockback takes the direction towards the attacker)
			// (the only push now: Minecraft's own from the hurt is off for hero hits, #93; it pushed twice, X13)
			target.knockback(KNOCKBACK, -look.x, -look.z, source, Ow.hp(dealt));
			Combat.send(target);
			// a fall or the void after it is the hero's kill (#84: the classic boop)
			Combat.pushed(target, hero, claws ? Blow.melee("claws_out") : Blow.MELEE);
			if (!hit && s.hero == Hero.SIGMA) {
				// Sigma's Hyper Strike (major perk): the first one it lands on (W Sigma rev 409089: "Charge is not consumed if quick melee doesn't hit any enemies"; the phased aren't hit, canHurt)
				dev.overcraft.sigma.SigmaPerks.meleeHit(hero, s, target);
			}
			hit = true;
			if (claws) {
				dev.overcraft.jetpackcat.JetpackCat.wound(hero, target);
			}
		}
		if (claws && hit) {
			dev.overcraft.jetpackcat.JetpackCat.clawsUsed(s);
		}
		Combat.animate(hero, CLIPS[hero.getRandom().nextInt(CLIPS.length)]);
		Voice.say(hero, "melee", 0.8f);
		HeroSounds.fromHero(hero, hit ? OcSounds.MELEE_HIT : OcSounds.MELEE_SWING, 1);
		if (s.hero == Hero.LIFEWEAVER) {
			dev.overcraft.lifeweaver.Lifeweaver.meleeSwung(hero);
		}
		if (s.hero == Hero.ANA) {
			// her swing's foley from her melee clips (39BC at 0.4 s, built into the files)
			HeroSounds.split(hero, OcSounds.ANA_MELEE, OcSounds.ANA_MELEE_3P, hero.position(), 30);
		}
		if (s.hero == Hero.SOJOURN) {
			// her melee clips' foley: 1P 6B94's 36E7 (5ECD + 5F24) hers, 3P 7EF3's 372E (5FE7 30 m + 5FF4 45 m) for others
			HeroSounds.split(hero, dev.overcraft.sojourn.SojournSounds.of("melee"), dev.overcraft.sojourn.SojournSounds.of("melee_3p"),
				hero.position(), 45);
		}
		if (s.hero == Hero.PHARAH) {
			HeroSounds.split(hero, dev.overcraft.pharah.PharahSounds.of("melee"), dev.overcraft.pharah.PharahSounds.of("melee_3p"),
				hero.position(), 45);
		}
		if (s.hero == Hero.TRACER) {
			// her 3P melee clip 01BE's 1788 @0.133 (30 m): one copy for everyone (tracer_sfx.txt)
			dev.overcraft.tracer.TracerSounds.all(hero.level(), "melee", hero.position(), 30);
		}
		if (s.hero == Hero.JUNKRAT) {
			// his melee clip's foley (junkrat_sfx.txt): his own, and the 3P copy for others
			dev.overcraft.junkrat.JunkratSounds.split(hero, "melee", hero.position(), 30);
		}
		if (s.hero == Hero.CASSIDY) {
			// his melee clip's DBAD (cassidy_sfx.txt): one copy for everyone
			dev.overcraft.cassidy.CassidySounds.all(hero.level(), "melee", hero.position(), 30);
		}
		if (s.hero == Hero.SIGMA) {
			// his melee clips' foley: 1P 6FCD's 3086 (59F1 + 5A6C) his, 3P 6F89's 306A (59F7, 30 m) for others
			HeroSounds.split(hero, dev.overcraft.sigma.SigmaSounds.of("melee"), dev.overcraft.sigma.SigmaSounds.of("melee_3p"),
				hero.position(), 30);
		}
		if (s.hero == Hero.JUNO) {
			// her 1P melee clip E9B8's foley (5F7F: B2CF + B8BF); her 3P melee clips carry none (H), so hers only
			HeroSounds.toOwner(hero, dev.overcraft.juno.JunoSounds.of("melee"));
		}
	}
}
