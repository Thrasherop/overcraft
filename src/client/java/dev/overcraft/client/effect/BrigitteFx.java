package dev.overcraft.client.effect;

import dev.overcraft.client.effect.EffectArt.Palette;
import dev.overcraft.client.effect.Fx.Particle;
import dev.overcraft.client.effect.Fx.Shape;
import dev.overcraft.client.model.HeroAnimator;
import dev.overcraft.net.EffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Brigitte's effects (tools/reference/brigitte-effects.md, read from her effects' data; each moment's numbers there). Most
 * of what her graphs name are sounds; her visuals are her clips' own effects: the swing arcs, the bash's jets, the whip
 * head's rocket flare, Rally's burst. Her side sees the gold and sky-blue rows, her enemies the red-orange twins (M which
 * team sees which); the barrier is blue to her side and red to her enemies. The server sends her moments
 * (EffectPayload "brigitte_*"); the barrier's field, the whip's flight, the bash's jets and trail, the stun's halo, the pack
 * in flight and its heal are drawn each client tick from what it sent.
 */
public final class BrigitteFx {
	/** Its art, made ahead as the hero is picked (HeroFxArt, #128): what its first uses made (the client log) and what this class names. */
	public static final java.util.List<EffectArt.Art> ART = java.util.List.of(
		new EffectArt.Art("brigitte_barrier_flicker#16", Palette.BRIGITTE_ENERGY_BLUE_N, false),
		new EffectArt.Art("brigitte_barrier_frame", Palette.BRIGITTE_ENERGY_BLUE_N, false),
		new EffectArt.Art("brigitte_barrier_hex", Palette.BRIGITTE_ENERGY_BLUE_N, false),
		new EffectArt.Art("brigitte_barrier_sweep", Palette.BRIGITTE_ENERGY_BLUE_N, false),
		new EffectArt.Art("brigitte_chain_band", Palette.BRIGITTE_CHAIN_N, false),
		new EffectArt.Art("brigitte_cloud_a", Palette.BRIGITTE_RALLY_DOME_N, true),
		new EffectArt.Art("brigitte_dust#2", Palette.NATURAL, false),
		new EffectArt.Art("brigitte_energy_squares", Palette.BRIGITTE_ENERGY_BLUE_N, false),
		new EffectArt.Art("brigitte_flare_star", Palette.BRIGITTE_RALLY_FLARE_N, false),
		new EffectArt.Art("brigitte_heal_dot", Palette.NATURAL, false),
		new EffectArt.Art("brigitte_heal_swirl", Palette.NATURAL, false),
		new EffectArt.Art("brigitte_hex_pattern", Palette.BRIGITTE_BASH_SHIELD_N, false),
		new EffectArt.Art("brigitte_inspire_wisps", Palette.BRIGITTE_INSPIRE, false),
		new EffectArt.Art("brigitte_rally_armor", Palette.BRIGITTE_RALLY_ARMOR, false),
		new EffectArt.Art("brigitte_rally_decal_ring", Palette.BRIGITTE_RALLY_DECAL_N, false),
		new EffectArt.Art("brigitte_shield_front", Palette.BRIGITTE_BASH_EDGE_N, false),
		new EffectArt.Art("brigitte_shield_glow", Palette.BRIGITTE_BREAK_SHIELD_N, false),
		new EffectArt.Art("brigitte_smoke#32", Palette.BRIGITTE_WHIP_SMOKE_N, true),
		new EffectArt.Art("brigitte_smoke_puff#4", Palette.NATURAL, false),
		new EffectArt.Art("brigitte_soft_blob_b", Palette.BRIGITTE_PACK_HEAL_GLOW_N, true),
		new EffectArt.Art("brigitte_spark_streak#2", Palette.BRIGITTE_WHIP_SPARK, false),
		new EffectArt.Art("brigitte_stun_halo", Palette.NATURAL, false),
		new EffectArt.Art("brigitte_stun_star", Palette.NATURAL, false),
		new EffectArt.Art("brigitte_swing_band", Palette.BRIGITTE_SWING_N, false),
		new EffectArt.Art("cat_soft", Palette.BRIGITTE_BASH_FLASH, false),
		new EffectArt.Art("cat_soft", Palette.BRIGITTE_BASH_SPARK, false),
		new EffectArt.Art("cat_soft", Palette.BRIGITTE_WHIP_SPARK, false),
		new EffectArt.Art("cat_soft", Palette.BRIGITTE_WHIP_SPARK_ALT, false),
		new EffectArt.Art("glow", Palette.BRIGITTE_PACK_DOME_N, false),
		new EffectArt.Art("glow", Palette.BRIGITTE_RALLY_GOLD_N, false),
		new EffectArt.Art("lw_lens", Palette.BRIGITTE_PACK_ARRIVE, false),
		new EffectArt.Art("lw_lens", Palette.BRIGITTE_RALLY_LENS, false),
		new EffectArt.Art("mercy_glow", Palette.BRIGITTE_RALLY_ALLY_N, false),
		new EffectArt.Art("spark#4", Palette.BRIGITTE_BASH_STREAK, false),
		new EffectArt.Art("spark#4", Palette.BRIGITTE_RALLY_GOLD_N, false),
		new EffectArt.Art("spark#4", Palette.BRIGITTE_WHIP_SPARK, false));
	/** And as an enemy sees it, for a client seeing another player play the hero. */
	public static final java.util.List<EffectArt.Art> ART_ENEMY = java.util.List.of(
		new EffectArt.Art("brigitte_chain_band", Palette.BRIGITTE_CHAIN_ENEMY_N, false),
		new EffectArt.Art("brigitte_cloud_a", Palette.BRIGITTE_RALLY_DOME_ENEMY_N, true),
		new EffectArt.Art("brigitte_flare_star", Palette.BRIGITTE_RALLY_FLARE_ENEMY_N, false),
		new EffectArt.Art("brigitte_hex_pattern", Palette.BRIGITTE_BASH_SHIELD_ENEMY_N, false),
		new EffectArt.Art("brigitte_inspire_wisps", Palette.ENEMY, false),
		new EffectArt.Art("brigitte_rally_armor", Palette.BRIGITTE_RALLY_ARMOR_ENEMY_N, false),
		new EffectArt.Art("brigitte_rally_decal_ring", Palette.BRIGITTE_RALLY_DECAL_ENEMY_N, false),
		new EffectArt.Art("brigitte_shield_front", Palette.BRIGITTE_BASH_EDGE_ENEMY_N, false),
		new EffectArt.Art("brigitte_shield_glow", Palette.BRIGITTE_BREAK_SHIELD_ENEMY_N, false),
		new EffectArt.Art("brigitte_soft_blob_b", Palette.ENEMY, true),
		new EffectArt.Art("brigitte_swing_band", Palette.BRIGITTE_SWING_ENEMY_N, false),
		new EffectArt.Art("glow", Palette.BRIGITTE_RALLY_GOLD_ENEMY_N, false),
		new EffectArt.Art("glow", Palette.ENEMY, false),
		new EffectArt.Art("lw_lens", Palette.BRIGITTE_RALLY_LENS_ENEMY, false),
		new EffectArt.Art("lw_lens", Palette.ENEMY, false),
		new EffectArt.Art("spark#4", Palette.BRIGITTE_RALLY_GOLD_ENEMY_N, false));

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Whip Shot's head: launched from, along, when; where it turned back, when, for how long (null: still going out). */
	private record Whip(Vec3 from, Vec3 dir, float since, @Nullable Vec3 back, float backSince, float backTime, boolean enemy) {
	}

	/** A state on an entity: since when, until when (Fx.now() seconds), and whether its giver is an enemy of ours. */
	private record Held(float since, float until, boolean enemy) {
	}

	/** A Repair Pack in flight: from, to whom (entity id), since when. */
	private record Pack(Vec3 from, int ally, float since, boolean enemy) {
	}

	private static final Map<Integer, Whip> WHIPS = new HashMap<>();
	private static final Map<Integer, Held> BASHES = new HashMap<>();
	private static final Map<Integer, Held> STUNS = new HashMap<>();

	/** Dev only: what it draws on this entity now. */
	public static String devHeld(int id) {
		return STUNS.containsKey(id) ? "stun " : "";
	}
	private static final Map<Integer, Held> HEALS = new HashMap<>();
	private static final Map<Integer, Held> RALLIED = new HashMap<>();
	private static final Map<Integer, Held> BURSTS = new HashMap<>();
	private static final List<Pack> PACKS = new ArrayList<>();

	/** Whip Shot's head: 80 m/s out to 20 m (BrigitteStats.WHIP_SPEED / WHIP_RANGE). */
	private static final float WHIP_SPEED = 80, WHIP_RANGE = 20;
	/** Repair Pack's speed (55 m/s: BrigitteStats.REPAIR_SPEED). */
	private static final float PACK_SPEED = 55;
	/** The bash's dash (0.3 s) and its trail after it, to 1 s (2284.00D, #685 / #686). */
	private static final float BASH_TIME = (float) dev.overcraft.brigitte.BrigitteStats.BASH_TIME, TRAIL_END = 1.3f;
	/**
	 * The ground dust's tint: its sprite 14D1C is grey with its shape in the alpha (a colour sprite, drawn as it is), tinted
	 * by 220B's row (BRIGITTE_GROUND_DUST: red to gold; ours its middle, #FFB300). Through the palette as a mask it drew squares.
	 */
	private static final int DUST = 0xFFB300;
	/** Rally's flail-head burst, 0.17 s into the cast (27F1). */
	private static final float BURST_AT = 0.17f;

	private BrigitteFx() {
	}

	private static float range(float a, float b) {
		return a + Fx.RANDOM.nextFloat() * (b - a);
	}

	private static Vec3 randomUnit() {
		Vec3 v = new Vec3(Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian(), Fx.RANDOM.nextGaussian());
		return v.lengthSqr() < 1.0E-6 ? UP : v.normalize();
	}

	private static Vec3 facing(Entity e) {
		float yaw = e.getYRot() * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
	}

	private static Vec3 chest(Entity e) {
		return e.position().add(0, e.getBbHeight() * 0.6, 0);
	}

	/** The local player in first person: their own states aren't drawn round their eyes. */
	private static boolean ownFirstPerson(@Nullable Entity e) {
		Minecraft mc = Minecraft.getInstance();
		return e != null && e == mc.player && mc.options.getCameraType().isFirstPerson();
	}

	// ---- each client tick ----

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			return;
		}
		float now = Fx.now();
		for (Player p : mc.level.players()) {
			if ("brigitte".equals(p.getAttached(dev.overcraft.hero.HeroManager.HERO)) && p.isAlive()) {
				field(p, now);
			}
		}
		whips(mc, now);
		bashes(mc, now);
		bursts(mc, now);
		held(mc, STUNS, now, BrigitteFx::stunned);
		held(mc, HEALS, now, BrigitteFx::healing);
		held(mc, RALLIED, now, BrigitteFx::rallied);
		packs(mc, now);
	}

	private interface Draw {
		void draw(Entity entity, Held held, float now);
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

	/**
	 * The barrier's field while it's up (look 13917, material 5B2C: the frame mask, hex lines, a 16-frame hex flicker and a
	 * sweep line through a 3-row LUT; blue to her side, red-orange to her enemies). Drawn over the rectangle the server
	 * blocks (dev.overcraft.brigitte.Barrier: 1.51 m wide, 0.2 m under her feet to 2.64 m up, 0.9 m ahead; Rally 3.75 x
	 * 2.35), facing her; the hex lines and flicker ours at 16 fps, the sweep rising once a second (ours).
	 */
	private static void field(Player p, float now) {
		HeroAnimator a = HeroAnimator.of(p);
		float t = (p.level().getGameTime()) / 20f;
		if (!a.shieldUp(t)) {
			return;
		}
		float rallyFor = a.since("ult_start", t);
		// in Rally (from the cast's 0.7 s to its 10 s, or its stop): 3.75 x 2.35
		boolean rally = rallyFor >= 0.7f && rallyFor < a.since("ult_stop", t) && rallyFor < 10.7f;
		float w = rally ? 3.75f : 1.51f, top = rally ? 2.35f : 2.64f, bottom = -0.2f;
		float h = top - bottom;
		Vec3 ahead = facing(p);
		Vec3 centre = p.position().add(ahead.scale(0.9 + 0.02)).add(0, bottom + h / 2, 0);
		boolean enemy = KirikoFx.enemyOf(p);
		Palette colour = enemy ? Palette.BRIGITTE_BARRIER_ENEMY : Palette.BRIGITTE_BARRIER;
		// fades in over 0.075 s at the raise (0EE4 #245, M)
		float in = Math.min(1, a.since("shield_raise", t) / 0.075f);
		// her own view looks through it: half as strong there (ours)
		in *= ownFirstPerson(p) ? 0.5f : 1;
		// the masks through her own sky-blue row (red-orange for her enemies): read by raw brightness, the LUT's low end (a dim
		// red) turned the frame mask's dark-grey ground into a pink wash over the whole field (found playing; ours, M)
		Palette cells = enemy ? Palette.BRIGITTE_BARRIER_ENEMY : Palette.BRIGITTE_ENERGY_BLUE_N;
		layer("frame", p, centre, ahead, w, h, "brigitte_barrier_frame", cells, 0.8f * in, 0);
		layer("hex", p, centre, ahead, w, h, "brigitte_barrier_hex", cells, 0.25f * in, 0);
		layer("flicker", p, centre, ahead, w, h, "brigitte_barrier_flicker#16", cells, 0.15f * in, 16);
		// the sweep line, rising up the field once a second
		float up = (now % 1f) * h;
		Particle sweep = Fx.attach("brigitte_field/" + p.getId() + "/sweep", centre, () -> {
			Particle s = Fx.particle("brigitte_barrier_sweep", enemy ? colour : Palette.BRIGITTE_ENERGY_BLUE_N);
			return s == null ? null : s.life(0.2f).alpha(0.5f).fade(0.05f, 0.5f).nearFade(0.5f);
		});
		if (sweep != null) {
			sweep.at(centre.add(0, -h / 2 + up, 0)).normal(ahead).size(0.3f, 0.3f).aspect(w / 0.3f).alpha(0.4f * in);
		}
	}

	private static void layer(String name, Player p, Vec3 centre, Vec3 ahead, float w, float h, String mask, Palette colour,
		float alpha, float fps) {
		Particle layer = Fx.attach("brigitte_field/" + p.getId() + "/" + name, centre, () -> {
			Particle s = Fx.particle(mask, colour);
			if (s == null) {
				return null;
			}
			if (fps > 0) {
				s.loop(fps);
			}
			return s.life(0.2f).fade(0.05f, 0.5f).nearFade(0.5f);
		});
		if (layer != null) {
			layer.normal(ahead).size(h, h).aspect(w / h).alpha(alpha);
		}
	}

	/** Whip Shot's head in the world now, or null when no whip is out; on its way back it closes on {@code hand}. */
	public static @Nullable Vec3 whipHead(int id, Vec3 hand) {
		Whip w = WHIPS.get(id);
		if (w == null) {
			return null;
		}
		float now = Fx.now();
		if (w.back == null) {
			return w.from.add(w.dir.scale(Math.min(WHIP_SPEED * (now - w.since), WHIP_RANGE)));
		}
		float u = (now - w.backSince) / w.backTime;
		return u >= 1 ? null : w.back.lerp(hand, Mth.clamp(u, 0, 1));
	}

	/**
	 * The flail's chain (216C / 22CF: a 0.075 m ribbon, the 13DB9 band, olive to white HDR 10; red rows to her enemies), from
	 * the handle to the head while it's off its socket, in the world. Her own view's: viewChain.
	 */
	public static void chain(Entity brigitte, Vec3 from, Vec3 to) {
		Vec3 piece = to.subtract(from);
		float len = (float) piece.length();
		if (len < 0.1f) {
			return;
		}
		if (ownFirstPerson(brigitte)) {
			// (her own view's is viewChain; this renderer can run for her in first person too, and its yellow band then lay across the view)
			return;
		}
		boolean enemy = KirikoFx.enemyOf(brigitte);
		Particle ribbon = Fx.attach("brigitte_chain/" + brigitte.getId(), from, () -> {
			Particle p = Fx.particle("brigitte_chain_band", enemy ? Palette.BRIGITTE_CHAIN_ENEMY_N : Palette.BRIGITTE_CHAIN_N);
			return p == null ? null : p.life(0.08f).fade(0.01f, 0.5f).nearFade(0.3f);
		});
		if (ribbon != null) {
			ribbon.at(from.add(piece.scale(0.5))).axis(piece).size(len, len).aspect(0.075f / len);
		}
	}

	/** Her own view's chain, wide as the data's (0.075 m, the same ribbon as 216C's) and repeating its squares this often along it (ours). */
	private static final float VIEW_CHAIN_WIDTH = 0.075f, VIEW_CHAIN_TILE = 0.5f;
	/** Its squares run along it (ours), and it's laid this many times: the data's HDR 3.5 over a bright sky (ours, by eye). */
	private static final float VIEW_CHAIN_SCROLL = 2;
	private static final int VIEW_CHAIN_LAYERS = 2;

	/**
	 * Her first-person chain, the energy ribbon (33C1: squares, sky blue; v28985, M): drawn in the hand pass, in the arms' own pose, from
	 * the handle to the head as they are drawn this frame. It was a world particle (#113), and so a frame behind them, under another
	 * projection than the hand pass's and behind the depth it clears: the head flew off with no chain to be seen, its ends nowhere near.
	 * The ribbon faces the eye, the origin of the view's space.
	 */
	public static void viewChain(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector,
		org.joml.Vector3f from, org.joml.Vector3f to, float now) {
		net.minecraft.resources.Identifier texture = EffectArt.get("brigitte_energy_squares", Palette.BRIGITTE_ENERGY_BLUE_N);
		float len = from.distance(to);
		if (texture == null || len < 0.1f) {
			return;
		}
		org.joml.Vector3f eye = new org.joml.Matrix4f(poseStack.last().pose()).invert().transformPosition(new org.joml.Vector3f());
		org.joml.Vector3f along = new org.joml.Vector3f(to).sub(from).div(len);
		org.joml.Vector3f toEye = new org.joml.Vector3f(eye).sub(from).normalize();
		org.joml.Vector3f side = new org.joml.Vector3f(along).cross(toEye);
		if (side.lengthSquared() < 1.0E-6f) {
			return;
		}
		side.normalize(VIEW_CHAIN_WIDTH / 2);
		int pieces = Math.max(1, (int) Math.ceil(len / VIEW_CHAIN_TILE));
		float scroll = now * VIEW_CHAIN_SCROLL;
		for (int layer = 0; layer < VIEW_CHAIN_LAYERS; layer++) {
			collector.submitCustomGeometry(poseStack, FxRender.additive(texture), (pose, buffer) -> {
				for (int i = 0; i < pieces; i++) {
					org.joml.Vector3f a = new org.joml.Vector3f(from).add(new org.joml.Vector3f(along).mul(len * i / pieces));
					org.joml.Vector3f b = new org.joml.Vector3f(from).add(new org.joml.Vector3f(along).mul(len * (i + 1) / pieces));
					viewVertex(pose, buffer, a.x + side.x, a.y + side.y, a.z + side.z, 0, scroll);
					viewVertex(pose, buffer, a.x - side.x, a.y - side.y, a.z - side.z, 1, scroll);
					viewVertex(pose, buffer, b.x - side.x, b.y - side.y, b.z - side.z, 1, 1 + scroll);
					viewVertex(pose, buffer, b.x + side.x, b.y + side.y, b.z + side.z, 0, 1 + scroll);
				}
			});
		}
	}

	private static void viewVertex(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, float x, float y, float z, float u, float v) {
		buffer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
			.setLight(0xF000F0).setNormal(pose, 0, 1, 0);
	}

	/** The head in flight: the rocket flare's sparks shooting back from it for its first 0.5 s (2770), then nothing more. */
	private static void whips(Minecraft mc, float now) {
		for (Iterator<Map.Entry<Integer, Whip>> it = WHIPS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Whip> entry = it.next();
			Whip w = entry.getValue();
			Entity brig = mc.level.getEntity(entry.getKey());
			if (brig == null || now - w.since > 3 || w.back != null && now - w.backSince > w.backTime + 0.1f) {
				it.remove();
				continue;
			}
			float t = now - w.since;
			Vec3 head = whipHead(entry.getKey(), brig.getEyePosition());
			if (head == null || t > 0.5f) {
				continue;
			}
			// four jets at 64 a second, 8.2-9.6 m/s back from the nozzle, 0.2-0.26 m, 0.05-0.1 s (ours: 3 a tick)
			for (int i = 0; i < 3; i++) {
				Particle spark = Fx.particle("cat_soft", Fx.RANDOM.nextBoolean() ? Palette.BRIGITTE_WHIP_SPARK : Palette.BRIGITTE_WHIP_SPARK_ALT);
				if (spark != null) {
					Vec3 back = w.dir.scale(-range(8.2f, 9.6f)).add(randomUnit().scale(1.2));
					Fx.add(spark.at(head.add(randomUnit().scale(0.15))).shape(Shape.STREAK).velocity(back).life(range(0.05f, 0.1f))
						.size(range(0.2f, 0.26f), 0.1f).aspect(0.3f).fade(0.01f, 0.5f).nearFade(0.4f));
				}
			}
			if (t > 0.17f && t < 0.22f) {
				// 0.17 s after the launch (0.37 s into the clip): 3 smoke puffs, 1.2-1.5 m, 1-2 s, grey-brown
				for (int i = 0; i < 2; i++) {
					Particle smoke = Fx.soft("brigitte_smoke#32", Palette.BRIGITTE_WHIP_SMOKE_N);
					if (smoke != null) {
						float life = range(1, 2);
						Fx.add(smoke.at(head).velocity(randomUnit().scale(range(2, 4)).add(w.dir.scale(-1))).life(life).loop(32 / life)
							.size(range(0.6f, 0.8f), range(1.2f, 1.5f)).drag(0.2f).alpha(0.5f).fade(0.05f, 0.6f).nearFade(0.8f));
					}
				}
			}
		}
	}

	/**
	 * The bash: its jets for the 0.3 s dash (26A8 at her back's hp 0075 / 0077, (+-0.2, 1.47, -0.19): flames at 64 a second
	 * 8.2-9.6 m/s straight back, 0.2-0.26 m, a gold flash; dust at her feet), then its trail to 1 s after (smoke puffs 0.5 m
	 * 3 a second, an orange glow).
	 */
	private static void bashes(Minecraft mc, float now) {
		for (Iterator<Map.Entry<Integer, Held>> it = BASHES.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Held> entry = it.next();
			Entity brig = mc.level.getEntity(entry.getKey());
			float t = now - entry.getValue().since;
			if (brig == null || t > TRAIL_END) {
				it.remove();
				continue;
			}
			Vec3 ahead = facing(brig);
			Vec3 side = new Vec3(ahead.z, 0, -ahead.x);
			for (int s = -1; s <= 1; s += 2) {
				Vec3 nozzle = brig.position().add(side.scale(0.2 * s)).add(ahead.scale(-0.19)).add(0, 1.47, 0);
				if (t < BASH_TIME) {
					for (int i = 0; i < 2; i++) {
						Particle flame = Fx.particle("cat_soft", Palette.BRIGITTE_BASH_SPARK);
						if (flame != null) {
							Vec3 v = ahead.scale(-range(8.2f, 9.6f)).add(randomUnit().scale(0.6));
							Fx.add(flame.at(nozzle).shape(Shape.STREAK).velocity(v).life(range(0.05f, 0.1f)).size(range(0.2f, 0.26f), 0.1f)
								.aspect(0.35f).fade(0.01f, 0.5f).nearFade(0.4f));
						}
					}
					Particle flash = Fx.particle("cat_soft", Palette.BRIGITTE_BASH_FLASH);
					if (flash != null) {
						Fx.add(flash.at(nozzle).life(0.06f).size(range(0.2f, 0.23f), 0.15f).fade(0.01f, 0.5f).nearFade(0.4f));
					}
				} else if (Fx.RANDOM.nextFloat() < 3f / 20) {
					Particle smoke = Fx.particle("brigitte_smoke_puff#4", Palette.NATURAL);
					if (smoke != null) {
						Fx.add(smoke.at(nozzle).velocity(new Vec3(0, 0.2, 0)).life(range(0.5f, 1)).loop(6).size(0.4f, 0.6f).alpha(0.35f)
							.fade(0.1f, 0.6f).nearFade(0.8f));
					}
				}
			}
			if (t < BASH_TIME && Fx.RANDOM.nextFloat() < 0.6f) {
				Particle dust = Fx.particle("brigitte_dust#2", Palette.NATURAL);
				if (dust != null) {
					Vec3 v = ahead.scale(-range(2.5f, 5)).add(side.scale(range(-1, 1))).add(0, 0.5, 0);
					Fx.add(dust.at(brig.position().add(0, 0.1, 0)).velocity(v).drag(0.1f).life(range(0.25f, 0.6f)).loop(4)
						.size(range(0.6f, 1), 1.2f).tint(DUST).alpha(0.4f).fade(0.05f, 0.6f).nearFade(0.8f));
				}
			}
		}
	}

	/** Rally's flail-head burst, 0.17 s into the cast (27F1): flares 0.35 / 2 m, lens streaks, 3 x 45 sparks, a gold dome flash. */
	private static void bursts(Minecraft mc, float now) {
		for (Iterator<Map.Entry<Integer, Held>> it = BURSTS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Held> entry = it.next();
			Entity brig = mc.level.getEntity(entry.getKey());
			if (brig == null) {
				it.remove();
				continue;
			}
			if (now - entry.getValue().since < BURST_AT) {
				continue;
			}
			it.remove();
			boolean enemy = entry.getValue().enemy;
			// her flail head over her right hand as the cast raises it (hp 0256 / 0076; ours: above her right shoulder)
			Vec3 ahead = facing(brig);
			Vec3 at = brig.position().add(ahead.scale(0.3)).add(new Vec3(ahead.z, 0, -ahead.x).scale(-0.35)).add(0, 2.2, 0);
			Particle flare = Fx.particle("brigitte_flare_star", enemy ? Palette.BRIGITTE_RALLY_FLARE_ENEMY_N : Palette.BRIGITTE_RALLY_FLARE_N);
			if (flare != null) {
				Fx.add(flare.at(at).life(0.5f).size(2, 0.35f).spin(range(0, 6.28f), 0.5f).fade(0.02f, 0.7f).nearFade(0.8f));
			}
			Particle lens = Fx.particle("lw_lens", enemy ? Palette.BRIGITTE_RALLY_LENS_ENEMY : Palette.BRIGITTE_RALLY_LENS);
			if (lens != null) {
				Fx.add(lens.at(at).life(0.4f).size(1, 0.2f).aspect(4).fade(0.02f, 0.7f).nearFade(0.8f));
			}
			Particle dome = Fx.particle("glow", enemy ? Palette.BRIGITTE_RALLY_GOLD_ENEMY_N : Palette.BRIGITTE_RALLY_GOLD_N);
			if (dome != null) {
				Fx.add(dome.at(at).life(0.5f).size(0.8f, 1.6f).easeOut().fade(0.02f, 0.6f).nearFade(0.8f));
			}
			for (int i = 0; i < 45; i++) {
				Particle spark = Fx.particle("spark#4", enemy ? Palette.BRIGITTE_RALLY_GOLD_ENEMY_N : Palette.BRIGITTE_RALLY_GOLD_N);
				if (spark != null) {
					Fx.add(spark.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(0.5f, 5))).gravity(2).life(range(0.25f, 1))
						.size(range(0.1f, 0.2f), 0.05f).aspect(0.25f).fade(0.02f, 0.5f).nearFade(0.5f));
				}
			}
		}
	}

	/**
	 * Stunned (220D, for the stun's 0.75 s): over the head + 0.5 m two 1 m halo bands (DE0 x 247C, dim gold) turning
	 * opposite ways, a turn every 2 s, and star glints (248B, 2.5-8 cm).
	 */
	private static void stunned(Entity e, Held h, float now) {
		Vec3 over = e.position().add(0, e.getBbHeight() + 0.5, 0);
		for (int i = 0; i < 2; i++) {
			int n = i;
			Particle band = Fx.attach("brigitte_stun/" + e.getId() + "/" + i, over, () -> {
				Particle p = Fx.particle("brigitte_stun_halo", Palette.NATURAL);
				return p == null ? null : p.shape(Shape.ARC).arc(new Quaternionf(), Mth.TWO_PI).spin(n * 1.5f, n == 0 ? 3.14f : -3.14f)
					.life(0.2f).size(1, 1.11f).aspect(0.25f).alpha(0.8f).fade(0.05f, 0.5f).nearFade(0.6f);
			});
			if (band != null) {
				band.at(over.add(0, -0.12 + i * 0.1, 0));
			}
		}
		if (Fx.RANDOM.nextFloat() < 8.5f / 20) {
			Particle star = Fx.particle("brigitte_stun_star", Palette.NATURAL);
			if (star != null) {
				Vec3 round = randomUnit().multiply(1, 0, 1).normalize().scale(0.5);
				Fx.add(star.at(over.add(round)).life(range(0.12f, 0.24f)).size(range(0.04f, 0.14f), 0.02f).fade(0.05f, 0.5f).nearFade(0.4f));
			}
		}
	}

	/**
	 * Repair Pack's heal over time (2074, its 2 s): a 1.2-1.5 m swirl on them once a second, rising dots (0.5-0.6 m, 20 a
	 * second drifting up 0.07-0.42 m/s; ours: 4 a second, 0.25 m, so they read on a Minecraft-sized body).
	 */
	private static void healing(Entity e, Held h, float now) {
		boolean enemy = h.enemy;
		if (Fx.RANDOM.nextFloat() < 1f / 20) {
			Particle swirl = Fx.particle("brigitte_heal_swirl", Palette.NATURAL);
			if (swirl != null) {
				float s = range(1.2f, 1.5f);
				Fx.add(swirl.at(chest(e)).life(1).size(s * 0.8f, s).spin(range(0, 6.28f), 2).alpha(0.5f).fade(0.1f, 0.6f).nearFade(0.8f));
			}
		}
		if (Fx.RANDOM.nextFloat() < 4f / 20) {
			Particle dot = Fx.particle("brigitte_heal_dot", Palette.NATURAL);
			if (dot != null) {
				Vec3 side = randomUnit().multiply(1, 0, 1).normalize().scale(e.getBbWidth() * 0.6);
				Fx.add(dot.at(e.position().add(side.x, range(0.2f, e.getBbHeight()), side.z)).velocity(new Vec3(0, range(0.2f, 0.42f), 0))
					.spin(range(0, 6.28f), 2).life(range(0.6f, 1.3f)).size(0.25f, 0.2f).fade(0.1f, 0.5f).nearFade(0.6f));
			}
		}
	}

	/** Rally topping an ally up (21DE, 0.67 s): a gold tint on their body (ours: a soft gold glow over them). */
	private static void rallied(Entity e, Held h, float now) {
		Particle glow = Fx.attach("brigitte_rallied/" + e.getId(), chest(e), () -> {
			Particle p = Fx.particle("mercy_glow", h.enemy ? Palette.BRIGITTE_RALLY_ALLY_ENEMY_N : Palette.BRIGITTE_RALLY_ALLY_N);
			return p == null ? null : p.life(0.3f).alpha(0.35f).fade(0.1f, 0.5f).nearFade(0.8f);
		});
		if (glow != null) {
			glow.size(e.getBbHeight() * 1.3f, e.getBbHeight() * 1.3f).at(chest(e));
		}
	}

	/**
	 * Repair Pack in flight (218D): a 0.5 m core glow (olive to cream) with a 0.2 m gold ribbon behind it and orange sparks,
	 * at 55 m/s from her hand to the ally's hips; the server's arrival takes it from there.
	 */
	private static void packs(Minecraft mc, float now) {
		for (Iterator<Pack> it = PACKS.iterator(); it.hasNext(); ) {
			Pack p = it.next();
			Entity ally = mc.level.getEntity(p.ally);
			if (ally == null || now - p.since > 2) {
				it.remove();
				continue;
			}
			Vec3 to = ally.position().add(0, ally.getBbHeight() * 0.5, 0);
			Vec3 way = to.subtract(p.from);
			double flown = PACK_SPEED * (now - p.since);
			if (flown >= way.length()) {
				it.remove();
				continue;
			}
			Vec3 at = p.from.add(way.normalize().scale(flown));
			int key = System.identityHashCode(p);
			Particle core = Fx.attach("brigitte_pack/" + key, at, () -> {
				Particle c = Fx.particle("glow", p.enemy ? Palette.ENEMY : Palette.BRIGITTE_PACK_CORE_N);
				return c == null ? null : c.life(0.15f).size(0.5f, 0.5f).fade(0.02f, 0.5f).nearFade(0.5f);
			});
			if (core != null) {
				core.at(at);
			}
			Fx.trail(key, at, "brigitte_pack_trail", p.enemy ? Palette.ENEMY : Palette.BRIGITTE_PACK_RIBBON_N, 0.2f, 0.5f, 0.8f);
			for (int i = 0; i < 3; i++) {
				Particle spark = Fx.particle("spark#4", Palette.BRIGITTE_WHIP_SPARK);
				if (spark != null) {
					Fx.add(spark.at(at).shape(Shape.STREAK).velocity(randomUnit().scale(range(1, 5))).life(range(0.14f, 0.32f))
						.size(range(0.15f, 0.3f), 0.05f).aspect(0.3f).fade(0.01f, 0.5f).nearFade(0.4f));
				}
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
			case "brigitte_swing" -> swing(at, dir, e.variant() == 1, enemy, mc.level == null ? null : mc.level.getEntity(e.entity()));
			case "brigitte_whip" -> WHIPS.put(e.entity(), new Whip(at, dir.normalize(), now, null, 0, 0, enemy));
			case "brigitte_whip_back" -> {
				Whip w = WHIPS.get(e.entity());
				if (w != null) {
					WHIPS.put(e.entity(), new Whip(w.from, w.dir, w.since, at, now, Math.max(e.variant(), 1) / 20f, w.enemy));
				}
			}
			case "brigitte_bash" -> BASHES.put(e.entity(), new Held(now, now + TRAIL_END, enemy));
			case "brigitte_bash_hit" -> bashHit(at, dir, enemy);
			case "brigitte_bash_end" -> dev.overcraft.client.ClientBrigitte.ended(e.entity());
			case "brigitte_stun" -> STUNS.put(e.entity(), new Held(now - e.dx(), now + e.variant() / 20f, enemy));
			case "brigitte_stun_end" -> STUNS.remove(e.entity());
			case "brigitte_inspire" -> inspire(at, enemy);
			case "brigitte_pack" -> PACKS.add(new Pack(at, e.entity(), now, enemy));
			case "brigitte_pack_arrive" -> {
				arrive(mc.level == null ? null : mc.level.getEntity(e.entity()), at, enemy);
				HEALS.put(e.entity(), new Held(now, now + e.variant() / 20f, enemy));
			}
			case "brigitte_barrier_break" -> barrierBreak(at, dir, e.variant() == 1, enemy);
			case "brigitte_rally" -> BURSTS.put(e.entity(), new Held(now, now + 1, enemy));
			case "brigitte_rally_pulse" -> rallyPulse(at, enemy);
			case "brigitte_rally_ally" -> RALLIED.put(e.entity(), new Held(now, now + 0.67f, enemy));
			default -> {
				return false;
			}
		}
		return true;
	}

	/**
	 * The swing's arc (26EB forehand / 27CC backhand on hp 0079 = her pelvis pivot, 1.126 m up): the horseshoe band mesh
	 * 5F3F / 5F42 at size 1, reaching 6.5 m ahead, 0.5 s, its alpha 0 -> 0.45 at 60% of its life -> 0, black-amber-tan-sky
	 * blue (red-orange to her enemies). Where the swing's point 6 m ahead is within 2 m of the ground (220B), 4 dust puffs
	 * and sparks there. Her own view: the 1P arcs (5D48) aren't in the pack; the 3P arc is drawn there too (ours).
	 */
	private static void swing(Vec3 feet, Vec3 dir, boolean back, boolean enemy, @Nullable Entity brig) {
		Vec3 ahead = new Vec3(dir.x, 0, dir.z);
		if (ahead.lengthSqr() < 1.0E-6) {
			return;
		}
		ahead = ahead.normalize();
		float[] mesh = Fx.mesh(back ? "brigitte_swing_arc_back" : "brigitte_swing_arc");
		Particle arc = Fx.particle("brigitte_swing_band", enemy ? Palette.BRIGITTE_SWING_ENEMY_N : Palette.BRIGITTE_SWING_N);
		if (mesh != null && arc != null) {
			// the mesh, one metre wide in Fx.mesh, is 6.87 m long at size 1: its far end ahead, centred 3.07 m out from the
			// pivot, its pivot 0.42 m above its lowest point
			Vec3 at = feet.add(ahead.scale(back ? 2.92 : 3.07)).add(0, 1.126 - (back ? 0.85 : 0.42), 0);
			Fx.add(arc.at(at).mesh(mesh, ahead).life(0.5f).size(back ? 7.17f : 6.87f, back ? 7.17f : 6.87f).alpha(0.45f).fade(0.6f, 0.4f)
				.nearFade(1.5f));
		}
		// the ground hit: a 2 m ray down from 6 m ahead at pelvis height
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		Vec3 point = feet.add(ahead.scale(6)).add(0, 1.126, 0);
		var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(point, point.add(0, -2, 0),
			net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE,
			brig == null ? net.minecraft.world.phys.shapes.CollisionContext.empty() : net.minecraft.world.phys.shapes.CollisionContext.of(brig)));
		if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
			return;
		}
		Vec3 ground = hit.getLocation();
		for (int i = 0; i < 4; i++) {
			Particle dust = Fx.particle("brigitte_dust#2", Palette.NATURAL);
			if (dust != null) {
				Fx.add(dust.at(ground.add(0, 0.1, 0)).velocity(randomUnit().multiply(1, 0.3, 1).scale(range(2.5f, 5)).add(0, 0.5, 0)).drag(0.1f)
					.life(range(0.25f, 0.75f)).loop(4).size(i < 2 ? 1 : 0.5f, 1.4f).tint(DUST).alpha(0.45f).fade(0.05f, 0.6f).nearFade(0.8f));
			}
		}
		for (int i = 0; i < 2; i++) {
			Particle spark = Fx.particle("brigitte_spark_streak#2", Palette.BRIGITTE_WHIP_SPARK);
			if (spark != null) {
				Fx.add(spark.at(ground).shape(Shape.STREAK).velocity(randomUnit().multiply(1, 0, 1).add(0, 1, 0).scale(range(2, 4)))
					.life(0.2f).size(range(0.2f, 0.4f), 0.1f).aspect(0.3f).fade(0.01f, 0.5f).nearFade(0.5f));
			}
		}
	}

	/**
	 * The bash's hit on her shield (2206): the shield's imprint (5DF1 at 1.25: 0.74 x 1.14 m, hex pattern, blue HDR 12, red
	 * to her enemies, 0.17 s), its 1.2 x 1.8 m edge (0.13 s) and 7-9 streaks 0.9-1.2 m flying out.
	 */
	private static void bashHit(Vec3 at, Vec3 dir, boolean enemy) {
		Vec3 ahead = new Vec3(dir.x, 0, dir.z).normalize();
		Vec3 side = new Vec3(ahead.z, 0, -ahead.x);
		float[] plate = Fx.mesh("brigitte_shield_plate");
		if (plate != null) {
			Particle imprint = Fx.particle("brigitte_hex_pattern", enemy ? Palette.BRIGITTE_BASH_SHIELD_ENEMY_N : Palette.BRIGITTE_BASH_SHIELD_N);
			if (imprint != null) {
				Fx.add(imprint.at(at.add(0, -0.57, 0)).mesh(plate, side).life(0.17f).size(0.74f, 0.74f).fade(0.02f, 0.6f).nearFade(0.5f));
			}
			Particle edge = Fx.particle("brigitte_shield_front", enemy ? Palette.BRIGITTE_BASH_EDGE_ENEMY_N : Palette.BRIGITTE_BASH_EDGE_N);
			if (edge != null) {
				Fx.add(edge.at(at.add(0, -0.9, 0).add(ahead.scale(0.05))).mesh(plate, side).life(0.13f).size(1.2f, 1.2f).fade(0.02f, 0.6f)
					.nearFade(0.5f));
			}
		}
		int n = 7 + Fx.RANDOM.nextInt(3);
		for (int i = 0; i < n; i++) {
			Particle streak = Fx.particle("spark#4", Palette.BRIGITTE_BASH_STREAK);
			if (streak != null) {
				Vec3 v = ahead.add(side.scale(range(-1.2f, 1.2f))).add(0, range(-0.6f, 0.8f), 0).normalize().scale(range(6, 10));
				Fx.add(streak.at(at).shape(Shape.STREAK).velocity(v).life(range(0.1f, 0.3f)).size(range(0.9f, 1.2f), 0.2f).aspect(0.08f)
					.fade(0.01f, 0.5f).nearFade(0.5f));
			}
		}
	}

	/** Inspire's pulse (2182): one 3.85 m ring (296B at size 3, rising wisps) round her waist, 0.55 s, gold. */
	private static void inspire(Vec3 feet, boolean enemy) {
		float[] ring = Fx.mesh("brigitte_ring");
		Particle wisps = Fx.particle("brigitte_inspire_wisps", enemy ? Palette.ENEMY : Palette.BRIGITTE_INSPIRE);
		if (ring == null || wisps == null) {
			return;
		}
		// the ring stands in its X-Y plane: laid flat with its Y along +Z, from its far edge (half its 3.85 m back)
		Vec3 at = feet.add(0, 0.9, -3.85 / 2);
		Fx.add(wisps.at(at).mesh(ring, new Vec3(1, 0, 0), new Vec3(0, 0, 1)).life(0.55f).size(3.85f, 3.85f).alpha(0.7f).fade(0.1f, 0.6f)
			.nearFade(1));
	}

	/**
	 * Repair Pack's arrival at the ally's hips (2F2E): a dome flash (5F08, 1.8-3 m, 0.3 s, cream to orange), a glow growing
	 * 1 -> 2.2 m over 1 s, a lens streak 0.2 -> 0.55 m.
	 */
	private static void arrive(@Nullable Entity ally, Vec3 feet, boolean enemy) {
		Vec3 hips = ally == null ? feet.add(0, 1, 0) : ally.position().add(0, ally.getBbHeight() * 0.5, 0);
		Particle dome = Fx.particle("glow", enemy ? Palette.ENEMY : Palette.BRIGITTE_PACK_DOME_N);
		if (dome != null) {
			Fx.add(dome.at(hips.add(0, -0.5, 0)).shape(Shape.DOME).life(0.3f).size(1.8f, 3).easeOut().alpha(0.5f).fade(0.02f, 0.7f).nearFade(1));
		}
		Particle glow = Fx.soft("brigitte_soft_blob_b", enemy ? Palette.ENEMY : Palette.BRIGITTE_PACK_HEAL_GLOW_N);
		if (glow != null) {
			Fx.add(glow.at(hips).life(1).size(1, 2.2f).alpha(0.5f).fade(0.05f, 0.7f).nearFade(0.8f));
		}
		Particle lens = Fx.particle("lw_lens", enemy ? Palette.ENEMY : Palette.BRIGITTE_PACK_ARRIVE);
		if (lens != null) {
			Fx.add(lens.at(hips).life(0.5f).size(0.2f, 0.55f).aspect(4).fade(0.02f, 0.7f).nearFade(0.6f));
		}
	}

	/**
	 * The barrier breaking (268C at it, 1 s): the shield's outline flashing at 2.8 (1.65 x 2.56 m, 0.18 s, cream to blue; red
	 * to her enemies), 12 hexes 0.4-0.65 m flying 3-5 m/s, shards 5-14 cm thrown up 4.4-9.4 m/s in a 65 degree cone.
	 */
	private static void barrierBreak(Vec3 at, Vec3 dir, boolean rally, boolean enemy) {
		Vec3 ahead = new Vec3(dir.x, 0, dir.z).normalize();
		Vec3 side = new Vec3(ahead.z, 0, -ahead.x);
		float scale = rally ? 1.2f : 1;
		float[] plate = Fx.mesh("brigitte_shield_plate");
		Particle outline = Fx.particle("brigitte_shield_glow", enemy ? Palette.BRIGITTE_BREAK_SHIELD_ENEMY_N : Palette.BRIGITTE_BREAK_SHIELD_N);
		if (plate != null && outline != null) {
			Fx.add(outline.at(at.add(0, -1.4 * scale, 0)).mesh(plate, side).life(0.18f).size(1.65f * scale, 1.65f * scale).fade(0.02f, 0.6f)
				.nearFade(0.5f));
		}
		for (int i = 0; i < 12; i++) {
			Particle hex = Fx.particle(i % 2 == 0 ? "brigitte_hex_outline" : "brigitte_hex",
				enemy ? Palette.BRIGITTE_BREAK_HEX_ENEMY_N : Palette.BRIGITTE_BREAK_HEX_N);
			if (hex != null) {
				Vec3 from = at.add(side.scale(range(-0.8f, 0.8f) * scale)).add(0, range(-1.2f, 1.2f) * scale, 0);
				Fx.add(hex.at(from).velocity(randomUnit().add(ahead.scale(0.5)).normalize().scale(range(3, 5))).spin(range(0, 6.28f), 4)
					.life(range(0.2f, 0.27f)).size(range(0.4f, 0.65f), 0.3f).fade(0.02f, 0.5f).nearFade(0.5f));
			}
		}
		for (int i = 0; i < 30; i++) {
			Particle shard = Fx.particle(enemy ? "brigitte_shard_orange#4" : "brigitte_shard_blue#4", Palette.NATURAL);
			if (shard != null) {
				Vec3 from = at.add(side.scale(range(-0.8f, 0.8f) * scale)).add(0, range(-1.2f, 1.2f) * scale, 0);
				Vec3 v = randomUnit().multiply(0.6, 0, 0.6).add(0, 1, 0).normalize().scale(range(4.4f, 9.4f) * 0.6f);
				Fx.add(shard.at(from).velocity(v).gravity(9.8f).spin(range(0, 6.28f), 8).life(range(0.25f, 1)).loop(8)
					.size(range(0.05f, 0.14f), 0.05f).fade(0.02f, 0.4f).nearFade(0.4f));
			}
		}
	}

	/**
	 * Rally's pulse (219D, 0.5 s into the cast): a 16 m dome sweeping round her (5F08 at size 8: the 8 m reach), a 16 m ground
	 * ring (5BEE), a 1.2 m armour-pattern shell on her, rising sparks, and gold ground decals 4 m across (220E).
	 */
	private static void rallyPulse(Vec3 feet, boolean enemy) {
		Particle dome = Fx.soft("brigitte_cloud_a", enemy ? Palette.BRIGITTE_RALLY_DOME_ENEMY_N : Palette.BRIGITTE_RALLY_DOME_N);
		if (dome != null) {
			Fx.add(dome.at(feet).shape(Shape.DOME).life(1).size(2, 16).easeOut().alpha(0.25f).fade(0.05f, 0.7f).nearFade(2));
		}
		float[] ring = Fx.mesh("brigitte_rally_ring");
		Particle band = Fx.particle("glow", enemy ? Palette.BRIGITTE_RALLY_GOLD_ENEMY_N : Palette.BRIGITTE_RALLY_GOLD_N);
		if (ring != null && band != null) {
			Fx.add(band.at(feet.add(0, 0.05, 0)).mesh(ring, new Vec3(1, 0, 0)).life(0.75f).size(4, 16).easeOut().alpha(0.5f).fade(0.05f, 0.6f)
				.nearFade(1));
		}
		Particle armor = Fx.particle("brigitte_rally_armor", enemy ? Palette.BRIGITTE_RALLY_ARMOR_ENEMY_N : Palette.BRIGITTE_RALLY_ARMOR);
		if (armor != null) {
			Fx.add(armor.at(feet.add(0, 0.2, 0)).shape(Shape.DOME).life(0.8f).size(2.4f, 2.4f).alpha(0.6f).fade(0.1f, 0.6f).nearFade(1));
		}
		Particle decal = Fx.particle("brigitte_rally_decal_ring", enemy ? Palette.BRIGITTE_RALLY_DECAL_ENEMY_N : Palette.BRIGITTE_RALLY_DECAL_N);
		if (decal != null) {
			Fx.add(decal.at(feet.add(0, 0.03, 0)).normal(UP).life(0.6f).size(3, 4).alpha(0.7f).fade(0.05f, 0.6f).nearFade(0.5f));
		}
		for (int i = 0; i < 25; i++) {
			Particle spark = Fx.particle("spark#4", enemy ? Palette.BRIGITTE_RALLY_GOLD_ENEMY_N : Palette.BRIGITTE_RALLY_GOLD_N);
			if (spark != null) {
				Vec3 round = randomUnit().multiply(1, 0, 1).normalize().scale(range(0.3f, 2));
				Fx.add(spark.at(feet.add(round)).shape(Shape.STREAK).velocity(new Vec3(0, range(0.5f, 2), 0)).life(range(0.25f, 1.5f))
					.size(range(0.1f, 0.3f), 0.05f).aspect(0.25f).fade(0.05f, 0.5f).nearFade(0.5f));
			}
		}
	}

	public static void clear() {
		WHIPS.clear();
		BASHES.clear();
		STUNS.clear();
		HEALS.clear();
		RALLIED.clear();
		BURSTS.clear();
		PACKS.clear();
	}
}
