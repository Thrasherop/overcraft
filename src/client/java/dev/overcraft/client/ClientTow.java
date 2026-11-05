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
 * client, where her graph has the node (the ally's context, H): their mover, gravity and keys go on as ever (OwMovement, or
 * Minecraft's walking out of hero mode) and the rope is added to their speed before the tick moves them. The force's shape is
 * ours (LifelineRope). Played on a bot's rope and on a floating anchor, never with a real second player: see
 * techniques/heroes/jetpack_cat.md.
 */
public final class ClientTow {
	private static boolean taut;
	/** Where she was seen, these last ticks: her speed over a quarter of a second, not from one bunch of position updates. */
	private static final java.util.ArrayDeque<Vec3> SEEN = new java.util.ArrayDeque<>();
	private static final int SEEN_TICKS = 5;
	/** Her fastest change of speed, m/s²: 7.75 x 5.5, her flight's acceleration in the recording (jetpack_cat.md, #60). */
	private static final double HER_ACCEL = 42.7;
	private static Vec3 her = Vec3.ZERO;
	/** Ours: a jump of her position past this in a tick (m) is a blink. A hitch of 0.25 s at her top speed is 5.8 m, at her 7.15 m/s 1.8 m. */
	private static final double BLINK = 4;

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
			SEEN.clear();
			her = Vec3.ZERO;
			return;
		}
		Vec3 herAt = cat.position();
		if (!SEEN.isEmpty() && herAt.distanceToSqr(SEEN.peekLast()) > BLINK * BLINK) {
			SEEN.clear(); // she's blinked: where she is counts, and this jump isn't a speed
		}
		// her speed from where she's seen: other players arrive by their positions, not their speeds
		Vec3 seen = SEEN.isEmpty() ? Vec3.ZERO : herAt.subtract(SEEN.peekFirst()).scale(20.0 / SEEN.size());
		// and no faster a change than she can make (a jump of her position is a hitch or a blink, not a speed): after a 6 m /tp this
		// took the towed player to 29 m/s and past her
		her = OwMovement.approach(her, seen, HER_ACCEL * 0.05);
		SEEN.addLast(herAt);
		if (SEEN.size() > SEEN_TICKS) {
			SEEN.removeFirst();
		}
		Vec3 v = player.getDeltaMovement().scale(20);
		Vec3 toHer = herAt.subtract(player.position());
		// past what the server lets a rope be (it's let go, or she's blinked: its word is on the way): no pull
		if (!LifelineRope.taut(toHer, JetpackCatStats.LIFELINE_ROPE)
			|| toHer.horizontalDistance() > JetpackCatStats.LIFELINE_ROPE * JetpackCatStats.LIFELINE_BREAK) {
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
