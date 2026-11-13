package dev.overcraft.client;

import dev.overcraft.Ow;
import dev.overcraft.client.controls.HeroControls;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.Perk;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.sigma.Sigma;
import dev.overcraft.sigma.SigmaStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sigma on his own client: Levitation (major perk, 51E0 #370-#383): the jump held in the air from 0.2 s after leaving the
 * ground lifts him (OwMovement) for up to 1.75 s, once a landing, ending when let go; the server is told it began (its
 * landing is safe, #35). Not in his Gravitic Flux (he flies there).
 */
public final class ClientSigma {
	private static int airTicks;
	private static boolean used;
	/** Ticks into Levitation (-1: not levitating). */
	private static int lifting = -1;
	private static int tell;

	private ClientSigma() {
	}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		boolean perk = player != null && ClientHero.hero() == Hero.SIGMA && ClientHero.controls()
			&& ClientHero.sync().majorPerk() == Perk.index(Perk.SIGMA_LEVITATION)
			&& (ClientHero.sync().mode() & Sigma.MODE_FLUX_FLIGHT) == 0;
		if (!perk || player.onGround() || player.isInWater() || player.onClimbable()) {
			airTicks = 0;
			lifting = -1;
			if (player == null || player.onGround() || player.isInWater() || player.onClimbable()) {
				used = false;
			}
			return;
		}
		airTicks++;
		boolean jump = HeroControls.get(HeroControls.Action.JUMP).isDown();
		if (lifting >= 0) {
			if (!jump || ++lifting >= Ow.ticks(SigmaStats.LEVITATION_TIME)) {
				lifting = -1;
			}
		} else if (!used && jump && airTicks > Ow.ticks(SigmaStats.LEVITATION_FROM)) {
			lifting = 0;
			used = true;
			tell = HeroInputPayload.EXTRA_JUMP;
		}
	}

	/** Levitation lifts him this tick (OwMovement.levitating). */
	public static boolean levitating(LivingEntity entity) {
		return entity == Minecraft.getInstance().player && lifting >= 0;
	}

	/** His extra input: Levitation began, once. */
	public static int input(LocalPlayer player) {
		int out = tell;
		tell = 0;
		return ClientHero.hero() == Hero.SIGMA ? out : 0;
	}
}
