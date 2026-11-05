package dev.overcraft.hero;

import dev.overcraft.Ow;
import dev.overcraft.ana.AnaStats;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sleep (Ana's Sleep Dart: the status 026F -> 0BDE -> the shared crowd-control graph 224E, tools/reference/ana.md).
 * Asleep a creature does nothing until its time runs out. Damage wakes it, but only once it has slept 0.5 s (earlier
 * damage is ignored, not saved), and the waking takes 0.5 s. A mob is stopped by holding its AI off (aiHeld: never its
 * saved NoAI flag, #97);
 * a player can't move (their client: ClientSleep) or use their hero's keys (HeroManager.controls). Bosses sleep too, and
 * no Minecraft mob counts as a tank for the data's shorter tank sleep (the owner's open questions, ana-plan.md: ours).
 */
public final class Sleep {
	private Sleep() {
	}

	/** One sleeper: since and until when (game time), waking from a hit. */
	private record Sleeper(LivingEntity entity, long since, long until, boolean waking, boolean stun, boolean slides, double gravity) {
		Sleeper(LivingEntity entity, long since, long until, boolean waking) {
			this(entity, since, until, waking, false, false, 1);
		}
	}

	/** (Concurrent: Mob.isNoAi asks this from worldgen threads too.) */
	private static final Map<UUID, Sleeper> SLEEPERS = new java.util.concurrent.ConcurrentHashMap<>();

	public static boolean asleep(LivingEntity entity) {
		return SLEEPERS.containsKey(entity.getUUID());
	}

	/** Puts it to sleep for {@code seconds}; a new sleep replaces one under way (ours: 224E's replace rule is M). */
	public static void put(LivingEntity entity, double seconds) {
		long now = entity.level().getGameTime();
		Sleeper old = SLEEPERS.get(entity.getUUID());
		SLEEPERS.put(entity.getUUID(), new Sleeper(entity, now, now + Ow.ticks(seconds), false));
		if (old == null) {
			// the sleep's bed (1A1F, as long as the sleep): the sleeper's own, everyone else's at it (45 m)
			sound(entity, dev.overcraft.registry.OcSounds.ANA_SLEEP_TARGET, dev.overcraft.registry.OcSounds.ANA_SLEEP);
		}
		if (entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
			Effects.play(level, "ana_asleep", entity.position(), Vec3.ZERO, entity, Ow.ticks(seconds), null);
		}
		if (entity instanceof Mob mob) {
			mob.setTarget(null);
		}
		if (entity instanceof ServerPlayer player) {
			player.setAttached(HeroManager.ASLEEP, true);
		}
	}

	/**
	 * A stun (Brigitte's Shield Bash in Rally, 224E with type 6BBE: 0.75 s): held as a sleeper is (a mob still, a player's
	 * movement and hero keys locked), but damage doesn't end it, and it has none of the sleep's look or sound.
	 */
	public static void stun(LivingEntity entity, double seconds) {
		stun(entity, seconds, false, 1);
	}

	/**
	 * A knockdown (Sigma's Accretion: 224E as Rally's bash, 1.1 s): a stun in which the creature keeps the knockback it was
	 * given. A mob's AI is off, and with it Minecraft's own movement (a NoAI mob never travels: LivingEntity.aiStep), so the
	 * knockdown moves it as Minecraft would (gravity, drag, the ground's friction) until it ends.
	 */
	public static void knockdown(LivingEntity entity, double seconds) {
		stun(entity, seconds, true, 1);
	}

	/** The same with its gravity scaled (Hyper Strike's float: x0.01, 54A5 #404). */
	public static void knockdown(LivingEntity entity, double seconds, double gravity) {
		stun(entity, seconds, true, gravity);
	}

	/**
	 * Whether Overcraft holds this mob's AI off now (asleep, stunned, knocked down, lifted by Gravitic Flux): MobMixin answers
	 * Mob.isNoAi with it, so the mob does nothing, as a NoAI mob does, without its saved NoAI flag ever being touched (#97).
	 * That flag used to be set and put back from this map: a mob unloaded or saved mid-sleep (its chunk left behind, Save
	 * and Quit, a server stop) was written NoAI and stayed a statue for good. Held only for the very object put to sleep, so
	 * the same mob loaded again (a new object, same UUID) is awake, and the client's copy (same UUID, same JVM in single
	 * player) never is.
	 */
	public static boolean aiHeld(Mob mob) {
		if (SLEEPERS.isEmpty()) {
			return dev.overcraft.sigma.GraviticFlux.holds(mob);
		}
		Sleeper s = SLEEPERS.get(mob.getUUID());
		return s != null && s.entity == mob || dev.overcraft.sigma.GraviticFlux.holds(mob);
	}

	/**
	 * This thread is reading mobs' own NoAI flags, not Overcraft's hold (a save, a bucket, a conversion: MobMixin). Per thread:
	 * Mob.addAdditionalSaveData also runs on worldgen threads (a mob placed in a new chunk is saved into it: ProtoChunk.addEntity),
	 * and a shared counter could stick, after which no mob could be held until the game restarted (critique_systems S1).
	 */
	private static final ThreadLocal<Integer> OWN_VIEW = ThreadLocal.withInitial(() -> 0);

	/** Runs {@code read} seeing every mob's own NoAI flag (what gets saved or copied), not the hold. */
	public static <T> T ownAi(java.util.function.Supplier<T> read) {
		OWN_VIEW.set(OWN_VIEW.get() + 1);
		try {
			return read.get();
		} finally {
			OWN_VIEW.set(OWN_VIEW.get() - 1);
		}
	}

	/** Whether isNoAi should answer with the hold now (not while something saves or copies the mob). */
	public static boolean holdApplies(Mob mob) {
		return aiHeld(mob) && OWN_VIEW.get() == 0;
	}

	private static void stun(LivingEntity entity, double seconds, boolean slides, double gravity) {
		long now = entity.level().getGameTime();
		Sleeper old = SLEEPERS.get(entity.getUUID());
		if (old != null && !old.stun) {
			return;
		}
		SLEEPERS.put(entity.getUUID(), new Sleeper(entity, now, now + Ow.ticks(seconds), false, true, slides, gravity));
		if (entity instanceof ServerPlayer player) {
			// held still by their client as a sleeper is (a stunned player walked on through it: found playing, two clients)
			player.setAttached(HeroManager.ASLEEP, true);
		}
	}

	/** {@code own} in the sleeper's head (a player), {@code others} at it for everyone else, out to 45 m (3866). */
	private static void sound(LivingEntity entity, net.minecraft.sounds.SoundEvent own, net.minecraft.sounds.SoundEvent others) {
		if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) {
			return;
		}
		ServerPlayer player = entity instanceof ServerPlayer p ? p : null;
		if (player != null) {
			HeroSounds.toOwner(player, own);
		}
		HeroSounds.at(level, others, entity.getBoundingBox().getCenter(), 45, player);
	}

	/** Damage dealt inside {@link #quietly} doesn't wake anyone (Local Anesthetic's ticks). */
	private static int quiet;

	/** Runs damage that never wakes a sleeper (Ana's Local Anesthetic: D; W). */
	public static float quietly(java.util.function.Supplier<Float> damage) {
		quiet++;
		try {
			return damage.get();
		} finally {
			quiet--;
		}
	}

	/** It took damage (Overcraft's AFTER_DAMAGE): after 0.5 s asleep it wakes, 0.5 s later. */
	public static void onDamaged(LivingEntity entity) {
		Sleeper s = SLEEPERS.get(entity.getUUID());
		if (s == null || s.waking || s.stun || quiet > 0) {
			return;
		}
		long now = entity.level().getGameTime();
		if (now - s.since >= Ow.ticks(AnaStats.SLEEP_WAKE_FROM) && s.until - now > Ow.ticks(AnaStats.SLEEP_WAKING)) {
			SLEEPERS.put(entity.getUUID(), new Sleeper(s.entity, s.since, now + Ow.ticks(AnaStats.SLEEP_WAKING), true));
		}
	}

	/** Each server tick (HeroManager): sleepers stay put, and wake when their time is up. */
	public static void tick(MinecraftServer server) {
		for (Sleeper s : new ArrayList<>(SLEEPERS.values())) {
			LivingEntity e = s.entity;
			if (e.isRemoved() || !e.isAlive() || e.level().getGameTime() >= s.until) {
				wake(s);
				continue;
			}
			if (e instanceof net.minecraft.world.entity.monster.Creeper creeper && !creeper.isIgnited()) {
				// a creeper's fuse runs in its own tick, not in its AI: held with its fuse lit it went off anyway (found playing, #101).
				// It burns back down, as when a target walks off. One lit by flint and steel goes on (Minecraft's: nothing puts that out)
				creeper.setSwellDir(-1);
			}
			if (dev.overcraft.sigma.GraviticFlux.lifted(e)) {
				// a Gravitic Flux moves it (its own tick)
				continue;
			}
			if (s.slides) {
				if (e instanceof Mob mob) {
					// (its AI held off, Minecraft doesn't move it: aiHeld)
					slide(mob, s.gravity);
				}
				continue;
			}
			Vec3 v = e.getDeltaMovement();
			e.setDeltaMovement(0, Math.min(v.y, 0), 0);
		}
	}

	/** One tick of a held mob carried by its speed, as LivingEntity.travel would (gravity, air drag, the ground's friction). */
	public static void slide(Mob mob, double gravity) {
		Vec3 v = mob.getDeltaMovement();
		mob.move(net.minecraft.world.entity.MoverType.SELF, v);
		Vec3 after = mob.getDeltaMovement();
		float friction = mob.onGround()
			? mob.level().getBlockState(mob.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91f : 0.91f;
		mob.setDeltaMovement(after.x * friction, (after.y - mob.getGravity() * gravity) * 0.98, after.z * friction);
		mob.needsSync = true;
	}

	/** The world stopped: nothing held over (an entry would pin its mob and level until the next world's first tick). */
	public static void clear() {
		SLEEPERS.clear();
	}

	private static void wake(Sleeper s) {
		SLEEPERS.remove(s.entity.getUUID());
		if (s.stun) {
			if (s.entity instanceof ServerPlayer player) {
				// (left set, a stunned player stayed frozen for good: found playing, two clients)
				player.removeAttached(HeroManager.ASLEEP);
			}
			return;
		}
		if (s.entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
			// 1A1F stops: its Stop events cut the bed and play the waking
			for (ServerPlayer listener : level.players()) {
				if (listener.distanceTo(s.entity) <= 53) {
					for (var bed : new net.minecraft.sounds.SoundEvent[] {dev.overcraft.registry.OcSounds.ANA_SLEEP,
						dev.overcraft.registry.OcSounds.ANA_SLEEP_TARGET}) {
						listener.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(bed.location(),
							net.minecraft.sounds.SoundSource.PLAYERS));
					}
				}
			}
			if (s.entity.isAlive()) {
				sound(s.entity, dev.overcraft.registry.OcSounds.ANA_WAKE_TARGET, dev.overcraft.registry.OcSounds.ANA_WAKE);
			}
			Effects.play(level, "ana_wake", s.entity.position(), Vec3.ZERO, s.entity, 0, null);
		}
		if (s.entity instanceof ServerPlayer player) {
			player.removeAttached(HeroManager.ASLEEP);
		}
	}
}
