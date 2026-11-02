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
 * damage is ignored, not saved), and the waking takes 0.5 s. A mob is stopped by switching its AI off (restored after);
 * a player can't move (their client: ClientSleep) or use their hero's keys (HeroManager.controls). Bosses sleep too, and
 * no Minecraft mob counts as a tank for the data's shorter tank sleep (the owner's open questions, ana-plan.md: ours).
 */
public final class Sleep {
	private Sleep() {
	}

	/** One sleeper: since and until when (game time), whether its AI was already off, waking from a hit. */
	private record Sleeper(LivingEntity entity, long since, long until, boolean hadNoAi, boolean waking, boolean stun, boolean slides) {
		Sleeper(LivingEntity entity, long since, long until, boolean hadNoAi, boolean waking) {
			this(entity, since, until, hadNoAi, waking, false, false);
		}
	}

	private static final Map<UUID, Sleeper> SLEEPERS = new HashMap<>();

	public static boolean asleep(LivingEntity entity) {
		return SLEEPERS.containsKey(entity.getUUID());
	}

	/** Puts it to sleep for {@code seconds}; a new sleep replaces one under way (ours: 224E's replace rule is M). */
	public static void put(LivingEntity entity, double seconds) {
		long now = entity.level().getGameTime();
		Sleeper old = SLEEPERS.get(entity.getUUID());
		boolean hadNoAi = old != null ? old.hadNoAi : entity instanceof Mob mob && mob.isNoAi();
		SLEEPERS.put(entity.getUUID(), new Sleeper(entity, now, now + Ow.ticks(seconds), hadNoAi, false));
		if (old == null) {
			// the sleep's bed (1A1F, as long as the sleep): the sleeper's own, everyone else's at it (45 m)
			sound(entity, dev.overcraft.registry.OcSounds.ANA_SLEEP_TARGET, dev.overcraft.registry.OcSounds.ANA_SLEEP);
		}
		if (entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
			Effects.play(level, "ana_asleep", entity.position(), Vec3.ZERO, entity, Ow.ticks(seconds), null);
		}
		if (entity instanceof Mob mob) {
			mob.setNoAi(true);
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
		stun(entity, seconds, false);
	}

	/**
	 * A knockdown (Sigma's Accretion: 224E as Rally's bash, 1.1 s): a stun in which the creature keeps the knockback it was
	 * given. A mob's AI is off, and with it Minecraft's own movement (a NoAI mob never travels: LivingEntity.aiStep), so the
	 * knockdown moves it as Minecraft would (gravity, drag, the ground's friction) until it ends.
	 */
	public static void knockdown(LivingEntity entity, double seconds) {
		stun(entity, seconds, true);
	}

	/** Whether its AI was off before Overcraft put it to sleep or stunned it (else its AI now). */
	public static boolean hadNoAi(Mob mob) {
		Sleeper s = SLEEPERS.get(mob.getUUID());
		return s != null ? s.hadNoAi : mob.isNoAi();
	}

	private static void stun(LivingEntity entity, double seconds, boolean slides) {
		long now = entity.level().getGameTime();
		Sleeper old = SLEEPERS.get(entity.getUUID());
		if (old != null && !old.stun) {
			return;
		}
		boolean hadNoAi = old != null ? old.hadNoAi : entity instanceof Mob mob && mob.isNoAi();
		SLEEPERS.put(entity.getUUID(), new Sleeper(entity, now, now + Ow.ticks(seconds), hadNoAi, false, true, slides));
		if (entity instanceof Mob mob) {
			mob.setNoAi(true);
		}
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
			SLEEPERS.put(entity.getUUID(), new Sleeper(s.entity, s.since, now + Ow.ticks(AnaStats.SLEEP_WAKING), s.hadNoAi, true));
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
			if (dev.overcraft.sigma.GraviticFlux.lifted(e)) {
				// a Gravitic Flux moves it (its own tick)
				continue;
			}
			if (s.slides) {
				if (e instanceof Mob mob && mob.isNoAi()) {
					slide(mob);
				}
				continue;
			}
			Vec3 v = e.getDeltaMovement();
			e.setDeltaMovement(0, Math.min(v.y, 0), 0);
		}
	}

	/** One tick of a held mob carried by its speed, as LivingEntity.travel would (gravity, air drag, the ground's friction). */
	public static void slide(Mob mob) {
		Vec3 v = mob.getDeltaMovement();
		mob.move(net.minecraft.world.entity.MoverType.SELF, v);
		Vec3 after = mob.getDeltaMovement();
		float friction = mob.onGround()
			? mob.level().getBlockState(mob.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction() * 0.91f : 0.91f;
		mob.setDeltaMovement(after.x * friction, (after.y - mob.getGravity()) * 0.98, after.z * friction);
		mob.needsSync = true;
	}

	private static void wake(Sleeper s) {
		SLEEPERS.remove(s.entity.getUUID());
		if (s.stun) {
			if (s.entity instanceof Mob mob) {
				mob.setNoAi(s.hadNoAi);
			}
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
		if (s.entity instanceof Mob mob) {
			mob.setNoAi(s.hadNoAi);
		}
		if (s.entity instanceof ServerPlayer player) {
			player.removeAttached(HeroManager.ASLEEP);
		}
	}
}
