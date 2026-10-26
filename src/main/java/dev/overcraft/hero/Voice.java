package dev.overcraft.hero;

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

	private Voice() {
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
