package dev.overcraft.hero;

import dev.overcraft.net.KillFeedPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Overwatch's kill feed (top right): who killed whom, with what. Sent for kills by a player playing a hero and for
 * every player's death (a hero killed by a zombie shows too), to players within RANGE. A hero who kills also gets the
 * kill marker and "ELIMINATED" here, from the death itself, whatever dealt it (#106).
 *
 * <p>What the line shows between the names follows the wiki's "Elimination Feed" (2026-06-19): the weapon's fire, any
 * mode, shows only the arrow; Quick Melee a fist; an ability, a perk's own damage or the ult its icon; a critical hit the
 * small red target after it; a death by the world alone only the arrow and the victim. What dealt a hero's damage comes
 * with it (Blow, given to Combat.damage), so no hero has a rule of its own here (#84, #102).
 */
public final class KillFeed {
	/** Blocks around the death that see the line (ours: about a fight's reach). */
	private static final double RANGE = 64;

	private KillFeed() {
	}

	public static void onDeath(LivingEntity victim, DamageSource source) {
		if (!(victim.level() instanceof ServerLevel level)) {
			return;
		}
		Entity killer = source.getEntity();
		Blow blow = Combat.blow(victim, source);
		if (killer == null && !source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
			// the world dealt it (a fall, the void, lava): a hero's blow in the last 5 s takes the kill, with its icon (the
			// wiki: a stun or knockback is shown when the world finishes the job). Not /kill: a command isn't the world (a
			// /kill of a zombie just shot was credited to the shooter, found playing)
			Combat.LastBlow last = Combat.recentBlow(victim);
			Player hero = last == null ? null : level.getServer().getPlayerList().getPlayer(last.attacker());
			if (hero != null && hero != victim) {
				killer = hero;
				blow = last.blow().crit(false);
			}
		}
		Combat.forget(victim);
		boolean heroKiller = killer instanceof Player player && HeroManager.hero(player) != null;
		if (killer instanceof ServerPlayer hero && hero != victim && HeroManager.hero(hero) != null && HeroManager.heroMode(hero)) {
			Effects.killMarker(hero, victim);
		}
		if (!heroKiller && !(victim instanceof Player)) {
			return;
		}
		String weapon = weapon(source, killer, blow);
		boolean headshot = blow != null && blow.crit();
		String killerName = killer == null ? "" : killer.getDisplayName().getString();
		String killerHero = heroOf(killer);
		String victimName = victim.getDisplayName().getString();
		String victimHero = heroOf(victim);
		for (ServerPlayer viewer : PlayerLookup.around(level, victim.position(), RANGE)) {
			boolean killerAlly = killer != null && (killer == viewer || Allies.wasAlly(viewer, killer));
			boolean victimAlly = victim == viewer || Allies.wasAlly(viewer, victim);
			ServerPlayNetworking.send(viewer, new KillFeedPayload(killerName, killerHero, typeOf(killer), weapon, headshot, victimName,
				victimHero, typeOf(victim), killerAlly, victimAlly, killer == viewer));
		}
	}

	/**
	 * What the kill was made with, as the payload carries it (Blow.feed): a hero's blow as it was dealt; otherwise a blow
	 * by the killer's own hand (a mob's bite, a sword) is melee, anything else (an arrow, the world) the arrow alone.
	 */
	static String weapon(DamageSource source, @Nullable Entity killer, @Nullable Blow blow) {
		if (blow != null) {
			return blow.feed();
		}
		return killer != null && source.getDirectEntity() == killer ? "melee" : "weapon";
	}

	/** The entity's type id ("minecraft:zombie"), for the feed's face; "" for none. */
	private static String typeOf(@Nullable Entity entity) {
		return entity == null ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
	}

	private static String heroOf(@Nullable Entity entity) {
		if (entity instanceof Player player) {
			Hero hero = HeroManager.hero(player);
			return hero == null ? "" : hero.id;
		}
		return "";
	}
}
