package dev.overcraft.client;

import dev.overcraft.hero.Perk;
import dev.overcraft.client.comms.CommsWheel;
import dev.overcraft.client.comms.EmoteCamera;
import dev.overcraft.net.PickPerkPayload;
import dev.overcraft.net.SelectHeroPayload;
import dev.overcraft.net.HeroModePayload;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.Overcraft;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.client.controls.HeroOptions;
import dev.overcraft.client.controls.HeroControls.Action;
import dev.overcraft.client.mixin.MinecraftInvoker;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The client's side of the hero: the last state the server sent, and turning the hero's keys (HeroControls) into
 * hero input while hero mode is on. Hero mode has its own key set, Overwatch's: G switches between it and Minecraft's
 * (the hotbar used to: slot 1 was hero mode, until #26).
 */
public final class ClientHero {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Overcraft.id("heroes"));

	/** Minecraft mode's keys for getting into hero mode and picking a hero (hero mode has its own: HeroControls). */
	public static final KeyMapping HERO_MODE = key("hero_mode", InputConstants.KEY_G);
	public static final KeyMapping HERO_SELECT = key("hero_select", InputConstants.KEY_H);

	private static HeroSyncPayload sync = HeroSyncPayload.NONE;
	private static int lastHeld;
	/** Toggle Healing Ofuda: whether it's on, and whether the ofuda were refilling last tick. */
	private static boolean ofudaOn;
	private static boolean wasRefilling;
	/**
	 * Hero mode as switched here, until the server's synced flag (HeroManager.HERO_MODE) agrees: G acts at once, not a
	 * round trip later. Null: the server's flag.
	 */
	private static @Nullable Boolean heroModeLocal;
	private static boolean openSelectOnJoin;
	private static boolean interacting;

	private ClientHero() {
	}

	private static KeyMapping key(String name, int keyCode) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.overcraft." + name, InputConstants.Type.KEYBOARD, keyCode, CATEGORY));
	}

	public static void init() {
		// Minecraft-mode keys are registered by the static fields above; hero mode's come from their own file
		HeroControls.load();
		HeroOptions.load();
	}

	/** The interact key is being handled: Minecraft's right-click may run (MinecraftMixin). */
	public static boolean interacting() {
		return interacting;
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

	/**
	 * The screen in hero mode: a hero and hero mode on, alive or not (the HUD's choice; dying flipped it to Minecraft's
	 * hotbar and hearts behind the death screen).
	 */
	public static boolean heroView() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && hero() != null && heroModeOn() && !player.isSpectator();
	}

	/** Hero mode is switched on (G), whether or not the hero can act right now. */
	public static boolean heroModeOn() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return false;
		}
		boolean synced = player.getAttachedOrElse(HeroManager.HERO_MODE, true);
		if (heroModeLocal != null && heroModeLocal == synced) {
			heroModeLocal = null;
		}
		return heroModeLocal != null ? heroModeLocal : synced;
	}

	/** A hero was just picked (the server puts the player in hero mode). */
	public static void pickedHero() {
		heroModeLocal = true;
	}

	/**
	 * Hero mode's chat as Overwatch's (small, the HUD font, no panel): in hero view with nobody typing, including behind
	 * the death screen (there Minecraft's chat crossed the portrait, the sixth critique).
	 */
	public static boolean chatQuiet() {
		Screen screen = Minecraft.getInstance().gui.screen();
		return heroView() && (screen == null || screen instanceof DeathScreen);
	}

	/** Hero mode: a hero, hero mode on, alive and playing. */
	public static boolean controls() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && hero() != null && heroModeOn() && player.isAlive() && !player.isSpectator();
	}

	public static void onJoin() {
		sync = HeroSyncPayload.NONE;
		lastHeld = 0;
		ofudaOn = false;
		heroModeLocal = null;
		// on a server without Overcraft the mod stays out of sight: no hero select, no messages (#32)
		if (!serverHasOvercraft()) {
			openSelectOnJoin = false;
			return;
		}
		Component outdated = dev.overcraft.client.assets.AssetPackVersion.warning();
		if (outdated != null && Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.sendSystemMessage(outdated.copy().withStyle(net.minecraft.ChatFormatting.RED));
		}
		HeroOptions.send();
		openSelectOnJoin = true;
	}

	/**
	 * The server has Overcraft (it takes our hero picks). A plain server never sends a hero, so the HUD, controls and
	 * models stay Minecraft's on their own; this keeps the rest quiet there too (#32).
	 */
	public static boolean serverHasOvercraft() {
		return Minecraft.getInstance().player != null && ClientPlayNetworking.canSend(SelectHeroPayload.TYPE);
	}

	public static void onDisconnect() {
		EmoteCamera.stop(Minecraft.getInstance());
		sync = HeroSyncPayload.NONE;
		heroModeLocal = null;
		openSelectOnJoin = false;
	}

	/** Start of each client tick, before Minecraft handles its own keys. */
	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		// emoting, the view swings out round her and the mouse orbits it, as Overwatch's does; back when it ends
		boolean emoting = hero() != null && dev.overcraft.client.model.HeroAnimator.of(player).emoting();
		if (emoting) {
			EmoteCamera.start(mc, player);
		} else {
			EmoteCamera.stop(mc);
		}
		// hero chat is split into lines in its own face and width (ChatComponentMixin): split the history again on a switch
		boolean view = heroView();
		if (view != chatHeroView) {
			chatHeroView = view;
			mc.gui.hud.getChat().rescaleChat();
		}
		if (openSelectOnJoin && mc.gui.screen() == null && mc.level != null) {
			openSelectOnJoin = false;
			mc.gui.setScreen(new HeroSelectScreen(true));
		}

		if (!serverHasOvercraft()) {
			// a server without Overcraft: G and H do nothing (no "pick a hero", no select screen: #32)
			drain(HERO_MODE);
			drain(HERO_SELECT);
			return;
		}
		// each mode's own keys switch modes and open the hero select (the other mode's are silenced or drained)
		boolean heroMode = controls();
		if (heroMode) {
			// Minecraft mode's G and H are silenced in hero mode; asking drops their clicks (else they'd fire later)
			drain(HERO_MODE);
			drain(HERO_SELECT);
		}
		KeyMapping select = heroMode ? HeroControls.get(Action.HERO_SELECT) : HERO_SELECT;
		KeyMapping toggle = heroMode ? HeroControls.get(Action.MINECRAFT_MODE) : HERO_MODE;
		while (select.consumeClick()) {
			if (hero() == null || sync.canChangeHero()) {
				mc.gui.setScreen(new HeroSelectScreen(false));
			} else {
				player.sendOverlayMessage(Component.translatable("overcraft.change_hero.too_far"));
			}
		}
		boolean toggled = false;
		while (toggle.consumeClick()) {
			toggled = true;
		}
		if (toggled) {
			toggleHeroMode(player);
		}

		boolean controls = controls() && mc.gui.screen() == null;
		if (!controls || toggled) {
			// Minecraft mode (or just switched): the hero keys' clicks must not fire later
			HeroControls.drainAll();
			ofudaOn = false;
			perkMenu = false;
			CommsWheel.cancel();
			if (lastHeld != 0) {
				lastHeld = 0;
				ClientPlayNetworking.send(new HeroInputPayload(0, 0));
			}
			return;
		}

		// the perk menu (#36): Left Alt held while a perk waits; the left or right mouse button picks the left or right
		// perk (Overwatch's, whatever the fire buttons are bound to), and fires nothing meanwhile
		if (perkMenu(mc) | CommsWheel.tick(mc)) {
			HeroControls.drainAll();
			ofudaOn = false;
			if (lastHeld != 0) {
				lastHeld = 0;
				ClientPlayNetworking.send(new HeroInputPayload(0, 0));
			}
			return;
		}

		// the fire buttons: held, or tapped since last tick. Overwatch's primary fire throws Kiriko's ofuda, its
		// secondary her kunai; the server's weapon slots are the other way round (primary = kunai).
		boolean ofudaPressed = drain(HeroControls.get(Action.PRIMARY_FIRE));
		boolean kunaiPressed = drain(HeroControls.get(Action.SECONDARY_FIRE));
		boolean kunai = kunaiPressed | HeroControls.get(Action.SECONDARY_FIRE).isDown();
		boolean ofuda;
		if (HeroOptions.get().toggleOfuda) {
			// Overwatch's default: a press starts Healing Ofuda, the next stops it; it also stops when the clip runs out
			// (its refill starts) or the kunai are thrown
			boolean refilling = sync.secondaryReload() >= 0;
			if (ofudaPressed) {
				ofudaOn = !ofudaOn;
			}
			if (refilling && !wasRefilling || kunai) {
				ofudaOn = false;
			}
			wasRefilling = refilling;
			ofuda = ofudaOn;
		} else {
			ofuda = ofudaPressed | HeroControls.get(Action.PRIMARY_FIRE).isDown();
		}
		int held = (kunai ? HeroInputPayload.HELD_PRIMARY : 0) | (ofuda ? HeroInputPayload.HELD_SECONDARY : 0)
			| (OwMovement.isClimbing(player) ? HeroInputPayload.HELD_WALL_CLIMB : 0);

		int pressed = 0;
		pressed |= drain(HeroControls.get(Action.ABILITY_1)) ? HeroInputPayload.PRESS_ABILITY_1 : 0;
		pressed |= drain(HeroControls.get(Action.ABILITY_2)) ? HeroInputPayload.PRESS_ABILITY_2 : 0;
		pressed |= drain(HeroControls.get(Action.ULTIMATE)) ? HeroInputPayload.PRESS_ULTIMATE : 0;
		pressed |= drain(HeroControls.get(Action.RELOAD)) ? HeroInputPayload.PRESS_RELOAD : 0;
		pressed |= drain(HeroControls.get(Action.QUICK_MELEE)) ? HeroInputPayload.PRESS_MELEE : 0;

		dev.overcraft.client.comms.Pings.tick(mc);

		// chat on Overwatch's key (Enter): T is the communication wheel's in hero mode
		if (drain(HeroControls.get(Action.CHAT))) {
			mc.gui.openChatScreen(net.minecraft.client.gui.components.ChatComponent.ChatMethod.MESSAGE);
			return;
		}

		// interact: Minecraft's right-click (doors, buttons, chests, villagers), which the fire buttons don't do
		if (drain(HeroControls.get(Action.INTERACT))) {
			interacting = true;
			try {
				((MinecraftInvoker) mc).overcraft$startUseItem();
			} finally {
				interacting = false;
			}
		}

		if (held != lastHeld || pressed != 0) {
			lastHeld = held;
			ClientPlayNetworking.send(new HeroInputPayload(held, pressed));
		}
	}

	private static boolean chatHeroView;

	/** The perk menu is up (HeroHud draws it). */
	private static boolean perkMenu;
	private static boolean perkLeftWas;
	private static boolean perkRightWas;
	/** A pick was sent: no other until the server's answer (game time), so a double click picks once. */
	private static long perkPickedAt = -100;
	/** The mouse button that picked a perk is still down. */
	private static boolean perkClickHeld;

	public static boolean perkMenuOpen() {
		return perkMenu;
	}

	/** Runs the perk menu this tick; true while it's up. */
	private static boolean perkMenu(Minecraft mc) {
		int tier = sync.perkReady();
		boolean open = tier > 0 && HeroControls.get(Action.PERK_MENU).isDown() && hero() != null;
		// the physical buttons, or a hero key bound to them being held (what the dev harness presses)
		boolean left = mc.mouseHandler.isLeftPressed() || HeroControls.mouseButtonHeld(InputConstants.MOUSE_BUTTON_LEFT);
		boolean right = mc.mouseHandler.isRightPressed() || HeroControls.mouseButtonHeld(InputConstants.MOUSE_BUTTON_RIGHT);
		if (open && perkMenu && mc.level != null && mc.level.getGameTime() - perkPickedAt > 10) {
			// a press, not a button already held when the menu came up
			int side = left && !perkLeftWas ? 0 : right && !perkRightWas ? 1 : -1;
			java.util.List<Perk> choices = Perk.of(hero(), tier == 1 ? Perk.Tier.MINOR : Perk.Tier.MAJOR);
			if (side >= 0 && side < choices.size()) {
				ClientPlayNetworking.send(new PickPerkPayload(choices.get(side).ordinal()));
				perkPickedAt = mc.level.getGameTime();
				perkClickHeld = true;
			}
		}
		perkLeftWas = left;
		perkRightWas = right;
		perkMenu = open;
		// the click that picked fires nothing, even once the menu has closed under it (it threw a kunai: found playing)
		if (perkClickHeld && !left && !right) {
			perkClickHeld = false;
		}
		return open || perkClickHeld;
	}

	/** End of each client tick: heroes don't sprint. */
	public static void endTick(Minecraft mc) {
		if (mc.player != null && controls() && mc.player.isSprinting()) {
			mc.player.setSprinting(false);
		}
		// no Creative flight in hero mode: tapping jump twice (letting go and climbing on) switched it on mid-climb, and
		// our movement under Minecraft's flight damping left her hovering (#25). Minecraft mode keeps it
		if (mc.player != null && controls() && mc.player.getAbilities().flying && !mc.player.isSpectator()) {
			mc.player.getAbilities().flying = false;
			mc.player.onUpdateAbilities();
		}
	}

	/**
	 * One of the hero's own sounds, in their head: no position, no falloff, as Overwatch plays a hero's own sounds
	 * (OwnSoundPayload, #17). Nothing if no pack has the sound.
	 */
	public static void playOwn(Identifier sound, float volume, float pitch, long seed) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getSoundManager().getSoundEvent(sound) != null) {
			mc.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.PLAYERS, volume, pitch, RandomSource.create(seed), false,
				0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
		}
	}

	private static void toggleHeroMode(LocalPlayer player) {
		if (hero() == null) {
			player.sendOverlayMessage(Component.translatable("overcraft.hero_mode.no_hero", HERO_SELECT.getTranslatedKeyMessage()));
			return;
		}
		setHeroMode(!heroModeOn());
	}

	/** Hero mode on or off: here at once, and on the server. */
	public static void setHeroMode(boolean on) {
		heroModeLocal = on;
		if (ClientPlayNetworking.canSend(HeroModePayload.TYPE)) {
			ClientPlayNetworking.send(new HeroModePayload(on));
		}
	}

	private static boolean drain(KeyMapping key) {
		boolean any = false;
		while (key.consumeClick()) {
			any = true;
		}
		return any;
	}
}
