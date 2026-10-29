package dev.overcraft.ana;

import dev.overcraft.Ow;
import dev.overcraft.entity.AnaDartEntity;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static dev.overcraft.ana.AnaStats.*;

/**
 * Ana's kit (tools/reference/ana-plan.md "Order"), built step by step, each played before the next. Now: the Biotic
 * Rifle unscoped (her rifle graphs 0B94 / 0B95): a dart every 0.8 s while the left button is held, 15 to a clip, a
 * 1.5 s reload (the clip full at 0.6 s), automatic at 0 and on R. Take Aim, her abilities and Nano Boost come next.
 */
public final class Ana implements HeroKit {
	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = RIFLE_AMMO;
		s.primaryReload = -1;
		s.weaponRecovery = 0;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double attack = Combat.attackRate(player);
		reload(s, attack);
		if (!controls || !player.isAlive()) {
			return;
		}
		s.weaponRecovery = Math.max(0, s.weaponRecovery - attack);
		// the clip empty: its reload starts by itself once the shot's recovery is over (0B94 #4 / #10)
		if (s.primaryAmmo == 0 && s.primaryReload < 0 && s.weaponRecovery <= 0) {
			startReload(player, s);
		}
		if (s.primaryHeld && s.primaryReload < 0 && s.weaponRecovery <= 0 && s.primaryAmmo > 0) {
			fire(player, s);
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0) {
			startReload(player, s);
		}
	}

	private static void fire(ServerPlayer player, HeroState s) {
		s.primaryAmmo--;
		s.weaponRecovery += Ow.ticks(RIFLE_RECOVERY);
		ServerLevel level = player.level();
		Vec3 from = fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		AnaDartEntity dart = new AnaDartEntity(player, level);
		dart.launch(from, aimPoint(player).subtract(from), Ow.speed(DART_SPEED));
		level.addFreshEntity(dart);
		Combat.animate(player, "fire");
	}

	/** R (not with a full clip or mid-reload), or the clip empty: 1.5 s, the clip full at 0.6 s (0B94 #33 / #72). */
	private static void startReload(ServerPlayer player, HeroState s) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= RIFLE_AMMO || s.weaponRecovery > 0) {
			return;
		}
		s.primaryReload = 0;
		Combat.animate(player, "reload");
	}

	private static void reload(HeroState s, double attack) {
		if (s.primaryReload < 0) {
			return;
		}
		s.primaryReload += attack;
		if (s.primaryReload >= Ow.ticks(RIFLE_RELOAD * RIFLE_RELOAD_AMMO)) {
			s.primaryAmmo = RIFLE_AMMO;
		}
		if (s.primaryReload >= Ow.ticks(RIFLE_RELOAD)) {
			s.primaryReload = -1;
		}
	}

	/**
	 * Where the dart leaves, from her eyes in her look's frame (right, up, ahead, m): her rifle's muzzle (its hardpoint
	 * 0006, 0.9 m ahead of her hands) is out in front; ours a point by the crosshair until measured on her model.
	 */
	private static final double MUZZLE_RIGHT = 0.18, MUZZLE_UP = -0.12, MUZZLE_AHEAD = 0.6;

	static Vec3 fromEyes(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 upv = side.cross(look).normalize();
		Vec3 at = eye.add(side.scale(right)).add(upv.scale(up)).add(look.scale(ahead));
		return player.level().clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType()
			== HitResult.Type.MISS ? at : eye;
	}

	/** What her crosshair is on (a block, else 100 m out): the dart flies there from the muzzle. */
	static Vec3 aimPoint(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(player.getViewVector(1.0f).scale(100));
		Vec3 hit = player.level().clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		return hit.distanceTo(eye) < 1 ? far : hit;
	}

	// ---- the rifle's doses: 75 over 0.5 s, heal or damage (0B95 #193 / #205) ----

	private record Dose(ServerPlayer owner, LivingEntity target, boolean heal, Entity dart, double perTick, int ticks) {
	}

	private static final List<Dose> DOSES = new ArrayList<>();

	/** A dart reached a hurt ally (heal) or an enemy: its 75 lands over 0.5 s. */
	public static void dose(ServerPlayer owner, LivingEntity target, boolean heal, Entity dart) {
		int ticks = Math.max(1, Ow.ticks(RIFLE_OVER));
		DOSES.add(new Dose(owner, target, heal, dart, (heal ? RIFLE_HEAL : RIFLE_DAMAGE) / ticks, ticks));
		if (!heal) {
			Effects.hitMarker(owner, target, false);
		}
	}

	/** Every server tick (HeroManager): the doses in flight land their share. */
	public static void tickDoses() {
		List<Dose> next = new ArrayList<>();
		for (Dose d : DOSES) {
			if (!d.target.isAlive() || d.target.isRemoved()) {
				continue;
			}
			if (d.heal) {
				float healed = Combat.heal(d.target, d.perTick);
				if (healed > 0) {
					Combat.credit(d.owner, healed);
					Combat.addFireForHealing(d.owner, healed);
				}
			} else {
				float dealt = Combat.damage(d.target, d.owner.damageSources().thrown(d.dart, d.owner), d.perTick);
				Combat.credit(d.owner, dealt);
				Combat.addFireForDamage(d.owner, dealt, Ow.toOw(d.target.getMaxHealth()));
			}
			if (d.ticks > 1) {
				next.add(new Dose(d.owner, d.target, d.heal, d.dart, d.perTick, d.ticks - 1));
			}
		}
		DOSES.clear();
		DOSES.addAll(next);
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float reload = s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(RIFLE_RELOAD)) : -1;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			s.primaryAmmo, reload, 0, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, 0, 0, 0);
	}
}
