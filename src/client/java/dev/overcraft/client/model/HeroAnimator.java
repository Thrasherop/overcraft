package dev.overcraft.client.model;

import dev.overcraft.client.effect.KirikoFx;
import dev.overcraft.hero.OwMovement;
import dev.overcraft.registry.OcSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Which clip a hero plays, from what they're doing, and the pose that gives. The base clip follows movement (idle,
 * run and its directions, crouch, crouch-walk, jump, fall, land, wall climb), crossfading over {@link #FADE} seconds;
 * actions (throws, casts: {@link ActionTrack}) play over the upper body on top. Missing clips fall back (crouch_walk
 * to crouch_idle, ...).
 */
public final class HeroAnimator {
	public static final float FADE = 0.15f;
	/** Metres per first-person walk-bob cycle (the research reads Overwatch's as distance-driven, scale 2). */
	private static final float WALK_CYCLE = 2.0f;
	/** Ticks a landing plays after a fall at least this fast (blocks per tick). */
	private static final double HARD_LANDING = 0.4;
	/**
	 * Ground speed (m/s) the run and crouch-walk clips cover at 1x, measured from the planted foot in Kiriko's clips
	 * (tools/clipmaps/kiriko.txt): they play faster or slower to match how fast the hero really moves, so the feet
	 * don't slide (Overwatch runs 5.5 m/s).
	 */
	private static final double RUN_CLIP_SPEED = 3.4;
	private static final double CROUCH_CLIP_SPEED = 2.7;
	/**
	 * How far (m) the model moves back from the wall while climbing: Overwatch climbs pressed to the wall, hands 0.6 m
	 * in front of the feet; a Minecraft player stops 0.3 m short of it, which would put the hands in the blocks.
	 */
	public static final float CLIMB_SHIFT = 0.3f;
	/** Seconds between the climb's hand grips (dust and scrape): the climb clip's hands land twice a 1.03 s cycle. */
	private static final float CLIMB_DUST_EVERY = 0.5f;
	/** The aim sheet's frames per second (its 63 frames are keyed 1/30 s apart in the export). */
	private static final float AIM_FPS = 30;
	/** Aim pitch (degrees) at the ends of the sheet's pitch row (ours: Minecraft's whole range). */
	private static final float AIM_PITCH = 90;
	/** Which end of the row is looking down: frame 21 tilts the spine one way, 41 the other (checked in game). */
	private static final float AIM_SIGN = -1;
	/** How much of the pitch the sheet's spine, neck and head turn at its ends (27-31 of 90 degrees, read from it). */
	private static final float AIM_SHARE = 0.3f;

	private static final Map<Integer, HeroAnimator> BY_ENTITY = new HashMap<>();

	private String base = "idle";
	/** Seconds into the base clip (it can play faster or slower than real time: {@link #rate}). */
	private float basePhase;
	private float rate = 1;
	private @Nullable String previous;
	private float previousPhase;
	private float switchedAt = -100;
	private float lastNow = -1;
	private float climbShift;
	private final ActionTrack actions = new ActionTrack();
	private final ActionTrack actionsFirstPerson = new ActionTrack();
	/** Metres walked on the ground (drives the first-person walk bob, as Overwatch's does). */
	private float walked;
	private float groundSpeed;
	private boolean wasOnGround = true;
	private double fallSpeed;
	private float landedAt = -100;
	private int climbTicks;
	private boolean wasClimbing;
	/** Its motion last tick (blocks per tick), for the cloth's lag. */
	private double tickVx, tickVy, tickVz;
	private @Nullable ClothSim cloth;
	private @Nullable SkinnedModel clothFor;
	private float dustAt = -10;
	private float climbTopAt = -100;
	private long lastTick = -1;
	private float lastSeen;

	public static HeroAnimator of(LivingEntity entity) {
		return BY_ENTITY.computeIfAbsent(entity.getId(), id -> new HeroAnimator());
	}

	/** An action happened (from the server): plays over the upper body (third person) and the view model. */
	public static void trigger(int entityId, String action, float now) {
		HeroAnimator a = BY_ENTITY.computeIfAbsent(entityId, id -> new HeroAnimator());
		a.actions.trigger(action, now);
		a.actionsFirstPerson.trigger(action, now);
	}

	/** How far (m) to draw the model back from where it faces this frame: away from the wall it's climbing. */
	public float climbShift() {
		return climbShift;
	}

	private static boolean isClimb(String clip) {
		return clip.equals("climb") || clip.equals("climb_top");
	}

	/** The third-person action playing now ("throw_suzu", ...), or null. */
	public @Nullable String action() {
		return actions.playing();
	}

	/** The clips playing now (base, and the action on top if any), for the dev harness. */
	/** The first-person view's action step now (dev harness). */
	public @Nullable String playingFirstPerson() {
		return actionsFirstPerson.playing();
	}

	public String playing() {
		String action = actions.playing();
		return action == null ? base : base + "+" + action;
	}

	public static void clear() {
		BY_ENTITY.clear();
	}

	/** Drops animators of entities not drawn for a while. */
	public static void prune(float now) {
		BY_ENTITY.values().removeIf(a -> now - a.lastSeen > 10);
	}

	/**
	 * First person: skin matrices for the hero's view model (Overwatch's first-person arms, a model of its own) and, in
	 * {@code view}, the transform from model space to the camera's (looking down +Z): Overwatch animates the Camera
	 * bone along with the arms (throws turn and dip it), and the arms are drawn as that camera sees them. The base is
	 * the first-person idle, a held pose (A3A7 for Kiriko: hands crossed in front) or the climb, with Overwatch's
	 * breathing and walk-bob layers, then the actions (their own track: first-person clips last differently).
	 */
	/** The ofuda hand's fingertip in the model's space, as last posed by poseFirstPerson. */
	public final Vector3f fingertip = new Vector3f();

	public Matrix4f[] poseFirstPerson(SkinnedModel model, LivingEntity entity, float now, Matrix4f view) {
		lastSeen = now;
		long tick = entity.level().getGameTime();
		if (tick != lastTick) {
			lastTick = tick;
			tickState(model, entity, now);
		}
		float dt = lastNow < 0 ? 0 : Math.clamp(now - lastNow, 0, 0.25f);
		lastNow = now;
		basePhase += dt * rate;

		Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
		boolean climb = isClimb(base) && model.clips.containsKey(base);
		SkinnedModel.Clip baseClip = clip(model, climb ? base : "idle");
		if (baseClip != null) {
			Skeleton.sample(baseClip, climb ? time(baseClip, basePhase, loops(base)) : 0, pose);
		}
		SkinnedModel.Clip breathe = clip(model, "add_breathe");
		if (breathe != null) {
			Skeleton.addAdditive(breathe, time(breathe, now, true), pose, 1);
		}
		SkinnedModel.Clip walk = clip(model, "add_walk");
		if (walk != null && !climb) {
			// the bob follows the distance walked, one cycle per WALK_CYCLE metres; full strength at running speed
			float cycle = (walked % WALK_CYCLE) / WALK_CYCLE * walk.duration();
			Skeleton.addAdditive(walk, cycle, pose, Math.clamp(groundSpeed / 5.5f, 0, 1));
		}
		actionsFirstPerson.apply(model, pose, now, null);
		int tip = UpperBody.fingertip(model);
		if (tip >= 0) {
			Skeleton.world(model, pose, tip).getTranslation(fingertip);
		}
		int camera = UpperBody.eye(model);
		if (camera >= 0) {
			// the camera's transform now, with its rest turn taken out (at rest it looks along the model's facing)
			Matrix4f restRotation = new Matrix4f().rotation(Skeleton.world(model, new Skeleton.Pose(model.nodeNames.length).rest(model), camera)
				.getUnnormalizedRotation(new Quaternionf()).normalize());
			view.set(Skeleton.world(model, pose, camera)).mul(restRotation.invert()).invert();
		} else {
			view.translation(0, -1.6f, 0);
		}
		return Skeleton.skin(model, pose, -1, null);
	}

	/** Skin matrices for the entity this frame. {@code now} in seconds (game time + partial tick). */
	public Matrix4f[] pose(SkinnedModel model, LivingEntity entity, float now, float netHeadYaw, float headPitch) {
		lastSeen = now;
		long tick = entity.level().getGameTime();
		if (tick != lastTick) {
			lastTick = tick;
			tickState(model, entity, now);
		}

		float dt = lastNow < 0 ? 0 : Math.clamp(now - lastNow, 0, 0.25f);
		lastNow = now;
		basePhase += dt * rate;
		previousPhase += dt;

		Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
		SkinnedModel.Clip baseClip = clip(model, base);
		if (baseClip != null) {
			Skeleton.sample(baseClip, time(baseClip, basePhase, loops(base)), pose);
		}
		float fade = Math.clamp((now - switchedAt) / FADE, 0, 1);
		SkinnedModel.Clip prevClip = previous == null ? null : clip(model, previous);
		if (prevClip != null && fade < 1) {
			Skeleton.Pose old = new Skeleton.Pose(model.nodeNames.length).rest(model);
			Skeleton.sample(prevClip, time(prevClip, previousPhase, loops(previous)), old);
			Skeleton.blend(old, pose, fade, null);
			pose = old;
		}
		// root motion: Overwatch moves the character through the bone above the pelvis (climb_top lifts it 2.2 m);
		// Minecraft already moves the player, so that bone stays where it rests
		int mover = UpperBody.mover(model);
		if (mover >= 0) {
			pose.t[mover].set(model.restT[mover]);
		}
		float climbNow = isClimb(base) ? 1 : 0;
		float climbBefore = previous != null && isClimb(previous) ? 1 : 0;
		climbShift = CLIMB_SHIFT * (climbBefore + (climbNow - climbBefore) * fade);
		// Overwatch's breathing layer under the stand (standing or crouched)
		SkinnedModel.Clip breathe = clip(model, base.equals("crouch_idle") ? "add_breathe_crouch" : "add_breathe");
		if (breathe != null && (base.equals("idle") || base.equals("crouch_idle"))) {
			Skeleton.addAdditive(breathe, time(breathe, now, true), pose, 1);
		}
		actions.apply(model, pose, now, UpperBody.mask(model));
		boolean asleep = entity.isSleeping();
		if (asleep || entity.isFallFlying() || entity.isVisuallySwimming()) {
			// lying in bed: her idle arms, legs straight (the model's rest pose). Laid flat, the idle stance's bent
			// forward leg pointed at the sky (found playing)
			for (int i = 0; i < model.nodeNames.length; i++) {
				if (isLeg(model.nodeNames[i])) {
					pose.r[i].set(model.restR[i]);
				}
			}
		}
		// Overwatch's aim pose sheet (A71F) on top of everything: her spine, neck and arms follow where she aims up or
		// down. Its middle row (frames 21-41) runs through the aim's pitch, 31 level; the rows either side are aim yaw,
		// not needed while her body faces her aim
		SkinnedModel.Clip aim = asleep ? null : clip(model, "add_aim");
		float lookPitch = asleep ? 0 : headPitch;
		if (aim != null) {
			float frame = 31 + AIM_SIGN * 10 * Math.clamp(headPitch / AIM_PITCH, -1, 1);
			Skeleton.addAdditive(aim, frame / AIM_FPS, pose, 1, UpperBody.aimMask(model));
			// the sheet already tilts the spine, neck and head by about this share of the pitch: the head turns the rest
			lookPitch = headPitch * (1 - AIM_SHARE);
		}

		// the cloth the clips leave rigid (coat tails): simulated, kept out of the legs
		if (clothFor != model) {
			clothFor = model;
			cloth = ClothSim.of(model);
		}
		if (cloth != null) {
			// her motion this frame, turned into the model's frame (the renderer turns the model by -facing)
			Vector3f moved = new Vector3f((float) tickVx, (float) tickVy, (float) tickVz).mul(20 * dt)
				.rotateY((float) Math.toRadians(facing(entity, 1)));
			cloth.step(model, pose, dt, moved);
		}

		Quaternionf look = asleep ? new Quaternionf()
			: new Quaternionf().rotateY(-netHeadYaw * Mth.DEG_TO_RAD).rotateX(lookPitch * Mth.DEG_TO_RAD);
		return Skeleton.skin(model, pose, UpperBody.head(model), look);
	}

	/**
	 * A leg bone by Overwatch's skeleton names: the hips (the legs' own parent, under the pelvis with the spine: the idle
	 * stance tilts it, which laid flat raised one leg) down to the toes.
	 */
	private static boolean isLeg(String name) {
		return name.equals("Hips") || name.startsWith("Thigh") || name.startsWith("Knee") || name.startsWith("Foot")
			|| name.startsWith("Toe");
	}

	/**
	 * The yaw her model is drawn at: where she aims, as Overwatch's heroes always face their aim and step in the
	 * direction they move with the directional clips. Minecraft turns the body up to 75 degrees toward the movement,
	 * which on top of a strafe clip read as a split from the side (the critique). On a wall, Minecraft's body yaw (she
	 * faces the wall while looking about).
	 */
	public float facing(LivingEntity entity, float partialTicks) {
		if (wasClimbing || isClimb(base)) {
			return Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
		}
		return Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
	}

	private void tickState(SkinnedModel model, LivingEntity entity, float now) {
		double vx = entity.getX() - entity.xo;
		double vy = entity.getY() - entity.yo;
		double vz = entity.getZ() - entity.zo;
		tickVx = vx;
		tickVy = vy;
		tickVz = vz;
		boolean onGround = entity.onGround();
		if (!onGround) {
			fallSpeed = Math.max(fallSpeed, -vy);
		} else if (!wasOnGround) {
			if (fallSpeed >= HARD_LANDING) {
				landedAt = now;
			}
			fallSpeed = 0;
		}
		wasOnGround = onGround;
		// climbing: the local player's own climb state; others: going up steadily at about the climb speed
		boolean climbing = OwMovement.isClimbing(entity);
		if (!climbing && !onGround && vy > 0.3 && vy < 0.5) {
			climbTicks++;
		} else {
			climbTicks = 0;
		}
		climbing |= climbTicks > 3;
		// out of wall while still going up: momentum carries her over the top, with the hop over the edge
		if (wasClimbing && !climbing && vy > 0 && !entity.horizontalCollision) {
			climbTopAt = now;
			entity.level().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), OcSounds.KIRIKO_CLIMB_TOP, SoundSource.PLAYERS, 1, 1, false);
		}
		if (climbing && !wasClimbing) {
			entity.level().playLocalSound(entity.getX(), entity.getY() + 1, entity.getZ(), OcSounds.KIRIKO_CLIMB_START, SoundSource.PLAYERS, 1, 1, false);
		}
		wasClimbing = climbing;
		if (climbing && now - dustAt > CLIMB_DUST_EVERY) {
			// a puff of dust where her hands grip the wall, each reach (the game's wall-climb effect 289)
			dustAt = now;
			double facing = Math.toRadians(entity.getYRot());
			// her box touches the wall: just in front of its face (further in, the wall hides the puff)
			double reach = entity.getBbWidth() / 2 - 0.1;
			Vec3 hands = entity.position().add(-Math.sin(facing) * reach, entity.getBbHeight() + 0.1, Math.cos(facing) * reach);
			KirikoFx.climbDust(hands);
			// and its scrape, heard by whoever sees her climb (the climb is moved client-side, so played here)
			entity.level().playLocalSound(hands.x, hands.y, hands.z, OcSounds.KIRIKO_CLIMB_STEP, SoundSource.PLAYERS, 1, 1, false);
		}
		SkinnedModel.Clip top = clip(model, "climb_top");
		boolean overTheTop = top != null && now - climbTopAt < top.duration();

		double yaw = Math.toRadians(facing(entity, 1));
		double forward = -vx * Math.sin(yaw) + vz * Math.cos(yaw);
		double left = vx * Math.cos(yaw) + vz * Math.sin(yaw);
		double speed = Math.sqrt(vx * vx + vz * vz);
		boolean moving = speed > 0.02;
		groundSpeed = onGround ? (float) speed * 20 : 0;
		if (onGround) {
			walked += (float) speed;
		}
		boolean crouching = entity.getPose() == Pose.CROUCHING;

		String next;
		if (entity.isDeadOrDying()) {
			next = "death";
		} else if (entity.isPassenger()) {
			// riding (a boat, a horse): Overwatch has no sitting clip; crouched is the nearest
			next = "crouch_idle";
		} else if (entity.isFallFlying() || entity.isVisuallySwimming()) {
			// pitched flat by the renderer; legs straightened below
			next = "idle";
		} else if (climbing) {
			next = "climb";
		} else if (overTheTop) {
			next = "climb_top";
		} else if (!onGround && entity.isInWater()) {
			// treading water: Overwatch has no swimming (and hero mode no sprint-swim), so the air pose
			next = "fall";
		} else if (!onGround) {
			next = vy > 0 ? "jump" : "fall";
		} else if (now - landedAt < 0.2f) {
			next = "land";
		} else if (crouching) {
			next = moving ? "crouch_walk" + direction(forward, left) : "crouch_idle";
		} else if (moving) {
			next = switch (direction(forward, left)) {
				case "_left" -> "strafe_left";
				case "_right" -> "strafe_right";
				case "" -> "run";
				case String d -> "run" + d;
			};
		} else {
			next = "idle";
		}
		next = resolve(model, next);
		if (!next.equals(base)) {
			previous = base;
			previousPhase = basePhase;
			switchedAt = now;
			base = next;
			basePhase = 0;
		}
		double metresPerSecond = speed * 20;
		if (base.startsWith("run") || base.startsWith("strafe")) {
			rate = (float) Math.clamp(metresPerSecond / RUN_CLIP_SPEED, 0.5, 2);
		} else if (base.startsWith("crouch_walk")) {
			// standing still crouched with no crouch idle clip: the crouch-walk's pose, held
			rate = moving ? (float) Math.clamp(metresPerSecond / CROUCH_CLIP_SPEED, 0.5, 2) : 0;
		} else if (base.equals("idle") || base.equals("crouch_idle")) {
			// Overwatch doesn't play its stand clips on the clock: their frame follows a game parameter (turn-in-place
			// progress) and holds while it doesn't change. Frame 0 is the neutral stand (kiriko-research.md)
			rate = 0;
		} else {
			rate = 1;
		}
	}

	/** Which of 8 directions the movement goes, relative to the body: "" (forward), "_forward_left", "_left", ... */
	private static String direction(double forward, double left) {
		int sector = Math.floorMod((int) Math.round(Math.atan2(left, forward) / (Math.PI / 4)), 8);
		return switch (sector) {
			case 1 -> "_forward_left";
			case 2 -> "_left";
			case 3 -> "_back_left";
			case 4 -> "_back";
			case 5 -> "_back_right";
			case 6 -> "_right";
			case 7 -> "_forward_right";
			default -> "";
		};
	}

	private static boolean loops(String clip) {
		return !(clip.equals("jump") || clip.equals("land") || clip.equals("climb_top") || clip.equals("death"));
	}

	private static float time(SkinnedModel.Clip clip, float t, boolean loop) {
		if (clip.duration() <= 0) {
			return 0;
		}
		return loop ? t % clip.duration() : Math.min(t, clip.duration());
	}

	private static SkinnedModel.@Nullable Clip clip(SkinnedModel model, String name) {
		return model.clips.get(name);
	}

	/** The clip to play for a state the model may not have. */
	private static String resolve(SkinnedModel model, String want) {
		String name = want;
		while (!model.clips.containsKey(name)) {
			String fallback = switch (name) {
				// 8 directions down to 4 (models with only forward/back/left/right)
				case "run_forward_left", "run_forward_right" -> "run";
				case "run_back_left", "run_back_right" -> "run_back";
				case "crouch_walk_forward_left", "crouch_walk_forward_right", "crouch_walk_back" -> "crouch_walk";
				case "crouch_walk_back_left" -> "crouch_walk_left";
				case "crouch_walk_back_right" -> "crouch_walk_right";
				case "crouch_walk_left", "crouch_walk_right" -> "crouch_walk";
				case "crouch_idle" -> "crouch_walk"; // held still (see the rate below)
				case "run_back", "strafe_left", "strafe_right" -> "run";
				case "land", "jump" -> "fall";
				case "climb", "fall", "run" -> "idle";
				default -> null;
			};
			if (fallback == null) {
				return want;
			}
			name = fallback;
		}
		return name;
	}

	/**
	 * The upper body (spine and up, arms): where actions play. Bones by name: Overwatch's own skeletons (DataTool +
	 * io_scene_owm name the main bones) or the Source (ValveBiped) rigs of SFM ports.
	 */
	static final class UpperBody {
		private static final Map<SkinnedModel, boolean[]> MASKS = new HashMap<>();
		private static final Map<SkinnedModel, boolean[]> AIM_MASKS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> HEADS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> MOVERS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> EYES = new HashMap<>();
		private static final Map<SkinnedModel, Integer> FINGERTIPS = new HashMap<>();

		private static int first(SkinnedModel model, String... names) {
			for (String name : names) {
				int i = model.node(name);
				if (i >= 0) {
					return i;
				}
			}
			return -1;
		}

		/**
		 * The bones the aim sheet moves: the upper body's named bones (spine, neck, head, clavicles, arms, hands). Its
		 * unnamed helper bones swing up to 177 degrees and its legs a little: left alone.
		 */
		static boolean[] aimMask(SkinnedModel model) {
			return AIM_MASKS.computeIfAbsent(model, m -> {
				boolean[] upper = mask(m);
				boolean[] aim = new boolean[upper.length];
				for (int i = 0; i < aim.length; i++) {
					aim[i] = upper[i] && !m.nodeNames[i].startsWith("bone_");
				}
				return aim;
			});
		}

		static boolean[] mask(SkinnedModel model) {
			return MASKS.computeIfAbsent(model, m -> {
				boolean[] mask = new boolean[m.nodeNames.length];
				int spine = first(m, "Spine_1", "ValveBiped.Bip01_Spine");
				for (int i = 0; i < mask.length; i++) {
					for (int p = i; p >= 0; p = m.parent[p]) {
						if (p == spine) {
							mask[i] = true;
							break;
						}
					}
				}
				return mask;
			});
		}

		/** The bone that carries the clips' root motion: the pelvis's parent, unless that's the skeleton's root. */
		static int mover(SkinnedModel model) {
			return MOVERS.computeIfAbsent(model, m -> {
				int pelvis = first(m, "Pelvis");
				int parent = pelvis >= 0 ? m.parent[pelvis] : -1;
				return parent >= 0 && m.parent[parent] >= 0 ? parent : -1;
			});
		}

		/** Overwatch's first-person eye: the Camera bone. */
		static int eye(SkinnedModel model) {
			return EYES.computeIfAbsent(model, m -> first(m, "Camera"));
		}

		/** The ofuda hand's index fingertip (the hand on the left of the view: the model's "_R" bones, as drawn there). */
		static int fingertip(SkinnedModel model) {
			return FINGERTIPS.computeIfAbsent(model, m -> first(m, "IndexFinger_3_R"));
		}

		static int head(SkinnedModel model) {
			return HEADS.computeIfAbsent(model, m -> first(m, "Head", "ValveBiped.Bip01_Head"));
		}
	}
}
