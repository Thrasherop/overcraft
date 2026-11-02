package dev.overcraft.client;

import dev.overcraft.hero.Hero;
import dev.overcraft.hero.Perk;
import dev.overcraft.juno.Juno;
import dev.overcraft.juno.JunoStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Juno on her own client: her Martian Overboots and Glide Boost move her here (OwMovement's airJumps / junoGlide), the
 * server starting and ending the glide (Juno: the synced mode's glide flag).
 */
public final class ClientJuno {
	/** Ticks into her glide as this client moves her (-1 not gliding). */
	private static int glide = -1;

	private ClientJuno() {
	}

	/** Each client tick: the glide follows the server's flag. */
	public static void tick(Minecraft mc) {
		boolean on = mc.player != null && ClientHero.hero() == Hero.JUNO && ClientHero.controls()
			&& (ClientHero.sync().mode() & Juno.MODE_GLIDE) != 0;
		glide = on ? glide + 1 : -1;
	}

	public static int glide(LivingEntity entity) {
		return entity == Minecraft.getInstance().player && ClientHero.hero() == Hero.JUNO && ClientHero.controls() ? glide : -1;
	}

	/** Her jumps in the air: one, two with Lift Off; none for anyone else. */
	public static int airJumps(LivingEntity entity) {
		if (entity != Minecraft.getInstance().player || ClientHero.hero() != Hero.JUNO || !ClientHero.controls()) {
			return 0;
		}
		return Perk.byIndex(ClientHero.sync().majorPerk()) == Perk.JUNO_LIFT_OFF ? JunoStats.AIR_JUMPS_LIFT_OFF : JunoStats.AIR_JUMPS;
	}
}
