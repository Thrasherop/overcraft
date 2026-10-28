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
		new Spec(new Mount("Weapon_R", new Quaternionf(-1, 0, 0, 0)), new Mount("Weapon_R", new Quaternionf())),
		// Mercy's staff and blaster, entities on 000A = Weapon_R with no offset or turn in both her models' tables
		// (research/mercy/models.json)
		Hero.MERCY, new Spec(new Mount("Weapon_R", new Quaternionf()), new Mount("Weapon_R", new Quaternionf())));

	private HeroWeapon() {
	}

	/** The weapon as the hero holds it now (in the hero model's space), or null when this hero has none or isn't posed yet. */
	public static EmoteProps.@Nullable Placed place(Hero hero, HeroAnimator animator, SkinnedModel heroModel, boolean firstPerson,
		float now) {
		Spec spec = SPECS.get(hero);
		// Mercy holds one of two (hero_models/mercy_staff.glb, mercy_blaster.glb): the one her state has out
		HeroModels.Loaded weapon = hero == Hero.MERCY ? HeroModels.get(animator.blaster() ? "mercy_blaster" : "mercy_staff")
			: HeroModels.weapon(hero);
		// in an emote his hand is where the emote's pose has it (the props hang on the same pose: EmoteProps)
		Skeleton.Pose heroPose = firstPerson ? animator.lastViewPose()
			: animator.emoting() && animator.emotePose() != null ? animator.emotePose() : animator.lastPose();
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

	/**
	 * Where the held weapon's muzzle is (its hardpoint 0006: the staff's head, where Mercy's beams start; the blaster's
	 * barrel), in the hero model's space, or null.
	 */
	public static org.joml.@Nullable Vector3f muzzle(Hero hero, HeroAnimator animator, SkinnedModel heroModel, boolean firstPerson,
		float now) {
		EmoteProps.Placed placed = place(hero, animator, heroModel, firstPerson, now);
		if (placed == null) {
			return null;
		}
		SkinnedModel weapon = placed.loaded().model();
		int node = weapon.node("hardpoint_0006");
		if (node < 0) {
			return null;
		}
		Skeleton.Pose own = pose(weapon, animator, firstPerson, now);
		return new Matrix4f(placed.at()).mul(Skeleton.world(weapon, own, node)).getTranslation(new org.joml.Vector3f());
	}

	/** Its own pose: held, its reload and Sound Barrier clips while his play, and its additive layers on top. */
	/**
	 * Whether the weapon shows in the hero's emote now: in the hand unless the emote's data puts it away
	 * (EmoteData.stowed: Lúcio's emotes stow it over windows, Heroic keeps it out); hidden when there's no data.
	 */
	public static boolean inEmote(Hero hero, HeroAnimator animator) {
		EmoteData.Emotes data = EmoteData.of(hero.id);
		String emote = animator.emote();
		return data != null && emote != null
			&& !data.stowed(emote, animator.emoteClip(), animator.emoteAt(), hero == Hero.MERCY && animator.blaster());
	}

	private static Skeleton.Pose pose(SkinnedModel model, HeroAnimator a, boolean firstPerson, float now) {
		Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
		SkinnedModel.Clip idle = model.clips.get("idle");
		boolean staff = model.clips.containsKey("holstered");
		if (idle != null && staff) {
			// Mercy's staff: its held loop (84A: the head's rings turning) on the clock
			Skeleton.sample(idle, now % Math.max(idle.duration(), 1.0E-3f), pose);
		} else if (idle != null) {
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
		// Mercy's: the blaster's reload and shot (CF0, CDB), the staff's Flash Heal (126E7)
		SkinnedModel.Clip own = model.clips.get("reload");
		t = a.since("reload_blaster", now);
		if (own != null && t >= 0 && t < own.duration()) {
			Skeleton.sample(own, t, pose);
		}
		add(model, pose, "add_fire", a.since("fire_blaster", now));
		add(model, pose, "add_flash_heal", a.since("flash_heal", now));
		SkinnedModel.Clip beam = model.clips.get("add_beam");
		if (beam != null && a.mercyBeam() != 0) {
			Skeleton.addAdditive(beam, now % Math.max(beam.duration(), 1.0E-3f), pose, 1);
		}
		if (staff) {
			// the staff's clips key its root (bone_0001) at a bake from her first-person rig (84A's equals her 1P Weapon_R
			// to the mm): not applied, the root stays on her Weapon_R (research\mercy\weapon_attach.md, the contacts)
			int root = model.node("bone_0001");
			if (root >= 0) {
				Skeleton.Pose rest = new Skeleton.Pose(model.nodeNames.length).rest(model);
				pose.t[root].set(rest.t[root]);
				pose.r[root].set(rest.r[root]);
				pose.s[root].set(rest.s[root]);
			}
		}
		return pose;
	}

	private static void add(SkinnedModel model, Skeleton.Pose pose, String name, float t) {
		SkinnedModel.Clip clip = model.clips.get(name);
		if (clip != null && t >= 0 && t <= clip.duration()) {
			Skeleton.addAdditive(clip, t, pose, 1);
		}
	}
}
