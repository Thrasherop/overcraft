package dev.overcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overcraft.client.hud.PotgCard;
import dev.overcraft.client.screen.HighlightsScreen;
import dev.overcraft.net.HighlightsPayload;
import dev.overcraft.net.PotgPayload;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Play of the Game, the client's half (techniques/potg/design.md, techniques/potg/potg_look.md): from the server's CARD
 * (or SOLO, a highlight watched alone) until the end screen has played out, this client is in a showing. The server's
 * replay engine takes the player out of the world and logs them into the replay and back again, so the mod's own
 * join-time reactions are held off meanwhile: no Hero Select, no asset-pack line, no toasts (the spike: Hero Select opened
 * on the replay's login and again on return). The holder screen (PotgCard.Holder) is kept open the whole time: it takes
 * every key and click (owner: nobody skips) and keeps the pause menu from opening when the window loses focus, which would
 * hold a single player's replay stream. The look itself is PotgCard. Also the Highlights key (owner, 2026-11-11: a menu
 * of the player's own top 5, after the world's dragon has died).
 */
public final class ClientPotg {
	private ClientPotg() {
	}

	/** The Highlights menu (ours: K; it's also in hero mode's Esc menu). */
	public static final KeyMapping HIGHLIGHTS = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.overcraft.highlights", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, ClientHero.CATEGORY));

	private static boolean active;
	/** The server said the viewer is back (END). */
	private static boolean back;
	/** Wall clock: the play's start (the body held still till then), and a safety end if nothing ends it. */
	private static long cardUntil;
	private static long giveUpAt;

	public static boolean active() {
		return active;
	}

	/** Wall clock: when the last showing ended here (0: none since connecting). */
	private static long endedAt;

	/**
	 * Whether a login now is the replay engine logging this player back after a showing (it can land after the end screen:
	 * the hero select opened after every watched highlight, found playing 0.3.109). Once: the next login is a join again.
	 */
	public static boolean backFromShowing() {
		boolean back = endedAt > 0 && net.minecraft.util.Util.getMillis() - endedAt < 15_000;
		endedAt = 0;
		return back;
	}

	/**
	 * In a showing's replay, the player the camera follows (the scorer, PotgReplay's camera lock), as the viewer's client
	 * knows them; null otherwise. Their view is drawn as theirs: their hero's arms and HUD (#155, the owner: "No UI of the
	 * player. like, no hands, no animations, no cooldowns shown"; Overwatch's POTG is the scorer's own first person).
	 */
	public static net.minecraft.client.player.@org.jspecify.annotations.Nullable AbstractClientPlayer followed() {
		if (!active) {
			return null;
		}
		Minecraft mc = Minecraft.getInstance();
		return mc.getCameraEntity() instanceof net.minecraft.client.player.AbstractClientPlayer p && p != mc.player ? p : null;
	}

	/** Before the play starts: the player's own body is held still (the world is frozen too). */
	public static boolean holdBody() {
		return active && net.minecraft.util.Util.getMillis() < cardUntil;
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(PotgPayload.TYPE, (payload, context) -> on(payload));
		ClientPlayNetworking.registerGlobalReceiver(HighlightsPayload.TYPE, (payload, context) -> {
			if (payload.watch() == HighlightsPayload.STATUS) {
				highlightsOpen("1".equals(payload.data()));
			} else {
				HighlightsScreen.received(payload.data());
			}
		});
		// a new world starts closed until its server says otherwise
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> highlightsOpen = false);
		dev.overcraft.potg.ReplayTap.clientPaused = () -> Minecraft.getInstance().isPaused();
	}

	/** Whether this world's highlights are open (its dragon killed), as its server last said: the menu's item shows only then. */
	private static boolean highlightsOpen;

	public static boolean highlightsOpen() {
		return highlightsOpen;
	}

	/** The server's answer, to the menu if it is open (its item comes or goes). */
	public static void highlightsOpen(boolean open) {
		boolean changed = open != highlightsOpen;
		highlightsOpen = open;
		if (changed && Minecraft.getInstance().gui.screen() instanceof dev.overcraft.client.screen.OverwatchMenuScreen menu) {
			menu.refreshItems();
		}
	}

	/** Asks the server whether the highlights are open (the menu, as it opens). */
	public static void askHighlightsOpen() {
		if (ClientPlayNetworking.canSend(HighlightsPayload.TYPE)) {
			ClientPlayNetworking.send(new HighlightsPayload(HighlightsPayload.STATUS, ""));
		}
	}

	/** Asks the server for the player's highlights: the menu opens when they come. */
	public static void openHighlights() {
		ClientPlayNetworking.send(new HighlightsPayload(HighlightsPayload.LIST, ""));
	}

	private static void on(PotgPayload p) {
		Minecraft mc = Minecraft.getInstance();
		long now = net.minecraft.util.Util.getMillis();
		switch (p.phase()) {
			case PotgPayload.CARD, PotgPayload.SOLO -> {
				active = true;
				back = false;
				cardUntil = now + Math.round(p.cardSeconds() * 1000);
				// a lost END gives up a little after the server's own watchdog would have ended it, and its end screen
				// (#172: a flat 90 s locked the player out for over a minute)
				giveUpAt = cardUntil + dev.overcraft.potg.PotgReplay.WINDOW_TICKS * 50L
					+ dev.overcraft.potg.PotgReplay.WATCHDOG_EXTRA_MS + dev.overcraft.potg.PotgReplay.OUTRO_MS + 5_000;
				EndMusic.pauseForShowing();
				PotgCard.show(p.phase() == PotgPayload.CARD, p.data());
				mc.gui.setScreen(new PotgCard.Holder());
			}
			case PotgPayload.CUT -> PotgCard.cut();
			default -> {
				back = true;
				// the play ended without a CUT (a lost viewer): cut now
				PotgCard.cut();
			}
		}
	}

	private static void finish(Minecraft mc) {
		active = false;
		endedAt = net.minecraft.util.Util.getMillis();
		// the server holds the world until every viewer's end screen is over (critique 6): this one's is
		if (mc.getConnection() != null && ClientPlayNetworking.canSend(HighlightsPayload.TYPE)) {
			ClientPlayNetworking.send(new HighlightsPayload(HighlightsPayload.DONE, ""));
		}
		PotgCard.hide();
		PotgMusic.stop();
		mc.getSoundManager().stop();
		mc.gui.toastManager().clear();
		if (mc.gui.screen() instanceof PotgCard.Holder) {
			mc.gui.setScreen(null);
		}
	}

	/** Each client tick: the look's clock, the holder back if a login's loading screen took it, the end, the safety end. */
	public static void tick(Minecraft mc) {
		PotgMusic.tick();
		if (!active) {
			while (HIGHLIGHTS.consumeClick()) {
				if (mc.player != null && mc.gui.screen() == null) {
					openHighlights();
				}
			}
			return;
		}
		PotgCard.tick(mc);
		PotgCard.advance(mc);
		long now = net.minecraft.util.Util.getMillis();
		if (now > giveUpAt || back && PotgCard.finished()) {
			finish(mc);
			return;
		}
		var screen = mc.gui.screen();
		// the holder back if a login's loading screen took it, or the pause menu (the window losing focus) did: that menu
		// would hold a single player's replay stream (found in play: the LAN guest's menu opened mid-replay)
		boolean loading = screen != null && (screen.getClass().getSimpleName().contains("Loading")
			|| screen.getClass().getSimpleName().contains("Receiving"));
		if (mc.level != null && !(screen instanceof PotgCard.Holder) && !loading) {
			mc.gui.setScreen(new PotgCard.Holder());
		}
		mc.gui.toastManager().clear();
	}

	public static void onDisconnect() {
		active = false;
		endedAt = 0;
		PotgMusic.stop();
		PotgCard.hide();
	}
}
