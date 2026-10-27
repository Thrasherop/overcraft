package dev.overcraft.client.controls;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.Overcraft;
import dev.overcraft.client.ClientHero;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Hero mode's own controls: Overwatch's key set, separate from Minecraft's (owner, 2026-10-26: "Hitting G toggles
 * between all keybinds"). In hero mode these keys work and Minecraft's gameplay keys (moving, attacking, using,
 * inventory, dropping, sneaking...) don't; in Minecraft mode it's the other way round. Menus, chat, screenshots and the
 * hotbar's number keys work in both.
 *
 * <p>The mappings aren't in Minecraft's Controls screen: they're set on the Overwatch options screen (Esc in hero mode)
 * and saved in config/overcraft-hero-controls.json. Defaults are Overwatch's, except the fire buttons: the owner chose
 * kunai on the left button and ofuda on the right (2026-10-25), the other way round from Overwatch's default.
 */
public final class HeroControls {
	private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Overcraft.id("hero_controls"));
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("overcraft-hero-controls.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public enum Section { MOVEMENT, COMBAT, INTERFACE }

	public enum Action {
		MOVE_FORWARD(Section.MOVEMENT, keyboard(InputConstants.KEY_W)),
		MOVE_BACK(Section.MOVEMENT, keyboard(InputConstants.KEY_S)),
		MOVE_LEFT(Section.MOVEMENT, keyboard(InputConstants.KEY_A)),
		MOVE_RIGHT(Section.MOVEMENT, keyboard(InputConstants.KEY_D)),
		JUMP(Section.MOVEMENT, keyboard(InputConstants.KEY_SPACE)),
		CROUCH(Section.MOVEMENT, keyboard(InputConstants.KEY_LCONTROL)),
		/** Overwatch's primary fire (Kiriko: Healing Ofuda), on the right button by the owner's choice. */
		PRIMARY_FIRE(Section.COMBAT, mouse(InputConstants.MOUSE_BUTTON_RIGHT)),
		/** Overwatch's secondary fire (Kiriko: Kunai), on the left button. */
		SECONDARY_FIRE(Section.COMBAT, mouse(InputConstants.MOUSE_BUTTON_LEFT)),
		ABILITY_1(Section.COMBAT, keyboard(InputConstants.KEY_LSHIFT)),
		ABILITY_2(Section.COMBAT, keyboard(InputConstants.KEY_E)),
		ULTIMATE(Section.COMBAT, keyboard(InputConstants.KEY_Q)),
		RELOAD(Section.COMBAT, keyboard(InputConstants.KEY_R)),
		QUICK_MELEE(Section.COMBAT, keyboard(InputConstants.KEY_V)),
		/** Overwatch's interact key: here doors, buttons, chests and villagers, which the fire buttons no longer do. */
		INTERACT(Section.INTERFACE, keyboard(InputConstants.KEY_F)),
		/** Hold to choose a perk once one is ready, left or right click picking (Overwatch's: Left Alt, #36). */
		PERK_MENU(Section.INTERFACE, keyboard(InputConstants.KEY_LALT)),
		/** Hold for the communication wheel, tap for "Good job" (the owner: T, as Overwatch's; dev.overcraft.comms). */
		COMMS(Section.INTERFACE, keyboard(InputConstants.KEY_T)),
		/** Overwatch's chat key: Enter, since T is the wheel's in hero mode. */
		CHAT(Section.INTERFACE, keyboard(InputConstants.KEY_RETURN)),
		HERO_SELECT(Section.INTERFACE, keyboard(InputConstants.KEY_H)),
		MINECRAFT_MODE(Section.INTERFACE, keyboard(InputConstants.KEY_G));

		public final Section section;
		public final InputConstants.Key defaultKey;
		public final KeyMapping mapping;

		Action(Section section, InputConstants.Key defaultKey) {
			this.section = section;
			this.defaultKey = defaultKey;
			this.mapping = new KeyMapping("key.overcraft.hero." + id(), defaultKey.getType(), defaultKey.getValue(), CATEGORY);
		}

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		public InputConstants.Key key() {
			return KeyMappingHelper.getBoundKeyOf(mapping);
		}
	}

	private HeroControls() {
	}

	private static InputConstants.Key keyboard(int key) {
		return InputConstants.Type.KEYBOARD.getOrCreate(key);
	}

	private static InputConstants.Key mouse(int button) {
		return InputConstants.Type.MOUSE.getOrCreate(button);
	}

	public static KeyMapping get(Action action) {
		return action.mapping;
	}

	/** A hero key bound to this mouse button is down. */
	public static boolean mouseButtonHeld(int button) {
		InputConstants.Key mouse = mouse(button);
		for (Action action : Action.values()) {
			if (action.key().equals(mouse) && action.mapping.isDown()) {
				return true;
			}
		}
		return false;
	}

	/** Loads the saved keys (once, at start). */
	public static void load() {
		if (Files.exists(FILE)) {
			try {
				JsonObject saved = GSON.fromJson(Files.readString(FILE), JsonObject.class);
				for (Action action : Action.values()) {
					if (saved != null && saved.has(action.id())) {
						action.mapping.setKey(InputConstants.getKey(saved.get(action.id()).getAsString()));
					}
				}
			} catch (IOException | RuntimeException e) {
				Overcraft.LOG.warn("Couldn't read {}: {}", FILE, e.toString());
			}
		}
		KeyMapping.resetMapping();
	}

	public static void save() {
		JsonObject out = new JsonObject();
		for (Action action : Action.values()) {
			out.addProperty(action.id(), action.key().getName());
		}
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(out));
		} catch (IOException e) {
			Overcraft.LOG.warn("Couldn't save {}: {}", FILE, e.toString());
		}
	}

	public static void set(Action action, InputConstants.Key key) {
		action.mapping.setKey(key);
		KeyMapping.resetMapping();
		save();
	}

	public static void resetAll() {
		for (Action action : Action.values()) {
			action.mapping.setKey(action.defaultKey);
		}
		KeyMapping.resetMapping();
		save();
	}

	/** Other actions on the same key (both would fire). */
	public static boolean conflicts(Action action) {
		InputConstants.Key key = action.key();
		if (key.equals(InputConstants.UNKNOWN)) {
			return false;
		}
		for (Action other : Action.values()) {
			if (other != action && other.key().equals(key)) {
				return true;
			}
		}
		return false;
	}

	/** Drops the clicks hero keys collect while Minecraft mode is on, so none fire on entering hero mode. */
	public static void drainAll() {
		for (Action action : Action.values()) {
			while (action.mapping.consumeClick()) {
			}
		}
	}

	/**
	 * Minecraft's gameplay keys, which do nothing in hero mode (the hero's keys do instead). Minecraft's own hero-mode
	 * and change-hero keys are here too: in hero mode MINECRAFT_MODE and HERO_SELECT take their place.
	 */
	public static boolean suppressedInHeroMode(KeyMapping key) {
		Options o = Minecraft.getInstance().options;
		if (o == null) {
			return false;
		}
		return key == o.keyUp || key == o.keyDown || key == o.keyLeft || key == o.keyRight || key == o.keyJump
			|| key == o.keyShift || key == o.keySprint || key == o.keyAttack || key == o.keyUse || key == o.keyPickItem
			|| key == o.keyDrop || key == o.keyInventory || key == o.keySwapOffhand
			|| key == ClientHero.HERO_MODE || key == ClientHero.HERO_SELECT
			// Minecraft's chat on the wheel's key (T): the wheel has it in hero mode, chat is on the hero's chat key
			|| key == o.keyChat && KeyMappingHelper.getBoundKeyOf(key).equals(Action.COMMS.key())
			// no hotbar in hero mode: its keys don't change the held slot (they used to switch modes, until #26)
			|| java.util.Arrays.asList(o.keyHotbarSlots).contains(key);
	}
}
