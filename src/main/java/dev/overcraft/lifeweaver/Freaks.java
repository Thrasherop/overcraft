package dev.overcraft.lifeweaver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * The freak list (owner, 2026-11-20): players on it get their own Tree of Life as Lifeweaver, its own model
 * (hero_models/lifeweaver_tree_freak.glb in the local pack; the normal tree until there is one) and its own cast line
 * (lifeweaver.ult.freak, everyone hears it). Everyone else casts the normal one. By name, any case, so a player not online
 * can be added; kept per world in {@code <world>/overcraft/freaks.json}. SaltySardines is on it by default.
 * {@code /addfreak <name>}, {@code /removefreak <name>} and {@code /freaks} (operators).
 */
public final class Freaks {
	private static final Set<String> DEFAULT = Set.of("saltysardines");
	private static final Gson GSON = new Gson();
	/** Lowercased names. */
	private static final Set<String> NAMES = new TreeSet<>(DEFAULT);
	private static @Nullable Path file;

	private Freaks() {
	}

	public static boolean is(ServerPlayer player) {
		return NAMES.contains(player.getScoreboardName().toLowerCase(Locale.ROOT));
	}

	/** Reads this world's list (SERVER_STARTED); a world with none starts from the default. */
	public static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("overcraft").resolve("freaks.json");
		NAMES.clear();
		try {
			if (Files.exists(file)) {
				for (var name : GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonArray.class)) {
					NAMES.add(name.getAsString().toLowerCase(Locale.ROOT));
				}
				return;
			}
		} catch (Exception e) {
			dev.overcraft.Overcraft.LOG.warn("Couldn't read {}: {}", file, e.toString());
		}
		NAMES.addAll(DEFAULT);
	}

	private static void save() {
		if (file == null) {
			return;
		}
		try {
			Files.createDirectories(file.getParent());
			JsonArray names = new JsonArray();
			NAMES.forEach(names::add);
			Files.writeString(file, GSON.toJson(names), StandardCharsets.UTF_8);
		} catch (Exception e) {
			dev.overcraft.Overcraft.LOG.warn("Couldn't write {}: {}", file, e.toString());
		}
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("addfreak")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.argument("player", StringArgumentType.word())
				.suggests((c, b) -> SharedSuggestionProvider.suggest(c.getSource().getOnlinePlayerNames(), b))
				.executes(c -> change(c, true))));
		dispatcher.register(Commands.literal("removefreak")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.argument("player", StringArgumentType.word())
				.suggests((c, b) -> SharedSuggestionProvider.suggest(NAMES, b))
				.executes(c -> change(c, false))));
		dispatcher.register(Commands.literal("freaks")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.executes(c -> {
				c.getSource().sendSuccess(() -> Component.literal(NAMES.isEmpty() ? "The freak list is empty"
					: "Freaks (" + NAMES.size() + "): " + String.join(", ", NAMES)), false);
				return NAMES.size();
			}));
	}

	private static int change(CommandContext<CommandSourceStack> c, boolean add) {
		String name = StringArgumentType.getString(c, "player");
		String key = name.toLowerCase(Locale.ROOT);
		boolean changed = add ? NAMES.add(key) : NAMES.remove(key);
		if (changed) {
			save();
		}
		String text = changed ? (add ? name + " is now a freak" : name + " is no longer a freak")
			: (add ? name + " is already a freak" : name + " isn't on the freak list");
		if (changed) {
			c.getSource().sendSuccess(() -> Component.literal(text), true);
		} else {
			c.getSource().sendFailure(Component.literal(text));
		}
		return changed ? 1 : 0;
	}
}
