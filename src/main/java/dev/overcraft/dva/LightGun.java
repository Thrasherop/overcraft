package dev.overcraft.dva;

import dev.overcraft.Ow;
import dev.overcraft.entity.LightGunShotEntity;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.dva.DvaStats.*;

/**
 * The pilot's Light Gun (02B8; dva-plan.md "Pilot: Light Gun"): the left button as the pilot (the Light Gun Input option's
 * default: Primary). 7 rounds a second while held (an accumulator: 20 / 7 ticks a round), each a 50 m/s projectile
 * ({@link LightGunShotEntity}: 14, x2 on the head, no falloff, no spread). 20 rounds; R reloads (or an empty magazine does)
 * in 1.4 s, the rounds back at 0.91 s, and from then a shot cuts the rest short (#218 v414: M). Full again each time she's
 * in a mech (026A #152's 007D), so she always ejects with a full gun. Stand-in sound.
 */
public final class LightGun {
	private LightGun() {
	}

	/** A full magazine, no reload (each link: D 026A #152). */
	static void refill(HeroState s) {
		s.primaryAmmo = LIGHT_GUN_AMMO;
		s.primaryReload = -1;
		s.dvaGunRecovery = 0;
	}

	/** R as the pilot: a reload, unless it's full or reloading. */
	static void reload(ServerPlayer player, HeroState s) {
		if (s.primaryAmmo < LIGHT_GUN_AMMO && s.primaryReload < 0) {
			startReload(player, s);
		}
	}

	/** Each tick in hero mode (Dva.tick); {@code can}: she's the pilot with her controls. */
	static void tick(ServerPlayer player, HeroState s, boolean can) {
		double attack = Combat.attackRate(player);
		s.dvaGunRecovery = Math.max(0, s.dvaGunRecovery - attack);
		if (!can) {
			return;
		}
		if (s.primaryAmmo <= 0 && s.primaryReload < 0) {
			startReload(player, s);
		}
		boolean ammoBack = false;
		if (s.primaryReload >= 0) {
			s.primaryReload += attack;
			if (s.primaryReload >= Ow.ticks(LIGHT_GUN_RELOAD * LIGHT_GUN_RELOAD_AMMO)) {
				s.primaryAmmo = LIGHT_GUN_AMMO;
				ammoBack = true;
			}
			if (s.primaryReload >= Ow.ticks(LIGHT_GUN_RELOAD)) {
				s.primaryReload = -1;
			}
		}
		boolean canShoot = s.primaryReload < 0 || ammoBack;
		if (s.primaryHeld && canShoot && s.primaryAmmo > 0 && s.dvaGunRecovery <= 0) {
			// the reload's last 0.49 s cut short by a shot
			s.primaryReload = -1;
			s.primaryAmmo--;
			s.dvaGunRecovery += 20.0 / LIGHT_GUN_RATE;
			shoot(player);
		}
	}

	private static void startReload(ServerPlayer player, HeroState s) {
		s.primaryReload = 0;
		// her reload (alias 0x4: 3234, 1P 30BB; the gun's 327D)
		Combat.animate(player, "reload");
		// her reload clips' sounds: 1P 30BB's six (hers), 3P 3234's 2E14 (45 m)
		DvaSounds.split(player, "gun.reload", player.getEyePosition(), 45);
	}

	/**
	 * Where a round leaves from, from her eyes in her look's frame (right, up, ahead, m): the gun's hp 0006 (02B8 #94). Ours:
	 * Mercy's blaster's measured place (a pistol in the right hand the same way), until hers is measured (M).
	 */
	private static final double MUZZLE_RIGHT = 0.16, MUZZLE_UP = -0.11, MUZZLE_AHEAD = 0.71;

	/** One round from the gun toward what her crosshair is on (a wall in the way: from her eyes). */
	private static void shoot(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(look).normalize();
		Vec3 from = eye.add(side.scale(MUZZLE_RIGHT)).add(up.scale(MUZZLE_UP)).add(look.scale(MUZZLE_AHEAD));
		if (level.clip(new ClipContext(eye, from, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			from = eye;
		}
		Vec3 far = eye.add(look.scale(100));
		Vec3 target = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		if (target.distanceTo(eye) < MUZZLE_AHEAD + 0.5) {
			target = far;
		}
		LightGunShotEntity shot = new LightGunShotEntity(player, level);
		shot.launch(from, target.subtract(from), Ow.speed(LIGHT_GUN_SPEED));
		level.addFreshEntity(shot);
		Combat.animate(player, "fire");
		// the round's 'sound only' slot as it's made (entity 1000: effect F03, 228D 60 m, for everyone)
		DvaSounds.all(level, "gun.fire", from, 60);
	}
}
