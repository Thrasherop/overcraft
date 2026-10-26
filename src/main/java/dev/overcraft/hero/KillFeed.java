package dev.overcraft.hero;

import dev.overcraft.entity.KunaiEntity;
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
 * every player's death (a hero killed by a zombie shows too), to players within RANGE.
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
		boolean heroKiller = killer instanceof Player player && HeroManager.hero(player) != null;
		if (!heroKiller && !(victim instanceof Player)) {
			return;
		}
		String weapon = weapon(source, killer);
		String killerName = killer == null ? "" : killer.getDisplayName().getString();
		String killerHero = heroOf(killer);
		String victimName = victim.getDisplayName().getString();
		String victimHero = heroOf(victim);
		for (ServerPlayer viewer : PlayerLookup.around(level, victim.position(), RANGE)) {
			boolean killerAlly = killer != null && (killer == viewer || Allies.wasAlly(viewer, killer));
			boolean victimAlly = victim == viewer || Allies.wasAlly(viewer, victim);
			ServerPlayNetworking.send(viewer, new KillFeedPayload(killerName, killerHero, typeOf(killer), weapon, victimName,
				victimHero, typeOf(victim), killerAlly, victimAlly, killer == viewer));
		}
	}

	/** What the kill was made with, as the feed shows it: the hero's weapon, or nothing in particular. */
	private static String weapon(DamageSource source, @Nullable Entity killer) {
		if (source.getDirectEntity() instanceof KunaiEntity) {
			return "kunai";
		}
		if (killer instanceof Player player && HeroManager.hero(player) != null && source.getDirectEntity() == killer) {
			return "melee";
		}
		return "";
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
