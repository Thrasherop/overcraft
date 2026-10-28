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
					out.addProperty("slot", player.getInventory().getSelectedSlot());
					out.addProperty("heroMode", ClientHero.controls());
					HeroSyncPayload s = ClientHero.sync();
					out.addProperty("hero", s.heroId());
					out.addProperty("ult", s.ultCharge());
					out.addProperty("perkLevel", s.perkLevel());
					out.addProperty("perkProgress", s.perkProgress());
					out.addProperty("primaryAmmo", s.primaryAmmo());
					out.addProperty("secondaryAmmo", s.secondaryAmmo());
					out.addProperty("ability1Cooldown", s.ability1Cooldown());
					out.addProperty("ability2Cooldown", s.ability2Cooldown());
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
					out.addProperty("anim1p", HeroAnimator.of(player).playingFirstPerson());
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
				} else if (arg.equals("reset")) {
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
			case "hero" -> {
				ClientPlayNetworking.send(new SelectHeroPayload(arg));
				out.addProperty("ok", true);
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
					mc.keyboardHandler.keyPress(handle, a[1].equals("up") ? 0 : 1, event);
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
							TRACE.add(String.format(java.util.Locale.ROOT, "%d %.2f %.2f %.2f %.3f %s %s %.2f %.2f", traceTick++,
								Math.sqrt(dx * dx + dz * dz), dx, p.getDeltaMovement().y * 20, p.getY(), p.onGround() ? "G" : "-",
								keys.isEmpty() ? "." : keys, dz, p.getYRot()));
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
			case "heromode" -> {
				// dev only: hero mode on or off, as G does
				ClientHero.setHeroMode(!arg.equals("off"));
				out.addProperty("ok", true);
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
}
