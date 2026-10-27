package dev.overcraft.client.comms;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Overwatch's emote camera: while the hero emotes, the view leaves her eyes, swings round to her front, and the mouse
 * turns it freely around her (owner, 2026-10-27). Her aim, and how everyone else sees her, stay where they were; the
 * emote over, the view is back in her eyes looking where she looked before. ClientHero starts and stops it with the
 * emote, MouseHandlerMixin gives it the mouse, CameraMixin places the view.
 */
public final class EmoteCamera {
	/** Seconds the camera takes to pull out and swing round to her front (ours). */
	private static final float SWING = 0.5f;
	/** How far it orbits from her (blocks; ours, nearer than Minecraft's third person, 4: she fills the screen more). */
	private static final float DISTANCE = 2.5f;
	/** Nearer than this to the point it orbits, it's inside her head: she isn't drawn (her held ofuda filled the view). */
	private static final float INSIDE = 0.8f;
	/** It orbits her middle, this share of her eye height up (ours: she fills the middle of the screen). */
	private static final float PIVOT = 0.6f;
	/** Its first look at her: from the front, a little from above (degrees; ours). */
	private static final float START_PITCH = 10;
	/** How far over and under her the mouse may take it (degrees). */
	private static final float MIN_PITCH = -60, MAX_PITCH = 80;
	/** Degrees per mouse unit: Entity.turn's. */
	private static final float TURN = 0.15f;

	private static boolean active;
	private static CameraType before = CameraType.FIRST_PERSON;
	/** Where she looked when it started (the swing starts in her eyes). */
	private static float fromYaw, fromPitch;
	/** The orbit's own look (the mouse turns it; yaw unwrapped so the swing never jumps sides). */
	private static float yaw, pitch;
	private static long startedAt;
	/** Dev only (DevServer's "state"): frames drawn in first person while she emoted. */
	public static int firstPersonFrames;
	/** Dev only (DevServer's "emotef5 off"): F5 may reach first person again, to show the hold below is the fix. */
	public static boolean holdF5 = true;

	private EmoteCamera() {
	}

	/** Her emote started, or goes on: the view leaves first person (any view she was in comes back after). */
	public static void start(Minecraft mc, LocalPlayer player) {
		if (active) {
			return;
		}
		active = true;
		before = mc.options.getCameraType();
		fromYaw = player.getYRot();
		fromPitch = player.getXRot();
		pitch = START_PITCH;
		yaw = fromYaw + 180 + roomiestTurn(mc, player);
		startedAt = Util.getMillis();
		mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
	}

	/**
	 * How far round from her front the orbit starts (degrees): her front where there's room for it, else the nearest
	 * angle with the most (ours: facing a wall, the front view was squeezed against her face).
	 */
	private static float roomiestTurn(Minecraft mc, LocalPlayer player) {
		if (mc.level == null) {
			return 0;
		}
		Vec3 pivot = player.position().add(0, player.getEyeHeight() * PIVOT, 0);
		float best = 0;
		double bestRoom = -1;
		for (float turn : new float[] {0, 30, -30, 60, -60, 90, -90, 120, -120, 150, -150, 180}) {
			// the camera sits back along its look from the point it orbits
			Vec3 back = Vec3.directionFromRotation(START_PITCH, player.getYRot() + 180 + turn).scale(-(DISTANCE + 0.3));
			HitResult hit = mc.level.clip(new ClipContext(pivot, pivot.add(back), ClipContext.Block.VISUAL,
				ClipContext.Fluid.NONE, player));
			double room = hit.getType() == HitResult.Type.MISS ? DISTANCE + 0.3 : hit.getLocation().distanceTo(pivot);
			if (room >= DISTANCE + 0.29) {
				return turn;
			}
			if (room > bestRoom) {
				bestRoom = room;
				best = turn;
			}
		}
		return best;
	}

	/** Her emote ended: back to the view she had. */
	public static void stop(Minecraft mc) {
		if (active) {
			active = false;
			mc.options.setCameraType(before);
		}
	}

	public static boolean active() {
		return active;
	}

	/**
	 * The view Options.setCameraType is asked for (OptionsMixin): while she emotes, never first person, which would
	 * leave the orbit with nobody drawn in it and her arms on screen. F5 cycles back, front, first: here back, front,
	 * back. (Putting it back each client tick was too late: F5 is handled after that tick's start, and the frames before
	 * the next showed first person.)
	 */
	public static CameraType allowed(CameraType type) {
		return active && holdF5 && type.isFirstPerson() ? CameraType.THIRD_PERSON_BACK : type;
	}

	/** The mouse while she emotes (MouseHandlerMixin): it turns the orbit, not her. */
	public static void turn(double dx, double dy) {
		yaw += (float) dx * TURN;
		pitch = Mth.clamp(pitch + (float) dy * TURN, MIN_PITCH, MAX_PITCH);
	}

	/** How far through the swing out (0 to 1). */
	private static float swing() {
		return Math.clamp((Util.getMillis() - startedAt) / (SWING * 1000), 0, 1);
	}

	/** The view's look now: from her eyes, round to the orbit's (smoothstep). */
	public static float viewYaw() {
		return Mth.lerp(Mth.smoothstep(swing()), fromYaw, yaw);
	}

	public static float viewPitch() {
		return Mth.lerp(Mth.smoothstep(swing()), fromPitch, pitch);
	}

	/** How far the view is from the point it orbits: out fast, then swinging round (ease-out cubic). */
	public static float distance() {
		float s = 1 - swing();
		return DISTANCE * (1 - s * s * s);
	}

	/** The view is still inside her (the first moments of the swing): HeroModelRenderer doesn't draw her. */
	public static boolean inside() {
		return active && distance() < INSIDE;
	}

	/** The orbit's point, up from her feet: her eyes at first, her middle once out. */
	public static float pivot(float eyeHeight) {
		return eyeHeight * Mth.lerp(Mth.smoothstep(swing()), 1, PIVOT);
	}
}
