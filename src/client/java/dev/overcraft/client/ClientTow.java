package dev.overcraft.client;

import dev.overcraft.client.effect.JetpackCatFx;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.jetpackcat.JetpackCatStats;
import dev.overcraft.jetpackcat.LifelineRope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The local player on a Jetpack Cat's Lifeline (#89). A player moves themselves, so the rope pulls them here, on their own
 * client, as her graph runs it in the ally's context: their mover, gravity and keys go on as ever (OwMovement, or Minecraft's
 * walking out of hero mode) and the rope is added to their speed before the tick moves them. UNVERIFIED on a second player
 * so far: see techniques/heroes/jetpack_cat.md.
 */
public final class ClientTow {
	private static boolean taut;
	private static Vec3 herLast;

	private ClientTow() {
	}

	/** The rope is pulling the local player this tick. */
	public static boolean taut() {
		return taut;
	}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		boolean was = taut;
		taut = false;
		if (player == null || mc.level == null || mc.isPaused()) {
			return;
		}
		int catId = JetpackCatFx.towedBy(player);
		Entity cat = catId < 0 ? null : mc.level.getEntity(catId);
		if (cat == null) {
			herLast = null;
			return;
		}
		Vec3 herAt = cat.position();
		// her speed from where she's seen: other players arrive by their positions, not their speeds
		Vec3 her = herLast == null ? Vec3.ZERO : herAt.subtract(herLast).scale(20);
		herLast = herAt;
		Vec3 v = player.getDeltaMovement().scale(20);
		Vec3 toHer = herAt.subtract(player.position());
		if (!LifelineRope.taut(toHer, JetpackCatStats.LIFELINE_ROPE)) {
			return;
		}
		taut = true;
		double dt = 0.05 / OwMovement.SUBSTEPS;
		for (int i = 0; i < OwMovement.SUBSTEPS; i++) {
			v = LifelineRope.pull(toHer.subtract(v.subtract(her).scale(i * dt)), her, v, JetpackCatStats.LIFELINE_ROPE, dt);
		}
		player.setDeltaMovement(v.scale(0.05));
		player.resetFallDistance();
	}
}
