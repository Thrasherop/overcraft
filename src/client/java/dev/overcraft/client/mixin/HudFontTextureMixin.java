package dev.overcraft.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.client.gui.font.FontTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

/**
 * The HUD's fonts (overcraft:hud*, Overwatch's faces from the local pack) sampled smoothly: Minecraft samples every font
 * texture nearest, made for its pixel font drawn at whole multiples; the HUD draws Overwatch's faces shrunk about 2x
 * from their raster, and nearest sampling left their edges in stair steps (the eighth critique, #7: "all small text
 * is smooth" in Overwatch). Minecraft's own fonts are untouched.
 */
@Mixin(FontTexture.class)
public abstract class HudFontTextureMixin {
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/systems/SamplerCache;getRepeat(Lcom/mojang/renderpearl/api/textures/FilterMode;)Lcom/mojang/renderpearl/api/textures/GpuSampler;"))
	private GpuSampler overcraft$smoothHudFonts(SamplerCache cache, FilterMode mode, Operation<GpuSampler> original,
												@Local(argsOnly = true) Supplier<String> label) {
		return original.call(cache, label.get().startsWith("overcraft:hud") ? FilterMode.LINEAR : mode);
	}
}
