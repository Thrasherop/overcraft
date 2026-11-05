package dev.overcraft.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A weapon's own clips, played by rule from what the hero is doing rather than by a list of names: a clip map's name is
 * [add_]&lt;base&gt;[_1p | _3p | _sm0 | _sm1], where &lt;base&gt; is the action it answers (the server's name for it, Combat.animate, or an alias
 * below) and the view suffixes are the data's two-state machines (sm[0] first person, sm[1] third: every weapon's clip map). Before
 * this, a fixed list found Lúcio's and Mercy's clips and no other weapon's (#91, the 2026-11-02 audit: Sigma's spheres,
 * Brigitte's flail head, Sombra's EMP and melee, Sojourn's energy never moved).
 */
final class WeaponClips {
	/** Clip bases played by state, or by {@link #fire}, not by an action of their own name. */
	private static final List<String> STATES = List.of("idle", "stance", "holstered", "energy", "pose", "layer", "template", "beam", "move", "emote",
		"reload_ammo_left", "fire_first", "fire_alt", "fire_start", "fire_end", "ult_end", "ult_cast_end");
	/** Bases whose action goes by other names. */
	private static final Map<String, String[]> ALIASES = Map.of(
		"reload", new String[] {"reload", "reload_blaster"},
		"melee", new String[] {"melee", "melee_2", "melee_3"},
		"flash_heal", new String[] {"flash_heal", "flash_heal_flying"},
		// Lúcio's Sound Barrier, Sombra's EMP, Brigitte's Rally
		"ult_cast", new String[] {"sound_barrier", "ult_cast", "ult_start"},
		// the first-person volley's three clips cycle (187F v59): the third person's alias plays sm[1] for all three (fire_a below)
		"fire_a", new String[] {"spheres_0"},
		"fire_b", new String[] {"spheres_1"},
		"fire_c", new String[] {"spheres_2"});

	private WeaponClips() {
	}

	/** Lays the clips the hero's actions and state call for over the pose (which holds the weapon's idle). */
	static void play(SkinnedModel model, HeroAnimator a, boolean firstPerson, float now, Skeleton.Pose pose) {
		stance(model, a, firstPerson, now, pose);
		SkinnedModel.Clip newest = null;
		float newestAt = Float.MAX_VALUE;
		List<SkinnedModel.Clip> layers = new ArrayList<>();
		List<Float> layerAt = new ArrayList<>();
		for (String clipName : model.clips.keySet()) {
			boolean additive = clipName.startsWith("add_");
			String base = base(clipName);
			// "reload_ammo_left" is the state list's for its own name, but the reload's clip when pick chooses it
			String name = clipName;
			if (base.equals("reload_ammo_left") && clipName.equals(pick(model, false, "reload", a, firstPerson))) {
				base = "reload";
			}
			if (base.equals("fire") || isState(base) || !clipName.equals(pick(model, additive, base, a, firstPerson))) {
				continue;
			}
			float since = since(a, base, firstPerson, now);
			SkinnedModel.Clip clip = model.clips.get(name);
			if (since < 0 || since >= clip.duration()) {
				continue;
			}
			if (additive || reloadIsAdditive(model, base)) {
				layers.add(clip);
				layerAt.add(since);
			} else if (since < newestAt) {
				newest = clip;
				newestAt = since;
			}
		}
		if (newest != null) {
			Skeleton.sample(newest, newestAt, pose);
		}
		chain(model, a, now, pose, "ult_cast", "ult_end", false);
		chain(model, a, now, pose, "ult_cast", "add_ult_cast_end", true);
		// over the pose clip: sampled first they'd be lost on its keyed bones for its whole length
		layers(model, a, firstPerson, now, pose);
		for (int i = 0; i < layers.size(); i++) {
			Skeleton.addAdditive(layers.get(i), layerAt.get(i), pose, 1);
		}
		fire(model, a, firstPerson, now, pose);
	}

	private static boolean isState(String base) {
		for (String state : STATES) {
			if (base.startsWith(state)) {
				return true;
			}
		}
		return false;
	}

	/** Mercy's blaster's reload CF0 is from an additive folder (4-4), though named as a pose (additive_clips.md). */
	private static boolean reloadIsAdditive(SkinnedModel model, String base) {
		return base.equals("reload") && model.clips.containsKey("add_layer") && !model.clips.containsKey("holstered");
	}

	private static float since(HeroAnimator a, String base, boolean firstPerson, float now) {
		String[] actions = ALIASES.getOrDefault(base, new String[] {base});
		float best = Float.MAX_VALUE;
		for (String action : actions) {
			best = Math.min(best, a.since(action, now));
		}
		if (!firstPerson && base.equals("fire_a")) {
			best = Math.min(best, Math.min(a.since("spheres_1", now), a.since("spheres_2", now)));
		}
		return best;
	}

	/** The action a clip answers: its name less "add_" and its view suffix. */
	static String base(String name) {
		String s = name.startsWith("add_") ? name.substring(4) : name;
		for (String suffix : new String[] {"_1p", "_3p", "_sm0", "_sm1"}) {
			if (s.endsWith(suffix)) {
				return s.substring(0, s.length() - suffix.length());
			}
		}
		return s;
	}

	/** This view's clip for a base: its own suffix, else the plain one, else (third person) a clip for no view in particular. */
	static String pick(SkinnedModel model, boolean additive, String base, HeroAnimator a, boolean firstPerson) {
		String p = additive ? "add_" + base : base;
		if (base.equals("reload") && firstPerson && model.clips.containsKey("reload_ammo_left_1p")) {
			// Ana's rifle: its plain "reload" is the third person's (3CE4, time df 0x66), its first person's empty reload 3C5A isn't extracted, so the ammo-left one
			return "reload_ammo_left_1p";
		}
		if (base.equals("reload") && !firstPerson && a.reloadWithAmmo() && model.clips.containsKey("reload_ammo_left")) {
			return "reload_ammo_left";
		}
		for (String name : firstPerson ? new String[] {p + "_1p", p + "_sm0", p} : new String[] {p + "_3p", p + "_sm1", p}) {
			if (model.clips.containsKey(name)) {
				return name;
			}
		}
		if (firstPerson) {
			return null;
		}
		// a set whose third person has the first person's clip alone (Sigma's Flux: sm[0] only), not the third's own volleys
		return base.equals("fire_b") || base.equals("fire_c") ? null : model.clips.containsKey(p + "_sm0") ? p + "_sm0" : null;
	}

	/** A clip that follows another when it has run (Lúcio's Sound Barrier's end, Sombra's EMP's). */
	private static void chain(SkinnedModel model, HeroAnimator a, float now, Skeleton.Pose pose, String base, String then, boolean additive) {
		SkinnedModel.Clip first = model.clips.get(base);
		SkinnedModel.Clip next = model.clips.get(then);
		if (first == null || next == null) {
			return;
		}
		float t = since(a, base, false, now) - first.duration();
		if (t >= 0 && t < next.duration()) {
			if (additive) {
				Skeleton.addAdditive(next, t, pose, 1);
			} else {
				Skeleton.sample(next, t, pose);
			}
		}
	}

	/** A held burst's gap (s): as HeroAnimator.BURST_GAP. */
	private static final float BURST_GAP = 0.12f;

	/**
	 * The shot's layer by view: a burst's first (Sojourn), the view's own (Sojourn's, Ana's whose add_fire_1p isn't extracted),
	 * the plain one (Lúcio's, the blaster's); a burst with its end clip plays on from the burst's start and ends (Sombra's 8181C011).
	 */
	private static void fire(SkinnedModel model, HeroAnimator a, boolean firstPerson, float now, Skeleton.Pose pose) {
		float t = Math.min(a.since("fire", now), a.since("fire_blaster", now));
		SkinnedModel.Clip tail = model.clips.get("add_fire_end");
		if (tail != null) {
			SkinnedModel.Clip burst = model.clips.get("add_fire");
			if (t < BURST_GAP && burst != null) {
				Skeleton.addAdditive(burst, a.burstAge(now) % Math.max(burst.duration(), 1.0E-3f), pose, 1);
			} else if (t - BURST_GAP >= 0 && t - BURST_GAP < tail.duration()) {
				Skeleton.addAdditive(tail, t - BURST_GAP, pose, 1);
			}
			return;
		}
		String[] names = firstPerson
			? new String[] {a.fireFirst() ? "add_fire_first_1p" : "", "add_fire_1p", "add_fire_alt_1p", "add_fire"}
			: new String[] {"add_fire_3p", "add_fire"};
		for (String name : names) {
			SkinnedModel.Clip clip = model.clips.get(name);
			if (clip != null) {
				if (t >= 0 && t < clip.duration()) {
					Skeleton.addAdditive(clip, t, pose, 1);
				}
				return;
			}
		}
	}

	/** The poses a state holds, under the action's: Brigitte's shield stance, Sigma's air stance. */
	private static void stance(SkinnedModel model, HeroAnimator a, boolean firstPerson, float now, Skeleton.Pose pose) {
		SkinnedModel.Clip shield = firstPerson ? null : model.clips.get("idle_shield");
		if (shield != null && a.shieldUp(now)) {
			Skeleton.sample(shield, 0, pose);
		}
		SkinnedModel.Clip air = model.clips.get("stance_air_sm0_se1");
		if (air != null && a.airborne()) {
			Skeleton.sample(air, now % Math.max(air.duration(), 1.0E-3f), pose);
		}
	}

	/** The layers a state holds: Sigma's stance, Lúcio's move, Sojourn's energy, Mercy's beams. */
	private static void layers(SkinnedModel model, HeroAnimator a, boolean firstPerson, float now, Skeleton.Pose pose) {
		loop(model, a.moving() && model.clips.containsKey("add_stance_sm2_move") ? "add_stance_sm2_move" : "add_stance_sm2", now, pose);
		if (a.moving()) {
			loop(model, "add_move", now, pose);
		}
		energy(model, a, firstPerson, now, pose);
		beam(model, a, now, pose);
	}

	private static void loop(SkinnedModel model, String name, float now, Skeleton.Pose pose) {
		SkinnedModel.Clip clip = model.clips.get(name);
		if (clip != null) {
			Skeleton.addAdditive(clip, now % Math.max(clip.duration(), 1.0E-3f), pose, 1);
		}
	}

	/**
	 * The Railgun's energy machine (sojourn_weapon.txt, 82AD806B first person, 82AD809D third; its conditions aren't decoded, so
	 * ours: empty is state 0, charging 3, full 2, and each change plays the clip of that change where the data has one).
	 */
	private static void energy(SkinnedModel model, HeroAnimator a, boolean firstPerson, float now, Skeleton.Pose pose) {
		if (!model.clips.containsKey("add_energy_state_2")) {
			return;
		}
		String view = firstPerson ? "" : "_3p";
		SkinnedModel.Clip change = model.clips.get("add_energy_" + a.energyFrom() + "_to_" + a.energyState() + view);
		float t = now - a.energyAt();
		if (change != null && t >= 0 && t < change.duration()) {
			Skeleton.addAdditive(change, t, pose, 1);
		} else if (a.energyState() != 0) {
			loop(model, "add_energy_state_" + a.energyState() + view, now, pose);
		}
	}

	/** Mercy's staff's beam layers (mercy_staff.txt, 0x092): start, loop and end, heal plain and boost "_2" (M which is which). */
	private static void beam(SkinnedModel model, HeroAnimator a, float now, Skeleton.Pose pose) {
		if (!model.clips.containsKey("add_beam")) {
			return;
		}
		int mode = a.mercyBeam();
		if (mode != 0) {
			String v = mode == 2 ? "_2" : "";
			SkinnedModel.Clip start = model.clips.get("add_beam_start" + v);
			float t = now - a.beamOnSince();
			if (start != null && t >= 0 && t < start.duration()) {
				Skeleton.addAdditive(start, t, pose, 1);
			} else {
				loop(model, "add_beam" + v, now, pose);
			}
		} else {
			SkinnedModel.Clip end = model.clips.get("add_beam_end" + (a.beamLast() == 2 ? "_2" : ""));
			float t = now - a.beamOffAt();
			if (end != null && t >= 0 && t < end.duration()) {
				Skeleton.addAdditive(end, t, pose, 1);
			}
		}
	}
}
