package dev.overcraft.mixin;

import dev.overcraft.potg.PlayScore;
import dev.overcraft.potg.PotgValues;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Play of the Game's scoring: an end crystal destroyed (PotgValues.END_CRYSTAL, owner 2026-11-11). EndCrystal.onDestroyedBy
 * is where hurtServer ends once the crystal is removed: the source's entity is the hero for a hit, the owner for a
 * projectile, and for a crystal set off by another's blast the hero who set off the first (hurtServer passes the
 * explosion's cause on). A /kill's generic source has no one, and scores nothing.
 */
@Mixin(EndCrystal.class)
public abstract class PotgCrystalMixin {
	@Inject(method = "onDestroyedBy", at = @At("HEAD"))
	private void overcraft$potgCrystal(ServerLevel level, DamageSource source, CallbackInfo ci) {
		PlayScore.progress(source.getEntity(), PotgValues.END_CRYSTAL, "end crystal");
	}
}
