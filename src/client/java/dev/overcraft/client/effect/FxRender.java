package dev.overcraft.client.effect;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
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

	/** As RenderTypes.entityCutout, in triangles: hero models (their emit writes three corners a triangle). */
	public static RenderType heroCutout(Identifier texture) {
		return HERO_TYPES.computeIfAbsent(texture, t -> RenderType.create("overcraft_hero_cutout",
			RenderSetup.builder(HERO_CUTOUT).withTexture("Sampler0", t).useLightmap().useOverlay().affectsCrumbling()
				.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE).createRenderSetup()));
	}

	private FxRender() {
	}

	/** Loads with the class: the pipeline must be registered before Minecraft compiles its shaders. */
	public static void init() {
	}

	public static RenderType additive(Identifier texture) {
		return ADDITIVE_TYPES.computeIfAbsent(texture, t -> RenderType.create("overcraft_effect_additive",
			RenderSetup.builder(ADDITIVE).withTexture("Sampler0", t).useLightmap().useOverlay().sortOnUpload().createRenderSetup()));
	}

	/** Blended by the texture's own alpha (colour sprites), full bright, no depth writes. */
	public static RenderType blended(Identifier texture) {
		return RenderTypes.eyes(texture);
	}
}
