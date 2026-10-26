package dev.overcraft.client.model;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * A hero model's texture, smoothly filtered and mipmapped like a real 3D game's (as Minecraft's MipmappedTexture
 * does for resource textures). DynamicTexture samples nearest-neighbour with no mipmaps: right for 16x16 pixel art,
 * but it turns a 2048 px Overwatch texture seen small into hard pixels and shimmer.
 */
public final class HeroTexture extends AbstractTexture {
	/** Mip levels down to 16 px: below that a texture is one blur anyway. */
	private static final int MAX_MIP_LEVEL = 7;

	/** A model texture: anything with holes mipmaps as a cutout (Minecraft's AUTO), keeping hair and fringes whole. */
	public HeroTexture(Identifier id, NativeImage image) {
		this(id, image, MipmapStrategy.AUTO);
	}

	/**
	 * With a mipmap strategy: MEAN for soft masks drawn blended (HUD shapes, effects). A cutout's smaller mips get 2.5%
	 * alpha added everywhere and their alpha rescaled (MipmapGenerator.scaleAlphaToCoverage), which drew a faint box
	 * round every HUD shape and distorted soft glows.
	 */
	public HeroTexture(Identifier id, NativeImage image, MipmapStrategy strategy) {
		GpuDevice device = RenderSystem.getDevice();
		int levels = Math.min(MAX_MIP_LEVEL, Mth.log2(Math.min(image.getWidth(), image.getHeight())) - 4);
		Transparency transparency = image.computeTransparency();
		NativeImage[] mips = MipmapGenerator.generateMipLevels(id, new NativeImage[] {image}, Math.max(0, levels),
			strategy, 0.0F, transparency);
		texture = device.createTexture(id::toString, 5, GpuFormat.RGBA8_UNORM, image.getWidth(), image.getHeight(), 1, mips.length);
		textureView = device.createTextureView(texture);
		for (int level = 0; level < mips.length; level++) {
			device.createCommandEncoder().writeToTexture(texture, mips[level], level, 0, 0, 0);
		}
		for (NativeImage mip : mips) {
			mip.close();
		}
		sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR,
			FilterMode.LINEAR, true);
	}
}
