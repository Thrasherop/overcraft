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
 * the player's pack (hero_models/prop_*.glb: tools/build_asset_pack.py props), each on one of her hands' weapon bones,
 * moved by her emote's own clip as Overwatch's are. Which emote holds what, in which hand, and the prop's turn and
 * offset on the bone: ours, by eye against the emotes as they play.
 */
public final class EmoteProps {
	/** A prop in an emote: its model, the hand (true: the "_L" weapon bone, her kunai hand), its turn and offset. */
	public record Prop(String model, boolean kunaiHand, float[] place) {
	}

	/** The props to draw this frame, each with its hand bone's matrix (in her model's space). */
	public record Held(List<Prop> props, Matrix4f[] hands) {
	}

	public static final RenderStateDataKey<Held> HELD = RenderStateDataKey.create(() -> "overcraft:emote_props");

	/**
	 * Each emote's props (DevServer's "prop" changes a place live: turned about X, Y, Z in degrees, then moved along the
	 * bone's axes in metres).
	 */
	public static final Map<String, List<Prop>> PROPS = Map.of(
		"donut_break", List.of(new Prop("prop_donut", false, new float[] {0, 0, 0, 0, 0, 0})),
		"sweet_treat", List.of(new Prop("prop_donut", false, new float[] {0, 0, 0, 0, 0, 0})),
		"mic_drop", List.of(new Prop("prop_mic", true, new float[] {0, 0, 0, 0, 0, 0})),
		"skateboard", List.of(new Prop("prop_skateboard", true, new float[] {0, 0, 0, 0, 0, 0}),
			new Prop("prop_bubble_tea", false, new float[] {0, 0, 0, 0, 0, 0})));

	private EmoteProps() {
	}

	/** What the hero's emote holds now, or null for nothing (no emote, no props, or not in the pack). */
	public static @Nullable Held held(HeroAnimator animator) {
		String emote = animator.emote();
		List<Prop> props = emote == null ? null : PROPS.get(emote);
		if (props == null) {
			return null;
		}
		List<Prop> loaded = new ArrayList<>();
		for (Prop prop : props) {
			if (HeroModels.get(prop.model()) != null) {
				loaded.add(prop);
			}
		}
		return loaded.isEmpty() ? null : new Held(loaded,
			new Matrix4f[] {new Matrix4f(animator.emoteHands[0]), new Matrix4f(animator.emoteHands[1])});
	}

	/** Draws the props, the pose stack in her model's space (as her own mesh is drawn). */
	static void submit(Held held, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
		for (Prop prop : held.props()) {
			HeroModels.Loaded loaded = HeroModels.get(prop.model());
			if (loaded == null) {
				continue;
			}
			float[] p = prop.place();
			poseStack.pushPose();
			poseStack.mulPose(held.hands()[prop.kunaiHand() ? 0 : 1]);
			poseStack.rotateDegrees(Axis.XP, p[0]);
			poseStack.rotateDegrees(Axis.YP, p[1]);
			poseStack.rotateDegrees(Axis.ZP, p[2]);
			poseStack.translate(p[3], p[4], p[5]);
			Matrix4f[] skin = Skeleton.skin(loaded.model(), new Skeleton.Pose(loaded.model().nodeNames.length).rest(loaded.model()),
				-1, null);
			HeroModelRenderer.submitModel(loaded, skin, poseStack, collector, light, overlay, Set.of(), false, false);
			poseStack.popPose();
		}
	}
}
