package dev.overcraft.hero;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.overcraft.kiriko.KirikoStats;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * The operators' command (#23), for testing and for servers: {@code /overcraft reset [players]} readies every
 * cooldown and refills the ammo; {@code /overcraft ult [players] [percent]} sets the ult charge (100% by default).
 * Without players, the one running it.
 */
public final class OvercraftCommand {
	private OvercraftCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("overcraft")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("reset")
				.executes(c -> reset(c, List.of(c.getSource().getPlayerOrException())))
				.then(Commands.argument("players", EntityArgument.players())
					.executes(c -> reset(c, EntityArgument.getPlayers(c, "players")))))
			.then(Commands.literal("ult")
				.executes(c -> ult(c, List.of(c.getSource().getPlayerOrException()), 100))
				.then(Commands.argument("players", EntityArgument.players())
					.executes(c -> ult(c, EntityArgument.getPlayers(c, "players"), 100))
					.then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
						.executes(c -> ult(c, EntityArgument.getPlayers(c, "players"), IntegerArgumentType.getInteger(c, "percent")))))));
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
