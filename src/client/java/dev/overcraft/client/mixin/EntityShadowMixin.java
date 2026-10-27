package dev.overcraft.client.mixin;

import dev.overcraft.client.model.HeroModelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No round blob under a hero who casts her body's shadow (HeroModelRenderer.shadow): Overwatch's heroes cast
 * body-shaped shadows and no blob (the seventh critique, #11; the owner, 2026-10-27).
 */
@Mixin(EntityRenderer.class)
public abstract class EntityShadowMixin {
	@Inject(method = "finalizeRenderState", at = @At("TAIL"))
	private void overcraft$noBlobUnderHeroes(Entity entity, EntityRenderState state, CallbackInfo ci) {
		if (HeroModelRenderer.drawsHero(state)) {
			state.shadowPieces.clear();
		}
	}
}
