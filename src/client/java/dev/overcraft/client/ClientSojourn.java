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
	/** The art generation her effects were last made ahead for another Sojourn in the world. */
	private static int othersArtMade = -1;

	private ClientSojourn() {
	}

	/** Each client tick: the slide follows the server's flag. */
	public static void tick(Minecraft mc) {
		if (!hearing) {
			hearing = true;
			net.minecraft.resources.Identifier others = dev.overcraft.Overcraft.id("sojourn.railgun.reload_3p");
			mc.getSoundManager().addListener((sound, events, range) -> {
				if (sound.getIdentifier().equals(others)) {
					OTHERS_RELOADS.add(sound);
				}
			});
		}
		OTHERS_RELOADS.removeIf(s -> !mc.getSoundManager().isActive(s));
		if (stopReloadTicks > 0) {
			stopReloadTicks--;
			mc.getSoundManager().stop(dev.overcraft.Overcraft.id("sojourn.railgun.reload"), null);
		}
		// (KeyMapping.set clears it as the key goes up: this is for a release something else ate, an overlay's)
		fireDown &= dev.overcraft.client.controls.HeroControls.get(dev.overcraft.client.controls.HeroControls.Action.PRIMARY_FIRE).isDown();
		int art = dev.overcraft.client.effect.EffectArt.generation();
		if (ClientHero.hero() == Hero.SOJOURN && artMade != art) {
			// her art made as she's picked, not in the frame of her first shot (EffectArt.prepare)
			artMade = art;
			dev.overcraft.client.effect.EffectArt.prepare(dev.overcraft.client.effect.SojournFx.ART);
		}
		if (othersArtMade != art && mc.level != null && mc.level.getGameTime() % 20 == 0) {
			// another Sojourn in the world: this client's first sight of her first shot cost 209 ms as an ally, 153 as an enemy
			for (net.minecraft.client.player.AbstractClientPlayer other : mc.level.players()) {
				if (other != mc.player && Hero.SOJOURN.id.equals(other.getAttached(dev.overcraft.hero.HeroManager.HERO))) {
					othersArtMade = art;
					dev.overcraft.client.effect.EffectArt.prepare(dev.overcraft.client.effect.SojournFx.ART);
					dev.overcraft.client.effect.EffectArt.prepare(dev.overcraft.client.effect.SojournFx.ART_ENEMY);
					break;
				}
			}
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
		if (ClientHero.hero() != Hero.SOJOURN) {
			return;
		}
		// the first of a press only: KeyboardHandler clicks again on the OS's repeat of a held key, and that was a shot every 0.65 s. (Q's repeats aren't filtered:
		// ClientHero.drain sends them as presses, so one can start Overclock)
		boolean fire = bound(dev.overcraft.client.controls.HeroControls.Action.PRIMARY_FIRE).equals(key), ult = bound(dev.overcraft.client.controls.HeroControls.Action.ULTIMATE).equals(key);
		if (fire && fireDown) {
			dev.overcraft.hero.PressTrace.mark("refused");
			return;
		}
		fireDown |= fire;
		Minecraft mc = Minecraft.getInstance();
		net.minecraft.client.player.LocalPlayer player = mc.player;
		if (player == null || mc.level == null || !ClientHero.controls() || mc.gui.screen() != null
			|| ClientHero.perkMenuOpen() || dev.overcraft.client.comms.CommsWheel.holding()
			|| Boolean.TRUE.equals(player.getAttached(dev.overcraft.hero.HeroManager.ASLEEP))) {
			return;
		}
		if (ult) {
			ultPressed();
		}
		if (!fire) {
			return;
		}
		double energy = ClientHero.sync().meter() * SojournStats.ENERGY_MAX;
		long now = System.nanoTime();
		if (energy <= 0) {
			return;
		}
		if (now < lockedUntil) {
			if (lockedUntil - now <= CALL_WINDOW_NANOS) {
				// late in its recovery: the server's buffer keeps this press and fires it as the recovery ends, and that shot only reaches her a
				// round trip on: a click before then must not play a shot the server will drop. Sent now, so the window is the server's to the tick
				ClientHero.pressNow(0, 0, 0, 1);
				serverMayFireUntil = lockedUntil + roundTripNanos(mc, player);
			}
			return;
		}
		if (now < serverMayFireUntil || dev.overcraft.client.controls.HeroControls.get(dev.overcraft.client.controls.HeroControls.Action.ULTIMATE).isDown()) {
			// the same, with that shot still on its way: this press is the server's to judge. Or Q is held: a repeat may have started Overclock before this
			// client can know her ult is ready, and its cast would drop this shot
			ClientHero.pressNow(0, 0, 0, 1);
			serverMayFireUntil = now + roundTripNanos(mc, player);
			return;
		}
		locked(SojournStats.CHARGED_LOCKOUT);
		// the mouse's movement this frame hasn't turned her yet: Minecraft.runTick applies it after the events and the tick, and a click in
		// the same poll as a flick read the old aim (4 of 10). Applied now, in the events' order; the frame's own call finds what's left
		mc.mouseHandler.handleAccumulatedMovement();
		// with her aim and where between her last two positions she saw from: the camera is drawn between ticks, the server's position is the tick's: 0.7 m at a
		// slide, 3 of 10 perfect flicks missed. The server works out the place itself, on her own path (Railgun.pressedShown)
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		net.minecraft.world.phys.Vec3 eye = player.getEyePosition(partial);
		dev.overcraft.hero.PressTrace.mark(String.format(java.util.Locale.ROOT, "click %.3f %.3f %.3f %.2f %.2f", eye.x, eye.y, eye.z, player.getYRot(), player.getXRot()));
		shownAt = System.nanoTime();
		ClientHero.pressNow(HeroInputPayload.PRESS_CHARGED_SHOWN, player.getYRot(), player.getXRot(), partial);
		show(mc, player, energy, eye);
	}

	/** How near its end a press in the recovery is kept: the server's 0.15 s buffer, and the tick it may wait to be looked at (ours; measured in sojourn.md). */
	private static final long CALL_WINDOW_NANOS = 200_000_000;
	/** Beyond the ping: the server's tick and a frame here (ours). */
	private static final long SHOT_ON_ITS_WAY_NANOS = 150_000_000;

	/**
	 * From now: how long a shot the server fires now takes to reach her. Measured on her own shots (Railgun's "charged_ack"): vanilla's list ping is too slow to lean
	 * on alone: the server sends it every 30 s, from a keepalive every 15 s averaged 3 to 1, and just after joining it's the login's figure. A spike can still show a shot it drops.
	 */
	private static long roundTripNanos(Minecraft mc, net.minecraft.client.player.LocalPlayer player) {
		var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(player.getUUID());
		return Math.max((info == null ? 0 : info.getLatency()) * 1_000_000L, measuredRoundTrip) + SHOT_ON_ITS_WAY_NANOS;
	}

	/** The server fired a press she'd shown, at once: a round trip's measure. */
	public static void acked() {
		if (shownAt != 0) {
			long rtt = System.nanoTime() - shownAt;
			measuredRoundTrip = measuredRoundTrip == 0 ? rtt : (measuredRoundTrip * 3 + rtt) / 4;
			shownAt = 0;
		}
	}

	/** A key went up (KeyMapping.set): the next click on it is a press, whenever the next tick is. */
	public static void released(com.mojang.blaze3d.platform.InputConstants.Key key) {
		if (fireDown && bound(dev.overcraft.client.controls.HeroControls.Action.PRIMARY_FIRE).equals(key)) {
			fireDown = false;
		}
	}

	private static long shownAt;
	private static long measuredRoundTrip;
	/** The right button is down, as far as a press goes (its OS repeats aren't presses). */
	private static boolean fireDown;
	/** A Q lock's end: a repeat inside it doesn't stretch it. */
	private static long ultArmedUntil;

	/** Until when (nanoTime) the server may be about to fire a press this client didn't show. */
	private static long serverMayFireUntil;

	/** Q as it goes down, if her ult is ready: no shot through its cast, from now, not a round trip on (ghost shots at a 100 ms dev lag). */
	private static void ultPressed() {
		var sync = ClientHero.sync();
		// (hacked, the server strips the press: HeroManager.onInput)
		if (sync.ultCharge() >= sync.ultCost() && !sync.ultActive() && (sync.mode() & Sojourn.MODE_SILENCED) == 0 && System.nanoTime() >= ultArmedUntil) {
			locked(SojournStats.OVERCLOCK_CAST);
			ultArmedUntil = lockedUntil;
		}
	}

	private static com.mojang.blaze3d.platform.InputConstants.Key bound(dev.overcraft.client.controls.HeroControls.Action action) {
		return net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(dev.overcraft.client.controls.HeroControls.get(action));
	}

	/** She can't fire it for this long from now (s). A shot from the server has arrived: nothing more is on its way. */
	public static void locked(double seconds) {
		lockedUntil = System.nanoTime() + (long) (seconds * 1.0e9);
		serverMayFireUntil = 0;
	}

	private static void show(Minecraft mc, net.minecraft.client.player.LocalPlayer player, double energy, net.minecraft.world.phys.Vec3 eye) {
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		dev.overcraft.client.model.HeroAnimator.trigger(player.getId(), "charged_shot", (mc.level.getGameTime() + partial) / 20f);
		ClientHero.playOwn(dev.overcraft.sojourn.SojournSounds.of(dev.overcraft.sojourn.Railgun.chargedFireSound(energy)).location(), 1, 1,
			player.getRandom().nextLong());
		net.minecraft.world.phys.Vec3 look = player.getViewVector(1);
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

	/** Others' reloads sounding here, to stop the right one: a sound by id would stop every Sojourn's. */
	private static final java.util.List<net.minecraft.client.resources.sounds.SoundInstance> OTHERS_RELOADS = new java.util.ArrayList<>();
	private static boolean hearing;
	private static int stopReloadTicks;

	/**
	 * The shot cut a reload on her own gun or another's: its sound stops with its clips (HeroAnimator.trigger). Her own is hers alone. Of
	 * others', the one nearest this Sojourn: the server's packet says where, not whose (ours; two side by side could swap).
	 */
	public static void reloadCut(int entityId, boolean own) {
		Minecraft mc = Minecraft.getInstance();
		if (own) {
			mc.getSoundManager().stop(dev.overcraft.Overcraft.id("sojourn.railgun.reload"), null);
			// and again: a sound just started gets its buffer on the sound thread, and a stop ahead of that lets it play on (1 of 9 at a 100 ms dev lag)
			stopReloadTicks = 3;
			return;
		}
		net.minecraft.world.entity.Entity shooter = mc.level == null ? null : mc.level.getEntity(entityId);
		OTHERS_RELOADS.removeIf(s -> !mc.getSoundManager().isActive(s));
		if (shooter != null) {
			OTHERS_RELOADS.stream().min(java.util.Comparator.comparingDouble(s -> shooter.distanceToSqr(s.getX(), s.getY(), s.getZ()))).ifPresent(s -> {
				mc.getSoundManager().stop(s);
				OTHERS_RELOADS.remove(s);
			});
		}
	}

	/** Her extra input: the slide's end, once. */
	public static int input(net.minecraft.client.player.LocalPlayer player) {
		int out = tell;
		tell = 0;
		// (not sent long after, as Juno's double jump: a screen holding her input back)
		return out != 0 && player.level().getGameTime() - toldAt <= 2 && ClientHero.hero() == Hero.SOJOURN ? out : 0;
	}
}
