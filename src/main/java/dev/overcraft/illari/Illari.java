package dev.overcraft.illari;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.CaptiveSunEntity;
import dev.overcraft.entity.HealingPylonEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Voice;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.overcraft.illari.IllariStats.*;

/**
 * Illari's kit (#135; numbers in {@link IllariStats}, the data map tools/reference/illari-map.md). Her buttons by her data's ids: the
 * left button the Solar Rifle (it charges by itself; a press fires from 40 % charge, held it fires at full), the right the healing
 * beam on her solar energy (the left button while beaming is Solar Flare with that perk), Shift Outburst (Jump held goes higher), E
 * Healing Pylon (again: taken back), Q Captive Sun (a launch, then her flight; the left button fires the ball). Survivor: Outburst
 * starts her regeneration at once. The server owns everything; her client flies her in Captive Sun (OwMovement.illariFlight).
 */
public final class Illari implements HeroKit {
	/** The synced mode's bits. */
	public static final int MODE_FLIGHT = 1, MODE_PYLON_OUT = 2, MODE_BEAM = 4, MODE_SILENCED = 8, MODE_BALL_READY = 16,
		MODE_EMPTIED = 32, MODE_SOLSTICE = 64;

	static final Blow RIFLE_BLOW = Blow.weapon(false).by(Hero.ILLARI);
	static final Blow OUTBURST_BLOW = Blow.ability("outburst").by(Hero.ILLARI);
	static final Blow SUNBURN_BLOW = Blow.ability("outburst").by(Hero.ILLARI);
	static final Blow SUN_BLOW = Blow.ult("captive_sun").by(Hero.ILLARI);
	/** How long a launch of hers counts as moving her before the server sees her off the ground (ticks, ours: 0.5 s). */
	static final int LAUNCH_GRACE = 10;
	private static final Identifier SLOW_ID = Overcraft.id("captive_sun_slow");
	private static final DustParticleOptions BEAM_DUST = new DustParticleOptions(0xFFE7A8, 0.6f);
	private static final DustParticleOptions SHOT_DUST = new DustParticleOptions(0xFFC24A, 0.5f);

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		// her pylon outlives a death (HeroKit.leavePlay): the respawn's fresh kit keeps it hers
		UUID pylon = s.illari.pylon;
		var level = s.illari.level;
		s.illari.clear();
		s.illari.pylon = pylon;
		s.illari.level = level;
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		s.weaponRecovery = 0;
		s.ability1Cooldown = 0;
		s.ability2Cooldown = 0;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		IllariState j = s.illari;
		long now = player.level().getGameTime();
		j.silenced = dev.overcraft.sombra.Hack.silenced(player);
		if (j.level != null && j.level != player.level().dimension()) {
			clearWorld(player, j, true);
		}
		j.level = player.level().dimension();
		double rate = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		if (pylon(player, j) == null && j.pylon != null) {
			// it went without us (unloaded, killed off by the game): forgotten, its cooldown runs on
			j.pylon = null;
		}
		// the cooldown runs from the throw, pylon out or not (W: "starts at activation"; D 11FB from the throw)
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		j.flareCooldown = Math.max(0, j.flareCooldown - rate);
		if (s.reloadHeld > 0) {
			s.reloadHeld--;
		}
		boolean able = controls && player.isAlive() && !Sleep.asleep(player);
		boolean leftPress = s.primaryHeld && !j.leftWas;
		j.leftWas = s.primaryHeld;
		sun(player, s, j, able, leftPress, now);
		// her flight for every client's animator (HeroAnimator: the flight clip)
		boolean fly = flying(j);
		if (!java.util.Objects.equals(player.getAttached(HeroManager.ILLARI_FLIGHT), fly)) {
			player.setAttached(HeroManager.ILLARI_FLIGHT, fly);
		}
		pylonCast(player, s, j, able);
		outburstCast(player, s, j, able);
		beam(player, s, j, able, now);
		flare(player, s, j, able, leftPress);
		rifle(player, s, j, able, leftPress, now);
		boolean launched = j.launchGrace > 0;
		if (launched) {
			j.launchGrace = player.onGround() ? j.launchGrace - 1 : 0;
		}
		HeroManager.softLanding(player, s, launched || flying(j), false);
	}

	// ---- Solar Rifle (3A65) ----

	/** Her attack rate: Summer Solstice's +20 % in her flight (D 51F3). */
	static double attackRate(ServerPlayer player, HeroState s) {
		return Combat.attackRate(player) * (flying(s.illari) && s.has(Perk.ILLARI_SUMMER_SOLSTICE) ? 1 + SOLSTICE_SPEED : 1);
	}

	/** The rifle's damage at {@code charge} (0..1): 25 + 50 x the curve (0.4 -> 0, 0.7 -> 0.5, 1 -> 1: D #3042), before falloff. */
	public static double chargedDamage(double charge) {
		double c = Math.max(0, Math.min(1, charge));
		double curve = c <= MIN_CHARGE ? 0 : c <= CURVE_MID_CHARGE ? CURVE_MID * (c - MIN_CHARGE) / (CURVE_MID_CHARGE - MIN_CHARGE)
			: CURVE_MID + (1 - CURVE_MID) * (c - CURVE_MID_CHARGE) / (1 - CURVE_MID_CHARGE);
		return DAMAGE_MIN + (DAMAGE_MAX - DAMAGE_MIN) * curve;
	}

	/** Its falloff's share at {@code metres}: 1 to 30 m, down to 0.3 at 50 m and beyond (D 11C9 / 11CA). */
	public static double falloff(double metres) {
		if (metres <= FALLOFF_START) {
			return 1;
		}
		if (metres >= FALLOFF_END) {
			return FALLOFF_MIN;
		}
		return 1 - (metres - FALLOFF_START) / (FALLOFF_END - FALLOFF_START) * (1 - FALLOFF_MIN);
	}

	/** Whether the left button fires now: a press from 40 % charge, held at full (ours, M: the data's press/hold split not read). */
	public static boolean fires(boolean press, boolean held, double charge) {
		return press && charge >= MIN_CHARGE || held && charge >= 1;
	}

	private static void rifle(ServerPlayer player, HeroState s, IllariState j, boolean able, boolean leftPress, long now) {
		double attack = attackRate(player, s);
		if (s.primaryReload >= 0) {
			s.primaryReload += attack;
			// the ammo back 1.08 s in (D #2862)
			if (s.primaryReload >= RELOAD_AMMO_AT * 20 && s.primaryAmmo < AMMO) {
				s.primaryAmmo = AMMO;
			}
			if (s.primaryReload >= RELOAD * 20) {
				s.primaryReload = -1;
			}
		}
		// it charges by itself, from 0.25 s after a shot (D #2922; not paused by the beam or Captive Sun: BZ 2024-01-09)
		if (j.chargeWait > 0) {
			j.chargeWait = Math.max(0, j.chargeWait - attack);
		} else if (j.charge < 1) {
			double before = j.charge;
			j.charge = Math.min(1, j.charge + attack / (CHARGE_TIME * 20));
			if (before < 1 && j.charge >= 1) {
				IllariSounds.split(player, "rifle.charged", player.getEyePosition(), 10);
			}
		}
		if (!able || j.outburstCast >= 0 || j.pylonCast >= 0 || j.flareCast >= 0 || sunLaunching(j) || ballReady(j)) {
			return;
		}
		if (s.primaryReload < 0 && s.primaryAmmo <= 0 && s.reloadHeld <= 0) {
			// empty: the reload starts by itself
			startReload(player, s);
			return;
		}
		if (j.beaming || s.primaryReload >= 0 || s.primaryAmmo <= 0) {
			return;
		}
		// 0.15 s since her last shot and 0.2 s since the beam (D #544)
		if (now - j.shotAt < Ow.ticks(SHOT_GAP) || now - j.beamAt < Ow.ticks(AFTER_BEAM)) {
			return;
		}
		if (!fires(leftPress, s.primaryHeld, j.charge)) {
			return;
		}
		double charge = j.charge;
		s.primaryAmmo--;
		j.shotAt = now;
		j.charge = 0;
		j.chargeWait = Ow.ticks(RECOVERY);
		shot(player, s, chargedDamage(charge));
		Combat.animate(player, "fire");
		IllariSounds.split(player, charge >= 1 ? "rifle.fire" : "rifle.fire_uncharged", player.getEyePosition(), 80);
	}

	private static void startReload(ServerPlayer player, HeroState s) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= AMMO) {
			return;
		}
		s.primaryReload = 0;
		Combat.animate(player, "reload");
		IllariSounds.split(player, "reload", player.getEyePosition(), 30);
		// the sun in her hand 4F07 (IllariFx: "illari reload sun ball", #150)
		Effects.play(player.level(), "illari_reload", player.getEyePosition(), Vec3.ZERO, player, 0, player);
	}

	/** One hitscan shot from her eyes: the first creature she can hurt within 0.04 m before a block, its falloff, x1.5 on the head. */
	private static void shot(ServerPlayer player, HeroState s, double damage) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = player.getViewVector(1.0f);
		Vec3 far = eye.add(dir.scale(SHOT_RANGE));
		BlockHitResult blockHit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 block = blockHit.getLocation();
		Entity best = null;
		Vec3 bestAt = null;
		double bestD = Double.MAX_VALUE;
		for (Entity e : level.getEntities(player, new AABB(eye, block).inflate(1))) {
			LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
			// a pylon stops her shot only when it's an enemy's: her own and her side's let it through (the critic, 2026-11-14: her own
			// pylon ate her shots)
			boolean pylon = e instanceof HealingPylonEntity p && enemyPylon(player, p);
			if (e instanceof HealingPylonEntity && !pylon) {
				continue;
			}
			if (!pylon && (target == null || !target.isAlive() || !Allies.canHurt(player, target))) {
				continue;
			}
			var hit = e.getBoundingBox().inflate(SHOT_RADIUS).clip(eye, block);
			if (hit.isPresent() && hit.get().distanceTo(eye) < bestD) {
				best = e;
				bestAt = hit.get();
				bestD = hit.get().distanceTo(eye);
			}
		}
		Vec3 stop = block;
		if (best != null) {
			stop = bestAt;
			DamageSource source = Combat.hitscan(player);
			if (best instanceof HealingPylonEntity enemyPylon) {
				enemyPylon.hurtServer(level, source, Ow.hp(damage * falloff(bestD)));
			} else {
				LivingEntity target = best instanceof EnderDragonPart part ? part.parentMob : (LivingEntity) best;
				boolean crit = dev.overcraft.hero.Headshot.crossed(target, bestAt, dir, SHOT_RADIUS);
				double ow = damage * falloff(bestD) * (crit ? CRIT : 1);
				float dealt = best instanceof EnderDragonPart part ? Combat.damagePart(level, part, source, ow, RIFLE_BLOW.crit(crit))
					: Combat.damage(target, source, ow, RIFLE_BLOW.crit(crit));
				dealt(player, dealt, target, crit);
				if (crit && dealt > 0) {
					s.critAt = level.getGameTime();
				}
				IllariSounds.split(player, "rifle.hit_enemy", bestAt, 60);
			}
		} else if (blockHit.getType() == HitResult.Type.BLOCK) {
			IllariSounds.at(level, "rifle.hit_world", block, 30);
		}
		// her shot's band 48BE and muzzle star 4BA3, then the hit 4BA1 (IllariFx, #150)
		Vec3 from = muzzle(player);
		Effects.play(level, "illari_shot", from, stop.subtract(from), player, 0, player);
		Effects.play(level, "illari_hit", stop, Vec3.ZERO, null, 0, player);
	}

	// ---- the healing beam (3A65) ----

	private static void beam(ServerPlayer player, HeroState s, IllariState j, boolean able, long now) {
		boolean wants = able && s.secondaryHeld && !j.emptied && j.energy > 0 && j.outburstCast < 0 && j.pylonCast < 0 && !sunLaunching(j)
			&& s.primaryReload < 0;
		if (wants) {
			if (!j.beaming) {
				IllariSounds.split(player, "beam.start", player.getEyePosition(), 40);
				Combat.animate(player, "beam");
			}
			j.beaming = true;
			j.beamAt = now;
			j.energy = Math.max(0, j.energy - ENERGY_DRAIN / 20.0);
			j.energyWait = Ow.ticks(ENERGY_REGEN_DELAY);
			LivingEntity target = beamTarget(player);
			Vec3 from = muzzle(player);
			Vec3 to;
			if (target != null) {
				to = target.getBoundingBox().getCenter();
				float healed = Combat.heal(target, BEAM_HEAL / 20.0);
				if (healed > 0) {
					Combat.credit(player, healed);
					Combat.addFireForHealing(player, healed);
				}
				if (j.beamTarget != target.getId()) {
					if (target.getHealth() < target.getMaxHealth() * 0.4) {
						// "You're hurt. Let me see." on an ally under 40 % (D 3A65 #2804)
						Voice.say(player, "beam_hurt_ally", 1);
					}
					IllariSounds.split(player, "beam.hit", to, 30);
				}
				j.beamTarget = target.getId();
			} else {
				Vec3 eye = player.getEyePosition();
				to = player.level().clip(new ClipContext(eye, eye.add(player.getViewVector(1.0f).scale(BEAM_RANGE)), ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, player)).getLocation();
				j.beamTarget = -1;
			}
			if (player.tickCount % 2 == 0) {
				trail(player.level(), from, to, BEAM_DUST, 0.6);
			}
			if (j.energy <= 0) {
				// emptied: locked until full, its regeneration from 1.0 s later (D #2560 / #2565 / #2573)
				j.emptied = true;
				j.energyWait = Ow.ticks(ENERGY_EMPTY_DELAY);
				Voice.say(player, "beam_empty", 1);
				endBeam(player, j);
			}
			return;
		}
		if (j.beaming) {
			endBeam(player, j);
		}
		if (j.energyWait > 0) {
			j.energyWait--;
		} else if (j.energy < ENERGY) {
			j.energy = Math.min(ENERGY, j.energy + ENERGY_REGEN / 20.0);
			if (j.energy >= ENERGY) {
				if (j.emptied) {
					IllariSounds.split(player, "beam.full", player.getEyePosition(), 10);
				}
				j.emptied = false;
			}
		}
	}

	private static void endBeam(ServerPlayer player, IllariState j) {
		j.beaming = false;
		j.beamTarget = -1;
		IllariSounds.split(player, "beam.stop", player.getEyePosition(), 40);
		Combat.animate(player, "beam_end");
	}

	/** The beam's ally: the first along her aim within 20 m whose box, 0.25 m wider, the beam crosses before a block (D #1763 / #1795). */
	static @Nullable LivingEntity beamTarget(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(player.getViewVector(1.0f).scale(BEAM_RANGE));
		Vec3 block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		LivingEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, block).inflate(1))) {
			if (!Allies.isAlly(player, e)) {
				continue;
			}
			var hit = e.getBoundingBox().inflate(BEAM_RADIUS).clip(eye, block);
			if (hit.isPresent() && hit.get().distanceTo(eye) < bestD) {
				best = e;
				bestD = hit.get().distanceTo(eye);
			}
		}
		return best;
	}

	// ---- Solar Flare (51F3) ----

	private static void flare(ServerPlayer player, HeroState s, IllariState j, boolean able, boolean leftPress) {
		if (j.flareCast >= 0) {
			if (!able) {
				j.flareCast = -1;
				return;
			}
			if (++j.flareCast >= Ow.ticks(FLARE_CAST)) {
				j.flareCast = -1;
				flareHeal(player);
			}
			return;
		}
		if (able && leftPress && j.beaming && s.has(Perk.ILLARI_SOLAR_FLARE) && j.flareCooldown <= 0) {
			j.flareCast = 0;
			j.flareCooldown = Ow.ticks(FLARE_COOLDOWN);
			Combat.animate(player, "solar_flare");
			IllariSounds.split(player, "flare", player.getEyePosition(), 40);
		}
	}

	/** 100 to every ally within 20 m and 40 degrees of her aim, in her sight (D 51F3 #453 / #482: the cone read as its whole width, M). */
	private static void flareHeal(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(FLARE_RANGE))) {
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			if (!Allies.isAlly(player, e) || to.length() > FLARE_RANGE
				|| Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, to.normalize().dot(look))))) > FLARE_CONE / 2
				|| !player.hasLineOfSight(e)) {
				continue;
			}
			float healed = Combat.heal(e, FLARE_HEAL);
			if (healed > 0) {
				Combat.credit(player, healed);
				Combat.addFireForHealing(player, healed);
			}
			IllariSounds.at(player.level(), "flare.hit", e.getBoundingBox().getCenter(), 30);
			player.level().sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 8, 0.3, 0.4, 0.3, 0.03);
		}
	}

	// ---- Healing Pylon (3DF4) ----

	/**
	 * Her cooldown: 7 s from the throw (D 11FB; W "redeploy 7"), 14 s from its destruction by enemies (D 11FC; W 14); Rapid Construction
	 * 2 s off each, 5 / 12 (BZ 2026-06-16 "reduced by 2 seconds", 1.5 -> 2; the data's 6 kept out: patch notes first).
	 */
	static double pylonCooldown(HeroState s, boolean hit) {
		boolean rapid = s.has(Perk.ILLARI_RAPID_CONSTRUCTION);
		return hit ? rapid ? RAPID_COOLDOWN_HIT : PYLON_COOLDOWN_HIT : rapid ? RAPID_COOLDOWN : PYLON_COOLDOWN;
	}

	private static void pylonCast(ServerPlayer player, HeroState s, IllariState j, boolean able) {
		if (j.pylonCast < 0) {
			return;
		}
		if (!able) {
			j.pylonCast = -1;
			return;
		}
		int t = ++j.pylonCast;
		if (t == Ow.ticks(PYLON_THROW_AT)) {
			HealingPylonEntity old = pylon(player, j);
			if (old != null) {
				// one at a time: the new one takes the old one's place (W: "a new pylon destroys the old one")
				endPylon(player, s, j, old, true);
			}
			s.ability2Cooldown = Ow.ticks(pylonCooldown(s, false));
			Vec3 from = offset(player, 0.25, -0.1, 0.4);
			HealingPylonEntity pylon = new HealingPylonEntity(player, player.level(), s.has(Perk.ILLARI_RAPID_CONSTRUCTION) ? RAPID_BUILD : 1);
			pylon.launch(from, player.getViewVector(1.0f), Ow.speed(PYLON_SPEED));
			pylon.setYRot(player.getYRot());
			player.level().addFreshEntity(pylon);
			j.pylon = pylon.getUUID();
			IllariSounds.split(player, "pylon.throw", from, 40);
			Voice.say(player, "pylon", 1);
		}
		if (t >= Ow.ticks(PYLON_CAST)) {
			j.pylonCast = -1;
		}
	}

	/** It set down on a block: its build starts. */
	public static void pylonLanded(ServerPlayer owner, HealingPylonEntity pylon) {
		HeroState s = HeroManager.state(owner);
		boolean rapid = s != null && s.has(Perk.ILLARI_RAPID_CONSTRUCTION);
		IllariSounds.all((ServerLevel) pylon.level(), "pylon.land", pylon.position(), 30);
		IllariSounds.all((ServerLevel) pylon.level(), rapid ? "pylon.build_fast" : "pylon.build", pylon.position(), 30);
	}

	public static void pylonBuilt(ServerPlayer owner, HealingPylonEntity pylon) {
		pylonHum(pylon);
	}

	/** How often its hum starts again (ticks): the shortest of its one-shot takes, 24 s (illari_sfx.txt pylon.loop; ours the restart). */
	public static final int HUM_EVERY = 24 * 20;

	/** Its hum: a long one-shot in the data, cut and started again every 24 s while it stands (the critic, 2026-11-14). */
	public static void pylonHum(HealingPylonEntity pylon) {
		stopAround(pylon, "pylon.loop");
		IllariSounds.all((ServerLevel) pylon.level(), "pylon.loop", pylon.position(), 20);
	}

	/** It's gone (broken, replaced, taken away with her): its hum stops (it played on 24-38 s after it went). */
	public static void pylonGone(HealingPylonEntity pylon) {
		stopAround(pylon, "pylon.loop");
	}

	/** Stops sound "illari." + {@code name} for every player within 48 m of {@code at} (the hum's reach and more). */
	private static void stopAround(Entity at, String name) {
		net.minecraft.sounds.SoundEvent sound = IllariSounds.of(name);
		if (sound == null || !(at.level() instanceof ServerLevel level)) {
			return;
		}
		for (ServerPlayer listener : level.players()) {
			if (listener.distanceTo(at) < 48) {
				listener.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(sound.location(),
					net.minecraft.sounds.SoundSource.PLAYERS));
			}
		}
	}

	/** Its pick: the ally (her included) in 15 m and in its sight with the lowest health share, short of full (D 3DF5, M the ordering). */
	public static @Nullable LivingEntity pylonTarget(ServerPlayer owner, HealingPylonEntity pylon) {
		ServerLevel level = (ServerLevel) pylon.level();
		Vec3 eye = pylon.eye();
		LivingEntity best = null;
		double bestShare = 1;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, pylon.getBoundingBox().inflate(PYLON_RANGE))) {
			if (!Allies.isSelfOrAlly(owner, e) || !e.isAlive() || e.getHealth() >= e.getMaxHealth()) {
				continue;
			}
			Vec3 to = e.getBoundingBox().getCenter();
			if (to.distanceTo(eye) > PYLON_RANGE) {
				continue;
			}
			if (level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pylon)).getType() != HitResult.Type.MISS
				&& level.clip(new ClipContext(eye, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pylon)).getType()
				!= HitResult.Type.MISS) {
				continue;
			}
			double share = e.getHealth() / e.getMaxHealth();
			if (share < bestShare) {
				best = e;
				bestShare = share;
			}
		}
		return best;
	}

	/** A heal shot leaves the pylon toward {@code target} (its 55 m/s flight drawn as a line of light: ours). */
	public static void pylonFired(ServerPlayer owner, HealingPylonEntity pylon, LivingEntity target) {
		ServerLevel level = (ServerLevel) pylon.level();
		trail(level, pylon.eye(), target.getBoundingBox().getCenter(), BEAM_DUST, 0.5);
		Combat.animateOthers(owner, "pylon_fire");
		IllariSounds.all(level, "pylon.heal", pylon.eye(), 30);
	}

	/** The shot reaches {@code target}: 50, 25 on her (BZ 2026-11-03; D 1204 / 1206). */
	public static void pylonHealLands(ServerPlayer owner, HealingPylonEntity pylon, LivingEntity target) {
		float healed = Combat.heal(target, target == owner ? PYLON_SELF_HEAL : PYLON_HEAL);
		if (healed > 0) {
			Combat.credit(owner, healed);
			Combat.addFireForHealing(owner, healed);
		}
		ServerLevel level = (ServerLevel) pylon.level();
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 4, 0.3, 0.4, 0.3, 0);
		IllariSounds.all(level, "pylon.heal_hit", target.getBoundingBox().getCenter(), 20);
	}

	public static void pylonHit(ServerPlayer owner, HealingPylonEntity pylon, @Nullable Entity by) {
		ServerLevel level = (ServerLevel) pylon.level();
		level.sendParticles(ParticleTypes.CRIT, pylon.getX(), pylon.getY() + 0.4, pylon.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
	}

	/** Broken by her enemies: her 14 s cooldown (it was hit), her line (D 3DF5 #428 -> 13E1). */
	public static void pylonDestroyed(ServerPlayer owner, HealingPylonEntity pylon) {
		HeroState s = HeroManager.state(owner);
		ServerLevel level = (ServerLevel) pylon.level();
		IllariSounds.split(owner, "pylon.destroyed", pylon.position(), 40);
		level.sendParticles(ParticleTypes.EXPLOSION, pylon.getX(), pylon.getY() + 0.4, pylon.getZ(), 1, 0, 0, 0, 0);
		if (s != null && s.hero == Hero.ILLARI && pylon.getUUID().equals(s.illari.pylon)) {
			s.illari.pylon = null;
			s.ability2Cooldown = Ow.ticks(pylonCooldown(s, true));
			Voice.say(owner, "pylon_destroyed", 1);
		}
	}

	/** A new one replaces it: the old one goes (its cooldown is the new throw's). */
	private static void endPylon(ServerPlayer player, HeroState s, IllariState j, HealingPylonEntity pylon, boolean voice) {
		IllariSounds.split(player, "pylon.removed", pylon.position(), 40);
		pylon.discard();
		j.pylon = null;
		if (voice) {
			Voice.say(player, "pylon_moved", 1);
		}
	}

	// ---- Outburst (3B38) ----

	private static void outburstCast(ServerPlayer player, HeroState s, IllariState j, boolean able) {
		if (j.outburstCast < 0) {
			return;
		}
		int t = ++j.outburstCast;
		if (t == Ow.ticks(OUTBURST_AT)) {
			if (able) {
				Input in = player.getLastClientInput();
				outburst(player, s, j, in.jump());
			}
		}
		if (t >= Ow.ticks(OUTBURST_CAST)) {
			j.outburstCast = -1;
		}
	}

	/**
	 * Her launch's velocity (m/s): standing, 6 straight up on a tap, 13.5 held; moving, 13.5 along (x, 0.6, z) tapped or (x, 2, z) held,
	 * (x, z) her move's direction (D 0EC2 / 0EC3, 3B38).
	 */
	public static Vec3 outburstVelocity(double moveX, double moveZ, boolean held) {
		double h = Math.sqrt(moveX * moveX + moveZ * moveZ);
		if (h < 1.0E-6) {
			return new Vec3(0, held ? OUTBURST_HELD : OUTBURST_TAP, 0);
		}
		Vec3 dir = new Vec3(moveX / h, held ? OUTBURST_HELD_RISE : OUTBURST_TAP_RISE, moveZ / h).normalize();
		return dir.scale(OUTBURST_HELD);
	}

	private static void outburst(ServerPlayer player, HeroState s, IllariState j, boolean held) {
		ServerLevel level = player.level();
		Vec3 v = outburstVelocity(j.outburstX, j.outburstZ, held || j.outburstHeld);
		// an impulse on top of her own motion (D #812 STU_FF3471E9: an impulse, not a set speed): with her 5.5 m/s run it comes to W's
		// 18.4 m/s tapped and 16.7 held (the critic, 2026-11-14: set, it was 13.5); a fall's speed it takes away first, as a jump does
		Vec3 own = Combat.motion(player);
		Combat.launch(player, new Vec3(own.x, Math.max(0, own.y), own.z).add(v.scale(1 / 20.0)), player, OUTBURST_BLOW);
		s.softLanding = true;
		j.launchGrace = LAUNCH_GRACE;
		// Survivor (01C7 #2564, tag 1173): Outburst starts her regeneration at once (message 26FD)
		s.lastDamagedTick = Math.min(s.lastDamagedTick, Combat.now(player) - Combat.REGEN_DELAY);
		Vec3 at = player.getBoundingBox().getCenter();
		boolean hit = false;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(OUTBURST_RADIUS))) {
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (e == player || d > OUTBURST_RADIUS || !e.isAlive() || !Allies.areaHits(player, e) || !Combat.inSight(level, at, e, player)
				|| Barriers.between(level, at, e.getBoundingBox().getCenter(), player) != null) {
				continue;
			}
			float dealt = Combat.damage(e, Combat.blast(level, player, player), OUTBURST_DAMAGE, OUTBURST_BLOW);
			dealt(player, dealt, e, false);
			double k = share(d, OUTBURST_FULL, OUTBURST_RADIUS, OUTBURST_EDGE);
			Vec3 away = new Vec3(e.getX() - at.x, 0, e.getZ() - at.z);
			away = away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize();
			Combat.push(e, away.scale(Ow.speed(OUTBURST_AWAY * k)).add(0, Ow.speed(OUTBURST_UP * k), 0), player, OUTBURST_BLOW);
			// off the ground at once: Minecraft takes a creature's ground friction on the tick a push lands while it still stands, and an
			// 18 m/s shove went ~2 m (the critic, 2026-11-14)
			if (!(e instanceof net.minecraft.world.entity.player.Player)) {
				e.setOnGround(false);
			}
			if (s.has(Perk.ILLARI_SUNBURN)) {
				burn(player, e);
			}
			hit = true;
			IllariSounds.split(player, "outburst.hit_enemy", e.getBoundingBox().getCenter(), 30);
		}
		level.sendParticles(ParticleTypes.FLAME, at.x, player.getY() + 0.2, at.z, 30, 1.2, 0.2, 1.2, 0.05);
		level.sendParticles(BEAM_DUST, at.x, player.getY() + 0.2, at.z, 30, 1.5, 0.2, 1.5, 0);
		Voice.say(player, hit ? "outburst_hit" : "outburst", 1);
	}

	/** The push's share at {@code d}: 1 to {@code full}, down to {@code edge} at {@code radius} (linear: M). */
	public static double share(double d, double full, double radius, double edge) {
		if (d >= radius) {
			return edge;
		}
		return d <= full ? 1 : 1 - (d - full) / (radius - full) * (1 - edge);
	}

	// ---- Sunburn and the Sunstruck marks (server-wide) ----

	private record Burn(UUID owner, long until, long next) {
	}

	private static final class Mark {
		final UUID owner;
		long until;
		double taken;
		long goesAt = -1;
		Vec3 last;

		Mark(UUID owner, long until, Vec3 at) {
			this.owner = owner;
			this.until = until;
			this.last = at;
		}
	}

	private static final Map<UUID, Burn> BURNS = new HashMap<>();
	/** The ball's own damage landing now: not counted toward a mark (onDamaged). */
	private static boolean ballDamaging;
	private static final Map<UUID, Mark> MARKS = new HashMap<>();
	private static final Map<UUID, LivingEntity> MARKED = new HashMap<>();
	private static final Map<UUID, LivingEntity> BURNING = new HashMap<>();

	private static void burn(ServerPlayer owner, LivingEntity e) {
		long now = owner.level().getGameTime();
		BURNS.put(e.getUUID(), new Burn(owner.getUUID(), now + Ow.ticks(SUNBURN_TIME), now + 10));
		BURNING.put(e.getUUID(), e);
		IllariSounds.all(owner.level(), "outburst.burn", e.getBoundingBox().getCenter(), 20);
	}

	/** Every server tick: Sunburn's 50 over 3 s in sixths, every half second; the marks' slows, their damage and their blasts. */
	public static void tickAll(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Burn>> it = BURNS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Burn> en = it.next();
			LivingEntity e = BURNING.get(en.getKey());
			Burn b = en.getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(b.owner());
			if (e == null || !e.isAlive() || owner == null) {
				it.remove();
				BURNING.remove(en.getKey());
				continue;
			}
			long now = e.level().getGameTime();
			if (now >= b.next()) {
				float dealt = Combat.damage(e, Combat.blast(e.level(), null, owner), SUNBURN_DAMAGE / 6, SUNBURN_BLOW);
				dealt(owner, dealt, e, false);
				if (e.level() instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.SMALL_FLAME, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
				}
				if (b.next() + 10 > b.until()) {
					it.remove();
					BURNING.remove(en.getKey());
				} else {
					en.setValue(new Burn(b.owner(), b.until(), b.next() + 10));
				}
			}
		}
		for (Iterator<Map.Entry<UUID, Mark>> it = MARKS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Mark> en = it.next();
			Mark m = en.getValue();
			LivingEntity e = MARKED.get(en.getKey());
			ServerPlayer owner = server.getPlayerList().getPlayer(m.owner);
			if (e == null || owner == null) {
				it.remove();
				MARKED.remove(en.getKey());
				continue;
			}
			long now = e.level().getGameTime();
			if (e.isAlive()) {
				m.last = e.getBoundingBox().getCenter();
			} else if (m.goesAt < 0) {
				// it died marked: Sunstruck goes off where it was (W)
				m.goesAt = now + Ow.ticks(SUNSTRUCK_DELAY);
			}
			if (m.goesAt >= 0 && now >= m.goesAt) {
				it.remove();
				MARKED.remove(en.getKey());
				unslow(e);
				sunstruck(owner, (ServerLevel) e.level(), m.last);
				continue;
			}
			if (m.goesAt < 0 && now >= m.until) {
				it.remove();
				MARKED.remove(en.getKey());
				unslow(e);
				continue;
			}
			if (e.isAlive()) {
				double into = (now - (m.until - Ow.ticks(MARK_TIME))) / 20.0;
				Combat.buff(e, Attributes.MOVEMENT_SPEED, SLOW_ID, -markSlow(into), 0.2);
				if (now % 20 == 0 && e.level() instanceof ServerLevel level) {
					// its mark 4B50, each second, growing with what it has stored (IllariFx, #150)
					int stored = (int) Math.round(100 * Math.min(1, m.taken / SUNSTRUCK_AT));
					Effects.play(level, "illari_mark", e.getBoundingBox().getCenter(), Vec3.ZERO, null, stored, level.getServer().getPlayerList().getPlayer(m.owner));
				}
			}
		}
	}

	/** The mark's slow {@code seconds} in: 40 % for 1 s, then easing to none at its end (D 3DFF, M linear). */
	public static double markSlow(double seconds) {
		if (seconds < 0 || seconds >= MARK_TIME) {
			return 0;
		}
		return seconds <= MARK_SLOW_HOLD ? MARK_SLOW : MARK_SLOW * (1 - (seconds - MARK_SLOW_HOLD) / (MARK_TIME - MARK_SLOW_HOLD));
	}

	private static void unslow(LivingEntity e) {
		Combat.buff(e, Attributes.MOVEMENT_SPEED, SLOW_ID, 0, 0);
	}

	/** Damage a marked creature took (Overcraft's AFTER_DAMAGE, Minecraft points): 90 of it sets Sunstruck off 0.8 s later (D 1A34). */
	public static void onDamaged(LivingEntity e, float damage) {
		if (ballDamaging) {
			// the ball's own direct hit and splash don't count (W), even on one its direct hit has just marked (replayed 2026-11-14:
			// the splash after the pass-through counted 50, and a 40 set it off)
			return;
		}
		Mark m = MARKS.get(e.getUUID());
		if (m == null || m.goesAt >= 0) {
			return;
		}
		m.taken += Ow.toOw(damage);
		if (sunstruckReady(m.taken)) {
			m.goesAt = e.level().getGameTime() + Ow.ticks(SUNSTRUCK_DELAY);
			if (e.level() instanceof ServerLevel level) {
				IllariSounds.all(level, "sun.trigger", e.getBoundingBox().getCenter(), 40);
			}
		}
	}

	/** Whether the damage a mark took sets it off (90 or more: D 1A34). */
	public static boolean sunstruckReady(double taken) {
		return taken >= SUNSTRUCK_AT;
	}

	/** Sunstruck's damage at {@code d} m: 160 inside 1 m down to 120 at 5 m, none past (D 1212 / 1213, 1D3A / 1D3B). */
	public static double sunstruckDamage(double d) {
		if (d > SUNSTRUCK_RADIUS) {
			return 0;
		}
		if (d <= SUNSTRUCK_FULL) {
			return SUNSTRUCK_DAMAGE;
		}
		return SUNSTRUCK_DAMAGE - (SUNSTRUCK_DAMAGE - SUNSTRUCK_EDGE_DAMAGE) * (d - SUNSTRUCK_FULL) / (SUNSTRUCK_RADIUS - SUNSTRUCK_FULL);
	}

	private static void sunstruck(ServerPlayer owner, ServerLevel level, Vec3 at) {
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(SUNSTRUCK_RADIUS + 2))) {
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d > SUNSTRUCK_RADIUS || !e.isAlive() || !Allies.areaHits(owner, e) || !Combat.inSight(level, at, e, owner)) {
				continue;
			}
			float dealt = Combat.damage(e, Combat.blast(level, null, owner), sunstruckDamage(d), SUN_BLOW);
			dealt(owner, dealt, e, false);
		}
		// its detonation 4E17 (IllariFx, #150)
		Effects.play(level, "illari_sunstruck", at, Vec3.ZERO, null, 0, owner);
		IllariSounds.blast(owner, level, "sun.explode", at, 120);
	}

	// ---- Captive Sun (3CAB) ----

	static boolean flying(IllariState j) {
		return j.sun >= Ow.ticks(SUN_REFILL_AT) && j.sun < j.flightTicks;
	}

	static boolean sunLaunching(IllariState j) {
		return j.sun >= 0 && j.sun < Ow.ticks(SUN_LAUNCH);
	}

	/** The ball still in her hand once the launch is done (the left button fires it). */
	static boolean ballReady(IllariState j) {
		return j.sun >= Ow.ticks(SUN_LAUNCH) && !j.ballFired;
	}

	/** Her flight's speed (m/s) for her client, 0 when she isn't flying: 5.5 x 1.45, +20 % with Summer Solstice (D 1D3D; 51F3). */
	public static double flightSpeed(boolean solstice) {
		return dev.overcraft.hero.OwMovement.RUN_SPEED * SUN_FLIGHT_SPEED * (solstice ? 1 + SOLSTICE_SPEED : 1);
	}

	/** Knockback she takes: halved in her flight (D 1DF9). */
	public static double knockbackTaken(HeroState s) {
		return s.hero == Hero.ILLARI && flying(s.illari) ? SUN_KNOCK_TAKEN : 1;
	}

	private static void castSun(ServerPlayer player, HeroState s, IllariState j) {
		s.ultCharge = Combat.afterUlt(Hero.ILLARI, s.ultCharge);
		j.sun = 0;
		j.ballFired = false;
		j.flightTicks = Ow.ticks(SUN_FLIGHT + (s.has(Perk.ILLARI_SUMMER_SOLSTICE) ? SOLSTICE_TIME : 0));
		s.ultActive = j.flightTicks;
		j.outburstCast = -1;
		j.pylonCast = -1;
		Combat.launch(player, new Vec3(0, Ow.speed(SUN_LAUNCH_UP), 0), player, SUN_BLOW);
		s.softLanding = true;
		j.launchGrace = LAUNCH_GRACE;
		Combat.animate(player, "ult_launch");
		IllariSounds.split(player, "sun.cast", player.getEyePosition(), 80);
		// the sun forming in her hand 4EF3 (IllariFx, #150)
		Effects.play(player.level(), "illari_cast", player.getEyePosition(), Vec3.ZERO, player, 0, player);
		ultLine(player);
	}

	private static void sun(ServerPlayer player, HeroState s, IllariState j, boolean able, boolean leftPress, long now) {
		if (j.sun < 0) {
			return;
		}
		int t = ++j.sun;
		if (t == Ow.ticks(SUN_REFILL_AT)) {
			// the beam's energy and the ammo full (D #1076 -> 007D; BZ 2024-01-09 the overheat cleared)
			j.energy = ENERGY;
			j.emptied = false;
			s.primaryAmmo = AMMO;
			s.primaryReload = -1;
			IllariSounds.split(player, "sun.flight", player.getEyePosition(), 60);
		}
		if (t == Ow.ticks(SUN_LAUNCH)) {
			IllariSounds.split(player, "sun.armed", player.getEyePosition(), 40);
			// the sun held at her rifle 4EF2 until it's thrown (IllariFx, #150)
			Effects.play(player.level(), "illari_armed", player.getEyePosition(), Vec3.ZERO, player, 0, player);
		}
		if (!j.ballFired && t >= Ow.ticks(SUN_LAUNCH) && (able && leftPress || t >= Ow.ticks(SUN_AUTO_FIRE))) {
			fireBall(player, j);
		}
		s.ultActive = Math.max(0, j.flightTicks - t);
		if (t >= j.flightTicks) {
			endSun(player, s, j);
		}
	}

	private static void fireBall(ServerPlayer player, IllariState j) {
		j.ballFired = true;
		// the press that fired the ball isn't also a rifle shot (played 2026-11-14: the same tick fired both, 14 -> 13)
		j.shotAt = player.level().getGameTime();
		j.chargeWait = Ow.ticks(RECOVERY);
		j.charge = 0;
		Vec3 from = muzzle(player);
		CaptiveSunEntity ball = new CaptiveSunEntity(player, player.level());
		ball.launch(from, aimPoint(player, 200).subtract(from), Ow.speed(BALL_SPEED));
		player.level().addFreshEntity(ball);
		Combat.animate(player, "ball_fire");
		IllariSounds.split(player, "sun.fire", from, 80);
		// the throw 4F06 (IllariFx, #150)
		Effects.play(player.level(), "illari_throw", from, aimPoint(player, 200).subtract(from), player, 0, player);
	}

	private static void endSun(ServerPlayer player, HeroState s, IllariState j) {
		j.sun = -1;
		j.ballFired = false;
		s.ultActive = 0;
		// her flight's sound is one long shot: cut at the flight's end (illari-map.md "Open")
		stopForAll(player, "sun.flight");
		stopForAll(player, "sun.flight_3p");
	}

	/** Stops sound "illari." + {@code name} for everyone in her world. */
	private static void stopForAll(ServerPlayer player, String name) {
		net.minecraft.sounds.SoundEvent sound = IllariSounds.of(name);
		if (sound == null) {
			return;
		}
		for (ServerPlayer listener : player.level().players()) {
			listener.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(sound.location(),
				net.minecraft.sounds.SoundSource.PLAYERS));
		}
	}

	/**
	 * The ball passed through {@code e} (W: it passes through enemies): its 5 direct, once each, and the mark (D 1D38). The ball flies on.
	 */
	public static void ballDirect(ServerPlayer owner, CaptiveSunEntity ball, Entity direct) {
		ServerLevel level = (ServerLevel) ball.level();
		LivingEntity target = direct instanceof EnderDragonPart part ? part.parentMob : direct instanceof LivingEntity l ? l : null;
		if (target == null || !Allies.canHurt(owner, target)) {
			return;
		}
		ballDamaging = true;
		float dealt;
		try {
			dealt = direct instanceof EnderDragonPart part ? Combat.damagePart(level, part, level.damageSources().thrown(ball, owner), BALL_DIRECT, SUN_BLOW)
				: Combat.damage(target, level.damageSources().thrown(ball, owner), BALL_DIRECT, SUN_BLOW);
		} finally {
			ballDamaging = false;
		}
		dealt(owner, dealt, target, false);
		mark(owner, target);
	}

	/**
	 * The ball bursts at {@code at} (terrain, a barrier, its 10 s): 50 to every enemy within 10 m in sight, each marked (D 3CB1; BZ
	 * 2024-12-10). Marked after the damage: the ball's own direct hit and splash don't count toward Sunstruck's 90 (W).
	 */
	public static void ballBurst(ServerPlayer owner, CaptiveSunEntity ball, Vec3 at) {
		ServerLevel level = (ServerLevel) ball.level();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(BALL_RADIUS + 2))) {
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d > BALL_RADIUS || !e.isAlive() || !Allies.areaHits(owner, e) || !Combat.inSight(level, at, e, owner)
				|| Barriers.between(level, at, e.getBoundingBox().getCenter(), owner) != null) {
				continue;
			}
			ballDamaging = true;
			float dealt;
			try {
				dealt = Combat.damage(e, Combat.blast(level, ball, owner), BALL_SPLASH, SUN_BLOW);
			} finally {
				ballDamaging = false;
			}
			dealt(owner, dealt, e, false);
			mark(owner, e);
		}
		// its burst 4E27 (IllariFx, #150)
		Effects.play(level, "illari_burst", at, Vec3.ZERO, null, 0, owner);
		IllariSounds.all(level, "sun.impact", at, 150);
	}

	/**
	 * Marks {@code e} for 6 s: one mark at a time; a second hit refreshes its time and keeps what it has taken; none while one is
	 * going off (W).
	 */
	static void mark(ServerPlayer owner, LivingEntity e) {
		if (!e.isAlive()) {
			return;
		}
		long now = e.level().getGameTime();
		Mark old = MARKS.get(e.getUUID());
		if (old != null) {
			if (old.goesAt < 0) {
				old.until = now + Ow.ticks(MARK_TIME);
			}
			return;
		}
		MARKS.put(e.getUUID(), new Mark(owner.getUUID(), now + Ow.ticks(MARK_TIME), e.getBoundingBox().getCenter()));
		MARKED.put(e.getUUID(), e);
		IllariSounds.at((ServerLevel) e.level(), "sun.mark", e.getBoundingBox().getCenter(), 30);
		// the mark's eight-point sun stamped (4B50's 3F2CB, IllariFx: variant 1000)
		Effects.play((ServerLevel) e.level(), "illari_mark", e.getBoundingBox().getCenter(), Vec3.ZERO, null, 1000, owner);
	}

	/** Captive Sun's line: her own copy in her head, the allies' and the enemies' from her (D 3CAB #1084, as Junkrat's). */
	private static void ultLine(ServerPlayer hero) {
		net.minecraft.sounds.SoundEvent own = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("illari.voice.ultimate"));
		net.minecraft.sounds.SoundEvent friendly = IllariSounds.of("ult.friendly"), enemy = IllariSounds.of("ult.enemy");
		if (own != null) {
			HeroSounds.toOwner(hero, own);
		}
		for (ServerPlayer listener : hero.level().players()) {
			net.minecraft.sounds.SoundEvent line = Allies.isSelfOrAlly(hero, listener) ? friendly : enemy;
			if (listener != hero && line != null) {
				listener.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
					net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(line), net.minecraft.sounds.SoundSource.PLAYERS,
					hero.getX(), hero.getEyeY(), hero.getZ(), 5, 1.0f, hero.getRandom().nextLong()));
			}
		}
	}

	// ---- presses ----

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		IllariState j = s.illari;
		if (Sleep.asleep(player) || !player.isAlive()) {
			return;
		}
		boolean busy = j.outburstCast >= 0 || j.pylonCast >= 0 || sunLaunching(j);
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && !j.silenced && !busy && s.ability1Cooldown <= 0) {
			Input in = player.getLastClientInput();
			double yaw = Math.toRadians(player.getYRot());
			Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
			Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
			double f = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0), l = (in.left() ? 1 : 0) - (in.right() ? 1 : 0);
			Vec3 move = forward.scale(f).add(left.scale(l));
			j.outburstX = move.x;
			j.outburstZ = move.z;
			j.outburstHeld = in.jump();
			j.outburstCast = 0;
			s.ability1Cooldown = Ow.ticks(OUTBURST_COOLDOWN);
			Combat.animate(player, "outburst");
			IllariSounds.split(player, "outburst", player.getEyePosition(), 60);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && !j.silenced && !busy) {
			HealingPylonEntity pylon = pylon(player, j);
			// with one out, E places a new one once the cooldown allows (W: the hero option "Interact cancels Healing Pylon", on by default),
			// but not within 3 s of the old one being hit (W; D 1D36)
			if (s.ability2Cooldown <= 0 && (pylon == null || !pylon.recentlyHit())) {
				j.pylonCast = 0;
				Combat.animate(player, "pylon_throw");
			}
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && !j.silenced && j.sun < 0 && !busy && s.ultCharge >= ULT_COST) {
			castSun(player, s, j);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && !ballReady(j)) {
			startReload(player, s);
		}
	}

	// ---- her things in the world ----

	/** Whether {@code pylon} is against {@code hero}: its owner is no ally of theirs (one with no owner here: nobody's, so not). */
	public static boolean enemyPylon(LivingEntity hero, HealingPylonEntity pylon) {
		return pylon.getOwner() instanceof LivingEntity owner && owner != hero && !Allies.isSelfOrAlly(hero, owner);
	}

	/** Whether a pylon is still the one she has out (not once she's another hero or has thrown another). Unknown while she's away: kept. */
	public static boolean stillHis(Entity placed, @Nullable Entity owner) {
		if (!(owner instanceof ServerPlayer player)) {
			return true;
		}
		HeroState s = HeroManager.state(player);
		return s != null && s.hero == Hero.ILLARI && placed.getUUID().equals(s.illari.pylon);
	}

	static @Nullable HealingPylonEntity pylon(ServerPlayer player, IllariState j) {
		return j.pylon != null && player.level().getEntity(j.pylon) instanceof HealingPylonEntity p && p.isAlive() ? p : null;
	}

	private static void clearWorld(ServerPlayer player, IllariState j, boolean all) {
		UUID me = player.getUUID();
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (Entity e : level.getAllEntities()) {
				if ((e instanceof HealingPylonEntity || all && e instanceof CaptiveSunEntity)
					&& e instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() != null && me.equals(p.getOwner().getUUID())) {
					e.discard();
				}
			}
		}
		j.pylon = null;
	}

	// ---- aim and looks ----

	private static void dealt(ServerPlayer owner, float dealt, LivingEntity e, boolean crit) {
		Combat.credit(owner, dealt, e);
		Combat.addFireForDamage(owner, dealt, e);
		if (dealt > 0) {
			Effects.hitMarker(owner, e, crit);
		}
	}

	private static void trail(ServerLevel level, Vec3 from, Vec3 to, DustParticleOptions dust, double step) {
		Vec3 d = to.subtract(from);
		double len = d.length();
		int n = (int) Math.min(40, Math.max(1, len / step));
		for (int k = 1; k <= n; k++) {
			Vec3 p = from.add(d.scale(k / (double) n));
			level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	private static Vec3 muzzle(ServerPlayer player) {
		return offset(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
	}

	private static Vec3 offset(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 top = side.cross(look).normalize();
		Vec3 from = eye.add(side.scale(right)).add(top.scale(up)).add(look.scale(ahead));
		if (player.level().clip(new ClipContext(eye, from, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			return eye;
		}
		return from;
	}

	/** What her crosshair is on (a creature first, then a block), else {@code range} m out. */
	private static Vec3 aimPoint(ServerPlayer player, double range) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(player.getViewVector(1.0f).scale(range));
		Vec3 target = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		net.minecraft.world.phys.EntityHitResult onIt = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,
			player, eye, target, new AABB(eye, target).inflate(1.0), e -> e != player && e.isPickable() && !e.isSpectator()
				&& !(e instanceof dev.overcraft.entity.OwProjectile), 0.0f);
		if (onIt != null) {
			target = onIt.getLocation();
		}
		return target.distanceTo(eye) < 1.5 ? far : target;
	}

	// ---- sync and leaving play ----

	@Override
	public HeroSyncPayload sync(HeroState s) {
		IllariState j = s.illari;
		int mode = (flying(j) ? MODE_FLIGHT : 0) | (j.pylon != null ? MODE_PYLON_OUT : 0) | (j.beaming ? MODE_BEAM : 0)
			| (j.silenced ? MODE_SILENCED : 0) | (ballReady(j) ? MODE_BALL_READY : 0) | (j.emptied ? MODE_EMPTIED : 0)
			| (s.has(Perk.ILLARI_SUMMER_SOLSTICE) ? MODE_SOLSTICE : 0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, j.sun >= 0,
			s.primaryAmmo, s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / (RELOAD * 20)) : -1,
			0, (float) j.charge, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero, s.onFire,
			Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk), (float) (j.flareCooldown / 20.0),
			mode, j.sun >= 0 ? (float) Math.max(0, (j.flightTicks - j.sun) / 20.0) : 0, (float) (j.energy / ENERGY));
	}

	/** Out of play (#100): her casts cut, the beam off, Captive Sun ended (its charge spent); the pylon stays through G and death. */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, HeroKit.OutOfPlay why) {
		IllariState j = s.illari;
		j.pylonCast = -1;
		j.outburstCast = -1;
		j.flareCast = -1;
		if (j.beaming) {
			endBeam(player, j);
		}
		if (j.sun >= 0) {
			endSun(player, s, j);
		}
		if (why.forGood()) {
			clearWorld(player, j, false);
		}
	}
}
