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
		Hero.MERCY, new Spec(new Mount("Weapon_R", new Quaternionf()), new Mount("Weapon_R", new Quaternionf())),
		// Ana's Biotic Rifle, entity 159C on her hardpoint 00C9 = bone_0606, no offset or turn in both her models' tables
		// (research/ana/models.json)
		Hero.ANA, new Spec(new Mount("bone_0606", new Quaternionf()), new Mount("bone_0606", new Quaternionf())),
		// Sombra's Machine Pistol, entity 1221 on her hardpoint 000A = Weapon_R, no offset or turn in both her models' tables
		// (research/sombra/work/assets.md)
		Hero.SOMBRA, new Spec(new Mount("Weapon_R", new Quaternionf()), new Mount("Weapon_R", new Quaternionf())),
		// Brigitte's Rocket Flail handle, entity 2282 on her hardpoint 000A = Weapon_R, no offset in both views
		// (research/brigitte/work/assets.md); hero_models/brigitte_weapon.glb
		Hero.BRIGITTE, new Spec(new Mount("Weapon_R", new Quaternionf()), new Mount("Weapon_R", new Quaternionf())));

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
		if (!firstPerson && animator.emoting() && heroPose == animator.emotePose()) {
			// the carrier's chain as the emote's data has it (EmoteProps.atRestUndriven: Ana's rifle in Listen Up)
			heroPose = EmoteProps.atRestUndriven(heroModel, heroPose, bone, animator.emoteClip());
		}
		Matrix4f at = Skeleton.world(heroModel, heroPose, bone).rotate(mount.turn());
		Skeleton.Pose own = pose(weapon.model(), animator, firstPerson, now);
		return new EmoteProps.Placed(weapon, at, Skeleton.skin(weapon.model(), own, -1, null));
	}

	/**
	 * A second model held for a moment: Ana's Sleep Dart pistol (1175E, entity 15CA), out from the cast's start until
	 * 0.7 s (0BDD #105 / #116) on her hardpoint 0143 (1P) / 00A6 (3P), both Weapon_R with no offset or turn (research/ana
	 * models.json). Its own clip (3DC2) isn't in the extract: its rest pose. Null otherwise.
	 */
	private static EmoteProps.@Nullable Placed sidearm(Hero hero, HeroAnimator animator, SkinnedModel heroModel, boolean firstPerson,
		float now) {
		if (hero != Hero.ANA || animator.emoting()) {
			return null;
		}
		float t = animator.since("cast_sleep", now);
		HeroModels.Loaded pistol = HeroModels.get("ana_pistol");
		Skeleton.Pose heroPose = firstPerson ? animator.lastViewPose() : animator.lastPose();
		int bone = heroModel.node("Weapon_R");
		if (t < 0 || t >= dev.overcraft.ana.AnaStats.SLEEP_PISTOL_GONE || pistol == null || heroPose == null || bone < 0) {
			return null;
		}
		Skeleton.Pose own = new Skeleton.Pose(pistol.model().nodeNames.length).rest(pistol.model());
		return new EmoteProps.Placed(pistol, Skeleton.world(heroModel, heroPose, bone), Skeleton.skin(pistol.model(), own, -1, null));
	}

	/**
	 * Every model the hero carries besides the weapon in hand: Ana's Sleep Dart pistol for a moment (sidearm); Brigitte's
	 * flail head seated on the handle's socket (hardpoint 0254 = bone_0FCD, entity 2378 on it: brigitte_flail.txt) and, in
	 * third person, her shield folded on her back (entity 1B25 on her hardpoint 01D8 = bone_0F74, no offset: assets.md).
	 */
	public static java.util.List<EmoteProps.Placed> extras(Hero hero, HeroAnimator animator, SkinnedModel heroModel, boolean firstPerson,
		float now) {
		java.util.List<EmoteProps.Placed> out = new java.util.ArrayList<>();
		EmoteProps.Placed pistol = sidearm(hero, animator, heroModel, firstPerson, now);
		if (pistol != null) {
			out.add(pistol);
		}
		if (hero == Hero.BRIGITTE) {
			EmoteProps.Placed flail = place(hero, animator, heroModel, firstPerson, now);
			HeroModels.Loaded head = HeroModels.get("brigitte_flail_head");
			if (flail != null && head != null) {
				SkinnedModel handle = flail.loaded().model();
				int socket = handle.node("bone_0FCD");
				if (socket >= 0) {
					Matrix4f at = new Matrix4f(flail.at()).mul(Skeleton.world(handle, pose(handle, animator, firstPerson, now), socket));
					out.add(new EmoteProps.Placed(head, at, Skeleton.skin(head.model(), rest(head.model()), -1, null)));
				}
			}
			HeroModels.Loaded shield = HeroModels.get("brigitte_shield");
			EmoteProps.Placed barrier = firstPerson ? null : barrierShown(animator, now);
			if (barrier != null) {
				out.add(barrier);
			} else if (shield != null && animator.shieldUp(now)) {
				// up in first person: drawn in the view's own frame (HeroModelRenderer.submitArms)
			} else {
				Skeleton.Pose heroPose = firstPerson ? null : animator.emoting() && animator.emotePose() != null ? animator.emotePose()
					: animator.lastPose();
				int back = heroModel.node("bone_0F74");
				if (heroPose != null && shield != null && back >= 0) {
					out.add(new EmoteProps.Placed(shield, Skeleton.world(heroModel, heroPose, back),
						Skeleton.skin(shield.model(), rest(shield.model()), -1, null)));
				}
			}
		}
		return out;
	}

	/**
	 * Her barrier when it's up, in her model's space (her feet the origin, her facing ahead): the shield unfolded (its deploy
	 * clip, then its held idle_up: brigitte_shield.txt), upright, square to her facing, 0.9 m ahead, from the ground up,
	 * where dev.overcraft.brigitte.Barrier takes hits. Null when it's down.
	 */
	public static EmoteProps.@Nullable Placed barrierShown(HeroAnimator animator, float now) {
		HeroModels.Loaded shield = HeroModels.get("brigitte_shield");
		if (shield == null || !animator.shieldUp(now)) {
			return null;
		}
		Skeleton.Pose own = rest(shield.model());
		float t = animator.since("shield_raise", now);
		SkinnedModel.Clip deploy = shield.model().clips.get("deploy");
		SkinnedModel.Clip held = shield.model().clips.get("idle_up");
		if (deploy != null && t < deploy.duration()) {
			Skeleton.sample(deploy, t, own);
		} else if (held != null) {
			Skeleton.sample(held, t % Math.max(held.duration(), 1.0E-3f), own);
		}
		Matrix4f at = new Matrix4f().translate(0, (float) BARRIER_BASE, (float) dev.overcraft.brigitte.BrigitteStats.BARRIER_AHEAD);
		return new EmoteProps.Placed(shield, at, Skeleton.skin(shield.model(), own, -1, null));
	}

	/** The deployed shield's origin above her feet: its lowest point (-1.51 m on the mesh) just under the ground. */
	private static final double BARRIER_BASE = 1.31;

	private static Skeleton.Pose rest(SkinnedModel model) {
		return new Skeleton.Pose(model.nodeNames.length).rest(model);
	}

	/**
	 * The weapon in the hero select's pose (the showcase entity's own weapon, research\mercy\gaps2.md 4, H): Mercy's
	 * select entity 3A80 carries her staff on Weapon_R (slot 12A5), out for every clip; Lúcio's amplifier is put away
	 * in his intro (C1EB) and out in his hold (BC3F). Mercy's staff plays clips of its own there (ACA4 / AE33 / AE31 /
	 * AE35: not in the extraction yet): ours, its rest pose, as the research says until they're exported. Null when the
	 * hero has no weapon of its own or it's put away now.
	 */
	public static EmoteProps.@Nullable Placed placeShowcase(Hero hero, SkinnedModel heroModel, Skeleton.Pose pose, boolean intro) {
		Spec spec = SPECS.get(hero);
		if (spec == null || hero == Hero.LUCIO && intro) {
			return null;
		}
		HeroModels.Loaded weapon = hero == Hero.MERCY ? HeroModels.get("mercy_staff") : HeroModels.weapon(hero);
		int bone = heroModel.node(spec.thirdPerson().bone());
		if (weapon == null || bone < 0) {
			return null;
		}
		Matrix4f at = Skeleton.world(heroModel, pose, bone).rotate(spec.thirdPerson().turn());
		Skeleton.Pose own = new Skeleton.Pose(weapon.model().nodeNames.length).rest(weapon.model());
		SkinnedModel.Clip idle = weapon.model().clips.get("idle");
		if (idle != null && hero != Hero.MERCY) {
			Skeleton.sample(idle, 0, own);
			// Ana's rifle: its idle keys the root 1.44 m up, as in play (pose below); kept, the select screen drew it out of
			// sight over her head and her hands held nothing (the owner, 0.3.24)
			int root = weapon.model().node("bone_0001");
			if (root >= 0 && bakedRoot(weapon.model(), root)) {
				Skeleton.Pose rest = new Skeleton.Pose(weapon.model().nodeNames.length).rest(weapon.model());
				own.t[root].set(rest.t[root]);
				own.r[root].set(rest.r[root]);
				own.s[root].set(rest.s[root]);
			}
		}
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
		EmoteData.WeaponClip played = null;
		if (staff && a.emoting() && a.emote() != null) {
			// in an emote whose data plays the staff's own clip (Caduceus, Parasol, Flurry: research\mercy\emote_weapons.md)
			EmoteData.Emotes data = EmoteData.of("mercy");
			played = data == null ? null : data.weaponClip(a.emote(), a.emoteClip(), a.emoteAt(), "staff");
		}
		SkinnedModel.Clip staffClip = played == null ? null : model.clips.get(played.clip());
		if (staffClip != null) {
			Skeleton.sample(staffClip, Math.min(Math.max(a.emoteAt() - played.start(), 0), staffClip.duration()), pose);
		} else if (idle != null && staff) {
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
		int root = model.node("bone_0001");
		if (staff || root >= 0 && bakedRoot(model, root)) {
			// the staff's clips key its root (bone_0001) at a bake from her first-person rig (84A's equals her 1P Weapon_R
			// to the mm): not applied, the root stays on her Weapon_R (research\mercy\weapon_attach.md, the contacts).
			// Ana's rifle the same: its idle 3C59 keys the root 1.44 m up, as if from her feet, while her own clips already
			// carry its hardpoint (00C9 = bone_0606) to her hands; applied, it hung 1.4 m over her head (found playing)
			if (root >= 0) {
				Skeleton.Pose rest = new Skeleton.Pose(model.nodeNames.length).rest(model);
				pose.t[root].set(rest.t[root]);
				pose.r[root].set(rest.r[root]);
				pose.s[root].set(rest.s[root]);
			}
		}
		return pose;
	}

	/**
	 * One of the weapon's own (not additive) clips carries its root half a metre or more from rest: keyed in the hero's space,
	 * not the hardpoint's. Ana's rifle's idle (1.44 m up); Sombra's pistol has no idle, but its third-person reload 3E60 keys
	 * the root 1.2 m up and 0.56 m ahead: applied, the pistol flew over her head through every reload (found playing).
	 */
	private static boolean bakedRoot(SkinnedModel model, int root) {
		return BAKED.computeIfAbsent(model, m -> {
			Skeleton.Pose rest = new Skeleton.Pose(m.nodeNames.length).rest(m);
			for (SkinnedModel.Clip clip : m.clips.values()) {
				if (clip.name().startsWith("add_")) {
					continue;
				}
				Skeleton.Pose at = new Skeleton.Pose(m.nodeNames.length).rest(m);
				Skeleton.sample(clip, 0, at);
				if (at.t[root].distance(rest.t[root]) >= 0.5f) {
					return true;
				}
			}
			return false;
		});
	}

	private static final java.util.Map<SkinnedModel, Boolean> BAKED = new java.util.WeakHashMap<>();

	private static void add(SkinnedModel model, Skeleton.Pose pose, String name, float t) {
		SkinnedModel.Clip clip = model.clips.get(name);
		if (clip != null && t >= 0 && t <= clip.duration()) {
			Skeleton.addAdditive(clip, t, pose, 1);
		}
	}
}
