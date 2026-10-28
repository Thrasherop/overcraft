package dev.overcraft.client.model;

import dev.overcraft.hero.Hero;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A hero's weapon that is a model of its own: Lúcio's Sonic Amplifier, an entity hung on his hardpoint 000A (graph 0776's
 * StateCosmeticEntity, lucio-map.md), from the player's pack (hero_models/lucio_weapon.glb). It plays its own clips with
 * his (the reload, Sound Barrier, the volley's and Soundwave's additives: tools/clipmaps/lucio_weapon.txt).
 */
public final class HeroWeapon {
	/** Where it hangs: a bone of the hero's, turned by the hardpoint's rotation (its offset is zero for Lúcio). */
	private record Mount(String bone, Quaternionf turn) {
	}

	private record Spec(Mount thirdPerson, Mount firstPerson) {
	}

	/**
	 * Hardpoint 000A from the models' own hardpoint tables (research/lucio/models.json): on Weapon_R in both, turned 180
	 * degrees about X in the third-person model DB3F (rotation x -1) and not at all in the first-person DB40.
	 */
	private static final Map<Hero, Spec> SPECS = Map.of(Hero.LUCIO,
		new Spec(new Mount("Weapon_R", new Quaternionf(-1, 0, 0, 0)), new Mount("Weapon_R", new Quaternionf())));

	private HeroWeapon() {
	}

	/** The weapon as the hero holds it now (in the hero model's space), or null when this hero has none or isn't posed yet. */
	public static EmoteProps.@Nullable Placed place(Hero hero, HeroAnimator animator, SkinnedModel heroModel, boolean firstPerson,
		float now) {
		Spec spec = SPECS.get(hero);
		HeroModels.Loaded weapon = HeroModels.weapon(hero);
		Skeleton.Pose heroPose = firstPerson ? animator.lastViewPose() : animator.lastPose();
		if (spec == null || weapon == null || heroPose == null) {
			return null;
		}
		Mount mount = firstPerson ? spec.firstPerson() : spec.thirdPerson();
		int bone = heroModel.node(mount.bone());
		if (bone < 0) {
			return null;
		}
		Matrix4f at = Skeleton.world(heroModel, heroPose, bone).rotate(mount.turn());
		Skeleton.Pose own = pose(weapon.model(), animator, firstPerson, now);
		return new EmoteProps.Placed(weapon, at, Skeleton.skin(weapon.model(), own, -1, null));
	}

	/** Its own pose: held, its reload and Sound Barrier clips while his play, and its additive layers on top. */
	private static Skeleton.Pose pose(SkinnedModel model, HeroAnimator a, boolean firstPerson, float now) {
		Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
		SkinnedModel.Clip idle = model.clips.get("idle");
		if (idle != null) {
			Skeleton.sample(idle, 0, pose);
		}
		// two reloads, 248D at 30 fps and 26E8 at 60 (aliases 80AA8045 / 80AA8050): which view each is for isn't read
		// (ours: the 60 fps one in first person, where it's seen up close)
		SkinnedModel.Clip reload = model.clips.get(firstPerson && model.clips.containsKey("reload_1p") ? "reload_1p" : "reload");
		float t = a.since("reload", now);
		if (reload != null && t >= 0 && t < reload.duration()) {
			Skeleton.sample(reload, t, pose);
		}
		SkinnedModel.Clip cast = model.clips.get("ult_cast");
		SkinnedModel.Clip end = model.clips.get("ult_end");
		t = a.since("sound_barrier", now);
		if (cast != null && t >= 0 && t < cast.duration()) {
			Skeleton.sample(cast, t, pose);
		} else if (cast != null && end != null && t >= 0 && t < cast.duration() + end.duration()) {
			Skeleton.sample(end, t - cast.duration(), pose);
		}
		add(model, pose, "add_fire", a.since("fire", now));
		add(model, pose, "add_soundwave", a.since("soundwave", now));
		return pose;
	}

	private static void add(SkinnedModel model, Skeleton.Pose pose, String name, float t) {
		SkinnedModel.Clip clip = model.clips.get(name);
		if (clip != null && t >= 0 && t <= clip.duration()) {
			Skeleton.addAdditive(clip, t, pose, 1);
		}
	}
}
