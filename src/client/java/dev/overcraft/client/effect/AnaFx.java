package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.entity.AnaDartEntity;
import dev.overcraft.entity.BioticGrenadeEntity;
import dev.overcraft.entity.SleepDartEntity;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Ana's effects (tools/reference/ana-effects.md, read from her effects' data; each moment's numbers there). Her side sees
 * the gold and blue rows, her enemies the red (the data's red-violet twins: M which team sees which). The server sends her
 * moments (EffectPayload "ana_*"); her projectiles' trails and the states she puts on others (asleep, the grenade's boost
 * and anti-heal, Nano Boost) are drawn each client tick from what it sent, until their time is up.
 */
public final class AnaFx {
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** A state on an entity: since when, until when (Fx.now() seconds), and whether its giver is an enemy of ours. */
	private record Held(float since, float until, boolean enemy) {
	}

	private static final Map<Integer, Held> ASLEEP = new HashMap<>();
	private static final Map<Integer, Held> BOOSTED = new HashMap<>();
	private static final Map<Integer, Held> ANTI_HEALED = new HashMap<>();
	private static final Map<Integer, Held> NANOED = new HashMap<>();
	/** Dev only: what it draws on this entity now. */
	public static String devHeld(int id) {
		return (ASLEEP.containsKey(id) ? "asleep " : "") + (ANTI_HEALED.containsKey(id) ? "antiHealed " : "") + (NUMBED.containsKey(id) ? "numbed " : "");
	}

	private static final Map<Integer, Held> NUMBED = new HashMap<>();

	private AnaFx() {
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}

	private static Vec3 randomUnit() {
		Vec3 v = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return v.lengthSqr() < 1.0E-6 ? UP : v.normalize();
	}

	private static Palette or(boolean enemy, Palette ours) {
		return enemy ? Palette.ENEMY : ours;
	}

	/** {@code n} sparks thrown from {@code at} at {@code speed} m/s, stretched along their flight. */
	private static void sparks(Vec3 at, int n, float speed, Palette palette, float size, float life, float gravity) {
		for (int i = 0; i < n; i++) {
			Particle p = Fx.particle("spark#4", palette);
			if (p == null) {
				return;
			}
			Fx.add(p.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(speed * range(0.7f, 1.1f))).life(life * range(0.7f, 1.2f))
				.size(size, size * 0.4f).aspect(0.15f).gravity(gravity).fade(0.02f, 0.5f).nearFade(0.5f));
		}
	}

	// ---- each client tick ----

	/** Her projectiles' trails and the states on others. */
	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			return;
		}
		float now = Fx.now();
		for (Entity e : mc.level.entitiesForRendering()) {
			boolean enemy = e instanceof net.minecraft.world.entity.projectile.Projectile p && KirikoFx.enemyOf(p.getOwner());
			if (e instanceof AnaDartEntity) {
				// the unscoped dart: two thin gold ribbons trailing 0.1 s (19F7: 0.03 m, ours a touch wider to be seen)
				Fx.trail(e.getId(), e.position(), "ana_dart_band", or(enemy, Palette.ANA_SHOT_TRAIL), 0.05f, 0.1f, 0.9f);
			} else if (e instanceof SleepDartEntity) {
				// the sleep dart: blue ribbons trailing 0.3 s and a soft blue glow round it (15E8's 1A7D)
				Fx.trail(e.getId(), e.position(), "ana_dart_band", or(enemy, Palette.ANA_SLEEP_GLOW_N), 0.06f, 0.3f, 0.9f);
				Particle glow = Fx.attach("ana_sleep_dart/" + e.getId(), e.position(), () -> {
					Particle p = Fx.particle("mercy_glow", or(enemy, Palette.ANA_SLEEP_GLOW_N));
					return p == null ? null : p.life(0.15f).size(0.6f, 0.6f).alpha(0.5f).fade(0.02f, 0.5f).nearFade(0.8f);
				});
				if (glow != null) {
					glow.at(e.position());
				}
			} else if (e instanceof BioticGrenadeEntity) {
				// the grenade: a 0.12 m tube trail living 0.5 s, violet to gold (19FB)
				Fx.trail(e.getId(), e.position(), "ana_tube_line", or(enemy, Palette.ANA_GRENADE_TRAIL), 0.12f, 0.5f, 0.9f);
			}
		}
		held(mc, ASLEEP, now, AnaFx::asleep);
		held(mc, BOOSTED, now, AnaFx::boosted);
		held(mc, ANTI_HEALED, now, AnaFx::antiHealed);
		held(mc, NANOED, now, AnaFx::nanoed);
		held(mc, NUMBED, now, AnaFx::numbed);
	}

	private interface Draw {
		void draw(LivingEntity entity, Held held, float now);
	}

	private static void held(Minecraft mc, Map<Integer, Held> states, float now, Draw draw) {
		for (Iterator<Map.Entry<Integer, Held>> it = states.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Held> entry = it.next();
			Entity e = mc.level.getEntity(entry.getKey());
			if (!(e instanceof LivingEntity living) || !living.isAlive() || now >= entry.getValue().until) {
				it.remove();
				continue;
			}
			if (!ownFirstPerson(living)) {
				draw.draw(living, entry.getValue(), now);
			}
		}
	}

	/** The local player in first person: their own states aren't drawn round their eyes. */
	private static boolean ownFirstPerson(Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	/**
	 * Asleep (1A58 on the head): Zs (98DA), about 1.85 a second, each living 3 s, growing 0.04 -> 0.33 m as they drift up
	 * 1.4 m and off to one side, lilac turning white.
	 */
	private static void asleep(LivingEntity e, Held h, float now) {
		if (Fx.RANDOM.nextFloat() >= 1.85f / 20) {
			return;
		}
		Particle z = Fx.particle("ana_z", or(h.enemy, Palette.ANA_SLEEP_Z));
		if (z != null) {
			Vec3 head = e.position().add(0, e.getBbHeight() + 0.1, 0);
			Vec3 drift = new Vec3(range(-0.15f, 0.15f), 1.4f / 3, range(-0.15f, 0.15f));
			Fx.add(z.at(head).velocity(drift).life(3).size(0.04f, 0.33f).fade(0.15f, 0.6f).nearFade(0.5f));
		}
	}

	/** The grenade's healing boost (19F5): pale-gold pluses rising round them, 20 a second, 0.3 s each. */
	private static void boosted(LivingEntity e, Held h, float now) {
		for (int i = 0; i < 1; i++) {
			Particle plus = Fx.particle("mercy_plus", or(h.enemy, Palette.ANA_BOOST_PLUS));
			if (plus != null) {
				Vec3 at = e.position().add(range(-0.4f, 0.4f), range(0.1f, e.getBbHeight()), range(-0.4f, 0.4f));
				Fx.add(plus.at(at).velocity(new Vec3(0, range(0.5f, 0.65f), 0)).life(0.3f).size(0.12f, 0.1f).fade(0.05f, 0.5f)
					.nearFade(0.5f));
			}
		}
	}

	/** Its anti-heal (19F4): violet pluses, 3-6 a second, drifting down for about 1 s. */
	private static void antiHealed(LivingEntity e, Held h, float now) {
		if (Fx.RANDOM.nextFloat() >= 4.5f / 20) {
			return;
		}
		Particle plus = Fx.particle("mercy_plus", or(h.enemy, Palette.ANA_ANTIHEAL));
		if (plus != null) {
			Vec3 at = e.position().add(range(-0.4f, 0.4f), e.getBbHeight() + range(0, 0.3f), range(-0.4f, 0.4f));
			Fx.add(plus.at(at).velocity(new Vec3(0, -0.3f, 0)).life(1).size(0.14f, 0.1f).spin(range(0, 6.28f), 1).fade(0.1f, 0.5f)
				.nearFade(0.5f));
		}
	}

	/**
	 * Nano Boost on them (1A16, 8 s): a blue glow on their body, blue flame tongues and long blue streaks rising (14-20 a
	 * second), in Mercy's boost blue.
	 */
	private static void nanoed(LivingEntity e, Held h, float now) {
		Vec3 chest = e.position().add(0, e.getBbHeight() * 0.55, 0);
		Particle glow = Fx.attach("ana_nano/" + e.getId(), chest, () -> {
			Particle p = Fx.particle("mercy_glow", or(h.enemy, Palette.ANA_NANO_BLUE));
			return p == null ? null : p.life(0.3f).alpha(0.35f).fade(0.1f, 0.5f).nearFade(0.8f);
		});
		if (glow != null) {
			// wider than them: a glow their own size sat inside their body and hardly showed (found playing)
			glow.size(e.getBbHeight() * 1.6f, e.getBbHeight() * 1.6f).at(chest);
		}
		for (int i = 0; i < (Fx.RANDOM.nextFloat() < 0.5f ? 1 : 0) + 1; i++) {
			Particle streak = Fx.particle("ana_streak_v", or(h.enemy, Palette.ANA_NANO_BEAM_N));
			if (streak != null) {
				Vec3 side = randomUnit().multiply(1, 0, 1).normalize().scale(e.getBbWidth() * 0.65);
				Vec3 at = e.position().add(side.x, range(0, e.getBbHeight()), side.z);
				Fx.add(streak.at(at).shape(Shape.STREAK).velocity(new Vec3(0, range(1.5f, 2.5f), 0)).life(range(0.3f, 0.5f))
					.size(0.5f, 0.3f).aspect(0.08f).fade(0.05f, 0.5f).nearFade(0.6f));
			}
		}
		if (Fx.RANDOM.nextFloat() < 0.7f) {
			Particle flame = Fx.particle("ana_flame#4", or(h.enemy, Palette.ANA_NANO_FLAME_N));
			if (flame != null) {
				// on their outline, not inside them
				Vec3 side = randomUnit().multiply(1, 0, 1).normalize().scale(e.getBbWidth() * 0.6);
				Vec3 at = e.position().add(side.x, range(0.2f, e.getBbHeight() * 0.9f), side.z);
				Fx.add(flame.at(at).velocity(new Vec3(0, 0.8f, 0)).life(0.4f).size(0.25f, 0.35f).loop(10).fade(0.05f, 0.5f)
					.nearFade(0.6f));
			}
		}
	}

	/** Numbed by Local Anesthetic (3D2F, its 3 s): cyan wisps drawn in toward them, and vertical streaks. */
	private static void numbed(LivingEntity e, Held h, float now) {
		Vec3 chest = e.position().add(0, e.getBbHeight() * 0.55, 0);
		if (Fx.RANDOM.nextFloat() < 0.6f) {
			Particle wisp = Fx.particle("mercy_wisp", or(h.enemy, Palette.ANA_LA_WISP_N));
			if (wisp != null) {
				Vec3 from = chest.add(randomUnit().scale(0.8));
				Fx.add(wisp.at(from).velocity(chest.subtract(from).scale(1.6)).life(0.5f).size(0.35f, 0.15f).fade(0.1f, 0.5f)
					.nearFade(0.6f));
			}
		}
		if (Fx.RANDOM.nextFloat() < 0.4f) {
			Particle streak = Fx.particle("ana_streak_v", or(h.enemy, Palette.ANA_LA_DROP));
			if (streak != null) {
				Vec3 side = randomUnit().multiply(1, 0, 1).normalize().scale(e.getBbWidth() * 0.6);
				Fx.add(streak.at(e.position().add(side.x, range(0, e.getBbHeight()), side.z)).shape(Shape.STREAK)
					.velocity(new Vec3(0, -1.2f, 0)).life(0.4f).size(0.4f, 0.3f).aspect(0.08f).fade(0.05f, 0.5f).nearFade(0.6f));
			}
		}
	}

	// ---- her moments (EffectPayload) ----

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Minecraft mc = Minecraft.getInstance();
		float now = Fx.now();
		switch (e.effect()) {
			case "ana_fire" -> muzzle(at, dir, enemy, mc.level != null && ownFirstPerson(mc.level.getEntity(e.entity())));
			case "ana_tracer" -> tracer(at, at.add(dir), enemy);
			case "ana_heal_hit" -> healHit(at, enemy);
			case "ana_harm_hit" -> harmHit(at, enemy);
			case "ana_hit_block" -> blockHit(at, dir, enemy, false);
			case "ana_sleep_hit_block" -> blockHit(at, dir, enemy, true);
			case "ana_sleep_hit" -> sleepHit(at, enemy);
			case "ana_grenade_burst" -> grenadeBurst(at, enemy, e.variant() == 1);
			case "ana_anesthetic" -> anesthetic(at, enemy);
			case "ana_nano_beam" -> nanoBeam(at, at.add(dir), enemy);
			case "ana_asleep" -> ASLEEP.put(e.entity(), new Held(now - e.dx(), now + e.variant() / 20f, enemy));
			case "ana_wake" -> ASLEEP.remove(e.entity());
			case "ana_boosted" -> BOOSTED.put(e.entity(), new Held(now - e.dx(), now + e.variant() / 20f, enemy));
			case "ana_anti_healed" -> ANTI_HEALED.put(e.entity(), new Held(now - e.dx(), now + e.variant() / 20f, enemy));
			case "ana_anti_healed_end" -> ANTI_HEALED.remove(e.entity());
			case "ana_nano" -> NANOED.put(e.entity(), new Held(now - e.dx(), now + e.variant() / 20f, enemy));
			case "ana_nano_end" -> NANOED.remove(e.entity());
			case "ana_numbed" -> NUMBED.put(e.entity(), new Held(now - e.dx(), now + e.variant() / 20f, enemy));
			case "ana_numbed_end" -> NUMBED.remove(e.entity());
			default -> {
				return false;
			}
		}
		return true;
	}

	/**
	 * The rifle's muzzle flash (19F8): a gold half-sphere flash 0.79 m wide for 0.14 s, gold flares and two-sided flame
	 * streaks about 0.2 s, 5 cm in front of the muzzle. That is the third-person rifle's: the shooter's own first-person
	 * view has the muzzle half a metre from the eye, where the 0.79 m flash filled the screen (found playing), so theirs is
	 * a third the size and fainter (ours).
	 */
	private static void muzzle(Vec3 at, Vec3 dir, boolean enemy, boolean own) {
		dev.overcraft.client.ClientAna.State mine = own ? dev.overcraft.client.ClientAna.state(Minecraft.getInstance().player) : null;
		if (mine != null && mine.has(dev.overcraft.ana.Ana.FLAG_SCOPED)) {
			// scoped, her own view is the scope's: no rifle on screen, no flash (it filled the zoomed view: found playing)
			return;
		}
		Vec3 front = at.add(dir.normalize().scale(0.05));
		float k = own ? 0.3f : 1;
		Particle flash = Fx.particle("mercy_glow", or(enemy, Palette.ANA_MUZZLE_N));
		if (flash != null) {
			Fx.add(flash.at(front).life(0.14f).size(0.79f * k, 0.5f * k).alpha(own ? 0.4f : 0.8f).fade(0.01f, 0.5f).nearFade(own ? 0 : 0.4f));
		}
		Particle flame = Fx.particle("ana_muzzle_flame", or(enemy, Palette.ANA_MUZZLE_N));
		if (flame != null) {
			Fx.add(flame.at(front.add(dir.normalize().scale(0.2 * k))).axis(dir).size(0.5f * k, 0.3f * k).aspect(0.35f).life(0.12f)
				.fade(0.01f, 0.5f).nearFade(own ? 0 : 0.4f));
		}
	}

	/** The scoped shot's tracer (1A80): a gold band from the muzzle to the hit, 0.2 m wide, fading over 0.75 s. */
	private static void tracer(Vec3 from, Vec3 to, boolean enemy) {
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len < 0.5f) {
			return;
		}
		Particle band = Fx.particle("ana_tracer", or(enemy, Palette.ANA_TRACER_N));
		if (band != null) {
			Fx.add(band.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(0.2f / len).life(0.75f).fade(0.01f, 0.8f)
				.nearFade(0.6f));
		}
	}

	/** A dart heals (1A18 on the chest): two pale-gold bursts 1.5-2 m, a 1 m amber glow, 10-15 gold sparks at 4.3 m/s. */
	private static void healHit(Vec3 at, boolean enemy) {
		for (int i = 0; i < 2; i++) {
			Particle burst = Fx.particle("ana_burst", or(enemy, Palette.ANA_HEAL_BURST_N));
			if (burst != null) {
				float s = range(1.5f, 2);
				Fx.add(burst.at(at).life(0.25f).size(s * 0.5f, s).spin(range(0, 6.28f), 0).alpha(0.6f).fade(0.02f, 0.6f).nearFade(0.8f));
			}
		}
		Particle glow = Fx.particle("mercy_glow", or(enemy, Palette.ANA_HEAL_GLOW));
		if (glow != null) {
			Fx.add(glow.at(at).life(0.3f).size(1, 0.8f).alpha(0.5f).fade(0.02f, 0.6f).nearFade(0.8f));
		}
		sparks(at, 10 + Fx.RANDOM.nextInt(6), 4.3f, or(enemy, Palette.ANA_HEAL_GLOW), 0.12f, 0.35f, 4);
	}

	/** A dart hurts (19FC): purple swirling discs 0.3-1 m across and purple bubbles. */
	private static void harmHit(Vec3 at, boolean enemy) {
		for (int i = 0; i < 3; i++) {
			Particle disc = Fx.particle("ana_splash#10", or(enemy, Palette.ANA_HARM_N));
			if (disc != null) {
				float s = range(0.3f, 1);
				Fx.add(disc.at(at.add(randomUnit().scale(0.15))).life(0.4f).size(s * 0.6f, s).loop(25).spin(range(0, 6.28f), 3)
					.fade(0.02f, 0.5f).nearFade(0.6f));
			}
		}
		sparks(at, 6, 1.5f, or(enemy, Palette.ANA_HARM_BUBBLE), 0.08f, 0.6f, -0.5f);
	}

	/**
	 * A shot or dart on a block: a 0.4 m flare and a 0.35 m star mark on the face, lasting 3 s (the shot's gold, the sleep
	 * dart's blue).
	 */
	private static void blockHit(Vec3 at, Vec3 normal, boolean enemy, boolean sleep) {
		Palette palette = or(enemy, sleep ? Palette.ANA_SLEEP_GLOW_N : Palette.ANA_DECAL_GOLD_N);
		Particle flare = Fx.particle("ana_star", palette);
		if (flare != null) {
			Fx.add(flare.at(at.add(normal.scale(0.05))).life(0.15f).size(0.4f, 0.3f).fade(0.01f, 0.5f).nearFade(0.4f));
		}
		Particle mark = Fx.particle("ana_star_decal", palette);
		if (mark != null && normal.lengthSqr() > 1.0E-6) {
			Fx.add(mark.at(at).normal(normal).lift(normal).life(3).size(0.35f, 0.35f).spin(range(0, 6.28f), 0).alpha(0.8f)
				.fade(0.01f, 0.3f));
		}
	}

	/** The sleep dart lands on someone (15E8): 10 sparks at 15-17 m/s, a 0.35 m blue star, a 1.2-1.5 m blue flash. */
	private static void sleepHit(Vec3 at, boolean enemy) {
		sparks(at, 10, 16, or(enemy, Palette.ANA_SLEEP_GLOW_N), 0.25f, 0.15f, 0);
		Particle star = Fx.particle("ana_star_burst", or(enemy, Palette.ANA_SLEEP_GLOW_N));
		if (star != null) {
			Fx.add(star.at(at).life(0.25f).size(0.35f, 0.5f).fade(0.01f, 0.5f).nearFade(0.5f));
		}
		Particle flash = Fx.particle("mercy_glow", or(enemy, Palette.ANA_SLEEP_GLOW_N));
		if (flash != null) {
			Fx.add(flash.at(at).life(0.3f).size(1.2f, 1.5f).alpha(0.6f).fade(0.02f, 0.6f).nearFade(0.8f));
		}
	}

	/**
	 * The grenade bursts (1874): a gold dome 6.9 m across and a violet-to-yellow shell (drawn as faint washes), violet and
	 * gold splashes, 33 gold and 33 violet droplets at 6-12 m/s, a ground splat about 4 m wide. The bounce's second burst
	 * (Biotic Bounce: 647D, a smaller copy, M) at 60%.
	 */
	private static void grenadeBurst(Vec3 at, boolean enemy, boolean bounce) {
		float k = bounce ? 0.6f : 1;
		Particle dome = Fx.particle("mercy_glow", or(enemy, Palette.ANA_BURST_DOME));
		if (dome != null) {
			Fx.add(dome.at(at).shape(Shape.DOME).life(0.45f).size(3 * k, 6.9f * k).alpha(0.25f).fade(0.02f, 0.6f).nearFade(1.5f));
		}
		for (int i = 0; i < 3; i++) {
			Particle splash = Fx.particle("ana_splash#10", or(enemy, i == 0 ? Palette.ANA_SPLASH_GOLD_N : Palette.ANA_SPLASH_N));
			if (splash != null) {
				float s = range(5, 7) * k;
				Fx.add(splash.at(at.add(randomUnit().scale(0.3))).life(0.4f).size(s * 0.5f, s).loop(25).spin(range(0, 6.28f), 1)
					.alpha(0.5f).fade(0.02f, 0.5f).nearFade(1.5f));
			}
		}
		int drops = Math.round(33 * k);
		for (int i = 0; i < drops * 2; i++) {
			Particle drop = Fx.soft("ana_blob#2", or(enemy, i % 2 == 0 ? Palette.ANA_DROP_GOLD_N : Palette.ANA_DROP_PURPLE));
			if (drop == null) {
				break;
			}
			Vec3 v = randomUnit().add(0, 0.6, 0).normalize().scale(range(6, 12) * k);
			Fx.add(drop.at(at).shape(Shape.STREAK).velocity(v).gravity(9.8f).drag(0.3f).life(range(0.4f, 0.8f)).size(0.12f, 0.06f)
				.aspect(0.5f).fade(0.02f, 0.4f).nearFade(0.5f));
		}
		Particle splat = Fx.particle("ana_splat#3", or(enemy, Palette.ANA_BURST_DECAL));
		if (splat != null) {
			Fx.add(splat.at(at).normal(UP).lift(UP).life(1.2f).size(4 * k, 4 * k).spin(range(0, 6.28f), 0).alpha(0.5f)
				.fade(0.02f, 0.5f));
		}
	}

	/** Local Anesthetic's burst (7DBF): a 3.5 m cyan-blue dome, gas, 33 droplets. */
	private static void anesthetic(Vec3 at, boolean enemy) {
		Particle dome = Fx.particle("mercy_glow", or(enemy, Palette.ANA_LA_DOME));
		if (dome != null) {
			Fx.add(dome.at(at).shape(Shape.DOME).life(0.5f).size(1.5f, 3.5f).alpha(0.3f).fade(0.02f, 0.6f).nearFade(1));
		}
		for (int i = 0; i < 4; i++) {
			Particle gas = Fx.soft("mercy_glow", or(enemy, Palette.ANA_LA_GAS));
			if (gas != null) {
				Fx.add(gas.at(at.add(randomUnit().scale(1))).velocity(randomUnit().scale(0.3)).life(range(1, 1.5f)).size(1.5f, 2.5f)
					.alpha(0.25f).fade(0.2f, 0.6f).nearFade(1));
			}
		}
		sparks(at, 33, 6, or(enemy, Palette.ANA_LA_DROP), 0.1f, 0.6f, 9.8f);
	}

	/**
	 * Nano Boost flies to the ally (1A5F): a blue core band 0.16 m wide lasting 1 s, from her left forearm to them (1A5E's
	 * orange and blue puffs at her end), and a glow at the ally.
	 */
	private static void nanoBeam(Vec3 from, Vec3 to, boolean enemy) {
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len > 0.3f) {
			Particle band = Fx.particle("ana_nano_core", or(enemy, Palette.ANA_NANO_BEAM_N));
			if (band != null) {
				Fx.add(band.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(0.16f / len).life(1).fade(0.02f, 0.7f)
					.nearFade(0.6f));
			}
		}
		Particle puff = Fx.particle("mercy_glow", or(enemy, Palette.ANA_NANO_MUZZLE));
		if (puff != null) {
			Fx.add(puff.at(from).life(0.3f).size(0.3f, 0.6f).alpha(0.6f).fade(0.02f, 0.6f).nearFade(0.5f));
		}
		Particle glow = Fx.particle("mercy_glow", or(enemy, Palette.ANA_NANO_BLUE));
		if (glow != null) {
			Fx.add(glow.at(to).life(2).size(1.75f, 1).alpha(0.5f).fade(0.05f, 0.6f).nearFade(0.8f));
		}
	}

	public static void clear() {
		ASLEEP.clear();
		BOOSTED.clear();
		ANTI_HEALED.clear();
		NANOED.clear();
		NUMBED.clear();
	}
}
