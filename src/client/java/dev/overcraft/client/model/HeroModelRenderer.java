package dev.overcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.overcraft.hero.Hero;
import dev.overcraft.client.ClientHero;
import dev.overcraft.client.hud.HudShapes;
import dev.overcraft.client.effect.FxRender;
import dev.overcraft.client.effect.Cleansed;
import dev.overcraft.hero.HeroManager;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Pose;
import net.minecraft.core.Direction;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import dev.overcraft.hero.HeroWeaponItem;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Draws a hero's own model in place of the Minecraft player model, for a player in hero mode (holding the hero's
 * weapon: other clients see that too) when the local asset pack has the model. Skinned on the CPU each frame.
 */
public final class HeroModelRenderer {
	/** What to draw this frame, worked out while extracting the render state. */
	/** {@code cull}: back faces left out before upload (not for the select screen's GUI view, whose camera isn't at the origin). */
	public record Draw(HeroModels.Loaded loaded, Matrix4f[] skin, float climbShift, Set<Integer> hidden, boolean cull, boolean angryFace) {
	}

	public static final RenderStateDataKey<Draw> DRAW = RenderStateDataKey.create(() -> "overcraft:hero_draw");
	/** The camera's inside her (the emote camera's first moments): she isn't drawn, not even as Minecraft's player. */
	private static final RenderStateDataKey<Boolean> UNSEEN = RenderStateDataKey.create(() -> "overcraft:hero_unseen");
	/** Sombra's stealth: how far phased out (0-1) and the time, for her phased-out look (ClientSombra.phase, #87). */
	private static final RenderStateDataKey<float[]> STEALTH = RenderStateDataKey.create(() -> "overcraft:hero_stealth");

	/**
	 * Her body's shadow this frame (the owner, 2026-10-27: "casting a body shaped shadow would be good"): the direction
	 * toward the light (the world's axes), how dark it is (0-1), the ground she stands on (metres about her feet), and the
	 * ground around her: the top of each block column (the same way, NaN where there's none near her height), so the
	 * shadow lies on steps and stops at ledges.
	 */
	public record Shadow(Vector3f toLight, float strength, float plane, float[] ground, int groundX, int groundZ, double feetX,
						 double feetY, double feetZ) {
		/** The ground's height under this point (metres about her feet), NaN for none. */
		float groundAt(float x, float z) {
			int cx = Mth.floor(feetX + x) - groundX;
			int cz = Mth.floor(feetZ + z) - groundZ;
			return cx < 0 || cz < 0 || cx >= GROUND_SIZE || cz >= GROUND_SIZE ? Float.NaN : ground[cz * GROUND_SIZE + cx];
		}
	}

	private static final RenderStateDataKey<Shadow> SHADOW = RenderStateDataKey.create(() -> "overcraft:hero_shadow");
	/** How far below her feet the ground may be for her shadow to fall on it (blocks, ours); it fades out down to there. */
	private static final float SHADOW_REACH = 4;
	/** Her shadow's darkness in full sun (ours: about Minecraft's blob shadow's at its centre). */
	private static final float SHADOW_ALPHA = 0.45f;
	/** The lowest the light comes from (degrees above the horizon, ours): a low sun stretched her across the ground. */
	private static final float SHADOW_MIN_ELEVATION = 40;
	/** The shadow's two passes come after everything drawn at Minecraft's default order (0), marks first. */
	private static final int SHADOW_MARK_ORDER = 1;
	private static final int SHADOW_DARKEN_ORDER = 2;
	/** The block columns her shadow may fall on: this many either side of her (her shadow reaches about 2.2 m at 40°). */
	private static final int GROUND_RADIUS = 4;
	private static final int GROUND_SIZE = GROUND_RADIUS * 2 + 1;
	/** How far above and below her feet's ground her shadow still finds ground (blocks, ours): a step up, a drop down. */
	private static final float GROUND_UP = 1.5f, GROUND_DOWN = 2.5f;

	/** Minecraft's own round shadow isn't drawn under her (EntityShadowMixin): this state has her body's shadow. */
	public static boolean drawsHero(net.minecraft.client.renderer.entity.state.EntityRenderState state) {
		return state.getData(DRAW) != null && state.getData(SHADOW) != null;
	}
	/** Prone (crawling, swimming), how far back along her body she's drawn and how far off the floor (metres: ours). */
	private static final float PRONE_BACK = 0.95f;
	private static final float PRONE_LIFT = 0.25f;
	/** Lying in bed, how far she's lifted so her back rests on the mattress rather than in it (metres: ours). */
	private static final float SLEEP_LIFT = 0.12f;
	/** Packed light for full brightness (block and sky light 15). */
	private static final int FULL_BRIGHT = 0xF000F0;
	/**
	 * The view model's least light, as block light (0-15). Overwatch's arms stay readable on night maps; at Minecraft's
	 * night light they went nearly black and purple, a quarter of the screen merging into dark grass (the fourth
	 * critique). Ours.
	 */
	private static final int VIEW_MODEL_MIN_LIGHT = 11;
	/** How white an entity flashes (Protection Suzu's cleanse: client/effect/Cleansed). */
	public static final RenderStateDataKey<Float> WHITE = RenderStateDataKey.create(() -> "overcraft:white");
	/** The hero each entity was last drawn as (by entity id), while it's in hero mode or dying. */
	private static final Map<Integer, Hero> LAST_HERO = new HashMap<>();

	private HeroModelRenderer() {
	}

	/** The hero the select screen shows the local player as, while it draws them; null otherwise. */
	private static @Nullable Hero preview;

	/** The select screen draws the player as this hero (null: as they are). */
	public static void preview(@Nullable Hero hero) {
		preview = hero;
	}

	/** When the select screen's showcase started (Util.getMillis): its intro plays from here (#29). */
	private static long showcaseSince = -1;

	/** The select screen opened, or its hero changed: the showcase intro plays again. */
	public static void restartShowcase() {
		showcaseSince = Util.getMillis();
	}

	/** Forgets who was which hero (leaving a world). */
	public static void clear() {
		dev.overcraft.client.ClientSombra.clear();
		LAST_HERO.clear();
	}

	/** While extracting a player's render state: a hero in hero mode with a model gets a {@link Draw}. */
	public static void extract(LivingEntity entity, LivingEntityRenderState state, float partialTicks) {
		state.setData(DRAW, null);
		state.setData(UNSEEN, entity == Minecraft.getInstance().player
			&& (dev.overcraft.client.comms.EmoteCamera.inside() || dev.overcraft.client.AbilityCamera.inside()));
		Hero hero;
		String chosen = entity.getAttached(HeroManager.HERO);
		if (preview != null && entity == Minecraft.getInstance().player) {
			hero = preview;
		} else if (entity.getMainHandItem().getItem() instanceof HeroWeaponItem weapon) {
			hero = weapon.hero;
			LAST_HERO.put(entity.getId(), hero);
		} else if (chosen != null && Hero.byId(chosen) != null) {
			// out of hero mode (Minecraft mode) a player still looks like their hero (owner)
			hero = Hero.byId(chosen);
			LAST_HERO.put(entity.getId(), hero);
		} else if (entity.isDeadOrDying() && LAST_HERO.containsKey(entity.getId())) {
			// the weapon is gone the moment a hero dies (Overcraft removes it): keep the hero for the death animation
			hero = LAST_HERO.get(entity.getId());
		} else {
			LAST_HERO.remove(entity.getId());
			return;
		}
		HeroModels.Loaded loaded = HeroModels.get(hero);
		if (loaded == null) {
			return;
		}
		float now = (entity.level().getGameTime() + partialTicks) / 20f;
		HeroAnimator animator = HeroAnimator.of(entity);
		// the body faces her aim (HeroAnimator.facing); the head's turn is then measured from there
		float facing = animator.facing(entity, partialTicks) + (DEV_AIM != null && entity == Minecraft.getInstance().player ? DEV_AIM[1] : 0);
		state.yRot = Mth.wrapDegrees(state.yRot + state.bodyRot - facing);
		state.bodyRot = facing;
		long t0 = System.nanoTime();
		// in a menu's preview the head looks straight ahead: Minecraft's preview sets the head angles only after this
		// pose, so it took the player's own pitch in the world (looking down, the head bent over and the back of the
		// hair opened up: #29, #30)
		boolean previewing = preview != null && entity == Minecraft.getInstance().player;
		// on the select screen, Overwatch's hero select animation when the model has it (#29)
		float showcaseAt = showcaseSince < 0 ? 1000 : (Util.getMillis() - showcaseSince) / 1000f;
		Skeleton.Pose showcase = previewing ? HeroAnimator.showcasePose(loaded.model(), showcaseAt) : null;
		Matrix4f[] skin = showcase != null ? Skeleton.skin(loaded.model(), showcase, -1, null) : null;
		if (skin == null) {
			skin = animator.pose(loaded.model(), entity, now, previewing ? 0 : DEV_AIM != null ? 0 : state.yRot,
				previewing ? 0 : DEV_AIM != null && entity == Minecraft.getInstance().player ? DEV_AIM[0] : state.xRot);
		}
		Timing.pose += System.nanoTime() - t0;
		// her weapons in an emote: as its GeoSetFlags hide them (the data); where the data has none, an emote with props
		// of its own puts them away (a donut in the hand that held the ofuda)
		Set<Integer> hidden = hiddenProps(loaded.model(), animator.action());
		Set<Integer> byEmote = previewing ? null : EmoteProps.hiddenPieces(hero.id, animator, loaded.model());
		if (byEmote != null) {
			hidden = new java.util.HashSet<>(hidden);
			hidden.addAll(byEmote);
		} else if (!previewing && EmoteProps.holds(hero.id, animator)) {
			hidden = allProps(loaded.model());
		}
		state.setData(DRAW, new Draw(loaded, skin, animator.climbShift(), hidden, preview == null,
			hero == Hero.JUNO && preview == null && dev.overcraft.client.ClientJuno.faceAngry(entity)));
		float phase = hero == Hero.SOMBRA && !previewing ? dev.overcraft.client.ClientSombra.phase(entity, now) : 0;
		state.setData(STEALTH, phase > 0 ? new float[] {phase, now} : null);
		state.setData(SHADOW, previewing ? null : shadow(entity, partialTicks));
		state.setData(EmoteProps.HELD, previewing ? null : EmoteProps.held(hero.id, animator, loaded.model()));
		// his weapon when it's a model of its own (Lúcio's), on its hardpoint; put away in an emote, as Overwatch's are
		boolean weaponShown = !previewing && (!animator.emoting() || HeroWeapon.inEmote(hero, animator));
		state.setData(WEAPON, weaponShown ? HeroWeapon.place(hero, animator, loaded.model(), false, now)
			: showcase != null ? HeroWeapon.placeShowcase(hero, loaded.model(), showcase, HeroAnimator.showcaseIntro(loaded.model(), showcaseAt))
			// no showcase clips in the pack (Brigitte's C06C / C07D / C0B8 aren't extracted): she stands in her idle, and
			// her showcase entity holds the flail in her hand all the same (3A90: 23D4 on 000A, brigitte assets.md)
			: previewing ? HeroWeapon.place(hero, animator, loaded.model(), false, now)
			: null);
		// Brigitte's whip head where BrigitteFx flies it, brought into her model's space (her feet, her facing, her scale)
		net.minecraft.world.phys.Vec3 feet = entity.getPosition(partialTicks);
		float turnY = (float) Math.toRadians(-facing);
		net.minecraft.world.phys.Vec3 whip = hero == Hero.BRIGITTE && !previewing
			? dev.overcraft.client.effect.BrigitteFx.whipHead(entity.getId(), entity.getEyePosition(partialTicks)) : null;
		animator.whipHead = whip == null ? null
			: new Vector3f((float) (whip.x - feet.x), (float) (whip.y - feet.y), (float) (whip.z - feet.z)).rotateY(-turnY).div(state.scale);
		// the extras decide for themselves in an emote (Brigitte's folded shield stays when the flail is put away)
		state.setData(SIDEARM, !previewing || showcase == null
			? new EmoteProps.Held(HeroWeapon.extras(hero, animator, loaded.model(), false, now)) : null);
		if (hero == Hero.BRIGITTE && !previewing && animator.chainFrom != null && animator.chainTo != null) {
			// the flail's chain, from the handle to the flung head, in the world (BrigitteFx draws it)
			Vector3f a = new Vector3f(animator.chainFrom).mul(state.scale).rotateY(turnY);
			Vector3f b = new Vector3f(animator.chainTo).mul(state.scale).rotateY(turnY);
			dev.overcraft.client.effect.BrigitteFx.chain(entity, feet.add(a.x, a.y, a.z), feet.add(b.x, b.y, b.z));
		}
		if (hero == Hero.MERCY && !previewing && entity.isAlive()) {
			// her staff's head (where her beams start) and her hardpoints (chest, hand, wings: MercyFx), as posed, in the world
			Skeleton.Pose pose = animator.emoting() && animator.emotePose() != null ? animator.emotePose() : animator.lastPose();
			net.minecraft.world.phys.Vec3 base = entity.getPosition(partialTicks);
			float turn = (float) Math.toRadians(-facing);
			Vector3f head = weaponShown && !animator.blaster() ? HeroWeapon.muzzle(hero, animator, loaded.model(), false, now) : null;
			net.minecraft.world.phys.Vec3 staff = null;
			if (head != null) {
				Vector3f p = head.mul(state.scale).rotateY(turn);
				staff = base.add(p.x, p.y, p.z);
			}
			String[] names = dev.overcraft.client.effect.MercyFx.POINTS;
			net.minecraft.world.phys.Vec3[] points = new net.minecraft.world.phys.Vec3[names.length];
			for (int i = 0; i < names.length && pose != null; i++) {
				int node = loaded.model().node("hardpoint_" + names[i]);
				if (node < 0) {
					points = new net.minecraft.world.phys.Vec3[0];
					break;
				}
				Vector3f p = Skeleton.world(loaded.model(), pose, node).getTranslation(new Vector3f()).mul(state.scale).rotateY(turn);
				points[i] = base.add(p.x, p.y, p.z);
			}
			dev.overcraft.client.effect.MercyFx.posed(entity, staff, pose == null ? new net.minecraft.world.phys.Vec3[0] : points);
		}
		if (hero == Hero.JUNO && !previewing && entity.isAlive()) {
			// her muzzle, torpedo pods, feet and casting hand, as posed, in the world (JunoFx.POINTS)
			Skeleton.Pose pose = animator.emoting() && animator.emotePose() != null ? animator.emotePose() : animator.lastPose();
			String[] names = dev.overcraft.client.effect.JunoFx.POINTS;
			net.minecraft.world.phys.Vec3[] points = new net.minecraft.world.phys.Vec3[names.length];
			for (int i = 0; i < names.length && pose != null; i++) {
				points[i] = bone(loaded.model(), pose, names[i], entity.getPosition(partialTicks), facing, state.scale);
			}
			if (pose != null) {
				dev.overcraft.client.effect.JunoFx.posed(entity, points);
			}
		}
		if (hero == Hero.SOJOURN && !previewing && entity.isAlive()) {
			// her Railgun's muzzle (hp 0006) and energy glow point (hp 007C) as held, her body's hardpoints (SojournFx.POINTS),
			// as posed, in the world
			Skeleton.Pose pose = animator.emoting() && animator.emotePose() != null ? animator.emotePose() : animator.lastPose();
			if (pose != null) {
				net.minecraft.world.phys.Vec3 base = entity.getPosition(partialTicks);
				float turn = (float) Math.toRadians(-facing);
				String[] names = dev.overcraft.client.effect.SojournFx.POINTS;
				net.minecraft.world.phys.Vec3[] points = new net.minecraft.world.phys.Vec3[names.length];
				for (int i = 0; i < names.length; i++) {
					if (i >= dev.overcraft.client.effect.SojournFx.GUN_POINTS) {
						points[i] = bone(loaded.model(), pose, names[i], base, facing, state.scale);
						continue;
					}
					Vector3f gun = weaponShown ? HeroWeapon.point(hero, animator, loaded.model(), false, now, names[i]) : null;
					if (gun != null) {
						gun.mul(state.scale).rotateY(turn);
						points[i] = base.add(gun.x, gun.y, gun.z);
					}
				}
				dev.overcraft.client.effect.SojournFx.posed(entity, points);
			}
		}
		if (hero == Hero.SIGMA && !previewing && entity.isAlive()) {
			// his hand spheres' cores and muzzle as held (SigmaFx.POINTS: bone_0442 / bone_0444 / hp 007C on 106B4) and his body's
			// hardpoints, as posed, in the world
			Skeleton.Pose pose = animator.emoting() && animator.emotePose() != null ? animator.emotePose() : animator.lastPose();
			if (pose != null) {
				net.minecraft.world.phys.Vec3 base = entity.getPosition(partialTicks);
				float turn = (float) Math.toRadians(-facing);
				String[] names = dev.overcraft.client.effect.SigmaFx.POINTS;
				net.minecraft.world.phys.Vec3[] points = new net.minecraft.world.phys.Vec3[names.length];
				for (int i = 0; i < names.length; i++) {
					if (i >= dev.overcraft.client.effect.SigmaFx.GUN_POINTS) {
						points[i] = bone(loaded.model(), pose, names[i], base, facing, state.scale);
						continue;
					}
					Vector3f held = weaponShown ? HeroWeapon.point(hero, animator, loaded.model(), false, now, names[i]) : null;
					if (held != null) {
						held.mul(state.scale).rotateY(turn);
						points[i] = base.add(held.x, held.y, held.z);
					}
				}
				dev.overcraft.client.effect.SigmaFx.posed(entity, points);
			}
		}
		if (hero == Hero.JETPACK_CAT && !previewing && entity.isAlive()) {
			// her jetpack's flames and her paws' points, as posed, in the world (JetpackCatFx)
			Skeleton.Pose pose = animator.emoting() && animator.emotePose() != null ? animator.emotePose() : animator.lastPose();
			var points = pose == null ? null : jetpackCatPoints(loaded.model(), pose, entity.getPosition(partialTicks), facing,
				state.scale);
			if (points != null) {
				dev.overcraft.client.effect.JetpackCatFx.jetpack(entity, points[0], points[1]);
			}
		}
		if (hero == Hero.LUCIO && !previewing && entity.isAlive() && entity instanceof net.minecraft.world.entity.player.Player player) {
			// his song on his skates (LucioFx): from his foot bones as posed, in the world
			Integer song = player.getAttached(HeroManager.SONG);
			Skeleton.Pose pose = animator.emoting() && animator.emotePose() != null ? animator.emotePose() : animator.lastPose();
			if (song != null && pose != null) {
				net.minecraft.world.phys.Vec3 base = entity.getPosition(partialTicks);
				net.minecraft.world.phys.Vec3 left = bone(loaded.model(), pose, "Foot_L", base, facing, state.scale);
				net.minecraft.world.phys.Vec3 right = bone(loaded.model(), pose, "Foot_R", base, facing, state.scale);
				if (left != null && right != null) {
					dev.overcraft.client.effect.LucioFx.skates(entity.getId(), left, right, song, now,
						dev.overcraft.client.effect.KirikoFx.enemyOf(entity), animator.playing().startsWith("wallride"),
						dev.overcraft.hero.OwMovement.wallRideNormal(player));
				}
				if (song == (2 | 4)) {
					// amped Speed Boost shows on his allies in reach (and him): speed lines (4ADD)
					float dt = dev.overcraft.client.effect.Fx.since(-(entity.getId() * 8 + 6) - 1);
					for (LivingEntity ally : entity.level().getEntitiesOfClass(LivingEntity.class,
						entity.getBoundingBox().inflate(dev.overcraft.lucio.LucioStats.SONG_RADIUS))) {
						boolean firstPersonSelf = ally == Minecraft.getInstance().player
							&& Minecraft.getInstance().options.getCameraType().isFirstPerson();
						if (!firstPersonSelf && dev.overcraft.hero.Allies.isSelfOrAlly(player, ally)
							&& ally.distanceTo(entity) <= dev.overcraft.lucio.LucioStats.SONG_RADIUS) {
							dev.overcraft.client.effect.LucioFx.speedLines(ally, dt);
						}
					}
				}
			}
		}
	}

	/**
	 * Jetpack Cat's points, from her third-person look's own hardpoint table (look 2673's 76 entries, not the model's 24:
	 * E:\Overwatch Archive\research\jetpack_cat\gaps.md 2, H): the jetpack's nozzles 0075 / 0077 (high, outer) and
	 * 0078 / 007A (low, inner), then her paws 000A / 000B, then her jetpack arm's tip 0283 (bone 3E67, where Lifeline's
	 * hook hangs and its rope starts: assets2.md 2). Each a bone, an offset in its frame and a turn (x, y, z, w).
	 */
	private record Hardpoint(String bone, Vector3f offset, org.joml.Quaternionf turn) {
	}

	private static final Hardpoint[] JETPACK_CAT_POINTS = {
		new Hardpoint("bone_0124", new Vector3f(0.1778f, 0, 0.0007f), new org.joml.Quaternionf()),
		new Hardpoint("bone_0125", new Vector3f(-0.1796f, 0, -0.0004f), new org.joml.Quaternionf(1, 0, 0, 0)),
		new Hardpoint("bone_0126", new Vector3f(0.08f, 0, -0.1f), new org.joml.Quaternionf(0, -1, 0, 0)),
		new Hardpoint("bone_0127", new Vector3f(-0.08f, 0, 0.1f), new org.joml.Quaternionf(0, 0, -1, 0)),
		new Hardpoint("bone_0170", new Vector3f(0, 0, -0.0359f), new org.joml.Quaternionf(1, 0, 0, 0)),
		new Hardpoint("bone_016F", new Vector3f(0, 0, 0.031f), new org.joml.Quaternionf()),
		new Hardpoint("bone_3E67", new Vector3f(), new org.joml.Quaternionf()),
	};

	/**
	 * Her points as posed, in the world, with each one's down axis (the hardpoint's -Y: at rest the nozzles' frames line
	 * up with the model's, so their flames point down her back), handed to her effects every frame. Null if a bone is
	 * missing.
	 */
	private static net.minecraft.world.phys.Vec3 @Nullable [][] jetpackCatPoints(SkinnedModel model, Skeleton.Pose pose,
		net.minecraft.world.phys.Vec3 base, float facing, float scale) {
		net.minecraft.world.phys.Vec3[] at = new net.minecraft.world.phys.Vec3[JETPACK_CAT_POINTS.length];
		net.minecraft.world.phys.Vec3[] down = new net.minecraft.world.phys.Vec3[JETPACK_CAT_POINTS.length];
		float turn = (float) Math.toRadians(-facing);
		for (int i = 0; i < JETPACK_CAT_POINTS.length; i++) {
			Hardpoint h = JETPACK_CAT_POINTS[i];
			int node = model.node(h.bone());
			if (node < 0) {
				return null;
			}
			Matrix4f m = new Matrix4f(Skeleton.world(model, pose, node)).translate(h.offset()).rotate(h.turn());
			Vector3f p = m.getTranslation(new Vector3f()).mul(scale).rotateY(turn);
			Vector3f d = m.transformDirection(new Vector3f(0, -1, 0)).normalize().rotateY(turn);
			at[i] = base.add(p.x, p.y, p.z);
			down[i] = new net.minecraft.world.phys.Vec3(d.x, d.y, d.z);
		}
		return new net.minecraft.world.phys.Vec3[][] {at, down};
	}

	/** Where a bone of the posed model is in the world: turned by her facing and scaled, as submit draws her. */
	private static net.minecraft.world.phys.@Nullable Vec3 bone(SkinnedModel model, Skeleton.Pose pose, String name,
		net.minecraft.world.phys.Vec3 base, float facing, float scale) {
		int node = model.node(name);
		if (node < 0) {
			return null;
		}
		Vector3f at = Skeleton.world(model, pose, node).getTranslation(new Vector3f()).mul(scale)
			.rotateY((float) Math.toRadians(-facing));
		return base.add(at.x, at.y, at.z);
	}

	private static final RenderStateDataKey<EmoteProps.Placed> WEAPON = RenderStateDataKey.create(() -> "overcraft:hero_weapon");
	private static final RenderStateDataKey<EmoteProps.Held> SIDEARM = RenderStateDataKey.create(() -> "overcraft:hero_sidearm");

	/**
	 * Where her shadow falls this frame, or null for none (shadows off, invisible, asleep, nothing under her). The light
	 * is the sun's, or the moon's at night, no lower than SHADOW_MIN_ELEVATION; out of the sky's light (indoors, under
	 * trees) it comes from straight above, a fainter contact shadow; in the dark there's none, as Minecraft's own shadow.
	 * Her shadow lies flat on the ground under her feet, fading as she rises off it.
	 */
	private static @Nullable Shadow shadow(LivingEntity entity, float partialTicks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !mc.options.entityShadows().get() || entity.isInvisible() || entity.isSleeping()) {
			return null;
		}
		Vec3 feet = entity.getPosition(partialTicks);
		net.minecraft.world.phys.BlockHitResult ground = mc.level.clip(new net.minecraft.world.level.ClipContext(
			feet.add(0, 0.2, 0), feet.add(0, -SHADOW_REACH, 0), net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE, entity));
		if (ground.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
			return null;
		}
		float h = (float) (ground.getLocation().y - feet.y);
		net.minecraft.core.BlockPos above = net.minecraft.core.BlockPos.containing(feet.x, ground.getLocation().y + 0.5, feet.z);
		int brightness = mc.level.getMaxLocalRawBrightness(above);
		if (brightness <= 3) {
			return null;
		}
		// toward the sun (Minecraft's sky turns the sun about the X axis: overhead at noon, rising in the east)
		float sun = mc.gameRenderer.mainCamera().attributeProbe()
			.getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, partialTicks) * Mth.DEG_TO_RAD;
		Vector3f toLight = new Vector3f(-Mth.sin(sun), Mth.cos(sun), 0);
		boolean day = toLight.y > 0;
		if (!day) {
			// the moon is opposite the sun
			toLight.negate();
		}
		float minY = Mth.sin(SHADOW_MIN_ELEVATION * Mth.DEG_TO_RAD);
		if (toLight.y < minY) {
			float across = Mth.sqrt(toLight.x * toLight.x + toLight.z * toLight.z);
			float keep = across > 1.0E-4f ? Mth.cos(SHADOW_MIN_ELEVATION * Mth.DEG_TO_RAD) / across : 0;
			toLight.set(toLight.x * keep, minY, toLight.z * keep);
		}
		// how much the sky lights her: the sun's direction that much, the rest from straight above
		float sky = mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, above) / 15f;
		float sunlit = sky * (day ? 1 - mc.level.getRainLevel(partialTicks) * 0.6f : 0.35f);
		toLight.lerp(new Vector3f(0, 1, 0), 1 - sunlit).normalize();
		float strength = SHADOW_ALPHA * (0.6f + 0.4f * sunlit) * Math.min(1, brightness / 12f) * (1 + h / SHADOW_REACH);
		if (strength <= 0.01f) {
			return null;
		}
		// the ground around her: each column's highest top from a step above her feet's ground to a drop below it
		int ox = Mth.floor(feet.x) - GROUND_RADIUS;
		int oz = Mth.floor(feet.z) - GROUND_RADIUS;
		float[] heights = new float[GROUND_SIZE * GROUND_SIZE];
		double base = ground.getLocation().y;
		net.minecraft.core.BlockPos.MutableBlockPos at = new net.minecraft.core.BlockPos.MutableBlockPos();
		for (int cz = 0; cz < GROUND_SIZE; cz++) {
			for (int cx = 0; cx < GROUND_SIZE; cx++) {
				float top = Float.NaN;
				for (int y = Mth.floor(base + GROUND_UP); y >= Mth.floor(base - GROUND_DOWN); y--) {
					at.set(ox + cx, y, oz + cz);
					net.minecraft.world.phys.shapes.VoxelShape shape = mc.level.getBlockState(at).getCollisionShape(mc.level, at);
					if (!shape.isEmpty()) {
						double surface = y + shape.max(Direction.Axis.Y);
						if (surface <= base + GROUND_UP) {
							top = (float) (surface - feet.y);
							break;
						}
					}
				}
				heights[cz * GROUND_SIZE + cx] = top;
			}
		}
		return new Shadow(toLight, strength, h, heights, ox, oz, feet.x, feet.y, feet.z);
	}

	private static float white(LivingEntityRenderState state) {
		Float white = state.getData(WHITE);
		return white == null ? 0 : white;
	}

	/** How much of a stealthed hero shows to those who see her, and to her own eyes, fully phased out (ours). */
	private static final float SEE_THROUGH = 0.38f, SEE_THROUGH_OWN = 0.6f;
	/** Her phased-out tint (lilac, over her own colours), and her stealth's purple over it (A06C: M). */
	private static final int STEALTH_TINT = 0xC8B4FF, STEALTH_GLOW = 0x9A55FF;
	/** 1AD9's wipe edge row, by the fade (sombra-effects: #6D0000 #CE0000 #F6502B #00DFB6; read by age: M). */
	private static final int[] WIPE_EDGE = {0x6D0000, 0xCE0000, 0xF6502B, 0x00DFB6};
	private static final Identifier WIPE_MASK = dev.overcraft.Overcraft.id("hud/sombra_wipe");
	/** The wipe's mask is built: null not yet, false without the pack's frames. */
	private static @Nullable Boolean wipeMade;

	/**
	 * A7BF's 6 frames as one mask for Minecraft's dissolve shader (RenderTypes.entityCutoutDissolve): each frame whites out
	 * more of it, so a texel's alpha is when it goes (1 for the first frame's, 1/6 for the last's, near 0 for those never
	 * white), against the vertex alpha falling 1 -> 0. The data's frames, on her own UVs.
	 */
	private static boolean wipeMask() {
		if (wipeMade == null) {
			wipeMade = false;
			try {
				var resources = Minecraft.getInstance().getResourceManager();
				com.mojang.blaze3d.platform.NativeImage out = null;
				for (int frame = 5; frame >= 0; frame--) {
					try (java.io.InputStream in = resources.open(dev.overcraft.Overcraft.id("textures/effect/sombra_dissolve_" + frame + ".png"));
						 com.mojang.blaze3d.platform.NativeImage image = com.mojang.blaze3d.platform.NativeImage.read(in)) {
						if (out == null) {
							out = new com.mojang.blaze3d.platform.NativeImage(image.getWidth(), image.getHeight(), true);
							for (int y = 0; y < out.getHeight(); y++) {
								for (int x = 0; x < out.getWidth(); x++) {
									out.setPixel(x, y, 8 << 24 | 0xFFFFFF);
								}
							}
						}
						int alpha = Math.round(255 * (6 - frame) / 6f);
						for (int y = 0; y < out.getHeight(); y++) {
							for (int x = 0; x < out.getWidth(); x++) {
								if ((image.getPixel(x, y) >> 16 & 0xFF) > 127) {
									out.setPixel(x, y, alpha << 24 | 0xFFFFFF);
								}
							}
						}
					}
				}
				if (out != null) {
					Minecraft.getInstance().getTextureManager().register(WIPE_MASK, new HeroTexture(WIPE_MASK, out, net.minecraft.client.renderer.texture.MipmapStrategy.MEAN));
					wipeMade = true;
				}
			} catch (java.io.IOException e) {
				dev.overcraft.Overcraft.LOG.warn("Sombra's stealth wipe: {}", e.toString());
			}
		}
		return wipeMade;
	}

	/**
	 * Sombra stealthed (#87), as she shows to those who still see her and in her own view. The fades are the data's: 1AD9 / 1AD8
	 * wipe her solid self away along A7BF's diagonal frames, with its edge row's colours lit on the frame reached (phase 0 solid, 1
	 * phased out). What's held has no art in the data (16EA is empty, the invisibility a renderer state, STU_33A46033: M): ours, one
	 * see-through lilac for all of her, a slow pulse, and A7BF's frames turning over her as faint digital glints.
	 */
	static void submitStealth(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		Set<Integer> hidden, float phase, float time, int light, int overlay, boolean own) {
		phase = Math.clamp(phase, 0, 1);
		boolean wipe = wipeMask();
		float pulse = 0.5f + 0.5f * (float) Math.sin(time * Mth.TWO_PI * 0.7f);
		float opacity = (own ? SEE_THROUGH_OWN : SEE_THROUGH) * (0.92f + 0.16f * pulse) * phase;
		int phased = Math.clamp(Math.round(opacity * 255), 0, 255) << 24 | STEALTH_TINT;
		// what's left of her solid self, as vertex alpha against the wipe's mask (or plainly fading without the pack)
		int solid = Math.clamp(Math.round((1 - phase) * 255), 0, 255) << 24 | 0xFFFFFF;
		float fading = phase;
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			Identifier texture = piece.image() >= 0 ? loaded.textures()[piece.image()] : null;
			boolean prop = PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (texture == null || isEyeShadow(loaded, piece) || hiddenNow(loaded.model(), piece, hidden, prop)) {
				continue;
			}
			if (fading < 1) {
				collector.submitCustomGeometry(poseStack, wipe ? RenderTypes.entityCutoutDissolve(texture, WIPE_MASK) : RenderTypes.entityTranslucent(texture),
					(pose, buffer) -> emit(piece, skin, pose, buffer, light, overlay, prop, solid, true, false));
			}
			collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(texture),
				(pose, buffer) -> emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, prop, phased, true, false));
		}
		if (wipe) {
			// mid-fade the edge: the frame reached, lit in the row's colour by the fade; held, the frames turning slowly as glints
			float mid = 4 * phase * (1 - phase);
			int frame = phase < 1 ? Math.clamp((int) (phase * 6), 0, 5) : Math.floorMod((int) Math.floor(time * 5), 6);
			float along = phase * 3;
			int from = Math.clamp((int) along, 0, 2);
			int rgb = phase < 1 ? net.minecraft.util.ARGB.srgbLerp(along - from, WIPE_EDGE[from], WIPE_EDGE[from + 1]) & 0xFFFFFF : STEALTH_GLOW;
			float strength = phase < 1 ? 0.85f * mid : 0.1f;
			if (strength > 0.01f) {
				submitField(loaded, skin, poseStack, collector, Math.round(strength * 255) << 24 | rgb, dev.overcraft.Overcraft.id("textures/effect/sombra_dissolve_" + frame + ".png"));
			}
		}
		submitGlow(loaded, skin, poseStack, collector, hidden, 0.12f * phase, true, STEALTH_GLOW);
	}

	/** In place of LivingEntityRenderer.submit: false when there's nothing of ours to draw. */
	public static boolean submit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		Draw draw = state.getData(DRAW);
		if (draw == null || state.isInvisibleToPlayer) {
			// (hidden from this viewer: nothing of ours, as Minecraft draws nothing of an invisible player)
			return draw != null;
		}
		// invisible but seen (a stealthed Sombra by her own side, or an enemy within 4 m: Stealth.seenBy): see-through
		boolean seeThrough = state.isInvisible;
		if (Boolean.TRUE.equals(state.getData(UNSEEN))) {
			return true;
		}
		int light = state.lightCoords;
		int overlay = LivingEntityRenderer.getOverlayCoords(state, white(state) * 0.6f);
		// where she stands, before her turn and pose: her shadow is laid on the ground from here
		Matrix4f feet = new Matrix4f(poseStack.last().pose());
		poseStack.pushPose();
		Direction bed = state.hasPose(Pose.SLEEPING) ? state.bedOrientation : null;
		if (bed != null) {
			// asleep: on her back, head on the pillow, feet toward the bed's foot, as Minecraft lays a sleeping player
			// (LivingEntityRenderer: the origin moved eyeHeight - 0.1 toward the foot). She stood upright on the bed
			// (found playing)
			float headOffset = state.eyeHeight - 0.1f;
			poseStack.translate(-bed.getStepX() * headOffset, SLEEP_LIFT, -bed.getStepZ() * headOffset);
			// her up (+Y) toward the pillow, her front (+Z) to the sky, her side (+X = Y x Z) across the bed
			Vector3f head = new Vector3f(bed.getStepX(), 0, bed.getStepZ());
			Vector3f side = new Vector3f(head).cross(0, 1, 0);
			poseStack.mulPose(new Matrix4f().set(new Matrix3f(side, head, new Vector3f(0, 1, 0))));
		} else {
			// the model faces +Z; Minecraft's yaw 0 faces +Z (south) and turns the other way
			poseStack.rotateDegrees(Axis.YP, -state.bodyRot);
			poseStack.translate(0, 0, -draw.climbShift());
			if (state instanceof AvatarRenderState avatar) {
				// gliding, swimming, crawling: pitched forward about her feet as Minecraft pitches a player
				// (AvatarRenderer.setupRotations); upright, she crawled standing through a one-high gap, the camera in
				// her legs, and swam and flew standing (found playing)
				float pitch = 0;
				if (avatar.isFallFlying) {
					pitch = avatar.fallFlyingScale() * (90 + avatar.xRot);
				} else if (avatar.swimAmount > 0) {
					pitch = Mth.lerp(avatar.swimAmount, 0, avatar.isInWater ? 90 + avatar.xRot : 90);
				}
				if (pitch != 0) {
					poseStack.rotateDegrees(Axis.XP, pitch);
					if (avatar.isVisuallySwimming) {
						// lying over her box, not ahead of it: back along her body, lifted off the floor
						poseStack.translate(0, -PRONE_BACK, -PRONE_LIFT);
					}
				}
			}
		}
		float scale = state.scale;
		poseStack.scale(scale, scale, scale);
		float[] stealth = state.getData(STEALTH);
		if (seeThrough || stealth != null) {
			// Overwatch's stealthed Sombra to those who see her (her side, an enemy within 4 m, herself from behind): phased
			// out, her weapon too; fading there over the stealth's 0.75 s and back over its 0.3 s (#87)
			float phase = stealth != null ? stealth[0] : 1;
			float time = stealth != null ? stealth[1] : state.ageInTicks / 20f;
			submitStealth(draw.loaded(), draw.skin(), poseStack, collector, draw.hidden(), phase, time, light, overlay, false);
			EmoteProps.Placed held = state.getData(WEAPON);
			if (held != null) {
				poseStack.pushPose();
				poseStack.mulPose(held.at());
				submitStealth(held.loaded(), held.skin(), poseStack, collector, Set.of(), phase, time, light, overlay, false);
				poseStack.popPose();
			}
			// her magazine goes with the gun (critique_parts C9)
			EmoteProps.Held heldParts = state.getData(SIDEARM);
			if (heldParts != null) {
				for (EmoteProps.Placed part : heldParts.props()) {
					poseStack.pushPose();
					poseStack.mulPose(part.at());
					submitStealth(part.loaded(), part.skin(), poseStack, collector, Set.of(), phase, time, light, overlay, false);
					poseStack.popPose();
				}
			}
			poseStack.popPose();
			return true;
		}
		altGlow = draw.angryFace();
		submitModel(draw.loaded(), draw.skin(), poseStack, collector, light, overlay, draw.hidden(), draw.cull());
		altGlow = false;
		if (state.outlineColor != 0) {
			submitOutline(draw.loaded(), draw.skin(), poseStack, collector, draw.hidden(), draw.cull(), state.outlineColor);
		}
		submitGlow(draw.loaded(), draw.skin(), poseStack, collector, draw.hidden(), white(state), draw.cull());
		EmoteProps.Held props = state.getData(EmoteProps.HELD);
		if (props != null) {
			EmoteProps.submit(props, poseStack, collector, light, overlay);
		}
		EmoteProps.Placed held = state.getData(WEAPON);
		if (held != null) {
			EmoteProps.submit(new EmoteProps.Held(java.util.List.of(held)), poseStack, collector, light, overlay);
		}
		EmoteProps.Held extras = state.getData(SIDEARM);
		if (extras != null && !extras.props().isEmpty()) {
			EmoteProps.submit(extras, poseStack, collector, light, overlay);
		}
		Shadow shadow = state.getData(SHADOW);
		if (shadow != null && bed == null) {
			// her body's own turn and pose about her feet, then each vertex along the light onto the ground there
			Matrix4f body = new Matrix4f(feet).invert().mul(poseStack.last().pose());
			Vector3f lightInModel = new Matrix4f(body).invert().transformDirection(new Vector3f(shadow.toLight())).normalize();
			poseStack.pushPose();
			poseStack.last().pose().set(feet);
			submitShadow(draw.loaded(), draw.skin(), poseStack, collector, draw.hidden(), body, lightInModel, shadow);
			poseStack.popPose();
		}
		poseStack.popPose();
		return true;
	}

	/**
	 * Her shadow: each piece laid on the ground along the light, marked then darkened (FxRender: once wherever its
	 * triangles overlap), from a coarser copy of her mesh (shadowMesh) posed once for both passes. A closed piece sends
	 * only its side facing the light: its far side lands on the same ground.
	 */
	private static void submitShadow(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		Set<Integer> hidden, Matrix4f body, Vector3f lightInModel, Shadow shadow) {
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			Identifier texture = piece.image() >= 0 ? loaded.textures()[piece.image()] : null;
			if (texture == null || isEyeShadow(loaded, piece)) {
				continue;
			}
			boolean prop = PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (hiddenNow(loaded.model(), piece, hidden, prop)) {
				continue;
			}
			ShadowMesh mesh = SHADOW_MESHES.computeIfAbsent(piece, HeroModelRenderer::shadowMesh);
			if (mesh.indices().length == 0) {
				continue;
			}
			// this piece's flattened corners, worked out by the first pass and drawn again by the second; after everything
			// at the default order, the marks first, so they never meet another hero's darkening
			ShadowLaid[] laid = new ShadowLaid[1];
			if (Minecraft.getInstance().gameRenderer.useImprovedTransparency()) {
				// Improved Transparency: one blended pass through its order-independent pipelines (the two-pass mark and
				// darken has none, and crashed the frame: "does not have OIT pipelines set up"); overlaps darken more there
				collector.submitCustomGeometry(poseStack, FxRender.heroShadowOit(), (pose, buffer) -> {
					emitShadow(mesh, layShadow(piece, mesh, skin, pose, prop, body, lightInModel, shadow), buffer, null);
				});
				continue;
			}
			collector.order(SHADOW_MARK_ORDER).submitCustomGeometry(poseStack, FxRender.heroShadowMark(), (pose, buffer) -> {
				long t0 = System.nanoTime();
				laid[0] = layShadow(piece, mesh, skin, pose, prop, body, lightInModel, shadow);
				emitShadow(mesh, laid[0], buffer, true);
				Timing.shadow += System.nanoTime() - t0;
			});
			collector.order(SHADOW_DARKEN_ORDER).submitCustomGeometry(poseStack, FxRender.heroShadowDarken(), (pose, buffer) -> {
				long t0 = System.nanoTime();
				if (laid[0] != null) {
					emitShadow(mesh, laid[0], buffer, false);
				}
				Timing.shadow += System.nanoTime() - t0;
			});
		}
	}

	/**
	 * A piece's shadow mesh: its vertices merged into cells SHADOW_CELL across (its rest pose; each cell keeps its first
	 * vertex, and its skinning), the triangles left between three different cells, once each. Her face's and costume's
	 * fine detail goes, the outline stays: her 67k triangles cost 6 ms a frame laid twice over (84 to 47 FPS).
	 */
	private record ShadowMesh(int[] vertices, int[] indices, boolean closed) {
	}

	/** The cells' size (metres, ours: her fingers stay, her buttons go). */
	private static final float SHADOW_CELL = 0.035f;
	private static final Map<SkinnedModel.Piece, ShadowMesh> SHADOW_MESHES = new IdentityHashMap<>();

	private static ShadowMesh shadowMesh(SkinnedModel.Piece piece) {
		float[] pos = piece.pos();
		int n = piece.vertexCount();
		Map<Long, Integer> cells = new HashMap<>();
		int[] rep = new int[n];
		for (int v = 0; v < n; v++) {
			long key = Math.round(pos[v * 3] / SHADOW_CELL) * 73856093L ^ Math.round(pos[v * 3 + 1] / SHADOW_CELL) * 19349663L
				^ Math.round(pos[v * 3 + 2] / SHADOW_CELL) * 83492791L;
			Integer first = cells.putIfAbsent(key, v);
			rep[v] = first == null ? v : first;
		}
		int[] idx = piece.indices();
		Set<Long> seen = new HashSet<>();
		java.util.List<Integer> kept = new java.util.ArrayList<>();
		for (int t = 0; t + 2 < idx.length; t += 3) {
			int a = rep[idx[t]], b = rep[idx[t + 1]], c = rep[idx[t + 2]];
			if (a == b || b == c || a == c) {
				continue;
			}
			int lo = Math.min(a, Math.min(b, c));
			int hi = Math.max(a, Math.max(b, c));
			if (seen.add(((long) lo * 1_000_003L + (a + b + c - lo - hi)) * 1_000_003L + hi)) {
				kept.add(a);
				kept.add(b);
				kept.add(c);
			}
		}
		int[] indices = kept.stream().mapToInt(Integer::intValue).toArray();
		int[] vertices = java.util.Arrays.stream(indices).distinct().toArray();
		return new ShadowMesh(vertices, indices, CLOSED.computeIfAbsent(piece, HeroModelRenderer::isClosed));
	}

	/**
	 * A piece's shadow as laid this frame: each corner's x, y, z in the pose's space and its mark (at the original vertex's
	 * index, four floats each), and which triangles are left out (facing away from the light, a stowed prop's).
	 */
	private record ShadowLaid(float[] corners, boolean[] dropped) {
	}

	/**
	 * The shadow mesh's corners posed, laid along the light onto the ground under them and into the pose's space, each
	 * with its mark: 1 - strength over ground, 1 over none (a triangle across a ledge fades out over it).
	 */
	private static ShadowLaid layShadow(SkinnedModel.Piece piece, ShadowMesh mesh, Matrix4f[] skin, PoseStack.Pose pose, boolean prop,
		Matrix4f body, Vector3f lightInModel, Shadow shadow) {
		int n = piece.vertexCount();
		float[] p = new float[n * 3];
		skinnedSome(piece, skin, mesh.vertices(), p);
		// x, y, z and the mark's alpha per vertex
		float[] out = new float[n * 4];
		Matrix4f m = pose.pose();
		Vector3f t = new Vector3f();
		Vector3f toLight = shadow.toLight();
		float plane = shadow.plane();
		float marked = 1 - shadow.strength();
		for (int v : mesh.vertices()) {
			// about her feet, then along the light down to her feet's ground, then onto the ground under that spot
			body.transformPosition(p[v * 3], p[v * 3 + 1], p[v * 3 + 2], t);
			float along = (t.y - plane) / toLight.y;
			float sx = t.x - toLight.x * along;
			float sz = t.z - toLight.z * along;
			float gy = shadow.groundAt(sx, sz);
			m.transformPosition(sx, Float.isNaN(gy) ? plane : gy, sz, t);
			out[v * 4] = t.x;
			out[v * 4 + 1] = t.y;
			out[v * 4 + 2] = t.z;
			out[v * 4 + 3] = Float.isNaN(gy) ? 1 : marked;
		}
		float parked = STOWED_RADIUS * STOWED_RADIUS;
		int[] idx = mesh.indices();
		boolean[] dropped = new boolean[idx.length / 3];
		for (int t0 = 0; t0 + 2 < idx.length; t0 += 3) {
			int a = idx[t0], b = idx[t0 + 1], c = idx[t0 + 2];
			boolean drop = prop && near(p, a, parked) && near(p, b, parked) && near(p, c, parked);
			if (!drop && mesh.closed()) {
				float e1x = p[b * 3] - p[a * 3], e1y = p[b * 3 + 1] - p[a * 3 + 1], e1z = p[b * 3 + 2] - p[a * 3 + 2];
				float e2x = p[c * 3] - p[a * 3], e2y = p[c * 3 + 1] - p[a * 3 + 1], e2z = p[c * 3 + 2] - p[a * 3 + 2];
				float nx = e1y * e2z - e1z * e2y;
				float ny = e1z * e2x - e1x * e2z;
				float nz = e1x * e2y - e1y * e2x;
				drop = nx * lightInModel.x() + ny * lightInModel.y() + nz * lightInModel.z() < 0;
			}
			dropped[t0 / 3] = drop;
		}
		return new ShadowLaid(out, dropped);
	}

	/** One pass of the laid shadow: marking (each corner's mark in its alpha) or darkening (white). */
	/** mark: the first pass (alpha 1 - strength), else the second (white); null: one plain pass (black at its strength). */
	private static void emitShadow(ShadowMesh mesh, ShadowLaid shadow, VertexConsumer buffer, @Nullable Boolean mark) {
		int[] idx = mesh.indices();
		float[] laid = shadow.corners();
		for (int t0 = 0; t0 + 2 < idx.length; t0 += 3) {
			if (shadow.dropped()[t0 / 3]) {
				continue;
			}
			for (int k = 0; k < 3; k++) {
				int v = idx[t0 + k];
				int alpha = mark == null ? Math.round((1 - laid[v * 4 + 3]) * 255) : mark ? Math.round(laid[v * 4 + 3] * 255) : 255;
				buffer.addVertex(laid[v * 4], laid[v * 4 + 1], laid[v * 4 + 2])
					.setColor(alpha << 24 | (mark == null ? 0 : 0xFFFFFF))
					.setUv(0.5f, 0.5f)
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(FULL_BRIGHT)
					.setNormal(0, 1, 0);
			}
		}
	}

	/**
	 * Her shape again into Minecraft's outline target, in the outline's colour (a ping on an enemy, #40; the Glowing
	 * effect; a spectator's view of players). Custom geometry carries no outline of its own, so it's drawn as
	 * DragonFireballRenderer draws its quad's: a hero pinged as an enemy got the red ring but no outline (found on LAN).
	 */
	private static void submitOutline(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		Set<Integer> hidden, boolean cull, int outlineColor) {
		int color = 0xFF000000 | outlineColor;
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			Identifier texture = piece.image() >= 0 ? loaded.textures()[piece.image()] : null;
			if (texture == null || isEyeShadow(loaded, piece)) {
				continue;
			}
			boolean prop = PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (hiddenNow(loaded.model(), piece, hidden, prop)) {
				continue;
			}
			RenderType outline = FxRender.heroCutout(texture).outline().orElse(null);
			if (outline != null) {
				// vanilla's outline pipeline draws quads, not the hero pipeline's triangles
				collector.submitCustomGeometry(poseStack, outline, (pose, buffer) ->
					emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, prop, color, cull, false));
			}
		}
	}

	/**
	 * The first-person ofuda hand's index fingertip from the camera, in the world's axes (the arms are drawn in the
	 * world's camera-relative space, turned with the view), from the last frame drawn, and when (Util.getMillis).
	 */
	private static final Vector3f VIEW_FINGERTIP = new Vector3f();
	private static long viewFingertipAt = -1;

	public static Vector3f viewFingertip() {
		return new Vector3f(VIEW_FINGERTIP);
	}

	/** The first-person weapon's hp 0006 from the camera, in the world's axes, as last drawn (as VIEW_FINGERTIP). */
	private static final Vector3f VIEW_MUZZLE = new Vector3f();
	private static long viewMuzzleAt = -1;

	private static final Vector3f VIEW_GUN = new Vector3f();
	private static long viewGunAt = -1;

	/** Sojourn's own view's Railgun glow point (hp 007C) as drawn, from the camera in the world's axes, or null. */
	public static @Nullable Vector3f viewGun() {
		return viewGunAt >= 0 && Util.getMillis() - viewGunAt < 200 ? new Vector3f(VIEW_GUN) : null;
	}

	private static final Vector3f[] VIEW_PODS = {new Vector3f(), new Vector3f()};
	private static long viewPodsAt = -1;

	/** Juno's own view's torpedo pod (0 or 1) as drawn, from the camera in the world's axes, or null (as viewGun). */
	public static @Nullable Vector3f viewPod(int side) {
		return viewPodsAt >= 0 && Util.getMillis() - viewPodsAt < 200 ? new Vector3f(VIEW_PODS[side]) : null;
	}

	public static @Nullable Vector3f viewMuzzle() {
		return viewMuzzleAt >= 0 && Util.getMillis() - viewMuzzleAt < 200 ? new Vector3f(VIEW_MUZZLE) : null;
	}

	/** The arms were drawn just now (not hidden: F1, another view), so the fingertip is where they are. */
	public static boolean viewFingertipFresh() {
		return viewFingertipAt >= 0 && Util.getMillis() - viewFingertipAt < 200;
	}

	/**
	 * First person in hero mode: the hero's view model (hero_models/<hero>_1p.glb), its Camera bone put at the eye. The
	 * pose stack starts as the inverse of the view's turn (GameRenderer.renderItemInHand): camera-relative, in the
	 * world's axes. False (Minecraft draws its hand) without the model or out of hero mode.
	 */
	public static boolean submitFirstPerson(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, int light,
											float swayPitch, float swayYaw) {
		// heroView, not controls: her arms stay while she dies (Minecraft's blocky arm took their place on the death screen)
		return ClientHero.heroView() && submitArms(partialTicks, poseStack, collector, light, swayPitch, swayYaw, null, null);
	}

	/**
	 * Minecraft mode's first person with a hero picked (the owner, 2026-10-27: "Kiri's actual arms, but have kunai
	 * replaced with the block/item she is holding"): her view model with her own props put away, the main hand's item in
	 * her kunai hand and the other's in her ofuda hand (the owner's own idea too), a swing turning her arm. False (Minecraft draws its arm,
	 * dressed as her: HeroArmSkin, and its items) where an item's own animation is wanted: using one (eating, drinking,
	 * drawing a bow, blocking), a map, a loaded crossbow, the spyglass.
	 */
	public static boolean submitMinecraftArms(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, int light,
											  float swayPitch, float swayYaw, FirstPersonHandsAndItemsRenderState hands, AvatarRenderState avatar) {
		minecraftDraws = null;
		// Jetpack Cat's paws are on her controls, with no fist to hold an item: Minecraft's arm and item (her paws put a
		// pickaxe floating by her left console: found playing; how she should hold one is the owner's call)
		if (ClientHero.heroView() || !dev.overcraft.client.controls.HeroOptions.get().heroArms || hands.isScoping
			|| ClientHero.hero() == dev.overcraft.hero.Hero.JETPACK_CAT
			|| avatar.isUsingItem || ownAnimation(hands.mainHandItem)
			|| ownAnimation(hands.offHandItem)) {
			return false;
		}
		return submitArms(partialTicks, poseStack, collector, light, swayPitch, swayYaw, hands, avatar);
	}

	/** Items Minecraft holds its own way: maps (held up to read), a loaded crossbow (raised to aim). */
	private static boolean ownAnimation(ItemStack item) {
		return item.has(net.minecraft.core.component.DataComponents.MAP_ID)
			|| item.is(net.minecraft.world.item.Items.CROSSBOW) && net.minecraft.world.item.CrossbowItem.isCharged(item);
	}

	/**
	 * How a held item sits in each of her fists: turned (degrees about the hand bone's X, Y, Z) and moved (metres along
	 * them) from her weapon bone to the frame Minecraft's third-person grip starts from (ItemInHandLayer, after
	 * translateToHand); the kunai hand's (right of the view), then the ofuda hand's (mirrored). DevServer's "held" sets
	 * them. Ours, by eye (each axis tried both ways): turned half round and a twelfth more about X, a sword's handle sits
	 * in her fist and its blade rises out of it as her kunai's does, in either hand.
	 */
	public static final float[] HELD_KUNAI = {210, 0, 0, 0, 0, 0, 0.08f};
	public static final float[] HELD_OFUDA = {210, 0, 0, 0, 0, 0, 0.03f};
	/**
	 * A sprite item's extra turn about the kunai hand's grip (degrees; ours): Minecraft holds sprites (a torch, a bow,
	 * food) a quarter turn from tools, and in her low kunai hand they showed their edge to the eye. Told apart by the
	 * thin side of their box as held: a tool's is across, a sprite's along the view.
	 */
	private static final float SPRITE_TURN = -45;
	/**
	 * The most a held item may measure on screen (the owner: "items should have a max size"): its longest side as held,
	 * against its hand's distance from the eye; a flat one (a sword, food, a torch) more than a solid one (a block, a
	 * shield), whose bulk fills more of the view; and less in her ofuda hand, raised into the middle of the view, than in
	 * her kunai hand low at its side (there an item shrunk as far vanished into her fist). At their third-person size a
	 * shield or bread filled a quarter of the view. Ours, by eye; DevServer's "heldmax" sets them: kunai hand flat and
	 * solid, ofuda hand flat and solid.
	 */
	public static final float[] HELD_MAX = {0.55f, 0.35f, 0.3f, 0.22f};
	/**
	 * Held items' size against Minecraft's third-person size, kunai hand and ofuda hand (ours, by eye), under the caps
	 * above: one scale keeps Minecraft's own proportions (an apple smaller than a sword), where capping alone made an
	 * apple as long as a sword. DevServer's "heldscale" sets them.
	 */
	public static final float[] HELD_SCALE = {0.23f, 0.23f};
	/** Thinner than this (metres, as held) an item counts as flat. */
	private static final float FLAT = 0.1f;
	/** Dev only: the held items' measured sizes (DevServer's "state"). */
	public static final Map<Boolean, String> heldSizes = new HashMap<>();
	/** The held items as Minecraft's third-person hands show them (resolved each frame, reused). */
	private static final net.minecraft.client.renderer.item.ItemStackRenderState MAIN_HELD =
		new net.minecraft.client.renderer.item.ItemStackRenderState();
	private static final net.minecraft.client.renderer.item.ItemStackRenderState OFF_HELD =
		new net.minecraft.client.renderer.item.ItemStackRenderState();

	private static boolean submitArms(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, int light,
									  float swayPitch, float swayYaw, @Nullable FirstPersonHandsAndItemsRenderState hands,
									  @Nullable AvatarRenderState avatar) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		Hero hero = ClientHero.hero();
		if (player == null || hero == null) {
			return false;
		}
		HeroModels.Loaded loaded = HeroModels.firstPerson(hero);
		if (loaded == null) {
			return false;
		}
		boolean minecraft = hands != null && avatar != null;
		float now = (player.level().getGameTime() + partialTicks) / 20f;
		Matrix4f view = new Matrix4f();
		HeroAnimator animator = HeroAnimator.of(player);
		// Minecraft mode's swing turns her arm (hero mode's actions are her clips)
		animator.swing = !minecraft || hands.attackHand == net.minecraft.world.InteractionHand.OFF_HAND ? 0
			: HeroAnimator.SWING[4] > 0 ? HeroAnimator.SWING[4] : avatar.swingAnimation;
		Matrix4f[] skin = animator.poseFirstPerson(loaded.model(), player, now, view);
		if (hero == Hero.ANA && !minecraft && dev.overcraft.client.ClientAna.ownScope() >= 1) {
			// fully scoped, Overwatch draws nothing of her: her view model goes (0B94 #199, M) and her scope's overlay fills
			// the view (HeroHud.anaScope, #90). Her held pose 3B92 already takes the arms out of view (played); not drawing
			// them makes sure no edge of the rifle shows inside the lens while she turns (the arms' sway)
			return true;
		}
		if (hero == Hero.BRIGITTE && !minecraft) {
			// her barrier up, as her side sees it: square to her view, 0.9 m ahead, from about her feet up (HeroWeapon)
			EmoteProps.Placed barrier = HeroWeapon.barrierShown(animator, now);
			if (barrier != null) {
				poseStack.pushPose();
				poseStack.rotateDegrees(Axis.YP, 180);
				poseStack.translate(0, -player.getEyeHeight(), 0);
				EmoteProps.submit(new EmoteProps.Held(java.util.List.of(barrier)), poseStack, collector,
					Math.max(light & 0xFFFF, VIEW_MODEL_MIN_LIGHT << 4) | light & 0xFFFF0000, OverlayTexture.NO_OVERLAY);
				poseStack.popPose();
			}
		}
		poseStack.pushPose();
		// (the stack acts in view space from here: x right, y up, -z ahead)
		// a little behind the movement, as Overwatch's arms drift: aside when strafing, up when falling (#14)
		drift(player, partialTicks);
		poseStack.translate(driftX, driftY, 0);
		// bigger on screen than the model's own (ARM_SCALE): scaled across the view, not along it
		poseStack.scale(ARM_SCALE, ARM_SCALE, 1);
		// the arms lag a little behind the view when turning (Minecraft's hand sway, a bit stronger: Overwatch's arms
		// swing visibly)
		poseStack.rotateDegrees(Axis.XP, swayPitch * ARM_SWAY);
		poseStack.rotateDegrees(Axis.YP, swayYaw * ARM_SWAY);
		// the camera looks down +Z in the model's space and down -Z in the view's: turn it round, then the model as
		// its Camera bone sees it
		poseStack.rotateDegrees(Axis.YP, 180);
		poseStack.mulPose(view);
		// where her ofuda hand's fingertip is in the view: the throw's puff pops there
		poseStack.last().pose().transformPosition(animator.fingertip, VIEW_FINGERTIP);
		viewFingertipAt = Util.getMillis();
		if (hero == Hero.JUNO && !minecraft && animator.viewPose() != null) {
			// her torpedo pods (bones 0182 / 0183 = hp 048D / 048E) as her own view draws them: the lock raises them into the
			// top corners (torpedo_lock_start), and her torpedoes leave from there (#86: they started behind her eyes)
			for (int side = 0; side < 2; side++) {
				int node = loaded.model().node(side == 0 ? "bone_0182" : "bone_0183");
				if (node >= 0) {
					Vector3f at = Skeleton.world(loaded.model(), animator.viewPose(), node).getTranslation(new Vector3f());
					poseStack.last().pose().transformPosition(at, VIEW_PODS[side]);
				}
			}
			viewPodsAt = Util.getMillis();
		}
		// her own arms flash white when her Suzu cleanses her
		float white = Cleansed.white(player.getId());
		int overlay = white > 0 ? OverlayTexture.pack(white * 0.6f, false) : OverlayTexture.NO_OVERLAY;
		int lit = Math.max(light & 0xFFFF, VIEW_MODEL_MIN_LIGHT << 4) | light & 0xFFFF0000;
		// Sombra stealthed: her own arms and pistol phased out too, as Overwatch's (#87: they stayed solid)
		float stealth = hero == Hero.SOMBRA && !minecraft ? dev.overcraft.client.ClientSombra.phase(player, now) : 0;
		if (stealth > 0) {
			submitStealth(loaded, skin, poseStack, collector, Set.of(), stealth, now, lit, overlay, true);
		} else {
			altGlow = hero == Hero.JUNO && !minecraft && dev.overcraft.client.ClientJuno.faceAngry(player);
			submitModel(loaded, skin, poseStack, collector, lit, overlay, minecraft ? allProps(loaded.model()) : Set.of(), true);
			altGlow = false;
			submitGlow(loaded, skin, poseStack, collector, Set.of(), white, true);
		}
		// his weapon in his hand when it's a model of its own (Lúcio's), not in Minecraft mode (Minecraft's items then)
		EmoteProps.Placed weapon = minecraft ? null : HeroWeapon.place(hero, animator, loaded.model(), true, now);
		net.minecraft.world.phys.Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
		net.minecraft.world.phys.Vec3 whip = hero == Hero.BRIGITTE && !minecraft
			? dev.overcraft.client.effect.BrigitteFx.whipHead(player.getId(), player.getEyePosition(partialTicks)) : null;
		// her whip head in the view's own frame (the pose stack is camera-relative, in the world's axes, turned with the view)
		animator.whipHead = whip == null ? null : new Matrix4f(poseStack.last().pose()).invert()
			.transformPosition(new Vector3f((float) (whip.x - cam.x), (float) (whip.y - cam.y), (float) (whip.z - cam.z)));
		if (!minecraft) {
			java.util.List<EmoteProps.Placed> viewParts = HeroWeapon.extras(hero, animator, loaded.model(), true, now);
			if (stealth > 0) {
				for (EmoteProps.Placed part : viewParts) {
					poseStack.pushPose();
					poseStack.mulPose(part.at());
					submitStealth(part.loaded(), part.skin(), poseStack, collector, Set.of(), stealth, now, lit, overlay, true);
					poseStack.popPose();
				}
			} else {
				EmoteProps.submit(new EmoteProps.Held(viewParts), poseStack, collector, lit, overlay);
			}
			if (hero == Hero.BRIGITTE && animator.chainFrom != null && animator.chainTo != null) {
				dev.overcraft.client.effect.BrigitteFx.viewChain(poseStack, collector, animator.chainFrom, animator.chainTo, now);
			}
		}
		if (weapon != null) {
			if (stealth > 0) {
				poseStack.pushPose();
				poseStack.mulPose(weapon.at());
				submitStealth(weapon.loaded(), weapon.skin(), poseStack, collector, Set.of(), stealth, now, lit, overlay, true);
				poseStack.popPose();
			} else {
				EmoteProps.submit(new EmoteProps.Held(java.util.List.of(weapon)), poseStack, collector, lit, overlay);
			}
			if (hero == Hero.ANA) {
				anaCounter(weapon, poseStack, collector);
			}
			// its hp 0006 (Mercy's staff head, her blaster's muzzle) as drawn: her own view's beam starts there (MercyFx)
			Vector3f muzzle = HeroWeapon.muzzle(hero, animator, loaded.model(), true, now);
			if (muzzle != null) {
				poseStack.last().pose().transformPosition(muzzle, VIEW_MUZZLE);
				viewMuzzleAt = Util.getMillis();
			}
			// Sojourn's Railgun's energy glow point (hp 007C) as drawn: SojournFx's glow on her own view's gun
			Vector3f gun = hero == Hero.SOJOURN ? HeroWeapon.point(hero, animator, loaded.model(), true, now, "hardpoint_007C") : null;
			if (gun != null) {
				poseStack.last().pose().transformPosition(gun, VIEW_GUN);
				viewGunAt = Util.getMillis();
			}
		}
		if (minecraft && animator.viewPose() != null) {
			// dev only (DevServer "viewbones"): the hands' bones in the view's space, to see which hand is where, and their
			// turns in the view (Kiriko's are HELD_RIGHT_REF / HELD_LEFT_REF)
			VIEW_BONES.clear();
			Quaternionf l = new Matrix4f(view).mul(animator.kunaiHand).getUnnormalizedRotation(new Quaternionf()).normalize();
			Quaternionf r = new Matrix4f(view).mul(animator.ofudaHand).getUnnormalizedRotation(new Quaternionf()).normalize();
			VIEW_BONES.put("~turn _L", new Vector3f(l.x, l.y, l.z));
			VIEW_BONES.put("~turn _L w", new Vector3f(l.w, 0, 0));
			VIEW_BONES.put("~turn _R", new Vector3f(r.x, r.y, r.z));
			VIEW_BONES.put("~turn _R w", new Vector3f(r.w, 0, 0));
			SkinnedModel m = loaded.model();
			for (int i = 0; i < m.nodeNames.length; i++) {
				String n = m.nodeNames[i];
				if (n != null && n.matches("(?i).*(wrist|weapon|hand|palm|finger_1|thumb_1|arm_|elbow|forearm).*")) {
					Vector3f at = Skeleton.world(m, animator.viewPose(), i).getTranslation(new Vector3f());
					VIEW_BONES.put(n, poseStack.last().pose().transformPosition(at));
				}
			}
		}
		if (minecraft) {
			// the main hand's item in her hand on its side of the view, the other's in the other (the owner's idea too); an
			// item on a side with no hand of hers in view (most heroes' other hand is behind the camera) is Minecraft's to
			// draw, its own way, on that side: a left-handed Juno holds the off hand's item in her right-of-view hand (#78)
			Matrix4f[] seen = viewHands(poseStack, animator, loaded.model());
			boolean right = avatar.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT;
			Matrix4f main = seen[right ? 0 : 1];
			Matrix4f off = seen[right ? 1 : 0];
			// the swing turns the arm holding the main hand's item (none when Minecraft holds it: it swings its own)
			animator.swingSide = main == null ? null : main == animator.kunaiHand;
			submitGripped(poseStack, collector, lit, player, held(loaded.model(), animator, view, main, main == seen[0]),
				main == seen[0], hands.mainHandItem, MAIN_HELD);
			if (off != null) {
				submitGripped(poseStack, collector, lit, player, held(loaded.model(), animator, view, off, off == seen[0]),
					off == seen[0], hands.offHandItem, OFF_HELD);
			}
			minecraftDraws = main == null && off == null ? FirstPersonHandsAndItemsRenderState.HandRenderSelection.RENDER_BOTH_HANDS
				: main == null ? FirstPersonHandsAndItemsRenderState.HandRenderSelection.RENDER_MAIN_HAND_ONLY
				: off == null && !hands.offHandItem.isEmpty() ? FirstPersonHandsAndItemsRenderState.HandRenderSelection.RENDER_OFF_HAND_ONLY
				: null;
		}
		poseStack.popPose();
		return true;
	}

	/**
	 * Set by submitMinecraftArms: the items with no hand of hers in view to hold them, which Minecraft draws its own way
	 * (FirstPersonHandsMixin); null when she holds them all.
	 */
	public static FirstPersonHandsAndItemsRenderState.@org.jspecify.annotations.Nullable HandRenderSelection minecraftDraws;

	/**
	 * How Kiriko's hands' weapon bones sit in her view at rest (quaternions x, y, z, w, in her view model's frame:
	 * DevServer "viewbones" with her picked; ours, measured): the grips (HELD_KUNAI, HELD_OFUDA) were set by eye on them.
	 * Every other view model's hand bone is turned to match at rest, so an item sits in each hero's fist as in hers, and
	 * still moves with the hand (#78: Juno's sword pointed down under her hand, Brigitte's was lost in her fist).
	 */
	public static final float[] HELD_RIGHT_REF = {0.741f, 0.185f, -0.458f, -0.456f}, HELD_LEFT_REF = {0.481f, 0.402f, 0.748f, -0.217f};
	private static final Map<String, Quaternionf> HELD_TURNS = new HashMap<>();

	/** A hand bone with the turn that puts it, at rest, the way Kiriko's on that side of the view sits (#78). */
	private static @org.jspecify.annotations.Nullable Matrix4f held(SkinnedModel model, HeroAnimator animator, Matrix4f view,
																   @org.jspecify.annotations.Nullable Matrix4f bone, boolean rightOfView) {
		if (bone == null) {
			// no hand of hers in view (Lúcio's): Minecraft draws the items (a null here crashed the frame, found playing)
			return null;
		}
		String key = System.identityHashCode(model) + (bone == animator.kunaiHand ? "L" : "R") + (rightOfView ? ">" : "<");
		if (animator.viewAtRest() || !HELD_TURNS.containsKey(key)) {
			float[] ref = rightOfView ? HELD_RIGHT_REF : HELD_LEFT_REF;
			Quaternionf seen = new Matrix4f(view).mul(bone).getUnnormalizedRotation(new Quaternionf()).normalize();
			HELD_TURNS.put(key, seen.invert().mul(new Quaternionf(ref[0], ref[1], ref[2], ref[3]).normalize()));
		}
		Matrix4f grip = new Matrix4f(bone).rotate(HELD_TURNS.get(key));
		// and moved so her knuckles sit where Kiriko's do in her grip: the item in the fist, not at a gun's grip bone
		Vector3f knuckles = knuckles(model, animator, bone == animator.kunaiHand);
		if (knuckles != null) {
			Vector3f local = new Matrix4f(grip).invert().transformPosition(knuckles);
			float[] ref = rightOfView ? HELD_RIGHT_KNUCKLES : HELD_LEFT_KNUCKLES;
			VIEW_BONES.put("~knuckles " + (rightOfView ? "right" : "left"), new Vector3f(local));
			if (ref != null) {
				grip.translate(local.x - ref[0], local.y - ref[1], local.z - ref[2]);
			}
		}
		return grip;
	}

	/** Kiriko's knuckles in her grip frames (metres; ours, measured with DevServer "viewbones"; null: not moved). */
	public static float @org.jspecify.annotations.Nullable [] HELD_RIGHT_KNUCKLES = {0.023f, -0.042f, 0.070f}, HELD_LEFT_KNUCKLES = {0.037f, 0.023f, -0.028f};

	/** A hand's knuckles in the model's space: the middle of its four fingers' first joints (null without them). */
	private static @org.jspecify.annotations.Nullable Vector3f knuckles(SkinnedModel model, HeroAnimator animator, boolean left) {
		Skeleton.Pose pose = animator.viewPose();
		if (pose == null) {
			return null;
		}
		Vector3f sum = new Vector3f();
		for (String finger : new String[] {"IndexFinger_1_", "MiddleFinger_1_", "RingFinger_1_", "PinkyFinger_1_"}) {
			int i = model.node(finger + (left ? "L" : "R"));
			if (i < 0) {
				return null;
			}
			sum.add(Skeleton.world(model, pose, i).getTranslation(new Vector3f()));
		}
		return sum.div(4);
	}

	/** How far out of the view's edges a hand bone may sit and its fist still show (the fist and item rise into the view). */
	private static final float HAND_SEEN_MARGIN = 1.15f;

	/**
	 * Her two hands' weapon bones (the model's "_L" and "_R") as they show in the view: [right of the view, left of the
	 * view], each null when out of view. Which side is which differs by view model (Kiriko's "_L" hand is on the right
	 * of the view, Juno's "_R"), and most heroes' other hand is behind the camera (#78, measured with DevServer
	 * "viewbones").
	 */
	private static Matrix4f[] viewHands(PoseStack poseStack, HeroAnimator animator, SkinnedModel model) {
		// judged at rest and kept through a swing or an action: a swing carries the hand out of the view's edge, and the
		// item jumped to Minecraft's own hand for those frames
		boolean[] kept = SEEN_HANDS.get(model);
		if (kept != null && !animator.viewAtRest()) {
			Matrix4f[] out = new Matrix4f[2];
			for (int side = 0; side < 2; side++) {
				out[side] = kept[side * 2] ? (kept[side * 2 + 1] ? animator.kunaiHand : animator.ofudaHand) : null;
			}
			return out;
		}
		Matrix4f[] out = judgeHands(poseStack, animator, model);
		SEEN_HANDS.put(model, new boolean[] {out[0] != null, out[0] == animator.kunaiHand, out[1] != null, out[1] == animator.kunaiHand});
		return out;
	}

	/** Per view model: [right seen, right is "_L", left seen, left is "_L"], as last judged at rest. */
	private static final Map<SkinnedModel, boolean[]> SEEN_HANDS = new IdentityHashMap<>();

	private static Matrix4f[] judgeHands(PoseStack poseStack, HeroAnimator animator, SkinnedModel model) {
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.mainCamera();
		float tanY = (float) Math.tan(Math.toRadians(mc.options.fov().get() / 2.0));
		float tanX = tanY * mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
		Matrix4f[] out = new Matrix4f[2];
		float[] across = new float[2];
		for (Matrix4f bone : new Matrix4f[] {animator.kunaiHand, animator.ofudaHand}) {
			// the knuckles, where the item will sit (Mercy's "_R" weapon bone is out at her staff, her hand by the camera)
			Vector3f knuckles = knuckles(model, animator, bone == animator.kunaiHand);
			Vector3f at = poseStack.last().pose().transformPosition(knuckles != null ? knuckles : bone.getTranslation(new Vector3f()));
			float ahead = at.dot(camera.forwardVector());
			if (ahead < 0.1f) {
				continue;
			}
			float x = -at.dot(camera.leftVector()) / ahead;
			float y = at.dot(camera.upVector()) / ahead;
			if (Math.abs(x) > tanX * HAND_SEEN_MARGIN || Math.abs(y) > tanY * HAND_SEEN_MARGIN) {
				continue;
			}
			int side = x >= 0 ? 0 : 1;
			if (out[side] != null) {
				// both on one side of the view: the one further over keeps that side, the other takes the other
				int other = 1 - side;
				boolean further = side == 0 ? x > across[0] : x < across[1];
				out[other] = further ? out[side] : bone;
				across[other] = further ? across[side] : x;
				if (!further) {
					continue;
				}
			}
			out[side] = bone;
			across[side] = x;
		}
		return out;
	}

	/**
	 * A held item gripped in her fist, as Minecraft's players hold theirs in third person (its own third-person display
	 * turn and size: a sword's handle in the fist and its blade out, a block small and square), on her hand's weapon
	 * bone so it moves with her hand, no bigger than heldMax. Held to the view's corner the first-person way, items
	 * floated beside her fist (the owner, 2026-10-27).
	 */
	private static void submitGripped(PoseStack poseStack, SubmitNodeCollector collector, int light, LocalPlayer player,
									  @org.jspecify.annotations.Nullable Matrix4f bone, boolean kunaiSide, ItemStack item,
									  net.minecraft.client.renderer.item.ItemStackRenderState state) {
		if (item.isEmpty() || bone == null) {
			return;
		}
		// her kunai hand shows on the right of the view: Minecraft's right hand's grip; her ofuda hand its left
		Minecraft.getInstance().getItemModelResolver().updateForLiving(state, item, kunaiSide
			? net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
			: net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND, player);
		if (state.isEmpty()) {
			return;
		}
		float[] held = kunaiSide ? HELD_KUNAI : HELD_OFUDA;
		AABB box = state.getModelBoundingBox();
		float longest = (float) Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
		float thinnest = (float) Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize()));
		// the hand's distance from the eye (the stack's start is at the eye, turned with the view)
		float distance = poseStack.last().pose().transformPosition(bone.getTranslation(new Vector3f())).length();
		float max = HELD_MAX[(kunaiSide ? 0 : 2) + (thinnest < FLAT ? 0 : 1)] * distance;
		float shrink = Math.min(HELD_SCALE[kunaiSide ? 0 : 1], max / Math.max(longest, 1.0E-4f));
		heldSizes.put(kunaiSide, String.format(java.util.Locale.ROOT, "%s %.3f x %.3f x %.3f at %.2f m", item.getItem(),
			box.getXsize(), box.getYsize(), box.getZsize(), distance));
		poseStack.pushPose();
		poseStack.mulPose(bone);
		poseStack.translate(held[3], held[4], held[5]);
		poseStack.rotateDegrees(Axis.XP, held[0]);
		boolean sprite = box.getZsize() < FLAT && box.getXsize() >= FLAT;
		poseStack.rotateDegrees(Axis.YP, held[1] + (kunaiSide && sprite ? SPRITE_TURN : 0));
		poseStack.rotateDegrees(Axis.ZP, held[2]);
		// ItemInHandLayer's turn into the grip (its move from the shoulder down to the hand isn't wanted: this is the hand)
		poseStack.rotateDegrees(Axis.XP, -90);
		poseStack.rotateDegrees(Axis.YP, 180);
		// and out of her fist along Minecraft's grip (its items rise from the hand along it): a small item shrunk about
		// the fist's middle vanished inside it
		poseStack.translate(0, held[6], 0);
		poseStack.scale(shrink, shrink, shrink);
		state.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}

	/** Dev only: the view model's hand bones in the view's space as last drawn in Minecraft mode (DevServer "viewbones"). */
	public static final Map<String, Vector3f> VIEW_BONES = new java.util.TreeMap<>();

	private static final Map<SkinnedModel, Set<Integer>> ALL_PROPS = new IdentityHashMap<>();

	/** Every held prop's bone (her kunai, ofuda and Suzu), all put away: Minecraft mode holds Minecraft's items. */
	private static Set<Integer> allProps(SkinnedModel model) {
		return ALL_PROPS.computeIfAbsent(model, m -> {
			Set<Integer> joints = new HashSet<>();
			for (SkinnedModel.Piece piece : m.pieces) {
				if (piece.vertexCount() > 0 && PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp)) {
					joints.add(piece.joint4()[0]);
				}
			}
			return joints;
		});
	}

	/**
	 * Props shown only during one action: Kiriko's locomotion clips (run, strafe, jump, fall) carry the Suzu in her hand,
	 * which the game hides until she throws it (the props survey: those clips move its bone, bone_006B, to the hand).
	 * Bone name -> the action (prefix) that shows it.
	 */
	private static final Map<String, String> PROP_ACTIONS = Map.of("bone_006B", "throw_suzu");

	/** The joints of props to hide this frame (their action isn't playing). */
	private static Set<Integer> hiddenProps(SkinnedModel model, @Nullable String action) {
		Set<Integer> hidden = new HashSet<>();
		for (int j = 0; j < model.joints.length; j++) {
			String shownBy = PROP_ACTIONS.get(model.nodeNames[model.joints[j]]);
			if (shownBy != null && (action == null || !action.startsWith(shownBy))) {
				hidden.add(j);
			}
		}
		return hidden;
	}

	/** Dev timing: nanoseconds spent posing and emitting hero models since the last report (DevServer "timing"). */
	public static final class Timing {
		public static long pose;
		public static long emit;
		/** Her shadows' share (both passes). */
		public static long shadow;
		/** Its glows' share, apart from emit. */
		public static long glowEmit;
		/** Dev ("timing glow N"): how glows draw, to measure them: off, whole pieces sorted as they first were, whole unsorted, lit triangles unsorted (the shipped way). */
		public static final int GLOW_OFF = 0, GLOW_WHOLE_SORTED = 1, GLOW_WHOLE = 2, GLOW_LIT = 3;
		public static int glow = GLOW_LIT;
		/** Dev: draw only every this many triangles (1 = all), to see what the vertex count costs. */
		public static int stride = 1;
		/** Dev: CPU back-face culling on (compare pictures with it off). */
		public static boolean cull = true;
	}

	/** Submits a skinned model's pieces (hero models, Kitsune Rush's fox), held props not in use left out. */
	public static void submitModel(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		int light, int overlay) {
		submitModel(loaded, skin, poseStack, collector, light, overlay, Set.of(), true);
	}

	/** ... with the props on these joints hidden. */
	public static void submitModel(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		int light, int overlay, Set<Integer> hidden, boolean cull) {
		submitModel(loaded, skin, poseStack, collector, light, overlay, hidden, cull, true);
	}

	/**
	 * ... and, with {@code props} false, no hiding of stowed props: a projectile's model is small and centred on its
	 * origin, so as a hero's prop every triangle counted as parked and none was drawn (the fifth critique: no paper on
	 * the ofuda; no kunai or Suzu model either).
	 */
	public static void submitModel(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		int light, int overlay, Set<Integer> hidden, boolean cull, boolean props) {
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			Identifier texture = piece.image() >= 0 ? loaded.textures()[piece.image()] : null;
			if (texture == null && piece.material().endsWith(SIGMA_CORE_MATERIAL)
				&& (piece.mesh().endsWith(SIGMA_CORE) || piece.mesh().endsWith(SIGMA_CORE_BASE))) {
				// Sigma's hand-sphere cores (submeshes 2 / 3 of 106B4, material 7C98: a shader, no colour texture): its cloud
				// noise 19167 through its LUT's colour row times its emission (8000DFB0EAFE r1: plum, violet to blue), as added light
				Identifier core = dev.overcraft.client.effect.EffectArt.get("cat_glow_noise",
					dev.overcraft.client.effect.EffectArt.Palette.SIG_HAND_EMISSION, false);
				if (core != null) {
					collector.submitCustomGeometry(poseStack, FxRender.additive(core),
						(pose, buffer) -> emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false, 0xFFFFFFFF, false, false));
				}
				continue;
			}
			// its added light (HeroModels.Glow, tools/glow_maps.py): over its colour, or alone where the piece has none (a light card)
			HeroModels.Glow glow = loaded.glow().get(piece.material());
			if (texture == null && glow == null || texture != null && isEyeShadow(loaded, piece)) {
				continue;
			}
			boolean prop = props && PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (hiddenNow(loaded.model(), piece, hidden, prop)) {
				continue;
			}
			if (glow != null && Timing.glow != Timing.GLOW_OFF) {
				int tint = Math.clamp(Math.round(glow.strength() * GLOW_ALPHA * 255), 0, 255) << 24 | 0xFFFFFF;
				// a screen with two faces (Juno's gun 4E48C: the other on an enemy, by that Juno's own aim: altGlow)
				Identifier image = glow.alt() != null && altGlow ? glow.alt() : glow.texture();
				// its lit triangles alone (HeroModels.litPieces), not sorted: added light sums alike in any order
				SkinnedModel.Piece part = Timing.glow == Timing.GLOW_LIT ? loaded.lit().getOrDefault(piece, piece) : piece;
				if (part.indices().length > 0) {
					collector.submitCustomGeometry(poseStack, Timing.glow == Timing.GLOW_WHOLE_SORTED ? FxRender.additive(image) : FxRender.glow(image),
						(pose, buffer) -> {
							long t0 = System.nanoTime();
							emit(part, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, prop, tint, cull && texture != null, false);
							Timing.glowEmit += System.nanoTime() - t0;
						});
				}
			}
			if (glow != null) {
				if (texture == null || glow.glass()) {
					continue;
				}
			}
			if (piece.mesh().startsWith("holo_") || piece.mesh().startsWith("holoc_")) {
				// a hologram (tools/owm_static.py): its light mask added in the tint, lit by itself, seen from both sides; a
				// screen (holoc_, tools/owm_survey.py SCREENS: her tablet) is its own colours, added untinted
				int tint = piece.mesh().startsWith("holoc_") ? 0xFFFFFFFF : HOLOGRAM;
				collector.submitCustomGeometry(poseStack, FxRender.additive(texture),
					(pose, buffer) -> emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, prop, tint, false, false));
				continue;
			}
			collector.submitCustomGeometry(poseStack, FxRender.heroCutout(texture), (pose, buffer) -> {
				long t0 = System.nanoTime();
				emit(piece, skin, pose, buffer, light, overlay, prop, -1, cull, true);
				Timing.emit += System.nanoTime() - t0;
			});
		}
	}

	/** Ana's rifle's sight screen (the 48 vertices of material BB56, shader 0x32, the image 38704). */
	private static final String ANA_SCREEN = "00000000BB56";
	/** Her ammo's digits on it: colour from the screen's image, size and offset ours, read against the screen in first person. */
	private static final int ANA_DIGITS = 0xFFC8F4FF;
	/** A plate behind them, over the screen's own art: without it they read as a scribble on it at play distance (critique_parts D). */
	private static final int ANA_DIGIT_PLATE = 0xF2081420;
	private static final float ANA_DIGIT_SCALE = 0.00105f;

	/**
	 * The ammo counter on her rifle's sight: tens and units of her ammo, 9 from 100 up (0B94 #542 / #543, ana-map.md), which the data
	 * sends to dataflows 0B9A / 0B9B and shows by a means not read; drawn as text on the screen's own piece, facing out of it.
	 */
	private static void anaCounter(EmoteProps.Placed weapon, PoseStack poseStack, SubmitNodeCollector collector) {
		dev.overcraft.net.HeroSyncPayload s = dev.overcraft.client.ClientHero.sync();
		SkinnedModel.Piece screen = null;
		for (SkinnedModel.Piece piece : weapon.loaded().model().pieces) {
			if (piece.material().equals(ANA_SCREEN)) {
				screen = piece;
			}
		}
		if (s == null || screen == null || screen.vertexCount() == 0) {
			return;
		}
		Vector3f c = new Vector3f(), n = new Vector3f();
		for (int v = 0; v < screen.vertexCount(); v++) {
			c.add(screen.pos()[v * 3], screen.pos()[v * 3 + 1], screen.pos()[v * 3 + 2]);
			n.add(screen.normal()[v * 3], screen.normal()[v * 3 + 1], screen.normal()[v * 3 + 2]);
		}
		c.div(screen.vertexCount());
		n.normalize();
		Vector3f up = new Vector3f(0, 1, 0).sub(new Vector3f(n).mul(n.y)).normalize();
		Vector3f right = new Vector3f(up).cross(n).normalize();
		int ammo = s.primaryAmmo();
		String digits = ammo > 99 ? "99" : String.format(java.util.Locale.ROOT, "%02d", ammo);
		net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
		poseStack.pushPose();
		poseStack.mulPose(weapon.at());
		poseStack.mulPose(weapon.skin()[screen.joint4()[0]]);
		poseStack.mulPose(new Matrix4f(right.x, right.y, right.z, 0, up.x, up.y, up.z, 0, n.x, n.y, n.z, 0, c.x + n.x * 0.0008f, c.y + n.y * 0.0008f, c.z + n.z * 0.0008f, 1));
		poseStack.scale(ANA_DIGIT_SCALE, -ANA_DIGIT_SCALE, ANA_DIGIT_SCALE);
		collector.submitText(poseStack, -font.width(digits) / 2f, -4, net.minecraft.network.chat.Component.literal(digits).getVisualOrderText(), false,
			net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, FULL_BRIGHT, ANA_DIGITS, ANA_DIGIT_PLATE, 0);
		poseStack.popPose();
	}

	/** Whether the piece screens drawn now show their other face (Juno's gun: this Juno aims at an enemy). */
	private static boolean altGlow;

	/** Dev only (DevServer "aimpose"): the local player's model aims this pitch and is turned this far, the camera left level. */
	public static float @Nullable [] DEV_AIM;

	/** A glow at strength 1 (ours: its image holds the colour, Overwatch's HDR strength is clamped to 2 by the tool). */
	private static final float GLOW_ALPHA = 0.8f;

	/**
	 * Sigma's hand-sphere cores' material (7C98, a shader with no colour texture). The submesh keys below are shared with other
	 * models' untextured pieces (Lucio's, Sombra's, Ana's pistol, Brigitte's and her shield, Lifeweaver's platform and flower
	 * ring all have a 6B24EE2D2B083AFE piece), which keyed alone drew violet: found by the 2026-11-02 QA audit, seen on
	 * Brigitte's shield in the release play.
	 */
	private static final String SIGMA_CORE_MATERIAL = "7C98";
	/** Sigma's hand-sphere cores' submesh key (tools/reference/sigma-effects.md "The hand spheres' real look"). */
	private static final String SIGMA_CORE = "6B24EE2D2B083AFE";
	/**
	 * The same cores on the spheres' base model D7DD (submeshes 2 / 3, material 7C98 again), which his emotes spawn as props
	 * (Dans' juggled copy, Constellation's two: techniques/emotes/sigma_emotes.md); drawn dark without this.
	 */
	private static final String SIGMA_CORE_BASE = "3060878AEBDEB872";

	/** A hologram's tint: the middle of its colour strip A06C (tools/reference/sombra-effects.md, M: the strip read as one colour). */
	private static final int HOLOGRAM = 0xFF8344EF;

	/**
	 * A spirit: the model see-through and glowing (Kitsune Rush's fox: Overwatch's is a translucent ice-blue spirit; drawn
	 * solid it read as a plush toy, the third critique), its texture at this opacity, full bright, with a cyan glow over it.
	 */
	public static void submitSpirit(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		float opacity, float glow, int rgb, int glowRgb, boolean added) {
		int color = Math.clamp(Math.round(opacity * 255), 0, 255) << 24 | rgb;
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			Identifier texture = piece.image() >= 0 ? loaded.textures()[piece.image()] : null;
			if (texture == null) {
				continue;
			}
			// added: its light and the glow's sum whatever order they're drawn in (blended, it was drawn over its own glow
			// and hid most of it: a matte blue fox, the seventh critique)
			collector.submitCustomGeometry(poseStack, added ? FxRender.additive(texture) : RenderTypes.entityTranslucentEmissive(texture),
				(pose, buffer) -> emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false, color, true, false));
		}
		submitGlow(loaded, skin, poseStack, collector, Set.of(), glow, true, glowRgb);
	}

	/**
	 * A model with no texture of its own drawn as added light in one colour (ARGB; its alpha the strength), seen from both
	 * sides, full bright: an energy field whose look is a shader in Overwatch (Sigma's barrier C015, material 7C75: no
	 * texture in its export), a stand-in until its effect is read.
	 */
	public static void submitField(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector, int argb) {
		HudShapes.ensure();
		submitField(loaded, skin, poseStack, collector, argb, HudShapes.WHITE);
	}

	/** The same through a texture on the model's own UVs (Sigma's barrier: its field's rim and hexes, SigmaBarrierRenderer). */
	public static void submitField(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector, int argb,
		net.minecraft.resources.Identifier texture) {
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			collector.submitCustomGeometry(poseStack, FxRender.additive(texture),
				(pose, buffer) -> emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, false, argb, false, false));
		}
	}

	/**
	 * The model again as pure added light, white-cyan at this strength (0 to 1): Protection Suzu's cleanse, which in
	 * Overwatch turns them a glowing white (the second critique: the overlay alone only paled her, a ghost).
	 */
	public static void submitGlow(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		Set<Integer> hidden, float strength, boolean cull) {
		submitGlow(loaded, skin, poseStack, collector, hidden, strength, cull, 0xCFF8FF);
	}

	/** The same in this colour. */
	public static void submitGlow(HeroModels.Loaded loaded, Matrix4f[] skin, PoseStack poseStack, SubmitNodeCollector collector,
		Set<Integer> hidden, float strength, boolean cull, int rgb) {
		if (strength <= 0) {
			return;
		}
		HudShapes.ensure();
		// below clipping, so her shape still reads (at full white she was a flat cut-out, the third critique)
		int color = Math.clamp(Math.round(strength * 0.45f * 255), 0, 255) << 24 | rgb;
		for (SkinnedModel.Piece piece : loaded.model().pieces) {
			if (piece.image() < 0 || loaded.textures()[piece.image()] == null || isEyeShadow(loaded, piece)) {
				continue;
			}
			boolean prop = PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (hiddenNow(loaded.model(), piece, hidden, prop)) {
				continue;
			}
			collector.submitCustomGeometry(poseStack, FxRender.additive(HudShapes.WHITE),
				(pose, buffer) -> emit(piece, skin, pose, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, prop, color, cull, false));
		}
	}

	/**
	 * Held props: Overwatch models carry their weapons (Kiriko's three kunai, ofuda, Suzu) as small pieces on their own
	 * bones, parked at the model's origin (between the feet) unless an animation puts them in a hand. One piece can hold
	 * several (the three kunai), so a prop piece's triangles still at the origin after skinning aren't drawn.
	 */
	/** Added to a piece's index in a hidden set: that piece itself is put away (EmoteProps.hiddenPieces). */
	public static final int PIECE = 1 << 20;

	/**
	 * Whether a piece isn't drawn now: a held prop by its bone (her kunai, ofuda, Suzu), or any piece by itself (a geoset
	 * an emote hides that isn't a held prop: Lúcio's weapon cable, PIECE + its index).
	 */
	private static boolean hiddenNow(SkinnedModel model, SkinnedModel.Piece piece, Set<Integer> hidden, boolean prop) {
		if (hidden.isEmpty() || piece.vertexCount() == 0) {
			return false;
		}
		return prop && hidden.contains(piece.joint4()[0]) || hidden.contains(PIECE + model.pieces.indexOf(piece));
	}

	private static boolean isProp(SkinnedModel.Piece piece) {
		float[] pos = piece.pos();
		Vector3f lo = new Vector3f(Float.MAX_VALUE), hi = new Vector3f(-Float.MAX_VALUE);
		for (int v = 0; v < piece.vertexCount(); v++) {
			lo.min(new Vector3f(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2]));
			hi.max(new Vector3f(pos[v * 3], pos[v * 3 + 1], pos[v * 3 + 2]));
		}
		Vector3f center = new Vector3f(lo).add(hi).mul(0.5f);
		return hi.distance(lo) <= PROP_SIZE && center.length() <= STOWED_RADIUS;
	}

	/** How much of the view's lag the arms follow (Minecraft's hand uses 0.1: ours, by eye against gameplay footage). */
	private static final float ARM_SWAY = 0.15f;
	/**
	 * The first-person arms' size on screen, against the model's, scaled about the view's centre (a narrower field of
	 * view for them alone). Matched to the owner's Overwatch screenshot (2026-10-27: default weapons, Field of View 103,
	 * which is about Minecraft's 70° hand view): the ofuda fingertip, the kunai's tip and the kunai hand all sat about
	 * 1.27 times as far from the screen's centre there as at the earlier 1.1 (the owner's "10% bigger", #14).
	 */
	private static final float ARM_SCALE = 1.4f;
	/**
	 * How far the arms drift behind the movement (metres per m/s, ours), its limit, and how fast it follows (s): about
	 * 9 mm aside at a 5.5 m/s strafe (1.2 cm moved the arms 42 px: more than "slightly"), up to 2.5 cm falling;
	 * Overwatch's is subtle (#14).
	 */
	private static final float DRIFT_SIDE = 0.0016f;
	private static final float DRIFT_UP = 0.0015f;
	private static final float DRIFT_MAX = 0.025f;
	private static final float DRIFT_TIME = 0.1f;
	private static float driftX;
	private static float driftY;
	private static long driftAt = -1;

	/** Moves the arms' drift toward the player's velocity in view space, against it. */
	private static void drift(LocalPlayer player, float partialTicks) {
		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
		Vec3 v = player.getDeltaMovement().scale(20);
		Vector3fc left = camera.leftVector();
		Vector3fc up = camera.upVector();
		float right = -(float) (v.x * left.x() + v.y * left.y() + v.z * left.z());
		float rise = (float) (v.x * up.x() + v.y * up.y() + v.z * up.z());
		float targetX = Math.clamp(-right * DRIFT_SIDE, -DRIFT_MAX, DRIFT_MAX);
		float targetY = Math.clamp(-rise * DRIFT_UP, -DRIFT_MAX, DRIFT_MAX);
		long now = Util.getMillis();
		float dt = driftAt < 0 ? 1 : Math.min(1, (now - driftAt) / 1000f);
		driftAt = now;
		float k = 1 - (float) Math.exp(-dt / DRIFT_TIME);
		driftX += (targetX - driftX) * k;
		driftY += (targetY - driftY) * k;
	}
	/** Pieces at most this big (metres, corner to corner) can be held props. */
	private static final float PROP_SIZE = 0.6f;
	/** A prop triangle within this of the model's origin (metres) is parked, not held. */
	private static final float STOWED_RADIUS = 0.3f;
	private static final Map<SkinnedModel.Piece, Boolean> PROPS = new IdentityHashMap<>();

	/**
	 * Overwatch's eye shadow: a shell over the eyes, skinned to the eyelids, in one flat near-black (Kiriko's: 32 x 32 of
	 * #0E0E0E). The game's shader fades it softly; drawn as it is, it laid black slabs over her eyes ("she looks dead
	 * inside", the owner's playtest: #19), so it isn't drawn.
	 */
	private static boolean isEyeShadow(HeroModels.Loaded loaded, SkinnedModel.Piece piece) {
		return EYE_SHADOWS.computeIfAbsent(piece, p -> p.image() >= 0 && p.image() < loaded.flat().length && loaded.flat()[p.image()]
			&& loaded.model().mostlyOn(p, "Lid"));
	}

	private static final Map<SkinnedModel.Piece, Boolean> EYE_SHADOWS = new IdentityHashMap<>();

	/** Skins the piece's vertices and writes its triangles (as quads with the last corner doubled). */
	private static void emit(SkinnedModel.Piece piece, Matrix4f[] skin, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
							 boolean prop, boolean cull) {
		emit(piece, skin, pose, buffer, light, overlay, prop, -1, cull, false);
	}

	private static void emit(SkinnedModel.Piece piece, Matrix4f[] skin, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
							 boolean prop, int color, boolean cull, boolean triangles) {
		int n = piece.vertexCount();
		float[] p = new float[n * 3];
		float[] nm = new float[n * 3];
		skinned(piece, skin, p, nm);
		int[] idx = piece.indices();
		float[] uv = piece.uv();
		float parked = STOWED_RADIUS * STOWED_RADIUS;
		boolean closed = cull && Timing.cull && CLOSED.computeIfAbsent(piece, HeroModelRenderer::isClosed);
		// into the pose's space once per vertex (camera-relative: the camera at the origin), normals too
		Matrix4f m = pose.pose();
		Vector3f t = new Vector3f();
		float[] q = new float[n * 3];
		float[] qn = new float[n * 3];
		for (int v = 0; v < n; v++) {
			m.transformPosition(p[v * 3], p[v * 3 + 1], p[v * 3 + 2], t);
			q[v * 3] = t.x;
			q[v * 3 + 1] = t.y;
			q[v * 3 + 2] = t.z;
			pose.transformNormal(nm[v * 3], nm[v * 3 + 1], nm[v * 3 + 2], t);
			qn[v * 3] = t.x;
			qn[v * 3 + 1] = t.y;
			qn[v * 3 + 2] = t.z;
		}
		for (int t0 = 0; t0 + 2 < idx.length; t0 += 3 * Timing.stride) {
			int a = idx[t0], b = idx[t0 + 1], c = idx[t0 + 2];
			if (prop && near(p, a, parked) && near(p, b, parked) && near(p, c, parked)) {
				continue;
			}
			if (closed && facesAway(q, a, b, c)) {
				continue;
			}
			vertex(buffer, q, qn, uv, a, light, overlay, color);
			vertex(buffer, q, qn, uv, b, light, overlay, color);
			vertex(buffer, q, qn, uv, c, light, overlay, color);
			if (!triangles) {
				// a quad's fourth corner, the third again (for render types that draw quads)
				vertex(buffer, q, qn, uv, c, light, overlay, color);
			}
		}
	}

	/** Some of the piece's vertices posed by the skin: their positions into p (model space; the rest left as they are). */
	private static void skinnedSome(SkinnedModel.Piece piece, Matrix4f[] skin, int[] vertices, float[] p) {
		float[] pos = piece.pos();
		for (int v : vertices) {
			float px = pos[v * 3], py = pos[v * 3 + 1], pz = pos[v * 3 + 2];
			float ox = 0, oy = 0, oz = 0;
			for (int k = 0; k < 4; k++) {
				float w = piece.weight4()[v * 4 + k];
				if (w <= 0) {
					continue;
				}
				Matrix4f m = skin[piece.joint4()[v * 4 + k]];
				ox += w * (m.m00() * px + m.m10() * py + m.m20() * pz + m.m30());
				oy += w * (m.m01() * px + m.m11() * py + m.m21() * pz + m.m31());
				oz += w * (m.m02() * px + m.m12() * py + m.m22() * pz + m.m32());
			}
			p[v * 3] = ox;
			p[v * 3 + 1] = oy;
			p[v * 3 + 2] = oz;
		}
	}

	/** The piece's vertices posed by the skin: positions into p, unit normals into nm (model space). */
	private static void skinned(SkinnedModel.Piece piece, Matrix4f[] skin, float[] p, float[] nm) {
		int n = piece.vertexCount();
		float[] pos = piece.pos();
		float[] normal = piece.normal();
		for (int v = 0; v < n; v++) {
			float px = pos[v * 3], py = pos[v * 3 + 1], pz = pos[v * 3 + 2];
			float nx = normal[v * 3], ny = normal[v * 3 + 1], nz = normal[v * 3 + 2];
			float ox = 0, oy = 0, oz = 0, qx = 0, qy = 0, qz = 0;
			for (int k = 0; k < 4; k++) {
				float w = piece.weight4()[v * 4 + k];
				if (w <= 0) {
					continue;
				}
				Matrix4f m = skin[piece.joint4()[v * 4 + k]];
				ox += w * (m.m00() * px + m.m10() * py + m.m20() * pz + m.m30());
				oy += w * (m.m01() * px + m.m11() * py + m.m21() * pz + m.m31());
				oz += w * (m.m02() * px + m.m12() * py + m.m22() * pz + m.m32());
				qx += w * (m.m00() * nx + m.m10() * ny + m.m20() * nz);
				qy += w * (m.m01() * nx + m.m11() * ny + m.m21() * nz);
				qz += w * (m.m02() * nx + m.m12() * ny + m.m22() * nz);
			}
			float len = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
			if (len > 1.0E-6f) {
				qx /= len;
				qy /= len;
				qz /= len;
			}
			p[v * 3] = ox;
			p[v * 3 + 1] = oy;
			p[v * 3 + 2] = oz;
			nm[v * 3] = qx;
			nm[v * 3 + 1] = qy;
			nm[v * 3 + 2] = qz;
		}
	}

	/**
	 * The triangle faces away from the camera (at the origin). On a closed piece (her body) it's hidden behind the
	 * piece's near side anyway, so it isn't sent: sending her 67k triangles cost 23 ms a frame on an integrated GPU (31
	 * FPS in third person against 115 without her), almost all of it the upload, not our CPU work. Open pieces (hair
	 * and cloth cards, paper) show both sides and are sent whole: culled, her hair showed the sky through it.
	 */
	private static boolean facesAway(float[] q, int a, int b, int c) {
		float ax = q[a * 3], ay = q[a * 3 + 1], az = q[a * 3 + 2];
		float e1x = q[b * 3] - ax, e1y = q[b * 3 + 1] - ay, e1z = q[b * 3 + 2] - az;
		float e2x = q[c * 3] - ax, e2y = q[c * 3 + 1] - ay, e2z = q[c * 3 + 2] - az;
		float nx = e1y * e2z - e1z * e2y;
		float ny = e1z * e2x - e1x * e2z;
		float nz = e1x * e2y - e1y * e2x;
		return nx * ax + ny * ay + nz * az >= 0;
	}

	/**
	 * Nearly closed: under CLOSED_OPEN_EDGES of its edges border only one triangle (corners welded by position, as UV
	 * seams split them). Her body pieces: 0.8-2%; hair and prop cards: 5-50%.
	 */
	private static boolean isClosed(SkinnedModel.Piece piece) {
		float[] pos = piece.pos();
		Map<Long, Integer> weld = new HashMap<>();
		int[] id = new int[piece.vertexCount()];
		for (int v = 0; v < id.length; v++) {
			long key = Math.round(pos[v * 3] * 1e4) * 73856093L ^ Math.round(pos[v * 3 + 1] * 1e4) * 19349663L
				^ Math.round(pos[v * 3 + 2] * 1e4) * 83492791L;
			id[v] = weld.computeIfAbsent(key, k -> weld.size());
		}
		Map<Long, Integer> edges = new HashMap<>();
		int[] idx = piece.indices();
		for (int t = 0; t + 2 < idx.length; t += 3) {
			for (int e = 0; e < 3; e++) {
				int a = id[idx[t + e]];
				int b = id[idx[t + (e + 1) % 3]];
				edges.merge(Math.min(a, b) * 4294967296L + Math.max(a, b), 1, Integer::sum);
			}
		}
		long open = edges.values().stream().filter(n -> n == 1).count();
		return !edges.isEmpty() && open < edges.size() * CLOSED_OPEN_EDGES;
	}

	private static final float CLOSED_OPEN_EDGES = 0.03f;
	private static final Map<SkinnedModel.Piece, Boolean> CLOSED = new IdentityHashMap<>();

	private static boolean near(float[] p, int i, float radiusSq) {
		return p[i * 3] * p[i * 3] + p[i * 3 + 1] * p[i * 3 + 1] + p[i * 3 + 2] * p[i * 3 + 2] < radiusSq;
	}

	/** One corner, already in the pose's space. */
	private static void vertex(VertexConsumer buffer, float[] q, float[] n, float[] uv, int i, int light, int overlay, int color) {
		buffer.addVertex(q[i * 3], q[i * 3 + 1], q[i * 3 + 2])
			.setColor(color)
			.setUv(uv[i * 2], uv[i * 2 + 1])
			.setOverlay(overlay)
			.setLight(light)
			.setNormal(n[i * 3], n[i * 3 + 1], n[i * 3 + 2]);
	}
}
