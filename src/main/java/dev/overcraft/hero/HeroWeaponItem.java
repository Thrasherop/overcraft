package dev.overcraft.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A hero's weapon, kept in hotbar slot 1. It does nothing by itself: with it selected, the client sends the hero's
 * controls instead of Minecraft's attack/use. The server takes away any copy outside slot 1.
 */
public class HeroWeaponItem extends Item {
	public final Hero hero;

	public HeroWeaponItem(Hero hero, Properties properties) {
		super(properties);
		this.hero = hero;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return InteractionResult.FAIL;
	}

	@Override
	public boolean canDestroyBlock(ItemStack itemStack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
		return false;
	}
}
