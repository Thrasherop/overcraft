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
	public static final int PORT = 25599;

	private DevServer() {
	}

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
					out.addProperty("primaryAmmo", s.primaryAmmo());
					out.addProperty("secondaryAmmo", s.secondaryAmmo());
					out.addProperty("ability1Cooldown", s.ability1Cooldown());
					out.addProperty("ability2Cooldown", s.ability2Cooldown());
					out.addProperty("vx", player.getDeltaMovement().x);
					out.addProperty("vy", player.getDeltaMovement().y);
					out.addProperty("vz", player.getDeltaMovement().z);
					out.addProperty("anim", HeroAnimator.of(player).playing());
					String attached = player.getAttached(dev.overcraft.hero.HeroManager.HERO);
					out.addProperty("heroAttached", attached == null ? "" : attached);
				}
			}
			case "newworld" -> {
				String[] a = arg.split(" ");
				String name = a[0];
				boolean creative = a.length > 1 && a[1].equals("creative");
				CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(null));
				mc.execute(() -> {
					if (mc.gui.screen() instanceof CreateWorldScreen create) {
						WorldCreationUiState ui = create.getUiState();
						ui.setName(name);
						ui.setGameMode(creative ? WorldCreationUiState.SelectedGameMode.CREATIVE : WorldCreationUiState.SelectedGameMode.SURVIVAL);
						ui.setAllowCommands(true);
						ui.getNormalPresetList().stream()
							.filter(e -> e.preset() != null && e.preset().is(WorldPresets.FLAT))
							.findFirst().ifPresent(ui::setWorldType);
						((CreateWorldScreenAccessor) create).overcraft$onCreate();
					}
				});
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
				InputConstants.Key key = InputConstants.getKey(arg.trim());
				long handle = mc.getWindow().handle();
				net.minecraft.client.input.KeyEvent event = new net.minecraft.client.input.KeyEvent(key.getValue(), 0, 0);
				mc.keyboardHandler.keyPress(handle, 1, event);
				mc.keyboardHandler.keyPress(handle, 0, event);
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
