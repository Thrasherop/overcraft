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
import dev.overcraft.hero.OwMovement;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.Voice;
import dev.overcraft.registry.OcSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
	/** Lifeline is switched on (seeking or towing). */
	public static final int MODE_LIFELINE = 16;
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
		lifelineOff(player, s, false);
	}

	/**
	 * Flight needs Minecraft's permission to fly while she's the hero in play: a dedicated server kicks a player who
	 * floats for 4 s otherwise. Run every tick for every hero player (HeroManager): it hands the permission back to the
	 * game mode as soon as she isn't flying as Jetpack Cat. Out of play (dead, Minecraft mode, another hero), what she
	 * was doing ends too: her tick doesn't run then (Lifeline's tow and its speed outlived her death: found playing).
	 */
	public static void keepFlight(ServerPlayer player, @Nullable HeroState s, boolean controls) {
		boolean cat = s != null && s.hero == Hero.JETPACK_CAT && controls && player.isAlive();
		// Mercy flies in Valkyrie: the same permission (her own kit ends what she does out of play)
		boolean valkyrie = s != null && s.hero == Hero.MERCY && controls && player.isAlive() && dev.overcraft.mercy.Mercy.flying(s);
		if (s != null && !cat && s.hero != Hero.MERCY) {
			if (s.lifelineOn) {
				lifelineOff(player, s, false);
			}
			if (s.ultPhase != ULT_NONE) {
				endUlt(player, s);
			}
			endPurr(s);
		}
		boolean modeAllows = player.isCreative() || player.isSpectator();
		boolean want = cat || valkyrie || modeAllows;
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
			// firing switches Lifeline off (her data's 5A8F: primary fire, as Shift, once it may)
			if (s.lifelineOn && lifelineMayStop(s)) {
				lifelineOff(player, s, true);
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
		// its wound effect (74D0) at their chest
		Effects.play(player.level(), "claws_out", target.position().add(0, target.getBbHeight() * 0.7, 0),
			target.position().subtract(player.position()), target, player.getId(), player);
	}

	private static final net.minecraft.resources.Identifier CLAWS_SLOW = dev.overcraft.Overcraft.id("jetpack_cat_claws_out");

	/** Protection Suzu's cleanse (Combat.cleanse's table): the wound and its slow stop. */
	public static boolean cleanse(LivingEntity target) {
		boolean wounded = WOUNDS.removeIf(w -> w.target == target);
		if (wounded) {
			Combat.buff(target, Attributes.MOVEMENT_SPEED, CLAWS_SLOW, 0, 0);
		}
		return wounded;
	}

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
			double dealt = Combat.damage(w.target, player.damageSources().playerAttack(player), CLAWS_OUT_DAMAGE / Ow.ticks(1),
				dev.overcraft.hero.Blow.perk("claws_out").by(dev.overcraft.hero.Hero.JETPACK_CAT));
			Combat.credit(player, dealt, w.target);
		}
	}

	/** Transport Shielding (minor perk): up to 50 shields when she takes someone along, for 3 s (BZ 2026-06-16). */
	private static void transportShield(ServerPlayer player) {
		if (HeroManager.hasPerk(player, Perk.JETPACK_CAT_TRANSPORT_SHIELDING)) {
			// ours: Minecraft has no shields, so the golden hearts Sound Barrier's use, running down over the 3 s
			Combat.overhealth(player, TRANSPORT_SHIELD, TRANSPORT_SHIELD_TIME);
			// its hexes (5513), which only she sees
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new dev.overcraft.net.EffectPayload("transport_shield", player.getX(), player.getY(),
				player.getZ(), 0, 0, 0, player.getId(), player.getId(), false));
		}
	}

	/** Ulterior Motive (minor perk): 15% of her Pawjectiles' healing comes back as energy. */
	public static void healedWithPaws(Player cat, double healed) {
		HeroState s = HeroManager.state(cat);
		if (s != null && cat instanceof ServerPlayer player && HeroManager.hasPerk(player, Perk.JETPACK_CAT_ULTERIOR_MOTIVE)) {
			s.fuel = Math.min(100, s.fuel + healed * ULTERIOR_MOTIVE);
		}
	}

	// ---- Lifeline: her graph 5A8F (E:\Overwatch Archive\research\jetpack_cat\assets2.md 1) ----
	//
	// A toggle. On (Shift, off cooldown), she flies 30% faster and seeks without end: a player ally in her cylinder
	// opts in with Interact (her data: a "Lifeline" prompt on them; LifelineGrabPayload), a mob ally is caught at once,
	// the nearest (ours: a mob can't press a key). Towed, the ally can jump off (from 0.6 s) or be dragged too far; let
	// go either way, they can't be caught again for 2 s and she seeks on. Shift or primary fire switches it off (from
	// 0.6 s after switching it on, and not within 0.5 s of a catch); its 2 s cooldown runs from then.

	private static final java.util.Map<LivingEntity, Long> LOCKED_OUT = new java.util.WeakHashMap<>();

	/** This ally is in her Lifeline's cylinder, in her sight, not on another rope, not just let go. */
	public static boolean lifelineReaches(ServerPlayer cat, LivingEntity ally) {
		// (asleep or carrying a rider isn't an ally to pull away, ours: a mob can't opt in, and a sleeper flew off its bed, a horse with its rider)
		if (ally == cat || !ally.isAlive() || !Allies.isAlly(cat, ally) || ally.isPassenger() || ally.isVehicle() || ally.isSleeping()) {
			return false;
		}
		Long until = LOCKED_OUT.get(ally);
		if (until != null && cat.level().getServer().getTickCount() < until) {
			return false;
		}
		Vec3 to = ally.position().subtract(cat.position());
		if (to.horizontalDistance() > LIFELINE_REACH || to.y < -LIFELINE_BELOW || to.y > LIFELINE_HEIGHT - LIFELINE_BELOW) {
			return false;
		}
		for (ServerPlayer other : cat.level().getServer().getPlayerList().getPlayers()) {
			HeroState o = HeroManager.state(other);
			if (o != null && other != cat && o.lifelineTarget != null && o.lifelineTarget == ally.getId()) {
				return false;
			}
		}
		Vec3 from = cat.position().add(0, LIFELINE_SIGHT, 0);
		return cat.level().clip(new net.minecraft.world.level.ClipContext(from, ally.getBoundingBox().getCenter(),
			net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, cat))
			.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
	}

	/** The nearest mob ally in reach (players opt in themselves). */
	private static @Nullable LivingEntity lifelineMob(ServerPlayer player) {
		AABB box = player.getBoundingBox().inflate(LIFELINE_REACH, LIFELINE_HEIGHT, LIFELINE_REACH);
		LivingEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box)) {
			double d = e.distanceToSqr(player);
			if (!(e instanceof Player) && d < bestD && lifelineReaches(player, e)) {
				best = e;
				bestD = d;
			}
		}
		return best;
	}

	/** A player ally pressed Interact on her Lifeline's prompt (LifelineGrabPayload): caught, if she's still seeking. */
	public static void grab(ServerPlayer ally, int catId) {
		if (!(ally.level().getEntity(catId) instanceof ServerPlayer cat)) {
			return;
		}
		HeroState s = HeroManager.state(cat);
		if (s != null && s.hero == Hero.JETPACK_CAT && s.lifelineOn && s.lifelineTarget == null && lifelineReaches(cat, ally)) {
			startLifeline(cat, s, ally);
		}
	}

	private static boolean lifelineMayStop(HeroState s) {
		return s.lifelineOnTicks >= Ow.ticks(LIFELINE_OFF_AFTER) && (s.lifelineTarget == null || s.lifelineTicks >= Ow.ticks(LIFELINE_HOLD));
	}

	/**
	 * A player has just started to see her (joined, respawned, came into range): her Lifeline switched on and whoever
	 * she tows or carries, which other players otherwise hear of only as they change (found playing with two players:
	 * an ally who respawned under her got no Lifeline prompt until she switched it off and on), and Catnapper's aim (#96).
	 */
	@Override
	public void showState(ServerPlayer cat, HeroState s, ServerPlayer viewer) {
		if (s.ultPhase == ULT_AIM) {
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(viewer, new dev.overcraft.net.HeroActionPayload(cat.getId(), "ult_aim"));
		}
		if (s.purrTicks >= 0 && !dev.overcraft.hero.HeroManager.options(cat).hidePurrRing()) {
			Effects.sendState(viewer, "purr_state", cat.position(), new Vec3(PURR_TIME - s.purrTicks / 20.0, 0, 0), null, cat.getId(), cat);
		}
		if (s.lifelineOn) {
			Effects.send(viewer, "lifeline_state", cat.position(), null, cat.getId(), cat);
		}
		boolean carrying = s.ultPhase == ULT_CARRY && s.carried != null;
		Integer towed = s.lifelineTarget != null ? s.lifelineTarget : carrying ? s.carried : null;
		Entity on = towed == null ? null : cat.level().getEntity(towed);
		if (on != null) {
			Effects.send(viewer, s.lifelineTarget != null ? "tether_state" : "carry_state", on.position(), on, cat.getId(), cat);
		}
	}

	private static void lifelineOn(ServerPlayer player, HeroState s) {
		s.lifelineOn = true;
		s.lifelineOnTicks = 0;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(LIFELINE_SPEED_BUFF, LIFELINE_SPEED,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		Combat.animate(player, "lifeline");
		// every client shows her seeking (JetpackCatFx: the marker under her, the hook's glow) and offers the prompt
		Effects.play(player.level(), "lifeline_on", player.position(), Vec3.ZERO, null, player.getId(), player);
	}

	/** Switched off (or out of play: no cooldown): any ally let go, the speed gone. */
	private static void lifelineOff(ServerPlayer player, HeroState s, boolean cooldown) {
		boolean was = s.lifelineOn;
		release(player, s);
		s.lifelineOn = false;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(LIFELINE_SPEED_BUFF);
		}
		if (was) {
			Effects.play(player.level(), "lifeline_off", player.position(), Vec3.ZERO, null, player.getId(), player);
			if (cooldown) {
				s.ability1Cooldown = Ow.ticks(LIFELINE_COOLDOWN);
			}
		}
	}

	/** Each tick while it's on: seek, or tow. */
	private static void lifeline(ServerPlayer player, HeroState s) {
		if (!s.lifelineOn) {
			return;
		}
		s.lifelineOnTicks++;
		if (s.lifelineTarget == null) {
			LivingEntity mob = lifelineMob(player);
			if (mob != null) {
				startLifeline(player, s, mob);
			}
			return;
		}
		Entity found = player.level().getEntity(s.lifelineTarget);
		Vec3 apart = found == null ? Vec3.ZERO : player.position().subtract(found.position());
		if (!(found instanceof LivingEntity ally) || !ally.isAlive() || ally.isPassenger()
			|| s.lifelineTicks >= Ow.ticks(LIFELINE_BREAK_GRACE) && (apart.horizontalDistance() > LIFELINE_ROPE * LIFELINE_BREAK
				|| throughTerrain(player, ally, apart))) {
			release(player, s);
			return;
		}
		tow(player, ally);
		if (player.tickCount % 4 == 0) {
			Combat.heal(ally, LIFELINE_HEAL / 5);
		}
		// a towed player lets go with their jump key, from 0.6 s into the tow (her data). D #1014: their vertical speed
		// cancelled (x57643451 = 0) and (F.x x 8, 10.5, F.z x 8) added (xA634D7B9: Juno's and Sojourn's reading, M), F the
		// ally's own facing (STU_021D01CD{self}, in the ally's context: M): what they had across is kept (W: "momentum kept").
		// Read from the keys the client sends: rising faster than her had been the test, and the rope's swing alone let a
		// player go (found playing with a second player)
		s.lifelineTicks++;
		if (ally instanceof ServerPlayer towed && towed.getLastClientInput().jump() && s.lifelineTicks >= Ow.ticks(LIFELINE_DETACH_AFTER)) {
			Vec3 had = towed.getKnownMovement();
			Vec3 facing = towed.getLookAngle().multiply(1, 0, 1);
			facing = facing.lengthSqr() > 1.0E-4 ? facing.normalize() : Vec3.ZERO;
			release(player, s);
			towed.setDeltaMovement(had.x + facing.x * Ow.speed(LIFELINE_LAUNCH_FORWARD), Ow.speed(LIFELINE_LAUNCH_UP),
				had.z + facing.z * Ow.speed(LIFELINE_LAUNCH_FORWARD));
			Combat.send(towed);
		}
	}

	/**
	 * The rope is broken pulled through terrain (W): straight up through a roof the data's across-only test (#928) never
	 * fires, and she took a villager's rope 30 m up through one (found playing). The same 3 lengths, in any direction, with
	 * something solid between them, is ours.
	 */
	private static boolean throughTerrain(ServerPlayer player, LivingEntity ally, Vec3 apart) {
		return apart.length() > LIFELINE_ROPE * LIFELINE_BREAK
			&& player.level().clip(new ClipContext(player.position().add(0, LIFELINE_SIGHT, 0), ally.getBoundingBox().getCenter(),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != net.minecraft.world.phys.HitResult.Type.MISS;
	}

	/** Where a towed mob was last tick and the speed it was last given (blocks a tick), null while it's left to itself. */
	private static final class Towing {
		Vec3 last;
		@Nullable Vec3 sent;

		Towing(Vec3 last) {
			this.last = last;
		}
	}

	private static final java.util.Map<LivingEntity, Towing> TOWING = new java.util.WeakHashMap<>();
	/** Minecraft's air drag a tick, across and down (LivingEntity.travelInAir, decompiled). */
	private static final double MC_AIR_DRAG = 0.91, MC_FALL_DRAG = 0.98;
	/** Ours: a speed past this since last tick (m/s) wasn't a move, a teleport: not taken as its speed. */
	private static final double BLINK_SPEED = 40;
	/** Ours: a change past this (blocks a tick) over what Minecraft's drag explains is a hit, not its AI's little push (0.02 in the air). */
	private static final double HIT_SEEN = 0.1;
	/** Ours: m/s pressing a towed mob to the ground while the rope isn't lifting it (Minecraft's own gravity for a tick). */
	private static final double PRESS_DOWN = 1.6;

	/**
	 * The rope on whoever it tows, this tick (LifelineRope). A player moves themselves: their own client pulls them
	 * (ClientTow), keys and all, and here they're only kept from falls and from the floating kick. A mob moves here, by the
	 * same math with no keys, so a villager on the rope behaves as a player who lets go of the keys.
	 */
	private static void tow(ServerPlayer player, LivingEntity ally) {
		ally.resetFallDistance();
		if (ally instanceof ServerPlayer towed) {
			((dev.overcraft.mixin.ServerGamePacketListenerAccessor) towed.connection).overcraft$setAboveGroundTickCount(0);
			return;
		}
		Towing t = TOWING.computeIfAbsent(ally, a -> new Towing(a.position()));
		Vec3 pos = ally.position();
		Vec3 moved = pos.subtract(t.last).scale(20);
		Vec3 now = ally.getDeltaMovement();
		t.last = pos;
		Vec3 toHer = player.position().subtract(pos);
		boolean taut = LifelineRope.taut(toHer, LIFELINE_ROPE);
		Vec3 her = player.getKnownMovement().scale(20);
		double dt = 0.05 / OwMovement.SUBSTEPS;
		if (ally.isInLiquid()) {
			// the water or lava has them, its own drag and all; the rope only adds to it
			t.sent = null;
			if (taut) {
				Vec3 v = now.scale(20);
				for (int i = 0; i < OwMovement.SUBSTEPS; i++) {
					v = LifelineRope.pull(toHer.subtract(v.subtract(her).scale(i * dt)), her, v, LIFELINE_ROPE, dt);
				}
				ally.setDeltaMovement(v.scale(0.05));
				ally.needsSync = true;
			}
			return;
		}
		if (ally.onGround() && !taut) {
			t.sent = null; // on their own feet inside the rope: Minecraft walks them
			return;
		}
		// the speed we gave it, less what a wall or the ground took. Not the speed it really moved at: that holds its AI's walk,
		// which without Minecraft's friction adds up to 40 m/s² and held a villager against the rope, on its feet (found playing).
		// Picked up from the ground it has no walk to speak of (a player's keys are their own: ClientTow)
		Vec3 v = moved.lengthSqr() > BLINK_SPEED * BLINK_SPEED ? now.scale(20) : moved;
		if (t.sent != null) {
			Vec3 sent = t.sent.scale(20);
			v = new Vec3(ally.horizontalCollision ? moved.x : sent.x, ally.verticalCollision ? moved.y : sent.y,
				ally.horizontalCollision ? moved.z : sent.z);
			// a hit since the last tick shows as a speed that isn't what Minecraft made of the one we gave it
			if (!ally.horizontalCollision && !ally.verticalCollision) {
				Vec3 expected = new Vec3(t.sent.x * MC_AIR_DRAG, (t.sent.y - ally.getGravity()) * MC_FALL_DRAG, t.sent.z * MC_AIR_DRAG);
				Vec3 hit = now.subtract(expected);
				if (hit.lengthSqr() > HIT_SEEN * HIT_SEEN && !ally.onGround()) {
					v = v.add(hit.scale(20));
				}
			}
		}
		for (int i = 0; i < OwMovement.SUBSTEPS; i++) {
			v = LifelineRope.free(v, dt);
			v = LifelineRope.pull(toHer.subtract(v.subtract(her).scale(i * dt)), her, v, LIFELINE_ROPE, dt);
		}
		if (ally.onGround() && v.y <= 0) {
			// the rope isn't lifting them: on the ground, pressed to it as Minecraft keeps a walker (with no gravity here the pull's little
			// upward share hopped them every other tick, found in a trace)
			v = new Vec3(v.x, -PRESS_DOWN, v.z);
		}
		t.sent = v.scale(0.05);
		ally.setDeltaMovement(t.sent);
		ally.needsSync = true;
	}

	private static void startLifeline(ServerPlayer player, HeroState s, LivingEntity ally) {
		s.lifelineTarget = ally.getId();
		s.lifelineTicks = 0;
		transportShield(player);
		HeroSounds.toOwner(player, OcSounds.JC_LIFELINE_ATTACH);
		Voice.say(player, "lifeline", 1);
		// the variant carries her id: every client draws the rope between the two (JetpackCatFx)
		Effects.play(player.level(), "lifeline_attach", ally.position(), Vec3.ZERO, ally, player.getId(), player);
	}

	/** The towed ally let go (Lifeline stays as it was): they land unhurt and can't be caught again for 2 s. */
	private static void release(ServerPlayer player, HeroState s) {
		if (s.lifelineTarget == null) {
			return;
		}
		Effects.play(player.level(), "tether_end", player.position(), Vec3.ZERO, null, player.getId(), player);
		// and whoever was on it, wherever they are: effects only go 96 m round her, and if she's blinked past that their client went
		// on pulling them to her, let go by the server (found on a bot's rope)
		if (player.level().getEntity(s.lifelineTarget) instanceof ServerPlayer towed) {
			Effects.send(towed, "tether_end", player.position(), null, player.getId(), player);
		}
		int target = s.lifelineTarget;
		TOWING.keySet().removeIf(a -> a.getId() == target);
		HeroSounds.toOwner(player, OcSounds.JC_LIFELINE_DETACH);
		if (player.level().getEntity(s.lifelineTarget) instanceof LivingEntity ally) {
			// (the server's own count: /time set moved the world's clock under these, found locking a player out for good)
			LET_GO.put(ally, (long) player.level().getServer().getTickCount() + Ow.ticks(LET_GO_GRACE));
			LOCKED_OUT.put(ally, (long) player.level().getServer().getTickCount() + Ow.ticks(LIFELINE_LOCKOUT));
		}
		s.lifelineTarget = null;
	}

	// ---- Purr ----

	private static void startPurr(ServerPlayer player, HeroState s) {
		s.purrTicks = 0;
		Combat.animate(player, "purr");
		ServerLevel level = player.level();
		// dx 1: her "Hide Purr Range Ring" is on, and the clients make no range wall (70F9)
		boolean hideRing = dev.overcraft.hero.HeroManager.options(player).hidePurrRing();
		Effects.play(level, "purr", player.position(), new Vec3(hideRing ? 1 : 0, 0, 0), player, player.getId(), player);
		HeroSounds.toOwner(player, OcSounds.JC_PURR_CAST);
		Voice.say(player, "purr", 1);
		// the cast: 30 damage and a knockback to enemies in 10 m, full within 5 m, 0.3 of it at 10 m (her data's 5A7E)
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(PURR_RADIUS))) {
			double d = e.distanceTo(player);
			if (e == player || !e.isAlive() || d > PURR_RADIUS || !Allies.areaHits(player, e)) {
				continue;
			}
			double scale = d <= PURR_RADIUS / 2 ? 1 : 1 - 0.7 * (d - PURR_RADIUS / 2) / (PURR_RADIUS / 2);
			float dealt = Combat.damage(e, level.damageSources().playerAttack(player), PURR_DAMAGE * scale, dev.overcraft.hero.Blow.ability("purr").by(dev.overcraft.hero.Hero.JETPACK_CAT));
			Combat.credit(player, dealt, e);
			if (dev.overcraft.brigitte.Barrier.shields(e, player)) {
				// a barrier in the way took it: no push behind it (#88; any barrier: Barriers)
				continue;
			}
			Vec3 out = e.position().subtract(player.position()).multiply(1, 0, 1);
			out = out.lengthSqr() < 1.0E-6 ? player.getViewVector(1.0f).multiply(1, 0, 1) : out.normalize();
			Combat.push(e, out.scale(Ow.speed(PURR_KNOCKBACK) * scale).add(0, Ow.speed(PURR_KNOCKBACK_UP) * scale, 0), player,
				dev.overcraft.hero.Blow.ability("purr").by(dev.overcraft.hero.Hero.JETPACK_CAT));
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
		// a wall's hit point lies on its face, inside the block for the ray down, which then stopped at once: the dive went to a
		// spot on the wall 1.6 m up (#139, played). Stepped back off the face by her half width, the floor in front of it.
		boolean wall = hit instanceof net.minecraft.world.phys.BlockHitResult block && hit.getType() != HitResult.Type.MISS
			&& block.getDirection().getAxis() != net.minecraft.core.Direction.Axis.Y;
		Vec3 at = hit.getType() == HitResult.Type.MISS ? end
			: wall ? hit.getLocation().subtract(player.getViewVector(1.0f).multiply(1, 0, 1).normalize().scale(0.35)) : hit.getLocation();
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
						// her cast (7373) and, after the windup, her dive (7163): the client times the dive from this
						Effects.play(player.level(), "catnapper_cast", player.position(), target.subtract(player.position()), player,
							player.getId(), player);
						if (s.lifelineOn) {
							lifelineOff(player, s, false);
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
					// her data's move (5B7B #95, StateMoveTo): to the target point + 0.25 m up, ending there or at the 1.5 s wait.
					// Nothing in it reads the ground, so cast from the ground she skims along it to the spot (#139: our onGround
					// check ended a ground cast the tick it began). Laid out here, where she's held still: her client moves her and
					// the server's copy of her position lags it, so steering by it each tick overshot the spot by up to 3 ticks
					// (played)
					Vec3 to = s.ultTarget.add(0, 0.25, 0).subtract(player.position());
					s.ultDir = to.lengthSqr() > 1.0E-6 ? to.normalize() : Vec3.ZERO;
					s.ultLeft = to.length();
				}
			}
			case ULT_DIVE -> {
				// ours: a wall in the way ends it (her client's horizontalCollision, from the dive's second tick on, so one she cast
				// beside doesn't)
				boolean blocked = player.horizontalCollision && s.ultTimer < Ow.ticks(DIVE_LIMIT);
				if (s.ultLeft <= 1.0E-3 || blocked || --s.ultTimer <= 0) {
					Vec3 spot = s.ultTarget.add(0, 0.25, 0);
					if (s.ultLeft <= 1.0E-3 && player.level().noCollision(player, player.getBoundingBox().move(spot.subtract(player.position())))) {
						// arrived: onto the spot itself, the move's own end (#95). The steps alone came up 1-1.7 m short (played: her
						// client doesn't take exactly one speed packet a tick)
						player.connection.teleport(new net.minecraft.world.entity.PositionMoveRotation(spot, Vec3.ZERO, 0, 0),
							java.util.Set.of(net.minecraft.world.entity.Relative.X_ROT, net.minecraft.world.entity.Relative.Y_ROT));
					}
					land(player, s);
				} else {
					// 30 m/s, the last part-step onto the spot, landing the tick after
					double d = Math.min(Ow.speed(ULT_DIVE_SPEED), s.ultLeft);
					s.ultLeft -= d;
					player.setDeltaMovement(s.ultDir.scale(d));
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
				// (played on a real client, #109: this alone holds a player up there, y -58.9 under a bot on a ledge, both modes; the 4 s in runs/polish3/p109_carry.txt)
				enemy.syncVelocity = true;
				if (player.tickCount % 4 == 0) {
					Combat.heal(player, ULT_SELF_HEAL / 5);
				}
			}
			default -> {
			}
		}
	}

	/** The landing: 25 damage, a 1.5 s knock-down and a push in 5.5 m; the nearest enemy but a boss is tethered and carried. */
	private static void land(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		player.setDeltaMovement(Vec3.ZERO);
		// to her client at once (Combat.send, #109): on syncVelocity she slid on 2 ticks at 24.6 m/s after landing, now 1 (played)
		Combat.send(player);
		Effects.play(level, "catnapper_land", player.position(), Vec3.ZERO, player, player.getId(), player);
		Combat.animate(player, "ult_land");
		HeroSounds.toOwner(player, OcSounds.JC_ULT_IMPACT);
		LivingEntity nearest = null;
		double nearestD = Double.MAX_VALUE;
		double radius = ULT_RADIUS;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius))) {
			double d = e.distanceTo(player);
			if (e == player || !e.isAlive() || d > radius || !Allies.areaHits(player, e)) {
				continue;
			}
			Combat.credit(player, Combat.damage(e, level.damageSources().playerAttack(player), ULT_DAMAGE, dev.overcraft.hero.Blow.ult("catnapper").by(dev.overcraft.hero.Hero.JETPACK_CAT)), e);
			if (dev.overcraft.brigitte.Barrier.shields(e, player)) {
				// a barrier in the way took it: no knock-down or push behind it (#88; any barrier: Barriers)
				continue;
			}
			// the shockwave's per-target burst (7256), thrown out from her
			Effects.play(level, "catnapper_hit", e.position(), e.position().subtract(player.position()), e, player.getId(), player);
			// the knock-down: "knocked down (stun 1.5 s) and pushed" (W): held as Rally's and Accretion's are (Sleep: no AI, no keys, no
			// shots), and it keeps the push, as a knock-down does
			dev.overcraft.hero.Sleep.knockdown(e, ULT_KNOCKDOWN);
			Vec3 out = e.position().subtract(player.position()).multiply(1, 0, 1);
			if (out.lengthSqr() > 1.0E-6) {
				Vec3 push = out.normalize().scale(Ow.speed(ULT_PUSH));
				Combat.push(e, new Vec3(push.x, 0.1, push.z), player, dev.overcraft.hero.Blow.ult("catnapper").by(dev.overcraft.hero.Hero.JETPACK_CAT));
			}
			// the tether takes the nearest it can carry: a boss is knocked down with the rest and stays where it is (Allies.isBoss, ours)
			// (not one the landing killed, a shulker fixed to its wall, anything riding, or a dart's sleeper Sleep holds still: she'd hang on
			// a tether that doesn't move)
			if (d < nearestD && e.isAlive() && !Allies.isBoss(e) && !e.isPassenger() && !dev.overcraft.hero.Sleep.sleeping(e)
				&& !(e instanceof net.minecraft.world.entity.monster.Shulker)) {
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
			if (!s.lifelineOn && s.ability1Cooldown <= 0) {
				lifelineOn(player, s);
			} else if (s.lifelineOn && lifelineMayStop(s)) {
				lifelineOff(player, s, true);
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
		int mode = s.ultPhase | (s.lifelineTarget != null ? MODE_TOWING : 0) | (s.lifelineOn ? MODE_LIFELINE : 0);
		float left = s.purrTicks >= 0 ? (float) (PURR_TIME - s.purrTicks / 20.0) : s.ultPhase == ULT_CARRY ? s.ultTimer / 20f : 0;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, ULT_COST, s.ultPhase != ULT_NONE,
			s.primaryAmmo, reload, (int) Math.round(s.fuel), -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, mode, left, (float) (s.fuel / 100));
	}

	/** Her widgets (#85): target, who Lifeline tows (0BA3, 5A8F #1097: the towed ally's portrait v59143). Catnapper's carried enemy has none. */
	@Override
	public HeroSyncPayload.Widgets widgets(ServerPlayer player, HeroState s) {
		return new HeroSyncPayload.Widgets(s.lifelineTarget == null ? -1 : s.lifelineTarget, -1, -1, false, 0, 0, 0, 0, "", "");
	}

	/** Out of play (#100: HeroKit.leavePlay): Lifeline lets go and its speed goes, Catnapper's carry ends, Purr stops. */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, dev.overcraft.hero.HeroKit.OutOfPlay why) {
		if (s.lifelineOn) {
			lifelineOff(player, s, false);
		}
		if (s.ultPhase != ULT_NONE) {
			endUlt(player, s);
		}
		endPurr(s);
	}

	/** A Purr cut short still costs its cooldown: its burst has gone out (it would come back at once on G back). */
	private static void endPurr(HeroState s) {
		if (s.purrTicks >= 0) {
			s.ability2Cooldown = Ow.ticks(PURR_COOLDOWN);
		}
		s.purrTicks = -1;
	}
}
