package dev.overcraft.client.effect;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.BlendOp;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.overcraft.Overcraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * How effects are drawn: glows added to the scene (Overwatch draws its effects additive: "render additive", the
 * effects survey), full bright, both sides, behind solid things but not hiding each other. Colour textures with
 * their own alpha (the climb's dust) are blended instead.
 */
public final class FxRender {
	/** Additive by the vertex alpha (so effects fade), no depth writes, no culling, unlit. */
	private static final RenderPipeline ADDITIVE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
		.withLocation(Overcraft.id("pipeline/effect_additive"))
		.withVertexShader("core/entity")
		.withFragmentShader("core/entity")
		.withShaderDefine("EMISSIVE")
		.withShaderDefine("NO_OVERLAY")
		.withShaderDefine("NO_CARDINAL_LIGHTING")
		.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
		.withCull(false)
		.withVertexBinding(0, DefaultVertexFormat.ENTITY)
		.withPrimitiveTopology(PrimitiveTopology.QUADS)
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
		.build());

	/**
	 * The same under Improved Transparency: Minecraft's own additive form (OIT_ADDITIVE, as its lightning and energy swirl
	 * have), adding light without covering what's behind. With the emissive entity set (alpha-blended) every effect's quad
	 * covered the scene with its black texels: Orbital Ray a black square (found in the 0.3.29 second pass).
	 */
	private static final net.minecraft.client.renderer.oit.OitPipelineSet ADDITIVE_OIT = RenderPipelines.register(
		net.minecraft.client.renderer.oit.OitPipelineSet.builder("overcraft_effect_additive", RenderPipeline.builder(RenderPipelines.OIT_ENTITY_SNIPPET)
			.withShaderDefine("EMISSIVE")
			.withShaderDefine("NO_OVERLAY")
			.withShaderDefine("NO_CARDINAL_LIGHTING")
			.withShaderDefine("OIT_ADDITIVE")
			.withCull(false)).build());

	private static final Map<Identifier, RenderType> ADDITIVE_TYPES = new HashMap<>();

	/**
	 * Minecraft's cutout entity pipeline drawing triangles instead of quads: skinned hero models are triangles, and as
	 * quads each went up with a corner doubled (a third more vertices: the upload is what their drawing costs).
	 */
	private static final RenderPipeline HERO_CUTOUT = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
		.withLocation(Overcraft.id("pipeline/hero_cutout"))
		.withShaderDefine("ALPHA_CUTOUT", 0.1F)
		.withShaderDefine("PER_FACE_LIGHTING")
		.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
		.withCull(false)
		.withColorTargetState(ColorTargetState.DEFAULT)
		.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
		.build());

	private static final Map<Identifier, RenderType> HERO_TYPES = new HashMap<>();

	/**
	 * A hero's body shadow (HeroModelRenderer.submitShadow), in two passes over the same flattened triangles, so that
	 * where they overlap (all of them lie on one plane, many layers deep) the ground darkens once: a depth test alone let
	 * every few overlapping ones through and the shadow came out speckled. The first marks the shadow's pixels in the
	 * frame's alpha: the least of 1 - strength there, however many triangles cover it, colour untouched. The second
	 * darkens each pixel by its mark and sets its alpha back to 1, so the next triangle over it changes nothing.
	 * Minecraft's entity shadow shader, in triangles, behind solid things, writing no depth.
	 */
	private static final RenderPipeline HERO_SHADOW_MARK = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SHADOW_SNIPPET)
		.withLocation(Overcraft.id("pipeline/hero_shadow_mark"))
		.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
		.withCull(false)
		.withColorTargetState(new ColorTargetState(Optional.of(new BlendFunction(BlendFactor.ZERO, BlendFactor.ONE, BlendOp.ADD,
			BlendFactor.ONE, BlendFactor.ONE, BlendOp.MIN)), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALPHA))
		.build());
	private static final RenderPipeline HERO_SHADOW_DARKEN = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SHADOW_SNIPPET)
		.withLocation(Overcraft.id("pipeline/hero_shadow_darken"))
		.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
		.withCull(false)
		.withColorTargetState(new ColorTargetState(Optional.of(new BlendFunction(BlendFactor.ZERO, BlendFactor.DST_ALPHA, BlendOp.ADD,
			BlendFactor.ONE, BlendFactor.ZERO, BlendOp.ADD)), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALL))
		.build());

	/**
	 * Improved Transparency's shadow: one blended pass (Minecraft's entity shadow, in triangles) with its order-independent
	 * pipelines; every blended render type needs them in that mode, or the frame crashes (found in the 0.3.29 release play).
	 */
	private static final RenderPipeline HERO_SHADOW_PLAIN = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SHADOW_SNIPPET)
		.withLocation(Overcraft.id("pipeline/hero_shadow_plain"))
		.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
		.withCull(false)
		// blended, as Minecraft's ENTITY_SHADOW: the snippet has no colour target, and a type with none counts as unblended,
		// goes to the solid phase and crashes it ("color attachment count must match": found in the 0.3.29 second pass)
		.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
		.build());
	private static final net.minecraft.client.renderer.oit.OitPipelineSet HERO_SHADOW_OIT = RenderPipelines.register(
		net.minecraft.client.renderer.oit.OitPipelineSet.builder("overcraft_hero_shadow",
			RenderPipeline.builder(RenderPipelines.ENTITY_SHADOW_SNIPPET).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)).build());

	private static RenderType heroShadowMark;
	private static RenderType heroShadowDarken;
	private static RenderType heroShadowOit;

	/** A hero's body shadow in one blended pass, for Improved Transparency (vertex colour black at its strength). */
	public static RenderType heroShadowOit() {
		if (heroShadowOit == null) {
			heroShadowOit = RenderType.create("overcraft_hero_shadow_oit", RenderSetup.builder(HERO_SHADOW_PLAIN)
				.setOitPipelines(HERO_SHADOW_OIT)
				.withTexture("Sampler0", dev.overcraft.client.hud.HudShapes.WHITE).useLightmap().useOverlay()
				.setLayeringTransform(net.minecraft.client.renderer.rendertype.LayeringTransform.VIEW_OFFSET_Z_LAYERING)
				.createRenderSetup());
		}
		return heroShadowOit;
	}

	/** As RenderTypes.entityCutout, in triangles: hero models (their emit writes three corners a triangle). */
	public static RenderType heroCutout(Identifier texture) {
		return HERO_TYPES.computeIfAbsent(texture, t -> RenderType.create("overcraft_hero_cutout",
			RenderSetup.builder(HERO_CUTOUT).withTexture("Sampler0", t).useLightmap().useOverlay().affectsCrumbling()
				.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).createRenderSetup()));
	}

	/** A hero's body shadow, first pass: its pixels marked (vertex alpha 1 - strength). */
	public static RenderType heroShadowMark() {
		if (heroShadowMark == null) {
			heroShadowMark = shadowType("overcraft_hero_shadow_mark", HERO_SHADOW_MARK);
		}
		return heroShadowMark;
	}

	/** A hero's body shadow, second pass: the marked pixels darkened once (vertex colour white). */
	public static RenderType heroShadowDarken() {
		if (heroShadowDarken == null) {
			heroShadowDarken = shadowType("overcraft_hero_shadow_darken", HERO_SHADOW_DARKEN);
		}
		return heroShadowDarken;
	}

	/** Set up as Minecraft's entity shadow is (nudged toward the camera off the ground it lies on). */
	private static RenderType shadowType(String name, RenderPipeline pipeline) {
		return RenderType.create(name, RenderSetup.builder(pipeline)
			.withTexture("Sampler0", dev.overcraft.client.hud.HudShapes.WHITE).useLightmap().useOverlay()
			.setLayeringTransform(net.minecraft.client.renderer.rendertype.LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			.createRenderSetup());
	}

	private FxRender() {
	}

	/** Loads with the class: the pipeline must be registered before Minecraft compiles its shaders. */
	public static void init() {
	}

	public static RenderType additive(Identifier texture) {
		return ADDITIVE_TYPES.computeIfAbsent(texture, t -> RenderType.create("overcraft_effect_additive",
			RenderSetup.builder(ADDITIVE).setOitPipelines(ADDITIVE_OIT)
				.withTexture("Sampler0", t).useLightmap().useOverlay().sortOnUpload().createRenderSetup()));
	}

	/** Blended by the texture's own alpha (colour sprites), full bright, no depth writes. */
	public static RenderType blended(Identifier texture) {
		return RenderTypes.eyes(texture);
	}
}
