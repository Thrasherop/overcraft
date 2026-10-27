package dev.overcraft.hero;

import dev.overcraft.Overcraft;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import java.util.Optional;

/**
 * Changing teams without operator commands (the owner, 2026-10-26: "make there a good easy way for players to change
 * teams. there should be a gamerule to allow non-ops to change teams, default to true"). Teams are Minecraft's
 * scoreboard teams, which already decide allies (Allies): no team at all is everyone together. Overwatch's two sides,
 * Blue and Red, are made the first time someone joins one, with friendly fire off as in Overwatch; any other team an
 * operator made can be joined too. From the Overwatch menu (CHANGE TEAM) or {@code /overcraft team}.
 */
public final class Teams {
	/** {@code /gamerule overcraft:allow_team_change}: players who aren't operators may change teams (on by default). */
	public static final GameRule<Boolean> ALLOW_TEAM_CHANGE = GameRuleBuilder.forBoolean(true).category(GameRuleCategory.PLAYER)
		.buildAndRegister(Overcraft.id("allow_team_change"));

	public static final String BLUE = "blue";
	public static final String RED = "red";

	private Teams() {
	}

	/** Registers the game rule at mod start (loading this class does). */
	public static void init() {
	}

	public static boolean mayChange(ServerPlayer player) {
		return player.level().getGameRules().get(ALLOW_TEAM_CHANGE) || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** Puts the player on a team ("" for none) and says what happened; a message saying why not when it can't. */
	public static Component join(ServerPlayer player, String name) {
		if (!mayChange(player)) {
			return Component.translatable("overcraft.team.not_allowed");
		}
		Scoreboard scoreboard = player.level().getScoreboard();
		String who = player.getScoreboardName();
		if (name.isEmpty()) {
			scoreboard.removePlayerFromTeam(who);
			return Component.translatable("overcraft.team.left");
		}
		PlayerTeam team = scoreboard.getPlayerTeam(name);
		if (team == null) {
			if (!name.equals(BLUE) && !name.equals(RED)) {
				return Component.translatable("overcraft.team.no_such", name);
			}
			team = scoreboard.addPlayerTeam(name);
			team.setDisplayName(Component.translatable("overcraft.team." + name));
			team.setColor(Optional.of(name.equals(BLUE) ? TeamColor.BLUE : TeamColor.RED));
			team.setAllowFriendlyFire(false);
		}
		scoreboard.addPlayerToTeam(who, team);
		return Component.translatable("overcraft.team.joined", team.getFormattedDisplayName());
	}
}
