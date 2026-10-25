package dev.overcraft.hero;

import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerPlayer;

/** One hero's weapons and abilities, run on the server. */
public interface HeroKit {
	/** Fresh kit: full ammo, no cooldowns. */
	void equip(ServerPlayer player, HeroState state);

	/** Every tick. {@code controls} is true while hotbar slot 1 is selected (the hero's keys are live). */
	void tick(ServerPlayer player, HeroState state, boolean controls);

	/** Ability keys ({@link dev.overcraft.net.HeroInputPayload} PRESS_ flags), only while controls are live. */
	void press(ServerPlayer player, HeroState state, int pressed);

	HeroSyncPayload sync(HeroState state);
}
