package dev.overcraft.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The inventory's doll of a hero taller than Minecraft's player (Sigma: his model is 2.5 m, HeroBody): framed by the hero's
 * own height in both modes. Minecraft sizes the doll at 30 px a block in a 70 px frame and centres it on half the box, so in
 * Minecraft mode (his box 1.8 m) his head was cut off at the frame's top (found in his release play, 2026-11-02).
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryDollMixin {
	@WrapOperation(method = "extractBackground", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;extractEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V"))
	private void overcraft$heroDoll(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int size, float offsetY,
		float mouseX, float mouseY, LivingEntity entity, Operation<Void> original) {
		if (entity instanceof Player player) {
			String id = player.getAttached(dev.overcraft.hero.HeroManager.HERO);
			EntityDimensions body = id == null ? null : dev.overcraft.hero.HeroBody.of(dev.overcraft.hero.Hero.byId(id), Pose.STANDING);
			if (body != null && body.height() > 1.8f) {
				// his height in the frame Minecraft gives 1.8 m, centred on half of it rather than half the box he has now
				float tall = body.height();
				size = Math.round(size * 1.8f / tall);
				offsetY += (tall - player.getBbHeight()) / 2;
			}
		}
		original.call(graphics, x0, y0, x1, y1, size, offsetY, mouseX, mouseY, entity);
	}
}
