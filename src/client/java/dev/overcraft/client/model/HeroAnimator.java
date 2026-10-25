package dev.overcraft.client.model;

import dev.overcraft.hero.OwMovement;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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
	/** Ticks a landing plays after a fall at least this fast (blocks per tick). */
	private static final double HARD_LANDING = 0.4;

	private static final Map<Integer, HeroAnimator> BY_ENTITY = new HashMap<>();

	private String base = "idle";
	private float baseStart;
	private @Nullable String previous;
	private float previousStart;
	private float switchedAt = -100;
	private @Nullable String action;
	private float actionStart;
	private boolean wasOnGround = true;
	private double fallSpeed;
	private float landedAt = -100;
	private int climbTicks;
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

	public static void clear() {
		BY_ENTITY.clear();
	}

	/** Drops animators of entities not drawn for a while. */
	public static void prune(float now) {
		BY_ENTITY.values().removeIf(a -> now - a.lastSeen > 10);
	}

	/** Skin matrices for the entity this frame. {@code now} in seconds (game time + partial tick). */
	public Matrix4f[] pose(SkinnedModel model, LivingEntity entity, float now, float netHeadYaw, float headPitch) {
		lastSeen = now;
		long tick = entity.level().getGameTime();
		if (tick != lastTick) {
			lastTick = tick;
			tickState(model, entity, now);
		}

		Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
		SkinnedModel.Clip baseClip = clip(model, base);
		if (baseClip != null) {
			Skeleton.sample(baseClip, time(baseClip, now - baseStart, loops(base)), pose);
		}
		float fade = (now - switchedAt) / FADE;
		SkinnedModel.Clip prevClip = previous == null ? null : clip(model, previous);
		if (prevClip != null && fade < 1) {
			Skeleton.Pose old = new Skeleton.Pose(model.nodeNames.length).rest(model);
			Skeleton.sample(prevClip, time(prevClip, now - previousStart, loops(previous)), old);
			Skeleton.blend(old, pose, Math.max(0, fade), null);
			pose = old;
		}
		SkinnedModel.Clip actionClip = action == null ? null : clip(model, action);
		if (actionClip != null) {
			float t = now - actionStart;
			if (t > actionClip.duration() + FADE) {
				action = null;
			} else {
				Skeleton.Pose act = new Skeleton.Pose(model.nodeNames.length).rest(model);
				Skeleton.sample(actionClip, Math.min(t, actionClip.duration()), act);
				// ease in and out
				float w = Math.min(1, t / 0.05f) * Math.min(1, (actionClip.duration() + FADE - t) / FADE);
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

		double yaw = Math.toRadians(entity.yBodyRot);
		double forward = -vx * Math.sin(yaw) + vz * Math.cos(yaw);
		double left = vx * Math.cos(yaw) + vz * Math.sin(yaw);
		double speed = Math.sqrt(vx * vx + vz * vz);
		boolean moving = speed > 0.02;
		boolean crouching = entity.getPose() == Pose.CROUCHING;

		String next;
		if (climbing) {
			next = "climb";
		} else if (!onGround) {
			next = vy > 0 ? "jump" : "fall";
		} else if (now - landedAt < 0.2f) {
			next = "land";
		} else if (crouching) {
			next = moving ? "crouch_walk" : "crouch_idle";
		} else if (moving) {
			if (Math.abs(left) > Math.abs(forward) * 1.2) {
				next = left > 0 ? "strafe_left" : "strafe_right";
			} else {
				next = forward >= 0 ? "run" : "run_back";
			}
		} else {
			next = "idle";
		}
		next = resolve(model, next);
		if (!next.equals(base)) {
			previous = base;
			previousStart = baseStart;
			switchedAt = now;
			base = next;
			baseStart = now;
		}
	}

	private static boolean loops(String clip) {
		return !(clip.equals("jump") || clip.equals("land"));
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
				case "crouch_walk" -> "crouch_idle";
				case "run_back", "strafe_left", "strafe_right" -> "run";
				case "land", "jump" -> "fall";
				case "climb", "fall", "run", "crouch_idle" -> "idle";
				default -> null;
			};
			if (fallback == null) {
				return want;
			}
			name = fallback;
		}
		return name;
	}

	/** The upper body (spine and up, arms): where actions play. */
	static final class UpperBody {
		private static final Map<SkinnedModel, boolean[]> MASKS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> HEADS = new HashMap<>();

		static boolean[] mask(SkinnedModel model) {
			return MASKS.computeIfAbsent(model, m -> {
				boolean[] mask = new boolean[m.nodeNames.length];
				int spine = m.node("ValveBiped.Bip01_Spine");
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

		static int head(SkinnedModel model) {
			return HEADS.computeIfAbsent(model, m -> m.node("ValveBiped.Bip01_Head"));
		}
	}
}
