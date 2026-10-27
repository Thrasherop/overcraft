package dev.overcraft.hero;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.overcraft.kiriko.KirikoStats;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * The operators' command (#23), for testing and for servers: {@code /overcraft reset [players]} readies every
 * cooldown and refills the ammo; {@code /overcraft ult [players] [percent]} sets the ult charge (100% by default);
 * {@code /overcraft perks [players] [level]} sets the perk level (1-3, 3 by default) with no perks picked, so they can
 * be picked again (#36). Without players, the one running it. {@code /overcraft team join <team>} and {@code /overcraft
 * team leave} change the player's own team, for everyone while the allow_team_change rule is on (Teams).
 */
public final class OvercraftCommand {
	private OvercraftCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// /overcraft team is for everyone (Teams.ALLOW_TEAM_CHANGE decides); the rest is for operators
		dispatcher.register(Commands.literal("overcraft")
			.then(Commands.literal("team")
				.then(Commands.literal("leave").executes(c -> team(c, "")))
				.then(Commands.literal("join").then(Commands.argument("team", StringArgumentType.word())
					.suggests((c, b) -> SharedSuggestionProvider.suggest(teamNames(c.getSource()), b))
					.executes(c -> team(c, StringArgumentType.getString(c, "team"))))))
			.then(Commands.literal("reset")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(c -> reset(c, List.of(c.getSource().getPlayerOrException())))
				.then(Commands.argument("players", EntityArgument.players())
					.executes(c -> reset(c, EntityArgument.getPlayers(c, "players")))))
			.then(Commands.literal("ult")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(c -> ult(c, List.of(c.getSource().getPlayerOrException()), 100))
				.then(Commands.argument("players", EntityArgument.players())
					.executes(c -> ult(c, EntityArgument.getPlayers(c, "players"), 100))
					.then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
						.executes(c -> ult(c, EntityArgument.getPlayers(c, "players"), IntegerArgumentType.getInteger(c, "percent"))))))
			.then(Commands.literal("perks")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(c -> perks(c, List.of(c.getSource().getPlayerOrException()), 3))
				.then(Commands.argument("players", EntityArgument.players())
					.executes(c -> perks(c, EntityArgument.getPlayers(c, "players"), 3))
					.then(Commands.argument("level", IntegerArgumentType.integer(1, 3))
						.executes(c -> perks(c, EntityArgument.getPlayers(c, "players"), IntegerArgumentType.getInteger(c, "level")))))));
	}

	private static int team(CommandContext<CommandSourceStack> c, String name) throws CommandSyntaxException {
		ServerPlayer player = c.getSource().getPlayerOrException();
		Component result = Teams.join(player, name);
		c.getSource().sendSuccess(() -> result, false);
		return 1;
	}

	/** Blue and Red (made when first joined) and every team that exists. */
	private static java.util.Set<String> teamNames(CommandSourceStack source) {
		java.util.Set<String> names = new java.util.TreeSet<>(List.of(Teams.BLUE, Teams.RED));
		names.addAll(source.getServer().getScoreboard().getTeamNames());
		return names;
	}

	private static int perks(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players, int level) {
		int done = 0;
		for (ServerPlayer player : players) {
			HeroState s = HeroManager.state(player);
			if (s == null || s.hero == null) {
				continue;
			}
			s.perkXp = level == 1 ? 0 : level == 2 ? Perks.minorXp(s.hero) : Perks.majorXp(s.hero);
			s.minorPerk = s.majorPerk = null;
			HeroManager.savePerks(player, s);
			done++;
		}
		int count = done;
		c.getSource().sendSuccess(() -> Component.translatable("overcraft.command.perks", count, level), true);
		return done;
	}

	private static int reset(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
		int done = 0;
		for (ServerPlayer player : players) {
			HeroState s = HeroManager.state(player);
			if (s == null || s.hero == null) {
				continue;
			}
			s.ability1Cooldown = s.ability2Cooldown = 0;
			s.meleeCooldown = 0;
			s.weaponRecovery = 0;
			s.primaryAmmo = KirikoStats.KUNAI_AMMO;
			s.secondaryAmmo = KirikoStats.OFUDA_AMMO;
			s.primaryReload = s.secondaryReload = -1;
			done++;
		}
		int count = done;
		c.getSource().sendSuccess(() -> Component.translatable("overcraft.command.reset", count), true);
		return done;
	}

	private static int ult(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players, int percent) {
		int done = 0;
		for (ServerPlayer player : players) {
			HeroState s = HeroManager.state(player);
			if (s == null || s.hero == null) {
				continue;
			}
			s.ultCharge = 0;
			Combat.addUltCharge(player, Combat.ultCost(s.hero) * percent / 100.0);
			done++;
		}
		int count = done;
		c.getSource().sendSuccess(() -> Component.translatable("overcraft.command.ult", count, percent), true);
		return done;
	}
}
