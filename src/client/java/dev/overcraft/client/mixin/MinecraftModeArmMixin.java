package dev.overcraft.client.mixin;

import dev.overcraft.client.ClientHero;
import dev.overcraft.client.model.HeroArmSkin;
import dev.overcraft.hero.Hero;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Minecraft mode's first-person arm dressed as the hero (HeroArmSkin): with a hero picked, the body others see (and
 * she sees in third person) is the hero's, so her first-person arm is too. Hero mode draws the hero's own arms instead.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MinecraftModeArmMixin {
	@ModifyVariable(method = "renderPlayerHand", at = @At("STORE"), ordinal = 0)
	private Identifier overcraft$heroArm(Identifier skinTexture) {
		Hero hero = ClientHero.hero();
		if (hero == null || ClientHero.heroView()) {
			return skinTexture;
		}
		Identifier arm = HeroArmSkin.get(hero);
		return arm != null ? arm : skinTexture;
	}
}
