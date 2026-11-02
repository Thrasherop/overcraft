package dev.overcraft.client;

import dev.overcraft.client.effect.SojournFx;
import dev.overcraft.entity.DisruptorShotEntity;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.sojourn.Sojourn;
import dev.overcraft.sojourn.SojournStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
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
 * Sojourn's sounds that follow a state (tools/soundmaps/sojourn.txt; tools/reference/sojourn-sounds.md), for every Sojourn this
 * client sees: her own copies in her head, the "_3p" ones on her for everyone else, at the map's ranges.
 * <ul>
 * <li>Power Slide (2905 held for it: E56A hers, E56B others' 70 m, its Stop's 0.68 s end) from the synced slide, and its scrape
 * while it touches the ground (the ground ray's contact set 06C1: 549B hers, 5579 50 m);</li>
 * <li>Overclock (6FE3 held through its three phases: E539's bed hers, E53A's others' 75 m, their Stop's end) from the synced
 * Overclock, and 6FE2 with 1.3 s of its 8 s left (E53C hers, E546 others');</li>
 * <li>each Disruptor Shot's flight loop at it (31F0's 2C8D: 54EB hers, 60 m; 54FF others', 30 m), each field's loop at it and
 * its end (2228: 9210 hers, 5402 others', 70 m), and on this client's player inside an enemy's field the burn's start, loop
 * and end (1443 #365 4DA2: 55AB, the target's own, no copy for others).</li>
 * </ul>
 * The one-shots of her kit come from the server (dev.overcraft.sojourn.SojournSounds). Each loop's file is a loop length long
 * and starts again as it ends; everything stops with what it follows (her death, another hero, leaving the world).
 */
public final class SojournSounds {
	/** Per Sojourn (entity id): her loops by name, and what was seen of her last tick. */
	private static final class Her {
		final Map<String, SoundInstance> playing = new HashMap<>();
		boolean sliding, overclock, endingSaid;
		float overclockSince;
	}

	private static final Map<Integer, Her> HERS = new HashMap<>();
	/** Loops on what she sends flying and on her fields ("disruptor/<id>", "field/<key>/<her id>"), and those fields' centres. */
	private static final Map<String, SoundInstance> FLYING = new HashMap<>();
	private static final Map<String, Vec3> FIELDS = new HashMap<>();
	/** This client's player's burn inside an enemy field. */
	private static final Map<String, SoundInstance> MINE = new HashMap<>();
	private static boolean burning;

	/** Others' copy of Overclock's ending: 1.3 s of its 8 s left, from its press (0.75 s cast + 8 s - 1.3 s). */
	private static final float ENDING_LEFT = 1.3f;

	private SojournSounds() {
	}

	/** Each client tick (after SojournFx's, which keeps her fields). */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			HERS.values().forEach(h -> stopAll(mc, h.playing));
			HERS.clear();
			stopAll(mc, FLYING);
			FIELDS.clear();
			stopAll(mc, MINE);
			burning = false;
			return;
		}
		Set<Integer> seen = new HashSet<>();
		for (Player p : mc.level.players()) {
			if (!"sojourn".equals(p.getAttached(HeroManager.HERO)) || !p.isAlive() || p.isSpectator()) {
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
		burn(mc);
	}

	private static void her(Minecraft mc, Player p, Her her) {
		boolean own = p == mc.player;
		float now = p.level().getGameTime() / 20f;
		boolean sliding = Boolean.TRUE.equals(p.getAttached(HeroManager.SOJOURN_SLIDE));
		hold(mc, her.playing, "slide", p, own, sliding, "slide.loop", 70);
		hold(mc, her.playing, "scrape", p, own, sliding && p.onGround(), "slide.scrape.loop", 50);
		if (!sliding && her.sliding) {
			// E56A / E56B's Stop event: its 0.68 s end
			once(mc, p, own, "slide.end", 70);
		}
		her.sliding = sliding;

		Integer gun = p.getAttached(HeroManager.SOJOURN_GUN);
		boolean overclock = gun != null && (gun & Sojourn.SYNC_OVERCLOCK) != 0;
		if (overclock && !her.overclock) {
			her.overclockSince = now;
			her.endingSaid = false;
		}
		hold(mc, her.playing, "overclock", p, own, overclock, "ult.loop", 75);
		if (overclock && !her.endingSaid && (own ? ClientHero.sync().activeLeft() <= ENDING_LEFT + SojournStats.OVERCLOCK_TAIL
			&& ClientHero.sync().activeLeft() > 0
			: now - her.overclockSince >= SojournStats.OVERCLOCK_CAST + SojournStats.OVERCLOCK_TIME - ENDING_LEFT)) {
			// 2159 #370 -> #795 -> #796 op71(v215, 1.3) -> #797 6FE2
			her.endingSaid = true;
			once(mc, p, own, "ult.ending", 75);
		}
		if (!overclock && her.overclock) {
			once(mc, p, own, "ult.end", 75);
		}
		her.overclock = overclock;
	}

	/** Each Disruptor Shot's flight loop at it, each field's loop at it and its end as it goes. */
	private static void flying(Minecraft mc) {
		Map<String, Vec3> now = new HashMap<>();
		Map<String, Boolean> hers = new HashMap<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof DisruptorShotEntity shot && shot.isAlive()) {
				now.put("disruptor/" + e.getId(), e.position());
				hers.put("disruptor/" + e.getId(), shot.getOwner() == mc.player);
			}
		}
		SojournFx.fieldsSounding().forEach((key, at) -> {
			now.put(key, at);
			hers.put(key, key.endsWith("/" + mc.player.getId()));
		});
		for (Iterator<Map.Entry<String, SoundInstance>> it = FLYING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, SoundInstance> e = it.next();
			if (!now.containsKey(e.getKey())) {
				mc.getSoundManager().stop(e.getValue());
				it.remove();
			}
		}
		for (Iterator<Map.Entry<String, Vec3>> it = FIELDS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, Vec3> e = it.next();
			if (!now.containsKey(e.getKey())) {
				// the field's end: 9210's / 5402's Stop event (1.17 s) at it
				boolean own = e.getKey().endsWith("/" + mc.player.getId());
				SoundEvent end = event(mc, own ? "disruptor.field.end" : "disruptor.field.end_3p");
				if (end != null) {
					mc.getSoundManager().play(at(end, e.getValue(), 70));
				}
				it.remove();
			}
		}
		now.forEach((key, at) -> {
			boolean field = key.startsWith("field/");
			if (field) {
				FIELDS.put(key, at);
			}
			SoundInstance playing = FLYING.get(key);
			if (playing != null && mc.getSoundManager().isActive(playing)) {
				return;
			}
			boolean own = hers.getOrDefault(key, false);
			String name = field ? "disruptor.field.loop" : "disruptor.flight.loop";
			SoundEvent event = event(mc, own ? name : name + "_3p");
			if (event == null) {
				return;
			}
			double metres = field ? 70 : own ? 60 : 30;
			SoundInstance sound = field ? at(event, at, metres) : new Following(event, (float) Math.max(1, metres / 16), () -> {
				Entity e = mc.level == null ? null : mc.level.getEntity(Integer.parseInt(key.substring("disruptor/".length())));
				return e == null || !e.isAlive() ? null : e.position();
			});
			mc.getSoundManager().play(sound);
			FLYING.put(key, sound);
		});
	}

	/** This client's player inside an enemy's field: the burn's start, loop and end in their head (55AB). */
	private static void burn(Minecraft mc) {
		boolean inside = mc.player.isAlive() && SojournFx.inEnemyField(mc.player);
		if (inside && !burning) {
			play(mc, sound(mc, mc.player, true, "disruptor.burn.start", 0));
		} else if (!inside && burning) {
			play(mc, sound(mc, mc.player, true, "disruptor.burn.end", 0));
		}
		burning = inside;
		hold(mc, MINE, "burn", mc.player, true, inside, "disruptor.burn.loop", 0);
	}

	/** Keeps {@code name} going while {@code on} (hers in her head, {@code name}_3p on her for others), again as its file ends. */
	private static void hold(Minecraft mc, Map<String, SoundInstance> playing, String key, Player p, boolean own, boolean on,
							 String name, double metres) {
		SoundInstance sound = playing.get(key);
		if (!on) {
			if (sound != null) {
				mc.getSoundManager().stop(sound);
				playing.remove(key);
			}
			return;
		}
		if (sound != null && mc.getSoundManager().isActive(sound)) {
			return;
		}
		SoundInstance next = sound(mc, p, own, own ? name : name + "_3p", metres);
		if (next != null) {
			mc.getSoundManager().play(next);
			playing.put(key, next);
		}
	}

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

	/** "sojourn." + name, null without the local pack (the event names no file). */
	private static @Nullable SoundEvent event(Minecraft mc, String name) {
		SoundEvent event = dev.overcraft.sojourn.SojournSounds.of(name);
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

	/** At a still point (a field), heard out to {@code metres}. */
	private static SoundInstance at(SoundEvent event, Vec3 at, double metres) {
		return new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, (float) Math.max(1, metres / 16), 1,
			SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR, at.x, at.y, at.z, false);
	}

	/** A sound at something that moves (a Disruptor Shot): it follows it, and stops once it's gone. */
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
