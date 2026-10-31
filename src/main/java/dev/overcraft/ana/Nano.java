package dev.overcraft.ana;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.registry.OcSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who Ana's Nano Boost is on (her graph 0B9E, the buff 0F17): for 8 s they deal 50% more damage (with anything: a hero's
 * weapons, a sword, a wolf's bite, an arrow; added to a Mercy's boost, mercy.Boosts) and take 50% less (LivingEntityMixin,
 * before shields take it). It ends early when they die; Ana's death doesn't end it (tools/reference/ana.md).
 */
public final class Nano {
	private record Nanoed(LivingEntity entity, long until, UUID ana) {
	}

	private static final Map<UUID, Nanoed> NANOED = new HashMap<>();

	private Nano() {
	}

	public static void boost(LivingEntity entity, UUID ana) {
		NANOED.put(entity.getUUID(), new Nanoed(entity, entity.level().getGameTime() + Ow.ticks(AnaStats.NANO_TIME), ana));
	}

	public static boolean boosted(@Nullable Entity entity) {
		if (entity == null) {
			return false;
		}
		Nanoed n = NANOED.get(entity.getUUID());
		return n != null && entity.level().getGameTime() < n.until && n.entity.isAlive();
	}

	/** What Nano adds to the factor on the damage this attacker deals now (0 when not boosted). */
	public static double dealtBonus(@Nullable Entity attacker) {
		return boosted(attacker) ? AnaStats.NANO_DAMAGE_DEALT : 0;
	}

	/** The factor on the damage this victim takes now (1 when not boosted). */
	public static float takenFactor(Entity victim) {
		return boosted(victim) ? (float) (1 - AnaStats.NANO_DAMAGE_TAKEN) : 1;
	}

	/** Seconds of boost left (for the HUD), 0 when none. */
	public static double left(@Nullable Entity entity) {
		Nanoed n = entity == null ? null : NANOED.get(entity.getUUID());
		return n == null || !boosted(entity) ? 0 : (n.until - entity.level().getGameTime()) / 20.0;
	}

	public static void tick(MinecraftServer server) {
		NANOED.values().removeIf(n -> {
			boolean over = !n.entity.isAlive() || n.entity.isRemoved() || n.entity.level().getGameTime() >= n.until;
			if (over) {
				ended(n, server.getPlayerList().getPlayer(n.ana));
			}
			return over;
		});
	}

	/**
	 * The boost's bed (5FAA) on the ally for everyone else, following them: their side's copy or the enemies' (the
	 * boosted player's own is in their head: Ana.nanoCast). Its files run the boost's length, so it plays once.
	 */
	public static void loopOn(LivingEntity target, ServerPlayer ana) {
		for (ServerPlayer listener : ((ServerLevel) target.level()).players()) {
			boolean friendly = Allies.isSelfOrAlly(ana, listener);
			double metres = friendly ? 100 : 60;
			if (listener == target || listener.distanceTo(target) > metres + 8) {
				continue;
			}
			listener.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(
				friendly ? OcSounds.ANA_NANO_LOOP_3P : OcSounds.ANA_NANO_LOOP_ENEMY), SoundSource.PLAYERS, target,
				(float) (metres / 16), 1, target.getRandom().nextLong()));
		}
	}

	/** The boost ended (its 8 s, or the ally died): its end (38E6 / 3857's Stop events), and the beds stop. */
	private static void ended(Nanoed n, @Nullable ServerPlayer ana) {
		if (!(n.entity.level() instanceof ServerLevel level)) {
			return;
		}
		for (ServerPlayer listener : level.players()) {
			if (listener.distanceTo(n.entity) > 108) {
				continue;
			}
			for (var loop : new net.minecraft.sounds.SoundEvent[] {OcSounds.ANA_NANO_LOOP, OcSounds.ANA_NANO_LOOP_3P, OcSounds.ANA_NANO_LOOP_ENEMY}) {
				listener.connection.send(new ClientboundStopSoundPacket(loop.location(), SoundSource.PLAYERS));
			}
		}
		dev.overcraft.hero.Effects.play(level, "ana_nano_end", n.entity.position(), net.minecraft.world.phys.Vec3.ZERO, n.entity, 0, ana);
		if (n.entity instanceof ServerPlayer boosted) {
			HeroSounds.toOwner(boosted, OcSounds.ANA_NANO_END);
		}
		HeroSounds.at(level, OcSounds.ANA_NANO_END_3P, OcSounds.ANA_NANO_END_ENEMY, ana, n.entity.getBoundingBox().getCenter(), 100,
			n.entity instanceof ServerPlayer boosted ? boosted : null);
	}

	public static void clear() {
		NANOED.clear();
	}
}
