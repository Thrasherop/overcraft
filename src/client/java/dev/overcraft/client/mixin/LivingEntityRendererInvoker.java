package dev.overcraft.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Dev only (HeadBoxDump): a renderer's own model scale (LivingEntityRenderer.scale), to place a model's head as it's drawn. */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker {
	@Invoker("scale")
	void overcraft$scale(LivingEntityRenderState state, PoseStack poseStack);
}
