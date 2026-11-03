package dev.overcraft.sigma;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Sleep;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.sigma.SigmaStats.*;

/**
 * Kinetic Grasp (1D02, 1D03: sigma-map.md): ability 1, 2 s in which every enemy projectile entering a box before him (1 m
 * either side of his aim, 1.6 m down to 1.1 m up, 0.4 to 2.6 m ahead of his eyes) is absorbed: gone, its damage counted
 * (Barriers.projectileDamage) and 5% of it to his ult. At a normal end 60% of the count becomes overhealth, up to 400 with
 * what's left of an earlier pool (Combat.overhealthPool), held 3 s, then 5 a second; cut short (a stun, death, G) none.
 * 12 s cooldown from its end. His hands are busy: no spheres, no barrier sent meanwhile (ours, M). Its look and sound from his data
 * (step 10): the field 2BE4 from SIGMA_STATE (SigmaFx), each absorb 2C84, the overhealth 2D3E; sigma-sounds.md 3.
 */
public final class KineticGrasp {
	private KineticGrasp() {
	}

	static boolean active(HeroState s) {
		return s.sigmaGrasp >= 0;
	}

	/** Shift: it starts, if ready. */
	static void press(ServerPlayer player, HeroState s) {
		if (active(s) || s.ability1Cooldown > 0 || Sleep.asleep(player) || !player.isAlive()) {
			return;
		}
		s.sigmaGrasp = Ow.ticks(GRASP_TIME);
		s.sigmaGraspAbsorbed = 0;
		// his start clip (0x60A, #398) then the hold (0x609): HeroAnimator
		Combat.animate(player, "grasp");
		// #489 -> #751 -> #484 2C26 (5E05 his, 54C2 others' 70 m; its loop is his clients'), and his line (0B9A, #556)
		SigmaSounds.split(player, "grasp.start", player.getEyePosition(), 70);
		dev.overcraft.hero.Voice.say(player, "grasp", 1);
	}

	static void tick(ServerPlayer player, HeroState s, boolean controls) {
		if (!active(s)) {
			return;
		}
		if (!controls || !player.isAlive() || Sleep.asleep(player)) {
			// cut short (#554, the onAbort branch skips #593): no overhealth
			end(player, s, false);
			return;
		}
		absorb(player, s);
		if (s.sigmaGrasp == Ow.ticks(GRASP_TIME) - Ow.ticks(0.5)) {
			// the start clip's 0.5 s are over: its hold (0x609, #746 / #748)
			Combat.animate(player, "grasp_hold");
		}
		if (--s.sigmaGrasp <= 0) {
			end(player, s, true);
		}
	}

	/** The projectiles in the box this tick (their last and next steps sampled: an arrow covers 3 m a tick). */
	private static void absorb(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 f = player.getViewVector(1.0f);
		Vec3 r = f.cross(new Vec3(0, 1, 0));
		r = r.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : r.normalize();
		Vec3 u = r.cross(f).normalize();
		for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(eye, eye).inflate(GRASP_FAR + 6), Barriers::takes)) {
			Entity owner = p.getOwner();
			if (owner == player || owner != null && Allies.isSelfOrAlly(player, owner)) {
				continue;
			}
			Vec3 now = p.position(), before = new Vec3(p.xo, p.yo, p.zo), next = now.add(p.getDeltaMovement());
			Vec3 in = null;
			for (int i = 0; i <= 8 && in == null; i++) {
				Vec3 at = i <= 4 ? before.lerp(now, i / 4.0) : now.lerp(next, (i - 4) / 4.0);
				if (inBox(at.subtract(eye), f, r, u)) {
					in = at;
				}
			}
			if (in == null) {
				continue;
			}
			double damage = Barriers.projectileDamage(p);
			s.sigmaGraspAbsorbed += damage;
			// 5% of what it absorbs to his ult (#432, M)
			Combat.addUltCharge(player, damage * GRASP_ULT_SHARE, false);
			Barriers.stop(p, in, false);
			// 2C84 where it dies (#518 / #695); 2C46 (54AF his, 54B0 others' 30 m), the shooter's own 2F31 (#567 / #693 true)
			Effects.play(level, "sigma_absorb", in, p.getDeltaMovement(), player, 0, player);
			SigmaSounds.splitAt(player, "grasp.absorb", in, 30, 30);
			if (owner instanceof ServerPlayer shooter) {
				SigmaSounds.own(shooter, "grasp.absorb_shooter");
			}
			// "Hold it together...!" (0B9B, #560: an absorbed hit that isn't an ultimate)
			dev.overcraft.hero.Voice.say(player, "grasp_absorb", 1);
		}
	}

	static boolean inBox(Vec3 rel, Vec3 f, Vec3 r, Vec3 u) {
		double x = rel.dot(r), y = rel.dot(u), z = rel.dot(f);
		return Math.abs(x) <= GRASP_SIDE && y >= -GRASP_DOWN && y <= GRASP_UP && z >= GRASP_NEAR && z <= GRASP_FAR;
	}

	/** Its end: at a normal one 60% of what it took as overhealth (#593 -> 1D03); its 12 s from now (#424 / #425). */
	static void end(ServerPlayer player, HeroState s, boolean normal) {
		if (!active(s)) {
			return;
		}
		s.sigmaGrasp = -1;
		s.ability1Cooldown = Ow.ticks(GRASP_COOLDOWN);
		if (normal && s.sigmaGraspAbsorbed > 0) {
			Combat.overhealthPool(player, s.sigmaGraspAbsorbed * GRASP_CONVERSION, GRASP_MAX, GRASP_HOLD, GRASP_DECAY);
			// Kinetic Cycle (minor perk): Accretion's cooldown shorter for what it took
			SigmaPerks.graspEnded(player, s, s.sigmaGraspAbsorbed);
			// 1D03 #207 -> #209 -> #208 2D3E on him (hp 0001): its look and 1A1E his / 1A17 others' 70 m
			Effects.play(player.level(), "sigma_overhealth", player.position(), Vec3.ZERO, player, 0, player);
			SigmaSounds.split(player, "grasp.overhealth", player.getEyePosition(), 70);
		}
		Combat.animate(player, "grasp_end");
	}
}
