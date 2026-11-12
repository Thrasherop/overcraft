package dev.overcraft.client;

import dev.overcraft.client.hud.PotgCard;
import dev.overcraft.net.PotgPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Play of the Game, the client's half (techniques/potg/design.md): from the server's CARD (or SOLO, a highlight watched
 * alone) to its END, this client is in a showing. The server's replay engine takes the player out of the world and logs
 * them into the replay and back again, so the mod's own join-time reactions are held off meanwhile: no Hero Select, no
 * asset-pack line, no toasts (the spike: Hero Select opened on the replay's login and again on return). The card's
 * screen (PotgCard.Holder) is kept open the whole time: it takes every key and click (owner: nobody skips) and keeps the
 * pause menu from opening when the window loses focus, which would hold a single player's replay stream.
 */
public final class ClientPotg {
	private ClientPotg() {
	}

	private static boolean active;
	/** Wall clock: the card's end (the body held still till then), and a safety end if the END never comes. */
	private static long cardUntil;
	private static long giveUpAt;

	public static boolean active() {
		return active;
	}

	/** The card shows: the player's own body is held still under it (the world is frozen too). */
	public static boolean holdBody() {
		return active && net.minecraft.util.Util.getMillis() < cardUntil;
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(PotgPayload.TYPE, (payload, context) -> on(payload));
		dev.overcraft.potg.ReplayTap.clientPaused = () -> Minecraft.getInstance().isPaused();
	}

	private static void on(PotgPayload p) {
		Minecraft mc = Minecraft.getInstance();
		long now = net.minecraft.util.Util.getMillis();
		switch (p.phase()) {
			case PotgPayload.CARD -> {
				active = true;
				cardUntil = now + Math.round(p.cardSeconds() * 1000);
				giveUpAt = now + 60_000;
				mc.getSoundManager().stop();
				PotgCard.show(p.player(), p.hero(), p.category(), p.cardSeconds());
				mc.gui.setScreen(new PotgCard.Holder());
			}
			case PotgPayload.SOLO -> {
				// the hero's intro goes here (owner, 2026-11-11): the next step
				active = true;
				cardUntil = now;
				giveUpAt = now + 60_000;
				mc.getSoundManager().stop();
				mc.gui.setScreen(new PotgCard.Holder());
			}
			default -> finish(mc);
		}
	}

	private static void finish(Minecraft mc) {
		active = false;
		PotgCard.hide();
		mc.getSoundManager().stop();
		mc.gui.toastManager().clear();
		if (mc.gui.screen() instanceof PotgCard.Holder) {
			mc.gui.setScreen(null);
		}
	}

	/** Each client tick: the holder screen back if a login's loading screen took it, and the safety end. */
	public static void tick(Minecraft mc) {
		if (!active) {
			return;
		}
		if (net.minecraft.util.Util.getMillis() > giveUpAt) {
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
