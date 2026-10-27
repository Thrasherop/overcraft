package dev.overcraft.client.comms;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

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

	private EmoteCamera() {
	}

	/** Her emote started, or goes on: the view leaves first person (any view she was in comes back after). */
	public static void start(Minecraft mc, LocalPlayer player) {
		if (active) {
			// F5 cycling to first person mid-emote would leave the orbit with nobody drawn in it: it stays out
			if (mc.options.getCameraType().isFirstPerson()) {
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
			return;
		}
		active = true;
		before = mc.options.getCameraType();
		fromYaw = player.getYRot();
		fromPitch = player.getXRot();
		yaw = fromYaw + 180;
		pitch = START_PITCH;
		startedAt = Util.getMillis();
		mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
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
