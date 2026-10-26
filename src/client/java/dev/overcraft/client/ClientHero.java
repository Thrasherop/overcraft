package dev.overcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.Overcraft;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * The client's side of the hero: the last state the server sent, the hero keys, and turning mouse and keys into hero
 * controls while hero mode is on (hotbar slot 1). In hero mode, vanilla actions on the same keys (E inventory,
 * Q drop, Shift sneak, attack, use) don't happen; Ctrl crouches instead of sprinting, as in Overwatch.
 */
public final class ClientHero {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Overcraft.id("heroes"));

	public static final KeyMapping ABILITY_1 = key("ability_1", InputConstants.KEY_LSHIFT);
	public static final KeyMapping ABILITY_2 = key("ability_2", InputConstants.KEY_E);
	public static final KeyMapping ULTIMATE = key("ultimate", InputConstants.KEY_Q);
	public static final KeyMapping RELOAD = key("reload", InputConstants.KEY_R);
	/** Overwatch's default quick melee key. */
	public static final KeyMapping QUICK_MELEE = key("quick_melee", InputConstants.KEY_V);
	public static final KeyMapping HERO_MODE = key("hero_mode", InputConstants.KEY_G);
	public static final KeyMapping HERO_SELECT = key("hero_select", InputConstants.KEY_H);

	private static final Set<KeyMapping> HERO_KEYS = Set.of(ABILITY_1, ABILITY_2, ULTIMATE, RELOAD, QUICK_MELEE);

	private static HeroSyncPayload sync = HeroSyncPayload.NONE;
	private static int lastHeld;
	/** The Steve-mode slot to go back to when hero mode is toggled off. */
	private static int steveSlot = 1;
	private static boolean openSelectOnJoin;

	private ClientHero() {
	}

	private static KeyMapping key(String name, int keyCode) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.overcraft." + name, InputConstants.Type.KEYBOARD, keyCode, CATEGORY));
	}

	public static void init() {
		// keys are registered by the static fields above
	}

	public static void onSync(HeroSyncPayload payload) {
		sync = payload;
	}

	public static HeroSyncPayload sync() {
		return sync;
	}

	public static @Nullable Hero hero() {
		return sync.heroId().isEmpty() ? null : Hero.byId(sync.heroId());
	}

	/** Hero mode: a hero, hotbar slot 1 selected, alive and playing. */
	public static boolean controls() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && hero() != null && player.getInventory().getSelectedSlot() == HeroManager.HERO_SLOT
			&& player.isAlive() && !player.isSpectator();
	}

	public static void onJoin() {
		sync = HeroSyncPayload.NONE;
		lastHeld = 0;
		openSelectOnJoin = true;
	}

	public static void onDisconnect() {
		sync = HeroSyncPayload.NONE;
		openSelectOnJoin = false;
	}

	/** Start of each client tick, before Minecraft handles its own keys. */
	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		if (openSelectOnJoin && mc.gui.screen() == null && mc.level != null) {
			openSelectOnJoin = false;
			mc.gui.setScreen(new HeroSelectScreen(true));
		}

		if (player.getInventory().getSelectedSlot() != HeroManager.HERO_SLOT) {
			steveSlot = player.getInventory().getSelectedSlot();
		}
		while (HERO_SELECT.consumeClick()) {
			if (hero() == null || sync.canChangeHero()) {
				mc.gui.setScreen(new HeroSelectScreen(false));
			} else {
				player.sendOverlayMessage(Component.translatable("overcraft.change_hero.too_far"));
			}
		}
		while (HERO_MODE.consumeClick()) {
			toggleHeroMode(player);
		}

		boolean controls = controls() && mc.gui.screen() == null;
		if (!controls) {
			for (KeyMapping key : HERO_KEYS) {
				while (key.consumeClick()) {
				}
			}
			if (lastHeld != 0) {
				lastHeld = 0;
				ClientPlayNetworking.send(new HeroInputPayload(0, 0));
			}
			return;
		}

		// the fire buttons: held, or tapped since last tick
		boolean primary = drain(mc.options.keyAttack) | mc.options.keyAttack.isDown();
		boolean secondary = drain(mc.options.keyUse) | mc.options.keyUse.isDown();
		int held = (primary ? HeroInputPayload.HELD_PRIMARY : 0) | (secondary ? HeroInputPayload.HELD_SECONDARY : 0);

		int pressed = 0;
		pressed |= drain(ABILITY_1) ? HeroInputPayload.PRESS_ABILITY_1 : 0;
		pressed |= drain(ABILITY_2) ? HeroInputPayload.PRESS_ABILITY_2 : 0;
		pressed |= drain(ULTIMATE) ? HeroInputPayload.PRESS_ULTIMATE : 0;
		pressed |= drain(RELOAD) ? HeroInputPayload.PRESS_RELOAD : 0;
		pressed |= drain(QUICK_MELEE) ? HeroInputPayload.PRESS_MELEE : 0;

		// vanilla actions bound to the same keys as a hero key don't happen in hero mode (E inventory, Q drop, ...)
		for (KeyMapping vanilla : mc.options.keyMappings) {
			if (!HERO_KEYS.contains(vanilla) && sharesKey(vanilla) && vanilla != mc.options.keyShift) {
				drain(vanilla);
			}
		}

		if (held != lastHeld || pressed != 0) {
			lastHeld = held;
			ClientPlayNetworking.send(new HeroInputPayload(held, pressed));
		}
	}

	/** End of each client tick: heroes don't sprint. */
	public static void endTick(Minecraft mc) {
		if (mc.player != null && controls() && mc.player.isSprinting()) {
			mc.player.setSprinting(false);
		}
	}

	private static void toggleHeroMode(LocalPlayer player) {
		if (hero() == null) {
			player.sendOverlayMessage(Component.translatable("overcraft.hero_mode.no_hero", HERO_SELECT.getTranslatedKeyMessage()));
			return;
		}
		int slot = player.getInventory().getSelectedSlot();
		if (slot == HeroManager.HERO_SLOT) {
			player.getInventory().setSelectedSlot(steveSlot);
		} else {
			steveSlot = slot;
			player.getInventory().setSelectedSlot(HeroManager.HERO_SLOT);
		}
	}

	/** A vanilla key mapping on the same key as one of the hero's keys. */
	public static boolean sharesKey(KeyMapping vanilla) {
		for (KeyMapping hero : HERO_KEYS) {
			if (!hero.isUnbound() && hero.same(vanilla)) {
				return true;
			}
		}
		return false;
	}

	private static boolean drain(KeyMapping key) {
		boolean any = false;
		while (key.consumeClick()) {
			any = true;
		}
		return any;
	}
}
