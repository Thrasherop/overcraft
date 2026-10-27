package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The props an emote holds (#4: "Their props (the bike, the skateboard...) aren't in yet"): Overwatch's own models from
 * the player's pack (hero_models/prop_*.glb: tools/build_asset_pack.py props). A held prop sits on one of her hands'
 * weapon bones, moved by her emote's own clip; an animated one (the cat, the umbrella) plays its own clips, "intro" from
 * the emote's start then "loop", placed at her feet (or a hand) as Overwatch places it. Which emote holds what, where,
 * and its turn and offset: ours, by eye against the emotes as they play.
 */
public final class EmoteProps {
	/**
	 * Where a prop is put: at her feet (her model's origin), on a hand's weapon bone (turning with it), or at a hand
	 * but upright in her space (the umbrella: its own clips turn it; turned with her hand too, it held the shaft out
	 * level).
	 */
	public static final int FEET = 0, KUNAI_HAND = 1, OFUDA_HAND = 2, AT_KUNAI_HAND = 3, AT_OFUDA_HAND = 4;
	/**
	 * Overwatch's hand hardpoints: on a wrist bone, turned a quarter about the bone's X (her model's hardpoints, read
	 * from the game data: hardpoint_04E2 on Wrist_L, 04E3 / 0522 on Wrist_R, all at the wrist's origin).
	 */
	public static final int KUNAI_WRIST = 5, OFUDA_WRIST = 6;

	/**
	 * A prop in an emote: its model, and its place: turned about X, Y, Z (degrees), moved (metres), then [6] where (FEET,
	 * KUNAI_HAND, ...), and [7] when she lets go of it (seconds into the emote; 0: never): from then it falls from where
	 * it was to the floor (Mic Drop: her hand opens and the mic stayed stuck to her palm). DevServer's "prop" changes a
	 * place live.
	 */
	public record Prop(String model, float[] place) {
	}

	private static Prop prop(String model, int where) {
		return new Prop(model, new float[] {0, 0, 0, 0, 0, 0, where, 0});
	}

	/** A prop to draw this frame: where it is (in her model's space) and its pose. */
	public record Placed(Prop prop, Matrix4f at, Matrix4f[] skin) {
	}

	public record Held(List<Placed> props) {
	}

	public static final RenderStateDataKey<Held> HELD = RenderStateDataKey.create(() -> "overcraft:emote_props");

	/** Each emote's props. */
	public static final Map<String, List<Prop>> PROPS = Map.of(
		"donut_break", List.of(prop("prop_donut", OFUDA_WRIST)),
		"sweet_treat", List.of(prop("prop_donut", OFUDA_WRIST)),
		// her hand opens 0.95 s in (frame by frame, ours)
		"mic_drop", List.of(new Prop("prop_mic", new float[] {0, 0, 0, 0, 0, 0, KUNAI_WRIST, 0.95f})),
		"skateboard", List.of(prop("prop_skateboard", KUNAI_WRIST), prop("prop_bubble_tea", OFUDA_WRIST)),
		"friendly_kitty", List.of(prop("prop_cat", FEET)),
		"rainy_stroll", List.of(prop("prop_umbrella", OFUDA_WRIST)),
		"fox_cuddles", List.of(prop("prop_fox", FEET)),
		// its clip holds it low, its legs under the floor and inside her: lifted onto its feet, in front of her (ours)
		"pod_pet", List.of(new Prop("prop_pod", new float[] {0, 0, 0, 0, 0.25f, 0.6f, FEET, 0})));

	/** Props drawn as her Kitsune Rush fox is: a see-through glowing spirit, not a solid animal. */
	private static final Set<String> SPIRITS = Set.of("prop_fox");

	private EmoteProps() {
	}

	/** Her emote has props (in the pack): her own weapons are put away meanwhile. */
	public static boolean holds(HeroAnimator animator) {
		String emote = animator.emote();
		List<Prop> props = emote == null ? null : PROPS.get(emote);
		if (props != null) {
			for (Prop prop : props) {
				if (HeroModels.get(prop.model()) != null) {
					return true;
				}
			}
		}
		return false;
	}

	/** What the hero's emote holds now, posed, or null for nothing (no emote, no props, or not in the pack). */
	public static @Nullable Held held(HeroAnimator animator, SkinnedModel hero, float now) {
		String emote = animator.emote();
		List<Prop> props = emote == null ? null : PROPS.get(emote);
		if (props == null) {
			return null;
		}
		float t = animator.emoteTime(now);
		// her own steps before her loop: a prop's loop starts with hers (the fox's intro is 0.8 s shorter than hers: it
		// holds its last frame till then)
		EmotePlan plan = EmotePlan.of(hero, emote);
		float heroIntro = plan == null ? 0 : plan.total();
		List<Placed> placed = new ArrayList<>();
		for (Prop prop : props) {
			HeroModels.Loaded loaded = HeroModels.get(prop.model());
			if (loaded == null) {
				continue;
			}
			int where = Math.round(prop.place()[6]);
			Matrix4f at;
			if (where == FEET) {
				// from the root that doesn't move, as Overwatch places it: her root motion, kept off her, moves it back
				at = new Matrix4f().translation(new org.joml.Vector3f(animator.emoteRootShift).negate());
			} else if (where >= KUNAI_WRIST) {
				at = new Matrix4f(animator.emoteHands[where == KUNAI_WRIST ? 2 : 3]).rotateX((float) Math.toRadians(-90));
			} else {
				at = new Matrix4f(animator.emoteHands[where % 2 == 1 ? 0 : 1]);
				if (where >= AT_KUNAI_HAND) {
					at = new Matrix4f().translation(at.getTranslation(new org.joml.Vector3f()));
				}
			}
			SkinnedModel model = loaded.model();
			Skeleton.Pose pose = new Skeleton.Pose(model.nodeNames.length).rest(model);
			SkinnedModel.Clip intro = model.clips.get("intro");
			SkinnedModel.Clip loop = model.clips.get("loop");
			float loopFrom = Math.max(heroIntro, intro == null ? 0 : intro.duration());
			if (intro != null && (t < loopFrom || loop == null)) {
				Skeleton.sample(intro, Math.min(t, intro.duration()), pose);
			} else if (loop != null) {
				float into = t - loopFrom;
				Skeleton.sample(loop, loop.duration() > 0 ? into % loop.duration() : 0, pose);
			}
			float release = prop.place()[7];
			int slot = props.indexOf(prop);
			if (release > 0 && t >= release) {
				// let go: where it was when her hand opened, fallen since (g, ours: Minecraft's falling things are
				// slower; the floor at her feet)
				Matrix4f from = animator.released.get(emote + slot);
				if (from == null) {
					from = new Matrix4f(at);
					animator.released.put(emote + slot, from);
				}
				float fall = 0.5f * 9.8f * (t - release) * (t - release);
				float floor = from.m31() - 0.05f;
				at = new Matrix4f().translation(0, -Math.min(fall, floor), 0).mul(from);
			} else if (release > 0) {
				animator.released.put(emote + slot, new Matrix4f(at));
			}
			placed.add(new Placed(prop, at, Skeleton.skin(model, pose, -1, null)));
		}
		return placed.isEmpty() ? null : new Held(placed);
	}

	/** Draws the props, the pose stack in her model's space (as her own mesh is drawn). */
	static void submit(Held held, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
		for (Placed placed : held.props()) {
			HeroModels.Loaded loaded = HeroModels.get(placed.prop().model());
			if (loaded == null) {
				continue;
			}
			float[] p = placed.prop().place();
			poseStack.pushPose();
			poseStack.mulPose(placed.at());
			poseStack.rotateDegrees(Axis.XP, p[0]);
			poseStack.rotateDegrees(Axis.YP, p[1]);
			poseStack.rotateDegrees(Axis.ZP, p[2]);
			poseStack.translate(p[3], p[4], p[5]);
			if (SPIRITS.contains(placed.prop().model())) {
				HeroModelRenderer.submitSpirit(loaded, placed.skin(), poseStack, collector, 0.8f, 0.7f, KitsuneRenderer.SPIRIT,
					KitsuneRenderer.GLOW, true);
			} else {
				HeroModelRenderer.submitModel(loaded, placed.skin(), poseStack, collector, light, overlay, Set.of(), false, false);
			}
			poseStack.popPose();
		}
	}
}
