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
	 * Lúcio's skating strides play on the clock times his speed (first person: df 0x1A, lucio-map.md). Which speed is 1x
	 * isn't read from the data: ours, Overwatch's 5.5 m/s, so the strides quicken with Speed Boost.
	 */
	private static final double SKATE_CLIP_SPEED = 5.5;
	/** Lúcio's idle fidgets (additive, on the clock): one now and then, ours how often (the data: a Random, entered on a condition). */
	private static final float FIDGET_EVERY = 6;
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
	/** When each action last started (s): the additive layers (Lúcio's volleys, Soundwave) and the weapon's own clips. */
	private final Map<String, Float> actionAt = new HashMap<>();
	/** The last action was a whole-body one (Lúcio's Sound Barrier: he leaps), not the upper body's. */
	private boolean wholeBody;
	/** The volley's first-person clip (Overwatch picks one at random per volley). */
	private int fireVariant;
	/** Jetpack Cat's paw: the volleys swap paw each time (her add_fire / add_fire_2). */
	private boolean fireSide;
	/** Jetpack Cat: Catnapper's aim began (s), or -100 when she isn't aiming (its loop plays until the dive or a cancel). */
	private float aimingSince = -100;
	/** When he started skating (first person: a start layer once), stopped (his stop clip), left the ground (his jump). */
	private float movingSince = -100;
	private float stoppedAt = -100;
	private float airborneSince = -100;
	private boolean wasSkating;
	/** Which of 4 ways he skated last, and was going when he left the ground ("_forward", ...: his 4-way clips). */
	private String skateDirection = "_forward";
	private String jumpDirection = "_forward";
	/** The wall a wall ride is on: 1 his right, -1 his left, 0 none. */
	private int wallSide;
	private float fidgetAt = -100;
	private @Nullable String fidget;
	/** Mercy (her synced state, read each tick): she's Mercy, her blaster is out, her beam's mode and since when. */
	private boolean mercy;
	private boolean blaster;
	/**
	 * Lifeweaver in play (ClientLifeweaver.state): Thorn Volley in hand, a blossom charging (and its charge), the thorns
	 * firing, the tree aimed.
	 */
	private boolean lifeweaver;
	private boolean thorns;
	private boolean bloomCharging;
	private float bloomCharge;
	private boolean thornFiring;
	private boolean treeAim;
	/** The way the body faced last tick (degrees): the dash's pose is picked against it. */
	private float lastFacing;
	private int beamMode;
	private float beamOnSince = -100;
	private float beamOffAt = -100;
	private float gaSince = -100;
	private boolean wasGa;
	private int descentTicks;

	/** Mercy's blaster is out (her weapon model: HeroWeapon). */
	/** Mercy's beam mode now (0 none, 1 heal, 2 boost): her staff plays its beam layer meanwhile. */
	public int mercyBeam() {
		return beamMode;
	}

	public boolean blaster() {
		return blaster;
	}

	/** The pose last drawn (third person, first person): the weapon hangs on its bones. */
	private Skeleton.@Nullable Pose lastPose;
	private Skeleton.@Nullable Pose lastViewPose;

	public static HeroAnimator of(LivingEntity entity) {
		return BY_ENTITY.computeIfAbsent(entity.getId(), id -> new HeroAnimator());
	}

	/** The entity's animator, or null when it has none yet. */
	public static @Nullable HeroAnimator existing(int entityId) {
		return BY_ENTITY.get(entityId);
	}

	/** An action happened (from the server): plays over the upper body (third person) and the view model. */
	public static void trigger(int entityId, String action, float now) {
		HeroAnimator a = BY_ENTITY.computeIfAbsent(entityId, id -> new HeroAnimator());
		a.actionAt.put(action, now);
		if (action.equals("fire")) {
			// 2D24, 2D25, 2D26, 2D26: the data's Random of the first-person volleys (lucio-map.md)
			a.fireVariant = new int[] {2, 3, 4, 4}[java.util.concurrent.ThreadLocalRandom.current().nextInt(4)];
			a.fireSide = !a.fireSide;
		}
		// Jetpack Cat's aim loops until she dives or lets it go
		if (action.equals("ult_aim")) {
			a.aimingSince = now;
		} else if (action.equals("ult_aim_end") || action.equals("ult_dive")) {
			a.aimingSince = -100;
		}
		String clip = action;
		if (a.mercy) {
			// her launches are one clip; with the blaster out its clips are the "_blaster" ones where they exist
			clip = action.equals("ga_launch_up") ? "ga_launch" : action;
			if (a.blaster && !clip.endsWith("_blaster")) {
				clip = clip + "_blaster";
			}
		}
		String viewClip = a.mercy ? clip : action;
		if (a.lifeweaver) {
			clip = a.lifeweaverAction(action);
			viewClip = a.lifeweaverViewAction(action);
		}
		if (action.startsWith("crossfade_") && a.wasSkating) {
			// skating, his Crossfade is its own clip, one for forward/back and one for the sides (missing ones fall back)
			clip = action + (a.skateDirection.equals("_left") || a.skateDirection.equals("_right") ? "_moving_side" : "_moving");
		}
		boolean layerOnly = action.equals("fire") || action.equals("soundwave") || action.equals("fire_blaster")
			|| action.startsWith("flash_heal") || action.equals("ga_start") || action.equals("fire_thorn") || action.equals("charge");
		if (!layerOnly) {
			// (those are additive layers or base states only: they don't cut a reload's clip)
			a.wholeBody = action.equals("sound_barrier") || action.equals("ult_dive") || action.equals("ult_land")
				|| a.mercy && action.startsWith("ult_cast");
			a.actions.trigger(clip, now);
			a.actionsFirstPerson.trigger(viewClip, now);
		} else if (!a.mercy && !a.lifeweaver) {
			a.actions.trigger(clip, now);
			a.actionsFirstPerson.trigger(action, now);
		}
		// doing anything ends an emote, as in Overwatch
		a.emote = null;
	}

	/**
	 * An emote started (the communication wheel, dev.overcraft.comms): the hero's emote_<id>_<n> clips play over the
	 * whole body as their EmotePlan chains them: a one-shot once, a dance or a kneel looping until the hero moves or does
	 * something else.
	 */
	public static void emote(int entityId, String emote, float now) {
		HeroAnimator a = BY_ENTITY.computeIfAbsent(entityId, id -> new HeroAnimator());
		// no id: the emote ends (Comms.endEmote)
		a.emote = emote.isEmpty() ? null : emote;
		a.emoteStart = now;
		a.emoteFrom = null;
	}

	/** An emote is playing (the local player's camera goes to third person meanwhile: ClientHero). */
	public boolean emoting() {
		return emote != null;
	}

	/** The emote playing now, or null. */
	public @Nullable String emote() {
		return emote;
	}

	/** When the emote started (game time / 20): a new start of the same emote has a new one. */
	public float emoteStart() {
		return emoteStart;
	}

	/** Seconds since the emote started, at this time ((game time + partial) / 20, as poses are). */
	public float emoteTime(float now) {
		return Math.max(0, now - emoteStart);
	}

	/** The emote clip playing now (emote_<id>_<n>), or null. */
	public @Nullable String emoteClip() {
		return emote == null ? null : emoteClip;
	}

	/** Seconds into {@link #emoteClip()} (into the loop's current turn for a loop). */
	public float emoteAt() {
		return emoteAt;
	}

	/** {@link #emoteClip()}'s length, seconds. */
	public float emoteClipLength() {
		return emoteClipLength;
	}

	private static final Map<SkinnedModel, Map<String, SkinnedModel.Clip>> DRIVEN = new java.util.WeakHashMap<>();

	/**
	 * The clip with only the channels it drives. Our export writes every bone into every clip, filling the ones a clip
	 * doesn't drive with their rest pose (constant): those are left out, so the pose under the clip shows through.
	 */
	static SkinnedModel.Clip driven(SkinnedModel model, SkinnedModel.Clip clip) {
		return DRIVEN.computeIfAbsent(model, m -> new HashMap<>()).computeIfAbsent(clip.name(), name -> {
			java.util.List<SkinnedModel.Channel> kept = new java.util.ArrayList<>();
			for (SkinnedModel.Channel ch : clip.channels()) {
				if (!restFiller(model, ch)) {
					kept.add(ch);
				}
			}
			return new SkinnedModel.Clip(clip.name(), clip.duration(), kept);
		});
	}

	/** Whether every key of the channel is its bone's rest value (to 1e-4; a rotation either sign). */
	private static boolean restFiller(SkinnedModel model, SkinnedModel.Channel ch) {
		int node = ch.node();
		float[] rest = switch (ch.path()) {
			case 'T' -> new float[] {model.restT[node].x, model.restT[node].y, model.restT[node].z};
			case 'S' -> new float[] {model.restS[node].x, model.restS[node].y, model.restS[node].z};
			case 'R' -> new float[] {model.restR[node].x, model.restR[node].y, model.restR[node].z, model.restR[node].w};
			default -> null;
		};
		if (rest == null) {
			return false;
		}
		float[] v = ch.values();
		for (int i = 0; i + rest.length <= v.length; i += rest.length) {
			boolean same = true;
			boolean flipped = ch.path() == 'R';
			for (int j = 0; j < rest.length; j++) {
				same &= Math.abs(v[i + j] - rest[j]) <= 1.0E-4f;
				flipped &= Math.abs(v[i + j] + rest[j]) <= 1.0E-4f;
			}
			if (!same && !flipped) {
				return false;
			}
		}
		return true;
	}

	/** Her skeleton as last posed in an emote (her props hang on its bones: EmoteProps), or null before any. */
	public Skeleton.@Nullable Pose emotePose() {
		return emote == null ? null : emotePose;
	}

	/**
	 * Props that fly off when their time is up (EmoteProps): where each was last shown, how fast it was moving then
	 * (model space, m/s) and when (the emote clip's time), by clip and spawn.
	 */
	public record Released(Matrix4f at, Vector3f velocity, float time) {
	}

	public final java.util.Map<String, Released> released = new java.util.HashMap<>();
	/** How far her emote's root motion would have moved her (model space), kept off her: her props move by it instead. */
	public final Vector3f emoteRootShift = new Vector3f();
	/** Dev only (DevServer "emoteat"): every emote held this many seconds in, or -1 to play. */
	public static float devEmoteAt = -1;
	private @Nullable String emoteClip;
	private float emoteAt;
	private float emoteClipLength;
	private Skeleton.@Nullable Pose emotePose;

	private @Nullable String emote;
	private float emoteStart;
	private @Nullable Vec3 emoteFrom;
	/** How far (m) the hero may drift before an emote ends (ours: Minecraft nudges players a little standing still). */
	private static final double EMOTE_MOVE = 0.15;
	/** Seconds an emote eases in from the movement pose, and a one-shot back out to it (ours). */
	private static final float EMOTE_EASE = 0.2f;

	/** The emote's pose at this moment over the whole body, or false when there's none (or it just ended). */
	private boolean poseEmote(SkinnedModel model, LivingEntity entity, float now, Skeleton.Pose pose) {
		if (emote == null) {
			return false;
		}
		if (emoteFrom == null) {
			emoteFrom = entity.position();
		} else if (entity.position().distanceToSqr(emoteFrom) > EMOTE_MOVE * EMOTE_MOVE || !entity.isAlive()) {
			emote = null;
			return false;
		}
		EmotePlan plan = EmotePlan.of(model, emote);
		if (plan == null) {
			emote = null;
			return false;
		}
		if (devEmoteAt >= 0) {
			emoteStart = now - devEmoteAt;
		}
		float t = Math.max(0, now - emoteStart);
		// eased in from the movement pose over the first 0.2 s
		float weight = Math.clamp((now - emoteStart) / EMOTE_EASE, 0, 1);
		EmotePlan.At step = plan.at(t);
		SkinnedModel.Clip clip = step.clip();
		float at = step.at();
		if (step.past() > 0 && plan.ends()) {
			// a one-shot: back to her stance, eased out to the movement pose, and the emote's over (Overwatch's camera
			// comes back then too)
			float out = step.past() / EMOTE_EASE;
			if (out >= 1) {
				emote = null;
				return false;
			}
			weight *= 1 - out;
		}
		// else a plan with no loop holds its last pose until she moves
		emoteClip = clip.name();
		emoteAt = at;
		emoteClipLength = clip.duration();
		// over the pose under it: bones the clip doesn't drive keep that pose (Listen Up, Salty and Mic Drop drive 167 to
		// 223 of her 295 bones; from her rest pose instead, their kunai floated a metre beside her, parked where her
		// rest pose keeps them)
		Skeleton.Pose act = new Skeleton.Pose(model.nodeNames.length);
		for (int i = 0; i < model.nodeNames.length; i++) {
			act.t[i].set(pose.t[i]);
			act.r[i].set(pose.r[i]);
			act.s[i].set(pose.s[i]);
		}
		Skeleton.sample(driven(model, clip), at, act);
		Skeleton.blend(pose, act, weight, null);
		return true;
	}

	/** His feet's lowest bone standing (Lúcio's idle: 0.16 m, measured from the clip). */
	private static final float FOOT_HEIGHT = 0.16f;

	/** The lower foot bone's height in the model's space as posed. */
	private static float lowestFoot(SkinnedModel model, Skeleton.Pose pose) {
		float lowest = Float.MAX_VALUE;
		for (String foot : new String[] {"Foot_L", "Foot_R"}) {
			int bone = model.node(foot);
			if (bone >= 0) {
				lowest = Math.min(lowest, Skeleton.world(model, pose, bone).m31());
			}
		}
		return lowest == Float.MAX_VALUE ? FOOT_HEIGHT : lowest;
	}

	/** Seconds since the action last started, or a long time when it never has. */
	public float since(String action, float now) {
		Float at = actionAt.get(action);
		return at == null ? 1000 : now - at;
	}

	/** An action's additive clip, played once from when the action started (nothing when the model lacks it). */
	/**
	 * Lifeweaver's third-person clip for an action (lifeweaver.txt): his release on the move is its own clip, Life Grip
	 * with Thorn Volley out too; the dash by the way he's going (its 2D blend's poses: D 36A2 df 0x08 / 0x07, nearest).
	 */
	private String lifeweaverAction(String action) {
		return switch (action) {
			case "release" -> Math.abs(tickVx) + Math.abs(tickVz) > 0.05 ? "release_moving" : "release";
			case "reload_thorn_after_fire" -> "reload_thorn";
			case "grip" -> thorns ? "grip_thorn" : "grip";
			case "melee_2", "melee_3" -> "melee";
			case "dash" -> "dash" + dashWay();
			default -> action;
		};
	}

	/** The same for his first-person model (lifeweaver_1p.txt): the "_thorn" clip where Thorn Volley has its own. */
	private String lifeweaverViewAction(String action) {
		return switch (action) {
			case "release" -> "bloom_chain";
			case "melee", "melee_2", "melee_3" -> thorns ? "melee_thorn" : "melee";
			case "throw_blossom" -> "throw";
			case "tree_cast", "tree_aim_start" -> thorns ? action + "_thorn" : action;
			default -> action;
		};
	}

	/** Which of the dash's eight poses: the way he moved last tick against his facing ("" ahead). */
	private String dashWay() {
		double yaw = Math.toRadians(lastFacing);
		double forward = -tickVx * Math.sin(yaw) + tickVz * Math.cos(yaw);
		double left = tickVx * Math.cos(yaw) + tickVz * Math.sin(yaw);
		if (forward * forward + left * left < 1.0E-4) {
			return "";
		}
		int octant = Math.floorMod((int) Math.round(Math.atan2(left, forward) / (Math.PI / 4)), 8);
		return new String[] {"", "_forward_left", "_left", "_back_left", "_back", "_back_right", "_right", "_forward_right"}[octant];
	}

	/**
	 * Lifeweaver's layers over his base (third person): with Thorn Volley out its clip for the state over the upper body
	 * (its run, strafe and crouch clips are upper-body overrides: their legs don't move), firing its own stand; charging a
	 * blossom standing, the charging stand; aiming the tree, its aim loop. Then the volley's layer.
	 */
	private void lifeweaverLayers(SkinnedModel model, Skeleton.Pose pose, float now) {
		String over = null;
		boolean standing = base.equals("idle") || base.equals("idle_2");
		if (treeAim) {
			over = "tree_aim";
		} else if (thorns) {
			over = thornFiring && standing ? "idle_firing_thorn" : base + "_thorn";
		} else if (bloomCharging && standing) {
			over = "idle_charging";
		}
		SkinnedModel.Clip clip = over == null ? null : clip(model, over);
		if (clip != null) {
			Skeleton.Pose upper = new Skeleton.Pose(model.nodeNames.length).rest(model);
			Skeleton.sample(clip, time(clip, now, true), upper);
			Skeleton.blend(pose, upper, 1, UpperBody.mask(model));
		}
		if (thornFiring) {
			SkinnedModel.Clip firing = clip(model, "add_firing_thorn");
			if (firing != null) {
				Skeleton.addAdditive(firing, time(firing, now, true), pose, 1);
			}
		}
		additive(model, pose, "add_fire_thorn", "fire_thorn", now);
		landing(model, pose, now, Math.abs(tickVx) + Math.abs(tickVz) > 0.05 ? "add_land_forward" : "add_land");
		SkinnedModel.Clip dashEnd = clip(model, "add_dash_end");
		float sinceDash = since("dash", now);
		if (dashEnd != null && sinceDash >= 0.25f && sinceDash - 0.25f < dashEnd.duration()) {
			Skeleton.addAdditive(dashEnd, sinceDash - 0.25f, pose, 1);
		}
	}

	/** His landing: an additive layer from the moment he lands (lifeweaver.txt's 4-* land clips), over whatever he does. */
	private void landing(SkinnedModel model, Skeleton.Pose pose, float now, String name) {
		SkinnedModel.Clip land = clip(model, name);
		float t = now - landedAt;
		if (land != null && t >= 0 && t < land.duration()) {
			Skeleton.addAdditive(land, t, pose, 1);
		}
	}

	/**
	 * Lifeweaver's first person over its base: the flower blooming in his hand as the blossom charges (his hand's state
	 * machine 0x16E: the bloom's time is the charge, df 0xC9), held full at the end; the volley's layer.
	 */
	private void lifeweaverView(SkinnedModel model, Skeleton.Pose pose, float now) {
		if (bloomCharging) {
			SkinnedModel.Clip bloom = clip(model, "bloom");
			if (bloom != null) {
				Skeleton.sample(bloom, bloom.duration() * Math.clamp(bloomCharge, 0, 1), pose);
			}
		}
		// the volley's layers (his 1P fire state 0x00A is additive: its start, its loop while held, its end), the dash's
		// (one of four poses: their directions aren't in the data, L: the first), his landing
		if (thornFiring) {
			SkinnedModel.Clip firing = clip(model, "add_firing_thorn");
			if (firing != null) {
				Skeleton.addAdditive(firing, time(firing, now, true), pose, 1);
			}
		}
		additive(model, pose, "add_fire_thorn", "fire_thorn", now);
		additive(model, pose, thorns ? "add_dash_thorn" : "add_dash", "dash", now);
		landing(model, pose, now, thorns ? "add_land_thorn" : "add_land");
	}

	private void additive(SkinnedModel model, Skeleton.Pose pose, String clipName, String action, float now) {
		SkinnedModel.Clip clip = clip(model, clipName);
		float t = since(action, now);
		if (clip != null && t >= 0 && t <= clip.duration()) {
			Skeleton.addAdditive(clip, t, pose, 1);
		}
	}

	/**
	 * Jetpack Cat's additive layers over her flight (jetpack_cat-map.md): the hover's bob (on the clock), Frenetic
	 * Flight's lean while boosting, and her actions, which are all layers in the third person (the reload, Purr, the
	 * quick melee, Catnapper's aim: enter, then its 2-frame hold until she dives or lets go).
	 */
	private void flierLayers(SkinnedModel model, LivingEntity entity, Skeleton.Pose pose, float now) {
		SkinnedModel.Clip bob = clip(model, "add_bob");
		if (bob != null && (base.equals("idle") || base.startsWith("fly"))) {
			Skeleton.addAdditive(bob, time(bob, now, true), pose, 1);
		}
		SkinnedModel.Clip boost = clip(model, "add_boost");
		if (boost != null && boostWeight > 0) {
			Skeleton.addAdditive(boost, time(boost, now, true), pose, boostWeight);
		}
		additive(model, pose, "add_reload", "reload", now);
		additive(model, pose, "add_purr", "purr", now);
		for (String melee : new String[] {"melee", "melee_2", "melee_3"}) {
			additive(model, pose, "add_melee", melee, now);
		}
		if (now - aimingSince < AIM_HOLD_LIMIT) {
			SkinnedModel.Clip enter = clip(model, "add_ult_aim_start");
			float t = now - aimingSince;
			if (enter != null && t <= enter.duration()) {
				Skeleton.addAdditive(enter, t, pose, 1);
			} else {
				SkinnedModel.Clip hold = clip(model, "add_ult_aim");
				if (hold != null) {
					Skeleton.addAdditive(hold, time(hold, now, true), pose, 1);
				}
			}
		} else {
			additive(model, pose, "add_ult_aim_end", "ult_aim_end", now);
		}
	}

	/**
	 * Mercy's additive layers (mercy-map.md, alias 0x00A): the staff's beam (its start, its loop while it's on, its end;
	 * which of its clips are the heal's and which the boost's isn't read, M: the same here), a blaster shot, Flash Heal
	 * (on foot or flying).
	 */
	private void mercyLayers(SkinnedModel model, Skeleton.Pose pose, float now) {
		if (beamMode != 0) {
			SkinnedModel.Clip start = clip(model, "add_beam_start");
			float t = now - beamOnSince;
			if (start != null && t < start.duration()) {
				Skeleton.addAdditive(start, t, pose, 1);
			} else {
				SkinnedModel.Clip loop = clip(model, "add_beam");
				if (loop != null) {
					Skeleton.addAdditive(loop, time(loop, now, true), pose, 1);
				}
			}
		} else {
			SkinnedModel.Clip end = clip(model, "add_beam_end");
			float t = now - beamOffAt;
			if (end != null && t >= 0 && t < end.duration()) {
				Skeleton.addAdditive(end, t, pose, 1);
			}
		}
		additive(model, pose, "add_fire_blaster", "fire_blaster", now);
		additive(model, pose, "add_flash_heal", "flash_heal", now);
		additive(model, pose, clip(model, "add_flash_heal_flying") != null ? "add_flash_heal_flying" : "add_flash_heal",
			"flash_heal_flying", now);
	}

	/** Catnapper's aim loop gives up after this long with no word of a dive or a cancel (s, ours). */
	private static final float AIM_HOLD_LIMIT = 15;
	/** Frenetic Flight's lean, eased in and out (0-1): her boost, seen as speed above her flight's 5.5 m/s. */
	private float boostWeight;
	/** Jetpack Cat's first-person rise (+1) or sink (-1) layer, eased; and when the view was last posed. */
	private float verticalWeight;
	private float lastViewNow = -1;

	/** Lúcio's idle fidgets (alias 80AF80FD: a Random of 2CAF and 2A56, additive, on the clock), now and then while still. */
	private void fidget(SkinnedModel model, Skeleton.Pose pose, float now) {
		if (!base.equals("idle") || actions.playing() != null || !model.clips.containsKey("add_fidget_1")) {
			fidgetAt = now;
			fidget = null;
			return;
		}
		if (fidget == null && now - fidgetAt > FIDGET_EVERY) {
			fidget = java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ? "add_fidget_1" : "add_fidget_2";
			fidgetAt = now;
		}
		SkinnedModel.Clip clip = fidget == null ? null : clip(model, fidget);
		if (clip == null) {
			return;
		}
		if (now - fidgetAt > clip.duration()) {
			fidget = null;
			fidgetAt = now;
			return;
		}
		Skeleton.addAdditive(clip, now - fidgetAt, pose, 1);
	}

	/** The third-person pose last drawn (Lúcio's gun hangs on its bones: HeroWeapon), or null before any. */
	public Skeleton.@Nullable Pose lastPose() {
		return emote == null ? lastPose : null;
	}

	/** The first-person pose last drawn, or null before any. */
	public Skeleton.@Nullable Pose lastViewPose() {
		return lastViewPose;
	}

	/** A climb sound: in the head for the local player (her own sound, #17), at the spot for anyone else watching. */
	private static void climbSound(LivingEntity entity, Vec3 at, net.minecraft.sounds.SoundEvent sound) {
		if (entity == net.minecraft.client.Minecraft.getInstance().player) {
			dev.overcraft.client.ClientHero.playOwn(sound.location(), 1, 1, entity.getRandom().nextLong());
		} else {
			entity.level().playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, 1, 1, false);
		}
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
	/**
	 * Her hands' weapon bones in the model's space, as last posed by poseFirstPerson: where she holds her kunai (on the
	 * right of the view) and her ofuda (on the left). Minecraft mode puts the held items there (HeroModelRenderer).
	 */
	public final Matrix4f kunaiHand = new Matrix4f();
	public final Matrix4f ofudaHand = new Matrix4f();
	/**
	 * Each weapon bone's place in its hand (from the wrist), as last seen with no action playing: her quick melee parks
	 * the kunai's bone away from the hand, and a held item went with it (vanished mid-swing).
	 */
	private final Matrix4f kunaiGrip = new Matrix4f();
	private final Matrix4f ofudaGrip = new Matrix4f();

	/** Her right-of-view arm turned at the shoulder by these degrees about SWING's axis (her model's space). */
	private static void swingArm(SkinnedModel model, Skeleton.Pose pose, float degrees) {
		int arm = UpperBody.arm(model);
		if (arm < 0) {
			return;
		}
		int parent = model.parent[arm];
		Quaternionf up = parent >= 0
			? Skeleton.world(model, pose, parent).getUnnormalizedRotation(new Quaternionf()).normalize() : new Quaternionf();
		Quaternionf turn = new Quaternionf().fromAxisAngleDeg(new Vector3f(SWING[0], SWING[1], SWING[2]).normalize(), degrees);
		// the turn in her model's space, as seen from the arm's parent: r' = P^-1 Q P r
		pose.r[arm].premul(new Quaternionf(up).invert().mul(turn).mul(up));
	}

	/** A hand's weapon bone as posed: its wrist's, with the bone's place in the hand from when no action played. */
	private void hand(SkinnedModel model, Skeleton.Pose pose, boolean kunai, Matrix4f grip, Matrix4f out) {
		int wrist = UpperBody.wrist(model, kunai);
		int weapon = UpperBody.weapon(model, kunai);
		if (wrist < 0 || weapon < 0) {
			return;
		}
		Matrix4f wristWorld = Skeleton.world(model, pose, wrist);
		if (actionsFirstPerson.playing() == null) {
			grip.set(wristWorld).invert().mul(Skeleton.world(model, pose, weapon));
		}
		out.set(wristWorld).mul(grip);
	}

	/**
	 * Minecraft mode's swing with her own arms (attacking, mining, placing: 0 to 1 through it, Minecraft's own
	 * progress), set before each poseFirstPerson. Her clips have no swing of the hand on the right of the view: her quick
	 * melee and her kunai throws are both the other hand's.
	 */
	public float swing;
	/**
	 * The swing: her right-of-view arm turned about this axis of her model by this many degrees at its height, on
	 * Minecraft's swing curve (sin of the square root: quick out, slower back): a chop in across her toward the crosshair.
	 * DevServer's "swing" sets it, and can hold the swing at one point (its fifth number, 0 for none) to look at it.
	 * Ours, by eye (tried about each axis both ways: about X the arm went up out of view or down, about Z it rose).
	 */
	public static final float[] SWING = {-0.4f, 1, 0, 30, 0};

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
		String viewBase = viewBase(model, now);
		SkinnedModel.Clip baseClip = clip(model, viewBase);
		if (baseClip != null) {
			float t;
			if (isClimb(viewBase)) {
				t = time(baseClip, basePhase, loops(base));
			} else if (viewBase.startsWith("skate")) {
				// his strides: on the clock times his speed, as the third person's
				t = time(baseClip, basePhase, true);
			} else if (viewBase.equals("jump_start")) {
				t = Math.min(now - airborneSince, baseClip.duration());
			} else if (viewBase.equals("jump")) {
				// the rise, held at its end until he lands
				SkinnedModel.Clip start = clip(model, "jump_start");
				t = Math.clamp(now - airborneSince - (start == null ? 0 : start.duration()), 0, baseClip.duration());
			} else if (viewBase.equals("idle")) {
				t = 0;
			} else {
				t = time(baseClip, now, true);
			}
			Skeleton.sample(baseClip, t, pose);
		}
		SkinnedModel.Clip breathe = clip(model, "add_breathe");
		if (breathe != null) {
			Skeleton.addAdditive(breathe, time(breathe, now, true), pose, 1);
		}
		// Lúcio's additive layers: the volley (one of its clips at random), Soundwave, Amp It Up's
		String fire = fireVariant > 1 ? "add_fire_" + fireVariant : "add_fire";
		if (clip(model, fire) == null) {
			// Jetpack Cat's: one per paw, swapping each volley
			fire = fireSide ? "add_fire_2" : "add_fire";
		}
		additive(model, pose, fire, "fire", now);
		additive(model, pose, "add_soundwave", "soundwave", now);
		additive(model, pose, "add_amp", "amp_it_up", now);
		if (lifeweaver) {
			lifeweaverView(model, pose, now);
		}
		SkinnedModel.Clip idleLayer = clip(model, blaster && clip(model, "add_idle_blaster") != null ? "add_idle_blaster"
			: thorns && clip(model, "add_idle_thorn") != null ? "add_idle_thorn" : "add_idle");
		if (idleLayer != null) {
			// her paws' hover sway (195B6, on the clock); Mercy's idle sway, the blaster's with it out
			Skeleton.addAdditive(idleLayer, time(idleLayer, now, true), pose, 1);
		}
		if (mercy) {
			mercyLayers(model, pose, now);
		}
		SkinnedModel.Clip upLayer = clip(model, "add_up");
		SkinnedModel.Clip downLayer = clip(model, "add_down");
		if (upLayer != null && downLayer != null && entity instanceof net.minecraft.client.player.LocalPlayer local) {
			// her paws as she rises or sinks (set 3866: a Blend1d of 194EF at +1, 194F0 at -1 on her jetpack's vertical
			// input, jump +1 and crouch -1, H), eased over 0.1 s (ours: the blend's own easing isn't read)
			net.minecraft.world.entity.player.Input keys = local.input.keyPresses;
			float want = (keys.jump() ? 1 : 0) - (keys.shift() ? 1 : 0);
			float step = lastViewNow < 0 ? 0 : Math.clamp(now - lastViewNow, 0, 0.25f) / 0.1f;
			verticalWeight += Math.clamp(want - verticalWeight, -step, step);
			if (verticalWeight > 1.0E-3f) {
				Skeleton.addAdditive(upLayer, time(upLayer, now, true), pose, verticalWeight);
			} else if (verticalWeight < -1.0E-3f) {
				Skeleton.addAdditive(downLayer, time(downLayer, now, true), pose, -verticalWeight);
			}
		}
		lastViewNow = now;
		SkinnedModel.Clip setOff = base.startsWith("skate") && !base.startsWith("skate_stop") ? clip(model, "add_skate_start") : null;
		if (setOff != null && now - movingSince <= setOff.duration()) {
			Skeleton.addAdditive(setOff, now - movingSince, pose, 1);
		}
		// no walk bob: the add_walk layer (4-4_A525) moves nearly every bone by up to 28 cm and 28 degrees, which played as
		// raw deltas lifted the arms about 85 px for as long as she moved; Overwatch's arms only drift a little with the
		// movement (HeroModelRenderer.drift, the owner's playtest: #14)
		actionsFirstPerson.apply(model, pose, now, null);
		int tip = UpperBody.fingertip(model);
		if (tip >= 0) {
			Skeleton.world(model, pose, tip).getTranslation(fingertip);
		}
		if (swing > 0) {
			swingArm(model, pose, (float) Math.sin(Math.sqrt(swing) * Math.PI) * SWING[3]);
		}
		hand(model, pose, true, kunaiGrip, kunaiHand);
		hand(model, pose, false, ofudaGrip, ofudaHand);
		lastViewPose = pose;
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

	/**
	 * The first-person base clip for what the third person plays: the climb, Lúcio's skating stride (its start first),
	 * his air (4 ways in first person), his wall ride; else the held idle.
	 */
	private String viewBase(SkinnedModel model, float now) {
		String want = "idle";
		if (lifeweaver) {
			// his view model (lifeweaver_1p.txt): idle, walk, the jump's rise and fall; firing, the volley's loop; the
			// tree's aim loop; each with Thorn Volley's own where it has one
			if (treeAim) {
				want = "tree_aim";
			} else if (base.startsWith("jump")) {
				want = "jump";
			} else if (base.startsWith("fall")) {
				want = "fall";
			} else if (base.startsWith("run") || base.startsWith("strafe") || base.startsWith("crouch_walk")) {
				want = "walk";
			}
			if (thorns && model.clips.containsKey(want + "_thorn")) {
				want = want + "_thorn";
			}
			return model.clips.containsKey(want) ? want : thorns && model.clips.containsKey("idle_thorn") ? "idle_thorn" : "idle";
		}
		if (mercy) {
			// her view model (mercy_1p.txt): the third person's state, in the view's few clips
			if (base.startsWith("ga_start")) {
				want = "ga_start";
			} else if (base.startsWith("ga_fly")) {
				want = "ga_fly";
			} else if (base.startsWith("fly")) {
				want = "fly";
			} else if (base.startsWith("jump")) {
				want = "jump";
			} else if (base.startsWith("fall")) {
				want = "fall";
			} else if (base.startsWith("crouch_walk")) {
				want = "crouch_walk";
			} else if (base.startsWith("crouch")) {
				want = "crouch_idle";
			} else if (base.startsWith("run") || base.startsWith("strafe")) {
				want = "walk";
			}
			if (blaster && model.clips.containsKey(want + "_blaster")) {
				want = want + "_blaster";
			}
			return model.clips.containsKey(want) ? want : blaster && model.clips.containsKey("idle_blaster") ? "idle_blaster" : "idle";
		}
		if (isClimb(base)) {
			want = base;
		} else if (base.startsWith("skate") && !base.startsWith("skate_stop")) {
			want = "skate" + skateDirection;
		} else if (base.startsWith("jump")) {
			SkinnedModel.Clip start = clip(model, "jump_start");
			want = start != null && now - airborneSince < start.duration() ? "jump_start" : "jump";
		} else if (base.startsWith("wallride")) {
			want = base;
		}
		return model.clips.containsKey(want) ? want : "idle";
	}

	/**
	 * The hero select screen's pose, as Overwatch's (the hero's showcase entity, #29): its intro once, then its hold
	 * looping (breathing; she looks off past the camera). Null when the model lacks the hold. The weapon hangs on this
	 * pose (HeroWeapon.placeShowcase).
	 */
	public static Skeleton.@Nullable Pose showcasePose(SkinnedModel model, float sinceOpened) {
		SkinnedModel.Clip intro = model.clips.get("select_intro");
		SkinnedModel.Clip hold = model.clips.get("select_hold");
		if (hold == null || hold.duration() <= 0) {
			return null;
		}
		Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
		if (model.clips.containsKey("select_fidget_1")) {
			showcaseRun(model, sinceOpened, pose);
			return pose;
		}
		if (showcaseIntro(model, sinceOpened)) {
			Skeleton.sample(intro, sinceOpened, pose);
		} else {
			float t = sinceOpened - (intro == null ? 0 : intro.duration());
			Skeleton.sample(hold, t % hold.duration(), pose);
		}
		return pose;
	}

	/** The showcase is in its intro (before the hold loops). */
	public static boolean showcaseIntro(SkinnedModel model, float sinceOpened) {
		SkinnedModel.Clip intro = model.clips.get("select_intro");
		return intro != null && !model.clips.containsKey("select_fidget_1") && sinceOpened < intro.duration();
	}

	/**
	 * Jetpack Cat's showcase (entity 6C20, set 3FBB: E:\Overwatch Archive\research\jetpack_cat\gaps.md 5, H): a scripted
	 * run of seven clips, each to its end and cut to the next (blend 0), then a weighted pick each time one ends. The
	 * pick's empty slot (weight 1) is read as her hold (M).
	 */
	private static final String[] SHOWCASE_RUN = {"select_intro", "select_hold", "select_fidget_1", "select_fidget_2",
		"select_fidget_3", "select_hold", "select_fidget_4"};
	private static final String[] SHOWCASE_PICKS = {"select_fidget_4", "select_fidget_3", "select_hold", "select_fidget_2"};
	private static final int[] SHOWCASE_WEIGHTS = {5, 3 + 1, 3 + 1, 1};

	/** Samples her showcase at that time (the random picks seeded by their place in line: the same for every frame). */
	private static void showcaseRun(SkinnedModel model, float t, Skeleton.Pose pose) {
		int total = java.util.Arrays.stream(SHOWCASE_WEIGHTS).sum();
		for (int i = 0; i < 10_000; i++) {
			String name;
			if (i < SHOWCASE_RUN.length) {
				name = SHOWCASE_RUN[i];
			} else {
				int roll = new java.util.Random(0x6C20L * 31 + i).nextInt(total);
				int k = 0;
				while (roll >= SHOWCASE_WEIGHTS[k]) {
					roll -= SHOWCASE_WEIGHTS[k++];
				}
				name = SHOWCASE_PICKS[k];
			}
			SkinnedModel.Clip clip = model.clips.get(name);
			if (clip == null || clip.duration() <= 0) {
				continue;
			}
			if (t < clip.duration()) {
				Skeleton.sample(clip, t, pose);
				return;
			}
			t -= clip.duration();
		}
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
		// Lúcio's jump clips raise his pelvis 0.56 m through the takeoff and the rise (measured: the feet leave the ground
		// in the clip), a lift Minecraft gives him already: his pelvis stays at the takeoff's first height, so the legs tuck
		// under him instead of the whole of him floating above where he is
		int pelvis = base.startsWith("jump_") ? model.node("Pelvis") : -1;
		SkinnedModel.Clip takeoff = pelvis >= 0 ? clip(model, "jump_forward_start") : null;
		if (takeoff != null) {
			Skeleton.Pose first = new Skeleton.Pose(model.nodeNames.length).rest(model);
			Skeleton.sample(takeoff, 0, first);
			pose.t[pelvis].y = first.t[pelvis].y;
		}
		float climbNow = isClimb(base) ? 1 : 0;
		float climbBefore = previous != null && isClimb(previous) ? 1 : 0;
		climbShift = CLIMB_SHIFT * (climbBefore + (climbNow - climbBefore) * fade);
		// Overwatch's breathing layer under the stand (standing or crouched)
		SkinnedModel.Clip breathe = clip(model, base.equals("crouch_idle") ? "add_breathe_crouch" : "add_breathe");
		if (breathe != null && (base.equals("idle") || base.equals("crouch_idle"))) {
			Skeleton.addAdditive(breathe, time(breathe, now, true), pose, 1);
		}
		if (poseEmote(model, entity, now, pose)) {
			int root = UpperBody.mover(model);
			if (root >= 0) {
				// her emote's root motion is kept off (she stays on her spot); what was taken off moves her props
				// instead, which Overwatch places from the root that doesn't move (Friendly Kitty: she shifts 0.6 m as
				// she sits, and her cat landed behind her)
				Matrix4f moved = Skeleton.world(model, pose, root);
				pose.t[root].set(model.restT[root]);
				emoteRootShift.set(moved.getTranslation(new Vector3f()).sub(Skeleton.world(model, pose, root).getTranslation(new Vector3f())));
			}
			// her props hang on her bones as posed (EmoteProps)
			if (emotePose == null || emotePose.t.length != model.nodeNames.length) {
				emotePose = new Skeleton.Pose(model.nodeNames.length);
			}
			for (int i = 0; i < model.nodeNames.length; i++) {
				emotePose.t[i].set(pose.t[i]);
				emotePose.r[i].set(pose.r[i]);
				emotePose.s[i].set(pose.s[i]);
			}
			// the coat hangs and folds on the floor in an emote too (no emote clip moves the cloth bones)
			stepCloth(model, entity, pose, now);
			return Skeleton.skin(model, pose, -1, null);
		}
		if (lifeweaver) {
			lifeweaverLayers(model, pose, now);
		}
		actions.apply(model, pose, now, wholeBody ? null : UpperBody.mask(model));
		if (wholeBody && actions.playing() != null) {
			// Sound Barrier's clips carry its leap (his feet rise 1.63 m): the server leaps him that high, so the clip's own
			// lift comes off the pose, as his jump's does, and his feet stay where he is
			int hips = model.node("Pelvis");
			float lift = lowestFoot(model, pose) - FOOT_HEIGHT;
			if (hips >= 0 && lift > 0) {
				pose.t[hips].y -= lift;
			}
		}
		// Lúcio's additive layers (lucio-map.md): the volley at the root (one clip per volley), Soundwave, Crossfade's,
		// the wall ride's, and his idle fidgets now and then
		boolean flier = model.clips.containsKey("add_bob");
		additive(model, pose, flier && fireSide ? "add_fire_2" : "add_fire", "fire", now);
		if (flier) {
			flierLayers(model, entity, pose, now);
		}
		additive(model, pose, "add_soundwave", "soundwave", now);
		additive(model, pose, "add_crossfade_speed", "crossfade_speed", now);
		additive(model, pose, "add_crossfade_heal", "crossfade_heal", now);
		SkinnedModel.Clip wallLayer = base.startsWith("wallride") ? clip(model, "add_wallride") : null;
		if (wallLayer != null) {
			Skeleton.addAdditive(wallLayer, time(wallLayer, now, true), pose, 1);
		}
		fidget(model, pose, now);
		if (mercy) {
			mercyLayers(model, pose, now);
		}
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
		// (Kiriko's sheet: 63 frames. Lúcio's 265E has 74 laid out otherwise, not read yet: his head turns the whole pitch)
		if (aim != null && Math.round(aim.duration() * AIM_FPS) == 62) {
			float frame = 31 + AIM_SIGN * 10 * Math.clamp(headPitch / AIM_PITCH, -1, 1);
			Skeleton.addAdditive(aim, frame / AIM_FPS, pose, 1, UpperBody.aimMask(model));
			// the sheet already tilts the spine, neck and head by about this share of the pitch: the head turns the rest
			lookPitch = headPitch * (1 - AIM_SHARE);
		}

		stepCloth(model, entity, pose, now);
		lastPose = pose;

		Quaternionf look = asleep ? new Quaternionf()
			: new Quaternionf().rotateY(-netHeadYaw * Mth.DEG_TO_RAD).rotateX(lookPitch * Mth.DEG_TO_RAD);
		return Skeleton.skin(model, pose, UpperBody.head(model), look);
	}

	/** The cloth the clips leave rigid (coat tails): simulated, kept out of the legs and above the floor. */
	private void stepCloth(SkinnedModel model, LivingEntity entity, Skeleton.Pose pose, float now) {
		if (clothFor != model) {
			clothFor = model;
			cloth = ClothSim.of(model);
		}
		if (cloth != null) {
			// her velocity, turned into the model's frame (the renderer turns the model by -facing)
			Vector3f velocity = new Vector3f((float) tickVx, (float) tickVy, (float) tickVz).mul(20)
				.rotateY((float) Math.toRadians(facing(entity, 1)));
			cloth.step(model, pose, now, velocity);
		}
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
		dev.overcraft.client.ClientMercy.State mercyState = dev.overcraft.client.ClientMercy.state(entity);
		mercy = mercyState != null;
		blaster = mercy && mercyState.has(dev.overcraft.mercy.Mercy.FLAG_BLASTER);
		dev.overcraft.client.ClientLifeweaver.State lw = dev.overcraft.client.ClientLifeweaver.state(entity);
		lifeweaver = lw != null;
		thorns = lifeweaver && lw.has(dev.overcraft.lifeweaver.Lifeweaver.FLAG_THORNS);
		bloomCharging = lifeweaver && lw.has(dev.overcraft.lifeweaver.Lifeweaver.FLAG_CHARGING) && !thorns;
		bloomCharge = lifeweaver ? lw.charge() : 0;
		thornFiring = thorns && lw.has(dev.overcraft.lifeweaver.Lifeweaver.FLAG_FIRING);
		treeAim = lifeweaver && lw.has(dev.overcraft.lifeweaver.Lifeweaver.FLAG_TREE_AIM);
		int beamNow = mercy ? mercyState.beamMode() : 0;
		if (beamNow != 0 && beamMode == 0) {
			beamOnSince = now;
		} else if (beamNow == 0 && beamMode != 0) {
			beamOffAt = now;
		}
		beamMode = beamNow;
		boolean ga = mercy && mercyState.has(dev.overcraft.mercy.Mercy.FLAG_GA);
		if (ga && !wasGa) {
			gaSince = now;
		}
		wasGa = ga;
		// Angelic Descent, seen from her motion (anyone's view of her): falling at its 2 m/s, not on the ground
		descentTicks = mercy && !entity.onGround() && vy < -0.07 && vy > -0.125 ? descentTicks + 1 : 0;
		tickVx = vx;
		tickVy = vy;
		tickVz = vz;
		boolean onGround = entity.onGround();
		// Lúcio's model: skating, his stop, his 4-way jump and the wall ride in place of the run, jump and fall
		boolean skates = model.clips.containsKey("skate_stop_forward");
		lastFacing = facing(entity, 1);
		double yawNow = Math.toRadians(lastFacing);
		if (!onGround) {
			fallSpeed = Math.max(fallSpeed, -vy);
			if (wasOnGround) {
				airborneSince = now;
				// his jump goes the way he was going (standing still: forward)
				jumpDirection = Math.sqrt(vx * vx + vz * vz) > 0.02
					? direction4(-vx * Math.sin(yawNow) + vz * Math.cos(yawNow), vx * Math.cos(yawNow) + vz * Math.sin(yawNow)) : "_forward";
			}
		} else if (!wasOnGround) {
			if (fallSpeed >= HARD_LANDING) {
				landedAt = now;
			}
			fallSpeed = 0;
		}
		wallSide = OwMovement.wallRiding(entity);
		wasOnGround = onGround;
		// climbing: the local player's own climb state; others: going up steadily at about the climb speed
		boolean climbing = OwMovement.isClimbing(entity);
		// (only a model that can climb: Mercy rising in Valkyrie goes up at about the climb speed, and played Kiriko's
		// climb sounds: found playing)
		if (!climbing && !onGround && vy > 0.3 && vy < 0.5 && model.clips.containsKey("climb")) {
			climbTicks++;
		} else {
			climbTicks = 0;
		}
		climbing |= climbTicks > 3;
		// out of wall while still going up: momentum carries her over the top, with the hop over the edge
		if (wasClimbing && !climbing && vy > 0 && !entity.horizontalCollision) {
			climbTopAt = now;
			climbSound(entity, entity.position(), OcSounds.KIRIKO_CLIMB_TOP);
		}
		if (climbing && !wasClimbing) {
			climbSound(entity, entity.position().add(0, 1, 0), OcSounds.KIRIKO_CLIMB_START);
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
			climbSound(entity, hands, OcSounds.KIRIKO_CLIMB_STEP);
		}
		SkinnedModel.Clip top = clip(model, "climb_top");
		boolean overTheTop = top != null && now - climbTopAt < top.duration();

		double yaw = Math.toRadians(facing(entity, 1));
		double forward = -vx * Math.sin(yaw) + vz * Math.cos(yaw);
		double left = vx * Math.cos(yaw) + vz * Math.sin(yaw);
		double speed = Math.sqrt(vx * vx + vz * vz);
		boolean moving = speed > 0.02;
		boolean crouching = entity.getPose() == Pose.CROUCHING;
		boolean skating = moving && onGround && !crouching;
		if (skating && !wasSkating) {
			movingSince = now;
		} else if (!skating && wasSkating && onGround && !crouching) {
			stoppedAt = now;
		}
		if (skating) {
			skateDirection = direction4(forward, left);
		}
		wasSkating = skating;

		String next;
		if (entity.isDeadOrDying()) {
			next = "death";
		} else if (entity.isPassenger()) {
			// riding (a boat, a horse): Overwatch has no sitting clip; crouched is the nearest
			next = "crouch_idle";
		} else if (entity.isFallFlying() || entity.isVisuallySwimming()) {
			// pitched flat by the renderer; legs straightened below
			next = "idle";
		} else if (mercy && mercyState != null && mercyState.has(dev.overcraft.mercy.Mercy.FLAG_GA)) {
			// Guardian Angel (alias 0x06D): its start (cat 0x82), then the flight's 2D blend by her motion (centre, X+, X-,
			// Y-: ours from the clip names); a launch is an action clip (ga_launch)
			SkinnedModel.Clip start = clip(model, "ga_start");
			if (start != null && now - gaSince < start.duration()) {
				next = "ga_start";
			} else {
				next = switch (direction4(forward, left)) {
					case "_left" -> "ga_fly_left";
					case "_right" -> "ga_fly_right";
					case "_back" -> "ga_fly_back";
					default -> "ga_fly";
				};
			}
		} else if (mercy && (mercyState != null && mercyState.has(dev.overcraft.mercy.Mercy.FLAG_VALKYRIE)
			|| descentTicks >= Math.round(dev.overcraft.mercy.MercyStats.DESCENT_POSE * 20))) {
			// Valkyrie's flight and Angelic Descent (after 0.2 s of it): the 9-point 2D blend (alias 0x02F), its centre
			// still, its 8 outer points by her motion against her facing (Y+ forward: M)
			boolean flyingNow = Math.sqrt(vx * vx + vz * vz) * 20 > 0.8;
			String d = direction(forward, left);
			next = flyingNow ? "fly" + (d.isEmpty() ? "_forward" : d) : "fly";
		} else if (model.clips.containsKey("add_bob")) {
			// Jetpack Cat always flies: the 2D blend's 8 outer points by her motion against her facing, the hover idle
			// when she's still (its centre); on the ground too (she hovers 0.55 m over it)
			double speed3 = Math.sqrt(vx * vx + vy * vy + vz * vz) * 20;
			double yawF = Math.toRadians(facing(entity, 1));
			double fwdF = -vx * Math.sin(yawF) + vz * Math.cos(yawF);
			double leftF = vx * Math.cos(yawF) + vz * Math.sin(yawF);
			boolean flying = Math.sqrt(vx * vx + vz * vz) * 20 > 0.8;
			String d = direction(fwdF, leftF);
			next = flying ? "fly" + (d.isEmpty() ? "_forward" : d) : "idle";
			// her boost's lean follows her speed past the flight's own (ours: the data weights it by its dataflow)
			float want = (float) Math.clamp((speed3 - 6.5) / 4, 0, 1);
			boostWeight += (want - boostWeight) * 0.3f;
		} else if (climbing) {
			next = "climb";
		} else if (overTheTop) {
			next = "climb_top";
		} else if (skates && wallSide != 0) {
			next = wallSide > 0 ? "wallride_right" : "wallride_left";
		} else if (!onGround && entity.isInWater()) {
			// treading water: Overwatch has no swimming (and hero mode no sprint-swim), so the air pose
			next = "fall";
		} else if (!onGround && skates) {
			// his jump (alias 0x006): the takeoff, then the rise, held till he lands (lucio-map.md read these as skating)
			SkinnedModel.Clip start = clip(model, "jump" + jumpDirection + "_start");
			next = start != null && now - airborneSince < start.duration() ? "jump" + jumpDirection + "_start" : "jump" + jumpDirection;
		} else if (!onGround) {
			next = vy > 0 ? "jump" : "fall";
		} else if (skates && !moving && !crouching && clip(model, "skate_stop" + skateDirection) != null
			&& now - stoppedAt < clip(model, "skate_stop" + skateDirection).duration()) {
			// stopping: the brace from skating to standing (alias 0x01B)
			next = "skate_stop" + skateDirection;
		} else if (now - landedAt < 0.2f && !lifeweaver) {
			// (Lifeweaver's landing is an additive layer: lifeweaverLayers)
			next = "land";
		} else if (crouching) {
			next = moving ? "crouch_walk" + direction(forward, left) : "crouch_idle";
		} else if (moving && skates) {
			// skating, 8 ways (the research map called these clips airborne: their feet are on the ground)
			next = "skate" + direction(forward, left);
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
		if (blaster && model.clips.containsKey(next + "_blaster")) {
			// Mercy's blaster out: every state has its own clip (her data's category 0x7D on df 0xE4)
			next = next + "_blaster";
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
		if (base.startsWith("skate") && !base.startsWith("skate_stop")) {
			// the strides on the clock times his speed
			rate = (float) Math.clamp(metresPerSecond / SKATE_CLIP_SPEED, 0.2, 3);
		} else if (base.startsWith("run") || base.startsWith("strafe")) {
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

	/** Which of 4 directions (Lúcio's skating and landing slots): "_forward", "_left", "_back", "_right". */
	private static String direction4(double forward, double left) {
		int sector = Math.floorMod((int) Math.round(Math.atan2(left, forward) / (Math.PI / 2)), 4);
		return switch (sector) {
			case 1 -> "_left";
			case 2 -> "_back";
			case 3 -> "_right";
			default -> "_forward";
		};
	}

	private static boolean loops(String clip) {
		return !(clip.startsWith("jump") || clip.equals("land") || clip.equals("climb_top") || clip.equals("death")
			|| clip.startsWith("skate_stop") || clip.startsWith("knockdown"));
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
				// Lúcio has no fall clip: his jump's rise
				case "fall" -> model.clips.containsKey("jump_forward") ? "jump_forward" : "idle";
				// nor a death: Overwatch ragdolls its heroes, and his knock-down's fall is the nearest of his own clips
				case "death" -> model.clips.containsKey("knockdown") ? "knockdown" : null;
				case "climb", "run" -> "idle";
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
		private static final Map<SkinnedModel, Integer> KUNAI_HANDS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> KUNAI_WRISTS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> ARMS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> OFUDA_WRISTS = new HashMap<>();
		private static final Map<SkinnedModel, Integer> OFUDA_HANDS = new HashMap<>();

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

		/** The upper arm on the right of the view (the model's "_L" side). */
		static int arm(SkinnedModel model) {
			return ARMS.computeIfAbsent(model, m -> first(m, "Arm_L"));
		}

		/** The kunai hand's wrist (right of the view: the model's "_L" side), or the ofuda hand's. */
		static int wrist(SkinnedModel model, boolean kunai) {
			return (kunai ? KUNAI_WRISTS : OFUDA_WRISTS).computeIfAbsent(model, m -> first(m, kunai ? "Wrist_L" : "Wrist_R"));
		}

		/** The weapon bone of the kunai hand (right of the view: the model's "_L" side), or of the ofuda hand. */
		static int weapon(SkinnedModel model, boolean kunai) {
			return (kunai ? KUNAI_HANDS : OFUDA_HANDS).computeIfAbsent(model, m -> first(m, kunai ? "Weapon_L" : "Weapon_R"));
		}

		static int head(SkinnedModel model) {
			return HEADS.computeIfAbsent(model, m -> first(m, "Head", "ValveBiped.Bip01_Head"));
		}
	}
}
