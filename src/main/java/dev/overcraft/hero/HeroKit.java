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

	/** Why a hero leaves play (HeroManager.outOfPlay). */
	enum OutOfPlay {
		/** G: plain Minecraft, the hero kept for later. */
		MINECRAFT_MODE,
		/** Died: they come back as the same hero. */
		DEATH,
		/** Another hero picked (or none). */
		SWITCH,
		/** Left the world. */
		LEAVE;

		/** The hero is gone (switched away, left): what they placed in the world goes too, as in an Overwatch hero swap. */
		public boolean forGood() {
			return this == SWITCH || this == LEAVE;
		}
	}

	/**
	 * The hero leaves play (#100): G, death, a hero switch, leaving. Everything the hero is doing ends here, its effects on
	 * the player with it (a speed, invisibility, a flight, a held ability, a tow, a carried enemy, a cast under way), so
	 * nothing of it carries into Minecraft mode, a corpse, the next hero or the next session, and nothing resumes when they
	 * come back. An ult under way ends too, and its charge stays spent; an ability whose effect has gone out still costs its
	 * cooldown (ours, open for the owner: critique_systems M3; a player's G is one key from WASD). What the hero put in the world
	 * (a platform, a beacon, a ray or a tree that has landed) stays through G and death and goes when {@code why.forGood()}.
	 * Effects on others that the ability already gave (a sleep, a boost, a heal) run their course, as in Overwatch. The one
	 * place each kit hooks in; HeroManager calls it, with the hero's state as it was, before anything is reset.
	 */
	default void leavePlay(ServerPlayer player, HeroState state, OutOfPlay why) {
	}
}
