package dev.overcraft.client.mixin;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Dev only (HeadBoxDump): a renderer's adult and baby models (it picks one only as it draws: AgeableMobRenderer.submit). */
@Mixin(AgeableMobRenderer.class)
public interface AgeableMobRendererAccessor {
	@Accessor("adultModel")
	EntityModel<?> overcraft$adultModel();

	@Accessor("babyModel")
	EntityModel<?> overcraft$babyModel();
}
