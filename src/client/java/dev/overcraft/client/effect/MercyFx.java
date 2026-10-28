package dev.overcraft.client.effect;

import dev.overcraft.client.ClientMercy;
import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.mercy.Mercy;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Mercy's effects, from her effects reference (tools/reference/mercy-effects.md: every size, colour and rate there is
 * the data's unless marked ours). Her beam every frame for every Mercy whose synced state has one on (ClientMercy), her
 * held states (Guardian Angel, Angelic Descent, Valkyrie, her wings at rest) each tick from her posed hardpoints
 * (HeroModelRenderer), and her moments from the server (EffectPayload: Resurrect, Flash Heal, Valkyrie's cast, her
 * blaster's hits).
 */
public final class MercyFx {
	/** Her points as last drawn (world): the staff head, chest 0001, hand 0002, wing tips 002C / 002E, Valkyrie's 10 feathers. */
	public static final class Posed {
		public @Nullable Vec3 staff;
		public Vec3[] points = new Vec3[0];
		long tick;
	}

	/** The hardpoints HeroModelRenderer poses for her, in this order (her model's own table: the exported sockets). */
	public static final String[] POINTS = {"0001", "0002", "002C", "002E", "007E", "0080", "0155", "0157", "0158", "015A",
		"015B", "015D", "0169", "016B"};
	private static final int CHEST = 0, WING_L = 2, WING_R = 3, FEATHERS = 4;

	private static final Map<Integer, Posed> POSED = new HashMap<>();
	private static final Vec3 UP = new Vec3(0, 1, 0);

	private MercyFx() {
	}

	/** HeroModelRenderer, each frame she's drawn: where her staff's head and her points are. */
	public static void posed(Entity mercy, @Nullable Vec3 staff, Vec3[] points) {
		Posed p = POSED.computeIfAbsent(mercy.getId(), id -> new Posed());
		p.staff = staff;
		p.points = points;
		p.tick = mercy.level().getGameTime();
	}

	private static @Nullable Posed posed(Entity mercy) {
		Posed p = POSED.get(mercy.getId());
		Minecraft mc = Minecraft.getInstance();
		boolean ownView = mercy == mc.player && mc.options.getCameraType().isFirstPerson();
		return p == null || ownView || mercy.level().getGameTime() - p.tick > 1 ? null : p;
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}

	private static Vec3 randomUnit() {
		double z = Fx.RANDOM.nextDouble() * 2 - 1;
		double a = Fx.RANDOM.nextDouble() * Math.PI * 2;
		double s = Math.sqrt(1 - z * z);
		return new Vec3(s * Math.cos(a), z, s * Math.sin(a));
	}

	/** A direction within {@code degrees} of the axis. */
	private static Vec3 cone(Vec3 axis, double degrees) {
		Vec3 r = randomUnit();
		return axis.normalize().add(r.scale(Math.tan(Math.toRadians(degrees)) * Fx.RANDOM.nextDouble())).normalize();
	}

	// ---- the beam (graphs 0079 heal / 006C boost on the ally) ----

	/** Every frame (before the effects are drawn): each Mercy's beam, and Valkyrie's branches from her chest. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		for (Player player : mc.level.players()) {
			ClientMercy.State s = ClientMercy.state(player);
			if (s != null && player.isAlive() && s.has(Mercy.FLAG_VALKYRIE)) {
				Posed p = posed(player);
				if (p != null && p.points.length >= FEATHERS + 10) {
					feathers(player.getId(), p, KirikoFx.enemyOf(player), mc.level.getGameTime() + partial);
				}
			}
			if (s == null || s.beamMode() == Mercy.BEAM_NONE || !player.isAlive()) {
				continue;
			}
			if (!(mc.level.getEntity(s.beamTarget()) instanceof LivingEntity target) || !target.isAlive()) {
				continue;
			}
			boolean heal = s.beamMode() == Mercy.BEAM_HEAL;
			beam(player.getId(), 0, staffHead(mc, player, partial), chest(target, partial), heal);
			int n = 1;
			for (int id : s.chain()) {
				// Valkyrie's branches (and Chain Boost's link) start at her chest (the beam node's 360° mode, 0079#294)
				if (mc.level.getEntity(id) instanceof LivingEntity other && other.isAlive()) {
					Posed p = posed(player);
					Vec3 chest = p != null && p.points.length > CHEST ? p.points[CHEST] : player.getPosition(partial).add(0, 1.2, 0);
					beam(player.getId(), n, chest, chest(other, partial), heal);
				}
				n++;
			}
		}
	}

	/** The ally's chest (their hp 0001: ours, 60% up a Minecraft mob). */
	private static Vec3 chest(LivingEntity e, float partial) {
		return e.getPosition(partial).add(0, e.getBbHeight() * 0.6, 0);
	}

	/**
	 * Her staff's head (its hp 0006, where the beam starts), as posed this frame or the last tick: in her own first person
	 * the view model's staff as drawn (HeroModelRenderer.viewMuzzle); else ours, a little right of and below her view.
	 */
	private static Vec3 staffHead(Minecraft mc, Player mercy, float partial) {
		org.joml.Vector3f view = mercy == mc.player && mc.options.getCameraType().isFirstPerson()
			? dev.overcraft.client.model.HeroModelRenderer.viewMuzzle() : null;
		if (view != null) {
			return mc.gameRenderer.mainCamera().position().add(view.x, view.y, view.z);
		}
		Posed p = posed(mercy);
		if (p != null && p.staff != null) {
			return p.staff;
		}
		Vec3 look = mercy.getViewVector(partial);
		Vec3 side = look.cross(UP);
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(look).normalize();
		return mercy.getEyePosition(partial).add(look.scale(0.7)).add(side.scale(0.28)).add(up.scale(-0.22));
	}

	/**
	 * One beam, straight as the data's two-point ribbon (186F heal: 0.4 m wavy streaks over 0.2 m and 0.1 m soft core
	 * lines, gold; 1870 boost: a 0.5 m deep blue glow round two 0.15 m cores, blue). The in-game bend isn't in the data
	 * (mercy-effects.md): not drawn.
	 */
	private static void beam(int id, int branch, Vec3 from, Vec3 to, boolean heal) {
		if (to.distanceTo(from) < 0.2) {
			return;
		}
		String key = "mercy_beam/" + id + "/" + branch + "/";
		if (heal) {
			band(key + "wave", from, to, "mercy_beam_wave", Palette.MERCY_HEAL_BEAM_N, 0.4f, 1);
			band(key + "core_a", from, to, "mercy_beam_core", Palette.MERCY_HEAL_BEAM_SOFT_N, 0.2f, 1);
			band(key + "core_b", from, to, "mercy_beam_core", Palette.MERCY_HEAL_BEAM_SOFT_N, 0.1f, 1);
		} else {
			band(key + "glow", from, to, "mercy_beam_streak", Palette.MERCY_BOOST_BEAM_GLOW_N, 0.5f, 1);
			band(key + "core_a", from, to, "mercy_band", Palette.MERCY_BOOST_BEAM_N, 0.15f, 1);
			band(key + "core_b", from, to, "mercy_beam_noise", Palette.MERCY_BOOST_BEAM_N, 0.15f, 1);
		}
	}

	/** A straight band from a to b, kept on its ends frame by frame. */
	private static void band(String key, Vec3 a, Vec3 b, String mask, Palette palette, float width, float alpha) {
		Vec3 piece = b.subtract(a);
		float len = (float) piece.length();
		if (len < 1.0E-3f) {
			return;
		}
		Particle p = Fx.attach(key, a.add(piece.scale(0.5)), () -> {
			Particle s = Fx.particle(mask, palette);
			return s == null ? null : s.shape(Shape.STREAK).life(0.1f).fade(0.02f, 0.5f).nearFade(0.6f);
		});
		if (p != null) {
			p.axis(piece).size(len, len).aspect(width / len).alpha(alpha);
		}
	}

	// ---- each tick: her held states ----

	/** Each client tick: every Mercy's staff head and beamed allies, Guardian Angel, Angelic Descent, Valkyrie, her wings. */
	public static void tick(net.minecraft.client.multiplayer.ClientLevel level) {
		for (Player player : level.players()) {
			ClientMercy.State s = ClientMercy.state(player);
			if (s == null || !player.isAlive()) {
				continue;
			}
			boolean enemy = KirikoFx.enemyOf(player);
			Posed p = posed(player);
			if (s.beamMode() != Mercy.BEAM_NONE) {
				staffGlow(level, s, p);
			}
			if (p == null || p.points.length < FEATHERS + 10) {
				continue;
			}
			if (s.has(Mercy.FLAG_GA)) {
				guardianAngel(player, p);
			}
			double vy = player.getY() - player.yo;
			boolean descending = !player.onGround() && vy < -0.07 && vy > -0.125 && !s.has(Mercy.FLAG_VALKYRIE);
			if (descending) {
				wingGlows(p, 5 / 20f, 1.5f);
			}
			if (s.has(Mercy.FLAG_VALKYRIE)) {
				valkyrie(p, enemy);
			}
			wingMotes(p);
		}
	}

	/**
	 * The staff head while the beam is on (heal 0198: yellow dots drifting off, 50 a second, and a warm golden swirl;
	 * boost 0197: a 0.3 m blue sphere 5 a second, tiny blue sparks along the staff) and the beamed ally (healed 56D3:
	 * golden swirls and rising yellow dots; boosted 0100 / 3169: a blue flash, sparks and blue shimmer rising).
	 */
	private static void staffGlow(net.minecraft.client.multiplayer.ClientLevel level, ClientMercy.State s, @Nullable Posed p) {
		boolean heal = s.beamMode() == Mercy.BEAM_HEAL;
		Vec3 head = p != null ? p.staff : null;
		if (head != null) {
			if (heal) {
				for (int i = 0; i < 2; i++) {
					Particle dot = Fx.particle("cat_heal_dot", Palette.NATURAL);
					if (dot != null) {
						Fx.add(dot.at(head).velocity(cone(UP, 50).scale(range(0.1f, 0.175f))).life(range(0.82f, 1.23f))
							.size(0.075f, 0.075f).nearFade(1).fade(0.05f, 0.5f));
					}
				}
				// ours: the data's 2 m swirl is mostly clear, 0.6 m reads as it does in game (the reference's recipe)
				if (Fx.RANDOM.nextFloat() < 6 / 20f) {
					Particle swirl = Fx.particle("cat_heal_swirl", Palette.NATURAL);
					if (swirl != null) {
						Fx.add(swirl.at(head).life(1).size(0.6f, 0.6f).spin(range(0, 6.28f), 1).alpha(0.6f).nearFade(1).fade(0.1f, 0.5f));
					}
				}
			} else {
				if (Fx.RANDOM.nextFloat() < 5 / 20f) {
					Particle ball = Fx.particle("cat_soft", Palette.MERCY_STAFF_BLUE_N);
					if (ball != null) {
						Fx.add(ball.at(head).life(0.25f).size(0.3f, 0.3f).nearFade(1).fade(0.02f, 0.6f));
					}
				}
				Particle spark = Fx.particle("cat_glow_dot", Palette.MERCY_STAFF_BLUE_N);
				if (spark != null) {
					Fx.add(spark.at(head).velocity(cone(UP, 50).scale(range(0.5f, 0.75f))).life(range(0.17f, 0.31f))
						.size(range(0.075f, 0.15f), 0.02f).nearFade(1).fade(0.02f, 0.6f));
				}
			}
		}
		if (!(level.getEntity(s.beamTarget()) instanceof LivingEntity ally)) {
			return;
		}
		Vec3 chest = chest(ally, 1);
		if (heal) {
			if (Fx.RANDOM.nextFloat() < 6 / 20f) {
				Particle swirl = Fx.particle("cat_heal_swirl", Palette.NATURAL);
				if (swirl != null) {
					Fx.add(swirl.at(chest).life(1).size(0.6f, 1.2f).spin(range(0, 6.28f), 1).alpha(0.5f).nearFade(1).fade(0.1f, 0.5f));
				}
			}
			for (int i = 0; i < 2; i++) {
				Particle dot = Fx.particle("cat_heal_dot", Palette.NATURAL);
				if (dot != null) {
					Vec3 at = chest.add(randomUnit().scale(ally.getBbWidth() * 0.6));
					Fx.add(dot.at(at).velocity(cone(UP, 50).scale(range(0.1f, 0.175f))).life(range(0.82f, 1.23f))
						.size(range(0.08f, 0.12f), 0.08f).nearFade(1).fade(0.05f, 0.5f));
				}
			}
		} else {
			if (Fx.RANDOM.nextFloat() < 7 / 20f) {
				Particle flash = Fx.particle("cat_soft", Palette.MERCY_BOOSTED);
				if (flash != null) {
					Fx.add(flash.at(chest).life(0.25f).size(1, 1).alpha(0.5f).nearFade(1.5f).fade(0.02f, 0.6f));
				}
			}
			// the rising blue chevrons (3169, a mesh in the data: ours, the soft streak rising), 8 a second
			if (Fx.RANDOM.nextFloat() < 8 / 20f) {
				Particle rise = Fx.particle("cat_streak", Palette.MERCY_BOOSTED);
				if (rise != null) {
					Vec3 at = ally.position().add(range(-0.4f, 0.4f) * ally.getBbWidth(), range(0.2f, 0.9f) * ally.getBbHeight(),
						range(-0.4f, 0.4f) * ally.getBbWidth());
					Fx.add(rise.at(at).shape(Shape.STREAK).axis(UP).velocity(UP).life(range(0.75f, 1.25f)).size(0.25f, 0.25f)
						.aspect(0.3f).nearFade(1).fade(0.1f, 0.5f));
				}
			}
			for (int i = 0; i < 4; i++) {
				Particle spark = Fx.particle("cat_glow_dot", Palette.MERCY_BOOSTED);
				if (spark != null) {
					Fx.add(spark.at(chest.add(randomUnit().scale(0.3))).velocity(cone(UP, 50).scale(range(1, 2)))
						.life(range(0.17f, 0.31f)).size(range(0.02f, 0.1f), 0.01f).nearFade(1).fade(0.02f, 0.6f));
				}
			}
		}
	}

	/**
	 * Guardian Angel (019B / 1FAF, 17E9, 6BC3): a 1.5 m orange-gold trail behind her chest (0.3 s of travel), two 0.075 m
	 * gold ribbons from her wing tips, soft yellow glows rising off both wings, and pale yellow speed lines streaming past.
	 */
	private static void guardianAngel(Player mercy, Posed p) {
		int id = mercy.getId();
		Fx.trail(2_000_000 + id * 4, p.points[CHEST], "mercy_trail", Palette.MERCY_GA_TRAIL_T, 1.5f, 0.3f, 0.6f);
		Fx.trail(2_000_000 + id * 4 + 1, p.points[WING_L], "mercy_band", Palette.MERCY_GA_TRAIL_T, 0.075f, 0.3f, 0.9f);
		Fx.trail(2_000_000 + id * 4 + 2, p.points[WING_R], "mercy_band", Palette.MERCY_GA_TRAIL_T, 0.075f, 0.3f, 0.9f);
		wingGlows(p, 15 / 20f, 1.5f);
		Vec3 motion = mercy.getDeltaMovement();
		Vec3 back = motion.lengthSqr() > 1.0E-6 ? motion.normalize().scale(-1) : mercy.getLookAngle().scale(-1);
		for (int i = 0; i < 3; i++) {
			Particle line = Fx.particle("cat_speed_line#3", Palette.MERCY_WING_GLOW);
			if (line != null) {
				Vec3 at = mercy.position().add(range(-1, 1), range(0, 2), range(-1, 1));
				Fx.add(line.at(at).shape(Shape.STREAK).axis(back).velocity(back.scale(range(4, 5))).life(range(0.22f, 0.32f))
					.size(1, 1).aspect(0.03f).alpha(0.5f).nearFade(1.5f).fade(0.05f, 0.5f));
			}
		}
	}

	/**
	 * Soft yellow glows rising off both wings (Guardian Angel's 17E9, Angelic Descent's 019C: 1.5-2.1 m, rising 0.5 m/s in
	 * a 25° cone), {@code chance} a tick each, faint (ours: 35%: a glow on the wings), and yellow dots.
	 */
	private static void wingGlows(Posed p, float chance, float size) {
		for (int w = WING_L; w <= WING_R; w++) {
			if (Fx.RANDOM.nextFloat() < chance) {
				Particle glow = Fx.particle("mercy_soft", Palette.MERCY_WING_GLOW);
				if (glow != null) {
					Fx.add(glow.at(p.points[w]).velocity(cone(UP, 25).scale(range(0.47f, 0.52f))).life(range(0.45f, 0.54f))
						.size(size * 0.8f, size).alpha(0.35f).nearFade(1.5f).fade(0.1f, 0.6f));
				}
			}
			if (Fx.RANDOM.nextFloat() < 16 / 20f) {
				Particle dot = Fx.particle("cat_heal_dot", Palette.NATURAL);
				if (dot != null) {
					Fx.add(dot.at(p.points[w]).velocity(UP.scale(range(0.13f, 0.32f))).life(range(0.2f, 0.3f)).size(0.09f, 0.09f)
						.nearFade(1).fade(0.05f, 0.5f));
				}
			}
		}
	}

	/**
	 * Valkyrie's feathers (40CD / 1F7B): on each of her 10 wing hardpoints a long gold feather streak (a thin spike),
	 * 2.2 a second, living about 1 s, so the wings shimmer; gold sparks off the wing tips. Enemies see red. Ours: 2 m
	 * rather than the data's 3.7 m quads, the spike mask being mostly empty at its ends.
	 */
	/**
	 * Valkyrie's feathers (40CD / 1F7B), each frame: on each of her 10 wing hardpoints a long gold feather streak (a thin
	 * spike) pointing out along the wing from her chest, kept on the hardpoint as she flies (the data's emitters are on
	 * her wings: they go with her), its brightness pulsing at the data's 2.2 a second so the wings shimmer. Enemies see
	 * red. Ours: 2 m rather than the data's 3.7 m quads, the spike mask being mostly empty at its ends.
	 */
	private static void feathers(int id, Posed p, boolean enemy, float time) {
		Palette gold = enemy ? Palette.MERCY_VALK_ENEMY : Palette.MERCY_VALK;
		Vec3 chest = p.points[CHEST];
		for (int i = FEATHERS; i < FEATHERS + 10; i++) {
			Vec3 out = p.points[i].subtract(chest);
			if (out.lengthSqr() < 1.0E-4) {
				continue;
			}
			Vec3 dir = out.normalize();
			Particle feather = Fx.attach("mercy_feather/" + id + "/" + i, p.points[i].add(dir.scale(1)), () -> {
				Particle f = Fx.particle("mercy_spike", gold);
				return f == null ? null : f.shape(Shape.STREAK).life(0.1f).fade(0.02f, 0.5f).nearFade(1.5f);
			});
			if (feather != null) {
				float pulse = 0.45f + 0.3f * (float) Math.sin((time / 20f) * 2.2f * Math.PI * 2 + i * 1.7f);
				feather.axis(dir).size(2, 2).aspect(0.08f).alpha(pulse);
			}
		}
	}

	/** Valkyrie's sparks off her wing tips (1F7B: 1.7-4.1 m/s sideways, 0.68-1.12 s), gold, red for enemies. */
	private static void valkyrie(Posed p, boolean enemy) {
		Palette sparks = enemy ? Palette.MERCY_VALK_SPARK_ENEMY : Palette.MERCY_VALK_SPARK;
		for (int w = WING_L; w <= WING_R; w++) {
			if (Fx.RANDOM.nextFloat() < 0.5f) {
				Particle spark = Fx.particle("spark#4", sparks);
				if (spark != null) {
					Fx.add(spark.at(p.points[w]).velocity(randomUnit().scale(range(1.7f, 4.1f))).life(range(0.68f, 1.12f))
						.size(0.12f, 0.04f).gravity(2).nearFade(1).fade(0.02f, 0.6f));
				}
			}
		}
	}

	/** Her wings at rest (0301 on her child entity 02B8): a faint gold mote off each wing tip every 4 ticks (size ours). */
	private static void wingMotes(Posed p) {
		for (int w = WING_L; w <= WING_R; w++) {
			if (Fx.RANDOM.nextFloat() < 5 / 20f) {
				Particle mote = Fx.particle("cat_soft_ball", Palette.MERCY_WING_GOLD_N);
				if (mote != null) {
					Fx.add(mote.at(p.points[w].add(0, 0.1, 0)).velocity(randomUnit().scale(range(0.19f, 0.27f))).life(range(0.8f, 1.5f))
						.size(range(0.15f, 0.3f), 0.05f).alpha(0.6f).nearFade(1.5f).fade(0.2f, 0.5f));
				}
			}
		}
	}

	// ---- the blaster (01E1 round, 01E2 muzzle, 02F1 hit) ----

	/**
	 * A blaster round in flight, each frame: a 0.45 m gold star core round a 0.16 m hot centre, a 0.2 m gold tracer about
	 * 0.08 s of travel long, sparks left behind; red for enemies. Its first frame flashes at the muzzle (01E2). False
	 * without the pack.
	 */
	public static boolean shotFlight(int id, Vec3 at, @Nullable Entity owner) {
		boolean enemy = KirikoFx.enemyOf(owner);
		Palette gold = enemy ? Palette.MERCY_SHOT_ENEMY : Palette.MERCY_SHOT;
		boolean first = Fx.trailHead(id) == null;
		Particle core = Fx.attach("blaster_shot/" + id, at, () -> {
			Particle g = Fx.particle("mercy_star", gold);
			return g == null ? null : g.life(0.1f).size(0.45f, 0.45f).spin(range(0, 6.28f), 2).nearFade(1.5f).fade(0.02f, 0.5f);
		});
		if (core == null) {
			return false;
		}
		Fx.attach("blaster_shot_hot/" + id, at, () -> {
			Particle g = Fx.particle("lucio_spark", gold);
			return g == null ? null : g.life(0.1f).size(0.16f, 0.16f).nearFade(1.5f).fade(0.02f, 0.5f);
		});
		Fx.trail(id, at, "mercy_band", gold, 0.2f, 0.08f, 0.9f);
		if (Fx.RANDOM.nextFloat() < 0.5f) {
			Particle spark = Fx.particle("lucio_spark", gold);
			if (spark != null) {
				Fx.add(spark.at(at).velocity(randomUnit().scale(range(0.25f, 0.5f))).life(range(0.09f, 0.44f))
					.size(range(0.12f, 0.18f), 0.03f).spin(0, range(1, 3)).nearFade(1.5f).fade(0.02f, 0.6f));
			}
		}
		Minecraft mc = Minecraft.getInstance();
		boolean ownView = owner == mc.player && mc.options.getCameraType().isFirstPerson();
		if (first && !ownView) {
			muzzle(at, owner, gold);
		}
		return true;
	}

	/** The muzzle flash (01E2, all on the blaster's hp 0006): flame bursts 0.5-1.1 m, a 1.2 m flare, tiny gold streaks. */
	private static void muzzle(Vec3 at, @Nullable Entity owner, Palette gold) {
		Vec3 aim = owner == null ? UP : owner.getLookAngle();
		for (int i = 0; i < 2; i++) {
			Particle flame = Fx.particle("mercy_muzzle", gold);
			if (flame != null) {
				Fx.add(flame.at(at.add(aim.scale(0.3))).shape(Shape.STREAK).axis(aim).life(range(0.08f, 0.1f)).size(range(0.5f, 1.1f), 0.5f)
					.aspect(0.5f).nearFade(1.5f).fade(0.01f, 0.6f));
			}
		}
		Particle flare = Fx.particle("lucio_spark", gold);
		if (flare != null) {
			Fx.add(flare.at(at).life(0.1f).size(1.18f, 0.6f).alpha(0.6f).nearFade(1.5f).fade(0.01f, 0.6f));
		}
		for (int i = 0; i < 10; i++) {
			Particle streak = Fx.particle("mercy_streak", gold);
			if (streak != null) {
				Vec3 dir = cone(aim, 60);
				Fx.add(streak.at(at).shape(Shape.STREAK).axis(dir).velocity(dir.scale(range(1.6f, 2.8f))).life(range(0.05f, 0.1f))
					.size(range(0.12f, 0.4f), 0.1f).aspect(0.1f).nearFade(1).fade(0.01f, 0.6f));
			}
		}
	}

	/** A round's hit (02F1): a 0.3 m orange disc and soft glow for 0.1 s, 12 tiny discs flung off the face, falling. */
	private static void hit(Vec3 at, Vec3 face) {
		Particle disc = Fx.particle("mercy_impact", Palette.NATURAL);
		if (disc != null) {
			Fx.add(disc.at(at).life(0.1f).size(0.3f, 0.3f).nearFade(1).fade(0.01f, 0.6f));
		}
		Particle glow = Fx.particle("mercy_soft", Palette.MERCY_SHOT);
		if (glow != null) {
			Fx.add(glow.at(at).life(0.1f).size(0.3f, 0.3f).nearFade(1).fade(0.01f, 0.6f));
		}
		Vec3 out = face.lengthSqr() < 1.0E-6 ? UP : face.normalize();
		for (int i = 0; i < 12; i++) {
			Particle bit = Fx.particle("mercy_impact", Palette.NATURAL);
			if (bit != null) {
				Fx.add(bit.at(at).velocity(cone(out, 45).scale(range(0.8f, 5.7f))).gravity(9.8f).life(range(0.1f, 0.3f))
					.size(range(0.06f, 0.12f), 0.03f).nearFade(1).fade(0.01f, 0.6f));
			}
		}
	}

	// ---- her moments (EffectPayload) ----

	/** Her effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Minecraft mc = Minecraft.getInstance();
		Entity owner = mc.level == null ? null : mc.level.getEntity(e.entity());
		switch (e.effect()) {
			case "blaster_hit" -> hit(at, dir.scale(-1));
			case "blaster_hit_block" -> hit(at, dir);
			case "mercy_resurrect" -> resurrectCast(at, enemy);
			case "mercy_resurrected" -> resurrected(at);
			case "mercy_flash_cast" -> {
				Posed p = owner == null ? null : posed(owner);
				if (p != null && p.staff != null) {
					ring(p.staff, 1, Palette.MERCY_FLASH_N);
				}
			}
			case "mercy_flash_heal" -> flashHeal(owner, at);
			case "mercy_valkyrie_cast" -> {
				if (owner != null) {
					valkyrieCast(owner, enemy);
				}
			}
			case "mercy_ga_start", "mercy_ga_launch" -> {
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/** A flat gold ring flash (4DB x 3203: 1 m, 0.6 s). */
	private static void ring(Vec3 at, float size, Palette palette) {
		Particle ring = Fx.particle("cat_ring", palette);
		if (ring != null) {
			Fx.add(ring.at(at).life(0.6f).size(size * 0.4f, size).easeOut().nearFade(1).fade(0.02f, 0.6f));
		}
	}

	/**
	 * Resurrect's cast on the dead ally (2106, 2 s): a golden swirl growing 2 to 5 m (ours: the data's 5.1 to 11.4 m fills
	 * the screen), a soft glow growing 2.6 to 5.5 m, gold sparkles rising; enemies see red.
	 */
	private static void resurrectCast(Vec3 soul, boolean enemy) {
		Vec3 at = soul.add(0, 0.5, 0);
		Particle swirl = Fx.particle("cat_heal_swirl", Palette.NATURAL);
		if (swirl != null) {
			Fx.add(swirl.at(at).life(2).size(2, 5).spin(0, 1).alpha(0.5f).nearFade(2).fade(0.1f, 0.3f));
		}
		Particle glow = Fx.particle("cat_glow_dot", enemy ? Palette.MERCY_RES_HALO_ENEMY : Palette.MERCY_RES_HALO);
		if (glow != null) {
			Fx.add(glow.at(at).life(2).size(2.6f, 5.5f).alpha(0.4f).nearFade(2).fade(0.1f, 0.3f));
		}
		for (int i = 0; i < 60; i++) {
			Particle sparkle = Fx.particle("mercy_glow", Palette.MERCY_RES_SPARK);
			if (sparkle != null) {
				Vec3 from = at.add(range(-1, 1), range(-0.5f, 0.5f), range(-1, 1));
				Fx.add(sparkle.at(from).velocity(UP.scale(range(0.5f, 2.2f))).life(range(0.46f, 1.75f)).size(0.09f, 0.175f)
					.nearFade(1).fade(0.1f, 0.5f));
			}
		}
	}

	/**
	 * The ally standing up (017D / 019E): a halo flashing over their head, a 1.4 m white flash, golden swirls, a cloud of
	 * soft gold dots, and golden plus crosses rising from their chest.
	 */
	private static void resurrected(Vec3 feet) {
		Vec3 chest = feet.add(0, 1.1, 0);
		Particle halo = Fx.particle("mercy_halo", Palette.NATURAL);
		if (halo != null) {
			Fx.add(halo.at(feet.add(0, 2.1, 0)).shape(Shape.FLAT).normal(UP).life(0.3f).size(0.8f, 1.1f).nearFade(1).fade(0.02f, 0.6f));
		}
		Particle flash = Fx.particle("cat_soft", Palette.MERCY_WING_GLOW);
		if (flash != null) {
			Fx.add(flash.at(chest).life(0.3f).size(1.4f, 1.4f).nearFade(1.5f).fade(0.01f, 0.6f));
		}
		for (int i = 0; i < 3; i++) {
			Particle swirl = Fx.particle("cat_heal_swirl", Palette.NATURAL);
			if (swirl != null) {
				Fx.add(swirl.at(chest).life(range(0.38f, 0.71f)).size(1.4f, 2).spin(range(0, 6.28f), 1).alpha(0.6f).nearFade(1.5f)
					.fade(0.05f, 0.5f));
			}
		}
		for (int i = 0; i < 80; i++) {
			Particle dot = Fx.particle("lucio_soft", Palette.MERCY_WING_GOLD_N);
			if (dot != null) {
				Fx.add(dot.at(chest.add(randomUnit().scale(0.4))).velocity(randomUnit().scale(range(0.4f, 1.7f))).life(range(0.15f, 0.76f))
					.size(0.16f, 0.05f).nearFade(1).fade(0.02f, 0.6f));
			}
		}
		for (int i = 0; i < 25; i++) {
			Particle plus = Fx.particle("mercy_plus", Palette.MERCY_PLUS);
			if (plus != null) {
				Fx.add(plus.at(chest).velocity(cone(UP, 60).scale(range(1.7f, 3.1f))).life(range(0.25f, 1.0f)).size(0.22f, 0.22f)
					.nearFade(1).fade(0.05f, 0.5f));
			}
		}
	}

	/**
	 * Flash Heal landing (75A7 beam, 642D burst): a straight gold band from her staff head to the ally's chest fading over
	 * 0.4 s (0.5 m: ours, the data's 1.5 m quad being a thin streak), a 1 m gold ring flash on them, a golden swirl
	 * opening 1 to 4 m (ours: 15 m), yellow dots popping out.
	 */
	private static void flashHeal(@Nullable Entity mercy, Vec3 chest) {
		Posed p = mercy == null ? null : posed(mercy);
		Vec3 from = p != null && p.staff != null ? p.staff : mercy == null ? null : mercy.getEyePosition().add(mercy.getLookAngle().scale(0.7));
		if (from != null) {
			Vec3 piece = chest.subtract(from);
			float len = (float) piece.length();
			Particle band = Fx.particle("cat_streak", Palette.MERCY_FLASH_N);
			if (band != null && len > 0.2f) {
				Fx.add(band.at(from.add(piece.scale(0.5))).shape(Shape.STREAK).axis(piece).life(0.4f).size(len, len).aspect(0.5f / len)
					.nearFade(1).fade(0.01f, 0.8f));
			}
		}
		ring(chest, 1, Palette.MERCY_FLASH_N);
		Particle swirl = Fx.particle("cat_heal_swirl", Palette.NATURAL);
		if (swirl != null) {
			Fx.add(swirl.at(chest).life(0.6f).size(1, 4).spin(0, 2).alpha(0.6f).easeOut().nearFade(1.5f).fade(0.02f, 0.6f));
		}
		for (int i = 0; i < 25; i++) {
			Particle dot = Fx.particle("cat_heal_dot", Palette.NATURAL);
			if (dot != null) {
				Fx.add(dot.at(chest).velocity(randomUnit().scale(range(0.78f, 0.86f))).life(range(0.49f, 0.71f)).size(0.1f, 0.05f)
					.nearFade(1).fade(0.02f, 0.6f));
			}
		}
	}

	/** Valkyrie's cast (2372, 0.6 s into it: drawn at once): from each wing 40 gold sparks and 10 soft glows thrown back. */
	private static void valkyrieCast(Entity mercy, boolean enemy) {
		Posed p = posed(mercy);
		Vec3 back = mercy.getLookAngle().scale(-1).add(0, 0.6, 0).normalize();
		Vec3[] wings = p != null && p.points.length > WING_R ? new Vec3[] {p.points[WING_L], p.points[WING_R]}
			: new Vec3[] {mercy.position().add(0, 1.6, 0)};
		for (Vec3 wing : wings) {
			for (int i = 0; i < 40; i++) {
				Particle spark = Fx.particle("spark#4", enemy ? Palette.MERCY_VALK_SPARK_ENEMY : Palette.MERCY_VALK_SPARK);
				if (spark != null) {
					Fx.add(spark.at(wing).velocity(cone(back, 24).scale(range(5.6f, 7.2f))).life(range(0.68f, 1.12f)).size(0.15f, 0.04f)
						.drag(0.3f).nearFade(1).fade(0.02f, 0.6f));
				}
			}
			for (int i = 0; i < 10; i++) {
				Particle glow = Fx.particle("mercy_soft", Palette.MERCY_WING_GLOW);
				if (glow != null) {
					Fx.add(glow.at(wing).velocity(cone(back, 21).scale(range(8.7f, 14))).life(range(0.57f, 1)).size(1, 1.5f).alpha(0.4f)
						.drag(0.1f).nearFade(1.5f).fade(0.05f, 0.6f));
				}
			}
		}
	}
}
