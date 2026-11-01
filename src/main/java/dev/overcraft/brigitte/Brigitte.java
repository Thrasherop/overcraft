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
		s.barrierUp = false;
		s.barrierHealth = BARRIER_HEALTH;
		s.barrierBrokenUntil = -1;
		s.bashTicks = -1;
		s.rallyTicks = 0;
	}

	/** Leaving her (another hero, leaving the world): the barrier down, its slow off. */
	public static void clear(ServerPlayer player, HeroState s) {
		Barrier.clear(player, s);
		s.bashTicks = -1;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - dev.overcraft.hero.Combat.cooldownRate(player));
		swing(player, s);
		bash(player, s);
		Barrier.tick(player, s, controls);
		boolean pressed = s.primaryHeld && !s.leftWas;
		s.leftWas = s.primaryHeld;
		if (!controls || !player.isAlive()) {
			return;
		}
		if (s.barrierUp) {
			// the shield up: the left button bashes, the flail is still (0EE3 #670 / #681)
			if (pressed && s.bashTicks < 0 && s.ability2Cooldown <= 0) {
				startBash(player, s);
			}
		} else if (s.primaryHeld && s.flailSwing < 0) {
			startSwing(player, s);
		}
	}

	// ---- Shield Bash (0EE3 #495) ----

	/**
	 * The left button with the shield up: 0.3 s forward at 4x her speed (her client moves her: ClientBrigitte), the first
	 * enemy reached taking 70 and a 6 m/s push, Inspire whatever its cooldown, Survivor's regeneration at once (message
	 * 26FD); it stops on a hit; its 5 s cooldown from its end (#504). No stun outside Rally.
	 */
	private static void startBash(ServerPlayer player, HeroState s) {
		s.bashTicks = 0;
		s.bashHit = false;
		Combat.animate(player, "shield_bash");
		// Survivor (01C7, tag 1173): a movement ability starts the regeneration at once
		s.lastDamagedTick = Math.min(s.lastDamagedTick, Combat.now(player) - Combat.REGEN_DELAY);
	}

	private static void bash(ServerPlayer player, HeroState s) {
		if (s.bashTicks < 0) {
			return;
		}
		int t = s.bashTicks++;
		if (!s.bashHit && player.isAlive()) {
			LivingEntity target = bashTarget(player);
			if (target != null) {
				s.bashHit = true;
				float dealt = Combat.damage(target, flailSource(player), BASH_DAMAGE);
				Combat.credit(player, dealt);
				Combat.addFireForDamage(player, dealt, Ow.toOw(target.getMaxHealth()));
				Effects.hitMarker(player, target, false);
				Vec3 push = flat(player).scale(Ow.speed(BASH_KNOCKBACK) * 3).add(0, 0.25, 0);
				target.push(push.x, push.y, push.z);
				target.needsSync = true;
				Combat.animate(player, "shield_bash_hit");
				Inspire.trigger(player, s, true, INSPIRE_TIME);
				end(player, s);
				return;
			}
		}
		if (t + 1 >= Ow.ticks(BASH_TIME) || player.horizontalCollision && t > 0) {
			end(player, s);
		}
	}

	private static void end(ServerPlayer player, HeroState s) {
		s.bashTicks = -1;
		s.ability2Cooldown = Ow.ticks(BASH_COOLDOWN);
		// her client stops moving her now (a hit or a wall ends it early)
		Effects.play(player.level(), "brigitte_bash_end", player.position(), Vec3.ZERO, player, 0, player);
	}

	/** The first enemy within 2.5 m ahead of her, in a 35 degree cone, in sight (#501, L shape). */
	private static @org.jspecify.annotations.Nullable LivingEntity bashTarget(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 ahead = flat(player);
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(BASH_REACH + 1),
			e -> e.isAlive() && Allies.canHurt(player, e))) {
			AABB box = e.getBoundingBox();
			Vec3 near = new Vec3(Math.clamp(eye.x, box.minX, box.maxX), Math.clamp(eye.y, box.minY, box.maxY), Math.clamp(eye.z, box.minZ, box.maxZ));
			Vec3 to = new Vec3(box.getCenter().x - eye.x, 0, box.getCenter().z - eye.z);
			double dist = near.distanceTo(eye);
			double angle = Math.toDegrees(Math.acos(Math.clamp(to.normalize().dot(ahead), -1, 1)));
			if (dist <= BASH_REACH && (angle <= BASH_CONE || dist < 1) && dist < bestDist) {
				best = e;
				bestDist = dist;
			}
		}
		return best;
	}

	private static Vec3 flat(ServerPlayer player) {
		float yaw = player.getYRot() * (float) (Math.PI / 180);
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
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
			(int) Math.ceil(s.barrierHealth), -1, s.secondaryAmmo, -1, (float) (s.ability1Cooldown / 20.0),
			(float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, 0, 0, 0);
	}
}
