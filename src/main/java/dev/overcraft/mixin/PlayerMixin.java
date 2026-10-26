package dev.overcraft.mixin;

import dev.overcraft.Overcraft;
import dev.overcraft.hero.HeroManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A hero sounds like the hero, not Steve: their own pain grunt when hurt (Minecraft's plain hurt sound; burning,
 * drowning and the like keep theirs) and their own cry when they die. On every side (the hero attachment is synced),
 * as Minecraft plays these from the entity itself.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "getHurtSound", at = @At("RETURN"), cancellable = true)
	private void overcraft$heroPain(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
		if (cir.getReturnValue() == SoundEvents.PLAYER_HURT) {
			SoundEvent pain = heroLine("pain");
			if (pain != null) {
				cir.setReturnValue(pain);
			}
		}
	}

	@Inject(method = "getDeathSound", at = @At("RETURN"), cancellable = true)
	private void overcraft$heroDeath(CallbackInfoReturnable<SoundEvent> cir) {
		SoundEvent death = heroLine("death");
		if (death != null) {
			cir.setReturnValue(death);
		}
	}

	private SoundEvent heroLine(String moment) {
		String hero = ((Player) (Object) this).getAttached(HeroManager.HERO);
		return hero == null ? null : BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id(hero + ".voice." + moment));
	}
}
