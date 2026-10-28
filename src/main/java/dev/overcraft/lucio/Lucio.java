package dev.overcraft.lucio;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.SonicEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.Voice;
import dev.overcraft.registry.OcSounds;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.lucio.LucioStats.*;

/**
 * Lúcio, server side (tools/reference/lucio-plan.md). The left button fires the Sonic Amplifier's volleys, the right
 * button Soundwave; Shift Crossfade, E Amp It Up, Q Sound Barrier, R reload. His songs reach allies around him every
 * 0.2 s. Wall Ride is movement, on the client (OwMovement); it tells the server while he rides.
 */
public final class Lucio implements HeroKit {
	private static final Identifier SPEED_SONG_BUFF = Overcraft.id("lucio_speed_song");

	/**
	 * His song lines wait this long after he's picked or spawns (the game's: the swap's criteria event is gated for 2.5 s
	 * after a game message, voice_sounds_effects.md).
	 */
	private static final double SONG_LINE_GATE = 2.5;
	/** His line after this long on a wall (graph 0263's wait before event 1348). */
	private static final double WALLRIDE_LINE_AFTER = 2.2;
	/**
	 * The wall ride's loop, played again as each turn ends while he rides: his own 2.776 s, the one others hear 5.0 s (the
	 * decoded banks' loop lengths, tools/soundmaps/lucio.txt).
	 */
	private static final int WALLRIDE_LOOP_TICKS = 56;
	private static final int WALLRIDE_LOOP_3P_TICKS = 100;

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = AMP_AMMO;
		s.songLinesFrom = player.level().getGameTime() + Ow.ticks(SONG_LINE_GATE);
	}

	/** His song for every client (LucioMusic): none outside hero mode or dead, else heal 1 / speed 2, +4 amped. */
	private static void syncSong(ServerPlayer player, HeroState s, boolean controls) {
		int song = !controls || !player.isAlive() ? 0 : (s.speedSong ? 2 : 1) | (s.ampUp > 0 ? 4 : 0);
		Integer had = player.getAttached(dev.overcraft.hero.HeroManager.SONG);
		if (had == null || had != song) {
			player.setAttached(dev.overcraft.hero.HeroManager.SONG, song);
		}
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double attack = Combat.attackRate(player);
		double cooldown = Combat.cooldownRate(player);

		// Amp It Up starts 0.2 s after the press (the game data), runs, then its cooldown starts (the wiki: "starting after
		// the duration ends")
		if (s.ampDelay > 0 && --s.ampDelay == 0) {
			s.ampUp = Ow.ticks(AMP_UP_TIME);
			// its sound (effect A45) and his line (03B2, event 1344), as it starts
			HeroSounds.split(player, OcSounds.LUCIO_AMP_IT_UP, OcSounds.LUCIO_AMP_IT_UP_3P, player.position());
			Voice.say(player, "amp", 1);
		}
		if (s.ampUp > 0) {
			if (--s.ampUp == 0) {
				if (!s.ampFree) {
					s.ability2Cooldown = Ow.ticks(AMP_UP_COOLDOWN);
				}
				s.ampFree = false;
			}
		} else if (s.ampDelay == 0) {
			s.ability2Cooldown = Math.max(0, s.ability2Cooldown - cooldown);
		}
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - cooldown);
		s.ability3Cooldown = Math.max(0, s.ability3Cooldown - cooldown);

		// Crossfade: the song changes once the swap's 0.2 s are up, and his Crossfade animation for the new song plays
		// (graph 0266: the song switch plays it entering each song)
		if (s.songSwap >= 0 && --s.songSwap < 0) {
			s.speedSong = !s.speedSong;
			s.ability1Cooldown = Ow.ticks(CROSSFADE_COOLDOWN);
			Combat.animate(player, s.speedSong ? "crossfade_speed" : "crossfade_heal");
			// his own song cuts over with a sting; others hear a scratch before the new song comes in (LucioMusic)
			HeroSounds.toOwner(player, s.speedSong ? OcSounds.LUCIO_CROSSFADE_SPEED : OcSounds.LUCIO_CROSSFADE_HEAL);
			player.level().playSound(player, player.getX(), player.getY(), player.getZ(), OcSounds.LUCIO_SONG_SCRATCH_3P,
				net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
			if (player.level().getGameTime() >= s.songLinesFrom) {
				Voice.say(player, s.speedSong ? "crossfade_speed" : "crossfade_heal", 1);
			}
		}

		// an empty clip reloads by itself, once whatever cut its reload short (a melee, Amp It Up, the barrier) is done
		if (s.reloadHeld > 0) {
			s.reloadHeld--;
		}
		if (controls && s.primaryAmmo == 0 && s.primaryReload < 0 && s.volleyLeft == 0 && s.reloadHeld == 0 && s.barrierCast < 0) {
			startReload(player, s);
		}
		if (s.primaryReload >= 0) {
			s.primaryReload += attack;
			if (!s.primaryAmmoRestored && s.primaryReload >= Ow.ticks(AMP_RELOAD_AMMO_AT)) {
				s.primaryAmmo = AMP_AMMO;
				s.primaryAmmoRestored = true;
			}
			if (s.primaryReload >= Ow.ticks(AMP_RELOAD)) {
				s.primaryReload = -1;
			}
		}

		// the Sonic Amplifier: a held button fires volleys of 4, 0.125 s apart, a volley per 0.935 s
		s.weaponRecovery -= attack;
		if (s.volleyLeft == 0 && controls && s.primaryHeld && s.weaponRecovery <= 0 && s.primaryAmmo > 0
			&& s.barrierCast < 0 && (s.primaryReload < 0 || s.primaryAmmoRestored)) {
			s.primaryReload = -1;
			s.volleyLeft = Math.min(AMP_PER_VOLLEY, s.primaryAmmo);
			s.volleyNext = 0;
			s.weaponRecovery = Ow.ticks(AMP_VOLLEY_TIME);
			Combat.animate(player, "fire");
			// one sound per volley (effect 3495, played by the weapon graph's volley node)
			HeroSounds.split(player, OcSounds.LUCIO_AMP_FIRE, OcSounds.LUCIO_AMP_FIRE_3P, player.getEyePosition());
		}
		if (s.volleyLeft > 0) {
			s.volleyNext -= attack;
			while (s.volleyLeft > 0 && s.volleyNext <= 0 && s.primaryAmmo > 0) {
				fire(player);
				s.primaryAmmo--;
				s.volleyLeft--;
				s.volleyNext += Ow.ticks(AMP_SHOT_GAP);
			}
			if (s.primaryAmmo == 0) {
				s.volleyLeft = 0;
				startReload(player, s);
			}
		}
		if (s.volleyLeft == 0 && s.weaponRecovery < 0) {
			s.weaponRecovery = 0;
		}

		// Soundwave, while the right button is held and it's ready (holding it fires it the moment it's back: ours)
		if (controls && s.secondaryHeld && s.ability3Cooldown <= 0 && s.barrierCast < 0) {
			soundwave(player, s);
		}

		// Soundwave Rider (perk): riding a wall charges the next Soundwave over about 5 s; off the wall a part charge
		// drains, a full one holds until used (the wiki)
		if (HeroManager.hasPerk(player, Perk.LUCIO_SOUNDWAVE_RIDER)) {
			double step = 1.0 / Ow.ticks(RIDER_CHARGE_TIME);
			if (s.wallRiding) {
				s.riderCharge = Math.min(1, s.riderCharge + step);
			} else if (s.riderCharge < 1) {
				s.riderCharge = Math.max(0, s.riderCharge - step);
			}
		}

		if (player.tickCount % SONG_TICKS == 0) {
			songs(player, s);
		}
		syncSong(player, s, controls);

		// Wall Ride's sound while he rides, and his line once he's been on the wall 2.2 s
		if (s.wallRiding) {
			if (s.wallRideTicks == 0) {
				HeroSounds.split(player, OcSounds.LUCIO_WALLRIDE_START, OcSounds.LUCIO_WALLRIDE_START, player.position());
			}
			if (s.wallRideTicks % WALLRIDE_LOOP_TICKS == 0) {
				HeroSounds.toOwner(player, OcSounds.LUCIO_WALLRIDE);
			}
			if (s.wallRideTicks % WALLRIDE_LOOP_3P_TICKS == 0) {
				player.level().playSound(player, player.getX(), player.getY(), player.getZ(), OcSounds.LUCIO_WALLRIDE_3P,
					net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
			}
			if (++s.wallRideTicks == Ow.ticks(WALLRIDE_LINE_AFTER)) {
				Voice.say(player, "wallride", 1);
			}
		} else if (s.wallRideTicks > 0) {
			s.wallRideTicks = 0;
			HeroSounds.split(player, OcSounds.LUCIO_WALLRIDE_END, OcSounds.LUCIO_WALLRIDE_END, player.position());
		}

		// Sound Barrier: once the leap lands (and at least the cast's 0.656 s), the overhealth goes out
		if (s.barrierCast >= 0) {
			s.barrierCast++;
			if (s.barrierCast >= Ow.ticks(BARRIER_CAST) && player.onGround() || s.barrierCast > Ow.ticks(3)) {
				s.barrierCast = -1;
				barrier(player, s);
				// the slam (effects 44EE, 64ED), heard by all
				HeroSounds.split(player, OcSounds.LUCIO_BARRIER_SLAM, OcSounds.LUCIO_BARRIER_SLAM, player.position());
			}
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && s.songSwap < 0 && s.ability1Cooldown <= 0) {
			s.songSwap = Ow.ticks(SONG_SWAP);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && s.ampUp == 0 && s.ampDelay == 0 && s.ability2Cooldown <= 0) {
			s.ampDelay = Ow.ticks(AMP_UP_DELAY);
			s.ampFree = false;
			Combat.animate(player, "amp_it_up");
			// it takes the weapon from a reload (AMP_UP_LOCK): the ammo stays as it was unless it was already back
			s.interruptReload(AMP_UP_LOCK);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && s.barrierCast < 0 && s.ultActive == 0 && s.ultCharge >= ULT_COST) {
			s.ultCharge = Combat.afterUlt(s.hero, s.ultCharge);
			s.barrierCast = 0;
			s.ultActive = Ow.ticks(BARRIER_CAST + BARRIER_TIME);
			s.volleyLeft = 0;
			// the cast takes the weapon state too (graph 073A, priorities 0 and 10 over the reload's -10)
			s.interruptReload(BARRIER_CAST);
			Combat.animate(player, "sound_barrier");
			// the cast (effect A46) and its line: "Oh, let's break it down!" for him and allies, the Portuguese one for
			// enemies (stimuli 025B / 0489: 60 m, volume 4 = 64 blocks)
			HeroSounds.split(player, OcSounds.LUCIO_BARRIER_CAST, OcSounds.LUCIO_BARRIER_CAST_3P, player.position());
			// its look (11BF): a ring out across the ground and bands rising round him
			Effects.play((ServerLevel) player.level(), "sound_barrier_cast", player.position(), player);
			HeroSounds.ultLine(player, OcSounds.LUCIO_ULT_FRIENDLY, OcSounds.LUCIO_ULT_ENEMY, 4);
			// the small leap (ours: the wiki gives none): the client moves him, so it's sent as his motion
			player.setDeltaMovement(player.getDeltaMovement().x, Ow.speed(BARRIER_LEAP), player.getDeltaMovement().z);
			player.syncVelocity = true;
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && s.primaryAmmo < AMP_AMMO && s.primaryReload < 0 && s.volleyLeft == 0) {
			startReload(player, s);
		}
	}

	private static void startReload(ServerPlayer player, HeroState s) {
		s.primaryReload = 0;
		s.primaryAmmoRestored = false;
		Combat.animate(player, "reload");
		HeroSounds.split(player, OcSounds.LUCIO_AMP_RELOAD, OcSounds.LUCIO_AMP_RELOAD_3P, player.position());
	}

	private static void fire(ServerPlayer player) {
		ServerLevel level = player.level();
		SonicEntity round = new SonicEntity(player, level);
		round.launch(player.getEyePosition().add(0, -0.1, 0), player.getViewVector(1.0f), Ow.speed(AMP_SPEED));
		level.addFreshEntity(round);
	}

	/**
	 * Soundwave: 45 damage and a 17 m/s push along his aim (the wiki's 13.73 out and 10.15 up looking level) to every
	 * enemy in a 7 m arc in front, which then can't move for 0.45 s. Soundwave Rider's full charge: +50% damage, +25%
	 * push.
	 */
	private static void soundwave(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		boolean rider = s.riderCharge >= 1 && HeroManager.hasPerk(player, Perk.LUCIO_SOUNDWAVE_RIDER);
		double damage = WAVE_DAMAGE * (rider ? 1 + RIDER_DAMAGE : 1);
		double push = rider ? 1 + RIDER_KNOCKBACK : 1;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		double cosLimit = Math.cos(Math.toRadians(WAVE_HALF_ANGLE));
		float dealt = 0;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(WAVE_RANGE))) {
			if (target == player || !Allies.canHurt(player, target)) {
				continue;
			}
			Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
			if (to.length() > WAVE_RANGE + target.getBbWidth() / 2 || to.normalize().dot(look) < cosLimit || !player.hasLineOfSight(target)) {
				continue;
			}
			dealt += Combat.damage(target, player.damageSources().playerAttack(player), damage);
			if (target.isAlive()) {
				Vec3 velocity = look.scale(13.73).add(0, 10.15, 0);
				velocity = velocity.normalize().scale(WAVE_KNOCKBACK * push * (1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
				target.setDeltaMovement(Ow.speed(velocity.x), Ow.speed(velocity.y), Ow.speed(velocity.z));
				target.syncVelocity = true;
				// the lockout (ours: Minecraft has no "can't move", so a moment of heavy Slowness)
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Ow.ticks(WAVE_LOCKOUT), 9, false, false));
			}
		}
		Combat.credit(player, dealt);
		s.ability3Cooldown = Ow.ticks(WAVE_COOLDOWN);
		s.weaponRecovery = Math.max(s.weaponRecovery, Ow.ticks(WAVE_RECOVERY));
		s.volleyLeft = 0;
		if (rider) {
			s.riderCharge = 0;
		}
		Effects.play(level, "soundwave", eye, look, player, rider ? 1 : 0, player);
		Combat.animate(player, "soundwave");
		HeroSounds.split(player, OcSounds.LUCIO_SOUNDWAVE, OcSounds.LUCIO_SOUNDWAVE_3P, eye);
		Voice.say(player, "soundwave", 1);
	}

	/**
	 * The song, to himself and allies in 12 m he can see, every 0.2 s: Healing Boost 20 health a second (himself 12),
	 * Speed Boost +25%; Amp It Up makes them 56 a second (himself too) and +60%. The speed lingers 1 s after leaving
	 * (the buff's length); the healing doesn't (ours, simpler). Noise Violation: 2.5x the range while amped.
	 */
	private static void songs(ServerPlayer player, HeroState s) {
		boolean amped = s.ampUp > 0;
		double radius = SONG_RADIUS * (amped && HeroManager.hasPerk(player, Perk.LUCIO_NOISE_VIOLATION) ? 1 + NOISE_VIOLATION_RANGE : 1);
		double seconds = SONG_TICKS / 20.0;
		float healed = 0;
		for (LivingEntity ally : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius))) {
			if (!Allies.isSelfOrAlly(player, ally) || !ally.isAlive() || ally.distanceTo(player) > radius
				|| ally != player && !player.hasLineOfSight(ally)) {
				continue;
			}
			if (s.speedSong) {
				Combat.buff(ally, Attributes.MOVEMENT_SPEED, SPEED_SONG_BUFF, amped ? AMP_UP_SPEED : SPEED_SONG, SONG_LINGER + seconds);
			} else {
				double perSecond = amped ? AMP_UP_HEAL : ally == player ? HEAL_SONG_SELF : HEAL_SONG;
				healed += Combat.heal(ally, perSecond * seconds);
			}
		}
		Combat.credit(player, healed);
	}

	/** Sound Barrier lands: 625 overhealth to himself and allies within 30 m, gone over 6 s. Beat Drop: Amp It Up 4 s. */
	private static void barrier(ServerPlayer player, HeroState s) {
		for (LivingEntity ally : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(BARRIER_RADIUS))) {
			if (Allies.isSelfOrAlly(player, ally) && ally.isAlive() && ally.distanceTo(player) <= BARRIER_RADIUS) {
				Combat.overhealth(ally, BARRIER_OVERHEALTH, BARRIER_TIME);
			}
		}
		Effects.play(player.level(), "sound_barrier", player.position(), player);
		if (HeroManager.hasPerk(player, Perk.LUCIO_BEAT_DROP)) {
			s.ampUp = Math.max(s.ampUp, Ow.ticks(BEAT_DROP_TIME));
			s.ampFree = true;
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		float reload = s.primaryReload >= 0 && !s.primaryAmmoRestored
			? (float) Math.min(1, s.primaryReload / Ow.ticks(AMP_RELOAD_AMMO_AT)) : -1;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, ULT_COST, s.ultActive > 0,
			s.primaryAmmo, reload, 0, -1, s.songSwap >= 0 ? (float) (s.songSwap / 20.0) : 0, (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire,
			Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk),
			(float) (s.ability3Cooldown / 20.0), s.speedSong ? 1 : 0, s.ampUp / 20f, (float) s.riderCharge);
	}
}
