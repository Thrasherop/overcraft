package dev.overcraft.hero;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.overcraft.Overcraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Heroes' voice lines on their abilities ("I'm here!" on Swift Step): heard around the hero, not every time (chance),
 * and never two close together (the game spaces them too; the gap here is ours).
 */
public final class Voice {
	/** Ticks between two lines from one hero (ours). */
	private static final int GAP_TICKS = 60;
	/** A second kill within this many ticks of the last is a multikill (ours: 4 s). */
	private static final int KILL_CHAIN = 80;

	private Voice() {
	}

	/**
	 * The hero's line for a moment of the game ("ult_ready", "elimination", "death"...: sound events named
	 * "<hero>.voice.<moment>", the game's own lines through the local pack), heard around them. {@code spaced}: kept
	 * out of the gap after another line (chatter); unspaced lines (death, the ult) always play.
	 */
	public static void say(ServerPlayer player, String moment, float chance, boolean spaced) {
		HeroState state = HeroManager.state(player);
		if (state == null || state.hero == null) {
			return;
		}
		SoundEvent line = BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id(state.hero.id + ".voice." + moment));
		if (line == null) {
			return;
		}
		long now = player.level().getGameTime();
		if (spaced && now - state.voiceAt < GAP_TICKS || player.getRandom().nextFloat() >= chance) {
			return;
		}
		state.voiceAt = now;
		player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), line, SoundSource.PLAYERS, 1.0f, 1.0f);
	}

	/**
	 * A line said at most once per {@code minTicks} (on top of the usual spacing), by chance when it may: for moments that
	 * come many times a second (healing: ofuda land about 14 times a second).
	 */
	public static void sayNowAndThen(ServerPlayer player, String moment, float chance, int minTicks) {
		HeroState state = HeroManager.state(player);
		if (state == null) {
			return;
		}
		long now = player.level().getGameTime();
		Long last = state.saidAt.get(moment);
		if (last != null && now - last < minTicks) {
			return;
		}
		long before = state.voiceAt;
		say(player, moment, chance, true);
		if (state.voiceAt != before) {
			state.saidAt.put(moment, now);
		}
	}

	/**
	 * A hero player killed something: an elimination or melee remark, a multikill (within KILL_CHAIN). (Their own pain
	 * and death cries are their hurt and death sounds: PlayerMixin.)
	 */
	public static void onDeath(LivingEntity victim, DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer killer && killer != victim) {
			HeroState state = HeroManager.state(killer);
			if (state == null || state.hero == null) {
				return;
			}
			long now = killer.level().getGameTime();
			boolean chain = now - state.lastKillAt <= KILL_CHAIN;
			state.lastKillAt = now;
			if (chain) {
				say(killer, "multikill", 0.6f, false);
			} else if (source.getDirectEntity() == killer) {
				say(killer, "melee_kill", 0.6f, true);
			} else {
				say(killer, "elimination", 0.35f, true);
			}
		}
	}

	public static void maybe(ServerPlayer hero, HeroState state, SoundEvent line, float chance) {
		long now = hero.level().getGameTime();
		if (now - state.voiceAt < GAP_TICKS || hero.getRandom().nextFloat() >= chance) {
			return;
		}
		state.voiceAt = now;
		hero.level().playSound(null, hero.getX(), hero.getEyeY(), hero.getZ(), line, SoundSource.PLAYERS, 1.0f, 1.0f);
	}
}
