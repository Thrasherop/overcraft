package dev.overcraft.client.dev;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dev only (DevServer's "bot", -Dovercraft.dev=true): a second player on the integrated server with no second client (the
 * machine holds one game at a time). A real ServerPlayer on a connection to nowhere: it is in the player list, has a
 * hero, a team and the server's whole hero tick, and takes hits as a player does. It has no client, so it doesn't move, and
 * nothing it would see is checked: what it proves is server rules (a barrier stopping a shot); what a second player sees on
 * their screen still needs two clients. "bot add <name> <hero> <team|-> x y z yaw", "bot secondary <name> on|off"
 * (the right button, held), "bot read <name>", "bot set <name> <health> <barrier>", "bot asleep", "bot antiheal", "bot self", "bot side <type>", "bot remove <name>".
 */
final class DevBot {
	private DevBot() {
	}

	private static final Map<String, ServerPlayer> BOTS = new HashMap<>();
	private static final Map<String, EmbeddedChannel> CHANNELS = new HashMap<>();
	private static boolean hooked;

	static void handle(MinecraftServer server, String arg, JsonObject out) throws Exception {
		CompletableFuture<Void> done = new CompletableFuture<>();
		server.execute(() -> {
			try {
				run(server, arg.split(" "), out);
				done.complete(null);
			} catch (Exception e) {
				done.completeExceptionally(e);
			}
		});
		done.get(10, TimeUnit.SECONDS);
	}

	private static void run(MinecraftServer server, String[] a, JsonObject out) {
		for (EmbeddedChannel channel : CHANNELS.values()) {
			channel.releaseOutbound();
		}
		switch (a[0]) {
			case "add" -> add(server, a, out);
			case "ability1" -> {
				// its Shift, as a key press reaches the kit (#89: a bot Jetpack Cat's Lifeline, for a real client to take)
				var bot = BOTS.get(a[1]);
				HeroState s = HeroManager.state(bot);
				HeroManager.kit(s.hero).press(bot, s, dev.overcraft.net.HeroInputPayload.PRESS_ABILITY_1);
			}
			case "secondary" -> {
				HeroState s = HeroManager.state(BOTS.get(a[1]));
				s.secondaryHeld = a[2].equals("on");
			}
			case "read" -> {
				ServerPlayer bot = BOTS.get(a[1]);
				HeroState s = HeroManager.state(bot);
				out.addProperty("alive", bot.isAlive());
				out.addProperty("health", bot.getHealth());
				out.addProperty("barrierUp", s.barrierUp);
				out.addProperty("barrierHealth", s.barrierHealth);
				out.addProperty("sigmaBarrier", s.sigmaBarrierId == null ? -1 : s.sigmaBarrierId);
				out.addProperty("asleep", dev.overcraft.hero.Sleep.asleep(bot));
				out.addProperty("x", bot.getX());
				out.addProperty("z", bot.getZ());
			}
			case "asleep" -> {
				StringBuilder names = new StringBuilder();
				for (ServerLevel level : server.getAllLevels()) {
					for (var e : level.getAllEntities()) {
						if (e instanceof LivingEntity living && dev.overcraft.hero.Sleep.asleep(living)) {
							names.append(living.getType().toShortString()).append('@').append(Math.round(living.getX())).append(' ');
						}
					}
				}
				out.addProperty("asleep", names.toString().trim());
			}
			case "set" -> {
				// "bot set <name> <health> <barrier>": back to known numbers between shots
				ServerPlayer bot = BOTS.get(a[1]);
				bot.setHealth(Float.parseFloat(a[2]));
				HeroManager.state(bot).barrierHealth = Double.parseDouble(a[3]);
			}
			case "antiheal" -> {
				StringBuilder names = new StringBuilder();
				for (ServerLevel level : server.getAllLevels()) {
					for (var e : level.getAllEntities()) {
						if (e instanceof LivingEntity living && dev.overcraft.hero.Combat.antiHealed(living)) {
							names.append(living.getType().toShortString()).append(' ');
						}
					}
				}
				out.addProperty("antiHealed", names.toString().trim());
			}
			case "self" -> {
				// the local player's numbers no HUD shows
				ServerPlayer me = server.getPlayerList().getPlayers().stream().filter(x -> !BOTS.containsValue(x)).findFirst().orElseThrow();
				HeroState s = HeroManager.state(me);
				out.addProperty("fire", s.fireScore);
				out.addProperty("ult", s.ultCharge);
				out.addProperty("energy", s.sojournEnergy);
				out.addProperty("critAt", s.critAt);
				out.addProperty("now", me.level().getGameTime());
			}
			case "side" -> {
				// "bot side <type>": what Allies says of the nearest of that type, for the local player, and what it's doing
				ServerPlayer me = server.getPlayerList().getPlayers().stream().filter(x -> !BOTS.containsValue(x)).findFirst().orElseThrow();
				LivingEntity best = null;
				for (var e : me.level().getAllEntities()) {
					if (e instanceof LivingEntity living && e != me && living.getType().toShortString().equals(a[1])
						&& (best == null || e.distanceToSqr(me) < best.distanceToSqr(me))) {
						best = living;
					}
				}
				if (best != null) {
					out.addProperty("ally", dev.overcraft.hero.Allies.isAlly(me, best));
					out.addProperty("enemy", dev.overcraft.hero.Allies.isEnemy(me, best));
					out.addProperty("bystander", dev.overcraft.hero.Allies.isBystander(me, best));
					out.addProperty("area", dev.overcraft.hero.Allies.areaHits(me, best));
					if (best instanceof net.minecraft.world.entity.Mob mob) {
						out.addProperty("target", String.valueOf(mob.getTarget()));
						out.addProperty("brainTarget", String.valueOf(mob.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET) ? mob.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).orElse(null) : null));
					}
					out.addProperty("y", best.getY());
				}
			}
			case "remove" -> {
				ServerPlayer bot = BOTS.remove(a[1]);
				CHANNELS.remove(a[1]);
				if (bot != null) {
					server.getPlayerList().remove(bot);
				}
			}
			default -> out.addProperty("error", "add | secondary | read | set | asleep | antiheal | self | side | remove");
		}
	}

	private static void add(MinecraftServer server, String[] a, JsonObject out) {
		String name = a[1];
		GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
		ServerLevel level = server.overworld();
		ServerPlayer bot = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
		float yaw = Float.parseFloat(a[7]);
		bot.snapTo(Double.parseDouble(a[4]), Double.parseDouble(a[5]), Double.parseDouble(a[6]), yaw, 0);
		bot.setYHeadRot(yaw);
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		EmbeddedChannel channel = new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, bot, CommonListenerCookie.createInitial(profile, false));
		// (its hits count from now: a player is invulnerable until their client has loaded)
		bot.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		BOTS.put(name, bot);
		CHANNELS.put(name, channel);
		HeroManager.select(bot, Hero.byId(a[2]));
		if (!a[3].equals("-")) {
			var team = server.getScoreboard().getPlayerTeam(a[3]);
			if (team != null) {
				server.getScoreboard().addPlayerToTeam(name, team);
			}
		}
		if (!hooked) {
			hooked = true;
			// what a client's connection would tick: its effects, its timers (nothing while it stands still and sends nothing)
			ServerTickEvents.END_SERVER_TICK.register(s -> {
				for (ServerPlayer b : BOTS.values()) {
					if (!b.isRemoved() && b.level().getServer() == s) {
						b.doTick();
					}
				}
			});
		}
		out.addProperty("ok", true);
		out.addProperty("hero", String.valueOf(HeroManager.hero(bot)));
	}
}
