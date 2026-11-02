package dev.overcraft.client;

import dev.overcraft.client.effect.JunoFx;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.juno.JunoStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Juno's sounds that follow a state (tools/soundmaps/juno.txt; tools/reference/juno-sounds.md), for every Juno this client
 * sees: her own copies in her head, the "_3p" ones on her for everyone else, at the map's ranges.
 * <ul>
 * <li>Martian Overboots: her own double jump (53EB) as her movement makes it, and the hover's start, loop and end (5C0F,
 * 5D29, 5C10), seen from her motion by HeroAnimator's rule (a fall held at 3 m/s for 3 ticks);</li>
 * <li>Glide Boost (47B7: 59F5, its start, loop and Stop; 5843 with 0.4 s left, the others' copy 3.0 s in) from the synced
 * glide;</li>
 * <li>Pulsar Torpedoes' lock (520F: its one-shot, loop and, for others, its Stop) from the synced lock, and each torpedo's
 * flight loop at it (5350 / 5A5B);</li>
 * <li>the Hyper Ring's hum at it while it lasts (5A5C), the Orbital Ray's hum at it for its 8 s (54F7, built as one long
 * file), and on this client's player while inside a ray its loop (7AFA) and the damage amp's start, loop and end (54A6).</li>
 * </ul>
 * The one-shots of her kit come from the server (dev.overcraft.juno.JunoSounds). Each loop's file is a loop length long and
 * starts again as it ends; everything stops with what it follows (her death, another hero, leaving the world).
 */
public final class JunoSounds {
	/** Per Juno (entity id): her loops by name, and what was seen of her last tick. */
	private static final class Her {
		final Map<String, SoundInstance> playing = new HashMap<>();
		boolean gliding, locking, endingSaid, hovering;
		int glideTicks, hoverTicks, offTicks;
		/** Her last heights (5 ticks, a ring), for another player's fall over 4 ticks. */
		final double[] ys = new double[5];
		int yAt;
		float hoverEndAt = -100;
	}

	private static final Map<Integer, Her> HERS = new HashMap<>();
	/** The loops on what she sends flying, by JunoFx's key ("torpedo/<key>", "ring/<key>", "ray/<key>"). */
	private static final Map<String, SoundInstance> FLYING = new HashMap<>();
	/** This client's player's loops inside a ray, and whether they were inside last tick (the amp's start and end). */
	private static final Map<String, SoundInstance> MINE = new HashMap<>();
	private static boolean inRay;

	private JunoSounds() {
	}

	/** Each client tick (after JunoFx's, which moves what flies). */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			HERS.values().forEach(h -> stopAll(mc, h.playing));
			HERS.clear();
			stopAll(mc, FLYING);
			stopAll(mc, MINE);
			inRay = false;
			return;
		}
		Set<Integer> seen = new HashSet<>();
		for (Player p : mc.level.players()) {
			if (!"juno".equals(p.getAttached(HeroManager.HERO)) || !p.isAlive() || p.isSpectator()) {
				continue;
			}
			seen.add(p.getId());
			her(mc, p, HERS.computeIfAbsent(p.getId(), id -> new Her()));
		}
		HERS.entrySet().removeIf(e -> {
			if (!seen.contains(e.getKey())) {
				stopAll(mc, e.getValue().playing);
				return true;
			}
			return false;
		});
		flying(mc);
		ray(mc);
	}

	private static void her(Minecraft mc, Player p, Her her) {
		boolean own = p == mc.player;
		float now = p.level().getGameTime() / 20f;
		// Glide Boost: 59F5 held for the glide (B10F hers, B924 others' 60 m): its one-shot sets, its loop; its Stop event
		// as it ends cuts both and plays the end set
		boolean gliding = Boolean.TRUE.equals(p.getAttached(HeroManager.JUNO_GLIDE));
		her.glideTicks = gliding ? her.glideTicks + 1 : 0;
		if (gliding && !her.gliding) {
			her.endingSaid = false;
		}
		hold(mc, her.playing, "glide_start", p, own, gliding, "glide.start", 60, false);
		hold(mc, her.playing, "glide", p, own, gliding, "glide.loop", 60, true);
		if (gliding && !her.endingSaid && (own ? ClientHero.sync().activeLeft() <= JunoStats.GLIDE_ENDING
			: her.glideTicks >= Math.round(JunoStats.GLIDE_ENDING_3P * 20))) {
			// 0.4 s of it left on hers (47B7 #544 op71(v215, 0.4) -> #545 5843); the others' copy is part of B924's Play, 3.0 s in
			her.endingSaid = true;
			once(mc, p, own, "glide.ending", 60);
		}
		if (!gliding && her.gliding) {
			once(mc, p, own, "glide.end", 60);
		}
		her.gliding = gliding;

		// the lock (4695 #819 holds 520F while she locks: B16D hers, B91C others' 70 m; only B91C has a Stop sound)
		boolean locking = Boolean.TRUE.equals(p.getAttached(HeroManager.JUNO_LOCK));
		hold(mc, her.playing, "lock_start", p, own, locking, "torpedo.lock_start", 70, false);
		hold(mc, her.playing, "lock", p, own, locking, "torpedo.lock.loop", 70, true);
		if (!locking && her.locking && !own) {
			play(mc, sound(mc, p, false, "torpedo.lock_end_3p", 70));
		}
		her.locking = locking;

		// her hover, from her motion (HeroAnimator's rule): her fall held at its 3 m/s (0.15 a tick) for 3 ticks
		double vy = p.getY() - p.yo;
		// (another player's fall read over 4 ticks: their steps held a tick or two read as a hover's end, found playing)
		her.ys[her.yAt++ % her.ys.length] = p.getY();
		double fall = own || her.yAt < her.ys.length ? vy : (p.getY() - her.ys[her.yAt % her.ys.length]) / (her.ys.length - 1);
		boolean hoverNow = !p.onGround() && !gliding && fall < -0.13 && fall > -0.17;
		her.hoverTicks = hoverNow ? her.hoverTicks + 1 : 0;
		her.offTicks = hoverNow ? 0 : her.offTicks + 1;
		if (!her.hovering && her.hoverTicks >= 3) {
			her.hovering = true;
			if (now - her.hoverEndAt >= 0.5f) {
				// #378 5C0F as it begins; skipped after a hover within 0.5 s (4441 #593, as the clip: M for the sound)
				once(mc, p, own, "boots.hover_start", 60);
			}
		} else if (her.hovering && (p.onGround() || gliding || her.offTicks >= (own ? 1 : 4))) {
			// #380 onEnd -> #383 5C10 (another player's ends after 4 ticks out of it: their motion comes in steps)
			her.hovering = false;
			her.hoverEndAt = now;
			once(mc, p, own, "boots.hover_end", 60);
		}
		hold(mc, her.playing, "hover", p, own, her.hovering, "boots.hover.loop", 60, true);
	}

	/**
	 * Her double jump on her own client, as OwMovement makes it (4441 #570 -> #274 53EB: AD51 in her head); the others' copy
	 * (AD52) comes from the server, told by her input (ClientJuno.input), since another player's motion seen here comes in
	 * steps a kick can't be told from (found playing: missed jumps and doubled ones).
	 */
	public static void doubleJumped(Minecraft mc) {
		if (mc.player != null) {
			once(mc, mc.player, true, "boots.double_jump", 60);
		}
	}

	/** Each torpedo's flight loop, the ring's hum and the ray's, at it as it moves (JunoFx flies them as the server does). */
	private static void flying(Minecraft mc) {
		Map<String, Vec3> now = JunoFx.sounding();
		for (Iterator<Map.Entry<String, SoundInstance>> it = FLYING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, SoundInstance> e = it.next();
			if (!now.containsKey(e.getKey())) {
				mc.getSoundManager().stop(e.getValue());
				it.remove();
			}
		}
		now.forEach((key, at) -> {
			SoundInstance playing = FLYING.get(key);
			boolean ray = key.startsWith("ray/");
			if (playing != null && (ray || mc.getSoundManager().isActive(playing))) {
				// (the ray's hum is one long file, not a loop: never again)
				return;
			}
			// an ally torpedo 534F's 5350 (51E8: B400 / B3FE / B401, 30 m), an enemy-bound one 54CB's 5A5B (5940); the ring's
			// 5A5C (B16C, 70 m); the ray's 54F7 (B198, 75 m)
			String name = key.startsWith("torpedo_heal/") ? "torpedo.flight.loop" : key.startsWith("torpedo/") ? "torpedo.flight_enemy.loop"
				: key.startsWith("ring/") ? "ring.loop" : "ult.ray.hum";
			double metres = key.startsWith("torpedo") ? 30 : ray ? 75 : 70;
			SoundEvent event = event(mc, name);
			if (event != null) {
				SoundInstance sound = new Following(event, (float) Math.max(1, metres / 16), () -> JunoFx.sounding(key));
				mc.getSoundManager().play(sound);
				FLYING.put(key, sound);
			}
		});
	}

	/** This client's player inside an Orbital Ray: its loop (501A #396 7AFA) and the amp's start, loop and end (54A6). */
	private static void ray(Minecraft mc) {
		boolean inside = mc.player.isAlive() && JunoFx.inRay(mc.player);
		if (inside && !inRay) {
			play(mc, sound(mc, mc.player, true, "ult.amp.start", 0));
		} else if (!inside && inRay) {
			play(mc, sound(mc, mc.player, true, "ult.amp.end", 0));
		}
		inRay = inside;
		hold(mc, MINE, "inside", mc.player, true, inside, "ult.ray.inside.loop", 0, true);
		hold(mc, MINE, "amp", mc.player, true, inside, "ult.amp.loop", 0, true);
	}

	/**
	 * Keeps {@code name} going while {@code on} (her own copy in her head, {@code name}_3p on her for others): a loop again
	 * each time its file ends, a held one-shot ({@code loop} false) once; stopped once off.
	 */
	private static void hold(Minecraft mc, Map<String, SoundInstance> playing, String key, Player p, boolean own, boolean on,
							 String name, double metres, boolean loop) {
		SoundInstance sound = playing.get(key);
		if (!on) {
			if (sound != null) {
				mc.getSoundManager().stop(sound);
				playing.remove(key);
			}
			return;
		}
		if (sound != null && (!loop || mc.getSoundManager().isActive(sound))) {
			return;
		}
		SoundInstance next = sound(mc, p, own, own ? name : name + "_3p", metres);
		if (next != null) {
			mc.getSoundManager().play(next);
			playing.put(key, next);
		}
	}

	/** A one-shot: hers in her head, {@code name}_3p on her for others. */
	private static void once(Minecraft mc, Player p, boolean own, String name, double metres) {
		play(mc, sound(mc, p, own, own ? name : name + "_3p", metres));
	}

	private static void play(Minecraft mc, @Nullable SoundInstance sound) {
		if (sound != null) {
			mc.getSoundManager().play(sound);
		}
	}

	private static void stopAll(Minecraft mc, Map<String, SoundInstance> playing) {
		playing.values().forEach(s -> mc.getSoundManager().stop(s));
		playing.clear();
	}

	/** "juno." + name, null without the local pack (the event names no file). */
	private static @Nullable SoundEvent event(Minecraft mc, String name) {
		SoundEvent event = dev.overcraft.juno.JunoSounds.of(name);
		if (event == null) {
			return null;
		}
		var registered = mc.getSoundManager().getSoundEvent(event.location());
		return registered != null && registered.getSound(SoundInstance.createUnseededRandom()) != null ? event : null;
	}

	/** In the head for her own, else on her, heard out to {@code metres} (a volume over 1 only widens Minecraft's 16 blocks). */
	private static @Nullable SoundInstance sound(Minecraft mc, Player p, boolean own, String name, double metres) {
		SoundEvent event = event(mc, name);
		if (event == null) {
			return null;
		}
		if (own) {
			return new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, 1, 1, SoundInstance.createUnseededRandom(), false, 0,
				SoundInstance.Attenuation.NONE, 0, 0, 0, true);
		}
		return new EntityBoundSoundInstance(event, SoundSource.PLAYERS, (float) Math.max(1, metres / 16), 1, p, p.getRandom().nextLong());
	}

	/** A sound at something that moves (a torpedo, the ring, the ray): it follows it, and stops once it's gone. */
	private static final class Following extends AbstractTickableSoundInstance {
		private final Supplier<@Nullable Vec3> at;

		Following(SoundEvent event, float volume, Supplier<@Nullable Vec3> at) {
			super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			this.at = at;
			this.volume = volume;
			Vec3 now = at.get();
			if (now != null) {
				x = now.x;
				y = now.y;
				z = now.z;
			}
		}

		@Override
		public void tick() {
			Vec3 now = at.get();
			if (now == null) {
				stop();
				return;
			}
			x = now.x;
			y = now.y;
			z = now.z;
		}
	}
}
