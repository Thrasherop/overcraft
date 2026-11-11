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
		Hero.BRIGITTE, new Spec(new Mount("Weapon_R", new Quaternionf()), new Mount("Weapon_R", new Quaternionf())),
		// Sigma's hand spheres, entity 31A7 (model 106B4) on his hardpoint 000A = bone_004A (Weapon_R) in third person and 00C9 =
		// bone_0606 in first (the models' hardpoint tables: 106F7 / 106F6; sigma-map.md "Models"); no offset read (M)
		Hero.SIGMA, new Spec(new Mount("Weapon_R", new Quaternionf()), new Mount("bone_0606", new Quaternionf())),
		// D.Va's Light Gun, entity 0982 (model 10246) on the pilot's hardpoint 000A = Weapon_R in both views (dva-map.md
		// "Models"), no offset read (M); the mech's Fusion Cannons are part of its own model
		Hero.DVA, new Spec(new Mount("Weapon_R", new Quaternionf()), new Mount("Weapon_R", new Quaternionf())),
		// Sojourn's Railgun, entity 2D20 (model D7B4) on her hardpoint 00C9 = bone_0606, no offset, in both views (var 4E30:
		// research/sojourn/work/assets.md); hero_models/sojourn_weapon.glb
		Hero.SOJOURN, new Spec(new Mount("bone_0606", new Quaternionf()), new Mount("bone_0606", new Quaternionf())));

	private HeroWeapon() {
	}

	/** The weapon as the hero holds it now (in the hero model's space), or null when this hero has none or isn't posed yet. */
	public static EmoteProps.@Nullable Placed place(Hero hero, HeroAnimator animator, SkinnedModel heroModel, boolean firstPerson,
		float now) {
		Spec spec = SPECS.get(hero);
		if (hero == Hero.DVA && animator.dvaMech()) {
			// in the mech she holds no gun (the cannons are the mech's)
			return null;
		}
		// Mercy holds one of two (hero_models/mercy_staff.glb, mercy_blaster.glb): the one her state has out
		HeroModels.Loaded weapon = hero == Hero.MERCY ? HeroModels.get(animator.blaster() ? "mercy_blaster" : "mercy_staff")
			: HeroModels.weapon(hero);
		// in an emote his hand is where the emote's pose has it (the props hang on the same pose: EmoteProps)
		Skeleton.Pose heroPose = firstPerson ? animator.lastViewPose()
			: animator.emoting() && animator.emotePose() != null ? animator.emotePose() : animator.lastPose();
		if (spec == null || weapon == null || heroPose == null || heroPose.t.length != heroModel.nodeNames.length) {
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
		if (!firstPerson && keyedInHisSpace(weapon.model(), animator)) {
			// Sigma's spheres in Constellation (CBE3): the clip keys their root in his model's space, from his idle's hand
			// (-0.63, 1.79, 0.54, his idle Weapon_R to the cm) up to the sky and back, while his own Weapon_R starts 1.4 m
			// higher (3.16 m up at 0 s); on the hardpoint the spheres would jump 1.4 m off his hand on the first frame. So
			// the clip's root is where they are, from his origin (techniques/emotes/sigma_emotes.md). Sojourn's Railgun
			// clips equal her hardpoint to the cm, so either reading fits hers: she keeps the hardpoint
			at = new Matrix4f().translation(new org.joml.Vector3f(animator.emoteRootShift).negate());
			own = pose(weapon.model(), animator, false, now, true);
		}
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
		parts(hero, animator, heroModel, firstPerson, now, out);
		if (hero == Hero.BRIGITTE) {
			// in an emote the flail and its head go with the stow windows (slot 3572); the folded shield (339E) no emote touches
			EmoteProps.Placed flail = animator.emoting() && !inEmote(hero, animator) ? null
				: place(hero, animator, heroModel, firstPerson, now);
			HeroModels.Loaded head = HeroModels.get("brigitte_flail_head");
			animator.chainFrom = null;
			animator.chainTo = null;
			if (flail != null && head != null) {
				SkinnedModel handle = flail.loaded().model();
				Skeleton.Pose handlePose = pose(handle, animator, firstPerson, now);
				int socket = handle.node("bone_0FCD");
				int start = handle.node("bone_0596");
				if (socket >= 0) {
					Matrix4f seated = new Matrix4f(flail.at()).mul(Skeleton.world(handle, handlePose, socket));
					Matrix4f at = seated;
					Matrix4f flung = firstPerson || animator.emoting() ? null : flungHead(animator, heroModel, now);
					if (animator.whipHead != null) {
						// Whip Shot's head, flown where the server sent it (1259 #640: the graph moves it, 20 m; its clips reach 5)
						at = new Matrix4f(seated).setTranslation(animator.whipHead);
					} else if (flung != null) {
						at = flung;
					}
					// its own clips with the handle's (brigitte_flail_head.txt: idle, swings, whips, Rally); Cheer plays its own (525F on slot 33C0)
					Skeleton.Pose headPose = animator.emoting() ? emoteClipPose(head.model(), animator, "head")
						: pose(head.model(), animator, firstPerson, now);
					out.add(new EmoteProps.Placed(head, at, Skeleton.skin(head.model(), headPose, -1, null)));
					// the chain (216C / 22CF: the handle's hp 0076 = bone_0596 to the head) while the head is off its socket
					org.joml.Vector3f from = start < 0 ? seated.getTranslation(new org.joml.Vector3f())
						: new Matrix4f(flail.at()).mul(Skeleton.world(handle, handlePose, start)).getTranslation(new org.joml.Vector3f());
					// (to the head's own hardpoint 0004, as the data's ribbon: brigitte-effects.md)
					int tip = head.model().node("hardpoint_0004");
					org.joml.Vector3f to = tip < 0 ? at.getTranslation(new org.joml.Vector3f())
						: new Matrix4f(at).mul(Skeleton.world(head.model(), headPose, tip)).getTranslation(new org.joml.Vector3f());
					if (to.distance(from) > CHAIN_SHOWN) {
						animator.chainFrom = from;
						animator.chainTo = to;
					}
				}
			}
			EmoteProps.Placed hand = firstPerson ? null : handShield(animator, heroModel);
			if (hand != null) {
				out.add(hand);
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
				Matrix4f folded = heroPose == null || back < 0 ? null : Skeleton.world(heroModel, heroPose, back);
				// at rest it is folded (its bones as the deploy clip's first frame: 0.58 x 0.91 m, against 1.4 x 2.55 m open);
				// an emote that hides it parks its bone far off (Flail Tricks: 0F74 over 80 m away), as Kiriko's parked pieces
				if (folded != null && shield != null && folded.getTranslation(new org.joml.Vector3f()).length() < PARKED) {
					out.add(new EmoteProps.Placed(shield, folded, Skeleton.skin(shield.model(), rest(shield.model()), -1, null)));
				}
			}
		}
		return out;
	}


	/**
	 * The models the data attaches as equipment (weapon_parts.json, WeaponParts: Sombra's magazine, Ana's vial and dart, Juno's
	 * cell, Sojourn's reload magazine): each on its parent's hardpoint, as its window and hidden rows say for this view. In an emote
	 * only those on a weapon the emote keeps out (Juno's cell hangs on her body, whose gun an emote's geosets put away).
	 */
	private static void parts(Hero hero, HeroAnimator a, SkinnedModel heroModel, boolean firstPerson, float now, java.util.List<EmoteProps.Placed> out) {
		java.util.List<WeaponParts.Part> parts = WeaponParts.of(hero.id);
		if (parts.isEmpty()) {
			return;
		}
		EmoteProps.Placed weapon = !a.emoting() || inEmote(hero, a) ? place(hero, a, heroModel, firstPerson, now) : null;
		Skeleton.Pose heroPose = firstPerson ? a.lastViewPose() : a.lastPose();
		// the gun's pose once, however many parts hang on it (critique_parts C2)
		Skeleton.Pose gunPose = weapon == null ? null : pose(weapon.loaded().model(), a, firstPerson, now);
		for (WeaponParts.Part part : parts) {
			HeroModels.Loaded model = HeroModels.get(part.model());
			if (model == null || !part.shown(a, firstPerson, now)) {
				continue;
			}
			Matrix4f at;
			if (part.onWeapon()) {
				if (weapon == null) {
					continue;
				}
				SkinnedModel gun = weapon.loaded().model();
				int node = gun.node(part.bone());
				if (node < 0) {
					continue;
				}
				at = new Matrix4f(weapon.at()).mul(Skeleton.world(gun, gunPose, node));
			} else {
				int node = heroModel.node(part.bone());
				if (a.emoting() || heroPose == null || node < 0) {
					continue;
				}
				at = Skeleton.world(heroModel, heroPose, node);
			}
			at.translate(part.t()).rotate(part.q());
			out.add(new EmoteProps.Placed(model, at, Skeleton.skin(model.model(), rest(model.model()), -1, null)));
		}
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

	/**
	 * Her hand shield (24C5 on hardpoint 000B = Weapon_L, slot 3816, no offset: assets.md) in an emote that plays its own
	 * clip and doesn't put it away there (Cheer's 52F0 from 3.3 s; Recalibrate's 5108 is put away all through): the data's
	 * windows (tools/emote_data.py). Out of emotes it shows only as the barrier. Null otherwise.
	 */
	private static EmoteProps.@Nullable Placed handShield(HeroAnimator animator, SkinnedModel heroModel) {
		EmoteData.Emotes data = EmoteData.of("brigitte");
		HeroModels.Loaded shield = HeroModels.get("brigitte_shield");
		String emote = animator.emote();
		Skeleton.Pose heroPose = animator.emotePose();
		int bone = heroModel.node("Weapon_L");
		if (!animator.emoting() || data == null || emote == null || shield == null || heroPose == null || bone < 0
			|| data.weaponClip(emote, animator.emoteClip(), animator.emoteAt(), "shield") == null
			|| data.stowed(emote, animator.emoteClip(), animator.emoteAt(), "stowShield")) {
			return null;
		}
		return new EmoteProps.Placed(shield, Skeleton.world(heroModel, heroPose, bone),
			Skeleton.skin(shield.model(), emoteClipPose(shield.model(), animator, "shield"), -1, null));
	}

	/** A piece of hers posed by the clip the emote's data plays on it now (by its weapon name), else at rest. */
	private static Skeleton.Pose emoteClipPose(SkinnedModel model, HeroAnimator a, String weapon) {
		Skeleton.Pose pose = rest(model);
		EmoteData.Emotes data = a.emoting() && a.emote() != null ? EmoteData.of("brigitte") : null;
		EmoteData.WeaponClip played = data == null ? null : data.weaponClip(a.emote(), a.emoteClip(), a.emoteAt(), weapon);
		SkinnedModel.Clip clip = played == null ? null : model.clips.get(played.clip());
		if (clip != null) {
			Skeleton.sample(clip, Math.min(Math.max(a.emoteAt() - played.start(), 0), clip.duration()), pose);
		}
		return pose;
	}

	/** The head this far (metres) from the handle's chain start is off its socket: the chain shows. */
	private static final float CHAIN_SHOWN = 0.5f;

	/**
	 * Her flung flail head in third person: drawn at her hp 0257 = bone_0606 while the flail is out (1248 #640 at v432),
	 * which her swing clips carry 4.7-4.9 m out (swing at 0.25-0.35 s); seated (null) when the swing has it back at her
	 * pelvis or no swing plays.
	 */
	private static @Nullable Matrix4f flungHead(HeroAnimator a, SkinnedModel heroModel, float now) {
		Skeleton.Pose pose = a.lastPose();
		int bone = heroModel.node("bone_0606");
		boolean swinging = a.since("swing", now) < SWING_OUT || a.since("swing_2", now) < SWING_OUT;
		if (!swinging || pose == null || bone < 0) {
			return null;
		}
		Matrix4f at = Skeleton.world(heroModel, pose, bone);
		org.joml.Vector3f p = at.getTranslation(new org.joml.Vector3f());
		// out on its chain: further from her body's axis than a hand reaches (at rest 0606 hangs 0.73 m off it, at her hip)
		return (float) Math.hypot(p.x, p.z) > FLUNG ? at : null;
	}

	/** Metres from her body's axis beyond which bone_0606 has the head out on its chain. */
	private static final float FLUNG = 1.2f;

	/** A swing's whole clip (0.6 s: BrigitteStats.FLAIL_*). */
	private static final float SWING_OUT = 0.6f;

	/** A bone further than this from her feet (metres) is parked out of sight by the clip, not carried. */
	private static final float PARKED = 5;

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
		return point(hero, animator, heroModel, firstPerson, now, "hardpoint_0006");
	}

	/** The same for any node of the held weapon (Sojourn's Railgun: its energy glow's hp 007C), or null. */
	public static org.joml.@Nullable Vector3f point(Hero hero, HeroAnimator animator, SkinnedModel heroModel, boolean firstPerson,
		float now, String name) {
		EmoteProps.Placed placed = place(hero, animator, heroModel, firstPerson, now);
		if (placed == null) {
			return null;
		}
		SkinnedModel weapon = placed.loaded().model();
		int node = weapon.node(name);
		if (node < 0) {
			return null;
		}
		Skeleton.Pose own = pose(weapon, animator, firstPerson, now);
		return new Matrix4f(placed.at()).mul(Skeleton.world(weapon, own, node)).getTranslation(new org.joml.Vector3f());
	}

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

	/** Its own pose: held, the clips of what the hero is doing (WeaponClips, by name), and its additive layers on top. */
	private static Skeleton.Pose pose(SkinnedModel model, HeroAnimator a, boolean firstPerson, float now) {
		return pose(model, a, firstPerson, now, false);
	}

	/** The emote clip the data plays on this weapon now (by the weapon's name in the hero's data), or null. */
	private static EmoteData.@Nullable WeaponClip emoteClip(SkinnedModel model, HeroAnimator a) {
		if (!a.emoting() || a.emote() == null) {
			return null;
		}
		boolean staff = model.clips.containsKey("holstered");
		boolean flail = model.clips.containsKey("whip_return_blocked");
		// Sojourn's Railgun (its 3P fire layer is its own): Confidence, Standing By and Weapons Check play its clips on slot 4E30
		boolean railgun = model.clips.containsKey("add_fire_3p");
		// Sigma's hand spheres (their Flux aim clip is their own): Heroic, Observations, Relaxation and Constellation play
		// their clips on slot 54CE (techniques/emotes/sigma_emotes.md)
		boolean spheres = model.clips.containsKey("flux_aim_sm0");
		if (!staff && !flail && !railgun && !spheres) {
			return null;
		}
		EmoteData.Emotes data = EmoteData.of(staff ? "mercy" : railgun ? "sojourn" : spheres ? "sigma" : "brigitte");
		return data == null ? null : data.weaponClip(a.emote(), a.emoteClip(), a.emoteAt(),
			staff ? "staff" : railgun ? "railgun" : spheres ? "spheres" : "flail");
	}

	/** Sigma's spheres playing an emote clip that keys their root half a metre or more off rest: in his space (place). */
	private static boolean keyedInHisSpace(SkinnedModel model, HeroAnimator a) {
		if (!model.clips.containsKey("flux_aim_sm0")) {
			return false;
		}
		EmoteData.WeaponClip played = emoteClip(model, a);
		SkinnedModel.Clip clip = played == null ? null : model.clips.get(played.clip());
		int root = model.node("bone_0001");
		if (clip == null || root < 0) {
			return false;
		}
		Skeleton.Pose rest = new Skeleton.Pose(model.nodeNames.length).rest(model);
		Skeleton.Pose at = new Skeleton.Pose(model.nodeNames.length).rest(model);
		Skeleton.sample(clip, 0, at);
		return at.t[root].distance(rest.t[root]) >= 0.5f;
	}

	private static Skeleton.Pose pose(SkinnedModel model, HeroAnimator a, boolean firstPerson, float now, boolean keepRoot) {
		Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
		SkinnedModel.Clip idle = model.clips.get("idle");
		boolean staff = model.clips.containsKey("holstered");
		// in an emote whose data plays the staff's own clip (Caduceus, Parasol, Flurry: research\mercy\emote_weapons.md),
		// Brigitte's flail's (Cheer's 5265 on slot 3572), the Railgun's (techniques/emotes/sojourn_emotes.md) or Sigma's
		// spheres'. The Railgun's emote clips key its root at her bone_0606 in her space (a bake, equal to the mm): the root
		// is kept on the hardpoint below (bakedRoot), so only its moving parts play
		EmoteData.WeaponClip played = emoteClip(model, a);
		SkinnedModel.Clip staffClip = played == null ? null : model.clips.get(played.clip());
		if (staffClip != null) {
			Skeleton.sample(staffClip, Math.min(Math.max(a.emoteAt() - played.start(), 0), staffClip.duration()), pose);
		} else if (idle != null && staff) {
			// Mercy's staff: its held loop (84A: the head's rings turning) on the clock
			Skeleton.sample(idle, now % Math.max(idle.duration(), 1.0E-3f), pose);
		} else if (idle != null) {
			Skeleton.sample(idle, 0, pose);
		}
		if (staffClip == null) {
			WeaponClips.play(model, a, firstPerson, now, pose);
		}
		int root = model.node("bone_0001");
		if (!keepRoot && (staff || root >= 0 && bakedRoot(model, root))) {
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

}
