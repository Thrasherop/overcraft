package dev.overcraft.brigitte;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.brigitte.BrigitteStats.*;

/**
 * Brigitte's kit (tools/reference/brigitte-plan.md "Order"), built step by step, each played before the next. Her buttons by
 * her data's ids: the left button the flail (Shield Bash with the shield up), the right the shield, Shift Whip Shot, E Repair
 * Pack, Q Rally (brigitte-map.md, "Buttons"). Now: the Rocket Flail and Inspire (step 3); the rest comes next.
 */
public final class Brigitte implements HeroKit {
	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.weaponRecovery = 0;
		s.secondaryAmmo = REPAIR_CHARGES;
		s.flailSwing = -1;
		s.flailCombo = 0;
		s.flailHit.clear();
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		swing(player, s);
		if (controls && player.isAlive() && s.primaryHeld && s.flailSwing < 0) {
			startSwing(player, s);
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
	}

	/** Her quick melee is the flail's swing (1248 #267: no shared quick melee on her). */
	public static void melee(ServerPlayer player, HeroState s) {
		if (s.flailSwing < 0) {
			startSwing(player, s);
		}
	}

	// ---- Rocket Flail (1248) ----

	/** A swing: forehand and backhand in turn (1248 #636: 0 / 2 forehand, 1 backhand). */
	private static void startSwing(ServerPlayer player, HeroState s) {
		s.flailSwing = 0;
		s.flailHit.clear();
		Combat.animate(player, s.flailCombo % 2 == 0 ? "swing" : "swing_2");
	}

	/** Each tick of a swing: the sweep from 0.2 s to 0.4 s, then the rest of its 0.6 s. */
	private static void swing(ServerPlayer player, HeroState s) {
		if (s.flailSwing < 0) {
			return;
		}
		int t = s.flailSwing++;
		int from = Ow.ticks(FLAIL_WINDUP), to = Ow.ticks(FLAIL_WINDUP + FLAIL_SWEEP);
		if (t >= from && t <= to && player.isAlive()) {
			// the arc covered so far: -17.5 + 35 t^2 over the sweep (eased), the backhand the other way (1248 #715 / #717)
			double done = (double) (t - from) / Math.max(1, to - from);
			double edge = -FLAIL_ARC / 2 + FLAIL_ARC * done * done;
			sweep(player, s, s.flailCombo % 2 == 0 ? -FLAIL_ARC / 2 : -edge, s.flailCombo % 2 == 0 ? edge : FLAIL_ARC / 2);
		}
		if (s.flailSwing >= Ow.ticks(FLAIL_WINDUP + FLAIL_RECOVERY)) {
			s.flailSwing = -1;
			s.flailCombo++;
		}
	}

	/** Every enemy within 6 m in the arc swept so far (yaw degrees from her aim, + to her right), in sight, once a swing. */
	private static void sweep(ServerPlayer player, HeroState s, double fromYaw, double toYaw) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		float aimYaw = player.getYRot();
		float aimPitch = player.getXRot();
		for (net.minecraft.world.entity.Entity e : level.getEntities(player, player.getBoundingBox().inflate(FLAIL_RANGE + 1))) {
			LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
			if (target == null || !target.isAlive() || !Allies.canHurt(player, target) || s.flailHit.contains(target.getId())) {
				continue;
			}
			AABB box = e.getBoundingBox();
			Vec3 near = new Vec3(Math.clamp(eye.x, box.minX, box.maxX), Math.clamp(eye.y, box.minY, box.maxY), Math.clamp(eye.z, box.minZ, box.maxZ));
			double dist = near.distanceTo(eye);
			if (dist > FLAIL_RANGE) {
				continue;
			}
			Vec3 to = box.getCenter().subtract(eye);
			double yaw = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-to.x, to.z)) - aimYaw);
			double pitch = -Math.toDegrees(Math.atan2(near.y - eye.y, Math.hypot(to.x, to.z)));
			// its width counts: a target whose side is in the arc is hit
			double margin = Math.toDegrees(Math.atan2(box.getXsize() / 2, Math.max(0.3, Math.hypot(to.x, to.z))));
			if (yaw < fromYaw - margin || yaw > toYaw + margin || Math.abs(Mth.wrapDegrees(pitch - aimPitch)) > FLAIL_PITCH && dist > 1.5
				|| level.clip(new ClipContext(eye, box.getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType()
					!= HitResult.Type.MISS) {
				continue;
			}
			s.flailHit.add(target.getId());
			float dealt = Combat.damage(target, flailSource(player), FLAIL_DAMAGE);
			Combat.credit(player, dealt);
			Combat.addFireForDamage(player, dealt, Ow.toOw(target.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(player, target, false);
				Inspire.trigger(player, s, false, INSPIRE_TIME);
			}
		}
	}

	/** The flail's damage: from her, pushing no one (Overwatch's flail has no knockback: magic, as Sombra's zone). */
	static DamageSource flailSource(ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(DamageTypes.MAGIC), null, owner);
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			0, -1, s.secondaryAmmo, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, 0, 0, 0);
	}
}
