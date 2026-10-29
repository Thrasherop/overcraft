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
	private record Sleeper(LivingEntity entity, long since, long until, boolean hadNoAi, boolean waking) {
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
		if (entity instanceof Mob mob) {
			mob.setNoAi(true);
			mob.setTarget(null);
		}
		if (entity instanceof ServerPlayer player) {
			player.setAttached(HeroManager.ASLEEP, true);
		}
	}

	/** It took damage (Overcraft's AFTER_DAMAGE): after 0.5 s asleep it wakes, 0.5 s later. */
	public static void onDamaged(LivingEntity entity) {
		Sleeper s = SLEEPERS.get(entity.getUUID());
		if (s == null || s.waking) {
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
			Vec3 v = e.getDeltaMovement();
			e.setDeltaMovement(0, Math.min(v.y, 0), 0);
		}
	}

	private static void wake(Sleeper s) {
		SLEEPERS.remove(s.entity.getUUID());
		if (s.entity instanceof Mob mob) {
			mob.setNoAi(s.hadNoAi);
		}
		if (s.entity instanceof ServerPlayer player) {
			player.removeAttached(HeroManager.ASLEEP);
		}
	}
}
