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
 * their screen still needs two clients. "bot add <name> <hero> <team|-> x y z yaw", "bot input <name> <held> <pressed> <extra>" (a client's input packet, HeroManager.onInput), "bot perk <name> <PERK>", "bot strike <name>", "bot wall <name> <side> <yaw>", "bot wallme", "bot look <name> <yaw> <pitch>", "bot path <name> <vx> <vy> <vz> <ticks>", "bot secondary <name> on|off"
 * (the right button, held), "bot read <name>", "bot set <name> <health> <barrier>", "bot asleep", "bot antiheal", "bot self", "bot side <type>", "bot remove <name>",
 * "bot press <name> <ability1|ability2|ultimate|reload|melee>" (its kit's own press, along the way it faces: /tp it to aim it), "bot charge
 * <name> <ult points>", "bot emote <name> <id>" (Comms' own: what the wheel sends), "bot move <name> x y z", "bot seen <name>" (this client's
 * animator of it: its emote, Catnapper's aim), "bot showstate on|off" (#96's rule, to show what it does), "bot healcut <name>", "bot fx <name> <effect> <ticks>", "bot rays" (Juno's rays this client
 * holds), "bot roll [trace]" (the camera's roll, each frame with trace), "bot fix <119|129|rally|wave> on|off", "bot cc <name|me>" (the statuses on it), "bot held <name|me>", "bot playing [text]" (the sounds running in this client).
 */
final class DevBot {
	private static final java.util.List<String> SHOT_TRACE = new java.util.concurrent.CopyOnWriteArrayList<>();
	private static boolean shotTraceHooked;
	private DevBot() {
	}

	private static final Map<String, ServerPlayer> BOTS = new HashMap<>();
	private static final Map<String, EmbeddedChannel> CHANNELS = new HashMap<>();
	private static boolean hooked;
	private static final Map<String, double[]> PATHS = new HashMap<>();

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
			case "input" -> {
				// "bot input <name> <held> <pressed> <extra>": what a client's input packet does, by the same way in (HeroManager.onInput): its buttons, keys, wall ride
				HeroManager.onInput(BOTS.get(a[1]), new dev.overcraft.net.HeroInputPayload(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])));
				out.addProperty("ok", true);
			}
			case "wall" -> {
				// "bot wall <name> <side> <yaw>": what Lucio's client says riding a wall (#115), by the same way in; side 0: off it
				int side = Integer.parseInt(a[2]);
				HeroManager.onInput(BOTS.get(a[1]), new dev.overcraft.net.HeroInputPayload(side == 0 ? 0 : dev.overcraft.net.HeroInputPayload.HELD_WALL_RIDE, 0, 0,
					side == 0 ? 0 : dev.overcraft.hero.OwMovement.wallCode(side, Float.parseFloat(a[3]))));
				out.addProperty("ok", true);
			}
			case "look" -> {
				// "bot look <name> <yaw> <pitch>": a /tp doesn't reach a bot (it has no client to answer), this does
				ServerPlayer bot = BOTS.get(a[1]);
				float yaw = Float.parseFloat(a[2]);
				bot.setYRot(yaw);
				bot.setYHeadRot(yaw);
				bot.setXRot(Float.parseFloat(a[3]));
				out.addProperty("ok", true);
			}
			case "path" -> {
				// "bot path <name> <vx> <vy> <vz> <ticks>" (m/s): carried along by hand each tick, its known movement set as a client's packets would: a flying carrier, as far as a bot can be one
				PATHS.put(a[1], new double[] {Double.parseDouble(a[2]) / 20, Double.parseDouble(a[3]) / 20, Double.parseDouble(a[4]) / 20, Double.parseDouble(a[5])});
				out.addProperty("ok", true);
			}
			case "fixoff" -> {
				dev.overcraft.hero.Combat.devFixOff = a[1].equals("on");
				out.addProperty("fixoff", dev.overcraft.hero.Combat.devFixOff);
			}
			case "fix" -> {
				// "bot fix <119|rally> on|off": the fix itself on or off, in one build, to show it makes the difference
				boolean on = a[2].equals("on");
				if (a[1].equals("119")) {
					dev.overcraft.hero.Combat.devCleanseOff = !on;
				} else if (a[1].equals("129")) {
					dev.overcraft.hero.Voice.devPainEveryHit = !on;
				} else if (a[1].equals("wave")) {
					dev.overcraft.brigitte.Brigitte.devWaveThrough = !on;
				} else {
					dev.overcraft.brigitte.Brigitte.devRallyTwice = !on;
				}
				out.addProperty(a[1], on);
			}
			case "cc" -> {
				// "bot cc <name|me>": what's on it now, read from the server
				ServerPlayer who = a[1].equals("me") ? server.getPlayerList().getPlayers().stream().filter(x -> !BOTS.containsValue(x)).findFirst().orElseThrow() : BOTS.get(a[1]);
				out.addProperty("asleep", dev.overcraft.hero.Sleep.asleep(who));
				out.addProperty("sleeping", dev.overcraft.hero.Sleep.sleeping(who));
				dev.overcraft.hero.HeroState softState = HeroManager.state(who);
				out.addProperty("softLanding", softState != null && softState.softLanding);
				out.addProperty("climbable", who.onClimbable());
				out.addProperty("inHeroPlay", HeroManager.inHeroPlay(who));
				out.addProperty("fallDistance", who.fallDistance);
				out.addProperty("inWater", who.isInWater());
				out.addProperty("silenced", dev.overcraft.sombra.Hack.silenced(who));
				out.addProperty("hindered", dev.overcraft.cassidy.Hinder.hindered(who));
				out.addProperty("glowing", who.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING));
				out.addProperty("antiHeal", dev.overcraft.hero.Combat.antiHealed(who));
				out.addProperty("healCut", dev.overcraft.hero.Combat.healCut(who));
				out.addProperty("weak", dev.overcraft.sombra.Cyberspace.dealtFactor(who));
				out.addProperty("phased", dev.overcraft.hero.Combat.isPhased(who));
				out.addProperty("speed", who.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
				out.addProperty("gravity", who.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY));
				out.addProperty("health", who.getHealth());
				out.addProperty("lifted", dev.overcraft.sigma.GraviticFlux.lifted(who));
				out.addProperty("now", who.level().getGameTime());
				out.addProperty("held", Boolean.TRUE.equals(who.getAttached(HeroManager.ASLEEP)));
				out.addProperty("fire", who.getRemainingFireTicks());
				StringBuilder effects = new StringBuilder();
				who.getActiveEffects().forEach(e -> effects.append(e.getEffect().unwrapKey().map(k -> k.identifier().getPath()).orElse("?")).append(' '));
				out.addProperty("effects", effects.toString().trim());
				HeroState st = HeroManager.state(who);
				out.addProperty("stealth", st != null && dev.overcraft.sombra.Stealth.stealthed(who, st));
				String by = "";
				for (ServerPlayer p : server.getPlayerList().getPlayers()) {
					HeroState ps = HeroManager.state(p);
					if (ps != null && ps.carried != null && ps.carried == who.getId()) {
						by = p.getScoreboardName();
					}
				}
				out.addProperty("carriedBy", by);
				out.addProperty("ult", st == null ? 0 : st.ultCharge);
				out.addProperty("strike", st != null && st.sigmaStrikeReady);
				out.addProperty("claws", st == null ? 0 : st.clawsCooldown);
				out.addProperty("x", who.getX());
				out.addProperty("y", who.getY());
				out.addProperty("z", who.getZ());
			}
			case "phase" -> {
				// "bot phase <name|me> <seconds>": phased for as long as a test needs, Combat.phase itself (a Suzu gives 0.5 s, too short to aim a real weapon through); dev only
				ServerPlayer who = a[1].equals("me") ? server.getPlayerList().getPlayers().stream().filter(x -> !BOTS.containsValue(x)).findFirst().orElseThrow() : BOTS.get(a[1]);
				dev.overcraft.hero.Combat.phase(who, Double.parseDouble(a[2]));
				out.addProperty("phased", dev.overcraft.hero.Combat.isPhased(who));
			}
			case "sight" -> {
				// "bot sight": from the real player's eyes, each mob within 12 m: Combat.inSight and vanilla's eye-to-eye test
				ServerPlayer me = server.getPlayerList().getPlayers().stream().filter(x -> !BOTS.containsValue(x)).findFirst().orElseThrow();
				StringBuilder b = new StringBuilder(String.format("eye %.2f,%.2f,%.2f: ", me.getEyePosition().x, me.getEyePosition().y, me.getEyePosition().z));
				for (net.minecraft.world.entity.LivingEntity e : me.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, me.getBoundingBox().inflate(12), e -> e instanceof net.minecraft.world.entity.Mob)) {
					b.append(String.format("%s@%.1f,%.1f,%.1f combat=%s vanilla=%s; ", e.getType().toShortString(), e.getX(), e.getY(), e.getZ(), dev.overcraft.hero.Combat.inSight(me.level(), me.getEyePosition(), e, me), me.hasLineOfSight(e)));
				}
				out.addProperty("sight", b.toString());
			}
			case "shottrace" -> {
				// "bot shottrace start", then "bot shottrace": per client tick, each Overwatch projectile's z on this client (c) and on the server (s): an ordinary hit's overshoot, read tick by tick
				if (a.length > 1 && a[1].equals("start")) {
					SHOT_TRACE.clear();
					if (!shotTraceHooked) {
						shotTraceHooked = true;
						net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> {
							if (mc.level == null || SHOT_TRACE.size() > 400) {
								return;
							}
							StringBuilder row = new StringBuilder();
							for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
								if (e instanceof dev.overcraft.entity.OwProjectile) {
									row.append(String.format("c%.2f ", e.getZ()));
								}
							}
							net.minecraft.server.MinecraftServer sv = mc.getSingleplayerServer();
							if (sv != null) {
								for (net.minecraft.world.entity.Entity e : sv.overworld().getAllEntities()) {
									if (e instanceof dev.overcraft.entity.OwProjectile) {
										row.append(String.format("s%.2f ", e.getZ()));
									}
								}
							}
							if (row.length() > 0) {
								SHOT_TRACE.add(mc.level.getGameTime() + " " + row.toString().trim());
							}
						});
					}
				} else {
					out.addProperty("trace", String.join(" | ", SHOT_TRACE));
				}
			}
			case "shots" -> {
				// "bot shots": every Overwatch projectile as this client has it and as the server has it (type@x,y,z), and each Sombra-hackable deployable on the server
				StringBuilder cl = new StringBuilder();
				for (net.minecraft.world.entity.Entity e : net.minecraft.client.Minecraft.getInstance().level.entitiesForRendering()) {
					if (e instanceof dev.overcraft.entity.OwProjectile) {
						cl.append(e.getClass().getSimpleName()).append(String.format("@%.1f,%.1f,%.1f ", e.getX(), e.getY(), e.getZ()));
					}
				}
				StringBuilder sv = new StringBuilder();
				StringBuilder dep = new StringBuilder();
				for (net.minecraft.world.entity.Entity e : server.overworld().getAllEntities()) {
					if (e instanceof dev.overcraft.entity.OwProjectile) {
						sv.append(e.getClass().getSimpleName()).append(String.format("@%.1f,%.1f,%.1f ", e.getX(), e.getY(), e.getZ()));
					}
					if (e instanceof dev.overcraft.entity.PetalPlatformEntity || e instanceof dev.overcraft.entity.TreeOfLifeEntity) {
						dep.append(e.getClass().getSimpleName()).append(" hacked=").append(dev.overcraft.sombra.Hack.hacked(e)).append(' ');
					}
				}
				out.addProperty("client", cl.toString().trim());
				out.addProperty("server", sv.toString().trim());
				out.addProperty("deployables", dep.toString().trim());
			}
			case "playing" -> {
				// "bot playing [text]": the sounds this client has running now (SoundEngine's own list, read by reflection, dev only), for what a stop packet stopped
				StringBuilder names = new StringBuilder();
				try {
					var f = net.minecraft.client.sounds.SoundManager.class.getDeclaredField("soundEngine");
					f.setAccessible(true);
					Object engine = f.get(net.minecraft.client.Minecraft.getInstance().getSoundManager());
					var g = net.minecraft.client.sounds.SoundEngine.class.getDeclaredField("instanceToChannel");
					g.setAccessible(true);
					for (Object o : new java.util.ArrayList<>(((Map<?, ?>) g.get(engine)).keySet())) {
						String id = ((net.minecraft.client.resources.sounds.SoundInstance) o).getIdentifier().toString();
						if (a.length < 2 || id.contains(a[1])) {
							names.append(id).append(' ');
						}
					}
				} catch (ReflectiveOperationException e) {
					names.append("error ").append(e);
				}
				out.addProperty("playing", names.toString().trim());
			}
			case "held" -> {
				// "bot held <name|me>": the held looks the server remembers on it, for late viewers (Effects), and what this client is drawing on it
				ServerPlayer who = a[1].equals("me") ? server.getPlayerList().getPlayers().stream().filter(x -> !BOTS.containsValue(x)).findFirst().orElseThrow() : BOTS.get(a[1]);
				out.addProperty("server", dev.overcraft.hero.Effects.devHeld(who));
				out.addProperty("client", dev.overcraft.client.effect.AnaFx.devHeld(who.getId()) + dev.overcraft.client.effect.SombraFx.devHeld(who.getId()) + dev.overcraft.client.effect.BrigitteFx.devHeld(who.getId()) + dev.overcraft.client.effect.SigmaFx.devHeld(who.getId()));
			}
			case "pos" -> {
				ServerPlayer bot = BOTS.get(a[1]);
				out.addProperty("pos", bot.position().toString());
			}
			case "wallme" -> {
				// the local player as the server has him: what his client told it, and what it sends others (#115)
				ServerPlayer me = server.getPlayerList().getPlayers().stream().filter(x -> !BOTS.containsValue(x)).findFirst().orElseThrow();
				HeroState s = HeroManager.state(me);
				int code = me.getAttachedOrElse(HeroManager.LUCIO_WALL, 0);
				out.addProperty("riding", s.wallRiding);
				out.addProperty("code", code);
				out.addProperty("side", dev.overcraft.hero.OwMovement.wallCodeSide(code));
				out.addProperty("yaw", dev.overcraft.hero.OwMovement.wallCodeYaw(code));
			}
			case "perk" -> {
				// "bot perk <name> <PERK_ENUM>": a perk picked, as the perk menu does
				HeroManager.state(a[1].equals("me") ? server.getPlayerList().getPlayers().stream().filter(x -> !BOTS.containsValue(x)).findFirst().orElseThrow() : BOTS.get(a[1])).majorPerk = dev.overcraft.hero.Perk.valueOf(a[2]);
				out.addProperty("ok", true);
			}
			case "strike" -> {
				// "bot strike <name>": Sigma's Hyper Strike charged
				HeroManager.state(BOTS.get(a[1])).sigmaStrikeReady = true;
				out.addProperty("ok", true);
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
			case "press" -> {
				ServerPlayer bot = BOTS.get(a[1]);
				int flag = switch (a[2]) {
					case "ability1" -> dev.overcraft.net.HeroInputPayload.PRESS_ABILITY_1;
					case "ability2" -> dev.overcraft.net.HeroInputPayload.PRESS_ABILITY_2;
					case "ultimate" -> dev.overcraft.net.HeroInputPayload.PRESS_ULTIMATE;
					case "reload" -> dev.overcraft.net.HeroInputPayload.PRESS_RELOAD;
					default -> dev.overcraft.net.HeroInputPayload.PRESS_MELEE;
				};
				HeroManager.kit(HeroManager.state(bot).hero).press(bot, HeroManager.state(bot), flag);
				out.addProperty("ok", true);
			}
			case "seen" -> {
				// "bot seen <name>": what this client's animator holds of it (#96): the emote and how far it has run, Catnapper's aim
				ServerPlayer bot = BOTS.get(a[1]);
				var seen = dev.overcraft.client.model.HeroAnimator.existing(bot.getId());
				out.addProperty("animator", seen != null);
				if (seen != null) {
					out.addProperty("playing", seen.playing());
					var drawn = net.minecraft.client.Minecraft.getInstance().level == null ? null : net.minecraft.client.Minecraft.getInstance().level.getEntity(bot.getId());
					if (drawn instanceof LivingEntity living) {
						out.addProperty("wallSide", dev.overcraft.hero.OwMovement.wallRiding(living));
						out.addProperty("wallYaw", dev.overcraft.hero.OwMovement.wallRideYaw(living));
						out.addProperty("wallNormal", dev.overcraft.hero.OwMovement.wallRideNormal(living).toString());
						out.addProperty("code", living.getAttachedOrElse(HeroManager.LUCIO_WALL, 0));
					}
					float now = bot.level().getGameTime() / 20f;
					out.addProperty("emote", String.valueOf(seen.emote()));
					out.addProperty("emoteTime", seen.emoteTime(now));
					out.addProperty("sinceAim", seen.since("ult_aim", now));
					out.addProperty("sinceShield", seen.since("shield_raise", now));
				}
			}
			case "showstate" -> HeroManager.devNoShowState = a[1].equals("off");
			case "fx" -> {
				// "bot fx <name> <effect> <ticks>": the effect on the bot as a kit plays it, by the bot (a state a bot's kit can't reach here)
				ServerPlayer bot = BOTS.get(a[1]);
				dev.overcraft.hero.Effects.play((ServerLevel) bot.level(), a[2], bot.position(), net.minecraft.world.phys.Vec3.ZERO, bot, Integer.parseInt(a[3]), bot);
				out.addProperty("ok", true);
			}
			case "fxw" -> {
				// "bot fxw <name> <effect> <variant>": the same as one laid in the world at it, made by it (Cyberspace, the Disruptor's field)
				ServerPlayer bot = BOTS.get(a[1]);
				dev.overcraft.hero.Effects.play((ServerLevel) bot.level(), a[2], bot.position(), net.minecraft.world.phys.Vec3.ZERO, null, Integer.parseInt(a[3]), bot);
				out.addProperty("ok", true);
			}
			case "rays" -> out.addProperty("rays", dev.overcraft.client.effect.JunoFx.devRays());
			case "roll" -> {
				// "bot roll": the camera's roll now; "bot roll trace": and every frame's (aim yaw off the ride, wanted, roll) since the last call
				out.addProperty("roll", dev.overcraft.client.ClientHero.devRoll());
				if (a.length > 1) {
					com.google.gson.JsonArray list = new com.google.gson.JsonArray();
					dev.overcraft.client.ClientHero.devRollTrace().forEach(list::add);
					out.add("trace", list);
				}
			}
			case "healcut" -> {
				// "bot healcut <name>": the healing reduction on it now (#103)
				out.addProperty("cut", dev.overcraft.hero.Combat.healCut(BOTS.get(a[1])));
			}
			case "charge" -> {
				HeroManager.state(BOTS.get(a[1])).ultCharge = Double.parseDouble(a[2]);
				out.addProperty("ok", true);
			}
			case "emote" -> {
				dev.overcraft.comms.Comms.emote(BOTS.get(a[1]), a[2]);
				out.addProperty("ok", true);
			}
			case "move" -> {
				ServerPlayer bot = BOTS.get(a[1]);
				bot.snapTo(Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4]), bot.getYRot(), bot.getXRot());
				out.addProperty("ok", true);
			}
			case "remove" -> {
				ServerPlayer bot = BOTS.remove(a[1]);
				CHANNELS.remove(a[1]);
				if (bot != null) {
					server.getPlayerList().remove(bot);
				}
			}
			default -> out.addProperty("error", "add | secondary | read | set | asleep | antiheal | self | side | press | charge | emote | move | seen | showstate | healcut | fx | fxw | rays | roll | remove");
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
				PATHS.entrySet().removeIf(e -> {
					ServerPlayer b = BOTS.get(e.getKey());
					double[] p = e.getValue();
					if (b == null || p[3]-- <= 0) {
						if (b != null) {
							b.setKnownMovement(net.minecraft.world.phys.Vec3.ZERO);
						}
						return true;
					}
					b.snapTo(b.getX() + p[0], b.getY() + p[1], b.getZ() + p[2], b.getYRot(), b.getXRot());
					b.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
					b.setKnownMovement(new net.minecraft.world.phys.Vec3(p[0], p[1], p[2]));
					return false;
				});
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
