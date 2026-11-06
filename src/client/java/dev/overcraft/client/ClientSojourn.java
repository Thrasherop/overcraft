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
	/** The art generation her effects were last made ahead for. */
	private static int artMade = -1;

	private ClientSojourn() {
	}

	/** Each client tick: the slide follows the server's flag. */
	public static void tick(Minecraft mc) {
		if (ClientHero.hero() == Hero.SOJOURN && artMade != dev.overcraft.client.effect.EffectArt.generation()) {
			// her art made as she's picked, not in the frame of her first shot (EffectArt.prepare)
			artMade = dev.overcraft.client.effect.EffectArt.generation();
			dev.overcraft.client.effect.EffectArt.prepare(dev.overcraft.client.effect.SojournFx.ART);
		}
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

	// ---- the Charged Shot on the press (#126) ----

	/** Until when (nanoTime) she can't fire it: its recovery, or Overclock's cast, as this client knows them. */
	private static long lockedUntil;

	/**
	 * A key went down. On her right button, the shot leaves now: "firing railgun is the highest priority ... with 0 cast time" (the
	 * owner, #126; W "0 second startup"). Her sounds, its look and clip start here, on the press, as the press goes to the server with
	 * her aim of this moment; the server does the damage and tells the others (Railgun.pressedShown). A press this client can't call
	 * (no energy, its recovery) goes the usual way at the next tick (ClientHero), and the server decides.
	 */
	public static void clicked(com.mojang.blaze3d.platform.InputConstants.Key key) {
		Minecraft mc = Minecraft.getInstance();
		net.minecraft.client.player.LocalPlayer player = mc.player;
		if (player == null || mc.level == null || ClientHero.hero() != Hero.SOJOURN || !ClientHero.controls() || mc.gui.screen() != null
			|| ClientHero.perkMenuOpen() || dev.overcraft.client.comms.CommsWheel.holding()
			|| Boolean.TRUE.equals(player.getAttached(dev.overcraft.hero.HeroManager.ASLEEP))
			|| !net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(
				dev.overcraft.client.controls.HeroControls.get(dev.overcraft.client.controls.HeroControls.Action.PRIMARY_FIRE)).equals(key)) {
			return;
		}
		double energy = ClientHero.sync().meter() * SojournStats.ENERGY_MAX;
		if (energy <= 0 || System.nanoTime() < lockedUntil) {
			return;
		}
		locked(SojournStats.CHARGED_LOCKOUT);
		// with her aim of this moment: the server's copy may be a tick old, and she may have just flicked onto the target
		ClientHero.pressNow(HeroInputPayload.PRESS_CHARGED_SHOWN, player.getYRot(), player.getXRot());
		show(mc, player, energy);
	}

	/** She can't fire it for this long from now (s). */
	public static void locked(double seconds) {
		lockedUntil = System.nanoTime() + (long) (seconds * 1.0e9);
	}

	private static void show(Minecraft mc, net.minecraft.client.player.LocalPlayer player, double energy) {
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		dev.overcraft.client.model.HeroAnimator.trigger(player.getId(), "charged_shot", (mc.level.getGameTime() + partial) / 20f);
		ClientHero.playOwn(dev.overcraft.sojourn.SojournSounds.of(dev.overcraft.sojourn.Railgun.chargedFireSound(energy)).location(), 1, 1,
			player.getRandom().nextLong());
		net.minecraft.world.phys.Vec3 eye = player.getEyePosition(), look = player.getViewVector(1);
		// (60 m: Railgun's copy for everyone, HeroSounds.at's volume)
		mc.level.playLocalSound(eye.x, eye.y, eye.z, dev.overcraft.sojourn.SojournSounds.of("charged.shot"), net.minecraft.sounds.SoundSource.PLAYERS,
			Math.max(1, 60f / 16), 1, false);
		// the same line as the server's: its blocks stop it (Railgun.chargedShot)
		net.minecraft.world.phys.Vec3 end = eye.add(look.scale(SojournStats.CHARGED_RANGE));
		var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE, player));
		net.minecraft.world.phys.Vec3 stop = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : hit.getLocation();
		net.minecraft.world.phys.Vec3 dir = stop.subtract(eye);
		dev.overcraft.client.effect.KirikoFx.play(new dev.overcraft.net.EffectPayload("sojourn_tracer", eye.x, eye.y, eye.z, (float) dir.x, (float) dir.y,
			(float) dir.z, player.getId(), (int) Math.round(energy), false));
	}

	/** The shot cut a reload on {@code own} or another's gun: its sound stops with its clips (HeroAnimator.trigger). */
	public static void reloadCut(boolean own) {
		Minecraft.getInstance().getSoundManager().stop(dev.overcraft.Overcraft.id(own ? "sojourn.railgun.reload" : "sojourn.railgun.reload_3p"), null);
	}

	/** Her extra input: the slide's end, once. */
	public static int input(net.minecraft.client.player.LocalPlayer player) {
		int out = tell;
		tell = 0;
		// (not sent long after, as Juno's double jump: a screen holding her input back)
		return out != 0 && player.level().getGameTime() - toldAt <= 2 && ClientHero.hero() == Hero.SOJOURN ? out : 0;
	}
}
