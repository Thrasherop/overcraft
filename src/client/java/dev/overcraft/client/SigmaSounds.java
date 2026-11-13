package dev.overcraft.client;

import dev.overcraft.client.effect.SigmaFx;
import dev.overcraft.entity.AccretionEntity;
import dev.overcraft.entity.HypersphereEntity;
import dev.overcraft.entity.SigmaBarrierEntity;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.sigma.Sigma;
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
 * Sigma's sounds that follow a state (tools/soundmaps/sigma.txt; tools/reference/sigma-sounds.md), for every Sigma this client
 * sees: his own copies in his head, the "_3p" ones on him (or at what they follow) for everyone else, at the map's ranges.
 * <ul>
 * <li>Kinetic Grasp's hum (2C26: 5428 his, 542B others' 70 m) and Gravitic Flux's flight (2D85, the whole ult: 5900 his, 54F7
 * others' 70 m) from the synced state (HeroManager.SIGMA_STATE); Levitation's loop (652D's C2F6, his own: his client's
 * Levitation);</li>
 * <li>each barrier's travel (2E34 while sent: 54EC in his head, 54ED at it for others, 40 m) and its standing hum (2C78 while
 * stopped: 54CB his at it, 30 m; 54D0 others', 40 m);</li>
 * <li>each sphere's flight loop (2BDE's 5562, 10 m) and each rock's (2D99's 964E, 25 m) at them, the rock's fly-by (its slot
 * m_2170A015, 964D, as it's made: 60 m) following it, and his reticle's loop (2C9E's 5A15, 30 m) at it.</li>
 * </ul>
 * The one-shots of his kit come from the server (dev.overcraft.sigma.SigmaSounds). Each loop's file is a loop length long and
 * starts again as it ends; everything stops with what it follows (his death, another hero, leaving the world).
 */
public final class SigmaSounds {
	/** Per Sigma (entity id): his loops by name. */
	private static final Map<Integer, Map<String, SoundInstance>> HIS = new HashMap<>();
	/** Loops at things that move or stand ("sphere/<id>", "rock/<id>", "barrier/<id>", "reticle"). */
	private static final Map<String, SoundInstance> AT = new HashMap<>();
	/** Rocks whose fly-by has played. */
	private static final Set<Integer> FLYBY = new HashSet<>();

	private SigmaSounds() {
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			HIS.values().forEach(m -> stopAll(mc, m));
			HIS.clear();
			stopAll(mc, AT);
			FLYBY.clear();
			return;
		}
		Set<Integer> seen = new HashSet<>();
		for (Player p : mc.level.players()) {
			if (!"sigma".equals(p.getAttached(HeroManager.HERO)) || !p.isAlive() || p.isSpectator()) {
				continue;
			}
			seen.add(p.getId());
			Map<String, SoundInstance> his = HIS.computeIfAbsent(p.getId(), id -> new HashMap<>());
			boolean own = p == mc.player;
			Integer synced = p.getAttached(HeroManager.SIGMA_STATE);
			int state = synced == null ? 0 : synced;
			hold(mc, his, "grasp", p, own, (state & Sigma.MODE_GRASP) != 0, "grasp.loop", 70);
			hold(mc, his, "flux", p, own, (state & Sigma.MODE_FLUX_FLIGHT) != 0, "flux.flight.loop", 70);
			hold(mc, his, "levitate", p, true, own && ClientSigma.levitating(p), "perk.levitation.loop", 0);
		}
		HIS.entrySet().removeIf(e -> {
			if (!seen.contains(e.getKey())) {
				stopAll(mc, e.getValue());
				return true;
			}
			return false;
		});
		things(mc);
	}

	/** The loops at his spheres, rocks, barriers and reticle. */
	private static void things(Minecraft mc) {
		Map<String, Supplier<@Nullable Vec3>> want = new HashMap<>();
		Map<String, String> names = new HashMap<>();
		Map<String, Double> ranges = new HashMap<>();
		Map<String, Boolean> heads = new HashMap<>();
		Set<Integer> rocks = new HashSet<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!e.isAlive()) {
				continue;
			}
			int id = e.getId();
			if (e instanceof HypersphereEntity) {
				String key = "sphere/" + id;
				want.put(key, following(mc, id));
				names.put(key, "sphere.flight.loop");
				ranges.put(key, 10.0);
			} else if (e instanceof AccretionEntity) {
				String key = "rock/" + id;
				want.put(key, following(mc, id));
				names.put(key, "accretion.flight.loop");
				ranges.put(key, 25.0);
				rocks.add(id);
				if (FLYBY.add(id)) {
					SoundEvent flyby = event(mc, "accretion.flyby");
					if (flyby != null) {
						mc.getSoundManager().play(new Following(flyby, 60 / 16f, following(mc, id)));
					}
				}
			} else if (e instanceof SigmaBarrierEntity b) {
				boolean his = b.ownerId() == mc.player.getId();
				String key = "barrier/" + id + "/" + b.state();
				Vec3 at = b.barrierCentre();
				want.put(key, () -> at);
				if (b.state() == SigmaBarrierEntity.SENT) {
					names.put(key, his ? "barrier.move.loop" : "barrier.move.loop_3p");
					ranges.put(key, 40.0);
					heads.put(key, his);
					want.put(key, () -> {
						Entity now = mc.level == null ? null : mc.level.getEntity(id);
						return now instanceof SigmaBarrierEntity nb && nb.isAlive() ? nb.barrierCentre() : null;
					});
				} else {
					names.put(key, his ? "barrier.hum.loop" : "barrier.hum.loop_3p");
					ranges.put(key, his ? 30.0 : 40.0);
				}
			}
		}
		FLYBY.retainAll(rocks);
		Vec3 reticle = SigmaFx.reticle();
		if (reticle != null) {
			want.put("reticle", SigmaFx::reticle);
			names.put("reticle", "flux.aim.loop");
			ranges.put("reticle", 30.0);
		}
		for (Iterator<Map.Entry<String, SoundInstance>> it = AT.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, SoundInstance> e = it.next();
			if (!want.containsKey(e.getKey())) {
				mc.getSoundManager().stop(e.getValue());
				it.remove();
			}
		}
		want.forEach((key, at) -> {
			SoundInstance playing = AT.get(key);
			if (playing != null && mc.getSoundManager().isActive(playing)) {
				return;
			}
			SoundEvent event = event(mc, names.get(key));
			if (event == null) {
				return;
			}
			SoundInstance sound = heads.getOrDefault(key, false) ? head(event)
				: new Following(event, (float) Math.max(1, ranges.get(key) / 16), at);
			mc.getSoundManager().play(sound);
			AT.put(key, sound);
		});
	}

	private static Supplier<@Nullable Vec3> following(Minecraft mc, int id) {
		return () -> {
			Entity e = mc.level == null ? null : mc.level.getEntity(id);
			return e == null || !e.isAlive() ? null : e.position();
		};
	}

	/** Keeps {@code name} going while {@code on} (his in his head, {@code name}_3p on him for others), again as its file ends. */
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
		SoundEvent event = event(mc, own ? name : name + "_3p");
		if (event == null) {
			return;
		}
		SoundInstance next = own ? head(event)
			: new EntityBoundSoundInstance(event, SoundSource.PLAYERS, (float) Math.max(1, metres / 16), 1, p, p.getRandom().nextLong());
		mc.getSoundManager().play(next);
		playing.put(key, next);
	}

	private static SoundInstance head(SoundEvent event) {
		return new SimpleSoundInstance(event.location(), SoundSource.PLAYERS, 1, 1, SoundInstance.createUnseededRandom(), false, 0,
			SoundInstance.Attenuation.NONE, 0, 0, 0, true);
	}

	private static void stopAll(Minecraft mc, Map<String, SoundInstance> playing) {
		playing.values().forEach(s -> mc.getSoundManager().stop(s));
		playing.clear();
	}

	/** "sigma." + name, null without the local pack (the event names no file). */
	private static @Nullable SoundEvent event(Minecraft mc, String name) {
		SoundEvent event = dev.overcraft.sigma.SigmaSounds.of(name);
		if (event == null) {
			return null;
		}
		var registered = mc.getSoundManager().getSoundEvent(event.location());
		return registered != null && registered.getSound(SoundInstance.createUnseededRandom()) != null ? event : null;
	}

	/** A sound at something that moves or stands (a sphere, the rock, the barrier, the reticle); it stops once it's gone. */
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
