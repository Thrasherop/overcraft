package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.net.EffectPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Jetpack Cat's effects, rebuilt from Overwatch's own (tools/reference/jetpack_cat-effects.md: the emitters, sizes,
 * lifetimes, speeds and colour strips read from her effect files). Sizes in metres and times in seconds are the
 * reference's unless marked "ours". Played by EffectPayload from the server (KirikoFx hands hers on), by her shots'
 * renderer, and each client tick (the rope, the boost trails, Catnapper's target).
 */
public final class JetpackCatFx {
	/** Whether the effect being made is an enemy hero's (drawn in the enemy red and orange). */
	private static boolean enemy;
	/** Each Jetpack Cat towing or carrying someone: her entity id -> {theirs, 1 if she's an enemy's}. */
	private static final Map<Integer, int[]> TETHERS = new HashMap<>();
	/** Dev only (DevServer's "devtowed"): shows the towed one's prompt with one client on the machine, no second player to tow. */
	public static boolean devTowed;
	/** When the local player's tow began (the effects' clock), -1 not towed. */
	private static float towedAt = -1;

	/**
	 * How long a Jetpack Cat's Lifeline has towed the local player (s), -1 not towed: their Detach prompt (#85). Catnapper's carry
	 * (a tether's third value) isn't a tow: jump does nothing there.
	 */
	public static float towedFor(net.minecraft.client.player.LocalPlayer player) {
		boolean towed = devTowed;
		for (int[] tether : TETHERS.values()) {
			towed |= tether[0] == player.getId() && tether[2] == 0;
		}
		if (!towed) {
			towedAt = -1;
			return -1;
		}
		if (towedAt < 0) {
			towedAt = dev.overcraft.client.effect.Fx.now();
		}
		return dev.overcraft.client.effect.Fx.now() - towedAt;
	}

	/**
	 * Dev only (DevServer's "devtow"): ties the local player's rope to any entity, with no second player to tow them: ClientTow
	 * and the rope's look on the towed one's own screen, not the server's side of a tow. -1 lets go.
	 */
	public static void devTow(net.minecraft.client.player.LocalPlayer player, int anchor) {
		TETHERS.entrySet().removeIf(e -> e.getValue()[0] == player.getId());
		if (anchor >= 0) {
			TETHERS.put(anchor, new int[] {player.getId(), 0, 0});
			hookLeaves(anchor);
		}
	}

	private static java.util.List<String> devFrames;

	/** Dev only ("towframes"): starts recording each frame's rope ends, or stops and returns them. */
	public static String devFrames(boolean start) {
		String all = devFrames == null ? "" : String.join("\n", devFrames);
		devFrames = start ? new java.util.ArrayList<>() : null;
		return all;
	}

	/** This one hangs on a Jetpack Cat's rope (a tow or a carry). */
	public static boolean towed(int id) {
		for (int[] tether : TETHERS.values()) {
			if (tether[0] == id) {
				return true;
			}
		}
		return false;
	}

	public static final net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey<float[]> TILT = net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey.create(() -> "overcraft tow tilt");
	/** Each towed body's lean as last drawn: its "up", eased (entity id -> direction). */
	private static final Map<Integer, Vec3> TILTS = new HashMap<>();
	private static float tiltAt;
	/** Ours: how much of the rope's angle a body takes, and how fast its lean follows (a second). */
	private static final double TILT_SHARE = 0.8, TILT_RATE = 8;

	/**
	 * The lean of a body on her rope, ours (no data: mobs aren't in Overwatch): its up turned toward her, from the point it hangs by:
	 * {x, y, z, w of the turn, the point's height}, or null. Heroes pose by their own clips (HeroAnimator's "pulled"), so not those.
	 */
	public static float @Nullable [] tilt(net.minecraft.world.entity.LivingEntity entity, float partial) {
		if (TETHERS.isEmpty()) {
			TILTS.clear();
			return null;
		}
		int catId = -1;
		for (Map.Entry<Integer, int[]> e : TETHERS.entrySet()) {
			if (e.getValue()[0] == entity.getId()) {
				catId = e.getKey();
			}
		}
		net.minecraft.world.entity.Entity cat = catId < 0 ? null : entity.level().getEntity(catId);
		Vec3 last = TILTS.get(entity.getId());
		if (cat == null && last == null) {
			return null;
		}
		Vec3 want = UP;
		if (cat != null && !entity.onGround()) {
			Vec3 to = cat.getPosition(partial).subtract(entity.getPosition(partial));
			if (to.length() > 3.5) {
				to = to.normalize();
				want = UP.scale(1 - TILT_SHARE).add(to.scale(TILT_SHARE)).normalize();
			}
		}
		float now = Fx.now();
		float dt = Math.clamp(now - tiltAt, 0, 0.1f);
		tiltAt = now;
		Vec3 up = last == null ? UP : last.add(want.subtract(last).scale(Math.min(1, TILT_RATE * dt))).normalize();
		if (cat == null && up.subtract(UP).lengthSqr() < 1.0E-4) {
			TILTS.remove(entity.getId());
			return null;
		}
		TILTS.put(entity.getId(), up);
		org.joml.Quaternionf turn = new org.joml.Quaternionf().rotationTo(0, 1, 0, (float) up.x, (float) up.y, (float) up.z);
		return new float[] {turn.x, turn.y, turn.z, turn.w, entity.getBbHeight()};
	}

	/** The Jetpack Cat whose Lifeline tows the local player, or -1 (Catnapper's carry isn't a tow: the server holds that). */
	public static int towedBy(net.minecraft.client.player.LocalPlayer player) {
		for (Map.Entry<Integer, int[]> e : TETHERS.entrySet()) {
			if (e.getValue()[0] == player.getId() && e.getValue()[2] == 0) {
				return e.getKey();
			}
		}
		return -1;
	}

	/** Jetpack Cats with Lifeline switched on (her id -> seen as an enemy), towing or seeking. */
	private static final Map<Integer, Boolean> SEEKING = new HashMap<>();
	/** Jetpack Cats in Purr: her id -> when it ends (client seconds), for her range wall (70F9). */
	private static final Map<Integer, Float> PURRING = new HashMap<>();
	/** Catnapper's aim rings still alive, and the ult phase last seen: a cancelled aim takes its rings with it. */
	private static final java.util.List<Particle> AIM_RINGS = new java.util.ArrayList<>();
	private static int aimPhase;

	/**
	 * The Jetpack Cat whose Lifeline the local player could take now (Interact: LifelineGrabPayload), or -1: an ally's,
	 * on and towing nobody, with the player in its cylinder (the server checks again: sight, the lock-out).
	 */
	public static int lifelineOffer(net.minecraft.client.player.LocalPlayer player) {
		for (Map.Entry<Integer, Boolean> e : SEEKING.entrySet()) {
			if (e.getValue() || TETHERS.containsKey(e.getKey()) || e.getKey() == player.getId()) {
				continue;
			}
			if (player.level().getEntity(e.getKey()) instanceof net.minecraft.world.entity.LivingEntity cat && cat.isAlive()) {
				Vec3 to = player.position().subtract(cat.position());
				double below = dev.overcraft.jetpackcat.JetpackCatStats.LIFELINE_BELOW;
				if (to.horizontalDistance() <= dev.overcraft.jetpackcat.JetpackCatStats.LIFELINE_REACH && to.y >= -below
					&& to.y <= dev.overcraft.jetpackcat.JetpackCatStats.LIFELINE_HEIGHT - below) {
					return e.getKey();
				}
			}
		}
		return -1;
	}

	/** Jetpack Cats with Lifeline on and towing nobody (their seeking marker and hook glow: seekers). */
	public static boolean seeking(int cat) {
		return SEEKING.containsKey(cat) && !TETHERS.containsKey(cat);
	}
	/** Each Jetpack Cat's position last tick, for her speed (the boost trails). */
	private static final Map<Integer, Vec3> LAST_AT = new HashMap<>();
	/** Above this speed (m/s) she's boosting: her flight tops out at 12, Frenetic Flight at 17.8 (ours). */
	private static final double BOOST_SEEN = 12.5;
	private static final Vec3 UP = new Vec3(0, 1, 0);
	/**
	 * Each Jetpack Cat's points as last drawn (HeroModelRenderer, from her look's hardpoint table): nozzles 0075, 0077,
	 * 0078, 007A, paws 000A, 000B, each with its down axis, and the game time they were posed at.
	 */
	private record Posed(Vec3[] at, Vec3[] down, long tick) {
	}

	private static final Map<Integer, Posed> POSED = new HashMap<>();
	private static final int HIGH_LEFT = 0, HIGH_RIGHT = 1, LOW_LEFT = 2, LOW_RIGHT = 3, PAW_A = 4, PAW_B = 5, ARM = 6;

	/** Her points posed this frame or the last tick, or null (in her own first person: the 1P look has no nozzles). */
	private static @Nullable Posed posed(net.minecraft.world.entity.Entity cat) {
		Posed p = POSED.get(cat.getId());
		return p == null || cat.level().getGameTime() - p.tick > 1 ? null : p;
	}

	private JetpackCatFx() {
	}

	/** Her gold parts' palette, or the enemy view's red-to-white. */
	private static Palette gold(Palette normal) {
		return enemy ? Palette.CAT_ENEMY_HOT : normal;
	}

	/** Her blue parts' palette, or the enemy view's red-to-orange. */
	private static Palette blue(Palette normal) {
		return enemy ? Palette.CAT_ENEMY_ORANGE : normal;
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

	/** A random direction within halfAngle (radians) of axis. */
	private static Vec3 cone(Vec3 axis, double halfAngle) {
		Vec3 a = axis.lengthSqr() < 1.0E-6 ? UP : axis.normalize();
		for (int i = 0; i < 20; i++) {
			Vec3 d = randomUnit();
			if (d.dot(a) >= Math.cos(halfAngle)) {
				return d;
			}
		}
		return a;
	}

	/** Her effects by name (from KirikoFx's dispatch); false for one that isn't hers. */
	static boolean dispatch(EffectPayload e, boolean enemyOwner) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		enemy = enemyOwner;
		try {
			switch (e.effect()) {
				// a paw shot on a creature has no effect of its own (its target slot is empty); the centre shot prints
				// the paw (709F) and flashes for the enemy (7077) or the ally (7075)
				case "paw_heal", "paw_hit" -> {
				}
				case "paw_hit_centre" -> {
					pawPrint(at, dir);
					enemyHit(at, 1);
				}
				case "paw_heal_centre" -> {
					pawPrint(at, dir);
					allyHit(at);
				}
				case "paw_hit_block" -> wallHit(at, dir);
				case "purr" -> {
					purrCast(at);
					// her range wall (70F9) only if her hero setting "Hide Purr Range Ring" (v74715) is off (5A7E #267):
					// the server sends it as dx 1 (HeroOptions)
					if (!enemy && e.dx() < 0.5f) {
						PURRING.put(e.variant(), Fx.now() + (float) dev.overcraft.jetpackcat.JetpackCatStats.PURR_TIME);
					}
				}
				// one who has just come to see it (JetpackCat.showState, #96): her range wall only, for what's left
				case "purr_state" -> {
					if (!enemy) {
						PURRING.put(e.variant(), Fx.now() + e.dx());
					}
				}
				case "purr_wave" -> purrPulse(at);
				case "purr_heal" -> allyHit(at);
				case "purr_hit" -> enemyHit(at, 1);
				case "lifeline_attach" -> {
					TETHERS.put(e.variant(), new int[] {e.entity(), enemy ? 1 : 0, 0});
					hookLeaves(e.variant());
					attach(at);
				}
				case "catnapper_tether" -> {
					TETHERS.put(e.variant(), new int[] {e.entity(), enemy ? 1 : 0, 1});
					hookLeaves(e.variant());
					tetherStart(at);
				}
				case "tether_end" -> TETHERS.remove(e.variant());
				case "lifeline_on" -> SEEKING.put(e.variant(), enemy);
				// the same states, quietly, for a player who has just started to see her (JetpackCat.showState)
				case "lifeline_state" -> SEEKING.put(e.variant(), enemy);
				case "tether_state" -> TETHERS.put(e.variant(), new int[] {e.entity(), enemy ? 1 : 0, 0});
				case "carry_state" -> TETHERS.put(e.variant(), new int[] {e.entity(), enemy ? 1 : 0, 1});
				case "lifeline_off" -> SEEKING.remove(e.variant());
				case "catnapper_cast" -> {
					net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
					net.minecraft.world.entity.Entity cat = level == null ? null : level.getEntity(e.variant());
					if (cat != null) {
						cast(cat);
						DIVES.put(e.variant(), new Dive(Fx.now(), dir, enemy));
					}
				}
				case "catnapper_land" -> {
					DIVES.remove(e.variant());
					landing(at);
				}
				case "catnapper_hit" -> landingHit(at, dir);
				case "claws_out" -> claws(at, dir);
				case "transport_shield" -> {
					net.minecraft.client.player.LocalPlayer me = net.minecraft.client.Minecraft.getInstance().player;
					// shown to her alone (5A8F #1036 tests the local player: M)
					if (me != null && e.entity() == me.getId()) {
						shieldUntil = Fx.now() + (float) dev.overcraft.jetpackcat.JetpackCatStats.TRANSPORT_SHIELD_TIME;
					}
				}
				default -> {
					return false;
				}
			}
			return true;
		} finally {
			enemy = false;
		}
	}

	// ---- Biotic Pawjectiles ----

	/**
	 * A shot in flight (707A paw / 7079 centre), each frame: a gold core (0.157 / 0.34 m) in a blue halo (0.233 / 0.49
	 * m), a gold tube tracer (0.084 / 0.2 m wide, 0.07 / 0.1 s of travel), a fainter blue flat trail (0.15 / 0.35 m,
	 * 0.12 s) and sparks left behind (20 a second, 0.055-0.118 s). Its first frame flares at the paw (728B / 728C). False
	 * without the pack.
	 */
	public static boolean pawFlight(int id, Vec3 at, Vec3 velocity, boolean centre,
		net.minecraft.world.entity.@Nullable Entity owner, boolean enemyOwner, boolean ownView) {
		enemy = enemyOwner;
		try {
			boolean first = Fx.trailHead(id) == null;
			// her own shots in first person fade in over their first 4 m (60 m/s: 0.07 s): at 1 m a centre shot's halo
			// was a bright ball beside her paw (found playing)
			float near = ownView ? 4 : 1.5f;
			float core = centre ? 0.34f : 0.157f;
			float halo = centre ? 0.49f : 0.233f;
			Particle glow = Fx.attach("paw_core/" + id, at, () -> {
				Particle g = Fx.particle("cat_soft", gold(Palette.CAT_CORE));
				return g == null ? null : g.life(0.2f).size(core, core).nearFade(near).fade(0.02f, 0.5f);
			});
			if (glow == null) {
				return false;
			}
			Fx.attach("paw_halo/" + id, at, () -> {
				Particle h = Fx.particle("cat_soft", blue(Palette.CAT_HALO));
				return h == null ? null : h.life(0.2f).size(halo, halo).alpha(0.8f).nearFade(near).fade(0.02f, 0.5f);
			});
			// the data's mask pairs, baked (build_asset_pack PRODUCTS): the tracer 18E48 x 7A9, the ribbon 299E1 x 7A9
			Fx.trail(id, at, "cat_tracer", gold(Palette.CAT_TRACER), centre ? 0.2f : 0.084f, centre ? 0.1f : 0.07f, 0.9f);
			Fx.trail(-id - 1, at, "cat_trail_ribbon", blue(Palette.CAT_RIBBON), centre ? 0.35f : 0.15f, 0.12f, 0.5f);
			Fx.trailNearFade(id, near + 0.5f);
			Fx.trailNearFade(-id - 1, near + 0.5f);
			int sparks = (int) (20 * Fx.since(id) + Fx.RANDOM.nextFloat());
			for (int i = 0; i < sparks; i++) {
				Particle s = Fx.particle("spark#4", blue(Palette.CAT_SPARK));
				if (s != null) {
					// the data's spawn size, 0.15-0.504 m of the 1F35 blob (707A C15 / 7079 C13); its blob fills about half the
					// quad
					Fx.add(s.at(at.add(randomUnit().scale(0.05))).life(range(0.055f, centre ? 0.18f : 0.118f))
						.size(range(0.15f, 0.504f), 0.02f).nearFade(1.5f).fade(0.02f, 0.6f));
				}
			}
			// her own shots start at her eyes in first person, not at the view model's paw: a flare there covered the
			// whole view (found playing)
			if (first && !ownView) {
				// a shot's velocity reaches the client a frame late: on its first frame it's still zero, and the centre
				// shot's rings never showed (found playing); her aim is the shot's way then
				Vec3 way = velocity.lengthSqr() > 1.0E-6 || owner == null ? velocity : owner.getLookAngle();
				muzzle(paw(owner, at), way, centre);
			}
			return true;
		} finally {
			enemy = false;
		}
	}

	/**
	 * The paw a shot left from: of her posed paws (hardpoints 000A / 000B, as drawn), the one nearer the shot (the server
	 * swaps paws each volley and doesn't say which); where she isn't drawn, the shot's own first point.
	 */
	private static Vec3 paw(net.minecraft.world.entity.@Nullable Entity owner, Vec3 shot) {
		Posed posed = owner == null ? null : posed(owner);
		if (posed == null) {
			return shot;
		}
		Vec3 a = posed.at[4], b = posed.at[5];
		return a.distanceToSqr(shot) <= b.distanceToSqr(shot) ? a : b;
	}

	/** At the paw (728B / 728C): a 0.8 m star flare for 0.25 s; the centre shot adds a gold and a blue ring along it. */
	private static void muzzle(Vec3 at, Vec3 velocity, boolean centre) {
		Particle flare = Fx.particle("cat_flare", blue(Palette.CAT_HALO));
		if (flare != null) {
			Fx.add(flare.at(at).life(0.25f).size(0.8f, 0.5f).spin(range(0, Mth.TWO_PI), 0).nearFade(1).fade(0.02f, 0.6f));
		}
		if (!centre || velocity.lengthSqr() < 1.0E-6) {
			return;
		}
		Vec3 axis = velocity.normalize();
		// two fat tori (15A26) at the data's sizes 0.4 and 0.5 (1.38 and 1.73 m across), facing along the shot, centred
		// 0.25 m ahead (ours: grown from 60%)
		float[] torus = Fx.mesh("cat_torus");
		if (torus == null) {
			return;
		}
		float[][] rings = {{1.38f, 0}, {1.73f, 1}};
		for (float[] ring : rings) {
			Particle r = Fx.particle("cat_band", ring[1] == 0 ? gold(Palette.CAT_CORE) : blue(Palette.CAT_HALO));
			if (r != null) {
				// the torus stands on its base: back half its thickness (0.245 of its width) to centre it
				Vec3 mid = at.add(axis.scale(0.25 - 0.1225 * ring[0]));
				Fx.add(r.at(mid).mesh(torus, new Vec3(axis.z, 0, -axis.x), axis).life(0.3f).size(ring[0] * 0.6f, ring[0])
					.easeOut().fade(0.02f, 0.6f));
			}
		}
	}

	/**
	 * The centre shot on a creature (709F): a paw print of five quads, a pad and four toes, standing across the shot at
	 * the data's layout and sizes (each quad the teardrop D89 x 18E48: 0.09-0.27 m gold, life 0.22 s, and 0.09-0.47 m
	 * blue, 0.18 s; a print about 0.1 m across); flares alternating gold and blue; 3 gold streaks.
	 */
	private static void pawPrint(Vec3 at, Vec3 shot) {
		Vec3 axis = shot.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : shot.normalize();
		Vec3 side = axis.cross(UP).lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : axis.cross(UP).normalize();
		Vec3 up = side.cross(axis).normalize();
		// its hardpoint table: the pad at (0, -0.02) and 0.048 behind, toes at (+-0.02, 0.03) and (+-0.03, 0.01), toes
		// 0.4545 of the pad; each set's quads share one size, between the data's two (C45-C50, C55-C60)
		double[][] parts = {{0, -0.02, 1, 0.048}, {0.02, 0.03, 0.4545, 0}, {-0.02, 0.03, 0.4545, 0}, {0.03, 0.01, 0.4545, 0},
			{-0.03, 0.01, 0.4545, 0}};
		float gold = range(0.0909f, 0.271f);
		float blue = range(0.0909f, 0.471f);
		for (double[] part : parts) {
			Vec3 where = at.add(side.scale(part[0])).add(up.scale(part[1])).subtract(axis.scale(0.02 + part[3]));
			Particle glow = Fx.particle("cat_paw_toe", gold(Palette.CAT_PAW_PRINT));
			if (glow != null) {
				float size = (float) (gold * part[2]);
				Fx.add(glow.at(where).shape(Shape.FLAT).normal(axis).life(0.22f).size(size, size).fade(0.01f, 0.7f));
			}
			Particle rim = Fx.particle("cat_paw_toe", blue(Palette.CAT_PAW_PRINT_BLUE));
			if (rim != null) {
				float size = (float) (blue * part[2]);
				Fx.add(rim.at(where).shape(Shape.FLAT).normal(axis).life(0.18f).size(size, size).alpha(0.7f).fade(0.01f, 0.6f));
			}
		}
		for (int i = 0; i < 4; i++) {
			Particle flash = Fx.particle(i % 2 == 0 ? "cat_flare" : "cat_soft",
				i % 2 == 0 ? gold(Palette.CAT_STAMP_GOLD) : blue(Palette.CAT_STAMP_BLUE));
			if (flash != null) {
				Fx.add(flash.at(at).life(0.08f).size(range(0.19f, 0.85f), 0.3f).spin(range(0, Mth.TWO_PI), 0).fade(0.01f, 0.5f));
			}
		}
		for (int i = 0; i < 3; i++) {
			Particle s = Fx.particle("cat_streak", gold(Palette.CAT_PAW_PRINT));
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(randomUnit()).life(0.12f).size(range(0.57f, 0.67f), 0.3f)
					.aspect(0.15f).fade(0.01f, 0.6f));
			}
		}
	}

	/** On an enemy (7077 / Purr's 741C): 5-10 violet streaks flung 10-12 m/s, a 2.7 m blue star flare, soft glows. */
	private static void enemyHit(Vec3 at, float scale) {
		int n = 5 + Fx.RANDOM.nextInt(6);
		for (int i = 0; i < n; i++) {
			Particle s = Fx.particle("cat_streak", blue(Palette.CAT_FLASH_BLUE));
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(9.9f, 11.9f))).drag(0.02f)
					.life(range(0.25f, 0.37f)).size(range(0.12f, 0.18f) * 2, 0.05f).aspect(0.25f).fade(0.01f, 0.6f));
			}
		}
		Particle flare = Fx.particle("cat_flare", blue(Palette.CAT_FLASH_BLUE));
		if (flare != null) {
			Fx.add(flare.at(at).life(range(0.05f, 0.16f)).size(2.7f * scale, 1.5f * scale).spin(range(0, Mth.TWO_PI), 0)
				.nearFade(1.5f).fade(0.01f, 0.6f));
		}
		Particle glow = Fx.particle("cat_soft", blue(Palette.CAT_HALO));
		if (glow != null) {
			Fx.add(glow.at(at).life(range(0.05f, 0.2f)).size(1.5f * scale, 2.4f * scale).alpha(0.5f).nearFade(1.5f).fade(0.01f, 0.7f));
		}
	}

	/** On an ally (7075 / Purr's 741D): gold sparks, a ring opening to 2.4 m, a soft yellow glow. */
	private static void allyHit(Vec3 at) {
		int n = 5 + Fx.RANDOM.nextInt(6);
		for (int i = 0; i < n; i++) {
			Particle s = Fx.particle("cat_streak", gold(Palette.CAT_HEAL_GOLD));
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(1.6f, 4))).drag(0.1f)
					.life(range(0.16f, 0.28f)).size(range(0.12f, 0.18f) * 2, 0.05f).aspect(0.25f).fade(0.01f, 0.6f));
			}
		}
		Particle ring = Fx.particle("cat_ring", blue(Palette.CAT_HALO));
		if (ring != null) {
			Fx.add(ring.at(at).shape(Shape.FLAT).normal(UP).life(range(0.21f, 0.23f)).size(1.5f, 2.4f).easeOut().nearFade(1.5f)
				.fade(0.01f, 0.6f));
		}
		Particle glow = Fx.particle("cat_soft", gold(Palette.CAT_HEAL_GOLD));
		if (glow != null) {
			Fx.add(glow.at(at).life(0.3f).size(0.9f, 1.5f).alpha(0.6f).nearFade(1.5f).fade(0.02f, 0.7f));
		}
	}

	/** On a block (7084): two glows, a 0.25 m flat ring on the face, 4 sparks in a 35-degree cone at 2.5 m/s, a mark. */
	private static void wallHit(Vec3 at, Vec3 normal) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? UP : normal;
		for (int i = 0; i < 2; i++) {
			Particle glow = Fx.particle("lucio_glow", blue(Palette.CAT_HALO));
			if (glow != null) {
				Fx.add(glow.at(at.add(n.scale(0.05))).life(range(0.07f, 0.16f)).size(range(0.14f, 0.53f), 0.1f).fade(0.01f, 0.6f));
			}
		}
		Particle ring = Fx.particle("cat_ring", blue(Palette.CAT_RIBBON));
		if (ring != null) {
			Fx.add(ring.at(at.add(n.scale(0.02))).shape(Shape.FLAT).normal(n).life(range(0.07f, 0.16f)).size(0.2f, 0.5f).easeOut()
				.fade(0.01f, 0.6f));
		}
		for (int i = 0; i < 4; i++) {
			Particle s = Fx.particle("cat_streak", blue(Palette.CAT_SPARK));
			if (s != null) {
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(cone(n, Math.toRadians(35)).scale(range(2.4f, 2.7f)))
					.life(range(0.14f, 0.17f)).size(range(0.12f, 0.2f), 0.04f).aspect(0.3f).fade(0.01f, 0.6f));
			}
		}
		Particle decal = Fx.particle("cat_soft", gold(Palette.CAT_CORE));
		if (decal != null) {
			// the data's 0.05 m decal read as half-extents: a 0.1 m mark (M)
			Fx.add(decal.at(at.add(n.scale(0.01))).shape(Shape.FLAT).normal(n).life(0.67f).size(0.1f, 0.1f).fade(0.01f, 0.8f));
		}
	}

	// ---- Purr ----

	/**
	 * The cast (70ED): a blue sphere flashing out to the 10 m range and a violet one to 8 m (both centred 1.45 m below
	 * her, 0.45 s), a 2.7 m blue-white sphere on her body for 0.6 s, and 7 short gold streaks round her.
	 */
	private static void purrCast(Vec3 feet) {
		Vec3 centre = feet.subtract(0, 1.45, 0);
		// her spheres (1385) at 20.0 and 16.6 (19.6 and 16.3 m across), rim-lit noise (19167); without the mesh, the dome
		float[] sphere = Fx.mesh("cat_sphere");
		// (alpha: ours, halved from the dome's: she's always inside the spheres, and at the dome's the whole view went
		// cyan)
		float[][] shells = {{19.6f, 0.18f, 4, 0}, {16.3f, 0.12f, 3, 1}};
		for (float[] shell : shells) {
			Particle p = Fx.particle(sphere != null ? "cat_glow_noise" : "cat_noise",
				blue(shell[3] == 0 ? Palette.CAT_PURR_BLUE : Palette.CAT_FLASH_BLUE));
			if (p == null) {
				continue;
			}
			if (sphere != null) {
				// standing on its base: down by half its height to centre it
				Fx.add(p.at(centre.subtract(0, shell[0] * 0.51, 0)).mesh(sphere, new Vec3(1, 0, 0)).life(0.45f)
					.size(shell[2], shell[0]).alpha(shell[1]).easeOut().fade(0.02f, 0.7f));
			} else {
				Fx.add(p.at(centre).shape(Shape.DOME).life(0.45f).size(shell[2], shell[0]).alpha(shell[1]).easeOut()
					.fade(0.02f, 0.7f));
			}
		}
		Particle body = Fx.particle("cat_soft", blue(Palette.CAT_PURR_BLUE));
		if (body != null) {
			Fx.add(body.at(feet.add(0, 0.84, 0)).life(0.6f).size(1.5f, 2.7f).alpha(0.7f).easeOut().nearFade(1.5f).fade(0.02f, 0.7f));
		}
		for (int i = 0; i < 7; i++) {
			Particle line = Fx.particle("cat_purr_streak", gold(Palette.CAT_PURR_GOLD));
			if (line != null) {
				Vec3 out = cone(UP, Math.toRadians(120));
				Fx.add(line.at(feet.add(0, 0.84, 0).add(out.scale(0.6))).shape(Shape.STREAK).velocity(out.scale(0.33))
					.life(range(0.14f, 0.2f)).size(1, 0.8f).aspect(0.2f).fade(0.01f, 0.6f));
			}
		}
	}

	/**
	 * Each wave (70F3): a 1 m gold flash on her body, a gold ring 1.1 m across her middle (0.3 s) and a flat blue ring
	 * sweeping out to 6.4 m (0.33 s; ours: grown from 1 m).
	 */
	private static void purrPulse(Vec3 feet) {
		Vec3 middle = feet.add(0, 1.13, 0);
		Particle flash = Fx.particle("cat_soft", gold(Palette.CAT_PURR_GOLD));
		if (flash != null) {
			Fx.add(flash.at(feet.add(0, 0.6, 0)).life(0.6f).size(1, 0.6f).alpha(0.7f).nearFade(1.5f).fade(0.02f, 0.7f));
		}
		// her rings (torus 1BD54, stored standing like a wheel: laid flat) at the data's 1.6 and 9 (2.3 and 12.8 m across)
		float[] torus = Fx.mesh("cat_purr_torus");
		float[][] rings = {{1.6f, 2.27f, 0.3f, 0}, {2, 12.8f, 0.33f, 1}};
		for (float[] ring : rings) {
			Particle p = Fx.particle("cat_ring", ring[3] == 0 ? gold(Palette.CAT_PURR_GOLD) : blue(Palette.CAT_PURR_RING));
			if (p == null) {
				continue;
			}
			if (torus != null) {
				// its axis (Z) up: Y along -Z of the world; centred by going back half its size along that
				Vec3 tilt = new Vec3(0, 0, -1);
				Fx.add(p.at(middle.subtract(tilt.scale(ring[1] / 2))).mesh(torus, new Vec3(1, 0, 0), tilt).life(ring[2])
					.size(ring[0], ring[1]).easeOut().fade(0.01f, 0.6f));
			} else {
				Fx.add(p.at(middle).shape(Shape.FLAT).normal(UP).life(ring[2]).size(ring[0], ring[1]).easeOut().fade(0.01f, 0.6f));
			}
		}
	}

	// ---- Lifeline ----

	/** On the ally as the rope latches (7255): 13 blue star sparkles on a circle and a 0.57 m glow. */
	private static void attach(Vec3 feet) {
		Vec3 chest = feet.add(0, 1.1, 0);
		for (int i = 0; i < 13; i++) {
			double a = Mth.TWO_PI * i / 13;
			Particle star = Fx.particle("cat_star", blue(Palette.CAT_TETHER));
			if (star != null) {
				Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
				Fx.add(star.at(chest.add(out.scale(0.4))).velocity(out).life(range(0.17f, 0.27f))
					.size(range(0.12f, 0.24f), 0.05f).fade(0.01f, 0.6f));
			}
		}
		Particle glow = Fx.particle("cat_soft", blue(Palette.CAT_HALO));
		if (glow != null) {
			Fx.add(glow.at(chest).life(0.3f).size(0.57f, 0.8f).nearFade(1.5f).fade(0.01f, 0.7f));
		}
	}

	/**
	 * Each client tick: every rope, every Lifeline seeking (its marker, rings and hook glow) and every Jetpack Cat's boost
	 * trails, and each hook's way out and back. The rope (assets2.md 2: 71B0, Catnapper's 71C4) runs straight, as the data's two-point beam, from her arm's
	 * tip (0283) to the hook over the ally (their hardpoint 0520, 2.2 m up: in Minecraft, just over their head).
	 */
	public static void tethers(net.minecraft.client.multiplayer.ClientLevel level) {
		java.util.Iterator<Map.Entry<Integer, int[]>> it = TETHERS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, int[]> t = it.next();
			net.minecraft.world.entity.Entity cat = level.getEntity(t.getKey());
			net.minecraft.world.entity.Entity towed = level.getEntity(t.getValue()[0]);
			if (cat == null || towed == null || !cat.isAlive() || !towed.isAlive()) {
				it.remove();
				continue;
			}
			enemy = t.getValue()[1] == 1;
			try {
				Hook out = HOOKS.computeIfAbsent(t.getKey(), k -> new Hook(1));
				out.at = hook(towed, 1);
				out.carry = t.getValue()[2] == 1;
				out.enemy = t.getValue()[1] == 1;
				if (out.out < 1) {
					out.out = Math.min(1, out.out + HOOK_OUT / 20);
					if (out.out >= 1) {
						latch(t.getKey(), t.getValue()[0]);
					}
				} else {
					hookGlow(t.getKey(), towed);
				}
			} finally {
				enemy = false;
			}
		}
		ARCS.keySet().retainAll(TETHERS.keySet());
		// let go: the hook goes back to her arm from where it was (D #874: at 3.5 a second)
		HOOKS.entrySet().removeIf(h -> {
			if (TETHERS.containsKey(h.getKey())) {
				return false;
			}
			h.getValue().out -= HOOK_BACK / 20;
			return h.getValue().out <= 0 || level.getEntity(h.getKey()) == null;
		});
		seekers(level);
		purrWalls(level);
		boostTrails(level);
		dives(level);
		shield();
	}

	/** Her jetpack arm's tip (0283), posed; else ours, behind and above her middle (her own first person). */
	private static Vec3 armTip(net.minecraft.world.entity.Entity cat, float partial) {
		Posed p = posed(cat);
		if (p != null && p.at.length > ARM) {
			return p.at[ARM];
		}
		double yaw = Math.toRadians(cat.getYRot(partial));
		return cat.getPosition(partial).add(0, 0.8, 0).add(new Vec3(Math.sin(yaw), 0, -Math.cos(yaw)).scale(0.3));
	}

	/** Where the hook sits on a towed ally: their 0520, 2.2 m over their feet (H); ours, just over a mob's head. */
	private static Vec3 hook(net.minecraft.world.entity.Entity towed, float partial) {
		return towed.getPosition(partial).add(0, Math.min(2.2, towed.getBbHeight() + 0.25), 0);
	}

	/** The hook's way out to the ally and back, a second (D 5A8F #1175: v73806 chases 1 at 4.5, 0 at 3.5; H). */
	private static final float HOOK_OUT = 4.5f, HOOK_BACK = 3.5f;

	/**
	 * The rope's outer glow. 71B0 C22's 0.2573 m strip is 1344 x the noise 18F00, so most of its width is dark; as a plain soft line at 0.45 it
	 * read as a thick glowing bar. 0.18 is ours: about that strength less the noise's mean.
	 */
	private static final float ROPE_GLOW = 0.18f;

	/** A hook out of her arm: how far (0 at her arm, 1 on the ally) and the point it's going to or last had. */
	private static final class Hook {
		float out;
		Vec3 at = Vec3.ZERO;
		boolean carry;
		boolean enemy;

		Hook(float out) {
			this.out = out;
		}
	}

	/** Each Jetpack Cat's hook while it's out of her arm. A player who starts to see her mid-tow sees it already there. */
	private static final Map<Integer, Hook> HOOKS = new HashMap<>();

	/** Thrown (again, from where it had got to if it was on its way back). */
	private static void hookLeaves(int catId) {
		HOOKS.computeIfAbsent(catId, k -> new Hook(0));
	}

	/**
	 * Each frame, ahead of the effects' own step: the rope from where her arm is drawn to where the hook is, at this frame's
	 * partial tick. Fed once a tick it stood still between ticks while she and the ally were drawn moving: 0.4 m out at her
	 * 7 m/s, and over a metre at a boost. Straight whatever it's doing: the data's beam (5A8F #966) has two points and no
	 * sag or noise of its own (H), so it swings and goes slack only as its two ends move.
	 */
	static void ropes() {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.level == null || HOOKS.isEmpty()) {
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		for (Map.Entry<Integer, Hook> e : HOOKS.entrySet()) {
			net.minecraft.world.entity.Entity cat = mc.level.getEntity(e.getKey());
			if (cat == null || !cat.isAlive()) {
				continue;
			}
			Hook h = e.getValue();
			int[] tether = TETHERS.get(e.getKey());
			net.minecraft.world.entity.Entity towed = tether == null ? null : mc.level.getEntity(tether[0]);
			Vec3 to = towed == null ? h.at : hook(towed, partial);
			if (devFrames != null && towed != null) {
				Vec3 a = cat.getPosition(partial);
				Vec3 b = towed.getPosition(partial);
				devFrames.add(String.format(java.util.Locale.ROOT, "%.3f %.3f %.3f %.3f %.3f %.3f %.3f", Fx.now(), a.x, a.y, a.z, b.x, b.y, b.z));
			}
			Vec3 from = armTip(cat, partial);
			if (towed == mc.player && mc.options.getCameraType().isFirstPerson()) {
				// on the towed one's own screen the hook is 0.25 m over their eyes and the rope's width fans out to fill the sky
				// (found on the rope, s2_first_up): theirs starts a metre up it (ours)
				Vec3 up = from.subtract(to);
				to = to.add(up.normalize().scale(Math.min(1, up.length())));
			}
			enemy = h.enemy;
			try {
				rope(e.getKey(), from, from.add(to.subtract(from).scale(h.out)), h.carry);
			} finally {
				enemy = false;
			}
		}
	}

	/**
	 * The held rope (71B0; Catnapper's 71C4 adds a 0.32 m strip): a 0.04 m braided cable (mask 6E132) in teal, over a
	 * 0.13 m noisy cyan glow (19167) and a 0.257 m soft one, a yellow-white ball in a cyan halo at each end.
	 * Straight, as the data's two-point beam. The braid is stretched along the rope (the data repeats it: L how often).
	 */
	private static void rope(int id, Vec3 from, Vec3 to, boolean carry) {
		Vec3 piece = to.subtract(from);
		if (piece.lengthSqr() < 1.0E-6) {
			return;
		}
		Palette cable = enemy ? Palette.CAT_TETHER_ENEMY : Palette.CAT_TETHER;
		Palette glow = blue(Palette.CAT_HALO);
		strip("rope/braid/" + id, from, piece, "cat_rope_braid", cable, 0.04f, 1);
		strip("rope/noise/" + id, from, piece, "cat_glow_noise", glow, 0.13f, 0.7f);
		// (the data's 1344 strip is multiplied by a scrolling noise there; alone it drew a wedge: a soft line instead)
		strip("rope/soft/" + id, from, piece, "cat_soft", glow, 0.257f, ROPE_GLOW);
		if (carry) {
			strip("rope/carry/" + id, from, piece, "cat_cloud", glow, 0.32f, 0.35f);
		}
		ball("rope/end_a/" + id, from);
		ball("rope/end_b/" + id, to);
	}

	/**
	 * At the hook over the one she tows or carries (71B1, held on the hook at its origin 0282): a cone (1B972, size 1.48:
	 * 0.36 m) turned over (pointing up) 0.36 m below it (the data's 3 a second for 1 s, as one held), and its bubbles
	 * (spheres DBF, 1.76 m, a noisy band round their middle) rising 1.58-1.79 m/s from 1.28 m under the hook, 5 a second
	 * for 0.8 s; and round them the arcs (7454).
	 */
	private static void hookGlow(int catId, net.minecraft.world.entity.Entity towed) {
		Vec3 hook = hook(towed, 1);
		float[] cone = Fx.mesh("cat_cone");
		if (cone != null) {
			// held on the hook (spawned free, they were left behind as she towed: found playing); the cone is stored
			// pointing down and turned over, so it points up at the hook from under it
			Particle c = Fx.attach("hook_cone/" + catId, hook.subtract(0, 0.36, 0), () -> {
				Particle p = Fx.particle("cat_cone_noise", enemy ? Palette.CAT_TETHER_ENEMY : Palette.CAT_HOOK);
				return p == null ? null : p.mesh(cone, new Vec3(1, 0, 0), new Vec3(0, -1, 0)).life(0.3f).size(0.36f, 0.36f)
					.alpha(0.8f).fade(0.1f, 0.5f);
			});
			if (c != null) {
				c.at(hook.subtract(0, 0.36, 0));
			}
		}
		float[] bubble = Fx.mesh("cat_bubble");
		if (bubble != null && Fx.RANDOM.nextFloat() < 5 / 20f) {
			// the data's sphere DBF at size 1.79 (1.76 m across), its noisy band (299E3 x 16E9D x 6C057) round its middle,
			// from 1.28 m under the hook; the mesh stands on its base, so it starts half its size lower. Fading over its
			// life (its over-life row 800999B8 r3 runs cyan to dark: M); its strength 0.5 is ours (4 overlap at once)
			Particle b = Fx.particle("cat_bubble_band", enemy ? Palette.CAT_TETHER_ENEMY : Palette.CAT_BUBBLE);
			if (b != null) {
				Vec3 centre = hook.subtract(0, 1.28, 0);
				Fx.add(b.at(centre.subtract(0, BUBBLE / 2, 0)).mesh(bubble, new Vec3(1, 0, 0))
					.velocity(new Vec3(0, range(1.578f, 1.789f), 0)).life(0.8f).size(BUBBLE, BUBBLE).alpha(0.5f).nearFade(1)
					.fade(0.05f, 0.9f));
			}
		}
		arcs(catId, towed);
	}

	/** The hook's bubbles' size (71B1 C9: the sphere DBF, 0.984 m, at 1.79). */
	private static final float BUBBLE = 1.76f;
	/** Each tow's arcs (7454): when its 1 s loop started, and its two arcs alive, kept on the towed one. */
	private static final Map<Integer, Object[]> ARCS = new HashMap<>();

	/**
	 * Round the one she tows (7454, held on them, a 1 s loop): the half ring 14874 at size 2 (2 m across, standing at
	 * their feet, facing as they do: the data's ECIM 40, M), a cyan flash for 0.35 s at the loop's start (C6) and, from
	 * 0.067 s, its lines for 1 s (C8), each with its own four masks baked (build_asset_pack bake_cat_arcs).
	 */
	private static void arcs(int catId, net.minecraft.world.entity.Entity towed) {
		float[] arc = Fx.mesh("cat_half_ring");
		if (arc == null) {
			return;
		}
		float now = Fx.now();
		Object[] state = ARCS.computeIfAbsent(catId, k -> new Object[] {now - 1, null, null});
		float start = (float) state[0];
		if (now - start >= 1 || now < start) {
			start = now;
			state[0] = start;
			state[1] = arcPart(arc, "cat_arc_flash", enemy ? Palette.CAT_TETHER_ENEMY : Palette.CAT_ARC_FLASH, 0.35f);
			state[2] = null;
		}
		if (state[2] == null && now - start >= 0.0667f) {
			Particle lines = arcPart(arc, "cat_arc_lines", enemy ? Palette.CAT_TETHER_ENEMY : Palette.CAT_ARC_LINES, 1);
			state[2] = lines == null ? Boolean.FALSE : lines;
		}
		double yaw = Math.toRadians(towed instanceof net.minecraft.world.entity.LivingEntity l ? l.yBodyRot : towed.getYRot());
		Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		for (int i = 1; i <= 2; i++) {
			if (state[i] instanceof Particle p && !p.removed) {
				p.at(towed.position()).mesh(arc, side);
			}
		}
	}

	private static @Nullable Particle arcPart(float[] arc, String mask, Palette palette, float life) {
		Particle p = Fx.particle(mask, palette);
		if (p == null) {
			return null;
		}
		Fx.add(p.mesh(arc, new Vec3(1, 0, 0)).life(life).size(2, 2).alpha(0.9f).fade(0.05f, 0.5f));
		return p;
	}

	/**
	 * Purr's range (70F9), held on her while it lasts, seen by her side: an open cylinder (19181) at size 20, 9.8 m out
	 * and 20 m tall, centred 6 m below her, gold to white, lit by the 1C7A strip up its height times the noises 18F00
	 * and 19167. Not made at all when her hero setting "Hide Purr Range Ring" is on (dispatch "purr").
	 */
	private static void purrWalls(net.minecraft.client.multiplayer.ClientLevel level) {
		float now = Fx.now();
		java.util.Iterator<Map.Entry<Integer, Float>> it = PURRING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Float> e = it.next();
			net.minecraft.world.entity.Entity cat = level.getEntity(e.getKey());
			if (cat == null || !cat.isAlive() || now > e.getValue()) {
				it.remove();
				continue;
			}
			float[] wall = Fx.mesh("cat_purr_wall");
			if (wall == null) {
				continue;
			}
			// standing on its base: 6 m below her, then half its 20 m height further down
			Vec3 base = cat.position().subtract(0, 6 + 10, 0);
			Particle w = Fx.attach("purr_wall/" + e.getKey(), base, () -> {
				Particle p = Fx.particle("cat_purr_wall_band", gold(Palette.CAT_PURR_GOLD));
				return p == null ? null : p.mesh(wall, new Vec3(1, 0, 0)).life(0.3f).size(19.6f, 19.6f).alpha(0.35f)
					.fade(0.1f, 0.5f);
			});
			if (w != null) {
				w.at(base);
			}
		}
	}

	/** A straight band along piece from from, kept frame to frame by its key. */
	private static void strip(String key, Vec3 from, Vec3 piece, String mask, Palette palette, float width, float alpha) {
		float len = (float) piece.length();
		Particle band = Fx.attach(key, from.add(piece.scale(0.5)), () -> {
			Particle r = Fx.particle(mask, palette);
			return r == null ? null : r.shape(Shape.STREAK).life(0.15f).alpha(alpha).fade(0.02f, 0.5f);
		});
		if (band != null) {
			band.axis(piece).size(len, len).aspect(width / len);
		}
	}

	/** A rope end's ball (sphere 85B, 0.11 / 0.14 m): a yellow-white core in a cyan halo. */
	private static void ball(String key, Vec3 at) {
		Particle core = Fx.attach(key + "/core", at, () -> {
			Particle c = Fx.particle("cat_soft", gold(Palette.CAT_CORE));
			return c == null ? null : c.life(0.15f).size(0.11f, 0.11f).nearFade(1).fade(0.02f, 0.5f);
		});
		if (core == null) {
			return;
		}
		Fx.attach(key + "/halo", at, () -> {
			Particle h = Fx.particle("cat_soft", blue(Palette.CAT_HALO));
			return h == null ? null : h.life(0.15f).size(0.28f, 0.28f).alpha(0.7f).nearFade(1).fade(0.02f, 0.5f);
		});
	}

	/** The catch (72FA, 0.27 s, once): a bright 0.178 m cyan core and a 0.324 m glow along the rope, as it latches. */
	private static void latch(int catId, int towedId) {
		net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
		net.minecraft.world.entity.Entity cat = level == null ? null : level.getEntity(catId);
		net.minecraft.world.entity.Entity towed = level == null ? null : level.getEntity(towedId);
		if (cat == null || towed == null) {
			return;
		}
		Vec3 from = armTip(cat, 1);
		Vec3 piece = hook(towed, 1).subtract(from);
		float len = (float) piece.length();
		if (len < 1.0E-3f) {
			return;
		}
		Palette palette = enemy ? Palette.CAT_TETHER_ENEMY : Palette.CAT_TETHER;
		String[] masks = {"cat_rope", "cat_soft"};
		float[] widths = {0.178f, 0.324f};
		for (int i = 0; i < masks.length; i++) {
			Particle flash = Fx.particle(masks[i], palette);
			if (flash != null) {
				Fx.add(flash.at(from.add(piece.scale(0.5))).shape(Shape.STREAK).axis(piece).size(len, len).aspect(widths[i] / len)
					.life(0.27f).alpha(i == 0 ? 1 : 0.5f).fade(0.01f, 0.6f));
			}
		}
	}

	/**
	 * Every Jetpack Cat whose Lifeline is on and towing nobody, each tick: its marker on the ground under her (7432: the
	 * paw in a crosshair, 4 m, blue to cyan; the data's half icon mirrored whole), cyan rings opening on it (7490: the
	 * torus 15A26, 0.69 a second, twice as often with an ally in reach; their growth is ours, the data's curve L), and the
	 * glow at her arm's tip (6EA2: the cone, pale blue, hanging under it). Only her allies see the marker and rings (it
	 * has no enemy colours).
	 */
	private static void seekers(net.minecraft.client.multiplayer.ClientLevel level) {
		for (Map.Entry<Integer, Boolean> e : SEEKING.entrySet()) {
			int id = e.getKey();
			net.minecraft.world.entity.Entity cat = level.getEntity(id);
			if (TETHERS.containsKey(id) || cat == null || !cat.isAlive()) {
				continue;
			}
			enemy = e.getValue();
			try {
				Vec3 top = cat.position().add(0, 1, 0);
				net.minecraft.world.phys.HitResult hit = level.clip(new net.minecraft.world.level.ClipContext(top, top.add(0, -16, 0),
					net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, cat));
				if (!enemy && hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
					Vec3 ground = hit.getLocation();
					Fx.attach("lifeline_marker/" + id, ground.add(0, 0.05, 0), () -> {
						Particle m = Fx.particle("cat_paw_marker_full", Palette.CAT_MARKER);
						return m == null ? null : m.shape(Shape.FLAT).normal(UP).life(0.15f).size(4, 4).fade(0.05f, 0.5f);
					});
					float[] torus = Fx.mesh("cat_torus");
					double rate = allyInReach(level, cat) ? 1.38 : 0.69;
					if (torus != null && Fx.RANDOM.nextFloat() < rate / 20) {
						// the 299E3 noise times the 1C7A strip, baked (build_asset_pack PRODUCTS): a lit rim, not a solid tube
						Particle ring = Fx.particle("cat_ring_band", Palette.CAT_MARKER);
						if (ring != null) {
							Fx.add(ring.at(ground.add(0, 0.3, 0)).mesh(torus, new Vec3(1, 0, 0)).life(1).size(1.2f, 3.67f).easeOut()
								.alpha(0.8f).fade(0.1f, 0.6f));
						}
					}
				}
				float[] coneMesh = Fx.mesh("cat_cone");
				if (coneMesh != null && Fx.RANDOM.nextFloat() < 3 / 20f) {
					// the cone 1B972 with its gradient (2C41F, an 8 x 256 strip along it), kept on her arm's tip
					Particle cone = Fx.particle("cat_cone_noise", enemy ? Palette.CAT_TETHER_ENEMY : Palette.CAT_HOOK);
					if (cone != null) {
						Vec3 tip = armTip(cat, 1);
						Fx.add(cone.at(tip.add(0, -0.58, 0)).mesh(coneMesh, new Vec3(1, 0, 0)).life(1).size(0.3f, 0.3f)
							.alpha(0.8f).fade(0.1f, 0.7f));
					}
				}
			} finally {
				enemy = false;
			}
		}
	}

	/** An ally of hers in Lifeline's cylinder (the rings pulse faster: the data's dataflow CEC). */
	private static boolean allyInReach(net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.world.entity.Entity cat) {
		double r = dev.overcraft.jetpackcat.JetpackCatStats.LIFELINE_REACH;
		double below = dev.overcraft.jetpackcat.JetpackCatStats.LIFELINE_BELOW;
		for (net.minecraft.world.entity.LivingEntity other : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
			cat.getBoundingBox().inflate(r, dev.overcraft.jetpackcat.JetpackCatStats.LIFELINE_HEIGHT, r))) {
			Vec3 to = other.position().subtract(cat.position());
			if (other != cat && cat instanceof net.minecraft.world.entity.LivingEntity c && dev.overcraft.hero.Allies.isAlly(c, other)
				&& to.horizontalDistance() <= r && to.y >= -below && to.y <= dev.overcraft.jetpackcat.JetpackCatStats.LIFELINE_HEIGHT - below) {
				return true;
			}
		}
		return false;
	}

	/** The rope latching onto the mob she carries (72BE): blue spheres flashing to 4.4 and 6.3 m and a 10 m disc. */
	private static void tetherStart(Vec3 feet) {
		Vec3 chest = feet.add(0, 1, 0);
		for (float size : new float[] {4.4f, 6.3f}) {
			Particle p = Fx.particle("cat_soft", blue(Palette.CAT_PURR_BLUE));
			if (p != null) {
				Fx.add(p.at(chest).life(0.3f).size(size * 0.3f, size).alpha(0.5f).easeOut().nearFade(2).fade(0.01f, 0.7f));
			}
		}
		Particle disc = Fx.particle("cat_noise", blue(Palette.CAT_HALO));
		if (disc != null) {
			Fx.add(disc.at(feet.add(0, 0.05, 0)).shape(Shape.FLAT).normal(UP).life(0.25f).size(4, 10.3f).alpha(0.4f).easeOut()
				.fade(0.01f, 0.7f));
		}
	}

	// ---- Frenetic Flight ----

	/**
	 * Frenetic Flight (7067), for every Jetpack Cat moving faster than her flight can: two cyan ribbon trails from each
	 * side of her jetpack, 0.073 and 0.173 m wide, white at the head, 0.2-0.32 s long, from her low nozzles 0078 / 007A
	 * as posed (where she isn't drawn, as in her own first person: ours, 0.5 m to each side and 0.3 m behind).
	 */
	private static void boostTrails(net.minecraft.client.multiplayer.ClientLevel level) {
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (net.minecraft.world.entity.player.Player player : level.players()) {
			if (!"jetpack_cat".equals(player.getAttached(dev.overcraft.hero.HeroManager.HERO)) || !player.isAlive()) {
				continue;
			}
			int id = player.getId();
			seen.add(id);
			Vec3 now = player.position();
			Vec3 before = LAST_AT.put(id, now);
			if (before == null || now.distanceTo(before) * 20 < BOOST_SEEN) {
				continue;
			}
			enemy = KirikoFx.enemyOf(player);
			try {
				double yaw = Math.toRadians(player.getYRot());
				Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
				Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
				Palette palette = enemy ? Palette.CAT_BOOST_ENEMY : Palette.CAT_BOOST;
				Posed posed = posed(player);
				for (int s = -1; s <= 1; s += 2) {
					Vec3 nozzle = posed != null ? posed.at[s < 0 ? LOW_LEFT : LOW_RIGHT]
						: now.add(0, 0.7, 0).add(side.scale(0.5 * s)).add(back.scale(0.3));
					int trail = 1_000_000 + id * 4 + (s < 0 ? 0 : 2);
					Fx.trail(trail, nozzle, "cat_trail", palette, 0.073f, 0.2f, 0.9f);
					Fx.trail(trail + 1, nozzle, "cat_trail", palette, 0.173f, 0.32f, 0.6f);
				}
			} finally {
				enemy = false;
			}
		}
		LAST_AT.keySet().retainAll(seen);
	}

	/**
	 * Her jetpack, every frame she's drawn (HeroModelRenderer hands in her points as posed): the idle flames (7064, "always
	 * while flying") on the high nozzles 0075 / 0077. Each is the data's open tube 1B580 (0.128 m wide, 0.17 m long) in
	 * three layers: a teal one and a gold one at 1.1, a white-hot core at 0.55, its noise scrolling down it; a 0.08 m glow
	 * on each low nozzle (0078 / 007A); sparks from 0.13 m under the nozzle, 50 gold and 45 pale a second of 0.011-0.014 m
	 * blobs; an exhaust ring (0.2 m) 4 times a second drifting down. Her heat haze isn't drawn. Out of hero mode she
	 * walks: no flames.
	 */
	public static void jetpack(net.minecraft.world.entity.LivingEntity cat, Vec3[] at, Vec3[] down) {
		POSED.put(cat.getId(), new Posed(at, down, cat.level().getGameTime()));
		if (Boolean.FALSE.equals(cat.getAttached(dev.overcraft.hero.HeroManager.HERO_MODE))) {
			return;
		}
		enemy = KirikoFx.enemyOf(cat);
		try {
			int id = cat.getId();
			float dt = Fx.since(-(id * 8 + 7) - 1);
			for (int n = HIGH_LEFT; n <= HIGH_RIGHT; n++) {
				Vec3 nozzle = at[n];
				Vec3 axis = down[n];
				// C24 (1.1, deep blue to teal, steady), C41 (1.1, gold, 4 a second), C30 (0.55, blue to white-hot)
				flame("jetpack_flame/" + id + "/" + n + "/0", nozzle, axis, 1.1f, blue(Palette.CAT_FLAME_TEAL), 0.7f, false);
				flame("jetpack_flame/" + id + "/" + n + "/1", nozzle, axis, 1.1f, gold(Palette.CAT_FLAME_GOLD), 0.8f, true);
				flame("jetpack_flame/" + id + "/" + n + "/2", nozzle, axis, 0.55f, gold(Palette.CAT_FLAME), 1, false);
				Vec3 under = nozzle.add(axis.scale(0.1292));
				// C44 / C47: the blobs 18BC2 stretched 6 along their way (a6DFA 5.995); their speed is an engine input
				// (ECIN): ours, 1 m/s down
				for (int kind = 0; kind < 2; kind++) {
					int sparks = (int) ((kind == 0 ? 50 : 45) * dt + Fx.RANDOM.nextFloat());
					Palette palette = enemy ? Palette.CAT_SPARK_ENEMY : kind == 0 ? Palette.CAT_SPARK_GOLD : Palette.CAT_SPARK_PALE;
					for (int i = 0; i < sparks; i++) {
						Particle spark = Fx.particle("cat_blob#4", palette);
						if (spark != null) {
							float size = range(0.0114f, 0.0136f);
							Fx.add(spark.at(under).shape(Shape.STREAK).velocity(axis.scale(1).add(randomUnit().scale(0.3)))
								.life(range(0.0701f, 0.25f)).size(size * 6, size * 6).aspect(1 / 6f).nearFade(0.5f).fade(0.02f, 0.6f));
						}
					}
				}
				if (Fx.RANDOM.nextFloat() < 4 * dt) {
					Particle ring = Fx.particle("cat_ring", blue(Palette.CAT_FLAME));
					if (ring != null) {
						Fx.add(ring.at(under).shape(Shape.FLAT).normal(axis).velocity(axis.scale(0.12)).life(0.35f)
							.size(0.2f, 0.4f).alpha(0.5f).easeOut().nearFade(0.5f).fade(0.05f, 0.6f));
					}
				}
			}
			for (int n = LOW_LEFT; n <= LOW_RIGHT; n++) {
				Vec3 glowAt = at[n];
				Fx.attach("jetpack_glow/" + id + "/" + n, glowAt, () -> {
					Particle g = Fx.particle("cat_soft", blue(Palette.CAT_FLAME));
					return g == null ? null : g.life(0.1f).size(0.08f, 0.08f).alpha(0.8f).nearFade(0.5f).fade(0.02f, 0.5f);
				});
			}
		} finally {
			enemy = false;
		}
	}

	/** The flame tube 1B580: 0.128 m wide, 0.17 m long (its length over its width). */
	private static final float FLAME_LONG = 0.17f / 0.128f;

	/**
	 * One layer of a nozzle's flame, kept on it frame by frame: the data's open tube 1B580 at its size (1.1: 0.14 m wide,
	 * 0.19 m long) hanging from the nozzle along its down axis, its masks (2C41F x 7A9 x the noise 299E3) baked into 8
	 * frames of the noise scrolling down it (build_asset_pack bake_cat_flame), looped. The gold layer is blended: added
	 * over bright grass it turned lime (found playing).
	 */
	private static void flame(String key, Vec3 from, Vec3 axis, float scale, Palette palette, float alpha, boolean blended) {
		float[] tube = Fx.mesh("cat_flame");
		if (tube == null) {
			return;
		}
		float width = 0.128f * scale;
		// the mesh stands on its far end (its V 1) and reaches up to the nozzle (V 0)
		Vec3 tip = from.add(axis.scale(width * FLAME_LONG));
		Particle p = Fx.attach(key, tip, () -> {
			Particle f = blended ? Fx.soft("cat_flame_tube#8", palette) : Fx.particle("cat_flame_tube#8", palette);
			return f == null ? null : f.loop(20).life(0.1f).fade(0.02f, 0.5f).nearFade(0.5f);
		});
		if (p != null) {
			p.mesh(tube, axis.cross(UP).lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : axis.cross(UP), axis.scale(-1)).size(width, width)
				.alpha(alpha);
		}
	}

	/** A flame tube flaring once, not kept on the nozzle (the cast 7373, its life 0.13-0.2 s). */
	private static void flare(Vec3 from, Vec3 axis, float scale, Palette palette, float life, boolean blended) {
		float[] tube = Fx.mesh("cat_flame");
		Particle f = tube == null ? null : blended ? Fx.soft("cat_flame_tube#8", palette) : Fx.particle("cat_flame_tube#8", palette);
		if (f == null) {
			return;
		}
		float width = 0.128f * scale;
		Fx.add(f.at(from.add(axis.scale(width * FLAME_LONG))).mesh(tube, axis.cross(UP).lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0)
			: axis.cross(UP), axis.scale(-1)).loop(20).size(width, width).life(life).nearFade(0.5f).fade(0.02f, 0.6f));
	}

	// ---- Catnapper ----

	/**
	 * Catnapper's aim, each tick while she aims (71B3 at the target point, shown to her): cyan rings about 3-4 m across
	 * pulsing out 3 times a second, a deep blue disc on a darkened spot under them, and from each paw three thin lines to
	 * it (71AF).
	 */
	/** Dev only (DevServer's "state"): where Catnapper's aim marker was last drawn. */
	public static @Nullable Vec3 aimSeen;

	public static void aim(net.minecraft.client.Minecraft mc) {
		int phase = mc.player == null || dev.overcraft.client.ClientHero.hero() != dev.overcraft.hero.Hero.JETPACK_CAT ? 0
			: dev.overcraft.client.ClientHero.sync().mode() & 7;
		if (phase != aimPhase) {
			// the marker is an entity (6DF5) that goes with the aim: on a cancel its rings go too (they kept opening in
			// front of her in first person for 1.5 s, found playing); after the confirm they stay to mark the landing
			if (aimPhase == dev.overcraft.jetpackcat.JetpackCat.ULT_AIM && phase == dev.overcraft.jetpackcat.JetpackCat.ULT_NONE) {
				AIM_RINGS.forEach(r -> r.age = r.life);
			}
			AIM_RINGS.clear();
			aimPhase = phase;
		}
		if (phase != dev.overcraft.jetpackcat.JetpackCat.ULT_AIM) {
			return;
		}
		Vec3 at = dev.overcraft.jetpackcat.JetpackCat.diveTarget(mc.player);
		if (at == null) {
			return;
		}
		Vec3 ground = at.add(0, 0.06, 0);
		aimSeen = ground;
		// the rings (71B3: the torus 15A26 at size 1.19, 3 a second, 1.5 s, cyan to blue) and two ground glows (4C09,
		// 0.45 and 1 m)
		float[] torus = Fx.mesh("cat_torus");
		if (torus != null && Fx.RANDOM.nextFloat() < 3 / 20f) {
			// masked by the 1C7A strip times the 18E48 cloud, as the data pairs them (build_asset_pack PRODUCTS): a lit rim
			Particle ring = Fx.particle("cat_nap_ring", Palette.CAT_NAP_MARKER);
			if (ring != null) {
				Fx.add(ring.at(ground.add(0, 0.1, 0)).mesh(torus, new Vec3(1, 0, 0)).life(1.5f).size(1.5f, 4.1f).easeOut()
					.alpha(0.85f).fade(0.1f, 0.6f));
				AIM_RINGS.add(ring);
			}
		}
		// its two decals (4C09): 0.45 m deep blue (800C0DD7 r1 #00002F to #005CF4), and a 1 m one near black at half
		// strength (8002F509 r1 / r2), which darkens the ground: drawn blended
		Fx.attach("catnapper_aim_disc/0", ground, () -> {
			Particle d = Fx.particle("cat_soft", Palette.CAT_STAMP_BLUE);
			return d == null ? null : d.shape(Shape.FLAT).normal(UP).life(0.3f).size(0.45f, 0.45f).fade(0.05f, 0.5f);
		});
		Fx.attach("catnapper_aim_disc/1", ground.subtract(0, 0.01, 0), () -> {
			Particle d = Fx.soft("cat_soft", Palette.NATURAL);
			return d == null ? null : d.shape(Shape.FLAT).normal(UP).tint(0x000002).life(0.3f).size(1, 1).alpha(0.5f).fade(0.05f, 0.5f);
		});
		// the lines (71AF) from her paws (000A / 000B) as posed; ours, beside her below the eyes, without the pose
		net.minecraft.world.entity.player.Player cat = mc.player;
		Posed posed = posed(cat);
		double yaw = Math.toRadians(cat.getYRot());
		Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		for (int s = -1; s <= 1; s += 2) {
			Vec3 paw = posed != null ? posed.at[s < 0 ? PAW_A : PAW_B] : cat.getEyePosition().add(side.scale(0.25 * s)).add(0, -0.35, 0);
			Vec3 piece = ground.subtract(paw);
			if (piece.length() < 0.1f) {
				continue;
			}
			// its three ribbons (71AF C8 / C11 / C14): 0.01 m and 0.02 m of the 299E3 noise, cyan and cyan to white, and
			// 0.0455 m of the 1344 strip times 18E48, blue
			strip("catnapper_line/" + s + "/a", paw, piece, "cat_ring_line", Palette.CAT_TETHER, 0.01f, 1);
			strip("catnapper_line/" + s + "/b", paw, piece, "cat_ring_line", Palette.CAT_FLASH_BLUE, 0.02f, 1);
			strip("catnapper_line/" + s + "/c", paw, piece, "cat_nap_line", Palette.CAT_NAP_MARKER, 0.0455f, 0.9f);
		}
	}

	/** A Catnapper on its way: when she confirmed (the cast), her way to the point, seen as an enemy's. */
	private record Dive(float start, Vec3 way, boolean enemy) {
	}

	/** Jetpack Cats in Catnapper's windup or dive: her id -> the dive (until "catnapper_land" or it runs out). */
	private static final Map<Integer, Dive> DIVES = new HashMap<>();
	/** Catnapper's windup (graph 5B7B: 0.35 s) and the server's longest dive (JetpackCat.DIVE_LIMIT, 1.5 s). */
	private static final float WINDUP = 0.35f, DIVE_LONGEST = 1.5f;

	/** Her two high nozzles (0075 / 0077) and their down axes as posed; where she isn't drawn, behind her middle (ours). */
	private static Vec3[][] nozzles(net.minecraft.world.entity.Entity cat) {
		Posed p = posed(cat);
		if (p != null) {
			return new Vec3[][] {{p.at[HIGH_LEFT], p.at[HIGH_RIGHT]}, {p.down[HIGH_LEFT], p.down[HIGH_RIGHT]}};
		}
		double yaw = Math.toRadians(cat.getYRot());
		Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
		Vec3 mid = cat.position().add(0, 0.9, 0).add(back.scale(0.25));
		Vec3 down = new Vec3(0, -1, 0);
		return new Vec3[][] {{mid.add(side.scale(-0.15)), mid.add(side.scale(0.15))}, {down, down}};
	}

	/**
	 * Catnapper's cast (7373, at the start of the 0.35 s windup), on both nozzles: the flames flare (the tube at 0.55
	 * white-hot and at 1.1 teal and gold, 0.13-0.2 s), two exhaust rings each (the torus 1BD54 at 0.05 cyan and 0.07
	 * gold, 1C7A x 18E48) shot out along the nozzle at 6.9 m/s for 0.15 s, and a gold bell (19370 at 1.1: 2.2 m wide, 1.43
	 * m tall, its wide end 0.43 m under her feet) for 0.2 s.
	 */
	private static void cast(net.minecraft.world.entity.Entity cat) {
		Vec3[][] noz = nozzles(cat);
		float[] torus = Fx.mesh("cat_purr_torus");
		for (int n = 0; n < 2; n++) {
			Vec3 at = noz[0][n], axis = noz[1][n];
			flare(at, axis, 0.55f, gold(Palette.CAT_FLAME), 0.13f, false);
			flare(at, axis, 1.1f, blue(Palette.CAT_FLAME_TEAL), 0.13f, false);
			flare(at, axis, 1.1f, gold(Palette.CAT_FLAME_GOLD), 0.2f, true);
			if (torus != null) {
				exhaustRing(torus, at, axis, 0.05f, blue(Palette.CAT_RING_CYAN), 6.9352f);
				exhaustRing(torus, at, axis, 0.07f, gold(Palette.CAT_RING_GOLD), 6.9352f);
			}
		}
		float[] bell = Fx.mesh("cat_bell");
		Particle b = bell == null ? null : Fx.particle("cat_bell_band", gold(Palette.CAT_BELL_GOLD));
		if (b != null) {
			// (its orientation: the data's mesh as it stands, its narrow end up: M)
			Fx.add(b.at(cat.position().subtract(0, 0.4276, 0)).mesh(bell, new Vec3(1, 0, 0)).size(2.2f, 2.2f).life(0.2f)
				.alpha(0.8f).fade(0.02f, 0.6f));
		}
	}

	/** An exhaust ring (the torus 1BD54, 1.42 m at size 1) flying out along axis, its hole along it, for 0.15 s. */
	private static void exhaustRing(float[] torus, Vec3 at, Vec3 axis, float scale, Palette palette, float speed) {
		Particle r = Fx.particle("cat_nap_ring", palette);
		if (r == null) {
			return;
		}
		Vec3 a = axis.normalize();
		Vec3 up = Math.abs(a.y) > 0.9 ? new Vec3(1, 0, 0).cross(a).normalize() : UP.cross(a).cross(a).normalize();
		float width = 1.42f * scale;
		Fx.add(r.at(at.subtract(up.scale(width / 2))).mesh(torus, up.cross(a), up).velocity(a.scale(speed)).size(width, width)
			.life(0.15f).nearFade(0.5f).fade(0.02f, 0.6f));
	}

	/**
	 * Each tick, every Catnapper in its windup or dive: after the 0.35 s windup, the dive (7163) from both nozzles (their
	 * trails, sparks and rings) and, for its first 0.2 s, the head slash (71B8).
	 */
	private static void dives(net.minecraft.client.multiplayer.ClientLevel level) {
		float now = Fx.now();
		java.util.Iterator<Map.Entry<Integer, Dive>> it = DIVES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Dive> e = it.next();
			net.minecraft.world.entity.Entity cat = level.getEntity(e.getKey());
			float t = now - e.getValue().start - WINDUP;
			if (cat == null || !cat.isAlive() || t > DIVE_LONGEST + 0.1f || now < e.getValue().start) {
				it.remove();
				continue;
			}
			if (t < 0) {
				continue;
			}
			enemy = e.getValue().enemy;
			try {
				Vec3 way = e.getValue().way.lengthSqr() < 1.0E-6 ? cat.getLookAngle() : e.getValue().way.normalize();
				dive(cat, way, t);
				if (t < 0.2f) {
					slash(cat, way);
				}
			} finally {
				enemy = false;
			}
		}
	}

	/**
	 * The dive (7163, held), from each high nozzle: three tube trails (16E9D: 0.17 m cyan for 0.067 s, 0.21 m blue for
	 * 0.14 s, 0.56 m gold for 0.13 s), two strip trails (219FA x 16E9D: 0.073 m and 0.17 m, cyan, 0.15 / 0.2 s), sparks
	 * (18BC2: 30 at the start at 4.4-13.6 m/s in a 7-degree cone, then 144 pale and 50 gold a second), gold exhaust rings
	 * 7 a second at 6-7.3 m/s (a cyan one at 11.3-12.6 m/s at the start). The flames are her jetpack's own (7064, drawn
	 * anyway). Not drawn: its heat streaks (distortion) and its speed lines (placed by an engine input not decoded).
	 */
	private static void dive(net.minecraft.world.entity.Entity cat, Vec3 way, float t) {
		Vec3[][] noz = nozzles(cat);
		int id = cat.getId();
		float dt = Fx.since(-(id * 8 + 3) - 1);
		boolean first = t < 0.06f;
		float[] torus = Fx.mesh("cat_purr_torus");
		for (int n = 0; n < 2; n++) {
			Vec3 at = noz[0][n];
			// out of the nozzle: behind her as she dives
			Vec3 axis = way.scale(-1);
			int trail = 2_000_000 + id * 16 + n * 8;
			Fx.trail(trail, at, "cat_cone_line", enemy ? Palette.CAT_DIVE_ENEMY : Palette.CAT_DIVE_CYAN, 0.1727f, 0.0673f, 1);
			Fx.trail(trail + 1, at, "cat_cone_line", enemy ? Palette.CAT_DIVE_ENEMY : Palette.CAT_DIVE_BLUE, 0.2091f, 0.14f, 1);
			Fx.trail(trail + 2, at, "cat_cone_line", enemy ? Palette.CAT_DIVE_ENEMY : Palette.CAT_DIVE_GOLD, 0.5636f, 0.13f, 0.8f);
			Fx.trail(trail + 3, at, "cat_dive_band", enemy ? Palette.CAT_BOOST_ENEMY : Palette.CAT_BOOST, 0.0727f, 0.15f, 1);
			Fx.trail(trail + 4, at, "cat_dive_band", enemy ? Palette.CAT_BOOST_ENEMY : Palette.CAT_DIVE_STRIP, 0.1727f, 0.2f, 1);
			int burst = first ? 30 : 0;
			int pale = (int) (144 * dt + Fx.RANDOM.nextFloat());
			int gold = (int) (50 * dt + Fx.RANDOM.nextFloat());
			for (int i = 0; i < burst + pale + gold; i++) {
				boolean isBurst = i < burst;
				boolean isPale = !isBurst && i < burst + pale;
				Palette palette = enemy ? Palette.CAT_SPARK_ENEMY : isPale ? Palette.CAT_SPARK_PALE : Palette.CAT_SPARK_GOLD;
				Particle s = Fx.particle("cat_blob#4", palette);
				if (s == null) {
					break;
				}
				float size = isBurst ? range(0.0114f, 0.0682f) : isPale ? range(0.0114f, 0.05f) : range(0.0114f, 0.0318f);
				float speed = isBurst ? range(4.4046f, 13.6096f) : isPale ? range(2.5564f, 7.3917f) : range(2.5564f, 12.7275f);
				float stretch = isPale ? range(2.671f, 3.447f) : range(1.73f, 2.507f);
				Vec3 v = cone(axis, isBurst ? 0.1277 : 0.0873).scale(speed);
				Fx.add(s.at(at).shape(Shape.STREAK).velocity(v).life(isBurst ? range(0.1065f, 0.1955f) : range(0.0701f, 0.25f))
					.size(size * stretch, size * stretch).aspect(1 / stretch).nearFade(1).fade(0.02f, 0.6f));
			}
			if (torus != null) {
				if (first) {
					exhaustRing(torus, at, axis, 0.05f, blue(Palette.CAT_RING_CYAN), range(11.28f, 12.55f));
				}
				if (Fx.RANDOM.nextFloat() < 7 * dt) {
					exhaustRing(torus, at, axis, 0.05f, gold(Palette.CAT_RING_GOLD), range(5.996f, 7.264f));
				}
			}
		}
	}

	/**
	 * The head slash (71B8, the dive's first 0.2 s), 1.6 m ahead of her middle (its hardpoint 01DA, M): claw arcs (152B9,
	 * 37021 x 299E3 x 2C41F) 100 a second at sizes 2.6-4.6 (0.51-0.9 m), each at a random roll round her way, for
	 * 0.15-0.2 s; and two domes (1B2FF at 2.58: 2.58 m across) facing her way 0.21 m ahead of her middle, kept on her.
	 */
	private static void slash(net.minecraft.world.entity.Entity cat, Vec3 way) {
		Vec3 middle = cat.position().add(0, cat.getBbHeight() / 2, 0);
		float dt = Fx.since(-(cat.getId() * 8 + 4) - 1);
		float[] arc = Fx.mesh("cat_slash_arc");
		Vec3 p1 = Math.abs(way.y) > 0.9 ? new Vec3(1, 0, 0).cross(way).normalize() : UP.cross(way).normalize();
		Vec3 p2 = way.cross(p1).normalize();
		int arcs = (int) (100 * dt + Fx.RANDOM.nextFloat());
		for (int i = 0; arc != null && i < arcs; i++) {
			Particle a = Fx.particle("cat_slash_band", enemy ? Palette.CAT_SLASH_ENEMY : Palette.CAT_SLASH);
			if (a == null) {
				break;
			}
			double roll = Fx.RANDOM.nextDouble() * Mth.TWO_PI;
			Vec3 normal = p1.scale(Math.cos(roll)).add(p2.scale(Math.sin(roll)));
			float width = 0.196f * range(2.5971f, 4.5866f);
			// the arc lies in its own plane (X, Y), standing on its Y 0: centred on the point by half its 0.89 height
			Vec3 at = middle.add(way.scale(1.5958)).subtract(way.scale(width * 0.45));
			Fx.add(a.at(at).mesh(arc, way.cross(normal), way).size(width, width).life(range(0.1545f, 0.2f)).nearFade(1)
				.fade(0.02f, 0.6f));
		}
		float[] dome = Fx.mesh("cat_dome");
		if (dome == null) {
			return;
		}
		// the dome's pole lies along its own Z (side x up): up square to her way, side = up x way
		Vec3 up = p2;
		Vec3 base = middle.add(way.scale(0.2079)).subtract(up.scale(2.5778 / 2));
		String[][] layers = {{"cat_dome_glow", "0"}, {"cat_dome_band", "1"}};
		for (String[] layer : layers) {
			Palette palette = enemy ? Palette.CAT_DOME_ENEMY : layer[1].equals("0") ? Palette.CAT_DOME : Palette.CAT_DOME_DEEP;
			Particle d = Fx.attach("catnapper_dome/" + cat.getId() + "/" + layer[1], base, () -> {
				Particle p = Fx.particle(layer[0], palette);
				// (strengths ours: at full, the inner dome's mostly white strip 55A46 drew a solid cyan disc, found playing)
				return p == null ? null : p.life(0.2f).size(2.5778f, 2.5778f).alpha(layer[1].equals("0") ? 0.6f : 0.35f)
					.fade(0.02f, 0.5f);
			});
			if (d != null) {
				d.mesh(dome, up.cross(way), up);
			}
		}
	}

	/**
	 * The landing's shockwave reaching an enemy (7256, its per-target slot; at the enemy, thrown out from her along
	 * way): a 0.26 m sphere (DBF, 16E9D x 18A1 x 18E48 x 1C7A) and a puff of smoke (377D5, tan) flung 1.9-5.2 m/s, a
	 * small flame burst (F7D2, 0.05-0.09 m), a dust puff (5429) flung 5-17 m/s, and 2-4 sparks (1F35) back toward her at
	 * 5-13 m/s. The data's directions are the effect's own (up 0.3, out 0.98: M that out is away from her).
	 */
	private static void landingHit(Vec3 at, Vec3 way) {
		Vec3 out = new Vec3(way.x, 0, way.z).lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : new Vec3(way.x, 0, way.z).normalize();
		Vec3 kick = out.scale(0.9816).add(0, 0.3, 0).normalize();
		Vec3 low = at.add(0, 0.3, 0);
		float[] sphere = Fx.mesh("cat_bubble");
		Particle s = sphere == null ? null : Fx.particle("cat_shock_sphere", blue(Palette.CAT_SHOCK));
		if (s != null) {
			float size = range(0.2551f, 0.2712f);
			Fx.add(s.at(low.subtract(0, size / 2, 0)).mesh(sphere, new Vec3(1, 0, 0)).velocity(kick.scale(range(1.92f, 5.2369f)))
				.size(size, size).life(range(0.0864f, 0.1273f)).fade(0.02f, 0.6f));
		}
		Particle smoke = Fx.soft("smoke#48", Palette.NATURAL);
		if (smoke != null) {
			Fx.add(smoke.at(low).velocity(kick.scale(range(1.92f, 5.2369f))).tint(0xC5A565).spin(range(-Mth.PI, Mth.PI), 0)
				.size(range(0.2369f, 0.8086f), 0.9f).life(range(0.0683f, 0.1818f)).alpha(0.8f).fade(0.05f, 0.6f));
		}
		Particle flame = Fx.particle("cat_claw_flame#4", enemy ? Palette.CAT_SHOCK_FLAME_ENEMY : Palette.CAT_SHOCK_FLAME);
		if (flame != null) {
			float size = range(0.0545f, 0.0909f);
			Fx.add(flame.at(low).velocity(UP.scale(0.05)).size(size, size).life(0.2f).fade(0.01f, 0.6f));
		}
		Particle dust = Fx.soft("cat_dust_puff#4", Palette.NATURAL);
		if (dust != null) {
			float size = range(0.2983f, 0.3364f);
			Fx.add(dust.at(low).velocity(out.scale(1).add(0, 0.3818, 0).normalize().scale(range(5, 17.1921f))).tint(0xCCB179)
				.size(size, size).life(range(0.1091f, 0.1909f)).alpha(0.8f).fade(0.02f, 0.6f));
		}
		int sparks = 2 + Fx.RANDOM.nextInt(3);
		for (int i = 0; i < sparks; i++) {
			Particle sp = Fx.particle("spark#4", blue(Palette.CAT_SHOCK_SPARK));
			if (sp != null) {
				float size = range(0.0155f, 0.03f);
				Fx.add(sp.at(low).velocity(cone(out.scale(-1), 0.2428).scale(range(5, 13.1f))).gravity(9.8f).size(size, size)
					.life(range(0.3526f, 0.859f)).spin(0, -1.9209f).fade(0.02f, 0.6f));
			}
		}
	}

	// ---- perks ----

	/**
	 * Claws Out's wound (74D0, held 1 s on the clawed enemy at their chest 0001): two claw marks (720CE x 18E48, 0.49-1.01
	 * m: the data's size runs on an input not decoded, ours its middle) at a random turn between -141 and 40 degrees, a
	 * third 0.067 s later, each 0.25-0.3 s; 5 sparks (1F35) at 3.8-4.4 m/s. Teal for her side, pink-red for the enemy's.
	 */
	private static void claws(Vec3 inside, Vec3 way) {
		// on the clawed one's front, toward her (ours: at the chest's middle a zombie's own body hid them, found playing)
		Vec3 flat = new Vec3(way.x, 0, way.z);
		Vec3 chest = flat.lengthSqr() < 1.0E-6 ? inside : inside.subtract(flat.normalize().scale(0.4));
		Palette marks = enemy ? Palette.CAT_CLAWS_ENEMY : Palette.CAT_CLAWS;
		for (int i = 0; i < 3; i++) {
			Particle c = Fx.particle("cat_claw_marks", marks);
			if (c != null) {
				// the third waits its 0.067 s by fading in over it
				Fx.add(c.at(chest).size(0.75f, 0.75f).spin(range(-2.4671f, 0.6981f), 0).life(range(0.25f, 0.3f))
					.fade(i == 2 ? 0.25f : 0.02f, 0.6f).nearFade(1));
			}
		}
		for (int i = 0; i < 5; i++) {
			Particle s = Fx.particle("spark#4", enemy ? Palette.CAT_CLAW_SPARK_ENEMY : Palette.CAT_CLAW_SPARK);
			if (s != null) {
				float size = range(0.02f, 0.05f);
				Fx.add(s.at(chest).velocity(randomUnit().scale(range(3.7611f, 4.3656f))).size(size, size).spin(0, -5)
					.life(range(0.2f, 0.2136f)).fade(0.02f, 0.6f));
			}
		}
	}

	/** When the local player's Transport Shielding hexes end (client seconds; 0: none). */
	private static float shieldUntil;

	/**
	 * Transport Shielding (5513, held on her while her shields last, shown to her alone): the frame 5C75 (a square with
	 * an octagon cut out) at size 2 with the hex grid 18F9B (tiled 4.75 x 4) under the edge mask 18400, teal: hexes in its
	 * corners. The mask is black wherever the frame is open, so it's drawn as the square facing the camera. Through her
	 * own eyes it stands 1.45 m before the camera, its corners at the view's top and bottom (ours: M that it reads as
	 * hexes at the screen's edge); from a camera off her (third person, the lifted ability camera) at her chest.
	 */
	private static void shield() {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		net.minecraft.client.player.LocalPlayer me = mc.player;
		if (me == null || shieldUntil <= 0) {
			return;
		}
		// (her absorption reaches the client a moment after this effect: the first ticks don't look at it, found playing)
		boolean started = Fx.now() > shieldUntil - (float) dev.overcraft.jetpackcat.JetpackCatStats.TRANSPORT_SHIELD_TIME + 0.5f;
		if (Fx.now() > shieldUntil || started && me.getAbsorptionAmount() <= 0 || !me.isAlive()) {
			shieldUntil = 0;
			return;
		}
		// her own eyes, or a camera off her (third person, and the lifted camera Lifeline and Catnapper's carry use)
		boolean firstPerson = mc.gameRenderer.mainCamera().position().distanceTo(me.getEyePosition()) < 0.3;
		Vec3 chest = me.position().add(0, me.getBbHeight() * 0.7, 0);
		Particle p = Fx.attach("transport_shield", firstPerson ? Vec3.ZERO : chest, () -> {
			Particle h = Fx.particle("cat_hex_frame", Palette.CAT_HEX);
			return h == null ? null : h.life(0.3f).size(2, 2).near().fade(0.1f, 0.5f);
		});
		if (p == null) {
			return;
		}
		if (firstPerson && p.follow == null) {
			p.follow(() -> new org.joml.Vector3f(net.minecraft.client.Minecraft.getInstance().gameRenderer.mainCamera().forwardVector())
				.mul(1.45f));
		} else if (!firstPerson && p.follow != null) {
			p.follow = null;
			p.at(chest);
		}
	}

	/**
	 * The landing (71C2): a 5.9 m violet-cyan flash, the 21.7 m ground flash, a 1.9 m sphere, 15 smoke puffs thrown up
	 * 12-19 m/s, and the crater's glowing cracks, 10 m across, fading over 2.5 s.
	 */
	private static void landing(Vec3 feet) {
		Particle flash = Fx.particle("cat_soft", gold(Palette.CAT_NAP_IMPACT));
		if (flash != null) {
			Fx.add(flash.at(feet.add(0, 1, 0)).life(0.25f).size(3, 5.9f).easeOut().nearFade(2).fade(0.01f, 0.7f));
		}
		// the ground flash (C19): 21.7 m of 4C09 x the noise 16E9D, navy to deep blue (800C2FC5 r3: never brighter than
		// #0004FF, so at its full size it darkens nothing and whitens nothing), 0.25 s
		Particle ground = Fx.particle("cat_ground_flash", enemy ? Palette.CAT_NAP_GROUND_ENEMY : Palette.CAT_NAP_GROUND);
		if (ground != null) {
			Fx.add(ground.at(feet.add(0, 0.05, 0)).shape(Shape.FLAT).normal(UP).life(0.25f).size(21.71f, 21.71f).fade(0.01f, 0.7f));
		}
		Particle sphere = Fx.particle("cat_soft", blue(Palette.CAT_PURR_BLUE));
		if (sphere != null) {
			Fx.add(sphere.at(feet.add(0, 0.6, 0)).life(0.3f).size(1, 1.9f).nearFade(2).fade(0.01f, 0.7f));
		}
		for (int i = 0; i < 15; i++) {
			Particle puff = Fx.soft("smoke#48", Palette.NATURAL);
			if (puff != null) {
				Fx.add(puff.at(feet.add(0, 0.3, 0)).velocity(cone(UP, Math.toRadians(45)).scale(range(11.6f, 18.9f))).drag(0.02f)
					.gravity(9.8f).life(range(0.62f, 1.3f)).size(range(0.61f, 1.12f), range(1.2f, 2)).alpha(0.7f).fade(0.05f, 0.7f));
			}
		}
		// the crater's dark cracks (CBB9) darken the ground (a multiplied decal: white where there's no crack), which these
		// additive and blended particles can't do: drawn as a 10 m square of white (found playing), so left out; its
		// glowing edges (CA10) stay
		Particle glowCrack = Fx.particle("cat_crack_glow", blue(Palette.CAT_HALO));
		if (glowCrack != null) {
			Fx.add(glowCrack.at(feet.add(0, 0.04, 0)).shape(Shape.FLAT).normal(UP).life(2.47f).size(10, 10).fade(0.01f, 0.5f));
		}
	}
}
