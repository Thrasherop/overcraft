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
	public record Draw(HeroModels.Loaded loaded, Matrix4f[] skin, float climbShift, Set<Integer> hidden, boolean cull) {
	}

	public static final RenderStateDataKey<Draw> DRAW = RenderStateDataKey.create(() -> "overcraft:hero_draw");
	/** The camera's inside her (the emote camera's first moments): she isn't drawn, not even as Minecraft's player. */
	private static final RenderStateDataKey<Boolean> UNSEEN = RenderStateDataKey.create(() -> "overcraft:hero_unseen");

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
		LAST_HERO.clear();
	}

	/** While extracting a player's render state: a hero in hero mode with a model gets a {@link Draw}. */
	public static void extract(LivingEntity entity, LivingEntityRenderState state, float partialTicks) {
		state.setData(DRAW, null);
		state.setData(UNSEEN, entity == Minecraft.getInstance().player && dev.overcraft.client.comms.EmoteCamera.inside());
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
		float facing = animator.facing(entity, partialTicks);
		state.yRot = Mth.wrapDegrees(state.yRot + state.bodyRot - facing);
		state.bodyRot = facing;
		long t0 = System.nanoTime();
		// in a menu's preview the head looks straight ahead: Minecraft's preview sets the head angles only after this
		// pose, so it took the player's own pitch in the world (looking down, the head bent over and the back of the
		// hair opened up: #29, #30)
		boolean previewing = preview != null && entity == Minecraft.getInstance().player;
		// on the select screen, Overwatch's hero select animation when the model has it (#29)
		Matrix4f[] skin = previewing ? HeroAnimator.poseShowcase(loaded.model(), showcaseSince < 0 ? 1000
			: (Util.getMillis() - showcaseSince) / 1000f) : null;
		if (skin == null) {
			skin = animator.pose(loaded.model(), entity, now, previewing ? 0 : state.yRot, previewing ? 0 : state.xRot);
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
		state.setData(DRAW, new Draw(loaded, skin, animator.climbShift(), hidden, preview == null));
		state.setData(SHADOW, previewing ? null : shadow(entity, partialTicks));
		state.setData(EmoteProps.HELD, previewing ? null : EmoteProps.held(hero.id, animator, loaded.model()));
		// his weapon when it's a model of its own (Lúcio's), on its hardpoint; put away in an emote, as Overwatch's are
		boolean weaponShown = !previewing && (!animator.emoting() || HeroWeapon.inEmote(hero, animator));
		state.setData(WEAPON, weaponShown ? HeroWeapon.place(hero, animator, loaded.model(), false, now) : null);
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
						dev.overcraft.client.effect.KirikoFx.enemyOf(entity), animator.playing().startsWith("wallride"));
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

	/** In place of LivingEntityRenderer.submit: false when there's nothing of ours to draw. */
	public static boolean submit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		Draw draw = state.getData(DRAW);
		if (draw == null || state.isInvisible) {
			return false;
		}
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
		submitModel(draw.loaded(), draw.skin(), poseStack, collector, light, overlay, draw.hidden(), draw.cull());
		if (state.outlineColor != 0) {
			submitOutline(draw.loaded(), draw.skin(), poseStack, collector, draw.hidden(), draw.cull(), state.outlineColor);
		}
		submitGlow(draw.loaded(), draw.skin(), poseStack, collector, draw.hidden(), white(state), draw.cull());
		EmoteProps.Held props = state.getData(EmoteProps.HELD);
		if (props != null) {
			EmoteProps.submit(props, poseStack, collector, light, overlay);
		}
		EmoteProps.Placed weapon = state.getData(WEAPON);
		if (weapon != null) {
			EmoteProps.submit(new EmoteProps.Held(java.util.List.of(weapon)), poseStack, collector, light, overlay);
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
	private static void emitShadow(ShadowMesh mesh, ShadowLaid shadow, VertexConsumer buffer, boolean mark) {
		int[] idx = mesh.indices();
		float[] laid = shadow.corners();
		for (int t0 = 0; t0 + 2 < idx.length; t0 += 3) {
			if (shadow.dropped()[t0 / 3]) {
				continue;
			}
			for (int k = 0; k < 3; k++) {
				int v = idx[t0 + k];
				int alpha = mark ? Math.round(laid[v * 4 + 3] * 255) : 255;
				buffer.addVertex(laid[v * 4], laid[v * 4 + 1], laid[v * 4 + 2])
					.setColor(alpha << 24 | 0xFFFFFF)
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
		if (ClientHero.heroView() || !dev.overcraft.client.controls.HeroOptions.get().heroArms || hands.isScoping
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
		// her own arms flash white when her Suzu cleanses her
		float white = Cleansed.white(player.getId());
		int overlay = white > 0 ? OverlayTexture.pack(white * 0.6f, false) : OverlayTexture.NO_OVERLAY;
		int lit = Math.max(light & 0xFFFF, VIEW_MODEL_MIN_LIGHT << 4) | light & 0xFFFF0000;
		submitModel(loaded, skin, poseStack, collector, lit, overlay, minecraft ? allProps(loaded.model()) : Set.of(), true);
		submitGlow(loaded, skin, poseStack, collector, Set.of(), white, true);
		// his weapon in his hand when it's a model of its own (Lúcio's), not in Minecraft mode (Minecraft's items then)
		EmoteProps.Placed weapon = minecraft ? null : HeroWeapon.place(hero, animator, loaded.model(), true, now);
		if (weapon != null) {
			EmoteProps.submit(new EmoteProps.Held(java.util.List.of(weapon)), poseStack, collector, lit, overlay);
		}
		if (minecraft) {
			// the main hand's item in her kunai hand, the other's in her ofuda hand (the owner's idea too)
			boolean right = avatar.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT;
			submitGripped(poseStack, collector, lit, player, right ? animator.kunaiHand : animator.ofudaHand, right,
				hands.mainHandItem, MAIN_HELD);
			submitGripped(poseStack, collector, lit, player, right ? animator.ofudaHand : animator.kunaiHand, !right,
				hands.offHandItem, OFF_HELD);
		}
		poseStack.popPose();
		return true;
	}

	/**
	 * A held item gripped in her fist, as Minecraft's players hold theirs in third person (its own third-person display
	 * turn and size: a sword's handle in the fist and its blade out, a block small and square), on her hand's weapon
	 * bone so it moves with her hand, no bigger than heldMax. Held to the view's corner the first-person way, items
	 * floated beside her fist (the owner, 2026-10-27).
	 */
	private static void submitGripped(PoseStack poseStack, SubmitNodeCollector collector, int light, LocalPlayer player, Matrix4f bone,
									  boolean kunaiSide, ItemStack item, net.minecraft.client.renderer.item.ItemStackRenderState state) {
		if (item.isEmpty()) {
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
			if (texture == null || isEyeShadow(loaded, piece)) {
				continue;
			}
			boolean prop = props && PROPS.computeIfAbsent(piece, HeroModelRenderer::isProp);
			if (hiddenNow(loaded.model(), piece, hidden, prop)) {
				continue;
			}
			collector.submitCustomGeometry(poseStack, FxRender.heroCutout(texture), (pose, buffer) -> {
				long t0 = System.nanoTime();
				emit(piece, skin, pose, buffer, light, overlay, prop, -1, cull, true);
				Timing.emit += System.nanoTime() - t0;
			});
		}
	}

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
