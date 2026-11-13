package dev.overcraft.client;

import dev.overcraft.entity.RipTireEntity;
import dev.overcraft.hero.Hero;
import dev.overcraft.junkrat.Junkrat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Junkrat on his own client. The server drives RIP-Tire from his keys and facing (Junkrat.driveTire); here his walk and jump are
 * off while it runs (inputsOff: D 0232 #476 zeroes his movement inputs) and the view follows the tire from behind (CameraMixin, W:
 * "view on the tire"), the mouse still turning him, which turns the tire.
 */
public final class ClientJunkrat {
	/** How far behind the tire the view sits, and how high over it (ours: the data's tire camera isn't read). */
	public static final float TIRE_VIEW_BACK = 3.5f, TIRE_VIEW_UP = 1.2f;
	private static int tireId = -1;

	private ClientJunkrat() {
	}

	public static void reset() {
		tireId = -1;
	}

	/** Whether the server says his tire is out (the synced mode). */
	public static boolean onTire() {
		return ClientHero.hero() == Hero.JUNKRAT && ClientHero.controls() && (ClientHero.sync().mode() & Junkrat.MODE_TIRE) != 0;
	}

	/**
	 * His walk and jump off while the tire runs (D 0232 #476 zeroes his movement inputs; KeyboardInputMixin, OwMovement.noJump): only
	 * the inputs, his body still under gravity, so a tire cast in mid-air leaves him falling where he was (critic, 2026-11-13: held
	 * whole, he hung in the air for the tire's 10 s and took the fall after). His keys still reach the server, which drives the tire.
	 */
	public static boolean inputsOff(LivingEntity entity) {
		return entity == Minecraft.getInstance().player && onTire();
	}

	/** His tire here, if this client has it. */
	public static @Nullable RipTireEntity tire() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || !onTire()) {
			return null;
		}
		Entity known = tireId >= 0 ? mc.level.getEntity(tireId) : null;
		if (known instanceof RipTireEntity t && t.isAlive() && t.ownerId() == mc.player.getId()) {
			return t;
		}
		tireId = -1;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof RipTireEntity t && t.isAlive() && t.ownerId() == mc.player.getId()) {
				tireId = t.getId();
				return t;
			}
		}
		return null;
	}
}
