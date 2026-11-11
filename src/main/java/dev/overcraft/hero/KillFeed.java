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
 * small red target after it; a death with no killer the skull (#33, the owner's table). What dealt a hero's damage comes
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
		if (Combat.pushDeath(source)) {
			// the world finished what a hero's push began (a fall, the void, lava, a wall): the hero takes the kill, with the
			// push's icon (the wiki: a stun or knockback is shown when an environmental hazard kills). Not sunburn, drowning
			// or /kill; and a later blow by a mob takes it over (Combat.recentPush)
			Combat.LastPush push = Combat.recentPush(victim);
			Player hero = push == null ? null : level.getServer().getPlayerList().getPlayer(push.attacker());
			if (hero != null && hero != victim) {
				killer = hero;
				blow = push.blow().crit(false);
			}
		}
		Combat.forget(victim);
		dev.overcraft.potg.PlayScore.kill(killer, victim);
		Voice.onKill(victim, source, killer, blow);
		boolean heroKiller = blow != null || killer instanceof Player player && HeroManager.hero(player) != null;
		if (!heroKiller && !(victim instanceof Player)) {
			return;
		}
		String weapon = weapon(source, killer, blow);
		boolean headshot = blow != null && blow.crit();
		String killerName = killer == null ? "" : killer.getDisplayName().getString();
		// the portrait of the hero who dealt it, even if the killer has switched hero since (a dose, a torpedo, a fall)
		String killerHero = blow != null && blow.hero() != null ? blow.hero().id : heroOf(killer);
		String victimName = victim.getDisplayName().getString();
		String victimHero = heroOf(victim);
		java.util.Set<ServerPlayer> viewers = new java.util.LinkedHashSet<>(PlayerLookup.around(level, victim.position(), RANGE));
		if (killer instanceof ServerPlayer own) {
			// the killer always sees their own line, however far the shot (Ana's rifle): the client's ELIMINATED reads the
			// victim's name from it when the victim is past tracking range
			viewers.add(own);
		}
		for (ServerPlayer viewer : viewers) {
			boolean killerAlly = killer != null && (killer == viewer || Allies.wasAlly(viewer, killer));
			boolean victimAlly = victim == viewer || Allies.wasAlly(viewer, victim);
			ServerPlayNetworking.send(viewer, new KillFeedPayload(killerName, killerHero, typeOf(killer), weapon, headshot, victimName,
				victimHero, typeOf(victim), killerAlly, victimAlly, killer == viewer));
		}
		// the kill marker and ELIMINATED, after the line: a hero's blow (whatever mode they're in now) or a kill in hero mode
		if (killer instanceof ServerPlayer hero && hero != victim && (blow != null || HeroManager.hero(hero) != null && HeroManager.heroMode(hero))) {
			Effects.killMarker(hero, victim);
		}
	}

	/**
	 * What the kill was made with, as the payload carries it (Blow.feed): a hero's blow as it was dealt; otherwise a blow
	 * by the killer's own hand (a mob's bite, a sword) is melee, anything else the arrow alone (a skeleton's arrow: its
	 * weapon). A death with no killer at all draws the skull on the client (#33, the owner's table, kept).
	 */
	static String weapon(DamageSource source, @Nullable Entity killer, @Nullable Blow blow) {
		return weapon(blow, killer != null && source.getDirectEntity() == killer);
	}

	/** The same, from the blow and whether the killer dealt it by hand. Package-private for BlowTest. */
	static String weapon(@Nullable Blow blow, boolean byHand) {
		return blow != null ? blow.feed() : byHand ? "melee" : "weapon";
	}

	/**
	 * What a feed line draws between the names, left to right, from its payload (the client draws each token): "skull"
	 * (no killer: #33), "fist" (Quick Melee, a mob's bite), "icon:<kind>:<slug>" (the hero's art for that slug: an
	 * ability, a perk's own damage, the ult, a weapon mode or melee with an icon of its own), then "headshot". Weapon fire
	 * draws nothing (only the arrow). Here, not in the client, so BlowTest can check it.
	 */
	public static java.util.List<String> iconTokens(String weapon, boolean noKiller, boolean headshot) {
		java.util.List<String> tokens = new java.util.ArrayList<>(2);
		int colon = weapon.indexOf(':');
		String kind = colon > 0 ? weapon.substring(0, colon) : weapon;
		String slug = colon > 0 ? weapon.substring(colon + 1) : "";
		if (noKiller) {
			tokens.add("skull");
		} else if (!slug.isEmpty()) {
			tokens.add("icon:" + kind + ":" + slug);
		} else if (kind.equals("melee")) {
			tokens.add("fist");
		}
		if (headshot) {
			tokens.add("headshot");
		}
		return tokens;
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
