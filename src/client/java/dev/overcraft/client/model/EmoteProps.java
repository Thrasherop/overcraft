package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The props an emote shows (#4: "Their props (the bike, the skateboard...) aren't in yet"), all from the game data
 * (EmoteData, techniques/emotes/emote_reproduction.md): in each of her clips, a prop shows on its hardpoint (a bone of hers and an offset)
 * from its start for its duration, playing its own clip where the data names one, and a projectile one (Mic Drop's
 * mic) falls once its time is up. The models are Overwatch's own, from the player's pack (hero_models/prop_<id>.glb:
 * tools/build_asset_pack.py props).
 */
public final class EmoteProps {
	/** A prop to draw this frame: its model, where it is (in her model's space) and its pose. */
	public record Placed(HeroModels.Loaded loaded, Matrix4f at, Matrix4f[] skin) {
	}

	public record Held(List<Placed> props) {
	}

	public static final RenderStateDataKey<Held> HELD = RenderStateDataKey.create(() -> "overcraft:emote_props");

	/** Gravity for a dropped prop (m/s², ours: the data says it's a projectile, not how it falls). */
	private static final float FALL = 9.8f;
	/** It rests this far above her feet's level once down (ours). */
	private static final float FLOOR = 0.05f;

	private EmoteProps() {
	}

	/** Her emote has props (any in the pack): her own weapons are put away meanwhile. */
	public static boolean holds(String hero, HeroAnimator animator) {
		String emote = animator.emote();
		EmoteData.Emotes data = EmoteData.of(hero);
		if (emote == null || data == null || !data.hasProps(emote)) {
			return false;
		}
		for (var clip : data.emotes().get(emote).values()) {
			for (EmoteData.Spawn spawn : clip) {
				if (HeroModels.get(spawn.file()) != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Her weapon pieces the emote hides now (their first joints, as the renderer hides pieces), or null when the data
	 * has no geosets for this moment: Overwatch's GeoSetFlags, a bit per geoset (EmoteData).
	 */
	public static @Nullable Set<Integer> hiddenPieces(String hero, HeroAnimator animator, SkinnedModel heroModel) {
		String emote = animator.emote();
		EmoteData.Emotes data = EmoteData.of(hero);
		if (emote == null || data == null) {
			return null;
		}
		int mask = data.hiddenGeosets(emote, animator.emoteClip(), animator.emoteAt(), animator.emoteClipLength());
		Skeleton.Pose pose = animator.emotePose();
		if (mask < 0 && (data.pieceGeosets().isEmpty() || pose == null)) {
			return null;
		}
		Set<Integer> hidden = new java.util.HashSet<>();
		for (SkinnedModel.Piece piece : heroModel.pieces) {
			Integer bit = data.pieceGeosets().get(piece.mesh());
			if (bit == null || piece.vertexCount() == 0) {
				continue;
			}
			// no mask now: a hero whose weapon is an entity of its own (Lúcio) puts its pieces away with it (the stow
			// windows); one whose weapons are her own pieces (Kiriko) by whether a hand holds them
			boolean away = mask >= 0 ? (mask & bit) != 0
				: data.weaponEntity() ? data.stowed(emote, animator.emoteClip(), animator.emoteAt())
				: parked(heroModel, pose, heroModel.joints[piece.joint4()[0]]);
			if (away) {
				hidden.add(piece.joint4()[0]);
				// the piece itself too, for one that isn't a held prop (Lúcio's weapon cable, geoset 1, along his arm)
				hidden.add(HeroModelRenderer.PIECE + heroModel.pieces.indexOf(piece));
			}
		}
		return hidden;
	}

	/**
	 * A weapon piece no hand holds: its bone further than this from both wrists (metres, ours). Listen Up's and Salty's
	 * clips set no geosets and park her ofuda's bones a few metres up; Overwatch layers two hand clips over them
	 * (12625, 12638) that aren't read yet (techniques/emotes/kiriko_emotes.md, open), so until then a parked piece isn't drawn.
	 */
	private static final float PARKED = 0.3f;

	private static boolean parked(SkinnedModel model, Skeleton.Pose pose, int node) {
		org.joml.Vector3f at = Skeleton.world(model, pose, node).getTranslation(new org.joml.Vector3f());
		for (boolean left : new boolean[] {true, false}) {
			int wrist = HeroAnimator.UpperBody.wrist(model, left);
			if (wrist >= 0 && Skeleton.world(model, pose, wrist).getTranslation(new org.joml.Vector3f()).distance(at) <= PARKED) {
				return false;
			}
		}
		return true;
	}

	/** What the hero's emote shows now, posed, or null for nothing (no emote, no props, or not in the pack). */
	public static @Nullable Held held(String hero, HeroAnimator animator, SkinnedModel heroModel) {
		String emote = animator.emote();
		EmoteData.Emotes data = EmoteData.of(hero);
		Skeleton.Pose pose = animator.emotePose();
		if (emote == null || data == null || pose == null) {
			return null;
		}
		String clip = animator.emoteClip();
		float at = animator.emoteAt();
		List<EmoteData.Spawn> spawns = data.spawns(emote, clip);
		List<Placed> placed = new ArrayList<>();
		for (int i = 0; i < spawns.size(); i++) {
			EmoteData.Spawn spawn = spawns.get(i);
			HeroModels.Loaded loaded = HeroModels.get(spawn.file());
			if (loaded == null) {
				continue;
			}
			String key = clip + "/" + i;
			float end = spawn.start() + spawn.duration();
			Matrix4f where;
			if (at < spawn.start()) {
				continue;
			} else if (at <= end) {
				where = hardpoint(data, spawn.hardpoint(), heroModel, pose, animator);
				if (where == null) {
					continue;
				}
				if (spawn.falls()) {
					animator.released.put(key, new Matrix4f(where));
				}
			} else if (spawn.falls() && animator.released.get(key) != null) {
				// let go when its time is up: from where it was then, fallen since, down to her feet's level
				Matrix4f from = animator.released.get(key);
				float fall = 0.5f * FALL * (at - end) * (at - end);
				where = new Matrix4f().translation(0, -Math.min(fall, from.m31() - FLOOR), 0).mul(from);
			} else {
				continue;
			}
			SkinnedModel model = loaded.model();
			Skeleton.Pose own = new Skeleton.Pose(model.nodeNames.length).rest(model);
			SkinnedModel.Clip ownClip = spawn.clip() == null ? null : model.clips.get(spawn.clip());
			if (ownClip != null) {
				Skeleton.sample(ownClip, Math.clamp(at - spawn.clipStart(), 0, ownClip.duration()), own);
			}
			placed.add(new Placed(loaded, where, Skeleton.skin(model, own, -1, null)));
		}
		return placed.isEmpty() ? null : new Held(placed);
	}

	/**
	 * A hardpoint's place in her model's space as she's posed: its bone's, then its offset. No hardpoint (Whirlwind's
	 * swirl): the entity's own origin, which her root motion, kept off her, would have moved from (HeroAnimator).
	 */
	private static @Nullable Matrix4f hardpoint(EmoteData.Emotes data, @Nullable String id, SkinnedModel heroModel,
		Skeleton.Pose pose, HeroAnimator animator) {
		if (id == null) {
			return new Matrix4f().translation(new Vector3f(animator.emoteRootShift).negate());
		}
		EmoteData.Hardpoint hp = data.hardpoints().get(id);
		int bone = hp == null ? -1 : heroModel.node(hp.bone());
		if (bone < 0) {
			return null;
		}
		return Skeleton.world(heroModel, pose, bone).translate(hp.t()).rotate(hp.q());
	}

	/** Draws the props, the pose stack in her model's space (as her own mesh is drawn). */
	static void submit(Held held, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
		for (Placed placed : held.props()) {
			poseStack.pushPose();
			poseStack.mulPose(placed.at());
			HeroModelRenderer.submitModel(placed.loaded(), placed.skin(), poseStack, collector, light, overlay, Set.of(), false, false);
			poseStack.popPose();
		}
	}
}
