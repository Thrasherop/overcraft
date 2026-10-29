package dev.overcraft.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * Abilities that show the hero from behind while they last (the owner, #55: "When lifeline or catnap is active it
 * should automatically go into 3rd person"): the view leaves first person when one starts and comes back to the view
 * the player had when it ends; meanwhile F5 skips first person, as EmoteCamera does.
 */
public final class AbilityCamera {
	private static boolean active;
	private static CameraType before = CameraType.FIRST_PERSON;

	private AbilityCamera() {
	}

	/** Each client tick: whether such an ability runs now. */
	public static void update(Minecraft mc, boolean wanted) {
		if (wanted && !active) {
			active = true;
			before = mc.options.getCameraType();
			if (before.isFirstPerson()) {
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
		} else if (!wanted && active) {
			active = false;
			mc.options.setCameraType(before);
		}
	}

	/** The view Options.setCameraType is asked for (OptionsCameraMixin): never first person while one runs. */
	public static CameraType allowed(CameraType type) {
		return active && type.isFirstPerson() ? CameraType.THIRD_PERSON_BACK : type;
	}
}
