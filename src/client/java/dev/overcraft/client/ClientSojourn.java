package dev.overcraft.client;

import dev.overcraft.Ow;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.sojourn.Sojourn;
import dev.overcraft.sojourn.SojournStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Sojourn on her own client: Power Slide moves her here (OwMovement.slide), the server starting it and ending it on time (the
 * synced mode's slide flag); her slide jump and a wall end it here first, and the server is told with her next input.
 */
public final class ClientSojourn {
	/** Ticks into her slide as this client moves her (-1 not sliding). */
	private static int slide = -1;
	/** This client ended the slide (its jump, a wall): no more slide until the server's flag goes off. */
	private static boolean ended;
	/** What to tell the server with the next input (EXTRA_JUMP / EXTRA_SLIDE_END), and when it happened (game time). */
	private static int tell;
	private static long toldAt = -1;

	private ClientSojourn() {
	}

	/** Each client tick: the slide follows the server's flag. */
	public static void tick(Minecraft mc) {
		boolean on = mc.player != null && ClientHero.hero() == Hero.SOJOURN && ClientHero.controls()
			&& (ClientHero.sync().mode() & Sojourn.MODE_SLIDE) != 0;
		if (!on) {
			ended = false;
			slide = -1;
		} else {
			slide = ended ? -1 : slide + 1;
			if (slide >= 0 && mc.player.isInWater()) {
				// into water it ends: Minecraft's swimming moves her there, and the slide would only have run out its time
				// standing still (found playing; ours, Overwatch has no water)
				ended(mc.player, false);
			}
		}
	}

	/** Her slide this tick for OwMovement, or null. */
	public static OwMovement.@Nullable Slide slide(LivingEntity entity) {
		// Dual Thrusters: a 0.75 s slide, its jump 13.75 up and 11 along (D 51EB #422, 1E6F #1031)
		boolean dual = ClientHero.sync().majorPerk() == dev.overcraft.hero.Perk.index(dev.overcraft.hero.Perk.SOJOURN_DUAL_THRUSTERS);
		int length = Ow.ticks(dual ? SojournStats.SLIDE_TIME_DUAL : SojournStats.SLIDE_TIME);
		if (entity != Minecraft.getInstance().player || ClientHero.hero() != Hero.SOJOURN || !ClientHero.controls() || slide < 0
			|| slide >= length) {
			return null;
		}
		return dual ? new OwMovement.Slide(slide, length, SojournStats.SLIDE_JUMP_UP_DUAL, SojournStats.SLIDE_JUMP_ALONG_DUAL)
			: new OwMovement.Slide(slide, length, SojournStats.SLIDE_JUMP_UP, SojournStats.SLIDE_JUMP_ALONG);
	}

	/** OwMovement ended it: her jump or a wall. */
	public static void ended(LivingEntity entity, boolean jumped) {
		if (entity != Minecraft.getInstance().player) {
			return;
		}
		ended = true;
		slide = -1;
		tell = jumped ? HeroInputPayload.EXTRA_JUMP : HeroInputPayload.EXTRA_SLIDE_END;
		toldAt = entity.level().getGameTime();
	}

	/** Her extra input: the slide's end, once. */
	public static int input(net.minecraft.client.player.LocalPlayer player) {
		int out = tell;
		tell = 0;
		// (not sent long after, as Juno's double jump: a screen holding her input back)
		return out != 0 && player.level().getGameTime() - toldAt <= 2 && ClientHero.hero() == Hero.SOJOURN ? out : 0;
	}
}
