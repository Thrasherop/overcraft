package dev.overcraft.comms;

import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Voice;
import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The communication wheel's callouts, for any hero (comms-plan.md). A callout is a chat line to the caller's teammates
 * (Allies: the scoreboard team, or every player when there are no teams) at any distance, and the hero's voice line
 * in the world around them (Voice), as the owner asked (2026-10-26). Too many in a row and the game says so, as
 * Overwatch does ("Too many messages! Please wait a few seconds...").
 */
public final class Comms {
	/** Team chat's light blue (the owner's screenshot: Overwatch's team lines, measured about #4FC6F5). */
	public static final int TEAM_BLUE = 0x4FC6F5;
	/** At most this many callouts in SPAM_WINDOW ticks (ours: Overwatch gives no numbers). */
	private static final int SPAM_LIMIT = 4;
	private static final int SPAM_WINDOW = 160;

	private static final Map<UUID, Deque<Long>> RECENT = new HashMap<>();

	private Comms() {
	}

	/** Counts a message against the spam limit; false (and the warning) when there have been too many. */
	private static boolean allowed(ServerPlayer player) {
		long now = player.level().getGameTime();
		Deque<Long> recent = RECENT.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
		while (!recent.isEmpty() && now - recent.peekFirst() > SPAM_WINDOW) {
			recent.pollFirst();
		}
		if (recent.size() >= SPAM_LIMIT) {
			player.sendSystemMessage(Component.translatable("overcraft.callout.too_many").withColor(TEAM_BLUE));
			return false;
		}
		recent.addLast(now);
		return true;
	}

	/**
	 * One of the hero's voice lines (the wheel's voice page): the line itself, heard in the world around the hero and in
	 * their own head (HeroSounds), no chat line. The lines come from the player's local pack ("<hero>.voiceline.<id>"):
	 * silent where a listener's pack doesn't have it.
	 */
	public static void voiceLine(ServerPlayer player, String id) {
		HeroState state = HeroManager.state(player);
		if (state == null || state.hero == null || !id.matches("[a-z0-9_]{1,48}") || !allowed(player)) {
			return;
		}
		Identifier sound = Overcraft.id(state.hero.id + ".voiceline." + id);
		HeroSounds.fromHero(player, SoundEvent.createVariableRangeEvent(sound), 2);
	}

	/**
	 * One of the hero's emotes (the wheel's emote page): everyone who can see the hero (and the hero) plays it, as an
	 * action ("emote:<id>"); it ends on their screens when the hero moves or does anything else (HeroAnimator).
	 */
	public static void emote(ServerPlayer player, String id) {
		HeroState state = HeroManager.state(player);
		if (state != null && state.hero != null && id.matches("[a-z0-9_]{1,40}") && player.isAlive()) {
			dev.overcraft.hero.Combat.animate(player, "emote:" + id);
		}
	}

	/** The hero's emote, if one's playing, ends on every screen ("emote:" with no id): switching modes with G. */
	public static void endEmote(ServerPlayer player) {
		dev.overcraft.hero.Combat.animate(player, "emote:");
	}

	public static void callout(ServerPlayer player, Callout callout) {
		if (!allowed(player)) {
			return;
		}

		HeroState state = HeroManager.state(player);
		Component text = Component.translatable("overcraft.callout." + callout.id);
		String moment = "call_" + callout.id;
		if (callout == Callout.ULTIMATE_STATUS && state != null && state.hero != null) {
			// Overwatch's three: charging, almost ready (90%, ours), ready; the chat line gives the charge
			int percent = (int) Math.floor(100 * state.ultCharge / Combat.ultCost(state.hero));
			moment = percent >= 100 ? "ult_ready" : percent >= 90 ? "call_ult_almost" : "call_ult_charging";
			text = percent >= 100 ? Component.translatable("overcraft.callout.ultimate_ready")
				: Component.translatable("overcraft.callout.ultimate_charge", Math.min(99, percent));
		}
		if (state != null && state.hero != null) {
			Voice.say(player, moment, 1, false);
		}
		MutableComponent line = Component.empty().append(player.getDisplayName());
		if (state != null && state.hero != null) {
			line.append(" (" + state.hero.displayName + ")");
		}
		line.append(": ").append(text);
		Component message = line.withColor(TEAM_BLUE);
		for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
			if (other == player || Allies.isAlly(player, other)) {
				other.sendSystemMessage(message);
			}
		}
	}

	public static void forget(ServerPlayer player) {
		RECENT.remove(player.getUUID());
	}
}
