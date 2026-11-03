package dev.overcraft.brigitte;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
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
		s.rallyCast = -1;
		s.shockwaveIn = -1;
	}

	/** Leaving her (another hero, leaving the world): the barrier down, its slow off. */
	public static void clear(ServerPlayer player, HeroState s) {
		Barrier.clear(player, s);
		s.bashTicks = -1;
		endRally(player, s);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double rate = dev.overcraft.hero.Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		recharge(player, s, rate);
		if (s.strikeFastUntil >= 0 && player.level().getGameTime() >= s.strikeFastUntil) {
			s.strikeFastUntil = -1;
			var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.removeModifier(STRIKE_FAST);
			}
		}
		rally(player, s);
		shockwave(player, s);
		whip(player, s);
		swing(player, s);
		bash(player, s);
		Barrier.tick(player, s, controls);
		boolean pressed = s.primaryHeld && !s.leftWas;
		if (!s.primaryHeld && s.leftWas && !s.barrierUp && player.isAlive()) {
			// the flail put up as the left button is let go (223A)
			BrigitteSounds.split(player, "flail.release", player.getEyePosition(), 30);
		}
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
			s.flailMelee = false;
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
		Effects.play(player.level(), "brigitte_bash", player.position(), flat(player), player, 0, player);
		BrigitteSounds.split(player, "bash.dash", player.getEyePosition(), 70);
		dev.overcraft.hero.Voice.say(player, "bash", 1);
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
				float dealt = Combat.damage(target, flailSource(player), BASH_DAMAGE, dev.overcraft.hero.Blow.ability("shield_bash").by(dev.overcraft.hero.Hero.BRIGITTE));
				Combat.credit(player, dealt, target);
				Combat.addFireForDamage(player, dealt, Ow.toOw(target.getMaxHealth()));
				Effects.hitMarker(player, target, false);
				// a Stalwart takes 40% less of it (Tank)
				Vec3 push = flat(player).scale(Ow.speed(BASH_KNOCKBACK) * 3).add(0, 0.25, 0).scale(dev.overcraft.hero.Tank.knockbackFactor(target));
				target.push(push.x, push.y, push.z);
				target.needsSync = true;
				dev.overcraft.hero.Combat.pushed(target, player, dev.overcraft.hero.Blow.ability("shield_bash").by(dev.overcraft.hero.Hero.BRIGITTE));
				Combat.animate(player, "shield_bash_hit");
				// 2206 on her shield (hp 01D8): the imprint, its edge, the streaks
				Effects.play(player.level(), "brigitte_bash_hit", player.position().add(flat(player).scale(0.6)).add(0, 1.2, 0), flat(player),
					player, 0, player);
				dev.overcraft.hero.Voice.say(player, "bash_hit", 1);
				if (HeroManager.hasPerk(player, Perk.BRIGITTE_INSPIRING_STRIKE)) {
					BrigitteSounds.split(player, "perk.inspiring_strike", player.getEyePosition(), 45);
					Inspire.strike(player, s);
				} else {
					Inspire.trigger(player, s, true, INSPIRE_TIME);
				}
				if (s.rallyTicks > 0) {
					// in Rally the bash stuns, and a shockwave follows (12BE: 1340, #878)
					stun(player, target);
					s.shockwaveIn = Ow.ticks(RALLY_SHOCKWAVE_AT);
				}
				end(player, s);
				return;
			}
		}
		if (t + 1 >= Ow.ticks(BASH_TIME) || player.horizontalCollision && t > 0) {
			end(player, s);
		}
	}

	private static final net.minecraft.resources.Identifier STRIKE_FAST = dev.overcraft.Overcraft.id("brigitte_inspiring_strike");

	private static void end(ServerPlayer player, HeroState s) {
		s.bashTicks = -1;
		s.ability2Cooldown = Ow.ticks(BASH_COOLDOWN);
		if (HeroManager.hasPerk(player, Perk.BRIGITTE_INSPIRING_STRIKE)) {
			// Inspiring Strike: 30% faster for 2 s after the dash (51F2 #455 / #456)
			var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(STRIKE_FAST,
					INSPIRING_STRIKE_SPEED, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
			s.strikeFastUntil = player.level().getGameTime() + Ow.ticks(INSPIRING_STRIKE_TIME);
		}
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
		if ((pressed & dev.overcraft.net.HeroInputPayload.PRESS_ULTIMATE) != 0) {
			startRally(player, s);
		}
	}

	// ---- Rally (12BE, 12C1 on each ally) ----

	private static final net.minecraft.resources.Identifier RALLY_FAST = dev.overcraft.Overcraft.id("brigitte_rally");
	private static final net.minecraft.resources.Identifier RALLY_HEALTH = dev.overcraft.Overcraft.id("brigitte_rally_armor");

	/**
	 * Q with the charge full: spent at the press (12BE #152), a 0.7 s cast, then 10 s: her armor +100 (her health's top grows
	 * by it), 15% faster, her barrier 750 and refilled (and 1.2x: Barrier), both cooldowns reset, her bash stunning; every
	 * 0.5 s overhealth for her and her allies within 8 m.
	 */
	private static void startRally(ServerPlayer player, HeroState s) {
		if (s.ultCharge < ULT_COST || s.rallyTicks > 0 || s.rallyCast >= 0 || s.bashTicks >= 0) {
			return;
		}
		s.ultCharge = Combat.afterUlt(dev.overcraft.hero.Hero.BRIGITTE, s.ultCharge);
		s.ultActive = Ow.ticks(RALLY_CAST + RALLY_TIME);
		s.rallyCast = 0;
		Combat.animate(player, "ult_start");
		Effects.play(player.level(), "brigitte_rally", player.position(), flat(player), player, 0, player);
		BrigitteSounds.split(player, "rally.cast", player.getEyePosition(), 75);
		dev.overcraft.hero.HeroSounds.ultLine(player, BrigitteSounds.of("ult.friendly"), BrigitteSounds.of("ult.enemy"), 5);
	}

	private static void rally(ServerPlayer player, HeroState s) {
		if (s.rallyCast >= 0 && s.rallyCast + 1 == Ow.ticks(RALLY_PULSE_AT)) {
			Effects.play(player.level(), "brigitte_rally_pulse", player.position(), flat(player), player, 0, player);
		}
		if (s.rallyCast >= 0 && ++s.rallyCast >= Ow.ticks(RALLY_CAST)) {
			s.rallyCast = -1;
			s.rallyTicks = Ow.ticks(RALLY_TIME);
			s.ability2Cooldown = 0;
			BrigitteSounds.split(player, "rally.loop", player.getEyePosition(), 75);
			s.barrierBrokenUntil = -1;
			s.barrierHealth = BARRIER_HEALTH_RALLY;
			s.extraArmor = RALLY_ARMOR;
			var max = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
			if (max != null) {
				max.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(RALLY_HEALTH,
					Ow.hp(RALLY_ARMOR), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
			}
			// her armor is the layer at the top of her health (Shields): the 100 goes on as armor
			dev.overcraft.hero.Shields.grant(player, s, RALLY_ARMOR);
			var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.addOrReplacePermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(RALLY_FAST, RALLY_SPEED,
					net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}
		if (s.rallyTicks <= 0) {
			return;
		}
		if ((Ow.ticks(RALLY_TIME) - s.rallyTicks) % Ow.ticks(RALLY_PULSE) == 0) {
			for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RALLY_RADIUS),
				e -> e.isAlive() && Allies.isSelfOrAlly(player, e) && e.distanceTo(player) <= RALLY_RADIUS
					&& (e == player || player.hasLineOfSight(e)))) {
				// each 15 lasts 30 s (ours: one pool, held 30 s from the last, then gone)
				Combat.overhealthPool(e, RALLY_OVERHEALTH, RALLY_OVERHEALTH_MAX, RALLY_OVERHEALTH_TIME, 1000);
				Effects.play(player.level(), "brigitte_rally_ally", e.position(), Vec3.ZERO, e, 0, player);
				if (e instanceof ServerPlayer ally && ally != player) {
					BrigitteSounds.own(ally, "rally.overhealth");
				}
			}
		}
		if (s.rallyTicks == Ow.ticks(RALLY_ENDING)) {
			BrigitteSounds.split(player, "rally.ending", player.getEyePosition(), 75);
		}
		if (--s.rallyTicks <= 0) {
			BrigitteSounds.others(player, "rally.end_3p", player.getEyePosition(), 75);
			endRally(player, s);
			Combat.animate(player, "ult_stop");
		}
	}

	/** Rally over (its time, her death, another hero): her armor and speed back, the barrier back to 300 at most. */
	private static void endRally(ServerPlayer player, HeroState s) {
		if (s.rallyTicks > 0 || s.rallyCast >= 0) {
			BrigitteSounds.stop(player.level(), "rally.loop", "rally.loop_3p", "rally.ending", "rally.ending_3p");
		}
		s.rallyTicks = 0;
		s.rallyCast = -1;
		if (s.extraArmor > 0) {
			s.extraArmor = 0;
			s.shield = Math.min(s.shield, s.hero == null ? 0 : s.hero.layer());
		}
		s.barrierHealth = Math.min(s.barrierHealth, BARRIER_HEALTH);
		var max = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
		if (max != null) {
			max.removeModifier(RALLY_HEALTH);
			if (player.getHealth() > player.getMaxHealth()) {
				player.setHealth(player.getMaxHealth());
			}
		}
		var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(RALLY_FAST);
		}
	}

	/** Her bash's shockwave in Rally, 0.15 s after its hit: up to 10 enemies within 8 m ahead, 70, a push and the stun. */
	private static void shockwave(ServerPlayer player, HeroState s) {
		if (s.shockwaveIn < 0 || s.shockwaveIn-- > 0) {
			return;
		}
		Vec3 ahead = flat(player);
		int hit = 0;
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RALLY_SHOCKWAVE_RADIUS),
			e -> e.isAlive() && Allies.canHurt(player, e) && e.distanceTo(player) <= RALLY_SHOCKWAVE_RADIUS)) {
			Vec3 to = new Vec3(e.getX() - player.getX(), 0, e.getZ() - player.getZ());
			if (hit >= RALLY_SHOCKWAVE_TARGETS || to.lengthSqr() > 0.25 && to.normalize().dot(ahead) < 0) {
				continue;
			}
			hit++;
			float dealt = Combat.damage(e, flailSource(player), BASH_DAMAGE, dev.overcraft.hero.Blow.ult("rally").by(dev.overcraft.hero.Hero.BRIGITTE));
			Combat.credit(player, dealt, e);
			Combat.addFireForDamage(player, dealt, Ow.toOw(e.getMaxHealth()));
			Effects.hitMarker(player, e, false);
			Vec3 push = to.lengthSqr() < 1.0E-6 ? ahead : to.normalize();
			double kept = dev.overcraft.hero.Tank.knockbackFactor(e);
			e.push(push.x * Ow.speed(BASH_KNOCKBACK) * 3 * kept, 0.25 * kept, push.z * Ow.speed(BASH_KNOCKBACK) * 3 * kept);
			e.needsSync = true;
			dev.overcraft.hero.Combat.pushed(e, player, dev.overcraft.hero.Blow.ult("rally").by(dev.overcraft.hero.Hero.BRIGITTE));
			stun(player, e);
		}
	}

	/** Rally's stun (Sleep.stun) and its halo over the target's head (220D, for the stun's time). */
	private static void stun(ServerPlayer player, LivingEntity target) {
		dev.overcraft.hero.Sleep.stun(target, RALLY_STUN);
		BrigitteSounds.on(target, "stun", 30);
		Effects.play(player.level(), "brigitte_stun", target.position(), Vec3.ZERO, target, Ow.ticks(RALLY_STUN), player);
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
		BrigitteSounds.split(player, "whip.throw", player.getEyePosition(), 70);
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
			Effects.play(player.level(), "brigitte_whip", s.whipFrom, s.whipDir, player, 0, player);
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
				float dealt = Combat.damage(hit, flailSource(player), WHIP_DAMAGE, dev.overcraft.hero.Blow.ability("whip_shot").by(dev.overcraft.hero.Hero.BRIGITTE));
				Combat.credit(player, dealt, hit);
				Combat.addFireForDamage(player, dealt, Ow.toOw(hit.getMaxHealth()));
				Effects.hitMarker(player, hit, false);
				BrigitteSounds.split(player, "whip.hit", hit.getEyePosition(), 60);
				if (hit instanceof net.minecraft.world.entity.player.Player) {
					dev.overcraft.hero.Voice.say(player, "whip_hit", 1);
				}
				Vec3 level = new Vec3(s.whipDir.x, 0, s.whipDir.z);
				Vec3 push = (level.lengthSqr() < 1.0E-6 ? Vec3.ZERO : level.normalize().scale(Ow.speed(WHIP_KNOCKBACK)))
					.add(0, Ow.speed(WHIP_LIFT), 0);
				PUSHED.add(new Push(hit, push, Ow.ticks(WHIP_PUSH_TIME),
					HeroManager.hasPerk(player, Perk.BRIGITTE_WHIPLASH) ? player : null));
				dev.overcraft.hero.Combat.pushed(hit, player, dev.overcraft.hero.Blow.ability("whip_shot").by(dev.overcraft.hero.Hero.BRIGITTE));
				Inspire.trigger(player, s, true, INSPIRE_TIME * (HeroManager.hasPerk(player, Perk.BRIGITTE_MORALE_BOOST) ? MORALE_BOOST : 1));
				back(player, s, "whip_return", a.add(s.whipDir.scale(Math.sqrt(best))));
			} else if (block.getType() != HitResult.Type.MISS) {
				BrigitteSounds.at(player.level(), "whip.hit_block", block.getLocation(), 90);
				back(player, s, "whip_return_blocked", block.getLocation());
			} else {
				s.whipOut += reach;
				if (s.whipOut >= WHIP_RANGE - 1.0E-6) {
					back(player, s, "whip_return_miss", s.whipFrom.add(s.whipDir.scale(WHIP_RANGE)));
				}
			}
		}
		if (s.whipOut == -2 && t >= s.whipBackAt) {
			BrigitteSounds.own(player, "whip.catch");
			s.whipTicks = -1;
			s.ability1Cooldown = Ow.ticks(WHIP_COOLDOWN);
		}
	}

	/** Whip Shot's push, held a few ticks (WHIP_PUSH_TIME). */
	private static final class Push {
		final LivingEntity target;
		final Vec3 velocity;
		int left;
		/** Whiplash's Brigitte (the perk), for 0.5 s from the hit; null without it or once it's dealt. */
		@org.jspecify.annotations.Nullable ServerPlayer whiplash;
		int whiplashLeft = Ow.ticks(WHIPLASH_TIME);

		Push(LivingEntity target, Vec3 velocity, int left, @org.jspecify.annotations.Nullable ServerPlayer whiplash) {
			this.target = target;
			this.velocity = velocity;
			this.left = left;
			this.whiplash = whiplash;
		}
	}

	private static final java.util.List<Push> PUSHED = new java.util.ArrayList<>();

	public static void tickPushes() {
		PUSHED.removeIf(p -> {
			if (!p.target.isAlive() || p.target.isRemoved()) {
				return true;
			}
			if (p.whiplash != null && p.whiplashLeft-- > 0 && p.target.horizontalCollision) {
				// Whiplash: into a wall within 0.5 s, 60 more, and it stops there (551F #526)
				float dealt = Combat.damage(p.target, flailSource(p.whiplash), WHIPLASH_DAMAGE, dev.overcraft.hero.Blow.perk("whiplash").by(dev.overcraft.hero.Hero.BRIGITTE));
				Combat.credit(p.whiplash, dealt, p.target);
				Combat.addFireForDamage(p.whiplash, dealt, Ow.toOw(p.target.getMaxHealth()));
				Effects.hitMarker(p.whiplash, p.target, false);
				BrigitteSounds.split(p.whiplash, "perk.whiplash", p.target.getEyePosition(), 45);
				p.whiplash = null;
				p.left = 0;
				p.target.setDeltaMovement(Vec3.ZERO);
				p.target.needsSync = true;
				return true;
			}
			if (p.left-- <= 0) {
				return p.whiplash == null || p.whiplashLeft <= 0;
			}
			Vec3 v = p.target.getDeltaMovement();
			// level speed held, its first lift once (Minecraft's gravity brings them down)
			p.target.setDeltaMovement(p.velocity.x, p.left == Ow.ticks(WHIP_PUSH_TIME) - 1 ? p.velocity.y : v.y, p.velocity.z);
			p.target.needsSync = true;
			return false;
		});
	}

	/** The flail starts back from where its head is: no more hits; the cooldown when it's in her hand (0.3 s). */
	private static void back(ServerPlayer player, HeroState s, String clip, Vec3 head) {
		s.whipOut = -2;
		s.whipBackAt = s.whipTicks + Ow.ticks(WHIP_RETURN);
		Combat.animate(player, clip);
		BrigitteSounds.split(player, "whip.return", player.getEyePosition(), 70);
		Effects.play(player.level(), "brigitte_whip_back", head, Vec3.ZERO, player, Ow.ticks(WHIP_RETURN), player);
	}

	// ---- Repair Pack (0EDD) ----

	/** E: a charge to the ally nearest her aim (10 degrees, 25 m, in sight, not herself). */
	private static void repairPack(ServerPlayer player, HeroState s) {
		if (s.bashTicks >= 0) {
			return;
		}
		if (s.secondaryAmmo <= 0) {
			dev.overcraft.hero.Voice.say(player, "repair_empty", 1);
			return;
		}
		LivingEntity ally = dev.overcraft.hero.Targeting.allyInView(player, REPAIR_RANGE, REPAIR_CONE, true);
		if (ally == null) {
			return;
		}
		// the launch by the charges held as it goes (2EC3-2EC5), her arm's foley; others hear nothing of it (no ranged copy)
		BrigitteSounds.own(player, "repair.throw_" + Math.min(3, (int) s.secondaryAmmo));
		BrigitteSounds.own(player, "repair.cast");
		dev.overcraft.hero.Voice.say(player, "repair_pack", 1);
		s.secondaryAmmo--;
		RepairPack.launch(player, ally);
		Combat.animate(player, "throw");
	}

	/** One charge back every 5 s while she has fewer than three (133A), at the cooldown rate. */
	private static void recharge(HeroState s, double rate) {
		recharge(null, s, rate);
	}

	private static void recharge(@org.jspecify.annotations.Nullable ServerPlayer owner, HeroState s, double rate) {
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
			if (owner != null) {
				BrigitteSounds.own(owner, "repair.recharge_" + Math.min(3, (int) s.secondaryAmmo));
			}
			s.repairRecharge = s.secondaryAmmo < REPAIR_CHARGES ? Ow.ticks(REPAIR_RECHARGE) : -1;
		}
	}

	/** Her quick melee is the flail's swing (1248 #267: no shared quick melee on her). */
	public static void melee(ServerPlayer player, HeroState s) {
		if (s.flailSwing < 0) {
			s.flailMelee = true;
			startSwing(player, s);
		}
	}

	// ---- Rocket Flail (1248) ----

	/** A swing: forehand and backhand in turn (1248 #636: 0 / 2 forehand, 1 backhand). */
	private static void startSwing(ServerPlayer player, HeroState s) {
		s.flailSwing = 0;
		s.flailHit.clear();
		Combat.animate(player, s.flailCombo % 2 == 0 ? "swing" : "swing_2");
		Effects.play(player.level(), "brigitte_swing", player.position(), flat(player), player, s.flailCombo % 2, player);
		BrigitteSounds.split(player, s.flailCombo % 2 == 0 ? "flail.forehand" : "flail.backhand", player.getEyePosition(), 60);
		if (s.flailCombo % 2 == 1) {
			dev.overcraft.hero.Voice.say(player, "flail", 1);
		}
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
			// a melee blow: through enemy barriers (Barrier.isMelee; the wiki lists her flail among them)
			Barrier.meleeDepth++;
			float dealt;
			try {
				dealt = Combat.damage(target, flailSource(player), FLAIL_DAMAGE,
					(s.flailMelee ? dev.overcraft.hero.Blow.MELEE : dev.overcraft.hero.Blow.WEAPON).by(dev.overcraft.hero.Hero.BRIGITTE));
			} finally {
				Barrier.meleeDepth--;
			}
			Combat.credit(player, dealt, target);
			Combat.addFireForDamage(player, dealt, Ow.toOw(target.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(player, target, false);
				BrigitteSounds.split(player, "flail.impact", target.getEyePosition(), 60);
				BrigitteSounds.split(player, "flail.hit", target.getEyePosition(), 60);
				Inspire.trigger(player, s, false, INSPIRE_TIME);
				if (HeroManager.hasPerk(player, Perk.BRIGITTE_COMBAT_MEDIC) && s.repairRecharge > 0) {
					// Combat Medic: the running recharge 0.75 s shorter (floor 0: the charge comes on the next tick)
					s.repairRecharge = Math.max(0, s.repairRecharge - Ow.ticks(COMBAT_MEDIC));
				}
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
			Perk.index(s.majorPerk), 0, (s.barrierUp ? MODE_BARRIER : 0) | (s.rallyTicks > 0 ? MODE_RALLY : 0)
				| (s.barrierLocked ? MODE_BROKEN : 0), 0, 0);
	}

	/**
	 * A player who starts seeing her (Overcraft's START_TRACKING) hears of a barrier already up: it's an action others
	 * heard begin, so one who came into range while she held it saw her without it, its stand or its field (#88).
	 */
	public static void showState(ServerPlayer brig, ServerPlayer viewer) {
		HeroState s = HeroManager.state(brig);
		if (s != null && s.hero == dev.overcraft.hero.Hero.BRIGITTE && s.barrierUp) {
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(viewer,
				new dev.overcraft.net.HeroActionPayload(brig.getId(), "shield_raise"));
		}
	}

	/** Her synced mode's bits (the client's barrier camera, ClientHero): the barrier up, Rally on, broken (locked out). */
	public static final int MODE_BARRIER = 1, MODE_RALLY = 2, MODE_BROKEN = 4;
}
