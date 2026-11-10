package dev.overcraft.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * Abilities that show the hero from behind while they last (the owner, #55: "When lifeline or catnap is active it
 * should automatically go into 3rd person"; Brigitte's Barrier Shield, #88): the view leaves first person when one
 * starts and comes back to the view the player had when it ends; meanwhile F5 skips first person, as EmoteCamera does.
 * <p>
 * Brigitte's is her barrier's camera node (0EE3 #857 `STU_582E0D5F`, on while the shield is up and she is the one
 * viewing, v1319): 2 m back times her scale (v6054, chased to 4 m in Rally over 0.4 s: #869 / #870), blending in and out
 * over 0.3 s for her own view (m_F5C94645 / m_63FFE8DF), and off for 0.35 s from the start of a Shield Bash (v68007,
 * #1042 -> #909). H for the numbers and conditions, M that the node is the third-person camera (the wiki: Reinhardt's
 * barrier "allows Reinhardt to view 360"; her hero settings have no option to turn it off).
 */
public final class AbilityCamera {
	/**
	 * How far the view rises off her line of sight, in its own up (ours: the data gives no placement for these views).
	 * Minecraft's third person sits on that line, so what she aims at (Catnapper's marker) was always behind her head.
	 */
	public static final float LIFT = 1;
	/** Under this far back (m), the view is in her: her model isn't drawn (mid-blend her ponytail filled the screen). */
	private static final float INSIDE = 1.0f;
	/** How far her line of sight is followed for the point the lifted view turns to (ours). */
	public static final double AIM_REACH = 64;

	private static boolean active;
	/** Blending out: still drawn from behind until the blend reaches 0, then back to the view before. */
	private static boolean leaving;
	private static CameraType before = CameraType.FIRST_PERSON;
	/** How far back the view wants to be (m), or a negative number for Minecraft's own third-person distance. */
	private static float wantDistance = -1;
	/** How far back it is now (m), chased toward wantDistance at chaseSpeed. */
	private static float distance = -1;
	private static float chaseSpeed;
	/** The blend in and out (s; 0: at once) and how far in it is (0 at her eyes, 1 all the way back). */
	private static float blendTime;
	/** How far the view rises off her line of sight (m) for the ability holding it, and how far back it was last frame. */
	private static float lift = LIFT;
	/** How far the view sits to her right (m) for the ability holding it. */
	private static float side;
	private static float shown;
	private static float blend;
	private static long lastNanos;

	private AbilityCamera() {
	}

	/** Each client tick: whether such an ability runs now (Minecraft's own distance, no blend). */
	public static void update(Minecraft mc, boolean wanted) {
		update(mc, wanted, -1, 0, 0, LIFT);
	}

	/**
	 * Each client tick: whether such an ability runs now, how far back its view sits ({@code distance} m, negative for
	 * Minecraft's own), how fast a change in that distance is chased (m/s, 0 at once) and its blend in and out (s).
	 */
	public static void update(Minecraft mc, boolean wanted, float distance, float chase, float blendSeconds, float rise) {
		update(mc, wanted, distance, chase, blendSeconds, rise, 0);
	}

	/** The same, the view {@code right} m to her right. */
	public static void update(Minecraft mc, boolean wanted, float distance, float chase, float blendSeconds, float rise, float right) {
		// dev only ("aimpose"): the body seen from any side, whatever its ability's view
		wanted = wanted && dev.overcraft.client.model.HeroModelRenderer.DEV_AIM == null;
		if (leaving && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
			// the player chose another view (F5) while it blended out: theirs stands
			active = false;
			leaving = false;
		}
		if (wanted) {
			lift = rise;
			side = right;
			wantDistance = distance;
			chaseSpeed = chase;
			blendTime = blendSeconds;
		}
		if (wanted && (!active || leaving)) {
			if (!active) {
				before = mc.options.getCameraType();
				AbilityCamera.distance = distance;
				blend = blendSeconds > 0 ? 0 : 1;
				lastNanos = System.nanoTime();
			}
			active = true;
			leaving = false;
			if (mc.options.getCameraType().isFirstPerson()) {
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
		} else if (!wanted && active && !leaving) {
			if (blendTime > 0 && before.isFirstPerson() && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
				leaving = true;
			} else {
				end(mc);
			}
		}
		if (leaving && blend <= 0) {
			end(mc);
		}
	}

	private static void end(Minecraft mc) {
		active = false;
		leaving = false;
		mc.options.setCameraType(before);
	}

	/** Whether such an ability holds the view now. */
	public static boolean active() {
		return active;
	}

	/**
	 * Each frame the view is placed (CameraMixin): the blend and the chased distance moved on by the time since the last
	 * frame. Returns how far back the view sits now (m), {@code minecraftDistance} standing for Minecraft's own.
	 */
	public static float frameDistance(float minecraftDistance) {
		long now = System.nanoTime();
		// (up to half a second a frame: at a low frame rate the 0.3 s blend took over a second, found tracing it)
		float dt = Math.clamp((now - lastNanos) / 1.0E9f, 0, 0.5f);
		lastNanos = now;
		if (blendTime > 0) {
			blend = Math.clamp(blend + (leaving ? -dt : dt) / blendTime, 0, 1);
		} else {
			blend = 1;
		}
		float target = wantDistance < 0 ? minecraftDistance : wantDistance;
		if (distance < 0 || chaseSpeed <= 0) {
			distance = target;
		} else {
			float step = chaseSpeed * dt;
			distance = Math.abs(target - distance) <= step ? target : distance + Math.signum(target - distance) * step;
		}
		shown = distance * eased();
		return shown;
	}

	/** How far to her right the view sits now (m). */
	public static float side() {
		return side * eased();
	}

	/** How far the view rises off her line of sight now (m). */
	public static float lift() {
		return lift * eased();
	}

	/** Whether the view is inside her now (mid-blend): her model isn't drawn then (HeroModelRenderer). */
	public static boolean inside() {
		return active && shown < INSIDE;
	}

	/** The blend eased in and out (0 at her eyes, 1 all the way back), for the lift as for the distance. */
	public static float eased() {
		return blend * blend * (3 - 2 * blend);
	}

	/** For the harness (DevServer's state): whether it holds the view, is blending out, the blend and the distance. */
	public static String describe() {
		return String.format(java.util.Locale.ROOT, "%s%s blend %.2f dist %.2f", active ? "on" : "off", leaving ? " leaving" : "",
			blend, distance);
	}

	/** The view Options.setCameraType is asked for (OptionsCameraMixin): never first person while one runs. */
	public static CameraType allowed(CameraType type) {
		return active && !leaving && type.isFirstPerson() ? CameraType.THIRD_PERSON_BACK : type;
	}
}
