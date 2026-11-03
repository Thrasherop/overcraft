package dev.overcraft.ana;

import dev.overcraft.Ow;
import dev.overcraft.entity.AnaDartEntity;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.hero.Voice;
import dev.overcraft.registry.OcSounds;
import net.minecraft.sounds.SoundEvent;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static dev.overcraft.ana.AnaStats.*;

/**
 * Ana's kit (tools/reference/ana-plan.md "Order"), built step by step, each played before the next. Now: the Biotic
 * Rifle (her rifle graphs 0B94 / 0B95): a shot every 0.8 s while the left button is held, 15 to a clip, a 1.5 s reload
 * (the clip full at 0.6 s), automatic at 0 and on R. Unscoped a dart; the right button held, Take Aim: 0.25 s in,
 * 0.167 s out, no shot during either, scoped a hitscan shot, and 65% slower. Her abilities and Nano Boost come next.
 */
public final class Ana implements HeroKit {
	/** Her synced state's flags (HeroManager.ANA [0]): fully scoped; scoping in or out. */
	public static final int FLAG_SCOPED = 1, FLAG_SCOPING = 2;
	private static final net.minecraft.resources.Identifier SCOPE_SLOW = dev.overcraft.Overcraft.id("ana_take_aim");
	private static final net.minecraft.resources.Identifier SPEED_SERUM_ID = dev.overcraft.Overcraft.id("ana_speed_serum");

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = RIFLE_AMMO;
		s.primaryReload = -1;
		s.weaponRecovery = 0;
		s.scope = 0;
		s.castTicks = -1;
		s.sleepCast = -1;
		s.nanoCast = -1;
		publish(player, s);
	}

	/** Leaving Ana (another hero, leaving the world): the scope's slow and her synced state go. */
	public static void clear(ServerPlayer player, HeroState s) {
		s.scope = 0;
		slow(player, false);
		player.removeAttached(HeroManager.ANA);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double attack = Combat.attackRate(player);
		reload(s, attack);
		double cooldown = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - cooldown);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - cooldown);
		boolean live = controls && player.isAlive();
		if (!live) {
			s.castTicks = -1;
			s.sleepCast = -1;
			s.nanoCast = -1;
		} else {
			grenadeCast(player, s);
			sleepCast(player, s);
			nanoCast(player, s);
		}
		// Take Aim: held, with rounds and no reload; let go, a reload or the clip empty takes it down (0B94 #135 / #91 /
		// #189). The shot's recovery pauses while it moves (#63 / #172)
		// (an ability's cast takes the rifle: no scope during it, ours for the grenade)
		boolean wantScope = live && s.secondaryHeld && s.primaryAmmo > 0 && s.primaryReload < 0 && !casting(s);
		double was = s.scope;
		s.scope = wantScope ? Math.min(1, s.scope + 1 / (SCOPE_IN * 20)) : Math.max(0, s.scope - 1 / (SCOPE_OUT * 20));
		// scoping in (1A2F) as it starts to rise, out (1A30) as it starts to fall
		if (wantScope != s.scopeWanted && s.scope != was) {
			if (wantScope) {
				HeroSounds.split(player, OcSounds.ANA_SCOPE_IN, OcSounds.ANA_SCOPE_IN_3P, player.getEyePosition(), 30);
			} else {
				HeroSounds.split(player, OcSounds.ANA_SCOPE_OUT, OcSounds.ANA_SCOPE_OUT_3P, player.getEyePosition(), 30);
			}
		}
		s.scopeWanted = wantScope;
		slow(player, live && s.scope >= 1);
		if (!live) {
			publish(player, s);
			return;
		}
		if (s.scope == was) {
			s.weaponRecovery = Math.max(0, s.weaponRecovery - attack);
		}
		// the clip empty: its reload starts by itself once the shot's recovery is over (0B94 #4 / #10)
		if (s.primaryAmmo == 0 && s.primaryReload < 0 && s.weaponRecovery <= 0) {
			startReload(player, s);
		}
		// no shot at all while scoping in or out (#143 / #475)
		boolean steady = (s.scope == 0 || s.scope >= 1) && !casting(s);
		if (s.primaryHeld && steady && s.primaryReload < 0 && s.weaponRecovery <= 0 && s.primaryAmmo > 0) {
			fire(player, s);
		}
		publish(player, s);
	}

	/** Fully scoped she moves 65% slower (1E61 into the shared slow channel: M). */
	private static void slow(ServerPlayer player, boolean on) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		if (!on) {
			speed.removeModifier(SCOPE_SLOW);
		} else if (!speed.hasModifier(SCOPE_SLOW)) {
			speed.addTransientModifier(new AttributeModifier(SCOPE_SLOW, -SCOPED_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	/** Her state for every client (HeroManager.ANA): [flags, how far scoped (%)]. */
	private static void publish(ServerPlayer player, HeroState s) {
		List<Integer> state = List.of(flags(s), (int) Math.round(s.scope * 100));
		if (!state.equals(player.getAttached(HeroManager.ANA))) {
			player.setAttached(HeroManager.ANA, state);
		}
	}

	private static int flags(HeroState s) {
		return (s.scope >= 1 ? FLAG_SCOPED : 0) | (s.scope > 0 && s.scope < 1 ? FLAG_SCOPING : 0);
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0) {
			startReload(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			startSleepDart(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0) {
			startGrenade(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0) {
			startNano(player, s);
		}
	}

	/** Casting an ability (the grenade's throw, the Sleep Dart's, Nano Boost's): the rifle is down. */
	private static boolean casting(HeroState s) {
		return s.castTicks >= 0 || s.sleepCast >= 0 || s.nanoCast >= 0;
	}

	// ---- Nano Boost (Q: 0B9E) ----

	/**
	 * Q with the charge full: the ally nearest her crosshair within 40 m and 10 degrees, in sight, not already boosted
	 * (Targeting); with none, herself unless she's boosted already (the data nanos her only when her team is just her;
	 * Minecraft's allies are pets and villagers, so they come first: the owner's default). No target, nothing spent. The
	 * charge goes as the cast starts (a Tactician's excess stays: Combat.afterUlt), the boost lands 0.13 s in, the cast
	 * lasts 0.75 s (0B9E #176 / #175). The Requires Target Confirmation option isn't in the mod: its default, off.
	 */
	private static void startNano(ServerPlayer player, HeroState s) {
		if (s.ultCharge < ULT_COST || casting(s)) {
			return;
		}
		LivingEntity target = dev.overcraft.hero.Targeting.allyInView(player, NANO_RANGE, NANO_CONE, true, e -> !Nano.boosted(e));
		if (target == null && !Nano.boosted(player)) {
			target = player;
		}
		if (target == null) {
			return;
		}
		s.ultCharge = Combat.afterUlt(dev.overcraft.hero.Hero.ANA, s.ultCharge);
		s.nanoCast = 0;
		s.nanoTarget = target.getId();
		Combat.animate(player, "cast_nano");
		HeroSounds.split(player, OcSounds.ANA_NANO_CAST, OcSounds.ANA_NANO_CAST_3P, player.getEyePosition(), 75);
		nanoLines(player, target);
		if (HeroManager.hasPerk(player, dev.overcraft.hero.Perk.ANA_SPEED_SERUM)) {
			// Speed Serum (minor): +30% for her from the cast to the boost's end (D 51F0 #401 / 0B9F #317: M)
			Combat.buff(player, Attributes.MOVEMENT_SPEED, SPEED_SERUM_ID, SPEED_SERUM, NANO_START + NANO_TIME);
		}
	}

	/**
	 * Her Nano Boost lines as the cast starts (545C's listener keys, ana.txt's voice section): her own (1836, M: read as
	 * hers) to her, one to the boosted ally (0678, L which listener group), "Nano boost administered!" (05F6) to the rest of
	 * her side and her Arabic line (05F7) to enemies, from her, out to 80 m.
	 */
	private static void nanoLines(ServerPlayer ana, LivingEntity target) {
		HeroSounds.toOwner(ana, line("ana.voice.ultimate"));
		float volume = 80 / 16f;
		for (ServerPlayer listener : ana.level().players()) {
			if (listener == ana || listener.distanceTo(ana) > 88) {
				continue;
			}
			SoundEvent sound = listener == target ? line("ana.voice.nano_cast")
				: Allies.isSelfOrAlly(ana, listener) ? OcSounds.ANA_ULT_FRIENDLY : OcSounds.ANA_ULT_ENEMY;
			listener.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
				net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), net.minecraft.sounds.SoundSource.PLAYERS,
				ana.getX(), ana.getEyeY(), ana.getZ(), volume, 1, ana.getRandom().nextLong()));
		}
	}

	private static SoundEvent line(String name) {
		return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(dev.overcraft.Overcraft.id(name));
	}

	/**
	 * Each tick of the cast: 0.13 s in, the target is healed 250 and boosted for 8 s (Nano), whatever the syringe does on
	 * the way (a visual). An ultimate's own healing builds no ult charge (W): not credited.
	 */
	private static void nanoCast(ServerPlayer player, HeroState s) {
		if (s.nanoCast < 0) {
			return;
		}
		if (s.nanoCast == Ow.ticks(NANO_START) && player.level().getEntity(s.nanoTarget) instanceof LivingEntity target
			&& target.isAlive()) {
			Combat.heal(target, NANO_HEAL);
			Nano.boost(target, player.getUUID());
			// the boost on the ally (5FA8, its bed 5FAA as long as the boost): theirs, their side's, the enemies'
			if (target instanceof ServerPlayer boosted) {
				HeroSounds.toOwner(boosted, OcSounds.ANA_NANO_START);
				HeroSounds.toOwner(boosted, OcSounds.ANA_NANO_LOOP);
			}
			Vec3 at = target.getBoundingBox().getCenter();
			ServerPlayer except = target instanceof ServerPlayer p ? p : null;
			HeroSounds.at(player.level(), OcSounds.ANA_NANO_START_3P, OcSounds.ANA_NANO_START_ENEMY, player, at, 100, except);
			Nano.loopOn(target, player);
			Effects.play(player.level(), "ana_nano_beam", fromEyes(player, WRIST_RIGHT, WRIST_UP, WRIST_AHEAD),
				target.getBoundingBox().getCenter().subtract(fromEyes(player, WRIST_RIGHT, WRIST_UP, WRIST_AHEAD)), null, 0, player);
			Effects.play(player.level(), "ana_nano", at, Vec3.ZERO, target, Ow.ticks(NANO_TIME), player);
			// what the boosted hero says, 1.4 s in (0B9F #227 -> #226)
			if (target instanceof ServerPlayer boosted) {
				later(player.level(), NANO_LINE_AT, () -> {
					if (Nano.boosted(boosted)) {
						Voice.say(boosted, "nano_boosted", 1);
					}
				});
			}
			if (HeroManager.hasPerk(player, dev.overcraft.hero.Perk.ANA_SPEED_SERUM)) {
				Combat.buff(target, Attributes.MOVEMENT_SPEED, SPEED_SERUM_ID, SPEED_SERUM, NANO_TIME);
			}
		}
		if (++s.nanoCast >= Ow.ticks(NANO_CAST)) {
			s.nanoCast = -1;
			s.nanoTarget = -1;
		}
	}

	// ---- Sleep Dart (Shift: 0BDD) ----

	/**
	 * Shift: a 1.0 s cast (the pistol in her right hand), the dart leaving 0.3 s in, the 14 s cooldown from the cast's end,
	 * not from the hit (0BDD #107 / #109 / #87).
	 */
	private static void startSleepDart(ServerPlayer player, HeroState s) {
		if (s.ability1Cooldown > 0 || casting(s)) {
			return;
		}
		s.sleepCast = 0;
		s.ability1Cooldown = Ow.ticks(SLEEP_COOLDOWN + SLEEP_CAST);
		Combat.animate(player, "cast_sleep");
		// 1A00's layers start 0.30-0.42 s in, at the shot: built into the files
		HeroSounds.split(player, OcSounds.ANA_SLEEP_CAST, OcSounds.ANA_SLEEP_CAST_3P, player.getEyePosition(), 100);
	}

	private static void sleepCast(ServerPlayer player, HeroState s) {
		if (s.sleepCast < 0) {
			return;
		}
		if (s.sleepCast == Ow.ticks(SLEEP_FIRE_AT)) {
			ServerLevel level = player.level();
			Vec3 from = fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
			dev.overcraft.entity.SleepDartEntity dart = new dev.overcraft.entity.SleepDartEntity(player, level);
			dart.launch(from, aimPoint(player).subtract(from), Ow.speed(SLEEP_DART_SPEED));
			level.addFreshEntity(dart);
			HeroSounds.at(level, OcSounds.ANA_SLEEP_DART_SHOT, from, 60);
		}
		if (++s.sleepCast >= Ow.ticks(SLEEP_CAST)) {
			s.sleepCast = -1;
		}
	}

	// ---- Biotic Grenade (E: 0B97, its bursts 0B99 / 0C72) ----

	/** E: a 0.7 s throw, the grenade leaving 0.1 s in from her left wrist, the 12 s cooldown from the throw's end. */
	private static void startGrenade(ServerPlayer player, HeroState s) {
		if (s.ability2Cooldown > 0 || casting(s)) {
			return;
		}
		s.castTicks = 0;
		s.ability2Cooldown = Ow.ticks(GRENADE_COOLDOWN + GRENADE_CAST);
		Combat.animate(player, "throw_grenade");
	}

	/** Each tick: the throw's moment. */
	private static void grenadeCast(ServerPlayer player, HeroState s) {
		if (s.castTicks < 0) {
			return;
		}
		if (s.castTicks == Ow.ticks(GRENADE_THROW_AT)) {
			ServerLevel level = player.level();
			Vec3 from = fromEyes(player, WRIST_RIGHT, WRIST_UP, WRIST_AHEAD);
			dev.overcraft.entity.BioticGrenadeEntity grenade = new dev.overcraft.entity.BioticGrenadeEntity(player, level);
			grenade.launch(from, player.getViewVector(1.0f), Ow.speed(GRENADE_SPEED));
			level.addFreshEntity(grenade);
			HeroSounds.split(player, OcSounds.ANA_GRENADE_THROW, OcSounds.ANA_GRENADE_THROW_3P, player.getEyePosition(), 80);
		}
		if (++s.castTicks >= Ow.ticks(GRENADE_CAST)) {
			s.castTicks = -1;
		}
	}

	/** Her left wrist (hp 0002), from her eyes in her look's frame: ours until measured on her model. */
	private static final double WRIST_RIGHT = -0.25, WRIST_UP = -0.2, WRIST_AHEAD = 0.45;

	/**
	 * The burst: everyone within 4 m of it (no falloff: D 119B) and in sight of it (its sight profile 0019, L: ours by a
	 * ray to the centre or the eyes). Allies, Ana too, are healed 90 and their healing raised 50% for 3 s (its own heal
	 * isn't raised); enemies take 90 and can't be healed for 3 s.
	 */
	public static void grenadeBurst(ServerLevel level, ServerPlayer owner, Vec3 at, Entity grenade) {
		grenadeBurst(level, owner, at, grenade, GRENADE_HEAL, GRENADE_DAMAGE);
	}

	/** A burst of {@code heal} / {@code damage}: the throw's (90), or Biotic Bounce's second (60) with the same 4 m and 3 s. */
	public static void grenadeBurst(ServerLevel level, ServerPlayer owner, Vec3 at, Entity grenade, double heal, double damage) {
		DamageSource source = owner.damageSources().thrown(grenade, owner);
		// the burst (384F): hers in her head, everyone else's at it
		HeroSounds.toOwner(owner, OcSounds.ANA_GRENADE_BURST);
		HeroSounds.at(level, OcSounds.ANA_GRENADE_BURST_3P, at, 150, owner);
		Effects.play(level, "ana_grenade_burst", at, Vec3.ZERO, null, heal < GRENADE_HEAL ? 1 : 0, owner);
		boolean healedAny = false, hurtAny = false;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(GRENADE_RADIUS + 2))) {
			if (!e.isAlive() || e.getBoundingBox().distanceToSqr(at) > GRENADE_RADIUS * GRENADE_RADIUS || !inSight(level, at, e)) {
				continue;
			}
			if (Allies.isSelfOrAlly(owner, e)) {
				float healed = Combat.heal(e, heal);
				if (healed > 0) {
					Combat.credit(owner, healed);
					Combat.addFireForHealing(owner, healed);
					healedAny = true;
				}
				Combat.boostHealing(e, GRENADE_EFFECT_TIME);
				Effects.play(level, "ana_boosted", at, Vec3.ZERO, e, Ow.ticks(GRENADE_EFFECT_TIME), owner);
				if (e == owner) {
					HeroSounds.toOwner(owner, OcSounds.ANA_GRENADE_BOOST);
				} else {
					onTarget(owner, e, OcSounds.ANA_GRENADE_BOOST, OcSounds.ANA_GRENADE_BOOST_3P, 45);
				}
			} else if (Allies.canHurt(owner, e)) {
				float dealt = Combat.damage(e, source, damage, dev.overcraft.hero.Blow.ability("biotic_grenade"));
				Combat.credit(owner, dealt);
				Combat.addFireForDamage(owner, dealt, Ow.toOw(e.getMaxHealth()));
				if (dealt > 0) {
					Effects.hitMarker(owner, e, false);
					hurtAny = true;
				}
				Combat.antiHeal(e, GRENADE_EFFECT_TIME);
				Effects.play(level, "ana_anti_healed", at, Vec3.ZERO, e, Ow.ticks(GRENADE_EFFECT_TIME), owner);
				onTarget(owner, e, OcSounds.ANA_GRENADE_ANTIHEAL, OcSounds.ANA_GRENADE_ANTIHEAL_3P, 45);
			}
		}
		// her lines: 0.1 s after a heal (0B99 #232), 0.2 s after damage (0C72 #231)
		if (healedAny) {
			later(level, GRENADE_HEAL_LINE_AT, () -> Voice.say(owner, "grenade_heal", 1));
		}
		if (hurtAny) {
			later(level, GRENADE_DAMAGE_LINE_AT, () -> Voice.say(owner, "grenade_damage", 1));
		}
	}

	private static boolean inSight(ServerLevel level, Vec3 at, LivingEntity e) {
		for (Vec3 to : new Vec3[] {e.getBoundingBox().getCenter(), e.getEyePosition()}) {
			if (level.clip(new ClipContext(at, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}

	private static void fire(ServerPlayer player, HeroState s) {
		s.primaryAmmo--;
		s.weaponRecovery += Ow.ticks(RIFLE_RECOVERY);
		Combat.animate(player, "fire");
		ServerLevel level = player.level();
		// the rifle's cycling after each shot, from its fire layer clips (3842 / 389F)
		HeroSounds.split(player, OcSounds.ANA_BOLT, OcSounds.ANA_BOLT_3P, player.getEyePosition(), 45);
		if (s.scope >= 1) {
			HeroSounds.split(player, OcSounds.ANA_SCOPED_FIRE, OcSounds.ANA_SCOPED_FIRE_3P, player.getEyePosition(), 100);
			HeroSounds.at(level, OcSounds.ANA_SCOPED_SHOT, player.getEyePosition(), 60);
			Effects.play(level, "ana_fire", fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD), player.getViewVector(1.0f), player, 0,
				player);
			hitscan(player);
			return;
		}
		Vec3 from = fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		HeroSounds.split(player, OcSounds.ANA_FIRE, OcSounds.ANA_FIRE_3P, player.getEyePosition(), 100);
		HeroSounds.at(level, OcSounds.ANA_DART_SHOT, from, 60);
		Effects.play(level, "ana_fire", from, player.getViewVector(1.0f), player, 0, player);
		AnaDartEntity dart = new AnaDartEntity(player, level);
		dart.launch(from, aimPoint(player).subtract(from), Ow.speed(DART_SPEED));
		level.addFreshEntity(dart);
	}

	/**
	 * A scoped shot: hitscan from her eyes along her aim to 100 m (the tracer 1615 has no speed: M; D's range 100, L as
	 * metres), the first enemy within 0.04 m or hurt ally within 0.3 m, before any block. Full-health allies and Ana
	 * herself are passed through, as the dart's are (M: the dart's filter, read for the projectile).
	 */
	private static void hitscan(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(SCOPED_RANGE));
		var blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 block = blockHit.getLocation();
		LivingEntity best = null;
		boolean heal = false;
		double bestDist = eye.distanceToSqr(block);
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, block).inflate(1))) {
			if (e == player || !e.isAlive()) {
				continue;
			}
			boolean ally = Allies.isAlly(player, e);
			if (ally && e.getHealth() >= e.getMaxHealth() || !ally && !Allies.canHurt(player, e)) {
				continue;
			}
			double margin = ally ? SCOPED_RADIUS_ALLY : SCOPED_RADIUS_ENEMY;
			java.util.Optional<Vec3> hit = e.getBoundingBox().inflate(margin).clip(eye, block);
			if (hit.isPresent() && hit.get().distanceToSqr(eye) < bestDist) {
				best = e;
				heal = ally;
				bestDist = hit.get().distanceToSqr(eye);
			}
		}
		if (best != null) {
			Vec3 at = best.getBoundingBox().inflate(heal ? SCOPED_RADIUS_ALLY : SCOPED_RADIUS_ENEMY).clip(eye, block).orElse(best.getEyePosition());
			dose(player, best, heal, null, at);
			tracer(player, at);
		} else if (blockHit.getType() == HitResult.Type.BLOCK) {
			HeroSounds.at(player.level(), OcSounds.ANA_HIT_BLOCK, block, 32);
			Effects.play(player.level(), "ana_hit_block", block, Vec3.atLowerCornerOf(blockHit.getDirection().getUnitVec3i()), null, 0,
				player);
			tracer(player, block);
		} else {
			tracer(player, end);
		}
	}

	/** The scoped shot's tracer (1A80), from her muzzle to where it went. */
	private static void tracer(ServerPlayer player, Vec3 to) {
		Vec3 from = fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		Effects.play(player.level(), "ana_tracer", from, to.subtract(from), null, 0, player);
	}

	/** R (not with a full clip or mid-reload), or the clip empty: 1.5 s, the clip full at 0.6 s (0B94 #33 / #72). */
	private static void startReload(ServerPlayer player, HeroState s) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= RIFLE_AMMO || s.weaponRecovery > 0) {
			return;
		}
		s.primaryReload = 0;
		Combat.animate(player, "reload");
		// from an empty clip, or with rounds left (her two reload clips' sets)
		if (s.primaryAmmo == 0) {
			HeroSounds.split(player, OcSounds.ANA_RELOAD, OcSounds.ANA_RELOAD_3P, player.position(), 30);
		} else {
			HeroSounds.split(player, OcSounds.ANA_RELOAD_PARTIAL, OcSounds.ANA_RELOAD_PARTIAL_3P, player.position(), 30);
		}
		Voice.say(player, "reload", 1);
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

	private record Dose(ServerPlayer owner, LivingEntity target, boolean heal, @org.jspecify.annotations.Nullable Entity dart,
						double perTick, int ticks, boolean crit) {
	}

	private static final List<Dose> DOSES = new ArrayList<>();

	/**
	 * A dart or a scoped shot (no dart) reached a hurt ally (heal) or an enemy at {@code at}: its 75 lands over 0.5 s.
	 * Headhunter: an enemy hit in the head takes x2 (0B95 #269; allies are never crit). The head is the top of the box, as
	 * for Kiriko's kunai (ours: 0.25 m under the eyes and up).
	 */
	public static void dose(ServerPlayer owner, LivingEntity target, boolean heal, @org.jspecify.annotations.Nullable Entity dart, Vec3 at) {
		boolean crit = !heal && HeroManager.hasPerk(owner, dev.overcraft.hero.Perk.ANA_HEADHUNTER) && at.y >= target.getEyeY() - HEAD_DEPTH;
		int ticks = Math.max(1, Ow.ticks(RIFLE_OVER));
		double amount = heal ? RIFLE_HEAL : RIFLE_DAMAGE * (crit ? HEADHUNTER : 1);
		DOSES.add(new Dose(owner, target, heal, dart, amount / ticks, ticks, crit));
		HeroSounds.toOwner(owner, OcSounds.ANA_HIT);
		// where it struck, on the side facing her: at their centre the burst was hidden inside their body (found playing)
		Effects.play((ServerLevel) target.level(), heal ? "ana_heal_hit" : "ana_harm_hit", at, Vec3.ZERO, null, 0, owner);
		if (heal) {
			HeroSounds.toOwner(owner, OcSounds.ANA_HEAL);
			onTarget(owner, target, OcSounds.ANA_HEAL_ALLY, OcSounds.ANA_HEAL_3P, 80);
			// as the dart's heal starts (0B95 #217)
			Voice.say(owner, "heal_dart", 1);
		} else {
			HeroSounds.toOwner(owner, OcSounds.ANA_DAMAGE);
			if (crit) {
				HeroSounds.toOwner(owner, OcSounds.ANA_HEADSHOT);
			}
			onTarget(owner, target, OcSounds.ANA_DAMAGE_TARGET, OcSounds.ANA_DAMAGE_3P, 80);
		}
		if (!heal) {
			if (crit) {
				HeroState state = HeroManager.state(owner);
				if (state != null) {
					state.critAt = owner.level().getGameTime();
				}
			}
			Effects.hitMarker(owner, target, crit);
		}
	}

	private static final double HEAD_DEPTH = 0.25;

	/**
	 * What one of her shots or abilities does to {@code target} sounds twice: {@code own} in its head (a player: the healed
	 * ally, the hit enemy, the sleeper), {@code others} at it for everyone else but Ana (who hears her own) out to
	 * {@code metres}.
	 */
	public static void onTarget(ServerPlayer ana, LivingEntity target, SoundEvent own, SoundEvent others, double metres) {
		if (target instanceof ServerPlayer player && player != ana) {
			HeroSounds.toOwner(player, own);
		}
		HeroSounds.at((ServerLevel) target.level(), others, target.getBoundingBox().getCenter(), metres, ana,
			target instanceof ServerPlayer player ? player : ana);
	}

	/** Moments due a little later (her lines after a heal, a sleep, a boost: the graphs' waits). */
	private record Later(long at, Runnable run) {
	}

	private static final List<Later> LATER = new ArrayList<>();

	public static void later(ServerLevel level, double seconds, Runnable run) {
		LATER.add(new Later(level.getGameTime() + Ow.ticks(seconds), run));
	}

	/** Every server tick (HeroManager): the doses in flight land their share. */
	public static void tickDoses(net.minecraft.server.MinecraftServer server) {
		long now = server.overworld().getGameTime();
		new ArrayList<>(LATER).forEach(l -> {
			if (now >= l.at) {
				LATER.remove(l);
				l.run.run();
			}
		});
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
				float dealt = Combat.damage(d.target, source(d), d.perTick, dev.overcraft.hero.Blow.weapon(d.crit));
				Combat.credit(d.owner, dealt);
				Combat.addFireForDamage(d.owner, dealt, Ow.toOw(d.target.getMaxHealth()));
			}
			if (d.ticks > 1) {
				next.add(new Dose(d.owner, d.target, d.heal, d.dart, d.perTick, d.ticks - 1, d.crit));
			}
		}
		DOSES.clear();
		DOSES.addAll(next);
	}

	/**
	 * The dart's own damage, or a scoped shot's: a shot from her with no projectile of its own. Her own blow (her
	 * playerAttack) would make a kill a melee kill (Voice.onDeath reads the direct entity: Superbloom's was, 0.3.22).
	 */
	private static DamageSource source(Dose d) {
		return d.dart != null ? d.owner.damageSources().thrown(d.dart, d.owner) : shotSource(d.owner);
	}

	/** Damage from her with no projectile of its own (a scoped shot, Local Anesthetic's ticks). */
	public static DamageSource shotSource(ServerPlayer owner) {
		return dev.overcraft.hero.Combat.hitscan(owner);
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float reload = s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(RIFLE_RELOAD)) : -1;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, s.nanoCast >= 0,
			s.primaryAmmo, reload, 0, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), 0, flags(s), 0, (float) s.scope);
	}
}
