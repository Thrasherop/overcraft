package dev.overcraft.jetpackcat;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.PawEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.Voice;
import dev.overcraft.registry.OcSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.jetpackcat.JetpackCatStats.*;

/**
 * Jetpack Cat (#38): her kit on the server, from her game data (E:\Overwatch Archive\research\jetpack_cat\
 * jetpack_cat-map.md) and the public numbers (tools/reference/jetpack_cat.md; plan: tools/reference/jetpack_cat-plan.md).
 * Her flight itself is movement, which the local player simulates (OwMovement); here are its energy and her abilities.
 * Left button: Biotic Pawjectiles; right button (held): Frenetic Flight; Shift: Lifeline; E: Purr; Q: Catnapper (Q, then
 * the left button to dive).
 */
public final class JetpackCat implements HeroKit {
	private static final Identifier LIFELINE_SPEED_BUFF = Overcraft.id("jetpack_cat_lifeline");

	/** Catnapper's phases (synced as the sync's mode, low bits). */
	public static final int ULT_NONE = 0, ULT_AIM = 1, ULT_WINDUP = 2, ULT_DIVE = 3, ULT_CARRY = 4;
	/** The sync's mode bit for Lifeline towing. */
	public static final int MODE_TOWING = 8;
	/** The dive gives up after this long (her graph 5B7B: a StateWait timing out at 1.5 s inside the move, H). */
	private static final double DIVE_LIMIT = 1.5;
	/** Purr's waves inside a pulse: the k-th gap is 0.25 / (1 + 0.1 k) s (her graph 5A7E's wait #196, read as M). */
	private static double purrWaveGap(int k) {
		return 0.25 / (1 + 0.1 * k);
	}

	/**
	 * When each of Purr's waves heals (ticks after the cast): 4 pulses 0.95 s apart (graph 5A7E: 23AC, over its 4 s,
	 * 2503), pulse n with n waves (Purrfect Form: 3 each). Every wave of a pulse lands, past the 4 s too: the wiki's 300
	 * (360) a Purr counts them all (the 4 s cut the last pulse short: found playing).
	 */
	private static java.util.List<Integer> purrWaveTicks(boolean perfect) {
		java.util.List<Integer> ticks = new java.util.ArrayList<>();
		for (int pulse = 0; pulse < PURR_PULSES.length; pulse++) {
			double t = PURR_PULSE_GAP * (pulse + 1);
			int waves = perfect ? PURRFECT_WAVES : PURR_PULSES[pulse];
			for (int wave = 0; wave < waves; wave++) {
				ticks.add(Ow.ticks(t));
				t += purrWaveGap(wave);
			}
		}
		return ticks;
	}

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = AMMO;
		s.fuel = 100;
		s.ultPhase = ULT_NONE;
		s.purrTicks = -1;
		endLifeline(player, s, false);
	}

	/**
	 * Flight needs Minecraft's permission to fly while she's the hero in play: a dedicated server kicks a player who
	 * floats for 4 s otherwise. Run every tick for every hero player (HeroManager): it hands the permission back to the
	 * game mode as soon as she isn't flying as Jetpack Cat. Out of play (dead, Minecraft mode, another hero), what she
	 * was doing ends too: her tick doesn't run then (Lifeline's tow and its speed outlived her death: found playing).
	 */
	public static void keepFlight(ServerPlayer player, @Nullable HeroState s, boolean controls) {
		boolean cat = s != null && s.hero == Hero.JETPACK_CAT && controls && player.isAlive();
		if (s != null && !cat) {
			if (s.lifelineTarget != null) {
				endLifeline(player, s, false);
			}
			if (s.ultPhase != ULT_NONE) {
				endUlt(player, s);
			}
			s.purrTicks = -1;
		}
		boolean modeAllows = player.isCreative() || player.isSpectator();
		boolean want = cat || modeAllows;
		if (player.getAbilities().mayfly != want) {
			player.getAbilities().mayfly = want;
			if (!want) {
				player.getAbilities().flying = false;
			}
			player.onUpdateAbilities();
		}
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double attack = Combat.attackRate(player);
		double cooldown = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - cooldown);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - cooldown);
		// Claws Out's 6 s: cooldown reduction applies (BZ 2026-04-14)
		s.clawsCooldown = Math.max(0, s.clawsCooldown - cooldown);
		tickWounds(player);

		// Frenetic Flight's energy (graph 59E0): 40 a second burnt while it's held, 25 a second back 0.75 s after,
		// a third of that while towing (BZ Jun 16 2026)
		boolean boosting = controls && s.secondaryHeld && s.fuel > 0 && s.ultPhase != ULT_DIVE;
		if (boosting) {
			s.fuel = Math.max(0, s.fuel - FUEL_BURN * 100 / 20);
			s.fuelIdle = 0;
			if (s.boostTicks++ == 0) {
				// her Frenetic Flight line (1993: 5 s cooldown); its sounds are her client's (JetpackCatSounds)
				Voice.say(player, "boost", 1);
			}
		} else {
			s.boostTicks = 0;
			if (++s.fuelIdle > Ow.ticks(FUEL_DELAY)) {
				double refill = FUEL_REFILL * 100 / 20 * (s.lifelineTarget != null ? FUEL_CARRY_FACTOR : 1);
				s.fuel = Math.min(100, s.fuel + refill);
			}
		}

		reloadAndFire(player, s, controls, attack);
		lifeline(player, s);
		purr(player, s);
		catnapper(player, s, controls);
	}

	private static void reloadAndFire(ServerPlayer player, HeroState s, boolean controls, double attack) {
		if (s.reloadHeld > 0) {
			s.reloadHeld--;
		}
		if (controls && s.primaryAmmo == 0 && s.primaryReload < 0 && s.reloadHeld == 0) {
			startReload(player, s);
		}
		if (s.primaryReload >= 0) {
			s.primaryReload += attack;
			// ammo back at 80% of the reload (her data: 1.0 of 1.25 s)
			if (!s.primaryAmmoRestored && s.primaryReload >= Ow.ticks(RELOAD * 0.8)) {
				s.primaryAmmo = AMMO;
				s.primaryAmmoRestored = true;
			}
			if (s.primaryReload >= Ow.ticks(RELOAD)) {
				s.primaryReload = -1;
			}
		}
		s.weaponRecovery -= attack;
		boolean canFire = s.ultPhase == ULT_NONE || s.ultPhase == ULT_CARRY;
		if (controls && s.primaryHeld && canFire && s.ultPhase != ULT_CARRY && s.weaponRecovery <= 0 && s.primaryAmmo > 0
			&& (s.primaryReload < 0 || s.primaryAmmoRestored)) {
			s.primaryReload = -1;
			s.primaryAmmo--;
			s.weaponRecovery += Ow.ticks(VOLLEY_TIME);
			s.pawSide = !s.pawSide;
			volley(player, s.pawSide);
			// 7847, one per volley: hers in her head, everyone else's at her (38 m)
			HeroSounds.split(player, OcSounds.JC_PAW_FIRE, OcSounds.JC_PAW_FIRE_3P, player.getEyePosition());
			Combat.animate(player, "fire");
			// firing ends Lifeline (the wiki; her data's 5A8F)
			if (s.lifelineTarget != null) {
				endLifeline(player, s, true);
			}
		}
		if (s.weaponRecovery < 0) {
			s.weaponRecovery = 0;
		}
	}

	/** A volley from one paw (they swap each volley): 4 paw shots in an arc above the reticle and the centre shot below. */
	private static void volley(ServerPlayer player, boolean right) {
		ServerLevel level = player.level();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(look).normalize();
		// a little to that paw's side and below the eyes (ours: where her graph spawns the shots isn't decoded); the flare
		// is drawn at her posed paw, hardpoints 000A / 000B (JetpackCatFx)
		Vec3 from = player.getEyePosition().add(side.scale(right ? 0.25 : -0.25)).add(0, -0.25, 0);
		double near = Math.toRadians(PAW_SPREAD_NEAR);
		double far = Math.toRadians(PAW_SPREAD_FAR);
		// the paw arc: far left, near left, near right, far right, a little above the reticle
		double[][] arc = {{-far, near}, {-near, far}, {near, far}, {far, near}};
		for (double[] a : arc) {
			Vec3 dir = look.add(side.scale(Math.tan(a[0]))).add(up.scale(Math.tan(a[1]))).normalize();
			PawEntity paw = new PawEntity(player, level, false);
			paw.launch(from, dir, Ow.speed(PROJECTILE_SPEED));
			level.addFreshEntity(paw);
		}
		PawEntity centre = new PawEntity(player, level, true);
		centre.launch(from, look.add(up.scale(-Math.tan(near))).normalize(), Ow.speed(PROJECTILE_SPEED));
		level.addFreshEntity(centre);
	}

	private static void startReload(ServerPlayer player, HeroState s) {
		s.primaryReload = 0;
		s.primaryAmmoRestored = false;
		Combat.animate(player, "reload");
		HeroSounds.split(player, OcSounds.JC_RELOAD, OcSounds.JC_RELOAD_3P, player.getEyePosition());
		Voice.say(player, "reload", 1);
	}

	/**
	 * Allies she lets go of land unhurt (Overwatch has no falls; dropped from her rope a villager took fall damage:
	 * found playing): until they touch ground, or this long (s, ours).
	 */
	private static final double LET_GO_GRACE = 10;
	private static final java.util.Map<LivingEntity, Long> LET_GO = new java.util.WeakHashMap<>();

	/** Every server tick (HeroManager): the let-go allies' falls don't count until they land. */
	public static void tickLetGo(long now) {
		LET_GO.entrySet().removeIf(e -> {
			LivingEntity ally = e.getKey();
			if (!ally.isAlive() || ally.onGround() || ally.isInWater() || now > e.getValue()) {
				return true;
			}
			ally.resetFallDistance();
			return false;
		});
	}

	// ---- Claws Out (major perk) ----

	/** A Claws Out wound: 40 damage over 1 s (BZ 2026-03-10), dealt a share each tick by the cat who clawed. */
	private record Wound(LivingEntity target, ServerPlayer owner, int[] ticksLeft) {
	}

	private static final java.util.List<Wound> WOUNDS = new java.util.ArrayList<>();

	public static boolean clawsReady(ServerPlayer player, HeroState s) {
		return s.clawsCooldown <= 0 && HeroManager.hasPerk(player, Perk.JETPACK_CAT_CLAWS_OUT);
	}

	public static void clawsUsed(HeroState s) {
		s.clawsCooldown = Ow.ticks(CLAWS_OUT_COOLDOWN);
	}

	/** The wound (40 over 1 s) and the slow (30% for 1 s) on an enemy her empowered quick melee hit. */
	public static void wound(ServerPlayer player, LivingEntity target) {
		WOUNDS.removeIf(w -> w.target == target && w.owner == player);
		WOUNDS.add(new Wound(target, player, new int[] {Ow.ticks(1)}));
		Combat.buff(target, Attributes.MOVEMENT_SPEED, CLAWS_SLOW, -CLAWS_OUT_SLOW, 1);
	}

	private static final net.minecraft.resources.Identifier CLAWS_SLOW = dev.overcraft.Overcraft.id("jetpack_cat_claws_out");

	private static void tickWounds(ServerPlayer player) {
		java.util.Iterator<Wound> it = WOUNDS.iterator();
		while (it.hasNext()) {
			Wound w = it.next();
			if (w.owner != player) {
				continue;
			}
			if (!w.target.isAlive() || w.target.isRemoved() || w.owner.isRemoved() || w.ticksLeft[0]-- <= 0) {
				it.remove();
				continue;
			}
			double dealt = Combat.damage(w.target, player.damageSources().playerAttack(player), CLAWS_OUT_DAMAGE / Ow.ticks(1));
			Combat.credit(player, dealt);
		}
	}

	/** Transport Shielding (minor perk): up to 50 shields when she takes someone along, for 3 s (BZ 2026-06-16). */
	private static void transportShield(ServerPlayer player) {
		if (HeroManager.hasPerk(player, Perk.JETPACK_CAT_TRANSPORT_SHIELDING)) {
			// ours: Minecraft has no shields, so the golden hearts Sound Barrier's use, running down over the 3 s
			Combat.overhealth(player, TRANSPORT_SHIELD, TRANSPORT_SHIELD_TIME);
		}
	}

	/** Ulterior Motive (minor perk): 15% of her Pawjectiles' healing comes back as energy. */
	public static void healedWithPaws(Player cat, double healed) {
		HeroState s = HeroManager.state(cat);
		if (s != null && cat instanceof ServerPlayer player && HeroManager.hasPerk(player, Perk.JETPACK_CAT_ULTERIOR_MOTIVE)) {
			s.fuel = Math.min(100, s.fuel + healed * ULTERIOR_MOTIVE);
		}
	}

	// ---- Lifeline ----

	/** The ally she looks at within reach, else the nearest one (allies opt in in Overwatch: ours, they're taken). */
	private static @Nullable LivingEntity lifelineTarget(ServerPlayer player) {
		AABB box = player.getBoundingBox().inflate(LIFELINE_REACH, LIFELINE_HEIGHT / 2, LIFELINE_REACH);
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box)) {
			if (e == player || !e.isAlive() || !Allies.isAlly(player, e)) {
				continue;
			}
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > LIFELINE_REACH) {
				continue;
			}
			// aimed at counts most: the angle off the reticle, then the distance
			double angle = Math.acos(Math.clamp(to.normalize().dot(look), -1, 1));
			double score = angle * 10 + to.length();
			if (score < bestScore) {
				best = e;
				bestScore = score;
			}
		}
		return best;
	}

	/** While towing: the ally on a 4 m rope behind her, healed 25 a second; her +30% speed. */
	private static void lifeline(ServerPlayer player, HeroState s) {
		if (s.lifelineTarget == null) {
			return;
		}
		Entity found = player.level().getEntity(s.lifelineTarget);
		Vec3 apart = found == null ? Vec3.ZERO : player.position().subtract(found.position());
		if (!(found instanceof LivingEntity ally) || !ally.isAlive()
			|| apart.horizontalDistance() > LIFELINE_ROPE * LIFELINE_BREAK) {
			endLifeline(player, s, true);
			return;
		}
		Vec3 to = player.position().subtract(ally.position());
		double dist = to.length();
		if (dist > LIFELINE_ROPE) {
			// the rope pulls the slack out: her speed, plus a spring on the extra length (ours: the rope node's 30, 2)
			Vec3 pull = to.normalize().scale((dist - LIFELINE_ROPE) * 0.5).add(player.getKnownMovement());
			ally.setDeltaMovement(pull);
			ally.syncVelocity = true;
			ally.resetFallDistance();
		}
		if (player.tickCount % 4 == 0) {
			Combat.heal(ally, LIFELINE_HEAL / 5);
		}
		// a towed player lets go with their jump key, from 0.6 s into the tow (her data), launched along her flight: 8 m/s
		// ahead and 10.5 up (her data's detach launch; the wiki's 13.2 m/s is the two together). Read from the keys the
		// client sends: rising faster than her had been the test, and the rope's swing alone let a player go (found
		// playing with a second player)
		s.lifelineTicks++;
		if (ally instanceof ServerPlayer towed && towed.getLastClientInput().jump() && s.lifelineTicks >= Ow.ticks(0.6)) {
			Vec3 heading = player.getKnownMovement().multiply(1, 0, 1);
			heading = heading.lengthSqr() > 1.0E-4 ? heading.normalize() : player.getLookAngle().multiply(1, 0, 1).normalize();
			endLifeline(player, s, true);
			towed.setDeltaMovement(heading.scale(Ow.speed(LIFELINE_LAUNCH_FORWARD)).add(0, Ow.speed(LIFELINE_LAUNCH_UP), 0));
			towed.syncVelocity = true;
		}
	}

	private static void startLifeline(ServerPlayer player, HeroState s, LivingEntity ally) {
		s.lifelineTarget = ally.getId();
		s.lifelineTicks = 0;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrReplacePermanentModifier(new AttributeModifier(LIFELINE_SPEED_BUFF, LIFELINE_SPEED,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		Combat.animate(player, "lifeline");
		transportShield(player);
		HeroSounds.toOwner(player, OcSounds.JC_LIFELINE_ATTACH);
		Voice.say(player, "lifeline", 1);
		// the variant carries her id: every client draws the rope between the two (JetpackCatFx)
		Effects.play(player.level(), "lifeline_attach", ally.position(), Vec3.ZERO, ally, player.getId(), player);
	}

	private static void endLifeline(ServerPlayer player, HeroState s, boolean cooldown) {
		boolean was = s.lifelineTarget != null;
		if (was) {
			Effects.play(player.level(), "tether_end", player.position(), Vec3.ZERO, null, player.getId(), player);
			HeroSounds.toOwner(player, OcSounds.JC_LIFELINE_DETACH);
			if (player.level().getEntity(s.lifelineTarget) instanceof LivingEntity ally) {
				LET_GO.put(ally, player.level().getGameTime() + Ow.ticks(LET_GO_GRACE));
			}
		}
		s.lifelineTarget = null;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(LIFELINE_SPEED_BUFF);
		}
		if (was && cooldown) {
			s.ability1Cooldown = Ow.ticks(LIFELINE_COOLDOWN);
		}
	}

	// ---- Purr ----

	private static void startPurr(ServerPlayer player, HeroState s) {
		s.purrTicks = 0;
		Combat.animate(player, "purr");
		ServerLevel level = player.level();
		Effects.play(level, "purr", player.position(), player);
		HeroSounds.toOwner(player, OcSounds.JC_PURR_CAST);
		Voice.say(player, "purr", 1);
		// the cast: 30 damage and a knockback to enemies in 10 m, full within 5 m, 0.3 of it at 10 m (her data's 5A7E)
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(PURR_RADIUS))) {
			double d = e.distanceTo(player);
			if (e == player || !e.isAlive() || d > PURR_RADIUS || !Allies.canHurt(player, e)) {
				continue;
			}
			double scale = d <= PURR_RADIUS / 2 ? 1 : 1 - 0.7 * (d - PURR_RADIUS / 2) / (PURR_RADIUS / 2);
			float dealt = Combat.damage(e, level.damageSources().playerAttack(player), PURR_DAMAGE * scale);
			Combat.credit(player, dealt);
			Vec3 out = e.position().subtract(player.position()).multiply(1, 0, 1);
			out = out.lengthSqr() < 1.0E-6 ? player.getViewVector(1.0f).multiply(1, 0, 1) : out.normalize();
			Vec3 push = out.scale(Ow.speed(PURR_KNOCKBACK) * scale).add(0, Ow.speed(PURR_KNOCKBACK_UP) * scale, 0);
			e.push(push.x, push.y, push.z);
			e.syncVelocity = true;
			// a blue-violet flash at its chest (741C)
			Effects.play(level, "purr_hit", e.position().add(0, e.getBbHeight() * 0.6, 0), player);
		}
	}

	/** Purr's healing: 4 pulses 0.95 s apart, pulse n healing n times (Purrfect Form: 3 every pulse), 30 a wave (18 her). */
	private static void purr(ServerPlayer player, HeroState s) {
		if (s.purrTicks < 0) {
			return;
		}
		s.purrTicks++;
		java.util.List<Integer> waves = purrWaveTicks(HeroManager.hasPerk(player, Perk.JETPACK_CAT_PURRFECT_FORM));
		for (int at : waves) {
			// two waves rounding to one tick both heal
			if (s.purrTicks == at) {
				purrWave(player);
			}
		}
		for (int pulse = 1; pulse <= PURR_PULSES.length; pulse++) {
			if (s.purrTicks == Ow.ticks(PURR_PULSE_GAP * pulse)) {
				// each pulse's sound (70F3 -> 71D8), once a pulse
				HeroSounds.toOwner(player, OcSounds.JC_PURR_PULSE);
			}
		}
		if (s.purrTicks >= Math.max(Ow.ticks(PURR_TIME), waves.getLast())) {
			s.purrTicks = -1;
			s.ability2Cooldown = Ow.ticks(PURR_COOLDOWN);
			// 19BA, after the Purr line
			Voice.say(player, "purr_end", 1);
		}
	}

	private static void purrWave(ServerPlayer player) {
		ServerLevel level = player.level();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(PURR_RADIUS))) {
			if (!e.isAlive() || e.distanceTo(player) > PURR_RADIUS || !(e == player || Allies.isAlly(player, e))) {
				continue;
			}
			float healed = Combat.heal(e, e == player ? PURR_SELF_HEAL : PURR_HEAL);
			if (e != player && healed > 0) {
				// gold sparks and a ring at their chest (741D)
				Effects.play(level, "purr_heal", e.position().add(0, e.getBbHeight() * 0.6, 0), player);
				level.playSound(null, e.getX(), e.getY(), e.getZ(), OcSounds.JC_PURR_HEAL, net.minecraft.sounds.SoundSource.PLAYERS,
					1, 1);
			}
			if (e != player) {
				Combat.credit(player, healed);
				Combat.addFireForHealing(player, healed);
			}
		}
		Effects.play(level, "purr_wave", player.position(), player);
	}

	// ---- Catnapper ----

	/**
	 * Where she'd land: the ground she aims at within 25 m (past it, the ground under the 25 m point; ours). The client
	 * marks the same spot while she aims (JetpackCatFx).
	 */
	public static @Nullable Vec3 diveTarget(Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(ULT_RANGE));
		HitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 at = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
		// down to the ground below it
		HitResult ground = player.level().clip(new ClipContext(at.add(0, 0.5, 0), at.add(0, -ULT_RANGE, 0), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		return ground.getType() == HitResult.Type.MISS ? null : ground.getLocation();
	}

	private static void catnapper(ServerPlayer player, HeroState s, boolean controls) {
		switch (s.ultPhase) {
			case ULT_AIM -> {
				if (controls && s.primaryHeld && s.weaponRecovery <= 0) {
					// the left button confirms (her data: primary confirms, M)
					Vec3 target = diveTarget(player);
					if (target != null) {
						s.ultCharge = Combat.afterUlt(s.hero, s.ultCharge);
						s.ultTarget = target;
						s.ultPhase = ULT_WINDUP;
						s.ultTimer = Ow.ticks(0.35);
						s.ultActive = Ow.ticks(0.35 + DIVE_LIMIT + ULT_CARRY);
						Combat.animate(player, "ult_dive");
						HeroSounds.toOwner(player, OcSounds.JC_ULT_DIVE);
						// her Catnapper line: the friendly meow to her side, the enemy one to theirs (1988 / 1989)
						HeroSounds.ultLine(player, OcSounds.JC_ULT_FRIENDLY, OcSounds.JC_ULT_ENEMY, 4);
						Effects.play(player.level(), "catnapper_cast", player.position(), player);
						if (s.lifelineTarget != null) {
							endLifeline(player, s, false);
						}
					}
				}
			}
			case ULT_WINDUP -> {
				player.setDeltaMovement(Vec3.ZERO);
				player.syncVelocity = true;
				if (--s.ultTimer <= 0) {
					s.ultPhase = ULT_DIVE;
					s.ultTimer = Ow.ticks(DIVE_LIMIT);
				}
			}
			case ULT_DIVE -> {
				Vec3 to = s.ultTarget.subtract(player.position());
				double step = Ow.speed(ULT_DIVE_SPEED);
				if (to.length() <= step || player.onGround() || --s.ultTimer <= 0) {
					land(player, s);
				} else {
					player.setDeltaMovement(to.normalize().scale(step));
					player.syncVelocity = true;
					player.resetFallDistance();
				}
			}
			case ULT_CARRY -> {
				Entity found = s.carried == null ? null : player.level().getEntity(s.carried);
				if (!(found instanceof LivingEntity enemy) || !enemy.isAlive() || --s.ultTimer <= 0) {
					endUlt(player, s);
					return;
				}
				// carried below her on the tether, its top 1 m under her feet. Ours: her data reels the rope in to 0.5 m from
				// a point 1 m behind her hips (graph 5B7B #209, hardpoint 0283: M), but held that close (at her feet) a
				// zombie hit her the whole carry and killed her (found playing)
				Vec3 hold = player.position().add(0, -enemy.getBbHeight() - 1.0, 0);
				enemy.setDeltaMovement(hold.subtract(enemy.position()).scale(0.5).add(player.getKnownMovement()));
				enemy.syncVelocity = true;
				if (player.tickCount % 4 == 0) {
					Combat.heal(player, ULT_SELF_HEAL / 5);
				}
			}
			default -> {
			}
		}
	}

	/** The landing: 25 damage, a 1.5 s knock-down and a push in 5.5 m; the nearest enemy is tethered and carried. */
	private static void land(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		player.setDeltaMovement(Vec3.ZERO);
		player.syncVelocity = true;
		Effects.play(level, "catnapper_land", player.position(), player);
		Combat.animate(player, "ult_land");
		HeroSounds.toOwner(player, OcSounds.JC_ULT_IMPACT);
		LivingEntity nearest = null;
		double nearestD = Double.MAX_VALUE;
		double radius = ULT_RADIUS;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius))) {
			double d = e.distanceTo(player);
			if (e == player || !e.isAlive() || d > radius || !Allies.canHurt(player, e)) {
				continue;
			}
			Combat.credit(player, Combat.damage(e, level.damageSources().playerAttack(player), ULT_DAMAGE));
			// the knock-down (ours: Minecraft has no stun, so the heaviest Slowness and Weakness: no moving, no hitting)
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Ow.ticks(ULT_KNOCKDOWN), 9, false, false));
			e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Ow.ticks(ULT_KNOCKDOWN), 9, false, false));
			Vec3 out = e.position().subtract(player.position()).multiply(1, 0, 1);
			if (out.lengthSqr() > 1.0E-6) {
				Vec3 push = out.normalize().scale(Ow.speed(ULT_PUSH));
				e.push(push.x, 0.1, push.z);
				e.syncVelocity = true;
			}
			if (d < nearestD) {
				nearest = e;
				nearestD = d;
			}
		}
		if (nearest != null) {
			s.carried = nearest.getId();
			s.ultPhase = ULT_CARRY;
			s.ultTimer = Ow.ticks(ULT_CARRY);
			transportShield(player);
			HeroSounds.toOwner(player, OcSounds.JC_ULT_TETHER_START);
			Voice.say(player, "tether_attached", 1);
			Effects.play(level, "catnapper_tether", nearest.position(), Vec3.ZERO, nearest, player.getId(), player);
		} else {
			endUlt(player, s);
		}
	}

	private static void endUlt(ServerPlayer player, HeroState s) {
		if (s.carried != null) {
			Effects.play(player.level(), "tether_end", player.position(), Vec3.ZERO, null, player.getId(), player);
			HeroSounds.toOwner(player, OcSounds.JC_ULT_TETHER_END);
			// 19B6, "Unlatch tether"
			Voice.say(player, "tether_end", 1);
		}
		s.ultPhase = ULT_NONE;
		s.carried = null;
		s.ultActive = 0;
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && s.ultPhase == ULT_NONE) {
			if (s.lifelineTarget != null) {
				endLifeline(player, s, true);
			} else if (s.ability1Cooldown <= 0) {
				LivingEntity ally = lifelineTarget(player);
				if (ally != null) {
					startLifeline(player, s, ally);
				}
			}
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && s.purrTicks < 0 && s.ability2Cooldown <= 0) {
			startPurr(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0) {
			if (s.ultPhase == ULT_AIM) {
				s.ultPhase = ULT_NONE;
				Combat.animate(player, "ult_aim_end");
			} else if (s.ultPhase == ULT_CARRY) {
				endUlt(player, s);
			} else if (s.ultPhase == ULT_NONE && s.ultCharge >= ULT_COST) {
				s.ultPhase = ULT_AIM;
				// the confirming click mustn't also be a shot: a moment before the left button counts
				s.weaponRecovery = Math.max(s.weaponRecovery, 2);
				Combat.animate(player, "ult_aim");
				HeroSounds.toOwner(player, OcSounds.JC_ULT_CAST);
				HeroSounds.toOwner(player, OcSounds.JC_ULT_AIM_START);
			}
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && s.primaryAmmo < AMMO && s.primaryReload < 0) {
			startReload(player, s);
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float reload = s.primaryReload >= 0 && !s.primaryAmmoRestored
			? (float) Math.min(1, s.primaryReload / Ow.ticks(RELOAD * 0.8)) : -1;
		int mode = s.ultPhase | (s.lifelineTarget != null ? MODE_TOWING : 0);
		float left = s.purrTicks >= 0 ? (float) (PURR_TIME - s.purrTicks / 20.0) : s.ultPhase == ULT_CARRY ? s.ultTimer / 20f : 0;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, ULT_COST, s.ultPhase != ULT_NONE,
			s.primaryAmmo, reload, (int) Math.round(s.fuel), -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, mode, left, (float) (s.fuel / 100));
	}
}
