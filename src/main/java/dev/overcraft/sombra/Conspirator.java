package dev.overcraft.sombra;

import dev.overcraft.hero.Allies;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.net.ConspiratorPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/**
 * Conspirator, her passive (BZ 2026-10-06; W): "fully charged enemy ultimates are shown to her", an ult icon above those
 * enemies. Her graphs don't read it (sombra-map.md: the engine's or the UI's), so ours: twice a second the server sends
 * her the enemy players in her world whose ultimate is ready, in hero mode, and her HUD draws their ult's icon over their
 * heads, through walls, while they're loaded on her side (ClientConspirator). Mobs have no ultimates.
 */
public final class Conspirator {
	private static final int EVERY = 10;

	private Conspirator() {
	}

	public static void tick(ServerPlayer player, HeroState s) {
		if (player.level().getGameTime() % EVERY != 0) {
			return;
		}
		int[] ready = player.level().players().stream()
			.filter(other -> Allies.isEnemy(player, other) && ultReady(other))
			.mapToInt(ServerPlayer::getId).sorted().toArray();
		send(player, s, ready);
	}

	/** Leaving Sombra: her markers go. */
	public static void clear(ServerPlayer player, HeroState s) {
		send(player, s, new int[0]);
	}

	private static boolean ultReady(ServerPlayer other) {
		HeroState o = HeroManager.state(other);
		return o != null && o.hero != null && HeroManager.heroMode(other) && o.ultCharge >= dev.overcraft.hero.Combat.ultCost(o.hero);
	}

	private static void send(ServerPlayer player, HeroState s, int[] ready) {
		if (!Arrays.equals(ready, s.conspiracy)) {
			s.conspiracy = ready;
			if (ServerPlayNetworking.canSend(player, ConspiratorPayload.TYPE)) {
				ServerPlayNetworking.send(player, new ConspiratorPayload(ready));
			}
		}
	}
}
