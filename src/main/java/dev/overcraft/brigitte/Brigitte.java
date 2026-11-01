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
		s.whipTicks = -1;
		s.repairRecharge = -1;
	}

	/** Leaving her (another hero, leaving the world): the barrier down, its slow off. */
	public static void clear(ServerPlayer player, HeroState s) {
		Barrier.clear(player, s);
		s.bashTicks = -1;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double rate = dev.overcraft.hero.Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		recharge(s, rate);
		whip(player, s);
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
		if ((pressed & dev.overcraft.net.HeroInputPayload.PRESS_ABILITY_1) != 0) {
			startWhip(player, s);
		}
		if ((pressed & dev.overcraft.net.HeroInputPayload.PRESS_ABILITY_2) != 0) {
			repairPack(player, s);
		}
	}

	// ---- Whip Shot (1259) ----

	/** Shift: not mid-bash, not while it's out (allowed with the shield up: #902, M). */
	private static void startWhip(ServerPlayer player, HeroState s) {
		if (s.ability1Cooldown > 0 || s.whipTicks >= 0 || s.bashTicks >= 0) {
			return;
		}
		s.whipTicks = 0;
		s.whipOut = -1;
		Combat.animate(player, "whip");
	}

	/**
	 * 0.2 s, then the head flies straight from her eyes along her aim at 80 m/s; the first enemy within 0.5 m of its path
	 * before a block takes 70 and is pushed along her level aim at 25 m/s and 2 up; it comes back 0.3 s (none on the way back),
	 * and the 4 s cooldown starts then.
	 */
	private static void whip(ServerPlayer player, HeroState s) {
		if (s.whipTicks < 0) {
			return;
		}
		int t = s.whipTicks++;
		if (t == Ow.ticks(WHIP_WINDUP)) {
			s.whipFrom = player.getEyePosition();
			s.whipDir = player.getViewVector(1.0f);
			s.whipOut = 0;
		}
		if (s.whipOut >= 0 && s.whipFrom != null && s.whipDir != null) {
			double step = Ow.speed(WHIP_SPEED);
			Vec3 a = s.whipFrom.add(s.whipDir.scale(s.whipOut));
			double reach = Math.min(step, WHIP_RANGE - s.whipOut);
			Vec3 b = a.add(s.whipDir.scale(reach));
			HitResult block = player.level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Vec3 end = block.getType() == HitResult.Type.MISS ? b : block.getLocation();
			LivingEntity hit = null;
			double best = Double.MAX_VALUE;
			for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(a, end).inflate(WHIP_RADIUS + 1),
				e -> e.isAlive() && Allies.canHurt(player, e))) {
				java.util.Optional<Vec3> at = e.getBoundingBox().inflate(WHIP_RADIUS).clip(a, end);
				if (at.isPresent() && at.get().distanceToSqr(a) < best) {
					hit = e;
					best = at.get().distanceToSqr(a);
				}
			}
			if (hit != null) {
				float dealt = Combat.damage(hit, flailSource(player), WHIP_DAMAGE);
				Combat.credit(player, dealt);
				Combat.addFireForDamage(player, dealt, Ow.toOw(hit.getMaxHealth()));
				Effects.hitMarker(player, hit, false);
				Vec3 level = new Vec3(s.whipDir.x, 0, s.whipDir.z);
				Vec3 push = (level.lengthSqr() < 1.0E-6 ? Vec3.ZERO : level.normalize().scale(Ow.speed(WHIP_KNOCKBACK)))
					.add(0, Ow.speed(WHIP_LIFT), 0);
				PUSHED.add(new Push(hit, push, Ow.ticks(WHIP_PUSH_TIME)));
				Inspire.trigger(player, s, true, INSPIRE_TIME);
				back(player, s, "whip_return");
			} else if (block.getType() != HitResult.Type.MISS) {
				back(player, s, "whip_return_blocked");
			} else {
				s.whipOut += reach;
				if (s.whipOut >= WHIP_RANGE - 1.0E-6) {
					back(player, s, "whip_return_miss");
				}
			}
		}
		if (s.whipOut == -2 && t >= s.whipBackAt) {
			s.whipTicks = -1;
			s.ability1Cooldown = Ow.ticks(WHIP_COOLDOWN);
		}
	}

	/** Whip Shot's push, held a few ticks (WHIP_PUSH_TIME). */
	private static final class Push {
		final LivingEntity target;
		final Vec3 velocity;
		int left;

		Push(LivingEntity target, Vec3 velocity, int left) {
			this.target = target;
			this.velocity = velocity;
			this.left = left;
		}
	}

	private static final java.util.List<Push> PUSHED = new java.util.ArrayList<>();

	public static void tickPushes() {
		PUSHED.removeIf(p -> {
			if (p.left-- <= 0 || !p.target.isAlive() || p.target.isRemoved()) {
				return true;
			}
			Vec3 v = p.target.getDeltaMovement();
			// level speed held, its first lift once (Minecraft's gravity brings them down)
			p.target.setDeltaMovement(p.velocity.x, p.left == Ow.ticks(WHIP_PUSH_TIME) - 1 ? p.velocity.y : v.y, p.velocity.z);
			p.target.needsSync = true;
			return false;
		});
	}

	/** The flail starts back: no more hits; the cooldown when it's in her hand (0.3 s). */
	private static void back(ServerPlayer player, HeroState s, String clip) {
		s.whipOut = -2;
		s.whipBackAt = s.whipTicks + Ow.ticks(WHIP_RETURN);
		Combat.animate(player, clip);
	}

	// ---- Repair Pack (0EDD) ----

	/** E: a charge to the ally nearest her aim (10 degrees, 25 m, in sight, not herself). */
	private static void repairPack(ServerPlayer player, HeroState s) {
		if (s.secondaryAmmo <= 0 || s.bashTicks >= 0) {
			return;
		}
		LivingEntity ally = dev.overcraft.hero.Targeting.allyInView(player, REPAIR_RANGE, REPAIR_CONE, true);
		if (ally == null) {
			return;
		}
		s.secondaryAmmo--;
		RepairPack.launch(player, ally);
		Combat.animate(player, "throw");
	}

	/** One charge back every 5 s while she has fewer than three (133A), at the cooldown rate. */
	private static void recharge(HeroState s, double rate) {
		if (s.secondaryAmmo >= REPAIR_CHARGES) {
			s.repairRecharge = -1;
			return;
		}
		if (s.repairRecharge < 0) {
			s.repairRecharge = Ow.ticks(REPAIR_RECHARGE);
		}
		s.repairRecharge -= rate;
		if (s.repairRecharge <= 0) {
			s.secondaryAmmo++;
			s.repairRecharge = s.secondaryAmmo < REPAIR_CHARGES ? Ow.ticks(REPAIR_RECHARGE) : -1;
		}
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
