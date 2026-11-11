package dev.overcraft.client.dev;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.Overcraft;
import dev.overcraft.client.ClientHero;
import dev.overcraft.client.mixin.CreateWorldScreenAccessor;
import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.net.HeroSyncPayload;
import dev.overcraft.net.SelectHeroPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dev-only remote control (like Lethalcraft's DevServer), on when the game is started with -Dovercraft.dev=true:
 * localhost only, one command per line, one JSON line back. For play-testing from scripts (tools/dev.py): real key
 * and mouse input through Minecraft's own key mappings, looking around, commands, screenshots.
 *
 * Commands: state | newworld <name> [creative|survival] | openworld <folder> | screen | close | key <mapping> down|up|tap
 * | look <yaw> <pitch> | turn <dyaw> <dpitch> | cmd <command> | screenshot <file.png> | perspective first|back|front
 * | respawn | subtitles on|off
 * | hero <id>
 */
public final class DevServer {
	/** 25599, or -Dovercraft.devPort for a second client (gradlew runClient2: 25600). */
	private static long timingSince = System.nanoTime();
	public static final int PORT = Integer.getInteger("overcraft.devPort", 25599);

	private DevServer() {
	}

	/** Ticks recorded since "trace start" (dev only): one line per client tick. */
	private static final java.util.List<String> TRACE = new java.util.ArrayList<>();
	private static boolean tracing;
	private static boolean traceHooked;
	private static int traceTick;
	private static double traceX, traceZ;

	/** Lifeline's tow since "towtrace start" (dev only, #89): one line per server tick. */
	private static final java.util.List<String> TOW = new java.util.ArrayList<>();
	private static boolean towTracing;
	private static boolean towHooked;
	private static int towTick;

	private static boolean pressTraceListening;
	/** Sounds heard since the last "heard" command (dev only). */
	private static final java.util.List<String> HEARD = new java.util.ArrayList<>();
	private static boolean listening;

	public static void startIfEnabled() {
		if (!Boolean.getBoolean("overcraft.dev")) {
			return;
		}
		Thread thread = new Thread(DevServer::serve, "Overcraft DevServer");
		thread.setDaemon(true);
		thread.start();
	}

	private static void serve() {
		try (ServerSocket server = new ServerSocket(PORT, 4, InetAddress.getLoopbackAddress())) {
			Overcraft.LOG.info("Overcraft DevServer on 127.0.0.1:{}", PORT);
			while (true) {
				try (Socket socket = server.accept();
					 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
					 PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
					String line;
					while ((line = in.readLine()) != null) {
						out.println(run(line.trim()));
					}
				} catch (Exception e) {
					Overcraft.LOG.warn("DevServer connection: {}", e.toString());
				}
			}
		} catch (Exception e) {
			Overcraft.LOG.error("DevServer stopped", e);
		}
	}

	private static String run(String line) {
		Minecraft mc = Minecraft.getInstance();
		CompletableFuture<JsonObject> result = new CompletableFuture<>();
		mc.execute(() -> {
			try {
				result.complete(handle(mc, line.split(" ", 2)));
			} catch (Exception e) {
				JsonObject err = new JsonObject();
				err.addProperty("error", e.toString());
				result.complete(err);
			}
		});
		try {
			return result.get(30, TimeUnit.SECONDS).toString();
		} catch (Exception e) {
			return "{\"error\":\"" + e + "\"}";
		}
	}

	private static JsonObject handle(Minecraft mc, String[] parts) throws Exception {
		String cmd = parts[0];
		String arg = parts.length > 1 ? parts[1] : "";
		JsonObject out = new JsonObject();
		LocalPlayer player = mc.player;
		switch (cmd) {
			case "state" -> {
				Screen screen = mc.gui.screen();
				out.addProperty("screen", screen == null ? "" : screen.getClass().getSimpleName());
				out.addProperty("fps", mc.getFps());
				out.addProperty("fx", dev.overcraft.client.effect.Fx.count());
				out.addProperty("inWorld", player != null);
				if (player != null) {
					out.addProperty("x", player.getX());
					out.addProperty("y", player.getY());
					out.addProperty("z", player.getZ());
					out.addProperty("yaw", player.getYRot());
					out.addProperty("pitch", player.getXRot());
					out.addProperty("health", player.getHealth());
					out.addProperty("maxHealth", player.getMaxHealth());
					out.addProperty("onGround", player.onGround());
					out.addProperty("crouching", player.isCrouching());
					out.addProperty("sprinting", player.isSprinting());
					// the body box (dev.overcraft.hero.HeroBody): pose, size and eye on this client and on the integrated server
					out.addProperty("box", String.format(java.util.Locale.ROOT, "%s %.2fx%.2f eye %.2f", player.getPose(),
						player.getBbWidth(), player.getBbHeight(), player.getEyeHeight()));
					var server = mc.getSingleplayerServer();
					var serverPlayer = server == null ? null : server.getPlayerList().getPlayer(player.getUUID());
					if (serverPlayer != null) {
						out.addProperty("serverBox", String.format(java.util.Locale.ROOT, "%s %.2fx%.2f eye %.2f",
							serverPlayer.getPose(), serverPlayer.getBbWidth(), serverPlayer.getBbHeight(), serverPlayer.getEyeHeight()));
					}
					out.addProperty("slot", player.getInventory().getSelectedSlot());
					out.addProperty("heroMode", ClientHero.controls());
					HeroSyncPayload s = ClientHero.sync();
					out.addProperty("hero", s.heroId());
					out.addProperty("ult", s.ultCharge());
					// the hero's state bits (Jetpack Cat: the ult phase in the low 3, 8 towing, 16 Lifeline on)
					out.addProperty("mode", s.mode());
					net.minecraft.world.phys.Vec3 aim = dev.overcraft.client.effect.JetpackCatFx.aimSeen;
					out.addProperty("aimSeen", aim == null ? "" : aim.toString());
					out.addProperty("perkLevel", s.perkLevel());
					out.addProperty("perkProgress", s.perkProgress());
					out.addProperty("primaryAmmo", s.primaryAmmo());
					out.addProperty("primaryReload", s.primaryReload());
					out.addProperty("meter", s.meter());
					out.addProperty("secondaryAmmo", s.secondaryAmmo());
					out.addProperty("ability1Cooldown", s.ability1Cooldown());
					out.addProperty("ability2Cooldown", s.ability2Cooldown());
					out.addProperty("ability3Cooldown", s.ability3Cooldown());
					out.addProperty("vx", player.getDeltaMovement().x);
					out.addProperty("vy", player.getDeltaMovement().y);
					out.addProperty("vz", player.getDeltaMovement().z);
					out.addProperty("anim", HeroAnimator.of(player).playing());
					out.addProperty("clothLowest", dev.overcraft.client.model.ClothSim.devLowest);
					out.addProperty("emoteClip", HeroAnimator.of(player).emoteClip());
					out.addProperty("emoteClipAt", HeroAnimator.of(player).emoteAt());
					out.addProperty("emoteCamera", dev.overcraft.client.comms.EmoteCamera.active());
					out.addProperty("emoteFirstPersonFrames", dev.overcraft.client.comms.EmoteCamera.firstPersonFrames);
					out.addProperty("food", player.getFoodData().getFoodLevel());
					out.addProperty("saturation", player.getFoodData().getSaturationLevel());
					out.addProperty("onFire", dev.overcraft.client.ClientHero.sync().onFire());
					// where the first-person ofuda fingertip was last drawn, from the camera in the world's axes (the throw
					// puff pops there)
					out.addProperty("fingertip", dev.overcraft.client.model.HeroModelRenderer.viewFingertip().toString(java.text.NumberFormat.getInstance(java.util.Locale.ROOT)));
					// the first-person weapon's hp 0006 as last drawn, in her look's frame: right, up, ahead (m)
					org.joml.Vector3f muzzle = dev.overcraft.client.model.HeroModelRenderer.viewMuzzle();
					if (muzzle != null) {
						net.minecraft.world.phys.Vec3 look = player.getViewVector(1);
						net.minecraft.world.phys.Vec3 right = look.cross(new net.minecraft.world.phys.Vec3(0, 1, 0)).normalize();
						net.minecraft.world.phys.Vec3 up = right.cross(look).normalize();
						net.minecraft.world.phys.Vec3 m = new net.minecraft.world.phys.Vec3(muzzle.x, muzzle.y, muzzle.z);
						out.addProperty("muzzle", String.format(java.util.Locale.ROOT, "%.3f %.3f %.3f pitch %.1f",
							m.dot(right), m.dot(up), m.dot(look), player.getXRot()));
					}
					out.addProperty("anim1p", HeroAnimator.of(player).playingFirstPerson());
					out.addProperty("camera", mc.options.getCameraType().name() + " " + dev.overcraft.client.AbilityCamera.describe());
					String attached = player.getAttached(dev.overcraft.hero.HeroManager.HERO);
					out.addProperty("heroAttached", attached == null ? "" : attached);
				}
			}
			case "newworld" -> {
				String[] a = arg.split(" ");
				String name = a[0];
				// "newworld <name> [creative] [normal]": flat unless "normal" (real terrain, for survival play)
				boolean creative = java.util.Arrays.asList(a).contains("creative");
				boolean normal = java.util.Arrays.asList(a).contains("normal");
				CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(null));
				mc.execute(() -> {
					if (mc.gui.screen() instanceof CreateWorldScreen create) {
						WorldCreationUiState ui = create.getUiState();
						ui.setName(name);
						ui.setGameMode(creative ? WorldCreationUiState.SelectedGameMode.CREATIVE : WorldCreationUiState.SelectedGameMode.SURVIVAL);
						ui.setAllowCommands(true);
						if (!normal) {
							ui.getNormalPresetList().stream()
								.filter(e -> e.preset() != null && e.preset().is(WorldPresets.FLAT))
								.findFirst().ifPresent(ui::setWorldType);
						}
						((CreateWorldScreenAccessor) create).overcraft$onCreate();
					}
				});
				out.addProperty("ok", true);
			}
			case "players" -> {
				// dev only: the other players this client sees, with the yaws it renders them by
				com.google.gson.JsonArray list = new com.google.gson.JsonArray();
				for (net.minecraft.world.entity.player.Player other : mc.level.players()) {
					com.google.gson.JsonObject o = new com.google.gson.JsonObject();
					o.addProperty("name", other.getName().getString());
					o.addProperty("yRot", other.getYRot());
					o.addProperty("yHeadRot", other.getYHeadRot());
					o.addProperty("yBodyRot", other.yBodyRot);
					o.addProperty("x", other.getX());
					o.addProperty("z", other.getZ());
					list.add(o);
				}
				out.add("players", list);
			}
			case "timing" -> {
				// dev only: "timing reset", then "timing": ms per frame spent posing and emitting hero models since
				if (arg.startsWith("cull ")) {
					dev.overcraft.client.model.HeroModelRenderer.Timing.cull = arg.endsWith("on");
				} else if (arg.startsWith("stride ")) {
					dev.overcraft.client.model.HeroModelRenderer.Timing.stride = Integer.parseInt(arg.substring(7));
				} else if (arg.startsWith("glow ")) {
					dev.overcraft.client.model.HeroModelRenderer.Timing.glow = Integer.parseInt(arg.substring(5));
				} else if (arg.equals("reset")) {
					dev.overcraft.client.model.HeroModelRenderer.Timing.glowEmit = 0;
					dev.overcraft.client.model.HeroModelRenderer.Timing.pose = 0;
					dev.overcraft.client.model.HeroModelRenderer.Timing.emit = 0;
					dev.overcraft.client.model.HeroModelRenderer.Timing.shadow = 0;
					timingSince = System.nanoTime();
				} else {
					double seconds = (System.nanoTime() - timingSince) / 1e9;
					int fps = Math.max(1, mc.getFps());
					double frames = seconds * fps;
					out.addProperty("fps", fps);
					out.addProperty("poseMsPerFrame", dev.overcraft.client.model.HeroModelRenderer.Timing.pose / 1e6 / frames);
					out.addProperty("emitMsPerFrame", dev.overcraft.client.model.HeroModelRenderer.Timing.emit / 1e6 / frames);
					out.addProperty("glowMsPerFrame", dev.overcraft.client.model.HeroModelRenderer.Timing.glowEmit / 1e6 / frames);
					out.addProperty("shadowMsPerFrame", dev.overcraft.client.model.HeroModelRenderer.Timing.shadow / 1e6 / frames);
				}
			}
			case "connect" -> {
				// dev only: join a server or a LAN world ("connect localhost:25565"), as the multiplayer screen does
				net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(), mc,
					net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(arg),
					new net.minecraft.client.multiplayer.ServerData("Dev", arg, net.minecraft.client.multiplayer.ServerData.Type.LAN), false, null);
				out.addProperty("ok", true);
			}
			case "openworld" -> {
				mc.createWorldOpenFlows().openWorld(arg, () -> mc.gui.setScreen(null));
				out.addProperty("ok", true);
			}
			case "screen" -> out.addProperty("screen", mc.gui.screen() == null ? "" : mc.gui.screen().getClass().getName());
			case "close" -> {
				mc.gui.setScreen(null);
				out.addProperty("ok", true);
			}
			case "subtitles" -> {
				// shows which sound played in screenshots (on or off)
				mc.options.showSubtitles().set(!arg.equals("off"));
				out.addProperty("ok", true);
			}
			case "cut" -> {
				// dev only: "cut sleep|stun|hack <seconds>" on the local player, in the one-player world: what an enemy's dart, stun or EMP
				// does, without a second client
				String[] a = arg.split(" ");
				double seconds = a.length > 1 ? Double.parseDouble(a[1]) : 2;
				var server = mc.getSingleplayerServer();
				var target = server == null || player == null ? null : server.getPlayerList().getPlayer(player.getUUID());
				if (target != null) {
					server.execute(() -> {
						switch (a[0]) {
							case "sleep" -> dev.overcraft.hero.Sleep.put(target, seconds);
							case "stun" -> dev.overcraft.hero.Sleep.stun(target, seconds);
							case "hack" -> dev.overcraft.sombra.Hack.creature(target, seconds);
							default -> {
							}
						}
					});
				}
				out.addProperty("ok", target != null);
			}
			case "bot" -> {
				// dev only: a second player with no second client (DevBot)
				var server = mc.getSingleplayerServer();
				if (server == null) {
					out.addProperty("error", "no integrated server");
				} else {
					DevBot.handle(server, arg, out);
				}
			}
			case "presstrace" -> {
				// dev only (#126): "presstrace start", then a press, then "presstrace": from the press to the shot, its sounds, its
				// look and clip, each on one clock (hero.PressTrace). "presstrace energy <n>": her energy, as if she'd just earned it; "ammo <n>"
				String[] a = arg.split(" ");
				switch (a[0]) {
					case "start" -> {
						if (!pressTraceListening) {
							pressTraceListening = true;
							mc.getSoundManager().addListener((sound, events, range) -> {
								if (sound.getIdentifier().getPath().startsWith("sojourn.charged.") || sound.getIdentifier().getPath().startsWith("sojourn.railgun.reload")) {
									dev.overcraft.hero.PressTrace.mark("snd:" + sound.getIdentifier().getPath().substring("sojourn.".length()));
								}
							});
						}
						dev.overcraft.hero.PressTrace.start();
					}
					case "lag" -> DevLag.set(Long.parseLong(a[1]));
					// a modified client's press, as it would send it: "send <held> <pressed> <yaw> <pitch> <partial>"
					case "send" -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new dev.overcraft.net.HeroInputPayload(Integer.parseInt(a[1]), Integer.parseInt(a[2]), 0, 0,
						Float.parseFloat(a[3]), Float.parseFloat(a[4]), Float.parseFloat(a[5])));
					case "bind" -> dev.overcraft.client.controls.HeroControls.set(dev.overcraft.client.controls.HeroControls.Action.valueOf(a[1].toUpperCase(java.util.Locale.ROOT)), InputConstants.getKey(a[2]));
					case "flags" -> {
						var s = dev.overcraft.hero.HeroManager.state(mc.getSingleplayerServer().getPlayerList().getPlayer(player.getUUID()));
						out.addProperty("shown", s.sojournChargedShown);
						out.addProperty("buffer", s.sojournCharged);
						out.addProperty("lock", s.sojournChargedLock);
					}
					case "aimpress" -> {
						// dev only: from where the camera is this frame, aim at x y z, and the right button down, in one go: a perfect flick at speed
						float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
						net.minecraft.world.phys.Vec3 cam = player.getEyePosition(partial);
						net.minecraft.world.phys.Vec3 to = new net.minecraft.world.phys.Vec3(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])).subtract(cam);
						player.setYRot((float) Math.toDegrees(Math.atan2(-to.x, to.z)));
						player.setXRot((float) -Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z))));
						player.setYHeadRot(player.getYRot());
						dev.overcraft.hero.PressTrace.mark(String.format(java.util.Locale.ROOT, "press cam %.3f %.3f %.3f", cam.x, cam.y, cam.z));
						InputConstants.Key bound = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(dev.overcraft.client.controls.HeroControls.get(dev.overcraft.client.controls.HeroControls.Action.PRIMARY_FIRE));
						KeyMapping.set(bound, true);
						KeyMapping.click(bound);
						KeyMapping.set(bound, false);
					}
					case "fps" -> mc.options.framerateLimit().set(Integer.parseInt(a[1]));
					case "rtt" -> out.addProperty("rtt", dev.overcraft.client.ClientSojourn.measuredRoundTripMillis());
					case "art" -> {
						out.addProperty("art", dev.overcraft.client.effect.EffectArt.outstanding());
						out.addProperty("artBytes", dev.overcraft.client.effect.EffectArt.madeBytes());
					}
					case "sounding" -> {
						// the channels actually running, by id: SoundEngine lists a stopped sound for 20 ticks from its start, so "bot playing" can't tell
						var f = net.minecraft.client.sounds.SoundManager.class.getDeclaredField("soundEngine");
						f.setAccessible(true);
						var g = net.minecraft.client.sounds.SoundEngine.class.getDeclaredField("instanceToChannel");
						g.setAccessible(true);
						int n = 0;
						for (var e : new java.util.ArrayList<>(((java.util.Map<net.minecraft.client.resources.sounds.SoundInstance, net.minecraft.client.sounds.ChannelAccess.ChannelHandle>) g.get(f.get(mc.getSoundManager()))).entrySet())) {
							if (e.getKey().getIdentifier().getPath().contains(a[1]) && !e.getValue().isStopped()) {
								n++;
							}
						}
						out.addProperty("sounding", n);
					}
					case "frames" -> dev.overcraft.hero.PressTrace.allFrames(a[1].equals("on"));
					case "sample" -> {
						// the client's thread sampled for this many ms from now, into the log (dev only)
						Thread main = Thread.currentThread();
						int ms = Integer.parseInt(a[1]);
						Thread sampler = new Thread(() -> {
							try {
								dev.overcraft.Overcraft.LOG.info("SAMPLE\n{}", DevLag.sample(main, ms));
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}, "Overcraft sampler");
						sampler.setDaemon(true);
						sampler.start();
					}
					case "energy" -> {
						var server = mc.getSingleplayerServer();
						double energy = Double.parseDouble(a[1]);
						java.util.UUID id = player.getUUID();
						String named = a.length > 2 ? a[2] : null;
						server.execute(() -> {
							var who = named != null ? server.getPlayerList().getPlayerByName(named) : server.getPlayerList().getPlayer(id);
							var s = dev.overcraft.hero.HeroManager.state(who);
							s.sojournEnergy = energy;
							s.sojournGainAt = who.level().getGameTime();
						});
					}
					case "ammo" -> {
						var server = mc.getSingleplayerServer();
						int ammo = Integer.parseInt(a[1]);
						java.util.UUID id = player.getUUID();
						server.execute(() -> dev.overcraft.hero.HeroManager.state(server.getPlayerList().getPlayer(id)).primaryAmmo = ammo);
					}
					default -> out.addProperty("trace", dev.overcraft.hero.PressTrace.dump());
				}
			}
			case "respawn" -> {
				// what the death screen's Respawn button does
				if (player != null) {
					player.respawn();
				}
				out.addProperty("ok", player != null);
			}
			case "key" -> {
				String[] a = arg.split(" ");
				KeyMapping key = mapping(mc, a[0]);
				InputConstants.Key bound = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(key);
				switch (a.length > 1 ? a[1] : "tap") {
					case "down" -> {
						dev.overcraft.hero.PressTrace.mark("press");
						KeyMapping.set(bound, true);
						KeyMapping.click(bound);
					}
					case "up" -> KeyMapping.set(bound, false);
					default -> {
						KeyMapping.set(bound, true);
						KeyMapping.click(bound);
						mc.execute(() -> mc.execute(() -> KeyMapping.set(bound, false)));
					}
				}
				out.addProperty("key", bound.getName());
			}
			case "look", "turn" -> {
				String[] a = arg.split(" ");
				float yaw = Float.parseFloat(a[0]);
				float pitch = Float.parseFloat(a[1]);
				if (cmd.equals("turn")) {
					yaw += player.getYRot();
					pitch += player.getXRot();
				}
				player.setYRot(yaw);
				player.setXRot(pitch);
				player.setYHeadRot(yaw);
				out.addProperty("ok", true);
			}
			case "emote" -> {
				// dev only: "emote <id>": plays one of the hero's emotes as the wheel does (any of them, equipped or not)
				net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new dev.overcraft.net.EmotePayload(arg.trim()));
				out.addProperty("ok", true);
			}
			case "clothfloor" -> {
				// dev only: "clothfloor on|off": the cloth's floor constraint (off shows the coat under the floor it fixed)
				dev.overcraft.client.model.ClothSim.devFloor = !arg.trim().equals("off");
				out.addProperty("clothFloor", dev.overcraft.client.model.ClothSim.devFloor);
			}
			case "reload" -> {
				// dev only: reloads the resource packs (F3+T): a regenerated emote data file or pack model without a restart
				mc.reloadResourcePacks();
				out.addProperty("ok", true);
			}
			case "fxslow" -> {
				// dev only: "fxslow <share>": effects play at that share of real time (0.1: ten times slower), to look at
				// effects shorter than a screenshot takes; "fxslow 1" for real time
				dev.overcraft.client.effect.Fx.devSlow = Float.parseFloat(arg.trim());
				out.addProperty("fxslow", dev.overcraft.client.effect.Fx.devSlow);
			}
			case "emoteat" -> {
				// dev only: "emoteat <seconds>": holds every emote at that time into it, to look at exact frames (the
				// emote data's prop windows); "emoteat off" lets them play again
				String a = arg.trim();
				dev.overcraft.client.model.HeroAnimator.devEmoteAt = a.equals("off") ? -1 : Float.parseFloat(a);
				out.addProperty("emoteAt", dev.overcraft.client.model.HeroAnimator.devEmoteAt);
			}
			case "chat" -> {
				// a chat message as typed in the chat box ("chat hello there"), signed and sent as Minecraft sends it
				player.connection.sendChat(arg);
				out.addProperty("ok", true);
			}
			case "cmd" -> {
				player.connection.sendCommand(arg.startsWith("/") ? arg.substring(1) : arg);
				out.addProperty("ok", true);
			}
			case "screenshot" -> {
				File dir = mc.gameDirectory;
				Screenshot.grab(dir, arg.isEmpty() ? null : arg, mc.gameRenderer.mainRenderTarget(), 1, message -> { });
				out.addProperty("file", new File(new File(dir, "screenshots"), arg).getAbsolutePath());
			}
			case "perspective" -> {
				mc.options.setCameraType(switch (arg) {
					case "back" -> CameraType.THIRD_PERSON_BACK;
					case "front" -> CameraType.THIRD_PERSON_FRONT;
					default -> CameraType.FIRST_PERSON;
				});
				out.addProperty("ok", true);
			}
			case "devwidget" -> {
				// dev only, for shots of widgets one client can't reach (#85): "lockout": Sombra's hack lockout runs, with no enemy
				// deployable about; "barrier1": Brigitte's barrier down to 1, so the next real hit breaks it
				var server = mc.getSingleplayerServer();
				java.util.UUID id = player.getUUID();
				server.execute(() -> {
					var serverPlayer = server.getPlayerList().getPlayer(id);
					var state = serverPlayer == null ? null : dev.overcraft.hero.HeroManager.state(serverPlayer);
					if (state != null && arg.equals("lockout")) {
						state.hackLockoutUntil = serverPlayer.level().getGameTime() + 120;
					} else if (state != null && arg.equals("barrier1")) {
						state.barrierHealth = 1;
					}
				});
				out.addProperty("ok", true);
			}
			case "devtowed" -> {
				dev.overcraft.client.effect.JetpackCatFx.devTowed = arg.equals("on");
				out.addProperty("ok", true);
			}
			case "hero" -> {
				ClientPlayNetworking.send(new SelectHeroPayload(arg));
				out.addProperty("ok", true);
			}
			case "equip" -> {
				// dev only: "equip <slot 0-3> <emote id>": what the loadout screen does, equips an emote on the wheel's slot
				// (up, right, down, left) for the current hero, so the wheel can play each in turn
				String[] a = arg.trim().split(" ");
				dev.overcraft.client.comms.Loadout.setEmote(dev.overcraft.client.ClientHero.hero(), Integer.parseInt(a[0]), a[1]);
				out.addProperty("emotes", String.valueOf(dev.overcraft.client.comms.Loadout.emotes(dev.overcraft.client.ClientHero.hero())));
			}
			case "press" -> {
				// a real key press through Minecraft's keyboard handler (Esc pauses, screens get keyPressed): the harness's
				// way to type when the desktop can't take input. "press key.keyboard.escape"
				// "press <key> down" / "press <key> up" holds it (a movement key), through the same handler
				String[] a = arg.trim().split(" ");
				InputConstants.Key key = InputConstants.getKey(a[0]);
				long handle = mc.getWindow().handle();
				net.minecraft.client.input.KeyEvent event = new net.minecraft.client.input.KeyEvent(key.getValue(), 0, 0);
				if (a.length > 1) {
					// (repeat: what the OS sends for a held key, through the same handler)
					mc.keyboardHandler.keyPress(handle, a[1].equals("up") ? 0 : a[1].equals("repeat") ? 2 : 1, event);
				} else {
					mc.keyboardHandler.keyPress(handle, 1, event);
					mc.keyboardHandler.keyPress(handle, 0, event);
				}
				out.addProperty("key", key.getName());
			}
			case "screenclick" -> {
				// a click on the open screen at GUI coordinates: "screenclick 120 80 [button]"
				String[] a = arg.split(" ");
				int button = a.length > 2 ? Integer.parseInt(a[2]) : InputConstants.MOUSE_BUTTON_LEFT;
				var event = new net.minecraft.client.input.MouseButtonEvent(Double.parseDouble(a[0]), Double.parseDouble(a[1]),
					new net.minecraft.client.input.MouseButtonInfo(button, 0));
				if (mc.gui.screen() != null) {
					mc.gui.screen().mouseClicked(event, false);
					if (mc.gui.screen() != null) {
						mc.gui.screen().mouseReleased(event);
					}
				}
				out.addProperty("screen", mc.gui.screen() == null ? "" : mc.gui.screen().getClass().getSimpleName());
			}
			case "swing" -> {
				// dev only: "swing ax ay az degrees [hold]": Minecraft mode's arm swing with her arms (hold: stop it there)
				String[] a = arg.trim().split(" ");
				for (int i = 0; i < a.length && i < 5; i++) {
					dev.overcraft.client.model.HeroAnimator.SWING[i] = Float.parseFloat(a[i]);
				}
				out.addProperty("swing", java.util.Arrays.toString(dev.overcraft.client.model.HeroAnimator.SWING));
			}
			case "props" -> {
				// dev only: "props [bone ...]": each emote prop placed now (its model, place and axes in her model's space)
				// and these bones of her posed emote skeleton, to check against the data (tools/emote_bones.py)
				dev.overcraft.client.model.HeroAnimator animator = dev.overcraft.client.model.HeroAnimator.of(player);
				dev.overcraft.hero.Hero hero = dev.overcraft.client.ClientHero.hero();
				var loaded = hero == null ? null : dev.overcraft.client.model.HeroModels.get(hero);
				com.google.gson.JsonArray list = new com.google.gson.JsonArray();
				if (loaded != null) {
					var held = dev.overcraft.client.model.EmoteProps.held(hero.id, animator, loaded.model());
					for (var placed : held == null ? java.util.List.<dev.overcraft.client.model.EmoteProps.Placed>of() : held.props()) {
						org.joml.Matrix4f m = placed.at();
						list.add(String.format(java.util.Locale.ROOT, "%s at %s +X %s +Y %s +Z %s",
							placed.loaded().model().nodeNames[placed.loaded().model().nodeNames.length - 1],
							m.getTranslation(new org.joml.Vector3f()), m.getColumn(0, new org.joml.Vector3f()),
							m.getColumn(1, new org.joml.Vector3f()), m.getColumn(2, new org.joml.Vector3f())));
					}
					var pose = animator.emotePose();
					for (String bone : arg.trim().isEmpty() ? new String[0] : arg.trim().split(" ")) {
						int node = loaded.model().node(bone);
						if (pose != null && node >= 0) {
							org.joml.Matrix4f m = dev.overcraft.client.model.Skeleton.world(loaded.model(), pose, node);
							list.add(String.format(java.util.Locale.ROOT, "%s at %s +Y %s +Z %s", bone, m.getTranslation(new org.joml.Vector3f()),
								m.getColumn(1, new org.joml.Vector3f()), m.getColumn(2, new org.joml.Vector3f())));
						}
					}
				}
				out.add("props", list);
				out.addProperty("clip", animator.emoteClip());
				out.addProperty("at", animator.emoteAt());
			}
			case "weapon" -> {
				// dev only: the hero's own weapon and extras placed now (each model's last node, place in her model's space),
				// and whether the emote has the weapon out
				dev.overcraft.client.model.HeroAnimator animator = dev.overcraft.client.model.HeroAnimator.of(player);
				dev.overcraft.hero.Hero hero = dev.overcraft.client.ClientHero.hero();
				var loaded = hero == null ? null : dev.overcraft.client.model.HeroModels.get(hero);
				com.google.gson.JsonArray list = new com.google.gson.JsonArray();
				if (loaded != null) {
					float now = player.level().getGameTime() / 20f;
					var weapon = dev.overcraft.client.model.HeroWeapon.place(hero, animator, loaded.model(), false, now);
					java.util.List<dev.overcraft.client.model.EmoteProps.Placed> all = new java.util.ArrayList<>();
					if (weapon != null) {
						all.add(weapon);
					}
					all.addAll(dev.overcraft.client.model.HeroWeapon.extras(hero, animator, loaded.model(), false, now));
					for (var placed : all) {
						list.add(placed.loaded().model().nodeNames[placed.loaded().model().nodeNames.length - 1] + " at "
							+ placed.at().getTranslation(new org.joml.Vector3f()));
					}
					out.addProperty("out", !animator.emoting() || dev.overcraft.client.model.HeroWeapon.inEmote(hero, animator));
				}
				out.add("placed", list);
			}
			case "turnclips" -> {
				// dev only: "turnclips [hero]": the clips of a hero's model held at their first frame as turn-in-place stands
				dev.overcraft.hero.Hero hero = arg.isBlank() ? dev.overcraft.client.ClientHero.hero() : dev.overcraft.hero.Hero.byId(arg.trim());
				var loaded = hero == null ? null : dev.overcraft.client.model.HeroModels.get(hero);
				com.google.gson.JsonArray list = new com.google.gson.JsonArray();
				if (loaded != null) {
					loaded.model().clips.keySet().stream().sorted()
						.filter(n -> dev.overcraft.client.model.HeroAnimator.turnsInPlace(loaded.model(), n)).forEach(list::add);
				}
				out.add("turns", list);
			}
			case "aimpose" -> {
				// dev only: "aimpose <pitch> [turn]" / "aimpose off": the model aims this pitch, turned this far from the camera, to judge the
				// aim sheets from any side with a level camera (the third-person camera otherwise swings with the aim)
				String[] a = arg.trim().split(" ");
				dev.overcraft.client.model.HeroModelRenderer.DEV_AIM = a[0].equals("off") ? null
					: new float[] {Float.parseFloat(a[0]), a.length > 1 ? Float.parseFloat(a[1]) : 0};
				out.addProperty("ok", true);
			}
			case "aimcheck" -> {
				// dev only: the gun as posed now: the barrel's pitch (+ down) from its origin to its muzzle (hp 0006), and how far the origin is from the hand, in her model's space
				dev.overcraft.hero.Hero hero = dev.overcraft.client.ClientHero.hero();
				var loaded = hero == null ? null : dev.overcraft.client.model.HeroModels.get(hero);
				dev.overcraft.client.model.HeroAnimator animator = dev.overcraft.client.model.HeroAnimator.of(player);
				float now = player.level().getGameTime() / 20f;
				var placed = loaded == null ? null : dev.overcraft.client.model.HeroWeapon.place(hero, animator, loaded.model(), false, now);
				org.joml.Vector3f muzzle = loaded == null ? null : dev.overcraft.client.model.HeroWeapon.muzzle(hero, animator, loaded.model(), false, now);
				if (placed != null && muzzle != null) {
					org.joml.Vector3f at = placed.at().getTranslation(new org.joml.Vector3f());
					org.joml.Vector3f d = new org.joml.Vector3f(muzzle).sub(at);
					out.addProperty("barrelPitch", Math.toDegrees(Math.atan2(-d.y, Math.hypot(d.x, d.z))));
					out.addProperty("muzzleY", muzzle.y);
				}
				if (animator.aimSheetInUse() != null) {
					out.addProperty("sheet", animator.aimSheetInUse().describe());
				}
			}
			case "aimsheets" -> {
				// dev only: "aimsheets [hero]": the additive clips of a hero's model that measure as aim sheets, and their pitch lines
				dev.overcraft.hero.Hero hero = arg.isBlank() ? dev.overcraft.client.ClientHero.hero() : dev.overcraft.hero.Hero.byId(arg.trim());
				var loaded = hero == null ? null : dev.overcraft.client.model.HeroModels.get(hero);
				com.google.gson.JsonObject list = new com.google.gson.JsonObject();
				if (loaded != null) {
					loaded.model().clips.keySet().stream().sorted().filter(n -> n.startsWith("add_")).forEach(n -> {
						var sheet = dev.overcraft.client.model.AimSheet.of(loaded.model(), loaded.model().clips.get(n));
						if (sheet != null) {
							list.addProperty(n, sheet.describe());
						}
					});
				}
				out.add("sheets", list);
			}
			case "viewbones" -> {
				// dev only: the first-person hand bones in view space (x right, y up, -z ahead), Minecraft mode
				com.google.gson.JsonObject bones = new com.google.gson.JsonObject();
				dev.overcraft.client.model.HeroModelRenderer.VIEW_BONES.forEach((n, v) -> bones.addProperty(n,
					String.format(java.util.Locale.ROOT, "%.3f %.3f %.3f", v.x, v.y, v.z)));
				out.add("bones", bones);
			}
			case "held" -> {
				// dev only: "held kunai|ofuda rx ry rz tx ty tz lift": how Minecraft mode's held items sit in her fists
				String[] a = arg.trim().split(" ");
				float[] held = a[0].equals("ofuda") ? dev.overcraft.client.model.HeroModelRenderer.HELD_OFUDA
					: dev.overcraft.client.model.HeroModelRenderer.HELD_KUNAI;
				for (int i = 1; i < a.length && i <= 7; i++) {
					held[i - 1] = Float.parseFloat(a[i]);
				}
				out.addProperty("held", java.util.Arrays.toString(held));
			}
			case "selectframe" -> {
				// dev only: "selectframe [dva] <framing> <drop>": Sigma's (or D.Va's) size and drop on the hero select
				String[] a = arg.trim().split(" ");
				float[] frame = dev.overcraft.client.HeroSelectScreen.SIGMA_FRAME;
				if (a.length > 0 && a[0].equals("dva")) {
					frame = dev.overcraft.client.HeroSelectScreen.DVA_FRAME;
					a = java.util.Arrays.copyOfRange(a, 1, a.length);
				}
				for (int i = 0; i < a.length && i < 2 && !a[i].isEmpty(); i++) {
					frame[i] = Float.parseFloat(a[i]);
				}
				out.addProperty("frame", java.util.Arrays.toString(frame));
			}
			case "heldmax" -> {
				// "heldmax [kunaiFlat kunaiSolid ofudaFlat ofudaSolid]"
				String[] a = arg.trim().split(" ");
				for (int i = 0; i < a.length && i < 4 && !a[i].isEmpty(); i++) {
					dev.overcraft.client.model.HeroModelRenderer.HELD_MAX[i] = Float.parseFloat(a[i]);
				}
				out.addProperty("sizes", dev.overcraft.client.model.HeroModelRenderer.heldSizes.toString());
			}
			case "heldscale" -> {
				// "heldscale kunai ofuda"
				String[] a = arg.trim().split(" ");
				for (int i = 0; i < a.length && i < 2 && !a[i].isEmpty(); i++) {
					dev.overcraft.client.model.HeroModelRenderer.HELD_SCALE[i] = Float.parseFloat(a[i]);
				}
				out.addProperty("scale", java.util.Arrays.toString(dev.overcraft.client.model.HeroModelRenderer.HELD_SCALE));
			}
			case "shadows" -> {
				// "shadows on|off": Minecraft's entity shadows option (hers among them), to compare frame times
				mc.options.entityShadows().set(!arg.trim().equals("off"));
				out.addProperty("shadows", mc.options.entityShadows().get());
			}
			case "emotef5" -> {
				// "emotef5 on|off": whether F5 is held out of first person during an emote (off shows what it fixes)
				dev.overcraft.client.comms.EmoteCamera.holdF5 = !arg.trim().equals("off");
				out.addProperty("holdF5", dev.overcraft.client.comms.EmoteCamera.holdF5);
			}
			case "scroll" -> {
				// the mouse wheel over the open screen at GUI coordinates, as MouseHandler hands it on: "scroll 120 80 -3"
				String[] a = arg.split(" ");
				boolean done = mc.gui.screen() != null && mc.gui.screen().mouseScrolled(Double.parseDouble(a[0]),
					Double.parseDouble(a[1]), 0, Double.parseDouble(a[2]));
				out.addProperty("ok", done);
			}
			case "hover" -> {
				// puts the game's cursor at GUI coordinates, for hover states, without moving the real mouse: "hover 120 80"
				String[] a = arg.split(" ");
				var window = mc.getWindow();
				mc.mouseHandler.onMove(window.handle(), Double.parseDouble(a[0]) * window.getScreenWidth() / window.getGuiScaledWidth(),
					Double.parseDouble(a[1]) * window.getScreenHeight() / window.getGuiScaledHeight(), 0, 0);
				out.addProperty("ok", true);
			}
			case "fx" -> {
				// dev only: plays an effect 3 m in front of the camera: "fx suzu_burst 40", "fx climb_dust", "fx rush_gate 100
				// enemy" (as an enemy hero's)
				String[] a = arg.split(" ");
				net.minecraft.world.phys.Vec3 at = player.getEyePosition().add(player.getViewVector(1).scale(3));
				int variant = a.length > 1 ? Integer.parseInt(a[1]) : 0;
				if (a[0].equals("climb_dust")) {
					dev.overcraft.client.effect.KirikoFx.climbDust(at);
				} else {
					dev.overcraft.client.effect.KirikoFx.play(new dev.overcraft.net.EffectPayload(a[0], at.x, at.y, at.z, 0, 1, 0, -1, variant,
					a.length > 2 && a[2].equals("enemy")));
				}
				out.addProperty("ok", true);
			}
			case "fxnear" -> {
				// dev only: the live particles within r of a point: "fxnear 5.1 -59.9 0.5 3", or of the player: "fxnear 3"
				String[] a = arg.split(" ");
				net.minecraft.world.phys.Vec3 at = a.length >= 3 ? new net.minecraft.world.phys.Vec3(Double.parseDouble(a[0]),
					Double.parseDouble(a[1]), Double.parseDouble(a[2])) : player.position();
				double r = Double.parseDouble(a[a.length >= 4 ? 3 : 0]);
				com.google.gson.JsonArray list = new com.google.gson.JsonArray();
				dev.overcraft.client.effect.Fx.near(at, r).forEach(list::add);
				out.add("fx", list);
			}
			case "select" -> {
				// dev only: the hero select screen, as if joined ("select") or at the bed ("select bed")
				mc.gui.setScreen(new dev.overcraft.client.HeroSelectScreen(!arg.equals("bed")));
				out.addProperty("ok", true);
			}
			case "heard" -> {
				// dev only: the overcraft sounds the client played since the last "heard" (the first call starts listening):
				// event -> the file it picked, which shows which voice line played
				if (!listening) {
					listening = true;
					mc.getSoundManager().addListener((sound, events, range) -> {
						if (sound.getIdentifier().getNamespace().equals("overcraft")) {
							synchronized (HEARD) {
								HEARD.add(sound.getIdentifier().getPath() + " -> " + sound.getSound().getLocation().getPath() + " ("
									+ sound.getClass().getSimpleName() + (sound.isRelative() ? ", in the head"
									: String.format(java.util.Locale.ROOT, ", at %.1f %.1f %.1f", sound.getX(), sound.getY(), sound.getZ())) + ")");
							}
						}
					});
				}
				synchronized (HEARD) {
					out.addProperty("heard", String.join(" | ", HEARD));
					HEARD.clear();
				}
			}
			case "trace" -> {
				// dev only: "trace start", then "trace": per client tick, the local player's horizontal speed (from the
				// position change, m/s), vertical speed (m/s), height, on ground and the movement keys held
				if (!traceHooked) {
					traceHooked = true;
					net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
						LocalPlayer p = client.player;
						if (!tracing || p == null) {
							return;
						}
						double dx = (p.getX() - traceX) * 20;
						double dz = (p.getZ() - traceZ) * 20;
						traceX = p.getX();
						traceZ = p.getZ();
						var o = client.options;
						String keys = (o.keyUp.isDown() ? "W" : "") + (o.keyLeft.isDown() ? "A" : "") + (o.keyDown.isDown() ? "S" : "")
							+ (o.keyRight.isDown() ? "D" : "") + (o.keyJump.isDown() ? "J" : "") + (o.keyShift.isDown() ? "C" : "");
						synchronized (TRACE) {
							// then the synced ammo, reload share and meter (Sojourn's energy / 100)
							HeroSyncPayload hs = ClientHero.sync();
							// (and the cooldowns, the second ammo, the ult flag: Sojourn's step 6-7 timings)
							TRACE.add(String.format(java.util.Locale.ROOT, "%d %.2f %.2f %.2f %.3f %s %s %.2f %.2f a%d r%.2f m%.4f c1%.2f c2%.2f s%d u%s",
								traceTick++, Math.sqrt(dx * dx + dz * dz), dx, p.getDeltaMovement().y * 20, p.getY(), p.onGround() ? "G" : "-",
								keys.isEmpty() ? "." : keys, dz, p.getYRot(), hs.primaryAmmo(), hs.primaryReload(), hs.meter(),
								hs.ability1Cooldown(), hs.ability2Cooldown(), hs.secondaryAmmo(), hs.ultActive() ? "1" : "0")
								// Sigma: spheres seen for the first time this tick, his barrier's place, state and health share
								+ sigmaTrace(client));
						}
					});
				}
				if (arg.equals("start")) {
					synchronized (TRACE) {
						TRACE.clear();
					}
					traceTick = 0;
					traceX = player.getX();
					traceZ = player.getZ();
					tracing = true;
					out.addProperty("ok", true);
				} else {
					tracing = false;
					synchronized (TRACE) {
						out.addProperty("trace", String.join("\n", TRACE));
						TRACE.clear();
					}
				}
			}
			case "devtow" -> {
				// dev only (#89): "devtow <entity type>" hangs the local player on a rope to the nearest of that type (JetpackCatFx.devTow);
				// "devtow off". The player's own side of a tow, for want of a second client
				net.minecraft.world.entity.Entity anchor = null;
				if (!arg.equals("off")) {
					double best = Double.MAX_VALUE;
					for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
						if (e != player && net.minecraft.world.entity.EntityType.getKey(e.getType()).getPath().equals(arg) && e.distanceToSqr(player) < best) {
							best = e.distanceToSqr(player);
							anchor = e;
						}
					}
				}
				dev.overcraft.client.effect.JetpackCatFx.devTow(player, anchor == null ? -1 : anchor.getId());
				out.addProperty("anchor", anchor == null ? -1 : anchor.getId());
			}
			case "towframes" -> {
				// dev only (#89): "towframes start", then "towframes": each rendered frame, as drawn: time (s), her x y z, the towed one's x y z
				if (arg.equals("start")) {
					dev.overcraft.client.effect.JetpackCatFx.devFrames(true);
					out.addProperty("ok", true);
				} else {
					out.addProperty("trace", dev.overcraft.client.effect.JetpackCatFx.devFrames(false));
				}
			}
			case "towtrace" -> {
				// dev only (#89): "towtrace start", then "towtrace": per server tick, the Jetpack Cat and whoever her Lifeline tows:
				// tick, her x y z, their x y z, their on-ground (m). The server's view, so it reads a mob or a player the same
				if (!towHooked) {
					towHooked = true;
					net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
						if (!towTracing) {
							return;
						}
						for (net.minecraft.server.level.ServerPlayer cat : server.getPlayerList().getPlayers()) {
							dev.overcraft.hero.HeroState s = dev.overcraft.hero.HeroManager.state(cat);
							if (s == null || s.hero != dev.overcraft.hero.Hero.JETPACK_CAT) {
								continue;
							}
							net.minecraft.world.entity.Entity a = s.lifelineTarget == null ? null : cat.level().getEntity(s.lifelineTarget);
							synchronized (TOW) {
								TOW.add(a == null ? String.format(java.util.Locale.ROOT, "%d %.3f %.3f %.3f - - - -", towTick++, cat.getX(), cat.getY(), cat.getZ())
									: String.format(java.util.Locale.ROOT, "%d %.3f %.3f %.3f %.3f %.3f %.3f %s", towTick++, cat.getX(), cat.getY(), cat.getZ(),
										a.getX(), a.getY(), a.getZ(), a.onGround() ? "G" : "-"));
							}
						}
					});
				}
				if (arg.equals("start")) {
					synchronized (TOW) {
						TOW.clear();
					}
					towTick = 0;
					towTracing = true;
					out.addProperty("ok", true);
				} else {
					towTracing = false;
					synchronized (TOW) {
						out.addProperty("trace", String.join("\n", TOW));
						TOW.clear();
					}
				}
			}
			case "heromode" -> {
				// dev only: hero mode on or off, as G does
				ClientHero.setHeroMode(!arg.equals("off"));
				out.addProperty("ok", true);
			}
			case "playing" -> {
				// dev only: the overcraft sounds playing now (the sound engine's live instances, read by reflection), so a loop's
				// stop can be checked as well as its start
				java.util.Set<String> now = new java.util.TreeSet<>();
				try {
					for (java.lang.reflect.Field f : mc.getSoundManager().getClass().getDeclaredFields()) {
						if (!f.getType().getSimpleName().equals("SoundEngine")) {
							continue;
						}
						f.setAccessible(true);
						Object engine = f.get(mc.getSoundManager());
						for (java.lang.reflect.Field g : engine.getClass().getDeclaredFields()) {
							if (!java.util.Map.class.isAssignableFrom(g.getType())) {
								continue;
							}
							g.setAccessible(true);
							// the instance -> channel map (a stopped channel stays in it up to 1 s after its start: asked whether stopped)
							for (var e : new java.util.ArrayList<>(((java.util.Map<?, ?>) g.get(engine)).entrySet())) {
								if (!(e.getKey() instanceof net.minecraft.client.resources.sounds.SoundInstance sound)
									|| !sound.getIdentifier().getNamespace().equals("overcraft") || e.getValue() == null) {
									continue;
								}
								java.lang.reflect.Method stopped;
								try {
									stopped = e.getValue().getClass().getMethod("isStopped");
								} catch (NoSuchMethodException none) {
									continue;
								}
								if (!(Boolean) stopped.invoke(e.getValue())) {
									now.add(sound.getIdentifier().getPath());
								}
							}
						}
					}
				} catch (ReflectiveOperationException | RuntimeException e) {
					out.addProperty("error", e.toString());
				}
				out.addProperty("playing", String.join(" ", now));
			}
			case "soundinfo" -> {
				// dev only: which files a sound event resolves to (the local pack's recordings, or the jar's stand-ins)
				var event = mc.getSoundManager().getSoundEvent(net.minecraft.resources.Identifier.parse(arg));
				java.util.Set<String> files = new java.util.TreeSet<>();
				if (event != null) {
					net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
					for (int i = 0; i < 64; i++) {
						files.add(event.getSound(random).getLocation().toString());
					}
				}
				out.addProperty("files", String.join(", ", files));
			}
			case "mouse" -> {
				// dev only: mouse movement in pixels, handed to Minecraft's own MouseHandler.turnPlayer as if the window had
				// it (Minecraft only reads the mouse while its window has it, which the harness's window often doesn't): the
				// communication wheel's pointer while it's open, the emote camera while emoting, else the view
				String[] a = arg.split(" ");
				var mouse = (dev.overcraft.client.mixin.MouseHandlerInvoker) mc.mouseHandler;
				mouse.overcraft$setAccumulatedDX(Double.parseDouble(a[0]));
				mouse.overcraft$setAccumulatedDY(Double.parseDouble(a[1]));
				mouse.overcraft$turnPlayer(0);
				mouse.overcraft$setAccumulatedDX(0);
				mouse.overcraft$setAccumulatedDY(0);
				out.addProperty("ok", true);
			}
			case "focus" -> {
				// dev only: brings the game's window back from the taskbar and in front, for play with the real mouse and keys
				long handle = mc.getWindow().handle();
				org.lwjgl.sdl.SDLVideo.SDL_RestoreWindow(handle);
				org.lwjgl.sdl.SDLVideo.SDL_ShowWindow(handle);
				org.lwjgl.sdl.SDLVideo.SDL_RaiseWindow(handle);
				out.addProperty("ok", true);
			}
			case "window" -> {
				// dev only: a window size for screenshots (a 1920x1080 window matches the HUD reference)
				String[] a = arg.split(" ");
				mc.getWindow().setWindowed(Integer.parseInt(a[0]), Integer.parseInt(a[1]));
				out.addProperty("ok", true);
			}
			case "tutorial" -> {
				mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
				out.addProperty("ok", true);
			}
			case "ult" -> {
				// dev only: sets the ult charge (percent) on the integrated server, to see the meter's states
				double percent = Double.parseDouble(arg);
				double cost = dev.overcraft.client.ClientHero.sync().ultCost();
				java.util.UUID id = player.getUUID();
				var server = mc.getSingleplayerServer();
				server.execute(() -> {
					var serverPlayer = server.getPlayerList().getPlayer(id);
					var state = serverPlayer == null ? null : dev.overcraft.hero.HeroManager.state(serverPlayer);
					if (state != null) {
						state.ultCharge = cost * percent / 100;
					}
				});
				out.addProperty("ok", server != null);
			}
			default -> out.addProperty("error", "unknown command " + cmd);
		}
		return out;
	}

	private static KeyMapping mapping(Minecraft mc, String name) {
		// hero mode's keys by their action ("primary_fire", "ability_2"), then Minecraft's
		for (dev.overcraft.client.controls.HeroControls.Action action : dev.overcraft.client.controls.HeroControls.Action.values()) {
			if (action.id().equals(name)) {
				return action.mapping;
			}
		}
		for (KeyMapping key : mc.options.keyMappings) {
			if (key.getName().equals(name) || key.getName().equals("key." + name) || key.getName().equals("key.overcraft." + name)) {
				return key;
			}
		}
		throw new IllegalArgumentException("no key mapping " + name);
	}

	/** Dev only (trace): Sigma's new Hyperspheres this tick (" h2"), his barrier (" b x y z state share"). */
	private static final java.util.Set<Integer> SPHERES_SEEN = new java.util.HashSet<>();

	private static String sigmaTrace(Minecraft client) {
		StringBuilder out = new StringBuilder();
		int fresh = 0;
		for (net.minecraft.world.entity.Entity e : client.level.entitiesForRendering()) {
			if (e instanceof dev.overcraft.entity.HypersphereEntity && SPHERES_SEEN.add(e.getId())) {
				fresh++;
			}
			if (e instanceof dev.overcraft.entity.SigmaBarrierEntity b) {
				out.append(String.format(java.util.Locale.ROOT, " b %.2f %.2f %.2f %d %.3f", b.getX(), b.getY(), b.getZ(), b.state(), b.health()));
			}
		}
		return (fresh > 0 ? " h" + fresh : "") + out;
	}
}
