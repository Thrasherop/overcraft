package dev.overcraft.potg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The end screen's enemy side (owner, 2026-11-11: "if a zombie killed two players have it show up there. Thats so funny.
 * I imagine the dragon will always be there."): per mob, over the run, what it did to players: players killed and damage
 * dealt (Overwatch HP), its type and custom name. The top few are kept, and every dragon, saved with the world beside
 * the highlights (overcraft/potg/mobs.json); at a dragon's decision (tick 199) the list is copied to mobs_last.json for
 * that end screen and cleared. Data only: the screen is a later step.
 */
public final class MobStats {
	private MobStats() {
	}

	/** Kept besides the dragons (ours). */
	public static final int KEPT = 8;

	public static final class Mob {
		public String uuid = "";
		public String type = "";
		public @Nullable String name;
		public int playersKilled;
		public double damageToPlayers;
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, Mob> MOBS = new LinkedHashMap<>();
	private static boolean loaded;
	private static boolean dirty;

	private static Path file(MinecraftServer server, String name) {
		return PotgRecorder.root(server).resolve(name);
	}

	private static void load(MinecraftServer server) {
		if (loaded) {
			return;
		}
		loaded = true;
		Path f = file(server, "mobs.json");
		if (Files.exists(f)) {
			try (Reader r = Files.newBufferedReader(f)) {
				List<Mob> list = GSON.fromJson(r, new TypeToken<List<Mob>>() { }.getType());
				if (list != null) {
					for (Mob m : list) {
						MOBS.put(m.uuid, m);
					}
				}
			} catch (Exception e) {
				Overcraft.LOG.warn("[POTG] couldn't read {}: {}", f, e.toString());
			}
		}
	}

	private static @Nullable Mob of(Entity attacker, MinecraftServer server) {
		if (!(attacker instanceof LivingEntity) || attacker instanceof Player) {
			return null;
		}
		load(server);
		Mob m = MOBS.computeIfAbsent(attacker.getUUID().toString(), k -> new Mob());
		m.uuid = attacker.getUUID().toString();
		m.type = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString();
		m.name = attacker.hasCustomName() && attacker.getCustomName() != null ? attacker.getCustomName().getString() : null;
		return m;
	}

	/** AFTER_DAMAGE: a mob hurt a player. */
	public static void damaged(LivingEntity victim, DamageSource source, float dealt) {
		if (victim instanceof ServerPlayer player && dealt > 0 && source.getEntity() != null) {
			Mob m = of(source.getEntity(), player.level().getServer());
			if (m != null) {
				m.damageToPlayers += Ow.toOw(dealt);
				dirty = true;
			}
		}
	}

	/** AFTER_DEATH: a mob killed a player. */
	public static void killed(ServerPlayer player, DamageSource source) {
		Entity killer = source.getEntity();
		if (killer != null) {
			Mob m = of(killer, player.level().getServer());
			if (m != null) {
				m.playersKilled++;
				dirty = true;
			}
		}
	}

	/** Once a second (PlayScore's step): the list trimmed and saved, if anything changed. */
	public static void flush(MinecraftServer server) {
		if (!dirty) {
			return;
		}
		dirty = false;
		List<Mob> list = top();
		MOBS.clear();
		for (Mob m : list) {
			MOBS.put(m.uuid, m);
		}
		save(server, "mobs.json", list);
	}

	/** The top {@link #KEPT} by players killed, then damage, and every dragon. */
	public static List<Mob> top() {
		List<Mob> all = new ArrayList<>(MOBS.values());
		all.sort(Comparator.comparingInt((Mob m) -> m.playersKilled).thenComparingDouble(m -> m.damageToPlayers).reversed());
		List<Mob> out = new ArrayList<>();
		for (Mob m : all) {
			if (out.size() < KEPT || m.type.equals("minecraft:ender_dragon")) {
				out.add(m);
			}
		}
		return out;
	}

	/** The dragon's decision (tick 199): the list as it stands goes to mobs_last.json (the end screen's), then a new run. */
	public static void decided(MinecraftServer server, EnderDragon dragon) {
		load(server);
		of(dragon, server);
		List<Mob> list = top();
		save(server, "mobs_last.json", list);
		Overcraft.LOG.info("[POTG] mobs this run: {}", GSON.toJson(list).replaceAll("\\s+", " "));
		MOBS.clear();
		dirty = true;
		flush(server);
		save(server, "mobs.json", List.of());
	}

	/** The list of the last dragon's decision (mobs_last.json): the end screen's enemy side. */
	public static List<Mob> last(MinecraftServer server) {
		Path f = file(server, "mobs_last.json");
		if (Files.exists(f)) {
			try (Reader r = Files.newBufferedReader(f)) {
				List<Mob> list = GSON.fromJson(r, new TypeToken<List<Mob>>() { }.getType());
				if (list != null) {
					return list;
				}
			} catch (Exception e) {
				Overcraft.LOG.warn("[POTG] couldn't read {}: {}", f, e.toString());
			}
		}
		return List.of();
	}

	private static void save(MinecraftServer server, String name, List<Mob> list) {
		try {
			Files.createDirectories(PotgRecorder.root(server));
			try (Writer w = Files.newBufferedWriter(file(server, name))) {
				GSON.toJson(list, w);
			}
		} catch (IOException e) {
			Overcraft.LOG.warn("[POTG] couldn't save {}: {}", name, e.toString());
		}
	}

	public static void clear() {
		MOBS.clear();
		loaded = false;
		dirty = false;
	}
}
