package dev.overcraft.client.effect;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.ClientLifeweaver;
import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.entity.BlossomEntity;
import dev.overcraft.entity.PetalPlatformEntity;
import dev.overcraft.entity.PetalThrowEntity;
import dev.overcraft.entity.ThornEntity;
import dev.overcraft.entity.TreeOfLifeEntity;
import dev.overcraft.hero.Hero;
import dev.overcraft.lifeweaver.Lifeweaver;
import dev.overcraft.lifeweaver.LifeweaverStats;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Lifeweaver's effects, from his effects reference (tools/reference/lifeweaver-effects.md: every size, colour, rate and
 * time there is the data's unless marked ours). Two looks: gold for the blossom's side (the blossom, its heal, the tree's
 * waves), magenta-pink for the rest (Life Grip, the platform, thorns). Held things each client tick (the charging flower,
 * the blossom and thorns in flight, the thrown egg, the platform's states, the tree, the dash, the gripped ally), Life
 * Grip's vine each frame, his moments from the server (EffectPayload).
 */
public final class LifeweaverFx {
	private static final Vec3 UP = new Vec3(0, 1, 0);
	/**
	 * The mask for his 8-sided tube ribbons (075D: the thrown egg's, the platform's rise, the dash's), whose own near-white
	 * cloud 2103C isn't in the pack: a soft band, dark at both sides, as a tube reads from the side. The wisps (lw_wisp)
	 * stay bright at its sides, and on the rise's 0.95 m ribbons they showed as grainy rectangles (found playing). The
	 * cross ribbons (07F5: the blossom's trail, the vine) keep their wisps, as in the data.
	 */
	private static final String TUBE = "mercy_band";

	/** A vine: Life Grip's, from the point in front of him to the ally (3110 on 21EE#316), since when. */
	private record Vine(int ally, Vec3 to, float start, boolean drawn) {
	}

	private static final Map<Integer, Vine> VINES = new HashMap<>();
	/** Dashes under way: the player and when (47B3's emitters live 0.26-0.36 s). */
	private static final Map<Integer, Float> DASHES = new HashMap<>();
	/** Moments to play a little later (the grip point's burst at 0.56 s). */
	private record Later(float at, Runnable run) {
	}

	private static final List<Later> LATER = new ArrayList<>();

	private LifeweaverFx() {
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

	private static Palette pink(boolean enemy) {
		return enemy ? Palette.LW_PINK_ENEMY_N : Palette.LW_PINK_N;
	}

	/** Gold reads red to an enemy too (the same LUTs' enemy rows). */
	private static Palette gold(boolean enemy) {
		return enemy ? Palette.LW_PINK_ENEMY_N : Palette.LW_GOLD_N;
	}

	/**
	 * His right hand (hp 000A, where both weapons leave and the blossom charges), from his body: ours, the posed bone
	 * isn't handed out for him (Mercy's staff is): in front of his chest a little to his right, where his charging and
	 * throwing clips hold the flower (by eye, from the third-person screenshots).
	 */
	private static Vec3 hand(Entity e, float partial) {
		Vec3 look = e.getViewVector(partial);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3 right = new Vec3(-flat.z, 0, flat.x);
		return e.getEyePosition(partial).add(right.scale(0.15)).add(0, -0.42, 0).add(flat.scale(0.45));
	}

	private static boolean ownFirstPerson(Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	// ---- sparkles, petals, flares: the pieces most of his effects are made of ----

	/** His hex sparkles (3EB7D), thrown out from at. */
	private static void sparkles(Vec3 at, int n, float speedFrom, float speedTo, Vec3 axis, double degrees, Palette palette,
								 float sizeFrom, float sizeTo, float lifeFrom, float lifeTo) {
		for (int i = 0; i < n; i++) {
			Particle p = Fx.particle("lw_sparkle", palette);
			if (p == null) {
				return;
			}
			Vec3 dir = degrees >= 180 ? randomUnit() : cone(axis, degrees);
			Fx.add(p.at(at).velocity(dir.scale(range(speedFrom, speedTo))).drag(0.3f).life(range(lifeFrom, lifeTo))
				.size(range(sizeFrom, sizeTo), 0.01f).nearFade(0.8f).fade(0.02f, 0.5f));
		}
	}

	/**
	 * Petals (3FD63 on the curved sheet D06): cream gold that blushes pink to crimson as they fall (the data's over-life
	 * row, M). Ours: each petal drawn in one of the three, the later ones redder.
	 */
	private static void petals(Vec3 at, int n, float speedFrom, float speedTo, float sizeFrom, float sizeTo, float life) {
		int[] tints = {0xFFE6C8, 0xF5A8D8, 0xE0506E};
		for (int i = 0; i < n; i++) {
			Particle p = Fx.particle("lw_petal", Palette.LW_PETAL_N);
			if (p == null) {
				return;
			}
			Fx.add(p.at(at).velocity(randomUnit().scale(range(speedFrom, speedTo))).gravity(0.6f).drag(0.4f)
				.life(life * range(0.8f, 1.1f)).size(range(sizeFrom, sizeTo), range(sizeFrom, sizeTo) * 0.7f)
				.spin(range(0, 6.28f), range(-6, 6)).tint(tints[Fx.RANDOM.nextInt(tints.length)]).nearFade(0.6f).fade(0.05f, 0.4f));
		}
	}

	/** A four-pointed star flare (E221). */
	private static void star(Vec3 at, float size, float life, Palette palette) {
		Particle p = Fx.particle("lw_star", palette);
		if (p != null) {
			Fx.add(p.at(at).life(life).size(size, size * 0.6f).spin(range(0, 6.28f), 0).nearFade(1).fade(0.02f, 0.7f));
		}
	}

	/** Thin rings (2E8AF) flying out. */
	private static void rings(Vec3 at, int n, float size, Palette palette) {
		for (int i = 0; i < n; i++) {
			Particle p = Fx.particle("lw_ring", palette);
			if (p == null) {
				return;
			}
			Fx.add(p.at(at.add(randomUnit().scale(0.15))).velocity(randomUnit().scale(range(0.5f, 1.5f))).life(range(0.25f, 0.4f))
				.size(size * 0.3f, size).easeOut().nearFade(0.8f).fade(0.02f, 0.6f));
		}
	}

	// ---- each client tick ----

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			return;
		}
		float now = Fx.now();
		for (Iterator<Later> it = LATER.iterator(); it.hasNext(); ) {
			Later l = it.next();
			if (now >= l.at) {
				it.remove();
				l.run.run();
			}
		}
		for (Player player : mc.level.players()) {
			ClientLifeweaver.State s = ClientLifeweaver.state(player);
			if (s == null || !player.isAlive()) {
				continue;
			}
			boolean enemy = KirikoFx.enemyOf(player);
			if (s.has(Lifeweaver.FLAG_CHARGING) && !s.has(Lifeweaver.FLAG_THORNS) && !ownFirstPerson(player)) {
				charging(player, s.charge(), enemy);
			}
			Float dashed = DASHES.get(player.getId());
			if (dashed != null) {
				if (now - dashed > 0.36f) {
					DASHES.remove(player.getId());
				} else {
					dashing(player, enemy);
				}
			}
		}
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof PetalPlatformEntity platform) {
				platform(platform);
			} else if (e instanceof TreeOfLifeEntity tree) {
				treeHeld(tree);
			}
		}
		grippedAllies(mc, now);
		markers(mc);
	}

	/**
	 * The charging flower, 30DE on hp 000A: a 0.3 m pale-gold glow inside a 0.6 m amber one (alpha ours: 40%), a gold wisp
	 * rising off the hand now and then (39C48, 8-12 a second), soft glows growing with the charge, and gold sparkles at
	 * full (above 90%).
	 */
	private static void charging(Player player, float charge, boolean enemy) {
		Vec3 at = hand(player, 1);
		Particle glow = Fx.attach("lw_charge/" + player.getId(), at, () -> {
			Particle p = Fx.particle("mercy_glow", enemy ? Palette.ENEMY : Palette.LW_BLOSSOM_GLOW);
			return p == null ? null : p.life(0.3f).fade(0.05f, 0.6f).nearFade(0.8f);
		});
		if (glow != null) {
			glow.size(0.3f + 0.15f * charge, 0.3f + 0.15f * charge).alpha(0.4f + 0.3f * charge);
		}
		Particle amber = Fx.attach("lw_charge_amber/" + player.getId(), at, () -> {
			Particle p = Fx.particle("mercy_glow", enemy ? Palette.ENEMY : Palette.LW_BLOSSOM_AMBER);
			return p == null ? null : p.life(0.3f).fade(0.05f, 0.6f).nearFade(0.8f);
		});
		if (amber != null) {
			amber.size(0.6f, 0.6f).alpha(0.25f + 0.2f * charge);
		}
		if (Fx.RANDOM.nextFloat() < 0.5f) {
			Particle wisp = Fx.particle("lw_leaf_flame", gold(enemy));
			if (wisp != null) {
				Fx.add(wisp.at(at.add(randomUnit().scale(0.05))).velocity(new Vec3(0, 0.15, 0)).life(range(0.5f, 0.8f))
					.size(0.18f, 0.26f).alpha(0.6f).nearFade(0.6f).fade(0.1f, 0.5f));
			}
		}
		if (charge >= 0.9f) {
			sparkles(at, 2, 0.2f, 0.6f, UP, 60, gold(enemy), 0.04f, 0.06f, 0.4f, 0.8f);
		}
	}

	/** How many of something emitted perSecond fall in dt seconds (the remainder by chance). */
	private static int count(float perSecond, float dt) {
		float n = perSecond * dt;
		int whole = (int) n;
		return whole + (Fx.RANDOM.nextFloat() < n - whole ? 1 : 0);
	}

	/**
	 * The blossom in flight, 30D0, fed each frame by its renderer: a gold core (none below 29%, up to 0.45 m at full:
	 * ours a glow, not the sphere), a gold trail 0.5 -> 1 m wide living 0.1 -> 0.25 s by the charge, gold sparkles 40 ->
	 * 100 a second, 35 petals a second, and above 70% rings shed backward at 10 m/s (10 a second). True when it drew
	 * (the local pack's masks): the item sprite stands in without it.
	 */
	public static boolean blossomFlight(BlossomEntity b, Vec3 at, float dt) {
		if (Fx.particle("lw_wisp", Palette.LW_BLOSSOM_TRAIL_N) == null) {
			return false;
		}
		float c = b.charge();
		boolean enemy = KirikoFx.enemyOf(b.getOwner());
		if (c >= 0.29f) {
			float core = 0.28f + 0.17f * Math.min(1, (c - 0.29f) / 0.71f);
			Particle p = Fx.attach("lw_blossom/" + b.getId(), at, () -> {
				Particle g = Fx.particle("lw_glow", enemy ? Palette.ENEMY : Palette.LW_BLOSSOM_TRAIL);
				return g == null ? null : g.life(0.2f).fade(0.02f, 0.5f).nearFade(0.5f);
			});
			if (p != null) {
				p.size(core * 1.6f, core * 1.6f);
			}
		}
		Fx.trail(b.getId(), at, "lw_wisp", enemy ? Palette.ENEMY : Palette.LW_BLOSSOM_TRAIL_N, 0.5f + 0.5f * c, 0.1f + 0.15f * c, 0.9f);
		Vec3 back = b.getDeltaMovement().lengthSqr() < 1.0E-6 ? UP : b.getDeltaMovement().normalize().scale(-1);
		sparkles(at, count(40 + 60 * c, dt), 0.2f, 1.2f, back, 40, gold(enemy), 0.04f, 0.07f, 0.4f, 0.85f);
		petals(at, count(35, dt), 1, 3, 0.04f, 0.12f, 0.85f);
		if (c > 0.7f && count(10, dt) > 0) {
			Particle ring = Fx.particle("lw_ring", enemy ? Palette.ENEMY : Palette.LW_HEAL_RING_N);
			if (ring != null) {
				Fx.add(ring.at(at).shape(Shape.FLAT).normal(back).velocity(back.scale(10)).life(0.4f).size(0.1f, 0.8f)
					.easeOut().nearFade(0.8f).fade(0.02f, 0.7f));
			}
		}
		return true;
	}

	/**
	 * A thorn in flight, 3119, fed each frame by its renderer: a 0.29 m purple-magenta spike (ours: a streak along its
	 * flight, not the mesh) with a 0.04 m pink tracer (ours: 0.12 s of travel, 12 m at 100 m/s, as its shorter trail).
	 */
	public static boolean thornFlight(ThornEntity t, Vec3 at) {
		boolean enemy = KirikoFx.enemyOf(t.getOwner());
		Vec3 v = t.getDeltaMovement();
		Particle head = Fx.attach("lw_thorn/" + t.getId(), at, () -> {
			Particle p = Fx.particle("lucio_spark", enemy ? Palette.LW_THORN_ENEMY_N : Palette.LW_THORN_N);
			return p == null ? null : p.shape(Shape.STREAK).life(0.08f).size(0.3f, 0.3f).aspect(0.25f).nearFade(0.5f).fade(0.01f, 0.3f);
		});
		if (head == null) {
			return false;
		}
		if (v.lengthSqr() > 1.0E-6) {
			head.velocity(v.normalize().scale(0.01));
		}
		Fx.trail(t.getId(), at, "cat_fade", enemy ? Palette.LW_THORN_ENEMY_N : Palette.LW_PINK_N, 0.05f, 0.12f, 0.9f);
		return true;
	}

	/**
	 * The thrown egg, fed each frame by its renderer (its model is his 126CC): Petal Platform's (4216: a 0.61 m pink tube
	 * trail living 0.35 s, 56-86 pink sparkles and 38-48 petals a second) or Sow the Seed's (7404: the same in gold).
	 */
	public static void eggFlight(PetalThrowEntity egg, Vec3 at, float dt) {
		boolean enemy = KirikoFx.enemyOf(egg.getOwner());
		boolean seed = egg.seed();
		Palette trail = enemy ? Palette.LW_PLATFORM_TRAIL_ENEMY_N : seed ? Palette.LW_BLOSSOM_TRAIL_N : Palette.LW_PLATFORM_TRAIL_N;
		Fx.trail(egg.getId(), at, TUBE, trail, 0.61f, 0.35f, 0.8f);
		sparkles(at, count(70, dt), 0.5f, 2, UP, 180, seed ? gold(enemy) : pink(enemy), 0.04f, 0.08f, 0.5f, 1.2f);
		petals(at, count(43, dt), 1, 3, 0.05f, 0.14f, 1.2f);
	}

	/** The five petal tips (hp 007E-0080, 0155, 0157): 1.79 m from its middle, a fifth of a turn apart. */
	private static Vec3[] petalTips(PetalPlatformEntity p) {
		Vec3[] tips = new Vec3[5];
		double top = p.getBoundingBox().maxY;
		for (int i = 0; i < 5; i++) {
			double a = Math.PI / 2 + i * Math.PI * 2 / 5;
			tips[i] = new Vec3(p.getX() + Math.cos(a) * 1.79, top, p.getZ() + Math.sin(a) * 1.79);
		}
		return tips;
	}

	/**
	 * The platform's states: armed (422B: a slowly turning 5 m pink swirl on it, pink streaks rising), rising (4982: a pink
	 * trail under each petal tip, 0.95 m wide, 0.45 s), raised (4989: a magenta glow at each tip twice a second, rising).
	 */
	private static void platform(PetalPlatformEntity p) {
		Entity owner = Minecraft.getInstance().level.getEntity(p.ownerId());
		boolean enemy = KirikoFx.enemyOf(owner);
		Vec3 top = new Vec3(p.getX(), p.getBoundingBox().maxY + 0.02, p.getZ());
		switch (p.phase()) {
			case PetalPlatformEntity.ARMED -> {
				Particle swirl = Fx.attach("lw_swirl/" + p.getId(), top, () -> {
					Particle s = Fx.particle("lw_swirl", pink(enemy));
					return s == null ? null : s.shape(Shape.FLAT).normal(UP).life(0.4f).size(5, 5).alpha(0.55f)
						.spin(0, (float) Math.PI / 2).fade(0.1f, 0.5f);
				});
				if (swirl != null) {
					swirl.at(top);
				}
				if (p.tickCount % 2 == 0) {
					Particle streak = Fx.particle("lw_sparkle", pink(enemy));
					if (streak != null) {
						Vec3 from = top.add(range(-1.4f, 1.4f), 0, range(-1.4f, 1.4f));
						Fx.add(streak.at(from).shape(Shape.STREAK).velocity(new Vec3(0, 1.76, 0)).life(range(0.6f, 1))
							.size(range(0.06f, 0.2f), 0.04f).aspect(0.3f).nearFade(0.6f).fade(0.05f, 0.5f));
					}
				}
			}
			case PetalPlatformEntity.RISING -> {
				Vec3[] tips = petalTips(p);
				for (int i = 0; i < tips.length; i++) {
					Fx.trail(-(p.getId() * 8 + i) - 3_000_000, tips[i].add(0, -0.1, 0), TUBE,
						enemy ? Palette.LW_PLATFORM_TRAIL_ENEMY_N : Palette.LW_PLATFORM_TRAIL_N, 0.95f, 0.45f, 0.7f);
				}
			}
			case PetalPlatformEntity.RAISED -> {
				if (p.tickCount % 10 == 0) {
					for (Vec3 tip : petalTips(p)) {
						Particle glow = Fx.particle("lw_glow", enemy ? Palette.LW_PETAL_CUP_ENEMY : Palette.LW_PETAL_GLOW);
						if (glow != null) {
							Fx.add(glow.at(tip.add(0, 0.1, 0)).velocity(new Vec3(0, 0.3, 0)).life(1).size(0.9f, 1).alpha(0.6f)
								.nearFade(1).fade(0.2f, 0.5f));
						}
					}
				}
			}
			default -> {
			}
		}
	}

	/**
	 * The tree held (4248): gold sparkles drifting in round its trunk (50-80 a second in the data, ours 2 a tick) and a
	 * gold glow under it (its 3.5 m ground decal).
	 */
	private static void treeHeld(TreeOfLifeEntity tree) {
		Entity owner = Minecraft.getInstance().level.getEntity(tree.ownerId());
		boolean enemy = KirikoFx.enemyOf(owner);
		Vec3 root = tree.root();
		for (int i = 0; i < 2; i++) {
			Particle p = Fx.particle("lw_sparkle", gold(enemy));
			if (p == null) {
				break;
			}
			double a = Fx.RANDOM.nextDouble() * Math.PI * 2;
			double r = range(1.2f, 3);
			Vec3 at = root.add(Math.cos(a) * r, range(0.5f, 5), Math.sin(a) * r);
			Vec3 in = root.add(0, 2.5, 0).subtract(at).normalize();
			Fx.add(p.at(at).velocity(in.scale(range(1, 2))).life(range(0.8f, 1.5f)).size(range(0.15f, 0.4f), 0.02f)
				.nearFade(1).fade(0.1f, 0.5f));
		}
		Particle ground = Fx.attach("lw_tree_ground/" + tree.getId(), root.add(0, 0.03, 0), () -> {
			Particle g = Fx.particle("cat_soft", gold(enemy));
			return g == null ? null : g.shape(Shape.FLAT).normal(UP).life(0.5f).size(7, 7).alpha(0.35f).fade(0.1f, 0.5f);
		});
		if (ground != null) {
			ground.alpha(0.35f);
		}
	}

	/** Rejuvenating Dash (47B3): cream-gold ribbons from his sash (ours: three points round his hips), petals and gold. */
	private static void dashing(Player player, boolean enemy) {
		Vec3 feet = player.position();
		float yaw = (float) Math.toRadians(player.getYRot());
		Vec3 flat = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 right = new Vec3(-flat.z, 0, flat.x);
		Vec3[] sash = {feet.add(right.scale(0.25)).add(0, 0.9, 0), feet.add(right.scale(-0.25)).add(0, 0.9, 0),
			feet.add(flat.scale(-0.2)).add(0, 0.75, 0)};
		for (int i = 0; i < sash.length; i++) {
			Fx.trail(-(player.getId() * 4 + i) - 4_000_000, sash[i], TUBE, enemy ? Palette.ENEMY : Palette.LW_DASH_N, 0.3f, 0.15f, 0.8f);
		}
		petals(sash[2], 2, 1, 3, 0.05f, 0.15f, 0.6f);
		sparkles(sash[2], 2, 1, 2, UP, 180, gold(enemy), 0.04f, 0.07f, 0.3f, 0.6f);
	}

	/**
	 * Life Grip's ally while it holds them: inside a translucent pink bud (41E9: the bud mesh 11D57, 1.96 m), a whirl of
	 * petals off their chest at 5 m/s and pink sparkles (318E), yellow-green speed lines streaming past (2EB4).
	 */
	private static void grippedAllies(Minecraft mc, float now) {
		for (Iterator<Map.Entry<Integer, Vine>> it = VINES.entrySet().iterator(); it.hasNext(); ) {
			Vine v = it.next().getValue();
			Entity ally = mc.level.getEntity(v.ally);
			if (!(ally instanceof LivingEntity living) || !living.isAlive() || now - v.start > LifeweaverStats.GRIP_MAX + 0.4f
				|| now - v.start > 0.6f && ally.position().distanceTo(v.to) < LifeweaverStats.GRIP_ARRIVE + 0.2) {
				it.remove();
				continue;
			}
			Vec3 chest = ally.position().add(0, ally.getBbHeight() * 0.6, 0);
			if (!ownFirstPerson(ally)) {
				float[] bud = Fx.mesh("lw_bud");
				Particle shell = Fx.attach("lw_bud/" + v.ally, ally.position(), () -> {
					Particle b = Fx.particle(bud != null ? "lw_glow" : "lw_ring", Palette.LW_VINE_COIL_N);
					return b == null ? null : (bud != null ? b.mesh(bud, new Vec3(1, 0, 0)) : b).life(0.3f).alpha(0.3f).fade(0.1f, 0.5f);
				});
				if (shell != null) {
					float s = ally.getBbHeight() / 1.96f * 1.05f;
					shell.size(s, s).at(ally.position());
				}
			}
			petals(chest, 2, 3, 5, 0.06f, 0.17f, 0.5f);
			sparkles(chest, 2, 2, 5, UP, 180, Palette.LW_PINK_N, 0.04f, 0.08f, 0.3f, 0.6f);
			Vec3 motion = ally.getDeltaMovement();
			if (motion.lengthSqr() > 1.0E-4) {
				for (int i = 0; i < 3; i++) {
					Particle line = Fx.particle("cat_speed_line#3", Palette.NATURAL);
					if (line != null) {
						Vec3 at = chest.add(randomUnit().scale(0.6));
						Fx.add(line.at(at).shape(Shape.STREAK).velocity(motion.normalize().scale(-4)).life(range(0.27f, 0.37f))
							.size(0.4f, 0.3f).aspect(0.08f).tint(0xD8FF60).nearFade(0.8f).fade(0.02f, 0.5f));
					}
				}
			}
		}
	}

	/**
	 * His aim markers (his own client): Tree of Life's spot while aiming (4784: the 12 m range ring, a ring at the trunk,
	 * pink sparkles rising: ours, the ghost tree isn't drawn), and the ally his charging blossom will go to.
	 */
	private static void markers(Minecraft mc) {
		if (mc.player == null || ClientHero.hero() != Hero.LIFEWEAVER || !ClientHero.controls()) {
			return;
		}
		ClientLifeweaver.State s = ClientLifeweaver.state(mc.player);
		if (s == null) {
			return;
		}
		if (s.has(Lifeweaver.FLAG_TREE_AIM)) {
			Vec3 at = Lifeweaver.treeSpot(mc.player);
			if (at != null) {
				Vec3 ground = at.add(0, 0.05, 0);
				Particle range = Fx.attach("lw_aim_range", ground, () -> {
					Particle r = Fx.particle("lw_ring", Palette.LW_PINK_N);
					return r == null ? null : r.shape(Shape.FLAT).normal(UP).life(0.25f).size(24, 24).alpha(0.7f).fade(0.05f, 0.5f);
				});
				if (range != null) {
					range.at(ground);
				}
				Particle trunk = Fx.attach("lw_aim_trunk", ground, () -> {
					Particle r = Fx.particle("lw_ring", Palette.LW_PINK_N);
					return r == null ? null : r.shape(Shape.FLAT).normal(UP).life(0.25f).size(2.2f, 2.2f).fade(0.05f, 0.5f);
				});
				if (trunk != null) {
					trunk.at(ground);
				}
				sparkles(ground.add(range(-1, 1), 0, range(-1, 1)), 2, 1.76f, 3, UP, 5, Palette.LW_PINK_N, 0.06f, 0.15f, 0.6f, 1);
			}
		}
		if (s.has(Lifeweaver.FLAG_CHARGING) && s.bloomTarget() >= 0 && mc.level.getGameTime() % 2 == 0) {
			Entity ally = mc.level.getEntity(s.bloomTarget());
			if (ally != null) {
				Vec3 top = ally.position().add(0, ally.getBbHeight() + 0.35, 0);
				Particle ring = Fx.attach("lw_target", top, () -> {
					Particle r = Fx.particle("lw_ring", Palette.LW_GOLD_N);
					return r == null ? null : r.shape(Shape.FLAT).normal(UP).life(0.2f).size(0.7f, 0.7f).fade(0.05f, 0.5f);
				});
				if (ring != null) {
					ring.at(top);
				}
			}
		}
	}

	// ---- every frame: the vines ----

	/** Each frame: each Life Grip vine, growing from its point to the ally's pelvis at 88 m/s. */
	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || VINES.isEmpty()) {
			return;
		}
		float now = Fx.now();
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		for (Vine v : VINES.values()) {
			if (!v.drawn || now - v.start > 1.5f || !(mc.level.getEntity(v.ally) instanceof LivingEntity ally)) {
				continue;
			}
			Vec3 pelvis = ally.getPosition(partial).add(0, ally.getBbHeight() * 0.58, 0);
			Vec3 piece = pelvis.subtract(v.to);
			double reach = Math.min(piece.length(), 88 * (now - v.start));
			if (reach < 0.2) {
				continue;
			}
			Vec3 end = v.to.add(piece.normalize().scale(reach));
			band("lw_vine/" + v.ally + "/wisp", v.to, end, "lw_wisp", Palette.LW_VINE_N, 0.25f);
			band("lw_vine/" + v.ally + "/line", v.to, end, "lw_vine_line", Palette.LW_VINE_N, 0.12f);
		}
	}

	/** A straight band from a to b, kept on its ends frame by frame (Mercy's beam's way). */
	private static void band(String key, Vec3 a, Vec3 b, String mask, Palette palette, float width) {
		Vec3 piece = b.subtract(a);
		float len = (float) piece.length();
		Particle p = Fx.attach(key, a.add(piece.scale(0.5)), () -> {
			Particle s = Fx.particle(mask, palette);
			return s == null ? null : s.shape(Shape.STREAK).life(0.1f).fade(0.02f, 0.5f).nearFade(0.6f);
		});
		if (p != null) {
			p.axis(piece).size(len, len).aspect(width / len);
		}
	}

	// ---- his moments (EffectPayload) ----

	/** His effects from the server; false for others' names. */
	static boolean dispatch(EffectPayload e, boolean enemy) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Minecraft mc = Minecraft.getInstance();
		Entity entity = mc.level == null ? null : mc.level.getEntity(e.entity());
		float c = e.variant() / 100f;
		switch (e.effect()) {
			case "lifeweaver_blossom_throw" -> {
				// 30E0: two star flares 0.21 -> 0.45 m by charge, 4 -> 60 tiny gold sparks, gold rings
				if (entity == null || !ownFirstPerson(entity)) {
					star(at, 0.21f + 0.24f * c, 0.5f, enemy ? Palette.ENEMY : Palette.LW_BLOSSOM_GLOW);
					sparkles(at, 4 + Math.round(20 * c), 1, 3, dir.lengthSqr() > 0 ? dir : UP, 60, gold(enemy), 0.02f, 0.04f, 0.2f, 0.4f);
				}
			}
			case "lifeweaver_blossom_lost" -> {
				// 30E6: the amber glows fizzling at the hand, gold sparkles drifting off
				Particle glow = Fx.particle("mercy_glow", enemy ? Palette.ENEMY : Palette.LW_BLOSSOM_AMBER);
				if (glow != null) {
					Fx.add(glow.at(at).life(0.33f).size(0.5f, 0.2f).alpha(0.6f).nearFade(0.8f).fade(0.02f, 0.8f));
				}
				sparkles(at, 8, 0.2f, 0.8f, UP, 180, gold(enemy), 0.03f, 0.05f, 0.4f, 0.8f);
			}
			case "lifeweaver_blossom_heal" -> {
				// 472A on the ally's chest: two 0.5 m white-gold star flares; gold rings (above 50%), petals and gold
				// sparkles by the charge (ours: a third of the data's counts)
				star(at, 0.5f, 0.7f, enemy ? Palette.ENEMY : Palette.LW_BLOSSOM_GLOW);
				star(at, 0.35f, 0.5f, gold(enemy));
				if (c >= 0.5f) {
					rings(at, Math.round(10 * (c - 0.5f) * 2), 1, enemy ? Palette.ENEMY : Palette.LW_HEAL_RING_N);
					sparkles(at, Math.round(27 * c), 4, 7, UP, 180, gold(enemy), 0.04f, 0.08f, 0.3f, 0.77f);
				}
				petals(at, Math.round(17 * c), 4, 6, 0.05f, 0.14f, 0.45f);
			}
			case "lifeweaver_thorn_fire" -> {
				// 311A (not in his own first person, as Jetpack Cat's): a hot-pink flash along the shot, thin streaks
				if (entity != null && !ownFirstPerson(entity) && dir.lengthSqr() > 1.0E-6) {
					Particle flame = Fx.particle("lw_thorn_flame#2", enemy ? Palette.ENEMY : Palette.LW_THORN_FLASH);
					if (flame != null) {
						Fx.add(flame.at(at.add(dir.normalize().scale(0.3))).shape(Shape.STREAK).velocity(dir.normalize().scale(0.01))
							.life(0.1f).size(0.9f, 0.9f).aspect(0.5f).loop(20).nearFade(0.8f).fade(0.01f, 0.5f));
					}
				}
			}
			case "lifeweaver_thorn_hit", "lifeweaver_thorn_hit_block" -> {
				// 311B: a 0.3-1.3 m purple-to-yellow burst (4 frames), 2-5 pink sparks
				Particle burst = Fx.particle("lw_hit#4", enemy ? Palette.ENEMY : Palette.LW_THORN_HIT);
				if (burst != null) {
					Fx.add(burst.at(at).life(range(0.12f, 0.25f)).size(range(0.3f, 0.6f), range(0.6f, 1)).loop(16)
						.nearFade(0.8f).fade(0.01f, 0.5f));
				}
				Vec3 out = dir.lengthSqr() > 1.0E-6 ? dir.normalize() : UP;
				sparkles(at, 3, 2, 5, e.effect().endsWith("block") ? out : out.scale(-1), 60, pink(enemy), 0.03f, 0.06f, 0.15f, 0.3f);
			}
			case "lifeweaver_superbloom" -> {
				// 6548: a big star flash on the enemy (ours: 2.5 m, the data's 5-6.6 m filled the view), a burst, sparks
				star(at, 2.5f, 0.3f, enemy ? Palette.LW_PINK_ENEMY_N : Palette.LW_PINK_N);
				Particle burst = Fx.particle("lw_hit#4", Palette.LW_THORN_HIT);
				if (burst != null) {
					Fx.add(burst.at(at).life(0.3f).size(1.5f, 2.8f).loop(14).nearFade(1).fade(0.01f, 0.5f));
				}
				sparkles(at, 18, 4.6f, 10.6f, UP, 180, pink(enemy), 0.05f, 0.1f, 0.2f, 0.4f);
			}
			case "lifeweaver_platform_deploy" -> platformBloom(at, enemy);
			case "lifeweaver_platform_destroyed" -> {
				// 499C / 4E21: a 2.5 m lens streak, leaf shards in a cone, sparkles and petals at 5 m/s. The lens is raised
				// clear of the ground (ours: 0.2 m up it was cut flat by the grass, half its quad below), the shards blended in
				// the petal glow's magenta (added, thirty pinks on one spot summed to a white cloud: found playing)
				Particle lens = Fx.particle("lw_lens", pink(enemy));
				if (lens != null) {
					Fx.add(lens.at(at.add(0, 0.45, 0)).life(0.2f).size(2.5f, 2.5f).nearFade(1).fade(0.01f, 0.6f));
				}
				for (int i = 0; i < 30; i++) {
					Particle shard = Fx.soft("lw_petal", enemy ? Palette.LW_PETAL_CUP_ENEMY : Palette.LW_PETAL_GLOW);
					if (shard != null) {
						Fx.add(shard.at(at).velocity(cone(UP, 50).scale(range(3, 4.4f))).gravity(9.8f).life(range(0.48f, 0.84f))
							.size(range(0.2f, 0.35f), 0.1f).spin(range(0, 6.28f), range(-20, 20)).nearFade(0.8f).fade(0.02f, 0.4f));
					}
				}
				sparkles(at, 30, 3, 5, UP, 180, pink(enemy), 0.04f, 0.08f, 0.3f, 0.7f);
				petals(at, 20, 3, 5, 0.06f, 0.16f, 0.7f);
			}
			case "lifeweaver_platform_blocked" -> sparkles(at.add(0, 0.3, 0), 12, 1, 3, UP, 180, pink(enemy), 0.04f, 0.08f, 0.3f, 0.6f);
			case "lifeweaver_dash" -> {
				if (entity != null) {
					DASHES.put(entity.getId(), Fx.now());
				}
			}
			case "lifeweaver_grip" -> grip(e.entity(), at, enemy);
			case "lifeweaver_tree_spawn" -> treeSpawn(at, enemy);
			case "lifeweaver_tree_pulse" -> treePulse(at, enemy);
			case "lifeweaver_tree_heal" -> {
				// 43C6 on each healed ally: two 1 m white-orange flares, gold rings, petals and sparkles
				star(at, 1, 0.7f, enemy ? Palette.ENEMY : Palette.LW_BLOSSOM_GLOW);
				rings(at, 5, 1, enemy ? Palette.ENEMY : Palette.LW_HEAL_RING_N);
				petals(at, 6, 4, 6, 0.06f, 0.14f, 0.5f);
				sparkles(at, 6, 1, 3, UP, 180, gold(enemy), 0.1f, 0.3f, 0.4f, 0.8f);
			}
			case "lifeweaver_tree_end" -> {
				// 4DCF: pale pink-white sparkles that hang in the air 2-3.6 s while the tree withers (ours: 60 of 300)
				for (int i = 0; i < 60; i++) {
					Particle p = Fx.particle("lw_sparkle", enemy ? Palette.LW_PINK_ENEMY_N : Palette.LW_PINK_N);
					if (p == null) {
						break;
					}
					Vec3 off = new Vec3(range(-3, 3), range(-2.5f, 3), range(-3, 3));
					Fx.add(p.at(at.add(off)).velocity(new Vec3(0, 0.05, 0)).life(range(2.1f, 3.6f)).size(range(0.25f, 0.5f), 0.05f)
						.twinkle(2).nearFade(1).fade(0.2f, 0.5f));
				}
			}
			case "lifeweaver_seed_land" -> {
				// 46C0: a 0.5 m star flare, a soft gold glow, petals, gold sparkles
				star(at.add(0, 0.15, 0), 0.5f, 0.7f, gold(enemy));
				petals(at.add(0, 0.15, 0), 10, 0.3f, 0.9f, 0.05f, 0.12f, 0.8f);
				sparkles(at.add(0, 0.15, 0), 8, 0.9f, 2.7f, UP, 70, gold(enemy), 0.04f, 0.07f, 0.4f, 0.8f);
			}
			case "lifeweaver_seed_taken" -> {
				// 4DFC and the blossom's heal on the ally: a 1 m gold flare, petals, gold sparks, rings
				Vec3 chest = at.add(0, 1.1, 0);
				star(chest, 1, 0.7f, enemy ? Palette.ENEMY : Palette.LW_BLOSSOM_GLOW);
				rings(chest, 5, 1, enemy ? Palette.ENEMY : Palette.LW_HEAL_RING_N);
				petals(chest, 12, 0.7f, 1.25f, 0.05f, 0.14f, 0.8f);
				sparkles(chest, 12, 4, 7, UP, 180, gold(enemy), 0.04f, 0.08f, 0.3f, 0.7f);
			}
			case "lifeweaver_platform_rise", "lifeweaver_platform_top", "lifeweaver_platform_hit", "lifeweaver_grip_none",
				 "lifeweaver_tree_cancel", "lifeweaver_tree_hit", "lifeweaver_thorn_fire_1p" -> {
				// sound only in his data
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/**
	 * The platform blooms where it lands (47B2): the flat five-petal outline (mesh 122DA, 3.83 x 4.02 m) on its top for
	 * 1.1 s, a 5 m radial burst, pink sparkles and petals thrown at 5 m/s, big pink sparkles rising (ours: a third).
	 */
	private static void platformBloom(Vec3 at, boolean enemy) {
		float[] ring = Fx.mesh("lw_flower_ring");
		Particle flower = Fx.particle(ring != null ? "lw_glow" : "lw_ring", enemy ? Palette.ENEMY : Palette.LW_PLATFORM_FLOWER_N);
		if (flower != null) {
			if (ring != null) {
				// the mesh lies in its XY plane: stood up with up = -Z, it lies flat on the platform's top
				Fx.add(flower.at(at.add(0, LifeweaverStats.PLATFORM_THICKNESS + 0.05, 0)).mesh(ring, new Vec3(1, 0, 0), new Vec3(0, 0, -1))
					.life(1.1f).size(1, 1.05f).alpha(0.9f).fade(0.02f, 0.6f));
			} else {
				Fx.add(flower.at(at.add(0, 0.4, 0)).shape(Shape.FLAT).normal(UP).life(1.1f).size(3, 4).fade(0.02f, 0.6f));
			}
		}
		Particle burst = Fx.particle("lw_burst", pink(enemy));
		if (burst != null) {
			Fx.add(burst.at(at.add(0, 0.5, 0)).life(0.27f).size(2, 5).alpha(0.8f).nearFade(1.5f).fade(0.01f, 0.6f));
		}
		sparkles(at.add(0, 0.4, 0), 30, 3, 5, UP, 180, pink(enemy), 0.04f, 0.08f, 0.4f, 0.8f);
		sparkles(at.add(0, 0.4, 0), 30, 1.2f, 4, UP, 30, pink(enemy), 0.2f, 0.4f, 0.8f, 1.4f);
		petals(at.add(0, 0.4, 0), 30, 3, 5, 0.06f, 0.18f, 1);
	}

	/**
	 * Life Grip's vine starts (3110, drawn by frame()), unless the ally started within 4 m (21EE#366); at its point a little
	 * flower pops open (4A24: 11D6E laid flat, 1.5 s), and at 0.56 s pink rings and sparkles burst there.
	 */
	private static void grip(int allyId, Vec3 to, boolean enemy) {
		Minecraft mc = Minecraft.getInstance();
		Entity ally = mc.level == null ? null : mc.level.getEntity(allyId);
		boolean drawn = ally != null && ally.position().distanceTo(to) > 4;
		VINES.put(allyId, new Vine(allyId, to, Fx.now(), drawn));
		float[] flower = Fx.mesh("lw_flower");
		Particle bloom = Fx.particle(flower != null ? "lw_glow" : "lw_petal", enemy ? Palette.ENEMY : Palette.LW_VINE_COIL_N);
		if (bloom != null) {
			if (flower != null) {
				Fx.add(bloom.at(to.add(0, 0.02, 0)).mesh(flower, new Vec3(1, 0, 0)).life(1.5f).size(0.3f, 1.5f).easeOut().fade(0.02f, 0.4f));
			} else {
				Fx.add(bloom.at(to.add(0, 0.05, 0)).shape(Shape.FLAT).normal(UP).life(1.5f).size(0.1f, 0.45f).easeOut().fade(0.02f, 0.4f));
			}
		}
		Vec3 point = to.add(0, 0.3, 0);
		LATER.add(new Later(Fx.now() + 0.56f, () -> {
			rings(point, 8, 0.35f, pink(enemy));
			sparkles(point, 18, 0.54f, 1.93f, UP, 180, pink(enemy), 0.05f, 0.1f, 0.3f, 0.77f);
		}));
	}

	/**
	 * The tree sprouts (4E07 + 43C1): a gold spiral rising up its trunk (the 5 m growth vine; ours: sparkles along a helix),
	 * big gold sparkles bursting 3-5 m/s from it, a radial burst, and the gold shell growing 8 -> 10 m.
	 */
	private static void treeSpawn(Vec3 root, boolean enemy) {
		for (int i = 0; i < 30; i++) {
			Particle p = Fx.particle("lw_sparkle", gold(enemy));
			if (p == null) {
				break;
			}
			double a = i * 0.7;
			Vec3 at = root.add(Math.cos(a) * 0.7, i * 5 / 30.0, Math.sin(a) * 0.7);
			Fx.add(p.at(at).velocity(new Vec3(0, 0.5, 0)).life(1.5f).size(0.4f, 0.1f).nearFade(1).fade(0.05f, 0.5f));
		}
		sparkles(root.add(0, 1.5, 0), 50, 3, 5, UP, 180, gold(enemy), 0.3f, 0.6f, 1, 2);
		Particle burst = Fx.particle("lw_burst", gold(enemy));
		if (burst != null) {
			Fx.add(burst.at(root.add(0, 2.5, 0)).life(0.4f).size(1, 2.3f).nearFade(1.5f).fade(0.01f, 0.6f));
		}
		shell(root, 8, 10, enemy);
	}

	/**
	 * Each pulse (47E2): the gold sphere shell (8.4 m radius; ours grown from 5 m so it reads as a wave) and the flat gold
	 * ground ring 8 m wide, 0.5 s; just before it, big gold sparkles burst from the trunk (4248 at 1.73 s; ours 40 of 300).
	 */
	private static void treePulse(Vec3 root, boolean enemy) {
		shell(root, 5, 8.4f, enemy);
		Particle ring = Fx.particle("lw_ring", enemy ? Palette.LW_TREE_WAVE_ENEMY_N : Palette.LW_TREE_RING_N);
		if (ring != null) {
			Fx.add(ring.at(root.add(0, 0.1, 0)).shape(Shape.FLAT).normal(UP).life(0.5f).size(6, 16.2f).easeOut().fade(0.02f, 0.6f));
		}
		sparkles(root.add(0, 2.5, 0), 40, 1, 2.5f, UP, 180, gold(enemy), 0.3f, 0.6f, 1, 2);
	}

	/** A gold sphere shell (mesh 1385: the pack's cat_sphere) growing from r0 to r1 m over 0.5 s, faint (ours). */
	private static void shell(Vec3 centre, float r0, float r1, boolean enemy) {
		float[] sphere = Fx.mesh("cat_sphere");
		// its three masks multiplied (the pack's lw_tree_wave): the bokeh alone lit the whole sphere, and from inside it
		// the whole view went white-gold at the sprout (found playing)
		Particle p = Fx.particle("lw_tree_wave", enemy ? Palette.LW_TREE_WAVE_ENEMY_N : Palette.LW_TREE_WAVE_N);
		if (p == null) {
			return;
		}
		if (sphere != null) {
			// the sphere stands on its base: down by half its height to centre it
			Fx.add(p.at(centre.subtract(0, r1 * 0.51, 0)).mesh(sphere, new Vec3(1, 0, 0)).life(0.5f).size(r0 * 2, r1 * 2)
				.alpha(0.35f).easeOut().fade(0.02f, 0.7f));
		} else {
			Fx.add(p.at(centre).shape(Shape.DOME).life(0.5f).size(r0, r1).alpha(0.35f).easeOut().fade(0.02f, 0.7f));
		}
	}

	/** World left: nothing carries over. */
	public static void clear() {
		VINES.clear();
		DASHES.clear();
		LATER.clear();
	}
}
