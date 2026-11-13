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

	/** Before the play starts: the player's own body is held still (the world is frozen too). */
	public static boolean holdBody() {
		return active && net.minecraft.util.Util.getMillis() < cardUntil;
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(PotgPayload.TYPE, (payload, context) -> on(payload));
		ClientPlayNetworking.registerGlobalReceiver(HighlightsPayload.TYPE, (payload, context) -> HighlightsScreen.received(payload.data()));
		dev.overcraft.potg.ReplayTap.clientPaused = () -> Minecraft.getInstance().isPaused();
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
				giveUpAt = now + 90_000;
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
		// the server holds the world until every viewer's end screen is over (critique 6): this one's is
		if (mc.getConnection() != null && ClientPlayNetworking.canSend(HighlightsPayload.TYPE)) {
			ClientPlayNetworking.send(new HighlightsPayload(HighlightsPayload.DONE, ""));
		}
		PotgCard.hide();
		mc.getSoundManager().stop();
		mc.gui.toastManager().clear();
		if (mc.gui.screen() instanceof PotgCard.Holder) {
			mc.gui.setScreen(null);
		}
	}

	/** Each client tick: the look's clock, the holder back if a login's loading screen took it, the end, the safety end. */
	public static void tick(Minecraft mc) {
		if (!active) {
			while (HIGHLIGHTS.consumeClick()) {
				if (mc.player != null && mc.gui.screen() == null) {
					openHighlights();
				}
			}
			return;
		}
		PotgCard.tick(mc);
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
		PotgCard.hide();
	}
}
