package dev.overcraft.client.model;

import dev.overcraft.hero.OwMovement;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Which clip a hero plays, from what they're doing, and the pose that gives. The base clip follows movement (idle,
 * run and its directions, crouch, crouch-walk, jump, fall, land, wall climb), crossfading over {@link #FADE} seconds;
 * actions (throws, casts) play over the upper body on top. Missing clips fall back (crouch_walk to crouch_idle, ...).
 */
public final class HeroAnimator {
	public static final float FADE = 0.15f;
	/** Seconds an action (throw, cast) takes to swing from the current pose to its clip's first frame. */
	private static final float ACTION_IN = 0.1f;
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
	private @Nullable String action;
	private float actionStart;
	private boolean wasOnGround = true;
	private double fallSpeed;
	private float landedAt = -100;
	private int climbTicks;
	private boolean wasClimbing;
	private float climbTopAt = -100;
	private long lastTick = -1;
	private float lastSeen;

	public static HeroAnimator of(LivingEntity entity) {
		return BY_ENTITY.computeIfAbsent(entity.getId(), id -> new HeroAnimator());
	}

	/** An action happened (from the server): plays over the upper body. */
	public static void trigger(int entityId, String clip, float now) {
		HeroAnimator a = BY_ENTITY.computeIfAbsent(entityId, id -> new HeroAnimator());
		a.action = clip;
		a.actionStart = now;
	}

	/** How far (m) to draw the model back from where it faces this frame: away from the wall it's climbing. */
	public float climbShift() {
		return climbShift;
	}

	private static boolean isClimb(String clip) {
		return clip.equals("climb") || clip.equals("climb_top");
	}

	/** The clips playing now (base, and the action on top if any), for the dev harness. */
	public String playing() {
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
	 * First person: skin matrices for the hero's view model (Overwatch's first-person arms, a model of its own) and,
	 * in {@code eye}, where its Camera bone is in model space. Same state as the third-person pose (actions, climbing);
	 * otherwise the first-person idle, a held pose (A3A7 for Kiriko: both hands in front).
	 */
	public Matrix4f[] poseFirstPerson(SkinnedModel model, LivingEntity entity, float now, Vector3f eye) {
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
		SkinnedModel.Clip actionClip = action == null ? null : clip(model, action);
		if (actionClip != null) {
			float t = now - actionStart;
			float end = ACTION_IN + actionClip.duration() + FADE;
			if (t <= end) {
				Skeleton.Pose act = new Skeleton.Pose(model.nodeNames.length).rest(model);
				Skeleton.sample(actionClip, Math.clamp(t - ACTION_IN, 0, actionClip.duration()), act);
				float in = Math.min(1, t / ACTION_IN);
				Skeleton.blend(pose, act, in * in * (3 - 2 * in) * Math.min(1, (end - t) / FADE), null);
			}
		}
		int camera = UpperBody.eye(model);
		eye.set(camera >= 0 ? Skeleton.world(model, pose, camera).getTranslation(new Vector3f()) : new Vector3f(0, 1.6f, 0));
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
		SkinnedModel.Clip actionClip = action == null ? null : clip(model, action);
		if (actionClip != null) {
			float t = now - actionStart;
			float end = ACTION_IN + actionClip.duration() + FADE;
			if (t > end) {
				action = null;
			} else {
				Skeleton.Pose act = new Skeleton.Pose(model.nodeNames.length).rest(model);
				// Overwatch's fire and cast clips start at the release (the shot leaves on the click): swing out to
				// that first frame, then play the clip from it, so the arm is seen going out and not just coming back
				Skeleton.sample(actionClip, Math.clamp(t - ACTION_IN, 0, actionClip.duration()), act);
				float in = Math.min(1, t / ACTION_IN);
				float w = in * in * (3 - 2 * in) * Math.min(1, (end - t) / FADE);
				Skeleton.blend(pose, act, w, UpperBody.mask(model));
			}
		}

		Quaternionf look = new Quaternionf().rotateY(-netHeadYaw * Mth.DEG_TO_RAD).rotateX(headPitch * Mth.DEG_TO_RAD);
		return Skeleton.skin(model, pose, UpperBody.head(model), look);
	}

	private void tickState(SkinnedModel model, LivingEntity entity, float now) {
		double vx = entity.getX() - entity.xo;
		double vy = entity.getY() - entity.yo;
		double vz = entity.getZ() - entity.zo;
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
		}
		wasClimbing = climbing;
		SkinnedModel.Clip top = clip(model, "climb_top");
		boolean overTheTop = top != null && now - climbTopAt < top.duration();

		double yaw = Math.toRadians(entity.yBodyRot);
		double forward = -vx * Math.sin(yaw) + vz * Math.cos(yaw);
		double left = vx * Math.cos(yaw) + vz * Math.sin(yaw);
		double speed = Math.sqrt(vx * vx + vz * vz);
		boolean moving = speed > 0.02;
		boolean crouching = entity.getPose() == Pose.CROUCHING;

		String next;
		if (entity.isDeadOrDying()) {
			next = "death";
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
		private static final Map<SkinnedModel, Integer> HEADS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> MOVERS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> EYES = new HashMap<>();

		private static int first(SkinnedModel model, String... names) {
			for (String name : names) {
				int i = model.node(name);
				if (i >= 0) {
					return i;
				}
			}
			return -1;
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

		static int head(SkinnedModel model) {
			return HEADS.computeIfAbsent(model, m -> first(m, "Head", "ValveBiped.Bip01_Head"));
		}
	}
}
