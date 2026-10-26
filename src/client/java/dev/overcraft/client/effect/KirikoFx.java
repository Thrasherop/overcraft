package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.net.EffectPayload;
import dev.overcraft.client.model.HeroModels;
import dev.overcraft.client.model.Skeleton;
import dev.overcraft.client.model.SkinnedModel;
import dev.overcraft.client.HeroHud;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Kiriko's effects, rebuilt from Overwatch's own (tools/reference/kiriko-effects.md: which textures, sizes, lifetimes,
 * speeds and colours each one uses, read from the game's effect files). Sizes in metres and times in seconds are the
 * survey's unless marked "ours". Played by EffectPayload from the server, or by renderers (trails, the fox's petals).
 */
public final class KirikoFx {
	/**
	 * Petals are tinted toward Kiriko's teal (the survey: "tinted teal-white"): the mask is solid white, and added
	 * white they read as snowballs.
	 */
	private static final int PETAL_TINT = 0x6EF0DA;

	private KirikoFx() {
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}

	private static Vec3 randomUnit(RandomSource r) {
		double z = r.nextDouble() * 2 - 1;
		double a = r.nextDouble() * Math.PI * 2;
		double s = Math.sqrt(1 - z * z);
		return new Vec3(s * Math.cos(a), z, s * Math.sin(a));
	}

	/** A random direction within halfAngle (radians) of axis. */
	private static Vec3 cone(Vec3 axis, double halfAngle) {
		Vec3 a = axis.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : axis.normalize();
		for (int i = 0; i < 20; i++) {
			Vec3 d = randomUnit(Fx.RANDOM);
			if (d.dot(a) >= Math.cos(halfAngle)) {
				return d;
			}
		}
		return a;
	}

	public static void play(EffectPayload e) {
		Vec3 at = new Vec3(e.x(), e.y(), e.z());
		Vec3 dir = new Vec3(e.dx(), e.dy(), e.dz());
		Palette team = e.variant() == 1 ? Palette.GOLD : Palette.TEAL;
		switch (e.effect()) {
			case "ofuda_hit_ally" -> ofudaHitAlly(at, team);
			case "ofuda_hit_block" -> ofudaHitBlock(at, dir, team);
			case "kunai_hit_block" -> kunaiHitBlock(at, dir);
			case "kunai_hit_entity" -> kunaiHitEntity(at);
			case "suzu_burst" -> suzuBurst(at, e.variant() / 10f);
			case "suzu_cleansed" -> Cleansed.mark(e.entity());
			case "swift_step_depart" -> swiftStep(at, false);
			case "swift_step_arrive" -> swiftStep(at, true);
			case "rush_cast" -> rushCast(at);
			case "rush_ripple" -> rushRipple(at);
			case "rush_gate" -> rushGate(at, dir, e.variant() / 10f);
			case "rush_tiles" -> rushTiles(at, dir, e.variant() / 10f);
			case "rush_end" -> rushEnd(at);
			case "rush_ally" -> {
				rushAlly(at, dir);
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null && e.entity() == mc.player.getId()) {
					HeroHud.markInRush();
				}
			}
			default -> {
			}
		}
	}

	// ---- Healing Ofuda ----

	/** In flight: the ribbon behind it and a soft glow on it, teal (gold while homing on an ally). */
	public static void ofudaFlight(int id, Vec3 at, boolean homing) {
		// a thin ribbon and a small glow, so the paper itself reads (the second critique: comets in big halos, no
		// paper). Ribbon width and length aren't in the game data: ours. Homing, the gold is blended rather than added:
		// added over grass it turned lime (the critique)
		if (homing) {
			Fx.trail(id, at, "ribbon", Palette.GOLD, 0.07f, 0.08f, 0.6f, true);
			Fx.attach("ofuda_glow/" + id, at, () -> {
				Particle g = Fx.soft("glow", Palette.GOLD);
				return g == null ? null : g.at(at).life(0.15f).size(0.24f, 0.24f).alpha(0.4f).fade(0.15f, 0.6f);
			});
		} else {
			Fx.trail(id, at, "ribbon", Palette.TEAL, 0.07f, 0.08f, 0.55f);
			Fx.attach("ofuda_glow/" + id, at, () -> glow("glow", Palette.TEAL, at, 0.2f, 0.15f, 0.25f));
		}
	}

	/**
	 * At her fingers each time an ofuda leaves: a soft golden puff (the reference: "a soft golden smoke puff pops at
	 * the fingertips each time"; the hand effect 2074's golden swirl).
	 */
	public static void ofudaHandPuff(Vec3 hand) {
		Particle puff = Fx.soft("smoke#48", Palette.GOLD);
		if (puff != null) {
			float size = range(0.06f, 0.08f);
			Fx.add(puff.at(hand).velocity(randomUnit(Fx.RANDOM).scale(0.12)).life(range(0.25f, 0.35f)).size(size, size * 1.6f)
				.spin(range(0, Mth.TWO_PI), 0).alpha(0.6f).fade(0.1f, 0.6f).near());
		}
	}

	/** Healing received: a quick flash of soft glows drifting up and a five-petal flower (effects 4004 / 40C0). */
	static void ofudaHitAlly(Vec3 at, Palette p) {
		// gold blended (it washed out to lime added over grass), the glows kept small: ten ofuda land in a second
		boolean gold = p == Palette.GOLD;
		for (int i = 0; i < 3; i++) {
			Particle g = gold ? Fx.soft("glow", p) : Fx.particle("glow", p);
			if (g != null) {
				float size = range(0.2f, 0.5f);
				Fx.add(g.at(at).velocity(new Vec3(0, range(0.25f, 0.35f), 0)).life(range(0.08f, 0.4f)).size(size, size * 1.2f)
					.alpha(gold ? 0.5f : 0.7f).fade(0.1f, 0.7f));
			}
		}
		Particle flower = gold ? Fx.soft("flower", p) : Fx.particle("flower", p);
		if (flower != null) {
			Fx.add(flower.at(at).life(0.34f).size(0.32f, 0.5f).easeOut().spin(range(0, Mth.TWO_PI), 1.5f).fade(0.1f, 0.6f));
		}
	}

	/** On terrain: a soft glow and the flower flat on the surface for 0.73 s (4111 / 3D4A). */
	static void ofudaHitBlock(Vec3 at, Vec3 normal, Palette p) {
		// the game's glow is 0.3-1.3 m; kept under a metre here, where it read as a haze two blocks wide (the critique)
		boolean gold = p == Palette.GOLD;
		float size = range(0.3f, 0.9f);
		Particle glow = gold ? Fx.soft("glow", p) : Fx.particle("glow", p);
		if (glow != null) {
			Fx.add(glow.at(at).life(range(0.08f, 0.4f)).size(size, size).alpha(0.55f).fade(0.15f, 0.6f));
		}
		Particle decal = Fx.soft("flower", p);
		if (decal != null && normal.lengthSqr() > 0.5) {
			Fx.add(decal.at(at.add(normal.scale(0.02))).normal(normal).life(0.73f).size(0.45f, 0.5f)
				.spin(range(0, Mth.TWO_PI), 0).alpha(0.7f).fade(0.05f, 0.9f));
		}
	}

	// ---- Kunai ----

	/** In flight: a thin cyan ribbon, and a lens glint at its head 16 times a second (effect 3BF2). */
	public static void kunaiFlight(int id, Vec3 at, float dt) {
		Fx.trail(id, at, "ribbon", Palette.CYAN, 0.12f, 0.09f, 1);
		if (Fx.RANDOM.nextFloat() < 16 * dt) {
			Particle glint = Fx.particle("glint", Palette.CYAN);
			if (glint != null) {
				float size = range(0.21f, 0.23f);
				Fx.add(glint.at(at).life(range(0.1f, 0.17f)).size(size, size).aspect(2).fade(0.1f, 0.6f));
			}
		}
	}

	/** On terrain: 6 tiny cyan sparks bursting off the surface (3BF4). */
	static void kunaiHitBlock(Vec3 at, Vec3 normal) {
		sparks(at, normal, 6, Palette.CYAN);
	}

	/** On a target the game plays only a sound (13B2); a few sparks so a hit reads in Minecraft (ours). */
	static void kunaiHitEntity(Vec3 at) {
		sparks(at, new Vec3(0, 1, 0), 3, Palette.CYAN);
	}

	private static void sparks(Vec3 at, Vec3 normal, int count, Palette p) {
		for (int i = 0; i < count; i++) {
			Particle s = Fx.particle("spark#4", p);
			if (s == null) {
				return;
			}
			Vec3 v = cone(normal, Math.toRadians(70)).scale(range(3, 5));
			// a spark streaks along its motion: 5-6 cm across, stretched to show its speed
			float size = range(0.05f, 0.06f);
			s.fps = 30;
			Fx.add(s.at(at).velocity(v).shape(Shape.STREAK).life(range(0.17f, 0.44f)).size(size * 4, size * 2).aspect(0.25f)
				.drag(0.02f).fade(0.02f, 0.6f));
		}
	}

	// ---- Protection Suzu ----

	/** In flight: a teal ribbon and sparkles shed 75 a second (3CB2). */
	public static void suzuFlight(int id, Vec3 at, float dt) {
		Fx.trail(id, at, "ribbon", Palette.TEAL, 0.25f, 0.2f, 0.8f);
		int n = (int) (75 * dt + Fx.RANDOM.nextFloat());
		for (int i = 0; i < n; i++) {
			Particle s = Fx.particle("spark#4", Palette.TEAL);
			if (s != null) {
				float size = range(0.15f, 0.3f);
				Fx.add(s.at(at.add(randomUnit(Fx.RANDOM).scale(0.1))).life(range(0.25f, 0.64f)).size(size, size * 0.3f)
					.velocity(randomUnit(Fx.RANDOM).scale(0.3)).spin(range(0, Mth.TWO_PI), 0).fade(0.05f, 0.7f));
			}
		}
	}

	/**
	 * The burst (360C): a dome flashing outward, a sky-blue wave across the ground, a flower, glows, then a gold ring
	 * on the ground for 5 s (6089). Sized to the ability's radius (the game's mesh sizes fit its own radius).
	 */
	static void suzuBurst(Vec3 at, float radius) {
		float r = radius > 0 ? radius : 4;
		Particle dome = Fx.particle("dome_noise", Palette.SUZU_BLUE);
		if (dome != null) {
			Fx.add(dome.at(at.add(0, -0.3, 0)).shape(Shape.DOME).life(0.3f).size(r, 2 * r).easeOut().spin(range(0, Mth.TWO_PI), 1)
				.alpha(0.5f).fade(0.05f, 0.7f));
		}
		Vec3 ground = at.add(0, 0.05, 0);
		Particle wave = Fx.particle("ring_line", Palette.SUZU_BLUE);
		if (wave != null) {
			Fx.add(wave.at(ground).normal(new Vec3(0, 1, 0)).life(0.25f).size(0.5f, 2 * r).easeOut().fade(0.02f, 0.5f));
		}
		// blended teal: added over grass it turned emerald (the second critique)
		Particle flower = Fx.soft("flower", Palette.TEAL);
		if (flower != null) {
			// the flower mesh at size 2 (the survey): about 2 m across, opening as it fades
			Fx.add(flower.at(ground.add(0, 0.02, 0)).normal(new Vec3(0, 1, 0)).life(0.5f).size(1.2f, 2.2f).easeOut()
				.spin(range(0, Mth.TWO_PI), 0.5f).alpha(0.7f).fade(0.1f, 0.6f));
		}
		// a flash, kept modest: Suzu is often thrown at her own feet, and a big glow there whites out the screen. Lifted
		// clear of the ground: a camera-facing glow centred on it was cut off flat by the ground (the critique)
		Fx.add(glow("glow_hot", Palette.SUZU_BLUE, at.add(0, 0.75, 0), 1.5f, 0.25f, 0.5f));
		Fx.add(glow("glow", Palette.TEAL, at.add(0, 1.1, 0), 2.4f, 0.4f, 0.3f));
		// the 5 s ground mark: soft gold, blended (added over grass it was a hard lime line)
		Particle mark = Fx.soft("ring_line", Palette.SUZU_GOLD);
		if (mark != null) {
			Fx.add(mark.at(ground.add(0, 0.03, 0)).normal(new Vec3(0, 1, 0)).life(5).size(2 * r, 2 * r).alpha(0.85f).fade(0.03f, 0.15f));
		}
	}

	// ---- Swift Step ----

	/**
	 * Where she leaves (3CC7) and where she arrives (3CC5): teal rings (1.4 m), a burst of sakura petals flung up, a
	 * flash; arriving also a teal puff and longer-lived petals.
	 */
	static void swiftStep(Vec3 feet, boolean arrive) {
		Vec3 chest = feet.add(0, 1, 0);
		// her own arrival seen in first person: the flash and puff would sit on the camera, a white disc over the HUD
		// (the critique); she sees the rings and petals round her instead
		Minecraft mc = Minecraft.getInstance();
		boolean atCamera = mc.player != null && mc.options.getCameraType().isFirstPerson()
			&& mc.player.position().distanceToSqr(feet) < 2.25;
		for (int i = 0; i < 2; i++) {
			Particle ring = Fx.particle("ring_line", Palette.TEAL);
			if (ring != null) {
				Fx.add(ring.at(feet.add(0, 0.15 + i * 0.8, 0)).normal(new Vec3(0, 1, 0)).life(range(0.25f, 0.35f)).size(0.4f, 2.8f)
					.easeOut().fade(0.05f, 0.6f));
			}
		}
		// a small ring (it was 3.2 m across: read as a shield bubble, the second critique)
		Particle burst = atCamera ? null : Fx.particle("ring_line", Palette.TEAL);
		if (burst != null) {
			Fx.add(burst.at(chest).life(0.3f).size(0.3f, 1.4f).easeOut().alpha(0.7f).fade(0.05f, 0.6f));
		}
		if (!atCamera) {
			Fx.add(glow("glow_hot", Palette.TEAL, chest, 1.6f, 0.22f, 0.45f));
		}
		int petals = arrive ? 65 : 39;
		for (int i = 0; i < petals; i++) {
			Particle petal = Fx.particle("petal", Palette.TEAL);
			if (petal == null) {
				break;
			}
			boolean quick = !arrive && i >= 33;
			float life = arrive ? range(1, 1.5f) : quick ? range(0.14f, 0.22f) : range(0.6f, 0.7f);
			float speed = arrive ? range(1, 3) : range(2, 3);
			float size = range(0.045f, 0.08f);
			Vec3 v = cone(new Vec3(0, 1, 0), Math.toRadians(60)).scale(speed);
			Vec3 from = chest.add(randomUnit(Fx.RANDOM).scale(0.3));
			if (atCamera) {
				// round her feet and flung outward, not up through the camera (they passed it as big blurred discs)
				double a = Fx.RANDOM.nextDouble() * Mth.TWO_PI;
				Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
				from = feet.add(out.scale(range(0.5f, 0.9f))).add(0, 0.2, 0);
				v = out.scale(range(1, 2)).add(0, range(1, 2.5f), 0);
			}
			// the petal mask is solid white: tinted teal and narrowed, or they read as round white dots (the critique)
			Fx.add(petal.at(from).velocity(v).gravity(2.5f).drag(0.15f).life(life)
				.size(size, size).aspect(0.65f).tint(PETAL_TINT).alpha(0.85f).spin(range(0, Mth.TWO_PI), range(-8, 8)).fade(0.05f, 0.4f));
		}
		if (arrive) {
			slashArcs(chest, atCamera);
		}
		if (arrive && !atCamera) {
			Particle puff = Fx.particle("glow", Palette.TEAL);
			if (puff != null) {
				float size = range(0.6f, 0.8f);
				Fx.add(puff.at(chest).life(range(0.24f, 0.48f)).size(size, size * 1.6f).easeOut().fade(0.05f, 0.7f));
			}
		}
	}

	/**
	 * Swift Step's 7 slash arcs at the arrival (10ADC, the half-cylinder shell, with its streak bands; size 3-3.7, 0.24-0.4
	 * s, random rotation): curved teal bands round her, each turned its own way and sweeping round. Seen from inside
	 * (her own arrival in first person) they're kept wider and fainter.
	 */
	private static void slashArcs(Vec3 chest, boolean atCamera) {
		for (int i = 0; i < 7; i++) {
			Particle arc = Fx.particle("band", Palette.TEAL);
			if (arc == null) {
				return;
			}
			Quaternionf turn = new Quaternionf().rotateY(range(0, Mth.TWO_PI)).rotateX(range(-0.9f, 0.9f)).rotateZ(range(-0.9f, 0.9f));
			float size = range(3, 3.7f) * 0.5f * (atCamera ? 1.6f : 1);
			Fx.add(arc.at(chest).arc(turn, range(1.6f, 2.4f)).life(range(0.24f, 0.4f)).size(size * 0.7f, size).easeOut()
				.aspect(0.12f).spin(range(0, Mth.TWO_PI), range(6, 10) * (Fx.RANDOM.nextBoolean() ? 1 : -1))
				.alpha(atCamera ? 0.18f : 0.8f).fade(0.05f, 0.6f));
		}
	}

	// ---- Kitsune Rush ----

	/** The cast (3C5F): teal smoke and rings at her hands. */
	static void rushCast(Vec3 at) {
		for (int i = 0; i < 8; i++) {
			Particle smoke = Fx.particle("smoke#48", Palette.TEAL);
			if (smoke == null) {
				break;
			}
			float size = range(0.5f, 2);
			Fx.add(smoke.at(at.add(randomUnit(Fx.RANDOM).scale(0.4))).velocity(randomUnit(Fx.RANDOM).scale(0.6)).life(range(0.4f, 0.5f))
				.size(size, size * 1.4f).spin(range(0, Mth.TWO_PI), 0).alpha(0.8f).fade(0.1f, 0.6f));
		}
		Particle ring = Fx.particle("ring_line", Palette.TEAL);
		if (ring != null) {
			Fx.add(ring.at(at).life(0.35f).size(0.3f, 1.5f * 2).easeOut().fade(0.05f, 0.6f));
		}
	}

	/** A ground ripple along the fox's path (2E87: 2.1 m annulus, 1 s). */
	static void rushRipple(Vec3 at) {
		Particle ring = Fx.particle("ring_line", Palette.TEAL);
		if (ring != null) {
			Fx.add(ring.at(at.add(0, 0.05, 0)).normal(new Vec3(0, 1, 0)).life(1).size(0.6f, 3.7f).easeOut().alpha(0.8f).fade(0.1f, 0.7f));
		}
	}

	/**
	 * A torii gate across the path, held while the rush lasts (3C82, 10.67 s): until its model is converted, its
	 * outline in glowing teal (two posts and two beams, ours), with its petals and "+" sparkles.
	 */
	static void rushGate(Vec3 base, Vec3 across, float seconds) {
		float life = seconds > 0 ? seconds : 10.67f;
		Vec3 side = across.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : across.normalize();
		double half = across.length() / 2;
		float[] torii = toriiMesh();
		Particle gate = torii == null ? null : Fx.particle("dome_noise", Palette.TEAL);
		if (gate != null) {
			// the game's gate mesh (1048D) glowing teal through its noise, as wide as the path less a step each side
			float width = (float) (2 * half + 1.6);
			// translucent, as the game's white-cyan gates are (the critique: ours were nearly solid cyan)
			Fx.add(gate.at(base).mesh(torii, side).size(width * 0.6f, width).easeOut().life(life).alpha(0.45f).fade(0.04f, 0.08f));
			Particle glow = Fx.particle("glow", Palette.TEAL);
			if (glow != null) {
				// a faint flash as it rises (it was a bright orb at the path's head, hiding the fox: the second critique)
				Fx.add(glow.at(base.add(0, width * 0.35, 0)).life(0.4f).size(width * 0.3f, width * 0.6f).easeOut().alpha(0.2f).fade(0.1f, 0.7f));
			}
		} else {
			// without the mesh (no local pack model): its outline, two posts and two beams (ours)
			for (int s = -1; s <= 1; s += 2) {
				gateBar(base.add(side.scale(s * half)).add(0, 2.2, 0), new Vec3(0, 1, 0), 4.6f, 0.35f, life);
			}
			gateBar(base.add(0, 4.4, 0), side, (float) (2 * half + 1.6), 0.4f, life);
			gateBar(base.add(0, 3.7, 0), side, (float) (2 * half), 0.25f, life);
		}
		for (int i = 0; i < 7; i++) {
			Particle petal = Fx.particle("petal", Palette.TEAL);
			if (petal != null) {
				float size = range(0.05f, 0.09f);
				Vec3 from = base.add(side.scale(range(-1, 1) * half)).add(0, range(0.5f, 4.4f), 0);
				Fx.add(petal.at(from).velocity(randomUnit(Fx.RANDOM).scale(range(0.5f, 1.5f))).gravity(0.6f).drag(0.4f)
					.life(range(1.5f, 3.5f)).size(size, size).aspect(0.65f).tint(PETAL_TINT).alpha(0.85f)
					.spin(range(0, Mth.TWO_PI), range(-5, 5)).fade(0.05f, 0.4f));
			}
		}
		for (int i = 0; i < 4; i++) {
			Particle plus = Fx.particle("sparkle", Palette.GOLD);
			if (plus != null) {
				float size = range(0.43f, 0.6f);
				Fx.add(plus.at(base.add(side.scale(range(-1, 1) * half)).add(0, range(1, 4.5f), 0)).life(range(0.6f, 1.2f))
					.size(size, size * 0.6f).fade(0.2f, 0.5f));
			}
		}
	}

	private static void gateBar(Vec3 at, Vec3 along, float length, float thickness, float life) {
		Particle bar = Fx.particle("band", Palette.TEAL);
		if (bar != null) {
			Fx.add(bar.at(at).axis(along).life(life).size(length, length).aspect(thickness / length)
				.alpha(0.75f).fade(0.04f, 0.08f));
		}
		Particle core = Fx.particle("glow", Palette.TEAL);
		if (core != null) {
			Fx.add(core.at(at).axis(along).life(life).size(length * 1.1f, length * 1.1f)
				.aspect(thickness * 2.5f / length).alpha(0.5f).fade(0.04f, 0.08f));
		}
	}

	/**
	 * Glowing talisman tiles lying on the path, held while the rush lasts (4013/4014: a rounded card 0.56 x 0.29 at size
	 * 1-2, 11 s; the reference: "teal glyph tiles cover the ground ahead"). Three across the corridor here and there.
	 */
	static void rushTiles(Vec3 ground, Vec3 forward, float seconds) {
		float life = seconds > 0 ? seconds : 8;
		Vec3 ahead = forward.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : forward.normalize();
		Vec3 side = new Vec3(-ahead.z, 0, ahead.x);
		// a flat particle's long side, turned by r, lies along (sin r, 0, cos r): across the path
		float across = (float) Math.atan2(side.x, side.z);
		for (int i = 0; i < 3; i++) {
			// Kiriko's own talisman, glowing teal (cut from the ofuda's texture), lying along the path; else a plain card
			Particle tile = Fx.particle("talisman", Palette.TEAL);
			boolean talisman = tile != null;
			if (tile == null) {
				tile = Fx.particle("card", Palette.TEAL);
			}
			if (tile == null) {
				return;
			}
			float width = range(0.56f, 1.12f);
			Vec3 at = ground.add(side.scale(range(-4, 4))).add(ahead.scale(range(-0.6f, 0.6f))).add(0, 0.03, 0);
			if (talisman) {
				Fx.add(tile.at(at).normal(new Vec3(0, 1, 0)).life(life).size(width * 1.1f, width * 1.1f).aspect(1 / 3f)
					.spin(across + range(-0.35f, 0.35f), 0).alpha(0.85f).fade(0.03f, 0.06f));
			} else {
				Fx.add(tile.at(at).normal(new Vec3(0, 1, 0)).life(life).size(width * 0.52f, width * 0.52f).aspect(1 / 0.52f)
					.spin(across + range(-0.25f, 0.25f), 0).alpha(0.75f).fade(0.03f, 0.06f));
			}
		}
	}

	/** The fox vanishing at the end of its run: smoke and petals (ours, from the cast's pieces). */
	static void rushEnd(Vec3 at) {
		rushCast(at.add(0, 0.6, 0));
		foxPetals(at, 1, 20);
	}

	/** Allies in the path: speed lines streaming back past them, against the rush's direction (3D2F). */
	static void rushAlly(Vec3 feet, Vec3 forward) {
		Vec3 back = forward.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, -1) : forward.normalize().scale(-1);
		for (int i = 0; i < 3; i++) {
			Particle line = Fx.particle("speed_line", Palette.TEAL);
			if (line != null) {
				double a = Fx.RANDOM.nextDouble() * Mth.TWO_PI;
				Vec3 from = feet.add(Math.cos(a) * 0.7, range(0.2f, 1.8f), Math.sin(a) * 0.7).add(forward.normalize().scale(0.8));
				float size = range(1.75f, 2.25f);
				Fx.add(line.at(from).velocity(back.scale(range(6, 9))).shape(Shape.STREAK).life(0.25f).size(size, size)
					.aspect(0.04f).alpha(0.5f).fade(0.2f, 0.5f));
			}
		}
	}

	/** The fox's wake: a wide cyan ribbon behind it and a soft glow on it (ours, so the fast fox reads). */
	public static void foxWake(int id, Vec3 at) {
		Fx.trail(id, at, "ribbon", Palette.CYAN, 0.9f, 0.35f, 0.75f);
		Fx.attach("fox_glow/" + id, at, () -> glow("glow", Palette.CYAN, at, 2.2f, 0.2f, 0.45f));
	}

	/** The fox sheds sakura petals as it runs (3E3D: 33 a second). */
	public static void foxPetals(Vec3 at, float dt, int perSecond) {
		int n = (int) (perSecond * dt + Fx.RANDOM.nextFloat());
		for (int i = 0; i < n; i++) {
			Particle petal = Fx.particle("petal", Palette.TEAL);
			if (petal == null) {
				return;
			}
			float size = range(0.05f, 0.09f);
			Fx.add(petal.at(at.add(randomUnit(Fx.RANDOM).scale(0.6)).add(0, 0.8, 0)).velocity(randomUnit(Fx.RANDOM).scale(0.8))
				.gravity(1.2f).drag(0.3f).life(range(0.6f, 0.7f) * 2).size(size, size).aspect(0.65f).tint(PETAL_TINT).alpha(0.85f)
				.spin(range(0, Mth.TWO_PI), range(-6, 6)).fade(0.05f, 0.4f));
		}
	}

	// ---- Wall climb ----

	/** Three warm dust puffs where she grips the wall (effect 289: tint (0.83, 0.53, 0.17), 1.1 m, 0.35 s). */
	public static void climbDust(Vec3 at) {
		for (int i = 0; i < 3; i++) {
			// the puff's shape in the game's warm light dust colour (its own colours, tinted, came out dark brown)
			Particle dust = Fx.soft("dust", Palette.NATURAL);
			if (dust == null) {
				return;
			}
			float size = range(1.08f, 1.2f) * 0.6f;
			Fx.add(dust.at(at.add(randomUnit(Fx.RANDOM).scale(0.2))).velocity(new Vec3(0, range(0.2f, 0.4f), 0)).life(range(0.33f, 0.4f))
				.size(size * 0.7f, size).spin(range(-Mth.HALF_PI, Mth.HALF_PI), 0).tint(0xEAC072).alpha(0.6f).fade(0.1f, 0.6f));
		}
	}

	private static float @Nullable [] torii;
	private static @Nullable SkinnedModel toriiFrom;

	/** The torii gate mesh from the local pack (hero_models/torii.glb), as triangles one metre wide; null without. */
	private static float @Nullable [] toriiMesh() {
		HeroModels.Loaded loaded = HeroModels.get("torii");
		if (loaded == null) {
			return null;
		}
		if (loaded.model() != toriiFrom) {
			toriiFrom = loaded.model();
			torii = triangles(loaded.model());
		}
		return torii;
	}

	/**
	 * A static model's triangles (x, y, z, u, v per corner), turned so its wider horizontal side is X, centred on X and
	 * Z, standing on y = 0 and scaled to one metre wide.
	 */
	private static float[] triangles(SkinnedModel model) {
		Matrix4f[] skin = Skeleton.skin(model, new Skeleton.Pose(model.nodeNames.length).rest(model), -1, null);
		List<float[]> corners = new ArrayList<>();
		Vector3f lo = new Vector3f(Float.MAX_VALUE);
		Vector3f hi = new Vector3f(-Float.MAX_VALUE);
		for (SkinnedModel.Piece piece : model.pieces) {
			float[] pos = piece.pos();
			int[] idx = piece.indices();
			for (int i : idx) {
				Vector3f at = skin[piece.joint4()[i * 4]].transformPosition(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2], new Vector3f());
				lo.min(at);
				hi.max(at);
				corners.add(new float[] {at.x, at.y, at.z, piece.uv()[i * 2], piece.uv()[i * 2 + 1]});
			}
		}
		boolean turn = hi.z - lo.z > hi.x - lo.x;
		float width = Math.max(hi.x - lo.x, hi.z - lo.z);
		float cx = (lo.x + hi.x) / 2;
		float cz = (lo.z + hi.z) / 2;
		float[] out = new float[corners.size() * 5];
		for (int i = 0; i < corners.size(); i++) {
			float[] c = corners.get(i);
			float x = c[0] - cx;
			float z = c[2] - cz;
			out[i * 5] = (turn ? z : x) / width;
			out[i * 5 + 1] = (c[1] - lo.y) / width;
			out[i * 5 + 2] = (turn ? -x : z) / width;
			out[i * 5 + 3] = c[3];
			out[i * 5 + 4] = c[4];
		}
		return out;
	}

	private static Particle glow(String mask, Palette p, Vec3 at, float size, float life, float alpha) {
		Particle g = Fx.particle(mask, p);
		return g == null ? null : g.at(at).life(life).size(size, size).alpha(alpha).fade(0.15f, 0.6f);
	}
}
