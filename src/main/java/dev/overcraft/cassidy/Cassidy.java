package dev.overcraft.cassidy;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.FlashbangEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Voice;
import dev.overcraft.net.DeadeyePayload;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.overcraft.cassidy.CassidyStats.*;

/**
 * Cassidy's kit (#72; numbers in {@link CassidyStats}, the data map tools/reference/cassidy-map.md). His buttons by his data's
 * ids: the left button Peacekeeper (PRIMARY), the right Fan the Hammer (SECONDARY; with Silver Bullet the aim for it), Shift Combat
 * Roll (ABILITY 1), E Flashbang (ABILITY 2; the live kit's grenade since BZ 2024-06-20, loadout 0060), Q Deadeye. The server owns
 * his rounds, cooldowns, shots and Deadeye's locks; his own client moves him through the roll (ClientCassidy) the moment he
 * presses, as Lifeweaver's dash and Brigitte's bash are, and the server times its damage cut and reload from the same press.
 */
public final class Cassidy implements HeroKit {
	/** The synced mode's bits: Deadeye's cast or channel, rolling, hindered, hacked, Silver Bullet aimed, Deadeye firing, throwing. */
	public static final int MODE_DEADEYE = 1, MODE_ROLL = 2, MODE_HINDERED = 4, MODE_SILENCED = 8, MODE_SILVER = 16,
		MODE_FIRING = 32, MODE_CASTING = 64;
	/** The synced mode's bits from here: the packets seen (CassidyState.pressSeen), and from CUT_SHIFT the rolls refused or cut. */
	public static final int SEEN_SHIFT = 8, CUT_SHIFT = 16;

	static final Blow FAN_BLOW = Blow.weapon("fan_the_hammer", false).by(Hero.CASSIDY);
	static final Blow FLASH_BLOW = Blow.ability("flashbang").by(Hero.CASSIDY);
	static final Blow DEADEYE_BLOW = Blow.ult("deadeye").by(Hero.CASSIDY);
	static final Blow BLEED_BLOW = Blow.perk("silver_bullet").by(Hero.CASSIDY);

	private static final Identifier DEADEYE_SPEED = Overcraft.id("deadeye_slow");
	private static final Identifier GIDDY_SPEED_ID = Overcraft.id("giddy_up");

	/** Silver Bullet's bleeds under way: what, by whom, its steps left. */
	private record Bleed(LivingEntity target, UUID owner, int[] left) {
	}

	/**
	 * By target and owner: two Cassidys' bleeds on one target each run (ours: the data's status is per caster, M); a second from the
	 * same Cassidy (a roll resets Silver Bullet) starts his again.
	 */
	private static final Map<String, Bleed> BLEEDS = new ConcurrentHashMap<>();

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.cassidy.clear();
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		s.weaponRecovery = 0;
		speed(player, DEADEYE_SPEED, 0);
		speed(player, GIDDY_SPEED_ID, 0);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		CassidyState c = s.cassidy;
		long now = player.level().getGameTime();
		c.silenced = dev.overcraft.sombra.Hack.silenced(player);
		c.hindered = Hinder.hindered(player);
		double rate = Combat.cooldownRate(player);
		if (c.roll < 0) {
			s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		}
		if (c.flashCast < 0) {
			s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		}
		c.silverCooldown = Math.max(0, c.silverCooldown - rate);
		// the wait an action's cut put on the empty cylinder's own reload (HeroState.interruptReload): counted down here, as the
		// other kits do (found playing, T9: a Flashbang mid-reload left him at 0 rounds for the rest of the fight)
		if (s.reloadHeld > 0) {
			s.reloadHeld--;
		}
		boolean able = controls && player.isAlive() && !Sleep.asleep(player);
		if (c.roll >= 0 && (c.hindered || !able)) {
			// a hinder interrupts a roll under way (W rev 409375: "interrupting and preventing movement abilities"; the data's 0228
			// blocks the ability, M), as a sleep or a stun does: it stops where it is, no reload if it hadn't come, its cooldown from
			// now (ours); his client is told by the cut count and stops too
			cutRoll(player, s, c);
		}
		roll(player, s, c);
		giddy(player, s, c);
		flashCast(player, s, c, able);
		if (c.deadeye >= 0) {
			deadeye(player, s, c, able, now);
		} else {
			weapon(player, s, c, able, now);
		}
		if (c.lineAt >= 0 && now >= c.lineAt) {
			c.lineAt = -1;
			// "It's high noon" (022F #621: his own key F1AC 183B, allies' F1AB 0487 "Step right up", enemies' F1AA 01D5): he hears
			// only his own (found playing, T12c: he heard both his and the allies' line)
			ultLine(player);
		}
	}

	/** Deadeye's cast line: his own copy in his head, the allies' and the enemies' from him (HeroSounds.ultLine's way). */
	private static void ultLine(ServerPlayer hero) {
		net.minecraft.sounds.SoundEvent own = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("cassidy.voice.ultimate"));
		net.minecraft.sounds.SoundEvent friendly = CassidySounds.of("ult.friendly"), enemy = CassidySounds.of("ult.enemy");
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

	// ---- Peacekeeper and Fan the Hammer (01D2) ----

	private static void weapon(ServerPlayer player, HeroState s, CassidyState c, boolean able, long now) {
		s.weaponRecovery = Math.max(0, s.weaponRecovery - Combat.attackRate(player));
		if (s.primaryReload >= 0) {
			s.primaryReload += Combat.attackRate(player);
			if (s.primaryReload >= Ow.ticks(RELOAD_AMMO_AT) && s.primaryAmmo < AMMO) {
				s.primaryAmmo = AMMO;
			}
			if (s.primaryReload >= Ow.ticks(RELOAD)) {
				s.primaryReload = -1;
			}
		}
		boolean busy = c.roll >= 0 || c.flashCast >= 0 || !able;
		if (c.fanLeft > 0) {
			fanTick(player, s, c, now, busy);
			return;
		}
		boolean silverPerk = s.has(Perk.CASSIDY_SILVER_BULLET);
		c.silverAim = silverPerk && able && s.secondaryHeld && c.silverCooldown <= 0 && c.roll < 0;
		if (busy) {
			return;
		}
		if (s.primaryReload < 0 && s.primaryAmmo <= 0 && s.weaponRecovery <= 0 && s.reloadHeld <= 0) {
			// empty: the reload starts by itself (D 01D2)
			startReload(player, s);
			return;
		}
		if (s.primaryReload >= 0 || s.primaryAmmo <= 0 || s.weaponRecovery > 0) {
			return;
		}
		if (!silverPerk && s.secondaryHeld) {
			// pressed, or held through the shot's recovery or a reload: it fans once it can (ours: the data buffers a press 0.1 s)
			startFan(player, s, c);
			return;
		}
		if (s.primaryHeld && now >= c.primaryLockUntil) {
			boolean silver = c.silverAim;
			s.primaryAmmo--;
			s.weaponRecovery = Ow.ticks(PEACEKEEPER_INTERVAL);
			CassidySounds.split(player, "peacekeeper.fire", player.getEyePosition(), 200);
			CassidySounds.at(player.level(), "peacekeeper.bullet_3p", player.getEyePosition(), 60, player);
			Combat.animate(player, "fire");
			if (silver) {
				// Silver Bullet: the shot that pierces, then bleeds (01D2 / 5FDA); 8 s from it
				c.silverCooldown = Ow.ticks(SILVER_COOLDOWN);
				c.silverAim = false;
				shot(player, s, player.getViewVector(1.0f), SILVER_DAMAGE, FALLOFF_START, FALLOFF_END, true, true,
					Blow.perk("silver_bullet").by(Hero.CASSIDY), RANGE);
			} else {
				shot(player, s, player.getViewVector(1.0f), PEACEKEEPER_DAMAGE, FALLOFF_START, FALLOFF_END, true, false,
					Blow.weapon(false).by(Hero.CASSIDY), RANGE);
			}
		}
	}

	private static void startFan(ServerPlayer player, HeroState s, CassidyState c) {
		// the cylinder run out from the press, 0.12 s apart, the first exact (01D2 loadout 005E)
		c.fanLeft = s.primaryAmmo;
		c.fanShot = 0;
		c.fanNext = 0;
		Combat.animate(player, "fan");
		fanTick(player, s, c, player.level().getGameTime(), false);
	}

	private static void fanTick(ServerPlayer player, HeroState s, CassidyState c, long now, boolean busy) {
		if (busy || s.primaryAmmo <= 0) {
			// a roll, a throw or a stun ends it (ours: its rounds stay)
			endFan(c, now);
			return;
		}
		c.fanNext -= Combat.attackRate(player);
		while (c.fanNext <= 0 && c.fanLeft > 0 && s.primaryAmmo > 0) {
			Vec3 dir = c.fanShot == 0 ? player.getViewVector(1.0f)
				: dev.overcraft.sombra.Sombra.cone(player.getViewVector(1.0f), FAN_SPREAD, player.getRandom());
			c.fanShot++;
			c.fanLeft--;
			s.primaryAmmo--;
			c.fanNext += 20 * FAN_INTERVAL;
			CassidySounds.split(player, "fan.fire", player.getEyePosition(), 200);
			CassidySounds.at(player.level(), "peacekeeper.bullet_3p", player.getEyePosition(), 60, player);
			if (c.fanShot > 1) {
				Combat.animate(player, "fire");
			}
			// no crit (D: override 1, M)
			shot(player, s, dir, FAN_DAMAGE, FAN_FALLOFF_START, FAN_FALLOFF_END, false, false, FAN_BLOW, FAN_RANGE);
		}
		if (c.fanLeft <= 0 || s.primaryAmmo <= 0) {
			endFan(c, now);
		}
	}

	private static void endFan(CassidyState c, long now) {
		if (c.fanShot > 0) {
			// 0.3 s before Peacekeeper (D #347 v173 = 0.3, M)
			c.primaryLockUntil = now + Ow.ticks(FAN_PRIMARY_LOCK);
		}
		c.fanLeft = 0;
		c.fanShot = 0;
	}

	private static void startReload(ServerPlayer player, HeroState s) {
		CassidyState c = s.cassidy;
		if (s.primaryReload >= 0 || s.primaryAmmo >= AMMO || c.roll >= 0 || c.deadeye >= 0 || c.fanLeft > 0) {
			return;
		}
		s.primaryReload = 0;
		Combat.animate(player, "reload");
		CassidySounds.split(player, "reload", player.getEyePosition(), 45);
	}

	/** A falloff share at {@code metres}: full to {@code start}, x0.3 from {@code end}, linear between (D; M the line). */
	public static double falloff(double metres, double start, double end) {
		if (metres <= start) {
			return 1;
		}
		if (metres >= end) {
			return FALLOFF_MIN;
		}
		return 1 - (1 - FALLOFF_MIN) * (metres - start) / (end - start);
	}

	/** Peacekeeper's damage at {@code metres}, x2 on the head. */
	public static double peacekeeperDamage(double metres, boolean crit) {
		return PEACEKEEPER_DAMAGE * falloff(metres, FALLOFF_START, FALLOFF_END) * (crit ? CRIT : 1);
	}

	/** Fan the Hammer's damage at {@code metres} (no crit). */
	public static double fanDamage(double metres) {
		return FAN_DAMAGE * falloff(metres, FAN_FALLOFF_START, FAN_FALLOFF_END);
	}

	/**
	 * One hitscan shot from his eyes along {@code dir}: the first enemy within 0.04 m before a block ({@code pierce}: every enemy
	 * before it, Silver Bullet's), its falloff, x2 on the head when {@code crits}.
	 */
	private static void shot(ServerPlayer player, HeroState s, Vec3 dir, double damage, double start, double end, boolean crits,
							 boolean pierce, Blow blow, double range) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(dir.scale(range));
		BlockHitResult blockHit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 block = blockHit.getLocation();
		List<Object[]> hits = new ArrayList<>();
		for (Entity e : level.getEntities(player, new AABB(eye, block).inflate(1))) {
			LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
			if (target == null || !target.isAlive() || !Allies.canHurt(player, target)) {
				continue;
			}
			java.util.Optional<Vec3> hit = e.getBoundingBox().inflate(RADIUS).clip(eye, block);
			if (hit.isPresent()) {
				// the part it crossed, when it's one of the dragon's (its head takes the whole hit: Combat.damagePart)
				hits.add(new Object[] {target, hit.get(), hit.get().distanceTo(eye), e instanceof EnderDragonPart part ? part : null});
			}
		}
		hits.sort(java.util.Comparator.comparingDouble(h -> (double) h[2]));
		Vec3 stop = block;
		java.util.Set<LivingEntity> done = new java.util.HashSet<>();
		for (Object[] h : hits) {
			LivingEntity target = (LivingEntity) h[0];
			if (!done.add(target)) {
				continue;
			}
			Vec3 at = (Vec3) h[1];
			double metres = (double) h[2];
			boolean crit = crits && dev.overcraft.hero.Headshot.crossed(target, at, dir, RADIUS);
			double dealtOw = damage * falloff(metres, start, end) * (crit ? CRIT : 1);
			// a dragon part through Combat.damagePart, as every other hero's shot (critic b79a4d1 #5: through the body it was quartered)
			float dealt = h[3] instanceof EnderDragonPart part ? Combat.damagePart(level, part, Combat.hitscan(player), dealtOw, blow.crit(crit))
				: Combat.damage(target, Combat.hitscan(player), dealtOw, blow.crit(crit));
			Combat.credit(player, dealt, target);
			Combat.addFireForDamage(player, dealt, target);
			CassidySounds.split(player, "peacekeeper.hit_enemy", at, 500);
			if (dealt > 0) {
				Effects.hitMarker(player, target, crit);
				if (crit) {
					s.critAt = level.getGameTime();
					// (Combat.damage answers in Overwatch points)
					sharpshooter(s, dealt);
				}
				if (pierce && target.isAlive()) {
					bleed(player, target);
				}
			}
			stop = at;
			if (!pierce) {
				break;
			}
		}
		if (hits.isEmpty() && blockHit.getType() == HitResult.Type.BLOCK) {
			CassidySounds.at(level, "peacekeeper.hit_block", block, 32);
		}
		trail(level, fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD), pierce ? block : stop);
	}

	/** Sharpshooter (01C7): crit damage dealt takes 0.0075 s a point off Combat Roll's cooldown. */
	public static double sharpshooterCut(double critDamage) {
		return Math.max(0, critDamage) * SHARPSHOOTER * 20;
	}

	private static void sharpshooter(HeroState s, double critDamage) {
		if (s.ability1Cooldown > 0) {
			s.ability1Cooldown = Math.max(0, s.ability1Cooldown - sharpshooterCut(critDamage));
		}
	}

	private static void trail(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		double len = d.length();
		int n = (int) Math.min(16, Math.max(1, len / 1.5));
		for (int k = 1; k <= n; k++) {
			Vec3 p = from.add(d.scale(k / (double) n * Math.min(1, 24 / Math.max(len, 1e-3))));
			level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		level.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 2, 0, 0, 0, 0);
	}

	/** A point from his eyes in his look's frame (right, up, ahead), or his eyes when a wall is between. */
	static Vec3 fromEyes(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 top = side.cross(look).normalize();
		Vec3 at = eye.add(side.scale(right)).add(top.scale(up)).add(look.scale(ahead));
		if (player.level().clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			return eye;
		}
		return at;
	}

	// ---- Silver Bullet's bleed (5FDA) ----

	private static void bleed(ServerPlayer owner, LivingEntity target) {
		int steps = bleedSteps();
		BLEEDS.put(target.getUUID() + "/" + owner.getUUID(), new Bleed(target, owner.getUUID(), new int[] {steps, Ow.ticks(SILVER_BLEED_STEP)}));
	}

	/** The bleed's steps: 1.5 s in steps of 0.2 s, rounded (8). */
	public static int bleedSteps() {
		return (int) Math.round(SILVER_BLEED_TIME / SILVER_BLEED_STEP);
	}

	/** Every server tick (Overcraft's): the bleeds' steps, 50 over 1.5 s, no crit. */
	public static void tickBleeds(net.minecraft.server.MinecraftServer server) {
		BLEEDS.values().removeIf(b -> {
			if (!b.target.isAlive() || b.target.isRemoved()) {
				return true;
			}
			if (--b.left[1] > 0) {
				return false;
			}
			b.left[1] = Ow.ticks(SILVER_BLEED_STEP);
			ServerPlayer owner = server.getPlayerList().getPlayer(b.owner);
			// 50 in all, shared by its steps (found playing: 8 steps of a 7.5-step rate gave 53)
			double step = SILVER_BLEED / bleedSteps();
			if (owner != null) {
				// already in them: a barrier they then stand behind doesn't stop it (Barriers.pierce)
				float dealt = dev.overcraft.hero.Barriers.pierce(() -> Combat.damage(b.target, Combat.hitscan(owner), step, BLEED_BLOW));
				Combat.credit(owner, dealt, b.target);
			}
			return --b.left[0] <= 0;
		});
	}

	// ---- Combat Roll (01F0) ----

	/** Whether a roll may start now (the server's side of it: his client asks the same of the last sync, ClientCassidy). */
	public static boolean mayRoll(boolean rolling, double cooldown, boolean deadeye, boolean casting, boolean silenced, boolean hindered,
								  boolean asleep, boolean riding) {
		return !rolling && cooldown <= 0 && !deadeye && !casting && !silenced && !hindered && !asleep && !riding;
	}

	/**
	 * A roll his client made (it sends the roll's direction only for one it made: ClientCassidy). One authority: his client rolls only
	 * when the last sync says the server will take it and every packet it sent that could change that has been seen; a press it
	 * refuses isn't sent as a roll, so the server never rolls for a press that didn't move him. What the client couldn't know (a
	 * hinder, a hack or a sleep landing in the round trip) the server refuses here, counts in {@code rollCut}, and his client stops.
	 */
	private static void clientRoll(ServerPlayer player, HeroState s, CassidyState c, int direction) {
		if (!HeroManager.controls(player) || !mayRoll(c.roll >= 0, s.ability1Cooldown, c.deadeye >= 0, c.flashCast >= 0,
			dev.overcraft.sombra.Hack.silenced(player), Hinder.hindered(player), Sleep.asleep(player), player.isPassenger())) {
			c.rollCut = (c.rollCut + 1) & 0xFF;
			return;
		}
		startRoll(player, s, c, direction);
	}

	/** The roll stops where it is (a hinder, a stun): his client stops with it (the cut count). */
	private static void cutRoll(ServerPlayer player, HeroState s, CassidyState c) {
		c.roll = -1;
		c.rollCut = (c.rollCut + 1) & 0xFF;
		s.ability1Cooldown = Ow.ticks(ROLL_COOLDOWN);
	}

	private static void startRoll(ServerPlayer player, HeroState s, CassidyState c, int direction) {
		c.roll = 0;
		c.giddy = -1;
		speed(player, GIDDY_SPEED_ID, 0);
		endFan(c, player.level().getGameTime());
		s.interruptReload(0);
		// Silver Bullet's reset (D 01D2: Combat Roll)
		c.silverCooldown = 0;
		Combat.animate(player, "roll_s" + Math.clamp(direction, 0, 3));
		CassidySounds.split(player, "roll", player.position(), 40);
		Voice.say(player, "roll", 1);
		player.resetFallDistance();
	}

	private static void roll(ServerPlayer player, HeroState s, CassidyState c) {
		if (c.roll < 0) {
			return;
		}
		c.roll++;
		player.resetFallDistance();
		if (c.roll == Ow.ticks(ROLL_RELOAD_AT)) {
			// the cylinder full (D 01F0 message 007D); Rollin' Round-Up: 15 for each round it put back (51E5)
			int back = Math.max(0, AMMO - s.primaryAmmo);
			s.primaryAmmo = AMMO;
			s.primaryReload = -1;
			if (back > 0 && s.has(Perk.CASSIDY_ROLLIN_ROUND_UP)) {
				Combat.heal(player, ROUND_UP_HEAL * back);
			}
		}
		if (c.roll >= Ow.ticks(ROLL_TIME)) {
			c.roll = -1;
			// 5 s from its end (D 01F0)
			s.ability1Cooldown = Ow.ticks(ROLL_COOLDOWN);
			if (s.has(Perk.CASSIDY_GIDDY_UP)) {
				c.giddy = 0;
				speed(player, GIDDY_SPEED_ID, GIDDY_SPEED);
			}
		}
	}

	/** Giddy Up's speed now: +60 % at the roll's end, down to none over 1.5 s in 10 steps (D 51E5). */
	public static double giddyBonus(int ticksSinceRoll) {
		double step = GIDDY_TIME / GIDDY_STEPS;
		int steps = (int) Math.floor(ticksSinceRoll / 20.0 / step + 1.0E-9);
		return steps >= GIDDY_STEPS ? 0 : GIDDY_SPEED * (1 - steps / (double) GIDDY_STEPS);
	}

	private static void giddy(ServerPlayer player, HeroState s, CassidyState c) {
		if (c.giddy < 0) {
			return;
		}
		c.giddy++;
		double bonus = giddyBonus(c.giddy);
		speed(player, GIDDY_SPEED_ID, bonus);
		if (bonus <= 0) {
			c.giddy = -1;
		}
	}

	// ---- Flashbang (01E9) ----

	private static void startFlash(ServerPlayer player, HeroState s, CassidyState c) {
		if (c.flashCast >= 0 || s.ability2Cooldown > 0 || c.roll >= 0 || c.deadeye >= 0 || c.silenced || Sleep.asleep(player)) {
			return;
		}
		c.flashCast = 0;
		endFan(c, player.level().getGameTime());
		s.interruptReload(FLASH_CAST + FLASH_RECOVERY);
		Combat.animate(player, "flashbang");
		CassidySounds.split(player, "flashbang.throw", player.getEyePosition(), 50);
	}

	private static void flashCast(ServerPlayer player, HeroState s, CassidyState c, boolean able) {
		if (c.flashCast < 0) {
			return;
		}
		c.flashCast++;
		if (c.flashCast == Ow.ticks(FLASH_CAST)) {
			if (!able || c.silenced) {
				// a stun, a sleep or a hack in the 0.1 s: no throw, no cooldown (ours)
				c.flashCast = -1;
				return;
			}
			Vec3 from = fromEyes(player, FLASH_RIGHT, FLASH_UP, FLASH_AHEAD);
			throwFlash(player, from, FLASH_SPEED, FLASH_RANGE, s.has(Perk.CASSIDY_BANG_BANG) ? BANG_DAMAGE : 1);
			if (s.has(Perk.CASSIDY_BANG_BANG)) {
				// Bang Bang: a second with the first, slower and farther (01E9 #203 / #206)
				throwFlash(player, from, BANG_SPEED, BANG_RANGE, BANG_DAMAGE);
			}
			// "Hold up, now" (01E9 #116 -> 01D4)
			Voice.say(player, "flashbang", 1);
		}
		if (c.flashCast >= Ow.ticks(FLASH_CAST + FLASH_RECOVERY)) {
			c.flashCast = -1;
			// 12 s from the cast's end (D 01E9)
			s.ability2Cooldown = Ow.ticks(FLASH_COOLDOWN);
		}
	}

	private static void throwFlash(ServerPlayer player, Vec3 from, double speed, double range, double share) {
		FlashbangEntity flash = new FlashbangEntity(player, player.level(), range, share);
		flash.launch(from, player.getViewVector(1.0f), Ow.speed(speed));
		player.level().addFreshEntity(flash);
	}

	/** Whether a Flashbang's burst takes one {@code metres} from it: within 3 m, in sight of it, no barrier between (01E9). */
	public static boolean flashTakes(double metres, boolean inSight, boolean barrierBetween) {
		return metres <= FLASH_BLAST && inSight && !barrierBetween;
	}

	/**
	 * Where along {@code from -> to} (0..1) it is {@code range} from {@code start}: 0 when already there, -1 when the leg stays inside.
	 * Flashbang's range along its move (FlashbangEntity.tick).
	 */
	public static double rangeCrossing(Vec3 start, Vec3 from, Vec3 to, double range) {
		Vec3 d = to.subtract(from);
		Vec3 f = from.subtract(start);
		double c = f.lengthSqr() - range * range;
		if (c >= 0) {
			return 0;
		}
		double a = d.lengthSqr();
		if (a < 1.0E-12) {
			return -1;
		}
		double b = 2 * f.dot(d);
		double t = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
		return t <= 1 ? t : -1;
	}

	/** A Flashbang goes off at {@code at}: 75 (x {@code share}) to every enemy within 3 m in sight, and hinders them (01E9). */
	public static void flashBang(ServerPlayer owner, Entity flash, Vec3 at, double share) {
		ServerLevel level = owner.level();
		for (dev.overcraft.hero.Barriers.Shape b : dev.overcraft.hero.Barriers.enemyBarriers(level, owner)) {
			Vec3 near = dev.overcraft.hero.Barriers.nearest(b, at);
			if (near.distanceTo(at) <= FLASH_BLAST) {
				b.barrierTake(FLASH_DAMAGE * share, near);
			}
		}
		// an enemy Brigitte's too, once (her barrier is her state, not a Barriers.Shape)
		dev.overcraft.brigitte.Barrier.burst(level, owner, at, FLASH_BLAST, d -> FLASH_DAMAGE * share);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(FLASH_BLAST + 2))) {
			if (!e.isAlive() || e == owner || !Allies.areaHits(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (!flashTakes(d, d <= FLASH_BLAST && Combat.inSight(level, at, e, flash),
				// any enemy barrier between, Brigitte's too (Barrier.shields asks Barriers.between first): behind hers they
				// were hindered, the damage alone stopped (#104's sweep)
				d <= FLASH_BLAST && dev.overcraft.brigitte.Barrier.shields(e, owner, at))) {
				continue;
			}
			// a hero's explosion (Combat.blast): not scaled by the difficulty, as Minecraft's own explosion was against players
			float dealt = Combat.damage(e, Combat.blast(level, flash, owner), FLASH_DAMAGE * share, FLASH_BLOW);
			if (dealt > 0) {
				Combat.credit(owner, dealt, e);
				Combat.addFireForDamage(owner, dealt, e);
				Effects.hitMarker(owner, e, false);
			}
			if (e.isAlive()) {
				Hinder.apply(e);
			}
		}
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 20, 0.6, 0.6, 0.6, 0.15);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 30, 1, 1, 1, 0.3);
		CassidySounds.split(owner, "flashbang.explode", at, 60);
	}

	// ---- Deadeye (022F / 01EE) ----

	private static void startDeadeye(ServerPlayer player, HeroState s, CassidyState c) {
		if (c.deadeye >= 0 || c.roll >= 0 || c.flashCast >= 0 || c.silenced || player.isPassenger() || Sleep.asleep(player)
			|| s.ultCharge < Combat.ultCost(s)) {
			return;
		}
		// the charge spent at the cast (D 022F), his cylinder full, Silver Bullet back
		s.ultCharge = Combat.afterUlt(Hero.CASSIDY, s.ultCharge);
		s.ultActive = Ow.ticks(DEADEYE_CAST + DEADEYE_TIME + 2);
		c.deadeye = 0;
		c.deadeyeFire = -1;
		c.deadeyeCancel = -1;
		c.locks.clear();
		endFan(c, player.level().getGameTime());
		s.interruptReload(0);
		s.primaryAmmo = AMMO;
		c.silverCooldown = 0;
		c.lineAt = player.level().getGameTime() + Ow.ticks(DEADEYE_LINE_AT);
		speed(player, DEADEYE_SPEED, -DEADEYE_SLOW);
		Combat.animate(player, "deadeye");
		CassidySounds.split(player, "deadeye.start", player.position(), 60);
		CassidySounds.split(player, "deadeye.loop", player.position(), 60);
	}

	/**
	 * Deadeye's self-cut now (0.4 -> 0 in 10 steps of 0.15 s after 2 s), {@code ticks} into the channel. The steps read as Giddy Up's
	 * (D #604 "10 steps every 0.15 s", the same loop as 51E5's): full at 2 s, the first step 0.15 s on, none from 3.5 s (W: 1.4 s).
	 * It had its first step at 2.0 s, Giddy Up its at 0.15 s (critic b79a4d1 #6).
	 */
	public static double deadeyeFade(int ticks) {
		double after = ticks / 20.0 - DEADEYE_FADE_AFTER;
		if (after < 0) {
			return 1;
		}
		int steps = Math.min(DEADEYE_FADE_STEPS, (int) Math.floor(after / DEADEYE_FADE_STEP + 1.0E-9));
		return 1 - steps / (double) DEADEYE_FADE_STEPS;
	}

	/** A target's charge after one more tick of lock: 150 a second for its own first 2 s, then 300 (D 01EE). */
	public static double lockStep(double lockTicks) {
		return (lockTicks < Ow.ticks(LOCK_RAMP) ? LOCK_SLOW : LOCK_FAST) / 20.0;
	}

	private static void deadeye(ServerPlayer player, HeroState s, CassidyState c, boolean able, long now) {
		c.deadeye++;
		player.resetFallDistance();
		if (!player.isAlive()) {
			endDeadeye(player, s, c);
			return;
		}
		int cast = Ow.ticks(DEADEYE_CAST);
		int channel = c.deadeye - cast;
		if (c.deadeyeCancel >= 0) {
			if (--c.deadeyeCancel <= 0) {
				endDeadeye(player, s, c);
			}
			return;
		}
		if (c.deadeyeFire >= 0) {
			fireTick(player, s, c);
			return;
		}
		if (channel < 0) {
			return;
		}
		double fade = deadeyeFade(channel);
		speed(player, DEADEYE_SPEED, -(DEADEYE_SLOW_AFTER + (DEADEYE_SLOW - DEADEYE_SLOW_AFTER) * fade));
		if (!able || c.silenced) {
			// stunned, asleep or hacked: it ends, no shot (W rev 409375: "stunned, knocked down or hacked")
			endDeadeye(player, s, c);
			return;
		}
		lock(player, c);
		sendLocks(player, c);
		if (s.secondaryFresh) {
			// the right button cancels it (D 022F 0.1 s)
			c.ultAgain = false;
			c.deadeyeCancel = Ow.ticks(DEADEYE_CANCEL);
			return;
		}
		boolean asked = s.primaryFresh || c.ultAgain;
		c.ultAgain = false;
		if (asked && canFire(c.inSight)) {
			// the left button or Q again (W: "press Ultimate again or Primary Fire"); with no one in sight in the cone the press does
			// nothing and Deadeye goes on (W: "cannot be fired unless there is at least one target with unobstructed line of sight
			// within the targeting area"; critic b79a4d1 #2: it fired at nothing and spent the ult)
			startFire(player, c);
			return;
		}
		if (channel >= Ow.ticks(DEADEYE_TIME)) {
			// out of time: no shot (D 022F)
			endDeadeye(player, s, c);
		}
	}

	/** One tick of locking: every enemy in the 105 degree cone within 200 m and in sight gains its charge; out of sight it waits. */
	private static void lock(ServerPlayer player, CassidyState c) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		c.inSight = 0;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(DEADEYE_RANGE))) {
			// an enemy only, never a bystander (Allies.areaHits: what every lock and area ult takes; a cow or a sheep isn't one)
			if (!e.isAlive() || !Allies.areaHits(player, e)) {
				continue;
			}
			Vec3 at = aim(e);
			Vec3 to = at.subtract(eye);
			double dist = to.length();
			if (!lockable(dist, dist < 1.0E-3 ? 1 : to.scale(1 / dist).dot(look), Combat.inSight(level, eye, e, player))) {
				continue;
			}
			c.inSight++;
			CassidyState.Lock lock = c.locks.computeIfAbsent(e.getId(), k -> new CassidyState.Lock());
			lock.charge += lockStep(lock.ticks);
			lock.ticks++;
		}
	}

	/** Whether one {@code metres} off, at {@code cosine} from his aim, gains its lock: in the 105 degree cone, within 200 m, in sight. */
	public static boolean lockable(double metres, double cosine, boolean inSight) {
		return metres <= DEADEYE_RANGE && metres >= 1.0E-3 && cosine >= Math.cos(Math.toRadians(DEADEYE_CONE / 2)) && inSight;
	}

	/** Whether Deadeye can fire: someone in the cone and in sight now (W rev 409375). */
	public static boolean canFire(int inSight) {
		return inSight > 0;
	}

	/** Where Deadeye aims on a target (hp 0001, read as its chest: ours). */
	private static Vec3 aim(LivingEntity e) {
		return e.position().add(0, e.getBbHeight() * 0.6, 0);
	}

	private static void sendLocks(ServerPlayer player, CassidyState c) {
		int n = c.locks.size();
		int[] ids = new int[n];
		float[] share = new float[n];
		int i = 0;
		for (Map.Entry<Integer, CassidyState.Lock> en : c.locks.entrySet()) {
			Entity e = player.level().getEntity(en.getKey());
			ids[i] = en.getKey();
			double health = e instanceof LivingEntity l ? Ow.toOw(l.getHealth() + l.getAbsorptionAmount()) : 1;
			// what the shot would take off it (its armor, resistance, the hero damage path: Combat.projected), not the charge itself
			// (critic b79a4d1 #3: the skull showed on a zombie in iron that the shot left at 0.9)
			double lands = e instanceof LivingEntity l ? Combat.projected(l, Combat.hitscan(player), en.getValue().charge) : 0;
			share[i] = (float) skullShare(lands, health);
			i++;
		}
		send(player, new DeadeyePayload(ids, share));
	}

	/** The skull's share: what the shot would deal over the health it has (1 or more: the skull, the shot kills). */
	public static double skullShare(double lands, double health) {
		return lands / Math.max(1, health);
	}

	private static void send(ServerPlayer player, DeadeyePayload payload) {
		if (ServerPlayNetworking.canSend(player, DeadeyePayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static void startFire(ServerPlayer player, CassidyState c) {
		// the most charged first, the nearest first among equals (ours: the data's list order is the tracker's; played, the six rounds
		// went to far-off slimes locked as long as the zombie in front of him)
		c.deadeyeOrder = c.locks.entrySet().stream().filter(en -> en.getValue().charge > 0)
			.sorted(java.util.Comparator.<Map.Entry<Integer, CassidyState.Lock>>comparingDouble(en -> -en.getValue().charge)
				.thenComparingDouble(en -> {
					Entity e = player.level().getEntity(en.getKey());
					return e == null ? Double.MAX_VALUE : e.distanceToSqr(player);
				}))
			.mapToInt(Map.Entry::getKey).toArray();
		c.deadeyeFire = 0;
		speed(player, DEADEYE_SPEED, -DEADEYE_SLOW);
	}

	/** When Deadeye's shot {@code k} goes, ticks from the press: 0.1 s, then one every 0.1 s, each 0.066 s after its turn. */
	public static int shotTick(int k) {
		return Ow.ticks(DEADEYE_FIRE_START + k * DEADEYE_SHOT_GAP + DEADEYE_SHOT_DELAY);
	}

	private static void fireTick(ServerPlayer player, HeroState s, CassidyState c) {
		c.deadeyeFire++;
		int n = c.deadeyeOrder.length;
		for (int k = 0; k < n; k++) {
			if (c.deadeyeFire == shotTick(k)) {
				deadeyeShot(player, s, c, c.deadeyeOrder[k]);
			}
		}
		int last = n == 0 ? Ow.ticks(DEADEYE_FIRE_START) : shotTick(n - 1);
		if (n == 0 && c.deadeyeFire == last) {
			CassidySounds.split(player, "deadeye.fire", player.getEyePosition(), 300);
		}
		if (c.deadeyeFire >= last + Ow.ticks(DEADEYE_END)) {
			endDeadeye(player, s, c);
		}
	}

	private static void deadeyeShot(ServerPlayer player, HeroState s, CassidyState c, int id) {
		CassidyState.Lock lock = c.locks.get(id);
		Entity e = player.level().getEntity(id);
		s.primaryAmmo = Math.max(0, s.primaryAmmo - 1);
		Combat.animate(player, "deadeye_fire");
		CassidySounds.split(player, "deadeye.fire", player.getEyePosition(), 300);
		Voice.say(player, "deadeye_fire", 1);
		if (!(e instanceof LivingEntity target) || lock == null || !target.isAlive()
			|| !Combat.inSight(player.level(), player.getEyePosition(), target, player)) {
			return;
		}
		Vec3 at = aim(target);
		// the dragon through its head, which takes the whole shot (Combat.damagePart; through the body it was quartered)
		float dealt = target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon
			? Combat.damagePart(player.level(), dragon.head, Combat.hitscan(player), lock.charge, DEADEYE_BLOW)
			: Combat.damage(target, Combat.hitscan(player), lock.charge, DEADEYE_BLOW);
		Combat.credit(player, dealt, target);
		Combat.addFireForDamage(player, dealt, target);
		trail(player.level(), fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD), at);
		if (dealt > 0) {
			Effects.hitMarker(player, target, false);
			if (!target.isAlive()) {
				// a Deadeye kill (022F #452 -> 01D6)
				Voice.say(player, "deadeye_kill", 1);
			}
		}
	}

	private static void endDeadeye(ServerPlayer player, HeroState s, CassidyState c) {
		if (c.deadeye < 0) {
			return;
		}
		c.deadeye = -1;
		c.deadeyeFire = -1;
		c.deadeyeCancel = -1;
		c.deadeyeOrder = new int[0];
		c.locks.clear();
		c.lineAt = -1;
		s.ultActive = 0;
		speed(player, DEADEYE_SPEED, 0);
		send(player, new DeadeyePayload(new int[0], new float[0]));
		Combat.animate(player, "deadeye_end");
		CassidySounds.split(player, "deadeye.end", player.position(), 60);
	}

	// ---- damage taken: Combat Roll's and Deadeye's cuts ----

	/** The factor on the damage this player takes now: 0.5 rolling, 0.6 -> 1 through Deadeye (LivingEntityMixin). */
	public static float takenFactor(Entity victim) {
		if (!(victim instanceof ServerPlayer player)) {
			return 1;
		}
		HeroState s = HeroManager.state(player);
		if (s == null || s.hero != Hero.CASSIDY || !HeroManager.heroMode(player)) {
			return 1;
		}
		return (float) takenFactorRule(s.cassidy.roll >= 0, s.cassidy.deadeye);
	}

	/** takenFactor's rule: rolling, or {@code deadeyeTicks} into Deadeye (-1 not in it). */
	public static double takenFactorRule(boolean rolling, int deadeyeTicks) {
		if (rolling) {
			return 1 - ROLL_DAMAGE_TAKEN;
		}
		if (deadeyeTicks >= 0) {
			int channel = deadeyeTicks - Ow.ticks(DEADEYE_CAST);
			return 1 - DEADEYE_DAMAGE_TAKEN * (channel < 0 ? 1 : deadeyeFade(channel));
		}
		return 1;
	}

	/** Whether quick melee is his now: not during Deadeye (W rev 409375: no jump, crouch or other abilities while channelling). */
	public static boolean meleeBlocked(HeroState s) {
		return s.hero == Hero.CASSIDY && s.cassidy.deadeye >= 0;
	}

	/**
	 * A movement speed modifier of his own, or none at 0: Giddy Up's a share of his base, Deadeye's slow multiplying the rest
	 * (ADD_MULTIPLIED_TOTAL), so with another Cassidy's hinder it's x0.3 x0.5 and not -0.7 - 0.5 clamped to a stop (critic b79a4d1 #6;
	 * ours that they multiply: no source read says how Overwatch stacks two slows).
	 */
	private static void speed(ServerPlayer player, Identifier id, double amount) {
		AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(id);
		if (amount == 0) {
			instance.removeModifier(id);
		} else if (current == null || current.amount() != amount) {
			instance.removeModifier(id);
			instance.addTransientModifier(new AttributeModifier(id, amount, id.equals(DEADEYE_SPEED)
				? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL : AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	// ---- presses ----

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		CassidyState c = s.cassidy;
		// (Shift: the roll comes from his client's own, input below; a bare Shift press his client refused does nothing here)
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0) {
			startFlash(player, s, c);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0) {
			if (c.deadeye >= 0) {
				c.ultAgain = true;
			} else {
				startDeadeye(player, s, c);
			}
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && c.flashCast < 0) {
			startReload(player, s);
		}
	}

	/** Packets his client sent the server hasn't seen yet (mod 256). */
	public static int pending(int sent, int seen) {
		return (sent - seen) & 0xFF;
	}

	/**
	 * Whether his client may roll now (ClientCassidy): the last sync's state says the server will take it, and the server has seen
	 * every packet sent that could change that (an E or a Q it may have started, his last roll), and he isn't rolling there.
	 */
	public static boolean clientMay(int pending, double cooldown, int mode, boolean asleep, boolean riding, boolean rollingHere) {
		return pending == 0 && !rollingHere && mayRoll((mode & MODE_ROLL) != 0, cooldown, (mode & MODE_DEADEYE) != 0,
			(mode & MODE_CASTING) != 0, (mode & MODE_SILENCED) != 0, (mode & MODE_HINDERED) != 0, asleep, riding);
	}

	/** Whether a packet counts toward pressSeen: E, Q, or a roll (his client counts the same: ClientCassidy). */
	public static boolean counts(int pressed, int extra) {
		return (pressed & (HeroInputPayload.PRESS_ABILITY_2 | HeroInputPayload.PRESS_ULTIMATE)) != 0 || extra != 0;
	}

	/**
	 * Each packet from his client, before anything strips its presses (a hack) or drops them (out of hero mode): counted, and a roll it
	 * made (its direction with it: 0 forward, 1 right, 2 back, 3 left, HeroInputPayload.extra - 1) taken or refused. HeroManager runs
	 * this before the kit's press, so a roll and an E in one packet go as his client saw them: the roll, the throw refused.
	 */
	public static void input(ServerPlayer player, HeroState s, int pressed, int extra) {
		CassidyState c = s.cassidy;
		if (counts(pressed, extra)) {
			c.pressSeen = (c.pressSeen + 1) & 0xFF;
		}
		if (extra != 0) {
			s.cassidyRollDir = Math.clamp(extra - 1, 0, 3);
			clientRoll(player, s, c, s.cassidyRollDir);
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		CassidyState c = s.cassidy;
		int mode = (c.deadeye >= 0 ? MODE_DEADEYE : 0) | (c.roll >= 0 ? MODE_ROLL : 0) | (c.hindered ? MODE_HINDERED : 0) | (c.silenced ? MODE_SILENCED : 0)
			| (c.silverAim ? MODE_SILVER : 0) | (c.deadeyeFire >= 0 || c.deadeyeCancel >= 0 ? MODE_FIRING : 0) | (c.flashCast >= 0 ? MODE_CASTING : 0)
			| (c.pressSeen & 0xFF) << SEEN_SHIFT | (c.rollCut & 0xFF) << CUT_SHIFT;
		float left = c.deadeye < 0 ? 0 : (float) Math.max(0, (Ow.ticks(DEADEYE_CAST + DEADEYE_TIME) - c.deadeye) / 20.0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, c.deadeye >= 0,
			s.primaryAmmo, s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(RELOAD)) : -1,
			0, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero, s.onFire,
			dev.overcraft.hero.Perks.level(s), dev.overcraft.hero.Perks.progress(s), dev.overcraft.hero.Perks.ready(s),
			Perk.index(s.minorPerk), Perk.index(s.majorPerk), (float) (c.silverCooldown / 20.0), mode, left, 0);
	}

	/** Out of play (#100): Deadeye ends (its charge stays spent), the roll, the fan and the throw end, his speeds go. */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, HeroKit.OutOfPlay why) {
		CassidyState c = s.cassidy;
		endDeadeye(player, s, c);
		if (c.roll >= 0) {
			c.roll = -1;
			s.ability1Cooldown = Ow.ticks(ROLL_COOLDOWN);
		}
		if (c.flashCast >= 0) {
			c.flashCast = -1;
		}
		c.giddy = -1;
		c.fanLeft = 0;
		c.silverAim = false;
		speed(player, DEADEYE_SPEED, 0);
		speed(player, GIDDY_SPEED_ID, 0);
		if (why.forGood()) {
			for (ServerLevel level : player.level().getServer().getAllLevels()) {
				for (FlashbangEntity f : level.getEntities(dev.overcraft.registry.OcEntities.FLASHBANG, f -> f.getOwner() == player)) {
					f.discard();
				}
			}
		}
	}
}
