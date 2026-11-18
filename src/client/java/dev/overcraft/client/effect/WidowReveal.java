package dev.overcraft.client.effect;

import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Widowmaker's reveals on this client (#138): what the server says this player sees through walls (widow_reveal, the variant its
 * ticks): her Venom Mine's poisoned, to her (D 0057 #62, effect 02FF); everyone her Infra-Sight finds, to her side (D 0067 #13 / #38).
 * Drawn as Minecraft's outline in red (PingOutlineMixin / PingOutlineColourMixin, as a ping's), through walls. Overwatch also shows
 * their health bars; ours: the outline only.
 */
public final class WidowReveal {
	/** Infra-Sight's and the mine's red (ours: Overwatch's enemy red, as the pinged enemy's). */
	public static final int RED = 0xFF2A2A;
	/** Entity id -> until (game time). */
	private static final Map<Integer, Long> UNTIL = new HashMap<>();

	private WidowReveal() {
	}

	static boolean dispatch(EffectPayload e) {
		if (!e.effect().equals("widow_reveal")) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && e.entity() >= 0) {
			UNTIL.merge(e.entity(), mc.level.getGameTime() + Math.max(1, e.variant()), Math::max);
		}
		return true;
	}

	/** The outline's colour on this entity, or -1 when it isn't revealed to this player. */
	public static int outlineColour(Entity entity) {
		if (UNTIL.isEmpty()) {
			return -1;
		}
		Long until = UNTIL.get(entity.getId());
		Minecraft mc = Minecraft.getInstance();
		if (until == null || mc.level == null) {
			return -1;
		}
		if (mc.level.getGameTime() > until || !entity.isAlive()) {
			UNTIL.remove(entity.getId());
			return -1;
		}
		return RED;
	}

	static void clear() {
		UNTIL.clear();
	}
}
