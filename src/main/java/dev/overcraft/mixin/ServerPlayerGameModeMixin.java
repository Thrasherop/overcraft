package dev.overcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.overcraft.hero.HeroManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In hero mode the hotbar is hidden and the mouse is the hero's: Interact (F) reaches doors, chests, buttons and levers as an
 * empty hand would, but never uses the held item. It placed the held block, poured a lava bucket, threw an ender pearl (a
 * critique: ClientHero's Interact runs Minecraft's whole right click). Server side, so a client can't do it either.
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void overcraft$noItemUse(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand,
									  CallbackInfoReturnable<InteractionResult> cir) {
		if (HeroManager.inHeroPlay(player)) {
			cir.setReturnValue(InteractionResult.PASS);
		}
	}

	/** A block's own use of the held item (a bucket on a cauldron, a dye on a sign): tried as an empty hand instead. */
	@WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
	private InteractionResult overcraft$emptyHandOnBlocks(BlockState state, ItemStack stack, Level level, Player player, InteractionHand hand,
														  BlockHitResult hit, Operation<InteractionResult> original) {
		if (player instanceof ServerPlayer server && HeroManager.inHeroPlay(server)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		return original.call(state, stack, level, player, hand, hit);
	}

	/** The held item's own use on the block (placing it, a bucket, flint and steel): none in hero mode. */
	@WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
	private InteractionResult overcraft$noUseOn(ItemStack stack, UseOnContext context, Operation<InteractionResult> original,
												@Local(argsOnly = true) ServerPlayer player) {
		if (HeroManager.inHeroPlay(player)) {
			return InteractionResult.PASS;
		}
		return original.call(stack, context);
	}
}
